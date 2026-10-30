package app.cashflow.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.core.CashflowState
import app.cashflow.core.Direction
import app.cashflow.core.Entry
import app.cashflow.core.Money
import app.cashflow.core.totals
import app.cashflow.ui.components.AmountText
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.components.CurrencyGlyphPicker
import app.cashflow.ui.components.FeedRow
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
        .sortedByDescending { PaperDates.dayNumber(it.date) ?: Long.MIN_VALUE }

private enum class DirFilter(val label: String) { ALL("Any direction"), IN("In"), OUT("Out") }
private enum class SourceFilter(val label: String) { ALL("Any source"), CAPTURE("Captured"), MANUAL("Manual") }

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
@Composable
internal fun HomeScreen(
    state: CashflowState,
    listState: LazyListState,
    onSetCurrency: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalCashflowColors.current
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

    val visible = remember(state.entries, period, today, dir, src, query) {
        sortedEntries(state.entries, deleted = false).filter { e ->
            inPeriod(e, period, today) &&
                (dir == DirFilter.ALL || (dir == DirFilter.IN) == (e.direction == Direction.IN)) &&
                (src == SourceFilter.ALL || (src == SourceFilter.MANUAL) == (e.source == "Manual")) &&
                (query.isBlank() ||
                    e.counterparty.contains(query, true) || e.note.contains(query, true) ||
                    e.account.contains(query, true) || e.source.contains(query, true))
        }
    }
    val sum = remember(visible, state.currency) {
        runCatching { totals(visible, state.currency) }.getOrNull()
    }
    val otherCurrencies = visible.any { it.currency != state.currency }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CurrencyGlyphPicker(
                        selected = state.currency,
                        onSelect = { it?.let(onSetCurrency) },
                        descriptionBase = "View currency",
                    )
                    Spacer(Modifier.width(8.dp))
                    PeriodSelector(period, enabled = today != null) { periodIndex = it.ordinal }
                    Spacer(Modifier.weight(1f))
                    Ictl(
                        CFIcons.Magnifier,
                        if (queryActive) "Search, active: $query" else "Search",
                        onClick = { searchOpen = !searchOpen },
                        on = searchOpen || queryActive,
                    )
                    Ictl(
                        CFIcons.Funnel,
                        if (filterCount > 0) "Filter, $filterCount active" else "Filter",
                        onClick = { filterOpen = !filterOpen },
                        on = filterOpen || filterCount > 0,
                        count = filterCount,
                    )
                    Ictl(Icons.Outlined.Settings, "Settings", onClick = onSettings)
                }
                AnimatedVisibility(searchOpen) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PaperField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Name, note, account",
                            leading = CFIcons.Magnifier,
                            modifier = Modifier.weight(1f),
                        )
                        if (queryActive) {
                            Ictl(Icons.Outlined.Close, "Clear search", onClick = { query = "" })
                        }
                    }
                }
                AnimatedVisibility(filterOpen) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DirFilter.entries.forEach {
                                OptionChip(it.label, dir == it, { dirIndex = it.ordinal })
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SourceFilter.entries.forEach {
                                OptionChip(it.label, src == it, { srcIndex = it.ordinal })
                            }
                        }
                        if (filterCount == 0) {
                            Spacer(Modifier.height(6.dp))
                            Text("No filter active", style = CamelType.LedgerLabel, color = c.muted)
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowTile("In", CFIcons.ArrowIn, sum?.incoming, state.currency, true, Modifier.weight(1f))
                    FlowTile("Out", CFIcons.ArrowOut, sum?.outgoing, state.currency, false, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Kicker(if (scoped) "Matched net" else "Net")
                    Spacer(Modifier.width(10.dp))
                    val net = sum?.net
                    Text(
                        text = if (net == null) "Too large to total" else signedMoney(net, state.currency),
                        style = CamelType.LedgerAmount,
                        color = if (net != null && net > 0) c.citron else c.ink,
                    )
                }
                if (otherCurrencies) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Totals count ${state.currency} only. Other currencies stay in the feed.",
                        style = CamelType.LedgerLabel, color = c.muted,
                    )
                }
                if (state.capturePaused) {
                    Spacer(Modifier.height(4.dp))
                    Text("Capture is paused. New alerts are not being recorded.", style = CamelType.LedgerLabel, color = c.lavender)
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
            // Flatten into header and row items so the feed stays lazy.
            val rows = buildList<Any> {
                var last: String? = null
                visible.forEach { e ->
                    val key = PaperDates.isoDateOnly(e.date)
                    if (key != last) { last = key; add(key) }
                    add(e)
                }
            }
            items(rows) { item ->
                if (item is String) {
                    Text(
                        text = groupLabel(item, today),
                        style = CamelType.GroupHeader,
                        color = c.muted,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                    )
                } else if (item is Entry) {
                    FeedRow(item, onClick = { onOpen(item.id) }, showCurrencyCode = item.currency != state.currency)
                }
            }
        }
        // Room for the floating dock.
        item { Spacer(Modifier.height(120.dp)) }
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

@Composable
private fun PeriodSelector(period: Period, enabled: Boolean, onPick: (Period) -> Unit) {
    val c = LocalCashflowColors.current
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .clickable(enabled = enabled) { open = true }
                .padding(horizontal = 12.dp, vertical = 11.dp)
                .semantics { contentDescription = "Period: ${period.label()}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(period.label(), style = CamelType.LedgerLabel, color = c.ink)
            Icon(CFIcons.Caret, null, tint = c.muted, modifier = Modifier.size(10.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.sheet) {
            Period.entries.forEach { p ->
                DropdownMenuItem(
                    text = {
                        Text(p.label(), style = CamelType.VoiceBody, color = if (p == period) c.citron else c.ink)
                    },
                    onClick = { open = false; onPick(p) },
                )
            }
        }
    }
}

@Composable
private fun FlowTile(
    caption: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    minor: Long?,
    currency: String,
    incoming: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = LocalCashflowColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (incoming) c.citron else c.muted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Kicker(caption)
        }
        Spacer(Modifier.height(4.dp))
        if (minor != null) {
            // Long values wrap and use the smaller style; never an ellipsis.
            val long = Money.display(minor, currency).length > 15
            AmountText(
                minor, currency,
                direction = if (incoming) Direction.IN else Direction.OUT,
                style = if (long) CamelType.LedgerTileLong else CamelType.LedgerTile,
            )
        } else {
            Text("—", style = CamelType.LedgerTile, color = c.muted)
        }
    }
}
