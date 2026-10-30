package app.cashflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Money
import app.cashflow.core.SourceApp
import app.cashflow.ui.components.AppGrid
import app.cashflow.ui.components.ActionButton
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.components.CurrencyGlyphPicker
import app.cashflow.ui.components.Divider
import app.cashflow.ui.components.GhostButton
import app.cashflow.ui.components.Ictl
import app.cashflow.ui.components.Kicker
import app.cashflow.ui.components.PaperField
import app.cashflow.ui.components.SettingRow
import app.cashflow.ui.components.SheetOverlay
import app.cashflow.ui.components.ToggleRow
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.PaperDates

private enum class SettingsPage(val title: String) {
    MAIN("Settings"),
    HELP("Help"),
    CAPTURE("Capture"),
    APPS("Apps to watch"),
    PASTE("Paste an alert"),
    RECOVERY("Recovery"),
}

private const val PASTE_LIMIT = 2000

/**
 * Small, honest settings sheet. Sub-pages swap inside the same sheet so there
 * is only ever one overlay; the back chevron returns to the list.
 */
@Composable
internal fun SettingsSheet(
    state: CashflowState,
    actions: CashflowActions,
    reduceMotion: Boolean,
    onReduceMotion: (Boolean) -> Unit,
    onReplayIntro: () -> Unit,
    onToast: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalCashflowColors.current
    var page by remember { mutableStateOf(SettingsPage.MAIN) }
    SheetOverlay(title = null, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            if (page != SettingsPage.MAIN) {
                Ictl(CFIcons.ChevronLeft, "Back to settings", onClick = { page = SettingsPage.MAIN })
            }
            Text(page.title, style = CamelType.VoiceTitle, color = c.ink)
        }
        when (page) {
            SettingsPage.MAIN -> MainPage(
                state, actions, reduceMotion, onReduceMotion, onToast,
                open = { page = it },
            )
            SettingsPage.HELP -> HelpPage(onReplayIntro)
            SettingsPage.CAPTURE -> CapturePage(state, actions, open = { page = it })
            SettingsPage.APPS -> AppsPage(state, actions)
            SettingsPage.PASTE -> PastePage(actions, onToast, onDone = { page = SettingsPage.MAIN })
            SettingsPage.RECOVERY -> RecoveryPage(state, actions, onToast)
        }
    }
}

@Composable
private fun MainPage(
    state: CashflowState,
    actions: CashflowActions,
    reduceMotion: Boolean,
    onReduceMotion: (Boolean) -> Unit,
    onToast: (String) -> Unit,
    open: (SettingsPage) -> Unit,
) {
    val c = LocalCashflowColors.current
    val recoverable = state.entries.count { it.deleted } + state.ignoredReview.size
    val watching = state.capture.sources.count { it.selected }
    NavLine("Capture",
        when {
            state.capturePaused -> "Paused"
            state.capture.platform == "Android" && !state.capture.accessGranted -> "Needs access"
            watching > 0 -> if (watching == 1) "1 app" else "$watching apps"
            else -> "On"
        },
        onClick = { open(SettingsPage.CAPTURE) },
    )
    SettingRow(
        icon = null,
        title = "View currency",
        trailing = {
            CurrencyGlyphPicker(
                selected = state.currency,
                onSelect = { it?.let(actions::setCurrency) },
                descriptionBase = "View currency",
            )
        },
    )
    ToggleLine("Reduce motion", reduceMotion, onReduceMotion)
    NavLine("Recovery", if (recoverable == 0) "" else "$recoverable", onClick = { open(SettingsPage.RECOVERY) })
    NavLine("Export CSV", "", onClick = { onToast(actions.exportCsv() ?: "Share sheet opened.") })
    NavLine("Help", "", onClick = { open(SettingsPage.HELP) })
    Spacer(Modifier.height(10.dp))
    Text(
        "A record book. It never moves money or touches a bank, and alert text stays on this device.",
        style = CamelType.LedgerLabel, color = c.muted,
    )
}

/** One-line row: icon, title, quiet value, chevron. */
@Composable
private fun NavLine(title: String, value: String, onClick: () -> Unit) {
    val c = LocalCashflowColors.current
    SettingRow(
        icon = null, title = title, onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) Text(value, style = CamelType.LedgerBody, color = c.muted)
                Icon(CFIcons.ChevronRight, null, tint = c.muted, modifier = Modifier.size(18.dp))
            }
        },
    )
}

@Composable
private fun ToggleLine(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ToggleRow(title = title, value = null, checked = checked, onChecked = onChecked)
}

@Composable
private fun HelpPage(onReplayIntro: () -> Unit) {
    GhostButton("Replay the intro", onReplayIntro, Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    LegendPage()
}

@Composable
private fun LegendPage() {
    val c = LocalCashflowColors.current
    val rows: List<Triple<ImageVector, String, String>> = listOf(
        Triple(CFIcons.ArrowIn, "In", "Money received. Arrow points to the lower left."),
        Triple(CFIcons.ArrowOut, "Out", "Money sent. Arrow points to the upper right."),
        Triple(Icons.Outlined.Home, "Home", "The activity feed."),
        Triple(Icons.Outlined.Add, "Add", "Write a record by hand."),
        Triple(CFIcons.Tray, "Review", "Alerts waiting for your answer; the number is how many."),
        Triple(CFIcons.Magnifier, "Search", "Find a record. A mark shows while a search is active."),
        Triple(CFIcons.Funnel, "Filter", "Narrow the feed. A count shows how many filters are on."),
        Triple(CFIcons.MinusCircle, "Ignore", "Set an alert aside without recording it."),
        Triple(CFIcons.Receipt, "Original alert", "The message a record was read from."),
        Triple(Icons.Outlined.Check, "Save", "Records only when everything required is valid."),
        Triple(Icons.Outlined.Delete, "Delete", "Removes a record; you can restore it in Recovery."),
    )
    rows.forEach { (icon, name, text) ->
        Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = c.citron, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, style = CamelType.VoiceBody, color = c.ink)
                Text(text, style = CamelType.LedgerLabel, color = c.muted)
            }
        }
    }
}

@Composable
internal fun CaptureGuideBody(state: CashflowState, actions: CashflowActions) {
    val c = LocalCashflowColors.current
    if (state.capture.platform == "Android") {
        GuideLine("1", "Open capture access and allow Cashflow to read notifications.")
        GuideLine("2", "Choose the banking apps to watch under Apps to watch.")
        GuideLine("3", "Clear debit or credit alerts are recorded; unsure ones wait in Review.")
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.capture.accessGranted) "Notification access is on." else "Notification access is not on yet.",
            style = CamelType.LedgerLabel, color = if (state.capture.accessGranted) c.citron else c.muted,
        )
        Spacer(Modifier.height(10.dp))
        ActionButton("Open capture access", { actions.openCaptureSettings() }, Modifier.fillMaxWidth())
    } else {
        GuideLine("1", "In Shortcuts, add an automation for Email or Messages.")
        GuideLine("2", "Run the Cashflow action \"Capture transaction notification\".")
        GuideLine("3", "Clear alerts are recorded; unsure ones wait in Review.")
        Spacer(Modifier.height(6.dp))
        Text(
            "Shortcuts cannot see arbitrary notifications from other apps. For those, add a record by hand or paste the text.",
            style = CamelType.LedgerLabel, color = c.muted,
        )
        Spacer(Modifier.height(10.dp))
        ActionButton("Open Shortcuts setup", { actions.openCaptureSettings() }, Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(6.dp))
    Text("Manual entry always works, with or without capture.", style = CamelType.LedgerLabel, color = c.muted)
}

@Composable
private fun GuideLine(n: String, text: String) {
    val c = LocalCashflowColors.current
    Row(Modifier.padding(vertical = 5.dp)) {
        Text(n, style = CamelType.LedgerAmount, color = c.citron, modifier = Modifier.width(22.dp))
        Text(text, style = CamelType.VoiceBody, color = c.ink2)
    }
}

@Composable
private fun CapturePage(state: CashflowState, actions: CashflowActions, open: (SettingsPage) -> Unit) {
    val c = LocalCashflowColors.current
    val watching = state.capture.sources.count { it.selected }
    // Until access is on, the steps are the whole story; afterwards they get out of the way.
    if (state.capture.accessGranted) {
        ToggleRow("Pause capture", null, state.capturePaused, actions::setCapturePaused)
        if (state.capture.sources.isNotEmpty()) {
            NavLine("Apps to watch", if (watching == 0) "None" else "$watching", onClick = { open(SettingsPage.APPS) })
        }
        NavLine("Paste an alert", "", onClick = { open(SettingsPage.PASTE) })
        Text(
            state.capture.lastReceived?.let { "Last alert: ${it.take(16).replace('T', ' ')}" } ?: "No alert received yet.",
            style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        GhostButton("Refresh status", { actions.refreshCapture() }, Modifier.fillMaxWidth())
    } else {
        CaptureGuideBody(state, actions)
        NavLine("Paste an alert instead", "", onClick = { open(SettingsPage.PASTE) })
        if (state.capture.sources.isNotEmpty()) {
            NavLine("Apps to watch", if (watching == 0) "None" else "$watching", onClick = { open(SettingsPage.APPS) })
        }
    }
}

@Composable
private fun AppsPage(state: CashflowState, actions: CashflowActions) {
    val c = LocalCashflowColors.current
    var query by remember { mutableStateOf("") }
    val all = state.capture.sources
    val shown = all.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    val watching = shown.filter { it.selected }
    val rest = shown.filterNot { it.selected }
    val toggle: (SourceApp, Boolean) -> Unit = { app, on -> actions.selectSource(app.id, on) }
    Text("Tap an app to watch its alerts.", style = CamelType.LedgerLabel, color = c.muted)
    Spacer(Modifier.height(8.dp))
    PaperField(value = query, onValueChange = { query = it }, placeholder = "Search apps")
    Spacer(Modifier.height(10.dp))
    if (watching.isNotEmpty()) {
        Kicker("Watching · ${all.count { it.selected }}")
        Spacer(Modifier.height(6.dp))
        AppGrid(watching, toggle)
        Spacer(Modifier.height(14.dp))
    }
    if (rest.isNotEmpty()) {
        Kicker("All apps")
        Spacer(Modifier.height(6.dp))
        AppGrid(rest, toggle)
    }
    if (shown.isEmpty()) Text("No app matches.", style = CamelType.LedgerLabel, color = c.muted)
}

@Composable
private fun PastePage(actions: CashflowActions, onToast: (String) -> Unit, onDone: () -> Unit) {
    val c = LocalCashflowColors.current
    var text by remember { mutableStateOf("") }
    val valid = text.isNotBlank()
    Text(
        "Clear alerts are recorded; unsure ones wait in Review. The text is read on this device only.",
        style = CamelType.LedgerLabel, color = c.muted,
    )
    Spacer(Modifier.height(10.dp))
    PaperField(
        value = text,
        onValueChange = { if (it.length <= PASTE_LIMIT) text = it },
        placeholder = "Paste a payment message",
        singleLine = false,
        minLines = 4,
    )
    Spacer(Modifier.height(10.dp))
    ActionButton("Check alert", {
        val problem = actions.captureText(text.trim())
        if (problem == null) {
            onToast("Alert checked. See Home or Review.")
            onDone()
        } else onToast(problem)
    }, Modifier.fillMaxWidth(), enabled = valid)
}

@Composable
private fun RecoveryPage(state: CashflowState, actions: CashflowActions, onToast: (String) -> Unit) {
    val c = LocalCashflowColors.current
    val deleted = sortedEntries(state.entries, deleted = true)
    Kicker("Deleted records")
    if (deleted.isEmpty()) {
        Text("None.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(vertical = 8.dp))
    }
    deleted.forEach { e ->
        RecoveryRow(
            title = e.counterparty.ifBlank { "Unnamed record" },
            detail = Money.display(e.amountMinor, e.currency) + " · " + PaperDates.dateLabel(e.date),
            description = "Restore record ${e.counterparty}",
            onRestore = { actions.restore(e.id)?.let(onToast) ?: onToast("Record restored.") },
        )
    }
    Spacer(Modifier.height(12.dp))
    Kicker("Ignored alerts")
    if (state.ignoredReview.isEmpty()) {
        Text("None.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(vertical = 8.dp))
    }
    state.ignoredReview.forEach { r ->
        val d = r.draft
        val amount = Money.parse(d.amount, d.currency)?.let { Money.display(it, d.currency) } ?: "Amount missing"
        RecoveryRow(
            title = d.counterparty.ifBlank { r.source },
            detail = amount,
            description = "Restore ignored alert from ${d.counterparty.ifBlank { r.source }}",
            onRestore = { actions.restoreReview(r.id)?.let(onToast) ?: onToast("Alert is back in Review.") },
        )
    }
    Spacer(Modifier.height(8.dp))
    Text("Restoring keeps the same record. Only Cashflow records are affected, never your bank.", style = CamelType.LedgerLabel, color = c.muted)
}

@Composable
private fun RecoveryRow(title: String, detail: String, description: String, onRestore: () -> Unit) {
    val c = LocalCashflowColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = CamelType.VoiceBody, color = c.ink)
            Text(detail, style = CamelType.LedgerLabel, color = c.muted)
        }
        Ictl(CFIcons.Undo, description, onClick = onRestore, tint = c.citron)
    }
}
