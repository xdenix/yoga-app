package com.example.yogaapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.data.datastore.AudioMode
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.data.datastore.ThemeMode
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val audioPlayer: PoseAudioPlayer,
    private val resetProgressUseCase: ResetProgressUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.themeMode,
        settingsDataStore.dynamicColor,
        settingsDataStore.audioMode,
        settingsDataStore.globalDurationSec,
        settingsDataStore.practiceMelody,
    ) { themeMode, dynamicColor, audioMode, globalDuration, melody ->
        SettingsUiState(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            audioMode = audioMode,
            globalDurationSec = globalDuration,
            hasAudioPack = audioPlayer.hasAudioPack,
            melody = melody,
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

    fun setGlobalDuration(sec: Int) {
        viewModelScope.launch { settingsDataStore.setGlobalDuration(sec) }
    }

    fun setMelody(melody: PracticeMelody) {
        viewModelScope.launch { settingsDataStore.setPracticeMelody(melody) }
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
    val themeMode: ThemeMode = ThemeMode.Dark,
    val dynamicColor: Boolean = false,
    val audioMode: AudioMode = AudioMode.Tts,
    val globalDurationSec: Int = 30,
    val hasAudioPack: Boolean = false,
    val melody: PracticeMelody = PracticeMelody.SufiMorning,
)

sealed interface SettingsEffect {
    data class Snackbar(val messageRes: Int) : SettingsEffect
    data class Error(val messageRes: Int) : SettingsEffect
}
