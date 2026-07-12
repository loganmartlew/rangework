package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubGlyphShape
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.toGlyphShape

/** One completed Ball Step in the edit sheet: its global step index and 1-based ordinal. */
internal data class BallEditEntry(val stepIndex: Int, val ballNumber: Int)

/** One ball instruction's section within the block-scoped edit sheet. */
internal data class BallEditGroup(val instructionText: String, val entries: List<BallEditEntry>)

/**
 * The per-ball correction sheet (design "per-ball edit sheet"), scoped to the whole
 * block's completed Ball Steps, grouped into one section per ball instruction
 * (newest instruction first; within a section, newest ball first). A row shows
 * "Ball N" (numbered within its section) and the value summary in enabled-type
 * order (`—` per unobserved type, italic "not observed" for an empty ball).
 * Tapping a row expands it (single-expanded accordion, shared across sections) to
 * the same chip/launcher surfaces as capture — pre-selected, no tally counts, no
 * +1/−, no Save: every tap writes through immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BallEditSheet(
    groups: List<BallEditGroup>,
    enabledTypes: List<ObservationType>,
    observationsByStep: Map<Int, Observation>,
    handedness: Handedness,
    steps: List<SnapshotStep> = emptyList(),
    clubOverrides: Map<String, String> = emptyMap(),
    enabledClubs: List<Club> = emptyList(),
    expandedStepIndex: Int?,
    enabled: Boolean,
    onToggleExpand: (Int) -> Unit,
    onEditChip: (stepIndex: Int, typeId: String, value: String) -> Unit,
    onOpenGrid: (stepIndex: Int, type: ObservationType) -> Unit,
    onDismiss: () -> Unit,
) {
    val ordered = orderedCaptureTypes(enabledTypes)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Recorded balls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Tap a ball to correct its observations · changes save immediately",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            groups.forEachIndexed { groupIndex, group ->
                if (groupIndex > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                Text(
                    text = group.instructionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                group.entries.asReversed().forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider()
                    val expanded = expandedStepIndex == entry.stepIndex
                    BallEditRow(
                        entry = entry,
                        observation = observationsByStep[entry.stepIndex],
                        enabledTypes = ordered,
                        handedness = handedness,
                        expanded = expanded,
                        enabled = enabled,
                        onToggle = { onToggleExpand(entry.stepIndex) },
                    )
                    if (expanded && enabled) {
                        BallEditor(
                            stepIndex = entry.stepIndex,
                            observation = observationsByStep[entry.stepIndex],
                            enabledTypes = ordered,
                            handedness = handedness,
                            steps = steps,
                            clubOverrides = clubOverrides,
                            enabledClubs = enabledClubs,
                            onEditChip = onEditChip,
                            onOpenGrid = onOpenGrid,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BallEditRow(
    entry: BallEditEntry,
    observation: Observation?,
    enabledTypes: List<ObservationType>,
    handedness: Handedness,
    expanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onToggle) else it }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Ball ${entry.ballNumber}",
            style = RangeworkMono.small,
            color = MaterialTheme.colorScheme.secondary,
        )
        BallSummary(
            observation = observation,
            enabledTypes = enabledTypes,
            handedness = handedness,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BallSummary(
    observation: Observation?,
    enabledTypes: List<ObservationType>,
    handedness: Handedness,
    modifier: Modifier = Modifier,
) {
    val anyObserved = enabledTypes.any { observation?.value(it) != null }
    if (!anyObserved) {
        Text(
            text = "not observed",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val summary = enabledTypes.joinToString(" · ") { type ->
        observation?.value(type)?.let { observationValueLabel(type, it, handedness) } ?: "—"
    }
    Text(
        text = summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun BallEditor(
    stepIndex: Int,
    observation: Observation?,
    enabledTypes: List<ObservationType>,
    handedness: Handedness,
    steps: List<SnapshotStep>,
    clubOverrides: Map<String, String>,
    enabledClubs: List<Club>,
    onEditChip: (stepIndex: Int, typeId: String, value: String) -> Unit,
    onOpenGrid: (stepIndex: Int, type: ObservationType) -> Unit,
) {
    val ballShape = remember(stepIndex, steps, clubOverrides, enabledClubs) {
        if (stepIndex >= steps.size) return@remember ClubGlyphShape.IRON
        val clubCode = clubOverrides[stepIndex.toString()] ?: steps[stepIndex].club
        enabledClubs.firstOrNull { it.code == clubCode }?.category.toGlyphShape()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (type in enabledTypes) {
            val current = observation?.value(type)
            if (type == ObservationType.STRIKE_LOCATION || type == ObservationType.SHAPE) {
                GridLauncherRow(
                    type = type,
                    stagedValue = current,
                    denominatorText = null,
                    handedness = handedness,
                    clubGlyphShape = ballShape,
                    arming = false,
                    enabled = true,
                    onOpen = { onOpenGrid(stepIndex, type) },
                )
            } else {
                ObservationChipRow(
                    type = type,
                    selectedValue = current,
                    tally = null,
                    denominatorText = null,
                    successCriterion = null,
                    arming = false,
                    enabled = true,
                    onSelect = { value -> onEditChip(stepIndex, type.id, value) },
                )
            }
        }
    }
}
