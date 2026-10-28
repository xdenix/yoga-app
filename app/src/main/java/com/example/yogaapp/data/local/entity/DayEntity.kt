package com.example.yogaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.yogaapp.domain.model.DayPoseEntry

/**
 * Persisted day row. Belongs to either:
 *  - the program (programId = "default-program-<level>") OR
 *  - a custom day created by the user (isCustom = true, programId = "custom")
 */
@Entity(
    tableName = "days",
    indices = [
        Index(value = ["programId"]),
        Index(value = ["dayNumber"]),
    ],
)
data class DayEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val dayNumber: Int,
    val title: String,
    val isCustom: Boolean,
    val isSkipped: Boolean,
)

/**
 * Persisted pose entry within a day. [orderIndex] determines display order; supports
 * drag-to-reorder. [durationOverrideSec] null = use the pose's default duration.
 */
@Entity(
    tableName = "day_pose_entries",
    primaryKeys = ["dayId", "poseId", "orderIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["dayId"])],
)
data class DayPoseEntryEntity(
    val dayId: String,
    val poseId: String,
    val orderIndex: Int,
    val durationOverrideSec: Int?,
) {
    fun toDomain(): DayPoseEntry = DayPoseEntry(
        poseId = poseId,
        orderIndex = orderIndex,
        durationOverrideSec = durationOverrideSec,
    )

    companion object {
        fun fromDomain(dayId: String, e: DayPoseEntry) = DayPoseEntryEntity(
            dayId = dayId,
            poseId = e.poseId,
            orderIndex = e.orderIndex,
            durationOverrideSec = e.durationOverrideSec,
        )
    }
}
