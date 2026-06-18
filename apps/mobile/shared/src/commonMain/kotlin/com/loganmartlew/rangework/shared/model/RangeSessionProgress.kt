package com.loganmartlew.rangework.shared.model

fun RangeSession.completedStepCount(): Int = completedSteps.size

fun RangeSession.totalStepCount(): Int = snapshot.steps.size

fun RangeSession.completionPercentage(): Double {
    val total = totalStepCount()
    if (total == 0) return 0.0
    return completedStepCount().toDouble() / total.toDouble()
}

fun RangeSession.completedBalls(): Int {
    val completedIndices = completedSteps.map { it.stepIndex }.toSet()
    return snapshot.steps
        .filterIndexed { index, _ -> index in completedIndices }
        .sumOf { it.ballCount ?: 0 }
}

fun RangeSession.totalBalls(): Int =
    snapshot.steps.sumOf { it.ballCount ?: 0 }

fun RangeSession.completedUnits(): Int {
    val completedIndices = completedSteps.map { it.stepIndex }.toSet()
    val stepsByUnit = snapshot.steps
        .mapIndexed { index, step -> Pair(index, step) }
        .groupBy { (_, step) -> step.unitIndex }
    return stepsByUnit.count { (_, indexedSteps) ->
        indexedSteps.all { (stepIndex, _) -> stepIndex in completedIndices }
    }
}

fun RangeSession.isFullyComplete(): Boolean =
    completedSteps.size == snapshot.steps.size

fun RangeSession.isActive(): Boolean =
    completedAt == null && abandonedAt == null
