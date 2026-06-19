# RWK35 — Product Website Requirements

## Overview

Implement the Rangework product website as a single-page landing site using the **Concept 02: Range Yardage Map** design direction. The site is an Astro project at `apps/site` using Tailwind 4, the `@rangework/ui-tokens` shared package, Material Symbols, and optionally `cva`. It must support light and dark modes (system preference with manual toggle).

---

## Design Direction

**Concept 02: Range Yardage Map** — The homepage is structured as an aerial diagram of a driving range. A sticky yardage rail on the left anchors section navigation (0Y → 250Y). Content scrolls in the main column. The yardage metaphor gives the page spatial structure while keeping the earthy, course-inspired Rangework brand.

Reference implementation: `design-docs/RWK35-product-website/design-spec/Concept 02 - Range Yardage Map.html`

---

## Sections & Content

The page follows the brand strategy messaging hierarchy (recognition → reframing → mechanism → proof → action), mapped to yardage markers:

### 1. Sticky Navigation Bar
- Brand mark + "Rangework" text logo (left)
- Anchor links: "How it works", "Sample session" (center)
- Dark/light mode toggle + "Get Rangework" CTA button (right)

### 2. Hero (0Y — Tee box)
- Subheading: "Where rounds are actually built" (mono, uppercase)
- Headline: "Aim every ball." (DM Sans light, ~84px desktop)
- Body: Concise value prop paragraph
- CTA: "Get Rangework" pill button + "Walk the range ↓" anchor link
- Phone mockup: Stylized device showing a live range session step (carry/tempo badges, ball count, complete button)

### 3. Reframe (50Y — The gap)
- Headline: "You've got the tools. You're missing the plan."
- Two-column body text: problem statement (left) and resolution (right)

### 4. How It Works (100Y — The system)
- Headline: "Build. Plan. Execute."
- Three cards with green top accent bar:
  - 01: Build a drill
  - 02: Plan a session
  - 03: Execute on the range
- Each card has: mono step number, title, body, dashed footer with example text

### 5. Features (150Y — Built for range)
- Headline: "Built for the range, not the round."
- 2×2 grid of feature cards with Material Symbol icons:
  - Ball count planning
  - Your bag, your rules
  - One step in focus
  - Never lose your place

### 6. Sample Session (200Y — Sample session)
- Headline: "A real session."
- Side-by-side: descriptive text + tags (left), session card (right)
- Session card: "Pre-round warm-up" with 4 drill rows (index, name, detail, ball count, time)
- Summary footer: total balls (60) + total time (~25 min)

### 7. CTA (250Y — Pin)
- Yardage badge: "250Y · Pin"
- Headline: "Stop winging it at the range."
- Body: "Build your first session in under two minutes. Free on Google Play."
- CTA: "Download Rangework" pill button + "Free · Google Play" label
- Link: Placeholder URL for now

---

## Yardage Rail (Desktop)

- Sticky sidebar on the left (~160px wide)
- Contains 6 yardage markers: 0Y (Tee box), 50Y (The gap), 100Y (The system), 150Y (Built for range), 200Y (Sample session), 250Y (Pin)
- Each marker: mono number in primary green + small uppercase label
- Markers separated by dashed horizontal borders
- Rail background subtly distinct from main content

---

## Responsive Behavior

### Desktop (≥1024px)
- Two-column layout: sticky yardage rail (left) + scrollable content (right)
- Hero: two-column grid (text + phone mockup)
- Full navigation bar with all elements visible

### Tablet (~768px–1023px)
- Yardage rail hidden; markers transform to horizontal section dividers within content
- Each section prefixed with its yardage marker as a horizontal badge/label
- Hero phone mockup may stack below text or be reduced in size
- Navigation may condense

### Mobile (<768px)
- Single column, no rail
- Yardage markers become horizontal section dividers (inline yardage badge + label above each section)
- Hero: stacked — text on top, phone mockup below (smaller)
- Feature grid: single column stack
- Sample session card: simplified layout, drill rows stack if needed
- Navigation: hamburger menu or simplified inline links
- CTA sections: full-width centered

---

## Dark / Light Mode

### Behavior
- Default to OS preference via `prefers-color-scheme`
- Manual toggle in the nav bar overrides system preference
- Toggle state persisted in `localStorage`
- Smooth transitions (background, border, text color) on mode change (~300ms)

### Token Requirements (ui-tokens changes)
The `@rangework/ui-tokens` package currently only exports dark-mode semantic tokens. It needs to support both themes:

- **Light mode semantic tokens** (default):
  - `--color-background`: neutral-50 (#FAFAF8)
  - `--color-onbackground`: neutral-900 (#161412)
  - `--color-surface`: neutral-100 (#F5F3EF)
  - `--color-onsurface`: neutral-800 (#282624)
  - `--color-primary`: primary-500 (#2D6A4F)
  - `--color-onprimary`: neutral-50 (#FAFAF8)
  - `--color-primarycontainer`: primary-100 (#DDEEE6)
  - `--color-onprimarycontainer`: primary-900 (#09160D)
  - (and all other semantic roles mapped to light values)

- **Dark mode semantic tokens** (current values, scoped under `[data-theme="dark"]` or `@media (prefers-color-scheme: dark)`):
  - Keep existing dark-mode mappings

- The tonal palette (primary-50 through primary-950, etc.) remains theme-independent.

### Visual Implementation
- Use the token set from the Concept 02 prototype as reference for light/dark custom properties
- The site may need a few additional CSS custom properties beyond what ui-tokens provides (e.g., `--hero-bg`, `--rail-bg`, `--card-border`, `--num`) — these should be defined in the site's own CSS, derived from the tonal palette tokens
- Brand mark SVG inverts in dark mode (filter or separate mono variant)

---

## Typography

Follow the design system rules strictly:
- **DM Sans** (300/400/500) for all UI text
- **DM Mono** (400/500) for numbers only: yardage markers, ball counts, step indices, time estimates, stat values
- Display/headline sizes use light weight (300)
- Titles and labels use medium weight (500)
- Section labels: mono, uppercase, letter-spacing ~0.14–0.18em
- No inline anonymous text styles — use Tailwind utilities mapped to the type scale

---

## Iconography

- Material Symbols Rounded (already available via `material-symbols` npm package)
- Brand mark SVG (`logo.svg` or equivalent from design-spec assets)
- No emoji anywhere on the page

---

## Animations & Motion

- Restrained Material 3 standard easing: `cubic-bezier(0.2, 0, 0, 1)`
- Short transitions: 150–250ms
- Dark/light mode transitions: ~300ms on background, color, border-color
- Scroll-triggered fade-in for content sections (subtle, using `IntersectionObserver`)
- Hover states: subtle brightness shift on filled surfaces, 8% primary tint on text/outlined buttons
- No bounces, no decorative looping animation

---

## Accessibility

- Semantic HTML: proper heading hierarchy (h1 → h2 → h3), nav, section, main landmarks
- All interactive elements keyboard-focusable with visible focus indicators
- Color contrast meets WCAG 2.1 AA for both light and dark modes
- `aria-label` on the theme toggle button
- Skip-to-content link
- Alt text on the brand mark; decorative phone mockup marked as `aria-hidden`
- Smooth scroll via CSS `scroll-behavior: smooth` with `prefers-reduced-motion` respect
- Animations disabled/reduced when `prefers-reduced-motion: reduce` is active

---

## SEO & Meta

- Page title: "Rangework — Practice with purpose"
- Meta description: Product-aligned summary
- Open Graph tags (title, description, image)
- Favicon: existing `favicon.svg` / `favicon.ico`
- Canonical URL (placeholder)
- `lang="en"` on html element

---

## Performance

- Static site generation (Astro default)
- Fonts loaded via `@font-face` with `font-display: swap` (already configured in ui-tokens)
- No client-side JavaScript framework — only vanilla JS for theme toggle and scroll animations
- Inline critical CSS where possible (Astro handles this)
- Optimized SVG assets

---

## Assets Required

- Brand mark SVG (two-color + mono variants) — available in design-spec `assets/`
- Phone mockup: built with HTML/CSS (not an image), matching the Concept 02 prototype
- Material Symbols Rounded icons: `calculate`, `golf_course`, `linear_scale`, `cloud_done`, `arrow_downward`, `arrow_forward`

---

## Out of Scope

- Additional pages (about, blog, docs, privacy policy, terms)
- Contact forms or email capture
- Analytics integration
- CMS or dynamic content
- App store badge images (just text CTA link for now)
- Backend or API integration

---

## Technical Stack

| Layer | Tool |
|---|---|
| Framework | Astro 6.x (static) |
| Styling | Tailwind CSS 4 |
| Design tokens | `@rangework/ui-tokens` (workspace package) |
| Icons | `material-symbols` (npm) |
| Variants | `class-variance-authority` (if needed) |
| JS | Vanilla (theme toggle, scroll observers) |

---

## Deliverables

1. Updated `@rangework/ui-tokens` with light/dark semantic token support
2. Fully implemented single-page landing site at `apps/site`
3. Responsive across desktop, tablet, and mobile breakpoints
4. Light and dark mode with system detection + manual toggle
5. All sections from the Concept 02 design faithfully implemented
