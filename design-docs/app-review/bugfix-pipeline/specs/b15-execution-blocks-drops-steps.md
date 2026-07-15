# B15 — `executionBlocks()` silently drops steps with out-of-range `unitIndex`

Batch: shared-validation
Source: ../../potential-bugs.md#b15 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `ExecutionBlocks.kt:38-47` vs `RangeSessionProgress.kt`
>
> Blocks are built from `units.mapIndexed`, so steps referencing `unitIndex >= units.size`
> vanish from the block view while `totalStepCount()` still counts them — a session that can
> never reach 100%. Snapshots are server-generated so likelihood is low; a decode-time assertion
> (`steps.unitIndex ∈ units.indices`) would make the failure loud instead of silent.

## Confirmation method

New test case appended to
`apps/mobile/shared/src/commonTest/kotlin/com/loganmartlew/rangework/shared/model/ExecutionBlocksTest.kt`
(**additions only**):

Build a `RangeSessionSnapshot` with N units and at least one step whose `unitIndex == N`
(out of range). Assert the divergence the finding claims — the sum of `stepIndices` across
`executionBlocks()` is strictly less than `totalStepCount()`, i.e. the orphaned step is
addressable by neither block and the session can never reach 100%.

That failing assertion is the confirmation. Note the *fix* changes where the failure surfaces
(see DoD), so expect to rewrite this test's expectation as part of the fix — that is legal
here because the test is new in this same change. Do not modify pre-existing cases in the file.

Sanity-check the corroborating half before confirming: verify in `RangeSessionProgress.kt`
that `totalStepCount()` really does count every step regardless of `unitIndex`. If it doesn't,
the "can never reach 100%" claim collapses — DISMISS with that reason.

## Definition of done

- The new test passes against the fixed behaviour
- Out-of-range `unitIndex` fails **loudly at decode time** rather than silently vanishing —
  the finding explicitly prescribes the decode-time assertion
  (`steps.unitIndex ∈ units.indices`), and it is the right call: `executionBlocks()` is
  called from Compose (`ui/screens/RangeSessionScreen.kt:148`), so throwing there would
  crash a live range session mid-round, whereas failing at decode surfaces a corrupt snapshot
  before the session opens
- Existing `ExecutionBlocksTest` and `SnapshotV3DecodingTest` cases still pass unmodified —
  if the new assertion breaks one, that is a signal to reconsider the fix, not to edit the test
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest`
  green; `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- Scope boundary: the snapshot decode path plus `model/ExecutionBlocks.kt` and the two test
  files named above. No Compose changes, no repository changes.

## Notes for the fixer

- The exact mechanism: `executionBlocks()` (`ExecutionBlocks.kt:38-47`) groups step indices by
  `unitIndex` into `indicesByUnit`, then iterates `units.mapIndexed` — so any key in
  `indicesByUnit` beyond `units.lastIndex` is simply never read. Nothing throws; the steps
  just cease to exist for the block view.
- Look at `SnapshotV3DecodingTest.kt` first — it shows the established shape for
  decode-time snapshot validation and tells you where the assertion belongs and what the
  existing failure mode looks like. Mirror it rather than inventing a new error type.
- Keep the assertion's message specific enough to debug from a crash report: name the
  offending `unitIndex` and the unit count.
- Do not "fix" this by making `executionBlocks()` tolerate the bad index (e.g. bucketing
  orphans into a synthetic block). That preserves the silent-corruption behaviour the finding
  objects to and would be gaming the spec.
