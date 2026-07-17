// Mechanical guards. These check what the agents *did*, not what they said they
// did — every input here is git history, not agent-reported JSON.

import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { REPO_ROOT } from "../config.mts";
import type { VerifyVerdict } from "../schemas.mts";

const exec = promisify(execFile);

const git = async (args: string[], cwd: string = REPO_ROOT): Promise<string> => {
  const { stdout } = await exec("git", args, { cwd, maxBuffer: 32 * 1024 * 1024 });
  return stdout;
};

export interface Violation {
  readonly rule: string;
  readonly detail: string;
}

/**
 * Test files, by convention across the three toolchains in this repo.
 * Deliberately broad: a false positive here costs a manual look at a flagged
 * batch, a false negative lets a gamed test through to a human who is trusting
 * this check to have run.
 */
const isTestFile = (path: string): boolean =>
  /(^|\/)src\/(test|androidTest)\//.test(path) ||
  /\.(test|spec)\.[cm]?[jt]sx?$/.test(path) ||
  /(^|\/)__tests__\//.test(path) ||
  /Test\.kt$/.test(path) ||
  /(^|\/)supabase\/tests\//.test(path);

/**
 * Existing test files are additions-only.
 *
 * The fix stage is graded by "the tests pass", so the cheapest way to pass is to
 * edit the test. This makes that mechanically impossible rather than merely
 * forbidden in the prompt. New test files are unrestricted; a pre-existing one
 * may only grow.
 *
 * Renames and deletions of pre-existing test files are violations too — both are
 * ways to make an inconvenient assertion stop running.
 */
export const checkTestFilesAdditionsOnly = async (
  base: string,
  branch: string,
  cwd: string = REPO_ROOT,
): Promise<Violation[]> => {
  // A static-evidence verify stage produces no commits, so its branch may not
  // exist on the host yet. No branch means no diff means no violations — not an
  // error. (Sandcastle's `branch` strategy only creates the ref once something
  // is committed to it.)
  if (!(await branchExists(branch, cwd))) return [];

  const violations: Violation[] = [];

  // --find-renames so a rename reports as R rather than as an add/delete pair.
  const nameStatus = await git(
    ["diff", "--name-status", "--find-renames", `${base}...${branch}`],
    cwd,
  );

  for (const line of nameStatus.trim().split("\n").filter(Boolean)) {
    const [status, ...paths] = line.split("\t");
    const from = paths[0]!;
    const to = paths[1] ?? from;
    const code = status![0]!;

    if (code === "A") continue; // new test files are fine

    if (!isTestFile(from) && !isTestFile(to)) continue;

    if (code === "D") {
      violations.push({
        rule: "test-files-additions-only",
        detail: `deleted pre-existing test file ${from}`,
      });
    } else if (code === "R") {
      violations.push({
        rule: "test-files-additions-only",
        detail: `renamed pre-existing test file ${from} -> ${to}`,
      });
    }
  }

  // For modified test files, "additions-only" means zero deleted lines.
  const numstat = await git(
    ["diff", "--numstat", "--find-renames", `${base}...${branch}`],
    cwd,
  );

  for (const line of numstat.trim().split("\n").filter(Boolean)) {
    const [added, deleted, path] = line.split("\t");
    if (!path || !isTestFile(path)) continue;
    // Binary files report "-". Nothing to reason about; skip.
    if (deleted === "-") continue;
    if (Number(deleted) > 0) {
      violations.push({
        rule: "test-files-additions-only",
        detail: `modified pre-existing test file ${path}: ${deleted} line(s) removed (${added} added). Existing tests may be appended to, not edited.`,
      });
    }
  }

  return violations;
};

/**
 * One commit per bug, tagged with the bug id.
 *
 * The point is recoverability: an unattended run that dies mid-batch should lose
 * at most one bug's work, which only holds if each bug's changes are already a
 * self-contained commit. Checked against the commit subjects on the branch.
 */
export const checkPerBugCommits = async (
  base: string,
  branch: string,
  expectedBugs: readonly string[],
  cwd: string = REPO_ROOT,
): Promise<Violation[]> => {
  if (expectedBugs.length === 0) return [];

  // No branch means no commits — every expected bug is missing one. Reported as
  // a clean violation list rather than a thrown "unknown revision".
  if (!(await branchExists(branch, cwd))) {
    return expectedBugs.map((bug) => ({
      rule: "per-bug-commit",
      detail: `no commit on ${branch} for ${bug} (the branch has no commits at all)`,
    }));
  }

  const log = await git(["log", "--format=%s", `${base}..${branch}`], cwd);
  const subjects = log.trim().split("\n").filter(Boolean);

  const violations: Violation[] = [];

  for (const bug of expectedBugs) {
    const matching = subjects.filter((s) =>
      new RegExp(`^${bug}\\b`, "i").test(s.trim()),
    );
    if (matching.length !== 1) {
      violations.push({
        rule: "per-bug-commit",
        detail:
          matching.length === 0
            ? `no commit on ${branch} whose subject starts with "${bug}"`
            : `${matching.length} commits on ${branch} start with "${bug}"; exactly one fix commit is required`,
      });
    }
  }

  for (const subject of subjects) {
    if (!expectedBugs.some((bug) => new RegExp(`^${bug}\\b`, "i").test(subject.trim()))) {
      violations.push({
        rule: "per-bug-commit",
        detail: `fix-stage commit is not assigned to one expected bug: "${subject}"`,
      });
    }
  }

  // A commit mentioning two bug ids up front is a bundled commit.
  for (const subject of subjects) {
    const mentioned = expectedBugs.filter((b) =>
      new RegExp(`\\b${b}\\b`, "i").test(subject),
    );
    if (mentioned.length > 1) {
      violations.push({
        rule: "per-bug-commit",
        detail: `commit bundles ${mentioned.join(" + ")}: "${subject}"`,
      });
    }
  }

  return violations;
};

/** Resolve a branch or commit to the exact commit used as a stage boundary. */
export const resolveCommit = async (
  ref: string,
  cwd: string = REPO_ROOT,
): Promise<string> => (await git(["rev-parse", "--verify", `${ref}^{commit}`], cwd)).trim();

/**
 * A failing-test confirmation is an observable git fact, not just two strings
 * in agent JSON: the named file must exist and be touched by that bug's verify
 * commit between base and branch.
 */
export const checkFailingTestConfirmations = async (
  base: string,
  branch: string,
  verdicts: readonly VerifyVerdict[],
  cwd: string = REPO_ROOT,
): Promise<Violation[]> => {
  const violations: Violation[] = [];
  if (!(await branchExists(branch, cwd))) {
    return verdicts.map((verdict) => ({
      rule: "committed-failing-test",
      detail: `${verdict.bug} named a failing test, but ${branch} has no commits`,
    }));
  }

  for (const verdict of verdicts) {
    if (!verdict.testFile) continue;
    try {
      await git(["cat-file", "-e", `${branch}:${verdict.testFile}`], cwd);
    } catch {
      violations.push({
        rule: "committed-failing-test",
        detail: `${verdict.bug} named ${verdict.testFile}, but that file does not exist on ${branch}`,
      });
      continue;
    }

    const subjects = (
      await git(["log", "--format=%s", `${base}..${branch}`, "--", verdict.testFile], cwd)
    )
      .trim()
      .split("\n")
      .filter(Boolean);
    if (!subjects.some((subject) => new RegExp(`^${verdict.bug}\\b`, "i").test(subject.trim()))) {
      violations.push({
        rule: "committed-failing-test",
        detail: `${verdict.bug} has no verify commit starting with its bug id that touches ${verdict.testFile}`,
      });
    }
  }
  return violations;
};

/** Verify is evidence-only: failing-test batches may change tests, nothing else. */
export const checkVerifyStageChanges = async (
  base: string,
  branch: string,
  allowTests: boolean,
  cwd: string = REPO_ROOT,
): Promise<Violation[]> => {
  if (!(await branchExists(branch, cwd))) return [];
  const changed = (
    await git(["diff", "--name-only", `${base}..${branch}`], cwd)
  )
    .trim()
    .split("\n")
    .filter(Boolean);

  return changed
    .filter((path) => !allowTests || !isTestFile(path))
    .map((path) => ({
      rule: "verify-evidence-only",
      detail: allowTests
        ? `verify changed production/non-test file ${path}`
        : `static-evidence verify created a change at ${path}`,
    }));
};

/** Stage-one failing tests are immutable during the fix stage. */
export const checkFilesUnchanged = async (
  baseline: string,
  branch: string,
  files: readonly string[],
  cwd: string = REPO_ROOT,
): Promise<Violation[]> => {
  const unique = [...new Set(files.filter(Boolean))];
  if (unique.length === 0 || !(await branchExists(branch, cwd))) return [];
  const changed = (
    await git(["diff", "--name-only", `${baseline}..${branch}`, "--", ...unique], cwd)
  )
    .trim()
    .split("\n")
    .filter(Boolean);
  return changed.map((path) => ({
    rule: "verify-tests-immutable",
    detail: `fix stage changed the stage-one failing test ${path}`,
  }));
};

/** Commits on the branch that aren't on base. */
export const commitsOnBranch = async (
  base: string,
  branch: string,
  cwd: string = REPO_ROOT,
): Promise<{ sha: string; subject: string }[]> => {
  const log = await git(["log", "--format=%H%x09%s", `${base}..${branch}`], cwd);
  return log
    .trim()
    .split("\n")
    .filter(Boolean)
    .map((line) => {
      const [sha, subject] = line.split("\t");
      return { sha: sha!, subject: subject ?? "" };
    });
};

export const branchExists = async (
  branch: string,
  cwd: string = REPO_ROOT,
): Promise<boolean> => {
  try {
    await git(["rev-parse", "--verify", `refs/heads/${branch}`], cwd);
    return true;
  } catch {
    return false;
  }
};
