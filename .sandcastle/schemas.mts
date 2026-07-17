// Structured output schemas for the three stages.
//
// Each stage's prompt asks the agent to emit exactly one XML tag containing
// JSON matching one of these shapes. Sandcastle extracts and validates it;
// a missing tag or a schema violation throws StructuredOutputError rather than
// letting a malformed verdict through as a silent pass.

import { z } from "zod";
import type { ConfirmationMethod } from "./config.mts";

const bugId = z.string().regex(/^B\d+$/, "bug id must look like B6");

// ---------------------------------------------------------------------------
// Verify
// ---------------------------------------------------------------------------

export const VERIFY_TAG = "verify-verdicts";

/** A quoted line that proves part of the claim, for static-evidence batches. */
export const evidenceSchema = z.object({
  /** `file.kt:162-179` — where the quote came from. */
  location: z.string().min(1),
  /** The lines themselves, quoted verbatim. */
  quote: z.string().min(1),
});

export const verifyVerdictSchema = z.object({
  bug: bugId,
  /**
   * What `confirmed` requires depends on the batch's confirmation method — a
   * committed failing test, or quoted static evidence. `dismissed` is the
   * honest outcome when the claim doesn't hold up; the hard rule is that we
   * never fix speculatively.
   */
  verdict: z.enum(["confirmed", "dismissed"]),
  /** Why. For a dismissal this is the disposition written back to the issue. */
  reason: z.string().min(1),
  /** Repo-relative path of the failing test. Required when confirmed on a failing-test batch. */
  testFile: z.string().optional(),
  /** Name of the failing test. Required when confirmed on a failing-test batch. */
  testName: z.string().optional(),
  /** Quoted proof. Required when confirmed on a static-evidence batch. */
  evidence: z.array(evidenceSchema).optional(),
  /**
   * The concrete failure sequence the evidence establishes — which calls, in
   * which order, lose which data. Required when confirmed on a static-evidence
   * batch: quotes alone show what the code says, not why it's wrong.
   */
  sequence: z.string().optional(),
});

export const verifyOutputSchema = z.object({
  verdicts: z.array(verifyVerdictSchema).min(1),
});

export type Evidence = z.infer<typeof evidenceSchema>;
export type VerifyVerdict = z.infer<typeof verifyVerdictSchema>;
export type VerifyOutput = z.infer<typeof verifyOutputSchema>;

/**
 * What a confirmation is missing, or undefined when it's well-formed.
 *
 * Zod could express this as a discriminated union, but the failure would
 * surface as an opaque union error naming neither the bug nor the missing
 * field. Checked here so the gate can say exactly what's wrong, per bug.
 *
 * Dismissals are never deficient — a dismissal's evidence is its `reason`, and
 * demanding proof of absence is what would push an agent toward inventing it.
 */
export const confirmationDeficiency = (
  verdict: VerifyVerdict,
  method: ConfirmationMethod,
): string | undefined => {
  if (verdict.verdict !== "confirmed") return undefined;

  if (method === "failing-test") {
    if (!verdict.testFile || !verdict.testName) {
      return "confirmed without naming the committed failing test (testFile + testName)";
    }
    return undefined;
  }

  if (!verdict.evidence || verdict.evidence.length === 0) {
    return "confirmed without quoting any evidence — this batch has no test surface, so the quoted lines are the whole proof";
  }
  if (!verdict.sequence) {
    return "confirmed with evidence but no `sequence` — the quotes show what the code says, not what goes wrong";
  }
  return undefined;
};

// ---------------------------------------------------------------------------
// Fix
// ---------------------------------------------------------------------------

export const FIX_TAG = "fix-results";

export const fixResultSchema = z.object({
  bug: bugId,
  status: z.enum(["fixed", "failed"]),
  /** What changed and why it addresses the spec, not just the test. */
  summary: z.string().min(1),
  /** Required when failed — what blocked it. */
  blocker: z.string().optional(),
});

export const fixOutputSchema = z.object({
  results: z.array(fixResultSchema).min(1),
  /** The agent's own claim that the suites are green. Never trusted — the
   *  orchestrator re-runs the suites itself. Kept to compare claim vs reality. */
  suitesGreen: z.boolean(),
});

export type FixResult = z.infer<typeof fixResultSchema>;
export type FixOutput = z.infer<typeof fixOutputSchema>;

// ---------------------------------------------------------------------------
// Review
// ---------------------------------------------------------------------------

export const REVIEW_TAG = "review-verdict";

export const reviewFindingSchema = z.object({
  bug: bugId,
  /** `test-gaming` is called out separately because it is the failure mode the
   *  review stage exists to catch: a fix shaped to the assertion, not the bug. */
  kind: z.enum(["test-gaming", "scope-violation", "incomplete", "regression"]),
  detail: z.string().min(1),
});

export const reviewOutputSchema = z.object({
  verdict: z.enum(["clean", "findings"]),
  findings: z.array(reviewFindingSchema),
  summary: z.string().min(1),
});

export type ReviewFinding = z.infer<typeof reviewFindingSchema>;
export type ReviewOutput = z.infer<typeof reviewOutputSchema>;
