# Stage 5: Timer & Elapsed Time

## Objective

Track active time spent on the execution screen using timestamp-based time entries stored in `range_session_time_entries`. Display elapsed time live in the progress header. The timer starts when the user enters the execution screen, pauses when they navigate away, and resumes when they return.

**Tickets:** RWK-16, RWK-22

## Dependencies

- **Stage 3** must be complete (execution screen and `RangeSessionViewModel` exist).
- Depends on Stage 2 use cases: `RecordTimeEntryUseCase`, `CloseTimeEntryUseCase`, `GetElapsedSecondsUseCase`.
- Depends on Stage 2 repository methods: `recordTimeEntry`, `closeTimeEntry`, `getElapsedSeconds`.
- If Stage 4 (completion) is already done, the timer integrates into the existing `RangeSessionProgressHeader`. If not, the timer display is added directly to the execution screen and later merged into the progress header.

## Affected Screens

| Screen | Change |
|---|---|
| `RangeSessionScreen` | Timer display (in progress header if Stage 4 complete, or standalone) |
| `RangeSessionProgressHeader` | Add elapsed time field (if Stage 4 complete) |

## Likely Files

### New files

None — all timer logic lives in the ViewModel and existing screen components.

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Add timer lifecycle: record/close time entries on enter/exit, coroutine-based tick, elapsed state |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Wire `DisposableEffect` for lifecycle detection, display elapsed time |
| `androidApp/src/main/java/.../ui/components/RangeSessionProgressHeader.kt` | Add elapsed time display (if Stage 4 complete) |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | Add tests for timer start/stop, elapsed computation |

## New Components Required

### Timer state in `RangeSessionUiState`

New fields:
- `elapsedSeconds: Long = 0` — total accumulated active time
- `isTimerRunning: Boolean = false` — whether the tick coroutine is active

### Timer display

Elapsed time formatted as:
- `MM:SS` for sessions under 60 minutes (e.g., "12:45")
- `H:MM:SS` for sessions ≥ 60 minutes (e.g., "1:05:30")
- Displayed in `RangeworkMono.medium` (numeric value)
- Updates every second while the timer is running
- Shows accumulated time (paused intervals included) — not a wall-clock timer

### ViewModel timer logic

**On init (entering execution screen):**
1. Call `GetElapsedSecondsUseCase(rangeSessionId)` to get previously accumulated time
2. Set `elapsedSeconds` to the returned value
3. Call `RecordTimeEntryUseCase(rangeSessionId, now())` to start a new time entry
4. Start a coroutine that increments `elapsedSeconds` every second
5. Set `isTimerRunning = true`

**On dispose (leaving execution screen):**
1. Stop the tick coroutine
2. Call `CloseTimeEntryUseCase(rangeSessionId, enteredAt, now())` to close the open time entry
3. Set `isTimerRunning = false`

**Tick coroutine:**
- Use `viewModelScope.launch` with `delay(1000)` in a loop
- Increment `elapsedSeconds` by 1 each tick
- Use `Dispatchers.Default` for the delay to avoid main-thread timing issues

**Lifecycle integration:**
- `DisposableEffect` in `RangeSessionScreen` calls ViewModel's `onScreenEnter()` and `onScreenExit()` (via `onDispose`)
- `onScreenEnter()` → record time entry, start tick
- `onScreenExit()` → close time entry, stop tick
- These are separate from init — init handles the first entry, but the screen may be re-entered without recreating the ViewModel (if scoped to a broader nav graph)

**Edge case — ViewModel scoped to nav graph:**
- If the ViewModel is destroyed on navigation away (nav-graph scoped), `onCleared()` is the exit signal → close time entry there
- If the ViewModel survives navigation (activity-scoped fallback), use `DisposableEffect` for enter/exit signals
- The approach depends on the scoping decision made in Stage 3

### Time entry lifecycle

```
Enter screen → INSERT (entered_at = now, exited_at = NULL)
                ↓
User is on screen, timer ticking
                ↓
Leave screen → UPDATE (exited_at = now)
```

If the app crashes before the UPDATE:
- The time entry has `exited_at = NULL` (open-ended)
- On next load, `GetElapsedSecondsUseCase` treats open entries as "still running" up to the range session's `updated_at` timestamp
- This is an accepted approximation — the timer is informational, not billing-critical

### Elapsed time computation (in `GetElapsedSecondsUseCase` / repository)

```
For each time entry:
  if exited_at is not null:
    interval = exited_at - entered_at
  else:
    interval = now() - entered_at  (or cap at range_session.updated_at)
Total = sum of all intervals in seconds
```

## Validation Checklist

- [ ] Timer starts counting from 0:00 when entering a new range session's execution screen for the first time
- [ ] Timer display updates every second while on the execution screen
- [ ] Timer format is MM:SS for times under 60 minutes
- [ ] Timer format switches to H:MM:SS at 60 minutes
- [ ] Navigating away from the execution screen pauses the timer
- [ ] Navigating back to the execution screen resumes the timer with accumulated time
- [ ] Time entries are written to `range_session_time_entries` (verify via Supabase Studio)
- [ ] `entered_at` is set on entry, `exited_at` is set on exit
- [ ] Multiple enter/exit cycles create multiple time entries, each correctly closed
- [ ] Accumulated time is correct across multiple enter/exit cycles
- [ ] Timer keeps running when the drawer is open (drawer is part of the execution screen)
- [ ] Timer display uses `RangeworkMono.medium` styling
- [ ] App crash recovery: re-entering after a crash shows approximately correct accumulated time
- [ ] Timer does not drift significantly over a 30-minute session (stays within 1-2 seconds of wall clock)
- [ ] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest` passes

## Accessibility Requirements

- Elapsed time must be announced by TalkBack: "Elapsed time: 12 minutes 45 seconds"
- Timer updates should not be announced on every tick (noisy). Only announce on explicit focus or when the progress header is read as a group.
- Use `LiveRegion.Polite` if the timer is in a standalone display — but avoid if it's inside the progress header (too frequent)
- The time display must be large enough to read at arm's length (the user's phone may be on a shelf at the range)

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `DisposableEffect` lifecycle doesn't align with ViewModel lifecycle | High | This is the primary technical risk. Test thoroughly: navigate away → back, app background → foreground, rotate device, back-press from execution. Verify time entries are correctly opened/closed in each case. |
| Timer coroutine leaks if not properly cancelled | Medium | Use `viewModelScope` — it auto-cancels on `onCleared()`. Additionally, cancel the tick job explicitly in `onScreenExit()`. |
| `onCleared()` not called on app kill (process death) | Medium | Accepted tradeoff. Open-ended time entry is handled by the elapsed computation logic. Verify the approximation is reasonable. |
| Network failure on time entry write | Low | Time entry writes are fire-and-forget. If the INSERT fails, the timer still ticks locally. The accumulated time may be slightly off on next load — acceptable for an informational metric. |
| Timer interferes with `keepScreenOn` or step navigation | Very Low | Timer is independent of navigation logic. The tick coroutine only updates `elapsedSeconds` state. |
| Adding elapsed time to `RangeSessionProgressHeader` shifts layout | Low | Test on small screens. The elapsed time adds one more metric — ensure the header layout handles 4-5 metrics without overflow. |
