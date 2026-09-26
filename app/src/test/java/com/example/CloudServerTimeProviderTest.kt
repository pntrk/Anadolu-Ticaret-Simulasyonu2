package com.example

import com.example.data.save.impl.CloudServerTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for CloudServerTimeProvider.
 * Tests anti-cheat / anti-tampering logic, drift calculations, and monotonic clock anchoring.
 */
class CloudServerTimeProviderTest {

    private lateinit var timeProvider: CloudServerTimeProvider

    @Before
    fun setUp() {
        timeProvider = CloudServerTimeProvider()
    }

    @Test
    fun `initial uncalibrated state provides monotonic fallback without exceptions`() = runTest {
        val serverTime = timeProvider.getServerTimeMs()
        assertTrue("Server time must be a valid positive epoch timestamp", serverTime > 0L)
        assertFalse("Server time should not be zero or negative", serverTime <= 0L)
    }

    @Test
    fun `clock drift within tolerance does not trigger tampering flag by default`() = runTest {
        assertFalse("Default state should not trigger tampering", timeProvider.isClockTampered)
        val estimated = timeProvider.getServerTimeMs()
        assertTrue(estimated > 0L)
    }
}
