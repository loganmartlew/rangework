# Stage 14 - Editor consistency and terminology repair

> Post-delivery audit stage **S14**. Covers redesign-audit findings **#4, #5, and #12**. Source: `08-implementation/redesign-audit.md`.

## Objective

Repair the drift between the twin editors and restore the terminology lock that was applied in S1 but later regressed.

- Session editor rows reuse `ReorderableItemRow` instead of maintaining a separate inline reorder/delete layout (audit #4, R8).
- Notes labels are scope-prefixed consistently: "Unit notes", "Session notes", and "Item notes" where appropriate (audit #5, systemic 8.1).
- Unused local editor `SnackbarHostState` plumbing is removed unless this stage deliberately wires a real snackbar behavior (audit #12).

## Dependencies

- **Upstream:** S11 drag-to-reorder should already be wired. Preserve its gesture modifier and the chevron fallback.
- **Related:** S13 may adjust detail copy for notes/focus. Coordinate terminology so the same strings appear across detail and editor screens.

## Affected screens

- Unit editor.
- Session editor.
- Unit detail and Session detail notes headings.
- Shared `ReorderableItemRow`.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionEditorScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitDetailScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionDetailScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ReorderableItemRow.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PlannerActions.kt`
- Existing `PracticePlannerViewModelTest` coverage if callbacks change.

## Implementation steps

### 1. Inventory current editor row behavior

Before refactoring, list the current capabilities in the session item row:

- Drag handle.
- Number badge.
- Unit picker/display.
- Ball count, repeat count, subtotal, and duration text.
- Club override and item notes in progressive disclosure.
- Move up/down chevrons.
- Delete action.

The refactor is complete only if all of these behaviors survive.

### 2. Generalize `ReorderableItemRow` only as much as needed

If the existing shared row is too instruction-shaped, extend it with slots rather than forking another row.

Useful slot boundaries:

- Leading content: drag handle and/or number badge.
- Main content: editor fields.
- Trailing content: delete and reorder controls.
- Optional footer: subtotal or extra row content if Session editor needs it.

Keep the public API small. The goal is one shared row shell for both editors, not a large generic layout framework.

### 3. Refactor Session editor onto the shared row

Replace the inline `SessionItemEditorCard` reorder/delete header structure with `ReorderableItemRow`.

- Preserve S11's `dragHandleModifier`.
- Preserve `onMoveSessionItem(from, to)` and chevron callbacks.
- Keep delete target at least 48dp.
- Keep the current item-field order from S7/S11.
- Keep subtotal/duration visibility.

After this step, Unit and Session editors should place drag, badge, move, and delete controls consistently.

### 4. Restore notes terminology

Apply the terminology lock in display strings only.

- Unit editor field: "Unit notes".
- Unit detail card/title: "Unit notes".
- Session editor field: "Session notes" should remain.
- Session detail card/title: "Session notes".
- Session item editor notes field: "Item notes" should remain.

Do not rename serialized fields, model properties, database columns, or repository mappings.

### 5. Remove unused editor snackbar hosts

Both editors currently create local `SnackbarHostState` instances but do not show messages through them.

- Remove the unused state and `SnackbarHost` wiring if no snackbar is emitted.
- Keep shell-level save confirmation behavior unchanged.
- Do not implement in-editor delete undo in this stage; that was explicitly deferred as a larger feature.

If a local host is still needed after code inspection, document the producer that calls `showSnackbar`. Otherwise, the dead host should be gone.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Unit editor reorder/delete controls unchanged.
- [ ] Session editor drag gesture still reorders items.
- [ ] Session editor chevron fallback still reorders items.
- [ ] Session editor delete action still removes the correct item.
- [ ] Session item subtotal and total update correctly after edits/reorders.
- [ ] Notes labels are scope-prefixed across editor and detail screens.
- [ ] No unused `SnackbarHostState` remains in editor screens.
- [ ] Save confirmation still appears through the existing shell-level snackbar.

## Accessibility requirements

- Reorder and delete controls keep content descriptions and 48dp targets.
- The shared row should not merge semantics in a way that hides inner fields from TalkBack.
- Notes labels should be descriptive when read out of context.

## Regression risks

- Session editor rows are denser than unit instruction rows. A shared row shell must not force the session layout into a cramped shape.
- Drag libraries usually depend on stable item keys. Preserve stable order/id keys while moving the row shell.
- Removing snackbar hosts is safe only if no coroutine still calls them. Search for all `showSnackbar` and `SnackbarHostState` usage before deleting.
