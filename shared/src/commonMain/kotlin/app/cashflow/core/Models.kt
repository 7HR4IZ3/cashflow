package app.cashflow.core

import kotlinx.serialization.Serializable

enum class Direction { IN, OUT }
enum class Kind { INCOME, EXPENSE, TRANSFER, REFUND, FEE, UNCLASSIFIED }
enum class PaymentStatus { COMPLETED, PENDING, FAILED }

@Serializable
data class Entry(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val direction: Direction,
    val kind: Kind,
    val status: PaymentStatus,
    val counterparty: String,
    val account: String = "Unassigned",
    val note: String = "",
    val date: String,
    val source: String = "Manual",
    val deleted: Boolean = false,
    val userEdited: Boolean = false,
    val revision: Long = 1,
)

@Serializable
data class EntryDraft(
    val amount: String = "",
    val currency: String = "NGN",
    val direction: Direction = Direction.OUT,
    val kind: Kind = Kind.EXPENSE,
    val status: PaymentStatus = PaymentStatus.COMPLETED,
    val counterparty: String = "",
    val account: String = "Unassigned",
    val note: String = "",
    val date: String = "",
)

@Serializable
data class ReviewItem(
    val id: String,
    val source: String,
    val text: String,
    val reason: String,
    val draft: EntryDraft,
)

data class SourceApp(val id: String, val name: String, val selected: Boolean)
data class CaptureState(
    val platform: String,
    val accessGranted: Boolean = false,
    val lastReceived: String? = null,
    val sources: List<SourceApp> = emptyList(),
)

data class CashflowState(
    val entries: List<Entry> = emptyList(),
    val review: List<ReviewItem> = emptyList(),
    val onboardingComplete: Boolean = false,
    val onboardingStep: Int = 0,
    val currency: String = "NGN",
    val capturePaused: Boolean = false,
    val confirmations: Boolean = true,
    val capture: CaptureState = CaptureState("Android"),
    val error: String? = null,
    val requestedEntryId: String? = null,
)

/** Synchronous UI boundary. Mutations return null on success, otherwise a user-visible error. */
interface CashflowActions {
    fun save(draft: EntryDraft, entryId: String? = null, reviewId: String? = null): String?
    fun delete(entryId: String): String?
    fun restore(entryId: String): String?
    fun ignoreReview(reviewId: String): String?
    fun captureText(text: String, source: String = "Pasted text"): String?
    fun setCurrency(currency: String)
    fun setOnboardingStep(step: Int)
    fun finishOnboarding()
    fun setCapturePaused(paused: Boolean)
    fun setConfirmations(enabled: Boolean)
    fun selectSource(sourceId: String, selected: Boolean)
    fun refreshCapture()
    fun openCaptureSettings()
    fun requestNotificationPermission()
    fun exportCsv(): String?
    fun clearError()
    fun clearFocus() { }
}

data class Totals(val incoming: Long, val outgoing: Long) {
    val net: Long get() = incoming - outgoing
}

fun totals(entries: List<Entry>, currency: String): Totals {
    val eligible = entries.filter {
        !it.deleted && it.currency == currency && it.status == PaymentStatus.COMPLETED && it.kind != Kind.TRANSFER
    }
    return Totals(
        eligible.filter { it.direction == Direction.IN }.fold(0L) { sum, e -> checkedAdd(sum, e.amountMinor) },
        eligible.filter { it.direction == Direction.OUT }.fold(0L) { sum, e -> checkedAdd(sum, e.amountMinor) },
    )
}

private fun checkedAdd(a: Long, b: Long): Long {
    require(b >= 0 && a <= Long.MAX_VALUE - b) { "Amount total exceeds supported range" }
    return a + b
}

object Money {
    val currencies = listOf("NGN", "USD", "GBP", "EUR", "CAD", "GHS", "KES", "ZAR", "INR", "JPY", "KWD")
    fun exponent(currency: String): Int = when (currency) { "JPY" -> 0; "KWD" -> 3; else -> 2 }
    fun parse(value: String, currency: String): Long? {
        if (currency !in currencies) return null
        val clean = value.trim()
        if (!Regex("[0-9]+(?:\\.[0-9]+)?").matches(clean)) return null
        val parts = clean.split('.')
        val exp = exponent(currency)
        if (parts.getOrElse(1) { "" }.length > exp) return null
        return (parts[0] + parts.getOrElse(1) { "" }.padEnd(exp, '0')).toLongOrNull()?.takeIf { it > 0 }
    }
    fun decimal(minor: Long, currency: String): String {
        val exp = exponent(currency)
        val digits = minor.toString().padStart(exp + 1, '0')
        return if (exp == 0) digits else digits.dropLast(exp) + "." + digits.takeLast(exp)
    }
    fun display(minor: Long, currency: String): String {
        val value = decimal(minor, currency)
        val pieces = value.split('.')
        val whole = pieces[0].reversed().chunked(3).joinToString(",").reversed()
        return "$currency $whole" + if (pieces.size > 1) ".${pieces[1]}" else ""
    }
}
