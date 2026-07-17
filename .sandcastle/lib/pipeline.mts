// The stage chain: verify -> gate -> fix -> guards -> suites -> review -> PR.
//
// Everything an agent reports is treated as a claim. Verdicts are schema-checked,
// commit discipline is read out of git, and the suites are re-run by the
// orchestrator rather than taken on the fixer's word. The agents supply
// judgment; this file supplies the facts.

import {
  createSandbox,
  Output,
  run,
  StructuredOutputError,
  type RunResult,
} from "@ai-hero/sandcastle";
import { noSandboxPwsh } from "./no-sandbox-pwsh.mts";
import {
  BASE_BRANCH,
  branchFor,
  fixAgent,
  IDLE_TIMEOUT_SECONDS,
  REPO_ROOT,
  reviewAgent,
  verifyAgent,
  type Batch,
} from "../config.mts";
import {
  FIX_TAG,
  fixOutputSchema,
  REVIEW_TAG,
  reviewOutputSchema,
  VERIFY_TAG,
  verifyOutputSchema,
  confirmationDeficiency,
  type FixOutput,
  type ReviewOutput,
  type VerifyOutput,
  type VerifyVerdict,
} from "../schemas.mts";
import {
  classifyFailure,
  clearCheckpoint,
  parseResetTime,
  saveCheckpoint,
  type Checkpoint,
  type Stage,
} from "./checkpoint.mts";
import * as github from "./github.mts";
import { exactBugCoverageProblems, reviewContractProblems } from "./contracts.mts";
import {
  checkFailingTestConfirmations,
  checkFilesUnchanged,
  checkPerBugCommits,
  checkTestFilesAdditionsOnly,
  checkVerifyStageChanges,
  commitsOnBranch,
  resolveCommit,
  type Violation,
} from "./guards.mts";
import {
  loadPartial,
  loadSpecs,
  loadTemplate,
  render,
  renderSpecs,
} from "./prompts.mts";

export interface RunBatchOptions {
  readonly batch: Batch;
  readonly fixAgentKind: "codex" | "claude";
  /** Checkpoint to resume from, when the last run parked mid-chain. */
  readonly resume?: Checkpoint;
}

/** Thrown when the chain stops deliberately. Already checkpointed and reported. */
export class Parked extends Error {
  constructor(readonly stage: Stage, message: string) {
    super(message);
    this.name = "Parked";
  }
}

const log = (message: string): void => {
  console.log(`\n[${new Date().toISOString().slice(11, 19)}] ${message}`);
};

// ---------------------------------------------------------------------------
// Parking
// ---------------------------------------------------------------------------

/**
 * Stops the chain: checkpoint, comment on the batch issue, label `needs-info`,
 * throw. Per the hard rules, anything unresolved parks — there is no path here
 * that retries on its own.
 */
const park = async (options: {
  readonly batch: Batch;
  readonly stage: Stage;
  readonly branch: string;
  readonly reason: string;
  readonly detail: string;
  readonly error?: unknown;
  readonly sessionId?: string;
  readonly verdicts?: readonly VerifyVerdict[];
  readonly verifyHead?: string;
  readonly fixOutput?: FixOutput;
  readonly reviewOutput?: ReviewOutput;
  readonly retryOutput?: FixOutput;
}): Promise<never> => {
  const kind = options.error ? classifyFailure(options.error) : "unknown";

  const statePath = await saveCheckpoint({
    batch: options.batch.id,
    stage: options.stage,
    branch: options.branch,
    sessionId: options.sessionId,
    preservedWorktreePath:
      options.error instanceof StructuredOutputError
        ? options.error.preservedWorktreePath
        : undefined,
    reason: options.reason,
    kind,
    verdicts: options.verdicts,
    verifyHead: options.verifyHead,
    fixOutput: options.fixOutput,
    reviewOutput: options.reviewOutput,
    retryOutput: options.retryOutput,
  });

  await github
    .comment(
      options.batch.issue,
      [
        `**Pipeline parked at \`${options.stage}\`** — ${options.reason}`,
        "",
        options.detail,
        "",
        `- Branch: \`${options.branch}\``,
        `- Classification: \`${kind}\``,
        options.sessionId ? `- Session: \`${options.sessionId}\`` : undefined,
        `- Checkpoint: \`${statePath.replace(REPO_ROOT, "").replace(/^[\\/]/, "")}\``,
        "",
        "No automatic retry. Resume with `pnpm pipeline " +
          `${options.batch.id} --resume\`` +
          " once the cause is understood.",
      ]
        .filter(Boolean)
        .join("\n"),
    )
    .catch((e) => console.error(`(issue comment failed: ${String(e)})`));

  await github
    .setState(options.batch.issue, "needs-info")
    .catch((e) => console.error(`(issue state update failed: ${String(e)})`));

  log(`PARKED at ${options.stage}: ${options.reason}`);
  throw new Parked(options.stage, options.reason);
};

// ---------------------------------------------------------------------------
// Agent stages
// ---------------------------------------------------------------------------

/**
 * One agent stage. `maxIterations` is pinned to 1 — structured output requires
 * it, and it happens to be exactly the iteration cap the hard rules ask for:
 * one attempt per stage. The agent loops internally to get to green; what it
 * cannot do is get a second fresh attempt on its own.
 *
 * On a Claude usage limit with a known reset time, sleeps and resumes the same
 * session once. Every other failure parks.
 */
const runStage = async <T,>(options: {
  readonly batch: Batch;
  readonly stage: Stage;
  readonly branch: string;
  readonly agent: ReturnType<typeof verifyAgent>;
  readonly prompt: string;
  readonly tag: string;
  readonly schema: Parameters<typeof Output.object>[0]["schema"];
  readonly verdicts?: readonly VerifyVerdict[];
  readonly resumeSession?: string;
  readonly verifyHead?: string;
  readonly fixOutput?: FixOutput;
  readonly reviewOutput?: ReviewOutput;
  readonly retryOutput?: FixOutput;
}): Promise<RunResult & { output: T }> => {
  const invoke = (resumeSession?: string) =>
    run({
      // Also becomes part of the worktree directory name (patched into
      // create()'s naming — see patches/@ai-hero__sandcastle@0.12.0.patch),
      // so each stage gets its own worktree dir under the same branch. That
      // keeps one stage's undeletable-on-Windows leftover (a locked
      // node_modules, say) from blocking the next stage's `git worktree add`
      // at the same path. The branch name already carries the batch id, so
      // repeating it here would just be noise in log filenames too.
      name: options.stage,
      agent: options.agent,
      sandbox: noSandboxPwsh(),
      cwd: REPO_ROOT,
      prompt: options.prompt,
      maxIterations: 1,
      idleTimeoutSeconds: IDLE_TIMEOUT_SECONDS,
      branchStrategy: {
        type: "branch",
        branch: options.branch,
        baseBranch: BASE_BRANCH,
      },
      resumeSession,
      // maxRetries lets the agent re-emit malformed JSON without redoing the
      // work — a bad tag is a formatting slip, not a reason to burn the stage.
      output: Output.object({
        tag: options.tag,
        schema: options.schema,
        maxRetries: 1,
      }),
    }) as Promise<RunResult & { output: T }>;

  try {
    return await invoke(options.resumeSession);
  } catch (error) {
    const kind = classifyFailure(error);
    const sessionId =
      error instanceof StructuredOutputError ? error.sessionId : undefined;

    if (kind === "claude-usage-limit") {
      const resetAt = parseResetTime(error);
      // Resume needs a session id to continue; without one the only "retry"
      // available is redoing the stage from scratch, which is the auto-retry
      // loop the hard rules forbid. Park instead.
      if (resetAt && sessionId) {
        const waitMs = resetAt.getTime() - Date.now() + 60_000;
        if (waitMs > 0 && waitMs < 6 * 60 * 60 * 1000) {
          log(
            `Claude usage limit. Sleeping ${Math.round(waitMs / 60_000)}m until ${resetAt.toISOString()}, then resuming session ${sessionId}.`,
          );
          await saveCheckpoint({
            batch: options.batch.id,
            stage: options.stage,
            branch: options.branch,
            sessionId,
            reason: `Claude usage limit; sleeping until ${resetAt.toISOString()}`,
            kind,
            verdicts: options.verdicts,
            verifyHead: options.verifyHead,
            fixOutput: options.fixOutput,
            reviewOutput: options.reviewOutput,
            retryOutput: options.retryOutput,
          });
          await new Promise((resolve) => setTimeout(resolve, waitMs));
          return await invoke(sessionId);
        }
      }
    }

    return park({
      batch: options.batch,
      stage: options.stage,
      branch: options.branch,
      reason:
        kind === "unknown"
          ? `${options.stage} stage threw`
          : `${options.stage} stage hit a ${kind}`,
      detail: [
        "```",
        error instanceof Error ? error.message : String(error),
        "```",
        error instanceof StructuredOutputError && error.rawMatched
          ? `\nLast \`<${options.tag}>\` payload:\n\`\`\`\n${error.rawMatched.slice(0, 2000)}\n\`\`\``
          : "",
      ].join("\n"),
      error,
      sessionId,
      verdicts: options.verdicts,
      verifyHead: options.verifyHead,
      fixOutput: options.fixOutput,
      reviewOutput: options.reviewOutput,
      retryOutput: options.retryOutput,
    });
  }
};

// ---------------------------------------------------------------------------
// Suites — re-run by the orchestrator, not taken on trust
// ---------------------------------------------------------------------------

interface SuiteResult {
  readonly command: string;
  readonly exitCode: number;
  readonly tail: string;
}

/**
 * Re-runs the batch's test and lint commands against the branch.
 *
 * The fix stage reports `suitesGreen` itself, but "the tests pass" is the whole
 * definition of done — grading that on the agent's own say-so would make the
 * gate decorative.
 */
const runSuites = async (
  batch: Batch,
  branch: string,
  commands: readonly string[] = [...batch.testCommands, ...batch.lintCommands],
): Promise<SuiteResult[]> => {
  const sandbox = await createSandbox({
    branch,
    baseBranch: BASE_BRANCH,
    sandbox: noSandboxPwsh(),
    cwd: REPO_ROOT,
  });

  const results: SuiteResult[] = [];
  try {
    for (const command of commands) {
      log(`suite: ${command}`);
      const lines: string[] = [];
      const result = await sandbox.exec(command, {
        onLine: (line) => {
          lines.push(line);
          if (lines.length > 400) lines.shift();
        },
      });
      results.push({
        command,
        exitCode: result.exitCode,
        tail: lines.slice(-40).join("\n"),
      });
      log(`  -> exit ${result.exitCode}`);
    }
  } finally {
    await sandbox.close();
  }
  return results;
};

// ---------------------------------------------------------------------------
// Formatting
// ---------------------------------------------------------------------------

const formatVerdicts = (verdicts: readonly VerifyVerdict[]): string =>
  verdicts
    .map((v) => {
      if (v.verdict === "dismissed") {
        return `- **${v.bug}: dismissed** — ${v.reason}`;
      }
      const proof = v.testFile
        ? `\n  - failing test: \`${v.testName}\` in \`${v.testFile}\``
        : v.evidence?.length
          ? `\n  - evidence: ${v.evidence.map((e) => `\`${e.location}\``).join(", ")}` +
            (v.sequence ? `\n  - sequence: ${v.sequence}` : "")
          : "";
      return `- **${v.bug}: confirmed** — ${v.reason}${proof}`;
    })
    .join("\n");

const formatViolations = (violations: readonly Violation[]): string =>
  violations.map((v) => `- \`${v.rule}\`: ${v.detail}`).join("\n");

const formatFindings = (review: ReviewOutput): string =>
  review.findings
    .map((f) => `- **${f.bug}** (\`${f.kind}\`): ${f.detail}`)
    .join("\n");

// ---------------------------------------------------------------------------
// The chain
// ---------------------------------------------------------------------------

interface RunProgress {
  stage: Stage;
  verdicts?: readonly VerifyVerdict[];
  verifyHead?: string;
  fixOutput?: FixOutput;
  reviewOutput?: ReviewOutput;
  retryOutput?: FixOutput;
}

const runBatchImpl = async (
  options: RunBatchOptions,
  progress: RunProgress,
): Promise<void> => {
  const { batch } = options;
  const branch = branchFor(batch);

  await github.checkAuth();

  const specs = await loadSpecs(batch);
  log(`Batch ${batch.id}: ${batch.bugs.join(", ")} -> ${branch}`);

  await github.setState(batch.issue, "in-progress");

  // -------------------------------------------------------------------------
  // Stage 1 — Verify
  // -------------------------------------------------------------------------

  let confirmed: VerifyVerdict[];
  let verifyOutput: VerifyOutput;

  if (options.resume?.verdicts) {
    // Verdicts were already earned before the park; re-verifying would rewrite
    // history the fix stage is already sitting on, and on a failing-test batch
    // would collide with the tests already committed.
    verifyOutput = { verdicts: [...options.resume.verdicts] };
    confirmed = verifyOutput.verdicts.filter((v) => v.verdict === "confirmed");
    log(`Resuming past verify — confirmed: ${confirmed.map((v) => v.bug).join(", ")}`);
  } else {
    progress.stage = "verify";
    log("Stage 1: verify");
    const verify = await runStage<VerifyOutput>({
      batch,
      stage: "verify",
      branch,
      agent: verifyAgent(),
      tag: VERIFY_TAG,
      schema: verifyOutputSchema,
      resumeSession:
        options.resume?.stage === "verify" ? options.resume.sessionId : undefined,
      prompt: render(await loadTemplate("verify"), {
        BATCH: batch.id,
        BRANCH: branch,
        BUGS: batch.bugs.join(", "),
        CONFIRMATION: batch.confirmation,
        CONFIRM_BLOCK: await loadPartial("verify", batch.confirmation),
        SCOPE: batch.scope,
        TEST_COMMANDS: batch.testCommands.join("\n"),
        SPECS: renderSpecs(specs),
        TAG: VERIFY_TAG,
      }),
    });

    verifyOutput = verify.output;

    confirmed = verifyOutput.verdicts.filter((v) => v.verdict === "confirmed");
  }

  const coverageProblems = exactBugCoverageProblems(
    batch.bugs,
    verifyOutput.verdicts.map((verdict) => verdict.bug),
  );
  if (coverageProblems.length > 0) {
    await park({
      batch,
      stage: "verify",
      branch,
      reason: "verify output did not contain exactly one verdict per batch bug",
      detail: coverageProblems.map((problem) => `- ${problem}`).join("\n"),
      verdicts: verifyOutput.verdicts,
    });
  }

  const deficient = verifyOutput.verdicts
    .map((v) => ({ v, why: confirmationDeficiency(v, batch.confirmation) }))
    .filter((x): x is { v: VerifyVerdict; why: string } => x.why !== undefined);

  if (deficient.length > 0) {
    await park({
      batch,
      stage: "verify",
      branch,
      reason: `a bug was confirmed without the proof this batch requires (${batch.confirmation})`,
      detail: deficient.map((x) => `- **${x.v.bug}**: ${x.why}`).join("\n"),
      verdicts: verifyOutput.verdicts,
    });
  }

  progress.verdicts = verifyOutput.verdicts;

  await github.comment(
    batch.issue,
    `### Verify\n\n${formatVerdicts(verifyOutput.verdicts)}`,
  );

  // -------------------------------------------------------------------------
  // Gate
  // -------------------------------------------------------------------------

  if (confirmed.length === 0) {
    log("No bugs confirmed — nothing to fix.");
    await github.comment(
      batch.issue,
      "### Result\n\nEvery bug in this batch was dismissed. No fix stage ran, no PR opened. " +
        "Record the dispositions in `batches.md` and close this issue if you agree with the reasoning.",
    );
    await github.setState(batch.issue, "ready-for-human");
    await clearCheckpoint(batch.id);
    return;
  }

  const confirmedBugs = confirmed.map((v) => v.bug);
  log(`Confirmed: ${confirmedBugs.join(", ")}`);

  // The verify stage commits failing tests, so the additions-only rule is
  // already live. Catching a violation here rather than after the fix stage
  // saves a whole Codex pass on a batch that can't be accepted anyway.
  const verifyViolations = await checkTestFilesAdditionsOnly(BASE_BRANCH, branch);
  if (!options.resume?.verifyHead) {
    verifyViolations.push(
      ...(await checkVerifyStageChanges(
        BASE_BRANCH,
        branch,
        batch.confirmation === "failing-test",
      )),
    );
  }
  if (verifyViolations.length > 0) {
    await park({
      batch,
      stage: "verify",
      branch,
      reason: "verify modified pre-existing test files",
      detail: formatViolations(verifyViolations),
      verdicts: confirmed,
    });
  }

  let verifyHead = options.resume?.verifyHead;
  if (!verifyHead) {
    verifyHead =
      batch.confirmation === "failing-test"
        ? await resolveCommit(branch)
        : await resolveCommit(BASE_BRANCH);
  }

  if (batch.confirmation === "failing-test") {
    const confirmationViolations = await checkFailingTestConfirmations(
      BASE_BRANCH,
      branch,
      confirmed,
    );
    if (!options.resume?.verifyHead) {
      confirmationViolations.push(
        ...(await checkPerBugCommits(BASE_BRANCH, branch, confirmedBugs)),
      );
    }
    if (confirmationViolations.length > 0) {
      await park({
        batch,
        stage: "verify",
        branch,
        reason: "verify did not commit the named failing tests",
        detail: formatViolations(confirmationViolations),
        verdicts: confirmed,
      });
    }

    if (!options.resume?.verifyHead) {
      const verificationSuites = await runSuites(batch, branch, batch.testCommands);
      if (verificationSuites.every((suite) => suite.exitCode === 0)) {
        await park({
          batch,
          stage: "verify",
          branch,
          reason: "verification tests are green before the fix stage",
          detail: "A failing-test confirmation must make at least one configured test command fail.",
          verdicts: confirmed,
        });
      }
    }
  }
  progress.verifyHead = verifyHead;

  const confirmedSpecs = specs.filter((s) => confirmedBugs.includes(s.bug));

  // -------------------------------------------------------------------------
  // Stage 2 — Fix
  // -------------------------------------------------------------------------

  progress.stage = "fix";
  log(`Stage 2: fix (${options.fixAgentKind})`);
  const fix = options.resume?.fixOutput
    ? { output: options.resume.fixOutput }
    : await runStage<FixOutput>({
    batch,
    stage: "fix",
    branch,
    agent: fixAgent(options.fixAgentKind),
    tag: FIX_TAG,
    schema: fixOutputSchema,
    verdicts: confirmed,
    resumeSession:
      options.resume?.stage === "fix" ? options.resume.sessionId : undefined,
    verifyHead,
    prompt: render(await loadTemplate("fix"), {
      BATCH: batch.id,
      BRANCH: branch,
      BUGS: confirmedBugs.join(", "),
      CONFIRMATION: batch.confirmation,
      FIX_BASIS_BLOCK: await loadPartial("fix", batch.confirmation),
      SCOPE: batch.scope,
      TEST_COMMANDS: batch.testCommands.join("\n"),
      LINT_COMMANDS: batch.lintCommands.join("\n") || "(no lint step for this batch)",
      SPECS: renderSpecs(confirmedSpecs, confirmed),
      TAG: FIX_TAG,
    }),
      });
  progress.fixOutput = fix.output;

  const fixCoverageProblems = exactBugCoverageProblems(
    confirmedBugs,
    fix.output.results.map((result) => result.bug),
  );
  if (fixCoverageProblems.length > 0) {
    await park({
      batch,
      stage: "fix",
      branch,
      reason: "fix output did not contain exactly one result per confirmed bug",
      detail: fixCoverageProblems.map((problem) => `- ${problem}`).join("\n"),
      verdicts: confirmed,
      verifyHead,
      fixOutput: fix.output,
    });
  }

  const fixedBugs = fix.output.results
    .filter((r) => r.status === "fixed")
    .map((r) => r.bug);

  await github.comment(
    batch.issue,
    `### Fix (${options.fixAgentKind})\n\n` +
      fix.output.results
        .map((r) =>
          r.status === "fixed"
            ? `- **${r.bug}: fixed** — ${r.summary}`
            : `- **${r.bug}: failed** — ${r.summary}\n  - blocker: ${r.blocker ?? "(none given)"}`,
        )
        .join("\n"),
  );

  const failedResults = fix.output.results.filter((result) => result.status === "failed");
  if (failedResults.length > 0) {
    await park({
      batch,
      stage: "fix",
      branch,
      reason: "one or more confirmed bugs remain unresolved after the fix stage",
      detail: failedResults
        .map((result) => `- **${result.bug}**: ${result.blocker ?? result.summary}`)
        .join("\n"),
      verdicts: confirmed,
      verifyHead,
      fixOutput: fix.output,
    });
  }

  // -------------------------------------------------------------------------
  // Guards + suites
  // -------------------------------------------------------------------------

  await gateOnEvidence({
    batch,
    branch,
    stage: "fix",
    fixedBugs,
    verdicts: confirmed,
    verifyHead,
    fixOutput: fix.output,
  });

  // -------------------------------------------------------------------------
  // Stage 3 — Review
  // -------------------------------------------------------------------------

  progress.stage = "review";
  log("Stage 3: review");
  const review = options.resume?.reviewOutput
    ? { output: options.resume.reviewOutput }
    : await runStage<ReviewOutput>({
    batch,
    stage: "review",
    branch,
    agent: reviewAgent(),
    tag: REVIEW_TAG,
    schema: reviewOutputSchema,
    verdicts: confirmed,
    resumeSession:
      options.resume?.stage === "review" ? options.resume.sessionId : undefined,
    verifyHead,
    fixOutput: fix.output,
    prompt: render(await loadTemplate("review"), {
      BATCH: batch.id,
      BRANCH: branch,
      BASE_BRANCH,
      BUGS: fixedBugs.join(", "),
      CONFIRMATION: batch.confirmation,
      REVIEW_FOCUS_BLOCK: await loadPartial("review", batch.confirmation),
      SCOPE: batch.scope,
      SPECS: renderSpecs(
        specs.filter((s) => fixedBugs.includes(s.bug)),
        confirmed,
      ),
      TAG: REVIEW_TAG,
    }),
      });
  progress.reviewOutput = review.output;

  const reviewProblems = reviewContractProblems(fixedBugs, review.output);
  if (reviewProblems.length > 0) {
    await park({
      batch,
      stage: "review",
      branch,
      reason: "review output violated its stage contract",
      detail: reviewProblems.map((problem) => `- ${problem}`).join("\n"),
      verdicts: confirmed,
      verifyHead,
      fixOutput: fix.output,
    });
  }

  await github.comment(
    batch.issue,
    `### Review\n\n**${review.output.verdict}** — ${review.output.summary}` +
      (review.output.findings.length > 0 ? `\n\n${formatFindings(review.output)}` : ""),
  );

  if (review.output.verdict === "clean" && review.output.findings.length === 0) {
    progress.stage = "pr";
    await openPr({ batch, branch, fixedBugs, verifyOutput, fix: fix.output, review: review.output });
    await clearCheckpoint(batch.id);
    return;
  }

  // -------------------------------------------------------------------------
  // Stage 2b — one bounded retry, then stop regardless of outcome
  // -------------------------------------------------------------------------

  progress.stage = "fix-retry";
  log("Stage 2b: fix retry (bounded, single attempt)");
  const retry = options.resume?.retryOutput
    ? { output: options.resume.retryOutput }
    : await runStage<FixOutput>({
    batch,
    stage: "fix-retry",
    branch,
    agent: fixAgent(options.fixAgentKind),
    tag: FIX_TAG,
    schema: fixOutputSchema,
    verdicts: confirmed,
    resumeSession:
      options.resume?.stage === "fix-retry" ? options.resume.sessionId : undefined,
    verifyHead,
    fixOutput: fix.output,
    reviewOutput: review.output,
    prompt: render(await loadTemplate("fix-retry"), {
      BATCH: batch.id,
      BRANCH: branch,
      CONFIRMATION: batch.confirmation,
      SCOPE: batch.scope,
      TEST_COMMANDS: batch.testCommands.join("\n"),
      LINT_COMMANDS: batch.lintCommands.join("\n") || "(no lint step for this batch)",
      FINDINGS: formatFindings(review.output),
      SPECS: renderSpecs(
        specs.filter((s) => fixedBugs.includes(s.bug)),
        confirmed,
      ),
      TAG: FIX_TAG,
    }),
      });
  progress.retryOutput = retry.output;

  await github.comment(
    batch.issue,
    `### Fix retry\n\n` +
      retry.output.results
        .map(
          (r) =>
            `- **${r.bug}: ${r.status}** — ${r.summary}` +
            (r.blocker ? `\n  - blocker: ${r.blocker}` : ""),
        )
        .join("\n"),
  );

  // The retry is the end of the automated road: no second review, no PR. The
  // branch is pushed so a human can read the diff on GitHub.
  const suites = await runSuites(batch, branch);
  const red = suites.filter((s) => s.exitCode !== 0);

  await pushBranch(branch);

  await github.comment(
    batch.issue,
    [
      "### Stopped after the bounded retry",
      "",
      "The review found problems and the fixer had its one retry. Nothing further runs automatically, and no PR was opened — a human decides from here.",
      "",
      `- Branch: \`${branch}\` (pushed)`,
      `- Suites after retry: ${red.length === 0 ? "green" : `**${red.length} red** — ${red.map((r) => `\`${r.command}\``).join(", ")}`}`,
      "",
      `Open a PR yourself with: \`gh pr create --base ${BASE_BRANCH} --head ${branch}\``,
    ].join("\n"),
  );

  await github.setState(batch.issue, "ready-for-human");
  await clearCheckpoint(batch.id);
  log(`Batch ${batch.id} stopped after retry — flagged for human review.`);
};

/**
 * Last-resort durability boundary. Explicit gates park themselves with richer
 * detail; every other throw is still checkpointed before it reaches main.
 */
export const runBatch = async (options: RunBatchOptions): Promise<void> => {
  const progress: RunProgress = {
    stage: options.resume?.stage ?? "verify",
    verdicts: options.resume?.verdicts,
    verifyHead: options.resume?.verifyHead,
    fixOutput: options.resume?.fixOutput,
    reviewOutput: options.resume?.reviewOutput,
    retryOutput: options.resume?.retryOutput,
  };

  try {
    await runBatchImpl(options, progress);
  } catch (error) {
    if (error instanceof Parked) throw error;
    await park({
      batch: options.batch,
      stage: progress.stage,
      branch: branchFor(options.batch),
      reason: `unexpected ${progress.stage} pipeline failure`,
      detail: `\`\`\`\n${error instanceof Error ? error.message : String(error)}\n\`\`\``,
      error,
      verdicts: progress.verdicts,
      verifyHead: progress.verifyHead,
      fixOutput: progress.fixOutput,
      reviewOutput: progress.reviewOutput,
      retryOutput: progress.retryOutput,
    });
  }
};

// ---------------------------------------------------------------------------
// Evidence gate — git facts + suite reality
// ---------------------------------------------------------------------------

const gateOnEvidence = async (options: {
  readonly batch: Batch;
  readonly branch: string;
  readonly stage: Stage;
  readonly fixedBugs: readonly string[];
  readonly verdicts: readonly VerifyVerdict[];
  readonly verifyHead: string;
  readonly fixOutput: FixOutput;
}): Promise<void> => {
  const { batch, branch } = options;

  const violations = [
    ...(await checkTestFilesAdditionsOnly(BASE_BRANCH, branch)),
    ...(await checkFilesUnchanged(
      options.verifyHead,
      branch,
      options.verdicts.flatMap((verdict) => verdict.testFile ? [verdict.testFile] : []),
    )),
    ...(await checkPerBugCommits(options.verifyHead, branch, options.fixedBugs)),
  ];

  if (violations.length > 0) {
    await park({
      batch,
      stage: options.stage,
      branch,
      reason: "guard violation",
      detail:
        `${formatViolations(violations)}\n\nCommits on \`${branch}\`:\n` +
        (await commitsOnBranch(BASE_BRANCH, branch))
          .map((c) => `- \`${c.sha.slice(0, 8)}\` ${c.subject}`)
          .join("\n"),
      verdicts: options.verdicts,
      verifyHead: options.verifyHead,
      fixOutput: options.fixOutput,
    });
  }

  const suites = await runSuites(batch, branch);
  const red = suites.filter((s) => s.exitCode !== 0);
  if (red.length > 0) {
    await park({
      batch,
      stage: options.stage,
      branch,
      reason: "the suites are red after the fix stage",
      detail: red
        .map(
          (r) =>
            `**\`${r.command}\`** exited ${r.exitCode}\n\n\`\`\`\n${r.tail}\n\`\`\``,
        )
        .join("\n\n"),
      verdicts: options.verdicts,
      verifyHead: options.verifyHead,
      fixOutput: options.fixOutput,
    });
  }
  if (!options.fixOutput.suitesGreen) {
    await park({
      batch,
      stage: options.stage,
      branch,
      reason: "fixer reported suites red although the orchestrator found them green",
      detail: "The fix output is internally unresolved (`suitesGreen: false`), so the batch cannot advance to review.",
      verdicts: options.verdicts,
      verifyHead: options.verifyHead,
      fixOutput: options.fixOutput,
    });
  }
};

// ---------------------------------------------------------------------------
// PR
// ---------------------------------------------------------------------------

const pushBranch = async (branch: string): Promise<void> => {
  const { execFile } = await import("node:child_process");
  const { promisify } = await import("node:util");
  await promisify(execFile)("git", ["push", "-u", "origin", branch], {
    cwd: REPO_ROOT,
  });
};

const openPr = async (options: {
  readonly batch: Batch;
  readonly branch: string;
  readonly fixedBugs: readonly string[];
  readonly verifyOutput: VerifyOutput;
  readonly fix: FixOutput;
  readonly review: ReviewOutput;
}): Promise<void> => {
  const { batch, branch } = options;

  const body = [
    `Automated bugfix batch **${batch.id}** — closes nothing automatically; nothing merges without a human.`,
    "",
    `Tracking issue: #${batch.issue}`,
    "",
    "## Verify",
    "",
    formatVerdicts(options.verifyOutput.verdicts),
    "",
    "## Fix",
    "",
    options.fix.results
      .map((r) => `- **${r.bug}: ${r.status}** — ${r.summary}`)
      .join("\n"),
    "",
    "## Review",
    "",
    `Clean. ${options.review.summary}`,
    "",
    "## What the orchestrator checked itself",
    "",
    "- Every fixed bug has its own commit, tagged with the bug id.",
    "- No pre-existing test file was edited, renamed, or deleted (additions-only).",
    "- The batch's test and lint commands were re-run against this branch and exited 0 — this is not the fixer's self-report.",
    "",
    `Scope boundary: ${batch.scope}`,
    "",
    "🤖 Generated with [Claude Code](https://claude.com/claude-code)",
  ].join("\n");

  const url = await github.createPr({
    title: `Bugfix batch: ${batch.id} (${options.fixedBugs.join(", ")})`,
    body,
    branch,
    base: BASE_BRANCH,
  });

  await github.comment(batch.issue, `### PR opened\n\n${url}`);
  await github.setState(batch.issue, "ready-for-human");
  log(`PR opened: ${url}`);
};
