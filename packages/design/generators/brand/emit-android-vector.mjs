import { resolveAndroidFill, formatAlpha } from './colors.mjs';

const ANDROID_NS = 'http://schemas.android.com/apk/res/android';
const HEADER = '<!-- generated — do not edit. source: brand/mark.base.svg -->';

/** Parse "translate(x,y)" into Android translateX/translateY group attributes. */
function transformToGroupAttrs(transform) {
  const match = /translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\s*\)/.exec(transform ?? '');
  if (!match) return '';
  const [, x, y] = match;
  const attrs = [];
  if (Number(x) !== 0) attrs.push(`android:translateX="${x}"`);
  if (Number(y) !== 0) attrs.push(`android:translateY="${y}"`);
  return attrs.length ? ` ${attrs.join(' ')}` : '';
}

/**
 * Emit an Android drawable XML for a variant. Handles both vector marks (built
 * from the geometry model) and Class 2 wrappers (<inset> / <color>) declared
 * directly in the manifest.
 *
 * @param {import('./mark-model.mjs').MarkModel} model
 * @param {object} variant
 * @param {import('../../scripts/tokens.mjs').ResolvedTokens} tokens
 */
export function emitAndroidVector(model, variant, tokens) {
  const head = `<?xml version="1.0" encoding="utf-8"?>\n${HEADER}\n`;

  if (variant.wrapper) {
    const w = variant.wrapper;
    if (w.type === 'inset') {
      return `${head}<inset xmlns:android="${ANDROID_NS}"
    android:drawable="@drawable/${w.drawable}"
    android:inset="${w.inset}" />
`;
    }
    if (w.type === 'color') {
      const color = tokens.resolve(w.token, w.theme);
      return `${head}<color xmlns:android="${ANDROID_NS}"
    android:color="${color}" />
`;
    }
    throw new Error(`unknown wrapper type: ${w.type}`);
  }

  const [, , vbW, vbH] = model.viewBox;
  const fills = {
    band: resolveAndroidFill(variant.colors.band, tokens),
    rod: resolveAndroidFill(variant.colors.rod, tokens),
  };
  const tint = variant.tint ? `\n  android:tint="@color/${variant.tint}"` : '';
  const groupAttrs = transformToGroupAttrs(model.transform);

  const paths = model.paths
    .map(p => {
      const alpha = p.opacity < 1 ? `\n      android:fillAlpha="${formatAlpha(p.opacity)}"` : '';
      return `    <path
      android:fillColor="${fills[p.role]}"${alpha}
      android:pathData="${p.d}" />`;
    })
    .join('\n');

  return `${head}<vector xmlns:android="${ANDROID_NS}"
  android:width="${vbW}dp"
  android:height="${vbH}dp"
  android:viewportWidth="${vbW}"
  android:viewportHeight="${vbH}"${tint}>
  <group${groupAttrs}>
${paths}
  </group>
</vector>
`;
}
