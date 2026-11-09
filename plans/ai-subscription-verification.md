# AI and subscriptions verification

30 October 2026. This is implementation evidence, not a live billing or Jev success claim.

## Implemented

- Free/Lite/Premium: 2/5/unlimited watched apps; 0/300/3,000 AI-assisted alerts per UTC calendar month.
- Unlimited local transactions, manual records, Review, Recovery and CSV export.
- Plan comparison with Google-provided localized prices, restore, management, purchase and replacement flows.
- Server verification and acknowledgement through Google's subscriptions API; bounded offline entitlement leases.
- Explicit AI consent and a second automatic-help switch, both default off.
- Ask Jev for individual Review items; optional WorkManager queue for new unclear alerts only, with persisted IDs, network constraints and bounded retries.
- Typed relevance/direction/kind suggestions, with abstention for uncertain results. Suggestions do not change amounts, resolve Review or write to the ledger.
- Opt-out, resolved-review and stale-response protection.
- Convex atomic quota reservations, idempotency, replacement-token linking, burst limits and bounded metadata cleanup.

## Checks

- Shared JVM tests: 18 passing, zero failures or errors, including five new subscription/consent/automatic-help cases.
- Backend Bun tests: 11 passing, covering entitlement states, typed model validation, quota exhaustion, retry refunds, replay and token replacement.
- Convex TypeScript and ESLint checks pass.
- Convex development deployment accepts schema and functions, including the rate-limiter component.
- Live development `/health` returns `ok: true`, `billingReady: false`, `aiReady: false`.
- Live unauthenticated `/v1/analyse` returns HTTP 401 and `Purchase required`. A request without consent returns HTTP 400 and `AI consent required` before any provider request.
- Android debug build succeeds with the development Convex URL. Shared iOS simulator compilation also succeeds with the automatic-help additions.

## Runtime and external gaps

- APK installed on the Android emulator. Device controller could not relaunch it: `Android app app.cashflow.mobile process remained running after close`. Retrying with the suggested `--debug` produced the same error. Device UI verification stopped; no claim is made that the plan or AI screens were exercised.
- TypeSafe API key and Google service-account private entry prompts both timed out without credentials.
- No live Jev inference, receipt verification, purchase, renewal, cancellation or restore was tested.
- Play products/base plans/prices are specified in the backend README, but have not been created or activated in Console.
- Checkout stays gated off by `ENABLE_PURCHASES`, missing credentials and backend readiness. App source is version 0.2.0/code 2; Play's 0.1.0/code 1 internal release remains unchanged.
- Published policy/listing/Data safety must be updated before the AI build is distributed. The existing public policy still describes the shipped offline build.
- Changes are uncommitted.
