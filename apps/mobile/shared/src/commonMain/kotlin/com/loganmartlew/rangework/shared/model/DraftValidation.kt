package com.loganmartlew.rangework.shared.model

fun PracticeUnitDraft.validationIssues(): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    val normalizedTitle = title.trim()

    instructions
        .sortedBy(PracticeInstructionDraft::order)
        .forEachIndexed { index, instruction ->
            if (instruction.text.trim().isEmpty()) {
                issues += ValidationIssue(
                    target = ValidationTarget.InstructionText(index),
                    message = "Instruction text cannot be blank.",
                )
            }
            if (instruction.ballCount != null && instruction.ballCount < 0) {
                issues += ValidationIssue(
                    target = ValidationTarget.InstructionBallCount(index),
                    message = "Ball count cannot be negative.",
                )
            }
        }

    if (normalizedTitle.isEmpty()) {
        issues += ValidationIssue(
            target = ValidationTarget.UnitTitle,
            message = "Title cannot be blank.",
        )
    }
    if (instructions.isEmpty()) {
        issues += ValidationIssue(
            target = ValidationTarget.UnitInstructions,
            message = "At least one instruction is required.",
        )
    }

    issues += tagValidationIssues(tagIds)

    return issues
}

fun PracticeUnitDraft.validated(): PracticeUnitDraft {
    val issues = validationIssues()

    val normalizedTitle = title.trim()
    val normalizedInstructions = instructions
        .sortedBy(PracticeInstructionDraft::order)
        .mapIndexed { index, instruction ->
            PracticeInstructionDraft(
                order = index + 1,
                text = instruction.text.trim(),
                ballCount = instruction.ballCount,
                clubCode = instruction.clubCode.normalizedOptionalText(),
            )
        }

    if (issues.isNotEmpty()) {
        throw SharedValidationException(issues)
    }

    return copy(
        title = normalizedTitle,
        instructions = normalizedInstructions,
        notes = notes.normalizedOptionalText(),
        focus = focus.normalizedOptionalText(),
        defaultClubCode = defaultClubCode.normalizedOptionalText(),
        successCriterion = successCriterion.normalizedOptionalText(),
        tagIds = tagIds.normalizedTagIds(),
    )
}

fun PracticeSessionDraft.validationIssues(): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    val normalizedName = name.trim()

    items
        .sortedBy(PracticeSessionItemDraft::order)
        .forEachIndexed { index, item ->
            if (item.practiceUnitId.trim().isEmpty()) {
                issues += ValidationIssue(
                    target = ValidationTarget.ItemUnitReference(index),
                    message = "Every session item must reference a practice unit.",
                )
            }
            if (item.repeatCount <= 0) {
                issues += ValidationIssue(
                    target = ValidationTarget.ItemRepeatCount(index),
                    message = "Repeat count must be greater than zero.",
                )
            }
        }

    if (normalizedName.isEmpty()) {
        issues += ValidationIssue(
            target = ValidationTarget.SessionName,
            message = "Session name cannot be blank.",
        )
    }

    issues += tagValidationIssues(tagIds)

    return issues
}

fun PracticeSessionDraft.validated(): PracticeSessionDraft {
    val issues = validationIssues()

    val normalizedName = name.trim()
    val normalizedItems = items
        .sortedBy(PracticeSessionItemDraft::order)
        .mapIndexed { index, item ->
            PracticeSessionItemDraft(
                practiceUnitId = item.practiceUnitId.trim(),
                order = index + 1,
                repeatCount = item.repeatCount,
                clubCode = item.clubCode.normalizedOptionalText(),
                notes = item.notes.normalizedOptionalText(),
                focusCue = item.focusCue.normalizedOptionalText(),
                observationTypes = item.observationTypes.normalizedObservationTypes(),
            )
        }

    if (issues.isNotEmpty()) {
        throw SharedValidationException(issues)
    }

    return copy(
        name = normalizedName,
        items = normalizedItems,
        notes = notes.normalizedOptionalText(),
        tagIds = tagIds.normalizedTagIds(),
    )
}

/**
 * Normalizes the presentation-derived unit fields for the IMPERIAL/METRIC
 * presets while preserving every other field (notably [MeasurementPreferences.handedness],
 * which is orthogonal to the unit system). `copy(...)` — not the static presets —
 * so a non-unit-system field is never clobbered.
 */
fun MeasurementPreferences.validated(): MeasurementPreferences = when (unitSystem) {
    UnitSystem.IMPERIAL -> copy(
        unitSystem = UnitSystem.IMPERIAL,
        distanceUnit = DistanceUnit.YARDS,
        speedUnit = SpeedUnit.MILES_PER_HOUR,
    )
    UnitSystem.METRIC -> copy(
        unitSystem = UnitSystem.METRIC,
        distanceUnit = DistanceUnit.METERS,
        speedUnit = SpeedUnit.KILOMETRES_PER_HOUR,
    )
    UnitSystem.CUSTOM -> copy()
}

/** Blank/whitespace-only text normalizes to null; otherwise trimmed. */
internal fun String?.normalizedOptionalText(): String? = this
    ?.trim()
    ?.takeIf(String::isNotEmpty)

/** De-duplicated Observation Types in catalog (enum declaration) order. */
private fun List<ObservationType>.normalizedObservationTypes(): List<ObservationType> =
    distinct().sortedBy(ObservationType::ordinal)

/** De-duplicated, order-preserving tag ids with blanks removed. */
private fun List<String>.normalizedTagIds(): List<String> = this
    .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
    .distinct()

private fun tagValidationIssues(tagIds: List<String>): List<ValidationIssue> {
    if (tagIds.normalizedTagIds().size > MAX_TAGS_PER_ITEM) {
        return listOf(
            ValidationIssue(
                target = ValidationTarget.Tags,
                message = "At most $MAX_TAGS_PER_ITEM tags can be attached.",
            ),
        )
    }
    return emptyList()
}
