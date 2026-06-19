package com.loganmartlew.rangework.android.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.RangeSessionUiState
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ExecutionStepCard
import com.loganmartlew.rangework.android.ui.components.RangeSessionProgressHeader
import com.loganmartlew.rangework.android.ui.components.StepListDrawerContent
import com.loganmartlew.rangework.android.ui.components.StepNavigationBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RangeSessionScreen(
    uiState: RangeSessionUiState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onNavigateToStep: (Int) -> Unit,
    onToggleStepComplete: (Int) -> Unit,
    onConsumeNotification: () -> Unit,
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

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.notification) {
        val msg = uiState.notification ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onConsumeNotification()
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    if (isTablet) {
        TabletRangeSessionLayout(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onNextStep = onNextStep,
            onPreviousStep = onPreviousStep,
            onNavigateToStep = onNavigateToStep,
            onToggleStepComplete = onToggleStepComplete,
            onBack = onBack,
        )
    } else {
        PhoneRangeSessionLayout(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onNextStep = onNextStep,
            onPreviousStep = onPreviousStep,
            onNavigateToStep = onNavigateToStep,
            onToggleStepComplete = onToggleStepComplete,
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneRangeSessionLayout(
    uiState: RangeSessionUiState,
    snackbarHostState: SnackbarHostState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onNavigateToStep: (Int) -> Unit,
    onToggleStepComplete: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerListState = rememberLazyListState()

    val steps = uiState.rangeSession?.snapshot?.steps ?: emptyList()
    val totalSteps = steps.size
    val sessionName = uiState.rangeSession?.sessionName ?: "Session"

    // Scroll the step list to the current step when the drawer finishes opening.
    // Formula: lazyIndex = currentStepIndex + unitIndex + 1 accounts for unit header rows.
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open && steps.isNotEmpty()) {
            val step = steps.getOrNull(uiState.currentStepIndex) ?: return@LaunchedEffect
            val lazyIndex = (uiState.currentStepIndex + step.unitIndex + 1).coerceAtLeast(0)
            drawerListState.animateScrollToItem(lazyIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                StepListDrawerContent(
                    steps = steps,
                    completedStepIndices = uiState.completedStepIndices,
                    currentStepIndex = uiState.currentStepIndex,
                    lazyListState = drawerListState,
                    onStepSelected = { index ->
                        onNavigateToStep(index)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
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
                    actions = {
                        if (!uiState.isLoading && totalSteps > 0) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open step list",
                                )
                            }
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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            Column(modifier = Modifier.fillMaxSize()) {
                                RangeSessionProgressHeader(
                                    rangeSession = uiState.rangeSession,
                                    completedStepIndices = uiState.completedStepIndices,
                                )
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
                                        isCompleted = uiState.currentStepIndex in uiState.completedStepIndices,
                                        onToggleComplete = { onToggleStepComplete(uiState.currentStepIndex) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletRangeSessionLayout(
    uiState: RangeSessionUiState,
    snackbarHostState: SnackbarHostState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onNavigateToStep: (Int) -> Unit,
    onToggleStepComplete: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val steps = uiState.rangeSession?.snapshot?.steps ?: emptyList()
    val totalSteps = steps.size
    val sessionName = uiState.rangeSession?.sessionName ?: "Session"
    val drawerListState = rememberLazyListState()
    val showPanel = !uiState.isLoading && uiState.rangeSession != null && totalSteps > 0

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
            if (showPanel) {
                StepNavigationBar(
                    currentStepIndex = uiState.currentStepIndex,
                    totalSteps = totalSteps,
                    onPrevious = onPreviousStep,
                    onNext = onNextStep,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (showPanel) {
            val currentStep = steps.getOrNull(uiState.currentStepIndex)
            if (currentStep != null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f),
                    ) {
                        StepListDrawerContent(
                            steps = steps,
                            completedStepIndices = uiState.completedStepIndices,
                            currentStepIndex = uiState.currentStepIndex,
                            lazyListState = drawerListState,
                            onStepSelected = onNavigateToStep,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight(),
                    ) {
                        RangeSessionProgressHeader(
                            rangeSession = uiState.rangeSession,
                            completedStepIndices = uiState.completedStepIndices,
                        )
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
                                isCompleted = uiState.currentStepIndex in uiState.completedStepIndices,
                                onToggleComplete = { onToggleStepComplete(uiState.currentStepIndex) },
                            )
                        }
                    }
                }
            }
        } else {
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

                    else -> {
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
                }
            }
        }
    }
}
