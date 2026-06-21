package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.DeleteAccountUiState
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen

private const val CONFIRM_WORD = "DELETE"

@Composable
internal fun DeleteAccountScreen(
    uiState: DeleteAccountUiState,
    onDeleteAccount: () -> Unit,
    onAccountDeleted: () -> Unit,
    onClearError: () -> Unit,
) {
    var confirmationText by remember { mutableStateOf("") }
    var showFinalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is DeleteAccountUiState.Deleted) {
            onAccountDeleted()
        }
    }

    if (showFinalDialog) {
        AlertDialog(
            onDismissRequest = { showFinalDialog = false },
            title = { Text("This cannot be undone") },
            text = { Text("Your account and all practice data will be permanently deleted. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinalDialog = false
                        onDeleteAccount()
                    },
                ) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    ScrollableScreen {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "What gets deleted",
                style = MaterialTheme.typography.titleMedium,
            )
            val deletedItems = listOf(
                "Profile and sign-in credentials",
                "All practice units and instructions",
                "All practice sessions",
                "Range session history",
                "Measurement preferences",
                "Club bag configuration",
            )
            deletedItems.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "This action is permanent and cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedTextField(
                value = confirmationText,
                onValueChange = {
                    confirmationText = it
                    if (uiState is DeleteAccountUiState.Error) onClearError()
                },
                label = { Text("Type $CONFIRM_WORD to confirm") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState is DeleteAccountUiState.Error,
                supportingText = if (uiState is DeleteAccountUiState.Error) {
                    { Text((uiState as DeleteAccountUiState.Error).message) }
                } else null,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { showFinalDialog = true },
            enabled = confirmationText == CONFIRM_WORD && uiState !is DeleteAccountUiState.Working,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(
                text = if (uiState is DeleteAccountUiState.Working) "Deleting…" else "Delete my account",
            )
        }
    }
}
