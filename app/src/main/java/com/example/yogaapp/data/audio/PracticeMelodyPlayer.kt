package com.example.yogaapp.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Зацикленное тихое воспроизведение выбранной мелодии из assets.
 * Голос позы идёт отдельным плеером и эту дорожку не глушит.
 */
@Singleton
class PracticeMelodyPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var current: PracticeMelody = PracticeMelody.Off

    fun play(melody: PracticeMelody) {
        val asset = melody.assetName
        if (asset == null) {
            stop()
            return
        }
        if (current == melody && player != null) {
            if (player?.isPlaying != true) {
                runCatching { player?.start() }
            }
            return
        }
        releasePlayer()
        current = melody
        runCatching {
            val afd = context.assets.openFd(asset)
            val created = MediaPlayer()
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            created.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            created.isLooping = true
            created.setVolume(VOLUME, VOLUME)
            created.prepare()
            created.start()
            player = created
        }.onFailure {
            releasePlayer()
            current = PracticeMelody.Off
        }
    }

    fun pause() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun stop() {
        releasePlayer()
        current = PracticeMelody.Off
    }

    private fun releasePlayer() {
        val active = player ?: return
        player = null
        runCatching {
            if (active.isPlaying) active.stop()
            active.release()
        }
    }

    private companion object {
        const val VOLUME = 0.32f
    }
}
