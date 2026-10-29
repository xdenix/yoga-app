package com.example.yogaapp.ui.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaProgram
import com.example.yogaapp.domain.usecase.CreateProgramUseCase
import com.example.yogaapp.domain.usecase.ObserveProgramUseCase
import com.example.yogaapp.domain.usecase.ObserveProgressUseCase
import com.example.yogaapp.domain.usecase.SetCurrentDayUseCase
import com.example.yogaapp.domain.usecase.SkipDayUseCase
import com.example.yogaapp.domain.util.AppDispatchers
import com.example.yogaapp.domain.util.AppError
import com.example.yogaapp.domain.util.ErrorKind
import com.example.yogaapp.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramViewModel @Inject constructor(
    observeProgram: ObserveProgramUseCase,
    observeProgress: ObserveProgressUseCase,
    private val createProgram: CreateProgramUseCase,
    private val setCurrentDay: SetCurrentDayUseCase,
    private val skipDay: SkipDayUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _effects = Channel<ProgramEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<ProgramUiState> = combine(
        observeProgram(),
        observeProgress(),
        settingsDataStore.chosenLevel,
    ) { program, progress, level ->
        reduce(program, progress, level)
    }
        .catch { throwable ->
            emit(ProgramUiState.Error(messageResFor(AppError.from(throwable))))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ProgramUiState.Loading,
        )

    fun onEvent(event: ProgramEvent) {
        when (event) {
            is ProgramEvent.SelectDay -> viewModelScope.launch(dispatchers.io) {
                setCurrentDay(event.dayNumber)
            }
            is ProgramEvent.SkipDay -> viewModelScope.launch(dispatchers.io) {
                when (val r = skipDay(event.dayNumber, autoShift = true)) {
                    is Result.Success -> Unit
                    is Result.Failure ->
                        _effects.send(ProgramEffect.ShowError(messageResFor(r.error)))
                }
            }
            is ProgramEvent.EnsurePlan -> viewModelScope.launch(dispatchers.io) {
                // Triggered when the UI lands on the Empty state with no chosen level —
                // we don't have a default to pick here, so just emit a hint effect.
                _effects.send(ProgramEffect.RequestLevelSelection)
            }
            ProgramEvent.Retry -> Unit // Flow auto-resubscribes.
        }
    }

    private fun reduce(
        program: YogaProgram?,
        progress: List<com.example.yogaapp.domain.model.SessionProgress>,
        level: com.example.yogaapp.domain.model.YogaLevel?,
    ): ProgramUiState {
        if (program == null) {
            return if (level != null) {
                // Lazy-create the program on first observation when the level is known.
                viewModelScope.launch(dispatchers.io) {
                    val count = settingsDataStore.posesPerDay.first()
                    createProgram(level, count)
                }
                ProgramUiState.Loading
            } else {
                ProgramUiState.Empty
            }
        }
        val completedDayIds = progress.map { it.dayId }.toSet()
        val trainedDates = progress.map { entry ->
            java.time.Instant.ofEpochMilli(entry.completedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.toSet()
        return ProgramUiState.Content(
            program = program,
            currentDayNumber = program.currentDay,
            completedDayIds = completedDayIds,
            trainedDates = trainedDates,
        )
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

sealed interface ProgramUiState {
    data object Loading : ProgramUiState
    data class Content(
        val program: YogaProgram,
        val currentDayNumber: Int,
        val completedDayIds: Set<String>,
        val trainedDates: Set<java.time.LocalDate> = emptySet(),
    ) : ProgramUiState {
        val days: List<YogaDay> get() = program.days
        val completedCount: Int get() = completedDayIds.intersect(days.map { it.id }.toSet()).size
    }
    data object Empty : ProgramUiState
    data class Error(val messageRes: Int) : ProgramUiState
}

sealed interface ProgramEvent {
    data class SelectDay(val dayNumber: Int) : ProgramEvent
    data class SkipDay(val dayNumber: Int) : ProgramEvent
    data object EnsurePlan : ProgramEvent
    data object Retry : ProgramEvent
}

sealed interface ProgramEffect {
    data class ShowSnackbar(val messageRes: Int) : ProgramEffect
    data class ShowError(val messageRes: Int) : ProgramEffect
    data object RequestLevelSelection : ProgramEffect
}
