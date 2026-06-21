package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.UserProfile
import com.loganmartlew.rangework.shared.repository.ProfileRepository

class GetUserProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): UserProfile = profileRepository.getUserProfile()
}
