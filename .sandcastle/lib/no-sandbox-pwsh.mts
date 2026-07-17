/**
 * A Windows-safe fork of `@ai-hero/sandcastle`'s `noSandbox()` provider.
 *
 * The upstream provider (`dist/chunk-62WN33RK.js`) runs every command through
 * `cmd.exe /d /s /c <command>` with `windowsVerbatimArguments: true`. That
 * combination is broken on Windows: the library's `shellEscape()` always wraps
 * values in POSIX-style single quotes (`'opus'`), but `cmd.exe` has no concept
 * of single-quote quoting — it passes the quote characters through as literal
 * text. `claude --model 'opus'` and `codex -c 'model_reasoning_effort="high"'`
 * both arrive at the agent CLI with the stray quote characters baked into the
 * argument, which the CLI then reports as an unrecognized/inaccessible model
 * (e.g. "selected model ('opus')" — those apostrophes are the corrupted
 * quoting, not the CLI's own formatting).
 *
 * This fork only changes the shell: `pwsh` instead of `cmd.exe`, invoked via a
 * normal Node argv array (no `windowsVerbatimArguments`), so Node applies its
 * own correct Windows argument quoting for the single `-Command` string. pwsh
 * then parses that string with its own quoting rules, under which single
 * quotes ARE a real quoting mechanism — `'opus'` and
 * `'model_reasoning_effort="high"'` both round-trip to the exact original
 * value. Verified empirically (see scratchpad test during the 2026-07-17
 * bugfix-pipeline shakedown) against both the plain-value and embedded-quote
 * cases the two agent providers this rig uses (claudeCode, codex) produce.
 *
 * `createSandbox()` dispatches purely on shape (`provider.tag === "none"` then
 * `provider.create(...)`), so this only needs to match that runtime duck type
 * — see `SandboxProvider-EkSMuBp8.d.ts` in the installed package for the
 * `NoSandboxHandle` / `NoSandboxProvider` contract this reproduces.
 */
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';

const MAX_TAIL_CHARS = 64 * 1024;

/** Keeps only the last `maxChars` of joined output — mirrors the upstream BoundedTail. */
class BoundedTail {
  private items: string[] = [];
  private totalChars = 0;

  constructor(
    private readonly maxChars = MAX_TAIL_CHARS,
    private readonly separator = '',
  ) {}

  push(item: string): void {
    const bounded =
      item.length > this.maxChars ? item.slice(item.length - this.maxChars) : item;
    this.totalChars += bounded.length + (this.items.length > 0 ? this.separator.length : 0);
    this.items.push(bounded);
    while (this.totalChars > this.maxChars && this.items.length > 1) {
      const dropped = this.items.shift()!;
      this.totalChars -= dropped.length + this.separator.length;
    }
  }

  toString(): string {
    return this.items.join(this.separator);
  }
}

export interface NoSandboxPwshOptions {
  /** Environment variables injected by this provider. Merged at launch time. */
  readonly env?: Record<string, string>;
  /** Max characters of streamed exec output retained per stream (default 64KiB). */
  readonly maxOutputTailChars?: number;
}

const PWSH_ARGS = ['-NoLogo', '-NoProfile', '-NonInteractive', '-Command'];

/** Windows-safe no-sandbox provider — see file header. Non-Windows platforms are untouched (still `sh -c`). */
export const noSandboxPwsh = (options?: NoSandboxPwshOptions) => ({
  tag: 'none' as const,
  name: 'no-sandbox-pwsh',
  env: options?.env ?? {},
  create: async (createOptions: { worktreePath: string; env: Record<string, string> }) => {
    const worktreePath = createOptions.worktreePath;
    const processEnv = { ...process.env, ...createOptions.env };
    const maxOutputTailChars = options?.maxOutputTailChars ?? MAX_TAIL_CHARS;

    const handle = {
      worktreePath,
      exec: (
        command: string,
        opts?: {
          onLine?: (line: string) => void;
          cwd?: string;
          sudo?: boolean;
          stdin?: string;
        },
      ) => {
        const cwd = opts?.cwd ?? worktreePath;
        const isWindows = process.platform === 'win32';
        const shellCmd = isWindows ? 'pwsh' : 'sh';
        const shellArgs = isWindows ? [...PWSH_ARGS, command] : ['-c', command];
        return new Promise<{ stdout: string; stderr: string; exitCode: number }>(
          (resolve, reject) => {
            const proc = spawn(shellCmd, shellArgs, {
              cwd,
              env: processEnv,
              stdio: [opts?.stdin !== undefined ? 'pipe' : 'ignore', 'pipe', 'pipe'],
              // Deliberately omitted on Windows: default (non-verbatim) argv
              // quoting is what makes pwsh's -Command string arrive intact.
            });
            if (opts?.stdin !== undefined) {
              proc.stdin!.write(opts.stdin);
              proc.stdin!.end();
            }
            proc.on('error', (error) => {
              reject(new Error(`exec failed: ${error.message}`));
            });
            if (opts?.onLine) {
              const onLine = opts.onLine;
              const stdoutTail = new BoundedTail(maxOutputTailChars, '\n');
              const stderrTail = new BoundedTail(maxOutputTailChars, '');
              const rl = createInterface({ input: proc.stdout! });
              rl.on('line', (line) => {
                stdoutTail.push(line);
                onLine(line);
              });
              proc.stderr!.on('data', (chunk) => {
                stderrTail.push(chunk.toString());
              });
              proc.on('close', (code) => {
                resolve({
                  stdout: stdoutTail.toString(),
                  stderr: stderrTail.toString(),
                  exitCode: code ?? 0,
                });
              });
            } else {
              const stdoutChunks: string[] = [];
              const stderrChunks: string[] = [];
              proc.stdout!.on('data', (chunk) => {
                stdoutChunks.push(chunk.toString());
              });
              proc.stderr!.on('data', (chunk) => {
                stderrChunks.push(chunk.toString());
              });
              proc.on('close', (code) => {
                resolve({
                  stdout: stdoutChunks.join(''),
                  stderr: stderrChunks.join(''),
                  exitCode: code ?? 0,
                });
              });
            }
          },
        );
      },
      interactiveExec: (
        args: string[],
        opts: {
          stdin: NodeJS.ReadableStream;
          stdout: NodeJS.WritableStream;
          stderr: NodeJS.WritableStream;
          cwd?: string;
        },
      ) => {
        return new Promise<{ exitCode: number }>((resolve, reject) => {
          const [cmd, ...rest] = args;
          const proc = spawn(cmd, rest, {
            cwd: opts.cwd ?? worktreePath,
            env: processEnv,
            stdio: [opts.stdin as any, opts.stdout as any, opts.stderr as any],
            // buildInteractiveArgs() passes a plain argv array (no shellEscape),
            // so the cmd.exe single-quote bug this file exists to fix doesn't
            // apply here. Left as upstream: shell:true resolves .cmd/.bat shims.
            shell: process.platform === 'win32',
          });
          proc.on('error', (error) => {
            reject(new Error(`exec failed: ${error.message}`));
          });
          proc.on('close', (code) => {
            resolve({ exitCode: code ?? 0 });
          });
        });
      },
      close: async () => {},
    };
    return handle;
  },
});
