import fs from 'node:fs/promises';
import path from 'node:path';
import { parseMarkModel } from './mark-model.mjs';
import { variants } from '../../brand/manifest.mjs';
import { composeWebSvg } from './emit-web-svg.mjs';
import { emitAndroidVector } from './emit-android-vector.mjs';
import { emitRaster } from './emit-raster.mjs';

async function writeOutput(packageDir, relPath, data) {
  const abs = path.join(packageDir, relPath);
  await fs.mkdir(path.dirname(abs), { recursive: true });
  await fs.writeFile(abs, data);
}

/**
 * Brand-asset generator. Derives every brand variant (web SVG, Android vector,
 * raster) from one base SVG + the manifest, resolving all colour from
 * `ctx.tokens`.
 */
export function brandGenerator() {
  return {
    name: 'brand',
    async build(ctx) {
      const model = await parseMarkModel(ctx.packageDir);

      for (const variant of variants) {
        let payload;
        switch (variant.target) {
          case 'web-svg':
            payload = composeWebSvg(model, variant, ctx.tokens);
            break;
          case 'android-vector':
            payload = emitAndroidVector(model, variant, ctx.tokens);
            break;
          case 'raster':
            payload = await emitRaster(model, variant, ctx.tokens);
            break;
          default:
            throw new Error(`unknown target '${variant.target}' for variant '${variant.name}'`);
        }

        for (const output of variant.outputs) {
          await writeOutput(ctx.packageDir, output, payload);
        }
        ctx.log(`${variant.name} → ${variant.outputs.join(', ')}`);
      }
    },
  };
}
