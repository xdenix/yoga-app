package com.example.yogaapp.data.audio

import com.example.yogaapp.domain.model.YogaPose

/**
 * Contract for playing pose descriptions aloud. The implementation is selected at runtime
 * based on the user's [com.example.yogaapp.data.datastore.AudioMode] preference:
 *  - [TtsPoseAudioPlayer] (default) — synthesises speech from the pose's text description.
 *  - [Mp3PackPoseAudioPlayer] (when the audio pack is installed) — plays the recorded MP3.
 *
 * Why a contract: the UI never branches on audio mode. It just calls [playDescription],
 * [playGong], [stop] and the right backend is chosen by Hilt.
 */
interface PoseAudioPlayer {

    /** Whether the audio pack files are physically present in the app's assets. */
    val hasAudioPack: Boolean

    /** Speak the pose's description (TTS) or play the recorded MP3 (if pack is installed). */
    suspend fun playDescription(pose: YogaPose)

    /** Play a short chime (used at pose start/end). No-op when the chime asset is missing. */
    suspend fun playGong()

    /** Stop any in-flight playback immediately. */
    fun stop()

    /** Release native resources. Called from [com.example.yogaapp.YogaApp.onTerminate]. */
    fun release()
}
