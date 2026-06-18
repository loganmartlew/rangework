# Stage 7: Finish & Abandon

## Objective

Allow users to finish or abandon a range session from the execution screen. Finishing shows a summary with stats (balls, time, completion %). Abandoning soft-deletes the session after confirmation. Both actions close any open time entry and navigate back.

**Tickets:** RWK-18, RWK-23

## Dependencies

- **Stage 4** must be complete (completion data exists for the finish summary — without it, stats are empty/meaningless).
- Depends on Stage 2 use cases: `FinishRangeSessionUseCase`, `AbandonRangeSessionUseCase`, `CloseTimeEntryUseCase`.
- If **Stage 5** (timer) is done, the finish summary includes elapsed time. If not, elapsed time shows "—" and is added when Stage 5 completes.
- Depends on existing patterns: `DeleteConfirmationDialog` as reference for dialog styling.

## Affected Screens

| Screen | Change |
|---|---|
| `RangeSessionScreen` | Add "Finish Session" and "Abandon Session" actions to top bar overflow menu or action area |
| **`FinishSummaryScreen`** (new, or inline dialog) | Summary display after finishing a range session |

## Likely Files

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/components/FinishSummaryContent.kt` | Stats display for the finish summary (balls, time, %) |
| `androidApp/src/main/java/.../ui/components/AbandonConfirmDialog.kt` | Confirmation dialog for abandoning a range session |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Add `finishSession()`, `abandonSession()` methods; add `showAbandonDialog`, `finishSummary` state |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Add action buttons/menu for finish and abandon; render abandon dialog and finish summary |
| `androidApp/src/main/java/.../ui/RangeworkNavigation.kt` | Possibly add finish summary route (if it's a separate screen rather than a dialog) |
| `androidApp/src/main/java/.../ui/RangeworkApp.kt` | Add finish summary route to NavHost (if separate screen) |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | Add tests for finish/abandon flows |

## New Components Required

### `FinishSummaryContent`

Displays the results of a completed range session. Can be rendered as a full screen, a dialog, or a bottom sheet.

**Content:**
- Session name: `headlineMedium`
- "Session Complete" or similar header: `titleLarge`
- Total balls hit: `RangeworkMono.large` in `secondary` color, with "balls" label in `bodyMedium`
- Elapsed time: `RangeworkMono.large`, formatted as MM:SS or H:MM:SS. Shows "—" if timer data not available (Stage 5 not done).
- Completion percentage: `RangeworkMono.large` in `secondary`, e.g., "100%" or "85%"
- Steps completed: "X/Y steps" in `RangeworkMono.medium`
- "Done" or "Back to Overview" button: navigates back to the previous screen

**Layout:** Centered content, clean and celebratory but simple. Follow Material 3 card/surface patterns.

### `AbandonConfirmDialog`

Simple confirmation dialog following the existing `DeleteConfirmationDialog` pattern.

**Content:**
- Title: "Abandon Session?"
- Body: "Are you sure? Progress will be lost."
- Confirm button: "Abandon" (destructive styling — use `error` color)
- Cancel button: "Cancel" (text button)

**Behavior:**
- Confirm → calls `RangeSessionViewModel.confirmAbandon()` → soft-deletes, navigates back
- Cancel → dismisses dialog, returns to execution screen

### ViewModel changes

**New state fields in `RangeSessionUiState`:**
- `showAbandonDialog: Boolean = false` — controls abandon confirmation dialog visibility
- `finishSummary: FinishSummaryData? = null` — populated after finishing, triggers summary display
- `isFinishing: Boolean = false` — loading state during finish network call
- `isAbandoning: Boolean = false` — loading state during abandon network call

**`FinishSummaryData`:**
- `sessionName: String`
- `totalBalls: Int`
- `completedBalls: Int`
- `completionPercentage: Double`
- `completedStepCount: Int`
- `totalStepCount: Int`
- `elapsedSeconds: Long?` (null if Stage 5 not done)

**New methods:**

`finishSession()`:
1. Set `isFinishing = true`
2. Close any open time entry (if Stage 5 done): `CloseTimeEntryUseCase`
3. Call `FinishRangeSessionUseCase(rangeSessionId)` → sets `completed_at`
4. Compute `FinishSummaryData` from the current state (progress helpers + elapsed seconds)
5. Set `finishSummary = data`, `isFinishing = false`
6. The screen transitions to the summary view

`requestAbandon()`:
1. Set `showAbandonDialog = true`

`dismissAbandon()`:
1. Set `showAbandonDialog = false`

`confirmAbandon()`:
1. Set `isAbandoning = true`, `showAbandonDialog = false`
2. Close any open time entry (if Stage 5 done)
3. Call `AbandonRangeSessionUseCase(rangeSessionId)` → sets `abandoned_at`
4. Trigger navigation back (via a one-shot event or navigation callback)

### Action placement

Two approaches (decide during implementation):

**Option A: Overflow menu** — "Finish Session" and "Abandon Session" in the top bar's overflow menu (three-dot icon). "Finish Session" has a visual accent or badge when `isFullyComplete`.

**Option B: Dual-action bar** — "Finish Session" as a prominent button (filled when all complete, outlined otherwise) and "Abandon Session" as a text button or in overflow. This is more discoverable.

Recommendation: Option B — "Finish Session" should be easy to find when the user is done practicing. "Abandon" can be in the overflow menu to prevent accidental taps.

### Navigation after finish/abandon

- **Finish**: Show the `FinishSummaryContent`. From the summary, "Done" button pops the execution screen off the backstack, returning to wherever the user came from (session detail or Overview).
- **Abandon**: Navigate back immediately (pop execution screen). No summary.
- In both cases, the `RangeSessionViewModel` is destroyed (nav-graph scoped), cleaning up timer coroutines.

### Edge cases

- **Finish with 0% completion:** Allowed. The summary shows 0 balls, 0%, 0/Y steps. No special blocking.
- **Finish while a completion toggle is in-flight:** The finish call should use the latest server state. Since `FinishRangeSessionUseCase` only sets `completed_at`, it doesn't conflict with completion writes. The summary data should be computed from the locally held state (which includes the pending toggle).
- **Network failure on finish:** Show error snackbar, keep the user on the execution screen. Don't show summary.
- **Network failure on abandon:** Show error snackbar, dismiss the dialog, keep the user on the execution screen.
- **Back press during finish summary:** Navigate back (same as tapping "Done").

## Validation Checklist

- [ ] "Finish Session" action is accessible from the execution screen
- [ ] "Finish Session" is visually prominent when all steps are complete
- [ ] Tapping "Finish Session" shows loading state briefly, then transitions to summary
- [ ] Summary shows correct session name
- [ ] Summary shows correct balls hit count (completed balls / total balls)
- [ ] Summary shows correct completion percentage
- [ ] Summary shows correct steps count (completed / total)
- [ ] Summary shows correct elapsed time (if Stage 5 done) or "—" (if not)
- [ ] "Done" button on summary navigates back to session detail or Overview (wherever the user came from)
- [ ] Range session has `completed_at` set after finishing (verify in Supabase)
- [ ] "Abandon Session" action is accessible from the execution screen
- [ ] Tapping "Abandon Session" shows confirmation dialog
- [ ] Dialog text reads "Abandon Session?" / "Are you sure? Progress will be lost."
- [ ] Confirming abandon soft-deletes the session (`abandoned_at` set in Supabase)
- [ ] Confirming abandon navigates back to previous screen
- [ ] Cancelling abandon dismisses dialog and returns to execution
- [ ] Any open time entry is closed on both finish and abandon
- [ ] Network failure on finish shows error, stays on execution screen
- [ ] Network failure on abandon shows error, dismisses dialog, stays on execution
- [ ] Finishing with 0% completion works (no crash, shows 0 stats)
- [ ] Back press from execution screen does NOT trigger abandon (just navigates back — the session stays active)
- [ ] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes

## Accessibility Requirements

- "Finish Session" button must have clear content description: "Finish session"
- "Abandon Session" must have content description: "Abandon session"
- Abandon confirmation dialog must be announced by TalkBack: title, body text, and both buttons
- Finish summary stats must be readable: "85 of 120 balls, 12 minutes 30 seconds, 70 percent complete, 25 of 30 steps"
- "Done" button on summary must be focusable and announced
- Destructive "Abandon" confirm button should use `error` color and be the non-default action (Cancel should be the default/first button)
- Dialog must be dismissible via back press (equivalent to Cancel)

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Navigation after finish/abandon breaks backstack | Medium | The execution screen is popped off the backstack. Verify the previous entry (session detail or Overview) is correctly restored with its state. |
| Closing time entry on finish/abandon fails silently | Low | Time entry close is best-effort. If it fails, the entry stays open-ended — handled by elapsed computation on next load. Log the failure but don't block finish/abandon. |
| Adding action buttons/menu to top bar conflicts with existing top bar layout | Low | The execution screen's top bar is new (from Stage 3). Adding actions is straightforward. Test that back button still works alongside new actions. |
| `FinishSummaryContent` renders incorrectly with null elapsed time | Low | Explicitly handle the null case with "—" display. Test with and without Stage 5. |
| Abandon soft-delete doesn't exclude the session from active queries | Low | Active queries filter `WHERE abandoned_at IS NULL`. Verify the abandoned session disappears from active listings. |
| Rapid double-tap on "Finish" or "Abandon" fires the action twice | Medium | Guard with `isFinishing` / `isAbandoning` flags. Disable the button while processing. |
