package com.loganmartlew.rangework.android.ui

sealed class PlannerStatus(val showAsSnackbar: Boolean) {
    data class Notification(val text: String) : PlannerStatus(showAsSnackbar = true)
    data class Info(val text: String) : PlannerStatus(showAsSnackbar = false)
    data object Unavailable : PlannerStatus(showAsSnackbar = false) {
        val text = "Practice planning is not available in this build yet."
    }
    data object SchemaNotReady : PlannerStatus(showAsSnackbar = false) {
        val text = "Planning workspace still being prepared. Refresh once setup is complete."
    }
}
