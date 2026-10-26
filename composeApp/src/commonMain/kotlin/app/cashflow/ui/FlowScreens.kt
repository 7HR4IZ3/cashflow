package app.cashflow.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cashflow.core.Entry
import app.cashflow.core.EntryDraft
import app.cashflow.core.Kind
import app.cashflow.core.Money
import app.cashflow.core.PaymentStatus
import app.cashflow.core.ReviewItem
import app.cashflow.core.CashflowActions
import app.cashflow.core.CashflowState
import app.cashflow.core.Direction
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

/** Shared header. */
@Composable
private fun FlowHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
	val c = LocalCashflowColors.current
	Row(
		Modifier.fillMaxWidth().background(c.paper).padding(horizontal = 8.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		if (onBack != null) {
			IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Back", tint = c.ink2) }
		}
		Text(
			title,
			style = CamelType.VoiceTitle,
			color = c.ink,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 4.dp),
		)
		Spacer(Modifier.weight(1f))
		trailing?.invoke()
	}
}

/** Small skeleton card. */
@Composable
private fun SheetCard(content: @Composable () -> Unit) {
	val c = LocalCashflowColors.current
	Column(
		Modifier.fillMaxWidth().background(c.raise, MaterialTheme.shapes.medium).padding(16.dp),
	) { content() }
}

@Composable
private fun Label(text: String) = Text(text, style = CamelType.LedgerLabel, color = LocalCashflowColors.current.ink3)

/** Word for a Kind; own transfers are named for what they are, not a generic payment. */
private fun kindLabel(kind: Kind) = when (kind) {
	Kind.TRANSFER -> "Own transfer"
	else -> kind.name.lowercase().replaceFirstChar { r -> r.uppercase() }
}

@Composable
private fun Body(text: String) = Text(text, style = CamelType.VoiceBody, color = LocalCashflowColors.current.ink2)

/** Horizontally scrollable chip selector. */
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
	Row(
		Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		options.forEach { option ->
			FilterChip(
				selected = option == selected,
				onClick = { onPick(option) },
				label = { Text(label(option), style = CamelType.LedgerLabel) },
			)
		}
	}
}

// ---------------------------------------------------------------- Onboarding

@Composable
fun OnboardingScreen(state: CashflowState, actions: CashflowActions) {
	val c = LocalCashflowColors.current
	val step = state.onboardingStep.coerceIn(0, 3)

	Column(Modifier.fillMaxSize().background(c.paper).safeDrawingPadding()) {
		FlowHeader(
			"Welcome",
			onBack = if (step > 0) {
				{ actions.setOnboardingStep(step - 1) }
			} else {
				null
			},
		)
		AnimatedContent(
			targetState = step,
			modifier = Modifier.weight(1f),
			transitionSpec = { fadeIn() togetherWith fadeOut() },
			label = "onboarding",
		) { s ->
			Column(
				Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				when (s) {
					0 -> SandboxIntro(state.currency)
					1 -> SheetCard {
						Text("Your currency", style = CamelType.VoiceTitle, color = c.ink)
						Body("Choose your main currency. Each currency stays separate.")
						Spacer(Modifier.height(8.dp))
						ChipRow(Money.currencies, state.currency, { it }) { actions.setCurrency(it) }					}

					2 -> SheetCard {
						Text("Local processing", style = CamelType.VoiceTitle, color = c.ink)
						Body("Everything is processed on this device in this build. No model uploads, no accounts, no cloud sync — export is manual CSV, out only.")
					}

					3 -> CaptureCard(state, actions)
				}
			}
		}
		Row(
			Modifier.fillMaxWidth().padding(20.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			TextButton({ actions.finishOnboarding() }) { Text("Skip") }
			Spacer(Modifier.weight(1f))
			Button({
				if (step >= 3) actions.finishOnboarding() else actions.setOnboardingStep(step + 1)
			}) { Text(if (step >= 3) "Start" else "Next") }
		}
	}
}

/** Intro card with a worked sandbox example. Demo state only — never saved. */
@Composable
private fun SandboxIntro(currency: String) {
	val c = LocalCashflowColors.current
	val sample = "1,250.00"
	val variants = listOf("1,250.00", "89.90", "12.50")
	var demo by remember { mutableStateOf(sample) }
	var pick by remember { mutableStateOf(0) }

	Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
		SheetCard {
			Text("Cashflow", style = CamelType.VoiceHeadline, color = c.ink)
			Body("Payment alerts become a timeline. Undo or edit anytime.")
			Spacer(Modifier.height(8.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
				Icon(Icons.Filled.Notifications, null, Modifier.height(16.dp), tint = c.ink3)
				Icon(Icons.Filled.ArrowForward, null, Modifier.height(14.dp), tint = c.ink3)
				Icon(Icons.Filled.Check, null, Modifier.height(16.dp), tint = c.ink3)
				Body("Alert → recorded")
			}
		}
		SheetCard {
			Label("Example · never saved")
			Text("$currency $demo", style = CamelType.LedgerHero, color = c.ink)
			Spacer(Modifier.height(8.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				OutlinedButton(onClick = {
					pick = (pick + 1) % variants.size
					demo = variants[pick]
				}) {
					Icon(Icons.Filled.Edit, null, Modifier.height(16.dp))
					Text("  Edit")
				}
				TextButton(onClick = {
					pick = 0
					demo = sample
				}) { Text("Undo") }
			}
		}
	}
}

/** Capture-access guidance: Android broad listener vs posting, iOS Shortcuts user-managed. */
@Composable
private fun CaptureCard(state: CashflowState, actions: CashflowActions) {
	val c = LocalCashflowColors.current
	SheetCard {
		Text("Capture access", style = CamelType.VoiceTitle, color = c.ink)
		if (state.capture.platform == "Android") {
			Body("Cashflow listens broadly: it receives every app's notifications, then keeps only the sources you allow. Grant notification access in system settings, then pick watched apps in Settings.")
			Spacer(Modifier.height(8.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				OutlinedButton(onClick = { actions.openCaptureSettings() }) { Text("Capture access") }
				OutlinedButton(onClick = { actions.requestNotificationPermission() }) { Text("Post notifications") }
			}
			Spacer(Modifier.height(4.dp))
			Label("Posting Cashflow's own notifications is a separate permission, needed only for capture alerts and Edit shortcuts.")
		} else {
			Body("iOS: set up once in Shortcuts. Cashflow does not grant or revoke this itself — the setup stays yours.")
			val steps = listOf(
				"Open Shortcuts.",
				"New automation on notifications.",
				"Choose your banking apps as triggers.",
				"Add the Cashflow action \"Capture transaction notification\" and map the notification's text into it.",
				"Enable run-when-triggered if your iOS version offers it, save, then check Settings for the last alert received after a test transfer.",
			)
			steps.forEachIndexed { i, s ->
				Text("${i + 1}.  $s", style = CamelType.VoiceBody, color = c.ink2)
			}
			Spacer(Modifier.height(8.dp))
			OutlinedButton(onClick = { actions.openCaptureSettings() }) { Text("Open Shortcuts") }
			Spacer(Modifier.height(4.dp))
			Label("Manual entry always works while capture is being set up.")
		}
	}
}

// --------------------------------------------------------------- Entry editor

@Composable
fun EntryEditorScreen(
	draft: EntryDraft,
	entryId: String?,
	reviewId: String?,
	actions: CashflowActions,
	onBack: () -> Unit,
) {
	val c = LocalCashflowColors.current
	// Keyed by the draft identity so editing a different entry never shows stale fields.
	var amount by remember(entryId, reviewId, draft) { mutableStateOf(draft.amount) }
	var counterparty by remember(entryId, reviewId, draft) { mutableStateOf(draft.counterparty) }
	var account by remember(entryId, reviewId, draft) { mutableStateOf(draft.account) }
	var note by remember(entryId, reviewId, draft) { mutableStateOf(draft.note) }
	var date by remember(entryId, reviewId, draft) { mutableStateOf(draft.date) }
	var currency by remember(entryId, reviewId, draft) { mutableStateOf(draft.currency) }
	var direction by remember(entryId, reviewId, draft) { mutableStateOf(draft.direction) }
	var kind by remember(entryId, reviewId, draft) { mutableStateOf(draft.kind) }
	var status by remember(entryId, reviewId, draft) { mutableStateOf(draft.status) }
	var error by remember(entryId, reviewId, draft) { mutableStateOf<String?>(null) }

	val field: @Composable (String, String, (String) -> Unit) -> Unit =
		{ label, value, set ->
			OutlinedTextField(
				value = value,
				onValueChange = set,
				label = { Text(label) },
				singleLine = true,
				textStyle = CamelType.LedgerBody,
				modifier = Modifier.fillMaxWidth(),
			)
		}

	Column(Modifier.fillMaxSize().background(c.paper).safeDrawingPadding().imePadding()) {
		FlowHeader(if (entryId == null) "New entry" else "Edit entry", onBack)
		Column(
			Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			Label("Amount · $currency")
			OutlinedTextField(
				value = amount,
				onValueChange = { amount = it },
				placeholder = { Text("0.00", style = CamelType.LedgerInput) },
				singleLine = true,
				textStyle = CamelType.LedgerInput,
				modifier = Modifier.fillMaxWidth(),
			)
			ChipRow(Money.currencies, currency, { it }) { currency = it }
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				FilterChip(direction == Direction.IN, onClick = { direction = Direction.IN }, label = { Text("In") })
				FilterChip(direction == Direction.OUT, onClick = { direction = Direction.OUT }, label = { Text("Out") })
			}
			Label("Kind")
			ChipRow(Kind.entries.toList(), kind, { kindLabel(it) }, { kind = it })
			if (kind == Kind.TRANSFER) {
				Text(
					"Own transfers move between your own accounts, so they are excluded from the in and out totals.",
					style = CamelType.LedgerLabel,
					color = c.ink3,
				)
			}
			ChipsStatus(subject = status, onPick = { status = it })

			field("Who", counterparty, { counterparty = it })
			field("Account", account, { account = it })
			field("Date", date, { date = it })
			Text(
				if (date.isBlank()) "Blank date = today" else "",
				style = CamelType.LedgerLabel,
				color = c.ink3,
			)
			OutlinedTextField(
				value = note,
				onValueChange = { note = it },
				label = { Text("Note") },
				modifier = Modifier.fillMaxWidth(),
			)

			error?.let {
				Text(it, style = CamelType.VoiceBody, color = c.fail)
			}
			Button(
				onClick = {
					val err = actions.save(
						EntryDraft(
							amount = amount.trim(),
							currency = currency,
							direction = direction,
							kind = kind,
							status = status,
							counterparty = counterparty.trim(),
							account = account.ifBlank { "Unassigned" },
							note = note.trim(),
							date = date.trim(),
						),
						entryId,
						reviewId,
					)
					if (err == null) {
						actions.clearError()
						onBack()
					} else {
						error = err
					}
				},
				modifier = Modifier.fillMaxWidth().height(48.dp),
			) { Icon(Icons.Filled.Check, null, Modifier.height(18.dp)); Text("  Save") }
		}
	}
}

@Composable
private fun ChipsStatus(subject: PaymentStatus, onPick: (PaymentStatus) -> Unit) {
	val c = LocalCashflowColors.current
	Column {
		Label("Status")
		ChipRow(PaymentStatus.entries.toList(), subject, { it.name.lowercase().replaceFirstChar { r -> r.uppercase() } }, onPick)
		Text(
			when (subject) {
				PaymentStatus.PENDING -> "Counted when it completes"
				PaymentStatus.FAILED -> "Kept, but not counted"
				PaymentStatus.COMPLETED -> "Counted"
			},
			style = CamelType.LedgerLabel,
			color = c.ink3,
		)
	}
}

// --------------------------------------------------------------- Entry detail

@Composable
fun EntryDetailScreen(entry: Entry, actions: CashflowActions, onEdit: () -> Unit, onBack: () -> Unit) {
	val c = LocalCashflowColors.current
	var confirmDelete by remember { mutableStateOf(false) }
	var error by remember { mutableStateOf<String?>(null) }

	Column(Modifier.fillMaxSize().background(c.paper).safeDrawingPadding()) {
		FlowHeader("Entry", onBack, trailing = {
			IconButton(onClick = onEdit, enabled = !entry.deleted) {
				Icon(Icons.Filled.Edit, "Edit", tint = c.ink2)
			}
		})
		Column(
			Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			SheetCard {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						Money.display(entry.amountMinor, entry.currency),
						style = CamelType.LedgerHero,
						color = c.ink,
					)
					Spacer(Modifier.weight(1f))
					Text(entry.direction.name, style = CamelType.LedgerAmount, color = c.ink2)
				}
				Text(entry.counterparty.ifBlank { "Unknown" }, style = CamelType.VoiceBody, color = c.ink2)
			}
			DetailRows(
				listOf(
					"Kind" to kindLabel(entry.kind),
					"Status" to entry.status.name.lowercase().replaceFirstChar { r -> r.uppercase() },
					"Account" to entry.account,
					"Date" to entry.date,
					"Source" to entry.source,
				),
			)
			if (entry.note.isNotBlank()) {
				Column { Label("Note"); Body(entry.note) }
			}
			if (entry.note.isBlank()) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(Icons.Filled.Info, null, Modifier.height(16.dp), tint = c.ink3)
					Text("  No note", style = CamelType.LedgerLabel, color = c.ink3)
				}
			}
			Spacer(Modifier.weight(1f))
			error?.let {
				Text(it, style = CamelType.VoiceBody, color = c.fail)
			}
			if (entry.deleted) {
				OutlinedButton({
					val err = actions.restore(entry.id)
					if (err == null) onBack() else error = err
				}, Modifier.fillMaxWidth().height(48.dp)) {
					Text("Undo delete")
				}
			} else {
				OutlinedButton(
					{ confirmDelete = true },
					Modifier.fillMaxWidth().height(48.dp),
				) { Icon(Icons.Filled.Delete, null, Modifier.height(18.dp)); Text("  Delete") }
			}
		}
	}
	if (confirmDelete) {
		AlertDialog(
			onDismissRequest = { confirmDelete = false },
			title = { Text("Delete entry?", style = CamelType.VoiceTitle) },
			text = {
				Body("Removes this entry from Cashflow's ledger only — it never touches your bank or any real transfer.")
			},
			confirmButton = {
				TextButton({
					confirmDelete = false
					val err = actions.delete(entry.id)
					if (err == null) onBack() else error = err
				}) { Text("Delete", color = c.fail) }
			},
			dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep") } },
		)
	}
}

@Composable
private fun DetailRows(rows: List<Pair<String, String>>) {	val c = LocalCashflowColors.current
	Column(
		Modifier.fillMaxWidth().background(c.raise, MaterialTheme.shapes.medium),
	) {
		rows.forEachIndexed { index, (name, value) ->
			Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
				Text(name, style = CamelType.LedgerLabel, color = c.ink3)
				Spacer(Modifier.weight(1f))
				Text(value, style = CamelType.LedgerBody, color = c.ink)
			}
			if (index < rows.lastIndex) {
				Spacer(Modifier.height(1.dp).fillMaxWidth().background(c.lineSoft))
			}
		}
	}
}

// ------------------------------------------------------------------- Review

@Composable
fun ReviewScreen(state: CashflowState, actions: CashflowActions, onEdit: (ReviewItem) -> Unit) {
	val c = LocalCashflowColors.current
	Column(Modifier.fillMaxSize().background(c.paper)) {
		FlowHeader("Review (${state.review.size})")
		Column(
			Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			state.error?.let {
				Text(it, style = CamelType.VoiceBody, color = c.fail)
			}
			if (state.review.isEmpty()) {
				SheetCard {
					Text("All clear", style = CamelType.VoiceTitle)
					Body("Alerts Cashflow could not read confidently wait here. Clear debit and credit alerts neither wait nor land here — they go straight into Activity.")
				}
			}
			state.review.forEach { item ->
				SheetCard {
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						Row(verticalAlignment = Alignment.CenterVertically) {
							Label(item.source)
							Spacer(Modifier.weight(1f))
							// draft.amount holds decimal major units (e.g. "1250.00"); show only parsed money.
							val minor = Money.parse(item.draft.amount, item.draft.currency)
							Text(
								minor?.let { Money.display(it, item.draft.currency) } ?: "Amount needs a check",
								style = CamelType.LedgerAmount,
								color = c.ink,
							)
						}
						Text(
							item.text,
							style = CamelType.Reference,
							color = c.ink2,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.background(c.inset, MaterialTheme.shapes.small).padding(6.dp),
						)
						Body(item.reason)
						Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							OutlinedButton({ onEdit(item) }) {
								Icon(Icons.Filled.Edit, null, Modifier.height(16.dp)); Text("  Edit")
							}
							TextButton({ actions.ignoreReview(item.id) }) {
								Icon(Icons.Filled.Close, null, Modifier.height(16.dp)); Text("  Ignore")
							}
						}
					}
				}
			}
		}
	}
}
