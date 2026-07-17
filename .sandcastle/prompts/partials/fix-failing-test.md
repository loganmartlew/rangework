Each bug below was confirmed by a previous agent, which committed a **failing test**
for it. Those tests are on this branch already. Your job is to make them pass by
fixing the underlying defect.

For each bug, in order:

1. Read the spec and run its failing test. Confirm it fails, and that you understand
   *why* — the mechanism, not just the assertion.
2. Fix the production code so the test passes.
3. Run the batch's full test and lint commands.
4. Commit — **one commit per bug**, message starting with the bug id
   (`B6: cap list_units page size`).

**Fix the bug the spec describes, not the assertion the test makes.** A reviewer
reads your diff against the spec next. Special-casing the test's inputs, weakening
an assertion, or short-circuiting the code path under test all fail review.

You may **not** edit or delete a pre-existing test — including the failing tests
from stage 1. If a stage-1 test looks wrong, leave it, mark that bug `failed`, and
explain in `blocker`. The orchestrator rejects the batch mechanically if you
modify a pre-existing test.
