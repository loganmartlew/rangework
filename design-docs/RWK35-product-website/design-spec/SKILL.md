---
name: rangework-design
description: Use this skill to generate well-branded interfaces and assets for Rangework, either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for prototyping.
user-invocable: true
---

Read the README.md file within this skill, and explore the other available files.
If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, you can copy assets and read the rules here to become an expert in designing with this brand.
If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

## Quick reference

- **Brand:** Rangework — an Android app for golfers who practice with purpose (plan drills → compose into repeatable session templates).
- **Colour:** Deep Fairway primary `#2D6A4F`, Lighter Sage secondary `#52796F`, Warm Graphite surfaces (off-white `#FAFAF8`, never pure white). Greens used sparingly. Brand-rod green `#386044`. Full light + dark Material 3 schemes in `tokens/colors.css`.
- **Type:** DM Sans (300/400/500) for all UI text; DM Mono (400/500) for numbers ONLY — timers, ball/rep counts, distances, percentages. Never mix these roles.
- **Shape:** rounded Material 3 — 12px cards, full-radius pill buttons, soft warm shadows reserved for the FAB and transient surfaces. Outlined cards (hairline border, no shadow) are the default.
- **Icons:** Material Symbols Rounded (CDN) + the geometric brand mark. No emoji.
- **Tone:** second person, sentence case, verb-first actions, concise golf-domain vocabulary.

## Files
- `styles.css` — link this one file to get all tokens + fonts.
- `tokens/` — colours, typography, spacing/shape/motion, fonts, base utilities (`.rw-*` type classes).
- `components/` — React primitives (Button, Fab, Card, ListEntryCard, StatCard, Chip, NumberBadge, TextField, CountStepper).
- `ui_kits/app/` — interactive Android-app recreation.
- `assets/` — brand mark SVGs, Google logo, DM Sans/Mono TTFs.
- `guidelines/cards/` — foundation specimen cards.

For static HTML artifacts, use the `.rw-*` type utility classes and CSS custom properties from `styles.css`, and `<span class="material-symbols-rounded">…</span>` for icons (load the Material Symbols Rounded webfont from Google Fonts).
