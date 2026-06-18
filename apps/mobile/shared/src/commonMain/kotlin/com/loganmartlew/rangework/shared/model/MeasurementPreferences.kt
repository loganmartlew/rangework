package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class UnitSystem {
    IMPERIAL,
    METRIC,
    CUSTOM,
}

@Serializable
enum class DistanceUnit {
    YARDS,
    METERS,
}

@Serializable
enum class SpeedUnit {
    MILES_PER_HOUR,
    KILOMETRES_PER_HOUR,
    METRES_PER_SECOND,
}

@Serializable
data class MeasurementPreferences(
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val distanceUnit: DistanceUnit = DistanceUnit.YARDS,
    val speedUnit: SpeedUnit = SpeedUnit.MILES_PER_HOUR,
) {
    companion object {
        val Imperial = MeasurementPreferences(
            unitSystem = UnitSystem.IMPERIAL,
            distanceUnit = DistanceUnit.YARDS,
            speedUnit = SpeedUnit.MILES_PER_HOUR,
        )

        val Metric = MeasurementPreferences(
            unitSystem = UnitSystem.METRIC,
            distanceUnit = DistanceUnit.METERS,
            speedUnit = SpeedUnit.KILOMETRES_PER_HOUR,
        )
    }
}
