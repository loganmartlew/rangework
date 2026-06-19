export function androidXmlColorsFormat({ dictionary }) {
  const { color } = dictionary.tokens;

  const colorEntries = [
    ...xmlColorGroup('primary', color.palette.primary),
    ...xmlColorGroup('secondary', color.palette.secondary),
    ...xmlColorGroup('tertiary', color.palette.tertiary),
    ...xmlColorGroup('neutral', color.palette.neutral),
    ...xmlColorGroup('neutral_variant', color.palette.neutralVariant),
    ...xmlColorGroup('error', color.palette.error),
    ...xmlNamedColorGroup('light_surface', color.surface.light),
    ...xmlNamedColorGroup('dark_surface', color.surface.dark),
    ...xmlNamedColorGroup('light_scheme', color.scheme.light),
    ...xmlNamedColorGroup('dark_scheme', color.scheme.dark),
  ];

  return `<?xml version="1.0" encoding="utf-8"?>
<resources>
${colorEntries.join('\n')}
</resources>
`;
}

function xmlColorGroup(prefix, group) {
  return Object.entries(group).map(
    ([key, token]) =>
      `    <color name="rangework_${prefix}_${key}">${token.value}</color>`,
  );
}

function xmlNamedColorGroup(prefix, group) {
  return Object.entries(group).map(
    ([key, token]) =>
      `    <color name="rangework_${prefix}_${toSnakeCase(key)}">${token.value}</color>`,
  );
}

function toSnakeCase(value) {
  return value.replace(/[A-Z]/g, char => `_${char.toLowerCase()}`);
}
