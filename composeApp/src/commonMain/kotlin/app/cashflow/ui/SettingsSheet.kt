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
import androidx.compose.ui.semantics.contentDescription
import app.cashflow.ui.components.FlowPoint
import app.cashflow.ui.components.FlowChart
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Direction
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
    PLAN("Plan & AI"),
    HELP("Help"),
    CAPTURE("Capture"),
    APPS("Apps to watch"),
    PASTE("Paste an alert"),
    RECOVERY("Recovery"),
    ACCOUNTS("Accounts"),
    GOALS("Goals"),
    NOTIFICATIONS("Notifications"),
    LEARNED("Learned alerts"),
    CHART("Home chart"),
}

private const val PASTE_LIMIT = 2000

/**
 * Settings, as one full-height screen. A live status card says how things stand, then everything is grouped by
 * what you are trying to do. Sub-pages swap in place under the same header; X always leaves.
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
    startAtGoals: Boolean = false,
) {
    val c = LocalCashflowColors.current
    var page by remember { mutableStateOf(if (startAtGoals) SettingsPage.GOALS else SettingsPage.MAIN) }
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page != SettingsPage.MAIN) {
                Ictl(CFIcons.ChevronLeft, "Back to settings", onClick = { page = SettingsPage.MAIN })
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(page.title, style = CamelType.VoiceTitle, color = c.ink, modifier = Modifier.weight(1f))
            Ictl(Icons.Outlined.Close, "Close settings", onClick = onDismiss)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            when (page) {
                SettingsPage.MAIN -> MainPage(state, actions, reduceMotion, onReduceMotion, onToast, open = { page = it })
                SettingsPage.PLAN -> PlanPage(state, actions)
                SettingsPage.HELP -> HelpPage(onReplayIntro)
                SettingsPage.CAPTURE -> CapturePage(state, actions, open = { page = it })
                SettingsPage.APPS -> AppsPage(state, actions)
                SettingsPage.PASTE -> PastePage(actions, onToast, onDone = { page = SettingsPage.MAIN })
                SettingsPage.RECOVERY -> RecoveryPage(state, actions, onToast)
                SettingsPage.ACCOUNTS -> AccountsPage(state, actions, onToast)
                SettingsPage.GOALS -> GoalsPage(state, actions, onToast)
                SettingsPage.NOTIFICATIONS -> NotificationsPage(state, actions)
                SettingsPage.LEARNED -> LearnedPage(state, actions, onToast)
                SettingsPage.CHART -> ChartPage(state, actions)
            }
            Spacer(Modifier.height(48.dp))
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
    val uriHandler = LocalUriHandler.current
    val watching = state.capture.sources.count { it.selected }
    val android = state.capture.platform == "Android"
    val needsAccess = android && !state.capture.accessGranted

    // How things stand, in one glance. Tapping it opens the page that fixes it.
    val attention = state.capturePaused || needsAccess
    StatusCard(
        attention = attention,
        title = when {
            state.capturePaused -> "Capture is paused"
            needsAccess -> "Capture needs access"
            state.capture.lastReceived != null -> "Capture is on"
            else -> "Waiting for the first alert"
        },
        detail = buildString {
            append(state.capture.lastReceived?.let { "Last alert ${it.take(16).replace('T', ' ')}" } ?: "No alerts received yet")
            append("  ·  ").append(state.subscription.tier.label).append(" plan")
        },
        onClick = { open(SettingsPage.CAPTURE) },
    )

    SettingsSection("Capture and help")
    SettingsGroup {
        SettingsItem(
            Icons.Outlined.Email, "Capture",
            when {
                state.capturePaused -> "Paused"
                needsAccess -> "Needs access"
                watching > 0 -> if (watching == 1) "1 app" else "$watching apps"
                else -> "On"
            },
            valueColor = if (attention) c.lavender else null,
        ) { open(SettingsPage.CAPTURE) }
        SettingsDivider()
        SettingsItem(Icons.Outlined.CheckCircle, "Learned alerts", state.learnedRules.size.takeIf { it > 0 }?.toString() ?: "") { open(SettingsPage.LEARNED) }
        SettingsDivider()
        SettingsItem(Icons.Outlined.Star, "Plan and AI", state.subscription.tier.label) { open(SettingsPage.PLAN) }
    }

    SettingsSection("Your money")
    SettingsGroup {
        SettingsItem(Icons.Outlined.AccountBox, "Accounts", state.accounts.count { !it.archived }.takeIf { it > 0 }?.toString() ?: "") { open(SettingsPage.ACCOUNTS) }
        SettingsDivider()
        SettingsItem(Icons.Outlined.Place, "Goals", state.goals.size.takeIf { it > 0 }?.toString() ?: "") { open(SettingsPage.GOALS) }
        SettingsDivider()
        SettingsItem(
            Icons.Outlined.Info, "View currency", "", onClick = null,
            trailing = {
                CurrencyGlyphPicker(
                    selected = state.currency,
                    onSelect = { it?.let(actions::setCurrency) },
                    descriptionBase = "View currency",
                )
            },
        )
        SettingsDivider()
        SettingsItem(Icons.Outlined.Share, "Export as CSV", "") { onToast(actions.exportCsv() ?: "Share sheet opened.") }
    }

    SettingsSection("This app")
    SettingsGroup {
        SettingsItem(Icons.Outlined.DateRange, "Home chart", state.chartStyle.label) { open(SettingsPage.CHART) }
        SettingsDivider()
        SettingsItem(Icons.Outlined.Notifications, "Notifications", "") { open(SettingsPage.NOTIFICATIONS) }
        SettingsDivider()
        SettingsItem(
            Icons.Outlined.PlayArrow, "Reduce motion", "", onClick = { onReduceMotion(!reduceMotion) },
            trailing = { SettingsSwitch(reduceMotion) },
        )
        SettingsDivider()
        SettingsItem(Icons.Outlined.Refresh, "Recovery", if (recoverable == 0) "" else "$recoverable") { open(SettingsPage.RECOVERY) }
    }

    SettingsSection("About")
    SettingsGroup {
        SettingsItem(Icons.Outlined.Info, "Help and legend", "") { open(SettingsPage.HELP) }
        SettingsDivider()
        SettingsItem(Icons.Outlined.Lock, "Privacy policy", "") {
            runCatching { uriHandler.openUri("https://7hr4iz3.github.io/cashflow/privacy.html") }
                .onFailure { onToast("Couldn't open the privacy policy. Try again with a browser installed.") }
        }
    }
    Spacer(Modifier.height(14.dp))
    Text(
        "A record book. It never moves money or touches a bank. Optional AI help sends alerts with your permission.",
        style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun StatusCard(attention: Boolean, title: String, detail: String, onClick: () -> Unit) {
    val c = LocalCashflowColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (attention) c.lavenderWash else c.citronWash),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (attention) Icons.Outlined.Warning else Icons.Outlined.Check, null,
                tint = if (attention) c.lavender else c.citron, modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = CamelType.VoiceBody.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.ink)
            Text(detail, style = CamelType.LedgerLabel, color = c.muted)
        }
        Icon(CFIcons.ChevronRight, null, tint = c.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun SettingsSection(title: String) {
    val c = LocalCashflowColors.current
    Text(
        title.uppercase(), style = CamelType.Kicker, color = c.muted,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SettingsGroup(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = LocalCashflowColors.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface), content = content)
}

@Composable
internal fun SettingsDivider() {
    val c = LocalCashflowColors.current
    Box(Modifier.fillMaxWidth().padding(start = 60.dp).height(1.dp).background(c.hairline))
}

/** One row: an icon tile, the name, then its value and a chevron. A custom [trailing] replaces both. */
@Composable
internal fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    // Last on purpose: a trailing lambda at the call site is the click handler, never composable content.
    onClick: (() -> Unit)? = null,
) {
    val c = LocalCashflowColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.field), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink2, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = CamelType.VoiceBody, color = c.ink)
            if (subtitle != null) Text(subtitle, style = CamelType.LedgerLabel, color = c.muted)
        }
        if (trailing != null) {
            trailing()
        } else {
            if (value.isNotEmpty()) Text(value, style = CamelType.LedgerBody, color = valueColor ?: c.muted)
            Spacer(Modifier.width(6.dp))
            Icon(CFIcons.ChevronRight, null, tint = c.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun SettingsSwitch(checked: Boolean) {
    val c = LocalCashflowColors.current
    Box(
        Modifier.width(46.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(if (checked) c.citron else c.field).padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(if (checked) c.onCitron else c.muted))
    }
}

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
    if (state.capture.platform == "Android") AndroidSetupGuide(state, actions) else IosSetupGuide(state, actions)
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
    if (state.captureOverLimit) Text("Capture is paused because your selected apps exceed this plan. Deselect apps to resume, or change your plan. Your records are safe.", style = CamelType.LedgerLabel, color = c.red)
    val all = state.capture.sources
    val shown = all.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    val watching = shown.filter { it.selected }
    val rest = shown.filterNot { it.selected }
    val toggle: (SourceApp, Boolean) -> Unit = { app, on -> actions.selectSource(app.id, on) }
    Text("${state.subscription.tier.label}: ${state.subscription.tier.watchedApps?.let { "up to $it watched apps" } ?: "unlimited watched apps"}. Tap an app to watch its alerts.", style = CamelType.LedgerLabel, color = c.muted)
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
        "Clear alerts are recorded; unsure ones wait in Review. Local parsing runs on this device. If automatic AI help is enabled, unclear alerts are also sent for AI suggestions.",
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
    SettingsSection("Deleted records")
    if (deleted.isEmpty()) EmptyNote("Nothing deleted")
    else SettingsGroup {
        deleted.forEachIndexed { i, e ->
            if (i > 0) SettingsDivider()
            RecoveryRow(
                title = e.counterparty.ifBlank { "Unnamed record" },
                detail = Money.display(e.amountMinor, e.currency) + " · " + PaperDates.dateLabel(e.date),
                description = "Restore record ${e.counterparty}",
                onRestore = { actions.restore(e.id)?.let(onToast) ?: onToast("Record restored.") },
            )
        }
    }
    SettingsSection("Ignored alerts")
    if (state.ignoredReview.isEmpty()) EmptyNote("Nothing ignored")
    else SettingsGroup {
        state.ignoredReview.forEachIndexed { i, r ->
            if (i > 0) SettingsDivider()
            val d = r.draft
            val amount = Money.parse(d.amount, d.currency)?.let { Money.display(it, d.currency) } ?: "Amount missing"
            RecoveryRow(
                title = d.counterparty.ifBlank { r.source },
                detail = amount,
                description = "Restore ignored alert from ${d.counterparty.ifBlank { r.source }}",
                onRestore = { actions.restoreReview(r.id)?.let(onToast) ?: onToast("Alert is back in Review.") },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Restoring brings back the same record. Your bank is never touched.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun EmptyNote(text: String) {
    val c = LocalCashflowColors.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Check, null, tint = c.faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = CamelType.VoiceBody, color = c.muted)
    }
}

@Composable
private fun LearnedPage(state: CashflowState, actions: CashflowActions, onToast: (String) -> Unit) {
    val c = LocalCashflowColors.current
    if (state.learnedRules.isEmpty()) {
        EmptyNote("Nothing learned yet")
        Spacer(Modifier.height(12.dp))
        Text("Fix an alert in Review and Cashflow remembers that kind of alert from the same app.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
        return
    }
    SettingsGroup {
        state.learnedRules.asReversed().forEachIndexed { i, r ->
            if (i > 0) SettingsDivider()
            val auto = r.confirmations >= app.cashflow.core.Learned.AUTO_AT
            SettingsItem(
                if (r.direction == Direction.IN) CFIcons.ArrowIn else CFIcons.ArrowOut,
                "${r.source} · ${if (r.direction == Direction.IN) "money in" else "money out"}",
                "",
                subtitle = r.sample + "\n" + (if (auto) "Records automatically" else "Needs one more confirmation") + " · used ${r.uses}×" + if (r.origin == "ai") " · from Jev" else "",
                trailing = { SetupButton("Forget", Icons.Outlined.Delete, { actions.deleteLearnedRule(r.id); onToast("Forgotten.") }, filled = false, small = true) },
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    SetupButton("Forget everything", Icons.Outlined.Delete, { actions.clearLearnedRules(); onToast("Learned alerts cleared.") }, Modifier.fillMaxWidth(), filled = false, tint = c.red)
    Spacer(Modifier.height(12.dp))
    Text("Rules stay on this device. They only decide money in or out and the kind. Amounts and one-time codes are always checked first.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun RecoveryRow(title: String, detail: String, description: String, onRestore: () -> Unit) {
    SettingsItem(
        Icons.Outlined.Refresh, title, "", subtitle = detail,
        trailing = { SetupButton("Restore", Icons.Outlined.Refresh, onRestore, filled = false, small = true) },
    )
}

/** Each home chart style, drawn live with sample data. Tap one to use it. */
@Composable
private fun ChartPage(state: CashflowState, actions: CashflowActions) {
    val c = LocalCashflowColors.current
    val demo = remember {
        val ins = listOf(0L, 40000, 20000, 0, 70000, 30000, 0, 55000, 25000, 90000)
        val outs = listOf(20000L, 0, 35000, 50000, 10000, 0, 45000, 15000, 0, 30000)
        ins.indices.map { FlowPoint(it.toLong(), "", ins[it], outs[it]) }
    }
    app.cashflow.core.ChartStyle.entries.forEach { style ->
        val on = state.chartStyle == style
        Column(
            Modifier
                .fillMaxWidth().padding(bottom = 12.dp)
                .clip(RoundedCornerShape(20.dp)).background(c.surface)
                .border(2.dp, if (on) c.citron else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(20.dp))
                .clickable { actions.setChartStyle(style) }
                .padding(14.dp)
                .semantics { contentDescription = "${style.label} chart" + if (on) ", selected" else "" },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(style.label, style = CamelType.VoiceBody.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = if (on) c.citron else c.ink, modifier = Modifier.weight(1f))
                if (on) Icon(Icons.Outlined.Check, null, tint = c.citron, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            FlowChart(demo, "NGN", selected = null, onSelect = {}, height = 96.dp, style = style, interactive = false, showAxis = false)
        }
    }
}

@Composable
private fun PlanPage(state: CashflowState, actions: CashflowActions) {
    val c = LocalCashflowColors.current
    val plan = state.subscription
    var details by rememberSaveable { mutableStateOf(false) }
    if (state.capture.platform != "Android") {
        EmptyNote("Subscriptions are on Android for now")
        Spacer(Modifier.height(12.dp))
        Text("Records, Review, export and Recovery all work fully here.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
        return
    }
    // Where you stand this month.
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Star, null, tint = c.citron, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("${plan.tier.label} plan", style = CamelType.VoiceTitle, color = c.ink, modifier = Modifier.weight(1f))
        }
        if (plan.tier != app.cashflow.core.PlanTier.FREE && plan.tier.monthlyAi > 0) {
            Spacer(Modifier.height(12.dp))
            val share = (plan.used.toFloat() / plan.tier.monthlyAi.toFloat()).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.field)) {
                Box(Modifier.fillMaxWidth(share).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (share >= 1f) c.lavender else c.citron))
            }
            Spacer(Modifier.height(6.dp))
            Text("${plan.used} of ${plan.tier.monthlyAi} AI alerts used${if (plan.month.isBlank()) "" else " in ${plan.month} (UTC)"}", style = CamelType.LedgerLabel, color = c.muted)
        }
    }

    SettingsSection("Plans")
    app.cashflow.core.PlanTier.entries.forEach { tier ->
        val current = tier == plan.tier
        Column(
            Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(20.dp)).background(c.surface)
                .border(2.dp, if (current) c.citron else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(20.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tier.label, style = CamelType.VoiceBody.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = if (current) c.citron else c.ink, modifier = Modifier.weight(1f))
                if (current) Icon(Icons.Outlined.Check, null, tint = c.citron, modifier = Modifier.size(20.dp))
            }
            Text(
                "${tier.watchedApps?.let { "$it watched apps" } ?: "Unlimited apps"} · ${if (tier.monthlyAi == 0) "Local processing" else "${tier.monthlyAi} AI alerts a month"}",
                style = CamelType.LedgerLabel, color = c.muted,
            )
            plan.offers.filter { it.tier == tier }.forEach { offer ->
                Spacer(Modifier.height(10.dp))
                SetupButton("${if (current) "Choose" else "Get ${tier.label}"} · ${offer.price} / ${offer.period}", Icons.Outlined.Star, { actions.purchase(offer.id) }, Modifier.fillMaxWidth(), filled = !current)
            }
        }
    }
    if (plan.offers.isEmpty()) Text("Prices appear when Google Play is connected.", style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))

    SettingsSection("AI help")
    SettingsGroup {
        SettingsItem(
            Icons.Outlined.CheckCircle, "Let AI check my alerts", "",
            subtitle = "Sends an alert's text and source for a suggestion",
            trailing = { SettingsSwitch(state.aiEnabled) }, onClick = { actions.setAiEnabled(!state.aiEnabled) },
        )
        if (state.aiEnabled) {
            SettingsDivider()
            SettingsItem(
                Icons.Outlined.Refresh, "Help with new unclear alerts", "",
                subtitle = "Sends them automatically",
                trailing = { SettingsSwitch(state.aiAutomatic) }, onClick = { actions.setAutomaticAi(!state.aiAutomatic) },
            )
        }
    }
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable { details = !details }.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = c.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("What gets sent, exactly", style = CamelType.LedgerLabel, color = c.muted)
    }
    if (details) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("When you tap Ask Jev in Review, or enable automatic help for new unclear alerts, the alert's text and source are sent through Cashflow's Convex backend to TypeSafe AI. They may contain financial or personal details. Your ledger and other alerts are not sent. Suggestions need your confirmation. Turning this off prevents future requests; a request already sent may finish at the provider.", style = CamelType.LedgerLabel, color = c.muted)
            Text("Automatic help sends new unclear payment alerts from your watched apps, or alerts you paste, to Convex and TypeSafe AI. Existing Review items are not sent automatically. Both switches are off by default. Suggestions still need your confirmation.", style = CamelType.LedgerLabel, color = c.muted)
            Text("AI allowances reset each calendar month in UTC. Subscriptions renew automatically unless cancelled in Google Play. No unused allowance carries over.", style = CamelType.LedgerLabel, color = c.muted)
        }
    }
    Spacer(Modifier.height(14.dp))
    SetupButton(if (plan.busy) "Checking purchases…" else "Restore purchases", Icons.Outlined.Refresh, { actions.refreshSubscription() }, Modifier.fillMaxWidth(), filled = false)
    Spacer(Modifier.height(8.dp))
    SetupButton("Manage in Google Play", Icons.Outlined.Settings, { actions.manageSubscription() }, Modifier.fillMaxWidth(), filled = false)
    plan.message?.let { Spacer(Modifier.height(8.dp)); Text(it, style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp)) }
}
