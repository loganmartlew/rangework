# Rangework Design System

**Rangework** is an Android-first app for golfers who want to practice with purpose. Instead of hitting balls aimlessly at the range, golfers plan ahead: they build **Practice Units** (individual drills — an ordered list of instructions with ball counts, a focus cue, notes, and a default club) and compose them into **Practice Sessions** (repeatable session templates that tally a total ball count). Data syncs to the cloud, gated behind Google sign-in.

This design system packages Rangework's real visual foundations — its Material 3 colour scheme, DM Sans / DM Mono type, brand mark, reusable component primitives, and a click-through recreation of the Android app — so you can design on-brand interfaces and assets.

## Sources

Everything here is reverse-engineered from the product's own source of truth:

- **GitHub:** [`github.com/loganmartlew/rangework`](https://github.com/loganmartlew/rangework) — a Kotlin Multiplatform monorepo (`androidApp` Jetpack Compose shell, `shared` domain, `supabase` backend). The brand foundations live in:
  - `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/Color.kt` — the full Material 3 tonal palette ("Combo A": Deep Fairway / Lighter Sage / Warm Graphite).
  - `.../ui/theme/Type.kt` — DM Sans + DM Mono type scale and the `RangeworkMono` numeric styles.
  - `.../ui/components/` (30+ composables) and `.../ui/screens/` (9 screens) — the source for the components and UI kit recreated here.
  - `CLAUDE.md` — an unusually detailed typography spec (when to use DM Mono vs DM Sans, colour pairings).
  - `androidApp/src/main/res/font/` and `.../res/drawable/` — the TTF webfonts and brand mark drawables, copied into `assets/`.

> Explore the repository further to build higher-fidelity designs — the Compose source is the ground truth for layout, states, and interaction details.

---

## Content fundamentals

How Rangework writes copy:

- **Voice — second person, encouraging, plain.** The app talks to the golfer directly: "Build reusable practice units and combine them into focused session plans." Empty states coach rather than scold: "Plan sharper range sessions."
- **Sentence case everywhere** for prose, titles, and buttons ("Start session", "New session", "Create your first unit"). **UPPERCASE** is reserved for small section labels and stat captions ("RECENTLY USED", "NEXT MOVE", "UNITS") — applied at the call site via `.uppercase()`, never baked into a text style.
- **Verb-first actions.** Buttons and CTAs lead with a verb: "Create unit", "New session", "Open most recent", "Resume editing", "Sign out".
- **Concise & concrete.** Metadata reads as terse, scannable fragments joined by middots: "7 Iron · 4 instructions · 40 balls". Numbers carry meaning (ball counts, reps, instruction counts) and are always shown in mono.
- **Golf-domain, never jargon-heavy.** "drill", "landing zone", "carry", "tempo", "rep", "ball count", "club bag" — familiar range vocabulary, no marketing fluff.
- **No emoji.** The brand expresses itself through the mark and restrained green accents, not emoji or exclamation.
- **Friendly fallbacks.** Misconfigured/empty states degrade to coherent guidance ("Planning unavailable", "No instructions yet — add instructions to define how this unit should be practiced.") rather than errors.

---

## Visual foundations

- **Colour — earthy, calm, course-inspired.** Primary is **Deep Fairway** green (`#2D6A4F`); secondary is a muted **Lighter Sage** (`#52796F`). Surfaces are **Warm Graphite** — an off-white (`#FAFAF8`), never pure white, on warm-grey container stops. Greens are used sparingly for emphasis (primary buttons, the "Next move" wash, selected nav, mono metrics), not as flood fills. A slightly darker brand green (`#386044`) tints the logo's rod and highlighted numeric values. Full light + dark Material 3 schemes are defined; light is the default brand presentation.
- **Type — two families, strict roles.** **DM Sans** (weights 300/400/500 only) for all UI text on the Material 3 type scale. **DM Mono** (400/500) exclusively for numbers — timers, ball/rep counts, distances, percentages, step indices — via the `RangeworkMono` large/medium/small styles. Display uses light (300) DM Sans; titles and labels use medium (500). Never use mono for prose; never use sans for a metric.
- **Shape — rounded, soft.** Material 3 shape scale: 8px (small), 12px (default card), 16px (FAB, pills), 28px (dialogs/large containers). Buttons are full-radius pills. Step badges and avatars are circles.
- **Cards — outlined first.** The dominant card is an **OutlinedCard**: surface fill, 1px `outline-variant` hairline border, 12px radius, **no shadow**. Filled tonal cards and the primary-container "accent" card (the "Next move" highlight) appear for emphasis. Shadows are reserved for the FAB (elevation-3) and transient surfaces (menus, dialogs); they're soft and warm-tinted (graphite, not pure black).
- **Backgrounds.** Flat warm off-white. **No gradients, no photography, no illustration textures.** The only illustrative element is the geometric brand mark, used at large sizes in empty/onboarding states. Visual interest comes from the green accents, mono numerals, and generous whitespace.
- **Spacing & layout.** 4px grid; **16px** is the default screen and card padding; 12px between list cards. Phone layout puts navigation in a **bottom bar**; tablet (≥840dp) switches to a **navigation rail** with a two-column overview. Lists are single-column stacks of cards; the overview leads with a greeting, two stat tiles, an accent "Next move" card, then a horizontally-scrolling "Recently used" rail.
- **Motion.** Restrained Material 3 standard easing (`cubic-bezier(0.2,0,0,1)`), short 150–250ms transitions. Crossfades and tonal state changes; no bounces, no decorative looping animation.
- **States.** Hover/press read as a subtle `brightness()` shift on filled surfaces and an 8% primary tint on text/outlined buttons. Disabled = 38% opacity (Material 3 convention). Selected nav items get a sage `secondary-container` pill behind a filled icon. Focused text fields thicken their border to 2px Deep Fairway.
- **Borders & dividers.** 1px `outline-variant` (`#D8D6D0` light). Full-width hairline dividers separate instruction rows inside detail cards.
- **Imagery vibe.** Effectively none — the brand is typographic and geometric. If imagery is ever added, keep it warm, natural, and course-adjacent.

---

## Iconography

- **Material Symbols (Rounded).** The app uses the Jetpack Compose **Material Icons** set (`Icons.Rounded.*` / `Icons.Filled.*`) — Home, Widgets, EventNote, Tune for navigation; Add, Edit, MoreVert, ChevronRight, Remove, GolfCourse, etc. throughout. These are **not vendored** in the repo (they ship with Compose), so this design system loads the matching **Material Symbols Rounded** webfont from Google Fonts CDN. Use `<span class="material-symbols-rounded">home</span>`; toggle `'FILL' 1` for selected/active states (matching the filled nav icons).
- **Brand mark** (`assets/rangework-mark.svg`, `assets/rangework-mark-mono.svg`) — three receding "range bands" (a driving range in perspective) with a vertical rod + tee cap. Bands are graphite, the rod is brand green. Two-colour at 40px+, monochrome (`currentColor`) below that or in single-colour contexts. Copied verbatim from `ic_rangework_mark_twocolor.xml` / `ic_rangework_mark.xml`.
- **Google logo** (`assets/ic_google_logo.svg`) — for the sign-in button.
- **No emoji, no unicode-symbol icons.** All iconography is either Material Symbols or the brand mark.

> **Substitution flag:** the navigation/UI glyphs are rendered with the Material Symbols Rounded webfont as the closest faithful match to Compose's bundled Material Icons (same rounded family). If you need the exact vector drawables, pull them from the Compose Material Icons artifact.

---

## What's in here (index)

**Foundations**
- `styles.css` — the single entry point consumers link. Imports the token files + `tokens/base.css`.
- `tokens/colors.css` — Material 3 light + dark scheme (`[data-theme="dark"]`), tonal palettes + semantic roles.
- `tokens/typography.css` — DM Sans / DM Mono scale variables.
- `tokens/spacing.css` — spacing grid, radius, elevation, motion.
- `tokens/fonts.css` — `@font-face` for the TTF webfonts in `assets/fonts/`.
- `tokens/base.css` — body defaults + `.rw-*` type-role utility classes.
- `guidelines/cards/` — foundation specimen cards (Colors, Type, Spacing, Brand).

**Components** (`components/<group>/`, namespace `window.RangeworkDesignSystem_9d40db`)
- `buttons/` — `Button` (filled/tonal/outlined/text), `Fab` (compact + extended).
- `cards/` — `Card` (outlined/filled/accent), `ListEntryCard`, `StatCard`.
- `data-display/` — `Chip` + `ClubChip` / `BallCountPill`, `NumberBadge`.
- `forms/` — `TextField`, `CountStepper`.

**UI kit** (`ui_kits/app/`)
- `index.html` — interactive Android-app recreation: Google sign-in → overview, units list, unit detail, sessions list, settings, with bottom navigation and a FAB.

**Brand assets** (`assets/`)
- `rangework-mark.svg`, `rangework-mark-mono.svg`, `ic_google_logo.svg`, `fonts/*.ttf`.

**Skill**
- `SKILL.md` — packaging for use as an Agent Skill.
