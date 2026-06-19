export function webFontsFormat({ dictionary }) {
  const { typography } = dictionary.tokens;
  const { family, fontAsset } = typography;

  const fontDefinitions = Object.entries(fontAsset).map(([key, token]) =>
    buildFontFaceDefinition(key, token.value, family),
  );

  return `${fontDefinitions.join('\n\n')}\n`;
}

function buildFontFaceDefinition(key, fileName, families) {
  const normalizedKey = key.toLowerCase();
  const family = normalizedKey.startsWith('sans') ? families.sans.value : families.mono.value;
  const fontStyle = normalizedKey.includes('italic') ? 'italic' : 'normal';
  const fontWeight = normalizedKey.includes('light')
    ? 300
    : normalizedKey.includes('medium')
      ? 500
      : 400;

  return `@font-face {
  font-family: "${family}";
  src: url("../fonts/${fileName}") format("truetype");
  font-style: ${fontStyle};
  font-weight: ${fontWeight};
  font-display: swap;
}`;
}
