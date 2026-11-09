package app.cashflow.core

import kotlinx.serialization.Serializable

/**
 * A template the user (or the user plus the AI) has already classified. Rules are per source app and only
 * settle direction and kind. Amount validation, OTP rejection and reversal handling in [AlertParser] always run first.
 */
@Serializable
data class LearnedRule(
    val id: String,
    val source: String,
    val words: List<String>,
    val sample: String,
    val direction: Direction,
    val kind: Kind = Kind.UNCLASSIFIED,
    /** Consistent user confirmations. A rule auto-records only from [Learned.AUTO_AT]. */
    val confirmations: Int = 1,
    val uses: Int = 0,
    /** "user" when corrected by hand, "ai" when the user confirmed an AI suggestion. */
    val origin: String = "user",
    val createdAt: String = "",
)

object Learned {
    const val AUTO_AT = 2
    const val REASON_HINT = "This looks like an alert you corrected before. Please confirm it."
    const val MATCH = 0.75
    const val MAX_RULES = 200
    private val greeting = Regex("\\b(?:hi|hello|dear)\\s+[\\p{L}' .-]{1,60}?(?=[,!:\\n]|$)")
    private val numbers = Regex("[0-9][0-9,.]*")
    private val word = Regex("[\\p{L}]{2,}")

    /** Reasons that mean "the amount is fine but the meaning needs a human". Everything else is never overridden. */
    val confirmable = setOf(AlertParser.REASON_DIRECTION, AlertParser.REASON_CONFIRM, REASON_HINT)

    /** The words that make a template, with the greeting name and every number removed. */
    fun words(text: String): List<String> {
        val cleaned = numbers.replace(greeting.replace(text.lowercase(), " "), " ")
        return word.findAll(cleaned).map { it.value }.distinct().sorted().take(60).toList()
    }

    fun sample(text: String): String = numbers.replace(text.replace('\n', ' '), "#").trim().take(90)

    fun similarity(a: Collection<String>, b: Collection<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val x = a.toSet(); val y = b.toSet()
        return x.intersect(y).size.toDouble() / x.union(y).size
    }

    fun best(rules: List<LearnedRule>, source: String, words: List<String>): LearnedRule? =
        rules.filter { it.source == source }
            .map { it to similarity(it.words, words) }
            .filter { it.second >= MATCH }
            .maxByOrNull { it.second }?.first

    /** Fold one reviewed alert into the rules. [aiAgreed] means the user recorded exactly what the AI suggested. */
    fun learn(
        rules: List<LearnedRule>, source: String, text: String, direction: Direction, kind: Kind,
        aiAgreed: Boolean, id: String, now: String,
    ): List<LearnedRule> {
        val words = words(text)
        if (words.size < 4) return rules
        val learnedKind = kind.takeIf { it == Kind.INCOME || it == Kind.EXPENSE || it == Kind.FEE } ?: Kind.UNCLASSIFIED
        val match = best(rules, source, words)
        val start = if (aiAgreed) AUTO_AT else 1
        if (match == null) {
            val fresh = LearnedRule(id, source, words, sample(text), direction, learnedKind, start, 0, if (aiAgreed) "ai" else "user", now)
            return (rules + fresh).takeLast(MAX_RULES)
        }
        val updated = if (match.direction == direction && (learnedKind == Kind.UNCLASSIFIED || match.kind == learnedKind || match.kind == Kind.UNCLASSIFIED)) {
            match.copy(confirmations = maxOf(match.confirmations + 1, start), kind = if (learnedKind != Kind.UNCLASSIFIED) learnedKind else match.kind)
        } else {
            // The user contradicted the rule: trust the newest answer, but make it earn auto-recording again.
            match.copy(direction = direction, kind = learnedKind, confirmations = start)
        }
        return rules.map { if (it.id == match.id) updated else it }
    }
}
