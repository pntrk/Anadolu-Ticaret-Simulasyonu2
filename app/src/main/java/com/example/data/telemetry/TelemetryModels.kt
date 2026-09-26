package com.example.data.telemetry

import androidx.annotation.Keep

@Keep
enum class TelemetrySeverity {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
}

@Keep
enum class TelemetryEventType {
    ONBOARDING_FRICTION,
    UI_HANG_WARNING,
    MANAGER_AUTOMATION_ERROR,
    LOGICAL_EDGE_CASE,
    API_FAILURE,
    UNCAUGHT_CRASH
}

/**
 * Lightweight, non-blocking telemetry event payload.
 */
@Keep
data class TelemetryPayload(
    val eventId: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val eventType: TelemetryEventType,
    val severity: TelemetrySeverity,
    val tag: String,
    val message: String,
    val playerLevel: Int = 1,
    val metadata: Map<String, String> = emptyMap(),
    val stackTraceSnippet: String? = null,
    val breadcrumbs: List<String> = emptyList()
)

/**
 * Onboarding friction record for players with Level <= 2.
 */
@Keep
data class OnboardingFrictionRecord(
    val playerLevel: Int,
    val frictionType: String,
    val screenName: String,
    val actionAttempted: String,
    val rootCause: String,
    val details: Map<String, String> = emptyMap()
)

/**
 * Autonomous Manager (e.g. Zeynep Demir, Ahmet Kaya) execution error diagnostic.
 */
@Keep
data class ManagerErrorRecord(
    val managerId: String,
    val managerName: String,
    val actionName: String,
    val executionDurationMs: Long,
    val errorMessage: String,
    val exceptionClass: String,
    val playerLevel: Int
)
