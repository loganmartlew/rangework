# Stage 19 - Create flow and onboarding polish

> Post-delivery audit stage **S19**. Covers redesign-audit-2 findings **#6 and #7**. Source: `08-implementation/redesign-audit-2.md`.

## Objective

Tighten the app's first-run and new-item moments so they read as intentional rather than half-finished.

- Unit create no longer presents a misleading `0 balls` default state in the first instruction row (audit #6).
- Login supporting copy fits cleanly without ellipsis on the baseline phone layout (audit #7).

## Dependencies

- **Upstream:** S16 complete so the final top-bar shell is already in place.
- **Upstream:** S18 complete so create-state editor layout is stable before we tune the default row behavior.

## Affected screens

- Login.
- Unit create.
- Potentially shared unit-editor state/validation helpers if the initial ball-count rule changes.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModel.kt`
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt`
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model/DraftValidation.kt`
- Existing editor/ViewModel tests that cover new-unit defaults and validation.

## Implementation steps

### 1. Decide and encode the unit-create ball-count rule

Close the mismatch between editor display and draft semantics.

Recommended direction:

- a newly created instruction row should start at a meaningful positive default such as `1`
- the UI should not render an unset optional value as a visible `0 balls` state

If the team prefers optional ball counts, render an explicit unset state instead of coercing blank into `0`.

### 2. Keep validation and editor display aligned

Once the product rule is chosen:

- align the editor's initial state
- align the stepper display
- align unit draft validation and any total/summary text

The user should never see a state that looks invalid but still saves, or a state that looks valid but fails unexpectedly.

### 3. Make the login value prop fit naturally

Fix the supporting text under the login headline so it does not truncate.

Preferred order:

1. tighten/rewrite the copy to fit the intended space
2. only relax line count if necessary after copy tuning

Keep the sign-in button and legal line comfortably above the bottom inset on the baseline phone layout.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] New unit create-state no longer shows a misleading `0 balls` default.
- [ ] Unit totals and validation still behave correctly after the default-state change.
- [ ] Login supporting copy fits cleanly without ellipsis on the baseline phone screenshot.
- [ ] Login screen still keeps the sign-in button and legal line on-screen and readable.
- [ ] Light and dark themes verified.

## Accessibility requirements

- Login supporting copy remains readable without relying on truncation.
- Stepper defaults and labels remain unambiguous to TalkBack.
- Validation messages still map cleanly to the underlying input if the default rule changes.

## Regression risks

- Changing the initial ball-count rule can affect tests and assumptions in create-state totals; recheck both new-unit UX and persistence behavior.
- Login copy changes are easy to overfit to one device size; verify the baseline phone first, then a slightly smaller/taller variant if available.

