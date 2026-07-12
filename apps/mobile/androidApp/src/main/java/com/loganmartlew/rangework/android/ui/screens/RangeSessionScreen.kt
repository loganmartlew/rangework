package com.loganmartlew.rangework.android.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.RangeSessionUiState
import com.loganmartlew.rangework.android.ui.components.AbandonConfirmDialog
import com.loganmartlew.rangework.android.ui.components.BlockOverviewContent
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.BallEditEntry
import com.loganmartlew.rangework.android.ui.components.BallEditGroup
import com.loganmartlew.rangework.android.ui.components.BallEditSheet
import com.loganmartlew.rangework.android.ui.components.ExecutionBlockPage
import com.loganmartlew.rangework.android.ui.components.FinishSessionDialog
import com.loganmartlew.rangework.android.ui.components.FinishSummaryContent
import com.loganmartlew.rangework.android.ui.components.ObservationGridDialog
import com.loganmartlew.rangework.android.ui.components.RangeSessionProgressHeader
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubGlyphShape
import com.loganmartlew.rangework.shared.model.ExecutionBlock
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.canEditObservations
import com.loganmartlew.rangework.shared.model.enabledObservationTypes
import com.loganmartlew.rangework.shared.model.executionBlocks
import com.loganmartlew.rangework.shared.model.isBallStep
import com.loganmartlew.rangework.shared.model.progress
import com.loganmartlew.rangework.shared.model.toGlyphShape
import com.loganmartlew.rangework.shared.model.typeTally
import kotlinx.coroutines.launch

/**
 * Block-first execution: one screen per Block (Session Item), a ball counter
 * instead of screen-per-step, free navigation via horizontal pager plus an
 * overview for jumping. See design-docs/range-execution-ux-review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RangeSessionScreen(
    uiState: RangeSessionUiState,
    onNavigateToBlock: (Int) -> Unit,
    onIncrementBlock: (Int) -> Unit,
    onDecrementBlock: (Int) -> Unit,
    onToggleActionInstruction: (blockIndex: Int, instructionIndex: Int) -> Unit,
    onSwapClub: (blockIndex: Int, instructionIndex: Int, clubCode: String) -> Unit,
    onConsumeNotification: () -> Unit,
    onScreenEnter: () -> Unit,
    onScreenExit: () -> Unit,
    onBack: () -> Unit,
    enabledClubs: List<Club> = emptyList(),
    onRequestFinish: () -> Unit = {},
    onCompleteRemainingAndFinish: () -> Unit = {},
    onFinishAsIs: () -> Unit = {},
    onDismissFinishDialog: () -> Unit = {},
    onRequestAbandon: () -> Unit = {},
    onDismissAbandon: () -> Unit = {},
    onConfirmAbandon: () -> Unit = {},
    onSaveBlockNote: (blockIndex: Int, note: String?) -> Unit = { _, _ -> },
    onSaveManualCount: (blockIndex: Int, count: Int?) -> Unit = { _, _ -> },
    onSaveSessionNote: (note: String?, onComplete: () -> Unit) -> Unit = { _, done -> done() },
    onStageObservation: (blockIndex: Int, typeId: String, value: String) -> Unit = { _, _, _ -> },
    onUpdateBallObservation: (stepIndex: Int, typeId: String, value: String?) -> Unit = { _, _, _ -> },
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

    val snapshot = uiState.rangeSession?.snapshot
    val blocks = remember(snapshot) { snapshot?.executionBlocks() ?: emptyList() }
    val steps = snapshot?.steps ?: emptyList()

    if (uiState.showFinishDialog) {
        FinishSessionDialog(
            blocks = blocks,
            steps = steps,
            completedStepIndices = uiState.completedStepIndices,
            onCompleteRemaining = onCompleteRemainingAndFinish,
            onFinishAsIs = onFinishAsIs,
            onDismiss = onDismissFinishDialog,
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
                showSessionNote = uiState.rangeSession?.supportsDataCapture == true,
                savedSessionNote = uiState.rangeSession?.sessionNote,
                isSavingSessionNote = uiState.isSavingSessionNote,
                onSaveSessionNote = onSaveSessionNote,
                modifier = Modifier.padding(innerPadding),
            )
        }
        return
    }

    if (isTablet) {
        TabletRangeSessionLayout(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onNavigateToBlock = onNavigateToBlock,
            onIncrementBlock = onIncrementBlock,
            onDecrementBlock = onDecrementBlock,
            onToggleActionInstruction = onToggleActionInstruction,
            onSwapClub = onSwapClub,
            onBack = onBack,
            enabledClubs = enabledClubs,
            onRequestFinish = onRequestFinish,
            onRequestAbandon = onRequestAbandon,
            onSaveBlockNote = onSaveBlockNote,
            onSaveManualCount = onSaveManualCount,
            onStageObservation = onStageObservation,
            onUpdateBallObservation = onUpdateBallObservation,
        )
    } else {
        PhoneRangeSessionLayout(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onNavigateToBlock = onNavigateToBlock,
            onIncrementBlock = onIncrementBlock,
            onDecrementBlock = onDecrementBlock,
            onToggleActionInstruction = onToggleActionInstruction,
            onSwapClub = onSwapClub,
            onBack = onBack,
            enabledClubs = enabledClubs,
            onRequestFinish = onRequestFinish,
            onRequestAbandon = onRequestAbandon,
            onSaveBlockNote = onSaveBlockNote,
            onSaveManualCount = onSaveManualCount,
            onStageObservation = onStageObservation,
            onUpdateBallObservation = onUpdateBallObservation,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneRangeSessionLayout(
    uiState: RangeSessionUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateToBlock: (Int) -> Unit,
    onIncrementBlock: (Int) -> Unit,
    onDecrementBlock: (Int) -> Unit,
    onToggleActionInstruction: (Int, Int) -> Unit,
    onSwapClub: (Int, Int, String) -> Unit,
    onBack: () -> Unit,
    enabledClubs: List<Club>,
    onRequestFinish: () -> Unit,
    onRequestAbandon: () -> Unit,
    onSaveBlockNote: (Int, String?) -> Unit,
    onSaveManualCount: (Int, Int?) -> Unit,
    onStageObservation: (Int, String, String) -> Unit,
    onUpdateBallObservation: (Int, String, String?) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snapshot = uiState.rangeSession?.snapshot
    val blocks = remember(snapshot) { snapshot?.executionBlocks() ?: emptyList() }
    val steps = snapshot?.steps ?: emptyList()
    val sessionName = uiState.rangeSession?.sessionName ?: "Session"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                BlockOverviewContent(
                    blocks = blocks,
                    steps = steps,
                    completedStepIndices = uiState.completedStepIndices,
                    currentBlockIndex = uiState.currentBlockIndex,
                    onBlockSelected = { index ->
                        onNavigateToBlock(index)
                        scope.launch { drawerState.close() }
                    },
                    onFinish = onRequestFinish,
                    isFinishing = uiState.isFinishing,
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
                        if (!uiState.isLoading && blocks.isNotEmpty()) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Open session overview",
                                )
                            }
                            SessionOverflowMenu(onRequestAbandon = onRequestAbandon)
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            RangeSessionBody(
                uiState = uiState,
                blocks = blocks,
                enabledClubs = enabledClubs,
                onNavigateToBlock = onNavigateToBlock,
                onIncrementBlock = onIncrementBlock,
                onDecrementBlock = onDecrementBlock,
                onToggleActionInstruction = onToggleActionInstruction,
                onSwapClub = onSwapClub,
                onRequestFinish = onRequestFinish,
                onSaveBlockNote = onSaveBlockNote,
                onSaveManualCount = onSaveManualCount,
                onStageObservation = onStageObservation,
                onUpdateBallObservation = onUpdateBallObservation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletRangeSessionLayout(
    uiState: RangeSessionUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateToBlock: (Int) -> Unit,
    onIncrementBlock: (Int) -> Unit,
    onDecrementBlock: (Int) -> Unit,
    onToggleActionInstruction: (Int, Int) -> Unit,
    onSwapClub: (Int, Int, String) -> Unit,
    onBack: () -> Unit,
    enabledClubs: List<Club>,
    onRequestFinish: () -> Unit,
    onRequestAbandon: () -> Unit,
    onSaveBlockNote: (Int, String?) -> Unit,
    onSaveManualCount: (Int, Int?) -> Unit,
    onStageObservation: (Int, String, String) -> Unit,
    onUpdateBallObservation: (Int, String, String?) -> Unit,
) {
    val snapshot = uiState.rangeSession?.snapshot
    val blocks = remember(snapshot) { snapshot?.executionBlocks() ?: emptyList() }
    val steps = snapshot?.steps ?: emptyList()
    val sessionName = uiState.rangeSession?.sessionName ?: "Session"
    val showPanel = !uiState.isLoading && uiState.rangeSession != null && blocks.isNotEmpty()

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
                        SessionOverflowMenu(onRequestAbandon = onRequestAbandon)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (showPanel) {
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
                    BlockOverviewContent(
                        blocks = blocks,
                        steps = steps,
                        completedStepIndices = uiState.completedStepIndices,
                        currentBlockIndex = uiState.currentBlockIndex,
                        onBlockSelected = onNavigateToBlock,
                        onFinish = onRequestFinish,
                        isFinishing = uiState.isFinishing,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                )
                RangeSessionBody(
                    uiState = uiState,
                    blocks = blocks,
                    enabledClubs = enabledClubs,
                    onNavigateToBlock = onNavigateToBlock,
                    onIncrementBlock = onIncrementBlock,
                    onDecrementBlock = onDecrementBlock,
                    onToggleActionInstruction = onToggleActionInstruction,
                    onSwapClub = onSwapClub,
                    onRequestFinish = onRequestFinish,
                    onSaveBlockNote = onSaveBlockNote,
                    onSaveManualCount = onSaveManualCount,
                    onStageObservation = onStageObservation,
                    onUpdateBallObservation = onUpdateBallObservation,
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight(),
                )
            }
        } else {
            RangeSessionBody(
                uiState = uiState,
                blocks = blocks,
                enabledClubs = enabledClubs,
                onNavigateToBlock = onNavigateToBlock,
                onIncrementBlock = onIncrementBlock,
                onDecrementBlock = onDecrementBlock,
                onToggleActionInstruction = onToggleActionInstruction,
                onSwapClub = onSwapClub,
                onRequestFinish = onRequestFinish,
                onSaveBlockNote = onSaveBlockNote,
                onSaveManualCount = onSaveManualCount,
                onStageObservation = onStageObservation,
                onUpdateBallObservation = onUpdateBallObservation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SessionOverflowMenu(onRequestAbandon: () -> Unit) {
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

@Composable
private fun RangeSessionBody(
    uiState: RangeSessionUiState,
    blocks: List<ExecutionBlock>,
    enabledClubs: List<Club>,
    onNavigateToBlock: (Int) -> Unit,
    onIncrementBlock: (Int) -> Unit,
    onDecrementBlock: (Int) -> Unit,
    onToggleActionInstruction: (Int, Int) -> Unit,
    onSwapClub: (Int, Int, String) -> Unit,
    onRequestFinish: () -> Unit,
    onSaveBlockNote: (Int, String?) -> Unit,
    onSaveManualCount: (Int, Int?) -> Unit,
    onStageObservation: (Int, String, String) -> Unit,
    onUpdateBallObservation: (Int, String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = uiState.rangeSession?.snapshot?.steps ?: emptyList()

    when {
        uiState.isLoading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.rangeSession == null -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
        }

        blocks.isEmpty() || steps.isEmpty() -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
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

        else -> {
            val isSessionComplete = uiState.completedStepIndices.size == steps.size
            val pagerState = rememberPagerState(
                initialPage = uiState.currentBlockIndex.coerceIn(0, blocks.lastIndex),
            ) { blocks.size }
            val scope = rememberCoroutineScope()

            // Keep pager and view-model block index in sync in both directions.
            LaunchedEffect(pagerState.settledPage) {
                onNavigateToBlock(pagerState.settledPage)
            }
            LaunchedEffect(uiState.currentBlockIndex) {
                if (pagerState.currentPage != uiState.currentBlockIndex &&
                    !pagerState.isScrollInProgress
                ) {
                    pagerState.animateScrollToPage(uiState.currentBlockIndex)
                }
            }

            val showDataCapture = uiState.rangeSession?.supportsDataCapture == true

            // Which grid dialog / edit sheet is open is screen-local state (survives
            // rotation via rememberSaveable); *what* they show comes from uiState.
            var gridTypeId by rememberSaveable { mutableStateOf<String?>(null) }
            var gridBlockIndex by rememberSaveable { mutableIntStateOf(-1) }
            var gridStepIndex by rememberSaveable { mutableIntStateOf(-1) } // -1 = staging mode
            var sheetBlockIndex by rememberSaveable { mutableIntStateOf(-1) }
            var sheetExpandedStep by rememberSaveable { mutableIntStateOf(-1) }

            Column(modifier = modifier) {
                RangeSessionProgressHeader(
                    rangeSession = uiState.rangeSession,
                    completedStepIndices = uiState.completedStepIndices,
                    elapsedSeconds = uiState.elapsedSeconds,
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.Top,
                ) { pageIndex ->
                    val block = blocks[pageIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        ExecutionBlockPage(
                            block = block,
                            steps = steps,
                            completedStepIndices = uiState.completedStepIndices,
                            clubOverrides = uiState.rangeSession.clubOverrides,
                            enabledClubs = enabledClubs,
                            onIncrement = { onIncrementBlock(pageIndex) },
                            onDecrement = { onDecrementBlock(pageIndex) },
                            onToggleActionInstruction = { instructionIndex ->
                                onToggleActionInstruction(pageIndex, instructionIndex)
                            },
                            onSwapClub = { instructionIndex, clubCode ->
                                onSwapClub(pageIndex, instructionIndex, clubCode)
                            },
                            isSessionComplete = isSessionComplete,
                            isFinishing = uiState.isFinishing,
                            onFinish = onRequestFinish,
                            showDataCapture = showDataCapture,
                            blockResult = uiState.rangeSession.blockResults[block.unitIndex.toString()],
                            isSavingBlockNote = block.unitIndex in uiState.savingBlockNoteIndices,
                            onSaveBlockNote = { note -> onSaveBlockNote(pageIndex, note) },
                            onSetManualCount = { count -> onSaveManualCount(pageIndex, count) },
                            observationsByStep = uiState.observationsByStep,
                            blockStaging = uiState.stagingByBlock[pageIndex].orEmpty(),
                            arming = uiState.armingBlockIndex == pageIndex,
                            handedness = uiState.handedness,
                            commitSignal = if (uiState.committedBlockIndex == pageIndex) uiState.commitSignal else 0,
                            onStageChip = { typeId, value -> onStageObservation(pageIndex, typeId, value) },
                            onOpenGrid = { type ->
                                gridTypeId = type.id
                                gridBlockIndex = pageIndex
                                gridStepIndex = -1
                            },
                            onOpenBallSheet = {
                                sheetBlockIndex = pageIndex
                                sheetExpandedStep = -1
                            },
                        )
                    }
                }
                BlockNavBar(
                    currentBlock = pagerState.currentPage,
                    totalBlocks = blocks.size,
                    onPrevious = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onNext = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                )
            }

            // ── Grid dialog (staging or edit mode) ───────────────────────────
            val gridType = gridTypeId?.let(ObservationType::fromId)
            val gridBlock = blocks.getOrNull(gridBlockIndex)
            if (gridType != null && gridBlock != null) {
                val editing = gridStepIndex >= 0
                val currentValue = if (editing) {
                    uiState.observationsByStep[gridStepIndex]?.value(gridType)
                } else {
                    uiState.stagingByBlock[gridBlockIndex]?.get(gridType.id)
                }
                val gridShape = remember(gridType, gridBlock, gridStepIndex, steps, enabledClubs) {
                    if (gridType != ObservationType.STRIKE_LOCATION) return@remember ClubGlyphShape.IRON
                    val stepIndex = if (gridStepIndex >= 0) gridStepIndex else {
                        gridBlock.stepIndices.firstOrNull {
                            steps[it].isBallStep && it !in uiState.completedStepIndices
                        } ?: return@remember ClubGlyphShape.IRON
                    }
                    if (stepIndex >= steps.size) return@remember ClubGlyphShape.IRON
                    val clubCode = uiState.rangeSession.clubOverrides[stepIndex.toString()]
                        ?: steps[stepIndex].club
                    enabledClubs.firstOrNull { it.code == clubCode }?.category.toGlyphShape()
                }
                ObservationGridDialog(
                    type = gridType,
                    handedness = uiState.handedness,
                    tally = gridBlock.typeTally(
                        steps, uiState.completedStepIndices, uiState.observationsByStep, gridType,
                    ),
                    completedBalls = gridBlock.progress(steps, uiState.completedStepIndices).completedBalls,
                    currentValue = currentValue,
                    editingBallNumber = if (editing) ballOrdinal(gridBlock, steps, gridStepIndex) else null,
                    clubGlyphShape = gridShape,
                    onPick = { value ->
                        if (editing) {
                            onUpdateBallObservation(gridStepIndex, gridType.id, value)
                        } else {
                            onStageObservation(gridBlockIndex, gridType.id, value)
                        }
                        gridTypeId = null
                    },
                    onDismiss = { gridTypeId = null },
                )
            }

            // ── Per-ball edit sheet ──────────────────────────────────────────
            val sheetBlock = blocks.getOrNull(sheetBlockIndex)
            if (sheetBlock != null) {
                val groups = ballEditGroups(sheetBlock, steps, uiState.completedStepIndices)
                BallEditSheet(
                    groups = groups,
                    enabledTypes = sheetBlock.unit.enabledObservationTypes,
                    observationsByStep = uiState.observationsByStep,
                    handedness = uiState.handedness,
                    steps = steps,
                    clubOverrides = uiState.rangeSession.clubOverrides,
                    enabledClubs = enabledClubs,
                    expandedStepIndex = sheetExpandedStep.takeIf { it >= 0 },
                    enabled = uiState.rangeSession.canEditObservations,
                    onToggleExpand = { step ->
                        sheetExpandedStep = if (sheetExpandedStep == step) -1 else step
                    },
                    onEditChip = { step, typeId, value -> onUpdateBallObservation(step, typeId, value) },
                    onOpenGrid = { step, type ->
                        gridTypeId = type.id
                        gridBlockIndex = sheetBlockIndex
                        gridStepIndex = step
                    },
                    onDismiss = { sheetBlockIndex = -1 },
                )
            }
        }
    }
}

/**
 * The block's committed Ball Steps as edit-sheet groups, one section per ball
 * instruction in snapshot order. Within a section, "Ball N" is the 1-based ordinal
 * by snapshot position among that instruction's Ball Steps, filtered to the
 * completed ones (only committed balls are correctable). Instructions with zero
 * committed balls are omitted.
 */
private fun ballEditGroups(
    block: ExecutionBlock,
    steps: List<com.loganmartlew.rangework.shared.model.SnapshotStep>,
    completedStepIndices: Set<Int>,
): List<BallEditGroup> = block.stepIndices
    .filter { steps[it].isBallStep }
    .groupBy { steps[it].instructionIndex }
    .toSortedMap()
    .mapNotNull { (_, stepIndices) ->
        val entries = stepIndices
            .mapIndexed { position, stepIndex -> BallEditEntry(stepIndex, position + 1) }
            .filter { it.stepIndex in completedStepIndices }
        if (entries.isEmpty()) return@mapNotNull null
        BallEditGroup(instructionText = steps[stepIndices.first()].instructionText, entries = entries)
    }

/** The 1-based ordinal of [stepIndex] among its instruction's Ball Steps. */
private fun ballOrdinal(
    block: ExecutionBlock,
    steps: List<com.loganmartlew.rangework.shared.model.SnapshotStep>,
    stepIndex: Int,
): Int {
    val instructionIndex = steps[stepIndex].instructionIndex
    val ballSteps = block.stepIndices
        .filter { steps[it].instructionIndex == instructionIndex && steps[it].isBallStep }
    return ballSteps.indexOf(stepIndex) + 1
}

/**
 * Explicit previous/next block navigation under the pager, so moving between
 * blocks never depends on discovering the swipe gesture.
 */
@Composable
private fun BlockNavBar(
    currentBlock: Int,
    totalBlocks: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = currentBlock > 0,
            modifier = Modifier.semantics { contentDescription = "Previous block" },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                contentDescription = null,
            )
        }
        Text(
            text = "Block ${currentBlock + 1} of $totalBlocks",
            style = RangeworkMono.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onNext,
            enabled = currentBlock < totalBlocks - 1,
            modifier = Modifier.semantics { contentDescription = "Next block" },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
            )
        }
    }
}
