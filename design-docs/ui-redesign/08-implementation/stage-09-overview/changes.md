# Stage 09 — Overview redesign: changes

## Summary of changes

### 1. `OverviewScreen.kt` — Full rewrite

Replaced the old passive dashboard (welcome card + non-clickable metric blocks + static Next-move tip) with a purposeful launchpad layout.

**Signature change**: dropped `onCreateUnit` / `onCreateSession` as the _only_ callbacks; replaced with a richer set:
- `onNavigateToUnits` — stat card tap → Units tab
- `onNavigateToSessions` — stat card tap → Sessions tab
- `onNavigateToUnitDetail(String)` — recents tap → unit detail
- `onNavigateToSessionDetail(String)` — recents tap / Next-move "Both" action → session detail
- `onCreateUnit` — first-run CTA, Next-move "No units" action
- `onCreateSession` — Next-move "Units no sessions" / "Both (no recents)" action
- `onEditUnit(String)` — Next-move "Resume editing" unit action
- `onEditSession(String)` — Next-move "Resume editing" session action

**First-run state** (`hasLoaded && units.isEmpty() && sessions.isEmpty()`): collapses to `EmptyStateCard` (icon `Widgets`, headline "Plan sharper range sessions", value prop, single "Create your first unit" CTA). Stat row / Next move / recents all hidden. The `hasLoaded` guard prevents a flash of the empty-state layout during initial load.

**Returning user layout** (phone — single column):
1. `GreetingStrip` — `headlineSmall` greeting with name derived from email prefix; `bodySmall` full email caption beneath.
2. `StatCardRow` — two `OutlinedCard`s side-by-side, each with `RangeworkMono.large` numeral (secondary tint), uppercase `labelSmall` label, and a trailing `ChevronRight` icon. Each is a single accessible click target (semantics `contentDescription = "$label, $count, open $label"`).
3. `NextMoveCard` — `primaryContainer`-tinted `Card` with a `labelMedium` "NEXT MOVE" eyebrow, `bodyMedium` contextual message, and a `FilledTonalButton`. Branches on `plannerUiState.nextMoveState`:
   - `NoUnits` → "Build your first practice unit to get started." / "Create unit"
   - `UnitsNoSessions` → "Combine your units into a session template." / "New session"
   - `Both` → "Pick a session to run at the range." / "Open most recent" (routes to most-recent `RecentItem.Session`) or "New session" if no session recents
   - `ResumeEditing(entityId, isUnit)` → "Resume editing your unit/session." / "Resume" (navigates to editor)
4. `RecentlyUsedSection` — hidden when `recentItems` is empty (graceful degradation). Shows a `labelMedium` "RECENTLY USED" subheader then a `LazyRow` of `RecentCard`s (up to 5, sorted by `updatedAt`).
5. Each `RecentCard` is a 200dp-wide `OutlinedCard` with: a chip-styled `Box` (secondaryContainer background, `labelSmall` type text — "Unit" or "Session"), `titleSmall` name, `bodySmall` metadata (ball total or instruction count for units; item count for sessions). Entire card is a single accessible target (`mergeDescendants = true`, `contentDescription = "$name, $typeLabel, open"`).

**Returning user layout** (tablet — two columns, `isExpandedLayout = true`): left column holds greeting + stat cards; right column holds next-move card + recents. Same components, no new logic.

**`!dataConfigured` guard** is still present and checked before the first-run / returning-user branch.

---

### 2. `RangeworkApp.kt` — Updated `OverviewScreen` call site

The `composable(RangeworkRoutes.Overview)` block now passes all eight navigation callbacks:
- `onNavigateToUnits` / `onNavigateToSessions` use the tab-navigation pattern (same `launchSingleTop + restoreState + popUpTo` as the bottom bar / rail items) so back-stack state is preserved.
- `onNavigateToUnitDetail` / `onNavigateToSessionDetail` push directly onto the shell back stack.
- `onEditUnit` / `onEditSession` call `unitActions.onEdit` / `sessionActions.onEdit` before navigating to the edit route (matching the existing pattern for edit-from-detail).
- `onCreateUnit` / `onCreateSession` call `onBeginNew()` then navigate to the create route (unchanged from before).

---

### 3. `PracticePlannerViewModelTest.kt` — 5 new tests

- `nextMoveStateIsNoUnitsWhenNoDataLoaded` — after sign-in with empty repos, `nextMoveState == NoUnits`.
- `nextMoveStateIsUnitsNoSessionsWithUnitsOnly` — units present, sessions absent → `UnitsNoSessions`.
- `nextMoveStateIsBothWhenUnitsAndSessionsExist` — both present → `Both`.
- `nextMoveStateIsResumeEditingAfterSavingUnitWhenSessionsExist` — edit and save an existing unit when sessions also exist → `ResumeEditing(id, isUnit = true)`. Note: `ResumeEditing` requires both units AND sessions to be non-empty; otherwise the function short-circuits to `UnitsNoSessions`.
- `firstRunStateRequiresHasLoadedToBeTrue` — `hasLoaded = false` before first sign-in (would-be first-run logic is false); after load with empty repos `hasLoaded = true` and the triple condition becomes true.

---

## Regression risks

**R-TAB-NAV**: The tab navigation lambdas (`onNavigateToUnits`, `onNavigateToSessions`) intentionally use `saveState = true` / `restoreState = true`. This means navigating from the Overview stat card to Units and then pressing Back returns to Overview rather than popping to a blank stack — consistent with standard bottom-nav behavior.

**R-DETAIL-BACKSTK**: `onNavigateToUnitDetail` / `onNavigateToSessionDetail` push onto the shell NavController without clearing back state. If the user was already deep in a detail and returns to Overview (e.g., via the bottom nav), then taps a recent card, two detail entries may stack. This matches the existing behavior for detail navigation from Lists (S5).

**R-FIRSTRUN-FLASH**: First-run layout is gated on `hasLoaded`. During the `isLoading = true` phase (before the first successful refresh), `hasLoaded` is false → Overview renders nothing (empty `ScrollableScreen`). This is intentional and avoids a flash of the first-run UI while data loads.

**R-RECENTS-GRACEFUL**: `RecentlyUsedSection` hides itself when `recentItems` is empty. The rest of the screen renders normally. This matches the S3 regression risk (R4).

**R-NEXTMOVE-RESUMEEDITING**: `ResumeEditing` depends on `savedUnitId` / `savedSessionId` in `PracticePlannerUiState`, which are consumed (nulled) when the editor navigation is triggered. If the user saves a unit then immediately navigates away from Overview before the next recomposition, the `savedUnitId` may already be consumed. The Next-move card will fall back to `Both`, which is safe.

**R-EDIT-SESSION-NAVBACK**: `onEditSession` calls `sessionActions.onEdit(sessionId)` which sets the session editor baseline. If the user taps "Resume" for a session then presses Back from the editor, they return to Overview with the session editor still populated. This is the same behavior as tapping "Edit" from the session detail screen.

---

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — green (67 tests pass).
- [ ] `.\gradlew.bat :androidApp:lintDebug` — clean.
- [ ] First-run state (zero units + zero sessions, `hasLoaded = true`): only the single-CTA welcome card is visible; stat row, Next move, and recents are hidden.
- [ ] During initial load (`hasLoaded = false`): blank screen (no flash of first-run layout).
- [ ] Returning user: greeting shows "Welcome back, {Name}" with email caption below.
- [ ] Stat card "Units · N" taps navigate to the Units tab; back returns to Overview.
- [ ] Stat card "Sessions · N" taps navigate to the Sessions tab; back returns to Overview.
- [ ] Next move — NoUnits branch: "Build your first practice unit" + "Create unit" button navigates to unit create.
- [ ] Next move — UnitsNoSessions branch: "Combine your units into a session template" + "New session" navigates to session create.
- [ ] Next move — Both branch: "Pick a session to run at the range" + "Open most recent" opens the most recently updated session detail.
- [ ] Next move — Both branch (no sessions in recents — edge case): button falls back to "New session".
- [ ] Next move — ResumeEditing branch: appears after saving a unit/session when both collections are non-empty; "Resume" navigates to the correct editor.
- [ ] Recently used strip: appears with 3–5 cards after loading; each card shows type chip, name, and metadata.
- [ ] Tapping a recent Unit card navigates to that unit's detail screen.
- [ ] Tapping a recent Session card navigates to that session's detail screen.
- [ ] Recents strip is hidden (not an empty placeholder) when `recentItems` is empty.
- [ ] `!dataConfigured` guard: shows "Planning unavailable" card; stat row / Next move / recents hidden.
- [ ] Phone layout (bottom bar): all sections visible, scrollable; no layout overflow.
- [ ] Tablet layout (navigation rail, width ≥ 840dp): two-column layout — left: greeting + stat cards; right: next move + recents.
- [ ] Duplicate "New unit" / "New session" buttons from the old WelcomeHomeCard are gone for returning users.
- [ ] TalkBack: stat cards announce "$label, $count, open $label" as a single target.
- [ ] TalkBack: recent cards announce "$name, $typeLabel, open" as a single target.
- [ ] Next move button has accessible label matching the contextual action text.
- [ ] Rotation / config change on Overview: state survives (ViewModel-backed).
- [ ] Terminology check: "Units" and "Sessions" are consistent across stat cards, recents chips, and navigation labels.
