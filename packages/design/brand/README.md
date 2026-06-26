# Rangework brand assets

Every brand-mark variant — web SVG, Android vector XML, and raster — is
**generated** from two hand-edited sources:

- `mark.base.svg` — the canonical geometry (three perspective bands + a
  tee-shaped rod). Edit shapes here only.
- `manifest.mjs` — the declarative list of variants (colour, background, crop,
  target format, output path).

Run `pnpm --filter @rangework/design build` to regenerate. Outputs land in
`packages/design/generated/` (gitignored) and are consumed in place by the
Android build and served to the site by a Vite plugin. **Do not edit generated
files** — they carry a `generated — do not edit` header and are overwritten on
every build.

## Where colour comes from

No hex is hand-written in the pipeline. Colours resolve from
`tokens/color.tokens.json` via the shared token context:

| Colour    | Role / use                                  | Token                |
| --------- | ------------------------------------------- | -------------------- |
| `#282624` | band, light                                 | `neutral.80`         |
| `#D5D3CF` | band, dark                                  | `neutral.30`         |
| `#C4C2BB` | band, fixed-on-dark (launcher / Play fg)    | `neutralVariant.30`  |
| `#386044` / `#93C8A8` | rod, light / dark               | `brandRod` light / dark |
| `#1C1A18` | favicon **and** launcher / Play background  | `neutral.88`         |

The two-colour and tinted Android marks reference the runtime-responsive
resources `@color/rangework_mark_band` / `@color/rangework_mark_rod`, which are
hand-authored in `androidApp/.../res/values{,-night}/colors.xml`.

## Variant usage guide

### Standalone mark — `ic_rangework_mark` (monochrome, tinted)

Use for empty states, onboarding, toolbar, any single-colour context. The root
`android:tint` resolves to `@color/rangework_mark_band` — graphite in light
mode, warm off-white in dark mode, no `ColorFilter` needed in Compose. Do **not**
use it for the launcher, notification, or splash — those have dedicated drawables.

Sizing guide:

| Size   | Use                                         |
| ------ | ------------------------------------------- |
| 96dp   | Onboarding hero                             |
| 64dp   | Empty-state illustration                    |
| 48dp   | Settings / about row                        |
| 24dp   | Inline / toolbar (consider `ic_notification`) |

### Standalone mark — `ic_rangework_mark_twocolor`

Use for the about/settings screen, onboarding hero, any context at **40dp+**
where full brand-colour expression is appropriate. Avoid below 40dp — the green
rod is too narrow to read as intentionally distinct from the bands at small
sizes; use `ic_rangework_mark` instead.

### Web marks

- `rangework-mark.svg` — two-colour mark (graphite bands, green rod) on a
  transparent background; used in the site nav and footer.
- `rangework-mark-mono.svg` — single-colour mark that inherits `currentColor`.
- `logo.svg` — tightly cropped mark for compact placements (e.g. the OAuth
  consent header); exported as `@rangework/design/brand/logo.svg`.

### Icons

- `favicon.svg` / `favicon.ico` — rounded dark tile with the fixed-on-dark mark.
- `ic_launcher_*` — adaptive launcher foreground / monochrome / background plus
  their inset and round wrappers; the `mipmap-anydpi-v26` definitions wire them
  together.
- `ic_splash_icon` — inset launcher foreground for the splash screen.
- `play-icon-512.png` — 512², 32-bit RGBA Play Store icon (square, no radius —
  Google Play applies its own mask). The committed copy lives at
  `business-docs/stores/rangework_play_icon_512.png`; a CI check guards it
  against drift.

## Hand-authored exceptions (not generated)

- `ic_notification.xml` — a 24×24, four-path small-size redraw, not a scale of
  the base mark.
- `ic_google_logo.xml` — third-party, out of scope.
- `mipmap-anydpi-v26/ic_launcher{,_round}.xml` — app-owned adaptive-icon wiring.
