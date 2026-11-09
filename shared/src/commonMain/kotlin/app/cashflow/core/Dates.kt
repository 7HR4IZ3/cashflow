package app.cashflow.core

/** Calendar maths over the ISO strings the ledger stores. No system clock is read here. */
internal object Days {
    private val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
    private val stamp = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(Z|[+-]\d{2}:?\d{2})?""")

    /** Days since 1970-01-01, or null when the text is not a real date. */
    fun number(date: String): Long? {
        if (!iso.matches(date) || !validDate(date)) return null
        return civilToDays(date.take(4).toLong(), date.substring(5, 7).toLong(), date.takeLast(2).toLong())
    }

    fun date(day: Long): String {
        val (y, m, d) = civil(day)
        return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    /** 0 = Monday. 1970-01-01 was a Thursday. */
    fun weekday(day: Long): Int = (((day + 3) % 7) + 7).toInt() % 7

    fun weekStart(day: Long): Long = day - weekday(day)

    fun monthStart(day: Long): Long {
        val (y, m, _) = civil(day)
        return civilToDays(y.toLong(), m.toLong(), 1)
    }

    fun monthEnd(day: Long): Long {
        val (y, m, _) = civil(day)
        return if (m == 12) civilToDays(y + 1L, 1, 1) - 1 else civilToDays(y.toLong(), m + 1L, 1) - 1
    }

    fun dayOfMonth(day: Long): Int = civil(day).third

    /** Seconds since the epoch for an ISO-8601 timestamp, or null. A missing zone is read as UTC. */
    fun epochSeconds(timestamp: String): Long? {
        val m = stamp.find(timestamp.trim()) ?: return null
        val g = m.groupValues
        val day = number("${g[1]}-${g[2]}-${g[3]}") ?: return null
        val zone = g[7]
        val offset = if (zone.isEmpty() || zone == "Z") 0L else {
            val digits = zone.drop(1).replace(":", "")
            val seconds = digits.take(2).toLong() * 3600 + digits.drop(2).toLong() * 60
            if (zone.startsWith("-")) -seconds else seconds
        }
        return day * 86400 + g[4].toLong() * 3600 + g[5].toLong() * 60 + g[6].toLong() - offset
    }

    /** Local hour as written in the timestamp (platforms supply their local offset). */
    fun hour(timestamp: String): Int? = stamp.find(timestamp.trim())?.groupValues?.get(4)?.toIntOrNull()

    private fun civil(days: Long): Triple<Int, Int, Int> {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        return Triple((yoe + era * 400 + (if (m <= 2) 1 else 0)).toInt(), m.toInt(), d.toInt())
    }

    private fun civilToDays(y: Long, m: Long, d: Long): Long {
        val year = if (m <= 2) y - 1 else y
        val era = (if (year >= 0) year else year - 399) / 400
        val yoe = year - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        return era * 146097 + yoe * 365 + yoe / 4 - yoe / 100 + doy - 719468
    }
}
