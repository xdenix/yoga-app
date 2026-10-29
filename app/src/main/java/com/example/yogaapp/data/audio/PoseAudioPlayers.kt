package com.example.yogaapp.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
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
 * Default implementation of [PoseAudioPlayer].
 *
 * Chinese ROM compatibility (Xiaomi CN, HarmonyOS, OriginOS, ColorOS):
 *  1. Bundled offline Russian voice (`assets/voice_ru/<id>.mp3`) is used whenever available —
 *     works on 100% of devices without Google Speech Services or system Russian TTS data.
 *  2. Android [TextToSpeech] scans installed engines (`com.google.android.tts`, RHVoice,
 *     Samsung, etc.) if the default Chinese system engine lacks Russian (`ru-RU`).
 */
@Singleton
class TtsPoseAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val mp3Pack: Mp3PackPoseAudioPlayer,
    private val bundledRuVoice: BundledRuVoicePlayer,
    private val sessionBell: SessionBellPlayer,
) : PoseAudioPlayer {

    override val hasAudioPack: Boolean
        get() = mp3Pack.hasAudioPack || bundledRuVoice.hasBundledVoice

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var triedFallbackEngines = false

    init {
        initTts(preferredEngine = null)
    }

    private fun initTts(preferredEngine: String?) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hasRu = configureHumanVoice()
                isReady = hasRu
                if (!hasRu && !triedFallbackEngines) {
                    triedFallbackEngines = true
                    trySwitchToRussianCapableEngine()
                }
            } else if (!triedFallbackEngines) {
                triedFallbackEngines = true
                trySwitchToRussianCapableEngine()
            }
        }
        tts = if (preferredEngine != null) {
            TextToSpeech(context, listener, preferredEngine)
        } else {
            TextToSpeech(context, listener)
        }
    }

    /**
     * On Chinese firmwares the default TTS is often XiaoAi / iFlytek / Vivo TTS without ru-RU.
     * If Google Speech Services or RHVoice is present on the device, switch to it.
     */
    private fun trySwitchToRussianCapableEngine() {
        val engine = tts ?: return
        val installed = runCatching { engine.engines.map { it.name }.toSet() }.getOrDefault(emptySet())
        val candidate = PREFERRED_RU_ENGINES.firstOrNull { it in installed } ?: return
        Timber.i("Switching TTS engine to Russian-capable %s", candidate)
        runCatching {
            engine.shutdown()
            initTts(preferredEngine = candidate)
        }.onFailure { Timber.w(it, "Failed to switch TTS engine to %s", candidate) }
    }

    override suspend fun playDescription(pose: YogaPose) {
        val mode = settingsDataStore.audioMode.first()
        if (mode == AudioMode.Off) return

        // 1. User-supplied external audio pack in files/audio-pack/
        if (mode == AudioMode.Mp3Pack && mp3Pack.hasPose(pose.id)) {
            mp3Pack.playDescription(pose)
            return
        }

        // 2. Built-in offline Russian voice in assets/voice_ru/ — guarantees Russian speech
        //    on Chinese ROMs even when no Russian TTS engine is installed.
        if (bundledRuVoice.playPose(pose)) {
            return
        }

        // 3. Fallback to Android TextToSpeech (ru-RU).
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

        // Prefer bundled offline Russian countdown clip so Chinese ROMs always speak Russian.
        if (bundledRuVoice.playCountdown(secondsLeft)) {
            tts?.stop()
            return
        }

        val word = COUNTDOWN_RU[secondsLeft] ?: secondsLeft.toString()
        withContext(Dispatchers.Main) {
            val engine = tts ?: return@withContext
            ensureReady(engine)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            engine.speak(word, TextToSpeech.QUEUE_FLUSH, params, "countdown-$secondsLeft")
        }
    }

    override fun stop() {
        tts?.stop()
        mp3Pack.stop()
        bundledRuVoice.stop()
        sessionBell.stop()
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        mp3Pack.release()
        bundledRuVoice.release()
        sessionBell.release()
    }

    private fun ensureReady(engine: TextToSpeech) {
        if (!isReady) {
            isReady = configureHumanVoice()
        }
    }

    /**
     * Configures Russian locale & voice. Returns `true` if the engine genuinely supports Russian.
     */
    private fun configureHumanVoice(): Boolean {
        val engine = tts ?: return false
        val ru = Locale.forLanguageTag("ru-RU")
        val avail = runCatching { engine.isLanguageAvailable(ru) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (avail < TextToSpeech.LANG_AVAILABLE) {
            Timber.w("TTS engine does not support ru-RU (code=%d)", avail)
            return false
        }
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
        return true
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

        private val PREFERRED_RU_ENGINES = listOf(
            "com.google.android.tts",
            "org.OlgaYakovleva.rhvoice.android",
            "com.yandex.speechkit.tts",
            "com.samsung.SMT",
        )
    }
}

/**
 * Plays bundled Russian voice MP3s from `assets/voice_ru/` so Russian speech works offline
 * on any Android firmware (including Chinese ROMs without Russian system TTS).
 */
@Singleton
class BundledRuVoicePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val availableAssets: Set<String> by lazy {
        runCatching { context.assets.list(VOICE_DIR)?.toSet().orEmpty() }.getOrDefault(emptySet())
    }

    val hasBundledVoice: Boolean
        get() = availableAssets.isNotEmpty()

    private var currentPlayer: MediaPlayer? = null

    suspend fun playPose(pose: YogaPose): Boolean {
        val suffix = when {
            pose.ruName.endsWith("(справа)") -> "_right"
            pose.ruName.endsWith("(слева)") -> "_left"
            else -> ""
        }
        val preferred = "${pose.id}$suffix.mp3"
        val fallback = "${pose.id}.mp3"
        val fileName = when {
            preferred in availableAssets -> preferred
            fallback in availableAssets -> fallback
            else -> return false
        }
        return playAsset("$VOICE_DIR/$fileName")
    }

    suspend fun playCountdown(secondsLeft: Int): Boolean {
        val fileName = "count_$secondsLeft.mp3"
        if (fileName !in availableAssets) return false
        return playAsset("$VOICE_DIR/$fileName")
    }

    private suspend fun playAsset(assetPath: String): Boolean = withContext(Dispatchers.Main) {
        stop()
        runCatching {
            val afd = context.assets.openFd(assetPath)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener {
                    runCatching { it.release() }
                    if (currentPlayer === it) currentPlayer = null
                }
                prepare()
                start()
            }
            currentPlayer = mp
            true
        }.getOrElse {
            Timber.w(it, "Bundled RU voice failed: %s", assetPath)
            false
        }
    }

    fun stop() {
        currentPlayer?.let {
            runCatching {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        currentPlayer = null
    }

    fun release() = stop()

    companion object {
        private const val VOICE_DIR = "voice_ru"
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
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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
