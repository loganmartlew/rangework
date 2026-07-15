# B9 — Drag-to-reorder relies on hardcoded header offsets (latent)

Batch: android-ui
Source: ../../potential-bugs.md#b9 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `UnitEditorScreen.kt:78-79` (`headerCount = 5`), `SessionEditorScreen.kt:104-105`
> (`headerCount = 1`)
>
> The list-index → instruction-index mapping subtracts a magic count of preceding non-list
> `item {}` blocks. Adding or removing any header item silently shifts every drag by one with no
> compile-time signal. Derive the offset or use segmented lists/stable keys.

## Confirmation method

**Read D7's ruling first: dismissal is an explicitly sanctioned outcome for this bug.**

> B9: confirmation via a unit test on the index-mapping function is acceptable; if there's no
> good testing surface, dismiss to tech-debt rather than forcing a Compose UI test.

This bug is marked **latent** — the offsets are correct *today*. There is no failing test to
write against current behaviour, because current behaviour is right. So:

1. First confirm the latency claim by counting the `item {}` blocks preceding the instruction
   list in `UnitEditorScreen.kt` (the comment at line 77 claims: title, notes/focus, club,
   tags, INSTRUCTIONS label = 5) and in `SessionEditorScreen.kt` (claims 1). If either count
   is **already wrong**, this is not latent — it's a live bug. Say so loudly; that changes
   the verdict and the priority.
2. If both are correct, the only fixable content is "make the offset impossible to get wrong".
   Confirm **only if** you can extract a pure index-mapping function testable from
   `:androidApp` unit tests (i.e. no Compose runtime), and can write a test that pins the
   mapping. `CountStepperTest.kt` is the nearest precedent for a testable unit under
   `ui/components/`.
3. Otherwise **DISMISS to tech-debt** with the reason. Do **not** add a Compose UI test
   dependency to make this testable — D7 forecloses that, and the setup cost is exactly what
   the android-ui batch was warned about.

## Definition of done

Only if confirmed:

- A new test pins the list-index → instruction-index mapping, and fails if a header item is
  added or removed without updating the mapping (that failure signal *is* the fix — a test
  that merely asserts `5 - 5 == 0` restates the magic number instead of removing it, and
  would be gaming the spec)
- Both screens are converted, or the PR explains why only one was
- `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest` green;
  `.\gradlew.bat :androidApp:lintDebug` green
- No behaviour change: drag-to-reorder still moves the same instruction to the same place
- Scope boundary: `UnitEditorScreen.kt`, `SessionEditorScreen.kt`, any new helper under
  `androidApp/.../ui/`, and its test. No shared-module changes, no ViewModel changes.

## Notes for the fixer

- The call site: `rememberReorderableLazyListState(lazyListState) { from, to -> onMoveInstruction(from.index - headerCount, to.index - headerCount) }`
  (`UnitEditorScreen.kt:76-80`). `from.index` / `to.index` are absolute `LazyColumn` indices;
  the callback needs instruction-list indices.
- The finding offers three directions — derive the offset, segment the list, or use stable
  keys. **Stable keys are the idiomatic Compose answer**: give the instruction items a `key`
  and resolve the drag by key instead of by arithmetic, which deletes the offset concept
  rather than testing it. That is the better fix, but it may not be reachable without a
  Compose test to prove it — in which case a derived-offset helper with a unit test is the
  pragmatic middle. Judge honestly and say which you chose and why.
- If you cannot make this genuinely safer without a Compose UI test, dismissal is the right
  answer. A cosmetic refactor that moves the `5` into a named constant is **not** a fix — it
  has the same silent-breakage property the finding objects to. Do not ship that.
