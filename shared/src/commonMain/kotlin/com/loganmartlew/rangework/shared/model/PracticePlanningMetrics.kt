package com.loganmartlew.rangework.shared.model

fun PracticeUnit.derivedBallCount(): Int = instructions.sumOf { instruction ->
    instruction.ballCount ?: 0
}

fun PracticeSessionItem.derivedBallCount(unit: PracticeUnit?): Int? = unit?.derivedBallCount()?.let { baseBallCount ->
    baseBallCount * repeatCount
}

fun PracticeSession.derivedBallCount(unitsById: Map<String, PracticeUnit>): Int = items.sumOf { item ->
    item.derivedBallCount(unitsById[item.practiceUnitId]) ?: 0
}
