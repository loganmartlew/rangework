# B8 — Missing FK-support indexes

Batch: supabase-schema
Source: ../../potential-bugs.md#b8 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> - `practice_session_items.practice_unit_id`
>   (`20260615132000_phase3_data_foundation.sql:66-85`): the `ON DELETE RESTRICT` FK has no
>   index, so every attempted unit delete scans the items table.
> - `range_session_time_entries.range_session_id` (`20260618120000_range_sessions.sql:31-37`):
>   no index, yet `getElapsedSeconds`/`listCompletedSessions` group by exactly this column on
>   every completed-session summary fetch.

## Confirmation method

**Static evidence — no failing test.** (No pgTAP / `supabase/tests/`; no Docker in the rig
per D2. Do not dismiss for lack of a test.)

Confirm by grepping **all** of `supabase/migrations/` for `create index` mentioning each of
the two columns, and quoting the (expected-empty) results alongside the two FK declarations
that need them. Note that a later migration may have added an index the finding predates —
that's exactly what the exhaustive grep is for. DISMISS either half whose index already
exists.

The read-path claim for `range_session_time_entries.range_session_id` is corroborated in
Kotlin at `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/SupabaseRangeSessionRepository.kt:92-99`
(`isIn("range_session_id", sessionIds)`) and `:280-289` (`eq("range_session_id", ...)`) —
cite it, but do not change it.

## Definition of done

- One new migration under `supabase/migrations/`, timestamped after the latest existing file,
  adding the missing index for each half that survived verification
- `create index if not exists`, matching the existing index idiom in the migration history
- Index names follow the convention already used in the repo (see the `events_*_idx` names in
  `20260621100000_audit_log_schema.sql:20-27` and the indexes in the two cited migrations —
  match the dominant local pattern, don't invent a new one)
- No existing migration file is edited (append-only migration history)
- Scope boundary: `supabase/migrations/` only. No Kotlin, no TypeScript.

## Notes for the fixer

- This is the most mechanical bug in the batch. Resist scope creep: add exactly the two
  indexes the finding names. Do not audit the rest of the schema for other unindexed FKs in
  this PR — if you notice more, list them in the PR body for a follow-up issue instead.
- Plain single-column B-tree indexes are what's wanted. No partial or covering indexes
  without a stated reason.
- Both indexes are write-path costs on tables that are written frequently
  (`range_session_time_entries` gets a row per screen enter/exit). That tradeoff is fine and
  expected — mention it in the PR body, don't agonize over it.
