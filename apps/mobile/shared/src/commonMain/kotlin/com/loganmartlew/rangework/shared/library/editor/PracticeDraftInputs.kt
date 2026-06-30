package com.loganmartlew.rangework.shared.library.editor

import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

/**
 * Raw-input editor models for the Draft editor.
 * Moved from Android PracticePlannerViewModel to shared so parse/validate/place
 * can be owned by a single module.
 *
 * Vocabulary matches CONTEXT.md: Practice Unit, Practice Instruction, Ball Count,
 * Practice Session, Session Item, Repeat Count.
 */

// ── Practice Unit draft input ────────────────────────────────────────

data class PracticeUnitDraftInput(
    val unitId: String? = null,
    val title: String = "",
    val notes: String = "",
    val focus: String = "",
    val defaultClubCode: String = "",
    val instructions: List<PracticeInstructionDraftInput> = listOf(
        PracticeInstructionDraftInput(order = 1),
    ),
    val titleError: String? = null,
) {
    fun withoutErrors() = copy(
        titleError = null,
        instructions = instructions.map { it.withoutErrors() },
    )
}

data class PracticeInstructionDraftInput(
    val order: Int,
    val text: String = "",
    val ballCount: String = "1",
    val textError: String? = null,
    val ballCountError: String? = null,
) {
    fun withoutErrors() = copy(textError = null, ballCountError = null)
}

// ── Practice Session draft input ─────────────────────────────────────

data class PracticeSessionDraftInput(
    val sessionId: String? = null,
    val name: String = "",
    val notes: String = "",
    val items: List<PracticeSessionItemDraftInput> = emptyList(),
    val nameError: String? = null,
) {
    fun withoutErrors() = copy(
        nameError = null,
        items = items.map { it.withoutErrors() },
    )
}

data class PracticeSessionItemDraftInput(
    val order: Int,
    val practiceUnitId: String = "",
    val repeatCount: String = "1",
    val clubCode: String = "",
    val notes: String = "",
    val focusCue: String = "",
    val unitError: String? = null,
    val repeatCountError: String? = null,
) {
    fun withoutErrors() = copy(unitError = null, repeatCountError = null)

    /**
     * Derives the ball count for this session item from the given [unit].
     * Replaces the local copy previously in SessionEditorScreen.
     * Built on [PracticeUnit.derivedBallCount].
     */
    fun derivedBallCount(unit: PracticeUnit?): Int? {
        val repeats = repeatCount.trim().toIntOrNull() ?: return unit?.derivedBallCount()
        return unit?.derivedBallCount()?.times(repeats)
    }
}