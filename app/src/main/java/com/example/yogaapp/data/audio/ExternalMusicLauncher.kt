package com.example.yogaapp.data.audio

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens an external music app (Yandex Music preferred) so the user can play
 * background music while YogaApp stays silent.
 */
object ExternalMusicLauncher {

    fun openYandexMusic(context: Context): Boolean {
        val candidates = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("yandexmusic://home")),
            Intent().setClassName(
                "ru.yandex.music",
                "ru.yandex.music.main.MainActivity",
            ),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://music.yandex.ru")),
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) {
                return true
            }
        }
        return false
    }

    fun openSystemMusicPicker(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
