// @ts-check
import { defineConfig } from 'astro/config';
import svelte from '@astrojs/svelte';
import tailwindcss from '@tailwindcss/vite';

// https://astro.build/config
export default defineConfig({
  integrations: [svelte()],
  server: {
    port: parseInt(process.env.PORT || '4321'),
  },
  vite: {
    plugins: [tailwindcss()],
  },
});