package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartRangeSessionUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(sessionId: String): RangeSession =
        rangeSessionRepository.startSession(
            rangeSessionId = Uuid.random().toString(),
            sessionId = sessionId,
        )
}

class GetRangeSessionUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(rangeSessionId: String): RangeSession? =
        rangeSessionRepository.getSession(rangeSessionId)
}

class ListActiveRangeSessionsUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(): List<ActiveRangeSessionSummary> =
        rangeSessionRepository.listActiveSessions()
}

class ListCompletedRangeSessionsUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(sessionId: String): List<CompletedRangeSessionSummary> =
        rangeSessionRepository.listCompletedSessions(sessionId)
}

class ToggleStepCompleteUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(
        rangeSessionId: String,
        stepIndex: Int,
        completed: Boolean,
    ): RangeSession = rangeSessionRepository.toggleStepComplete(rangeSessionId, stepIndex, completed)
}

class OverrideStepClubUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(
        rangeSessionId: String,
        stepIndex: Int,
        clubCode: String,
    ): RangeSession = rangeSessionRepository.overrideStepClub(rangeSessionId, stepIndex, clubCode)
}

class UpdateLastViewedStepUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(rangeSessionId: String, stepIndex: Int) {
        rangeSessionRepository.updateLastViewedStep(rangeSessionId, stepIndex)
    }
}

class FinishRangeSessionUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(rangeSessionId: String): RangeSession =
        rangeSessionRepository.finishSession(rangeSessionId)
}

class AbandonRangeSessionUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(rangeSessionId: String) {
        rangeSessionRepository.abandonSession(rangeSessionId)
    }
}

class RecordTimeEntryUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(rangeSessionId: String, enteredAt: Instant) {
        rangeSessionRepository.recordTimeEntry(rangeSessionId, enteredAt)
    }
}

class CloseTimeEntryUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(
        rangeSessionId: String,
        enteredAt: Instant,
        exitedAt: Instant,
    ) {
        rangeSessionRepository.closeTimeEntry(rangeSessionId, enteredAt, exitedAt)
    }
}

class GetElapsedSecondsUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(rangeSessionId: String): Long =
        rangeSessionRepository.getElapsedSeconds(rangeSessionId)
}

class HasActiveRangeSessionsUseCase(
    private val rangeSessionRepository: RangeSessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Boolean =
        rangeSessionRepository.hasActiveSessionsForTemplate(sessionId)
}
