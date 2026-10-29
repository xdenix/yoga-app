package com.example.yogaapp.domain.model

/**
 * Готовые последовательности асан в педагогическом порядке
 * (разминка → стойки → баланс/сила → скрутки/наклоны → расслабление).
 *
 * Используются генератором 30-дневного плана вместо случайного набора.
 * Id ссылаются на [PoseLibrary]; отсутствующие для уровня позы пропускаются.
 */
object DayPlanTemplates {

    data class Plan(
        val titleRu: String,
        val poseIds: List<String>,
    )

    /** Лёгкий утренний хатха-поток (новичок). */
    val morningBasics = Plan(
        titleRu = "Утренние основы",
        poseIds = listOf(
            "marjaryasana_bitilasana",
            "balasana",
            "adho_mukha_svanasana",
            "tadasana",
            "utkatasana",
            "virabhadrasana_1",
            "virabhadrasana_2",
            "utthita_trikonasana",
            "anjaneyasana",
            "bhujangasana",
            "setu_bandhasana",
            "paschimottanasana",
            "sukhasana",
            "savasana",
        ),
    )

    /** Баланс и устойчивость. */
    val balanceFocus = Plan(
        titleRu = "Баланс и устойчивость",
        poseIds = listOf(
            "tadasana",
            "marjaryasana_bitilasana",
            "adho_mukha_svanasana",
            "vrksasana",
            "virabhadrasana_2",
            "utthita_trikonasana",
            "anjaneyasana",
            "phalakasana",
            "vasisthasana",
            "balasana",
            "viparita_karani",
            "savasana",
        ),
    )

    /** Мягкий поток для спины и раскрытия груди. */
    val gentleBackbend = Plan(
        titleRu = "Мягкое раскрытие",
        poseIds = listOf(
            "sukhasana",
            "marjaryasana_bitilasana",
            "balasana",
            "sphinx",
            "bhujangasana",
            "setu_bandhasana",
            "vajrasana",
            "ustrasana",
            "baddha_konasana",
            "janu_sirsasana",
            "viparita_karani",
            "savasana",
        ),
    )

    /** Силовой виньяса-ориентир (средний). */
    val strengthFlow = Plan(
        titleRu = "Сила и поток",
        poseIds = listOf(
            "adho_mukha_svanasana",
            "phalakasana",
            "chaturanga",
            "urdhva_mukha_svanasana",
            "virabhadrasana_1",
            "virabhadrasana_2",
            "virabhadrasana_3",
            "utkatasana",
            "navasana",
            "salabhasana",
            "dhanurasana",
            "ardha_matsyendrasana",
            "paschimottanasana",
            "savasana",
        ),
    )

    /** Скрутки и наклоны вперёд. */
    val twistAndFold = Plan(
        titleRu = "Скрутки и наклоны",
        poseIds = listOf(
            "tadasana",
            "prasarita_padottanasana",
            "utthita_trikonasana",
            "anjaneyasana",
            "balasana",
            "ardha_matsyendrasana",
            "marichyasana",
            "janu_sirsasana",
            "paschimottanasana",
            "baddha_konasana",
            "halasana",
            "savasana",
        ),
    )

    /** Восстановление и инверсии лёгкого уровня. */
    val restoreAndInvert = Plan(
        titleRu = "Восстановление",
        poseIds = listOf(
            "sukhasana",
            "balasana",
            "setu_bandhasana",
            "viparita_karani",
            "halasana",
            "sarvangasana",
            "baddha_konasana",
            "paschimottanasana",
            "savasana",
        ),
    )

    /** Продвинутый баланс и сила рук. */
    val advancedPeak = Plan(
        titleRu = "Пиковые асаны",
        poseIds = listOf(
            "adho_mukha_svanasana",
            "phalakasana",
            "chaturanga",
            "bakasana",
            "pincha_mayurasana",
            "virabhadrasana_3",
            "ardha_chandrasana",
            "eka_pada_rajakapotasana",
            "kapotasana",
            "urdhva_dhanurasana",
            "sirsasana",
            "sarvangasana",
            "halasana",
            "savasana",
        ),
    )

    val beginnerPlans: List<Plan> = listOf(
        morningBasics,
        balanceFocus,
        gentleBackbend,
        twistAndFold,
        restoreAndInvert,
        morningBasics,
    )

    val intermediatePlans: List<Plan> = listOf(
        morningBasics,
        strengthFlow,
        balanceFocus,
        twistAndFold,
        gentleBackbend,
        restoreAndInvert,
        strengthFlow,
    )

    val advancedPlans: List<Plan> = listOf(
        strengthFlow,
        advancedPeak,
        balanceFocus,
        twistAndFold,
        gentleBackbend,
        advancedPeak,
        restoreAndInvert,
    )

    fun plansFor(level: YogaLevel): List<Plan> = when (level) {
        YogaLevel.Beginner -> beginnerPlans
        YogaLevel.Intermediate -> intermediatePlans
        YogaLevel.Advanced -> advancedPlans
    }

    fun planForDay(level: YogaLevel, dayNum: Int): Plan {
        val plans = plansFor(level)
        return plans[(dayNum - 1).mod(plans.size)]
    }
}
