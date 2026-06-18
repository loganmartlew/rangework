import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import StyleDictionary from "style-dictionary";
import styleDictionaryConfig from "../style-dictionary.config.mjs";

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const packageDir = path.resolve(currentDir, "..");
const generatedDir = path.join(packageDir, "generated");
const androidKotlinDir = path.join(
  generatedDir,
  "android",
  "kotlin",
  "com",
  "loganmartlew",
  "rangework",
  "android",
  "ui",
  "theme"
);
const androidResDir = path.join(generatedDir, "android", "res");
const androidFontDir = path.join(androidResDir, "font");
const androidValuesDir = path.join(androidResDir, "values");
const tokenFiles = [
  path.join(packageDir, "tokens", "color.tokens.json"),
  path.join(packageDir, "tokens", "typography.tokens.json"),
];

const allTokens = await loadAndResolveTokens(tokenFiles);

await fs.rm(path.join(packageDir, "dist"), { recursive: true, force: true });
await fs.rm(generatedDir, { recursive: true, force: true });
await fs.mkdir(androidKotlinDir, { recursive: true });
await fs.mkdir(androidFontDir, { recursive: true });
await fs.mkdir(androidValuesDir, { recursive: true });

const styleDictionary = new StyleDictionary(styleDictionaryConfig);
await styleDictionary.buildAllPlatforms();

await fs.writeFile(
  path.join(androidKotlinDir, "GeneratedRangeworkTokens.kt"),
  buildKotlinTokensFile(allTokens),
  "utf8"
);

await fs.writeFile(
  path.join(androidValuesDir, "rangework_tokens.xml"),
  buildAndroidColorsFile(allTokens),
  "utf8"
);

const fontAssets = Object.values(allTokens.typography.fontAsset);
await Promise.all(
  fontAssets.map(async ({ value }) => {
    const source = path.join(packageDir, "assets", "fonts", value);
    const destination = path.join(androidFontDir, value);
    await fs.copyFile(source, destination);
  })
);

async function loadAndResolveTokens(pathsToLoad) {
  const merged = {};
  for (const filePath of pathsToLoad) {
    const content = JSON.parse(await fs.readFile(filePath, "utf8"));
    deepMerge(merged, content);
  }

  return resolveReferences(merged, merged);
}

function deepMerge(target, source) {
  for (const [key, value] of Object.entries(source)) {
    if (isPlainObject(value) && isPlainObject(target[key])) {
      deepMerge(target[key], value);
      continue;
    }

    target[key] = value;
  }

  return target;
}

function resolveReferences(node, root) {
  if (Array.isArray(node)) {
    return node.map((value) => resolveReferences(value, root));
  }

  if (!isPlainObject(node)) {
    return node;
  }

  if (Object.keys(node).length === 1 && Object.hasOwn(node, "value")) {
    return { value: resolveValue(node.value, root) };
  }

  const resolved = {};
  for (const [key, value] of Object.entries(node)) {
    resolved[key] = resolveReferences(value, root);
  }
  return resolved;
}

function resolveValue(value, root) {
  if (typeof value !== "string") {
    return value;
  }

  const referenceMatch = value.match(/^\{(.+)\}$/);
  if (!referenceMatch) {
    return value;
  }

  const referencePath = referenceMatch[1].replace(/\.value$/, "").split(".");
  const referenced = referencePath.reduce((current, segment) => current?.[segment], root);
  if (!referenced) {
    throw new Error(`Unable to resolve token reference: ${value}`);
  }

  return resolveValue(referenced.value, root);
}

function buildKotlinTokensFile(tokens) {
  const paletteEntries = [
    ...flattenColorGroup("Primary", tokens.color.palette.primary),
    ...flattenColorGroup("Secondary", tokens.color.palette.secondary),
    ...flattenColorGroup("Tertiary", tokens.color.palette.tertiary),
    ...flattenColorGroup("Neutral", tokens.color.palette.neutral),
    ...flattenColorGroup("NeutralVariant", tokens.color.palette.neutralVariant),
    ...flattenColorGroup("Error", tokens.color.palette.error),
    ...flattenNamedColorGroup("LightSurface", tokens.color.surface.light),
    ...flattenNamedColorGroup("DarkSurface", tokens.color.surface.dark),
    ...flattenNamedColorGroup("LightScheme", tokens.color.scheme.light),
    ...flattenNamedColorGroup("DarkScheme", tokens.color.scheme.dark),
  ];

  const materialTypography = Object.entries(tokens.typography.material)
    .map(([roleName, roleTokens]) => buildTypographyConstants(roleName, roleTokens))
    .join("\n");
  const monoTypography = Object.entries(tokens.typography.mono)
    .map(([roleName, roleTokens]) => buildTypographyConstants(`mono${capitalize(roleName)}`, roleTokens))
    .join("\n");

  return `package com.loganmartlew.rangework.android.ui.theme

import androidx.compose.ui.graphics.Color

object GeneratedRangeworkColors {
${paletteEntries.join("\n")}
}

object GeneratedRangeworkTypographyTokens {
    const val SansFamilyName = "${tokens.typography.family.sans.value}"
    const val MonoFamilyName = "${tokens.typography.family.mono.value}"

    const val LightWeight = ${tokens.typography.weight.light.value}
    const val RegularWeight = ${tokens.typography.weight.regular.value}
    const val MediumWeight = ${tokens.typography.weight.medium.value}

${materialTypography}

${monoTypography}
}
`;
}

function buildTypographyConstants(roleName, roleTokens) {
  const constantPrefix = capitalize(roleName);
  return `    const val ${constantPrefix}FontFamily = "${roleTokens.fontFamily.value}"
    const val ${constantPrefix}FontWeight = ${roleTokens.fontWeight.value}
    const val ${constantPrefix}FontSizeSp = ${toKotlinFloat(roleTokens.fontSize.value)}
    const val ${constantPrefix}LineHeightSp = ${toKotlinFloat(roleTokens.lineHeight.value)}
    const val ${constantPrefix}LetterSpacingSp = ${toKotlinFloat(roleTokens.letterSpacing.value)}
`;
}

function buildAndroidColorsFile(tokens) {
  const colorEntries = [
    ...xmlColorGroup("primary", tokens.color.palette.primary),
    ...xmlColorGroup("secondary", tokens.color.palette.secondary),
    ...xmlColorGroup("tertiary", tokens.color.palette.tertiary),
    ...xmlColorGroup("neutral", tokens.color.palette.neutral),
    ...xmlColorGroup("neutral_variant", tokens.color.palette.neutralVariant),
    ...xmlColorGroup("error", tokens.color.palette.error),
    ...xmlNamedColorGroup("light_surface", tokens.color.surface.light),
    ...xmlNamedColorGroup("dark_surface", tokens.color.surface.dark),
    ...xmlNamedColorGroup("light_scheme", tokens.color.scheme.light),
    ...xmlNamedColorGroup("dark_scheme", tokens.color.scheme.dark),
  ];

  return `<?xml version="1.0" encoding="utf-8"?>
<resources>
${colorEntries.join("\n")}
</resources>
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

function xmlColorGroup(prefix, group) {
  return Object.entries(group).map(
    ([key, token]) => `    <color name="rangework_${prefix}_${key}">${token.value}</color>`
  );
}

function xmlNamedColorGroup(prefix, group) {
  return Object.entries(group).map(
    ([key, token]) => `    <color name="rangework_${prefix}_${toSnakeCase(key)}">${token.value}</color>`
  );
}

function hexToComposeColor(hex) {
  return `0xFF${hex.replace("#", "").toUpperCase()}`;
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function toSnakeCase(value) {
  return value.replace(/[A-Z]/g, (char) => `_${char.toLowerCase()}`);
}

function toKotlinFloat(value) {
  return Number.isInteger(value) ? `${value}f` : `${value}f`;
}

function isPlainObject(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
