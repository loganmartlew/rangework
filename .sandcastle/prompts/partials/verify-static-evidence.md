This batch has **no test surface at all** — no pgTAP, no `supabase/tests/`, no
fake or mock Supabase client in `:shared`, and the rig runs without Docker, so
`supabase start` is unavailable. Building a harness was considered and explicitly
rejected as out of scope.

So a confirmation here is **quoted static evidence**, not a test.

> **Read this twice: "there is no failing test" is NOT grounds to dismiss in this
> batch.** That is the normal, expected state of every bug here. Dismissing for
> lack of a test would throw away the whole batch — including high-severity
> data-loss bugs — on a technicality that was already decided.

- **confirmed** — you read the code and it says what the finding claims. Quote the
  exact lines that prove it, in `evidence`: one entry per location, with
  `location` (`File.kt:162-179`) and the lines themselves in `quote`. Each spec's
  **Confirmation method** section lists the specific things to quote — follow that
  list. Then state the concrete failure in `sequence`: which calls, in which order,
  lose which data. Quotes show what the code *says*; `sequence` is what makes it a
  bug.
- **dismissed** — **only** when the evidence *contradicts* the finding: the guard
  the finding says is missing already exists, the trigger it depends on isn't
  registered, the code has moved on. Quote the contradicting lines in `evidence`
  too. "I couldn't test it" is not a dismissal here.

Do not write tests. Do not fix anything. Do not add a test harness, a fake client,
or a SQL test dependency to make a bug verifiable — that is out of scope and would
be a scope violation, not initiative.

**This stage produces no commits in this batch.** There is no failing test to
commit; your deliverable is the verdict JSON alone. That is expected — do not
invent something to commit.
