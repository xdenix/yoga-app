package com.example.yogaapp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Navigation Compose 2.8+ (with `kotlinx.serialization`).
 *
 * Each `@Serializable` object/class is a destination. The type-safe plugin uses these to
 * generate `NavBackStackEntry` arguments, avoiding route-string typos.
 */
sealed interface Route {

    @Serializable
    data object Splash : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Program : Route

    @Serializable
    data class Day(val dayId: String) : Route

    @Serializable
    data class Session(val dayId: String) : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object CustomDayEditor : Route

    @Serializable
    data class CustomDayEdit(val dayId: String?) : Route

    @Serializable
    data object Settings : Route

    /**
     * Swap picker — invoked from DayDetail when the user taps "Заменить" on a pose.
     * [dayId] identifies the day being edited; [poseIdToReplace] is the entry to swap out.
     * After picking, the user is returned to DayDetail with the new pose substituted in.
     */
    @Serializable
    data class SwapPicker(val dayId: String, val poseIdToReplace: String) : Route
}
