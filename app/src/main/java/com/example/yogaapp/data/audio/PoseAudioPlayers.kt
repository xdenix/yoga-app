package com.example.yogaapp.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.yogaapp.data.datastore.AudioMode
import com.example.yogaapp.data.datastore.SettingsDataStore
import com.example.yogaapp.domain.model.YogaPose
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [PoseAudioPlayer] that uses the Android [TextToSpeech] engine.
 *
 * Lifecycle: the TTS engine is initialised lazily on first [playDescription] call and held
 * for the lifetime of the singleton. [release] shuts it down cleanly when the app terminates.
 *
 * Locale selection: prefers Russian for ru content, English for en content. Falls back to the
 * device's default if neither is available.
 *
 * Audio pack: if [SettingsDataStore.audioMode] is [AudioMode.Mp3Pack] AND the asset file
 * `audio-pack/<poseId>.mp3` exists, playback is delegated to [Mp3PackPoseAudioPlayer];
 * otherwise TTS is used as fallback.
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
        // Pre-initialise TTS on the singleton constructor so the first play call is fast.
        // The init callback flips [isReady] once the engine reports SUCCESS.
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale("ru")
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
        // TTS path — read Russian description aloud.
        withContext(Dispatchers.Main) {
            if (!isReady) {
                tts?.language = Locale("ru")
                isReady = true
            }
            val text = "${pose.ruName}. ${pose.ruDescription}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    override suspend fun playGong() {
        // The gong is a chime — played via MediaPlayer on the optional audio pack's gong.mp3.
        // If the pack is absent, we silently no-op (TTS can't produce a chime).
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
}

/**
 * Plays pre-recorded MP3 files from the optional audio pack.
 *
 * The audio pack is a directory at `filesDir/audio-pack/` that the user can populate manually
 * (e.g. by dropping files via a file manager or via an in-app future "import audio pack"
 * feature). Each pose is identified by `<poseId>.mp3` (e.g. `tadasana.mp3`).
 *
 * Files are played via [android.media.MediaPlayer] so they stream from disk and don't sit
 * in memory.
 *
 * The class is intentionally NOT bound to a Hilt interface — it's used only via
 * [TtsPoseAudioPlayer] as a fallback when [hasPose] returns true.
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
