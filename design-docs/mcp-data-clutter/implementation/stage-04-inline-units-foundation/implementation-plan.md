# Stage 4: Inline Units foundation (schema + RPC + shared) + ADR

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) (§6–§9)
**Vocabulary:** [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) — **Inline Unit**, **Promotion**
**Status:** proposed — awaiting owner sign-off on **D1** (`save_practice_session` inline payload
shape), **D2** (duplication locus / deep-copy mechanism), **D3** (cascade-delete ordering vs the
existing `on delete restrict`), and the **ADR** text below; ready for implementation once those
are confirmed

## Objective

Land inline-unit ownership end to end in the data layer and the KMP core: the
`scoped_to_session_id` column with its cascade FK, the `save_practice_session` RPC extended to
mint inline units from embedded definitions atomically, a server-side deep-copy path for
duplication, and the shared plumbing to keep inline units out of the library, reach them by id,
promote them, and cascade them. After this stage the database and shared module fully support
Inline Units; **no app UI (Stage 5) and no MCP surface (Stage 6) ship here.**

The gating property mirrors the archiving Stage 1: this stage merges ahead of everything that
consumes it. No client mints an inline unit yet (MCP embedding is Stage 6; app-side inline
creation is deferred, design §10), so `scoped_to_session_id` is null on every existing row and
every current flow — library listing, session get, save, duplicate, delete, start — behaves
bit-identically. The only new *behaviour* a merge of this stage exposes is that
`duplicateSession` routes through a copy RPC instead of a draft rebuild; that path is covered by
a parity test so library-only duplication stays identical.

Covers: `scoped_to_session_id` on `practice_units`; the `save_practice_session` inline-mint
extension (backward compatible); a `duplicate_practice_session` copy RPC; `scopedToSessionId` on
the unit model/row; the library-exclusion filter at the repository choke point; `promoteUnit`;
the session repository `duplicate` seam; and the ADR for ownership-via-nullable-reference +
cascade delete.

## Dependencies

- **Upstream: Stage 1 merged.** Inline lifecycle rests on the archived state — an inline unit is
  "dormant when archived" (design §6), which is derived from the owning session's `archived_at`
  and needs no storage, but the tests and the deep-copy-from-archived invariant assume Stage 1's
  column and `PracticeSession.archivedAt` exist. Rebase on Stage 1 before implementing.
- Local Supabase instance for validating the migration, the RPC extension, and — critically —
  the cascade-delete ordering (D3).
- Existing objects read or modified: `practice_units`, `practice_unit_instructions`,
  `practice_unit_tags`, `practice_session_items` (its `practice_unit_id` FK is the D3 hazard),
  `save_practice_session` (v4 — the epic's most-reworked SQL), `save_practice_unit` (verified for
  `scoped_to_session_id` preservation, not modified), `PracticeUnit`, `PracticeUnitRepository`
  and its Supabase / in-memory adapters, `PracticeSessionRepository` and its adapters,
  `PracticeLibrary` / `DefaultPracticeLibrary`.
- **Zero `androidApp` and zero `apps/mcp` changes in this stage.** Adding interface methods does
  not break existing callers; `duplicateSession`/`listUnits`/`getUnit` keep their signatures.

## Decisions (owner review)

### D1 — `save_practice_session` inline payload: an optional `inline_unit` per item _(epic-flagged, the one expensive-to-change surface)_

**Recommendation: each item carries either `practice_unit_id` (reference an existing unit) _or_
an `inline_unit` object (mint a new unit scoped to this session), exactly one of the two.** The
`inline_unit` shape is `create_unit`'s input verbatim, so the model already knows it (design §8):

```jsonc
// p_items element — reference form (unchanged, every existing call)
{ "practice_unit_id": "…", "order": 1, "repeat_count": 1, "club_code": "…", "observation_types": […] }

// p_items element — inline form (new)
{
  "inline_unit": {
    "title": "…",
    "instructions": [ { "order": 1, "text": "…", "ball_count": 10, "club_code": "…" } ],
    "notes": "…", "focus": "…", "default_club_code": "…",
    "success_criterion": "…", "tag_ids": ["…"]
  },
  "order": 1, "repeat_count": 1, "observation_types": […]
}
```

**RPC v4 sequence** (`create or replace`, signature unchanged — inline rides inside each
`p_items` element, so it stays `CREATE OR REPLACE` with no new params):

1. Upsert the session row (unchanged `on conflict` on name/notes; `archived_at` preserved per
   Stage 1).
2. `delete from practice_session_items where practice_session_id = p_session_id` (unchanged).
3. **Mint inline units:** for each item with an `inline_unit`, `gen_random_uuid()` a unit id,
   insert into `practice_units` with `owner_id = auth.uid()`, `scoped_to_session_id =
   p_session_id`, then its instructions and unit-tags. Build an ordered array of the resolved
   unit id per item (minted id for inline items, `practice_unit_id` for reference items).
4. Insert `practice_session_items` from that resolved array (rest of the item projection —
   order, repeat, overrides, `observation_types` — unchanged).
5. **Success-requires-criterion check** (unchanged) — now also covers minted inline units, since
   they exist by this point.
6. **Orphan GC:** `delete from practice_units where scoped_to_session_id = p_session_id and id
   <> all(<resolved unit ids>)`. This reaps inline units that an edit dropped or replaced.
   Promoted units are safe (their `scoped_to_session_id` is already null, so they are outside
   this predicate); still-referenced inline units are excluded; only genuinely orphaned scoped
   rows are removed.
7. Tags (unchanged).

**Mint-vs-reference contract:** `inline_unit` means *create new*; once minted, subsequent saves
reference the unit by `practice_unit_id` (which is exactly how the app editor works — it saves
the unit by id, and the session references it by id, design §8). MCP `create_session` is
create-only (always a fresh session), so it never re-sends `inline_unit` for an existing unit.
The GC in step 6 makes an accidental re-send at worst wasteful, never corrupting.

**Why not a second `add_inline_unit` mutation or a two-step flow:** design §8 already settled
this — the session doesn't exist to scope to until it's created, and its items need unit ids that
don't exist, so minting must be atomic inside the one save. The `inline_unit`-or-`practice_unit_id`
discriminated item is the minimal expression of that.

**RLS:** `save_practice_session` is `security invoker`; `auth.uid()` is the caller; the inline
insert defaults `owner_id` to the caller and scopes to a session the same call just upserted
under the caller — no cross-user surface (design §9). No RLS policy change: the existing "manage
their own practice units" policy already covers owner-scoped inserts.

### D2 — Duplication locus: a server-side `duplicate_practice_session` copy RPC _(epic-flagged)_

The epic requires deep-copy of inline units on duplicate, **atomic with the duplicate** (design
§6). Today `DefaultPracticeLibrary.duplicateSession` rebuilds a `PracticeSessionDraft` and calls
`persist` (one `save_practice_session`). A draft rebuild cannot express "copy these owned units
into new owned units" without polluting the draft model with an inline representation the app
otherwise never needs (app-side inline creation is deferred, §10) — and a client-side
mint-units-then-save-session sequence is **not atomic** (a partial failure orphans units).

**Recommendation: add `duplicate_practice_session(p_source_id uuid, p_new_id uuid) returns
void`, a pure server-side copy.** It runs in one transaction:

- Insert a new `practice_sessions` row (new id, copied name/notes, **`archived_at` left null** —
  a duplicate is always unarchived, design §3 / Stage 1).
- Deep-copy the source's inline units: for each `practice_units` row with `scoped_to_session_id =
  p_source_id`, insert a copy with a fresh id and `scoped_to_session_id = p_new_id`, plus its
  instructions and unit-tags; record an old→new unit-id mapping.
- Copy `practice_session_items` verbatim (order, repeat, overrides, `observation_types`),
  **repointing** `practice_unit_id` through the mapping for inline units and leaving library
  references untouched.
- Copy `practice_session_tags`.

`DefaultPracticeLibrary.duplicateSession` becomes `sessionRepository.duplicate(id)` and drops the
draft rebuild. **Behaviour change to note:** library-only duplication now copies stored rows
server-side instead of round-tripping through `validated()`. A duplicate of already-valid stored
data is equivalent (normalization ran at the original save; override hygiene is already baked
in), and it removes a re-normalization drift risk — but it is a real change, so a **parity test**
(below) pins that a library-only session duplicates with identical items, overrides,
`observation_types`, and tags.

**Alternative (owner may prefer):** carry an inline definition on `PracticeSessionItemDraft` and
reuse the D1 `save_practice_session` path for duplication too (one RPC for both create-with-inline
and duplicate-with-inline). Rejected as the default because it spreads inline representation into
the draft/validation model for a capability the app doesn't otherwise have this epic, and a copy
is cleaner expressed as a copy than as a re-composed draft. The copy RPC keeps the draft model
untouched.

### D3 — Cascade delete vs the existing `practice_session_items → practice_units` `on delete restrict` _(new — must validate)_

`practice_session_items.practice_unit_id` is `on delete restrict`
(`20260615132000_phase3_data_foundation.sql:69`). When a session is deleted, **two** cascades
fire from `practice_sessions`: items (`on delete cascade`) and the new inline units
(`scoped_to_session_id … on delete cascade`). If Postgres processes the unit cascade before the
item cascade, the still-present item's `restrict` FK to that unit blocks the delete and the whole
session delete fails.

**This is the single most important thing to validate in this stage.** The mitigation, in order
of preference:

1. **Make the two cascades order-safe by construction.** The cleanest guarantee is a `before
   delete` statement on `practice_sessions` (or reusing an existing delete path) that removes
   `practice_session_items` for the session *first*, so by the time the inline-unit cascade runs
   no `restrict` FK references it. Recommendation: **a `before delete` trigger on
   `practice_sessions`** that deletes its items, leaving the declared cascades to handle the rest.
2. If validation shows Postgres already frees the restrict within the single statement (it may,
   depending on constraint evaluation order), no trigger is needed — but we do **not** ship on
   that assumption; the checklist has an explicit delete-a-session-with-an-inline-unit test, and
   we add the trigger unless that test passes deterministically.

Do **not** relax the item→unit FK to `cascade`/`no action` — that FK deliberately protects
library units from being deleted out from under a referencing session; widening it is a
correctness regression. Scope the fix to the inline-unit delete ordering only.

### D4 — Library-exclusion locus: the repository `list()` choke point (resolved, mirrors Stage 1 D1)

`SupabasePracticeUnitRepository.list()` gains a `scoped_to_session_id is null` filter; `get(id)`
stays **unfiltered** so session detail can load an inline unit by id and promotion/duplication
can read it. Same single-choke-point rationale as Stage 1: one filter, no caller can leak an
inline unit into the library. In-memory adapter mirrors it. No `list(includeInline)` flag —
inline units are reached only through their session (design §8), and there is no library caller
that ever wants them.

### D5 — `save_practice_unit` preserves `scoped_to_session_id` on edit (resolved)

The app editor edits an inline unit by saving it under its own id (design §8), which routes
through `save_practice_unit`. Its `on conflict do update set` must not touch
`scoped_to_session_id` (verify against the current definition — the effective RPC sets only
title/notes/focus/club/instructions/tags/criterion). Confirmed by inspection to be safe; the
checklist re-verifies that an edit of an inline unit keeps it scoped and out of the library.

## Likely files

### New

| File | Purpose |
| --- | --- |
| `supabase/migrations/20260713130000_inline_units.sql` | `scoped_to_session_id` column + index; `save_practice_session` v4 (inline mint + orphan GC); `duplicate_practice_session` RPC; D3 delete-ordering trigger |
| `docs/adr/0007-inline-unit-ownership-and-cascade.md` | ADR: ownership via nullable session reference + cascade delete |

### Modified — shared (KMP)

| File | Change |
| --- | --- |
| `model/PracticeUnit.kt` | Add `scopedToSessionId: String? = null` (trailing, defaulted — wire/back-compat safe); optional `val isInline get() = scopedToSessionId != null` |
| `repository/PracticeUnitRepository.kt` | Add `setScopedSession(id, sessionId: String?)` (promotion = set null) |
| `data/SupabasePracticeUnitRepository.kt` | Row field + `toModel`; filter `list()` to `scoped_to_session_id is null`; `get()` unfiltered; implement `setScopedSession` |
| `data/InMemoryPracticeUnitRepository.kt` | Carry `scopedToSessionId`; filter `list()`; implement `setScopedSession` |
| `repository/PracticeSessionRepository.kt` | Add `duplicate(id): PracticeSession` |
| `data/SupabasePracticeSessionRepository.kt` | Implement `duplicate` via `duplicate_practice_session` RPC + read-back |
| `data/InMemoryPracticeSessionRepository.kt` | Implement `duplicate` (deep-copy scoped units held in the in-memory unit repo — see Test plan) |
| `library/PracticeLibrary.kt` | Add `promoteUnit(id): PracticeUnit` |
| `library/DefaultPracticeLibrary.kt` | Implement `promoteUnit`; route `duplicateSession` through `sessionRepository.duplicate` |

### Modified — tests

| File | Change |
| --- | --- |
| `shared/src/commonTest/.../library/PracticeLibraryTest.kt` | New tests: library exclusion, promotion detachment, deep-copy independence, cascade/GC behaviour (see Test plan) |

### Referenced (not modified)

| File | Purpose |
| --- | --- |
| `supabase/migrations/20260615132000_phase3_data_foundation.sql` | `practice_units`, `practice_unit_instructions`, `practice_session_items` (the `restrict` FK, line ~69) |
| `supabase/migrations/20260709120000_range_session_data_recording.sql` | Current `save_practice_session` v3 — the v4 base to `create or replace` |
| `supabase/migrations/20260616200000_atomic_planning_save_rpcs.sql` | Current `save_practice_unit` — verify `scoped_to_session_id` preservation (D5) |
| `supabase/migrations/20260713120000_practice_session_archiving.sql` | Stage 1's `archived_at` — the state inline dormancy derives from |

## Migration contents

`supabase/migrations/20260713130000_inline_units.sql`, in order:

1. **Column:** `alter table public.practice_units add column if not exists scoped_to_session_id
   uuid references public.practice_sessions (id) on delete cascade;` — nullable (null = library
   citizen; non-null = inline, owned by that session), design §9. Existing rows read as null.
2. **Index:** `create index if not exists practice_units_scoped_session_idx on
   public.practice_units (scoped_to_session_id) where scoped_to_session_id is not null;` — serves
   the deep-copy scan and orphan GC; partial so the library's hot path (all-null) is untouched.
   Consider also that `list()`'s `scoped_to_session_id is null` filter rides the existing
   `practice_units_owner_id_idx` — verify the plan; a second partial `where scoped_to_session_id
   is null` index is likely unnecessary at single-user scale.
3. **`save_practice_session` v4:** `create or replace` per D1's sequence. Body is the v3 body
   (migration `20260709120000`) plus the inline-mint block (step 3), the resolved-id item insert
   (step 4), and the orphan-GC (step 6). No signature change, no snapshot involvement.
4. **`duplicate_practice_session`:** `create or replace function … (p_source_id uuid, p_new_id
   uuid) returns void`, `security invoker`, `set search_path = ''`, per D2. Grant execute to
   `authenticated`. RLS enforces the caller owns the source (the source `select`s return nothing
   otherwise) and the copy defaults `owner_id` to the caller.
5. **D3 delete-ordering trigger** (unless validation proves it unnecessary): `before delete on
   public.practice_sessions for each row` → delete `practice_session_items` for `old.id`, so the
   inline-unit cascade cannot trip the item→unit `restrict`.

No new grants beyond the two RPCs; `set_updated_at` and the audit trigger already cover
`practice_units`.

## Shared changes

### Model

`PracticeUnit` gains `val scopedToSessionId: String? = null` as the **last** property (defaulted;
`ignoreUnknownKeys` and existing constructions stay compatible). A `val isInline get() =
scopedToSessionId != null` readability helper is a reasonable add for Stage 5's call sites.

### Unit repository seam

```kotlin
abstract suspend fun setScopedSession(id: String, sessionId: String?): PracticeUnit
```

- **Supabase `list()`** adds `scoped_to_session_id=is.null` (verify the exact supabase-kt DSL
  during implementation). `listArchived`-style symmetry is **not** wanted — there is no "list
  inline units" surface; they load by id through their session.
- **Supabase `get()`** gains the row field, **no filter** — inline units must stay loadable by
  id (session detail, promotion, duplication read-back).
- **Supabase `setScopedSession()`** issues a PostgREST update of `scoped_to_session_id` (owner
  RLS), then re-`get(id)`. Promotion passes `null`.
- **In-memory** stores `scopedToSessionId`; `list()` filters it; `setScopedSession` copies the
  stored unit with the new value.

### Session repository seam

```kotlin
abstract suspend fun duplicate(id: String): PracticeSession
```

- **Supabase**: `gen_random_uuid()` a new id client-side, call `duplicate_practice_session(source,
  new)`, then `get(new)` for the read-back (mirrors `persist`).
- **In-memory**: replicate the deep copy — new session id; for each source item whose unit is
  inline (look it up in the shared in-memory unit repo), mint a copy via the unit repo's
  `persist` with a fresh scope to the new session and repoint the item; library items keep their
  unit id; copy tags. (The in-memory session repo will need a handle to the unit repo, or the
  deep copy lives in `DefaultPracticeLibrary` for the in-memory path — decide during
  implementation to keep the test double honest; see Test plan.)

### Library

```kotlin
override suspend fun promoteUnit(id: String): PracticeUnit =
    unitRepository.setScopedSession(id, null)

override suspend fun duplicateSession(id: String): PracticeSession =
    sessionRepository.duplicate(id)
```

`promoteUnit` is unconditional detach — content and identity unchanged, the session keeps
referencing the same unit id (design §7); one-way (no demotion). `duplicateSession` loses its
draft rebuild; the deep copy is the RPC's job. `listUnits` already returns
`unitRepository.list()`, which now excludes inline units for free (verify, don't re-implement —
this is the Stage 5 "library continues to exclude inline units" guarantee falling out here).

## ADR — `docs/adr/0007-inline-unit-ownership-and-cascade.md`

Qualifies on all three counts (design §9): **hard to reverse** (a cascade FK + a mint/GC RPC
contract that MCP and duplication both depend on), **surprising to a future reader** (a row in
`practice_units` that is deliberately invisible to the library and dies with a session), **a
genuine trade-off** (ownership vs a visibility flag vs a separate table). Structure per the
existing ADRs (`0006` as the template):

- **Decision:** an Inline Unit is an ordinary `practice_units` row with a nullable
  `scoped_to_session_id` owning reference; `on delete cascade` ties its life to the session;
  Promotion is `set null`; the library filters `scoped_to_session_id is null` at the repository
  choke point.
- **Considered options:** (a) **ownership via nullable reference + cascade (chosen)** — inline
  units can never orphan, reuse all unit machinery, and have a clean escape hatch; (b) a "hidden
  from library" boolean with independent lifecycle (**rejected** — recreates the clutter
  invisibly, orphans accumulate forever, no cleanup story, design §6); (c) a separate
  `inline_units` table (**rejected** — loses shared instructions/tags/editor machinery, design
  §6).
- **Consequences:** cascade delete means a unit the user thought was "theirs" vanishes with the
  session — Promotion is the designed mitigation; the item→unit `restrict` FK forces a
  delete-ordering guarantee (D3); duplication is deep copy, so repeated duplication yields
  independent near-identical inline units by design (design §6, no dedup); MCP is the only
  creation path this epic (app-side inline creation deferred, §10).

## Test plan (`PracticeLibraryTest`, in-memory repos)

`runTest` + `createLibrary()` pattern as in the file. The in-memory unit/session repos share a
world so ownership and cascade are observable.

- `listUnitsExcludesInlineUnits` — persist a unit, mark it scoped (via `setScopedSession` or a
  seeded scoped unit), assert `listUnits()` omits it while `getUnit(id)` still returns it.
- `promoteUnitDetachesOwnership` — a scoped unit, `promoteUnit(id)`; now `scopedToSessionId ==
  null`, it appears in `listUnits()`, and the owning session still references the same unit id.
- `promoteIsOneWayContentUnchanged` — title/instructions/criterion identical before and after
  promotion; no demotion path exists (no API to re-scope from the library).
- `duplicateSessionDeepCopiesInlineUnits` — a session with one library item and one inline unit;
  duplicate; the copy references a **new** inline unit id (not the source's), both units exist
  independently, editing the copy's inline unit leaves the source's untouched.
- `duplicateSessionOfLibraryOnlyIsIdentical` — **parity test** for D2's behaviour change: a
  library-only session duplicates with identical item order, overrides, `observation_types`, and
  tags, a new session id, and no inline units created.
- `deleteSessionCascadesInlineUnits` — delete a session owning an inline unit; the inline unit is
  gone, a library unit the session also referenced survives (the D3 invariant, exercised in the
  in-memory model; the SQL-level ordering is validated on real Supabase below).
- `editDroppingInlineUnitReapsOrphan` — re-save a session (persist with the same id) whose item
  list no longer includes a previously-owned inline unit; that unit is gone (orphan GC), a
  promoted unit re-saved out of the session survives (its scope is already null).
- `inlineUnitDormantWhenArchived` — archive (Stage 1) the owning session; the inline unit is
  still absent from `listUnits()` and reachable by id (dormancy is derived from the owner, no
  extra state) — a thin test locking the §6 wording.

## Validation checklist

- [ ] `supabase db reset` applies the full chain cleanly; `practice_units.scoped_to_session_id`
      exists as nullable `uuid` with the cascade FK; null on all pre-existing rows
- [ ] `save_practice_session` with a mixed payload (one `practice_unit_id`, one `inline_unit`)
      creates the session, mints the inline unit scoped to it, inserts both items, and the
      success check still fires for an inline unit missing a criterion
- [ ] a plain `save_practice_session` payload (no `inline_unit`) produces byte-identical rows to
      v3 — reference-only sessions and existing MCP calls unaffected
- [ ] re-saving a session that drops an inline item reaps the orphan (GC); a **promoted** unit
      dropped from the session survives (scope already null)
- [ ] **D3:** deleting a session that owns an inline unit succeeds and removes the inline unit;
      library units the session referenced survive — run with and without the delete-ordering
      trigger to confirm whether the trigger is required, and ship it unless the delete is
      deterministically safe without it
- [ ] `duplicate_practice_session` deep-copies inline units (new ids, repointed items), copies
      tags and `observation_types`, and leaves `archived_at` null on the copy
- [ ] promotion (`scoped_to_session_id` → null) removes the unit from the library filter's
      exclusion and the session keeps referencing the same id
- [ ] `save_practice_unit` on an inline unit preserves `scoped_to_session_id` (D5 edit path)
- [ ] cross-owner: a caller cannot scope a unit to, or duplicate, another user's session (RLS)
- [ ] KMP: `:shared:testDebugUnitTest` + `:shared:testReleaseUnitTest` green, including the new
      `PracticeLibraryTest` cases
- [ ] `:androidApp:testDebugUnitTest` / `assembleDebug` green — `listUnits`/`getUnit`/
      `duplicateSession` signatures intact, no ViewModel change needed
- [ ] **Pre-Stage-5/6 smoke:** the current app lists units (no inline present), opens a session,
      duplicates it, deletes it — all unchanged, proving the widened row, new filter, and copy-RPC
      duplication are transparent before any UI or MCP ships

## Regression risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| Session delete blocked by the item→unit `on delete restrict` when an inline unit cascades | **Medium** | D3 — explicit delete-a-session-with-inline test; `before delete` trigger removing items first unless the delete is proven safe; do not relax the protective FK |
| `save_practice_session` v4 regresses reference-only saves (most-reworked SQL) | Low | Signature unchanged; inline block is additive and skipped when no `inline_unit` present; checklist compares a plain payload to v3 byte-for-byte; extend RPC tests with mixed payloads |
| Duplication behaviour change (copy RPC vs draft rebuild) alters library-only duplicates | Low | Parity test on items/overrides/observation_types/tags; a copy of already-normalized rows is equivalent to a re-validated rebuild |
| Orphan GC deletes a unit that should survive (e.g. a just-promoted unit) | Low | GC predicate is scoped to `scoped_to_session_id = p_session_id`; promoted units have null scope and are outside it; test `editDroppingInlineUnitReapsOrphan` pins both directions |
| Inline unit leaks into the library listing | Low | Single repository `list()` choke point (D4); tests on `listUnits`; `get` deliberately unfiltered so session detail/promotion/duplication keep working |
| Widened `practice_units` row breaks the current app before Stage 5 | Low | `scopedToSessionId` is a trailing defaulted field; `ignoreUnknownKeys` tolerates it; smoke test is the merge gate |
| `save_practice_unit` clears `scoped_to_session_id` on an inline-unit edit | Very Low | `on conflict do update set` verified to exclude the column (D5); checklist re-confirms |
