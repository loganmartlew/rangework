package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.ClubGlyphShape
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.ShapeFlight
import com.loganmartlew.rangework.shared.model.StrikeLocation
import com.loganmartlew.rangework.shared.model.StrikeRow
import com.loganmartlew.rangework.shared.model.TypeTally
import com.loganmartlew.rangework.shared.model.shapeDisplayColumns
import com.loganmartlew.rangework.shared.model.shapeDisplayRows
import com.loganmartlew.rangework.shared.model.strikeDisplayColumns

/**
 * The 3×3 grid picker for Strike Location and Shape (design "grid types"). One tap
 * in, one tap out: a cell tap stages/sets the value and dismisses; re-tapping the
 * staged cell clears it; a scrim tap dismisses without changing anything.
 *
 * Cells are the live magnitude heatmap (P4: continuous single-hue primary alpha,
 * never a judgement ramp) with the count in the corner and a value glyph in the
 * body. Axis order and labels come from the shipped handedness transforms: strike
 * columns mirror for a left-hander (rows never flip); shape keeps golfer-term
 * labels in constant screen positions with the physical value per cell.
 */
@Composable
internal fun ObservationGridDialog(
    type: ObservationType,
    handedness: Handedness,
    tally: TypeTally,
    completedBalls: Int,
    currentValue: String?,
    editingBallNumber: Int?,
    clubGlyphShape: ClubGlyphShape = ClubGlyphShape.IRON,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val spec = gridSpec(type, handedness, clubGlyphShape)
    val maxCount = (tally.valueCounts.values.maxOrNull() ?: 0).coerceAtLeast(1)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = type.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${tally.observedCount}/$completedBalls",
                        style = RangeworkMono.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Header row: empty corner + three column headers.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.width(28.dp))
                    for (head in spec.columnHeaders) {
                        Text(
                            text = head,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                for (r in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = spec.rowHeaders[r],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(28.dp),
                        )
                        for (c in 0..2) {
                            val value = spec.valueAt(r, c)
                            val count = tally.valueCounts[value] ?: 0
                            GridCell(
                                modifier = Modifier.weight(1f),
                                count = count,
                                alpha = if (count == 0) 0f else 0.12f + 0.45f * (count / maxCount.toFloat()),
                                staged = currentValue == value,
                                glyph = { spec.cellGlyph(this, r, c) },
                                onClick = { onPick(value) },
                            )
                        }
                    }
                }

                Text(
                    text = editingBallNumber?.let { "Editing Ball $it — tap to set, re-tap to clear" }
                        ?: "Tap to stage · re-tap to clear",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GridCell(
    count: Int,
    alpha: Float,
    staged: Boolean,
    glyph: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(64.dp)
            .background(colors.primary.copy(alpha = alpha), RoundedCornerShape(10.dp))
            .then(
                if (staged) {
                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (staged) colors.primary else colors.onSurfaceVariant,
        ) {
            glyph()
        }
        if (count > 0) {
            Text(
                text = count.toString(),
                style = RangeworkMono.small,
                color = colors.onSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
        }
    }
}

// ── Grid spec (axis order, labels, per-cell value + glyph) ──────────────────────

private class GridSpec(
    val columnHeaders: List<String>,
    val rowHeaders: List<String>,
    val valueAt: (row: Int, col: Int) -> String,
    val cellGlyph: @Composable androidx.compose.foundation.layout.BoxScope.(row: Int, col: Int) -> Unit,
)

@Composable
private fun gridSpec(type: ObservationType, handedness: Handedness, clubGlyphShape: ClubGlyphShape): GridSpec =
    if (type == ObservationType.STRIKE_LOCATION) {
        val columns = strikeDisplayColumns(handedness)
        val rows = StrikeRow.entries.toList()
        GridSpec(
            columnHeaders = columns.map { it.label },
            rowHeaders = rows.map { it.label },
            valueAt = { r, c -> StrikeLocation(rows[r], columns[c]).id },
            cellGlyph = { r, c ->
                ClubfaceGlyph(StrikeLocation(rows[r], columns[c]), clubGlyphShape, handedness, height = 40.dp)
            },
        )
    } else {
        val startRows = shapeDisplayRows(handedness)
        val curveColumns = shapeDisplayColumns(handedness)
        GridSpec(
            columnHeaders = listOf("Draw", "Straight", "Fade"),
            rowHeaders = listOf("Pull", "Straight", "Push"),
            valueAt = { r, c -> ShapeFlight(startRows[r], curveColumns[c]).id },
            cellGlyph = { r, c ->
                FlightGlyph(ShapeFlight(startRows[r], curveColumns[c]), height = 44.dp)
            },
        )
    }
