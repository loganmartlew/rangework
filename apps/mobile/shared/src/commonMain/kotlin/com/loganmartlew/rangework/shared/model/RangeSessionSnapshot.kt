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
)

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
