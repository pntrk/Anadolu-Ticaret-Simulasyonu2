package com.example

import com.example.data.telemetry.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit test suite for TelemetryService, OnboardingFrictionTracker, and ManagerAutomationGuard.
 */
@RunWith(RobolectricTestRunner::class)
class TelemetryAndAutomationGuardTest {

    @Test
    fun `manager automation guard intercepts unexpected exceptions without crashing loop`() = runTest {
        // Simulate Zeynep Demir (mgr_logistics) failing with NullPointerException
        val result = ManagerAutomationGuard.executeManagerSafely(
            managerId = "mgr_logistics",
            managerName = "Zeynep Demir",
            actionName = "100 Ton Kota Satışı",
            playerLevel = 2
        ) {
            throw NullPointerException("Simulated missing warehouse inventory item pointer")
        }

        // Must return false without propagating exception
        assertFalse("Guard must catch exception and return false", result)
    }

    @Test
    fun `manager automation guard succeeds cleanly on normal execution`() = runTest {
        var executed = false

        val result = ManagerAutomationGuard.executeManagerSafely(
            managerId = "mgr_logistics",
            managerName = "Zeynep Demir",
            actionName = "100 Ton Kota Satışı",
            playerLevel = 1
        ) {
            executed = true
        }

        assertTrue("Guard must return true on successful execution", result)
        assertTrue("Execution block must be invoked", executed)
    }

    @Test
    fun `onboarding friction tracker identifies level 1 and 2 as new players`() {
        assertTrue(OnboardingFrictionTracker.isNewPlayer(1))
        assertTrue(OnboardingFrictionTracker.isNewPlayer(2))
        assertFalse(OnboardingFrictionTracker.isNewPlayer(3))
        assertFalse(OnboardingFrictionTracker.isNewPlayer(10))
    }
}
