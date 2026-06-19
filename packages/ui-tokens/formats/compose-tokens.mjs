export function composeTokensFormat({ dictionary }) {
  const { color, typography } = dictionary.tokens;

  const paletteEntries = [
    ...flattenColorGroup('Primary', color.palette.primary),
    ...flattenColorGroup('Secondary', color.palette.secondary),
    ...flattenColorGroup('Tertiary', color.palette.tertiary),
    ...flattenColorGroup('Neutral', color.palette.neutral),
    ...flattenColorGroup('NeutralVariant', color.palette.neutralVariant),
    ...flattenColorGroup('Error', color.palette.error),
    ...flattenNamedColorGroup('LightSurface', color.surface.light),
    ...flattenNamedColorGroup('DarkSurface', color.surface.dark),
    ...flattenNamedColorGroup('LightScheme', color.scheme.light),
    ...flattenNamedColorGroup('DarkScheme', color.scheme.dark),
  ];

  const materialTypography = Object.entries(typography.material)
    .map(([roleName, roleTokens]) => buildTypographyConstants(roleName, roleTokens))
    .join('\n');

  const monoTypography = Object.entries(typography.mono)
    .map(([roleName, roleTokens]) =>
      buildTypographyConstants(`mono${capitalize(roleName)}`, roleTokens),
    )
    .join('\n');

  return `package com.loganmartlew.rangework.android.ui.theme

import androidx.compose.ui.graphics.Color

object GeneratedRangeworkColors {
${paletteEntries.join('\n')}
}

object GeneratedRangeworkTypographyTokens {
    const val SansFamilyName = "${typography.family.sans.value}"
    const val MonoFamilyName = "${typography.family.mono.value}"

    const val LightWeight = ${typography.weight.light.value}
    const val RegularWeight = ${typography.weight.regular.value}
    const val MediumWeight = ${typography.weight.medium.value}

${materialTypography}

${monoTypography}
}
`;
}

function flattenColorGroup(prefix, group) {
  return Object.entries(group).map(([key, token]) => {
    const name = `${prefix}${key}`;
    return `    val ${name} = Color(${hexToComposeColor(token.value)})`;
  });
}

function flattenNamedColorGroup(prefix, group) {
  return Object.entries(group).map(([key, token]) => {
    const name = `${prefix}${capitalize(key)}`;
    return `    val ${name} = Color(${hexToComposeColor(token.value)})`;
  });
}

function buildTypographyConstants(roleName, roleTokens) {
  const prefix = capitalize(roleName);
  return `    const val ${prefix}FontFamily = "${roleTokens.fontFamily.value}"
    const val ${prefix}FontWeight = ${roleTokens.fontWeight.value}
    const val ${prefix}FontSizeSp = ${toKotlinFloat(roleTokens.fontSize.value)}
    const val ${prefix}LineHeightSp = ${toKotlinFloat(roleTokens.lineHeight.value)}
    const val ${prefix}LetterSpacingSp = ${toKotlinFloat(roleTokens.letterSpacing.value)}
`;
}

function hexToComposeColor(hex) {
  return `0xFF${hex.replace('#', '').toUpperCase()}`;
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function toKotlinFloat(value) {
  return `${value}f`;
}
