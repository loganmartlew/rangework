# Stage 5 follow-up: Block-level "Recorded balls" card

**Parent stage:** [`implementation-plan.md`](./implementation-plan.md) + [`changes.md`](./changes.md) — the
per-ball edit sheet shipped and works; this is a discoverability refinement to how it is *entered*,
not new capability.
**Design reference:** [`prototype.html`](./prototype.html) — the prototype's instruction-row entry is
what shipped and what this plan deliberately replaces (see "Deviation" below).
**Status:** implemented 2026-07-12. Builds green: `:androidApp` debug/release unit tests,
`:androidApp:assembleDebug`, `:shared:lintDebug` + `:androidApp:lintDebug`. See
[`changes.md`](./changes.md) for the shipped summary.

## Why (deviation from shipped stage 5)

Stage 5 followed the prototype: each ball-instruction row was tappable (a trailing `›`) and opened a
**per-instruction** edit sheet. In use this proved hard to spot — the affordance is visually
indistinguishable from a club-swap chip or an action-row toggle, so the correction feature reads as
absent even though it is fully wired. We move the entry to a single, clearly labelled **block-level
card** and combine the block's ball instructions into **one sheet**, grouped into per-instruction
sections. This is a UI/interaction change only: no shared logic, no ViewModel, no schema. The owner
judged it not ADR-worthy (a clunk fix, not a durable architectural commitment) — this section is the
record of the deviation.

## Objective

1. **One "Recorded balls" card** on the block screen, below the instruction list and above Notes,
   showing the committed-ball count. Tapping opens the edit sheet. It is the *sole* entry.
2. **Instruction rows revert to plain** — ball-instruction rows are no longer tappable and drop the
   `›`. Action rows keep tap-to-toggle. (Declutters the instruction card.)
3. **Block-scoped, instruction-grouped sheet** — one sheet listing every committed ball in the block,
   grouped into a section per ball instruction (instruction text as header), with "Ball 1..N"
   numbered *within* each section. Everything inside a section is unchanged from today.

Android UI + wiring only, all in `androidApp/`. No `shared/` touch. The ViewModel is untouched:
`updateBallObservation(stepIndex, …)` is already keyed by global step index, so it works identically
regardless of how the sheet groups rows.

## Fixed rules

- **Card visibility:** render iff `captureEnabled` (block unit has ≥1 enabled Observation Type)
  **and** the block has ≥1 committed Ball Step. Zero-types blocks, action-only blocks, and
  not-yet-hit blocks show no card — same floor as the capture stack.
- **Card content:** label "Recorded balls" + count `N`, where `N = block.progress(steps,
  completed).completedBalls` (for v3 each Ball Step carries `ballCount` 1, so this is the number of
  committed balls). A small edit/history icon, not a bare chevron. **No** progress-toward-target or
  judgement framing (stage-5 tripwire holds — this is not "N of M observed").
- **Placement:** directly below the instruction-list card, above `CollapsibleNotes`. Live-input
  surfaces (counter+capture) stay at the top; review/correct sits lower.
- **Sheet title:** block-level ("Recorded balls"). Each instruction's text becomes a **section
  header** inside the sheet.
- **Per-section numbering unchanged:** "Ball 1..N" is the 1-based ordinal within that instruction's
  Ball Steps in snapshot order (today's `ballEditEntries` logic, applied per instruction).
- **Single-instruction blocks** still render their one header — uniform structure, matches today's
  sheet title behaviour, costs nothing.
- **Read-only / block-complete while Active:** card and sheet remain available; edits stay gated by
  `canEditObservations` exactly as today. When the session is no longer Active the execution screen
  isn't shown; the sheet's editors already fall back to read-only.

## Likely files (all `androidApp/`)

| File | Change |
|---|---|
| `ui/components/BallEditSheet.kt` | `BallEditSheet` takes a block title + `List<BallEditGroup>` (new: `data class BallEditGroup(val instructionText: String, val entries: List<BallEditEntry>)`) instead of a single `instructionText` + flat `entries`. Render one section per group: header (instruction text) then that group's rows. Row/editor internals (`BallEditRow`, `BallSummary`, `BallEditor`, write-through, grid launchers, single-expanded `expandedStepIndex`) are unchanged and shared across sections. |
| `ui/components/ExecutionBlockPage.kt` | Remove the `editable` param + instruction-row `clickable`/`›` (revert `InstructionRow` ball rows to plain). Change `onOpenBallSheet` to a no-arg block callback. Add a `RecordedBallsCard` composable (count + icon, `clickable → onOpenBallSheet`), placed between the instruction-list card and `CollapsibleNotes`, gated on `captureEnabled && progress.completedBalls > 0`. |
| `ui/screens/RangeSessionScreen.kt` | Drop `sheetInstructionIndex`; keep `sheetBlockIndex` + `sheetExpandedStep`. `onOpenBallSheet` sets `sheetBlockIndex = pageIndex`. Replace `ballEditEntries(…, instructionIndex)` with a block-scoped `ballEditGroups(block, steps, completed)` that walks the block's ball instructions in snapshot order, builds each group's entries (per-instruction ordinal, filtered to committed), and drops empty groups. Pass groups + block title to `BallEditSheet`. Grid-launcher edit path (`gridBlockIndex = sheetBlockIndex`, `gridStepIndex = step`, `ballOrdinal`) is unchanged — `ballOrdinal` is already per-instruction. |

No changes to `RangeSessionViewModel.kt`, `RangeworkApp.kt`, or any `shared/` file.

## Behaviour

- **Entry:** tap "Recorded balls" card → sheet opens showing all committed balls in the block, grouped
  by instruction, newest-instruction-order top-to-bottom (preserve existing newest-first ordering
  *within* a section as today via `entries.asReversed()`).
- **Ladder-drill example** (one block, three ball instructions): three sections — "PW … / Ball 1..5",
  "9I … / Ball 1..5", "8I … / Ball 1..5". Each ball row and its editor behave exactly as the shipped
  sheet.
- **Edit:** expand a ball → pre-selected chip rows / grid launchers; every tap writes through via
  `updateBallObservation(stepIndex, …)`; single-expanded accordion is global across sections.
- **Grid launcher from sheet:** opens the grid dialog in edit mode targeting that `stepIndex`; dialog
  title uses `ballOrdinal` (per-instruction), heatmap/tally still show block history. Unchanged.

## Edge cases

- **Block with one ball instruction:** single section; card count = that instruction's committed balls.
- **Block with zero committed balls / zero enabled types / action-only:** no card.
- **Committing a ball while the sheet is open** (sheet and card coexist on screen): sheet content is
  derived from UiState, so a new committed ball appears in its section on recomposition; expansion
  state keyed by step index is undisturbed.
- **Decrement (−) voiding the last ball while sheet open:** the voided step leaves
  `completedStepIndices`, so its row drops from its section and the card count decrements; if it was
  the expanded row, it collapses (no matching step index).
- **Multiple instructions where clubs/targets differ:** disambiguated by the section header
  (instruction text). Numbering never collides because it's per-section.

## Validation checklist

- [ ] `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug`
- [ ] `.\gradlew.bat :androidApp:lintDebug`
- [ ] Existing `RangeSessionViewModelTest` untouched-green (VM unchanged by design — a guard that this
      stayed a pure UI change).
- [ ] **Manual device flow:**
      **(A)** single-ball-instruction block: card shows correct count, opens one-section sheet, edit a
      ball, re-open and confirm the correction stuck;
      **(B)** ladder block (≥2 ball instructions, different clubs): card count = block total, sheet
      shows one section per instruction with correct headers and independent "Ball N" numbering;
      **(C)** confirm ball-instruction rows are no longer tappable and show no `›`; action rows still
      toggle;
      **(D)** zero-types / action-only / not-yet-hit block: no card;
      **(E)** grid-launcher edit from within a multi-section sheet targets the right ball;
      **(F)** −1 an observed ball with the sheet open: row and card count update.

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Removing the instruction-row entry regresses a path something else depended on | Low | Entry was self-contained (`editable`/`onOpenBallSheet`); grep confirms no other caller; VM untouched |
| Grouped sheet layout crowds tall blocks (20+ balls, several sections) | Low–Medium | Sheet already scrolls; sections are cheap headers; validate case (B) in hand |
| Placement of the new card feels wrong at the range | Low | Low-stakes, trivially movable; re-judge at field test #2 |
| Scope creep toward stage 6 summaries (per-type counts, provenance) on the new card | Low | Card is a bare count + launcher; tripwire restated in Fixed rules |
