package com.loganmartlew.rangework.shared.library.editor

import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.ValidationIssue
import com.loganmartlew.rangework.shared.model.ValidationTarget
import com.loganmartlew.rangework.shared.model.validationIssues

/**
 * Result of reviewing a draft input: either [Valid] with a ready-to-save draft,
 * or [Invalid] with the input carrying field-level error slots.
 */
sealed interface DraftReview<out Draft, out Input> {
    data class Valid<Draft>(val draft: Draft) : DraftReview<Draft, Nothing>
    data class Invalid<Input>(val input: Input, val issues: List<ValidationIssue>) : DraftReview<Nothing, Input>
}

/**
 * Stateless Draft editor that owns the full parse → validate → place cycle
 * for both Practice Unit and Practice Session drafts.
 *
 * No dependencies → directly testable.
 */
object PracticeDraftEditor {

    // ── Public API ────────────────────────────────────────────────────────

    fun reviewUnit(input: PracticeUnitDraftInput): DraftReview<PracticeUnitDraft, PracticeUnitDraftInput> =
        reviewDraft(
            input = input,
            parse = { parseUnitInput(it) },
            validate = { it.validationIssues() },
            place = { input, issues -> placeUnitErrors(input, issues) },
        )

    fun reviewSession(input: PracticeSessionDraftInput): DraftReview<PracticeSessionDraft, PracticeSessionDraftInput> =
        reviewDraft(
            input = input,
            parse = { parseSessionInput(it) },
            validate = { it.validationIssues() },
            place = { input, issues -> placeSessionErrors(input, issues) },
        )

    // ── Generic cycle (no per-domain duplication) ──────────────────────

    private fun <Input, Draft> reviewDraft(
        input: Input,
        parse: (Input) -> ParseResult<Draft>,
        validate: (Draft) -> List<ValidationIssue>,
        place: (Input, List<ValidationIssue>) -> Input,
    ): DraftReview<Draft, Input> {
        // 1. Parse raw input → draft + parse issues
        val (draft, parseIssues) = parse(input)

        // 2. If parse issues → Invalid with placed errors
        if (parseIssues.isNotEmpty()) {
            return DraftReview.Invalid(place(input, parseIssues), parseIssues)
        }

        @Suppress("UNCHECKED_CAST")
        val safeDraft = draft as Draft

        // 3. Rule validation
        val ruleIssues = validate(safeDraft)

        // 4. If rule issues → Invalid with placed errors
        if (ruleIssues.isNotEmpty()) {
            return DraftReview.Invalid(place(input, ruleIssues), ruleIssues)
        }

        // 5. Valid
        return DraftReview.Valid(safeDraft)
    }

    // ── Per-domain parsing ─────────────────────────────────────────────

    private data class ParseResult<out Draft>(
        val draft: Draft?,
        val issues: List<ValidationIssue>,
    )

    private fun parseUnitInput(input: PracticeUnitDraftInput): ParseResult<PracticeUnitDraft> {
        val parseIssues = mutableListOf<ValidationIssue>()

        val instructions = input.instructions.mapIndexed { index, instr ->
            val ballCount = parseOptionalBallCount(instr.ballCount, index, parseIssues)
            PracticeInstructionDraft(
                order = instr.order,
                text = instr.text,
                ballCount = ballCount,
            )
        }

        return ParseResult(
            draft = if (parseIssues.isEmpty()) {
                PracticeUnitDraft(
                    title = input.title,
                    instructions = instructions,
                    notes = input.notes,
                    focus = input.focus,
                    defaultClubCode = input.defaultClubCode,
                    tagIds = input.tagIds,
                )
            } else null,
            issues = parseIssues,
        )
    }

    private fun parseSessionInput(input: PracticeSessionDraftInput): ParseResult<PracticeSessionDraft> {
        val parseIssues = mutableListOf<ValidationIssue>()

        val items = input.items.mapIndexed { index, item ->
            val repeatCount = parseOptionalInt(item.repeatCount, index, parseIssues)
            PracticeSessionItemDraft(
                practiceUnitId = item.practiceUnitId,
                order = item.order,
                repeatCount = repeatCount,
                clubCode = item.clubCode,
                notes = item.notes,
                focusCue = item.focusCue,
            )
        }

        return ParseResult(
            draft = if (parseIssues.isEmpty()) {
                PracticeSessionDraft(
                    name = input.name,
                    items = items,
                    notes = input.notes,
                    tagIds = input.tagIds,
                )
            } else null,
            issues = parseIssues,
        )
    }

    private fun parseOptionalBallCount(raw: String, index: Int, issues: MutableList<ValidationIssue>): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toIntOrNull()?.let { value ->
            if (value < 0) {
                issues += ValidationIssue(
                    target = ValidationTarget.InstructionBallCount(index),
                    message = "Ball count must be a positive number.",
                )
                return@let null
            }
            value
        } ?: run {
            issues += ValidationIssue(
                target = ValidationTarget.InstructionBallCount(index),
                message = "Ball count must be a whole number.",
            )
            null
        }
    }

    private fun parseOptionalInt(raw: String, index: Int, issues: MutableList<ValidationIssue>): Int {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            issues += ValidationIssue(
                target = ValidationTarget.ItemRepeatCount(index),
                message = "Repeat count is required.",
            )
            return 0
        }
        return trimmed.toIntOrNull()?.let { value ->
            if (value < 0) {
                issues += ValidationIssue(
                    target = ValidationTarget.ItemRepeatCount(index),
                    message = "Repeat count must be a positive number.",
                )
                return@let 0
            }
            value
        } ?: run {
            issues += ValidationIssue(
                target = ValidationTarget.ItemRepeatCount(index),
                message = "Repeat count must be a whole number.",
            )
            0
        }
    }

    // ── Per-domain error placement (replaces the regex remap) ──────────

    fun placeUnitErrors(
        input: PracticeUnitDraftInput,
        issues: List<ValidationIssue>,
    ): PracticeUnitDraftInput {
        var updated = input
        for (issue in issues) {
            updated = when (val target = issue.target) {
                is ValidationTarget.UnitTitle -> updated.copy(titleError = issue.message)
                is ValidationTarget.UnitInstructions -> updated.copy(titleError = issue.message)
                is ValidationTarget.InstructionText -> updated.copy(
                    instructions = updated.instructions.mapIndexed { i, instr ->
                        if (i == target.index) instr.copy(textError = issue.message) else instr
                    },
                )
                is ValidationTarget.InstructionBallCount -> updated.copy(
                    instructions = updated.instructions.mapIndexed { i, instr ->
                        if (i == target.index) instr.copy(ballCountError = issue.message) else instr
                    },
                )
                is ValidationTarget.SessionName,
                is ValidationTarget.ItemUnitReference,
                is ValidationTarget.ItemRepeatCount,
                is ValidationTarget.Tags -> updated // not applicable for units
            }
        }
        return updated
    }

    fun placeSessionErrors(
        input: PracticeSessionDraftInput,
        issues: List<ValidationIssue>,
    ): PracticeSessionDraftInput {
        var updated = input
        for (issue in issues) {
            updated = when (val target = issue.target) {
                is ValidationTarget.SessionName -> updated.copy(nameError = issue.message)
                is ValidationTarget.ItemUnitReference -> updated.copy(
                    items = updated.items.mapIndexed { i, item ->
                        if (i == target.index) item.copy(unitError = issue.message) else item
                    },
                )
                is ValidationTarget.ItemRepeatCount -> updated.copy(
                    items = updated.items.mapIndexed { i, item ->
                        if (i == target.index) item.copy(repeatCountError = issue.message) else item
                    },
                )
                is ValidationTarget.UnitTitle,
                is ValidationTarget.UnitInstructions,
                is ValidationTarget.InstructionText,
                is ValidationTarget.InstructionBallCount,
                is ValidationTarget.Tags -> updated // not applicable for sessions
            }
        }
        return updated
    }
}