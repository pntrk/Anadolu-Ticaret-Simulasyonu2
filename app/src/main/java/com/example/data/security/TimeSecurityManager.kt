package com.example.data.security

import android.os.SystemClock
import android.util.Log
import com.example.data.CloudServerTimeManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Enterprise Time Security & Anti-Clock-Tampering Engine.
 *
 * Prevents device clock manipulation attacks (advancing phone clock to cheat offline production,
 * deposit yields, Ar-Ge research timers, or rolling back clock to replay daily rewards).
 */
object TimeSecurityManager {

    private const val TAG = "TimeSecurityManager"
    const val MAX_OFFLINE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 Hours Max Offline Accumulation
    const val MIN_OFFLINE_DURATION_MS = 60 * 1000L // Minimum 1 minute to trigger offline calculations
    private const val TAMPER_TOLERANCE_MS = 30_000L // 30 seconds clock drift tolerance

    @Volatile
    private var lastRecordedTimeMs: Long = 0L

    @Volatile
    private var isClockTamperingDetected: Boolean = false

    val isTampered: Boolean
        get() = isClockTamperingDetected || CloudServerTimeManager.isClockTampered

    /**
     * Returns a reliable, anti-cheat protected current timestamp in milliseconds.
     * Uses synchronized cloud server time anchored to monotonic hardware uptime.
     */
    fun getSecureCurrentTimeMs(): Long {
        val serverTime = CloudServerTimeManager.getServerTimeMs()
        val wallTime = System.currentTimeMillis()

        val timeToUse = if (serverTime > 0L) serverTime else wallTime

        // Anti-rollback check: Time should never flow backwards
        if (lastRecordedTimeMs > 0L && timeToUse < (lastRecordedTimeMs - TAMPER_TOLERANCE_MS)) {
            Log.w(TAG, "Clock rollback detected! TimeToUse=$timeToUse < LastRecorded=$lastRecordedTimeMs")
            isClockTamperingDetected = true
            return lastRecordedTimeMs + 1000L // Advance forward monotonically
        }

        lastRecordedTimeMs = max(lastRecordedTimeMs, timeToUse)
        return timeToUse
    }

    /**
     * Validates and computes a strictly bounded, anti-tampered offline duration in milliseconds.
     *
     * @param lastSavedTimeMs Timestamp when the game was last saved.
     * @param lastSavedElapsedRealtime Monotonic hardware uptime when last saved (if available).
     * @return Validated offline duration in milliseconds, safely clamped between 0 and MAX_OFFLINE_DURATION_MS.
     */
    fun validateAndComputeOfflineDuration(
        lastSavedTimeMs: Long,
        lastSavedElapsedRealtime: Long = 0L
    ): Long {
        if (lastSavedTimeMs <= 0L) return 0L

        val secureNow = getSecureCurrentTimeMs()

        // 1. Clock Rollback Detection (User set device clock backwards)
        if (secureNow < lastSavedTimeMs) {
            Log.w(TAG, "Negative offline duration detected (Device clock rewound): Now=$secureNow < LastSaved=$lastSavedTimeMs. Resetting delta to 0.")
            isClockTamperingDetected = true
            return 0L
        }

        // 2. Compute Raw Delta
        val rawDeltaMs = secureNow - lastSavedTimeMs

        // 3. Monotonic ElapsedRealtime Cross-Verification (if device wasn't rebooted)
        val currentElapsed = SystemClock.elapsedRealtime()
        if (lastSavedElapsedRealtime > 0L && currentElapsed > lastSavedElapsedRealtime) {
            val monotonicDelta = currentElapsed - lastSavedElapsedRealtime
            // If device was not restarted, monotonic hardware delta is strictly authoritative
            if (rawDeltaMs > monotonicDelta + TAMPER_TOLERANCE_MS) {
                Log.w(TAG, "Device clock forward tamper detected! RawDelta=${rawDeltaMs}ms vs MonotonicHardwareDelta=${monotonicDelta}ms. Clamping to hardware delta.")
                isClockTamperingDetected = true
                return monotonicDelta.coerceIn(0L, MAX_OFFLINE_DURATION_MS)
            }
        }

        // 4. Safe Clamping (Cap at 24 hours max)
        val clampedDelta = min(rawDeltaMs, MAX_OFFLINE_DURATION_MS)
        if (clampedDelta >= MIN_OFFLINE_DURATION_MS) {
            Log.d(TAG, "Offline progress validated: ${clampedDelta / 1000}s (${clampedDelta / 60000} mins)")
        }

        return clampedDelta
    }

    /**
     * Validates if a daily reward can be safely claimed (must be >= 20 hours after last claim).
     */
    fun canClaimDailyReward(lastDailyRewardMs: Long): Boolean {
        if (lastDailyRewardMs <= 0L) return true
        val secureNow = getSecureCurrentTimeMs()
        val diff = secureNow - lastDailyRewardMs
        // If device clock was moved backwards into the past before last claim, reject claim
        if (diff < 0L) {
            Log.w(TAG, "Daily reward check failed: secureNow ($secureNow) < lastDailyRewardMs ($lastDailyRewardMs)")
            return false
        }
        return diff >= (20 * 60 * 60 * 1000L) // 20 hours cooldown
    }
}
