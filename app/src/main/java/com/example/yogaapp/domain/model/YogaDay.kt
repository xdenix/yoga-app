package com.example.yogaapp.domain.model

/**
 * One day's session within a 30-day program (or a custom day created by the user).
 *
 * Each day has:
 *  - [id] stable slug (program-1, program-2, ..., custom-1, custom-2, ...)
 *  - [dayNumber] 1-indexed display number (1..30 for the program, or 1..N for custom days)
 *  - [title] e.g. "День 1: Основы"
 *  - [poses] ordered list of pose entries (id + per-pose duration override; null = use pose default)
 *
 * Custom days ([isCustom] = true) are user-created from the PoseLibrary; the user can also
 * edit program days (reorder, swap, change durations) — these edits are persisted as
 * overrides on top of the generated plan, not as a destructive change to the original.
 */
data class YogaDay(
    val id: String,
    val dayNumber: Int,
    val title: String,
    val poses: List<DayPoseEntry>,
    val isCustom: Boolean = false,
    val isSkipped: Boolean = false,
)

/**
 * Which side to practice for an asymmetric ([YogaPose.bilateral]) asana.
 * [None] is used for symmetric poses (or a single unscoped slot).
 */
enum class PoseSide {
    None,
    Right,
    Left,
}

/**
 * A pose entry within a day. Holds the pose id (references [YogaPose.id]) and an optional
 * duration override — when null, the player uses the pose's [YogaPose.defaultDurationSec].
 *
 * [orderIndex] is the position in the session (0-indexed) — supports drag-to-reorder.
 * [side] is [PoseSide.Right]/[PoseSide.Left] when the day expands a bilateral pose into
 * two consecutive slots (right, then left).
 */
data class DayPoseEntry(
    val poseId: String,
    val orderIndex: Int,
    val durationOverrideSec: Int?,
    val side: PoseSide = PoseSide.None,
)

/**
 * A 30-day program generated from the user's chosen level. Persisted in Room so the user
 * can pick up where they left off across launches.
 *
 * [currentDay] is the user's progress pointer; advanced when they complete a session.
 */
data class YogaProgram(
    val id: String,
    val level: YogaLevel,
    val days: List<YogaDay>,
    val currentDay: Int = 1, // 1..30
)

/**
 * Per-day progress tracking — when the user completes a session, a record is written.
 * [completedAt] is epoch millis for sort/display.
 */
data class SessionProgress(
    val dayId: String,
    val completedAt: Long,
    val durationSec: Int,
)
