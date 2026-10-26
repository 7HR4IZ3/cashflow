# Platform evidence and verification limits

Reviewed for this planning draft on 2026-10-25. Documentation supports feasibility; it does not establish successful Cashflow integration. No device capture tests have been performed for this plan.

## Android

[NotificationListenerService reference](https://developer.android.com/reference/android/service/notification/NotificationListenerService) documents notification callbacks for a user-enabled listener. This supports the proposed native Android source adapter.

[Android sensitive notification changes](https://developer.android.com/about/versions/15/behavior-changes-all#otp-redaction) describe redaction of OTP-related content. Do not promise every financial notification will expose complete text.

Still to verify: selected financial app formats, package discovery, callback replay/update semantics, grouped notifications, device-specific lifecycle behaviour, and complete text availability on supported releases.

## iOS Shortcuts

[Apple WWDC26: What's new in Shortcuts](https://developer.apple.com/videos/play/wwdc2026/310/) describes a notification automation triggered by a selected app and content filters. It supports investigating the user-configured Shortcuts bridge.

The retrieved page's summary refers to iOS 26 despite appearing under WWDC26, while other retrieved descriptions refer to iOS 27. This conflict is unresolved in this draft. Do not use either as a verified minimum OS claim. Resolve through release documentation and physical-device testing.

[Apple Shortcuts automation guide](https://support.apple.com/guide/shortcuts/add-automations-apdfbdbd7123/ios) covers user-managed automation configuration. Its version-specific options and execution settings must be checked on supported devices.

[App Intent execution modes](https://developer.apple.com/documentation/appintents/appintent/supportedmodes) document actions that support background execution. This does not guarantee unlimited runtime or success while protected storage is unavailable.

[Notification service extensions](https://developer.apple.com/documentation/usernotifications/modifying-content-in-newly-delivered-notifications) process an app's own remote notifications. They are not the mechanism for reading other banks' notifications; the proposed bridge is the user's Shortcuts automation.

Still to verify: exact trigger minimum version, input fields and source metadata, lock-screen redaction, automatic execution conditions, cold starts, durable queue processing, action discovery, and shareable shortcut installation limits.

## Shared stack

[Kotlin Multiplatform FAQ](https://kotlinlang.org/docs/multiplatform/faq.html) documents stable Compose targets for Android and iOS.

[Kotlin platform API integration](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html) describes shared interfaces with platform-specific implementations. Context7 was consulted using `/jetbrains/kotlin-multiplatform-dev-docs` for this boundary. Native Android capture and Swift App Intents remain platform responsibilities.

No dependency versions, compile targets, storage compatibility, or native intent interoperability have been validated in this folder.

## Decision model

[Jev on Venice](https://venice.ai/lp/jev) documents bounded decision outputs and identifies the API as beta. Keep provider schemas behind an adapter, evaluate using real task examples, and validate current availability and privacy terms before integration.

No inference call, accuracy evaluation, latency benchmark, cost calculation, or provider retention audit was performed for this draft.
