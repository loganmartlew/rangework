/**
 * Declarative list of every generated brand output. The base SVG supplies the
 * geometry; this manifest supplies colour, background, crop and target format.
 * No literal hex lives here — colours are token references the generators
 * resolve from `ctx.tokens`.
 *
 * Colour expression (`colorExpr`):
 *   { mode: 'currentColor' }                         web mono — inherits CSS color
 *   { mode: 'token-responsive', token }              Android @color/<token> (flips light/dark at runtime)
 *   { mode: 'token-fixed', token, theme? }           resolve a token to one value and bake it
 *   { mode: 'mask' }                                 solid white mask (Android monochrome / tinted mono)
 *
 * `token` for token-fixed is a token path ('neutral.80', 'neutralVariant.30',
 * 'brandRod'); for token-responsive it is the app-owned Android colour resource
 * name (those responsive mark colours live in androidApp res, not the tokens).
 */

const currentColor = { mode: 'currentColor' };
const mask = { mode: 'mask' };
/** @param {string} resource Android colour resource name */
const responsive = resource => ({ mode: 'token-responsive', token: resource });
/** @param {string} token token path  @param {'light'|'dark'} [theme] */
const fixed = (token, theme) => ({ mode: 'token-fixed', token, theme });

// "fixed-on-dark" band — neutralVariant.30 (#C4C2BB) at the base opacities.
const fixedOnDarkBand = fixed('neutralVariant.30');
const rodDark = fixed('brandRod', 'dark');

const DRAWABLE = 'generated/android/res/drawable';
const BRAND = 'generated/brand';

/** @typedef {{ mode: string, token?: string, theme?: 'light'|'dark' }} ColorExpr */

export const variants = [
  // ── Web SVG marks ──────────────────────────────────────────────────────
  {
    name: 'rangework-mark',
    target: 'web-svg',
    colors: { band: fixed('neutral.80', 'light'), rod: fixed('brandRod', 'light') },
    outputs: [`${BRAND}/rangework-mark.svg`],
  },
  {
    name: 'rangework-mark-mono',
    target: 'web-svg',
    colors: { band: currentColor, rod: currentColor },
    outputs: [`${BRAND}/rangework-mark-mono.svg`],
  },
  {
    name: 'logo',
    target: 'web-svg',
    colors: { band: fixed('neutral.80', 'light'), rod: fixed('brandRod', 'light') },
    crop: [20, 17, 68, 71],
    outputs: [`${BRAND}/logo.svg`],
  },
  {
    name: 'favicon',
    target: 'web-svg',
    colors: { band: fixedOnDarkBand, rod: rodDark },
    background: { token: 'neutral.88', shape: 'rounded', radius: 20 },
    outputs: [`${BRAND}/favicon.svg`],
  },

  // ── Raster ─────────────────────────────────────────────────────────────
  {
    name: 'favicon-ico',
    target: 'raster',
    colors: { band: fixedOnDarkBand, rod: rodDark },
    background: { token: 'neutral.88', shape: 'rounded', radius: 20 },
    raster: { format: 'ico', sizes: [16, 32, 48] },
    outputs: [`${BRAND}/favicon.ico`],
  },
  {
    name: 'play-icon',
    target: 'raster',
    colors: { band: fixedOnDarkBand, rod: rodDark },
    // Square, no radius — Google Play applies its own mask.
    background: { token: 'neutral.88', shape: 'square' },
    raster: { format: 'png', size: 512 },
    outputs: [`${BRAND}/play-icon-512.png`],
  },

  // ── Android vector marks ───────────────────────────────────────────────
  {
    name: 'ic_rangework_mark',
    target: 'android-vector',
    // Monochrome, tinted: white mask paths + root android:tint.
    tint: 'rangework_mark_band',
    colors: { band: mask, rod: mask },
    outputs: [`${DRAWABLE}/ic_rangework_mark.xml`],
  },
  {
    name: 'ic_rangework_mark_twocolor',
    target: 'android-vector',
    colors: { band: responsive('rangework_mark_band'), rod: responsive('rangework_mark_rod') },
    outputs: [`${DRAWABLE}/ic_rangework_mark_twocolor.xml`],
  },
  {
    name: 'ic_launcher_foreground',
    target: 'android-vector',
    colors: { band: fixedOnDarkBand, rod: rodDark },
    outputs: [`${DRAWABLE}/ic_launcher_foreground.xml`],
  },
  {
    name: 'ic_launcher_monochrome',
    target: 'android-vector',
    colors: { band: mask, rod: mask },
    outputs: [`${DRAWABLE}/ic_launcher_monochrome.xml`],
  },

  // ── Android wrappers (Class 2) ─────────────────────────────────────────
  {
    name: 'ic_launcher_foreground_inset',
    target: 'android-vector',
    wrapper: { type: 'inset', drawable: 'ic_launcher_foreground', inset: '12dp' },
    outputs: [`${DRAWABLE}/ic_launcher_foreground_inset.xml`],
  },
  {
    name: 'ic_launcher_foreground_round',
    target: 'android-vector',
    wrapper: { type: 'inset', drawable: 'ic_launcher_foreground_inset', inset: '12dp' },
    outputs: [`${DRAWABLE}/ic_launcher_foreground_round.xml`],
  },
  {
    name: 'ic_launcher_monochrome_inset',
    target: 'android-vector',
    wrapper: { type: 'inset', drawable: 'ic_launcher_monochrome', inset: '12dp' },
    outputs: [`${DRAWABLE}/ic_launcher_monochrome_inset.xml`],
  },
  {
    name: 'ic_launcher_monochrome_round',
    target: 'android-vector',
    wrapper: { type: 'inset', drawable: 'ic_launcher_monochrome_inset', inset: '12dp' },
    outputs: [`${DRAWABLE}/ic_launcher_monochrome_round.xml`],
  },
  {
    name: 'ic_launcher_background',
    target: 'android-vector',
    wrapper: { type: 'color', token: 'neutral.88' },
    outputs: [`${DRAWABLE}/ic_launcher_background.xml`],
  },
  {
    name: 'ic_splash_icon',
    target: 'android-vector',
    wrapper: { type: 'inset', drawable: 'ic_launcher_foreground', inset: '18dp' },
    outputs: [`${DRAWABLE}/ic_splash_icon.xml`],
  },
];

export default { variants };
