# B14 — Snapshot expansion is unbounded

Batch: shared-validation
Source: ../../potential-bugs.md#b14 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `DraftValidation.kt:16-21, 85`
>
> Ball counts are validated only non-negative and repeat counts only `> 0`. Server-side snapshot
> expansion creates one step per ball × repeat — a unit with `ballCount = 1_000_000` becomes a
> million-row snapshot at start. Cheapest guard is an upper bound at the validation layer
> (mirrored in MCP validation and the DB check constraint).

## Confirmation method

New test cases appended to
`apps/mobile/shared/src/commonTest/kotlin/com/loganmartlew/rangework/shared/model/DraftValidationTest.kt`
(**additions only** — do not modify or delete existing cases):

1. A `PracticeUnitDraft` with an instruction whose `ballCount` is absurd (e.g. 1_000_000)
   currently passes `validated()` without throwing. Assert it raises a
   `SharedValidationException` carrying a `ValidationTarget.InstructionBallCount(index)`
   issue.
2. The same for a `PracticeSessionItemDraft.repeatCount` of 1_000_000 against
   `PracticeSessionDraft.validated()`, expecting `ValidationTarget.ItemRepeatCount(index)`.

Both must fail on current `main`. Follow the existing assertion style in that file.

## Definition of done

- New tests pass
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest` green;
  `:androidApp:testDebugUnitTest` green (the Android VMs surface these messages);
  `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- The bounds are named constants, exported the way `MAX_TAGS_PER_ITEM` is, not magic numbers
  buried in the check
- Scope boundary: `apps/mobile/shared/src/commonMain/.../model/DraftValidation.kt` plus the
  file declaring the new constants and the matching test file. Any UI-side error surfacing
  falls out of the existing `ValidationTarget` plumbing — do **not** touch Compose screens.
  **Do not** add the mirrored MCP validation or DB check constraint (see notes).

## Notes for the fixer

- The exact code the finding names: `DraftValidation.kt:16-21` checks only
  `instruction.ballCount != null && instruction.ballCount < 0`, and `:85` checks only
  `item.repeatCount <= 0`.
- **Pattern to mirror:** `tagValidationIssues` at `DraftValidation.kt:169-179` — a small
  private helper returning `ValidationIssue`s, driven by a `MAX_TAGS_PER_ITEM` constant, with
  a message that names the limit ("At most $MAX_TAGS_PER_ITEM tags can be attached."). Do the
  same for ball and repeat counts. This is the house style for exactly this kind of cap.
- Reuse the existing `ValidationTarget.InstructionBallCount` / `ValidationTarget.ItemRepeatCount`
  targets — they already route to the right fields via `placeUnitErrors` / `placeSessionErrors`
  (`library/editor/PracticeDraftEditor.kt:206-210, 234-238`). Adding a new target would drag
  in B18's territory; don't.
- Picking the numbers is a judgment call. Ground them in what a real range session can be
  (a large bucket is a few hundred balls; a session is a couple of hours) and state the
  reasoning in the PR body. Err generous — this is a sanity bound against pathological input,
  not a product limit, and a bound that rejects a legitimate session is a worse bug than the
  one being fixed.
- **Deliberate scope exclusion:** the finding suggests mirroring the bound into MCP validation
  and the DB check constraint. Both are out of this batch (different harness, different PR).
  Note the resulting divergence — MCP and the DB will still accept what `:shared` now rejects
  — in the PR body so a follow-up issue can be filed.
