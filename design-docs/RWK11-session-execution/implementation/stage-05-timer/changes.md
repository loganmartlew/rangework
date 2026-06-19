# Stage 5: Timer & Elapsed Time — Changes

## Summary

Added timestamp-based active time tracking for range session execution. The timer starts when the user enters the execution screen, accumulates elapsed seconds, and pauses when they leave. Elapsed time is displayed live in the progress header.

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Added `elapsedSeconds: Long` and `isTimerRunning: Boolean` to `RangeSessionUiState`. Added `onScreenEnter()` / `onScreenExit()` lifecycle methods, private `startTick()` / `stopTick()` helpers, and `override fun onCleared()` safety net. `onScreenEnter()` fetches prior elapsed time from the repo, records a time entry, then starts a 1-second tick coroutine on `Dispatchers.Default`. `onScreenExit()` stops the tick, updates state, and closes the open time entry. A `currentEnteredAt: Instant?` guard prevents double-entry if `onScreenEnter()` is called twice. |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Added `onScreenEnter: () -> Unit` and `onScreenExit: () -> Unit` parameters. Added a `DisposableEffect(Unit)` that calls `onScreenEnter()` on enter and `onScreenExit()` in `onDispose`. Passed `elapsedSeconds = uiState.elapsedSeconds` to both `RangeSessionProgressHeader` calls (phone and tablet layouts). |
| `androidApp/src/main/java/.../ui/components/RangeSessionProgressHeader.kt` | Added `elapsedSeconds: Long = 0` parameter. Added `formatElapsedTime()` private function (MM:SS for < 60 min, H:MM:SS otherwise). Added `accessibleElapsedDescription()` private function for TalkBack. Added elapsed time `Text` (in `RangeworkMono.medium`, `onSurface` colour) as a third metric in the top metrics row. Updated merged `contentDescription` to include elapsed time. |
| `androidApp/androidApp/build.gradle.kts` | Moved `kotlinx.datetime` from `testImplementation` to `implementation` — required because `RangeSessionViewModel` now uses `Clock.System.now()` and `Instant` in production code. |
| `androidApp/src/main/java/.../ui/RangeworkApp.kt` | Added `onScreenEnter = rangeSessionViewModel::onScreenEnter` and `onScreenExit = rangeSessionViewModel::onScreenExit` to the `RangeSessionScreen` call. |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | Updated `FakeRangeSessionRepo`: added `getElapsedSecondsResult: Long = 0L` constructor param, added `recordedTimeEntries` and `closedTimeEntries` tracking lists, implemented `recordTimeEntry` / `closeTimeEntry` / `getElapsedSeconds` with tracking instead of no-ops. Added 7 new tests: `timerIsNotRunningInitially`, `timerStartsOnScreenEnter`, `recordTimeEntryCalledOnScreenEnter`, `timerStopsOnScreenExit`, `closeTimeEntryCalledOnScreenExit`, `doubleEnterGuardPreventsMultipleTimeEntries`, `closeTimeEntryEnteredAtMatchesRecordedEnteredAt`. |

### Key implementation decisions

- **`DisposableEffect` for lifecycle, `onCleared()` as safety net**: Since the ViewModel is scoped to the back-stack entry, it's destroyed on navigation away. In the normal flow `onDispose` fires first (calling `onScreenExit()`), then `onCleared()` runs but the `currentEnteredAt == null` guard makes it a no-op. For edge cases where `onDispose` doesn't fire (e.g., process death), `onCleared()` attempts a best-effort exit.

- **`currentEnteredAt` set synchronously**: The double-entry guard (`if (currentEnteredAt != null) return`) and the `Instant` capture happen synchronously before launching the coroutine, so rapid successive `onScreenEnter()` calls produce exactly one time entry.

- **`Dispatchers.Default` for tick**: As specified in the plan. The 1-second `delay` is non-blocking and won't starve the main thread. In tests the tick runs on the real Default dispatcher (not the test dispatcher), so tests check `isTimerRunning` and `elapsedSeconds` after `advanceUntilIdle()` — before the 1-second real-time delay fires.

- **`kotlinx.datetime` moved to `implementation`**: It was previously test-only; using `Clock.System.now()` in production ViewModel code requires the library at runtime.

- **Elapsed time as 4th metric in progress header**: Added alongside steps, balls, and completion %. The `SpaceBetween` row now contains 4 items. The time value uses `RangeworkMono.medium` / `onSurface` (neutral, non-highlighted) to distinguish it from the highlighted completion percentage.

- **No `LiveRegion` on elapsed time**: Per the plan, the timer sits inside the merged `RangeSessionProgressHeader` group. TalkBack reads the group on focus, not on every tick update.

## Potential Regressions

| Risk | Assessment |
|---|---|
| `RangeSessionScreen` callers that don't pass `onScreenEnter`/`onScreenExit` | **Fixed** — only one call site in `RangeworkApp.kt`; updated. No default values used (the parameters are required). |
| `RangeSessionProgressHeader` layout overflow on small screens | **Low** — the 4-item `SpaceBetween` row is tight on very narrow screens. The elapsed time is short (e.g., "12:45") and doesn't expand significantly. Monitor on physical device. |
| `kotlinx.datetime` version conflict with `shared` module | **Low** — `shared` already uses `kotlinx.datetime`; the library is in the version catalog. Moving from `testImplementation` to `implementation` in `androidApp` doesn't change the resolved version. |
| Tick running on `Dispatchers.Default` leaving thread-leaked coroutines in tests | **Low** — all timer tests call `onScreenExit()` (directly or verified) which cancels `tickJob`. ViewModel would be GC'd anyway. |
| `onCleared()` + `onDispose` double-close | **Not possible** — `currentEnteredAt` is set to `null` in `onScreenExit()` on first call; the second call returns early. |

## Validation Checklist

- [ ] Timer starts counting from 0:00 when entering a new range session's execution screen for the first time
- [ ] Timer display updates every second while on the execution screen
- [ ] Timer format is MM:SS for times under 60 minutes (e.g., "12:45")
- [ ] Timer format switches to H:MM:SS at 60 minutes (e.g., "1:05:30")
- [ ] Navigating away from the execution screen pauses the timer (display stops incrementing)
- [ ] Navigating back to the execution screen resumes the timer with accumulated time
- [ ] Time entries are written to `range_session_time_entries` (verify via Supabase Studio)
- [ ] `entered_at` is set on entry, `exited_at` is set on exit
- [ ] Multiple enter/exit cycles create multiple time entries, each correctly closed
- [ ] Accumulated time is correct across multiple enter/exit cycles
- [ ] Timer keeps running when the step list drawer is open
- [ ] Timer display uses `RangeworkMono.medium` styling
- [ ] Timer display is visible in the progress header alongside steps, balls, and completion %
- [ ] TalkBack reads elapsed time as part of the progress header group (not announced on every tick)
- [ ] App crash recovery: re-entering after a crash shows approximately correct accumulated time
- [ ] Progress header does not overflow on small-screen devices
- [x] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes
- [x] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest` passes
- [x] `.\gradlew.bat :androidApp:lintDebug` passes
