# Stage 3: Archiving MCP (parallel with Stage 2)

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) (§3–§5)
**Vocabulary:** [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) — **Archived**
**Status:** implemented 2026-07-13 on `feature-archiveinlineunits-3` (D1, D2, D3, D4 all shipped
as recommended/drafted). See [`changes.md`](changes.md) in this folder.

## Objective

Expose the archived lifecycle state (landed in Stage 1) on the MCP tool surface so an AI planning
conversation can tidy the session list: two explicit-verb tools `archive_session(id)` /
`unarchive_session(id)`, plus an `include_archived` flag on `list_sessions` whose results carry a
visible archived marker. Tool descriptions and the coaching guide teach the model the semantics it
must not get wrong (archived ≠ deleted, history survives, archiving is safe while a Range Session
is Active, tidy only when the user asks).

This stage runs **concurrently with Stage 2** (app UI) — different apps, zero file overlap
(epic parallelism table). It depends only on Stage 1's `archived_at` column being merged; it does
**not** depend on Stage 2. No `apps/mobile` and no `supabase/` changes here unless D1 lands the
optional RPC (see below).

Covers: `apps/mcp` tool registration for the two archiving verbs; the `include_archived` param and
`archived` marker on `list_sessions`; a `SESSION_NOT_FOUND` error code; coaching-guide "tidying up"
guidance and a `methodology_version` bump; unit tests per tool and regression-script additions.

## Dependencies

- **Upstream: Stage 1 merged** — `practice_sessions.archived_at` must exist. The tools read and
  write that column directly via PostgREST; nothing else from Stage 1's KMP layer is consumed
  (MCP does not go through `PracticeLibrary`).
- Not blocked by Stage 2. Can draft as soon as this plan is approved (pipelining rule); implement
  once Stage 1 is merged.
- Existing objects read or modified: `list_sessions` tool + test, `server.ts`, `tool-errors.ts`,
  `coaching-guide.md`, `scripts/regression.ts`. RLS on `practice_sessions` (unchanged — same-owner
  column write, already covered by the existing "manage their own practice sessions" policy).

## Decisions (owner review)

### D1 — Archive write mechanism: direct PostgREST update _(epic-flagged: MCP tool shape)_

**Recommendation (confirmed): `archive_session` / `unarchive_session` write `archived_at` with a
PostgREST `update` on the row, owner-scoped by RLS — no new SQL.** Archive sets
`archived_at = <ISO now>` (JS-generated, consistent with Stage 1's "client supplies the timestamp;
the instant is informational — only null-vs-non-null carries meaning"); unarchive sets
`archived_at = null`.

```ts
const { data, error } = await ctx.supabaseClient
  .from('practice_sessions')
  .update({ archived_at: archived ? new Date().toISOString() : null })
  .eq('id', id)
  .select('id, name, archived_at')
  .maybeSingle();
```

**Why direct update, not a `set_session_archived` RPC:** Stage 1 D3 established that sessions are
read via direct PostgREST (no RPC), and archiving is a trivial same-owner single-column write that
RLS already governs. Adding an RPC buys nothing here and adds SQL surface. This mirrors the app
path, where Stage 1's `setArchived` is a PostgREST update too. (Note: the design §5 rejected a
`set_session_archived(id, bool)` **tool** — smuggling one state change through a generic mutation.
That rejection is about the *tool surface* the model sees; the two explicit verbs are what we ship.
The internal write mechanism is unrelated.)

Both verbs are **unconditional** (matching Stage 1's library semantics): archive sets the timestamp
regardless of current state; re-archiving an already-archived session simply refreshes the
timestamp (harmless, informational). No state-coupling — archiving succeeds while a Range Session
is Active (design §3; the Snapshot makes the run immune).

### D2 — Not-found handling: new `SESSION_NOT_FOUND` error code

An `update … .eq('id', id)` against a nonexistent id, or one owned by another user (RLS filters it
out), matches **zero rows** and returns `data: null` with no error. That must surface as a clean
tool error, not a silent success.

**Recommendation (confirmed): add `SESSION_NOT_FOUND` to `ErrorCodes` and return it when the update
resolves to `null`.** There is no session-scoped not-found code today (only `UNIT_NOT_FOUND` and
`RANGE_SESSION_NOT_FOUND`); this is the natural sibling and follows the `get_range_session`
precedent of a typed not-found for another-user / missing ids. Genuine DB failures still map to
`DATABASE_ERROR`.

### D3 — `list_sessions` marker shape: an always-present `archived` boolean

**Recommendation: every session in the `list_sessions` output gains `archived: boolean`**, derived
from `archived_at != null`. It's `false` on all rows in the default call and flags the archived
ones when `include_archived: true`. Rationale: an always-present boolean is a stable, unambiguous
marker the model reads the same way in both modes, versus leaking the raw `archived_at` timestamp
(informational noise the model doesn't need). The timestamp stays server-side. The `select` at
[`list-sessions.ts:38`](../../../../apps/mcp/src/tools/list-sessions.ts#L38) widens from
`id, name, notes` to `id, name, notes, archived_at`; the tag-filter and ball-count logic are
untouched. Owner may prefer surfacing `archived_at` too — cheap to add, but the boolean is the
marker the design calls for.

### D4 — Coaching-guide wording _(epic-flagged: shapes model behaviour)_

The guide must teach: archived ≠ deleted (history survives, re-runnable after unarchive), tidy
**only when the user initiates** (never archive unprompted), unarchive to reuse, archiving is safe
while a Range Session is Active. Draft wording in [Coaching-guide changes](#coaching-guide-changes)
below for async review — this is the one surface that changes what the model *does*.

## Likely files

### New

| File | Purpose |
| --- | --- |
| `apps/mcp/src/tools/archive-session.ts` | Registers both `archive_session` and `unarchive_session` (mirror ops sharing one `setArchived` helper) |
| `apps/mcp/src/tests/archive-session.test.ts` | Unit tests for both verbs (archive, unarchive, not-found) |

**One module for the pair, not two files:** archive/unarchive are inverse operations over the same
row and share the entire PostgREST-update + not-found path; splitting them would duplicate that
helper. Each still registers as its own distinct tool on the server (two `registerTool` calls), so
the model sees two explicit verbs per design §5. This is a small departure from the one-file-per-tool
convention, justified by the shared helper; called here rather than flagged to the owner.

### Modified

| File | Change |
| --- | --- |
| `apps/mcp/src/server.ts` | Import + register the two new tools |
| `apps/mcp/src/tools/list-sessions.ts` | `include_archived` param (default `false`); widen `select` with `archived_at`; default filter `archived_at is null`; add `archived` boolean to each output session; extend the tool description |
| `apps/mcp/src/tests/list-sessions.test.ts` | New cases: default excludes archived; `include_archived: true` includes them with `archived: true`; existing cases assert `archived: false` |
| `apps/mcp/src/validation/tool-errors.ts` | Add `SESSION_NOT_FOUND` to `ErrorCodes` |
| `apps/mcp/methodology/coaching-guide.md` | "Tidying up" guidance; `include_archived` note in the tool runbook; bump `methodology_version` |
| `apps/mcp/scripts/regression.ts` | Exercise archive → default-list-hides → unarchive; add the three tool names to the `tools/list` expectation |
| `.agents/instructions/mcp.md` | Add the two tools to the Tools reference table; note `SESSION_NOT_FOUND` in the error codes; add the new tool/test files to the file map |

### Referenced (not modified)

| File | Purpose |
| --- | --- |
| `apps/mcp/src/tools/create-tag.ts` | Explicit-verb write-tool pattern (RPC/update + re-read + structured return) to mirror |
| `apps/mcp/src/tests/create-tag.test.ts` | Mutation-tool test pattern (mock `from().update()`, assert the tool call shape) |
| `supabase/migrations/20260713120000_practice_session_archiving.sql` | Stage 1 migration that added `archived_at` (the column these tools read/write) |

## Tool details

### `archive_session(id)` / `unarchive_session(id)`

- **Input:** `session_id: z.string()` (match the `snake_case` id-param convention used across tools;
  describe it as "the session's `id` from `list_sessions`").
- **Success return:** the mutated session's identity and new state, re-read from the update's
  `select`, so the model can confirm the outcome:
  `{ "session": { "id": "...", "name": "...", "archived": true } }` (and `false` for unarchive).
- **Not-found:** `SESSION_NOT_FOUND` when the update matches zero rows (D2).
- **DB error:** `DATABASE_ERROR`.
- **Descriptions (draft, tighten in review):**
  - `archive_session` — "Archive a practice session: hide it from the default `list_sessions` view
    while keeping it fully re-runnable and its range-session history intact. Archived is not
    deleted — the session can be viewed, duplicated, and unarchived at any time. Safe to call even
    if a range session is currently in progress. Only archive when the player asks to tidy up."
  - `unarchive_session` — "Unarchive a previously archived practice session, returning it to the
    default `list_sessions` view so it can be started or edited again. Call this when the player
    wants to reuse an archived session."

### `list_sessions` changes

- Add `include_archived: z.boolean().optional()` (default `false`) to the input schema, described as
  "Set true to also return archived sessions (hidden by default). Each session carries an `archived`
  flag." When absent/false, apply `.is('archived_at', null)` to the `practice_sessions` query; when
  true, no archived filter.
- Widen the initial `select` to include `archived_at`; add `archived: session.archived_at != null`
  to each object in `sessionsOutput` ([`list-sessions.ts:232`](../../../../apps/mcp/src/tools/list-sessions.ts#L232)).
- Extend the description's first lines to note the default excludes archived and how to include them.
- Tag-filter, ball-count, and item/tag assembly logic are **untouched** — the archived filter is
  purely additive at the top query.

## Coaching-guide changes

Draft for D4 review. Three edits to `methodology/coaching-guide.md`:

1. **Bump `methodology_version`** `2.3.0` → `2.4.0` (new tool surface + guidance; `get_coaching_guide`
   already surfaces this version, and the regression/version check keys off it).

2. **New subsection under §3 (Design principles) or a short new "Tidying up" section** — draft:

   > ### Tidying up (archiving)
   >
   > A practice session that has served its purpose can be **archived** — hidden from the default
   > session list while staying fully intact: it keeps its range-session history and can be viewed,
   > duplicated, or unarchived and re-run at any time. Archiving is not deleting.
   >
   > - **Only archive when the player asks.** Never archive a session on your own initiative. A
   >   typical cue: the player says a program or block of work is finished ("I'm done with the
   >   putting program we built").
   > - Use `archive_session` with the session's `id`. It is safe even if a range session is in
   >   progress.
   > - To bring one back, use `unarchive_session`; to see archived sessions, pass
   >   `include_archived: true` to `list_sessions` (they carry an `archived` flag).

3. **Tool runbook note** — in the §4 discovery step alongside the existing `list_sessions` mention,
   add that archived sessions are excluded by default and `include_archived: true` reveals them.

## Test plan

### `archive-session.test.ts` (mirror `create-tag.test.ts` structure)

- `archive_session sets archived_at and returns archived: true` — mock `from('practice_sessions')
  .update().eq().select().maybeSingle()` returning the row with a non-null `archived_at`; assert the
  update payload has a non-null `archived_at` and the response is `{ session: { archived: true } }`.
- `unarchive_session clears archived_at and returns archived: false` — update payload
  `archived_at: null`; response `archived: false`.
- `archive_session returns SESSION_NOT_FOUND when the row is missing` — `maybeSingle` resolves
  `{ data: null, error: null }`; assert `isError` and `code: SESSION_NOT_FOUND`.
- `archive_session returns DATABASE_ERROR on a DB failure` — `error` non-null; assert `DATABASE_ERROR`.
- (Mirror the not-found + DB-error cases for `unarchive_session` — one each, or a shared helper.)

### `list-sessions.test.ts` additions

- `excludes archived sessions by default` — assert the query builder received the `archived_at is
  null` filter (spy on `.is`), or that an archived row supplied by the mock is absent from output.
- `includes archived sessions with archived flag when include_archived is true` — call with
  `{ include_archived: true }`; a mixed mock returns both; assert the archived session has
  `archived: true` and the unarchived one `archived: false`.
- Existing passing cases get `archived: false` added to their `toMatchObject` expectations (the new
  field is always present).

### Regression additions (`scripts/regression.ts`)

- Add `archive_session`, `unarchive_session` to the expected `tools/list` names (~line 200).
- New section after the `list_sessions` check: create (or reuse the `[TEST]` regression session id),
  `archive_session` it, assert a default `list_sessions` no longer returns it, assert
  `list_sessions { include_archived: true }` does return it with `archived: true`, then
  `unarchive_session` and assert it reappears in the default list. Leaves state clean (unarchived).

## Validation checklist

- [ ] `pnpm --filter @rangework/mcp test` green, including the new `archive-session.test.ts` and the
      extended `list-sessions.test.ts`
- [ ] `pnpm --filter @rangework/mcp typecheck` clean
- [ ] `pnpm --filter @rangework/mcp lint` clean
- [ ] `tools/list` exposes `archive_session` and `unarchive_session` (server registration wired)
- [ ] Regression script (against a live Worker, Stage-1 migration applied): plan/create → archive →
      hidden from default `list_sessions` → visible with `include_archived: true` (`archived: true`)
      → unarchive → reappears
- [ ] `get_coaching_guide` returns the bumped `methodology_version` and the tidying-up guidance
- [ ] Manual conversation smoke: "archive that session" archives only on explicit ask; the model
      does not archive unprompted (guide wording holds)

## Regression risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| `list_sessions` default silently starts hiding rows existing conversations expect | Low | Behaviour is intended (design §5); the description states the default and `include_archived` is one param away; `archived` marker makes state explicit |
| Zero-row update read as success (archiving a missing/other-user id silently no-ops) | Medium | D2 `SESSION_NOT_FOUND` on `data: null`; explicit test for the null path |
| Archived filter breaks the tag-filter or ball-count path | Low | Filter is additive at the top query only; assembly logic untouched; existing `list_sessions` tests still pass unchanged (bar the new `archived` field) |
| Model archives proactively / treats archive as delete | Medium | D4 guide wording ("only when the player asks", "not deleting"); tool descriptions restate it; manual conversation smoke in the checklist |
| `methodology_version` not bumped, cached guide served | Low | Checklist confirms the new version via `get_coaching_guide`; version bump is part of the guide edit |
| RLS lets a cross-user archive through | Very Low | No RLS change; existing owner-scoped policy filters the row out, which D2 turns into `SESSION_NOT_FOUND`; regression's second-token isolation check covers it |

## On merge

Record outcomes in `changes.md` in this stage folder (per the operating model). No epic-doc status
change until ship point 1 (that's gated on Stage 2, the app UI). Confirm `.agents/instructions/mcp.md`
reflects the two new tools and the new error code.
