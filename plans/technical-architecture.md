# Technical architecture

## Proposed stack and ownership

Use Kotlin Multiplatform for shared transaction logic and Compose Multiplatform for shared screens. Native Android Kotlin owns notification listening and system permissions. A native Swift App Intents integration exposes the iOS capture action and passes input to shared ingestion through a narrow interface.

This is a proposed stack, pending user confirmation and capture feasibility. Choose dependency versions at scaffolding time using current Context7 and official documentation. No framework or storage version is pinned by this draft.

Suggested future project layout under this folder:

- `shared/`: money types, ledger rules, matching, parsing, repositories, processing orchestration, and common tests.
- `composeApp/`: shared screens, navigation, presentation state, onboarding, and accessibility.
- `androidApp/`: Android application entry, listener, permission checks, capture queue scheduling, and notification actions.
- `iosApp/`: Xcode host, Swift intents, secure storage integration, local notification actions, and Shortcuts integration.
- `fixtures/`: synthetic/redacted examples and expected decisions.
- `services/analysis/`: optional provider proxy only if remote analysis is selected.
- `plans/`: product requirements, design proposals, guides, and verification gates.
- `docs/`: redirects from earlier planning links; future technical evidence must be named and linked clearly.

Keep the shared business modules independent of UI and OS callback types. Inject platform implementations for secure storage, capture status, scheduling, notifications, clock, and ID generation. Every input source calls the same ingestion contract.

## Persistence

Use a transactional SQLite-backed shared repository. Evaluate Room KMP and SQLDelight during scaffolding against migration, encryption, and Swift intent access requirements. Select one and record the rationale before writing the schema.

Require schema migrations, crash recovery, integer money storage, indexed candidate lookup, revision checks, and transactional ledger/evidence writes. Never allow app and intent processes to race through uncoordinated file writes. If an intent extension is used, verify shared-container entitlements and process-safe database access; do not assume an extension is required.

Persist captured inputs before acknowledgement. Keep queue recovery independent of a screen being open. On iOS, prove the supported intent execution and deferred work path, including locked-device data protection. A queue entry is durable, but iOS does not guarantee arbitrary immediate background processing.

## Analysis service

Remote analysis is optional. For app-owned model credentials, use a minimal authenticated proxy rather than embedding a reusable provider secret in the app. No Cashflow login is proposed for the local ledger; the proxy still needs per-install credentials, rate limits, request validation, abuse controls, and cost caps. Confirm how those credentials are provisioned before a remote pilot.

The proxy accepts bounded redacted context and a client idempotency key and returns a versioned interpretation. It does not own the ledger, receive database backups, or store raw financial payloads in routine logs. Document actual provider and infrastructure retention before asking for consent.

Use an adapter for the decision provider. Separate model output validation from transaction application. Version parsers, prompts, schemas, and models so a regression can be traced and remote auto-recording disabled without disabling manual entry.

Any JavaScript/TypeScript service or tooling uses Bun/Bunx per the workspace instructions. Android and iOS builds use their platform toolchains.

## Android capture

Use NotificationListenerService with explicit user-enabled notification access. Separate listener access from permission to post Cashflow notifications. Filter source packages immediately, exclude Cashflow itself, account for posted/updated/grouped notifications, and reconnect without replaying captured events as new payments.

Do not depend on reading old notification history after installation. Do not request accessibility or SMS access as substitutes for the listener. Verify package visibility and modern background work requirements on supported Android releases.

## iOS capture

Expose "Capture transaction notification" as a Swift App Intent with only the verified input fields and background execution mode supported by the target OS. Validate size, content, optional source identity, and timestamps. A missing source identity is not proof that the input came from a bank.

Treat Shortcut invocations as user-provided input, not authenticated bank records. Durable deduplication must tolerate missing external IDs. Preserve platform source labels separately from user-entered labels.

The guide provides setup assistance; only actual incoming events provide capture evidence. Platform minimums, notification field availability, automation installability, locked execution, cold launch, and deferred processing are implementation gates.

## Privacy and security

- Filter non-selected sources before persistence and remote transmission.
- Discard authentication notifications such as OTPs; never retain them as useful transaction context.
- Keep full account numbers and unrelated message content out of payloads. Retain masked identifiers only where needed.
- Protect secrets with Keychain/Keystore and verify database/file protection for the chosen storage stack. Do not claim encryption beyond verified guarantees.
- Offer biometric/app lock while checking how protected access affects locked-device capture. Hide financial details in notification previews by default.
- Proposed raw event retention is 30 days, configurable shorter, with immediate per-event deletion. Structured records and minimal evidence metadata survive until the user deletes them. Explain that expired source text cannot be reconstructed for an explanation view.
- Exclude raw text, amounts, recipients, and account identifiers from analytics and crash logs. Optional analytics needs a separate choice.
- Define OS backup exclusions and protection for raw events, credentials, and the ledger. Never silently rely on untested platform backup behaviour.
- Provide versioned backup export/restore and separate CSV export. Warn before exporting sensitive data; state whether the selected backup format is encrypted. Verify round-trip integrity before release.
- Reset clears the local ledger, raw inputs, queued work, credentials, and settings as selected. Explain that an iOS automation may still exist and guide users to remove it. Old automation calls must not silently re-enable capture after reset.

## Observability

Keep a content-free processing history with event/job IDs, processing state, parser/model version, duration, and reason codes. A user can inspect last received and last processed times without exposing notification text to support.

Record operational failures separately from financial status. Include provider unavailable, permission revoked, storage inaccessible, unsupported input, queue retry, and user review required. Never label a bank transfer failed because a network request timed out.

## Design approach

The user's design direction is fun, minimalist, and intentional, with meaningful icons or concise icon-plus-text controls in preference to prose. Keep amounts and status readable. Include text where needed for permissions, unfamiliar actions, setup, errors, and financial consequences. Icon-only actions require accessible names.

GLM-5.3 Flash Free owns the UI design draft in [UI design](ui-design.md). Keep primary ledger operations consistent across platforms and use native system screens for permissions and platform setup. Visual proposals remain drafts until reviewed; this planning task does not implement screens.
