# Stage 6 Changes: Step Drawer & Tablet Layout

## Summary of Changes

### New files

**`androidApp/.../ui/components/StepListItem.kt`**
- Individual step row composable for the drawer/panel list
- Props: `stepIndex`, `instructionText`, `repNumber`, `totalReps`, `isCompleted`, `isCurrent`, `onClick`
- `isCurrent` → `primaryContainer` background; completed steps show a 16dp check icon in `secondary` color
- Step number in `RangeworkMono.small`; instruction text in `bodySmall` (single line, ellipsis); rep indicator in `labelSmall`
- Accessibility: `semantics(mergeDescendants = true)` with a composed `contentDescription` ("Step N, instruction, Rep X of Y, current step, completed/incomplete")

**`androidApp/.../ui/components/StepListDrawerContent.kt`**
- Scrollable step list grouped by unit, backed by `LazyColumn` with stable keys
- Pre-computes a flat `List<DrawerRow>` (sealed: `UnitHeader` | `StepItem`) inside `remember(steps, completedStepIndices)` to avoid recomputation on every recomposition
- Unit completion fraction per-unit computed in O(n) using a pre-built `unitToStepIndices` map
- **`UnitHeaderRow`**: unit title in `labelMedium` uppercase; per-unit completion fraction `N/M` in `RangeworkMono.small`; `semantics(mergeDescendants = true) { heading() }` for TalkBack section heading
- **Auto-scroll formula**: `lazyIndex = currentStepIndex + steps[currentStepIndex].unitIndex + 1` — accounts for one unit-header row inserted before each unit's step block
- Accepts external `LazyListState` so callers control scroll position and auto-scroll timing

### Modified files

**`androidApp/.../ui/screens/RangeSessionScreen.kt`**
- Added `onNavigateToStep: (Int) -> Unit` parameter (wired to `RangeSessionViewModel.navigateToStep`)
- Split into three composables:
  - **`RangeSessionScreen`** (public): sets up keep-screen-on effect, snackbar state, screen-width detection (`configuration.screenWidthDp >= 840`), then branches to phone or tablet layout
  - **`PhoneRangeSessionLayout`** (private): wraps Scaffold in `ModalNavigationDrawer`; drawer opens via a `Menu` icon button in `TopAppBar` actions (hidden during loading or when no steps); `LaunchedEffect(drawerState.currentValue)` auto-scrolls to current step on drawer open; tapping a step calls `onNavigateToStep` then `drawerState.close()`
  - **`TabletRangeSessionLayout`** (private): Scaffold with `Row { StepListDrawerContent(weight=0.35) | HorizontalDivider | step-detail-column(weight=0.65) }` in the content area; no drawer, no menu icon; loading/error/empty states rendered full-width outside the Row
- Step card content (progress header + scrollable `ExecutionStepCard`) is identical in both layouts

**`androidApp/.../ui/RangeworkApp.kt`**
- Added `onNavigateToStep = rangeSessionViewModel::navigateToStep` to the `RangeSessionScreen` call site

## Identified Regressions

| Area | Risk | Notes |
|---|---|---|
| Existing phone layout | Low | Phone layout is structurally unchanged (same Scaffold + content); only wrapped in `ModalNavigationDrawer` and a menu icon added to actions |
| `ModalNavigationDrawer` swipe + Android back gesture | Medium | Material 3 handles this by default; test on gesture-nav devices |
| `HorizontalDivider` used as vertical divider on tablet | Very Low | Same pattern as `RangeworkApp.kt`'s nav rail divider — already proven in the codebase |
| `drawerState.currentValue` key in `LaunchedEffect` | Low | Reads compose state correctly; triggers on settled Open/Closed transitions only |
| `remember(steps, completedStepIndices)` key equality | Very Low | Both are structural-equality types (`List<SnapshotStep>`, `Set<Int>`); recomputes only on content change |

## Validation Checklist

### Phone layout
- [ ] Menu (hamburger) icon appears in execution screen top bar when steps are loaded
- [ ] Menu icon is hidden while loading and when session has no steps
- [ ] Tapping the icon opens the drawer from the left
- [ ] Swiping from the left edge opens the drawer (Material 3 gesture default)
- [ ] Drawer shows all steps grouped by unit title section headers
- [ ] Unit headers show `N/M` completion fraction
- [ ] Each step row shows: step number, truncated instruction (1 line), rep indicator if `totalReps > 1`
- [ ] Completed steps show a check icon in `secondary` color
- [ ] Current step row has `primaryContainer` background
- [ ] Tapping a step closes the drawer and navigates to that step
- [ ] Drawer auto-scrolls to current step when it opens (after animation settles)
- [ ] `lastViewedStepIndex` updates when jumping via drawer (via `navigateToStep`)
- [ ] Back button still works correctly from execution screen
- [ ] Loading/error/empty states display correctly (drawer cannot be opened in these states)
- [ ] Timer continues running while drawer is open (drawer is part of the composable, not a separate screen)

### Tablet layout (≥840dp)
- [ ] No drawer icon or gesture present on tablet
- [ ] Step list appears as a permanent left panel (≈35% width)
- [ ] Right side shows progress header + scrollable step card (≈65% width)
- [ ] Tapping a step in the panel updates the right detail immediately without closing anything
- [ ] Panel and detail column scroll independently
- [ ] Loading/error/empty states span the full content width (no empty panel shown)

### Accessibility
- [ ] Drawer open button: `contentDescription = "Open step list"`
- [ ] Each step row: TalkBack announces "Step N, [instruction], Rep X of Y, current step, completed/incomplete"
- [ ] Unit headers announced as headings by TalkBack
- [ ] Current step announced as "current step"
- [ ] Touch targets for step rows meet 48dp minimum (rows are 10dp vertical padding × 2 + text height ≈ satisfies this)

### Performance
- [ ] Large step list (500+ steps) scrolls smoothly (LazyColumn used, not Column)
- [ ] `StepListDrawerContent` rows are keyed (`"header_N"` / `"step_N"`) — stable recomposition
- [ ] Drawer open/close animation is smooth

### Edge cases
- [ ] Very long unit titles truncate with ellipsis in header row
- [ ] Very long instruction text truncates to one line in step rows
- [ ] Sessions with one unit: single header shown, no visual anomaly
- [ ] Sessions where `totalReps == 1`: no rep indicator shown

### Build verification
- [x] `:androidApp:testDebugUnitTest` passes
- [x] `:androidApp:testReleaseUnitTest` passes
- [x] `:shared:testDebugUnitTest` passes
- [x] `:shared:testReleaseUnitTest` passes
- [x] `:androidApp:assembleDebug` succeeds
- [x] `:androidApp:lintDebug` passes
