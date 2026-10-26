# Night current — Cashflow UI direction (concept spec, ≤100 lines)

Status: user confirmed RESET of the Fresh-current direction + icon-only navigation, and confirmed the NEW
day/night direction request. The dark charcoal/citron palette is **proposed, not user-signed-off** yet.
This is a NEW composition, not a recolor. App name stays **Cashflow**. Old `fresh-current-*` files and
all native source are untouched; this concept lives only in `night-current-concept.html` + this spec.

## Palette (dark-native, matte — no cards-on-black, no purple gradients, no glow)
- Background `#101213` (page-level), layered surface `#1A1D1E`
- Ink `#F5F4EE` (warm off-white), muted `#A0A7A6`
- **Citron `#E1F58C`**: the single brand/action accent (primary action, selected surface accents, net when positive)
- Lavender, only for uncertainty (e.g. review badge); red, failure/destructive only
- Prior direction: warm white app `#FAFAF8` + mint accent + labeled full-width nav → replaced wholesale.

## Composition (completely new)
- **Activity-first home.** Compact contextual header (glyph/monogram + currency + period, tiny selects).
  One horizontal In/Out magnitude pair — arrows convey direction — with an understated "Recorded net".
  No giant net hero with a stack of utility labels. The date-grouped, edge-to-edge feed is the centerpiece.
- **Feed rows**: geometric/initial avatars, name + full amount hierarchy, arrows for direction, quiet status
  only when it matters (Unclassified, Pending, Failed). Origin/metadata lives in the detail receipt only.
- **Filter & search** are icon buttons revealing their controls on demand; no always-on text controls.
- **Icon-only floating dock** (not the labeled nav bar): Home, Review tray with numeric badge, central Add
  (citron filled circle), Settings cog. 44–48px targets, `aria-label`+`title` (no visible words),
  `aria-current="page"` on the active destination, selection shown by fill/shape — not color alone.
  Hidden while Add is focused or a review sheet is open.
- **Review = one focused question at a time** with position progress ("1 / 2"). Full amount + currency, or a
  genuine "Amount missing" affordance where the amount editor is shown right away (not hidden behind a gate).
  Currency carries an explicit "Unknown" option — it stays unknown until picked; never silently defaults to NGN.
  In/Out as arrow buttons with tiny In/Out captions + accessible labels (no signing of unknown items).
  Picking a direction never records; an explicit Record check-button (icon-only) records only when valid.
  Ignore is a distinct icon control; the original alert opens from a receipt/document icon. No big warning cards.
- **Add = full-screen calculator.** Huge tabular amount, currency chooser, direction segment with arrow icons,
  48–56px keypad, citron check/save, X cancel, a details icon revealing real optional fields
  (kind/status/account/name/note/date). Add is "write in the record book", never a bank transfer.
- **Detail = coherent receipt sheet** with meaningful edit / delete / close icons; delete proposes confirmation.
- **Settings**: a small honest sheet describing current Settings scope — no invented capture toggles, no dead controls.

## Icon vocabulary (consistent 2px stroke, 24 grid)
Home = simple house · Review/tray = open tray + numeric badge · Add = plus (citron filled)
Settings = cog · Filter = funnel · Search = magnifier · In = arrow pointing up, Out = arrow pointing down
Ignore = minus-circle · Original alert = receipt/document · Save/Record = check · Cancel = X
Backspace = vector erase · Close = chevron/X · Trash = delete, red only at the destructive moment.
On the financial direction question, arrows get tiny "In"/"Out" captions — an arrow alone is
ambiguous on a small touchscreen; no longer explanations there.

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
- `ui/theme/CashflowTheme.kt` — charcoal/citron palette (proposed, not signed off), tabular-num typography, dark `colorScheme`
- `ui/components/LedgerRow.kt` — row geometry, avatars, arrow direction cues, status economy
