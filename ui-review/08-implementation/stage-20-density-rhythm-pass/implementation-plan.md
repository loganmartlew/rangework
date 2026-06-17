# Stage 20 - Density and rhythm pass

> Post-delivery audit stage **S20**. Covers redesign-audit-2 finding **#9**. Source: `08-implementation/redesign-audit-2.md`.

## Objective

Finish the redesign with a spacing/density pass that improves scan speed without sacrificing clarity or touch comfort.

- Settings reads as a tighter, more native settings surface instead of a long, airy document (audit #9).
- Unit and Session editors feel less like tall card stacks and more like efficient editing tools (audit #9).
- Touch targets, helper text, and large-font readability remain intact.

## Dependencies

- **Upstream:** S17, S18, and S19 complete. This stage should tune against final interaction and layout behavior, not moving targets.

## Affected screens

- Settings.
- Manage clubs, if its spacing still feels out of family after S16.
- Unit editor.
- Session editor.
- Shared row/components used by those surfaces.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SettingsScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/ManageClubsScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionEditorScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ReorderableItemRow.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/CountStepper.kt`
- Any shared settings/editor spacing helpers already in use.

## Implementation steps

### 1. Run a compact-phone density audit

Before editing, capture the specific places where spacing is doing the wrong job:

- oversized gaps between settings groups/rows
- oversized editor row padding
- lower control bands that make rows feel like card-with-toolbar stacks

Use the compact phone layout as the primary calibration target.

### 2. Tighten Settings rhythm

Reduce unnecessary vertical whitespace while keeping the list calm and readable.

- tighten section-to-section spacing
- tighten row/divider rhythm where safe
- keep the surface clearly grouped without cardifying it again

Do not shrink semantic tap targets below accessibility expectations.

### 3. Tighten editor row structure

Focus on the repeated row surfaces, not on headline fields first.

- reduce redundant vertical padding inside repeated instruction/item rows
- reduce the visual separation between content and move/delete bands where practical
- keep steppers, drag handles, chevrons, and delete controls readable and touchable

The result should feel more like an editing tool and less like stacked content cards.

### 4. Recheck hierarchy after compaction

After tightening spacing, make sure the screens still communicate:

- what is the main field
- what is optional
- what is the primary action
- what is a destructive/reorder affordance

This stage is polish, not just compression.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Settings feels more compact/scannable on the baseline phone layout.
- [ ] Unit editor repeated rows feel denser without losing clarity.
- [ ] Session editor repeated rows feel denser without losing clarity.
- [ ] Touch-target sizes remain acceptable after spacing changes.
- [ ] Large-font and tablet passes still read correctly.
- [ ] Before/after screenshots confirm the rhythm improvement rather than just smaller gaps.

## Accessibility requirements

- Do not trade away 48dp targets to gain density.
- Helper text and error text must remain readable and not collide after spacing changes.
- Dividers and grouping should still help screen-scanning users orient themselves.

## Regression risks

- Density passes are easy to turn into global spacing churn. Keep edits scoped to settings/editor surfaces and their shared row helpers.
- Compacting repeated rows can accidentally crowd stepper and delete controls; verify real touch comfort, not just visual neatness.
