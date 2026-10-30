package app.cashflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.ui.components.ActionButton
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.components.GhostButton
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

private const val LAST_STEP = 2

/**
 * Skippable three-step intro: the diary message, three icon-led lines, then
 * capture setup. [onFinish] runs from Skip and from the final action.
 */
@Composable
internal fun IntroScreen(
    state: CashflowState,
    actions: CashflowActions,
    step: Int,
    onStep: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    val c = LocalCashflowColors.current
    val s = step.coerceIn(0, LAST_STEP)
    Column(
        Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                repeat(LAST_STEP + 1) { i ->
                    Box(
                        Modifier.size(width = if (i == s) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (i == s) c.citron else c.line),
                    )
                }
            }
            if (s < LAST_STEP) GhostButton("Skip", onFinish)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp)) {
            when (s) {
                0 -> {
                    Text("A record book, not a bank", style = CamelType.VoiceHero, color = c.ink)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Cashflow keeps a diary of money you received and sent. It never moves money, " +
                            "and it does not show your bank balance.",
                        style = CamelType.VoiceBody, color = c.muted,
                    )
                }
                1 -> {
                    Text("Three things to know", style = CamelType.VoiceHero, color = c.ink)
                    Spacer(Modifier.height(16.dp))
                    IntroLine(Icons.Outlined.Add, "Write a record", "Tap the citron plus to add a payment by hand.")
                    IntroLine(CFIcons.Tray, "Review the unsure", "Alerts Cashflow cannot read clearly wait in the tray for you.")
                    IntroLine(CFIcons.Undo, "Nothing is final", "Deleted records and ignored alerts can be restored from Settings.")
                }
                else -> {
                    Text("Bring alerts in", style = CamelType.VoiceHero, color = c.ink)
                    Spacer(Modifier.height(12.dp))
                    CaptureGuideBody(state, actions)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (s > 0) {
                GhostButton("Back", { onStep(s - 1) })
            }
            ActionButton(
                if (s < LAST_STEP) "Next" else "Start with manual entry",
                { if (s < LAST_STEP) onStep(s + 1) else onFinish() },
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IntroLine(icon: ImageVector, title: String, text: String) {
    val c = LocalCashflowColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Icon(icon, null, tint = c.citron, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = CamelType.VoiceBody, color = c.ink)
            Text(text, style = CamelType.LedgerBody, color = c.muted)
        }
    }
}
