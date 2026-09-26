package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test suite for MacroEconomyEngine.
 * Covers:
 * - Negative balance and heavy leverage/debt scenarios
 * - Multi-billion/trillion currency calculations with sublinear valuation curves
 * - Extreme market shocks with tanh damping
 * - Zero divide & empty state protections
 */
class MacroEconomyEngineTest {

    @Test
    fun `calculateCompanySharePrice handles negative or zero valuation safely without NaN or infinite`() {
        val macroState = MacroEconomyState(
            cycle = EconomicCycle.RECOVERY,
            globalInflationRate = 0.05f,
            centralBankInterestRate = 0.10f,
            marketLiquidityMultiplier = 1.0f,
            peRatioBase = 10.0f
        )

        val priceForZero = MacroEconomyEngine.calculateCompanySharePrice(
            totalValuation = 0L,
            xpProgress = 0,
            macroState = macroState
        )

        val priceForNegative = MacroEconomyEngine.calculateCompanySharePrice(
            totalValuation = -50_000_000L,
            xpProgress = 0,
            macroState = macroState
        )

        assertFalse("Price must not be NaN", priceForZero.isNaN())
        assertFalse("Price must not be Infinite", priceForZero.isInfinite())
        assertTrue("Price should respect minimum floor", priceForZero >= 10.0)
        assertTrue("Price for negative valuation should be clamped to floor", priceForNegative >= 10.0)
    }

    @Test
    fun `calculateCompanySharePrice with multi-trillion empire scales with sublinear diminishing returns`() {
        val macroState = MacroEconomyState(
            cycle = EconomicCycle.BOOM,
            globalInflationRate = 0.08f,
            centralBankInterestRate = 0.08f,
            marketLiquidityMultiplier = 1.5f,
            peRatioBase = 20.0f
        )

        val smallEmpirePrice = MacroEconomyEngine.calculateCompanySharePrice(
            totalValuation = 100_000_000L, // 100M
            xpProgress = 50_000,
            macroState = macroState
        )

        val massiveEmpirePrice = MacroEconomyEngine.calculateCompanySharePrice(
            totalValuation = 1_000_000_000_000L, // 1 Trillion
            xpProgress = 5_000_000,
            macroState = macroState
        )

        assertTrue("Massive empire price must be higher", massiveEmpirePrice > smallEmpirePrice)
        assertFalse("Price must not overflow", massiveEmpirePrice.isInfinite())
        assertFalse("Price must not be NaN", massiveEmpirePrice.isNaN())
    }

    @Test
    fun `calculateCompanySharePrice handles crisis and city shocks with bounded dampening`() {
        val macroState = MacroEconomyState(
            cycle = EconomicCycle.RECESSION,
            globalInflationRate = 0.15f,
            centralBankInterestRate = 0.20f,
            marketLiquidityMultiplier = 0.8f,
            peRatioBase = 8.0f
        )

        val severeCrisisState = ConsortiumCrisisState(
            isCrisisActive = true,
            consecutiveCrisisTicks = 15,
            stockMarketDropMultiplier = 0.65f, // -35% drop
            systemStrainIndex = 0.95f
        )

        val cityShock = CityMarketEvent(
            id = "event_kocaeli_strike",
            cityId = "kocaeli",
            cityName = "Kocaeli",
            affectedProductIds = listOf("steel", "iron_ore"),
            affectedProductNames = listOf("Çelik", "Demir"),
            initialPriceMultiplier = 1.40f,
            headline = "Kocaeli Ağır Sanayi Grevi",
            description = "Tüm sanayi üretimi durma noktasında.",
            strategyTip = "Sanayi hammaddesi stoklayın.",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "🏭"
        )

        val sharePriceNormal = MacroEconomyEngine.calculateCompanySharePrice(
            totalValuation = 500_000_000L,
            xpProgress = 100_000,
            macroState = macroState,
            crisisState = ConsortiumCrisisState(isCrisisActive = false),
            activeCityEvents = emptyList()
        )

        val sharePriceUnderCrisis = MacroEconomyEngine.calculateCompanySharePrice(
            totalValuation = 500_000_000L,
            xpProgress = 100_000,
            macroState = macroState,
            crisisState = severeCrisisState,
            activeCityEvents = listOf(cityShock)
        )

        assertTrue("Share price should be positive", sharePriceNormal > 0.0)
        assertTrue("Share price under crisis should be discounted", sharePriceUnderCrisis < sharePriceNormal)
        assertTrue("Share price drop should be damped and bounded, not zero or negative", sharePriceUnderCrisis >= sharePriceNormal * 0.4)
    }
}
