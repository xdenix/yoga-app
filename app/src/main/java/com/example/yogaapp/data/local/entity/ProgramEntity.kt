package com.example.yogaapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted program metadata (level + current day pointer). The actual days live in [DayEntity].
 */
@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val levelName: String,
    val currentDay: Int,
)

/**
 * Persisted completion record for a given day. Multiple records per day are allowed
 * (the user can re-do a day); the UI shows the latest by [completedAt].
 */
@Entity(tableName = "session_progress")
data class SessionProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: String,
    val completedAt: Long,
    val durationSec: Int,
)
