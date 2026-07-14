package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.estimateDurationMinutes

/**
 * Presentation labels for the Observation Type vocabulary. Storage/ids live in
 * shared [ObservationType]; these strings are androidApp-side copy only and are
 * rendered in catalog order regardless of toggle order.
 */
internal fun observationTypeLabel(type: ObservationType): String = when (type) {
    ObservationType.SUCCESS -> "Success"
    ObservationType.STRIKE_LOCATION -> "Strike location"
    ObservationType.CONTACT -> "Contact"
    ObservationType.SHAPE -> "Shape"
    ObservationType.DISTANCE -> "Distance"
    ObservationType.DIRECTION -> "Direction"
}

internal fun ballSummary(ballCount: Int?): String = when (ballCount) {
    null -> "Uncounted"
    0 -> "0 balls"
    1 -> "1 ball"
    else -> "$ballCount balls"
}

internal fun sessionEditorTotalText(totalBalls: Int): String {
    if (totalBalls == 0) return ballSummary(0)
    val durationMinutes = estimateDurationMinutes(totalBalls)
    return "${ballSummary(totalBalls)} · ~${durationMinutes}min"
}
