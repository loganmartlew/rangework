# RWK-4 AI Integration — Implementation Review

> **Branch:** `ai-integration`  
> **Reviewed:** 2026-06-23  
> **Scope:** All changes from the `ai-integration` branch vs `main`, including committed history (Stages 1–5).

---

## Summary

The implementation is substantially complete and well-structured. All five MCP tools (`get_user_clubs`, `list_units`, `list_sessions`, `create_unit`, `create_session`) are implemented and tested, the `build_practice_plan` prompt and `get_coaching_guide` fallback tool are in place, OAuth token validation is wired to Supabase JWKS, and the consent page is implemented. The deployment pipeline (GitHub Actions → Cloudflare Workers + R2) is configured.

**16 issues were found across four tiers.** Two issues are critical/broken, four are high-priority correctness problems, six are medium-priority design/reliability gaps, and four are low-priority cleanup items.

---

## Critical

### C1 — OAuth Callback Race Condition in Consent Page

**File:** [`apps/site/src/components/oauth/consent-island.ts:71-84`](apps/site/src/components/oauth/consent-island.ts)

After Google OAuth redirects back to the consent page (with `?code=...&authorization_id=...`), the Supabase client must perform an async PKCE code exchange before `getSession()` reflects the new session. `mountConsentIsland` runs in `DOMContentLoaded` and calls `supabase.auth.getSession()` immediately. If the exchange has not yet completed, `getSession()` returns `null`, the code calls `signInWithOAuth()` again, and the user is redirected back to Google — creating a redirect loop that makes the consent flow unusable.

**Fix:** Replace the synchronous `getSession()` check with a `supabase.auth.onAuthStateChange` listener that waits for the `SIGNED_IN` or `INITIAL_SESSION` event before proceeding to `getAuthorizationDetails`. Only fall back to the OAuth redirect if the initial state is `null` and no auth callback is in progress (check for absence of `?code=` in the current URL).

---

### C2 — JWKS Integration Test Checks the Wrong Endpoint

**File:** [`apps/mcp/src/tests/jwks-reachability.test.ts:22-23`](apps/mcp/src/tests/jwks-reachability.test.ts)

The test constructs:
```typescript
const jwksUrl = new URL('/.well-known/jwks.json', supabaseUrl);
// → https://<project>.supabase.co/.well-known/jwks.json
```

The Worker validates tokens against:
```typescript
const jwksUri = `${env.SUPABASE_URL}/auth/v1/.well-known/jwks.json`;
// → https://<project>.supabase.co/auth/v1/.well-known/jwks.json
```

The discovery response confirms the correct path is `/auth/v1/.well-known/jwks.json`. The integration test is checking a different path entirely, so it provides no real confidence that the Worker's JWKS validation will work. It may silently pass or fail independent of actual token-validation readiness.

**Fix:** Change line 22 to:
```typescript
const jwksUrl = new URL('/auth/v1/.well-known/jwks.json', supabaseUrl);
```

---

## High

### H1 — Private API Access in Worker Fetch Handler

**File:** [`apps/mcp/src/index.ts:104-119`](apps/mcp/src/index.ts)

The fetch handler accesses `transport._webStandardTransport.handleRequest()`, a private internal field (underscore-prefixed, not part of the public `@modelcontextprotocol/sdk` contract). TypeScript's protection is suppressed with `as unknown as {...}`. Any future SDK update that renames or removes this internal — without a semver-breaking change — silently kills the entire MCP endpoint. There is currently no stable public alternative for the Cloudflare Workers / web-standard fetch context.

**Action:** Track the MCP SDK for a public web-standard fetch handler API (the SDK team is aware of this gap). Pin the SDK dependency more tightly (e.g., `~1.17.3` instead of `^1.17.3`) and add a comment that the private API must be re-verified on any SDK upgrade. Until a public API exists, this is an accepted risk that must be surfaced in a changelogy note.

---

### H2 — `fetchAllClubCodes` Throws But Callers Don't Catch

**Files:** [`apps/mcp/src/validation/club-codes.ts:19-21`](apps/mcp/src/validation/club-codes.ts), [`apps/mcp/src/tools/create-unit.ts:136`](apps/mcp/src/tools/create-unit.ts), [`apps/mcp/src/tools/create-session.ts:152`](apps/mcp/src/tools/create-session.ts)

`fetchAllClubCodes` throws `new Error(...)` when the Supabase query fails:
```typescript
if (error) {
  throw new Error(`Failed to fetch club codes: ${error.message}`);
}
```

Both `create_unit` and `create_session` `await` this function without try/catch. In production, if the `clubs` table is temporarily unreachable during club-code validation, the tool handler rejects with an unhandled error — bypassing the `toolError(ErrorCodes.DATABASE_ERROR, ...)` path used everywhere else. The MCP SDK propagates this as an unstructured internal error rather than the structured error shape the contracts define.

**Fix:** Wrap `fetchAllClubCodes` calls in both tools with try/catch and return `toolError(ErrorCodes.DATABASE_ERROR, 'Failed to validate club codes. Please try again.')` on failure. Alternatively, change `fetchAllClubCodes` to return `string[] | null` instead of throwing and handle the null in callers.

---

### H3 — Regression Script Uses `sort_order` Instead of `order` for Session Items

**File:** [`apps/mcp/scripts/regression.ts:326`](apps/mcp/scripts/regression.ts)

The `create_session` call in the regression script passes `sort_order: 1` in the items array:
```typescript
items: [{
  practice_unit_id: createdUnitId,
  sort_order: 1,   // ← wrong field name
  repeat_count: 3,
  ...
}],
```

The tool schema requires `order: number` (a required field). `sort_order` is ignored and `order` is absent, causing Zod to reject the input. The `create_session` portion of the regression test will always fail with a validation error. The contracts document explicitly states: "`order` (not `sort_order`) is used in all tool I/O."

**Fix:** Change `sort_order: 1` to `order: 1` on line 326.

---

### H4 — Regression Script Passes `null` for `z.string().optional()` Fields

**File:** [`apps/mcp/scripts/regression.ts:328-330`](apps/mcp/scripts/regression.ts)

The same `create_session` call also passes explicit `null` values for optional fields:
```typescript
club_reference: null,
notes: null,
focus_cue: null,
```

The Zod schema defines these as `z.string().optional()` — which accepts `string | undefined` but **not** `null`. Zod will reject `null` with a type error. The fix for H3 alone will not unblock this test; these fields must also be either omitted or changed to `undefined`.

**Fix:** Remove `club_reference`, `notes`, and `focus_cue` from the regression test's items object entirely (or set them to `undefined` rather than `null`). Optional fields should be omitted when not needed.

---

## Medium

### M1 — No Positive Integer Validation for `order`, `repeat_count`, `ball_count`

**Files:** [`apps/mcp/src/tools/create-unit.ts`](apps/mcp/src/tools/create-unit.ts), [`apps/mcp/src/tools/create-session.ts`](apps/mcp/src/tools/create-session.ts)

The Stage 3 contracts explicitly specify:
- `order`: "positive integer, unique within array"
- `repeat_count`: "positive integer"
- `ball_count`: "positive integer if provided"

The implementation validates uniqueness of `order` values but does not check positivity. A caller (or misbehaving LLM) passing `order: 0`, `order: -1`, `repeat_count: 0`, or `ball_count: -5` will pass tool-level validation and hit the RPC, which enforces the constraint at the DB level. The result is a generic `DATABASE_ERROR` response instead of the structured `VALIDATION_ERROR` with a `field` pointer that the contracts promise.

**Fix:** Add `.min(1)` to the Zod schemas for `order` and `repeat_count`, and add a runtime check that rejects `ball_count <= 0` when provided.

---

### M2 — `list_sessions` Makes 4 Serial Database Round-Trips

**File:** [`apps/mcp/src/tools/list-sessions.ts`](apps/mcp/src/tools/list-sessions.ts)

The tool fires four sequential Supabase queries: sessions → items → units → instructions. Each is a separate HTTP round-trip to PostgREST. For a user with many sessions referencing many distinct units, the final two queries scale with the number of unique referenced units. In edge latency (Cloudflare Workers → Supabase), this adds hundreds of milliseconds per call compared to a single join or RPC.

**Fix (short-term):** Run the units and instructions queries in parallel (`Promise.all`) once the item list is known — reduces 4 round-trips to 3. **Fix (long-term):** Implement a `list_sessions` Postgres RPC or use Supabase's embedded relations (`select('*, items(*, practice_units(*))')`).

---

### M3 — JWT Algorithm Hardcoded to ES256 Only

**File:** [`apps/mcp/src/auth/validateToken.ts:54`](apps/mcp/src/auth/validateToken.ts)

```typescript
const { payload } = await jwtVerify(token, jwks, {
  issuer,
  algorithms: ['ES256'],
});
```

The Supabase OAuth AS discovery response (`discovery-response.json`) lists `id_token_signing_alg_values_supported: ["RS256", "HS256", "ES256"]`. The protected-resource metadata advertises `ES256` only, which is the intended restriction. However, if the Supabase project is configured to issue RS256-signed JWTs (a common Supabase default), all token validation will fail and the server will be completely inaccessible. This must be verified as part of RWK-28 completion before production use.

**Action:** Confirm in the RWK-28 checklist that the project is configured to use ES256. If it uses RS256, add `'RS256'` to the allowed algorithms array and update the protected-resource metadata accordingly.

---

### M4 — Consent Page Supabase Client Has No Env Var Guard

**File:** [`apps/site/src/lib/supabase-client.ts`](apps/site/src/lib/supabase-client.ts)

```typescript
const supabaseUrl = import.meta.env.PUBLIC_SUPABASE_URL as string;
const supabaseAnonKey = import.meta.env.PUBLIC_SUPABASE_ANON_KEY as string;
```

`as string` casts `undefined` to `string` silently if the env vars aren't set. This creates a Supabase client with the literal string `"undefined"` as the project URL, causing runtime fetch errors with cryptic messages rather than a clear "env var not set" build or startup failure.

**Fix:** Add a guard:
```typescript
if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error('PUBLIC_SUPABASE_URL and PUBLIC_SUPABASE_ANON_KEY must be set');
}
```

---

### M5 — Test Mocks Inaccurate for `create_session` Unit Lookup

**File:** [`apps/mcp/src/tests/create-session.test.ts:129-143, 183-196`](apps/mcp/src/tests/create-session.test.ts)

Two test cases (`rejects duplicate order values`, `rejects unknown unit id`) mock `practice_units.select()` to return `{ order: async () => ({data, error}) }`, implying a `.order()` chain. The actual code does:
```typescript
const { data: ownedUnits, error: unitsError } = await ctx.supabaseClient
  .from('practice_units')
  .select('id');   // ← awaited directly, no .order()
```

The tests still pass because the assertions they make fire during validation phases that precede the DB query. But the mocks don't represent the real query chain, meaning if the query path ever changes, the tests won't catch it.

**Fix:** Update both mocks to return a Promise directly from `.select()`:
```typescript
select: () => Promise.resolve({ data: [{ id: 'unit-1' }], error: null })
```

---

### M6 — `CONTENT_UNAVAILABLE` Error Code Not in `ErrorCodes` Constant

**File:** [`apps/mcp/src/tools/get-coaching-guide.ts:29`](apps/mcp/src/tools/get-coaching-guide.ts)

```typescript
return toolError('CONTENT_UNAVAILABLE', 'Coaching guide is temporarily unavailable...');
```

Every other tool error uses `ErrorCodes.*` constants from `tool-errors.ts`. `CONTENT_UNAVAILABLE` is a string literal with no constant, no documentation in the error shape contracts, and no test asserting the specific code. This breaks the error-contract pattern established by the rest of the implementation.

**Fix:** Add `CONTENT_UNAVAILABLE: 'CONTENT_UNAVAILABLE'` to `ErrorCodes` in `tool-errors.ts` and reference it in `get-coaching-guide.ts`.

---

## Low

### L1 — README Has a Duplicate `## Deployment` Section

**File:** [`apps/mcp/README.md`](apps/mcp/README.md)

The `## Deployment` section with `### One-time setup` appears twice. The second occurrence adds an R2 bucket creation step that is absent from the first. The content should be merged into a single section.

---

### L2 — Regression Script Accumulates Test Data With Each Run

**File:** [`apps/mcp/scripts/regression.ts`](apps/mcp/scripts/regression.ts)

Each run creates `[TEST] Regression unit <timestamp>` and `[TEST] Regression session <timestamp>` in the user's real Supabase account. These persist with no automated cleanup. Over repeated regression runs the test account accumulates stale `[TEST]` records that clutter `list_units` / `list_sessions` output and may affect ball-count or structure assertions if those tools become count-sensitive.

**Fix:** Add a cleanup step at the end of `main()` that calls the Supabase client directly to delete records with titles/names matching the `[TEST]` prefix created during this run (use the captured IDs from create responses).

---

### L3 — Methodology Cache Doesn't Invalidate on R2-Only Update

**File:** [`apps/mcp/src/methodology/loader.ts`](apps/mcp/src/methodology/loader.ts)

The in-memory cache persists for a Worker isolate's lifetime. If the coaching methodology in R2 is updated without redeploying the Worker (e.g., hot-patching `coaching-guide.md` via `wrangler r2 object put`), existing isolates serve the stale content until natural eviction. The `deploy` script correctly uploads R2 then deploys the Worker, but the README's "Updating the methodology" section does not warn against bare R2 uploads.

**Fix:** Add a note to the README that a Worker deploy is always required after a methodology update; the R2-only path will not reliably propagate.

---

### L4 — Consent Page `aria-live` Region Not Cleaned Up on State Transition

**File:** [`apps/site/src/pages/oauth/consent.astro:68`](apps/site/src/pages/oauth/consent.astro)

The loading `div` is `aria-live="polite" aria-busy="true"`. When `setState('consent')` or `setState('error')` hides it via the `hidden` attribute, `aria-busy` remains `true` on the hidden element. Screen readers may remain in a wait state. Setting `aria-busy="false"` when transitioning away from loading would be more semantically correct.

---

## Issues Not Found

The following areas were reviewed and no issues were identified:

- **Token validation logic** — `validateToken.ts` correctly handles missing header, empty token, expired JWT (`JoseErrors.JWTExpired`), missing `sub` claim, and generic signature failures. JWKS caching across Worker isolate requests is implemented correctly.
- **User context construction** — `createUserContext` correctly sets `Authorization: Bearer <token>` in the Supabase client headers with `persistSession: false` and `autoRefreshToken: false`. RLS will apply correctly.
- **Club code validation** — The two-layer validation (pre-check via `fetchAllClubCodes` + FK-violation fallback in `create_unit`) is correct. `validateClubCode` returns `null` for valid codes as expected.
- **UUID generation** — `crypto.randomUUID()` is used for both unit and session IDs, which is the correct Worker-native API.
- **RPC argument mapping** — `save_practice_unit` and `save_practice_session` parameter names and JSONB shapes match the schema contracts. Optional fields are correctly omitted (not null-set) in the JSONB.
- **Error shape contract** — All errors use `isError: true` with `{ code, message, data? }` shape consistently (except M6 above).
- **Protected resource metadata** — Correctly returns `authorization_servers`, `bearer_methods_supported`, and `resource_signing_alg_values_supported` per RFC 9728.
- **Coaching methodology content** — The `coaching-guide.md` covers all required topics from the Stage 4 requirements: information gathering, ball allocation, session balance, drill design, tool sequence, and data format rules.
- **CI/CD workflow** — `mcp-deploy.yml` runs typecheck → lint → test before deploying, paths-filtered to `apps/mcp/**`, with correct secret references.
- **`get_user_clubs` output** — Returns `code`, `display_name`, `category` (not `sort_order`), ordered correctly via `clubs(sort_order)`.
- **`list_units` ball count handling** — `has_uncounted_instructions` and null `total_ball_count` logic is correct per the F5 flag resolution.
- **Consent page state machine** — `setState()` correctly hides/shows all three states. Approve/deny buttons are disabled during the async call to prevent double-submission.
- **Security posture** — No service-role key is used anywhere in the Worker. All data access goes through the user's JWT with RLS. The anon key + user token pattern is correct.
