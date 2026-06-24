package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.validated

abstract class PracticeUnitRepository {
    suspend fun save(draft: PracticeUnitDraft, unitId: String? = null): PracticeUnit =
        persist(draft.validated(), unitId?.trim()?.takeIf(String::isNotEmpty))

    suspend fun duplicate(id: String): PracticeUnit {
        val unit = get(id) ?: error("Unit $id not found")
        return persist(
            PracticeUnitDraft(
                title = unit.title,
                notes = unit.notes,
                focus = unit.focus,
                defaultClubCode = unit.defaultClubCode,
                instructions = unit.instructions.map { instruction ->
                    PracticeInstructionDraft(
                        order = instruction.order,
                        text = instruction.text,
                        ballCount = instruction.ballCount,
                    )
                },
            ),
            null,
        )
    }

    protected abstract suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit
    abstract suspend fun get(id: String): PracticeUnit?
    abstract suspend fun list(): List<PracticeUnit>
    abstract suspend fun delete(id: String)
}
