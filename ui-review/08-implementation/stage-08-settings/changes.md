# Stage 08 — Settings redesign + new Manage clubs screen: changes

## Summary of changes

### 1. `ThemePreference.kt` — dynamic color preference

Added `dynamicColor: Flow<Boolean>` and `setDynamicColor(enabled: Boolean)` to the `ThemePreferenceStore` interface and `DataStoreThemePreferenceStore` implementation. Persisted via a `DYNAMIC_COLOR_KEY` boolean in the same DataStore as the theme mode.

---

### 2. `SettingsViewModel.kt` — new state and actions

- `SettingsUiState` gains `dynamicColor: Boolean = false` and a computed property `enabledClubCount: EnabledClubCount` (derived from `clubCatalog` and `enabledClubCodes` — no extra network round-trip, uses the `EnabledClubCount.from()` helper from S3).
- `init` block now subscribes to `themePreferenceStore.dynamicColor` in parallel with `themeMode`.
- New public methods:
  - `toggleDynamicColor()` — flips and persists the dynamic color preference.
  - `enableCommonBag()` — bulk-enables the 14 default clubs (matching the `default_enabled = true` seed in the migration) and disables the rest. Uses the existing `clubMutex` / `clubToken` pattern.
  - `disableAllClubs()` — bulk-disables every club in the catalog. Same locking pattern.
- `COMMON_BAG_CODES` is a `companion object` constant holding the 14 default-enabled club codes from the migration seed.

---

### 3. `PlannerActions.kt` — `SettingsActions` extended

Added three new fields to `SettingsActions`:
- `onToggleDynamicColor: () -> Unit`
- `onEnableCommonBag: () -> Unit`
- `onDisableAllClubs: () -> Unit`

---

### 4. `RangeworkNavigation.kt` — new `ManageClubs` route

Added `ManageClubs = "settings/clubs"` constant to `RangeworkRoutes`. The route is a pushed (non-top-level) destination, so:
- `isTopLevelRoute()` returns `false` for it (no change needed — the existing exhaustive check already excludes it).
- `shouldRefreshPlanningOnEnter()` returns `false` (no change needed — it only refreshes named top-level and detail routes).

---

### 5. `SettingsListItem.kt` — `SignOutItem` gets a logout icon (B47)

`SignOutItem` now renders a leading `Icons.AutoMirrored.Filled.Logout` icon (20 dp, error tint) alongside the "Sign out" label, satisfying B47 (error-colored label + icon, not color-only).

---

### 6. `SettingsScreen.kt` — full structural redesign

Replaced card-wrapped sections with flat `ScrollableScreen` content using `SettingsSubheader` and `HorizontalDivider`s. Section order is now **Preferences → Clubs → Account → About** (B17, B58):

- **Preferences**
  - Theme `SingleChoiceSegmentedButtonRow` (unchanged behavior).
  - Dynamic color `SettingsListItem` (switch + `supportingText = "Use colours from your wallpaper"`) (B43).
  - Distance and Speed `SingleChoiceSegmentedButtonRow`s kept; speed row gains a caption: "Applies to launch monitor readings." (B45).
  - "Not available in this build" fallback message kept.
- **Clubs** — a single `Row` (leading `GolfCourse` icon, headline "Club bag", `RangeworkMono.small` supporting count "${N} of ${M} clubs enabled", trailing `NavigateNext` chevron) that pushes to `ManageClubsScreen` (B10, B44).
- **Account** — `AccountCircle` icon row showing email as supporting text, followed by `HorizontalDivider` + `SignOutItem` with confirmation dialog (B47).
- **About** — four `Row`s with leading icons and chevrons/values: Version (`RangeworkMono.small`), Help, Feedback, Privacy. Bottom sheet content is unchanged.

The `onSetClubEnabled` parameter has been removed from `SettingsScreen` (club toggling now belongs to `ManageClubsScreen`). `onToggleDynamicColor` and `onNavigateToManageClubs` are new parameters.

---

### 7. `ManageClubsScreen.kt` — new pushed destination (B10, B39, B46, B59)

New composable at `androidApp/.../ui/screens/ManageClubsScreen.kt`:

- **Header row**: live count caption (`RangeworkMono.small` "N of M enabled"), trailing search `IconButton` and overflow `IconButton` with `DropdownMenu` ("Enable common bag" / "Disable all").
- **Search field** (`OutlinedTextField`, full-width): appears when search icon is pressed; filters the club list by `displayName` in-memory using local state.
- **Club list** (`LazyColumn`): categories in `CATEGORY_ORDER` (Woods → Hybrids → Irons → Wedges → Putter), each category under a `labelMedium` uppercase subheader (B46). Each club is a `Row` with `bodyMedium` label + `Switch`; the `Row` carries a `semantics { contentDescription = "Enable {club}" / "{club}, enabled" }` (B39). Items are keyed by club code for stable recomposition.
- Loading / unconfigured / empty-search states handled with fallback `Text` messages.

---

### 8. `RangeworkApp.kt` — wiring

- Added `ManageClubsScreen` import.
- `settingsActions` construction now passes `onToggleDynamicColor`, `onEnableCommonBag`, and `onDisableAllClubs`.
- `dynamicColor` read from `settingsUiState` and forwarded to `RangeworkTheme(dynamicColor = …)`.
- `SettingsScreen` composable call updated: removed `onSetClubEnabled`, added `onToggleDynamicColor` and `onNavigateToManageClubs`.
- New `composable(RangeworkRoutes.ManageClubs)` block hosting `ManageClubsScreen`.
- `titleForRoute` extended: `ManageClubs` → `"Club bag"`.

---

### 9. `SettingsViewModelTest.kt` — test updates

- `FakeThemePreferenceStore` now implements `dynamicColor: Flow<Boolean>` and `setDynamicColor(enabled: Boolean)` to satisfy the updated `ThemePreferenceStore` interface.
- Five new tests added:
  - `disableAllClubsClearsEnabledSet` — verifies all clubs disabled in both state and repo.
  - `enableCommonBagEnablesOnlyCommonCodes` — verifies only default-enabled codes are turned on.
  - `toggleDynamicColorPersistsToStore` — verifies `dynamicColor` flips to `true` after toggle.
  - `enabledClubCountComputedFromState` — verifies the computed property returns correct `enabled`/`total`.

---

## Regression risks

**R1 — `SettingsActions` is a data class with new required fields.**
Any call site constructing `SettingsActions(...)` directly (besides `RangeworkApp.kt`) will fail to compile. Only `RangeworkApp.kt` constructs it; updated in this stage.

**R2 — `ThemePreferenceStore` interface has two new methods.**
Any class implementing the interface outside this module (e.g. future test doubles) must add `dynamicColor` and `setDynamicColor`. `FakeThemePreferenceStore` in `SettingsViewModelTest.kt` updated in this stage.

**R3 — `SettingsScreen` no longer accepts `onSetClubEnabled`.**
Any hypothetical call site passing that parameter will not compile. There is only one call site (in `RangeworkApp.kt`), updated in this stage.

**R4 — Dynamic color overrides the brand palette on Android 12+.**
When the user enables dynamic color, `RangeworkTheme` switches to the system dynamic palette, replacing the hand-crafted green/coral scheme. This is intentional (B43) but is a visible change and should be tested on a device/emulator running API 31+.

**R5 — `enableCommonBag` / `disableAllClubs` use the same `clubToken` as `setClubEnabled`.**
A rapid sequence of single-club toggles followed immediately by "Enable common bag" could be superseded by the bulk operation token, which is the correct behavior. However, a slow in-flight single-toggle arriving after the bulk operation began will be ignored (token check), which is also correct. No known regression; documented for awareness.

**R6 — Clubs assigned to existing practice units / sessions are not orphaned.**
`enableCommonBag` / `disableAllClubs` only affect the `user_enabled_clubs` table (which drives dropdowns). Existing `default_club_reference` and `club_reference` FKs are untouched. Disabling a club hides it from future pickers but does not remove it from saved plans.

**R7 — Speed units control is retained (not deferred).**
B45 was resolved as "caption" (not full deferral). The control persists; if speed units are later surfaced in a new screen the preference is already stored.

---

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` — clean.
- [ ] Sections appear in order: Preferences → Clubs → Account → About; About is reachable without excessive scrolling.
- [ ] Club-bag row shows "{N} of {M} clubs enabled" in mono type; tapping it pushes to Manage clubs screen.
- [ ] Back navigation from Manage clubs returns to Settings with club count updated.
- [ ] Manage clubs search field appears when search icon is pressed; filters the list by club name.
- [ ] "Enable common bag" preset enables the 14 default clubs and disables the rest; count caption updates live.
- [ ] "Disable all" preset disables every club; count caption reads "0 of 30 enabled".
- [ ] Per-club switch toggles persist via the existing preferences path; only enabled clubs appear in unit/session dropdowns.
- [ ] Dynamic color toggle shows supporting text "Use colours from your wallpaper"; toggling on/off changes the app color scheme (API 31+ device/emulator).
- [ ] Speed units row shows caption "Applies to launch monitor readings."
- [ ] Sign out is a list item with leading logout icon + error-colored label; confirmation dialog fires; sign-out works.
- [ ] Distance/speed segmented buttons behave as before; disabled when `dataConfigured = false`.
- [ ] Theme segmented button (System/Light/Dark) behaves as before.
- [ ] Version value renders in `RangeworkMono.small`; other About items have chevrons.
- [ ] Phone layout (bottom bar) verified: all sections visible on Settings and Manage clubs screens.
- [ ] Tablet layout (expanded rail) verified: no layout breakage on wide widths.
- [ ] Rotation / config change on Manage clubs screen: club states survive.
- [ ] TalkBack: each club switch in Manage clubs announces "Enable {club}" or "{club}, enabled" (B39).
- [ ] No crashes when `dataConfigured = false` (missing Supabase config): Settings shows "not available" messages; Manage clubs shows "not available" fallback.
