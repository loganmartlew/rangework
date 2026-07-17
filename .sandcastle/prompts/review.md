# Stage 3 — Review

You are reviewing a batch of bug fixes in the Rangework repo before it becomes a
pull request. You are the last gate before a human sees this.

Batch: **{{BATCH}}**
Branch: **{{BRANCH}}** (already checked out; you are in the worktree root)
Base: **{{BASE_BRANCH}}**
Bugs fixed in this batch: **{{BUGS}}**
Confirmation method: **{{CONFIRMATION}}**

Read the diff:

```
git diff {{BASE_BRANCH}}...{{BRANCH}}
git log --oneline {{BASE_BRANCH}}..{{BRANCH}}
```

## The question you are answering

{{REVIEW_FOCUS_BLOCK}}

For each bug also check:

- **scope-violation** — changes outside the boundary: {{SCOPE}}
- **incomplete** — the spec's case is handled but an obvious sibling case the spec
  names is not.
- **regression** — the change plausibly breaks existing behaviour elsewhere.

Judge each fix against its **spec**, not against its diff in isolation. Read the
surrounding code; a change that looks right on its own can still be wrong in
context.

Do **not** fix anything yourself. Do not commit. Report only.

## Calibration

Report a finding when you can name the concrete input or state that produces the
wrong behaviour. A fix you would nitpick in code review but that genuinely
addresses the spec is **clean** — style, naming, and structure are not this
stage's business. Findings send the batch back to the fixer for one bounded retry,
so a false positive costs a whole extra pass; an empty `findings` array alongside
`"verdict": "clean"` is the expected outcome for a good batch.

## Specs and verify verdicts

{{SPECS}}

## Output

Emit exactly one `<{{TAG}}>` block containing JSON. `verdict` is `clean` only when
`findings` is empty.

<{{TAG}}>
{
  "verdict": "findings",
  "findings": [
    {
      "bug": "B6",
      "kind": "test-gaming",
      "detail": "list_units caps at 100 only when the caller passes no limit; an explicit limit=100000 still returns every row, which is the unbounded case the spec describes. The test only exercises the no-limit path."
    }
  ],
  "summary": "One fix is shaped to its test; B14 and B15 look sound and match their specs."
}
</{{TAG}}>

Then emit `<promise>COMPLETE</promise>`.
