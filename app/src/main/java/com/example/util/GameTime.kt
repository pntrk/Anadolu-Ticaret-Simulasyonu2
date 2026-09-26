package com.example.util

import java.util.concurrent.atomic.AtomicLong

/**
 * Centralized game time clock engine.
 * Decouples time calculations (consortiums, crisis discounts, remainingSeconds)
 * from device system clock manipulators by synchronizing with server time offsets (NTP / Supabase timestamp).
 */
object GameTime {

    private val serverOffsetMs = AtomicLong(0L)

    /**
     * Returns current server-compensated timestamp in milliseconds.
     */
    fun now(): Long = System.currentTimeMillis() + serverOffsetMs.get()

    /**
     * Calibrates local clock offset using trusted server epoch timestamp.
     */
    fun syncServerTime(serverEpochMs: Long) {
        val localTime = System.currentTimeMillis()
        serverOffsetMs.set(serverEpochMs - localTime)
    }

    /**
     * Computes remaining seconds until [expiryTimeMs] guaranteed to be >= 0L.
     */
    fun remainingSeconds(expiryTimeMs: Long): Long {
        return ((expiryTimeMs - now()) / 1000L).coerceAtLeast(0L)
    }

    /**
     * Computes remaining milliseconds until [expiryTimeMs] guaranteed to be >= 0L.
     */
    fun remainingMs(expiryTimeMs: Long): Long {
        return (expiryTimeMs - now()).coerceAtLeast(0L)
    }

    /**
     * Checks if a target timestamp has expired relative to calibrated server time.
     */
    fun isExpired(expiryTimeMs: Long): Boolean {
        return now() >= expiryTimeMs
    }
}
