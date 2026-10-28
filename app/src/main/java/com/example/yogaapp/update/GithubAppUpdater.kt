package com.example.yogaapp.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.yogaapp.BuildConfig
import com.example.yogaapp.R
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Checks the public GitHub Releases API for a newer APK and offers to install it.
 * Asset name must contain the version code: `yoga-app-{versionCode}-…apk`.
 * Android always asks the user to confirm the package installer step.
 *
 * Two entry points:
 *  - [check]           — silent, used by MainActivity.onResume. Shows the install dialog
 *                        only when an update is available. Skipped if already prompted
 *                        in this process.
 *  - [checkExplicit]   — used by Settings → «Проверить обновления». Always re-checks
 *                        (even after a previous dismissal) and returns a [CheckResult]
 *                        so the caller can show a Snackbar for the NoUpdate/Error cases.
 */
class GithubAppUpdater(
    private val activity: Activity,
) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val busy = AtomicBoolean(false)
    private var promptedThisProcess = false

    /** Silent background check used by MainActivity.onResume. */
    suspend fun check() {
        if (promptedThisProcess || !busy.compareAndSet(false, true)) return
        try {
            val update = withContext(Dispatchers.IO) { findUpdate() } ?: return
            promptedThisProcess = true
            val apk = withContext(Dispatchers.IO) { download(update) } ?: return
            withContext(Dispatchers.Main) { askAndInstall(update, apk) }
        } catch (t: Throwable) {
            Timber.w(t, "GitHub update check failed")
        } finally {
            busy.set(false)
        }
    }

    /**
     * Explicit check from the Settings screen. Always performs the network call (ignores
     * [promptedThisProcess]). Returns a [CheckResult] so the caller can reflect the
     * outcome in the UI. When an update is available, the install dialog is shown
     * exactly as in [check].
     */
    suspend fun checkExplicit(): CheckResult {
        if (!busy.compareAndSet(false, true)) return CheckResult.Busy
        return try {
            val update = withContext(Dispatchers.IO) { findUpdate() }
                ?: return CheckResult.NoUpdate
            promptedThisProcess = true
            val apk = withContext(Dispatchers.IO) { download(update) }
                ?: return CheckResult.Error
            withContext(Dispatchers.Main) { askAndInstall(update, apk) }
            CheckResult.Available(update.label)
        } catch (t: Throwable) {
            Timber.w(t, "GitHub explicit update check failed")
            CheckResult.Error
        } finally {
            busy.set(false)
        }
    }

    private fun findUpdate(): PendingUpdate? {
        val url =
            "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.w("GitHub releases HTTP %s", response.code)
                return null
            }
            val body = response.body?.string().orEmpty()
            val release = json.decodeFromString(GithubRelease.serializer(), body)
            if (release.draft || release.prerelease) return null
            val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return null
            val remoteCode = parseVersionCode(asset.name) ?: return null
            if (remoteCode <= BuildConfig.VERSION_CODE) return null
            return PendingUpdate(
                versionCode = remoteCode,
                label = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                downloadUrl = asset.browserDownloadUrl,
                fileName = asset.name,
            )
        }
    }

    private fun download(update: PendingUpdate): File? {
        val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, update.fileName)
        val request = Request.Builder()
            .url(update.downloadUrl)
            .header("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.w("APK download HTTP %s", response.code)
                return null
            }
            val body = response.body ?: return null
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        return target.takeIf { it.isFile && it.length() > 0L }
    }

    private fun askAndInstall(update: PendingUpdate, apk: File) {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_available_title)
            .setMessage(activity.getString(R.string.update_available_body, update.label))
            .setPositiveButton(R.string.update_install) { _, _ -> install(apk) }
            .setNegativeButton(R.string.action_cancel, null)
            .setCancelable(true)
            .show()
    }

    private fun install(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            activity.startActivity(settings)
            return
        }
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    private data class PendingUpdate(
        val versionCode: Int,
        val label: String,
        val downloadUrl: String,
        val fileName: String,
    )

    companion object {
        /** Release assets must look like `yoga-app-{versionCode}-{versionName}.apk`. */
        private val VERSION_CODE_IN_NAME =
            Regex("""^yoga-app-(\d+)-.+\.apk$""", RegexOption.IGNORE_CASE)

        fun parseVersionCode(fileName: String): Int? =
            VERSION_CODE_IN_NAME.find(fileName)?.groupValues?.get(1)?.toIntOrNull()
    }
}

/**
 * Result of an explicit update check, used by the Settings screen.
 * - [NoUpdate]  — release on GitHub is not newer than the installed build.
 * - [Available] — newer release found; the install dialog has been shown.
 * - [Error]     — network or API failure.
 * - [Busy]     — another check is already in flight; retry later.
 */
sealed interface CheckResult {
    data object NoUpdate : CheckResult
    data class Available(val label: String) : CheckResult
    data object Error : CheckResult
    data object Busy : CheckResult
}

