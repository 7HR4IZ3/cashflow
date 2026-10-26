# Night current — Cashflow UI direction (concept spec, ≤100 lines)

Status: current design baseline. The user **likes the dark charcoal/citron direction** and the
icon-only navigation, and has asked for **ongoing UX/visual polish** on this exact composition —
hierarchy, clarity, ease of use. This is a living concept, not a gated proposal. App name stays
**Cashflow**. Old `fresh-current-*` files and all native source are untouched; this concept lives
only in `night-current-concept.html` + this spec.

## Palette (dark-native, matte — no cards-on-black, no purple gradients, no glow)
- Background `#101213` (page-level), layered surface `#1A1D1E`
- Ink `#F5F4EE` (warm off-white), muted `#A0A7A6`
- **Citron `#E1F58C`**: the single brand/action accent (primary action, selected surface accents, net when positive)
- Lavender, only for uncertainty (e.g. review badge); red, failure/destructive only
- Prior direction: warm white app `#FAFAF8` + mint accent + labeled full-width nav → replaced wholesale.

## Composition (completely new)
- **Activity-first home.** Compact contextual header (glyph/monogram + currency + period, tiny selects,
  and the Settings cog — the only place Settings lives, styled as an icon with aria-label/title and no
  visible label). Icon-only closed currency pickers: the compact coin-glyph button (₦ $ £ € KSh ₵,
  real currency glyphs, readable at 360px via wrapping) is all the closed selector shows; opening it
  shows full name — ISO — symbol for each supported currency, screen readers announce the selected
  currency in full, and the underlying value stays the ISO code.
  One horizontal In/Out magnitude pair — arrows convey direction — with an understated "Recorded net".
  No giant net hero with a stack of utility labels. The date-grouped, edge-to-edge feed is the centerpiece.
- **Feed rows**: geometric/initial avatars, name + full amount hierarchy, arrows for direction, quiet status
  only when it matters (Unclassified, Pending, Failed). Origin/metadata lives in the detail receipt only.
- **Filter & search** are icon buttons revealing their controls on demand; no always-on text controls.
- **Icon-only floating dock** (not the labeled nav bar): exactly three actions — Home on the left,
  the central Add plus (citron filled, no individual border) held at the geometric center via three
  equal grid columns, and the Review tray with numeric badge on the right. No per-icon borders,
  backplates or inset shadows; the active destination keeps `aria-current="page"` and is shown by a
  citron glyph with a small under-dot — no bounding-box ring. 44–48px targets, `aria-label`+`title`
  (no visible words), non-color cue via the dot; only the keyboard `:focus-visible` ring remains.
  Settings is not in the dock: its cog lives in the Home header. Hidden while Add is focused or a
  review sheet is open.
- **Review = one focused question at a time** with position progress ("1 / 2"). Full amount + currency, or a
  genuine "Amount missing" affordance where the amount editor is shown right away (not hidden behind a gate).
  Currency carries an explicit "Unknown" option — it stays unknown until picked; never silently defaults to NGN.
  In/Out as arrow buttons with tiny In/Out captions + accessible labels (no signing of unknown items).
  Picking a direction never records; an explicit Record check-button (icon-only) records only when valid.
  Ignore is a distinct icon control; the original alert opens from a receipt/document icon. No big warning cards.
- **Add = full-screen calculator.** Huge tabular amount, glyph-closed currency chooser, direction
  segment with arrow icons, 48–56px keypad, citron check/save, X cancel, a details icon revealing
  real optional fields (kind/status/account/name/note/date). Add is "write in the record book",
  never a bank transfer.
- **Detail = coherent receipt sheet** with meaningful edit / delete / close icons; delete proposes confirmation.
- **Settings** sheet is small and honest: working view-currency selector, working reduce-motion toggle,
  and an opt-in icon legend/help disclosure — opened from the Home header cog. No invented capture
  toggles; anything not implemented states so in one short line or stays out of scope.

## Implementation note (spec only, not displayed in-app)
Implementation-thinking text lives here, never in the product flow copy: internal `_R#` ids, hidden
native <select> widths used for glyph-over-closed-pickers, and class naming like `dirty` are
engineering details. In-app copy describes only user-meaningful state (e.g. "no filter active",
"nothing matched").

## Icon vocabulary (consistent 2px stroke, 24 grid)
Home = simple house · Review/tray = open tray + numeric badge · Add = plus (citron filled, dock center) · Settings = cog (Home header only, not the dock) ·
Filter = funnel · Search = magnifier (both show a small count dot + spoken state only while a
filter/query is active; obvious clear actions appear only when needed) ·
**In = one financial convention everywhere: incoming arrow to the lower-left, outgoing arrow to the
upper-right** (summary, feed rows, Add segment, Review answer) — never the old up/down double meaning.
Review previous/next = independent left/right chevrons beside the explicit "1 / 2" position counter.
Ignore = minus-circle · Original alert = receipt/document · Save/Record = check · Cancel = X
Backspace = vector erase · Close = chevron/X · Trash = delete, red only at the destructive moment.
On the financial direction question, arrows get tiny "In"/"Out" captions — an arrow alone is
ambiguous on a small touchscreen; no longer explanations there.

## Polish round (current)
- Home: slightly larger closed currency glyphs (~19px so ₦ stays legible), tiny In/Out captions on the
  summary pair only, terse "Net" label, full untruncated money everywhere, compact header at 360px.
- Review: explicit Record check (and Add Save in the calculator) renders **disabled until every
  required fact is valid**; saveDemo/resolveReview guard methods keep rejecting invalid input.
- Settings sheet is honestly functional: view-currency selector, reduce-motion toggle, optional icon
  legend; export is one plain "not in this prototype yet" line. No invented toggles, no dev prose.
- Detail sheet: note row only when set; provenance on demand.
- Sheets: focus trap + Escape, focus restore with `preventScroll`, background screens `inert`.

## Core invariants carried unchanged from the working sandbox
All money is exact integer minor units (`toMinor`); demo currencies all 2-decimals. Money is never
truncated or clipped — no ellipsis on any amount; long values wrap or shrink, currency always shown.
Signed values carry the sign separately from `fmt` (which assumes non-negative): net renders
`− NGN 8,750.50`, zero unsigned.
Seed data unchanged: NGN 1,250.50 out (Lunch, Manual), NGN 8,500.00 out (Ada), NGN 1,000.00 in (Bob);
Review holds USD 50.00 with unknown direction and an NGN item with unknown amount → pool IN 100000,
OUT 975050, net −875050, NGN only. Totals count only Completed accepted records; own Transfer principal
excluded both directions; Pending/Failed and Review excluded. Income forces IN, Expense/Fee force OUT,
Transfer/Refund allow either. Editing keeps the record ID (no duplicate) and preserves cleared optional
fields; `resetAll` restores every seeded state. `show()` always scrolls `phone.scrollTop=0` and focus moves
use `{preventScroll:true}`. Sandbox memory only — no network, no native persistence.

## Public API (regression contract, preserved)
`seed, renderAll, entries, review, editingId, totalsFor(cur,dir), toMinor, openAdd, saveDemo, editRow,
openDetail, openReviewEdit, resolveReview, show, viewCur, addAmt, addDir, addCur, kind, stat, resetAll,
toastSay` — plus DOM ids `phone, sheet, toast, resetAll, rmBtn, rvAmt`.

## Accessibility & motion
Icon-only controls carry `aria-label` + `title`; obvious navigation stays label-complete in the a11y tree.
Native `button/input/select` semantics only; W3C HTML button/keyboard/label guidance followed.
44px minimum hit targets; safe-area inset under the dock; no `vh/svh/dvh/lvh`, no `html/body height:100%`;
no page horizontal overflow at 360–728px. Reduced motion: manual `rm` toggle + `prefers-reduced-motion`;
press scale `.96`, one short screen fade; no count-ups, no loops, no fake live claims.

## Native mapping (actual files, unchanged this round)
- `ui/CashflowApp.kt` — screen shell only: dock → icon-only NavigationBar (no labels), `HomeScreen` /
  `ActivityScreen` map the conceptual Home + feed, badge state, routes
- `ui/FlowScreens.kt` — editor / review / detail screens map to the fullscreen Add entry, Review
  focused-question flow, and receipt sheet
- `ui/theme/CashflowTheme.kt` — charcoal/citron palette (user-liked dark direction), tabular-num typography, dark `colorScheme`
- `ui/components/LedgerRow.kt` — row geometry, avatars, arrow direction cues, status economy
