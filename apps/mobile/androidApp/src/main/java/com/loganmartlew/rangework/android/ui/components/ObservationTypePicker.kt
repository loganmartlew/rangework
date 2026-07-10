package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.observationTypeLabel
import com.loganmartlew.rangework.shared.model.ObservationType

/**
 * Chip group letting an author toggle which Observation Types a Session Item
 * enables. Chips render in [ObservationType] catalog order regardless of the
 * item's toggle order. Success is gated on the selected unit having a criterion:
 * when [successEnabled] is false the Success chip is disabled and a supporting
 * line points the author at the unit editor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ObservationTypePicker(
    selectedTypes: List<ObservationType>,
    successEnabled: Boolean,
    enabled: Boolean,
    onToggle: (ObservationType) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Observe",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ObservationType.entries.forEach { type ->
                // Keep a currently-selected chip tappable so a stale SUCCESS (unit
                // criterion removed after the item was configured) can always be
                // deselected — otherwise a failed save is a dead-end. Only an
                // *unselected* SUCCESS chip on a criterion-less unit is disabled.
                val chipEnabled = enabled &&
                    (type != ObservationType.SUCCESS || successEnabled || type in selectedTypes)
                FilterChip(
                    selected = type in selectedTypes,
                    onClick = { onToggle(type) },
                    enabled = chipEnabled,
                    label = { Text(observationTypeLabel(type)) },
                    leadingIcon = if (type in selectedTypes) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
        if (!successEnabled) {
            Text(
                text = "Add a success criterion to the unit to record hit/miss.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
