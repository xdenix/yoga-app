package com.example.yogaapp.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Asks Google Play for a newer version and starts the immediate update flow.
 * If this install did not come from Play (a sideloaded APK), the request fails and the app continues.
 */
class PlayAppUpdater(
    context: Context,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
) {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private var promptedThisProcess = false

    fun check() {
        manager.appUpdateInfo
            .addOnSuccessListener { info -> onInfo(info) }
            .addOnFailureListener { /* Play is absent or this build was not installed from it. */ }
    }

    private fun onInfo(info: AppUpdateInfo) {
        val inProgress = info.updateAvailability() ==
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        if (inProgress || (available && !promptedThisProcess)) {
            promptedThisProcess = true
            startImmediate(info)
            return
        }
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            manager.completeUpdate()
        }
    }

    private fun startImmediate(info: AppUpdateInfo) {
        runCatching {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
        }
    }
}
