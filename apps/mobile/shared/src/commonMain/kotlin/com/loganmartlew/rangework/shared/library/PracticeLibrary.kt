package com.loganmartlew.rangework.shared.library

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.ValidationIssue

interface PracticeLibrary {
    // ── Practice Units ────────────────────────────────────────────────
    suspend fun listUnits(): List<PracticeUnit>
    suspend fun getUnit(id: String): PracticeUnit?
    fun validateUnit(draft: PracticeUnitDraft): List<ValidationIssue>
    suspend fun saveUnit(draft: PracticeUnitDraft, unitId: String? = null): PracticeLibraryResult<PracticeUnit>
    suspend fun duplicateUnit(id: String): PracticeUnit
    suspend fun restoreUnit(unit: PracticeUnit): PracticeUnit
    suspend fun deleteUnit(id: String)

    // ── Practice Sessions ─────────────────────────────────────────────
    suspend fun listSessions(): List<PracticeSession>
    suspend fun getSession(id: String): PracticeSession?
    suspend fun validateSession(draft: PracticeSessionDraft): List<ValidationIssue>
    suspend fun saveSession(draft: PracticeSessionDraft, sessionId: String? = null): PracticeLibraryResult<PracticeSession>
    suspend fun duplicateSession(id: String): PracticeSession
    suspend fun restoreSession(session: PracticeSession): PracticeSession
    suspend fun deleteSession(id: String)
}