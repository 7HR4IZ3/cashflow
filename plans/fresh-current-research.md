# Fresh current: reference comparison

Research date: 2026-10-26. The user selected Fresh current after rejecting the first draft's appearance. This comparison precedes native UI changes. Fresh current replaces the old paper-ledger visual direction; transaction correctness and capture limitations still apply.

## What was inspected

Official product pages, help documentation, and publisher imagery for Copilot Money, Things, Cash App, and Buddy. Copilot's desktop dashboard image and Buddy's phone dashboard image were inspected directly. These are published examples, not installed-app usability tests. Things' feature page is a historical launch page, useful for interaction principles rather than evidence of its latest OS appearance.

The collaborative browser returned `Preview automation snapshot timed out after 15000ms.` followed by `No preview automation host is available for evaluate`. Research continued through retrieved official pages and publisher assets. Cash App's brand-guideline index loaded; its typography, motion, and graphic-UI subpages timed out. No exact typeface, motion curve, or design token is attributed to those inaccessible pages.

## Comparison

| Reference | Observed pattern | Cashflow adaptation | Boundary |
| --- | --- | --- | --- |
| Copilot Money | Searchable/filterable transactions, editable transaction details, manual add, and a dashboard review section. Its published dashboard shows compact date-grouped rows with aligned amounts. | A single activity destination, compact identifiable rows, and an explicit route into uncertain entries. | Its review queue covers newly imported transactions. Cashflow queues uncertain interpretations; a clearly directed but economically unclassified entry can already be accepted. |
| Things | Optional item details stay tucked away until needed; date/list headings organize content; the plus action is consistent; motion connects editing with its source item. | Amount-first Add Entry, optional details disclosed on demand, clear date groups, and spatial continuity on opening a row. | Do not copy drag-to-insert gestures or task-completion semantics into financial acceptance. Explicit controls remain available. |
| Cash App | Strong brand expression, personalized payment presentation, and Activity routes into individual payment state. Its brand guidelines distinguish typography, motion, icons, and graphic UI as coordinated systems. | A confident amount display, one clear add action, a distinctive current mark, and a coherent icon family. | Cashflow records money movement. Labels must not imply sending money, cancelling a bank payment, holding a balance, or issuing a card. No Cash App proprietary assets are reused. |
| Buddy | Published phone imagery uses a prominent monthly figure, small account illustrations, and a friendly colored header. Product material describes customizable categories/themes. | Friendly small illustrations in onboarding/empty states and restrained mint accents around intentional actions. | Avoid importing budgets, account balances, savings goals, oversized colored headers, or category semantics the ledger does not currently implement. |

## Decisions for Cashflow

1. Home and Activity become one continuous destination. A compact period/currency summary sits above complete searchable history. Scrolling into older history must remain possible; the new Home cannot be only a latest-three preview.
2. The hierarchy is amount/name first, date/account second, evidence on demand. Completed/manual/captured icons no longer repeat on every normal row. Pending, failed, transfer/refund/fee, and uncertain direction still receive explicit words where needed.
3. Review remains a named, reachable destination. Its count means unresolved uncertainty, not all recently captured entries. A Home count shortcut disappears when there is nothing to check; the Review destination remains reachable.
4. Add Entry begins with amount, currency, and In/Out. Required classification compatibility, status, date, account, counterparty, and notes remain available. Optional-field disclosure changes presentation, not the persisted data model.
5. The primary action says Add entry or Save entry. Undo and Delete name ledger-only effects. A clear automatic record never waits for user confirmation merely because this redesign resembles a task inbox.
6. Surface color is soft white with near-black content and vivid mint action accents. Small abstract current strokes provide personality. Keep money stable during motion. A current mark is decorative, not a chart or evidence of active capture.
7. A period selector and source/direction/status filters are proposed UI work. They must actually filter both their documented scope and any displayed matching summary; the prototype must not disguise inert controls as implemented features.
8. Totals stay currency-specific and scope-specific. Label them Recorded net and In/Out, not Balance or Free to spend. Completed accepted directions count; pending/failed and own-transfer principal do not. Ambiguous Review items do not affect totals.

## Why the current screen feels heavy

The previous Home presents Recorded activity, All time, Net recorded, and Net sent before the first amount. Separate borders around every row give metadata the same visual importance as money. Multiple status/origin/kind cues repeat within each entry. Home and Activity duplicate browsing. The form displays advanced fields before the user has entered an amount. These are hierarchy and interaction problems, not problems a palette swap alone will solve.

## Proposed acceptance checks

- At a 360px logical width, the main amount, every transaction amount, currency, direction, and actions remain readable without horizontal page scrolling.
- Review can be opened from its destination even when its count is zero. Unknown direction or missing currency cannot be silently accepted.
- Full history, search, paste capture, source selection, capture settings, CSV, manual CRUD, and Recently deleted have explicit routes in the native mapping.
- Default transaction rows do not show evidence/reference blocks or decorative completion badges.
- Tap targets are at least 44px, preferably 48dp in Compose. Text scaling can grow rows and controls instead of clipping data.
- Reduced motion removes decorative transitions; amounts do not animate numerically in either mode.
- Dark mode uses separately checked contrast tokens. Mint actions use dark foreground text in the light design.
- A screenshot is not interaction evidence. Prototype controls must be exercised before calling the comparison complete; native flows need later platform builds and device checks.

## Comparison prototype verification

The GLM-authored prototype was rendered at 360px and 728px. The narrow document stays within 360px; the navigation-map table has its own horizontal scroll region. Home, Review, and Add were visually inspected. Scripted browser checks confirmed the seeded net of -875,050 minor units, edit-in-place without an extra record, own-transfer exclusion, USD Review resolution into a separate USD entry, unchanged NGN totals, precision/unsafe-integer rejection, and reset of the example state. These checks apply to the HTML sandbox, not to the native app. No Compose source changed in this comparison pass.

The refinement pass also checked the preview inside light and dark host themes. Direct button interactions confirmed that choosing In/Out in Review does not record automatically, explicit Record creates the entry, a missing-amount item can be completed, and invalid saves keep the completion sheet open. Opening a detail sheet, editing, saving, and resetting no longer shifts the phone's internal scroll position. Pending entries remain excluded, and the manual reduced-motion control was checked. The native UI is still unchanged.

## Sources

- [Copilot Transactions overview](https://help.copilot.money/en/articles/9554412-transactions-tab-overview)
- [Copilot Dashboard overview](https://help.copilot.money/en/articles/6045480-dashboard-tab-overview)
- [Copilot product imagery](https://www.copilot.money/)
- [Things feature and interaction explanations](https://culturedcode.com/things/features/)
- [Cash App product](https://cash.app/)
- [Cash App design guidelines](https://design.cash.app/)
- [Cash App pending-payment Activity flow](https://cash.app/learn/getting-started/how-to-send-and-receive-money)
- [Buddy product and publisher imagery](https://buddy.download/)

The comparison and proposed adaptations are our design judgments. References establish patterns to study; they do not establish that the same pattern will work for Cashflow without testing.
