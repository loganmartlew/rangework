# Stage 5: Per-ball Observation Capture — changes

**Status:** implemented 2026-07-12. Builds green: `:shared` + `:androidApp` debug/release unit
tests, `:androidApp:assembleDebug`, and `:shared:lintDebug` + `:androidApp:lintDebug`.

## What shipped

All six objectives from the plan, matching the file list. One `shared/` touch (P2), the rest
Android UI + ViewModel.

### Shared — the one P2 touch

- `recording/RangeSessionRecorder.kt`, `recording/DefaultRangeSessionRecorder.kt`:
  `completeStepsRecordingObservation(rangeSessionId, stepIndices, values)` — the +1/auto-commit
  mirror of `uncompleteStepsVoidingObservations`. Validates non-empty values *before* any write
  (rejection leaves nothing written), completes the steps, then upserts the ball's Observation on
  the single Ball Step among the targets. Empty values or action-only targets degrade to plain
  completion.
- `recording/RangeSessionRecorderTest.kt`: values-land-on-ball-step, empty-values-no-row,
  bad-value-rejected-writes-nothing, unsupported-snapshot-rejected, action-only-degrades.

### New — `androidApp/.../ui/components/`

- `ObservationGlyphs.kt` — P3 Canvas glyph set (`DirectionGlyph`, `DistanceGlyph`, `ContactGlyph`,
  `FlightGlyph`, `ClubfaceGlyph`, `MiniGridGlyph`). Geometry ported verbatim from the prototype
  SVG viewBoxes; always `LocalContentColor`; flight/clubface mirror by construction for LH.
- `ObservationCaptureSection.kt` — the capture stack inside the counter card: `ObservationChipRow`
  (glyph/label/count segments, header + rubric + denominator) and `GridLauncherRow` (mini-grid ⇢
  value glyph + golfer summary + chevron), fixed row order, staged/arm visual states, read-only
  dimming at block-complete. Pure presentation. Reused by the edit sheet.
- `ObservationGridDialog.kt` — 3×3 picker for Strike/Shape: axis order/labels from the shipped
  rendering transforms, P4 continuous primary-alpha heatmap with corner counts and body glyphs,
  staged border, one-tap-in/one-tap-out.
- `BallEditSheet.kt` — `ModalBottomSheet` scoped to one instruction's completed Ball Steps
  (newest first), value summary with `—`/italic "not observed", single-expanded accordion reusing
  the capture surfaces, write-through, no counts/±/Save.

### Modified — `androidApp/`

- `ui/RangeSessionViewModel.kt` — `+ measurementPreferencesRepository` + `autoCommitDelayMillis`
  ctor params (defaulted; factory updated). UiState: `observationsByStep`, `stagingByBlock`,
  `armingBlockIndex`, `handedness`, `commitSignal`/`committedBlockIndex`. `loadSession` loads
  observations (failure → empty + snackbar) and handedness (failure → RIGHT) for v3. New
  `stageObservation` (toggle + arm scheduling), `commitBall`/`doCommit` (both paths → P2 method,
  optimistic + revert, staging not restored on failure), `updateBallObservation` (edit
  write-through). `incrementBlock`/`decrementBlock` branch v3 vs the untouched v1/v2 paths.
- `ui/components/ExecutionBlockPage.kt` — `BlockCounter` hosts the capture stack between readout
  and buttons + counter-pulse animation + haptic tick (P6, `LongPress` — Compose 1.7 has no
  `Confirm`). Ball-instruction rows become tappable (trailing `›`) into the edit sheet.
- `ui/screens/RangeSessionScreen.kt` — threads capture state/callbacks into `ExecutionBlockPage`;
  hosts the grid dialog + edit sheet with screen-local `rememberSaveable` open-state (what they
  show comes from UiState).
- `ui/RangeworkApp.kt` — wires `measurementPreferencesRepository` into the factory and the two new
  callbacks.
- `test/.../RangeSessionViewModelTest.kt` — fake repo now stores observations; new cases per the
  plan's ViewModel test matrix (auto-commit, partial/empty +1, arm-window no-ops, commit/decrement
  failure revert, edit full-map upsert + toggle-off + empty, v2 regression, null recorder,
  handedness load/fallback, global-step-index keying).

## Deviations from plan

- **Haptic type:** plan specified `HapticFeedbackType.Confirm`; the project's Compose BOM
  (2024.10.01 / Compose 1.7) predates `Confirm`, so `LongPress` is used. Trivially swappable when
  the BOM advances; the exact type is plan-decided per the design review.

## Not done here (plan says so)

- D1 decision-log confirmation in `design-review.md` — needs the physical device build.
- Field test #2 — post-merge epic gate, not a PR blocker.
