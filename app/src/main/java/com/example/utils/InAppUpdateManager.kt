package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.ui.components.NotificationType
import com.example.ui.components.SmartNotificationManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play In-App Update Helper.
 * Supports both Flexible (background download with restart prompt) and Immediate update flows.
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"
    const val REQUEST_CODE_IN_APP_UPDATE = 8801

    private var appUpdateManager: AppUpdateManager? = null
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private fun isPlayStoreAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("com.android.vending", 0)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Initializes the Play Core AppUpdateManager and registers listeners.
     */
    fun initialize(context: Context) {
        if (!isPlayStoreAvailable(context)) {
            Log.d(TAG, "Google Play Store is not installed/available. In-App Updates disabled.")
            return
        }
        try {
            val manager = AppUpdateManagerFactory.create(context.applicationContext)
            appUpdateManager = manager

            installStateUpdatedListener = InstallStateUpdatedListener { state: InstallState ->
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        _isUpdateDownloaded.value = true
                        Log.i(TAG, "In-app update downloaded successfully. Ready to install.")
                        SmartNotificationManager.show(
                            "📲 Yeni Güncelleme İndirildi! Uygulamayı yeniden başlatarak son sürüme geçebilirsiniz.",
                            "📲 New Update Downloaded! Restart the app to apply the latest features.",
                            NotificationType.SUCCESS
                        )
                    }
                    InstallStatus.DOWNLOADING -> {
                        val bytesDownloaded = state.bytesDownloaded()
                        val totalBytes = state.totalBytesToDownload()
                        Log.d(TAG, "Downloading update: $bytesDownloaded / $totalBytes bytes")
                    }
                    InstallStatus.INSTALLED -> {
                        _isUpdateDownloaded.value = false
                        _isUpdateAvailable.value = false
                        Log.i(TAG, "Update installation completed.")
                    }
                    InstallStatus.FAILED -> {
                        Log.w(TAG, "In-app update download failed (Error code: ${state.installErrorCode()})")
                    }
                    else -> {}
                }
            }

            manager.registerListener(installStateUpdatedListener!!)
            Log.d(TAG, "InAppUpdateManager initialized and listener registered.")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize AppUpdateManager safely: ${e.message}")
        }
    }

    /**
     * Checks Google Play for available app updates and starts either Flexible or Immediate update flow.
     */
    fun checkForUpdates(
        activity: Activity,
        forceImmediate: Boolean = false,
        onUpdateAvailable: ((AppUpdateInfo) -> Unit)? = null
    ) {
        if (!isPlayStoreAvailable(activity)) {
            Log.d(TAG, "Google Play Store is not available. Skipping update check.")
            return
        }
        try {
            val manager = appUpdateManager ?: AppUpdateManagerFactory.create(activity).also { appUpdateManager = it }
            val appUpdateInfoTask = manager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                val availability = appUpdateInfo.updateAvailability()
                val updateAvailable = availability == UpdateAvailability.UPDATE_AVAILABLE ||
                        availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

                _isUpdateAvailable.value = updateAvailable

                if (updateAvailable) {
                    onUpdateAvailable?.invoke(appUpdateInfo)
                    
                    val updateType = if (forceImmediate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        AppUpdateType.IMMEDIATE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        AppUpdateType.FLEXIBLE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        AppUpdateType.IMMEDIATE
                    } else {
                        null
                    }

                    if (updateType != null) {
                        try {
                            val options = AppUpdateOptions.newBuilder(updateType).build()
                            manager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                options,
                                REQUEST_CODE_IN_APP_UPDATE
                            )
                            Log.i(TAG, "Started in-app update flow ($updateType)")
                        } catch (e: Throwable) {
                            Log.w(TAG, "Failed to start update flow: ${e.message}")
                        }
                    }
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
            }.addOnFailureListener { e ->
                Log.d(TAG, "Check for update failed or not running in Play Store environment: ${e.message}")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "checkForUpdates exception: ${e.message}")
        }
    }

    /**
     * Completes flexible update installation by restarting the app.
     */
    fun completeUpdate() {
        try {
            appUpdateManager?.completeUpdate()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to complete update: ${e.message}")
        }
    }

    /**
     * Call on Activity onResume to handle pending updates or downloaded packages.
     */
    fun onResume(activity: Activity) {
        if (!isPlayStoreAvailable(activity)) return
        try {
            val manager = appUpdateManager ?: return
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume immediate update in progress
                    try {
                        manager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_IN_APP_UPDATE
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Resume immediate update error: ${e.message}")
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Clean up listeners when activity or application is destroyed.
     */
    fun onDestroy() {
        try {
            installStateUpdatedListener?.let {
                appUpdateManager?.unregisterListener(it)
            }
        } catch (_: Throwable) {}
    }
}
