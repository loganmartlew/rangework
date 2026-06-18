package com.loganmartlew.rangework.android.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Shows a snackbar with an "Undo" action. Calls [onRestore] if the user taps Undo
 * before the snackbar expires.
 */
internal suspend fun SnackbarHostState.showUndoSnackbar(
    message: String,
    onRestore: () -> Unit,
) {
    val result = showSnackbar(
        message = message,
        actionLabel = "Undo",
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed) {
        onRestore()
    }
}
