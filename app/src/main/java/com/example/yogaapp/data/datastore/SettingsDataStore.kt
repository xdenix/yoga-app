package com.example.yogaapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences DataStore for app-level settings.
 *
 * Persisted keys:
 *  - themeMode: System / Light / Dark / DustyRose (default: DustyRose)
 *  - dynamicColor: Material You colours on Android 12+ (default: false)
 *  - onboardingShown: flag set after the user picks a level on first launch
 *  - chosenLevel: Beginner / Intermediate / Advanced (drives program generation)
 *  - audioMode: Off / Male1 / Male2 / Male3 (default: Male1)
 *  - customMusicUri: content:// URI for [PracticeMelody.FromFile]
 *  - Block 1 (30-day program): posesPerDay + globalDurationSec
 *  - Block 2 (Weight-loss yoga): weightLossPosesCount + weightLossDurationSec
 *  - Block 3 (Strength block): strengthPosesCount + strengthDurationSec
 *  - pauseBetweenSec: rest seconds between poses in a session (default: 10)
 */
private val Context.yogaSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "yoga_app_settings",
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val store get() = context.yogaSettingsDataStore

    val themeMode: Flow<ThemeMode> = store.data.map { p ->
        when (p[KEY_THEME_MODE]) {
            ThemeMode.Light.name -> ThemeMode.Light
            ThemeMode.System.name -> ThemeMode.System
            ThemeMode.Dark.name -> ThemeMode.Dark
            ThemeMode.DustyRose.name -> ThemeMode.DustyRose
            else -> ThemeMode.DustyRose
        }
    }

    val dynamicColor: Flow<Boolean> = store.data.map { p -> p[KEY_DYNAMIC_COLOR] ?: false }

    val onboardingShown: Flow<Boolean> = store.data.map { p -> p[KEY_ONBOARDING_SHOWN] ?: false }

    val chosenLevel: Flow<YogaLevel?> = store.data.map { p ->
        p[KEY_LEVEL]?.let { runCatching { YogaLevel.valueOf(it) }.getOrNull() }
    }

    val audioMode: Flow<AudioMode> = store.data.map { p ->
        AudioMode.fromStored(p[KEY_AUDIO_MODE])
    }

    /** Block 1 (30-day plan): exercise duration in seconds. */
    val globalDurationSec: Flow<Int> = store.data.map { p ->
        (p[KEY_GLOBAL_DURATION] ?: DEFAULT_GLOBAL_DURATION).coerceIn(MIN_DURATION, MAX_DURATION)
    }

    /** Block 1 (30-day plan): exercises per day. */
    val posesPerDay: Flow<Int> = store.data.map { p ->
        (p[KEY_POSES_PER_DAY] ?: YogaRules.DEFAULT_POSES_PER_DAY)
            .coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)
    }

    /** Block 2 (Weight-loss yoga): exercises count. */
    val weightLossPosesCount: Flow<Int> = store.data.map { p ->
        (p[KEY_WEIGHT_LOSS_POSES] ?: DEFAULT_WEIGHT_LOSS_POSES)
            .coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)
    }

    /** Block 2 (Weight-loss yoga): exercise duration in seconds. */
    val weightLossDurationSec: Flow<Int> = store.data.map { p ->
        (p[KEY_WEIGHT_LOSS_DURATION] ?: DEFAULT_WEIGHT_LOSS_DURATION)
            .coerceIn(MIN_DURATION, MAX_DURATION)
    }

    /** Block 3 (Strength): exercises count. */
    val strengthPosesCount: Flow<Int> = store.data.map { p ->
        (p[KEY_STRENGTH_POSES] ?: DEFAULT_STRENGTH_POSES)
            .coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)
    }

    /** Block 3 (Strength): exercise duration in seconds. */
    val strengthDurationSec: Flow<Int> = store.data.map { p ->
        (p[KEY_STRENGTH_DURATION] ?: DEFAULT_STRENGTH_DURATION)
            .coerceIn(MIN_DURATION, MAX_DURATION)
    }

    val pauseBetweenSec: Flow<Int> = store.data.map { p ->
        (p[KEY_PAUSE_BETWEEN] ?: YogaRules.DEFAULT_PAUSE_BETWEEN_SEC)
            .coerceIn(YogaRules.MIN_PAUSE_SEC, YogaRules.MAX_PAUSE_SEC)
    }

    val audioPackEnabled: Flow<Boolean> = store.data.map { p -> p[KEY_AUDIO_PACK_ENABLED] ?: false }

    val practiceMelody: Flow<PracticeMelody> = store.data.map { p ->
        PracticeMelody.fromStored(p[KEY_MELODY])
    }

    val customMusicUri: Flow<String?> = store.data.map { p ->
        p[KEY_CUSTOM_MUSIC_URI]?.takeIf { it.isNotBlank() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun markOnboardingShown() {
        store.edit { it[KEY_ONBOARDING_SHOWN] = true }
    }

    suspend fun setChosenLevel(level: YogaLevel) {
        store.edit { it[KEY_LEVEL] = level.name }
    }

    suspend fun setAudioMode(mode: AudioMode) {
        store.edit { it[KEY_AUDIO_MODE] = mode.name }
    }

    suspend fun setGlobalDuration(sec: Int) {
        store.edit { it[KEY_GLOBAL_DURATION] = sec.coerceIn(MIN_DURATION, MAX_DURATION) }
    }

    suspend fun setPosesPerDay(count: Int) {
        store.edit {
            it[KEY_POSES_PER_DAY] = count.coerceIn(
                YogaRules.MIN_POSES_PER_DAY,
                YogaRules.MAX_POSES_PER_DAY,
            )
        }
    }

    suspend fun setWeightLossPosesCount(count: Int) {
        store.edit {
            it[KEY_WEIGHT_LOSS_POSES] = count.coerceIn(
                YogaRules.MIN_POSES_PER_DAY,
                YogaRules.MAX_POSES_PER_DAY,
            )
        }
    }

    suspend fun setWeightLossDuration(sec: Int) {
        store.edit { it[KEY_WEIGHT_LOSS_DURATION] = sec.coerceIn(MIN_DURATION, MAX_DURATION) }
    }

    suspend fun setStrengthPosesCount(count: Int) {
        store.edit {
            it[KEY_STRENGTH_POSES] = count.coerceIn(
                YogaRules.MIN_POSES_PER_DAY,
                YogaRules.MAX_POSES_PER_DAY,
            )
        }
    }

    suspend fun setStrengthDuration(sec: Int) {
        store.edit { it[KEY_STRENGTH_DURATION] = sec.coerceIn(MIN_DURATION, MAX_DURATION) }
    }

    suspend fun setPauseBetweenSec(sec: Int) {
        store.edit {
            it[KEY_PAUSE_BETWEEN] = sec.coerceIn(YogaRules.MIN_PAUSE_SEC, YogaRules.MAX_PAUSE_SEC)
        }
    }

    suspend fun setAudioPackEnabled(enabled: Boolean) {
        store.edit { it[KEY_AUDIO_PACK_ENABLED] = enabled }
    }

    suspend fun setPracticeMelody(melody: PracticeMelody) {
        store.edit { it[KEY_MELODY] = melody.name }
    }

    suspend fun setCustomMusicUri(uri: String?) {
        store.edit {
            if (uri.isNullOrBlank()) it.remove(KEY_CUSTOM_MUSIC_URI)
            else it[KEY_CUSTOM_MUSIC_URI] = uri
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
        private val KEY_LEVEL = stringPreferencesKey("level")
        private val KEY_AUDIO_MODE = stringPreferencesKey("audio_mode")
        private val KEY_GLOBAL_DURATION = intPreferencesKey("global_duration")
        private val KEY_POSES_PER_DAY = intPreferencesKey("poses_per_day")
        private val KEY_WEIGHT_LOSS_POSES = intPreferencesKey("weight_loss_poses_count")
        private val KEY_WEIGHT_LOSS_DURATION = intPreferencesKey("weight_loss_duration_sec")
        private val KEY_STRENGTH_POSES = intPreferencesKey("strength_poses_count")
        private val KEY_STRENGTH_DURATION = intPreferencesKey("strength_duration_sec")
        private val KEY_PAUSE_BETWEEN = intPreferencesKey("pause_between_sec")
        private val KEY_AUDIO_PACK_ENABLED = booleanPreferencesKey("audio_pack_enabled")
        private val KEY_MELODY = stringPreferencesKey("practice_melody")
        private val KEY_CUSTOM_MUSIC_URI = stringPreferencesKey("custom_music_uri")

        const val DEFAULT_GLOBAL_DURATION = 45
        const val DEFAULT_WEIGHT_LOSS_POSES = 15
        const val DEFAULT_WEIGHT_LOSS_DURATION = 45
        const val DEFAULT_STRENGTH_POSES = 15
        const val DEFAULT_STRENGTH_DURATION = 40
        const val MIN_DURATION = 10
        const val MAX_DURATION = 300
    }
}

/**
 * Voice narration mode: Off or one of 3 distinct Russian male voice profiles.
 */
enum class AudioMode(
    val bundledPitch: Float,
    val bundledSpeed: Float,
    val ttsPitch: Float,
    val ttsRate: Float,
) {
    Off(1.0f, 1.0f, 1.0f, 1.0f),
    /** Мужской 1 — спокойный баритон (по умолчанию). */
    Male1(bundledPitch = 1.00f, bundledSpeed = 1.00f, ttsPitch = 0.78f, ttsRate = 0.92f),
    /** Мужской 2 — глубокий бас (наставник). */
    Male2(bundledPitch = 0.87f, bundledSpeed = 0.94f, ttsPitch = 0.64f, ttsRate = 0.86f),
    /** Мужской 3 — бодрый тренер. */
    Male3(bundledPitch = 1.11f, bundledSpeed = 1.06f, ttsPitch = 0.90f, ttsRate = 1.02f),
    ;

    companion object {
        val voiceVariants: List<AudioMode> = listOf(Male1, Male2, Male3)

        fun fromStored(raw: String?): AudioMode = when (raw) {
            Off.name -> Off
            Male2.name -> Male2
            Male3.name -> Male3
            // Migrate legacy Tts / Mp3Pack / null to default Male1
            else -> Male1
        }
    }
}
