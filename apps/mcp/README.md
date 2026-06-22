# @rangework/mcp

MCP (Model Context Protocol) server for Rangework AI Session Creation. Deployed on Cloudflare Workers at `mcp.rangework.app`.

## Stack rationale

- **TypeScript + Cloudflare Workers** — low-latency global edge runtime, no cold starts, minimal operational overhead. See `design-docs/RWK4-ai-integration/roadmap.md` §2 for the full architecture decision.
- **`@modelcontextprotocol/sdk`** — official MCP SDK for tool registration and Streamable HTTP transport.
- **Vitest** — workspace convention for TypeScript test runners.

## Prerequisites

- Node.js `>=22.12.0`
- pnpm `11.8.0` (workspace package manager)
- Cloudflare account (for deployment)
- Wrangler CLI (installed automatically via `pnpm install`)

## Local development

Start the MCP server locally with Wrangler (miniflare):

```powershell
pnpm --filter @rangework/mcp dev
```

The server runs at `http://localhost:8787` by default. Override the port via the `PORT` environment variable or by editing `wrangler.jsonc` → `dev.port`.

### MCP endpoint

The MCP Streamable HTTP transport is mounted at `/mcp`. POST requests to this path are forwarded to the MCP SDK. Requests must include a valid Supabase JWT in the `Authorization: Bearer <token>` header — the auth middleware validates the token before any tool handler runs.

### Health check

A non-MCP health endpoint is available at `/health` (GET). Returns `{ "status": "ok" }`.

## Testing with MCP Inspector

Connect MCP Inspector to the local server to verify tool registration and invocation:

```powershell
npx @modelcontextprotocol/inspector http://localhost:8787/mcp
```

In the Inspector UI:

1. Click **Connect**.
2. Call `tools/list` — expect all registered tools in the response.
3. Set `Authorization: Bearer <token>` in the request headers (use a valid Supabase access token from a logged-in Rangework account).
4. Call individual tools to verify behaviour.

## Running tests

```powershell
pnpm --filter @rangework/mcp test
```

Tests include:

- **Unit tests** — `ping` tool, all five Stage 3 tools, `toolError` factory, club code validation helpers.
- **Integration test** — JWKS reachability (confirms the Worker runtime can fetch the Supabase JWKS endpoint). Requires `SUPABASE_URL` environment variable.

### JWKS reachability test setup

The integration test fetches the Supabase JWKS endpoint to confirm RWK-28's configuration is live. Provide the Supabase project URL via environment variable:

```powershell
$env:SUPABASE_URL="https://your-project-ref.supabase.co"
pnpm --filter @rangework/mcp test
```

Or copy `.dev.vars.example` to `.dev.vars` and fill in the value (Wrangler loads `.dev.vars` automatically during `wrangler dev`, but Vitest does not — you must export it manually for tests).

## Type checking

```powershell
pnpm --filter @rangework/mcp typecheck
```

Runs `tsc --noEmit`. Not promoted to a workspace-wide Turbo task in Stage 1; run manually or promote in a later stage if type errors start slipping through CI.

## Linting

```powershell
pnpm --filter @rangework/mcp lint
```

Picked up by `turbo run lint` automatically.

## Tools

All tools except `ping` require a valid Supabase JWT (`Authorization: Bearer <token>`). The auth middleware returns `401` before any tool handler is reached when the token is missing or invalid.

### `ping`

Health-check tool. No authentication required.

**Input:** none  
**Output:** `{ "status": "ok" }`

---

### `get_user_clubs`

Returns the clubs currently enabled in the user's bag, ordered by natural bag progression (driver → putter). Call this at the start of a planning session to learn which clubs are available. Use the `code` field (not `display_name`) in all subsequent tool calls that accept a club reference.

**Input:** none

**Output:**
```json
{
  "clubs": [
    { "code": "driver", "display_name": "Driver", "category": "WOOD" },
    { "code": "seven_iron", "display_name": "7 Iron", "category": "IRON" }
  ]
}
```

Returns `{ "clubs": [] }` when the user has no enabled clubs.

---

### `list_units`

Returns all of the user's practice units, including full instruction text, ball counts, club assignment, and coaching notes. Call this before creating new units to avoid duplication and to find units that can be reused in a new session.

**Input:** none

**Output:**
```json
{
  "units": [
    {
      "id": "uuid",
      "title": "Gate Drill",
      "notes": null,
      "focus": "Keep the club face square through impact",
      "default_club_reference": "seven_iron",
      "instruction_count": 2,
      "total_ball_count": 15,
      "has_uncounted_instructions": false,
      "instructions": [
        { "order": 1, "text": "Set up two tees as a gate", "ball_count": 5 },
        { "order": 2, "text": "Hit through the gate", "ball_count": 10 }
      ]
    }
  ]
}
```

- `total_ball_count` is `null` when any instruction has no `ball_count`; `has_uncounted_instructions` will be `true` in that case.
- Returns `{ "units": [] }` when the user has no units.

---

### `list_sessions`

Returns all of the user's practice sessions, including their item lineup, club overrides, repeat counts, and coaching notes. Call this to understand how existing sessions are structured before creating a new one.

**Input:** none

**Output:**
```json
{
  "sessions": [
    {
      "id": "uuid",
      "name": "Wedge Wednesday",
      "notes": null,
      "total_ball_count": 60,
      "has_uncounted_items": false,
      "items": [
        {
          "order": 1,
          "unit_id": "uuid",
          "unit_title": "Gate Drill",
          "repeat_count": 2,
          "club_reference": "pitching_wedge",
          "notes": null,
          "focus_cue": "Hinge earlier"
        }
      ]
    }
  ]
}
```

- `total_ball_count` is `null` when any referenced unit has uncounted instructions; `has_uncounted_items` will be `true` in that case.
- Returns `{ "sessions": [] }` when the user has no sessions.

---

### `create_unit`

Creates a new practice unit (a single drill) in the user's account. Returns the new unit's id, which can be used immediately in `create_session`.

**Input:**
```json
{
  "title": "Gate Drill",
  "instructions": [
    { "order": 1, "text": "Set up two tees as a gate", "ball_count": 5 },
    { "order": 2, "text": "Hit through the gate", "ball_count": 10 }
  ],
  "focus": "Keep the club face square through impact",
  "notes": "Use an alignment stick",
  "default_club_reference": "seven_iron"
}
```

- `title` and `instructions` are required. `instructions` must have 1–10 items.
- `focus`, `notes`, and `default_club_reference` are optional.
- `ball_count` on each instruction is optional; omit rather than setting to null.
- Club references must use the `code` from `get_user_clubs`, not the display name.

**Output:** `{ "unit_id": "uuid" }`

**Retry behaviour:** Each call generates a fresh UUID. Retrying a failed `create_unit` call will create a second unit — not idempotent. Check `list_units` before retrying if unsure.

---

### `create_session`

Creates a new practice session in the user's account. Call `list_units` or `create_unit` first to obtain unit ids.

**Input:**
```json
{
  "name": "Pre-round warm-up",
  "items": [
    {
      "practice_unit_id": "uuid",
      "order": 1,
      "repeat_count": 2,
      "club_reference": "seven_iron",
      "focus_cue": "Hinge earlier",
      "notes": "Use the 50y stake"
    }
  ],
  "notes": "Tournament prep — focus on short game"
}
```

- `name` and `items` are required. `items` must have at least 1 entry.
- Each item's `practice_unit_id` must be a unit owned by the authenticated user.
- `club_reference`, `focus_cue`, and `notes` on each item are optional.
- Session-level `notes` is optional.

**Output:** `{ "session_id": "uuid" }`

**Retry behaviour:** Each call generates a fresh UUID. Retrying a failed `create_session` call will create a second session — not idempotent. Check `list_sessions` before retrying if unsure.

---

## Error shape

All tool errors set `isError: true` on the MCP content block and return a structured JSON body:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "title must not be empty",
  "data": { "field": "title" }
}
```

| Code | Meaning |
|---|---|
| `VALIDATION_ERROR` | Input failed validation — check `data.field` for which field |
| `UNKNOWN_CLUB_CODE` | Club reference not found in catalog — check `data.valid_codes` for valid options |
| `UNIT_NOT_FOUND` | Unit id not found or not owned by the user — check `data.invalid_unit_ids` |
| `DATABASE_ERROR` | Unexpected RPC failure — safe to retry once |

## Deployment

### One-time setup

1. **Custom domain** — add a DNS CNAME record for `mcp.rangework.app` on the `rangework.app` Cloudflare zone pointing at the Workers target. Verify with `dig mcp.rangework.app` before acceptance.
2. **GitHub Actions secrets** — add `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` as GitHub Actions secrets (ready for future deploy automation).

### Deploy command

```powershell
pnpm --filter @rangework/mcp deploy
```

Or run `wrangler deploy` directly from `apps/mcp`. Requires `wrangler login` or `CLOUDFLARE_API_TOKEN` + `CLOUDFLARE_ACCOUNT_ID` environment variables set locally.

### Verify deployment

```powershell
dig mcp.rangework.app
```

Should resolve to the Workers target. Then test with MCP Inspector:

```powershell
npx @modelcontextprotocol/inspector https://mcp.rangework.app/mcp
```

## Public URL

`mcp.rangework.app` — custom domain on the `rangework.app` Cloudflare zone.

## Documentation

- `design-docs/RWK4-ai-integration/roadmap.md` — full architecture decision and stage breakdown.
- `design-docs/RWK4-ai-integration/stage3/contracts.md` — final tool schemas and error contracts.
