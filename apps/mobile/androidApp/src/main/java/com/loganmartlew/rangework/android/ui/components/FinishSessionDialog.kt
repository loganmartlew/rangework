package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.shared.model.ExecutionBlock
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.progress

/**
 * Three-way dialog shown when finishing with incomplete steps. Names what's
 * incomplete in block terms ("Backswing ladder 8/24, Random flags untouched")
 * and never blocks finishing.
 */
@Composable
internal fun FinishSessionDialog(
    blocks: List<ExecutionBlock>,
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    onCompleteRemaining: () -> Unit,
    onFinishAsIs: () -> Unit,
    onDismiss: () -> Unit,
) {
    val incompleteSummaries = blocks.mapNotNull { block ->
        val progress = block.progress(steps, completedStepIndices)
        when {
            progress.isComplete -> null
            progress.isUntouched -> "${block.unit.unitTitle} untouched"
            progress.totalBalls > 0 ->
                "${block.unit.unitTitle} ${progress.completedBalls}/${progress.totalBalls}"
            else ->
                "${block.unit.unitTitle} ${progress.completedSteps}/${progress.totalSteps}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish with incomplete blocks?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = incompleteSummaries.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "You can mark the remaining steps complete, or finish leaving them as they are.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedButton(
                    onClick = onCompleteRemaining,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Complete remaining steps and finish" },
                ) {
                    Text("Complete remaining steps")
                }
                OutlinedButton(
                    onClick = onFinishAsIs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Finish as-is" },
                ) {
                    Text("Finish as-is")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Cancel finish" },
                ) {
                    Text("Cancel")
                }
            }
        },
    )
}
