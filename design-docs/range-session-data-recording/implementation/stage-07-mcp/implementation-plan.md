# Stage 7: MCP — Range Session Reads + Authoring Parity

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) §6.3
**Status:** draft — awaiting owner review of D1 (`get_range_session` response schema) and D2
(coaching-guide additions); everything else is mechanical

## Objective

Give the coaching model both halves of the data-recording loop:

- **Read:** `list_range_sessions` (thin summaries, Completed sessions only) and
  `get_range_session` (one session's block-level detail: notes, counts, observation
  aggregates, and raw per-ball observations).
- **Write parity:** `create_unit` gains optional `success_criterion`; `create_session` items
  gain optional `observation_types` with the success-requires-criterion rule mirrored.
- **Coaching guide:** when to enable what, the restraint rule (≤1–2 observed blocks per
  session unless asked), and the announce-what-you-enabled rule.

Per the epic, the read tools must return **empty-but-valid** results until app capture
(Stage 4/5) ships — completed sessions already exist from the execution feature; they simply
carry no notes, results, or observations yet. That interim state is designed, not a bug.

## Dependencies

- **Stage 1 plan (approved)** — this plan builds against the approved schema: `block_results`
  JSONB keyed by unitIndex, `range_session_observations` table keyed by
  `(range_session_id, step_index)`, snapshot v3 unit keys `successCriterion` /
  `observationTypes`, the six type identifiers, and the extended `save_practice_unit` /
  `save_practice_session` RPCs.
- **Stage 1 merge** gates *validation only* (the tools need the migrated stack to run
  against). Implementation can proceed in parallel on its own branch — zero file overlap
  with any Android stage.
- **No dependency on Stage 2.** The canonical handedness encoding decides what value strings
  go *into* `observed_values`; this stage treats them as opaque strings on the way out
  (aggregates are value→count maps, raw entries pass values through verbatim). Nothing here
  hardcodes value vocabulary, so the Stage 2 decision can land before or after this stage.
- Existing MCP infrastructure: `UserContext`, `toolError`/`ErrorCodes`, per-tool test files,
  R2-backed coaching guide.

## D1 — `get_range_session` response schema (owner review)

The one shape worth a look: raw per-ball exposure was Logan's call over aggregates-only, and
this schema is what the coaching model pays tokens for on every deep dive.

### Proposed response

```json
{
  "id": "…",
  "session_name": "Wedge Wednesday",
  "started_at": "2026-07-12T09:14:00Z",
  "completed_at": "2026-07-12T10:02:00Z",
  "session_note": "Struck it great until the last block",
  "planning_notes": "Tournament prep",
  "blocks": [
    {
      "block_index": 0,
      "unit_title": "60m wedge ladder",
      "focus_cue": "Hinge earlier",
      "success_criterion": "inside 5m of the 60m flag",
      "observation_types": ["success", "shape"],
      "balls_planned": 20,
      "balls_hit": 20,
      "result": { "note": "left misses all day", "manual_count": null },
      "observations": {
        "success": { "observed": 18, "counts": { "hit": 11, "miss": 7 } },
        "shape": { "observed": 15, "counts": { "straight": 9, "fade": 4, "pull": 2 } }
      },
      "balls": [
        { "step_index": 12, "values": { "success": "hit", "shape": "straight" } },
        { "step_index": 13, "values": { "success": "miss" } }
      ]
    }
  ]
}
```

Shape decisions inside this, and why:

- **Block-shaped, one entry per snapshot unit** (`block_index` = `unitIndex`, the block
  identity from `ExecutionBlocks.kt` and the `block_results` key). Planning context
  (`unit_title`, `focus_cue`, `success_criterion`, `observation_types`) comes from the
  snapshot unit entry — the criterion **in force at session start**, which is exactly what
  makes the counts interpretable.
- **Aggregates always present, raw always present.** Aggregates alone were already rejected
  (design decision 14); raw alone would force the model to re-tally counts itself — slow and
  error-prone. Both together cost little: aggregates are a few dozen tokens per block.
  `observations` keys only enabled types that have ≥1 observation; each carries the per-type
  `observed` denominator (denominator honesty, design §5).
- **No hardcoded value semantics.** The `success` aggregate *is* the derived count — the
  model reads `{"hit": 11, "observed": 18}` without the tool knowing "hit" means good. Same
  for every other type. Provenance stays unambiguous structurally: a manual count only ever
  appears in `result.manual_count`; a derived count only ever appears in
  `observations.success` — they cannot coexist (the app/RPC rule), and the tool description
  says so.
- **Raw `balls` lists only balls with ≥1 observed value**, ordered by `step_index`. The DB's
  "no row and empty row both mean unobserved" semantics carry through — empty records are
  omitted. No timestamps on raw entries (step order already gives within-block sequence;
  timestamps would roughly double the per-ball token cost for marginal coaching value).
- **Token cost:** worst realistic case — 100 observed balls × 3 types ≈ 100 entries ×
  ~35 tokens ≈ 3.5k tokens, plus aggregates. Acceptable for a deliberate single-session
  drill-down; the list tool stays thin so this price is only paid on demand.
  **Alternative considered and rejected:** an `include_raw` flag defaulting to false — saves
  tokens only when the model asks for detail it then doesn't use, and costs an extra
  round-trip in the common case. Revisit if real conversations show pain.
- **Canonical values pass through verbatim** (handedness-canonical storage, Stage 2). The
  tool description notes values are stored from a canonical perspective so the model doesn't
  mirror-flip for left-handed players.
- `balls_hit` = count of completed Ball Steps (snapshot v≥2: steps with `ballCount == 1`);
  `balls_planned` = the block's Ball Step total. For v1 snapshots both are `null` (coarse
  steps; honest degradation).

### `list_range_sessions` summary shape (fixed, listed for completeness)

```json
{
  "sessions": [
    {
      "id": "…",
      "session_name": "Wedge Wednesday",
      "source_session_id": "…",
      "started_at": "…",
      "completed_at": "…",
      "balls_hit": 74,
      "session_note": "…",
      "blocks_with_results": [0, 2],
      "has_observations": true
    }
  ]
}
```

- Completed only: `completed_at is not null and abandoned_at is null` in the query;
  Abandoned sessions are invisible to the coach at the query level, not filtered in JS.
- Ordered `completed_at` desc; optional `limit` (default 20) — computing `balls_hit`
  requires each session's snapshot + `completed_steps`, so bounding the fetch keeps the
  worker's per-request work flat as history grows. The model can raise the limit.
- `blocks_with_results` = the numeric keys of `block_results` (which blocks carry a note or
  count); `has_observations` = one grouped `range_session_id` count over
  `range_session_observations`. Neither requires snapshot interpretation.

## D2 — Coaching-guide additions (owner review of wording)

Guide text drives model behaviour; the restraint and announce rules only exist as wording.
Proposed additions to `apps/mcp/methodology/coaching-guide.md`:

**In "1. Discover" (new bullet under "Gather context just-in-time"):**

> - Past range sessions are data, not just memory. Call `list_range_sessions` to see what the
>   player has actually done; call `get_range_session` on 1–2 recent or relevant sessions
>   before diagnosing. Session notes and block results are the player's own words — quote
>   them back. Observation counts always name their denominator ("11 of 18 observed") —
>   never present an observed-ball rate as a rate of all balls hit.

**In "3. Design principles" (new subsection after "Measurement & feedback"):**

> ### Success criteria and observations
>
> - A **success criterion** is a short text rubric on a unit ("inside 5m of the 60m flag").
>   The player judges it per ball; it is never parsed by code. Set one whenever a drill has a
>   checkable target — it's what makes an X-of-Y count meaningful and comparable across
>   sessions. Changing a criterion later resets the baseline, so word it carefully.
> - **Observation types** are enabled per session item and record what happened per ball —
>   they are judgement-free. Only `success` records whether a ball was good, and `success`
>   can only be enabled on items whose unit has a criterion.
> - **Restraint:** observing every ball is a tax on practice. Enable observations on at most
>   1–2 blocks per session — the ones tied to the player's stated focus — unless the player
>   asks for more. Zero observed blocks is a fine session.
> - **Announce what you enabled and why**, every time ("I've set shape tracking on the
>   draw-shot block so we can see if the start line is improving"). Never enable silently.
> - Pick the type that matches the diagnosis: `success` for outcome vs a criterion,
>   `shape`/`direction` for pattern work, `strike_location`/`contact` for strike issues,
>   `distance` for distance control.

**In "4. Tool runbook":** step 3 gains "call `list_range_sessions` / `get_range_session` to
ground the diagnosis in recent data"; step 6 gains "set `success_criterion` on new units with
a checkable target"; step 7 gains "enable `observation_types` per item, observing the
restraint rule, and announce what you enabled".

**In "Data format rules":**

> - `success_criterion` (optional on `create_unit`): short free text; the player-judged
>   success rubric.
> - `observation_types` (optional on `create_session` items): array drawn from `success`,
>   `strike_location`, `contact`, `shape`, `distance`, `direction`. `success` is valid only
>   when the item's unit has a `success_criterion`. Omit for no per-ball capture (the
>   default). Don't enable types on no-ball (action-only) drills.

## Fixed in this plan (not owner decisions)

- **Tool names** `list_range_sessions` / `get_range_session` — design-fixed; the `range_`
  prefix keeps them distinct from the planning-side `list_sessions`.
- **New error code** `RANGE_SESSION_NOT_FOUND` in `tool-errors.ts`. `get_range_session` on
  an Active or Abandoned session returns it with message "range session not found or not yet
  completed" — invisibility without leaking which non-completed state it's in.
- **Observation vocabulary helper** — the six type ids live once in
  `apps/mcp/src/validation/observation-types.ts` (mirroring `club-codes.ts`), used by
  `create_session` validation and the tool descriptions.
- **`create_session` validation order** mirrors the existing pattern: the unit pre-fetch
  gains `success_criterion`, so success-requires-criterion is pre-validated with a friendly
  field-scoped error (`items[i].observation_types`); the RPC exception remains the backstop
  and maps to the same error. Unknown type ids → `VALIDATION_ERROR` listing the vocabulary;
  duplicates deduped silently (matches the RPC); empty array treated as absent.
- **`create_unit`** passes `p_success_criterion` (named param on the extended RPC); blank or
  whitespace input treated as absent (matches the RPC's `nullif(trim(...))`).
- **No update tools.** Criterion editing ("new baseline") stays an in-app act; there is no
  `update_unit` today and this stage doesn't add one.
- **Deliberate omissions from the read tools:** session duration (separate coupled design,
  ball-weighted crediting — additive later), cross-session trend computation (deferred by
  design §6; the model can trend across `get_range_session` calls itself if asked).

## Likely files

### New

| File | Purpose |
|---|---|
| `apps/mcp/src/tools/list-range-sessions.ts` | Completed-session summaries |
| `apps/mcp/src/tools/get-range-session.ts` | Block detail: results, aggregates, raw balls |
| `apps/mcp/src/validation/observation-types.ts` | Type-id vocabulary + validation helper |
| `apps/mcp/src/tests/list-range-sessions.test.ts` | Per-tool tests (pattern: existing test files) |
| `apps/mcp/src/tests/get-range-session.test.ts` | Incl. v1/v2/v3 snapshot fixtures |
| `apps/mcp/src/tests/observation-types.test.ts` | Vocabulary helper tests |

### Modified

| File | Change |
|---|---|
| `apps/mcp/src/server.ts` | Register the two new tools |
| `apps/mcp/src/tools/create-unit.ts` | `success_criterion` input + RPC param + description |
| `apps/mcp/src/tools/create-session.ts` | Per-item `observation_types` + validation + description |
| `apps/mcp/src/validation/tool-errors.ts` | `RANGE_SESSION_NOT_FOUND` |
| `apps/mcp/src/tests/create-unit.test.ts`, `create-session.test.ts` | New-field cases |
| `apps/mcp/methodology/coaching-guide.md` | D2 additions |
| `apps/mcp/scripts/regression.ts` | Coverage for all four touched tools (see validation) |
| `.agents/instructions/mcp.md` | File map + tools table |
| `apps/mcp/CONTEXT.md` | One "Range Session (coaching view)" language entry: Completed-only visibility, observed-denominator phrasing |

## Read-tool assembly (implementation notes)

`get_range_session`, per request:

1. Fetch the session row (id filter + completed-only predicate) — snapshot,
   `completed_steps`, `block_results`, `session_note`, timestamps.
2. Fetch its `range_session_observations` rows ordered by `step_index` (one query).
3. Map each observation's `step_index` → `unitIndex` via the snapshot steps array; group per
   block; tally aggregates per type from raw values; attach `block_results[unitIndex]`.
4. v1/v2 snapshots: unit entries lack `successCriterion`/`observationTypes` → emit `null` /
   `[]`; `balls_planned`/`balls_hit` null for v1.

Defensive rules: a `block_results` key or observation `step_index` that doesn't resolve
against the snapshot is skipped, never thrown — the tool must not brick on data written by a
future or buggy app version. An observation on a step that resolves to an Action Step is
still reported under its block (opaque pass-through; app-level rules are not re-enforced on
read).

## Edge cases

- Player with zero completed range sessions → `{ "sessions": [] }` (the designed interim
  state until Stage 4 ships; also the new-user state forever).
- Completed session with no notes, no results, no observations → valid summary with nulls,
  `blocks_with_results: []`, `has_observations: false`; detail blocks carry planning context
  only.
- Note-only or count-only block result → the absent half is `null` (both-optional shape from
  Stage 1 D1).
- Same unit in multiple session items → distinct blocks, each with its own types/results
  (snapshot unit entries are per-item).
- `get_range_session` with a well-formed UUID belonging to another user → RLS returns no row
  → `RANGE_SESSION_NOT_FOUND` (indistinguishable from nonexistent, as it should be).
- `create_session` enabling `success` on a unit created earlier in the same conversation →
  works; the pre-fetch reads the just-saved criterion.
- Criterion added via MCP then session created enabling `success` → the exact intended
  authoring flow; validated end-to-end in the checklist.
- `observation_types` on an action-only unit → allowed (DB-harmless, matches Stage 1);
  guide + description tell the model not to.

## Validation checklist

- [ ] `pnpm --filter @rangework/mcp test`, `typecheck`, `lint` all green
- [ ] Unit tests cover: completed-only filtering (Active/Abandoned invisible in list and
      not-found in get); v1/v2/v3 snapshot fixtures; aggregate tallying incl. per-type
      denominators; empty-record omission from `balls`; unresolvable `step_index`/block key
      skipped; `success`-without-criterion rejection; unknown type id rejection; dedupe;
      blank criterion → absent
- [ ] Against the migrated local stack (Stage 1 merged): `create_unit` with
      `success_criterion` → `create_session` enabling `["success","shape"]` on it →
      succeeds; same with a criterion-less unit → friendly rejection
- [ ] Seed a completed session + observation rows via SQL; `get_range_session` returns the
      D1 shape with correct aggregates and raw balls; `list_range_sessions` summary matches
- [ ] Pre-existing completed v1/v2 sessions (from the execution feature) list cleanly with
      null/empty capture fields — the empty-but-valid interim state
- [ ] Coaching guide re-seeded to local R2 (`dev:seed`) and returned by `get_coaching_guide`
      with the new sections
- [ ] Regression script extended with the four touched tools. **Known issue:** the script is
      currently broken (predates this stage, RWK-4). If the cause is script-local, fix it
      here; if it's the consent-page OAuth race, a manual MCP Inspector pass is the accepted
      fallback (Stage 1 precedent) — record which path was taken in `changes.md`
- [ ] `.agents/instructions/mcp.md` tools table updated; deploy step unchanged

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Token bloat from raw per-ball data in real coaching conversations | Medium | Thin list + on-demand detail; only observed balls serialized; no timestamps. If it hurts in practice, `include_raw` is an additive param |
| Value-string semantics hardcoded before Stage 2's encoding lands | Low | Structural rule: tools treat `observed_values` as opaque; aggregates are value→count maps; test fixtures use placeholder strings |
| Restraint/announce rules ignored by coaching models | Medium | Wording is the only lever — hence D2 owner review; observed behaviour feeds guide iteration (same loop as existing guide) |
| `list_range_sessions` cost grows with history (snapshot fetch per session) | Low | Default `limit` 20; single-user product; server-side computation only — response stays thin |
| Existing `create_unit`/`create_session` callers regress | Low | New inputs optional; existing tests must stay green untouched |
