# B6 — MCP: no length caps and no pagination on the list tools

Batch: mcp
Source: ../../potential-bugs.md#b6 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `apps/mcp/src/validation/inline-units.ts:78-131`,
> `supabase/migrations/20260615132000_phase3_data_foundation.sql:28,46`,
> `apps/mcp/src/tools/list-units.ts`, `list-sessions.ts`
>
> Two compounding issues:
>
> 1. **No max-length validation on any free-text field** — `title`, instruction `text`, `notes`,
>    `focus`, `success_criterion` are checked only for non-emptiness at every layer (tool
>    validation and DB constraints alike). An LLM or malformed client can persist arbitrarily
>    large strings.
> 2. **`list_units` and `list_sessions` return everything** — no `limit`/cursor, unlike
>    `list_range_sessions` (`list-range-sessions.ts:47-54`, default 20). A large library returns
>    every unit/session with full instruction text in one response.
>
> Together these are the most likely path to a genuinely large/slow response or an LLM context
> blowout.

## Confirmation method

Two new vitest tests, added to the existing files (**additions only** — do not modify existing
cases):

1. `apps/mcp/src/tests/create-unit.test.ts` — call `create_unit` with a title (and an
   instruction `text`) of, say, 100_000 characters. Today it is accepted; the test asserts a
   `VALIDATION_ERROR` naming the offending field. Follow the existing error-assertion style in
   that file.
2. `apps/mcp/src/tests/list-units.test.ts` — seed more units than the intended default page
   size and assert the response is bounded (and that an explicit `limit` is honoured).

Both must fail against current `main` before any fix. If either cannot be made to fail —
e.g. a cap already exists somewhere in the chain — DISMISS that half with the reason and
proceed with the other; this is the one spec where a partial dismissal is expected to be legal.

## Definition of done

- Both new tests pass
- `pnpm --filter @rangework/mcp test` green; `pnpm --filter @rangework/mcp lint` green
- Tool descriptions updated where the contract changes (the `limit` parameter must be
  described for the model, as `list_range_sessions` does)
- Scope boundary: changes limited to `apps/mcp/src/validation/inline-units.ts`,
  `apps/mcp/src/tools/list-units.ts`, `apps/mcp/src/tools/list-sessions.ts`, the
  `create_unit` / `create_session` tool files that feed them, and the corresponding test
  files. **No migration and no DB check constraints in this batch** — see below. No changes
  under `apps/mobile/` or `supabase/`.

## Notes for the fixer

- **Pagination pattern to mirror exactly:** `apps/mcp/src/tools/list-range-sessions.ts` —
  `const DEFAULT_LIMIT = 20`, an optional `z.number()` `limit` in the input schema, the
  `Number.isInteger(args.limit) && args.limit > 0` guard at lines 57-62, and `.limit(limit)`
  applied at the query level. Match it rather than inventing a cursor scheme; a `limit` with
  a sane default is what this bug asks for.
- Note `list_units` currently filters by tag **after** building the full output array
  (`list-units.ts:174-180`). A DB-level `.limit()` therefore interacts with tag filtering —
  decide deliberately whether the limit applies before or after the filter, and say which in
  the tool description. Getting this coherent matters more than the exact choice.
- Free-text validation belongs in `validateInlineUnit` (`validation/inline-units.ts`), which
  is already the shared choke point for both `create_unit` and `create_session`'s embedded
  `inline_unit` — that's why the fix lands in one place. Follow the existing
  `toolError(ErrorCodes.VALIDATION_ERROR, ..., { field: scopedField(...) })` shape so field
  names stay scoped correctly for embedded items.
- There is an existing precedent for a hard cap: `instructions.length > 10` at
  `inline-units.ts:68-76`. Mirror its style; put the numeric limits in named constants.
- **Deliberate scope exclusion:** the finding also faults the DB check constraints and, by
  implication, the Android `:shared` validation layer for lacking the same caps. Both are out
  of scope here — a migration would belong to the supabase-schema batch, and the app-side
  cap has no bug filed. Flag the resulting divergence (MCP rejects what the DB and the app
  still accept) in the PR body rather than fixing it silently.
