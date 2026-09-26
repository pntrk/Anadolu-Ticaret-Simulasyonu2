package com.example

import com.example.viewmodel.*

import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AdMobManager
import com.example.data.AppDatabase
import com.example.data.EconomicDataStore
import com.example.data.GameRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.GameViewModelFactory
import java.util.Locale
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@Composable
fun LocaleWrapper(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val locale = remember(languageCode) {
        if (languageCode.equals("en", ignoreCase = true)) Locale.ENGLISH else Locale("tr", "TR")
    }

    val configuration = remember(languageCode, context) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        config
    }

    val localizedContext = remember(languageCode, context) {
        val wrapper = android.view.ContextThemeWrapper(context, 0)
        wrapper.applyOverrideConfiguration(configuration)
        wrapper
    }

    val layoutDirection = if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides localizedContext,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    companion object {
        var currentActivity: ComponentActivity? = null
    }

    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = this

        // Clean up any residual or accumulated cache files (excluding system-critical folders like WebView and osmdroid) to free up disk space in the emulator environment safely
        try {
            val cacheDir = applicationContext.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name != "WebView" && file.name != "osmdroid" && file.name != "lib_shared_uid") {
                        file.deleteRecursively()
                    }
                }
            }
        } catch (_: Throwable) {}

        // Initialize Global App Exception Handler and Telemetry
        try {
            com.example.data.telemetry.AppExceptionHandler.install(applicationContext)
            com.example.data.telemetry.TelemetryService.addBreadcrumb("MainActivity.onCreate")
        } catch (_: Throwable) {}

        // Initialize App Dependency Container (Save Gateway & Server Time Provider)
        try {
            com.example.di.AppContainer.initialize(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "AppContainer init warning", e)
        }

        // Initialize Google Mobile Ads SDK (AdMob)
        try {
            AdMobManager.initialize(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "AdMobManager init warning", e)
        }

        // Initialize Google Play Games Services SDK v2
        try {
            com.example.data.PlayGamesManager.initialize(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "PlayGamesManager init warning", e)
        }

        // Initialize Google Play Billing Client
        try {
            com.example.data.billing.BillingManager.initialize(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "BillingManager init warning", e)
        }
        
        // Initialize Haptic Manager
        try {
            com.example.utils.HapticManager.init(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "HapticManager init warning", e)
        }

        // Register Context with Museum Heritage Manager
        try {
            com.example.data.MuseumHeritageManager.registerContext(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "MuseumHeritageManager registerContext warning", e)
        }

        // Initialize Local Game Notification Channels
        try {
            com.example.notification.LocalGameNotificationManager.initializeChannels(applicationContext)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "LocalGameNotificationManager init warning", e)
        }

        // Initialize Google Play In-App Update
        try {
            com.example.utils.InAppUpdateManager.initialize(applicationContext)
            com.example.utils.InAppUpdateManager.checkForUpdates(this, forceImmediate = false)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "InAppUpdateManager init warning", e)
        }

        val database = AppDatabase.getDatabase(this)
        val economicDataStore = EconomicDataStore(applicationContext)
        val repository = GameRepository(database.gameDao(), economicDataStore)

        gameViewModel = androidx.lifecycle.ViewModelProvider(this, GameViewModelFactory(repository))[GameViewModel::class.java]

        try {
            com.example.utils.GoogleAuthHelper.checkAndRestoreGoogleSession(this, gameViewModel)
        } catch (_: Throwable) {}

        enableEdgeToEdge()

        setContent {
            val selectedTheme by gameViewModel.selectedTheme.collectAsStateWithLifecycle()
            val selectedLanguage by gameViewModel.selectedLanguage.collectAsStateWithLifecycle()

            LocaleWrapper(languageCode = selectedLanguage) {
                MyApplicationTheme(
                    selectedThemeId = selectedTheme,
                    selectedLanguageCode = selectedLanguage
                ) {
                    MainScreen(gameViewModel = gameViewModel)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (com.example.utils.GoogleAuthHelper.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::gameViewModel.isInitialized) {
                gameViewModel.setAppForegroundState(true)
            }
        } catch (_: Throwable) {}
        try {
            com.example.utils.InAppUpdateManager.onResume(this)
        } catch (_: Throwable) {}
        try {
            com.example.data.billing.BillingManager.queryAndConsumeUnfinishedPurchases()
        } catch (_: Throwable) {}
        try {
            if (::gameViewModel.isInitialized) {
                gameViewModel.checkOfflineMuseumAuctions()
                gameViewModel.checkAndApplyRemoteAdminModifications()
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "onResume checkOfflineMuseumAuctions warning", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (::gameViewModel.isInitialized) {
                gameViewModel.saveEconomicDataToDataStore(immediate = true)
                gameViewModel.forceSyncCloudSaveToSupabase()
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "onPause save warning", e)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            if (::gameViewModel.isInitialized) {
                gameViewModel.setAppForegroundState(false)
                gameViewModel.saveEconomicDataToDataStore(immediate = true)
                gameViewModel.forceSyncCloudSaveToSupabase()
            }
            // Schedule intelligent retention push reminders (4h, 12h, 24h)
            com.example.notification.LocalGameNotificationManager.scheduleSmartRetentionPack()
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "onStop save warning", e)
        }
    }

    override fun onDestroy() {
        try {
            com.example.utils.InAppUpdateManager.onDestroy()
        } catch (_: Throwable) {}
        try {
            if (::gameViewModel.isInitialized) {
                gameViewModel.saveEconomicDataToDataStore(immediate = true)
                gameViewModel.forceSyncCloudSaveToSupabase()
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "onDestroy save warning", e)
        }
        super.onDestroy()
        if (currentActivity == this) {
            currentActivity = null
        }
    }
}
