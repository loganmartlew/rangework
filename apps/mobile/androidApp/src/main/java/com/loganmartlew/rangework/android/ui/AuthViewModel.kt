package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.android.auth.GoogleIdTokenProvider
import com.loganmartlew.rangework.android.auth.GoogleIdTokenRequestResult
import com.loganmartlew.rangework.android.config.AndroidAppAuthConfig
import com.loganmartlew.rangework.shared.auth.AuthFoundation
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.auth.createAuthFoundation
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val environment: AppEnvironment,
    val authState: AuthState,
    val userProfile: UserProfile? = null,
    val actionInProgress: Boolean = false,
    val statusMessage: String? = null,
)

class AuthViewModel(
    private val androidAuthConfig: AndroidAppAuthConfig,
    private val authFoundation: AuthFoundation? = createAuthFoundation(
        androidAuthConfig.environment.supabaseConfig,
    ),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(
            environment = androidAuthConfig.environment,
            authState = if (authFoundation == null) AuthState.SignedOut else AuthState.Restoring,
            statusMessage = if (authFoundation == null) {
                missingConfigMessage(androidAuthConfig.environment)
            } else {
                null
            },
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val foundation = authFoundation
        if (foundation != null) {
            viewModelScope.launch {
                foundation.observeAuthStateUseCase().collect { authState ->
                    _uiState.update { state ->
                        state.copy(authState = authState)
                    }
                    if (authState is AuthState.SignedIn) {
                        fetchProfile(foundation)
                    } else if (authState is AuthState.SignedOut) {
                        _uiState.update { state -> state.copy(userProfile = null) }
                    }
                }
            }

            restoreSession()
        }
    }

    fun restoreSession() {
        val foundation = authFoundation ?: return markMissingConfig()

        launchAuthAction {
            authStateMessage(foundation.restoreAuthSessionUseCase())
        }
    }

    fun signInWithGoogle(googleIdTokenProvider: GoogleIdTokenProvider) {
        if (!androidAuthConfig.environment.isAuthConfigured) {
            markMissingConfig()
            return
        }

        val foundation = authFoundation ?: return markMissingConfig()

        launchAuthAction {
            when (val tokenResult = googleIdTokenProvider.requestIdToken()) {
                is GoogleIdTokenRequestResult.Cancelled -> tokenResult.message
                is GoogleIdTokenRequestResult.Failure -> tokenResult.message
                is GoogleIdTokenRequestResult.Success -> {
                    try {
                        authStateMessage(
                            foundation.signInWithGoogleIdTokenUseCase(
                                idToken = tokenResult.idToken,
                            ),
                        )
                    } catch (exception: IllegalStateException) {
                        exception.message ?: "Sign-in could not start."
                    } catch (exception: IllegalArgumentException) {
                        exception.message ?: "Sign-in was rejected."
                    } catch (exception: Exception) {
                        exception.message ?: "Sign-in failed."
                    }
                }
            }
        }
    }

    fun signOut() {
        val foundation = authFoundation ?: return markMissingConfig()

        launchAuthAction {
            try {
                foundation.signOutUseCase()
                "Signed out."
            } catch (exception: IllegalStateException) {
                exception.message ?: "Sign out failed."
            } catch (exception: Exception) {
                exception.message ?: "Sign out failed."
            }
        }
    }

    private fun fetchProfile(foundation: AuthFoundation) {
        viewModelScope.launch {
            try {
                val profile = foundation.getUserProfileUseCase()
                _uiState.update { state -> state.copy(userProfile = profile) }
            } catch (_: Exception) {
                // Profile display is supplementary — a fetch failure doesn't break auth
            }
        }
    }

    private fun markMissingConfig() {
        _uiState.update { state ->
            state.copy(
                statusMessage = missingConfigMessage(state.environment),
                actionInProgress = false,
            )
        }
    }

    private fun launchAuthAction(action: suspend () -> String) {
        if (_uiState.value.actionInProgress) {
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(actionInProgress = true) }

            val statusMessage = action()

            _uiState.update { state ->
                state.copy(
                    actionInProgress = false,
                    statusMessage = statusMessage,
                )
            }
        }
    }

    companion object {
        fun factory(
            androidAuthConfig: AndroidAppAuthConfig,
            authFoundation: AuthFoundation? = createAuthFoundation(
                androidAuthConfig.environment.supabaseConfig,
            ),
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }

                    return AuthViewModel(
                        androidAuthConfig = androidAuthConfig,
                        authFoundation = authFoundation,
                    ) as T
                }
            }
    }
}
