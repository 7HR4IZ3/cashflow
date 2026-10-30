package app.cashflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cashflow.core.SourceApp
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val COLUMNS = 4

/** Icon-first grid of apps. Selected apps get a citron ring and check; everything stays glanceable. */
@Composable
fun AppGrid(apps: List<SourceApp>, onToggle: (SourceApp, Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        apps.chunked(COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { app ->
                    AppTile(app, { onToggle(app, !app.selected) }, Modifier.weight(1f))
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AppTile(app: SourceApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    val loader = LocalAppIcons.current
    val icon by produceState<ImageBitmap?>(null, app.id) { value = withContext(Dispatchers.Default) { loader(app.id) } }
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = app.name
                stateDescription = if (app.selected) "Watching" else "Not watching"
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp)) {
            Box(
                Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .clip(shape)
                    .background(c.surface)
                    .then(if (app.selected) Modifier.border(2.dp, c.citron, shape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = icon
                if (bitmap != null) {
                    Image(bitmap, contentDescription = null, modifier = Modifier.size(44.dp))
                } else {
                    Text(app.name.firstOrNull()?.uppercase() ?: "?", style = CamelType.LedgerAmount, color = c.ink)
                }
            }
            if (app.selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(c.citron),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(CFIcons.CheckMark, null, tint = c.onCitron, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            app.name,
            style = CamelType.LedgerLabel,
            color = if (app.selected) c.ink else c.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
