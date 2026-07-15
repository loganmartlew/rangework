# B18 — Validation errors misrouted or swallowed in the draft editor

Batch: android-ui — **but the code and test surface are in `:shared`** (see below)
Source: ../../potential-bugs.md#b18 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `PracticeDraftEditor.kt:200, 215, 248`
>
> `ValidationTarget.UnitInstructions` maps onto `titleError` ("At least one instruction is
> required" appears under the title field), and `ValidationTarget.Tags` is dropped in both
> `placeUnitErrors` and `placeSessionErrors` — a `MAX_TAGS_PER_ITEM` violation fails the save
> with no visible error anywhere.

## Batch note

`PracticeDraftEditor.kt` lives at
`apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/library/editor/PracticeDraftEditor.kt`
— it is a **shared-module** file, not an androidApp one, and its tests are in
`:shared` commonTest. It sits in the android-ui batch because the defect is about UI error
surfacing, but run and keep both suites green. The fix itself needs no Compose change if the
error fields already render (verify this — see DoD).

## Confirmation method

New test cases appended to
`apps/mobile/shared/src/commonTest/kotlin/com/loganmartlew/rangework/shared/library/editor/PracticeDraftEditorTest.kt`
(**additions only**):

1. **Misrouting:** call `placeUnitErrors` with a `ValidationIssue(target = ValidationTarget.UnitInstructions, ...)`
   and assert the message lands on an instructions-scoped error field — **not** on
   `titleError`. Today it sets `titleError` (`PracticeDraftEditor.kt:200`).
2. **Swallowing (unit):** call `placeUnitErrors` with a `ValidationTarget.Tags` issue and
   assert the message surfaces somewhere. Today the `Tags` branch returns `updated` unchanged
   (`:215`).
3. **Swallowing (session):** same for `placeSessionErrors` (`:248`).

All three must fail on current `main`. The producing side already works — `tagValidationIssues`
(`model/DraftValidation.kt:169-179`) emits the `Tags` issue with message
"At most $MAX_TAGS_PER_ITEM tags can be attached."; only the placement drops it. Confirm that
before writing the fix, since it's what makes this a routing bug rather than a validation gap.

## Definition of done

- All three new tests pass
- A `MAX_TAGS_PER_ITEM` violation produces a **user-visible** error, and the "At least one
  instruction is required" message no longer appears under the title field
- The Compose side actually renders whatever new error fields you add. This is the part most
  easily faked: a test asserting `input.tagsError == "..."` passes happily while the screen
  still shows nothing. Grep the editor screens for where `titleError` / `nameError` are
  rendered and wire the new fields in the same way; state in the PR body where each new error
  surfaces on screen. A reviewer will check this specifically
- The exhaustive `when` over `ValidationTarget` in both functions stays exhaustive with no
  `else` branch — the compile-time exhaustiveness check is what will catch the next target
  someone adds. Keep the "not applicable for units/sessions" grouping honest: `Tags` must
  leave that group in both functions
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest`
  green; `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- Scope boundary: `shared/.../library/editor/PracticeDraftEditor.kt`, the
  `PracticeUnitDraftInput` / `PracticeSessionDraftInput` models, `PracticeDraftEditorTest.kt`,
  and the minimum Compose wiring to render the new error fields. **Do not** change
  `DraftValidation.kt`'s issue-producing logic or add new `ValidationTarget` cases.

## Notes for the fixer

- The two functions are `placeUnitErrors` (`:192-219`) and `placeSessionErrors` (`:221-252`).
  Both fold a `List<ValidationIssue>` into the draft input via `copy(...)`, with a `when` on
  `issue.target`. The bugs are: line 200 (`UnitInstructions -> updated.copy(titleError = ...)`)
  and the `is ValidationTarget.Tags ->` arms grouped into the "not applicable" fall-through at
  lines 215 and 248.
- `Tags` being in the not-applicable group is plainly wrong in *both* functions — units and
  sessions each carry `tagIds` and each run `tagValidationIssues`. That's the giveaway.
- The per-index targets show the established shape for routing to a sub-field
  (`InstructionText` at :201-205 maps over `instructions` by index). `UnitInstructions` is
  list-level rather than per-index, so it needs a list-level error field — closer to
  `titleError`'s shape than to the mapped ones. That new field is the crux of fix 1.
- The comment at `:190` ("Per-domain error placement (replaces the regex remap)") means this
  code already replaced a worse mechanism once. Stay in that spirit: typed targets, explicit
  placement, no string matching.
