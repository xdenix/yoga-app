package com.example.yogaapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.yogaapp.ui.customday.CustomDayEditorScreen
import com.example.yogaapp.ui.day.DayScreen
import com.example.yogaapp.ui.library.LibraryScreen
import com.example.yogaapp.ui.navigation.Route
import com.example.yogaapp.ui.onboarding.OnboardingScreen
import com.example.yogaapp.ui.program.ProgramScreen
import com.example.yogaapp.ui.session.SessionPlayerScreen
import com.example.yogaapp.ui.settings.SettingsScreen
import com.example.yogaapp.ui.splash.SplashScreen
import com.example.yogaapp.ui.theme.YogaAppTheme

/**
 * Root composable. Owns the [NavHostController] and wires the [YogaAppTheme] to the
 * user's preferences. All VMs are Hilt-resolved via [hiltViewModel].
 *
 * Navigation flow:
 *  1. App starts at [Route.Splash].
 *  2. After ~1s, [SplashScreen] navigates to [RootUiState.nextDestinationAfterSplash]
 *     (Onboarding or Program), popping Splash from the back stack.
 *  3. Onboarding writes the chosen level to DataStore and navigates to Program.
 *  4. Program → Day → Session (player) → back to Program (with progress updated).
 */
@Composable
fun YogaAppRoot(
    navController: NavHostController = rememberNavController(),
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val state by rootViewModel.uiState.collectAsStateWithLifecycle()

    YogaAppTheme(themeMode = state.themeMode, dynamicColor = state.dynamicColor) {
        NavHost(
            navController = navController,
            startDestination = Route.Splash,
        ) {
            composable<Route.Splash> {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(state.nextDestinationAfterSplash) {
                            popUpTo<Route.Splash> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onComplete = { level ->
                        rootViewModel.onOnboardingFinished(level)
                        navController.navigate(Route.Program) {
                            popUpTo<Route.Onboarding> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<Route.Program> {
                ProgramScreen(
                    onDayClick = { id -> navController.navigate(Route.Day(id)) },
                    onSessionClick = { id -> navController.navigate(Route.Session(id)) },
                    onLibraryClick = { navController.navigate(Route.Library) },
                    onCreateCustomDay = { navController.navigate(Route.CustomDayEdit(dayId = null)) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Day> { backStackEntry ->
                val route: Route.Day = backStackEntry.toRoute()
                DayScreen(
                    dayId = route.dayId,
                    onBack = { navController.popBackStack() },
                    onSwapClick = { dayId, orderIndex ->
                        navController.navigate(Route.SwapPicker(dayId, orderIndex))
                    },
                    onStartSession = { dayId ->
                        navController.navigate(Route.Session(dayId))
                    },
                )
            }
            composable<Route.Session> { backStackEntry ->
                val route: Route.Session = backStackEntry.toRoute()
                SessionPlayerScreen(
                    dayId = route.dayId,
                    onFinish = { navController.popBackStack() },
                )
            }
            composable<Route.Library> {
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.CustomDayEdit> { backStackEntry ->
                val route: Route.CustomDayEdit = backStackEntry.toRoute()
                CustomDayEditorScreen(
                    dayId = route.dayId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<Route.SwapPicker> { backStackEntry ->
                val route: Route.SwapPicker = backStackEntry.toRoute()
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onPicked = { newPoseId ->
                        // The Day VM listens for a result via SavedStateHandle; we just pop here.
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "swap_result_new_pose_id",
                            newPoseId,
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "swap_result_target_order_index",
                            route.orderIndex,
                        )
                        navController.popBackStack()
                    },
                    pickerMode = true,
                )
            }
            composable<Route.Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
