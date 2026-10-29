package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.BodyFocus
import com.example.yogaapp.domain.model.DayPlanTemplates
import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaProgram
import com.example.yogaapp.domain.model.YogaRules
import javax.inject.Inject

/**
 * Generates a 30-day yoga program for the chosen level.
 *
 * Days follow curated [DayPlanTemplates] sequences (warm-up → peak → cool-down)
 * rather than a random shuffle. Bilateral asanas expand to right + left; savasana stays last.
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
        val byId = eligible.associateBy { it.id }

        val days = (1..YogaRules.PROGRAM_DAYS).map { dayNum ->
            val plan = DayPlanTemplates.planForDay(level, dayNum)
            val dailyPoses = buildDayFromPlan(plan, byId, eligible, count, dayNum, level)
            YogaDay(
                id = "program-$dayNum",
                dayNumber = dayNum,
                title = "День $dayNum: ${plan.titleRu}",
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

    /**
     * Takes poses from the template in order, respecting level gates for early weeks,
     * filling up to [slotLimit] after bilateral expansion, always ending with rest.
     */
    private fun buildDayFromPlan(
        plan: DayPlanTemplates.Plan,
        byId: Map<String, YogaPose>,
        pool: List<YogaPose>,
        slotLimit: Int,
        dayNum: Int,
        level: YogaLevel,
    ): List<YogaPose> {
        val phaseOk: (YogaPose) -> Boolean = when {
            dayNum <= 7 -> { p -> p.level == YogaLevel.Beginner }
            dayNum <= 15 && level != YogaLevel.Beginner -> { p -> p.level != YogaLevel.Advanced }
            level == YogaLevel.Advanced -> { _ -> true }
            else -> { p -> p.level != YogaLevel.Advanced }
        }

        val rest = byId["savasana"]
            ?: pool.firstOrNull { BodyFocus.Rest in it.focuses }
        val bodyLimit = if (rest != null) (slotLimit - 1).coerceAtLeast(0) else slotLimit

        val ordered = LinkedHashSet<YogaPose>()
        for (id in plan.poseIds) {
            if (id == rest?.id) continue
            val pose = byId[id] ?: continue
            if (!phaseOk(pose)) continue
            ordered += pose
        }
        // Fill gaps from the same pedagogical buckets if the template was filtered heavily.
        if (ordered.sumOf { YogaRules.slotCost(it) } < bodyLimit) {
            for (pose in pool) {
                if (pose.id == rest?.id) continue
                if (!phaseOk(pose)) continue
                ordered += pose
                if (ordered.sumOf { YogaRules.slotCost(it) } >= bodyLimit) break
            }
        }

        val body = trimToSlotLimit(ordered.toList(), bodyLimit)
        return if (rest != null) body + rest else body
    }

    private fun trimToSlotLimit(poses: List<YogaPose>, slotLimit: Int): List<YogaPose> {
        val out = mutableListOf<YogaPose>()
        var slots = 0
        for (p in poses) {
            if (slots >= slotLimit) break
            val cost = YogaRules.slotCost(p)
            if (slots + cost > slotLimit) continue
            out += p
            slots += cost
        }
        return out
    }
}
