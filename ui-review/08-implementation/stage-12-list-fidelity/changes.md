# Stage 12 — List shell and card fidelity: Changes

## Summary of changes

### 1. List-route FAB rules now match the stage plan
Updated the authenticated shell so Units and Sessions follow the stage 12 thresholds exactly:
- Empty lists render no FAB.
- Lists with 1-2 items render the labelled extended FAB.
- Lists with 3+ items render the compact FAB.

Added `listFabStyleForCount()` coverage in `RangeworkNavigationTest` so the threshold policy is locked down by test.

### 2. Units and Sessions list routes now use a left-aligned small top app bar
Adjusted the shared app-shell route policy so `Units` and `Sessions` use a left-aligned `TopAppBar`, while existing detail routes keep the `MediumTopAppBar` collapsing treatment and other routes keep the centered treatment.

### 3. List cards now use the intended container and type hierarchy
Updated `ListEntryCard` to use a clickable `OutlinedCard`, promoted the title to `titleMedium`, and removed the old duplicated subtitle tier so the card reads as title, metadata, then preview/body.

### 4. List metadata is now composed as a single scan row
Reworked list metadata to match the redesign scan pattern:
- Units combine optional club chip plus instruction count and ball count in one metadata row.
- Sessions combine the ball-count pill plus item count in one metadata row.
- Supporting text remains the optional preview/body line only.

### 5. Metadata pills are now truly non-interactive
Replaced the previous clickable `AssistChip` implementation for `BallCountPill` and `ClubChip` with non-interactive `Surface`-based pills so list cards remain a single primary click target apart from overflow/swipe actions.

## Regression risks

| Risk | Mitigation |
|---|---|
| Global shell changes could affect non-list routes | The left-aligned small app bar is gated to `Units` and `Sessions` only; detail routes still use the existing collapsing medium app bar path |
| FAB state could use the wrong list count per tab | The shell computes Units and Sessions thresholds independently from `plannerUiState.units.size` and `plannerUiState.sessions.size` |
| Swipe wrappers could conflict with the new clickable card container | The swipe wrappers were left in place; only the inner card container changed from filled card + row click to clickable outlined card |
| Metadata pills could visually drift on detail screens because the shared pill components changed | The pill change stayed stylistically close to the old appearance, but detail screens should still be visually rechecked on phone and tablet |

## Validation checklist

- [x] `.\gradlew.bat :androidApp:testDebugUnitTest --tests com.loganmartlew.rangework.android.ui.RangeworkNavigationTest` — green
- [x] `.\gradlew.bat :androidApp:assembleDebug` — green
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- [ ] Manual: Empty Units list shows only the empty-state CTA and no FAB
- [ ] Manual: Empty Sessions list shows the dependency-aware CTA and no FAB
- [ ] Manual: Units and Sessions lists with 1-2 items show the extended FAB
- [ ] Manual: Units and Sessions lists with 3+ items show the compact FAB
- [ ] Manual: Unit and Session list titles are left-aligned small top-app-bar titles
- [ ] Manual: Unit and Session cards still tap through to detail
- [ ] Manual: Overflow menus and S11 swipe actions still work independently from card tap
- [ ] Manual: Metadata reads as one row without duplicate counts above it
- [ ] Manual: Phone and tablet layouts visually match the redesign intent
