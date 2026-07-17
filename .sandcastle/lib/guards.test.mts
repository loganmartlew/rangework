import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { promisify } from "node:util";
import {
  checkFailingTestConfirmations,
  checkFilesUnchanged,
  checkPerBugCommits,
  checkVerifyStageChanges,
  resolveCommit,
} from "./guards.mts";
import type { VerifyVerdict } from "../schemas.mts";

const exec = promisify(execFile);

test("verify commit cannot satisfy the fix commit guard and its test is frozen", async () => {
  const cwd = await mkdtemp(join(tmpdir(), "rw-pipeline-guards-"));
  const git = (args: string[]) => exec("git", args, { cwd });
  const commit = async (message: string) => {
    await git(["add", "."]);
    await git(["commit", "-m", message]);
  };

  try {
    await git(["init", "-b", "main"]);
    await git(["config", "user.email", "pipeline-test@example.invalid"]);
    await git(["config", "user.name", "Pipeline Test"]);
    await writeFile(join(cwd, "production.ts"), "export const value = 1;\n");
    await commit("initial");
    await git(["switch", "-c", "bugfix/mcp"]);

    const testDir = join(cwd, "src", "test");
    await mkdir(testDir, { recursive: true });
    const testPath = "src/test/bug.test.ts";
    await writeFile(join(cwd, testPath), "throw new Error('red');\n");
    await commit("B6: add failing test");

    const verdict: VerifyVerdict = {
      bug: "B6",
      verdict: "confirmed",
      reason: "reproduced",
      testFile: testPath,
      testName: "is red",
    };
    assert.deepEqual(
      await checkFailingTestConfirmations("main", "bugfix/mcp", [verdict], cwd),
      [],
    );
    assert.deepEqual(await checkVerifyStageChanges("main", "bugfix/mcp", true, cwd), []);

    const verifyHead = await resolveCommit("bugfix/mcp", cwd);
    assert.equal(
      (await checkPerBugCommits(verifyHead, "bugfix/mcp", ["B6"], cwd)).length,
      1,
      "the verify commit must not count as the production fix commit",
    );

    await writeFile(join(cwd, "production.ts"), "export const value = 2;\n");
    await commit("B6: fix production behavior");
    assert.equal((await checkVerifyStageChanges("main", "bugfix/mcp", true, cwd)).length, 1);
    assert.deepEqual(await checkPerBugCommits(verifyHead, "bugfix/mcp", ["B6"], cwd), []);
    assert.deepEqual(await checkFilesUnchanged(verifyHead, "bugfix/mcp", [testPath], cwd), []);

    await writeFile(join(cwd, testPath), "// weakened\n");
    await commit("B6: weaken test");
    assert.equal(
      (await checkFilesUnchanged(verifyHead, "bugfix/mcp", [testPath], cwd)).length,
      1,
    );
  } finally {
    await rm(cwd, { recursive: true, force: true });
  }
});
