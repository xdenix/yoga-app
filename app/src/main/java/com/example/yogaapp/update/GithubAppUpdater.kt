package com.example.yogaapp.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.yogaapp.BuildConfig
import com.example.yogaapp.R
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Checks the public GitHub Releases API for a newer APK and offers to install it.
 * Asset name must contain the version code: `yoga-app-{versionCode}-…apk`.
 *
 * Auto-[check] (MainActivity.onResume): delayed + retried — cold start often has no
 * network yet, which previously failed silently and never showed a dialog.
 * [checkExplicit] (Settings button) always re-checks and surfaces errors in the UI.
 *
 * Call [onHostResumed] from [Activity.onResume] so install / deferred dialog continue
 * after unknown-sources settings or when the activity becomes resumed again.
 */
class GithubAppUpdater(
    private val activity: Activity,
) {
    private val apiClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val busy = AtomicBoolean(false)
    private var promptedThisProcess = false
    private val pendingInstallApk = AtomicReference<File?>(null)
    private val deferredPrompt = AtomicReference<Pair<PendingUpdate, File>?>(null)

    /** Retry package install / show deferred update dialog when the host is resumed. */
    fun onHostResumed() {
        val apk = pendingInstallApk.get()
        if (apk != null) {
            if (!apk.isFile || apk.length() <= 0L) {
                pendingInstallApk.set(null)
            } else if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                activity.packageManager.canRequestPackageInstalls()
            ) {
                pendingInstallApk.set(null)
                install(apk, rememberIfNeedPermission = false)
            }
        }
        deferredPrompt.getAndSet(null)?.let { (update, file) ->
            if (file.isFile && file.length() > 0L && isUiReady()) {
                promptedThisProcess = true
                askAndInstall(update, file)
            } else if (file.isFile && file.length() > 0L) {
                deferredPrompt.set(update to file)
            }
        }
    }

    /**
     * Silent background check used by MainActivity.onResume.
     * Waits briefly for splash/network, retries a few times, then shows the dialog
     * when the activity is resumed (otherwise defers until [onHostResumed]).
     */
    suspend fun check() {
        if (promptedThisProcess) return
        if (!busy.compareAndSet(false, true)) return
        try {
            // Let splash finish and connectivity settle after cold start.
            delay(AUTO_CHECK_INITIAL_DELAY_MS)
            if (promptedThisProcess) return

            val update = run {
                repeat(AUTO_CHECK_ATTEMPTS) { attempt ->
                    when (val found = withContext(Dispatchers.IO) { findUpdate() }) {
                        is FindOutcome.UpToDate -> return
                        is FindOutcome.Failed -> {
                            Timber.w("GitHub auto-check failed (%d): %s", attempt + 1, found.reason)
                            if (attempt < AUTO_CHECK_ATTEMPTS - 1) {
                                delay(AUTO_CHECK_RETRY_DELAY_MS * (attempt + 1))
                            }
                        }
                        is FindOutcome.Available -> return@run found.update
                    }
                }
                return
            }

            var apk: File? = null
            for (attempt in 0 until AUTO_CHECK_ATTEMPTS) {
                apk = withContext(Dispatchers.IO) { download(update) }
                if (apk != null) break
                Timber.w("GitHub auto-download failed (attempt %d)", attempt + 1)
                if (attempt < AUTO_CHECK_ATTEMPTS - 1) {
                    delay(AUTO_CHECK_RETRY_DELAY_MS * (attempt + 1))
                }
            }
            val file = apk
            if (file == null) {
                // Last resort: same browser offer as the Settings button path.
                withContext(Dispatchers.Main) {
                    if (isUiReady()) offerBrowserDownload(update)
                }
                return
            }

            withContext(Dispatchers.Main) {
                if (isUiReady()) {
                    promptedThisProcess = true
                    askAndInstall(update, file)
                } else {
                    deferredPrompt.set(update to file)
                    Timber.d("Update dialog deferred until next resume")
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "GitHub update check failed")
        } finally {
            busy.set(false)
        }
    }

    /**
     * Explicit check from the Settings screen. Always performs the network call (ignores
     * [promptedThisProcess]). Returns a [CheckResult] so the caller can reflect the
     * outcome in the UI.
     */
    suspend fun checkExplicit(): CheckResult {
        if (!busy.compareAndSet(false, true)) return CheckResult.Busy
        return try {
            when (val found = withContext(Dispatchers.IO) { findUpdate() }) {
                is FindOutcome.UpToDate -> CheckResult.NoUpdate
                is FindOutcome.Failed -> {
                    Timber.w("GitHub update find failed: %s", found.reason)
                    CheckResult.Error
                }
                is FindOutcome.Available -> {
                    val apk = withContext(Dispatchers.IO) { download(found.update) }
                    if (apk == null) {
                        withContext(Dispatchers.Main) {
                            offerBrowserDownload(found.update)
                        }
                        return CheckResult.Error
                    }
                    promptedThisProcess = true
                    withContext(Dispatchers.Main) { askAndInstall(found.update, apk) }
                    CheckResult.Available(found.update.label)
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "GitHub explicit update check failed")
            CheckResult.Error
        } finally {
            busy.set(false)
        }
    }

    private fun isUiReady(): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        val owner = activity as? LifecycleOwner ?: return true
        return owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    private fun findUpdate(): FindOutcome {
        val url =
            "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", userAgent())
            .build()
        return try {
            apiClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return FindOutcome.Failed("releases HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return FindOutcome.Failed("empty body")
                val release = json.decodeFromString(GithubRelease.serializer(), body)
                if (release.draft || release.prerelease) {
                    return FindOutcome.Failed("latest is draft/prerelease")
                }
                val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: return FindOutcome.Failed("no apk asset")
                val remoteCode = parseVersionCode(asset.name)
                    ?: return FindOutcome.Failed("bad asset name: ${asset.name}")
                if (remoteCode <= BuildConfig.VERSION_CODE) {
                    return FindOutcome.UpToDate
                }
                FindOutcome.Available(
                    PendingUpdate(
                        versionCode = remoteCode,
                        label = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                        downloadUrl = asset.browserDownloadUrl,
                        fileName = asset.name,
                    ),
                )
            }
        } catch (t: Throwable) {
            Timber.w(t, "findUpdate failed")
            FindOutcome.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

    private fun download(update: PendingUpdate): File? {
        val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, update.fileName)
        val request = Request.Builder()
            .url(update.downloadUrl)
            .header("User-Agent", userAgent())
            .header("Accept", "application/octet-stream,*/*")
            .build()
        return try {
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("APK download HTTP %s", response.code)
                    return null
                }
                val body = response.body ?: return null
                target.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            target.takeIf { it.isFile && it.length() > 0L }.also {
                if (it == null) Timber.w("APK download empty file")
            }
        } catch (t: Throwable) {
            Timber.w(t, "APK download failed")
            target.delete()
            null
        }
    }

    private fun askAndInstall(update: PendingUpdate, apk: File) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_available_title)
            .setMessage(activity.getString(R.string.update_available_body, update.label))
            .setPositiveButton(R.string.update_install) { _, _ ->
                install(apk, rememberIfNeedPermission = true)
            }
            .setNeutralButton(R.string.update_open_browser) { _, _ ->
                openInBrowser(update.downloadUrl)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setCancelable(true)
            .show()
    }

    private fun offerBrowserDownload(update: PendingUpdate) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_available_title)
            .setMessage(activity.getString(R.string.update_download_failed_body, update.label))
            .setPositiveButton(R.string.update_open_browser) { _, _ ->
                openInBrowser(update.downloadUrl)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun install(apk: File, rememberIfNeedPermission: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            if (rememberIfNeedPermission) {
                pendingInstallApk.set(apk)
            }
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            activity.startActivity(settings)
            return
        }
        pendingInstallApk.set(null)
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

    private fun openInBrowser(url: String) {
        runCatching {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { Timber.w(it, "open browser failed") }
    }

    private fun userAgent(): String =
        "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (Android update check)"

    private sealed interface FindOutcome {
        data class Available(val update: PendingUpdate) : FindOutcome
        data object UpToDate : FindOutcome
        data class Failed(val reason: String) : FindOutcome
    }

    private data class PendingUpdate(
        val versionCode: Int,
        val label: String,
        val downloadUrl: String,
        val fileName: String,
    )

    companion object {
        private const val AUTO_CHECK_INITIAL_DELAY_MS = 2_500L
        private const val AUTO_CHECK_RETRY_DELAY_MS = 2_000L
        private const val AUTO_CHECK_ATTEMPTS = 3

        private val VERSION_CODE_IN_NAME =
            Regex("""^yoga-app-(\d+)-.+\.apk$""", RegexOption.IGNORE_CASE)

        fun parseVersionCode(fileName: String): Int? =
            VERSION_CODE_IN_NAME.find(fileName)?.groupValues?.get(1)?.toIntOrNull()
    }
}

sealed interface CheckResult {
    data object NoUpdate : CheckResult
    data class Available(val label: String) : CheckResult
    data object Error : CheckResult
    data object Busy : CheckResult
}
