package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Deserialization regression for the Stage 1 wire shape. The JSON here mirrors a
 * `getSession` row (snake_case columns; camelCase inside the snapshot). Decoding
 * uses `ignoreUnknownKeys = true`, matching supabase-kt's default serializer, so
 * newer columns and unknown vocabulary degrade rather than crash.
 */
class SnapshotV3DecodingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun v3RowDecodesWithNoteResultsAndTypedUnits() {
        val row = """
            {
              "id": "rs-1",
              "session_name": "Wedge work",
              "snapshot_version": 3,
              "session_note": "solid session",
              "block_results": { "0": { "note": "left misses", "manualCount": 4 } },
              "completed_steps": [],
              "club_overrides": {},
              "started_at": "2026-07-09T10:00:00Z",
              "snapshot": {
                "units": [
                  {
                    "unitTitle": "Wedge",
                    "repeatCount": 1,
                    "instructions": [],
                    "successCriterion": "inside 5m",
                    "observationTypes": ["success", "shape", "rating"]
                  },
                  {
                    "unitTitle": "Driver",
                    "repeatCount": 1,
                    "instructions": [],
                    "observationTypes": ["success", "distance"]
                  }
                ],
                "steps": [
                  { "unitIndex": 0, "instructionIndex": 0, "repNumber": 1, "totalReps": 1, "instructionText": "Hit", "ballCount": 1, "unitTitle": "Wedge" }
                ]
              }
            }
        """.trimIndent()

        val session = json.decodeFromString(RangeSession.serializer(), row)

        assertTrue(session.supportsDataCapture)
        assertEquals("solid session", session.sessionNote)
        assertEquals(BlockResult(note = "left misses", manualCount = 4), session.blockResults["0"])

        val wedge = session.snapshot.units[0]
        assertEquals("inside 5m", wedge.successCriterion)
        // Unknown "rating" dropped; order preserved.
        assertEquals(listOf(ObservationType.SUCCESS, ObservationType.SHAPE), wedge.enabledObservationTypes)

        val driver = session.snapshot.units[1]
        assertNull(driver.successCriterion)
        // Success filtered out when no criterion — belt-and-braces beside the RPC filter.
        assertEquals(listOf(ObservationType.DISTANCE), driver.enabledObservationTypes)
    }

    @Test
    fun v2RowDecodesToDefaults() {
        val row = """
            {
              "id": "rs-2",
              "session_name": "Old session",
              "snapshot_version": 2,
              "completed_steps": [],
              "club_overrides": {},
              "started_at": "2026-07-09T10:00:00Z",
              "snapshot": {
                "units": [ { "unitTitle": "Unit", "repeatCount": 1, "instructions": [] } ],
                "steps": []
              }
            }
        """.trimIndent()

        val session = json.decodeFromString(RangeSession.serializer(), row)

        assertTrue(!session.supportsDataCapture)
        assertNull(session.sessionNote)
        assertTrue(session.blockResults.isEmpty())
        val unit = session.snapshot.units[0]
        assertNull(unit.successCriterion)
        assertTrue(unit.enabledObservationTypes.isEmpty())
    }
}
