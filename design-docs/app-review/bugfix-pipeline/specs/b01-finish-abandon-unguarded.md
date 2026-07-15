# B1 — Range-session finish/abandon transitions are unguarded (data loss)

Batch: shared-repo
Source: ../../potential-bugs.md#b1 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/SupabaseRangeSessionRepository.kt:162-179`
>
> Both `finishSession` and `abandonSession` issue unconditional column updates filtered only by
> `id` — no `WHERE completed_at IS NULL` / `abandoned_at IS NULL` guard:
>
> - **Abandon-after-finish destroys history.** `RangeSession.state` checks `abandonedAt` first
>   (`RangeSessionRecordingRules.kt:18-23`), and `listCompletedSessions` filters
>   `abandoned_at IS null` (`SupabaseRangeSessionRepository.kt:84-85`). A stray abandon call on
>   an already-completed session silently flips it to ABANDONED and removes it from history.
> - **Double-finish re-stamps `completed_at`**, moving the completion time and any duration
>   derived from it.
> - Finish-after-abandon yields a row with both timestamps set.
>
> The recording writes are carefully guarded by the freeze matrix; the lifecycle transitions
> themselves are not. The UI probably prevents these today, but the shared contract offers no
> protection — and B2 below shows how easily two concurrent writers appear.
> **Fix:** an atomic guarded RPC (or conditional update) mirroring
> `set_range_session_steps_completion`.

## Confirmation method

**Static evidence — no failing test.** `:shared` has no fake/mock Supabase client and
`SupabaseRangeSessionRepository` has no test coverage at all; the rig has no SQL harness and
no Docker (D2). Building a postgrest fake was considered and rejected as out of scope for this
batch. **Do not dismiss this bug for lack of a test** — that outcome is explicitly disallowed
here.

Confirm by quoting, in the verify verdict:

1. `SupabaseRangeSessionRepository.kt:162-171` (`finishSession`) and `:173-179`
   (`abandonSession`) showing each update filtered by `eq("id", rangeSessionId)` alone.
2. `RangeSessionRecordingRules.kt:18-23` showing `state` resolves `abandonedAt` before
   `completedAt` — this is what makes abandon-after-finish *destructive* rather than merely
   untidy.
3. `SupabaseRangeSessionRepository.kt:83-85` showing `listCompletedSessions` filters
   `exact("abandoned_at", null)` — the session disappears from history.

Then state the concrete sequence: which two calls, in which order, lose which data. DISMISS
only if the evidence contradicts the finding (e.g. a guard already exists in the RPC layer).

## Definition of done

- A new migration under `supabase/migrations/` (timestamped after the latest existing file)
  adding guarded finish and abandon RPCs, **plus** the client change in
  `SupabaseRangeSessionRepository` to call them — one PR, per D7
- The guard is enforced **in the database**, not by a client-side read-then-check: a
  read-check-write in Kotlin reintroduces exactly the race this bug is about
- The already-in-that-state case has a defined, documented outcome — decide deliberately
  between a no-op and a raised exception, and state which in the migration comment and the
  PR body. This is the call a reviewer will scrutinize: `abandonSession` returns `Unit`
  today, so a thrown error is a new failure mode the ViewModels must tolerate; check
  `RangeSessionViewModel`'s abandon path (`notification = "Failed to abandon session: ..."`)
  before choosing
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest`
  green; `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- Scope boundary: `supabase/migrations/` (new file only), `SupabaseRangeSessionRepository.kt`,
  and the `RangeSessionRepository` interface **only if** the return contract must change. No
  Compose changes. No changes to the recording/freeze-matrix rules.

## Notes for the fixer

- **The pattern to mirror is named in the finding and exists in the repo:**
  `supabase/migrations/20260715120000_atomic_range_session_step_completion.sql`. Study it
  closely — it is the template for this entire batch:
  - `select ... from public.range_sessions where id = ... for update` (lines 27-31), with
    `if not found then raise exception 'Range session not found or not accessible'` (33-35)
  - `security invoker` + `set search_path = ''` — RLS enforces ownership, the lock enforces
    serialization; the comment header at lines 1-7 explains exactly this
  - `returning to_jsonb(rs.*) into v_result` (line 81) so the caller gets the updated row
    without a follow-up SELECT that could race
  - explicit `grant execute on function ... to authenticated`
- Client-side, mirror `setStepsCompletion` (`SupabaseRangeSessionRepository.kt:121-140`): a
  `@Serializable` params data class with `@SerialName("p_...")` fields,
  `client.postgrest.rpc(name, Json.encodeToJsonElement(...).jsonObject)`, then
  `.decodeAs<RangeSession>()`. The params-class declarations live in the
  `── RPC params ──` section at the bottom of the file (lines 304-317) — put yours there.
- `finishSession` currently stamps `Clock.System.now()` **client-side** (line 164). Moving the
  guard into an RPC is the natural moment to let the database stamp `now()` instead, as
  `set_range_session_steps_completion` does (line 21). Prefer that; note it in the PR body.
- Related-but-separate: ball-weighted gap crediting also runs at finish, in SQL. Do not
  disturb it — if the guarded finish RPC must coexist with it, say so in the PR body rather
  than refactoring it into your new function.
- B7 (in this same batch) needs the identical RPC treatment for two other columns. If you fix
  both, still **commit per bug**, not per batch.
