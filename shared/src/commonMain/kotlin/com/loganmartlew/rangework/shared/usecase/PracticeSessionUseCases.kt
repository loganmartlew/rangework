package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.validated
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository

class ListPracticeSessionsUseCase(
    private val practiceSessionRepository: PracticeSessionRepository,
) {
    suspend operator fun invoke(): List<PracticeSession> = practiceSessionRepository.listPracticeSessions()
}

class GetPracticeSessionUseCase(
    private val practiceSessionRepository: PracticeSessionRepository,
) {
    suspend operator fun invoke(sessionId: String): PracticeSession? =
        practiceSessionRepository.getPracticeSession(sessionId)
}

class SavePracticeSessionUseCase(
    private val practiceSessionRepository: PracticeSessionRepository,
) {
    suspend operator fun invoke(
        draft: PracticeSessionDraft,
        sessionId: String? = null,
    ): PracticeSession = practiceSessionRepository.savePracticeSession(
        draft = draft.validated(),
        sessionId = sessionId?.trim()?.takeIf(String::isNotEmpty),
    )
}

class DeletePracticeSessionUseCase(
    private val practiceSessionRepository: PracticeSessionRepository,
) {
    suspend operator fun invoke(sessionId: String) {
        practiceSessionRepository.deletePracticeSession(sessionId.trim())
    }
}
