// Spec loading and prompt rendering.
//
// Prompts are rendered here and handed to run() as an *inline* string rather
// than via promptFile + promptArgs. That is deliberate: Sandcastle expands
// !`shell command` expressions in file-based prompts, and spec text is
// arbitrary markdown we paste in wholesale. Inline prompts skip both
// substitution and expansion (sandcastle ADR 0008), so a spec can never smuggle
// a shell command into an unattended run.

import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import { PROMPTS_DIR, SPECS_DIR, type Batch, type ConfirmationMethod } from "../config.mts";
import type { VerifyVerdict } from "../schemas.mts";

export interface Spec {
  readonly bug: string;
  /** Repo-relative path, for error messages. */
  readonly path: string;
  readonly body: string;
}

/**
 * Loads one spec per bug in the batch. Specs are written by hand (plan.md
 * Phase 3) at specs/bNN-slug.md.
 *
 * Missing specs are fatal rather than skippable: the hard rule is "no
 * done-criteria → no fix attempt", and a batch that quietly drops the bug it
 * couldn't find a spec for would report success having done less than asked.
 */
export const loadSpecs = async (batch: Batch): Promise<Spec[]> => {
  let entries: string[];
  try {
    entries = await readdir(SPECS_DIR);
  } catch {
    throw new Error(
      `No specs directory at ${SPECS_DIR}.\n` +
        `Per-bug specs are plan.md Phase 3 ("Write all per-bug specs into specs/") ` +
        `and must exist before a batch can run.`,
    );
  }

  const specs: Spec[] = [];
  const missing: string[] = [];

  for (const bug of batch.bugs) {
    // Spec files zero-pad the number (`b06-slug.md`) while bug ids don't (`B6`),
    // so match the numeric part, not a literal prefix: `b0*0*<n>-`.
    const number = bug.replace(/^B/i, "");
    const pattern = new RegExp(`^b0*${number}-.*\\.md$`, "i");
    const match = entries.find((e) => pattern.test(e));
    if (!match) {
      missing.push(bug);
      continue;
    }
    const path = join(SPECS_DIR, match);
    specs.push({
      bug,
      path: `design-docs/app-review/bugfix-pipeline/specs/${match}`,
      body: (await readFile(path, "utf8")).trim(),
    });
  }

  if (missing.length > 0) {
    throw new Error(
      `Batch "${batch.id}" is missing specs for: ${missing.join(", ")}.\n` +
        `Expected files like ${SPECS_DIR}\\${missing[0]!.toLowerCase()}-slug.md`,
    );
  }

  return specs;
};

/**
 * Renders the specs into the block the prompts interpolate, optionally with each
 * bug's verify verdict attached.
 *
 * The verdict travels with the spec into the fix and review stages because on a
 * static-evidence batch it *is* the proof — there is no failing test carrying
 * that information in the branch, so without this the downstream agents would be
 * re-deriving the confirmation from scratch or taking it on faith.
 */
export const renderSpecs = (
  specs: readonly Spec[],
  verdicts?: readonly VerifyVerdict[],
): string =>
  specs
    .map((s) => {
      const verdict = verdicts?.find((v) => v.bug === s.bug);
      return [
        `### ${s.bug} — from ${s.path}`,
        "",
        s.body,
        verdict ? `\n#### Verify verdict for ${s.bug}\n\n${renderVerdict(verdict)}` : "",
      ].join("\n");
    })
    .join("\n\n---\n\n");

const renderVerdict = (verdict: VerifyVerdict): string => {
  const lines = [`**${verdict.verdict}** — ${verdict.reason}`];

  if (verdict.testFile) {
    lines.push("", `Failing test: \`${verdict.testName}\` in \`${verdict.testFile}\``);
  }

  if (verdict.evidence?.length) {
    lines.push("", "Evidence quoted at verify time:", "");
    for (const item of verdict.evidence) {
      lines.push(`- \`${item.location}\``, "", "  ```", ...indent(item.quote), "  ```", "");
    }
  }

  if (verdict.sequence) {
    lines.push("", `Failure sequence: ${verdict.sequence}`);
  }

  return lines.join("\n");
};

const indent = (text: string): string[] =>
  text.split("\n").map((line) => `  ${line}`);

/**
 * Fills `{{PLACEHOLDER}}` in a template.
 *
 * Throws on any placeholder left unfilled. A `{{SPECS}}` that silently rendered
 * as the literal string would produce an agent run that looks fine in the log
 * and verifies nothing.
 */
export const render = (
  template: string,
  vars: Record<string, string>,
): string => {
  const out = template.replace(/\{\{(\w+)\}\}/g, (whole, key: string) => {
    const value = vars[key];
    if (value === undefined) return whole;
    return value;
  });

  const leftover = [...out.matchAll(/\{\{(\w+)\}\}/g)].map((m) => m[1]!);
  if (leftover.length > 0) {
    throw new Error(
      `Prompt template has unfilled placeholders: ${[...new Set(leftover)].join(", ")}`,
    );
  }
  return out;
};

export const loadTemplate = (name: string): Promise<string> =>
  readFile(join(PROMPTS_DIR, `${name}.md`), "utf8");

/**
 * Loads the method-specific half of a stage prompt.
 *
 * The verify, fix, and review stages all have to say materially different things
 * depending on whether the batch has a test surface. Two full copies of each
 * template would drift; a partial keeps the branching in one visible place per
 * stage.
 */
export const loadPartial = (
  stage: "verify" | "fix" | "review",
  method: ConfirmationMethod,
): Promise<string> =>
  readFile(join(PROMPTS_DIR, "partials", `${stage}-${method}.md`), "utf8");
