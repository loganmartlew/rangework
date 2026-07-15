# B17 — `restoreUnit` loses inline scoping

Batch: shared-validation
Source: ../../potential-bugs.md#b17 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `DefaultPracticeLibrary.kt:65-83`, `InMemoryPracticeUnitRepository.kt:46`
>
> `PracticeUnitDraft` has no `scopedToSessionId`, so restore-after-delete of an inline unit
> recreates it as a library citizen. Narrow path (inline units normally die with their session
> via cascade), but the abstraction leaks — see the `toDraft()` consolidation in
> [tech-debt.md](tech-debt.md) D11.

## Confirmation method

New test case appended to
`apps/mobile/shared/src/commonTest/kotlin/com/loganmartlew/rangework/shared/library/PracticeLibraryTest.kt`
(**additions only**), using the existing `InMemoryPracticeUnitRepository` fake:

Create an inline (session-scoped) unit, delete it, then `restoreUnit(...)` it and assert the
restored unit is **still scoped to its session**. Today it comes back as a library unit —
that's the failing assertion.

Before writing the fix, establish that the path is reachable at all: find the caller of
`restoreUnit` (it exists to back an undo-after-delete affordance) and confirm an *inline* unit
can actually reach it. The finding itself calls the path "narrow". If inline units provably
cannot reach `restoreUnit` — e.g. the delete affordance is offered only for library units —
then **dismiss to tech-debt** with that reason and a pointer to the `toDraft()` consolidation
(tech-debt D11), which would subsume this anyway. That is a legitimate outcome; say so in the
verdict rather than forcing a fix.

## Definition of done

- New test passes
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest`
  green; `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- `promoteUnit`'s contract is untouched: promotion (`DefaultPracticeLibrary.kt:85-88`) is
  documented as one-way with no demotion path. Whatever you add must not become a backdoor
  demotion — a reviewer will check this specifically
- Scope boundary: `shared/.../library/DefaultPracticeLibrary.kt`, the `PracticeUnitDraft`
  model, `InMemoryPracticeUnitRepository`, the `PracticeUnitRepository.persist` contract if
  it must change, and `PracticeLibraryTest`. **Not** in scope: the `toDraft()` consolidation
  (tech-debt D11), the MCP `promote_unit` tool, or any Compose screen.

## Notes for the fixer

- The mechanism: `restoreUnit` (`DefaultPracticeLibrary.kt:65-83`) rebuilds a
  `PracticeUnitDraft` field-by-field from the `PracticeUnit` and calls
  `unitRepository.persist(draft.validated(), unit.id)`. `PracticeUnitDraft` carries title,
  notes, focus, defaultClubCode, successCriterion, instructions and tagIds — but no
  `scopedToSessionId`, so the scoping is dropped on the floor at that copy. Compare
  `duplicateUnit` immediately above (`:~40-63`), which does the same field-by-field rebuild and
  is *correct* to drop the scoping, since a duplicate should be a library citizen.
- That contrast is the design constraint: whatever you add must let restore preserve scoping
  **without** making duplicate inherit it. Two call sites, same helper, different intent —
  don't collapse them.
- The scoping is set elsewhere via `unitRepository.setScopedSession(id, ...)` (see
  `promoteUnit`, `DefaultPracticeLibrary.kt:85-88`), which is a plausible seam if you'd rather
  not widen `PracticeUnitDraft`. Weigh both; adding the field to the draft is what the finding
  suggests, but it touches every draft construction site including the editor — the narrower
  fix may be better. State the choice in the PR body.
- If the fix ends up requiring changes across more than the scope boundary above, stop and
  flag rather than expanding — that's the signal that D11's consolidation should land first.
