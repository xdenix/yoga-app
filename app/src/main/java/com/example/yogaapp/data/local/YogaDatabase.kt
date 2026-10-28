package com.example.yogaapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.yogaapp.data.local.converter.RoomTypeConverters
import com.example.yogaapp.data.local.dao.DayDao
import com.example.yogaapp.data.local.dao.PoseDao
import com.example.yogaapp.data.local.dao.ProgramDao
import com.example.yogaapp.data.local.dao.ProgressDao
import com.example.yogaapp.data.local.entity.DayEntity
import com.example.yogaapp.data.local.entity.DayPoseEntryEntity
import com.example.yogaapp.data.local.entity.PoseEntity
import com.example.yogaapp.data.local.entity.ProgramEntity
import com.example.yogaapp.data.local.entity.SessionProgressEntity

@Database(
    entities = [
        PoseEntity::class,
        DayEntity::class,
        DayPoseEntryEntity::class,
        ProgramEntity::class,
        SessionProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun poseDao(): PoseDao
    abstract fun programDao(): ProgramDao
    abstract fun dayDao(): DayDao
    abstract fun progressDao(): ProgressDao

    companion object {
        const val DATABASE_NAME = "yoga-app.db"
    }
}
