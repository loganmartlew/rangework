package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class RangeSessionSnapshot(
    val sessionNotes: String? = null,
    val units: List<SnapshotUnit>,
    val steps: List<SnapshotStep>,
)

@Serializable
data class SnapshotUnit(
    val unitTitle: String,
    val unitNotes: String? = null,
    val unitFocus: String? = null,
    val itemNotes: String? = null,
    val itemFocusCue: String? = null,
    val repeatCount: Int,
    val instructions: List<SnapshotInstruction>,
    /** The unit's Success Criterion in force at session start; null when absent. */
    val successCriterion: String? = null,
    /**
     * The item's enabled Observation Type wire ids, baked in at start. Kept as
     * raw strings for wire tolerance; read through [enabledObservationTypes].
     */
    val observationTypes: List<String> = emptyList(),
)

/**
 * The typed, known Observation Types enabled for this unit entry, in the order
 * stored. Drops ids this app version doesn't recognise (forward-compat), and
 * filters `SUCCESS` when there is no criterion — belt-and-braces beside the RPC
 * filter, since derived counting is meaningless without a rubric.
 */
val SnapshotUnit.enabledObservationTypes: List<ObservationType>
    get() = observationTypes
        .mapNotNull(ObservationType::fromId)
        .distinct()
        .let { types ->
            if (successCriterion == null) types.filter { it != ObservationType.SUCCESS } else types
        }

@Serializable
data class SnapshotInstruction(
    val text: String,
    val ballCount: Int? = null,
    val club: String? = null,
    val clubDisplayName: String? = null,
)

@Serializable
data class SnapshotStep(
    val unitIndex: Int,
    val instructionIndex: Int,
    val repNumber: Int,
    val totalReps: Int,
    val instructionText: String,
    val ballCount: Int? = null,
    val club: String? = null,
    val clubDisplayName: String? = null,
    val unitTitle: String,
    val notes: String? = null,
    val focusCue: String? = null,
)
