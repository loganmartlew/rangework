// @ts-check
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'astro/config';
import svelte from '@astrojs/svelte';
import tailwindcss from '@tailwindcss/vite';

// Brand assets are generated (gitignored) by @rangework/design. The site keeps
// zero committed brand footprint: this integration serves them from the package
// in dev and copies them into dist/ at build. The `predev` script guarantees
// they exist before `astro dev`; turbo's `^build` ordering covers builds.
const brandDir = fileURLToPath(
  new URL('../../packages/design/generated/brand', import.meta.url),
);
const brandAssets = ['favicon.svg', 'favicon.ico', 'rangework-mark.svg', 'rangework-mark-mono.svg', 'og-card.png'];
const contentTypes = { '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.png': 'image/png' };

/** @returns {import('astro').AstroIntegration} */
function brandAssets_() {
  return {
    name: 'rangework-brand-assets',
    hooks: {
      'astro:server:setup': ({ server }) => {
        server.middlewares.use((req, res, next) => {
          const url = (req.url ?? '').split('?')[0];
          const file = brandAssets.find(f => url === `/${f}`);
          if (!file) return next();
          const full = path.join(brandDir, file);
          if (!fs.existsSync(full)) return next();
          res.setHeader('Content-Type', contentTypes[path.extname(file)] ?? 'application/octet-stream');
          fs.createReadStream(full).pipe(res);
        });
      },
      'astro:build:done': ({ dir, logger }) => {
        const outDir = fileURLToPath(dir);
        for (const file of brandAssets) {
          fs.copyFileSync(path.join(brandDir, file), path.join(outDir, file));
        }
        logger.info(`copied ${brandAssets.length} brand assets into dist/`);
      },
    },
  };
}

// https://astro.build/config
export default defineConfig({
  integrations: [svelte(), brandAssets_()],
  server: {
    port: parseInt(process.env.PORT || '4321'),
  },
  vite: {
    plugins: [tailwindcss()],
  },
});
