# Tech Debt

Structural debt ranked by carrying cost. Counterweight first: MCP test coverage (22 files,
~4,900 lines, per-tool edge cases including cross-user RLS isolation and inline-unit
smuggling), the shared layer's pure-logic test suite, and the recorder-path ViewModel tests
(~67) are all in genuinely good shape. The debt below concentrates in localization, the
untested seams those suites *don't* reach, CI scope, and documentation drift.

---

## D1 — All user-facing strings are hardcoded in Kotlin — HIGH

`apps/mobile/androidApp/src/main/res/values/strings.xml` contains a single entry
(`app_name`); there are zero `stringResource`/`R.string` usages in `src/main/java`. Every
label, error message, and content description is inline — no localization is possible, and
error copy is duplicated ("Please try again." appears 8 times in `RangeSessionViewModel.kt`
alone). The biggest single pre-launch debt, and it grows with every screen added.

## D2 — CI covers only `apps/mobile` + `packages/design`; two modules merge unchecked — HIGH

`.github/workflows/android.yml:7-13` path-filters both `push` and `pull_request` to
`apps/mobile/**` and `packages/design/**`. Consequences:

- **`apps/site/**` triggers no workflow at all** — no lint, no `astro build` on PRs, no deploy
  workflow in the repo.
- **`apps/mcp/**` triggers no workflow on PR/merge.** `mcp-deploy.yml` runs
  typecheck/lint/test, but only on manual `workflow_dispatch` — a failing MCP PR merges with
  zero signal.
- `packages/design` has only a `build` script (no test/lint), so the token pipeline is
  verified only by "did it produce output."

This contradicts what README/AGENTS.md document as each module's validation commands — the
commands exist; nothing runs them automatically. Highest-leverage build-system fix available.

Related, lower severity:

- **CI doesn't actually go through Turborepo** despite `.agents/instructions/build-system.md:30`
  and `README.md:43` saying so — `android.yml:70` calls the `pnpm mobile:*` package-script
  shortcuts, and `turbo.json`'s task graph/caching goes unused. Fix the docs or the workflow.
- **The release workflow doesn't re-verify** — `release.yml:97-125` builds signed artifacts
  with no test/lint step, relying on main's debug-variant CI having been green. No guarantee
  the released commit has passing release-variant tests.

## D3 — Test coverage is absent exactly where the risk concentrates — HIGH

Two distinct holes:

- **No `androidTest` source set exists.** ViewModel logic is well covered, but nothing
  exercises the composables: pager↔state sync, the arm/auto-commit interaction, dialog flows,
  the phone/tablet split. The highest-stakes surface (execution) has zero UI-level tests.
- **Every `Supabase*Repository` is untested** — and that's where this review's worst bugs live
  (lifecycle guards, `closeTimeEntry` timestamp matching, RMW merges, DTO `@SerialName`
  correctness). Worse, the `Fake`/`InMemory` doubles re-implement merge logic that does *not*
  mirror the SQL RPC's actual semantics (e.g. `RangeSessionUseCaseTest:331-349`), so green
  tests don't validate the production path. A small integration suite against a local
  Supabase (or contract tests pinning the RPC semantics) would cover the seam.

## D4 — Documentation drift — MEDIUM-HIGH

- **`apps/mcp/README.md` documents 6 of 12 tools** (:128-296) — missing `list_range_sessions`,
  `get_range_session`, `list_tags`, `create_tag`, `archive_session`, `unarchive_session`,
  `promote_unit` — and its error-code table (:310-315) is missing four codes. It also contains
  a duplicate `## Deployment` section (:317-346 vs :412-431). `.agents/instructions/mcp.md` is
  current; the README fell behind across several epics. Either sync it or make it point at the
  instructions file as the source of truth.
- **`design-docs/RWK4-ai-integration/implementation-review.md` (2026-06-23) is stale** — its
  critical/high findings are all fixed on main; a reader (or agent) treating it as open will
  re-flag resolved issues. Mark it superseded.
- **`apps/site` has no `CONTEXT.md`**, inconsistent with the documented per-module pattern
  (`CONTEXT-MAP.md:7` describes the site inline; mobile and mcp both have linked files).

## D5 — Oversized files / god-composable — MEDIUM

`PracticePlannerViewModel.kt` (1,607 lines), `RangeworkApp.kt` (1,471),
`RangeSessionViewModel.kt` (917), `RangeSessionScreen.kt` (821). `AuthenticatedAppShell`
(`RangeworkApp.kt:511`) holds ~10 `remember` slots, five dialogs, and the entire authenticated
nav graph inline. Extracting the nav graph and the dialog cluster gives the most relief per
edit.

## D6 — Fragile string-based route logic — MEDIUM

Route handling relies on scattered `startsWith("units/")`/`endsWith("/edit")` matching across
`titleForRoute` (`RangeworkApp.kt:1414`), `shouldRefreshPlanningOnEnter` (:1443),
`editorType`/`isTopLevelRoute` (`RangeworkNavigation.kt:96-113`), and the auth redirect
(`RangeworkApp.kt:194`). Works today; breaks silently when a route is added. Centralize route
parsing or adopt type-safe navigation.

## D7 — Duplicated layout/formatting code — MEDIUM

- Phone/tablet execution layouts pass an identical ~14-argument `RangeSessionBody(...)` call
  three times (`RangeSessionScreen.kt:326-343, 427-465`); every new parameter threads through
  all three. The Units/Sessions FAB blocks (`RangeworkApp.kt:778-823`) are similarly
  near-identical.
- Two divergent `formatElapsedTime` implementations (`FinishSummaryContent.kt:203` uses
  `"%d:%02d:%02d".format`; `RangeSessionProgressHeader.kt:164` hand-rolls `padStart`) — they
  can drift visibly.
- `duplicateUnit`/`restoreUnit` (`DefaultPracticeLibrary.kt:44-83`) are near-identical
  entity→draft mappers (sessions likewise); a `toDraft()` helper removes the duplication *and*
  the inline-scope-loss footgun ([potential-bugs.md](potential-bugs.md) B17) in one change.

## D8 — MCP internal inconsistencies — MEDIUM

- Four older tools hand-roll the `{code, content, isError}` error envelope inline
  (`list-units.ts:47-59,82-94,111-123`, `list-sessions.ts` ×5, `get-user-clubs.ts:29-41`,
  `list-tags.ts:29-40`) instead of the shared `toolError()` factory every newer tool uses. Any
  envelope change now has four easy-to-miss copies.
- `list_units` makes 3 serial round-trips (units → instructions → tags,
  `list-units.ts:41-124`) where `list_sessions` was already parallelized with `Promise.all`
  (`list-sessions.ts:112-124`). Same mechanical fix, never applied.
- No idempotency guard on `create_unit`/`create_session` — documented as a deliberate
  tradeoff, but there's no backstop if an LLM retries a timed-out write.

## D9 — No spacing scale; ~470 hardcoded `.dp` literals — LOW-MEDIUM

Recurring values (16/20/12/8) with no shared tokens, despite the `packages/design` pipeline
existing and typography already being centralized. See
[integration-opportunities.md](integration-opportunities.md) I6.

## D10 — Schema and backend hygiene — LOW-MEDIUM

- **`seed.sql` is stuck at Phase 2** — only profile-name backfill; no clubs, tags, sessions,
  or range-session dev data. A fresh local instance starts empty.
- **Dead column `range_sessions.last_viewed_step_index`**
  (`20260618120000_range_sessions.sql:19`) — deserialized into the Kotlin model
  (`RangeSession.kt:21-22`) but never written and never read. It looks like it was meant for
  resume-position navigation; wire it up or drop it.
- **Superfluous grant**: `EXECUTE on audit.log_change() to authenticated, anon`
  (`20260621100000_audit_log_schema.sql:117`) — trigger functions aren't client-callable;
  the grant is misleading on a service-only audit trail.
- **`start_range_session` has been fully re-pasted eight times** across migrations — a valid
  Postgres pattern, but auditing its history means diffing full ~150-line bodies. Worth
  knowing before the next edit; consider keeping the canonical body in one referenced file.
- **Inconsistent `SECURITY` clause style** — some RPCs declare `security invoker` explicitly,
  `save_practice_*` omit it; harmless but makes SECURITY DEFINER auditing needlessly manual.
- **`config.toml` pins no Postgres version** — local `supabase start` can drift from hosted.

## D11 — Shared-layer over-fetch in list paths — LOW

`SupabasePracticeUnitRepository.kt:41-48` and `SupabasePracticeSessionRepository.kt:51-59`
fetch *all* child instruction/item/tag rows unfiltered, then discard orphans in
`assembleParentsWithChildren` (`ParentChildAssembly.kt:27-36`). Correct (RLS bounds it) but
cost grows with the whole library on every list — including archived sessions' and inline
units' children that are about to be dropped.

## D12 — Legal pages are un-customized Termly boilerplate — LOW (but reputational)

`terms-of-use.md:128-134` charges in NZD with shipping fees for a free app; :167-195 governs
message boards that don't exist; `privacy-policy.md:163-172` describes "AI document
generation" rather than MCP planning; `cookie-policy.md` is full of ad-cookie language for a
product with no ads. Reads as untrustworthy to a careful user or Play reviewer — needs one
real pass (and see [potential-bugs.md](potential-bugs.md) B13 for the compliance-relevant
piece).
