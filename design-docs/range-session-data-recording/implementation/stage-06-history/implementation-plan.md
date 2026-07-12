# Stage 6: History Detail

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md) — ⛳ ship point 2.
**Design reference:** [`../../design-decisions.md`](../../design-decisions.md) §4 (count provenance),
§6.2 (history detail consumer), and the mutability summary in §5. No prototype exists for this
screen — the epic flags no owner decisions here, so presentation calls are recorded below as
plan-level calls (P1–P4), same convention as Stage 5.
**Stage 4 record:** [`../stage-04-prose-capture/changes.md`](../stage-04-prose-capture/changes.md) —
`RangeSessionHistoryScreen` + `CompletedRangeSessionViewModel` shipped as "the notes/results half
of the Stage 6 history detail screen"; this stage adds the other half, no rework of what's there.
**Stage 5 record:** [`../stage-05-observations/changes.md`](../stage-05-observations/changes.md) —
glyphs, chip rows, grid heatmap, and rendering transforms shipped; this stage reuses them
read-only.
**Status:** draft — awaiting review.

## Objective

Extend the completed-session detail screen with the observation half the Stage 4 screen
deliberately left out (its own kdoc: "No observation summaries or provenance labels — that is
Stage 6's remaining scope"):

1. **Per-block observation summaries** — one read-only tally surface per enabled Observation
   Type, in the same visual vocabulary as the block screen (chip-row counts, 3×3 heatmaps,
   glyphs), handedness-aware.
2. **Provenance-labeled success counts** — the existing frozen-count row becomes a
   `BlockSuccessCount`-driven row: derived "X of Y observed" vs manual "X of Y balls", never
   both, per design §4.
3. **Denominator honesty throughout** — every per-type row carries its own
   `observed/completed balls` denominator.

Android-only: ViewModel + screen + one new component file. **Zero `shared/` changes** — tallies
(`typeTallies`), provenance (`successCount`), rendering transforms, and the recorder read
(`observations`) all shipped in Stages 2/5. No SQL, no MCP, no authoring or block-screen changes.

This is the epic's second ship point: after merge (and Stage 7, tracked separately), the epic
close steps apply — update `design-decisions.md` status, re-check `apps/mobile/CONTEXT.md`
vocabulary, file follow-up issues for deferred items.

## Dependencies — all shipped, names verified against code

- **Tallies & provenance** (`shared/model/ObservationTallies.kt`): `ExecutionBlock.typeTallies`
  (per-type `TypeTally(observedCount, valueCounts)` over completed Ball Steps, hygiene rules
  built in) and `ExecutionBlock.successCount` → `BlockSuccessCount.Derived/Manual/None`.
  `successCount` already encodes "derived wins" — a stray `manualCount` alongside an enabled
  Success type is ignored. Feeding history through it (instead of the current direct
  `blockResult.manualCount` render) closes the one place a stray manual number could still leak.
- **Read path** (`recording/RangeSessionRecorder.kt`): `observations(rangeSessionId)` →
  `List<Observation>`. Same call the block screen uses.
- **Rendering** (`shared/model/ObservationRendering.kt` + Stage 5 components):
  `strikeDisplayColumns`, `shapeDisplayRows/Columns`, `golferLabel`; `ObservationGlyphs.kt`
  (all glyphs take `LocalContentColor`); `ObservationCaptureSection.kt` (`ObservationChipRow`
  already supports a no-staging, disabled render — the block-complete state);
  `ObservationGridDialog.kt` (heatmap cell rendering + `gridSpec`, currently private — see P2).
- **Handedness:** `MeasurementPreferences.handedness` via `measurementPreferencesRepository`;
  RIGHT fallback pattern established in `RangeSessionViewModel.loadSession`.
- **Club glyph shape:** `resolveCurrentBallShape` pattern in `ExecutionBlockPage.kt` maps a
  step's club code → `ClubGlyphShape` via the enabled-club catalog; the history route in
  `RangeworkApp.kt` can reuse the same `plannerUiState.clubCatalog.filter { it.code in
  plannerUiState.enabledClubCodes }` expression the range-session route uses (line ~366).
- **Screen being extended:** `RangeSessionHistoryScreen.kt` (per-block: title → frozen manual
  count → note editor) + `CompletedRangeSessionViewModel` (loads session + elapsed; saves the
  two prose fields).

## Plan-level calls (P1–P4) — recorded, not gates

### P1 — Summaries reuse the capture surfaces read-only; grids render inline, not behind a tap

The design's §6.1 rule ("the tally surface is the input surface") fixed the *vocabulary*:
chips with counts, heatmap grids, glyphs, per-type denominators. History should speak the same
vocabulary — a player who tallied at the range recognizes the same rows at home. **Chosen:**

- **Chip types** (Success, Contact, Distance, Direction): reuse `ObservationChipRow` with
  `enabled = false`, no staged value, no arming — the exact block-complete tally render, but
  **not dimmed** (the block screen's 50% alpha means "input disabled *here, now*"; history is a
  reading surface, and dimming a whole screen's content is noise). Success keeps its criterion
  rubric on the header.
- **Grid types** (Strike Location, Shape): render the 3×3 heatmap **inline**, always visible —
  not the launcher-row-plus-dialog gesture. The launcher/dialog split is capture ergonomics
  (one glance, one tap, glove on); history has vertical space and no pending ball, and hiding
  the heatmap behind a tap would make the summary section mostly-empty rows. Cells keep the
  Stage 5 ramp, corner counts, and body glyphs; axis headers from the shipped transforms;
  no staged border ever.
- The section sits in one card per block ("Observations" is redundant as a title — the rows
  are self-labeling; no card header) so the tallies group visually the way the capture card
  groups them on the block screen.

**Rejected:** compact text summaries ("Flush 8 · Thin 3") — a second vocabulary to learn,
loses zero-count visibility and the glyphs; dimmed reuse of the full capture stack including
launcher rows — carries input affordances (chevrons, dialogs) into a surface with nothing to
input. Tripwire restated: no charts, no judgement colours (Success tallies get the same neutral
treatment as every type), no progress framing — this is exactly the surface where "just one
bar chart" will itch. Trends are deferred by design until real data exists.

### P2 — Extract the grid body from `ObservationGridDialog` rather than duplicate it

The inline history grid and the capture dialog must render identically (same `gridSpec` axis
logic, same heatmap ramp, same cell anatomy). **Chosen:** extract the dialog's grid content
into an internal `ObservationGridContent` composable (grid spec, axis headers, heatmap cells,
optional staged-cell border + `onCell` tap only when a handler is passed) in the same file or
the new summary file; `ObservationGridDialog` becomes a thin `Dialog` wrapper around it;
history renders it directly with taps disabled. Pure refactor — the dialog's behaviour and
visuals are pixel-identical, covered by the existing manual-flow checks.

**Rejected:** copying the ~80 lines into the summary component — two ramps to keep in sync is
how the heatmap drifts.

### P3 — Handedness read at view time, current preference

Stored values are canonical (Stage 2 D4); rendering flips. History renders with the viewer's
*current* handedness preference, loaded once per screen load with the RIGHT fallback — same as
the block screen. No per-session handedness is stored anywhere, and inventing one for history
would contradict D4 (the data is player-independent; presentation follows the present player).
Practical consequence: none for the single-user product.

### P4 — Success line: replace `FrozenCountRow` with a provenance row driven by `successCount`

The current row renders `blockResult.manualCount` directly. Route it through the shared
`ExecutionBlock.successCount(...)` and render by case:

| `BlockSuccessCount` | Render (mono style, criterion rubric above as today) |
|---|---|
| `Derived(hits, observed)` | `"{hits} of {observed} observed"` |
| `Manual(count, totalBalls)` | `"{count} of {totalBalls} balls"` (today's wording, unchanged) |
| `None` | row absent |

The two wordings are the design §4 labels verbatim — "observed" vs "balls" *is* the provenance
label; no "(manual)"/"(derived)" suffix needed. Accessibility strings spell it out:
"...successful, of observed balls" / "...successful, of all balls". This is a behaviour fix as
well as a feature: a stray manual count on a Success-enabled block (impossible from the app UI,
possible from data skew) currently renders as if legitimate; `successCount` ignores it.

## Fixed in this plan

- **Gating:** everything new is gated on `session.supportsDataCapture` (v3). v1/v2 sessions
  render the screen byte-identical to Stage 4 — no observation load, no summary cards, the
  (always-absent for v2) count row logic untouched in effect.
- **Per-block render order:** block title → success/provenance row → observation summary card
  → note editor. Note stays last, matching the block screen's "block result last" placement.
- **Which blocks get a summary card:** v3 blocks with ≥1 enabled Observation Type **and** ≥1
  Ball Step. Enabled-but-unobserved types still render their row at zero with a `0/N` observed
  denominator — that a block went unobserved is information, not absence (denominator honesty).
  Zero-enabled-types blocks and action-only blocks render exactly as Stage 4 (note ± count).
- **Denominators:** every type row header shows `observed/completedBalls` for that block
  (completed Ball Steps, not planned balls) — same semantics and mono style as capture.
  Per-type denominators diverge by design; nothing aggregates across types.
- **Success chip row + provenance row coexist:** when Success is enabled, the block shows both
  the Success tally row (inside the summary card, with Hit/Miss counts) and the derived
  provenance line (outside it, the block's one headline number). Same number twice is fine —
  same provenance, two granularities; the design's one-source rule forbids *rival* numbers,
  not restatement.
- **Read-only means read-only:** no tap targets in the summary card — no staging, no dialogs,
  no edit sheet from history. Corrections ended when the session left Active (freeze matrix);
  the recorder would reject writes anyway. Prose editing (session + block notes) is untouched.
- **Load failure degrades:** observation load failure → summaries render empty-but-present is
  wrong (it would read as "0 observed", a false statement) — instead, on failure the summary
  cards are **omitted** and one snackbar surfaces ("Couldn't load observations"); notes and
  counts still work. This differs deliberately from the block screen's empty-tallies fallback,
  where capture had to stay usable; history has no such floor to protect, and honesty wins.
- **Manual count for `Manual` blocks renders from `successCount`,** not from `blockResult`
  directly — one code path for the number.

## Likely files

No `shared/` changes. No new strings beyond the labels above.

### New — `androidApp/.../ui/components/`

| File | Purpose |
|---|---|
| `ObservationSummarySection.kt` | The per-block read-only summary card: ordered types (reuse `orderedCaptureTypes` — export from `ObservationCaptureSection` if private), `ObservationChipRow` in tally-only mode for chip types, inline `ObservationGridContent` (P2) for Strike/Shape with axis headers and denominator header rows. Props in: enabled types, tallies, completed-ball count, criterion, handedness, club glyph shape. Pure presentation |

### Modified — `androidApp/`

| File | Change |
|---|---|
| `ui/components/ObservationGridDialog.kt` | P2 extraction: `ObservationGridContent` internal composable (cells, heatmap, axis headers, optional tap/staged handling); dialog wraps it unchanged |
| `ui/CompletedRangeSessionViewModel.kt` | + `measurementPreferencesRepository` ctor param (defaulted null; factory updated). UiState: + `observationsByStep: Map<Int, Observation>`, + `handedness: Handedness = RIGHT`, + `observationsUnavailable: Boolean`. `load()`: for v3 sessions, load observations via recorder (failure → `observationsUnavailable = true` + notification) and handedness (failure → RIGHT); v1/v2 skip both. No new write paths |
| `ui/screens/RangeSessionHistoryScreen.kt` | Per block: compute `typeTallies`/`successCount` (`remember` over session + observations); `FrozenCountRow` → provenance row (P4); insert `ObservationSummarySection` per the fixed render order and gating; thread handedness + per-block club glyph shape (first Ball Step's club via the `resolveCurrentBallShape` pattern — for a completed block "current ball" is empty, so resolve from the block's *first* Ball Step, overrides respected, IRON fallback) |
| `ui/RangeworkApp.kt` | History route: pass `measurementPreferencesRepository` into the factory; pass the enabled-club catalog (same expression as the range-session route) into the screen for glyph-shape resolution |
| `test/.../CompletedRangeSessionViewModelTest.kt` | New cases per the test matrix below; fake gains observation storage if it lacks it (the shared fake from `RangeSessionViewModelTest` is the precedent) |

## Behaviour specs

### ViewModel test matrix

- v3 session: observations + handedness loaded; state carries both keyed by step index.
- v3 observation load failure: `observationsUnavailable` set, notification set, session still
  renders, note saves still function.
- v3 handedness load failure/null repo: `RIGHT`, no notification (matches Stage 5).
- v2 session: no recorder read call (fake asserts), state stays empty/default.
- Null recorder: loads skipped without crash; prose saves already no-op (existing behaviour).

### Screen/logic specs (presentation-level, verified in manual flow + any pure-helper tests)

- Provenance row: Derived renders even at `0 of 0 observed` (Success enabled, nothing
  observed — honest); Manual renders only when criterion + stored count; None renders nothing.
- Stray manual count with Success enabled: derived line shown, manual number nowhere on the
  screen (unit-testable via `successCount`, already covered in shared tests — screen just must
  not add a second path; asserted by code shape, spot-checked in review).
- Summary rows honour handedness: strike columns mirrored for LH with mirrored clubface glyphs;
  shape golfer labels fixed with physical values mirrored; Direction/Distance identical.
- Same-unit-twice sessions: summaries are per *block* (step-index scoped); block results/notes
  remain per *unit index* — existing Stage 4 semantics, unchanged; the summary card must key
  off the block, not the unit index.

## Edge cases

- **Partially-completed blocks** (finish with "complete remaining" or abandoned mid-block →
  completed): denominators use completed Ball Steps; batch-completed balls read as unobserved.
  Numbers may look sparse — that is the design's honesty, not a bug.
- **Block with zero completed Ball Steps** but enabled types: rows render `0/0` — acceptable
  (rare: a criterion block never started before finish). No special case.
- **Orphan observation rows / out-of-vocabulary values:** excluded by tally hygiene in shared;
  nothing for the UI to do.
- **Elapsed/stats failures:** untouched — existing paths.
- **Abandoned sessions:** unreachable from the history list (Completed only); if deep-linked,
  the screen renders whatever loads under the same rules — no new handling.
- **Rotation:** tallies are derived state (`remember` over loaded data), note drafts already
  `rememberSaveable` — nothing new to save.

## Validation checklist

- [ ] `CompletedRangeSessionViewModelTest` matrix above; existing cases untouched-green
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug` passes
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` passes
- [ ] **Manual device flow (local stack):**
      **(A)** complete a session with all six types on one block, partial observation (skip
      some balls, leave one type unstaged on others): history shows diverging per-type
      denominators, derived "X of Y observed" line, zero-count segments blank-with-space;
      **(B)** criterion block, no Success type, manual count entered: "X of Y balls" wording,
      no observation card beyond enabled types (if any);
      **(C)** note-only block (no types, no criterion): renders exactly as before this stage;
      **(D)** capture-dialog regression: Stage 5 grid dialog pixel-identical after the P2
      extraction (side-by-side with a pre-change build or screenshot);
      **(E)** switch Settings to left-handed, reopen the same history session: strike grid
      mirrored, shape flight glyphs mirrored under fixed labels, Direction unchanged;
      **(F)** v2 session from history: screen identical to Stage 4, no observation load;
      **(G)** notes still edit and auto-save on this screen, counts frozen.
- [ ] `changes.md` written in this folder on merge
- [ ] Epic-close reminders once Stage 7 + field test #2 are also done: `design-decisions.md`
      status line, `CONTEXT.md` vocabulary check, follow-up issues for deferred items (trends
      UI, prompted block results, item-level criterion override)

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| P2 extraction subtly changes the capture dialog (every v3 session uses it) | Medium | Extraction is move-not-modify; manual flow (D) does a side-by-side check |
| History screen renders differently for v1/v2 sessions | Low | All new UI behind `supportsDataCapture`; manual flow (F) |
| Provenance-row swap drops the manual count some existing session displays today | Low | `Manual` case reproduces today's wording/data exactly; only the Success-enabled-plus-stray-manual case changes, and that change is the design's rule |
| `ObservationChipRow` reuse forces API changes that ripple into the block screen | Low–Medium | The row already renders the needed state (block-complete mode); if a flag is unavoidable, default it so capture call sites are untouched |
| Scope creep toward trends/charts on the first screen that invites them | Medium | Tripwire restated in P1 and Fixed rules; review against design §6 deferred list |
