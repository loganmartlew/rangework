import fs from 'node:fs/promises';
import StyleDictionary from 'style-dictionary';
import config from '../style-dictionary.config.mjs';

await fs.rm('dist', { recursive: true, force: true });
await fs.rm('generated', { recursive: true, force: true });

const sd = new StyleDictionary(config);
await sd.buildAllPlatforms();
