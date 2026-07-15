# Bugfix Pipeline — autonomous burn-down of potential-bugs.md

Strategy for fixing the 22 findings in [../potential-bugs.md](../potential-bugs.md) with
minimal human input, using scripted headless agent runs (Claude Code + Codex) orchestrated
by [Sandcastle](https://github.com/mattpocock/sandcastle) with a custom host provider.

## Files

| File                         | Contents                                                        |
| ---------------------------- | --------------------------------------------------------------- |
| [plan.md](plan.md)           | To-do checklist: setup tasks, open decisions, execution phases   |
| [batches.md](batches.md)     | Batch composition, per-batch status, per-bug dispositions        |
| `specs/` (created in setup)  | Per-bug spec files fed to the agents — one file per pipeline bug |

## Pipeline shape

One orchestrator script per batch, chaining three headless stages over a git worktree on
the host (no containers):

1. **Verify — Claude.** Reads the batch's bug specs, confirms or dismisses each bug.
   For testable logic bugs, "confirmed" means a committed failing test. Emits structured
   JSON verdicts; dismissed bugs drop out with a written reason.
2. **Fix — Codex.** Receives only confirmed bugs + failing tests + definition of done.
   Iterates until the new tests and full suite are green. **Commits per bug**, not per
   batch, so an interrupted run loses at most one bug's work.
3. **Review — Claude.** Diffs the branch against the specs; checks the fix addresses the
   spec rather than gaming the test. Clean → script opens the PR via `gh`. Findings →
   one bounded Codex retry, then stop and flag regardless of outcome.

Human involvement per batch: launch the script, review the resulting PR. Nothing merges
without a human.

## Hard rules (guardrails for unattended runs)

- **Definition of done is deterministic per bug**: new failing test passes + existing
  suites green + lint green + explicit scope boundary. No done-criteria → no fix attempt.
- **Can't reproduce → dismiss with reason.** Never fix speculatively.
- **No automatic retry loops.** One attempt per stage (plus the single bounded review
  retry). Anything unresolved → checkpoint, comment on the batch issue, label
  `needs-info`, stop.
- **Existing test files are additions-only.** The orchestrator mechanically rejects diffs
  that modify or delete pre-existing tests (anti-test-gaming guard).
- **Usage-limit hits are checkpoints, not failures.** Sandcastle captures `sessionId` /
  `branch` / `preservedWorktreePath` on throw. Claude 5-hour limit → sleep until reset,
  resume the session. Codex weekly cap → park the batch or fail the fix stage over to
  Claude (specs are harness-agnostic by design).

## Decision log

| # | Decision | Status | Outcome |
| - | -------- | ------ | ------- |
| D1 | Execution model | **Decided** | Manually launched orchestrator runs (one command per batch, or sequential all-batches run); no event-driven triggering unless the rig outlives this bug list |
| D2 | Sandbox provider | **Decided** | Custom Sandcastle bind-mount provider running on the host (git worktree + spawn); no Docker — local Android SDK and warm Gradle caches used as-is |
| D3 | Model roles | **Decided** | Claude = verify + review (judgment); Codex = fix (weekly bucket suits long Gradle-heavy runs) |
| D4 | OpenRouter cheap tier | **Decided** | Not for this project — addressable surface is ~1 batch (B6); revisit only if Codex's weekly bucket runs dry or the rig becomes a standing setup. Keep the provider abstraction clean so adding one is config, not refactor |
| D5 | Durable state | **Decided** | GitHub issues, one per batch (not per bug), using existing triage labels; labels are the state machine, comments are the fix/dismissal log |
| D6 | Out-of-pipeline items | **Decided** | B10–B12 fixed by hand (trivial); B13, B21, B22 deferred to their own issues (need product/legal/design decisions) |
| D7 | Batch composition | **Decided** (2026-07-15) | Batches as proposed in [batches.md](batches.md). Sub-decisions: B3 spec covers only `closeTimeEntry` hardening (lifecycle/rotation items split out at spec-writing time); shared-repo batch carries its own guarded-RPC migrations (client change + RPC in one PR); B9 may be confirmed via a unit test on the index mapping, or dismissed to tech-debt if there's no good testing surface |
| D8 | Pre-emptive dismissals | **Decided** (2026-07-15) | None — all 16 pipeline bugs get a verify run |
| D9 | Batch ordering / scheduling | **Decided** (2026-07-15) | Overnight-unattended, cheap batches (mcp, supabase-schema) first as rig shakedown, then the Kotlin batches |
| D10 | Spec format sign-off | **Decided** (2026-07-15) | Template in plan.md approved as-is |
