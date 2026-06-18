# Stage 02 — Shared UI component library

> Roadmap stage **S2**. Builds every reusable composable once, in isolation, so the screen stages stay decoupled. Backlog: B04, B20, B13, B02, B21, B05, B01, B38, B09, B12, B40, B16, B60, B46, B47, B23, B57, B06, B18, B41.

## Objective

Implement the ~18 reusable composables that the redesigned screens consume, as standalone, previewable components with no screen wiring. Each component is built and screenshot-tested on its own; screen stages (S4–S9) then assemble them. This is the backbone of "minimally coupled" — twin screens (lists, details, editors) cannot drift because they share these exact components.

## Dependencies

- **Upstream:** S1 (tokens, typography roles, restored field defaults, error token).
- **Downstream:** S4 (Google button), S5, S6, S7, S8, S9 all consume these components. No screen stage should hand-roll a component that belongs here.
- **Parallel:** can run alongside S3 (data enablers) — no overlap.

## Affected screens

None directly (no wiring in this stage). Indirectly: every screen, via the components it will later import.

## Likely files

New files under the extracted UI/components package (per the recent "Extract UI screens/components" commit) — e.g. `androidApp/.../ui/components/`:

- `RangeworkTopAppBar.kt`, `RangeworkNavBar.kt` (or extend existing nav from S1)
- `ListEntryCard.kt`, `EmptyState.kt`, `OverflowMenu.kt`
- `DeleteConfirmDialog.kt`, `UndoSnackbar.kt` (snackbar host helper)
- `StatBlock.kt` / `BriefingRow.kt`, `NumberBadge.kt`, `BallCountPill.kt`, `ClubChip.kt`
- `CountStepper.kt`, `ReorderableItemRow.kt`, `DockedSaveBar.kt`, `StickyTotalBar.kt`
- `MoreOptionsExpander.kt`, `FocusCard.kt`
- `SettingsListItem.kt` / `SettingsSubheader.kt` / `SignOutItem.kt`
- `GoogleSignInButton.kt`
- `RangeworkFab.kt` / `RangeworkExtendedFab.kt`
- Tests: `androidApp/src/test/.../components/` (where logic is testable, e.g. stepper bounds, reorder callback).
- `gradle/libs.versions.toml` — **only if** a drag-reorder library is adopted (see R3); pin carefully, do not bump toolchain.

## New components required

All of them — this stage *is* the component inventory:

| Component | Purpose | Consumers |
|---|---|---|
| `RangeworkTopAppBar` | Small, title-only, optional back + trailing actions (B34, B35) | all except Login |
| `RangeworkNavBar` | active pill + 80dp (B08) | all authenticated |
| `ListEntryCard` | 3-tier card, clickable, trailing overflow (B04, B20, B13, B49) | Units/Sessions list, Overview recents |
| `EmptyState` | icon + headline + body + CTA, dependency-aware variant (B02, B19) | lists, Overview first-run, detail placeholders |
| `OverflowMenu` | Edit/Duplicate/Delete dropdown (B29, B30, B35) | list cards, detail app bars |
| `DeleteConfirmDialog` + `UndoSnackbar` | guarded destructive flow (B06, B18, B41) | details, editors |
| `StatBlock` / `BriefingRow` | numeral + caption strip (B13, B14, B15, B25, B51) | details, Overview |
| `NumberBadge` | circular index (B21) | detail, editors |
| `BallCountPill` / `ClubChip` | structured metadata pills (B13, B20) | list, details |
| `CountStepper` | − value + bounded integer (B05) | editors |
| `ReorderableItemRow` | drag handle + content (B01, B38) | editors |
| `DockedSaveBar` | pinned primary `FilledButton` (B09) | editors |
| `StickyTotalBar` | live total under app bar (B12, B15) | session editor (+ unit total) |
| `MoreOptionsExpander` | progressive disclosure (B40) | editors |
| `FocusCard` | `secondaryContainer`-tinted cue (B16, B60) | details |
| `SettingsListItem` / `SettingsSubheader` / `SignOutItem` | M3 list groups (B46, B47) | Settings, Manage clubs |
| `GoogleSignInButton` | Identity-compliant (B23) | Login |
| `RangeworkFab` / `ExtendedFab` | `primaryContainer` fill (B07, B57) | lists |

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] Every component has at least one `@Preview` (populated + empty/edge variants where relevant) and a screenshot baseline.
- [ ] `CountStepper` unit-tested for min/max clamping and invalid-input rejection.
- [ ] `ReorderableItemRow` reorder callback unit-tested (order in → order out).
- [ ] `DeleteConfirmDialog` confirm/cancel callbacks fire correctly; `UndoSnackbar` restore callback fires within the timeout window.
- [ ] `EmptyState` renders both the normal and dependency-aware (no-units) variants.
- [ ] Drag-reorder library compatibility spike documented against the pinned Compose/toolchain/SDK 35 versions before adoption (R3).
- [ ] Components use S1 tokens only — no hardcoded colours or anonymous `TextStyle`s.
- [ ] All interactive components verified at 48dp minimum touch target.

## Accessibility requirements

- Bake a11y into the components so screen stages inherit it:
  - Content descriptions parameterized on `OverflowMenu`, `ReorderableItemRow` (drag handle + delete), `CountStepper` (− / +), settings `Switch` rows ("Enable {club}") — B39, systemic 9.2.
  - 48dp minimum interactive size on every icon button (B38; consistency M5) via `Modifier.minimumInteractiveComponentSize()` or explicit `size(48.dp)`.
  - `DeleteConfirmDialog` provides the non-colour confirmation that pairs with the error colour (9.3).
  - `GoogleSignInButton` meets Google's branding *and* contrast spec.
  - `StatBlock`/`BallCountPill` use `RangeworkMono` for the numeric values per `CLAUDE.md` typography rules; captions/labels use DM Sans.
- Keep the ↑/↓ chevron path available as an accessibility fallback for reordering (Material audit).

## Regression risks

- **R3:** drag-to-reorder (B01) may need a library or custom gesture that conflicts with pinned Compose/toolchain (Java 17 target, SDK 35). Spike first; keep chevron fallback; do not bump the toolchain casually.
- **R8:** if components are under-parameterized, twin screens will be forced to fork them — defeating the decoupling goal. Design APIs to serve both members of each twin (Unit+Session) from the start.
- `StickyTotalBar` + `DockedSaveBar` are layout-sensitive; validate them inside a representative scaffold during S2 so S7 doesn't discover IME/scroll problems late (R2).
- Over-eager `MoreOptionsExpander` defaults could hide populated fields — default to auto-expand when a wrapped field has a value (R9).
- Library addition (if any) must not regress build time or CI (SDK 35 / build-tools 35.0.0 on Java 21).
