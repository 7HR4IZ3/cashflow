# Cashflow

Cashflow turns financial notifications into a money diary that users can inspect and correct. Users choose the sources to watch, receive automatic transaction records, and can undo or edit them. Manual entry remains available throughout the app.

Status: first running draft. Android and iOS simulator builds pass, with a shared ledger and a UI implemented by GLM-5.3 Flash Free. Android synthetic capture and manual-entry persistence are verified; iOS onboarding renders. See [implementation status](plans/implementation-status.md) and [verification](plans/verification.md) for scope and gaps. This is not a production-ready release.

## Repository

[7HR4IZ3/cashflow](https://github.com/7HR4IZ3/cashflow) owns the app source, plans, and verification notes. The Android utility portfolio references this repository as a Git submodule at `apps/cashflow`.

Clone this app directly, or initialize it from the portfolio with `git submodule update --init apps/cashflow`. Commit app changes here first, push them, then commit the updated submodule pointer in the portfolio.

## Build

Use JDK 21 and an Android SDK with platform 36. Set the SDK path in an untracked `local.properties` file.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew :shared:jvmTest :androidApp:assembleDebug
```

For the iOS host, install Xcode and XcodeGen, then generate `iosApp/Cashflow.xcodeproj` with `xcodegen generate` from `iosApp/`. The target's build script builds the shared Kotlin framework. Signing for a physical device needs a development team configured in Xcode. Notification-triggered Shortcuts compatibility requires separate device verification; the host deployment target is not a capture compatibility guarantee.

For a lower-memory Apple Silicon simulator build, use `scripts/build-ios-simulator.sh`. It builds Kotlin and Swift serially, disables Kotlin native caches, and targets ARM64 only. It still requires free disk space for native dependencies and build outputs.

## Planning documents

Start with the [first-draft overview](plans/README.md) and [Night current concept](plans/night-current-design.md). Its [interactive prototype](plans/night-current-concept.html) proposes the new icon-led direction before native implementation. Earlier [reference research](plans/fresh-current-research.md) remains available. The `plans/` directory is the current source of truth; `docs/` contains redirects for earlier links.

- [Product plan](plans/product-plan.md): scope, screens, transaction behaviour, and release acceptance criteria.
- [Onboarding and setup](plans/onboarding-and-setup.md): introduction, permissions, Android setup, guided iOS Shortcuts configuration, and recovery.
- [Transaction engine](plans/transaction-engine.md): notification context, model decisions, matching, accounting rules, and corrections.
- [Technical architecture](plans/technical-architecture.md): proposed shared codebase, native integrations, storage, privacy, and service boundaries.
- [Delivery plan](plans/delivery-plan.md): staged implementation, tests, release gates, and open decisions.
- [Platform evidence](plans/platform-evidence.md): sources and platform questions that need device verification.
- [Implementation status](plans/implementation-status.md) and [verification](plans/verification.md): what is built, checked, and still pending.

## Working assumptions

- Android and iOS are both intended targets. React Native is excluded.
- Kotlin Multiplatform with Compose Multiplatform is the implemented stack for this draft.
- Android uses notification listener access. iOS uses a user-configured Shortcuts notification automation that calls a native Cashflow action, subject to OS and device verification.
- The first release stores the ledger locally and does not require a Cashflow account. Remote model analysis is optional and requires a separate explanation and opt-in.
- Jev is a candidate decision provider. No specific provider, minimum OS version, launch market, pricing, or cloud service has been selected.
- This app is independent of the neighbouring portfolio apps. Do not reuse their package identities, permission grants, credentials, release configuration, or databases.

Start with the capture feasibility gates in the delivery plan before promising automatic capture on a particular iOS version.
