package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

internal data class BriefingStat(
    val value: String,
    val label: String,
    val colored: Boolean = false,
)

/** A single numeric value + caption pair, stacked vertically. */
@Composable
internal fun StatBlock(
    value: String,
    label: String,
    colored: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = RangeworkMono.medium,
            color = if (colored) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A horizontal strip of [StatBlock]s, evenly distributed. */
@Composable
internal fun BriefingRow(
    stats: List<BriefingStat>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stats.forEach { stat ->
            StatBlock(
                value = stat.value,
                label = stat.label,
                colored = stat.colored,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatBlockPreview() {
    RangeworkTheme {
        StatBlock(
            value = "40",
            label = "Balls",
            colored = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BriefingRowPreview() {
    RangeworkTheme {
        BriefingRow(
            stats = listOf(
                BriefingStat(value = "40", label = "Balls", colored = true),
                BriefingStat(value = "4", label = "Instructions"),
                BriefingStat(value = "3", label = "Sessions"),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
