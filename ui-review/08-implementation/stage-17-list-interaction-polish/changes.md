# Stage 17 - Changes

## Summary

Fixed swipe-background leakage on list cards at rest, and centralized the duplicated swipe background helper.

### Root cause

`SwipeActionBackground` always rendered a colored background regardless of swipe state. When `direction == Settled`, `isDelete` evaluated to `false`, so the composable rendered the `secondaryContainer` color (teal) with an Edit icon — visible around outlined cards even at rest.

### Changes made

**New file: `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/SwipeActionBackground.kt`**
- Extracted the swipe background helper from both list screens into a shared public component.
- Added an early return when `direction == SwipeToDismissBoxValue.Settled`, rendering nothing (transparent) in the resting state.
- Active directions (`StartToEnd` → edit, `EndToStart` → delete) continue to render the colored background and icon as before.

**Modified: `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitListScreen.kt`**
- Removed private `SwipeActionBackground` function.
- Added import for the shared component.
- Cleaned unused imports (`background`, `Box`, `fillMaxSize`, `padding`, `size`, `Delete`, `Edit`, `Icon`).

**Modified: `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionListScreen.kt`**
- Same removals and import cleanup as `UnitListScreen.kt`.

## Regressions

None identified. Build, tests, and assembly all pass. Swipe interaction logic (confirmValueChange, spring-back via returning `false`) is unchanged. Only the visual rendering of the settled state was modified.

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] Units list cards show no swipe-background leakage at rest (teal slab gone).
- [ ] Sessions list cards show no swipe-background leakage at rest.
- [ ] Swiping right still reveals the edit background and springs back.
- [ ] Swiping left still reveals the delete (error) background, triggers delete confirmation dialog, and springs back.
- [ ] Card tap still navigates to detail.
- [ ] Overflow menu (edit / delete / duplicate) still works independently from swipe.
- [ ] Phone and tablet list screenshots verified with no false-positive selected state.
