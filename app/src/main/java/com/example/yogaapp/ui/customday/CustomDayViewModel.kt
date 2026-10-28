package com.example.yogaapp.ui.customday

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.repository.YogaRepository
import com.example.yogaapp.domain.usecase.CreateCustomDayUseCase
import com.example.yogaapp.domain.util.AppDispatchers
import com.example.yogaapp.domain.util.AppError
import com.example.yogaapp.domain.util.ErrorKind
import com.example.yogaapp.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomDayViewModel @Inject constructor(
    private val repository: YogaRepository,
    private val createCustomDay: CreateCustomDayUseCase,
    private val dispatchers: AppDispatchers,
    handle: SavedStateHandle,
) : ViewModel() {

    private val editingDayId: String? = handle.get<String>("dayId")

    private val _uiState = MutableStateFlow(CustomDayUiState())
    val uiState: StateFlow<CustomDayUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CustomDayEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadPoses()
    }

    private fun loadPoses() {
        viewModelScope.launch(dispatchers.io) {
            val poses = repository.allPoses()
            _uiState.update { it.copy(availablePoses = poses) }
        }
    }

    fun onEvent(event: CustomDayEvent) {
        when (event) {
            is CustomDayEvent.TitleChanged -> _uiState.update { it.copy(title = event.title) }
            is CustomDayEvent.TogglePose -> togglePose(event.poseId)
            CustomDayEvent.Save -> save()
            CustomDayEvent.Cancel -> viewModelScope.launch { _effects.send(CustomDayEffect.Cancelled) }
        }
    }

    private fun togglePose(poseId: String) {
        _uiState.update { state ->
            val newList = state.selectedPoseIds.toMutableList().apply {
                if (contains(poseId)) remove(poseId) else add(poseId)
            }
            state.copy(selectedPoseIds = newList)
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.selectedPoseIds.isEmpty()) {
            viewModelScope.launch { _effects.send(CustomDayEffect.Error(com.example.yogaapp.R.string.customday_empty)) }
            return
        }
        viewModelScope.launch(dispatchers.io) {
            when (val r = createCustomDay(state.title.ifBlank { "Без названия" }, state.selectedPoseIds)) {
                is Result.Success -> _effects.send(CustomDayEffect.Saved)
                is Result.Failure -> _effects.send(CustomDayEffect.Error(messageResFor(r.error)))
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

data class CustomDayUiState(
    val title: String = "",
    val availablePoses: List<YogaPose> = emptyList(),
    val selectedPoseIds: List<String> = emptyList(),
    val isSaving: Boolean = false,
)

sealed interface CustomDayEvent {
    data class TitleChanged(val title: String) : CustomDayEvent
    data class TogglePose(val poseId: String) : CustomDayEvent
    data object Save : CustomDayEvent
    data object Cancel : CustomDayEvent
}

sealed interface CustomDayEffect {
    data object Saved : CustomDayEffect
    data object Cancelled : CustomDayEffect
    data class Error(val messageRes: Int) : CustomDayEffect
}
