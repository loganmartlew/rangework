package com.loganmartlew.rangework.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ExecutionBlock
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.clubShortLabel
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.decrementTargets
import com.loganmartlew.rangework.shared.model.enabledObservationTypes
import com.loganmartlew.rangework.shared.model.hasIncompleteBallSteps
import com.loganmartlew.rangework.shared.model.isBallStep
import com.loganmartlew.rangework.shared.model.progress
import com.loganmartlew.rangework.shared.model.totalBalls
import com.loganmartlew.rangework.shared.model.typeTallies

/**
 * One Block — the live view of one Session Item — rendered as a single screen.
 * The Focus Cue leads, the instruction list shows the structure of one pass,
 * and repetition is a ball counter, not screen-per-step.
 */
@Composable
internal fun ExecutionBlockPage(
    block: ExecutionBlock,
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    clubOverrides: Map<String, String>,
    enabledClubs: List<Club>,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleActionInstruction: (Int) -> Unit,
    onSwapClub: (instructionIndex: Int, clubCode: String) -> Unit,
    isSessionComplete: Boolean,
    isFinishing: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    showDataCapture: Boolean = false,
    blockResult: BlockResult? = null,
    isSavingBlockNote: Boolean = false,
    onSaveBlockNote: (String?) -> Unit = {},
    onSetManualCount: (Int?) -> Unit = {},
    // ── Per-ball observation capture (v3) ────────────────────────────────────
    observationsByStep: Map<Int, Observation> = emptyMap(),
    blockStaging: Map<String, String> = emptyMap(),
    arming: Boolean = false,
    handedness: Handedness = Handedness.RIGHT,
    commitSignal: Int = 0,
    onStageChip: (typeId: String, value: String) -> Unit = { _, _ -> },
    onOpenGrid: (ObservationType) -> Unit = {},
    onOpenBallSheet: () -> Unit = {},
) {
    val progress = block.progress(steps, completedStepIndices)
    val enabledObservationTypes = if (showDataCapture) block.unit.enabledObservationTypes else emptyList()
    val captureEnabled = enabledObservationTypes.isNotEmpty()
    val instructionRows = remember(block, steps, completedStepIndices, clubOverrides, enabledClubs) {
        buildInstructionRows(block, steps, completedStepIndices, clubOverrides, enabledClubs)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Block header. Block position lives in the pager nav bar below the
        // pages, so only the pass position renders here.
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (progress.totalPasses > 1) {
                Text(
                    text = "Pass ${progress.currentPass} of ${progress.totalPasses}",
                    style = RangeworkMono.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = block.unit.unitTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Focus cue — the one glanceable thing, ahead of everything else.
        block.focusCue?.let { cue ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Focus".uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = cue,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        BlockCounter(
            block = block,
            steps = steps,
            completedStepIndices = completedStepIndices,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            commitSignal = commitSignal,
            captureSection = if (captureEnabled) {
                {
                    val readOnly = !block.hasIncompleteBallSteps(steps, completedStepIndices)
                    ObservationCaptureSection(
                        enabledTypes = enabledObservationTypes,
                        tallies = block.typeTallies(steps, completedStepIndices, observationsByStep),
                        staging = blockStaging,
                        completedBalls = progress.completedBalls,
                        successCriterion = block.unit.successCriterion,
                        arming = arming,
                        handedness = handedness,
                        readOnly = readOnly,
                        onStageChip = onStageChip,
                        onOpenGrid = onOpenGrid,
                    )
                }
            } else {
                null
            },
        )

        if (isSessionComplete) {
            Button(
                onClick = onFinish,
                enabled = !isFinishing,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Finish session" },
            ) {
                Text("Finish Session", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Instruction list: the structure of one pass, not N sequential tasks.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                instructionRows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    InstructionRow(
                        row = row,
                        enabledClubs = enabledClubs,
                        onToggleAction = { onToggleActionInstruction(row.instructionIndex) },
                        onSwapClub = { clubCode -> onSwapClub(row.instructionIndex, clubCode) },
                    )
                }
            }
        }

        if (captureEnabled && progress.completedBalls > 0) {
            RecordedBallsCard(
                recordedCount = progress.completedBalls,
                onClick = onOpenBallSheet,
            )
        }

        block.notes?.let { notes ->
            CollapsibleNotes(notes = notes)
        }

        // Passive per-block capture (v3 only): note always; manual count only when
        // the unit has a criterion and did not enable the Success Observation Type.
        if (showDataCapture) {
            val manualCountEligible = block.unit.successCriterion != null &&
                ObservationType.SUCCESS !in block.unit.enabledObservationTypes &&
                block.totalBalls(steps) > 0
            BlockResultSection(
                blockResult = blockResult,
                isSavingNote = isSavingBlockNote,
                onSaveNote = onSaveBlockNote,
                manualCountEligible = manualCountEligible,
                successCriterion = block.unit.successCriterion,
                totalBalls = block.totalBalls(steps),
                onSetManualCount = onSetManualCount,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BlockCounter(
    block: ExecutionBlock,
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    commitSignal: Int = 0,
    captureSection: (@Composable () -> Unit)? = null,
) {
    val progress = block.progress(steps, completedStepIndices)
    val hasBalls = progress.totalBalls > 0
    val showPlusOne = block.hasIncompleteBallSteps(steps, completedStepIndices)
    val canDecrement = block.decrementTargets(steps, completedStepIndices).isNotEmpty()

    // Counter pulse + haptic tick, fired once per commit (design P6). commitSignal
    // only advances for the page that committed, so other pages never bump.
    val haptics = LocalHapticFeedback.current
    val bump = remember { Animatable(1f) }
    var lastSignal by remember { mutableIntStateOf(commitSignal) }
    LaunchedEffect(commitSignal) {
        if (commitSignal != lastSignal && commitSignal > 0) {
            lastSignal = commitSignal
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            bump.animateTo(1.18f, tween(120))
            bump.animateTo(1f, tween(180))
        } else {
            lastSignal = commitSignal
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = if (hasBalls) {
                        "${progress.completedBalls} of ${progress.totalBalls} balls"
                    } else {
                        "${progress.completedSteps} of ${progress.totalSteps} steps"
                    }
                },
            ) {
                Text(
                    text = if (hasBalls) {
                        "${progress.completedBalls}/${progress.totalBalls}"
                    } else {
                        "${progress.completedSteps}/${progress.totalSteps}"
                    },
                    style = RangeworkMono.large,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.graphicsLayer {
                        scaleX = bump.value
                        scaleY = bump.value
                    },
                )
                Text(
                    text = if (hasBalls) "balls" else "steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // The per-ball capture stack lives between the readout and the button
            // row — one container, one thumb zone (design §0).
            captureSection?.invoke()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedIconButton(
                    onClick = onDecrement,
                    enabled = canDecrement,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription =
                                if (canDecrement) "Undo last ball" else "Undo last ball, disabled"
                        },
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                }

                when {
                    showPlusOne -> {
                        Button(
                            onClick = onIncrement,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .semantics { contentDescription = "Count one ball" },
                        ) {
                            Text(text = "+1", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    !progress.isComplete -> {
                        Button(
                            onClick = onIncrement,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .semantics { contentDescription = "Mark block done" },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Done", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .semantics { contentDescription = "Block complete" },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Block complete",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class InstructionRowData(
    val instructionIndex: Int,
    val text: String,
    val isAction: Boolean,
    val completedSteps: Int,
    val totalSteps: Int,
    val completedBalls: Int,
    val totalBalls: Int,
    val clubCode: String?,
    val clubDisplayName: String?,
    val canSwapClub: Boolean,
)

private fun buildInstructionRows(
    block: ExecutionBlock,
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    clubOverrides: Map<String, String>,
    enabledClubs: List<Club>,
): List<InstructionRowData> {
    val byInstruction = block.stepIndices
        .groupBy { steps[it].instructionIndex }
        .toSortedMap()
    return byInstruction.map { (instructionIndex, indices) ->
        val first = steps[indices.first()]
        val completed = indices.count { it in completedStepIndices }
        val referenceIndex = indices.firstOrNull { it !in completedStepIndices } ?: indices.last()
        val overrideCode = clubOverrides[referenceIndex.toString()]
        val clubCode = overrideCode ?: steps[referenceIndex].club
        val clubDisplayName = if (overrideCode != null) {
            enabledClubs.find { it.code == overrideCode }?.displayName
                ?: steps[referenceIndex].clubDisplayName
        } else {
            steps[referenceIndex].clubDisplayName
        }
        InstructionRowData(
            instructionIndex = instructionIndex,
            text = first.instructionText,
            isAction = indices.none { steps[it].isBallStep },
            completedSteps = completed,
            totalSteps = indices.size,
            completedBalls = indices.filter { it in completedStepIndices }
                .sumOf { steps[it].ballCount ?: 0 },
            totalBalls = indices.sumOf { steps[it].ballCount ?: 0 },
            clubCode = clubCode,
            clubDisplayName = clubDisplayName,
            canSwapClub = clubCode != null &&
                enabledClubs.isNotEmpty() &&
                indices.any { it !in completedStepIndices },
        )
    }
}

@Composable
private fun InstructionRow(
    row: InstructionRowData,
    enabledClubs: List<Club>,
    onToggleAction: () -> Unit,
    onSwapClub: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClubPicker by remember { mutableStateOf(false) }
    val isDone = row.totalSteps > 0 && row.completedSteps == row.totalSteps

    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { base ->
                if (row.isAction) {
                    base
                        .clickable(onClick = onToggleAction)
                        .semantics {
                            contentDescription = buildString {
                                append(row.text)
                                append(if (isDone) ". Done" else ". Not done")
                                append(". Tap to toggle")
                            }
                        }
                } else {
                    base
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Leading state: check-off for Action instructions, ball tally for Ball ones.
        if (row.isAction) {
            Icon(
                imageVector = if (isDone) Icons.Default.Check else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isDone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = "${row.completedBalls}/${row.totalBalls}",
                style = RangeworkMono.small,
                color = if (isDone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }

        // Compact club chip: short label ("7I", "PW"), full name in semantics.
        // The swap glyph only renders when the club can actually be changed.
        row.clubDisplayName?.takeIf(String::isNotBlank)?.let { name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = if (row.canSwapClub) {
                    Modifier
                        .clickable { showClubPicker = true }
                        .semantics { contentDescription = "Club: $name, tap to change" }
                } else {
                    Modifier.semantics { contentDescription = "Club: $name" }
                },
            ) {
                Text(
                    text = clubShortLabel(name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.canSwapClub) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (row.canSwapClub) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    val clubCode = row.clubCode
    if (showClubPicker && clubCode != null && enabledClubs.isNotEmpty()) {
        ClubOverridePickerDialog(
            currentClubCode = clubCode,
            availableClubs = enabledClubs,
            onClubSelected = { newClubCode ->
                onSwapClub(newClubCode)
                showClubPicker = false
            },
            onDismiss = { showClubPicker = false },
        )
    }
}

/**
 * The sole entry into the per-ball correction sheet: a dedicated, legible card
 * (rather than a subtle chevron buried in the instruction list) so the ability to
 * review/correct recorded balls is easy to spot. Bare count, no judgement framing.
 */
@Composable
private fun RecordedBallsCard(
    recordedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics {
                    contentDescription = "Recorded balls, $recordedCount. Tap to review and correct."
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Recorded balls",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$recordedCount",
                style = RangeworkMono.small,
                color = MaterialTheme.colorScheme.secondary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CollapsibleNotes(
    notes: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics {
                        contentDescription = if (expanded) "Collapse notes" else "Expand notes"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Notes".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}
