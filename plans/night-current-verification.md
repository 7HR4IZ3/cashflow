# Night current concept verification

2026-10-26. This verifies the standalone HTML example, not the native app.

- Rendered at 360px and 728px inside light and dark host themes. Home, focused Review, and Add were inspected. The document stays within the viewport width.
- The floating dock has no visible destination labels; its controls have accessible names, a visible selected state, and a Review count badge.
- Summary figures remain fully visible. The initial recorded net renders exactly as `− NGN 8,750.50`; its minus sign is separate from the unsigned money formatter.
- Regression checks passed for initial totals, editing without duplication, transfer and pending exclusions, uncertainty blocking, separate currencies, precision and unsafe-integer rejection, reset, reduced motion, and editor focus/scroll behavior.
- Direct Review interactions passed: choosing a direction does not record; explicit Record adds the USD entry; the missing-amount item exposes an input and can be completed; unknown currency blocks recording.
- Selecting a direction leaves it visible before recording. The pencil opens a prefilled editor for a known amount; changing USD 50.00 to USD 51.25 records the edited amount.
- The refined dock was checked at 360px and 728px: three actions in Home/Add/Review order, Add centered within one pixel, and no individual destination borders, background plates, or inset rings. Home has its active dot on initial load. Keyboard focus outlines remain available.
- Settings opens from the Home header. Closed currency pickers show a symbol; their native options retain full names and ISO values. Switching the Home picker to USD updates its symbol and selected currency, and reset restores NGN. Existing ledger and Review checks still pass after these changes.

The previous white/mint prototype remains available for comparison. The user requested a new direction and icon-only navigation; this charcoal/citron concept is a proposal. No Compose, native, or ledger source changed, so no native builds were run for this pass.
