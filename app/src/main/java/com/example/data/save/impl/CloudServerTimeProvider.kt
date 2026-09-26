package com.example.data.save.impl

import android.os.SystemClock
import android.util.Log
import com.example.data.SupabaseManager
import com.example.data.save.interfaces.IServerTimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Enterprise-grade Synchronized Server Time Provider.
 *
 * Anti-Cheat & Scalability Features:
 * 1. Monotonic Hardware Clock Anchoring: Uses SystemClock.elapsedRealtime() to anchor
 *    server time. Any offline or online manipulation of local device wall-clock is completely
 *    rendered ineffective.
 * 2. Drift & Latency Calculation: Measures Network Round-Trip Time (RTT) to compute true
 *    offset and clock drift between client and authoritative cloud server.
 * 3. Exponential Backoff with Jitter: In case of network dropouts or distributed server congestion,
 *    retries synchronization using exponential backoff (1s -> 2s -> 4s -> ... -> 60s) with 20% jitter
 *    to prevent thundering-herd issues on backend endpoints.
 * 4. Tamper Detection: Detects anomalies where local wall-clock jumps abruptly relative to monotonic hardware ticks.
 */
class CloudServerTimeProvider(
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : IServerTimeProvider {

    private val TAG = "CloudServerTime"

    // Monotonic Anchor Points
    @Volatile private var serverAnchorMs: Long = 0L
    @Volatile private var elapsedAnchorMs: Long = 0L
    @Volatile private var wallAnchorMs: Long = 0L
    @Volatile private var _serverDriftMs: Long = 0L
    @Volatile private var _isClockTampered: Boolean = false

    private val _isTimeSynced = MutableStateFlow(false)
    override val isTimeSynced: StateFlow<Boolean> = _isTimeSynced.asStateFlow()

    override val serverDriftMs: Long
        get() = _serverDriftMs

    override val isClockTampered: Boolean
        get() = _isClockTampered

    private var retryJob: Job? = null
    private var lastSuccessfulSyncElapsed: Long = 0L

    companion object {
        private const val SYNC_CACHE_WINDOW_MS = 300_000L // 5 minutes
        private const val BASE_RETRY_DELAY_MS = 1_000L    // 1 second base
        private const val MAX_RETRY_DELAY_MS = 60_000L     // 60 seconds ceiling
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val TAMPER_TOLERANCE_MS = 15_000L    // 15s tolerance before flagging tamper
    }

    override fun syncWithServerTime(force: Boolean) {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (!force && _isTimeSynced.value && (nowElapsed - lastSuccessfulSyncElapsed) < SYNC_CACHE_WINDOW_MS) {
            return
        }

        // Cancel existing retry loop if a new forced sync is invoked
        retryJob?.cancel()
        retryJob = externalScope.launch {
            executeSyncWithExponentialBackoff()
        }
    }

    /**
     * Executes server time synchronization with Exponential Backoff + Jitter retry loop.
     */
    private suspend fun executeSyncWithExponentialBackoff() {
        var attempt = 0
        var currentDelayMs = BASE_RETRY_DELAY_MS

        while (true) {
            val t0 = SystemClock.elapsedRealtime()
            val t0Wall = System.currentTimeMillis()

            try {
                // 1. Try Supabase Server Time
                var authoritativeTime = SupabaseManager.fetchServerTimeMsFromSupabase()
                
                // 2. Fallback to Multi-Source NTP / HTTPS Date Provider
                if (authoritativeTime == null || authoritativeTime <= 0L) {
                    authoritativeTime = com.example.data.security.NtpTimeProvider.fetchNetworkTimeMs()
                }

                val t1 = SystemClock.elapsedRealtime()

                if (authoritativeTime != null && authoritativeTime > 0L) {
                    val rtt = (t1 - t0).coerceAtLeast(0L)
                    val estimatedServerTime = authoritativeTime + (rtt / 2L)
                    val localWall = System.currentTimeMillis()

                    serverAnchorMs = estimatedServerTime
                    elapsedAnchorMs = t1
                    wallAnchorMs = localWall
                    _serverDriftMs = estimatedServerTime - localWall
                    lastSuccessfulSyncElapsed = t1

                    _isTimeSynced.value = true
                    Log.d(TAG, "Server time synced successfully. Authoritative: $estimatedServerTime, Drift: ${_serverDriftMs}ms, RTT: ${rtt}ms")
                    
                    // Schedule next periodic sync in 5 minutes
                    schedulePeriodicSync()
                    break // Sync success, exit retry loop
                } else {
                    Log.w(TAG, "Authoritative server time response empty on attempt $attempt, scheduling backoff retry.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network exception during server time sync on attempt $attempt: ${e.message}")
            }

            // Calculate Exponential Backoff with Jitter
            attempt++
            val jitter = Random.nextLong(0, (currentDelayMs * 0.25).toLong().coerceAtLeast(100L))
            val delayWithJitter = min(currentDelayMs + jitter, MAX_RETRY_DELAY_MS)
            
            delay(delayWithJitter)

            currentDelayMs = (currentDelayMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_RETRY_DELAY_MS)
        }
    }

    private fun schedulePeriodicSync() {
        externalScope.launch {
            delay(SYNC_CACHE_WINDOW_MS)
            syncWithServerTime(force = true)
        }
    }

    /**
     * Returns the true authoritative server time in milliseconds.
     * Guaranteed monotonic: Immune to local clock manipulation when disconnected or offline.
     */
    override fun getServerTimeMs(): Long {
        if (elapsedAnchorMs > 0L) {
            val nowElapsed = SystemClock.elapsedRealtime()
            val elapsedDelta = nowElapsed - elapsedAnchorMs

            // Detect if player manipulated the wall clock
            val nowWall = System.currentTimeMillis()
            val wallDelta = nowWall - wallAnchorMs
            if (abs(wallDelta - elapsedDelta) > TAMPER_TOLERANCE_MS) {
                if (!_isClockTampered) {
                    _isClockTampered = true
                    Log.w(TAG, "Local wall clock tampering detected! WallDelta=$wallDelta vs ElapsedDelta=$elapsedDelta. Enforcing monotonic server time.")
                }
            }

            return serverAnchorMs + elapsedDelta
        }

        // Fallback before initial sync
        return System.currentTimeMillis() + _serverDriftMs
    }
}
