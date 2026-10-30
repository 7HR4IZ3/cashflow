# Native Night current implementation

The native Android/iOS port uses `night-current-concept.html` as its visual reference. The HTML remains a sandbox; the native app uses the existing SQLite ledger and platform capture hooks.

## Data integration

- Ignoring an alert retains its review payload and marks its captured event ignored. Active Review and ignored-alert recovery read separate lists. Restoring the same alert repeatedly does not duplicate it, including after recreating the store.
- Saving a resolved or ignored Review item is rejected. Editing a deleted record requires restoring it first.
- Review drafts carry `directionConfirmed`. Ambiguous direction cannot be saved until explicitly confirmed. A readable amount and currency survive an ambiguous-direction parse; missing currency is left unknown.
- `CashflowState.today` comes from the platform clock, allowing native period controls to use the real date rather than the sandbox anchor.
- Existing currencies and exponents remain supported, including JPY and KWD. Completed accepted movements count by direction; own transfers, deleted records, pending records and failed records remain excluded.

The ignored-alert change adds a query, not a database schema migration. Alerts ignored by older versions have already lost their review payload and cannot be reconstructed by this change.

## Verification status

- Shared JVM tests: 13 passed on 2026-10-27.
- Shared Kotlin iOS simulator compilation: passed.
- Native UI ported (Home, dock, Add calculator, Review, receipt sheet, Settings/Recovery, intro). Android debug compile, shared JVM tests and iOS simulator compile pass (build under JDK 21).
- Emulator (API 36) walkthrough: Home, Add save, Review and Settings render and work. Fixed on the way: `PaperDates.daysToCivil` year bug (headers read "Sun -10 ?") and dark-on-dark status bar. Remaining gaps: no "Try a sample alert" demo; reduce motion not persisted; "View record" toast action unavailable after adding (save returns no id); new records get a default "Example entry" name.

Real banking-notification capture and physical-iPhone Shortcut automation are separate from UI verification. No remote model integration is included in this port.
