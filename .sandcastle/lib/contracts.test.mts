import assert from "node:assert/strict";
import test from "node:test";
import { exactBugCoverageProblems, reviewContractProblems } from "./contracts.mts";

test("exactBugCoverageProblems rejects missing, extra, and duplicate bug results", () => {
  assert.deepEqual(exactBugCoverageProblems(["B1", "B3"], ["B1", "B1", "B7"]), [
    "missing: B3",
    "out of batch: B7",
    "duplicated: B1",
  ]);
  assert.deepEqual(exactBugCoverageProblems(["B1", "B3"], ["B3", "B1"]), []);
});

test("reviewContractProblems enforces verdict consistency and bug scope", () => {
  assert.deepEqual(
    reviewContractProblems(["B1"], {
      verdict: "clean",
      findings: [{ bug: "B7", kind: "incomplete", detail: "still broken" }],
      summary: "bad output",
    }),
    ["findings reference bugs outside the reviewed fixes: B7", "clean verdict contains findings"],
  );
  assert.deepEqual(
    reviewContractProblems(["B1"], { verdict: "findings", findings: [], summary: "bad output" }),
    ["findings verdict contains no findings"],
  );
});
