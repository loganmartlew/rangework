# RWK-11: Range Sessions — Epic Implementation Plan

## Terminology

This glossary is the **authoritative source** for naming throughout all implementation work on this epic. Where Jira ticket titles, previous documentation, or conversation history use different terms, the terms below take precedence. All code (Kotlin types, DB tables, columns, RPCs), UI copy, documentation, commit messages, and PR descriptions must use these terms consistently.

| Term | Definition | Replaces |
|---|---|---|
| **Range Session** | A live, in-progress or completed execution instance of a practice session. The core entity of this feature. | "run", "session run", "execution", "execution instance" |
| **Practice Session** | A reusable template that defines units, instructions, and structure. Unchanged from existing app. | "session template", "source session" |
| **Snapshot** | The frozen, immutable copy of a practice session's content captured when a range session is started. | — |
| **Step** | A single, individually addressable item within a range session. Repeat counts are expanded into discrete steps. | "instruction/rep", "item" |
| **Start Session** | The action of creating a new range session from a practice session. | "start a run", "begin execution" |
| **Finish Session** | The action of marking a range session as done (all steps complete or user chooses to end). | "complete a run", "finalize" |
| **Abandon Session** | The action of discarding an in-progress range session (soft delete). | "abandon a run", "cancel", "early end" |
| **Resume Session** | Returning to an in-progress range session after navigating away or relaunching the app. | "resume a run" |
| **Complete Step** | Marking a single step within a range session as done. | "mark complete", "toggle complete" |
| **Active Range Session** | A range session that has been started but not yet finished or abandoned. | "active run", "in-progress run" |

**Code naming convention:** All Kotlin types, DB objects, and identifiers use `RangeSession` as the entity name. Examples: `RangeSession`, `RangeSessionRepository`, `StartRangeSessionUseCase`, `range_sessions` (DB table), `range_session_time_entries` (DB table), `start_range_session` (RPC).

**UI copy convention:** User-facing text uses "session" in context (e.g., "Start Session", "Finish Session", "Abandon Session"). The term "range session" appears where disambiguation from "practice session" is needed.

---

## Overview

Transform Practice Sessions from static, reusable templates into interactive, real-time workflows. A user starts a range session from a practice session at the driving range, steps through every instruction, marks steps complete, and tracks progress — all guided by a frozen snapshot of the practice session template.

This plan breaks the epic into 8 independently implementable stages, ordered by dependency. Each stage produces a testable increment. The full feature set spans Jira tickets RWK-13 through RWK-24.

---

## A. Dependency Graph

```
Stage 1: Schema & Migration
    │
    ▼
Stage 2: Shared Models & Data Layer
    │
    ▼
Stage 3: Start & Step Through ◄── minimum viable flow
    │
    ├──────────────┬──────────────┐
    ▼              ▼              ▼
Stage 4:       Stage 5:       Stage 6:
Completion     Timer          Step Drawer &
& Progress                    Tablet Layout
    │              │
    ▼              │
Stage 7:           │
Finish & Abandon   │
    │              │
    ├──────────────┘
    ▼
Stage 8: Resume, Active Sessions & Polish
```

**Key dependency rules:**
- Stages 1 → 2 → 3 are strictly sequential
- Stages 4, 5, and 6 can proceed in parallel after Stage 3
- Stage 7 depends on Stage 4 (needs completion data for finish summary)
- Stage 8 depends on Stages 4 and 7 (needs completion, finish, and abandon for active session resolution)
- Stage 5 can slot in at any point after Stage 3

---

## B. Implementation Stages

### Stage 1: Database Schema & Migration

**Goal:** Create all Supabase tables, indexes, RLS policies, triggers, and the `start_range_session` RPC in a single migration file. After this stage, the database is ready for the feature — no app changes yet.

**Tickets:** RWK-19 (data model), RWK-20 (snapshot generation)

**Scope:**

- `range_sessions` table with all columns: `id`, `owner_id`, `source_session_id`, `session_name`, `snapshot` (JSONB), `snapshot_version`, `completed_steps` (JSONB), `club_overrides` (JSONB), `last_viewed_step_index`, `started_at`, `completed_at`, `abandoned_at`, `created_at`, `updated_at`
- `range_session_time_entries` table: `id`, `range_session_id`, `entered_at`, `exited_at`
- Foreign keys: `owner_id` → `profiles.id` (CASCADE), `source_session_id` → `practice_sessions.id` (SET NULL), `range_session_id` → `range_sessions.id` (CASCADE)
- Partial indexes for active range sessions and completed session history queries
- RLS policies following existing `auth.uid() = owner_id` pattern; EXISTS subquery for time entries
- `updated_at` trigger on `range_sessions`
- `start_range_session(p_range_session_id uuid, p_session_id uuid) → jsonb` RPC: reads the practice session template, units, instructions; expands repeat counts; builds snapshot JSONB; inserts range session row; returns full row. Runs as `SECURITY INVOKER`. Validates session is non-empty.

**Risks:**
- The snapshot RPC is the most complex piece of SQL in the project. It must correctly read across 4 tables (`practice_sessions`, `practice_session_items`, `practice_units`, `practice_unit_instructions`), preserve ordering, expand repeats, and build a well-formed JSONB structure. Thorough manual testing against the local Supabase instance is essential.
- `SECURITY INVOKER` means the RPC respects RLS — if a session item references a unit the user doesn't own (shouldn't happen in practice), the snapshot will silently omit it. The RPC should validate the resulting snapshot is non-empty.

**Edge cases to resolve:**
- What should the RPC return/raise if the practice session has been deleted between the user tapping "Start" and the RPC executing? → Raise a descriptive exception.
- Should `snapshot_version` be hardcoded in the RPC or passed by the client? → Hardcoded in the RPC (the server defines the schema version).
- Maximum snapshot size — a practice session with many units and high repeat counts could produce a large JSONB blob. Is there a practical upper bound? → Unlikely to hit Postgres JSONB limits in normal use, but worth noting.

**Validation checkpoint:** Run the migration against local Supabase. Manually call `start_range_session` via Supabase Studio or `psql` for a populated practice session. Verify the returned JSONB contains correctly expanded steps with proper ordering.

---

### Stage 2: Shared Models & Data Layer

**Goal:** Build the complete Kotlin shared-module infrastructure: domain models, repository interface, Supabase implementation, all use cases, progress computation helpers, and DataFoundation wiring. After this stage, the shared module can start range sessions, read them, toggle step completions, and compute progress — all testable without any UI.

**Tickets:** RWK-19 (data model), RWK-21 (persistence layer), RWK-22 (progress logic), RWK-23 (lifecycle/navigation primitives)

**Scope:**

*Models:*
- `RangeSession`, `RangeSessionSnapshot`, `SnapshotUnit`, `SnapshotInstruction`, `SnapshotStep`, `CompletedStep`
- `ActiveRangeSessionSummary` (lightweight, for Overview), `CompletedRangeSessionSummary` (for practice session detail history)
- All models use `kotlinx.serialization` with `@SerialName` annotations matching the JSONB/DB column names

*Repository:*
- `RangeSessionRepository` interface with all methods: `startSession`, `getSession`, `listActiveSessions`, `listCompletedSessions`, `toggleStepComplete`, `overrideStepClub`, `updateLastViewedStep`, `finishSession`, `abandonSession`, `recordTimeEntry`, `closeTimeEntry`, `getElapsedSeconds`
- `SupabaseRangeSessionRepository` implementing via PostgREST and RPC calls
- Row DTOs (`RangeSessionRow`, `TimeEntryRow`, etc.) with `@SerialName` mappings

*Use cases:*
- `StartRangeSessionUseCase`, `GetRangeSessionUseCase`, `ListActiveRangeSessionsUseCase`, `ListCompletedRangeSessionsUseCase`, `ToggleStepCompleteUseCase`, `OverrideStepClubUseCase`, `UpdateLastViewedStepUseCase`, `FinishRangeSessionUseCase`, `AbandonRangeSessionUseCase`, `RecordTimeEntryUseCase`, `CloseTimeEntryUseCase`, `GetElapsedSecondsUseCase`

*Progress helpers:*
- Extension functions on `RangeSession`: `completedStepCount()`, `totalStepCount()`, `completionPercentage()`, `completedBalls()`, `totalBalls()`, `completedUnits()`, `isFullyComplete()`, `isActive()`

*Foundation:*
- Extend `DataFoundation` with all new use case fields
- Update `createDataFoundation` factory to wire the new repository and use cases

**Risks:**
- Snapshot deserialization: the JSONB schema must exactly match the Kotlin model's `@SerialName` annotations. A mismatch will cause runtime crashes. Careful alignment with the RPC's output structure is critical.
- `toggleStepComplete` does a read-modify-write on the `completed_steps` JSONB. With online-only and single-device use the conflict risk is low, but the operation is not atomic at the DB level. If this becomes a problem later, it could move to an RPC.
- `listActiveSessions` needs to derive summary data (step counts) without deserializing the full snapshot. This requires either a server-side computed column, a JSONB path query (`jsonb_array_length(snapshot->'steps')`), or fetching the snapshot and computing client-side. The approach should be decided during implementation.

**Edge cases to resolve:**
- How should `listActiveSessions` compute `totalSteps` and `completedStepCount` without pulling full snapshots? Options: (a) JSONB path query in the SELECT, (b) denormalize `total_steps` as a column on `range_sessions` (set by the RPC at creation), (c) pull the snapshot. → Recommend adding a `total_steps` integer column populated by the RPC, to keep summary queries lightweight.
- Should `getElapsedSeconds` handle open-ended time entries (no `exited_at`)? → Yes, treat them as "still running" up to `now()`.

**Validation checkpoint:** Shared module unit tests pass. Use case tests verify: start returns a valid range session, toggle adds/removes completion entries, progress helpers compute correctly for edge cases (zero balls, all complete, empty), abandon sets timestamp, finish sets timestamp.

---

### Stage 3: Start & Step Through

**Goal:** The minimum viable end-to-end flow. A user taps "Start Session" on a practice session detail screen, a range session is created, and they land on the execution screen where they can navigate through steps using next/previous buttons. No completion, no timer, no drawer — just start and read through.

**Tickets:** RWK-13 (start a range session), RWK-14 (step through instructions), RWK-24 (navigation routing)

**Scope:**

*ViewModel:*
- `RangeSessionViewModel` scoped to the execution screen's nav graph
- State: `RangeSessionUiState` with `rangeSession`, `currentStepIndex`, `isLoading`, `statusMessage`
- On init: fetch range session by ID, restore `lastViewedStepIndex`
- Methods: `nextStep()`, `previousStep()`, `navigateToStep(index)`
- Factory pattern matching existing ViewModels

*Navigation:*
- New route: `range-sessions/{rangeSessionId}`
- Route helper: `fun rangeSession(rangeSessionId: String)`
- Navigation from practice session detail → start range session → navigate to `range-sessions/{newId}`

*Practice session detail screen changes:*
- "Start Session" button added to `SessionDetailScreen`
- Disabled with explanation if session has zero units or zero instructions
- On tap: calls `StartRangeSessionUseCase`, navigates to execution screen

*Execution screen:*
- Displays current step: instruction text, ball count, club + display name, notes, focus cue
- Unit context: unit title, step X of Y, rep N of M
- Next/Previous buttons (disabled at boundaries)
- `Modifier.keepScreenOn()` applied
- Phone-only layout for now (tablet layout comes with the drawer in Stage 6)

**Risks:**
- ViewModel scoping to nav graph: Compose Navigation's `viewModel()` scoping depends on the nav graph structure. If the execution route is a top-level destination (not nested), the ViewModel may be scoped to the activity. Needs careful nav graph setup, potentially a nested graph for the execution flow.
- The start action on practice session detail triggers a network RPC and then navigation. Error handling for RPC failure (network error, empty session rejection) needs to be user-friendly — show a toast/snackbar, don't navigate.
- `keepScreenOn` must not leak — verify it only applies while the execution composable is in the composition.

**Edge cases to resolve:**
- What happens if the user navigates to `range-sessions/{id}` for a range session that doesn't exist (e.g., deep link, stale URL)? → Show an error state and offer to go back.
- What happens if the user presses back from the execution screen? → Navigate back to wherever they came from (practice session detail or Overview). No confirmation dialog needed at this stage (no progress to lose yet).
- Step boundary behavior: at step 0, "Previous" is disabled. At the last step, "Next" is disabled. Confirm this is correct (or should it wrap?). → Don't wrap, disable at boundaries.

**Validation checkpoint:** Build and run the app. Navigate to a practice session detail screen, tap Start Session, verify a range session is created, and step through all instructions with next/previous. Verify unit context, ball counts, clubs, and notes display correctly. Verify keep-awake is active. Verify pressing back returns to session detail. Run ViewModel tests.

---

### Stage 4: Completion & Progress

**Goal:** Add the ability to complete/uncomplete steps and display live progress metrics. This is the core interactive value of the feature — the user can now actively work through their practice at the range.

**Tickets:** RWK-15 (mark complete), RWK-16 (progress overview), RWK-22 (progress computation), RWK-23 (per-step states)

**Scope:**

*ViewModel additions:*
- `toggleStepComplete(stepIndex)` — calls use case, updates local state, auto-advances to next step if completing the current step
- Live progress computation using the shared progress helpers
- State additions: completion state per step, progress metrics

*Execution screen additions:*
- Complete/uncomplete toggle on the current step (prominent button or checkbox)
- Visual distinction: completed steps vs. incomplete
- Progress header at top of execution screen: steps completed/total, balls hit/total, completion %, per-unit indicators
- Progress updates live as steps are toggled

*New components:*
- `RangeSessionProgressHeader` — progress stats bar
- Completion toggle control on step card

**Risks:**
- Auto-advance after completion: if the user completes the last incomplete step, where does auto-advance go? Options: stay on the current step, or jump to the first remaining incomplete step (which may be earlier if they skipped). Needs clear logic.
- Rapid toggling: if the user taps complete/uncomplete rapidly, each toggle triggers a network write. Without debouncing or optimistic UI, the UX could feel sluggish. Consider optimistic local state updates with background persistence.
- Progress computation must handle: instructions with null ball counts (excluded from ball totals), all-null ball counts (show "N/A" for ball metric), division by zero on completion percentage.

**Edge cases to resolve:**
- Auto-advance target: when completing a step, advance to the next step by index (even if it's already complete), or advance to the next *incomplete* step? → Recommend next by index — simpler, predictable. The user can then navigate further with next/prev.
- What visual state does a completed step have when the user navigates back to it? → Show it as completed (checkmark, muted styling) but still allow uncompleting.
- If all steps are complete, the progress header should show 100%. No auto-trigger for finish (manual per Q49), but the "Finish Session" button should become prominent.

**Validation checkpoint:** Start a range session, complete some steps, verify progress updates. Uncomplete a step, verify progress decrements. Complete all steps, verify 100% progress. Test with a practice session containing instructions with null ball counts. Run shared progress helper tests. Run ViewModel tests for completion flows.

---

### Stage 5: Timer & Elapsed Time

**Goal:** Track active time spent on the execution screen using timestamp-based time entries. Display elapsed time live in the progress header and include it in the eventual finish summary.

**Tickets:** RWK-16 (elapsed time display), RWK-22 (elapsed computation)

**Scope:**

*ViewModel additions:*
- On init (entering execution screen): call `RecordTimeEntryUseCase` with current timestamp
- On dispose (leaving execution screen): call `CloseTimeEntryUseCase` with entry and exit timestamps
- Coroutine-based timer tick: update `elapsedSeconds` every second while on screen
- State additions: `elapsedSeconds`, `isTimerRunning`
- On resume (re-entering): fetch accumulated time, start new time entry, resume ticking

*Lifecycle handling:*
- Use `DisposableEffect` or ViewModel `onCleared` to close time entries on screen exit
- Handle edge cases: app backgrounded, screen turned off, process killed

*Progress header addition:*
- Elapsed time display (formatted as MM:SS or H:MM:SS)

**Risks:**
- **Lifecycle complexity is the primary risk.** The enter/exit pattern interacts with Android's activity lifecycle, Compose's composition lifecycle, and the nav graph's backstack. The ViewModel must reliably detect "user is on execution screen" vs. "user navigated away" vs. "app backgrounded" vs. "app killed."
- ViewModel scoped to nav graph means `onCleared` fires when the user navigates away — this is the exit signal. But if the app is killed, `onCleared` may not fire, leaving an open-ended time entry. This is an accepted tradeoff (the open entry gets treated as "still running" on next load, or capped at a reasonable duration).
- The timer coroutine must use `Dispatchers.Default` or similar to avoid being paused by the main dispatcher during heavy UI work.

**Edge cases to resolve:**
- Open-ended time entries (no `exited_at`): when computing elapsed on next load, should these be capped? → Cap at the `updated_at` of the range session row as a reasonable proxy, or treat as still running if the session is still active.
- Timer display format: MM:SS for sessions under an hour, H:MM:SS for longer? → Use MM:SS always, switching to H:MM:SS when ≥ 60 minutes.
- Should elapsed time include time spent in the drawer/step list (which is still "on the execution screen")? → Yes, the drawer is part of the execution screen — timer keeps running.

**Validation checkpoint:** Enter execution screen, verify timer starts. Navigate away, verify timer pauses. Come back, verify timer resumes and accumulated time is correct. Kill and restart app, verify accumulated time is roughly correct (within the open-entry tolerance). Run ViewModel lifecycle tests.

---

### Stage 6: Step Drawer & Tablet Layout

**Goal:** Add the step-list drawer for browsing and jumping to any step, and implement the tablet layout with a permanent side panel.

**Tickets:** RWK-14 (step-through UI), RWK-23 (navigation primitives)

**Scope:**

*Phone layout:*
- `ModalNavigationDrawer` containing the step list
- Drawer trigger: hamburger/list button in the top bar
- Drawer content: all steps listed, grouped by unit title
- Each step shows: instruction text (truncated), completion state (checkmark), step number, rep indicator
- Currently viewed step is visually highlighted
- Tapping a step: closes drawer, navigates to that step

*Tablet layout:*
- Responsive detection using existing `navigationTypeForScreenWidth` pattern
- `Row` layout: permanent step list panel on the left, current step detail on the right
- Step list panel is the same content as the drawer, always visible
- No drawer gesture needed on tablet

*New components:*
- `StepListDrawerContent` — scrollable list of all steps grouped by unit, with completion states
- `StepListItem` — individual step row in the list

**Risks:**
- `ModalNavigationDrawer` may conflict with the swipe-back gesture on some Android versions/launchers. Test on multiple devices.
- The step list could be very long for practice sessions with many units and high repeat counts. Lazy scrolling (`LazyColumn`) is important. Auto-scroll to the current step when the drawer opens.
- Tablet breakpoint must align with the existing navigation rail breakpoint (840dp) to avoid inconsistent layouts.

**Edge cases to resolve:**
- Should the drawer auto-scroll to the current step when opened? → Yes.
- When the user jumps to a far-away step via the drawer, should `lastViewedStepIndex` be updated immediately? → Yes, persist on every step change.
- Very long unit titles or instruction text in the drawer list: truncate with ellipsis? → Yes, single-line truncation.

**Validation checkpoint:** Open drawer on phone, verify all steps are listed with correct grouping and completion states. Tap a step, verify navigation. Test on tablet-width screen, verify permanent panel layout. Test with a large practice session (many units, high repeats). Verify auto-scroll to current step.

---

### Stage 7: Finish & Abandon

**Goal:** Allow users to finish or abandon a range session, with appropriate confirmation, summary display, and cleanup.

**Tickets:** RWK-18 (complete or abandon), RWK-23 (lifecycle transitions)

**Scope:**

*Finish flow:*
- "Finish Session" button: always available in the execution screen action menu, visually prominent when all steps are complete
- On tap: call `FinishRangeSessionUseCase` (sets `completed_at`), close any open time entry, navigate to finish summary
- Finish summary screen: total balls hit, elapsed time, completion percentage
- From summary: navigate back to Overview or practice session detail

*Abandon flow:*
- "Abandon Session" button in execution screen action menu
- On tap: show simple confirmation dialog ("Are you sure? Progress will be lost.")
- On confirm: call `AbandonRangeSessionUseCase` (sets `abandoned_at`, soft delete), close any open time entry, navigate back
- On cancel: dismiss dialog, stay on execution screen

*New components:*
- `FinishSummaryContent` — stats display for the finish screen
- `AbandonConfirmDialog` — simple confirmation dialog

*Navigation:*
- New route for finish summary (or inline dialog/bottom sheet)
- Back-navigation after finish/abandon returns to the previous screen (Overview or practice session detail)

**Risks:**
- Race condition: user taps "Finish Session" while a completion toggle is still in-flight. The finish call should wait for pending writes or use the latest server state.
- Navigation after finish/abandon: if the user came from the Overview banner, back should go to Overview. If they came from practice session detail, back should go there. The nav backstack handles this naturally, but verify.
- The finish summary needs elapsed time. If Stage 5 (Timer) is not yet implemented, the summary can show "—" for elapsed time — the summary should degrade gracefully.

**Edge cases to resolve:**
- Can the user finish a range session with 0 steps completed (immediate finish after start)? → Technically yes — the session is "finished" with 0% progress. The summary would show 0 balls, 0% completion. This is acceptable.
- Can the user finish while on a step that's not the last? → Yes, "Finish Session" is available at any time.
- What happens to the `RangeSessionViewModel` after finish/abandon? → It's destroyed (nav-graph scoped), and the range session is no longer active.

**Validation checkpoint:** Start a range session, complete all steps, tap Finish Session, verify summary shows correct stats. Start a range session, complete some steps, tap Abandon Session, verify confirmation dialog, verify session disappears from active list. Verify navigation returns to the correct screen. Run ViewModel tests for finish/abandon flows.

---

### Stage 8: Resume, Active Sessions & Polish

**Goal:** Surface active range sessions on the Overview screen, enable resume, and implement remaining ancillary features: club override, Overview FAB session picker, range session history on practice session detail, template deletion warning, and empty session validation.

**Tickets:** RWK-13 (start entry points), RWK-15 (club override), RWK-17 (resume/persist), RWK-24 (active session resolution & routing)

**Scope:**

*Active range sessions on Overview:*
- `ListActiveRangeSessionsUseCase` called in `PracticePlannerViewModel` on navigation to Overview
- `activeRangeSessions: List<ActiveRangeSessionSummary>` added to `PracticePlannerUiState`
- Horizontally scrollable card carousel on Overview screen
- Each card: practice session name, progress bar, date/time started
- Tapping a card navigates to `range-sessions/{id}`
- Carousel hidden when no active range sessions

*Resume:*
- `lastViewedStepIndex` persisted on every step change (from Stage 3)
- On re-entering execution screen: restore last viewed step
- Range session data re-fetched from server on every entry (ViewModel is nav-graph scoped)

*Club override:*
- Club picker on the execution step card (only for the current step)
- Calls `OverrideStepClubUseCase`, persists to `club_overrides` JSONB
- Displays the overridden club name, with an indicator that it was changed from the original

*Overview FAB session picker:*
- FAB on Overview screen opens a session picker dialog/bottom sheet
- Lists all runnable practice sessions (non-empty)
- Selecting a session calls `StartRangeSessionUseCase`, navigates to execution

*Range session history on practice session detail:*
- `ListCompletedRangeSessionsUseCase` called in `PracticePlannerViewModel`
- `completedRangeSessionHistory: Map<String, List<CompletedRangeSessionSummary>>` added to `PracticePlannerUiState`
- Section on `SessionDetailScreen` showing past completed range sessions (date, elapsed time, completion %)

*Template deletion warning:*
- When deleting a practice session that has active range sessions: show warning ("This session has active range sessions. They will continue to work, but will no longer be linked to this session.")
- Do not block deletion
- Requires a check: query active range sessions by `source_session_id` before confirming delete

*Empty session guard:*
- "Start Session" button disabled with explanation when practice session has zero units or all units have zero instructions (from Stage 3, but the validation logic for the FAB picker also needs it)

*New components:*
- `ActiveRangeSessionCard` — card for Overview carousel
- `RangeSessionHistoryItem` — row for practice session detail history section
- `SessionPickerDialog` — dialog listing runnable practice sessions

**Risks:**
- Active range sessions refresh on every Overview navigation could be slow if there are many. In practice this is unlikely (users will have 0–3 active range sessions), but the query should be efficient (partial index helps).
- Club override interacts with the snapshot's immutability contract. The snapshot JSONB is never mutated — overrides are stored in a separate column. The execution screen must merge `snapshot.steps[i].club` with `clubOverrides[i]` at display time.
- The session picker dialog needs the current list of practice sessions. It can reuse the sessions already loaded in `PracticePlannerUiState`, but must filter to only runnable ones.
- Template deletion warning requires a query for active range sessions by practice session ID. This is a new query not covered by the existing repository. Either add a method to `RangeSessionRepository` or inline the check.

**Edge cases to resolve:**
- Club override: should the picker show all clubs in the catalog, or only the user's enabled clubs? → Show enabled clubs (consistent with session editing).
- Club override: can the user revert to the original club? → Yes, by selecting it in the picker. The override entry can be removed or set back to the original code.
- Range session history: what if the source practice session is deleted? History entries are linked by `source_session_id` which becomes null — those range sessions won't appear in any practice session's detail view. This is acceptable (the sessions are "orphaned" but still stored).
- Session picker: should it show practice sessions that already have active range sessions? → Yes (multiple range sessions of the same practice session are allowed).

**Validation checkpoint:** Verify active range sessions appear on Overview after starting one. Verify tapping a card resumes at the correct step. Verify club override persists across resume. Verify FAB picker shows practice sessions and starts range sessions. Verify range session history appears on practice session detail. Verify template deletion warning. Verify empty session guard on both practice session detail and FAB picker.

---

## C. Stage Ordering

| Order | Stage | Depends On | Parallel With |
|---|---|---|---|
| 1 | Schema & Migration | — | — |
| 2 | Shared Models & Data Layer | Stage 1 | — |
| 3 | Start & Step Through | Stage 2 | — |
| 4 | Completion & Progress | Stage 3 | Stages 5, 6 |
| 5 | Timer & Elapsed Time | Stage 3 | Stages 4, 6 |
| 6 | Step Drawer & Tablet Layout | Stage 3 | Stages 4, 5 |
| 7 | Finish & Abandon | Stage 4 | Stage 6 |
| 8 | Resume, Active Sessions & Polish | Stages 4, 7 | — |

**Recommended implementation order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

While Stages 4/5/6 can theoretically run in parallel, the recommended linear order prioritizes building the most user-valuable features first (completion tracking before timer, timer before drawer). Stage 7 naturally follows completion, and Stage 8 ties everything together.

---

## D. Validation Checkpoints

Each stage has its own test criteria (detailed above). The key epic-wide checkpoints are:

| After Stage | What You Can Verify |
|---|---|
| 1 | Migration runs clean. RPC returns valid snapshot for a test practice session. |
| 2 | Shared unit tests pass. All models serialize/deserialize. Progress helpers handle edge cases. |
| 3 | **End-to-end smoke test:** start a range session from practice session detail, step through all instructions, see correct content. |
| 4 | **Core workflow test:** start, complete steps, see progress update, uncomplete, verify progress decrements. |
| 5 | Timer accumulates correctly across enter/exit cycles. |
| 6 | Drawer shows all steps, jump-to works, tablet layout renders correctly. |
| 7 | **Full lifecycle test:** start → complete all → finish → see summary. Start → abandon → confirm → gone. |
| 8 | **Full feature test:** Overview shows active range sessions, resume works, club override persists, FAB starts sessions, history visible on practice session detail. |

**Final acceptance test (all stages complete):**
1. Open app → Overview shows no active range sessions
2. Tap FAB → pick a practice session → range session starts → execution screen with first step
3. Step through, complete some steps, see progress update
4. Navigate away → range session persists → Overview shows active session card with progress bar
5. Tap card → resume at correct step → timer resumes
6. Open drawer → see all steps with completion states → jump to a step
7. Override a club on one step → see updated club
8. Complete all steps → tap Finish Session → see summary with balls, time, %
9. Check practice session detail → range session appears in history
10. Start another range session of same practice session → both are independent

---

## E. Risks

### High Impact

| Risk | Likelihood | Mitigation |
|---|---|---|
| **Snapshot RPC complexity** — the SQL function reads 4 tables, expands repeats, builds JSONB. Bugs here break the entire feature. | Medium | Thorough manual testing with varied practice session shapes. Add a `total_steps` column populated by the RPC for validation. |
| **JSONB ↔ Kotlin serialization mismatch** — any field name or structure difference between the RPC's output and the Kotlin model causes runtime crashes. | Medium | Define the snapshot schema once (in the tech requirements) and implement both sides from it. Deserialize in unit tests using fixture JSON. |
| **Timer lifecycle edge cases** — Android lifecycle (background, kill, config change) makes reliable enter/exit tracking difficult. | Medium-High | Accept that open-ended time entries are possible. Cap them at a reasonable duration on next load. Don't over-engineer — the timer is informational, not billing-critical. |

### Medium Impact

| Risk | Likelihood | Mitigation |
|---|---|---|
| **Completion JSONB read-modify-write** — concurrent writes to `completed_steps` could conflict on multi-device. | Low (online-only, single-device typical) | Accept last-write-wins for now. Document as a known limitation. Move to RPC if needed later. |
| **ViewModel nav-graph scoping** — may not work as expected with Compose Navigation, causing re-creation or incorrect lifecycle. | Medium | Prototype early in Stage 3. Fall back to Activity-scoped ViewModel if nav-graph scoping proves unreliable. |
| **Large step lists** — practice sessions with many units × high repeat counts produce long step lists that could impact drawer performance. | Low | Use `LazyColumn` for the step list. Test with a worst-case session (e.g., 10 units × 10 reps × 5 instructions = 500 steps). |

### Low Impact

| Risk | Likelihood | Mitigation |
|---|---|---|
| **Active sessions query performance** — `listActiveSessions` on every Overview navigation. | Very Low | Partial index on active range sessions keeps the query fast. Only returns summary data. |
| **Snapshot size** — very large practice sessions could produce large JSONB blobs. | Very Low | Postgres handles JSONB well. No action needed unless profiling shows an issue. |
| **Club override complexity** — merge logic between snapshot club and override on every step render. | Low | Simple map lookup. Override present → use override, else use snapshot club. |
