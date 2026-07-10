package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InMemoryPracticeUnitRepository : PracticeUnitRepository() {
    private val store = mutableMapOf<String, PracticeUnit>()
    val drafts = mutableListOf<PracticeUnitDraft>()

    override suspend fun list(): List<PracticeUnit> =
        store.values.sortedByDescending { it.updatedAt }

    override suspend fun get(id: String): PracticeUnit? = store[id]

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit {
        val resolvedId = unitId ?: Uuid.random().toString()
        val now = Clock.System.now()
        val unit = PracticeUnit(
            id = resolvedId,
            title = validated.title,
            instructions = validated.instructions.map { instruction ->
                PracticeInstruction(
                    id = Uuid.random().toString(),
                    order = instruction.order,
                    text = instruction.text,
                    ballCount = instruction.ballCount,
                    clubCode = instruction.clubCode,
                )
            },
            notes = validated.notes,
            focus = validated.focus,
            defaultClubCode = validated.defaultClubCode,
            successCriterion = validated.successCriterion,
            createdAt = store[resolvedId]?.createdAt ?: now,
            updatedAt = now,
        )
        store[resolvedId] = unit
        drafts += validated
        return unit
    }

    override suspend fun delete(id: String) {
        store.remove(id)
    }
}