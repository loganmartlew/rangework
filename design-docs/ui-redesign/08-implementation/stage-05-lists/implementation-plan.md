# Stage 05 — Units list + Sessions list redesign

> Roadmap stage **S5**. The two list screens are structural twins built from one shared card/empty/FAB set. Backlog: B04, B07, B13, B19, B20, B29, B30, B34, B37, B49, B57, B02. Specs: `07-redesigns/unit-list-redesign.md`, `session-list-redesign.md`.

## Objective

Rebuild both list screens on the shared `ListEntryCard`, `EmptyState`, and `RangeworkFab` so they scan identically and turn from passive walls of text into scannable, tappable, action-discoverable lists.

- Small TopAppBar, title-only — drop the "Rangework / {Section}" double-title (B34).
- Three-tier card: title → metadata row → preview line; the card body is fully tappable → detail, with the overflow ⋮ demoted to a shortcut menu (B04, B49, B20).
- **Units:** differentiate the default club from instruction text (B20); Duplicate added to overflow (B29, uses S3 use case).
- **Sessions:** raise total ball count to a prominent figure with a unit-lineup preview (B13); Duplicate promoted to a first-class overflow item (B30).
- FAB → `primaryContainer` green (B07, from S1) and Extended FAB while the list is sparse (B57); 96dp bottom content padding so the FAB never occludes the last card (B37).
- Empty states with icon + headline + body + CTA (B02); **Sessions empty state is dependency-aware** — with zero units it routes to create a unit first instead of an empty picker (B19).

## Dependencies

- **Upstream:** S1 (FAB colour, nav), S2 (`ListEntryCard`, `EmptyState`, `OverflowMenu`, `RangeworkFab`/`ExtendedFab`, `DeleteConfirmDialog`/`UndoSnackbar`), S3 (`DuplicateUnitUseCase`).
- **Downstream:** S9 (Overview) reuses `ListEntryCard` for recents and navigates into these screens.

## Affected screens

- **Units list** and **Sessions list**. Indirectly opens Unit/Session detail (existing routes) and the create flows.

## Likely files

- Extracted `UnitsListScreen` / `SessionsListScreen` composables under `androidApp/.../ui/` (the screens split out in the recent extraction commit).
- `androidApp/.../ui/RangeworkApp.kt` — list ↔ detail ↔ create navigation; dependency-aware routing from the Sessions empty state.
- `androidApp/.../ui/PracticePlannerViewModel.kt` — list state, delete + undo, duplicate actions, has-units flag for the dependency-aware empty state.
- `androidApp/.../ui/components/*` (S2 components — consumed, not created).
- `androidApp/src/test/.../PracticePlannerViewModelTest` — extend for duplicate/delete/undo and empty-state routing.

## New components required

None new — all from S2. This stage **configures** `ListEntryCard` for the unit vs session content shape, wires `OverflowMenu` items (Edit/Duplicate/Delete) per screen, and supplies the two empty-state variants (including the dependency-aware Sessions variant).

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] Tapping a card body opens the corresponding detail screen (both lists).
- [ ] Overflow ⋮ shows Edit / Duplicate / Delete; Duplicate works on Units (new) and Sessions; Delete fires confirm + undo, not instant deletion.
- [ ] Empty Units → CTA routes to create unit.
- [ ] Empty Sessions **with units** → CTA routes to create session; **without units** → routes/explains create-a-unit-first (B19).
- [ ] FAB shows green `primaryContainer`; Extended FAB on sparse lists collapses to standard FAB as the list grows.
- [ ] 96dp bottom padding: scroll to the last card — FAB does not cover it or its overflow icon.
- [ ] Both list cards use the identical three-tier template (visual parity check).
- [ ] Units card: club reads as a distinct pill/chip, not as the first instruction.
- [ ] Sessions card: total ball count is the prominent figure; unit lineup shows as the preview line.
- [ ] Phone + tablet (expanded) layouts both verified.

## Accessibility requirements

- Card is a single accessible click target with a meaningful name (the unit/session title); overflow is a separate target with "More options for {name}" (systemic 9.2).
- Overflow menu items have clear labels; Delete pairs colour with icon + label + confirmation (9.3, B06).
- Ball/instruction counts use `RangeworkMono` numerals; titles and preview use DM Sans (`CLAUDE.md`).
- 48dp targets for the overflow icon and FAB; empty-state CTA is a standard 48dp button.
- Preview/metadata secondary text meets AA on dark (carried from S1).

## Regression risks

- **R8:** Units and Sessions diverging — enforce the shared `ListEntryCard`; review both together.
- Duplicate-unit (B29) is brand-new behaviour — confirm the copy is independent and owner-scoped (depends on S3 correctness).
- Demoting the overflow to a shortcut while making the card tappable can create two paths to Delete — ensure both go through the same confirm + undo flow.
- 96dp padding interacts with the S1 nav-bar height change — verify combined bottom inset doesn't leave a dead gap or clip.
- Dependency-aware routing must not strand the user mid-flow — provide a clear back path from the redirected unit-creation screen.
