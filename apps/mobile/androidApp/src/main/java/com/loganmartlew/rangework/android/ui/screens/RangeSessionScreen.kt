package com.loganmartlew.rangework.android.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.RangeSessionUiState
import com.loganmartlew.rangework.android.ui.components.AbandonConfirmDialog
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ExecutionStepCard
import com.loganmartlew.rangework.android.ui.components.FinishSummaryContent
import com.loganmartlew.rangework.android.ui.components.RangeSessionProgressHeader
import com.loganmartlew.rangework.android.ui.components.StepListDrawerContent
import com.loganmartlew.rangework.android.ui.components.StepNavigationBar
import com.loganmartlew.rangework.shared.model.Club
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
    onScreenEnter: () -> Unit,
    onScreenExit: () -> Unit,
    onBack: () -> Unit,
    enabledClubs: List<Club> = emptyList(),
    onClubOverride: (Int, String) -> Unit = { _, _ -> },
    onFinish: () -> Unit = {},
    onRequestAbandon: () -> Unit = {},
    onDismissAbandon: () -> Unit = {},
    onConfirmAbandon: () -> Unit = {},
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    DisposableEffect(Unit) {
        onScreenEnter()
        onDispose { onScreenExit() }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.notification) {
        val msg = uiState.notification ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onConsumeNotification()
    }

    if (uiState.showAbandonDialog) {
        AbandonConfirmDialog(
            onConfirm = onConfirmAbandon,
            onDismiss = onDismissAbandon,
        )
    }

    BackHandler(enabled = uiState.finishSummary != null) {
        onBack()
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    if (uiState.finishSummary != null) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.rangeSession?.sessionName ?: "Session",
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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            FinishSummaryContent(
                summary = uiState.finishSummary,
                onDone = onBack,
                modifier = Modifier.padding(innerPadding),
            )
        }
        return
    }

    if (isTablet) {
        TabletRangeSessionLayout(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onNextStep = onNextStep,
            onPreviousStep = onPreviousStep,
            onNavigateToStep = onNavigateToStep,
            onToggleStepComplete = onToggleStepComplete,
            onBack = onBack,
            enabledClubs = enabledClubs,
            onClubOverride = onClubOverride,
            onFinish = onFinish,
            onRequestAbandon = onRequestAbandon,
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
            enabledClubs = enabledClubs,
            onClubOverride = onClubOverride,
            onFinish = onFinish,
            onRequestAbandon = onRequestAbandon,
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
    enabledClubs: List<Club> = emptyList(),
    onClubOverride: (Int, String) -> Unit = { _, _ -> },
    onFinish: () -> Unit = {},
    onRequestAbandon: () -> Unit = {},
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
                            var overflowExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { overflowExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                    )
                                }
                                DropdownMenu(
                                    expanded = overflowExpanded,
                                    onDismissRequest = { overflowExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Abandon session",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            overflowExpanded = false
                                            onRequestAbandon()
                                        },
                                        modifier = Modifier.semantics {
                                            contentDescription = "Abandon session"
                                        },
                                    )
                                }
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
                        val currentStepRaw = steps.getOrNull(uiState.currentStepIndex)
                        val currentStep = currentStepRaw?.let { step ->
                            val overrideCode = uiState.rangeSession?.clubOverrides?.get(uiState.currentStepIndex.toString())
                            if (overrideCode != null) {
                                val overrideClub = enabledClubs.find { it.code == overrideCode }
                                step.copy(
                                    club = overrideCode,
                                    clubDisplayName = overrideClub?.displayName ?: step.clubDisplayName,
                                )
                            } else step
                        }
                        if (currentStep != null) {
                            val isFullyComplete = uiState.completedStepIndices.size == totalSteps
                            Column(modifier = Modifier.fillMaxSize()) {
                                RangeSessionProgressHeader(
                                    rangeSession = uiState.rangeSession,
                                    completedStepIndices = uiState.completedStepIndices,
                                    elapsedSeconds = uiState.elapsedSeconds,
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
                                        enabledClubs = enabledClubs,
                                        onClubOverride = { clubCode ->
                                            onClubOverride(uiState.currentStepIndex, clubCode)
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (isFullyComplete) {
                                        Button(
                                            onClick = onFinish,
                                            enabled = !uiState.isFinishing,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .semantics { contentDescription = "Finish session" },
                                        ) {
                                            Text(
                                                text = "Finish Session",
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = onFinish,
                                            enabled = !uiState.isFinishing,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .semantics { contentDescription = "Finish session" },
                                        ) {
                                            Text(
                                                text = "Finish Session",
                                                style = MaterialTheme.typography.labelLarge,
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
    enabledClubs: List<Club> = emptyList(),
    onClubOverride: (Int, String) -> Unit = { _, _ -> },
    onFinish: () -> Unit = {},
    onRequestAbandon: () -> Unit = {},
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
                actions = {
                    if (showPanel) {
                        var overflowExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                )
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Abandon session",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        onRequestAbandon()
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Abandon session"
                                    },
                                )
                            }
                        }
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
            val currentStepRaw = steps.getOrNull(uiState.currentStepIndex)
            val currentStep = currentStepRaw?.let { step ->
                val overrideCode = uiState.rangeSession?.clubOverrides?.get(uiState.currentStepIndex.toString())
                if (overrideCode != null) {
                    val overrideClub = enabledClubs.find { it.code == overrideCode }
                    step.copy(
                        club = overrideCode,
                        clubDisplayName = overrideClub?.displayName ?: step.clubDisplayName,
                    )
                } else step
            }
            if (currentStep != null) {
                val isFullyComplete = uiState.completedStepIndices.size == totalSteps
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
                            elapsedSeconds = uiState.elapsedSeconds,
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
                                enabledClubs = enabledClubs,
                                onClubOverride = { clubCode ->
                                    onClubOverride(uiState.currentStepIndex, clubCode)
                                },
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (isFullyComplete) {
                                Button(
                                    onClick = onFinish,
                                    enabled = !uiState.isFinishing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Finish session" },
                                ) {
                                    Text(
                                        text = "Finish Session",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onFinish,
                                    enabled = !uiState.isFinishing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Finish session" },
                                ) {
                                    Text(
                                        text = "Finish Session",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
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
