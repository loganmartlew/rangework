// Checkpoint / resume state.
//
// Written on every throw, so an unattended run that dies at 3am leaves enough
// behind to pick up rather than restart. State lives on disk next to the
// worktrees; the GitHub issue is the human-facing state machine (README D5).

import { mkdir, readFile, writeFile, rm } from "node:fs/promises";
import { join } from "node:path";
import { STATE_DIR } from "../config.mts";
import type { FixOutput, ReviewOutput, VerifyVerdict } from "../schemas.mts";

export type Stage = "verify" | "fix" | "review" | "fix-retry" | "pr";

export interface Checkpoint {
  readonly batch: string;
  readonly stage: Stage;
  readonly branch: string;
  /** Agent session to resume, when the failed stage captured one. */
  readonly sessionId?: string;
  /** Host path to the worktree Sandcastle preserved on the failure path. */
  readonly preservedWorktreePath?: string;
  /** Why we parked. Copied into the issue comment. */
  readonly reason: string;
  /** Loose classification — drives whether a resume is even offered. */
  readonly kind: FailureKind;
  /**
   * The verify stage's verdicts in full, so a resume past verify doesn't
   * re-verify — and, on a static-evidence batch, doesn't lose the quoted proof.
   * The evidence there isn't recoverable from the branch the way a committed
   * failing test is; if it isn't carried here, it's gone.
   */
  readonly verdicts?: readonly VerifyVerdict[];
  /** Exact commit after verification; fix commits and test immutability are measured from here. */
  readonly verifyHead?: string;
  /** Completed stage artifacts let resume continue at the failed gate/stage without replaying agents. */
  readonly fixOutput?: FixOutput;
  readonly reviewOutput?: ReviewOutput;
  readonly retryOutput?: FixOutput;
  readonly updatedAt: string;
}

const pathFor = (batch: string): string => join(STATE_DIR, `${batch}.json`);

export const saveCheckpoint = async (
  checkpoint: Omit<Checkpoint, "updatedAt">,
): Promise<string> => {
  await mkdir(STATE_DIR, { recursive: true });
  const full: Checkpoint = {
    ...checkpoint,
    updatedAt: new Date().toISOString(),
  };
  const path = pathFor(checkpoint.batch);
  await writeFile(path, `${JSON.stringify(full, null, 2)}\n`, "utf8");
  return path;
};

export const loadCheckpoint = async (
  batch: string,
): Promise<Checkpoint | undefined> => {
  try {
    return JSON.parse(await readFile(pathFor(batch), "utf8")) as Checkpoint;
  } catch {
    return undefined;
  }
};

export const clearCheckpoint = async (batch: string): Promise<void> => {
  await rm(pathFor(batch), { force: true });
};

// ---------------------------------------------------------------------------
// Failure classification
// ---------------------------------------------------------------------------

export type FailureKind =
  /** Claude's 5-hour window. Resets at a known-ish time; sleep and resume. */
  | "claude-usage-limit"
  /** Codex's weekly bucket. No useful sleep window; park for a human. */
  | "codex-usage-limit"
  /** Anything else. Park. Never auto-retry. */
  | "unknown";

/**
 * Classifies a thrown error loosely, per the plan: "classify usage-limit errors
 * loosely (unknown error after partial progress = park, never auto-retry)".
 *
 * The asymmetry is deliberate. Mistaking a usage limit for an unknown error
 * parks a batch that could have resumed — you lose a night. Mistaking a real
 * bug for a usage limit sends the rig into a sleep-and-retry loop against a
 * failure that will never clear, burning the quota it is waiting for. So the
 * patterns below are narrow, and everything else is "unknown".
 */
export const classifyFailure = (error: unknown): FailureKind => {
  const text = [
    error instanceof Error ? error.message : String(error),
    error instanceof Error && error.cause ? String(error.cause) : "",
  ]
    .join(" ")
    .toLowerCase();

  const mentionsLimit =
    text.includes("usage limit") ||
    text.includes("rate limit") ||
    text.includes("quota") ||
    text.includes("429") ||
    text.includes("resets at") ||
    text.includes("out of credit");

  if (!mentionsLimit) return "unknown";

  if (text.includes("weekly") || text.includes("codex")) {
    return "codex-usage-limit";
  }
  if (text.includes("claude") || text.includes("anthropic") || text.includes("5-hour")) {
    return "claude-usage-limit";
  }
  return "unknown";
};

/**
 * Best-effort parse of a reset time out of a Claude limit message, e.g.
 * "resets at 3pm" / "resets at 2026-07-15T22:00:00Z". Returns undefined when
 * there's nothing parseable — the caller then parks rather than guessing a
 * sleep duration.
 */
export const parseResetTime = (error: unknown): Date | undefined => {
  const text = error instanceof Error ? error.message : String(error);

  const iso = text.match(/resets? (?:at|in) (\d{4}-\d{2}-\d{2}T[\d:.]+Z?)/i);
  if (iso) {
    const date = new Date(iso[1]!);
    if (!Number.isNaN(date.getTime())) return date;
  }

  const epoch = text.match(/resets? at (\d{10,13})\b/i);
  if (epoch) {
    const raw = Number(epoch[1]);
    const date = new Date(raw > 1e12 ? raw : raw * 1000);
    if (!Number.isNaN(date.getTime())) return date;
  }

  return undefined;
};
