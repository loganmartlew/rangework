package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft

interface PracticeUnitRepository {
    suspend fun listPracticeUnits(): List<PracticeUnit>

    suspend fun getPracticeUnit(unitId: String): PracticeUnit?

    suspend fun savePracticeUnit(
        draft: PracticeUnitDraft,
        unitId: String? = null,
    ): PracticeUnit

    suspend fun deletePracticeUnit(unitId: String)
}
