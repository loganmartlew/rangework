package com.loganmartlew.rangework.shared.model

/**
 * A structured target identifying which field (and optional index) a [ValidationIssue] refers to.
 * Vocabulary matches CONTEXT.md: Practice Unit, Practice Instruction, Ball Count, Practice Session,
 * Session Item, Repeat Count.
 */
sealed interface ValidationTarget {
    // Practice Unit
    data object UnitTitle : ValidationTarget
    data object UnitInstructions : ValidationTarget // "at least one instruction"
    data class InstructionText(val index: Int) : ValidationTarget
    data class InstructionBallCount(val index: Int) : ValidationTarget

    // Practice Session
    data object SessionName : ValidationTarget
    data class ItemUnitReference(val index: Int) : ValidationTarget
    data class ItemRepeatCount(val index: Int) : ValidationTarget

    fun label(): String = when (this) {
        UnitTitle -> "title"
        UnitInstructions -> "instructions"
        is InstructionText -> "instructions[$index].text"
        is InstructionBallCount -> "instructions[$index].ballCount"
        SessionName -> "name"
        is ItemUnitReference -> "items[$index].practiceUnitId"
        is ItemRepeatCount -> "items[$index].repeatCount"
    }
}

data class ValidationIssue(
    val target: ValidationTarget,
    val message: String,
) {
    @Deprecated("Use target instead", replaceWith = ReplaceWith("target.label()"))
    val field: String get() = target.label()
}

class SharedValidationException(
    val issues: List<ValidationIssue>,
) : IllegalArgumentException(
    issues.joinToString(separator = "; ") { issue -> "${issue.target.label()}: ${issue.message}" },
)
