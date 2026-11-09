# Cashflow plans and pricing

Updated 30 October 2026. Supersedes the earlier Free/Pro proposal. Cashflow stays free to install, with no ads.

| Feature | Free | Lite | Premium |
|---|---|---|---|
| Watched apps | 2 | 5 | Unlimited |
| Manual records and local parsing | Unlimited | Unlimited | Unlimited |
| AI-assisted alerts per UTC calendar month | 0 | 300 | 3,000 |
| Review, Recovery and CSV export | Included | Included | Included |
| Proposed monthly USD price | Free | $1.49 | $4.99 |
| Proposed yearly USD price | Free | $11.99 | $39.99 |

Premium is for heavy capture usage. The AI allowance limits cloud requests, not transactions recorded per day. A clear alert handled locally consumes no AI allowance. No unused AI allowance carries over. Prices are recommendations, not activated Play products; the app always displays localized prices returned by Google Play.

## Cost target

The user wants one Lite subscription to cover the expected usage of two to five additional Lite users. TypeSafe currently advertises $42 per billion input tokens, equivalent to $0.042 per million. At an estimated 2,000 tokens per AI-assisted alert, the maximum Lite allowance costs about $0.0252 per month in model input charges. Six Lite users total about $0.1512. A $1.49 monthly subscription leaves about $1.2665 after a 15% Play fee, before taxes, infrastructure, support, refunds or other costs. An $11.99 annual subscription averages about $0.8493 per month after that fee. These estimates cover the model-cost target with room to spare, but are not measured total margins.

At the same token estimate, Premium's full allowance costs about $0.252 per month in model input charges. Measure actual token usage and Convex costs before raising allowances or reducing regional prices.

Sources checked 30 October 2026:
- https://typesafe.ai/
- https://support.google.com/googleplay/android-developer/answer/112622

## AI behavior

- Both AI consent and automatic help default off.
- Ask Jev sends one selected Review alert and its source.
- Optional automatic help queues only new unclear payment alerts after both switches are enabled. It does not upload an existing Review backlog.
- Amount, currency, date and counterparty remain locally parsed. Jev suggests relevance, direction and transaction kind from fixed choices. Low-confidence answers abstain.
- Suggestions remain in Review. Choosing a suggested direction or kind is explicit; recording is another explicit action. No cloud decision silently creates, changes, deletes or ignores a transaction.
- Authentication alerts are rejected locally and again by the backend.
- Disabling consent stops future sends. An in-flight provider request cannot be retracted; its suggestion is discarded after opt-out. Turning off automatic help also prevents queued automatic requests from being sent.
- Android WorkManager persists only a Review ID and automatic/manual flag, with network constraints and bounded retries. No raw alert is copied into WorkManager.

## Subscriptions

- Product IDs: `cashflow_lite`, `cashflow_premium`.
- Each product has `monthly` and `yearly` auto-renewing base plans.
- Prices come from Google Play; checkout, cancellation, renewal, restore and plan replacement use Play Billing.
- Convex verifies purchases with Google's subscriptionsv2 API and acknowledges valid initial purchases. Clients cannot choose their entitlement or allowance.
- Pending, held, paused, expired and unrecognized products grant no paid entitlement. Cancelled subscriptions remain paid only until their verified expiry. Grace-period subscriptions retain access.
- The device caches a verified entitlement for at most 24 hours and never past subscription expiry. Every AI request verifies the purchase again. No RTDN webhook is required for the current request-time checks; offline watched-app entitlements may remain until the bounded lease expires after a refund.
- Downgrades preserve all records and app selections. Capture pauses if the selected count exceeds the new tier; the user chooses which apps to keep. Manual entry, Review, export and Recovery keep working.

## Release state

Version 0.1.0, code 1, remains the published internal-test release. Its offline-only privacy declarations still apply to that build. The new implementation is version 0.2.0, code 2, and has not been uploaded to Play.

The Convex development backend is deployed. Jev credentials, Google service-account access, Play subscription products, localized pricing and licensed purchase testing still need setup. Checkout is disabled until the backend advertises readiness. Update the published privacy policy, listing and Data safety before distributing the AI-enabled build. See [backend setup](../backend/README.md).
