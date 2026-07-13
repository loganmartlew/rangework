# MCP & Data Clutter — Epic Implementation Plan

**Date:** 2026-07-13
**Status:** proposed — awaiting owner sign-off on stage split and decision points
**Design:** [`design-decisions.md`](design-decisions.md) (agreed 2026-07-13). Vocabulary:
`apps/mobile/CONTEXT.md` (Archived, Inline Unit, Promotion).
**Process precedent:** range-session-data-recording epic
(`design-docs/range-session-data-recording/`) — epic plan, then one
`implementation/stage-NN-*/implementation-plan.md` + `changes.md` per stage.

## Shape of the epic

Two features, two stage groups, one theme: stop AI-planned one-offs from cluttering the
system. Three structural calls drive the split:

1. **Archiving lands first; both groups are committed.** Archiving is the base Inline Units
   stand on — an Inline Unit's "dormant when archived" behaviour is meaningless until archiving
   exists — so its foundation (Stage 1) gates everything. Both features are confirmed in scope
   (owner decision 2026-07-13), so the Inline Units foundation runs concurrently with the
   archiving surface stages rather than waiting behind a decision gate.

2. **Schema per group, not per epic.** Unlike the data-recording epic there is no snapshot
   version bump forcing all schema up front — the two features touch different tables with
   trivial columns (`archived_at` on `practice_sessions`; `scoped_to_session_id` on
   `practice_units`). Each group opens with its own small foundation stage; the Inline Units
   migration isn't written until the group is confirmed go.

3. **MCP stages parallelize, app stages don't.** Same precedent as data-recording Stage 7:
   `apps/mcp` and `apps/mobile` have zero file overlap, so each group's MCP stage runs
   concurrently with its app stage once the group's foundation merges.

No Range Session behaviour changes anywhere in this epic. The Snapshot's immunity is what
makes every lifecycle rule here safe (archive-while-active, cascade delete of inline units);
no execution code is touched.

## Stages

```
Stage 1: Archiving foundation (schema + shared)
    │
    ├─────────────────────┬─────────────────────┐
    ▼                     ▼                     ▼
Stage 2:              Stage 3:              Stage 4:
Archiving app UI      Archiving MCP         Inline Units foundation
(⛳ ship point 1)                            (schema + RPC + shared) + ADR
    │                     │                     │
    │  (needs 2 + 4)      │  (needs 3 + 4)      │
    └────────┬────────────┼─────────────────────┤
             ▼            └──────────┬──────────┘
Stage 5: Inline Units app UI         ▼
             │            Stage 6: Inline Units MCP
             └──────────────┬────────┘
                            ▼
                     ⛳ ship point 2
```

**Parallelism rules:**

| Can run concurrently | Why it's safe |
| --- | --- |
| Stage 2 with Stage 3 | Different apps (`apps/mobile` vs `apps/mcp`), zero file overlap |
| Stage 4 with Stages 2 and 3 | Stage 4 is migrations + shared KMP unit/session-save code; Stage 2 is `androidApp` UI, Stage 3 is `apps/mcp` — minimal overlap, and Stage 4's tables/RPC are untouched by archiving |
| Stage 5 with Stage 6 | Different apps, zero file overlap |

| Must stay sequential | Why |
| --- | --- |
| 1 → 2, 1 → 3 | Both consume the archived state in shared models / schema |
| 1 → 4 | Inline lifecycle rules ("dormant when archived", duplicate-from-archived deep copy) build on the archived state and its tests |
| 4 → 5, 4 → 6 | Both consume ownership schema, RPC shape, and shared use cases |
| 2 → 5 | Both rework the session detail/editor screens — parallel branches would collide on the same composables |
| 3 → 6 | Both rework the MCP tool surface and coaching guide — same files |

**Pipelining rule** (unchanged from precedent): stage N+1's plan drafts as soon as stage N's
plan is approved; implementation waits for merge where the table says sequential. Owner review
of a stage plan is the serialization point.

### Stage 1 — Archiving foundation

- Migration: `archived_at timestamptz null` on `practice_sessions` (timestamp over boolean —
  "when" comes free). No RLS changes expected (same-owner rows). Check whether any list RPC /
  view needs the column surfaced.
- Shared (KMP): archived state on the session model; `archiveSession` / `unarchiveSession` in
  `PracticeLibrary`; session listing split (default list excludes archived; archived list).
  **Decide in stage plan:** where default-exclusion lives — recommendation: the repository
  layer, one choke point, so no caller can accidentally leak archived sessions into a default
  listing.
- Guard rails as testable logic: start-Range-Session and edit paths reject archived sessions
  (belt-and-braces behind UI gating); duplicate-from-archived produces an unarchived copy;
  archive with an Active Range Session in flight is permitted.
- Unit tests are the bulk: state transitions, listing splits, guard rails.

Validation: migration clean on local Supabase; KMP unit tests green; existing session flows
unaffected (archived_at null everywhere).

### Stage 2 — Archiving app UI  ⛳ ship point 1

- Archive action on session detail (and/or list overflow) for unarchived sessions.
- **Archived destination**: separate screen off the session list (quiet entry point — overflow
  item or footer row like "Archived (12)"); rows offer view, duplicate, unarchive, delete —
  no start, no edit. Entry point placement/copy: **decide in stage plan**.
- Unarchive action on archived session detail.
- **Finish-screen affordance**: a deliberately secondary "Archive this session" control in the
  Range Session finish flow, archiving the source template; self-hides when the template is
  already archived. Must read as passive, matching the Block Result stance — never a prompt or
  dialog. Exact placement/weight: **decide in stage plan**.
- Session detail for an archived session shows its state plainly and gates edit/start behind
  unarchive.

Validation: build + manual flow on device — archive from finish screen, find it in Archived,
duplicate from archive, unarchive, re-run. History for the archived template still groups.

### Stage 3 — Archiving MCP (parallel with Stage 2)

- `archive_session(id)`, `unarchive_session(id)` — explicit-verb tools per the design.
- `list_sessions`: `include_archived` param (default `false`); archived results carry a
  visible archived marker in the response shape.
- Tool descriptions cover the semantics the model must know: archived ≠ deleted, history
  survives, unarchive to reuse, archiving is safe while a Range Session is Active.
- Coaching guide: when tidying is appropriate (user-initiated — e.g. the external
  knowledge-base flow from the design §5); never archive unprompted.
- MCP regression additions for the three tool behaviours.

Validation: `pnpm --filter @rangework/mcp test` + regression run; manual conversation: plan →
run (or skip) → "archive that session" → confirm hidden from default `list_sessions`.

### Stage 4 — Inline Units foundation (parallel with Stages 2–3)

- Migration: `scoped_to_session_id uuid null` on `practice_units`,
  `references practice_sessions (id) on delete cascade`; index on the column. RLS review
  (owner-scoped as today; scoping adds no cross-user surface).
- `create_session` RPC (`save_practice_session` family): items accept `unit_id` *or* an
  embedded inline unit definition; session + inline units + items created atomically. This RPC
  is the project's most complex SQL and has been reworked repeatedly — **payload shape
  proposed in the stage plan for owner review** before implementation.
- **Decide in stage plan:** duplication locus — deep-copying inline units when a session is
  duplicated, client-side (shared KMP logic) vs in an RPC. Recommendation: wherever session
  duplication lives today; deep copy must be atomic with the duplicate.
- Shared (KMP): ownership on the unit model; library queries exclude inline units; session
  detail includes them; promotion use case (detach ownership); deep-copy duplication;
  promotion is the only path out, deletion cascades.
- **ADR written this stage** (`docs/adr/`): inline-unit ownership via nullable
  session reference + cascade delete, vs visibility flag / separate table. Hard to reverse,
  surprising to future readers, genuine trade-off — qualifies on all three counts.
- Unit tests: library exclusion, cascade behaviour, deep-copy independence, promotion
  detachment (session keeps referencing the same unit id).

Validation: migration + RPC exercised against local Supabase with a mixed payload (library
ref + inline def); v-existing sessions and plain `create_session` calls unaffected.

### Stage 5 — Inline Units app UI

- Session detail/editor renders inline units as session content; tapping edit navigates to the
  **existing unit editor** (saves key off unit ID — the editor needs no inline-awareness
  beyond hiding library-only affordances if any).
- **Promote** button on the inline unit within its session; promoted unit appears in the
  library, session unaffected.
- Unit library continues to exclude inline units (should fall out of Stage 4's repository
  work; verify, don't re-implement).

Validation: build + manual flow — open an MCP-created session with an inline unit, edit it,
promote it, confirm it appears in the library and the session still references it.

### Stage 6 — Inline Units MCP (parallel with Stage 5)

- `create_session` tool schema: items take `unit_id` or an inline definition (same shape as
  `create_unit`'s input); tool description teaches the model when to inline (one-off drill for
  this session) vs create a library unit (player asked for a reusable drill).
- `promote_unit(id)`: user-initiated only — tool description must say "invoke when the player
  asks to keep/reuse a drill", never proactively.
- Session detail responses expose inline units (that's how the AI finds one to promote);
  `list_units` untouched — pure library.
- Coaching guide: inline-by-default for session-specific drills; promotion etiquette.
- Regression additions.

Validation: MCP tests + a real planning conversation that creates a session with an inline
unit; then a follow-up conversation that finds and promotes it.

## Operating model — how a stage runs

Identical to the data-recording epic (its §"Operating model" applies verbatim): plan →
owner review (serialization point) → implement on a feature branch (`data-clutter-stage-NN`)
→ validate per the stage's checkpoint → merge → `changes.md` in the stage folder. Agents
draft, implement, test, record; Logan reviews plans at the flagged decision points and
merges PRs.

## Decision points (owner involvement)

No further product grilling needed — the design doc resolves the product questions. Flagged
check-ins, all async:

| When | What | Format |
| --- | --- | --- |
| Stage 1 planning | Repository-level default exclusion; anything the migration must surface to list RPCs | Written recommendation in stage plan |
| Stage 2 planning | Archived-destination entry point; finish-screen affordance placement/weight (the "quiet, not jumping out" requirement is subjective — one screenshot/mock in the plan) | Async review |
| Stage 4 planning | `create_session` RPC payload shape for embedded inline definitions; duplication locus; ADR text | Async review — the RPC shape is the one expensive-to-change surface |
| Stage 6 planning | Tool schema + coaching-guide wording (inline-by-default guidance and promotion etiquette shape model behaviour) | Async review |

## Validation gates

- Per-stage checkpoints as above (KMP unit tests carry the lifecycle invariants; build +
  manual device flow for UI stages; MCP regression for Stages 3/6).
- **End-to-end walkthrough at each ship point**, exercising the real loop the epic exists
  for: (1) plan via MCP → run → archive from the finish screen → confirm the list is clean and
  history intact; (2) plan via MCP with an inline unit → run → conversationally promote the
  drill into a new session.
- Scope tripwires from the design: no auto-archive; no unit archiving; no demotion; no
  dedup of deep-copied inline units; no app-side inline creation. All are §10 deferred/rejected
  — resist mid-stage temptation.

## Risks

| Risk | Mitigation |
| --- | --- |
| `save_practice_session` RPC regressions (most complex SQL, touched again for embedded units) | Stage 4 owner-reviewed payload shape before code; extend existing RPC tests with mixed payloads; plain payloads must be bit-identical in behaviour |
| Cascade delete destroys a unit the user thought was in the library | Promotion is the designed escape hatch; delete confirmation copy in-app should say inline units go with the session; cascade applies only to `scoped_to_session_id` rows |
| Archived sessions leak into default listings (query misses the filter) | Single repository choke point for default exclusion (Stage 1 decision); unit tests on every listing path |
| `list_sessions` default change surprises existing MCP conversations | Archived-marker in responses; tool description states the default; `include_archived` is one param away |
| Finish-screen affordance too prominent (nags) or too buried (unused) | Stage 2 plan includes a mock; it's one composable to tune after field use |

## Epic close

After ship point 2: update `design-decisions.md` status line, confirm
`apps/mobile/CONTEXT.md` vocabulary matches what shipped, file follow-up issues for §10
deferred items (unit decluttering, app-side inline creation, retro-scoping), and update the
project memory.
