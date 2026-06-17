package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * A surface-elevated strip pinned at the bottom of an editor, containing a
 * full-width primary [Button]. Wire this inside a [Scaffold] via the
 * [bottomBar] slot so it stays above the IME and navigation bar.
 */
@Composable
internal fun DockedSaveBar(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DockedSaveBarPreview() {
    RangeworkTheme {
        DockedSaveBar(label = "Save unit", onClick = {})
    }
}

@Preview(showBackground = true, name = "Disabled")
@Composable
private fun DockedSaveBarDisabledPreview() {
    RangeworkTheme {
        DockedSaveBar(label = "Save unit", onClick = {}, enabled = false)
    }
}
