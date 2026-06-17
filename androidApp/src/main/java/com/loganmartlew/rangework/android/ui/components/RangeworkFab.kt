package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * Standard FAB using the theme's [primaryContainer] fill (Deep Fairway).
 * Used on list screens.
 */
@Composable
internal fun RangeworkFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Extended FAB with an [Add] icon and a text label.
 * Shown when the list is empty to provide a more prominent call to action.
 */
@Composable
internal fun RangeworkExtendedFab(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
        },
        text = { Text(text) },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun RangeworkFabPreview() {
    RangeworkTheme {
        RangeworkFab(
            onClick = {},
            contentDescription = "New unit",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RangeworkExtendedFabPreview() {
    RangeworkTheme {
        RangeworkExtendedFab(
            onClick = {},
            text = "New unit",
            modifier = Modifier.padding(16.dp),
        )
    }
}
