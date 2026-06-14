package com.loganmartlew.rangework.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loganmartlew.rangework.android.auth.AndroidGoogleIdTokenProvider
import com.loganmartlew.rangework.android.auth.GoogleIdTokenRequestResult
import com.loganmartlew.rangework.android.config.baselineAndroidAppAuthConfig
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.auth.createAuthFoundation
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.config.missingConfigurationLabels
import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessageUseCase
import kotlinx.coroutines.launch

@Composable
fun RangeworkApp(
    activity: ComponentActivity,
) {
    val androidAuthConfig = remember { baselineAndroidAppAuthConfig() }
    val bootstrapMessage = remember(androidAuthConfig.environment) {
        AppBootstrapMessageUseCase().invoke(androidAuthConfig.environment)
    }
    val authFoundation = remember(androidAuthConfig.environment.supabaseConfig) {
        createAuthFoundation(androidAuthConfig.environment.supabaseConfig)
    }
    val googleIdTokenProvider = remember(activity, androidAuthConfig.googleWebClientId) {
        AndroidGoogleIdTokenProvider(
            activity = activity,
            webClientId = androidAuthConfig.googleWebClientId,
        )
    }
    val authState = authFoundation?.let { foundation ->
        foundation.observeAuthStateUseCase().collectAsStateWithLifecycle(
            initialValue = AuthState.Restoring,
        ).value
    } ?: AuthState.SignedOut
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var actionInProgress by remember { mutableStateOf(false) }

    suspend fun restoreSession() {
        val foundation = authFoundation
        if (foundation == null) {
            statusMessage = missingConfigMessage(androidAuthConfig.environment)
            return
        }

        actionInProgress = true
        statusMessage = authStateMessage(foundation.restoreAuthSessionUseCase())
        actionInProgress = false
    }

    suspend fun signIn() {
        val foundation = authFoundation
        if (foundation == null) {
            statusMessage = missingConfigMessage(androidAuthConfig.environment)
            return
        }

        actionInProgress = true
        statusMessage = when (val tokenResult = googleIdTokenProvider.requestIdToken()) {
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
                    exception.message ?: "Supabase sign-in could not start."
                } catch (exception: IllegalArgumentException) {
                    exception.message ?: "Supabase sign-in was rejected."
                } catch (exception: Exception) {
                    exception.message ?: "Supabase sign-in failed."
                }
            }
        }
        actionInProgress = false
    }

    suspend fun signOut() {
        val foundation = authFoundation
        if (foundation == null) {
            statusMessage = missingConfigMessage(androidAuthConfig.environment)
            return
        }

        actionInProgress = true
        statusMessage = try {
            foundation.signOutUseCase()
            "Signed out of the local Supabase session."
        } catch (exception: IllegalStateException) {
            exception.message ?: "Sign out failed."
        } catch (exception: Exception) {
            exception.message ?: "Sign out failed."
        }
        actionInProgress = false
    }

    LaunchedEffect(authFoundation) {
        restoreSession()
    }

    RangeworkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Text(
                        text = "Rangework",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = bootstrapMessage.headline,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = bootstrapMessage.detail,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ConfigurationStatusCard(
                        environment = androidAuthConfig.environment,
                        authState = authState,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !actionInProgress && authFoundation != null,
                            onClick = {
                                coroutineScope.launch {
                                    restoreSession()
                                }
                            },
                        ) {
                            Text("Restore session")
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !actionInProgress && androidAuthConfig.environment.isAuthConfigured,
                            onClick = {
                                coroutineScope.launch {
                                    signIn()
                                }
                            },
                        ) {
                            Text("Sign in with Google")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !actionInProgress && authState is AuthState.SignedIn,
                            onClick = {
                                coroutineScope.launch {
                                    signOut()
                                }
                            },
                        ) {
                            Text("Sign out")
                        }
                    }
                    if (actionInProgress || authState is AuthState.Restoring) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationStatusCard(
    environment: AppEnvironment,
    authState: AuthState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Auth foundation",
                style = MaterialTheme.typography.titleMedium,
            )
            StatusLine(
                label = "Supabase URL",
                value = if (environment.supabaseConfig.hasProjectUrl) "Configured" else "Missing",
            )
            StatusLine(
                label = "Supabase anon key",
                value = if (environment.supabaseConfig.hasAnonKey) "Configured" else "Missing",
            )
            StatusLine(
                label = "Google web client ID",
                value = if (environment.googleAuthConfig.isConfigured) "Configured" else "Missing",
            )
            StatusLine(
                label = "Session",
                value = authStateMessage(authState),
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun authStateMessage(authState: AuthState): String = when (authState) {
    AuthState.Restoring -> "Restoring any saved Supabase session."
    AuthState.SignedOut -> "No active Supabase session on this device."
    is AuthState.Error -> authState.message
    is AuthState.SignedIn -> authState.userEmail?.let { "Signed in as $it." }
        ?: "Signed in as ${authState.userId}."
}

private fun missingConfigMessage(environment: AppEnvironment): String =
    "Auth config is incomplete: ${environment.missingConfigurationLabels.joinToString()}. " +
        "Set rangeworkSupabaseUrl, rangeworkSupabaseAnonKey, and rangeworkGoogleWebClientId to continue."
