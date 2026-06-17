# Stage 07 — Unit edit + Session edit redesign

> Roadmap stage **S7**. The heaviest interaction-debt screens; twin editors built from one shared stepper/drag/save/total set. Backlog: B01, B03, B05, B09, B12, B15, B19, B21, B32, B34, B36, B38, B40, B41, B48, B50. Specs: `07-redesigns/unit-edit-create-redesign.md`, `session-edit-create-redesign.md`.

## Objective

Fix the app's worst interaction surface: cramped sub-44dp control clusters, arrow-tap reordering, free-text numerics, a buried Save, and (for sessions) a ball total hidden at the very bottom.

- **Save** → green `FilledButton`, docked to the bottom so it's always reachable, with a confirmation snackbar (B09, B41).
- **Reorder** → drag handle replacing ↑/↓ arrows (B01); a single 48dp delete icon with undo (B38). Long-press-drag is the native gesture.
- **Numerics** → `CountStepper` (− value +): ball count (unit) and repeat count (session) (B05).
- **Fields** → restored `OutlinedTextField` rest borders (B36); helper text distinguishing Notes vs Focus (B32); standardized 16/12/20dp spacing grid (B48); drop the duplicate H1 (B34).
- **Number badges** replace "Instruction N" / "Session item N" headings (B21).
- **Progressive disclosure** — optional fields fold behind "More options" so a new record starts minimal (B40); "Add instruction/item" becomes a full-width TextButton + icon (B50).
- **Session-specific:** sticky live ball total under the app bar with estimated duration (B12, B15); item card reordered so unit + repeat-count stepper lead and the live subtotal shows in the header (B03); Add-item flow is dependency-aware — no units → route to create a unit first (B19).

## Dependencies

- **Upstream:** S1 (field borders, tokens), S2 (`CountStepper`, `ReorderableItemRow`, `DockedSaveBar`, `StickyTotalBar`, `MoreOptionsExpander`, `UndoSnackbar`, restored field defaults). Soft: S3 (dependency-aware Add-item uses existing units; duration helper for the sticky total).
- **Downstream:** none structurally; editors are leaf screens reached from lists/details.

## Affected screens

- **Unit edit/create** and **Session edit/create**.

## Likely files

- Extracted `UnitEditScreen` / `SessionEditScreen` composables under `androidApp/.../ui/`.
- `androidApp/.../ui/PracticePlannerViewModel.kt` — editing/save/delete flows, validation, live total derivation, reorder persistence, dependency-aware Add-item routing, setup messaging.
- `androidApp/.../ui/RangeworkApp.kt` — editor navigation + unsaved-changes back/up guard.
- S2 components (consumed); `shared` validation helpers and duration (from S3).
- `androidApp/src/test/.../PracticePlannerViewModelTest` — extend for stepper bounds, reorder, live total, save/validation.

## New components required

None new — all from S2. This stage composes the **instruction row** (drag handle + number badge + instruction field + ball stepper + 48dp delete) and the **session item card** (header: drag handle + badge + live subtotal + delete; unit dropdown; repeat stepper; "More options" expander wrapping club override / item notes / focus cue).

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] Drag-to-reorder persists the new order on save (both editors).
- [ ] Stepper clamps to min/max and rejects invalid input; keyboard not required for counts.
- [ ] Session sticky total updates live on every repeat-count/item change and shows estimated duration; unit editor shows a running total beneath instructions.
- [ ] Save persists and shows the confirmation snackbar; Save is a green `FilledButton`, docked, reachable without scrolling.
- [ ] Unsaved-changes guard prompts on back/up when the form is dirty.
- [ ] Notes vs Focus helper text present; fields show borders at rest.
- [ ] "More options" folds optional fields; auto-expands when a wrapped field already has a value.
- [ ] "Add instruction/item" is a full-width TextButton + icon directly under the list.
- [ ] Add-item with **no units** routes to create-a-unit-first (B19), not an empty picker.
- [ ] IME open does not hide the docked Save bar; no scroll jank with sticky total + docked save + nested lists.
- [ ] Phone + tablet (expanded rail) verified.

## Accessibility requirements

- Drag handle and delete each carry content descriptions ("Reorder {instruction}", "Delete instruction"); 48dp targets (B01, B38, 9.2).
- Keep ↑/↓ reordering available as a TalkBack-accessible fallback (drag gestures are hard for assistive tech) — Material audit.
- `CountStepper` − / + buttons labelled ("Decrease ball count" / "Increase"); current value announced.
- Restored borders + helper text raise field legibility above AA (B36, 9.1) — outdoor use.
- Stepper displayed value uses `RangeworkMono.medium` per the `CLAUDE.md` numeric-stepper exception; field content uses `bodyLarge`.
- Docked Save bar remains reachable and announced when the keyboard is open.

## Regression risks

- **R2 (primary):** sticky total bar + docked Save + nested `LazyColumn` + IME can overlap, jank, or hide Save. Prototype the scaffold during S2; test keyboard-open on small screens and tablet.
- **R3:** drag-reorder library/gesture compatibility (validated in S2) — keep chevron fallback.
- **R9:** progressive disclosure hiding fields users expect — auto-expand on populated values; usability check.
- Reorder persistence must map to the existing instruction/item ordering in `shared`/Supabase (sort-order columns) — don't break the stored order.
- Field reordering (B03) and validation changes must keep save semantics identical — extend, don't rewrite, the ViewModel save flow.
- This is the largest single stage — consider it the highest-effort, validate most thoroughly before merge.
