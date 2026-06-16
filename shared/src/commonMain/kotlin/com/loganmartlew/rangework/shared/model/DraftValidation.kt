package com.loganmartlew.rangework.shared.model

fun PracticeUnitDraft.validationIssues(): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    val normalizedTitle = title.trim()

    instructions
        .sortedBy(PracticeInstructionDraft::order)
        .forEachIndexed { index, instruction ->
            if (instruction.text.trim().isEmpty()) {
                issues += ValidationIssue(
                    field = "instructions[$index].text",
                    message = "Instruction text cannot be blank.",
                )
            }
            if (instruction.ballCount != null && instruction.ballCount <= 0) {
                issues += ValidationIssue(
                    field = "instructions[$index].ballCount",
                    message = "Ball count must be greater than zero.",
                )
            }
        }

    if (normalizedTitle.isEmpty()) {
        issues += ValidationIssue(
            field = "title",
            message = "Title cannot be blank.",
        )
    }
    if (instructions.isEmpty()) {
        issues += ValidationIssue(
            field = "instructions",
            message = "At least one instruction is required.",
        )
    }

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
        defaultClubReference = defaultClubReference.normalizedOptionalText(),
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
                    field = "items[$index].practiceUnitId",
                    message = "Every session item must reference a practice unit.",
                )
            }
            if (item.repeatCount <= 0) {
                issues += ValidationIssue(
                    field = "items[$index].repeatCount",
                    message = "Repeat count must be greater than zero.",
                )
            }
        }

    if (normalizedName.isEmpty()) {
        issues += ValidationIssue(
            field = "name",
            message = "Session name cannot be blank.",
        )
    }

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
                clubReference = item.clubReference.normalizedOptionalText(),
                notes = item.notes.normalizedOptionalText(),
                focusCue = item.focusCue.normalizedOptionalText(),
            )
        }

    if (issues.isNotEmpty()) {
        throw SharedValidationException(issues)
    }

    return copy(
        name = normalizedName,
        items = normalizedItems,
        notes = notes.normalizedOptionalText(),
    )
}

fun MeasurementPreferences.validated(): MeasurementPreferences = when (unitSystem) {
    UnitSystem.IMPERIAL -> MeasurementPreferences.Imperial
    UnitSystem.METRIC -> MeasurementPreferences.Metric
    UnitSystem.CUSTOM -> copy()
}

private fun String?.normalizedOptionalText(): String? = this
    ?.trim()
    ?.takeIf(String::isNotEmpty)
