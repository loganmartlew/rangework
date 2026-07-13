# Stage 4: Inline Units foundation — changes

**Status:** implemented 2026-07-13. `:shared` debug/release and `:androidApp` debug unit
tests pass; the migration applies cleanly on `supabase db reset`; the RPC/cascade behaviours
(mint, orphan GC, deep-copy duplicate, **D3 cascade delete**, success-criterion guard,
cross-owner RLS) are validated against the local Supabase stack (see Validation below).

## What shipped

Inline-unit ownership end to end in the data layer and the KMP core, per the plan. **No app UI
(Stage 5) and no MCP surface (Stage 6).** `scoped_to_session_id` is null on every existing row
and no client mints an inline unit yet, so all current flows behave bit-identically; the only new
behaviour a merge exposes is that `duplicateSession` now routes through a copy RPC (covered by a
parity test).

## Decisions taken as recommended

- **D1** — `save_practice_session` v4: each `p_items` element carries either `practice_unit_id`
  or an `inline_unit` object (`create_unit`'s input shape). Mint → resolved-id item insert →
  success check → orphan GC. Signature unchanged (`CREATE OR REPLACE`).
- **D2** — server-side `duplicate_practice_session(p_source_id, p_new_id)` deep copies; the
  library route becomes `sessionRepository.duplicate(id)`, dropping the draft rebuild.
- **D3** — shipped the `before delete` trigger on `practice_sessions` that removes the session's
  items first, so the inline-unit cascade cannot trip the item→unit `on delete restrict`. Validated
  a session-with-inline delete succeeds with the trigger; the protective FK is untouched.
- **D4/D5** — library exclusion at the repository `list()` choke point (`get` unfiltered);
  `save_practice_unit` preserves `scoped_to_session_id` on edit (confirmed by inspection + the
  in-memory persist mirrors it).

## New files

- `supabase/migrations/20260713130000_inline_units.sql` — `scoped_to_session_id` column +
  partial index; `save_practice_session` v4 (inline mint + orphan GC); `duplicate_practice_session`
  copy RPC; the D3 before-delete ordering trigger.
- `docs/adr/0007-inline-unit-ownership-and-cascade.md` — ownership via nullable session reference
  + cascade delete, vs a visibility flag / a separate table.

## Modified — shared (KMP)

- `model/PracticeUnit.kt` — `+ scopedToSessionId: String? = null` (trailing, defaulted) +
  `val isInline`.
- `repository/PracticeUnitRepository.kt` — `+ setScopedSession(id, sessionId?)`.
- `data/SupabasePracticeUnitRepository.kt` — row field + `toModel`; `list()` filters
  `scoped_to_session_id=is.null`; `get()` unfiltered; `setScopedSession` (PostgREST update +
  read-back).
- `data/InMemoryPracticeUnitRepository.kt` — carries scope; `list()` filters it; `persist`
  preserves scope on edit (D5); `setScopedSession`; internal `cloneScoped` / `deleteScopedTo` /
  `reapOrphansScopedTo` mirroring the RPC's deep copy, cascade, and orphan GC.
- `repository/PracticeSessionRepository.kt` — `+ duplicate(id)`.
- `data/SupabasePracticeSessionRepository.kt` — `duplicate` via `duplicate_practice_session` RPC
  + read-back.
- `data/InMemoryPracticeSessionRepository.kt` — takes an optional unit-repo handle; `duplicate`
  deep-copies inline units and repoints items (library refs shared, copy unarchived); `delete`
  cascades scoped units; `persist` reaps orphans.
- `library/PracticeLibrary.kt` + `library/DefaultPracticeLibrary.kt` — `+ promoteUnit(id)` =
  `setScopedSession(id, null)`; `duplicateSession` now `sessionRepository.duplicate(id)` (draft
  rebuild dropped).

## Modified — tests

- `library/PracticeLibraryTest.kt` — shared unit/session world in `createLibrary()`; new cases:
  `listUnitsExcludesInlineUnits`, `promoteUnitDetachesOwnership`, `promoteIsOneWayContentUnchanged`,
  `duplicateSessionDeepCopiesInlineUnits`, `duplicateSessionOfLibraryOnlyIsIdentical` (D2 parity),
  `deleteSessionCascadesInlineUnits`, `editDroppingInlineUnitReapsOrphan`,
  `inlineUnitDormantWhenArchived`.
- Test fakes gained the new abstract members: `PracticePlannerViewModelTest` (android),
  `Stage03DataEnablerTest` stub repos.

## Validation

`supabase db reset` applies the full chain cleanly. Against the local stack, under an
authenticated RLS context:

- Mixed payload (one `practice_unit_id`, one `inline_unit`) → session + minted scoped unit + two
  items; library unit stays unscoped.
- Re-saving the session dropping the inline item reaps the orphan (0 scoped units remain).
- `duplicate_practice_session` deep-copies the inline unit (source 1 / copy 1), repoints the copy's
  item to a new id, leaves `archived_at` null.
- **D3:** deleting a session that owns an inline unit succeeds; the inline unit is gone, the
  referenced library unit survives.
- Success-requires-criterion fires for a minted inline unit missing a criterion, and the failed
  save leaves nothing behind (atomic).
- Cross-owner: user B cannot duplicate user A's session (source invisible under RLS → "not
  found"); no copy created.

## Deviations from plan

- The D3 trigger is **shipped**, not conditionally omitted: the plan allowed skipping it only if
  the delete is deterministically safe without it. Rather than depend on Postgres constraint
  evaluation order, the trigger makes the ordering safe by construction; the validated delete uses
  it.
- The in-memory session repo is given a handle to the in-memory unit repo (the plan's first
  option) so deep copy, cascade, and orphan GC are observable in the shared test world, keeping
  `DefaultPracticeLibrary.duplicateSession` a straight repository delegation for both the real and
  in-memory paths.
