package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.android.auth.GoogleIdTokenProvider
import com.loganmartlew.rangework.android.auth.GoogleIdTokenRequestResult
import com.loganmartlew.rangework.android.config.AndroidAppAuthConfig
import com.loganmartlew.rangework.shared.auth.AuthFoundation
import com.loganmartlew.rangework.shared.auth.AuthRepository
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.GoogleAuthConfig
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig
import com.loganmartlew.rangework.shared.usecase.ObserveAuthStateUseCase
import com.loganmartlew.rangework.shared.usecase.RestoreAuthSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SignInWithGoogleIdTokenUseCase
import com.loganmartlew.rangework.shared.usecase.SignOutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun configuredViewModelRestoresSavedSessionOnInit() = runTest {
        val viewModel = createViewModel(
            repository = FakeAuthRepository(
                restoreState = AuthState.SignedIn(
                    userId = "user-1",
                    userEmail = "logan@example.com",
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
            viewModel.uiState.value.authState,
        )
        assertEquals(
            "Signed in as logan@example.com.",
            viewModel.uiState.value.statusMessage,
        )
        assertFalse(viewModel.uiState.value.actionInProgress)
    }

    @Test
    fun missingConfigReportsSetupRequirements() = runTest {
        val viewModel = AuthViewModel(
            androidAuthConfig = AndroidAppAuthConfig(
                environment = baselineEnvironment(),
                googleWebClientId = "",
            ),
            authFoundation = null,
        )

        advanceUntilIdle()

        assertEquals(AuthState.SignedOut, viewModel.uiState.value.authState)
        assertTrue(viewModel.uiState.value.statusMessage.orEmpty().contains("rangeworkSupabaseUrl"))
        assertFalse(viewModel.uiState.value.actionInProgress)
    }

    @Test
    fun signInWithGoogleSurfacesProviderFailure() = runTest {
        val viewModel = createViewModel(
            repository = FakeAuthRepository(),
        )

        advanceUntilIdle()
        viewModel.signInWithGoogle(
            googleIdTokenProvider = FakeGoogleIdTokenProvider(
                result = GoogleIdTokenRequestResult.Failure("Google sign-in failed."),
            ),
        )
        advanceUntilIdle()

        assertEquals(AuthState.SignedOut, viewModel.uiState.value.authState)
        assertEquals("Google sign-in failed.", viewModel.uiState.value.statusMessage)
        assertFalse(viewModel.uiState.value.actionInProgress)
    }

    private fun createViewModel(repository: FakeAuthRepository): AuthViewModel {
        val authFoundation = AuthFoundation(
            observeAuthStateUseCase = ObserveAuthStateUseCase(repository),
            restoreAuthSessionUseCase = RestoreAuthSessionUseCase(repository),
            signInWithGoogleIdTokenUseCase = SignInWithGoogleIdTokenUseCase(repository),
            signOutUseCase = SignOutUseCase(repository),
        )

        return AuthViewModel(
            androidAuthConfig = AndroidAppAuthConfig(
                environment = baselineEnvironment(
                    supabaseConfig = SupabaseEndpointConfig(
                        projectUrl = "https://rangework.supabase.co",
                        anonKey = "anon-key",
                    ),
                    googleAuthConfig = GoogleAuthConfig(
                        webClientId = "google-web-client-id",
                    ),
                ),
                googleWebClientId = "google-web-client-id",
            ),
            authFoundation = authFoundation,
        )
    }
}

private class FakeAuthRepository(
    private val restoreState: AuthState = AuthState.SignedOut,
    private val signInState: AuthState = AuthState.SignedIn(
        userId = "user-1",
        userEmail = "logan@example.com",
    ),
) : AuthRepository {
    private val authState = MutableStateFlow<AuthState>(AuthState.Restoring)

    override val authStates: Flow<AuthState> = authState.asStateFlow()

    override suspend fun restoreSession(): AuthState {
        authState.value = restoreState
        return restoreState
    }

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        accessToken: String?,
    ): AuthState {
        authState.value = signInState
        return signInState
    }

    override suspend fun signOut() {
        authState.value = AuthState.SignedOut
    }
}

private class FakeGoogleIdTokenProvider(
    private val result: GoogleIdTokenRequestResult,
) : GoogleIdTokenProvider {
    override suspend fun requestIdToken(): GoogleIdTokenRequestResult = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
