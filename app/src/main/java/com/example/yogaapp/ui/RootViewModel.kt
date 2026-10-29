package com.example.yogaapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.data.datastore.ThemeMode
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root-level state holder. Decides:
 *  - the next screen after Splash (Onboarding or Program, depending on whether the user
 *    has already completed onboarding)
 *  - the user's theme + audio settings (propagated to the [YogaAppTheme] and screens)
 *  - the chosen level (used by ProgramViewModel to fall back when the program is missing)
 *
 * The NavHost always starts at [Route.Splash]; after the splash delay, navigation goes to
 * [RootUiState.nextDestinationAfterSplash] (Onboarding or Program).
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<RootUiState> = combine(
        settingsDataStore.themeMode,
        settingsDataStore.dynamicColor,
        settingsDataStore.onboardingShown,
        settingsDataStore.chosenLevel,
    ) { themeMode, dynamicColor, onboardingShown, level ->
        RootUiState(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            chosenLevel = level,
            nextDestinationAfterSplash =
                if (onboardingShown) Route.Program else Route.Onboarding,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RootUiState(),
    )

    fun onOnboardingFinished(level: YogaLevel) {
        viewModelScope.launch {
            settingsDataStore.setChosenLevel(level)
            settingsDataStore.markOnboardingShown()
        }
    }
}

data class RootUiState(
    val themeMode: ThemeMode = ThemeMode.DustyRose,
    val dynamicColor: Boolean = false,
    val chosenLevel: YogaLevel? = null,
    val nextDestinationAfterSplash: Route = Route.Onboarding,
)
