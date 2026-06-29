package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft

abstract class PracticeUnitRepository {
    abstract suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit
    abstract suspend fun get(id: String): PracticeUnit?
    abstract suspend fun list(): List<PracticeUnit>
    abstract suspend fun delete(id: String)
}
