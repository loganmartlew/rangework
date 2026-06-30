import { resolveWebFill } from './colors.mjs';

const HEADER = '<!-- generated — do not edit. source: brand/mark.base.svg -->';

/**
 * Compose a web SVG string for a mark variant from the geometry model.
 *  - resolves each path's fill from its role's colour expression
 *  - wraps an optional background <rect> (square or rounded)
 *  - applies an optional crop by rewriting the viewBox
 *
 * @param {import('./mark-model.mjs').MarkModel} model
 * @param {object} variant
 * @param {import('../../scripts/tokens.mjs').ResolvedTokens} tokens
 * @param {{ includeHeader?: boolean }} [opts]
 */
export function composeWebSvg(model, variant, tokens, opts = {}) {
  const [, , vbW, vbH] = model.viewBox;
  const viewBox = (variant.crop ?? model.viewBox).join(' ');

  const fills = {
    band: resolveWebFill(variant.colors.band, tokens),
    rod: resolveWebFill(variant.colors.rod, tokens),
  };

  const lines = [];
  if (variant.background) {
    const fill = tokens.resolve(variant.background.token, variant.background.theme);
    const radius =
      variant.background.shape === 'rounded'
        ? ` rx="${variant.background.radius}" ry="${variant.background.radius}"`
        : '';
    lines.push(`  <rect x="0" y="0" width="${vbW}" height="${vbH}"${radius} fill="${fill}" />`);
  }

  lines.push(`  <g transform="${model.transform}">`);
  for (const p of model.paths) {
    const alpha = p.opacity < 1 ? ` fill-opacity="${p.opacity}"` : '';
    lines.push(`    <path fill="${fills[p.role]}"${alpha} d="${p.d}" />`);
  }
  lines.push('  </g>');

  const header = opts.includeHeader === false ? '' : `${HEADER}\n`;
  return `${header}<svg xmlns="http://www.w3.org/2000/svg" width="${vbW}" height="${vbH}" viewBox="${viewBox}">
${lines.join('\n')}
</svg>
`;
}
