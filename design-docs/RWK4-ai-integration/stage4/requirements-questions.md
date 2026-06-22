# Stage 4 — Requirements Questions

**Ticket:** RWK-32 (Practice Plan MCP Prompt) · **Depends on:** RWK-31 (tools must exist)

Registers the `build_practice_plan` MCP prompt encoding golf coaching methodology. The prompt **content** — not the registration mechanism — is the main deliverable. Questions below must be answered before the implementation plan can be written. Auto-resolved questions have a pre-filled answer.

---

## Methodology Content

### M1 — Source of the Coaching Methodology

The repo has no coaching methodology document. What is the source for the prompt content?

**Options:**

- **A.** Author from scratch as part of RWK-32, with Logan as the subject-matter expert
- **B.** Transcribe from an existing external document (a book, pro programme, or Logan's offline notes)
- **C.** Consult an external PGA coach for a review pass before finalising

**Recommendation:** A — Logan is the primary SME. Budget authoring time accordingly; this is the gating deliverable, not the code.

> **Answer:** \_\_\_ A

---

### M2 — SME and Review Owner

Who approves the methodology content before the prompt is considered done?

**Options:**

- **A.** Logan self-reviews against the two Jira test personas
- **B.** Logan reviews + validates with an actual Claude.ai conversation run
- **C.** External coaching reference or peer review

**Recommendation:** B — self-review is necessary; a real conversation run is the validation signal.

> **Answer:** \_\_\_ B

---

### M3 — Handicap Range to Cover

What range should the v1 prompt serve well?

**Options:**

- **A.** 5–18 (brand strategy primary audience: "Intentional Practiser")
- **B.** 10–35 (UI-redesign persona context)
- **C.** All handicaps

**Recommendation:** A — focused on the primary Rangework audience. State explicitly that the prompt is not tuned for scratch or plus-handicap players in v1.

> **Answer:** \_\_\_ C. It should adapt using the LLM based on the users handicap they tell it.

---

### M4 — Miss Pattern Input

How should the prompt handle miss patterns?

**Options:**

- **A.** Fixed taxonomy: slice / hook / fat / thin / push / pull / block — user picks from a list
- **B.** Free-text — accept any description and have the LLM interpret it

**Recommendation:** B — more natural in conversation. The LLM can map free-text to drill suggestions without requiring the user to know a taxonomy.

> **Answer:** \_\_\_ B

---

### M5 — `get_user_clubs` in the Plan Flow

**Options:**

- **A.** Prompt always instructs the LLM to call `get_user_clubs` before recommending clubs — step 1 of the runbook
- **B.** Optional — only call it if the user mentions clubs

**Recommendation:** A — calling it up front prevents club-code validation errors in `create_unit`/`create_session`. Make it a required early step.

> **Answer:** \_\_\_ A

---

### M6 — Ball Budget Heuristics

The ticket asks for "ball allocation principles" but gives no numbers. What defaults should the methodology encode?

**Options:**

- **A.** Small ~30 / Medium ~60 / Large ~100+ with a fixed split (warm-up 15%, focus 60%, finishing 25%)
- **B.** Ask the user for their budget; let the LLM allocate freely without hardcoded ratios
- **C.** Encode default ratios as a starting point the LLM adapts based on stated goals

**Recommendation:** C — give the LLM starting heuristics it can override. Encode a small table in the prompt text.

> **Answer:** \_\_\_ C

---

### M7 — Full-Swing / Short-Game / Putting Balance

**Options:**

- **A.** Encode fixed default ratios (e.g. range session: 70% full-swing / 25% short-game / 5% putting)
- **B.** No ratios — let the LLM decide based on stated focus
- **C.** Ratios per session type (range vs short-game facility) that the LLM adapts to the user's stated goals

**Recommendation:** C — ratios give the LLM a starting point; per-session-type splits handle "no short-game facility" gracefully.

> **Answer:** \_\_\_ C

---

### M8 — Rest / Recovery Guidance

The schema has no rest field on instructions or items. How should rest be handled?

**Options:**

- **A.** Omit — no rest guidance since there is nowhere structured to put it
- **B.** Encode rest verbally in unit `notes` or instruction `text` (e.g. "Rest 2 min between sets")

**Recommendation:** B — lightweight rest guidance in `notes` is useful in the app even without a dedicated field.

> **Answer:** \_\_\_ A

---

### M9 — Drill Archetypes

**Options:**

- **A.** Named archetypes inline in the prompt (e.g. "Wedge distance ladder", "Gate putting drill", "Driver tempo ladder") — consistent, testable output
- **B.** Principles-only — let the LLM invent drills from guidelines
- **C.** Named archetypes in a separate `get_drill_archetypes` tool (expands Stage 3 scope)

**Recommendation:** A — inline archetypes produce consistent output and reuse the vocabulary already in `business-docs/`. Start with the drill names that already appear there.

> **Answer:** \_\_\_ B, we want to leverage the intelligence of the LLM to do research and use it's own knowledge

---

### M10 — Distance Units Handling (F9)

No tool exposes `user_preferences` (yards vs meters) in v1.

**Options:**

- **A.** Prompt explicitly asks the user for their distance unit early in the conversation
- **B.** Prompt avoids all distance values — let the user fill them in the app later

**Recommendation:** A — avoids mixed yards/meters in generated instruction text, which is a usability bug.

> **Answer:** \_\_\_ A

---

### M11 — Tech / Facility Adaptation

The ticket lists "launch monitor availability" and "short game facility vs range" as info to gather. How should these change the generated plan?

**Options:**

- **A.** Define explicit rules in the prompt (e.g. launch monitor → add carry-number targets; no short-game area → drop wedge/putting drills and rebalance)
- **B.** Ask the LLM to "adapt based on available resources" without explicit rules

**Recommendation:** A — explicit rules produce consistent, testable behaviour. Document at least 3–4 named adaptations in the prompt.

> **Answer:** \_\_\_ B, this could determine what data the user has available to validate their drills against

---

## Prompt Structure & MCP Mechanics

### P1 — Prompt Arguments: Conversational vs Parameterised

MCP `Prompt` objects support named `arguments`. Should `build_practice_plan` take any?

**Options:**

- **A.** Zero arguments — fully conversational; gathers all info in dialogue
- **B.** One optional `focus` argument (e.g. "driver distance") — rest conversational
- **C.** Multiple arguments: `handicap`, `focus`, `ball_budget`, `time_available`

**Recommendation:** B — a single optional arg makes the prompt clickable from Claude.ai's prompt UI without requiring a form. Everything else is conversational.

> **Answer:** \_\_\_ B

---

### P2 — Single vs Multiple Prompts ✅ Resolved

> **Answer:** One prompt: `build_practice_plan`. The ticket says one; keep scope tight. Specialist prompts (short-game session, pre-round warmup) can be added in future stages.

---

### P3 — Tool-Call Sequencing Instructions

How explicitly should the prompt tell the LLM the order to call tools?

**Options:**

- **A.** Numbered runbook: (1) gather info conversationally → (2) `get_user_clubs` → (3) optionally `list_units` → (4) `create_unit`(s), capture returned UUIDs → (5) `create_session` referencing those UUIDs
- **B.** Describe capabilities and let the LLM sequence itself

**Recommendation:** A — the RLS + FK constraints mean units **must** be created before sessions and their UUIDs used. A runbook prevents hard-to-debug tool-call failures.

> **Answer:** \_\_\_ A

---

### P4 — Confirm Before Creating vs Proactive Create

**Options:**

- **A.** Proactive — create units/session directly after gathering info
- **B.** Confirm — summarise the proposed plan and wait for user approval before writing
- **C.** Hybrid — create units proactively, confirm the session before `create_session`

**Recommendation:** B — a confirm step reduces junk data in the user's account during testing and is better UX. Compatible with the ticket's "create directly" instruction.

> **Answer:** \_\_\_ B

---

### P5 — Prompt Length / Token Budget

**Options:**

- **A.** No limit — include the full methodology
- **B.** Target under 2000 tokens — forces prioritisation of content
- **C.** Target under 1000 tokens

**Recommendation:** B — 2000 tokens covers all methodology sections; beyond that, context window competition becomes a concern.

> **Answer:** \_\_\_ B

---

### P6 — Prompt Message Structure ✅ Resolved

> **Answer:** Single `user` role message containing the full methodology. Most portable across clients (F6); avoids primed-exchange compatibility issues.

---

### P7 — Prompt Name and Description Copy

**Options:**

- **A.** `build_practice_plan` — matches ticket and roadmap
- **B.** `create_practice_plan` — more action-oriented
- **C.** `plan_practice_session`

**Recommendation:** A — matches the ticket spec and is the established name. Write the client-facing `description` to match the app's voice ("Practice with purpose").

> **Answer (confirm name + write description copy):** \_\_\_ A

---

## Cross-client Fallback (F6)

### F1 — Which Clients Support MCP Prompts Today?

**Options:**

- **A.** Test prompt support on Claude.ai and ChatGPT web before deciding on the fallback strategy
- **B.** Assume support is partial; always ship the `get_coaching_guide` fallback tool regardless
- **C.** Ship prompt only; add a fallback if testing reveals gaps

**Recommendation:** A — test first. The result directly decides whether the fallback is primary or secondary.

> **Answer:** \_\_\_ B

---

### F2 — `get_coaching_guide` Fallback Tool Shape

**Options:**

- **A.** Returns the full methodology text as a markdown string — DRY, shared with the prompt
- **B.** Returns a condensed reference guide
- **C.** Returns structured JSON the LLM can parse

**Recommendation:** A — share the same methodology text between the prompt and the tool. Avoids maintaining two copies.

> **Answer:** \_\_\_ A

---

### F3 — Fallback Tool Write-Tool Instructions

**Options:**

- **A.** Fallback returns methodology only — no explicit tool-call instructions
- **B.** Fallback includes the same "call `create_unit`/`create_session`" instructions as the prompt

**Recommendation:** B — a fallback that doesn't end in tool calls is just a text dump. It must drive writes to fulfil Stage 4's done criteria.

> **Answer:** \_\_\_ B

---

### F4 — Runtime Detection of Prompt Support ✅ Resolved

> **Answer:** Ship both prompt and fallback tool unconditionally. No runtime detection for v1 — clients use what they support, and the overlap is acceptable.

---

## Testing & Personas

### T1 — Persona List

The ticket names two. Should Stage 4 testing cover more?

**Options:**

- **A.** The two from the ticket: "beginner with a slice" + "single-digit working on wedges"
- **B.** Expand to 3–4: add a mid-handicapper (15–18) + "coached player following a pro programme"
- **C.** Cover all brand-strategy segments

**Recommendation:** A for v1 — two personas validate the methodology range without blowing the testing budget. Document them as scripted inputs with expected output assertions.

> **Answer:** \_\_\_ A

---

### T2 — Success Criteria for Tool Calls

**Options:**

- **A.** Every test run must end in real `create_unit` + `create_session` calls
- **B.** Majority of runs (e.g. 2 out of 3) per persona
- **C.** At least one successful end-to-end run per persona

**Recommendation:** C — LLM tool calls are non-deterministic; one clean run per persona with valid created data is sufficient.

> **Answer:** \_\_\_ C

---

### T3 — Test Methodology ✅ Resolved

> **Answer:** Manual conversation runs in Claude.ai / MCP Inspector are the gate. No automated Stage 4 tests. Record transcripts and verify created data in the Android app.

---

### T4 — "Coherent Conversation" Definition

What observable properties make a conversation pass?

**Options:**

- **A.** Informal — Logan's judgement call
- **B.** Checklist: gathers info from the ticket's list, proposes drills matched to stated miss pattern, stays within stated ball budget, ends in create tool calls with valid data

**Recommendation:** B — write the checklist before testing. It becomes the pass/fail criteria.

> **Answer (write the checklist here or in a separate file):** \_\_\_ A

---

### T5 — Test Data Cleanup

**Options:**

- **A.** Manual delete of test units/sessions in the Android app after each run
- **B.** SQL cleanup script against the test account
- **C.** Use a dedicated throwaway Supabase account for Stage 4 testing

**Recommendation:** C — avoids polluting Logan's real account. The test account can be reset between runs.

> **Answer:** \_\_\_ A

---

## Cross-cutting

### CC1 — Localization ✅ Resolved

> **Answer:** English-only for v1. Note this explicitly in the prompt file or its surrounding comment.

---

### CC2 — Prompt Text Location

**Options:**

- **A.** Inline string in the `apps/mcp` prompt registration file
- **B.** Separate markdown file imported at build — editable without touching code
- **C.** Database-backed — updatable without a Worker redeploy

**Recommendation:** B — a markdown file can be shared between the prompt and the `get_coaching_guide` fallback tool (single source of truth). Easy to edit and diff in git.

> **Answer:** \_\_\_ The markdown file should exist in the codebase, and on deploy should be pushed to a cloudflare R2 Object Storage bucket. The worker should pull it from there.

---

### CC3 — Methodology Versioning ✅ Resolved

> **Answer:** Add a `methodology_version` string (e.g. `"1.0.0"`) in the prompt text and returned by `get_coaching_guide`. Bump on changes. No semver tooling needed.

---

### CC4 — Review / Approval Before Shipping

**Options:**

- **A.** Logan self-reviews + persona test runs pass (T2, T4)
- **B.** Logan reviews + external coaching reference check
- **C.** Beta behind a flag; gather real user feedback before GA

**Recommendation:** A — persona test runs are the gate. Note that coaching quality is subjective and this deliverable may need iteration post-ship.

> **Answer:** \_\_\_ A

---

### CC5 — Dependency on RWK-31 Tool Contracts

**Options:**

- **A.** Block Stage 4 on finalised RWK-31 contracts — the prompt's runbook explicitly encodes what to do with `create_unit`'s return value
- **B.** Proceed against roadmap §3 shapes; adapt if RWK-31 changes them

**Recommendation:** A — the prompt text references specific tool return shapes (UUID from F3, club codes from F4); a shape change requires a prompt rewrite.

> **Answer:** \_\_\_ A
