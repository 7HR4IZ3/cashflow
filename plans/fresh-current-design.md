# Fresh current — confirmed design spec (Option 1)

Status: **Confirmed supersedes** the old paper-ledger palette (cream `#F7F2E8`, bordered boxes-in-boxes, duplicate Home/Activity). Previous comparison brief: [fresh-current-comparison.html](fresh-current-comparison.html). Research: [fresh-current-research.md](fresh-current-research.md). Native app was **not** modified in this round.

**Scope of confirmation:** only the **Fresh current aesthetic** (soft white / mint, open rows, type, colour, motion) is user-confirmed. The navigation map and screen details below are **proposed**, pending the native round.

## Confirmed direction
- Soft white ground, near-black ink, **one vivid mint accent** (#2BDF9C, dark foreground #0B2F22 on mint).
- **Open date rows** on the ground, hairline-separated; cards exist nowhere in a list.
- Simple **Home / Review / Add** experience with one creative mint current-mark (ink tile, mint wave + dot).
- One violet "to check" pill on Home; unclassified-but-clear rows get a quiet chip, never a warning.

## Color
| Token | Value | Use |
|---|---|---|
| bg | #FAFAF8 | ground |
| surface | #FFFFFF | search, sheets, ref cards |
| ink | #17171A | titles, money, near-black |
| ink2 | #55555E | body/lede, row sub-lines |
| ink3 | #6E6E78 (darkened from #8B8B94 for AA on white) | labels, supporting text |
| mint / deep / ink | #2BDF9C / #0E7A55 / #0B2F22 | accent, add button, "in" |
| violet | #5B4BC9 | Review badge/pill only |
| fail | #C23C22 | real destructive actions and errors only (Reset and Reduced motion are neutral) |
| line | rgba(23,23,26,.09) | hairlines |

## Type & spacing
- System stack; primary text **16px** (row leads, money), supporting ≥**13px** (row subs, chips 13px+), hero 33–42px tabular numerals, letter-spacing −0.02/−0.03em.
- Radii: 24 (cards/phone), 16 (inset cards), 12/14 (fields), 999 (chips/pills). Radius is grouping, not borders.
- In-app padding 16px, page section padding 18px (12px ≤420px). **Outermost horizontal padding removed** — document must never exceed 360px scroll width; rows use `grid: auto minmax(0,1fr) auto` and wrap metadata instead of clipping.

## Controls & motion
- All interactive targets ≥44px (chips, ghost, tabs, keypad). Scale .94–.96 on press. Switch/sheet/screen transitions 160–260ms `cubic-bezier(.22,.9,.3,1)`; **no numeric/counting animation** — money changes instantly. `prefers-reduced-motion` + manual toggle kill transitions/animations.

## Three destinations + Add action (proposed)
| Destination | Contains |
|---|---|
| Home | Currency + period as **two compact native selects** (not always-on chip rows) driving totals **and** history together via one matching pool; period **All time / 7 / 30 — all time unrestricted**; search; direction + source filter chips collapsed inside a **Filters disclosure** (active count shown); **concise scope line** ("Completed only · transfers excluded" + filtered-view flag) with the full scope explanation behind an info details; rows **newest first (Today on top)**; "to check" pill |
| Review | Compact cards: tag (what's missing) + amount + one-line reason + collapsible original alert; Edit opens completion sheet; invalid Save keeps the sheet open with an inline reason; Record only when valid; Ignore |
| Add | **Fast entry first**: amount keypad, **compact currency chooser (native select, not a row of chips)**, In/Out (kind snaps direction), Save; **kind, status and who/account/note/editable native date live behind the More details expander**; Save sits **in flow below the keypad** (never overlays the decimal/zero/backspace row); **bottom nav hidden with an explicit Cancel**; editing keeps the editor (kind/status inside the expander) |
| Settings | Untouched this round; stays in the map next to Home/Review; app-bar entry |

The Add **(+)** button floats between bottom tabs and opens a clean draft.

## Field & state retention
- Adding always starts a **fresh draft** (`openAdd` resets); editing a row prefills the form and **saves in place** (tracked `editingId`) — never appends a duplicate.
- Save keeps entered who/account/note/date; date derives the day offset from fixed sandbox anchor **26 Oct 2026**.
- Review edits keep the item until Save is valid; Record creates a **real accepted entry** in the selected currency, then removes the alert.

## Money rules (prototype parity)
- Integer **minor units** everywhere; parse accepts only `\d+(\.\d{1,2})?` (≤2 decimals, no parseFloat rounding). All demo currencies are 2-decimal.
- Totals include **Completed, non-Transfer** only: pending/failed and **own transfers (both legs)** excluded.
- Kind/direction compatibility matches the backend `LedgerStore.save` validation exactly: **Expense/Fee must be OUT, Income must be IN; Transfer and Refund allow BOTH directions** (no forced direction). Incompatible picks show an inline error, never silently recorded; unknown/zero values also inline-error. Transfer is labelled **"Own transfer"** in the UI (internal kind key stays `Transfer`). Own transfers are excluded from totals (both legs), as the prototype summary scope states.
- Currencies never mix: each currency totals and lists separately (USD recorded from Review stays out of NGN).

## Native UI file mapping (shipped app, for the later Kotlin round — untouched now; destinations proposed)
- `ui/CashflowApp.kt` → `HomeScreen` ← Home: merge the existing Home + `ActivityScreen` pair; add currency/period selectors, search, filter chips, one "to check" pill.
- `ui/FlowScreens.kt` → `ReviewScreen` ← Review cards + completion sheet (amount+currency+direction first); reuse the existing manual editor surface as the Add/entry editor (keypad kept, date editable).
- `ui/components/LedgerRow.kt` ← open date-grouped row styling for the merged Home.
- `ui/theme/CashflowTheme.kt` ← adopt the confirmed Fresh current tokens (soft white / mint).
- `SettingsScreen` (in `ui/CashflowApp.kt`) unchanged. CSV export and sources unchanged.

## Prototype-only vs shipped
Prototype (`fresh-current-comparison.html`) is a sandbox: seeded example data only, memory-only edits, **Reset examples** restores ledger/review/drafts/filters, fixed anchor date, status/time bar is mock chrome. The shipped app keeps real capture, persistence, CSV export — these specs describe the **target**, not a finished implementation.

## Links
- Comparison prototype: [fresh-current-comparison.html](fresh-current-comparison.html)
- Research: [fresh-current-research.md](fresh-current-research.md)
- References: copilot.money (+ Transactions/Dashboard help), culturedcode.com/things/features, cash.app, buddy.download; W3C/MDN HTML button semantics via Context7.
