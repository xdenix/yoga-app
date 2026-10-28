package com.example.yogaapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.usecase.ObservePosesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observePoses: ObservePosesUseCase,
) : ViewModel() {

    private val _filter = MutableStateFlow<YogaLevel?>(null)
    private val _query = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> = combine(
        observePoses(),
        _filter,
        _query,
    ) { poses, filter, query ->
        val filtered = poses
            .filter { p -> filter == null || p.level == filter }
            .filter { p -> query.isBlank() || p.ruName.contains(query, ignoreCase = true) }
        val state: LibraryUiState =
            LibraryUiState.Content(poses = filtered, filter = filter, query = query)
        state
    }
        .catch { emit(LibraryUiState.Error(com.example.yogaapp.R.string.error_unknown)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LibraryUiState.Loading,
        )

    fun setFilter(level: YogaLevel?) {
        _filter.value = level
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(
        val poses: List<YogaPose>,
        val filter: YogaLevel?,
        val query: String,
    ) : LibraryUiState
    data class Error(val messageRes: Int) : LibraryUiState
}
