# Stage 2b — Fix, retry after review findings

A reviewer read your batch's diff against the specs and found problems. This is the
**only** retry: whatever the outcome, the batch stops after this pass and a human
looks at it. Fix what you can; do not paper over what you cannot.

Batch: **{{BATCH}}**
Branch: **{{BRANCH}}** (already checked out; you are in the worktree root)
Confirmation method: **{{CONFIRMATION}}**

## Findings

{{FINDINGS}}

## Your job

Address each finding on its own terms.

- A **test-gaming** finding means the fix was shaped to look like a fix rather than
  to be one — fitted to the assertion on a failing-test batch, or fitted to the
  wrong layer on a static-evidence batch (the classic being a client-side
  read-then-check where the spec demands a database guard). Re-read the spec and
  fix the actual defect. Do not make the finding go away by editing the test — you
  cannot: existing test files are additions-only, and the orchestrator rejects the
  batch mechanically if you modify a pre-existing test.
- A **scope-violation** finding means reverting the out-of-scope change, not
  justifying it. Scope boundary: {{SCOPE}}
- If you disagree with a finding, leave the code as it is and mark that bug `failed`
  with your reasoning in `blocker`. A human reads this. Arguing your case in
  `blocker` is a legitimate outcome; quietly ignoring the finding is not.

Keep **one commit per bug**, message starting with the bug id.

## Definition of done

Every command below still exits 0:

```
{{TEST_COMMANDS}}
{{LINT_COMMANDS}}
```

On Windows, invoke the Gradle wrapper as `.\gradlew.bat`, never bare `gradlew.bat` —
this host does not resolve executables from the current directory.

## Specs

{{SPECS}}

## Output

Emit exactly one `<{{TAG}}>` block containing JSON, in the same shape as before:
one entry per bug you touched in this retry.

<{{TAG}}>
{
  "results": [
    {
      "bug": "B6",
      "status": "fixed",
      "summary": "Cap now applies to explicit limits too, not just the default path."
    }
  ],
  "suitesGreen": true
}
</{{TAG}}>

Then emit `<promise>COMPLETE</promise>`.
