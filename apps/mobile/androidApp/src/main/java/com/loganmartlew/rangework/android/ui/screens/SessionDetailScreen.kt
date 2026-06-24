package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.BallCountPill
import com.loganmartlew.rangework.android.ui.components.BriefingStat
import com.loganmartlew.rangework.android.ui.components.BriefingRow
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.NumberBadge
import com.loganmartlew.rangework.android.ui.components.RangeSessionHistoryItem
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount
import com.loganmartlew.rangework.shared.model.estimateSessionDurationMinutes

@Composable
internal fun SessionDetailScreen(
    plannerUiState: PracticePlannerUiState,
    sessionId: String,
    onCreateSession: () -> Unit,
    onEditSession: () -> Unit,
    onStartSession: () -> Unit = {},
    onSessionDetailViewed: () -> Unit = {},
) {
    LaunchedEffect(sessionId) {
        onSessionDetailViewed()
    }
    val session = plannerUiState.sessions.firstOrNull { it.id == sessionId }
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }
    ScrollableScreen {
        if (session == null) {
            EntryHighlightCard(
                title = "Session not found",
                body = "This session no longer exists in the current planner data.",
            )
            FilledTonalButton(onClick = onCreateSession) {
                Text("New session")
            }
            return@ScrollableScreen
        }

        val totalBalls = session.derivedBallCount(unitsById)
        val estimatedMinutes = estimateSessionDurationMinutes(session, unitsById)
        val durationDisplay = if (estimatedMinutes == 0) "—" else "~$estimatedMinutes min"

        val isSessionExecutable = session.items.isNotEmpty() &&
            session.items.any { item ->
                (unitsById[item.practiceUnitId]?.instructions?.size ?: 0) > 0
            }
        val startSessionDisabledReason = when {
            session.items.isEmpty() -> "Add units to this session before starting."
            !isSessionExecutable -> "All units in this session have no instructions."
            else -> null
        }

        // Briefing strip: balls (primary) + unit count + estimated duration
        BriefingRow(
            stats = listOf(
                BriefingStat(
                    value = totalBalls.toString(),
                    label = "Balls",
                    colored = true,
                ),
                BriefingStat(
                    value = session.items.size.toString(),
                    label = "Units",
                ),
                BriefingStat(
                    value = durationDisplay,
                    label = "Est. time",
                ),
            ),
        )

        // Start session button
        Button(
            onClick = onStartSession,
            enabled = isSessionExecutable,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (startSessionDisabledReason != null) {
                        "Start session, disabled: $startSessionDisabledReason"
                    } else {
                        "Start session"
                    }
                },
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start session", style = MaterialTheme.typography.labelLarge)
        }
        startSessionDisabledReason?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Session notes
        session.notes?.takeIf(String::isNotBlank)?.let { notes ->
            EntryHighlightCard(title = "Session notes", body = notes)
        }

        // Session items card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Session items",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (session.items.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.EventNote,
                        title = "No items yet",
                        body = "Add practice units to this session to define what to practice.",
                        actionLabel = "Edit session",
                        onAction = onEditSession,
                    )
                } else {
                    session.items.forEachIndexed { index, item ->
                        SessionItemDetailRow(
                            item = item,
                            unit = unitsById[item.practiceUnitId],
                            position = item.order,
                            clubCatalog = plannerUiState.clubCatalog,
                            unitsById = unitsById,
                        )
                        if (index != session.items.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Range session history section
        val history = plannerUiState.completedRangeSessionHistory[sessionId] ?: emptyList()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Session history",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (history.isEmpty()) {
                    Text(
                        text = "No completed sessions yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    history.forEach { completedSession ->
                        RangeSessionHistoryItem(session = completedSession)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItemDetailRow(
    item: PracticeSessionItem,
    unit: PracticeUnit?,
    position: Int,
    clubCatalog: List<Club>,
    unitsById: Map<String, PracticeUnit>,
) {
    fun resolveClubName(code: String?): String? = code?.takeIf(String::isNotBlank)?.let { c ->
        clubCatalog.firstOrNull { it.code == c }?.displayName ?: c
    }

    // Club is only shown when item explicitly overrides the unit default
    val isClubOverride = !item.clubCode.isNullOrBlank() &&
        item.clubCode != unit?.defaultClubCode
    val overrideClubName = if (isClubOverride) resolveClubName(item.clubCode) else null

    val itemBallCount = item.derivedBallCount(unit)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Primary row: badge + unit name + ball subtotal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NumberBadge(number = position)
            Text(
                text = unit?.title ?: "Missing unit",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            itemBallCount?.let { count ->
                BallCountPill(count = count)
            }
        }

        // Secondary row: repeat count chip + override club chip (aligned after badge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = "×${item.repeatCount}",
                        style = RangeworkMono.small,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
            overrideClubName?.let { name ->
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "$name (override)",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GolfCourse,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }

        // Focus cue line (conditional, aligned after badge)
        item.focusCue?.takeIf(String::isNotBlank)?.let { cue ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(start = 40.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Focus cue",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = cue,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        item.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 40.dp),
            )
        }
    }
}
