package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.ExecutionBlock
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.progress

/**
 * The session overview: every Block with its progress at a glance. This
 * display — not locked doors — is what answers "did I finish everything?".
 * Finish lives here: quiet while blocks are incomplete, prominent once all
 * are complete.
 */
@Composable
internal fun BlockOverviewContent(
    blocks: List<ExecutionBlock>,
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    currentBlockIndex: Int,
    onBlockSelected: (Int) -> Unit,
    onFinish: () -> Unit,
    isFinishing: Boolean,
    modifier: Modifier = Modifier,
) {
    val allComplete = blocks.isNotEmpty() && blocks.all { block ->
        block.stepIndices.all { it in completedStepIndices }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Blocks".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            itemsIndexed(blocks) { index, block ->
                BlockOverviewRow(
                    block = block,
                    steps = steps,
                    completedStepIndices = completedStepIndices,
                    isCurrent = index == currentBlockIndex,
                    onClick = { onBlockSelected(index) },
                )
            }
        }
        val finishModifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics { contentDescription = "Finish session" }
        if (allComplete) {
            Button(onClick = onFinish, enabled = !isFinishing, modifier = finishModifier) {
                Text("Finish Session", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            OutlinedButton(onClick = onFinish, enabled = !isFinishing, modifier = finishModifier) {
                Text("Finish Session", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun BlockOverviewRow(
    block: ExecutionBlock,
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = block.progress(steps, completedStepIndices)
    val hasBalls = progress.totalBalls > 0
    // Per-block progress at a glance: "15/15 ✓", "8/24", or "—" (untouched).
    val progressText = when {
        progress.isUntouched -> "—"
        hasBalls -> "${progress.completedBalls}/${progress.totalBalls}"
        else -> "${progress.completedSteps}/${progress.totalSteps}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(block.unit.unitTitle)
                    when {
                        progress.isComplete -> append(", complete")
                        progress.isUntouched -> append(", not started")
                        hasBalls -> append(
                            ", ${progress.completedBalls} of ${progress.totalBalls} balls",
                        )
                        else -> append(
                            ", ${progress.completedSteps} of ${progress.totalSteps} steps",
                        )
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = when {
                        progress.isComplete -> MaterialTheme.colorScheme.primary
                        !progress.isUntouched -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape,
                ),
        )
        Text(
            text = block.unit.unitTitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = progressText,
                style = RangeworkMono.small,
                color = if (progress.isComplete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (progress.isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
