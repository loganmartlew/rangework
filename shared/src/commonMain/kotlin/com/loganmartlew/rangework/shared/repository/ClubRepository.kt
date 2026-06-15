package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.Club

interface ClubRepository {
    suspend fun listCatalog(): List<Club>
    suspend fun getEnabledClubCodes(): Set<String>
    suspend fun setClubEnabled(code: String, enabled: Boolean)
}
