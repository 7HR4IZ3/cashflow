# UI design plan

Draft date: 2026-10-25. Second revision, after review of the first draft. Written plan only. No app code, no screens, no image assets exist yet.

Scope of this document: visual direction, typography, palette, surfaces, icon language, navigation, every core screen, onboarding and platform setup guides, confirmation behaviour as seen in the interface, correction and deletion flows, state design, motion, dark mode, reusable components, and testable design acceptance criteria.

Inputs:

- user brief: fun, minimalist, intentional, minimal text. Prefer icons, or icons plus short text, for actions and cues.
- [Product plan](product-plan.md): scope, screens, confirmation wording, financial correctness, accessibility.
- [Onboarding and setup](onboarding-and-setup.md): introduction steps, Android branch, iOS Shortcuts guide, verification states.
- [Transaction engine](transaction-engine.md): evidence versus record, decision dimensions, review reasons.
- [Technical architecture](technical-architecture.md): proposed Kotlin Multiplatform and Compose Multiplatform, restrained readable interface.
- [Platform evidence](platform-evidence.md): unresolved iOS minimum OS and trigger field facts.

Constraint carried through the whole document: the iOS minimum OS version and the exact notification automation inputs are unresolved. The iOS guide here is a version-adapted draft. It contains no invented screenshots, no invented Apple button labels, and no version claims. Every verification-dependent item is marked [VERIFY]. Real screenshots and exact labels come from supported release devices per the delivery plan.

## User-confirmed requirements versus proposed baseline

The user's brief is the only user-confirmed source in this planning set. The product, engine, and onboarding plans are themselves proposals from earlier drafting; their functional defaults, four destinations, confirmation wording, currency rules, and the rest, are treated here as a proposed baseline for review, not as explicit user approval. Where sections below say confirmed about anything other than the brief, it always means plan baseline.

| # | Item | Status |
|---|------|--------|
| 1 | Four destinations: Home, Activity, Review, Settings. Add transaction reachable from Home and Activity. Accounts and capture setup live in Settings. | Proposed baseline (product plan) |
| 2 | Confirmation wording "Recorded {amount} sent to {counterparty}" with Undo and Edit. Ambiguous wording "Payment needs a check" with Review and Ignore. Nothing enters totals until committed. | Proposed baseline (product plan) |
| 3 | Doing nothing keeps the entry. Dismissing counts as doing nothing. Undo changes Cashflow only, never the bank. | Proposed baseline (product plan) |
| 4 | Recent deleted view with restore, proposed 30 day window. Deleting is recoverable. | Proposed baseline (product plan, window proposed there) |
| 5 | Amounts and names hidden on the lock screen by default. | Proposed baseline (product plan) |
| 6 | Totals labelled "Recorded activity". No implied bank balance. Transfers excluded from income and spending. Unclassified movements stay visible. | Proposed baseline (product plan) |
| 7 | Currencies never summed together. Pending and failed excluded from completed totals. Every currency visible on Home. | Proposed baseline (product plan) |
| 8 | Every optional setup step has "Set up later" and is resumable. Refused permissions are not re-asked every launch. | Proposed baseline (onboarding doc) |
| 9 | Support screen readers, scalable text, comfortable targets, reduced motion, direction labels beyond colour. | Proposed baseline (product plan) |
| 10 | A failed save must never display "Recorded". | Proposed baseline (product plan) |
| 11 | Fun, minimalist, intentional, minimal text, icon preferred cues. | User-confirmed (brief) |
| 12 | Interface tone: "a well kept receipt book" aesthetic, cream paper surfaces, ink text, one green accent. | Proposed |
| 13 | Signature element: the connector line, a slightly irregular hand-drawn stroke at constant width, purely decorative, never scaled by any data value. | Proposed |
| 14 | Typography: three bundled faces, voice for headings and empty states, ledger for every piece of money on screen including the hero numeral, mono for references only. Exact fonts below, metrics to verify when bundling. | Proposed |
| 15 | Palette: cream paper light mode, warm carbon dark mode. Green for money in and brand, ink for money out (not red), amber for pending, pencil violet for Review, stamp red for failures only. | Proposed |
| 16 | Bottom bar with icon plus one-word label for the four destinations. Labels kept despite the minimal text brief, for learnability and accessibility. | Proposed |
| 17 | Confirmation shown as an in-app card plus a system notification with the same wording. | Proposed (wording is plan baseline, presentation proposed) |
| 18 | Icon set approached as a semantic meaning-to-label mapping, drawn with Material Symbols Outlined where a suitable glyph exists, custom vectors where not. | Proposed |
| 19 | Motion: short stamp-settle confirmation, no count-up animation on money, no continuous pulse animation anywhere. Reduced motion swaps all movement for crossfades. | Proposed |
| 20 | Undo stays reachable after the confirmation card leaves, through the record's detail view and Recently deleted, never through OS notification history. | Proposed |
| 21 | Home shows one selected currency's totals at a time; additional currencies get their own separated blocks, never one combined hero with stacked currency codes. Net recorded cash movement is direction-based, while income and spending stay classification-based. | Required by planning review, revision 2 |
| 22 | Accepted manual and captured entries require a known currency. Unknown currency lives only in drafts and Review and is excluded from confirmed totals. | Required by planning review, revision 2 |
| 23 | Capture and setup status names observed evidence only: configured, sample received, real event last received, processing completed. No "healthy" or "listening" wording, no continuous pulse. | Required by planning review, revision 2 |
| 24 | Lock-screen privacy in the default mode redacts the system notification's title and body at posting time; amounts and names appear only by explicit opt-in. | Required by planning review, revision 2 |

## Domain exploration

The interface-design discipline requires understanding the product's world before proposing any visual direction.

Domain concepts, the territory this product lives in:

1. **Receipt book / ledger book.** A private paper ledger the user checks and corrects by hand. Lines are written in, struck through, amended. This matches the product exactly: automatic records that are always editable, edits that survive automation.
2. **Evidence trail.** Every row carries provenance: captured from an alert, entered by hand, or linked to other evidence. The engine treats source events as evidence and transactions as records. The UI must be able to show what the machine saw versus what the user confirmed.
3. **Sorting tray.** Incoming mail for the ledger. Most items go straight into the book with a stamp. Ambiguous items sit in a tray until someone makes a call. This is the Review inbox.
4. **Cash flow, the actual metaphor.** Money is water in the product's name. The app watches a current move through accounts, sometimes with bends and fees, sometimes with a false reading. Direction and net movement are the primary reading.
5. **Ink stamps.** The old clerical gesture for "this has been processed": PAID, VOID, PENDING. A stamp reads instantly, needs no translation, and survives without large text. It also gives the interface a moment of play.
6. **Honest bookkeeping.** A ledger does not judge. Money out is not bad, it is just out. Failures are the only genuinely negative state. This distinction should live in the colour system.

Colour world, colours that exist if this product were a physical space:

1. **Ledger paper cream**, off-white with warmth, the ground everything sits on.
2. **Ledger ink**, warm near-black with a trace of brown, for writing and structure.
3. **Accounting green**, the bookkeeper's green used for credits in classic ledgers, here repurposed as the single brand and money-in accent.
4. **Amber stamp ink**, the colour of a PENDING stamp from a busy office, used only for things not yet settled.
5. **Pencil violet**, coloured correcting pencil used to mark suspect entries, reserved for Review.
6. **Void stamp red**, deep red-orange, used exclusively for processing failure and destructive consequence, never for ordinary spending.

Signature: **the connector line.** One continuous, slightly irregular stroke, like a pen line drawn without a ruler, at one constant width everywhere it appears. It threads related surfaces together: it underlines the Home hero's per-currency figures, passes beneath Activity date group headers, runs across the confirmation card's top edge, and links onboarding progress ticks. It is decoration, nothing more. Its width, length, and motion never follow any data value: it encodes no amount, no flow magnitude, no freshness, and no direction, and it is never rescaled or animated by a change in totals. It must never read as a chart, a sparkline, a trend, or a live feed; all data meaning lives in the numerals, glyphs, and stamps. It appears as a quiet thread precisely where the app wants to say "these things belong to one book". A secondary motif, the stamp, handles confirmation states; the line stays the single signature.

Defaults this interface type always falls into, named so they can be rejected:

1. **Default: dashboard grid of metric cards with a donut chart.** Rejected. Cashflow is a single story, "money moved like this", not a panel of nine tiles. The Home hero is one numeral per currency block over a decorative thread.
2. **Default: green equals income, red equals expense.** Rejected. Red is reserved for failure and destructive action. Money out is printed in ink like any other fact. Direction is carried by an arrow and a word, colour stays in supporting role.
3. **Default: dynamic theming picking up whatever blue or pink the phone is set to.** Rejected. Cashflow's identity is its paper-and-ink world, which dynamic palettes would flatten into generic system chrome. A fixed brand palette, tuned per mode.
4. **Default: icon-only bottom navigation.** Rejected, against the brief this one time, because money apps punish unclear navigation and four icons can collide in meaning. Icon plus one-word label, with the label presence confirmed in the acceptance criteria.

## Visual direction

One sentence: **a pocket ledger book, kept deliberately, with a stamp for moments the machine earns your trust.**

- Ground: paper, not grey. Surfaces are warm and quiet, structure arrives with one-point borders, not shadows.
- Ink: text hierarchy built from a single ink colour stepped in lightness, four levels.
- One accent. Green does brand work and money-in work and nothing else.
- Status is stamped, not painted: small chip glyphs with slight rotational settling, readable at a glance, always paired with a word.
- Playful, but only at the seams: the constant-width hand-drawn thread, the settled stamp, a line of voice in empty states. The ledger rows themselves stay calm and scannable. Fun lives in moments, not in decoration on data.
- Minimal text is enforced with priority: amounts, directions, and statuses show as glyph plus word with the word tiny or absent where meaning is unambiguous; anything unfamiliar, financial with multiple meanings, permission related, or destructive always carries its word. This hierarchy prevents the brief from degrading safety.

### Amount display rules (plan baseline for correctness, expression proposed)

1. Amounts are always numerals with tabular figures so columns of amounts align.
2. Currency is never dropped. In mixed-currency views the code sits with every amount. In single-currency views the code appears once in the list header.
3. Direction is shown by a glyph beside the amount. No sign characters on their own; a lone minus means nothing unread, and colour is not the carrier.
4. An amount with unknown currency renders with the words "unknown currency" beside the currency-unknown marker, never a guessed symbol. Unknown currency exists only in drafts and Review: an accepted entry requires a known currency, so an unknown amount never appears in confirmed totals, on Home, or in any per-currency block.
5. Fees, refunds, and transfers each display their own amount next to their own glyph, never a net made silently.
6. Amounts never animate their value. The container may settle, the number is stable from first paint.

## Typography

Three bundled faces, no reliance on platform default styling:

| Role | Face | Use |
|------|------|-----|
| Voice | Bricolage Grotesque, semi-bold for headings, medium elsewhere | Screen titles, onboarding headlines, empty-state lines, toggle labels in Settings not critical to data. Never used for money |
| Ledger | IBM Plex Sans, regular and medium, tabular figures on | Every amount on screen, the hero figures included, plus list rows, form fields, dates, statuses, notification text equivalences |
| Reference | IBM Plex Mono | Evidence IDs, references, build info in Settings, nothing prose-like |

Three bundled faces is a deliberate cost, acknowledged here rather than hidden: Voice gives the human, slightly playful sound the brief asks for, Ledger carries every number, and Reference is voided first if bundling weight becomes a problem. IBM Plex serves as the money face because of its tabular figures and its lineage in technical and financial documentation, replacing the AI-default Inter. The hero is not a typographic exception: money amounts use one face at every size, so the hero figure is the Ledger face set at hero scale, warm through the palette and the connector below it, not through a different typeface.

Rules:

1. Money numbers always render in the Ledger face with tabular figures verified in the shipped font file. Verification is a Phase 1 task using the bundled font; if a candidate face lacks tabular figures it is replaced before build.
2. Four text levels exist at minimum: primary ink, secondary ink, tertiary ink, placeholder/disabled. All four get used; hierarchy is never size-only.
3. Amounts on rows render at the leading size of the row and never smaller than body text.
4. Text scales with the OS setting. Every layout must survive the largest standard accessibility size without clipping amounts; rows push, not truncate.
5. Locale formatting for amounts and dates is applied at render, while storage keeps exact values. No rounding, no dropped currency code.

## Palette

Candidate values, to be tuned against contrast targets during implementation. Names follow the interface-design token rule: reading the tokens should tell you which product they belong to.

| Token | Light value candidate | Dark value candidate | Meaning |
|-------|----------------------|----------------------|---------|
| paper | warm cream, roughly #F7F2E8 | warm carbon, roughly #14120D | ground of the whole app |
| raise | slightly lighter cream, roughly #FCF8EF | one step lighter carbon, roughly #1D1A13 | elevated surface above paper |
| sop (raised-raised) | near white cream, roughly #FFFCF3 | two steps lighter, roughly #26221A | sheets above cards, only where stacking occurs |
| ink | warm near-black, roughly #221E17 | warm off-white, roughly #F0EBE0 | primary text |
| ink-2 | roughly 60 percent lightness of ink | roughly 70 percent of paper | secondary text |
| ink-3 | roughly 45 percent | roughly 50 percent | metadata, timestamps, origins |
| ink-4 | roughly 30 percent | roughly 35 percent | placeholders, disabled |
| line | ink at 10 percent alpha | paper at 12 percent alpha | standard hairline border |
| line-soft | ink at 6 percent alpha | paper at 7 percent alpha | soft separation |
| green | accounting green, roughly #0E6B43 | same hue lightened and desaturated, roughly #3FA372 | brand accent and money in, never money out |
| green-wash | green at 10 percent alpha | green at 16 percent alpha | tinted chip ground for money in |
| amber | pending amber, roughly #9A6A00 | lightened, roughly #D9A441 | pending, capture paused, needs nothing yet, waiting states |
| amber-wash | amber at 12 percent alpha | amber at 16 percent alpha | tinted chip ground |
| violet | pencil violet, roughly #5645C2 | lightened, roughly #9A8BE8 | Review, unclassified interpretation, pencil marks |
| violet-wash | violet at 10 percent alpha | violet at 16 percent alpha | Review tinted ground |
| fail | void stamp red, roughly #B3391F | lightened, roughly #E57157 | processing failure, destructive action only |
| fail-wash | fail at 10 percent alpha | fail at 16 percent alpha | tinted chip ground |
| read | blue ink, roughly #2F5DA8 | lightened, roughly #7FA3DE | links to guides, evidence, help. Only where a thing opens elsewhere |

1. Green is the lone accent by default. If a screen shows green, amber, violet, red, blue and more than one status at once, statuses still read because each carries a glyph and a word, and washes stay at low alpha.
2. Currency is never inferred from a flag emoji or a country colour.
3. Dark mode keeps the same hue families, lightened and desaturated, never the same hex values.
4. No gradients. The only tonal effects are washes, hairlines, and the hand-drawn line stroke.

## Surfaces and depth

Depth strategy: **borders only, with washes**. No shadows, which suits a paper world and stays legible in dark mode. The elevation scale:

1. **paper**, the screen ground.
2. **raise**, cards, sheets, rows in lists, the bottom bar itself. Separated by line or by one lift of tone, never both visibly.
3. **sop**, floating elements: the confirmation card, pickers, the account create sheet, full-screen overlays. One level above their parent surface, always.

Inputs are inset: form fields render a tone darker than their surrounding surface with a one-point border, signalling "write here" without heavy outlines. Sidebars do not exist in this layout; the bottom bar shares the paper tone with a soft top line, keeping one visual world instead of navigation-below versus content-above.

Border progression, matched to importance: line-soft for row separation, line for card edges, an emphasis border at ink 20 percent for the focused form field and selected chips, and a fail-coloured border for destructive confirmation contexts. Radii scale: small radius, roughly 8 dp, for chips and inputs; medium, roughly 16 dp, for cards and rows; large, roughly 24 dp, for sheets and the confirmation card. Never mixed randomly; small elements never get large radii.

## Icon vocabulary

Mapping approach: the left column of every table below names a semantic meaning, not a glyph. Each meaning maps to exactly one glyph at build time: chosen from Material Symbols Outlined where the set contains a good drawn form, otherwise a custom vector drawn at the same stroke weight for the same grid. The fixed, tested contract is meaning, accessibility label, and where the glyph may appear; the exact drawing may change between builds. The build-time mapping table records, per meaning, whether the glyph is a Material Symbols reference or a custom vector, so no meaning silently loses its drawing. No mixing with geometry from other sets. Every icon in the app carries the fixed accessibility label from this table, localised, never reused by meaning drift.

Navigation and action icons:

| Meaning | Use | Accessibility label | Glyph note |
|------|-----|--------------------|-------|
| Destination, home | Home destination | "Home" | Material Symbols house-style glyph expected |
| Destination, activity | Activity destination | "Activity" | Ledger-row style glyph expected |
| Destination, review | Review destination, badge with count | "Review, {n} need checking" | Tray reads as sorting tray, matching the Review concept |
| Destination, settings | Settings destination | "Settings" | Material Symbols sliders expected |
| Add transaction | "Add transaction" button | "Add transaction" | Always paired with the word on its button face |
| Edit | Row and field editing | "Edit" | Only when the row or field is the clear subject |
| Delete | Record deletion | "Delete" | Never icon-only. The destructive button always reads the word Delete with the count or item named in the surrounding copy |
| Undo | Remove the Cashflow record, never the bank payment | "Undo" | The word is always present |
| Confirm | Confirm a pending or reviewed entry | "Confirm" | Word always present |
| Ignore | Ignore a review or evidence item | "Ignore this item" | Word present in menu, icon acceptable in dense card row with tooltip and label |

Direction and status stamps:

| Meaning | Use | Accessibility label | Glyph note |
|------|-----|--------------------|-------|
| Money in, by direction | Direction In on rows, figures, and summaries | "Money in" | Green when available |
| Money out, by direction | Direction Out on rows, figures, and summaries | "Money out" | Ink, never fail red |
| Direction unclear | Direction Unclear where evidence does not settle it | "Direction unclear" | The third and only other direction value |
| Own-account transfer | Transfer kind and its linked legs | "Transfer between own accounts" | The word Transfer is present with the glyph wherever the kind appears |
| Pending | Lifecycle status | "Pending" | Always with the word Pending |
| Payment failed | Financial failure | "Payment failed" | Distinguishes from processing failure below |
| Processing failed | Processing failure | "Processing failed" | Different glyph from payment failure on purpose, word present in both |
| Refund | Refund or reversal, linked to original, in either direction | "Refund" or "Reversal" | Word always present, since a return arrow alone is ambiguous and direction is carried separately |
| Fee | Fee kind | "Fee" | Word present, fee amounts shown separately |
| Unclassified movement | Economic meaning not yet named | "Not yet classified" | Shown as an honest state, never as income or spending |
| Recorded confirmation | Confirmed recorded entry | "Recorded" | The stamp glyph, paired with the connector line on the confirmation card |

Capture and provenance icons:

| Meaning | Use | Accessibility label | Glyph note |
|------|-----|--------------------|-------|
| Capture source | Notification source, capture | "Capture source" | |
| Capture off | Source muted or revoked access | "Capture off" | With a line of explanation, never the bare icon |
| New captured activity | A new captured event has arrived | "New captured activity" | |
| Entered by hand | Manual entry origin | "Entered by hand" | Shares visual language with the edit pencil but distinct glyph |
| Linked evidence | Evidence linked, duplicate evidence, linked legs | "Linked evidence" | |
| Analysis contributed | Model or rule analysis contributed | "Checked by analysis" | Glyph kept abstract, not a generic AI sparkle, and the word always present, since a sparkle alone would obscure the meaning |
| Privacy | Privacy and data controls | "Privacy" | Only in Settings context |
| App lock | App lock | "App lock" | |
| Export | Export | "Export data" | Word present |
| Restore | Restore | "Restore data" | Word present |

Guides and onboarding:

| Meaning | Use | Accessibility label | Glyph note |
|------|-----|--------------------|-------|
| Guide step | Guide step | "Step {n} of {total}" | Announced, not relied on visually |
| Checklist item done | Step state | "Done" | |
| Checklist item not started | Step state | "Not started" | |
| Checklist item in progress | Step state | "In progress" | |
| Looks different | A step does not match the device | "This step does not match what you see" | Opens troubleshooting path |

Money-meaning safety rules, plan baseline intent expressed as proposed rules:

1. No icon may be the sole carrier of a financial meaning. Amount, direction, and economic kind always have a word or an unambiguous numeral beside the glyph somewhere in the row or control.
2. Unfamiliar financial states, transfer, refund, fee, unclassified, and direction unclear, always render glyph plus word in every list, card, and summary where they appear. There is no encounter counter and no learning curve: the word never shrinks away with repeated use, so the label a screen reader reads is the label a daily user sees.
3. Currency amounts and currency codes are always text. Icons never stand in for currency.
4. Permission requests and destructive actions are always in prose with a named consequence. No icon-only grant or delete.
5. Unfamiliar actions, anything that is not add, edit, delete, confirm, ignore, undo, use an icon beside a word at all times.

## Navigation

Proposed on top of the plan-baseline four-destination model.

Bottom bar, all heights, icon above a one-word label:

- Home: house icon, label "Home".
- Activity: ledger-rows icon, label "Activity". One word, consistent with the other three bar labels. The screen's own title carries the longer phrasing "All transactions" where space allows; the bar never says it.
- Review: tray icon, label "Review". Badge shows count of items needing a decision, hidden when zero, announced by the accessibility label when present.
- Settings: sliders icon, label "Settings".

The Add transaction control is a distinct floating button in the lower right above the bar on Home and Activity, green ground, plus icon with the word "Add" on its face. It is reachable with the thumb and visually separated from the bar so it reads as an action, not a fifth destination.

Where Add is not available, per the confirmed model, Review and Settings keep Edit-equivalents out of their surfaces but Review items open Edit from within cards, and Settings keeps Provide-manual-entry reachable through a Home shortcut back. Concretely: Review items each carry Edit in their action row, and Settings guides a user back through Home where needed.

Rules:

1. Back navigation is system-consistent on both platforms, with the Android back gesture and iOS swipe handled by the shared navigation layer. From any Settings sub-screen, back returns to Settings, never Home.
2. State is preserved when switching destinations: scroll positions, filters, half-filled add form preserved when tripping to a guide or Settings, per the confirmed requirement.
3. The Review badge is the soul of the tray concept; it must not duplicate with a second banner on Review itself.
4. Deep links from notifications land on the specific record, Review, or the flagged capture state, never a generic screen.

## Core screens

### Home

Purpose, plan baseline: money in, money out, net movement, period, currency, recent activity, review count, restrained capture status. All totals carry the label "Recorded activity".

Currency treatment, required by planning review: Home shows one currency's totals at a time. The default is the currency of the most recent recorded activity, switchable. Every currency with recorded activity in the period is visible on Home, as its own separated block, stacked below or next to the selected one; a compact selector appears when more than two exist. There is never one combined hero with stacked currency codes, never a summed cross-currency figure, and a cross-currency transfer contributes each of its legs to its own currency's block only.

Net recorded cash movement, defined once for every screen that uses the phrase: completed, accepted money in minus completed, accepted money out for the period in that currency, counted by direction. An unclassified completed movement counts toward this net by its direction, because the movement itself happened, while it stays economically unclassified and is reported separately. Income and spending are narrower, classification-based figures: income counts entries recorded as income, spending counts entries recorded as expense. A refund that came in raises recorded cash movement without raising income. Transfer principal between owned accounts counts in neither figure and appears separately as moved between accounts.

Proposed layout, top to bottom:

1. **Greeting-free header row.** Left: period chips, Today, Week, Month, and a custom range behind a more-options icon, each an icon-with-word chip. Right: capture status expressed as evidence only, never the word healthy or listening and no pulse: a small stamp naming the last observed fact, for example "Last payment captured 09:12", "Sample test received", "Processing completed 09:13", or, when action is needed, the failure or pause wording from the capture states. Before any evidence exists the slot is empty. A screen-reader label reads the same sentence of evidence.
2. **Per-currency hero block.** Top to bottom inside the block: an overline "Recorded activity, 20 to 26 October" with the currency code; the hero figure, the net recorded cash movement for that currency in the ledger face; the constant-width connector thread beneath it, decorative only; then a two-figure row, money in with the up-circle glyph and money out with the down-circle glyph, both direction-based per the definition above; then a quiet classification line: "Income {a}, Spending {b}, Moved between accounts {c}, {n} not yet classified", each figure present only when nonzero. A read-blue "what this means" link opens one screen explaining the difference between cash movement and income or spending, and recorded versus verified, in three sentences.
3. **Additional currency blocks** repeat the same shape, separated by soft lines. No block ever aggregates another currency's figures.
4. **Review strip.** Present only when Review is not empty. Violet wash card, tray glyph, count, one sentence naming the biggest uncertainty at hand, tap opens Review. Hidden when zero. This is not a banner, it is an inbox row.
5. **Capture attention row.** Present only when action is needed: access revoked, no sources selected, capture paused, an unsupported device trying setup. Bell-status glyph, one sentence, action link. When nothing needs attention, nothing at all appears in this slot.
6. **Recent activity.** Last five rows in the FlowRow component, "See all" text link to Activity.
7. **Empty home.** If no entries at all: voice headline "Your book is open", one sentence "Add a payment by hand, or set up capture to write it in for you", buttons Add manually and Set up capture, plus "Explore an example" which opens the sample sandbox marked Example. Sample data never enters totals, plan baseline.

Home does not show: donut charts, a balance trend line pretending to be a bank balance, a combined multi-currency hero, a data-shaped or animating connector, promotions, a widget of icons. Restraint here is the brand.

### Activity

Purpose, plan baseline: date-grouped rows with amount, direction, counterparty, account, category, status, origin. Search and filter.

Proposed layout:

1. **Search field** at top, inset surface, placeholder "Search people, notes, references". Live results update the list; no separate results screen.
2. **Filter chip row.** Chips for account, currency, direction, status, origin manual or captured, category, date range. Each chip icon plus word. Active chips show a count. A reset chip appears whenever any filter is active.
3. **Date-grouped list.** Group header: date word, underlined by the constant-width connector, decoration only. Rows are FlowRow components: direction glyph, amount with currency, counterparty on the lead line; account dot, status stamp glyph, origin glyph on the second line. Group headers and rows both keep row text at body size minimum.
4. **Row menu**, tap the row itself to open detail. A detail is where edit, delete, merge, split and mark-as-transfer live, so the list stays tappable.
5. **Empty state with filters off.** Voice line "Nothing in the book yet", action chips Add manually and Set up capture.
6. **Empty state with filters on.** "No matches for these filters", a reset chip in the flow, not hidden in a menu.

Transaction detail view, plan-baseline contents: linked source events, plain explanation of classification, edit history, edit, delete, merge, split mistaken merge, mark as own-account transfer.

Proposed layout:

1. **Hero row**: amount, currency, direction glyph, status stamp. Below, one sentence in voice type explaining classification plainly, for example: "Recorded as spending at {counterparty}. Captured from {source}. You confirmed it on 21 October.", or for an unclassified entry: "Money moved. Direction is clear, meaning is not. You can name it when you know it."
2. **Opposing leg or evidence cards.** For a transfer: both account legs with their own currencies and amounts, the fee if any as its own row, never silently net. For any record: source events as evidence rows, link glyph, source name, capture time, access to the raw event while within the retention window, with an honest note when expired raw text is gone.
3. **Edit history.** Timeline of corrections: who, when, what changed, prior value. Readable rows, mono type for changed values.
4. **Action row**: Edit, Merge with another record where evidence suggests, Split where a merge happened, Mark as transfer, Delete. Delete on the detail view names the consequence and opens the confirmation defined in the manual add and remove section.
5. Merge and split each explain their effect in one sentence before committing, confirmed. Proposed: a sheet with a before-and-after mini ledger, two columns of rows, so the user sees the numeric result before it commits.

### Add and edit transaction

Plan baseline requirements: required fields for an accepted entry are amount, a known currency, occurrence date, direction, status. Account may be unassigned. Optional category, counterparty, note. Incomplete entries may be held as drafts or handed to Review. Type choices include income, expense, transfer, refund, fee, unclassified movement. Direction, In, Out, or Unclear, stays distinct from economic meaning, and own-account transfer is an economic kind with two directional legs, not a third direction. For own-account transfers, request both accounts when known, support different currencies and explicit fees, never infer an exchange rate. Validate before saving. Preserve unsaved edits when switching to a guide or settings where practical.

Proposed layout, single column, amount first:

1. **Amount hero field.** A large numeral entry field in the ledger face, currency picker chip beside the entry: code word, tap opens a searchable currency sheet listing common currencies plus a full list. A known currency is required before an entry can be accepted: an accepted record with unknown currency is blocked, with the inline sentence "Choose the currency to record this, or keep it waiting". Unknown currency is allowed only as a draft held in Review, never as a saved accepted entry and never inside confirmed totals; the app never guesses a currency from locale or counterparty.
2. **Direction and kind as two stepped choices, not one.** Direction has exactly three values, In, Out, Unclear, carried by glyphs and words: money came to you, money left you, or the evidence does not settle it. Own-account transfer is not a direction; it is an economic kind with its own relationship and two directional legs. Kind chips: Income, Expense, Transfer, Refund, Fee, Unclassified. The app validates the pair and explains mismatches with one inline sentence that never claims what a refund usually is, for example: "A refund can be money you received, such as a merchant repaying you, or money you sent, repaying someone else. Kind records the relationship; direction records what actually happened."
3. **Occurrence date.** Date field with a calendar sheet, default today. An advanced affordance clarifies whether the date is the event time, capture time is kept automatically and shown in evidence, and user correction overrides automation, plan baseline.
4. **Status.** Chips: Completed, Pending, Failed. Default completed for manual entry, since the user is recording what happened.
5. **Account.** A picker with Unassigned as the first, valid option. A hint "Leave unassigned if you are not sure" prevents forced accuracy.
6. **Counterparty, note, category.** Optional, collapsed into a single "more details" sheet to keep the base form three scrolls deep at most.
7. **Transfer specifics.** When kind is Transfer: the direction pair becomes the two legs. Each owned leg has its own account picker, its own direction, out of one account and into the other, and its own amount and currency when they differ, plus an explicit fee field defaulting to none with its own currency. If only one account is known, the other leg stays Unassigned and the entry keeps its place in Review for completion. Wording "If the exchange rate is not known, enter each side as it appeared", no constructed rate, never a netted single figure.
8. **Save.** Green word button, bottom anchored, always with the word Save. Validation errors land inline next to the field with the sentence, never as a toast. A failed save shows the wording from the failed save state defined in the states section and never the word Recorded. When the currency or amount is missing, the control offers Save as draft beside the disabled save: the draft sits outside confirmed totals and is labelled "Not recorded yet" wherever shown, and its path onward is completing it or handing it to Review.
9. **Unsaved edit preservation.** Plan baseline where practical: leaving the form to a guide or Settings keeps the draft. Return restores it. Discarding a draft requires a tap with the words Discard draft.

### Review

Plan baseline: each item explains the uncertainty: missing amount, unknown currency, conflicting evidence, possible duplicate, possible internal transfer, unknown account. Users confirm, edit, link to existing, or ignore. Partial data stays editable. Processing failure is separate from ambiguous financial interpretation. A Review item's direction is whatever the evidence supports, In, Out, or Unclear, and is never guessed to fill a figure. An item with unknown currency states plainly on the card: "This stays out of your totals until the currency is known", and confirming it requires the currency to be set first.

Proposed layout:

1. **Two sections, never mixed.** "Needs a decision" holds ambiguous financial interpretation: violet wash section header. "Did not process" holds processing failures with operational reason: fail wash header, different glyph, different corrective actions, retry versus resolve. This split is confirmed by the engine docs and deserves visual expression.
2. **Uncertainty cards.** Each card top to bottom: stamp-style uncertainty marker, one sentence in plain words naming exactly what is uncertain, available parsed pieces shown as partial data, which stays editable per confirmed requirement, and an action row: Confirm, Edit, Link, Ignore, each with word. Cards never bury the action row; worst case the row scrolls into view with a persistent bottom sheet holding the same actions.
3. **Group naming.** Header counts by reason where useful: "3 payment checks, 1 possible duplicate". Not exhaustive at card level; the reason sentence is always on the card.
4. **Lightweight batch.** Some uncertainties resolve in one tap when the evidence is that unambiguous, for example a duplicate attaching to an existing record. The card shows the resolved action as a single confirm chip with the sentence "Attach this to {existing record}" and one sentence explaining that exact duplicates attach quietly as a policy.
5. **Empty state**: voice line "Nothing in the tray", one sentence "Payments land here when Cashflow is not sure. Clear ones never bother you.", plus the connector thread at constant width drawing a small resting underline.

### Manual add, edit, remove, restore

Add and edit are defined with the add and edit screen. Remove, confirmed requirements: recoverable deletion, undo immediately, recently deleted view with a proposed 30 day restoration window, replayed source evidence must not recreate a deleted transaction, permanent deletion explains which evidence goes with it.

Proposed interface:

1. **Delete.** The button always reads the word Delete, never icon-only. First tap opens a sheet, not a system dialog, with the record summary, the sentence "Deleted entries can be restored for 30 days", and the count of source evidence attached that will lose its raw retention anchor. Confirm sheet button reads "Delete transaction". Destructive styling: fail colour on the confirm control, border not fill where possible.
2. **Undo on delete** appears as a confirmation card like the capture undo, word Undo on the card, with the same semantics confirmed.
3. **Recently deleted**, reachable from Activity overflow and from Settings under data. Rows show name, amount, date deleted, days remaining, and Restore word-button. Bulk restore allowed. Permanent delete per item names the exact evidence and fingerprint consequences and requires a two-step confirm.
4. **Suppression visibility.** The detail of a recently deleted entry can say "Cashflow will not recreate this from the same alert" honest and small.
5. **Restore.** On restore, a confirmation card states the record is back and totals updated. Restored entries keep their revision chain.

### Capture status surface

Plan baseline: capture status distinguishes configuration, last observed input, and last successful processing. This surface names evidence only: each row is an observed fact with a time, or the words Not yet. Nothing is ever called healthy or listening, no glyph pulses to imply live watching, and no state claims monitoring between events, on either platform; the card reports what the app last saw and when it saw it.

Proposed: a Capture card inside Settings summarising:

1. Configuration: "Access granted, 3 sources selected", or the specific missing piece.
2. Last observed input: "Last payment alert seen 09:12 from {source}", or "Not yet".
3. Last successful processing: "Processing completed 09:13", "Processing completed with a check needed", or "Not yet".

Three rows, a stamp glyph and one sentence each. Sample and test events are named as sample or test, never recorded as if they were source payments. From the Home attention row this card is one tap away.

### Settings and accounts

Plan baseline: capture sources, platform setup guide, model processing choice, notification preferences, context window, currencies, accounts, privacy and data retention, export and restore, app lock, help, reset.

Proposed layout: a single Settings list, grouped:

1. **Capture.** Sources, platform guide, capture status card.
2. **Interpretation.** Model choice, context window under advanced.
3. **Output.** Notifications, lock-screen notification privacy with the amounts-and-names opt-in.
4. **Money.** Currencies, accounts.
5. **Data and privacy.** Retention, export, restore, app lock, reset.
6. **Help.** About, version, feedback.

Each row icon plus word plus current value where one exists, for example Notification preference: All activity. Reset is visually the fail family only by its border on tap, not plastered in red at rest.

Accounts screen, confirmed requirements: user-defined name and currency, optional institution and masked suffix, never require full bank credentials or full account numbers, archive instead of breaking history, new evidence about an unknown account is not silently assigned to the last-used account.

Proposed interface:

1. **Account cards**, raise surface, name, currency code, masked suffix as entered, linked activity count. Archived accounts collapse into an archived section with a clear restore link.
2. **Create or edit as a sheet**: name required, currency picker required, institution optional, masked suffix optional with an input rule forbidding full numbers, showing masked preview as you type.
3. **Archive** asks one question in a sheet: "Activity on {account} stays in the book." Confirm word is Archive.
4. **Unknown account handling.** When captured evidence arrives with no account match, the Review card asks about assignment with the accounts listed plus Unassigned plus "new account". Never a subtle default.

## Onboarding

Confirmed structure: shared introduction of five steps, Android branch, iOS branch, finish and later education. Every optional step has Set up later and is resumable from Settings. Permission requests follow the explanation they depend on. Refused permissions are not requested repeatedly.

Proposed visual frame:

1. **Stepped sheets, not a carousel.** Each step is a screen, not a horizontally scrolling card, so back navigation is standard and predictable. A plain progress rule at the top fills between tick marks one step at a time; a plain progress rule, not the connector thread, because progress is a measured length and the connector never carries meaning. Accessibility label announces "Step {n} of {total}".
2. **The Example stamp.** Every sample artefact in onboarding carries a stamp reading Example, per confirmed requirement that examples never enter totals and are never mistaken for a real event. The stamp is a visual seal, slightly rotated, in violet to match the pencil-mark language, and its accessibility label includes that it is a sample.
3. **The sandbox.** Step 3 of the shared introduction lets the user press Undo or Edit on a fake confirmation without writing to the ledger, confirmed. Proposed interaction: the sandbox card behaves exactly like the real one, but on Undo the card visibly stamps a small "sandbox" seal and resets in place, with one sentence "That is all Undo does in the real app, remove the Cashflow record, not the payment." Copy confirmed by the onboarding doc, layout proposed.
4. **Typography and tone.** Voice face headlines, one sentence maximum of body copy above the action buttons, action button always word labelled. Set up later as quiet text link, never a grey main button that could be missed and interpreted as refusal.

Shared steps, mapping to the confirmed copy:

1. **Welcome.** Headline "Keep track as money moves." Copy per doc. Action buttons Get started, Explore with an example. Example path shows one sample payment and its timeline row, marked Example, then returns.
2. **Choose what Cashflow watches.** Two-panel diagram, purposed drawing: a dotted bell to the left, awaited filtering, the book to the right awaiting written rows. Gaps acknowledged: "Missing or hidden alerts leave gaps", with a small visual of a skipped line in the ledger illustration. Honest scope, no implication of complete banking history, confirmed.
3. **Recorded automatically. Always editable.** Sandbox confirmation card as above.
4. **Basic preferences.** Currency suggested from locale with an explicit confirm tap, not pre-affirmed. Optional account name and currency in the same sheet, both can be skipped.
5. **Processing and privacy.** Two cards, local parsing default with "known formats parsed on your device", remote analysis opt-in with the exact copy from the doc, showing what leaves the device, retention, disable path, named provider. Decline leaves manual entry and local parsing usable, confirmed visually by keeping both actions full width.

Android branch, confirmed steps with proposed presentations:

1. **Source picker.** Full-screen sheet: search field, list of installed apps with icon and name, nothing selected by default. Suggested supported financial sources shown in a first group labelled "Commonly used" without implying exclusivity. Each row is a checkbox tile. A Continue with none selected is possible, leading to manual entry path, confirmed.
2. **Explain access.** Explainer card before leaving the app: broad notification listener, Cashflow filters to your selection before anything is stored, confirmed. One diagram: bell, funnel, book.
3. **System grant.** A card with the action button opening notification access settings, plus the honest sentence that returning from settings is not proof, confirmed. A check runs on return.
4. **Post notifications.** A separate card asking whether Cashflow may post its own confirmations, explaining the two permissions differ, confirmed.
5. **Sample test.** Card with action Run sample test, labelled Sample test, kept outside the ledger. Then an option "Wait for a payment alert", turning the card into a static waiting state: word Waiting beside the bell glyph, no pulse, no continuous animation, until an event arrives or the card is dismissed.
6. **Results.** Four checklist tiles: sources selected, access granted or missing, sample tested, real event observed or not yet. Tile states: done check, in progress half, not started dashed, missing fail octagon with a fix link. Badges match the guide icon vocabulary.

iOS branch, version-adapted draft. No verified screenshots or labels exist. This section describes what each screen must contain and leave [VERIFY] slots; the delivery plan produces real screenshots on supported devices before shipping.

Compatibility check screen. States from the probe: supported with versions stated only once verified, unsupported with alternatives. Unsupported copy: explains the limitation, offers pasted text entry, manual entry, and any verified alternative automation path, avoids an endless loop, confirmed. Deliberate avoidance: no minimum version claim anywhere in this draft.

Setup landing screen. Headline "Connect payment alerts with Shortcuts." Copy confirmed: user chooses apps, Shortcuts passes their details to Cashflow. Checklist of the steps below as a preview. Actions Open guide, Set up later.

Guided setup, each step a sheet carrying: a purpose line, an area reserved for a real screenshot with text equivalent, one primary action, the always-present secondary action "Something looks different", and progress persistence, confirmed. Steps:

1. Find the Cashflow action. Explain the action named Capture transaction notification and its expected inputs. A standalone sample shortcut may demonstrate. Importing a shortcut is not described as automatic installation, confirmed. [VERIFY]: action discovery path on the device, exact gallery or search presentation.
2. Create an automation. Open Shortcuts or clear manual navigation, choose the notification trigger. [VERIFY]: exact trigger name and placement on supported versions.
3. Choose source apps. Selection happens in Shortcuts. Cashflow checklist tiles are guidance, not proof, confirmed. [VERIFY]: app selection presentation.
4. Choose filters. Recommend broad capture within selected apps initially, narrow filter guidance only where fields verified, confirmed. [VERIFY]: what filter inputs the trigger exposes on each supported version.
5. Add the Cashflow action. Map notification title, subtitle, body, and source identity where the OS exposes them. Unavailable fields are tagged unknown at ingestion, confirmed. Diagram shows slots with real names only after verification. [VERIFY]: field mapping presentations.
6. Choose execution. Automatic run and locked device settings as probed. [VERIFY]: execution labels and permission prompts.
7. Save and return. Guide progress persists when switching apps. A persistent Continue setup entry exists when returning, confirmed.
8. Test the connection. Run a labelled sample into the Cashflow action, then wait for a real source notification, plan baseline. Never send a fake bank payment or ask the user to spend, plan baseline. The waiting card mirrors the Android one: static bell, word Waiting.

Compact alternate: a text-only checklist view of all eight steps, available from every guide step for experienced users, confirmed as a requirement in the onboarding doc.

Verification states display. The guide tracks the confirmed list: guide not started, in progress with the last completed step, user reports configured, sample invocation received, real input observed with last time, input received but processing needs attention, setup unsupported. The status card states each in words with a glyph. Completion wording follows evidence: "Cashflow received your test", "First payment alert received", or before a real event, "Setup saved. Waiting for a payment alert." Confirmed.

Troubleshooting screens, confirmed items, proposed as a single list of cards each with the symptom, one cause sentence, and the fix action. No claims about detecting automation deletion beyond what is verified; an "I changed my automation" option and fresh connection test exist, confirmed.

Finish. Ask confirmation preference and privacy level, explain Review, show Add transaction, enter Home. Complete even when capture was skipped. Later education, confirmed items, proposed as quiet inline seats: first likely transfer match shows a one line note visible in the ledger detail, first fee detected stamps the fee row with a first-time note, first uncertain event highlights the Review strip, no repeated full onboarding after updates, only the affected guide reopens.

## Confirmation behaviour, interface treatment

Confirmed wording and semantics, proposed presentation.

### Recorded confirmation

In-app card, sop elevation, bottom sheet style, roughly 24 dp radius, the connector thread drawn across its top edge:

> stamp glyph. Recorded {amount} {direction word} to Ada.
> Undo   Edit

Behaviour: saved before shown, doing nothing keeps it, Undo removes the record's effect from totals, suppresses that captured evidence with restoration offered in the app, Edit opens the entry. Dismissing the card equals doing nothing. The card does not block the app: it floats over content and dismisses on outside tap. Undo and Edit stay reachable afterwards without any history row or OS assist: through the record's own detail view, which carries Undo while the record still exists, and through Recently deleted once the record has been removed. Nothing relies on the operating system's notification history, which is unordered, user-clearable, and never a safe place for an undo.

System notification, baseline wording, proposed posting design:

- The privacy default applies to what the operating system receives, not to an in-app template: unless the user opts in to amounts and names, Cashflow composes the notification without that content, so the sensitive text is never placed in the title or body at all. Default title: "Transaction recorded". Default body: "Open Cashflow to see the entry." Redacting at posting time is the real guarantee, because the sensitive values are never delivered to the OS.
- When the user explicitly opts in, the title reads "Recorded {amount} sent to {counterparty}" and the body carries the notification mode's text. The opt-in setting sits in Settings under Output.
- OS lock-screen preview settings, such as hiding or preview display, are outside Cashflow's control and are not relied on: the user can change them at any time, they do not cover every surface a notification can reach, and their behaviour across OS versions is not verified here. No preview setting is treated as privacy; the payload redaction above is the mechanism.
- Actions: Undo and Edit word buttons. Any action fired from the notification, including from the locked screen, first loads the current record into the app before changing anything, so a stale action acts on fresh data or is safely refused by the app's stale check.
- The opted-out default replaces the confirming title with the generic one so the pattern is learnable, but never with wording that contains an amount or a name.

### Ambiguous confirmation

> tray glyph. Payment needs a check.
> Review   Ignore

Same card structure, violet accent, actions confirmed. Ignore from the notification suppresses the event with Restore available in the app, matching the confirmed suppression model. A status update produces no second new payment alert, confirmed, and the existing record's card in the ledger quietly shows the revision.

Failed processing never shows a confirmation card, always the error state defined above, so a failure can never be mistaken for Recorded, confirmed by the failed-save rule.

## Motion

Default motion, confirm intent: short and settled, never springy, nothing bounces in a money book.

| Interaction | Motion | Duration | Reduced motion |
|-------------|--------|----------|----------------|
| Screen navigation | Content slides 8 dp into place; the connector never animates a transition | 200 to 240 ms | Crossfade only |
| Confirmation card arrival | Card settles up 12 dp with a 2 degree stamp rotation settling to | 220 ms, decel easing | Fade in, no rotation |
| Confirmation card dismissal | Slide down 8 dp, fade | 160 ms | Fade out |
| Stamp settling on status | One settle rotation from 3 degrees to rest with a tiny overshoot | 180 ms | No rotation |
| Hero numeral | No value animation, ever | none | none |
| Row add or remove in list | Height settle 1 frame plus 120 | 120 ms | Appear or disappear without layout animation |
| Waiting for a source event in setup | Static bell glyph with the word Waiting; no pulse, no continuous animation | none | none |
| Review badge | Count crossfades | 150 ms | Instant swap |
| Progress rule in onboarding | Rule fills between ticks | 260 ms | Ticks fill without rule growth |

Reduced motion behaviour, plan baseline: all motion degrades to opacity or instant state swaps, the waiting-for-event state is static by default (see its row), the sandbox works identically. Also honoured for OS natives: the Compose motion settings map to each platform accessibility setting.

## Dark mode

Same world, reversed light. Proposed treatment:

1. paper becomes warm carbon, not neutral black, so it still belongs to ink and paper, not void.
2. Definition by borders, not shadows. Hairline alphas become paper-based rather than ink-based.
3. Raises step lighter, at most two or three discernible steps in the whole app.
4. Semantic colours lighten and desaturate per the palette table so they pass on dark ground.
5. Washes increase in alpha slightly, since tinted grounds disappear at low alpha on dark.
6. The connector thread renders in paper ink, constant width unchanged in both modes.
7. The fail family darkens its wash and lightens its glyph so destructive intent stays obvious.
8. Nothing in dark mode uses pure white text, the warm off-white applies, which reduces halation against cream-keyed fonts.

## Reusable components

Minimal set, each screen composes from these:

1. **AmountText.** Renders amount, currency, tabular figures, never animates value, optional direction glyph slot. Accessibility label composes one phrase in the ledger order: "Sent 8,500 naira" or "Received 8,500 naira", currency expanded from code. No doubled words, no sign characters read aloud.
2. **DirectionGlyph.** Circle-wrapped arrow, in, out, or unclear, in the token colour, always paired with a word somewhere in the row. The transfer kind shows its linked leg pair and the word Transfer, never a third direction glyph.
3. **StampChip.** Status or evidence chip: glyph, word, wash ground, optional first-render rotation settle. Variants: completed, pending, failed, processing failed, refund, fee, unclassified, example, recorded, linked.
4. **ConnectorLine.** The hand-drawn decorative thread, one constant stroke width everywhere, drawn above the surface but below text. It carries no data, no weight scaling, no growth animation, and no accessibility semantics; it is hidden from screen readers as pure decoration. Its appearances: under the per-currency hero figures, beneath date group headers, across the confirmation card's top edge, in quiet empty states.
5. **FlowRow.** The ledger row: direction, amount, counterparty, secondary origins line, tap opens detail. A fundamental component, all Activity and Home lists use it.
6. **UncertaintyCard.** Review card: uncertainty sentence, partial data, action row, violet accent.
7. **CaptureBanner.** The one sentence attention row with bell status glyph and a fix action. When nothing needs attention the banner is absent entirely; there is no all-clear variant to keep empty.
8. **ConfirmCard.** The in-app confirmation, stamp variant and tray variant, action slots.
9. **ChecklistTile.** Guide and setup step state: dashed, half, done, failed, with the accessibility labels from the icon vocabulary.
10. **StateBlock.** Generic empty, error, offline, permission blocked presentation: glyph, one voice sentence, actions.
11. **FieldSheet.** The form sheet with inset fields, currency picker, validation slot.
12. **GuideStepSheet.** Onboarding screen frame: progress rule, plain, not the connector thread, purpose sentence, content area reserved for a verified screenshot plus textual equivalent, action row with Set up later or Something looks different by step.

Testability of components: each variant state is specified in this document, so components can be verified against the matrix in the acceptance criteria without inventing states at build time.

## State designs

Plan-baseline list of required states: empty, loading, offline, analysis unavailable, permission denied, unsupported OS, no selected sources, capture paused, partial notification, storage failure, restore failure. The list is defined here in full at design time; each state is implemented and tested in the phase where its feature ships, per the phasing rule in the acceptance criteria.

Each is a StateBlock or CaptureBanner variant. Words, not lorem:

1. **Empty, no entries anywhere.** "Your book is open." Actions Add manually, Set up capture, Explore an example.
2. **Empty, filters on.** "No matches for these filters." Action Reset filters.
3. **Empty, Review.** "Nothing in the tray." Sentence about which cases land here.
4. **Loading, cold start.** Skeleton rows matching FlowRow silhouettes, never spinners alone. Hero figures render as dashes until real values arrive, never zeros that could be misread as totals of no activity.
5. **Offline.** Local ledger works, plan baseline. A connection banner appears only when an operation that needs connectivity is attempted, such as export or restore: "Not connected. Recording continues." Queue items waiting on connectivity for already-consented remote work show a clock stamp with the words "Waiting for connection". Connectivity alone never explains an analysis failure; that is the next state.
6. **Analysis unavailable.** A distinct card and stamp, different glyph from offline, appearing even while the device is online, when the analysis provider or model call fails, times out after its bounded retries, or remote analysis is disabled by consent. Wording: "Analysis unavailable. Retry or review manually." Actions: Retry, which requeues the item, and Review manually, which opens the item with its parsed fields. The ledger and manual entry are unaffected. Analysis absence is never presented as a payment failure and never as a connection problem, plan baseline by the engine docs.
7. **Permission denied or revoked, capture.** CaptureBanner, fail wash, "Cashflow cannot read selected apps. Fix access", opening settings. Ledger fully usable, plan baseline.
8. **Permission denied, posting notifications.** Quiet row in Settings. Capture continues, per the ledger independence baseline.
9. **No selected sources.** CaptureBanner amber, "Watching no apps yet. Choose apps", opening the source picker. Manual entry unaffected.
10. **Capture paused.** Per current pause, amber with the pause reason and Resume.
11. **Unsupported OS.** Guide compatibility card, words per the plan-baseline unsupported device requirements. Stored state, shown once per device state, no endless loop.
12. **Partial notification text.** Review card explains what could not be read, for example "Amount missing from the alert. Enter it to complete this record." Partial data stays editable, plan baseline.
13. **Storage failure.** Recording halts visibly, but no data display lies about persistence. Card: "Could not save this entry. Nothing was recorded." with the retry and details path. This is the one state where failure to persist is the entire message, satisfying never fake success.
14. **Restore failure.** Export and restore screens carry a failure card listing what matched, what did not, with the file path retained. Restore verification is round trip, plan baseline by the delivery plan.
15. **Failed save.** Inline above the form field plus a persistent error card until addressed: "This did not save" never the word Recorded anywhere. Data that failed to persist returns the user to the form with the values intact where held in memory only, announced as unsaved.

## Design acceptance criteria

Testable at design-review time, before or alongside implementation. Each criterion references the section that specifies it.

1. Money display. Every amount in every specified screen renders with a currency visible at the row or list-header level, tabular figures, and a direction glyph with a word. No screen specifies money without currency. Source: amount display rules.
2. No red spending. No screen may show an ordinary expense in the fail colour. Fail colour only on processing failure, financial failure, destructive confirmations, storage failure. Source: palette.
3. Colour never carries meaning alone. Direction, status, review state, and capture state are each legible with colour removed, using glyph and word. PerfTest: greyscale render of Home, Activity, Review shows distinct direction and status readings. Source: icon vocabulary.
4. Icon never the sole financial carrier. Every icon-only control has a localised label. Transfer, refund, fee, unclassified, and direction-unclear meanings always render glyph plus word wherever they appear, with no encounter counting. Source: money-meaning safety rules.
5. Destructive and permission copy. Delete, permanent delete, reset, archive, grant access, remote analysis opt-in always appear in prose naming the consequence. No icon-only grant or destructive control exists in any specified screen. Source: brief safety hierarchy.
6. Totals honesty. Home contains a "Recorded activity" label with a link explaining recorded versus verified, and no surface that combines or sums multiple currencies into one hero. Each currency block shows net recorded cash movement by direction, with income and spending reported as their own classification-based figures and unclassified completed movements counted by direction and separately named. Transfer principal is displayed as moved between accounts and excluded from the in, out, and net figures. Unclassified movements appear as their own stamp, not inside income or expense. Source: Home and plan-baseline financial rules.
7. Confirmation. The confirmation card and notification present the plan-baseline wording, actions Undo and Edit, and Undo and Edit remain reachable after dismissal through the record detail and Recently deleted, never through a confirmation history row or OS notification history. A failed-processing path can never emit this card. Source: confirmation behaviour.
8. Review separation. Ambiguous interpretation cards and processing failure cards never share a section header, glyph family, or accent wash. Source: Review screen.
9. Deletion. Delete flows specify a restore window display, permanent deletion naming evidence consequences, and a Recently deleted row per entry with days remaining. Source: manual remove.
10. Currency correctness. No screen sums currencies. Transfer legs carry their own currencies and amounts, fee has its own field, no constructed exchange rates. No accepted entry with unknown currency exists in any confirmed total, in an exported confirmation, or on Home; unknown currency appears only in drafts and Review. Source: add/edit, Review, and detail.
11. Onboarding. Every step has a Set up later or equivalent, resumable progress, back navigation, sample content stamped Example, and sandbox interactions that write nothing to the ledger. Source: onboarding.
12. iOS guide honesty. Every iOS guide step contains at least one [VERIFY] marker where a screenshot or label is required, no invented Apple labels, no version claims, and the compact checklist is reachable from every step. Source: iOS branch.
13. Verification truth. Setup status screens distinguish user-reported configuration, sample invocation received, and real source event observed, using the plan-baseline wording. A sample event never produces the wording First payment alert received. Source: iOS verification states and Android results.
14. States defined, phased to ship. All fifteen specified states are designed and specified in this document now, each as a component variant with screen placement, greyscale and screen-reader checkable. Each is implemented and tested in the delivery phase where its feature ships: ledger-own states, empty, loading, failed save, storage failure, in Phase 1; capture-own states, permission for capture, sources, paused, offline queue, partial notification, in Phase 2; posting-notification permission and analysis unavailable in Phase 3; export, restore, and restore-failure states in Phase 4. No phase's exit requires states belonging to features that phase does not ship. Source: state designs and delivery plan.
15. Motion. Every animation in the motion table has a reduced motion alternative; values on amounts never animate; spring or bounce easing is nowhere; no continuous pulse animation exists anywhere in the specification. Script: enable reduced motion, walk through specified interactions, verify all degrade. Source: motion.
16. Dark mode. All specified screens render both modes from one token set; contrast of body-level text at minimum 4.5 to 1 against their grounds in both modes; large hero numerals 3 to 1 minimum; hairlines visible at the squint test against a dark dimming. Source: palette and dark mode.
17. Type hierarchy. Four text levels are distinguishable at a glance in both modes, and every specified layout survives the largest accessibility text size without amount truncation. Source: typography.
18. One money face. Every amount renders in the Ledger face, hero figures included. Money never renders in the Voice face. Source: typography.
19. Bottom bar. All four destinations use icon plus one-word label, Activity labelled Activity, the Review badge is hidden at zero, and Add from Home and Activity always exists as a word-carrying button. Source: navigation.
20. Components reuse. FlowRow, StampChip, AmountText, UncertaintyCard, ChecklistTile, StateBlock, ConfirmCard, ConnectorLine appear in the specified screens where stated, and no screen introduces a bespoke list row or status chip that duplicates them. Source: components.
21. Unsaved work. Leaving the add form for a guide or Settings preserves the draft; return restores it. Discard requires an explicit tap. Source: add/edit.
22. Failure is never success. Any path where persistence fails shows the failed save or storage failure design and never the plan-baseline wording Recorded {amount}. Grep of specifications finds no screen where a failure state shares wording with a success card. Source: states and confirmation.
23. Evidence-only status. Capture status in any specified screen names only observed facts with times, configured, sample received, real event last received, processing completed, or their missing piece, and never uses healthy or listening wording, never claims live monitoring, and ships no continuous pulse animation. Source: capture status surface, Home, motion.
24. Notification privacy at posting time. The lock-screen default applies to the system notification's title and body itself: in default mode the posted payload contains no amount and no counterparty name, and OS preview settings are never presented as the privacy mechanism. The opt-in setting exists in Settings under Output. Source: confirmation behaviour.

## Open questions for review

1. Font licensing and bundling weight across three faces, Bricolage Grotesque, IBM Plex Sans, IBM Plex Mono, on both platforms, verified when dependencies are chosen, with the tabular figure verification as a gate. If weight forces a cut, the Reference face is dropped first, leaving two families.
2. Does the fun tone extend to notification wording with an emoji? Default proposal: no emoji in money notifications, keeping them copy-safe and screen-reader friendly, especially aboard the redacted default title.
3. The connector's resting punctuation: whether each end carries a small mark or a plain taper, deferred to wireframes; the stroke width itself is fixed everywhere.
4. Sample sandbox reset cadence and whether the Example stamp is violet or a dedicated sample colour.
5. Drafts: where a currency-missing draft lives in the storage model before Review, coordinated with the transaction engine plan so drafts stay outside confirmed totals at the data layer as well.

Next step after review: produce screen-level wireframe descriptions or mocks per screen order listed here, coordinated with Phase 1 of the delivery plan before any implementation.
