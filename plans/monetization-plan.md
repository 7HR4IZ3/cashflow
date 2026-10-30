# Cashflow monetization plan

Decided 2026-10-29. The app is **free on Google Play** (a free app cannot become paid later; paid can become free). **No ads.** Revenue comes from a paid subscription that unlocks AI help.

## Model

Freemium with a subscription.

| | Free | Pro (subscription) |
|---|---|---|
| Manual entry, Review, Recovery, CSV export | yes | yes |
| Rule-based capture from watched apps | up to 2 apps | unlimited apps |
| AI help resolving unsure alerts | small monthly allowance (about 10), or none | unlimited |
| Auto-categorising, later insights | no | yes |

Rules:
- Limit by value, not cost. Model cost is negligible (below).
- Never gate access to the user's own records: export and Recovery stay free.
- Launch price to test: about $2.99 per month or $19.99 per year, with regional pricing (Nigerian users). Play keeps 15% of subscription revenue.

## Decision model: Jev (TypeSafe)

Jev is a decision-only model. It returns typed answers (choice, score, probability), not generated text. It fits classifying notifications.

Pricing, as reported in October 2026 (verify on TypeSafe's pricing page before relying on it; the model was in early access and TypeSafe has not shown the pricing is sustainable):
- Input about $0.042 per 1M tokens; output free. AI/ML API charges about $0.058 per 1M.
- Waitlist reported dropped 2026-09-20; new accounts get $5 credit.
- Accuracy is modest: an independent evaluation reports median macro-F1 0.581, below larger open models near 0.60.

Expected cost (estimate, not measured):
- About 300 input tokens per notification at 100 notifications per month is about $0.001 per user per month.
- 10x that (extra context per call) is about $0.01 per user per month.

Design consequence: keep amount and currency parsing deterministic in `AlertParser`. Call the model only for what the parser cannot settle (direction, kind, counterparty, relevance). That cuts calls and limits the damage of a wrong answer. Anything the model is unsure about still goes to Review.

## Privacy and policy consequences

- The listing and README say alert text stays on the device. Sending text to Jev breaks that. Make AI **opt-in**, say so plainly in the listing and in-app, and keep capture fully working without it.
- Data safety: when AI ships, declare financial info as shared with a third-party processor, with the retention terms of the chosen provider. Update the privacy policy.
- No ad SDKs, so no advertising ID. The Play Ads declaration stays "No".
- The app currently has no internet permission. AI and Play Billing need `INTERNET` and the billing library.
- Notification access is a sensitive permission; Play reviews it closely. Never send notification content anywhere for any purpose other than the user's own AI help.

## To build

1. Entitlement state in the shared module (`isPro`), free-tier limits enforced in `LedgerStore` or the capture selection (2 watched apps).
2. Paywall and upgrade prompt when adding a third app.
3. Play Billing integration (subscription product, restore purchases).
4. AI client behind an interface, off by default, with a per-month free allowance.
5. Settings: AI opt-in switch and a plain description of what is sent.
6. Update listing text, privacy policy and Data safety before the AI release.

## Play Console state

- Store listing, Finance category, Ads = No: done.
- Pending: privacy policy URL, sign-in details, content rating, target audience, Data safety, government apps, financial features, health, store contact email.
- First release ships without AI and without billing; Data safety for it is "no data collected".
- Production access needs a closed test with at least 12 testers for 14 days.
