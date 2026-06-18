package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.RangeSession

@Composable
internal fun RangeSessionProgressHeader(
    rangeSession: RangeSession,
    completedStepIndices: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val steps = rangeSession.snapshot.steps
    val totalSteps = steps.size
    val completedStepCount = completedStepIndices.size
    val completionFraction = if (totalSteps == 0) 0f else completedStepCount.toFloat() / totalSteps.toFloat()
    val completionPercent = (completionFraction * 100).toInt()

    val hasAnyBalls = steps.any { it.ballCount != null }
    val completedBalls = steps
        .filterIndexed { i, _ -> i in completedStepIndices }
        .sumOf { it.ballCount ?: 0 }
    val totalBalls = steps.sumOf { it.ballCount ?: 0 }

    val stepsByUnit = steps
        .mapIndexed { i, step -> Pair(i, step) }
        .groupBy { (_, step) -> step.unitIndex }
    val unitCompletions = rangeSession.snapshot.units.mapIndexed { unitIdx, unit ->
        val stepsForUnit = stepsByUnit[unitIdx] ?: emptyList()
        val completedInUnit = stepsForUnit.count { (i, _) -> i in completedStepIndices }
        Triple(unit.unitTitle, completedInUnit, stepsForUnit.size)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { completionFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Session progress: $completionPercent percent"
                    },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = buildString {
                                append("$completedStepCount of $totalSteps steps completed")
                                if (hasAnyBalls) append(", $completedBalls of $totalBalls balls")
                                append(", $completionPercent percent")
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "$completedStepCount/$totalSteps",
                            style = RangeworkMono.medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (hasAnyBalls) "$completedBalls/$totalBalls" else "—",
                            style = RangeworkMono.medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (hasAnyBalls) {
                            Text(
                                text = "balls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = "$completionPercent%",
                        style = RangeworkMono.medium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (unitCompletions.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        unitCompletions.forEachIndexed { index, (title, completed, total) ->
                            val dotColor = when {
                                total == 0 -> MaterialTheme.colorScheme.outline
                                completed == total -> MaterialTheme.colorScheme.primary
                                completed > 0 -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                                    .semantics {
                                        contentDescription =
                                            "Unit ${index + 1}: $title, $completed of $total steps complete"
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
