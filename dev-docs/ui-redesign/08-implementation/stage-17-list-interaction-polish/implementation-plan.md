# Stage 17 - List interaction polish

> Post-delivery audit stage **S17**. Covers redesign-audit-2 finding **#3**. Source: `08-implementation/redesign-audit-2.md`.

## Objective

Restore clean resting list cards while keeping the S11 swipe interaction model intact.

- Units and Sessions list cards no longer show swipe-background leakage in the resting state (audit #3).
- Active swipe still reveals the intended edit/delete affordance.
- Card tap-to-detail, overflow, and swipe all keep working together.

## Dependencies

- **Upstream:** S16 complete so list shells and titles are already stable before we recheck screenshots.
- **Upstream context:** S11 swipe actions and S12 list-card fidelity are already in place and must not regress.

## Affected screens

- Units list.
- Sessions list.
- Any shared swipe-background helper factored between the two screens.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitListScreen.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionListScreen.kt`
- Optional shared helper in `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/`

## Implementation steps

### 1. Make the resting swipe background inert

Update the swipe background rendering so the settled state is visually neutral.

- When the dismiss direction is settled, render no colored background.
- Keep the edit/delete containers only for active swipe directions.

This should remove the visible teal slab currently peeking around outlined cards at rest.

### 2. Preserve active-swipe feedback

Keep the existing interaction semantics:

- Start-to-end: edit affordance.
- End-to-start: delete affordance.
- The row springs back after triggering the action.

Only the resting visual state should change in this stage.

### 3. Recheck shape and clipping

Verify the swipe wrapper and outlined card agree on shape/clipping:

- no colored corners at rest
- no card-edge tearing while swiping
- no visual selected-state false positive

If the two screens still duplicate the same helper logic, this is a good stage to centralize the background helper without changing behavior.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Units list cards show no swipe-background leakage at rest.
- [ ] Sessions list cards show no swipe-background leakage at rest.
- [ ] Swiping right still triggers edit and springs back.
- [ ] Swiping left still opens delete confirmation and springs back.
- [ ] Card tap still opens detail.
- [ ] Overflow menu still works independently from swipe.
- [ ] Phone and tablet list screenshots verified.

## Accessibility requirements

- Swipe is still an enhancement, not the only path to edit/delete.
- Visible card state should not imply selection when no action is active.
- Edit/delete affordances remain color + icon, not color alone.

## Regression risks

- Gesture wrappers can be sensitive to clipping and background changes. Re-test horizontal swipe, vertical scroll, tap, and overflow separately.
- If a helper is centralized, keep the Units and Sessions behavior identical after the refactor.

