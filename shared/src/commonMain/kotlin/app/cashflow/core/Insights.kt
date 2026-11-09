package app.cashflow.core

import kotlin.math.abs
import kotlin.math.roundToInt

enum class SignalKind { LARGE_DEBIT, FREQUENT_SMALL, SPENDING_PACE, NEW_RECURRING, SAVING_STREAK }

/** A pattern worth mentioning. [key] is stable for the same observation so it can be cooled down. */
data class Signal(val key: String, val kind: SignalKind, val positive: Boolean?, val text: String)

data class PeriodSummary(
    val from: String, val to: String, val incoming: Long, val outgoing: Long,
    val saved: Long, val withdrawn: Long, val topCounterparty: String?,
)

data class Insights(val month: PeriodSummary?, val week: PeriodSummary?, val signals: List<Signal>) {
    val headline: Signal? get() = signals.firstOrNull()
}

object InsightEngine {
    fun compute(entries: List<Entry>, accounts: List<Account>, currency: String, todayDate: String): Insights {
        val today = Days.number(todayDate) ?: return Insights(null, null, emptyList())
        val week = summary(entries, accounts, currency, Days.weekStart(today), today)
        val month = summary(entries, accounts, currency, Days.monthStart(today), today)
        val signals = listOfNotNull(
            largeDebit(entries, currency, today), frequentSmall(entries, currency, todayDate, today),
            pace(entries, currency, today), recurring(entries, currency, today), streak(entries, accounts, currency, today),
        )
        return Insights(month, week, signals)
    }

    fun summary(entries: List<Entry>, accounts: List<Account>, currency: String, fromDay: Long, toDay: Long): PeriodSummary {
        val from = Days.date(fromDay)
        val to = Days.date(toDay)
        val inRange = entries.filter { it.date >= from && it.date <= to }
        val totals = runCatching { totals(inRange, currency) }.getOrDefault(Totals(0, 0))
        val moved = savings(entries, accounts, currency, from, to)
        val top = inRange.filter {
            !it.deleted && it.currency == currency && it.status == PaymentStatus.COMPLETED &&
                it.kind != Kind.TRANSFER && it.direction == Direction.OUT && it.counterparty.isNotBlank()
        }.groupBy { norm(it.counterparty) }.maxByOrNull { (_, group) -> group.sumOf { it.amountMinor.coerceAtMost(Long.MAX_VALUE / 4096) } }
            ?.value?.first()?.counterparty
        return PeriodSummary(from, to, totals.incoming, totals.outgoing, moved.saved, moved.withdrawn, top)
    }

    private fun debits(entries: List<Entry>, currency: String) = entries.filter {
        !it.deleted && it.currency == currency && it.status == PaymentStatus.COMPLETED &&
            it.kind != Kind.TRANSFER && it.direction == Direction.OUT
    }

    private fun median(values: List<Long>): Long {
        val sorted = values.sorted()
        return if (sorted.isEmpty()) 0 else sorted[sorted.size / 2]
    }

    private fun largeDebit(entries: List<Entry>, currency: String, today: Long): Signal? {
        val all = debits(entries, currency)
        val recent = all.filter { Days.number(it.date)?.let { d -> d in (today - 6)..today } == true }
        val history = all.filterNot { it in recent }
        if (history.size < 8) return null
        val usual = median(history.map { it.amountMinor })
        if (usual <= 0) return null
        val big = recent.filter { it.amountMinor > usual * 3 }.maxByOrNull { it.amountMinor } ?: return null
        val times = (big.amountMinor.toDouble() / usual).roundToInt()
        return Signal("large:${big.id}", SignalKind.LARGE_DEBIT, null,
            "A payment of ${Money.display(big.amountMinor, currency)} stands out, about $times times your usual.")
    }

    private fun frequentSmall(entries: List<Entry>, currency: String, todayDate: String, today: Long): Signal? {
        val group = debits(entries, currency).filter { it.date == todayDate && it.counterparty.isNotBlank() }
            .groupBy { norm(it.counterparty) }.filterValues { it.size >= 4 }.maxByOrNull { it.value.size } ?: return null
        return Signal("freq:${group.key}:$today", SignalKind.FREQUENT_SMALL, null,
            "${group.value.size} payments to ${group.value.first().counterparty} today.")
    }

    private fun pace(entries: List<Entry>, currency: String, today: Long): Signal? {
        val start = Days.weekStart(today)
        val elapsed = (today - start + 1).toInt()
        if (elapsed < 3) return null
        val previous = (1..4).map { n ->
            spending(entries, currency, Days.date(start - 7L * n), Days.date(start - 7L * n + 6))
        }
        if (previous.count { it > 0 } < 3) return null
        val expected = median(previous).toDouble() * elapsed / 7.0
        if (expected <= 0) return null
        val current = spending(entries, currency, Days.date(start), Days.date(today)).toDouble()
        val change = (current - expected) / expected
        val percent = (abs(change) * 100).roundToInt()
        return when {
            change <= -0.2 -> Signal("pace:$start:down", SignalKind.SPENDING_PACE, true, "Spending is $percent% lower than your usual week so far.")
            change >= 0.2 -> Signal("pace:$start:up", SignalKind.SPENDING_PACE, false, "Spending is $percent% higher than your usual week so far.")
            else -> null
        }
    }

    private fun recurring(entries: List<Entry>, currency: String, today: Long): Signal? {
        val groups = debits(entries, currency).filter { it.counterparty.isNotBlank() }
            .groupBy { norm(it.counterparty) to it.amountMinor }.filterValues { it.size == 3 }
        for ((key, group) in groups) {
            val days = group.mapNotNull { Days.number(it.date) }.sorted()
            if (days.size != 3 || today - days.last() > 45) continue
            val gaps = days.zipWithNext { a, b -> b - a }
            val label = when {
                gaps.all { it in 6..8 } -> "weekly"
                gaps.all { it in 27..34 } -> "monthly"
                else -> continue
            }
            return Signal("recurring:${key.first}:${key.second}", SignalKind.NEW_RECURRING, null,
                "${group.first().counterparty} looks like a $label payment of ${Money.display(key.second, currency)}.")
        }
        return null
    }

    private fun streak(entries: List<Entry>, accounts: List<Account>, currency: String, today: Long): Signal? {
        val thisWeek = Days.weekStart(today)
        var weeks = 0
        for (n in 1..12) {
            val start = thisWeek - 7L * n
            if (savings(entries, accounts, currency, Days.date(start), Days.date(start + 6)).net > 0) weeks++ else break
        }
        return if (weeks >= 2) Signal("streak:$weeks:$thisWeek", SignalKind.SAVING_STREAK, true, "You've added to savings $weeks weeks in a row.") else null
    }
}
