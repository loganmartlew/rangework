package com.loganmartlew.rangework.android.ui

internal fun ballSummary(ballCount: Int?): String = when (ballCount) {
    null -> "Ball total unavailable"
    0 -> "0 balls"
    1 -> "1 ball"
    else -> "$ballCount balls"
}
