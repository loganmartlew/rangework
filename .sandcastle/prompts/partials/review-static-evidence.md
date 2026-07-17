**This batch has no test surface, so you are the only real gate before a human.**
No test went from red to green here; the suites passing means the fixer broke
nothing, not that anything was fixed. Nothing mechanical has checked this work.
Read it like nobody else will, because nobody else has.

There is no test to game, so `test-gaming` takes a different shape: a fix that
*looks* like it addresses the finding while leaving the actual defect intact.
Specifically, check:

- **Is the fix at the layer the spec demands?** These specs are prescriptive
  about this and the prescription *is* the fix. "The guard is enforced in the
  database, not by a client-side read-then-check" means a Kotlin read-check-write
  reintroduces the exact race the bug is about — it would look plausible, pass
  every suite, and be wrong. Report it as `test-gaming`.
- **Does the SQL do what its comment says?** Read the migration, not its header.
  Check the lock is actually taken and actually covers the read-modify-write, that
  `security invoker` / `search_path` match the pattern the spec points at, and that
  the grants exist.
- **Is the migration additive?** New timestamped file, ordered after the latest
  existing one, no edits to existing migrations. A migration that edits history is
  a `scope-violation`.
- **Was the deliberate decision made and stated?** Where a spec requires a
  documented choice (e.g. no-op vs. raise for the already-in-that-state case),
  check it was made *and* written down, and that callers tolerate it — a new thrown
  error is a new failure mode for the ViewModels. Silence here is `incomplete`.

Verify the verify stage too: the quoted evidence is reproduced with each spec
below. Spot-check the quotes against the real files. If the confirmation rested on
a misquote, the fix is built on sand — report it.
