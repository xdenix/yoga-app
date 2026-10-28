package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.BodyFocus
import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaProgram
import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaRules
import javax.inject.Inject
import kotlin.math.min

/**
 * Generates a 30-day yoga program tailored to the user's chosen level.
 *
 * Algorithm (deterministic — same level always produces same plan, so users can swap levels
 * and back without surprises):
 *  - Pull poses eligible for the level via [PoseLibrary.forLevel]
 *  - Spread across 30 days; each day gets 5-8 poses (more as the program progresses)
 *  - Order poses within a day by flow: warmup (standing) → peak (balance/backbend) → cool (seated/forward fold) → rest
 *  - Difficulty ramps: days 1-7 use only Beginner poses; days 8-15 mix in Intermediate;
 *    days 16-30 add Advanced (only at level Advanced).
 *
 * Pure function — no Android or coroutine dependency. Tested in isolation.
 */
class GenerateProgramUseCase @Inject constructor() {

    operator fun invoke(level: YogaLevel): YogaProgram {
        val eligible = PoseLibrary.forLevel(level)
        require(eligible.isNotEmpty()) {
            "Pose library must contain poses for level $level"
        }

        val days = (1..YogaRules.PROGRAM_DAYS).map { dayNum ->
            val dailyPoses = pickForDay(dayNum, level, eligible)
            val title = titleForDay(dayNum, level)
            YogaDay(
                id = "program-$dayNum",
                dayNumber = dayNum,
                title = title,
                poses = dailyPoses.mapIndexed { idx, p ->
                    DayPoseEntry(poseId = p.id, orderIndex = idx, durationOverrideSec = null)
                },
                isCustom = false,
            )
        }
        return YogaProgram(
            id = "default-program-${level.name.lowercase()}",
            level = level,
            days = days,
            currentDay = 1,
        )
    }

    /**
     * Picks 5-7 poses for the given day, biased by day progression (later days → longer sessions
     * and higher intensity). Ordering follows the standard vinyasa arc:
     *   warmup (standing, simple) → sun salutation arc → peak (balance/backbend) → cool-down → savasana
     */
    private fun pickForDay(dayNum: Int, level: YogaLevel, pool: List<YogaPose>): List<YogaPose> {
        // Pose count grows from 5 (day 1) to 7 (day 30).
        val count = (5 + (dayNum - 1) * 2 / (YogaRules.PROGRAM_DAYS - 1)).coerceIn(5, 7)

        // Filter pool by day-phase difficulty.
        val phasePool = when {
            dayNum <= 7 -> pool.filter { it.level == YogaLevel.Beginner }
            dayNum <= 15 && level != YogaLevel.Beginner ->
                pool.filter { it.level != YogaLevel.Advanced }
            level == YogaLevel.Advanced -> pool
            else -> pool.filter { it.level != YogaLevel.Advanced }
        }.ifEmpty { pool }

        // Bucket poses by focus for arc ordering.
        val warmups = phasePool.filter { BodyFocus.Standing in it.focuses || BodyFocus.Core in it.focuses }
        val peaks = phasePool.filter {
            BodyFocus.Backbend in it.focuses || BodyFocus.Balance in it.focuses ||
                BodyFocus.Twist in it.focuses
        }
        val cools = phasePool.filter {
            BodyFocus.Seated in it.focuses || BodyFocus.ForwardFold in it.focuses
        }
        val rests = phasePool.filter { BodyFocus.Rest in it.focuses }

        val picked = mutableListOf<YogaPose>()
        // Warmup: 2 poses (rotating by dayNum so different days use different warmups)
        picked += warmups.rotatePick(dayNum - 1, min(2, warmups.size))
        // Peak: 2 poses
        picked += peaks.rotatePick(dayNum + 1, min(2, peaks.size))
        // Cool: 1-2 poses
        picked += cools.rotatePick(dayNum + 3, min(if (count >= 7) 2 else 1, cools.size))
        // Rest: always end with a rest (savasana preferred). Reserve the last slot so
        // take(count) cannot drop it.
        val rest = rests.firstOrNull { it.id == "savasana" } ?: rests.firstOrNull()
        if (rest != null) {
            picked.removeAll { it.id == rest.id }
        }
        val bodyLimit = if (rest != null) (count - 1).coerceAtLeast(0) else count
        val result = picked.distinctBy { it.id }.take(bodyLimit).toMutableList()
        if (rest != null) result += rest
        return result
    }

    private fun List<YogaPose>.rotatePick(offset: Int, count: Int): List<YogaPose> {
        if (isEmpty() || count <= 0) return emptyList()
        val result = mutableListOf<YogaPose>()
        var idx = ((offset % size) + size) % size
        repeat(count) {
            result += this[idx]
            idx = (idx + 1) % size
        }
        return result
    }

    private fun titleForDay(dayNum: Int, level: YogaLevel): String = when {
        dayNum == 1 -> "День 1: Знакомство с дыханием"
        dayNum == 7 -> "День 7: Контрольная точка"
        dayNum == 15 -> "День 15: Половина пути"
        dayNum == 30 -> "День 30: Итоговая сессия"
        dayNum <= 7 -> "День $dayNum: Основы"
        dayNum <= 15 -> "День $dayNum: Углубление"
        dayNum <= 22 -> "День $dayNum: Сила и баланс"
        else -> "День $dayNum: Интеграция"
    }
}
