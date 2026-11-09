package app.cashflow.core

import kotlinx.serialization.Serializable

@Serializable
enum class AccountRole { SPENDING, SAVINGS, OTHER }

/** One of the user's own banks or wallets. Entries refer to it by [name], which is unique ignoring case. */
@Serializable
data class Account(
    val id: String,
    val name: String,
    val role: AccountRole = AccountRole.SPENDING,
    /** Watched-app IDs or text fragments (bank name, last digits) used to match alerts to this account. */
    val hints: List<String> = emptyList(),
    val archived: Boolean = false,
)

/** Two entries that look like both sides of one move between the user's accounts. Needs a yes or no. */
@Serializable
data class TransferSuggestion(val id: String, val firstId: String, val secondId: String)

data class Savings(val saved: Long, val withdrawn: Long) {
    val net: Long get() = saved - withdrawn
}

internal fun norm(value: String): String = value.trim().lowercase()
internal fun isNamed(account: String): Boolean = account.isNotBlank() && !account.equals("Unassigned", ignoreCase = true)

private fun wholeWord(haystack: String, needle: String): Boolean =
    needle.length >= 3 && Regex("(^|\\W)${Regex.escape(needle)}(\\W|$)").containsMatchIn(haystack)

/**
 * Money moved into savings accounts minus money taken back out, over completed transfers.
 * A linked pair counts once, through its outgoing leg. [only] limits the result to named savings accounts.
 */
fun savings(
    entries: List<Entry>, accounts: List<Account>, currency: String,
    from: String? = null, to: String? = null, only: Set<String>? = null,
): Savings {
    val roles = accounts.associate { norm(it.name) to it.role }
    val wanted = only?.map(::norm)?.toSet()
    var saved = 0L
    var withdrawn = 0L
    for (e in entries) {
        if (e.deleted || e.currency != currency || e.status != PaymentStatus.COMPLETED || e.kind != Kind.TRANSFER) continue
        if (from != null && e.date < from) continue
        if (to != null && e.date > to) continue
        if (e.transferGroupId != null && e.direction == Direction.IN) continue
        val (source, destination) = if (e.direction == Direction.OUT) e.account to e.counterAccount else e.counterAccount to e.account
        val sourceRole = roles[norm(source)]
        val destinationRole = roles[norm(destination)]
        if (destinationRole == AccountRole.SAVINGS && sourceRole != AccountRole.SAVINGS && (wanted == null || norm(destination) in wanted)) {
            saved = saturatingAdd(saved, e.amountMinor)
        }
        if (sourceRole == AccountRole.SAVINGS && destinationRole != AccountRole.SAVINGS && (wanted == null || norm(source) in wanted)) {
            withdrawn = saturatingAdd(withdrawn, e.amountMinor)
        }
    }
    return Savings(saved, withdrawn)
}

internal fun saturatingAdd(a: Long, b: Long): Long = if (b > 0 && a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b

/** What the user has told Cashflow about their own money, used to decide when a payment is really a transfer. */
internal class OwnershipContext(
    val accounts: List<Account>,
    val myNames: List<String>,
    val mine: Set<String>,
    val notMine: Set<String>,
)

internal data class OwnMatch(val account: String?)

internal object Transfers {
    /** The one account whose hints appear in the alert source or text. Ambiguity matches nothing. */
    fun matchAccount(accounts: List<Account>, source: String, text: String): Account? {
        val haystacks = listOf(norm(source), norm(text))
        val hits = accounts.filter { account ->
            !account.archived && account.hints.any { hint ->
                val h = norm(hint)
                h.length >= 3 && haystacks.any { it.contains(h) }
            }
        }
        return hits.singleOrNull()
    }

    /** Whether this counterparty is the user themselves or one of their other accounts. */
    fun ownCounterparty(context: OwnershipContext, counterparty: String, ownAccount: String): OwnMatch? {
        val cp = norm(counterparty)
        if (cp.isEmpty() || cp in context.notMine) return null
        context.accounts.firstOrNull { account ->
            !account.archived && !account.name.equals(ownAccount, ignoreCase = true) &&
                (wholeWord(cp, norm(account.name)) || account.hints.any { norm(it) == cp })
        }?.let { return OwnMatch(it.name) }
        if (context.myNames.any { wholeWord(cp, norm(it)) }) return OwnMatch(null)
        if (cp in context.mine) return OwnMatch(null)
        return null
    }

    /** Opposite, equal, unlinked completed entries from a different account inside the window. */
    fun counterparts(entry: Entry, entries: List<Entry>, windowSeconds: Long): List<Entry> {
        val at = Days.epochSeconds(entry.capturedAt) ?: return emptyList()
        return entries.filter { other ->
            other.id != entry.id && !other.deleted && other.status == PaymentStatus.COMPLETED &&
                other.transferGroupId == null && other.currency == entry.currency &&
                other.amountMinor == entry.amountMinor && other.direction != entry.direction &&
                differentAccounts(entry, other) &&
                Days.epochSeconds(other.capturedAt)?.let { kotlin.math.abs(it - at) <= windowSeconds } == true
        }
    }

    private fun differentAccounts(a: Entry, b: Entry): Boolean = when {
        isNamed(a.account) && isNamed(b.account) -> !a.account.equals(b.account, ignoreCase = true)
        isNamed(a.account) != isNamed(b.account) -> true
        else -> a.source != b.source
    }

    /** Remembered answers are keyed by the pair of named accounts, in either order. */
    fun pairKey(a: String, b: String): String? =
        if (isNamed(a) && isNamed(b)) listOf(norm(a), norm(b)).sorted().joinToString("|") else null
}
