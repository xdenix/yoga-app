package com.example.yogaapp.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.data.audio.PracticeMelodyPlayer
import com.example.yogaapp.data.datastore.AudioMode
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.PoseSide
import com.example.yogaapp.domain.model.WorkoutBlocks
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.repository.YogaRepository
import com.example.yogaapp.domain.usecase.RecordCompletionUseCase
import com.example.yogaapp.domain.usecase.SessionTimerUseCase
import com.example.yogaapp.domain.util.AppDispatchers
import com.example.yogaapp.domain.util.AppError
import com.example.yogaapp.domain.util.ErrorKind
import com.example.yogaapp.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: YogaRepository,
    private val settingsDataStore: SettingsDataStore,
    private val audioPlayer: PoseAudioPlayer,
    private val melodyPlayer: PracticeMelodyPlayer,
    private val recordCompletion: RecordCompletionUseCase,
    private val dispatchers: AppDispatchers,
    handle: SavedStateHandle,
) : ViewModel() {

    private val dayId: String = handle.get<String>("dayId").orEmpty()
    private val timer = SessionTimerUseCase(dispatchers)

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SessionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _melody = MutableStateFlow(PracticeMelody.BuiltIn)
    val melody: StateFlow<PracticeMelody> = _melody.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private var sessionActive = false
    private var userPaused = false
    private var currentMelody = PracticeMelody.BuiltIn
    private var customMusicUri: Uri? = null

    init {
        viewModelScope.launch {
            settingsDataStore.audioMode.collect { mode ->
                _voiceEnabled.value = mode != AudioMode.Off
                if (mode == AudioMode.Off) audioPlayer.stop()
            }
        }
        viewModelScope.launch {
            combine(
                settingsDataStore.practiceMelody,
                settingsDataStore.customMusicUri,
            ) { selected, uri -> selected to uri }
                .collect { (selected, uri) ->
                    currentMelody = selected
                    customMusicUri = uri?.let(Uri::parse)
                    _melody.value = selected
                    if (sessionActive && !userPaused && _uiState.value !is SessionUiState.Finished) {
                        syncMelodyPlayback(selected)
                    }
                }
        }
        loadSession()
    }

    private fun syncMelodyPlayback(melody: PracticeMelody) {
        if (melody.playsInApp) {
            melodyPlayer.play(melody, customMusicUri)
        } else {
            melodyPlayer.stop()
        }
    }

    private fun loadSession() {
        viewModelScope.launch(dispatchers.io) {
            val program = repository.observeProgram().first()
            val customDays = repository.observeCustomDays().first()
            val wlCount = settingsDataStore.weightLossPosesCount.first()
            val strCount = settingsDataStore.strengthPosesCount.first()
            val day = program?.days?.firstOrNull { it.id == dayId }
                ?: customDays.firstOrNull { it.id == dayId }
                ?: WorkoutBlocks.byId(dayId, weightLossCount = wlCount, strengthCount = strCount)
            if (day == null) {
                sessionActive = false
                melodyPlayer.stop()
                _uiState.value = SessionUiState.Error(messageResFor(AppError.NotFound(what = "day:$dayId")))
                return@launch
            }
            val allPoses = repository.allPoses()
            val poseMap = allPoses.associateBy { it.id }
            val resolvedPoses: List<Pair<YogaPose, DayPoseEntry>> = day.poses.mapNotNull { entry ->
                poseMap[entry.poseId]?.let { pose -> pose to entry }
            }
            val blockDuration = when (dayId) {
                WorkoutBlocks.WEIGHT_LOSS_ID -> settingsDataStore.weightLossDurationSec.first()
                WorkoutBlocks.STRENGTH_ID -> settingsDataStore.strengthDurationSec.first()
                else -> settingsDataStore.globalDurationSec.first()
            }
            val pauseBetween = settingsDataStore.pauseBetweenSec.first()
            timer.start(
                resolvedPoses,
                globalDurationSec = blockDuration,
                pauseBetweenSec = pauseBetween,
            )
            collectTimerState()
            // Start cue for the first pose (collector also tracks index to avoid a double speak).
            lastSpokenIndex = -1
            lastCountdownSec = -1
            wasRestPause = false
            wasFinished = false
            completionRecorded = false
            sessionActive = true
            if (!userPaused) syncMelodyPlayback(currentMelody)
        }
    }

    private fun poseForSpeech(pose: YogaPose, side: PoseSide): YogaPose = when (side) {
        PoseSide.Right -> pose.copy(
            ruName = "${pose.ruName} (справа)",
            ruDescription = "Правая сторона. ${pose.ruDescription}",
            enName = "${pose.enName} (right)",
            enDescription = "Right side. ${pose.enDescription}",
        )
        PoseSide.Left -> pose.copy(
            ruName = "${pose.ruName} (слева)",
            ruDescription = "Левая сторона. ${pose.ruDescription}",
            enName = "${pose.enName} (left)",
            enDescription = "Left side. ${pose.enDescription}",
        )
        PoseSide.None -> pose
    }

    private fun collectTimerState() {
        viewModelScope.launch {
            timer.state.collect { timerState ->
                _uiState.value = if (timerState.isFinished) {
                    melodyPlayer.pause()
                    SessionUiState.Finished
                } else {
                    SessionUiState.Running(
                        current = timerState.current,
                        currentIndex = timerState.currentIndex,
                        total = timerState.poses.size,
                        remainingSec = timerState.remaining,
                        elapsedSec = timerState.elapsedSec,
                        totalSec = timerState.totalDurationSec,
                        isPaused = !timerState.isRunning,
                        isRestPause = timerState.isPause,
                    )
                }

                // End-of-exercise bell: pose → rest pause, or last pose → finished.
                if (!wasRestPause && timerState.isPause) {
                    audioPlayer.playGong()
                    lastCountdownSec = -1
                }
                if (!wasFinished && timerState.isFinished) {
                    audioPlayer.playGong()
                    recordSessionOnce(timerState.elapsedSec)
                }
                wasRestPause = timerState.isPause
                wasFinished = timerState.isFinished

                // Start-of-exercise bell + description (not during rest pause).
                val currentPose = timerState.current
                if (currentPose != null && !timerState.isPause && !timerState.isFinished) {
                    if (lastSpokenIndex != timerState.currentIndex) {
                        lastSpokenIndex = timerState.currentIndex
                        lastCountdownSec = -1
                        audioPlayer.playGong()
                        audioPlayer.playDescription(
                            poseForSpeech(currentPose.pose, currentPose.entry.side),
                        )
                    }
                }

                // Voice the last 5 seconds of an exercise or a rest pause.
                if (timerState.isRunning && !timerState.isFinished) {
                    val rem = timerState.remaining
                    if (rem in 1..5 && rem != lastCountdownSec) {
                        lastCountdownSec = rem
                        audioPlayer.speakCountdown(rem)
                    } else if (rem > 5) {
                        lastCountdownSec = -1
                    }
                }
            }
        }
    }

    private var lastSpokenIndex = -1
    private var lastCountdownSec = -1
    private var wasRestPause = false
    private var wasFinished = false
    private var completionRecorded = false

    private fun recordSessionOnce(elapsedSec: Int) {
        if (completionRecorded) return
        completionRecorded = true
        viewModelScope.launch(dispatchers.io) {
            recordCompletion(dayId, elapsedSec)
        }
    }

    fun onEvent(event: SessionEvent) {
        when (event) {
            SessionEvent.Pause -> {
                userPaused = true
                timer.pause()
                melodyPlayer.pause()
            }
            SessionEvent.Resume -> {
                userPaused = false
                timer.resume()
                syncMelodyPlayback(currentMelody)
            }
            SessionEvent.Skip -> timer.skip()
            SessionEvent.Previous -> timer.previous()
            SessionEvent.ListenAgain -> {
                viewModelScope.launch {
                    val current = timer.state.value.current ?: return@launch
                    audioPlayer.playDescription(poseForSpeech(current.pose, current.entry.side))
                }
            }
            SessionEvent.StopAudio -> audioPlayer.stop()
            SessionEvent.Complete -> viewModelScope.launch(dispatchers.io) {
                if (!completionRecorded) {
                    completionRecorded = true
                    val elapsed = timer.state.value.elapsedSec
                    when (val r = recordCompletion(dayId, elapsed)) {
                        is Result.Success -> _effects.send(SessionEffect.Completed)
                        is Result.Failure -> _effects.send(SessionEffect.Error(messageResFor(r.error)))
                    }
                } else {
                    _effects.send(SessionEffect.Completed)
                }
            }
            SessionEvent.Finish -> viewModelScope.launch { _effects.send(SessionEffect.Close) }
        }
    }

    fun setMelody(melody: PracticeMelody) {
        viewModelScope.launch {
            settingsDataStore.setPracticeMelody(melody)
            when (melody) {
                PracticeMelody.YandexMusic -> _effects.send(SessionEffect.OpenYandexMusic)
                PracticeMelody.External -> _effects.send(SessionEffect.OpenSystemMusic)
                else -> Unit
            }
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (settingsDataStore.audioMode.first() == AudioMode.Off) {
                    settingsDataStore.setAudioMode(AudioMode.Male1)
                }
            } else {
                audioPlayer.stop()
                settingsDataStore.setAudioMode(AudioMode.Off)
            }
        }
    }

    override fun onCleared() {
        timer.cancel()
        audioPlayer.stop()
        melodyPlayer.stop()
        super.onCleared()
    }

    companion object {
        fun messageResFor(error: AppError): Int = when (error.kind) {
            ErrorKind.Database -> com.example.yogaapp.R.string.error_database
            ErrorKind.Network -> com.example.yogaapp.R.string.error_network
            ErrorKind.NotFound -> com.example.yogaapp.R.string.error_not_found
            ErrorKind.Validation -> com.example.yogaapp.R.string.error_validation
            ErrorKind.Unknown -> com.example.yogaapp.R.string.error_unknown
        }
    }
}

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Running(
        val current: com.example.yogaapp.domain.usecase.SessionPose?,
        val currentIndex: Int,
        val total: Int,
        val remainingSec: Int,
        val elapsedSec: Int,
        val totalSec: Int,
        val isPaused: Boolean,
        val isRestPause: Boolean = false,
    ) : SessionUiState
    data object Finished : SessionUiState
    data class Error(val messageRes: Int) : SessionUiState
}

sealed interface SessionEvent {
    data object Pause : SessionEvent
    data object Resume : SessionEvent
    data object Skip : SessionEvent
    data object Previous : SessionEvent
    data object ListenAgain : SessionEvent
    data object StopAudio : SessionEvent
    data object Complete : SessionEvent
    data object Finish : SessionEvent
}

sealed interface SessionEffect {
    data object Completed : SessionEffect
    data object Close : SessionEffect
    data object OpenYandexMusic : SessionEffect
    data object OpenSystemMusic : SessionEffect
    data class Error(val messageRes: Int) : SessionEffect
}
