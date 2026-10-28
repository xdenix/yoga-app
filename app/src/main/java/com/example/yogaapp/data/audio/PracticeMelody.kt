package com.example.yogaapp.data.audio

/**
 * Фоновые мелодии практики. Файлы — короткие зацикленные WAV в assets,
 * написанные для приложения (не сторонние записи).
 */
enum class PracticeMelody {
    Off,
    Morning,
    Breath,
    River,
    Evening,
    ;

    val assetName: String?
        get() = when (this) {
            Off -> null
            Morning -> "melodies/morning.wav"
            Breath -> "melodies/breath.wav"
            River -> "melodies/river.wav"
            Evening -> "melodies/evening.wav"
        }

    companion object {
        fun fromStored(value: String?): PracticeMelody =
            entries.firstOrNull { it.name == value } ?: Morning
    }
}
