package com.loganmartlew.rangework.shared.library

import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.model.ValidationIssue
import com.loganmartlew.rangework.shared.model.ValidationTarget
import com.loganmartlew.rangework.shared.model.validated
import com.loganmartlew.rangework.shared.model.validationIssues
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import kotlinx.datetime.Clock

class DefaultPracticeLibrary(
    private val unitRepository: PracticeUnitRepository,
    private val sessionRepository: PracticeSessionRepository,
) : PracticeLibrary {

    // ── Units ─────────────────────────────────────────────────────────

    override suspend fun listUnits(): List<PracticeUnit> = unitRepository.list()

    override suspend fun getUnit(id: String): PracticeUnit? = unitRepository.get(id)

    override fun validateUnit(draft: PracticeUnitDraft): List<ValidationIssue> =
        draft.validationIssues()

    override suspend fun saveUnit(draft: PracticeUnitDraft, unitId: String?): PracticeLibraryResult<PracticeUnit> {
        val issues = draft.validationIssues()
        if (issues.isNotEmpty()) {
            return PracticeLibraryResult.Invalid(issues)
        }
        val normalized = draft.validated()
        val resolvedId = unitId?.trim()?.takeIf(String::isNotEmpty)
        val saved = unitRepository.persist(normalized, resolvedId)
        return PracticeLibraryResult.Saved(saved)
    }

    override suspend fun duplicateUnit(id: String): PracticeUnit {
        val unit = unitRepository.get(id) ?: error("Unit $id not found")
        val draft = PracticeUnitDraft(
            title = unit.title,
            notes = unit.notes,
            focus = unit.focus,
            defaultClubCode = unit.defaultClubCode,
            successCriterion = unit.successCriterion,
            instructions = unit.instructions.map { instruction ->
                PracticeInstructionDraft(
                    order = instruction.order,
                    text = instruction.text,
                    ballCount = instruction.ballCount,
                    clubCode = instruction.clubCode,
                )
            },
            tagIds = unit.tags.map(Tag::id),
        )
        return unitRepository.persist(draft.validated(), null)
    }

    override suspend fun restoreUnit(unit: PracticeUnit): PracticeUnit {
        val draft = PracticeUnitDraft(
            title = unit.title,
            notes = unit.notes,
            focus = unit.focus,
            defaultClubCode = unit.defaultClubCode,
            successCriterion = unit.successCriterion,
            instructions = unit.instructions.map { instruction ->
                PracticeInstructionDraft(
                    order = instruction.order,
                    text = instruction.text,
                    ballCount = instruction.ballCount,
                    clubCode = instruction.clubCode,
                )
            },
            tagIds = unit.tags.map(Tag::id),
        )
        return unitRepository.persist(draft.validated(), unit.id)
    }

    override suspend fun deleteUnit(id: String) = unitRepository.delete(id)

    // ── Sessions ──────────────────────────────────────────────────────

    override suspend fun listSessions(): List<PracticeSession> = sessionRepository.list()

    override suspend fun listArchivedSessions(): List<PracticeSession> = sessionRepository.listArchived()

    override suspend fun getSession(id: String): PracticeSession? = sessionRepository.get(id)

    override suspend fun validateSession(draft: PracticeSessionDraft): List<ValidationIssue> =
        draft.validationIssues() + successCriterionIssues(draft)

    override suspend fun saveSession(draft: PracticeSessionDraft, sessionId: String?): PracticeLibraryResult<PracticeSession> {
        val issues = validateSession(draft)
        if (issues.isNotEmpty()) {
            return PracticeLibraryResult.Invalid(issues)
        }
        if (sessionId != null) {
            val existing = sessionRepository.get(sessionId)
            if (existing?.archivedAt != null) error("Cannot edit archived session $sessionId; unarchive first")
        }
        val normalized = draft.validated()
        val resolvedId = sessionId?.trim()?.takeIf(String::isNotEmpty)
        val saved = sessionRepository.persist(normalized, resolvedId)
        return PracticeLibraryResult.Saved(saved)
    }

    override suspend fun archiveSession(id: String): PracticeSession =
        sessionRepository.setArchived(id, Clock.System.now())

    override suspend fun unarchiveSession(id: String): PracticeSession =
        sessionRepository.setArchived(id, null)

    /**
     * A friendly mirror of the RPC's success-requires-criterion exception: any
     * item enabling the Success Observation Type whose referenced unit has no
     * Success Criterion is a validation issue. The RPC stays as the backstop.
     */
    private suspend fun successCriterionIssues(draft: PracticeSessionDraft): List<ValidationIssue> {
        // Cache the criterion verdict per unit id so a session referencing the
        // same unit twice only loads it once.
        val missingCriterionByUnit = mutableMapOf<String, Boolean>()
        return draft.items
            .sortedBy(PracticeSessionItemDraft::order)
            .mapIndexedNotNull { index, item ->
                if (ObservationType.SUCCESS !in item.observationTypes) return@mapIndexedNotNull null
                val unitId = item.practiceUnitId.trim().takeIf(String::isNotEmpty)
                    ?: return@mapIndexedNotNull null
                // A unit that can't be loaded (e.g. deleted) is not a criterion
                // problem — leave it to reference validation / the RPC backstop
                // rather than mislabel it as a missing success criterion.
                val missingCriterion = missingCriterionByUnit.getOrPut(unitId) {
                    val unit = unitRepository.get(unitId)
                    unit != null && unit.successCriterion == null
                }
                if (missingCriterion) {
                    ValidationIssue(
                        target = ValidationTarget.ItemObservationTypes(index),
                        message = "Success can only be observed when the practice unit has a success criterion.",
                    )
                } else {
                    null
                }
            }
    }

    override suspend fun duplicateSession(id: String): PracticeSession {
        val session = sessionRepository.get(id) ?: error("Session $id not found")
        val draft = PracticeSessionDraft(
            name = session.name,
            notes = session.notes,
            items = session.items.map { item ->
                PracticeSessionItemDraft(
                    practiceUnitId = item.practiceUnitId,
                    order = item.order,
                    repeatCount = item.repeatCount,
                    clubCode = item.clubCode,
                    notes = item.notes,
                    focusCue = item.focusCue,
                    observationTypes = item.observationTypes,
                )
            },
            tagIds = session.tags.map(Tag::id),
        )
        return sessionRepository.persist(draft.validated(), null)
    }

    override suspend fun restoreSession(session: PracticeSession): PracticeSession {
        val draft = PracticeSessionDraft(
            name = session.name,
            notes = session.notes,
            items = session.items.map { item ->
                PracticeSessionItemDraft(
                    practiceUnitId = item.practiceUnitId,
                    order = item.order,
                    repeatCount = item.repeatCount,
                    clubCode = item.clubCode,
                    notes = item.notes,
                    focusCue = item.focusCue,
                    observationTypes = item.observationTypes,
                )
            },
            tagIds = session.tags.map(Tag::id),
        )
        return sessionRepository.persist(draft.validated(), session.id)
    }

    override suspend fun deleteSession(id: String) = sessionRepository.delete(id)
}
