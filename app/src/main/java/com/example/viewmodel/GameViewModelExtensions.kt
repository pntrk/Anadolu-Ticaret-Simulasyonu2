package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.quest.*
import com.example.ui.components.NotificationType
import com.example.ui.components.SmartNotificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)

// --- BORSA EXTENSIONS ---

fun GameViewModel.buyFromBorsa(itemId: String, quantity: Int, originCountryParam: String? = "Global"): Boolean {
    val engine = com.example.data.BorsaEngine(repository, viewModelScope, this)
    return engine.buyFromBorsa(itemId, quantity, originCountryParam ?: "Global")
}

fun GameViewModel.sell(itemId: String, quantity: Int, originCountry: String? = "Global", isSilent: Boolean = false) {
    if (quantity <= 0) return
    val exactInv = inventory.value.find { it.itemId == itemId }
    val baseProductId = com.example.data.ItemQuality.extractBaseProductId(itemId)
    val matchingItems = if (exactInv != null) listOf(exactInv) else inventory.value.filter { it.baseProductId == baseProductId && it.quantity > 0 }
    val totalOwned = matchingItems.sumOf { it.quantity }

    if (totalOwned < quantity) {
        if (!isSilent) SmartNotificationManager.show("Yetersiz Merkez Depo Stoğu!", "Insufficient Central Depot Stock!", NotificationType.ALERT)
        return
    }
    viewModelScope.launch {
        val prod = Product.values().find { it.id == baseProductId }
        val priceEntity = marketPrices.value.find { it.itemId == baseProductId }
        val basePricePerUnit = priceEntity?.price ?: (prod?.basePrice ?: 100L)

        var remainingToSell = quantity
        var grossIncome = 0L
        val consumedItems = mutableListOf<Pair<String, Int>>()

        for (item in matchingItems) {
            if (remainingToSell <= 0) break
            val take = minOf(item.quantity, remainingToSell)
            val unitPrice = (basePricePerUnit * item.quality.priceMultiplier).toLong()
            grossIncome += unitPrice * take
            consumedItems.add(item.itemId to take)
            remainingToSell -= take
        }

        // 🏛️ Borsa Krizinde "Devlet / Hazine Teşvik Fonu" ve Üretici Sübvansiyonu (+%25 Nakit Prim)
        val isItemInCrisis = priceEntity?.let {
            it.isCrisis || it.borsaStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
        } ?: false
        val subsidyBonus = if (isItemInCrisis) (grossIncome * 0.25).toLong() else 0L
        val totalPrice = grossIncome + subsidyBonus

        val activeBuffs = activeArtifactBuffs.value
        val sellBonus = activeBuffs[ArtifactBuffType.BORSA_SELL_BONUS] ?: 0f
        val artifactBonusValue = (totalPrice * sellBonus).toLong()
        val finalRevenue = totalPrice + artifactBonusValue

        for ((cItemId, cQty) in consumedItems) {
            repository.consumeItem(cItemId, cQty)
        }

        addMoney(finalRevenue)

        val p = player.value
        if (p != null) {
            val originCityId = if (p.currentCity.isNotBlank()) p.currentCity else "istanbul"
            val destCityId = "new_york"
            val durationMs = calculateLogisticsDuration(originCityId, destCityId)
            addActiveDelivery(
                DeliveryItem(
                    itemId = itemId,
                    quantity = quantity,
                    originCityId = originCityId,
                    destinationCityId = destCityId,
                    pricePerUnit = if (quantity > 0) grossIncome / quantity else basePricePerUnit,
                    totalCost = finalRevenue,
                    startTimeMs = System.currentTimeMillis(),
                    totalDurationMs = durationMs,
                    isOutboundSale = true
                )
            )

            val prodName = prod?.getDisplayName() ?: baseProductId.uppercase()
            val originName = com.example.data.cities.find { it.id == originCityId }?.name ?: originCityId.uppercase()
            if (!isSilent) {
                if (subsidyBonus > 0L) {
                    SmartNotificationManager.show(
                        "🚚 Borsa Satış Sevkiyatı: $quantity Ton $prodName (+₳${com.example.ui.components.formatCredit(grossIncome)} + 🏛️ ₳${com.example.ui.components.formatCredit(subsidyBonus)} %25 Devlet Teşvik Primi)",
                        "🚚 Exchange Sale Dispatched: $quantity Tons of $prodName (+₳${com.example.ui.components.formatCredit(grossIncome)} + 🏛️ ₳${com.example.ui.components.formatCredit(subsidyBonus)} 25% State Subsidy Bonus)",
                        NotificationType.SUCCESS
                    )
                } else {
                    SmartNotificationManager.show(
                        "🚚 Borsa Satış Sevkiyatı Yola Çıktı! Merkez Depo ($originName) ➔ New York Emtia Borsası: $quantity Ton $prodName (+₳${com.example.ui.components.formatCredit(grossIncome)})",
                        "🚚 Borsa Outbound Delivery Dispatched! Central Depot ($originName) ➔ New York Borsa: $quantity Tons of $prodName (+₳${com.example.ui.components.formatCredit(grossIncome)})",
                        NotificationType.SUCCESS
                    )
                }
            }
        }
        updateDailyQuestProgress(QuestType.BORSA_TRADE, 1L)
        updateDailyQuestProgress(QuestType.SELL_COMMODITY, quantity.toLong())
        onBorsaItemSold(baseProductId, quantity, originCountry ?: "Global")
    }
}

fun GameViewModel.sellFacilityStockOnBorsa(facilityId: Any, itemId: String = "", quantity: Int = 100) {
    val biz = businesses.value.find { it.id.toString() == facilityId.toString() } ?: return
    val storedMap = biz.getStoredItemsMap()
    val targetKey = if (itemId.isNotBlank()) {
        storedMap.keys.find { it == itemId || com.example.data.ItemQuality.extractBaseProductId(it) == itemId }
    } else {
        storedMap.keys.firstOrNull()
    } ?: return
    val availableQty = storedMap[targetKey] ?: 0
    if (availableQty <= 0) return
    val sellQty = minOf(availableQty, quantity)

    viewModelScope.launch {
        val bizQuality = if (targetKey.contains("_star")) com.example.data.ItemQuality.extractQuality(targetKey) else com.example.data.QualityCraftingService.getQualityByFacilityLevel(biz.level)
        val baseId = com.example.data.ItemQuality.extractBaseProductId(targetKey)
        val prod = Product.values().find { it.id == baseId }
        val basePrice = marketPrices.value.find { it.itemId == baseId }?.price ?: prod?.basePrice ?: 100L
        val unitPrice = (basePrice * bizQuality.priceMultiplier).toLong()
        val earned = unitPrice * sellQty

        val updatedBiz = biz.withRemovedItem(targetKey, sellQty)
        repository.updateBusiness(updatedBiz)

        val p = player.value
        if (p != null) {
            repository.updatePlayer(p.copy(money = p.money + earned))
        }
        saveEconomicDataToDataStore()
        SmartNotificationManager.show(
            "💰 $sellQty Ton [${bizQuality.starsText}] ${prod?.getDisplayName() ?: baseId} borsada satıldı! (+₳${com.example.ui.components.formatCredit(earned)})",
            "💰 $sellQty Tons of [${bizQuality.starsText}] ${prod?.getDisplayName() ?: baseId} sold on borsa! (+₳${com.example.ui.components.formatCredit(earned)})",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.sellAllFacilityStockOnBorsa(facilityId: Any? = null) {
    val targetBusinesses = if (facilityId != null) {
        businesses.value.filter { it.id.toString() == facilityId.toString() }
    } else {
        businesses.value
    }
    if (targetBusinesses.isEmpty()) return

    viewModelScope.launch {
        var totalEarned = 0L
        var totalTonsSold = 0

        for (biz in targetBusinesses) {
            val stored = biz.getStoredItemsMap()
            if (stored.isEmpty()) continue
            var currentBiz = biz
            for ((storedItemId, qty) in stored) {
                if (qty <= 0) continue
                val quality = if (storedItemId.contains("_star")) com.example.data.ItemQuality.extractQuality(storedItemId) else com.example.data.QualityCraftingService.getQualityByFacilityLevel(biz.level)
                val baseId = com.example.data.ItemQuality.extractBaseProductId(storedItemId)
                val prod = Product.values().find { it.id == baseId }
                val basePrice = marketPrices.value.find { it.itemId == baseId }?.price ?: prod?.basePrice ?: 100L
                val unitPrice = (basePrice * quality.priceMultiplier).toLong()
                val income = unitPrice * qty
                totalEarned += income
                totalTonsSold += qty
                currentBiz = currentBiz.withRemovedItem(storedItemId, qty)
            }
            if (currentBiz != biz) {
                repository.updateBusiness(currentBiz)
            }
        }

        if (totalEarned > 0) {
            val p = player.value
            if (p != null) {
                repository.updatePlayer(p.copy(money = p.money + totalEarned))
            }
            saveEconomicDataToDataStore()
            SmartNotificationManager.show(
                "💰 Tesis Deposu Satışı: $totalTonsSold Ton ürün kalite çarpanlarıyla borsada satıldı! (+₳${com.example.ui.components.formatCredit(totalEarned)})",
                "💰 Facility Storage Sale: $totalTonsSold Tons sold on exchange with quality multipliers! (+₳${com.example.ui.components.formatCredit(totalEarned)})",
                NotificationType.SUCCESS
            )
        }
    }
}

fun GameViewModel.onBorsaItemBought(
    itemId: String,
    quantity: Int,
    originCountry: String? = "Global",
    syncToRemote: Boolean = false
) {
    if (quantity <= 0) return
    val product = Product.values().find { it.id == itemId } ?: return
    val currentPrices = marketPrices.value
    val target = currentPrices.find { it.itemId == itemId }
    val currentStock = target?.borsaStock ?: MacroEconomyEngine.DEFAULT_BORSA_STOCK
    val currentPrice = target?.price ?: product.basePrice.coerceAtLeast(10L)
    val basePrice = product.basePrice.coerceAtLeast(10L)
    
    val (newPrice, newStock) = MacroEconomyEngine.onBorsaProductPurchased(
        currentPrice = currentPrice,
        currentStock = currentStock,
        quantityBought = quantity,
        basePrice = basePrice
    )
    
    // Kriz Kontrolü (Stok 999 Ton ve altına düştüğünde kriz patlak verir ve fiyat 2 katına çıkar)
    val wasInCrisis = currentStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
    val enteredCrisis = !wasInCrisis && newStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
    
    if (enteredCrisis) {
        val crisisMsg = "🚨 KÜRESEL BORSA KRİZİ: ${product.getDisplayName()} rezervleri kritik seviyenin altına indi ($newStock Ton <= 999)! Borsa fiyatı 2 katına fırladı!"
        val crisisMsgEng = "🚨 GLOBAL EXCHANGE CRISIS: ${product.getDisplayName()} reserves dropped below critical threshold ($newStock Tons <= 999)! Price doubled!"
        _newsTickerMessage.value = "$crisisMsg | ${_newsTickerMessage.value}"
        SmartNotificationManager.show(crisisMsg, crisisMsgEng, NotificationType.ALERT)
        
        // ⚡ Canlı Borsa Push Bildirimi (Fiyat/Kriz Alarmı)
        com.example.notification.LocalGameNotificationManager.postBorsaCrisisNotification(
            productDisplayName = product.getDisplayName(),
            price = newPrice,
            stock = newStock
        )
    }
    
    val updatedPrices = currentPrices.map { p ->
        if (p.itemId == itemId) {
            p.copy(
                price = newPrice,
                borsaStock = newStock
            )
        } else {
            p
        }
    }
    
    viewModelScope.launch {
        repository.updateMarketPrices(updatedPrices, syncToRemote = false)
        if (syncToRemote) {
            val updatedEntity = updatedPrices.find { it.itemId == itemId }
            if (updatedEntity != null) {
                SupabaseManager.syncSingleMarketPrice(updatedEntity)
            }
        }
    }
}

fun GameViewModel.onBorsaItemSold(
    itemId: String,
    quantity: Int,
    originCountry: String? = "Global",
    syncToRemote: Boolean = false
) {
    if (quantity <= 0) return
    val product = Product.values().find { it.id == itemId } ?: return
    val currentPrices = marketPrices.value
    val target = currentPrices.find { it.itemId == itemId }
    val currentStock = target?.borsaStock ?: MacroEconomyEngine.DEFAULT_BORSA_STOCK
    val currentPrice = target?.price ?: product.basePrice.coerceAtLeast(10L)
    val basePrice = product.basePrice.coerceAtLeast(10L)
    
    val (newPrice, newStock) = MacroEconomyEngine.onBorsaProductSold(
        currentPrice = currentPrice,
        currentStock = currentStock,
        quantitySold = quantity,
        basePrice = basePrice
    )
    
    // Krizden Çıkış Kontrolü (Stok 999 Ton üstüne çıktığında kriz sona erer)
    val wasInCrisis = currentStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
    val resolvedCrisis = wasInCrisis && newStock > MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
    
    if (resolvedCrisis) {
        val resolvedMsg = "🟢 PİYASA RAHATLADI: ${product.getDisplayName()} borsa rezervleri takviye edildi ($newStock Ton). Kriz sona erdi!"
        val resolvedMsgEng = "🟢 MARKET RELIEVED: ${product.getDisplayName()} exchange reserves replenished ($newStock Tons). Crisis resolved!"
        _newsTickerMessage.value = "$resolvedMsg | ${_newsTickerMessage.value}"
        SmartNotificationManager.show(resolvedMsg, resolvedMsgEng, NotificationType.SUCCESS)
    }
    
    val updatedPrices = currentPrices.map { p ->
        if (p.itemId == itemId) {
            p.copy(
                price = newPrice,
                borsaStock = newStock
            )
        } else {
            p
        }
    }
    
    viewModelScope.launch {
        repository.updateMarketPrices(updatedPrices, syncToRemote = false)
        if (syncToRemote) {
            val updatedEntity = updatedPrices.find { it.itemId == itemId }
            if (updatedEntity != null) {
                SupabaseManager.syncSingleMarketPrice(updatedEntity)
            }
        }
    }
}

fun GameViewModel.resolveNaturalBorsaCountry(itemId: String, currentCity: String? = null): String {
    return "Türkiye"
}

fun GameViewModel.resolveProductBorsaCountry(itemId: String, currentCity: String? = null): String {
    return resolveNaturalBorsaCountry(itemId, currentCity)
}

fun GameViewModel.updatePriceHistory(prices: List<MarketPriceEntity>? = null) {}

// --- BANKING EXTENSIONS ---

fun GameViewModel.calculateTotalFacilityValuation(): Long {
    // Tier 4 Tesisler (Mega Projeler) haczedilemez ve iflas masasına devredilemez, bu nedenle teminatlı kredi limitinde sadece haczedilebilir tesisler hesaplanır
    return businesses.value.filter { b ->
        val prod = com.example.data.Product.values().find { it.facilityId == b.type || it.id == b.type }
        prod?.tier != com.example.data.ProductTier.TIER_4
    }.sumOf { b ->
        val baseCost = calculateFacilityBaseCost(b)
        (baseCost * b.level * 0.9f).toLong()
    }.coerceAtLeast(0L)
}

fun GameViewModel.calculateMaxLoanLimit(): Long {
    val totalFacilityVal = calculateTotalFacilityValuation()
    return (totalFacilityVal / 2L).coerceAtLeast(0L)
}

fun GameViewModel.takeLoan(amount: Long) {
    val p = player.value ?: return
    val totalFacilityVal = calculateTotalFacilityValuation()
    val maxLoanLimit = (totalFacilityVal / 2L).coerceAtLeast(0L)
    val availableLoanLimit = (maxLoanLimit - p.loanAmount).coerceAtLeast(0L)

    if (amount <= 0L) {
        SmartNotificationManager.show("Lütfen geçerli bir kredi miktarı giriniz!", "Please enter a valid loan amount!", NotificationType.ALERT)
        return
    }
    if (totalFacilityVal <= 0L) {
        SmartNotificationManager.show(
            "Teminatsız Kredi Çekilemez! Kredi limiti tesislerinizin toplam değerinin %50'si kadardır. Kredi çekebilmek için en az 1 tesise sahip olmalısınız.",
            "No Collateral! Loan limit is 50% of your total facility valuation. You must own at least 1 facility to take a loan.",
            NotificationType.ALERT
        )
        return
    }
    if (amount > availableLoanLimit) {
        SmartNotificationManager.show(
            "Kredi limitinizi aşıyorsunuz! %50 Tesis Teminat Limiti: ₳${com.example.ui.components.formatCredit(maxLoanLimit)} (Kalan Limit: ₳${com.example.ui.components.formatCredit(availableLoanLimit)})",
            "Exceeding loan limit! 50% Facility Collateral Limit: ₳${com.example.ui.components.formatCredit(maxLoanLimit)} (Available: ₳${com.example.ui.components.formatCredit(availableLoanLimit)})",
            NotificationType.ALERT
        )
        return
    }

    viewModelScope.launch {
        val updatedMoney = p.money + amount
        val updatedLoan = p.loanAmount + amount
        val updatedPlayer = p.copy(money = updatedMoney, loanAmount = updatedLoan)
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
        }
        updateDailyBankingMetrics()
        SmartNotificationManager.show(
            "Kredi Çekildi! +₳${com.example.ui.components.formatCredit(amount)} (Toplam Borç: ₳${com.example.ui.components.formatCredit(updatedLoan)})",
            "Loan Taken! +₳${com.example.ui.components.formatCredit(amount)} (Total Debt: ₳${com.example.ui.components.formatCredit(updatedLoan)})",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.repayLoan(amount: Long) {
    val p = player.value ?: return
    if (p.loanAmount <= 0L) {
        SmartNotificationManager.show("Ödenecek aktif kredi borcunuz bulunmamaktadır!", "You have no active loan debt to repay!", NotificationType.INFO)
        return
    }
    if (amount <= 0L) {
        SmartNotificationManager.show("Lütfen geçerli bir miktar giriniz!", "Please enter a valid amount!", NotificationType.ALERT)
        return
    }
    if (p.money < amount) {
        SmartNotificationManager.show("Yetersiz Bakiye!", "Insufficient Balance!", NotificationType.ALERT)
        return
    }

    val actualRepay = amount.coerceAtMost(p.loanAmount)
    viewModelScope.launch {
        val updatedMoney = p.money - actualRepay
        val updatedLoan = (p.loanAmount - actualRepay).coerceAtLeast(0L)
        val updatedPlayer = p.copy(money = updatedMoney, loanAmount = updatedLoan)
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
        }
        updateDailyBankingMetrics()
        SmartNotificationManager.show(
            "Kredi Ödendi! -₳${com.example.ui.components.formatCredit(actualRepay)} (Kalan Borç: ₳${com.example.ui.components.formatCredit(updatedLoan)})",
            "Loan Repaid! -₳${com.example.ui.components.formatCredit(actualRepay)} (Remaining Debt: ₳${com.example.ui.components.formatCredit(updatedLoan)})",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.depositMoney(amount: Long, isSilent: Boolean = false) {
    val p = player.value ?: return
    if (p.money < amount) {
        if (!isSilent) SmartNotificationManager.show("Yetersiz Bakiye!", "Insufficient Balance!", com.example.ui.components.NotificationType.ALERT)
        return
    }
    viewModelScope.launch {
        val updatedMoney = p.money - amount
        val updatedDeposit = p.depositBalance + amount
        val updatedPlayer = p.copy(money = updatedMoney, depositBalance = updatedDeposit)
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
        }
        updateDailyBankingMetrics()
        if (!isSilent) {
            SmartNotificationManager.show(
                "Mevduat Yatırıldı! -₳${com.example.ui.components.formatCredit(amount)}",
                "Deposit Made! -₳${com.example.ui.components.formatCredit(amount)}",
                com.example.ui.components.NotificationType.SUCCESS
            )
        }
    }
}

fun GameViewModel.withdrawDeposit(amount: Long) {
    val p = player.value ?: return
    if (p.depositBalance < amount) {
        SmartNotificationManager.show("Yetersiz Vadeli Mevduat Bakiyesi!", "Insufficient Term Deposit Balance!", com.example.ui.components.NotificationType.ALERT)
        return
    }
    viewModelScope.launch {
        val updatedMoney = p.money + amount
        val updatedDeposit = p.depositBalance - amount
        val updatedPlayer = p.copy(money = updatedMoney, depositBalance = updatedDeposit)
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
        }
        updateDailyBankingMetrics()
        SmartNotificationManager.show(
            "Mevduat Çekildi! +₳${com.example.ui.components.formatCredit(amount)}",
            "Deposit Withdrawn! +₳${com.example.ui.components.formatCredit(amount)}",
            com.example.ui.components.NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.payDailyLoanInstallmentNow() {
    val p = player.value ?: return
    if (p.loanAmount <= 0L) {
        SmartNotificationManager.show("Ödenecek aktif kredi borcunuz bulunmamaktadır!", "You have no active loan debt to repay!", NotificationType.INFO)
        return
    }
    val gs = gameState.value
    val loanRate = gs?.centralBankLoanRate ?: 0.20f
    val breakdown = com.example.data.BankDailySettlementManager.calculateDailyLoanInstallment(p.loanAmount, loanRate, activeArtifactBuffs.value)
    val totalDue = breakdown.totalDailyInstallment
    val principal = breakdown.principalInstallment

    if (p.money >= totalDue) {
        val updatedMoney = p.money - totalDue
        val updatedLoan = (p.loanAmount - principal).coerceAtLeast(0L)
        val updatedPlayer = p.copy(money = updatedMoney, loanAmount = updatedLoan)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            updateDailyBankingMetrics()
            SmartNotificationManager.show(
                "⚡ Günlük kredi taksiti ödendi! -₳${com.example.ui.components.formatCredit(totalDue)} (Kalan Borç: ₳${com.example.ui.components.formatCredit(updatedLoan)})",
                "⚡ Daily loan installment paid! -₳${com.example.ui.components.formatCredit(totalDue)} (Remaining Debt: ₳${com.example.ui.components.formatCredit(updatedLoan)})",
                NotificationType.SUCCESS
            )
        }
    } else if (p.money + p.depositBalance >= totalDue) {
        val fromMoney = p.money
        val fromDeposit = totalDue - fromMoney
        val updatedDeposit = p.depositBalance - fromDeposit
        val updatedLoan = (p.loanAmount - principal).coerceAtLeast(0L)
        val updatedPlayer = p.copy(money = 0L, depositBalance = updatedDeposit, loanAmount = updatedLoan)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            updateDailyBankingMetrics()
            SmartNotificationManager.show(
                "⚡ Günlük kredi taksiti ödendi (Mevduattan karşılandı)! -₳${com.example.ui.components.formatCredit(totalDue)}",
                "⚡ Daily loan installment paid (from deposit)! -₳${com.example.ui.components.formatCredit(totalDue)}",
                NotificationType.SUCCESS
            )
        }
    } else {
        SmartNotificationManager.show(
            "Yetersiz Bakiye! Günlük taksit tutarı: ₳${com.example.ui.components.formatCredit(totalDue)}",
            "Insufficient Balance! Daily installment amount: ₳${com.example.ui.components.formatCredit(totalDue)}",
            NotificationType.ALERT
        )
    }
}

fun GameViewModel.payAllLoanDebt() {
    val p = player.value ?: return
    if (p.loanAmount <= 0L) {
        SmartNotificationManager.show("Ödenecek aktif kredi borcunuz bulunmamaktadır!", "You have no active loan debt to repay!", NotificationType.INFO)
        return
    }
    val totalDebt = p.loanAmount
    if (p.money >= totalDebt) {
        val updatedMoney = p.money - totalDebt
        val updatedPlayer = p.copy(money = updatedMoney, loanAmount = 0L)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            updateDailyBankingMetrics()
            SmartNotificationManager.show(
                "🎉 Tüm kredi borcunuz başarıyla kapatıldı! -₳${com.example.ui.components.formatCredit(totalDebt)}",
                "🎉 All loan debt successfully paid off! -₳${com.example.ui.components.formatCredit(totalDebt)}",
                NotificationType.SUCCESS
            )
        }
    } else if (p.money + p.depositBalance >= totalDebt) {
        val fromMoney = p.money
        val fromDeposit = totalDebt - fromMoney
        val updatedDeposit = p.depositBalance - fromDeposit
        val updatedPlayer = p.copy(money = 0L, depositBalance = updatedDeposit, loanAmount = 0L)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            updateDailyBankingMetrics()
            SmartNotificationManager.show(
                "🎉 Tüm kredi borcunuz başarıyla kapatıldı (Mevduattan karşılandı)! -₳${com.example.ui.components.formatCredit(totalDebt)}",
                "🎉 All loan debt paid off (from deposit)! -₳${com.example.ui.components.formatCredit(totalDebt)}",
                NotificationType.SUCCESS
            )
        }
    } else {
        SmartNotificationManager.show(
            "Yetersiz Bakiye! Kapatmak için gereken toplam borç: ₳${com.example.ui.components.formatCredit(totalDebt)}",
            "Insufficient Balance! Total debt to pay off: ₳${com.example.ui.components.formatCredit(totalDebt)}",
            NotificationType.ALERT
        )
    }
}

fun GameViewModel.exchangeGemsForMoney(gems: Int) {
    val p = player.value ?: return
    if (p.gems < gems || gems <= 0) {
        SmartNotificationManager.show("Yetersiz Elmas!", "Insufficient Gems!", NotificationType.ALERT)
        return
    }
    viewModelScope.launch {
        val rewardPerGem = 3_000L
        val totalGain = gems * rewardPerGem
        val updatedMoney = p.money + totalGain
        val updatedGems = p.gems - gems
        val updatedPlayer = p.copy(money = updatedMoney, gems = updatedGems)
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
        }
        SmartNotificationManager.show(
            "Elmas Bozduruldu! +₳${com.example.ui.components.formatCredit(totalGain)}",
            "Gems Exchanged! +₳${com.example.ui.components.formatCredit(totalGain)}",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.exchangeGemsForUsd(gems: Int) {}
fun GameViewModel.wipeLoanInterestWithGems(loanId: String) {}
fun GameViewModel.buyUsdWithTry(amountTry: Long) {}
fun GameViewModel.sellUsdForTry(amountUsd: Long) {}
fun GameViewModel.depositUsdToBank(amountUsd: Long) {}
fun GameViewModel.withdrawUsdFromBank(amountUsd: Long) {}
fun GameViewModel.refreshLiveForexRate() {}

fun GameViewModel.updateDailyBankingMetrics(showNotification: Boolean = false) {
    val p = player.value ?: return
    val gs = gameState.value
    val depRate = gs?.centralBankDepositRate ?: 0.15f
    val loanRate = gs?.centralBankLoanRate ?: 0.20f

    _dailyDepositYieldTry.value = com.example.data.BankDailySettlementManager.calculateDailyDepositYield(p.depositBalance, depRate, activeArtifactBuffs.value)
    val breakdown = com.example.data.BankDailySettlementManager.calculateDailyLoanInstallment(p.loanAmount, loanRate, activeArtifactBuffs.value)
    _dailyLoanInstallmentTry.value = breakdown.totalDailyInstallment
    _dailyLoanPrincipalTry.value = breakdown.principalInstallment
    _dailyLoanInterestTry.value = breakdown.dailyInterestCost
}

fun GameViewModel.checkAndPerformBankDailySettlement(showNotification: Boolean = false) {
    val context = repository.economicDataStore?.context ?: return
    val p = player.value ?: return
    val gs = gameState.value
    val depRate = gs?.centralBankDepositRate ?: 0.15f
    val loanRate = gs?.centralBankLoanRate ?: 0.20f

    val currentSnapshot = com.example.data.EconomicSnapshot(
        isIpoActive = _isIpoActive.value,
        publicSharePercent = _publicSharePercent.value,
        totalDividendsPaid = _totalDividendsPaid.value
    )
    val currentBusinesses = businesses.value

    val (updatedPlayer, receipt) = com.example.data.BankDailySettlementManager.performDailySettlementIfDue(
        context = context,
        player = p,
        annualDepositRate = depRate,
        annualLoanRate = loanRate,
        activeArtifactBuffs = activeArtifactBuffs.value,
        isEnglish = false,
        snapshot = currentSnapshot,
        businesses = currentBusinesses
    )

    if (receipt != null) {
        viewModelScope.launch {
            if (receipt.foreclosedFacilities.isNotEmpty()) {
                var totalDebtCleared = 0L
                for (facInfo in receipt.foreclosedFacilities) {
                    repository.deleteBusinessById(facInfo.facilityId)
                    totalDebtCleared += facInfo.debtClearedAmount
                    
                    val calculatedBuyout = (facInfo.valuation * 0.85f).toLong().coerceAtLeast(20_000L)
                    val startingBid = (facInfo.valuation * 0.40f).toLong().coerceAtLeast(10_000L)
                    val newAuction = com.example.data.ForeclosureAuction(
                        id = "AUC-${java.util.UUID.randomUUID().toString().take(8).uppercase()}",
                        originalOwnerId = player.value?.id ?: "player",
                        originalOwnerName = player.value?.name ?: "Şirketiniz (İflas/Haciz)",
                        facilityType = facInfo.facilityType,
                        cityId = facInfo.cityId,
                        level = facInfo.level,
                        startingBid = startingBid,
                        buyoutPrice = calculatedBuyout,
                        currentHighestBid = startingBid,
                        reason = if (facInfo.debtClearedAmount > 0L) "Banka Kredi Temerrüdü (Haciz Satışı - ₳${com.example.ui.components.formatCredit(facInfo.debtClearedAmount)} Borç Kapatıldı)" else facInfo.reason,
                        endsAtMs = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
                        isSettled = false
                    )
                    com.example.data.ForeclosureManager.addAuction(newAuction)
                }
                
                if (totalDebtCleared > 0L) {
                    com.example.ui.components.SmartNotificationManager.show(
                        "⚖️ İflas Masası Haciz & Tasfiye Kararı!",
                        "Kredi borcunuz zamanında ödenemediği için ${receipt.foreclosedFacilities.size} adet tesisiniz en değerlisinden başlanarak haczedildi ve İflas Masasından tasfiye edilerek ₳${com.example.ui.components.formatCredit(totalDebtCleared)} kredi borcunuz kapatıldı!",
                        com.example.ui.components.NotificationType.ALERT
                    )
                } else {
                    com.example.ui.components.SmartNotificationManager.show(
                        "⚠️ Tesis Haczi Gerçekleşti!",
                        "Temerrüt nedeniyle ${receipt.foreclosedFacilities.size} tesisinize el konularak İflas Masasına devredildi!",
                        com.example.ui.components.NotificationType.ALERT
                    )
                }
            }
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            updateDailyBankingMetrics()
            if (showNotification) {
                val netFlow = receipt.totalDepositYieldEarned - receipt.totalLoanInstallmentPaid
                val sign = if (netFlow >= 0) "+" else "-"
                SmartNotificationManager.show(
                    "🏦 Günlük Banka Mutabakatı: $sign₳${com.example.ui.components.formatCredit(kotlin.math.abs(netFlow))} (Mevduat: +₳${com.example.ui.components.formatCredit(receipt.totalDepositYieldEarned)}, Kredi: -₳${com.example.ui.components.formatCredit(receipt.totalLoanInstallmentPaid)})",
                    "🏦 Daily Bank Settlement: $sign₳${com.example.ui.components.formatCredit(kotlin.math.abs(netFlow))} (Deposit: +₳${com.example.ui.components.formatCredit(receipt.totalDepositYieldEarned)}, Loan: -₳${com.example.ui.components.formatCredit(receipt.totalLoanInstallmentPaid)})",
                    if (netFlow >= 0) NotificationType.SUCCESS else NotificationType.INFO
                )
            }
        }
    }
}

// --- SHARES & IPO EXTENSIONS ---

fun GameViewModel.calculateConsortiumValuation(): Long {
    val p = player.value ?: return 0L
    val pId = p.id.ifBlank { "local_player" }
    val pName = p.name

    fun isUserSlot(slot: com.example.data.ConsortiumSupplierSlot): Boolean {
        if (slot.assignedPartnerId == "local_player" || slot.assignedPartnerId == pId) return true
        if (pName.isNotBlank() && (slot.assignedPartnerName == pName || slot.assignedPartnerId == pName)) return true
        return false
    }

    val priceMap = marketPrices.value.associateBy({ it.itemId }, { it.price })
    
    // 1. Konsorsiyuma teslim edilen tüm hammadde ve ara mamullerin güncel piyasa ve maliyet değeri
    val deliveredMaterialsVal = _megaProjects.value.sumOf { project ->
        project.slots.filter { isUserSlot(it) && it.quantityDelivered > 0 }.sumOf { slot ->
            val mPrice = priceMap[slot.productId] ?: (com.example.data.Product.values().find { it.id == slot.productId }?.basePrice ?: 1000L)
            maxOf((mPrice * slot.quantityDelivered * 0.8f).toLong(), slot.costContributionValue)
        }
    }

    // 2. Oyuncunun sahip olduğu konsorsiyum / lonca hisselerinin portföy değeri
    val guildSharesVal = _playerGuildShares.value.entries.sumOf { (guildId, quantity) ->
        if (quantity <= 0) return@sumOf 0L
        val proj = _megaProjects.value.find { it.id == guildId }
        val sharePrice = if (proj != null) {
            val initialPrice = proj.baseSharePrice
            (initialPrice * (1.0 + (quantity * 0.002))).toLong()
        } else {
            (_playerGuildBuyPrices.value[guildId] ?: 1000.0).toLong()
        }
        sharePrice * quantity
    }

    return (deliveredMaterialsVal + guildSharesVal).coerceAtLeast(0L)
}

fun GameViewModel.calculateConsortiumDeliveredMaterialsValuation(): Long {
    val p = player.value ?: return 0L
    val pId = p.id.ifBlank { "local_player" }
    val pName = p.name

    fun isUserSlot(slot: com.example.data.ConsortiumSupplierSlot): Boolean {
        if (slot.assignedPartnerId == "local_player" || slot.assignedPartnerId == pId) return true
        if (pName.isNotBlank() && (slot.assignedPartnerName == pName || slot.assignedPartnerId == pName)) return true
        return false
    }

    val priceMap = marketPrices.value.associateBy({ it.itemId }, { it.price })
    return _megaProjects.value.sumOf { project ->
        project.slots.filter { isUserSlot(it) && it.quantityDelivered > 0 }.sumOf { slot ->
            val mPrice = priceMap[slot.productId] ?: (com.example.data.Product.values().find { it.id == slot.productId }?.basePrice ?: 1000L)
            maxOf((mPrice * slot.quantityDelivered * 0.8f).toLong(), slot.costContributionValue)
        }
    }.coerceAtLeast(0L)
}

fun GameViewModel.calculateRdInvestmentValuation(): Long {
    var totalRdSpent = 0L
    val allTechKeys = (com.example.data.TechTree.nodes.map { it.id.removePrefix("tech_") } +
            _researchLevels.value.keys.map { it.removePrefix("tech_") }).distinct()

    // 1. Tamamlanan ve kazanılan teknoloji seviyeleri için harcanan nakit tutarları
    allTechKeys.forEach { baseId ->
        val currentLvl = getTechLevel(baseId)
        for (lvl in 0 until currentLvl) {
            totalRdSpent += calculateTechCost(baseId, lvl)
        }
    }

    // 2. Halihazırda yürütülen aktif Ar-Ge araştırmaları için kasadan peşin ödenen nakit
    _activeResearches.value.keys.map { it.removePrefix("tech_") }.distinct().forEach { baseId ->
        val currentLvl = getTechLevel(baseId)
        if (currentLvl < 5) {
            totalRdSpent += calculateTechCost(baseId, currentLvl)
        }
    }

    return totalRdSpent.coerceAtLeast(0L)
}

fun GameViewModel.calculateCompanyValuation(): Long {
    val p = player.value ?: return 0L
    
    // 1. Nakit, mevduat, vadeli mevduat ve kredi borçları (TRY)
    val tryNet = p.money + p.depositBalance + p.lockedDepositBalance - p.loanAmount
    
    // 2. Dolar varlıkları (Dolar kasası, dolar mevduat hesabı, dolar kredi borçları)
    val usdRate = ForexRateManager.currentUsdRate.coerceAtLeast(1.0)
    val usdNet = p.dollarBalance + p.dollarDepositBalance - p.dollarLoanAmount
    val convertedUsdNet = (usdNet * usdRate).toLong()
    
    // 3. Tesislerin toplam değeri (Facility base cost * level * 0.9f)
    val busVal = businesses.value.sumOf { b ->
        val baseCost = calculateFacilityBaseCost(b)
        (baseCost * b.level * 0.9f).toLong()
    }
    
    // 4. Depodaki ürünlerin piyasa değeri (market price * quantity * 0.8f)
    val priceMap = marketPrices.value.associateBy({ it.itemId }, { it.price })
    val invVal = inventory.value.sumOf { i ->
        val price = priceMap[i.itemId] ?: 100L
        (price * i.quantity * 0.8f).toLong()
    }
    
    // 5. Konsorsiyuma gönderilen hammaddelerin ve ortaklık hisselerinin değeri
    val consortiumVal = calculateConsortiumValuation()
    
    // 6. Ar-Ge bölümünde harcanan nakitler ve teknoloji sermaye değeri
    val rdVal = calculateRdInvestmentValuation()
    
    val totalNetWorth = tryNet + convertedUsdNet + busVal + invVal + consortiumVal + rdVal
    return totalNetWorth.coerceAtLeast(0L)
}

fun GameViewModel.sellAdditionalShares(shares: Int) {}
fun GameViewModel.launchIpo(sharesToOfferRatio: Float) {}
fun GameViewModel.launchIpo(sharesToOfferRatio: Int) {
    launchIpo(sharesToOfferRatio.toFloat())
}
fun GameViewModel.distributeDividends(totalAmount: Long) {}
fun GameViewModel.buybackShares(sharesToBuyback: Int) {}
fun GameViewModel.buyGuildShares(guildId: String, quantity: Int) {
    if (quantity <= 0) return
    val p = player.value ?: return
    val proj = _megaProjects.value.find { it.id == guildId } ?: return
    
    val owned = _playerGuildShares.value[guildId] ?: 0
    val currentSharePrice = proj.currentSharePrice
    val totalCost = (currentSharePrice * quantity).toLong()
    
    if (p.money < totalCost) {
        com.example.ui.components.SmartNotificationManager.show(
            "Yetersiz bakiye! (Gereken: ₳${com.example.ui.components.formatMoney(totalCost)})",
            "Insufficient balance! (Required: ₳${com.example.ui.components.formatMoney(totalCost)})",
            com.example.ui.components.NotificationType.ALERT
        )
        return
    }
    
    val currentShares = _playerGuildShares.value.toMutableMap()
    currentShares[guildId] = owned + quantity
    _playerGuildShares.value = currentShares
    
    val currentPrices = _playerGuildBuyPrices.value.toMutableMap()
    currentPrices[guildId] = currentSharePrice
    _playerGuildBuyPrices.value = currentPrices
    
    // Alım talebi hisse fiyatını yukarı doğru yeniden değerler (Supply/Demand Model)
    val curMultiplier = if (proj.sharePriceMultiplier > 0.0) proj.sharePriceMultiplier else 1.0
    val newMultiplier = (curMultiplier * (1.0 + (quantity * 0.008))).coerceIn(0.20, 5.0)
    val updatedProject = proj.copy(
        previousSharePrice = currentSharePrice,
        sharePriceMultiplier = newMultiplier
    )
    _megaProjects.update { list -> list.map { if (it.id == guildId) updatedProject else it } }
    
    val updatedPlayer = p.copy(money = p.money - totalCost)
    
    viewModelScope.launch {
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStoreSuspend(customPlayer = updatedPlayer)
    }
    
    val changePct = updatedProject.sharePriceChangePercent
    val signStr = if (changePct >= 0) "+" else ""
    com.example.ui.components.SmartNotificationManager.show(
        "+$quantity ${proj.brandName} hissesi alındı! Güncel Değer: ₳${com.example.ui.components.formatMoney(updatedProject.currentSharePrice.toLong())} ($signStr${String.format(java.util.Locale.US, "%.1f", changePct)}%)",
        "+$quantity ${proj.brandName} shares purchased! Current Price: ₳${com.example.ui.components.formatMoney(updatedProject.currentSharePrice.toLong())} ($signStr${String.format(java.util.Locale.US, "%.1f", changePct)}%)",
        com.example.ui.components.NotificationType.SUCCESS
    )
}

fun GameViewModel.sellGuildShares(guildId: String, quantity: Int) {
    if (quantity <= 0) return
    val p = player.value ?: return
    val proj = _megaProjects.value.find { it.id == guildId } ?: return
    
    val owned = _playerGuildShares.value[guildId] ?: 0
    if (owned < quantity) return
    
    val currentSharePrice = proj.currentSharePrice
    val totalRevenue = (currentSharePrice * quantity).toLong()
    
    val currentShares = _playerGuildShares.value.toMutableMap()
    currentShares[guildId] = owned - quantity
    if (currentShares[guildId] == 0) {
        currentShares.remove(guildId)
    }
    _playerGuildShares.value = currentShares
    
    // Satış arzı hisse fiyatını gevşetir ve yeniden değerler
    val curMultiplier = if (proj.sharePriceMultiplier > 0.0) proj.sharePriceMultiplier else 1.0
    val newMultiplier = (curMultiplier * (1.0 - (quantity * 0.008))).coerceIn(0.20, 5.0)
    val updatedProject = proj.copy(
        previousSharePrice = currentSharePrice,
        sharePriceMultiplier = newMultiplier
    )
    _megaProjects.update { list -> list.map { if (it.id == guildId) updatedProject else it } }
    
    val updatedPlayer = p.copy(money = p.money + totalRevenue)
    
    viewModelScope.launch {
        repository.updatePlayer(updatedPlayer)
        saveEconomicDataToDataStoreSuspend(customPlayer = updatedPlayer)
    }
    
    val changePct = updatedProject.sharePriceChangePercent
    val signStr = if (changePct >= 0) "+" else ""
    com.example.ui.components.SmartNotificationManager.show(
        "-$quantity ${proj.brandName} hissesi satıldı (+₳${com.example.ui.components.formatMoney(totalRevenue)})! Güncel Değer: ₳${com.example.ui.components.formatMoney(updatedProject.currentSharePrice.toLong())} ($signStr${String.format(java.util.Locale.US, "%.1f", changePct)}%)",
        "-$quantity ${proj.brandName} shares sold (+₳${com.example.ui.components.formatMoney(totalRevenue)})! Current Price: ₳${com.example.ui.components.formatMoney(updatedProject.currentSharePrice.toLong())} ($signStr${String.format(java.util.Locale.US, "%.1f", changePct)}%)",
        com.example.ui.components.NotificationType.SUCCESS
    )
}
fun GameViewModel.buyIpoShares(companyId: String, quantity: Int) {
    buyGuildShares(companyId, quantity)
}
fun GameViewModel.sellIpoShares(companyId: String, quantity: Int) {
    sellGuildShares(companyId, quantity)
}

// --- CONSORTIUM / MEGA PROJECT EXTENSIONS ---

fun GameViewModel.joinConsortiumSlot(projectId: String, slotId: String, playerId: String = "", playerName: String = "") {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val slot = project.slots.find { it.slotId == slotId } ?: return
    if (slot.assignedPartnerId != null) return

    val p = player.value
    val finalId = if (playerId.isNotBlank() && playerId != "local_player") playerId else (p?.id?.ifBlank { "local_player" } ?: "local_player")
    val finalName = if (playerName.isNotBlank()) playerName else (p?.name ?: "Tüccar")

    // Dinamik Tedarik Kotaları & Adil Dağıtım Kontrolü (%40 Kota Tavanı / Azami 2 Slot)
    val (canClaim, claimReason) = project.canPlayerClaimSlot(finalId)
    if (!canClaim) {
        SmartNotificationManager.show(claimReason, claimReason, NotificationType.ALERT)
        return
    }

    val updatedSlots = project.slots.map {
        if (it.slotId == slotId) it.copy(assignedPartnerId = finalId, assignedPartnerName = finalName) else it
    }
    val updatedProject = project.copy(slots = updatedSlots)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show(
            "${slot.productName} Tedarikçi Slotuna Katıldın! 🤝",
            "Joined ${slot.productName} Supplier Slot! 🤝",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.leaveConsortiumSlot(projectId: String, slotId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val slot = project.slots.find { it.slotId == slotId } ?: return
    val delivered = slot.quantityDelivered

    val returnQty = if (delivered > 0) (delivered * 80) / 100 else 0
    val penaltyQty = delivered - returnQty

    val updatedCost = if (delivered > 0) (slot.costContributionValue * penaltyQty) / delivered else 0L
    val updatedActual = if (delivered > 0) (slot.actualCostIncurred * penaltyQty) / delivered else 0L

    val updatedSlots = project.slots.map {
        if (it.slotId == slotId) {
            it.copy(
                assignedPartnerId = null,
                assignedPartnerName = null,
                quantityDelivered = penaltyQty,
                costContributionValue = updatedCost,
                actualCostIncurred = updatedActual,
                isBottleneckWarning = false
            )
        } else it
    }
    val updatedProject = project.copy(slots = updatedSlots)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    viewModelScope.launch {
        if (returnQty > 0) {
            val currentStock = inventory.value.find { it.itemId == slot.productId }?.quantity ?: 0
            repository.insertInventory(com.example.data.InventoryEntity(slot.productId, currentStock + returnQty))
        }
        syncMegaProject(updatedProject, force = true)
        saveEconomicDataToDataStore(immediate = true)

        if (delivered > 0) {
            SmartNotificationManager.show(
                "Slottan ayrıldın. Sevk edilen $delivered Ton ürünün %80'i ($returnQty Ton ${slot.productName}) depona iade edildi (%20 ceza kesildi: $penaltyQty Ton).",
                "Left slot. 80% ($returnQty Tons of ${slot.productName}) refunded to your warehouse (20% penalty retained: $penaltyQty Tons).",
                NotificationType.INFO
            )
        } else {
            SmartNotificationManager.show("Slottan Ayrıldın!", "Left the Slot!", NotificationType.INFO)
        }
    }
}

fun GameViewModel.leaveEntireConsortium(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val p = player.value
    val pId = p?.id ?: "local_player"
    val pName = p?.name ?: ""

    var totalDelivered = 0
    var totalRefunded = 0
    var totalPenalty = 0

    val updatedSlots = project.slots.map { slot ->
        val isMySlot = slot.assignedPartnerId == pId || slot.assignedPartnerId == "local_player" || (pName.isNotBlank() && slot.assignedPartnerName == pName)
        if (isMySlot) {
            val delivered = slot.quantityDelivered
            totalDelivered += delivered
            val returnQty = if (delivered > 0) (delivered * 80) / 100 else 0
            val penaltyQty = delivered - returnQty
            totalRefunded += returnQty
            totalPenalty += penaltyQty

            val updatedCost = if (delivered > 0) (slot.costContributionValue * penaltyQty) / delivered else 0L
            val updatedActual = if (delivered > 0) (slot.actualCostIncurred * penaltyQty) / delivered else 0L

            slot.copy(
                assignedPartnerId = null,
                assignedPartnerName = null,
                quantityDelivered = penaltyQty,
                costContributionValue = updatedCost,
                actualCostIncurred = updatedActual,
                isBottleneckWarning = false
            )
        } else {
            slot
        }
    }
    val updatedProject = project.copy(slots = updatedSlots)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    viewModelScope.launch {
        project.slots.forEach { slot ->
            val isMySlot = slot.assignedPartnerId == pId || slot.assignedPartnerId == "local_player" || (pName.isNotBlank() && slot.assignedPartnerName == pName)
            if (isMySlot && slot.quantityDelivered > 0) {
                val returnQty = (slot.quantityDelivered * 80) / 100
                if (returnQty > 0) {
                    val currentStock = inventory.value.find { it.itemId == slot.productId }?.quantity ?: 0
                    repository.insertInventory(com.example.data.InventoryEntity(slot.productId, currentStock + returnQty))
                }
            }
        }
        syncMegaProject(updatedProject, force = true)
        saveEconomicDataToDataStore(immediate = true)

        if (totalDelivered > 0) {
            SmartNotificationManager.show(
                "Konsorsiyumdan ayrıldın. Sevk ettiğin ürünlerin %80'i ($totalRefunded Ton) depona iade edildi (%20 ceza kesildi: $totalPenalty Ton).",
                "Left consortium. 80% ($totalRefunded Tons) of delivered goods refunded to your warehouse (20% penalty: $totalPenalty Tons).",
                NotificationType.INFO
            )
        } else {
            SmartNotificationManager.show("Konsorsiyumdan Ayrıldın!", "Left Consortium!", NotificationType.INFO)
        }
    }
}

fun GameViewModel.takeoverBottleneckSlot(projectId: String, slotId: String, playerId: String = "", playerName: String = "") {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val p = player.value
    val finalId = if (playerId.isNotBlank() && playerId != "local_player") playerId else (p?.id?.ifBlank { "local_player" } ?: "local_player")
    val finalName = if (playerName.isNotBlank()) playerName else (p?.name ?: "Tüccar")

    val updatedSlots = project.slots.map {
        if (it.slotId == slotId) it.copy(assignedPartnerId = finalId, assignedPartnerName = finalName, isBottleneckWarning = false) else it
    }
    val updatedProject = project.copy(slots = updatedSlots)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show("Tedarik Slotunu Devraldın! 🚀", "Took Over Bottleneck Slot! 🚀", NotificationType.SUCCESS)
    }
}

fun GameViewModel.kickPartnerFromConsortiumSlot(projectId: String, slotId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val updatedSlots = project.slots.map {
        if (it.slotId == slotId) it.copy(assignedPartnerId = null, assignedPartnerName = null) else it
    }
    val updatedProject = project.copy(slots = updatedSlots)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show("Ortak Çıkarıldı!", "Partner Kicked From Slot!", NotificationType.INFO)
    }
}

fun GameViewModel.sellConsortiumWarehouseStock(projectId: String, isSilent: Boolean = false) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    if (project.warehouseStock <= 0) return
    
    val unitPrice = project.unitBatchPrice
    val totalRevenue = unitPrice * project.warehouseStock
    
    val updatedProject = project.copy(
        warehouseStock = 0
    )
    
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    
    val p = player.value
    if (p != null) {
        viewModelScope.launch {
            val updatedMoney = p.money + totalRevenue
            repository.updatePlayer(p.copy(money = updatedMoney))
            saveEconomicDataToDataStore(immediate = true)
            
            if (!isSilent) {
                SmartNotificationManager.show(
                    "Konsorsiyum Satışı Başarılı! +₳${com.example.ui.components.formatCredit(totalRevenue)} kazanıldı.",
                    "Consortium Sale Successful! +₳${com.example.ui.components.formatCredit(totalRevenue)} earned.",
                    NotificationType.SUCCESS
                )
            }
        }
    }
}

fun GameViewModel.advanceMegaProjectStage(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val nextStage = when (project.currentStage) {
        com.example.data.MegaProjectStage.STAGE_1_BODY -> com.example.data.MegaProjectStage.STAGE_2_HARDWARE
        com.example.data.MegaProjectStage.STAGE_2_HARDWARE -> com.example.data.MegaProjectStage.STAGE_3_TESTING
        com.example.data.MegaProjectStage.STAGE_3_TESTING -> com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION
        else -> com.example.data.MegaProjectStage.COMPLETED
    }
    val updatedProject = project.copy(
        currentStage = nextStage,
        completedAtMs = if (nextStage == com.example.data.MegaProjectStage.COMPLETED) System.currentTimeMillis() else project.completedAtMs
    )
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show(
            "Konsorsiyum Yeni Aşamaya Geçti! 🏆",
            "Consortium Advanced to New Stage! 🏆",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.claimMegaProjectDividend(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    if (project.isDividendClaimed) return

    val p = player.value ?: return
    val pId = p.id
    val pName = p.name
    val cleanEmail = _onlineEmail.value.replace(".", "_")

    val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player" || (cleanEmail.isNotBlank() && project.leaderPlayerId.contains(cleanEmail))
    val userSlot = project.slots.find { it.assignedPartnerId == pId || it.assignedPartnerId == "local_player" || it.assignedPartnerName == pName }

    val dividendAmount = if (isLeader) {
        (project.totalProjectValue * 0.40).toLong()
    } else if (userSlot != null) {
        (project.totalProjectValue * (userSlot.sharePercentage / 100.0)).toLong()
    } else {
        0L
    }

    if (dividendAmount > 0) {
        val updatedProject = project.copy(isDividendClaimed = true)
        _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
        syncMegaProject(updatedProject, force = true)

        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money + dividendAmount))
            saveEconomicDataToDataStore(immediate = true)
            SmartNotificationManager.show(
                "Temettü Alındı! +₳${com.example.ui.components.formatCredit(dividendAmount)} 💰",
                "Dividend Claimed! +₳${com.example.ui.components.formatCredit(dividendAmount)} 💰",
                NotificationType.SUCCESS
            )
        }
    }
}

fun GameViewModel.produceConsortiumBrandItem(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    if (project.warehouseStock >= project.warehouseCapacity) {
        SmartNotificationManager.show("Konsorsiyum Deposu Dolu!", "Consortium Warehouse Full!", NotificationType.ALERT)
        return
    }

    val updatedProject = project.copy(
        warehouseStock = project.warehouseStock + 1,
        totalItemsProduced = project.totalItemsProduced + 1,
        isTestProductProduced = true,
        testProducedAtMs = System.currentTimeMillis()
    )
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show("Marka Ürünü Üretildi! 📦", "Brand Product Produced! 📦", NotificationType.SUCCESS)
    }
}

fun GameViewModel.resetConsortiumNewBatch(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val resetSlots = project.slots.map { it.copy(quantityDelivered = 0) }
    val updatedProject = project.copy(
        slots = resetSlots,
        currentStage = com.example.data.MegaProjectStage.STAGE_1_BODY,
        isDividendClaimed = false,
        isTestProductProduced = false
    )
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show("Yeni Üretim Döngüsü Başlatıldı!", "New Production Batch Started!", NotificationType.INFO)
    }
}

fun GameViewModel.listenToConsortiumChat(projectId: String) {
    repository.startListeningToConsortiumChat(projectId) { updatedMessages ->
        _consortiumChatMessages.update { current ->
            current + (projectId to updatedMessages)
        }
    }
}

fun GameViewModel.stopListeningToConsortiumChat(projectId: String) {
    repository.stopListeningToConsortiumChat(projectId)
}

fun GameViewModel.disbandConsortium(projectId: String) {
    _megaProjects.value = _megaProjects.value.filter { it.id != projectId }
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        com.example.data.SupabaseManager.deleteMegaProjectFromSupabase(projectId)
        com.example.data.MultiplayerManager.refreshGuildsFromSupabase()
        saveEconomicDataToDataStore(immediate = true)
    }
    SmartNotificationManager.show("Konsorsiyum Feshedildi!", "Consortium Disbanded!", NotificationType.INFO)
}

fun GameViewModel.sendConsortiumChatMessage(projectId: String, text: String) {
    if (text.isBlank()) return
    val p = player.value
    val pId = p?.id ?: "local_player"
    val pName = p?.name ?: "Tüccar"

    val newMsg = com.example.data.ConsortiumChatMessage(
        id = java.util.UUID.randomUUID().toString(),
        projectId = projectId,
        senderId = pId,
        senderName = pName,
        senderRole = "Üye",
        messageText = text,
        timestampMs = System.currentTimeMillis(),
        isSystemMessage = false
    )

    _consortiumChatMessages.update { current ->
        val list = current[projectId] ?: emptyList()
        current + (projectId to (list + newMsg))
    }

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        com.example.data.MultiplayerManager.sendBroadcastChatMessage(newMsg)
        com.example.data.SupabaseManager.sendConsortiumChatMessageToSupabase(newMsg)
    }
}

fun GameViewModel.createNewMegaProject(
    consortiumName: String,
    brandName: String,
    targetProductId: String,
    qualityTier: com.example.data.ConsortiumQualityTier = com.example.data.ConsortiumQualityTier.GRADE_C,
    founderClaimedProductIds: Set<String> = emptySet(),
    cityId: String = ""
) {
    val p = player.value ?: return
    val chosenCityId = if (cityId.isNotBlank()) cityId else if (p.currentCity.isNotBlank()) p.currentCity else "istanbul"
    val leaderId = p.id.ifBlank { _onlineEmail.value.replace(".", "_").ifBlank { "local_player" } }
    val leaderName = p.name.ifBlank { "Tüccar" }

    try {
        val newProj = com.example.data.MegaProjectFactory.createMegaProject(
            consortiumName = consortiumName,
            brandName = brandName,
            targetProductId = targetProductId,
            leaderPlayerId = leaderId,
            leaderPlayerName = leaderName,
            allProducts = com.example.data.Product.values().toList(),
            qualityTier = qualityTier,
            founderClaimedProductIds = founderClaimedProductIds,
            cityId = chosenCityId
        )

        _megaProjects.update { current -> current + newProj }
        localMegaProjectUpdates[newProj.id] = System.currentTimeMillis()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            saveEconomicDataToDataStore(immediate = true)
            val synced = com.example.data.SupabaseManager.syncMegaProjectToSupabase(newProj)
            if (synced) {
                com.example.data.MultiplayerManager.sendBroadcastConsortiumCreated(
                    com.example.data.network.LiveConsortiumCreatedEventDto(
                        projectId = newProj.id,
                        consortiumName = newProj.consortiumName,
                        targetProductId = newProj.targetProductId,
                        leaderName = newProj.leaderPlayerName,
                        timestampMs = System.currentTimeMillis()
                    )
                )
                com.example.data.MultiplayerManager.refreshGuildsFromSupabase()
            }
        }

        SmartNotificationManager.show(
            "Konsorsiyum Kuruldu: $consortiumName 🚀",
            "Consortium Created: $consortiumName 🚀",
            NotificationType.SUCCESS
        )
    } catch (e: Exception) {
        android.util.Log.e("GameViewModel", "Error creating mega project", e)
        SmartNotificationManager.show(
            "Konsorsiyum kurulurken hata oluştu!",
            "Error creating consortium!",
            NotificationType.ALERT
        )
    }
}

fun GameViewModel.deliverMaterialsToConsortium(projectId: String, slotId: String, quantity: Int, isSilent: Boolean = false) {
    if (quantity <= 0) return
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val slot = project.slots.find { it.slotId == slotId } ?: return

    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"

    // Dinamik Tedarik Kotaları & Adil Dağıtım Kontrolü
    val (canDeliver, deliverReason) = project.canPlayerDeliver(pId, slotId, quantity)
    if (!canDeliver) {
        if (!isSilent) {
            SmartNotificationManager.show(deliverReason, deliverReason, NotificationType.ALERT)
        }
        return
    }

    // Projenin hedef kalite standardına (A, B, C) uygun envanter kalemlerini bul
    val currentInv = inventory.value
    val matchingItems = currentInv.filter { item ->
        val baseId = com.example.data.ItemQuality.extractBaseProductId(item.itemId)
        val itemQ = item.quality
        baseId == slot.productId && project.qualityTier.isQualityAllowed(itemQ) && item.quantity > 0
    }

    val totalAvailable = matchingItems.sumOf { it.quantity }
    if (totalAvailable <= 0) {
        if (!isSilent) {
            val allForProduct = currentInv.filter { com.example.data.ItemQuality.extractBaseProductId(it.itemId) == slot.productId && it.quantity > 0 }
            val msg = if (allForProduct.isNotEmpty()) {
                "Bu konsorsiyum ${project.qualityTier.titleTr} standardındadır! Yalnızca ${project.qualityTier.allowedQualityRangeTextTr} kabul edilir. Depondaki ürünlerin kalitesi uygun değil."
            } else {
                "Yetersiz Envanter! Deponda ${slot.productName} bulunmuyor."
            }
            SmartNotificationManager.show(msg, NotificationType.ALERT)
        }
        return
    }

    val deliverQty = minOf(quantity, slot.remainingQuantity, totalAvailable)
    if (deliverQty <= 0) return

    // Envanterden uygun kalitelerden düş ve ağırlıklı kalite puanını hesapla
    var remainingToDeduct = deliverQty
    var weightedQualitySum = 0.0
    val inventoryUpdates = mutableListOf<com.example.data.InventoryEntity>()

    val sortedMatching = matchingItems.sortedByDescending { it.quality.stars }
    for (invItem in sortedMatching) {
        if (remainingToDeduct <= 0) break
        val deductFromThis = minOf(invItem.quantity, remainingToDeduct)
        remainingToDeduct -= deductFromThis
        weightedQualitySum += (invItem.quality.stars * deductFromThis)
        val newQty = invItem.quantity - deductFromThis
        inventoryUpdates.add(com.example.data.InventoryEntity(invItem.itemId, newQty))
    }

    val actualAvgDeliveredQuality = if (deliverQty > 0) (weightedQualitySum / deliverQty).coerceIn(1.0, 5.0) else 1.0
    val deliveredQualityTierInt = kotlin.math.round(actualAvgDeliveredQuality).toInt().coerceIn(1, 5)

    val unitCost = com.example.data.Product.values().find { it.id == slot.productId }?.basePrice ?: 1000L

    val updatedSlot = slot.copy(
        quantityDelivered = slot.quantityDelivered + deliverQty,
        costContributionValue = slot.costContributionValue + (unitCost * deliverQty),
        lastDeliveryTimeMs = System.currentTimeMillis(),
        isBottleneckWarning = false,
        deliveredQualityTier = deliveredQualityTierInt,
        deliveredQualityScore = actualAvgDeliveredQuality
    )
    val updatedSlots = project.slots.map { if (it.slotId == slotId) updatedSlot else it }
    val deliveredSlots = updatedSlots.filter { it.quantityDelivered > 0 }
    val newAvgScore = if (deliveredSlots.isNotEmpty()) deliveredSlots.map { it.deliveredQualityScore }.average().coerceIn(1.0, 5.0) else 1.0
    val updatedProject = project.copy(
        slots = updatedSlots,
        averageCraftsmanshipScore = newAvgScore
    )

    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    viewModelScope.launch {
        inventoryUpdates.forEach { ent ->
            repository.insertInventory(ent)
        }
        syncMegaProject(updatedProject, force = true)
        saveEconomicDataToDataStore(immediate = true)

        if (!isSilent) {
            val qBadge = com.example.data.ItemQuality.fromStars(deliveredQualityTierInt).label
            SmartNotificationManager.show(
                "$deliverQty Adet ${slot.productName} ($qBadge) Teslim Edildi! 📦",
                "Delivered $deliverQty units of ${slot.productName} ($qBadge)! 📦",
                NotificationType.SUCCESS
            )
        }
    }
}

fun GameViewModel.approveConsortiumMassProduction(projectId: String, isSilent: Boolean = false) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val updatedProject = project.copy(
        isMassProductionApproved = true,
        massProductionApprovedAtMs = System.currentTimeMillis(),
        currentStage = com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION
    )
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        if (!isSilent) {
            SmartNotificationManager.show("Seri Üretim Onaylandı! 🏭", "Mass Production Approved! 🏭", NotificationType.SUCCESS)
        }
    }
}

fun GameViewModel.toggleConsortiumProductionState(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val updatedProject = project.copy(isProductionPaused = !project.isProductionPaused)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        val msg = if (updatedProject.isProductionPaused) "Üretim Duraklatıldı ⏸️" else "Üretim Devam Ediyor ▶️"
        SmartNotificationManager.show(msg, msg, NotificationType.INFO)
    }
}

fun GameViewModel.toggleConsortiumAutoSell(projectId: String, autoSellActive: Boolean = true) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val updatedProject = project.copy(isAutoSellActive = autoSellActive)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
    }
}

fun GameViewModel.upgradeConsortiumWarehouseWithGems(projectId: String, costGems: Int = 100) {
    val p = player.value ?: return
    if (p.gems < costGems) {
        SmartNotificationManager.show("Yetersiz Elmas! ($costGems gerekli) 💎", "Not enough Gems! ($costGems required) 💎", NotificationType.ALERT)
        return
    }
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val updatedProject = project.copy(warehouseCapacity = project.warehouseCapacity + 1000)
    _megaProjects.value = _megaProjects.value.map { if (it.id == projectId) updatedProject else it }

    viewModelScope.launch {
        repository.updatePlayer(p.copy(gems = p.gems - costGems))
        syncMegaProject(updatedProject, force = true)
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show(
            "Konsorsiyum Deposu Genişletildi! (+1,000 Kapasite) 🏗️",
            "Consortium Warehouse Expanded! (+1,000 Capacity) 🏗️",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.changeConsortiumSalesChannel(projectId: String, channel: com.example.data.ConsortiumSalesChannel) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"
    val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player"
    if (!isLeader) {
        SmartNotificationManager.show(
            "Yalnızca Konsorsiyum Başkanı satış kanalını değiştirebilir!",
            "Only the Consortium President can change the sales channel!",
            NotificationType.ALERT
        )
        return
    }

    val updatedProject = project.copy(salesChannel = channel)
    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        val channelName = channel.getTitle()
        SmartNotificationManager.show(
            "Satış Kanalı Güncellendi: $channelName 🏛️",
            "Sales Channel Updated: $channelName 🏛️",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.setConsortiumProductionStrategy(projectId: String, strategy: com.example.data.ConsortiumProductionStrategy) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"
    val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player"
    if (!isLeader) {
        SmartNotificationManager.show(
            "Yalnızca Konsorsiyum Başkanı üretim stratejisini doğrudan değiştirebilir. Ortaklar Yönetim Kurulu'na tasarı sunabilir!",
            "Only the Consortium President can directly change the production strategy. Partners can submit proposals!",
            NotificationType.ALERT
        )
        return
    }

    val updatedProject = project.copy(productionStrategy = strategy)
    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        val strategyTitle = strategy.getTitle()
        SmartNotificationManager.show(
            "Üretim Stratejisi Güncellendi: $strategyTitle ⚙️",
            "Production Strategy Updated: $strategyTitle ⚙️",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.createConsortiumBoardProposal(
    projectId: String,
    titleTr: String,
    titleEn: String,
    descriptionTr: String,
    descriptionEn: String,
    proposalType: com.example.data.ConsortiumProposalType,
    proposedValue: String
) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"
    val pName = curPlayer?.name?.ifBlank { "Sanayi Ortağı" } ?: "Sanayi Ortağı"

    val newProposal = com.example.data.ConsortiumBoardProposal(
        proposerPlayerId = pId,
        proposerPlayerName = pName,
        titleTr = titleTr,
        titleEn = titleEn,
        descriptionTr = descriptionTr,
        descriptionEn = descriptionEn,
        proposalType = proposalType,
        proposedValue = proposedValue,
        votes = mapOf(pId to true)
    )

    val updatedProposals = (project.boardProposals + newProposal).takeLast(10)
    val updatedProject = project.copy(boardProposals = updatedProposals)
    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show(
            "🏛️ Yönetim Kurulu Tasarısı Sunuldu: $titleTr",
            "🏛️ Board Proposal Submitted: $titleEn",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.voteOnConsortiumBoardProposal(projectId: String, proposalId: String, voteYes: Boolean) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"
    val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player"

    val targetProposal = project.boardProposals.find { it.id == proposalId } ?: return
    val updatedVotes = targetProposal.votes.toMutableMap()
    updatedVotes[pId] = voteYes

    val yesCount = updatedVotes.values.count { it }
    val requiredVotes = ((project.participatingPartnerIds.size / 2) + 1).coerceAtLeast(1)

    var enactedProject = project
    var isProposalEnacted = targetProposal.isEnacted

    // If yes votes reach majority or leader approves
    if (!isProposalEnacted && (yesCount >= requiredVotes || (isLeader && voteYes))) {
        isProposalEnacted = true
        when (targetProposal.proposalType) {
            com.example.data.ConsortiumProposalType.SALES_CHANNEL -> {
                val newChannel = try { com.example.data.ConsortiumSalesChannel.valueOf(targetProposal.proposedValue) } catch (_: Exception) { null }
                if (newChannel != null) {
                    enactedProject = enactedProject.copy(salesChannel = newChannel)
                }
            }
            com.example.data.ConsortiumProposalType.PRODUCTION_STRATEGY -> {
                val newStrategy = com.example.data.ConsortiumProductionStrategy.fromString(targetProposal.proposedValue)
                enactedProject = enactedProject.copy(productionStrategy = newStrategy)
            }
            com.example.data.ConsortiumProposalType.DIVIDEND_REINVESTMENT -> {
                // Strategy logged
            }
        }
    }

    val updatedProposal = targetProposal.copy(votes = updatedVotes, isEnacted = isProposalEnacted)
    val updatedProposals = enactedProject.boardProposals.map { if (it.id == proposalId) updatedProposal else it }
    val finalProject = enactedProject.copy(boardProposals = updatedProposals)

    _megaProjects.update { list -> list.map { if (it.id == projectId) finalProject else it } }
    syncMegaProject(finalProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        val voteMsg = if (voteYes) "Kabul (EVET)" else "Red (HAYIR)"
        val statusMsg = if (isProposalEnacted) " • 📜 TASARI ONAYLANDI & YÜRÜRLÜĞE GİRDİ!" else ""
        SmartNotificationManager.show(
            "Oyunuz Kaydedildi: $voteMsg$statusMsg",
            "Vote Recorded: $voteMsg$statusMsg",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.assignConsortiumRole(
    projectId: String,
    targetPlayerId: String,
    targetPlayerName: String,
    role: com.example.data.ConsortiumRole
) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"
    val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player"
    if (!isLeader) {
        SmartNotificationManager.show(
            "Yalnızca Konsorsiyum Başkanı rol ataması yapabilir!",
            "Only the Consortium President can assign roles!",
            NotificationType.ALERT
        )
        return
    }

    val updatedProject = when (role) {
        com.example.data.ConsortiumRole.CHIEF_ENGINEER -> project.copy(chiefEngineerId = targetPlayerId, chiefEngineerName = targetPlayerName)
        com.example.data.ConsortiumRole.LOGISTICS_CHIEF -> project.copy(logisticsChiefId = targetPlayerId, logisticsChiefName = targetPlayerName)
        else -> project
    }

    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        val roleBadge = role.getBadge()
        SmartNotificationManager.show(
            "$targetPlayerName, $roleBadge olarak atandı! 🎖️",
            "$targetPlayerName appointed as $roleBadge! 🎖️",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.electConsortiumRoles(projectId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val assignedSlots = project.slots.filter { it.assignedPartnerId != null }
    if (assignedSlots.isEmpty()) {
        SmartNotificationManager.show(
            "Rol seçimi için en az bir aktif tedarikçi ortak gereklidir.",
            "At least one active supplier partner is required for role election.",
            NotificationType.INFO
        )
        return
    }

    // Baş Mühendis: En yüksek teslimat değerine veya teknik slotuna sahip ortak
    val chiefCandidateSlot = assignedSlots.maxByOrNull { it.costContributionValue }
    val logisticsCandidateSlot = assignedSlots.filter { it.assignedPartnerId != chiefCandidateSlot?.assignedPartnerId }.firstOrNull() ?: chiefCandidateSlot

    val updatedProject = project.copy(
        chiefEngineerId = chiefCandidateSlot?.assignedPartnerId,
        chiefEngineerName = chiefCandidateSlot?.assignedPartnerName,
        logisticsChiefId = logisticsCandidateSlot?.assignedPartnerId,
        logisticsChiefName = logisticsCandidateSlot?.assignedPartnerName
    )

    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)
    viewModelScope.launch {
        saveEconomicDataToDataStore(immediate = true)
        SmartNotificationManager.show(
            "Konsorsiyum Rolleri Seçildi: ⚙️ ${updatedProject.chiefEngineerName} (Baş Mühendis), 🚚 ${updatedProject.logisticsChiefName} (Lojistik)",
            "Consortium Roles Elected: ⚙️ ${updatedProject.chiefEngineerName} (Chief Eng), 🚚 ${updatedProject.logisticsChiefName} (Logistics)",
            NotificationType.SUCCESS
        )
    }
}

fun GameViewModel.nudgeConsortiumPartner(projectId: String, slotId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val slot = project.slots.find { it.slotId == slotId } ?: return
    val curPlayer = player.value
    val pId = curPlayer?.id?.ifBlank { "local_player" } ?: "local_player"
    val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player"
    if (!isLeader) {
        SmartNotificationManager.show(
            "Yalnızca Başkan tedarikçilere resmi hızlandırma uyarısı gönderebilir!",
            "Only the President can send formal speed warnings to suppliers!",
            NotificationType.ALERT
        )
        return
    }

    val now = System.currentTimeMillis()
    if (now - project.lastNudgeTimeMs < 30_000L) {
        SmartNotificationManager.show(
            "Lütfen bekleyin! Uyarılar 30 saniyede bir gönderilebilir.",
            "Please wait! Warnings can only be sent once every 30 seconds.",
            NotificationType.INFO
        )
        return
    }

    val updatedSlots = project.slots.map {
        if (it.slotId == slotId) it.copy(isBottleneckWarning = true) else it
    }
    val updatedProject = project.copy(slots = updatedSlots, lastNudgeTimeMs = now)
    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)

    val partnerName = slot.assignedPartnerName ?: slot.productName
    sendConsortiumChatMessage(
        projectId,
        "⚠️ BAŞKAN UYARISI: Sayın $partnerName, '${slot.productName}' parça teslimatını tamamlamalı! Darboğaz sürerse slot devredilebilir."
    )

    SmartNotificationManager.show(
        "⚡ $partnerName ortağına acil teslimat uyarısı iletildi!",
        "⚡ Urgent delivery warning dispatched to $partnerName!",
        NotificationType.ALERT
    )
}

fun GameViewModel.broadcastConsortiumRadioSos(projectId: String, slotId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val slot = project.slots.find { it.slotId == slotId } ?: return

    val now = System.currentTimeMillis()
    if (now - project.lastNudgeTimeMs < 15_000L) {
        SmartNotificationManager.show(
            "Telsiz frekansı meşgul! 15 saniye sonra tekrar deneyin.",
            "Radio frequency busy! Try again in 15 seconds.",
            NotificationType.INFO
        )
        return
    }

    val updatedSlots = project.slots.map {
        if (it.slotId == slotId) it.copy(isBottleneckWarning = true) else it
    }
    val updatedProject = project.copy(slots = updatedSlots, lastNudgeTimeMs = now)
    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }
    syncMegaProject(updatedProject, force = true)

    val remaining = slot.remainingQuantity
    sendConsortiumChatMessage(
        projectId,
        "📻 [TELSİZ ACİL ÇAĞRI] Konsorsiyum Acil Tedarik: ${slot.productName} için $remaining Ton malzeme aranıyor! Deponuzdan hemen teslim edebilirsiniz."
    )

    SmartNotificationManager.show(
        "📻 Acil Telsiz Çağrısı Gönderildi: '${slot.productName}' için $remaining Ton yardım istendi!",
        "📻 Radio SOS broadcasted: $remaining Tons requested for '${slot.productName}'!",
        NotificationType.ALERT
    )
}

fun GameViewModel.oneTapDeliverToConsortium(projectId: String, slotId: String) {
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val slot = project.slots.find { it.slotId == slotId } ?: return

    val allProductInv = inventory.value.filter { 
        com.example.data.ItemQuality.extractBaseProductId(it.itemId) == slot.productId && it.quantity > 0 
    }
    val eligibleItems = allProductInv.filter { project.qualityTier.isQualityAllowed(it.quality) }
    val eligibleStock = eligibleItems.sumOf { it.quantity }

    if (eligibleStock <= 0) {
        val totalIneligible = allProductInv.sumOf { it.quantity }
        val msg = if (totalIneligible > 0) {
            "Depondaki $totalIneligible Ton '${slot.productName}' bu projenin ${project.qualityTier.titleTr} (${project.qualityTier.allowedQualityRangeTextTr}) standardına uymuyor!"
        } else {
            "Şirket deponuzda '${slot.productName}' bulunmuyor!"
        }
        SmartNotificationManager.show(msg, NotificationType.ALERT)
        return
    }

    val needed = slot.remainingQuantity
    if (needed <= 0) {
        SmartNotificationManager.show(
            "Bu slotun ihtiyacı zaten tamamlanmış!",
            "This slot requirement is already completed!",
            NotificationType.INFO
        )
        return
    }

    val toTransfer = minOf(eligibleStock, needed)
    deliverMaterialsToConsortium(projectId, slotId, toTransfer)
}

fun GameViewModel.fulfillConsortiumExportTender(projectId: String, tenderId: String, quantity: Int) {
    if (quantity <= 0) return
    val project = _megaProjects.value.find { it.id == projectId } ?: return
    val tender = project.activeTenders.find { it.id == tenderId } ?: return

    if (tender.isCompleted) {
        SmartNotificationManager.show("Bu ihale zaten tamamlandı!", "This tender is already completed!", NotificationType.INFO)
        return
    }
    if (tender.isExpired) {
        SmartNotificationManager.show("Bu ihalenin süresi dolmuş!", "This tender has expired!", NotificationType.ALERT)
        return
    }

    val availableUnits = project.warehouseStock
    if (availableUnits <= 0) {
        SmartNotificationManager.show(
            "Konsorsiyum ambarında ihraç edilecek ürün yok! Önce seri üretim yapmalısınız.",
            "No finished products in consortium warehouse! Produce units first.",
            NotificationType.ALERT
        )
        return
    }

    val toDeliver = minOf(quantity, minOf(availableUnits, tender.remainingQuantity))
    if (toDeliver <= 0) return

    val newDelivered = tender.deliveredQuantity + toDeliver
    val isTenderDone = newDelivered >= tender.requiredQuantity
    val updatedTender = tender.copy(
        deliveredQuantity = newDelivered,
        isCompleted = isTenderDone,
        completedAtMs = if (isTenderDone) System.currentTimeMillis() else null
    )

    val newWarehouseStock = availableUnits - toDeliver
    val updatedTenders = project.activeTenders.map { if (it.id == tenderId) updatedTender else it }
    val newBadges = if (isTenderDone && !project.completedTenderBadges.contains(tender.badgeRewardTr)) {
        project.completedTenderBadges + tender.badgeRewardTr
    } else {
        project.completedTenderBadges
    }

    val updatedProject = project.copy(
        warehouseStock = newWarehouseStock,
        activeTenders = updatedTenders,
        completedTenderBadges = newBadges
    )

    _megaProjects.update { list -> list.map { if (it.id == projectId) updatedProject else it } }

    viewModelScope.launch {
        if (isTenderDone) {
            val curPlayer = player.value
            if (curPlayer != null) {
                val pId = curPlayer.id.ifBlank { "local_player" }
                val isLeader = project.leaderPlayerId == pId || project.leaderPlayerId == "local_player"
                val userSlot = project.slots.find { it.assignedPartnerId == pId }

                val founderCut = if (isLeader) (tender.cashReward * 0.10).toLong() else 0L
                val supplierPool = tender.cashReward * 0.90
                val userSharePercent = userSlot?.sharePercentage ?: if (isLeader) 100f else 0f
                val supplierCut = (supplierPool * (userSharePercent / 100.0)).toLong()

                val totalUserReward = founderCut + supplierCut
                if (totalUserReward > 0L) {
                    val updatedP = curPlayer.copy(
                        money = curPlayer.money + totalUserReward,
                        xp = curPlayer.xp + tender.reputationReward
                    )
                    repository.updatePlayer(updatedP)
                }

                logManagerAction(
                    "mgr_logistics",
                    "🌐 İhracat İhalesi Tamamlandı: ${tender.titleTr} (${tender.destinationCountry}) -> ₳${com.example.ui.components.formatCredit(totalUserReward)} İhracat Kâr Payı & +${tender.reputationReward} İtibar!",
                    totalUserReward
                )

                SmartNotificationManager.show(
                    "🎉 İHRACAT İHALESİ TAMAMLANDI! ${tender.destinationCountry} anlaşmasından ₳${com.example.ui.components.formatCredit(totalUserReward)} kazandınız! 🏅 ${tender.badgeRewardTr}",
                    "🎉 EXPORT TENDER COMPLETED! Earned ₳${com.example.ui.components.formatCredit(totalUserReward)} from ${tender.destinationCountry}! 🏅 ${tender.badgeRewardEn}",
                    NotificationType.SUCCESS
                )

                sendConsortiumChatMessage(
                    projectId,
                    "🎉 MÜJDE: ${tender.destinationCountry} ihracat ihalesi (${tender.requiredQuantity} Adet) başarıyla teslim edildi! Konsorsiyuma ₳${com.example.ui.components.formatCredit(tender.cashReward)} ihracat geliri aktarıldı."
                )
            }
        } else {
            SmartNotificationManager.show(
                "🚢 $toDeliver Adet ${project.targetProductName} İhracat Sevkiyatına Yüklendi! (${newDelivered}/${tender.requiredQuantity})",
                "🚢 Loaded $toDeliver units of ${project.targetProductName} for export! (${newDelivered}/${tender.requiredQuantity})",
                NotificationType.INFO
            )
        }

        syncMegaProject(updatedProject, force = true)
        saveEconomicDataToDataStore(immediate = true)
    }
}

fun GameViewModel.startConsortiumSupplyLoop() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        while (true) {
            kotlinx.coroutines.delay(5000L)
            try {
                val currentProjects = _megaProjects.value
                if (currentProjects.isEmpty()) continue
                
                val curPlayer = player.value
                val pId = curPlayer?.id ?: "local_player"
                val pName = curPlayer?.name ?: ""
                val cleanEmail = _onlineEmail.value.replace(".", "_")
                val emailPrefix = _onlineEmail.value.substringBefore("@")
                
                fun isUserSlot(slot: com.example.data.ConsortiumSupplierSlot): Boolean {
                    if (slot.assignedPartnerId == "local_player" || slot.assignedPartnerId == pId) return true
                    if (pName.isNotBlank() && slot.assignedPartnerName == pName) return true
                    if (cleanEmail.isNotBlank() && slot.assignedPartnerId?.contains(cleanEmail) == true) return true
                    if (emailPrefix.isNotBlank() && slot.assignedPartnerId?.contains(emailPrefix) == true) return true
                    return false
                }
                
                fun isUserConsortium(proj: com.example.data.MegaProject): Boolean {
                    if (proj.leaderPlayerId == "local_player" || proj.leaderPlayerId == pId) return true
                    if (pName.isNotBlank() && proj.leaderPlayerName == pName) return true
                    if (cleanEmail.isNotBlank() && proj.leaderPlayerId.contains(cleanEmail)) return true
                    if (emailPrefix.isNotBlank() && proj.leaderPlayerId.contains(emailPrefix)) return true
                    if (proj.participatingPartnerIds.contains("local_player") || proj.participatingPartnerIds.contains(pId)) return true
                    if (proj.slots.any { isUserSlot(it) }) return true
                    return false
                }

                val updatedList = currentProjects.map { project ->
                    var proj = project
                    val isUserProj = isUserConsortium(proj)
                    val isBotFounder = !isUserProj && proj.leaderPlayerId != "local_player" && proj.leaderPlayerId != pId

                    // 1. Stage Advancement (Stages 1 -> 2 -> 3 -> 4)
                    if (proj.isCurrentStageFinished && proj.currentStage != com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION && proj.currentStage != com.example.data.MegaProjectStage.COMPLETED) {
                        val nextStage = when (proj.currentStage) {
                            com.example.data.MegaProjectStage.STAGE_1_BODY -> com.example.data.MegaProjectStage.STAGE_2_HARDWARE
                            com.example.data.MegaProjectStage.STAGE_2_HARDWARE -> com.example.data.MegaProjectStage.STAGE_3_TESTING
                            com.example.data.MegaProjectStage.STAGE_3_TESTING -> com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION
                            else -> com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION
                        }
                        proj = proj.copy(currentStage = nextStage)
                    }

                    // 2. Mass Production Transition & Batch Automation
                    val allSlotsDelivered = proj.slots.isNotEmpty() && proj.slots.all { it.isFullyDelivered }
                    if (allSlotsDelivered) {
                        if (!proj.isMassProductionApproved || proj.currentStage != com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION) {
                            proj = proj.copy(
                                isMassProductionApproved = true,
                                massProductionApprovedAtMs = proj.massProductionApprovedAtMs ?: System.currentTimeMillis(),
                                currentStage = com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION
                            )
                        }

                        // Auto-consume delivered materials and start 1-batch production
                        if (!proj.isProductionPaused && !proj.isBatchInProduction && proj.warehouseStock < proj.warehouseCapacity) {
                            val consumedSlots = proj.slots.map { it.copy(quantityDelivered = 0) }
                            proj = proj.copy(
                                isBatchInProduction = true,
                                batchProductionStartTimeMs = System.currentTimeMillis(),
                                slots = consumedSlots
                            )
                        }
                    }

                    // 3. Mass Production Batch Progression & Automated Sales / Dividends
                    if (proj.isBatchInProduction && !proj.isProductionPaused) {
                        val isDone = proj.isProductionTimeCompleted || (proj.remainingProductionTimeMs <= 0L && proj.batchProductionStartTimeMs > 0L)
                        if (isDone) {
                            val newStock = proj.warehouseStock + 1
                            val totalProd = proj.totalItemsProduced + 1
                            proj = proj.copy(
                                warehouseStock = newStock,
                                totalItemsProduced = totalProd,
                                isBatchInProduction = false,
                                isTestProductProduced = true,
                                testProducedAtMs = System.currentTimeMillis(),
                                lastBatchStartTimeMs = System.currentTimeMillis()
                            )

                            // Auto sell if bot consortium or autoSellActive to give dividends to players
                            val unitPrice = proj.unitBatchPrice
                            val userSlot = proj.slots.find { isUserSlot(it) }
                            
                            val userSharePercent = if (userSlot != null) {
                                userSlot.sharePercentage
                            } else {
                                0f
                            }

                            if ((isBotFounder || proj.isAutoSellActive) && userSharePercent > 0f && curPlayer != null) {
                                val userEarning = (unitPrice * (userSharePercent / 100.0)).toLong()
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    val updatedPlayer = curPlayer.copy(money = curPlayer.money + userEarning)
                                    repository.updatePlayer(updatedPlayer)
                                    logManagerAction(
                                        "mgr_logistics",
                                        "${proj.consortiumName} (Ortak): 1 Adet ${proj.targetProductName} üretildi & satıldı (+₳${com.example.ui.components.formatCredit(userEarning)} kâr payı)",
                                        userEarning
                                    )
                                }
                                proj = proj.copy(warehouseStock = 0) // Auto-sold
                            }

                            // If slots are already filled by players while this batch was running, immediately trigger next batch
                            if (proj.slots.isNotEmpty() && proj.slots.all { it.isFullyDelivered } && proj.warehouseStock < proj.warehouseCapacity) {
                                val consumedSlots = proj.slots.map { it.copy(quantityDelivered = 0) }
                                proj = proj.copy(
                                    isBatchInProduction = true,
                                    batchProductionStartTimeMs = System.currentTimeMillis(),
                                    slots = consumedSlots
                                )
                            }
                        }
                    }

                    // 4. Aşama IV: Uluslararası İhracat İhalelerinin Otomatik Üretimi
                    if (proj.currentStage == com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION || proj.currentStage == com.example.data.MegaProjectStage.COMPLETED) {
                        if (proj.activeTenders.isEmpty() || proj.activeTenders.all { it.isCompleted || it.isExpired }) {
                            val generatedTenders = com.example.data.ConsortiumExportTenderFactory.generateTendersForProject(proj)
                            proj = proj.copy(activeTenders = generatedTenders)
                        }
                    }

                    // 5. Konsorsiyum Baş Mühendis & Lojistik Sorumlusu Otomatik Atama (Boş ise)
                    if (proj.chiefEngineerId == null || proj.logisticsChiefId == null) {
                        val activePartners = proj.slots.filter { it.assignedPartnerId != null && it.assignedPartnerId != proj.leaderPlayerId }
                        if (activePartners.isNotEmpty()) {
                            val chiefSlot = activePartners.maxByOrNull { it.costContributionValue }
                            val logSlot = activePartners.filter { it.assignedPartnerId != chiefSlot?.assignedPartnerId }.firstOrNull() ?: chiefSlot
                            proj = proj.copy(
                                chiefEngineerId = proj.chiefEngineerId ?: chiefSlot?.assignedPartnerId,
                                chiefEngineerName = proj.chiefEngineerName ?: chiefSlot?.assignedPartnerName,
                                logisticsChiefId = proj.logisticsChiefId ?: logSlot?.assignedPartnerId,
                                logisticsChiefName = proj.logisticsChiefName ?: logSlot?.assignedPartnerName
                            )
                        }
                    }

                    proj
                }
                
                _megaProjects.value = updatedList
                
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Consortium bot stage automation error", e)
            }
        }
    }
}

// --- CLOUD, AUTH & GENERAL EXTENSIONS ---

fun GameViewModel.updateCompanyName(newName: String) {
    val p = player.value ?: return
    viewModelScope.launch {
        repository.updatePlayer(p.copy(name = newName))
        SmartNotificationManager.show("Şirket İsmi Güncellendi!", "Company Name Updated!", NotificationType.SUCCESS)
    }
}

fun GameViewModel.updateTraderNameWithDiamond(newName: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }): Boolean {
    updateCompanyName(newName)
    onResult(true, "Başarılı")
    return true
}

fun GameViewModel.signInAnonymously(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    onResult(true, "Anonim giriş yapıldı.")
}
fun GameViewModel.signInAnonymously(onSuccess: () -> Unit, onError: (String) -> Unit) {
    signInAnonymously { success, msg -> if (success) onSuccess() else onError(msg ?: "") }
}

fun GameViewModel.signInWithGoogle(idToken: String = "", onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    onResult(true, "Google ile giriş yapıldı.")
}
fun GameViewModel.signInWithGoogle(onSuccess: () -> Unit, onError: (String) -> Unit) {
    signInWithGoogle { success, msg -> if (success) onSuccess() else onError(msg ?: "") }
}

fun GameViewModel.signInWithGoogleAccount(
    email: String,
    displayName: String = "Tüccar",
    idToken: String? = null,
    onResult: (Boolean, String?) -> Unit = { _, _ -> }
) {
    viewModelScope.launch {
        try {
            updateCompanyName(displayName)
            val cleanEmail = email.trim()
            val resolvedPlayerId = cleanEmail.replace(".", "_")

            // 1. DO NOT set _isOnlineRegistered.value = true yet!
            // First fetch the cloud save from Supabase with retries
            var cloudSaveJson: String? = null
            var attempts = 0
            while (cloudSaveJson.isNullOrBlank() && attempts < 3) {
                attempts++
                cloudSaveJson = com.example.data.SupabaseManager.fetchPlayerSaveData(cleanEmail)
                    ?: com.example.data.SupabaseManager.fetchPlayerSaveData(resolvedPlayerId)
                if (cloudSaveJson.isNullOrBlank() && attempts < 3) {
                    kotlinx.coroutines.delay(1200L)
                }
            }

            if (!cloudSaveJson.isNullOrBlank()) {
                val importSuccess = repository.economicDataStore?.importSaveJson(cloudSaveJson, force = true) ?: false
                val snapshot = repository.initializeGame(resolvedPlayerId)
                if (snapshot != null) {
                    applySnapshotToState(snapshot)
                }
                repository.saveOnlineAuth(cleanEmail, "", true)
                _onlineEmail.value = cleanEmail
                _isOnlineRegistered.value = true
                onResult(true, "Supabase bulut yedeğiniz başarıyla yüklendi!")
            } else {
                // Cloud save was not found on remote.
                // CRITICAL SAFETY: Check if local player has real progress.
                val curPlayer = player.value
                val hasLocalProgress = (curPlayer != null && (curPlayer.level > 1 || curPlayer.money > 250_000L || curPlayer.gems > 0 || curPlayer.totalProfit > 0L))
                
                repository.saveOnlineAuth(cleanEmail, "", true)
                _onlineEmail.value = cleanEmail
                _isOnlineRegistered.value = true

                if (hasLocalProgress) {
                    syncCloudSaveToSupabase(force = true, immediate = true)
                    val snapshot = repository.initializeGame(resolvedPlayerId)
                    if (snapshot != null) {
                        applySnapshotToState(snapshot)
                    }
                    onResult(true, "Yerel ilerlemeniz bulut hesabınıza yedeklendi!")
                } else {
                    val snapshot = repository.initializeGame(resolvedPlayerId)
                    if (snapshot != null) {
                        applySnapshotToState(snapshot)
                    }
                    onResult(true, "Google hesabınız başarıyla bağlandı!")
                }
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error in signInWithGoogleAccount", e)
            onResult(false, "Profil senkronizasyon hatası: ${e.localizedMessage}")
        }
    }
}

fun GameViewModel.loginOnline(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
        try {
            val res = com.example.data.SupabaseManager.signInWithEmail(email, pass)
            if (res.first) {
                val cleanEmail = email.trim()
                val resolvedPlayerId = cleanEmail.replace(".", "_")

                var cloudSaveJson: String? = null
                var attempts = 0
                while (cloudSaveJson.isNullOrBlank() && attempts < 3) {
                    attempts++
                    cloudSaveJson = com.example.data.SupabaseManager.fetchPlayerSaveData(cleanEmail)
                        ?: com.example.data.SupabaseManager.fetchPlayerSaveData(resolvedPlayerId)
                    if (cloudSaveJson.isNullOrBlank() && attempts < 3) {
                        kotlinx.coroutines.delay(1200L)
                    }
                }

                if (!cloudSaveJson.isNullOrBlank()) {
                    val importSuccess = repository.economicDataStore?.importSaveJson(cloudSaveJson, force = true) ?: false
                    val snapshot = repository.initializeGame(resolvedPlayerId)
                    if (snapshot != null) {
                        applySnapshotToState(snapshot)
                    }
                    repository.saveOnlineAuth(cleanEmail, pass, true)
                    _onlineEmail.value = cleanEmail
                    _isOnlineRegistered.value = true
                    onResult(true, "Çevrimiçi profil başarıyla yüklendi!")
                } else {
                    val curPlayer = player.value
                    val hasLocalProgress = (curPlayer != null && (curPlayer.level > 1 || curPlayer.money > 250_000L || curPlayer.gems > 0 || curPlayer.totalProfit > 0L))

                    repository.saveOnlineAuth(cleanEmail, pass, true)
                    _onlineEmail.value = cleanEmail
                    _isOnlineRegistered.value = true

                    if (hasLocalProgress) {
                        syncCloudSaveToSupabase(force = true, immediate = true)
                        val snapshot = repository.initializeGame(resolvedPlayerId)
                        if (snapshot != null) {
                            applySnapshotToState(snapshot)
                        }
                        onResult(true, "Giriş başarılı, ilerlemeniz buluta yedeklendi.")
                    } else {
                        val snapshot = repository.initializeGame(resolvedPlayerId)
                        if (snapshot != null) {
                            applySnapshotToState(snapshot)
                        }
                        onResult(true, "Giriş başarılı.")
                    }
                }
            } else {
                onResult(false, res.second ?: "Giriş başarısız.")
            }
        } catch (e: Exception) {
            onResult(false, "Hata: ${e.localizedMessage}")
        }
    }
}

fun GameViewModel.registerOnline(email: String, pass: String, name: String = "Tüccar", onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
        try {
            val res = com.example.data.SupabaseManager.signUpWithEmail(email, pass)
            if (res.first) {
                repository.saveOnlineAuth(email, pass, true)
                _isOnlineRegistered.value = true
                _onlineEmail.value = email
                syncCloudSaveToSupabase(force = true, immediate = true)
                onResult(true, "Kayıt ve senkronizasyon başarılı!")
            } else {
                onResult(false, res.second ?: "Kayıt başarısız.")
            }
        } catch (e: Exception) {
            onResult(false, "Hata: ${e.localizedMessage}")
        }
    }
}

fun GameViewModel.logoutOnline() {
    viewModelScope.launch {
        try {
            repository.saveOnlineAuth("", "", false)
            _isOnlineRegistered.value = false
            _onlineEmail.value = ""
            val snapshot = repository.initializeGame("local_player")
            if (snapshot != null) {
                applySnapshotToState(snapshot)
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error in logoutOnline", e)
        }
    }
}

fun GameViewModel.claimRewardedAdGems(amount: Int = 10) {
    val p = player.value ?: return
    val newGems = p.gems + amount
    val updatedPlayer = p.copy(gems = newGems)

    viewModelScope.launch {
        try {
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value && _onlineEmail.value.isNotBlank()) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error in claimRewardedAdGems", e)
        }
    }

    SmartNotificationManager.show(
        "🎬 +$amount Elmas ödülü hesabınıza eklendi!",
        "🎬 +$amount Gems reward added to your account!",
        NotificationType.SUCCESS
    )
}

fun GameViewModel.recordGrowthPointIfNeeded(isNewDay: Boolean = false) {
    val p = player.value ?: return
    val nw = uiState.value.netWorth.toDouble().coerceAtLeast(0.0)
    val cash = p.money.toDouble().coerceAtLeast(0.0)
    val facVal = uiState.value.facilityValuation.toDouble().coerceAtLeast(0.0)
    val invVal = uiState.value.inventoryValuation.toDouble().coerceAtLeast(0.0)
    val conVal = uiState.value.consortiumValuation.toDouble().coerceAtLeast(0.0)
    val depVal = (p.depositBalance + p.lockedDepositBalance).toDouble().coerceAtLeast(0.0)
    val assets = (cash + depVal + facVal + invVal + conVal).coerceAtLeast(0.0)

    val currentHistory = _growthHistory.value.toMutableList()

    if (currentHistory.isEmpty()) {
        val initialPoint = com.example.data.GrowthPointDto(
            timestampMs = System.currentTimeMillis(),
            dayLabel = "1. Gün",
            netWorth = nw,
            cashBalance = cash,
            totalAssets = assets,
            depositBalance = depVal,
            facilityValuation = facVal,
            inventoryValuation = invVal,
            consortiumValuation = conVal
        )
        _growthHistory.value = listOf(initialPoint)
    } else {
        if (isNewDay) {
            val dayIndex = currentHistory.size + 1
            val newPoint = com.example.data.GrowthPointDto(
                timestampMs = System.currentTimeMillis(),
                dayLabel = "$dayIndex. Gün",
                netWorth = nw,
                cashBalance = cash,
                totalAssets = assets,
                depositBalance = depVal,
                facilityValuation = facVal,
                inventoryValuation = invVal,
                consortiumValuation = conVal
            )
            currentHistory.add(newPoint)
            if (currentHistory.size > 90) {
                currentHistory.removeAt(0)
            }
            _growthHistory.value = currentHistory
        } else {
            val lastIdx = currentHistory.size - 1
            val lastPoint = currentHistory[lastIdx]
            currentHistory[lastIdx] = lastPoint.copy(
                netWorth = nw,
                cashBalance = cash,
                totalAssets = assets,
                depositBalance = depVal,
                facilityValuation = facVal,
                inventoryValuation = invVal,
                consortiumValuation = conVal
            )
            _growthHistory.value = currentHistory
        }
    }
}

suspend fun GameViewModel.saveEconomicDataToDataStoreSuspend(
    customPlayer: PlayerEntity? = null,
    customBusinesses: List<BusinessEntity>? = null
) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val p = customPlayer ?: player.value ?: return@withContext
        recordGrowthPointIfNeeded(isNewDay = false)
        val state = gameState.value ?: com.example.data.GameStateEntity()

        // Ensure research state flows and research levels map are synchronized
        syncResearchStateFlowsAndMap()

        val currentBusinesses = customBusinesses ?: try {
            val direct = repository.getAllBusinessesDirect()
            if (direct.isNotEmpty()) direct else businesses.value
        } catch (_: Exception) {
            businesses.value
        }

        repository.saveEconomicStateToDataStore(
            player = p,
            gameState = state,
            businesses = currentBusinesses,
            inventory = inventory.value,
            marketPrices = marketPrices.value,
            marketListings = uiState.value.marketState.marketListings,
            isAutoSell = isAutoSellActive.value,
            isAutoBuy = isAutoBuyActive.value,
            isAutoProduce = isAutoProduceActive.value,
            isProdManagerHired = isProdManagerHired.value,
            isEngineerHired = isEngineerHired.value,
            isEngineerActive = isEngineerActive.value,
            isSalesExecHired = isSalesExecHired.value,
            isSalesExecActive = isSalesExecActive.value,
            techGreenEnergy = techGreenEnergy.value,
            techQualityControl = techQualityControl.value,
            techLogistics = techLogistics.value,
            techAutomation = techAutomation.value,
            techQuantumAi = techQuantumAi.value,
            techNanotech = techNanotech.value,
            techCyberSecurity = techCyberSecurity.value,
            techBiotechCloning = techBiotechCloning.value,
            techAerospace = techAerospace.value,
            techHeavyIndustry = techHeavyIndustry.value,
            techConsumerGoods = techConsumerGoods.value,
            techPetrochem = techPetrochem.value,
            activeResearchTechKey = activeResearchTechKey.value ?: "",
            researchEndTimeMs = researchEndTimeMs.value,
            activeResearches = _activeResearches.value,
            researchLevels = _researchLevels.value,
            isIpoActive = _isIpoActive.value,
            publicSharePercent = _publicSharePercent.value,
            totalDividendsPaid = _totalDividendsPaid.value,
            playerGuildShares = _playerGuildShares.value,
            playerGuildBuyPrices = _playerGuildBuyPrices.value,
            managers = managers.value,
            auctions = _auctions.value,
            megaProjects = _megaProjects.value,
            deliveries = _activeDeliveries.value,
            activeProductions = _activeProductions.value,
            growthHistoryJson = com.example.data.network.AppJson.encodeToString<List<com.example.data.GrowthPointDto>>(_growthHistory.value),
            dailyQuestStateJson = com.example.data.network.AppJson.encodeToString<com.example.data.quest.DailyQuestState>(_dailyQuestState.value)
        )
    } catch (e: Exception) {
        android.util.Log.e("GameViewModel", "Error in saveEconomicDataToDataStoreSuspend", e)
    }
}

private var lastSupabaseSyncTimeMs = 0L

fun GameViewModel.saveEconomicDataToDataStore(
    showNotification: Boolean = false,
    force: Boolean = false,
    immediate: Boolean = false,
    customPlayer: PlayerEntity? = null,
    customBusinesses: List<BusinessEntity>? = null
) {
    viewModelScope.launch {
        saveEconomicDataToDataStoreSuspend(customPlayer = customPlayer, customBusinesses = customBusinesses)
        if (showNotification) {
            com.example.ui.components.SmartNotificationManager.show("Oyun kaydedildi.", com.example.ui.components.NotificationType.INFO)
        }
        val now = System.currentTimeMillis()
        if (force) {
            lastSupabaseSyncTimeMs = now
            if (_isOnlineRegistered.value && _onlineEmail.value.isNotBlank() && _onlineEmail.value != "misafir_tuccar") {
                syncCloudSaveToSupabase(force = true, immediate = immediate, customPlayer = customPlayer, customBusinesses = customBusinesses, isManual = false)
            }
        }
    }
}

fun GameViewModel.syncCloudSaveToSupabase(
    force: Boolean = false,
    immediate: Boolean = false,
    customPlayer: PlayerEntity? = null,
    customBusinesses: List<BusinessEntity>? = null,
    isManual: Boolean = false
) {
    if (!isManual && !force) {
        val now = System.currentTimeMillis()
        if (now - lastSupabaseSyncTimeMs < 10_000L) {
            return
        }
        lastSupabaseSyncTimeMs = now
    }
    appScope.launch {
        try {
            var email = _onlineEmail.value
            if (email.isBlank() || email == "misafir_tuccar" || email.startsWith("guest")) {
                val stored = repository.economicDataStore?.getEconomicSnapshot()
                val candidate = stored?.onlineEmail.orEmpty().trim()
                if (candidate.isNotBlank() && candidate != "misafir_tuccar" && !candidate.startsWith("guest")) {
                    email = candidate
                    _onlineEmail.value = candidate
                    _isOnlineRegistered.value = true
                }
            }
            if (email.isBlank() || email == "misafir_tuccar" || email.startsWith("guest")) {
                if (isManual) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        com.example.ui.components.SmartNotificationManager.show(
                            "⚠️ Bulut yedeklemesi için lütfen Profil menüsünden Google Hesabınızla giriş yapın.",
                            "⚠️ Please sign in with your Google Account in the Profile menu for cloud backup.",
                            com.example.ui.components.NotificationType.ALERT
                        )
                    }
                }
                return@launch
            }
            val p = customPlayer ?: player.value ?: return@launch
            
            // Safeguard: Never automatically upload fresh empty beginner state in background
            val localBizCount = (customBusinesses ?: businesses.value).size
            if (!isManual && p.level <= 1 && p.money <= 200_000L && p.gems == 0 && localBizCount == 0 && p.totalProfit <= 0L) {
                Log.d("GameViewModel", "Skipping automatic cloud sync: player state is at initial start (100k AL, 0 biz). Cloud remains protected.")
                return@launch
            }

            // Move heavy IO and Serialization work to Dispatchers.IO to prevent UI freezing
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Ensure research state flows and research levels map are synchronized
                syncResearchStateFlowsAndMap()

                // First save synchronously to local DataStore so exportSaveJson() reads the exact current state!
                saveEconomicDataToDataStoreSuspend(customPlayer = p, customBusinesses = customBusinesses)

                val currentBusinesses = customBusinesses ?: try {
                    val direct = repository.getAllBusinessesDirect()
                    if (direct.isNotEmpty()) direct else businesses.value
                } catch (_: Exception) {
                    businesses.value
                }

                val uid = email.replace(".", "_")
                val rawSave = repository.exportSaveJson()

                val stateVal = uiState.value
                val activeDeliveriesStr = com.example.data.network.AppJson.encodeToString<List<com.example.data.DeliveryItem>>(_activeDeliveries.value)
                val activeProductionsStr = com.example.data.network.AppJson.encodeToString<List<com.example.data.ActiveProduction>>(_activeProductions.value)
                val currentManagers = _managers.value
                val managersStr = repository.economicDataStore?.serializeManagers(currentManagers)
                    ?: com.example.data.network.AppJson.encodeToString<List<com.example.data.CompanyManager>>(currentManagers)
                val dailyQuestStateStr = com.example.data.network.AppJson.encodeToString<com.example.data.quest.DailyQuestState>(_dailyQuestState.value)
                syncResearchStateFlowsAndMap()
                val activeResearchesStr = com.example.data.network.AppJson.encodeToString<Map<String, Long>>(_activeResearches.value)
                val researchLevelsStr = com.example.data.network.AppJson.encodeToString<Map<String, Int>>(_researchLevels.value)
                val guildSharesStr = com.example.data.network.AppJson.encodeToString<Map<String, Int>>(_playerGuildShares.value)
                val guildBuyPricesStr = com.example.data.network.AppJson.encodeToString<Map<String, Double>>(_playerGuildBuyPrices.value)

                var success = false
                var attempts = 0
                while (!success && attempts < 3) {
                    success = com.example.data.SupabaseManager.syncPlayerToSupabase(
                        uid = uid,
                        name = p.name,
                        companyName = p.name,
                        money = p.money,
                        loanAmount = p.loanAmount,
                        depositBalance = p.depositBalance,
                        dailyIncome = p.dailyIncome,
                        dailyExpense = p.dailyExpense,
                        totalProfit = p.totalProfit,
                        xp = p.xp,
                        level = p.level,
                        inventoryCapacity = p.inventoryCapacity,
                        currentCity = p.currentCity,
                        isVip = p.isVip,
                        gems = p.gems,
                        lastDailyRewardMs = p.lastDailyRewardMs,
                        loginStreak = p.loginStreak,
                        dollarBalance = p.dollarBalance,
                        dollarDepositBalance = p.dollarDepositBalance,
                        dollarLoanAmount = p.dollarLoanAmount,
                        isOnlineRegistered = true,
                        onlineEmail = email,
                        businesses = currentBusinesses,
                        inventory = inventory.value,
                        activeDeliveriesJson = activeDeliveriesStr,
                        activeProductionsJson = activeProductionsStr,
                        managersJson = managersStr,
                        dailyQuestStateJson = dailyQuestStateStr,
                        activeResearchesJson = activeResearchesStr,
                        researchLevelsJson = researchLevelsStr,
                        guildSharesJson = guildSharesStr,
                        guildBuyPricesJson = guildBuyPricesStr,
                        rawSaveJson = rawSave
                    )
                    if (!success) {
                        attempts++
                        if (attempts < 3) {
                            Log.w("GameViewModel", "Supabase sync attempt $attempts failed. Retrying in 2 seconds...")
                            kotlinx.coroutines.delay(2000L)
                        }
                    }
                }
                
                // Backup heavy save JSON to Google Drive AppData Space to protect Supabase quotas
                val rawJsonToUpload = rawSave
                if (!rawJsonToUpload.isNullOrBlank()) {
                    try {
                        val token = com.example.data.GoogleDriveSaveManager.getAccessToken()
                            ?: repository.economicDataStore?.context?.let { com.example.data.GoogleDriveSaveManager.resolveAccessToken(it) }
                        if (!token.isNullOrBlank()) {
                            com.example.data.GoogleDriveSaveManager.uploadSaveJson(rawJsonToUpload, token)
                        }
                    } catch (e: Exception) {
                        Log.w("GameViewModel", "Failed to upload save JSON to Google Drive AppData", e)
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (success) {
                        _lastCloudBackupTimeMs.value = System.currentTimeMillis()
                        val backupTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        _lastCloudBackupStatus.value = backupTimeStr
                        Log.i("GameViewModel", "Supabase sync succeeded after ${attempts + 1} attempts! ($backupTimeStr)")
                        if (isManual) {
                            com.example.ui.components.SmartNotificationManager.show(
                                "🌐 Şirket ilerlemeniz buluta başarıyla yedeklendi! ($backupTimeStr)",
                                "🌐 Your company progress has been successfully backed up to the cloud! ($backupTimeStr)",
                                com.example.ui.components.NotificationType.SUCCESS
                            )
                        }
                    } else {
                        _lastCloudBackupStatus.value = "Hata"
                        Log.e("GameViewModel", "Supabase sync failed completely after 3 attempts.")
                        if (isManual) {
                            com.example.ui.components.SmartNotificationManager.show(
                                "⚠️ Bulut yedeklemesi başarısız oldu. Lütfen internet bağlantınızı kontrol edin.",
                                "⚠️ Cloud backup failed. Please check your internet connection.",
                                com.example.ui.components.NotificationType.ALERT
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error in syncCloudSaveToSupabase", e)
        }
    }
}

fun GameViewModel.forceSyncCloudSaveToSupabase(context: Context? = null, immediate: Boolean = false) {
    syncCloudSaveToSupabase(force = true, immediate = immediate, isManual = true)
}

fun GameViewModel.forceRestoreFromCloud(context: Context? = null, immediate: Boolean = false) {
    viewModelScope.launch {
        try {
            var email = _onlineEmail.value
            if (email.isBlank() || email == "misafir_tuccar" || email.startsWith("guest")) {
                val stored = repository.economicDataStore?.getEconomicSnapshot()
                val candidate = stored?.onlineEmail.orEmpty().trim()
                if (candidate.isNotBlank() && candidate != "misafir_tuccar" && !candidate.startsWith("guest")) {
                    email = candidate
                    _onlineEmail.value = candidate
                    _isOnlineRegistered.value = true
                }
            }
            if (!_isOnlineRegistered.value || email.isBlank() || email == "misafir_tuccar" || email.startsWith("guest")) {
                com.example.ui.components.SmartNotificationManager.show(
                    "⚠️ Buluttan geri yüklemek için lütfen Profil menüsünden Google Hesabınızla giriş yapın.",
                    "⚠️ Please sign in with your Google Account in the Profile menu to restore from cloud.",
                    com.example.ui.components.NotificationType.ALERT
                )
                return@launch
            }
            val cleanEmail = email.trim()
            val resolvedId = cleanEmail.replace(".", "_")

            var cloudSaveJson: String? = null

            // 1. Önce Google Drive AppData Space kontrol et
            try {
                val token = com.example.data.GoogleDriveSaveManager.getAccessToken()
                    ?: repository.economicDataStore?.context?.let { com.example.data.GoogleDriveSaveManager.resolveAccessToken(it) }
                if (!token.isNullOrBlank()) {
                    cloudSaveJson = com.example.data.GoogleDriveSaveManager.downloadSaveJson(token)
                }
            } catch (e: Exception) {
                Log.w("GameViewModel", "Error downloading from Google Drive during forceRestore", e)
            }

            // 2. Drive'da yoksa Supabase'den çek
            if (cloudSaveJson.isNullOrBlank()) {
                var attempts = 0
                while (cloudSaveJson.isNullOrBlank() && attempts < 3) {
                    attempts++
                    cloudSaveJson = com.example.data.SupabaseManager.fetchPlayerSaveData(cleanEmail)
                        ?: com.example.data.SupabaseManager.fetchPlayerSaveData(resolvedId)
                    if (cloudSaveJson.isNullOrBlank() && attempts < 3) {
                        kotlinx.coroutines.delay(1000L)
                    }
                }
            }

            if (!cloudSaveJson.isNullOrBlank()) {
                val success = repository.economicDataStore?.importSaveJson(cloudSaveJson, force = true) ?: false
                if (success) {
                    val snapshot = repository.initializeGame(resolvedId)
                    if (snapshot != null) {
                        applySnapshotToState(snapshot)
                        val parsedLevel = snapshot.level
                        val parsedMoney = com.example.ui.components.formatMoney(snapshot.money)
                        val parsedGems = snapshot.gems
                        com.example.ui.components.SmartNotificationManager.show(
                            "🌐 En güncel bulut kaydınız başarıyla geri yüklendi! (Seviye $parsedLevel | 💎 $parsedGems | ₳$parsedMoney)",
                            "🌐 Your latest cloud save was restored! (Level $parsedLevel | 💎 $parsedGems | ₳$parsedMoney)",
                            com.example.ui.components.NotificationType.SUCCESS
                        )
                    } else {
                        com.example.ui.components.SmartNotificationManager.show(
                            "🌐 Bulut kaydı yüklendi!",
                            "🌐 Cloud save loaded!",
                            com.example.ui.components.NotificationType.SUCCESS
                        )
                    }
                } else {
                    com.example.ui.components.SmartNotificationManager.show(
                        "⚠️ Bulut kaydı geri yüklenirken yerel veriler uyumsuzluk yaşadı.",
                        "⚠️ Local data mismatch occurred while restoring cloud save.",
                        com.example.ui.components.NotificationType.ALERT
                    )
                }
            } else {
                com.example.ui.components.SmartNotificationManager.show(
                    "⚠️ Bulutta kayıtlı şirket ilerlemeniz bulunamadı.",
                    "⚠️ No company progress found in the cloud.",
                    com.example.ui.components.NotificationType.ALERT
                )
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error in forceRestoreFromCloud", e)
            com.example.ui.components.SmartNotificationManager.show(
                "⚠️ Geri yükleme hatası: ${e.localizedMessage}",
                "⚠️ Restore error: ${e.localizedMessage}",
                com.example.ui.components.NotificationType.ALERT
            )
        }
    }
}

fun GameViewModel.applySnapshotToState(snapshot: EconomicSnapshot) {
    _isOnlineRegistered.value = snapshot.isOnlineRegistered
    _onlineEmail.value = snapshot.onlineEmail
    _hasCompletedFirstTrade.value = snapshot.hasCompletedFirstTrade
    
    viewModelScope.launch {
        repository.economicDataStore?.let { store ->
            _isEntrepreneurGuideCompleted.value = store.getIsEntrepreneurGuideCompleted()
        }
    }
    
    _techGreenEnergy.value = maxOf(_techGreenEnergy.value, snapshot.techGreenEnergy)
    _techQualityControl.value = maxOf(_techQualityControl.value, snapshot.techQualityControl)
    _techLogistics.value = maxOf(_techLogistics.value, snapshot.techLogistics)
    _techAutomation.value = maxOf(_techAutomation.value, snapshot.techAutomation)
    _techQuantumAi.value = maxOf(_techQuantumAi.value, snapshot.techQuantumAi)
    _techNanotech.value = maxOf(_techNanotech.value, snapshot.techNanotech)
    _techCyberSecurity.value = maxOf(_techCyberSecurity.value, snapshot.techCyberSecurity)
    _techBiotechCloning.value = maxOf(_techBiotechCloning.value, snapshot.techBiotechCloning)
    _techAerospace.value = maxOf(_techAerospace.value, snapshot.techAerospace)
    _techHeavyIndustry.value = maxOf(_techHeavyIndustry.value, snapshot.techHeavyIndustry)
    _techConsumerGoods.value = maxOf(_techConsumerGoods.value, snapshot.techConsumerGoods)
    _techPetrochem.value = maxOf(_techPetrochem.value, snapshot.techPetrochem)
    _activeResearchTechKey.value = snapshot.activeResearchTechKey.ifBlank { null }
    _researchEndTimeMs.value = snapshot.researchEndTimeMs

    _isIpoActive.value = snapshot.isIpoActive
    _publicSharePercent.value = snapshot.publicSharePercent
    _totalDividendsPaid.value = snapshot.totalDividendsPaid

    _selectedTheme.value = snapshot.selectedTheme
    _selectedLanguage.value = snapshot.selectedLanguage

    val currentRLevels = _researchLevels.value.toMutableMap()

    // 1. Direct tech levels from snapshot fields
    val directSnapshotTechs = mapOf(
        "green_energy" to snapshot.techGreenEnergy,
        "quality_control" to snapshot.techQualityControl,
        "logistics" to snapshot.techLogistics,
        "automation" to snapshot.techAutomation,
        "quantum_ai" to snapshot.techQuantumAi,
        "nanotech" to snapshot.techNanotech,
        "cyber_security" to snapshot.techCyberSecurity,
        "biotech_cloning" to snapshot.techBiotechCloning,
        "biotech_med" to snapshot.techBiotechCloning,
        "aerospace" to snapshot.techAerospace,
        "heavy_industry" to snapshot.techHeavyIndustry,
        "consumer_goods" to snapshot.techConsumerGoods,
        "petrochem" to snapshot.techPetrochem
    )
    directSnapshotTechs.forEach { (tech, lvl) ->
        if (lvl > 0) {
            currentRLevels[tech] = maxOf(currentRLevels[tech] ?: 0, lvl).coerceAtMost(5)
            currentRLevels["tech_$tech"] = maxOf(currentRLevels["tech_$tech"] ?: 0, lvl).coerceAtMost(5)
        }
    }

    // 2. Decode researchLevelsJson and merge
    val parsedResearchLevels = try {
        com.example.data.network.AppJson.decodeFromString<Map<String, Int>>(snapshot.researchLevelsJson)
    } catch (e: Exception) {
        emptyMap()
    }
    parsedResearchLevels.forEach { (k, v) ->
        val base = k.removePrefix("tech_")
        if (v > 0) {
            currentRLevels[base] = maxOf(currentRLevels[base] ?: 0, v).coerceAtMost(5)
            currentRLevels["tech_$base"] = maxOf(currentRLevels["tech_$base"] ?: 0, v).coerceAtMost(5)
        }
    }

    // 3. Decode activeResearchesJson and handle ongoing vs offline completed
    val clean = mutableMapOf<String, Long>()
    val now = System.currentTimeMillis()
    val parsedActiveResearches = try {
        val raw = com.example.data.network.AppJson.decodeFromString<Map<String, Long>>(snapshot.activeResearchesJson)
        raw.forEach { (k, v) ->
            val base = k.removePrefix("tech_")
            if (v > now) {
                clean[base] = maxOf(clean[base] ?: 0L, v)
            } else if (v > 0L) {
                // Completed while game was closed / backgrounded -> advance tech level
                val curLvl = currentRLevels[base] ?: 0
                val newLvl = (curLvl + 1).coerceAtMost(5)
                currentRLevels[base] = newLvl
                currentRLevels["tech_$base"] = newLvl
            }
        }
        clean
    } catch (e: Exception) {
        emptyMap()
    }

    // 4. Legacy single active research key / timestamp handling
    if (snapshot.activeResearchTechKey.isNotBlank() && snapshot.researchEndTimeMs > 0L) {
        val baseKey = snapshot.activeResearchTechKey.removePrefix("tech_")
        if (snapshot.researchEndTimeMs > now) {
            clean[baseKey] = maxOf(clean[baseKey] ?: 0L, snapshot.researchEndTimeMs)
        } else {
            // Completed while offline
            val curLvl = currentRLevels[baseKey] ?: 0
            val newLvl = (curLvl + 1).coerceAtMost(5)
            currentRLevels[baseKey] = newLvl
            currentRLevels["tech_$baseKey"] = newLvl
        }
    }

    _researchLevels.value = currentRLevels
    _activeResearches.value = clean

    // Synchronize research state flows and research levels map
    syncResearchStateFlowsAndMap(currentRLevels)

    // Restore active research key and timer if active
    val firstActive = _activeResearches.value.entries.firstOrNull { it.value > System.currentTimeMillis() }
    if (firstActive != null) {
        val baseKey = firstActive.key.removePrefix("tech_")
        _activeResearchTechKey.value = baseKey
        _researchEndTimeMs.value = firstActive.value
        _researchRemainingMs.value = (firstActive.value - System.currentTimeMillis()).coerceAtLeast(0L)
    } else {
        _activeResearchTechKey.value = null
        _researchEndTimeMs.value = 0L
        _researchRemainingMs.value = 0L
    }

    _activeDeliveries.value = try {
        com.example.data.network.AppJson.decodeFromString<List<com.example.data.DeliveryItem>>(snapshot.activeDeliveriesJson)
    } catch (e: Exception) {
        emptyList()
    }

    val parsedMegaProjects = try {
        repository.economicDataStore?.deserializeMegaProjects(snapshot.megaProjectsJson) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    if (parsedMegaProjects.isNotEmpty()) {
        _megaProjects.value = parsedMegaProjects
    }

    _activeProductions.value = try {
        com.example.data.network.AppJson.decodeFromString<List<com.example.data.ActiveProduction>>(snapshot.activeProductionsJson)
    } catch (e: Exception) {
        emptyList()
    }

    val parsedGrowthHistory = try {
        com.example.data.network.AppJson.decodeFromString<List<com.example.data.GrowthPointDto>>(snapshot.growthHistoryJson)
    } catch (e: Exception) {
        emptyList()
    }
    _growthHistory.value = parsedGrowthHistory
    recordGrowthPointIfNeeded(isNewDay = false)

    val parsedManagers = try {
        repository.economicDataStore?.deserializeManagers(snapshot.managersJson)
            ?: com.example.data.network.AppJson.decodeFromString<List<com.example.data.CompanyManager>>(snapshot.managersJson)
    } catch (e: Exception) {
        emptyList()
    }
    val defaultList = com.example.data.getDefaultCompanyManagers()
    val baseManagers = if (parsedManagers.isEmpty()) {
        defaultList
    } else {
        parsedManagers
    }
    val currentLocal = _managers.value
    val mergedManagers = defaultList.map { def ->
        val remoteMgr = baseManagers.find { it.id == def.id }
        val localMgr = currentLocal.find { it.id == def.id }
        val hasHire = (remoteMgr?.isHired == true) || (localMgr?.isHired == true) || 
                      ((remoteMgr?.level ?: 1) > 1) || ((localMgr?.level ?: 1) > 1) ||
                      (remoteMgr?.actionLogs?.isNotEmpty() == true) || (localMgr?.actionLogs?.isNotEmpty() == true) ||
                      (remoteMgr?.name?.isNotBlank() == true && remoteMgr.name != def.name) ||
                      (localMgr?.name?.isNotBlank() == true && localMgr.name != def.name)
        val isHired = hasHire
        val level = maxOf(remoteMgr?.level ?: 1, localMgr?.level ?: 1).coerceIn(1, 5)
        val eff = maxOf(remoteMgr?.efficiency ?: 1.0f, localMgr?.efficiency ?: 1.0f)
        val name = when {
            remoteMgr != null && remoteMgr.name.isNotBlank() -> remoteMgr.name
            localMgr != null && localMgr.name.isNotBlank() -> localMgr.name
            else -> def.name
        }
        val logs = if (remoteMgr != null && remoteMgr.actionLogs.isNotEmpty()) remoteMgr.actionLogs else (localMgr?.actionLogs ?: emptyList())
        val sal = maxOf(remoteMgr?.dailySalary ?: 0L, localMgr?.dailySalary ?: 0L).let {
            if (it > 0L) it else def.dailySalary
        }
        def.copy(
            isHired = isHired,
            level = level,
            efficiency = eff,
            name = if (isHired) name else "",
            dailySalary = sal,
            actionLogs = logs,
            isActive = remoteMgr?.isActive ?: localMgr?.isActive ?: true
        )
    }
    val recalculatedManagers = recalculateManagerSalaries(mergedManagers)
    _managers.value = recalculatedManagers

    // Save synchronously to local DataStore to keep snapshot in sync with loaded cloud state
    viewModelScope.launch {
        saveEconomicDataToDataStoreSuspend()
    }
    
    try {
        val dq = if (snapshot.dailyQuestStateJson.isNotBlank() && snapshot.dailyQuestStateJson != "{}") {
            try {
                com.example.data.network.AppJson.decodeFromString<com.example.data.quest.DailyQuestState>(snapshot.dailyQuestStateJson)
            } catch (e: Exception) {
                com.example.data.quest.DailyQuestState()
            }
        } else {
            com.example.data.quest.DailyQuestState()
        }
        val todayKey = com.example.data.quest.DailyQuestManager.getTodayDateKey()
        val currentLevel = snapshot.level
        val currentDq = _dailyQuestState.value

        val mergedFreeTiers = (currentDq.claimedFreeTiers + dq.claimedFreeTiers).toSet()
        val mergedVipTiers = (currentDq.claimedVipTiers + dq.claimedVipTiers).toSet()
        val mergedClaimedQuestIds = if (currentDq.lastResetDateKey == todayKey && dq.lastResetDateKey == todayKey) {
            (currentDq.claimedQuestIdsToday + dq.claimedQuestIdsToday).toSet()
        } else if (currentDq.lastResetDateKey == todayKey) {
            currentDq.claimedQuestIdsToday
        } else if (dq.lastResetDateKey == todayKey) {
            dq.claimedQuestIdsToday
        } else {
            emptySet()
        }

        val mergedQuests: List<com.example.data.quest.DailyQuest>
        val finalResetKey: String

        if (currentDq.lastResetDateKey == todayKey && dq.lastResetDateKey == todayKey && currentDq.quests.isNotEmpty() && dq.quests.isNotEmpty()) {
            val currentQuestsMap = currentDq.quests.associateBy { it.id }
            mergedQuests = dq.quests.map { dqQuest ->
                val currQuest = currentQuestsMap[dqQuest.id]
                val wasClaimed = dqQuest.isClaimed || (currQuest?.isClaimed == true) || mergedClaimedQuestIds.contains(dqQuest.id)
                val progress = if (currQuest != null) maxOf(dqQuest.currentProgress, currQuest.currentProgress) else dqQuest.currentProgress
                dqQuest.copy(
                    currentProgress = progress,
                    isClaimed = wasClaimed
                )
            }
            finalResetKey = todayKey
        } else if (currentDq.lastResetDateKey == todayKey && currentDq.quests.isNotEmpty()) {
            mergedQuests = currentDq.quests.map { q ->
                q.copy(isClaimed = q.isClaimed || mergedClaimedQuestIds.contains(q.id))
            }
            finalResetKey = todayKey
        } else if (dq.lastResetDateKey == todayKey && dq.quests.isNotEmpty()) {
            mergedQuests = dq.quests.map { q ->
                q.copy(isClaimed = q.isClaimed || mergedClaimedQuestIds.contains(q.id))
            }
            finalResetKey = todayKey
        } else {
            mergedQuests = com.example.data.quest.DailyQuestManager.generateFreshDailyQuests(currentLevel)
            finalResetKey = todayKey
        }

        val maxSeasonXp = maxOf(dq.seasonXp, currentDq.seasonXp)
        val maxSeasonLevel = com.example.data.quest.DailyQuestManager.computeSeasonLevel(maxSeasonXp)

        _dailyQuestState.value = dq.copy(
            quests = mergedQuests,
            seasonXp = maxSeasonXp,
            seasonLevel = maxSeasonLevel,
            lastResetDateKey = finalResetKey,
            claimedFreeTiers = mergedFreeTiers,
            claimedVipTiers = mergedVipTiers,
            claimedQuestIdsToday = mergedClaimedQuestIds,
            isVipPassUnlocked = currentDq.isVipPassUnlocked || dq.isVipPassUnlocked
        )
    } catch (e: Exception) {
        checkDailyQuestsReset()
    }

    checkAndProcessFacilityConstructions(isOffline = true)
    checkAndProcessFacilityUpgrades(isOffline = true)
    checkAndProcessActiveProductions(isOffline = true)
    checkAndProcessActiveDeliveries()
}
fun GameViewModel.syncPlayerToSupabaseOnAuth(
    username: String = "",
    pass: String = "",
    force: Boolean = false,
    lastSavedTime: Long = 0L,
    isStartup: Boolean = false
) {
    if (!isStartup && _isOnlineRegistered.value && _onlineEmail.value.isNotBlank() && _onlineEmail.value != "misafir_tuccar") {
        viewModelScope.launch {
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }
}
fun GameViewModel.checkOfflineMuseumAuctions() {
    viewModelScope.launch {
        com.example.data.MuseumHeritageManager.seedMissingArtifactsToSupabase()
    }
}
fun GameViewModel.checkAndApplyRemoteAdminModifications() {}
private val processedPurchaseOrders = java.util.Collections.synchronizedSet(mutableSetOf<String>())

fun GameViewModel.onIapPurchaseSuccess(productId: String = "", gems: Int = 0, orderId: String? = null) {
    val gemsToAward = if (gems > 0) {
        gems
    } else if (productId.isNotBlank()) {
        com.example.data.billing.BillingManager.getGemsForProduct(productId)
    } else {
        50
    }

    val p = player.value ?: return

    if (!orderId.isNullOrBlank()) {
        if (processedPurchaseOrders.contains(orderId)) {
            Log.d("GameViewModel", "Purchase orderId $orderId already processed")
            return
        }
        processedPurchaseOrders.add(orderId)
    }

    val newGems = p.gems + gemsToAward
    val updatedPlayer = p.copy(gems = newGems)

    appScope.launch {
        try {
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value && _onlineEmail.value.isNotBlank()) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error in onIapPurchaseSuccess", e)
        }
    }

    SmartNotificationManager.show(
        "💎 +$gemsToAward Elmas başarıyla hesabınıza eklendi!",
        "💎 +$gemsToAward Gems successfully added to your account!",
        NotificationType.SUCCESS
    )
}

fun GameViewModel.relocateWarehouse(targetCityId: String) {}
fun GameViewModel.sellGemsForGameMoney(gems: Int, expectedMoney: Long) {}
fun GameViewModel.skipProductionWithGems(productId: String) {
    val production = _activeProductions.value.firstOrNull { it.productId == productId } ?: return
    val remainingMs = (production.startTimeMs + production.totalDurationMs) - System.currentTimeMillis()
    if (remainingMs <= 0) return
    
    val gemCost = maxOf(1, (remainingMs / 3_600_000L).toInt())
    val p = player.value ?: return
    
    if (p.gems >= gemCost) {
        val updatedPlayer = p.copy(gems = p.gems - gemCost)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            val updatedProduction = production.copy(
                startTimeMs = System.currentTimeMillis() - production.totalDurationMs
            )
            _activeProductions.value = _activeProductions.value.map { if (it.id == production.id) updatedProduction else it }
            _productionSkip.update { it + productId }
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            
            SmartNotificationManager.show(
                "Üretim Hızlandırıldı! -$gemCost Elmas 🚀",
                "Production Sped Up! -$gemCost Gems 🚀",
                com.example.ui.components.NotificationType.SUCCESS
            )
        }
    } else {
        SmartNotificationManager.show(
            "Yetersiz Elmas! ($gemCost gerekli)",
            "Not enough Gems! ($gemCost required)",
            com.example.ui.components.NotificationType.ALERT
        )
    }
}
fun GameViewModel.skipDeliveryWithGems(deliveryId: String) {
    val delivery = _activeDeliveries.value.firstOrNull { it.id == deliveryId } ?: return
    val remainingMs = (delivery.startTimeMs + delivery.totalDurationMs) - System.currentTimeMillis()
    if (remainingMs <= 0) return
    
    val gemCost = maxOf(1, (remainingMs / 3_600_000L).toInt())
    val p = player.value ?: return
    
    if (p.gems >= gemCost) {
        val updatedPlayer = p.copy(gems = p.gems - gemCost)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            val updatedDelivery = delivery.copy(
                startTimeMs = System.currentTimeMillis() - delivery.totalDurationMs
            )
            _activeDeliveries.value = _activeDeliveries.value.map { if (it.id == delivery.id) updatedDelivery else it }
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            
            SmartNotificationManager.show(
                "Teslimat Hızlandırıldı! -$gemCost Elmas 🚀",
                "Delivery Sped Up! -$gemCost Gems 🚀",
                com.example.ui.components.NotificationType.SUCCESS
            )
        }
    } else {
        SmartNotificationManager.show(
            "Yetersiz Elmas! ($gemCost gerekli)",
            "Not enough Gems! ($gemCost required)",
            com.example.ui.components.NotificationType.ALERT
        )
    }
}
fun GameViewModel.applyTimeWarpWithGems(hours: Int = 1, gemCost: Int = 100) {}
fun GameViewModel.skipResearchWithGems(techId: String) {
    val baseId = techId.removePrefix("tech_")
    val endMs = _activeResearches.value[baseId] ?: _activeResearches.value["tech_$baseId"]
    if (endMs == null || endMs <= 0L) return
    
    val remainingMs = endMs - System.currentTimeMillis()
    if (remainingMs <= 0) {
        completeResearch(baseId)
        return
    }
    
    val gemCost = kotlin.math.ceil(remainingMs / 3600_000.0).toInt().coerceAtLeast(1)
    val p = player.value ?: return
    
    if (p.gems >= gemCost) {
        val updatedPlayer = p.copy(gems = p.gems - gemCost)
        viewModelScope.launch {
            repository.updatePlayer(updatedPlayer)
            completeResearch(baseId)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            
            SmartNotificationManager.show(
                "Araştırma Hızlandırıldı! -$gemCost Elmas 🚀",
                "Research Sped Up! -$gemCost Gems 🚀",
                com.example.ui.components.NotificationType.SUCCESS
            )
        }
    } else {
        SmartNotificationManager.show(
            "Yetersiz Elmas! ($gemCost gerekli)",
            "Not enough Gems! ($gemCost required)",
            com.example.ui.components.NotificationType.ALERT
        )
    }
}
fun GameViewModel.resetManagerDisciplineWithGems(managerId: String) {}

fun GameViewModel.onProductProduced(productId: String, quantity: Int, facilityId: String? = null) {}
fun GameViewModel.addXp(amount: Int) {
    if (amount > 0) {
        processXpGain(amount)
    }
}
fun GameViewModel.maintainBusiness(facilityId: Int) {
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val biz = businesses.value.find { it.id == facilityId } ?: return@launch
        val p = player.value ?: return@launch
        val repairCost = calculateFacilityMaintenanceCost(biz)
        if (p.money >= repairCost) {
            val updatedPlayer = p.copy(money = p.money - repairCost)
            repository.updatePlayer(updatedPlayer)
            val repairedBiz = biz.copy(wearLevel = 0f)
            repository.updateBusiness(repairedBiz)
            com.example.ui.components.SmartNotificationManager.show("Tesis bakımı tamamlandı (₳${com.example.ui.components.formatMoney(repairCost)}).", "Facility maintenance completed (₳${com.example.ui.components.formatMoney(repairCost)}).", com.example.ui.components.NotificationType.SUCCESS)
        } else {
            com.example.ui.components.SmartNotificationManager.show("Yetersiz bakiye. Gerekli: ₳${com.example.ui.components.formatMoney(repairCost)}", "Insufficient funds. Required: ₳${com.example.ui.components.formatMoney(repairCost)}", com.example.ui.components.NotificationType.ALERT)
        }
    }
}
fun GameViewModel.maintainBusiness(facilityId: String) {
    facilityId.toIntOrNull()?.let { maintainBusiness(it) }
}
fun GameViewModel.maintainBusiness(business: BusinessEntity) {
    maintainBusiness(business.id)
}
fun GameViewModel.setInitialWarehouseCity(cityId: String) {
    viewModelScope.launch {
        val p = player.value
        if (p != null) {
            val startingMoney = if (p.money <= 0L) 100_000L else p.money
            val updatedPlayer = p.copy(
                currentCity = cityId,
                money = startingMoney
            )
            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        }
        repository.markWarehouseSet()
    }
}
fun GameViewModel.getTreasuryCashReserve(): Long = player.value?.money ?: 0L

fun GameViewModel.startBorsaEconomyLoop() {}
fun GameViewModel.startAutoLevelCheckLoop() {}
fun GameViewModel.startCentralBankSimulation() {}
fun GameViewModel.startPublicConsumptionLoop() {}
fun GameViewModel.startLogisticsDeliveryLoop() {}
fun GameViewModel.startAutomationLoop() {
    viewModelScope.launch {
        while (true) {
            kotlinx.coroutines.delay(15000L) // Every 15 seconds
            try {
                // 1. Banka Para Yatırım (mgr_treasury)
                val isTreasuryHired = _managers.value.find { it.id == "mgr_treasury" }?.let { it.isHired && it.isActive } == true
                if (isTreasuryHired) {
                    val p = player.value
                    if (p != null && p.money > 15_000_000L) {
                        val toDeposit = (p.money - 5_000_000L) // Keep 5m safe
                        depositMoney(toDeposit, isSilent = true)
                    }
                }

                // 2. Üretim (mgr_prod - Üretim Müdürü)
                if (isAutoProduceActive.value) {
                    for (b in businesses.value) {
                        val prod = com.example.data.Product.values().find { it.facilityId == b.type || it.id == b.type }
                        if (prod != null && !b.isConstructing && b.getRemainingStorageCapacity() >= 5) {
                            if (!_productionProgress.value.containsKey(prod.id) && _activeProductions.value.none { it.productId == prod.id }) {
                                val batchQty = minOf(10, b.getRemainingStorageCapacity()).coerceAtLeast(1)
                                produce(prod.id, batchQty, isSilent = true)
                            }
                        }
                    }
                }

                // 3. Depo Sevkiyatı ve Otomatik Satış (mgr_logistics - Depo Müdürü)
                if (isAutoSellActive.value) {
                    val totalCap = player.value?.inventoryCapacity ?: 5000
                    val currentStock = inventory.value.sumOf { it.quantity }
                    val fillRatio = if (totalCap > 0) currentStock.toFloat() / totalCap.toFloat() else 0f
                    
                    // Depo Müdürü merkez deposunda %75 veya üzeri doluluk oranı gördüğünde otomatik satış yapar
                    if (fillRatio >= 0.75f) {
                        val sellableItems = inventory.value.filter { it.quantity > 0 }
                        if (sellableItems.isNotEmpty()) {
                            // Öncelik: İşlenmiş ürünler, yoksa en çok stoğu olan ürün
                            val prods = com.example.data.Product.values().filter { !it.recipe.isEmpty() }
                            val targetItem = sellableItems.find { item -> prods.any { it.id == item.itemId } }
                                ?: sellableItems.maxByOrNull { it.quantity }
                                
                            if (targetItem != null) {
                                val targetReduction = (currentStock - (totalCap * 0.50f).toInt()).coerceAtLeast(10)
                                val sellQty = minOf(targetItem.quantity, targetReduction)
                                if (sellQty > 0) {
                                    sell(targetItem.itemId, sellQty, isSilent = true)
                                    val perc = (fillRatio * 100).toInt()
                                    logManagerAction(
                                        "mgr_logistics",
                                        "Depo Müdürü: Merkez depo %$perc doluluğa ulaştığı için ${targetItem.itemId} x$sellQty Ton borsa satışı yapıldı.",
                                        0L
                                    )
                                }
                            }
                        }
                    }
                    
                    // MegaProject Auto Sell
                    for (proj in _megaProjects.value) {
                        if (proj.isAutoSellActive && proj.warehouseStock > 0) {
                            sellConsortiumWarehouseStock(proj.id, isSilent = true)
                        }
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Automation loop error", e)
            }
        }
    }
}

fun GameViewModel.startRealTimeEconomyLoop() {}
fun GameViewModel.startMidnightResetLoop() {}
fun GameViewModel.startDemoPlayerBotLoop() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
        // Initial sync to leaderboard
        com.example.data.BotTycoonManager.syncBotsToMultiplayerLeaderboard()
        kotlinx.coroutines.delay(2000L)

        // Seed initial bot listings in market if needed
        com.example.data.BotTycoonManager.processMarketTradingCycle(this@startDemoPlayerBotLoop)

        var cycleCount = 0
        while (true) {
            try {
                kotlinx.coroutines.delay(12000L) // Every 12 seconds
                cycleCount++

                // 1. Market Trading Cycle (Buys player's goods, posts fresh market offers, fulfills buy orders)
                com.example.data.BotTycoonManager.processMarketTradingCycle(this@startDemoPlayerBotLoop)

                // 2. Live Borsa Exchange Cycle (Bots trade Tier 1 commodities with Borsa pool)
                com.example.data.BotTycoonManager.processBorsaTradingCycle(this@startDemoPlayerBotLoop)

                // 3. Consortium Delivery Cycle (Every 24s)
                if (cycleCount % 2 == 0) {
                    com.example.data.BotTycoonManager.processConsortiumCycle(this@startDemoPlayerBotLoop)
                }

                // 4. R&D and Net Worth Progression Cycle (Every 36s)
                if (cycleCount % 3 == 0) {
                    com.example.data.BotTycoonManager.processBotProgressionCycle()
                }

                // 5. Foreclosure Auction Maintenance & Bot Bidding Cycle (Every 24s)
                if (cycleCount % 2 == 0) {
                    val p = player.value
                    if (p != null) {
                        val wins = com.example.data.ForeclosureManager.settleCompletedAuctions(p)
                        for (win in wins) {
                            if (p.money >= win.winningBidAmount) {
                                repository.buyBusinessTransaction(cost = win.winningBidAmount, business = win.wonBusiness)
                                com.example.ui.components.SmartNotificationManager.show(
                                    "🏆 İhale Kazanıldı: ${win.wonBusiness.cityId.replaceFirstChar { it.uppercase() }} - Seviye ${win.wonBusiness.level} tesis ₳${com.example.ui.components.formatMoney(win.winningBidAmount)} bedelle portföyünüze eklendi!",
                                    com.example.ui.components.NotificationType.SUCCESS
                                )
                                saveEconomicDataToDataStore(immediate = true)
                            }
                        }
                    }
                    com.example.data.ForeclosureManager.generateBotAuctions()
                    com.example.data.ForeclosureManager.processBotBiddingCycle()
                }

                // 6. Autopilot Smart Directives Execution Cycle (Every 24s)
                if (cycleCount % 2 == 0) {
                    com.example.data.automation.AutopilotEngine.evaluateLiveGameContext(this@startDemoPlayerBotLoop)
                    val smartDirectives = com.example.data.automation.AutopilotEngine.directivesState.value
                    for (d in smartDirectives) {
                        if (d.isAutoPilotEnabled) {
                            com.example.data.automation.AutopilotEngine.executeDirective(d.id, this@startDemoPlayerBotLoop)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "DemoPlayerBotLoop error", e)
            }
        }
    }
}
fun GameViewModel.startAutoSaveDataStoreLoop() {
    viewModelScope.launch {
        while (true) {
            kotlinx.coroutines.delay(10000L) // Save locally every 10 seconds
            saveEconomicDataToDataStore()
        }
    }
}
fun GameViewModel.triggerMinuteAutoCloudBackup() {
    val cal = java.util.Calendar.getInstance()
    val minuteKey = cal.get(java.util.Calendar.DAY_OF_YEAR) * 1440 + cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    if (lastAutoCloudBackupMinuteKey == minuteKey) {
        return // Bu dakikada zaten yedekleme tetiklendi
    }
    lastAutoCloudBackupMinuteKey = minuteKey
    val timeFormatted = String.format(
        java.util.Locale.getDefault(),
        "%02d:%02d:00",
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE)
    )
    Log.i("GameViewModel", "⏰ Her dakika başı otomatik bulut yedekleme tetiklendi: $timeFormatted")

    // syncCloudSaveToSupabase arka planda onlineEmail/snapshot kontrolünü yapıp Supabase'e güvenle yedekler
    syncCloudSaveToSupabase(force = true, immediate = true, isManual = false)
}

fun GameViewModel.checkAndTriggerHourlyBorsaPriceRecalculation(cal: java.util.Calendar) {
    val currentMinute = cal.get(java.util.Calendar.MINUTE)
    val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val hourKey = "${cal.get(java.util.Calendar.DAY_OF_YEAR)}_$currentHour"

    // Her saat başında (:00 dakikasında) veya uygulama yeni açıldığında bu saat için henüz hesaplanmamışsa
    if ((currentMinute == 0 || lastHourlyBorsaRecalculationKey.isEmpty()) && lastHourlyBorsaRecalculationKey != hourKey) {
        lastHourlyBorsaRecalculationKey = hourKey
        _lastHourlyBorsaRecalculationTimeMs.value = System.currentTimeMillis()
        recalculateAllBorsaPricesFromStocks(notifyUser = (currentMinute == 0))
    }
}

fun GameViewModel.recalculateAllBorsaPricesFromStocks(notifyUser: Boolean = true) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val currentPrices = marketPrices.value
        if (currentPrices.isEmpty()) return@launch

        val updatedPrices = currentPrices.map { p ->
            val prod = Product.values().find { it.id == p.itemId }
            val basePrice = prod?.basePrice ?: p.price
            val recalculatedPrice = MacroEconomyEngine.calculatePriceFromStock(
                stock = p.borsaStock,
                basePrice = basePrice
            )
            p.copy(price = recalculatedPrice)
        }

        repository.updateMarketPrices(updatedPrices, syncToRemote = false)
        repository.syncGlobalBorsaPricesToSupabase(updatedPrices, force = true)
        MultiplayerManager.sendBroadcastBorsaPrices(updatedPrices)

        if (notifyUser) {
            val msgTr = "⏰ SAATLİK BORSA GÜNCELLEMESİ: Tüm emtiaların fiyatları güncel stok arz-talep formülüyle yenilendi! 📈"
            val msgEn = "⏰ HOURLY EXCHANGE UPDATE: Commodity prices recalculated via real-time stock supply-demand formula! 📈"
            _newsTickerMessage.value = "$msgTr | ${_newsTickerMessage.value}"
            com.example.ui.components.SmartNotificationManager.show(msgTr, msgEn, com.example.ui.components.NotificationType.INFO)
        }
    }
}

fun GameViewModel.startSupabaseAutoSyncLoop() {
    viewModelScope.launch {
        // Başlangıçta DataStore ve yerel durum yüklemesi için kısa süre bekle
        kotlinx.coroutines.delay(3500L)

        // Açılışta kullanıcı online ise ilk senkronizasyonu yap
        try {
            var email = _onlineEmail.value.trim()
            if (email.isBlank() || email == "misafir_tuccar" || email.startsWith("guest")) {
                val stored = repository.economicDataStore?.getEconomicSnapshot()
                val candidate = stored?.onlineEmail.orEmpty().trim()
                if (candidate.isNotBlank() && candidate != "misafir_tuccar" && !candidate.startsWith("guest")) {
                    _onlineEmail.value = candidate
                    _isOnlineRegistered.value = true
                }
            }
            if (_isOnlineRegistered.value && _onlineEmail.value.isNotBlank() && _onlineEmail.value != "misafir_tuccar") {
                syncCloudSaveToSupabase(force = true, immediate = true, isManual = false)
            }
        } catch (e: Exception) {
            Log.e("GameViewModel", "Initial Supabase sync error", e)
        }

        while (true) {
            val now = System.currentTimeMillis()
            // Dakika başına (:00 saniyesine) kalan milisaniyeyi kesin olarak hesapla
            val millisInCurrentMinute = now % 60_000L
            val millisUntilNextMinute = (60_000L - millisInCurrentMinute).coerceIn(100L, 60_000L)

            // Tam dakika başına kadar bekle
            kotlinx.coroutines.delay(millisUntilNextMinute)

            // Dakika başı otomatik bulut yedeklemesini gerçekleştir
            triggerMinuteAutoCloudBackup()

            // Saat başı (:00) borsa stok-fiyat yeniden hesaplamasını gerçekleştir
            checkAndTriggerHourlyBorsaPriceRecalculation(java.util.Calendar.getInstance())
        }
    }
}

fun GameViewModel.addBorsaLimitOrder(order: com.example.data.BorsaLimitOrder): Boolean {
    val currentOrders = _borsaLimitOrders.value
    val borsaMgr = _managers.value.find { it.id == "mgr_borsa" }
    val maxAllowedOrders = if (borsaMgr != null && borsaMgr.isHired) {
        3 + (borsaMgr.level * 3) // Lvl 1: 6, Lvl 2: 9, Lvl 3: 12
    } else {
        3 // Free basic limit orders
    }

    if (currentOrders.size >= maxAllowedOrders) {
        val msgTr = "Maksimum aktif limit emri kapasitesine ulaşıldı ($maxAllowedOrders)! Borsa Müdürü Burak Koç'u terfi ettirerek limitinizi artırabilirsiniz."
        val msgEn = "Max active limit order capacity reached ($maxAllowedOrders)! Promote Borsa Manager to increase capacity."
        com.example.ui.components.SmartNotificationManager.show(msgTr, msgEn, com.example.ui.components.NotificationType.ALERT)
        return false
    }

    val updated = currentOrders + order
    _borsaLimitOrders.value = updated
    viewModelScope.launch {
        repository.saveBorsaLimitOrders(updated)
        logManagerAction(
            "mgr_borsa",
            "🤖 AI Borsa Emri Kuruldu: ${order.itemId} (${order.orderType.name}) Hedef: ₳${com.example.ui.components.formatMoney(order.targetPrice)} x${order.quantity} Ton",
            0L
        )
    }
    val prodName = com.example.data.Product.values().find { it.id == order.itemId }?.getDisplayName() ?: order.itemId
    val typeName = when (order.orderType) {
        com.example.data.LimitOrderType.BUY_BELOW -> "ALIM"
        com.example.data.LimitOrderType.SELL_ABOVE -> "SATIM"
        com.example.data.LimitOrderType.ARBITRAGE_AUTO -> "ARBITRAJ"
    }
    com.example.ui.components.SmartNotificationManager.show(
        "🤖 AI Borsa Emri Aktif: $prodName $typeName (Hedef: ₳${com.example.ui.components.formatCredit(order.targetPrice)})",
        com.example.ui.components.NotificationType.SUCCESS
    )
    checkAndExecuteBorsaLimitOrders()
    return true
}

fun GameViewModel.removeBorsaLimitOrder(orderId: String) {
    val updated = _borsaLimitOrders.value.filter { it.id != orderId }
    _borsaLimitOrders.value = updated
    viewModelScope.launch {
        repository.saveBorsaLimitOrders(updated)
    }
    com.example.ui.components.SmartNotificationManager.show(
        "Borsa emri iptal edildi.",
        "Limit order cancelled.",
        com.example.ui.components.NotificationType.INFO
    )
}

fun GameViewModel.toggleBorsaLimitOrder(orderId: String, isActive: Boolean) {
    val updated = _borsaLimitOrders.value.map {
        if (it.id == orderId) it.copy(isActive = isActive) else it
    }
    _borsaLimitOrders.value = updated
    viewModelScope.launch {
        repository.saveBorsaLimitOrders(updated)
    }
    val msg = if (isActive) "Emir tekrar aktifleştirildi." else "Emir duraklatıldı."
    val msgEn = if (isActive) "Order activated." else "Order paused."
    com.example.ui.components.SmartNotificationManager.show(msg, msgEn, com.example.ui.components.NotificationType.INFO)
}

fun GameViewModel.createSmartArbitrageBot(
    itemId: String,
    quantity: Int = 10,
    customBuyPrice: Long? = null,
    customSellPrice: Long? = null
) {
    val prod = com.example.data.Product.values().find { it.id == itemId } ?: return
    val currentPrice = marketPrices.value.find { it.itemId == itemId }?.price ?: prod.basePrice
    val basePrice = prod.basePrice.coerceAtLeast(10L)

    val buyTarget = customBuyPrice ?: ((currentPrice * 0.85).toLong().coerceAtLeast(basePrice / 2))
    val sellTarget = customSellPrice ?: ((currentPrice * 1.25).toLong().coerceAtLeast(buyTarget + (basePrice * 0.2).toLong()))

    val newOrder = com.example.data.BorsaLimitOrder(
        itemId = itemId,
        orderType = com.example.data.LimitOrderType.ARBITRAGE_AUTO,
        targetPrice = buyTarget,
        targetSellPrice = sellTarget,
        quantity = quantity,
        autoRepeat = true,
        note = "🤖 Akıllı AI Arbitraj: ₳${com.example.ui.components.formatCredit(buyTarget)} Al ➔ ₳${com.example.ui.components.formatCredit(sellTarget)} Sat"
    )

    addBorsaLimitOrder(newOrder)
}

fun GameViewModel.checkAndProcessActiveDeliveries() {
    viewModelScope.launch {
        val currentDeliveries = _activeDeliveries.value
        if (currentDeliveries.isEmpty()) return@launch

        val now = System.currentTimeMillis()
        val arrivedDeliveries = currentDeliveries.filter { (now - it.startTimeMs) >= it.totalDurationMs }

        if (arrivedDeliveries.isNotEmpty()) {
            var deliveryBonus = 0L
            val curPlayer = player.value ?: return@launch
            var playerMoney = curPlayer.money
            var playerDailyIncome = curPlayer.dailyIncome

            arrivedDeliveries.forEach { item ->
                if (!item.isOutboundSale) {
                    repository.produceItem(item.itemId, item.quantity)
                    
                    val prod = com.example.data.Product.values().find { p -> p.id == item.itemId }
                    val prodName = prod?.getDisplayName() ?: item.itemId
                    com.example.ui.components.SmartNotificationManager.show(
                        "🚚 Sevkiyat Ulaştı: $prodName x${item.quantity} Ton deponuza eklendi.",
                        com.example.ui.components.NotificationType.SUCCESS
                    )
                } else {
                    val prod = com.example.data.Product.values().find { p -> p.id == item.itemId }
                    val prodName = prod?.getDisplayName() ?: item.itemId
                    com.example.ui.components.SmartNotificationManager.show(
                        "🚚 İhracat Tamamlandı: $prodName başarıyla teslim edildi.",
                        com.example.ui.components.NotificationType.SUCCESS
                    )
                }
                
                deliveryBonus += item.totalCost / 10L
            }

            if (deliveryBonus > 0) {
                playerMoney += deliveryBonus
                playerDailyIncome += deliveryBonus
                repository.updatePlayer(
                    curPlayer.copy(
                        money = playerMoney,
                        dailyIncome = playerDailyIncome
                    )
                )
            }

            _activeDeliveries.value = currentDeliveries.filter { (now - it.startTimeMs) < it.totalDurationMs }
        }
    }
}

fun GameViewModel.checkAndExecuteBorsaLimitOrders() {
    viewModelScope.launch {
        val orders = _borsaLimitOrders.value.filter { it.isActive }
        if (orders.isEmpty()) return@launch

        val curPrices = marketPrices.value
        val curPlayer = player.value ?: return@launch
        val curInventory = inventory.value
        var playerMoney = curPlayer.money
        var playerProfit = curPlayer.totalProfit
        var playerDailyIncome = curPlayer.dailyIncome
        var playerDailyExpense = curPlayer.dailyExpense
        var ordersModified = false
        val updatedOrdersList = _borsaLimitOrders.value.toMutableList()

        for (i in updatedOrdersList.indices) {
            val order = updatedOrdersList[i]
            if (!order.isActive) continue

            val priceEntity = curPrices.find { it.itemId == order.itemId } ?: continue
            val currentPrice = priceEntity.price
            val product = com.example.data.Product.values().find { it.id == order.itemId } ?: continue

            // 1. BUY CHECK (BUY_BELOW or ARBITRAGE_AUTO buy phase)
            if (order.orderType == com.example.data.LimitOrderType.BUY_BELOW || order.orderType == com.example.data.LimitOrderType.ARBITRAGE_AUTO) {
                if (currentPrice <= order.targetPrice) {
                    val totalCost = currentPrice * order.quantity
                    val currentInvTotal = curInventory.sumOf { it.quantity }
                    val hasSpace = (currentInvTotal + order.quantity) <= curPlayer.inventoryCapacity
                    val hasMoney = playerMoney >= totalCost

                    if (hasMoney && hasSpace) {
                        playerMoney -= totalCost
                        playerDailyExpense += totalCost
                        repository.produceItem(order.itemId, order.quantity)
                        onBorsaItemBought(order.itemId, order.quantity, "Global")

                        val newExecCount = order.executedCount + 1
                        val newIsActive = if (order.orderType == com.example.data.LimitOrderType.ARBITRAGE_AUTO) true else order.autoRepeat
                        val updatedOrder = order.copy(
                            executedCount = newExecCount,
                            lastExecutedAtMs = System.currentTimeMillis(),
                            isActive = newIsActive
                        )
                        updatedOrdersList[i] = updatedOrder
                        ordersModified = true

                        val prodName = product.getDisplayName()
                        logManagerAction(
                            "mgr_borsa",
                            "🤖 AI Arbitraj Botu: $prodName x${order.quantity} Ton ₳${com.example.ui.components.formatMoney(currentPrice)} seviyesinden ALINDI (Hedef: <= ₳${com.example.ui.components.formatMoney(order.targetPrice)})",
                            -totalCost
                        )
                        com.example.ui.components.SmartNotificationManager.show(
                            "🤖 AI Bot ALIM Yaptı: $prodName x${order.quantity} Ton @ ₳${com.example.ui.components.formatCredit(currentPrice)}",
                            com.example.ui.components.NotificationType.SUCCESS
                        )
                        com.example.notification.LocalGameNotificationManager.postArbitrageExecutedNotification(
                            productDisplayName = prodName,
                            actionType = "ALIM",
                            quantity = order.quantity,
                            price = currentPrice
                        )
                    }
                }
            }

            // 2. SELL CHECK (SELL_ABOVE or ARBITRAGE_AUTO sell phase)
            val sellTarget = if (order.orderType == com.example.data.LimitOrderType.ARBITRAGE_AUTO && order.targetSellPrice > 0L) {
                order.targetSellPrice
            } else {
                order.targetPrice
            }

            if (order.orderType == com.example.data.LimitOrderType.SELL_ABOVE || (order.orderType == com.example.data.LimitOrderType.ARBITRAGE_AUTO && order.targetSellPrice > 0L)) {
                if (currentPrice >= sellTarget) {
                    val inStock = curInventory.find { it.itemId == order.itemId }?.quantity ?: 0
                    if (inStock >= order.quantity) {
                        repository.consumeItem(order.itemId, order.quantity)

                        val isCrisis = priceEntity.isCrisis
                        val crisisBonus = if (isCrisis) (currentPrice * order.quantity * 0.25).toLong() else 0L
                        val totalRevenue = (currentPrice * order.quantity) + crisisBonus
                        val baseCost = product.basePrice * order.quantity
                        val netProfit = (totalRevenue - baseCost).coerceAtLeast(0L)

                        playerMoney += totalRevenue
                        playerProfit += netProfit
                        playerDailyIncome += totalRevenue

                        onBorsaItemSold(order.itemId, order.quantity, "Global")

                        val newExecCount = order.executedCount + 1
                        val newProfit = order.totalRealizedProfit + netProfit
                        val newIsActive = if (order.orderType == com.example.data.LimitOrderType.ARBITRAGE_AUTO) true else order.autoRepeat
                        val updatedOrder = order.copy(
                            executedCount = newExecCount,
                            totalRealizedProfit = newProfit,
                            lastExecutedAtMs = System.currentTimeMillis(),
                            isActive = newIsActive
                        )
                        updatedOrdersList[i] = updatedOrder
                        ordersModified = true

                        val prodName = product.getDisplayName()
                        val bonusText = if (crisisBonus > 0L) " (+₳${com.example.ui.components.formatCredit(crisisBonus)} Devlet Teşviki!)" else ""
                        logManagerAction(
                            "mgr_borsa",
                            "🤖 AI Arbitraj Botu: $prodName x${order.quantity} Ton ₳${com.example.ui.components.formatMoney(currentPrice)} seviyesinden SATILDI$bonusText (Net Kâr: +₳${com.example.ui.components.formatMoney(netProfit)})",
                            totalRevenue
                        )
                        com.example.ui.components.SmartNotificationManager.show(
                            "🤖 AI Bot SATIM Yaptı: $prodName x${order.quantity} Ton @ ₳${com.example.ui.components.formatCredit(currentPrice)} (+₳${com.example.ui.components.formatCredit(netProfit)} Kâr)",
                            com.example.ui.components.NotificationType.SUCCESS
                        )
                        com.example.notification.LocalGameNotificationManager.postArbitrageExecutedNotification(
                            productDisplayName = prodName,
                            actionType = "SATIM",
                            quantity = order.quantity,
                            price = currentPrice,
                            profit = netProfit
                        )
                    }
                }
            }
        }

        if (ordersModified) {
            _borsaLimitOrders.value = updatedOrdersList
            repository.updatePlayer(
                curPlayer.copy(
                    money = playerMoney,
                    dollarBalance = playerMoney,
                    totalProfit = playerProfit,
                    dailyIncome = playerDailyIncome,
                    dailyExpense = playerDailyExpense
                )
            )
            repository.saveBorsaLimitOrders(updatedOrdersList)
            saveEconomicDataToDataStore()
        }
    }
}
