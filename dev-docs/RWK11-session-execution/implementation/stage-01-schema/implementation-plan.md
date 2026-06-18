# Stage 1: Database Schema & Migration

## Objective

Create all Supabase database objects required for the Range Sessions feature in a single migration file: the `range_sessions` table, `range_session_time_entries` table, indexes, RLS policies, triggers, and the `start_range_session` RPC. After this stage the database is ready for the feature with no application changes yet.

**Tickets:** RWK-19, RWK-20

## Dependencies

- None. This is the first stage and has no prerequisites.
- Requires access to local Supabase instance for testing the migration.
- Depends on existing schema objects: `profiles.id` (FK target), `practice_sessions.id` (FK target), `practice_session_items`, `practice_units`, `practice_unit_instructions` (read by the RPC), `public.set_updated_at()` (trigger function).

## Affected Screens

None. This stage is database-only.

## Likely Files

### New files

| File | Purpose |
|---|---|
| `supabase/migrations/YYYYMMDDHHMMSS_range_sessions.sql` | Single migration file containing all schema objects |

### Existing files referenced (not modified)

| File | Purpose |
|---|---|
| `supabase/migrations/20260615132000_phase3_data_foundation.sql` | Reference for table/RLS/trigger patterns |
| `supabase/migrations/20260616200000_atomic_planning_save_rpcs.sql` | Reference for RPC patterns (grants, parameter conventions) |
| `supabase/config.toml` | Supabase project configuration |

## New Components Required

### `range_sessions` table

All columns per tech requirements 1.1:

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | PK, default `gen_random_uuid()` |
| `owner_id` | `uuid` | NOT NULL, default `auth.uid()`, FK → `profiles.id` ON DELETE CASCADE |
| `source_session_id` | `uuid` | FK → `practice_sessions.id` ON DELETE SET NULL |
| `session_name` | `text` | NOT NULL |
| `snapshot` | `jsonb` | NOT NULL |
| `snapshot_version` | `integer` | NOT NULL, default `1` |
| `completed_steps` | `jsonb` | NOT NULL, default `'[]'::jsonb` |
| `club_overrides` | `jsonb` | NOT NULL, default `'{}'::jsonb` |
| `last_viewed_step_index` | `integer` | nullable |
| `started_at` | `timestamptz` | NOT NULL, default `now()` |
| `completed_at` | `timestamptz` | nullable |
| `abandoned_at` | `timestamptz` | nullable |
| `created_at` | `timestamptz` | NOT NULL, default `now()` |
| `updated_at` | `timestamptz` | NOT NULL, default `now()` |

### `range_session_time_entries` table

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | PK, default `gen_random_uuid()` |
| `range_session_id` | `uuid` | NOT NULL, FK → `range_sessions.id` ON DELETE CASCADE |
| `entered_at` | `timestamptz` | NOT NULL |
| `exited_at` | `timestamptz` | nullable |

### Indexes

- `range_sessions_owner_active_idx` on `(owner_id, started_at DESC)` WHERE `completed_at IS NULL AND abandoned_at IS NULL`
- `range_sessions_source_session_idx` on `(source_session_id, completed_at DESC)` WHERE `completed_at IS NOT NULL AND abandoned_at IS NULL`

### RLS policies

- `range_sessions`: ALL operations gated by `auth.uid() = owner_id`
- `range_session_time_entries`: ALL operations gated by EXISTS subquery checking parent `range_sessions.owner_id = auth.uid()`

### Trigger

- `set_range_sessions_updated_at` BEFORE UPDATE on `range_sessions` → `public.set_updated_at()`

### `start_range_session` RPC

- Signature: `start_range_session(p_range_session_id uuid, p_session_id uuid) RETURNS jsonb`
- Security: `SECURITY INVOKER`
- Grants: `TO authenticated`
- Behavior:
  1. Read `practice_sessions` row by `p_session_id` (respects caller's RLS)
  2. Read `practice_session_items` for the session, ordered by `sort_order`
  3. For each item, read `practice_units` and `practice_unit_instructions` (ordered by `sort_order`)
  4. Build the snapshot JSONB (version 1 schema): `units` array preserving template structure, `steps` array expanding repeat counts into discrete entries
  5. Validate the resulting `steps` array is non-empty — raise exception if empty
  6. INSERT `range_sessions` row with `id = p_range_session_id`, `owner_id = auth.uid()`, snapshot, `session_name` from practice session
  7. Return the full inserted row as JSONB (including `id`, `snapshot`, all timestamps, etc.)

#### Snapshot JSONB structure (version 1)

```json
{
  "sessionNotes": "...",
  "units": [
    {
      "unitTitle": "...", "unitNotes": "...", "unitFocus": "...",
      "itemNotes": "...", "itemFocusCue": "...",
      "club": "pitching_wedge", "clubDisplayName": "Pitching Wedge",
      "repeatCount": 3,
      "instructions": [{ "text": "...", "ballCount": 10 }]
    }
  ],
  "steps": [
    {
      "unitIndex": 0, "instructionIndex": 0,
      "repNumber": 1, "totalReps": 3,
      "instructionText": "...", "ballCount": 10,
      "club": "pitching_wedge", "clubDisplayName": "Pitching Wedge",
      "unitTitle": "...", "notes": "...", "focusCue": "..."
    }
  ]
}
```

#### Step expansion logic

For each practice session item (ordered by `sort_order`):
- Look up the practice unit and its instructions (ordered by `sort_order`)
- For each instruction, emit `repeatCount` steps (rep 1 through N)
- Steps carry: unit index (position of the item in the session), instruction index (position within the unit), rep number, total reps, and all display fields (text, ball count, club, notes, focus cue)

#### Edge cases the RPC must handle

- Practice session not found or not owned by caller → RLS blocks read, RPC gets no row → raise descriptive exception
- Session has zero items, or all items reference units with zero instructions → steps array is empty → raise exception ("Session has no instructions")
- Items reference a unit that the caller doesn't own (shouldn't happen) → RLS blocks read → unit is omitted, potentially resulting in empty steps → caught by the non-empty validation
- Club fields may be null (unit has no club assigned) → preserve nulls in snapshot
- Notes/focus cue fields may be null → preserve nulls
- Ball count may be null → preserve nulls
- `p_range_session_id` collision (UUID already exists) → PK violation → let Postgres raise the error naturally

## Validation Checklist

- [ ] Migration applies cleanly to local Supabase (`supabase db reset` or `supabase migration up`)
- [ ] `range_sessions` table exists with all columns and correct types
- [ ] `range_session_time_entries` table exists with all columns
- [ ] Foreign keys work: inserting with invalid `owner_id` fails, inserting with invalid `source_session_id` fails
- [ ] `ON DELETE SET NULL` works: deleting a practice session sets `source_session_id` to null on existing range sessions
- [ ] `ON DELETE CASCADE` works: deleting a profile cascades to range sessions; deleting a range session cascades to time entries
- [ ] Partial indexes exist (verify via `\di` or Supabase Studio)
- [ ] RLS policies enforce ownership: user A cannot read/write user B's range sessions or time entries
- [ ] `updated_at` trigger fires on UPDATE
- [ ] `start_range_session` RPC: call with a valid practice session → returns full row JSONB with correct snapshot
- [ ] RPC snapshot has correct step count (instructions × repeat counts)
- [ ] RPC snapshot preserves correct ordering (items by sort_order, instructions by sort_order, reps 1..N)
- [ ] RPC snapshot includes null fields where source data is null (clubs, notes, ball counts)
- [ ] RPC rejects empty sessions (no units or no instructions) with a descriptive error
- [ ] RPC uses caller's UUID for the range session ID
- [ ] RPC is callable by `authenticated` role only
- [ ] `snapshot_version` is set to `1` on created rows
- [ ] `completed_steps` defaults to `[]` and `club_overrides` defaults to `{}`
- [ ] Time entry insert works with `entered_at` only (null `exited_at`)
- [ ] Time entry update to set `exited_at` works

## Accessibility Requirements

Not applicable — this stage has no UI.

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Migration fails on existing data | Low | New tables only — no ALTER on existing tables. Safe for all environments. |
| RPC reads cross existing RLS boundaries incorrectly | Low | Uses `SECURITY INVOKER` — respects caller's RLS grants. Test with multiple users. |
| Existing `set_updated_at()` function assumed but missing | Very Low | Already used by multiple existing tables — verify it exists before writing migration. |
| `ON DELETE SET NULL` on `source_session_id` affects existing practice session deletion flows | Low | Practice session deletion already works; the new FK adds a SET NULL side effect that is invisible to existing code. Verify deletion still works. |
| Large snapshot JSONB for sessions with many units and high repeat counts | Very Low | Postgres handles JSONB well. No practical limit expected for normal use. |
