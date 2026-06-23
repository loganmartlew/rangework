# Stage 3 — Requirements Questions

**Ticket:** RWK-31 (MCP Server Tools) · **Depends on:** RWK-30 (token validation)

Five tools to implement: `get_user_clubs`, `list_units`, `list_sessions` (read) and `create_unit`, `create_session` (write). Questions below must be answered before the implementation plan can be written. Auto-resolved questions have a pre-filled answer.

---

## `get_user_clubs`

### G1 — Output Schema

What fields should each club object include?

**Options:**

- **A.** `{ code, display_name, category }` — matches roadmap spec
- **B.** `{ code, display_name, category, sort_order }` — adds bag-ordering context for the LLM at no cost

**Recommendation:** B — `sort_order` helps the LLM reason about club progression without extra queries.

> **Answer:** \_\_\_ A

---

### G2 — Category Value Format

How should the `category` enum be returned?

**Options:**

- **A.** Raw enum string: `"IRON"`, `"WOOD"`, `"WEDGE"`, etc.
- **B.** Human-readable label: `"Irons"`, `"Woods"`, `"Wedges"`, etc.

**Recommendation:** A — unambiguous for use in downstream tool calls; the LLM can humanize it in prose.

> **Answer:** \_\_\_ A

---

### G3 — Result Ordering ✅ Resolved

> **Answer:** Order by `clubs.sort_order ASC`. Matches the app's catalog ordering and is natural for bag progression (driver → putter).

---

### G4 — Empty Bag Behaviour

What if a user has disabled all their clubs in the app?

**Options:**

- **A.** Return empty array — LLM tells the user to enable clubs in the app
- **B.** Fall back to `default_enabled` clubs from the catalog
- **C.** Return the full catalog with an `enabled: boolean` flag

**Recommendation:** A — preserve the user's explicit choice. The LLM can prompt them to enable clubs via the app's Manage Clubs screen.

> **Answer:** \_\_\_ A

---

### G5 — Expose `default_enabled` Field? ✅ Resolved

> **Answer:** No. `default_enabled` is catalog metadata, not user state. Exposing it would confuse the LLM about what the user actually carries.

---

## `list_units`

### U1 — Summary Only vs Full Instructions ⚠️ Scope Gap

The roadmap specifies a summary (`id`, `title`, `instruction_count`, `total_ball_count`), but there is **no `get_unit` detail tool**, leaving the LLM with no way to read existing instruction text.

**Options:**

- **A.** Summary only — the LLM creates fresh units rather than reasoning about existing ones
- **B.** Include the full `instructions` array in each unit — larger payload, but the LLM can adapt or reuse existing drills
- **C.** Summary + add a `get_unit` detail tool (expands Stage 3 scope by one tool)

**Recommendation:** B — includes instruction text so the LLM can reason about existing units. If payload size becomes a problem for large accounts, revisit with option C.

> **Answer:** \_\_\_ B

---

### U2 — Fields Returned

Beyond the roadmap minimum (`id`, `title`, `instruction_count`, `total_ball_count`), what else should be included?

**Options:**

- **A.** Roadmap minimum only
- **B.** Add `notes`, `focus`, `default_club_reference` — coaching context the LLM needs to understand a unit's intent

**Recommendation:** B — `focus` and `notes` are key coaching context; `default_club_reference` prevents redundant `get_user_clubs` lookups.

> **Answer:** \_\_\_ B

---

### U3 — Instruction Count Definition ✅ Resolved

> **Answer:** `COUNT(*)` of `practice_unit_instructions` rows. A unit with 5 instructions (2 of which have null `ball_count`) has `instruction_count = 5`.

---

### U4 — Ball Count Null Handling (F5)

Some instructions have no `ball_count`. What should the tool return?

**Options:**

- **A.** `total_ball_count: number | null` (null = some uncounted) + `has_uncounted_instructions: boolean`
- **B.** `total_ball_count: number` (summing nulls as 0) + `uncounted_instruction_count: number`
- **C.** Sum with nulls as 0, no flag

**Recommendation:** A — null is more honest than 0 for uncounted instructions; the flag tells the LLM to hedge ("approximately X balls").

> **Answer:** \_\_\_ A

---

### U5 — Result Cap

How many units can `list_units` return?

**Options:**

- **A.** No cap — return all
- **B.** Cap at 50, no indicator
- **C.** Cap at 50 with `truncated: boolean` in the response

**Recommendation:** C — pragmatic guard against large accounts. The LLM can tell the user "showing your 50 most recently edited units."

> **Answer:** \_\_\_ A

---

### U6 — Ordering ✅ Resolved

> **Answer:** `updated_at DESC`. Matches `SupabasePracticeUnitRepository.listPracticeUnits()` and surfaces recently-edited units first.

---

### U7 — Empty State ✅ Resolved

> **Answer:** Return an empty array `[]`. Not an error. The LLM should respond by offering to create units via `create_unit`.

---

## `list_sessions`

### S1 — Session Item Schema

What fields should each item in the `items` array include?

**Options:**

- **A.** `{ unit_id, unit_title, repeat_count, sort_order }` — enough for the LLM to describe the session
- **B.** Also include `club_reference`, `notes`, `focus_cue` — full coaching context per item

**Recommendation:** B — `club_reference` and `focus_cue` are important per-item context; omitting them loses detail the LLM should surface.

> **Answer:** \_\_\_ B

---

### S2 — Session Ball Count Null Handling

How should null `ball_count` on referenced unit instructions propagate to the session total?

**Options:**

- **A.** Session total is null/flagged if any referenced unit has uncounted instructions
- **B.** Sum known counts; include a `has_uncounted_items: boolean` flag separately
- **C.** Sum with nulls as 0, no flag

**Recommendation:** B — consistent with U4; the flag tells the LLM to hedge on the total.

> **Answer:** \_\_\_ B

---

### S3 — Unit Title Join Strategy ✅ Resolved

> **Answer:** Two selects (sessions, then referenced units by id) joined in-memory in the Worker. Matches the Android app's pattern and avoids needing a SQL view.

---

### S4 — Session-Level Fields

Beyond `id`, `name`, `items`, `total_ball_count`, should anything else be returned?

**Options:**

- **A.** Roadmap spec only
- **B.** Add `notes` — often meaningful coaching context (e.g. "tournament prep session")

**Recommendation:** B — small cost, frequently useful.

> **Answer:** \_\_\_ B

---

### S5 — Result Cap, Ordering, Empty State ✅ Resolved

> **Answer:** No cap. Order by `updated_at DESC`. Empty state returns `[]`.

---

## `create_unit`

### CU1 — Required vs Optional Input Fields

**Options:**

- **A.** Required: `title`, `instructions` (at least 1). Optional: `notes`, `focus`, `default_club_reference`
- **B.** Required: `title` only. `instructions` optional

**Recommendation:** A — requiring instructions produces useful units and prevents empty-unit creation.

> **Answer:** \_\_\_ A

---

### CU2 — Instruction Element Schema ✅ Resolved

> **Answer:** `{ order: number (>0, unique within unit), text: string (non-empty), ball_count?: number (>0 or omit) }`. Matches the `save_practice_unit` RPC JSONB shape.

---

### CU3 — UUID Generation ✅ Resolved

> **Answer:** `crypto.randomUUID()` (Web Crypto API, available in Cloudflare Workers). Produces a hyphenated RFC 4122 v4 UUID that Postgres `uuid` columns accept.

---

### CU4 — Return Value

**Options:**

- **A.** Return `{ unit_id: string }` only — matches flag F3; the LLM has what it needs for `create_session`
- **B.** Re-fetch and return the full created unit — one extra round-trip

**Recommendation:** A — the id is sufficient. Document that a re-fetch is needed if the full unit is ever required downstream.

> **Answer:** \_\_\_ A

---

### CU5 — Club Code Validation Scope

`default_club_reference` must exist in the `clubs` catalog. Should validation check the full catalog or only the user's enabled clubs?

**Options:**

- **A.** Full `clubs` catalog — matches the FK constraint behaviour
- **B.** User's enabled clubs only — stricter than the DB

**Recommendation:** A — match the DB's FK semantics. Error message should list the user's enabled codes for convenience.

> **Answer:** \_\_\_ A

---

### CU6 — Club Code on Instructions ✅ Resolved

> **Answer:** No club field on instructions. The schema dropped `club_reference` from `practice_unit_instructions`. The tool schema must not accept a per-instruction club.

---

### CU7 — Empty Instructions Array

**Options:**

- **A.** Allow — structurally valid in the schema
- **B.** Reject — a unit with no instructions is not useful; return a validation error

**Recommendation:** B — require at least 1 instruction with a clear error message.

> **Answer:** \_\_\_ B

---

### CU8 — Max Instructions Per Unit

**Options:**

- **A.** No cap
- **B.** Soft cap at 20 — return a validation error above this

**Recommendation:** B — 20 is generous for any real drill; guards against LLM runaway.

> **Answer:** \_\_\_ Cap at 10

---

### CU9 — RPC Error Mapping

How should DB errors surface as tool errors?

**Options:**

- **A.** Pass raw Postgres error messages through
- **B.** Map common errors to clean messages (e.g. FK violation → "Unknown club code: X")
- **C.** Map common errors + include structured `data` (e.g. `{ valid_codes: [...] }`)

**Recommendation:** C — structured errors let the LLM retry with corrected inputs without asking the user.

> **Answer:** \_\_\_ C

---

### CU10 — Idempotency on Retry ✅ Resolved

> **Answer:** Accept duplicates in v1. The Worker generates a new UUID per call, so retries create separate units. Document this in the MCP README.

---

## `create_session`

### CS1 — Required vs Optional Input Fields ✅ Resolved

> **Answer:** Required: `name`, `items` (at least 1 item). Optional: `notes`. Matches DB constraints (`name` NOT NULL, items need `practice_unit_id` + `repeat_count`).

---

### CS2 — Item Element Schema ✅ Resolved

> **Answer:** `{ practice_unit_id: string (UUID), order: number (>0, unique within session), repeat_count: number (>0), club_reference?: string, notes?: string, focus_cue?: string }`. Matches the `save_practice_session` RPC JSONB shape.

---

### CS3 — Unit Ownership Pre-check

RLS will reject items referencing another user's unit with a generic "policy violation". Should the tool pre-validate?

**Options:**

- **A.** Rely on RLS — generic error message, no extra query
- **B.** Pre-fetch the user's unit ids and validate before calling the RPC — returns "unit \<id\> not found or not yours"

**Recommendation:** B — cleaner error message helps the LLM understand what went wrong and retry correctly.

> **Answer:** \_\_\_ B

---

### CS4 — Club Code Validation on Items

Same question as CU5 — validate item-level `club_reference` against the full catalog or the user's enabled clubs?

**Options:**

- **A.** Full catalog (matches FK behaviour)
- **B.** User's enabled clubs (stricter)

**Recommendation:** A — consistent with CU5.

> **Answer:** \_\_\_ A

---

### CS5 — Empty Items Array

**Options:**

- **A.** Allow — structurally valid in the schema
- **B.** Reject — require at least 1 item

**Recommendation:** B — consistent with CU7.

> **Answer:** \_\_\_ B

---

### CS6 — Return Value ✅ Resolved

> **Answer:** `{ session_id: string }` only. Consistent with CU4 and flag F3.

---

### CS7 — Error Mapping ✅ Resolved

> **Answer:** Same approach as CU9 — map common DB errors to clean messages with structured `data` where helpful (e.g. `{ invalid_unit_ids: [...] }`, `{ valid_codes: [...] }`).

---

## Cross-cutting

### X1 — Tool Descriptions (LLM-facing UX)

RWK-31 notes descriptions are "part of the UX." Who writes and how are they validated?

**Options:**

- **A.** Logan writes all five; no separate review
- **B.** Logan writes first drafts; validated by a test conversation in MCP Inspector (does the LLM pick the right tool and populate args correctly?)

**Recommendation:** B — treat descriptions as first-class deliverables. One Inspector run per tool before finalising.

> **Answer:** \_\_\_ B

---

### X2 — Error Response Shape

**Options:**

- **A.** MCP `isError: true` on the tool result content block, plain message string only
- **B.** Structured: `{ code: string, message: string, data?: { field?: string, valid_codes?: string[] } }`

**Recommendation:** B — structured errors let the LLM retry with corrected inputs automatically.

> **Answer:** \_\_\_ B

---

### X3 — `user_preferences` Exposure ✅ Resolved

> **Answer:** Not exposed in Stage 3. The Stage 4 prompt will ask the user for their distance unit (yards/meters) at conversation start. Document this deferral explicitly.

---

### X4 — Result Size / Truncation ✅ Resolved

> **Answer:** DO NOT cap `list_units` and `list_sessions` (U5, S5). No truncation on `get_user_clubs` — bag size is inherently small.

---

### X5 — Write Tool Idempotency ✅ Resolved

> **Answer:** Accept duplicates on retry in v1 (CU10). Document the behaviour in the MCP README. No idempotency key mechanism for v1.

---

### X6 — Testing Strategy

**Options:**

- **A.** Manual MCP Inspector gate only
- **B.** Vitest unit tests for validation logic + manual Inspector gate

**Recommendation:** B — unit tests for club code validation, instruction schema checks, and error mapping. MCP Inspector for the end-to-end gate ("done when all five tools work with a real account").

> **Answer:** \_\_\_ B

---

### X7 — `get_unit` / `get_session` Detail Tool Gap ✅ Resolved by U1

> **Answer:** Resolved by U1. If full instructions are included in `list_units`, a separate `get_unit` tool is not needed in v1.

---

### X8 — JSON Key Naming ✅ Resolved

> **Answer:** snake_case throughout all tool input and output. Matches PostgREST wire format.

---

### X9 — `order` vs `sort_order` Key ✅ Resolved

> **Answer:** Use `order` in tool input/output. Matches the RPC's JSONB key. The tool does not expose `sort_order` as a separate field.

---

### X10 — Timestamp Fields ✅ Resolved

> **Answer:** Omit `created_at` and `updated_at` from tool output. The LLM rarely needs timestamps for practice planning.

---

### X11 — Record Stage 3 Contracts ✅ Resolved

> **Answer:** Once all questions above are answered, record the final tool schemas (input/output shapes, validation rules, error format) in `design-docs/RWK4-ai-integration/stage3/contracts.md`. Stages 4 and 5 reference this file.
