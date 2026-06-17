# Stage 13 Changes

## Summary

- Added `StatProminence` and `BriefingStat` to support primary vs secondary summary metrics without introducing custom type sizes.
- Promoted Unit and Session detail ball totals to the primary metric role and kept supporting facts secondary.
- Moved Unit detail default-club information into the summary strip and removed the redundant lower default-club highlight card.
- Upgraded session item focus cues to a labelled, tinted treatment with an icon and preserved item notes as a separate text block below.
- Added the missing target-style icon to `FocusCard`.
- Constrained detail-route titles to a single line with ellipsis support through the shared top-bar title content.

## Regressions To Watch

- Three summary blocks can feel tight with long club names or large font scale; verify wrapping and truncation on narrow phones.
- The new session-item focus container changes row density, so check that notes still read as a separate concern and do not appear visually merged.
- Detail title truncation now depends on the shared title composable, so confirm Unit and Session detail bars still read well in both collapsed and expanded states.

## Validation Checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- [ ] Unit detail ball count is visibly larger than instruction count and default-club text.
- [ ] Session detail ball count is visibly larger than unit count and estimated-time text.
- [ ] Unit detail summary shows balls, instructions, and default club together.
- [ ] Session item focus cue shows an icon, label, and tinted container.
- [ ] Session item notes still appear separately below the focus cue when present.
- [ ] `FocusCard` shows its icon and still reads correctly with TalkBack.
- [ ] Long Unit and Session detail titles truncate with ellipsis instead of wrapping.
- [ ] Phone and tablet layouts verified.
