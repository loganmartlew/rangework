package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.model.SECONDS_PER_BALL

internal fun ballSummary(ballCount: Int?): String = when (ballCount) {
    null -> "Ball total unavailable"
    0 -> "0 balls"
    1 -> "1 ball"
    else -> "$ballCount balls"
}

internal fun sessionEditorTotalText(totalBalls: Int): String {
    if (totalBalls == 0) return ballSummary(0)
    val durationMinutes = (totalBalls * SECONDS_PER_BALL + 30) / 60
    return "${ballSummary(totalBalls)} · ~${durationMinutes}min"
}
