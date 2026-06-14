package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.auth.AuthRepository
import com.loganmartlew.rangework.shared.auth.AuthState
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthState> = authRepository.authStates
}
