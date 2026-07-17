// Batch definitions and pipeline configuration.
//
// Mirrors design-docs/app-review/bugfix-pipeline/batches.md — when a batch's
// composition changes there, change it here too.

import { claudeCode, codex, type AgentProvider } from '@ai-hero/sandcastle';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');

export const DOCS_DIR = join(
  REPO_ROOT,
  'design-docs',
  'app-review',
  'bugfix-pipeline',
);
export const SPECS_DIR = join(DOCS_DIR, 'specs');
export const STATE_DIR = join(REPO_ROOT, '.sandcastle', 'state');
export const PROMPTS_DIR = join(REPO_ROOT, '.sandcastle', 'prompts');

/** Branch every batch's work lands on top of, and the PR base. */
export const BASE_BRANCH = 'main';

/**
 * How a batch's bugs can be confirmed — and it is not uniform across batches.
 * See specs/README.md ("Confirmation methods are not uniform").
 *
 * - `failing-test` — a confirmation means a committed failing test, and "no test
 *   can be written" is grounds to dismiss. The D10 default.
 * - `static-evidence` — the batch has **no test surface at all**: no pgTAP, no
 *   `supabase/tests/`, no fake Supabase client in `:shared`, and no Docker (D2),
 *   so `supabase start` isn't available either. Confirmation means quoting the
 *   lines that prove the claim. **Lack of a test is not grounds to dismiss** —
 *   applying the D10 default here would auto-dismiss 8 bugs, including the three
 *   high-severity data-loss ones (B1, B3, B7).
 *
 * The trade-off to respect: static-evidence batches have no mechanical
 * "test went green" signal, so the review stage and the human PR review are the
 * only anti-gaming guards. Their specs carry stricter scope boundaries to
 * compensate, which is why `scope` below is narrower for those two.
 */
export type ConfirmationMethod = 'failing-test' | 'static-evidence';

export interface Batch {
  /** Batch id — also the CLI argument and the state file name. */
  readonly id: string;
  /** GitHub issue tracking this batch (the state machine — see README D5). */
  readonly issue: number;
  /** Bug ids, in batches.md order. Each needs a spec at specs/bNN-*.md. */
  readonly bugs: readonly string[];
  readonly confirmation: ConfirmationMethod;
  /** Commands that must exit 0 before a fix counts as done. Run in the worktree root. */
  readonly testCommands: readonly string[];
  /** Lint commands that must exit 0 before a fix counts as done. */
  readonly lintCommands: readonly string[];
  /** Scope boundary, quoted verbatim into the prompts. */
  readonly scope: string;
}

// `.\` on gradlew.bat is required: the host sets
// NoDefaultCurrentDirectoryInExePath=1, so cmd.exe will not resolve an
// executable from the current directory. See .sandcastle/shakedown.mts.
const GRADLE = 'cd /d apps\\mobile && .\\gradlew.bat';

// Test/lint commands are the union of the Definition of done lines across each
// batch's specs — the suites the orchestrator re-runs to grade the batch.
const ALL_UNIT_TESTS = `${GRADLE} :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest`;
const KOTLIN_LINT = `${GRADLE} :shared:lintDebug :androidApp:lintDebug`;

export const BATCHES: readonly Batch[] = [
  {
    id: 'mcp',
    issue: 47,
    bugs: ['B6'],
    confirmation: 'failing-test',
    testCommands: ['pnpm --filter @rangework/mcp test'],
    lintCommands: ['pnpm --filter @rangework/mcp lint'],
    scope:
      'apps/mcp only. No changes to the Android app, the shared KMP module, or Supabase migrations.',
  },
  {
    id: 'supabase-schema',
    issue: 48,
    bugs: ['B4', 'B5', 'B8', 'B16', 'B20'],
    confirmation: 'static-evidence',
    // There is no SQL harness to run (no pgTAP, no Docker). The MCP suite is a
    // regression check — it is the only thing in the repo that exercises these
    // functions — not proof the migrations are correct. `supabase db lint` is
    // deliberately absent: it needs a running local database, which D2 rules out.
    testCommands: ['pnpm --filter @rangework/mcp test'],
    lintCommands: [],
    scope:
      'supabase/migrations/ only — new timestamped files, never an edit to an existing migration. No Kotlin, no TypeScript.',
  },
  {
    id: 'shared-validation',
    issue: 49,
    bugs: ['B14', 'B15', 'B17'],
    confirmation: 'failing-test',
    testCommands: [ALL_UNIT_TESTS],
    lintCommands: [KOTLIN_LINT],
    scope:
      'apps/mobile/shared/ only. No Compose/UI changes, no migrations, no public API changes outside the validation and decode paths named in the specs.',
  },
  {
    id: 'shared-repo',
    issue: 50,
    bugs: ['B1', 'B3', 'B7'],
    confirmation: 'static-evidence',
    testCommands: [ALL_UNIT_TESTS],
    lintCommands: [KOTLIN_LINT],
    scope:
      'supabase/migrations/ (new files only) and SupabaseRangeSessionRepository.kt, plus the RangeSessionRepository interface only if the return contract must change. No Compose changes. No changes to the recording/freeze-matrix rules.',
  },
  {
    id: 'android-ui',
    issue: 51,
    bugs: ['B2', 'B9', 'B18', 'B19'],
    confirmation: 'failing-test',
    // B18's code and tests live in :shared even though the batch is android-ui,
    // so both suites run here and a :shared diff in this batch's PR is expected.
    testCommands: [ALL_UNIT_TESTS],
    lintCommands: [KOTLIN_LINT],
    scope:
      'apps/mobile/androidApp/, plus PracticeDraftEditor.kt in shared/ for B18. No migrations.',
  },
] as const;

export const getBatch = (id: string): Batch => {
  const batch = BATCHES.find(b => b.id === id);
  if (!batch) {
    throw new Error(
      `Unknown batch "${id}". Known batches: ${BATCHES.map(b => b.id).join(', ')}`,
    );
  }
  return batch;
};

/** Branch a batch's work lands on. Stable across stages and resumes. */
export const branchFor = (batch: Batch): string => `bugfix/${batch.id}`;

// ---------------------------------------------------------------------------
// Agent roles (README decision D3)
// ---------------------------------------------------------------------------

/** Claude owns the judgment stages: verify and review. */
export const verifyAgent = (): AgentProvider =>
  claudeCode('claude-opus-4-8', { effort: 'high' });

export const reviewAgent = (): AgentProvider =>
  claudeCode('claude-opus-4-8', { effort: 'high' });

/**
 * Model the Codex CLI is invoked with. Override it without changing the rig by
 * setting RANGEWORK_CODEX_MODEL.
 */
export const CODEX_MODEL = process.env.RANGEWORK_CODEX_MODEL ?? 'gpt-5.6-terra';

/**
 * Codex owns the fix stage — its weekly bucket suits long Gradle-heavy runs.
 *
 * D5's fallback ("Codex weekly cap → park the batch or fail the fix stage over
 * to Claude") is wired to the `--fix-agent` flag rather than an automatic
 * switch: silently spending Claude's 5-hour budget on a stage that was
 * explicitly assigned to Codex is exactly the kind of surprise an unattended
 * run should not spring on you.
 */
export const fixAgent = (kind: 'codex' | 'claude'): AgentProvider =>
  kind === 'codex'
    ? codex(CODEX_MODEL, { effort: 'high' })
    : claudeCode('claude-opus-4-8', { effort: 'high' });

/**
 * Agent stages get a long idle timeout: a cold `:androidApp:assembleDebug` can
 * sit silent for minutes, and a false idle-timeout kill mid-fix is worse than
 * waiting. The guard against a genuinely wedged run is the checkpoint, not this.
 */
export const IDLE_TIMEOUT_SECONDS = 1_800;
