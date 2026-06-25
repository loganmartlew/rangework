import StyleDictionary from 'style-dictionary';
import sdConfig from '../../style-dictionary.config.mjs';

/**
 * The original Style Dictionary build, now one generator among peers. Outputs
 * are byte-identical to the pre-pipeline build:
 *   dist/web/{tailwind-theme.css,fonts.css}
 *   generated/android/{kotlin,res}
 */
export function tokensGenerator() {
  return {
    name: 'tokens',
    async build(ctx) {
      const sd = new StyleDictionary(sdConfig);
      await sd.buildAllPlatforms();
      ctx.log('built design tokens (Style Dictionary)');
    },
  };
}
