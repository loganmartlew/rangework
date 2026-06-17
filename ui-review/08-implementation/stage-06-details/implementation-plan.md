# Stage 06 — Unit detail + Session detail redesign

> Roadmap stage **S6**. Twin detail screens; moves actions to the app bar and reframes the summary as an at-a-glance briefing. Backlog: B06, B13, B14, B15, B16, B21, B30, B34, B35, B42, B60. Specs: `07-redesigns/unit-detail-redesign.md`, `session-detail-redesign.md`. **Resolve decision gate F-1 (B11) before/at this stage.**

## Objective

Recover the vertical band the two co-equal pill buttons consume, fix the destructive-action hierarchy, and turn the summary into a useful pre-range glance.

- Edit/Delete leave the content area → app-bar trailing icons: an Edit pencil + an overflow ⋮ holding Duplicate and Delete (B35, B30). Delete fires a confirmation dialog + undo snackbar (B06).
- Small TopAppBar carries the entity title; drop the duplicate H1 (B34).
- **Summary → stat strip:** total balls as the largest numeral, plus counts (B13). **Session adds a briefing**: balls + units + estimated duration (B14, B15, from S3).
- Number badges replace "Instruction N" / index labels (B21); per-instruction ball counts become right-aligned pills forming a totalable column (B13, B42).
- Tinted `FocusCard` (`secondaryContainer`) renders only when a focus cue exists (B16, B60).
- **Session items:** structured rows — unit name primary; repeat ×N and club shown only when it overrides the unit default and tagged as an override; ball subtotal right-aligned; slot/unit focus cue line when present.
- Inline empty placeholders for sparse states (no instructions / no items) instead of blank or absent cards.

## Dependencies

- **Upstream:** S1 (tokens), S2 (`RangeworkTopAppBar` actions, `OverflowMenu`, `DeleteConfirmDialog`/`UndoSnackbar`, `StatBlock`/`BriefingRow`, `NumberBadge`, `BallCountPill`, `FocusCard`, `EmptyState` inline), S3 (`estimateSessionDuration`).
- **Decision gate F-1:** if B11 (remove the session-detail waypoint) is chosen, the Session briefing strip + item rows migrate into the Edit read-state / expandable list card instead of being built here.
- **Downstream:** S9 (Overview recents) navigates into these screens.

## Affected screens

- **Unit detail** and **Session detail**. App-bar action change affects the route into the editors (Edit) but not the route graph.

## Likely files

- Extracted `UnitDetailScreen` / `SessionDetailScreen` composables under `androidApp/.../ui/`.
- `androidApp/.../ui/RangeworkApp.kt` — app-bar action wiring (Edit → editor, Delete → dialog, Duplicate); F-1 may change the list→detail→edit route.
- `androidApp/.../ui/PracticePlannerViewModel.kt` — delete + undo, duplicate, duration derivation surfaced to the UI.
- S2 components (consumed). `shared` duration helper (from S3).
- `androidApp/src/test/.../PracticePlannerViewModelTest` — extend.

## New components required

None new. Assembles S2 components; this stage defines the **Session item row** composition (unit name + override-tagged club chip + repeat chip + ball-subtotal pill + conditional focus line) from existing primitives, and the **briefing strip** arrangement of `StatBlock`s.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] App-bar Edit opens the editor; app-bar Duplicate works; Delete opens the confirmation dialog and Undo restores the entity.
- [ ] No co-equal Edit/Delete buttons remain in the content area; title is in the bar only (no duplicate H1).
- [ ] Ball total is the largest numeral on the screen; per-instruction counts right-align into a column.
- [ ] `FocusCard` renders only when a cue exists; absent gracefully when not.
- [ ] Session briefing shows balls + units + estimated duration matching the S3 helper.
- [ ] Session item club shows **only** when it differs from the unit default and is visibly tagged as an override; otherwise hidden.
- [ ] Sparse states show inline placeholders with a route to Edit, not blank/absent cards.
- [ ] Number badges replace "Instruction N" / "Session item N" labels.
- [ ] Phone + tablet verified.

## Accessibility requirements

- App-bar icon buttons have content descriptions ("Edit {name}", "More options"); 48dp targets (B38, 9.2).
- Delete pairs error colour with icon + label + confirmation dialog — never colour-only (9.3, B06).
- `FocusCard` `secondaryContainer` tint vs its content text must clear AA.
- Override tag on the club must convey meaning by text/label, not colour alone.
- Ball numerals use `RangeworkMono`; section labels/instruction text use DM Sans (`CLAUDE.md`).
- Number badge announces position to TalkBack (e.g. "Step 2") so removing the text label doesn't lose order semantics.

## Regression risks

- **R6 / F-1:** building the Session-detail redesign while B11 proposes deleting the screen risks wasted work — resolve the gate first. If removed, redirect this effort into the Edit read-state.
- App-bar overflow on detail must use the same `OverflowMenu` as the list cards (consistency) — divergence reintroduces the inconsistency the audit flagged (C1).
- Override-only club display changes what users see versus today (club shown on every row) — confirm the unit-default comparison is correct so genuine overrides are never hidden.
- Duration is an estimate — label it clearly ("~15 min") so it isn't read as exact.
- Moving actions out of content changes muscle memory — ensure the editor is still reachable in one obvious tap.
