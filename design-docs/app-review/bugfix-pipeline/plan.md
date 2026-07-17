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
- [x] Write all per-bug specs into `specs/` per the approved template — 16 specs +
      [specs/README.md](specs/README.md) index. Deviated from the template on two points
      (D10 amended, see README decision log); actions arising are listed below.

### Follow-ups from spec writing (2026-07-15)

**The one thing to know:** the D10 template's "confirmed = a committed failing test,
otherwise DISMISS" rule only works for 3 of the 5 batches. **supabase-schema** and
**shared-repo** have no test surface at all — no pgTAP, no `supabase/tests/`, no fake
Supabase client in `:shared`, and no Docker per D2. They confirm by **static evidence**
instead (agreed 2026-07-15), which means those two batches have no mechanical "test went
green" signal and lean entirely on the review stage plus the human PR review.

- [x] **Blocks Phase 4:** the verify and review prompt templates must branch on the batch's
      confirmation method. For supabase-schema and shared-repo they must require quoted proof
      lines, and must **not** treat "no failing test" as grounds to dismiss — that wording
      would auto-dismiss 8 bugs, including the 3 high-severity data-loss ones (B1, B3, B7).
      Done: `prompts/partials/{verify,fix,review}-static-evidence.md` vs. `-failing-test.md`,
      selected per batch by `Batch.confirmation` in `config.mts`. The schema requires
      `evidence` + `sequence` on a static-evidence confirmation and the gate parks if either
      is missing (`confirmationDeficiency`).
- [ ] **Blocks Phase 5:** update the five batch issue bodies (#47–#51). Each still reads
      "Per-bug specs (to be written per plan.md Phase 3)" with glob placeholders — point them
      at the real files now that they exist. While there: the bodies use repo-relative links
      (`../potential-bugs.md`, `../bugfix-pipeline/batches.md`) which do **not** resolve from
      a GitHub issue; they need full blob URLs.
- [ ] **Before Phase 6:** file issues for the three sub-items D7 split out of B3's spec, or
      they'll vanish at close-out — (a) process death leaves an unclosed time entry,
      (b) rotation churn fires exit+enter per configuration change (both are android-ui
      lifecycle bugs in `RangeSessionScreen.kt` / `RangeSessionViewModel.kt`, neither fixable
      from the repo layer), (c) the two divergent duration computations — an investigation,
      not a defect.
- [ ] **After B6 and B14 land:** file the divergence issue. Both specs deliberately fix only
      their own layer, so MCP will reject free-text lengths the DB and app still accept (B6),
      and `:shared` will reject ball/repeat counts MCP and the DB still accept (B14). Each
      spec requires the gap to be named in its PR body — collect them into one follow-up.
- [ ] **Optional, revisit at D4/close-out:** tech-debt issue for the missing Supabase test
      harness. It's the root cause of the static-evidence deviation above; a fake postgrest
      client in `:shared` would make B1/B3/B7 mechanically verifiable. Considered and
      rejected as in-batch work (too big for an unattended agent) — worth doing deliberately
      if the rig outlives this bug list.

Noted, no action needed: **B18** sits in the android-ui batch but its code and tests are in
`:shared` (`PracticeDraftEditor.kt`); its spec runs both suites. Expect a `:shared` diff in
that batch's PR.

## Phase 4 — Orchestrator build

Rig lives in [`.sandcastle/`](../../../.sandcastle/README.md); run it with `pnpm pipeline <batch>`.

- [x] Scaffold Sandcastle project — `.sandcastle/` in the repo, hand-written rather than
      via `sandcastle init` (its init templates all scaffold a Docker setup, which D2 rules
      out). Package is `@ai-hero/sandcastle`; the bare `sandcastle` on npm is an unrelated
      2015 JS sandbox
- [x] ~~Implement the **host provider** via `createBindMountSandboxProvider()`~~ — **not
      needed.** Sandcastle now ships `noSandbox()` as a first-class provider (its ADR 0015):
      spawn on the host against a git worktree, with the library owning worktree creation
      and teardown, and the top-level `run()` path passing `--dangerously-skip-permissions`
      so unattended runs don't hang on prompts. It already handles the Windows quirks a
      hand-rolled provider would have had to rediscover. D2's intent is met; the custom code
      isn't. Bind-mount providers are the container-oriented path
- [x] **Windows shakedown** (`pnpm pipeline:shakedown`) — `gh`, `pnpm`, `node`, `git`,
      `gradlew.bat` all green under the provider's spawn inside a worktree. Caught the
      predicted gotcha: this host sets `NoDefaultCurrentDirectoryInExePath=1`, so `cmd.exe`
      won't resolve from the cwd and **bare `gradlew.bat` fails — it must be `.\gradlew.bat`**.
      Encoded in `config.mts`, in all prompts, and in the shakedown itself
- [x] Stage prompt templates (`.sandcastle/prompts/`: verify / fix / review + `fix-retry`)
      with structured JSON output (`schemas.mts`, zod via Sandcastle's `Output.object`).
      **They branch on the batch's confirmation method** via `prompts/partials/<stage>-<method>.md`
      — the static-evidence partials state plainly that "no failing test" is *not* grounds to
      dismiss, so the 8 static-evidence bugs (incl. B1/B3/B7) survive verify. The verify
      verdict — quotes and failure sequence included — travels with the spec into the fix and
      review stages, because on a static-evidence batch it *is* the proof (no committed test
      carries it). Prompts are passed **inline**, not as `promptFile`, so Sandcastle's
      `` !`shell` `` expansion never touches pasted spec text. `pnpm exec tsx .sandcastle/render-check.mts`
      renders all 20 prompt/batch combinations offline
- [x] Stage chain: verify → gate on verdicts → fix → guards+suites → review →
      `gh pr create` + issue label/comment updates (`lib/pipeline.mts`, `lib/github.mts`).
      Labels are the state machine, comments the log (D5)
- [x] Guards (`lib/guards.mts`): per-bug commit enforcement, additions-only check on existing
      test files, iteration caps. Both guards read **git**, not agent JSON; both tolerate a
      not-yet-created branch (static-evidence verify commits nothing). The orchestrator also
      **re-runs the suites itself** rather than trusting the fixer's `suitesGreen`
- [x] Checkpoint/resume (`lib/checkpoint.mts`): persists
      `{batch, stage, branch, sessionId, kind, verdicts}` on any throw — the full verdicts,
      so a resume doesn't lose static-evidence proof. Usage-limit classification is
      deliberately narrow (mis-flagging a real bug as a limit would sleep-and-retry forever);
      Claude limit + parseable reset + captured session → sleep then `resumeSession`; Codex
      limit → park, with `--fix-agent claude --resume` as the manual fail-over. Caveat:
      Sandcastle only surfaces `sessionId` on `StructuredOutputError`, so a usage-limit throw
      may carry none — no session, no resume, so it parks
- [ ] Dry-run the full chain on the **mcp batch** — **left for the owner** (it executes real
      bugfixes: agent runs on the host, then comments on #47, pushes a branch, opens a PR).
      Claude Code and Codex are both installed and covered by the shakedown. Preflight first:
      `pnpm pipeline:check-specs`, `pnpm pipeline:test`, and `pnpm pipeline:shakedown`.

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
