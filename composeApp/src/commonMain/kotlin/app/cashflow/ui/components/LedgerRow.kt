package app.cashflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.core.Entry
import app.cashflow.core.Kind
import app.cashflow.core.Money
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.MoneyWords

/**
 * LedgerRow, the book's one row component. Counterparty and amount share the
 * lead line across the full available width; account, status stamp and origin
 * glyph sit on their own line; kind stamps wrap on a full-width line beneath so
 * they are never squeezed between metadata and money. Rows push, never truncate
 * amounts. One tap opens the record detail. The row's merged accessibility
 * label reads the whole record in ledger order.
 */
@Composable
fun LedgerRow(
    entry: Entry,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    currencyVisible: Boolean = false,
) {
    val c = LocalCashflowColors.current
    val spoken = buildString {
        append(MoneyWords.directionVerb(entry.direction).replaceFirstChar { it.uppercase() })
        append(' ')
        append(Money.display(entry.amountMinor, entry.currency))
        append(' ')
        append(MoneyWords.currencyName(entry.currency))
        entry.counterparty.takeIf { it.isNotBlank() }?.let { appended ->
            val rel = if (entry.direction == app.cashflow.core.Direction.IN) "from" else "to"
            append(". $rel ${entry.counterparty}")
        }
        append(". ")
        append(MoneyWords.status(entry.status))
        append(". ")
        append(MoneyWords.origin(entry.source))
        if (entry.deleted) append(". In recently deleted")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.raise)
            .border(0.5.dp, c.line, RoundedCornerShape(16.dp))
            .let { m -> if (onClick != null) m.clickable { onClick() } else m }
            .semantics { contentDescription = spoken }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Lead line: the name gives way so the money keeps its full exact width.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.counterparty.ifBlank { MoneyWords.kind(entry.kind) },
                modifier = Modifier.weight(1f),
                style = CamelType.VoiceBody,
                color = c.ink,
            )
            Spacer(Modifier.width(8.dp))
            if (currencyVisible) CurrencyTag(entry.currency)
            Spacer(Modifier.width(10.dp))
            trailing?.invoke() ?: AmountRow(
                entry = entry,
                showKindCaption = !kindStampShown(entry.kind),
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.account,
                style = CamelType.LedgerBody,
                color = c.ink3,
            )
            Spacer(Modifier.width(8.dp))
            StatusStamp(entry.status)
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = originIcon(entry.source),
                contentDescription = MoneyWords.origin(entry.source),
                modifier = Modifier.size(14.dp),
                tint = c.ink3,
            )
        }
        KindStampsLine(entry.kind)
    }
}

/**
 * Amount column for a row: direction glyph and amount together so direction is
 * never colour-carried alone; unclassified movements read as their own stamp.
 * [showKindCaption] silences the "not yet classified" caption when the same
 * kind word is already visible elsewhere on the row.
 */
@Composable
fun AmountRow(entry: Entry, modifier: Modifier = Modifier, showKindCaption: Boolean = true) {
    val c = LocalCashflowColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DirectionGlyph(entry.direction, size = 22.dp)
            Spacer(Modifier.width(6.dp))
            AmountText(
                minor = entry.amountMinor,
                currency = entry.currency,
                direction = entry.direction,
            )
        }
        if (entry.kind == Kind.UNCLASSIFIED && showKindCaption) {
            Text(
                text = "not yet classified",
                style = CamelType.LedgerLabel,
                color = c.violet,
            )
        }
    }
}

/** True when the row itself carries this kind's word on a stamp. */
private fun kindStampShown(kind: Kind): Boolean =
    kind == Kind.TRANSFER || kind == Kind.REFUND || kind == Kind.FEE || kind == Kind.UNCLASSIFIED

/**
 * Kind stamps on their own full-width wrapping line beneath the metadata, so a
 * long stamp never squeezes between money and metadata. Transfer, refund, fee
 * and unclassified keep their word on the row itself.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KindStampsLine(kind: Kind) {
    if (kindStampShown(kind)) {
        Spacer(Modifier.height(4.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            KindStamp(kind)
        }
    }
}

@Composable
private fun CurrencyTag(code: String) {
    val c = LocalCashflowColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(c.lineSoft)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(text = code, style = CamelType.LedgerLabel, color = c.ink3)
    }
}

private fun originIcon(source: String): ImageVector =
    if (source.equals("Manual", ignoreCase = true)) {
        Icons.Outlined.Edit
    } else {
        Icons.Outlined.Notifications
    }

/** Quiet shared hairline divider, part of the row rhythm. */
@Composable
fun RowRule(modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.lineSoft),
    )
}
