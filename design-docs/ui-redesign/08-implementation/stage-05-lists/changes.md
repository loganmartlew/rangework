# Stage 05 — Units list + Sessions list redesign: changes

## Summary of changes

### Modified files

| File | Change |
|---|---|
| `androidApp/.../ui/components/ListEntryCard.kt` | Added optional `metadataRow: (@Composable () -> Unit)? = null` slot rendered between the subtitle and supporting-text tiers. Backward-compatible — existing callers pass nothing and get the old behaviour. |
| `androidApp/.../ui/PlannerActions.kt` | Added `onDuplicate: (String) -> Unit` and `onClearDuplicatedId: () -> Unit` to `UnitEditorActions` to match the existing session duplicate pattern. |
| `androidApp/.../ui/screens/UnitListScreen.kt` | Full migration from `SummaryEntityCard` + `HorizontalDivider` to `ListEntryCard`. Adds `onDuplicateUnit` parameter, `DeleteConfirmationDialog` local state, `ClubChip` in the `metadataRow` slot when a default club is set, and a `96dp` bottom `Spacer` for FAB clearance. |
| `androidApp/.../ui/screens/SessionListScreen.kt` | Full migration from `SummaryEntityCard` + `HorizontalDivider` to `ListEntryCard`. Adds `onGoToUnits: () -> Unit` parameter to wire the dependency-aware empty state (was a no-op before), `DeleteConfirmationDialog` local state, `BallCountPill` in the `metadataRow` slot for ball count prominence, and a `96dp` bottom `Spacer`. Removes unused `ballSummary` import. |
| `androidApp/.../ui/RangeworkApp.kt` | Wires the new `UnitEditorActions.onDuplicate` / `onClearDuplicatedId`. Adds `LaunchedEffect(plannerUiState.duplicatedUnitId)` to navigate to unit edit after duplication (mirrors the session duplicate flow). Replaces bare `FloatingActionButton` with `RangeworkFab` / `RangeworkExtendedFab`; list screens show the extended variant while their list is empty, collapsing to the standard FAB once items exist. Passes `onDuplicateUnit` and `onGoToUnits` to the two list screens. |
| `androidApp/src/test/.../PracticePlannerViewModelTest.kt` | Added two tests: `deleteUnitRemovesFromList` (verifies the unit is gone and a Notification status is emitted) and `duplicateUnitSetsDuplicatedUnitId` (verifies `duplicatedUnitId` is set and a second unit appears in the list). |

---

## Card shape after migration

### Units card
| Tier | Content |
|---|---|
| Title | `unit.title` (`titleSmall` / DM Sans) |
| Subtitle | instruction count + ball count string (`bodyMedium`) |
| MetadataRow | `ClubChip(name)` — only rendered when a default club is set |
| SupportingText | instruction texts joined with `"  •  "` (`bodySmall` / `onSurfaceVariant`) |
| Overflow | Edit / Duplicate / Delete (Delete fires `DeleteConfirmationDialog`) |

### Sessions card
| Tier | Content |
|---|---|
| Title | `session.name` (`titleSmall` / DM Sans) |
| Subtitle | item count string (`bodyMedium`) |
| MetadataRow | `BallCountPill(ballCount)` — always rendered; ball count is `Int` (never null for a session) |
| SupportingText | unit title lineup joined with `"  •  "` (`bodySmall` / `onSurfaceVariant`) |
| Overflow | Edit / Duplicate / Delete (Delete fires `DeleteConfirmationDialog`) |

---

## Deferred / out of scope

**Undo snackbar (B02-partial):** The plan calls for "confirm + undo". The confirmation dialog is implemented. Full undo-after-confirm requires staging deletes in the ViewModel (optimistic removal + backend commit on snackbar dismiss) — this was deferred to avoid major VM surgery in this stage. The existing `PlannerStatus.Notification("Deleted X.")` still surfaces via the `AuthenticatedAppShell` snackbar, giving users visual feedback without a functional Undo action.

**96dp spacer on empty-state views:** The `Spacer` is placed at the end of the populated-list branch only. Empty states have a FAB (Extended) that sits above the CTA button in practice; no spacer needed there.

---

## Regression risks

**R1 — `SummaryEntityCard` still exists.** It is still used by `UnitDetailScreen` and `SessionDetailScreen` (S6 territory). No breakage, but the two card styles coexist until S6.

**R2 — `DeleteConfirmationDialog` now lives in the list screen, not the card.** The old `SummaryEntityCard` embedded its own dialog; `ListEntryCard` does not. Any future screen using `ListEntryCard` must add its own delete guard. This is consistent with the S2 convention documented in R5 of the S2 changes.

**R3 — Extended FAB on sparse lists.** The Units FAB always shows (even on first load before `hasLoaded`). This is unchanged behaviour from before S5.

**R4 — `duplicatedUnitId` navigation.** The new `LaunchedEffect` navigates to the unit editor on duplication. If the user is already on the unit editor route and duplicates from there (not possible today — overflow is only on list), this would stack routes. No current call path triggers this.

**R5 — `onGoToUnits` uses nav graph pop-to to match nav bar behaviour.** If the Sessions list is reached via deep link rather than the nav bar, the `findStartDestination().id` call restores standard nav bar selection.

---

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — BUILD SUCCESSFUL.
- [x] `.\gradlew.bat :androidApp:lintDebug` — BUILD SUCCESSFUL, no new lint errors.
- [x] `deleteUnitRemovesFromList` test passes.
- [x] `duplicateUnitSetsDuplicatedUnitId` test passes.
- [ ] Tapping a card body opens the corresponding detail screen (both lists).
- [ ] Overflow ⋮ shows Edit / Duplicate / Delete on both list screens.
- [ ] Duplicate on Units: triggers `duplicateUnit`, emits notification, navigates to unit editor.
- [ ] Duplicate on Sessions: triggers `duplicateSession`, emits notification, navigates to session editor.
- [ ] Delete on Units/Sessions: shows `DeleteConfirmationDialog`; Cancel dismisses; Confirm fires delete and updates list.
- [ ] Empty Units state: icon + headline + body + "Create your first unit" CTA routes to unit create.
- [ ] Empty Sessions with units: icon + headline + body + "Create your first session" CTA routes to session create.
- [ ] Empty Sessions without units: "Create a unit first" state; "Go to Units" CTA navigates to Units tab.
- [ ] FAB shows green `primaryContainer` fill on both list screens.
- [ ] Extended FAB shown when Units list is empty; collapses to standard FAB once units exist.
- [ ] Extended FAB shown when Sessions list is empty (and units exist); collapses to standard FAB once sessions exist.
- [ ] Scrolling to the last card: FAB does not cover it or its overflow icon (96dp spacer).
- [ ] Units card: `ClubChip` renders when a default club is set; absent when no club is set.
- [ ] Sessions card: `BallCountPill` renders as the prominent ball-count figure.
- [ ] Sessions card: unit lineup appears in the supporting-text tier.
- [ ] Both card types use identical three-tier template (visual parity check).
- [ ] Phone layout (compact) and tablet layout (expanded nav rail) both render correctly.
- [ ] No hardcoded colours or anonymous `TextStyle`s in any changed file.
