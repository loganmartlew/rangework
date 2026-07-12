package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.shared.model.ClubGlyphShape
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.TypeTally

/**
 * The per-block read-only observation summary card for history (Stage 6, P1):
 * the same tally vocabulary as the block screen's capture stack — chip rows
 * for the single-value scales, inline heatmap grids for Strike Location and
 * Shape — rendered with no staging, no arming, and no tap targets. "Observations"
 * is redundant as a card title (the rows are self-labeling), so the card has none.
 *
 * Pure presentation: [tallies] and [completedBalls] are the block's observed
 * counts and denominator baseline (completed Ball Steps, not planned balls).
 */
@Composable
internal fun ObservationSummarySection(
    enabledTypes: List<ObservationType>,
    tallies: Map<ObservationType, TypeTally>,
    completedBalls: Int,
    successCriterion: String?,
    handedness: Handedness,
    clubGlyphShape: ClubGlyphShape = ClubGlyphShape.IRON,
    modifier: Modifier = Modifier,
) {
    val ordered = orderedCaptureTypes(enabledTypes)
    if (ordered.isEmpty()) return

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (type in ordered) {
                val tally = tallies[type] ?: TypeTally(observedCount = 0, valueCounts = emptyMap())
                val denom = "${tally.observedCount}/$completedBalls"
                if (type.isChipType) {
                    ObservationChipRow(
                        type = type,
                        selectedValue = null,
                        tally = tally,
                        denominatorText = denom,
                        successCriterion = successCriterion,
                        arming = false,
                        enabled = false,
                        onSelect = {},
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ObservationRowHeader(
                            label = type.displayLabel,
                            rubric = null,
                            denominatorText = denom,
                        )
                        ObservationGridContent(
                            type = type,
                            handedness = handedness,
                            tally = tally,
                            clubGlyphShape = clubGlyphShape,
                        )
                    }
                }
            }
        }
    }
}
