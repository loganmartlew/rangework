package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.CompletedRangeSessionStats
import com.loganmartlew.rangework.android.ui.CompletedRangeSessionUiState
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ObservationSummarySection
import com.loganmartlew.rangework.android.ui.components.SessionNoteCard
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.BlockSuccessCount
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubGlyphShape
import com.loganmartlew.rangework.shared.model.ExecutionBlock
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.enabledObservationTypes
import com.loganmartlew.rangework.shared.model.executionBlocks
import com.loganmartlew.rangework.shared.model.isBallStep
import com.loganmartlew.rangework.shared.model.progress
import com.loganmartlew.rangework.shared.model.successCount
import com.loganmartlew.rangework.shared.model.toGlyphShape
import com.loganmartlew.rangework.shared.model.typeTallies
import kotlin.math.roundToInt

/**
 * The completed-session detail screen: the notes/results half shipped in Stage 4
 * (session note and per-block notes stay editable — freeze matrix permits prose
 * after Completion); Stage 6 adds the observation half — read-only per-block
 * tallies/grids and the provenance-labeled success row, gated on
 * `supportsDataCapture` (v3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RangeSessionHistoryScreen(
    uiState: CompletedRangeSessionUiState,
    stats: CompletedRangeSessionStats?,
    onSaveSessionNote: (String?) -> Unit,
    onSaveBlockNote: (blockIndex: Int, note: String?) -> Unit,
    onConsumeNotification: () -> Unit,
    onBack: () -> Unit,
    enabledClubs: List<Club> = emptyList(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.notification) {
        val msg = uiState.notification ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onConsumeNotification()
    }

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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.rangeSession == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EntryHighlightCard(
                        title = "Session not found",
                        body = uiState.statusMessage ?: "This session could not be loaded.",
                    )
                }
            }

            else -> {
                val session = uiState.rangeSession
                val blocks = remember(session.snapshot) { session.snapshot.executionBlocks() }
                val completedStepIndices = remember(session.completedSteps) {
                    session.completedSteps.map { it.stepIndex }.toSet()
                }
                val showSummaries = session.supportsDataCapture && !uiState.observationsUnavailable

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    stats?.let { StatsCard(it) }

                    SessionNoteHistoryCard(
                        saveKey = "session-note",
                        label = "Session note",
                        savedNote = session.sessionNote,
                        isSaving = uiState.isSavingSessionNote,
                        onSave = onSaveSessionNote,
                    )

                    blocks.forEach { block ->
                        val blockResult = session.blockResults[block.unitIndex.toString()]
                        val steps = session.snapshot.steps
                        val successCount = remember(block, completedStepIndices, uiState.observationsByStep, blockResult) {
                            block.successCount(steps, completedStepIndices, uiState.observationsByStep, blockResult)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = block.unit.unitTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            SuccessProvenanceRow(
                                successCount = successCount,
                                criterion = block.unit.successCriterion,
                                observationsAvailable = showSummaries,
                            )
                            if (showSummaries && block.stepIndices.any { steps[it].isBallStep }) {
                                val tallies = remember(block, completedStepIndices, uiState.observationsByStep) {
                                    block.typeTallies(steps, completedStepIndices, uiState.observationsByStep)
                                }
                                val glyphShape = remember(block, session.clubOverrides, enabledClubs) {
                                    resolveBlockGlyphShape(block, steps, session.clubOverrides, enabledClubs)
                                }
                                ObservationSummarySection(
                                    enabledTypes = block.unit.enabledObservationTypes,
                                    tallies = tallies,
                                    completedBalls = block.progress(steps, completedStepIndices).completedBalls,
                                    successCriterion = block.unit.successCriterion,
                                    handedness = uiState.handedness,
                                    clubGlyphShape = glyphShape,
                                )
                            }
                            SessionNoteHistoryCard(
                                saveKey = "block-note-${block.unitIndex}",
                                label = "Note",
                                savedNote = blockResult?.note,
                                isSaving = block.unitIndex in uiState.savingBlockNoteIndices,
                                onSave = { note -> onSaveBlockNote(block.unitIndex, note) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Wraps [SessionNoteCard] with a hoisted, uniquely-keyed draft (loop-safe). */
@Composable
private fun SessionNoteHistoryCard(
    saveKey: String,
    label: String,
    savedNote: String?,
    isSaving: Boolean,
    onSave: (String?) -> Unit,
) {
    var draft by rememberSaveable(key = saveKey) { mutableStateOf(savedNote ?: "") }
    SessionNoteCard(
        label = label,
        draft = draft,
        onDraftChange = { draft = it },
        savedNote = savedNote,
        isSaving = isSaving,
        onSave = onSave,
    )
}

@Composable
private fun StatsCard(stats: CompletedRangeSessionStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatRow(
                label = "Balls hit",
                value = "${stats.completedBalls}",
                unit = "of ${stats.totalBalls}",
                accessible = "${stats.completedBalls} of ${stats.totalBalls} balls hit",
            )
            HorizontalDivider()
            StatRow(
                label = "Steps completed",
                value = "${stats.completedStepCount}/${stats.totalStepCount}",
                accessible = "${stats.completedStepCount} of ${stats.totalStepCount} steps completed",
            )
            HorizontalDivider()
            StatRow(
                label = "Completion",
                value = "${(stats.completionPercentage * 100).roundToInt()}%",
                accessible = "${(stats.completionPercentage * 100).roundToInt()} percent complete",
            )
            HorizontalDivider()
            val mins = stats.elapsedSeconds / 60
            val secs = stats.elapsedSeconds % 60
            StatRow(
                label = "Time",
                value = if (mins > 0) "${mins}m ${secs}s" else "${secs}s",
                accessible = "$mins minutes $secs seconds",
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    accessible: String,
    unit: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = accessible },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = RangeworkMono.large,
                color = MaterialTheme.colorScheme.secondary,
            )
            unit?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The block's headline success number, with its provenance made explicit
 * (Stage 6, P4): "X of Y observed" when derived from an enabled Success type,
 * "X of Y balls" when a manual count, absent otherwise. Never both — the two
 * wordings *are* the provenance label, no separate suffix needed.
 *
 * When [observationsAvailable] is false (the observation load failed), the
 * Derived line is suppressed rather than shown as "0 of 0 observed" — that
 * would be a false statement, the same reason the summary cards are omitted on
 * load failure. The Manual line is unaffected: it doesn't depend on observations.
 */
@Composable
private fun SuccessProvenanceRow(
    successCount: BlockSuccessCount,
    criterion: String?,
    observationsAvailable: Boolean,
) {
    val labels = when (successCount) {
        is BlockSuccessCount.Derived ->
            if (!observationsAvailable) {
                null
            } else {
                "${successCount.hits} of ${successCount.observed} observed" to
                    "${successCount.hits} of ${successCount.observed} successful, of observed balls"
            }
        is BlockSuccessCount.Manual ->
            "${successCount.count} of ${successCount.totalBalls} balls" to
                "${successCount.count} of ${successCount.totalBalls} successful, of all balls"
        BlockSuccessCount.None -> null
    } ?: return
    val (valueText, accessible) = labels

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        criterion?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = valueText,
            style = RangeworkMono.small,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.semantics { contentDescription = accessible },
        )
    }
}

/**
 * The club glyph shape for a block's observation summary: unlike the live
 * block screen's "current ball" (there is none — every ball here is frozen),
 * this resolves from the block's *first* Ball Step, respecting club overrides,
 * falling back to [ClubGlyphShape.IRON] when the block has none or the club
 * isn't in the enabled catalog.
 */
private fun resolveBlockGlyphShape(
    block: ExecutionBlock,
    steps: List<SnapshotStep>,
    clubOverrides: Map<String, String>,
    enabledClubs: List<Club>,
): ClubGlyphShape {
    val firstBallStep = block.stepIndices.firstOrNull { steps[it].isBallStep } ?: return ClubGlyphShape.IRON
    val clubCode = clubOverrides[firstBallStep.toString()] ?: steps[firstBallStep].club
    return enabledClubs.firstOrNull { it.code == clubCode }?.category.toGlyphShape()
}
