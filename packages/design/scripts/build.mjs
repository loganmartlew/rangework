import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import config from '../design.config.mjs';
import { resolveTokens } from './tokens.mjs';

/**
 * Design-asset build orchestrator.
 *
 * Resolves the design tokens once, builds a shared BuildContext, and runs each
 * registered generator (Style Dictionary tokens, brand assets, …) in order.
 *
 * @typedef {object} BuildContext
 * @property {string} packageDir  absolute package root
 * @property {string} outDir      generated/ (gitignored)
 * @property {string} distDir     dist/web/ (gitignored)
 * @property {import('./tokens.mjs').ResolvedTokens} tokens  parsed once, shared
 * @property {(msg: string) => void} log  namespaced per generator
 */

const packageDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

await fs.rm(path.join(packageDir, 'dist'), { recursive: true, force: true });
await fs.rm(path.join(packageDir, 'generated'), { recursive: true, force: true });

const tokens = await resolveTokens(packageDir);

const baseCtx = {
  packageDir,
  outDir: path.join(packageDir, 'generated'),
  distDir: path.join(packageDir, 'dist', 'web'),
  tokens,
};

for (const generator of config.generators) {
  const ctx = { ...baseCtx, log: msg => console.log(`  [${generator.name}] ${msg}`) };
  await generator.build(ctx);
}
