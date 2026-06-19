package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun RangeSessionHistoryItem(
    session: CompletedRangeSessionSummary,
    modifier: Modifier = Modifier,
) {
    val startedLocalDateTime = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val completedLocalDateTime = session.completedAt.toLocalDateTime(TimeZone.currentSystemDefault())

    val dateLabel = startedLocalDateTime.date.toString()
    val timeLabel = buildString {
        val hour = startedLocalDateTime.hour
        val minute = startedLocalDateTime.minute
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        append("$displayHour:${minute.toString().padStart(2, '0')} $amPm")
    }

    val elapsedMinutes = session.elapsedSeconds / 60
    val elapsedSeconds = session.elapsedSeconds % 60
    val timeDisplay = when {
        elapsedMinutes > 0 -> "${elapsedMinutes}m ${elapsedSeconds}s"
        else -> "${elapsedSeconds}s"
    }

    val completionPercent = if (session.totalSteps > 0) {
        (session.completedStepCount * 100) / session.totalSteps
    } else {
        0
    }

    val progressText = "${session.completedStepCount}/${session.totalSteps}"

    val accessibleDescription = "Session completed on $dateLabel at $timeLabel, " +
        "$progressText steps completed, $completionPercent percent complete, elapsed $timeDisplay"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$completionPercent%",
                        style = RangeworkMono.small,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = progressText,
                        style = RangeworkMono.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = timeDisplay,
                    style = RangeworkMono.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
    }
}
