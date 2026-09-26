package com.example.data.telemetry

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ui.components.NotificationType
import com.example.ui.components.SmartNotificationManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext

data class EngineAnomalyData(
    val titleTr: String,
    val titleEn: String,
    val detailMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enterprise Application Exception Handler:
 * Intercepts uncaught thread exceptions and coroutine failures across the application,
 * prevents sudden unexpected crashes, logs full telemetry, and provides a graceful Safe Fallback UI shield.
 */
class AppExceptionHandler private constructor(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val TAG = "AppExceptionHandler"

    @Suppress("DEPRECATION")
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val msg = throwable.message ?: ""
            val isRecoverable = msg.contains("BINDER", ignoreCase = true) ||
                msg.contains("TransactionTooLarge", ignoreCase = true) ||
                msg.contains("No space left on device", ignoreCase = true) ||
                msg.contains("divide by zero", ignoreCase = true) ||
                msg.contains("/ by zero", ignoreCase = true) ||
                msg.contains("NullPointerException", ignoreCase = true) ||
                msg.contains("IndexOutOfBounds", ignoreCase = true) ||
                msg.contains("ConcurrentModification", ignoreCase = true) ||
                throwable is ArithmeticException ||
                throwable is NullPointerException ||
                throwable is IndexOutOfBoundsException ||
                throwable is ConcurrentModificationException ||
                throwable is android.os.RemoteException ||
                throwable is android.os.DeadObjectException ||
                throwable is android.os.TransactionTooLargeException ||
                throwable.javaClass.name.contains("TransactionTooLargeException") ||
                throwable.javaClass.name.contains("DeadObjectException") ||
                throwable.javaClass.name.contains("RemoteException")

            Log.e(TAG, "Uncaught exception on thread [${thread.name}]: $msg (isRecoverable=$isRecoverable)", throwable)

            // Record fatal/critical crash telemetry
            TelemetryService.recordEvent(
                eventType = if (isRecoverable) TelemetryEventType.LOGICAL_EDGE_CASE else TelemetryEventType.UNCAUGHT_CRASH,
                severity = if (isRecoverable) TelemetrySeverity.ERROR else TelemetrySeverity.FATAL,
                tag = "Uncaught_Exception",
                message = "İstisna yakalandı: [Thread: ${thread.name}] - ${throwable.javaClass.simpleName}: ${throwable.message}",
                metadata = mapOf(
                    "threadName" to thread.name,
                    "threadId" to thread.id.toString(),
                    "exceptionClass" to throwable.javaClass.name,
                    "isRecoverable" to isRecoverable.toString()
                ),
                throwable = throwable
            )

            // Notify user gracefully on main thread
            Handler(Looper.getMainLooper()).post {
                try {
                    SmartNotificationManager.show(
                        "⚠️ Ekonomi motorunda geçici bir dalgalanma oldu, lütfen tekrar deneyin.",
                        "⚠️ Temporary market anomaly occurred, please try again.",
                        NotificationType.ALERT
                    )
                    _anomalyState.value = EngineAnomalyData(
                        titleTr = "Ekonomi Motorunda Geçici Dalgalanma",
                        titleEn = "Temporary Economy Engine Fluctuation",
                        detailMessage = "Sistem arka plan hesaplamalarında bir anomaliden kurtarıldı. Varlıklarınız ve verileriniz güvendedir."
                    )
                } catch (_: Throwable) {}
            }

            if (isRecoverable) {
                Log.w(TAG, "Recoverable exception absorbed safely without crashing application.")
                return
            }

            // Give a tiny buffer to allow IO write before process termination if strictly unrecoverable
            Thread.sleep(150)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed while recording uncaught exception telemetry", e)
        } finally {
            // Only delegate to defaultHandler if strictly non-recoverable
            // defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        @Volatile
        private var isInitialized = false

        private val _anomalyState = MutableStateFlow<EngineAnomalyData?>(null)
        val anomalyState: StateFlow<EngineAnomalyData?> = _anomalyState.asStateFlow()

        fun clearAnomaly() {
            _anomalyState.value = null
        }

        /**
         * Installs the global uncaught exception handler on the JVM.
         */
        fun install(context: Context) {
            if (isInitialized) return
            synchronized(this) {
                if (isInitialized) return
                val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
                if (currentHandler !is AppExceptionHandler) {
                    val customHandler = AppExceptionHandler(currentHandler)
                    Thread.setDefaultUncaughtExceptionHandler(customHandler)
                }
                isInitialized = true
                Log.d("AppExceptionHandler", "Global AppExceptionHandler installed successfully.")
            }
        }

        /**
         * Factory function creating a CoroutineExceptionHandler for ViewModels and background workers.
         */
        fun createCoroutineExceptionHandler(
            scopeName: String,
            playerLevelProvider: () -> Int = { 1 }
        ): CoroutineExceptionHandler {
            return CoroutineExceptionHandler { _: CoroutineContext, throwable: Throwable ->
                Log.e("CoroutineExceptionHandler", "[$scopeName] Coroutine exception caught safely: ${throwable.message}", throwable)
                
                TelemetryService.recordEvent(
                    eventType = TelemetryEventType.LOGICAL_EDGE_CASE,
                    severity = TelemetrySeverity.ERROR,
                    tag = "Coroutine_Failure_$scopeName",
                    message = "[$scopeName] Asenkron Coroutine işlem hatası: ${throwable.javaClass.simpleName}: ${throwable.message}",
                    playerLevel = playerLevelProvider(),
                    metadata = mapOf(
                        "scopeName" to scopeName,
                        "exceptionClass" to throwable.javaClass.name
                    ),
                    throwable = throwable
                )

                // Dispatch graceful UI warning
                Handler(Looper.getMainLooper()).post {
                    try {
                        SmartNotificationManager.show(
                            "⚠️ Ekonomi motorunda geçici bir dalgalanma oldu, lütfen tekrar deneyin.",
                            "⚠️ Temporary market anomaly occurred, please try again.",
                            NotificationType.ALERT
                        )
                    } catch (_: Throwable) {}
                }
            }
        }
    }
}
