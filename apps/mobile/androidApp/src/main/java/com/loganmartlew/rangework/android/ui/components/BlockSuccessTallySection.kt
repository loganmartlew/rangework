package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * The block-level success tally: the aggregate twin of the per-ball Success
 * chips, for items that define a Success Criterion but did *not* enable the
 * Success Observation Type (CONTEXT.md — Block Result, count provenance).
 *
 * Sits with the capture surface under the counter rather than with the Block
 * Result note: the count is data, like Observations, not prose — and burying it
 * beside the note made it undiscoverable. The two success modes are mutually
 * exclusive, so this and the Success chip row never render together.
 *
 * Never prompted (design): the stepper idles at "Not counted" until the first
 * tap, so an untouched block records no count at all — unset ≠ 0. It stays
 * editable once the block's balls are done, because tallying is what you do
 * *after* hitting; the session-level freeze is the recorder's job.
 */
@Composable
internal fun BlockSuccessTallySection(
    criterion: String?,
    count: Int?,
    totalBalls: Int,
    onSetCount: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The rubric rides the header, as on the capture rows: without the
            // criterion the bare number is the meaningless count design disallows.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Success".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (criterion != null) {
                    Text(
                        text = " · $criterion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountStepper(
                    value = count ?: 0,
                    onValueChange = onSetCount,
                    min = 0,
                    max = totalBalls,
                    label = "Success count",
                )
                Text(
                    text = if (count == null) "Not counted" else "of $totalBalls balls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (count != null) {
                    TextButton(
                        onClick = { onSetCount(null) },
                        modifier = Modifier.semantics {
                            contentDescription = "Remove success count"
                        },
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Not counted")
@Composable
private fun BlockSuccessTallyUnsetPreview() {
    RangeworkTheme {
        BlockSuccessTallySection(
            criterion = "Within 10 feet",
            count = null,
            totalBalls = 10,
            onSetCount = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Counted")
@Composable
private fun BlockSuccessTallyCountedPreview() {
    RangeworkTheme {
        BlockSuccessTallySection(
            criterion = "Within 10 feet",
            count = 6,
            totalBalls = 10,
            onSetCount = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
