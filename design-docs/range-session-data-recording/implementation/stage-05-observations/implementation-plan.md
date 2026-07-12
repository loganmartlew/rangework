# Stage 5: Per-ball Observation Capture

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design reference:** [`design-review.md`](./design-review.md) + [`prototype.html`](./prototype.html) — per D2 the
prototype (plus its doc) *is* the UI spec. The "Prototype vs. production" section of the review
governs what is authoritative (interaction grammar, layout structure, glyph vocabulary,
handedness rules) vs. plan-decided (dp values, Material components, glyph format, ramps).
**Stage 2 record:** [`../stage-02-models/changes.md`](../stage-02-models/changes.md) — models, tallies, rendering
transforms, recorder, and freeze rules are shipped and consumed here, not built.
**Stage 4 record:** [`../stage-04-prose-capture/changes.md`](../stage-04-prose-capture/changes.md) — block screen
already carries `BlockResultSection`; notes auto-save (no Save buttons anywhere on this screen).
**Status:** draft — six plan-level calls recorded below (P1–P6) for review; no epic-flagged owner
decisions remain (the capture-UI design review gate was satisfied by the prototype sign-off).

## Objective

The per-ball capture surface on the block screen, exactly as the prototype demonstrates:

1. **Merged counter+capture card** — chip rows and grid launchers live inside the existing
   counter card, between the ball readout and the −/+1 row (design review §0, D1 provisional —
   this stage is the "first device build" that confirms it).
2. **Staging + commit model** — tap-to-stage, auto-commit with arm-flash when every enabled
   type is staged, +1 commits partial/empty any time, − voids the last ball's observations.
3. **Tally-is-input rendering** — live counts on chips, magnitude-only heatmap in the 3×3 grid
   dialogs, per-type `observed/completed` denominators, count-blank-at-zero.
4. **SVG-vocabulary value glyphs** drawn in Compose (D3).
5. **Per-ball edit sheet** off the ball-instruction rows, write-through corrections while Active.
6. **Handedness-aware rendering** via the shipped `ObservationRendering.kt` transforms.

Android UI + ViewModel work in `androidApp/`, plus **one confirmed `shared/` touch** (P2). No
SQL, no MCP, no authoring changes. After merge: **field test #2** (epic validation gate) with
observations enabled on 1–2 blocks.

## Dependencies — all shipped, names verified against code

- **Vocabulary & rendering** (`shared/model/`): `ObservationType` (+ `enabledObservationTypes`
  on `SnapshotUnit` — already filters unknown ids and SUCCESS-without-criterion),
  `SuccessValue`/`ContactValue`/`DistanceValue`/`DirectionValue`/`StrikeLocation`/`ShapeFlight`,
  `Handedness`, `strikeDisplayRows/Columns`, `shapeDisplayRows/Columns`,
  `startLabel`/`curveLabel`/`golferLabel`. ⚠️ One prototype drift to *not* copy: the prototype JS
  uses `'on'` for Direction's centre value; the shipped wire id is **`on_line`**
  (`DirectionValue.ON_LINE`). Production code iterates the enums, so the drift cannot recur —
  but do not port value strings from the prototype by hand.
- **Tallies** (`ObservationTallies.kt`): `typeTally`/`typeTallies` (already exclude
  uncompleted steps, out-of-vocabulary values, and orphan rows — tally hygiene is not UI work),
  `BlockSuccessCount` (untouched here; `BlockResultSection`'s manual-count suppression when
  Success is enabled shipped in Stage 4).
- **Recorder** (`recording/RangeSessionRecorder.kt` via `DataFoundation.rangeSessionRecorder`):
  `recordObservation` (full-map upsert by step index), `voidObservations`,
  `uncompleteStepsVoidingObservations` (the −1 sweep), `observations`. All return
  `RecordingResult` (rejections are data → snackbar). Freeze matrix and
  `validateObservationWrite` are enforced inside — **UI hides, shared rejects**; no freeze
  logic in composables.
- **Step granularity:** snapshot v2+ expands one Ball Step per ball (ADR 0004), so
  `stepIndex` *is* the ball identity — the observation keying assumed throughout.
- **Counter mechanics** (`ExecutionBlocks.kt`): `incrementTargets` (next incomplete Ball Step,
  sweeping preceding Action Steps; all-remaining-Actions = the "Done" tap),
  `decrementTargets` (last completed segment), `hasIncompleteBallSteps`.
- **Handedness preference:** `MeasurementPreferences.handedness` via
  `measurementPreferencesRepository` (Stage 3 settings toggle shipped). Not yet threaded into
  the range-session flow — this stage adds that wiring.

## Plan-level calls (P1–P6) — recorded, not gates

### P1 — No 5a/5b split: one stage, one PR; the edit sheet is the cut line

The epic pre-agreed a possible split (5a capture gesture + commit model, 5b tallies +
corrections). **Chosen: don't split.** The merged design dissolved the seam: the tally surface
*is* the input surface, so chips without counts aren't a shippable intermediate; and −1 must
void observations from the first commit onward (otherwise decrement-then-recount resurrects a
stale ball's values — a correctness hole, not a polish gap). The domain heavy-lift already
shipped in Stage 2; what remains is UI + ViewModel. If the PR grows unwieldy, the one
genuinely severable piece is the **per-ball edit sheet** (§ Behaviour specs) — it corrects
data the capture surface creates, and its absence for a week costs only "use − and re-hit".
Cut there and only there.

### P2 — Commit is one guarded shared write: `completeStepsRecordingObservation`

A commit is "counter ticks + observation written" perceived as one event. Implementing it as
two independent calls from the ViewModel (repository `setStepsCompletion` + recorder
`recordObservation`) puts the partial-failure ordering invariant in UI code, against the
project rule that invariants live in shared KMP logic. **Chosen:** add one recorder method —
the mirror image of the shipped `uncompleteStepsVoidingObservations`:

```kotlin
/**
 * The +1/auto-commit: validates, completes the steps, then upserts the ball's
 * Observation. Validation runs first (nothing written on a rejected value);
 * completion before observation so a partial failure leaves a legal state
 * (completed-but-unobserved), never an observation on an uncompleted step.
 * An empty [values] map writes no observation row at all (Stage 1 D2: an
 * empty commit is byte-identical to never observing).
 */
suspend fun completeStepsRecordingObservation(
    rangeSessionId: String,
    stepIndices: List<Int>,
    values: Map<String, String>,
): RecordingResult<RangeSession>
```

Semantics: `stepIndices` = `incrementTargets` output; the observation attaches to the single
Ball Step among them (`steps[it].isBallStep`); when `values` is non-empty, validate via
`validateObservationWrite(ballStep, values)` *before* any write and reject the whole commit on
failure. When `values` is empty, or the targets contain no Ball Step (the "Done" tap arriving
here defensively), it degrades to plain step completion. This is the **only permitted
`shared/` edit** (interface + `DefaultRangeSessionRecorder` + `RangeSessionRecorderTest`
cases + the test fakes it ripples into).

**Rejected:** two sequential calls in the ViewModel — ordering and rollback rules would be
re-derived (and re-tested) at the UI layer; a combined method is also what MCP or any future
capture surface would want.

### P3 — Glyphs as parametric Canvas draw functions, one file

The glyph set is parametric, not iconic: the impact dot moves per strike cell, the flight path
is computed from physical start/curve, the clubface mirrors per handedness. Static
`ImageVector` assets would mean ~30 near-duplicate vectors that still couldn't mirror.
**Chosen:** one file of small composables wrapping `Canvas`, coordinates ported from the
prototype's SVG (its viewBoxes are the reference geometry, per the review): `DirectionGlyph`,
`DistanceGlyph`, `ContactGlyph`, `FlightGlyph(start, curve)` (physical values — lefty curves
mirror by construction), `ClubfaceGlyph(row, column, handedness)` (face art + hosel mirror via
`strikeDisplayColumns`), plus the nine-dot `MiniGridGlyph` for unstaged launchers. Colour is
always `LocalContentColor.current` (monochrome, judgement-free — tripwire); size via a
`size: Dp` param with the prototype's aspect ratios.

### P4 — Heatmap ramp: continuous primary-alpha, no steps

Cell fill = `colorScheme.primary.copy(alpha = a)` where `a = 0f` at zero count, else
`0.12f + 0.45f * (count / maxCount)` — the prototype's exact ramp. Continuous, single-hue,
magnitude-only; no discrete Material tonal roles (a stepped ramp invites reading bands as
categories, which drifts toward judgement). Count renders in the cell corner only when > 0.

### P5 — Staging lives in the ViewModel

Pending-ball staging is per block, keyed by block index, in `RangeSessionUiState`
(`Map<Int, Map<String, String>>` of type id → value). It survives rotation and pager swipes
(swipe away mid-stage and back — still there), and dies with process death — accepted: a
pending ball was never data, and `rememberSaveable`-ing a nested map buys complexity for a
state the design explicitly treats as uncommitted. The auto-commit delay is a constructor
param (`autoCommitDelayMillis: Long = 300`) so tests drive it with a test dispatcher.

### P6 — Haptic tick on commit: yes

The review's nice-to-have costs one line: `LocalHapticFeedback.current.performHapticFeedback(
HapticFeedbackType.Confirm)` fired on commit (both paths — it's part of "perceived as one
event", alongside the counter bump and staging release). No sound, no snackbar, no toast.

## Fixed in this plan (from the authoritative spec)

- **Gate on `supportsDataCapture` and `enabledObservationTypes`.** v1/v2 sessions render the
  block screen byte-for-byte as today (including the old decrement path). v3 blocks with zero
  enabled types get no capture stack — the counter card is unchanged for them.
- **Row order:** Success (two-segment, rubric on header, no glyph) → chip types in catalog
  order (Contact, Distance, Direction) → the two grid launchers together at the bottom
  (Strike Location, then Shape), nearest the +1. Grouped by input pattern.
- **Chip rows:** full-width, equal `weight(1f)` segments, never scrollable. Segment anatomy
  top-to-bottom: glyph (except Success), value label, tally count (empty-but-space-reserved at
  zero so rows don't jump). Header: type label left, `observed/completedBalls` denominator
  right (mono style). Exact labels:

  | Type | Segments |
  |---|---|
  | Success | Hit · Miss |
  | Contact | V. Fat · Fat · Flush · Thin · V. Thin |
  | Distance | W. Short · Short · On · Long · W. Long |
  | Direction | W. Left · Left · On · Right · W. Right |

  Labels at `labelSmall`-scale, single line. Compact-width floor: five segments inside the
  card at a 360dp screen ≈ 58dp each — these strings fit; if a future locale/font breaks
  that, the label may shrink but the row **never** scrolls (fixed constraint).
- **Staging semantics:** tap stages for the pending ball; re-tap un-stages; tapping another
  segment moves the selection. Counts are history; the fill is *this* ball — counts never bump
  before commit. Staged = `secondaryContainer` treatment; arm-flash = `primary` treatment for
  the delay window, then commit.
- **Auto-commit:** fires when every enabled type is staged (never when zero types enabled).
  During the ~300ms arm window all capture input and +1/− for that block are ignored
  (prototype behaviour). Commit clears staging, bumps the counter (scale bump animation +
  haptic), writes through P2.
- **+1 commits whatever is staged** — partial or empty; an empty commit writes no observation
  row. Neither button ever changes appearance based on staging; committed partial/empty balls
  carry no badges. **"Done"** (action-only remainder) and block-complete states are unchanged.
- **Undo (−):** v3 sessions route the existing decrement through
  `uncompleteStepsVoidingObservations` — optimistic removal of the segment's step indices
  *and* their observations from UI state, revert both on failure. Voided values do not return
  to staging; staging in progress survives untouched.
- **Grid dialogs:** one tap in, one tap out. Cell tap stages and closes immediately (auto-
  commit may arm as it closes); re-tap of the staged cell clears and closes; scrim tap
  dismisses without staging. Header: type label + denominator. Strike columns/labels from
  `strikeDisplayColumns(handedness)` (Heel·Center·Toe RH, mirrored LH; rows never flip);
  Shape rows/cols keep golfer labels Pull/Straight/Push × Draw/Straight/Fade in constant
  screen positions, physical value per cell from `shapeDisplayRows/Columns(handedness)`.
  Staged cell = 2dp primary border; heatmap per P4; glyph in every cell body.
- **Block-complete state:** when `hasIncompleteBallSteps` is false, the capture stack renders
  as dimmed read-only tallies (50% alpha, input disabled) — a block-review surface. − and the
  edit sheet stay available while Active.
- **Page placement unchanged:** focus cue → counter+capture card → Finish → instruction list →
  Notes → Block result last. No reordering, no new cards.
- **Deferred-scope tripwire:** no charts, no judgement colours (Success chips get the same
  neutral staged/tally treatment as every other type), no progress-toward-target framing.
- **UI hides, shared rejects:** recorder rejections land in the existing snackbar channel.
  Null recorder (misconfigured foundation) → capture affordances render, writes no-op into
  the existing degradation path.

## Likely files

### Shared — the one P2 touch

| File | Change |
|---|---|
| `shared/.../recording/RangeSessionRecorder.kt` | + `completeStepsRecordingObservation` |
| `shared/.../recording/DefaultRangeSessionRecorder.kt` | Implementation: validate → `setStepsCompletion` → conditional `upsertObservation`; rejection short-circuits before any write |
| `shared/.../recording/RangeSessionRecorderTest.kt` | Happy path (values land on the Ball Step), empty-values path (no row written), rejection paths (frozen, v2, bad value → nothing written), action-only targets degrade to completion |

### New — `androidApp/.../ui/components/`

| File | Purpose |
|---|---|
| `ObservationGlyphs.kt` | P3 Canvas glyph set: `DirectionGlyph`, `DistanceGlyph`, `ContactGlyph`, `FlightGlyph`, `ClubfaceGlyph`, `MiniGridGlyph`. Geometry ported from prototype SVGs; `LocalContentColor`; no judgement colours |
| `ObservationCaptureSection.kt` | The capture stack rendered inside the counter card: `ObservationChipRow` (header + rubric + denominator + equal segments with glyph/label/count), `GridLauncherRow` (mini-grid ⇢ value glyph + golfer-term summary + chevron), staged/arm visual states, read-only mode. Pure presentation — all state and callbacks in |
| `ObservationGridDialog.kt` | 3×3 `Dialog` for Strike/Shape: axis headers from the shipped rendering transforms, heatmap cells (P4) with corner counts and body glyphs, staged border, tap/re-tap/scrim semantics, staging vs edit-mode target |
| `BallEditSheet.kt` | `ModalBottomSheet` scoped to one instruction's completed Ball Steps, newest first. Row: "Ball N" (ordinal within the instruction) + value summary in enabled-type order (`—` per missing type, italic "not observed" when empty). Single-expanded accordion → same chip/launcher surfaces, pre-selected, **no tally counts, no +1/−, no Save**; every tap writes through immediately |

### Modified — `androidApp/`

| File | Change |
|---|---|
| `ui/RangeSessionViewModel.kt` | + `measurementPreferencesRepository` + `autoCommitDelayMillis` ctor params (defaulted; factory updated). UiState additions: `observationsByStep: Map<Int, Observation>`, `stagingByBlock: Map<Int, Map<String, String>>`, `armingBlockIndex: Int?`, `handedness: Handedness` (default RIGHT). Load observations (recorder) + handedness (preferences repo, failure → RIGHT) in `loadSession` for v3. New: `stageObservation(blockIndex, typeId, value)` (toggle semantics + arm scheduling), `commitBall(blockIndex)` (both commit paths land here → P2 method; optimistic counter+tally+staging-clear, revert on failure), `updateBallObservation(stepIndex, typeId, value?)` (edit-sheet write-through: build full map from current observation ± the toggled value → `recordObservation`; optimistic + revert). `decrementBlock` branches: v3 → `uncompleteStepsVoidingObservations` (optimistic steps+observations removal), v1/v2 → existing repository path untouched. Increment for v3 ball-taps routes through `commitBall`; the action-only "Done" tap keeps the plain completion path |
| `ui/components/ExecutionBlockPage.kt` | `BlockCounter` gains the capture stack between readout and button row (props: enabled types, tallies, staging, arm flag, handedness, denominators, callbacks, read-only flag) + counter bump animation + haptic on commit event. Ball-instruction rows: trailing `›` + `onClick` → edit sheet when capture is enabled and the instruction has ≥1 completed Ball Step. Thread new props |
| `ui/screens/RangeSessionScreen.kt` | Thread new state/callbacks through both layouts into `ExecutionBlockPage`; host `BallEditSheet` + grid-dialog state (which dialog/sheet is open is screen-local `rememberSaveable` state; *what they show* comes from UiState) |
| `ui/RangeworkApp.kt` | Wire `measurementPreferencesRepository` into `RangeSessionViewModel.factory` |
| `test/.../RangeSessionViewModelTest.kt` | Fake recorder gains the new method; new cases per Behaviour specs below |

## Behaviour specs

### Staging and commit (Active, v3, enabled types ≥ 1)

- Stage/un-stage/move selection per chip; grid pick stages and closes the dialog.
- When the staged set covers every enabled type: `armingBlockIndex = blockIndex`, staged
  surfaces flash primary, input for that block locks; after `autoCommitDelayMillis` the ball
  commits. (Arm is not cancellable — matches prototype; the correction path is the edit sheet.)
- Commit (either path): targets = `incrementTargets`; optimistic = completed indices + targets,
  `observationsByStep[ballStep] = Observation(ballStep, staging)` when staging non-empty,
  staging cleared, arm cleared, bump+haptic. Persist via `completeStepsRecordingObservation`.
  Failure/rejection: revert **all** optimistic pieces (staging is *not* restored — the tap
  meaning "count one ball" failed wholesale; the user re-taps), snackbar.
- +1 with empty staging on a typed block = today's +1 plus an entry in no map — byte-identical
  to a v3 block with no types.
- Rapid +1 taps: each computes targets from current optimistic state (existing pattern); the
  arm lock only guards the auto-commit window.

### Undo (−, v3)

- Optimistic: segment steps un-completed, their entries dropped from `observationsByStep`.
- Persist via `uncompleteStepsVoidingObservations`; failure reverts both, snackbar.
- Staging untouched (staged = next ball; − = last ball). Trailing-action "Done" undo carries
  no observations — the void is a no-op delete, same call.

### Edit sheet (Active, v3)

- Entry: tap a ball-instruction row with ≥1 completed Ball Step. Sheet lists that
  instruction's completed Ball Steps newest-first; "Ball N" = 1-based position within the
  instruction's Ball Steps in snapshot order.
- Expand a ball → chip rows/launchers pre-selected from `observationsByStep[stepIndex]`.
  Tap = set, re-tap selected = remove (unobserved for that type). Each tap: optimistic map
  update, then `recordObservation(stepIndex, fullNewMap)` (full-map upsert — always send every
  value the ball still carries; an emptied ball upserts `{}`, which is D2-equivalent to no
  row). Failure: revert that ball's map, snackbar.
- Grid launchers open the same dialog in edit mode (`target = stepIndex`); tallies/heatmap in
  the dialog still show block history — only the *staged* marker reflects the edited ball.
- No counts on sheet chips; corrections never touch step completion. Session no longer
  Active (belt-and-braces — the execution screen is Active-only in practice): rows render,
  editors read-only; recorder would reject anyway.

### Handedness

- Loaded once per session load; RIGHT on null repo/failure/absence. Affects: strike dialog
  column order + labels, clubface glyph mirror, shape dialog physical-value mapping + flight
  curves, golfer-term labels (`golferLabel`). Direction and Distance never vary. Changing the
  Settings toggle mid-session applies on next session load — accepted (nobody switches hands
  mid-bucket).

### ViewModel test matrix (the load-bearing additions)

- Stage → auto-commit at full coverage (advance test dispatcher): one recorder call, values on
  the Ball Step, staging cleared. No auto-commit with a type missing or zero types enabled.
- +1 partial commit: values persisted for staged subset; +1 empty commit: recorder called with
  empty map (fake asserts no observation stored).
- Arm window: stage/+1/− during arming are no-ops.
- Commit failure: counter, tallies, and staging all revert; notification set.
- − voids: observations map loses the segment's entries; staging survives; failure restores both.
- Edit write-through: full-map upsert including untouched values; toggle-off removes key;
  emptied ball upserts empty map; failure reverts.
- v2 session: increment/decrement use the plain repository paths; no observation load; no
  staging accepted. Null recorder: capture methods no-op without crash.
- Handedness: loads from preferences; RIGHT fallback.
- Same-unit-twice snapshots: observations keyed by global step index — no collision (fixture).

## Edge cases

- **Ball Step with preceding Action Steps:** +1 sweeps them along (existing); observation
  attaches only to the Ball Step. P2 method finds it via `isBallStep`.
- **Block's pending ball belongs to a later instruction:** capture is block-scoped by design
  (the counter is block-scoped); the sheet is instruction-scoped. No conflict — commit always
  targets the block's next incomplete Ball Step.
- **Finish with values staged:** staged values are UI state on an uncommitted ball — discarded
  on finish. Correct by definition (absent = unobserved); no flush, no prompt.
- **"Complete remaining" at finish:** batch-completed balls carry no observations — they read
  "not observed" everywhere. Honest.
- **Rotation mid-stage:** VM state survives. Process death: staging lost (P5, accepted);
  committed data unaffected.
- **Dialog/sheet open across pager swipe:** dialog and sheet state are screen-local and keyed
  to the block/step they were opened for; a swipe doesn't retarget them.
- **Observation load failure at session open:** capture renders with empty tallies; first
  write repairs nothing silently — surface one snackbar ("Couldn't load observations") and
  keep the counter fully functional (the floor is "counter still works").
- **Success enabled + old stray manualCount:** `BlockSuccessCount` already prefers Derived;
  `BlockResultSection` already suppresses the manual row. No new rival numbers.
- **Action-only block / v3 with zero types:** no capture stack, no tappable instruction rows,
  counter card pixel-identical to Stage 4.

## Validation checklist

- [ ] Recorder: new-method cases above; existing `RangeSessionRecorderTest` untouched-green
- [ ] ViewModel test matrix above
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug` passes
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` passes
- [ ] **Manual device flow (local stack)** — recreate the prototype's five scenarios as real
      sessions:
      **(A)** Direction-only block: stage/un-stage/move, auto-commit flash, counter bump;
      **(B)** all-six-types block with criterion: card height acceptable in hand (the D1
      confirmation), skip a ball with bare +1, verify diverging denominators and `—`/"not
      observed" in the sheet;
      **(C)** criterion-less Strike+Shape: no Success row, Block result note-only;
      **(D)** Strike-only ~20 balls: heatmap magnitude reads, counts in corners, no colour
      judgement anywhere;
      **(E)** switch Settings to left-handed, new session: strike columns/labels mirrored,
      shape labels fixed with mirrored flight curves, Direction identical.
      Plus: − voids (undo an observed ball, re-count it, verify it comes back unobserved);
      edit a mis-tapped ball from the sheet; complete a block and verify read-only tallies +
      live −/sheet; v2 session regression (zero new affordances, decrement works)
- [ ] D1 recorded in `design-review.md`'s decision log after the device build confirms the
      merged card (or the sibling-card fallback is invoked — that's a plan revision, not a
      silent pivot)
- [ ] After merge: **field test #2** — real session, observations on 1–2 blocks (epic gate,
      not a PR blocker)

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Decrement rerouted through the recorder breaks v1/v2 undo | Medium | Branch on `supportsDataCapture`; v1/v2 path is literally the existing code; explicit v2 VM test |
| `BlockCounter` rework disturbs the plain counter (every session renders it) | Medium | Capture stack is one conditional child; zero-types path asserted pixel-logic-identical in VM/screen assertions; manual v2 flow |
| Commit's two-write nature surfaces as half-states under flaky network | Medium | P2 puts ordering in shared with tests; the only reachable partial state (completed-unobserved) is legal by design |
| Arm-window lockout feels laggy or eats taps at the range | Low–Medium | 300ms matches prototype; delay is a ctor param — trivially tunable after field test #2 |
| `ModalBottomSheet`/`Dialog` inside the pager: gesture and IME interplay | Low | Both are window-level surfaces (not pager children); verify swipe-while-open in manual flow |
| Glyph geometry drifts from the prototype vocabulary | Low | Port coordinates from the SVGs verbatim; side-by-side visual check against `prototype.html` in the manual flow |
| ViewModel ctor/factory change ripples through tests and wiring | Certain (mechanical) | Defaulted params keep old call sites compiling (Stage 4 precedent) |
| Scope creep toward Stage 6 (summaries, provenance labels) or the tripwire | Medium | History detail untouched; tripwire restated in Fixed rules; review against design-review.md's "not authoritative" list |
