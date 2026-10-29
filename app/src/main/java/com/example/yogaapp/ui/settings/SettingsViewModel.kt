package com.example.yogaapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.data.datastore.AudioMode
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.data.datastore.ThemeMode
import com.example.yogaapp.domain.model.YogaRules
import com.example.yogaapp.domain.usecase.CreateProgramUseCase
import com.example.yogaapp.domain.usecase.ResetProgressUseCase
import com.example.yogaapp.domain.util.AppDispatchers
import com.example.yogaapp.domain.util.AppError
import com.example.yogaapp.domain.util.ErrorKind
import com.example.yogaapp.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val audioPlayer: PoseAudioPlayer,
    private val resetProgressUseCase: ResetProgressUseCase,
    private val createProgram: CreateProgramUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private data class CoreSettings(
        val themeMode: ThemeMode,
        val dynamicColor: Boolean,
        val audioMode: AudioMode,
        val globalDurationSec: Int,
        val melody: PracticeMelody,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsDataStore.themeMode,
            settingsDataStore.dynamicColor,
            settingsDataStore.audioMode,
            settingsDataStore.globalDurationSec,
            settingsDataStore.practiceMelody,
        ) { themeMode, dynamicColor, audioMode, globalDuration, melody ->
            CoreSettings(themeMode, dynamicColor, audioMode, globalDuration, melody)
        },
        settingsDataStore.posesPerDay,
        settingsDataStore.pauseBetweenSec,
        settingsDataStore.customMusicUri,
    ) { core, posesPerDay, pauseBetween, customUri ->
        SettingsUiState(
            themeMode = core.themeMode,
            dynamicColor = core.dynamicColor,
            audioMode = core.audioMode,
            globalDurationSec = core.globalDurationSec,
            posesPerDay = posesPerDay,
            pauseBetweenSec = pauseBetween,
            hasAudioPack = audioPlayer.hasAudioPack,
            melody = core.melody,
            customMusicUri = customUri,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
    }

    fun setAudioMode(mode: AudioMode) {
        viewModelScope.launch { settingsDataStore.setAudioMode(mode) }
    }

    /** Toggles pose narration independently from background music. */
    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val current = settingsDataStore.audioMode.first()
                if (current == AudioMode.Off) {
                    settingsDataStore.setAudioMode(AudioMode.Tts)
                }
            } else {
                settingsDataStore.setAudioMode(AudioMode.Off)
            }
        }
    }

    fun setGlobalDuration(sec: Int) {
        viewModelScope.launch { settingsDataStore.setGlobalDuration(sec) }
    }

    fun setPosesPerDay(count: Int) {
        viewModelScope.launch(dispatchers.io) {
            settingsDataStore.setPosesPerDay(count)
            val level = settingsDataStore.chosenLevel.first() ?: return@launch
            createProgram(level, count)
            _effects.send(SettingsEffect.Snackbar(com.example.yogaapp.R.string.settings_plan_regenerated))
        }
    }

    fun setPauseBetweenSec(sec: Int) {
        viewModelScope.launch { settingsDataStore.setPauseBetweenSec(sec) }
    }

    fun setMelody(melody: PracticeMelody) {
        viewModelScope.launch {
            settingsDataStore.setPracticeMelody(melody)
            when (melody) {
                PracticeMelody.YandexMusic -> _effects.send(SettingsEffect.OpenYandexMusic)
                PracticeMelody.External -> _effects.send(SettingsEffect.OpenSystemMusic)
                PracticeMelody.FromFile -> {
                    if (settingsDataStore.customMusicUri.first().isNullOrBlank()) {
                        _effects.send(SettingsEffect.PickMusicFile)
                    }
                }
                else -> Unit
            }
        }
    }

    fun setCustomMusicUri(uri: String?) {
        viewModelScope.launch {
            settingsDataStore.setCustomMusicUri(uri)
            if (!uri.isNullOrBlank()) {
                settingsDataStore.setPracticeMelody(PracticeMelody.FromFile)
            }
        }
    }

    fun resetProgress() {
        viewModelScope.launch(dispatchers.io) {
            when (val r = resetProgressUseCase()) {
                is Result.Success -> _effects.send(SettingsEffect.Snackbar(com.example.yogaapp.R.string.action_done))
                is Result.Failure -> _effects.send(SettingsEffect.Error(messageResFor(r.error)))
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun messageResFor(error: AppError): Int = when (error.kind) {
            ErrorKind.Database -> com.example.yogaapp.R.string.error_database
            ErrorKind.Network -> com.example.yogaapp.R.string.error_network
            ErrorKind.NotFound -> com.example.yogaapp.R.string.error_not_found
            ErrorKind.Validation -> com.example.yogaapp.R.string.error_validation
            ErrorKind.Unknown -> com.example.yogaapp.R.string.error_unknown
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DustyRose,
    val dynamicColor: Boolean = false,
    val audioMode: AudioMode = AudioMode.Tts,
    val globalDurationSec: Int = YogaRules.DEFAULT_GLOBAL_DURATION_SEC,
    val posesPerDay: Int = YogaRules.DEFAULT_POSES_PER_DAY,
    val pauseBetweenSec: Int = YogaRules.DEFAULT_PAUSE_BETWEEN_SEC,
    val hasAudioPack: Boolean = false,
    val melody: PracticeMelody = PracticeMelody.BuiltIn,
    val customMusicUri: String? = null,
)

sealed interface SettingsEffect {
    data class Snackbar(val messageRes: Int) : SettingsEffect
    data class Error(val messageRes: Int) : SettingsEffect
    data object PickMusicFile : SettingsEffect
    data object OpenYandexMusic : SettingsEffect
    data object OpenSystemMusic : SettingsEffect
}
