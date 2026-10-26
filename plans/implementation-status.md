# First implementation pass

First running draft, updated 2026-10-26. This is an implementation log, not a release-readiness claim.

## Decisions used for this draft

- Kotlin 2.2.21 and Compose Multiplatform 1.9.3, shared UI plus native Android and iOS hosts.
- SQLDelight 2.1.0 with Android and native SQLite drivers. Chosen for explicit SQL, generated shared queries, and synchronous native intent ingestion without a second persistence layer. The initial schema stores versioned Kotlin records as JSON inside SQLite rows; more relational constraints can be added as transfer groups and accounts become independent entities.
- Local-only known-format interpretation. Cloud credentials, provider provisioning, evaluation thresholds, and consent terms are not selected, so remote analysis is not enabled.
- Android compile/target SDK 36 with a draft minimum API 26. The iOS app host has a draft build deployment target of iOS 16; this is not a minimum for notification-triggered Shortcuts capture. That feature's minimum and locked-device behaviour remain unverified.
- GLM-5.3 Flash Free implements UI, following the design plan with room for creative iconography and restrained animations. Parent handles core, native integrations, and independent verification.

## Implemented source so far

- Shared typed money, currency exponent handling, validated dates, direction-based totals, and transfer/pending exclusion rules.
- SQLite ledger, preferences, captured events, Review, deletion/restoration, and replay protection.
- Local conservative parser, OTP rejection, reference-based event linking, pending completion, and protection against overwriting user edits.
- CSV generation with quoted fields and spreadsheet formula protection.
- Android listener, selected-source filtering, generic confirmation payloads, revision-checked Undo, permissions, and CSV share plumbing.
- Swift capture App Intent and a native host around the shared Compose framework. The intent accepts notification text, a source label, and an optional event identifier.

## Validation completed for this draft

Ten JVM tests pass, including classification and aggregate-overflow validation. An Android debug APK builds and runs on the emulator. Manual entry/relaunch persistence and selected-source synthetic capture, reference duplicate linking, and ambiguous-alert Review were verified. The iOS ARM64 simulator framework and Swift host also build successfully, App Intent metadata is generated, and onboarding renders on the iPhone simulator. See [verification](verification.md) for evidence and remaining gaps. Physical bank capture is not yet verified.

## Important remaining scope

- Exhaustive UI action coverage, accessibility, reduced-motion, and device-size verification. The shared screens and animations are implemented by GLM.
- Runtime notification Edit/Undo and stale-action UX checks. Entry-ID routing and revision checks exist in source.
- Physical iPhone Shortcuts setup and bank-notification capture, including OS/version guidance and verified screenshots.
- Model/provider integration and accuracy evaluation.
- Durable deferred analysis jobs, full cross-source matching, paired internal-transfer legs, merge/split, refund/reversal reconciliation, separate account entities, and correction history.
- Versioned backup/restore, complete reset, explicit raw retention enforcement, encrypted export options, and biometric app lock.
- Store packaging, icons, accessibility/device coverage, and release privacy disclosures.

Do not describe the first draft as the full planned release until those gaps are implemented and checked. A successful App Intent build does not establish automatic notification capture.
