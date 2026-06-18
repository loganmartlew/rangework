package com.loganmartlew.rangework.android.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.RangeSessionUiState
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ExecutionStepCard
import com.loganmartlew.rangework.android.ui.components.StepNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RangeSessionScreen(
    uiState: RangeSessionUiState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onBack: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val sessionName = uiState.rangeSession?.sessionName ?: "Session"
    val steps = uiState.rangeSession?.snapshot?.steps ?: emptyList()
    val totalSteps = steps.size

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = sessionName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.rangeSession != null && totalSteps > 0) {
                StepNavigationBar(
                    currentStepIndex = uiState.currentStepIndex,
                    totalSteps = totalSteps,
                    onPrevious = onPreviousStep,
                    onNext = onNextStep,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.rangeSession == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EntryHighlightCard(
                            title = "Session not found",
                            body = uiState.statusMessage
                                ?: "This session could not be loaded.",
                        )
                    }
                }

                totalSteps == 0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EntryHighlightCard(
                            title = "No steps",
                            body = "This session has no steps to execute.",
                        )
                    }
                }

                else -> {
                    val currentStep = steps.getOrNull(uiState.currentStepIndex)
                    if (currentStep != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            ExecutionStepCard(
                                step = currentStep,
                                stepNumber = uiState.currentStepIndex + 1,
                                totalSteps = totalSteps,
                            )
                        }
                    }
                }
            }
        }
    }
}
