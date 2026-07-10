package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The v1 Observation Type vocabulary and each type's fixed value encoding.
 *
 * See `design-docs/range-session-data-recording/design-decisions.md` §5 and the
 * Stage 2 plan's D4 decision (physical target-line frame): stored value strings
 * describe what the ball and club physically did, in a frame that does not depend
 * on the player. Handedness affects rendering geometry and golfer-term labels
 * only — never the stored value. Rendering transforms live in
 * [ObservationRendering]; storage lives here.
 *
 * Wire ids are fixed by Stage 1 (the migration constrains them). Value strings on
 * the wire are tolerated as opaque `String`s and mapped to these enums by typed
 * accessors that *drop* unknowns instead of failing decode — an old app reading
 * data written by a newer schema must degrade, not crash.
 */
@Serializable
enum class ObservationType(val id: String) {
    @SerialName("success")
    SUCCESS("success"),

    @SerialName("strike_location")
    STRIKE_LOCATION("strike_location"),

    @SerialName("contact")
    CONTACT("contact"),

    @SerialName("shape")
    SHAPE("shape"),

    @SerialName("distance")
    DISTANCE("distance"),

    @SerialName("direction")
    DIRECTION("direction");

    /** The set of canonical value strings this type accepts. */
    fun vocabulary(): Set<String> = when (this) {
        SUCCESS -> SuccessValue.entries.map(SuccessValue::id).toSet()
        CONTACT -> ContactValue.entries.map(ContactValue::id).toSet()
        DISTANCE -> DistanceValue.entries.map(DistanceValue::id).toSet()
        DIRECTION -> DirectionValue.entries.map(DirectionValue::id).toSet()
        STRIKE_LOCATION -> StrikeLocation.all.map(StrikeLocation::id).toSet()
        SHAPE -> ShapeFlight.all.map(ShapeFlight::id).toSet()
    }

    /** True when [value] is a canonical member of this type's vocabulary. */
    fun accepts(value: String): Boolean = value in vocabulary()

    companion object {
        /** The wire id → type mapping; null for ids this app version doesn't know. */
        fun fromId(id: String): ObservationType? = entries.firstOrNull { it.id == id }
    }
}

// ── Single-value scale vocabularies (handedness-neutral) ──────────────────────

/** Success — the only type that records goodness. */
enum class SuccessValue(val id: String) {
    HIT("hit"),
    MISS("miss");

    companion object {
        fun fromId(id: String): SuccessValue? = entries.firstOrNull { it.id == id }
    }
}

/** Turf/face contact quality — ordered scale, fat → thin. */
enum class ContactValue(val id: String) {
    VERY_FAT("very_fat"),
    FAT("fat"),
    FLUSH("flush"),
    THIN("thin"),
    VERY_THIN("very_thin");

    companion object {
        fun fromId(id: String): ContactValue? = entries.firstOrNull { it.id == id }
    }
}

/** Carry relative to target depth — ordered scale, short → long. */
enum class DistanceValue(val id: String) {
    WAY_SHORT("way_short"),
    SHORT("short"),
    ON("on"),
    LONG("long"),
    WAY_LONG("way_long");

    companion object {
        fun fromId(id: String): DistanceValue? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Physical left/right of the target line — ordered scale, left → right.
 * Under the D4 physical frame these read Way Left → Way Right for every player;
 * only derived coaching commentary needs handedness.
 */
enum class DirectionValue(val id: String) {
    WAY_LEFT("way_left"),
    LEFT("left"),
    ON_LINE("on_line"),
    RIGHT("right"),
    WAY_RIGHT("way_right");

    companion object {
        fun fromId(id: String): DirectionValue? = entries.firstOrNull { it.id == id }
    }
}

// ── Handedness ────────────────────────────────────────────────────────────────

/**
 * User Preference orienting the perspective-dependent rendering surfaces.
 * Wire values `'RIGHT'|'LEFT'` are fixed by Stage 1; RIGHT is the app's historical
 * implicit rendering and the identity for every transform in [ObservationRendering].
 */
@Serializable
enum class Handedness {
    RIGHT,
    LEFT;

    companion object {
        fun fromId(id: String): Handedness? = entries.firstOrNull { it.name == id }
    }
}

// ── Strike Location (row × column) ────────────────────────────────────────────

/** Vertical face axis — never mirrors (high/low is handedness-independent). */
enum class StrikeRow(val id: String) {
    HIGH("high"),
    MIDDLE("middle"),
    LOW("low"),
}

/** Horizontal face axis — club anatomy, inherently handedness-neutral in storage. */
enum class StrikeColumn(val id: String) {
    HEEL("heel"),
    CENTER("center"),
    TOE("toe");

    /** Heel ↔ toe swap; center is fixed. An involution ([mirror].[mirror] == identity). */
    fun mirror(): StrikeColumn = when (this) {
        HEEL -> TOE
        CENTER -> CENTER
        TOE -> HEEL
    }
}

/**
 * A face-strike zone: `{row}_{column}` (e.g. `high_heel`, `middle_center`).
 * Nine handedness-neutral values; rendering mirrors the *columns* for a
 * left-handed player so screen positions match the face as they look down at it.
 */
data class StrikeLocation(val row: StrikeRow, val column: StrikeColumn) {
    val id: String get() = "${row.id}_${column.id}"

    companion object {
        val all: List<StrikeLocation> =
            StrikeRow.entries.flatMap { row -> StrikeColumn.entries.map { column -> StrikeLocation(row, column) } }

        fun fromId(id: String): StrikeLocation? = all.firstOrNull { it.id == id }
    }
}

// ── Shape (start line × curvature) ────────────────────────────────────────────

/**
 * A physical lateral direction relative to the target line, used for both the
 * start line and the curvature of a [ShapeFlight].
 */
enum class ShapeDirection(val id: String) {
    LEFT("left"),
    STRAIGHT("straight"),
    RIGHT("right");

    /** Left ↔ right swap; straight is fixed. An involution. */
    fun mirror(): ShapeDirection = when (this) {
        LEFT -> RIGHT
        STRAIGHT -> STRAIGHT
        RIGHT -> LEFT
    }
}

/**
 * A ball flight: `{start}_{curve}` (e.g. `straight_right` = started straight,
 * curved right). Nine physical values; golfer-term labels (draw/fade/pull/push)
 * are derived at display time from value + handedness in [ObservationRendering].
 */
data class ShapeFlight(val start: ShapeDirection, val curve: ShapeDirection) {
    val id: String get() = "${start.id}_${curve.id}"

    companion object {
        val all: List<ShapeFlight> =
            ShapeDirection.entries.flatMap { start -> ShapeDirection.entries.map { curve -> ShapeFlight(start, curve) } }

        fun fromId(id: String): ShapeFlight? = all.firstOrNull { it.id == id }
    }
}
