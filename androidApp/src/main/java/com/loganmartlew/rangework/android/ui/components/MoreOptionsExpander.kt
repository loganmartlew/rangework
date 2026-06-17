package com.loganmartlew.rangework.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

/**
 * Progressive disclosure wrapper. Auto-expands when [hasContent] is true (i.e., the
 * wrapped field already has a saved value), so populated optional fields are never hidden.
 */
@Composable
internal fun MoreOptionsExpander(
    label: String = "More options",
    hasContent: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(hasContent) }

    LaunchedEffect(hasContent) {
        if (hasContent) expanded = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { expanded = !expanded },
                    role = Role.Button,
                )
                .padding(vertical = 12.dp)
                .semantics { role = Role.Button },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $label" else "Expand $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreOptionsExpanderCollapsedPreview() {
    RangeworkTheme {
        MoreOptionsExpander(
            label = "More options",
            hasContent = false,
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Focus cue field here")
        }
    }
}

@Preview(showBackground = true, name = "Auto-expanded (has content)")
@Composable
private fun MoreOptionsExpanderExpandedPreview() {
    RangeworkTheme {
        MoreOptionsExpander(
            label = "More options",
            hasContent = true,
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Focus cue: Keep your head still")
        }
    }
}
