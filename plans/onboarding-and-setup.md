# Onboarding and capture setup

The first run should explain the value, teach correction, and help the user configure capture. Permission requests follow the explanation they depend on. Every optional step has "Set up later" and can be resumed from Settings.

## Shared introduction

### 1. Welcome

Headline: "Keep track as money moves."

Copy: "Cashflow turns payment alerts into a timeline you can check and edit."

Actions: Get started; Explore with an example. Show one sample payment and its resulting timeline row. Mark all sample content as Example.

### 2. Explain automatic recording

Headline: "Choose what Cashflow watches."

Explain that the user chooses sources, Cashflow interprets transaction details, and uncertain entries need review. Say that missing or hidden alerts can leave gaps. Do not imply access to the user's complete banking history.

### 3. Teach acceptance and correction

Headline: "Recorded automatically. Always editable."

Show an in-app example of the confirmation notification. Explain that doing nothing keeps a clearly identified transaction, Undo removes the Cashflow record, and Edit corrects it. Let the user try one action without creating a real ledger entry.

Explicit helper text: "Undo changes Cashflow only. It does not cancel a bank payment."

### 4. Basic preferences

Suggest a currency from locale and require the user to confirm it. Locale does not prove account currency. Offer an optional first account name and currency. Other currencies and accounts can be added later.

### 5. Processing and privacy

Offer local parsing as the default. Explain that known formats can be handled locally and ambiguous text may require manual review.

Optional remote analysis copy: "Send selected transaction text and relevant recent activity to our analysis provider to help recognise payments and duplicates." Name the actual provider before release. Show which data leaves the device, retention, and how to disable it. Declining must leave manual entry and local parsing usable.

Consent is versioned and separate from notification access, analytics, and account creation. Do not upload historical events merely because the user enables remote processing; offer an explicit choice to analyse pending items.

## Android branch

1. Show a source picker. Include search, app name, and icon, with nothing selected by default. The app may suggest supported financial sources without claiming those are the only apps installed. Verify Android package visibility requirements before implementation.
2. Explain notification access before opening system settings. Say that Android grants broad notification listener access; Cashflow filters to the user's selection before persisting or uploading content. Do not describe Android's grant as an OS-enforced per-app permission.
3. Open the appropriate notification access settings. On return, check the actual grant. Returning from settings is not proof of permission.
4. Ask separately whether Cashflow may send confirmation and review notifications. Explain that capture access and posting notifications are different permissions.
5. Run a safe sample through the local ingestion path. Label it Sample test and keep it outside the ledger. Then offer "Wait for a payment alert" to verify the selected source without asking the user to spend money.
6. Show setup results: sources selected, access granted or missing, sample path tested, and real source event received or not yet observed.

If access is denied, show manual entry and a Settings shortcut. If access is revoked later, show capture as requiring attention. A quiet period alone must not be labelled a failure. Capture disabled for one app leaves existing records intact.

## iOS branch

### Compatibility check

The notification trigger is a platform capability gate. The exact minimum supported OS and available input fields require the device probe in the delivery plan. Apple documents the trigger, but currently retrieved sources disagree about version naming. Do not hardcode a version claim in onboarding until verified.

For unsupported devices, explain the limitation and offer pasted text, manual entry, and a guide to Wallet or message automations only where those alternatives have been verified. Avoid an endless setup loop.

### Setup landing screen

Headline: "Connect payment alerts with Shortcuts."

Copy: "You choose which apps trigger an automation. Shortcuts passes their selected notification details to Cashflow."

Show the steps as a short checklist. Actions: Open guide; Set up later. Explain that configuration happens in Apple's Shortcuts app and Cashflow cannot silently create or inspect every personal automation.

### Guided setup

These are intended steps, not validated OS-specific button labels. Produce actual screenshots and exact labels from supported release devices before shipping.

1. **Find the Cashflow action.** Explain the action named "Capture transaction notification" and show the inputs it expects. A standalone sample shortcut may demonstrate it, but importing a shortcut must not be described as automatically installing the personal automation.
2. **Create an automation.** Open Shortcuts where supported, or provide clear manual navigation. Choose the notification trigger.
3. **Choose source apps.** Select bank or wallet apps. Explain that this selection must be made in Shortcuts. A checklist in Cashflow is guidance, not proof of the automation's configuration.
4. **Choose filters.** Initially recommend broad capture within selected financial apps so wording variants are not missed. Offer optional title, subtitle, or body filters only when their fields are verified. Explain that overly narrow filters can miss payments and reversals.
5. **Add the Cashflow action.** Insert "Capture transaction notification". Map the notification title, subtitle, body, and source identity where the OS exposes them. Do not promise identifiers, timestamps, or metadata the trigger does not supply. Tag unavailable fields as unknown at ingestion.
6. **Choose automatic execution.** Guide the user through the supported run and locked-device settings. Include permission prompts and consequences. Exact labels and whether all actions can run without interaction are probe results.
7. **Save and return.** Preserve guide progress when switching apps. Provide a persistent "Continue setup" entry when the user returns.
8. **Test the connection.** Run a labelled sample into the Cashflow action, then wait for a notification from an actual selected source to validate the automation. Do not send a fake bank payment or ask the user to make a financial transaction.

Each guide step includes a screenshot for that OS version, a text equivalent for accessibility, one action, and "Something looks different" help. Keep a compact checklist available for people who already know Shortcuts. Include a replayable short demonstration later if it improves setup completion.

### Verification states

Track these separately:

- Guide not started.
- Guide in progress, with last completed step.
- User reports automation configured.
- Sample action invocation received.
- Real source input observed, with last received time.
- Input received but transaction processing needs attention.
- Setup unsupported or unavailable on this device.

Cashflow must not claim continuous monitoring health merely because a sample action ran. A received event verifies that path at that time; it does not prove all selected apps or locked-device cases work.

The completion screen can say "Cashflow received your test" or "First payment alert received" based on evidence. Before a real event, say "Setup saved. Waiting for a payment alert."

### Troubleshooting

- No Notification trigger: verify supported OS and show alternatives.
- Cashflow action missing: verify installed app version and native intent registration; provide a sample action discovery check.
- Automation never runs: check selected app, filters, execution settings, and source notifications.
- Runs only when unlocked: inspect execution settings and protected storage access; preserve an actionable error without leaking content.
- Missing or hidden text: explain that Cashflow cannot reconstruct redacted details; route to manual review.
- Connection works but model fails: show queued or review status, with local editing still available.
- Duplicate captures: inspect retries and overlapping automations. Never ask users to repair duplicates manually as the normal fix.

Do not claim the app can reliably detect deletion or disabling of a user's Shortcuts automation unless a public capability is verified. Offer "I changed my automation" and a fresh connection test.

## Finish and later education

Ask for confirmation notification preference and privacy level. Explain the Review inbox, show Add transaction, and enter Home. Onboarding is complete even when capture setup was skipped; capture readiness remains separate.

Use contextual education later: explain own-account transfers on the first likely match, fees when first detected, and Review on the first uncertain event. Avoid repeating the full onboarding after an update. Reopen only the affected guide when a platform change invalidates setup.

## Acceptance criteria

- Onboarding is resumable after app termination and trips to system settings or Shortcuts.
- Every step has clear back navigation, accessible text, and a non-blocking fallback.
- Refused permissions are not requested repeatedly on every launch.
- An example cannot enter real totals or be mistaken for a real bank event.
- An iOS sample invocation, actual source event, and user-reported setup remain distinct states.
- Setup screenshots and instructions match each supported release OS, verified on a device.
- Users can find and repeat setup from Settings without resetting their ledger.
