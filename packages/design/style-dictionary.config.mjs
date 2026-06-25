import { composeTokensFormat } from './formats/compose-tokens.mjs';
import { androidXmlColorsFormat } from './formats/android-xml-colors.mjs';
import { tailwindThemeFormat } from './formats/tailwind-theme.mjs';
import { webFontsFormat } from './formats/web-fonts.mjs';
import { copyFontsAction } from './actions/copy-fonts.mjs';

export default {
  source: ['tokens/**/*.json'],
  hooks: {
    formats: {
      'rangework/compose-tokens': composeTokensFormat,
      'rangework/android-xml-colors': androidXmlColorsFormat,
      'rangework/tailwind-theme': tailwindThemeFormat,
      'rangework/web-fonts': webFontsFormat,
    },
    actions: {
      'rangework/copy-fonts': copyFontsAction,
    },
  },
  platforms: {
    android: {
      buildPath: 'generated/android/',
      transforms: ['name/kebab'],
      options: { fontDest: 'res/font' },
      files: [
        {
          destination:
            'kotlin/com/loganmartlew/rangework/android/ui/theme/GeneratedRangeworkTokens.kt',
          format: 'rangework/compose-tokens',
        },
        {
          destination: 'res/values/rangework_tokens.xml',
          format: 'rangework/android-xml-colors',
        },
      ],
      actions: ['rangework/copy-fonts'],
    },
    web: {
      buildPath: 'dist/web/',
      transforms: ['name/kebab'],
      options: { fontDest: '../fonts' },
      files: [
        {
          destination: 'tailwind-theme.css',
          format: 'rangework/tailwind-theme',
        },
        {
          destination: 'fonts.css',
          format: 'rangework/web-fonts',
        },
      ],
      actions: ['rangework/copy-fonts'],
    },
  },
};
