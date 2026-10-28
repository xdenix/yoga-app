package com.example.yogaapp.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.repository.YogaRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DayViewModel @Inject constructor(
    private val repository: YogaRepository,
    private val settingsDataStore: SettingsDataStore,
    private val dispatchers: AppDispatchers,
    handle: SavedStateHandle,
) : ViewModel() {

    private val dayId: String = handle.get<String>("dayId").orEmpty()

    /** Captured swap result emitted by the LibraryScreen picker (set via NavBackStackEntry). */
    private val swapResultPoseId = MutableStateFlow<String?>(null)
    private val swapResultTarget = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<DayUiState>(DayUiState.Loading)
    val uiState: StateFlow<DayUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DayEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Most recent loaded day; used so reorder/swap edits don't require a DB roundtrip. */
    private var currentDay: YogaDay? = null

    init {
        loadDay()
        observeSwapResult(handle)
    }

    private fun loadDay() {
        viewModelScope.launch(dispatchers.io) {
            // Fetch poses once so we can resolve ids → names quickly.
            val allPoses = repository.allPoses()
            val poseMap = allPoses.associateBy { it.id }
            // Observe the program (or custom days) so we get live updates.
            val program = repository.observeProgram().first()
            val customDays = repository.observeCustomDays().first()
            val day = program?.days?.firstOrNull { it.id == dayId }
                ?: customDays.firstOrNull { it.id == dayId }
            if (day == null) {
                _uiState.value = DayUiState.NotFound
                return@launch
            }
            currentDay = day
            val globalDuration = settingsDataStore.globalDurationSec.first()
            _uiState.value = DayUiState.Content(
                day = day,
                poseMap = poseMap,
                globalDurationSec = globalDuration,
            )
        }
    }

    private fun observeSwapResult(handle: SavedStateHandle) {
        // Read once on init; the result is set by the SwapPicker route's onPicked callback.
        handle.getStateFlow<String?>("swap_result_new_pose_id", null)
            .let { flow ->
                viewModelScope.launch {
                    flow.collect { newPoseId ->
                        if (newPoseId != null) {
                            val target = handle.get<String?>("swap_result_target_pose_id")
                            if (target != null) {
                                performSwap(target, newPoseId)
                                handle.remove<String?>("swap_result_new_pose_id")
                                handle.remove<String?>("swap_result_target_pose_id")
                            }
                        }
                    }
                }
            }
    }

    fun onEvent(event: DayEvent) {
        when (event) {
            is DayEvent.Reorder -> reorder(event.fromIndex, event.toIndex)
            is DayEvent.SetDuration -> setDuration(event.poseId, event.durationSec)
            is DayEvent.DeletePose -> deletePose(event.poseId)
            is DayEvent.ResetOverrides -> resetOverrides()
            is DayEvent.SkipDay -> skipDay()
            DayEvent.Retry -> loadDay()
        }
    }

    private fun reorder(fromIndex: Int, toIndex: Int) {
        val state = _uiState.value as? DayUiState.Content ?: return
        val day = state.day
        if (fromIndex !in day.poses.indices || toIndex !in day.poses.indices) return
        val updatedList = day.poses.toMutableList().apply {
            val moved = removeAt(fromIndex)
            add(toIndex, moved)
        }.mapIndexed { idx, e -> e.copy(orderIndex = idx) }
        val updatedDay = day.copy(poses = updatedList)
        currentDay = updatedDay
        _uiState.value = state.copy(day = updatedDay)
        persistDay(updatedDay)
    }

    private fun setDuration(poseId: String, durationSec: Int) {
        val state = _uiState.value as? DayUiState.Content ?: return
        val updatedList = state.day.poses.map { e ->
            if (e.poseId == poseId) e.copy(durationOverrideSec = durationSec) else e
        }
        val updatedDay = state.day.copy(poses = updatedList)
        currentDay = updatedDay
        _uiState.value = state.copy(day = updatedDay)
        persistDay(updatedDay)
    }

    private fun deletePose(poseId: String) {
        val state = _uiState.value as? DayUiState.Content ?: return
        val updatedList = state.day.poses
            .filterNot { it.poseId == poseId }
            .mapIndexed { idx, e -> e.copy(orderIndex = idx) }
        val updatedDay = state.day.copy(poses = updatedList)
        currentDay = updatedDay
        _uiState.value = state.copy(day = updatedDay)
        persistDay(updatedDay)
    }

    private fun performSwap(targetPoseId: String, newPoseId: String) {
        val state = _uiState.value as? DayUiState.Content ?: return
        val updatedList = state.day.poses.map { e ->
            if (e.poseId == targetPoseId) e.copy(poseId = newPoseId) else e
        }
        val updatedDay = state.day.copy(poses = updatedList)
        currentDay = updatedDay
        _uiState.value = state.copy(day = updatedDay)
        persistDay(updatedDay)
        viewModelScope.launch { _effects.send(DayEffect.Snackbar(com.example.yogaapp.R.string.action_done)) }
    }

    private fun resetOverrides() {
        val state = _uiState.value as? DayUiState.Content ?: return
        val updatedList = state.day.poses.mapIndexed { idx, e ->
            e.copy(orderIndex = idx, durationOverrideSec = null)
        }
        val updatedDay = state.day.copy(poses = updatedList)
        currentDay = updatedDay
        _uiState.value = state.copy(day = updatedDay)
        persistDay(updatedDay)
    }

    private fun skipDay() {
        val state = _uiState.value as? DayUiState.Content ?: return
        viewModelScope.launch(dispatchers.io) {
            // Reuse the repository's skip logic (sets isSkipped + advances current day).
            repository.skipDay(state.day.dayNumber, autoShift = true)
            _effects.send(DayEffect.Snackbar(com.example.yogaapp.R.string.day_skip))
        }
    }

    private fun persistDay(day: YogaDay) {
        viewModelScope.launch(dispatchers.io) {
            runCatching { repository.updateProgramDay(day) }
                .onFailure { error ->
                    _effects.send(DayEffect.Error(messageResFor(AppError.from(error))))
                }
        }
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

sealed interface DayUiState {
    data object Loading : DayUiState
    data object NotFound : DayUiState
    data class Content(
        val day: YogaDay,
        val poseMap: Map<String, YogaPose>,
        val globalDurationSec: Int,
    ) : DayUiState {
        val totalDurationSec: Int
            get() = day.poses.sumOf { e ->
                e.durationOverrideSec
                    ?: poseMap[e.poseId]?.defaultDurationSec
                    ?: globalDurationSec
            }
    }
    data class Error(val messageRes: Int) : DayUiState
}

sealed interface DayEvent {
    data class Reorder(val fromIndex: Int, val toIndex: Int) : DayEvent
    data class SetDuration(val poseId: String, val durationSec: Int) : DayEvent
    data class DeletePose(val poseId: String) : DayEvent
    data object ResetOverrides : DayEvent
    data object SkipDay : DayEvent
    data object Retry : DayEvent
}

sealed interface DayEffect {
    data class Snackbar(val messageRes: Int) : DayEffect
    data class Error(val messageRes: Int) : DayEffect
}
