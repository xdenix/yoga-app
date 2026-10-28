package com.example.yogaapp.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.data.audio.PracticeMelodyPlayer
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.domain.model.DayPoseEntry
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

    private val _melody = MutableStateFlow(PracticeMelody.SufiMorning)
    val melody: StateFlow<PracticeMelody> = _melody.asStateFlow()

    private var sessionActive = false
    private var userPaused = false
    private var currentMelody = PracticeMelody.SufiMorning

    init {
        viewModelScope.launch {
            settingsDataStore.practiceMelody.collect { selected ->
                currentMelody = selected
                _melody.value = selected
                if (sessionActive && !userPaused && _uiState.value !is SessionUiState.Finished) {
                    melodyPlayer.play(selected)
                }
            }
        }
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch(dispatchers.io) {
            val program = repository.observeProgram().first()
            val customDays = repository.observeCustomDays().first()
            val day = program?.days?.firstOrNull { it.id == dayId }
                ?: customDays.firstOrNull { it.id == dayId }
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
            val globalDuration = settingsDataStore.globalDurationSec.first()
            timer.start(resolvedPoses, globalDurationSec = globalDuration)
            collectTimerState()
            // Play gong + speak the first pose's description.
            audioPlayer.playGong()
            audioPlayer.playDescription(resolvedPoses.first().first)
            sessionActive = true
            if (!userPaused) melodyPlayer.play(currentMelody)
        }
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
                    )
                }
                // When the pose changes, play the gong + describe the new pose.
                val currentPose = timerState.current
                if (currentPose != null) {
                    // (Re-)speak when currentIndex changes — guarded by tracking the last spoken idx.
                    if (lastSpokenIndex != timerState.currentIndex) {
                        lastSpokenIndex = timerState.currentIndex
                        audioPlayer.playGong()
                        audioPlayer.playDescription(currentPose.pose)
                    }
                }
            }
        }
    }

    private var lastSpokenIndex = -1

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
                melodyPlayer.play(currentMelody)
            }
            SessionEvent.Skip -> timer.skip()
            SessionEvent.Previous -> timer.previous()
            SessionEvent.ListenAgain -> {
                viewModelScope.launch {
                    val current = timer.state.value.current ?: return@launch
                    audioPlayer.playDescription(current.pose)
                }
            }
            SessionEvent.StopAudio -> audioPlayer.stop()
            SessionEvent.Complete -> viewModelScope.launch(dispatchers.io) {
                val elapsed = timer.state.value.elapsedSec
                when (val r = recordCompletion(dayId, elapsed)) {
                    is Result.Success -> _effects.send(SessionEffect.Completed)
                    is Result.Failure -> _effects.send(SessionEffect.Error(messageResFor(r.error)))
                }
            }
            SessionEvent.Finish -> viewModelScope.launch { _effects.send(SessionEffect.Close) }
        }
    }

    fun setMelody(melody: PracticeMelody) {
        viewModelScope.launch { settingsDataStore.setPracticeMelody(melody) }
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
    data class Error(val messageRes: Int) : SessionEffect
}
