# Site instructions

`apps/site` is the marketing and support website for Rangework. Deployed as a static site.

## Stack

- **Astro** — static site generator (SSG) with island architecture
- **Svelte 5** — interactive islands (OAuth consent flow)
- **Tailwind CSS v4** — utility-first styling via Vite plugin
- **`@rangework/design`** — shared design tokens (colors, typography scales) and brand assets from `packages/design`
- **`@supabase/supabase-js`** — Supabase client for the OAuth consent page

## File map

- `apps/site/src/pages/` — route pages:
  - `index.astro` — marketing homepage
  - `ai-planning.astro` — AI planning feature page
  - `oauth/consent.astro` — OAuth consent page (uses Svelte island)
  - `delete-account.astro` — account deletion page
  - `privacy-policy.md`, `terms-of-use.md`, `cookie-policy.md` — legal pages (Markdown)
  - `404.astro`, `500.astro` — error pages
- `apps/site/src/components/` — Astro components (sections, UI elements):
  - `HeroSection.astro`, `FeaturesSection.astro`, `HowItWorksSection.astro`, etc. — marketing page sections
  - `oauth/OAuthConsent.svelte` — Svelte island for the OAuth consent flow
  - `oauth/consent-logic.ts` — consent flow logic (token exchange, redirect)
  - `oauth/types.ts` — consent flow TypeScript types
- `apps/site/src/layouts/` — `Layout.astro` (full), `MarkdownLayout.astro` (legal pages), `MinimalLayout.astro`.
- `apps/site/src/lib/` — `links.ts` (external URL constants), `supabase-client.ts` (browser Supabase client), `ui.ts` (CVA helpers).
- `apps/site/src/styles/global.css` — Tailwind v4 config and CSS custom properties.
- `apps/site/astro.config.mjs` — Astro config (Svelte integration, Tailwind Vite plugin, port).

## Commands

```powershell
pnpm --filter @rangework/site dev     # dev server at http://localhost:4321
pnpm --filter @rangework/site build   # production build to apps/site/dist/
pnpm --filter @rangework/site lint    # ESLint (astro + tailwindcss plugins)
```

## Conventions

- Prefer Astro components for static content; use Svelte islands only when interactivity is required (currently only the OAuth consent flow).
- Keep Supabase client usage in `src/lib/supabase-client.ts`; do not create ad-hoc client instances in components.
- External links and app deep-links are centralized in `src/lib/links.ts`.
- Legal pages are Markdown files rendered via `MarkdownLayout.astro` — no custom components needed.
- Design tokens from `packages/design` are imported in `global.css`; do not hardcode brand colors or type scales inline.
