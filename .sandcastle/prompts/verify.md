# Stage 1 — Verify

You are verifying reported bugs in the Rangework repo. You are running unattended:
nobody will answer a question, so a bug you cannot settle is a dismissal, not a pause.

Batch: **{{BATCH}}**
Branch: **{{BRANCH}}** (already checked out; you are in the worktree root)
Bugs: **{{BUGS}}**
Confirmation method: **{{CONFIRMATION}}**

## Your job

For each bug spec below, decide **confirmed** or **dismissed**.

{{CONFIRM_BLOCK}}

Work through the bugs one at a time. Each spec carries the full finding text — you
do not need to open `potential-bugs.md`.

## Rules

- **Existing test files are additions-only.** You may add new test files, and you
  may append cases to an existing test file. You may **not** edit or delete a test
  that already exists. The orchestrator rejects the batch mechanically if you do.
- **Existing migrations are immutable.** `supabase/migrations/` is append-only
  history; never edit a file that is already there.
- **Do not touch production code in this stage.** Fixing is stage 2's job.
- Scope boundary: {{SCOPE}}
- Commands available for this batch:
  ```
  {{TEST_COMMANDS}}
  ```
  On Windows, invoke the Gradle wrapper as `.\gradlew.bat`, never bare `gradlew.bat` —
  this host does not resolve executables from the current directory.

## Specs

{{SPECS}}

## Output

When every bug has a verdict, emit exactly one `<{{TAG}}>` block containing JSON.
Include a verdict for **every** bug listed above.

Fields per verdict: `bug`, `verdict`, `reason` always. On a **failing-test** batch a
confirmation also needs `testFile` and `testName`. On a **static-evidence** batch a
confirmation also needs `evidence` (a list of `location` + `quote`) and `sequence`.

<{{TAG}}>
{
  "verdicts": [
    {
      "bug": "B6",
      "verdict": "confirmed",
      "reason": "list_units returns all rows; no limit/offset. Test asserts a cap and fails.",
      "testFile": "apps/mcp/src/tools/list-units.test.ts",
      "testName": "caps the number of returned units"
    },
    {
      "bug": "B1",
      "verdict": "confirmed",
      "reason": "Both lifecycle updates are filtered by id alone; no state guard.",
      "evidence": [
        {
          "location": "SupabaseRangeSessionRepository.kt:162-171",
          "quote": "postgrest.from(\"range_sessions\").update({ set(\"completed_at\", now) }) { filter { eq(\"id\", rangeSessionId) } }"
        },
        {
          "location": "RangeSessionRecordingRules.kt:18-23",
          "quote": "val state: RangeSessionState get() = when { abandonedAt != null -> ABANDONED; completedAt != null -> COMPLETED; else -> ACTIVE }"
        }
      ],
      "sequence": "finishSession(x) then abandonSession(x): the second call stamps abandoned_at on an already-completed row. state resolves abandonedAt first, so the session reads ABANDONED, and listCompletedSessions filters abandoned_at IS NULL — the completed session vanishes from history."
    },
    {
      "bug": "B9",
      "verdict": "dismissed",
      "reason": "Both header counts are correct today and there is no pure index-mapping function extractable without a Compose UI test dependency, which D7 forecloses. Dismissed to tech-debt per the spec's own ruling."
    }
  ]
}
</{{TAG}}>

Then emit `<promise>COMPLETE</promise>`.
