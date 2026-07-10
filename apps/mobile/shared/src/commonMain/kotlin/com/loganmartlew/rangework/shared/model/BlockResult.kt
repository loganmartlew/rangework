package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

/**
 * The passive per-Block capture: a free-text note and, for criterion units whose
 * Session Item did *not* enable the Success Observation Type, a manual X-of-Y
 * success count. Both fields optional — note-only, count-only, and both are all
 * legal shapes; an all-null result is equivalent to no result (the repository
 * drops the key on write).
 *
 * Serialized shape matches the `block_results` JSONB entry exactly: camelCase
 * keys `note` and `manualCount`, keyed in the parent map by unitIndex-as-string.
 */
@Serializable
data class BlockResult(
    val note: String? = null,
    val manualCount: Int? = null,
) {
    /** True when neither field carries data — the repository removes such keys. */
    val isEmpty: Boolean get() = note == null && manualCount == null
}
