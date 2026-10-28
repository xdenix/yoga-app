package com.example.yogaapp.data.audio

// Background practice melodies: five Sufi tracks under assets/melodies (mp3).
// Source: Freesound CC0 (nesibe 171927, 171928, 172368, 183938, 183939).
enum class PracticeMelody {
    Off,
    SufiMorning,
    SufiBreath,
    SufiRiver,
    SufiEvening,
    SufiWhirl,
    ;

    val assetName: String?
        get() = when (this) {
            Off -> null
            SufiMorning -> "melodies/sufi_morning.mp3"
            SufiBreath -> "melodies/sufi_breath.mp3"
            SufiRiver -> "melodies/sufi_river.mp3"
            SufiEvening -> "melodies/sufi_evening.mp3"
            SufiWhirl -> "melodies/sufi_whirl.mp3"
        }

    companion object {
        fun fromStored(value: String?): PracticeMelody =
            entries.firstOrNull { it.name == value } ?: SufiMorning
    }
}
