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

The MCP Streamable HTTP transport is mounted at `/mcp`. POST requests to this path are forwarded to the MCP SDK.

### Health check

A non-MCP health endpoint is available at `/health` (GET). Returns `{ "status": "ok" }`.

## Testing with MCP Inspector

Connect MCP Inspector to the local server to verify tool registration and invocation:

```powershell
npx @modelcontextprotocol/inspector http://localhost:8787/mcp
```

In the Inspector UI:

1. Click **Connect**.
2. Call `tools/list` — expect `ping` in the response.
3. Call `tools/call` with `{ "name": "ping", "arguments": {} }` — expect `{ "status": "ok" }`.

## Running tests

```powershell
pnpm --filter @rangework/mcp test
```

Tests include:

- **Unit test** — `ping` tool registration and invocation.
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

## Deployment

### One-time setup

1. **Custom domain** — add a DNS CNAME record for `mcp.rangework.app` on the `rangework.app` Cloudflare zone pointing at the Workers target. Verify with `dig mcp.rangework.app` before acceptance.
2. **GitHub Actions secrets** — add `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` as GitHub Actions secrets (ready for future deploy automation).

### Deploy command

```powershell
pnpm --filter @rangework/mcp deploy
```

Or run `wrangler deploy` directly from `apps/mcp`. Requires `wrangler login` or `CLOUDFLARE_API_TOKEN` + `CLOUDFLARE_ACCOUNT_ID` environment variables set locally.

Stage 1 deploy is manual. No CI/CD deploy workflow is added in Stage 1.

### Verify deployment

```powershell
dig mcp.rangework.app
```

Should resolve to the Workers target. Then test with MCP Inspector:

```powershell
npx @modelcontextprotocol/inspector https://mcp.rangework.app/mcp
```

## Stage 1 scope

- **In scope:** `ping` tool (unauthenticated), JWKS reachability test, manual deployment.
- **Deferred:** token validation (RWK-30), MCP read/write tools (RWK-31), coaching prompt (RWK-32), consent page logic (RWK-33), end-to-end testing (RWK-34).

The `ping` tool is unauthenticated in Stage 1. Auth enforcement lands in RWK-30.

## Public URL

`mcp.rangework.app` — custom domain on the `rangework.app` Cloudflare zone.

## Documentation

- `design-docs/RWK4-ai-integration/roadmap.md` — full architecture decision and stage breakdown.
- `design-docs/RWK4-ai-integration/stage1/requirements.md` — Stage 1 requirements.
- `design-docs/RWK4-ai-integration/stage1/implementation-plan.md` — this implementation plan.
