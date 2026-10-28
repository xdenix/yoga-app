package com.example.yogaapp.domain.model

/**
 * Difficulty level of an asana or a program. Drives the 30-day plan generator:
 * the Beginner program pulls mostly [Beginner] poses, Intermediate adds [Intermediate],
 * Advanced pulls from the full library including [Advanced] poses.
 */
enum class YogaLevel {
    Beginner,
    Intermediate,
    Advanced,
}

/**
 * Body focus — used on the Day card to give the user a quick sense of what the session targets.
 * Multiple focuses can apply to a single pose (e.g. Warrior II is both Legs and Balance).
 */
enum class BodyFocus { Standing, Seated, Balance, Twist, Backbend, ForwardFold, Core, Rest }

/**
 * Geometric spec of a pose for the [com.example.yogaapp.ui.components.PoseFigure] Canvas.
 *
 * Angles are in degrees, 0 = pointing up (toward head), measured clockwise.
 * All angles are between the body segment and the vertical axis of the torso.
 *
 * The figure is a stick-figure: head, neck (fixed length), torso (fixed length),
 * upper-arm, lower-arm, upper-leg, lower-leg — drawn on each side (left/right) using
 * the same angles mirrored. This keeps the spec compact (one set of angles per pose)
 * while still producing a recognisable silhouette for all 40 asanas.
 *
 * Example: Tadasana (Mountain) — all angles 0 → straight standing figure.
 *          Bhujangasana (Cobra) — torsoTilt=180 (lying face-down), arms straight under shoulders.
 */
data class PoseSpec(
    /** Torso tilt from vertical. 0 = standing upright. 90 = horizontal. 180 = inverted. */
    val torsoTilt: Float,
    /** Hip angle relative to torso (0 = aligned with torso). */
    val hipAngle: Float,
    /** Upper-leg angle relative to vertical (knee out from body). */
    val upperLegAngle: Float,
    /** Lower-leg angle relative to upper-leg (knee bend). 0 = straight leg. */
    val lowerLegAngle: Float,
    /** Upper-arm angle relative to vertical (arm out from body). */
    val upperArmAngle: Float,
    /** Lower-arm angle relative to upper-arm (elbow bend). 0 = straight arm. */
    val lowerArmAngle: Float,
    /** Whether legs are crossed (lotus / sukhasana). When true, lower legs are drawn crossed. */
    val legsCrossed: Boolean = false,
    /** Whether the figure is kneeling (knees on the ground). Used for Vajrasana, Camel. */
    val kneeling: Boolean = false,
    /** Whether arms are overhead (Warrior I, Tree). When true, arms drawn above head. */
    val armsOverhead: Boolean = false,
    /** Whether arms are in prayer position at chest (Tree, Prayer Twist). */
    val armsInPrayer: Boolean = false,
)

/**
 * Domain entity for a single asana. Immutable. No Android imports — JVM-testable.
 *
 * [id] is a stable slug (e.g. "tadasana") used as the Room primary key; this lets the
 * 30-day plan reference poses by id without worrying about UUID churn.
 *
 * [defaultDurationSec] is the per-pose default; the user can override it globally or per-pose
 * on the Day detail screen (see [com.example.yogaapp.ui.day.DayViewModel]).
 *
 * [audioAssetName] is optional — when null, the AudioPlayer falls back to TTS for this pose.
 * When set, it's the file name (without extension) of an MP3 in the optional audio pack
 * (assets/audio-pack/); see [com.example.yogaapp.data.audio.AudioPackAssetProvider].
 */
data class YogaPose(
    val id: String,
    val sanskritName: String,
    val ruName: String,
    val enName: String,
    val ruDescription: String,
    val enDescription: String,
    val level: YogaLevel,
    val focuses: List<BodyFocus>,
    val defaultDurationSec: Int,
    val spec: PoseSpec,
    val audioAssetName: String? = null,
)

/**
 * Pure business rules for poses & programs.
 */
object YogaRules {

    const val MIN_DURATION_SEC = 10
    const val MAX_DURATION_SEC = 300
    const val DEFAULT_GLOBAL_DURATION_SEC = 30
    const val PROGRAM_DAYS = 30

    fun isDurationValid(sec: Int): Boolean = sec in MIN_DURATION_SEC..MAX_DURATION_SEC

    fun durationLabel(sec: Int): String {
        return when {
            sec < 60 -> "${sec}с"
            sec % 60 == 0 -> "${sec / 60} мин"
            else -> "${sec / 60} мин ${sec % 60} с"
        }
    }
}
