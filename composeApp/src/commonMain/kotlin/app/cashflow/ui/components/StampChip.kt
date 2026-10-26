package app.cashflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Warning
import app.cashflow.core.Kind
import app.cashflow.core.PaymentStatus
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.MoneyWords

/**
 * StampChip: a clerical stamp. Glyph plus word where the plan requires the word
 * (pending, failed, transfer, refund, fee, unclassified always show words), on a
 * low-alpha wash ground, with a one-settle rotation on appearance.
 */
@Composable
fun StampChip(
    label: String,
    glyph: ImageVector,
    tint: Color,
    wash: Color,
    modifier: Modifier = Modifier,
    description: String? = null,
    settle: Boolean = true,
) {
    val rotation = if (settle) rememberStampSettle().value else 0f
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                if (description != null) contentDescription = description
            }
            .background(wash, StampShape)
            .border(0.5.dp, tint.copy(alpha = 0.30f), StampShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .rotate(rotation),
            tint = tint,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 5.dp),
            style = CamelType.LedgerLabel,
            color = tint,
        )
    }
}

private val StampShape = RoundedCornerShape(6.dp)

/*
 * Semantic variants, one glyph mapping per meaning, fixed spoken words.
 */

/** Status stamps. Completed may drop the word; pending and failed never do. */
@Composable
fun StatusStamp(status: PaymentStatus, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    when (status) {
        PaymentStatus.COMPLETED -> Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = MoneyWords.status(status),
            modifier = modifier.size(16.dp),
            tint = c.ink2,
        )
        PaymentStatus.PENDING -> StampChip(
            label = "Pending",
            description = "Pending",
            glyph = Icons.Outlined.Refresh, // pending: not settled yet
            tint = c.amber,
            wash = c.amberWash,
            modifier = modifier,
        )
        PaymentStatus.FAILED -> StampChip(
            label = "Payment failed",
            description = "Payment failed",
            glyph = Icons.Outlined.Warning,
            tint = c.fail,
            wash = c.failWash,
            modifier = modifier,
        )
    }
}

/** Kind stamps: these meanings always carry their word. Income and expense are
 * written as words next to amounts by the rows, no stamp needed. */
@Composable
fun KindStamp(kind: Kind, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    when (kind) {
        Kind.INCOME, Kind.EXPENSE -> Box(modifier)
        Kind.TRANSFER -> StampChip(
            label = "Own transfer",
            description = "Own transfer, excluded from aggregate totals",
            glyph = Icons.Outlined.Refresh,
            tint = c.ink2,
            wash = c.lineSoft,
            modifier = modifier,
        )
        Kind.REFUND -> StampChip(
            label = "Refund",
            description = "Refund",
            glyph = Icons.Outlined.Check,
            tint = c.ink2,
            wash = c.lineSoft,
            modifier = modifier,
        )
        Kind.FEE -> StampChip(
            label = "Fee",
            description = "Fee",
            glyph = Icons.Outlined.Info,
            tint = c.ink2,
            wash = c.lineSoft,
            modifier = modifier,
        )
        Kind.UNCLASSIFIED -> StampChip(
            label = "Not yet classified",
            description = "Not yet classified",
            glyph = Icons.Outlined.Info,
            tint = c.violet,
            wash = c.violetWash,
            modifier = modifier,
        )
    }
}

/**
 * The Example seal: violet pencil language, slightly rotated, marking every
 * sample artefact so it can never be mistaken for a real event.
 */
@Composable
fun ExampleStamp(modifier: Modifier = Modifier, label: String = "Example") {
    val c = LocalCashflowColors.current
    StampChip(
        label = label,
        description = "$label. Sample content, not a real payment.",
        glyph = Icons.Outlined.Create, // example seal: pencil marks sample content
        tint = c.violet,
        wash = c.violetWash,
        modifier = modifier,
    )
}
