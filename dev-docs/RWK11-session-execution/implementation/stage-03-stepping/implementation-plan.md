# Stage 3: Start & Step Through

## Objective

Deliver the minimum viable end-to-end flow. A user taps "Start Session" on a practice session detail screen, a range session is created via the RPC, and they land on the execution screen where they can navigate through steps using next/previous buttons. No completion toggling, no timer, no drawer — just start and read through.

**Tickets:** RWK-13, RWK-14, RWK-24

## Dependencies

- **Stage 2** must be complete (shared models, repository, use cases, DataFoundation wiring all exist).
- Depends on existing Android patterns: ViewModel factory, Compose Navigation, `RangeworkApp.kt` shell, `PracticePlannerViewModel`, `SessionDetailScreen`.

## Affected Screens

| Screen | Change |
|---|---|
| `SessionDetailScreen` | Add "Start Session" button with empty-session guard |
| **`RangeSessionScreen`** (new) | New execution screen displaying the current step with next/prev navigation |
| `RangeworkApp` | Add new route to NavHost, wire ViewModel factory |

## Likely Files

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | ViewModel managing range session state, step navigation |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Execution screen composable |
| `androidApp/src/main/java/.../ui/components/ExecutionStepCard.kt` | Displays current step: instruction, ball count, club, notes, focus cue, unit context |
| `androidApp/src/main/java/.../ui/components/StepNavigationBar.kt` | Next/Previous buttons with step counter |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | ViewModel unit tests |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeworkNavigation.kt` | Add `RangeSession = "range-sessions/{rangeSessionId}"` route and `fun rangeSession(id)` helper |
| `androidApp/src/main/java/.../ui/RangeworkApp.kt` | Add `composable(RangeworkRoutes.RangeSession)` to NavHost; create `RangeSessionViewModel` via factory |
| `androidApp/src/main/java/.../ui/screens/SessionDetailScreen.kt` | Add "Start Session" button; pass start callback through |
| `androidApp/src/main/java/.../ui/PracticePlannerViewModel.kt` | Add `startRangeSession(sessionId)` method that calls `StartRangeSessionUseCase` and returns the new ID |

## New Components Required

### `RangeSessionViewModel`

- Scoped to the execution screen's nav graph (created on entry, destroyed on leave)
- Constructor receives: `rangeSessionId: String`, all relevant use cases from `DataFoundation`
- Factory pattern matching `PracticePlannerViewModel.Factory`

**State:**

```
RangeSessionUiState(
    rangeSession: RangeSession? = null,
    currentStepIndex: Int = 0,
    isLoading: Boolean = true,
    statusMessage: String? = null,
)
```

**Behavior:**
- On init: call `GetRangeSessionUseCase(rangeSessionId)`, populate state, restore `lastViewedStepIndex` if non-null
- `nextStep()`: increment `currentStepIndex` (clamped to max), persist `lastViewedStepIndex`
- `previousStep()`: decrement `currentStepIndex` (clamped to 0), persist `lastViewedStepIndex`
- `navigateToStep(index)`: set `currentStepIndex` directly, persist `lastViewedStepIndex`
- `lastViewedStepIndex` persistence is fire-and-forget via `UpdateLastViewedStepUseCase`

### `RangeSessionScreen`

- Receives `uiState: RangeSessionUiState` and callback lambdas from ViewModel
- Layout:
  - Top bar with session name and back navigation
  - `ExecutionStepCard` for current step content
  - `StepNavigationBar` at the bottom
- Applies `Modifier.keepScreenOn()` at the root composable
- Shows loading indicator while `isLoading` is true
- Shows error state if `rangeSession` is null after loading

### `ExecutionStepCard`

Displays the current step's full details:
- **Instruction text** — primary content, `bodyLarge`
- **Ball count** — `RangeworkMono.medium` (numeric value), with label in `bodyMedium`
- **Club display name** — `bodyMedium`
- **Notes** — `bodySmall` in `onSurfaceVariant`
- **Focus cue** — `bodyMedium`, potentially in a `FocusCard` or similar accent container
- **Unit context header** — "Unit Title — Step X of Y, Rep N of M" in `titleMedium` / `labelMedium`

### `StepNavigationBar`

- Row with Previous and Next buttons
- Previous disabled when `currentStepIndex == 0`
- Next disabled when `currentStepIndex == totalSteps - 1`
- Center: step indicator "Step X of Y" in `RangeworkMono.small`
- Buttons use `labelLarge` text

### Navigation changes

Add to `RangeworkRoutes`:
```
const val RangeSession = "range-sessions/{rangeSessionId}"
fun rangeSession(rangeSessionId: String): String = "range-sessions/$rangeSessionId"
```

The route is a top-level destination outside the bottom bar/rail nav graph (execution screen hides the bottom navigation). Consider a nested nav graph for ViewModel scoping.

### SessionDetailScreen changes

- Add a "Start Session" action button (e.g., filled button or FAB)
- Button is disabled with explanatory text when the practice session has zero units or all units have zero instructions
- On tap: calls a callback that triggers `PracticePlannerViewModel.startRangeSession(sessionId)`, which returns the new range session ID, then navigates to `range-sessions/{newId}`
- Error handling: if the RPC fails (network error, empty session rejection), show a snackbar with the error message and don't navigate

### `PracticePlannerViewModel` changes

Add:
- `startRangeSession(sessionId: String): String?` — calls `StartRangeSessionUseCase`, returns the new range session ID on success, null on failure. Sets a status message on failure.

## Validation Checklist

- [ ] "Start Session" button appears on `SessionDetailScreen`
- [ ] "Start Session" is disabled with explanation for empty practice sessions (zero units or zero instructions)
- [ ] Tapping "Start Session" creates a range session (verify via Supabase Studio or query)
- [ ] App navigates to the execution screen after starting
- [ ] Execution screen displays the first step with correct instruction text, ball count, club, notes, focus cue
- [ ] Unit context shows correctly: unit title, step position, rep number
- [ ] "Next" button advances to the next step
- [ ] "Previous" button goes back to the previous step
- [ ] "Previous" is disabled on the first step
- [ ] "Next" is disabled on the last step
- [ ] Step counter updates correctly ("Step 1 of 30")
- [ ] Steps are in correct order: units by session order, instructions by sort order, reps 1..N
- [ ] Null fields render gracefully: missing club shows nothing (not "null"), missing notes shows nothing, missing ball count shows nothing
- [ ] `Modifier.keepScreenOn()` is active (screen does not dim/sleep while on execution screen)
- [ ] Back button returns to session detail screen
- [ ] Loading state shows while fetching range session data
- [ ] Error state shows if range session not found
- [ ] Network error during start shows snackbar, doesn't navigate
- [ ] `lastViewedStepIndex` is persisted on step changes
- [ ] Re-entering the execution screen restores the last viewed step
- [ ] Bottom navigation bar/rail is hidden on execution screen
- [ ] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes
- [ ] `.\gradlew.bat :androidApp:assembleDebug` succeeds
- [ ] `.\gradlew.bat :androidApp:lintDebug` passes

## Accessibility Requirements

- `ExecutionStepCard` content must be readable by TalkBack: instruction text, ball count, club, notes, focus cue should have logical reading order
- Step navigation buttons must have content descriptions: "Previous step", "Next step"
- Step counter must be announced: "Step 3 of 20"
- Disabled buttons must announce their disabled state ("Previous step, disabled")
- "Start Session" button on session detail must have clear content description, including disabled reason when applicable
- Touch targets for Next/Previous must meet 48dp minimum

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `RangeworkApp.kt` NavHost changes break existing navigation | Medium | Add the new route without modifying existing routes. Test all existing navigation paths after adding. |
| ViewModel scoping to nav graph doesn't work as expected | Medium | Prototype early. If nested graph scoping fails, fall back to passing `rangeSessionId` as a SavedStateHandle argument and using `viewModel()` with the backstack entry scope. |
| `SessionDetailScreen` layout changes shift existing content | Low | The "Start Session" button is an addition — use existing spacing/layout patterns. Test existing session detail rendering. |
| `PracticePlannerViewModel` modification breaks existing state flows | Low | Adding a new method doesn't affect existing `StateFlow` emissions. Test existing planner flows. |
| `Modifier.keepScreenOn()` leaks beyond execution screen | Low | The modifier is scoped to the composable's composition — when the user navigates away, the composable leaves composition and the flag is cleared. Verify on device. |
| Back navigation from execution screen doesn't restore session detail scroll position | Low | Standard Compose Navigation behavior preserves backstack entries. Verify scroll position is maintained. |
