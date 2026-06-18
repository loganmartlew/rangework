package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.validated
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository

class GetMeasurementPreferencesUseCase(
    private val measurementPreferencesRepository: MeasurementPreferencesRepository,
) {
    suspend operator fun invoke(): MeasurementPreferences =
        measurementPreferencesRepository.getMeasurementPreferences()
}

class SaveMeasurementPreferencesUseCase(
    private val measurementPreferencesRepository: MeasurementPreferencesRepository,
) {
    suspend operator fun invoke(
        preferences: MeasurementPreferences,
    ): MeasurementPreferences = measurementPreferencesRepository.saveMeasurementPreferences(
        preferences.validated(),
    )
}
