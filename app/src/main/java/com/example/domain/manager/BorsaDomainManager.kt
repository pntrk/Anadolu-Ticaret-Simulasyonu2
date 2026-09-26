package com.example.domain.manager

import com.example.data.MarketPriceEntity
import com.example.data.Product
import com.example.data.ProductTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Borsa & Financial Market Domain Manager.
 * Handles algorithmic stock, commodity & currency market price oscillations.
 */
class BorsaDomainManager {

    private val seasons = listOf("İlkbahar", "Yaz", "Sonbahar", "Kış")
    private val events = listOf(
        "Normal",
        "Don (Tarım Düşüşü)",
        "Kuraklık (Tahıl Azalır)",
        "Ticaret Anlaşması (Fiyat Artışı)",
        "Turizm Patlaması",
        "Merkez Bankası Müdahalesi",
        "Ekonomik Büyüme",
        "Süveyş Navlun Artışı",
        "Yeşil Enerji Teşvik Paketi",
        "Yapay Zeka Yatırım Dalgası",
        "Jeopolitik Risk Primi",
        "Global Emtia Rallisi"
    )

    data class MarketTickResult(
        val updatedPrices: List<MarketPriceEntity>,
        val currentSeason: String,
        val activeEvent: String
    )

    suspend fun computeNextMarketTick(
        currentPrices: List<MarketPriceEntity>,
        currentSeasonIndex: Int,
        activeEvent: String,
        globalInflationRate: Float
    ): MarketTickResult = withContext(Dispatchers.Default) {
        val selectedSeason = seasons[currentSeasonIndex % seasons.size]
        val updatedList = currentPrices.map { item ->
            val productDef = Product.values().find { it.id == item.itemId }
            val basePrice = productDef?.basePrice ?: item.price

            var deltaPercent = (Random.nextFloat() * 0.12f) - 0.058f // -5.8% to +6.2%

            if (productDef != null) {
                when (productDef.tier) {
                    ProductTier.TIER_1 -> {
                        if (activeEvent.contains("Kuraklık") || activeEvent.contains("Don")) deltaPercent += 0.05f
                    }
                    ProductTier.TIER_3, ProductTier.TIER_4 -> {
                        if (activeEvent.contains("Yapay Zeka") || activeEvent.contains("Ekonomik Büyüme")) deltaPercent += 0.07f
                    }
                    else -> {}
                }
            }

            // Inflation impact
            deltaPercent += (globalInflationRate * 0.02f)

            val minP = (basePrice * 0.25).toLong().coerceAtLeast(1L)
            val maxP = (basePrice * 3.5).toLong()
            val newPrice = ((item.price * (1.0f + deltaPercent)).toLong()).coerceIn(minP, maxP)

            item.copy(
                price = newPrice
            )
        }

        MarketTickResult(
            updatedPrices = updatedList,
            currentSeason = selectedSeason,
            activeEvent = activeEvent
        )
    }
}
