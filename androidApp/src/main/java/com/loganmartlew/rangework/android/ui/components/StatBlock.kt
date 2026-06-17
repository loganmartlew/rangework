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
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/** A single numeric value + caption pair, stacked vertically. */
@Composable
internal fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = RangeworkMono.medium,
            color = MaterialTheme.colorScheme.secondary,
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
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        stats.forEach { (value, label) ->
            StatBlock(value = value, label = label)
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
                "40" to "Balls",
                "4" to "Instructions",
                "3" to "Sessions",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
