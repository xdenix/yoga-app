package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.repository.YogaRepository
import com.example.yogaapp.domain.util.AppError
import com.example.yogaapp.domain.util.Result
import com.example.yogaapp.domain.util.resultOf
import javax.inject.Inject

class ObserveProgramUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    operator fun invoke() = repository.observeProgram()
}

class GetCurrentProgramUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke() = repository.getProgram()
}

class CreateProgramUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(
        level: com.example.yogaapp.domain.model.YogaLevel,
        posesPerDay: Int = com.example.yogaapp.domain.model.YogaRules.DEFAULT_POSES_PER_DAY,
    ) = repository.createProgram(level, posesPerDay)
}

class UpdateProgramDayUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(day: YogaDay): Result<Unit> = resultOf {
        repository.updateProgramDay(day)
    }
}

class SetCurrentDayUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(dayNumber: Int) = repository.setCurrentDay(dayNumber)
}

class SkipDayUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(dayNumber: Int, autoShift: Boolean): Result<Unit> = resultOf {
        repository.skipDay(dayNumber, autoShift)
    }
}

class ObserveCustomDaysUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    operator fun invoke() = repository.observeCustomDays()
}

class CreateCustomDayUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(title: String, poseIds: List<String>): Result<YogaDay> =
        resultOf { repository.createCustomDay(title, poseIds) }
}

class DeleteCustomDayUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(id: String): Result<Boolean> = resultOf {
        repository.deleteCustomDay(id)
    }
}

class ObserveProgressUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    operator fun invoke() = repository.observeProgress()
}

class RecordCompletionUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(dayId: String, durationSec: Int): Result<Unit> = resultOf {
        repository.recordCompletion(dayId, durationSec)
    }
}

class ResetProgressUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(): Result<Unit> = resultOf { repository.resetProgress() }
}

class ObservePosesUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    operator fun invoke() = repository.observePoses()
}

class GetPoseUseCase @Inject constructor(
    private val repository: YogaRepository,
) {
    suspend operator fun invoke(id: String) = repository.getPose(id)
}
