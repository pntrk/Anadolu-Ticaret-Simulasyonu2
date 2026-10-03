package com.example.data

import com.example.viewmodel.*

import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationType
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BorsaEngine(
    private val repository: GameRepository,
    private val scope: CoroutineScope,
    private val viewModel: GameViewModel
) {
    fun buyFromBorsa(itemId: String, quantity: Int, originCountryParam: String = "Global"): Boolean {
        if (quantity <= 0) return false
        val p = viewModel.player.value ?: return false
        val product = Product.values().find { it.id == itemId } ?: return false
        
        val finalPriceEntity = viewModel.marketPrices.value.find { it.itemId == itemId }
        val destCityId = viewModel.businesses.value.find { biz ->
            val bizProduct = Product.values().find { it.facilityId == biz.type || it.id == biz.type }
            bizProduct?.recipe?.any { it.productId == itemId } == true
        }?.cityId ?: viewModel.businesses.value.firstOrNull()?.cityId ?: p.currentCity
        
        val currentPrice = finalPriceEntity?.price ?: product.basePrice.coerceAtLeast(10L)
        val originCityId = "new_york"

        val baseCost = currentPrice * quantity
        val vipDiscount = if (p.isVip) (baseCost * 0.05).toLong() else 0L
        val activeBuffs = viewModel.activeArtifactBuffs.value
        val borsaBuyDiscount = activeBuffs[ArtifactBuffType.BORSA_BUY_DISCOUNT] ?: 0f
        val artifactDiscount = (baseCost * borsaBuyDiscount).toLong()
        val totalCostProduct = (baseCost - vipDiscount - artifactDiscount).coerceAtLeast(0L)
        val logisticsCost = viewModel.calculateLogisticsCost(originCityId, destCityId, quantity)
        val finalRequired = totalCostProduct + logisticsCost

        val currentInvTotal = viewModel.inventory.value.sumOf { it.quantity }
        if (currentInvTotal + quantity > p.inventoryCapacity) {
            com.example.data.telemetry.OnboardingFrictionTracker.trackWarehouseOverflow(
                playerLevel = p.level,
                currentUsage = currentInvTotal,
                maxCapacity = p.inventoryCapacity,
                blockedProductId = itemId
            )
            SmartNotificationManager.show("Hata: Depo Kapasitesi Dolu!", "Error: Warehouse Capacity Full!", NotificationType.ALERT)
            return false
        }

        if (p.money < finalRequired) {
            com.example.data.telemetry.OnboardingFrictionTracker.trackInsufficientBalance(
                playerLevel = p.level,
                requiredAmount = finalRequired,
                availableAmount = p.money,
                purchaseType = "Borsa_Alim",
                itemId = itemId
            )
            val costText = "${com.example.ui.components.formatCredit(totalCostProduct)} + ${com.example.ui.components.formatCredit(logisticsCost)} Lojistik (Toplam: ${com.example.ui.components.formatCredit(finalRequired)})"
            SmartNotificationManager.show("Yetersiz Bakiye! Gerekli: $costText", "Insufficient Balance! Required: $costText", NotificationType.ALERT)
            return false
        }

        scope.launch {
            val currentP = viewModel.player.value ?: p
            val newMoney = (currentP.money - finalRequired).coerceAtLeast(0L)

            repository.updatePlayer(currentP.copy(
                money = newMoney,
                dollarBalance = newMoney,
                dailyExpense = currentP.dailyExpense + finalRequired
            ))

            // Borsa alımı yerel simülasyon ve Room DB üzerinde anında gerçekleştirilir (Sıfır ağ gecikmesi, sıfır sunucu kotası)
            viewModel.onBorsaItemBought(itemId, quantity, "Global")

            val prodName = product.getDisplayName()
            com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
            val boughtText = com.example.ui.components.formatCredit(finalRequired)
            val borsaWarehouseName = "New York Emtia Borsası Deposu"
            val destCityName = cities.find { it.id == destCityId }?.name ?: destCityId

            // Ticaret Rotası Sevkiyatı: Borsa deposundan oyuncu deposuna lojistik transfer
            if (originCityId != destCityId) {
                val durationMs = viewModel.calculateLogisticsDuration(originCityId, destCityId)
                viewModel.addActiveDelivery(
                    DeliveryItem(
                        itemId = itemId,
                        quantity = quantity,
                        originCityId = originCityId,
                        destinationCityId = destCityId,
                        pricePerUnit = currentPrice,
                        totalCost = finalRequired,
                        startTimeMs = System.currentTimeMillis(),
                        totalDurationMs = durationMs,
                        isOutboundSale = false
                    )
                )
                SmartNotificationManager.show(
                    "🚚 Borsa Sevkiyatı Yola Çıktı! $borsaWarehouseName ➔ $destCityName: $quantity Ton $prodName sevk ediliyor. (-$boughtText)",
                    "Exchange Delivery Dispatched! $borsaWarehouseName ➔ $destCityName: $quantity Tons of $prodName in transit. (-$boughtText)",
                    NotificationType.SUCCESS
                )
            } else {
                repository.produceItem(itemId, quantity)
                SmartNotificationManager.show(
                    "Borsadan $quantity Ton $prodName ($borsaWarehouseName) anında teslim alındı! (-$boughtText)",
                    "Bought $quantity Tons of $prodName directly from $borsaWarehouseName! (-$boughtText)",
                    NotificationType.SUCCESS
                )
            }

            viewModel.updateDailyQuestProgress(com.example.data.quest.QuestType.BORSA_TRADE, 1L)
            viewModel.markFirstTradeCompleted()
            SaveSyncCoordinator.markDirty()
        }
        return true
    }

    fun sell(itemId: String, quantity: Int) {
        if (quantity <= 0) return
        val inv = viewModel.inventory.value.find { it.itemId == itemId }
        val ownedQuantity = inv?.quantity ?: 0
        if (ownedQuantity < quantity) {
            SmartNotificationManager.show("Hata: Yetersiz stok!", "Error: Insufficient stock!", NotificationType.ALERT)
            return
        }

        viewModel.sell(itemId, quantity)
        viewModel.updateDailyQuestProgress(com.example.data.quest.QuestType.BORSA_TRADE, 1L)
        viewModel.updateDailyQuestProgress(com.example.data.quest.QuestType.SELL_COMMODITY, quantity.toLong())
    }
}
