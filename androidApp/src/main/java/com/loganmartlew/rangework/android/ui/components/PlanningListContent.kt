package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.loganmartlew.rangework.android.ui.PlannerStatus

@Composable
internal fun ColumnScope.PlanningListContent(
    dataConfigured: Boolean,
    status: PlannerStatus?,
    hasLoaded: Boolean,
    isLoading: Boolean,
    unavailableContent: @Composable ColumnScope.() -> Unit,
    skeletonContent: @Composable ColumnScope.() -> Unit = { SkeletonList() },
    listContent: @Composable ColumnScope.() -> Unit,
) {
    if (!dataConfigured) {
        unavailableContent()
    } else if (!hasLoaded && isLoading) {
        skeletonContent()
    } else {
        listContent()
    }
}
