# Cashflow implementation plan

First draft, 2026-10-25. This folder is the current source of truth for what we intend to implement. It contains plans, not claims of implemented or device-tested behaviour.

## Current redesign

The user selected **Fresh current** on 2026-10-26, replacing the original paper-ledger appearance. Read the [reference comparison](fresh-current-research.md), [screen and interaction specification](fresh-current-design.md), and [interactive screen comparison](fresh-current-comparison.html) before the next UI implementation pass. The HTML is an isolated example-data prototype, not the installed app. The former [UI design](ui-design.md) remains historical context; its palette and stamp-heavy presentation are superseded.

## The app idea

Cashflow is a money diary that builds itself from transaction alerts. The user chooses which sources to watch. Cashflow interprets relevant alerts, compares them with recent evidence, and records or updates transactions. Clear entries stay recorded unless corrected. Uncertain entries wait for review.

The user remains in control through manual entry, editing, Undo, deletion and restoration, and correction of duplicate matches. The app tracks money; it never initiates or reverses bank payments.

## Main goals

1. Reduce manual tracking while making every recorded amount explainable.
2. Record each real movement once, without merging separate payments by mistake.
3. Make corrections quick and preserve the user's edits.
4. Give Android and iOS a shared ledger experience with platform-specific capture setup.
5. Help users understand and configure the app through a short, practical onboarding.
6. Make the interface fun, minimalist, intentional, and easy to scan.
7. Keep financial data local by default, with an explicit choice for remote analysis.

## Confirmed direction from the user

- Android and iOS, preferably one shared codebase. React Native is excluded.
- The user configures which app notifications to watch.
- A model can interpret notifications with relevant recent context.
- Automatically recorded transactions offer Undo and Edit; doing nothing keeps a clear entry.
- Full manual add, edit, and remove functionality.
- Onboarding explains the app, how it works, and how to use it.
- iOS setup includes a guided Shortcuts configuration flow.
- Visual design is fun, minimalist, and intentional. Prefer meaningful icons and concise icon-plus-text controls over explanatory paragraphs.
- GLM-5.3 Flash Free owns UI design tasks, with parent review for consistency and correctness.

## Proposed defaults

These keep the first draft actionable and remain changeable before scaffolding:

- Kotlin Multiplatform with Compose Multiplatform, plus native Android capture and Swift App Intents.
- Local ledger, no required sign-in, and no first-release cross-device sync.
- Local known-format parsing with optional remote decision analysis for ambiguity.
- Jev as a provider to evaluate, not an approved dependency.
- Four destinations: Home, Activity, Review, and Settings. GLM's UI proposal may refine presentation without removing these responsibilities.
- Confirmed activity totals separated by currency, without implying a bank-verified balance.
- Review required for uncertain financial facts or relationships.
- Notifications for each clear recorded transaction as an initial selectable preference, with review-only, digest, and off alternatives.

## Feature inventory

### Introduction and setup

- A short product explanation through an example payment.
- A safe demonstration of automatic recording, Undo, Edit, and Review.
- Currency confirmation and optional account setup.
- Platform source selection and permission explanation.
- Android notification listener settings with grant verification.
- Version-specific iOS Shortcuts guidance with screenshots, field mapping, resumable progress, a sample test, and a real source-event check.
- Optional cloud-analysis consent and confirmation notification preferences.
- Skip and resume paths that always leave manual entry available.

### Capture and interpretation

- Per-source selection and pause, plus global capture pause.
- Durable event intake and a recoverable processing queue.
- Local parsing and bounded model decisions using selected recent context.
- Relevance, direction, amount/currency validation, status, economic meaning, and match decisions kept separate.
- Duplicate evidence linking, pending-to-completed updates, failed payments, refunds, reversals, fees, and owned-account transfers.
- Explicit uncertainty and provider-failure states.
- No silent overwrite of user corrections and no recreation of an undone capture from a simple replay.

### Daily use

- Recorded money in, money out, and net movement for a chosen period and currency.
- Searchable transaction timeline with practical filters.
- Transaction details with provenance and an understandable reason for recording.
- Add/edit/delete/restore, manual cash entries, categories, notes, and account assignment.
- Review, ignore, confirm, merge, and split mistaken merges.
- Undo/Edit confirmation actions and privacy-aware lock-screen content.
- Owned-account management and transfer grouping that avoids inflated totals.

### Controls and data

- Capture health and setup repair guides.
- Notification modes, context settings, retention, and local/remote processing controls.
- App lock, accessible interfaces, and reduced-motion support.
- CSV export plus tested versioned backup and restore.
- Recently deleted and complete reset with iOS automation removal guidance.

## Design rule

Use an icon alone when the action is familiar and the context makes it clear. Add a short label when meaning is uncertain. Keep necessary text for amounts, currency, permission choices, errors, destructive consequences, and setup instructions. Every icon-only action needs an accessible name. A playful interface must still make financial status unambiguous.

The specific visual proposal and screen behaviour belong in [UI design](ui-design.md), owned by the requested GLM model. No palette, typeface, or mockup is approved merely by appearing in that draft.

## Build order

1. Prove Android capture and iOS Shortcuts input on devices.
2. Confirm the shared stack and storage, then implement a manual ledger on both platforms.
3. Add capture, onboarding, and the verified setup guides.
4. Add evaluated model decisions, reconciliation, and confirmation actions.
5. Complete privacy, export/restore, accessibility, and release verification.

No app implementation is included in this planning task. Minimum OS versions, bank coverage, model accuracy, and background capture reliability are validation gates.

Implementation has since begun: see [implementation status](implementation-status.md) and [verification](verification.md). The original build order above remains the product roadmap; it does not mean every planned feature is complete in the first running draft.

## Remaining decisions

First market and bank apps; framework confirmation; processing provider and consent terms; minimum supported OS after probes; storage library; backup protection and retention defaults; model quality thresholds; pricing and any later sync scope.

## Detailed plans

- [Product plan](product-plan.md)
- [Onboarding and setup](onboarding-and-setup.md)
- [UI design](ui-design.md)
- [Transaction engine](transaction-engine.md)
- [Technical architecture](technical-architecture.md)
- [Delivery plan](delivery-plan.md)
- [Platform evidence and unresolved questions](platform-evidence.md)

If drafts conflict, confirmed user requirements take priority. The UI document owns presentation proposals; the transaction and product plans own financial behaviour. Resolve conflicts before implementation.
