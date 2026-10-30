package com.example.yogaapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.data.audio.PracticeMelodyPlayer
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val melodyPlayer: PracticeMelodyPlayer,
    private val resetProgressUseCase: ResetProgressUseCase,
    private val createProgram: CreateProgramUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private data class CoreSettings(
        val themeMode: ThemeMode,
        val dynamicColor: Boolean,
        val audioMode: AudioMode,
        val melody: PracticeMelody,
        val customUri: String?,
    )

    private data class BlockSettings(
        val posesPerDay: Int,
        val globalDurationSec: Int,
        val weightLossPosesCount: Int,
        val weightLossDurationSec: Int,
        val strengthPosesCount: Int,
        val strengthDurationSec: Int,
        val pauseBetweenSec: Int,
    )

    private val coreFlow = combine(
        settingsDataStore.themeMode,
        settingsDataStore.dynamicColor,
        settingsDataStore.audioMode,
        settingsDataStore.practiceMelody,
        settingsDataStore.customMusicUri,
    ) { themeMode, dynamicColor, audioMode, melody, customUri ->
        CoreSettings(themeMode, dynamicColor, audioMode, melody, customUri)
    }

    private val blocksFlow = combine(
        combine(
            settingsDataStore.posesPerDay,
            settingsDataStore.globalDurationSec,
            settingsDataStore.weightLossPosesCount,
            settingsDataStore.weightLossDurationSec,
        ) { pCount, pDur, wlCount, wlDur ->
            intArrayOf(pCount, pDur, wlCount, wlDur)
        },
        settingsDataStore.strengthPosesCount,
        settingsDataStore.strengthDurationSec,
        settingsDataStore.pauseBetweenSec,
    ) { firstFour, strCount, strDur, pauseBetween ->
        BlockSettings(
            posesPerDay = firstFour[0],
            globalDurationSec = firstFour[1],
            weightLossPosesCount = firstFour[2],
            weightLossDurationSec = firstFour[3],
            strengthPosesCount = strCount,
            strengthDurationSec = strDur,
            pauseBetweenSec = pauseBetween,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        coreFlow,
        blocksFlow,
    ) { core, blocks ->
        SettingsUiState(
            themeMode = core.themeMode,
            dynamicColor = core.dynamicColor,
            audioMode = core.audioMode,
            posesPerDay = blocks.posesPerDay,
            globalDurationSec = blocks.globalDurationSec,
            weightLossPosesCount = blocks.weightLossPosesCount,
            weightLossDurationSec = blocks.weightLossDurationSec,
            strengthPosesCount = blocks.strengthPosesCount,
            strengthDurationSec = blocks.strengthDurationSec,
            pauseBetweenSec = blocks.pauseBetweenSec,
            hasAudioPack = audioPlayer.hasAudioPack,
            melody = core.melody,
            customMusicUri = core.customUri,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var previewJob: Job? = null

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
    }

    fun setAudioMode(mode: AudioMode) {
        viewModelScope.launch {
            settingsDataStore.setAudioMode(mode)
            if (mode != AudioMode.Off) {
                audioPlayer.speakCountdown(5)
            } else {
                audioPlayer.stop()
            }
        }
    }

    /** Toggles pose narration independently from background music. */
    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val current = settingsDataStore.audioMode.first()
                if (current == AudioMode.Off) {
                    settingsDataStore.setAudioMode(AudioMode.Male1)
                }
                audioPlayer.speakCountdown(5)
            } else {
                audioPlayer.stop()
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

    fun setWeightLossPosesCount(count: Int) {
        viewModelScope.launch { settingsDataStore.setWeightLossPosesCount(count) }
    }

    fun setWeightLossDuration(sec: Int) {
        viewModelScope.launch { settingsDataStore.setWeightLossDuration(sec) }
    }

    fun setStrengthPosesCount(count: Int) {
        viewModelScope.launch { settingsDataStore.setStrengthPosesCount(count) }
    }

    fun setStrengthDuration(sec: Int) {
        viewModelScope.launch { settingsDataStore.setStrengthDuration(sec) }
    }

    fun setPauseBetweenSec(sec: Int) {
        viewModelScope.launch { settingsDataStore.setPauseBetweenSec(sec) }
    }

    fun setMelody(melody: PracticeMelody) {
        viewModelScope.launch {
            settingsDataStore.setPracticeMelody(melody)
            when (melody) {
                PracticeMelody.YandexMusic -> {
                    stopPreview()
                    _effects.send(SettingsEffect.OpenYandexMusic)
                }
                PracticeMelody.External -> {
                    stopPreview()
                    _effects.send(SettingsEffect.OpenSystemMusic)
                }
                PracticeMelody.FromFile -> {
                    val uriStr = settingsDataStore.customMusicUri.first()
                    if (uriStr.isNullOrBlank()) {
                        stopPreview()
                        _effects.send(SettingsEffect.PickMusicFile)
                    } else {
                        startPreview(melody, Uri.parse(uriStr))
                    }
                }
                PracticeMelody.Off -> stopPreview()
                else -> startPreview(melody, null)
            }
        }
    }

    fun setCustomMusicUri(uri: String?) {
        viewModelScope.launch {
            settingsDataStore.setCustomMusicUri(uri)
            if (!uri.isNullOrBlank()) {
                settingsDataStore.setPracticeMelody(PracticeMelody.FromFile)
                startPreview(PracticeMelody.FromFile, Uri.parse(uri))
            }
        }
    }

    private fun startPreview(melody: PracticeMelody, customUri: Uri?) {
        previewJob?.cancel()
        if (!melody.playsInApp) {
            melodyPlayer.stop()
            return
        }
        previewJob = viewModelScope.launch {
            melodyPlayer.play(melody, customUri)
            delay(MELODY_PREVIEW_MS)
            melodyPlayer.stop()
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        melodyPlayer.stop()
    }

    fun resetProgress() {
        viewModelScope.launch(dispatchers.io) {
            when (val r = resetProgressUseCase()) {
                is Result.Success -> _effects.send(SettingsEffect.Snackbar(com.example.yogaapp.R.string.action_done))
                is Result.Failure -> _effects.send(SettingsEffect.Error(messageResFor(r.error)))
            }
        }
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val MELODY_PREVIEW_MS = 10_000L

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
    val audioMode: AudioMode = AudioMode.Male1,
    val posesPerDay: Int = YogaRules.DEFAULT_POSES_PER_DAY,
    val globalDurationSec: Int = YogaRules.DEFAULT_GLOBAL_DURATION_SEC,
    val weightLossPosesCount: Int = SettingsDataStore.DEFAULT_WEIGHT_LOSS_POSES,
    val weightLossDurationSec: Int = SettingsDataStore.DEFAULT_WEIGHT_LOSS_DURATION,
    val strengthPosesCount: Int = SettingsDataStore.DEFAULT_STRENGTH_POSES,
    val strengthDurationSec: Int = SettingsDataStore.DEFAULT_STRENGTH_DURATION,
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
