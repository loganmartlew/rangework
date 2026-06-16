package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.AuthUiState
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SnapshotMetricCard
import com.loganmartlew.rangework.android.ui.components.WelcomeHomeCard
import com.loganmartlew.rangework.shared.auth.AuthState

@Composable
internal fun OverviewScreen(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
    isExpandedLayout: Boolean,
    onCreateUnit: () -> Unit,
    onCreateSession: () -> Unit,
) {
    val signedInState = authUiState.authState as AuthState.SignedIn
    val primaryActionLabel = if (plannerUiState.units.isEmpty()) "Create your first unit" else "New unit"
    val secondaryActionLabel = if (plannerUiState.sessions.isEmpty()) "Start a session template" else "New session"

    ScrollableScreen {
        WelcomeHomeCard(
            signedInLabel = signedInState.userEmail ?: signedInState.userId,
            body = if (plannerUiState.units.isEmpty()) {
                "Start by shaping one repeatable practice unit. Once the building blocks are in place, your full range sessions come together fast."
            } else {
                "Your planning workspace is ready. Build full sessions from saved units and arrive at the range with structure already mapped out."
            },
            primaryActionLabel = primaryActionLabel,
            onPrimaryAction = onCreateUnit,
            secondaryActionLabel = secondaryActionLabel,
            onSecondaryAction = onCreateSession,
            secondaryEnabled = plannerUiState.dataConfigured && plannerUiState.units.isNotEmpty(),
        )
        if (!plannerUiState.dataConfigured) {
            EntryHighlightCard(
                title = "Planning unavailable",
                body = PlannerStatus.Unavailable.text,
            )
            return@ScrollableScreen
        }
        if (isExpandedLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SnapshotMetricCard(
                        label = "Practice units",
                        value = plannerUiState.units.size.toString(),
                        body = "Saved building blocks ready to reuse.",
                    )
                    SnapshotMetricCard(
                        label = "Session templates",
                        value = plannerUiState.sessions.size.toString(),
                        body = "Structured plans assembled from your live units.",
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EntryHighlightCard(
                        title = "Next move",
                        body = if (plannerUiState.sessions.isEmpty()) {
                            "Build a first session template once your units feel solid."
                        } else {
                            "Tighten the unit details first, then use sessions to string them into a repeatable practice block."
                        },
                    )
                }
            }
        } else {
            SnapshotMetricCard(
                label = "Practice units",
                value = plannerUiState.units.size.toString(),
                body = "Saved building blocks ready to reuse.",
            )
            SnapshotMetricCard(
                label = "Session templates",
                value = plannerUiState.sessions.size.toString(),
                body = "Structured plans assembled from your live units.",
            )
            EntryHighlightCard(
                title = "Next move",
                body = if (plannerUiState.sessions.isEmpty()) {
                    "Build a first session template once your units feel solid."
                } else {
                    "Tighten the unit details first, then use sessions to string them into a repeatable practice block."
                },
            )
        }
    }
}
