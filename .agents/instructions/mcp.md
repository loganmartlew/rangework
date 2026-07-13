# MCP server instructions

`apps/mcp` is a Cloudflare Worker (TypeScript) that implements the Model Context Protocol server for AI-assisted practice planning. Deployed at `mcp.rangework.app`.

## Stack

- TypeScript + Cloudflare Workers (Wrangler, miniflare for local dev)
- `@modelcontextprotocol/sdk` — official MCP SDK (Streamable HTTP transport)
- Vitest — test runner
- Supabase JWT auth (all tools except `ping` require `Authorization: Bearer <token>`)

## File map

- `apps/mcp/src/index.ts` — Cloudflare Workers fetch handler; mounts MCP at `/mcp`, health at `/health`, OAuth metadata at `/.well-known/oauth-protected-resource`.
- `apps/mcp/src/server.ts` — `createServer()` factory; registers all tools and prompts on the MCP server instance.
- `apps/mcp/src/auth/validateToken.ts` — Supabase JWT validation (JWKS, issuer check).
- `apps/mcp/src/auth/userContext.ts` — `UserContext` type and factory; passes authenticated user id and Supabase client to tools.
- `apps/mcp/src/tools/ping.ts` — `ping` health-check (no auth).
- `apps/mcp/src/tools/get-user-clubs.ts` — `get_user_clubs` read tool.
- `apps/mcp/src/tools/list-units.ts` — `list_units` read tool.
- `apps/mcp/src/tools/list-sessions.ts` — `list_sessions` read tool.
- `apps/mcp/src/tools/list-range-sessions.ts` — `list_range_sessions` read tool (completed range-session summaries).
- `apps/mcp/src/tools/get-range-session.ts` — `get_range_session` read tool (one completed session's block detail, aggregates, raw balls).
- `apps/mcp/src/tools/create-unit.ts` — `create_unit` write tool (calls `save_practice_unit` RPC).
- `apps/mcp/src/tools/create-session.ts` — `create_session` write tool (calls `save_practice_session` RPC).
- `apps/mcp/src/tools/list-tags.ts` — `list_tags` read tool (Default + the user's Custom Tags).
- `apps/mcp/src/tools/create-tag.ts` — `create_tag` write tool (calls `create_or_get_tag` RPC; the only way the AI mints a Custom Tag).
- `apps/mcp/src/tools/archive-session.ts` — `archive_session` / `unarchive_session` write tools (direct PostgREST update of `practice_sessions.archived_at`, owner-scoped by RLS).
- `apps/mcp/src/tools/get-coaching-guide.ts` — `get_coaching_guide` fallback tool (loads methodology from R2).
- `apps/mcp/src/prompts/build-practice-plan.ts` — `build_practice_plan` prompt (returns coaching methodology as a user message).
- `apps/mcp/src/methodology/loader.ts` — R2-backed coaching guide loader with in-memory isolate cache.
- `apps/mcp/src/validation/tool-errors.ts` — `toolError` factory and `ErrorCodes` constants.
- `apps/mcp/src/validation/club-codes.ts` — `fetchAllClubCodes` and `validateClubCode` helpers.
- `apps/mcp/src/validation/observation-types.ts` — `OBSERVATION_TYPES` vocabulary + `validateObservationTypes` helper (used by `create_session`).
- `apps/mcp/src/validation/tags.ts` — `slugifyTag`, `fetchVisibleTags`, and `resolveTagCodes` helpers (tag-code → id resolution for write tools).
- `apps/mcp/src/tests/archive-session.test.ts` — unit tests for `archive_session` / `unarchive_session`.
- `apps/mcp/src/tests/` — unit tests for every tool + integration test for JWKS reachability.
- `apps/mcp/scripts/regression.ts` — end-to-end regression script against a live Worker.
- `apps/mcp/methodology/coaching-guide.md` — the coaching methodology document (source of truth; deployed to R2).

## Tools reference

All tools except `ping` require a valid Supabase JWT.

| Tool | Type | Description |
| --- | --- | --- |
| `ping` | read | Health check. No auth. |
| `get_user_clubs` | read | User's enabled clubs (ordered driver → putter). Always call first; use `code` field in subsequent calls. |
| `list_units` | read | All practice units with full instructions and tags. Accepts an optional `tag_codes` OR filter. Call before creating units to avoid duplication. |
| `list_sessions` | read | All practice sessions with item lineups and tags. Accepts an optional `tag_codes` OR filter. Excludes archived sessions unless `include_archived: true`; each session carries an `archived` flag. |
| `list_range_sessions` | read | Completed range-session summaries (newest first, Completed only). Thin — carries `balls_hit`, `blocks_with_results`, `has_observations`. Optional `limit` (default 20). |
| `get_range_session` | read | One completed range session's block-level detail: notes, manual counts, per-type observation aggregates (with denominators), and raw per-ball observations. Returns `RANGE_SESSION_NOT_FOUND` for non-completed/other-user ids. |
| `list_tags` | read | Default Tags + the user's Custom Tags. Use `code` when attaching/filtering. |
| `create_unit` | write | Creates a new drill (optional `tag_codes`, optional `success_criterion`). Not idempotent — retrying creates duplicates. |
| `create_session` | write | Creates a new session (optional `tag_codes`, the session's own goal; optional per-item `observation_types`). `success` requires the unit to have a `success_criterion`. Not idempotent — retrying creates duplicates. |
| `create_tag` | write | Mints a Custom Tag by name (or returns the matching tag). The only way the AI creates a tag. |
| `archive_session` | write | Archives a practice session (hidden from default `list_sessions`, fully re-runnable). Not deleted. Returns `SESSION_NOT_FOUND` for non-existent/other-user ids. |
| `unarchive_session` | write | Restores an archived session to the default `list_sessions` view. Returns `SESSION_NOT_FOUND` for non-existent/other-user ids. |
| `get_coaching_guide` | read | Fallback for clients that don't support MCP prompts. Returns coaching methodology. |

## Error shape

All tool errors return `isError: true` with a structured JSON body:

```json
{ "code": "VALIDATION_ERROR", "message": "title must not be empty", "data": { "field": "title" } }
```

Error codes: `VALIDATION_ERROR`, `UNKNOWN_CLUB_CODE`, `UNKNOWN_TAG_CODE`, `UNIT_NOT_FOUND`, `RANGE_SESSION_NOT_FOUND`, `SESSION_NOT_FOUND`, `DATABASE_ERROR`, `CONTENT_UNAVAILABLE`.

## Local development

```powershell
pnpm --filter @rangework/mcp dev:seed   # seed local R2 with coaching guide
pnpm --filter @rangework/mcp dev        # starts Worker at http://localhost:8787
pnpm --filter @rangework/mcp test       # unit + integration tests
pnpm --filter @rangework/mcp typecheck  # tsc --noEmit
```

Requires `SUPABASE_URL` env var for the JWKS integration test. Copy `.dev.vars.example` → `.dev.vars` and fill in.

## Deployment

```powershell
pnpm --filter @rangework/mcp deploy   # uploads coaching-guide.md to R2 then wrangler deploy
```

Requires `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` (via env or `wrangler login`).

## Conventions

- Auth is enforced at the fetch handler level — all tool handlers can assume a valid `UserContext`.
- New tools follow the same pattern: register in `server.ts`, accept `UserContext`, use `toolError()` for all error returns.
- The coaching methodology lives in `methodology/coaching-guide.md`; edit it there and redeploy — do not hardcode methodology text in TypeScript.
- Tests live in `apps/mcp/src/tests/`; add a test file per tool.
