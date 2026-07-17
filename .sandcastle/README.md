# Bugfix pipeline — the rig

Orchestrator for the autonomous burn-down of `design-docs/app-review/potential-bugs.md`.
Strategy, decisions, and batch composition live in
[design-docs/app-review/bugfix-pipeline/](../design-docs/app-review/bugfix-pipeline/README.md) —
this file covers only how to run the thing.

## Commands

```powershell
pnpm pipeline --list              # batch status + any parked checkpoints
pnpm pipeline:check-specs         # preflight: does every bug have a spec?
pnpm pipeline:test                # stage-contract and git-guard tests
pnpm pipeline:shakedown           # preflight: do gh/pnpm/gradlew.bat run under the provider?
pnpm pipeline mcp                 # run one batch
pnpm pipeline mcp --fix-agent claude
pnpm pipeline mcp --resume        # continue from a checkpoint
```

Exit codes: `0` landed, `2` parked (checkpointed and reported on the issue), `1` crashed.

## Layout

| Path                | Contents                                                             |
| ------------------- | -------------------------------------------------------------------- |
| `main.mts`          | CLI entry — arg parsing, checkpoint loading                           |
| `config.mts`        | Batch definitions, agent roles, branch naming. Mirrors `batches.md`   |
| `schemas.mts`       | Zod schemas for the three stages' structured output                   |
| `prompts/`          | Stage prompt templates (`{{PLACEHOLDER}}` filled in `lib/prompts.mts`) |
| `lib/pipeline.mts`  | The stage chain and its gates                                         |
| `lib/guards.mts`    | Mechanical checks read out of git                                     |
| `lib/checkpoint.mts`| Park/resume state and failure classification                          |
| `lib/github.mts`    | `gh` wrappers — labels as state, comments as log                      |
| `shakedown.mts`     | Windows toolchain check                                               |

Per-run output (`worktrees/`, `logs/`, `state/`) is gitignored.

## How it runs

Sandcastle drives each stage as a single `run()` against a git worktree at
`.sandcastle/worktrees/bugfix-<batch>/`, on branch `bugfix/<batch>` off `main`.

```
verify (Claude)  ->  gate  ->  fix (Codex)  ->  guards + suites  ->  review (Claude)
                      |            |                  |                    |
                 none confirmed?   |            violation/red?        clean -> PR
                 -> stop           |            -> park              findings -> one
                                   |                                  bounded retry
                              any unresolved?                         -> stop, flag
                              -> park
```

Every stage is `maxIterations: 1` — that is both what structured output requires
and exactly the "one attempt per stage" cap the hard rules ask for. The agent
loops internally to reach green; what it never gets is a second fresh attempt of
its own accord.

## What is checked rather than trusted

Agent-reported JSON is a claim. The orchestrator independently verifies:

- **Per-bug commits** — read from `git log`, not from the fixer's report.
- **Failing-test confirmation** — the named test must exist in that bug's verify
  commit, and the configured test command must be red before fixing.
- **Additions-only on existing test files** — read from `git diff --numstat`.
  The fix stage is graded by "the tests pass", so the cheapest way to pass is to
  edit the test. This makes that mechanically impossible rather than merely
  forbidden in the prompt. Tests committed by verify are additionally frozen at
  the exact post-verify commit so the fixer cannot rewrite or remove them.
- **The suites** — re-run by the orchestrator against the branch. The fix stage
  reports `suitesGreen` itself; that field exists to be compared against reality,
  not to be believed.

## Sandbox model

`noSandbox()` — the agent runs **directly on this host** with permissions
bypassed, against a git worktree. That is decision D2 (no Docker; use the local
Android SDK and warm Gradle caches as-is), and the worktree is a blast-radius
convenience, not a security boundary. Do not point this rig at a repo you would
not hand an unsupervised shell.

Sandcastle ships `noSandbox()` as a first-class provider, so the custom
`createBindMountSandboxProvider()` the plan called for was not needed — the
library's version already handles the Windows quirks (`cmd.exe` routing, PATHEXT
resolution for `.cmd` shims) that a hand-rolled one would have had to rediscover.

## Windows notes

`NoDefaultCurrentDirectoryInExePath=1` is set on this host, so `cmd.exe` will not
resolve an executable from the current directory. **Invoke the Gradle wrapper as
`.\gradlew.bat`, never bare `gradlew.bat`** — bare fails with "not recognized as
an internal or external command". `config.mts` and every prompt already say so;
`pnpm pipeline:shakedown` is what keeps that claim honest after a toolchain
change.

## Before the first real run

1. **Write the specs.** `pnpm pipeline:check-specs` — plan.md Phase 3. A batch
   with a missing spec refuses to start rather than quietly skipping the bug.
2. **Confirm the agent CLIs and model selection.** `pipeline:shakedown` checks that
   both Claude Code and Codex are executable. `CODEX_MODEL` in `config.mts` can
   be overridden with `RANGEWORK_CODEX_MODEL` when the configured model changes.
3. `gh auth status` — checked at entry, because a run that does all its work and
   then fails at the PR step wastes the whole night.

## Checkpoints

Any unresolved outcome parks: checkpoint to `state/<batch>.json`, comment on the
batch issue, label `needs-info`, stop. There is no automatic retry anywhere.

The one exception is a **Claude usage limit with a parseable reset time and a
captured session id** — that sleeps until the reset and resumes the same session
once. Without a session id there is nothing to resume, and re-running the stage
from scratch would be the auto-retry loop the rules forbid, so it parks instead.

A **Codex** limit always parks: its weekly bucket has no useful sleep window.
Re-run with `--fix-agent claude --resume` to fail the fix stage over to Claude —
the specs are harness-agnostic by design. That switch is deliberately manual;
silently spending Claude's budget on a stage assigned to Codex is not a decision
an unattended script should make for you.

Checkpoints carry the completed verify/fix/review artifacts and the exact
post-verify commit. `--resume` reuses those artifacts and resumes the failed
agent session when a session id was captured; it does not replay earlier stages.

Failure classification is deliberately narrow (`lib/checkpoint.mts`). Mistaking a
usage limit for an unknown error parks a batch that could have resumed — you lose
a night. Mistaking a real bug for a usage limit sends the rig into a sleep-and-retry
loop against a failure that will never clear, burning the quota it is waiting for.
So anything not clearly a limit is `unknown`, and `unknown` parks.
