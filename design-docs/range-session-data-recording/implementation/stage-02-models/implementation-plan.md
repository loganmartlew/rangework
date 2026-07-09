# Stage 2: Shared Models & Data Layer

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md)
**Stage 1 record:** [`../stage-01-schema/implementation-plan.md`](../stage-01-schema/implementation-plan.md) (approved 2026-07-09 — column names, vocabulary ids, and snapshot v3 shape referenced below are fixed there)
**Status:** draft — awaiting owner review of D4 (canonical handedness encoding); everything else follows mechanically from Stage 1 and the design

## Objective

Build every piece of Kotlin shared-module infrastructure the feature needs, so Stages 3–6 are
pure UI work: Observation Type vocabulary and value encodings, snapshot v3 models, Block
Result / Session Note / Observation models, handedness preference, authoring plumbing (draft
fields, validation, save-RPC params), the recording repository surface and its Supabase
implementation, a guarded recording layer that owns the mutability and count-provenance
invariants, derived-count/tally computation, and handedness rendering transforms. No Android
UI changes; no schema changes (Stage 1 shipped them all).

Unit tests are the bulk of this stage — the freeze matrix and count-provenance rules are the
design's load-bearing invariants and they live here, not in UI conditionals.

**Scope note:** the epic's Stage 3 ("Authoring & Settings UI") and Stage 4 ("Prose capture")
are UI-only stages that "consume shared use cases" from this one. This plan therefore
includes the *authoring data plumbing* (criterion on unit drafts, observation types on item
drafts, handedness on preferences, save-RPC wiring) as well as the recording layer — Stage 3
should not need to touch `shared/` at all.

## Dependencies

- **Stage 1 merged** — the migration and RPCs exist; a snapshot v3 fixture can be captured
  from the local stack for deserialization tests.
- Existing shared patterns: `PracticeLibrary`/`DefaultPracticeLibrary` (guarded domain layer
  over thin repos), `DraftValidation` (`validated()` + `ValidationIssue`), `DataFoundation`
  wiring, `kotlinx.serialization` row DTOs in `Supabase*Repository`.
- Wire values fixed by Stage 1: type ids `success`, `strike_location`, `contact`, `shape`,
  `distance`, `direction`; `block_results` JSONB keyed by unitIndex-as-string with camelCase
  fields (`note`, `manualCount`); `range_session_observations` rows with `step_index` +
  `observed_values` (map of type id → opaque value string); `handedness` `'RIGHT'|'LEFT'`;
  `session_note` column; snapshot unit keys `successCriterion` + `observationTypes`.

## D4 — Canonical encoding for handedness-sensitive values (owner decision)

The one decision the epic flags for this stage: what the stored value strings *mean* for
Shape, Direction, and Strike Location, given the design's rule "stored values are canonical;
rendering flips". This is the encoding that is expensive to change once observation data
exists.

### Recommendation: physical target-line frame; handedness affects rendering geometry and golfer-term labels only

Store what the ball and club physically did, in a frame that does not depend on the player:

| Type | Stored values | Handedness affects |
|---|---|---|
| **Strike Location** | `{high\|middle\|low}_{heel\|center\|toe}` (9 values, e.g. `high_heel`, `middle_center`) — club anatomy, inherently handedness-neutral | Grid *layout* only: columns mirror so screen positions match the face as the player looks down at it |
| **Shape** | `{start}_{curve}` where start ∈ `left\|straight\|right` (start line vs target line) and curve ∈ `left\|straight\|right` (curvature direction) — 9 values, e.g. `straight_right` = started straight, curved right | Grid layout mirrors; golfer-term labels (draw/fade/pull/push) are *derived at display time* from value + handedness |
| **Direction** | `way_left`, `left`, `on_line`, `right`, `way_right` — physical left/right of target line | Nothing structural — a lefty's miss left of target is physically left. Only derived commentary ("pull side") needs handedness |

Non-perspective types, fixed regardless of D4: Success `hit|miss`; Contact
`very_fat|fat|flush|thin|very_thin`; Distance `way_short|short|on|long|way_long`.

**Why the physical frame:**

1. **Stored data never depends on a mutable preference.** Handedness is a settings toggle.
   In the physical frame, flipping it (typo correction, shared device, whatever) changes only
   how grids are laid out and which golfer terms are printed — no stored row silently changes
   meaning. In a player-relative frame, every historical Shape/Direction value would
   reinterpret physically on toggle.
2. **Direction's designed labels are already physical.** The design fixed the user-facing
   values as "Way Left … Way Right". In the physical frame, stored value and displayed label
   agree verbatim for both handednesses. In a player-relative frame, a left-handed player's
   stored `left` would sometimes have to render as "Right" — a standing invitation for
   mirror-image bugs.
3. **Strike Location is already anatomical.** Heel and toe are club anatomy, identical for
   left- and right-handed clubs. The physical frame extends that same "describe the thing,
   not the player" rule to the other two types.
4. **The MCP cost is small and pays once.** The coach model wants golfer terms ("fade"). In
   the physical frame those are a pure function of value + handedness; Stage 7 either includes
   handedness in responses or (better) has the tools emit derived labels alongside raw values.
   That is one read-time mapping in one place, versus a capture-time mirror transform on every
   write and a mirror-back on every render.

**Consequence to note:** under this frame Direction chips are *not* actually
perspective-dependent — they read Way Left → Way Right for everyone. The design listed
direction chips among the handedness-oriented surfaces; with the physical frame that reduces
to derived labeling only. Flagging explicitly since it prunes a Stage 5 rendering behaviour.

### Alternative (rejected): player-relative frame ("as-if-right-handed")

Store golfer-semantic values — a left-handed player's physically-right-curving ball stores as
`draw`; Direction stores pull-side/push-side. Pros: raw MCP payloads are directly coach-legible
without handedness; cross-player semantic stability (irrelevant — single-user product). Cons:
stored meaning depends on the preference at capture time (toggle = silent physical
mirroring of history); Direction's fixed Left/Right labels stop matching storage; every
capture and render crosses a mirror transform. The single-user product gets the costs without
the benefit.

If the owner prefers raw-payload legibility over toggle-safety, the player-relative frame is
implementable with the same file layout — only the transform locations move. Decide before
any observation row exists.

## Fixed in this plan (no owner decision needed)

- **Wire tolerance rule:** snapshot `observationTypes` and `observed_values` keys/values are
  `String` in serialized models; typed accessors map known ids to enums and *drop* unknowns
  instead of failing decode. The DB constrains vocabulary (Stage 1), so unknowns only arise
  from version skew — an old app reading data written by a newer schema must degrade, not
  crash. Enum-typed `@Serializable` fields would throw on unknown constants.
- **Capture gate:** all capture affordance logic keys off `snapshotVersion >= 3` — one
  tested predicate (`RangeSession.supportsDataCapture`), per the epic's "feature detection by
  version". v1/v2 sessions take no notes, results, or observations, full stop (design §8).
- **Recording layer shape:** a `RangeSessionRecorder` interface + `DefaultRangeSessionRecorder`
  in a new `recording/` package, mirroring the `library/` pattern (guarded domain layer
  composing the thin repository). `DataFoundation` exposes the recorder *alongside* the
  existing raw `rangeSessionRepository` (execution UI keeps using the repo for
  stepping/finish; capture goes through the recorder). RWK-11 precedent: no per-method
  use-case classes.
- **Tally hygiene rule:** derived counts and per-type denominators count only observations
  whose `stepIndex` is a **completed Ball Step of the block**, and only values inside the
  type's vocabulary. This makes a stray observation row (e.g. a −1 sweep whose delete
  half-failed) harmless to every consumer — deletion is hygiene, not correctness.
- **Empty-record equivalence** (Stage 1 D2): no row and an `{}` row both mean unobserved;
  tallies treat them identically.

## Likely files

### New — `apps/mobile/shared/src/commonMain/kotlin/.../shared/`

| File | Purpose |
|---|---|
| `model/ObservationCatalog.kt` | `ObservationType` enum (wire ids via `@SerialName`-style `id` field), per-type value vocabularies (`SuccessValue`, `ContactValue`, `DistanceValue`, `DirectionValue`, `StrikeLocation` (row × column), `ShapeFlight` (start × curve)), `Handedness` enum, parse helpers (`fromId(...): T?` — null on unknown), canonical value-string round-trips |
| `model/ObservationRendering.kt` | Handedness transforms: mirrored column/order helpers for the strike and shape grids, golfer-term label derivation (draw/fade/pull/push from `ShapeFlight` + `Handedness`). Pure functions — Stage 5/6 consume |
| `model/Observation.kt` | `Observation(stepIndex: Int, values: Map<String, String>)` domain model + typed accessors (`value(type): String?`, vocabulary-checked) |
| `model/BlockResult.kt` | `BlockResult(note: String? = null, manualCount: Int? = null)` — serializable, camelCase matches the `block_results` JSONB shape |
| `model/ObservationTallies.kt` | Pure derived-count computation: per-type `TypeTally(observedCount, valueCounts)` for a block; `BlockSuccessCount` sealed type (`Manual(count, totalBalls)` / `Derived(hits, observed)` / `None`) + the provenance selection rule |
| `model/RangeSessionRecordingRules.kt` | `RangeSessionState` (ACTIVE/COMPLETED/ABANDONED) derivation; the freeze matrix as predicates (`canEditSessionNote`, `canEditBlockNote`, `canEditManualCount`, `canEditObservations`); manual-count validation (criterion required, Success not enabled, `0..totalBalls`, ball-bearing block, v3 gate); observation validation (Active, v3, Ball Step, keys ⊆ the unit's enabled types, values in vocabulary) |
| `recording/RangeSessionRecorder.kt` | Interface: `saveSessionNote`, `saveBlockNote`, `saveManualCount`, `recordObservation` (upsert), `voidObservations`, `uncompleteStepsVoidingObservations`, `observations(rangeSessionId)` |
| `recording/DefaultRangeSessionRecorder.kt` | Loads the session, applies `RangeSessionRecordingRules`, delegates to the repository; rejection surfaces as a sealed result (follow `PracticeLibraryResult` conventions) |

### New — `apps/mobile/shared/src/commonTest/kotlin/.../shared/`

| File | Purpose |
|---|---|
| `model/ObservationCatalogTest.kt` | Vocabulary round-trips; unknown id/value tolerance |
| `model/ObservationRenderingTest.kt` | Mirror transforms are involutions; RIGHT is identity; golfer-term labels correct per handedness (the D4 encoding proven in tests) |
| `model/ObservationTalliesTest.kt` | Per-type denominators; incomplete/non-ball/empty/out-of-vocabulary exclusions; provenance selection matrix |
| `model/RangeSessionRecordingRulesTest.kt` | Full freeze matrix (3 states × 4 field kinds); manual-count validation matrix; observation validation |
| `recording/RangeSessionRecorderTest.kt` | Recorder against the extended fake repo: guarded writes, rejections, −1 sweep voiding |
| `model/SnapshotV3DecodingTest.kt` | Fixture captured from the Stage 1 RPC decodes; a v2 fixture still decodes identically (regression) |

### Modified

| File | Change |
|---|---|
| `model/RangeSession.kt` | + `sessionNote: String? = null` (`session_note`), `blockResults: Map<String, BlockResult> = emptyMap()` (`block_results`); `supportsDataCapture` helper |
| `model/RangeSessionSnapshot.kt` | `SnapshotUnit` + `successCriterion: String? = null`, `observationTypes: List<String> = emptyList()`; typed accessor `enabledObservationTypes: List<ObservationType>` (drops unknowns, filters `SUCCESS` when `successCriterion == null` as belt-and-braces beside the RPC filter) |
| `model/PracticeUnit.kt` / `model/PracticeUnitDraft.kt` | + `successCriterion: String? = null` |
| `model/PracticeSessionItem.kt` / `model/PracticeSessionItemDraft.kt` | + `observationTypes: List<ObservationType> = emptyList()` (enum entries carry `@SerialName` wire ids) |
| `model/DraftValidation.kt` | Criterion blank→null normalization in `validated()`; observation-type dedupe with catalog-order normalization; **fix `MeasurementPreferences.validated()`** — the IMPERIAL/METRIC branches currently return static presets and would clobber a new handedness field; switch to `copy(...)` of the unit fields |
| `model/ValidationIssue.kt` | + `ValidationTarget.ItemObservationTypes(index)` (or nearest fit) for the success-requires-criterion issue |
| `model/MeasurementPreferences.kt` | + `handedness: Handedness = RIGHT` (keeping the existing model/repo; the vocabulary already calls this bundle "User Preferences — measurement and presentation settings") |
| `library/DefaultPracticeLibrary.kt` | Session save path: item enabling `SUCCESS` whose referenced unit has no criterion → validation issue (friendly mirror of the RPC exception, which stays as backstop) |
| `repository/RangeSessionRepository.kt` | + `saveSessionNote`, `saveBlockResult(rangeSessionId, unitIndex, result)` (merge semantics; all-null result removes the key), `listObservations`, `upsertObservation`, `deleteObservations(stepIndices)` |
| `data/SupabaseRangeSessionRepository.kt` | Implement the above: `session_note`/`block_results` PATCHes (read-merge-write, `club_overrides` precedent); `range_session_observations` select / upsert (`onConflict = "range_session_id,step_index"`) / delete (`isIn`); row DTOs |
| `data/SupabasePracticeUnitRepository.kt` | `PracticeUnitRow` + `success_criterion`; `SavePracticeUnitParams` + `p_success_criterion` |
| `data/SupabasePracticeSessionRepository.kt` | `PracticeSessionItemRow` + `observation_types`; `SessionItemParam` + `observation_types` (list of wire ids) |
| `data/SupabaseMeasurementPreferencesRepository.kt` | Row/insert DTOs + `handedness`; mapping both ways |
| `data/InMemoryPracticeUnitRepository.kt` / `InMemoryPracticeSessionRepository.kt` | Carry the new draft fields through |
| `data/DataFoundation.kt` | + `rangeSessionRecorder: RangeSessionRecorder`; wire `DefaultRangeSessionRecorder(rangeSessionRepository)` |
| `commonTest/.../RangeSessionUseCaseTest.kt` | Extend `FakeRangeSessionRepository` with the new members (in-memory observations map keyed by step index) |
| `androidApp/.../RangeSessionViewModelTest.kt`, `PracticePlannerViewModelTest.kt` | Their `RangeSessionRepository` fakes/stubs gain the new members (stub bodies) |
| `apps/mobile/CONTEXT.md` | Only if implementation reveals a vocabulary gap — no planned change |

No Android UI, MCP, or SQL changes in this stage.

## Behaviour specs (what the tests pin down)

### Freeze matrix (design "prose is reflection, counts are data")

| | Session Note | Block note | Manual count | Observations (record/edit/void) |
|---|---|---|---|---|
| **Active** | ✔ | ✔ | ✔ | ✔ |
| **Completed** | ✔ | ✔ | ✘ | ✘ |
| **Abandoned** | ✘ | ✘ | ✘ | ✘ |

All four columns additionally gated on `snapshotVersion >= 3`.

### Manual count validation (recorder rejects otherwise)

Requires, for the target block's snapshot unit: non-null `successCriterion`; `success` **not**
in enabled types; block has ≥ 1 Ball Step; `count in 0..block.totalBalls`. `null` clears the
count (subject to the freeze matrix — clearing is still a count edit).

### Provenance selection (`BlockSuccessCount`)

- `success` enabled in the snapshot unit → **Derived(hits, observed)** — even at 0 observed
  ("0 of 0 observed" is honest; presentation may soften it). Manual entry rejected.
- Else criterion present and `manualCount != null` → **Manual(count, totalBalls)**.
- Else **None**. A stored `manualCount` on a success-enabled block (impossible via the
  recorder, conceivable via bad data) resolves as Derived — derived wins, per design §4.

### Observation write rules

- Upsert keyed by step index; keys must be ⊆ the step's unit-entry enabled types; values must
  be in-vocabulary; step must be a Ball Step. Empty `values` map is legal (the bare +1 commit).
- `uncompleteStepsVoidingObservations(stepIndices)`: delete observation rows for the swept
  indices **first**, then `setStepsCompletion(..., false)`. If the second half fails, the
  ball is still complete and merely unobserved (legal state); if only the first half runs,
  tally hygiene ignores any survivor. No cross-call transaction needed.
- Correcting an observation never touches `completed_steps` (separate rows/columns — verified
  by construction, asserted in the recorder test anyway).

## Edge cases

- v2 (and v1) session: `supportsDataCapture` false; every recorder method rejects; snapshot
  decode unchanged (defaults absorb the missing keys).
- Snapshot with `observationTypes` containing an id this app version doesn't know: decode
  succeeds, typed accessor drops it, capture UI (later) simply never offers it.
- `success` in snapshot types but `successCriterion` null (shouldn't occur post-Stage-1 RPC
  filter): typed accessor filters it — derived counting never runs without a rubric.
- Same unit in multiple session items: blocks are keyed by unitIndex (snapshot unit *entry*),
  so results/tallies stay per-block; asserted in tallies test.
- Block with zero Ball Steps (action-only): note allowed, manual count rejected, no
  observation may reference its steps.
- `blockResults` keys outside `0..units.lastIndex` or non-integer (bad data): ignored by
  consumers, preserved on merge-write (repo merges by key, never rewrites the whole map from
  a model that dropped entries — merge test covers this).
- Whitespace-only session/block note → normalized to null → key dropped (matches the RPC's
  blank-criterion handling style).
- Handedness toggle with existing observation data: stored values unaffected (the D4 point);
  rendering transforms are stateless functions of current preference.
- `MeasurementPreferences.validated()` on IMPERIAL preset with LEFT handedness → handedness
  survives (the preset-clobber regression test).

## Validation checklist

- [ ] `ObservationType.fromId` round-trips all six Stage 1 ids; unknown → null
- [ ] Every vocabulary value string round-trips through its typed enum; sets exactly match
      the design catalog (§5) under the D4 encoding
- [ ] Mirror transforms: applying twice = identity; RIGHT = identity; LEFT mirrors columns of
      strike and shape grids; golfer-term labels correct for both handednesses (fade/draw
      cross-checked by hand for LH)
- [ ] Snapshot v3 fixture (captured from the migrated local stack's RPC output) decodes;
      unit entries expose criterion + typed enabled types
- [ ] v2 fixture decodes byte-for-byte to the same model as before this stage
- [ ] `RangeSession` decodes a row carrying `session_note` + `block_results` (and one
      without — defaults)
- [ ] Freeze matrix: all 12 cells asserted through the recorder
- [ ] Manual-count matrix: no criterion / success enabled / out of range / action-only block
      all rejected; happy path accepted; `null` clears
- [ ] Provenance: enabled→Derived, criterion+manual→Manual, neither→None; stray manual on
      success-enabled block resolves Derived
- [ ] Tallies: per-type denominators; excludes incomplete steps, empty records (for that
      type), out-of-vocabulary values; "11 of 18 observed, 20 hit"-shaped fixture reproduced
- [ ] −1 sweep: `uncompleteStepsVoidingObservations` deletes swept balls' observations and
      un-completes; observations on untouched balls survive
- [ ] Draft validation: blank criterion → null; observation-type dedupe + catalog order;
      `SUCCESS` on criterion-less unit → validation issue from `DefaultPracticeLibrary`
- [ ] `MeasurementPreferences.validated()` preserves handedness on IMPERIAL/METRIC
- [ ] Supabase param/row DTOs match Stage 1 names exactly: `p_success_criterion`,
      `observation_types` (item + row), `success_criterion`, `session_note`, `block_results`,
      `range_session_observations` columns, upsert `onConflict = "range_session_id,step_index"`
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug` passes
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` passes
- [ ] **Integration smoke (manual, local stack):** via a scratch test or debug hook — save a
      unit with a criterion and a session enabling types through the app repos; start it;
      decode; upsert + delete an observation row; PATCH a block result and session note

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `MeasurementPreferences.validated()` preset branches silently discard handedness | **High if unfixed** — it's the current behaviour | Named in plan; dedicated test |
| `RangeSessionRepository` interface growth breaks implementers | Certain (3 fakes + Supabase impl) | All four call sites named in the files table; compiler enforces |
| Enum-typed wire fields throw on unknown vocabulary | Medium | Tolerance rule fixed in plan: strings on the wire, typed accessors drop unknowns; decode tests cover it |
| Snapshot/DTO field-name drift vs Stage 1 SQL | Medium | Names copied into the checklist verbatim from the approved Stage 1 plan; fixture captured from the real RPC, not hand-written |
| `block_results` merge-write drops sibling keys (read-modify-write races or model-side filtering) | Low | Merge semantics specified (never rewrite from a filtered model); single-user, same pattern `club_overrides` already accepts |
| D4 encoding wrong → data migration later | Low after review | That is what this plan's owner gate is for; no observation data can exist before Stage 5 ships anyway — but the *models* freeze here, so review now |
| DataFoundation constructor churn breaks androidApp wiring | Low | Additive field; factory wires it; `assembleDebug` in the checklist |
