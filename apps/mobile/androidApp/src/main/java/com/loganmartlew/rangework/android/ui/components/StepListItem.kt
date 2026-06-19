package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono

@Composable
internal fun StepListItem(
    stepIndex: Int,
    instructionText: String,
    repNumber: Int,
    totalReps: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val semanticsDescription = buildString {
        append("Step ${stepIndex + 1}, $instructionText")
        if (totalReps > 1) append(", Rep $repNumber of $totalReps")
        if (isCurrent) append(", current step")
        if (isCompleted) append(", completed") else append(", incomplete")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = semanticsDescription }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${stepIndex + 1}",
            style = RangeworkMono.small,
            color = secondaryColor,
            modifier = Modifier.width(28.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = instructionText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (totalReps > 1) {
                Text(
                    text = "Rep $repNumber of $totalReps",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                )
            }
        }
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
