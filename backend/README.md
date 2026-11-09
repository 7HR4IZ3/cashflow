# Cashflow Convex backend

This is the backend for Cashflow's native Android subscription and Jev features. It has no web frontend, separate database, or Cashflow login account. Possession of a Google Play purchase token is the credential; Google verifies it for every entitlement or AI request. Do not log or share purchase tokens.

## Deployment

Project: https://dashboard.convex.dev/t/gamerxville-gmail-com/cashflow

Current development deployment:
- Client API: https://hidden-ostrich-923.convex.cloud
- Native HTTP API: https://hidden-ostrich-923.convex.site

Run from this directory:

```sh
bun install --frozen-lockfile
bun run typecheck
bun run lint
bun test tests
bunx --bun convex dev --once
```

The linked deployment is in ignored `.env.local`. Do not commit it, credential files, or environment values. To deploy production after configuring its separate environment settings, use `bunx --bun convex deploy`. Dev environment variables do not automatically carry to production.

## Environment settings

Set these in the selected Convex deployment's Environment Variables settings, or through the Convex CLI with private input:

- `TYPESAFE_API_KEY`: TypeSafe API key, used only in the backend.
- `GOOGLE_SERVICE_ACCOUNT_JSON`: complete JSON credential for a Google Cloud service account. Enable the Google Play Android Developer API and grant the account access to `app.cashflow.mobile` in Play Console, including subscription/order viewing and management needed to verify and acknowledge purchases. Give it access only to the required app.
- `JEV_MODEL`: optional; defaults to pinned `jev-1.13.0`. Verify that model is available to the account before enabling AI.
- `ENABLE_PURCHASES`: defaults off. Set to `true` only when Jev credentials, service-account access and Play products are ready for licensed purchase tests. This gates checkout readiness in the app.

No paid entitlement is granted by a local debug switch. No provider keys are shipped in Android BuildConfig.

## Play products

Create auto-renewing subscriptions for package `app.cashflow.mobile`:

| Product ID | Base plans | Proposed USD prices |
|---|---|---|
| `cashflow_lite` | `monthly`, `yearly` | $1.49/month, $11.99/year |
| `cashflow_premium` | `monthly`, `yearly` | $4.99/month, $39.99/year |

Configure regional prices in Play rather than hardcoding currency conversions. The client uses base-plan offers, without promotional trials. Add license testers and test purchases through a Play-installed build. A sideloaded APK alone is not proof that billing works.

Test purchase, pending payment, cancellation, restore after reinstall, upgrade, deferred downgrade, grace period, hold, expiry and refund. Verify each through Google's purchase record and the resulting entitlement. Receipts are acknowledged on the server; a verification outage leaves the transaction available for Restore purchases to retry.

## Build the Android client

From the Cashflow app directory, with JDK 21:

```sh
./gradlew :shared:jvmTest :androidApp:assembleDebug \
  -PcashflowBackendUrl=https://hidden-ostrich-923.convex.site
```

Use the production `.convex.site` URL for a production build. Without this property the app leaves checkout disconnected. Do not use the `.convex.cloud` URL for these custom HTTP routes.

## HTTP contract

- `GET /health`: readiness booleans, no credentials or financial data.
- `POST /v1/subscription`: `Authorization: Bearer <Google purchase token>`; body `{}`. Returns verified `tier`, bounded `expiresAt` in epoch milliseconds, UTC `month`, and `used`.
- `POST /v1/analyse`: same authorization; body with `requestId`, `text`, `source`, `consent: true`. Returns a nullable typed `suggestion` and refreshed subscription usage.

Alert text is capped at 2,000 characters and request bodies at 16 KiB. HTTP requests and responses are not logged by application code. Raw alerts are not sent as Convex function arguments or written to its tables. They pass through the HTTP action to TypeSafe only. The native ledger is never uploaded.

Convex stores token hashes and stable purchase-family identities, monthly counters, alert fingerprints and request IDs for idempotency, and typed suggestions. Completed request metadata is eligible for deletion after 40 days, via a bounded cron sweep. Purchase identity and usage records remain for entitlement and abuse prevention. The TypeSafe provider's separate retention terms must be reviewed and reflected in the published policy before activation.

Monthly quota reservation is transactional. Repeated requests reuse a cached result; concurrent repeats do not make another model call. Changed content under an existing request ID is rejected. Failed requests refund their reservation once. Crashed pending requests are refunded by cleanup after ten minutes. Success replay is free, including across a month boundary while metadata is retained. The rate-limiter component adds global and token-specific burst limits.

Google is rechecked on every request. Replaced purchase tokens are revoked locally and linked to the same allowance identity. Cached device entitlements are bounded to 24 hours and the subscription's actual expiry, so offline watched-app limits can lag refunds by at most that lease. This version has no RTDN webhook.

## Release requirements

The public privacy policy currently describes the shipped offline 0.1.0 build. Publish the updated AI/subscription policy, explain automatic sending, confirm provider retention terms, and update Play Data safety/listing before uploading 0.2.0. No activation or successful live Jev/payment test is implied by a green build or deployment.

Official references:
- https://docs.typesafe.ai/api/system-one
- https://developer.android.com/google/play/billing/integrate
- https://developer.android.com/google/play/billing/security
- https://developers.google.com/android-publisher/getting_started
- https://docs.convex.dev/functions/http-actions
