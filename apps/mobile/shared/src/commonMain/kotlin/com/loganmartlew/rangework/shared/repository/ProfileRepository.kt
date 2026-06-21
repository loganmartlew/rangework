package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.UserProfile

interface ProfileRepository {
    suspend fun getUserProfile(): UserProfile
}
