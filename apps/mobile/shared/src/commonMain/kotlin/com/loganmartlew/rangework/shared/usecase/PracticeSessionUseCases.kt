package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
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

class DuplicatePracticeSessionUseCase(
    private val getPracticeSessionUseCase: GetPracticeSessionUseCase,
    private val savePracticeSessionUseCase: SavePracticeSessionUseCase,
) {
    suspend operator fun invoke(sessionId: String): PracticeSession {
        val original = getPracticeSessionUseCase(sessionId)
            ?: error("Session $sessionId not found")
        val draft = PracticeSessionDraft(
            name = original.name,
            notes = original.notes,
            items = original.items.map { item ->
                PracticeSessionItemDraft(
                    practiceUnitId = item.practiceUnitId,
                    order = item.order,
                    repeatCount = item.repeatCount,
                    clubCode = item.clubCode,
                    notes = item.notes,
                    focusCue = item.focusCue,
                )
            },
        )
        return savePracticeSessionUseCase(draft)
    }
}
