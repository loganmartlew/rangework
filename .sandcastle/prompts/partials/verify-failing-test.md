This batch **has a test surface**, so a confirmation is a failing test.

- **confirmed** — you reproduced the claimed behaviour with a **new failing test**.
  Write the test, run it, watch it fail *for the reason the spec describes*, and
  commit it. A test that fails for an unrelated reason is not a confirmation.
- **dismissed** — you could not produce a failing test matching the claimed
  behaviour. This is a perfectly good outcome here. Say why in `reason`: the code
  already guards the case, the spec's premise is wrong about the code, there is no
  testing surface that doesn't cost more than the bug is worth. **Do not fix
  anything. Do not write a passing test to "prove" the bug is absent.**

Read each spec's own **Confirmation method** section — some name the exact test to
write, and at least one sanctions dismissal explicitly. The spec beats this
general guidance where they differ.

For every confirmed bug, `testFile` and `testName` must name the test you actually
committed.

**Commit discipline:** one commit per bug, message starting with the bug id
(`B6: add failing test for ...`). Commit the failing test even though it fails — a
red test is this stage's deliverable. Never commit two bugs' tests together; an
interrupted run must lose at most one bug.
