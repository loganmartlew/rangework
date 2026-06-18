package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * Pure-logic state for a bounded integer stepper. Extracted for unit testing.
 */
internal data class CountStepperState(
    val value: Int,
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
) {
    val canDecrement: Boolean get() = value > min
    val canIncrement: Boolean get() = value < max

    fun increment(): CountStepperState = copy(value = (value + 1).coerceAtMost(max))
    fun decrement(): CountStepperState = copy(value = (value - 1).coerceAtLeast(min))

    /** Returns a state with [newValue] clamped into [min]..[max]. */
    fun withValue(newValue: Int): CountStepperState = copy(value = newValue.coerceIn(min, max))
}

/**
 * A − / value / + row for bounded integer entry. Buttons are disabled at bounds.
 * Minimum touch target is guaranteed by [Modifier.size(48.dp)].
 */
@Composable
internal fun CountStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
    label: String = "Count",
) {
    val state = CountStepperState(value = value, min = min, max = max)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = { onValueChange(state.decrement().value) },
            enabled = state.canDecrement,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Decrease $label" },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = null)
        }

        Text(
            text = value.toString(),
            style = RangeworkMono.medium,
            modifier = Modifier.semantics { contentDescription = "$label: $value" },
        )

        IconButton(
            onClick = { onValueChange(state.increment().value) },
            enabled = state.canIncrement,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Increase $label" },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CountStepperPreview() {
    RangeworkTheme {
        CountStepper(
            value = 10,
            onValueChange = {},
            min = 1,
            max = 50,
            label = "Ball count",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "At minimum")
@Composable
private fun CountStepperAtMinPreview() {
    RangeworkTheme {
        CountStepper(
            value = 1,
            onValueChange = {},
            min = 1,
            max = 50,
            label = "Ball count",
            modifier = Modifier.padding(16.dp),
        )
    }
}
