package com.example.data.telemetry

import android.util.Log

/**
 * ManagerAutomationGuard:
 * Provides bulletproof execution wrappers and timer exception handlers for autonomous executives
 * (e.g. Zeynep Demir - Lojistik & Satış, Ahmet Kaya - Hazine, Canan Eren - Üretim, Banu Aydın - İK).
 *
 * Guarantees that no manager timer glitch, null reference, or arithmetic exception can crash
 * the background automation loop or block the UI thread.
 */
object ManagerAutomationGuard {

    const val TAG: String = "ManagerAutomationGuard"
    const val WARN_DURATION_THRESHOLD_MS: Long = 3000L

    /**
     * Executes an autonomous manager task within a safe, monitored sandbox.
     *
     * @param managerId Unique manager ID (e.g. "mgr_logistics")
     * @param managerName Human-readable manager name (e.g. "Zeynep Demir")
     * @param actionName Descriptive action name (e.g. "100 Ton Kota Satışı")
     * @param playerLevel Current player level
     * @param block Autonomous business logic suspend block
     * @return True if execution succeeded cleanly, False if an error was caught and isolated.
     */
    suspend inline fun executeManagerSafely(
        managerId: String,
        managerName: String,
        actionName: String,
        playerLevel: Int = 1,
        crossinline block: suspend () -> Unit
    ): Boolean {
        val startTime = System.currentTimeMillis()
        return try {
            block()
            val duration = System.currentTimeMillis() - startTime
            if (duration > WARN_DURATION_THRESHOLD_MS) {
                TelemetryService.recordEvent(
                    eventType = TelemetryEventType.UI_HANG_WARNING,
                    severity = TelemetrySeverity.WARN,
                    tag = "Manager_Performance_Warning",
                    message = "Otonom yönetici $managerName ($managerId) işlemi beklenenden uzun sürdü: ${duration}ms (Eylem: $actionName)",
                    playerLevel = playerLevel,
                    metadata = mapOf(
                        "managerId" to managerId,
                        "managerName" to managerName,
                        "actionName" to actionName,
                        "durationMs" to duration.toString()
                    )
                )
            }
            true
        } catch (t: Throwable) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Exception caught during autonomous execution of $managerName ($managerId): ${t.message}", t)

            // Dispatch error telemetry asynchronously
            TelemetryService.recordEvent(
                eventType = TelemetryEventType.MANAGER_AUTOMATION_ERROR,
                severity = TelemetrySeverity.ERROR,
                tag = "Manager_Timer_Exception",
                message = "Otonom yönetici $managerName ($managerId) zamanlayıcı/işlem hatası yakalandı: ${t.javaClass.simpleName} - ${t.message}",
                playerLevel = playerLevel,
                metadata = mapOf(
                    "managerId" to managerId,
                    "managerName" to managerName,
                    "actionName" to actionName,
                    "durationMs" to duration.toString(),
                    "exceptionClass" to t.javaClass.name
                ),
                throwable = t
            )
            false
        }
    }
}
