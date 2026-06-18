# Stage 06 — Changes

## Summary of changes

### `OverflowMenu.kt`
- Made `onEdit` parameter optional (`(() -> Unit)? = null`) so it can be omitted when a separate Edit icon button is present in the app bar. Existing list-card callers continue to pass `onEdit` unchanged.

### `PlannerActions.kt`
- Added `onDuplicate: (String) -> Unit` and `onClearDuplicatedId: () -> Unit` to `UnitEditorActions`, matching the existing pattern in `SessionEditorActions`.

### `PracticePlannerViewModel.kt`
- Added `restoreUnit(unit: PracticeUnit)`: re-saves a deleted unit with its original ID using the existing `savePracticeUnitUseCase`, enabling the Undo snackbar to reverse a delete.
- Added `restoreSession(session: PracticeSession)`: same pattern for sessions.

### `UnitDetailScreen.kt` (full redesign)
- **Removed** duplicate H1 title (entity name is now in the app bar only).
- **Removed** `ActionRow` with co-equal Edit/Delete buttons.
- **Removed** `onDeleteUnit` parameter (delete is now handled at the app-bar/shell level).
- **Added** `BriefingRow` stat strip: total balls (primary numeral) + instruction count.
- **Added** `FocusCard` (`secondaryContainer`-tinted) rendered only when a focus cue exists; absent otherwise.
- **Added** `EntryHighlightCard` for Notes and Default club when set.
- **Redesigned** instructions list: each row uses `NumberBadge` for position + instruction text + right-aligned `BallCountPill` for per-instruction ball count.
- **Added** `EmptyStateCard` inside the instructions card when no instructions exist, with a CTA to edit the unit.

### `SessionDetailScreen.kt` (full redesign)
- **Removed** duplicate H1 title.
- **Removed** `ActionRow` with co-equal Edit/Delete buttons.
- **Removed** `onDeleteSession` parameter.
- **Added** `BriefingRow` briefing strip: total balls + unit count + estimated duration (`~N min` via `estimateSessionDurationMinutes` from S3, labelled "Est. time").
- **Added** `EntryHighlightCard` for Notes when set.
- **Redesigned** session items list: each row is a structured `SessionItemDetailRow` with:
  - `NumberBadge` for position.
  - Unit name as primary title.
  - Right-aligned `BallCountPill` for item ball subtotal.
  - Secondary chip row (aligned after badge): repeat ×N chip (mono font) + override club chip shown only when `item.clubReference` differs from `unit.defaultClubReference`, labelled `"<Name> (override)"`.
  - Focus cue as a secondary body-small text line when present.
- **Added** `EmptyStateCard` for empty session items with a CTA to edit the session.

### `RangeworkApp.kt`
- **Wired** unit duplicate callbacks (`onDuplicate`, `onClearDuplicatedId`) into the `unitActions` block.
- **Added** `LaunchedEffect(plannerUiState.duplicatedUnitId)` to navigate to the unit editor after duplication (mirrors the session pattern).
- **Added** `onRestoreUnit`/`onRestoreSession` parameters to `AuthenticatedAppShell`, wired to `plannerViewModel::restoreUnit` / `plannerViewModel::restoreSession`.
- **Added** delete dialog state (`showUnitDeleteDialog`, `pendingDeleteUnit`, `showSessionDeleteDialog`, `pendingDeleteSession`) and undo state (`justDeletedUnit`, `justDeletedSession`).
- **Added** `DeleteConfirmationDialog` for unit and session deletes (shown before the `Scaffold`).
- **Added** `LaunchedEffect(justDeletedUnit)` and `LaunchedEffect(justDeletedSession)` that show the `UndoSnackbar` and call the restore callback if tapped.
- **Added** `currentUnitId`/`currentSessionId` derived from the current back-stack route to drive app-bar actions.
- **Updated** `CenterAlignedTopAppBar` to include trailing `actions`: Edit `IconButton` + `OverflowMenu` (Duplicate, Delete) for unit and session detail routes. No actions shown on other routes.
- **Simplified** `UnitDetail` composable call (removed `onDeleteUnit`).
- **Simplified** `SessionDetail` composable call (removed `onDeleteSession`).

---

## Regression risks

| Risk | Mitigation |
|---|---|
| `OverflowMenu` param order changed (`onEdit` moved after `onDelete`) — callers using positional args would break | All callers use named arguments. `ListEntryCard` passes `onEdit = onEdit` named. Build confirms no breakage. |
| Unit duplicate nav (`duplicatedUnitId` LaunchedEffect) fires on app start if stale state is present | `duplicatedUnitId` starts as `null` and is cleared immediately on navigation via `onClearDuplicatedId()` — same pattern as session duplicate nav. |
| `restoreUnit`/`restoreSession` use upsert-by-id after deletion — may conflict if id is reused | IDs are UUIDs; collision probability is negligible. RPC `save_practice_unit` was confirmed to accept an explicit ID. |
| Delete + popBackStack fires synchronously before the snackbar shows; user sees the list before undo appears | This is intentional: undo appears on the list screen snackbar host, consistent with M3 destructive-action patterns. |
| Club override check (`item.clubReference != unit?.defaultClubReference`) — previously the UI showed the effective club always | This is a deliberate UX change per the plan. Rows where the item uses the same club as the unit default now show no club chip, which is correct — the unit default applies implicitly. |
| Duration label "~N min" for 0-ball sessions shows "—" rather than "~0 min" | Intentional: 0-ball sessions have no meaningful duration estimate. |
| Session detail no longer shows `item.notes` | Notes field is not in the session item row spec for S6. If surfacing item notes is needed it can be added in a later stage. |

---

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — **BUILD SUCCESSFUL**
- [x] `.\gradlew.bat :androidApp:lintDebug` — **BUILD SUCCESSFUL** (no new issues)
- [ ] App-bar Edit icon navigates to the editor on unit detail.
- [ ] App-bar Edit icon navigates to the editor on session detail.
- [ ] Overflow ⋮ → Duplicate triggers duplication and opens the editor for the new copy (unit and session).
- [ ] Overflow ⋮ → Delete shows `DeleteConfirmationDialog`; Cancel closes it without deleting.
- [ ] Confirm in `DeleteConfirmationDialog` deletes the entity, pops back to the list, and shows an Undo snackbar.
- [ ] Tapping Undo on the snackbar restores the deleted entity and it reappears in the list.
- [ ] No co-equal Edit/Delete buttons remain in the content area of either detail screen.
- [ ] No duplicate H1 title in either detail screen (entity name appears in app bar only).
- [ ] `BriefingRow` appears for both screens; total balls is the largest numeral on screen.
- [ ] Per-instruction `BallCountPill` right-aligns into a column on unit detail.
- [ ] `FocusCard` (secondaryContainer tint) renders only when unit has a focus cue; absent otherwise.
- [ ] Session briefing shows balls + units + "~N min" duration; "—" when 0 balls.
- [ ] Session item club chip shown only when `item.clubReference` differs from `unit.defaultClubReference`, labelled "(override)".
- [ ] `NumberBadge` replaces "Instruction N" / "Item N" text labels.
- [ ] Empty instructions state shows `EmptyStateCard` with "Edit unit" CTA → navigates to editor.
- [ ] Empty session items state shows `EmptyStateCard` with "Edit session" CTA → navigates to editor.
- [ ] "Unit not found" / "Session not found" fallback renders correctly with New unit/session CTA.
- [ ] Phone layout (bottom nav) verified.
- [ ] Tablet layout (navigation rail) verified.
