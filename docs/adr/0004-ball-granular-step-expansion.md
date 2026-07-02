# Snapshot Steps are ball-granular: `ball_count = N` expands to N Steps

The `start_range_session` RPC expands each Practice Instruction into one Step **per
ball**, not one Step per instruction × repetition: a positive Ball Count N yields N Ball
Steps, while Zero and Uncounted each yield a single Action Step (per the three-valued
semantics of ADR 0003). Snapshots produced this way carry `snapshot_version = 2`;
version-1 rows are never migrated and keep their coarser shape forever. We chose this so
that completed Steps and hit balls are the *same fact*: the block-screen ball counter's
"+1" completes exactly one Ball Step, `completed_steps` **is** the ball count, and each
tick is a discrete, timestamp-bearing identity a future `ball_events` record can
reference. Both authoring styles — batched (`ball_count: 15`, repeat 1) and per-ball
(`repeat_count: 15`) — converge to the identical execution shape, decoupling authoring
granularity from execution UX.

This is the deliberately surprising part: the RPC emits N near-identical step rows where
one row with `ballCount: N` would look "normal." That is not accidental denormalization —
collapsing it would break the tick-equals-ball identity the execution model is built on.

## Status

accepted

## Considered options

- **Keep coarse steps, track ball tallies as separate counter state (rejected).** A
  `ball_tallies` map beside `completed_steps` avoids the row multiplication, but creates
  two parallel progress representations that must be reconciled (is a step with 7/15
  balls complete?), and a tally increment has no stable identity for a future
  `ball_events` row to reference.
- **Per-block progress records (rejected).** Coarsest option; loses per-ball timestamps
  entirely and forecloses per-ball capture rather than deferring it.
- **Client-side re-expansion of v1 snapshots (rejected).** Would create the second
  expansion implementation ADR 0002 exists to forbid. V1 snapshots are instead rendered
  generically: the block screen groups the flat step list by unit and ticks steps, so a
  v1 step with `ballCount: 20` is one tick labelled "20 balls."
- **Ball-granular expansion in the RPC, versioned (chosen).** Expansion stays
  backend-authoritative (ADR 0002); the Kotlin module keeps treating the snapshot as an
  opaque trusted blob; `range_sessions.snapshot_version` (already present, default 1)
  distinguishes the shapes without renderer branching.

## Consequences

- **A Step is no longer homogeneous.** It is a Ball Step (one ball) or an Action Step
  (Zero or Uncounted directive) — see `apps/mobile/CONTEXT.md`. UI counts Ball Steps;
  Action Steps render as check-offs.
- **Uncounted is lossy at execution.** An Uncounted instruction yields one Action Step
  regardless of balls actually hit; explicit Ball Counts are quietly rewarded. Accepted —
  the coaching guide already steers authoring toward explicit counts.
- **Snapshot row counts grow ~an order of magnitude** (one row per ball). Accepted as
  jsonb bloat, not a table: a 100-ball session is ~100 small JSON objects.
- **`completed_steps` doubles as the ball count**; per-step `clubOverrides` keys become
  per-ball-capable for free, satisfying "let speculative features shape the schema, not
  the screens."
- **Version 2 must ship in the RPC first** (ADR 0002's ordering rule); Kotlin models
  follow the RPC's output shape and need no structural change.
