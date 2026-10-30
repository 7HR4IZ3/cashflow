package app.cashflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Direction
import app.cashflow.core.Entry
import app.cashflow.core.EntryDraft
import app.cashflow.core.Kind
import app.cashflow.core.Money
import app.cashflow.core.PaymentStatus
import app.cashflow.core.ReviewItem
import app.cashflow.ui.components.AmountText
import app.cashflow.ui.components.CFIcons
import app.cashflow.ui.components.CurrencyGlyphPicker
import app.cashflow.ui.components.DangerGhostButton
import app.cashflow.ui.components.GhostButton
import app.cashflow.ui.components.Ictl
import app.cashflow.ui.components.KvRow
import app.cashflow.ui.components.Kicker
import app.cashflow.ui.components.LabelledField
import app.cashflow.ui.components.OptionChip
import app.cashflow.ui.components.PaperField
import app.cashflow.ui.components.SheetOverlay
import app.cashflow.ui.components.SideNote
import app.cashflow.ui.components.StateBlock
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.util.MoneyWords
import app.cashflow.ui.util.PaperDates

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

/** Why an amount string cannot be recorded, or null when it parses. */
private fun amountProblem(text: String, currency: String): String? {
    val clean = text.trim().trimEnd('.')
    if (!MoneyWords.isSupported(currency)) return "Pick a currency, or leave this for later."
    if (clean.isEmpty()) return "Enter the amount."
    if (!Regex("[0-9]+(?:\\.[0-9]+)?").matches(clean)) return "Use digits and one decimal point."
    val exp = Money.exponent(currency)
    val decimals = clean.substringAfter('.', "").length
    if (decimals > exp) {
        return if (exp == 0) "$currency has no decimals." else "$currency allows at most $exp decimals."
    }
    if (Money.parse(clean, currency) == null) return "The amount must be more than zero."
    return null
}

private fun groupedAmount(text: String): String {
    val whole = text.substringBefore('.').ifEmpty { "0" }
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return if (text.contains('.')) grouped + "." + text.substringAfter('.') else grouped
}

private fun pressKey(current: String, key: String, currency: String): String {
    val exp = Money.exponent(currency)
    return when (key) {
        "<" -> current.dropLast(1)
        "." -> when {
            exp == 0 || current.contains('.') -> current
            current.isEmpty() -> "0."
            else -> "$current."
        }
        else -> {
            val digits = current.count { it.isDigit() }
            val decimals = current.substringAfter('.', "").length
            when {
                digits >= 15 -> current
                current.contains('.') && decimals >= exp -> current
                current == "0" -> key
                else -> current + key
            }
        }
    }
}

/** Kind forces direction for Income/Expense/Fee; reconcile either way. */
private fun kindAfterDirection(kind: Kind, direction: Direction): Kind = when {
    kind == Kind.INCOME && direction == Direction.OUT -> Kind.UNCLASSIFIED
    (kind == Kind.EXPENSE || kind == Kind.FEE) && direction == Direction.IN -> Kind.UNCLASSIFIED
    else -> kind
}

private fun statusLabel(s: PaymentStatus) = MoneyWords.status(s)

@Composable
private fun TopIcon(icon: ImageVector, description: String, onClick: () -> Unit, on: Boolean = false, tint: androidx.compose.ui.graphics.Color? = null) {
    Ictl(icon = icon, description = description, onClick = onClick, on = on, tint = tint)
}

/** Arrow + tiny In/Out caption; the arrow never carries the meaning alone. */
@Composable
private fun DirectionArrowButton(
    direction: Direction,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    val c = LocalCashflowColors.current
    val isIn = direction == Direction.IN
    val label = if (isIn) "In" else "Out"
    Column(
        modifier = modifier
            .heightIn(min = height)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.citronWash else c.surface)
            .then(if (selected) Modifier.border(1.dp, c.citron, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = (if (isIn) "Money in" else "Money out") + if (selected) ", selected" else ""
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isIn) CFIcons.ArrowIn else CFIcons.ArrowOut,
            contentDescription = null,
            tint = if (selected) c.citron else c.muted,
            modifier = Modifier.size(22.dp),
        )
        Text(label, style = CamelType.Kicker, color = if (selected) c.citron else c.muted)
    }
}

@Composable
private fun KeypadKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalCashflowColors.current
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (label == "<") "Delete last digit" else if (label == ".") "Decimal point" else label },
        contentAlignment = Alignment.Center,
    ) {
        if (label == "<") {
            Icon(CFIcons.Backspace, contentDescription = null, tint = c.ink, modifier = Modifier.size(24.dp))
        } else {
            Text(label, style = CamelType.VoiceTitle.copy(fontSize = 24.sp), color = c.ink)
        }
    }
}

/** Round citron check (or a muted disc when disabled). */
@Composable
private fun CheckButton(description: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val c = LocalCashflowColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(if (enabled) c.citron else c.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = if (enabled) description else "$description, unavailable" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            CFIcons.CheckMark,
            contentDescription = null,
            tint = if (enabled) c.onCitron else c.faint,
            modifier = Modifier.size(26.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Add / Edit calculator
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryEditorScreen(
    draft: EntryDraft,
    entryId: String?,
    reviewId: String?,
    state: CashflowState,
    actions: CashflowActions,
    onBack: () -> Unit,
    onSaved: (savedEntryId: String?) -> Unit,
) {
    val c = LocalCashflowColors.current
    val editing = entryId != null
    var amount by remember { mutableStateOf(draft.amount.trim()) }
    var currency by remember {
        mutableStateOf(if (MoneyWords.isSupported(draft.currency)) draft.currency.uppercase() else state.currency)
    }
    var direction by remember { mutableStateOf(draft.direction) }
    var kind by remember { mutableStateOf(draft.kind) }
    var status by remember { mutableStateOf(draft.status) }
    var account by remember { mutableStateOf(draft.account.takeIf { it != "Unassigned" } ?: "") }
    var name by remember { mutableStateOf(draft.counterparty) }
    var note by remember { mutableStateOf(draft.note) }
    var date by remember { mutableStateOf(draft.date.ifBlank { state.today }) }
    var details by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val valid = amountProblem(amount, currency) == null
    val context = if (editing && draft.counterparty.isNotBlank() && valid) {
        "${draft.counterparty} · ${Money.display(Money.parse(amount.trimEnd('.'), currency) ?: 0L, currency)}"
    } else if (editing) draft.counterparty.takeIf { it.isNotBlank() } else null

    fun save() {
        val clean = amount.trim().trimEnd('.')
        val result = actions.save(
            draft.copy(
                amount = clean,
                currency = currency,
                direction = direction,
                kind = kind,
                status = status,
                counterparty = name,
                account = account.ifBlank { "Unassigned" },
                note = note,
                date = date.trim(),
                directionConfirmed = true,
            ),
            entryId,
            reviewId,
        )
        if (result == null) onSaved(entryId) else error = result
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopIcon(Icons.Outlined.Close, "Cancel and close", onBack)
            Column(Modifier.weight(1f).padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (editing) "Edit record" else "Add record",
                    style = CamelType.VoiceBody.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink,
                )
                if (context != null) {
                    Text(context, style = CamelType.LedgerLabel, color = c.muted, textAlign = TextAlign.Center)
                } else if (!editing) {
                    Text("Write it in your record book", style = CamelType.LedgerLabel, color = c.muted)
                }
            }
            TopIcon(
                CFIcons.MoreDots,
                if (details) "Hide details" else "Show details: kind, status, account, name, note, date",
                { details = !details },
                on = details,
            )
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (details) 8.dp else 20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                CurrencyGlyphPicker(selected = currency, onSelect = { it?.let { code -> currency = code; amount = amount.let { a -> if (Money.exponent(code) == 0) a.substringBefore('.') else a.substringBefore('.') + (if (a.contains('.')) "." + a.substringAfter('.').take(Money.exponent(code)) else "") } } })
                Spacer(Modifier.width(10.dp))
                Kicker(currency)
            }
            Spacer(Modifier.height(10.dp))
            val shown = if (amount.isEmpty()) "0" else groupedAmount(amount)
            val digitCount = shown.length
            val size = when {
                digitCount <= 9 -> 52.sp
                digitCount <= 12 -> 40.sp
                digitCount <= 16 -> 32.sp
                else -> 26.sp
            }
            Text(
                text = shown,
                style = CamelType.LedgerDisplay.copy(fontSize = size, lineHeight = size * 1.1f),
                color = if (amount.isEmpty()) c.faint else c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Amount ${if (amount.isEmpty()) "zero" else shown} $currency" },
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DirectionArrowButton(Direction.IN, direction == Direction.IN, {
                    direction = Direction.IN; kind = kindAfterDirection(kind, Direction.IN)
                }, Modifier.weight(1f))
                DirectionArrowButton(Direction.OUT, direction == Direction.OUT, {
                    direction = Direction.OUT; kind = kindAfterDirection(kind, Direction.OUT)
                }, Modifier.weight(1f))
            }

            if (details) {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Kicker("Kind")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Kind.values().forEach { k ->
                            OptionChip(MoneyWords.kind(k), kind == k, {
                                kind = k
                                when (k) {
                                    Kind.INCOME -> direction = Direction.IN
                                    Kind.EXPENSE, Kind.FEE -> direction = Direction.OUT
                                    else -> {}
                                }
                            })
                        }
                    }
                    MoneyWords.directionHint(kind)?.let { SideNote(it) }
                    Kicker("Status")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentStatus.values().forEach { s -> OptionChip(statusLabel(s), status == s, { status = s }) }
                    }
                    LabelledField("Name", name, { name = it }, "Who or what was it for? (optional)")
                    LabelledField("Account", account, { account = it }, "Unassigned")
                    LabelledField("Note", note, { note = it }, "Optional note", minLines = 2)
                    LabelledField(
                        "Date", date, { date = it }, "YYYY-MM-DD",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (PaperDates.dayNumber(date) != null) SideNote(PaperDates.dateLabel(date))
                }
                Spacer(Modifier.height(10.dp))
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                SideNote(it, color = c.red)
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!details) {
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "<"))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            KeypadKey(k, Modifier.weight(1f)) {
                                error = null
                                amount = pressKey(amount, k, currency)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!valid && amount.isNotEmpty()) {
                SideNote(amountProblem(amount, currency).orEmpty(), Modifier.weight(1f).padding(end = 12.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            CheckButton(if (editing) "Save changes" else "Save record", valid, ::save)
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------------------------------------------------------------------------
// Receipt sheet
// ---------------------------------------------------------------------------

@Composable
internal fun EntryDetailSheet(
    entry: Entry,
    actions: CashflowActions,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalCashflowColors.current
    var confirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showOrigin by remember { mutableStateOf(false) }
    val isIn = entry.direction == Direction.IN

    SheetOverlay(onDismiss = onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Kicker(if (isIn) "Money in" else "Money out")
                Text(
                    entry.counterparty.ifBlank { "Unnamed record" },
                    style = CamelType.VoiceTitle, color = c.ink,
                )
            }
            TopIcon(Icons.Outlined.Edit, "Edit this record", onEdit)
            TopIcon(Icons.Outlined.Delete, "Delete this record", { confirm = true }, tint = c.red)
            TopIcon(Icons.Outlined.Close, "Close", onBack)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AmountText(entry.amountMinor, entry.currency, direction = entry.direction, style = CamelType.LedgerStage)
            Spacer(Modifier.width(8.dp))
            Icon(
                if (isIn) CFIcons.ArrowIn else CFIcons.ArrowOut,
                contentDescription = null,
                tint = if (isIn) c.citron else c.muted,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (confirm) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.redWash).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Delete this record?", style = CamelType.VoiceBody, color = c.ink)
                SideNote("It leaves your totals. Only the Cashflow record is removed.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DangerGhostButton("Delete", {
                        val result = actions.delete(entry.id)
                        if (result == null) onBack() else { error = result; confirm = false }
                    }, Modifier.weight(1f))
                    GhostButton("Keep", { confirm = false }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        error?.let { SideNote(it, color = c.red) }
        KvRow("Kind", MoneyWords.kind(entry.kind))
        KvRow("Status", MoneyWords.status(entry.status))
        KvRow("Account", entry.account)
        KvRow("Date", PaperDates.dateLabel(entry.date))
        if (entry.note.isNotBlank()) KvRow("Note", entry.note)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { showOrigin = !showOrigin }
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .semantics { contentDescription = if (showOrigin) "Hide where this came from" else "Show where this came from" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(CFIcons.Receipt, contentDescription = null, tint = c.muted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("Origin", style = CamelType.LedgerLabel, color = c.muted)
        }
        if (showOrigin) {
            Text(
                MoneyWords.origin(entry.source) + if (entry.userEdited) ". Edited by you." else ".",
                style = CamelType.Reference, color = c.muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bg).padding(11.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Review: one focused question at a time
// ---------------------------------------------------------------------------

@Composable
internal fun ReviewScreen(
    state: CashflowState,
    actions: CashflowActions,
    onEdit: (ReviewItem) -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalCashflowColors.current
    val items = state.review
    var index by remember { mutableStateOf(0) }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StateBlock(
                headline = "Nothing to review",
                sentence = "Alerts that need a human look will wait here.",
                actions = { GhostButton("Close", onClose) },
            )
        }
        return
    }
    val i = index.coerceIn(0, items.lastIndex)
    val item = items[i]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TopIcon(CFIcons.ChevronLeft, "Previous question", { if (i > 0) index = i - 1 })
            Text(
                "${i + 1} / ${items.size}",
                style = CamelType.LedgerAmount, color = c.ink,
                modifier = Modifier.padding(horizontal = 6.dp).semantics { contentDescription = "Question ${i + 1} of ${items.size}" },
            )
            TopIcon(CFIcons.ChevronRight, "Next question", { if (i < items.lastIndex) index = i + 1 })
        }
        key(item.id) {
            ReviewQuestion(
                item = item,
                actions = actions,
                lastOne = items.size <= 1,
                onEdit = onEdit,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun ReviewQuestion(
    item: ReviewItem,
    actions: CashflowActions,
    lastOne: Boolean,
    onEdit: (ReviewItem) -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalCashflowColors.current
    val d = item.draft
    var amountText by remember { mutableStateOf(d.amount.trim()) }
    var currency by remember { mutableStateOf(if (MoneyWords.isSupported(d.currency)) d.currency.uppercase() else "") }
    var direction by remember { mutableStateOf(if (d.directionConfirmed) d.direction else null) }
    var editingAmount by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val problem = amountProblem(amountText, currency)
    val minor = if (problem == null) Money.parse(amountText.trim().trimEnd('.'), currency) else null
    val amountMissing = amountText.isBlank()
    val reason = problem ?: if (direction == null) "Choose In or Out." else null

    fun current() = d.copy(
        amount = amountText.trim().trimEnd('.'),
        currency = currency,
        direction = direction ?: d.direction,
        directionConfirmed = direction != null,
    )

    fun finish() { if (lastOne) onClose() }

    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Kicker(item.source)
            Text(
                item.reason.ifBlank { "Check this alert" },
                style = CamelType.VoiceTitle, color = c.ink,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    Spacer(Modifier.height(16.dp))

    // Amount + currency
    if (minor != null && !editingAmount) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AmountText(minor, currency, direction = direction, style = CamelType.LedgerStage)
            }
            TopIcon(Icons.Outlined.Edit, "Edit amount", { editingAmount = true })
        }
    } else {
        if (amountMissing) {
            Text("Amount missing", style = CamelType.VoiceBody, color = c.lavender)
            Spacer(Modifier.height(6.dp))
        }
        PaperField(
            value = amountText,
            onValueChange = { amountText = it; error = null },
            placeholder = "Amount, e.g. 1250.50",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = CamelType.LedgerInput,
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CurrencyGlyphPicker(
            selected = currency,
            onSelect = { currency = it ?: ""; error = null },
            includeUnknown = true,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (currency.isEmpty()) "Currency unknown" else MoneyWords.currencyLong(currency),
            style = CamelType.LedgerLabel, color = c.muted,
        )
    }

    // Direction
    Spacer(Modifier.height(18.dp))
    Kicker("Did money come in or go out?")
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DirectionArrowButton(Direction.IN, direction == Direction.IN, { direction = Direction.IN; error = null }, Modifier.weight(1f))
        DirectionArrowButton(Direction.OUT, direction == Direction.OUT, { direction = Direction.OUT; error = null }, Modifier.weight(1f))
    }

    // Controls
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        TopIcon(CFIcons.Receipt, if (showAlert) "Hide the original alert" else "Show the original alert", { showAlert = !showAlert }, on = showAlert)
        TopIcon(CFIcons.MinusCircle, "Ignore this alert", {
            val r = actions.ignoreReview(item.id)
            if (r == null) finish() else error = r
        })
        TopIcon(Icons.Outlined.Edit, "Edit all details", { onEdit(item.copy(draft = current())) })
        Spacer(Modifier.weight(1f))
        CheckButton("Record this alert", reason == null, {
            val r = actions.save(current().copy(directionConfirmed = true), null, item.id)
            if (r == null) finish() else error = r
        })
    }
    Spacer(Modifier.height(8.dp))
    val shownReason = error ?: reason
    if (shownReason != null) SideNote(shownReason, color = if (error != null) c.red else c.muted)
    if (showAlert) {
        Spacer(Modifier.height(8.dp))
        Text(
            item.text,
            style = CamelType.Reference, color = c.muted,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(12.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GhostButton("Not sure? Leave it", onClose)
    }
    Spacer(Modifier.height(16.dp))
}
