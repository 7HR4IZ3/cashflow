package app.cashflow.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Check
import app.cashflow.ui.components.CFIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.cashflow.core.Account
import app.cashflow.core.AccountRole
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Entry
import app.cashflow.core.Goal
import app.cashflow.core.GoalPeriod
import app.cashflow.core.GoalProgress
import app.cashflow.core.GoalState
import app.cashflow.core.GoalType
import app.cashflow.core.Kind
import app.cashflow.core.Money
import app.cashflow.core.NudgeCategory
import app.cashflow.ui.components.ActionButton
import app.cashflow.ui.components.Divider
import app.cashflow.ui.components.GhostButton
import app.cashflow.ui.components.Kicker
import app.cashflow.ui.components.KvRow
import app.cashflow.ui.components.LabelledField
import app.cashflow.ui.components.OptionChip
import app.cashflow.ui.components.SettingRow
import app.cashflow.ui.components.SideNote
import app.cashflow.ui.components.ToggleRow
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

private fun AccountRole.label() = when (this) {
    AccountRole.SPENDING -> "Spending"
    AccountRole.SAVINGS -> "Savings"
    AccountRole.OTHER -> "Other"
}

// ---------------------------------------------------------------------------
// Home: a yes/no question, goals and the one thing worth noticing
// ---------------------------------------------------------------------------

/** Asks about the first open "is this your own transfer?" pair. Answers are remembered. */
@Composable
internal fun TransferQuestion(state: CashflowState, actions: CashflowActions) {
    val c = LocalCashflowColors.current
    val suggestion = state.transferSuggestions.firstOrNull() ?: return
    val first = state.entries.firstOrNull { it.id == suggestion.firstId } ?: return
    val second = state.entries.firstOrNull { it.id == suggestion.secondId } ?: return
    val (out, inn) = if (first.direction == app.cashflow.core.Direction.OUT) first to second else second to first
    fun name(e: Entry) = e.account.takeIf { it.isNotBlank() && !it.equals("Unassigned", true) } ?: e.source
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(c.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Kicker("Is this you?")
        Text(
            "${Money.display(out.amountMinor, out.currency)} left ${name(out)} and arrived in ${name(inn)}. " +
                "Is that a move between your own accounts?",
            style = CamelType.VoiceBody, color = c.ink,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Yes, mine", { actions.answerTransfer(suggestion.id, true) }, Modifier.weight(1f))
            GhostButton("No", { actions.answerTransfer(suggestion.id, false) }, Modifier.weight(1f))
        }
        SideNote("Yes keeps it out of your spending and income. Cashflow remembers your answer.")
    }
}

/** Active goals as slim progress bars, the month's savings, and the most relevant insight. */
@Composable
internal fun GoalsStrip(state: CashflowState, onOpenGoals: () -> Unit) {
    val c = LocalCashflowColors.current
    val month = state.insights.month
    val saved = month?.saved ?: 0L
    val headline = state.insights.headline
    if (state.goals.isEmpty() && saved == 0L && headline == null) return
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(c.surface)
            .clickable(onClick = onOpenGoals).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (saved > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker("Saved this month")
                Spacer(Modifier.width(10.dp))
                Text(Money.display(saved, state.currency), style = CamelType.LedgerAmount, color = c.citron)
            }
        }
        state.goals.take(3).forEach { GoalBar(it) }
        headline?.let { Text(it.text, style = CamelType.LedgerLabel, color = c.muted) }
    }
}

@Composable
private fun GoalBar(progress: GoalProgress) {
    val c = LocalCashflowColors.current
    val goal = progress.goal
    val tint = when (progress.state) {
        GoalState.OVER -> c.red
        GoalState.CLOSE -> c.lavender
        else -> c.citron
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(goal.name, style = CamelType.VoiceBody, color = c.ink, modifier = Modifier.weight(1f))
            Text(goalSentence(progress), style = CamelType.LedgerLabel, color = c.muted)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.bg)) {
            Box(
                Modifier.fillMaxWidth(progress.fraction.coerceIn(0.0, 1.0).toFloat())
                    .height(6.dp).clip(RoundedCornerShape(3.dp)).background(tint),
            )
        }
    }
}

private fun goalSentence(p: GoalProgress): String {
    val currency = p.goal.currency
    return when {
        p.goal.type == GoalType.SPENDING_LIMIT && p.state == GoalState.OVER ->
            "Over by ${Money.display(p.amountMinor - p.goal.amountMinor, currency)}"
        p.goal.type == GoalType.SPENDING_LIMIT -> "${Money.display(p.remainingMinor, currency)} left"
        p.state == GoalState.REACHED -> "Reached"
        else -> "${Money.display(p.amountMinor, currency)} of ${Money.display(p.goal.amountMinor, currency)}"
    }
}

// ---------------------------------------------------------------------------
// Editor and receipt
// ---------------------------------------------------------------------------

/** Quick picks for the user's own accounts, shown above the free-text account field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountChips(state: CashflowState, current: String, onPick: (String) -> Unit) {
    val accounts = state.accounts.filterNot { it.archived }
    if (accounts.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.forEach { OptionChip(it.name, it.name.equals(current.trim(), true), { onPick(it.name) }) }
    }
}

/** Receipt section: what kind of transfer this is, and the two ways to change that. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransferSection(entry: Entry, state: CashflowState, actions: CashflowActions, onError: (String) -> Unit) {
    val c = LocalCashflowColors.current
    var choosing by remember { mutableStateOf(false) }
    if (entry.kind == Kind.TRANSFER) {
        val other = state.entries.firstOrNull { it.transferGroupId != null && it.transferGroupId == entry.transferGroupId && it.id != entry.id }
        val to = other?.account?.takeIf { it.isNotBlank() } ?: entry.counterAccount
        KvRow("Own transfer", when {
            other != null -> "Linked to ${other.account.ifBlank { "the other side" }}"
            to.isNotBlank() -> "With $to. The other side is not tracked."
            else -> "The other side is not tracked."
        })
        Spacer(Modifier.height(6.dp))
        GhostButton("Not a transfer", { actions.unlinkTransfer(entry.id)?.let(onError) }, Modifier.fillMaxWidth())
    } else if (entry.status == app.cashflow.core.PaymentStatus.COMPLETED) {
        if (!choosing) {
            GhostButton("Mark as my own transfer", { choosing = true }, Modifier.fillMaxWidth())
        } else {
            Kicker("With which of your accounts?")
            Spacer(Modifier.height(6.dp))
            val options = state.accounts.filterNot { it.archived || it.name.equals(entry.account, true) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { a ->
                    OptionChip(a.name + if (a.role == AccountRole.SAVINGS) " (savings)" else "", false, {
                        actions.markTransfer(entry.id, a.name)?.let(onError); choosing = false
                    })
                }
                OptionChip("Another bank", false, { actions.markTransfer(entry.id, "")?.let(onError); choosing = false })
            }
            if (options.isEmpty()) SideNote("Add your accounts in Settings to say which one.", color = c.muted)
        }
    }
}

// ---------------------------------------------------------------------------
// Settings pages
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountsPage(state: CashflowState, actions: CashflowActions, onToast: (String) -> Unit) {
    val c = LocalCashflowColors.current
    var editing by remember { mutableStateOf<Account?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var names by remember(state.myNames) { mutableStateOf(state.myNames.joinToString(", ")) }
    val form = editing
    if (form != null) {
        var name by remember(form.id) { mutableStateOf(form.name) }
        var role by remember(form.id) { mutableStateOf(form.role) }
        var hints by remember(form.id) { mutableStateOf(form.hints.joinToString(", ")) }
        Kicker("Type")
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountRole.entries.forEach { OptionChip(it.label(), role == it, { role = it }) }
        }
        Spacer(Modifier.height(8.dp))
        LabelledField("Name", name, { name = it }, "GTBank, Opay, Piggy")
        Spacer(Modifier.height(8.dp))
        LabelledField("Matches alerts containing", hints, { hints = it }, "gtb, 4821 (optional, comma separated)")
        SideNote("Savings accounts count money you move in as saved, not spent.")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Save", {
                val result = actions.saveAccount(form.copy(name = name, role = role, hints = hints.split(',')))
                if (result == null) editing = null else onToast(result)
            }, Modifier.weight(1f))
            GhostButton("Cancel", { editing = null }, Modifier.weight(1f))
        }
        if (form.id.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            GhostButton(if (form.archived) "Bring back" else "Hide this account", {
                actions.setAccountArchived(form.id, !form.archived)?.let(onToast); editing = null
            }, Modifier.fillMaxWidth())
            SideNote("Hidden accounts keep their history.")
        }
        return
    }
    val shown = state.accounts.filter { showArchived || !it.archived }
    if (shown.isEmpty()) Text("Add the banks and wallets you use, and mark any that hold savings.", style = CamelType.LedgerLabel, color = c.muted)
    shown.forEach { a ->
        SettingRow(
            icon = null, title = a.name, onClick = { editing = a },
            value = a.role.label() + if (a.archived) " · hidden" else "",
        )
    }
    Spacer(Modifier.height(8.dp))
    ActionButton("Add an account", { editing = Account("", "") }, Modifier.fillMaxWidth())
    if (state.accounts.any { it.archived }) {
        ToggleRow("Show hidden accounts", null, showArchived, { showArchived = it })
    }
    Spacer(Modifier.height(14.dp))
    Divider()
    Spacer(Modifier.height(10.dp))
    Kicker("Your names")
    Spacer(Modifier.height(6.dp))
    LabelledField("Names on your accounts", names, { names = it }, "Ada Okafor (comma separated)")
    SideNote("A payment to or from one of these is treated as your own transfer.")
    Spacer(Modifier.height(8.dp))
    GhostButton("Save names", { actions.setMyNames(names.split(',')); onToast("Saved") }, Modifier.fillMaxWidth())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalsPage(state: CashflowState, actions: CashflowActions, onToast: (String) -> Unit) {
    val c = LocalCashflowColors.current
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(GoalType.SPENDING_LIMIT) }
        var amount by remember { mutableStateOf("") }
        var period by remember { mutableStateOf(GoalPeriod.MONTHLY) }
        var start by remember { mutableStateOf("") }
        var end by remember { mutableStateOf("") }
        var scope by remember { mutableStateOf("") }
        var picked by remember { mutableStateOf(setOf<String>()) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("Spending limit", type == GoalType.SPENDING_LIMIT, { type = GoalType.SPENDING_LIMIT })
            OptionChip("Saving target", type == GoalType.SAVING_TARGET, { type = GoalType.SAVING_TARGET })
        }
        Spacer(Modifier.height(8.dp))
        LabelledField("Name", name, { name = it }, if (type == GoalType.SPENDING_LIMIT) "Monthly spending" else "Rainy day fund")
        Spacer(Modifier.height(8.dp))
        LabelledField("Amount (${state.currency})", amount, { amount = it }, "0", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip("Each week", period == GoalPeriod.WEEKLY, { period = GoalPeriod.WEEKLY })
            OptionChip("Each month", period == GoalPeriod.MONTHLY, { period = GoalPeriod.MONTHLY })
            OptionChip("Dates", period == GoalPeriod.CUSTOM, { period = GoalPeriod.CUSTOM })
        }
        if (period == GoalPeriod.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            LabelledField("From", start, { start = it }, "YYYY-MM-DD")
            Spacer(Modifier.height(8.dp))
            LabelledField("To", end, { end = it }, "YYYY-MM-DD")
        }
        if (type == GoalType.SPENDING_LIMIT) {
            Spacer(Modifier.height(8.dp))
            LabelledField("Only payments to (optional)", scope, { scope = it }, "Leave blank for all spending")
        } else {
            val savings = state.accounts.filter { it.role == AccountRole.SAVINGS && !it.archived }
            Spacer(Modifier.height(8.dp))
            if (savings.isEmpty()) SideNote("Mark an account as Savings first, so Cashflow knows what counts.")
            else {
                Kicker("Savings accounts that count")
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    savings.forEach { a -> OptionChip(a.name, a.name in picked, { picked = if (a.name in picked) picked - a.name else picked + a.name }) }
                }
                SideNote("None selected means all of them.")
            }
        }
        Spacer(Modifier.height(10.dp))
        val minor = Money.parse(amount.trim(), state.currency)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Save goal", {
                val result = actions.saveGoal(Goal("", name, type, minor ?: 0, state.currency, period, start.trim(), end.trim(), scope, picked.toList()))
                if (result == null) creating = false else onToast(result)
            }, Modifier.weight(1f), enabled = name.isNotBlank() && minor != null)
            GhostButton("Cancel", { creating = false }, Modifier.weight(1f))
        }
        return
    }
    if (state.goals.isEmpty()) Text("Set a spending limit or a saving target and watch it fill as alerts arrive.", style = CamelType.LedgerLabel, color = c.muted)
    state.goals.forEach { p ->
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GoalBar(p)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        p.state == GoalState.REACHED -> "Target reached"
                        p.state == GoalState.ENDED -> "Ended ${p.to}"
                        p.goal.type == GoalType.SPENDING_LIMIT -> "${p.daysLeft} days left. About ${Money.display(p.perDayMinor, p.goal.currency)} a day."
                        else -> "${p.daysLeft} days left. About ${Money.display(p.perDayMinor, p.goal.currency)} a day to go."
                    },
                    style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.weight(1f),
                )
                Text("Archive", style = CamelType.LedgerLabel, color = c.muted,
                    modifier = Modifier.clickable { actions.archiveGoal(p.goal.id)?.let(onToast) }.padding(8.dp))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    ActionButton("New goal", { creating = true }, Modifier.fillMaxWidth())
    val other = state.currency
    SideNote("Goals use $other. Switch the view currency to set goals in another one.")
}

@Composable
internal fun NotificationsPage(state: CashflowState, actions: CashflowActions) {
    val c = LocalCashflowColors.current
    val icons = mapOf(
        NudgeCategory.CREDIT to CFIcons.ArrowIn,
        NudgeCategory.GOAL to Icons.Outlined.Place,
        NudgeCategory.HABIT to Icons.Outlined.Info,
        NudgeCategory.SUMMARY to Icons.Outlined.DateRange,
        NudgeCategory.REMINDER to Icons.Outlined.Notifications,
    )
    SettingsGroup {
        SettingsItem(
            Icons.Outlined.Check, "Recorded payments", "",
            subtitle = "Each captured payment, with Undo",
            trailing = { SettingsSwitch(state.confirmations) }, onClick = { actions.setConfirmations(!state.confirmations) },
        )
        NudgeCategory.entries.forEach { category ->
            SettingsDivider()
            val on = state.notify[category] == true
            SettingsItem(
                icons.getValue(category), category.label, "",
                subtitle = when (category) {
                    NudgeCategory.CREDIT -> "Adds the day's net to money-in alerts"
                    NudgeCategory.GOAL -> "When a goal is close, exceeded or reached"
                    NudgeCategory.HABIT -> "Occasional notes on spending patterns"
                    NudgeCategory.SUMMARY -> "A recap early each week and month"
                    NudgeCategory.REMINDER -> "When several alerts wait in Review"
                },
                trailing = { SettingsSwitch(on) }, onClick = { actions.setNotify(category, !on) },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "At most three a day, none overnight. All worked out on this device.",
        style = CamelType.LedgerLabel, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
    )
}
