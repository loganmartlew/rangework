package com.loganmartlew.rangework.shared.model

fun PracticeUnitDraft.validated(): PracticeUnitDraft {
    val issues = mutableListOf<ValidationIssue>()
    val normalizedTitle = title.trim()
    val normalizedInstructions = instructions
        .sortedBy(PracticeInstructionDraft::order)
        .mapIndexed { index, instruction ->
            val normalizedText = instruction.text.trim()
            val normalizedClubReference = instruction.clubReference.normalizedOptionalText()

            if (normalizedText.isEmpty()) {
                issues += ValidationIssue(
                    field = "instructions[$index].text",
                    message = "Instruction text cannot be blank.",
                )
            }
            if (instruction.repCount != null && instruction.repCount <= 0) {
                issues += ValidationIssue(
                    field = "instructions[$index].repCount",
                    message = "Repetition count must be greater than zero.",
                )
            }
            if (instruction.ballCount != null && instruction.ballCount <= 0) {
                issues += ValidationIssue(
                    field = "instructions[$index].ballCount",
                    message = "Ball count must be greater than zero.",
                )
            }

            PracticeInstructionDraft(
                order = index + 1,
                text = normalizedText,
                clubReference = normalizedClubReference,
                repCount = instruction.repCount,
                ballCount = instruction.ballCount,
            )
        }
    val normalizedTags = tags
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()

    if (normalizedTitle.isEmpty()) {
        issues += ValidationIssue(
            field = "title",
            message = "Title cannot be blank.",
        )
    }
    if (normalizedInstructions.isEmpty()) {
        issues += ValidationIssue(
            field = "instructions",
            message = "At least one instruction is required.",
        )
    }
    if (defaultBallCount != null && defaultBallCount <= 0) {
        issues += ValidationIssue(
            field = "defaultBallCount",
            message = "Default ball count must be greater than zero.",
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
        tags = normalizedTags,
        defaultBallCount = defaultBallCount,
    )
}

fun PracticeSessionDraft.validated(): PracticeSessionDraft {
    val issues = mutableListOf<ValidationIssue>()
    val normalizedName = name.trim()
    val normalizedItems = items
        .sortedBy(PracticeSessionItemDraft::order)
        .mapIndexed { index, item ->
            val normalizedPracticeUnitId = item.practiceUnitId.trim()

            if (normalizedPracticeUnitId.isEmpty()) {
                issues += ValidationIssue(
                    field = "items[$index].practiceUnitId",
                    message = "Every session item must reference a practice unit.",
                )
            }
            if (item.restSeconds != null && item.restSeconds <= 0) {
                issues += ValidationIssue(
                    field = "items[$index].restSeconds",
                    message = "Rest seconds must be greater than zero.",
                )
            }
            if (item.overrideBallCount != null && item.overrideBallCount <= 0) {
                issues += ValidationIssue(
                    field = "items[$index].overrideBallCount",
                    message = "Override ball count must be greater than zero.",
                )
            }

            PracticeSessionItemDraft(
                practiceUnitId = normalizedPracticeUnitId,
                order = index + 1,
                notes = item.notes.normalizedOptionalText(),
                focusCue = item.focusCue.normalizedOptionalText(),
                restSeconds = item.restSeconds,
                overrideBallCount = item.overrideBallCount,
            )
        }

    if (normalizedName.isEmpty()) {
        issues += ValidationIssue(
            field = "name",
            message = "Session name cannot be blank.",
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
