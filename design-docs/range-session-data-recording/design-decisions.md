# Range Session Data Recording — Design Decisions

**Date:** 2026-07-09
**Method:** grilling session (question-by-question interview) with inline domain modeling
**Status:** **shipped 2026-07-13.** All seven stages implemented and merged to `main` (see
`epic-implementation-plan.md` and each `implementation/stage-NN-*/changes.md`); builds green
across `:shared`, `:androidApp`, and `@rangework/mcp`. What shipped matches this doc; deviations
are recorded per stage (notably Stage 4 prose auto-saves rather than using Save buttons, and
Stage 5's ball-edit entry moved to a "Recorded balls" card). Deferred items (§6) tracked as
follow-up issues. Remaining owner-side gate: field test #2 (a real range session with
observations enabled on 1–2 blocks) — an ergonomics check, not a code gate.
**Vocabulary:** all terms below are defined canonically in [`apps/mobile/CONTEXT.md`](../../apps/mobile/CONTEXT.md) (Success Criterion, Observation Type, Observation, Block Result, Session Note). This doc records the decisions and their reasoning; the glossary records the language.

## 1. Problem

A Range Session records only Completed Steps, Club Overrides, and Time Entries — no outcome or quality data. The execution UX review (`design-docs/range-execution-ux-review/findings-and-direction.md` §4) anticipated per-ball capture and concluded the schema was capture-ready but the UX should wait. This design defines what gets recorded, at what granularity, and who consumes it. It also supersedes the 2026-07-09 decision to defer MCP execution-history tools — the shot-quality data that decision was waiting on is exactly what this design adds.

## 2. The three capture layers

| Layer | What | When | Mandatory? |
| --- | --- | --- | --- |
| **Session Note** | one free-text reflection per Range Session | at finish (or later) | never |
| **Block Result** | free-text note + (criterion units only) success count | passive affordance on every Block | never — never even prompted |
| **Observations** | per-ball values for enabled Observation Types | each ball, during the block | opt-in per Session Item; skippable per ball |

Rejected alternative: a per-item "capture level" setting that makes block results prompted/mandatory at block end. **Deferred, not dead** — the planning-time toggle (built for Observations) can grow a "prompt for block result" value later; v1 block results are passive-only.

## 3. Success Criterion

- Optional **text** on the Practice Unit (e.g. "inside 5m of the 60m flag"). Never machine-interpreted — it is a rubric read by the player (and the coaching model), not parsed by code.
- **Unit-level only; no Session Item override.** This deliberately breaks the app's usual unit-default + item-override pattern (Focus Cue, Club). Reasoning: comparability across sessions of the same unit is the entire point of the count; a per-session criterion reproduces the disease (numbers whose meaning drifts) that motivated having a criterion at all. A criterion change is a **new baseline**, and deserves the weight of editing the drill. An item override remains possible later as an additive change.
- Snapshot bakes in the criterion in force at session start; editing the unit never reinterprets history.
- A **bare X-of-Y count with no criterion is disallowed** — a number whose success rule lives only in the user's head is not trend data. Criterion-less units get note-only Block Results.

## 4. Block Result count — one source, never two

- Item did **not** enable the Success Observation Type → count is **manual**, "X of the block's Y balls".
- Item **did** enable it → count is **derived read-only** from Hit/Miss taps, denominated in **observed balls** ("11 of 18 observed", 20 hit) and labeled as such in history. Manual entry disabled — provenance of every number must be unambiguous.
- The note is independent of all of this and always available.

## 5. Observations

**The law:** *observations record what happened; only Success records whether it was good.* Multi-value types (Shape, Contact, …) are judgement-free distributions — goal-neutral, which is what keeps them comparable while the player's goals shift. Explicitly rejected: structured criteria bound to observation-value subsets ("success = Shape ∈ {fade}"). Possible later; v1 keeps criterion-as-text and the orthogonality law.

**Enablement:** chosen per Session Item when building a Practice Session (zero to many types); baked into the Snapshot. The Practice Unit carries no default.

**Capture gesture:** one chip row / grid per enabled type; tapping fills the ball's record; **auto-commit when every enabled type has a value**; a plain **+1 always present** and commits a partial or empty record. Absent means *unobserved*, never a default (same philosophy as Zero vs Uncounted Ball Count). Recording is never a precondition for completing a Step. Consequence accepted: per-type denominators (shape observed on 18/20 balls, strike on 20/20).

**Corrections:** while the session is Active, any completed ball's Observations can be edited (tap the ball in the block's step list; undo-last is a special case). Freeze when the session leaves Active. Correcting an Observation never un-completes the Step or touches its timestamp.

**Mutability summary (all layers):** prose is reflection, counts are data. Session Note and Block Result notes stay editable after Completion; manual counts and Observations freeze at Completion. Abandoned sessions keep what was recorded, take no further edits, full stop.

**Action Steps never carry Observations** — no ball, nothing to observe. Action-only blocks still take Block Result notes.

### v1 Observation Type catalog

| Type | Values | Input surface | Notes |
| --- | --- | --- | --- |
| **Success** | Hit / Miss | chips | only offerable when the unit has a Success Criterion; renamed from "Criterion" (not user-friendly); the only type that records goodness |
| **Strike Location** | 9 face zones (heel/center/toe × high/middle/low) | 3×3 grid dialog | handedness-aware rendering |
| **Contact** | Very Fat / Fat / Flush / Thin / Very Thin | chips | ordered scale; deliberately split from Strike Location — face position and turf contact are different observables |
| **Shape** | 9-flight matrix (start line × curvature) | 3×3 grid | chose full matrix over curvature-only chips; handedness-canonical storage |
| **Distance** | Way Short / Short / On / Long / Way Long | chips | ordered scale, relative to target depth |
| **Direction** | Way Left / Left / On Line / Right / Way Right | chips | ordered scale; named Direction, not "Dispersion" — a single ball has a miss direction; dispersion is the emergent spread of a block's Direction observations |

Cut from v1: **Rating** (pure/okay/poor) — vaguest of the candidates; vague data invites vague tapping. Value sets are fixed app vocabulary — user-defined per-ball vocabularies would destroy the cross-session comparability this feature exists for. "Measurement" is reserved as a term for possible future instrument-captured (launch monitor) data.

**Handedness** joins User Preferences (right-/left-handed) to orient perspective-dependent surfaces (strike grid, shape grid, direction chips). Stored values are canonical; rendering flips.

## 6. Consumers

In scope for v1:

1. **In-session tallies, minimal form.** Rule: **the tally surface is the input surface** — counts render on the chips / grid cells that already exist (the strike grid becomes a live heatmap), plus one derived Success line on the block. No charts, no new screens, no progress-toward-target framing or judgement colors. Rationale: the review's founding complaint ("the tap earns nothing") applies to dumb capture chips too; and mid-block adjustment is the point of observing. Types whose input surface can't carry counts gracefully simply don't tally — display is presentation, not a domain promise.
2. **History detail** — completed Range Session detail shows session note, block results, per-block observation summaries.
3. **MCP (coaching context)** — in scope from day one (supersedes the deferral decision):
   - **Read:** `list_range_sessions` (thin summaries: date, source session name, ball count, session note, which blocks have results; **Completed sessions only** — Abandoned invisible to the coach) + `get_range_session` (one session's block-level detail: notes, counts, observation aggregates, **and raw per-ball observations**). Raw per-ball exposure was chosen by Logan over the aggregates-only recommendation.
   - **Write (full authoring parity):** `create_unit` gains optional `success_criterion`; `create_session` items gain optional `observation_types` (`success` valid only when the unit has a criterion, mirroring the app rule). Coaching guide gains: when to enable what; **restraint** (roughly ≤1–2 observed blocks per session, unless asked); and the coach must **announce** what it enabled and why.

Deferred: **cross-session trends UI** (a whole analytics surface; design it against real captured data later), prompted block results, first-half/second-half within-block splits, item-level criterion overrides, bound/structured criteria.

## 7. Decision trail (for future archaeology)

1. Granularity → all three layers, ball-level opt-in per Session Item (Logan's call, revising the review's "defer per-ball entirely").
2. Block results → passive affordance (option b), prompted variant deferred.
3. *(Reopened)* bare success counts rejected → Success Criterion introduced, unit-level, no override.
4. Prompted block-result escalation → deferred.
5. Per-ball payload → per-item chosen types, multiple at once (Logan's driver work needs Shape + Strike Location together), fixed vocabulary.
6. Multi-type commit → partial records legal, auto-commit + always-present +1.
7. Naming → Observation family ("dimension"/"metric"/"measure" rejected; observation matches coaching language and is honest about player-judged epistemology).
8. Count provenance → derived wins, manual only without Success observations; per-denominator labeling.
9. Orthogonality law → observations judgement-free; only Success records goodness; bound criteria rejected for v1.
10. Consumers → history + MCP primary; in-session tallies accepted in minimal form; trends deferred.
11. Corrections → edit any ball while Active, freeze at completion.
12. Post-completion → prose editable, counts frozen; Abandoned sessions locked.
13. Catalog → six types as tabled above; Criterion→Success rename; Dispersion→Direction rename; Rating cut.
14. MCP → read tools with raw per-ball data, Completed only; full authoring parity with restraint guidance.

ADR considered for the no-override decision and declined — this doc is its record.

## 8. Open items for implementation planning

- Schema: where Observations live (the review anticipated a `ball_events`-style table hanging off step identities in the snapshot), Block Result / Session Note storage, `success_criterion` column on units, `observation_types` on session items, handedness on preferences.
- Old-shape snapshots (pre ball-granular expansion) — observations are meaningless there; feature applies to new snapshots only.
- Exact canonical encoding for handedness-sensitive values (Shape, Direction, Strike Location).
- Block screen UI: chip/grid layout with tallies, per-ball edit sheet, +1 with partial records.
- MCP tool schemas and coaching-guide wording.
- No dedicated regression/analytics queries needed for v1.
