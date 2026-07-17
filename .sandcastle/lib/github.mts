// GitHub side effects: labels are the state machine, comments are the log
// (README decision D5). All of it goes through `gh`, which infers the repo from
// the git remote.

import { execFile } from "node:child_process";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { promisify } from "node:util";
import { REPO_ROOT } from "../config.mts";

const exec = promisify(execFile);

/** The pipeline's label vocabulary — see docs/agents/triage-labels.md. */
export type PipelineLabel =
  | "needs-verification"
  | "ready-for-agent"
  | "in-progress"
  | "ready-for-human"
  | "needs-info";

const ALL_LABELS: PipelineLabel[] = [
  "needs-verification",
  "ready-for-agent",
  "in-progress",
  "ready-for-human",
  "needs-info",
];

const gh = async (args: string[]): Promise<string> => {
  const { stdout } = await exec("gh", args, {
    cwd: REPO_ROOT,
    maxBuffer: 16 * 1024 * 1024,
  });
  return stdout.trim();
};

/**
 * Moves an issue to exactly one pipeline state.
 *
 * Removes the other pipeline labels rather than only adding the new one: a
 * batch showing both `in-progress` and `ready-for-human` tells you nothing about
 * where it actually is, which defeats the point of labels-as-state-machine.
 * Non-pipeline labels are left alone.
 */
export const setState = async (
  issue: number,
  label: PipelineLabel,
): Promise<void> => {
  const current = JSON.parse(
    await gh(["issue", "view", String(issue), "--json", "labels"]),
  ) as { labels: { name: string }[] };
  const carried = new Set(current.labels.map((item) => item.name));
  const remove = ALL_LABELS.filter((l) => l !== label && carried.has(l));
  const args = ["issue", "edit", String(issue), "--add-label", label];
  for (const l of remove) args.push("--remove-label", l);
  // Do not suppress this error. Labels are the durable state machine; claiming
  // the transition succeeded when it did not is worse than parking the run.
  await exec("gh", args, { cwd: REPO_ROOT });
};

export const comment = async (issue: number, body: string): Promise<void> => {
  // Via a temp file, not `--body`: evidence-heavy comments blow past Windows'
  // ~32KB command-line limit, and `execFile` has no stdin-input option to pipe
  // through `--body-file -`.
  const dir = await mkdtemp(join(tmpdir(), "rw-pipeline-"));
  const file = join(dir, "comment.md");
  try {
    await writeFile(file, body, "utf8");
    await exec("gh", ["issue", "comment", String(issue), "--body-file", file], {
      cwd: REPO_ROOT,
    });
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
};

export const createPr = async (options: {
  readonly title: string;
  readonly body: string;
  readonly branch: string;
  readonly base: string;
}): Promise<string> => {
  // The branch only exists locally until now — the worktree commits to it, and
  // nothing has pushed it.
  await exec("git", ["push", "-u", "origin", options.branch], {
    cwd: REPO_ROOT,
  });

  const dir = await mkdtemp(join(tmpdir(), "rw-pipeline-"));
  const file = join(dir, "pr-body.md");
  try {
    await writeFile(file, options.body, "utf8");
    return await gh([
      "pr",
      "create",
      "--base",
      options.base,
      "--head",
      options.branch,
      "--title",
      options.title,
      "--body-file",
      file,
    ]);
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
};

/** Verifies `gh` is present and authenticated before a long unattended run. */
export const checkAuth = async (): Promise<void> => {
  try {
    await gh(["auth", "status"]);
  } catch (error) {
    throw new Error(
      `gh is not authenticated — the run would do all its work and then fail at ` +
        `the PR step. Run \`gh auth login\` first.\n${String(error)}`,
    );
  }
};
