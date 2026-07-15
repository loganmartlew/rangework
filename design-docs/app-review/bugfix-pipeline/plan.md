# Plan — to-dos and phases

Work through the phases in order. Open decisions (D7–D10 in [README.md](README.md)) gate
the phases they're listed under. Check items off in place.

## Phase 0 — Decisions (human)

- [x] **D7:** Confirm/adjust batch composition in [batches.md](batches.md) — confirmed as proposed, incl. all three open batching questions (see README decision log)
- [x] **D8:** Mark any pre-emptive dismissals in batches.md — none; all 16 pipeline bugs get a verify run
- [x] **D9:** Confirm batch ordering and scheduling — overnight-unattended, cheap batches first
- [x] **D10:** Approve the per-bug spec template (below) — approved as-is

### Per-bug spec template (D10)

One file per pipeline bug at `specs/bNN-slug.md`:

```markdown
# B2 — Duplicate range sessions on double-tap

Batch: android-ui
Source: ../potential-bugs.md#b2 (copy the full finding text in here — specs must be self-contained)

## Confirmation method

Unit test in <suggested test file> calling startRangeSession twice concurrently;
asserts exactly one session is created. If no failing test can be produced that
matches the claimed behaviour, DISMISS with reason.

## Definition of done

- New test passes
- :shared and :androidApp test suites green; lint green
- Scope boundary: changes limited to PracticePlannerViewModel + SessionDetailScreen;
  no public API changes outside these files

## Notes for the fixer

Existing guard pattern to mirror: isStartingRangeSession in RangeworkApp.kt:566,757
```

## Phase 1 — Quick wins by hand (no pipeline, ~30 min interactive)

- [ ] **B12:** Point `playStoreHref` at the real app listing (or add a loud TODO if the
      listing URL doesn't exist yet) — `apps/site/src/lib/links.ts:1` — deferred, no
      listing yet
- [x] **B11:** Generate a ~1200×630 raster og:image and reference it from
      `Layout.astro` / `index.astro` — done via a new `og-card` target in the
      `@rangework/design` brand pipeline (`packages/design/generators/brand/emit-og-card.mjs`)
- [x] **B10:** Add the R2 methodology upload to the local `deploy` script in
      `apps/mcp/package.json` (mirror `.github/workflows/mcp-deploy.yml:44-56`) — resolved
      differently: removed the local `deploy` script entirely, docs now point at the
      `MCP Deploy` GitHub Actions workflow as the only deploy path

## Phase 2 — Deferred items → issues (human decisions needed, not pipeline work)

- [x] Create issue for **B13** (cookie policy vs. nonexistent consent mechanism —
      legal/content decision: add banner or fix policy text) — [#44](https://github.com/loganmartlew/rangework/issues/44)
- [x] Create issue for **B21** (delete-account atomicity — needs a retry/reconciliation
      design before GA) — [#45](https://github.com/loganmartlew/rangework/issues/45)
- [x] Create issue for **B22** (last-write-wins planning saves — multi-device concurrency
      design; links to integration-opportunities.md I4) — [#46](https://github.com/loganmartlew/rangework/issues/46)

## Phase 3 — GitHub scaffolding

- [x] Create one issue per batch (5 issues per batches.md), body = links to the batch's
      spec files + the state-machine contract — [#47 mcp](https://github.com/loganmartlew/rangework/issues/47),
      [#48 supabase-schema](https://github.com/loganmartlew/rangework/issues/48),
      [#49 shared-validation](https://github.com/loganmartlew/rangework/issues/49),
      [#50 shared-repo](https://github.com/loganmartlew/rangework/issues/50),
      [#51 android-ui](https://github.com/loganmartlew/rangework/issues/51); all labeled
      `needs-verification`
- [x] Confirm/create labels used by the pipeline: `needs-verification`, `ready-for-agent`,
      `in-progress`, `ready-for-human`, `needs-info` — `ready-for-agent` already existed;
      the other four created. Also applied `needs-info` to the Phase 2 deferred issues
      (#44, #45, #46), which are blocked on a human decision.
- [ ] Write all per-bug specs into `specs/` per the approved template

## Phase 4 — Orchestrator build

- [ ] Scaffold Sandcastle project (`sandcastle init`) — decide where it lives: .sandcastle folder in the repo - User Decision
- [ ] Implement the **host provider** via `createBindMountSandboxProvider()`:
      `exec` = spawn with cwd=worktree, `copyFileIn/Out` = fs, `close` = worktree removal.
      Crib from `src/sandboxes/docker.ts`
- [ ] **Windows shakedown:** verify `gradlew.bat`, `pnpm`, and `gh` all run correctly
      under the provider's spawn (shell/quoting) inside a worktree — this is the expected
      first-hour-of-debugging item
- [ ] Write the three stage prompt templates (verify / fix / review) with structured
      JSON output schemas
- [ ] Implement the stage chain: verify → gate on verdicts → fix → review →
      `gh pr create` + issue label/comment updates
- [ ] Implement guards: per-bug commit enforcement, additions-only check on existing
      test files, iteration caps
- [ ] Implement checkpoint/resume: persist `{batch, stage, sessionId, branch}` on any
      throw; classify usage-limit errors loosely (unknown error after partial progress =
      park, never auto-retry); Claude limit → sleep-until-reset + `resumeSession`;
      Codex limit → park or fail fix stage over to Claude
- [ ] Dry-run the full chain on the **mcp batch** (smallest, fastest feedback) before
      trusting it with a Kotlin batch

## Phase 5 — Execution (one line per batch; details in batches.md)

- [ ] Run **mcp** batch (also the rig shakedown)
- [ ] Run **supabase-schema** batch
- [ ] Run **shared-validation** batch
- [ ] Run **shared-repo** batch
- [ ] Run **android-ui** batch
- [ ] Review + merge each PR; record dispositions in batches.md as they land

## Phase 6 — Close-out

- [ ] Reconcile batches.md: every bug fixed / dismissed-with-reason / deferred-with-issue
- [ ] Update ../potential-bugs.md header (or add a disposition column) so the review doc
      reflects reality
- [ ] Decide whether the rig is kept (→ revisit D1/D4) or archived
