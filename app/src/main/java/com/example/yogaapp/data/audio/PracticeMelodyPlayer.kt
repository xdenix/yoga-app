package com.example.yogaapp.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Зацикленное тихое воспроизведение мелодии из assets или пользовательского URI.
 * Голос позы идёт отдельным плеером и эту дорожку не глушит.
 */
@Singleton
class PracticeMelodyPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var currentKey: String? = null

    fun play(melody: PracticeMelody, customUri: Uri? = null) {
        if (!melody.playsInApp) {
            stop()
            return
        }
        val key = when (melody) {
            PracticeMelody.BuiltIn -> "asset:${melody.assetName}"
            PracticeMelody.FromFile -> customUri?.toString()?.let { "uri:$it" }
            else -> null
        }
        if (key == null) {
            stop()
            return
        }
        if (currentKey == key && player != null) {
            if (player?.isPlaying != true) {
                runCatching { player?.start() }
            }
            return
        }
        releasePlayer()
        currentKey = key
        runCatching {
            val created = MediaPlayer()
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            when (melody) {
                PracticeMelody.BuiltIn -> {
                    val asset = melody.assetName ?: error("missing asset")
                    val afd = context.assets.openFd(asset)
                    created.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                PracticeMelody.FromFile -> {
                    created.setDataSource(context, customUri!!)
                }
                else -> error("unsupported")
            }
            created.isLooping = true
            created.setVolume(VOLUME, VOLUME)
            created.prepare()
            created.start()
            player = created
        }.onFailure {
            releasePlayer()
            currentKey = null
        }
    }

    fun pause() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun stop() {
        releasePlayer()
        currentKey = null
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
