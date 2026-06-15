package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PRACTICE_UNITS_TABLE = "practice_units"
private const val PRACTICE_UNIT_INSTRUCTIONS_TABLE = "practice_unit_instructions"

class SupabasePracticeUnitRepository(
    private val client: SupabaseClient,
) : PracticeUnitRepository {
    override suspend fun listPracticeUnits(): List<PracticeUnit> {
        val unitRows = client.postgrest[PRACTICE_UNITS_TABLE]
            .select()
            .decodeList<PracticeUnitRow>()

        if (unitRows.isEmpty()) {
            return emptyList()
        }

        val instructionRows = client.postgrest[PRACTICE_UNIT_INSTRUCTIONS_TABLE]
            .select()
            .decodeList<PracticeUnitInstructionRow>()
            .groupBy(PracticeUnitInstructionRow::practiceUnitId)

        return unitRows
            .map { row ->
                row.toModel(
                    instructions = instructionRows[row.id].orEmpty(),
                )
            }
            .sortedByDescending(PracticeUnit::updatedAt)
    }

    override suspend fun getPracticeUnit(unitId: String): PracticeUnit? {
        val unitRow = client.postgrest[PRACTICE_UNITS_TABLE]
            .select {
                filter {
                    eq("id", unitId)
                }
            }
            .decodeList<PracticeUnitRow>()
            .firstOrNull()
            ?: return null

        val instructionRows = client.postgrest[PRACTICE_UNIT_INSTRUCTIONS_TABLE]
            .select {
                filter {
                    eq("practice_unit_id", unitId)
                }
            }
            .decodeList<PracticeUnitInstructionRow>()

        return unitRow.toModel(instructionRows)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun savePracticeUnit(
        draft: PracticeUnitDraft,
        unitId: String?,
    ): PracticeUnit {
        val resolvedUnitId = unitId ?: Uuid.random().toString()

        if (unitId == null) {
            client.postgrest[PRACTICE_UNITS_TABLE].insert(
                PracticeUnitInsertRow(
                    id = resolvedUnitId,
                    title = draft.title,
                    notes = draft.notes,
                    focus = draft.focus,
                    defaultClubReference = draft.defaultClubReference,
                ),
            )
        } else {
            client.postgrest[PRACTICE_UNITS_TABLE].update(
                PracticeUnitUpdateRow(
                    title = draft.title,
                    notes = draft.notes,
                    focus = draft.focus,
                    defaultClubReference = draft.defaultClubReference,
                ),
            ) {
                filter {
                    eq("id", resolvedUnitId)
                }
            }
        }

        replaceInstructions(
            practiceUnitId = resolvedUnitId,
            instructions = draft.instructions,
        )

        return requireNotNull(getPracticeUnit(resolvedUnitId)) {
            "Practice unit $resolvedUnitId could not be loaded after save."
        }
    }

    override suspend fun deletePracticeUnit(unitId: String) {
        val existing = getPracticeUnit(unitId)
            ?: throw NoSuchElementException("Practice unit $unitId does not exist.")

        client.postgrest[PRACTICE_UNITS_TABLE].delete {
            filter {
                eq("id", existing.id)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun replaceInstructions(
        practiceUnitId: String,
        instructions: List<PracticeInstructionDraft>,
    ) {
        client.postgrest[PRACTICE_UNIT_INSTRUCTIONS_TABLE].delete {
            filter {
                eq("practice_unit_id", practiceUnitId)
            }
        }

        if (instructions.isEmpty()) {
            return
        }

        client.postgrest[PRACTICE_UNIT_INSTRUCTIONS_TABLE].insert(
            instructions.map { instruction ->
                PracticeUnitInstructionInsertRow(
                    id = Uuid.random().toString(),
                    practiceUnitId = practiceUnitId,
                    sortOrder = instruction.order,
                    text = instruction.text,
                    repCount = instruction.repCount,
                    ballCount = instruction.ballCount,
                )
            },
        )
    }
}

@Serializable
private data class PracticeUnitRow(
    val id: String,
    val title: String,
    val notes: String? = null,
    val focus: String? = null,
    @SerialName("default_club_reference")
    val defaultClubReference: String? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
)

@Serializable
private data class PracticeUnitInsertRow(
    val id: String,
    val title: String,
    val notes: String? = null,
    val focus: String? = null,
    @SerialName("default_club_reference")
    val defaultClubReference: String? = null,
)

@Serializable
private data class PracticeUnitUpdateRow(
    val title: String,
    val notes: String? = null,
    val focus: String? = null,
    @SerialName("default_club_reference")
    val defaultClubReference: String? = null,
)

@Serializable
private data class PracticeUnitInstructionRow(
    val id: String,
    @SerialName("practice_unit_id")
    val practiceUnitId: String,
    @SerialName("sort_order")
    val sortOrder: Int,
    val text: String,
    @SerialName("rep_count")
    val repCount: Int? = null,
    @SerialName("ball_count")
    val ballCount: Int? = null,
)

@Serializable
private data class PracticeUnitInstructionInsertRow(
    val id: String,
    @SerialName("practice_unit_id")
    val practiceUnitId: String,
    @SerialName("sort_order")
    val sortOrder: Int,
    val text: String,
    @SerialName("rep_count")
    val repCount: Int? = null,
    @SerialName("ball_count")
    val ballCount: Int? = null,
)

private fun PracticeUnitRow.toModel(
    instructions: List<PracticeUnitInstructionRow>,
): PracticeUnit = PracticeUnit(
    id = id,
    title = title,
    instructions = instructions
        .sortedBy(PracticeUnitInstructionRow::sortOrder)
        .map { row ->
            PracticeInstruction(
                id = row.id,
                order = row.sortOrder,
                text = row.text,
                repCount = row.repCount,
                ballCount = row.ballCount,
            )
        },
    notes = notes,
    focus = focus,
    defaultClubReference = defaultClubReference,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
