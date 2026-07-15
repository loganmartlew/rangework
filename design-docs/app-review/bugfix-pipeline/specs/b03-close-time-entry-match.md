# B3 — `closeTimeEntry` matches on timestamp string equality; silent time loss

Batch: shared-repo
Source: ../../potential-bugs.md#b3 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `SupabaseRangeSessionRepository.kt:265-278`
>
> The close update filters `eq("entered_at", enteredAt.toString())`, with no
> `exited_at IS NULL` filter. Two fragilities:
>
> 1. `timestamptz` equality via a serialized `Instant` string is brittle — microsecond
>    truncation or `+00:00` vs `Z` format drift makes the filter match zero rows, silently
>    leaving the entry open. Only closed entries contribute to elapsed time
>    (`getElapsedSeconds`, lines 286-288), so a missed close silently drops range time.
> 2. Without the open-entry filter, a re-close overwrites an existing exit timestamp.
>
> Related duration issues:
> - **Two divergent duration computations exist.** The shared layer sums closed intervals
>   (`SupabaseRangeSessionRepository.kt:101-104, 280-289`); ball-weighted gap crediting lives in
>   SQL at finish. Confirm which number each UI surface shows and whether they can disagree.
> - **Process death leaves an unclosed time entry.** `onScreenExit` runs from a
>   `DisposableEffect`/`onCleared` (`RangeSessionScreen.kt:128-131`,
>   `RangeSessionViewModel.kt:891-894`), neither guaranteed on process death; the open entry is
>   never closed and a fresh one opens on resume.
> - **Rotation churn:** the same `DisposableEffect(Unit)` fires exit+enter on every
>   configuration change — two Supabase writes per rotation, fragmenting the time record.
>   Key the enter/exit to lifecycle, not composition.

## Scope: the "Related duration issues" are NOT in this spec (D7)

Per decision D7, **this spec covers only the `closeTimeEntry` filter hardening** — the two
numbered fragilities above. The four "Related duration issues" bullets are explicitly out of
scope:

- *Two divergent duration computations* — an investigation, not a defect with a fix. File a
  follow-up issue if the verify stage turns up evidence they can disagree.
- *Process death leaves an unclosed entry* and *rotation churn* — both are Compose lifecycle
  bugs in `RangeSessionScreen.kt` / `RangeSessionViewModel.kt`, i.e. the android-ui batch's
  surface, and neither is fixable from the repository layer. File follow-up issues; do not
  fix them here.

Do not let the fix creep into the lifecycle. A PR touching `RangeSessionScreen.kt` fails this
spec.

## Confirmation method

**Static evidence — no failing test.** `:shared` has no fake/mock Supabase client and this
repository has no test coverage; no SQL harness, no Docker (D2). **Do not dismiss for lack of
a test.**

Confirm by quoting, in the verify verdict:

1. `SupabaseRangeSessionRepository.kt:265-278` — the update filtered on
   `eq("range_session_id", ...)` + `eq("entered_at", enteredAt.toString())`, with no
   `exited_at` filter.
2. `SupabaseRangeSessionRepository.kt:286-288` — `getElapsedSeconds` sums only entries where
   `exitedAt != null`, so an unmatched close silently costs range time (no error surfaces:
   a postgrest update matching zero rows is not an error).
3. For fragility 1 specifically: identify what `Instant.toString()` actually serializes to
   (kotlinx-datetime, ISO-8601 with `Z`) and what `entered_at` was written as — note
   `recordTimeEntry` (`:256-263`) inserts the `Instant` via the serializer, **not** via
   `toString()`. Whether a round-trip mismatch is *reachable* hinges on this, so establish it
   rather than assuming. If the two provably always agree, downgrade fragility 1 to
   defence-in-depth in the verdict and confirm on fragility 2 — which is unconditional and
   sufficient on its own.

## Definition of done

- `closeTimeEntry` no longer depends on timestamp-string equality **and** cannot overwrite an
  already-closed entry
- The fix is enforced server-side (filter or guarded RPC), not by a client read-then-check
- Zero-row-matched is no longer silent: the caller can tell that a close did nothing. Decide
  deliberately between a returned indicator and a raised exception, and check
  `RangeSessionViewModel`'s screen-exit path tolerates it — `onScreenExit` runs from
  `onCleared`, where a thrown exception has nowhere to go
- `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest`
  green; `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` green
- Scope boundary: `SupabaseRangeSessionRepository.kt`, the `RangeSessionRepository` interface
  if the contract must change, and a new `supabase/migrations/` file if you take the RPC
  route. **No Compose changes. No ViewModel lifecycle changes.**

## Notes for the fixer

- The cleanest shape is likely "close the open entry for this session" rather than "close the
  entry with this exact `entered_at`" — i.e. filter on `range_session_id` +
  `exited_at IS NULL`, which fixes both fragilities at once and stops depending on the
  timestamp round-trip. Note this changes the method's identity semantics: if multiple open
  entries exist for a session (which the rotation-churn bug can produce), "the open one" is
  ambiguous. Handle it explicitly — closing the most recent, or all of them, are both
  defensible; picking silently is not.
- The `enteredAt` parameter may become unused. If so, remove it from the interface rather than
  leaving a lying signature — but that is a `RangeSessionRepository` contract change, so
  update every implementation, including the test fakes in
  `apps/mobile/androidApp/src/test/java/.../RangeSessionViewModelTest.kt` and
  `CompletedRangeSessionViewModelTest.kt`. Those fakes are pre-existing test files: the
  additions-only guard permits the mechanical signature update required to keep them
  compiling, but nothing more.
- If you take the RPC route, mirror
  `supabase/migrations/20260715120000_atomic_range_session_step_completion.sql` — `for update`
  lock, `security invoker`, `set search_path = ''`, explicit grant. See b01's notes.
- Postgrest filter idioms already used in this file: `exact("exited_at", null)` for IS NULL
  (see `listActiveSessions`, lines 61-64) and `filterNot("completed_at", FilterOperator.IS, null)`
  for IS NOT NULL (line 83). Use them; don't hand-roll strings.
