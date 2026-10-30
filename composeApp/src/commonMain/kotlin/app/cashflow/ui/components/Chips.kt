package app.cashflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

/**
 * One settings line: icon, title, one optional honest value sentence, and an
 * optional trailing slot (switch or caret). Rows are 48dp-tall targets; when
 * the line carries no action it renders as plain information.
 */
@Composable
fun SettingRow(
    icon: ImageVector?,
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint ?: c.muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text = title, style = CamelType.VoiceBody, color = c.ink)
            if (value != null) {
                Text(text = value, style = CamelType.LedgerBody, color = c.muted)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * A switch line: the whole row is the one accessible toggle (the embedded
 * control is display-only), so the merged node announces label + state once.
 */
@Composable
fun ToggleRow(
    title: String,
    value: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCashflowColors.current
    SettingRow(
        icon = null,
        title = title,
        value = value,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = c.citron,
                    checkedThumbColor = c.onCitron,
                    uncheckedThumbColor = c.muted,
                ),
            )
        },
        modifier = modifier
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .semantics(mergeDescendants = true) {},
    )
}

/** Hairline divider within settings and guide sections. */
@Composable
fun Divider() {
    val c = LocalCashflowColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(1.dp)
            .background(c.hairline),
    )
}
