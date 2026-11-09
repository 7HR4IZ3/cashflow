# Accounts, goals and insights spec

Draft 2026-11-01; first implementation 2026-11-01. Extends [product plan](product-plan.md) and [transaction engine](transaction-engine.md). See [Implementation status](#implementation-status) for what exists and where it differs from the text below.

## Summary

Cashflow today records what the alerts say. This spec adds three things so the app understands the user's money, not just their notifications:

1. **Accounts**: the user's own banks and wallets, with one or more marked as **Savings**. Movement between owned accounts is a transfer, not spending or income.
2. **Goals**: spending limits and saving targets with live progress.
3. **Insights and nudges**: confirmations, credit alerts, spending-habit feedback and summaries that keep the user informed and engaged without becoming noise.

Everything is computed on-device from the local ledger. Cloud AI stays optional and is never required for any feature below.

## Goals and non-goals

Goals:
- Money moved between the user's own accounts is never counted as spending or income.
- Users can set a spending or saving goal in under 30 seconds and see progress at a glance.
- Notifications feel useful and are fully controllable; a user can silence any category.
- Unsure cases ask the user once and remember the answer.

Non-goals (this spec):
- Bank linking, open banking or any server-side balance access. Capture stays notification-based.
- Budget envelopes, bill reminders, investment tracking, multi-user households.
- Moving money. The app never initiates or reverses payments.

## Terminology

The request says "debt/credit"; this spec uses **debit** (money out) and **credit** (money in), matching `Direction.OUT` and `Direction.IN`. "Debt" (money owed) is out of scope.

## 1. Accounts

### Model

New `Account` entity, replacing the free-text `Entry.account` string.

| Field | Notes |
|---|---|
| id | Local ID |
| name | User label, e.g. "GTBank", "Opay" |
| role | `SPENDING` (default), `SAVINGS`, `OTHER` |
| currency | One currency per account |
| sourceHints | Watched-app IDs and text hints (last 4 digits, bank name) used to match alerts to this account |
| archived | Hidden from pickers; history preserved |

`Entry.account` migrates to `accountId`. Existing text values become accounts automatically on first launch after upgrade; "Unassigned" maps to no account.

### Behaviour

- **Create**: from Settings → Accounts, from an entry's account picker, or from a prompt when an alert names an unknown account ("Looks like a new account ending 4821. Add it?").
- **Savings flag**: toggle on any account. Exactly zero or more savings accounts are allowed.
- **Alert-to-account matching**: by watched app first, then last-digits or name hints in the alert text. Unmatched alerts stay on "No account" and never block capture.
- **Balance**: not tracked as truth. Bank balance lines in alerts are informational and, when parsed, shown as "last seen balance" with its timestamp. Cashflow does not compute a running balance it cannot verify.

### Own-account transfers

An alert pair (or single alert plus a user's hint) that moves money between two of the user's accounts becomes a **transfer group** with two legs, as already defined in the transaction engine.

Detection signals, strongest first:
1. Counterparty text names one of the user's accounts or the user's own name (user sets "my names" once in Settings, optional).
2. A debit on account A and a credit of the same amount on account B within a matching window (default 10 minutes; configurable 1 to 60) with no conflicting reference.
3. Counterparty is a known saved destination the user previously confirmed as "mine".

Outcomes:
- High confidence from signal 1 or 3: record as transfer automatically, with Undo.
- Signal 2 only: record both legs as unclassified, then ask once: "₦50,000 left GTBank and arrived in Opay. Move between your own accounts?" Yes links them as a transfer; No keeps them as expense and income. The answer is remembered for that account pair and counterparty.
- Single leg only (other account not watched): if the counterparty is a confirmed "mine" destination, record as transfer with the other leg marked *not tracked*.

Savings semantics:
- Transfer *into* a savings account counts as **saved**. Transfer *out of* savings counts as **withdrawn from savings**. Neither affects income or spending totals.
- Credits *originating in* a savings account from outside (e.g. interest) are income, categorised as interest.
- Transfers between two spending accounts are neutral and excluded from all totals, consistent with the current `totals()` rule.

User controls: any transfer can be unlinked, relinked, or reclassified from the entry detail sheet. Edits are preserved by the existing `userEdited` protection.

## 2. Goals

### Types

| Type | Definition | Progress measure |
|---|---|---|
| Spending limit | Max total debit for a scope in a period | Spent ÷ limit |
| Saving target | Amount to save by a date, or per period | Saved ÷ target |

Scope for spending limits: all spending, or a single category, or a single counterparty. Categories arrive in Phase 2 (below); until then limits apply to all spending.

Period: weekly, monthly (calendar), or custom date range. Weeks start on the user's chosen day (default Monday).

### Rules

- **Spending** counts completed debits with kind `EXPENSE` or `FEE`. It excludes transfers, pending, failed, deleted, and transfers into savings.
- **Saved** counts net transfers into savings accounts (in minus out) within the period. A saving target may optionally bind to specific savings accounts.
- Refunds reduce the matching spending total when linked to an original; unlinked refunds reduce all-spending only.
- One currency per goal. Entries in other currencies are ignored with a visible note, never converted.
- Editing or deleting an entry recomputes goal progress immediately. Goals store only their definition; progress is always derived.
- Archiving a goal keeps its history. Hitting a goal never blocks anything; it only informs.

### Screens

- **Goals** section on Home: up to three active goals as compact progress bars; tap for detail.
- **Goal detail**: progress, remaining amount, daily pace needed or allowed, contributing entries, edit, archive.
- **Create goal**: type, amount, period, optional scope, optional name. Sensible defaults (monthly, all spending). Suggest a starting limit from the last 3 months' average when at least 4 weeks of data exist.

### Goal states

`on track`, `close` (at 80% of a limit or behind pace on a target), `over` (limit exceeded), `reached` (target met), `ended`. State changes drive nudges (section 3).

## 3. Insights and nudges

### Notification categories

All delivered as local notifications, each independently switchable in Settings → Notifications. Defaults chosen so a new user gets confirmations and credits only; habit feedback is opt-in until the user has used the app for 2 weeks, at which point a single prompt offers to enable it.

| Category | Trigger | Example | Default |
|---|---|---|---|
| Confirm | Capture needs a decision (unclear direction, possible own-account transfer, unknown account) | "₦12,000 from Kuda. Money out or in?" with action buttons | On |
| Recorded | A transaction was captured and recorded | "Recorded ₦3,500 to Shoprite" with Undo | On (existing behaviour) |
| Credit | Incoming money recorded | "₦150,000 credited to GTBank. Today's net: +₦147,000" | On |
| Goal | Goal state changes | "You've used 80% of your food budget with 9 days left" | On |
| Habit | Positive or negative pattern detected | "Spending is 22% lower than your usual week. Nice." | Off, prompted |
| Summary | Weekly and monthly recap | Total in, out, saved, top spend, goal results | Weekly on, monthly on |
| Reminder | Stale Review queue or lapsed capture | "3 alerts are waiting for you" / "Capture has been off for 2 days" | On |

### Habit signals (v1 set)

Computed locally; each needs a minimum history and has a cool-down so the same message does not repeat.

- **Spending pace**: this week or month versus the trailing 4-period median. Triggers beyond ±20% with at least 3 prior periods.
- **Saving streak**: consecutive periods with a net positive transfer into savings.
- **Large single debit**: a debit above 3× the user's median debit or a user-set threshold.
- **Frequent small debits**: repeated debits to one counterparty in a short window (e.g. 4 in a day).
- **Low-activity credit**: salary-like credit detected (recurring, similar amount, similar day) to prompt "set aside some savings?".
- **New recurring charge**: same counterparty and amount seen 3 times at a regular interval.

Tone rules: factual, brief, no shaming or alarm wording for negative signals; positive signals are acknowledged without gamified pressure. No streaks that punish a missed day.

### Delivery limits

- Maximum 3 non-confirmation notifications per day. Confirm and Recorded are exempt.
- Quiet hours (default 22:00 to 07:00) delay everything except Confirm.
- Group Recorded notifications when more than 3 arrive within 5 minutes.
- Respect OS notification permission; if denied, surface pending items inside the app (Home banner and Review).
- Every notification text is generated from templates and local numbers. No cloud call, no raw alert text echoed back beyond counterparty and amount.

### Confirmation flow

Confirm notifications use action buttons so the user can answer without opening the app:

- Direction unclear: **Money in** / **Money out**.
- Possible own-account transfer: **My account** / **Not mine**.
- Unknown account: **Add** / **Ignore**.

Answers write through the same save path as Review, set `directionConfirmed` and remember the choice. A confirmation unanswered for 24 hours stays in Review with no further prompts. On iOS, where notification actions depend on the Shortcuts capture path, confirmations fall back to an in-app prompt until action support is device-verified.

### In-app

- **Home summary**: month in, out, saved, with a one-line insight chosen from the most relevant current signal.
- **Insights screen**: trends by week and month, savings total, top counterparties, goal history. Charts use the existing design system.
- Each insight links to the entries behind it.

## 4. Data model and migration

New tables (SQLDelight, following the current JSON-payload pattern until relational constraints are warranted):

- `account(id, payload)`
- `transfer_group(id, payload)` with leg entry IDs and match method
- `goal(id, payload)`
- `insight_state(key, value)` for cool-downs and last-sent timestamps
- `my_destination(id, payload)` for remembered "mine" counterparties and account pairs

`Entry` gains `accountId` and `transferGroupId`. A versioned migration converts existing `account` text to `Account` rows and leaves all other fields untouched. Export CSV adds columns for account, transfer group and goal-relevant category once available. Backup and Recovery cover the new tables.

Persistent invariants, with tests:
- A transfer group has two legs, or one leg flagged *not tracked*.
- Both legs of a group share amount and are opposite directions; cross-currency groups store each leg's amount and no conversion.
- Deleting one leg of a group prompts about the other and never leaves a dangling link.
- Totals and goal progress never include transfer legs.

## 5. Privacy and AI

- Accounts, goals, insights and nudges are local-only. They add no new data leaving the device.
- Cloud AI remains opt-in under the current consent. If Jev is used for a transfer-or-spend suggestion, it receives the alert and candidate counterparts only, never goals, balances or full history, and its answer stays a suggestion in Review.
- The user's own names and account hints are stored locally and are not sent in cloud requests.
- The privacy policy and Data safety text need no change for local-only features. Re-check before shipping if any sync or cloud insight is later added.

## 6. Plans and limits

Accounts, transfers, goals and local nudges are Free features: they run locally and cost nothing. Suggested gating for later consideration, to be decided by the monetization owner:

- Free: up to 2 goals, weekly summary.
- Lite and Premium: unlimited goals, monthly summary, habit insights.

This is a proposal; the base transfer and savings behaviour should never be paywalled because it affects correctness of totals.

## 7. Phased delivery

**Phase 1: Accounts and transfers** (correctness first)
- `Account` entity, migration, account picker, savings flag.
- Transfer detection signals 1 to 3, confirm prompt, unlink/relink.
- Totals continue excluding transfers; add *saved* and *withdrawn from savings* figures.
- Tests: pairing window, same-amount-different-account negatives, delete-one-leg, undo.

**Phase 2: Goals and categories**
- Minimal category list (user-editable), category on entry, auto-category from counterparty memory.
- Spending limit and saving target, Home progress, goal detail.
- Goal state notifications.

**Phase 3: Insights and nudges**
- Notification categories and Settings, delivery limits, quiet hours.
- Weekly and monthly summary, habit signals, Insights screen.
- Interactive confirmation actions on Android; iOS after device verification.

Each phase ships independently and leaves the app consistent.

## 8. Acceptance criteria

1. Marking a bank as Savings and moving ₦X into it from a spending account records one transfer; spending and income totals are unchanged; *saved* increases by ₦X.
2. A debit and credit of equal amount in two different accounts within the window prompts exactly once, and the answer is remembered for that pair.
3. Two independent same-amount debits to one account are never linked as a transfer.
4. Deleting or editing any entry updates every goal and the Home summary without restart.
5. A spending limit shows `close` at 80% and `over` above 100%, with exactly one notification per state change per period.
6. With notification permission denied, every pending confirmation is visible inside the app.
7. A user can disable each notification category; disabled categories send nothing.
8. No more than 3 non-confirmation notifications are delivered in a day, and none during quiet hours except Confirm.
9. Migrating a ledger with existing text accounts loses no entries and changes no totals.
10. All new features work offline with AI consent off.

## 9. Open decisions

- Transfer pairing window default (10 minutes proposed) and whether to widen for slow inter-bank transfers.
- Whether "my names" should be required during onboarding or discovered from the first confirmed transfer.
- Category set: fixed starter list versus fully user-defined from day one.
- Whether habit nudges should be opt-in permanently or after the two-week prompt.
- Free versus paid goal limits, owned by the plan policy.
- iOS confirmation actions depend on Shortcuts capture verification; treat as unconfirmed.
- Data needed for tuning thresholds (spending pace, large debit multiple) should come from real usage, not these placeholders.

## Implementation status

Implemented in `shared` (JVM tests: 21 new, 39 total pass) and in the Compose UI and Android host. The Android debug build compiles. The UI has not been run on a device or emulator, and the iOS framework has not been built.

Built:
- Accounts with a Savings role, alert-to-account hints, rename and hide; "your names"; own-account transfer detection (named self, other account, remembered destination), pairing within a window, one-time yes/no question with remembered answers; link, mark and unlink from the receipt; delete and edit never leave a dangling link.
- Saved and withdrawn figures; transfers stay out of income, spending and goals.
- Spending limits (all spending, or payments to a named counterparty) and saving targets; weekly, monthly or dated periods; on track, close, over, reached, ended; Home strip and Settings → Goals.
- Insight signals (spending pace, saving streak, large debit, frequent small debits, new recurring payment), weekly and monthly summaries, goal and reminder nudges, per-category switches, quiet hours, three-a-day cap, Android daily worker.

Differences from this spec:
- Accounts are keyed by unique name, not an `accountId`. Existing text accounts become accounts on first launch. Accounts, goals, remembered answers and notification switches are stored as JSON in the existing `preference` table rather than new tables, which avoids a schema migration on the published database.
- Spending counts outgoing completed money of any kind except transfers, because captured alerts are mostly `UNCLASSIFIED`. Refunds reduce unscoped totals only.
- The "Money in" nudge is the day's net added to the recorded alert, not a separate notification.
- Pairing uses the capture timestamp, so entries created without one (older data) are never paired.

Not built yet:
- Categories, so limits cannot be scoped to one; the counterparty scope stands in for it.
- Free versus paid goal limits. Everything is currently free.
- A setting for the pairing window (fixed at 10 minutes in the UI; the store reads an optional preference) and the two-week prompt to enable habit nudges (habits are simply off by default).
- Interactive notification buttons for confirmations; the question appears on Home only.
- Nudge delivery on iOS (no sink is registered there), a dedicated Insights screen, a salary-like credit signal, and the capture-lapsed reminder.
- On-device verification of all of the above.
