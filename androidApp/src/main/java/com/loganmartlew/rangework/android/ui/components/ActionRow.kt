package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryEnabled: Boolean,
    secondaryEnabled: Boolean,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            itemName = secondaryLabel,
            onConfirm = {
                showDeleteDialog = false
                onSecondary()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
            onClick = onPrimary,
        ) {
            Text(primaryLabel)
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = secondaryEnabled,
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(secondaryLabel)
        }
    }
}
