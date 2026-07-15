# B4 — Audit log grows O(N²) on range-session step completion

Batch: supabase-schema
Source: ../../potential-bugs.md#b4 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `supabase/migrations/20260621100000_audit_log_schema.sql`,
> `20260715120000_atomic_range_session_step_completion.sql`
>
> `audit.log_change()` diffs whole-row JSONB on UPDATE and logs the full new value of any
> changed column. `range_sessions.completed_steps` holds the *entire* array and is rewritten
> once per ball tap — so a session with N ball steps writes audit rows containing arrays of
> size 1, 2, 3 … N: O(N²) bytes for one session's completion history. `block_results` and
> `club_overrides` have the same shape on every partial update. Exclude these columns from the
> diff (or log deltas) before real usage volume arrives.

## Confirmation method

**Static evidence — no failing test.** This batch has no SQL test harness (no pgTAP, no
`supabase/tests/`, and the rig runs without Docker per D2, so `supabase start` is
unavailable). Do not dismiss for lack of a test.

Confirm by quoting, in the verify verdict:

1. The diff-and-log block in `audit.log_change()` —
   `20260621100000_audit_log_schema.sql:95-110` — showing the exclusion list is exactly
   `('updated_at', 'created_at')` and that the full `v_new_row -> k` value of every other
   changed column is stored.
2. The trigger registration that attaches `audit.log_change()` to `range_sessions` (find it;
   confirm it fires on UPDATE).
3. The per-tap write path: `set_range_session_steps_completion`
   (`20260715120000_atomic_range_session_step_completion.sql:78-81`) updates
   `completed_steps` with the whole merged array on every counter tap.

Those three together establish the quadratic growth claim. DISMISS only if the evidence
contradicts it — e.g. `range_sessions` turns out not to carry the trigger at all.

## Definition of done

- One new migration under `supabase/migrations/` with a timestamped filename ordered after
  `20260715120000`
- The three named columns (`completed_steps`, `block_results`, `club_overrides`) no longer
  have their full value written to `audit.events` on every partial update, **and** an UPDATE
  touching only those columns still leaves an audit trail of *some* form (do not silently
  drop the event entirely — see notes)
- Audit behaviour for every other table is provably unchanged
- No existing migration file is edited (append-only migration history)
- `pnpm --filter @rangework/mcp test` and the Android suites are unaffected; if nothing else
  in the repo references the changed function, say so explicitly rather than skipping the check
- Scope boundary: `supabase/migrations/` only. No Kotlin, no TypeScript.

## Notes for the fixer

- `audit.log_change()` is **generic** — one function shared by every audited table, with the
  PK column list passed via `TG_ARGV[0]` (`20260621100000_audit_log_schema.sql:55-59`,
  comma-separated, defaults to `'id'`). So the exclusion cannot be hardcoded to
  range-session column names without silently changing behaviour for unrelated tables. The
  natural shape is a second trigger argument (`TG_ARGV[1]`) carrying the columns to redact
  for that table, defaulting to none — then re-create only the `range_sessions` trigger with
  it. Follow the existing `string_to_array(TG_ARGV[0], ',')` idiom.
- Beware the early return at lines 101-103: `if v_new_diff = '{}' then return NEW`. If you
  redact by removing keys from the diff, an update that touched *only* redacted columns
  collapses to `'{}'` and logs nothing at all. Decide deliberately: either log the event with
  a redaction marker (e.g. the column name mapped to a placeholder rather than its value), or
  accept the dropped event and justify it in the PR body. Silently losing the "this session's
  progress changed" audit trail is a behaviour change a reviewer must see argued.
- `v_old_diff` is derived from `v_new_diff`'s keys (lines 105-107), so redacting a key from
  the new diff removes it from both sides — check that's what you want.
- Migration style: match the surrounding files — a comment header explaining *why*,
  `create or replace function`, `security definer`, `set search_path = ''`, explicit
  `grant execute`.
