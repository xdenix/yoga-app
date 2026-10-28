package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.BodyFocus
import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaProgram
import com.example.yogaapp.domain.model.YogaRules
import javax.inject.Inject
import kotlin.random.Random

/**
 * Generates a 30-day yoga program for the chosen level.
 *
 * Each day gets approximately [posesPerDay] timed slots (seeded by day number).
 * Bilateral asanas expand to right + left and count as two slots; savasana stays last.
 */
class GenerateProgramUseCase @Inject constructor() {

    operator fun invoke(
        level: YogaLevel,
        posesPerDay: Int = YogaRules.DEFAULT_POSES_PER_DAY,
    ): YogaProgram {
        val eligible = PoseLibrary.forLevel(level)
        require(eligible.isNotEmpty()) {
            "Pose library must contain poses for level $level"
        }
        val count = posesPerDay.coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)

        val days = (1..YogaRules.PROGRAM_DAYS).map { dayNum ->
            val dailyPoses = pickForDay(dayNum, level, eligible, count)
            YogaDay(
                id = "program-$dayNum",
                dayNumber = dayNum,
                title = titleForDay(dayNum),
                poses = YogaRules.toDayEntries(dailyPoses),
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

    private fun pickForDay(
        dayNum: Int,
        level: YogaLevel,
        pool: List<YogaPose>,
        count: Int,
    ): List<YogaPose> {
        val phasePool = when {
            dayNum <= 7 -> pool.filter { it.level == YogaLevel.Beginner }
            dayNum <= 15 && level != YogaLevel.Beginner ->
                pool.filter { it.level != YogaLevel.Advanced }
            level == YogaLevel.Advanced -> pool
            else -> pool.filter { it.level != YogaLevel.Advanced }
        }.ifEmpty { pool }

        val rest = phasePool.firstOrNull { it.id == "savasana" }
            ?: phasePool.firstOrNull { BodyFocus.Rest in it.focuses }
        val bodyPool = phasePool.filter { it.id != rest?.id }
        val rng = Random(dayNum * 31L + level.ordinal * 17L + count * 13L)
        val bodyLimit = if (rest != null) (count - 1).coerceAtLeast(0) else count
        val body = fillSlots(bodyPool.shuffled(rng).distinctBy { it.id }, bodyLimit)
        return if (rest != null) body + rest else body
    }

    /**
     * Picks unique poses until expanded slot count reaches [slotLimit].
     * Skips a bilateral pose that would overshoot when a 1-slot pose could still fit.
     */
    private fun fillSlots(candidates: List<YogaPose>, slotLimit: Int): List<YogaPose> {
        val picked = mutableListOf<YogaPose>()
        var slots = 0
        for (p in candidates) {
            if (slots >= slotLimit) break
            val cost = YogaRules.slotCost(p)
            if (slots + cost > slotLimit) continue
            picked += p
            slots += cost
        }
        return picked
    }

    private fun titleForDay(dayNum: Int): String = when {
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
