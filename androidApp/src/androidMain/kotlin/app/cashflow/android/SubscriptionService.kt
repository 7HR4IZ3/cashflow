package app.cashflow.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.cashflow.core.*
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

/** Play owns checkout; only the backend can grant a verified entitlement. */
class SubscriptionService(private val app: CashflowApplication) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = app.getSharedPreferences("subscriptions", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val details = mutableMapOf<String, Pair<ProductDetails, String>>()
    private var restoring = false
    private var backendReady = false
    private var aiReady = false
    private val endpoint = BuildConfig.CASHFLOW_BACKEND_URL.trimEnd('/')
    private val client = BillingClient.newBuilder(app).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection().build()

    fun start() {
        scope.launch {
            runCatching { health() }.onSuccess {
                backendReady = it.optBoolean("billingReady")
                aiReady = it.optBoolean("aiReady")
                app.store.updateSubscription(app.store.billingState().copy(available = aiReady))
            }
        }
        prefs.getString("verified", null)?.let { cached ->
            runCatching { json.decodeFromString<SubscriptionState>(cached) }.getOrNull()?.let {
                app.store.updateSubscription(it.copy(available = aiReady))
            }
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) { loadOffers(); refresh() }
                else message("Google Play billing is unavailable. Your records are still available.")
            }
            override fun onBillingServiceDisconnected() { message("Google Play disconnected. Try Restore purchases.") }
        })
    }
    private fun message(value: String?, busy: Boolean = false) {
        app.store.updateSubscription(app.store.billingState().copy(message = value, busy = busy))
    }
    private fun loadOffers() {
        val products = listOf("cashflow_lite", "cashflow_premium").map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, response ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) { message("Couldn't load prices from Google Play."); return@queryProductDetailsAsync }
            details.clear()
            val offers = response.productDetailsList.flatMap { product ->
                product.subscriptionOfferDetails.orEmpty().filter { it.offerId == null }.mapNotNull { offer ->
                    val phase = offer.pricingPhases.pricingPhaseList.singleOrNull() ?: return@mapNotNull null
                    val period = when (phase.billingPeriod) { "P1M" -> "month"; "P1Y" -> "year"; else -> return@mapNotNull null }
                    val id = "${product.productId}:${offer.basePlanId}"
                    details[id] = product to offer.offerToken
                    PlanOffer(id, if (product.productId == "cashflow_lite") PlanTier.LITE else PlanTier.PREMIUM, phase.formattedPrice, period)
                }
            }
            app.store.updateSubscription(app.store.billingState().copy(offers = offers, available = aiReady, message = if (offers.isEmpty()) "Subscriptions are not available in Google Play yet." else null))
        }
    }
    fun refresh() {
        if (restoring) return
        if (!client.isReady) { message("Connecting to Google Play. Try again shortly."); return }
        restoring = true
        message(null, true)
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) { restoring = false; message("Couldn't restore purchases. Try again."); return@queryPurchasesAsync }
            scope.launch {
                try {
                    val owned = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.any { id -> id == "cashflow_lite" || id == "cashflow_premium" } }
                    if (owned.isEmpty()) {
                        prefs.edit().remove("token").remove("verified").apply()
                        app.store.updateSubscription(SubscriptionState(offers = app.store.billingState().offers, available = aiReady, message = if (purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) "Payment is pending. Your plan activates after Google confirms it." else "No active subscription found."))
                    } else {
                        val verified = owned.mapNotNull { purchase ->
                            try { purchase to call("/v1/subscription", purchase.purchaseToken, JSONObject()) }
                            catch (_: Exception) { null }
                        }
                        check(verified.isNotEmpty())
                        val best = verified.maxBy { (_, response) -> tier(response.getString("tier")).ordinal }
                        applyVerified(best.second)
                        prefs.edit().putString("token", best.first.purchaseToken).apply()
                    }
                } catch (_: Exception) { message("Purchase verification is unavailable. Your last verified plan remains until its expiry.") }
                finally { restoring = false; app.store.updateSubscription(app.store.billingState().copy(busy = false)) }
            }
        }
    }
    fun buy(activity: Activity?, offerId: String) {
        if (endpoint.isBlank() || !backendReady) { message("Subscriptions aren't connected yet. No payment was started."); return }
        if (activity == null || !client.isReady) { message("Google Play is not ready. Try again."); return }
        val selected = details[offerId] ?: run { loadOffers(); message("Prices changed. Please choose your plan again."); return }
        val accountId = prefs.getString("accountId", null) ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit().putString("accountId", it).apply()
        }
        val params = BillingFlowParams.newBuilder().setObfuscatedAccountId(accountId).setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(selected.first).setOfferToken(selected.second).build()
        ))
        val oldToken = prefs.getString("token", null)
        if (oldToken != null && app.store.state.value.subscription.tier != PlanTier.FREE) {
            val nextTier = if (selected.first.productId == "cashflow_lite") PlanTier.LITE else PlanTier.PREMIUM
            params.setSubscriptionUpdateParams(BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldToken)
                .setSubscriptionReplacementMode(if (nextTier.ordinal < app.store.state.value.subscription.tier.ordinal)
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED else BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION)
                .build())
        }
        val result = client.launchBillingFlow(activity, params.build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) message("Couldn't open Google Play checkout. Try again.")
    }
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingClient.BillingResponseCode.USER_CANCELED -> message("Purchase cancelled. Your current plan is unchanged.")
            else -> message("Purchase couldn't be completed. Restore purchases if you were charged.")
        }
    }
    fun manage() {
        app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun analyse(item: ReviewItem, automatic: Boolean) {
        val work = androidx.work.OneTimeWorkRequestBuilder<AiReviewWorker>()
            .setInputData(androidx.work.workDataOf("reviewId" to item.id, "automatic" to automatic))
            .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        androidx.work.WorkManager.getInstance(app).enqueueUniqueWork("cashflow-ai:${item.id}", androidx.work.ExistingWorkPolicy.KEEP, work)
    }
    suspend fun runAnalysis(reviewId: String, automatic: Boolean, attempt: Int): Boolean = withContext(Dispatchers.Main.immediate) {
        val item = app.store.state.value.review.firstOrNull { it.id == reviewId }
        if (item == null || !app.store.state.value.aiEnabled || (automatic && !app.store.state.value.aiAutomatic)) {
            app.store.completeAi(reviewId, item?.text.orEmpty(), null)
            return@withContext false
        }
        try {
            val token = prefs.getString("token", null) ?: error("No verified purchase")
            val response = call("/v1/analyse", token, JSONObject().put("requestId", item.id).put("text", item.text).put("source", item.source).put("consent", true))
            val suggestion = json.decodeFromString<AiSuggestion>(response.getJSONObject("suggestion").toString())
            response.optJSONObject("subscription")?.let(::applyVerified)
            app.store.completeAi(item.id, item.text, if (automatic && !app.store.state.value.aiAutomatic) null else suggestion)
            false
        } catch (cancelled: CancellationException) {
            app.store.completeAi(item.id, item.text, null)
            throw cancelled
        } catch (error: Exception) {
            val retry = attempt < 2 && (error !is BackendException || error.status >= 500 || (error.status == 429 && error.code != "quota_exhausted"))
            app.store.completeAi(item.id, item.text, null, if (retry) "AI help will retry when connected. Your alert stays in Review." else "AI help couldn't finish. Your alert is still in Review; no record was changed.")
            retry
        }
    }
    private class BackendException(val status: Int, val code: String) : Exception("Service unavailable")
    private fun tier(value: String) = PlanTier.entries.firstOrNull { it.name == value } ?: PlanTier.FREE
    private fun applyVerified(response: JSONObject) {
        val state = SubscriptionState(tier(response.getString("tier")), response.getLong("expiresAt"), response.optInt("used"), response.optString("month"), aiReady, offers = app.store.billingState().offers)
        prefs.edit().putString("verified", json.encodeToString(SubscriptionState.serializer(), state.copy(offers = emptyList()))).apply()
        app.store.updateSubscription(state)
    }
    private suspend fun health(): JSONObject = withContext(Dispatchers.IO) {
        val uri = URI(endpoint)
        require(uri.scheme == "https" && uri.host != null)
        val connection = uri.resolve("/health").toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000; connection.readTimeout = 10000; connection.instanceFollowRedirects = false
            check(connection.responseCode == 200)
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }
    private suspend fun call(path: String, token: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val uri = URI(endpoint)
        require(uri.scheme == "https" && uri.host != null) { "HTTPS backend required" }
        val connection = uri.resolve(path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 15000; connection.readTimeout = 25000
            connection.instanceFollowRedirects = false; connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) {
                val error = runCatching { connection.errorStream?.bufferedReader()?.use { it.readText().take(2048) }?.let(::JSONObject) }.getOrNull()
                throw BackendException(connection.responseCode, error?.optString("code").orEmpty())
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            check(response.length < 32768)
            JSONObject(response)
        } finally { connection.disconnect() }
    }
}
