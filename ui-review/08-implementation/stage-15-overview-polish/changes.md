# Stage 15 Changes

## Summary

- Updated Overview's Next-move eyebrow to use full `onPrimaryContainer` contrast instead of the remaining `0.7f` alpha treatment.
- Restored richer recent-card metadata on Overview:
  - Unit recents now show ball count plus default club when available.
  - Session recents now show ball count plus item and unit counts.
  - Recent cards now use an `AssistChip` type label and more descriptive accessibility text.
- Personalized the Resume-editing branch so it uses the saved entity name when available, with a safe fallback if the saved entity no longer exists.
- Normalized authenticated non-detail top bars in the app shell to the left-aligned M3 small top app bar while preserving collapsing medium detail bars.
- Added shared and Android test coverage for the new next-move naming and missing-entity fallback behavior.

## Regressions

- No automated regressions were observed in unit tests, lint, or `assembleDebug`.
- Manual verification is still recommended for Overview contrast, recent-card layout density, and top-bar behavior on phone and tablet layouts.
- Existing Gradle warnings about deprecated `menuAnchor()` usage remain elsewhere in the app and were not introduced by this stage.

## Validation Checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- [x] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- [ ] Next-move eyebrow passes contrast in light and dark themes.
- [ ] Unit recents show type chip, ball count, and club where available.
- [ ] Session recents show type chip, ball count, and unit/item count where available.
- [ ] "Resume editing" copy includes the entity name when available.
- [x] Missing/deleted recent entity falls back cleanly without crashing.
- [ ] Overview, editors, settings, and manage-clubs top bars are left-aligned small bars.
- [ ] Detail collapsing top bars from S11 still work.
- [ ] Phone and tablet Overview verified.
