package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/** A non-interactive pill showing a ball count using the mono numeric style. */
@Composable
internal fun BallCountPill(
    count: Int,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = "$count balls",
                style = RangeworkMono.small,
            )
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

/** A non-interactive pill showing a club name. */
@Composable
internal fun ClubChip(
    name: String,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = name,
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
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun MetadataPillsPreview() {
    RangeworkTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BallCountPill(count = 40)
            ClubChip(name = "7 Iron")
        }
    }
}
