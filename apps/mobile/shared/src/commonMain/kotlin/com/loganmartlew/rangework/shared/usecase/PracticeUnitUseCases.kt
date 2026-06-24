package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.validated
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository

class ListPracticeUnitsUseCase(
    private val practiceUnitRepository: PracticeUnitRepository,
) {
    suspend operator fun invoke(): List<PracticeUnit> = practiceUnitRepository.listPracticeUnits()
}

class GetPracticeUnitUseCase(
    private val practiceUnitRepository: PracticeUnitRepository,
) {
    suspend operator fun invoke(unitId: String): PracticeUnit? = practiceUnitRepository.getPracticeUnit(unitId)
}

class SavePracticeUnitUseCase(
    private val practiceUnitRepository: PracticeUnitRepository,
) {
    suspend operator fun invoke(
        draft: PracticeUnitDraft,
        unitId: String? = null,
    ): PracticeUnit = practiceUnitRepository.savePracticeUnit(
        draft = draft.validated(),
        unitId = unitId?.trim()?.takeIf(String::isNotEmpty),
    )
}

class DeletePracticeUnitUseCase(
    private val practiceUnitRepository: PracticeUnitRepository,
) {
    suspend operator fun invoke(unitId: String) {
        practiceUnitRepository.deletePracticeUnit(unitId.trim())
    }
}

class DuplicateUnitUseCase(
    private val getPracticeUnitUseCase: GetPracticeUnitUseCase,
    private val savePracticeUnitUseCase: SavePracticeUnitUseCase,
) {
    suspend operator fun invoke(unitId: String): PracticeUnit {
        val original = getPracticeUnitUseCase(unitId)
            ?: error("Unit $unitId not found")
        val draft = PracticeUnitDraft(
            title = original.title,
            notes = original.notes,
            focus = original.focus,
            defaultClubCode = original.defaultClubCode,
            instructions = original.instructions.map { instruction ->
                PracticeInstructionDraft(
                    order = instruction.order,
                    text = instruction.text,
                    ballCount = instruction.ballCount,
                )
            },
        )
        return savePracticeUnitUseCase(draft)
    }
}
