package com.example.yogaapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.yogaapp.data.local.YogaDatabase
import com.example.yogaapp.data.local.YogaMigrations
import com.example.yogaapp.data.local.dao.DayDao
import com.example.yogaapp.data.local.dao.PoseDao
import com.example.yogaapp.data.local.dao.ProgramDao
import com.example.yogaapp.data.local.dao.ProgressDao
import com.example.yogaapp.data.repository.YogaRepositoryImpl
import com.example.yogaapp.domain.repository.YogaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindYogaRepository(impl: YogaRepositoryImpl): YogaRepository

    companion object {

        @Provides
        @Singleton
        fun provideYogaDatabase(@ApplicationContext context: Context): YogaDatabase =
            Room.databaseBuilder(
                context,
                YogaDatabase::class.java,
                YogaDatabase.DATABASE_NAME,
            )
                .addMigrations(YogaMigrations.MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

        @Provides
        @Singleton
        fun providePoseDao(db: YogaDatabase): PoseDao = db.poseDao()

        @Provides
        @Singleton
        fun provideProgramDao(db: YogaDatabase): ProgramDao = db.programDao()

        @Provides
        @Singleton
        fun provideDayDao(db: YogaDatabase): DayDao = db.dayDao()

        @Provides
        @Singleton
        fun provideProgressDao(db: YogaDatabase): ProgressDao = db.progressDao()
    }
}
