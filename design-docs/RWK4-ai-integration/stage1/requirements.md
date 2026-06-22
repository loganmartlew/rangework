# Stage 1 — Foundations — Requirements

> **Epic:** [RWK-4 — AI Session Creation](https://loganmartlew.atlassian.net/browse/RWK-4)
> **Stage 1 tickets:** [RWK-29 — Scaffold MCP server project](https://loganmartlew.atlassian.net/browse/RWK-29) · [RWK-28 — Configure Supabase OAuth 2.1 Server](https://loganmartlew.atlassian.net/browse/RWK-28)
> **Source documents:** `design-docs/RWK4-ai-integration/roadmap.md` · Jira tickets RWK-4, RWK-29, RWK-28 · `design-docs/RWK4-ai-integration/stage1/requirements-questions.md` (answered)
> **Status:** Requirements defined, ready for implementation planning

---

## 1. Overview

Stage 1 establishes the foundational infrastructure for the AI Session Creation feature. Two independent workstreams run in parallel:

- **RWK-29** — Scaffold and deploy the MCP server (TypeScript, `@modelcontextprotocol/sdk`, Cloudflare Workers).
- **RWK-28** — Configure Supabase's OAuth 2.1 Authorization Server (dashboard work with a verification checklist).

Both workstreams must complete before Stage 2 (token validation + consent page) can begin.

---

## 2. RWK-29 — MCP Server Scaffold

### 2.1 Package placement

| Property      | Value                                                      | Rationale                                                                               |
| ------------- | ---------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Directory     | `apps/mcp`                                                 | Globbed by `pnpm-workspace.yaml` (`apps/*`); consistent with `apps/site`, `apps/mobile` |
| Package name  | `@rangework/mcp`                                           | Matches naming convention (`@rangework/site`, `@rangework/mobile`)                      |
| Consumption   | Leaf package — not imported by any other workspace package | Turbo `^build` dependency chain unaffected                                              |
| Language      | TypeScript, ESM                                            | Matches `apps/site` (Node `>=22.12.0`, `"type": "module"`)                              |
| Build tool    | Wrangler (esbuild)                                         | No separate `tsup`/`tsc` build step; Wrangler bundles natively                          |
| Type checking | `tsc --noEmit` via `typecheck` script                      | Catches type errors before deploy                                                       |

### 2.2 Stack

| Layer          | Choice                                                                     |
| -------------- | -------------------------------------------------------------------------- |
| MCP SDK        | `@modelcontextprotocol/sdk` — latest stable version at time of development |
| Transport      | Streamable HTTP (stateless, required by Claude.ai/ChatGPT web)             |
| Runtime        | Cloudflare Workers                                                         |
| Test framework | Vitest (consistent with workspace conventions)                             |
| Linting        | ESLint flat config (same pattern as `apps/site`)                           |

### 2.3 Deployment target

| Property              | Value                                                                                                                   |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Worker name           | `rangework-mcp`                                                                                                         |
| Public URL (Stage 1+) | `mcp.rangework.app`                                                                                                     |
| Stage 1 deploy        | Manual `wrangler deploy` only — no CI/CD workflow in Stage 1                                                            |
| CI secret preparation | `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` added as GitHub Actions secrets (ready for future deploy automation) |
| Account ID            | Not committed to source — provided at deploy time via env / `wrangler login`                                            |
| Environment           | Production only (no preview/staging environment in Stage 1)                                                             |

> **Note:** The custom domain `mcp.rangework.app` requires a DNS CNAME record on the `rangework.app` Cloudflare zone pointing at the Workers target. This is a one-time dashboard step during initial deploy.

### 2.4 MCP Server Requirements

#### 2.4.1 Streamable HTTP endpoint

- Endpoint mounted at `/` (or typical Workers route)
- Accepts MCP Streamable HTTP `POST` requests per the MCP specification
- Responds to MCP `tools/list` with the `ping` tool
- Responds to MCP `tools/call` for the `ping` tool

#### 2.4.2 `ping` tool contract

| Property         | Value                                                                                |
| ---------------- | ------------------------------------------------------------------------------------ |
| Name             | `ping`                                                                               |
| Input arguments  | None                                                                                 |
| Response         | `{ status: "ok" }`                                                                   |
| Auth requirement | **None in Stage 1** — `ping` is unauthenticated; auth enforcement deferred to RWK-30 |
| Transport path   | `/` or `/mcp` — chosen during implementation, documented in README                   |

### 2.5 Local development workflow

| Aspect          | Requirement                                                         |
| --------------- | ------------------------------------------------------------------- |
| Dev command     | `wrangler dev` (miniflare, closest to production)                   |
| Default port    | Wrangler default (`8787`); should be overridable via `PORT` env var |
| Verification    | Connect MCP Inspector to `http://localhost:<port>` and call `ping`  |
| Iteration speed | `wrangler dev` hot-reloads on file changes                          |

### 2.6 Turbo integration

`apps/mcp` defines the following `package.json` scripts:

| Script      | Command        | Turbo task                                                         |
| ----------- | -------------- | ------------------------------------------------------------------ |
| `dev`       | `wrangler dev` | `dev` (persistent, no cache)                                       |
| `typecheck` | `tsc --noEmit` | Not added to `turbo.json` in Stage 1; run manually or via CI later |
| `lint`      | `eslint .`     | Picked up by `turbo run lint`                                      |
| `test`      | `vitest run`   | Picked up by `turbo run test`                                      |

> **Note:** Since Wrangler handles bundling directly, there is no `build` step in the traditional sense. A Turbo `build` target is not needed for `apps/mcp`. The `typecheck` script catches type errors and can be promoted to a workspace-wide Turbo task in a later stage if desired.

### 2.7 Testing requirements

| Test type                | Scope                                                                       | Automation                           |
| ------------------------ | --------------------------------------------------------------------------- | ------------------------------------ |
| Unit test — `ping` tool  | Assert `tools/list` returns `ping`, `tools/call` returns `{ status: "ok" }` | `vitest`, runs with `turbo run test` |
| MCP Inspector — local    | Verify connectability and `ping` callable against `wrangler dev`            | Manual (documented in README)        |
| MCP Inspector — deployed | Verify connectability and `ping` callable against `mcp.rangework.app`       | Manual (Stage 1 acceptance gate)     |

> Deferred to RWK-34: end-to-end flow, auth isolation, token expiry, cross-client testing.

### 2.8 Deliverables

1. `apps/mcp/` directory with complete project scaffold
2. `apps/mcp/README.md` documenting:
   - Stack rationale (linking roadmap §2)
   - Prerequisites (Node ≥22.12, Cloudflare account, Wrangler)
   - Local dev command (`wrangler dev`)
   - MCP Inspector connection steps
   - Deploy command (`wrangler deploy`)
   - Public URL (`mcp.rangework.app`)
3. Working `ping` tool, callable via MCP Inspector locally and at deployed URL
4. Unit tests for `ping` handler
5. Updated root `README.md` workspace map to include `apps/mcp`
6. Updated `CLAUDE.md` and `.github/copilot-instructions.md` codebase maps to include `apps/mcp`
7. `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` added to GitHub Actions secrets

### 2.9 Acceptance criteria

- [ ] `apps/mcp` directory exists with `package.json`, `tsconfig.json`, `wrangler.jsonc`, ESLint config, and source files
- [ ] `pnpm install --filter @rangework/mcp` resolves and installs dependencies
- [ ] `wrangler dev` starts the MCP server locally without errors
- [ ] MCP Inspector connects to local server and `ping` returns `{ status: "ok" }`
- [ ] `vitest run` passes (unit tests for ping handler)
- [ ] `turbo run lint` includes and passes for `apps/mcp`
- [ ] `turbo run test` includes and passes for `apps/mcp`
- [ ] Manual `wrangler deploy` succeeds to `rangework-mcp`
- [ ] MCP Inspector connects to `mcp.rangework.app` and `ping` returns `{ status: "ok" }`
- [ ] Root `README.md` updated with `apps/mcp` entry
- [ ] `CLAUDE.md` and `.github/copilot-instructions.md` codebase maps updated

---

## 3. RWK-28 — Supabase OAuth 2.1 Configuration

### 3.1 Dashboard configuration steps

The following are configured in the Supabase project dashboard (Authentication → OAuth Server):

| Setting                     | Value                                 | Notes                                                                                                                              |
| --------------------------- | ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| OAuth 2.1 server            | **Enabled**                           | Beta feature — accept known risk (flag F11)                                                                                        |
| JWT signing algorithm       | **RS256**                             | Industry-standard asymmetric algorithm; most widely supported by JWT libraries                                                     |
| Dynamic client registration | **Enabled (fully open)**              | No allowlist — any client can register (lowest friction for testing; accept security posture since no active production users yet) |
| Authorization path          | `https://rangework.app/oauth/consent` | Points to the consent page (stub in Stage 1, full page in Stage 2/RWK-33)                                                          |

### 3.2 JWT algorithm change: impact assessment

- **Impact on existing Android sessions:** Supabase manages token issuance and rotation at the dashboard level. Existing tokens are expected to continue working through Supabase's key rotation mechanism. The verification checklist (section 3.4) must confirm this.
- **Rollback:** No explicit rollback plan needed — there are no active production users. If issues arise, the setting can be reverted in the dashboard.

### 3.3 Consent page stub

Since RWK-28 must point at a consent URL, but the full consent page is built in Stage 2 (RWK-33), a **stub page** must be added to `apps/site` at `/oauth/consent`:

- Route: `https://rangework.app/oauth/consent`
- Content: A simple static page that displays a "coming soon" message acknowledging the OAuth flow
- Purpose: Prevents 404 errors if any OAuth flow is attempted between Stage 1 and Stage 2
- Implementation: Add as a static Astro page under `apps/site/src/pages/oauth/consent.astro`
- No Supabase client, no consent logic — RWK-33 replaces this entirely

> **Note:** The `mcp.rangework.app` Worker subdomain is independent of the `rangework.app/oauth/consent` page path, so no routing collision occurs.

### 3.4 Verification checklist deliverable

A markdown checklist is produced at `design-docs/RWK4-ai-integration/stage1/rwk-28-verification.md` covering:

1. **Discovery endpoint:** `https://<project-ref>.supabase.co/.well-known/oauth-authorization-server/auth/v1` returns valid JSON with `authorization_endpoint`, `token_endpoint`, `jwks_uri`, and `registration_endpoint` present
2. **JWKS endpoint:** The `jwks_uri` from step 1 returns valid JWKS keys (RS256)
3. **Dynamic client registration:** POST to `registration_endpoint` with a valid client metadata payload succeeds and returns a `client_id`
4. **Android sign-in (email):** Sign in to the Android app with email/password succeeds post-algorithm-switch
5. **Android sign-in (Google):** Sign in to the Android app with Google succeeds post-algorithm-switch
6. **Consent URL reachable:** `https://rangework.app/oauth/consent` returns 200 (stub page)
7. **Discovery response:** Record the full discovery response JSON for reference by RWK-30

> **Note:** (4) and (5) can only be verified if valid Android app credentials exist. See `CLAUDE.md` secrets section for `~/.gradle/gradle.properties` requirements. If the Android app cannot be built/run during Stage 1, document which steps were skipped and note that RWK-34 will cover full end-to-end verification.

### 3.5 Deliverables

1. Supabase OAuth 2.1 server enabled and configured per section 3.1
2. Consent stub page at `apps/site/src/pages/oauth/consent.astro`
3. Verification checklist at `design-docs/RWK4-ai-integration/stage1/rwk-28-verification.md`

### 3.6 Acceptance criteria

- [ ] Discovery endpoint returns valid JSON with all expected fields
- [ ] JWKS endpoint returns RS256 public keys
- [ ] Dynamic client registration accepts and returns a `client_id`
- [ ] Consent stub page returns 200 at `https://rangework.app/oauth/consent`
- [ ] Verification checklist completed and saved to repo
- [ ] Android sign-in verified (or explicitly noted as deferred)

---

## 4. Cross-cutting requirements

### 4.1 Scope boundary

| Item                                   | In Stage 1                       | Deferred |
| -------------------------------------- | -------------------------------- | -------- |
| Token validation                       | No — RWK-30                      | ✓        |
| MCP read/write tools                   | No — RWK-31                      | ✓        |
| Coaching prompt                        | No — RWK-32                      | ✓        |
| Consent page logic                     | No — RWK-33                      | ✓        |
| End-to-end testing                     | No — RWK-34                      | ✓        |
| Shared types between stages            | Each stage manages its own types | ✓        |
| Integration test (MCP ↔ Supabase JWKS) | No — explicitly RWK-30 scope     | ✓        |

### 4.2 Documentation updates

The following files must be updated when Stage 1 completes:

- `README.md` — add `apps/mcp` to workspace map and build/validation section
- `CLAUDE.md` — add `apps/mcp` to codebase map and build commands
- `.github/copilot-instructions.md` — add `apps/mcp` to codebase map

### 4.3 Stage 1 exit demo

The two workstreams are verified independently:

- **RWK-29:** MCP Inspector session screenshot showing `ping` returning `{ status: "ok" }` against `mcp.rangework.app`
- **RWK-28:** `curl` response showing the discovery endpoint JSON + consent stub page screenshot

Both acceptance checklists signed off before Stage 2 begins.

### 4.4 Risks

| Risk                                                         | Impact                                         | Mitigation                                                       |
| ------------------------------------------------------------ | ---------------------------------------------- | ---------------------------------------------------------------- |
| Supabase OAuth 2.1 server is beta (F11)                      | API changes may break RWK-30/33                | Pin behaviour during RWK-28; monitor Supabase changelog          |
| RS256 key rotation                                           | Existing tokens invalidated                    | Verify Android sign-in as part of RWK-28 checklist               |
| Cloudflare Workers custom domain (`mcp.rangework.app`) setup | DNS propagation delay, dashboard access needed | Document as a one-time step; verify with `dig` before acceptance |
| No active users yet (per user)                               | Low risk tolerance for breaking changes        | Acceptable — no production impact from errors                    |
