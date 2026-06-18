# RWK-11: Session Execution — Technical Requirements

Derived from Jira tickets RWK-11 through RWK-24, four rounds of requirements questions (Q1–Q50), and a codebase survey of the shared module, Android app, and Supabase schema.

---

## 1. Supabase Schema

### 1.1 New Tables

All new tables are created in a **single migration file**. *(RWK-19, Q40)*

#### `session_runs`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `uuid` | PK, default `gen_random_uuid()` | Client-generated UUID *(Q31)* |
| `owner_id` | `uuid` | NOT NULL, default `auth.uid()`, FK → `profiles.id` ON DELETE CASCADE | Ownership *(Q39)* |
| `source_session_id` | `uuid` | FK → `practice_sessions.id` ON DELETE SET NULL | Nullable — template may be deleted *(non-tech Q23)* |
| `session_name` | `text` | NOT NULL | Snapshot of session name at start time *(non-tech Q23)* |
| `snapshot` | `jsonb` | NOT NULL | Immutable frozen copy of session template *(RWK-19, Q28)* |
| `snapshot_version` | `integer` | NOT NULL, default `1` | Schema version for snapshot JSONB *(Q42)* |
| `completed_steps` | `jsonb` | NOT NULL, default `'[]'::jsonb` | Array of `{stepIndex, completedAt}` objects *(Q29, Q43)* |
| `club_overrides` | `jsonb` | NOT NULL, default `'{}'::jsonb` | Map of step index → club code *(Q44)* |
| `last_viewed_step_index` | `integer` | | Step the user was last viewing, for resume *(non-tech 3.3)* |
| `started_at` | `timestamptz` | NOT NULL, default `now()` | Run start timestamp *(RWK-13)* |
| `completed_at` | `timestamptz` | | Set when all steps done and user taps Finish *(RWK-18)* |
| `abandoned_at` | `timestamptz` | | Soft-delete marker for abandoned runs *(Q48)* |
| `created_at` | `timestamptz` | NOT NULL, default `now()` | |
| `updated_at` | `timestamptz` | NOT NULL, default `now()` | Trigger-managed |

**Indexes:**
- `session_runs_owner_active_idx` on `(owner_id, started_at DESC)` WHERE `completed_at IS NULL AND abandoned_at IS NULL` — for listing active runs *(RWK-24)*
- `session_runs_source_session_idx` on `(source_session_id, completed_at DESC)` WHERE `completed_at IS NOT NULL AND abandoned_at IS NULL` — for run history on session detail *(Q22)*

**Triggers:**
- `set_session_runs_updated_at` BEFORE UPDATE → `set_updated_at()` *(existing pattern)*

**RLS Policies:**
- ALL: `auth.uid() = owner_id` *(Q39, following existing pattern)*

#### `session_run_time_entries`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `uuid` | PK, default `gen_random_uuid()` | |
| `session_run_id` | `uuid` | NOT NULL, FK → `session_runs.id` ON DELETE CASCADE | Parent run |
| `entered_at` | `timestamptz` | NOT NULL | When user entered execution screen |
| `exited_at` | `timestamptz` | | When user left; null if still active *(Q45)* |

**RLS Policies:**
- ALL: EXISTS subquery checking `session_runs.owner_id = auth.uid()` *(Q39)*

**No `updated_at` trigger needed** — rows are write-once (insert `entered_at`), then updated once (set `exited_at`).

### 1.2 Snapshot JSONB Schema (Version 1)

The `snapshot` column stores a versioned, denormalized copy of the session at start time. *(RWK-19, RWK-20, Q28)*

```json
{
  "sessionNotes": "Optional session-level notes",
  "units": [
    {
      "unitTitle": "Wedge Warmup",
      "unitNotes": "Optional unit notes",
      "unitFocus": "Weight forward",
      "itemNotes": "Optional session-item notes",
      "itemFocusCue": "Optional focus cue",
      "club": "pitching_wedge",
      "clubDisplayName": "Pitching Wedge",
      "repeatCount": 3,
      "instructions": [
        {
          "text": "Half swings to target",
          "ballCount": 10
        }
      ]
    }
  ],
  "steps": [
    {
      "unitIndex": 0,
      "instructionIndex": 0,
      "repNumber": 1,
      "totalReps": 3,
      "instructionText": "Half swings to target",
      "ballCount": 10,
      "club": "pitching_wedge",
      "clubDisplayName": "Pitching Wedge",
      "unitTitle": "Wedge Warmup",
      "notes": "Combined unit + item notes if present",
      "focusCue": "Weight forward"
    }
  ]
}
```

- `units` preserves the template structure for reference/display
- `steps` is the expanded, pre-flattened list of individually addressable steps (repeat counts expanded into discrete entries) *(RWK-20)*
- Step indices (0-based) are the stable identifiers used in `completed_steps`, `club_overrides`, and `last_viewed_step_index`
- Ordering is deterministic: units in session order → instructions within each unit in sort order → reps 1..N *(RWK-20)*

### 1.3 Server-Side RPC

#### `start_session_run(p_run_id uuid, p_session_id uuid) → jsonb`

*(RWK-20, Q32, Q41)*

- Reads `practice_sessions`, `practice_session_items`, `practice_units`, `practice_unit_instructions` to build the snapshot
- Expands repeat counts into discrete steps
- Inserts the `session_runs` row with generated snapshot
- Returns the full run row as JSONB so the client can immediately navigate to the execution screen *(Q41)*
- Runs as `SECURITY INVOKER` — the user owns all template rows, so RLS grants sufficient access. This is the more secure option as it doesn't bypass RLS. *(Q41)*
- Validates session is non-empty (has units with instructions); raises exception if not *(non-tech Q7)*
- `p_run_id` is client-generated UUID *(Q31)*
- Grants: `TO authenticated`

### 1.4 Migration File

A single timestamped migration file (e.g., `YYYYMMDDHHMMSS_session_runs.sql`) creates: *(Q40)*
- `session_runs` table with all columns, constraints, indexes
- `session_run_time_entries` table
- RLS policies for both tables
- `updated_at` trigger for `session_runs`
- `start_session_run` RPC

---

## 2. Shared Module — Models

*(RWK-19, Q34 — follow identical layering to existing code)*

### 2.1 Domain Models

All models in `shared/src/commonMain/kotlin/.../model/`.

**`SessionRun`** (persisted model):
- `id: String`
- `sourceSessionId: String?` — null if template was deleted
- `sessionName: String` — from snapshot, always available
- `snapshot: SessionRunSnapshot`
- `snapshotVersion: Int`
- `completedSteps: List<CompletedStep>`
- `clubOverrides: Map<Int, String>` — step index → club code
- `lastViewedStepIndex: Int?`
- `startedAt: Instant`
- `completedAt: Instant?`
- `abandonedAt: Instant?`

**`SessionRunSnapshot`** (immutable, deserialized from JSONB):
- `sessionNotes: String?`
- `units: List<SnapshotUnit>`
- `steps: List<SnapshotStep>`

**`SnapshotUnit`**:
- `unitTitle: String`
- `unitNotes: String?`
- `unitFocus: String?`
- `itemNotes: String?`
- `itemFocusCue: String?`
- `club: String?` — club code
- `clubDisplayName: String?`
- `repeatCount: Int`
- `instructions: List<SnapshotInstruction>`

**`SnapshotInstruction`**:
- `text: String`
- `ballCount: Int?`

**`SnapshotStep`** (flattened, individually addressable):
- `unitIndex: Int`
- `instructionIndex: Int`
- `repNumber: Int`
- `totalReps: Int`
- `instructionText: String`
- `ballCount: Int?`
- `club: String?`
- `clubDisplayName: String?`
- `unitTitle: String`
- `notes: String?`
- `focusCue: String?`

**`CompletedStep`**:
- `stepIndex: Int`
- `completedAt: Instant`

**`ActiveRunSummary`** (lightweight, for Overview banner):
- `id: String`
- `sessionName: String`
- `totalSteps: Int`
- `completedStepCount: Int`
- `startedAt: Instant`

**`CompletedRunSummary`** (for session detail history):
- `id: String`
- `sessionName: String`
- `totalSteps: Int`
- `completedStepCount: Int`
- `totalBalls: Int`
- `completedBalls: Int`
- `startedAt: Instant`
- `completedAt: Instant`
- `elapsedSeconds: Long`

### 2.2 Progress Computation Helpers

*(RWK-22)*

Reusable functions, likely as extension functions on `SessionRun` or standalone helpers:

- `SessionRun.completedStepCount(): Int`
- `SessionRun.totalStepCount(): Int`
- `SessionRun.completionPercentage(): Double` — guards against division by zero
- `SessionRun.completedBalls(): Int` — sum of `ballCount` for completed steps, ignoring nulls
- `SessionRun.totalBalls(): Int` — sum of all step `ballCount` values, ignoring nulls
- `SessionRun.completedUnits(): Int` — count of units where all associated steps are complete
- `SessionRun.isFullyComplete(): Boolean` — all steps completed
- `SessionRun.isActive(): Boolean` — `completedAt == null && abandonedAt == null`

These are used by both the live progress overview *(RWK-16)* and the finish summary *(RWK-18)*.

---

## 3. Shared Module — Repository

*(RWK-21, Q34)*

### 3.1 Repository Interface

`SessionRunRepository` in `shared/src/commonMain/kotlin/.../repository/`:

```
suspend fun startRun(runId: String, sessionId: String): SessionRun
suspend fun getRun(runId: String): SessionRun?
suspend fun listActiveRuns(): List<ActiveRunSummary>
suspend fun listCompletedRuns(sessionId: String): List<CompletedRunSummary>
suspend fun toggleStepComplete(runId: String, stepIndex: Int, completed: Boolean): SessionRun
suspend fun overrideStepClub(runId: String, stepIndex: Int, clubCode: String): SessionRun
suspend fun updateLastViewedStep(runId: String, stepIndex: Int)
suspend fun completeRun(runId: String): SessionRun
suspend fun abandonRun(runId: String)
suspend fun recordTimeEntry(runId: String, enteredAt: Instant)
suspend fun closeTimeEntry(runId: String, enteredAt: Instant, exitedAt: Instant)
suspend fun getElapsedSeconds(runId: String): Long
```

### 3.2 Supabase Implementation

`SupabaseSessionRunRepository` in `shared/src/commonMain/kotlin/.../data/`:

- `startRun` → calls `start_session_run` RPC, deserializes returned JSONB into `SessionRun` *(Q32, Q41)*
- `toggleStepComplete` → reads current `completed_steps`, adds/removes entry, writes back via PostgREST PATCH *(Q29)*
- `overrideStepClub` → reads `club_overrides`, updates map, writes back via PATCH *(Q44)*
- `completeRun` → sets `completed_at` timestamp via PATCH *(RWK-18)*
- `abandonRun` → sets `abandoned_at` timestamp via PATCH (soft delete) *(Q48)*
- `listActiveRuns` → queries `session_runs` WHERE `completed_at IS NULL AND abandoned_at IS NULL`, returns `ActiveRunSummary` (no snapshot JSONB needed — derive counts from `completed_steps` array length and snapshot step count) *(Q46)*
- `listCompletedRuns(sessionId)` → queries by `source_session_id` WHERE `completed_at IS NOT NULL AND abandoned_at IS NULL` *(Q22)*
- `recordTimeEntry` / `closeTimeEntry` → insert / update on `session_run_time_entries` *(Q45)*
- `getElapsedSeconds` → queries time entries, sums intervals *(RWK-22)*

Row DTOs use `@SerialName` annotations matching snake_case DB columns, following existing pattern.

---

## 4. Shared Module — Use Cases

*(RWK-19, Q34)*

Wire into `DataFoundation` (extending the existing foundation, not a separate one):

| Use Case | Purpose | Tickets |
|---|---|---|
| `StartSessionRunUseCase` | Generates client UUID, calls repo `startRun` | RWK-13, RWK-20 |
| `GetSessionRunUseCase` | Fetches a single run by ID | RWK-14, RWK-17 |
| `ListActiveRunsUseCase` | Returns active run summaries for Overview banner | RWK-24 |
| `ListCompletedRunsUseCase` | Returns completed run history for a session | Q22 |
| `ToggleStepCompleteUseCase` | Toggles a step's completion state | RWK-15 |
| `OverrideStepClubUseCase` | Changes the club on a specific step | Q15 |
| `UpdateLastViewedStepUseCase` | Persists the user's current step position | RWK-17 |
| `CompleteRunUseCase` | Marks a run as finished, sets end timestamp | RWK-18 |
| `AbandonRunUseCase` | Soft-deletes a run | RWK-18 |
| `RecordTimeEntryUseCase` | Writes enter timestamp for timer tracking | RWK-16 |
| `CloseTimeEntryUseCase` | Writes exit timestamp for timer tracking | RWK-16 |
| `GetElapsedSecondsUseCase` | Computes total elapsed active time | RWK-22 |

---

## 5. Android — ViewModel Architecture

### 5.1 SessionExecutionViewModel (New)

*(RWK-14, Q27, Q47)*

- **Scoped to the execution screen's nav graph** — created on entry, destroyed on leave. Re-fetches run data on every resume. *(Q47)*
- Manages: current step, completion state, timer, club overrides
- State: `MutableStateFlow<SessionExecutionUiState>` exposed as `StateFlow`
- Dependencies: use cases from `DataFoundation` (injected via factory pattern, matching existing ViewModel convention)

**`SessionExecutionUiState`**:
- `run: SessionRun?` — full run data including snapshot
- `currentStepIndex: Int` — which step the user is viewing
- `elapsedSeconds: Long` — computed active time
- `isLoading: Boolean`
- `isTimerRunning: Boolean`
- `statusMessage: String?`

**Key responsibilities:**
- On init: fetch run by ID, restore `lastViewedStepIndex`, start timer (record time entry) *(RWK-17, Q45)*
- `navigateToStep(index)` / `nextStep()` / `previousStep()` — update `currentStepIndex`, persist `lastViewedStepIndex` *(RWK-14)*
- `toggleStepComplete(index)` — call use case, advance to next if completing current *(RWK-15)*
- `overrideClub(stepIndex, clubCode)` — call use case *(Q15)*
- `finishRun()` — close time entry, call `CompleteRunUseCase` *(RWK-18)*
- `abandonRun()` — close time entry, call `AbandonRunUseCase` *(RWK-18)*
- On dispose/leave: close current time entry, persist last viewed step *(Q45, Q47)*
- Timer: tick elapsed seconds while on screen using a coroutine, pause on leave *(non-tech 6.3)*

### 5.2 PracticePlannerViewModel (Extended)

*(Q37, Q38, Q46)*

Add to `PracticePlannerUiState`:
- `activeRuns: List<ActiveRunSummary>` — for Overview banner *(RWK-24)*
- `completedRunHistory: Map<String, List<CompletedRunSummary>>` — session ID → completed runs, for session detail *(Q22)*

Loading behavior:
- **Active runs**: refreshed on every navigation to Overview *(Q46)*
- **Completed run history**: loaded alongside session data *(Q46)*

---

## 6. Android — Navigation

*(RWK-14, Q33)*

### 6.1 New Routes

Add to `RangeworkRoutes`:
- `RunExecution = "runs/{runId}"` — the main execution screen
- Session picker for home FAB: dialog/bottom sheet overlay on Overview (not a separate route)

### 6.2 Route Helpers

- `fun runExecution(runId: String): String = "runs/$runId"`

### 6.3 Navigation Flow

- **Start from session detail** → call `StartSessionRunUseCase` → navigate to `runs/{newRunId}` *(RWK-13)*
- **Start from Overview FAB** → show session picker dialog → call `StartSessionRunUseCase` → navigate to `runs/{newRunId}` *(non-tech Q13, Q21)*
- **Resume from Overview banner** → navigate to `runs/{existingRunId}` *(RWK-17, RWK-24)*
- **Finish/Abandon** → navigate back to Overview *(RWK-18)*

---

## 7. Android — Screens

### 7.1 Session Execution Screen

*(RWK-14)*

The core execution view. Receives state from `SessionExecutionViewModel`.

**Phone layout:**
- `ModalNavigationDrawer` for step list (opened via button or swipe gesture) *(Q36)*
- Main content: current step card with instruction text, ball count, club, notes, focus cue
- Unit context shown: unit title, step position, rep number *(non-tech Q24)*
- Next/Previous navigation buttons *(non-tech 3.2)*
- Top bar: progress overview (completable steps, ball count) *(RWK-16)*
- Actions: Finish (enabled when all complete, always available), Abandon *(RWK-18)*
- `Modifier.keepScreenOn()` applied *(Q35)*

**Tablet layout:**
- Step list permanently mounted on the left side as a panel *(non-tech Q20)*
- Current step detail on the right
- Same controls and actions

**Drawer / step list contents:**
- All steps listed with: unit title grouping, instruction text preview, completion state (checkmark), step number *(non-tech 4.3)*
- Tapping a step jumps to it *(non-tech 3.2)*
- Current step visually highlighted

### 7.2 Session Execution — Progress Header

*(RWK-16)*

Visible at top of execution screen without losing step position:
- Steps completed vs. total (e.g., "12/30")
- Balls hit vs. total
- Per-unit completion indicators
- Elapsed active time (live-updating) *(non-tech 6.3)*
- Overall completion percentage

### 7.3 Run Completion Summary

*(RWK-18)*

Displayed when the user taps "Finish Run":
- Total balls hit
- Elapsed time
- Completion percentage
- Simple stats only — no per-unit breakdown for this release *(non-tech Q12)*
- Navigation back to Overview or session detail

### 7.4 Abandon Confirmation Dialog

*(RWK-18)*

- Simple confirmation dialog: "Are you sure? Progress will be lost." *(non-tech Q25)*
- Confirm → soft-delete run, navigate back
- Cancel → return to execution

### 7.5 Session Detail Screen — Run History Section

*(Q22)*

Add to existing `SessionDetailScreen`:
- Section showing completed past runs for this session
- Each entry: date, elapsed time, completion %
- Listed below existing session content

### 7.6 Session Detail Screen — Start Run Button

*(RWK-13)*

Add to existing `SessionDetailScreen`:
- "Start Session" action (button or FAB)
- Disabled with explanation if session has zero units or zero instructions *(non-tech 1.4)*

### 7.7 Overview Screen — Active Runs Carousel

*(RWK-24, Q14, Q18)*

Add to existing `OverviewScreen`:
- Horizontally scrollable card carousel showing all active runs *(Q18)*
- Each card shows: session name, progress bar, date/time started *(Q50)*
- Tapping a card navigates to `runs/{runId}`

### 7.8 Overview Screen — Start Run FAB

*(non-tech Q13, Q21)*

- FAB on Overview screen
- Opens a session picker dialog/bottom sheet
- Shows list of runnable sessions (non-empty sessions with instructions)
- Selecting a session starts a run and navigates to execution

---

## 8. Android — Components (New)

Reusable composables needed for the execution feature:

| Component | Purpose | Used In |
|---|---|---|
| `ExecutionStepCard` | Displays current step: instruction, ball count, club, notes, focus cue, unit context | Execution screen |
| `StepListDrawerContent` | Scrollable list of all steps with completion state, grouped by unit | Drawer / tablet panel |
| `ExecutionProgressHeader` | Progress stats bar (steps, balls, time, %) | Execution screen top |
| `ActiveRunCard` | Summary card for Overview carousel (name, progress bar, started date) | Overview screen |
| `RunHistoryItem` | Row for completed run in session detail (date, time, %) | Session detail screen |
| `SessionPickerDialog` | Dialog/bottom sheet listing sessions for quick-start | Overview FAB action |
| `StepNavigationBar` | Next/Previous buttons, step counter | Execution screen bottom |
| `CompletionSummaryContent` | Stats display for run finish (balls, time, %) | Completion summary screen |
| `AbandonConfirmDialog` | Simple confirmation dialog for abandoning a run | Execution screen |

---

## 9. Timer Implementation

*(RWK-16, non-tech 6.3, Q30, Q45)*

### 9.1 Behavior

- Timer starts when user enters the execution screen
- Timer keeps running when device screen turns off (app still foregrounded)
- Timer pauses when user navigates away from the execution screen
- Timer resumes when user returns to the execution screen
- Timer is **not** running when the app is backgrounded or killed

### 9.2 Storage

- `session_run_time_entries` table stores (entered_at, exited_at) pairs *(Q30)*
- **On enter**: INSERT a new row with `entered_at = now()`, `exited_at = NULL` *(Q45)*
- **On exit**: UPDATE the open row, setting `exited_at = now()` *(Q45)*
- Open-ended entries (crash before exit) are handled by treating them as "still running" or capping at a reasonable duration

### 9.3 Elapsed Computation

- Total elapsed = sum of all `(exited_at - entered_at)` intervals for the run
- For the currently open entry: add `(now - entered_at)` to the sum
- Client-side computation for live display; server-side computation via use case for summaries *(RWK-22)*

---

## 10. Run Lifecycle

*(RWK-23, non-tech 9, Q16, Q48, Q49)*

### 10.1 Status Inference

No explicit state column. Status is inferred from data:
- **Active**: `completed_at IS NULL AND abandoned_at IS NULL`
- **Completed**: `completed_at IS NOT NULL`
- **Abandoned**: `abandoned_at IS NOT NULL` (soft-deleted, excluded from all user-facing queries)

### 10.2 Completion

- User manually taps "Finish Run" — available at any time, shown prominently when all steps are done *(Q49)*
- Sets `completed_at` timestamp
- Run moves to completed history

### 10.3 Abandonment

- User taps "Abandon" → simple confirmation dialog → sets `abandoned_at` timestamp *(Q48)*
- Run excluded from active list and history (soft delete)

### 10.4 Concurrency

- Multiple runs can be active simultaneously, including multiple runs of the same session template *(non-tech 2.1)*
- No concurrency enforcement or "replace" prompts

---

## 11. Connectivity

*(non-tech 13, Q6)*

- **Online-only** for this release
- All operations (start, toggle, override, finish, abandon, timer writes) require network
- No offline queue, no optimistic local writes, no conflict resolution
- Deferred to future release: offline support, write queuing, replay on reconnect

---

## 12. Tests

### 12.1 Shared Module Tests

Follow existing patterns (`shared/src/commonTest/`):

- Hand-written fakes for `SessionRunRepository`
- Use case tests: verify `StartSessionRunUseCase` generates UUID and calls repo, `ToggleStepCompleteUseCase` delegates correctly, progress computation helpers handle edge cases (zero balls, empty sessions, all complete)
- Progress helper tests: `completedBalls()`, `totalBalls()`, `completionPercentage()`, `isFullyComplete()`, division-by-zero guards

### 12.2 Android ViewModel Tests

Follow existing patterns (`androidApp/src/test/`):

- `SessionExecutionViewModelTest` using `MainDispatcherRule` + fake repository
- Test: load run on init, step navigation, toggle completion + auto-advance, finish run, abandon run
- `PracticePlannerViewModelTest` extended: active runs loading, completed run history loading

### 12.3 Supabase RPC Tests

- The `start_session_run` RPC is best validated via integration testing or manual verification against the local Supabase instance
- Verify: snapshot contains expanded steps, correct ordering, correct club resolution, empty-session rejection

---

## 13. Template Deletion Interaction

*(non-tech 12, Q23)*

- `source_session_id` FK uses `ON DELETE SET NULL` — when a template is deleted, the run's `source_session_id` becomes null
- `session_name` column preserves the name from the snapshot regardless
- The planning UI shows a warning before deleting a template that has active runs (check via query), but does not block deletion
- Active runs continue functioning — the snapshot is self-contained
- No indication on the run or history that the source template was deleted

---

## Ticket Cross-Reference

| Technical Area | Primary Tickets | Clarified In |
|---|---|---|
| Schema & migration | RWK-19 | Q28, Q29, Q30, Q39, Q40, Q42, Q43, Q44 |
| Snapshot RPC | RWK-20 | Q31, Q32, Q41 |
| Repository & sync | RWK-21 | Q34 |
| Progress computation | RWK-22 | — |
| Run lifecycle | RWK-23 | Q16, Q48, Q49 |
| Concurrency & routing | RWK-24 | Q33, Q37, Q46 |
| ViewModel architecture | RWK-14 | Q27, Q47 |
| Execution screen | RWK-14 | Q35, Q36 |
| Start action | RWK-13 | Q33 |
| Completion / abandon | RWK-18 | Q48, Q49, Q50 |
| Timer | RWK-16 | Q30, Q45 |
| Active run banner | RWK-24 | Q37, Q46, Q50 |
| Run history | — | Q22, Q38 |
| Template deletion | — | Q23 |
