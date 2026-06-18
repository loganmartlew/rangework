# Stage 6: Step Drawer & Tablet Layout

## Objective

Add the step-list drawer for browsing and jumping to any step on phone, and implement the tablet layout with a permanent side panel. The drawer/panel shows all steps grouped by unit, with completion state indicators (if Stage 4 is done) and the currently viewed step highlighted.

**Tickets:** RWK-14, RWK-23

## Dependencies

- **Stage 3** must be complete (execution screen, step navigation, ViewModel exist).
- If **Stage 4** (completion) is done: drawer shows completion checkmarks per step. If not, drawer shows steps without completion indicators — they can be added later.
- Depends on existing responsive layout pattern: `navigationTypeForScreenWidth` in `RangeworkNavigation.kt` (840dp breakpoint).

## Affected Screens

| Screen | Change |
|---|---|
| `RangeSessionScreen` | Wrap in `ModalNavigationDrawer` (phone) or `Row` layout (tablet); add drawer trigger button to top bar |

## Likely Files

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/components/StepListDrawerContent.kt` | Scrollable step list grouped by unit, with completion state and current-step highlight |
| `androidApp/src/main/java/.../ui/components/StepListItem.kt` | Individual step row in the drawer list |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Add `ModalNavigationDrawer` wrapper (phone) or `Row` with permanent panel (tablet); add drawer toggle button; pass responsive layout decision |
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Add `onDrawerStepSelected(index)` if different from `navigateToStep` (likely reuses it) |

## New Components Required

### `StepListDrawerContent`

Scrollable list of all steps, grouped by unit title headers.

**Structure:**
```
Unit 1: Wedge Warmup
  ✓ Step 1 — Half swings to target (Rep 1/3)       ← completed
  → Step 2 — Half swings to target (Rep 2/3)       ← current (highlighted)
    Step 3 — Half swings to target (Rep 3/3)        ← incomplete
    Step 4 — Full swings 80% power (Rep 1/3)
Unit 2: Iron Play
    Step 5 — 7-iron to 150 flag (Rep 1/2)
    ...
```

**Content per step row:**
- Step number (position in the overall step list): `RangeworkMono.small`
- Instruction text (truncated, single line): `bodySmall`
- Rep indicator ("Rep 2/3") if `totalReps > 1`: `labelSmall`
- Completion checkmark (if Stage 4 complete): icon, `secondary` color
- Current-step highlight: background tint using `primaryContainer` / `surfaceVariant`

**Content per unit header:**
- Unit title: `titleSmall` or `labelMedium`, uppercase
- Unit completion status (if Stage 4 complete): "3/5" or progress fraction in `RangeworkMono.small`

**Behavior:**
- Uses `LazyColumn` for performance with large step lists
- Auto-scrolls to the current step when the drawer opens
- Tapping a step: calls `navigateToStep(index)`, closes the drawer (phone only — tablet panel stays open)
- Unit headers are non-tappable section dividers

### `StepListItem`

Individual step row composable for the drawer list.

Props:
- `stepIndex: Int`
- `instructionText: String`
- `repNumber: Int`, `totalReps: Int`
- `isCompleted: Boolean`
- `isCurrent: Boolean`
- `onClick: () -> Unit`

### Phone layout: `ModalNavigationDrawer`

- Wraps the existing execution screen content
- Drawer is opened via a hamburger/list icon button in the top bar
- Drawer is also openable via swipe gesture (Material 3 default behavior)
- Drawer content: `StepListDrawerContent`
- Drawer state managed via `DrawerState` (remember in the composable)

### Tablet layout: permanent side panel

- Detected using `navigationTypeForScreenWidth` or equivalent (≥840dp)
- Layout: `Row { StepListPanel(weight(0.35f)) | StepDetail(weight(0.65f)) }`
- No `ModalNavigationDrawer` on tablet — the panel is always visible
- Panel is the same `StepListDrawerContent` composable
- Tapping a step in the panel updates the right-side detail (no close animation)

### Top bar changes

- Add a list/menu icon button on the left of the top bar (phone only)
- On tap: toggle `DrawerState` open
- On tablet: icon is hidden (panel is always visible)

## Validation Checklist

### Phone layout
- [ ] List/menu icon appears in the execution screen top bar
- [ ] Tapping the icon opens the drawer from the left side
- [ ] Swiping from the left edge opens the drawer
- [ ] Drawer shows all steps grouped by unit title
- [ ] Unit titles are displayed as section headers
- [ ] Each step shows: step number, instruction text (truncated), rep indicator (if applicable)
- [ ] Completed steps show checkmark (if Stage 4 complete)
- [ ] Current step is visually highlighted with a different background
- [ ] Tapping a step closes the drawer and navigates to that step
- [ ] Drawer auto-scrolls to the current step when opened
- [ ] `lastViewedStepIndex` updates when jumping to a step via drawer

### Tablet layout
- [ ] On wide screens (≥840dp), the step list appears as a permanent left panel
- [ ] No drawer gesture or toggle button on tablet
- [ ] Tapping a step in the panel updates the right-side detail immediately
- [ ] Panel and detail area have appropriate width proportions
- [ ] Panel scrolls independently from the detail content

### Performance
- [ ] Large step list (500+ steps: 10 units × 10 reps × 5 instructions) scrolls smoothly
- [ ] `LazyColumn` is used for the step list (not `Column` with `forEach`)
- [ ] Opening/closing the drawer is smooth (60fps)

### Edge cases
- [ ] Very long unit titles truncate with ellipsis in the header
- [ ] Very long instruction text truncates to single line in step rows
- [ ] Sessions with a single unit: no visual issue with only one unit header
- [ ] Sessions with no reps (repeatCount = 1): rep indicator not shown

## Accessibility Requirements

- Drawer toggle button must have content description: "Open step list" / "Close step list"
- Each step in the list must be focusable and announce: "Step 3, Half swings to target, Rep 2 of 3, completed" (or "incomplete")
- Unit headers must be announced as section headers (use `Modifier.semantics { heading() }`)
- Current step must be announced: "Current step" as part of the step's accessibility description
- On tablet, the permanent panel must be navigable via TalkBack independently from the detail pane
- Focus should move to the step detail content after selecting a step in the drawer (phone: after drawer closes)

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `ModalNavigationDrawer` wrapping breaks existing execution screen layout | Medium | The drawer wraps the content composable — test that all existing content (step card, navigation bar, progress header) renders correctly inside the drawer's content lambda. |
| Drawer swipe gesture conflicts with Android back gesture | Medium | Test on devices with gesture navigation. Material 3's `ModalNavigationDrawer` handles this, but verify. Consider disabling the swipe gesture if it's problematic and relying on the button only. |
| Tablet breakpoint (840dp) conflicts with navigation rail breakpoint | Low | Use the same `navigationTypeForScreenWidth` function or the same threshold. The execution screen hides the nav rail anyway — verify the layout logic is consistent. |
| Step list re-renders on every step change (performance) | Low | Use `LazyColumn` with stable keys (step index). Ensure `StepListItem` doesn't recompose unnecessarily. |
| Timer keeps running while drawer is open (desired behavior) | Very Low | The drawer is part of the execution screen — the timer should continue. Verify this doesn't cause issues (it shouldn't). |
| Auto-scroll to current step causes visual jank on drawer open | Low | Use `LazyListState.animateScrollToItem` with a small delay after drawer open animation completes. |
