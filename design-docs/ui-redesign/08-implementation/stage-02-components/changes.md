# Stage 02 — Shared UI component library: changes

## Summary of changes

### New component files

All components are in `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/`.
Every component is `internal` (package-visible across the androidApp module), has at least one `@Preview`, and uses only S1 colour/typography tokens — no hardcoded colours, no anonymous `TextStyle`s.

| File | Composables | Notes |
|---|---|---|
| `ListEntryCard.kt` | `ListEntryCard` | 3-tier Card (title / subtitle / supporting text), clickable, trailing `OverflowMenu`. Replaces `SummaryEntityCard` structurally; screens migrate in S5. |
| `OverflowMenu.kt` | `OverflowMenu` | Standalone `MoreVert` icon + `DropdownMenu` with Edit / Duplicate? / Delete items. `onDelete` fires without a dialog — guarding is the screen's responsibility. Content description parameterized for a11y. |
| `UndoSnackbar.kt` | `SnackbarHostState.showUndoSnackbar` | `suspend` extension function. Shows `SnackbarDuration.Long` with "Undo" action label; calls `onRestore` if the user taps it. |
| `StatBlock.kt` | `StatBlock`, `BriefingRow` | `StatBlock` = `RangeworkMono.medium` value + `labelSmall` uppercase caption. `BriefingRow` = evenly spaced row of `StatBlock`s. |
| `NumberBadge.kt` | `NumberBadge` | 28dp circle, `secondaryContainer` fill, `RangeworkMono.small` numeral in `onSecondaryContainer`. |
| `MetadataPills.kt` | `BallCountPill`, `ClubChip` | `AssistChip`-based non-interactive pills. `BallCountPill` uses `RangeworkMono.small` for the count; `ClubChip` uses `labelMedium` + golf-course icon. |
| `CountStepper.kt` | `CountStepper`, `CountStepperState` | `−` / value / `+` row. `CountStepperState` is a pure data class (no Compose dependency) that models min/max clamping, `canDecrement`, `canIncrement`, `increment()`, `decrement()`, `withValue()`. 48dp icon buttons via explicit `Modifier.size(48.dp)`. Content descriptions parameterized on `label`. |
| `ReorderableItemRow.kt` | `ReorderableItemRow` | `DragHandle` icon (visual affordance — gesture deferred, see R3), content slot, ↑/↓ chevrons as accessible reorder mechanism, optional Delete button. 48dp buttons. Content descriptions parameterized. |
| `DockedSaveBar.kt` | `DockedSaveBar` | Full-width `Button` inside a `2.dp` tonal-elevated `Surface`. Wire into Scaffold `bottomBar`. |
| `StickyTotalBar.kt` | `StickyTotalBar` | `surfaceContainerLow` strip with `labelMedium` uppercase label (DM Sans) + `RangeworkMono.medium` value in `secondary`. Place in Scaffold body above the scrollable editor content. |
| `MoreOptionsExpander.kt` | `MoreOptionsExpander` | Animated expand/collapse via `AnimatedVisibility`. Auto-expands (`LaunchedEffect`) when `hasContent = true`, preventing hidden populated fields (R9). State saved with `rememberSaveable`. |
| `FocusCard.kt` | `FocusCard` | `secondaryContainer`-tinted `Card` with `labelMedium` "FOCUS CUE" header + `bodyLarge` cue text. |
| `SettingsListItem.kt` | `SettingsSubheader`, `SettingsListItem`, `SignOutItem` | `SettingsSubheader` = uppercase `labelMedium` section header (mirrors existing `SettingsSectionHeader`; both can coexist until S8 migrates). `SettingsListItem` = 56dp-min row with trailing `Switch`, optional supporting text, full a11y `toggleableState` semantics. `SignOutItem` = destructive action row in `error` colour. |
| `GoogleSignInButton.kt` | `GoogleSignInButton` | `OutlinedButton` with 18dp Google logo + "Sign in with Google" text. Extracted from `RangeworkApp.kt` `SignInActionsCard`. |
| `RangeworkFab.kt` | `RangeworkFab`, `RangeworkExtendedFab` | Thin wrappers over `FloatingActionButton` / `ExtendedFloatingActionButton` for `@Preview` and uniform API. Theme's `primaryContainer` fill (Deep Fairway) applied automatically. |

### Updated component files

| File | Change |
|---|---|
| `DeleteConfirmationDialog.kt` | Added `@Preview`. Revised dialog copy: title now includes the item name (`Delete "$name"?`); body states "cannot be undone" in addition to the red button colour, satisfying the non-colour confirmation requirement (plan §9.3). |

### New test files

| File | Tests | Coverage |
|---|---|---|
| `androidApp/src/test/.../ui/components/CountStepperTest.kt` | 14 passing | `CountStepperState.canDecrement`, `canIncrement`, `increment()`, `decrement()`, `withValue()` clamping (below min, above max, inside range, exact bounds, and min==max edge case). |

---

## Regression risks

**R1 — `DeleteConfirmationDialog` copy changed.** The dialog title and body text are more explicit. Any screenshot baselines for the old dialog text will need updating. Functionally identical.

**R2 — `SettingsSubheader` duplicates `SettingsSectionHeader`.** Both are `internal` and can coexist. `SettingsSectionHeader` is used in `SettingsScreen` (not changed here). S8 will consolidate; no breakage now.

**R3 — `ReorderableItemRow` drag gesture not wired.** The `DragHandle` icon is present but non-interactive. Chevrons remain the accessible reorder mechanism. This matches the plan's R3 spike requirement — no toolchain changes.

**R4 — `ListEntryCard` and `SummaryEntityCard` coexist.** Screens still reference `SummaryEntityCard` (unchanged). `ListEntryCard` is ready for S5 migration but not yet wired. No breakage.

**R5 — `OverflowMenu` does not embed `DeleteConfirmationDialog`.** Callers must handle the delete guard at the screen level. `SummaryEntityCard` still embeds its own dialog for backward compat. Screen stages must add their own guard when migrating to `ListEntryCard` + `OverflowMenu`.

**R6 — `BallCountPill` / `ClubChip` use `AssistChip` with `onClick = {}`.** These are visually non-interactive (no ripple intended) but technically clickable. If a ripple is undesirable, replace with a read-only chip variant in S5/S6.

---

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — BUILD SUCCESSFUL (17 tasks executed, 50 up-to-date).
- [x] `.\gradlew.bat :androidApp:lintDebug` — BUILD SUCCESSFUL, no new lint errors (2 pre-existing `menuAnchor` deprecation warnings in `ClubPickerField.kt` and `SessionEditorScreen.kt` unchanged from before S2).
- [x] `CountStepperTest` — 14/14 tests pass (canDecrement, canIncrement, increment clamping, decrement clamping, withValue clamping, min==max edge case).
- [ ] Every `@Preview` renders correctly in Android Studio — verify visually per component.
- [ ] `ListEntryCard` — both populated and empty/truncated variants render cleanly.
- [ ] `EmptyState` renders both normal and dependency-aware (no-units) variants — **note: `EmptyStateCard` predates S2; a `dependencyAware` variant was not added. Tracked for S5 where the screen wiring determines which variant to show.**
- [ ] `DeleteConfirmationDialog` confirm/cancel callbacks fire correctly; new copy visible.
- [ ] `CountStepper` at min: `−` button disabled; at max: `+` button disabled.
- [ ] `ReorderableItemRow` first item: ↑ disabled; last item: ↓ disabled; middle item: both enabled.
- [ ] `MoreOptionsExpander` collapses by default when `hasContent = false`; auto-expands when `hasContent = true`.
- [ ] `FocusCard` uses `secondaryContainer` / `onSecondaryContainer` on both light and dark themes.
- [ ] `StickyTotalBar` and `DockedSaveBar` layout correctly inside a representative scaffold (validate during S7 — see plan R2).
- [ ] `SettingsListItem` Switch has correct toggle semantics readable by TalkBack.
- [ ] `SignOutItem` text renders in `error` colour, not `onSurface`.
- [ ] `GoogleSignInButton` Google logo visible in both light and dark themes.
- [ ] All interactive icon buttons measure ≥ 48dp (verified by explicit `Modifier.size(48.dp)` on `CountStepper` and `ReorderableItemRow`; `OverflowMenu` relies on `IconButton` default).
- [ ] No hardcoded colours or anonymous `TextStyle`s in any new file — all reference `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, or `RangeworkMono.*`.
- [ ] Drag-to-reorder library compatibility spike still deferred — `DragHandle` icon is visual only; chevron fallback is the active mechanism (R3).
