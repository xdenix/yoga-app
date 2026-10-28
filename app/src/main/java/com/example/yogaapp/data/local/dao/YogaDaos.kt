package com.example.yogaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.yogaapp.data.local.entity.DayEntity
import com.example.yogaapp.data.local.entity.DayPoseEntryEntity
import com.example.yogaapp.data.local.entity.PoseEntity
import com.example.yogaapp.data.local.entity.ProgramEntity
import com.example.yogaapp.data.local.entity.SessionProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoseDao {

    @Query("SELECT * FROM poses ORDER BY level, ruName")
    fun observeAll(): Flow<List<PoseEntity>>

    @Query("SELECT * FROM poses WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PoseEntity?

    @Query("SELECT * FROM poses")
    suspend fun all(): List<PoseEntity>

    @Query("SELECT COUNT(*) FROM poses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(poses: List<PoseEntity>)
}

@Dao
interface ProgramDao {

    @Query("SELECT * FROM programs LIMIT 1")
    fun observeProgram(): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs LIMIT 1")
    suspend fun getProgram(): ProgramEntity?

    @Upsert
    suspend fun upsertProgram(program: ProgramEntity)

    @Query("UPDATE programs SET currentDay = :dayNumber")
    suspend fun setCurrentDay(dayNumber: Int)

    @Query("UPDATE programs SET currentDay = currentDay + 1 WHERE currentDay < 30")
    suspend fun advanceCurrentDay()
}

/** Joined result for a day with its pose entries — used so the repository can emit a single
 *  Flow of full [com.example.yogaapp.domain.model.YogaDay] instances. */
data class DayWithEntries(
    @Embedded val day: DayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId")
    val entries: List<DayPoseEntryEntity>,
)

@Dao
interface DayDao {

    @Transaction
    @Query("SELECT * FROM days WHERE programId = :programId ORDER BY dayNumber")
    fun observeByProgram(programId: String): Flow<List<DayWithEntries>>

    @Transaction
    @Query("SELECT * FROM days WHERE isCustom = 1 ORDER BY dayNumber DESC")
    fun observeCustomDays(): Flow<List<DayWithEntries>>

    @Query("SELECT * FROM days WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DayEntity?

    @Transaction
    @Query("SELECT * FROM days WHERE id = :id LIMIT 1")
    suspend fun findWithEntries(id: String): DayWithEntries?

    @Upsert
    suspend fun upsertDay(day: DayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DayPoseEntryEntity>)

    @Query("DELETE FROM day_pose_entries WHERE dayId = :dayId")
    suspend fun deleteEntries(dayId: String)

    @Query("DELETE FROM days WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE days SET isSkipped = :skipped WHERE id = :dayId")
    suspend fun setSkipped(dayId: String, skipped: Boolean)

    @Transaction
    suspend fun replaceDayAndEntries(day: DayEntity, entries: List<DayPoseEntryEntity>) {
        upsertDay(day)
        deleteEntries(day.id)
        insertEntries(entries)
    }
}

@Dao
interface ProgressDao {

    @Query("SELECT * FROM session_progress ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<SessionProgressEntity>>

    @Insert
    suspend fun insert(progress: SessionProgressEntity)

    @Query("DELETE FROM session_progress")
    suspend fun deleteAll()
}

