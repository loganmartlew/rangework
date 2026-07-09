# Stage 1: Schema & Snapshot v3

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md)
**Status:** approved 2026-07-09 — Decisions D1–D3 and the snapshot v3 shape signed off; ready for implementation

## Objective

Land every database object the Range Session Data Recording design needs in a single
migration, plus the RPC changes that write and snapshot them. After this stage the database
fully supports the feature; no application code changes ship. Unused columns/tables are inert
until Stages 3–5 give them UI, and Stage 7 gives them MCP surface.

Covers: `success_criterion` on units, `observation_types` on session items, `handedness` on
preferences, Session Note + Block Result storage on range sessions, the per-ball Observations
table, snapshot v3 in `start_range_session`, and matching `save_practice_unit` /
`save_practice_session` extensions.

## Dependencies

- None upstream — first stage of the epic.
- Local Supabase instance for validating the migration and exercising the RPCs.
- Existing objects read or modified: `practice_units`, `practice_session_items`,
  `user_preferences`, `range_sessions`, `public.set_updated_at()`, `audit.log_change()`,
  `save_practice_unit`, `save_practice_session`, `start_range_session`.

## Decisions (approved by owner, 2026-07-09)

The epic flagged three Stage-1 decisions. The recommendations below were reviewed and
approved as written; the rationale stays as the decision record.

### D1 — Block Result storage: JSONB column on `range_sessions`

Add `block_results jsonb not null default '{}'` to `range_sessions`, keyed by **unitIndex as
a string** (block identity = Session Item = snapshot unit entry, per `ExecutionBlocks.kt`),
value an object with camelCase keys matching `completed_steps` style:

```json
{ "0": { "note": "left misses all day", "manualCount": 4 } }
```

Both fields optional — a note-only result, a count-only result, or both are all legal shapes.

**Why JSONB, not a table:** bounded cardinality (one entry per block; sessions have a handful
of blocks), no independent lifecycle or identity, always read and written alongside the
session row the block screen already holds, and it mirrors `club_overrides` exactly (the
pattern the epic predicted). The design explicitly needs no analytics queries in v1 (§8), so
SQL-level queryability buys nothing. A table remains an additive refactor if trends UI ever
needs one.

**Alternative (rejected for v1):** `range_session_block_results` table — adds per-block audit
rows and joins for a structure with at most ~10 entries per session.

### D2 — Per-ball Observations: separate table

New table `range_session_observations`, one row per observed ball:

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | PK, default `gen_random_uuid()` |
| `range_session_id` | `uuid` | NOT NULL, FK → `range_sessions.id` ON DELETE CASCADE |
| `step_index` | `integer` | NOT NULL, CHECK `>= 0`; global snapshot step index (Ball Step identity, ADR 0004) |
| `observed_values` | `jsonb` | NOT NULL, default `'{}'`, CHECK `jsonb_typeof(observed_values) = 'object'` — map of observation type id → canonical value string |
| `created_at` / `updated_at` | `timestamptz` | NOT NULL defaults; `set_updated_at()` trigger |

Plus: `UNIQUE (range_session_id, step_index)` (doubles as the lookup index), RLS via parent
ownership (same EXISTS pattern as `range_session_time_entries`), `authenticated` role grants,
and an `audit.log_change()` trigger (consistent with every other user-mutable table).

**Why a table, not JSONB on the session row:** the epic already noted the MCP raw per-ball
read and the correction flow favour a table; the write pattern seals it. Capture is one
insert per ball during live use — small, append-only writes. A JSONB column would be
rewritten wholesale on every +1, *on the same row* the same tap is already updating for
`completed_steps`, forcing the app to serialize two read-modify-write cycles per tap. A
100-ball session with 2–3 enabled types is a trivial 100 rows but an unwieldy blob.
Corrections update one row by `(range_session_id, step_index)`; the −1 sweep voids a ball by
deleting its row; session deletion cascades.

**Why one row per ball with a values map, not one row per (ball, type):** the ball's record
is the commit unit in the design (auto-commit when all enabled types have values; +1 commits
a partial or empty record). One row per ball matches that lifecycle, and keeps the DB
agnostic to per-type value encodings — Stage 2 owns the canonical encoding proposal; the DB
never validates value strings. Keys of `observed_values` are the type identifiers below;
values are opaque text at this layer.

Semantics: **no row and an empty `{}` row mean the same thing — unobserved.** Denominators
are always computed per type from rows that carry a value for that type. The app may skip
writing empty records; the schema permits either. The DB does not enforce that `step_index`
addresses a Ball Step (snapshot-dependent; app-level rule, tested in Stage 2).

### D3 — Freeze enforcement: app-level only

Active = editable; Completed = prose editable, counts and Observations frozen; Abandoned =
locked. Enforce all of this in shared KMP logic (Stage 2) with unit tests, not in the DB.

**Why:** consistent with existing completed-session handling — `completed_steps` and
`club_overrides` immutability is already app-guarded only. The rule is sub-field-grained
(the `note` inside a `block_results` entry stays editable after Completion while
`manualCount` beside it freezes), which a trigger would have to express as JSONB diffing —
complex, brittle, and protecting a single-user, RLS-owner-scoped dataset against nothing but
our own app bugs. Stage 2's tested use-case layer is the right home for the invariant; a DB
trigger stays available as an additive hardening migration later.

### Fixed in this plan (approved alongside D1–D3)

- **Observation Type identifiers:** `success`, `strike_location`, `contact`, `shape`,
  `distance`, `direction` — lowercase snake_case (matches `club_code` style). These strings
  appear in `observation_types`, snapshot v3, and `observed_values` keys, and become the
  Stage 2 enum wire values.
- **`observation_types` column type:** `text[] not null default '{}'` on
  `practice_session_items`, with CHECK
  `observation_types <@ array['success','strike_location','contact','shape','distance','direction']::text[]`.
  Native array beats JSONB here: the containment check is one expression, and PostgREST
  serializes it as a JSON array anyway. Adding a future type is a one-line constraint
  migration shipped with the app change that introduces it — acceptable, since the vocabulary
  is fixed app vocabulary by design.
- **`handedness`:** `text not null default 'RIGHT'` on `user_preferences`, CHECK
  `in ('RIGHT','LEFT')` — matches the existing uppercase preference-enum style, and RIGHT
  matches the app's current implicit rendering.
- **`success_criterion`:** nullable `text` on `practice_units`, CHECK null-or-trimmed-non-empty
  (matches the `title` check pattern). Save RPC normalizes blank → null.
- **`session_note`:** nullable `text` on `range_sessions`. Singular, distinct from the
  snapshot's existing planning-time `sessionNotes`.
- **Success-requires-criterion, two layers:** `save_practice_session` raises an exception if
  an item enables `success` while its unit has no criterion (mirrors the app/MCP rule at the
  atomic boundary). `start_range_session` *additionally* filters `success` out of the
  snapshot's enabled types when the criterion is null at start time — the criterion can be
  legitimately removed from the unit after the session was saved, and the snapshot must stay
  self-consistent (derived counts are meaningless without a rubric).

## Likely files

### New

| File | Purpose |
|---|---|
| `supabase/migrations/20260709120000_range_session_data_recording.sql` | Single migration: all columns, the observations table, RLS/grants/triggers, both save RPCs, `start_range_session` v3 |

### Referenced (not modified)

| File | Purpose |
|---|---|
| `supabase/migrations/20260703120000_snapshot_v2_ball_granular_steps.sql` | Base version of `start_range_session` to extend |
| `supabase/migrations/20260626120000_tags.sql` | Current `save_practice_session`; drop-and-recreate precedent for signature changes |
| `supabase/migrations/20260630120000_per_instruction_club.sql` | Current `save_practice_unit` |
| `supabase/migrations/20260618120000_range_sessions.sql` + `...130000_range_sessions_role_grants.sql` | Table/RLS/grant patterns for the new table |
| `supabase/migrations/20260621100100_audit_log_triggers.sql` | Audit trigger pattern |
| `apps/mobile/shared/.../model/ExecutionBlocks.kt`, `RangeSession.kt` | Block identity (unitIndex) and row-decoding compatibility (read-only reference) |

No Kotlin or MCP code changes in this stage.

## Migration contents

In one file, in this order:

1. **`practice_units.success_criterion`** — nullable text + check constraint.
2. **`practice_session_items.observation_types`** — `text[] not null default '{}'` + vocabulary
   check constraint.
3. **`user_preferences.handedness`** — `text not null default 'RIGHT'` + check constraint
   (existing rows pick up the default).
4. **`range_sessions.session_note`** (nullable text) and **`range_sessions.block_results`**
   (`jsonb not null default '{}'`).
5. **`range_session_observations`** table per D2: table, unique constraint, RLS enable +
   policy, `set_updated_at` trigger, `audit_range_session_observations` trigger,
   `grant select, insert, update, delete ... to authenticated`.
6. **`save_practice_unit`** — `drop function if exists` the current 7-arg signature
   (`uuid, text, text, text, text, jsonb, uuid[]`), recreate with trailing
   `p_success_criterion text default null` (defaults must trail, so it sits after
   `p_tag_ids`). Insert/update `success_criterion = nullif(trim(p_success_criterion), '')`.
   Re-grant execute (the drop removes grants). Named-parameter callers (app, MCP) that omit
   the new arg keep working via the default — but the old signature **must** be dropped or
   those same calls become ambiguous between overloads.
7. **`save_practice_session`** — signature unchanged (`observation_types` rides inside each
   `p_items` element). Item insert gains
   `coalesce((select array_agg(...) from jsonb_array_elements_text(item->'observation_types')), '{}')`;
   missing key → empty array. Add the success-requires-criterion validation: after insert,
   raise if any inserted item has `'success' = any(observation_types)` and its unit's
   `success_criterion` is null. Recreate via `create or replace` (same signature; grants
   survive).
8. **`start_range_session` v3** — `create or replace` (same signature). Header comment
   documents v3 the way the v2 migration documents ADR 0004.

### Snapshot v3 shape

Only the unit entries change; the `steps` array shape, expansion rules (ADR 0004), and
ordering are untouched from v2. Each unit entry gains two keys:

```json
{
  "sessionNotes": "...",
  "units": [
    {
      "unitTitle": "...", "unitNotes": "...", "unitFocus": "...",
      "itemNotes": "...", "itemFocusCue": "...",
      "repeatCount": 3,
      "instructions": [{ "text": "...", "ballCount": 10, "club": "...", "clubDisplayName": "..." }],
      "successCriterion": "inside 5m of the 60m flag",
      "observationTypes": ["success", "shape"]
    }
  ],
  "steps": [ ...unchanged v2 step objects... ]
}
```

- `successCriterion`: the unit's criterion **in force at start** (null when absent) — editing
  the unit later never reinterprets this session.
- `observationTypes`: the item's enabled types, with `success` filtered out when
  `successCriterion` is null (see two-layer rule above). Empty array when nothing enabled.
- `snapshot_version` inserted as **3 for every new session**, including sessions using none
  of the new features — the version describes the shape, not the usage. Feature detection in
  the app is by version (≥ 3), per the epic. v1/v2 rows are never migrated or touched.

## Backward / forward compatibility

The critical property: **this migration ships before any app change (Stage 2+), so the
current app must keep working against the migrated database.** Checked against the code:

- `getSession` does `select *` → `decodeList<RangeSession>()`. supabase-kt's default
  `KotlinXSerializer` uses `ignoreUnknownKeys = true`, so the new `session_note` /
  `block_results` row columns and the new snapshot unit keys are ignored;
  `snapshotVersion: Int` accepts 3. `RangeSession.kt` needs no change.
- `to_jsonb(rs.*)` from the RPC gains the new columns — same tolerance applies (and the app
  discards the RPC return, re-fetching via `getSession`).
- MCP `create_unit`/`create_session` call the save RPCs with named params via PostgREST —
  unaffected by a trailing defaulted param once the old `save_practice_unit` signature is
  dropped.
- The on-device smoke test in the checklist is the go/no-go gate for merging Stage 1 ahead
  of Stage 2.

## Edge cases

- Unit with blank/whitespace criterion → stored as null; an item enabling `success` against
  it → save rejected.
- Criterion removed from unit after a session enabling `success` was saved → save succeeds
  historically; `start_range_session` filters `success` from that unit entry's
  `observationTypes` at start.
- Same unit appearing in multiple session items → each snapshot unit entry independently
  carries the (same) criterion and its own item's types.
- Item with `observation_types` on an action-only unit (zero-ball instructions) → allowed at
  the DB; meaningless but harmless (app never offers it; Stage 2 rule).
- Duplicate values in `observation_types` → save RPC deduplicates (`array_agg(distinct ...)`
  loses order; use a distinct-preserving select or accept catalog-order normalization in
  Stage 3 — dedupe here, canonical ordering is presentation).
- `observed_values` with unknown keys or arbitrary strings → accepted by the DB (opaque at
  this layer); Stage 2 validation owns vocabulary.
- Second observation insert for the same `(range_session_id, step_index)` → unique violation;
  the app's Stage 5 write path upserts.
- Deleting a range session → observations cascade; deleting a profile → cascades through.
  Account deletion needs no helper change (`scrub_user_audit_log` is table-agnostic).

## Validation checklist

- [ ] `supabase db reset` applies the full migration chain cleanly
- [ ] All four ALTERed tables show the new columns with correct types/defaults/constraints
- [ ] Constraint rejections: invalid observation type id; `handedness = 'AMBI'`; blank-only
      `success_criterion`
- [ ] `save_practice_unit` round-trips a criterion; blank criterion → null; a call omitting
      `p_success_criterion` (old-style named params, as MCP sends today) still succeeds
- [ ] Exactly one `save_practice_unit` function exists (old signature dropped); execute
      granted to `authenticated`
- [ ] `save_practice_session` round-trips items with and without `observation_types`;
      rejects `success` on a criterion-less unit with a descriptive error; deduplicates
- [ ] `start_range_session` on a criterion + observation-enabled session → unit entries carry
      `successCriterion` and `observationTypes`; `snapshot_version = 3`
- [ ] `start_range_session` filter case: remove the unit's criterion after saving the
      session, start again → `success` absent from that unit's `observationTypes`
- [ ] Plain session (no criterion, no types) → still v3, `successCriterion: null`,
      `observationTypes: []`; steps array byte-identical in shape to a v2 expansion
- [ ] Existing v1/v2 rows untouched (`snapshot_version` unchanged, snapshots unmodified)
- [ ] `range_session_observations`: RLS blocks cross-user read/write; unique constraint fires
      on duplicate `(range_session_id, step_index)`; cascade delete works; `updated_at`
      trigger fires; audit rows appear
- [ ] `block_results` and `session_note` accept direct PostgREST updates (the Stage 4 write
      path) under owner RLS
- [ ] **App smoke test (pre-Stage-2 build):** against the migrated local stack, start a
      session, complete steps, override a club, finish; history loads — proves the current
      app tolerates v3 snapshots and the widened row
- [ ] MCP `create_unit` / `create_session` against the migrated stack still succeed
      (regression script is currently broken — a manual tool call is acceptable)

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `start_range_session` regression — the project's most complex SQL, touched for the third time | Medium | v3 only adds two unit-entry keys, a filter, and the version literal; step expansion is copied verbatim from v2. Checklist explicitly compares a plain session's expansion against v2 behavior. |
| Current app chokes on widened `select *` payload / v3 snapshot before Stage 2 ships | Low | supabase-kt `ignoreUnknownKeys` default verified in code; on-device smoke test is a merge gate. |
| Dropping old `save_practice_unit` signature breaks callers | Low | Precedent from the tags migration; app + MCP both call with named params and get the default. Validated in checklist. |
| Vocabulary check constraint blocks future Observation Types | Accepted | Deliberate — a new type requires an app release anyway; constraint migration rides along. |
| Audit volume from per-ball observation writes | Very Low | Single-user product; `range_sessions` is already audited per tap today. |
| Migration fails on existing data | Very Low | Additive columns are nullable or defaulted; new table only; no data rewrites. |
