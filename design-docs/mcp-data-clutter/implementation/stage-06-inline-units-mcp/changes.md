# Stage 6: Inline Units MCP — changes

**Status:** implemented 2026-07-13 on `feature-archiveinlineunits-6`, working tree not yet
committed. Builds green: `pnpm --filter @rangework/mcp typecheck`, `lint`, and `test` (131
tests, 22 files) all pass.

## What shipped

Exactly the plan's D1–D6 recommendations.

- `apps/mcp/src/validation/inline-units.ts` (**new**) — `validateInlineUnit()` +
  `buildInlineUnitJsonb()`, the single validation implementation for a `create_unit`-shaped
  drill definition (title, 1–10 instructions, club codes against the catalog, `tag_codes`
  against the vocabulary). Takes a `fieldPrefix` so the same code produces `title`-style field
  names for `create_unit` and `items[idx].inline_unit.title`-style names for an embedded inline
  item.
- `apps/mcp/src/tools/create-unit.ts` — refactored onto the shared helper (behaviour-preserving;
  all 13 existing tests pass unchanged).
- `apps/mcp/src/tools/create-session.ts` — each item now carries `practice_unit_id` **or**
  `inline_unit` (exactly one, enforced with a field-scoped `VALIDATION_ERROR`); the owned-units
  pre-fetch gained `.is('scoped_to_session_id', null)` so an inline unit's id can't be smuggled
  in as a cross-session reference (D4/D5 — falls out of the owned set, resolves to the existing
  `UNIT_NOT_FOUND`); the `success`-requires-criterion check reads the criterion from the
  referenced unit or from the item's own embedded `inline_unit`; the RPC payload builds an
  `inline_unit` object (`title`, `instructions`, `notes`, `focus`, `default_club_code`,
  `success_criterion`, `tag_ids`) per item instead of `practice_unit_id` for inline items. Override
  hygiene (`sameAsBase`) already skips inline items for free — no referenced unit means nothing to
  compare against, so item-level overrides always survive.
- `apps/mcp/src/tools/promote-unit.ts` (**new**) — registers `promote_unit`, mirroring Stage 3's
  `archive_session` mechanism: a direct PostgREST `update(scoped_to_session_id: null).eq('id',
  …).select().maybeSingle()`, owner-scoped by existing RLS. Zero matched rows (missing id,
  another user's id, or a malformed uuid raising `22P02`) return `UNIT_NOT_FOUND` (already in
  `ErrorCodes` — no new code). Idempotent on an already-library unit (harmless no-op, still
  returns success).
- `apps/mcp/src/tools/list-units.ts` — added `.is('scoped_to_session_id', null)` to the initial
  `practice_units` select (D4 — the one place the epic's "list_units untouched" note is
  misleading for MCP: without this filter every inline unit would leak into the library listing
  once Stage 4's schema is live).
- `apps/mcp/src/tools/list-sessions.ts` — the units fetch now selects `scoped_to_session_id`;
  each item in the response carries `inline: boolean` (derived, not the raw column) alongside
  Stage 3's session-level `archived` flag.
- `apps/mcp/src/server.ts` — registers `registerPromoteUnitTool`.
- `apps/mcp/methodology/coaching-guide.md` — `methodology_version` `2.4.0` → `2.5.0`; two new
  subsections after "Reuse before you create": "Inline drills vs library drills" (default to
  inline for one-off drills; `create_unit` only for reusable ones) and "Promoting a drill
  (keeping it)" (player-initiated only, one-way). §4 tool-runbook steps 3/5/6/7 updated to
  prefer `inline_unit` over a separate `create_unit` call for session-specific drills. Data
  format rules gained the `inline_unit` / `promote_unit` contract.
- `apps/mcp/scripts/regression.ts` — `promote_unit` added to the `tools/list` expectation; new
  section after `create_session`: mints a session with one `inline_unit` item, confirms
  `list_sessions` marks it `inline: true` and the unit is absent from `list_units`, calls
  `promote_unit`, confirms the unit now appears in `list_units` and the session's item reports
  `inline: false`, and confirms a nonexistent id returns `UNIT_NOT_FOUND`.
- `.agents/instructions/mcp.md` — file map gains `promote-unit.ts`, `promote-unit.test.ts`, and
  `inline-units.ts`; tools table gains `promote_unit` and documents the `inline_unit` option on
  `create_session` and the library-only guarantee on `list_units`.

### Tests

- `apps/mcp/src/tests/promote-unit.test.ts` (**new**, 5 tests) — detaches scope and returns
  `inline: false`; idempotent on an already-library unit; `UNIT_NOT_FOUND` on a missing row;
  `DATABASE_ERROR` on a DB failure; empty `unit_id` rejected with `VALIDATION_ERROR`.
- `apps/mcp/src/tests/create-session.test.ts` — existing `practice_units` mocks updated to the
  new `select().is().order()`/`select().is()` chain; 8 new cases: rejects an item with neither
  field, rejects an item with both, builds a correct `inline_unit` RPC payload alongside a
  reference item, inline `success` reads the embedded criterion, rejects inline `success` with
  no criterion, an inline validation error is scoped to
  `items[idx].inline_unit.<field>`, and an inline unit's id used as a `practice_unit_id`
  resolves to `UNIT_NOT_FOUND`.
- `apps/mcp/src/tests/list-units.test.ts` — existing mocks updated to the new `.is()` chain; one
  new test spies on `.is('scoped_to_session_id', null)` and confirms the returned set.
- `apps/mcp/src/tests/list-sessions.test.ts` — first test's `practice_units` mock and expected
  item gained `scoped_to_session_id: null` / `inline: false`; one new test confirms a
  session-scoped unit's item is marked `inline: true` while a library-referenced item's is
  `inline: false`.
- `apps/mcp/src/tests/create-unit.test.ts` — unchanged, all 13 cases pass against the refactored
  helper (the behaviour-preserving gate).

## Deviations from plan

None material. `promote_unit`'s success message additionally maps a malformed-uuid `22P02`
Postgres error to `UNIT_NOT_FOUND` (not explicitly in the plan's D2 text, but the same
established precedent Stage 3's `archive_session` already applied — kept for consistency).

## Not done here (owner / live-environment gates)

- Regression script run against a live deployed Worker with Stage 4's migration applied (the
  script changes are written and typecheck clean, but exercising them needs `MCP_WORKER_URL` /
  `MCP_TEST_TOKEN` against a database that has `scoped_to_session_id` and `save_practice_session`
  v4).
- `get_coaching_guide` returning the bumped `2.5.0` version from R2 — needs a
  `pnpm --filter @rangework/mcp deploy` (or `dev:seed`) to push the updated guide.
- Manual conversation smoke test (inline-by-default, promotion only on explicit ask) — needs a
  live model conversation against the deployed guide.
- Epic close (design-decisions.md status, `apps/mobile/CONTEXT.md` vocabulary confirmation, §10
  follow-up issues, project memory) — deferred until Stage 5 (parallel branch) also merges, per
  the plan's "On merge" note (this is one of two trailing stages for ship point 2).
