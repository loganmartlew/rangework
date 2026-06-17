# Stage 18 - Editor viewport and sticky surface stabilization

> Post-delivery audit stage **S18**. Covers redesign-audit-2 findings **#4 and #5**. Source: `08-implementation/redesign-audit-2.md`.

## Objective

Make pinned editor surfaces behave like stable chrome instead of overlapping live form content.

- Docked Save bars no longer overpaint active controls while the user scrolls through editors (audit #4).
- The Session editor sticky total no longer collides with scrolled item content (audit #5).
- Small-phone and IME behavior remain usable.

## Dependencies

- **Upstream:** S16 complete so the authenticated shell and top-bar heights are already final.
- **Downstream:** S19 should build on the stabilized create/editor viewport behavior.

## Affected screens

- Unit editor.
- Session editor.
- Shared `DockedSaveBar`.
- Shared `StickyTotalBar`, if its behavior or placement changes.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionEditorScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/DockedSaveBar.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/StickyTotalBar.kt`

## Implementation steps

### 1. Audit the nested editor scaffolds

Review how the inner editor `Scaffold`s interact with the outer authenticated shell.

- Confirm where `innerPadding` is coming from.
- Confirm whether the editor list has enough effective bottom padding for the docked Save bar.
- Confirm whether the sticky total is living inside the scrolling content when it should really be shell-adjacent.

Prefer one clear pinned-surface model over compensating with scattered magic padding.

### 2. Stabilize the docked Save bar relationship to form content

Adjust the editor viewport so scrolling content never sits underneath the docked Save bar.

- Ensure lower controls remain visible while editing.
- Keep the final "Add ..." action and totals reachable.
- Verify the Save bar still stays pinned and reachable.

If necessary, extract a shared editor-layout pattern so the Unit and Session editors solve this once rather than separately.

### 3. Rework the Session sticky total placement

Move or restructure the sticky total so it cannot overlay session item controls.

Preferred direction:

- treat the total bar as stable pinned chrome above the scrolling item list
- avoid letting live form rows scroll underneath it

Keep the total visible during scrolling, but not at the cost of obscuring fields.

### 4. Recheck keyboard and compact-phone behavior

Run a full compact-phone pass with the keyboard open and scrolled to lower controls.

- Unit editor lower instruction controls
- Session editor lower item fields
- Save bar + keyboard + sticky total interaction

This stage should close the "looks broken while editing" concern before any density tuning.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Unit editor lower controls remain visible while scrolling.
- [ ] Session editor lower controls remain visible while scrolling.
- [ ] Session sticky total no longer overlays item content.
- [ ] Docked Save bars remain pinned and reachable.
- [ ] IME open/closed behavior verified on a compact phone layout.
- [ ] Phone and tablet editor screenshots verified at top, mid-scroll, and bottom.

## Accessibility requirements

- Pinned bars must not hide the only visible label or error state for a control.
- Save remains a standard accessible button with a stable label.
- Sticky total remains readable and does not crowd adjacent controls at larger font scales.

## Regression risks

- Editor layout changes can easily disturb drag/reorder, steppers, and sticky totals together. Re-test full edit flows after any scaffold restructuring.
- Moving the sticky total out of the `LazyColumn` may require revisiting spacing and scroll behavior around the header region.

