package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.SnapshotStep

private sealed interface DrawerRow {
    data class UnitHeader(
        val unitIndex: Int,
        val unitTitle: String,
        val completedCount: Int,
        val totalCount: Int,
    ) : DrawerRow

    data class StepItem(
        val globalStepIndex: Int,
        val step: SnapshotStep,
    ) : DrawerRow
}

@Composable
internal fun StepListDrawerContent(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    currentStepIndex: Int,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val rows = remember(steps, completedStepIndices) {
        val unitToStepIndices = mutableMapOf<Int, MutableList<Int>>()
        steps.forEachIndexed { i, s ->
            unitToStepIndices.getOrPut(s.unitIndex) { mutableListOf() }.add(i)
        }

        buildList<DrawerRow> {
            var prevUnitIndex = -1
            steps.forEachIndexed { index, step ->
                if (step.unitIndex != prevUnitIndex) {
                    val stepsForUnit = unitToStepIndices[step.unitIndex] ?: emptyList()
                    val completedCount = stepsForUnit.count { it in completedStepIndices }
                    add(
                        DrawerRow.UnitHeader(
                            unitIndex = step.unitIndex,
                            unitTitle = step.unitTitle,
                            completedCount = completedCount,
                            totalCount = stepsForUnit.size,
                        )
                    )
                    prevUnitIndex = step.unitIndex
                }
                add(DrawerRow.StepItem(globalStepIndex = index, step = step))
            }
        }
    }

    if (rows.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No steps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is DrawerRow.UnitHeader -> "header_${row.unitIndex}"
                    is DrawerRow.StepItem -> "step_${row.globalStepIndex}"
                }
            },
        ) { row ->
            when (row) {
                is DrawerRow.UnitHeader -> {
                    UnitHeaderRow(
                        title = row.unitTitle,
                        completedCount = row.completedCount,
                        totalCount = row.totalCount,
                    )
                }
                is DrawerRow.StepItem -> {
                    StepListItem(
                        stepIndex = row.globalStepIndex,
                        instructionText = row.step.instructionText,
                        repNumber = row.step.repNumber,
                        totalReps = row.step.totalReps,
                        isCompleted = row.globalStepIndex in completedStepIndices,
                        isCurrent = row.globalStepIndex == currentStepIndex,
                        onClick = { onStepSelected(row.globalStepIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitHeaderRow(
    title: String,
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (totalCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$completedCount/$totalCount",
                style = RangeworkMono.small,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
