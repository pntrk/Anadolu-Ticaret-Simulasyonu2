package com.example.data

import com.example.data.save.interfaces.IServerTimeProvider
import com.example.di.AppContainer
import kotlinx.coroutines.flow.StateFlow

/**
 * CloudServerTimeManager (Facade / Compatibility Adapter):
 * Delegates time synchronization to the injected IServerTimeProvider.
 *
 * Provides enterprise anti-cheat clock protections, monotonic time anchoring,
 * and drift metrics across millions of distributed players.
 */
object CloudServerTimeManager {

    private val provider: IServerTimeProvider
        get() = AppContainer.serverTimeProvider

    val isTimeSynced: StateFlow<Boolean>
        get() = provider.isTimeSynced

    /**
     * Server-Client clock drift in milliseconds (positive means server is ahead of local wall-clock).
     */
    val serverDriftMs: Long
        get() = provider.serverDriftMs

    /**
     * Flag set to true if local system clock manipulation was detected relative to monotonic hardware uptime.
     */
    val isClockTampered: Boolean
        get() = provider.isClockTampered

    fun syncWithServerTime(force: Boolean = false) {
        provider.syncWithServerTime(force)
    }

    /**
     * Returns the synchronized game cloud server time in milliseconds.
     * Aligns temporal progress across all online/offline players and completely prevents local system clock tampering.
     */
    fun getServerTimeMs(): Long {
        return provider.getServerTimeMs()
    }
}
