# Stage 08 — Settings redesign + new Manage clubs screen

> Roadmap stage **S8**. Refactors Settings to a native list and extracts the 30-club bag to its own screen. Backlog: B10, B17, B39, B43, B44, B45, B46, B47, B58, B59. Spec: `07-redesigns/settings-redesign.md`. **Adds one new route.**

## Objective

Collapse a three-scroll Settings screen into one scannable page and give the club bag the dedicated screen its volume warrants.

- **Reorder sections** to frequent-first: Preferences (Appearance + Units grouped — B58) → Clubs → Account → About (B17).
- Small TopAppBar, title-only (B34).
- **Extract the club bag** to a new pushed **Manage clubs** screen; Settings shows a single Club-bag `ListItem` with a "12 of 30 clubs enabled" summary + chevron (B10, B44).
- Replace card-wrapped groups with stock M3 list groups under `ListSubheader`s (B46).
- **Sign out** → a proper `ListItem` with a logout icon and error-coloured label (B47).
- **Dynamic color** toggle gains supporting text ("Use colours from your wallpaper") (B43).
- **Speed units** get a clarifying caption or are deferred if no feature consumes them (B45).
- **Manage clubs screen:** category `ListSubheader`s (Woods/Hybrids/Irons/Wedges/Putter — B46); per-club `ListItem` + `Switch` with content descriptions (B39); search/filter (B59); overflow presets "Enable common bag" / "Disable all" (B59); live enabled-count caption.

## Dependencies

- **Upstream:** S1 (tokens, error colour, nav), S2 (`SettingsListItem`/`SettingsSubheader`/`SignOutItem`), S3 (`EnabledClubCount`).
- **Downstream:** none. Independent of S5/S6/S7 — can run in parallel with them (shares only S2 + S3).

## Affected screens

- **Settings** (restructured) and **Manage clubs** (new pushed destination).

## Likely files

- Extracted `SettingsScreen` composable under `androidApp/.../ui/`; new `ManageClubsScreen` composable.
- `androidApp/.../ui/RangeworkApp.kt` — register the new Manage clubs route; Settings → Manage clubs push; preserve responsive nav (rail on expanded).
- `androidApp/.../ui/PracticePlannerViewModel.kt` (or a settings/preferences ViewModel) — club enable/disable persistence, enabled-count, search/preset bulk-updates, theme/units/dynamic-color state.
- `shared/.../data/Supabase*Repository.kt` + `model` — `user_preferences` club set (existing path; bulk preset writes should reuse it).
- S2 settings components (consumed).
- `androidApp/src/test/...` — extend for count summary, search filter, preset bulk-update.

## New components required

- `ManageClubsScreen` is a new **screen**, not a reusable component; it is assembled from S2 `SettingsSubheader` + `SettingsListItem`-with-`Switch` plus a search affordance (`SearchBar`/`DockedSearchBar`) and an overflow `DropdownMenu`. No new shared component beyond what S2 provides.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Sections appear in order: Preferences → Clubs → Account → About; About is reachable without a long scroll.
- [ ] Club-bag row shows the correct "{N} of 30 clubs enabled" summary (matches S3 aggregate) and pushes to Manage clubs.
- [ ] Manage clubs: toggles persist via the existing preferences path; only enabled clubs still appear in app-wide dropdowns.
- [ ] Search filters the 30-club list; "Enable common bag" / "Disable all" presets bulk-update and the count caption updates live.
- [ ] Sign out is a list item with logout icon + error label; sign-out still works.
- [ ] Dynamic-color toggle shows supporting text; theme/units selectors behave exactly as before.
- [ ] Back navigation from Manage clubs returns to Settings with state intact.
- [ ] Speed-units decision applied (caption or deferred) per F-5.
- [ ] Phone + tablet (expanded rail) verified.

## Accessibility requirements

- Every club `Switch` carries a content description ("Enable {club}" / "{club}, enabled/disabled") so TalkBack reads the club name, not just the toggle state (B39, Material audit).
- Club rows use `ListItem` at ≥48–56dp height with full-row `toggleable` semantics (Material audit) — 48dp targets.
- Sign out pairs error colour with a logout icon + text label — not colour-only (9.3).
- Section/group `ListSubheader`s announced as headings for TalkBack navigation.
- Numeric settings values (enabled count, version) use `RangeworkMono` per `CLAUDE.md`; labels use DM Sans.
- Search field labelled; preset overflow items have clear names.

## Regression risks

- New route is the only navigation-graph change in the screen stages — verify deep state (which clubs enabled) survives the push/pop and config changes (rotation).
- Bulk presets ("Disable all") must not orphan clubs already assigned to units — the spec/audit flagged uncertainty about toggling an in-use club; confirm behaviour (disable only affects future dropdowns, existing assignments preserved) and consider a note.
- Club enable/disable still drives dropdowns app-wide — regression-test a unit/session dropdown after changing the bag.
- Replacing card-wrapped groups with stock list groups changes spacing — verify on tablet rail layout.
- Speed-units deferral (B45) must not remove stored preference data if the control is only hidden.
