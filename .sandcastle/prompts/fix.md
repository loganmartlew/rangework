# Stage 2 — Fix

You are fixing confirmed bugs in the Rangework repo. You are running unattended:
nobody will answer a question, so a bug you cannot finish is a reported failure,
not a pause.

Batch: **{{BATCH}}**
Branch: **{{BRANCH}}** (already checked out; you are in the worktree root)
Bugs: **{{BUGS}}**
Confirmation method: **{{CONFIRMATION}}**

## Your job

{{FIX_BASIS_BLOCK}}

Iterate as much as you need inside this single session to get to green.

## Rules

- **One commit per bug.** Never bundle. An interrupted run must lose at most one
  bug's work.
- **Existing test files are additions-only.** You may add new test files and append
  cases; you may not edit or delete a pre-existing test.
- If a bug defeats you, commit nothing for it, mark it `failed`, and move on to the
  next bug. The orchestrator will park the batch for a human; it never opens a
  partial PR while a confirmed bug remains unresolved.
- Scope boundary: {{SCOPE}}

## Definition of done — per bug

- Every command below exits 0:
  ```
  {{TEST_COMMANDS}}
  {{LINT_COMMANDS}}
  ```
  On Windows, invoke the Gradle wrapper as `.\gradlew.bat`, never bare `gradlew.bat` —
  this host does not resolve executables from the current directory.
- The change stays inside the scope boundary above.
- Each spec's own **Definition of done** section is met — it is more specific than
  this list and it wins where they differ.

## Specs and verify verdicts

{{SPECS}}

## Output

When every bug is either fixed or failed, emit exactly one `<{{TAG}}>` block
containing JSON. Report `suitesGreen` honestly — the orchestrator re-runs the
suites itself and compares, so a false claim is caught and parks the batch.

<{{TAG}}>
{
  "results": [
    {
      "bug": "B6",
      "status": "fixed",
      "summary": "Added a 100-item default page cap with limit/offset params to list_units; free-text fields now length-checked at the tool boundary."
    },
    {
      "bug": "B16",
      "status": "failed",
      "summary": "Attempted a session-row lock in save_practice_session.",
      "blocker": "Needs a migration that conflicts with the guarded-RPC work in the shared-repo batch; out of scope here."
    }
  ],
  "suitesGreen": true
}
</{{TAG}}>

Then emit `<promise>COMPLETE</promise>`.
