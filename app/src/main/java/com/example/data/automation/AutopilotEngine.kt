package com.example.data.automation

import com.example.data.BusinessEntity
import com.example.data.CompanyManager
import com.example.data.ManagerActionLog
import com.example.data.getDefaultCompanyManagers
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.deliverMaterialsToConsortium
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class SmartDirective(
    val id: String,
    val managerId: String,
    val managerName: String,
    val managerRole: String = "",
    val managerAvatarEmoji: String = "👔",
    val title: String,
    val description: String,
    val category: String,
    val estimatedFinancialImpact: Long = 0L,
    val isAutoPilotEnabled: Boolean = true,
    val cooldownMinutes: Int = 10,
    val lastExecutedMs: Long = 0L
)

object AutopilotEngine {

    private val directiveCooldowns = mutableMapOf<String, Long>()
    private val autopilotSettings = mutableMapOf<String, Boolean>()

    private val _directivesState = MutableStateFlow<List<SmartDirective>>(emptyList())
    val directivesState: StateFlow<List<SmartDirective>> = _directivesState.asStateFlow()

    private fun getManagerInfo(managerId: String, viewModel: GameViewModel): Pair<String, String> {
        val liveManager = viewModel.managers.value.find { it.id == managerId }
        if (liveManager != null) {
            return Pair(liveManager.name, liveManager.title)
        }
        val defaultManager = getDefaultCompanyManagers().find { it.id == managerId }
        return Pair(
            defaultManager?.name ?: "Yönetici",
            defaultManager?.title ?: "Holding Yöneticisi"
        )
    }

    private fun getManagerAvatar(managerId: String): String {
        return when (managerId) {
            "mgr_treasury" -> "👑"
            "mgr_contracts" -> "📜"
            "mgr_borsa" -> "📈"
            "mgr_logistics" -> "🚚"
            "mgr_hr" -> "👥"
            "mgr_rd" -> "🔬"
            "mgr_prod" -> "🏭"
            "mgr_maintenance" -> "🛠️"
            else -> "👔"
        }
    }

    fun setAutopilotEnabled(directiveId: String, enabled: Boolean) {
        autopilotSettings[directiveId] = enabled
        _directivesState.value = _directivesState.value.map {
            if (it.id == directiveId) it.copy(isAutoPilotEnabled = enabled) else it
        }
    }

    fun isAutopilotEnabled(directiveId: String): Boolean {
        return autopilotSettings[directiveId] ?: true
    }

    /**
     * Evaluates live game state from GameViewModel and generates intelligent directives for real company managers.
     */
    fun evaluateLiveGameContext(viewModel: GameViewModel): List<SmartDirective> {
        val directives = mutableListOf<SmartDirective>()
        val p = viewModel.player.value ?: return emptyList()
        val businesses = viewModel.businesses.value
        val inventory = viewModel.inventory.value
        val megaProjects = viewModel.megaProjects.value
        val marketPrices = viewModel.marketPrices.value
        val now = System.currentTimeMillis()

        // 1. MEHMET ÖZ (mgr_maintenance - Tesis Bakım Müdürü): Tesis Aşınması Kontrolü
        val wornBusinesses = businesses.filter { it.wearLevel >= 0.50f }
        if (wornBusinesses.isNotEmpty()) {
            val directiveId = "directive_preventive_maintenance"
            val lastExec = directiveCooldowns[directiveId] ?: 0L
            val (name, title) = getManagerInfo("mgr_maintenance", viewModel)
            val totalEstimatedCost = wornBusinesses.sumOf { 15_000L * it.level }
            val highestWear = (wornBusinesses.maxOf { it.wearLevel } * 100).toInt()
            directives.add(
                SmartDirective(
                    id = directiveId,
                    managerId = "mgr_maintenance",
                    managerName = name,
                    managerRole = title,
                    managerAvatarEmoji = getManagerAvatar("mgr_maintenance"),
                    title = "Önleyici Bakım & Revizyon",
                    description = "Yıpranması %$highestWear seviyesine ulaşan ${wornBusinesses.size} adet tesis için önleyici bakım uygulansın mı?",
                    category = "maintenance",
                    estimatedFinancialImpact = -totalEstimatedCost,
                    isAutoPilotEnabled = isAutopilotEnabled(directiveId),
                    cooldownMinutes = 10,
                    lastExecutedMs = lastExec
                )
            )
        }

        // 2. CANAN ÇELİK (mgr_contracts - Vadeli Sözleşme ve Tedarik Müdürü): Konsorsiyum Sevkiyatı
        var hasConsortiumNeedWithStock = false
        var consortiumItemSummary = ""
        var availableDeliverStock = 0
        for (proj in megaProjects) {
            for (slot in proj.slots) {
                if (!slot.isFullyDelivered && slot.remainingQuantity > 0) {
                    val stock = inventory.find { it.itemId == slot.productId || it.baseProductId == slot.productId }?.quantity ?: 0
                    if (stock > 0) {
                        hasConsortiumNeedWithStock = true
                        availableDeliverStock = minOf(slot.remainingQuantity, stock, 50)
                        consortiumItemSummary = "${slot.productId} (${availableDeliverStock} Ton hazır)"
                        break
                    }
                }
            }
            if (hasConsortiumNeedWithStock) break
        }

        if (hasConsortiumNeedWithStock) {
            val directiveId = "directive_consortium_shipment"
            val lastExec = directiveCooldowns[directiveId] ?: 0L
            val (name, title) = getManagerInfo("mgr_contracts", viewModel)
            directives.add(
                SmartDirective(
                    id = directiveId,
                    managerId = "mgr_contracts",
                    managerName = name,
                    managerRole = title,
                    managerAvatarEmoji = getManagerAvatar("mgr_contracts"),
                    title = "Konsorsiyuma Sevk Et",
                    description = "Konsorsiyum mega projelerinde talep edilen hammadde depoda mevcut ($consortiumItemSummary). Sevkiyat başlatılsın mı?",
                    category = "consortium",
                    estimatedFinancialImpact = 0L,
                    isAutoPilotEnabled = isAutopilotEnabled(directiveId),
                    cooldownMinutes = 10,
                    lastExecutedMs = lastExec
                )
            )
        }

        // 3. ZEYNEP DEMİR (mgr_logistics - Lojistik ve Pazar Satış Müdürü): Fazla Stok Tasfiyesi
        val totalStock = inventory.sumOf { it.quantity }
        val capacity = p.inventoryCapacity.coerceAtLeast(1)
        val fullnessRatio = totalStock.toFloat() / capacity.toFloat()
        if (fullnessRatio >= 0.75f) {
            val directiveId = "directive_sell_excess_stock"
            val lastExec = directiveCooldowns[directiveId] ?: 0L
            val (name, title) = getManagerInfo("mgr_logistics", viewModel)
            val percentInt = (fullnessRatio * 100).toInt()
            val largestStockItem = inventory.filter { it.quantity > 0 }.maxByOrNull { it.quantity }
            val sellQty = largestStockItem?.let { (it.quantity * 0.30f).toInt().coerceIn(1, 150) } ?: 25
            val unitPrice = largestStockItem?.let { item -> marketPrices.find { it.itemId == item.itemId }?.price ?: 800L } ?: 800L
            val estimatedRevenue = sellQty.toLong() * unitPrice

            directives.add(
                SmartDirective(
                    id = directiveId,
                    managerId = "mgr_logistics",
                    managerName = name,
                    managerRole = title,
                    managerAvatarEmoji = getManagerAvatar("mgr_logistics"),
                    title = "Fazla Stoğu Borsada Sat",
                    description = "Merkez depo doluluğu %$percentInt! ${largestStockItem?.itemId ?: "Fazla ürünler"} ($sellQty Ton) satılarak nakde çevrilsin mi?",
                    category = "logistics",
                    estimatedFinancialImpact = estimatedRevenue,
                    isAutoPilotEnabled = isAutopilotEnabled(directiveId),
                    cooldownMinutes = 10,
                    lastExecutedMs = lastExec
                )
            )
        }

        // 4. AYŞE KAYA, CFA (mgr_treasury - Hazine ve Makroekonomi Müdürü): Atıl Likidite & Faiz Fonu
        if (p.money >= 150_000L) {
            val depositAmount = (p.money * 0.15f).toLong().coerceIn(20_000L, 500_000L)
            val directiveId = "directive_treasury_deposit"
            val lastExec = directiveCooldowns[directiveId] ?: 0L
            val (name, title) = getManagerInfo("mgr_treasury", viewModel)
            val estDailyInterest = (depositAmount * 0.045f).toLong()

            directives.add(
                SmartDirective(
                    id = directiveId,
                    managerId = "mgr_treasury",
                    managerName = name,
                    managerRole = title,
                    managerAvatarEmoji = getManagerAvatar("mgr_treasury"),
                    title = "Atıl Likiditeyi Vadeli Mevduata Aktar",
                    description = "Kasada bekleyen ₳${com.example.ui.components.formatMoney(depositAmount)} tutarındaki fazla nakdi vadeli hesaba aktararak günlük faiz kazancı elde edilsin mi?",
                    category = "treasury",
                    estimatedFinancialImpact = estDailyInterest,
                    isAutoPilotEnabled = isAutopilotEnabled(directiveId),
                    cooldownMinutes = 10,
                    lastExecutedMs = lastExec
                )
            )
        }

        // 5. BURAK KOÇ (mgr_borsa - Borsa ve Yatırım Analisti): Fırsat Spot Alımı
        val discountedProduct = com.example.data.Product.values().firstOrNull { prod ->
            val curPrice = marketPrices.find { it.itemId == prod.id }?.price ?: prod.basePrice
            curPrice < (prod.basePrice * 0.85f)
        }
        if (discountedProduct != null) {
            val curPrice = marketPrices.find { it.itemId == discountedProduct.id }?.price ?: discountedProduct.basePrice
            val buyQty = 20
            val cost = curPrice * buyQty
            if (p.money >= cost * 2) {
                val directiveId = "directive_borsa_dip_buy"
                val lastExec = directiveCooldowns[directiveId] ?: 0L
                val (name, title) = getManagerInfo("mgr_borsa", viewModel)
                val discountPercent = (((discountedProduct.basePrice - curPrice).toFloat() / discountedProduct.basePrice) * 100).toInt()

                directives.add(
                    SmartDirective(
                        id = directiveId,
                        managerId = "mgr_borsa",
                        managerName = name,
                        managerRole = title,
                        managerAvatarEmoji = getManagerAvatar("mgr_borsa"),
                        title = "Borsada Dip Fırsatı: ${discountedProduct.name}",
                        description = "${discountedProduct.name} piyasa fiyatı %$discountPercent düştü (Birim: ₳$curPrice). İleride işlemek veya satmak üzere $buyQty Ton spot alım yapılsın mı?",
                        category = "borsa",
                        estimatedFinancialImpact = -cost,
                        isAutoPilotEnabled = isAutopilotEnabled(directiveId),
                        cooldownMinutes = 10,
                        lastExecutedMs = lastExec
                    )
                )
            }
        }

        // 6. AHMET YILMAZ (mgr_prod - Üretim ve Operasyon Müdürü): Boş Tesislerde Otomatik Üretim
        val idleBusiness = businesses.firstOrNull { biz ->
            !biz.isConstructing && biz.getRemainingStorageCapacity() >= 10
        }
        if (idleBusiness != null) {
            val prod = com.example.data.Product.values().find { it.facilityId == idleBusiness.type || it.id == idleBusiness.type }
            if (prod != null) {
                val directiveId = "directive_auto_production"
                val lastExec = directiveCooldowns[directiveId] ?: 0L
                val (name, title) = getManagerInfo("mgr_prod", viewModel)
                val batchQty = minOf(idleBusiness.getRemainingStorageCapacity(), 25)

                directives.add(
                    SmartDirective(
                        id = directiveId,
                        managerId = "mgr_prod",
                        managerName = name,
                        managerRole = title,
                        managerAvatarEmoji = getManagerAvatar("mgr_prod"),
                        title = "Kapasite Optimizasyonu: ${prod.name}",
                        description = "${idleBusiness.type} tesisinde atıl kapasite mevcut. $batchQty Ton ${prod.name} üretimi başlatılsın mı?",
                        category = "production",
                        estimatedFinancialImpact = 0L,
                        isAutoPilotEnabled = isAutopilotEnabled(directiveId),
                        cooldownMinutes = 10,
                        lastExecutedMs = lastExec
                    )
                )
            }
        }

        _directivesState.value = directives
        return directives
    }

    /**
     * Executes the directive adhering to Safety Reserve (25% untouchable cash) and 10-minute cooldown rule.
     * Records financial impact in ManagerActionLog.
     */
    suspend fun executeDirective(directiveId: String, viewModel: GameViewModel): Boolean {
        val p = viewModel.player.value ?: return false
        val now = System.currentTimeMillis()

        // 1. COOLDOWN CONTROL (10 Minutes)
        val lastExec = directiveCooldowns[directiveId] ?: 0L
        val cooldownMs = 10 * 60 * 1000L
        if (now - lastExec < cooldownMs) {
            return false
        }

        // 2. SAFETY CASH RESERVE (25% of player's money is untouchable reserve)
        val untouchableReserve = (p.money * 0.25).toLong()
        val spendableCash = (p.money - untouchableReserve).coerceAtLeast(0L)

        var executedSuccessfully = false

        when (directiveId) {
            "directive_preventive_maintenance" -> {
                val wornBusinesses = viewModel.businesses.value
                    .filter { it.wearLevel >= 0.50f }
                    .sortedByDescending { it.wearLevel }

                var currentAvailableCash = spendableCash
                var totalRepairCost = 0L
                var repairedCount = 0

                for (target in wornBusinesses) {
                    val repairCost = (15_000L * target.level)
                    if (currentAvailableCash >= repairCost) {
                        currentAvailableCash -= repairCost
                        totalRepairCost += repairCost
                        val repaired = target.copy(wearLevel = 0f)
                        viewModel.repository.updateBusiness(repaired)
                        repairedCount++
                    } else {
                        break
                    }
                }

                if (repairedCount > 0 && totalRepairCost > 0) {
                    val updatedPlayer = p.copy(money = p.money - totalRepairCost)
                    viewModel.repository.updatePlayer(updatedPlayer)
                    val (name, _) = getManagerInfo("mgr_maintenance", viewModel)
                    viewModel.logManagerAction(
                        managerId = "mgr_maintenance",
                        description = "$name: Önleyici bakım ile $repairedCount tesisin aşınması sıfırlandı (-₳${com.example.ui.components.formatMoney(totalRepairCost)}).",
                        impact = -totalRepairCost
                    )
                    directiveCooldowns[directiveId] = now
                    executedSuccessfully = true
                }
            }

            "directive_consortium_shipment" -> {
                val megaProjects = viewModel.megaProjects.value
                val inventory = viewModel.inventory.value
                var totalDeliveredQty = 0
                var deliveredProductSummary = ""

                for (proj in megaProjects) {
                    for (slot in proj.slots) {
                        if (!slot.isFullyDelivered && slot.remainingQuantity > 0) {
                            val eligibleItems = inventory.filter { item ->
                                val baseId = com.example.data.ItemQuality.extractBaseProductId(item.itemId)
                                baseId == slot.productId && proj.qualityTier.isQualityAllowed(item.quality) && item.quantity > 0
                            }
                            val stock = eligibleItems.sumOf { it.quantity }
                            if (stock > 0) {
                                val deliverQty = minOf(slot.remainingQuantity, stock, 50)
                                if (deliverQty > 0) {
                                    viewModel.deliverMaterialsToConsortium(
                                        projectId = proj.id,
                                        slotId = slot.slotId,
                                        quantity = deliverQty
                                    )
                                    totalDeliveredQty += deliverQty
                                    deliveredProductSummary = slot.productId
                                    break
                                }
                            }
                        }
                    }
                    if (totalDeliveredQty > 0) break
                }

                if (totalDeliveredQty > 0) {
                    val (name, _) = getManagerInfo("mgr_contracts", viewModel)
                    viewModel.logManagerAction(
                        managerId = "mgr_contracts",
                        description = "$name: Konsorsiyum sevkiyatı tamamlandı ($totalDeliveredQty Ton $deliveredProductSummary sevk edildi).",
                        impact = 0L
                    )
                    directiveCooldowns[directiveId] = now
                    executedSuccessfully = true
                }
            }

            "directive_sell_excess_stock" -> {
                val inventory = viewModel.inventory.value.filter { it.quantity > 0 }
                if (inventory.isNotEmpty()) {
                    val largestStockItem = inventory.maxByOrNull { it.quantity }
                    if (largestStockItem != null) {
                        val sellQty = (largestStockItem.quantity * 0.30f).toInt().coerceIn(1, 150)
                        val unitPrice = viewModel.marketPrices.value.find { it.itemId == largestStockItem.itemId }?.price ?: 800L
                        val totalRevenue = sellQty.toLong() * unitPrice

                        viewModel.repository.sellItem(largestStockItem.itemId, sellQty, unitPrice)
                        val updatedMoney = p.money + totalRevenue
                        val updatedPlayer = p.copy(money = updatedMoney)
                        viewModel.repository.updatePlayer(updatedPlayer)

                        val (name, _) = getManagerInfo("mgr_logistics", viewModel)
                        viewModel.logManagerAction(
                            managerId = "mgr_logistics",
                            description = "$name: Fazla stok borsada satıldı ($sellQty Ton ${largestStockItem.itemId} -> +₳${com.example.ui.components.formatMoney(totalRevenue)}).",
                            impact = totalRevenue
                        )
                        directiveCooldowns[directiveId] = now
                        executedSuccessfully = true
                    }
                }
            }

            "directive_treasury_deposit" -> {
                val depositAmount = (p.money * 0.15f).toLong().coerceIn(20_000L, 500_000L)
                if (spendableCash >= depositAmount) {
                    val updatedMoney = p.money - depositAmount
                    val updatedDeposit = p.depositBalance + depositAmount
                    val updatedPlayer = p.copy(money = updatedMoney, depositBalance = updatedDeposit)
                    viewModel.repository.updatePlayer(updatedPlayer)

                    val (name, _) = getManagerInfo("mgr_treasury", viewModel)
                    viewModel.logManagerAction(
                        managerId = "mgr_treasury",
                        description = "$name: Fazla likidite vadeli mevduat fonuna aktarıldı (+₳${com.example.ui.components.formatMoney(depositAmount)} Mevduat).",
                        impact = depositAmount
                    )
                    directiveCooldowns[directiveId] = now
                    executedSuccessfully = true
                }
            }

            "directive_borsa_dip_buy" -> {
                val discountedProduct = com.example.data.Product.values().firstOrNull { prod ->
                    val curPrice = viewModel.marketPrices.value.find { it.itemId == prod.id }?.price ?: prod.basePrice
                    curPrice < (prod.basePrice * 0.85f)
                }
                if (discountedProduct != null) {
                    val curPrice = viewModel.marketPrices.value.find { it.itemId == discountedProduct.id }?.price ?: discountedProduct.basePrice
                    val buyQty = 20
                    val totalCost = curPrice * buyQty
                    if (spendableCash >= totalCost) {
                        val currentInvQty = viewModel.inventory.value.find { it.itemId == discountedProduct.id }?.quantity ?: 0
                        val updatedMoney = p.money - totalCost
                        viewModel.repository.updatePlayer(p.copy(money = updatedMoney))
                        viewModel.repository.insertInventory(
                            com.example.data.InventoryEntity(discountedProduct.id, currentInvQty + buyQty)
                        )

                        val (name, _) = getManagerInfo("mgr_borsa", viewModel)
                        viewModel.logManagerAction(
                            managerId = "mgr_borsa",
                            description = "$name: Dip fiyattan hammadde temin edildi ($buyQty Ton ${discountedProduct.name} -> -₳${com.example.ui.components.formatMoney(totalCost)}).",
                            impact = -totalCost
                        )
                        directiveCooldowns[directiveId] = now
                        executedSuccessfully = true
                    }
                }
            }

            "directive_auto_production" -> {
                val idleBusiness = viewModel.businesses.value.firstOrNull { biz ->
                    !biz.isConstructing && biz.getRemainingStorageCapacity() >= 10
                }
                if (idleBusiness != null) {
                    val prod = com.example.data.Product.values().find { it.facilityId == idleBusiness.type || it.id == idleBusiness.type }
                    if (prod != null) {
                        val batchQty = minOf(idleBusiness.getRemainingStorageCapacity(), 25)
                        viewModel.produce(prod.id, batchQty, isSilent = true, autoProcure = true)

                        val (name, _) = getManagerInfo("mgr_prod", viewModel)
                        viewModel.logManagerAction(
                            managerId = "mgr_prod",
                            description = "$name: ${idleBusiness.type} tesisinde $batchQty Ton ${prod.name} seri üretimi başlatıldı.",
                            impact = 0L
                        )
                        directiveCooldowns[directiveId] = now
                        executedSuccessfully = true
                    }
                }
            }
        }

        // Re-evaluate context after execution
        evaluateLiveGameContext(viewModel)
        return executedSuccessfully
    }
}
