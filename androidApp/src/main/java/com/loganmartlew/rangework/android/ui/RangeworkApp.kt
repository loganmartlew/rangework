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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loganmartlew.rangework.android.auth.AndroidGoogleIdTokenProvider
import com.loganmartlew.rangework.android.config.baselineAndroidAppAuthConfig
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessageUseCase

@Composable
fun RangeworkApp(
    activity: ComponentActivity,
) {
    val androidAuthConfig = remember { baselineAndroidAppAuthConfig() }
    val viewModel: AuthViewModel = viewModel(
        factory = remember(androidAuthConfig) {
            AuthViewModel.factory(androidAuthConfig)
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bootstrapMessage = remember(androidAuthConfig.environment) {
        AppBootstrapMessageUseCase().invoke(androidAuthConfig.environment)
    }
    val googleIdTokenProvider = remember(activity, androidAuthConfig.googleWebClientId) {
        AndroidGoogleIdTokenProvider(
            activity = activity,
            webClientId = androidAuthConfig.googleWebClientId,
        )
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
                        environment = uiState.environment,
                        authState = uiState.authState,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.actionInProgress &&
                                uiState.environment.supabaseConfig.isConfigured,
                            onClick = viewModel::restoreSession,
                        ) {
                            Text("Restore session")
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.actionInProgress && uiState.environment.isAuthConfigured,
                            onClick = {
                                viewModel.signInWithGoogle(googleIdTokenProvider)
                            },
                        ) {
                            Text("Sign in with Google")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.actionInProgress && uiState.authState is AuthState.SignedIn,
                            onClick = viewModel::signOut,
                        ) {
                            Text("Sign out")
                        }
                    }
                    if (uiState.actionInProgress || uiState.authState is AuthState.Restoring) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (uiState.statusMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.statusMessage.orEmpty(),
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
