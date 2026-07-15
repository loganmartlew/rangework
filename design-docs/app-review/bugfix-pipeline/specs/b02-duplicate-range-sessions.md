# B2 — Double-tap on "Start session" creates duplicate range sessions

Batch: android-ui
Source: ../../potential-bugs.md#b2 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `apps/mobile/androidApp/.../ui/screens/SessionDetailScreen.kt:147`,
> `PracticePlannerViewModel.kt:1057-1078`
>
> `SessionDetailScreen` gates the Start button only on the static `isSessionExecutable`, and
> `PracticePlannerViewModel.startRangeSession` has no in-flight guard. Two quick taps launch two
> `start()` calls → two active range sessions; navigation lands on the second, orphaning the
> first into the Active carousel. The picker path already guards with `isStartingRangeSession`
> (`RangeworkApp.kt:566,757`) — the same pattern just isn't applied here. Note
> `startRangeSessionFromPicker` (line 1439) also lacks a VM-level guard and survives only
> because the UI disables the picker; a VM-level guard would protect both.

## Confirmation method

New test case appended to
`apps/mobile/androidApp/src/test/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModelTest.kt`
(**additions only** — do not modify existing cases):

Call `viewModel.startRangeSession(id)` twice in immediate succession, `advanceUntilIdle()`,
and assert the repository's `start` was invoked exactly **once**. It is currently invoked
twice.

You will need to extend `StubRangeSessionRepository` (bottom of that same test file, line
~1507) — its `start` currently does `error("Not implemented in stub")`. Give it a call
counter and a suspension point (e.g. `delay(...)` under the test dispatcher) so the second
call genuinely lands while the first is in flight; without the suspension the test proves
nothing. `MainDispatcherRule` and `runTest`/`advanceUntilIdle` are already in use throughout
the file — follow that harness.

## Definition of done

- New test passes
- The guard lives in the **ViewModel**, not (only) in Compose — the finding's whole point is
  that the picker path is protected by a UI disable while the VM contract is unprotected
- `startRangeSessionFromPicker` (`PracticePlannerViewModel.kt:~1439`) gets the same guard, per
  the finding's closing note. One in-flight flag covering both entry points is the intent —
  two independent flags would let one tap on each path still double-start
- The existing `isStartingRangeSession` guard at `RangeworkApp.kt:566,757` is left working;
  if the VM guard makes it redundant, say so in the PR body rather than removing it in this PR
- The flag clears on the failure path too — `startRangeSession`'s `catch` sets a
  `PlannerStatus.Notification` (lines 1070-1076); a guard that sticks after a failed start
  would leave the button permanently dead. Add a test case for this
- `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :shared:testDebugUnitTest`
  green; `.\gradlew.bat :androidApp:lintDebug` green
- Scope boundary: `PracticePlannerViewModel.kt`, `SessionDetailScreen.kt`, and
  `PracticePlannerViewModelTest.kt`. No shared-module changes, no repository changes.

## Notes for the fixer

- **Pattern to mirror:** `isStartingRangeSession` at `RangeworkApp.kt:566,757` — the existing,
  working guard for the picker path. Match its naming and shape; the fix is to move that idea
  down into the VM, not to invent a new mechanism.
- `startRangeSession` currently early-returns when `dataFoundation` is null (lines 1058-1063)
  *before* the `viewModelScope.launch` — mind the interaction: the guard must be set and
  cleared around the launched work, and the early return must not leave it set.
- The VM already has a token-based staleness idiom for concurrent refreshes (`operationToken`,
  visible around lines 1044-1052). That solves a *different* problem — last-writer-wins on a
  refresh — and is not what this bug needs. A simple in-flight boolean is the right tool;
  don't over-engineer.
- Surfacing the flag in `PracticePlannerUiState` so `SessionDetailScreen:147` can disable the
  button alongside `isSessionExecutable` is good UI hygiene and worth doing — but the VM guard
  is the fix, and the test must pass with the VM guard alone.
