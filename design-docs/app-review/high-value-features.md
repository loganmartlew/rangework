# High-Value Features

Feature opportunities ranked by impact-per-effort, grounded in what the data model and
existing surfaces already support. The strongest signal in this review: **three independent
module reviews (Android UI, shared KMP, Supabase) converged on cross-session analytics as the
single highest-value addition** — the ball-granular recording investment is fully paid in on
the capture side and entirely uncashed on the read side.

The baseline plan (`design-docs/baseline-plan.md`) already anticipates most of these
("cross-unit analytics should be anticipated, including individual-ball club/ball data
points"; curated metric catalog; media; offline) — so these are sequencing proposals, not
scope inventions.

---

## F1 — Cross-session, per-club performance analytics — impact HIGH, effort MEDIUM-HIGH

The app captures per-ball data across six observation types (Success, Contact, Distance,
Direction, Strike location, Shape), each keyed by `stepIndex` with the actually-hit club
recoverable via `snapshot.steps[stepIndex].club` + `clubOverrides[stepIndex]`
(`RangeSession.kt:19-20`), and per-ball timestamps in `completed_steps`. Today the only read
surface is per-template history (`RangeSessionHistoryScreen`,
`CompletedRangeSessionViewModel.stats()`). Nothing aggregates *across* sessions.

What the data already supports with no schema change:

- **Per-club distributions** — contact quality, direction, distance, shape by club.
  `ObservationTallies.typeTally` (`ObservationTallies.kt:53-67`) already does this aggregation
  per block; a use case running it across a club filter and across sessions is the only
  missing piece.
- **Success/skill trend over time** — `BlockSuccessCount.Derived/Manual`
  (`ObservationTallies.kt:31-40`) already computes hit rate with provenance;
  `CompletedRangeSessionSummary.completedAt` gives the time axis. "Success rate for wedge work
  over the last 8 sessions" needs no new data.
- **Miss-pattern summaries** — the observation scales are ordinal (fat→thin, left→right,
  short→long in `ObservationCatalog.kt`), so mean tendency and dispersion ("you miss right
  with driver, thin with wedges") come straight from `TypeTally.valueCounts`, and
  `ObservationRendering` already handles the handedness transform.
- **Strike-location heat map over time** — the grid data is captured per ball.

**Recommended shape:** build the SQL flattened view first (see
[integration-opportunities.md](integration-opportunities.md) I2) so the app, the MCP server,
and any future stats screen all read the same aggregation, then add a shared-layer use case
and a stats screen. This is the differentiator between Rangework and a ball counter.

## F2 — MCP: update, duplicate, and delete tools — impact HIGH, effort LOW

The conversational planning loop currently dead-ends on "actually make that drill 20 balls
instead of 15" — the AI can only create duplicates. Full detail in
[integration-opportunities.md](integration-opportunities.md) I1; listed here because it's the
cheapest high-impact item in the whole review: the DB RPCs (upsert semantics,
`duplicate_practice_session`) already exist and are tested.

## F3 — Quantitative distance capture — impact MEDIUM, effort MEDIUM

The Distance observation is a 5-bucket ordinal (`WAY_SHORT…WAY_LONG`,
`ObservationCaptureSection.kt:387-393`), yet Settings already exposes distance/speed unit
preferences (`SettingsViewModel.selectDistanceUnit`/`selectSpeedUnit`) with nothing consuming
them for capture. Golfers with rangefinders/launch monitors would log real carry numbers.
This is also the schema gap the shared-layer review identified as *deliberate*: no continuous
numeric capture exists anywhere, so true dispersion in yards/degrees and any future
launch-monitor integration hang on this. The baseline plan's curated-metric-catalog design
(semantic key + dimension + canonical storage, display converted per user settings) is the
right frame when this gets picked up.

## F4 — Library search — impact MEDIUM, effort LOW

Free-text search on `UnitListScreen`/`SessionListScreen` alongside the existing tag filter.
AI-assisted creation makes libraries grow faster than hand-authoring; archiving solved
clutter, search solves findability. (Also add `limit`/pagination to the MCP list tools — same
underlying growth problem; see [potential-bugs.md](potential-bugs.md) B6.)

## F5 — Rest/interval awareness between blocks — impact MEDIUM, effort MEDIUM

Per-item rest timers were an explicit baseline-plan decision ("Session items should support
notes, rest timers, and focus cues") that never shipped; the timer was later removed in favor
of ball-weighted duration. The block pager is a natural host for an optional per-block rest
prompt — useful for pressure/interval practice without resurrecting the old timer model.

## F6 — Share/export a completed session summary — impact LOW-MEDIUM, effort LOW

`FinishSummaryContent` already renders a clean stat card (balls, steps, completion %, time)
with only Done and Archive actions (`FinishSummaryContent.kt:129-155`). A share intent
(image or text) is near-zero surface and gives the app organic distribution.

## F7 — Offline ball counting — impact HIGH (for the core use case), effort HIGH

Every `+1` tap is a `set_range_session_steps_completion` network round-trip; a phone with no
signal at the range cannot count balls — arguably the most consequential gap for an
at-the-range app. Filed under [integration-opportunities.md](integration-opportunities.md) I4
because the work is architectural, but it belongs on any feature roadmap discussion: the
ball-granular model (discrete, timestamped, idempotent, mergeable step completions) is
*already the right shape* for a local event-log/outbox, which is unusual — most apps have to
redesign their data model to get offline; this one just has to buffer.

## F8 — Proactive coaching from recorded data (MCP methodology, not code) — impact MEDIUM, effort LOW

`get_range_session`/`list_range_sessions` already expose per-block results and observations,
and the coaching guide instructs the model to consult them when asked. The next lever is
having the AI *proactively* reference observation trends when proposing a session ("Tuesday's
wedge block showed toe strikes — here's a gate drill"). This is a coaching-guide revision, not
a new tool — and it compounds with F1's aggregation views once those exist.

## F9 — "Most-used club" / monthly volume stats — impact LOW-MEDIUM, effort LOW (after F1's view)

A lightweight `user_stats` materialized view over the ball-granular data cheaply unlocks
"total balls this month," "most-practiced club," streaks — engagement surface for the
home/overview screen. Explicitly do *not* derive this from the audit log (see
[potential-bugs.md](potential-bugs.md) B4).

---

## Deliberately not recommended (yet)

- **Live range-session control via MCP** (AI starts/drives a session): execution is a
  real-time, on-device, snapshot-at-start flow; the current read-only MCP exposure matches the
  agreed design. Revisit only if a concrete use case appears.
- **Realtime/multi-device sync**: no blocker exists (RLS is realtime-safe), but the
  single-device execution model makes it premature. The conflict-handling asymmetry it would
  expose is documented in [integration-opportunities.md](integration-opportunities.md) I5.
- **Media attachments** (photos/videos on units): expected later per the baseline plan, but it
  drags in storage, upload UX, and cost before the analytics payoff is banked. F1 first.
