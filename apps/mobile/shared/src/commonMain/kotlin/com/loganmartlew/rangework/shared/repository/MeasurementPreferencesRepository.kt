package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.validated

abstract class MeasurementPreferencesRepository {
    suspend fun save(preferences: MeasurementPreferences): MeasurementPreferences =
        persist(preferences.validated())

    protected abstract suspend fun persist(validated: MeasurementPreferences): MeasurementPreferences

    abstract suspend fun get(): MeasurementPreferences
}
