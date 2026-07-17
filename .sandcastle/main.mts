// Bugfix pipeline entry point.
//
//   pnpm pipeline <batch> [--fix-agent codex|claude] [--resume]
//   pnpm pipeline --list
//
// One batch per invocation (README decision D1: manually launched runs, no
// event-driven triggering). Batches are defined in config.mts.

import { BATCHES, getBatch } from "./config.mts";
import { loadCheckpoint } from "./lib/checkpoint.mts";
import { Parked, runBatch } from "./lib/pipeline.mts";

const argv = process.argv.slice(2);

/** Options that take a value — everything else is a bare flag. */
const VALUED = new Set(["--fix-agent"]);

const flags = new Set<string>();
const options = new Map<string, string>();
const positionals: string[] = [];

for (let i = 0; i < argv.length; i++) {
  const arg = argv[i]!;
  if (!arg.startsWith("--")) {
    positionals.push(arg);
  } else if (VALUED.has(arg)) {
    const value = argv[++i];
    if (value === undefined) {
      console.error(`${arg} needs a value`);
      process.exit(1);
    }
    options.set(arg, value);
  } else {
    flags.add(arg);
  }
}

const flag = (name: string): boolean => flags.has(`--${name}`);
const option = (name: string): string | undefined => options.get(`--${name}`);

const usage = (): void => {
  console.log(
    [
      "Usage: pnpm pipeline <batch> [options]",
      "",
      "Batches:",
      ...BATCHES.map(
        (b) => `  ${b.id.padEnd(18)} #${b.issue}  ${b.bugs.join(", ")}`,
      ),
      "",
      "Options:",
      "  --fix-agent <codex|claude>  Agent for the fix stage (default: codex)",
      "  --resume                    Continue from this batch's checkpoint",
      "  --list                      Show batch status and exit",
    ].join("\n"),
  );
};

const list = async (): Promise<void> => {
  console.log("Batch              Issue  Bugs                     Checkpoint");
  for (const batch of BATCHES) {
    const checkpoint = await loadCheckpoint(batch.id);
    console.log(
      `${batch.id.padEnd(18)} #${String(batch.issue).padEnd(5)} ${batch.bugs.join(", ").padEnd(24)} ${
        checkpoint
          ? `parked at ${checkpoint.stage} (${checkpoint.kind}) ${checkpoint.updatedAt.slice(0, 16)}`
          : "-"
      }`,
    );
  }
};

if (flag("list")) {
  await list();
  process.exit(0);
}

if (flag("help") || positionals.length === 0) {
  usage();
  process.exit(flag("help") ? 0 : 1);
}

if (positionals.length > 1) {
  console.error(
    `One batch per run (decision D1). Got: ${positionals.join(", ")}`,
  );
  process.exit(1);
}

const batchId = positionals[0]!;

const fixAgentKind = option("fix-agent") ?? "codex";
if (fixAgentKind !== "codex" && fixAgentKind !== "claude") {
  console.error(`--fix-agent must be "codex" or "claude", got "${fixAgentKind}"`);
  process.exit(1);
}

let batch;
try {
  batch = getBatch(batchId);
} catch (error) {
  console.error((error as Error).message);
  process.exit(1);
}

const checkpoint = flag("resume") ? await loadCheckpoint(batch.id) : undefined;

if (flag("resume") && !checkpoint) {
  console.error(
    `No checkpoint for batch "${batch.id}" — nothing to resume. Drop --resume to start it.`,
  );
  process.exit(1);
}

if (checkpoint) {
  console.log(
    `Resuming ${batch.id} from checkpoint: parked at ${checkpoint.stage} (${checkpoint.kind}) — ${checkpoint.reason}`,
  );
}

try {
  await runBatch({ batch, fixAgentKind, resume: checkpoint });
  process.exit(0);
} catch (error) {
  if (error instanceof Parked) {
    // Already checkpointed, commented, and labelled. Exit non-zero so a wrapper
    // script running batches back to back can tell this one didn't land.
    process.exit(2);
  }
  console.error(error);
  process.exit(1);
}
