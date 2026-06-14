package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeUnitDraft

interface PracticeSessionRepository {
    suspend fun listUnits(): List<PracticeUnitDraft>
}
