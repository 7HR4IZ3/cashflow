package app.cashflow.core

import kotlinx.serialization.Serializable

enum class Direction { IN, OUT }
/** How the home chart draws money in and out. The choice is kept per device. */
enum class ChartStyle(val label: String) { LINES("Lines"), PULSE("Pulse"), BARS("Bars") }

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
    /** When Cashflow recorded this entry (ISO timestamp). Used to pair the two sides of a transfer. */
    val capturedAt: String = "",
    /** Shared by the two legs of a transfer between the user's own accounts. */
    val transferGroupId: String? = null,
    /** For transfers: the user's account on the other side, when known. */
    val counterAccount: String = "",
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
    val directionConfirmed: Boolean = true,
)

@Serializable
data class ReviewItem(
    val id: String,
    val source: String,
    val text: String,
    val reason: String,
    val draft: EntryDraft,
    val aiSuggestion: AiSuggestion? = null,
)

data class SourceApp(val id: String, val name: String, val selected: Boolean)
data class CaptureState(
    val platform: String,
    val accessGranted: Boolean = false,
    val lastReceived: String? = null,
    val sources: List<SourceApp> = emptyList(),
)

data class CashflowState(
    val subscription: SubscriptionState = SubscriptionState(),
    val aiEnabled: Boolean = false,
    val aiAutomatic: Boolean = false,
    val aiBusyIds: Set<String> = emptySet(),
    val entries: List<Entry> = emptyList(),
    val review: List<ReviewItem> = emptyList(),
    val ignoredReview: List<ReviewItem> = emptyList(),
    val today: String = "",
    val onboardingComplete: Boolean = false,
    val onboardingStep: Int = 0,
    val currency: String = "NGN",
    val capturePaused: Boolean = false,
    val captureOverLimit: Boolean = false,
    val confirmations: Boolean = true,
    val capture: CaptureState = CaptureState("Android"),
    val error: String? = null,
    val requestedEntryId: String? = null,
    val accounts: List<Account> = emptyList(),
    val myNames: List<String> = emptyList(),
    val transferSuggestions: List<TransferSuggestion> = emptyList(),
    val goals: List<GoalProgress> = emptyList(),
    val insights: Insights = Insights(null, null, emptyList()),
    val notify: Map<NudgeCategory, Boolean> = NudgeCategory.defaults,
    val transferWindowMinutes: Int = 10,
    val learnedRules: List<LearnedRule> = emptyList(),
    val chartStyle: ChartStyle = ChartStyle.LINES,
)

/** Local notification kinds beyond the per-transaction "recorded" alert, each switchable on its own. */
enum class NudgeCategory(val label: String, val defaultOn: Boolean) {
    CREDIT("Money in", true), GOAL("Goals", true), HABIT("Spending habits", false),
    SUMMARY("Weekly and monthly summary", true), REMINDER("Reminders", true);
    companion object { val defaults: Map<NudgeCategory, Boolean> = entries.associateWith { it.defaultOn } }
}

data class Nudge(val id: String, val category: NudgeCategory, val title: String, val text: String)

/** Delivers a [Nudge] to the user. Platforms register one; with none registered nothing is marked sent. */
fun interface NudgeSink { fun deliver(nudge: Nudge) }

/** Synchronous UI boundary. Mutations return null on success, otherwise a user-visible error. */
interface CashflowActions {
    fun save(draft: EntryDraft, entryId: String? = null, reviewId: String? = null): String?
    fun delete(entryId: String): String?
    fun restore(entryId: String): String?
    fun ignoreReview(reviewId: String): String?
    fun restoreReview(reviewId: String): String?
    fun captureText(text: String, source: String = "Pasted text"): String?
    fun setCurrency(currency: String)
    fun setOnboardingStep(step: Int)
    fun finishOnboarding()
    fun setCapturePaused(paused: Boolean)
    fun setConfirmations(enabled: Boolean)
    fun selectSource(sourceId: String, selected: Boolean)
    fun refreshCapture()
    fun openCaptureSettings()
    fun installShortcut() { }
    fun showSetupGuide(step: Int) { }
    fun setupGuideVisible(visible: Boolean) { }
    fun requestNotificationPermission()
    fun exportCsv(): String?
    fun clearError()
    fun clearFocus() { }
    fun setAiEnabled(enabled: Boolean) { }
    fun setAutomaticAi(enabled: Boolean) { }
    fun requestAi(reviewId: String): String? = "AI help is unavailable on this platform"
    fun setChartStyle(style: ChartStyle) { }
    fun deleteLearnedRule(ruleId: String) { }
    fun clearLearnedRules() { }
    fun refreshSubscription() { }
    fun purchase(offerId: String) { }
    fun manageSubscription() { }
    fun saveAccount(account: Account): String? = null
    fun setAccountArchived(accountId: String, archived: Boolean): String? = null
    fun setMyNames(names: List<String>) { }
    fun answerTransfer(suggestionId: String, mine: Boolean): String? = null
    fun linkTransfer(entryId: String, otherEntryId: String): String? = null
    fun markTransfer(entryId: String, counterAccount: String): String? = null
    fun unlinkTransfer(entryId: String): String? = null
    fun saveGoal(goal: Goal): String? = null
    fun archiveGoal(goalId: String): String? = null
    fun setNotify(category: NudgeCategory, enabled: Boolean) { }
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
