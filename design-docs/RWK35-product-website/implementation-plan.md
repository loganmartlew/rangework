# RWK35 — Implementation Plan

## 1. Prerequisites: ui-tokens Light/Dark Support

### Problem

`packages/ui-tokens/scripts/build.mjs` currently calls `buildTailwindThemeVariablesForSchemeColors(tokens)` which only outputs `tokens.color.scheme.dark` (line 281). The result is that `tailwind-theme.css` only has dark-mode semantic color values. It also omits surface container tokens and the `brand-rod` accent color.

### Solution

Modify `buildTailwindThemeFile()` in `build.mjs` to output:

1. **`@theme` block** — palette colors (unchanged) + fonts (unchanged) + **light** semantic scheme colors as defaults. This registers Tailwind utility names and sets the light-mode values.

2. **`@media (prefers-color-scheme: dark)` block** — dark semantic scheme colors scoped under `:root:not([data-theme="light"])`. This activates when the OS prefers dark and no explicit light override is set.

3. **`[data-theme="dark"]` block** — same dark semantic scheme colors. This activates when the user explicitly chooses dark mode via the toggle.

4. **Surface container tokens** — output `surface.light.*` and `surface.dark.*` as semantic `--color-surface-*` variables in the corresponding light/dark scopes.

5. **Brand-rod accent** — add `--color-brandrod` to `color.tokens.json` (light: `#386044`, dark: `#93C8A8`) and include in both scheme outputs.

### Resulting tailwind-theme.css structure

```css
@theme {
  /* ── Palette (static, theme-independent) ── */
  --color-primary-50: #F0FAF5;
  --color-primary-100: #DDEEE6;
  /* ... all palette stops ... */
  --color-neutral-50: #FAFAF8;
  /* ... */

  /* ── Semantic scheme (light defaults) ── */
  --color-primary: #2D6A4F;
  --color-onprimary: #FAFAF8;
  --color-primarycontainer: #B8DCC8;
  --color-onprimarycontainer: #09160D;
  --color-background: #FAFAF8;
  --color-onbackground: #161412;
  --color-surface: #F5F3EF;
  --color-onsurface: #282624;
  /* ... all semantic roles ... */
  --color-surfacebright: #F9F7F3;
  --color-surfacelowest: #F7F5F1;
  --color-surfacelow: #F5F3EF;
  --color-surfacedefault: #EFEDEA;
  --color-surfacehigh: #EAE8E4;
  --color-surfacehighest: #E4E2DE;
  --color-surfacedim: #D5D3CF;
  --color-brandrod: #386044;

  /* ── Fonts ── */
  --font-sans: "dm_sans", ui-sans-serif, system-ui, sans-serif;
  --font-mono: "dm_mono", ui-monospace, SFMono-Regular, monospace;
}

/* OS dark preference (no explicit user override) */
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --color-primary: #8FC8A8;
    --color-onprimary: #09160D;
    --color-background: #161412;
    --color-onbackground: #EAE8E4;
    /* ... all dark semantic roles ... */
    --color-surfacebright: #363432;
    --color-surfacelowest: #161412;
    /* ... all dark surface containers ... */
    --color-brandrod: #93C8A8;
  }
}

/* Explicit dark mode toggle */
[data-theme="dark"] {
  --color-primary: #8FC8A8;
  /* ... identical to above ... */
}
```

### Files changed

| File | Change |
|---|---|
| `packages/ui-tokens/tokens/color.tokens.json` | Add `brandRod` light/dark entries to `scheme.light` and `scheme.dark` |
| `packages/ui-tokens/scripts/build.mjs` | Rewrite `buildTailwindThemeFile()` to output light defaults + dark overrides + surface containers |

### Impact on existing consumers

- **Android app**: No change — the Kotlin/XML generators already use both `scheme.light` and `scheme.dark` separately.
- **Site**: Gets proper light/dark support.
- The palette variables and font families are identical before and after.

---

## 2. Site File Structure

Replace the current placeholder content. Final structure:

```
apps/site/
├── public/
│   ├── favicon.svg
│   ├── favicon.ico
│   └── rangework-mark.svg          # two-color brand mark
│   └── rangework-mark-mono.svg     # mono brand mark (for dark mode)
├── src/
│   ├── assets/
│   │   └── logo.svg                # existing (keep or replace)
│   ├── components/
│   │   ├── Nav.astro               # sticky navigation bar
│   │   ├── ThemeToggle.astro       # dark/light toggle (client JS)
│   │   ├── YardageRail.astro       # desktop sticky sidebar
│   │   ├── YardageMarker.astro     # inline marker for mobile
│   │   ├── PhoneMockup.astro       # CSS phone with live session screen
│   │   ├── HeroSection.astro
│   │   ├── ReframeSection.astro
│   │   ├── HowItWorksSection.astro
│   │   ├── FeaturesSection.astro
│   │   ├── SampleSessionSection.astro
│   │   ├── CtaSection.astro
│   │   └── Footer.astro            # minimal footer (copyright, links)
│   ├── layouts/
│   │   └── Layout.astro            # updated <head>, meta, dark mode script
│   ├── pages/
│   │   └── index.astro             # composes all sections
│   └── styles/
│       └── global.css              # tailwind imports + site-specific tokens
├── astro.config.mjs
├── package.json
└── tsconfig.json
```

### Files to remove
- `src/components/Header.astro` (replaced by Nav.astro)
- `src/components/TextLogo.astro` (logo markup moves inline into Nav)
- `src/components/Button.astro` (buttons are simple enough to be inline Tailwind; cva not needed for this page)
- `src/assets/session-card.svg` (replaced by HTML/CSS phone mockup)

---

## 3. Component Breakdown

### 3.1 Layout.astro

**Purpose**: HTML shell with `<head>` metadata, dark mode initialization, global styles.

**Key details**:
- `<html lang="en">` with no initial `data-theme` (system preference is the default)
- Inline `<script>` in `<head>` (blocking) that reads `localStorage.getItem('theme')` and sets `data-theme` before paint to prevent FOUC
- Meta tags: title, description, OG tags, favicon
- Imports `global.css`
- `<slot />` for page content

**Dark mode init script (inline, blocking)**:
```js
(function() {
  const saved = localStorage.getItem('theme');
  if (saved) {
    document.documentElement.setAttribute('data-theme', saved);
  }
})();
```

This runs before first paint. If no saved preference, `data-theme` is absent and `prefers-color-scheme` media query controls the theme.

### 3.2 Nav.astro

**Purpose**: Sticky top navigation bar.

**Layout**:
```
[Brand mark + "Rangework"] ---- [How it works | Sample session] ---- [ThemeToggle] [Get Rangework btn]
```

**Behavior**:
- `position: sticky; top: 0; z-index: 50`
- Background: `bg-neutral-50 dark:bg-neutral-900` (or semantic surface token)
- Bottom border: 1px outline-variant
- Anchor links use `href="#how-it-works"` and `href="#sample-session"`
- Brand mark: `<img>` of the SVG, with a CSS class that applies `filter: brightness(0) invert(1)` in dark mode
- "Get Rangework" button: filled pill, primary color

**Mobile**: Simplified — brand mark + CTA button. Nav links hidden or in a small dropdown. ThemeToggle stays visible.

### 3.3 ThemeToggle.astro

**Purpose**: Toggle button for dark/light mode.

**Visual**: Pill-shaped button with a small circular knob containing a Material Symbol icon (`light_mode` / `dark_mode`) and a text label ("Light" / "Dark").

**Implementation**: 
- Rendered as a `<button>` with `aria-label="Toggle dark mode"`
- Client-side `<script>` (Astro inline script with `is:inline` or a `<script>` tag):
  1. On click, toggle `data-theme` between `"light"` and `"dark"`
  2. If removing an override (back to system), remove `data-theme` and clear localStorage
  3. Actually: toggle cycles: current → opposite. Store in localStorage.
  4. Update icon and label text

**Logic**:
```js
function getEffectiveTheme() {
  const explicit = document.documentElement.getAttribute('data-theme');
  if (explicit) return explicit;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function toggleTheme() {
  const current = getEffectiveTheme();
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
  updateToggleUI(next);
}
```

### 3.4 YardageRail.astro

**Purpose**: Desktop-only sticky sidebar with yardage markers.

**Layout**:
- Fixed width: `w-40` (160px)
- `position: sticky; top: 64px` (below nav)
- Full viewport height: `min-h-screen`
- Background: slightly different surface tone (use `surfacelow` or a site-specific token)
- Right border: 1px dashed outline-variant
- Contains 6 marker slots, each with:
  - Mono number in primary green (e.g., "0Y")
  - Small uppercase mono label (e.g., "Tee box")
  - Top dashed border separator

**Responsive**: Hidden below `lg` breakpoint (1024px). Use `hidden lg:flex lg:flex-col`.

### 3.5 YardageMarker.astro

**Purpose**: Inline yardage marker for tablet/mobile (replaces rail).

**Props**: `yard: string` (e.g., "0Y"), `label: string` (e.g., "Tee box")

**Layout**: Horizontal badge above each section:
```
[0Y] · Tee box
```
Mono font, primary green number, muted label. Small padding, left-aligned.

**Responsive**: Visible only below `lg` breakpoint. Use `lg:hidden`.

### 3.6 PhoneMockup.astro

**Purpose**: Styled phone device showing a live range session step.

**Construction**: Pure HTML/CSS (no image). Based on the Concept 02 prototype.

**Structure**:
```
┌─────────────────────────┐  ← dark shell (rounded-[36px], padding 9px)
│ ┌─────────────────────┐ │  ← inner screen (rounded-[28px], surface bg)
│ │ 9:41          ●●●   │ │  ← status bar (mono, 10px)
│ │                     │ │
│ │ Pre-round · Step 02 │ │  ← session label (mono, uppercase)
│ │ 7-iron stock shot   │ │  ← step title
│ │ ┌─────┐ ┌─────┐   │ │  ← carry/tempo badges (2-col grid)
│ │ │Carry│ │Tempo│   │ │
│ │ │150Y │ │3:1  │   │ │
│ │ └─────┘ └─────┘   │ │
│ │ Same target...      │ │  ← instruction text
│ │                     │ │
│ │ 15        [Complete]│ │  ← ball count + CTA
│ │ Balls               │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

**Sizing**:
- Desktop: `w-[260px]` shell, `h-[520px]` screen
- Mobile: scaled down to `w-[220px]` or similar

**Dark mode**: Shell background stays dark in both modes. Screen interior uses surface tokens for its content area.

### 3.7 HeroSection.astro

**Purpose**: 0Y section — primary hero with headline + phone mockup.

**Desktop layout**: Two-column grid (`grid-cols-[1fr_300px]`), content left, phone right.

**Content**:
- Eyebrow: "Where rounds are actually built" — mono, uppercase, muted
- Headline: "Aim every ball." — DM Sans 300, ~84px, tight leading
- Body: Value prop paragraph
- CTAs: "Get Rangework" pill button + "Walk the range ↓" text link with `arrow_downward` icon

**Background**: Subtle sage wash (site-specific `--hero-bg` token). In dark mode, a muted dark surface.

**Mobile**: Stacked — text block, then phone mockup centered below. Headline scales down to ~48px.

### 3.8 ReframeSection.astro

**Purpose**: 50Y section — the problem reframe.

**Layout**: 
- Headline: "You've got the tools. You're missing the plan." — DM Sans 300, ~56px
- Two-column body text: problem (left), resolution (right)
- Mobile: single column, stacked paragraphs

### 3.9 HowItWorksSection.astro

**Purpose**: 100Y section — the three-step system.

**Layout**:
- Headline: "Build. Plan. Execute."
- Three equal cards in a row (`grid-cols-3`):
  - Each card: green top accent bar (4px, absolute positioned), mono step number (32px), title, body, dashed footer with example
- Mobile: single column stack

**Card structure**:
```html
<div class="relative overflow-hidden rounded-xl border ...">
  <div class="absolute top-0 inset-x-0 h-1 bg-primary"></div>
  <div class="p-8">
    <span class="font-mono text-3xl font-medium text-brandrod">01</span>
    <h3 class="text-xl font-medium mt-5 mb-3">Build a drill</h3>
    <p class="text-sm">...</p>
    <div class="border-t border-dashed mt-5 pt-4">
      <span class="font-mono text-xs text-neutral-600">e.g. ...</span>
    </div>
  </div>
</div>
```

### 3.10 FeaturesSection.astro

**Purpose**: 150Y section — feature highlights.

**Layout**:
- Headline: "Built for the range, not the round."
- 2×2 card grid, each with: Material Symbol icon (28px, primary) + title + body
- Mobile: single column stack

**Icons**: `calculate`, `golf_course`, `linear_scale`, `cloud_done`

### 3.11 SampleSessionSection.astro

**Purpose**: 200Y section — sample session showcase.

**Layout**: Two-column grid:
- Left: Headline "A real session.", body text, tag pills (4 drills / 60 balls / ~25 minutes)
- Right: Session card with drill list

**Session card structure**:
```
┌─────────────────────────────────────┐
│ SESSION                             │  ← mono label
│ Pre-round warm-up                   │  ← title-large
├─────────────────────────────────────┤
│ 01  Wedge distance ladder   15 balls│
│     PW · 56° · 3 distances    ~6min│
├─────────────────────────────────────┤
│ 02  7-iron stock shot       15 balls│
│     7-iron · same target       ~6min│
├─────────────────────────────────────┤
│ 03  Driver rhythm swings    15 balls│
│     Driver · 80% effort       ~7min│
├─────────────────────────────────────┤
│ 04  60-yard flop to a pin   15 balls│
│     60° · one specific flag    ~6min│
├─────────────────────────────────────┤
│                          60  ~25    │  ← summary footer
│                    Total balls  Min │
└─────────────────────────────────────┘
```

Each drill row: 4-column grid (index, name+detail, ball count, time).

**Mobile**: Stacked — text block on top, card below full width. Drill rows may simplify to 2-column (name, balls).

### 3.12 CtaSection.astro

**Purpose**: 250Y section — closing CTA.

**Layout**:
- Yardage badge: "250Y · Pin" in mono
- Headline: "Stop winging it at the range." — DM Sans 300, ~64px
- Body: "Build your first session in under two minutes. Free on Google Play."
- CTA: "Download Rangework" large pill button
- "Free · Google Play" mono label

**Background**: Subtle gradient or distinct surface tone (site-specific `--pin-bg` token).

### 3.13 Footer.astro

**Purpose**: Minimal footer.

**Content**: "© 2026 Rangework" + possible links (placeholder). Mono text, muted color.

---

## 4. Styling Strategy

### 4.1 global.css

```css
@import 'tailwindcss';
@import '@rangework/ui-tokens/tailwind-theme.css';
@import '@rangework/ui-tokens/fonts.css';
@import 'material-symbols';

/* ── Site-specific semantic tokens ── */
:root {
  --hero-bg: #EBF3EE;
  --rail-bg: #ECEAE6;
  --border-dash: #CAC8C3;
  --tag-bg: rgba(45, 106, 79, 0.09);
  --tag-on: var(--color-primary);
  --badge-bg: #E6F0EA;
  --badge-on: var(--color-primary);
  --pin-bg: linear-gradient(160deg, #EBF3EE 0%, #DAE9E1 100%);
}

[data-theme="dark"],
:root:is([data-theme="dark"]) {
  --hero-bg: #1E1C1A;
  --rail-bg: #191714;
  --border-dash: #383531;
  --tag-bg: rgba(107, 191, 143, 0.12);
  --tag-on: var(--color-primary);
  --badge-bg: #252320;
  --badge-on: var(--color-primary);
  --pin-bg: linear-gradient(160deg, #1E1C1A 0%, #141412 100%);
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --hero-bg: #1E1C1A;
    --rail-bg: #191714;
    --border-dash: #383531;
    --tag-bg: rgba(107, 191, 143, 0.12);
    --tag-on: var(--color-primary);
    --badge-bg: #252320;
    --badge-on: var(--color-primary);
    --pin-bg: linear-gradient(160deg, #1E1C1A 0%, #141412 100%);
  }
}
```

### 4.2 Tailwind utility approach

Use Tailwind utilities for:
- Layout: `grid`, `flex`, `gap-*`, `p-*`, `max-w-*`
- Typography: `font-sans`, `font-mono`, `text-*`, `font-light`, `font-medium`, `tracking-*`, `leading-*`, `uppercase`
- Colors: `text-primary`, `bg-background`, `text-onsurface`, `border-outlinevariant`, `text-brandrod`
- Spacing: Standard Tailwind scale
- Borders: `border`, `border-dashed`, `rounded-*`
- Responsive: `sm:`, `md:`, `lg:`, `xl:` prefixes

For one-off values not in the Tailwind scale (e.g., 84px headline), use arbitrary values: `text-[84px]`, `leading-[0.96]`.

### 4.3 Transitions

Add a utility layer in `global.css`:

```css
@layer utilities {
  .theme-transition {
    transition: background-color 300ms, color 300ms, border-color 300ms;
  }
}
```

Apply `theme-transition` to major containers (nav, sections, cards, rail).

### 4.4 Scroll animations

Use vanilla JS `IntersectionObserver` for fade-in-on-scroll:

```css
@layer utilities {
  .animate-on-scroll {
    opacity: 0;
    transform: translateY(20px);
    transition: opacity 600ms var(--ease-standard, cubic-bezier(0.2, 0, 0, 1)),
                transform 600ms var(--ease-standard, cubic-bezier(0.2, 0, 0, 1));
  }
  .animate-on-scroll.is-visible {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .animate-on-scroll {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
```

Observer script (in Layout.astro, deferred):
```js
const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.1 }
);

document.querySelectorAll('.animate-on-scroll').forEach((el) => {
  observer.observe(el);
});
```

---

## 5. Responsive Breakpoints

| Breakpoint | Width | Layout |
|---|---|---|
| Mobile | < 768px | Single column, inline yardage markers, stacked hero, hamburger nav |
| Tablet | 768px–1023px | Single column, inline yardage markers, wider content |
| Desktop | ≥ 1024px | Two-column (rail + content), full nav, side-by-side hero |

**Tailwind breakpoints**: `md:` (768px), `lg:` (1024px), `xl:` (1280px)

- Yardage rail: `hidden lg:sticky lg:flex`
- Inline markers: `flex lg:hidden`
- Hero grid: `flex flex-col lg:grid lg:grid-cols-[1fr_300px]`
- How-it-works cards: `grid grid-cols-1 md:grid-cols-3`
- Feature cards: `grid grid-cols-1 md:grid-cols-2`
- Sample session: `flex flex-col lg:grid lg:grid-cols-[320px_1fr]`
- Nav links: `hidden md:flex`

---

## 6. Accessibility Checklist

| Requirement | Implementation |
|---|---|
| Skip-to-content | `<a href="#main-content" class="sr-only focus:not-sr-only ...">Skip to content</a>` at top of body |
| Heading hierarchy | h1 (hero) → h2 (each section) → h3 (card titles) |
| Landmarks | `<nav>`, `<main id="main-content">`, `<footer>`, `<section>` with `aria-labelledby` |
| Theme toggle | `<button aria-label="Switch to dark mode">` (dynamic label) |
| Keyboard focus | Tailwind `focus-visible:ring-2 focus-visible:ring-primary` on interactive elements |
| Color contrast | WCAG AA — verified for both light (#161412 on #FAFAF8 = 16.5:1) and dark (#E8E6E2 on #141412 = 14.8:1) |
| Reduced motion | `@media (prefers-reduced-motion: reduce)` disables scroll animations |
| Phone mockup | `aria-hidden="true"` on the decorative mockup |
| Smooth scroll | `scroll-behavior: smooth` on `html`, honoring `prefers-reduced-motion` |
| Image alts | Brand mark: `alt="Rangework"`, decorative elements: `alt=""` |

---

## 7. SEO & Meta

In `Layout.astro` `<head>`:

```html
<title>Rangework — Practice with purpose</title>
<meta name="description" content="Build drills, plan sessions, execute at the range. Rangework is the practice planning app for golfers who want every ball to count.">
<meta property="og:title" content="Rangework — Practice with purpose">
<meta property="og:description" content="Build drills, plan sessions, execute at the range.">
<meta property="og:type" content="website">
<meta property="og:image" content="/og-image.png"> <!-- placeholder -->
<link rel="canonical" href="https://rangework.app"> <!-- placeholder -->
```

---

## 8. Implementation Order

### Phase 1: Token foundation
1. Add `brandRod` to `color.tokens.json` (light + dark scheme entries)
2. Modify `build.mjs` → `buildTailwindThemeFile()` to output light defaults + dark overrides + surface containers
3. Run `pnpm --filter @rangework/ui-tokens build` and verify output
4. Verify Android build is unaffected (Kotlin/XML generators already use both schemes)

### Phase 2: Site scaffold
5. Update `Layout.astro` with meta, dark mode init script, scroll observer
6. Update `global.css` with site-specific tokens and utility classes
7. Build `Nav.astro` + `ThemeToggle.astro`
8. Build `YardageRail.astro` + `YardageMarker.astro`
9. Wire up `index.astro` with the two-column layout shell

### Phase 3: Content sections (top to bottom)
10. `HeroSection.astro` + `PhoneMockup.astro`
11. `ReframeSection.astro`
12. `HowItWorksSection.astro`
13. `FeaturesSection.astro`
14. `SampleSessionSection.astro`
15. `CtaSection.astro` + `Footer.astro`

### Phase 4: Polish
16. Scroll animations (IntersectionObserver)
17. Responsive QA across breakpoints
18. Dark mode QA
19. Accessibility audit (heading order, focus, contrast, reduced motion)
20. Copy brand mark SVGs to `public/`

---

## 9. Asset Handling

| Asset | Source | Destination |
|---|---|---|
| `rangework-mark.svg` | `design-docs/.../design-spec/assets/` | `apps/site/public/rangework-mark.svg` |
| `rangework-mark-mono.svg` | `design-docs/.../design-spec/assets/` | `apps/site/public/rangework-mark-mono.svg` |
| Fonts (DM Sans, DM Mono) | Already handled by `@rangework/ui-tokens` | No action needed |
| Material Symbols | Already via npm `material-symbols` | No action needed |
| favicon | Already in `public/` | No action needed |

The brand mark in the nav uses the two-color version in light mode and needs either:
- The mono SVG with `currentColor` in dark mode, OR
- A CSS `filter: brightness(0) invert(1)` on the two-color SVG in dark mode

The Concept 02 prototype uses the filter approach. We'll follow that.

---

## 10. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Tailwind 4 `@theme` variables vs runtime overrides — dark mode variables might not cascade correctly | Test early in Phase 1. Tailwind 4's `@theme` compiles to `:root` which has lower specificity than `[data-theme="dark"]`, so overrides should win. Verify with a minimal test. |
| ui-tokens change breaks Android build | The Kotlin/XML generators are independent of `buildTailwindThemeFile`. Run full validation after Phase 1. |
| Phone mockup too complex in CSS | It's a fixed-content decorative element — HTML/CSS complexity is bounded. No dynamic data. |
| Material Symbols font size (~200KB variable font) | Already a dependency. The `material-symbols` npm package handles subsetting via tree-shaking of the CSS. Only referenced glyphs load. |
| Missing `prefers-color-scheme: dark` support in `@theme` scope | The `@theme` block sets light defaults. The `@media` block overrides them. Both compile to standard CSS. Should work in all modern browsers. |
