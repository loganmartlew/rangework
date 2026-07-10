package com.loganmartlew.rangework.shared.recording

import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.normalizedOptionalText
import com.loganmartlew.rangework.shared.model.validateBlockNoteEdit
import com.loganmartlew.rangework.shared.model.validateManualCountEdit
import com.loganmartlew.rangework.shared.model.validateObservationEdit
import com.loganmartlew.rangework.shared.model.validateObservationWrite
import com.loganmartlew.rangework.shared.model.validateSessionNoteEdit
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository

/**
 * Loads the session, applies [com.loganmartlew.rangework.shared.model.RangeSessionRecordingRules],
 * and delegates accepted writes to the repository. All mutability and
 * count-provenance enforcement lives in the rules; this class is the wiring.
 */
class DefaultRangeSessionRecorder(
    private val repository: RangeSessionRepository,
) : RangeSessionRecorder {

    override suspend fun saveSessionNote(
        rangeSessionId: String,
        note: String?,
    ): RecordingResult<RangeSession> {
        val session = load(rangeSessionId)
        session.validateSessionNoteEdit()?.let { return RecordingResult.Rejected(it) }
        return RecordingResult.Success(repository.saveSessionNote(rangeSessionId, note.normalizedOptionalText()))
    }

    override suspend fun saveBlockNote(
        rangeSessionId: String,
        unitIndex: Int,
        note: String?,
    ): RecordingResult<RangeSession> {
        val session = load(rangeSessionId)
        session.validateBlockNoteEdit(unitIndex)?.let { return RecordingResult.Rejected(it) }
        val merged = session.blockResultAt(unitIndex).copy(note = note.normalizedOptionalText())
        return RecordingResult.Success(repository.saveBlockResult(rangeSessionId, unitIndex, merged))
    }

    override suspend fun saveManualCount(
        rangeSessionId: String,
        unitIndex: Int,
        count: Int?,
    ): RecordingResult<RangeSession> {
        val session = load(rangeSessionId)
        session.validateManualCountEdit(unitIndex, count)?.let { return RecordingResult.Rejected(it) }
        val merged = session.blockResultAt(unitIndex).copy(manualCount = count)
        return RecordingResult.Success(repository.saveBlockResult(rangeSessionId, unitIndex, merged))
    }

    override suspend fun recordObservation(
        rangeSessionId: String,
        stepIndex: Int,
        values: Map<String, String>,
    ): RecordingResult<Observation> {
        val session = load(rangeSessionId)
        session.validateObservationWrite(stepIndex, values)?.let { return RecordingResult.Rejected(it) }
        return RecordingResult.Success(repository.upsertObservation(rangeSessionId, stepIndex, values))
    }

    override suspend fun voidObservations(
        rangeSessionId: String,
        stepIndices: List<Int>,
    ): RecordingResult<Unit> {
        val session = load(rangeSessionId)
        session.validateObservationEdit()?.let { return RecordingResult.Rejected(it) }
        repository.deleteObservations(rangeSessionId, stepIndices)
        return RecordingResult.Success(Unit)
    }

    override suspend fun uncompleteStepsVoidingObservations(
        rangeSessionId: String,
        stepIndices: List<Int>,
    ): RecordingResult<RangeSession> {
        val session = load(rangeSessionId)
        session.validateObservationEdit()?.let { return RecordingResult.Rejected(it) }
        // Delete observations first: a failure here aborts before un-completing,
        // and a failure of the un-complete afterwards leaves a legal
        // completed-but-unobserved state rather than a half-observed ghost.
        repository.deleteObservations(rangeSessionId, stepIndices)
        return RecordingResult.Success(
            repository.setStepsCompletion(rangeSessionId, stepIndices, completed = false),
        )
    }

    override suspend fun observations(rangeSessionId: String): List<Observation> =
        repository.listObservations(rangeSessionId)

    private suspend fun load(rangeSessionId: String): RangeSession =
        requireNotNull(repository.getSession(rangeSessionId)) {
            "Range session $rangeSessionId not found."
        }
}

private fun RangeSession.blockResultAt(unitIndex: Int): BlockResult =
    blockResults[unitIndex.toString()] ?: BlockResult()
