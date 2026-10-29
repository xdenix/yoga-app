package com.example.yogaapp.data.audio

/**
 * Источник фоновой музыки для практики.
 *
 * Встроенные 20–30-минутные треки в `assets/melodies/`:
 *  - [BuiltIn]          — суфийская церемония (`sufi_dervish.mp3`)
 *  - [OgdoadaWave]      — Ogdoada · волновой микс (`ogdoada_wave.mp3`)
 *  - [ZvukiPokoya]      — Звуки Покоя · концентрация (`zvuki_pokoya.mp3`)
 *  - [OgdoadaDeepFocus] — Ogdoada · Deep focus ambient (`ogdoada_deep_focus.mp3`)
 *
 * [FromFile] — файл, выбранный пользователем (URI в DataStore).
 * [YandexMusic] / [External] — музыка извне: приложение не играет свой трек.
 */
enum class PracticeMelody {
    Off,
    BuiltIn,
    OgdoadaWave,
    ZvukiPokoya,
    OgdoadaDeepFocus,
    FromFile,
    YandexMusic,
    External,
    ;

    val assetName: String?
        get() = when (this) {
            BuiltIn -> "melodies/sufi_dervish.mp3"
            OgdoadaWave -> "melodies/ogdoada_wave.mp3"
            ZvukiPokoya -> "melodies/zvuki_pokoya.mp3"
            OgdoadaDeepFocus -> "melodies/ogdoada_deep_focus.mp3"
            else -> null
        }

    val playsInApp: Boolean
        get() = assetName != null || this == FromFile

    companion object {
        private val LEGACY_BUILTIN = setOf(
            "SufiMorning",
            "SufiBreath",
            "SufiRiver",
            "SufiEvening",
            "SufiWhirl",
            "BuiltIn",
        )

        fun fromStored(value: String?): PracticeMelody = when {
            value == null || value in LEGACY_BUILTIN -> BuiltIn
            else -> entries.firstOrNull { it.name == value } ?: BuiltIn
        }
    }
}
