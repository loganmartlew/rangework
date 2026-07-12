# Stage 3: Authoring & Settings UI — changes

**Status:** implemented 2026-07-10 (commit `92e9bfd` "Phase 3 done"). Record written
2026-07-13 during epic close-out (the stage shipped without a `changes.md` at the time).
Builds green per the plan's checklist: `:shared` + `:androidApp` debug/release unit tests,
`:androidApp:assembleDebug`, `:shared:lintDebug` + `:androidApp:lintDebug`.

## What shipped

All three authoring inputs plus their read-back surfaces, exactly as the plan fixed them.
17 files, +509/−8 — matching the plan's "Likely files" table with no scope surprises.

### Shared — the draft-editor gap the plan flagged (both remaining items closed)

- `library/editor/PracticeDraftInputs.kt` — `PracticeUnitDraftInput` gained
  `successCriterion`; `PracticeSessionItemDraftInput` gained `observationTypes` +
  `observationTypesError`; both `withoutErrors()` clear the new slot (dirty-tracking).
- `library/editor/PracticeDraftEditor.kt` — `parseUnitInput` / `parseSessionInput` now carry
  the new fields into drafts, and `placeSessionErrors`' `ItemObservationTypes` branch was
  **re-routed** off Stage 2's temporary `unitError` nearest-fit onto the new
  `observationTypesError` slot (the plan's key correctness call).
- `library/DefaultPracticeLibrary.kt` — verified only; Stage 2's duplicate-carries-fields and
  success-requires-criterion validation were already in place, no edit needed.

### Android — authoring UI

- `ui/components/ObservationTypePicker.kt` (**new**, 93 lines) — `FlowRow` of `FilterChip`s
  over `ObservationType.entries` in catalog order; Success chip disabled with pointer copy when
  the unit lacks a criterion.
- `ui/screens/UnitEditorScreen.kt` — Success Criterion field inside the existing notes/focus
  `MoreOptionsExpander` (no new top-level LazyColumn item — reorderable `headerCount` preserved).
- `ui/screens/SessionEditorScreen.kt` — picker in each item card's More options; visibility
  gated on a selected ball-bearing unit; Success gating + unit-switch strip behaviour.
- `ui/screens/SettingsScreen.kt` — Handedness segmented row (Right / Left) in Preferences.
- `ui/screens/UnitDetailScreen.kt` — criterion `EntryHighlightCard` when present.
- `ui/screens/SessionDetailScreen.kt` — quiet "Observing: …" line on configured item cards.
- `ui/PracticePlannerViewModel.kt` — `updateUnitSuccessCriterion`,
  `toggleSessionItemObservationType`, `updateSessionItemUnit` strips `SUCCESS` when the new unit
  lacks a criterion; editor-state mappers + optimistic builders carry the new fields.
- `ui/PlannerFormatting.kt` — `observationTypeLabel(ObservationType)`.
- `ui/SettingsViewModel.kt` — `selectHandedness` copies **current** prefs (never a preset — the
  clobber bug Stage 2 fixed is not reintroduced), then optimistic save + rollback.
- `ui/PlannerActions.kt`, `ui/RangeworkApp.kt` — wire the new callbacks through.

### Tests

- `PracticePlannerViewModelTest.kt` (+143) — criterion round-trip; toggle types; unit-switch
  strips only `SUCCESS`; stale-success validation lands on the item's error slot; dirty flag
  stays clean after error placement + `withoutErrors()`.
- `SettingsViewModelTest.kt` (+43) — handedness optimistic save, rollback, distance/speed
  preservation.
- `PracticeDraftEditorTest.kt` (+53) — new-field round-trip; `ItemObservationTypes` lands on
  `observationTypesError`, not `unitError`.
- `PracticeLibraryTest.kt` (+8) — duplicate flows carry the new fields.

## Deviations from plan

None material. The plan's three-way shared-gap analysis held: item #1 (input models) was a
fresh addition, item #2 (parse + re-route) was one wiring change, item #3 (duplicate flows) was
verify-only.

## Not done here (owner / device gates)

- Manual device walkthrough of the authoring flow and the end-to-end snapshot-v3 proof (start a
  session from a configured Practice Session and inspect the RPC output) — needs the physical
  build and local stack; not a code-review blocker.
