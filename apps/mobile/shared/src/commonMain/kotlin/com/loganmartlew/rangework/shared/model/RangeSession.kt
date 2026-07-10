package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RangeSession(
    val id: String,
    @SerialName("source_session_id")
    val sourceSessionId: String? = null,
    @SerialName("session_name")
    val sessionName: String,
    val snapshot: RangeSessionSnapshot,
    @SerialName("snapshot_version")
    val snapshotVersion: Int,
    @SerialName("completed_steps")
    val completedSteps: List<CompletedStep>,
    @SerialName("club_overrides")
    val clubOverrides: Map<String, String>,
    @SerialName("last_viewed_step_index")
    val lastViewedStepIndex: Int? = null,
    @SerialName("session_note")
    val sessionNote: String? = null,
    @SerialName("block_results")
    val blockResults: Map<String, BlockResult> = emptyMap(),
    @SerialName("started_at")
    val startedAt: Instant,
    @SerialName("completed_at")
    val completedAt: Instant? = null,
    @SerialName("abandoned_at")
    val abandonedAt: Instant? = null,
) {
    /**
     * Whether this session's snapshot supports data capture. Feature detection is
     * by version: v3 baked in criterion + enabled Observation Types, v1/v2 did
     * not. Every capture affordance keys off this one predicate.
     */
    val supportsDataCapture: Boolean get() = snapshotVersion >= 3
}
