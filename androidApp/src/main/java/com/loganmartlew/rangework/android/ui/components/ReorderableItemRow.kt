package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * A row that combines a drag-handle icon,
 * a [content] slot, and ↑/↓ chevron buttons as the accessible reorder mechanism.
 */
@Composable
internal fun ReorderableItemRow(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: (() -> Unit)? = null,
    moveUpContentDescription: String = "Move up",
    moveDownContentDescription: String = "Move down",
    deleteContentDescription: String = "Delete",
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragHandleModifier.size(24.dp),
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                enabled = canMoveUp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMoveUp()
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = moveUpContentDescription },
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(
                enabled = canMoveDown,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMoveDown()
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = moveDownContentDescription },
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = deleteContentDescription },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReorderableItemRowPreview() {
    RangeworkTheme {
        ReorderableItemRow(
            canMoveUp = true,
            canMoveDown = true,
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Address the ball with a square stance",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true, name = "First item — can't move up")
@Composable
private fun ReorderableItemRowFirstPreview() {
    RangeworkTheme {
        ReorderableItemRow(
            canMoveUp = false,
            canMoveDown = true,
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Keep your head still through the swing",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
