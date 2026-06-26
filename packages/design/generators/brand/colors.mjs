/**
 * Resolve a manifest colour expression to a concrete fill for each target.
 * This is the single point where token references become values, so no emitter
 * ever hardcodes hex.
 */

const WHITE = '#FFFFFF';

/** @param {{mode:string,token?:string,theme?:'light'|'dark'}} expr */
export function resolveWebFill(expr, tokens) {
  switch (expr.mode) {
    case 'currentColor':
      return 'currentColor';
    case 'mask':
      return WHITE;
    case 'token-fixed':
      return tokens.resolve(expr.token, expr.theme);
    default:
      throw new Error(`colour mode '${expr.mode}' is not valid for a web/raster target`);
  }
}

/** @param {{mode:string,token?:string,theme?:'light'|'dark'}} expr */
export function resolveAndroidFill(expr, tokens) {
  switch (expr.mode) {
    case 'mask':
      return WHITE;
    case 'token-responsive':
      return `@color/${expr.token}`;
    case 'token-fixed':
      return tokens.resolve(expr.token, expr.theme);
    default:
      throw new Error(`colour mode '${expr.mode}' is not valid for an Android target`);
  }
}

/** Format a fill-opacity for Android's android:fillAlpha (two decimals, like the source). */
export function formatAlpha(opacity) {
  return opacity.toFixed(2);
}
