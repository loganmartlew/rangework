// Preflight: are the per-bug specs (plan.md Phase 3) in place?
//
//   pnpm pipeline:check-specs
//
// Reads spec files only — launches no agents and touches no remote. Worth
// running before an overnight batch: a missing spec is the one failure that
// costs nothing to catch now and a whole night to discover at 3am.

import { BATCHES } from "./config.mts";
import { loadSpecs } from "./lib/prompts.mts";

let missing = 0;

for (const batch of BATCHES) {
  try {
    const specs = await loadSpecs(batch);
    console.log(`ok    ${batch.id.padEnd(18)} ${specs.length} spec(s)`);
  } catch (error) {
    missing++;
    const first = (error as Error).message.split("\n")[0];
    console.log(`MISS  ${batch.id.padEnd(18)} ${first}`);
  }
}

console.log(
  missing === 0
    ? "\nAll batches have specs."
    : `\n${missing}/${BATCHES.length} batches are missing specs — write them before running the pipeline.`,
);

process.exit(missing === 0 ? 0 : 1);
