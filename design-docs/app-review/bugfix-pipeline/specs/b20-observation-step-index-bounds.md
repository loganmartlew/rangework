# B20 — `observations.step_index` unvalidated against snapshot bounds

Batch: supabase-schema
Source: ../../potential-bugs.md#b20 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `20260709120000_range_session_data_recording.sql:97-111`
>
> Only `step_index >= 0` is checked; nothing prevents observations for indices beyond the
> snapshot's step count or for non-Ball steps. Deliberately deferred to app-level rules, but a
> real DB-level integrity gap if any client bug or future MCP write path touches this table.

## Confirmation method

**Static evidence — no failing test.** (No pgTAP / `supabase/tests/`; no Docker in the rig
per D2. Do not dismiss for lack of a test.)

Quote `20260709120000_range_session_data_recording.sql:97-111` showing the
`step_index >= 0` check constraint and the absence of any upper-bound or step-type check.

**This bug's verdict is a judgment call, and dismissal is a legitimate outcome.** The finding
concedes the gap is "deliberately deferred to app-level rules". Before confirming, establish
whether the gap is reachable: enumerate every write path to `range_session_observations` —
the Kotlin `upsertObservation`
(`apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/SupabaseRangeSessionRepository.kt:229-244`)
and any MCP tool that writes observations (grep `apps/mcp/src/` — at the time of writing the
MCP surface is read-only for observations). Then state whether app-level rules currently
bound the index.

- Confirm if a reachable path can write an out-of-bounds index.
- **Dismiss to tech-debt** if every current writer is bounded by app-level rules and the
  finding reduces to defence-in-depth against a hypothetical future writer. Say so plainly —
  "the only claimed trigger is a future MCP write path that does not exist" is a good
  dismissal, and is a better outcome than adding a constraint nobody needs.

## Definition of done

Only if confirmed:

- One new migration under `supabase/migrations/`, timestamped after the latest existing file,
  adding the bound check
- The constraint cannot reject writes the app legitimately makes today — argue this
  explicitly in the PR body, referencing the snapshot's step count
- No existing migration file is edited (append-only migration history)
- Scope boundary: `supabase/migrations/` only. No Kotlin, no TypeScript.

## Notes for the fixer

- The hard part is that the bound lives in **another row's JSONB**: the valid range is
  `0 <= step_index < jsonb_array_length(range_sessions.snapshot -> 'steps')` for the parent
  session. A `check` constraint cannot reference another table, so the honest options are a
  trigger, or a `check` against a denormalized step-count column. Both are heavier than the
  gap warrants — which is a strong hint that dismissal may be the right verdict. Do not
  quietly implement a weaker constraint (e.g. a fixed magic upper bound like 10_000) and call
  the bug fixed; that would be gaming the spec.
- The "non-Ball steps" half of the finding is strictly harder than the bounds half — it needs
  the step's type from inside the snapshot array. Treat it as a separate sub-verdict; it is
  very likely a dismissal even if the bounds half is confirmed.
- Related in spirit but out of scope: B14 (unbounded snapshot expansion) lives in the
  shared-validation batch. Don't reach for it here.
