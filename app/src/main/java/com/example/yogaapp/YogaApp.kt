package com.example.yogaapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.repository.YogaRepositoryImpl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entrypoint. Hilt-annotated so the dependency graph is built once per process.
 *
 * Provides [workerFactory] to WorkManager (for future reminder workers — currently unused but
 * registered so the plumbing is in place).
 *
 * On first launch we trigger [YogaRepositoryImpl.seedPosesIfEmpty] from a background
 * supervisor scope so the user sees the 40-pose library immediately.
 *
 * Timber is initialised only on debug builds — release must never log to logcat.
 */
@HiltAndroidApp
class YogaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var repository: YogaRepositoryImpl
    @Inject lateinit var audioPlayer: PoseAudioPlayer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        appScope.launch {
            runCatching { repository.seedPosesIfEmpty() }
                .onFailure { Timber.e(it, "Failed to seed pose library") }
        }
    }

    override fun onTerminate() {
        audioPlayer.release()
        super.onTerminate()
    }
}
