# Rangework Redesign — Delivery Audit (v2)

Second-pass review comparing the delivered redesign against the intended `07-redesigns/*` specs, the `08-implementation/master-roadmap.md` change inventory, the v1/v2 screenshot sets, and the current app code.

This pass focuses on gaps still visible after the first re-audit implementation wave. Findings are ordered by user impact, highest first.

## Scope checked

- Screenshot sets:
  - `ui-review/08-implementation/screenshots-v1`
  - `ui-review/08-implementation/screenshots-v2`
- Redesign specs:
  - `ui-review/07-redesigns/*`
  - `ui-review/08-implementation/master-roadmap.md`
  - `ui-review/08-implementation/redesign-audit-1.md`
- Code surfaces:
  - `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt`
  - `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/*`
  - `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/*`

## Summary table

| # | Finding | Evidence | Impact |
|---|---|---|---|
| 1 | Detail screen app bars show generic `Unit` / `Session`, not entity names | v1 + v2 detail screenshots; `RangeworkApp.kt` route-title lookup | High |
| 2 | Authenticated top-level bars still show `Rangework + Screen`, not title-only small bars | v1 + v2 overview/list/settings screenshots; `BrandWordmark` in nav icon | High |
| 3 | Swipe action backgrounds leak around resting list cards | v2 list screenshots; swipe background paints even when settled | Medium-high |
| 4 | Editor pinned bars can overpaint active form content while scrolling | v1 + v2 editor screenshots; nested editor `Scaffold` + sticky total | Medium-high |
| 5 | Session editor sticky total collides with scrolled item content | v2 `session-edit-2.png`; sticky header overlays item field | Medium |
| 6 | Unit create starts the ball stepper at visible `0` despite blank optional state | v2 `unit-create.png`; editor state and validation allow blank | Medium |
| 7 | Login value-prop text is truncated with ellipsis | v2 `login.png`; login detail text forced to 2 lines | Medium |
| 8 | Manage Clubs top bar remains visually centered and inconsistent with the normalized small-bar goal | v2 `settings-clubs.png`; app-shell bar policy | Low-medium |
| 9 | Settings and editor screens remain very loose/dense in the wrong places | v2 settings/editor screenshots | Low-medium |
| 10 | Some v1 audit fixes lack manual-verification closure | stage 12/13/15 checklists | Low |

## High impact

### 1. Detail screen app bars show generic `Unit` / `Session`, not entity names

**Spec.** Detail app bars should carry the actual unit/session name, truncating as needed:

- Unit detail: title `"3 Shot Shaping Drill"`.
- Session detail: title `"Test"`.

The detail specs explicitly moved the entity title into the app bar so the duplicate H1 could be dropped.

**Delivered.** Both v1 and v2 screenshots show generic titles:

- `screenshots-v2/unit-detail.png` shows `Unit`.
- `screenshots-v2/session-detail.png` shows `Session`.

This is more than a cosmetic mismatch. A user entering detail from recents, reverse links, or a list loses their orientation at the exact moment the detail screen is supposed to confirm what they opened.

**Code cause.** `currentRoute` is read from `navBackStackEntry.destination.route` in `RangeworkApp.kt`, which is the route template (`units/{unitId}` / `sessions/{sessionId}`), not the concrete route. `titleForRoute(route, plannerUiState)` then tries to remove the prefix and match `{unitId}` against real IDs, fails, and falls back to `"Unit"` / `"Session"`:

- `RangeworkApp.kt:383`
- `RangeworkApp.kt:614-619`
- `RangeworkApp.kt:1068-1077`

The same file already reads the real ID correctly for detail actions via `navBackStackEntry.arguments` at `RangeworkApp.kt:443-448`, so the title path should use the same argument-derived IDs.

**Fix direction.** Derive the detail title from `currentUnitId` / `currentSessionId`, not from `destination.route`. Keep the generic fallback only for missing/deleted entities.

### 2. Authenticated top-level bars still show `Rangework + Screen`, not title-only small bars

**Spec.** Overview, Units, Sessions, Settings, and editor specs repeatedly call for M3 Small TopAppBar with the screen title only:

- `"Overview"` only.
- `"Units"` only.
- `"Sessions"` only.
- `"Settings"` only.
- `"Edit unit"` / `"Edit session"` only.

The roadmap frames this as dropping the old `"Rangework / Screen"` double-title to recover vertical and horizontal space.

**Delivered.** v1 and v2 screenshots still show `Rangework` next to the screen title on top-level routes:

- `screenshots-v2/overview.png`: `Rangework Overview`
- `screenshots-v2/unit-list.png`: `Rangework Units`
- `screenshots-v2/session-list.png`: `Rangework Sessions`
- `screenshots-v2/settings-1.png`: `Rangework Settings`

This is especially visible on Overview, where the first viewport already carries a long greeting; the bar consumes space while repeating the app brand already established by the icon and nav shell.

**Code cause.** On routes without back navigation, the shared top bar injects `BrandWordmark` as the navigation icon:

- `RangeworkApp.kt:621-630`

That component includes the word `Rangework`, so the app bar is not actually title-only even after stages 12 and 15.

**Fix direction.** Use only the compact brand mark, or no navigation icon, for top-level authenticated bars. Keep the title slot as the single textual screen title.

## Medium-high impact

### 3. Swipe action backgrounds leak around resting list cards

**Spec.** Swipe actions were deferred work, but the final list card state should still read as a clean clickable `OutlinedCard`. Swipe affordances should appear during the gesture, not as visible slabs in the resting state.

**Delivered.** In v2 list screenshots, a teal/secondary rectangle peeks out around the card edges:

- `screenshots-v2/unit-list.png`
- `screenshots-v2/session-list.png`

The v1 cards did not show this. It makes the cards look selected, clipped, or partially swiped while they are at rest.

**Code cause.** `SwipeActionBackground()` paints `secondaryContainer` for every non-delete state, including `SwipeToDismissBoxValue.Settled`:

- `UnitListScreen.kt:167-188`
- `SessionListScreen.kt:171-192`

Because the background fills the whole swipe box, the new outlined card shape exposes that background at the corners and sides.

**Fix direction.** Render a transparent/no-op background when `direction == Settled`, or clip the background to the same shape and only reveal it while a swipe is actually in progress.

### 4. Editor pinned bars can overpaint active form content while scrolling

**Spec.** Docked save bars should keep Save reachable without hiding fields. The S7 validation checklist explicitly calls out IME/docked Save overlap, and the redesign's whole editor promise is to make dense editing safer.

**Delivered.** Both v1 and v2 screenshots show the bottom Save surface covering form content at common scroll positions:

- `screenshots-v2/unit-edit-1.png`: the Save bar covers the first instruction's ball-count row.
- `screenshots-v2/session-edit-1.png`: the Save bar covers the item notes area.
- v1 shows the same pattern in `screenshots-v1/unit-edit-1.png`.

At the very bottom of the list, the final Add/Save actions are reachable, so this is not a total blocker. But mid-scroll it obscures the exact controls users are editing and makes the form feel unreliable.

**Code shape.** The editors use nested screen-level `Scaffold`s with `DockedSaveBar` as an inner `bottomBar`:

- `UnitEditorScreen.kt:74-89`
- `SessionEditorScreen.kt:94-109`
- `DockedSaveBar.kt:17-34`

**Fix direction.** Re-check the editor scaffold nesting against the outer authenticated shell and add enough effective bottom inset/content padding for the editor viewport. Verify with small-phone screenshots while focused on the lower fields and with the keyboard open.

## Medium impact

### 5. Session editor sticky total collides with scrolled item content

**Spec.** The sticky total should be pinned under the app bar and keep the live total visible while the rest of the form scrolls cleanly below it.

**Delivered.** `screenshots-v2/session-edit-2.png` shows the `TOTAL 18 balls · ~5min` strip covering the top of the session item card / practice-unit field. This looks like a layering issue rather than a deliberate pinned header.

**Code cause.** The total is implemented as a `LazyColumn` `stickyHeader`:

- `SessionEditorScreen.kt:112-117`

Sticky headers draw over list content unless spacing and content offsets are handled carefully. In the screenshot, the item content is visibly underneath it.

**Fix direction.** Move the total bar outside the scrolling list, or add a stable top content offset so no item field can scroll underneath the pinned bar.

### 6. Unit create starts the ball stepper at visible `0` despite blank optional state

**Spec.** B05 describes ball-count steppers as a way to avoid invalid keyboard entry for small integer counts. The create flow should start from a useful small positive count or show blank/placeholder state, not present `0 balls` as a normal value.

**Delivered.** `screenshots-v2/unit-create.png` shows the initial instruction with:

- stepper value `0`
- total `0 balls`
- an enabled-looking Save bar

**Code cause.**

- The default editor instruction has blank `ballCount`: `PracticeInstructionEditorState(order = 1)` in `PracticePlannerViewModel.kt:48-49`.
- The draft validation only rejects `ballCount <= 0` when `ballCount != null`; blank parses to optional/null and is allowed by validation in `DraftValidation.kt:16-20`.
- The UI display coerces blank into `0` for the stepper display.

This means the UI appears to be tracking a concrete zero-ball instruction even though persistence treats the count as optional.

**Fix direction.** Decide the product rule: if instruction ball count is required, initialize to `1` and enforce it; if optional, render an unset state instead of `0` and avoid calling it a totalled ball count until set.

### 7. Login value-prop text is truncated with ellipsis

**Spec.** The login redesign wants a short, legible value prop beneath the headline. It should not look like body copy was arbitrarily cut off.

**Delivered.** `screenshots-v2/login.png` truncates the supporting copy:

> `Create repeatable practice units, build session templates from them, and pick up t...`

**Code cause.** The login detail text is forced to two lines with ellipsis:

- `RangeworkApp.kt:304-310`

This is a polished first-impression screen, so truncation is more visible than it would be in a dense table/list.

**Fix direction.** Rewrite the copy to fit two lines naturally, or allow another line on the baseline phone layout while keeping the sign-in button above the bottom legal line.

## Low-medium impact

### 8. Manage Clubs top bar remains visually centered and inconsistent with the normalized small-bar goal

**Spec.** Settings and Manage Clubs should use standard small app bars. Stage 15 specifically says non-detail authenticated bars were normalized to left-aligned small bars.

**Delivered.** `screenshots-v2/settings-clubs.png` shows `Club bag` visually centered between the back arrow and actions. Compared with the other v2 non-detail bars, Manage Clubs now feels like a third top-bar treatment:

- top-level routes: brand wordmark + title
- editors: back arrow + left title
- Manage Clubs: back arrow + centered title + actions

**Fix direction.** Encode an explicit route policy for top bars:

- top-level: title-only small bar
- pushed non-detail: back + left title + actions
- detail: collapsing medium bar with entity title

### 9. Settings and editor screens remain very loose/dense in the wrong places

**Spec.** Settings was meant to become a compact, scannable native settings list. Editors were meant to reduce density by making optional fields progressive and using shared row controls.

**Delivered.**

- Settings is structurally much better than before, but `settings-1.png` and `settings-2.png` still use very tall vertical intervals between rows/sections. The Club bag / Account / About content is readable, but not yet "single-screen scannable" on the baseline phone.
- Editors now share the row shell, but the row itself is still large and visually heavy. `unit-edit-2.png` shows two instruction cards consuming most of the screen; the move chevrons and delete controls sit in a lower control band that makes the row feel like a large card-with-toolbar again.

This is not a missing feature, but it weakens the intended payoff of the redesign: faster scanning and less form fatigue.

**Fix direction.** Tune density after the layout bugs above are fixed. Prefer reducing repeated vertical padding/control bands before changing type sizes.

## Low impact

### 10. Some v1 audit fixes lack manual-verification closure

The stage 12/13/15 change notes correctly close many old audit findings in code, but several relevant manual checks remain unchecked:

- `stage-12-list-fidelity/changes.md`: empty/sparse/populated list visual checks, card tap/overflow/swipe, phone/tablet layout.
- `stage-13-detail-fidelity/changes.md`: stat prominence, focus cue treatment, title truncation, phone/tablet layout.
- `stage-15-overview-polish/changes.md`: contrast, recent-card density, top-bar behavior, detail-bar regression.

Given the screenshot-visible issues above, the unchecked manual items matter. The implementation is mostly present, but final acceptance should include a manual compact-phone and tablet pass after the remaining fixes.

## Previously reported issues that appear fixed in v2

These v1 audit findings should not be carried forward as open issues based on the v2 code/screenshots:

- Empty/sparse list FAB thresholds now match the plan in code: empty hidden, 1-2 extended, 3+ compact.
- List cards now use `OutlinedCard`, title-medium hierarchy, and a single metadata row.
- Ball totals now have primary stat prominence in detail summaries.
- Unit detail default club is in the summary strip.
- `FocusCard` now has an icon, and session item focus cues are labelled/tinted.
- Notes terminology is scope-prefixed across the targeted editor/detail screens.
- Session editor now uses the shared `ReorderableItemRow` shell.
- Overview recents now include richer metadata and `AssistChip` type labels.
- Overview `NEXT MOVE` eyebrow no longer uses the old `0.7f` alpha.

## Overall assessment

The second implementation wave materially improved fidelity: the core list/detail/editor/overview features are now much closer to the intended redesign. The remaining issues are less about missing screens and more about shell correctness and rendered polish.

The highest-priority fix is the app-bar system. It is currently the main place where the delivered app still contradicts the spec, and it causes real orientation problems on detail screens. After that, clean up swipe background leakage and pinned editor bars, because those are the most visibly "broken" parts of the v2 screenshots.
