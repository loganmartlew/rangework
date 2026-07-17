Each bug below was confirmed by a previous agent from **quoted static evidence**,
not from a test. Its verdict — including the exact lines it quoted and the failure
sequence it described — is reproduced with the spec. There is no failing test to
make pass, and **you should not write one**: this batch has no test surface (no
pgTAP, no `supabase/tests/`, no fake Supabase client in `:shared`, no Docker), and
building one is explicitly out of scope.

This changes what "done" means, so read carefully:

> **There is no mechanical signal that your fix works.** No test goes from red to
> green. The suites below are a *regression* check — they tell you that you broke
> nothing, not that you fixed anything. The only things standing between a wrong
> fix and a merged wrong fix are the reviewer and a human. Write for them.

For each bug, in order:

1. Read the spec, and re-read the quoted evidence from the verify stage. Go look at
   those lines yourself — do not take the quotes on trust.
2. Fix the defect *at the layer the spec names*. These specs are prescriptive about
   this, and the prescription is the fix: e.g. "the guard is enforced **in the
   database**, not by a client-side read-then-check" — a read-check-write in Kotlin
   reintroduces exactly the race the bug is about, while looking like a fix and
   passing every suite.
3. Run the batch's test and lint commands to prove you regressed nothing.
4. Commit — **one commit per bug**, message starting with the bug id
   (`B1: guard finish/abandon via RPC`).

Migrations are **append-only**: add a new timestamped file, ordered after the
latest existing one. Never edit a migration that is already there.

Where a spec says a decision must be made deliberately and stated (e.g. whether the
already-in-that-state case is a no-op or raises), make it, and put your reasoning in
the commit message. That is the sentence the human reviewer will look for.
