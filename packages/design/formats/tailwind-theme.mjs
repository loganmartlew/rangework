const tailwindScaleMap = {
  0: '50',
  10: '100',
  20: '200',
  30: '300',
  40: '400',
  50: '500',
  60: '600',
  70: '700',
  80: '800',
  90: '900',
  100: '950',
};

export function tailwindThemeFormat({ dictionary }) {
  const { color, typography } = dictionary.tokens;

  const lines = [
    '@theme {',
    ...buildDefaultColorVariables(color, typography),
    '}',
    '',
    '@media (prefers-color-scheme: dark) {',
    '  :root:not([data-theme="light"]) {',
    ...buildThemeVariablesForScheme(color, 'dark').map(line => `  ${line}`),
    '  }',
    '}',
    '',
    '[data-theme="dark"] {',
    ...buildThemeVariablesForScheme(color, 'dark'),
    '}',
  ];

  return `${lines.join('\n')}\n`;
}

function buildDefaultColorVariables(color, typography) {
  const paletteGroups = {
    primary: color.palette.primary,
    secondary: color.palette.secondary,
    tertiary: color.palette.tertiary,
    neutral: color.palette.neutral,
  };

  return Object.entries(paletteGroups)
    .flatMap(([name, group]) => buildPaletteVariables(name, group))
    .concat(buildThemeVariablesForScheme(color, 'light'))
    .concat([
      `  --font-sans: "${typography.family.sans.value}", ui-sans-serif, system-ui, sans-serif;`,
      `  --font-mono: "${typography.family.mono.value}", ui-monospace, SFMono-Regular, monospace;`,
    ]);
}

function buildPaletteVariables(name, group) {
  return Object.entries(group)
    .filter(([key]) => tailwindScaleMap[key] !== undefined)
    .map(([key, token]) => `  --color-${name}-${tailwindScaleMap[key]}: ${token.value};`);
}

function buildThemeVariablesForScheme(color, schemeName) {
  const scheme = color.scheme[schemeName];
  const surface = color.surface[schemeName];

  return [
    ...Object.entries(scheme).map(
      ([key, token]) => `  --color-${key.toLowerCase()}: ${token.value};`,
    ),
    ...Object.entries(surface).map(
      ([key, token]) => `  --color-surface${key.toLowerCase()}: ${token.value};`,
    ),
  ];
}
