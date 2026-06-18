# Stage 8: Resume, Active Sessions & Polish

## Objective

Tie together all remaining features: surface active range sessions on the Overview screen, enable resume, implement club override, add the Overview FAB session picker, show range session history on practice session detail, add template deletion warnings, and validate empty session guards across all entry points.

**Tickets:** RWK-13, RWK-15, RWK-17, RWK-24

## Dependencies

- **Stage 4** (completion) and **Stage 7** (finish & abandon) must be complete — active session resolution depends on completion state and finish/abandon lifecycle.
- Depends on Stage 2 use cases: `ListActiveRangeSessionsUseCase`, `ListCompletedRangeSessionsUseCase`, `OverrideStepClubUseCase`, `StartRangeSessionUseCase`, `HasActiveRangeSessionsUseCase`.
- Depends on existing `PracticePlannerViewModel` and `OverviewScreen`.
- Depends on existing `SessionDetailScreen` for history section.
- Depends on existing club catalog/enabled clubs data from `SettingsViewModel` or `PracticePlannerViewModel`.

## Affected Screens

| Screen | Change |
|---|---|
| `OverviewScreen` | Add active range sessions carousel, add start-session FAB |
| `SessionDetailScreen` | Add range session history section |
| `RangeSessionScreen` | Add club override picker on step card |
| `RangeworkApp` | Wire FAB action and active session navigation |

## Likely Files

### New files

| File | Purpose |
|---|---|
| `androidApp/src/main/java/.../ui/components/ActiveRangeSessionCard.kt` | Summary card for Overview carousel (name, progress bar, started date) |
| `androidApp/src/main/java/.../ui/components/RangeSessionHistoryItem.kt` | Row for completed range session in session detail history |
| `androidApp/src/main/java/.../ui/components/SessionPickerDialog.kt` | Dialog/bottom sheet listing practice sessions for quick-start from FAB |

### Modified files

| File | Change |
|---|---|
| `androidApp/src/main/java/.../ui/PracticePlannerViewModel.kt` | Add `activeRangeSessions`, `completedRangeSessionHistory` to state; add loading logic; add `startRangeSessionFromPicker` method; add `hasActiveSessionsForTemplate` for deletion warning |
| `androidApp/src/main/java/.../ui/screens/OverviewScreen.kt` | Add active sessions carousel, add FAB with session picker |
| `androidApp/src/main/java/.../ui/screens/SessionDetailScreen.kt` | Add range session history section below existing content |
| `androidApp/src/main/java/.../ui/RangeSessionViewModel.kt` | Add `overrideStepClub(stepIndex, clubCode)` method |
| `androidApp/src/main/java/.../ui/components/ExecutionStepCard.kt` | Add club picker/override control |
| `androidApp/src/main/java/.../ui/RangeworkApp.kt` | Wire FAB navigation, active session card navigation |
| `androidApp/src/test/java/.../ui/PracticePlannerViewModelTest.kt` | Add tests for active sessions loading, history loading, deletion warning |
| `androidApp/src/test/java/.../ui/RangeSessionViewModelTest.kt` | Add tests for club override |

## New Components Required

### `ActiveRangeSessionCard`

Compact card for the Overview carousel showing a single active range session.

**Content:**
- Session name: `titleMedium`
- Progress bar: `LinearProgressIndicator` showing completion fraction
- Progress text: "12/30" in `RangeworkMono.small`
- Started date/time: `bodySmall` in `onSurfaceVariant`, formatted relative or absolute (e.g., "Today, 2:30 PM")

**Behavior:**
- Tapping the card navigates to `range-sessions/{rangeSessionId}` (resume)
- Card uses `ElevatedCard` or `OutlinedCard` with appropriate elevation

**Layout:** Fixed width suitable for horizontal scrolling (e.g., 200-240dp wide). Height determined by content.

### Active sessions carousel on Overview

- Horizontally scrollable `LazyRow` of `ActiveRangeSessionCard`s
- Positioned above the existing Overview content (below the top bar)
- Hidden entirely when there are no active range sessions (no empty state, no placeholder)
- Section header: "Active Sessions" in `labelMedium`, uppercase, `onSurfaceVariant`

### `SessionPickerDialog`

Dialog or bottom sheet listing all runnable practice sessions for quick-start.

**Content:**
- Title: "Start Session"
- List of practice sessions, each showing:
  - Session name: `bodyLarge`
  - Unit count and instruction count summary: `bodySmall` in `onSurfaceVariant`
- Empty state: "No sessions available. Create a practice session first." with `bodyMedium`
- Cancel / dismiss action

**Filtering:**
- Only show practice sessions with at least one unit containing at least one instruction (runnable sessions)
- Show all runnable sessions regardless of whether they already have active range sessions (multiple allowed)

**Behavior:**
- Tapping a session: calls `StartRangeSessionUseCase`, navigates to `range-sessions/{newId}`, dismisses dialog
- Show loading indicator while starting
- Show error snackbar if start fails

### FAB on Overview

- Positioned as the main FAB on the Overview screen
- Icon: play/start icon (or the existing add icon repurposed)
- On tap: opens `SessionPickerDialog`
- If no practice sessions exist or none are runnable: FAB still visible but the picker shows empty state

### `RangeSessionHistoryItem`

Row composable for the session detail history section.

**Content:**
- Date: formatted start date, `bodyMedium`
- Elapsed time: `RangeworkMono.small`
- Completion percentage: `RangeworkMono.small` in `secondary`
- Steps completed: "X/Y" in `RangeworkMono.small`

**Layout:** Horizontal row with date on the left, metrics on the right. Similar density to existing list items.

### Range session history section on `SessionDetailScreen`

- Section at the bottom of the session detail screen
- Header: "Session History" in `labelMedium`, uppercase
- Shows `RangeSessionHistoryItem` for each completed range session of this practice session
- Ordered by most recent first (`completedAt DESC`)
- Empty state: "No completed sessions yet." in `bodySmall`, `onSurfaceVariant`
- Only shows completed range sessions (not active, not abandoned)

### Club override on `ExecutionStepCard`

- Club display area becomes tappable when a club is assigned to the step
- Tapping opens a club picker (reuse existing `ClubPickerField` pattern or a bottom sheet)
- Picker shows the user's enabled clubs
- Selecting a club: calls `OverrideStepClubUseCase(rangeSessionId, stepIndex, clubCode)`
- The overridden club displays with a visual indicator (e.g., small "edited" badge or different text color) to distinguish from the original
- Selecting the original club reverts the override (or removes the override entry)
- Club override persists to `club_overrides` JSONB column

### `PracticePlannerViewModel` changes

**New state fields in `PracticePlannerUiState`:**
- `activeRangeSessions: List<ActiveRangeSessionSummary>` — populated on Overview navigation
- `completedRangeSessionHistory: Map<String, List<CompletedRangeSessionSummary>>` — keyed by practice session ID, populated alongside session data

**New loading behavior:**
- Active sessions: call `ListActiveRangeSessionsUseCase` on every navigation to Overview (not just on auth change)
- Completed history: call `ListCompletedRangeSessionsUseCase(sessionId)` for each loaded practice session. Consider lazy loading (only when session detail is viewed) vs. eager loading (all at once). Lazy is more efficient.

**New methods:**
- `loadActiveRangeSessions()` — called when Overview is navigated to
- `loadRangeSessionHistory(sessionId)` — called when session detail is navigated to
- `startRangeSessionFromPicker(sessionId): String?` — same as existing `startRangeSession` but triggered from the FAB picker
- `checkActiveSessionsForTemplate(sessionId): Boolean` — calls `HasActiveRangeSessionsUseCase`, used before template deletion

### Template deletion warning

When the user attempts to delete a practice session:
1. Call `HasActiveRangeSessionsUseCase(sessionId)` to check if there are active range sessions
2. If active sessions exist: show a modified `DeleteConfirmationDialog` with extra warning text: "This session has active range sessions. They will continue to work, but will no longer be linked to this session."
3. If no active sessions: show the standard deletion dialog
4. Deletion is NOT blocked in either case — the warning is informational

### Empty session guard (cross-cutting)

Verify the "Start Session" button is properly disabled in both entry points:
1. `SessionDetailScreen`: button disabled when practice session has zero units or zero instructions (from Stage 3)
2. `SessionPickerDialog`: only runnable sessions are listed (sessions with at least one unit containing at least one instruction)

### Resume flow

Resume is mostly implicit from the work in previous stages:
- `lastViewedStepIndex` is persisted on every step change (Stage 3)
- `RangeSessionViewModel` restores `lastViewedStepIndex` on init (Stage 3)
- Tapping an `ActiveRangeSessionCard` navigates to `range-sessions/{id}`, which creates a new `RangeSessionViewModel` that fetches fresh data and restores position
- Timer resumes on re-entry (Stage 5)
- Completion state is loaded from server (Stage 4)

The key new work for resume in this stage is:
- Surfacing the active session cards on Overview so the user can actually get back
- Verifying the full end-to-end resume flow works correctly

## Validation Checklist

### Active range sessions on Overview
- [ ] Starting a range session → returning to Overview → active session card appears
- [ ] Card shows session name, progress bar, started date
- [ ] Progress bar reflects completion state accurately
- [ ] Tapping card navigates to execution screen and resumes at correct step
- [ ] Multiple active sessions show in a scrollable carousel
- [ ] Carousel is hidden when no active sessions (no empty state shown)
- [ ] Carousel refreshes on every navigation to Overview
- [ ] Finishing a session → returning to Overview → card disappears
- [ ] Abandoning a session → returning to Overview → card disappears

### Resume
- [ ] Resume restores the last viewed step position
- [ ] Resume shows correct completion state from server
- [ ] Timer resumes correctly (if Stage 5 done)
- [ ] Club overrides are preserved across resume

### Club override
- [ ] Club display area is tappable on step card
- [ ] Club picker shows the user's enabled clubs
- [ ] Selecting a different club updates the display immediately
- [ ] Overridden club shows a visual indicator (different from original)
- [ ] Club override persists to database (verify `club_overrides` in Supabase)
- [ ] Club override survives resume
- [ ] Selecting the original club reverts the visual indicator
- [ ] Steps with no assigned club: club picker is not shown (no override possible)

### FAB session picker
- [ ] FAB appears on Overview screen
- [ ] Tapping FAB opens session picker dialog
- [ ] Dialog lists all runnable practice sessions
- [ ] Empty/unrunnable sessions are excluded from the list
- [ ] Selecting a session starts a range session and navigates to execution
- [ ] Loading state shown while starting
- [ ] Error during start shows snackbar, doesn't navigate
- [ ] Dialog is dismissible (back press or cancel)
- [ ] If no runnable sessions exist, dialog shows empty state message

### Range session history
- [ ] Session detail screen shows "Session History" section
- [ ] Completed range sessions appear with date, time, completion %
- [ ] Sessions are ordered most recent first
- [ ] Empty state shown when no history exists
- [ ] Abandoned sessions do not appear in history
- [ ] History persists after source session edits

### Template deletion warning
- [ ] Deleting a practice session with active range sessions shows warning text
- [ ] Warning text mentions active sessions will continue to work
- [ ] Deletion proceeds after confirmation (not blocked)
- [ ] Deleting a practice session without active range sessions shows standard dialog (no extra warning)
- [ ] After deletion, active range sessions continue functioning (snapshot self-contained)
- [ ] After deletion, `source_session_id` is null on affected range sessions
- [ ] Range session history for deleted templates is not shown (orphaned — no session detail to show it on)

### Empty session guard
- [ ] "Start Session" disabled on session detail for empty sessions
- [ ] Session picker excludes empty/unrunnable sessions
- [ ] Both entry points handle edge case: session becomes empty between listing and starting (RPC rejects)

## Accessibility Requirements

- `ActiveRangeSessionCard`: must be focusable, announce "Active session: Wedge Practice, 12 of 30 steps, started today at 2:30 PM. Tap to resume."
- Carousel section header "Active Sessions" must be announced as a heading
- `SessionPickerDialog`: title, list items, and dismiss action must be TalkBack-navigable
- Each session in the picker must announce its name and summary
- `RangeSessionHistoryItem`: must announce date, elapsed time, completion percentage
- Club override picker must be accessible — label, current value, and available options announced
- Overridden club indicator must have content description: "Club changed from Pitching Wedge to 9 Iron"
- FAB must have content description: "Start a session"
- Template deletion warning dialog must be fully announced including the warning about active sessions

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Overview screen layout changes break existing content (recent items, metrics) | Medium | Active sessions carousel is added above existing content. Test that scroll position and existing cards render correctly. Test on small screens. |
| FAB conflicts with existing Overview FAB (if one exists) | Medium | Check if Overview already has a FAB. If so, this replaces it or coexists. Current Overview uses `RangeworkFab` for create actions — may need to merge or replace. |
| `PracticePlannerViewModel` becomes overloaded with state | Low | Active sessions and history are lightweight data. The ViewModel already manages sessions, units, metrics. Adding two more fields is within reason. |
| `loadActiveRangeSessions()` on every Overview navigation causes visible loading flicker | Medium | Consider caching active sessions in the ViewModel (retain across navigations within the same app session). Only re-fetch on explicit refresh or return from execution screen. |
| Club override picker interferes with step card layout | Low | The override control is a small tappable area on the existing club display. Test that it doesn't shift other step card content. |
| `DeleteConfirmationDialog` modification breaks existing deletion flows | Low | The modification adds conditional extra text — existing behavior is preserved when no active sessions exist. Test both paths. |
| History section on session detail pushes content below the fold on small screens | Low | Place history at the bottom. It's additional content — existing content is unaffected. Consider lazy loading or expandable section. |
| Multiple simultaneous active sessions of the same practice session display identically in the carousel | Low | Cards are identical except for progress and start time. The start time differentiates them. Consider adding a "Run #2" indicator if user testing shows confusion. |
