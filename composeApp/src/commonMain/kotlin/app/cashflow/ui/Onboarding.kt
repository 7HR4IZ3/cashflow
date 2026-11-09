package app.cashflow.ui

import androidx.compose.foundation.background
import org.jetbrains.compose.resources.painterResource
import cashflow.composeapp.generated.resources.brand_icon
import cashflow.composeapp.generated.resources.Res
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
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
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

private const val LAST_STEP = 2

/**
 * Three screens, almost no words: what this is, how it works, how to bring alerts in.
 * Icons carry the meaning; a label only where an icon alone would be a guess.
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
    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                repeat(LAST_STEP + 1) { i ->
                    Box(
                        Modifier.size(width = if (i == s) 26.dp else 8.dp, height = 8.dp).clip(CircleShape)
                            .background(if (i <= s) c.citron else c.line),
                    )
                }
            }
            if (s < LAST_STEP) SetupButton("Skip", CFIcons.ChevronRight, onFinish, filled = false, iconAtEnd = true, small = true)
        }
        if (s < LAST_STEP) {
            // Short screens sit in the middle of the space instead of hugging the top.
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                if (s == 0) RecordBookPage() else HowItWorksPage()
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 28.dp)) {
                Text("Bring alerts in", style = CamelType.VoiceHero, color = c.ink)
                Spacer(Modifier.height(22.dp))
                CaptureGuideBody(state, actions)
                // Clear the Back/Finish bar so the last buttons are never hidden under it.
                Spacer(Modifier.height(120.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (s > 0) SetupIconButton(CFIcons.ChevronLeft, "Back", { onStep(s - 1) })
            if (s < LAST_STEP) SetupButton("Next", CFIcons.ChevronRight, { onStep(s + 1) }, Modifier.weight(1f), iconAtEnd = true)
            else SetupButton("Finish", Icons.Outlined.Check, onFinish, Modifier.weight(1f))
        }
    }
}

/** Screen 1: the promise, as one mark and three facts. */
@Composable
private fun RecordBookPage() {
    val c = LocalCashflowColors.current
    Column(Modifier.fillMaxWidth()) {
        // The real app icon, so the first thing people see is the thing on their home screen.
        Image(
            painter = painterResource(Res.drawable.brand_icon),
            contentDescription = "Cashflow",
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(30.dp)),
        )
        Spacer(Modifier.height(22.dp))
        Text("Your money diary", style = CamelType.VoiceHero, color = c.ink)
        Spacer(Modifier.height(26.dp))
        Fact(Icons.Outlined.Lock, "Stays on this phone")
        Fact(CFIcons.MinusCircle, "Never touches your bank")
        Fact(CFIcons.Receipt, "A record book, nothing more")
    }
}

/** Screen 2: the whole loop as a picture. */
@Composable
private fun HowItWorksPage() {
    val c = LocalCashflowColors.current
    Column(Modifier.fillMaxWidth()) {
        Text("How it works", style = CamelType.VoiceHero, color = c.ink)
        Spacer(Modifier.height(44.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LoopTile(Icons.Outlined.Add, "Add", filled = true)
            Icon(CFIcons.ChevronRight, null, tint = c.faint, modifier = Modifier.size(18.dp))
            LoopTile(CFIcons.ArrowIn, "Alerts")
            Icon(CFIcons.ChevronRight, null, tint = c.faint, modifier = Modifier.size(18.dp))
            LoopTile(CFIcons.Tray, "Review")
        }
        Spacer(Modifier.height(48.dp))
        Fact(Icons.Outlined.Refresh, "Undo anything")
        Fact(CFIcons.Tray, "Unsure alerts wait for you")
    }
}

@Composable
private fun LoopTile(icon: ImageVector, label: String, filled: Boolean = false) {
    val c = LocalCashflowColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(if (filled) c.citron else c.surface),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (filled) c.onCitron else c.citron, modifier = Modifier.size(38.dp)) }
        Text(label, style = CamelType.LedgerLabel, color = c.ink2)
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    val c = LocalCashflowColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.citron, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = CamelType.VoiceBody, color = c.ink)
    }
}
