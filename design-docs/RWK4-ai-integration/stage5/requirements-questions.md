# Stage 5 — Requirements Questions

**Ticket:** RWK-34 (End-to-end integration testing) · **Depends on:** All other RWK-4 child tickets

**Done when:** A complete flow — connect server, authenticate, generate a practice plan, see the units and session in the app — works without errors on at least one of Claude.ai or ChatGPT web.

**Seven test scenarios from the ticket:**

1. Connect from Claude.ai: OAuth flow completes (discovery → consent page → approve → connected)
2. Connect from ChatGPT web (developer mode): same flow
3. Invoke `build_practice_plan` and complete a planning conversation
4. Verify `get_user_clubs` informs club selection
5. Verify `create_unit` + `create_session` appear correctly in the Android app
6. Verify a second user's data is not accessible (auth isolation)
7. Test token expiry / re-auth behaviour

Questions below must be answered before the implementation plan can be written. Auto-resolved questions have a pre-filled answer.

---

## Test Scope & Scenarios

### TS1 — Personas for Happy-Path Runs

Stage 4's personas document doesn't exist yet.

**Options:**

- **A.** Use the two Jira-named personas: "beginner with a slice" + "single-digit working on wedges" — documented as scripted inputs
- **B.** Block on Stage 4 producing a personas artifact first
- **C.** Run ad-hoc with no scripted input

**Recommendation:** A — use the named personas as scripted inputs. Document the scripts in Stage 5 if Stage 4 hasn't produced them.

> **Answer:** A — use the two Jira-named personas: "beginner with a slice" + "single-digit working on wedges" — documented as scripted inputs.

---

### TS2 — Read Tool Coverage

Scenarios 3–5 cover `get_user_clubs` → `create_unit` → `create_session`. Should `list_units` and `list_sessions` also be exercised?

**Options:**

- **A.** Include a scenario where the LLM lists existing units before creating a session
- **B.** Trust that RWK-31's MCP Inspector tests cover the read tools; skip in Stage 5

**Recommendation:** A — one additional scenario ("list my existing units and build a session using one of them") validates the read-then-write flow.

> **Answer:** A — include a scenario where the LLM lists existing units before creating a session.

---

### TS3 — Empty-Account Scenario

**Options:**

- **A.** Include a fresh-account scenario (no enabled clubs, no units)
- **B.** Test with a seeded account only

**Recommendation:** A — confirms `get_user_clubs` handles an empty bag gracefully and the LLM reacts correctly (offers to help the user enable clubs).

> **Answer:** A — include a fresh-account scenario (no enabled clubs, no units).

---

### TS4 — Large-Account Scenario

**Options:**

- **A.** Include a scenario with 50+ units / 20+ sessions to test truncation behaviour
- **B.** Skip — defer until a real user hits the limit

**Recommendation:** B — not a Stage 5 gate. Truncation behaviour is covered by RWK-31 unit tests.

> **Answer:** B — skip the large-account scenario. Defer until a real user hits the limit.

---

### TS5 — Multi-Unit Session Scenario ✅ Resolved

> **Answer:** Include one session-creation scenario that references at least 4–5 units to exercise the `save_practice_session` RPC with a realistic items array.

---

### TS6 — Invalid Club Code Error Path

**Options:**

- **A.** Include a scenario where the LLM is prompted toward a non-catalog code; assert the error surfaces clearly to the user
- **B.** Skip — covered by RWK-31 unit tests

**Recommendation:** A — validates the LLM-facing error UX, which is a distinct concern from the tool's internal error handling.

> **Answer:** A — include a scenario where the LLM is prompted toward a non-catalog code; assert the error surfaces clearly to the user.

---

### TS7 — Bogus `unit_id` in `create_session`

**Options:**

- **A.** Inject a fake UUID via MCP Inspector to confirm FK/RLS error handling
- **B.** Trust RWK-31 unit tests cover this; skip manual injection

**Recommendation:** B — this is an RWK-31 unit-test concern. Stage 5 tests the LLM-driven path only.

> **Answer:** B — trust RWK-31 unit tests cover fake UUID handling. Skip manual injection in Stage 5.

---

### TS8 — RLS Synthetic Token Test

**Options:**

- **A.** Mint a token with a mismatched `sub`, call tools via MCP Inspector, assert empty/403 result
- **B.** Trust RWK-30 unit tests cover RLS enforcement

**Recommendation:** B — RWK-30 owns this. Stage 5 covers real user flows only.

> **Answer:** B — trust RWK-30 unit tests cover RLS enforcement. Skip synthetic token test in Stage 5.

---

### TS9 — Token Expiry Simulation

Scenario 7 tests "token expiry / re-auth behaviour" but the ticket doesn't specify how expiry is induced.

**Options:**

- **A.** Wait out a real token TTL (Supabase default ~1 hour — impractical for testing)
- **B.** Configure a short-TTL test token (e.g. 5 min) for the test account in Supabase
- **C.** Revoke the session via the Supabase dashboard mid-conversation

**Recommendation:** B — configure a short TTL for the test account. Document what the expected client behaviour is (re-auth prompt vs hard error) and record what actually happens as the pass/fail definition for scenario 7.

> **Answer:** B — configure a short-TTL test token (e.g. 5 min) for the test account in Supabase.

---

### TS10 — Consent Flow Coverage in Stage 5

**Options:**

- **A.** Re-test the full consent flow (discovery → consent page → approve → connected) in Stage 5 scenarios 1–2
- **B.** Assume RWK-33 verified consent in isolation; Stage 5 only checks from the "already connected" state

**Recommendation:** A for scenarios 1–2 specifically. Re-verifying the connect flow in Stage 5 ensures the integrated system works end-to-end, not just isolated components.

> **Answer:** A — re-test the full consent flow (discovery → consent page → approve → connected) in Stage 5 scenarios 1–2.

---

### TS11 — Prompt vs Fallback Tool Coverage (F6)

**Options:**

- **A.** Test both the `build_practice_plan` prompt path AND the `get_coaching_guide` fallback tool end-to-end
- **B.** Test whichever Stage 4 identifies as primary; spot-check the other

**Recommendation:** B — avoid doubling the manual test effort. If Stage 4 settles on the prompt as primary, test it end-to-end and spot-check the fallback tool.

> **Answer:** B — test whichever Stage 4 identifies as primary end-to-end; spot-check the other.

---

## Test Environments & Accounts

### E1 — Supabase Project for Testing

**Options:**

- **A.** Test against the production Supabase project — simplest, but RLS isolation testing risks real-user data
- **B.** Create a dedicated staging/test Supabase project — separate, safe for destructive tests

**Recommendation:** B — RLS isolation testing with multiple users should never run against production.

> **Answer:** A — test against the production Supabase project. (Per user decision: simplest approach; RLS isolation testing will use MCP Inspector with separate tokens, not destructive operations.)

---

### E2 — Test Account Provisioning

RLS isolation requires ≥2 real Supabase user accounts.

**Options:**

- **A.** Two real Google accounts (manually signed in via the consent page)
- **B.** Email/password test accounts (if Supabase email auth is enabled)
- **C.** Seeded test-user script

**Recommendation:** A — simplest for Google OAuth. Document the two account email addresses and their roles (primary tester, isolation target).

> **Answer:** A — two real Google accounts (manually signed in via the consent page). Document the two account email addresses and their roles.

---

### E3 — Test Data Cleanup

**Options:**

- **A.** Manual delete in the Android app / Supabase dashboard after each run
- **B.** SQL cleanup script targeting the test account's `practice_*` tables
- **C.** Use throwaway accounts — discard entirely after Stage 5

**Recommendation:** B — a cleanup script is reusable across runs and prevents accumulation. Run it between scenarios.

> **Answer:** A — manual delete in the Android app / Supabase dashboard after each run. No automated cleanup script in Stage 5.

---

### E4 — Cloudflare Worker Environment

**Options:**

- **A.** Test against the production Worker URL
- **B.** Deploy a separate staging Worker
- **C.** Use `wrangler dev` tunnelled to the MCP client (e.g. via Cloudflare Tunnel)

**Recommendation:** C for local development; B for the final Stage 5 gate run. Avoid production during testing.

> **Answer:** C — use `wrangler dev` (local) for development and testing. No separate staging Worker deployment needed. A `mcp-deploy.yml` CI workflow will be created for future automated deployments, and one manual Cloudflare config step (custom domain DNS) will be documented if not already done in Stage 1.

---

### E5 — Android App Verification Method

RWK-34 scenario 5 says "created data appears correctly in the Rangework Android app."

**Options:**

- **A.** Physical device or emulator — install debug build, pull-to-refresh, visually confirm
- **B.** Query Supabase dashboard / CLI directly — cheaper but doesn't exercise the app's UI path
- **C.** Instrumented Android test — automated but large scope addition

**Recommendation:** A — the ticket requires the app's UI, not just the DB. Use a debug build with test project credentials via `~/.gradle/gradle.properties`.

> **Answer:** A — physical device or emulator with debug build. Pull-to-refresh, visually confirm created data.

---

### E6 — App Build Variant ✅ Resolved

> **Answer:** `assembleDebug` with test Supabase credentials via `~/.gradle/gradle.properties`. Already supported by `androidApp/build.gradle.kts`.

---

### E7 — Staging OAuth Config

If a staging Supabase project is used (E1-B), RWK-28's OAuth server configuration must be repeated there.

**Options:**

- **A.** Manually replicate RWK-28 config on the staging project; document the steps
- **B.** Script the OAuth config so it's reproducible (out of Stage 5 scope)

**Recommendation:** A — document the manual steps. A script would be nice but is not a Stage 5 deliverable.

> **Answer:** \_\_\_ Not needed

---

## Target Clients (F7)

### C1 — Primary Success Target

RWK-34's done-when says "at least one of Claude.ai or ChatGPT web."

**Options:**

- **A.** Claude.ai is primary; ChatGPT web is secondary
- **B.** ChatGPT web is primary; Claude.ai is secondary
- **C.** Both must pass

**Recommendation:** A — Claude.ai has more predictable MCP support. Confirm ChatGPT write-tool availability before committing to it as a target (see C2).

> **Answer:** A — Claude.ai is primary; ChatGPT web is secondary.

---

### C2 — ChatGPT Developer-Mode Prerequisite Check (F7)

F7 flags that ChatGPT may gate non-`search`/`fetch` tools outside developer mode.

**Options:**

- **A.** Run a prerequisite check (can `create_unit` be called from ChatGPT developer mode?) before counting ChatGPT as a Stage 5 target
- **B.** Proceed and document issues if found

**Recommendation:** A — if the check fails, drop ChatGPT scenarios from Stage 5 scope and focus on Claude.ai only.

> **Answer:** B — proceed with ChatGPT as a secondary target and document issues if found. Don't gate Stage 5 on a prerequisite check.

---

### C3 — Other Clients ✅ Resolved

> **Answer:** No additional clients in Stage 5. Claude Desktop or Cursor can be spot-checked post-launch as time allows, but they are not Stage 5 gates.

---

### C4 — Client-Specific Quirks Log

**Options:**

- **A.** Document per-client observations in `design-docs/RWK4-ai-integration/stage5/client-notes.md`
- **B.** Document in `apps/mcp/README.md`
- **C.** No formal quirks log

**Recommendation:** A — a dedicated file keeps it separate from the user-facing README. Key findings can be moved to README after Stage 5 closes.

> **Answer:** C — no formal quirks log. Observations will be captured in the test report (`stage5/test-report.md`).

---

### C5 — Client Version Recording ✅ Resolved

> **Answer:** Record the date of each test run. SaaS clients don't expose build versions; the date is sufficient to correlate with changelog changes.

---

## Auth & Security Testing

### AS1 — Auth Isolation Methodology

How is scenario 6 ("second user's data is not accessible") proven?

**Options:**

- **A.** Connect as user A via the client; try to retrieve user B's unit ids from the LLM conversation; confirm empty/error
- **B.** Use MCP Inspector with user B's token to directly call `list_units` and assert an empty result
- **C.** Both A and B

**Recommendation:** B — MCP Inspector is more deterministic. The LLM may not cooperate in actively trying to read another user's data.

> **Answer:** B — use MCP Inspector with user B's token to directly call `list_units` and assert an empty result.

---

### AS2 — Token Expiry: Expected Client Behaviour

**Options:**

- **A.** Client automatically re-triggers OAuth consent — user re-approves
- **B.** Client surfaces an error; user must manually reconnect
- **C.** Unknown — test and document what actually happens

**Recommendation:** C — test and document. The observed behaviour becomes the pass/fail definition for scenario 7.

> **Answer (fill in after testing):** C — unknown. Test and document what actually happens when the token expires mid-conversation.

---

### AS3 — Consent Denial Mid-Flight

**Options:**

- **A.** Include a scenario: user denies consent during an active conversation
- **B.** Skip — too edge-case for Stage 5; RWK-33 tested deny in isolation

**Recommendation:** B — skip for v1.

> **Answer:** B — skip consent denial mid-flight. Too edge-case for Stage 5; RWK-33 tested deny in isolation.

---

### AS4 — Scope Enforcement Testing

> ⚠️ **Blocked on Stage 2 C1 (Scope Definition).** If scopes are split read/write, Stage 5 must test that a read-only token cannot call write tools. If a single broad scope, this scenario does not apply.

> **Answer (fill in after Stage 2 C1 is resolved):** N/A — Stage 2 C1 resolved to a single broad scope (`manage_practice_data`). No read/write split to test.

---

### AS5 — JWT Algorithm Regression (F8)

**Options:**

- **A.** Include an Android sign-in smoke test in Stage 5 to confirm the JWT algorithm switch (RWK-28) didn't break mobile
- **B.** Trust RWK-28's own done-criteria gate; don't repeat in Stage 5

**Recommendation:** B — RWK-28 owns this. Stage 5 can note if mobile sign-in is broken during testing, but it's not a Stage 5 scenario.

> **Answer:** B — trust RWK-28's own done-criteria gate. Don't repeat in Stage 5.

---

### AS6 — Service-Role Key Absence ✅ Resolved

> **Answer:** One-time config inspection check: confirm the Worker has no `SUPABASE_SERVICE_KEY` secret bound in the Cloudflare dashboard. Document the result in the Stage 5 report.

---

## Automation vs Manual

### AU1 — Manual vs Automated Split

**Options:**

- **A.** Purely manual — all scenarios run by hand in the client UI
- **B.** Manual for LLM conversation scenarios (3–5) + MCP Inspector scripts for auth/error scenarios (6–8)
- **C.** Full automation (not realistic for LLM conversations)

**Recommendation:** B — manual for conversation scenarios; MCP Inspector scripts for the auth isolation and error path scenarios.

> **Answer:** B — manual for LLM conversation scenarios (3–5) + MCP Inspector scripts for auth/error scenarios (6–7).

---

### AU2 — MCP Inspector Regression Script

**Options:**

- **A.** Write a committed script under `apps/mcp/scripts/` that exercises all five tools with a test token
- **B.** Manual-only for Stage 5; add automation later

**Recommendation:** A — a committed script makes future regressions (after a migration or Worker deploy) re-runnable without a full manual client flow.

> **Answer:** A — write a committed script under `apps/mcp/scripts/` that exercises all five tools with a test token.

---

### AU3 — Test Runbook / Checklist

**Options:**

- **A.** Produce a `stage5/runbook.md` with step-by-step instructions for each scenario
- **B.** Document in `apps/mcp/README.md`
- **C.** No formal runbook

**Recommendation:** A — a dedicated runbook is reusable for future regression checks. Move key steps to `apps/mcp/README.md` after Stage 5 closes.

> **Answer:** A — produce a `stage5/runbook.md` with step-by-step instructions for each scenario.

---

### AU4 — Unit Test Coverage Audit ✅ Resolved

> **Answer:** After manual runs, note any error paths exercised manually but not covered by RWK-31 unit tests. Add them to a "test gaps" section of the Stage 5 report.

---

### AU5 — CI Integration

**Options:**

- **A.** Wire an MCP Inspector smoke test into `.github/workflows/` on `apps/mcp/**` changes
- **B.** Keep Stage 5 entirely out-of-band manual for v1

**Recommendation:** B — a CI job needs a staged Worker URL, test token, and test Supabase project in CI secrets, none of which exist yet. Defer to a future stage.

> **Answer:** B — keep Stage 5 entirely out-of-band manual for v1. No CI integration.

---

### AU6 — Cloudflare Deploy Automation Gap

**Options:**

- **A.** Create a `cloudflare-deploy.yml` workflow as part of Stage 5
- **B.** Deploy manually for Stage 5; defer automation to RWK-29 or a future stage

**Recommendation:** B — out of Stage 5 scope. Deployment automation is RWK-29's responsibility.

> **Answer:** A — create a `mcp-deploy.yml` CI workflow as part of Stage 5. Assume there have been no manual deployments yet. If any manual Cloudflare setup is needed (e.g. custom domain DNS), document it in the runbook. Testing will use `wrangler dev` locally; the CI workflow enables future automated deploys.

---

## Cross-cutting

### XX1 — "Without Errors" Precision ✅ Resolved

> **Answer:** A run passes if: no tool call returns an error code, the OAuth flow completes without a redirect failure, and created data appears in the Android app. A transient 5xx that succeeds on one retry = pass. Client UI bugs unrelated to Rangework = noted but not blocking.

---

### XX2 — Test Report Format

**Options:**

- **A.** `stage5/test-report.md` in the repo
- **B.** Jira comment on RWK-34
- **C.** Both

**Recommendation:** C — `test-report.md` for detail; a Jira comment linking to it for sign-off.

> **Answer:** A — `stage5/test-report.md` in the repo. No Jira comment required.

---

### XX3 — Rollback / Ship-Gate Relationship

**Options:**

- **A.** Stages 1–4 can go live; Stage 5 is a post-launch validation
- **B.** Stage 5 is a hard gate — nothing ships until Stage 5 passes

**Recommendation:** B — end-to-end testing should be a pre-launch gate.

> **Answer:** B — Stage 5 is a hard gate. Nothing ships until Stage 5 passes.

---

### XX4 — Beta-Feature Monitoring (F11)

**Options:**

- **A.** Subscribe to the Supabase changelog; check manually once a month
- **B.** React when something breaks — no proactive monitoring
- **C.** Set up automated alerting on `@supabase/supabase-js` releases

**Recommendation:** A — manual changelog watch is sufficient for v1. Document who is responsible for the watch.

> **Answer:** A — subscribe to the Supabase changelog; check manually once a month. Logan is responsible.

---

### XX5 — Privacy / Test Data

**Options:**

- **A.** Use a dedicated throwaway test account; delete it after Stage 5
- **B.** Use Logan's real account; manually clean up test data
- **C.** Mark test data with a prefix (e.g. `"[TEST]"`) and filter in the app

**Recommendation:** A — consistent with E1-B (staging project). The test account is separate from production and can be deleted cleanly.

> **Answer:** A — use a dedicated throwaway test account; delete it after Stage 5.

---

### XX6 — Stage 4 Dependency

**Options:**

- **A.** Stage 5 formally blocks on Stage 4 completion (personas + prompt deliverable must exist)
- **B.** Stage 5 can start environment setup and auth scenarios with ad-hoc personas from the roadmap if Stage 4 slips

**Recommendation:** B — don't block Stage 5's auth and environment work on Stage 4's methodology content. The two roadmap-named personas are sufficient to start the conversation scenarios.

> **Answer:** B — Stage 5 can start environment setup and auth scenarios with ad-hoc personas from the roadmap if Stage 4 slips.

---

### XX7 — RWK-5 Stub Ticket

RWK-4's children include RWK-5 ("MCP Server"), which predates the current plan and may be superseded by RWK-29.

**Options:**

- **A.** Close RWK-5 as superseded by RWK-29
- **B.** Review RWK-5's description; fold any unique testing expectations into RWK-34

**Recommendation:** A — close as superseded. Document the closure decision in a Jira comment on RWK-5.

> **Answer:** \_\_\_ It's already closed.
