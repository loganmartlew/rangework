package com.loganmartlew.rangework.shared.model

private val NON_ALPHANUMERIC_RUN = Regex("[^a-z0-9]+")

/**
 * Generate a Tag Code (lowercase-underscore slug) from a raw name.
 *
 * Lowercases, collapses every run of non-alphanumeric characters into a single
 * underscore, and strips leading/trailing underscores. Returns `null` when the
 * input has no alphanumeric content (an invalid tag name).
 *
 * This is the canonical dedup key. It is mirrored by `slugify_tag` in SQL and
 * `slugifyTag` in the MCP TypeScript — keep all three in sync.
 */
fun slugifyTag(raw: String): String? = raw
    .trim()
    .lowercase()
    .replace(NON_ALPHANUMERIC_RUN, "_")
    .trim('_')
    .ifEmpty { null }
