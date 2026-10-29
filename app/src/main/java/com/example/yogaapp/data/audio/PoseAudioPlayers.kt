package com.example.yogaapp.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.AudioManager
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
    private val sessionBell: SessionBellPlayer,
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
            ensureReady(engine)
            val text = "${pose.ruName}. ${pose.ruDescription}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
        }
    }

    override suspend fun playGong() {
        // Prefer optional audio-pack gong; otherwise bundled session bell / tone.
        if (mp3Pack.hasGong()) {
            mp3Pack.playGong()
        } else {
            sessionBell.play()
        }
    }

    override suspend fun speakCountdown(secondsLeft: Int) {
        if (secondsLeft !in 1..5) return
        val mode = settingsDataStore.audioMode.first()
        if (mode == AudioMode.Off) return
        val word = COUNTDOWN_RU[secondsLeft] ?: secondsLeft.toString()
        withContext(Dispatchers.Main) {
            val engine = tts ?: return@withContext
            ensureReady(engine)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            // Flush so the number stays in sync with the timer tick.
            engine.speak(word, TextToSpeech.QUEUE_FLUSH, params, "countdown-$secondsLeft")
        }
    }

    override fun stop() {
        tts?.stop()
        mp3Pack.stop()
        sessionBell.stop()
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        mp3Pack.release()
        sessionBell.release()
    }

    private fun ensureReady(engine: TextToSpeech) {
        if (!isReady) {
            configureHumanVoice()
            isReady = true
        }
    }

    private fun configureHumanVoice() {
        val engine = tts ?: return
        val ru = Locale.forLanguageTag("ru-RU")
        engine.language = ru
        engine.setSpeechRate(0.92f)
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
            if (v.isNetworkConnectionRequired) s += 15
            if (v.latency == Voice.LATENCY_VERY_LOW || v.latency == Voice.LATENCY_LOW) s += 5
            if (listOf("female", "woman", "irina", "milena", "oksana", "jane").any { it in name }) {
                s += 8
            }
            return s
        }
        return russian.maxByOrNull(::score)
    }

    companion object {
        private val COUNTDOWN_RU = mapOf(
            5 to "пять",
            4 to "четыре",
            3 to "три",
            2 to "два",
            1 to "один",
        )
    }
}

/**
 * Short session chime from `assets/audio/session_bell.wav`, with [ToneGenerator] fallback.
 */
@Singleton
class SessionBellPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null

    suspend fun play() = withContext(Dispatchers.Main) {
        stop()
        val played = runCatching {
            val afd = context.assets.openFd(BELL_ASSET)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener {
                    runCatching { it.release() }
                    if (player === it) player = null
                }
                prepare()
                start()
            }
            player = mp
            true
        }.getOrElse {
            Timber.w(it, "session bell asset failed")
            false
        }
        if (!played) playToneFallback()
    }

    fun stop() {
        player?.let {
            runCatching {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        player = null
    }

    fun release() = stop()

    private fun playToneFallback() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 280)
            // Release after tone duration.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { tg.release() }
            }, 400)
        }.onFailure { Timber.w(it, "tone fallback failed") }
    }

    companion object {
        private const val BELL_ASSET = "audio/session_bell.wav"
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

    fun hasGong(): Boolean = File(packDir, GONG_FILE).exists()

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

    private var currentPlayer: MediaPlayer? = null

    private suspend fun playFile(file: File) = withContext(Dispatchers.Main) {
        stop()
        runCatching {
            val player = MediaPlayer().apply {
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
