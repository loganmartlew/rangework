import { Resvg } from '@resvg/resvg-js';
import pngToIco from 'png-to-ico';
import { composeWebSvg } from './emit-web-svg.mjs';

/** Render a composed mark SVG to a PNG buffer at an exact pixel width. */
function renderPng(svg, size) {
  const resvg = new Resvg(svg, { fitTo: { mode: 'width', value: size } });
  return Buffer.from(resvg.render().asPng());
}

/**
 * Produce a raster artifact (favicon .ico or Play .png) for a variant.
 * The variant is composed to an SVG (full mark + background, no crop) and
 * rasterised via resvg.
 *
 * @returns {Promise<Buffer>}
 */
export async function emitRaster(model, variant, tokens) {
  const svg = composeWebSvg(model, variant, tokens, { includeHeader: false });
  const { format, size, sizes } = variant.raster;

  if (format === 'ico') {
    const pngs = sizes.map(s => renderPng(svg, s));
    return pngToIco(pngs);
  }
  if (format === 'png') {
    return renderPng(svg, size);
  }
  throw new Error(`unknown raster format: ${format}`);
}
