# Stage 13 - Detail briefing and focus fidelity

> Post-delivery audit stage **S13**. Covers redesign-audit findings **#2, #3, #6, #7 (detail remainder if needed), and #9**. Source: `08-implementation/redesign-audit.md`.

## Objective

Restore the detail screens' at-a-glance hierarchy and focus-cue treatment. The redesign intended details to be fast to scan at the range: ball count should dominate, summary facts should sit together, and focus cues should look intentional.

- Ball count is the dominant numeral in Unit detail and Session detail summary strips (audit #2, B13).
- Unit detail summary includes all three intended facts: balls, instructions, and default club (audit #3).
- Session item focus cues are labelled, tinted, and visually distinct (audit #6, B16/B60).
- `FocusCard` includes the intended target/center-focus icon (audit #9, B16).
- Any detail top-bar leftovers from audit #7 are checked against S11 and corrected, including title truncation behavior for long names.

## Dependencies

- **Upstream:** S11 detail changes, especially collapsing top app bar and session item notes, should be present.
- **Related:** S12 may update shared typography/card patterns, but this stage can proceed independently.

## Affected screens

- Unit detail.
- Session detail.
- Shared detail components: `StatBlock`, `BriefingRow`, `FocusCard`, and session item row rendering.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/StatBlock.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/FocusCard.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitDetailScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionDetailScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`

## Implementation steps

### 1. Add stat prominence support

Extend `StatBlock` with a small prominence API, for example:

```kotlin
enum class StatProminence { Primary, Secondary }
```

Then map:

- `Primary` value text to a larger numeric role, such as `headlineSmall` or the established `RangeworkMono` large metric role.
- `Secondary` value text to the existing metric size.
- Captions remain visually quieter and use normal Material typography.

Keep numeric values in `RangeworkMono`. Do not introduce anonymous type sizes.

### 2. Mark ball totals as primary

Update Unit detail and Session detail briefing rows:

- Ball count uses `StatProminence.Primary`.
- Instruction count, unit count, and estimated time remain secondary.

Verify the row still fits at large font sizes and narrow widths. If the row becomes cramped, prefer wrapping/weight changes over shrinking below the intended hierarchy.

### 3. Add default club to Unit detail summary

Move the default club from a lower "Default club" highlight into the Unit detail briefing strip as the third summary block.

- If a unit has a default club, show it as a compact chip/block in the summary row.
- If no default club is set, show a clear fallback such as "No club" only if the existing design uses explicit absence states. Otherwise omit the chip but preserve row balance.
- Remove or demote the redundant lower default-club card if it duplicates the same information.

The end state should be a three-fact Unit detail summary matching the spec: balls, instructions, default club.

### 4. Upgrade session item focus cues

In `SessionItemDetailRow`, replace the plain grey focus cue text with a labelled/tinted treatment.

- Add a small focus/target icon.
- Add a label such as "Focus cue".
- Use a tinted container or text color aligned with `FocusCard` / `secondaryContainer`.
- Preserve S11 session item notes below it, if present, and keep the two pieces visually distinct.

This can be a compact inline row rather than a full card. The important part is that the cue is no longer an anonymous sentence.

### 5. Add the missing icon to `FocusCard`

Add the target/center-focus icon to the shared `FocusCard`.

- Use the project's existing Material icon set.
- Keep the icon decorative if the card text already announces the purpose.
- Preserve the current tint and spacing intent.

### 6. Audit detail top-bar leftovers

Confirm S11's detail top-bar changes fully handle audit #7 for Unit and Session detail.

- Detail titles should left-align in collapsed/expanded states.
- Long unit/session names should use `maxLines = 1` and ellipsis where required by the spec.
- Do not alter list/editor/settings/overview bars here unless a shared helper requires it.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Unit detail ball count is visibly larger than instruction count/default-club text.
- [ ] Session detail ball count is visibly larger than unit count/time.
- [ ] Unit detail summary exposes balls, instructions, and default club together.
- [ ] Session item focus cue has icon, label, and tint.
- [ ] Session item notes still display correctly and do not blend into focus cues.
- [ ] `FocusCard` displays its icon and still handles empty/missing cue states.
- [ ] Long detail titles truncate gracefully.
- [ ] Phone and tablet layouts verified.

## Accessibility requirements

- Focus cue meaning must not rely on color alone. The icon and label carry meaning with the tint.
- The `FocusCard` icon should not create a redundant TalkBack stop if the card title/value already communicate the content.
- Summary stats should read in a sensible order: balls first, then supporting facts.
- Large font scale should not clip the primary ball total.

## Regression risks

- Enlarging the ball stat can crowd the summary row. Test short and long values, especially three-digit ball totals.
- Moving the default club could duplicate information if the lower card remains. Remove or reframe the lower card deliberately.
- Session item rows already gained notes in S11. Keep notes and focus cues separate enough that a user can tell which is which.
