// Windows shakedown — plan.md Phase 4.
//
// Proves the three toolchains the pipeline shells out to actually run under the
// sandbox provider's spawn (pwsh -NonInteractive -Command) from inside a git
// worktree, which is where shell/quoting/PATHEXT problems surface. Run this
// after any change to the provider wiring or a toolchain upgrade:
//
//   pnpm pipeline:shakedown
//
// Exits non-zero on the first failure so it can gate a run.

import { createSandbox } from "@ai-hero/sandcastle";
import { noSandboxPwsh } from "./lib/no-sandbox-pwsh.mts";

interface Check {
  readonly name: string;
  readonly command: string;
  /** Substring the output must contain for the check to count as passing. */
  readonly expect: string;
}

const checks: Check[] = [
  { name: "gh", command: "gh --version", expect: "gh version" },
  { name: "pnpm", command: "pnpm --version", expect: "." },
  { name: "node", command: "node --version", expect: "v" },
  { name: "git", command: "git rev-parse --abbrev-ref HEAD", expect: "" },
  { name: "Claude Code", command: "claude --version", expect: "." },
  { name: "Codex", command: "codex --version", expect: "codex-cli" },
  // The `.\` prefix is load-bearing, not style. This host sets
  // NoDefaultCurrentDirectoryInExePath=1, so a plain shell does not resolve
  // executables from the current directory and bare `gradlew.bat` fails with
  // "not recognized as an internal or external command". Prompts tell the
  // agents the same thing; this check is what keeps that claim honest.
  {
    name: "gradlew.bat",
    command: "Set-Location apps\\mobile && .\\gradlew.bat --version",
    expect: "Gradle",
  },
];

const branch = `sandcastle/shakedown/${Date.now()}`;

const sandbox = await createSandbox({
  branch,
  sandbox: noSandboxPwsh(),
});

let failed = 0;

try {
  console.log(`Worktree: ${sandbox.worktreePath}\n`);

  for (const check of checks) {
    const result = await sandbox.exec(check.command);
    const output = `${result.stdout}${result.stderr}`;
    const ok = result.exitCode === 0 && output.includes(check.expect);

    if (ok) {
      console.log(`PASS  ${check.name.padEnd(12)} ${firstLine(output)}`);
    } else {
      failed++;
      console.log(`FAIL  ${check.name.padEnd(12)} exit=${result.exitCode}`);
      console.log(indent(output.trim() || "(no output)"));
    }
  }
} finally {
  await sandbox.close();
}

console.log(
  failed === 0
    ? `\nAll ${checks.length} checks passed.`
    : `\n${failed}/${checks.length} checks FAILED.`,
);

process.exit(failed === 0 ? 0 : 1);

function firstLine(text: string): string {
  return text.trim().split("\n")[0]?.trim() ?? "";
}

function indent(text: string): string {
  return text
    .split("\n")
    .map((line) => `        ${line}`)
    .join("\n");
}
