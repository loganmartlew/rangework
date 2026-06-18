# Stage 3: Start & Step Through — Changes

## Summary

Delivered the minimum viable end-to-end session execution flow. A user taps **Start Session** on the session detail screen, a range session is created via `StartRangeSessionUseCase`, and the app navigates to a full-screen execution screen where they can read and step through each practice step using Next/Previous buttons.

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | ViewModel managing range session state and step navigation. Loads the session on init, restores `lastViewedStepIndex`, provides `nextStep()` / `previousStep()` / `navigateToStep()`, persists step changes fire-and-forget via `UpdateLastViewedStepUseCase`. |
| `androidApp/src/main/java/.../ui/screens/RangeSessionScreen.kt` | Full-screen execution composable with its own `Scaffold` (top bar + bottom bar). Handles loading, null-session error, zero-steps error, and the main step content. Sets `FLAG_KEEP_SCREEN_ON` via `DisposableEffect` while in composition. |
| `androidApp/src/main/java/.../ui/components/ExecutionStepCard.kt` | Card displaying the current step: unit context header (title + step/rep position), instruction text, ball count + club row, focus cue via `FocusCard`, and notes. Merges accessibility content description. |
| `androidApp/src/main/java/.../ui/components/StepNavigationBar.kt` | Bottom navigation bar with Previous/Next `FilledTonalButton`s and a centred step counter in `RangeworkMono.small`. Buttons have semantic content descriptions including disabled state. |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | 11 ViewModel unit tests covering: session load, `lastViewedStepIndex` restore, out-of-bounds restore, nextStep, previousStep, clamping at both ends, `navigateToStep`, out-of-bounds clamp, persistence call, not-found error state, null foundation error state. |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/RangeworkNavigation.kt` | Added `RangeSession = "range-sessions/{rangeSessionId}"` and `fun rangeSession(id)` helper to `RangeworkRoutes`. |
| `androidApp/src/main/java/.../ui/RangeworkApp.kt` | Added `RangeSessionIdArg` constant; `LaunchedEffect` on `plannerUiState.startedRangeSessionId` to navigate via `rootNavController`; `composable(RangeworkRoutes.RangeSession)` in the root `NavHost` with a `RangeSessionViewModel` factory scoped to the back stack entry; added `onStartRangeSession` parameter to `AuthenticatedAppShell`; wired callback through to `SessionDetailScreen`. |
| `androidApp/src/main/java/.../ui/screens/SessionDetailScreen.kt` | Added `onStartSession: () -> Unit` parameter (defaults to `{}` for backwards compatibility); computes `isSessionExecutable` and `startSessionDisabledReason`; renders a full-width `Button` with play icon, disabled with explanation text when session has no items or all units have zero instructions. Added semantic content description for accessibility. |
| `androidApp/src/main/java/.../ui/PracticePlannerViewModel.kt` | Added `startedRangeSessionId: String?` to `PracticePlannerUiState`; cleared it on sign-out; added `startRangeSession(sessionId)` which calls `StartRangeSessionUseCase` and sets `startedRangeSessionId` on success or emits a snackbar `Notification` on failure; added `consumeStartedRangeSessionId()` consumed by the `RangeworkApp` `LaunchedEffect` after navigation. |
| `androidApp/src/test/java/.../ui/PracticePlannerViewModelTest.kt` | Updated `FakePlannerRepositories.toDataFoundation()` to include all 13 range session use cases (required since `DataFoundation` added them in Stage 2). Added `StubRangeSessionRepository` private class for no-op compliance. Added necessary imports. |
| `androidApp/src/test/java/.../ui/SettingsViewModelTest.kt` | Same fix: added range session use cases to `fakeDataFoundation()` with an inline anonymous `RangeSessionRepository` stub. Added necessary imports. |

### Key implementation decisions

- **Navigation architecture**: `RangeSession` route is in the **root `NavHost`** alongside `SignIn` and `Authenticated` — not inside `AuthenticatedAppShell`'s inner NavHost. This naturally hides the bottom nav bar and navigation rail without any conditional logic. The back-stack returns to `Authenticated` (shell), which preserves the session detail backstack entry.
- **ViewModel scoping**: `RangeSessionViewModel` is created with `viewModelStoreOwner = backStackEntry`, scoping its lifetime to the back-stack entry. The `DataFoundation` is passed from `RangeworkApp` where `rangeworkFoundation` is available.
- **`startedRangeSessionId` pattern**: Mirrors the existing `savedUnitId` / `savedSessionId` one-shot pattern. `PracticePlannerViewModel` emits the new ID into state; `RangeworkApp` observes and navigates, then consumes the ID.
- **Keep-screen-on**: Implemented via `DisposableEffect` with `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`, which is cleared when the composable leaves composition.
- **Empty-session guard**: Computed in `SessionDetailScreen` using the already-available `unitsById` map — no new ViewModel state needed.

## Potential Regressions

| Risk | Assessment | Mitigation |
|---|---|---|
| Existing navigation routes broken by adding `RangeSession` to root `NavHost` | **Low** — `RangeSession` route uses a unique prefix `range-sessions/` not matched by any existing route logic. Existing routes unchanged. | All existing unit tests pass; no routing logic was modified. |
| `SessionDetailScreen` layout shift from adding "Start Session" button | **Low** — button is added between the briefing strip and session notes, consistent with existing vertical spacing. | Existing tests unaffected (the screen has no layout snapshot tests). |
| `PracticePlannerViewModel` state emission change | **Low** — `startedRangeSessionId` is a new additive field; existing state flows unchanged. `startRangeSession` launches a new coroutine that doesn't interact with the existing `operationMutex` (intentional: start is a separate operation). | Existing `PracticePlannerViewModelTest` passes unchanged. |
| `SettingsViewModelTest` / `PracticePlannerViewModelTest` test compilation | **Fixed** — both files had `toDataFoundation()` constructors that didn't include the 13 range session use cases added in Stage 2. Stage 3 fixed both. | `testDebugUnitTest` and `testReleaseUnitTest` pass. |
| `RangeSessionViewModel` fire-and-forget persistence failure swallowed silently | **By design** — the plan specifies fire-and-forget. A persistence failure for `lastViewedStepIndex` does not affect the user's ability to continue stepping. | Logged in `catch (_: Exception)` block. |

## Validation Checklist

- [ ] "Start Session" button appears on `SessionDetailScreen`
- [ ] "Start Session" is disabled with explanation for sessions with no items
- [ ] "Start Session" is disabled with explanation when all items' units have zero instructions
- [ ] Tapping "Start Session" creates a range session (verify via Supabase Studio)
- [ ] App navigates to the execution screen after starting
- [ ] Execution screen displays the first step with correct instruction text, ball count, club, notes, focus cue
- [ ] Unit context shows correctly: unit title, step position, rep number
- [ ] "Next" button advances to the next step
- [ ] "Previous" button goes back to the previous step
- [ ] "Previous" is disabled on the first step
- [ ] "Next" is disabled on the last step
- [ ] Step counter updates correctly ("Step 1 of 30")
- [ ] Null fields render gracefully: missing club/notes/ball count shows nothing
- [ ] `FLAG_KEEP_SCREEN_ON` is active while on execution screen (screen does not dim)
- [ ] Back button returns to session detail screen
- [ ] Loading indicator shows while fetching range session data
- [ ] Error state shows if range session not found
- [ ] Network error during start shows snackbar, doesn't navigate
- [ ] `lastViewedStepIndex` is persisted on step changes
- [ ] Re-entering the execution screen restores the last viewed step
- [ ] Bottom navigation bar/rail is hidden on execution screen
- [x] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` passes
- [x] `.\gradlew.bat :androidApp:assembleDebug` succeeds
- [x] `.\gradlew.bat :androidApp:lintDebug` passes
