# Stage 4: Completion & Progress

## Objective

Add the ability to complete/uncomplete individual steps and display live progress metrics on the execution screen. This is the core interactive value — the user can now actively work through their practice at the range, marking steps done and seeing their progress update in real time.

**Tickets:** RWK-15, RWK-16, RWK-22, RWK-23

## Dependencies

- **Stage 3** must be complete (execution screen exists, step navigation works, `RangeSessionViewModel` exists).
- Depends on Stage 2 progress computation helpers (`completedStepCount`, `totalStepCount`, `completionPercentage`, `completedBalls`, `totalBalls`, `completedUnits`, `isFullyComplete`).
- Depends on Stage 2 use cases: `ToggleStepCompleteUseCase`.

## Affected Screens

| Screen | Change |
|---|---|
| `RangeSessionScreen` | Add completion toggle on current step, add progress header |
| `ExecutionStepCard` | Add complete/uncomplete toggle control |

## Likely Files

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/components/RangeSessionProgressHeader.kt` | Progress stats bar: steps, balls, %, per-unit indicators |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Add `toggleStepComplete(stepIndex)`, add progress-derived state, add auto-advance logic |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Integrate progress header, pass completion callback to step card |
| `androidApp/src/main/java/.../ui/components/ExecutionStepCard.kt` | Add completion toggle (button/checkbox), visual distinction for completed steps |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | Add tests for completion toggling, auto-advance, progress computation |

## New Components Required

### `RangeSessionProgressHeader`

A compact stats bar displayed at the top of the execution screen, always visible without losing the current step position.

**Content:**
- Steps completed vs. total: "12/30 steps" — count in `RangeworkMono.medium`, label in `bodySmall`
- Balls hit vs. total: "45/120 balls" — same styling. Shows "—" if all steps have null ball count.
- Completion percentage: "40%" — `RangeworkMono.medium` in `secondary` color
- Per-unit completion indicators: small dots or chips showing which units are fully complete, partially complete, or not started
- Progress bar: `LinearProgressIndicator` showing overall completion fraction

**Layout:** Horizontal row or compact grid. Must fit comfortably in ~56dp height. Scrolls with content or stays fixed — fixed (sticky) is preferred for always-visible progress.

### Completion toggle on `ExecutionStepCard`

- Prominent "Complete" button when step is incomplete → toggles to "Completed" (with checkmark) when done
- Visual distinction: completed steps show muted styling, checkmark indicator, different background tint
- Un-completing: tap the "Completed" button again to revert
- Button is always available regardless of step position (out-of-order completion allowed)

### ViewModel changes

**New state fields in `RangeSessionUiState`:**
- `completedStepIndices: Set<Int>` — derived from `rangeSession.completedSteps` for fast lookup
- Progress metrics (derived, not stored): `completedStepCount`, `totalStepCount`, `completionPercentage`, `completedBalls`, `totalBalls`, `completedUnits`, `isFullyComplete`

**New methods:**
- `toggleStepComplete(stepIndex: Int)`:
  1. Determine current completion state from `completedStepIndices`
  2. Call `ToggleStepCompleteUseCase(rangeSessionId, stepIndex, !isCurrentlyComplete)`
  3. Update local state with returned `RangeSession`
  4. If completing (not uncompleting) the current step: auto-advance to the next step by index (if not at the end)
  5. Recompute all progress metrics

**Auto-advance behavior:**
- On completing the current step → advance `currentStepIndex` by 1 (next sequential step, even if it's already complete)
- On completing a step that isn't the current one (via drawer in a future stage) → don't change `currentStepIndex`
- On uncompleting → don't change `currentStepIndex`
- When at the last step and completing it → stay on the last step (don't wrap)

**Optimistic UI consideration:**
- Toggle completion immediately in local state for responsive UX
- Fire the network write in background
- If the network write fails, revert local state and show error snackbar
- This pattern prevents sluggish feel from network round-trips on every tap

## Validation Checklist

- [ ] Complete button appears on each step in `ExecutionStepCard`
- [ ] Tapping "Complete" marks the step as complete — visual change is immediate
- [ ] Tapping "Completed" on a completed step reverts it to incomplete
- [ ] Completion state persists to the database (verify via Supabase Studio)
- [ ] Completion state survives app restart (re-entering the execution screen shows correct completion state)
- [ ] Auto-advance: completing the current step advances to the next step
- [ ] Auto-advance: completing the last step stays on the last step
- [ ] Auto-advance: uncompleting a step does not change the current step
- [ ] Progress header displays correct steps count: "X/Y steps"
- [ ] Progress header displays correct balls count: "X/Y balls"
- [ ] Progress header shows "—" for balls when all steps have null ball count
- [ ] Progress header displays correct completion percentage
- [ ] Progress bar reflects completion fraction visually
- [ ] Per-unit indicators show correct completion state for each unit
- [ ] All progress metrics update immediately when toggling a step
- [ ] Completing all steps shows 100% — "Finish Session" button becomes visually prominent (placeholder for Stage 7)
- [ ] Mixed ball count scenario: steps with null ball counts are completable but excluded from ball totals
- [ ] Network failure on toggle: shows error, reverts local state
- [ ] Rapid toggling: multiple quick taps don't cause state corruption
- [ ] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest` passes

## Accessibility Requirements

- Completion toggle button must have content description: "Mark step complete" / "Mark step incomplete"
- State change must be announced by TalkBack: "Step 3 completed" / "Step 3 incomplete"
- Progress header values must be readable: "12 of 30 steps completed, 45 of 120 balls, 40 percent"
- Progress bar must have `semantics { contentDescription = "Session progress: 40%" }` or equivalent
- Per-unit indicators must have content descriptions: "Unit 1: Wedge Warmup, 3 of 5 steps complete"
- Color is not the only indicator of completion state — use checkmark icon in addition to color change
- Completion toggle touch target must be at least 48dp

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `ExecutionStepCard` layout changes break step display from Stage 3 | Medium | Add completion toggle as an additive element. Test that existing content (instruction, ball count, club, notes) still renders correctly. |
| Auto-advance interferes with `lastViewedStepIndex` persistence | Low | Auto-advance calls the same `navigateToStep` logic, which persists. Verify. |
| Progress header takes too much vertical space on small screens | Medium | Keep header compact (~56dp). Test on small-screen emulator (360×640). Consider collapsible header if needed. |
| Optimistic UI state gets out of sync with server state | Low | On re-enter (resume), always re-fetch from server. The optimistic pattern only affects the current viewing session. |
| `RangeSessionViewModel` state changes break step navigation from Stage 3 | Low | New state fields are additive. Existing `currentStepIndex`, `nextStep()`, `previousStep()` logic is unchanged. |
