package app.cashflow.core

import app.cashflow.db.LedgerDatabase
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface PlatformHooks {
    fun now(): String
    fun id(): String
    fun captureState(selected: Set<String>): CaptureState
    fun openCaptureSettings()
    fun requestNotificationPermission()
    fun exportCsv(contents: String): String?
    fun recorded(entry: Entry)
}

/** All writes go through SQLite transactions. UI and native ingestion share this instance. */
class LedgerStore(driver: SqlDriver, private val platform: PlatformHooks) : CashflowActions {
    private val database = LedgerDatabase(driver)
    private val queries = database.ledgerQueries
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutableState = MutableStateFlow(CashflowState())
    val state: StateFlow<CashflowState> = mutableState
    private var preferences = emptyMap<String, String>()
    private var focusedId: String? = null

    init { reload() }

    private fun reload() {
        preferences = queries.allPreferences().executeAsList().associate { it.key to it.value_ }
        val selected = preferences.filter { it.key.startsWith("source:") && it.value == "true" }.keys.map { it.removePrefix("source:") }.toSet()
        mutableState.value = CashflowState(
            entries = queries.allEntries().executeAsList().map { json.decodeFromString<Entry>(it.payload) }.sortedByDescending { it.date },
            review = queries.allEvents().executeAsList().filter { it.ignored == 0L }.mapNotNull { it.review_payload?.let { body -> json.decodeFromString<ReviewItem>(body) } },
            ignoredReview = queries.allEvents().executeAsList().filter { it.ignored != 0L }.mapNotNull { it.review_payload?.let { body -> json.decodeFromString<ReviewItem>(body) } },
            today = platform.now().take(10),
            onboardingComplete = preferences["onboarded"] == "true",
            onboardingStep = preferences["onboardingStep"]?.toIntOrNull() ?: 0,
            currency = preferences["currency"] ?: "NGN",
            capturePaused = preferences["paused"] == "true",
            confirmations = preferences["confirmations"] != "false",
            capture = platform.captureState(selected).copy(lastReceived = preferences["lastReceived"]),
            requestedEntryId = focusedId,
        )
    }

    private fun perform(block: () -> Unit): String? = try {
        block()
        reload()
        null
    } catch (e: Exception) {
        val message = if (e is IllegalArgumentException) e.message ?: "Check these details" else "Could not save. Please try again."
        mutableState.value = mutableState.value.copy(error = message)
        message
    }

    private fun preference(key: String, value: String) { perform { queries.putPreference(key, value) } }
    private fun put(entry: Entry) { queries.putEntry(entry.id, json.encodeToString(entry)) }
    private fun validateTotals(entry: Entry) { totals(state.value.entries.filterNot { it.id == entry.id } + entry, entry.currency) }
    private fun entry(id: String): Entry = state.value.entries.firstOrNull { it.id == id } ?: error("Entry no longer exists")

    override fun save(draft: EntryDraft, entryId: String?, reviewId: String?): String? = perform {
        require(draft.directionConfirmed) { "Choose whether money came in or went out" }
        require(entryId == null || reviewId == null) { "Choose an existing record or a review alert" }
        require(reviewId == null || state.value.review.any { it.id == reviewId }) { "This alert is no longer waiting for review" }
        val amount = Money.parse(draft.amount, draft.currency) ?: throw IllegalArgumentException("Enter a positive amount with the right decimal places and a supported currency")
        require(draft.kind != Kind.INCOME || draft.direction == Direction.IN) { "Income must be money in. Choose a different kind for money out." }
        require(draft.kind !in listOf(Kind.EXPENSE, Kind.FEE) || draft.direction == Direction.OUT) { "Spending and fees must be money out. Choose a different kind for money in." }
        val date = draft.date.ifBlank { platform.now().take(10) }
        require(validDate(date)) { "Use a real date in YYYY-MM-DD format" }
        val old = entryId?.let { entry(it) }
        require(old?.deleted != true) { "Restore this record before editing it" }
        val item = Entry(
            id = old?.id ?: platform.id(), amountMinor = amount, currency = draft.currency,
            direction = draft.direction, kind = draft.kind, status = draft.status,
            counterparty = draft.counterparty.trim(), account = draft.account.ifBlank { "Unassigned" },
            note = draft.note.trim(), date = date, source = old?.source ?: reviewId?.let { id -> state.value.review.firstOrNull { it.id == id }?.source } ?: "Manual",
            userEdited = old != null || reviewId != null, revision = (old?.revision ?: 0) + 1,
        )
        validateTotals(item)
        database.transaction {
            put(item)
            if (reviewId != null) queries.resolveEvent(item.id, 0, reviewId)
        }
    }

    override fun delete(entryId: String): String? = perform { val e = entry(entryId); put(e.copy(deleted = true, revision = e.revision + 1)) }
    override fun restore(entryId: String): String? = perform {
        val e = entry(entryId).let { it.copy(deleted = false, revision = it.revision + 1) }
        validateTotals(e)
        put(e)
    }
    override fun ignoreReview(reviewId: String): String? = perform { queries.setEventIgnored(1, reviewId) }
    override fun restoreReview(reviewId: String): String? = perform { queries.setEventIgnored(0, reviewId) }
    override fun captureText(text: String, source: String): String? = ingest(text, source, platform.id(), respectPause = false)

    /** The delivery key identifies a posted event, not a guessed financial duplicate. */
    fun ingest(text: String, source: String, deliveryKey: String, respectPause: Boolean = true): String? = perform {
        require(text.isNotBlank()) { "Paste a payment alert first" }
        require(text.length <= 8192) { "This alert is too long" }
        if (respectPause && state.value.capturePaused) return@perform
        val eventId = deliveryKey
        if (queries.findEvent(eventId).executeAsOneOrNull() != null) return@perform
        val parsed = AlertParser.parse(text, state.value.currency, platform.now().take(10))
        if (parsed.irrelevant) return@perform
        var created: Entry? = null
        database.transaction {
            queries.putPreference("lastReceived", platform.now())
            val draft = parsed.draft ?: return@transaction
            val referenceKey = parsed.reference?.let { "ref:$source:$it" }
            val existing = referenceKey?.let { key -> preferences[key] }?.let { id -> state.value.entries.firstOrNull { it.id == id } }
            var reason = parsed.reason
            var linked: Entry? = null
            val amount = Money.parse(draft.amount, draft.currency)
            if (existing != null) {
                if (existing.amountMinor == amount && existing.currency == draft.currency && existing.direction == draft.direction) {
                    linked = existing
                    if (!existing.deleted && !existing.userEdited && existing.status == PaymentStatus.PENDING && draft.status != PaymentStatus.PENDING) {
                        linked = existing.copy(status = draft.status, revision = existing.revision + 1)
                        validateTotals(linked)
                        put(linked)
                    } else if (existing.status != draft.status && !existing.deleted) reason = "Check this status update against the existing record"
                } else reason = "Reference matches but payment details differ"
            }
            if (reason == null && linked == null && amount != null) {
                created = Entry(platform.id(), amount, draft.currency, draft.direction, draft.kind, draft.status,
                    draft.counterparty, date = draft.date, source = source)
                validateTotals(created!!)
                put(created!!)
                if (referenceKey != null) queries.putPreference(referenceKey, created!!.id)
            }
            val review = if (reason != null) ReviewItem(eventId, source, text, reason, draft) else null
            queries.putEvent(eventId, source, text, platform.now(), linked?.id ?: created?.id,
                review?.let { json.encodeToString(it) }, 0)
        }
        // Persist before notifying. Notification failure must never roll back a committed ledger write.
        reload()
        created?.takeIf { it.status == PaymentStatus.COMPLETED && state.value.confirmations }?.let {
            try { platform.recorded(it) } catch (_: Exception) { /* Ledger remains committed. */ }
        }
    }

    override fun setCurrency(currency: String) { if (currency in Money.currencies) preference("currency", currency) }
    override fun setOnboardingStep(step: Int) = preference("onboardingStep", step.coerceAtLeast(0).toString())
    override fun finishOnboarding() = preference("onboarded", "true")
    override fun setCapturePaused(paused: Boolean) = preference("paused", paused.toString())
    override fun setConfirmations(enabled: Boolean) = preference("confirmations", enabled.toString())
    override fun selectSource(sourceId: String, selected: Boolean) = preference("source:$sourceId", selected.toString())
    override fun refreshCapture() { perform { } }
    override fun openCaptureSettings() = platform.openCaptureSettings()
    override fun requestNotificationPermission() = platform.requestNotificationPermission()
    override fun clearError() { mutableState.value = mutableState.value.copy(error = null) }
    fun focusEntry(id: String) { focusedId = id; mutableState.value = mutableState.value.copy(requestedEntryId = id) }
    override fun clearFocus() { focusedId = null; mutableState.value = mutableState.value.copy(requestedEntryId = null) }
    fun isSelected(sourceId: String): Boolean = preferences["source:$sourceId"] == "true"

    override fun exportCsv(): String? = try {
        val header = "id,date,amount,currency,direction,kind,status,counterparty,account,note,source"
        fun cell(value: String): String {
            // Avoid spreadsheet formula execution when opening untrusted notification-derived text.
            val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
            return "\"${safe.replace("\"", "\"\"")}\""
        }
        val lines = state.value.entries.filterNot { it.deleted }.map { e ->
            listOf(e.id, e.date, Money.decimal(e.amountMinor, e.currency), e.currency, e.direction.name,
                e.kind.name, e.status.name, e.counterparty, e.account, e.note, e.source).joinToString(",", transform = ::cell)
        }
        platform.exportCsv((listOf(header) + lines).joinToString("\r\n"))
    } catch (_: Exception) { "Could not export the ledger" }
}

internal fun validDate(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    val year = value.take(4).toInt(); val month = value.substring(5, 7).toInt(); val day = value.takeLast(2).toInt()
    if (year !in 1900..2200 || month !in 1..12) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    return day in 1..days[month - 1]
}
