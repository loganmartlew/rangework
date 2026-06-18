# Stage 20 – Density and rhythm pass: changes

## Summary of changes

### `SettingsListItem.kt` — `SettingsSubheader`
- Reduced top padding from `12.dp` to `8.dp`. Tightens the gap between the end of one section and the start of the next section header.

### `SettingsScreen.kt` — Section grouping
- **Key structural change**: divider-separated rows are now wrapped in tight `Column(spacedBy = 0)` groups per section instead of being direct children of `ScrollableScreen` (which carries `spacedBy(16.dp)` between all children).
- Previously every `HorizontalDivider()` was floating 16dp away from its adjacent rows on both sides, making the list feel like a loose document.
- After: dividers hug their rows. The 16dp spacing from `ScrollableScreen` now only separates sections (subheader → group, group → subheader), not individual rows within a section.
- Segmented-button control groups (Theme, Distance, Speed) gained consistent `padding(vertical = 8.dp)` so they breathe inside the tight section column.
- Sections restructured: **Preferences** (all control rows in one column), **Clubs** (unchanged, no internal dividers), **Account** (signed-in row + divider + sign-out in one column), **About** (version + divider + help + divider + feedback + divider + privacy in one column).

### `ReorderableItemRow.kt`
- Reduced outer `Column` `verticalArrangement` from `spacedBy(8.dp)` to `spacedBy(4.dp)`.
- Closes the gap between the content body / footer and the ↑/↓/delete action band, reducing the "stacked card with toolbar" feel.
- The 48dp icon buttons themselves are unchanged; only the visual gap above them is tighter.

### `UnitEditorScreen.kt` — `InstructionEditorRow`
- Reduced `ReorderableItemRow` inner padding from `vertical = 8.dp` to `vertical = 6.dp`.
- Shaves 4dp total from each instruction card height without affecting the content.

### `SessionEditorScreen.kt` — `SessionItemEditorCard`
- Changed `Modifier.padding(12.dp)` to `Modifier.padding(horizontal = 12.dp, vertical = 10.dp)`.
- Reduces vertical height of each session item card by 4dp total.

## Regressions to watch

- **Other screens using `ScrollableScreen`** (UnitList, SessionList, Overview, UnitDetail, SessionDetail): not touched. The `spacedBy(16.dp)` in `ScrollableScreen` is unchanged.
- **ManageClubsScreen**: not changed — the plan noted it was already in-family after S16.
- **`SettingsComponents.kt`** (`SettingsReadOnlyRow`, `SettingsActionRow`, `SettingsSectionHeader`): these unused legacy helpers were not changed, and they are not referenced by any screen. No risk.
- **CountStepper**: touch targets (48dp icon buttons) unchanged. No risk.
- The `ReorderableItemRow` spacing change affects both Unit and Session editor rows — check that the action band still feels reachable on small phones.

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [x] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Settings screen on compact phone: dividers sit tight against rows; no floating whitespace between rows and dividers.
- [ ] Settings sections are clearly separated by the 16dp gap from ScrollableScreen, not by inflated internal spacing.
- [ ] Segmented button groups (Theme / Distance / Speed) have 8dp breathing room top and bottom within the preferences section.
- [ ] Unit editor instruction rows feel more compact — action band closer to instruction content.
- [ ] Session editor item cards feel more compact — action band closer to unit picker / repeat stepper.
- [ ] Touch targets on ↑/↓/delete icon buttons remain 48dp and are easy to tap.
- [ ] Drag handles still reach 24dp icon size; NumberBadge and trailing text not clipped.
- [ ] Error / supporting text on OutlinedTextField is not occluded after padding reduction.
- [ ] Large-font pass (system font scale 1.5×) — rows don't overflow or clip.
- [ ] Tablet/expanded width layout still looks correct (Settings section columns stay within 600dp max-width from ScrollableScreen).
- [ ] Dynamic color toggle row (SettingsListItem) respects `heightIn(min = 56.dp)` — still tall enough.
- [ ] Sign-out row (SignOutItem) respects `heightIn(min = 56.dp)` — still tall enough.
