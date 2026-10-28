package com.example.yogaapp.domain.repository

import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaProgram
import com.example.yogaapp.domain.model.SessionProgress
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for everything yoga-related:
 *  - reading the pose library (static)
 *  - reading / writing the user's 30-day program (persisted)
 *  - reading / writing custom days (user-created)
 *  - tracking session completion progress
 *
 * The implementation is Room-backed today; swapping to a Retrofit + Room cache would touch
 * only the impl + Hilt binding.
 */
interface YogaRepository {

    // ---- Pose library ----
    fun observePoses(): Flow<List<YogaPose>>
    suspend fun getPose(id: String): YogaPose?
    suspend fun allPoses(): List<YogaPose>

    // ---- Program ----
    fun observeProgram(): Flow<YogaProgram?>
    suspend fun getProgram(): YogaProgram?
    suspend fun createProgram(
        level: YogaLevel,
        posesPerDay: Int = com.example.yogaapp.domain.model.YogaRules.DEFAULT_POSES_PER_DAY,
    ): YogaProgram
    suspend fun updateProgramDay(day: YogaDay)
    suspend fun setCurrentDay(dayNumber: Int)
    suspend fun skipDay(dayNumber: Int, autoShift: Boolean)

    // ---- Custom days ----
    fun observeCustomDays(): Flow<List<YogaDay>>
    suspend fun createCustomDay(title: String, poseIds: List<String>): YogaDay
    suspend fun deleteCustomDay(id: String): Boolean

    // ---- Progress ----
    fun observeProgress(): Flow<List<SessionProgress>>
    suspend fun recordCompletion(dayId: String, durationSec: Int)
    suspend fun resetProgress()
}
