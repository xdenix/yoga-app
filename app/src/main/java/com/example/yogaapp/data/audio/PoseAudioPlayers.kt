package com.example.yogaapp.data.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.example.yogaapp.data.datastore.AudioMode
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.domain.model.YogaPose
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [PoseAudioPlayer] that uses the Android [TextToSpeech] engine.
 *
 * Picks the highest-quality Russian voice available (prefers neural / high-quality names)
 * and speaks slightly slower for a calmer, more natural delivery.
 */
@Singleton
class TtsPoseAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val mp3Pack: Mp3PackPoseAudioPlayer,
) : PoseAudioPlayer {

    override val hasAudioPack: Boolean
        get() = mp3Pack.hasAudioPack

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureHumanVoice()
                isReady = true
            }
        }
    }

    override suspend fun playDescription(pose: YogaPose) {
        val mode = settingsDataStore.audioMode.first()
        if (mode == AudioMode.Off) return
        if (mode == AudioMode.Mp3Pack && mp3Pack.hasPose(pose.id)) {
            mp3Pack.playDescription(pose)
            return
        }
        withContext(Dispatchers.Main) {
            val engine = tts ?: return@withContext
            if (!isReady) {
                configureHumanVoice()
                isReady = true
            }
            val text = "${pose.ruName}. ${pose.ruDescription}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
        }
    }

    override suspend fun playGong() {
        mp3Pack.playGong()
    }

    override fun stop() {
        tts?.stop()
        mp3Pack.stop()
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        mp3Pack.release()
    }

    private fun configureHumanVoice() {
        val engine = tts ?: return
        val ru = Locale.forLanguageTag("ru-RU")
        engine.language = ru
        // Slightly slower + neutral pitch reads as calmer / more human for instructions.
        engine.setSpeechRate(0.88f)
        engine.setPitch(0.97f)

        val voices = runCatching { engine.voices }.getOrNull().orEmpty()
        val best = pickBestRussianVoice(voices)
        if (best != null) {
            runCatching { engine.voice = best }
                .onSuccess { Timber.d("TTS voice: ${best.name} quality=${best.quality}") }
                .onFailure { Timber.w(it, "Failed to set TTS voice ${best.name}") }
        }
    }

    private fun pickBestRussianVoice(voices: Set<Voice>): Voice? {
        val russian = voices.filter { it.locale.language.equals("ru", ignoreCase = true) }
        if (russian.isEmpty()) return null

        fun score(v: Voice): Int {
            var s = v.quality * 10
            val name = v.name.lowercase()
            if (listOf("neural", "natural", "wavenet", "studio", "premium", "enhanced", "hd")
                    .any { it in name }
            ) {
                s += 50
            }
            if (v.isNetworkConnectionRequired) s += 15 // often higher quality on-device fallbacks exist too
            if (v.latency == Voice.LATENCY_VERY_LOW || v.latency == Voice.LATENCY_LOW) s += 5
            // Prefer female instructional tone when the name hints at it (common on RU engines).
            if (listOf("female", "woman", "irina", "milena", "oksana", "jane").any { it in name }) {
                s += 8
            }
            return s
        }
        return russian.maxByOrNull(::score)
    }
}

/**
 * Plays pre-recorded MP3 files from the optional audio pack.
 */
@Singleton
class Mp3PackPoseAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val packDir: File by lazy {
        File(context.filesDir, AUDIO_PACK_DIR).apply { if (!exists()) mkdirs() }
    }

    val hasAudioPack: Boolean
        get() = packDir.exists() && packDir.listFiles()?.isNotEmpty() == true

    fun hasPose(poseId: String): Boolean = File(packDir, "$poseId.mp3").exists()

    suspend fun playDescription(pose: YogaPose) {
        val audioName = pose.audioAssetName ?: pose.id
        val file = File(packDir, "$audioName.mp3")
        if (!file.exists()) return
        playFile(file)
    }

    suspend fun playGong() {
        val file = File(packDir, GONG_FILE)
        if (!file.exists()) return
        playFile(file)
    }

    fun stop() {
        currentPlayer?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        currentPlayer = null
    }

    fun release() = stop()

    private var currentPlayer: android.media.MediaPlayer? = null

    private suspend fun playFile(file: File) = withContext(Dispatchers.Main) {
        stop()
        runCatching {
            val player = android.media.MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { release() }
                prepare()
                start()
            }
            currentPlayer = player
        }
    }

    companion object {
        private const val AUDIO_PACK_DIR = "audio-pack"
        private const val GONG_FILE = "gong.mp3"
    }
}
