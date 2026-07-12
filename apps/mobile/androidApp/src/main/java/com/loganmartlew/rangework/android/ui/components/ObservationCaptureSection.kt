package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.ContactValue
import com.loganmartlew.rangework.shared.model.DirectionValue
import com.loganmartlew.rangework.shared.model.DistanceValue
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.ShapeFlight
import com.loganmartlew.rangework.shared.model.StrikeColumn
import com.loganmartlew.rangework.shared.model.StrikeLocation
import com.loganmartlew.rangework.shared.model.StrikeRow
import com.loganmartlew.rangework.shared.model.SuccessValue
import com.loganmartlew.rangework.shared.model.TypeTally
import com.loganmartlew.rangework.shared.model.golferLabel

/**
 * The per-ball capture stack rendered *inside* the counter card, between the ball
 * readout and the −/+1 row (design §0). Chip rows for the single-value scales and
 * launcher rows for the two grid types, in the fixed row order (Success → Contact,
 * Distance, Direction → Strike, Shape). The tally surface is the input surface: live
 * counts sit on the chips, per-type `observed/completed` denominators on the headers.
 *
 * Pure presentation — all state (staging, tallies, arm flag) and callbacks are
 * passed in; nothing here reads or writes the recorder. When [readOnly] (block
 * complete), the whole stack dims to a read-only review surface.
 */
@Composable
internal fun ObservationCaptureSection(
    enabledTypes: List<ObservationType>,
    tallies: Map<ObservationType, TypeTally>,
    staging: Map<String, String>,
    completedBalls: Int,
    successCriterion: String?,
    arming: Boolean,
    handedness: Handedness,
    readOnly: Boolean,
    onStageChip: (typeId: String, value: String) -> Unit,
    onOpenGrid: (ObservationType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = orderedCaptureTypes(enabledTypes)
    if (ordered.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (readOnly) 0.5f else 1f),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (type in ordered) {
            val tally = tallies[type]
            val denom = tally?.let { "${it.observedCount}/$completedBalls" }
            if (type.isChipType) {
                ObservationChipRow(
                    type = type,
                    selectedValue = staging[type.id],
                    tally = tally,
                    denominatorText = denom,
                    successCriterion = successCriterion,
                    arming = arming,
                    enabled = !readOnly,
                    onSelect = { value -> onStageChip(type.id, value) },
                )
            } else {
                GridLauncherRow(
                    type = type,
                    stagedValue = staging[type.id],
                    denominatorText = denom,
                    handedness = handedness,
                    arming = arming,
                    enabled = !readOnly,
                    onOpen = { onOpenGrid(type) },
                )
            }
        }
    }
}

// ── Chip row ──────────────────────────────────────────────────────────────────

@Composable
internal fun ObservationChipRow(
    type: ObservationType,
    selectedValue: String?,
    tally: TypeTally?,
    denominatorText: String?,
    successCriterion: String?,
    arming: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = chipOptions(type)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ObservationRowHeader(
            label = type.displayLabel,
            rubric = if (type == ObservationType.SUCCESS) successCriterion else null,
            denominatorText = denominatorText,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (option in options) {
                val staged = selectedValue == option.id
                val count = tally?.valueCounts?.get(option.id) ?: 0
                ChipSegment(
                    modifier = Modifier.weight(1f),
                    label = option.label,
                    staged = staged,
                    armed = staged && arming,
                    enabled = enabled,
                    countText = if (tally != null && count > 0) count.toString() else "",
                    reserveCount = tally != null,
                    glyph = { color ->
                        CompositionLocalProvider(LocalContentColor provides color) {
                            ChipGlyph(type, option.id)
                        }
                    },
                    onClick = { onSelect(option.id) },
                )
            }
        }
    }
}

@Composable
private fun ChipSegment(
    label: String,
    staged: Boolean,
    armed: Boolean,
    enabled: Boolean,
    countText: String,
    reserveCount: Boolean,
    glyph: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when {
        armed -> colors.primary to colors.onPrimary
        staged -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .height(if (reserveCount) 58.dp else 46.dp)
            .background(container, RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                contentDescription = if (staged) "$label, selected" else label
            }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        glyph(content)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        if (reserveCount) {
            Text(
                text = countText,
                style = RangeworkMono.small,
                color = content,
                maxLines = 1,
            )
        }
    }
}

/** Draws the value glyph for a chip type; Success is deliberately glyphless. */
@Composable
private fun ChipGlyph(type: ObservationType, valueId: String) {
    val h = 16.dp
    when (type) {
        ObservationType.CONTACT -> ContactValue.fromId(valueId)?.let { ContactGlyph(it, h) }
        ObservationType.DISTANCE -> DistanceValue.fromId(valueId)?.let { DistanceGlyph(it, h) }
        ObservationType.DIRECTION -> DirectionValue.fromId(valueId)?.let { DirectionGlyph(it, h) }
        else -> Unit
    }
}

// ── Grid launcher row ───────────────────────────────────────────────────────────

@Composable
internal fun GridLauncherRow(
    type: ObservationType,
    stagedValue: String?,
    denominatorText: String?,
    handedness: Handedness,
    arming: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val staged = stagedValue != null
    val (container, content) = when {
        staged && arming -> colors.primary to colors.onPrimary
        staged -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ObservationRowHeader(
            label = type.displayLabel,
            rubric = null,
            denominatorText = denominatorText,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(container, RoundedCornerShape(10.dp))
                .then(if (enabled) Modifier.clickable(onClick = onOpen) else Modifier)
                .padding(horizontal = 12.dp)
                .semantics {
                    contentDescription = buildString {
                        append(type.displayLabel)
                        append(if (staged) ", ${gridSummaryLabel(type, stagedValue!!, handedness)}" else ", not marked")
                        append(", tap to open")
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                if (staged) {
                    GridValueGlyph(type, stagedValue!!, handedness)
                } else {
                    MiniGridGlyph(height = 20.dp)
                }
            }
            Text(
                text = if (staged) gridSummaryLabel(type, stagedValue!!, handedness) else "Tap to mark",
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = content,
            )
        }
    }
}

/** The staged/stored value glyph on a launcher or edit row. */
@Composable
internal fun GridValueGlyph(type: ObservationType, valueId: String, handedness: Handedness) {
    when (type) {
        ObservationType.STRIKE_LOCATION ->
            StrikeLocation.fromId(valueId)?.let { ClubfaceGlyph(it, handedness, height = 24.dp) }
        ObservationType.SHAPE ->
            ShapeFlight.fromId(valueId)?.let { FlightGlyph(it, height = 26.dp) }
        else -> Unit
    }
}

// ── Shared bits ─────────────────────────────────────────────────────────────────

@Composable
private fun ObservationRowHeader(
    label: String,
    rubric: String?,
    denominatorText: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rubric != null) {
                Text(
                    text = " · $rubric",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        if (denominatorText != null) {
            Text(
                text = denominatorText,
                style = RangeworkMono.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

// ── Vocabulary → display ────────────────────────────────────────────────────────

internal data class ChipOption(val id: String, val label: String)

/** The fixed capture row order: Success, then chip scales, then grid launchers. */
private val CAPTURE_ORDER = listOf(
    ObservationType.SUCCESS,
    ObservationType.CONTACT,
    ObservationType.DISTANCE,
    ObservationType.DIRECTION,
    ObservationType.STRIKE_LOCATION,
    ObservationType.SHAPE,
)

internal fun orderedCaptureTypes(types: List<ObservationType>): List<ObservationType> =
    CAPTURE_ORDER.filter { it in types }

private val ObservationType.isChipType: Boolean
    get() = this == ObservationType.SUCCESS ||
        this == ObservationType.CONTACT ||
        this == ObservationType.DISTANCE ||
        this == ObservationType.DIRECTION

internal val ObservationType.displayLabel: String
    get() = when (this) {
        ObservationType.SUCCESS -> "Success"
        ObservationType.CONTACT -> "Contact"
        ObservationType.DISTANCE -> "Distance"
        ObservationType.DIRECTION -> "Direction"
        ObservationType.STRIKE_LOCATION -> "Strike location"
        ObservationType.SHAPE -> "Shape"
    }

private fun chipOptions(type: ObservationType): List<ChipOption> = when (type) {
    ObservationType.SUCCESS -> listOf(
        ChipOption(SuccessValue.HIT.id, "Hit"),
        ChipOption(SuccessValue.MISS.id, "Miss"),
    )
    ObservationType.CONTACT -> listOf(
        ChipOption(ContactValue.VERY_FAT.id, "V. Fat"),
        ChipOption(ContactValue.FAT.id, "Fat"),
        ChipOption(ContactValue.FLUSH.id, "Flush"),
        ChipOption(ContactValue.THIN.id, "Thin"),
        ChipOption(ContactValue.VERY_THIN.id, "V. Thin"),
    )
    ObservationType.DISTANCE -> listOf(
        ChipOption(DistanceValue.WAY_SHORT.id, "W. Short"),
        ChipOption(DistanceValue.SHORT.id, "Short"),
        ChipOption(DistanceValue.ON.id, "On"),
        ChipOption(DistanceValue.LONG.id, "Long"),
        ChipOption(DistanceValue.WAY_LONG.id, "W. Long"),
    )
    ObservationType.DIRECTION -> listOf(
        ChipOption(DirectionValue.WAY_LEFT.id, "W. Left"),
        ChipOption(DirectionValue.LEFT.id, "Left"),
        ChipOption(DirectionValue.ON_LINE.id, "On"),
        ChipOption(DirectionValue.RIGHT.id, "Right"),
        ChipOption(DirectionValue.WAY_RIGHT.id, "W. Right"),
    )
    else -> emptyList()
}

/** The display label for a single stored value of any type (chip label or grid summary). */
internal fun observationValueLabel(type: ObservationType, valueId: String, handedness: Handedness): String =
    when (type) {
        ObservationType.STRIKE_LOCATION, ObservationType.SHAPE -> gridSummaryLabel(type, valueId, handedness)
        else -> chipOptions(type).firstOrNull { it.id == valueId }?.label ?: "—"
    }

/** The one-line summary shown on a staged grid launcher / edit row. */
internal fun gridSummaryLabel(type: ObservationType, valueId: String, handedness: Handedness): String =
    when (type) {
        ObservationType.STRIKE_LOCATION ->
            StrikeLocation.fromId(valueId)?.let { "${it.column.label} · ${it.row.label}" } ?: "—"
        ObservationType.SHAPE ->
            ShapeFlight.fromId(valueId)?.golferLabel(handedness) ?: "—"
        else -> "—"
    }

internal val StrikeColumn.label: String
    get() = when (this) {
        StrikeColumn.HEEL -> "Heel"
        StrikeColumn.CENTER -> "Center"
        StrikeColumn.TOE -> "Toe"
    }

internal val StrikeRow.label: String
    get() = when (this) {
        StrikeRow.HIGH -> "High"
        StrikeRow.MIDDLE -> "Middle"
        StrikeRow.LOW -> "Low"
    }
