# B7 — Client-side read-modify-write survives on two range-session columns

Batch: shared-repo
Source: ../../potential-bugs.md#b7 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `SupabaseRangeSessionRepository.kt:142-160` (`overrideStepClubs`), `:192-218` (`saveBlockResult`)
>
> The 2026-07-15 migration fixed the read-merge-write race for `completed_steps` with a
> `FOR UPDATE`-locked RPC — but `overrideStepClubs` and `saveBlockResult` still do
> select → merge → whole-column update, the exact pattern that caused the bug being fixed.
> `saveBlockResult` at least documents the tradeoff; `overrideStepClubs` doesn't, and is the more
> likely to race (rapid club swaps mid-block). Candidates for the same RPC treatment.

## Confirmation method

**Static evidence — no failing test.** `:shared` has no fake/mock Supabase client and this
repository has no test coverage; no SQL harness, no Docker (D2). **Do not dismiss for lack of
a test.**

Confirm by quoting, in the verify verdict:

1. `SupabaseRangeSessionRepository.kt:142-160` — `overrideStepClubs`: `getSession` →
   `session.clubOverrides + stepIndices.map { ... }` → whole-column `update` filtered by `id`.
2. `SupabaseRangeSessionRepository.kt:192-218` — `saveBlockResult`: same shape on
   `block_results`, with the tradeoff documented in the comment at lines 200-203.
3. `supabase/migrations/20260715120000_atomic_range_session_step_completion.sql:1-7` — the
   header comment describing the *identical* pattern as the bug it was written to fix
   ("The original client implementation did a SELECT, merged one counter tap locally, then
   replaced the whole array... two requests to read the same baseline and overwrite one
   another").

Point 3 is what makes this confirmable rather than speculative: the same race on the same
table was already diagnosed and fixed once. State the concrete losing interleaving for
`overrideStepClubs` (two rapid club swaps).

Note `saveBlockResult`'s existing comment explicitly *accepts* the tradeoff as a single-user
app. That is a real counter-argument, not something to brush past — address it in the verdict.
The strongest case is `overrideStepClubs` (rapid successive swaps by one user, no concurrency
required — just two in-flight requests); `saveBlockResult` may honestly be lower value. A
split verdict (confirm `overrideStepClubs`, dismiss `saveBlockResult`) is legitimate.

## Definition of done

- For each half that survived verification: a new migration under `supabase/migrations/`
  (timestamped after the latest existing file) adding a `FOR UPDATE`-locked merge RPC, plus
  the client change to call it — one PR, per D7
- The merge happens **in the database**. A client-side retry or compare-and-set does not
  satisfy this spec
- Existing semantics preserved exactly:
  - `overrideStepClubs` — sibling keys survive; the map is keyed by step index as a string
  - `saveBlockResult` — an `isEmpty` result **removes** the key rather than storing an empty
    object (`:205-209`); sibling blocks survive. This is easy to lose in translation to SQL —
    a reviewer will check it specifically
- Both still return the updated `RangeSession`
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest`
  green; `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- Scope boundary: `supabase/migrations/` (new files only), `SupabaseRangeSessionRepository.kt`.
  No Compose changes. **Do not** convert any other read-modify-write in the file on the way
  past — if you spot one, note it in the PR body.

## Notes for the fixer

- **The pattern to mirror is exact and local:**
  `supabase/migrations/20260715120000_atomic_range_session_step_completion.sql`. Same table,
  same lock, same concern — `select ... for update` (lines 27-31), `if not found then raise`
  (33-35), `security invoker`, `set search_path = ''`, `returning to_jsonb(rs.*)` (81),
  explicit `grant execute` (87-88). Your RPCs should read as siblings of that one.
- The JSONB shapes differ from `completed_steps`: `club_overrides` and `block_results` are
  **objects** keyed by index-as-string, not arrays. So the merge is `||` on jsonb objects
  (and `- key` for removal), not the array append loop that migration uses. Take the
  structure, not the array logic.
- Client-side, mirror `setStepsCompletion` (`SupabaseRangeSessionRepository.kt:121-140`) and
  the comment at 131-133 explaining why returning the updated row beats a follow-up SELECT.
  Params classes go in the `── RPC params ──` section (lines 304-317); the now-unused
  `ClubOverridesUpdate` / `BlockResultsUpdate` DTOs (lines 321-325, 345-349) should be deleted
  if nothing else references them.
- `BlockResult` is a `@Serializable` model — check how it round-trips into jsonb before
  assuming the RPC signature can take it directly.
- b01 in this same batch adds guarded finish/abandon RPCs against the same table. Expect
  overlap in the migration style; still **commit per bug**, not per batch.
