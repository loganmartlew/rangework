# Stage 15 - Overview fidelity and final polish

> Post-delivery audit stage **S15**. Covers redesign-audit findings **#7 (overview/settings remainder), #10, and #11**. Source: `08-implementation/redesign-audit.md`.

## Objective

Close the remaining post-delivery audit gaps on the Overview and finish any non-list top-bar normalization left after S12-S14.

- Overview "Next move" eyebrow no longer uses the low-contrast `0.7f` alpha treatment (audit #10, systemic 9.1).
- Overview recents include the spec'd metadata: type chip plus headline metadata such as ball count and club where available (audit #11, B26/B27).
- The "Resume editing" next-move branch includes the entity name instead of generic copy (audit #11, B26).
- Overview, Settings, Manage clubs, and editor screens use the intended left-aligned M3 small top bar if still center-aligned after earlier stages (audit #7, B34).

## Dependencies

- **Upstream:** S12 list/card fidelity should land first so Overview recents can match the corrected card metadata language.
- **Upstream:** S13 detail/top-bar verification should land first so final top-bar policy does not disturb detail behavior.
- **Upstream:** S14 terminology repair should land first so Overview copy uses the final locked terms.
- **Related:** S10 contrast expectations still apply.

## Affected screens

- Overview.
- Settings and Manage clubs, if top-bar normalization remains.
- Unit and Session editors, if top-bar normalization remains.
- Shared app shell top-bar route policy.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/OverviewScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ListEntryCard.kt` or recent-card helper if Overview has a separate implementation.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModel.kt`
- Existing `PracticePlannerViewModelTest` / navigation tests for next-move state.

## Implementation steps

### 1. Fix Next move eyebrow contrast

In the Overview Next-move card, remove the remaining alpha-dimmed label color:

- Replace `onPrimaryContainer.copy(alpha = 0.7f)` with an AA-safe token.
- Prefer `onPrimaryContainer` or an established label color that passes contrast on the actual container.
- Re-check both light and dark themes.

This mirrors the S10 fix applied elsewhere.

### 2. Restore recents metadata

Update recent cards so each item shows:

- Name.
- Type chip: Unit or Session. Use `AssistChip` if practical; otherwise match the final S12 card pattern.
- Unit metadata: ball count and club where available.
- Session metadata: ball count and unit/item count where available.

Avoid the current fallback where unit recents show either balls or instruction count and sessions show only unit count. The recents strip should be useful enough for someone choosing what to resume.

### 3. Personalize Resume editing copy

Update the "just edited" branch so it includes the entity name:

- Example: `Resume editing "Wedge ladder"` or the project's established quote style.
- Preserve graceful fallback if the entity was deleted or the name is unavailable.
- Keep action routing unchanged.

If the current ViewModel state only exposes type, add the minimum extra display-name field needed for this branch. Do not add persistence unless the existing recents/last-edited source cannot provide it.

### 4. Finish top-bar normalization

Audit all non-detail authenticated routes after S12-S14:

- Overview.
- Unit list and Session list (should already be done in S12).
- Unit editor and Session editor.
- Settings and Manage clubs.

Any remaining `CenterAlignedTopAppBar` usage on these routes should become the spec'd left-aligned M3 small top bar. Preserve S11 collapsing detail bars.

If this is best handled centrally, encode a route policy in the shell:

- Detail routes: S11 collapsing `MediumTopAppBar`.
- Authenticated non-detail routes: pinned small top app bar.
- Login/pre-auth: unchanged.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Next-move eyebrow passes contrast in light and dark themes.
- [ ] Unit recents show type chip, ball count, and club where available.
- [ ] Session recents show type chip, ball count, and unit/item count where available.
- [ ] "Resume editing" copy includes the entity name when available.
- [ ] Missing/deleted recent entity falls back cleanly without crashing.
- [ ] Overview, editors, settings, and manage-clubs top bars are left-aligned small bars.
- [ ] Detail collapsing top bars from S11 still work.
- [ ] Phone and tablet Overview verified.

## Accessibility requirements

- Recents must expose type by text, not color alone.
- Recent cards should announce enough metadata to distinguish similarly named units/sessions.
- Next-move button and copy should remain descriptive after personalization.
- Top-bar title changes should not alter navigation/action button labels.

## Regression risks

- Overview is data-integrative. If a recent item references an entity that no longer exists, filter or fall back rather than crashing.
- Top-bar changes touch the global shell. Re-test detail, editor, settings, and list routes after central changes.
- Adding entity names to next-move state can tempt a broader data refactor. Keep the data addition minimal and display-oriented.
