package app.cashflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cashflow.core.Direction
import app.cashflow.core.Entry
import app.cashflow.core.Kind
import app.cashflow.core.Money
import app.cashflow.core.PaymentStatus
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.MoneyWords

/**
 * FeedRow: the feed's single row geometry — edge-to-edge under a date-group
 * header, hairline-topped, 64dp minimum. Lead column: initial/geometric avatar
 * (the citron dot marks incoming money), name + quiet metadata; the amount
 * leads right full and exact, arrow beside it carrying direction. Statuses
 * appear only when they matter: Unclassified quiet, Pending lavender, Failed
 * red. One tap opens the record detail; the merged accessibility label reads
 * the whole record in ledger order.
 */
@Composable
fun FeedRow(
    entry: Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCurrencyCode: Boolean = false,
) {
    val c = LocalCashflowColors.current
    val isIn = entry.direction == Direction.IN
    val spoken = buildString {
        append(MoneyWords.directionVerb(entry.direction).replaceFirstChar { it.uppercase() })
        append(' ')
        append(Money.display(entry.amountMinor, entry.currency))
        val name = MoneyWords.currencyName(entry.currency)
        append(' ').append(name)
        entry.counterparty.takeIf { it.isNotBlank() }?.let { who ->
            append(". ").append(if (isIn) "From " else "To ").append(who)
        }
        if (entry.status != PaymentStatus.COMPLETED) {
            append(". ").append(MoneyWords.status(entry.status))
        }
        if (entry.kind == Kind.UNCLASSIFIED) {
            append(". Not yet classified")
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(entry = entry)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.counterparty.ifBlank { entry.note.ifBlank { if (isIn) "Money in" else "Money out" } },
                style = CamelType.RowLead,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = feedSubline(entry)
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = CamelType.LedgerBody,
                    color = c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (showCurrencyCode) {
                    Money.display(entry.amountMinor, entry.currency)
                } else {
                    digits(entry.amountMinor, entry.currency)
                },
                style = CamelType.LedgerAmount,
                color = c.ink,
                modifier = Modifier.semantics { contentDescription = Money.display(entry.amountMinor, entry.currency) },
            )
            Spacer(Modifier.width(5.dp))
            Icon(
                imageVector = if (isIn) CFIcons.ArrowIn else CFIcons.ArrowOut,
                contentDescription = null,
                tint = if (isIn) c.citron else c.muted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Grouped digits without the currency prefix; the ISO stays in spoken text. */
private fun digits(minor: Long, currency: String): String =
    Money.display(minor, currency).removePrefix("$currency ")

/**
 * The row's avatar: initials when a name exists, else the Cashflow mark. The
 * bottom-end citron dot rides only on incoming money — a second, non-color
 * cue next to the arrow.
 */
@Composable
private fun Avatar(entry: Entry) {
    val c = LocalCashflowColors.current
    val isIn = entry.direction == Direction.IN
    val initials = avatarInitials(entry.counterparty)
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isIn) c.citronWash else c.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (initials != null) {
            Text(
                text = initials,
                style = CamelType.RowLead,
                color = if (isIn) c.citron else c.ink,
            )
        } else {
            Icon(
                imageVector = CFIcons.Mark,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(17.dp),
            )
        }
        if (isIn) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(12.dp)
                    .background(c.bg, CircleShape)
                    .padding(2.dp)
                    .background(c.citron, CircleShape),
            )
        }
    }
}

/** First letters of the first and last word; single word yields its initial. */
private fun avatarInitials(counterparty: String): String? {
    val words = counterparty.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return null
    val first = words.first().first()
    val last = if (words.size > 1) { words.last().first() } else { null }
    return buildString {
        append(first.uppercaseChar())
        if (last != null) append(last.uppercaseChar())
    }
}

/**
 * Quiet metadata line: real account (not the Unassigned default) and a real
 * note; then only statuses that change the row's meaning.
 */
private fun feedSubline(entry: Entry): String {
    val parts = buildList {
        if (!entry.account.isBlank() && !entry.account.equals("Unassigned", ignoreCase = true)) {
            add(entry.account)
        }
        if (entry.note.isNotBlank()) {
            add(entry.note)
        }
        // Exception statuses only — completed, classified rows stay quiet.
        if (entry.kind == Kind.UNCLASSIFIED) add("Unclassified")
        if (entry.status == PaymentStatus.PENDING) add("Pending")
        if (entry.status == PaymentStatus.FAILED) add("Failed")
    }
    return parts.joinToString(" · ")
}

/** Edge-to-edge feed row separator: one hairline, nothing heavier. */
@Composable
fun RowRule(modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.hairline),
    )
}
