import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const mobileDir = path.resolve(currentDir, "..");
const gradleArgs = process.argv.slice(2);

if (gradleArgs.length === 0) {
  console.error("Expected at least one Gradle task.");
  process.exit(1);
}

const command = process.platform === "win32" ? "cmd.exe" : path.join(mobileDir, "gradlew");
const commandArgs =
  process.platform === "win32" ? ["/c", "gradlew.bat", ...gradleArgs] : gradleArgs;

const child = spawn(command, commandArgs, {
  cwd: mobileDir,
  stdio: "inherit",
});

child.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }

  process.exit(code ?? 1);
});
