package com.example.yogaapp.domain.repository

import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.SessionProgress
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID

/**
 * In-memory test double for [YogaRepository]. No Android / no Room — JVM-testable.
 *
 * Initial state mirrors the seeded production database (40 poses from [PoseLibrary]) so
 * plan-generation tests don't need to mock the seed step.
 */
class FakeYogaRepository(
    initialPoses: List<YogaPose> = PoseLibrary.all,
) : YogaRepository {

    private val _poses = MutableStateFlow(initialPoses)
    private val _program = MutableStateFlow<YogaProgram?>(null)
    private val _customDays = MutableStateFlow<List<YogaDay>>(emptyList())
    private val _progress = MutableStateFlow<List<SessionProgress>>(emptyList())

    val posesSnapshot get() = _poses.value
    val programSnapshot get() = _program.value
    val progressSnapshot get() = _progress.value

    override fun observePoses() = _poses.asStateFlow()

    override suspend fun getPose(id: String): YogaPose? = _poses.value.firstOrNull { it.id == id }

    override suspend fun allPoses(): List<YogaPose> = _poses.value

    override fun observeProgram() = _program.asStateFlow()

    override suspend fun getProgram(): YogaProgram? = _program.value

    override suspend fun createProgram(level: YogaLevel, posesPerDay: Int): YogaProgram {
        val generated = GenerateProgramForTest(level, posesPerDay)
        _program.value = generated
        return generated
    }

    override suspend fun updateProgramDay(day: YogaDay) {
        val program = _program.value ?: return
        val newDays = program.days.map { if (it.id == day.id) day else it }
        _program.value = program.copy(days = newDays)
    }

    override suspend fun setCurrentDay(dayNumber: Int) {
        _program.update { it?.copy(currentDay = dayNumber) }
    }

    override suspend fun skipDay(dayNumber: Int, autoShift: Boolean) {
        val program = _program.value ?: return
        val newDays = program.days.map { d ->
            if (d.dayNumber == dayNumber) d.copy(isSkipped = true) else d
        }
        val newCurrent = if (autoShift) program.currentDay + 1 else program.currentDay
        _program.value = program.copy(days = newDays, currentDay = newCurrent)
    }

    override fun observeCustomDays() = _customDays.asStateFlow()

    override suspend fun createCustomDay(title: String, poseIds: List<String>): YogaDay {
        val dayNumber = (_customDays.value.maxOfOrNull { it.dayNumber } ?: 0) + 1
        val day = YogaDay(
            id = "custom-${UUID.randomUUID()}",
            dayNumber = dayNumber,
            title = title,
            poses = poseIds.mapIndexed { idx, id -> DayPoseEntry(id, idx, null) },
            isCustom = true,
        )
        _customDays.update { it + day }
        return day
    }

    override suspend fun deleteCustomDay(id: String): Boolean {
        val before = _customDays.value.size
        _customDays.update { it.filterNot { d -> d.id == id } }
        return _customDays.value.size != before
    }

    override fun observeProgress() = _progress.asStateFlow()

    override suspend fun recordCompletion(dayId: String, durationSec: Int) {
        _progress.update {
            it + SessionProgress(
                dayId = dayId,
                completedAt = Instant.now().toEpochMilli(),
                durationSec = durationSec,
            )
        }
        _program.update { program ->
            program?.copy(currentDay = (program.currentDay + 1).coerceAtMost(30))
        }
    }

    override suspend fun resetProgress() {
        _progress.value = emptyList()
        _program.update { it?.copy(currentDay = 1) }
    }
}

/** Test-only program generator — mirrors [com.example.yogaapp.domain.usecase.GenerateProgramUseCase]
 *  but instantiated without DI so tests can call it directly. */
private fun GenerateProgramForTest(level: YogaLevel, posesPerDay: Int): YogaProgram =
    com.example.yogaapp.domain.usecase.GenerateProgramUseCase().invoke(level, posesPerDay)
