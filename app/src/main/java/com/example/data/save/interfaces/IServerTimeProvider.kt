package com.example.data.save.interfaces

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract for synchronized server time provider to prevent clock tampering.
 * Implements monotonic clock anchoring, drift tracking, and anti-cheat protections.
 */
interface IServerTimeProvider {
    fun syncWithServerTime(force: Boolean = false)
    fun getServerTimeMs(): Long
    val isTimeSynced: StateFlow<Boolean>
    val serverDriftMs: Long get() = 0L
    val isClockTampered: Boolean get() = false
}

