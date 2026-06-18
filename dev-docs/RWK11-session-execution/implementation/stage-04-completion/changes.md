# Stage 4: Completion & Progress — Changes

## Summary

Added the ability to complete and uncomplete individual steps during session execution, with live progress metrics displayed in a sticky header. Optimistic UI ensures completion state changes are reflected immediately; a background network write syncs to Supabase, reverting and surfacing an error if the write fails. Completing the current step auto-advances to the next step.

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/components/RangeSessionProgressHeader.kt` | Sticky progress bar showing steps, balls (or "—"), completion percentage, LinearProgressIndicator, and per-unit dot indicators. Computed from `completedStepIndices` + `rangeSession.snapshot`. All values have accessibility content descriptions. |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Added `completedStepIndices: Set<Int>` and `notification: String?` to `RangeSessionUiState`. Added `toggleStepComplete(stepIndex)` with optimistic update, auto-advance, background network call, and revert-on-failure. Added `consumeNotification()`. Updated `loadSession()` to populate `completedStepIndices` from the loaded session. |
| `androidApp/src/main/java/.../ui/components/ExecutionStepCard.kt` | Added `isCompleted: Boolean` and `onToggleComplete: () -> Unit` parameters. Completed state uses `primaryContainer` card background. Button at the bottom: `FilledTonalButton` "Mark Complete" when incomplete; filled `Button` with checkmark + "Completed" when complete. `contentDescription` includes completion state. |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Added `onToggleStepComplete: (Int) -> Unit` and `onConsumeNotification: () -> Unit` parameters. Added `SnackbarHost` + `LaunchedEffect` for `notification`. Added `RangeSessionProgressHeader` as sticky header above the scrollable step content. Passes `isCompleted` and `onToggleComplete` to `ExecutionStepCard`. |
| `androidApp/src/main/java/.../ui/RangeworkApp.kt` | Wired `onToggleStepComplete = rangeSessionViewModel::toggleStepComplete` and `onConsumeNotification = rangeSessionViewModel::consumeNotification` into `RangeSessionScreen`. |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | Updated `FakeRangeSessionRepo` to support `toggleStepComplete` (applies the change to the in-memory session list, supports `shouldFailOnToggle`). Updated `sampleRangeSession` to accept `completedStepIndices`. Added 9 new tests: initial index population, optimistic add, server-response sync, uncomplete, auto-advance on current step, no-advance on non-current step, no-advance past last step, no-advance on uncomplete, revert on failure, `consumeNotification`. |

### Key implementation decisions

- **Optimistic-only `completedStepIndices`**: The optimistic update only modifies `completedStepIndices` in state, not `rangeSession.completedSteps`. The `rangeSession` is only updated on server success. This avoids needing `Clock.System.now()` in Android production code (which would require adding `kotlinx-datetime` to `implementation` dependencies). All progress metrics in `RangeSessionProgressHeader` are computed from `completedStepIndices` directly.
- **Full revert on failure**: The catch block restores `rangeSession`, `completedStepIndices`, and `currentStepIndex` to their pre-toggle values. This means a failed toggle also un-advances the step if auto-advance had occurred.
- **Per-unit dots**: Computed inline in `RangeSessionProgressHeader` by grouping snapshot steps by `unitIndex`. Not-started = `outlineVariant`, partial = `secondary`, fully complete = `primary`.
- **Sticky progress header**: `RangeSessionProgressHeader` is placed above the scrollable `Column` containing `ExecutionStepCard`, making it always visible while the user scrolls through a long step card.
- **Snackbar in execution screen**: `RangeSessionScreen` manages its own `SnackbarHostState` (rather than routing through the app-level snackbar) because it renders outside the `AuthenticatedAppShell`.

## Potential Regressions

| Risk | Assessment | Mitigation |
|---|---|---|
| `ExecutionStepCard` layout shift from adding completion button | **Low** — button is added as a new last element in the card column. Existing content (instruction, ball/club row, focus cue, notes) renders unchanged above it. | All tests pass; existing step card tests are unaffected. |
| `RangeSessionUiState` new fields break deserialization or test initialization | **Low** — fields are additive with sensible defaults (`emptySet()`, `null`). No existing callers construct `RangeSessionUiState` directly. | All existing `RangeSessionViewModelTest` tests pass unchanged. |
| Auto-advance interferes with `navigateToStep` / `lastViewedStepIndex` persistence | **Low** — auto-advance calls `persistLastViewedStep(newStepIndex)` through the same code path as manual navigation. | Verified: `nextStepPersistsLastViewedStep` continues to pass. |
| Rapid toggling causes state corruption | **Low** — each `toggleStepComplete` call captures the state snapshot at call time. Concurrent calls may race, but the last network response wins (consistent with optimistic pattern). | No concurrent coroutine locking is needed for this use case. |
| `RangeSessionProgressHeader` compute errors on empty snapshot | **Low** — all division is guarded (`if (totalSteps == 0) 0f`), all group lookups use `?: emptyList()`. | Covered by zero-step guard in `RangeSessionScreen`. |

## Validation Checklist

- [ ] Complete button appears on the current step in `ExecutionStepCard`
- [ ] Tapping "Mark Complete" marks the step as complete — visual change (primaryContainer bg + "Completed ✓" button) is immediate
- [ ] Tapping "Completed" on a completed step reverts it to incomplete — visual change is immediate
- [ ] Completion state persists to the database (verify via Supabase Studio)
- [ ] Completion state survives app restart (re-entering the execution screen shows correct state)
- [ ] Auto-advance: completing the current step advances to the next step
- [ ] Auto-advance: completing the last step stays on the last step
- [ ] Auto-advance: uncompleting a step does not change the current step
- [ ] Progress header displays correct steps count: "X/Y steps"
- [ ] Progress header displays correct balls count: "X/Y balls"
- [ ] Progress header shows "—" for balls when all steps have null ball count
- [ ] Progress header displays correct completion percentage
- [ ] LinearProgressIndicator reflects completion fraction visually
- [ ] Per-unit dots show correct completion state (not-started, partial, complete) for each unit
- [ ] All progress metrics update immediately when toggling a step
- [ ] Network failure on toggle: shows error snackbar, reverts local state
- [ ] Rapid toggling: multiple quick taps don't cause state corruption
- [x] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes
- [x] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest` passes
- [x] `.\gradlew.bat :androidApp:assembleDebug` succeeds
- [x] `.\gradlew.bat :androidApp:lintDebug` passes
