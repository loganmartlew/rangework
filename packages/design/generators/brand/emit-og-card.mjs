import path from 'node:path';
import { Resvg } from '@resvg/resvg-js';
import { resolveWebFill } from './colors.mjs';

// Standard OG/Twitter card size. Content is centered within it (rather than
// left-anchored) because some surfaces (iMessage, WhatsApp) crop to a
// near-square region instead of showing the full 1.91:1 image.
const CARD_WIDTH = 1200;
const CARD_HEIGHT = 630;
const MARK_SIZE = 220;
const MARK_X = 265;
const TEXT_GAP = 64;

function composeMarkFragment(model, variant, tokens) {
  const fills = {
    band: resolveWebFill(variant.colors.band, tokens),
    rod: resolveWebFill(variant.colors.rod, tokens),
  };
  const [, , vbW, vbH] = model.viewBox;
  const paths = model.paths
    .map(p => {
      const alpha = p.opacity < 1 ? ` fill-opacity="${p.opacity}"` : '';
      return `<path fill="${fills[p.role]}"${alpha} d="${p.d}" />`;
    })
    .join('\n      ');
  return { viewBox: `0 0 ${vbW} ${vbH}`, transform: model.transform, paths };
}

/** Compose the 1200×630 OG-card SVG: card background, brand mark, and title/subtitle text. */
export function composeOgCardSvg(model, variant, tokens) {
  const background = tokens.resolve(variant.background.token, variant.background.theme);
  const mark = composeMarkFragment(model, variant, tokens);
  const markY = (CARD_HEIGHT - MARK_SIZE) / 2;
  const textX = MARK_X + MARK_SIZE + TEXT_GAP;
  const { title, subtitle } = variant.text;
  const titleColor = tokens.resolve(title.color.token, title.color.theme);
  const subtitleColor = tokens.resolve(subtitle.color.token, subtitle.color.theme);

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${CARD_WIDTH}" height="${CARD_HEIGHT}" viewBox="0 0 ${CARD_WIDTH} ${CARD_HEIGHT}">
  <rect x="0" y="0" width="${CARD_WIDTH}" height="${CARD_HEIGHT}" fill="${background}" />
  <svg x="${MARK_X}" y="${markY}" width="${MARK_SIZE}" height="${MARK_SIZE}" viewBox="${mark.viewBox}">
    <g transform="${mark.transform}">
      ${mark.paths}
    </g>
  </svg>
  <text x="${textX}" y="${CARD_HEIGHT / 2 - 22}" font-family="${title.fontFamily}" font-size="${title.fontSize}" fill="${titleColor}">${title.text}</text>
  <text x="${textX}" y="${CARD_HEIGHT / 2 + 44}" font-family="${subtitle.fontFamily}" font-size="${subtitle.fontSize}" letter-spacing="${subtitle.letterSpacing ?? 0}" fill="${subtitleColor}">${subtitle.text}</text>
</svg>
`;
}

/**
 * Rasterise the OG-card SVG to a 1200×630 PNG.
 * Loads every font in assets/fonts by directory — resvg's fontdb matches on
 * each TTF's own name-table family (e.g. "DM Sans Medium"), not the CSS
 * @font-face alias used on the web, so `text.*.fontFamily` in the manifest
 * must reference those literal family names.
 */
export async function emitOgCard(model, variant, tokens, packageDir) {
  const svg = composeOgCardSvg(model, variant, tokens);
  const fontsDir = path.join(packageDir, 'assets', 'fonts');
  const resvg = new Resvg(svg, { font: { loadSystemFonts: false, fontDirs: [fontsDir] } });
  return Buffer.from(resvg.render().asPng());
}
