# Range Session Data Recording — Epic Implementation Plan

**Date:** 2026-07-09
**Status:** proposed — awaiting owner sign-off on stage split and decision-point process
**Design:** [`design-decisions.md`](design-decisions.md) (agreed 2026-07-09). Vocabulary: `apps/mobile/CONTEXT.md`.
**Process precedent:** RWK-11 epic (`design-docs/RWK11-session-execution/`) — epic plan, then one
`implementation/stage-NN-*/implementation-plan.md` + `changes.md` per stage.

## Shape of the epic

Two structural calls drive everything below:

1. **All schema lands in one stage, up front.** The feature needs a snapshot format bump
   (criterion + enabled Observation Types baked in). Splitting schema across ships would mean
   two snapshot version bumps and two rounds of old-shape compatibility reasoning. One
   migration stage covering the entire design (even the parts whose UI ships weeks later) keeps
   that cost singular. Unused columns/tables are inert; old-shape snapshots are already
   out of scope per the design (§8).

2. **Prose layer ships before the per-ball layer.** Session Note + Block Results (Stages 3–4)
   are low-risk, independently valuable, and exactly what the execution UX review predicted as
   the first capture feature. Per-ball Observations (Stage 5) are the heavy lift — new
   interaction surfaces on the block screen, tallies, corrections — and the part most likely to
   need iteration after field contact. Shipping prose first means real range sessions start
   producing data while the observation UI is still being built, and the derived-vs-manual
   count rule stays trivially consistent (nothing can enable Success observations yet, so all
   counts are manual — exactly the design's rule).

## Stages

```
Stage 1: Schema & Snapshot v3
    │
    ▼
Stage 2: Shared Models & Data Layer
    │
    ├──────────────────────────────┐
    ▼                              ▼
Stage 3: Authoring & Settings   Stage 7: MCP (read + authoring parity)
    │                              (any time after Stage 1; most useful
    ├──────────────┐                after Stage 4 produces real data)
    ▼              ▼
Stage 4:        Stage 5:
Prose capture   Per-ball Observations   ◄── gated on capture-UI design review
(ship point 1)      │
    │               ▼
    └────────► Stage 6: History detail
                (ship point 2)
```

**Parallelism rules:**

| Can run concurrently | Why it's safe |
| --- | --- |
| Stage 7 (MCP) with any Android stage after Stage 1 | Different app (`apps/mcp` vs `apps/mobile`), zero file overlap |
| Stage 3 (authoring) with Stage 4 planning + capture-UI design prep | Stage 4/5 design work is docs-only |
| Stage 6 (history) with Stage 5, if split: notes/results view after Stage 4, observation summaries appended after Stage 5 | Different screens (history detail vs block screen) |

| Must stay sequential | Why |
| --- | --- |
| 1 → 2 | Models serialize what the migration defines |
| 2 → 3, 2 → 4 | UI stages consume shared use cases |
| 4 → 5 | Both rework the block screen — parallel branches would collide on the same composables; and Stage 5's design review takes Stage 4's field impressions as input |

**Pipelining rule:** stage N+1's *plan* can be drafted as soon as stage N's plan is approved
(decisions fixed) — it doesn't wait for stage N's code to merge. Implementation of N+1 waits
for N's merge where the table above says sequential. In practice this means Logan's review of
a stage plan is the serialization point, and agent implementation time is always overlapped
with planning the next stage.

### Stage 1 — Schema & Snapshot v3

One migration (plus RPC change) covering the whole design:

- `success_criterion` (nullable text) on `practice_units`.
- `observation_types` on `practice_session_items` (array/JSONB of type identifiers).
- `handedness` on user preferences.
- Session Note storage on `range_sessions` (nullable text column).
- Block Result storage — note + manual count per block. Likely JSONB keyed by item index on
  `range_sessions` (mirrors `club_overrides`) — **decide in stage plan**.
- Per-ball Observations storage — the review anticipated a `ball_events`-style structure
  hanging off step identities. Separate table (`range_session_observations`, keyed by
  range_session_id + step index) vs JSONB column — **decide in stage plan**; the MCP raw
  per-ball read and the correction/edit flow both favour a table.
- `start_range_session` bumps `snapshot_version` to 3: bakes in the unit's Success Criterion
  and each item's enabled Observation Types. v1/v2 snapshots untouched; feature detection by
  version.
- Mutability enforcement question: are freeze-at-completion rules app-level only, or backed by
  RLS/trigger? — **decide in stage plan** (recommendation: app-level, consistent with existing
  completed-session handling; DB enforcement is additive later).

Validation: migration runs clean locally; RPC output inspected for a criterion-bearing,
observation-enabled session; old-snapshot sessions still load.

### Stage 2 — Shared Models & Data Layer

- Observation Type model + fixed value vocabularies; **canonical encoding for
  handedness-sensitive values decided here** (Shape, Direction, Strike Location — stored
  canonical, rendered flipped). This is the one encoding that is expensive to change after
  data exists — it gets a short written proposal for owner review, not a silent pick.
- Snapshot v3 models; Block Result, Session Note, Observation models.
- Repository + use cases: record/edit/delete Observation (with the −1 sweep interaction from
  the block redesign — decrement must void the ball's observations), save Block Result, save
  Session Note, derived-count computation ("11 of 18 observed"), manual-count validation
  (criterion required; disabled when Success type enabled).
- Mutability rules as testable logic: Active = editable; Completed = prose editable, counts
  frozen; Abandoned = locked.
- Unit tests are the bulk of this stage — the count-provenance and freeze rules are the
  design's load-bearing invariants.

### Stage 3 — Authoring & Settings UI

- Success Criterion field on the Practice Unit editor (with the "editing = new baseline"
  framing in copy or docs).
- Observation Type picker per Session Item in the session builder — Success offered only when
  the unit has a criterion; zero types is the default and stays frictionless.
- Handedness toggle in Settings/preferences.

### Stage 4 — Prose capture  ⛳ ship point 1

- Block Result affordance on the block screen: passive, never prompted; note always; manual
  X-of-Y only when the unit has a criterion (and, later, no Success observations).
- Session Note capture at finish, editable afterwards from history.
- Post-completion editability for both notes; count freeze.
- After this stage the feature is field-usable: real sessions accumulate notes and counts.

### Stage 5 — Per-ball Observation capture

**Gate: capture-UI design review before implementation** (see decision points below).

- Chip rows / 3×3 grids per enabled type on the block screen; auto-commit when all enabled
  types have values; always-present +1 committing partial/empty records.
- The tally surface is the input surface: live counts on chips/grid cells (strike grid as
  heatmap), one derived Success line on the block. No charts, no judgement colors.
- Per-ball edit sheet (tap a ball in the step list) for corrections while Active; undo-last as
  a special case; freeze on leaving Active.
- Handedness-aware rendering of the three perspective-dependent surfaces.
- This is the largest stage; the stage plan may split it (5a capture gesture + commit model,
  5b tallies + corrections) if it's unwieldy as one PR.

### Stage 6 — History detail  ⛳ ship point 2

- Completed Range Session detail: session note, per-block results (with provenance labeling —
  manual "X of Y balls" vs derived "X of Y observed"), per-block observation summaries.
- Denominator honesty throughout (per-type observed counts).

### Stage 7 — MCP (parallelizable after Stage 1)

- `list_range_sessions` (Completed only, thin summaries) and `get_range_session` (block detail
  + raw per-ball observations). Response schemas proposed in the stage plan for owner review —
  raw per-ball shape affects coaching-model token cost and is worth one look.
- `create_unit` + `success_criterion`; `create_session` items + `observation_types` (Success
  valid only with criterion — mirror the app rule in validation and tool description).
- Coaching guide: when to enable what; restraint (≤1–2 observed blocks per session unless
  asked); announce-what-you-enabled rule.
- Precedent from the execution redesign: single-user product, MCP ships in parallel without
  sequencing risk. Read tools return empty-but-valid results until app capture ships.

## Operating model — how a stage actually runs

Every stage follows the same loop (the RWK-11 pattern, made explicit):

1. **Plan** — Claude drafts `implementation/stage-NN-<name>/implementation-plan.md`: scope,
   file-level changes, edge cases, validation checkpoint, and — where this epic plan flags a
   decision point — a written recommendation with alternatives.
2. **Review** — Logan reviews the plan. For most stages this is an async read focused on the
   flagged decisions; approve, adjust, or bounce. *This is the serialization point* — once
   approved, the next stage's planning can start (pipelining rule above).
3. **Implement** — hand the approved plan to an implementing agent on a feature branch
   (`data-recording-stage-NN` or similar). The plan file is the agent's brief; it should not
   need the whole conversation history. Agent runs the module's test + lint commands
   (`AGENTS.md`) before opening a PR.
4. **Validate** — the stage's validation checkpoint from its plan: migration against local
   Supabase for Stage 1, KMP unit tests for Stage 2, build + manual flow on device for UI
   stages, MCP regression run for Stage 7.
5. **Merge** — PR review (Logan, optionally with `/code-review`), merge to main.
6. **Record** — `changes.md` written in the stage folder: what shipped, deviations from plan,
   anything discovered that affects later stages. Deviations that touch a later stage's
   assumptions get noted in that stage's folder too.

**Division of labour:** agents draft plans, implement, test, and write `changes.md`. Logan
personally: reviews plans at the decision points, attends the one live design review (before
Stage 5), does the two field tests, and merges PRs. Nothing else needs him in the loop.

## Stage-by-stage: what to do from here

**Right now (Stage 1):**
1. Ask Claude to draft `implementation/stage-01-schema/implementation-plan.md`. It must
   include written recommendations for the three Stage-1 decisions (Block Result storage,
   Observations table-vs-JSONB, freeze enforcement level) and the snapshot v3 shape.
2. Review the plan — the three decisions are the only things that genuinely need your
   judgement; the rest is mechanical migration work.
3. On approval, hand to an implementing agent. Meanwhile, kick off the Stage 2 plan draft
   (its model shapes follow directly from the approved Stage 1 decisions) **and** the Stage 7
   plan draft (MCP is unblocked the moment the schema shape is fixed on paper).

**Stage 2 (shared models):**
1. Plan should already be drafting (see above). It must contain the canonical
   handedness-encoding proposal — review that one carefully; it's the decision that's
   expensive to reverse once observation data exists.
2. Implementation starts once Stage 1 is merged. While it runs: draft Stage 3 and Stage 4
   plans (both small), and let the Stage 7 implementation run in parallel on its own branch.

**Stage 3 (authoring UI) → Stage 4 (prose capture):**
1. Sequential implementation, but plan both together — they're small and share context
   (criterion authored in 3 is what legitimizes manual counts in 4).
2. After Stage 4 merges: **field test #1** — take a real session to the range, use the block
   result + session note affordances, write down what felt wrong. That note is a direct
   input to the Stage 5 design review.

**Stage 5 (per-ball capture):**
1. Before any implementation plan: Claude produces a short capture-UI design doc with
   wireframes/mockups (chip rows, 3×3 grids, tally rendering, +1 placement, per-ball edit
   sheet) — informed by field test #1.
2. **Live design review** with Logan — the one synchronous session in the epic.
3. Then the normal loop: implementation plan (possibly split 5a capture / 5b tallies +
   corrections), review, implement, validate on device.
4. After merge: **field test #2** with observations enabled on 1–2 blocks.

**Stage 6 (history detail):**
1. Normal loop; no decisions flagged. Can start its notes/results half after Stage 4 if
   Stage 5 is still in design review — otherwise just run it after Stage 5.

**Stage 7 (MCP):** already planned and possibly implemented by now (it unblocks at Stage 1
plan approval). Its review gate is async: response schema for raw per-ball data +
coaching-guide wording. If the branch merged before app capture shipped, verify the read
tools return empty-but-valid results — that's the designed interim state.

**Epic close:** after field test #2 and Stage 6/7 merge, update `design-decisions.md` status
line (as the execution UX review did), confirm `apps/mobile/CONTEXT.md` vocabulary still
matches what shipped, and file follow-up issues for the deferred items (trends UI, prompted
block results, item-level criterion override) so they're findable without re-reading this doc.

## Decision points (owner involvement)

No further product grilling is needed — the design doc resolves the product questions. Three
targeted check-ins, none full sessions:

| When | What | Format |
| --- | --- | --- |
| Stage 1–2 planning | Observations storage shape; canonical handedness encoding; freeze enforcement level | Written recommendation in the stage plans; async review |
| Before Stage 5 | Block-screen capture UI: chip/grid layout, tally rendering, edit sheet, +1 placement | Short design doc with mockups/wireframes + one live review — this is the founding-complaint surface ("the tap earns nothing" cuts both ways: a clumsy capture surface is worse than none) |
| Stage 7 planning | MCP raw per-ball response schema + coaching-guide wording | Async review of the stage plan |

## Validation gates

- Per-stage: the usual test-first checkpoints in each stage plan (KMP unit tests for Stage 2
  invariants; build + manual flow for UI stages; MCP regression additions for Stage 7).
- **Field test after Stage 4** and **again after Stage 5**: one real range session each. The
  execution redesign was corrected by a field report; capture ergonomics (glove on, phone
  glanced at) cannot be validated at a desk. Stage 5's design review uses Stage 4's field
  impressions as input.
- Deferred-scope tripwire: any temptation to add charts, judgement colors, or
  progress-toward-target framing during Stage 5/6 is out of scope by design — trends UI is
  explicitly deferred until real captured data exists.

## Risks

| Risk | Mitigation |
| --- | --- |
| Snapshot v3 RPC regressions (the RPC is the project's most complex SQL and is being touched a third time) | Stage 1 validates v1/v2/v3 loading paths explicitly; extend existing RPC tests |
| Canonical encoding chosen wrong → stored data needs migration | Owner-reviewed written proposal before any data exists (Stage 2 gate) |
| Capture UI too fiddly for the range → feature unused | Design review gate + field tests; +1-always-present keeps the floor at "counter still works" |
| Count provenance leaks (manual and derived mixing) | Invariants live in shared KMP logic with unit tests, not in UI conditionals |
| Stage 5 scope balloon | Pre-agreed split seam (capture vs tallies/corrections); tallies are presentation, not domain promise |
