package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * A narrow strip displayed beneath the top app bar that shows a live running
 * total for the editor currently on screen (ball count, rep total, etc.).
 * Wrap in a [androidx.compose.foundation.layout.Column] above the scrollable
 * editor content inside the scaffold body.
 */
@Composable
internal fun StickyTotalBar(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = RangeworkMono.medium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true)
@Composable
private fun StickyTotalBarPreview() {
    RangeworkTheme {
        StickyTotalBar(label = "Total balls", value = "80")
    }
}

@Preview(showBackground = true, name = "Zero total")
@Composable
private fun StickyTotalBarZeroPreview() {
    RangeworkTheme {
        StickyTotalBar(label = "Total balls", value = "0")
    }
}
