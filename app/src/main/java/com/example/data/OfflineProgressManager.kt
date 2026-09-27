package com.example.data

import android.content.Context
import android.util.Log
import kotlin.math.min

object OfflineProgressManager {
    private const val TAG = "OfflineProgressManager"
    private const val PREFS_NAME = "anadolu_offline_prefs"
    private const val KEY_LAST_LOGOUT = "ats_last_logout_timestamp"

    const val MAX_OFFLINE_DURATION_MS: Long = 8 * 3600 * 1000L // 8 Saatlik Idle Cap
    const val MIN_QUALIFYING_DURATION_MS: Long = 2 * 60 * 1000L // Minimum 2 dakika

    /**
     * Oyuncu oyundan çıkarken veya arka plana geçtiğinde çağrılır.
     */
    fun recordLogoutTimestamp(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_LOGOUT, System.currentTimeMillis()).apply()
            Log.d(TAG, "Recorded logout timestamp: ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w(TAG, "Error recording logout timestamp", e)
        }
    }

    /**
     * Oyuncu oyuna girdiğinde offline simülasyonu çalıştırır.
     * @return Pair of (OfflineReport?, Map of inventory updates)
     */
    fun processOfflineProgress(
        context: Context,
        facilities: List<BusinessEntity>,
        inventory: List<InventoryEntity>,
        playerLevel: Int = 1,
        marketPrices: List<MarketPriceEntity> = emptyList()
    ): Pair<OfflineReport?, Map<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastLogoutMs = prefs.getLong(KEY_LAST_LOGOUT, 0L)
        val now = System.currentTimeMillis()

        if (lastLogoutMs <= 0L) {
            recordLogoutTimestamp(context)
            return Pair(null, emptyMap())
        }

        val elapsedMs = now - lastLogoutMs
        if (elapsedMs < MIN_QUALIFYING_DURATION_MS) {
            recordLogoutTimestamp(context)
            return Pair(null, emptyMap())
        }

        val isCapReached = elapsedMs > MAX_OFFLINE_DURATION_MS
        val simulatedMs = if (isCapReached) MAX_OFFLINE_DURATION_MS else elapsedMs
        val simulatedHours = simulatedMs.toDouble() / (1000.0 * 3600.0)

        val priceMap = marketPrices.associateBy({ it.itemId }, { it.price })
        val virtualInventory = inventory.associate { it.itemId to it.quantity }.toMutableMap()

        val producedItems = mutableListOf<OfflineProducedItem>()
        val consumedResources = mutableMapOf<String, Int>()
        val inventoryAdditions = mutableMapOf<String, Int>()
        var totalGrossRevenue = 0L
        var totalExp = 0

        for (facility in facilities) {
            if (facility.isConstructing || facility.isUpgrading) continue

            val product = Product.values().find { it.facilityId == facility.type || it.id == facility.type } ?: continue
            
            // Saatlik üretim kapasitesi (Tesis seviyesi ve ürün kademesine göre)
            val baseHourlyRate = when (product.tier) {
                ProductTier.TIER_1 -> 20 + (facility.level * 10) // Ham Madde: 30 - 120 adet/saat
                ProductTier.TIER_2 -> 10 + (facility.level * 5)  // Ara Mamul: 15 - 60 adet/saat
                ProductTier.TIER_3 -> 2 + facility.level         // Nihai Ürün: 3 - 12 adet/saat
                ProductTier.TIER_4 -> 1 + (facility.level / 2)   // Yüksek Teknoloji: 1 - 6 adet/saat
            }

            val potentialUnits = (baseHourlyRate * simulatedHours).toInt()
            if (potentialUnits <= 0) continue

            // Hammadde darboğazı kontrolü
            var actualProduceUnits = potentialUnits
            if (product.recipe.isNotEmpty()) {
                for (req in product.recipe) {
                    val available = virtualInventory[req.productId] ?: 0
                    if (req.amountPerUnit > 0) {
                        val maxPossible = available / req.amountPerUnit
                        actualProduceUnits = min(actualProduceUnits, maxPossible)
                    }
                }
            }

            if (actualProduceUnits <= 0) continue

            // Hammaddeleri envanterden düş
            for (req in product.recipe) {
                val totalCost = req.amountPerUnit * actualProduceUnits
                val currentStock = virtualInventory[req.productId] ?: 0
                virtualInventory[req.productId] = (currentStock - totalCost).coerceAtLeast(0)
                consumedResources[req.productId] = (consumedResources[req.productId] ?: 0) + totalCost
            }

            // 1 - 5 Yıldız Kalite Dağılımı Hesaplaması
            val qualityDistribution = QualityCraftingService.simulateBatchCrafting(
                totalUnits = actualProduceUnits,
                facilityLevel = facility.level,
                playerLevel = playerLevel
            )

            val unitPrice = priceMap[product.id] ?: product.basePrice

            for ((quality, count) in qualityDistribution) {
                if (count <= 0) continue

                val item = OfflineProducedItem(
                    productId = product.id,
                    productName = product.getDisplayName(),
                    quality = quality,
                    quantity = count,
                    unitPrice = unitPrice,
                    icon = product.icon,
                    colorTint = product.colorTint
                )

                producedItems.add(item)
                totalGrossRevenue += item.totalPrice
                inventoryAdditions[product.id] = (inventoryAdditions[product.id] ?: 0) + count
            }

            totalExp += (actualProduceUnits * 15 * (1 + (facility.level * 0.1))).toInt()
        }

        // Tesis Gece Bakım/Yıpranma Masrafı (Brüt gelirin %4'ü)
        val maintenanceCost = (totalGrossRevenue * 0.04).toLong().coerceIn(0L, 100_000L)

        val consumedList = consumedResources.map { (id, amount) ->
            val pName = Product.values().find { it.id == id }?.getDisplayName() ?: id.replace("_", " ").uppercase()
            OfflineConsumedResource(
                resourceId = id,
                resourceName = pName,
                amount = amount
            )
        }

        // Logout timestamp'i güncelle
        recordLogoutTimestamp(context)

        if (producedItems.isEmpty() && totalGrossRevenue <= 0L) {
            return Pair(null, emptyMap())
        }

        val report = OfflineReport(
            logoutTime = lastLogoutMs,
            loginTime = now,
            elapsedDurationMs = elapsedMs,
            simulatedDurationMs = simulatedMs,
            isCapReached = isCapReached,
            producedItems = producedItems,
            consumedResources = consumedList,
            grossRevenue = totalGrossRevenue,
            maintenanceCosts = maintenanceCost,
            totalExpGained = totalExp
        )

        return Pair(report, inventoryAdditions)
    }
}
