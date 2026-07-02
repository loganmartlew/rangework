# Range execution UX review — findings and direction

**Date:** 2026-07-02
**Status:** Design resolved via grilling session 2026-07-02 (see §6); ready to implement.
Glossary updated (`apps/mobile/CONTEXT.md`: Step, Ball Step, Action Step, Block, Club
Resolution, Club Override); ADR 0004 records the expansion decision.
**Trigger:** Field report from a real range session ("Driver Slice Fix + Flag Distances",
MCP-generated) — execution felt "messy and unnatural, so many screens to go through."

## 1. The observed problem

The execution screen ([RangeSessionScreen.kt](../../apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/RangeSessionScreen.kt))
renders one screen per Step, where Steps are flattened as unit × repetition × instruction
(per ADR 0002, in the `start_range_session` RPC). The reviewed session flattened to:

| Item | Instructions × Reps | Steps | Balls |
| --- | --- | --- | --- |
| Warm-up ladder | 2 × 1 | 2 | 10 |
| Half-back, full-through driver | 2 × 15 | 30 | 15 |
| Backswing length ladder | 3 × 8 | 24 | 8 |
| Random flag distances | 2 × 20 | 40 | 20 |

**96 screens for a ~53-ball session** — roughly two "Mark Complete" taps per ball, most of
them near-identical cards. In practice the owner did not tap through: steps were
**batch-completed after the fact**. Hand-authored units (batched `ball_count`, repeat 1)
produce ~2 screens per 20 balls; MCP-authored units (per-ball instructions,
`repeat_count` = balls) produce up to 2 screens per ball.

## 2. Diagnosis: the app model, not the MCP authoring

The generated authoring is *good practice design* — "rehearse, then hit, 15 times" and
"new club + new flag every ball, full recommit" are legitimately per-ball drills, and
`repeat_count = balls` models them correctly. The app's flaw is that its only rendering of
a repetition is a whole new screen requiring a tap. The better and more granular the
practice design, the worse the UX gets.

Underlying issues in the current execution UI:

1. **The tap earns nothing.** Marking a step complete records no result — no outcome, no
   note. Pure bookkeeping friction, per ball, with a glove on. (Batch-completion in the
   field confirms it's ceremony.)
2. **The interaction unit is wrong for the range.** Real practice is block-shaped: settle
   into a drill for 10–15 minutes, glance at the phone for the cue. A screen-per-rep
   wizard assumes phone-in-hand between every swing.
3. **Priority inversion in the card.** The focus cue — the one glanceable thing — sits
   below the instruction, ball/club row, and (on rep 12 of 15) the same long notes
   paragraph shown 12 times ([ExecutionStepCard.kt](../../apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ExecutionStepCard.kt)).

Navigation *into* a session (list → detail → start) is fine; the pain is execution only.

## 3. Direction: block-first execution

Make the **block (session item), not the step, the screen**:

- One screen per unit/block. Focus cue large and glanceable; instruction list shown inline
  as the structure of one pass (not N sequential tasks); notes collapsed after first view.
- Repetition handled by a **counter** (ball tally / "+1 pass"), not screen-per-rep. The
  reviewed session becomes 4 screens with a counter each — matching how the night was
  actually experienced: four blocks.
- Cheaper intermediate step (if the full redesign is deferred): collapse consecutive reps
  of the same instruction into one card with a rep tally ("7 of 15") whose complete button
  increments instead of advancing screens. Cuts the reviewed session from 96 interactions
  to ~9 cards.

## 4. Future per-ball data capture

The step-per-screen model was originally built to accommodate future per-ball recording
(ball flight, feel, club data → progression tracking). Review conclusion: that future is
**better served by the block screen**, not the wizard.

- **Principle: let speculative features shape the schema, not the screens.** Keeping the
  data model capture-ready is nearly free (steps are stable identities in a snapshot; a
  future `ball_events` table hangs off them cleanly). Keeping the *UX* capture-ready is
  what produced the 96-screen session, and bought no head start.
- **The ball counter is the capture point.** Each "+1" is a ball event with a timestamp
  and a step identity. Later the same tap can grow optional payload — outcome chips
  (straight / push / slice, flush / fat / thin), long-press for detail. One gesture, zero
  navigation. Capture must be near-zero-cost or it won't survive contact with the range;
  a mandatory wizard doesn't make capture happen, it makes skipping feel like falling
  behind.
- **Sequencing:** ship block screens first, let usage reveal what's worth capturing.
  Likely first capture feature is **per-block results** ("slice crept back at ¾",
  "12/20 within 5m") — a single optional note/score at block end, which the generated
  sessions' notes already keep asking the player to observe — before any per-ball data.

## 5. Data model assessment

Verdict: **the concepts are right; keep the schema.** Every repetition dimension has a
drill that needs it: multiple instructions per unit (drill structure), `ball_count` per
instruction (batching), `repeat_count` on the session item (cycling ladders; correctly a
per-session decision), per-instruction club with unit default fallback (wedge ladder),
item-level overrides (template reuse — the reviewed session used a short item `focus_cue`
overriding the longer unit focus, which is the intended use).

Changes agreed in principle:

1. **Canonicalization rule, not a schema change.** A single-instruction unit with
   `repeat_count = N` and a unit with `ball_count = N` encode the identical drill but
   render completely differently — one degree of freedom too many, and the trap the MCP
   fell into. Rule: `repeat_count > 1` is only meaningful for multi-instruction units
   (cycling); single-instruction units express volume via `ball_count`. Enforce softly —
   normalize on save in the editor/MCP, and encode in the coaching guide and
   `create_unit`/`create_session` tool descriptions.
2. **Ball-granular flattening (when the block screen is built).** Progress today is a
   binary `completedStepIndices` set; a ball counter wants "7 of 15 balls into this
   instruction." Resolution: expand `ball_count = N` instructions to N steps at snapshot
   time, and have the block screen group them visually while the counter ticks them off.
   Then `completedStepIndices` *is* the ball count, each tick is a discrete event a future
   `ball_events` record can reference, and both authoring styles converge to the same
   execution shape. Per ADR 0002 this is a change to the `start_range_session` RPC (the
   single source of truth for expansion), not a Kotlin change and not a migration of
   existing rows — old snapshots keep their shape. Net effect: authoring
   (instructions/ball counts/repeats) and execution (flat sequence of ball-or-action
   events grouped into blocks) become cleanly decoupled.
3. **Club precedence needs documenting once, somewhere shared.** Club can come from four
   layers: instruction club → unit default → session-item override → runtime per-step
   override (the string-keyed `clubOverrides` map on the range session). Each layer is
   justified; the chain is the kind that grows a bug the day someone reorders it in one
   place.
4. **Override hygiene in MCP authoring.** The reviewed session had unit notes duplicated
   verbatim into session-item notes. Overrides should be null unless they differ from the
   base — coaching guide / tool-description fix.

Not a change — context: the reviewed session's rehearsal instructions carried
`ball_count: 1` despite consuming no ball, inflating totals. That data predates zero-ball
support; ADR 0003 now pins the three-valued semantics (`null` = uncounted, `0` =
deliberate no-ball action, `> 0` = N balls) and the coaching guide already directs the
MCP to use `0` for rehearsal/setup instructions.

## 6. Resolutions (grilling session, 2026-07-02)

All open questions resolved with the owner. One app ticket plus one parallel MCP change.

1. **Skip the intermediate rep-collapse step** — go straight to the block screen.
2. **Expansion rule** (ADR 0004): `ball_count = N > 0` → N Ball Steps; `0` → 1 Action
   Step; `null` (Uncounted) → 1 Action Step (lossy by design). New snapshots carry
   `snapshot_version = 2` (column already exists); v1 snapshots are never migrated and
   render generically (a v1 step with `ballCount: 20` is one tick labelled "20 balls").
3. **Terminology**: "Block" admitted as an execution-layer term — the live view of one
   Session Item within a Range Session. Ball Step / Action Step added to the glossary.
4. **Counter model**: one ball counter per block, driven by snapshot order. "+1"
   completes the next incomplete Ball Step, sweeping along preceding Action Steps in the
   same pass. Pass position ("Pass 3 of 8") is derived, not tracked. When only Action
   Steps remain, the button morphs into a "Done" check affordance.
5. **Decrement**: a "−1" affordance un-completes the last-completed Ball Step plus the
   Action Steps swept by that same tap (exact inverse of increment; may reopen a
   finished block). Future ball events must therefore be deletable/voidable.
6. **Navigation**: completely free — horizontal pager between blocks plus an overview
   for jumping. No gating. The overview shows per-block progress (e.g. `15/15 ✓ ·
   8/24 · —`) — that display, not locked doors, is what provides "did I finish
   everything?" confidence. Open lands on the first incomplete block. Swiping past the
   last block never auto-finishes.
7. **Finish**: lives on the overview (quiet until all blocks complete), plus an inline
   prompt when the final block completes. Finishing never requires 100% of steps.
   Finishing with incomplete steps opens a three-way dialog that names what's incomplete
   in block terms ("Backswing ladder 8/24, Random flags untouched"):
   **Complete remaining steps / Finish as-is / Cancel**.
   Batch-completed steps share a finish-time timestamp (self-evidently batch in data).
   Abandon unchanged. Completed = user said done, never step-derived.
8. **Canonicalization rule demoted to authoring guidance only** — ball-granular
   expansion makes both encodings converge at execution, and "normalize on save" would
   mutate shared Practice Units. Guidance goes in the coaching guide + tool
   descriptions; no editor enforcement.
9. **Per-ball / per-block results capture: fully out of scope.** This ticket records
   completions only. The block-complete state is the designed seam for future capture.
10. **Club swap gesture is instruction-scoped over step-keyed storage**: swapping mid-
    block fans out override keys to that instruction's remaining incomplete steps.
    Completed steps keep the club they were actually hit with; storage stays per-step
    (per-ball-capable for future capture). Prescribed rotations (wedge ladders) need no
    overrides; random-club drills author the randomness in instruction text and record
    nothing until capture exists. Club Resolution + Club Override now documented in
    `apps/mobile/CONTEXT.md` (two phases: planning layers collapse at snapshot time;
    exactly one override layer exists at runtime).
11. **MCP change ships in parallel** (single-user product, no sequencing risk):
    coaching-guide + tool-description guidance for override hygiene and the
    repeat-vs-ball-count convention, plus `create_session` stripping overrides that
    exactly equal the unit's base value (loss-free, guarantees override ≠ base in data).
    No validation errors for repeat-count style.
