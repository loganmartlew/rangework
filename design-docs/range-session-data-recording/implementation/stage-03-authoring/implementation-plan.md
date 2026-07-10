# Stage 3: Authoring & Settings UI

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md)
**Stage 1 record:** [`../stage-01-schema/implementation-plan.md`](../stage-01-schema/implementation-plan.md) (approved — wire names fixed)
**Stage 2 record:** [`../stage-02-models/changes.md`](../stage-02-models/changes.md) — **implemented 2026-07-10 on this branch.** Models, vocabulary, validation rules, and the shared draft/library plumbing referenced below are defined there; the scope note below is reconciled against the code that actually shipped.
**Status:** draft — Stage 2 code is present on this branch, so the shared touchpoints are no longer
speculative: the scope note below records their verified state (one delivered, one partial, one still
outstanding). No owner decision gate was flagged for this stage in the epic.

## Objective

Give the three authoring inputs a UI, so that by Stage 4 real sessions can carry capture
configuration:

1. **Success Criterion** field on the Practice Unit editor, with the "editing = new baseline"
   framing carried in field copy.
2. **Observation Type picker** per Session Item in the session builder — Success offered only
   when the item's unit has a criterion; zero types is the default and stays frictionless.
3. **Handedness** toggle in Settings preferences.

Plus the minimal read surfaces that make authored values visible without opening an editor:
criterion on the Unit detail screen, enabled types on the Session detail item cards. (Small,
included deliberately: authoring you can't see back is half a feature. Trimmable if review
disagrees.)

No block-screen, capture, or history changes — Stage 4/5/6 own those. No MCP changes (Stage 7).

## Dependencies

- **Stage 2 present on this branch** — this stage consumes `ObservationType`
  (`model/ObservationCatalog.kt`), `Handedness`, `PracticeUnitDraft.successCriterion`,
  `PracticeSessionItemDraft.observationTypes`, `MeasurementPreferences.handedness`, the
  `DefaultPracticeLibrary` success-requires-criterion validation issue, and
  `ValidationTarget.ItemObservationTypes`. All confirmed shipped (see scope note for the draft-input
  plumbing that Stage 2 left to this stage).
- Existing UI patterns: `MoreOptionsExpander`, `SettingsScreen` segmented-button preference rows,
  `PracticeDraftEditor` review/place cycle, optimistic-save flow in `PracticePlannerViewModel`.

## Scope note — shared draft-editor gap (verified against shipped Stage 2)

The authoring UI needs three shared touchpoints. Stage 2's plan claimed "Stage 3 should not need
to touch `shared/` at all"; that held for only one of the three. Now that Stage 2 is on this
branch, here is each one's **verified** state (confirmed by reading the shipped files, not the
Stage 2 plan):

1. **`library/editor/PracticeDraftInputs.kt` — outstanding, Stage 3 owns it.** Neither input model
   was touched: `PracticeUnitDraftInput` has no `successCriterion`; `PracticeSessionItemDraftInput`
   has no `observationTypes` and no dedicated error slot. `withoutErrors()` on both is unaware of
   the new fields. This is the plumbing the Compose screens bind to, and it does not exist yet.
2. **`library/editor/PracticeDraftEditor.kt` — partially delivered.** Stage 2 added
   `ValidationTarget.ItemObservationTypes` and satisfied both exhaustive `when` blocks
   (`placeUnitErrors` has the not-applicable branch). **But** `placeSessionErrors` routes the
   observation-type issue onto the item's `unitError` as a deliberate temporary "nearest fit"
   (code comment: *"No dedicated observation-type slot yet (Stage 3 UI)"*), and
   `parseUnitInput` / `parseSessionInput` do **not** carry `successCriterion` / `observationTypes`
   into their drafts — they cannot, since the input models above lack the fields. Stage 3 wires the
   parse and **re-routes** the placement from `unitError` to the new `observationTypesError` slot.
3. **`library/DefaultPracticeLibrary.kt` duplicate flows — delivered, no Stage 3 work.**
   `duplicateUnit` copies `successCriterion`; `duplicateSession` copies each item's
   `observationTypes`. Stage 2 also shipped the success-requires-criterion save validation and the
   `restore` carry. Verify only; do not re-touch.

**Net for Stage 3:** #1 is a fresh addition, #2 is one wiring + one re-route of existing code, #3
is done. The three-way split is why the files table below marks each row with its verified status
rather than the old blanket "(contingent)". Tests for the surviving shared work land with this
stage.

## Fixed in this plan (no owner decision needed)

The epic flags no Stage 3 decision points. These are the UI calls, made here so review can veto
cheaply:

- **Criterion field placement:** inside the Unit editor's existing notes/focus
  `MoreOptionsExpander`, after Focus cue. Keeps the criterion optional-feeling and the editor's
  reorderable header count untouched. The expander's `hasContent` and label logic extend to
  include it. The "new baseline" framing is field copy, not a dialog — supporting text:
  *"What counts as a successful ball. Changing it later starts a new baseline for counts."*
- **Observation Type picker placement:** a labeled chip group (`FilterChip`s in a `FlowRow`)
  inside each Session Item card's existing "More options" expander, below Focus cue. Zero types
  stays the frictionless default (collapsed, nothing to skip); `hasMoreOptions` extends to
  include a non-empty selection so a configured item shows its dot.
- **Picker visibility:** rendered only when the item has a unit selected **and** that unit has at
  least one ball-bearing instruction (`derivedBallCount > 0`). Action-only units never offer
  observations (Stage 1 edge-case note: "app never offers it"). No unit selected → no picker.
- **Success chip gating:** when the selected unit has no criterion, the Success chip renders
  disabled with supporting text *"Add a success criterion to the unit to record hit/miss."* It is
  gating, not hiding — discoverability is how the criterion field gets found.
- **Unit-switch rule:** changing an item's unit strips `SUCCESS` from its selection when the new
  unit lacks a criterion (other types survive the switch). Silent strip is acceptable because the
  chip row is visible right below the unit field — the state change is on-screen.
- **Type labels (presentation, androidApp-side):** Success, Strike location, Contact, Shape,
  Distance, Direction — via an `observationTypeLabel(type)` helper in `PlannerFormatting.kt`.
  Chips render in `ObservationType` catalog order regardless of toggle order.
- **Handedness row:** a "Handedness" segmented row (Right / Left) in Settings → Preferences,
  after Speed, following the Distance/Speed pattern exactly (optimistic save + rollback via
  `saveMeasurementPreferences`, disabled when `isWorking` or `!dataConfigured`). No restart, no
  explanation copy — it does nothing user-visible until Stage 5 renders grids; that is fine.
- **Detail display:** Unit detail shows the criterion as an `EntryHighlightCard`
  ("Success criterion") after notes/focus; Session detail item cards get one quiet line
  ("Observing: Shape, Strike location") beside the existing focus-cue/notes treatment. No pills,
  no color — authoring echo, not analytics.

## Likely files

### Shared — remaining work after Stage 2 (see scope note for each row's verified status)

| File | Status | Change |
|---|---|---|
| `library/editor/PracticeDraftInputs.kt` | **outstanding** | `PracticeUnitDraftInput` + `successCriterion: String = ""`; `PracticeSessionItemDraftInput` + `observationTypes: List<ObservationType> = emptyList()` + `observationTypesError: String? = null`; extend both `withoutErrors()` to clear the new slot (dirty-tracking depends on it) |
| `library/editor/PracticeDraftEditor.kt` | **partial** | `parseUnitInput` / `parseSessionInput` carry the new fields into drafts (not wired yet); **re-route** `placeSessionErrors`' existing `ItemObservationTypes` branch from `item.copy(unitError = …)` (Stage 2's temporary nearest-fit) to `item.copy(observationTypesError = …)`. The exhaustive-`when` branches and `placeUnitErrors`' not-applicable case already exist — do not re-add them |
| `library/DefaultPracticeLibrary.kt` | **done in Stage 2** | Verify only: `duplicateUnit` copies `successCriterion`, `duplicateSession` copies each item's `observationTypes`. No edit expected |
| `commonTest/.../editor/PracticeDraftEditorTest.kt` (or nearest) | new | Round-trip of new fields through review; `ItemObservationTypes` now lands on `observationTypesError`, not `unitError` |
| `commonTest/.../library/PracticeLibraryTest.kt` | verify/extend | Confirm the Stage 2 duplicate-carries-fields coverage exists; add it if the shipped test only covered success-requires-criterion |

### Modified — `apps/mobile/androidApp/src/main/java/.../android/`

| File | Change |
|---|---|
| `ui/PracticePlannerViewModel.kt` | + `updateUnitSuccessCriterion(String)`; + `toggleSessionItemObservationType(index, ObservationType)` (toggles membership, clears `observationTypesError`); `updateSessionItemUnit` strips `SUCCESS` when the new unit lacks a criterion; `toEditorState()` mappers (unit, session item) and `buildOptimisticUnit` / `buildOptimisticSession` carry the new fields |
| `ui/PlannerFormatting.kt` | + `observationTypeLabel(ObservationType): String` |
| `ui/screens/UnitEditorScreen.kt` | Criterion `OutlinedTextField` inside the notes/focus expander (+ `onUpdateSuccessCriterion` param); expander `hasContent`/label include it. **Do not add a new top-level LazyColumn item** — the reorderable `headerCount = 5` depends on the item count above the instructions |
| `ui/screens/SessionEditorScreen.kt` | `ObservationTypePicker` in `SessionItemEditorCard`'s More options (+ `onToggleObservationType(Int, ObservationType)` param); visibility + gating rules above; renders `observationTypesError` as supporting text; `hasMoreOptions` includes non-empty types |
| `ui/components/ObservationTypePicker.kt` (**new**) | `FlowRow` of `FilterChip`s over `ObservationType.entries` in catalog order; `successEnabled: Boolean` + disabled supporting text; selection from the item's `observationTypes` |
| `ui/screens/UnitDetailScreen.kt` | Criterion `EntryHighlightCard` when present and non-blank |
| `ui/screens/SessionDetailScreen.kt` | "Observing: …" line on item cards with non-empty types |
| `ui/SettingsViewModel.kt` | + `selectHandedness(Handedness)` — `copy(handedness = ...)` of **current** prefs (never a preset — the preset-clobber bug Stage 2 fixes must not be reintroduced here), then `saveMeasurementPreferences` |
| `ui/screens/SettingsScreen.kt` | Handedness segmented row in Preferences (+ `onSelectHandedness` param) |
| `ui/RangeworkApp.kt` | Wire the three new callbacks through to the screens |

### Modified — tests, `apps/mobile/androidApp/src/test/java/.../android/ui/`

| File | Change |
|---|---|
| `PracticePlannerViewModelTest.kt` | Criterion edit → save → editor round-trip; toggle types on an item; unit-switch strips `SUCCESS` (and only `SUCCESS`); library `Invalid` for success-without-criterion lands on the item's error slot; duplicate keeps fields; dirty flag stays false after error placement + `withoutErrors()` |
| `SettingsViewModelTest.kt` | `selectHandedness` optimistic save, rollback on failure, and preservation of distance/speed units (the no-preset rule) |

No SQL changes; no MCP changes; no block-screen (`RangeSessionScreen` / `ExecutionBlockPage`)
changes.

## Behaviour specs

### Success chip eligibility (evaluated live in the editor)

| Selected unit | Success chip |
|---|---|
| None selected | picker hidden entirely |
| Action-only (0 balls) | picker hidden entirely |
| Ball-bearing, no criterion | shown, **disabled**, supporting text points at the unit editor |
| Ball-bearing, has criterion | shown, enabled |

Eligibility reads the criterion from the loaded `units` list (saved models — already
blank-normalized), not from any editor draft.

### Validation (backstops, in order)

1. Chip gating prevents enabling `SUCCESS` without a criterion in the normal flow.
2. Unit-switch strip prevents carrying it onto a criterion-less unit.
3. `DefaultPracticeLibrary.saveSession` (Stage 2) returns a validation issue when a stale
   selection slips through (criterion removed from the unit after the item was configured) —
   placed on the item card via `placeSessionErrors`, message asks the user to remove hit/miss
   or add a criterion.
4. The `save_practice_session` RPC exception (Stage 1) remains the atomic backstop; it surfaces
   through the generic save-failure status if ever reached.

### Handedness

Pure preference write. `SettingsUiState.measurementPreferences.handedness` drives the segmented
selection; save failure rolls back to the previous value (existing pattern). No other screen
reads it until Stage 5.

## Edge cases

- Criterion edited on a unit used by existing sessions/snapshots: by design, nothing recomputes —
  snapshots baked the old value; the field copy carries the framing. No warning dialog.
- Criterion cleared on a unit whose saved session enables `SUCCESS`: session stays saved
  (historically valid, per Stage 1); next edit of that session shows the Success chip selected
  and (now) disabled-styled? — No: chip stays enabled-looking since selected; save triggers
  backstop 3 and the error lands on the item. Re-tapping the chip to deselect clears it.
- Whitespace-only criterion input: draft normalization (Stage 2 `validated()`) stores null;
  editor round-trips the raw string until save, detail screens hide blank.
- Duplicate unit/session: fields carried (shared duplicate flows above); duplicated session items
  keep types, still subject to live gating on next edit.
- Item reorder / repeat-count changes: `observationTypes` rides the item state through
  `reindexedSessionItems` untouched (data-class copy semantics — asserted in the VM test).
- Dirty tracking: `withoutErrors()` clears `observationTypesError`, so a failed save doesn't make
  a clean editor read dirty; new fields participate in baseline equality automatically.
- Data-unconfigured build: unit/session editors already gate on `dataConfigured`; the handedness
  row disables like Distance/Speed and the existing "not available in this build" copy covers it.
- Left-handed selection before Stage 5 ships: stored, no visible effect — acceptable and expected
  (canonical storage per Stage 2 D4; nothing renders handedness yet).

## Validation checklist

- [ ] Shared draft plumbing completed against shipped Stage 2 code: input models gain the new
      fields + `observationTypesError`; parse carries them; `placeSessionErrors` re-routed off
      `unitError` onto `observationTypesError`; duplicate flows (already delivered) still carry them
- [ ] Unit editor: enter criterion → save → reopen → present; blank → saved as absent; unit
      detail shows/hides it correctly
- [ ] Instruction drag-reorder in the unit editor still lands on the right rows (headerCount
      guard)
- [ ] Session editor: picker hidden with no unit and for action-only units; all six chips in
      catalog order; Success disabled without criterion, enabled with; selection saves and
      round-trips; "Observing" line on session detail
- [ ] Unit-switch strips `SUCCESS` only; other selections survive
- [ ] Stale-success save → error message on the item card (not a toast-only failure), deselect →
      save succeeds
- [ ] Settings: handedness toggles, persists across app restart, rolls back on save failure, and
      leaves distance/speed units untouched
- [ ] Start a range session from a configured session on the local stack → snapshot v3 unit
      entries carry `successCriterion` + `observationTypes` (end-to-end proof the authoring UI
      feeds Stage 1's RPC)
- [ ] Existing units/sessions (no criterion, no types) edit and save exactly as before
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug` passes
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` passes

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Unit editor reorderable `headerCount` breaks if the criterion becomes a new top-level item | High if the placement rule is ignored | Placement fixed in plan (inside the existing expander item); checklist has a drag test |
| `placeSessionErrors` left on Stage 2's temporary `unitError` route → observation errors surface on the wrong field | Medium (the nearest-fit code is already there and compiles) | Re-route called out explicitly in the files table; VM + editor tests assert the error lands on `observationTypesError` |
| Duplicate flows silently dropping criterion/types | Low (Stage 2 already delivered the copies) | Verify-only row in the files table; checklist re-confirms the round-trip |
| `selectHandedness` built from a preset clobbers distance/speed | Medium (the pattern exists in `validated()` today) | No-preset rule stated at the change site; dedicated test |
| `placeSessionErrors` exhaustive `when` left unhandled for the new target → issues vanish as toast-only | Low (compiler forces a branch; risk is choosing the discard branch) | Behaviour spec names the placement; VM test asserts the item-level slot |
| Editor dirty-flag false positives from the new error slot | Low | `withoutErrors()` extension named in files table; VM test covers |
| Success gating read from draft state instead of saved units → flicker/incorrect gating while editing a unit elsewhere | Low | Eligibility source fixed in spec (loaded `units` list) |
