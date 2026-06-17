# Stage 11 — Approved deferred work: Changes

## Summary of changes

### 1. Drag-to-reorder in editors
Added `sh.calvin.reorderable:reorderable:2.4.3` and wired drag handles in both editors:
- Unit instructions now use `ReorderableItem` with a real drag handle.
- Session items now use `ReorderableItem` with a real drag handle.
- Existing up/down chevrons remain as the accessible fallback.
- `PracticePlannerViewModel.moveInstruction(fromIndex, toIndex)` is public and wired through `UnitEditorActions`.
- Added `moveInstructionReordersToArbitraryIndex` ViewModel coverage.

### 2. Unit reverse links and delete warning
Added `sessionsUsingUnit(unitId, sessions)` as a shared in-memory derivation with common tests. Unit detail now shows a "Used in N sessions" card with tappable session rows. Unit delete dialogs now show an error-coloured warning when the unit is referenced by sessions.

### 3. Session item notes on detail
Session detail rows now show non-blank per-item notes under the focus cue area, using the same indentation and body-small styling.

### 4. Swipe actions on lists
Units and Sessions list cards now support swipe right to edit and swipe left to open delete confirmation. The row springs back after the action, so swipe never silently removes a card. Tap-to-detail and overflow actions are unchanged.

### 5. Collapsing detail app bar
Detail routes now use `MediumTopAppBar` with `exitUntilCollapsedScrollBehavior`. Non-detail routes keep the existing centered pinned app bar behavior.

### 6. Decorative login mark
`BrandMarkContainer` now accepts an optional `contentDescription`. The login mark passes `null`, removing the decorative TalkBack focus stop while preserving the default description elsewhere.

### 7. `ClickableText` migration
Login legal copy now uses `Text` with `LinkAnnotation.Clickable` spans. The Terms and Privacy spans remain no-op links, matching the approved scope.

## Regression risks

| Risk | Mitigation |
|---|---|
| Reorder index offsets could be wrong because LazyColumn indices include header items | Offsets follow the plan: 4 leading items for Unit editor, 2 leading items for Session editor. ViewModel move logic guards invalid indices. |
| Swipe gestures could conflict with vertical scrolling | `SwipeToDismissBox` handles horizontal gestures only; actions return `false` so cards spring back. |
| Unit deletion warning could diverge between delete entry points | Warning was applied to both the detail/app-bar delete dialog and the Units list delete dialog. |
| Global app-bar changes could affect non-detail routes | Non-detail routes use the existing `CenterAlignedTopAppBar` with pinned behavior. |
| Legal links could accidentally become functional | `LinkInteractionListener` is intentionally no-op until policy URLs are approved. |

## Validation checklist

- [x] Reorder library compatibility spike: `.\gradlew.bat :androidApp:assembleDebug` resolved the dependency; initial failure was only the partially wired action parameter.
- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — green.
- [x] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` — green.
- [x] Shared tests cover two matching sessions, no matches, and duplicate references within one session.
- [x] ViewModel test covers arbitrary instruction reordering and order renormalisation.
- [ ] Manual: drag first and last Unit instruction rows and confirm saved order persists.
- [ ] Manual: drag first and last Session item rows and confirm totals remain correct.
- [ ] Manual: swipe Units and Sessions cards left/right; verify edit/delete/tap/overflow all still work.
- [ ] Manual: Unit detail reverse links navigate to the expected Session detail screens.
- [ ] Manual: detail app bars collapse/expand on phone and tablet layouts.
- [ ] Manual: TalkBack pass for login mark, reorder chevrons, swipe fallback paths, and legal link spans.
- [ ] Manual: light and dark visual pass for new cards, swipe backgrounds, and warning copy.
