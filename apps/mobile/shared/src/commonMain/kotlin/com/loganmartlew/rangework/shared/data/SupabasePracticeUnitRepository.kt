package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PRACTICE_UNITS_TABLE = "practice_units"
private const val PRACTICE_UNIT_INSTRUCTIONS_TABLE = "practice_unit_instructions"

class SupabasePracticeUnitRepository(
    private val client: SupabaseClient,
) : PracticeUnitRepository() {
    override suspend fun list(): List<PracticeUnit> {
        val unitRows = client.postgrest[PRACTICE_UNITS_TABLE]
            .select {
                filter {
                    // Inline Units are session-owned content and must never
                    // surface in the library. `get` stays unfiltered so a
                    // session can still load its inline units by id.
                    exact("scoped_to_session_id", null)
                }
            }
            .decodeList<PracticeUnitRow>()

        if (unitRows.isEmpty()) {
            return emptyList()
        }

        val instructionRows = client.postgrest[PRACTICE_UNIT_INSTRUCTIONS_TABLE]
            .select()
            .decodeList<PracticeUnitInstructionRow>()

        val tagsById = client.loadVisibleTagsById()
        val tagsByUnit = client.postgrest[PRACTICE_UNIT_TAGS_TABLE]
            .select()
            .decodeList<PracticeUnitTagRow>()
            .groupBy(PracticeUnitTagRow::practiceUnitId)

        return assembleParentsWithChildren(
            parents = unitRows,
            children = instructionRows,
            parentId = PracticeUnitRow::id,
            childParentId = PracticeUnitInstructionRow::practiceUnitId,
            childOrder = PracticeUnitInstructionRow::sortOrder,
            toModel = { row, instructions ->
                row.toModel(
                    instructions = instructions,
                    tags = resolveTags(
                        tagsByUnit[row.id].orEmpty().map(PracticeUnitTagRow::tagId),
                        tagsById,
                    ),
                )
            },
            modelSort = PracticeUnit::updatedAt,
        )
    }

    override suspend fun get(id: String): PracticeUnit? {
        val unitRow = client.postgrest[PRACTICE_UNITS_TABLE]
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeList<PracticeUnitRow>()
            .firstOrNull()
            ?: return null

        val instructionRows = client.postgrest[PRACTICE_UNIT_INSTRUCTIONS_TABLE]
            .select {
                filter {
                    eq("practice_unit_id", id)
                }
            }
            .decodeList<PracticeUnitInstructionRow>()

        val tagIds = client.postgrest[PRACTICE_UNIT_TAGS_TABLE]
            .select {
                filter {
                    eq("practice_unit_id", id)
                }
            }
            .decodeList<PracticeUnitTagRow>()
            .map(PracticeUnitTagRow::tagId)
        val tags = resolveTags(tagIds, client.loadVisibleTagsById())

        return assembleParentWithChildren(
            parent = unitRow,
            children = instructionRows,
            childOrder = PracticeUnitInstructionRow::sortOrder,
            toModel = { row, instructions -> row.toModel(instructions, tags) },
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit {
        val resolvedUnitId = unitId ?: Uuid.random().toString()
        val params = SavePracticeUnitParams(
            unitId = resolvedUnitId,
            title = validated.title,
            notes = validated.notes,
            focus = validated.focus,
            defaultClubCode = validated.defaultClubCode,
            instructions = validated.instructions.map { instruction ->
                InstructionParam(
                    order = instruction.order,
                    text = instruction.text,
                    ballCount = instruction.ballCount,
                    clubCode = instruction.clubCode,
                )
            },
            tagIds = validated.tagIds,
            successCriterion = validated.successCriterion,
        )
        client.postgrest.rpc(
            "save_practice_unit",
            Json.encodeToJsonElement(SavePracticeUnitParams.serializer(), params).jsonObject,
        )
        return requireNotNull(get(resolvedUnitId)) {
            "Practice unit $resolvedUnitId could not be loaded after save."
        }
    }

    override suspend fun setScopedSession(id: String, sessionId: String?): PracticeUnit {
        client.postgrest[PRACTICE_UNITS_TABLE].update(
            ScopedSessionUpdate(scopedToSessionId = sessionId),
        ) {
            filter {
                eq("id", id)
            }
        }
        return requireNotNull(get(id)) {
            "Practice unit $id could not be loaded after scope update."
        }
    }

    override suspend fun delete(id: String) {
        val existing = get(id)
            ?: throw NoSuchElementException("Practice unit $id does not exist.")

        client.postgrest[PRACTICE_UNITS_TABLE].delete {
            filter {
                eq("id", existing.id)
            }
        }
    }
}

@Serializable
private data class PracticeUnitRow(
    val id: String,
    val title: String,
    val notes: String? = null,
    val focus: String? = null,
    @SerialName("default_club_code")
    val defaultClubCode: String? = null,
    @SerialName("success_criterion")
    val successCriterion: String? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("scoped_to_session_id")
    val scopedToSessionId: String? = null,
)

@Serializable
private data class ScopedSessionUpdate(
    @SerialName("scoped_to_session_id")
    val scopedToSessionId: String?,
)

@Serializable
private data class PracticeUnitInstructionRow(
    val id: String,
    @SerialName("practice_unit_id")
    val practiceUnitId: String,
    @SerialName("sort_order")
    val sortOrder: Int,
    val text: String,
    @SerialName("ball_count")
    val ballCount: Int? = null,
    @SerialName("club_code")
    val clubCode: String? = null,
)

@Serializable
private data class SavePracticeUnitParams(
    @SerialName("p_unit_id") val unitId: String,
    @SerialName("p_title") val title: String,
    @SerialName("p_notes") val notes: String?,
    @SerialName("p_focus") val focus: String?,
    @SerialName("p_default_club_code") val defaultClubCode: String?,
    @SerialName("p_instructions") val instructions: List<InstructionParam>,
    @SerialName("p_tag_ids") val tagIds: List<String>,
    @SerialName("p_success_criterion") val successCriterion: String?,
)

@Serializable
private data class InstructionParam(
    @SerialName("order") val order: Int,
    @SerialName("text") val text: String,
    @SerialName("ball_count") val ballCount: Int? = null,
    @SerialName("club_code") val clubCode: String? = null,
)

internal fun resolveTags(tagIds: List<String>, tagsById: Map<String, Tag>): List<Tag> =
    tagIds.mapNotNull(tagsById::get).sortedForDisplay()

private fun PracticeUnitRow.toModel(
    instructions: List<PracticeUnitInstructionRow>,
    tags: List<Tag>,
): PracticeUnit = PracticeUnit(
    id = id,
    title = title,
    instructions = instructions.map { row ->
        PracticeInstruction(
            id = row.id,
            order = row.sortOrder,
            text = row.text,
            ballCount = row.ballCount,
            clubCode = row.clubCode,
        )
    },
    notes = notes,
    focus = focus,
    defaultClubCode = defaultClubCode,
    successCriterion = successCriterion,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    scopedToSessionId = scopedToSessionId,
)