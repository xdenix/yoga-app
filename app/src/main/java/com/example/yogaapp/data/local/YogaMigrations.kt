package com.example.yogaapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.yogaapp.domain.model.PoseSide

/**
 * Room migrations for [YogaDatabase].
 *
 * v1 → v2: bilateral flag on poses; side (None/Right/Left) on day pose entries.
 */
object YogaMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE poses ADD COLUMN bilateral INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE day_pose_entries ADD COLUMN side TEXT NOT NULL DEFAULT '${PoseSide.None.name}'",
            )
        }
    }
}
