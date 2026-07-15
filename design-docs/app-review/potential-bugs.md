# Potential Bugs

Defects and latent defects found in static review, ranked by severity. Items marked **latent**
are not currently reachable through normal UI flows but will bite when an adjacent change lands.

Verified-fixed items are excluded: the OAuth consent race
(`apps/site/src/components/oauth/consent-logic.ts:26-51` now waits on `onAuthStateChange` with a
10s timeout), the MCP regression script field names, JWKS test path, `fetchAllClubCodes`
error handling, and the private-SDK-field access in the fetch handler are all fixed on `main`.

---

## High

### B1 — Range-session finish/abandon transitions are unguarded (data loss)

`apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/SupabaseRangeSessionRepository.kt:162-179`

Both `finishSession` and `abandonSession` issue unconditional column updates filtered only by
`id` — no `WHERE completed_at IS NULL` / `abandoned_at IS NULL` guard:

- **Abandon-after-finish destroys history.** `RangeSession.state` checks `abandonedAt` first
  (`RangeSessionRecordingRules.kt:18-23`), and `listCompletedSessions` filters
  `abandoned_at IS null` (`SupabaseRangeSessionRepository.kt:84-85`). A stray abandon call on
  an already-completed session silently flips it to ABANDONED and removes it from history.
- **Double-finish re-stamps `completed_at`**, moving the completion time and any duration
  derived from it.
- Finish-after-abandon yields a row with both timestamps set.

The recording writes are carefully guarded by the freeze matrix; the lifecycle transitions
themselves are not. The UI probably prevents these today, but the shared contract offers no
protection — and B2 below shows how easily two concurrent writers appear.
**Fix:** an atomic guarded RPC (or conditional update) mirroring
`set_range_session_steps_completion`.

### B2 — Double-tap on "Start session" creates duplicate range sessions

`apps/mobile/androidApp/.../ui/screens/SessionDetailScreen.kt:147`,
`PracticePlannerViewModel.kt:1057-1078`

`SessionDetailScreen` gates the Start button only on the static `isSessionExecutable`, and
`PracticePlannerViewModel.startRangeSession` has no in-flight guard. Two quick taps launch two
`start()` calls → two active range sessions; navigation lands on the second, orphaning the
first into the Active carousel. The picker path already guards with `isStartingRangeSession`
(`RangeworkApp.kt:566,757`) — the same pattern just isn't applied here. Note
`startRangeSessionFromPicker` (line 1439) also lacks a VM-level guard and survives only
because the UI disables the picker; a VM-level guard would protect both.

### B3 — `closeTimeEntry` matches on timestamp string equality; silent time loss

`SupabaseRangeSessionRepository.kt:265-278`

The close update filters `eq("entered_at", enteredAt.toString())`, with no
`exited_at IS NULL` filter. Two fragilities:

1. `timestamptz` equality via a serialized `Instant` string is brittle — microsecond
   truncation or `+00:00` vs `Z` format drift makes the filter match zero rows, silently
   leaving the entry open. Only closed entries contribute to elapsed time
   (`getElapsedSeconds`, lines 286-288), so a missed close silently drops range time.
2. Without the open-entry filter, a re-close overwrites an existing exit timestamp.

Related duration issues:
- **Two divergent duration computations exist.** The shared layer sums closed intervals
  (`SupabaseRangeSessionRepository.kt:101-104, 280-289`); ball-weighted gap crediting lives in
  SQL at finish. Confirm which number each UI surface shows and whether they can disagree.
- **Process death leaves an unclosed time entry.** `onScreenExit` runs from a
  `DisposableEffect`/`onCleared` (`RangeSessionScreen.kt:128-131`,
  `RangeSessionViewModel.kt:891-894`), neither guaranteed on process death; the open entry is
  never closed and a fresh one opens on resume.
- **Rotation churn:** the same `DisposableEffect(Unit)` fires exit+enter on every
  configuration change — two Supabase writes per rotation, fragmenting the time record.
  Key the enter/exit to lifecycle, not composition.

### B4 — Audit log grows O(N²) on range-session step completion

`supabase/migrations/20260621100000_audit_log_schema.sql`,
`20260715120000_atomic_range_session_step_completion.sql`

`audit.log_change()` diffs whole-row JSONB on UPDATE and logs the full new value of any
changed column. `range_sessions.completed_steps` holds the *entire* array and is rewritten
once per ball tap — so a session with N ball steps writes audit rows containing arrays of
size 1, 2, 3 … N: O(N²) bytes for one session's completion history. `block_results` and
`club_overrides` have the same shape on every partial update. Exclude these columns from the
diff (or log deltas) before real usage volume arrives.

### B5 — `profiles` INSERT/DELETE RLS policies are unreachable (grant/policy mismatch)

`supabase/migrations/20260615110600_auth_foundation.sql:103-114`,
`20260615120500_profiles_delete_policy.sql`, `20260621210000_add_profile_names.sql:77-79`

The only table-level grant ever issued on `profiles` is `SELECT, UPDATE` — no INSERT or
DELETE grant exists in the migration history, so those RLS policies can never pass the
privilege check. Profile creation works only via the `SECURITY DEFINER` trigger
`sync_profile_from_auth_user`. The DELETE policy is dead code implying a capability the API
can't actually deliver. Either grant the privileges or drop the dead policies so the schema
tells the truth.

### B6 — MCP: no length caps and no pagination on the list tools

`apps/mcp/src/validation/inline-units.ts:78-131`,
`supabase/migrations/20260615132000_phase3_data_foundation.sql:28,46`,
`apps/mcp/src/tools/list-units.ts`, `list-sessions.ts`

Two compounding issues:

1. **No max-length validation on any free-text field** — `title`, instruction `text`, `notes`,
   `focus`, `success_criterion` are checked only for non-emptiness at every layer (tool
   validation and DB constraints alike). An LLM or malformed client can persist arbitrarily
   large strings.
2. **`list_units` and `list_sessions` return everything** — no `limit`/cursor, unlike
   `list_range_sessions` (`list-range-sessions.ts:47-54`, default 20). A large library returns
   every unit/session with full instruction text in one response.

Together these are the most likely path to a genuinely large/slow response or an LLM context
blowout.

---

## Medium

### B7 — Client-side read-modify-write survives on two range-session columns

`SupabaseRangeSessionRepository.kt:142-160` (`overrideStepClubs`), `:192-218` (`saveBlockResult`)

The 2026-07-15 migration fixed the read-merge-write race for `completed_steps` with a
`FOR UPDATE`-locked RPC — but `overrideStepClubs` and `saveBlockResult` still do
select → merge → whole-column update, the exact pattern that caused the bug being fixed.
`saveBlockResult` at least documents the tradeoff; `overrideStepClubs` doesn't, and is the more
likely to race (rapid club swaps mid-block). Candidates for the same RPC treatment.

### B8 — Missing FK-support indexes

- `practice_session_items.practice_unit_id`
  (`20260615132000_phase3_data_foundation.sql:66-85`): the `ON DELETE RESTRICT` FK has no
  index, so every attempted unit delete scans the items table.
- `range_session_time_entries.range_session_id` (`20260618120000_range_sessions.sql:31-37`):
  no index, yet `getElapsedSeconds`/`listCompletedSessions` group by exactly this column on
  every completed-session summary fetch.

### B9 — Drag-to-reorder relies on hardcoded header offsets (latent)

`UnitEditorScreen.kt:78-79` (`headerCount = 5`), `SessionEditorScreen.kt:104-105`
(`headerCount = 1`)

The list-index → instruction-index mapping subtracts a magic count of preceding non-list
`item {}` blocks. Adding or removing any header item silently shifts every drag by one with no
compile-time signal. Derive the offset or use segmented lists/stable keys.

### B10 — Local MCP `deploy` script silently skips the R2 methodology upload

`apps/mcp/package.json:11`

`"deploy": "wrangler deploy"` omits the `wrangler r2 object put` step. Both
`apps/mcp/README.md:405-431` and `.agents/instructions/mcp.md:89` document deploy as a
two-step upload-then-deploy — the two-step sequence exists only in
`.github/workflows/mcp-deploy.yml:44-56`. A manual hotfix deploy ships code without the
matching coaching-guide update, exactly the tool/guide drift the guide-versioning scheme was
designed to prevent.

### B11 — Site: og:image is an SVG; social previews break

`apps/site/src/layouts/Layout.astro:10`, `index.astro:30`

Facebook, X, LinkedIn, Slack, and iMessage do not render SVG `og:image`. Shared links show no
preview. Needs a ~1200×630 raster card.

### B12 — Site: every store CTA points at the Play Store homepage

`apps/site/src/lib/links.ts:1`

`playStoreHref = 'https://play.google.com/store'` — the generic homepage, not the app listing,
across 4+ call sites (Nav, Hero, CtaSection, ai-planning). Likely a deliberate placeholder
while the app is in closed testing, but there is no TODO/marker, so it's primed to ship
forgotten. The site's entire conversion funnel is currently a dead end.

### B13 — Cookie Policy references a consent mechanism that doesn't exist

`apps/site/src/pages/cookie-policy.md:24-28`

The policy repeatedly references a "Cookie Preference Center" reachable from a banner — no
banner or preference center exists anywhere in the site source. If any non-essential cookies
are actually set this is a GDPR/ePrivacy gap; if not, the policy is misleading boilerplate.

---

## Low / latent

### B14 — Snapshot expansion is unbounded

`DraftValidation.kt:16-21, 85`

Ball counts are validated only non-negative and repeat counts only `> 0`. Server-side snapshot
expansion creates one step per ball × repeat — a unit with `ballCount = 1_000_000` becomes a
million-row snapshot at start. Cheapest guard is an upper bound at the validation layer
(mirrored in MCP validation and the DB check constraint).

### B15 — `executionBlocks()` silently drops steps with out-of-range `unitIndex`

`ExecutionBlocks.kt:38-47` vs `RangeSessionProgress.kt`

Blocks are built from `units.mapIndexed`, so steps referencing `unitIndex >= units.size`
vanish from the block view while `totalStepCount()` still counts them — a session that can
never reach 100%. Snapshots are server-generated so likelihood is low; a decode-time assertion
(`steps.unitIndex ∈ units.indices`) would make the failure loud instead of silent.

### B16 — Inline-unit orphan GC race on concurrent session saves

`supabase/migrations/20260713130000_inline_units.sql:78-213`

`save_practice_session` mints fresh inline-unit UUIDs and reaps orphans in the same call
without locking the session row. A double-submitted save can leave orphaned
`scoped_to_session_id` rows with no referencing item and no future GC path.

### B17 — `restoreUnit` loses inline scoping

`DefaultPracticeLibrary.kt:65-83`, `InMemoryPracticeUnitRepository.kt:46`

`PracticeUnitDraft` has no `scopedToSessionId`, so restore-after-delete of an inline unit
recreates it as a library citizen. Narrow path (inline units normally die with their session
via cascade), but the abstraction leaks — see the `toDraft()` consolidation in
[tech-debt.md](tech-debt.md) D11.

### B18 — Validation errors misrouted or swallowed in the draft editor

`PracticeDraftEditor.kt:200, 215, 248`

`ValidationTarget.UnitInstructions` maps onto `titleError` ("At least one instruction is
required" appears under the title field), and `ValidationTarget.Tags` is dropped in both
`placeUnitErrors` and `placeSessionErrors` — a `MAX_TAGS_PER_ITEM` violation fails the save
with no visible error anywhere.

### B19 — Identical consecutive error snackbars don't re-show

`RangeSessionScreen.kt:134-138`

The snackbar effect keys on the message string, so two back-to-back failures with the same
copy ("Couldn't record ball. Please try again.") show only once, masking repeated transient
errors. Key on a monotonic event id instead.

### B20 — `observations.step_index` unvalidated against snapshot bounds

`20260709120000_range_session_data_recording.sql:97-111`

Only `step_index >= 0` is checked; nothing prevents observations for indices beyond the
snapshot's step count or for non-Ball steps. Deliberately deferred to app-level rules, but a
real DB-level integrity gap if any client bug or future MCP write path touches this table.

### B21 — delete-account is not atomic across services

`supabase/functions/delete-account/index.ts:41-69`

Deleting `auth.users` (cascading owned data) and scrubbing `audit.events` are two separate
calls; a crash between them leaves PII in the audit log — self-documented in the function's
own error message. Wants a retry/reconciliation job before GA.

### B22 — Planning saves are last-write-wins with no concurrency check

`20260616200000_atomic_planning_save_rpcs.sql`

`save_practice_unit`/`save_practice_session` delete-and-reinsert children with no
`updated_at` compare; an older client's save silently clobbers a newer one. Long-standing
design (single-user assumption); becomes real the moment two devices edit the same library —
see [integration-opportunities.md](integration-opportunities.md) I4.
