# Brand assets are generated from a single source by a generator pipeline in `@rangework/design`

The Rangework brand mark (three perspective bands + a tee-shaped rod) was hand-replicated
across ~13 files in three formats — web SVG (`apps/site`), Android vector XML
(`apps/mobile`), and raster (`favicon.ico`, store assets) — so every geometry or colour
change had to be applied by hand in every copy, and the copies had already drifted (e.g. the
adaptive-icon background `#2A2A28` was a hand-eyeballed near-duplicate of token colours). We
decided to derive all brand-mark variants from **one role-annotated base SVG**
(`packages/design/brand/mark.base.svg`) plus a declarative manifest, generated at build
time by extending `@rangework/design` from a Style-Dictionary wrapper into a small
**design-asset build pipeline**: a thin orchestrator resolves the design tokens once and runs
an ordered list of self-contained **generators** (Style-Dictionary tokens being one, brand
assets another) over a shared context. Colour is single-sourced — every variant colour
resolves from `color.tokens.json` via the shared `ctx.tokens`, with generators emitting each
target format from a parsed geometry model (so Android keeps `@color/...` token references and
`android:tint`, not baked hex).

## Status

accepted

## Considered options

- **Hand-maintain each file (status quo).** Rejected: the duplication and drift this causes is
  the problem being solved.
- **Generate only the web SVGs, keep Android XML hand-authored.** Rejected: Android is where
  most of the duplication lives (8+ drawables) and where drift is hardest to spot.
- **Bend Style Dictionary's formatter API to also emit SVG/raster.** Rejected: SD's model is
  token→file mapping, not geometry composition or rasterisation. The brand work is a different
  shape of job and is cleaner as a peer generator than as an abused SD format.
- **A second, separate `packages/brand` package.** Rejected for now: the mark is coloured by
  token values that already live in the design package, so co-location keeps one design source of
  truth and one build. Revisit only if the brand system grows large and independent.

## Consequences

- **The package is no longer "just tokens."** It holds both design tokens and a brand-asset
  generator pipeline, so the Style-Dictionary work is now one generator among several. Because
  the name no longer described the contents, the package was renamed from `@rangework/ui-tokens`
  (folder `packages/ui-tokens`) to **`@rangework/design`** (folder `packages/design`) — "design"
  being the honest superset of tokens *and* brand assets. The rename is a standalone, mechanical
  first commit, ahead of the pipeline work.
- **Generated brand files are gitignored**, matching the existing `generated/` + `dist/`
  precedent. Class 1 (geometry) and Class 2 (inset/splash/background wrappers) Android
  drawables and all web brand SVGs stop being committed sources and become build outputs.
- **Two deliberate hand-authored exceptions remain committed:** `ic_notification.xml` (a
  24×24, four-path small-size redraw that is *not* a scale of the base) and the
  `mipmap-anydpi-v26` adaptive-icon definitions (app-owned Android icon wiring, including the
  themed-icon `monochrome` slot).
- **The site keeps zero brand footprint:** assets are served from the package in dev and copied
  into `dist/` at build, so nothing under `apps/site/` is committed for the brand.
- **The Play Store icon is the one committed generated artifact** (in `business-docs/stores/`,
  for repo availability), which reintroduces a staleness risk; a CI drift check guards it.
- **The launcher and Play Store icon backgrounds change `#2A2A28` → `#1C1A18`** as part of
  unifying all icon backgrounds on the `neutral.88` token — a small but visible on-device
  change, accepted deliberately.
