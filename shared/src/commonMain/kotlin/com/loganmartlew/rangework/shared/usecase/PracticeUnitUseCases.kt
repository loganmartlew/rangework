package com.loganmartlew.rangework.shared.usecase

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
