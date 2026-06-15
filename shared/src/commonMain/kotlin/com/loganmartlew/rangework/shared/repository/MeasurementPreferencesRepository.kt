package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.MeasurementPreferences

interface MeasurementPreferencesRepository {
    suspend fun getMeasurementPreferences(): MeasurementPreferences

    suspend fun saveMeasurementPreferences(
        preferences: MeasurementPreferences,
    ): MeasurementPreferences
}
