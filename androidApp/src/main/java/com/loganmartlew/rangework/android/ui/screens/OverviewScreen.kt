package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.AuthUiState
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.model.NextMoveState
import com.loganmartlew.rangework.shared.model.RecentItem

@Composable
internal fun OverviewScreen(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
    isExpandedLayout: Boolean,
    onNavigateToUnits: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToUnitDetail: (String) -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    onCreateUnit: () -> Unit,
    onCreateSession: () -> Unit,
    onEditUnit: (String) -> Unit,
    onEditSession: (String) -> Unit,
) {
    val signedInState = authUiState.authState as? AuthState.SignedIn
    val isFirstRun = plannerUiState.hasLoaded &&
        plannerUiState.units.isEmpty() &&
        plannerUiState.sessions.isEmpty()

    ScrollableScreen {
        if (!plannerUiState.dataConfigured) {
            EntryHighlightCard(
                title = "Planning unavailable",
                body = PlannerStatus.Unavailable.text,
            )
            return@ScrollableScreen
        }

        if (isFirstRun) {
            EmptyStateCard(
                icon = Icons.Rounded.Widgets,
                title = "Plan sharper range sessions",
                body = "Build reusable practice units and combine them into focused session plans.",
                actionLabel = "Create your first unit",
                onAction = onCreateUnit,
            )
            return@ScrollableScreen
        }

        val nameFromEmail = signedInState?.userEmail
            ?.substringBefore("@")
            ?.replaceFirstChar { it.uppercaseChar() }
        val greeting = "Welcome back${if (nameFromEmail != null) ", $nameFromEmail" else ""}"
        val email = signedInState?.userEmail

        if (isExpandedLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GreetingStrip(greeting = greeting, email = email)
                    StatCardRow(
                        unitCount = plannerUiState.units.size,
                        sessionCount = plannerUiState.sessions.size,
                        onNavigateToUnits = onNavigateToUnits,
                        onNavigateToSessions = onNavigateToSessions,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NextMoveCard(
                        nextMoveState = plannerUiState.nextMoveState,
                        recentItems = plannerUiState.recentItems,
                        onCreateUnit = onCreateUnit,
                        onCreateSession = onCreateSession,
                        onNavigateToSessionDetail = onNavigateToSessionDetail,
                        onEditUnit = onEditUnit,
                        onEditSession = onEditSession,
                    )
                    RecentlyUsedSection(
                        recentItems = plannerUiState.recentItems,
                        onNavigateToUnitDetail = onNavigateToUnitDetail,
                        onNavigateToSessionDetail = onNavigateToSessionDetail,
                    )
                }
            }
        } else {
            GreetingStrip(greeting = greeting, email = email)
            StatCardRow(
                unitCount = plannerUiState.units.size,
                sessionCount = plannerUiState.sessions.size,
                onNavigateToUnits = onNavigateToUnits,
                onNavigateToSessions = onNavigateToSessions,
            )
            NextMoveCard(
                nextMoveState = plannerUiState.nextMoveState,
                recentItems = plannerUiState.recentItems,
                onCreateUnit = onCreateUnit,
                onCreateSession = onCreateSession,
                onNavigateToSessionDetail = onNavigateToSessionDetail,
                onEditUnit = onEditUnit,
                onEditSession = onEditSession,
            )
            RecentlyUsedSection(
                recentItems = plannerUiState.recentItems,
                onNavigateToUnitDetail = onNavigateToUnitDetail,
                onNavigateToSessionDetail = onNavigateToSessionDetail,
            )
        }
    }
}

@Composable
private fun GreetingStrip(greeting: String, email: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
        )
        if (email != null) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCardRow(
    unitCount: Int,
    sessionCount: Int,
    onNavigateToUnits: () -> Unit,
    onNavigateToSessions: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            label = "Units",
            count = unitCount,
            onClick = onNavigateToUnits,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Sessions",
            count = sessionCount,
            onClick = onNavigateToSessions,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label, $count, open $label"
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = count.toString(),
                    style = RangeworkMono.large,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextMoveCard(
    nextMoveState: NextMoveState,
    recentItems: List<RecentItem>,
    onCreateUnit: () -> Unit,
    onCreateSession: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    onEditUnit: (String) -> Unit,
    onEditSession: (String) -> Unit,
) {
    val message: String
    val actionLabel: String
    val onAction: () -> Unit

    when (nextMoveState) {
        NextMoveState.NoUnits -> {
            message = "Build your first practice unit to get started."
            actionLabel = "Create unit"
            onAction = onCreateUnit
        }
        NextMoveState.UnitsNoSessions -> {
            message = "Combine your units into a session template."
            actionLabel = "New session"
            onAction = onCreateSession
        }
        NextMoveState.Both -> {
            val mostRecentSession = recentItems.filterIsInstance<RecentItem.Session>().firstOrNull()
            message = "Pick a session to run at the range."
            actionLabel = if (mostRecentSession != null) "Open most recent" else "New session"
            onAction = if (mostRecentSession != null) {
                { onNavigateToSessionDetail(mostRecentSession.id) }
            } else {
                onCreateSession
            }
        }
        is NextMoveState.ResumeEditing -> {
            val entityLabel = if (nextMoveState.isUnit) "unit" else "session"
            message = "Resume editing your $entityLabel."
            actionLabel = "Resume"
            onAction = if (nextMoveState.isUnit) {
                { onEditUnit(nextMoveState.entityId) }
            } else {
                { onEditSession(nextMoveState.entityId) }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Next move".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            FilledTonalButton(
                onClick = onAction,
                modifier = Modifier.semantics { contentDescription = actionLabel },
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun RecentlyUsedSection(
    recentItems: List<RecentItem>,
    onNavigateToUnitDetail: (String) -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
) {
    if (recentItems.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Recently used".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(recentItems, key = { it.id }) { item ->
                RecentCard(
                    item = item,
                    onNavigateToUnitDetail = onNavigateToUnitDetail,
                    onNavigateToSessionDetail = onNavigateToSessionDetail,
                )
            }
        }
    }
}

@Composable
private fun RecentCard(
    item: RecentItem,
    onNavigateToUnitDetail: (String) -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
) {
    val name: String
    val typeLabel: String
    val metadata: String
    val accessibleDescription: String
    val onClick: () -> Unit

    when (item) {
        is RecentItem.Unit -> {
            val unit = item.practiceUnit
            val ballTotal = unit.instructions.mapNotNull { it.ballCount }.sum()
            name = unit.title
            typeLabel = "Unit"
            metadata = if (ballTotal > 0) "$ballTotal balls" else "${unit.instructions.size} instruction${if (unit.instructions.size != 1) "s" else ""}"
            accessibleDescription = "${unit.title}, Unit, open"
            onClick = { onNavigateToUnitDetail(unit.id) }
        }
        is RecentItem.Session -> {
            val session = item.practiceSession
            val itemCount = session.items.size
            name = session.name
            typeLabel = "Session"
            metadata = "$itemCount unit${if (itemCount != 1) "s" else ""}"
            accessibleDescription = "${session.name}, Session, open"
            onClick = { onNavigateToSessionDetail(session.id) }
        }
    }

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .semantics(mergeDescendants = true) { contentDescription = accessibleDescription },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
