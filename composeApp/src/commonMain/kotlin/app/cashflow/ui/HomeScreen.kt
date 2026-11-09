package app.cashflow.ui

import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import app.cashflow.ui.theme.LocalMotion
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Direction
import app.cashflow.core.Entry
import app.cashflow.core.Money
import app.cashflow.core.totals
import app.cashflow.ui.components.AmountText
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.components.CurrencyGlyphPicker
import app.cashflow.ui.components.FeedRow
import app.cashflow.ui.components.FlowChart
import app.cashflow.ui.components.FlowSeries
import app.cashflow.ui.components.Ictl
import app.cashflow.ui.components.Kicker
import app.cashflow.ui.components.OptionChip
import app.cashflow.ui.components.PaperField
import app.cashflow.ui.components.StateBlock
import app.cashflow.ui.components.GhostButton
import app.cashflow.ui.components.signedMoney
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.PaperDates
import app.cashflow.ui.util.Period

/** Entries newest-first by real date; unreadable dates go last. */
internal fun sortedEntries(entries: List<Entry>, deleted: Boolean): List<Entry> =
    entries.filter { it.deleted == deleted }
        .sortedWith(
            compareByDescending<Entry> { PaperDates.dayNumber(it.date) ?: Long.MIN_VALUE }
                .thenByDescending { it.capturedAt }
        )

private class DayHeader(val iso: String, val sum: app.cashflow.core.Totals?)

private enum class DirFilter(val label: String) { ALL("Any direction"), IN("Money in"), OUT("Money out") }
private enum class SourceFilter(val label: String) { ALL("Any source"), CAPTURE("Alerts"), MANUAL("Manual") }

private fun Period.label() = when (this) {
    Period.DAY -> "Today"
    Period.WEEK -> "7 days"
    Period.MONTH -> "This month"
    Period.ALL -> "All time"
}

private fun inPeriod(entry: Entry, period: Period, today: Long?): Boolean {
    if (period == Period.ALL || today == null) return true
    val day = PaperDates.dayNumber(entry.date) ?: return false
    return when (period) {
        Period.DAY -> day == today
        Period.WEEK -> day in (today - 6)..today
        Period.MONTH -> PaperDates.sameCalendarMonth(day, today)
        Period.ALL -> true
    }
}

/**
 * Activity-first Home: compact header, In/Out pair with Net, then the
 * date-grouped edge-to-edge feed. Search and filter controls appear only when
 * their icon is toggled.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: CashflowState,
    listState: LazyListState,
    onSetCurrency: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    actions: CashflowActions,
    onGoals: () -> Unit,
) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    val today = PaperDates.dayNumber(state.today)
    var periodIndex by rememberSaveable { mutableStateOf(Period.ALL.ordinal) }
    val period = Period.entries[periodIndex]
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var dirIndex by rememberSaveable { mutableStateOf(0) }
    var srcIndex by rememberSaveable { mutableStateOf(0) }
    val dir = DirFilter.entries[dirIndex]
    val src = SourceFilter.entries[srcIndex]

    val filterCount = (if (dir != DirFilter.ALL) 1 else 0) + (if (src != SourceFilter.ALL) 1 else 0)
    val queryActive = query.isNotBlank()
    val scoped = period != Period.ALL || filterCount > 0 || queryActive

    val filtered = remember(state.entries, dir, src, query) {
        sortedEntries(state.entries, deleted = false).filter { e ->
            (dir == DirFilter.ALL || (dir == DirFilter.IN) == (e.direction == Direction.IN)) &&
                (src == SourceFilter.ALL || (src == SourceFilter.MANUAL) == (e.source == "Manual")) &&
                (query.isBlank() ||
                    e.counterparty.contains(query, true) || e.note.contains(query, true) ||
                    e.account.contains(query, true) || e.source.contains(query, true))
        }
    }
    val visible = remember(filtered, period, today) { filtered.filter { inPeriod(it, period, today) } }
    val sum = remember(visible, state.currency) {
        runCatching { totals(visible, state.currency) }.getOrNull()
    }
    val otherCurrencies = visible.any { it.currency != state.currency }
    val chartPoints = remember(filtered, state.currency, period, today) {
        FlowSeries.build(filtered, state.currency, period, today)
    }
    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchOpen) { if (searchOpen) runCatching { searchFocus.requestFocus() } }

    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            actions.refreshCapture()
            actions.refreshSubscription()
            scope.launch { delay(650); refreshing = false }
        },
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState, isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                containerColor = c.surface, color = c.citron,
            )
        },
    ) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp).height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (searchOpen) {
                        PaperField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Search name, note or account",
                            leading = CFIcons.Magnifier,
                            modifier = Modifier.weight(1f),
                            focusRequester = searchFocus,
                        )
                        Ictl(Icons.Outlined.Close, "Close search", onClick = { searchOpen = false; query = "" })
                    } else {
                        CurrencyGlyphPicker(
                            selected = state.currency,
                            onSelect = { it?.let(onSetCurrency) },
                            descriptionBase = "View currency",
                        )
                        Spacer(Modifier.weight(1f))
                        Ictl(
                            CFIcons.Magnifier,
                            if (queryActive) "Search, active: $query" else "Search",
                            onClick = { searchOpen = true },
                            on = queryActive,
                        )
                        Ictl(Icons.Outlined.Settings, "Settings", onClick = onSettings)
                    }
                }
            }
        }

        item {
            val sel = selectedPoint?.let { chartPoints.getOrNull(it) }
            val netTarget: Long? = if (sel != null) sel.inMinor - sel.outMinor else sum?.net
            val animNet = remember { Animatable(0f) }
            LaunchedEffect(netTarget, sel != null) {
                if (netTarget != null) {
                    if (motion.reduced || sel != null) animNet.snapTo(netTarget.toFloat())
                    else animNet.animateTo(netTarget.toFloat(), tween(700, easing = FastOutSlowInEasing))
                }
            }
            val shownNet = if (netTarget == null) null else if (animNet.isRunning) animNet.value.toLong() else netTarget
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Kicker(if (sel != null) sel.label else if (scoped) "Matched net" else "Net flow")
                        Spacer(Modifier.weight(1f))
                        PeriodTabs(period, enabled = today != null) { periodIndex = it.ordinal; selectedPoint = null }
                    }
                    Spacer(Modifier.height(6.dp))
                    val netText = if (shownNet == null) "Too large to total" else signedMoney(shownNet, state.currency)
                    Text(
                        text = netText,
                        style = CamelType.LedgerTile.copy(
                            fontSize = if (netText.length > 14) 30.sp else 42.sp,
                            lineHeight = if (netText.length > 14) 36.sp else 48.sp,
                            letterSpacing = (-1).sp,
                        ),
                        color = if (netTarget != null && netTarget > 0) c.citron else c.ink,
                        maxLines = 1,
                    )
                    Text(
                        text = if (sel != null) "In ${Money.display(sel.inMinor, state.currency)}  ·  Out ${Money.display(sel.outMinor, state.currency)}"
                        else "${period.span()} · ${state.currency}",
                        style = CamelType.LedgerLabel, color = c.muted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowChart(chartPoints, state.currency, selectedPoint, { selectedPoint = it }, style = state.chartStyle)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SplitBar(sum?.incoming, sum?.outgoing, state.currency)
                    val headline = state.insights.headline
                    if (headline != null && !scoped) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (headline.positive == false) CFIcons.ArrowOut else CFIcons.ArrowIn, null,
                                tint = if (headline.positive == false) c.ink2 else c.citron, modifier = Modifier.size(16.dp),
                            )
                            Text(headline.text, style = CamelType.LedgerLabel, color = c.ink2)
                        }
                    }
                    if (otherCurrencies) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Totals count ${state.currency} only. Other currencies stay in the feed.",
                            style = CamelType.LedgerLabel, color = c.muted,
                        )
                    }
                    if (state.capturePaused) {
                        Spacer(Modifier.height(6.dp))
                        Text("Capture is paused. New alerts are not being recorded.", style = CamelType.LedgerLabel, color = c.lavender)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (state.transferSuggestions.isNotEmpty()) item { TransferQuestion(state, actions) }
        item { GoalsStrip(state, onGoals) }
        item {
            // The gap between the chart and the feed: a section label on the left, the filter on the right.
            Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Kicker("Recent activity")
                    if (visible.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text("${visible.size}", style = CamelType.LedgerLabel, color = c.faint)
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        Ictl(
                            CFIcons.Funnel,
                            if (filterCount > 0) "Filter, $filterCount active" else "Filter",
                            onClick = { filterOpen = !filterOpen },
                            on = filterOpen || filterCount > 0,
                            count = filterCount,
                        )
                        FilterPopover(
                            expanded = filterOpen, onDismiss = { filterOpen = false },
                            dir = dir, src = src, activeCount = filterCount,
                            onDir = { dirIndex = it.ordinal }, onSrc = { srcIndex = it.ordinal },
                            onReset = { dirIndex = 0; srcIndex = 0 },
                        )
                    }
                }
                if (filterCount > 0 || (queryActive && !searchOpen)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (queryActive && !searchOpen) {
                            ActiveChip("“$query”", CFIcons.Magnifier, "Clear search") { query = "" }
                        }
                        if (dir != DirFilter.ALL) {
                            ActiveChip(dir.label, if (dir == DirFilter.IN) CFIcons.ArrowIn else CFIcons.ArrowOut, "Remove direction filter") { dirIndex = 0 }
                        }
                        if (src != SourceFilter.ALL) {
                            ActiveChip(src.label, if (src == SourceFilter.CAPTURE) CFIcons.Tray else CFIcons.Receipt, "Remove source filter") { srcIndex = 0 }
                        }
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                if (scoped && state.entries.any { !it.deleted }) {
                    StateBlock(
                        headline = "Nothing matched",
                        sentence = "No record fits this period, filter or search.",
                        modifier = Modifier.padding(top = 24.dp),
                        actions = {
                            GhostButton("Clear all", {
                                periodIndex = Period.ALL.ordinal; dirIndex = 0; srcIndex = 0; query = ""
                            })
                        },
                    )
                } else {
                    StateBlock(
                        headline = "Nothing recorded yet",
                        sentence = "Write a record with the plus below, or let a payment alert arrive.",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        } else {
            // Flatten into header and row items so the feed stays lazy. Each day carries its own net.
            val byDay = visible.groupBy { PaperDates.isoDateOnly(it.date) }
            val rows = buildList<Any> {
                var last: String? = null
                visible.forEach { e ->
                    val key = PaperDates.isoDateOnly(e.date)
                    if (key != last) {
                        last = key
                        add(DayHeader(key, runCatching { totals(byDay[key].orEmpty(), state.currency) }.getOrNull()))
                    }
                    add(e)
                }
            }
            items(rows) { item ->
                if (item is DayHeader) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(groupLabel(item.iso, today), style = CamelType.GroupHeader, color = c.muted)
                        Spacer(Modifier.weight(1f))
                        val net = item.sum?.net
                        if (net != null && (item.sum.incoming != 0L || item.sum.outgoing != 0L)) {
                            Text(
                                signedMoney(net, state.currency),
                                style = CamelType.LedgerLabel,
                                color = if (net > 0) c.citron else c.muted,
                            )
                        }
                    }
                } else if (item is Entry) {
                    FeedRow(item, onClick = { onOpen(item.id) }, showCurrencyCode = item.currency != state.currency)
                }
            }
        }
        // Room for the floating dock.
        item { Spacer(Modifier.height(120.dp)) }
    }
    }
}

private fun groupLabel(iso: String, today: Long?): String {
    val day = PaperDates.dayNumber(iso)
    return when {
        day != null && today != null && day == today -> "Today"
        day != null && today != null && day == today - 1 -> "Yesterday"
        else -> PaperDates.dateLabel(iso)
    }
}

/** Quiet text tabs: the chosen period is a citron wash, the rest are plain. No boxes competing with the number. */
@Composable
private fun PeriodTabs(period: Period, enabled: Boolean, onPick: (Period) -> Unit) {
    val c = LocalCashflowColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Period.entries.forEach { p ->
            val on = p == period
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) c.citronWash else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(enabled = enabled) { onPick(p) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .semantics { contentDescription = "Period: ${p.label()}" + if (on) ", selected" else "" },
                contentAlignment = Alignment.Center,
            ) {
                Text(p.shortLabel(), style = CamelType.LedgerLabel, color = if (on) c.citron else c.muted)
            }
        }
    }
}

private fun Period.span() = when (this) {
    Period.DAY -> "Last 7 days, ending today"
    Period.WEEK -> "Last 7 days"
    Period.MONTH -> "This month"
    Period.ALL -> "All recorded time"
}

private fun Period.shortLabel() = when (this) {
    Period.DAY -> "Today"
    Period.WEEK -> "7D"
    Period.MONTH -> "Month"
    Period.ALL -> "All"
}

/** A filter currently applied, removable in one tap. */
@Composable
private fun ActiveChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onRemove: () -> Unit) {
    val c = LocalCashflowColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.citronWash)
            .clickable(onClick = onRemove)
            .padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = c.citron, modifier = Modifier.size(14.dp))
        Text(label, style = CamelType.LedgerLabel, color = c.citron, maxLines = 1)
        Icon(Icons.Outlined.Close, null, tint = c.citron, modifier = Modifier.size(14.dp))
    }
}

/** Anchored under the funnel: choices apply immediately, so there is nothing to confirm. */
@Composable
private fun FilterPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    dir: DirFilter,
    src: SourceFilter,
    activeCount: Int,
    onDir: (DirFilter) -> Unit,
    onSrc: (SourceFilter) -> Unit,
    onReset: () -> Unit,
) {
    val c = LocalCashflowColors.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = c.sheet) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).width(250.dp)) {
            Kicker("Direction")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconChip("Any", null, dir == DirFilter.ALL, { onDir(DirFilter.ALL) })
                IconChip("In", CFIcons.ArrowIn, dir == DirFilter.IN, { onDir(DirFilter.IN) })
                IconChip("Out", CFIcons.ArrowOut, dir == DirFilter.OUT, { onDir(DirFilter.OUT) })
            }
            Spacer(Modifier.height(14.dp))
            Kicker("Source")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconChip("Any", null, src == SourceFilter.ALL, { onSrc(SourceFilter.ALL) })
                IconChip("Alerts", CFIcons.Tray, src == SourceFilter.CAPTURE, { onSrc(SourceFilter.CAPTURE) })
                IconChip("Manual", CFIcons.Receipt, src == SourceFilter.MANUAL, { onSrc(SourceFilter.MANUAL) })
            }
            if (activeCount > 0) {
                Spacer(Modifier.height(12.dp))
                GhostButton("Reset filters", onReset, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun IconChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val c = LocalCashflowColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.citron else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics { contentDescription = label + if (selected) ", selected" else "" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (selected) c.onCitron else c.muted, modifier = Modifier.size(14.dp))
        Text(label, style = CamelType.LedgerLabel, color = if (selected) c.onCitron else c.muted)
    }
}

/** Where the money went, as one bar: the citron share came in, the pale share went out. */
@Composable
private fun SplitBar(incoming: Long?, outgoing: Long?, currency: String) {
    val c = LocalCashflowColors.current
    val total = (incoming ?: 0L) + (outgoing ?: 0L)
    val share = if (incoming == null || outgoing == null || total <= 0L) 0.5f
    else (incoming.toFloat() / total.toFloat()).coerceIn(0.03f, 0.97f)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(CFIcons.ArrowIn, null, tint = c.citron, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(incoming?.let { Money.display(it, currency) } ?: "—", style = CamelType.LedgerLabel, color = c.citron, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(outgoing?.let { Money.display(it, currency) } ?: "—", style = CamelType.LedgerLabel, color = c.ink2, maxLines = 1)
            Spacer(Modifier.width(5.dp))
            Icon(CFIcons.ArrowOut, null, tint = c.ink2, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(8.dp))
        val empty = total <= 0L
        Row(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.weight(share).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (empty) c.field else c.citron))
            Box(Modifier.weight(1f - share).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (empty) c.field else c.ink2.copy(alpha = 0.55f)))
        }
    }
}
