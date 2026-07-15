# B16 — Inline-unit orphan GC race on concurrent session saves

Batch: supabase-schema
Source: ../../potential-bugs.md#b16 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `supabase/migrations/20260713130000_inline_units.sql:78-213`
>
> `save_practice_session` mints fresh inline-unit UUIDs and reaps orphans in the same call
> without locking the session row. A double-submitted save can leave orphaned
> `scoped_to_session_id` rows with no referencing item and no future GC path.

## Confirmation method

**Static evidence — no failing test.** (No pgTAP / `supabase/tests/`; no Docker in the rig
per D2. Do not dismiss for lack of a test.)

Read `supabase/migrations/20260713130000_inline_units.sql:78-213` in full and, in the verify
verdict, quote:

1. The point where `save_practice_session` mints inline-unit UUIDs.
2. The orphan-reaping delete (the statement that removes `scoped_to_session_id` rows with no
   referencing `practice_session_items` row).
3. The **absence** of any `select ... for update` on the `practice_sessions` row — or any
   other serialization (advisory lock, etc.) — between the two.

Then state the interleaving concretely: which statement of save A runs between which two
statements of save B to strand a row. A verdict that just repeats the finding is not
confirmation. DISMISS if a lock or an equivalent guard turns out to be present, or if the
reaping delete provably cannot strand a row under any interleaving (say why).

Also confirm the "no future GC path" half — i.e. that nothing else ever reaps orphaned
scoped units. That is the claim that turns a transient race into permanent garbage, and it's
the part most likely to be wrong.

## Definition of done

- One new migration under `supabase/migrations/`, timestamped after the latest existing file,
  replacing `save_practice_session` so the mint-and-reap sequence is serialized per session
- The fix holds for the concrete interleaving named in the verify verdict — state in the PR
  body why that interleaving can no longer strand a row
- No behaviour change for the single-save path
- No existing migration file is edited (append-only migration history)
- Scope boundary: `supabase/migrations/` only. **Do not** attempt to reconcile or clean up
  pre-existing orphaned rows — a data-repair migration is a separate decision and belongs in
  its own PR.

## Notes for the fixer

- **The pattern to mirror is in the repo already:**
  `supabase/migrations/20260715120000_atomic_range_session_step_completion.sql:25-35` — a
  `select ... from public.range_sessions where id = ... for update` taken at the top of the
  function, with `if not found then raise exception` immediately after, and a comment
  explaining that RLS enforces ownership while `FOR UPDATE` serializes concurrent mutations.
  That migration exists precisely because the same class of race was fixed for
  `completed_steps`. Follow its shape: same lock idiom, same `security invoker`,
  `set search_path = ''`, same commentary style.
- Lock the **session** row, not the unit rows — the session is the thing two saves contend
  over, and it's the row whose children are being deleted and re-inserted.
- Note the related-but-distinct B22 (planning saves are last-write-wins with no `updated_at`
  compare) is **deferred** to [#46](https://github.com/loganmartlew/rangework/issues/46) and
  is explicitly **not** in scope. B16 is only about not stranding orphan rows; it is not
  about deciding which of two concurrent saves should win. Do not add an optimistic-
  concurrency check here — that's #46's design decision to make.
