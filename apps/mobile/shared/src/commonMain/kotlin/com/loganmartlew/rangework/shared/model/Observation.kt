package com.loganmartlew.rangework.shared.model

/**
 * One observed ball's record: the values captured for whichever Observation Types
 * had a value at commit time. The commit unit in the design (auto-commit when all
 * enabled types have a value; the +1 tap commits a partial or empty record).
 *
 * [values] is keyed by Observation Type wire id; values are canonical value
 * strings. An empty map is a legal record ("committed, nothing observed") and is
 * indistinguishable from no row at all — both mean *unobserved* per type.
 *
 * The layer is deliberately string-typed: it round-trips whatever the DB holds,
 * and typed accessors below drop anything outside this app version's vocabulary.
 */
data class Observation(
    val stepIndex: Int,
    val values: Map<String, String> = emptyMap(),
) {
    /** The canonical value string for [type], or null when unobserved or out-of-vocabulary. */
    fun value(type: ObservationType): String? =
        values[type.id]?.takeIf(type::accepts)

    /** True when [type] carries an in-vocabulary value for this ball. */
    fun hasValue(type: ObservationType): Boolean = value(type) != null
}
