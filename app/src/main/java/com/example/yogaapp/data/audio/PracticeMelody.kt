package com.example.yogaapp.data.audio

/**
 * Источник фоновой музыки для практики.
 *
 * [BuiltIn] — обрезанный суфийский трек в `assets/melodies/sufi_dervish.mp3`.
 * [FromFile] — файл, выбранный пользователем (URI в DataStore).
 * [YandexMusic] / [External] — музыка извне: приложение не играет свой трек.
 */
enum class PracticeMelody {
    Off,
    BuiltIn,
    FromFile,
    YandexMusic,
    External,
    ;

    val playsInApp: Boolean
        get() = this == BuiltIn || this == FromFile

    val assetName: String?
        get() = when (this) {
            BuiltIn -> "melodies/sufi_dervish.mp3"
            else -> null
        }

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
