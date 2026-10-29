package com.example.yogaapp.data.repository

import com.example.yogaapp.data.local.dao.DayDao
import com.example.yogaapp.data.local.dao.DayWithEntries
import com.example.yogaapp.data.local.dao.PoseDao
import com.example.yogaapp.data.local.dao.ProgramDao
import com.example.yogaapp.data.local.dao.ProgressDao
import com.example.yogaapp.data.local.entity.DayEntity
import com.example.yogaapp.data.local.entity.DayPoseEntryEntity
import com.example.yogaapp.data.local.entity.PoseEntity
import com.example.yogaapp.data.local.entity.ProgramEntity
import com.example.yogaapp.data.local.entity.SessionProgressEntity
import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.SessionProgress
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaProgram
import com.example.yogaapp.domain.repository.YogaRepository
import com.example.yogaapp.domain.usecase.GenerateProgramUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [YogaRepository].
 *
 *  - Pose library is seeded from [PoseLibrary] on first launch (idempotent via count check)
 *  - Program is generated via [GenerateProgramUseCase] when the user picks a level on onboarding;
 *    persisted to Room so progress survives restarts.
 *  - Day edits (reorder, swap, duration override) overwrite the day's entries in Room.
 *  - Custom days are stored with programId = "custom" and isCustom = true.
 *
 * All Flow-returning methods observe Room so the UI re-emits automatically on writes.
 */
@Singleton
class YogaRepositoryImpl @Inject constructor(
    private val poseDao: PoseDao,
    private val programDao: ProgramDao,
    private val dayDao: DayDao,
    private val progressDao: ProgressDao,
    private val generateProgram: GenerateProgramUseCase,
) : YogaRepository {

    override fun observePoses(): Flow<List<YogaPose>> =
        poseDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPose(id: String): YogaPose? = poseDao.findById(id)?.toDomain()

    override suspend fun allPoses(): List<YogaPose> = poseDao.all().map { it.toDomain() }

    override fun observeProgram(): Flow<YogaProgram?> = combine(
        programDao.observeProgram(),
        dayDao.observeByProgram(PROGRAM_ID),
    ) { programEntity, daysWithEntries ->
        if (programEntity == null) return@combine null
        val level = runCatching { YogaLevel.valueOf(programEntity.levelName) }
            .getOrDefault(YogaLevel.Beginner)
        YogaProgram(
            id = programEntity.id,
            level = level,
            days = daysWithEntries.map { it.toDomain() },
            currentDay = programEntity.currentDay,
        )
    }

    override suspend fun getProgram(): YogaProgram? {
        val programEntity = programDao.getProgram() ?: return null
        // One-shot read — collect a single emission from the day flow.
        val days = dayDao.observeByProgram(PROGRAM_ID).first()
        val level = runCatching { YogaLevel.valueOf(programEntity.levelName) }
            .getOrDefault(YogaLevel.Beginner)
        return YogaProgram(
            id = programEntity.id,
            level = level,
            days = days.map { it.toDomain() },
            currentDay = programEntity.currentDay,
        )
    }

    override suspend fun createProgram(level: YogaLevel, posesPerDay: Int): YogaProgram {
        val generated = generateProgram(level, posesPerDay)
        programDao.upsertProgram(
            ProgramEntity(
                id = PROGRAM_ID,
                levelName = level.name,
                currentDay = 1,
            ),
        )
        // Clear any prior program days (when the user re-creates with a different level).
        generated.days.forEach { day ->
            dayDao.replaceDayAndEntries(
                day = DayEntity(
                    id = day.id,
                    programId = PROGRAM_ID,
                    dayNumber = day.dayNumber,
                    title = day.title,
                    isCustom = false,
                    isSkipped = false,
                ),
                entries = day.poses.map { DayPoseEntryEntity.fromDomain(day.id, it) },
            )
        }
        return generated
    }

    override suspend fun updateProgramDay(day: YogaDay) {
        val dayEntity = DayEntity(
            id = day.id,
            programId = if (day.isCustom) CUSTOM_PROGRAM_ID else PROGRAM_ID,
            dayNumber = day.dayNumber,
            title = day.title,
            isCustom = day.isCustom,
            isSkipped = day.isSkipped,
        )
        val entries = day.poses.map { DayPoseEntryEntity.fromDomain(day.id, it) }
        dayDao.replaceDayAndEntries(dayEntity, entries)
    }

    override suspend fun setCurrentDay(dayNumber: Int) {
        programDao.setCurrentDay(dayNumber)
    }

    override suspend fun skipDay(dayNumber: Int, autoShift: Boolean) {
        // Mark day as skipped (UI shows strike-through); if autoShift, advance current day
        // by one — the user effectively swaps this day's session for the next one.
        val dayId = "program-$dayNumber"
        dayDao.setSkipped(dayId, skipped = true)
        if (autoShift) {
            programDao.advanceCurrentDay()
        }
    }

    override fun observeCustomDays(): Flow<List<YogaDay>> =
        dayDao.observeCustomDays().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createCustomDay(title: String, poseIds: List<String>): YogaDay {
        val customNumber = (System.currentTimeMillis() / 1000L).toInt()
        val poseMap = allPoses().associateBy { it.id }
        val resolved = poseIds.mapNotNull { poseMap[it] }
        val day = YogaDay(
            id = "custom-$customNumber",
            dayNumber = customNumber,
            title = title,
            poses = com.example.yogaapp.domain.model.YogaRules.toDayEntries(resolved),
            isCustom = true,
        )
        dayDao.replaceDayAndEntries(
            day = DayEntity(
                id = day.id,
                programId = CUSTOM_PROGRAM_ID,
                dayNumber = day.dayNumber,
                title = day.title,
                isCustom = true,
                isSkipped = false,
            ),
            entries = day.poses.map { DayPoseEntryEntity.fromDomain(day.id, it) },
        )
        return day
    }

    override suspend fun deleteCustomDay(id: String): Boolean =
        dayDao.deleteById(id) > 0

    override fun observeProgress(): Flow<List<SessionProgress>> =
        progressDao.observeAll().map { entities ->
            entities.map {
                SessionProgress(
                    dayId = it.dayId,
                    completedAt = it.completedAt,
                    durationSec = it.durationSec,
                )
            }
        }

    override suspend fun recordCompletion(dayId: String, durationSec: Int) {
        progressDao.insert(
            SessionProgressEntity(
                dayId = dayId,
                completedAt = System.currentTimeMillis(),
                durationSec = durationSec,
            ),
        )
        if (com.example.yogaapp.domain.model.WorkoutBlocks.byId(dayId) == null) {
            programDao.advanceCurrentDay()
        }
    }

    override suspend fun resetProgress() {
        progressDao.deleteAll()
        programDao.setCurrentDay(1)
    }

    /** Seeds the pose library on first launch (idempotent). Called from [com.example.yogaapp.YogaApp]. */
    /**
     * Seeds the pose library on first launch, and refreshes rows on later launches
     * (REPLACE) so library fields like [YogaPose.bilateral] stay in sync with code.
     */
    suspend fun seedPosesIfEmpty() {
        poseDao.insertAll(PoseLibrary.all.map { PoseEntity.fromDomain(it) })
    }

    private fun DayWithEntries.toDomain(): YogaDay = YogaDay(
        id = day.id,
        dayNumber = day.dayNumber,
        title = day.title,
        poses = entries.map { it.toDomain() }.sortedBy { it.orderIndex },
        isCustom = day.isCustom,
        isSkipped = day.isSkipped,
    )

    companion object {
        const val PROGRAM_ID = "default-program"
        const val CUSTOM_PROGRAM_ID = "custom"
    }
}
