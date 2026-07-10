# Stage 2: Shared Models & Data Layer — changes

**Status:** implemented 2026-07-10. `:shared` + `:androidApp` debug/release unit tests, `:androidApp:assembleDebug`, and `:shared`/`:androidApp` `lintDebug` all pass.

## What shipped

All Kotlin shared-module infrastructure for the feature, per the plan. No Android UI, MCP, or SQL changes.

### D4 decision taken as recommended — physical target-line frame

Implemented the plan's recommendation: stored value strings describe what the ball/club physically did, in a player-independent frame. Handedness affects **rendering geometry and golfer-term labels only**, never stored values. Encodings:

- **Strike Location** — `{row}_{column}` over `{high|middle|low}` × `{heel|center|toe}` (9 anatomical, handedness-neutral values).
- **Shape** — `{start}_{curve}` over `{left|straight|right}` × `{left|straight|right}` (9 physical values); draw/fade/pull/push derived at display time.
- **Direction** — `way_left … way_right` physical of the target line (reads identically for both handednesses; only derived commentary needs handedness — pruning the "direction chips are perspective-dependent" Stage 5 behaviour, as the plan flagged).

> This freezes the encoding. No observation data exists until Stage 5 UI ships, so it remains cheap to revisit before then, but the models are fixed here. If the owner prefers the rejected player-relative frame, only the transform *locations* move (capture-time mirror instead of render-time derivation).

### New files (`shared/src/commonMain`)

- `model/ObservationCatalog.kt` — `ObservationType` enum (wire ids via `@SerialName` + `id`), value vocabularies (`SuccessValue`, `ContactValue`, `DistanceValue`, `DirectionValue`, `StrikeLocation` = `StrikeRow`×`StrikeColumn`, `ShapeFlight` = `ShapeDirection`×`ShapeDirection`), `Handedness`, `fromId`/`accepts`/`vocabulary` helpers.
- `model/ObservationRendering.kt` — strike/shape grid display-column mirrors (involutions; RIGHT = identity) + `curveLabel`/`startLabel`/`golferLabel` derivation.
- `model/Observation.kt` — `Observation(stepIndex, values)` with vocabulary-checked `value(type)`.
- `model/BlockResult.kt` — `BlockResult(note, manualCount)`, camelCase to match `block_results` JSONB; `isEmpty`.
- `model/ObservationTallies.kt` — `TypeTally`, `BlockSuccessCount` (`Derived`/`Manual`/`None`), `ExecutionBlock.typeTally`/`typeTallies`/`successCount` with the tally-hygiene + provenance rules.
- `model/RangeSessionRecordingRules.kt` — `RangeSessionState`, freeze-matrix predicates, `RecordingRejection`, and `validateSessionNoteEdit`/`validateBlockNoteEdit`/`validateManualCountEdit`/`validateObservationWrite`.
- `recording/RangeSessionRecorder.kt` — interface + `RecordingResult`.
- `recording/DefaultRangeSessionRecorder.kt` — guarded domain layer over the repository (mirrors `DefaultPracticeLibrary`).

### Modified files

- `model/RangeSession.kt` — `+ sessionNote`, `+ blockResults`, `supportsDataCapture` (member `snapshotVersion >= 3`).
- `model/RangeSessionSnapshot.kt` — `SnapshotUnit + successCriterion, observationTypes` (raw strings); `enabledObservationTypes` typed accessor (drops unknowns, filters `SUCCESS` without criterion).
- `model/PracticeUnit(+Draft)` — `+ successCriterion`.
- `model/PracticeSessionItem(+Draft)` — `+ observationTypes: List<ObservationType>`.
- `model/MeasurementPreferences.kt` — `+ handedness`.
- `model/DraftValidation.kt` — criterion blank→null; observation-type dedupe in catalog order; **`MeasurementPreferences.validated()` switched from static presets to `copy(...)`** so handedness survives (the named regression risk).
- `model/ValidationIssue.kt` — `+ ValidationTarget.ItemObservationTypes(index)`.
- `library/DefaultPracticeLibrary.kt` — success-requires-criterion session-save validation (friendly mirror of the RPC backstop); duplicate/restore carry the new fields.
- `library/editor/PracticeDraftEditor.kt` — new validation target routed to the item's unit-error slot as nearest fit (no dedicated UI slot until Stage 3).
- `repository/RangeSessionRepository.kt` + `data/SupabaseRangeSessionRepository.kt` — `saveSessionNote`, `saveBlockResult` (read-merge-write; empty removes key), `listObservations`, `upsertObservation` (`onConflict = "range_session_id,step_index"`), `deleteObservations` (`isIn`); row/update DTOs.
- `data/SupabasePracticeUnitRepository.kt` — `success_criterion` row + `p_success_criterion` param.
- `data/SupabasePracticeSessionRepository.kt` — `observation_types` row (→ typed, drops unknowns) + param (wire ids).
- `data/SupabaseMeasurementPreferencesRepository.kt` — `handedness` both ways.
- `data/InMemoryPracticeUnitRepository.kt` / `InMemoryPracticeSessionRepository.kt` — carry new draft fields.
- `data/DataFoundation.kt` — `+ rangeSessionRecorder`, wired to `DefaultRangeSessionRecorder`.
- Test fakes/stubs (`RangeSessionUseCaseTest`, `RangeSessionViewModelTest`, `PracticePlannerViewModelTest`) gained the five new repository members; the shared fake got an in-memory observations/block-results implementation.

### New tests

`ObservationCatalogTest`, `ObservationRenderingTest`, `ObservationTalliesTest`, `RangeSessionRecordingRulesTest`, `recording/RangeSessionRecorderTest`, `SnapshotV3DecodingTest`, plus additions to `DraftValidationTest` (handedness preservation, criterion normalization, type dedupe) and `PracticeLibraryTest` (success-requires-criterion).

## Deviations from plan

- **Snapshot v3 fixture is hand-written**, not captured from the live RPC (no local stack available in this environment). It matches the Stage 1 documented shape and covers the tolerance rules (unknown type id dropped, success-without-criterion filtered, v2 → defaults). The plan's "integration smoke against local stack" checklist item is **not** exercised here and remains for local validation before merge.
- `RecordingRejection` lives in the `model` package (beside the rules) rather than `recording`, keeping the dependency direction `recording → model`. `RecordingResult` stays in `recording`.
- Missing-session in the recorder throws (matching the existing repository `requireNotNull` style) rather than returning a rejection — there is no rejection reason for it and callers always hold a valid id.

## Notes for later stages

- Stage 3 (authoring UI) needs no `shared/` changes: criterion on unit drafts, observation types on item drafts, handedness on preferences, and the save-path validation are all in place. It will want a dedicated per-item observation-type error slot in `PracticeSessionItemDraftInput` (currently routed to `unitError`).
- Direction is **not** a perspective-dependent render surface under the shipped D4 frame — Stage 5 can drop that behaviour.
