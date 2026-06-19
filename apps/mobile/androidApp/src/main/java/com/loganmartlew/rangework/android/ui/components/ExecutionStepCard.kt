package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.SnapshotStep

@Composable
internal fun ExecutionStepCard(
    step: SnapshotStep,
    stepNumber: Int,
    totalSteps: Int,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    enabledClubs: List<Club> = emptyList(),
    onClubOverride: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val showClubPicker = remember { mutableStateOf(false) }
    val contextLabel = buildString {
        append(step.unitTitle)
        append(" — Step ")
        append(stepNumber)
        append(" of ")
        append(totalSteps)
        if (step.totalReps > 1) {
            append(", Rep ")
            append(step.repNumber)
            append(" of ")
            append(step.totalReps)
        }
    }

    val cardColors = if (isCompleted) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(contextLabel)
                    append(". Instruction: ${step.instructionText}")
                    step.ballCount?.let { append(". $it balls") }
                    step.clubDisplayName?.let { append(". Club: $it") }
                    step.focusCue?.takeIf(String::isNotBlank)?.let { append(". Focus cue: $it") }
                    step.notes?.takeIf(String::isNotBlank)?.let { append(". Notes: $it") }
                    if (isCompleted) append(". Completed") else append(". Incomplete")
                }
            },
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Unit context header
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = step.unitTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        append("Step $stepNumber of $totalSteps")
                        if (step.totalReps > 1) {
                            append(" · Rep ${step.repNumber} of ${step.totalReps}")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // Primary instruction text
            Text(
                text = step.instructionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Ball count + club row
            val hasBallCount = step.ballCount != null
            val hasClub = !step.clubDisplayName.isNullOrBlank()
            if (hasBallCount || hasClub) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    step.ballCount?.let { count ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = count.toString(),
                                style = RangeworkMono.medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "balls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    step.clubDisplayName?.takeIf(String::isNotBlank)?.let { name ->
                        val canOverride = enabledClubs.isNotEmpty() && step.club != null
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (canOverride) {
                                Modifier
                                    .semantics { contentDescription = "Club: $name, tap to change" }
                            } else {
                                Modifier
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.GolfCourse,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (canOverride) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.semantics { contentDescription = "Club override: $name, tap to change" },
                                )
                            } else {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            if (canOverride) {
                                IconButton(
                                    onClick = { showClubPicker.value = true },
                                    modifier = Modifier.semantics { contentDescription = "Change club" },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GolfCourse,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val stepClub = step.club
            if (showClubPicker.value && enabledClubs.isNotEmpty() && stepClub != null) {
                ClubOverridePickerDialog(
                    currentClubCode = stepClub,
                    availableClubs = enabledClubs,
                    onClubSelected = { newClubCode ->
                        onClubOverride(newClubCode)
                        showClubPicker.value = false
                    },
                    onDismiss = { showClubPicker.value = false },
                )
            }

            // Focus cue
            step.focusCue?.takeIf(String::isNotBlank)?.let { cue ->
                FocusCard(cue = cue)
            }

            // Notes
            step.notes?.takeIf(String::isNotBlank)?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Completion toggle
            if (isCompleted) {
                Button(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Mark step incomplete" },
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Completed", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                FilledTonalButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Mark step complete" },
                ) {
                    Text("Mark Complete", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
