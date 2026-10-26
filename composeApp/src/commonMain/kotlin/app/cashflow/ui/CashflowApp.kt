package app.cashflow.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.EntryDraft
import app.cashflow.core.Money
import app.cashflow.core.totals
import app.cashflow.ui.components.AmountText
import app.cashflow.ui.components.ConnectorLine
import app.cashflow.ui.components.GuideCard
import app.cashflow.ui.components.LedgerRow
import app.cashflow.ui.components.PaperField
import app.cashflow.ui.components.PaperChip
import app.cashflow.ui.components.SettingRow
import app.cashflow.ui.components.StateBlock
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.CashflowTheme
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.PaperDates

private enum class Tab(val label: String) {
    HOME("Home"), LOG("Activity"), REVIEW("Review"), SETTINGS("Settings"),
}

// One stack over the tabs: an open editor or detail row covers the current tab.
private data class EditorRoute(val draft: EntryDraft, val entryId: String?, val reviewId: String?)

/**
 * CashflowApp: the root shell. Theme, scaffold, four tabs, the editor/detail
 * stack, error surface, and the paste-capture dialog. Everything else renders
 * inside its own screen composable.
 */
@Composable
fun CashflowApp(state: CashflowState, actions: CashflowActions) {
    CashflowTheme {
        val colors = LocalCashflowColors.current
        val snackbar = remember { SnackbarHostState() }
        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
        var editor by remember { mutableStateOf<EditorRoute?>(null) }
        var detailId by remember { mutableStateOf<String?>(null) }
        var showPaste by remember { mutableStateOf(false) }
        var showGuide by rememberSaveable { mutableStateOf(false) }
        var toast by remember { mutableStateOf<String?>(null) }

        // Errors from any action surface once, then clear.
        LaunchedEffect(state.error) {
            state.error?.let {
                snackbar.showSnackbar(it, withDismissAction = true)
                actions.clearError()
            }
        }
        // Quiet confirmations (export done, text sent to review).
        LaunchedEffect(toast) {
            toast?.let {
                snackbar.showSnackbar(it)
                toast = null
            }
        }
        // A notification's Edit request opens the editor for the CURRENT entry,
        // never a stale copy held somewhere else.
        LaunchedEffect(state.requestedEntryId) {
            state.requestedEntryId?.let { id ->
                state.entries.firstOrNull { it.id == id }?.let { entry ->
                    editor = EditorRoute(draftFromEntry(entry), entryId = entry.id, reviewId = null)
                }
                actions.clearFocus()
            }
        }

        if (!state.onboardingComplete) {
            OnboardingScreen(state, actions)
            return@CashflowTheme
        }

        val openEditorAdd: () -> Unit = { editor = EditorRoute(newDraft(state), null, null) }
        val openEditor: (EntryDraft, String?, String?) -> Unit =
            { draft, entryId, reviewId -> editor = EditorRoute(draft, entryId, reviewId) }
        val back: () -> Unit = {
            if (detailId != null && editor != null) editor = null else {
                editor = null; detailId = null
            }
        }

        // Exclusive full-screen routes on top of the tabs: the guide, the
        // editor, and the detail row each own the whole screen, so Home and
        // its controls are not left in the accessibility tree behind them.
        if (showGuide) {
            GuideOverlay(state, actions, onClose = { showGuide = false })
            return@CashflowTheme
        }
        if (editor != null) {
            editor?.let { route ->
                EntryEditorScreen(
                    draft = route.draft,
                    entryId = route.entryId,
                    reviewId = route.reviewId,
                    actions = actions,
                    onBack = back,
                )
            }
            return@CashflowTheme
        }
        detailId?.let { id ->
            val entry = remember(id, state.entries) { state.entries.firstOrNull { it.id == id } }
            if (entry != null) {
                EntryDetailScreen(
                    entry = entry,
                    actions = actions,
                    onEdit = { openEditor(draftFromEntry(entry), entry.id, null) },
                    onBack = { detailId = null; editor = null },
                )
                return@CashflowTheme
            }
        }

        Scaffold(
            containerColor = colors.paper,
            bottomBar = {
                NavigationBar(containerColor = colors.raise, tonalElevation = 0.dp) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item && editor == null && detailId == null,
                            onClick = { editor = null; detailId = null; tab = item },
                            label = {
                                val count = if (item == Tab.REVIEW) state.review.size else 0
                                Text(
                                    if (count > 0) "${item.label} · $count" else item.label,
                                    style = CamelType.LedgerLabel,
                                )
                            },
                            icon = {
                                Icon(imageVector = item.icon(), contentDescription = item.label)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.green,
                                selectedTextColor = colors.ink,
                                indicatorColor = colors.greenWash,
                                unselectedIconColor = colors.ink3,
                                unselectedTextColor = colors.ink3,
                            ),
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.padding(padding).fillMaxSize(),
                transitionSpec = { fadeIn(androidx.compose.animation.core.tween(220)) togetherWith fadeOut(androidx.compose.animation.core.tween(180)) },
                label = "cashflow-tab",
            ) { current ->
                when (current) {
                    Tab.HOME -> HomeScreen(
                        state, onAdd = openEditorAdd, onPaste = { showPaste = true },
                        onOpen = { detailId = it },
                    )
                    Tab.LOG -> ActivityScreen(state, onOpen = { detailId = it })
                    Tab.REVIEW -> ReviewScreen(state, actions, onEdit = { item ->
                        openEditor(item.draft, null, item.id)
                    })
                    Tab.SETTINGS -> SettingsScreen(
                        state, actions, toast = { toast = it },
                        onReopenGuide = { showGuide = true },
                        onPaste = { showPaste = true },
                    )
                }
            }
        }

        if (showPaste) {
            PasteDialog(
                onCapture = { text ->
                    val problem = actions.captureText(text)
                    if (problem == null) {
                        showPaste = false
                        toast = "Alert checked. See Activity or Review."
                    } else {
                        // Keep the dialog open so the text is not lost.
                        toast = problem
                    }
                },
                onDismiss = { showPaste = false },
            )
        }
    }
}

private fun Tab.icon() = when (this) {
    Tab.HOME -> Icons.Outlined.Home
    Tab.LOG -> Icons.Outlined.List
    Tab.REVIEW -> Icons.Outlined.Notifications
    Tab.SETTINGS -> Icons.Outlined.Settings
}

/** Entries sorted newest-first by real date; unreadable dates go last. */
internal fun sortedEntries(entries: List<app.cashflow.core.Entry>, deleted: Boolean) =
    entries.filter { it.deleted == deleted }
        .sortedByDescending { PaperDates.dayNumber(it.date) ?: Long.MIN_VALUE }

private fun draftFromEntry(e: app.cashflow.core.Entry) = EntryDraft(
    amount = Money.decimal(e.amountMinor, e.currency),
    currency = e.currency,
    direction = e.direction,
    kind = e.kind,
    status = e.status,
    counterparty = e.counterparty,
    account = e.account,
    note = e.note,
    date = e.date,
)

private fun newDraft(state: CashflowState) = EntryDraft(currency = state.currency)

// ---------------------------------------------------------------- Home

@Composable
private fun HomeScreen(
    state: CashflowState,
    onAdd: () -> Unit,
    onPaste: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val c = LocalCashflowColors.current
    val currency = state.currency
    val all = totals(state.entries, currency)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header: word label and the two always-available capture actions.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cashflow", style = CamelType.VoiceTitle, color = c.ink2)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPaste, modifier = Modifier.semantics { contentDescription = "Capture pasted text" }) {
                Icon(Icons.Outlined.Send, null, tint = c.ink2)
            }
            IconButton(onClick = onAdd, modifier = Modifier.semantics { contentDescription = "Add an entry" }) {
                Icon(Icons.Outlined.Add, null, tint = c.green)
            }
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Recorded activity · All time", style = CamelType.LedgerLabel, color = c.ink3)
            Spacer(Modifier.height(6.dp))
            Text("Net recorded", style = CamelType.LedgerLabel, color = c.ink3)
            Spacer(Modifier.height(10.dp))
            if (all.net >= 0) {
                AmountText(all.net, currency, style = CamelType.LedgerHero, color = c.ink)
            } else {
                // Net drawn out: word carries the sign, amount stays legible.
                Text("Net sent", style = CamelType.LedgerLabel, color = c.ink3)
                AmountText(-all.net, currency, style = CamelType.LedgerHero, color = c.ink)
            }
            ConnectorLine()
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("Money in", style = CamelType.LedgerLabel, color = c.ink3)
                    AmountText(all.incoming, currency, style = CamelType.LedgerAmount, color = c.green)
                }
                Column {
                    Text("Money out", style = CamelType.LedgerLabel, color = c.ink3)
                    AmountText(all.outgoing, currency, style = CamelType.LedgerAmount, color = c.ink)
                }
            }
            if (state.capturePaused) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Capture is paused. New payments are not being recorded.",
                    style = CamelType.VoiceBody, color = c.amber,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Latest", style = CamelType.VoiceTitle, color = c.ink, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        val shown = sortedEntries(state.entries, deleted = false).take(6)
        if (shown.isEmpty()) {
            StateBlock(
                headline = "The book is empty",
                sentence = "Add a payment by hand, or send pasted text for review.",
                modifier = Modifier.padding(top = 16.dp),
                actions = {},
            )
        } else {
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shown.forEach { entry ->
                    LedgerRow(entry = entry, onClick = { onOpen(entry.id) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------- Activity

@Composable
private fun ActivityScreen(state: CashflowState, onOpen: (String) -> Unit) {
    val c = LocalCashflowColors.current
    var query by rememberSaveable { mutableStateOf("") }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    val pool = sortedEntries(state.entries, deleted = showDeleted)
    val filtered = if (query.isBlank()) pool else pool.filter {
        it.counterparty.contains(query, ignoreCase = true) ||
            it.account.contains(query, ignoreCase = true) ||
            it.note.contains(query, ignoreCase = true) ||
            it.source.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Activity",
            style = CamelType.VoiceTitle, color = c.ink,
            modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        )
        PaperField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search notes, people, accounts",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp)) {
            PaperChip("Recently deleted", selected = showDeleted, onClick = { showDeleted = !showDeleted }, glyph = Icons.Outlined.Delete)
            Spacer(Modifier.width(8.dp))
            Text("${filtered.size} kept", style = CamelType.LedgerLabel, color = c.ink3)
        }
        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            StateBlock(
                headline = if (showDeleted) "Nothing deleted" else "No entries found",
                sentence = if (showDeleted) "Deleted entries can be restored here."
                else "Try fewer words, or add a first entry.",
            )
        } else {
            // Grouped under the entries' own real month headers; no invented dates.
            var lastMonth: String? = null
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filtered.forEach { entry ->
                    val header = PaperDates.dayNumber(entry.date)?.let { day ->
                        val (y, m, _) = PaperDates.daysToCivil(day)
                        "${PaperDates.monthName(m)} $y"
                    } ?: "Unreadable date"
                    if (header != lastMonth) {
                        lastMonth = header
                        Column {
                            Text(header, style = CamelType.LedgerLabel, color = c.ink3)
                            ConnectorLine(seed = header.hashCode() / 1000)
                        }
                    }
                    LedgerRow(entry = entry, onClick = { onOpen(entry.id) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------- Settings

@Composable
private fun SettingsScreen(
    state: CashflowState,
    actions: CashflowActions,
    toast: (String) -> Unit,
    onReopenGuide: () -> Unit,
    onPaste: () -> Unit,
) {
    val c = LocalCashflowColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Settings",
            style = CamelType.VoiceTitle, color = c.ink,
            modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // Currency: the book's count. Real sources know theirs.
        Text("Count this money", style = CamelType.LedgerLabel, color = c.ink3, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        Money.currencies.chunked(6).forEach { rowCodes ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowCodes.forEach { code ->
                    PaperChip(code, state.currency == code, onClick = { actions.setCurrency(code) })
                }
            }
        }
        Text(
            "Totals count one currency at a time. Entries in other currencies are kept, not counted.",
            style = CamelType.LedgerBody, color = c.ink3,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )

        Spacer(Modifier.height(12.dp))
        // Capture control: pause, confirmations, sources, platform permissions.
        ToggleRow("Capture paused", if (state.capturePaused) "Nothing is recorded right now" else "Payments are recorded", state.capturePaused, actions::setCapturePaused, tint = if (state.capturePaused) c.amber else c.green)
        ToggleRow(
            "Confirmation notifications",
            "Clear entries are saved automatically. Undo or edit afterwards.",
            state.confirmations,
            actions::setConfirmations,
        )

        if (state.capture.sources.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Apps to watch", style = CamelType.LedgerLabel, color = c.ink3, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "Selected apps send their payment alerts into Activity or Review.",
                style = CamelType.LedgerBody, color = c.ink3,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            state.capture.sources.forEach { source ->
                ToggleRow(source.name, null, source.selected, onChecked = { selected -> actions.selectSource(source.id, selected) })
            }
        }

        Spacer(Modifier.height(10.dp))
        SettingRow(
            icon = Icons.Outlined.Check,
            title = "Capture access",
            value = when (state.capture.platform) {
                "iOS" -> "Managed in Shortcuts"
                else -> if (state.capture.accessGranted) "Granted" else "Needed so the app can read payment alerts"
            },
            onClick = { actions.openCaptureSettings() },
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        SettingRow(
            icon = Icons.Outlined.Notifications,
            title = "App notifications (posting)",
            value = "A separate permission, only for Cashflow's own capture alerts",
            onClick = { actions.requestNotificationPermission() },
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        SettingRow(
            icon = Icons.Outlined.Refresh,
            title = "Refresh capture state",
            onClick = { actions.refreshCapture() },
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Text(
            state.capture.lastReceived?.let { "Last alert received: $it" }
                ?: "No alert received yet.",
            style = CamelType.LedgerBody,
            color = c.ink2,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(14.dp))
        GuideCard(
            headline = "Local processing",
            body = "This build does not send alerts to a model.",
        )

        Spacer(Modifier.height(14.dp))
        Row(Modifier.padding(horizontal = 18.dp)) {
            PaperChip("Paste payment text", selected = false, onClick = onPaste)
            Spacer(Modifier.width(8.dp))
            PaperChip("Export CSV", selected = false, onClick = {
                val problem = actions.exportCsv()
                // null means the share sheet opened; an error never claims success.
                toast(problem ?: "Share sheet opened.")
            })
            Spacer(Modifier.width(8.dp))
            PaperChip("Reopen the setup guide", selected = false, onClick = onReopenGuide)
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** A settings line with a switch: label, one optional status sentence, the control. */
@Composable
private fun ToggleRow(
    title: String,
    value: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    tint: Color? = null,
) {
    val c = LocalCashflowColors.current
    SettingRow(
        icon = Icons.Outlined.Notifications,
        title = title,
        value = value,
        tint = tint,
        trailing = {
            // The switch is display-only; the row itself is the accessible
            // toggle, so there is one toggleable node per line, not two.
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = c.green, uncheckedTrackColor = c.inset),
            )
        },
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .semantics(mergeDescendants = true) { },
    )
}

// ---------------------------------------------------------------- Dialogs & guide

private const val PASTE_LIMIT = 2000

@Composable
private fun PasteDialog(onCapture: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LocalCashflowColors.current
    var text by remember { mutableStateOf("") }
    val valid = text.isNotBlank() && text.length <= PASTE_LIMIT
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.sop,
        title = { Text("Paste payment text", style = CamelType.VoiceTitle, color = c.ink) },
        text = {
            Column {
                Text(
                    "The pasted words are read on this device only. Clear debit or credit alerts go straight into Activity; alerts Cashflow is not sure about wait in Review.",
                    style = CamelType.VoiceBody, color = c.ink2,
                )
                Spacer(Modifier.height(12.dp))
                PaperField(
                    value = text,
                    onValueChange = { if (it.length <= PASTE_LIMIT) text = it },
                    placeholder = "Paste a payment message here",
                    singleLine = false,
                    minLines = 4,
                )
                if (text.length == PASTE_LIMIT) {
                    Spacer(Modifier.height(6.dp))
                    Text("That is the readable limit.", style = CamelType.LedgerBody, color = c.amber)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCapture(text.trim()) }, enabled = valid) {
                Text("Check alert", style = CamelType.VoiceBody, color = if (valid) c.green else c.ink4)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = CamelType.VoiceBody, color = c.ink2)
            }
        },
    )
}

@Composable
private fun GuideOverlay(state: CashflowState, actions: CashflowActions, onClose: () -> Unit) {
    val c = LocalCashflowColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.paper)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("How this book works", style = CamelType.VoiceHeadline, color = c.ink)
        Spacer(Modifier.height(10.dp))
        if (state.capture.platform == "Android") {
            GuideCard(
                headline = "1. Allow capture, then confirm",
                body = "Cashflow reads notifications from the apps you allow. Clear debit or credit alerts go straight into Recorded activity; alerts Cashflow is not sure about wait in Review for your eye. Choose allowed apps in Settings.",
                extra = {
                    app.cashflow.ui.components.GreenButton(
                        "Open capture access settings",
                        { actions.openCaptureSettings() },
                        Modifier.width(240.dp),
                    )
                },
            )
        } else {
            GuideCard(
                headline = "1. Connect Shortcuts to Cashflow",
                body = "In Shortcuts, build an automation for your banking apps' notifications that calls the Cashflow action \"Capture transaction notification\". Clear debit or credit alerts go straight into Recorded activity; uncertain alerts wait in Review. Shortcuts setup stays yours to manage.",
                extra = {
                    app.cashflow.ui.components.GreenButton(
                        "Open Shortcuts setup",
                        { actions.openCaptureSettings() },
                        Modifier.width(240.dp),
                    )
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        GuideCard(
            headline = "2. Keep the counts honest",
            body = "Totals count kept entries in the chosen currency, completed, all time; own transfers are excluded. Nothing is sent anywhere in this build. Export a CSV yourself when you want the numbers out.",
        )
        Spacer(Modifier.height(18.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            app.cashflow.ui.components.GreenButton("Close guide", onClose, Modifier.width(200.dp))
            Spacer(Modifier.height(10.dp))
            Text("You can reopen this guide any time from Settings.", style = CamelType.LedgerBody, color = c.ink3)
        }
    }
}
