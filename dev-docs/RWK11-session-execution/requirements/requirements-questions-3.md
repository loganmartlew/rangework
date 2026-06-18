# Technical Requirements Questions — Round 3

Based on codebase survey of shared module, Android app, and Supabase schema.

---

## Q27: ViewModel Architecture (RWK-13, RWK-14)

The existing app uses a single `PracticePlannerViewModel` for all planning state. Session execution is a substantially different concern (runtime progress vs. CRUD editing).

Options:

- **New dedicated ViewModel**: A `SessionExecutionViewModel` owns all run state (active runs, current step, completion, timer). PracticePlannerViewModel stays untouched.
- **Extend PracticePlannerViewModel**: Add run state into the existing monolithic ViewModel.

Recommendation: New ViewModel — it's a different lifecycle and concern. But your call.

**Answer:** New ViewModel

---

## Q28: Snapshot Storage — Denormalized JSON vs. Relational Tables (RWK-19, RWK-20)

The snapshot needs to store a frozen copy of units, instructions, clubs, notes, focus cues, repeat counts, and ball allocations. Two approaches:

- **Denormalized JSON column**: Store the entire snapshot as a single JSONB blob on the `session_runs` row. Simple to write, simple to read, naturally immutable. Matches the "frozen at creation time" requirement.
- **Relational child tables**: Create `session_run_units`, `session_run_instructions` etc. mirroring the planning tables. More complex, but enables server-side queries on individual steps.

The existing schema uses relational tables for planning data, but the snapshot is fundamentally different — it's write-once, read-many, never updated (except per-step club override and completion state).

Which approach do you prefer?

**Answer:** Denormalized JSON column is ok. Schemas for the JSON should be versioned in case they need to change in future.

---

## Q29: Completion State Storage (RWK-15, RWK-19)

Per-step completion needs to be written on every toggle. Options:

- **Separate `session_run_step_completions` table**: One row per completed step (step_index, completed_at). Completion = insert row, un-completion = delete row. Atomic per-step writes.
- **Array/JSONB on the run row**: A `completed_steps` array or JSONB map on the run itself. Simpler schema, but concurrent writes to the same row risk conflicts.
- **If using relational snapshot (Q28)**: A `completed` boolean column on the step rows.

Given online-only and single-device typical use, the conflict risk is low. Preference?

**Answer:** Array/JSONB

---

## Q30: Timer Elapsed-Time Storage (RWK-16, RWK-22)

Elapsed time is timestamp-based (recording enter/exit events). Storage options:

- **`session_run_time_entries` table**: Rows of (run_id, entered_at, exited_at). Client records entry/exit. Total time = sum of intervals.
- **Fields on the run row**: `accumulated_seconds` (integer) + `timer_started_at` (timestamp, null when not on screen). Client updates on enter/exit.
- **Client-only**: Compute from local timestamps, store only the final elapsed time on completion.

Recommendation: Entries table is cleanest for timestamp-based tracking and survives app crashes mid-session. But it adds write frequency.

**Answer:** Entries table

---

## Q31: Run Identification — UUID Generation (RWK-19)

The existing schema generates UUIDs server-side (`gen_random_uuid()` default). For runs, the client needs the run ID immediately after creation to navigate to the execution screen.

Options:

- **Client-generated UUID**: Generate UUID in Kotlin before the insert. The existing `save_practice_unit` RPC accepts a client-provided ID. Follow the same pattern.
- **Server-generated UUID**: Insert, then return the generated ID. Requires an RPC that returns the new row.

The existing pattern is client-generated (save RPCs accept `p_unit_id`, `p_session_id`). Follow that pattern?

**Answer:** Yes

---

## Q32: Snapshot Creation — RPC or Client-Side (RWK-20)

Snapshot generation (deep-copying session template into run data, expanding repeats) can happen:

- **Client-side in Kotlin**: The shared module fetches the session + units, builds the snapshot in memory, then writes the full snapshot to Supabase. Logic is testable in shared tests.
- **Server-side RPC**: A Postgres function accepts a session ID, reads the template, and creates the run + snapshot atomically. Guarantees consistency but is harder to test and maintain.

The existing save RPCs are server-side, but they receive the data from the client — they don't read other tables to build the payload.

**Answer:** Server-side RPC

---

## Q33: Navigation Route Structure (RWK-14, RWK-17)

The existing routes follow `entity/{id}` and `entity/{id}/edit`. For session runs:

- **`runs/{runId}`**: The execution screen for a specific run. Entered from Overview banner or after starting a new run.
- **Should runs be nested under sessions?** e.g., `sessions/{sessionId}/runs/{runId}` — more RESTful but the run is independent of the template after creation.
- **Session picker for home FAB**: A new route like `runs/start` that shows a session picker, or a dialog/bottom sheet overlay on Overview?

**Answer:** Use whatever strategy suits the features being built the best.

---

## Q34: Shared Module Layering for Runs (RWK-19, RWK-21)

The shared module follows the pattern: model → repository interface → Supabase implementation → use case → foundation.

For runs, should we follow the identical layering?

- New models: `SessionRun`, `SessionRunStep`, `SessionRunSnapshot`
- New repository: `SessionRunRepository` interface + `SupabaseSessionRunRepository`
- New use cases: `StartSessionRunUseCase`, `ToggleStepCompleteUseCase`, `AbandonRunUseCase`, `CompleteRunUseCase`, `ListActiveRunsUseCase`, `GetSessionRunUseCase`, etc.
- Wire into `DataFoundation` (or a new `ExecutionFoundation`)

Or is a lighter-weight approach preferred?

**Answer:** Same layering

---

## Q35: Keep-Awake Implementation (RWK-14)

Android keep-awake can be done via:

- **`FLAG_KEEP_SCREEN_ON`** on the Activity window — simplest, but affects the whole Activity
- **`Modifier.keepScreenOn()`** (Compose) — scoped to the composable's lifecycle, screen stays on only while the execution screen is displayed

The composable modifier approach is more targeted. Preference?

**Answer:** Modifier.keepScreenOn()

---

## Q36: Side Drawer Implementation (RWK-14, Q20)

For the step-list drawer on phone, Compose offers:

- **`ModalNavigationDrawer`**: Standard Material 3 side drawer. Opens via gesture or button, overlays content.
- **`ModalBottomSheet`**: Slides up from bottom. More natural for mobile step lists.
- **Custom drawer composable**: Full control over layout and animation.

On tablet, the permanent panel would be a simple `Row` layout with the list on the left.

Which approach for the phone drawer?

**Answer:** ModalNavigationDrawer

---

## Q37: Active Runs on Overview — Data Loading (RWK-24)

Active runs need to be loaded on app open. Options:

- **Load in PracticePlannerViewModel**: Add active runs to the existing `PracticePlannerUiState`. This ViewModel already loads on auth state change.
- **Load in the new ExecutionViewModel**: Separate concern, but the Overview screen would need to observe two ViewModels.
- **Shared data source**: Both ViewModels read from a shared repository/flow.

The current architecture has one ViewModel per concern area (Planner, Auth, Settings). What's your preference?

**Answer:** Load in PracticePlannerViewModel

---

## Q38: Run History on Session Detail (Q22)

Completed runs are viewable on the session detail screen. This means the session detail screen needs to fetch run history for that session.

- **Add to PracticePlannerViewModel**: Fetch runs alongside session data. The detail screen already receives `PracticePlannerUiState`.
- **Separate fetch in ExecutionViewModel**: Session detail observes both ViewModels.
- **On-demand load**: Only fetch run history when the session detail screen is navigated to.

**Answer:** Add to PracticePlannerViewModel

---

## Q39: Supabase Schema — RLS Pattern for Runs (RWK-19, RWK-21)

The existing schema enforces ownership via `owner_id` columns and `auth.uid()` checks. Runs will follow the same pattern:

- `session_runs` table with `owner_id` + RLS policy `auth.uid() = owner_id`
- Child tables (completions, time entries) use EXISTS subqueries to check parent run ownership

Any reason to deviate from this pattern?

**Answer:** Don't deviate.

---

## Q40: Migration Strategy (RWK-19)

The existing schema has 9 migrations applied sequentially. For the session run feature:

- **Single migration**: One migration file adds all new tables, indexes, RLS policies, and RPCs for runs.
- **Multiple migrations**: Split across logical groups (schema, RLS, RPCs) like the existing pattern.

The existing pattern did both — early migrations were combined, later ones were split. Preference?

**Answer:** Single migration

---
