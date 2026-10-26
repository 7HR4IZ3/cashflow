# First-draft verification

2026-10-26 update. This records checks performed during implementation. It does not establish production bank coverage or App Store readiness.

## Shared logic

Ten JVM tests pass: exact integer money and currency exponents; separate-currency totals; pending/internal-transfer exclusions; conservative alert parsing and OTP rejection; real calendar validation; persistence and replay protection across store recreation; same-amount separate payments; pending completion; review resolution and CSV formula escaping; classification validation; and aggregate overflow protection. Some scenarios share a test method.

The shared ledger also compiled for `iosSimulatorArm64`. Full shared-UI framework and Swift host verification are tracked separately below.

## Android runtime checks

An Android debug APK was built, installed, and launched on the `motion-cues-api36` emulator.

Verified through the UI:

- First-run onboarding and example controls render, currency selection is reachable, and Skip enters the app.
- A manual NGN 1,250.50 expense saves and updates Home totals.
- The entry survives a forced app termination and relaunch.
- Source selection persists.
- Android's native notification-listener permission can be enabled for Cashflow.

Verified using synthetic notifications posted through Android's shell notification command:

- A selected-source NGN 8,500 debit creates a captured entry.
- A second notification carrying the same reference links evidence without creating another payment.
- An independent NGN 1,000 credit creates a separate entry.
- An ambiguous USD 50 message enters Review and leaves confirmed totals unchanged.
- Cashflow posts a generic confirmation title/body without financial content after its notification posting permission is granted.

Database assertions checked three total entries and one unresolved review item after those probes. All data was synthetic. This does not verify any real bank's formats, notification delivery, or redaction behaviour.

The first probe was initially misquoted by the test shell command. Correcting the command delivered the intended alert and established the listener path. No capture failure was inferred from that malformed test input.

Runtime review found and addressed: an unwanted reference suffix in recipient parsing, underlying-screen accessibility leakage beneath editor/detail routes, source toggle accessibility, Home-tab label wrapping, status-bar icon contrast, and introductory copy length. Those fixes compile in the final shared UI on both platforms. Native date generation now includes the device's local offset, so a blank manual date follows the local calendar day.

Notification Undo/Edit actions have native plumbing, but an actual Undo tap and stale-notification UX have not yet been verified in the emulator. App deletion/restoration is covered by shared tests; UI flow coverage is not exhaustive.

## iOS and environment

The Xcode host project generates and exposes the Cashflow scheme. Shared core and shared UI iOS compilation pass. The ARM64 simulator framework linked successfully, and the full Swift host build passed with App Intent metadata extracted. The app installs and launches on the iOS 27.2 simulator. A screenshot confirms the shared onboarding renders.

The first full iOS build failed while downloading UI dependencies with `No space left on device`. The simulator automation driver also timed out during an earlier screen inspection. The physical-iPhone Shortcuts notification bridge, background/locked execution, and source-app field mapping remain unverified.

A reduced-memory, ARM64-only build approach succeeded: build the Kotlin framework without native caches, let its process exit, then build the Swift host separately with `CASHFLOW_SKIP_KOTLIN_BUILD=YES`. The repeatable command is [build-ios-simulator.sh](../scripts/build-ios-simulator.sh). Both build logs reported success.

The iOS driver still returns `Daemon request timed out` for UI inspection. Screenshot capture works, but interactive iOS entry/edit and Shortcuts-automation tests remain unverified. A native `simctl` screenshot attempt could not overwrite a Documents-folder artifact due to host permissions; the agent-device screenshot was captured successfully instead. Neither issue is evidence of an app rendering failure.

Final follow-up: the Android APK rebuilt successfully, ten tests remained green, and `compileKotlinIosSimulatorArm64` passed after the responsive ledger-row correction. The installed final Android build shows Home without tab-label wrapping. The editor accessibility tree contains only editor controls, and source/setting toggles expose their checked state. The unclassified stamp now occupies its own line without competing with the amount. `android-home-final.png` records that final layout. The previously linked and launched iOS host predates this last row-only correction; its updated shared UI was compile-checked, not relinked and relaunched.

Screenshots in `artifacts/` are synthetic test data. `ios-onboarding.png` shows the current shared onboarding. Other Android screenshots predate the final copy/navigation/accessibility changes and must not be treated as final design QA.

## Remaining release gates

Real banking samples and supported-source evaluation; selected remote model/provider and opt-in; fuller reconciliation and transfer-account modelling; robust queue recovery; backup/restore and reset; retention enforcement; biometric lock; notification action runtime coverage; accessibility/large text/reduced-motion checks on both platforms; device signing; and store privacy/release requirements.
