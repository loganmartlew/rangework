# Stage 19 — Changes

## Summary

### 1. Unit create — default ball count (`0 balls` → `1`)

**`androidApp/…/ui/PracticePlannerViewModel.kt`**
- Changed `PracticeInstructionEditorState.ballCount` default from `""` to `"1"`.
  Every newly created instruction row (via "Add instruction" or the initial row on new-unit create) now opens at 1 ball instead of the misleading 0.
  The reset state used by `removeInstruction` (last-row guard) inherits the same default.

**`androidApp/…/ui/screens/UnitEditorScreen.kt`**
- Changed stepper display fallback `toIntOrNull() ?: 0` → `?: 1`.
  Instructions loaded from the server with a null ball count now render as 1 rather than 0.
- Changed `CountStepper(min = 0)` → `min = 1`.
  The decrement button is now disabled at 1, preventing the user from landing in the `0 balls` state that would cause a validation error on save.

### 2. Login supporting copy — truncation fix

**`shared/…/usecase/AppBootstrapMessageUseCase.kt`**

| State | Old detail (140 / 128 chars) | New detail (~97 / ~104 chars) |
|---|---|---|
| Configured | "Create repeatable practice units, build session templates from them, and pick up the same plan whenever you come back on {platform}." | "Build repeatable units, assemble them into sessions, and pick up the same plan on {platform}." |
| Unconfigured | "Rangework is set up for focused practice planning. Sign-in still needs to be enabled on this build before your plans can sync across devices." | "Rangework is set up for focused practice planning — enable sign-in before plans can sync across devices." |

Both rewrites keep the exact substrings asserted by existing tests:
- `"pick up the same plan"` (configured, `AppBootstrapMessageUseCaseTest`)
- `"focused practice planning"` (unconfigured, `AppBootstrapMessageUseCaseTest`)
- `"plans can sync across devices"` (unconfigured, `AppBootstrapBridgeTest`)

**`androidApp/…/ui/RangeworkApp.kt`**
- Raised `maxLines` from `2` to `3` so the tightened copy can wrap naturally.
- Removed `overflow = TextOverflow.Ellipsis`; default clip behaviour means no visible truncation indicator even if a device renders slightly wider text.

## Regression risks

- **Ball-count default change affects `beginNewUnit`, `addInstruction`, and the last-row guard in `removeInstruction`.** All three now start at `"1"` instead of `""`. Saving without touching ball count sends `"1"` to `parseOptionalInt`, which returns `1` — a valid positive value. This is a semantic change from the previous "unset" default, but intentional per the audit.
- **Stepper min raised to 1.** Users can no longer decrement to 0 via the UI. Existing saved instructions that have a null ball count will display as 1 in the stepper (via the `?: 1` fallback); if the user saves without touching the field the ViewModel still holds `"1"` and persists 1. This is a minor data-enrichment side-effect when re-editing old units.
- **Login copy change.** Both detail strings are shorter. The headline is unchanged.

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — green (67 tasks, 0 failures).
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` — run before shipping.
- [ ] Open unit create on a device/emulator — first instruction row shows `1` not `0`.
- [ ] Tap "Add instruction" — new rows start at `1`.
- [ ] Decrement at `1` — minus button is disabled.
- [ ] Remove all instructions until only one remains — reset row shows `1`.
- [ ] Unit totals and the "Total" line update correctly from the new default.
- [ ] Save a unit without setting a ball count — saves with `ballCount = 1`.
- [ ] Edit an existing unit with a null ball count — ball count field shows `1`, not `0`.
- [ ] Login screen on baseline phone (360dp): supporting copy wraps naturally without ellipsis.
- [ ] Login screen: sign-in button and legal line remain on screen with the wider copy area.
- [ ] Light and dark themes — both login and unit editor look correct.
