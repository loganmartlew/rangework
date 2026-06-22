# Stage 2 — Requirements Questions

**Tracks:** RWK-30 (Token validation + user context) · RWK-33 (Consent page + web login)

Questions that must be answered before the Stage 2 implementation plan can be written. Auto-resolved questions show a pre-filled answer — no action needed unless you disagree.

---

## Track A — Token Validation (RWK-30)

### A1 — JWKS Endpoint: Hardcode or Discover?

Should the token validator fetch JWKS from a hardcoded path or discover it from the OAuth AS metadata?

**Options:**

- **A.** Hardcode `{SUPABASE_URL}/auth/v1/.well-known/jwks.json` — works independently of RWK-28
- **B.** Discover from `{SUPABASE_URL}/auth/v1/.well-known/oauth-authorization-server` — more correct, but adds a runtime dependency on RWK-28 being live first

**Recommendation:** A — stable URL, no blocking dependency on RWK-28.

> **Answer:** \_\_\_ I'd rather go the more correct approach of B

---

### A2 — JWKS Caching Strategy

Workers are stateless, but per-isolate in-memory state may persist briefly. How should the JWKS be cached?

**Options:**

- **A.** No cache — fetch on every request (simple, adds latency)
- **B.** In-memory TTL cache (e.g. 5–10 min) — fast, low complexity
- **C.** TTL cache + refresh on `kid` miss — handles key rotation, more code

**Recommendation:** B with a 5-minute TTL. Fast and handles most rotation scenarios without complexity.

> **Answer:** \_\_\_ B with a 5-minute TTL

---

### A3 — Clock Skew Tolerance ✅ Resolved

> **Answer:** **30 seconds**, applied to both `exp` and `nbf`. Standard default.

---

### A4 — JWT Algorithm (RS256 vs ES256)

Hard dependency on RWK-28, which must decide the signing algorithm. RWK-30 must pin the expected `alg` to prevent algorithm-confusion attacks.

**Options:**

- **A.** RS256
- **B.** ES256
- **C.** Accept both — less secure, widens attack surface

**Recommendation:** Confirm with RWK-28 and use whichever they pick. Implement as a `JWT_ALGORITHM` env var so it requires no code change.

> **Answer (blocked on RWK-28):** \_\_\_ A

---

### A5 — Issuer (`iss`) Claim Value

What exact string should the validator expect in the `iss` claim?

**Options:**

- **A.** `{SUPABASE_URL}/auth/v1` — current Supabase default
- **B.** The `issuer` field from the OAuth AS metadata — may differ post-RWK-28
- **C.** Configurable via `JWT_ISSUER` env var — set to whichever A or B resolves to

**Recommendation:** C — avoids a code change when RWK-28 finalises the issuer value.

> **Answer:** \_\_\_ C

---

### A6 — Audience (`aud`) Claim Validation

Should the validator check `aud`, and if so, against what value?

**Options:**

- **A.** Don't validate `aud` — simpler, but allows cross-resource token replay
- **B.** Validate `aud = "authenticated"` — matches current Supabase behaviour
- **C.** Validate `aud = MCP client_id` — more correct for OAuth AS tokens, but needs RWK-28 confirmation

**Recommendation:** B for v1. Revisit once RWK-28 confirms what `aud` the OAuth AS emits.

> **Answer:** \_\_\_ B

---

### A7 — Per-Request Supabase Client Construction ✅ Resolved

> **Answer:** Create a new `createClient(url, anonKey, { global: { headers: { Authorization: \`Bearer ${jwt}\` } }, auth: { persistSession: false } })` per request. Anon key is still required for PostgREST. No connection pooling.

---

### A8 — Auth Error Response Shape

What shape should auth errors return?

**Options:**

- **A.** Simple `401` with a JSON body `{ error: "...", message: "..." }`
- **B.** `401` + `WWW-Authenticate: Bearer resource_metadata="..."` header + JSON-RPC error object — RFC 9728 compliant, enables MCP client discovery
- **C.** MCP native error codes only (no HTTP-level header)

**Recommendation:** B — required for MCP clients (Claude.ai, ChatGPT) to discover the consent flow automatically.

> **Answer:** \_\_\_ B

---

### A9 — Protected Resource Metadata Endpoint Ownership

RFC 9728 defines `/.well-known/oauth-protected-resource`, which the `WWW-Authenticate` header points at. Who owns this Worker route?

**Options:**

- **A.** RWK-30 adds the route as part of the auth error flow
- **B.** RWK-29 (scaffold) was supposed to own it — RWK-30 is blocked if skipped
- **C.** Defer to v2

**Recommendation:** A — RWK-30 owns it; it's a small static JSON response.

> **Answer:** \_\_\_ A

---

### A10 — Test JWT Before the Consent Flow Exists

How should RWK-30 be tested before RWK-33 is built?

**Options:**

- **A.** `signInWithPassword` script — email/password via the Supabase anon key (note: claims may differ from OAuth-issued tokens)
- **B.** Copy a JWT from the Android app's active session
- **C.** Supabase CLI `supabase auth user`

**Recommendation:** A — document a small `test-token.ts` script; note which claims will differ from real OAuth tokens so early passing tests don't give false confidence.

> **Answer:** \_\_\_ C

---

### A11 — Auth Failure Logging ✅ Resolved

> **Answer:** Log `kid`, `iss`, and error reason (e.g. "expired", "invalid signature"). Never log `sub` or the raw JWT.

---

### A12 — `user_id` Claim Source ✅ Resolved

> **Answer:** Read from the `sub` claim. Require `role === "authenticated"`. Reject tokens missing either.

---

## Track B — Consent Page (RWK-33)

### B1 — Island Framework

The existing `apps/site` has no island framework. What client-side approach should the consent page use?

**Options:**

- **A.** Vanilla TypeScript — zero added deps, slightly more verbose for stateful UI
- **B.** Preact via `@astrojs/preact` — ~3 KB runtime, React-like model
- **C.** Solid via `@astrojs/solid-js` — ~7 KB runtime, reactive model

**Recommendation:** A — the consent page has ~3 states (loading / consent / error). Vanilla TS handles this without adding a framework runtime to the site bundle.

> **Answer:** \_\_\_ B

---

### B2 — Consent Page Route & Param Name

The route must match exactly what RWK-28 configures in Supabase OAuth settings.

**Options:**

- **A.** `/oauth/consent?authorization_id=` — matches roadmap §4 example
- **B.** `/auth/consent?authorization_id=`
- **C.** Confirm with RWK-28 and use whatever path they configure

**Recommendation:** C — coordinate with RWK-28 before implementing. Propose `/oauth/consent?authorization_id=` as the default if they have no preference.

> **Answer (coordinate with RWK-28):** \_\_\_ A

---

### B3 — Web Login: Redirect vs Popup ✅ Resolved

> **Answer:** Use redirect (`signInWithOAuth` default). Pass `authorization_id` as a query param on the `redirectTo` URL so it survives the Google sign-in redirect.

---

### B4 — Supabase Redirect URL Allowlist

Which URLs need to be added to the Supabase redirect allowlist, and where?

**Options:**

- **A.** Add to `supabase/config.toml` (`additional_redirect_urls`) — source-controlled
- **B.** Dashboard-only — not source-controlled, easier for prod-only URLs
- **C.** Both — config.toml for dev, dashboard for prod

**Recommendation:** A — commit to `config.toml`. Likely entries: `https://rangework.app`, `https://www.rangework.app`, `http://localhost:4321`.

> **Answer:** \_\_\_ C

---

### B5 — Google Web Client ID

**Options:**

- **A.** Reuse the existing Android web client ID — add `rangework.app` to authorized origins in Google Cloud Console
- **B.** Create a new web-origin OAuth client in Google Cloud Console

**Recommendation:** A — one client to maintain. Add the site origin + `localhost:4321` to the existing client's authorized JavaScript origins and redirect URIs.

> **Answer:** \_\_\_ A

---

### B6 — Cross-User Session Handling

What if a signed-in browser session belongs to a different user than the `authorization_id` was created for?

**Options:**

- **A.** Trust Supabase — `getAuthorizationDetails` should error for the wrong user; surface that error
- **B.** Proactively sign out and redirect to login when the session user doesn't match the authorization owner

**Recommendation:** A — test first to confirm Supabase enforces this server-side. Add B only if it doesn't.

> **Answer:** \_\_\_ A

---

### B7 — `getAuthorizationDetails` Response Shape

Supabase docs don't fully document what this method returns.

**Options:**

- **A.** Infer from Supabase SDK source — risk: incomplete or stale
- **B.** Run a real authorization and `console.log` the full response before building the UI
- **C.** Build with placeholder fields and adjust when a real response is observed

**Recommendation:** B — always verify against a real Supabase authorization before writing the component contract.

> **Answer:** \_\_\_ B

---

### B8 — Scope Display Text

> ⚠️ **Blocked on C1 (Scope Definition).** Once C1 is answered, map each scope string to a plain-language description and fill in below.

> **Answer (fill in after C1):** \_\_\_ manage_practice_data

---

### B9 — Redirect Handling After Approve / Deny ✅ Resolved

> **Answer:** Use `window.location.replace(redirect_url)` on the URL returned by `approveAuthorization()` / `denyAuthorization()`. Trust Supabase's returned URL — it's generated from the registered client redirect URI, so no additional validation is needed.

---

### B10 — Error State Copy & UX ✅ Resolved

> **Answer:**
>
> - Missing `authorization_id` → "This link is invalid." (no retry)
> - Expired request → "This authorization request has expired. Please reconnect from your app." (no retry)
> - Supabase / network error → "Something went wrong. Please try again." + retry button

---

### B11 — Post-Approve Destination ✅ Resolved

> **Answer:** Follow the `redirect_url` returned by `approveAuthorization()`. The MCP client's callback handles everything after. No Rangework success interstitial needed.

---

### B12 — Consent Page Styling ✅ Resolved

> **Answer:** Standalone chrome-less shell — no Nav or Footer from the marketing site. Use `ui-tokens` colour and typography tokens for consistency, but no full-site layout chrome. Keeps the user focused on the consent decision.

---

### B13 — Accessibility & i18n ✅ Resolved

> **Answer:** English-only for v1. Minimum a11y: semantic `<button>` elements, visible focus ring on Approve/Deny, `<ul>` for the scope list with an `aria-label`. No i18n infrastructure for v1.

---

### B14 — CSRF / State Parameter Across Google Redirect

How should `authorization_id` be preserved across the Google sign-in redirect?

**Options:**

- **A.** Pass as part of the `redirectTo` callback URL query string — visible in referer headers but survives redirects
- **B.** Store in `sessionStorage` before redirect, read on return — survives redirect but XSS-readable
- **C.** Rely on Supabase's built-in `state` parameter in `signInWithOAuth`

**Recommendation:** A — pass `authorization_id` as a query param on the `redirectTo` URL. The redirect chain is Supabase-controlled, so it's scoped and predictable.

> **Answer:** \_\_\_ A

---

### B15 — `@supabase/supabase-js` Dependency ✅ Resolved

> **Answer:** Add as a `dependency` (ships browser-side code). Pin to the same minor version as `apps/mobile`. Env vars: `PUBLIC_SUPABASE_URL` and `PUBLIC_SUPABASE_ANON_KEY` (Astro `PUBLIC_` convention), injected at build time via `.env`.

---

## Cross-cutting

### C1 — Scope Definition

**This decision blocks B8, RWK-30 token enforcement, and the consent UI copy.**

**Options:**

- **A.** Single broad scope: `manage_practice_data` — simpler consent UI, simpler token validation
- **B.** Read/write split: `read_practice_data` + `write_practice_data` — least-privilege, but adds scope enforcement in RWK-30 and more consent UI copy
- **C.** Granular per-resource scopes — maximum control, significant overhead

**Recommendation:** A for v1. Add a read/write split in v2 if users request read-only integrations.

> **Answer:** \_\_\_ A

---

### C2 — URL Coordination Table ✅ Resolved

> **Answer:** Once RWK-28 is done, record a canonical URL table (MCP server URL, `/.well-known/oauth-protected-resource` URL, Supabase authorization server URL, consent page URL) in a shared `design-docs/RWK4-ai-integration/urls.md`. All three tickets (RWK-28/30/33) reference it.

---

### C3 — Testing Strategy ✅ Resolved

> **Answer:**
>
> - **RWK-30:** Vitest unit tests with forged/expired/invalid JWTs + a manual `test-token.ts` script for testing against the real JWKS
> - **RWK-33:** Manual flow test against a real Supabase OAuth authorization; no component unit tests in v1

---

### C4 — Environment Variable Names ✅ Resolved

> **Answer:**
>
> - **Worker (RWK-30):** `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `JWT_ISSUER`, `JWT_ALGORITHM`
> - **Site (RWK-33):** `PUBLIC_SUPABASE_URL`, `PUBLIC_SUPABASE_ANON_KEY`
> - Both point to the same Supabase project.

---

### C5 — Supabase Beta Version Pinning ✅ Resolved

> **Answer:** Pin `@supabase/supabase-js` to a specific minor version in both `apps/site` and `apps/mcp`. Add a comment in each `package.json` noting the beta status. Subscribe to the Supabase changelog for breaking changes.
