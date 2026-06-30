// @ts-check
import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';
import astro from 'eslint-plugin-astro';
import tailwind from 'eslint-plugin-tailwindcss';

export default tseslint.config(
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  ...astro.configs.recommended,
  {
    files: ['**/*.astro'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
        extraFileExtensions: ['.astro'],
      },
    },
  },
  {
    // Node-run config files (astro.config.mjs, etc.) use Node globals.
    files: ['**/*.config.{js,mjs,cjs}'],
    languageOptions: {
      globals: {
        process: 'readonly',
        URL: 'readonly',
        console: 'readonly',
        Buffer: 'readonly',
        __dirname: 'readonly',
        __filename: 'readonly',
      },
    },
  },
  {
    plugins: { tailwindcss: tailwind },
    settings: {
      tailwindcss: {
        cssConfigPath: 'src/styles/global.css',
      },
    },
    rules: {
      'tailwindcss/no-unnecessary-arbitrary-value': 'warn',
    },
  },
  {
    ignores: ['dist/', '.astro/'],
  }
);
