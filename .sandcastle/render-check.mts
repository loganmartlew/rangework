// Offline prompt-render check. Renders every stage prompt for every batch with
// placeholder verdicts and asserts no {{PLACEHOLDER}} survives and no partial is
// missing. Launches no agents, touches no remote.
//
//   npx tsx .sandcastle/render-check.mts

import { BATCHES, BASE_BRANCH, branchFor } from "./config.mts";
import {
  loadPartial,
  loadSpecs,
  loadTemplate,
  render,
  renderSpecs,
} from "./lib/prompts.mts";
import {
  FIX_TAG,
  REVIEW_TAG,
  VERIFY_TAG,
  type VerifyVerdict,
} from "./schemas.mts";

let failures = 0;

for (const batch of BATCHES) {
  const branch = branchFor(batch);
  const specs = await loadSpecs(batch);

  // A confirmed verdict shaped per the batch's method, so renderSpecs exercises
  // the evidence/test branches.
  const verdicts: VerifyVerdict[] = batch.bugs.map((bug) =>
    batch.confirmation === "failing-test"
      ? {
          bug,
          verdict: "confirmed",
          reason: "placeholder",
          testFile: "x/y.test.ts",
          testName: "does the thing",
        }
      : {
          bug,
          verdict: "confirmed",
          reason: "placeholder",
          evidence: [{ location: "File.kt:1-2", quote: "line" }],
          sequence: "a then b loses c",
        },
  );

  const commands = {
    TEST_COMMANDS: batch.testCommands.join("\n"),
    LINT_COMMANDS: batch.lintCommands.join("\n") || "(no lint step)",
  };

  const rendered: Record<string, string> = {
    verify: render(await loadTemplate("verify"), {
      BATCH: batch.id,
      BRANCH: branch,
      BUGS: batch.bugs.join(", "),
      CONFIRMATION: batch.confirmation,
      CONFIRM_BLOCK: await loadPartial("verify", batch.confirmation),
      SCOPE: batch.scope,
      TEST_COMMANDS: commands.TEST_COMMANDS,
      SPECS: renderSpecs(specs),
      TAG: VERIFY_TAG,
    }),
    fix: render(await loadTemplate("fix"), {
      BATCH: batch.id,
      BRANCH: branch,
      BUGS: batch.bugs.join(", "),
      CONFIRMATION: batch.confirmation,
      FIX_BASIS_BLOCK: await loadPartial("fix", batch.confirmation),
      SCOPE: batch.scope,
      ...commands,
      SPECS: renderSpecs(specs, verdicts),
      TAG: FIX_TAG,
    }),
    review: render(await loadTemplate("review"), {
      BATCH: batch.id,
      BRANCH: branch,
      BASE_BRANCH,
      BUGS: batch.bugs.join(", "),
      CONFIRMATION: batch.confirmation,
      REVIEW_FOCUS_BLOCK: await loadPartial("review", batch.confirmation),
      SCOPE: batch.scope,
      SPECS: renderSpecs(specs, verdicts),
      TAG: REVIEW_TAG,
    }),
    "fix-retry": render(await loadTemplate("fix-retry"), {
      BATCH: batch.id,
      BRANCH: branch,
      CONFIRMATION: batch.confirmation,
      SCOPE: batch.scope,
      ...commands,
      FINDINGS: "- B0 (test-gaming): placeholder",
      SPECS: renderSpecs(specs, verdicts),
      TAG: FIX_TAG,
    }),
  };

  for (const [stage, text] of Object.entries(rendered)) {
    // render() already throws on leftover placeholders; belt-and-suspenders in
    // case a partial reintroduces one.
    const leftover = [...text.matchAll(/\{\{(\w+)\}\}/g)];
    const tagPresent = text.includes(
      `<${stage === "review" ? REVIEW_TAG : stage.startsWith("fix") ? FIX_TAG : VERIFY_TAG}>`,
    );
    const ok = leftover.length === 0 && tagPresent;
    if (!ok) failures++;
    console.log(
      `${ok ? "ok  " : "FAIL"} ${batch.id.padEnd(18)} ${stage.padEnd(10)} ${text.length} chars` +
        (leftover.length ? ` leftover: ${leftover.map((m) => m[0]).join(",")}` : "") +
        (tagPresent ? "" : " (output tag missing!)"),
    );
  }
}

console.log(failures === 0 ? "\nAll prompts render." : `\n${failures} render failure(s).`);
process.exit(failures === 0 ? 0 : 1);
