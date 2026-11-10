package app.cashflow.ui

import androidx.compose.animation.AnimatedVisibility
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import cashflow.composeapp.generated.resources.setup_tile
import cashflow.composeapp.generated.resources.setup_quiet
import cashflow.composeapp.generated.resources.setup_pick_banks
import cashflow.composeapp.generated.resources.Res
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import app.cashflow.ui.components.AppGrid
import app.cashflow.ui.components.PaperField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

// ---------------------------------------------------------------------------------------------------------
// Shared pieces: used by the three intro screens and by Settings → Capture.
// ---------------------------------------------------------------------------------------------------------

/** A button that is mostly an icon: filled citron for the main move, quiet for the rest. */
@Composable
internal fun SetupButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    iconAtEnd: Boolean = false,
    small: Boolean = false,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    val c = LocalCashflowColors.current
    val ink = when {
        !enabled -> c.faint
        tint != null -> tint
        filled -> c.onCitron
        else -> c.ink2
    }
    val glyph = @Composable { Icon(icon, null, tint = ink, modifier = Modifier.size(if (small) 17.dp else 20.dp)) }
    Row(
        modifier
            .heightIn(min = if (small) 40.dp else 54.dp)
            .clip(RoundedCornerShape(if (small) 14.dp else 18.dp))
            .background(if (filled && enabled) c.citron else c.surface)
            .then(if (filled && enabled) Modifier else Modifier.border(1.dp, c.line, RoundedCornerShape(if (small) 14.dp else 18.dp)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 20.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (!iconAtEnd) { glyph(); Spacer(Modifier.width(8.dp)) }
        Text(label, style = CamelType.VoiceBody.copy(fontWeight = FontWeight.SemiBold), color = ink)
        if (iconAtEnd) { Spacer(Modifier.width(8.dp)); glyph() }
    }
}

/** Icon-only round button, for Back. */
@Composable
internal fun SetupIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = LocalCashflowColors.current
    Box(
        Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = c.ink2, modifier = Modifier.size(22.dp)) }
}

/** One step on the rail: an icon in a disc (a citron check once done), the label, then whatever it needs. */
@Composable
internal fun SetupRow(icon: ImageVector, title: String, done: Boolean, last: Boolean, content: @Composable () -> Unit) {
    val c = LocalCashflowColors.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(42.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (done) c.citron else c.surface)
                    .then(if (done) Modifier else Modifier.border(1.dp, c.line, CircleShape)),
                contentAlignment = Alignment.Center,
            ) { Icon(if (done) Icons.Outlined.Check else icon, null, tint = if (done) c.onCitron else c.ink2, modifier = Modifier.size(20.dp)) }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(if (done) c.citron.copy(alpha = 0.45f) else c.line))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(bottom = if (last) 0.dp else 20.dp)) {
            Text(
                title, style = CamelType.VoiceBody.copy(fontWeight = FontWeight.SemiBold), color = c.ink,
                modifier = Modifier.heightIn(min = 40.dp).padding(top = 9.dp),
            )
            content()
        }
    }
}

/** The only honest signal, and the only line of status: has an alert really reached Cashflow? */
@Composable
internal fun ProofPill(lastReceived: String?, waitingWords: String) {
    val c = LocalCashflowColors.current
    val ok = lastReceived != null
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (ok) c.citronWash else c.surface).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if (ok) c.citron else c.field), contentAlignment = Alignment.Center) {
            Icon(if (ok) Icons.Outlined.Check else CFIcons.Tray, null, tint = if (ok) c.onCitron else c.muted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (ok) "Connected" else waitingWords,
                style = CamelType.VoiceBody.copy(fontWeight = FontWeight.SemiBold), color = if (ok) c.citron else c.ink,
            )
            if (ok) Text("Last alert ${lastReceived.take(16).replace('T', ' ')}", style = CamelType.LedgerLabel, color = c.muted)
        }
    }
}

/** Anything that needs explaining lives behind this one icon, never on the page. */
@Composable
internal fun HelpToggle(lines: List<String>) {
    val c = LocalCashflowColors.current
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable { open = !open }.padding(vertical = 8.dp, horizontal = 4.dp)
                .semantics { contentDescription = if (open) "Hide help" else "Show help" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, null, tint = c.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Stuck?", style = CamelType.LedgerLabel, color = c.muted)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 7.dp).size(5.dp).clip(CircleShape).background(c.faint))
                        Spacer(Modifier.width(10.dp))
                        Text(line, style = CamelType.LedgerLabel, color = c.muted)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// iOS: add the shortcut, pick the banks, run quietly.
// ---------------------------------------------------------------------------------------------------------

@Composable
internal fun IosSetupGuide(state: CashflowState, actions: CashflowActions) {
    var added by rememberSaveable { mutableStateOf(false) }
    var picked by rememberSaveable { mutableStateOf(false) }
    var quiet by rememberSaveable { mutableStateOf(false) }

    // The floating card belongs to this guide: allowed while it is on screen, gone the moment it is not.
    DisposableEffect(Unit) {
        actions.setupGuideVisible(true)
        onDispose { actions.setupGuideVisible(false) }
    }

    Column(Modifier.fillMaxWidth()) {
        SetupRow(CFIcons.ArrowIn, "Add the shortcut", done = added, last = false) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SetupButton("Add", CFIcons.ArrowIn, { actions.installShortcut(); added = true })
                    if (!added) SetupButton("Have it", Icons.Outlined.Check, { added = true }, filled = false, small = true)
                }
                Screenshot(Res.drawable.setup_tile, "The Capture transaction notification shortcut as it appears in Shortcuts", Modifier.weight(1f))
            }
        }
        SetupRow(Icons.Outlined.Notifications, "Pick your banks", done = picked, last = false) {
            Screenshot(Res.drawable.setup_pick_banks, "In Shortcuts: When I receive a notification from, with the bank app names you can tap, then the Capture transaction notification action.")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SetupButton("Shortcuts", CFIcons.ArrowOut, { actions.showSetupGuide(2); actions.openCaptureSettings() }, filled = false, small = true)
                if (!picked) SetupButton("Done", Icons.Outlined.Check, { picked = true }, filled = false, small = true)
            }
        }
        SetupRow(Icons.Outlined.PlayArrow, "Run quietly", done = quiet, last = true) {
            Screenshot(Res.drawable.setup_quiet, "Automation switched on, Notify switched off.")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SetupButton("Shortcuts", CFIcons.ArrowOut, { actions.showSetupGuide(3); actions.openCaptureSettings() }, filled = false, small = true)
                if (!quiet) SetupButton("Done", Icons.Outlined.Check, { quiet = true }, filled = false, small = true)
            }
        }
        Spacer(Modifier.height(20.dp))
        ProofPill(state.capture.lastReceived, "Waiting for first alert")
        Spacer(Modifier.height(4.dp))
        HelpToggle(
            listOf(
                "Look for the trigger \"When I receive a notification\". The wording shifts a little between iOS versions.",
                "A bank app must be installed and allowed to send notifications to appear in the list.",
                "Nothing arriving? Check Automation is on and your bank's alerts are not Deliver Quietly.",
                "You can always add a record by hand or paste an alert.",
            ),
        )
    }
}

/** A real screenshot from the Shortcuts app, in its own rounded frame. Never redrawn: people match what they see. */
@Composable
private fun Screenshot(image: DrawableResource, description: String, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    Image(
        painter = painterResource(image),
        contentDescription = description,
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, c.hairline, RoundedCornerShape(16.dp)),
    )
}

// ---------------------------------------------------------------------------------------------------------
// Android: allow access, then done.
// ---------------------------------------------------------------------------------------------------------

@Composable
internal fun AndroidSetupGuide(state: CashflowState, actions: CashflowActions) {
    val granted = state.capture.accessGranted
    var query by rememberSaveable { mutableStateOf("") }
    // Android 13+ needs a runtime permission before Cashflow can show its "recorded" notifications. Asked once, here,
    // where the person is already setting up alerts. Older versions ignore the request.
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!asked) { asked = true; actions.requestNotificationPermission() }
    }
    val sources = state.capture.sources
    val shown = sources.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    Column(Modifier.fillMaxWidth()) {
        SetupRow(Icons.Outlined.Notifications, "Allow access", done = granted, last = false) {
            if (!granted) SetupButton("Open", CFIcons.ArrowOut, { actions.openCaptureSettings() })
        }
        SetupRow(CFIcons.ArrowIn, "Pick your banks", done = sources.any { it.selected }, last = true) {
            if (sources.size > 12) {
                PaperField(value = query, onValueChange = { query = it }, placeholder = "Search apps")
                Spacer(Modifier.height(8.dp))
            }
            if (shown.isEmpty()) {
                Text("No app matches.", style = CamelType.LedgerLabel, color = LocalCashflowColors.current.muted)
            } else {
                AppGrid(shown, { app, on -> actions.selectSource(app.id, on) })
            }
        }
        Spacer(Modifier.height(8.dp))
        HelpToggle(
            listOf(
                "Cashflow only reads alerts from the apps you choose.",
                "Unsure alerts wait in Review for you.",
                "You can always add a record by hand or paste an alert.",
            ),
        )
    }
}
