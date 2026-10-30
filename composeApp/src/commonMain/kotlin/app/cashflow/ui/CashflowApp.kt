package app.cashflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Entry
import app.cashflow.core.EntryDraft
import app.cashflow.core.Money
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.components.CountBadge
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.CashflowTheme
import app.cashflow.ui.theme.LocalCashflowColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// One stack above Home: an open editor covers whatever is under it.
private data class EditorRoute(val draft: EntryDraft, val entryId: String?, val reviewId: String?)

private data class ToastMsg(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

/**
 * CashflowApp: the root shell. Home is the base; Add, Review, record detail
 * and Settings stack above it. The floating dock is only visible when none of
 * those is open.
 */
@Composable
fun CashflowApp(state: CashflowState, actions: CashflowActions) {
    var reduceMotion by rememberSaveable { mutableStateOf(false) }
    CashflowTheme(reduceMotion = reduceMotion) {
        val c = LocalCashflowColors.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        var editor by remember { mutableStateOf<EditorRoute?>(null) }
        var detailId by remember { mutableStateOf<String?>(null) }
        var reviewOpen by rememberSaveable { mutableStateOf(false) }
        val latestState by rememberUpdatedState(state)
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var replayStep by remember { mutableStateOf<Int?>(null) }
        var toast by remember { mutableStateOf<ToastMsg?>(null) }

        // Errors from any action surface once, then clear.
        LaunchedEffect(state.error) {
            state.error?.let {
                toast = ToastMsg(it)
                actions.clearError()
            }
        }
        // A notification's Edit request opens the editor for the CURRENT entry.
        LaunchedEffect(state.requestedEntryId) {
            state.requestedEntryId?.let { id ->
                state.entries.firstOrNull { it.id == id && !it.deleted }?.let { entry ->
                    editor = EditorRoute(draftFromEntry(entry), entryId = entry.id, reviewId = null)
                }
                actions.clearFocus()
            }
        }

        // Newly deleted records and newly ignored alerts offer Undo, whichever screen did it.
        val deletedIds = state.entries.filter { it.deleted }.map { it.id }.toSet()
        var prevDeleted by remember { mutableStateOf(deletedIds) }
        LaunchedEffect(deletedIds) {
            val added = deletedIds - prevDeleted
            prevDeleted = deletedIds
            added.singleOrNull()?.let { id ->
                toast = ToastMsg("Record deleted", "Undo") { actions.restore(id)?.let { toast = ToastMsg(it) } }
            }
        }
        val ignoredIds = state.ignoredReview.map { it.id }.toSet()
        var prevIgnored by remember { mutableStateOf(ignoredIds) }
        LaunchedEffect(ignoredIds) {
            val added = ignoredIds - prevIgnored
            prevIgnored = ignoredIds
            added.singleOrNull()?.let { id ->
                toast = ToastMsg("Alert ignored", "Undo") { actions.restoreReview(id)?.let { toast = ToastMsg(it) } }
            }
        }
        // A detail whose record was deleted closes itself.
        LaunchedEffect(detailId, state.entries) {
            val id = detailId ?: return@LaunchedEffect
            val entry = state.entries.firstOrNull { it.id == id }
            if (entry == null || entry.deleted) { detailId = null }
        }

        // First run (or replay): the intro owns the screen.
        if (!state.onboardingComplete) {
            IntroScreen(state, actions, state.onboardingStep, actions::setOnboardingStep, actions::finishOnboarding)
            return@CashflowTheme
        }
        replayStep?.let { step ->
            IntroScreen(state, actions, step, { replayStep = it }, { replayStep = null })
            return@CashflowTheme
        }

        val anyOverlay = editor != null || reviewOpen || settingsOpen || detailId != null

        Box(Modifier.fillMaxSize().background(c.bg)) {
            HomeScreen(
                state = state,
                listState = listState,
                onSetCurrency = actions::setCurrency,
                onOpen = { detailId = it },
                onSettings = { settingsOpen = true },
            )

            if (!anyOverlay) {
                Dock(
                    reviewCount = state.review.size,
                    onHome = { scope.launch { listState.animateScrollToItem(0) } },
                    onAdd = { editor = EditorRoute(EntryDraft(currency = state.currency), null, null) },
                    onReview = { reviewOpen = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (reviewOpen) {
                Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
                    ReviewScreen(
                        state, actions,
                        onEdit = { item -> editor = EditorRoute(item.draft, null, item.id) },
                        onClose = { reviewOpen = false },
                    )
                }
            }

            detailId?.let { id ->
                state.entries.firstOrNull { it.id == id && !it.deleted }?.let { entry ->
                    EntryDetailSheet(
                        entry = entry,
                        actions = actions,
                        onEdit = { editor = EditorRoute(draftFromEntry(entry), entry.id, null) },
                        onBack = { detailId = null },
                    )
                }
            }

            if (settingsOpen) {
                SettingsSheet(
                    state = state,
                    actions = actions,
                    reduceMotion = reduceMotion,
                    onReduceMotion = { reduceMotion = it },
                    onReplayIntro = { settingsOpen = false; replayStep = 0 },
                    onToast = { toast = ToastMsg(it) },
                    onDismiss = { settingsOpen = false },
                )
            }

            editor?.let { route ->
                EntryEditorScreen(
                    draft = route.draft,
                    entryId = route.entryId,
                    reviewId = route.reviewId,
                    state = state,
                    actions = actions,
                    onBack = { editor = null },
                    onSaved = { savedId ->
                        val fromReview = route.reviewId != null
                        // save() returns no id for new records: find the one that appears.
                        val knownIds = state.entries.mapTo(HashSet()) { it.id }
                        editor = null
                        if (!fromReview) detailId = null
                        toast = ToastMsg("Saved", "View record") {
                            val current = latestState.entries
                            val target = savedId?.let { id -> current.firstOrNull { it.id == id } }
                                ?: current.firstOrNull { it.id !in knownIds }
                            if (target != null) {
                                actions.setCurrency(target.currency)
                                reviewOpen = false
                                detailId = target.id
                            }
                        }
                    },
                )
            }

            toast?.let { msg ->
                ToastBar(
                    msg,
                    onDismiss = { toast = null },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    raised = !anyOverlay,
                )
            }
        }
    }
}

private fun draftFromEntry(e: Entry) = EntryDraft(
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

/**
 * Icon-only floating dock: Home left, citron Add at the geometric center
 * (three equal columns), Review tray with count badge right. The active
 * destination is a citron glyph with an under-dot, never a box.
 */
@Composable
private fun Dock(
    reviewCount: Int,
    onHome: () -> Unit,
    onAdd: () -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCashflowColors.current
    Row(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .width(240.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(c.surface)
            .border(1.dp, c.hairline, RoundedCornerShape(32.dp))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockSlot(Modifier.weight(1f)) {
            DockButton(Icons.Outlined.Home, "Home, current page", active = true, onClick = onHome)
        }
        DockSlot(Modifier.weight(1f)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(c.citron)
                    .clickable(onClick = onAdd)
                    .semantics { contentDescription = "Add a record" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, null, tint = c.onCitron, modifier = Modifier.size(26.dp))
            }
        }
        DockSlot(Modifier.weight(1f)) {
            DockButton(
                CFIcons.Tray,
                if (reviewCount > 0) "Review, $reviewCount waiting" else "Review, nothing waiting",
                active = false, count = reviewCount, onClick = onReview,
            )
        }
    }
}

@Composable
private fun DockSlot(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    count: Int = 0,
) {
    val c = LocalCashflowColors.current
    Column(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box {
            Icon(icon, null, tint = if (active) c.citron else c.muted, modifier = Modifier.size(24.dp).padding(0.dp))
            if (count > 0) CountBadge(count, Modifier.align(Alignment.TopEnd).padding(start = 14.dp))
        }
        Box(
            Modifier.padding(top = 4.dp).size(4.dp).clip(CircleShape)
                .background(if (active) c.citron else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

/** Toast with an optional action (Undo, View record); dismisses itself. */
@Composable
private fun ToastBar(msg: ToastMsg, onDismiss: () -> Unit, modifier: Modifier, raised: Boolean) {
    val c = LocalCashflowColors.current
    LaunchedEffect(msg) {
        delay(if (msg.action != null) 6000 else 3500)
        onDismiss()
    }
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = if (raised) 92.dp else 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.sheet)
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 6.dp)
            .semantics { contentDescription = msg.text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(msg.text, style = CamelType.VoiceBody, color = c.ink, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
        if (msg.actionLabel != null && msg.action != null) {
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDismiss(); msg.action.invoke() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(msg.actionLabel, style = CamelType.VoiceBody, color = c.citron)
            }
        }
    }
}
