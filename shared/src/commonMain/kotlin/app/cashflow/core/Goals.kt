package app.cashflow.core

import kotlinx.serialization.Serializable

@Serializable enum class GoalType { SPENDING_LIMIT, SAVING_TARGET }
@Serializable enum class GoalPeriod { WEEKLY, MONTHLY, CUSTOM }
enum class GoalState { ON_TRACK, CLOSE, OVER, REACHED, ENDED }

/** A goal stores only its definition. Progress is always derived from the ledger. */
@Serializable
data class Goal(
    val id: String,
    val name: String,
    val type: GoalType,
    val amountMinor: Long,
    val currency: String,
    val period: GoalPeriod = GoalPeriod.MONTHLY,
    val customStart: String = "",
    val customEnd: String = "",
    /** Spending limits only: count payments whose counterparty contains this text. Blank means all spending. */
    val counterparty: String = "",
    /** Saving targets only: limit to these savings accounts. Empty means all of them. */
    val accounts: List<String> = emptyList(),
    val archived: Boolean = false,
)

data class GoalProgress(
    val goal: Goal,
    val from: String,
    val to: String,
    /** Spent for a limit, saved (net) for a target. */
    val amountMinor: Long,
    val remainingMinor: Long,
    val fraction: Double,
    val daysLeft: Int,
    /** Per day: what is still allowed for a limit, or still needed for a target. */
    val perDayMinor: Long,
    val state: GoalState,
)

/** Outgoing completed money that is not a transfer, within the inclusive date range. */
internal fun spending(entries: List<Entry>, currency: String, from: String, to: String, counterparty: String = ""): Long {
    val scope = norm(counterparty)
    var total = 0L
    var refunded = 0L
    for (e in entries) {
        if (e.deleted || e.currency != currency || e.status != PaymentStatus.COMPLETED || e.kind == Kind.TRANSFER) continue
        if (e.date < from || e.date > to) continue
        if (e.direction == Direction.OUT && (scope.isEmpty() || norm(e.counterparty).contains(scope))) total = saturatingAdd(total, e.amountMinor)
        // Refunds can only be tied to a specific payment by the user, so they reduce unscoped totals only.
        if (e.direction == Direction.IN && e.kind == Kind.REFUND && scope.isEmpty()) refunded = saturatingAdd(refunded, e.amountMinor)
    }
    return (total - refunded).coerceAtLeast(0)
}

object Goals {
    /** Inclusive day range the goal covers around [today]. Null when a custom range is unreadable. */
    fun range(goal: Goal, today: Long): Pair<Long, Long>? = when (goal.period) {
        GoalPeriod.WEEKLY -> Days.weekStart(today).let { it to it + 6 }
        GoalPeriod.MONTHLY -> Days.monthStart(today) to Days.monthEnd(today)
        GoalPeriod.CUSTOM -> {
            val start = Days.number(goal.customStart)
            val end = Days.number(goal.customEnd)
            if (start == null || end == null || end < start) null else start to end
        }
    }

    fun progress(goal: Goal, entries: List<Entry>, accounts: List<Account>, todayDate: String): GoalProgress? {
        val today = Days.number(todayDate) ?: return null
        val (start, end) = range(goal, today) ?: return null
        val from = Days.date(start)
        val to = Days.date(end)
        val amount = when (goal.type) {
            GoalType.SPENDING_LIMIT -> spending(entries, goal.currency, from, to, goal.counterparty)
            GoalType.SAVING_TARGET -> savings(entries, accounts, goal.currency, from, to, goal.accounts.takeIf { it.isNotEmpty() }?.toSet()).net.coerceAtLeast(0)
        }
        val remaining = (goal.amountMinor - amount).coerceAtLeast(0)
        val daysLeft = (end - today + 1).coerceAtLeast(0).toInt()
        val fraction = amount.toDouble() / goal.amountMinor.toDouble()
        val over = today > end
        val state = when (goal.type) {
            GoalType.SPENDING_LIMIT -> when {
                amount > goal.amountMinor -> GoalState.OVER
                amount >= goal.amountMinor - goal.amountMinor / 5 -> GoalState.CLOSE
                over -> GoalState.ENDED
                else -> GoalState.ON_TRACK
            }
            GoalType.SAVING_TARGET -> {
                val elapsed = ((today.coerceAtMost(end) - start + 1).toDouble() / (end - start + 1).toDouble())
                when {
                    amount >= goal.amountMinor -> GoalState.REACHED
                    over -> GoalState.ENDED
                    elapsed >= 0.25 && fraction < 0.8 * elapsed -> GoalState.CLOSE
                    else -> GoalState.ON_TRACK
                }
            }
        }
        val perDay = if (daysLeft > 0) remaining / daysLeft else 0
        return GoalProgress(goal, from, to, amount, remaining, fraction, daysLeft, perDay, state)
    }
}
