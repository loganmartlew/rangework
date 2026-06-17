# Stage 16 - App bar and title system

> Post-delivery audit stage **S16**. Covers redesign-audit-2 findings **#1, #2, and #8**. Source: `08-implementation/redesign-audit-2.md`.

## Objective

Replace the remaining inconsistent authenticated app-bar treatments with one explicit shell policy.

- Detail screens show the real unit/session name instead of generic `Unit` / `Session` (audit #1).
- Top-level authenticated routes stop rendering `Rangework + Screen` and use title-only small top bars (audit #2).
- Manage clubs uses the same pushed non-detail app-bar treatment as other back-stack screens instead of a third visual pattern (audit #8).

## Dependencies

- **Upstream:** S15 complete. Its recents/name work should already have stabilized Overview copy and the first top-bar normalization pass.
- **Downstream:** S17, S18, and S19 should build on the final authenticated shell/title behavior from this stage.

## Affected screens

- Overview.
- Units list.
- Sessions list.
- Settings.
- Manage clubs.
- Unit detail.
- Session detail.
- Shared authenticated app shell.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/BrandWordmark.kt`
- Existing navigation/UI tests that already cover route policy or title rendering.

## Implementation steps

### 1. Define the authenticated app-bar route matrix

In the app shell, make the route policy explicit rather than implicit:

- Top-level authenticated routes: small top bar, screen title only.
- Pushed non-detail routes: back button + left-aligned screen title + any route actions.
- Detail routes: entity title + existing collapsing behavior.

Keep the login/pre-auth experience unchanged in this stage.

### 2. Fix detail title resolution

Update the detail-title path so it resolves entity names from nav arguments / looked-up entities, not from the route template string.

- Reuse the already-derived `currentUnitId` / `currentSessionId`.
- Look up the matching unit/session from `plannerUiState`.
- Fall back to generic copy only if the entity is missing.

Preserve truncation and the current edit/delete actions.

### 3. Remove the top-level wordmark title duplication

Stop injecting `BrandWordmark` into authenticated top-level app bars.

- Keep the title slot as the only text label on Overview, Units, Sessions, and Settings.
- If a leading icon is still desired, use a compact decorative mark only if it does not reintroduce textual duplication.

The main goal is to eliminate the current `Rangework + Screen` double-title treatment.

### 4. Normalize Manage clubs onto the pushed-screen treatment

Audit the Manage Clubs route after the shell changes:

- Back arrow remains.
- Title is left-aligned and consistent with other pushed non-detail routes.
- Search and overflow actions remain intact.

Do not change list content, search behavior, or presets here.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Unit detail shows the actual unit title in the app bar.
- [ ] Session detail shows the actual session name in the app bar.
- [ ] Missing/deleted entity falls back cleanly without crashing.
- [ ] Overview, Units, Sessions, and Settings show title-only authenticated top bars.
- [ ] Manage clubs uses the pushed non-detail top-bar treatment and retains its actions.
- [ ] Existing collapsing detail bars still work.
- [ ] Phone and tablet screenshots verified.

## Accessibility requirements

- App-bar titles remain text, not icon-only, on every route.
- Back, edit, search, overflow, and delete controls keep clear content descriptions.
- Removing the wordmark must not remove the only textual route cue.

## Regression risks

- `RangeworkApp.kt` owns the shared top-bar shell, so mistakes here affect most authenticated screens at once.
- Title resolution for detail routes should be minimal and display-oriented; avoid a broader navigation refactor.

