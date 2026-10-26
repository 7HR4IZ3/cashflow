# Delivery plan

## Phase 0: prove the capture paths

Before the full app scaffold, build the smallest disposable probes needed to verify platform behaviour. Keep synthetic test data separate from any real ledger.

Android probe:

- Select one test source, grant listener access, and persist a captured event.
- Exercise notification updates, key reuse, grouped notifications, process recreation, and permission revocation.
- Check redacted and incomplete input handling and capture filtering.

iOS probe:

- Register a minimal Cashflow App Intent accepting the fields exposed by the notification automation.
- Configure an automation on a physical supported iPhone and document exact OS/build and guide labels.
- Verify source app selection, input fields, source identity availability, execution settings, locked and unlocked behaviour, cold launch, and permission prompts.
- Prove input persistence and the supported path for deferred work after a slow or failed model call.
- Verify repeated delivery, missing fields, and privacy/redaction cases.
- Determine the oldest supported OS through testing and current official release documentation. Test on the oldest supported and latest stable release before promising a compatibility range.

Exit: saved probe evidence and screenshots identify what works, what needs user interaction, and the supported capture range. If iOS capture is not reliable enough, revise the iOS promise and guide before implementation. Never mark this gate passed from documentation alone.

## Phase 1: scaffold and manual ledger

- Confirm Kotlin/Compose Multiplatform or select Flutter before scaffolding.
- Choose storage library and exact dependencies using current Context7 guidance.
- Build shared money types, schema, migrations, transaction repository, and native host projects.
- Implement Home, Activity, transaction detail, add/edit/delete/restore, accounts, and currency-separated summaries.
- Implement the onboarding introduction and sample sandbox.

Exit: both platforms can manage a local ledger; restarts preserve it; manual corrections update totals. Meaningful common tests cover currency arithmetic, pending exclusion, internal transfers, and recoverable deletion.

## Phase 2: capture and setup guides

- Integrate Android listener and iOS capture intent using the proven contracts.
- Persist jobs, source selection, permission state, and resumable setup progress.
- Add platform guides with real screenshots and text equivalents.
- Add known-format parsing, pasted-text capture, exact replay handling, and Review.
- Implement capture health without claiming visibility into inaccessible iOS automation state.

Exit: one supported source event reaches the same engine on each platform. Offline capture does not lose an acknowledged input. Setup can be skipped, resumed, and repaired.

## Phase 3: model decisions and reconciliation

- Evaluate Jev or another bounded classifier on representative redacted fixtures.
- Add consent, provider adapter, secure proxy if needed, bounded context retrieval, cost limits, and retry policy.
- Implement lifecycle matching, duplicate evidence, own-account transfer linking, reversals, fees, and protected user edits.
- Implement confirmation notifications and Undo/Edit actions, including stale-action handling.

Exit: measured auto-record and auto-merge behaviour passes agreed precision targets. As a proposed pilot gate, require zero false merges in the curated release regression set; this does not imply zero production risk. Set numeric production targets only after dataset size and source coverage are known.

## Phase 4: complete the first release

- Finish filters, categories, summaries, CSV and backup export/restore, and Recently deleted.
- Add notification preferences, lock-screen privacy, retention controls, app lock, and complete reset.
- Test accessibility, locale/date handling, currencies, offline recovery, migrations, concurrency, and low-storage failures.
- Prepare privacy disclosures, support copy, store listings, and only the permissions justified by verified features.

Exit: all first-release product acceptance criteria pass on supported Android and iOS devices, with known limitations written in release notes.

## Verification scenarios

1. Replay a payment, restart the process, and retry a job. One ledger entry remains.
2. Send two same-amount payments to the same person close together. Both remain unless a shared reference proves otherwise.
3. Send debit and credit alerts for an owned-account transfer. Both account legs exist; aggregate income/spending excludes the principal and includes any fee correctly.
4. Move pending to completed, pending to failed, and completed to reversed. Completed totals reflect actual supported movements once.
5. Edit an amount while analysis is running. The stale result cannot overwrite the edit.
6. Undo, delete, restore, merge, and split. Replayed evidence does not recreate suppressed entries.
7. Deny confirmation notifications but allow capture. The ledger still records; Review remains accessible.
8. Disable cloud analysis, go offline, then return online. No queued upload bypasses the user's consent choice.
9. Resume onboarding after termination in system settings or Shortcuts. Progress survives; verification status remains truthful.
10. Export and restore a ledger containing multiple currencies, pending items, internal transfers, and corrections. IDs, relations, values, and totals survive accurately.
11. Reset Cashflow while an iOS automation remains installed. Subsequent invocations cannot silently start new capture without the new installation's setup choice.

## Open decisions

- Confirm the proposed Kotlin Multiplatform and Compose stack.
- Select first launch market, languages, currency defaults, and supported banking apps using representative notification samples.
- Determine minimum OS versions from Phase 0, not from neighbouring apps' settings.
- Choose storage and verify protection and concurrent native access.
- Select the analysis provider and a viable no-login credential provisioning approach for the optional proxy.
- Confirm retention defaults, backup format, and Recently deleted duration.
- Choose pricing after estimating model cost and the amount of manual review. No paywall should block users from correcting or exporting their existing records.
- Decide whether optional cloud sync belongs after the first release. It is not assumed by this plan.

## Completion reporting

Report source implementation, passing automated checks, simulator/emulator results, physical-device capture, and store readiness separately. A successful build alone does not prove notification access, Shortcuts automation, or model accuracy.
