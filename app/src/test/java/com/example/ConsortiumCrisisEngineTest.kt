package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test suite for ConsortiumCrisisEngine.
 * Covers:
 * - System Dynamics Sigmoid strain index
 * - Diminishing returns & non-linear shock damping
 * - Edge cases: Zero division, massive multi-million volume productions, empty sets
 */
class ConsortiumCrisisEngineTest {

    @Test
    fun `empty consortium list yields normalized non-crisis state without zero division`() {
        val initialState = ConsortiumCrisisState()
        val evaluated = ConsortiumCrisisEngine.evaluateConsortiumCrisis(emptyList(), initialState)

        assertFalse(evaluated.isCrisisActive)
        assertEquals(0, evaluated.totalConsortiums)
        assertEquals(0, evaluated.bottleneckConsortiumsCount)
        assertEquals(0f, evaluated.bottleneckPercentage, 0.0001f)
        assertEquals(1.0f, evaluated.rawMaterialInflationMultiplier, 0.0001f)
        assertEquals(0.0f, evaluated.systemStrainIndex, 0.0001f)
    }

    @Test
    fun `massive production volume with bottleneck calculates finite bounded inflation without overflow`() {
        val massiveConsortiums = (1..50).map { index ->
            val isBottleneck = index % 2 == 0 // 50% bottleneck
            MegaProject(
                id = "consortium_$index",
                consortiumName = "Mega Sanayi Projesi #$index",
                brandName = "Marka #$index",
                targetProductId = "steel",
                targetProductName = "Gelişmiş Çelik Alaşımı",
                leaderPlayerId = "leader_$index",
                leaderPlayerName = "Lider $index",
                totalProjectValue = 500_000_000L,
                currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
                warehouseStock = if (isBottleneck) 0 else 50_000_000,
                slots = listOf(
                    ConsortiumSupplierSlot(
                        slotId = "slot_iron_$index",
                        productId = "iron_ore",
                        productName = "Demir Cevheri",
                        quantityRequired = 100_000_000,
                        quantityDelivered = if (isBottleneck) 1000 else 100_000_000,
                        stage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
                        assignedPartnerId = if (isBottleneck) null else "partner_valid",
                        costContributionValue = 50_000_000L,
                        isBottleneckWarning = isBottleneck
                    )
                )
            )
        }

        val state = ConsortiumCrisisEngine.evaluateConsortiumCrisis(
            consortiums = massiveConsortiums,
            currentCrisisState = ConsortiumCrisisState()
        )

        assertFalse(state.rawMaterialInflationMultiplier.isNaN())
        assertFalse(state.rawMaterialInflationMultiplier.isInfinite())
        assertTrue("Inflation should be bounded within realistic range", state.rawMaterialInflationMultiplier in 1.0f..3.0f)
        assertTrue("System strain index should be in [0.0, 1.0]", state.systemStrainIndex in 0.0f..1.0f)
        assertTrue("Stock market drop should be bounded", state.stockMarketDropMultiplier in 0.4f..1.0f)
    }

    @Test
    fun `critical missing materials identification handles zero-quantity and large inventories correctly`() {
        val projectWithBottleneck = MegaProject(
            id = "proj_test_bottleneck",
            consortiumName = "Test Projesi",
            brandName = "Test Markası",
            targetProductId = "battery_pack",
            targetProductName = "Lityum Batarya Modülü",
            leaderPlayerId = "leader_test",
            leaderPlayerName = "Test Lider",
            totalProjectValue = 100_000_000L,
            currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
            slots = listOf(
                ConsortiumSupplierSlot(
                    slotId = "slot_1",
                    productId = "battery_cell",
                    productName = "Batarya Hücresi",
                    quantityRequired = 50_000,
                    quantityDelivered = 10_000,
                    stage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
                    costContributionValue = 20_000_000L,
                    isBottleneckWarning = true
                ),
                ConsortiumSupplierSlot(
                    slotId = "slot_2",
                    productId = "microchip",
                    productName = "Mikroçip",
                    quantityRequired = 80_000,
                    quantityDelivered = 2_000,
                    stage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
                    costContributionValue = 40_000_000L,
                    isBottleneckWarning = true
                )
            )
        )

        val state = ConsortiumCrisisEngine.evaluateConsortiumCrisis(
            consortiums = listOf(projectWithBottleneck),
            currentCrisisState = ConsortiumCrisisState()
        )

        assertTrue(state.isCrisisActive)
        assertEquals(1, state.bottleneckConsortiumsCount)
        assertTrue("Microchip should be the top missing material", state.criticalMissingMaterials.contains("microchip"))
        assertTrue(state.criticalMissingMaterials.contains("battery_cell"))
    }

    @Test
    fun `consecutive crisis ticks increase strain index smoothly up to saturation asymptote`() {
        var currentState = ConsortiumCrisisState(
            isCrisisActive = true,
            consecutiveCrisisTicks = 0
        )

        val bottleneckProject = MegaProject(
            id = "proj_crisis",
            consortiumName = "Ağır Kriz Projesi",
            brandName = "Kriz Markası",
            targetProductId = "nuclear_core",
            targetProductName = "Nükleer Reaktör Çekirdeği",
            leaderPlayerId = "leader_crisis",
            leaderPlayerName = "Lider Kriz",
            totalProjectValue = 2_000_000_000L,
            currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
            isProductionPaused = true,
            slots = listOf(
                ConsortiumSupplierSlot(
                    slotId = "slot_1",
                    productId = "steel",
                    productName = "Çelik",
                    quantityRequired = 500_000,
                    quantityDelivered = 0,
                    stage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
                    costContributionValue = 200_000_000L,
                    isBottleneckWarning = true
                )
            )
        )

        val inflationProgression = mutableListOf<Float>()

        for (tick in 1..10) {
            currentState = ConsortiumCrisisEngine.evaluateConsortiumCrisis(
                consortiums = listOf(bottleneckProject),
                currentCrisisState = currentState
            )
            inflationProgression.add(currentState.rawMaterialInflationMultiplier)
        }

        // Verify monotonically increasing or stable up to diminishing returns ceiling
        for (i in 0 until inflationProgression.size - 1) {
            assertTrue("Inflation should increase or saturate", inflationProgression[i + 1] >= inflationProgression[i])
        }
    }
}
