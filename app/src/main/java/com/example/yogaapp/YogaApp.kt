package com.example.yogaapp

import android.app.Application
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
 * On first launch we trigger [YogaRepositoryImpl.seedPosesIfEmpty] from a background
 * supervisor scope so the user sees the 42-pose library immediately.
 *
 * Timber is initialised only on debug builds — release must never log to logcat.
 */
@HiltAndroidApp
class YogaApp : Application() {

    @Inject lateinit var repository: YogaRepositoryImpl
    @Inject lateinit var audioPlayer: PoseAudioPlayer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
