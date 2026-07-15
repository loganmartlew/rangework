# B5 — `profiles` INSERT/DELETE RLS policies are unreachable (grant/policy mismatch)

Batch: supabase-schema
Source: ../../potential-bugs.md#b5 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `supabase/migrations/20260615110600_auth_foundation.sql:103-114`,
> `20260615120500_profiles_delete_policy.sql`, `20260621210000_add_profile_names.sql:77-79`
>
> The only table-level grant ever issued on `profiles` is `SELECT, UPDATE` — no INSERT or
> DELETE grant exists in the migration history, so those RLS policies can never pass the
> privilege check. Profile creation works only via the `SECURITY DEFINER` trigger
> `sync_profile_from_auth_user`. The DELETE policy is dead code implying a capability the API
> can't actually deliver. Either grant the privileges or drop the dead policies so the schema
> tells the truth.

## Confirmation method

**Static evidence — no failing test.** (No pgTAP / `supabase/tests/`; no Docker in the rig
per D2. Do not dismiss for lack of a test.)

Confirm by grepping the **whole** of `supabase/migrations/` for every `grant` touching
`profiles` and quoting the complete result set in the verify verdict. The claim is confirmed
if and only if the union of those grants contains no `insert` and no `delete` for
`authenticated`, while an INSERT and/or DELETE **policy** on `profiles` does exist. Quote
both sides. DISMISS if a grant is found anywhere in the history that the finding missed.

This one is genuinely falsifiable from the migration text alone — be exhaustive rather than
trusting the three files the finding cites.

## Definition of done

- One new migration under `supabase/migrations/`, timestamped after the latest existing file
- The schema tells the truth: either the missing privileges are granted, **or** the
  unreachable policies are dropped — not both, and the choice is argued in the migration's
  comment header (see notes)
- Profile creation via the `sync_profile_from_auth_user` trigger still works by inspection,
  and the reasoning is stated
- No existing migration file is edited (append-only migration history)
- Scope boundary: `supabase/migrations/` only. Do not touch the delete-account Edge Function
  (`supabase/functions/delete-account/`) — account deletion is B21's territory and is
  deferred to [#45](https://github.com/loganmartlew/rangework/issues/45).

## Notes for the fixer

- **This is a judgment call, not a mechanical fix, and it is the reason this bug is in a
  review-heavy batch.** The two directions are not equivalent:
  - *Drop the dead policies* — asserts that clients must never insert or delete a profile
    directly, and that the `SECURITY DEFINER` trigger plus the delete-account function are
    the only sanctioned paths. This matches how the app behaves today.
  - *Grant the privileges* — opens a client-facing capability that nothing currently uses.
    Only defensible if a caller actually needs it.
  Prefer the direction that matches observed behaviour (drop), and if you choose otherwise,
  name the caller that needs the privilege. Do not grant privileges "for symmetry".
- Check whether anything else — the delete-account Edge Function especially — depends on the
  DELETE policy being present before dropping it. It runs with the service role, which
  bypasses RLS, but verify rather than assume, and record the check.
- Match surrounding migration style: comment header explaining why, then the DDL.
