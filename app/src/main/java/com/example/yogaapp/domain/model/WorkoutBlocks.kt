package com.example.yogaapp.domain.model

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

    fun byId(id: String): YogaDay? = when (id) {
        WEIGHT_LOSS_ID -> weightLossDay()
        STRENGTH_ID -> strengthDay()
        else -> null
    }

    fun weightLossDay(): YogaDay {
        val poses = weightLossPoseIds.mapNotNull { PoseLibrary.byId(it) }
        return YogaDay(
            id = WEIGHT_LOSS_ID,
            dayNumber = 0,
            title = "Похудение · йога",
            poses = YogaRules.toDayEntries(poses),
            isCustom = true,
        )
    }

    fun strengthDay(): YogaDay {
        val poses = StrengthLibrary.all
        return YogaDay(
            id = STRENGTH_ID,
            dayNumber = 0,
            title = "Силовой блок",
            poses = YogaRules.toDayEntries(poses),
            isCustom = true,
        )
    }

    fun allBlocks(): List<YogaDay> = listOf(weightLossDay(), strengthDay())
}
