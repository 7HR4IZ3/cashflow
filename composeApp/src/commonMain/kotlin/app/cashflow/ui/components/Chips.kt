package app.cashflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

/**
 * PaperChip: the one chip shape for filters and pickers. Selected state is a
 * wash plus a matching hairline; unselected is a raise ground with a soft
 * line. Words always carry the meaning; the glyph, if any, is support only.
 */
@Composable
fun PaperChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: ImageVector? = null,
    tint: Color? = null,
) {
    val c = LocalCashflowColors.current
    val accent = tint ?: c.green
    val accentWash = tint?.copy(alpha = if (c.dark) 0.29f else 0.1f) ?: c.greenWash
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accentWash else c.raise)
            .border(
                0.5.dp,
                if (selected) accent.copy(alpha = 0.45f) else c.line,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = if (selected) accent else c.ink3,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = CamelType.LedgerLabel,
            color = if (selected) accent else c.ink2,
        )
    }
}

/**
 * SettingRow: one settings line, icon plus word plus optional value, with an
 * optional trailing slot for switches or word buttons.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalCashflowColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { m -> if (onClick != null) m.clickable { onClick() } else m }
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: c.ink2,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = CamelType.VoiceBody, color = c.ink)
            if (value != null) {
                Text(text = value, style = CamelType.LedgerBody, color = c.ink2)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
