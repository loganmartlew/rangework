package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedStep
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.completedBalls
import com.loganmartlew.rangework.shared.model.totalBalls
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val RANGE_SESSIONS_TABLE = "range_sessions"
private const val RANGE_SESSION_TIME_ENTRIES_TABLE = "range_session_time_entries"
private const val RANGE_SESSION_OBSERVATIONS_TABLE = "range_session_observations"

class SupabaseRangeSessionRepository(
    private val client: SupabaseClient,
) : RangeSessionRepository {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun start(sessionId: String): RangeSession {
        val rangeSessionId = Uuid.random().toString()
        val params = StartRangeSessionParams(
            rangeSessionId = rangeSessionId,
            sessionId = sessionId,
        )
        client.postgrest.rpc(
            "start_range_session",
            Json.encodeToJsonElement(StartRangeSessionParams.serializer(), params).jsonObject,
        )
        return requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId could not be loaded after start."
        }
    }

    override suspend fun getSession(rangeSessionId: String): RangeSession? =
        client.postgrest[RANGE_SESSIONS_TABLE]
            .select {
                filter {
                    eq("id", rangeSessionId)
                }
            }
            .decodeList<RangeSession>()
            .firstOrNull()

    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> =
        client.postgrest[RANGE_SESSIONS_TABLE]
            .select {
                filter {
                    exact("completed_at", null)
                    exact("abandoned_at", null)
                }
            }
            .decodeList<RangeSession>()
            .map { session ->
                ActiveRangeSessionSummary(
                    id = session.id,
                    sessionName = session.sessionName,
                    totalSteps = session.snapshot.steps.size,
                    completedStepCount = session.completedSteps.size,
                    startedAt = session.startedAt,
                )
            }
            .sortedByDescending { it.startedAt }

    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> {
        val sessions = client.postgrest[RANGE_SESSIONS_TABLE]
            .select {
                filter {
                    eq("source_session_id", sessionId)
                    filterNot("completed_at", FilterOperator.IS, null)
                    exact("abandoned_at", null)
                }
            }
            .decodeList<RangeSession>()

        if (sessions.isEmpty()) return emptyList()

        val sessionIds = sessions.map { it.id }
        val timeEntriesBySession = client.postgrest[RANGE_SESSION_TIME_ENTRIES_TABLE]
            .select {
                filter {
                    isIn("range_session_id", sessionIds)
                }
            }
            .decodeList<TimeEntryRow>()
            .groupBy { it.rangeSessionId }

        return sessions.map { session ->
            val elapsed = timeEntriesBySession[session.id].orEmpty()
                .filter { it.exitedAt != null }
                .sumOf { it.exitedAt!!.epochSeconds - it.enteredAt.epochSeconds }

            CompletedRangeSessionSummary(
                id = session.id,
                sessionName = session.sessionName,
                totalSteps = session.snapshot.steps.size,
                completedStepCount = session.completedSteps.size,
                totalBalls = session.totalBalls(),
                completedBalls = session.completedBalls(),
                startedAt = session.startedAt,
                completedAt = session.completedAt!!,
                elapsedSeconds = elapsed,
            )
        }.sortedByDescending { it.completedAt }
    }

    override suspend fun setStepsCompletion(
        rangeSessionId: String,
        stepIndices: List<Int>,
        completed: Boolean,
    ): RangeSession {
        val session = requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId not found."
        }
        val updatedSteps = if (completed) {
            val alreadyCompleted = session.completedSteps.map { it.stepIndex }.toSet()
            // One shared timestamp for the whole batch.
            val completedAt = Clock.System.now()
            session.completedSteps + stepIndices
                .filter { it !in alreadyCompleted }
                .map { CompletedStep(stepIndex = it, completedAt = completedAt) }
        } else {
            val toRemove = stepIndices.toSet()
            session.completedSteps.filter { it.stepIndex !in toRemove }
        }
        client.postgrest[RANGE_SESSIONS_TABLE].update(
            CompletedStepsUpdate(completedSteps = updatedSteps),
        ) {
            filter { eq("id", rangeSessionId) }
        }
        return requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId could not be loaded after step completion update."
        }
    }

    override suspend fun overrideStepClubs(
        rangeSessionId: String,
        stepIndices: List<Int>,
        clubCode: String,
    ): RangeSession {
        val session = requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId not found."
        }
        val updatedOverrides = session.clubOverrides +
            stepIndices.map { it.toString() to clubCode }
        client.postgrest[RANGE_SESSIONS_TABLE].update(
            ClubOverridesUpdate(clubOverrides = updatedOverrides),
        ) {
            filter { eq("id", rangeSessionId) }
        }
        return requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId could not be loaded after club override."
        }
    }

    override suspend fun finishSession(rangeSessionId: String): RangeSession {
        client.postgrest[RANGE_SESSIONS_TABLE].update(
            CompletedAtUpdate(completedAt = Clock.System.now()),
        ) {
            filter { eq("id", rangeSessionId) }
        }
        return requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId could not be loaded after finish."
        }
    }

    override suspend fun abandonSession(rangeSessionId: String) {
        client.postgrest[RANGE_SESSIONS_TABLE].update(
            AbandonedAtUpdate(abandonedAt = Clock.System.now()),
        ) {
            filter { eq("id", rangeSessionId) }
        }
    }

    override suspend fun saveSessionNote(rangeSessionId: String, note: String?): RangeSession {
        client.postgrest[RANGE_SESSIONS_TABLE].update(
            SessionNoteUpdate(sessionNote = note),
        ) {
            filter { eq("id", rangeSessionId) }
        }
        return requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId could not be loaded after session note update."
        }
    }

    override suspend fun saveBlockResult(
        rangeSessionId: String,
        unitIndex: Int,
        result: BlockResult,
    ): RangeSession {
        val session = requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId not found."
        }
        // Read-merge-write the whole map so sibling keys survive; an all-null
        // result removes just this key. Not atomic against a concurrent write to
        // a different block (last writer wins the whole column) — accepted as a
        // single-user app, mirroring the club_overrides precedent above.
        val key = unitIndex.toString()
        val updatedResults = if (result.isEmpty) {
            session.blockResults - key
        } else {
            session.blockResults + (key to result)
        }
        client.postgrest[RANGE_SESSIONS_TABLE].update(
            BlockResultsUpdate(blockResults = updatedResults),
        ) {
            filter { eq("id", rangeSessionId) }
        }
        return requireNotNull(getSession(rangeSessionId)) {
            "Range session $rangeSessionId could not be loaded after block result update."
        }
    }

    override suspend fun listObservations(rangeSessionId: String): List<Observation> =
        client.postgrest[RANGE_SESSION_OBSERVATIONS_TABLE]
            .select {
                filter { eq("range_session_id", rangeSessionId) }
            }
            .decodeList<ObservationRow>()
            .map { it.toModel() }
            .sortedBy(Observation::stepIndex)

    override suspend fun upsertObservation(
        rangeSessionId: String,
        stepIndex: Int,
        values: Map<String, String>,
    ): Observation {
        client.postgrest[RANGE_SESSION_OBSERVATIONS_TABLE].upsert(
            ObservationUpsertRow(
                rangeSessionId = rangeSessionId,
                stepIndex = stepIndex,
                observedValues = values,
            ),
        ) {
            onConflict = "range_session_id,step_index"
        }
        return Observation(stepIndex = stepIndex, values = values)
    }

    override suspend fun deleteObservations(rangeSessionId: String, stepIndices: List<Int>) {
        if (stepIndices.isEmpty()) return
        client.postgrest[RANGE_SESSION_OBSERVATIONS_TABLE].delete {
            filter {
                eq("range_session_id", rangeSessionId)
                isIn("step_index", stepIndices)
            }
        }
    }

    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) {
        client.postgrest[RANGE_SESSION_TIME_ENTRIES_TABLE].insert(
            TimeEntryInsertRow(
                rangeSessionId = rangeSessionId,
                enteredAt = enteredAt,
            ),
        )
    }

    override suspend fun closeTimeEntry(
        rangeSessionId: String,
        enteredAt: Instant,
        exitedAt: Instant,
    ) {
        client.postgrest[RANGE_SESSION_TIME_ENTRIES_TABLE].update(
            TimeEntryExitUpdate(exitedAt = exitedAt),
        ) {
            filter {
                eq("range_session_id", rangeSessionId)
                eq("entered_at", enteredAt.toString())
            }
        }
    }

    override suspend fun getElapsedSeconds(rangeSessionId: String): Long {
        val entries = client.postgrest[RANGE_SESSION_TIME_ENTRIES_TABLE]
            .select {
                filter { eq("range_session_id", rangeSessionId) }
            }
            .decodeList<TimeEntryRow>()
        return entries
            .filter { it.exitedAt != null }
            .sumOf { it.exitedAt!!.epochSeconds - it.enteredAt.epochSeconds }
    }

    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean =
        client.postgrest[RANGE_SESSIONS_TABLE]
            .select {
                filter {
                    eq("source_session_id", sessionId)
                    exact("completed_at", null)
                    exact("abandoned_at", null)
                }
            }
            .decodeList<RangeSessionIdRow>()
            .isNotEmpty()
}

// ── RPC params ────────────────────────────────────────────────────────────────

@Serializable
private data class StartRangeSessionParams(
    @SerialName("p_range_session_id") val rangeSessionId: String,
    @SerialName("p_session_id") val sessionId: String,
)

// ── Partial update DTOs ───────────────────────────────────────────────────────

@Serializable
private data class CompletedStepsUpdate(
    @SerialName("completed_steps")
    val completedSteps: List<CompletedStep>,
)

@Serializable
private data class ClubOverridesUpdate(
    @SerialName("club_overrides")
    val clubOverrides: Map<String, String>,
)

@Serializable
private data class CompletedAtUpdate(
    @SerialName("completed_at")
    val completedAt: Instant,
)

@Serializable
private data class AbandonedAtUpdate(
    @SerialName("abandoned_at")
    val abandonedAt: Instant,
)

@Serializable
private data class SessionNoteUpdate(
    @SerialName("session_note")
    val sessionNote: String?,
)

@Serializable
private data class BlockResultsUpdate(
    @SerialName("block_results")
    val blockResults: Map<String, BlockResult>,
)

// ── Observation DTOs ──────────────────────────────────────────────────────────

@Serializable
private data class ObservationRow(
    @SerialName("step_index")
    val stepIndex: Int,
    @SerialName("observed_values")
    val observedValues: Map<String, String> = emptyMap(),
) {
    fun toModel(): Observation = Observation(stepIndex = stepIndex, values = observedValues)
}

@Serializable
private data class ObservationUpsertRow(
    @SerialName("range_session_id")
    val rangeSessionId: String,
    @SerialName("step_index")
    val stepIndex: Int,
    @SerialName("observed_values")
    val observedValues: Map<String, String>,
)

// ── Time entry DTOs ───────────────────────────────────────────────────────────

@Serializable
private data class TimeEntryInsertRow(
    @SerialName("range_session_id")
    val rangeSessionId: String,
    @SerialName("entered_at")
    val enteredAt: Instant,
)

@Serializable
private data class TimeEntryExitUpdate(
    @SerialName("exited_at")
    val exitedAt: Instant,
)

@Serializable
private data class TimeEntryRow(
    val id: String,
    @SerialName("range_session_id")
    val rangeSessionId: String,
    @SerialName("entered_at")
    val enteredAt: Instant,
    @SerialName("exited_at")
    val exitedAt: Instant? = null,
)

// ── Minimal select DTO ────────────────────────────────────────────────────────

@Serializable
private data class RangeSessionIdRow(val id: String)
