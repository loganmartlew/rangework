# Ball Count is three-valued: positive, zero, and uncounted are distinct

A Practice Instruction's Ball Count (`PracticeInstruction.ballCount: Int?`) has three
semantically distinct states that must never be conflated: a **positive** count (N balls
hit), **zero** (a deliberate no-ball directive — practice swings for feel), and
**Uncounted** (`null` — the count is unknown / not specified). Zero means "deliberately
none"; Uncounted means "we don't know." We keep `null` and `0` as separate states rather
than collapsing them into one "no balls" value.

This is the deliberately surprising part: the type `Int?` permits *both* `null` and `0`,
which invites a future reader to "simplify" by collapsing them. Do not. The Coaching (MCP)
context depends on `null` as a load-bearing "uncounted" signal — `list_units` /
`list_sessions` report `has_uncounted_instructions` / `has_uncounted_items` and downgrade
a total to a partial estimate (`total_ball_count = null`) precisely when an instruction is
Uncounted. Collapsing `null` into `0` would silently destroy that distinction and report
fabricated-complete totals for plans whose volume is genuinely unknown.

## Status

accepted

## Considered options

- **Collapse `null` and `0` into a single "no balls" value (rejected).** Simpler model —
  one way to say "this instruction uses no balls," `ballCount` could even become a non-null
  `Int ≥ 0`. Rejected because `null` ("uncounted") and `0` ("deliberately zero") answer
  different questions ("do we know the count?" vs "how many balls?"), and the Coaching
  context already ships a feature built on that difference. Collapsing would delete a
  shipped capability and misreport partial totals as exact.

- **Three distinct states, `ballCount` stays `Int?` (chosen).** Issue #24 only required
  that `0` become *expressible*, not that "uncounted" be erased. We made `0` reachable
  everywhere (UI stepper floor `0`, draft validation rejects only `< 0`, DB check relaxed
  to `ball_count is null or ball_count >= 0`, MCP `create_unit` accepts nonnegative) while
  leaving `null` intact as the Uncounted signal. No type change, no row migration.

## Consequences

- **`ballCount` remains nullable on purpose.** `null` = Uncounted, `0` = deliberate zero,
  `> 0` = N balls. The looser-than-ideal `Int?` (vs `Int ≥ 0`) is the accepted cost of
  preserving the Uncounted distinction without a cross-context type/migration churn.
- **Display must not conflate the two.** `ballSummary(null)` renders **"Uncounted"**;
  `ballSummary(0)` renders **"0 balls"**.
- **Uncounted-total propagation is not yet uniform across contexts.** The Coaching context
  propagates Uncounted into Unit/Session totals; the mobile Planning & Execution totals
  (`PracticeUnit.derivedBallCount()`) currently flatten an Uncounted instruction to `0`
  when summing. This is a known, pre-existing gap, left out of scope for issue #24 — see
  the Ball Count / Uncounted entries in `apps/mobile/CONTEXT.md`.
