import rangeworkTailwindPreset from '@rangework/ui-tokens/tailwind-preset';

/** @type {import('tailwindcss').Config} */
export default {
  presets: [rangeworkTailwindPreset],
  content: [
    './src/**/*.{astro,html,js,jsx,md,mdx,pure,runtime,svelte,ts,tsx,vue}',
  ],
  plugins: [],
};
