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
 *  - themeMode: System / Light / Dark (default: Dark for meditative feel)
 *  - dynamicColor: Material You colours on Android 12+ (default: false — we want our custom palette)
 *  - onboardingShown: flag set after the user picks a level on first launch
 *  - chosenLevel: Beginner / Intermediate / Advanced (drives program generation)
 *  - audioMode: Off / TTS / MP3_PACK (default: TTS)
 *  - customMusicUri: content:// URI for [PracticeMelody.FromFile]
 *  - globalDurationSec: default duration applied to all poses when not overridden (default: 30)
 *  - posesPerDay: how many random poses each program day gets (default: 10)
 *  - pauseBetweenSec: rest seconds between poses in a session (default: 10)
 *  - audioPackEnabled: whether to use the optional MP3 pack when present (default: false)
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
            else -> ThemeMode.DustyRose // default: Dusty Rose
        }
    }

    val dynamicColor: Flow<Boolean> = store.data.map { p -> p[KEY_DYNAMIC_COLOR] ?: false }

    val onboardingShown: Flow<Boolean> = store.data.map { p -> p[KEY_ONBOARDING_SHOWN] ?: false }

    val chosenLevel: Flow<YogaLevel?> = store.data.map { p ->
        p[KEY_LEVEL]?.let { runCatching { YogaLevel.valueOf(it) }.getOrNull() }
    }

    val audioMode: Flow<AudioMode> = store.data.map { p ->
        when (p[KEY_AUDIO_MODE]) {
            AudioMode.Off.name -> AudioMode.Off
            AudioMode.Mp3Pack.name -> AudioMode.Mp3Pack
            else -> AudioMode.Tts
        }
    }

    val globalDurationSec: Flow<Int> = store.data.map { p ->
        p[KEY_GLOBAL_DURATION] ?: DEFAULT_GLOBAL_DURATION
    }

    val posesPerDay: Flow<Int> = store.data.map { p ->
        (p[KEY_POSES_PER_DAY] ?: YogaRules.DEFAULT_POSES_PER_DAY)
            .coerceIn(YogaRules.MIN_POSES_PER_DAY, YogaRules.MAX_POSES_PER_DAY)
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
        private val KEY_PAUSE_BETWEEN = intPreferencesKey("pause_between_sec")
        private val KEY_AUDIO_PACK_ENABLED = booleanPreferencesKey("audio_pack_enabled")
        private val KEY_MELODY = stringPreferencesKey("practice_melody")
        private val KEY_CUSTOM_MUSIC_URI = stringPreferencesKey("custom_music_uri")

        const val DEFAULT_GLOBAL_DURATION = 30
        const val MIN_DURATION = 10
        const val MAX_DURATION = 300
    }
}

enum class AudioMode { Off, Tts, Mp3Pack }
