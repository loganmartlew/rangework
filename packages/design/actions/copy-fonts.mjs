import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const packageDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

export const copyFontsAction = {
  do: async (dictionary, config) => {
    const fontDest = path.resolve(packageDir, config.buildPath, config.options.fontDest);
    await fs.mkdir(fontDest, { recursive: true });

    const fontAssets = Object.values(dictionary.tokens.typography.fontAsset);
    await Promise.all(
      fontAssets.map(token => {
        const source = path.join(packageDir, 'assets', 'fonts', token.value);
        const dest = path.join(fontDest, token.value);
        return fs.copyFile(source, dest);
      }),
    );
  },

  undo: async (dictionary, config) => {
    const fontDest = path.resolve(packageDir, config.buildPath, config.options.fontDest);
    const fontAssets = Object.values(dictionary.tokens.typography.fontAsset);
    await Promise.all(
      fontAssets.map(token => fs.rm(path.join(fontDest, token.value), { force: true })),
    );
  },
};
