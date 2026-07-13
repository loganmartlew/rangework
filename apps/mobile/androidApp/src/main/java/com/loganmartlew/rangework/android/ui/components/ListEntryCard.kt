package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

@Composable
internal fun ListEntryCard(
    title: String,
    supportingText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
    metadataRow: (@Composable () -> Unit)? = null,
    overflowContentDescription: String = "More options",
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metadataRow != null) {
                    metadataRow()
                }
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                OverflowMenu(
                    contentDescription = overflowContentDescription,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onArchive = onArchive,
                    onUnarchive = onUnarchive,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListEntryCardPreview() {
    RangeworkTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ListEntryCard(
                title = "Iron shot fundamentals",
                supportingText = "7 Iron  •  Hinge at impact  •  Follow through high",
                metadataRow = {
                    Text(
                        text = "7 Iron  •  4 instructions  •  40 balls",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                onClick = {},
                onEdit = {},
                onDelete = {},
                onDuplicate = {},
            )
            ListEntryCard(
                title = "Putting drills",
                supportingText = "Putter  •  Gate drill",
                metadataRow = {
                    Text(
                        text = "2 instructions  •  20 balls",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                onClick = {},
                onEdit = {},
                onDelete = {},
            )
        }
    }
}
