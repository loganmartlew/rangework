# Stage 07 — Changes

## Summary of changes

### `PlannerFormatting.kt`
- Added `sessionEditorTotalText(totalBalls: Int): String` — formats the live total bar value as `"N balls · ~Mmin"` using `SECONDS_PER_BALL` from shared for the duration estimate (B12, B15).

### `PlannerActions.kt`
- `UnitEditorActions.onUpdateInstructionBallCount` changed from `(Int, String) -> Unit` to `(Int, Int) -> Unit` — callers now supply an `Int` from `CountStepper` rather than a raw text string (B05).
- `SessionEditorActions.onUpdateItemRepeatCount` changed from `(Int, String) -> Unit` to `(Int, Int) -> Unit` — same reason (B05).

### `PracticePlannerViewModel.kt`
- Added `updateInstructionBallCount(index: Int, value: Int)` overload — delegates to the existing `String` overload via `.toString()`. The `String` overload is preserved so existing unit tests continue to compile unchanged.
- Added `updateSessionItemRepeatCount(index: Int, value: Int)` overload — same pattern.

### `RangeworkApp.kt`
- `unitActions.onUpdateInstructionBallCount` now uses an explicit lambda `{ i, v -> plannerViewModel.updateInstructionBallCount(i, v) }` to resolve the `Int` overload unambiguously.
- `sessionActions.onUpdateItemRepeatCount` same pattern.
- Both `SessionCreate` and `SessionEdit` composable calls in `NavHost` now pass `onNavigateToCreateUnit` — routes the user to `UnitCreate` when they tap "Add item" and no units exist yet (B19).

### `UnitEditorScreen.kt` (full redesign — B01, B05, B09, B21, B32, B34, B36, B40, B41, B48, B50)
- **Removed** the duplicate `headlineMedium` H1 below the app bar (B34).
- **Removed** the outer `Card`-within-`ScrollableScreen` layout; content now sits directly in a `LazyColumn` inside its own `Scaffold` (B48).
- **Added** `DockedSaveBar` in the Scaffold `bottomBar` slot — green `FilledButton`, always reachable without scrolling (B09). Save confirmation snackbar is already emitted by the ViewModel's `PlannerStatus.Notification` flow via the outer shell.
- **Notes & Focus** fields are now wrapped in a `MoreOptionsExpander("Add notes & focus")` that auto-expands when either field has content — keeps new-unit flow minimal (B40). Each field gains `supportingText` helper copy (B32).
- **Instructions** rendered via `LazyColumn`'s `itemsIndexed`.
  - Each row uses `ReorderableItemRow` (S2 component) — drag handle visual + accessible ↑/↓ chevrons (B01).
  - `NumberBadge` replaces the "Instruction N" headline text (B21).
  - Ball count replaced by `CountStepper` (min 0, max 100) — no keyboard required for small integers (B05).
  - Instruction row wrapped in a subtle `Surface(surfaceContainerLow)` instead of a nested card.
  - Delete button (48 dp) is provided by `ReorderableItemRow`'s `onDelete` slot with a labelled `contentDescription` (B38).
- **Running total** (`ballSummary`) displayed between the instruction list and the "Add instruction" button.
- **"Add instruction"** is now a full-width `TextButton` with a leading `+` icon (B50).

### `SessionEditorScreen.kt` (full redesign — B01, B03, B05, B09, B12, B15, B19, B21, B34, B36, B38, B40, B41, B48, B50)
- **Removed** the duplicate `headlineMedium` H1 (B34).
- **Removed** the outer `Card`-within-`ScrollableScreen` layout; content in `LazyColumn` inside a `Scaffold` (B48).
- **Added** `DockedSaveBar` in `bottomBar` (B09).
- **Added** `StickyTotalBar` as a `stickyHeader` — shows `"N balls · ~Mmin"` live as repeat counts and items change; always visible while scrolling (B12, B15). Requires `@OptIn(ExperimentalFoundationApi::class)`.
- **Session notes** field wrapped in `MoreOptionsExpander("Add notes")` — auto-expands when notes exist (B40). Helper text added (B32).
- **Session items** (`SessionItemEditorCard`):
  - Header row: drag handle icon + `NumberBadge` replacing "Session item N" (B21) + live ball subtotal (`RangeworkMono.medium`) + 48 dp delete `IconButton` with `contentDescription` (B03, B38).
  - Practice unit dropdown comes first, immediately followed by `CountStepper` for repeat count (min 1, max 50) — keyboard-free entry, subtotal is adjacent to the inputs driving it (B03, B05).
  - Accessible ↑/↓ chevrons retained as a separate row for TalkBack reorder (accessibility requirement).
  - Club override, item notes, and focus cue folded behind `MoreOptionsExpander("More options")` that auto-expands when any optional field has a value (B40). Helper text added to notes and focus fields (B32).
- **"Add item"** is a full-width `TextButton` with a leading `+` icon (B50 analogue).
  - When no units exist, clicking routes to `UnitCreate` instead of opening an empty picker (B19). Button label changes to "Create a unit first" for clarity.
- Empty-items hint text distinguishes between "no units" and "units exist but no items added" states.

### `PracticePlannerViewModelTest.kt`
- `FakePlannerRepositories` now captures `savedSessionDrafts` (mirrors existing `savedUnitDrafts`).
- Added `intBallCountStepperUpdatesInstructionAndPersistsOnSave` — verifies `updateInstructionBallCount(Int, Int)` stores correctly and persists through `saveUnit()`.
- Added `intBallCountStepperClampsToZeroMin` — spot-checks the min boundary.
- Added `intRepeatCountStepperUpdatesSessionItemAndPersistsOnSave` — verifies `updateSessionItemRepeatCount(Int, Int)` persists through `saveSession()`.
- Added `isSessionEditorDirtyTogglesCorrectly` — mirrors the existing unit-editor dirty test for sessions.

---

## Build status

- `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — **BUILD SUCCESSFUL**
- `.\gradlew.bat :androidApp:lintDebug` — **BUILD SUCCESSFUL** (no new errors; two pre-existing `menuAnchor()` deprecation warnings, not introduced by this stage)

---

## Regression risks identified

| Risk | Mitigation |
|---|---|
| **R2** Sticky total + docked save + IME overlap | `DockedSaveBar` is in the Scaffold `bottomBar` slot — Compose handles IME inset automatically. `StickyTotalBar` is a `stickyHeader` so it stays above content. Manual testing on small screen required. |
| **R3** Drag-handle gesture not yet wired | The visual drag handle is present; ↑/↓ chevrons remain as the reorder mechanism. No regression — this is the same as S2. |
| **R9** Progressive disclosure hides expected fields | `MoreOptionsExpander.hasContent` auto-expands when any wrapped field already has a value. Existing data is never hidden. |
| Ball count stepper shows `0` for un-entered instructions | Instructions created by `addInstruction()` default to `ballCount = ""`. The stepper shows `"".toIntOrNull() ?: 0` = 0. This is correct — 0 means "no count set", consistent with the nullable `ballCount` in the domain model. |
| `updateInstructionBallCount(Int, String)` still public | Intentional — preserves existing test compatibility. Consider making it internal in a future cleanup stage. |
| Undo snackbar on instruction/item delete not implemented | Deferred — requires per-editor `SnackbarHostState` wired to a restore callback. The 48 dp delete icon (B38 primary requirement) is met; undo is a secondary enhancement flagged in the plan. |

---

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] **Unit editor — new unit flow**: only Title + Default club + instructions visible initially; "Add notes & focus" expander is collapsed.
- [ ] **Unit editor — edit flow**: Notes & focus expander auto-opens when the unit has existing notes or focus.
- [ ] Helper text visible under Notes ("General reminders…") and Focus ("One mental cue…").
- [ ] Ball count stepper (−/value/+) works; min 0, max 100; keyboard not required.
- [ ] Drag handle visible on each instruction row; ↑/↓ chevrons reorder correctly; delete removes the row.
- [ ] Running total below instruction list updates live as ball counts change.
- [ ] "Add instruction" is a full-width TextButton with + icon.
- [ ] "Save unit" is a green `FilledButton` docked at the bottom, always visible without scrolling.
- [ ] Saving fires a confirmation snackbar (via ViewModel `PlannerStatus.Notification`).
- [ ] Unsaved-changes back guard prompts discard dialog (unchanged from S6 — verify not broken).
- [ ] **Session editor — sticky total bar** pinned below app bar, updates on every repeat-count or item change, shows estimated duration.
- [ ] Session notes expander collapsed on new session, auto-opens when editing a session that has notes.
- [ ] Each session item card: drag handle + number badge + live subtotal + delete (48 dp) in header.
- [ ] Unit dropdown and repeat stepper (min 1, max 50) lead the item card body.
- [ ] "More options" collapses club override / item notes / focus cue; auto-expands when any optional field has a value.
- [ ] "Add item" with **no units** routes to unit create screen and does not open an empty picker (B19).
- [ ] "Add item" with units present adds a new item correctly.
- [ ] "Save session" is a green docked `FilledButton`; saving fires confirmation snackbar.
- [ ] IME (keyboard) open does not obscure the docked Save bar on a small phone.
- [ ] Phone layout (bottom nav) and tablet/expanded layout (nav rail) both render correctly.
- [ ] No scroll jank with sticky total + docked save + many items.
