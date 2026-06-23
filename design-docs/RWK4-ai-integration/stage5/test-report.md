# Stage 5 — End-to-End Integration Test Report

> **Epic:** [RWK-4 — AI Session Creation](https://loganmartlew.atlassian.net/browse/RWK-4)
> **Ticket:** [RWK-34 — End-to-end integration testing](https://loganmartlew.atlassian.net/browse/RWK-34)
> **This template:** Fill in after test execution

---

## Run Metadata

| Field                     | Value                                                              |
| ------------------------- | ------------------------------------------------------------------ |
| **Date of test run**      | YYYY-MM-DD                                                         |
| **Tester**                | Logan Martlew                                                      |
| **Client(s)**             | Claude.ai (date: YYYY-MM-DD) / ChatGPT web (date: YYYY-MM-DD)      |
| **Worker URL**            | `https://mcp.rangework.app/mcp` (or `localhost:8787/mcp` if local) |
| **Consent page URL**      | `https://rangework.app/oauth/consent`                              |
| **Test Account A**        | `__________________`                                               |
| **Test Account B**        | `__________________`                                               |
| **Android build**         | `assembleDebug` (commit: `________`)                               |
| **Worker version/commit** | Commit: `________`                                                 |

---

## Summary

| #   | Scenario                           | Result                         | Notes |
| --- | ---------------------------------- | ------------------------------ | ----- |
| S1  | Connect from Claude.ai             | ⬜ Pass / ⬜ Fail              |       |
| S2  | Connect from ChatGPT web           | ⬜ Pass / ⬜ Fail / ⬜ Partial |       |
| S3a | Beginner with a slice              | ⬜ Pass / ⬜ Fail              |       |
| S3b | Single-digit working on wedges     | ⬜ Pass / ⬜ Fail              |       |
| S4  | `get_user_clubs` informs selection | ⬜ Pass / ⬜ Fail              |       |
| S5a | Data in Android app                | ⬜ Pass / ⬜ Fail              |       |
| S5b | Multi-unit session in app          | ⬜ Pass / ⬜ Fail              |       |
| S6  | Auth isolation                     | ⬜ Pass / ⬜ Fail              |       |
| S7  | Token expiry / re-auth             | ⬜ Pass / ⬜ Fail              |       |
| S8  | `list_units` → reuse unit          | ⬜ Pass / ⬜ Fail              |       |
| S9  | Empty account                      | ⬜ Pass / ⬜ Fail              |       |
| S10 | Invalid club code error UX         | ⬜ Pass / ⬜ Fail              |       |
| S11 | Service-role key absence           | ⬜ Pass / ⬜ Fail              |       |

**Overall:** ⬜ All pass / ⬜ Some fail (see per-scenario details below)

---

## Per-Scenario Details

### S1 — Connect from Claude.ai

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

**Blockers encountered:**

---

### S2 — Connect from ChatGPT web

**Result:** ⬜ Pass / ⬜ Fail / ⬜ Partial

**Observations:**

-
-
-

**Blockers encountered:**

---

### S3a — Beginner with a Slice

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

**Blockers encountered:**

---

### S3b — Single-Digit Working on Wedges

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

**Blockers encountered:**

---

### S4 — `get_user_clubs` Informs Club Selection

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

- [ ] LLM called `get_user_clubs` before drills
- [ ] LLM used club codes (not display names) in tool calls
- [ ] LLM did not suggest unenabled clubs

**Notes:**

---

### S5a — Created Data in Android App

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

---

### S5b — Multi-Unit Session in Android App

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

---

### S6 — Auth Isolation

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

**Script output:**

```
(paste regression script output here)
```

---

### S7 — Token Expiry / Re-auth

**Result:** ⬜ Pass / ⬜ Fail

**Observed behaviour:**

-
-
-

**Original JWT expiry value:** `________` seconds

**Was JWT expiry restored after test?** ⬜ Yes / ⬜ No

---

### S8 — `list_units` → Reuse Existing Unit

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

---

### S9 — Empty Account

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

---

### S10 — Invalid Club Code Error UX

**Result:** ⬜ Pass / ⬜ Fail

**Observations:**

-
-
-

---

### S11 — Service-Role Key Absence (Config Audit)

**Result:** ⬜ Pass / ⬜ Fail

- [ ] Confirmed: no `SUPABASE_SERVICE_KEY` in Cloudflare dashboard
- [ ] Confirmed: no `SUPABASE_SERVICE_ROLE_KEY` in Cloudflare dashboard

**Notes:**

---

## Client Quirks & Observations

### Claude.ai

-
-

### ChatGPT web (developer mode)

-
-

---

## Test Gaps (AU4)

Error paths exercised manually but not covered by RWK-31 unit tests:

| Gap | Description | Action |
| --- | ----------- | ------ |
|     |             |        |
|     |             |        |

---

## Token Expiry Behaviour (S7) — Detailed

**Observed behaviour:**

```
Describe what actually happened when the token expired
```

**Pass/fail determination:**

**Client(s) tested on:**

---

## Sign-off

- [ ] All scenarios executed
- [ ] Test data cleaned up
- [ ] Short-TTL token config restored to original value
- [ ] Supabase changelog monitoring noted (XX4-A — Logan responsible, monthly check)
- [ ] Test report reviewed and filed

**Signed:** ********\_\_******** **Date:** YYYY-MM-DD
