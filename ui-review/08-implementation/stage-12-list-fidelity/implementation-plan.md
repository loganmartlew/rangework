# Stage 12 - List shell and card fidelity

> Post-delivery audit stage **S12**. Covers redesign-audit findings **#1, #7 (list portion), and #8**. Source: `08-implementation/redesign-audit.md`.

## Objective

Bring the Units and Sessions list screens back into line with the delivered redesign spec where the implementation drifted after S5/S11.

- Empty lists show one clear create action: the inline empty-state CTA only. The FAB is hidden when the list is empty (audit #1, B02).
- Sparse lists with 1-2 items use the labelled Extended FAB. Lists with 3+ items use the compact FAB (audit #1, B57).
- List screens use the spec'd left-aligned M3 small top app bar instead of the global centered title treatment (audit #7, B34).
- List cards match the spec'd scan pattern: clickable `OutlinedCard`, `titleMedium` title, and a single metadata row combining the club chip plus count text (audit #8, B20/B13).

## Dependencies

- **Upstream:** S1-S11 complete. S11 swipe gestures may wrap list cards, so keep swipe behavior intact while changing card internals.
- **Downstream:** S15 Overview recents should consume the corrected card metadata pattern where practical.

## Affected screens

- Units list.
- Sessions list.
- Shared authenticated app shell for list top-bar behavior.
- Shared `ListEntryCard` / `RangeworkFab` call sites.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ListEntryCard.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/RangeworkFab.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitListScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionListScreen.kt`
- Existing `androidApp` UI/ViewModel tests where list behavior is already covered.

## Implementation steps

### 1. Correct the FAB visibility rules

In the app shell or list-screen FAB decision point:

- When the current list is empty, render no FAB. The empty-state card's CTA is the sole create action.
- When the list has 1 or 2 items, render the Extended FAB with text ("New unit" / "New session").
- When the list has 3 or more items, render the compact FAB.

Keep the existing create callbacks and `primaryContainer` styling. This is a behavior correction, not a restyle.

### 2. Normalize list top bars

Replace centered app-bar rendering for list routes with a left-aligned M3 small top app bar.

- Apply only to list routes in this stage.
- Preserve navigation icons, trailing actions, scroll behavior, and any S11 detail-screen app-bar behavior.
- Do not change editor, settings, or overview top bars here unless the shell abstraction makes the list-only change impossible without centralizing the route policy.

### 3. Restore the list card container and type hierarchy

Update `ListEntryCard` so list cards render as clickable `OutlinedCard`s.

- Title uses `MaterialTheme.typography.titleMedium`.
- Counts and club live in one metadata row instead of separate vertical tiers.
- Preserve the trailing overflow menu and any S11 swipe wrapper behavior.
- Keep 48dp touch targets and card tap-to-detail behavior.

### 4. Rework metadata composition per list type

Units list metadata should combine:

- Club chip, when a default club is available.
- Instruction count and ball count text.

Sessions list metadata should combine:

- Session headline counts, with ball count prominent enough to remain scannable.
- Unit/session item count text as the secondary part of the same row.

Avoid duplicating the same count in subtitle and metadata. The card should read as title, metadata row, optional preview/body.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Empty Units list shows the empty-state CTA and no FAB.
- [ ] Empty Sessions list shows the correct dependency-aware CTA and no FAB.
- [ ] Lists with 1 and 2 items show the Extended FAB.
- [ ] Lists with 3+ items show the compact FAB.
- [ ] Unit and Session list titles are left-aligned small top-bar titles.
- [ ] Cards remain tappable and still navigate to detail.
- [ ] Overflow menu and S11 swipe actions still work.
- [ ] Metadata row contains club/count information in one row and does not duplicate content above it.
- [ ] Phone and tablet layouts verified.

## Accessibility requirements

- Empty-state CTA remains a standard accessible button with a clear label.
- FAB content descriptions still match the visible action.
- Outlined cards remain a single accessible click target, with overflow and swipe actions exposed separately.
- Metadata order should read naturally in TalkBack: title, metadata, preview/action.

## Regression risks

- FAB state lives near route/tab shell code; verify both Units and Sessions use their own item counts and do not leak state across tabs.
- S11 swipe wrappers can be sensitive to card container changes. Check horizontal swipe, vertical scroll, tap, and overflow separately.
- The top app bar is global in the shell. Keep the route policy explicit so detail collapsing behavior from S11 does not regress.
