package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ActiveRangeSessionCard(
    session: ActiveRangeSessionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (session.totalSteps > 0) {
        session.completedStepCount.toFloat() / session.totalSteps.toFloat()
    } else {
        0f
    }

    val startedLocalDateTime = session.startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val startedTime = buildString {
        val hour = startedLocalDateTime.hour
        val minute = startedLocalDateTime.minute
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        append("$displayHour:${minute.toString().padStart(2, '0')} $amPm")
    }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val sessionDate = startedLocalDateTime.date
    val dateLabel = if (sessionDate == today) "Today" else sessionDate.toString()

    val progressText = "${session.completedStepCount}/${session.totalSteps}"
    val accessibleDescription = "Active session: ${session.sessionName}, $progressText steps, " +
        "started $dateLabel at $startedTime. Tap to resume."

    OutlinedCard(
        onClick = onClick,
        modifier = modifier
            .width(220.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = session.sessionName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = progressText,
                    style = RangeworkMono.small,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$dateLabel, $startedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
