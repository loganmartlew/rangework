package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.repository.ClubRepository

class GetClubCatalogUseCase(
    private val clubRepository: ClubRepository,
) {
    suspend operator fun invoke(): List<Club> = clubRepository.listCatalog()
}

class GetEnabledClubsUseCase(
    private val clubRepository: ClubRepository,
) {
    suspend operator fun invoke(): Set<String> = clubRepository.getEnabledClubCodes()
}

class SetClubEnabledUseCase(
    private val clubRepository: ClubRepository,
) {
    suspend operator fun invoke(code: String, enabled: Boolean) =
        clubRepository.setClubEnabled(code, enabled)
}
