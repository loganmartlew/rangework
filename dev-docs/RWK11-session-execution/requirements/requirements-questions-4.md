# Technical Requirements Questions — Round 4

Follow-ups based on Round 3 answers and deeper architectural implications.

---

## Q41: Server-Side Snapshot RPC — Scope & Return Value (from Q32)

You chose server-side RPC for snapshot creation. The RPC will accept a session ID, read the template + units, expand repeats, and insert the run + snapshot.

- Should the RPC **return the full run row** (including the JSONB snapshot) so the client can immediately display it? Or should the client do a separate fetch after creation?
- The RPC needs to read `practice_sessions`, `practice_session_items`, `practice_units`, and `practice_unit_instructions` to build the snapshot. It will run as `SECURITY DEFINER` to bypass RLS for reading template data, or as `SECURITY INVOKER` relying on the caller's RLS grants (the user owns all these rows anyway). Preference?

**Answer:** return the full run row. I'm unsure about the RLS. Use whatever will be most secure while still allowing access to what the user needs

---

## Q42: JSONB Snapshot Schema Versioning (from Q28)

You want versioned JSON schemas for the snapshot. Implementation options:

- **Version field on the run row**: A `snapshot_version` integer column (e.g., `1`). The client checks the version and uses the appropriate deserializer.
- **Version inside the JSONB**: The snapshot blob itself contains a `"version": 1` field.

The version-on-row approach is queryable without parsing JSON. Recommendation: version field on the row. Agree?

**Answer:** Version field on the run row

---

## Q43: Completed Steps JSONB Structure (from Q29)

You chose JSONB for completion state on the run row. Structure options:

- **Array of step indices**: `[0, 3, 7]` — list of completed step indices. Simple, small.
- **Array of objects**: `[{"stepIndex": 0, "completedAt": "..."}, ...]` — includes timestamps per step.
- **Map of index to metadata**: `{"0": {"completedAt": "..."}, "3": {"completedAt": "..."}}` — fast lookup by index.

Do you want completion timestamps per step, or just a set of completed indices?

**Answer:** Array of objects

---

## Q44: Club Override Storage (from Q15, Q28)

Per-step club override needs to persist. Since the snapshot is JSONB:

- **Separate JSONB column**: A `club_overrides` JSONB on the run row, e.g., `{"3": "pitching_wedge", "7": "nine_iron"}` mapping step index to club code.
- **Inside the snapshot JSONB**: Each step in the snapshot gets a mutable `clubOverride` field alongside the original `club` field.
- **Separate column (non-JSON)**: Not practical since it's per-step.

Since the snapshot is meant to be immutable, storing overrides separately keeps that contract clean. Preference?

**Answer:** Separate JSONB column

---

## Q45: Time Entries Table — Write Timing (from Q30)

You chose a `session_run_time_entries` table. When should the client write entries?

- **On enter/exit**: Write a new row with `entered_at` on screen enter, then UPDATE the same row with `exited_at` on screen exit. Risk: if the app crashes before writing `exited_at`, that entry is open-ended.
- **On exit only**: Write a complete (entered_at, exited_at) row when the user leaves the execution screen. The client holds timestamps in memory. Risk: app crash loses the current interval.
- **Heartbeat**: Periodically update the current entry's `exited_at` (e.g., every 30s). Crash recovery loses at most 30s.

**Answer:** On enter/exit

---

## Q46: PracticePlannerViewModel — Active Runs Loading Scope (from Q37)

You chose to load active runs in `PracticePlannerViewModel`. Clarifying scope:

- Should PlannerViewModel load **all active runs** (for the Overview banner), or just a summary (run IDs, session names, progress %)?
- Should PlannerViewModel also load **completed run history** per session (for Q38 session detail), or only active runs?
- When should active runs refresh? On every navigation to Overview, or only on auth state change (like the current planning data)?

**Answer:** just a summary, completed run history, every navigation to Overview

---

## Q47: ExecutionViewModel — Lifecycle & Scoping

The new `SessionExecutionViewModel` manages a single run's execution state (current step, timer, completions). Questions:

- Should it be **scoped to the execution screen's nav graph** (created when entering, destroyed when leaving)? This means re-fetching run data on every resume.
- Or should it be **scoped to the Activity** (like PracticePlannerViewModel), surviving navigation? This keeps the run in memory across navigation but the ViewModel lives for the whole app session.

The timer needs to track enter/exit of the execution screen either way.

**Answer:** if feasible, scoped to the execution screen's nav graph

---

## Q48: Abandon — Deletion Strategy (from non-tech Q3, Q25)

Abandoned runs are not stored. When the user confirms abandon:

- **Hard delete**: Delete the `session_runs` row and cascade to time entries. Permanent.
- **Soft delete**: Mark status as `abandoned` and exclude from queries. Data stays for potential recovery/analytics.

The non-technical requirement says "abandoned runs are deleted." But soft delete is safer and trivially cheap. Preference?

**Answer:** Soft delete, using an 'abandoned_at' timestamp or similar

---

## Q49: "Completing" a Run — Trigger (from non-tech Req 8.1)

When all steps are marked complete, the user gets a "Finish session" action. Clarifying:

- **Auto-detect**: When the last step is toggled complete, automatically show a finish dialog/screen.
- **Manual**: The user must explicitly tap "Finish Run" (available at any time, but shown prominently once all steps are done). This allows them to un-mark steps before finishing.

Which behavior?

**Answer:** Manual

---

## Q50: Overview Banner — What Data to Show Per Run Card

The active run cards on the Overview screen need to display enough for the user to identify and resume a run. What should each card show?

Candidates:

- Session name (from snapshot)
- Progress (e.g., "12/30 steps" or a progress bar)
- Elapsed time
- Date/time started
- Current unit name

Which of these?

**Answer:** Session name, progress (prefer a bar), Date/time started

---
