# Integration Opportunities

Cross-module seams: places where one module already has a capability another module can't
reach, plus the structural integration risks (rule triplication, offline, multi-device) that
sit between modules rather than inside any one of them.

---

## I1 — Close the MCP write-tool gap: update, duplicate, delete

The DB layer already supports all three; the MCP surface just doesn't expose them.

- **No edit/update tools.** `save_practice_unit`/`save_practice_session` are true upserts
  (`ON CONFLICT (id) DO UPDATE`, `20260630120000_per_instruction_club.sql:37-57`,
  `20260713130000_inline_units.sql:78-102`) — the app's own editors reuse them by passing an
  existing id. But every MCP write path mints `crypto.randomUUID()`
  (`create-unit.ts:101`, `create-session.ts:363`), so an AI can never edit, only recreate.
  `update_unit(unit_id, …)`/`update_session(session_id, …)` are thin wrappers over the same
  RPC + `validateInlineUnit`, plus an ownership check RLS already enforces.
- **No `duplicate_session` tool**, despite `duplicate_practice_session(p_source_id, p_new_id)`
  existing and handling inline-unit deep copy correctly
  (`20260713130000_inline_units.sql:261-339`). Session duplication is an advertised app
  feature (`RANGEWORK.md:32`); the tool is near-zero effort — same registration pattern as
  `promote_unit`. Enables "build me three variations of this session" in one turn.
- **No `delete_unit` tool.** Archiving covers sessions, but there's no way to retire an
  AI-minted library unit that turned out unwanted. The design doc defers unit *archiving*
  (`design-docs/mcp-data-clutter/design-decisions.md:158-159`), but a plain delete guarded by
  the existing `ON DELETE RESTRICT` FK isn't discussed and closes the clutter loop the epic
  started.

While in there: add `limit`/pagination to `list_units`/`list_sessions` and free-text length
caps ([potential-bugs.md](potential-bugs.md) B6) — both matter more as AI-driven creation
scales library size.

## I2 — A relational read layer over the ball-granular data

Today, MCP analytics are entirely app-side over JSONB: `get-range-session.ts` and
`list-range-sessions.ts` each fetch raw `snapshot` + `completed_steps` + observations and do
all grouping in TypeScript, per request. No SQL view flattens a session into per-ball rows,
so **no cross-session aggregate is possible at all** without fetching every session's full
snapshot client-side.

The highest-leverage single change in this review:

1. **A `range_session_step_events`-style view** (or materialized table) joining
   `snapshot->steps`, `completed_steps`, `club_overrides`, and `range_session_observations`
   into one row per ball: unit, instruction, rep, club (override-resolved), `completed_at`,
   observation values. Every consumer benefits at once — the two MCP tools drop their
   duplicated JSONB traversal, the future in-app stats screen
   ([high-value-features.md](high-value-features.md) F1) gets plain SQL, and per-ball timing
   ("average seconds between balls") becomes a `GROUP BY`.
2. **A `practice_session_totals` view** (ball count, unit count, tags) — "total ball count at
   a glance" is core UX currently recomputed client-side in at least two places.
3. **A periodically-refreshed `user_stats` materialized view** for volume/most-used-club
   surfaces (F9).

The per-ball timestamps and club-per-ball data are already captured correctly — this is purely
an exposure problem.

## I3 — Domain rules are triplicated across Kotlin, TypeScript, and SQL

The freeze matrix (`RangeSessionRecordingRules`), tag slugification (`slugifyTag`), the
success-requires-criterion rule, and validation bounds each exist in up to three
implementations: shared KMP, `apps/mcp/src/validation`, and SQL RPC backstops. Code comments
dutifully note the mirrors, which is correctness-by-convention with no enforcement — a change
to any rule requires remembering all three sites.

Full unification isn't realistic across Kotlin/TS/SQL, but two cheap mitigations:

- **Make SQL the authoritative backstop for every invariant** (some already are, e.g. the
  success-criterion RPC check; length caps and step-index bounds are not — see
  [potential-bugs.md](potential-bugs.md) B6, B20). Client-layer copies then only affect error
  quality, not integrity.
- **A shared conformance fixture** — a JSON file of rule cases (slug inputs/outputs, freeze
  matrix states, validation boundaries) consumed by both the Kotlin and TS test suites, so
  drift fails a test instead of a user.

## I4 — Offline: the data model is ready, the plumbing is absent

Every repository call goes straight to PostgREST; each `+1` is a network round-trip; the
`InMemory*` repos are test doubles, not a cache tier. Process-death *resume* works precisely
because state is server-persisted per action — the flip side is no-signal ranges can't count
balls.

The integration observation worth recording: **ball-granular step completion is already an
idempotent, timestamped, mergeable event log** — and the server merge RPC
(`set_range_session_steps_completion`) already resolves concurrent completions. A local
outbox that queues step-completion events and replays them through the existing RPC is a
natural fit; the baseline plan's SQLDelight direction remains the right vehicle. Planning
writes (`save_practice_*`) are the harder half (last-write-wins, B22) and can come later —
offline *execution* alone covers the core use case.

## I5 — Multi-device conflict handling is asymmetric

Step completion merges server-side under `FOR UPDATE` (good — and the RPC returns the merged
row, avoiding a racing re-SELECT, `SupabaseRangeSessionRepository.kt:131-139`). Everything
else — club overrides, block results, session notes, lifecycle transitions, planning saves —
is last-writer-wins. Single-device is fine; the product explicitly supports tablet layouts
(`RANGEWORK.md:65`), so phone+tablet users can silently lose block notes and manual counts.
The fix list is finite: give `overrideStepClubs` and `saveBlockResult` the same RPC treatment
(B7) and guard lifecycle transitions (B1). Notes and planning saves can stay LWW with eyes
open.

## I6 — Design tokens reach the site and Android build, but not Compose spacing

`packages/design` feeds the Android build and the site, and typography is centralized in the
app (`RangeworkMono`/`MaterialTheme.typography`) — but spacing never joined: ~470 hardcoded
`.dp` literals with recurring values (16/20/12/8) and no shared scale
([tech-debt.md](tech-debt.md) D9). A small spacing object sourced from the token pipeline
would complete the loop the package was built for.

## I7 — Site ↔ product integration is drifting

The site is the only module with no CI gate and no `CONTEXT.md`, and it shows: execution copy
describes the retired wizard ([usability.md](usability.md) U9), legal pages are un-customized
boilerplate, and the AI-planning page under-links the MCP feature. Two structural fixes make
the drift self-correcting rather than needing repeated copy audits: put the site in CI
([tech-debt.md](tech-debt.md) D2) and add `apps/site/CONTEXT.md` with the copy-accuracy rule
`CONTEXT-MAP.md` already states, so agent-driven site work loads it.

## I8 — What's already well integrated (don't touch)

- **MCP ↔ recorded data**: `get_range_session`/`list_range_sessions` close the coaching-read
  loop, and the coaching guide instructs consulting them before diagnosing. The next step is
  methodology, not tools (F8).
- **Snapshot compatibility**: `ignoreUnknownKeys`, unknown observation types dropped rather
  than crashing, single `snapshotVersion >= 3` feature predicate — the strongest part of the
  shared layer.
- **Shared-layer boundary**: `DataFoundation`/`RangeworkFoundation` expose guarded facades
  (`PracticeLibrary`, `RangeSessionRecorder`) rather than raw repos, and degrade to friendly
  setup messaging when unconfigured, per the AGENTS.md rule.
- **Schema ↔ query alignment**: the partial indexes on `range_sessions` match the MCP and app
  list queries exactly.
