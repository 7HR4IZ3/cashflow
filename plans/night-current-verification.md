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
- Usability pass: a blank Add form has a visibly disabled Save, and valid input enables it. Active filtering is named on the filter button; clearing filters and queries restores the full feed. Empty-search recovery was exercised.
- Sheet checks passed for inert background content, Tab wrapping, Escape dismissal, and restoring focus to the opening Settings button. Changing currency inside Settings now updates both its closed symbol and the Home view.
- Incoming/outgoing arrows are consistent across summary, feed, Review, and Add; Review item navigation uses separate left/right chevrons. These icon distinctions and the new Settings help disclosure were visually inspected.

The previous white/mint prototype remains available for comparison. The user requested a new direction and icon-only navigation; this charcoal/citron concept is a proposal. No Compose, native, or ledger source changed, so no native builds were run for this pass.

## First-use fixes verification

2026-10-26. Checked the saved HTML after the interrupted implementation was resumed.

- Ran 19 browser regression assertions covering seed totals, isolated sample launch, cancel-edit-then-add, currency inheritance, new and edited record identity, cleared optional fields, View record actions, delete/ignore restoration without duplication, combined filters, matched-total wording, USD separation, precision/overflow rejection, blank edited amounts, unknown currency, reset, and horizontal overflow. All passed.
- Walked manual entry and editing through browser controls, including account/note in the details sheet. The edit heading named the existing record, and saving kept its ID. Home returned with window scroll at zero.
- Checked Add at 360×844 and 390×844. Save remained visible below the keypad, and the page had no horizontal overflow. This desktop browser test does not verify an actual mobile software keyboard.
- Entered `12.345` in Review: Record stayed disabled with a visible precision explanation. Correcting it to `12.34` and choosing a direction cleared the message and enabled Record.
- Ignored a partly edited alert, then restored it through Settings recovery. Its amount draft and direction survived. Repeated restoration did not duplicate it.
- Opened the sample from the intro button, recorded its clear example, edited NGN 2,000.00 to NGN 2,100.00, and exercised Undo. Compared the ledger, Review queue, currency, filters, and dirty state with the baseline; they stayed unchanged. The demo editor is inside a scrollable sheet.
- Rechecked the missing sample entrypoint found during runtime testing. The public button now opens the demo. No browser console errors remained in the verified flow.

Capture setup is explanatory only. Browser source choices grant no permissions, and actual Android/iPhone capture was not tested in this pass. Recovery is in-memory and resets on page reload or Reset examples; it is not native persistence.
