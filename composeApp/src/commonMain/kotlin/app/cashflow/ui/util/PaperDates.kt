package app.cashflow.ui.util

import kotlin.math.abs
/**
 * Calendar helpers over ISO `yyyy-MM-dd` strings, the format the ledger stores.
 *
 * No system clock is readable from shared UI code. The UI therefore never
 * pretends a date is "today": relative Day/Week/Month periods are omitted and
 * figures are shown over all time with the entries' real date labels. Anything
 * unreadable falls back to displaying the raw string rather than inventing a date.
 */
object PaperDates {

    private val isoRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

    /** Day number (days from civil epoch 1970-01-01) for a parsed ISO date, or null. */
    fun dayNumber(date: String): Long? {
        val match = isoRegex.find(date.trim()) ?: return null
        val y = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        val d = match.groupValues[3].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31 || y !in 1800..9999) return null
        return civilToDays(y.toLong(), m.toLong(), d.toLong())
    }

    fun daysToCivil(days: Long): Triple<Int, Int, Int> {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yearOfEra = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yearOfEra + era * 400
        val doy = doe - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        return Triple(y.toInt() + (if (m <= 2) 1 else 0), m.toInt(), d.toInt())
    }

    private fun civilToDays(y: Long, m: Long, d: Long): Long {
        var year = y
        if (m <= 2) year -= 1
        val era = (if (year >= 0) year else year - 399) / 400
        val yoe = year - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** True when the date is inside the anchor's calendar month. */
    fun sameCalendarMonth(lhs: Long, rhs: Long): Boolean {
        val (ay, am, _) = daysToCivil(lhs)
        val (by, bm, _) = daysToCivil(rhs)
        return ay == by && am == bm
    }

    fun sameDay(lhs: Long, rhs: Long): Boolean = lhs == rhs

    fun addDays(day: Long, amount: Int): Long = day + amount

    /** Weekday name for a day number. */
    fun weekdayName(day: Long): String {
        // Civil epoch day 0 (1970-01-01) was a Thursday: day + 3 gives Mon=0..Sun=6.
        val mondayIndex = ((day + 3) % 7 + 7) % 7
        return names[mondayIndex.toInt()]
    }

    private val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun monthName(month: Int): String = when (month) {
        1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
        5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
        9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
        else -> "?"
    }

    fun monthShort(month: Int): String = when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> "?"
    }

    fun yearMonth(day: Long): Pair<Int, Int> {
        val (y, m, _) = daysToCivil(day)
        return y to m
    }

    fun monthStartDay(year: Int, month: Int): Long = civilToDays(year.toLong(), month.toLong(), 1)

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (isLeap(year)) 29 else 28
    }

    private fun isLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** Leading blank cells offset: 1st of the month falls in column (Mon=0..Sun=6). */
    fun firstWeekdayColumn(day: Long): Int {
        val mondayIndex = ((day + 3) % 7 + 7) % 7
        return mondayIndex.toInt()
    }

    /**
     * Human row header for a date string: "Wed 21 Oct" when readable, else the
     * raw ledger text (never a fabricated date).
     */
    fun dateLabel(date: String): String {
        val day = dayNumber(date) ?: return date.ifBlank { "Unknown date" }
        val (y, m, d) = daysToCivil(day)
        return "${weekdayName(day)} $d ${monthShort(m)}"
    }

    /** Strip an optional time tail: "2026-10-25T19:04" -> "2026-10-25". */
    fun isoDateOnly(value: String): String {
        val m = isoRegex.find(value) ?: return value
        return m.value
    }

    fun describeSpan(anchor: Long, period: Period): String = when (period) {
        Period.DAY -> dateLabel(daysToIso(anchor))
        Period.WEEK -> "week to ${dateLabel(daysToIso(anchor))}"
        Period.MONTH -> {
            val (y, m, _) = daysToCivil(anchor)
            "${monthShort(m)} $y"
        }
        Period.ALL -> "all time"
    }

    fun daysToIso(day: Long): String {
        val (y, m, d) = daysToCivil(day)
        return y.toString().padStart(4, '0') + "-" + m.toString().padStart(2, '0') + "-" + d.toString().padStart(2, '0')
    }
}

enum class Period { DAY, WEEK, MONTH, ALL }
