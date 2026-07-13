# Stage 3: Archiving MCP — changes

**Status:** implemented 2026-07-13 on `feature-archiveinlineunits-3`, working tree not yet
committed. Builds green: `pnpm --filter @rangework/mcp typecheck`, `lint`, and `test` (117
tests, 21 files) all pass.

## What shipped

Exactly the plan's D1/D2/D3 recommendations, all confirmed inline in the plan text; D4's draft
coaching-guide wording landed verbatim.

- `apps/mcp/src/tools/archive-session.ts` (**new**) — registers `archive_session` and
  `unarchive_session`, sharing a `setArchived` write path: a direct PostgREST
  `update(archived_at).eq('id', …).select().maybeSingle()`, owner-scoped by existing RLS. Zero
  matched rows (missing id, another user's id, or a malformed uuid raising Postgres `22P02`) all
  return the new `SESSION_NOT_FOUND` error code — the `22P02` handling mirrors
  `get_range_session`'s established not-found precedent, which the plan's D2 explicitly invoked.
- `apps/mcp/src/validation/tool-errors.ts` — added `SESSION_NOT_FOUND` to `ErrorCodes`.
- `apps/mcp/src/tools/list-sessions.ts` — `include_archived` boolean param (default `false`);
  query-level `.is('archived_at', null)` filter applied unless set (so Stage 1's partial index
  is hit); every returned session carries `archived: boolean`; description updated.
- `apps/mcp/src/server.ts` — registers `registerArchiveSessionTools`.
- `apps/mcp/methodology/coaching-guide.md` — `methodology_version` `2.3.0` → `2.4.0`; new
  "Tidying up (archiving)" subsection at the end of §3 Design principles (user-initiated only,
  archived ≠ deleted, safe mid-Range-Session); §4 tool-runbook step 3 now notes `list_sessions`
  excludes archived by default.
- `apps/mcp/scripts/regression.ts` — `archive_session` / `unarchive_session` added to the
  `tools/list` expectation; new section exercises archive → hidden from default `list_sessions`
  → visible with `include_archived: true` (`archived: true`) → unarchive → reappears → a
  nonexistent id returns `SESSION_NOT_FOUND`. Runs against the `[TEST]` session created earlier
  in the script.
- `.agents/instructions/mcp.md` — tools table gains both verbs, error-codes list gains
  `SESSION_NOT_FOUND`, file map gains `archive-session.ts` / `archive-session.test.ts`.

### Tests

- `apps/mcp/src/tests/archive-session.test.ts` (**new**, 7 tests) — archive sets `archived_at`
  and returns `archived: true`; unarchive clears it and returns `archived: false`;
  `SESSION_NOT_FOUND` on a missing row for both verbs; `DATABASE_ERROR` on a DB failure for both
  verbs; empty `session_id` rejected with `VALIDATION_ERROR`.
- `apps/mcp/src/tests/list-sessions.test.ts` — existing `practice_sessions` mocks updated to the
  new `select().is().order()` chain (default-exclude path); the first test's `toMatchObject`
  gained `archived: false`; two new tests cover the default-exclude behaviour (spies on the
  `.is('archived_at', null)` call) and `include_archived: true` returning a mixed archived/
  unarchived pair with the correct flag on each.

## Deviations from plan

None material. One addition beyond the plan's explicit checklist: `setArchived` also maps a
malformed-uuid `22P02` Postgres error to `SESSION_NOT_FOUND` rather than `DATABASE_ERROR`,
following `get_range_session`'s existing precedent (the plan's D2 cites that precedent by name
but only worked through the `data: null` zero-row case explicitly).

## Not done here (owner / live-environment gates)

- Regression script run against a live deployed Worker (the script changes are written and
  typecheck clean, but exercising them needs `MCP_WORKER_URL` / `MCP_TEST_TOKEN`).
- `get_coaching_guide` returning the bumped `2.4.0` version from R2 — needs a
  `pnpm --filter @rangework/mcp deploy` (or `dev:seed`) to push the updated guide.
- Manual conversation smoke test ("archive that session" only fires on explicit ask) — needs a
  live model conversation against the deployed guide.
- No epic-doc status change — Stage 3 is one of two parallel stages gating ship point 1; that
  update happens once Stage 2 (app UI) is also in.
