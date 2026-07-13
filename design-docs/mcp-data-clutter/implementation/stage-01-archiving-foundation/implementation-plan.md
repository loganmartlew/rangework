# Stage 1: Archiving foundation (schema + shared)

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) (§2–§5, §9)
**Vocabulary:** [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) — **Archived**
**Status:** proposed — awaiting owner sign-off on D1 (default-exclusion locus) and D2 (start-guard
locus) below; ready for implementation once those are confirmed

## Objective

Land the archived lifecycle state end to end in the data layer and the KMP core: the
`archived_at` column, the model/repository/library plumbing to read it, set it, and split the
session list into a default (unarchived) view and an Archived view, plus the archive lifecycle
rules as tested shared logic. After this stage the database and shared module fully support
archiving; **no UI (Stage 2) and no MCP surface (Stage 3) ship here.**

The gating property mirrors the data-recording Stage 1: this stage merges ahead of everything
that consumes it, so nothing can archive a session yet. `archived_at` is null on every row, so
every existing flow (listing, get, duplicate, save, start, history) behaves bit-identically. The
guard rails are dormant belt-and-braces until Stage 2/3 give the state a way to be set.

Covers: `archived_at` on `practice_sessions`; `archivedAt` on the session model and row;
`archiveSession` / `unarchiveSession` / `listArchivedSessions` on `PracticeLibrary`; the
repository listing split; and three tested lifecycle invariants (edit-rejects-archived,
duplicate-from-archived-is-unarchived, archive-while-Active-allowed).

## Dependencies

- None upstream — first stage of the epic.
- Local Supabase instance for validating the migration.
- Existing objects read or modified: `practice_sessions`, `save_practice_session` (verified,
  not modified), `start_range_session` (touched only if D2 lands DB-side), `PracticeSession`,
  `PracticeLibrary` / `DefaultPracticeLibrary`, `PracticeSessionRepository` and its Supabase /
  in-memory adapters.

## Decisions (owner review)

### D1 — Default-exclusion locus: the repository layer _(epic-flagged)_

**Recommendation (confirmed): exclude archived sessions at the repository `list()` choke point,
and add a sibling `listArchived()`.** The two populations become two repository methods, not one
method with a caller-supplied flag.

```
PracticeSessionRepository.list()        // archived_at is null      (unchanged contract, now filtered)
PracticeSessionRepository.listArchived()// archived_at is not null   (new)
```

`PracticeLibrary` mirrors this: `listSessions()` (default, unarchived) keeps its exact current
signature and every existing caller (`PracticePlannerViewModel`, `SettingsViewModel`) keeps
compiling and now transparently excludes archived rows; `listArchivedSessions()` is new and used
only by Stage 2's Archived screen.

**Why the repository, not the ViewModel or a `list(includeArchived)` flag:** one choke point
means no caller can accidentally leak archived sessions into a default listing — the epic's named
risk. A boolean flag defaults are easy to forget at a call site; two intention-named methods make
the wrong thing impossible to express by accident. `get(id)` stays **unfiltered** — an archived
session must remain fully viewable and duplicable (design §3), and everything that reaches a
session by id (duplicate, detail, start) already goes through `get`.

### D2 — Start-guard locus: a guard clause in `start_range_session`

The design requires start-Range-Session to reject an archived session as belt-and-braces behind
Stage 2's UI gating. Unlike the edit path, **start is not mediated by `PracticeLibrary`** —
`SupabaseRangeSessionRepository.start(sessionId)` calls the `start_range_session` RPC directly,
and the app invokes it straight from ViewModels. There is no shared use case to host the guard.

**Recommendation (confirmed): add the guard to `start_range_session`.** It already does
`select * into v_session from practice_sessions where id = p_session_id` (snapshot_v2 migration,
line ~49), so `v_session.archived_at` is in hand; the guard is a three-line clause right after
the existing `if not found` check:

```sql
if v_session.archived_at is not null then
  raise exception 'Cannot start a range session from an archived practice session';
end if;
```

This is the single choke point for start on the server, testable in SQL, and it costs one
`create or replace` of the RPC (pure additive guard clause — the v3 body is otherwise copied
verbatim; see Regression risks).

**Alternative (owner may prefer):** keep the migration to the column alone and enforce start
rejection in `SupabaseRangeSessionRepository.start` by reading the session's archived state first
— but that adds a round trip and lives outside the tested shared layer, so the DB guard is the
cleaner backstop. Either way, **Stage 2's UI gating is the primary enforcement**; this is
defense in depth for a state that cannot even be set until Stage 2/3.

### D3 — Migration surface check (resolved)

**No list RPC or view needs the column surfaced.** Sessions are listed by the app via a direct
PostgREST `select` on `practice_sessions` (not an RPC), so the repository filter (D1) is the only
default-exclusion point. `save_practice_session`'s `on conflict do update set` touches only
`name` and `notes` (verified, migration `20260709120000` line ~238) — it **preserves
`archived_at` across edits**, so no RPC change is needed for save. MCP `list_sessions` is Stage 3.
No database view references `practice_sessions`.

## Likely files

### New

| File                                                                | Purpose                                                                                            |
| ------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `supabase/migrations/20260713120000_practice_session_archiving.sql` | `archived_at` column (+ optional partial index; + `start_range_session` guard if D2 lands DB-side) |

### Modified — shared (KMP)

| File                                        | Change                                                                          |
| ------------------------------------------- | ------------------------------------------------------------------------------- |
| `model/PracticeSession.kt`                  | Add `archivedAt: Instant? = null` (trailing, defaulted — wire/back-compat safe) |
| `library/PracticeLibrary.kt`                | Add `archiveSession(id)`, `unarchiveSession(id)`, `listArchivedSessions()`      |
| `library/DefaultPracticeLibrary.kt`         | Implement the three; add the edit guard in `saveSession`                        |
| `repository/PracticeSessionRepository.kt`   | Add `listArchived()`, `setArchived(id, archivedAt: Instant?)`                   |
| `data/SupabasePracticeSessionRepository.kt` | Row field + `toModel`; filter `list()`; add `listArchived()`, `setArchived()`   |
| `data/InMemoryPracticeSessionRepository.kt` | Carry `archivedAt`; filter `list()`; add `listArchived()`, `setArchived()`      |

### Modified — tests

| File                                                       | Change                                                 |
| ---------------------------------------------------------- | ------------------------------------------------------ |
| `shared/src/commonTest/.../library/PracticeLibraryTest.kt` | New tests for the lifecycle invariants (see Test plan) |

### Referenced (not modified)

| File                                                                     | Purpose                                                             |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------- |
| `supabase/migrations/20260615132000_phase3_data_foundation.sql`          | Base `practice_sessions` table / index / RLS to extend              |
| `supabase/migrations/20260709120000_range_session_data_recording.sql`    | Current `save_practice_session` — verify `archived_at` preservation |
| `supabase/migrations/20260703120000_snapshot_v2_ball_granular_steps.sql` | Base `start_range_session` (D2 guard site)                          |

**No `androidApp` and no `apps/mcp` changes in this stage.** Adding methods to the
`PracticeLibrary` interface does not break existing callers; `listSessions()` keeps its signature.

## Migration contents

`supabase/migrations/20260713120000_practice_session_archiving.sql`, in order:

1. **Column:** `alter table public.practice_sessions add column if not exists archived_at
timestamptz;` — nullable, no default (null = unarchived; timestamp so "when" comes free,
   design §9). Existing rows read as null. No RLS change — archiving is a same-owner column write
   already covered by the existing "manage their own practice sessions" policy.
2. **Index (optional, recommend include):** a partial index for the default list's hot path —
   `create index if not exists practice_sessions_active_owner_idx on public.practice_sessions
(owner_id, updated_at desc) where archived_at is null;` — matches the existing
   `(owner_id, updated_at desc)` ordering the list uses, scoped to the unarchived population.
   Trivial at single-user scale; cheap insurance and self-documenting of the default filter.
3. **`start_range_session` guard (only if D2 lands DB-side):** `create or replace` the current v3
   function (migration `20260709120000`), inserting the `archived_at is not null` guard clause
   immediately after the existing `if not found then raise ...` block. The rest of the body is
   copied verbatim from v3 — no snapshot-shape change, no version bump (archiving is orthogonal
   to snapshot content).

No trigger, grant, or audit changes: `set_updated_at` already fires on any update (so an
archive/unarchive toggle bumps `updated_at`, which is acceptable and keeps recently-touched rows
sorted first), and the audit trigger already covers `practice_sessions`.

## Shared changes

### Model

`PracticeSession` gains `val archivedAt: Instant? = null` as the **last** property (defaulted, so
existing constructions and the `KotlinXSerializer` `ignoreUnknownKeys` path stay compatible). No
`isArchived` convenience is required, but a `val isArchived get() = archivedAt != null` is a
reasonable readability add if call sites want it.

### Repository seam

```kotlin
abstract suspend fun listArchived(): List<PracticeSession>
abstract suspend fun setArchived(id: String, archivedAt: Instant?): PracticeSession
```

- **Supabase `list()`** adds a PostgREST `archived_at is null` filter (`archived_at=is.null`;
  verify the exact supabase-kt DSL — `filter { exact("archived_at", null) }` — during
  implementation against the version in the catalog).
- **Supabase `listArchived()`** is `list()` with the inverse filter (`archived_at=not.is.null`),
  reusing the same item/tag assembly and `modelSort = PracticeSession::updatedAt`.
- **Supabase `setArchived()`** issues a PostgREST `update` of `archived_at` on the row
  (owner-scoped by RLS), then re-`get(id)` to return the fresh model (mirrors `persist`'s
  read-back). `get()` gains the `archived_at` row field but **no filter** — archived rows stay
  fully readable.
- **In-memory** stores `archivedAt` alongside each session; `list()`/`listArchived()` partition
  on it; `setArchived()` copies the stored session with the new value.

### Library

```kotlin
override suspend fun listArchivedSessions(): List<PracticeSession> = sessionRepository.listArchived()

override suspend fun archiveSession(id: String): PracticeSession =
    sessionRepository.setArchived(id, Clock.System.now())

override suspend fun unarchiveSession(id: String): PracticeSession =
    sessionRepository.setArchived(id, null)
```

`archiveSession`/`unarchiveSession` are unconditional — in particular archiving **does not**
consult range-session state, so archiving with an Active Range Session in flight simply succeeds
(design §3; the Snapshot makes the run immune). The client supplies the timestamp (consistent
with the in-memory adapter and with `persist`); the exact instant is informational — only
null-vs-non-null carries meaning.

**Edit guard** in `saveSession`: when `sessionId` is non-null and the existing session is
archived, reject before persisting. Follow the existing "session not found" style
(`error("Session $id not found")`) rather than inventing a `PracticeLibraryResult` variant — this
is a precondition violation the UI gates in Stage 2, not a draft `ValidationIssue`:

```kotlin
if (sessionId != null) {
    val existing = sessionRepository.get(sessionId)
    if (existing?.archivedAt != null) error("Cannot edit archived session $sessionId; unarchive first")
}
```

**Duplicate-from-archived is unarchived for free:** `duplicateSession` already builds a fresh
draft and persists with a null id, so the new row takes `archived_at`'s null default. No code
change — but it gets a test (below) to lock the invariant.

## Test plan (`PracticeLibraryTest`, in-memory repos)

Extend the existing session-section tests. `runTest` + `createLibrary()` pattern as in the file.

- `archiveSessionSetsArchivedState` — save a session, `archiveSession`, assert it's absent from
  `listSessions()` and present in `listArchivedSessions()`, `archivedAt != null`.
- `unarchiveSessionClearsArchivedState` — archive then unarchive; back in `listSessions()`, gone
  from `listArchivedSessions()`, `archivedAt == null`.
- `listSessionsExcludesArchived` / `listArchivedSessionsReturnsOnlyArchived` — with a mix of
  both, each listing returns exactly its partition.
- `getSessionReturnsArchivedSession` — an archived session is still returned by `getSession(id)`
  (view/duplicate must work).
- `saveSessionRejectsArchivedEdit` — archive, then `saveSession(draft, sessionId = archivedId)`
  throws; the stored row is unchanged.
- `duplicateSessionOfArchivedProducesUnarchivedCopy` — archive, duplicate, assert the copy has a
  new id, `archivedAt == null`, and appears in `listSessions()`.
- `archiveSucceedsRegardlessOfState` — archiving is unconditional (no range-session coupling in
  the library); a straight archive of a saved session succeeds. (The Active-Range-Session
  allowance is a DB/UI concern; the library simply never blocks — this test pins that it doesn't
  grow a guard later.)

## Validation checklist

- [ ] `supabase db reset` applies the full chain cleanly; `practice_sessions.archived_at` exists
      as nullable `timestamptz`, null on all pre-existing rows
- [ ] (if included) the partial index exists and is used by the default-list query plan
- [ ] Direct PostgREST update of `archived_at` under owner RLS succeeds; cross-owner update
      blocked (RLS unchanged)
- [ ] `save_practice_session` on an archived row preserves `archived_at` (edit path, even though
      the app gates it) — confirms the `on conflict` set list is unchanged
- [ ] (if D2 DB-side) `start_range_session` raises on an archived session; unaffected for
      unarchived; a plain start still produces a byte-identical v3 snapshot
- [ ] KMP: `:shared:testDebugUnitTest` + `:shared:testReleaseUnitTest` green, including the new
      `PracticeLibraryTest` cases
- [ ] `:androidApp:testDebugUnitTest` / `assembleDebug` green — `listSessions()` signature intact,
      no ViewModel change needed
- [ ] **Pre-Stage-2 smoke:** against the migrated stack the current app lists sessions, opens a
      detail, duplicates, and starts a range session unchanged (all rows unarchived, so nothing
      differs) — proves the widened row and filtered list are transparent before any UI ships

## Regression risks

| Risk                                                                   | Likelihood | Mitigation                                                                                                                                         |
| ---------------------------------------------------------------------- | ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| Archived rows leak into a default listing                              | Low        | Single repository choke point (D1); tests on both listing paths; `get` deliberately unfiltered so view/duplicate keep working                      |
| `start_range_session` regression from the D2 `create or replace`       | Low        | Guard is a pure additive clause after `if not found`; snapshot body copied verbatim; no version bump; checklist compares a plain start against v3  |
| Widened `select *` / new row key breaks the current app before Stage 2 | Low        | `archivedAt` is a trailing defaulted field; supabase-kt `ignoreUnknownKeys` tolerates it; smoke test is the merge gate                             |
| `save_practice_session` silently clears `archived_at` on edit          | Very Low   | `on conflict do update set` verified to touch only name/notes; checklist re-confirms; edit of an archived session is blocked in the library anyway |
| Archive/unarchive bumps `updated_at`, reordering lists                 | Accepted   | Intended — a just-archived/unarchived row sorting to the top of its list is reasonable; timestamp is informational                                 |
