package com.example.yogaapp.domain.model

import com.example.yogaapp.data.datastore.SettingsDataStore

/**
 * Отдельные блоки практики вне 30-дневного плана.
 * Открываются с главного экрана как обычные [YogaDay] (день → сессия).
 */
object WorkoutBlocks {

    const val WEIGHT_LOSS_ID = "block-weight-loss"
    const val STRENGTH_ID = "block-strength"

    /** Йога-последовательность с акцентом на жиросжигание / динамику. */
    private val weightLossPoseIds = listOf(
        "marjaryasana_bitilasana",
        "adho_mukha_svanasana",
        "utkatasana",
        "virabhadrasana_1",
        "virabhadrasana_2",
        "utthita_trikonasana",
        "anjaneyasana",
        "phalakasana",
        "chaturanga",
        "navasana",
        "vasisthasana",
        "setu_bandhasana",
        "ardha_matsyendrasana",
        "balasana",
        "savasana",
    )

    fun byId(
        id: String,
        weightLossCount: Int = SettingsDataStore.DEFAULT_WEIGHT_LOSS_POSES,
        strengthCount: Int = SettingsDataStore.DEFAULT_STRENGTH_POSES,
    ): YogaDay? = when (id) {
        WEIGHT_LOSS_ID -> weightLossDay(weightLossCount)
        STRENGTH_ID -> strengthDay(strengthCount)
        else -> null
    }

    fun weightLossDay(
        posesCount: Int = SettingsDataStore.DEFAULT_WEIGHT_LOSS_POSES,
    ): YogaDay {
        val base = weightLossPoseIds.mapNotNull { PoseLibrary.byId(it) }
        val allEntries = YogaRules.toDayEntries(base)
        val count = posesCount.coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)
        val entries = sliceOrRepeatEntries(allEntries, count, keepLast = true)
        return YogaDay(
            id = WEIGHT_LOSS_ID,
            dayNumber = 0,
            title = "Похудение · йога",
            poses = entries,
            isCustom = true,
        )
    }

    fun strengthDay(
        posesCount: Int = SettingsDataStore.DEFAULT_STRENGTH_POSES,
    ): YogaDay {
        val base = StrengthLibrary.all
        val allEntries = YogaRules.toDayEntries(base)
        val count = posesCount.coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)
        val entries = sliceOrRepeatEntries(allEntries, count, keepLast = false)
        return YogaDay(
            id = STRENGTH_ID,
            dayNumber = 0,
            title = "Силовой блок",
            poses = entries,
            isCustom = true,
        )
    }

    private fun sliceOrRepeatEntries(
        source: List<DayPoseEntry>,
        targetCount: Int,
        keepLast: Boolean,
    ): List<DayPoseEntry> {
        if (source.isEmpty()) return emptyList()
        val picked = when {
            targetCount == source.size -> source
            targetCount < source.size -> {
                if (keepLast && targetCount >= 2) {
                    source.take(targetCount - 1) + source.last()
                } else {
                    source.take(targetCount)
                }
            }
            else -> {
                val pool = if (keepLast && source.size >= 2) source.dropLast(1) else source
                val bodyTarget = if (keepLast) targetCount - 1 else targetCount
                val list = ArrayList<DayPoseEntry>(targetCount)
                var i = 0
                while (list.size < bodyTarget) {
                    list.add(pool[i % pool.size])
                    i++
                }
                if (keepLast) list.add(source.last())
                list
            }
        }
        return picked.mapIndexed { idx, e -> e.copy(orderIndex = idx) }
    }

    fun allBlocks(): List<YogaDay> = listOf(weightLossDay(), strengthDay())
}
