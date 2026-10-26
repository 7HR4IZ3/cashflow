# Product plan

Draft date: 2026-10-25. This document specifies proposed behaviour, not implemented functionality.

## Purpose and audience

Help people who use several banks or wallets understand incoming money, spending, and transfers without entering every payment manually. The core promise is: "Your money moves. Cashflow keeps track."

Success means that one real payment appears once, separate payments stay separate, and mistakes are easy to fix. The app records observed activity; it cannot promise a complete bank statement or verified available balance.

## First-release scope

- Explain the product and demonstrate a captured payment before requesting access.
- Let users choose financial notification sources and pause capture globally or per source.
- Capture selected Android app notifications after notification access is granted.
- Guide iOS users through a supported Shortcuts notification automation and receive its input through a Cashflow action.
- Provide manual entry and pasted transaction text on both platforms.
- Extract transaction details, classify direction and status, link duplicate evidence, and detect possible own-account transfers.
- Record clear completed transactions automatically. Put uncertain cases in Review, outside confirmed totals.
- Send configurable confirmation notifications with Undo and Edit.
- Add, edit, delete, restore, search, and filter transactions in the app.
- Create and archive accounts; identify accounts owned by the user.
- Handle pending payments, failures, refunds, reversals, and separate fees.
- Provide basic per-currency summaries and CSV export. Include a versioned backup export and restore path before release.
- Show capture health, privacy controls, and a resumable setup guide.

Later work includes screenshot recognition, statement import, budgets, recurring payment detection, bank integrations, optional account sync, and richer reporting. Cross-device sync, automated currency conversion, financial advice, and payment initiation are outside the first release.

## Main journey

1. Learn what Cashflow records and what notification access means.
2. Choose currency and optionally name an account.
3. Configure capture for the current platform, or continue with manual entry.
4. Choose local processing or opt into remote analysis.
5. See a clearly labelled sample transaction and try Edit or Undo.
6. Receive a real source event, inspect its interpretation, and correct it if needed.
7. Return to a timeline and summaries that reflect those corrections immediately.

## Navigation and screens

Use four main destinations: Home, Activity, Review, and Settings. Keep Add transaction available from Home and Activity. Accounts and capture setup live within Settings, with relevant links elsewhere.

### Home

Show money in, money out, net movement, the selected period, and currency. Summaries default to completed, accepted entries. Own-account transfers are shown separately and excluded from aggregate income and spending. Unknown classifications remain visible without pretending to be income or spending.

Money in and money out are direction-based cash movement totals, not synonyms for income and spending. Completed, accepted movements with known amount, currency, and direction contribute even when their economic category is unclassified. Net recorded movement is money in minus money out for the selected currency and scope. For an all-accounts view, exclude identified internal principal from these headline in/out figures and show it separately; an individual account's activity still includes its transfer legs. Income and spending breakdowns require economic classification. Never show a combined multi-currency hero amount.

Show recent activity, a Review count, and a restrained capture status message when action is needed. Label totals "Recorded activity". Do not present inferred totals as a verified bank balance. An empty home offers setup and manual entry; sample data never enters real totals.

### Activity

Show date-grouped rows with amount, direction, counterparty, account, category, status, and origin. Search recipients, merchants, notes, and references. Filter by date, account, currency, category, direction, status, and manual or captured origin.

A transaction detail view includes linked source events, a plain explanation of its classification, edit history, and actions to edit, delete, merge, split a mistaken merge, or mark as own-account transfer. Merging and splitting must explain their effect before committing.

### Add and edit transaction

Required fields are amount, currency, occurrence date, direction, and status. Account may be Unassigned. Category, counterparty, and note are optional. Type choices include income, expense, transfer, refund, fee, and unclassified movement. Direction and economic meaning remain distinct.

For an own-account transfer, request both accounts when known and support different currencies and explicit fees. Do not infer an exchange rate. Validate amounts and compatible fields before saving. Preserve unsaved edits when switching to a guide or app settings where practical.

Deleting uses a recoverable state. Offer Undo immediately and a Recently deleted view with a proposed 30-day restoration period. Replayed source evidence must not recreate the deleted transaction. Permanent deletion explains which evidence will also be removed.

### Review

Each item explains the uncertainty: missing amount, unknown currency, conflicting evidence, possible duplicate, possible internal transfer, or unknown account. Users can confirm, edit, link to an existing record, or ignore it. Partial data stays editable. Distinguish a processing failure from an ambiguous financial interpretation.

### Settings and accounts

Include capture sources, platform setup guide, model processing choice, notification preferences, context window, currencies, accounts, privacy and data retention, export and restore, app lock, help, and reset.

Accounts need a user-defined name, currency, and optional institution and masked suffix. Never require full bank credentials or full account numbers. Archive accounts instead of breaking historical links. New evidence about an unknown account must not be silently assigned to the last-used account.

## Confirmation behaviour

Clear completed entries are saved before the confirmation is shown:

The following detailed template is available only when the user opts into financial notification details. The default system notification title is "Transaction recorded" with a generic body, and neither contains an amount, recipient, or account name. Redact before posting the notification, rather than relying on OS preview settings. The in-app detail may show the full record after applicable app authentication.

> Recorded ₦8,500 sent to Ada
> Undo · Edit

Doing nothing keeps the entry. Dismissing a notification has the same effect. Undo removes the entry's effect from totals, suppresses that captured evidence, and offers restoration in the app. It does not reverse an actual bank transfer. Edit opens the specific entry.

Ambiguous cases use a different message:

> Payment needs a check
> Review · Ignore

These do not enter confirmed totals until the user resolves them. Exact duplicate evidence normally attaches quietly. A status update modifies the existing record without issuing a second "new payment" alert.

Notification modes are every recorded transaction, review only, digest, and off. Use every recorded transaction as the proposed initial preference, explicitly selectable during setup. Notification permission refusal does not block the ledger. Hide amounts and names on the lock screen by default. A stale notification action must load the current record before changing anything.

## Financial correctness

- Keep currencies separate. No sum of NGN and USD without an explicit conversion policy.
- Preserve both sides of an internal transfer while excluding principal from aggregate income and spending. Record fees independently.
- A credit is not necessarily income. Refunds, borrowing, and transfers need separate meaning.
- Pending and failed movements are visible but excluded from completed cash movement totals.
- Keep the completed original and a linked reversal or refund when evidence supports it. Do not count a failed pending authorisation and its cancellation as completed money movements.
- Notifications may arrive late or out of order. Distinguish event time, capture time, and user-corrected occurrence time.
- User edits override later automation unless the user explicitly accepts a proposed change.

## Accessibility and everyday states

The user wants a fun, minimalist, intentional interface with as little text as practical. Use icons for familiar actions and icon-plus-short-label controls for unfamiliar meanings. Keep text for financial facts, consent, corrective guidance, and consequences. UI design is delegated to GLM-5.3 Flash Free in [the UI design plan](ui-design.md).

Support screen readers, scalable text, comfortable tap targets, reduced motion, and direction labels in addition to colour. Format amounts and dates for locale without losing original currency or exact values.

Design empty, loading, offline, permission denied, unsupported OS, no selected sources, capture paused, model unavailable, partial notification, storage failure, and restore failure states. Persist a captured input before acknowledging success. A failed save must never display "Recorded".

## Product acceptance criteria

- A user can complete onboarding without granting capture or cloud permissions and add a manual entry.
- A captured payment appears once after replay or retry; two legitimate same-amount payments remain separate.
- Undo, edit, delete, restore, merge, and split update all summaries consistently.
- A user's edited amount survives later notifications and model retries.
- A transfer between owned accounts changes account activity without inflating income or spending.
- Review items do not become accepted through silence or elapsed time.
- Capture status distinguishes configuration, last observed input, and last successful processing.
- Both platforms support the same ledger operations even when capture mechanisms differ.
