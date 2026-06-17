# Stage 16 — App bar and title system: Changes

## Summary

All changes are in a single file: `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`.

### 1. Detail screen title resolution (audit finding #1)

**Problem.** `currentRoute` is read from `navBackStackEntry?.destination?.route`, which returns the route *template* (e.g. `units/{unitId}`), not the concrete route. The previous `titleForRoute(route, plannerUiState)` overload tried to strip the prefix and match `{unitId}` against real entity IDs — which always fails — and fell back to the generic strings `"Unit"` / `"Session"`.

**Fix.** Replaced the call to `titleForRoute(currentRoute, plannerUiState)` in the `topBar` with a direct lookup using the already-correctly-derived `currentUnitId` / `currentSessionId` (which come from `navBackStackEntry?.arguments?.getString(…)`, not from the route template):

```kotlin
val topBarTitle: String = when {
    currentUnitId != null ->
        plannerUiState.units.firstOrNull { it.id == currentUnitId }?.title ?: "Unit"
    currentSessionId != null ->
        plannerUiState.sessions.firstOrNull { it.id == currentSessionId }?.name ?: "Session"
    else -> titleForRoute(currentRoute)
}
```

The generic `"Unit"` / `"Session"` fallback is retained for the case where the entity has been deleted or is not yet loaded.

### 2. Removed wordmark from top-level authenticated nav icon (audit finding #2)

**Problem.** When there was no back destination (`canNavigateBack == false`), the nav icon slot rendered `BrandWordmark`, which combines the mark SVG and the text `"Rangework"`. This produced double-titled bars: `Rangework  Overview`, `Rangework  Units`, `Rangework  Sessions`, `Rangework  Settings`.

**Fix.** Removed the `else` branch from `navigationIconContent`. Top-level routes now render no navigation icon — only the screen title in the title slot — giving clean, title-only small top bars.

The `BrandWordmark` import was removed as it became unused.

### 3. Manage Clubs top-bar consistency (audit finding #8)

No dedicated code change was required. After removing the wordmark from top-level bars (fix #2), there are now exactly two non-detail top-bar treatments:

- **Top-level** (Overview, Units, Sessions, Settings): no nav icon, title only.
- **Pushed non-detail** (editors, Manage Clubs): back arrow + left-aligned title + any route actions.

Manage Clubs already used the pushed pattern (back arrow, title, search/overflow actions) and needed no further changes.

### 4. Removed the broken `titleForRoute(route, plannerUiState)` overload

The two-arg overload was the source of the detail-title bug and is no longer called anywhere after fix #1. It was removed to avoid future confusion. The single-arg `titleForRoute(route: String): String` and all existing tests remain unchanged.

---

## Regression risks

- **`RangeworkApp.kt` is the shared authenticated shell.** Any mistake here affects all authenticated screens simultaneously. The changes are minimal and targeted: the title computation and the nav icon composition block.
- **Missing entity fallback.** If a unit/session is deleted while the detail screen is still on the back stack, the title falls back to `"Unit"` / `"Session"`. This matches the previous behavior and is correct.
- **No nav icon on top-level routes.** Removing the wordmark means top-level routes have an empty navigation icon slot. The screen title remains the only textual route cue, satisfying the accessibility requirement.

---

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — **BUILD SUCCESSFUL**
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean
- [ ] Unit detail app bar shows the actual unit title (e.g. `"3 Shot Shaping Drill"`), not `"Unit"`
- [ ] Session detail app bar shows the actual session name (e.g. `"Test"`), not `"Session"`
- [ ] Deleting/missing entity while on detail screen shows `"Unit"` / `"Session"` fallback without crashing
- [ ] Overview, Units, Sessions, and Settings top bars show title text only — no `Rangework` wordmark alongside the title
- [ ] Manage Clubs shows: back arrow + left-aligned `"Club bag"` title + search/overflow actions
- [ ] Existing collapsing detail bars (MediumTopAppBar on unit/session detail) still collapse on scroll
- [ ] Edit/delete/duplicate actions in detail bars still work correctly
- [ ] Phone layout (bottom nav bar) verified
- [ ] Tablet layout (navigation rail) verified
