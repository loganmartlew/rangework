# Stage 5 — End-to-End Integration Test Runbook

> **Epic:** [RWK-4 — AI Session Creation](https://loganmartlew.atlassian.net/browse/RWK-4)
> **Ticket:** [RWK-34 — End-to-end integration testing](https://loganmartlew.atlassian.net/browse/RWK-34)
> **Updated:** 2026-06-22

This runbook contains step-by-step instructions for all 11 test scenarios (S1–S11) that validate the complete RWK-4 MCP integration. A tester unfamiliar with the codebase should be able to follow it.

---

## Prerequisites

Before starting, ensure everything on this checklist is in place:

- [ ] All Stage 1–4 deliverables complete and deployed to production
- [ ] Supabase OAuth 2.1 server enabled and configured (RWK-28)
- [ ] JWT signing set to asymmetric (RS256/ES256) — verified by RWK-28
- [ ] MCP Worker deployed and reachable at `https://mcp.rangework.app/mcp`
- [ ] Consent page deployed at `https://rangework.app/oauth/consent`
- [ ] Test accounts provisioned (see below)
- [ ] Android debug build installed on device/emulator with test Supabase credentials
- [ ] Claude.ai account with MCP connector support
- [ ] (Optional) ChatGPT Plus/Team account with developer mode enabled
- [ ] `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` secrets exist in GitHub
- [ ] No `SUPABASE_SERVICE_KEY` secret exists in Cloudflare dashboard

---

## Environment Setup

### Cloudflare — DNS & Secrets (D5)

One-time setup before the Worker can be deployed to a custom domain:

1. **Custom domain DNS**: In the Cloudflare Dashboard, add a CNAME record:
   - **Name**: `mcp`
   - **Target**: `rangework-mcp.<your-subdomain>.workers.dev`
   - **Proxy status**: Proxied (orange cloud)

   This makes the Worker accessible at `https://mcp.rangework.app`.

2. **Secrets**: Navigate to Workers & Pages → `rangework-mcp` → Settings → Variables.
   The following must be set:
   - `SUPABASE_ANON_KEY` — set via `wrangler secret put SUPABASE_ANON_KEY` or the dashboard.
   - `SUPABASE_URL` — can be set as a plain variable (not secret) in the dashboard, or via `wrangler secret put SUPABASE_URL`.

3. **R2 bucket**: Confirm the `rangework` R2 bucket exists:

   ```bash
   wrangler r2 bucket list
   ```

   If it does not exist, create it:

   ```bash
   wrangler r2 bucket create rangework
   ```

   Upload the coaching guide:

   ```bash
   wrangler r2 object put rangework/mcp/coaching-guide.md \
     --file methodology/coaching-guide.md \
     --content-type text/markdown
   ```

4. **Verification**: Health check endpoint (unauthenticated):
   ```bash
   curl https://mcp.rangework.app/health
   ```
   Expected response: `{"status":"ok"}`

### Supabase — Short-TTL Token Configuration (D6)

For the token expiry / re-auth test (S7), configure a shorter JWT TTL. **This is a project-wide setting — it affects all users briefly.**

1. Navigate to Supabase Dashboard → Authentication → Settings.
2. Under **JWT Settings**, note the current **JWT expiry** value (default: `3600` seconds / 1 hour).
3. **Record the original value here**: `________` seconds.
4. Temporarily change the JWT expiry to `300` seconds (5 minutes).
5. **Important**: Restore the original value after S7 testing completes (see Cleanup section).

### Test Accounts

Two real Google accounts are needed (E2-A):

| Role                             | Purpose                                                                                 |
| -------------------------------- | --------------------------------------------------------------------------------------- |
| **Account A** (primary tester)   | Main test account with clubs enabled. Used for S1–S5, S7–S10.                           |
| **Account B** (isolation target) | Second account for RLS isolation testing (S6). Must have no shared data with Account A. |

**Account A email:** `__________________`
**Account B email:** `__________________`

**Account A club setup**: Sign in to the Rangework Android app as Account A and enable at minimum: driver, 7-iron, pitching wedge, putter, gap wedge, sand wedge.

### Android App

- Build variant: `assembleDebug`
- Supabase credentials configured via `~/.gradle/gradle.properties` (see `README.md`)
- Physical device or emulator signed in as Account A

### MCP Inspector Regression Script

The committed regression script at `apps/mcp/scripts/regression.ts` can be run against any Worker instance:

```bash
# Local dev
MCP_WORKER_URL=http://localhost:8787/mcp \
MCP_TEST_TOKEN=<jwt> \
npx tsx apps/mcp/scripts/regression.ts

# With auth isolation check (S6)
MCP_WORKER_URL=http://localhost:8787/mcp \
MCP_TEST_TOKEN=<jwt-for-account-a> \
SECOND_TEST_TOKEN=<jwt-for-account-b> \
npx tsx apps/mcp/scripts/regression.ts

# Production
MCP_WORKER_URL=https://mcp.rangework.app/mcp \
MCP_TEST_TOKEN=<jwt> \
npx tsx apps/mcp/scripts/regression.ts
```

Obtain a test token via the Supabase CLI:

```bash
supabase auth user --token <user-id>
```

---

## Scenarios

### S1 — Connect from Claude.ai (Full OAuth Flow)

**Type:** Happy-path — Manual (Claude.ai UI)
**Depends on:** RWK-28, RWK-33
**Preconditions:**

- [ ] Claude.ai account with MCP connector support available
- [ ] Rangework MCP server deployed and reachable at `https://mcp.rangework.app`
- [ ] Consent page deployed at `https://rangework.app/oauth/consent`
- [ ] Account A signed out of all Rangework sessions (to test fresh OAuth)

**Steps:**

1. Open Claude.ai → Settings (or Profile menu) → MCP Connectors → Add connector
2. Enter the Rangework MCP server URL: `https://mcp.rangework.app`
3. **Expected:** Claude discovers the OAuth authorization server and redirects to Supabase login
4. **Expected:** User is redirected to `rangework.app/oauth/consent?authorization_id=...`
5. If not already signed in, sign in with Google using **Account A**
6. **Expected:** Consent page shows "Rangework MCP" requesting "Manage your practice data" (or similar scopes)
7. Click **Approve**
8. **Expected:** User is redirected back to Claude.ai
9. **Expected:** Claude.ai shows "Connected" status with the Rangework MCP server

**Pass/Fail Criteria:**

- [ ] **Pass:** Steps 1–9 complete without errors. Claude.ai shows connected state.
- [ ] **Fail:** Any redirect failure, consent page error, or Claude.ai shows "Connection failed."
- [ ] **Fail:** Claude.ai connects but tools/list returns no tools.

**Actual Result:** (fill during execution)

---

---

### S2 — Connect from ChatGPT web (Developer Mode)

**Type:** Happy-path — Manual (ChatGPT UI)
**Depends on:** RWK-28, RWK-33
**Preconditions:**

- [ ] ChatGPT Plus/Team account with developer mode enabled
- [ ] Same server and consent page as S1

**Steps:**

Same as S1 but in ChatGPT's MCP connector UI.

1. Open ChatGPT → Settings → Developer mode → MCP Connectors → Add connector
2. Enter `https://mcp.rangework.app`
3. Follow the OAuth flow as in S1 steps 3–9

**Pass/Fail Criteria:**

- [ ] **Pass:** Steps complete without errors. ChatGPT shows connected state with all tools.
- [ ] **Partial:** Connected, but write tools (`create_unit`, `create_session`) are gated/disabled — document as "partial — read tools only."
- [ ] **Fail:** Connection fails entirely.

**Actual Result:** (fill during execution)

---

---

### S3a — Beginner with a Slice (Persona 1)

**Type:** Happy-path — Manual (Claude.ai conversation)
**Depends on:** RWK-31, RWK-32
**Preconditions:**

- [ ] Claude.ai connected to Rangework MCP (from S1)
- [ ] Account A has clubs enabled (driver, 7-iron, pitching wedge, putter minimum)

**Persona Script — share with Claude:**

> "I'm a beginner golfer, I've been playing about 6 months. My biggest problem is slicing my driver and long irons. I have an hour at the range today and about 80 balls. Can you help me build a practice plan?"

**Expected LLM behaviour:**

1. Calls `get_user_clubs` to learn available clubs
2. Asks clarifying questions (handicap, miss pattern details, distance unit preference)
3. Proposes a plan with specific drills addressing the slice
4. Calls `create_unit` for each drill (likely 3–5 units)
5. Summarizes the proposed session and asks for confirmation
6. Calls `create_session` with the unit IDs
7. Returns the session ID

**Pass/Fail Criteria:**

- [ ] **Pass:** All tool calls succeed. Conversation is coherent. LLM adapts drills to the stated slice problem. Units created with appropriate club references.
- [ ] **Fail:** Any tool returns an error. LLM generates a plan without calling `get_user_clubs`. LLM suggests clubs the user doesn't have.

**Actual Result:** (fill during execution)

---

---

### S3b — Single-Digit Working on Wedges (Persona 2)

**Type:** Happy-path — Manual (Claude.ai conversation)
**Depends on:** RWK-31, RWK-32
**Preconditions:**

- [ ] Claude.ai connected to Rangework MCP (from S1)
- [ ] Account A has gap wedge and sand wedge enabled

**Persona Script — share with Claude:**

> "I'm a 4 handicap. My full swing is solid but my wedge game inside 100 yards is costing me strokes. I have 90 minutes and about 120 balls. I want to focus on distance control with my gap wedge and sand wedge."

**Expected LLM behaviour:** Similar to S3a but with wedge-focused drills and distance-control methodology.

**Pass/Fail Criteria:**

- [ ] **Pass:** All tool calls succeed. LLM proposes wedge-specific drills. Created units reference gap wedge / sand wedge club codes.
- [ ] **Fail:** Any tool returns an error. LLM suggests full-swing drills instead of wedge work.

**Actual Result:** (fill during execution)

---

---

### S4 — `get_user_clubs` Informs Club Selection

**Type:** Happy-path — Manual (embedded in S3a & S3b)
**Depends on:** RWK-31
**Preconditions:** Same as S3a and S3b

**Verification checklist — review conversations from S3a and S3b:**

- [ ] The LLM called `get_user_clubs` before proposing any club-specific drills
- [ ] The LLM used club `code` values (not `display_name`) in `create_unit` and `create_session` calls
- [ ] The LLM did not suggest clubs the user doesn't have enabled

**Pass/Fail Criteria:**

- [ ] **Pass:** All three conditions met in both persona runs.
- [ ] **Fail:** Any condition not met.

**Actual Result:** (fill during execution)

---

---

### S5a — Created Data Appears in Android App

**Type:** Happy-path — Manual (Android debug build)
**Depends on:** RWK-31, Android app
**Preconditions:**

- [ ] Android debug build installed with test Supabase credentials
- [ ] S3a or S3b completed (units and session exist in the database)
- [ ] Device/emulator signed in as Account A

**Steps:**

1. Open the Rangework Android app
2. Navigate to **Units** list → pull to refresh
3. **Expected:** All units created by the LLM appear with correct titles, instructions, and ball counts
4. Navigate to **Sessions** list → pull to refresh
5. Open the session created by the LLM
6. **Expected:** The session shows correct unit lineup, repeat counts, and club assignments

**Pass/Fail Criteria:**

- [ ] **Pass:** All created data appears correctly. No missing fields or display issues.
- [ ] **Fail:** Data missing, truncated, or misformatted.

**Actual Result:** (fill during execution)

---

---

### S5b — Multi-Unit Session in Android App

**Type:** Happy-path — Manual (Android debug build)
**Depends on:** RWK-31, Android app
**Preconditions:**

- [ ] A session with 4–5 units created via the LLM (from S3a or S3b should suffice)
- [ ] Android debug build signed in as Account A

**Steps:**

Same as S5a but specifically verify the multi-unit session renders all items correctly:

1. Navigate to Sessions → open the multi-unit session
2. Verify all 4–5 items display in the correct order
3. Verify each item shows the correct unit title, repeat count, and club assignment

**Pass/Fail Criteria:**

- [ ] **Pass:** All 4–5 items display correctly with proper order, titles, and metadata.
- [ ] **Fail:** Items missing, wrong order, truncated, or display errors.

**Actual Result:** (fill during execution)

---

---

### S6 — Auth Isolation

**Type:** Security — MCP Inspector script
**Depends on:** RWK-30, RWK-31
**Preconditions:**

- [ ] Account A has units and sessions created during S3a/S3b
- [ ] Account B is a separate Google account with no shared data
- [ ] Valid JWTs for both accounts obtained

**Method:** MCP Inspector regression script with `SECOND_TEST_TOKEN`

```bash
MCP_WORKER_URL=https://mcp.rangework.app/mcp \
MCP_TEST_TOKEN=<account-a-jwt> \
SECOND_TEST_TOKEN=<account-b-jwt> \
npx tsx apps/mcp/scripts/regression.ts
```

**The script asserts:**

1. Account A's units are NOT visible when using Account B's token
2. Account B's units are NOT visible when using Account A's token
3. Account B can create their own unit (writes under B's ownership)
4. Account A cannot see Account B's newly created unit

**Pass/Fail Criteria:**

- [ ] **Pass:** All assertions pass. Each account's data is fully isolated.
- [ ] **Fail:** Any cross-account data leak. Account B can see Account A's data.

**Actual Result:** (fill during execution)

---

---

### S7 — Token Expiry / Re-auth Behaviour

**Type:** Security — Manual (Claude.ai, short-TTL token)
**Depends on:** RWK-30, short-TTL token config (D6)
**Preconditions:**

- [ ] Supabase JWT expiry temporarily set to 300 seconds (5 minutes) — see Environment Setup
- [ ] Claude.ai connected to Rangework MCP

**Steps:**

1. Connected to Claude.ai with the short-TTL token active
2. Start a planning conversation (or resume from S3a)
3. Wait for the token to expire (5+ minutes of inactivity)
4. Send a follow-up message that would trigger a tool call (e.g., "Build a session with what you created")

**Expected:** Observe Claude.ai's behaviour:

- Does it automatically re-trigger OAuth?
- Does it surface a clear error?
- Does it fail silently?

**Pass/Fail Criteria:**

- [ ] **Pass:** Claude.ai automatically re-triggers OAuth and reconnects, OR surfaces a clear error with instructions to reconnect.
- [ ] **Pass (with note):** User can manually reconnect and resume.
- [ ] **Fail:** Claude.ai fails silently, shows an unhelpful error, or data is corrupted.

**Actual Result:** (fill during execution)

---

---

### S8 — `list_units` → Reuse Existing Unit in `create_session`

**Type:** Read-then-write — Manual (Claude.ai conversation)
**Depends on:** RWK-31
**Preconditions:**

- [ ] Claude.ai connected
- [ ] Account A has at least 3 existing practice units from prior runs (S3a/S3b results)

**Persona Script — share with Claude:**

> "I want to create a new practice session. First, show me what units I already have, then build a session that reuses two of them and adds one new unit."

**Expected LLM behaviour:**

1. Calls `list_units`
2. Identifies existing units by title
3. Proposes a session reusing 2 existing units + 1 new unit
4. Calls `create_unit` for the new unit only
5. Calls `create_session` referencing both existing and new unit IDs

**Pass/Fail Criteria:**

- [ ] **Pass:** LLM correctly reuses existing unit IDs. No duplicate units created. Session references mix of old and new unit IDs.
- [ ] **Fail:** LLM creates duplicate units or fails to reference existing IDs.

**Actual Result:** (fill during execution)

---

---

### S9 — Empty Account (No Clubs, No Units)

**Type:** Edge case — Manual (Claude.ai conversation)
**Depends on:** RWK-31
**Preconditions:**

- [ ] Fresh test account (or Account B) with no enabled clubs and no units
- [ ] Claude.ai connected with this account's OAuth

**Persona Script — share with Claude:**

> "I'm new to Rangework. Help me set up a practice plan."

**Expected LLM behaviour:**

1. Calls `get_user_clubs` → receives `{ clubs: [] }`
2. Informs the user they have no clubs enabled and should set up their bag in the Rangework app first
3. Does **NOT** attempt to call `create_unit` or `create_session` without clubs

**Pass/Fail Criteria:**

- [ ] **Pass:** LLM handles the empty state gracefully. No errors. Clear guidance to the user.
- [ ] **Fail:** LLM attempts to create units/sessions without club data, or surfaces an unhelpful error.

**Actual Result:** (fill during execution)

---

---

### S10 — Invalid Club Code Error UX

**Type:** Error path — Manual (Claude.ai conversation)
**Depends on:** RWK-31
**Preconditions:**

- [ ] Claude.ai connected as Account A
- [ ] Account A has clubs enabled

**Persona Script — share with Claude:**

> "Create a practice unit for my 'super driver' club."

(There is no club code `super_driver` in the catalog.)

**Expected LLM behaviour:**

1. Attempts to call `create_unit` with `default_club_reference: "super_driver"`
2. Receives `UNKNOWN_CLUB_CODE` error with `valid_codes` in the data
3. Surfaces the error to the user in plain language
4. Offers to use one of the valid club codes instead

**Pass/Fail Criteria:**

- [ ] **Pass:** Error is surfaced clearly to the user. LLM recovers and offers valid alternatives.
- [ ] **Fail:** LLM crashes, retries indefinitely, or gives a confusing error message.

**Actual Result:** (fill during execution)

---

---

### S11 — Service-Role Key Absence (Config Audit)

**Type:** Config audit — Manual (Cloudflare dashboard)
**Depends on:** RWK-29
**Preconditions:** Access to Cloudflare Dashboard

**Steps:**

1. Open Cloudflare Dashboard → Workers & Pages → `rangework-mcp` → Settings → Variables
2. Confirm no `SUPABASE_SERVICE_KEY` or `SUPABASE_SERVICE_ROLE_KEY` secret exists
3. Confirm only `SUPABASE_URL` and `SUPABASE_ANON_KEY` are bound

**Pass/Fail Criteria:**

- [ ] **Pass:** No service-role key present.
- [ ] **Fail:** Service-role key found in the Worker's environment.

**Actual Result:** (fill during execution)

---

---

## Cleanup

After all test scenarios are complete:

- [ ] **Restore JWT expiry**: Return the Supabase JWT expiry value to its original setting (from 300s back to the default `3600` seconds or the recorded value).
- [ ] **Delete test data**: Use the Android app or Supabase dashboard to delete any units/sessions created during testing (e.g., items with `[TEST]` prefix).
- [ ] **Check for orphaned data**: Run `list_units` and `list_sessions` to confirm no test artifacts remain.
- [ ] **Disconnect test connectors**: Remove the Rangework MCP connector from Claude.ai and ChatGPT.
- [ ] **Document results**: Fill in `stage5/test-report.md` with all results.
- [ ] **(Optional) Delete test accounts**: If the throwaway test accounts are no longer needed, delete them from the Supabase Auth → Users dashboard.

---

## Regression Script Usage

The committed regression script exercises all five data tools plus `get_coaching_guide`:

```bash
# Basic — all tools with one test account
MCP_WORKER_URL=https://mcp.rangework.app/mcp \
MCP_TEST_TOKEN=<jwt> \
npx tsx apps/mcp/scripts/regression.ts

# Auth isolation — two test accounts
MCP_WORKER_URL=https://mcp.rangework.app/mcp \
MCP_TEST_TOKEN=<jwt-for-a> \
SECOND_TEST_TOKEN=<jwt-for-b> \
npx tsx apps/mcp/scripts/regression.ts

# Local dev
MCP_WORKER_URL=http://localhost:8787/mcp \
MCP_TEST_TOKEN=<jwt> \
npx tsx apps/mcp/scripts/regression.ts
```

Run this after any Worker deploy or config change to confirm basic tool functionality before running the full manual suite.
