import fs from 'node:fs/promises';
import path from 'node:path';

/**
 * Resolve the design tokens once, up front, so every generator shares a single
 * parsed source of truth. The brand generator reads colours from here
 * (e.g. `tokens.color.brandRod.light`) — it never re-parses JSON or hardcodes hex.
 *
 * @typedef {{ light: string, dark: string }} ThemedColor
 * @typedef {object} ResolvedTokens
 * @property {{ color: object, typography: object }} raw  Parsed token trees.
 * @property {{ brandRod: ThemedColor }} color  Convenience colour accessors.
 * @property {(tokenPath: string, theme?: 'light' | 'dark') => string} resolve
 */

/**
 * @param {string} packageDir absolute package root
 * @returns {Promise<ResolvedTokens>}
 */
export async function resolveTokens(packageDir) {
  const tokensDir = path.join(packageDir, 'tokens');
  const color = JSON.parse(
    await fs.readFile(path.join(tokensDir, 'color.tokens.json'), 'utf8'),
  ).color;
  const typography = JSON.parse(
    await fs.readFile(path.join(tokensDir, 'typography.tokens.json'), 'utf8'),
  ).typography;

  const raw = { color, typography };

  /** Resolve `{color.palette.x.y.value}` references to a literal value. */
  const deref = value => {
    let v = value;
    while (typeof v === 'string' && v.startsWith('{') && v.endsWith('}')) {
      const refPath = v.slice(1, -1).replace(/\.value$/, '').split('.');
      v = refPath.reduce((node, key) => node?.[key], { color, typography })?.value;
    }
    return v;
  };

  /**
   * Resolve a colour token reference to a hex string.
   *  - `'neutral.80'`        → palette group + shade (theme-independent)
   *  - `'neutralVariant.30'` → palette group + shade
   *  - `'brandRod'`          → scheme colour for the given theme
   */
  const resolve = (tokenPath, theme) => {
    const parts = tokenPath.split('.');
    if (parts.length === 2) {
      const [group, shade] = parts;
      const token = color.palette?.[group]?.[shade];
      if (!token) throw new Error(`Unknown palette token: ${tokenPath}`);
      return deref(token.value);
    }
    if (!theme) {
      throw new Error(`Scheme token '${tokenPath}' requires a theme (light|dark)`);
    }
    const token = color.scheme?.[theme]?.[parts[0]];
    if (!token) throw new Error(`Unknown scheme token: ${tokenPath} (${theme})`);
    return deref(token.value);
  };

  return {
    raw,
    color: {
      brandRod: { light: resolve('brandRod', 'light'), dark: resolve('brandRod', 'dark') },
    },
    resolve,
  };
}
