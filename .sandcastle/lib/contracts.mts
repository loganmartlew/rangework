// Pure stage-contract validation. Keeping this separate from the orchestration
// makes malformed agent output cheap to test without launching an agent.

import type { ReviewOutput } from "../schemas.mts";

export const exactBugCoverageProblems = (
  expected: readonly string[],
  actual: readonly string[],
): string[] => {
  const problems: string[] = [];
  const counts = new Map<string, number>();
  for (const bug of actual) counts.set(bug, (counts.get(bug) ?? 0) + 1);

  const missing = expected.filter((bug) => !counts.has(bug));
  const extra = [...counts.keys()].filter((bug) => !expected.includes(bug));
  const duplicates = [...counts.entries()]
    .filter(([, count]) => count > 1)
    .map(([bug]) => bug);

  if (missing.length) problems.push(`missing: ${missing.join(", ")}`);
  if (extra.length) problems.push(`out of batch: ${extra.join(", ")}`);
  if (duplicates.length) problems.push(`duplicated: ${duplicates.join(", ")}`);
  return problems;
};

export const reviewContractProblems = (
  fixedBugs: readonly string[],
  review: ReviewOutput,
): string[] => {
  const problems: string[] = [];
  const extra = review.findings
    .map((finding) => finding.bug)
    .filter((bug) => !fixedBugs.includes(bug));

  if (extra.length) {
    problems.push(`findings reference bugs outside the reviewed fixes: ${[...new Set(extra)].join(", ")}`);
  }
  if (review.verdict === "clean" && review.findings.length > 0) {
    problems.push("clean verdict contains findings");
  }
  if (review.verdict === "findings" && review.findings.length === 0) {
    problems.push("findings verdict contains no findings");
  }
  return problems;
};
