# Stage 10 — Accessibility hardening sweep: Changes

## Summary of changes

### 1. `FocusCard.kt` — Contrast fix
Removed `.copy(alpha = 0.7f)` from the "FOCUS CUE" label colour. The dimmed colour failed WCAG AA in dark mode (~4.09:1 vs required 4.5:1 for small text). Full `onSecondaryContainer` opacity passes in both light (~5.3:1) and dark (~6.5:1) modes.

### 2. `OverflowMenu.kt` — Destructive item non-colour signalling
Added a leading `Icons.Default.Delete` icon (tinted `error`) to the Delete `DropdownMenuItem`. The action was previously signalled by colour alone. Icon + label + confirmation dialog now cover all three pillars.

### 3. `ActionRow.kt` — Destructive button non-colour signalling
Added `Icons.Default.Delete` (18dp) and a spacer inside the `OutlinedButton` for the secondary destructive action. The button was previously error-coloured text only.

### 4. `DeleteConfirmationDialog.kt` — Destructive confirm non-colour signalling
Added `Icons.Default.Delete` (18dp) and a spacer before the "Delete permanently" label inside the confirm `TextButton`. The button was previously error-coloured text only.

### 5. `ManageClubsScreen.kt` — Club row touch targets and semantics
Three improvements to club toggle rows:
- **Touch target**: Added `heightIn(min = 48.dp)` to each club row to meet the 48dp minimum interactive target requirement.
- **TalkBack state description**: Disabled state now announces `"${club.displayName}, disabled"` instead of `"Enable ${club.displayName}"`, matching the format `"{club}, enabled/disabled"` required by the plan.
- **Toggleable state semantics**: Added `toggleableState = if (enabled) ToggleableState.On else ToggleableState.Off` to the row `semantics {}` block, aligning with the `SettingsListItem` pattern.

### 6. `SettingsScreen.kt` — Clickable row roles
Added `role = Role.Button` to the four interactive rows that were missing it:
- Club bag (navigates to ManageClubs)
- Help (opens bottom sheet)
- Feedback (opens email intent)
- Privacy (opens bottom sheet)

TalkBack will now announce these as "button" after the label, enabling users to understand they are activatable controls.

## No-change rationale

- **`ReorderButtons.kt`**: Has generic "Move up"/"Move down"/"Remove" descriptions on Icons, which are correct for TalkBack (icon's contentDescription surfaces as the button's accessible name). No screens use this component directly — `ReorderableItemRow` (which already has parameterised content descriptions) is used instead.
- **`ReorderableItemRow.kt`**: Already uses named semantics (`moveUpContentDescription`, `moveDownContentDescription`, `deleteContentDescription`) and `Modifier.size(48.dp)` on all icon buttons.
- **`CountStepper.kt`**: Already has `Modifier.size(48.dp)` and contextual `contentDescription` via semantics.
- **`SettingsListItem.kt`**: Already has `heightIn(min = 56.dp)`, `role = Role.Switch`, `toggleableState`, and a full-row clickable.
- **Nav bar icons**: `NavigationBarItem` and `NavigationRailItem` with `label` set — Material handles a11y natively; icon `contentDescription = null` is correct.
- **App-bar back button**: Has `contentDescription = "Back"`.
- **FAB**: `RangeworkFab` takes an explicit `contentDescription`; all call sites supply a meaningful string.
- **Theme tokens**: No contrast fixes required a token change; per-component fix in FocusCard is sufficient.

## Regression risks

| Risk | Mitigation |
|---|---|
| OverflowMenu Delete item is now wider (icon + text) | The icon is a standard 24dp Material icon; dropdown menu handles variable-width items natively |
| DeleteConfirmationDialog confirm button wider | AlertDialog reflows confirm/dismiss buttons automatically; no layout breakage expected |
| ActionRow secondary button now taller/wider | ActionRow is not referenced in any screen — isolated component; still tested by unit build |
| FocusCard label lighter in light mode (was 70% opacity, now 100%) | Very minor visual shift; still within the card's `onSecondaryContainer` semantic; does not affect contrast budget |
| ManageClubs row heightIn could shift adjacent divider spacing | `heightIn(min = 48.dp)` only sets a floor; rows that already exceed 48dp are unaffected |

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — green
- [ ] `.\gradlew.bat :androidApp:lintDebug` — run after to check for remaining lint accessibility warnings
- [ ] **TalkBack pass** — all icon-only controls (edit, overflow, reorder, delete, search, FAB) announce meaningful labels
- [ ] TalkBack: Club bag, Help, Feedback, Privacy rows read as "button" after the label
- [ ] TalkBack: Each club row reads `"{club}, enabled"` or `"{club}, disabled"` (not "Enable {club}")
- [ ] TalkBack: Club rows reach focus individually and announce switch state
- [ ] Visual: FocusCard label ("FOCUS CUE") renders at full opacity — verify in light and dark
- [ ] Visual: OverflowMenu "Delete" row shows trash icon + red text
- [ ] Visual: DeleteConfirmationDialog confirm button shows trash icon + "Delete permanently"
- [ ] Visual: ActionRow secondary button shows trash icon + label
- [ ] Touch target: Club rows are tappable across their full 48dp height
- [ ] Contrast: FocusCard label meets AA in both themes (light ~5.3:1, dark ~6.5:1)
- [ ] No layout regressions on phone (compact) or tablet (expanded) layouts
- [ ] Font scaling: re-check ManageClubs rows at large system font sizes; `heightIn` ensures minimum but text wrapping should still be verified
