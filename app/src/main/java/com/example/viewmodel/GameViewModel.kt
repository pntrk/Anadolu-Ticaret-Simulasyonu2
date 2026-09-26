package com.example.viewmodel
import com.example.data.*
import kotlinx.collections.immutable.toPersistentList

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import android.util.Log

import kotlinx.coroutines.flow.first

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.data.BusinessEntity
import com.example.data.GameRepository
import com.example.data.TechTree

import com.example.data.InventoryEntity
import com.example.data.MarketPriceEntity
import com.example.data.MultiplayerManager
import com.example.data.PlayerEntity
import com.example.data.Product
import com.example.data.ProductTier
import com.example.data.GameStateEntity
import com.example.data.CloudServerTimeManager
import com.example.data.EconomicSnapshot
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationType
import com.example.ui.theme.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.network.jsonElementToAny
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.ln

import com.example.data.quest.DailyQuest
import com.example.data.quest.DailyQuestManager
import com.example.data.quest.DailyQuestState
import com.example.data.quest.QuestType
import com.example.data.quest.SeasonPassTier

data class DailyRewardData(
    val streakDay: Int,
    val rewardText: String,
    val isVipBonus: Boolean
)

data class OfflineEarningsData(
    val offlineDurationMs: Long,
    val netMoneyEarned: Long,
    val itemsProducedCount: Int,
    val xpGained: Int,
    val grossRevenue: Long = 0L,
    val totalUpkeepCost: Long = 0L,
    val depositInterestEarned: Long = 0L,
    val producedItemsSummary: Map<String, Int> = emptyMap(),
    val activeFacilitiesCount: Int = 0,
    val deliveriesCompletedCount: Int = 0,
    val consortiumDeliveriesCount: Int = 0,
    val consortiumDividendsEarned: Long = 0L,
    val managerActionsSummary: List<String> = emptyList()
)

class GameViewModel(internal val repository: GameRepository) : ViewModel() {

    internal fun processXpGain(xpGained: Int, currentPlayerState: PlayerEntity? = null) {
        val p = currentPlayerState ?: player.value ?: return
        val newXp = p.xp + xpGained
        val newLevel = com.example.data.XpLevelEngine.calculateLevel(newXp.toLong())
        viewModelScope.launch {
            if (newLevel > p.level) {
                repository.updatePlayer(p.copy(xp = newXp, level = newLevel))
                SmartNotificationManager.show(
                    "Tebrikler! Seviye atladınız: Seviye $newLevel 🎉",
                    "Congratulations! You leveled up: Level $newLevel 🎉",
                    NotificationType.SUCCESS
                )
            } else {
                repository.updatePlayer(p.copy(xp = newXp))
            }
        }
    }
    internal val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Exported StateFlows for clean architectural decoupling
    val _growthHistory = MutableStateFlow<List<com.example.data.GrowthPointDto>>(emptyList())
    val growthHistory: StateFlow<List<com.example.data.GrowthPointDto>> = _growthHistory.asStateFlow()
    val netWorth: StateFlow<Long> = _uiState.map { it.netWorth }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val facilityValuation: StateFlow<Long> = _uiState.map { it.facilityValuation }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val inventoryValuation: StateFlow<Long> = _uiState.map { it.inventoryValuation }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val marketTrends: StateFlow<Map<String, Float>> = _uiState.map { it.marketState.marketTrends }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val _playerGuildShares = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playerGuildShares: StateFlow<Map<String, Int>> = _playerGuildShares.asStateFlow()
    val _playerGuildBuyPrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val playerGuildBuyPrices: StateFlow<Map<String, Double>> = _playerGuildBuyPrices.asStateFlow()

    // Business & Facility Valuation Calculation Helpers (Moved out of UI layer)
    fun calculateFacilityBaseCost(business: BusinessEntity): Long {
        val product = Product.values().find { it.facilityId == business.type }
        val city = com.example.data.cities.find { it.id == business.cityId }
        val cityMultiplier = city?.economicMultiplier ?: 1.0f
        val rawCost = ((product?.facilityCost ?: 1_000_000L) * cityMultiplier).toLong()
        return rawCost.coerceAtLeast(1L)
    }

    fun calculateFacilityUpgradeCost(business: BusinessEntity): Long {
        val baseCost = calculateFacilityBaseCost(business)
        return (baseCost * 0.5 * 1.2.pow(business.level.toDouble())).toLong()
    }

    fun calculateFacilityMaintenanceCost(business: BusinessEntity): Long {
        val product = Product.values().find { it.facilityId == business.type }
        val city = com.example.data.cities.find { it.id == business.cityId }
        val cityMultiplier = city?.economicMultiplier ?: 1.0f
        val facilityCost = ((product?.facilityCost ?: 5000000L) * cityMultiplier).toLong()
        var rawMaintenanceCost = (facilityCost * 0.006f * business.wearLevel * (1.0f + (business.level - 1) * 0.2f))
        
        val maintManager = _managers.value.find { it.id == "mgr_maintenance" }
        if (maintManager != null && maintManager.isHired && maintManager.isActive) {
            val discount = (maintManager.level * 0.15f).coerceAtMost(0.9f)
            rawMaintenanceCost *= (1.0f - discount)
        }
        return rawMaintenanceCost.toLong().coerceAtLeast(1000L)
    }

    fun calculateFacilityValuationAndRefund(business: BusinessEntity): Pair<Long, Long> {
        val baseCost = calculateFacilityBaseCost(business)
        var totalUpgradeInvestment = 0L
        for (lvl in 1 until business.level) {
            totalUpgradeInvestment += (baseCost * 0.5 * 1.2.pow(lvl.toDouble())).toLong()
        }
        val totalInvestment = baseCost + totalUpgradeInvestment
        val refundAmount = (totalInvestment * 0.70f).toLong()
        return Pair(totalInvestment, refundAmount)
    }

    fun handleIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.RefreshDashboard -> {
                // Future use
            }
            is GameIntent.BuyFromBorsa -> buyFromBorsa(intent.productId, intent.quantity, intent.originCountry)
            is GameIntent.SellToBorsa -> sell(intent.productId, intent.quantity, intent.originCountry)
            is GameIntent.CreateMarketListing -> addMarketListing(intent.productId, intent.quantity, intent.price)
            is GameIntent.CancelMarketListing -> cancelMarketListing(intent.listingId)
            is GameIntent.CreateBuyOrder -> addBuyOrder(intent.productId, intent.quantity, intent.maxPrice)
            is GameIntent.CancelBuyOrder -> cancelBuyOrder(intent.orderId)
            is GameIntent.CreateFuturesContract -> addFuturesContract(intent.productId, intent.quantity, intent.strikePrice, intent.durationMinutes)
            is GameIntent.CancelFuturesContract -> cancelFuturesContract(intent.contractId)
            is GameIntent.FulfillFuturesContract -> fulfillFuturesContract(intent.contractId)
            is GameIntent.ExerciseFuturesContract -> {} // Not implemented
            is GameIntent.SellToBuyOrder -> sellToBuyOrder(intent.orderId)
            is GameIntent.UpdateListingPrice -> updateListingPrice(intent.listingId, intent.newPrice)
            is GameIntent.AddAuction -> addAuction(intent.productId, intent.quantity, intent.startingBid)
            is GameIntent.BuyFromGlobalMarket -> buyFromGlobalMarket(intent.listingId, intent.quantity)
            is GameIntent.UpgradeWarehouseCapacity -> upgradeWarehouseCapacity()
            is GameIntent.BuildBusiness -> buildBusiness(intent.facilityId, intent.cityId, intent.cost)
            is GameIntent.RelocateWarehouse -> relocateWarehouse(intent.targetCityId)
            is GameIntent.SellGems -> sellGemsForGameMoney(intent.gems, intent.expectedMoney)
            is GameIntent.TakeLoan -> takeLoan(intent.amount)
            is GameIntent.RepayLoan -> repayLoan(intent.amount)
            is GameIntent.DepositMoney -> depositMoney(intent.amount)
            is GameIntent.WithdrawDeposit -> withdrawDeposit(intent.amount)
            is GameIntent.SellGuildShares -> sellGuildShares(intent.guildId, intent.count)
            is GameIntent.ToggleManagerActive -> toggleManagerActive(intent.managerId)
            is GameIntent.HireManager -> hireManager(intent.managerId)
            is GameIntent.FireManager -> fireManager(intent.managerId)
            is GameIntent.UpgradeManager -> upgradeManager(intent.managerId)
            is GameIntent.ToggleAllManagersActiveStatus -> toggleAllManagersActiveStatus()
            is GameIntent.SpeedUpResearch -> speedUpResearch(intent.techKey)
            is GameIntent.UpgradeBusiness -> upgradeBusiness(intent.business)
            is GameIntent.SpeedUpBusinessUpgradeWithGems -> speedUpBusinessUpgradeWithGems(intent.businessId)
            is GameIntent.SpeedUpBusinessConstructionWithGems -> speedUpBusinessConstructionWithGems(intent.businessId)
            is GameIntent.MaintainBusiness -> maintainBusiness(intent.business)
            is GameIntent.Produce -> produce(intent.productId, intent.quantity, autoProcure = intent.autoProcure)
            is GameIntent.MassHarvestAndProduceAll -> massHarvestAndProduceAll()
            is GameIntent.BuyMissingIngredients -> buyMissingIngredients(intent.productId, intent.quantity)
            is GameIntent.SellBusiness -> sellBusiness(intent.business)
            is GameIntent.SkipProductionWithGems -> skipProductionWithGems(intent.productId)
            is GameIntent.SkipDeliveryWithGems -> skipDeliveryWithGems(intent.deliveryId)
            is GameIntent.ApplyTimeWarpWithGems -> applyTimeWarpWithGems(intent.hours, intent.gemCost)
            is GameIntent.SkipResearchWithGems -> skipResearchWithGems(intent.techId)
            is GameIntent.ResetManagerDisciplineWithGems -> resetManagerDisciplineWithGems(intent.managerId)

            is GameIntent.TransferFacilityStock -> transferFacilityStockToCentral(intent.businessId, intent.itemId, intent.quantity)
            is GameIntent.TransferAllFacilityStock -> transferAllFacilityStockToCentral(intent.businessId)
            is GameIntent.SellFacilityStockOnBorsa -> sellFacilityStockOnBorsa(intent.businessId, intent.itemId, intent.quantity)
            is GameIntent.SellAllFacilityStockOnBorsa -> sellAllFacilityStockOnBorsa(intent.businessId)
            is GameIntent.CheckAndClaimMonthlyLeaderboardReward -> checkAndClaimMonthlyLeaderboardReward(intent.forceManualCheck)
            is GameIntent.JoinConsortiumSlot -> joinConsortiumSlot(intent.projectId, intent.slotId, intent.playerId, intent.playerName)
            is GameIntent.LeaveConsortiumSlot -> leaveConsortiumSlot(intent.projectId, intent.slotId)
            is GameIntent.LeaveEntireConsortium -> leaveEntireConsortium(intent.projectId)
            is GameIntent.TakeoverBottleneckSlot -> takeoverBottleneckSlot(intent.projectId, intent.slotId, intent.playerId, intent.playerName)
            is GameIntent.KickPartnerFromConsortiumSlot -> kickPartnerFromConsortiumSlot(intent.projectId, intent.slotId)
            is GameIntent.SellConsortiumWarehouseStock -> sellConsortiumWarehouseStock(intent.projectId)
            is GameIntent.AdvanceMegaProjectStage -> advanceMegaProjectStage(intent.projectId)
            is GameIntent.ClaimMegaProjectDividend -> claimMegaProjectDividend(intent.projectId)
            is GameIntent.ProduceConsortiumBrandItem -> produceConsortiumBrandItem(intent.projectId)
            is GameIntent.ResetConsortiumNewBatch -> resetConsortiumNewBatch(intent.projectId)
            is GameIntent.ListenToConsortiumChat -> listenToConsortiumChat(intent.projectId)
            is GameIntent.DisbandConsortium -> disbandConsortium(intent.projectId)
            is GameIntent.SendConsortiumChatMessage -> sendConsortiumChatMessage(intent.projectId, intent.text)
            is GameIntent.CreateNewMegaProject -> createNewMegaProject(intent.consortiumName, intent.brandName, intent.targetProductId, intent.qualityTier, intent.founderClaimedProductIds, intent.cityId)
            is GameIntent.DeliverMaterialsToConsortium -> deliverMaterialsToConsortium(intent.projectId, intent.slotId, intent.quantity)
            is GameIntent.ApproveConsortiumMassProduction -> approveConsortiumMassProduction(intent.projectId)
            is GameIntent.ToggleConsortiumProductionState -> toggleConsortiumProductionState(intent.projectId)
            is GameIntent.ToggleConsortiumAutoSell -> toggleConsortiumAutoSell(intent.projectId, intent.autoSellActive)
            is GameIntent.ChangeConsortiumSalesChannel -> changeConsortiumSalesChannel(intent.projectId, intent.channel)
            is GameIntent.AssignConsortiumRole -> assignConsortiumRole(intent.projectId, intent.targetPlayerId, intent.targetPlayerName, intent.role)
            is GameIntent.ElectConsortiumRoles -> electConsortiumRoles(intent.projectId)
            is GameIntent.NudgeConsortiumPartner -> nudgeConsortiumPartner(intent.projectId, intent.slotId)
            is GameIntent.BroadcastConsortiumRadioSos -> broadcastConsortiumRadioSos(intent.projectId, intent.slotId)
            is GameIntent.OneTapDeliverToConsortium -> oneTapDeliverToConsortium(intent.projectId, intent.slotId)
            is GameIntent.FulfillConsortiumExportTender -> fulfillConsortiumExportTender(intent.projectId, intent.tenderId, intent.quantity)
            is GameIntent.SetConsortiumProductionStrategy -> setConsortiumProductionStrategy(intent.projectId, intent.strategy)
            is GameIntent.CreateConsortiumBoardProposal -> createConsortiumBoardProposal(intent.projectId, intent.titleTr, intent.titleEn, intent.descriptionTr, intent.descriptionEn, intent.proposalType, intent.proposedValue)
            is GameIntent.VoteOnConsortiumBoardProposal -> voteOnConsortiumBoardProposal(intent.projectId, intent.proposalId, intent.voteYes)
            is GameIntent.UpdateCompanyName -> updateCompanyName(intent.newName)
            is GameIntent.SignInAnonymously -> signInAnonymously(intent.onResult)
        }
    }

    internal val borsaEngine by lazy { com.example.data.BorsaEngine(repository, viewModelScope, this) }
    var macroState by mutableStateOf(com.example.data.MacroEconomyState())
    internal val _consortiumCrisisState = MutableStateFlow(com.example.data.ConsortiumCrisisState())
    val consortiumCrisisState: StateFlow<com.example.data.ConsortiumCrisisState> = _consortiumCrisisState.asStateFlow()

    internal val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    internal val _auctionLiability = MutableStateFlow(0L)
    val auctionLiability: StateFlow<Long> = _auctionLiability.asStateFlow()
    internal val _newsTickerMessage = MutableStateFlow("Piyasalar açıldı. Son dakika gelişmeleri bekleniyor...")
    val newsTickerMessage: StateFlow<String> = _newsTickerMessage.asStateFlow()


    internal val _productionProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    internal val _productionDurations = MutableStateFlow<Map<String, Long>>(emptyMap())
    internal val _productionSkip = MutableStateFlow<Set<String>>(emptySet())
    val productionProgress: StateFlow<Map<String, Float>> = _productionProgress.asStateFlow()
    val productionDurations: StateFlow<Map<String, Long>> = _productionDurations.asStateFlow()

    internal val _managers = MutableStateFlow<List<com.example.data.CompanyManager>>(
        com.example.data.getDefaultCompanyManagers()
    )
val managers: StateFlow<List<com.example.data.CompanyManager>> = _managers.asStateFlow()



    val isAutoProduceActive: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_prod" }?.let { mgr -> mgr.isHired && mgr.isActive } == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isProdManagerHired: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_prod" }?.isHired == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isAutoSellActive: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_logistics" }?.let { mgr -> mgr.isHired && mgr.isActive } == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isAutoBuyActive: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_borsa" }?.let { mgr -> mgr.isHired && mgr.isActive } == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isEngineerActive: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_maintenance" }?.let { mgr -> mgr.isHired && mgr.isActive } == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isEngineerHired: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_maintenance" }?.isHired == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isSalesExecActive: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_logistics" }?.let { mgr -> mgr.isHired && mgr.isActive } == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isSalesExecHired: StateFlow<Boolean> = _managers.map { list -> list.find { mgr -> mgr.id == "mgr_logistics" }?.isHired == true }.stateIn(viewModelScope, SharingStarted.Eagerly, false)


    // App Visual Theme & Language
    internal val _selectedTheme = MutableStateFlow("cyber_blue")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    fun setSelectedTheme(themeId: String) {
        _selectedTheme.value = themeId
        viewModelScope.launch {
            repository.saveSelectedTheme(themeId)
        }
    }

    internal val _selectedLanguage = MutableStateFlow("tr")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setSelectedLanguage(langCode: String) {
        _selectedLanguage.value = langCode
        viewModelScope.launch {
            repository.saveSelectedLanguage(langCode)
        }
    }

    // UI Mode: Always Expert (true)
    internal val _isExpertMode = MutableStateFlow(true)
    val isExpertMode: StateFlow<Boolean> = _isExpertMode.asStateFlow()

    fun setExpertMode(isExpert: Boolean) {
        _isExpertMode.value = true
        viewModelScope.launch {
            repository.saveUiMode(true)
        }
    }

    fun toggleExpertMode() {
        setExpertMode(true)
    }

    // R&D Tech State
    internal val _techGreenEnergy = MutableStateFlow(0)
    val techGreenEnergy: StateFlow<Int> = _techGreenEnergy.asStateFlow()

    internal val _techQualityControl = MutableStateFlow(0)
    val techQualityControl: StateFlow<Int> = _techQualityControl.asStateFlow()

    internal val _techLogistics = MutableStateFlow(0)
    val techLogistics: StateFlow<Int> = _techLogistics.asStateFlow()

    internal val _techAutomation = MutableStateFlow(0)
    val techAutomation: StateFlow<Int> = _techAutomation.asStateFlow()

    internal val _techQuantumAi = MutableStateFlow(0)
    val techQuantumAi: StateFlow<Int> = _techQuantumAi.asStateFlow()

    internal val _techNanotech = MutableStateFlow(0)
    val techNanotech: StateFlow<Int> = _techNanotech.asStateFlow()

    internal val _techCyberSecurity = MutableStateFlow(0)
    val techCyberSecurity: StateFlow<Int> = _techCyberSecurity.asStateFlow()

    internal val _techBiotechCloning = MutableStateFlow(0)
    val techBiotechCloning: StateFlow<Int> = _techBiotechCloning.asStateFlow()

    internal val _techAerospace = MutableStateFlow(0)
    val techAerospace: StateFlow<Int> = _techAerospace.asStateFlow()

    internal val _techHeavyIndustry = MutableStateFlow(0)
    val techHeavyIndustry: StateFlow<Int> = _techHeavyIndustry.asStateFlow()

    internal val _techConsumerGoods = MutableStateFlow(0)
    val techConsumerGoods: StateFlow<Int> = _techConsumerGoods.asStateFlow()

    internal val _techPetrochem = MutableStateFlow(0)
    val techPetrochem: StateFlow<Int> = _techPetrochem.asStateFlow()

    internal val _activeResearches = MutableStateFlow<Map<String, Long>>(emptyMap())
    val activeResearches: StateFlow<Map<String, Long>> = _activeResearches.asStateFlow()

    internal val _activeResearchTechKey = MutableStateFlow<String?>(null)
    val activeResearchTechKey: StateFlow<String?> = _activeResearchTechKey.asStateFlow()

    internal val _researchEndTimeMs = MutableStateFlow(0L)
    val researchEndTimeMs: StateFlow<Long> = _researchEndTimeMs.asStateFlow()

    internal val _researchRemainingMs = MutableStateFlow(0L)
    val researchRemainingMs: StateFlow<Long> = _researchRemainingMs.asStateFlow()

    data class FacilityPrerequisiteStatus(
        val isMet: Boolean,
        val reasonTr: String,
        val reasonEn: String,
        val requiredTechNode: TechNode? = null,
        val requiredTier: ProductTier? = null,
        val requiredLevel: Int = 0
    )

    fun getFacilityPrerequisites(product: Product): FacilityPrerequisiteStatus {
        // Tier 1: Temel Hammadde / Altyapı Tesisleri (Doğrudan kurulabilir)
        if (product.tier == ProductTier.TIER_1) {
            return FacilityPrerequisiteStatus(isMet = true, reasonTr = "", reasonEn = "")
        }

        val currentBusinesses = businesses.value

        // Tier 2: İleri Sanayi Tesisleri (Çelik, Tel, Cam, Tekstil, Pil, Plastik, Kereste vb.)
        // ŞART: En az 1 adet Seviye 2 Kademe 1 (Tier 1) altyapı tesisi olmalı
        if (product.tier == ProductTier.TIER_2) {
            val hasTier1Level2 = currentBusinesses.any { biz ->
                val p = Product.values().find { it.facilityId == biz.type || it.id == biz.type }
                p != null && p.tier == ProductTier.TIER_1 && biz.level >= 2
            }
            if (!hasTier1Level2) {
                return FacilityPrerequisiteStatus(
                    isMet = false,
                    reasonTr = "🔒 Gerekli: En az bir adet Seviye 2 Kademe 1 (Tier 1) altyapı tesisi",
                    reasonEn = "🔒 Required: At least one Level 2 Tier 1 facility",
                    requiredTier = ProductTier.TIER_1,
                    requiredLevel = 2
                )
            }
            return FacilityPrerequisiteStatus(isMet = true, reasonTr = "", reasonEn = "")
        }

        // Tier 3: Nihai Ürünler & Yüksek Teknoloji (Çip, Akıllı Telefon, Otomotiv, Makine, İlaç vb.)
        // ŞART 1: En az 1 adet Seviye 2 Kademe 2 (Tier 2) sanayi tesisi olmalı
        val hasTier2Level2 = currentBusinesses.any { biz ->
            val p = Product.values().find { it.facilityId == biz.type || it.id == biz.type }
            p != null && p.tier == ProductTier.TIER_2 && biz.level >= 2
        }
        if (!hasTier2Level2) {
            return FacilityPrerequisiteStatus(
                isMet = false,
                reasonTr = "🔒 Gerekli: En az bir adet Seviye 2 Kademe 2 (Tier 2) sanayi tesisi",
                reasonEn = "🔒 Required: At least one Level 2 Tier 2 facility",
                requiredTier = ProductTier.TIER_2,
                requiredLevel = 2
            )
        }

        // ŞART 2: İlgili Ar-Ge Araştırması Seviye 1 veya üzeri olmalı
        val techNode = TechTree.nodes.find { it.unlockedProductIds.contains(product.id) }
        if (techNode != null) {
            val techLevel = getTechLevel(techNode.id)
            if (techLevel < 1) {
                val techName = techNode.getName()
                return FacilityPrerequisiteStatus(
                    isMet = false,
                    reasonTr = "🔬 Ar-Ge Gerekli: $techName (Seviye 1)",
                    reasonEn = "🔬 R&D Required: $techName (Level 1)",
                    requiredTechNode = techNode
                )
            }
        }

        return FacilityPrerequisiteStatus(isMet = true, reasonTr = "", reasonEn = "")
    }

    fun isProductUnlocked(productId: String): Boolean {
        val product = Product.values().find { it.id == productId } ?: return true
        return getFacilityPrerequisites(product).isMet
    }

    
    fun calculateTechCost(techId: String, level: Int): Long {
        val fullId = if (techId.startsWith("tech_")) techId else "tech_$techId"
        val node = com.example.data.TechTree.nodes.find { it.id == fullId || it.id.removePrefix("tech_") == techId.removePrefix("tech_") } ?: return 1_000_000L
        val multiplier = when (level) {
            0 -> 1.0f
            1 -> 2.5f
            2 -> 6.0f
            3 -> 15.0f
            else -> 35.0f
        }
        val inflation = gameState.value?.globalInflationRate ?: 0f
        val finalMultiplier = multiplier * (1.0f + inflation)
        return (node.costMoney * finalMultiplier).toLong()
    }

    fun calculateTechGemCost(techId: String, level: Int): Int {
        val fullId = if (techId.startsWith("tech_")) techId else "tech_$techId"
        val node = com.example.data.TechTree.nodes.find { it.id == fullId || it.id.removePrefix("tech_") == techId.removePrefix("tech_") } ?: return 1
        val multiplier = when (level) {
            0 -> 1
            1 -> 2
            2 -> 4
            3 -> 8
            else -> 15
        }
        return node.costGems * multiplier
    }

    fun syncResearchStateFlowsAndMap(sourceMap: Map<String, Any?>? = null) {
        val currentMap = _researchLevels.value.toMutableMap()

        if (sourceMap != null) {
            sourceMap.forEach { (k, v) ->
                val lvl = when (v) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 0
                    else -> 0
                }
                if (lvl > 0) {
                    val base = k.removePrefix("tech_")
                    currentMap[base] = maxOf(currentMap[base] ?: 0, lvl).coerceAtMost(5)
                    currentMap["tech_$base"] = maxOf(currentMap["tech_$base"] ?: 0, lvl).coerceAtMost(5)
                }
            }
        }

        fun resolveLevel(keys: List<String>, currentFlowVal: Int): Int {
            val mapVal = keys.mapNotNull { currentMap[it] }.maxOrNull() ?: 0
            return maxOf(currentFlowVal, mapVal).coerceAtMost(5)
        }

        val greenLvl = resolveLevel(listOf("green_energy", "tech_green_energy"), _techGreenEnergy.value)
        val qualityLvl = resolveLevel(listOf("quality_control", "tech_quality_control"), _techQualityControl.value)
        val logisticsLvl = resolveLevel(listOf("logistics", "tech_logistics"), _techLogistics.value)
        val autoLvl = resolveLevel(listOf("automation", "tech_automation"), _techAutomation.value)
        val quantumLvl = resolveLevel(listOf("quantum_ai", "tech_quantum_ai"), _techQuantumAi.value)
        val nanoLvl = resolveLevel(listOf("nanotech", "tech_nanotech"), _techNanotech.value)
        val cyberLvl = resolveLevel(listOf("cyber_security", "tech_cyber_security"), _techCyberSecurity.value)
        val biotechLvl = resolveLevel(listOf("biotech_cloning", "tech_biotech_cloning", "biotech_med", "tech_biotech_med"), _techBiotechCloning.value)
        val aeroLvl = resolveLevel(listOf("aerospace", "tech_aerospace"), _techAerospace.value)
        val heavyLvl = resolveLevel(listOf("heavy_industry", "tech_heavy_industry"), _techHeavyIndustry.value)
        val consumerLvl = resolveLevel(listOf("consumer_goods", "tech_consumer_goods"), _techConsumerGoods.value)
        val petrochemLvl = resolveLevel(listOf("petrochem", "tech_petrochem"), _techPetrochem.value)

        _techGreenEnergy.value = greenLvl
        _techQualityControl.value = qualityLvl
        _techLogistics.value = logisticsLvl
        _techAutomation.value = autoLvl
        _techQuantumAi.value = quantumLvl
        _techNanotech.value = nanoLvl
        _techCyberSecurity.value = cyberLvl
        _techBiotechCloning.value = biotechLvl
        _techAerospace.value = aeroLvl
        _techHeavyIndustry.value = heavyLvl
        _techConsumerGoods.value = consumerLvl
        _techPetrochem.value = petrochemLvl

        currentMap["green_energy"] = greenLvl; currentMap["tech_green_energy"] = greenLvl
        currentMap["quality_control"] = qualityLvl; currentMap["tech_quality_control"] = qualityLvl
        currentMap["logistics"] = logisticsLvl; currentMap["tech_logistics"] = logisticsLvl
        currentMap["automation"] = autoLvl; currentMap["tech_automation"] = autoLvl
        currentMap["quantum_ai"] = quantumLvl; currentMap["tech_quantum_ai"] = quantumLvl
        currentMap["nanotech"] = nanoLvl; currentMap["tech_nanotech"] = nanoLvl
        currentMap["cyber_security"] = cyberLvl; currentMap["tech_cyber_security"] = cyberLvl
        currentMap["biotech_cloning"] = biotechLvl; currentMap["tech_biotech_cloning"] = biotechLvl
        currentMap["biotech_med"] = biotechLvl; currentMap["tech_biotech_med"] = biotechLvl
        currentMap["aerospace"] = aeroLvl; currentMap["tech_aerospace"] = aeroLvl
        currentMap["heavy_industry"] = heavyLvl; currentMap["tech_heavy_industry"] = heavyLvl
        currentMap["consumer_goods"] = consumerLvl; currentMap["tech_consumer_goods"] = consumerLvl
        currentMap["petrochem"] = petrochemLvl; currentMap["tech_petrochem"] = petrochemLvl

        _researchLevels.value = currentMap
        sanitizeActiveResearches()
    }

    private var isSanitizingResearches = false

    fun sanitizeActiveResearches() {
        if (isSanitizingResearches) return
        val current = _activeResearches.value
        if (current.isEmpty()) return
        val sanitized = mutableMapOf<String, Long>()
        val completedList = mutableListOf<String>()
        val now = System.currentTimeMillis()
        current.forEach { (k, v) ->
            val base = k.removePrefix("tech_")
            if (v > now) {
                // Ensure only 1 entry per base technology
                sanitized[base] = maxOf(sanitized[base] ?: 0L, v)
            } else if (v > 0L) {
                completedList.add(base)
            }
        }
        if (sanitized != current) {
            _activeResearches.value = sanitized
        }
        if (completedList.isNotEmpty()) {
            isSanitizingResearches = true
            try {
                completedList.distinct().forEach { base ->
                    completeResearch(base)
                }
            } finally {
                isSanitizingResearches = false
            }
        }
    }

    fun getTechLevel(techId: String): Int {
        val baseId = techId.removePrefix("tech_")
        val fullId = if (techId.startsWith("tech_")) techId else "tech_$techId"
        val levelFromVar = when (baseId) {
            "green_energy" -> _techGreenEnergy.value
            "aerospace" -> _techAerospace.value
            "quantum_ai" -> _techQuantumAi.value
            "nanotech" -> _techNanotech.value
            "biotech_cloning", "tech_biotech_med", "biotech_med" -> _techBiotechCloning.value
            "heavy_industry" -> _techHeavyIndustry.value
            "consumer_goods" -> _techConsumerGoods.value
            "petrochem" -> _techPetrochem.value
            "quality_control" -> _techQualityControl.value
            "logistics" -> _techLogistics.value
            "automation" -> _techAutomation.value
            "cyber_security" -> _techCyberSecurity.value
            else -> 0
        }
        val levelFromMap = _researchLevels.value[baseId] ?: _researchLevels.value[fullId] ?: 0
        return maxOf(levelFromVar, levelFromMap).coerceAtMost(5)
    }

    /**
     * Kademeli kilit sistemi denetleyicisi: Belirtilen özelliğin açık olup olmadığını döner.
     */
    fun isFeatureUnlocked(feature: com.example.data.GameFeature): Boolean {
        val lvl = player.value?.level ?: 1
        return com.example.data.FeatureLockManager.isUnlocked(feature, lvl, _researchLevels.value)
    }

    /**
     * Belirtilen özelliğin kilit durumu ve açılma gereksinimlerini döner.
     */
    fun getFeatureLockInfo(feature: com.example.data.GameFeature): com.example.data.FeatureLockInfo {
        val lvl = player.value?.level ?: 1
        return com.example.data.FeatureLockManager.getLockInfo(feature, lvl, _researchLevels.value)
    }

    /**
     * Tüm özelliklerin kilit detay listesini döner.
     */
    fun getAllFeaturesLockInfo(): List<com.example.data.FeatureLockInfo> {
        val lvl = player.value?.level ?: 1
        return com.example.data.FeatureLockManager.getAllFeaturesLockInfo(lvl, _researchLevels.value)
    }

    /**
     * Akıllı Danışman (Smart Advisor) güncel önerisini döner.
     */
    fun getAdvisorRecommendation(): com.example.data.AdvisorRecommendation? {
        val p = player.value
        val lvl = p?.level ?: 1
        val money = p?.money ?: 0L
        val depositBalance = p?.depositBalance ?: 0L
        val currentCity = p?.currentCity ?: "istanbul"
        val bizList = businesses.value
        val invList = inventory.value
        val prods = _activeProductions.value
        val resMap = _researchLevels.value
        val activeRes = _activeResearches.value

        return com.example.data.SmartAdvisorManager.analyzeAndRecommend(
            playerLevel = lvl,
            money = money,
            businesses = bizList,
            inventoryItems = invList,
            activeProductions = prods,
            researchLevels = resMap,
            activeResearches = activeRes,
            depositBalance = depositBalance,
            currentCityId = currentCity
        )
    }

    fun startResearch(techId: String, cost: Long? = null, spendGems: Boolean? = null) {
        val p = player.value ?: return
        val fullId = if (techId.startsWith("tech_")) techId else "tech_$techId"
        val baseId = fullId.removePrefix("tech_")
        
        val currentLevel = getTechLevel(baseId)

        if (currentLevel >= 5) {
            SmartNotificationManager.show("Bu teknoloji maksimum seviyeye (5) ulaştı!", "This technology reached max level (5)!", NotificationType.ALERT)
            return
        }

        // Sanitize active researches map (remove tech_ duplicates and expired ones)
        val cleanActiveMap = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()
        _activeResearches.value.forEach { (k, v) ->
            val base = k.removePrefix("tech_")
            if (v > now) {
                cleanActiveMap[base] = maxOf(cleanActiveMap[base] ?: 0L, v)
            }
        }

        if (cleanActiveMap.containsKey(baseId)) {
            SmartNotificationManager.show("Bu teknoloji için zaten devam eden bir araştırma var!", "There is already an ongoing research for this tech!", NotificationType.ALERT)
            return
        }

        if (cleanActiveMap.size >= 4) {
            SmartNotificationManager.show("Maksimum eşzamanlı araştırma sınırına (4) ulaşıldı!", "Max concurrent research limit (4) reached!", NotificationType.ALERT)
            return
        }

        val node = com.example.data.TechTree.nodes.find { it.id == techId || it.id == fullId || it.id.removePrefix("tech_") == baseId }
        val realCost = cost ?: calculateTechCost(baseId, currentLevel)
        val realGemCost = calculateTechGemCost(baseId, currentLevel)

        // Initial research (Level 0) requires gems. Level >= 1 upgrades require cash only.
        val requiresGems = spendGems ?: (currentLevel == 0)

        if (p.money < realCost) {
            SmartNotificationManager.show("Yetersiz Sermaye! Gerekli: ₳${com.example.ui.components.formatMoney(realCost)}", "Insufficient Capital! Required: ₳${com.example.ui.components.formatMoney(realCost)}", NotificationType.ALERT)
            return
        }

        if (requiresGems && p.gems < realGemCost) {
            SmartNotificationManager.show("Yetersiz Elmas! Araştırmayı başlatmak için $realGemCost 💎 gerekli.", "Insufficient Gems! $realGemCost 💎 required to start research.", NotificationType.ALERT)
            return
        }

        val actualGemDeduction = if (requiresGems) realGemCost else 0
        val durationMs = node?.durationMs ?: 14_400_000L
        val endTime = System.currentTimeMillis() + durationMs
        // Each research occupies EXACTLY ONE slot
        cleanActiveMap[baseId] = endTime
        val updatedMap = cleanActiveMap

        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money - realCost, gems = p.gems - actualGemDeduction))
            _activeResearches.value = updatedMap
            // Update legacy state for compatibility
            _activeResearchTechKey.value = baseId
            _researchEndTimeMs.value = endTime
            _researchRemainingMs.value = durationMs
            
            syncResearchStateFlowsAndMap()
            saveEconomicDataToDataStore(immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
            val techName = node?.getName() ?: baseId.uppercase()
            val isEn = _selectedLanguage.value == "en"
            val gemMsg = if (requiresGems) " + $realGemCost 💎" else if (isEn) " (0 💎 • Cash Only)" else " (0 💎 • Sadece Nakit)"
            val msg = if (isEn) "🔬 R&D Research Started: $techName (Lvl ${currentLevel + 1}) • ₳${com.example.ui.components.formatMoney(realCost)}$gemMsg" else "🔬 Ar-Ge Araştırması Başlatıldı: $techName (Lvl ${currentLevel + 1}) • ₳${com.example.ui.components.formatMoney(realCost)}$gemMsg"
            SmartNotificationManager.show(msg, NotificationType.SUCCESS)
        }
    }

    fun speedUpResearch(techId: String) {
        val p = player.value ?: return
        val baseId = techId.removePrefix("tech_")
        val endTime = _activeResearches.value[baseId] ?: _activeResearches.value["tech_$baseId"] ?: return
        val remainingMs = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
        val gemCost = kotlin.math.ceil(remainingMs / 3600_000.0).toInt().coerceAtLeast(1)

        if (p.gems < gemCost) {
            SmartNotificationManager.show("Yetersiz Elmas! Gerekli: $gemCost 💎", "Insufficient Gems! Required: $gemCost 💎", NotificationType.ALERT)
            return
        }

        viewModelScope.launch {
            val updatedPlayer = p.copy(gems = p.gems - gemCost)
            repository.updatePlayer(updatedPlayer)
            completeResearch(baseId)
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
            }
            val isEn = _selectedLanguage.value == "en"
            val msg = if (isEn) "⚡ Research Sped Up for $gemCost Gems! 🚀" else "⚡ Araştırma $gemCost Elmas Harcanarak Hızlandırıldı! 🚀"
            SmartNotificationManager.show(msg, NotificationType.SUCCESS)
        }
    }

    fun speedUpResearchWithGems() {
        val firstKey = _activeResearches.value.keys.firstOrNull() ?: return
        speedUpResearch(firstKey)
    }

    fun speedUpActiveResearchWithGems() {
        speedUpResearchWithGems()
    }

    fun skipResearchWithGems(techId: String) {
        speedUpResearch(techId)
    }

    fun skipProductionWithGems(productId: String) {
        val p = player.value ?: return
        val currentProg = _productionProgress.value[productId] ?: 0f
        val totalDur = _productionDurations.value[productId] ?: 0L
        val remainingTimeMs = ((1f - currentProg) * totalDur).toLong()
        val gemCost = kotlin.math.max(1, (remainingTimeMs / 3_600_000L).toInt())

        if (p.gems < gemCost) {
            SmartNotificationManager.show("Yetersiz Elmas! Gerekli: $gemCost 💎", "Insufficient Gems! Required: $gemCost 💎", NotificationType.ALERT)
            return
        }

        viewModelScope.launch {
            val updatedPlayer = p.copy(gems = p.gems - gemCost)
            repository.updatePlayer(updatedPlayer)
            _productionSkip.update { it + productId }
            val existingProd = _activeProductions.value.find { it.productId == productId }
            if (existingProd != null) {
                completeActiveProduction(existingProd, isOffline = false, isSilent = false, currentPlayerState = updatedPlayer)
            }
            saveEconomicDataToDataStore(immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
            val isEn = _selectedLanguage.value == "en"
            val msg = if (isEn) "⚡ Production Sped Up for $gemCost Gems! 🚀" else "⚡ Üretim $gemCost Elmas Harcanarak Hızlandırıldı! 🚀"
            SmartNotificationManager.show(msg, NotificationType.SUCCESS)
        }
    }

    fun completeResearch(techId: String) {
        val baseId = techId.removePrefix("tech_")
        val newLvl = (getTechLevel(baseId) + 1).coerceAtMost(5)
        when (baseId) {
            "green_energy" -> _techGreenEnergy.value = newLvl
            "aerospace" -> _techAerospace.value = newLvl
            "quantum_ai" -> _techQuantumAi.value = newLvl
            "nanotech" -> _techNanotech.value = newLvl
            "biotech_cloning", "biotech_med", "tech_biotech_med" -> _techBiotechCloning.value = newLvl
            "heavy_industry" -> _techHeavyIndustry.value = newLvl
            "consumer_goods" -> _techConsumerGoods.value = newLvl
            "petrochem" -> _techPetrochem.value = newLvl
            "quality_control" -> _techQualityControl.value = newLvl
            "logistics" -> _techLogistics.value = newLvl
            "automation" -> _techAutomation.value = newLvl
            "cyber_security" -> _techCyberSecurity.value = newLvl
            else -> {
                val levelsMap = _researchLevels.value.toMutableMap()
                levelsMap[baseId] = newLvl
                levelsMap["tech_$baseId"] = newLvl
                _researchLevels.value = levelsMap
            }
        }
        val updatedMap = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()
        _activeResearches.value.forEach { (k, v) ->
            val b = k.removePrefix("tech_")
            if (b != baseId && v > now) {
                updatedMap[b] = v
            }
        }
        _activeResearches.value = updatedMap

        if (_activeResearches.value.isEmpty()) {
            _activeResearchTechKey.value = null
            _researchEndTimeMs.value = 0L
            _researchRemainingMs.value = 0L
        } else {
            val first = _activeResearches.value.entries.first()
            _activeResearchTechKey.value = first.key.removePrefix("tech_")
            _researchEndTimeMs.value = first.value
            _researchRemainingMs.value = (first.value - System.currentTimeMillis()).coerceAtLeast(0L)
        }

        syncResearchStateFlowsAndMap()

        val node = TechTree.nodes.find { it.id == techId || it.id == "tech_$techId" || it.id.removePrefix("tech_") == baseId }

        viewModelScope.launch {
            saveEconomicDataToDataStore(immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
            val techName = node?.getName() ?: baseId
            val isEn = _selectedLanguage.value == "en"
            val msg = if (isEn) "🔬 R&D Completed! $techName (Level $newLvl / 5)" else "🔬 Ar-Ge Tamamlandı! $techName (Seviye $newLvl / 5)"
            SmartNotificationManager.show(msg, NotificationType.SUCCESS)
        }
    }

    fun completeActiveResearch() {
        val firstKey = _activeResearches.value.keys.firstOrNull() ?: return
        completeResearch(firstKey)
    }

    // IPO & Holding State
    internal val _isIpoActive = MutableStateFlow(false)
    val isIpoActive: StateFlow<Boolean> = _isIpoActive.asStateFlow()

    internal val _publicSharePercent = MutableStateFlow(0)
    val publicSharePercent: StateFlow<Int> = _publicSharePercent.asStateFlow()

    internal val _totalDividendsPaid = MutableStateFlow(0L)
    val totalDividendsPaid: StateFlow<Long> = _totalDividendsPaid.asStateFlow()
    
    internal val _isOnlineRegistered = MutableStateFlow(false)
    val isOnlineRegistered: StateFlow<Boolean> = _isOnlineRegistered.asStateFlow()
    
    internal val _onlineEmail = MutableStateFlow("")
    val onlineEmail: StateFlow<String> = _onlineEmail.asStateFlow()

    val isGoogleSignedIn: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(_isOnlineRegistered, _onlineEmail) { registered, email ->
        registered && email.isNotBlank() && email != "misafir_tuccar" && !email.startsWith("guest") && (email.contains("@") || email.endsWith(".com"))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun isUserGoogleSignedIn(): Boolean {
        val reg = _isOnlineRegistered.value
        val email = _onlineEmail.value
        return reg && email.isNotBlank() && email != "misafir_tuccar" && !email.startsWith("guest") && (email.contains("@") || email.endsWith(".com"))
    }

    internal var isRestoringFromCloud = false

    val player: StateFlow<PlayerEntity?> = repository.player.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val inventory: StateFlow<List<InventoryEntity>> = repository.inventory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val businesses: StateFlow<List<BusinessEntity>> = repository.businesses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val marketPrices: StateFlow<List<MarketPriceEntity>> = repository.marketPrices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val gameState: StateFlow<GameStateEntity?> = repository.gameState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val economicSnapshot: Flow<EconomicSnapshot?> = repository.economicSnapshotFlow

    internal val _activeCityEvents = MutableStateFlow<List<com.example.data.CityMarketEvent>>(com.example.data.CityNewsEventManager.generateInitialEvents())
    val activeCityEvents: StateFlow<List<com.example.data.CityMarketEvent>> = _activeCityEvents.asStateFlow()

    internal val _marketListings = MutableStateFlow<List<com.example.data.MarketListing>>(emptyList())
    val marketListings: StateFlow<List<com.example.data.MarketListing>> = _marketListings.asStateFlow()

    internal val _buyOrders = MutableStateFlow<List<com.example.data.BuyOrder>>(emptyList())
    val buyOrders: StateFlow<List<com.example.data.BuyOrder>> = _buyOrders.asStateFlow()

    internal val _futuresContracts = MutableStateFlow<List<com.example.data.FuturesContract>>(emptyList())
    val futuresContracts: StateFlow<List<com.example.data.FuturesContract>> = _futuresContracts.asStateFlow()

    internal val _megaProjects = MutableStateFlow<List<com.example.data.MegaProject>>(emptyList())
    internal val _marketPriceHistory = ConcurrentHashMap<String, CopyOnWriteArrayList<Pair<Long, Long>>>()
    val megaProjects: StateFlow<List<com.example.data.MegaProject>> = _megaProjects.asStateFlow()

    internal val _consortiumChatMessages = MutableStateFlow<Map<String, List<com.example.data.ConsortiumChatMessage>>>(emptyMap())
    val consortiumChatMessages: StateFlow<Map<String, List<com.example.data.ConsortiumChatMessage>>> = _consortiumChatMessages.asStateFlow()
    internal val consortiumChatListeners = ConcurrentHashMap<String, Any?>()

    internal val _unreadChatProjects = MutableStateFlow<Set<String>>(emptySet())
    val unreadChatProjects: StateFlow<Set<String>> = _unreadChatProjects.asStateFlow()

    val reservedInventory: StateFlow<Map<String, Int>> = combine(
        _futuresContracts,
        _buyOrders
    ) { contracts, orders ->
        val reservedMap = mutableMapOf<String, Int>()
        for (contract in contracts) {
            if (!contract.isFulfilled) {
                reservedMap[contract.itemId] = (reservedMap[contract.itemId] ?: 0) + contract.quantity
            }
        }
        for (order in orders) {
            reservedMap[order.itemId] = (reservedMap[order.itemId] ?: 0) + order.quantity
        }
        reservedMap.toMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun addFuturesContract(itemId: String, quantity: Int, lockedPricePerUnit: Long, durationDays: Int): Boolean {
        val p = player.value ?: return false
        val uid = if (_onlineEmail.value.isNotBlank()) _onlineEmail.value.replace(".", "_") else if (p.id.isNotBlank() && p.id != "local_player") p.id else "trader_${p.name.hashCode()}"
        val totalCost = quantity * lockedPricePerUnit
        val commission = (totalCost * 0.015).toLong()
        if (p.money < totalCost + commission) return false

        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money - totalCost - commission))
            val rawId = java.util.UUID.randomUUID().toString()
            val newContractId = if (rawId.startsWith("fc_")) rawId else "fc_$rawId"
            val newContract = com.example.data.FuturesContract(
                id = newContractId,
                creatorName = p.name,
                creatorId = uid,
                itemId = itemId,
                quantity = quantity,
                lockedPricePerUnit = lockedPricePerUnit,
                durationDays = durationDays,
                cityId = p.currentCity
            )
            _futuresContracts.value = _futuresContracts.value + newContract

            // Supabase ve Realtime WebSocket Canlı Yayını
            repository.syncFuturesContractToSupabase(newContract)
            com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                com.example.data.network.LiveMarketActionEventDto(
                    actionType = "FUTURES_CREATED",
                    id = newContractId,
                    playerId = uid,
                    playerName = p.name,
                    itemId = itemId,
                    quantity = quantity,
                    price = lockedPricePerUnit,
                    cityId = p.currentCity,
                    durationDays = durationDays
                )
            )

            val prodName = com.example.data.Product.values().find { it.id == itemId }?.getDisplayName() ?: itemId
            logManagerAction("mgr_contracts", "Vadeli B2B Sözleşmesi oluşturuldu ($quantity Ton $prodName)", -(totalCost + commission))
            logManagerAction("mgr_hr", "İnsan Kaynakları: Manuel Vadeli Sözleşme işlemi denetlendi ve onaylandı.", -(totalCost + commission))
            com.example.ui.components.SmartNotificationManager.show("$durationDays Günlük Vadeli B2B Sözleşme imzalandı ve bulut ağına yayınlandı!", "$durationDays-Day B2B Futures Contract signed and published to cloud network!", com.example.ui.components.NotificationType.SUCCESS)
            saveEconomicDataToDataStore(immediate = true)
        }
        return true
    }

    fun cancelFuturesContract(contractId: String): Boolean {
        val p = player.value ?: return false
        val contract = _futuresContracts.value.find { it.id == contractId } ?: return false
        val refund = contract.quantity * contract.lockedPricePerUnit

        viewModelScope.launch {
            _futuresContracts.value = _futuresContracts.value.filter { it.id != contractId }
            repository.updatePlayer(p.copy(money = p.money + refund))

            // Supabase ve Realtime WebSocket Canlı Yayını
            repository.deleteFuturesContractFromSupabase(contractId)
            com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                com.example.data.network.LiveMarketActionEventDto(
                    actionType = "FUTURES_CANCELLED",
                    id = contractId,
                    playerId = p.id,
                    playerName = p.name,
                    itemId = contract.itemId,
                    quantity = contract.quantity,
                    price = contract.lockedPricePerUnit,
                    cityId = contract.cityId,
                    durationDays = contract.durationDays
                )
            )

            val prodName = com.example.data.Product.values().find { it.id == contract.itemId }?.getDisplayName() ?: contract.itemId
            logManagerAction("mgr_contracts", "Vadeli B2B Sözleşmesi iptal edildi ($contract.quantity Ton $prodName, Teminat İadesi)", refund)
            logManagerAction("mgr_hr", "İnsan Kaynakları: İptal edilen sözleşmenin teminat iadesi işlendi.", refund)
            com.example.ui.components.SmartNotificationManager.show("Vadeli sözleşme iptal edildi ve ₳${com.example.ui.components.formatMoney(refund)} iade alındı.", "Futures contract canceled and ₳${com.example.ui.components.formatMoney(refund)} refunded.", com.example.ui.components.NotificationType.INFO)
        }
        return true
    }

    fun fulfillFuturesContract(contractId: String) {
        val p = player.value ?: return
        val contract = _futuresContracts.value.find { it.id == contractId } ?: return
        val invItem = inventory.value.find { it.itemId == contract.itemId }

        if (invItem == null || invItem.quantity < contract.quantity) {
            com.example.ui.components.SmartNotificationManager.show("Deponuzda teslimat için yeterli (${contract.quantity} Ton) ürün yok!", "Not enough product in your warehouse for delivery (${contract.quantity} Tons)!", com.example.ui.components.NotificationType.ALERT)
            return
        }

        viewModelScope.launch {
            repository.consumeItem(contract.itemId, contract.quantity)

            val totalRevenue = contract.quantity * contract.lockedPricePerUnit
            val stateTax = (totalRevenue * 0.03f).toLong()
            val netEarnings = totalRevenue - stateTax

            val hrMgr = _managers.value.find { it.id == "mgr_hr" }
            val hrBonus = if (hrMgr != null && hrMgr.isHired && hrMgr.isActive) (netEarnings * 0.02f).toLong() else 0L
            val finalEarnings = netEarnings + hrBonus

            repository.updatePlayer(p.copy(money = p.money + finalEarnings))

            _futuresContracts.value = _futuresContracts.value.filter { it.id != contractId }

            // Supabase ve Realtime WebSocket Canlı Yayını
            repository.deleteFuturesContractFromSupabase(contractId)
            com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                com.example.data.network.LiveMarketActionEventDto(
                    actionType = "FUTURES_FULFILLED",
                    id = contractId,
                    playerId = p.id,
                    playerName = p.name,
                    itemId = contract.itemId,
                    quantity = contract.quantity,
                    price = contract.lockedPricePerUnit,
                    cityId = contract.cityId,
                    durationDays = contract.durationDays
                )
            )

            if (contract.creatorId.isNotEmpty() && contract.creatorId != "local" && contract.creatorId != p.id) {
                
            } else if (contract.creatorId == p.id || contract.creatorName == p.name) {
                repository.insertInventory(com.example.data.InventoryEntity(contract.itemId, (inventory.value.find { it.itemId == contract.itemId }?.quantity ?: 0) + contract.quantity))
            }

            val prodName = com.example.data.Product.values().find { it.id == contract.itemId }?.getDisplayName() ?: contract.itemId
            logManagerAction("mgr_contracts", "Vadeli B2B Sözleşmesi teslim edildi ($prodName -> +₳${com.example.ui.components.formatMoney(finalEarnings)})", finalEarnings)
            logManagerAction("mgr_hr", "İnsan Kaynakları: Manuel Vadeli B2B işlemi denetlendi ve vergilendirildi.", finalEarnings)

            val bonusMsg = if (hrBonus > 0) " (İK Müdürü Verim Primi: +₳${com.example.ui.components.formatMoney(hrBonus)})" else ""
            com.example.ui.components.SmartNotificationManager.show("Sözleşme teslim edildi! +₳${com.example.ui.components.formatMoney(finalEarnings)}$bonusMsg", "Contract delivered! +₳${com.example.ui.components.formatMoney(finalEarnings)}$bonusMsg", com.example.ui.components.NotificationType.SUCCESS)
        }
    }

    fun getDynamicPriceMultiplier(itemId: String): Float {
        val bOrders = buyOrders.value.count { it.itemId == itemId }
        val sListings = marketListings.value.count { it.itemId == itemId }
        if (sListings == 0) return 1.30f
        val ratio = bOrders.toFloat() / sListings.toFloat()
        return (0.80f + (ratio * 0.25f)).coerceIn(0.70f, 1.50f)
    }

    internal val _auctions = MutableStateFlow<List<com.example.data.Auction>>(emptyList())
    val auctions: StateFlow<List<com.example.data.Auction>> = _auctions.asStateFlow()

    internal val _priceHistory = MutableStateFlow<Map<String, List<Long>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Long>>> = _priceHistory.asStateFlow()

    internal val _researchLevels = MutableStateFlow<Map<String, Int>>(
        mapOf(
            "battery_tech" to 0,
            "cyber_automation" to 0,
            "biotech_synthesis" to 0,
            "green_energy" to 0,
            "quantum_logistics" to 0
        )
    )
    val researchLevels: StateFlow<Map<String, Int>> = _researchLevels.asStateFlow()

    internal val _outbidAlert = MutableStateFlow<com.example.data.OutbidAlertData?>(null)
    val outbidAlert: StateFlow<com.example.data.OutbidAlertData?> = _outbidAlert.asStateFlow()

    fun dismissOutbidAlert() {
        _outbidAlert.value = null
    }

    internal val _guilds = MutableStateFlow<List<com.example.data.GuildGroup>>(emptyList())
    val guilds: StateFlow<List<com.example.data.GuildGroup>> = combine(
        MultiplayerManager.globalGuilds,
        MultiplayerManager.onlinePlayers,
        _isIpoActive,
        player,
        _publicSharePercent
    ) { globalGuilds, onlinePlayers, ipoActive, p, sharePct ->
        val list = mutableListOf<com.example.data.GuildGroup>()
        
        // 1. Supabase / Sunucudan gelen tüm Canlı Konsorsiyumları & IPO Şirketlerini Ekle
        globalGuilds.forEach { gg ->
            list.add(gg)
        }
        
        // 2. Çevrimiçi Oyuncuların Holding Yapılarını Ekle
        onlinePlayers.forEach { op ->
            val valuation = op.netWorth.coerceAtLeast(1_000_000L)
            val opId = "online_player_${op.id}"
            if (list.none { it.id == opId }) {
                list.add(
                    com.example.data.GuildGroup(
                        id = opId,
                        name = op.companyName.ifEmpty { "${op.name} Holding A.Ş." },
                        leaderName = "${op.name} (${op.city}) • Seviye ${op.level}",
                        memberCount = op.level,
                        megaProjectTitle = "Piyasa Değerlemesi (${op.badge})",
                        megaProjectTarget = valuation,
                        megaProjectCurrent = valuation,
                        perkDescription = "Net Varlık: ₳${com.example.ui.components.formatMoney(op.netWorth)}",
                        isIpoActive = true,
                        bankBalance = (valuation * 0.25).toLong()
                    )
                )
            }
        }

        // 3. Varsayılan BIST Kote Şirketler
        val defaultList = listOf(
            com.example.data.GuildGroup(
                id = "default_p1",
                name = "Ahmet Yılmaz Holding A.Ş.",
                leaderName = "Ahmet Yılmaz (İstanbul) • Seviye 18",
                memberCount = 18,
                megaProjectTitle = "BIST Lideri",
                megaProjectTarget = 15_200_000L,
                megaProjectCurrent = 15_200_000L,
                perkDescription = "Net Varlık: ₳15,200,000",
                isIpoActive = true,
                bankBalance = 5_000_000L
            ),
            com.example.data.GuildGroup(
                id = "default_p2",
                name = "Zeynep Şahin Teknoloji A.Ş.",
                leaderName = "Zeynep Şahin (Ankara) • Seviye 14",
                memberCount = 14,
                megaProjectTitle = "Teknoloji Devi",
                megaProjectTarget = 11_400_000L,
                megaProjectCurrent = 11_400_000L,
                perkDescription = "Net Varlık: ₳11,400,000",
                isIpoActive = true,
                bankBalance = 3_500_000L
            ),
            com.example.data.GuildGroup(
                id = "default_p3",
                name = "Anadolu Enerji & Madencilik A.Ş.",
                leaderName = "Mehmet Demir (İzmir) • Seviye 22",
                memberCount = 22,
                megaProjectTitle = "Enerji Devi",
                megaProjectTarget = 24_800_000L,
                megaProjectCurrent = 24_800_000L,
                perkDescription = "Net Varlık: ₳24,800,000",
                isIpoActive = true,
                bankBalance = 8_200_000L
            )
        )

        defaultList.forEach { def ->
            if (list.none { it.id == def.id }) {
                list.add(def)
            }
        }

        // 4. Yerel Oyuncunun Halka Açık Holdingi Aktifse Listenin En Başına Ekle
        if (ipoActive && p != null) {
            val valuation = calculateCompanyValuation()
            val holdingId = "player_holding_${p.id}"
            val existingIndex = list.indexOfFirst { it.id == holdingId }
            val playerGuild = com.example.data.GuildGroup(
                id = holdingId,
                name = "${p.name} Holding A.Ş.",
                leaderName = "${p.name} (Siz) • Seviye ${p.level}",
                memberCount = p.level,
                megaProjectTitle = "Oyuncu Holding Mega Yatırımı",
                megaProjectTarget = valuation,
                megaProjectCurrent = valuation,
                perkDescription = "%$sharePct Halka Açık • BIST Kote",
                isIpoActive = true,
                publicSharePercent = sharePct
            )
            if (existingIndex >= 0) {
                list[existingIndex] = playerGuild
            } else {
                list.add(0, playerGuild)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun unlockResearchTech(techId: String, cost: Long): Boolean {
        val p = player.value ?: return false
        if (p.money < cost) {
            SmartNotificationManager.show("Yetersiz Nakit Bakiye! (Gerekli: ₳${com.example.ui.components.formatMoney(cost)})", NotificationType.ALERT)
            return false
        }
        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money - cost))
            val baseId = techId.removePrefix("tech_")
            val current = _researchLevels.value.toMutableMap()
            current[baseId] = 1
            current["tech_$baseId"] = 1
            _researchLevels.value = current
            syncResearchStateFlowsAndMap()
            saveEconomicDataToDataStore(immediate = true)
            
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
            SmartNotificationManager.show("🔬 AR-GE Araştırması Tamamlandı! Yeni teknoloji ve imkanlar açıldı.", NotificationType.SUCCESS)
        }
        return true
    }

    fun contributeToGuildProject(guildId: String, amount: Long): Boolean {
        val p = player.value ?: return false
        if (p.money < amount) {
            SmartNotificationManager.show("Yetersiz Bakiye! (Gerekli: ₳${com.example.ui.components.formatMoney(amount)})", NotificationType.ALERT)
            return false
        }
        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money - amount))
            val current = _guilds.value.toMutableList()
            val index = current.indexOfFirst { it.id == guildId }
            if (index != -1) {
                val g = current[index]
                val updated = g.copy(
                    megaProjectCurrent = (g.megaProjectCurrent + amount).coerceAtMost(g.megaProjectTarget),
                    isJoined = true
                )
                current[index] = updated
                _guilds.value = current
                
                val uid = if (p.id.isNotBlank()) p.id else "local"
                if (uid != null) {
                    /* removed guild update */
                }
            }
            SmartNotificationManager.show("🏢 Sendika Mega Projesine ₳${com.example.ui.components.formatMoney(amount)} katkı sağlandı!", NotificationType.SUCCESS)
        }
        return true
    }

    fun contributeToTender(guildId: String, itemId: String, quantity: Int): Boolean {
        val currentStock = inventory.value.find { it.itemId == itemId }?.quantity ?: 0
        if (currentStock < quantity) {
            SmartNotificationManager.show("Yetersiz envanter! Deponuzda yeterli $itemId yok.", NotificationType.ALERT)
            return false
        }

        viewModelScope.launch {
            // 1. Client-Side Prediction: Anında yerel envanter düşüşü ve iyimser UI güncellemesi
            repository.consumeItem(itemId, quantity)

            val p = player.value
            val originCityId = if (p?.currentCity?.isNotBlank() == true) p.currentCity else "istanbul"
            val targetGuild = _guilds.value.find { it.id == guildId }
            val consortiumCity = targetGuild?.leaderName?.let { leader ->
                val match = Regex("""\(([^)]+)\)""").find(leader)
                match?.groupValues?.get(1)?.lowercase()
            } ?: "istanbul"
            val durationMs = calculateLogisticsDuration(originCityId, consortiumCity)
            addActiveDelivery(
                com.example.data.DeliveryItem(
                    itemId = itemId,
                    quantity = quantity,
                    originCityId = originCityId,
                    destinationCityId = consortiumCity,
                    pricePerUnit = 0L,
                    totalCost = 0L,
                    startTimeMs = System.currentTimeMillis(),
                    totalDurationMs = durationMs,
                    isOutboundSale = true,
                    isConsortiumDelivery = true
                )
            )

            com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
            SmartNotificationManager.show("🚚 $quantity Ton $itemId Konsorsiyum Deposuna (${consortiumCity.uppercase()}) sevk edildi!", NotificationType.SUCCESS)

            // 2. Arka Planda Sunucu (Supabase) Senkronizasyonu
            val success = MultiplayerManager.contributeToTender(guildId, itemId, quantity)
            if (!success) {
                // 3. Rollback Mekanizması: Sunucu başarısız olursa envanteri eski haline döndür
                repository.produceItem(itemId, quantity)
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.ERROR)
                SmartNotificationManager.show("⚠️ Bağış sunucu senkronizasyonunda hata oluştu! İade yapıldı.", NotificationType.ALERT)
            }
        }
        return true
    }

    fun deliverToConsortiumSlot(
        projectId: String,
        slotId: String,
        productId: String,
        quantity: Int,
        partnerId: String,
        partnerName: String
    ): Boolean {
        val currentStock = inventory.value.find { it.itemId == productId }?.quantity ?: 0
        if (currentStock < quantity) {
            SmartNotificationManager.show("Yetersiz envanter! Deponuzda yeterli $productId yok.", NotificationType.ALERT)
            return false
        }

        viewModelScope.launch {
            // 1. Client-Side Prediction: Anında yerel stok düşüşü
            repository.consumeItem(productId, quantity)

            val p = player.value
            val originCityId = if (p?.currentCity?.isNotBlank() == true) p.currentCity else "istanbul"
            val targetGuild = _guilds.value.find { it.id == projectId }
            val consortiumCity = targetGuild?.leaderName?.let { leader ->
                val match = Regex("""\(([^)]+)\)""").find(leader)
                match?.groupValues?.get(1)?.lowercase()
            } ?: "istanbul"
            val targetMegaProject = _megaProjects.value.find { it.id == projectId }
            val logisticsMultiplier = targetMegaProject?.logisticsTransitSpeedMultiplier ?: 1.0f
            val baseDurationMs = calculateLogisticsDuration(originCityId, consortiumCity)
            val durationMs = (baseDurationMs * logisticsMultiplier).toLong().coerceAtLeast(4_000L)
            addActiveDelivery(
                com.example.data.DeliveryItem(
                    itemId = productId,
                    quantity = quantity,
                    originCityId = originCityId,
                    destinationCityId = consortiumCity,
                    pricePerUnit = 0L,
                    totalCost = 0L,
                    startTimeMs = System.currentTimeMillis(),
                    totalDurationMs = durationMs,
                    isOutboundSale = true,
                    isConsortiumDelivery = true
                )
            )

            com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
            val speedNotice = if (logisticsMultiplier < 1.0f) " (Lojistik Sorumlusu %20 Hız Bonusu)" else ""
            SmartNotificationManager.show("🚚 $quantity Ton $productId Konsorsiyum Deposuna (${consortiumCity.uppercase()}) sevk edildi!$speedNotice", NotificationType.SUCCESS)

            // 2. Arka Planda Sunucu (Supabase) Senkronizasyonu
            val success = MultiplayerManager.deliverToConsortiumSlot(projectId, slotId, productId, quantity, partnerId, partnerName)
            if (!success) {
                // 3. Rollback Mekanizması
                repository.produceItem(productId, quantity)
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.ERROR)
                SmartNotificationManager.show("⚠️ Teslimat sunucu hatası nedeniyle iptal edildi ve ürünler iade edildi.", NotificationType.ALERT)
            }
        }
        return true
    }


    internal val _activeDeliveries = MutableStateFlow<List<com.example.data.DeliveryItem>>(emptyList())
    val activeDeliveries: StateFlow<List<com.example.data.DeliveryItem>> = _activeDeliveries.asStateFlow()

    internal val _borsaLimitOrders = MutableStateFlow<List<com.example.data.BorsaLimitOrder>>(emptyList())
    val borsaLimitOrders: StateFlow<List<com.example.data.BorsaLimitOrder>> = _borsaLimitOrders.asStateFlow()

    internal val _activeProductions = MutableStateFlow<List<com.example.data.ActiveProduction>>(emptyList())
    val activeProductions: StateFlow<List<com.example.data.ActiveProduction>> = _activeProductions.asStateFlow()

    internal val _dailyRewardDialogData = MutableStateFlow<DailyRewardData?>(null)
    val dailyRewardDialogData: StateFlow<DailyRewardData?> = _dailyRewardDialogData.asStateFlow()

    // Real-Time Clock & Real-World Sync State
    internal val _realTimeClockText = MutableStateFlow("")
    val realTimeClockText: StateFlow<String> = _realTimeClockText.asStateFlow()

    internal val _realTimeShiftName = MutableStateFlow("☀️ Gündüz Vardiyası")
    val realTimeShiftName: StateFlow<String> = _realTimeShiftName.asStateFlow()

    internal val _realTimeShiftBonusText = MutableStateFlow("+%10 Verimlilik Primi")
    val realTimeShiftBonusText: StateFlow<String> = _realTimeShiftBonusText.asStateFlow()

    internal val _nextBorsaTickSeconds = MutableStateFlow(30)
    val nextBorsaTickSeconds: StateFlow<Int> = _nextBorsaTickSeconds.asStateFlow()

    internal val _nextDailyRewardRemainingMs = MutableStateFlow(0L)
    val nextDailyRewardRemainingMs: StateFlow<Long> = _nextDailyRewardRemainingMs.asStateFlow()

    // Real-Time Daily Banking State
    internal val _nextBankSettlementRemainingMs = MutableStateFlow(0L)
    val nextBankSettlementRemainingMs: StateFlow<Long> = _nextBankSettlementRemainingMs.asStateFlow()

    internal val _dailyDepositYieldTry = MutableStateFlow(0L)
    val dailyDepositYieldTry: StateFlow<Long> = _dailyDepositYieldTry.asStateFlow()

    internal val _dailyLoanInstallmentTry = MutableStateFlow(0L)
    val dailyLoanInstallmentTry: StateFlow<Long> = _dailyLoanInstallmentTry.asStateFlow()

    internal val _dailyLoanPrincipalTry = MutableStateFlow(0L)
    val dailyLoanPrincipalTry: StateFlow<Long> = _dailyLoanPrincipalTry.asStateFlow()

    internal val _dailyLoanInterestTry = MutableStateFlow(0L)
    val dailyLoanInterestTry: StateFlow<Long> = _dailyLoanInterestTry.asStateFlow()

    internal val _offlineEarningsData = MutableStateFlow<OfflineEarningsData?>(null)
    val offlineEarningsData: StateFlow<OfflineEarningsData?> = _offlineEarningsData.asStateFlow()

    fun dismissOfflineEarningsDialog() {
        _offlineEarningsData.value = null
    }

    fun claimOfflineBonusWithGems(gemCost: Int = 10): Boolean {
        val data = _offlineEarningsData.value ?: return false
        val p = player.value ?: return false
        if (p.gems < gemCost) {
            SmartNotificationManager.show(
                "Yetersiz Elmas! Çevrimdışı kazancı 2'ye katlamak için $gemCost 💎 Elmas gerekiyor. (Mevcut: ${p.gems} 💎)",
                "Not enough gems! $gemCost 💎 Gems required to double offline earnings. (Current: ${p.gems} 💎)",
                NotificationType.ALERT
            )
            return false
        }
        val bonusMoney = data.netMoneyEarned.coerceAtLeast(0L)
        val bonusXp = data.xpGained
        viewModelScope.launch {
            val updatedPlayer = p.copy(
                money = p.money + bonusMoney,
                gems = p.gems - gemCost,
                xp = p.xp + bonusXp,
                totalProfit = p.totalProfit + bonusMoney
            )
            repository.updatePlayer(updatedPlayer)
            com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
            com.example.ui.components.ParticleManager.spawnCelebration()
            SmartNotificationManager.show(
                "🎉 $gemCost 💎 Elmas ile Çevrimdışı Gelir 2'ye katlandı! +₳${com.example.ui.components.formatMoney(bonusMoney)} ve +$bonusXp XP hesabınıza eklendi.",
                "🎉 Offline earnings doubled with $gemCost 💎 Gems! +₳${com.example.ui.components.formatMoney(bonusMoney)} and +$bonusXp XP added.",
                NotificationType.SUCCESS
            )
            dismissOfflineEarningsDialog()
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        }
        return true
    }

    fun claimOfflineBonusWithAd() {
        val data = _offlineEarningsData.value ?: return
        val p = player.value ?: return
        val bonusMoney = data.netMoneyEarned.coerceAtLeast(0L)
        val bonusXp = data.xpGained
        val bonusGems = 5
        viewModelScope.launch {
            val updatedPlayer = p.copy(
                money = p.money + bonusMoney,
                gems = p.gems + bonusGems,
                xp = p.xp + bonusXp,
                totalProfit = p.totalProfit + bonusMoney
            )
            repository.updatePlayer(updatedPlayer)
            com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
            com.example.ui.components.ParticleManager.spawnCelebration()
            SmartNotificationManager.show(
                "🎬 Bonus ile Çevrimdışı Gelir 2'ye katlandı! +₳${com.example.ui.components.formatMoney(bonusMoney)}, +$bonusGems 💎 ve +$bonusXp XP kasanıza eklendi.",
                "🎬 Bonus earned! +₳${com.example.ui.components.formatMoney(bonusMoney)}, +$bonusGems 💎 and +$bonusXp XP added.",
                NotificationType.SUCCESS
            )
            dismissOfflineEarningsDialog()
            saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
        }
    }

    // =========================================================================
    // DAILY QUESTS & SEASON PASS ENGINE
    // =========================================================================
    internal val _dailyQuestState = MutableStateFlow(
        DailyQuestState(
            quests = DailyQuestManager.generateFreshDailyQuests(),
            lastResetDateKey = DailyQuestManager.getTodayDateKey()
        )
    )
    val dailyQuestState: StateFlow<DailyQuestState> = _dailyQuestState.asStateFlow()

    internal val _showDailyQuestsDialog = MutableStateFlow(false)
    val showDailyQuestsDialog: StateFlow<Boolean> = _showDailyQuestsDialog.asStateFlow()

    fun openDailyQuestsDialog() {
        checkDailyQuestsReset()
        _showDailyQuestsDialog.value = true
    }

    fun dismissDailyQuestsDialog() {
        _showDailyQuestsDialog.value = false
    }

    internal var lastBackgroundTimestampMs: Long = System.currentTimeMillis()

    fun setAppForegroundState(isForeground: Boolean) {
        val wasBackground = !_isAppInForeground.value
        _isAppInForeground.value = isForeground
        try {
            com.example.notification.LocalGameNotificationManager.setAppInForeground(isForeground)
        } catch (_: Throwable) {}
        if (isForeground) {
            checkDailyQuestsReset()
            if (wasBackground) {
                viewModelScope.launch {
                    try {
                        com.example.data.CloudServerTimeManager.syncWithServerTime(force = false)
                    } catch (_: Throwable) {}
                    val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
                    val offlineMs = (now - lastBackgroundTimestampMs).coerceAtLeast(0L)
                    val offlineSeconds = offlineMs / 1000L
                    if (offlineSeconds >= 60L) {
                        calculateAndApplyOfflineEarnings(offlineSeconds, offlineMs)
                    } else {
                        checkAndProcessFacilityConstructions(isOffline = true)
                        checkAndProcessFacilityUpgrades(isOffline = true)
                        checkAndProcessActiveProductions(isOffline = true)
                        checkAndProcessActiveDeliveries()
                    }
                    if (_isOnlineRegistered.value) {
                        syncCloudSaveToSupabase(force = true, immediate = true)
                    }
                }
            }
        } else {
            lastBackgroundTimestampMs = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
        }
    }

    fun checkDailyQuestsReset() {
        val todayKey = DailyQuestManager.getTodayDateKey()
        val currentLevel = player.value?.level ?: 1
        val lastKey = _dailyQuestState.value.lastResetDateKey

        val isNewDay = lastKey.isNotBlank() && lastKey != todayKey
        var needsSave = false

        if (_dailyQuestState.value.quests.isEmpty()) {
            val freshQuests = DailyQuestManager.generateFreshDailyQuests(currentLevel)
            _dailyQuestState.update { current ->
                current.copy(
                    quests = freshQuests,
                    lastResetDateKey = todayKey
                )
            }
            needsSave = true
        } else if (isNewDay) {
            _dailyQuestState.update { current ->
                current.copy(lastResetDateKey = todayKey)
            }
            needsSave = true
        }

        if (isNewDay) {
            recordGrowthPointIfNeeded(isNewDay = true)
            needsSave = true
        }

        if (needsSave) {
            saveEconomicDataToDataStore()
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }

    fun updateDailyQuestProgress(type: QuestType, amount: Long) {
        checkDailyQuestsReset()
        val currentLevel = player.value?.level ?: 1
        _dailyQuestState.update { current ->
            DailyQuestManager.updateQuestProgress(current, type, amount, currentLevel)
        }
        saveEconomicDataToDataStore()
        if ((_isOnlineRegistered.value || _onlineEmail.value.isNotBlank()) && _onlineEmail.value != "misafir_tuccar") {
            syncCloudSaveToSupabase(force = false, immediate = false)
        }
    }

    fun claimDailyQuestReward(quest: DailyQuest) {
        if (!_isOnlineRegistered.value || _onlineEmail.value.isBlank() || _onlineEmail.value == "misafir_tuccar") {
            SmartNotificationManager.show(
                "Görev ödüllerini alabilmek için çevrimiçi oturum açmalısınız!",
                "You must be logged in online to claim quest rewards!",
                NotificationType.ALERT
            )
            return
        }

        val currentState = _dailyQuestState.value
        val isAlreadyClaimed = quest.isClaimed || currentState.claimedQuestIdsToday.contains(quest.id)
        if (!quest.isCompleted || isAlreadyClaimed) {
            SmartNotificationManager.show(
                "Bu görevin ödülü daha önce alındı veya görev henüz tamamlanmadı!",
                "Reward for this quest was already claimed or quest is not completed!",
                NotificationType.ALERT
            )
            return
        }

        val p = player.value ?: return
        val newMoney = p.money + quest.rewardMoney
        val newSeasonXp = currentState.seasonXp + quest.rewardSeasonXp
        val newSeasonLevel = DailyQuestManager.computeSeasonLevel(newSeasonXp)

        _dailyQuestState.update { current ->
            val updatedQuests = current.quests.map {
                if (it.id == quest.id) it.copy(isClaimed = true) else it
            }
            current.copy(
                quests = updatedQuests,
                seasonXp = newSeasonXp,
                seasonLevel = newSeasonLevel,
                claimedQuestIdsToday = current.claimedQuestIdsToday + quest.id
            )
        }

        viewModelScope.launch {
            val updatedP = p.copy(
                money = newMoney
            )
            repository.updatePlayer(updatedP)
            saveEconomicDataToDataStoreSuspend(customPlayer = updatedP)
            if ((_isOnlineRegistered.value || _onlineEmail.value.isNotBlank()) && _onlineEmail.value != "misafir_tuccar") {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedP)
            }
        }

        SmartNotificationManager.show(
            "🎯 Görev Tamamlandı! +₳${com.example.ui.components.formatMoney(quest.rewardMoney)} ve +${quest.rewardSeasonXp} TP eklendi.",
            "🎯 Quest Completed! +₳${com.example.ui.components.formatMoney(quest.rewardMoney)} and +${quest.rewardSeasonXp} XP added.",
            NotificationType.SUCCESS
        )
    }

    fun claimSeasonPassTierReward(tier: SeasonPassTier, isVip: Boolean) {
        if (!_isOnlineRegistered.value || _onlineEmail.value.isBlank() || _onlineEmail.value == "misafir_tuccar") {
            SmartNotificationManager.show(
                "Sezon ödüllerini alabilmek için çevrimiçi oturum açmalısınız!",
                "You must be logged in online to claim season rewards!",
                NotificationType.ALERT
            )
            return
        }

        val currentXp = _dailyQuestState.value.seasonXp
        if (currentXp < tier.requiredXp) {
            SmartNotificationManager.show(
                "Bu kademe için yeterli Sezon TP toplanmadı!",
                "Insufficient Season XP for this tier!",
                NotificationType.ALERT
            )
            return
        }

        if (!isVip && _dailyQuestState.value.claimedFreeTiers.contains(tier.tierLevel)) {
            SmartNotificationManager.show(
                "Bu kademenin ücretsiz ödülü daha önce zaten alındı!",
                "Free reward for this tier has already been claimed!",
                NotificationType.ALERT
            )
            return
        }
        if (isVip && _dailyQuestState.value.claimedVipTiers.contains(tier.tierLevel)) {
            SmartNotificationManager.show(
                "Bu kademenin VIP ödülü daha önce zaten alındı!",
                "VIP reward for this tier has already been claimed!",
                NotificationType.ALERT
            )
            return
        }
        if (isVip && !_dailyQuestState.value.isVipPassUnlocked) {
            SmartNotificationManager.show(
                "VIP Ticaret Pasosu henüz açılmadı! Elmas ile VIP kilidini açabilirsiniz.",
                "VIP Trade Pass is locked! You can unlock VIP using gems.",
                NotificationType.ALERT
            )
            return
        }

        val p = player.value ?: return
        val rewardMoney = if (isVip) tier.freeRewardMoney * 2 else tier.freeRewardMoney

        _dailyQuestState.update { current ->
            if (isVip) {
                current.copy(claimedVipTiers = current.claimedVipTiers + tier.tierLevel)
            } else {
                current.copy(claimedFreeTiers = current.claimedFreeTiers + tier.tierLevel)
            }
        }

        viewModelScope.launch {
            val updatedP = p.copy(
                money = p.money + rewardMoney
            )
            repository.updatePlayer(updatedP)
            saveEconomicDataToDataStoreSuspend(customPlayer = updatedP)
            if ((_isOnlineRegistered.value || _onlineEmail.value.isNotBlank()) && _onlineEmail.value != "misafir_tuccar") {
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedP)
            }
        }

        SmartNotificationManager.show(
            "👑 Sezon Pasosu Seviye ${tier.tierLevel} Ödülü Alındı! +₳${com.example.ui.components.formatMoney(rewardMoney)} kazandınız.",
            "👑 Season Pass Tier ${tier.tierLevel} Reward Claimed! Earned +₳${com.example.ui.components.formatMoney(rewardMoney)}.",
            NotificationType.SUCCESS
        )
    }

    internal val _hasCompletedFirstTrade = MutableStateFlow(false)
    val hasCompletedFirstTrade: StateFlow<Boolean> = _hasCompletedFirstTrade.asStateFlow()

    internal val _tradeTutorialStep = MutableStateFlow(1) // 1: Welcome/Nav, 2: Select & Buy, 3: Completed/Sell, 4: Dismissed
    val tradeTutorialStep: StateFlow<Int> = _tradeTutorialStep.asStateFlow()

    internal val _isEntrepreneurGuideCompleted = MutableStateFlow(false)
    val isEntrepreneurGuideCompleted: StateFlow<Boolean> = _isEntrepreneurGuideCompleted.asStateFlow()

    fun advanceTradeTutorialStep(step: Int) {
        _tradeTutorialStep.value = step
        if (step >= 6) {
            _isEntrepreneurGuideCompleted.value = true
            viewModelScope.launch {
                repository.economicDataStore?.setIsEntrepreneurGuideCompleted(true)
            }
        }
    }

    fun dismissTradeTutorial() {
        _tradeTutorialStep.value = 6
        _isEntrepreneurGuideCompleted.value = true
        viewModelScope.launch {
            repository.economicDataStore?.setIsEntrepreneurGuideCompleted(true)
        }
    }

    fun resetTradeTutorial() {
        _hasCompletedFirstTrade.value = false
        _tradeTutorialStep.value = 1
        _isEntrepreneurGuideCompleted.value = false
        viewModelScope.launch {
            repository.economicDataStore?.setIsEntrepreneurGuideCompleted(false)
        }
    }

    fun markFirstTradeCompleted() {
        if (!_hasCompletedFirstTrade.value) {
            _hasCompletedFirstTrade.value = true
            _tradeTutorialStep.value = 5
            SmartNotificationManager.show("🎉 TEBRİKLER! İlk ticaretiniz başarıyla gerçekleşti!", NotificationType.SUCCESS)
            viewModelScope.launch {
                repository.markFirstTradeCompleted()
            }
        }
    }

    fun dismissDailyRewardDialog() {
        _dailyRewardDialogData.value = null
    }

    fun fireEngineer() {
        SmartNotificationManager.show("Baş Mühendis işten çıkarıldı.", NotificationType.INFO)
        saveEconomicDataToDataStore()
    }

    fun fireSalesExec() {
        SmartNotificationManager.show("Satış Sorumlusu işten çıkarıldı.", NotificationType.INFO)
        saveEconomicDataToDataStore()
    }

    fun logManagerAction(managerId: String, description: String, impact: Long) {
        val managers = _managers.value
        val managerIndex = managers.indexOfFirst { it.id == managerId }
        if (managerIndex != -1) {
            val manager = managers[managerIndex]
            val newLog = com.example.data.ManagerActionLog(
                timestampMs = System.currentTimeMillis(),
                description = description,
                financialImpact = impact
            )
            val updatedLogs = (listOf(newLog) + manager.actionLogs).take(10)
            _managers.value = managers.mapIndexed { index, m ->
                if (index == managerIndex) m.copy(actionLogs = updatedLogs) else m
            }
        }
    }

    fun generateRandomTurkishManagerName(): String {
        val firstNames = listOf(
            "Ahmet", "Mehmet", "Mustafa", "Ali", "Hüseyin", "Hasan", "İbrahim", "İsmail", "Osman", "Murat",
            "Ömer", "Yusuf", "Emre", "Kaan", "Burak", "Eren", "Oğuz", "Serkan", "Volkan", "Deniz",
            "Ayşe", "Fatma", "Emine", "Hatice", "Zeynep", "Elif", "Merve", "Büşra", "Selin", "Ece",
            "Derya", "Gözde", "Ceren", "Aslı", "Canan", "Nazlı", "Pınar", "Tuğba", "Ebru", "Gizem",
            "Cem", "Koray", "Onur", "Barış", "Kerem", "Mert", "Alper", "İrem", "Seda", "Hande"
        )
        val lastNames = listOf(
            "Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir",
            "Arslan", "Doğan", "Kılıç", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan", "Şimşek",
            "Eroğlu", "Özcan", "Korkmaz", "Çakır", "Yalçın", "Güneş", "Bozkurt", "Ünal", "Gül", "Avcı",
            "Karadağ", "Aksoy", "Polat", "Tekin", "Coşkun"
        )
        val suffixes = listOf("", "", "", "", "", ", MBA", ", CFA", ", Ph.D.")
        return "${firstNames.random()} ${lastNames.random()}${suffixes.random()}"
    }

    fun hireManager(managerId: String): Boolean {
        val p = player.value ?: return false
        val currentList = _managers.value
        val mgr = currentList.find { it.id == managerId } ?: return false
        if (mgr.isHired) return false

        val gemCost = mgr.hireGemCost
        if (p.gems < gemCost) {
            SmartNotificationManager.show(
                "Müdür transferi için yetersiz elmas! Gerekli: $gemCost 💎 (Mevcut: ${p.gems} 💎)",
                NotificationType.ALERT
            )
            return false
        }

        val newName = generateRandomTurkishManagerName()
        val baseLevel1Mgr = mgr.copy(
            name = newName,
            level = 1,
            efficiency = 1.0f,
            isHired = true,
            isActive = true,
            actionLogs = emptyList()
        )

        val updatedList = currentList.map { if (it.id == managerId) baseLevel1Mgr else it }
        val calculatedList = recalculateManagerSalaries(updatedList)

        viewModelScope.launch {
            repository.updatePlayer(p.copy(gems = p.gems - gemCost))
            _managers.value = calculatedList
            SmartNotificationManager.show(
                "👔 ${mgr.title} ($newName) $gemCost 💎 transfer bütçesiyle işe alındı! (Maaş: ₳${com.example.ui.components.formatMoney(baseLevel1Mgr.dailySalary)}/Gün)",
                NotificationType.SUCCESS
            )
            logManagerAction(managerId, "Transfer Sözleşmesi İmzalandı: $gemCost 💎 elmas bütçesiyle Seviye 1 olarak işe alındı.", 0L)
            saveEconomicDataToDataStore()
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
        }
        return true
    }

    fun applyDisciplinaryAction(managerId: String, reason: String) {
        val currentList = _managers.value
        val mgr = currentList.find { it.id == managerId } ?: return
        if (!mgr.isHired) return

        if (mgr.level > 1) {
            viewModelScope.launch {
                val updatedList = currentList.map {
                    if (it.id == managerId) it.copy(level = it.level - 1) else it
                }
                _managers.value = recalculateManagerSalaries(updatedList)
                logManagerAction(managerId, "⚠️ DİSİPLİN CEZASI: $reason. Müdür 1 seviye düşürüldü (Yeni Seviye: ${mgr.level - 1}).", 0)
                SmartNotificationManager.show("Disiplin Cezası: ${mgr.title} 1 seviye düşürüldü!", NotificationType.ALERT)
                saveEconomicDataToDataStore()
            }
        } else {
            // Level 1 manager -> Fire them
            logManagerAction(managerId, "🚨 KOVULDU: $reason. 1. Seviye müdür disiplin cezası aldığı için işten atıldı!", 0)
            SmartNotificationManager.show("Kovuldu: ${mgr.title} disiplinsizlikten işten atıldı!", NotificationType.ALERT)
            fireManager(managerId)
        }
    }

    fun fireManager(managerId: String) {
        val p = player.value ?: return
        val currentList = _managers.value
        val mgr = currentList.find { it.id == managerId } ?: return
        if (!mgr.isHired) return

        viewModelScope.launch {
            val updatedList = currentList.map {
                if (it.id == managerId) it.copy(
                    name = "",
                    isHired = false,
                    isActive = false,
                    level = 1,
                    efficiency = 1.0f,
                    actionLogs = emptyList()
                ) else it
            }
            _managers.value = recalculateManagerSalaries(updatedList)
            SmartNotificationManager.show("🚪 ${mgr.title} (${if (mgr.name.isNotBlank()) mgr.name else "Müdür"}) işten çıkarıldı. Pozisyon boşaltıldı.", NotificationType.INFO)
            saveEconomicDataToDataStore()
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
        }
    }

    fun upgradeManager(managerId: String): Boolean {
        val p = player.value ?: return false
        val currentList = _managers.value
        val mgr = currentList.find { it.id == managerId } ?: return false
        if (!mgr.isHired || mgr.level >= 5) return false
        val upgradeCost = mgr.dailySalary * 5 * mgr.level
        if (p.money < upgradeCost) {
            SmartNotificationManager.show("Terfi için yetersiz bakiye! Gerekli: ₳${com.example.ui.components.formatMoney(upgradeCost)}", NotificationType.ALERT)
            return false
        }
        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money - upgradeCost))
            val updatedList = currentList.map {
                if (it.id == managerId) it.copy(
                    level = it.level + 1,
                    efficiency = it.efficiency + 0.25f
                ) else it
            }
            val recalculatedList = recalculateManagerSalaries(updatedList)
            _managers.value = recalculatedList
            val newLvl = recalculatedList.find { it.id == managerId }?.level ?: (mgr.level + 1)
            SmartNotificationManager.show("⭐ ${mgr.name} Seviye $newLvl'e terfi ettirildi! Maaşı ve performansı arttı.", NotificationType.SUCCESS)
            saveEconomicDataToDataStore()
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
        }
        return true
    }

    fun toggleManagerActive(managerId: String) {
        val currentList = _managers.value
        val mgr = currentList.find { it.id == managerId } ?: return
        if (!mgr.isHired) return
        val newActive = !mgr.isActive
        _managers.value = currentList.map { if (it.id == managerId) it.copy(isActive = newActive) else it }
        SmartNotificationManager.show(
            if (newActive) "✅ ${mgr.title} göreve başladı (Aktif)." else "⏸️ ${mgr.title} izinli (Pasif).",
            NotificationType.INFO
        )
        saveEconomicDataToDataStore()
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }

    fun toggleAllManagersActiveStatus() {
        val currentList = _managers.value
        val hiredManagers = currentList.filter { it.isHired }
        if (hiredManagers.isEmpty()) return

        val anyActive = hiredManagers.any { it.isActive }
        val newActive = !anyActive

        _managers.value = currentList.map { 
            if (it.isHired) it.copy(isActive = newActive) else it 
        }

        SmartNotificationManager.show(
            if (newActive) "✅ İŞBAŞI: Tüm müdürler göreve başladı." else "⏸️ RESMİ TATİL: Tüm müdürler izne ayrıldı.",
            NotificationType.INFO
        )

        saveEconomicDataToDataStore()
        if (_isOnlineRegistered.value) {
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }

    /**
     * İNSAN KAYNAKLARI MÜDÜR GÖREV OTOMASYONU (HR MANAGER DUTIES ENGINE)
     * Şirkette işe alınmış ve aktif durumda olan tüm departman müdürlerinin
     * periyodik olarak görevlerini otomatik icra etmesini sağlar.
     */
    /**
     * DİNAMİK YÖNETİCİ OTOMASYON MOTORU (CONSORTIUM-FIRST C-SUITE EXECUTIVE SYNERGY)
     * İşe alınmış ve aktif durumda olan holding müdürlerinin (CFO, COO, CCO, CIO, CPO, CTO, CHRO, Maintenance)
     * konsorsiyum mega projeleri ve holding operasyonları için senkronize çalışmasını sağlar:
     * 1. Hazine Müdürü (mgr_treasury): Konsorsiyum tedarik, borsa ve bakım masraflarını banka mevduatından finanse eder.
     * 2. Bakım Müdürü (mgr_maintenance): Yıpranan tesisleri tespit edip otomatik onararak üretimin kesilmesini önler.
     * 3. Üretim Müdürü (mgr_prod): Konsorsiyum teslimat yuvalarının ihtiyaç duyduğu ürünleri tesislerde öncelikli üretir.
     * 4. Lojistik Müdürü (mgr_logistics): Konsorsiyum için gereken ürünleri depoda sevkiyata kadar rezerve eder, ihtiyaç fazlasını satar.
     * 5. Borsa Analisti (mgr_borsa): Eksik konsorsiyum malzemelerini borsadan spot alımla tedarik eder veya vadeliye yönlendirir.
     * 6. Tedarik Müdürü (mgr_contracts): Konsorsiyum üretimi için malzemeleri en uygun maliyetle piyasadan veya vadeli sözleşmelerle temin eder.
     * 7. Ar-Ge Müdürü (mgr_rd): Konsorsiyum ve sanayi projeleri için kritik olan ileri teknoloji araştırmalarını öncelikle geliştirir.
     * 8. İK Müdürü (mgr_hr): Konsorsiyum hedeflerini gözetmeyen müdürleri tespit edip hizalar, terfileri yönetir ve sevkiyatları koordine eder.
     */
    fun processManagerAutomatedDuties() {
        val p = player.value ?: return
        val currentManagers = _managers.value
        val hiredActiveManagers = currentManagers.filter { it.isHired && it.isActive }
        if (hiredActiveManagers.isEmpty()) return

        viewModelScope.launch {
            val curPlayer = player.value ?: return@launch
            val pId = curPlayer.id
            val pName = curPlayer.name
            val cleanEmail = _onlineEmail.value.replace(".", "_")
            val emailPrefix = _onlineEmail.value.substringBefore("@")

            fun isUserSlotLocal(slot: com.example.data.ConsortiumSupplierSlot): Boolean {
                if (slot.assignedPartnerId == "local_player" || slot.assignedPartnerId == pId) return true
                if (pName.isNotBlank() && slot.assignedPartnerName == pName) return true
                if (cleanEmail.isNotBlank() && slot.assignedPartnerId?.contains(cleanEmail) == true) return true
                if (emailPrefix.isNotBlank() && slot.assignedPartnerId?.contains(emailPrefix) == true) return true
                return false
            }

            val playerConsortiums = _megaProjects.value.filter { proj ->
                proj.leaderPlayerId == "local_player" ||
                proj.leaderPlayerId == pId ||
                (pName.isNotBlank() && proj.leaderPlayerName == pName) ||
                (cleanEmail.isNotBlank() && proj.leaderPlayerId.contains(cleanEmail)) ||
                (emailPrefix.isNotBlank() && proj.leaderPlayerId.contains(emailPrefix)) ||
                proj.slots.any { isUserSlotLocal(it) }
            }

            // Calculate precise consortium requirements and deficits
            val neededConsortiumRequirements = mutableMapOf<String, Int>()
            playerConsortiums.forEach { proj ->
                proj.slots.filter { isUserSlotLocal(it) && !it.isFullyDelivered }.forEach { slot ->
                    val deficit = slot.remainingQuantity
                    if (deficit > 0) {
                        neededConsortiumRequirements[slot.productId] = (neededConsortiumRequirements[slot.productId] ?: 0) + deficit
                    }
                }
            }

            for (mgr in hiredActiveManagers) {
                try {
                    when (mgr.id) {
                        "mgr_treasury" -> {
                            val livePlayer = player.value ?: return@launch
                            // 1. HAZİNE VE MAKROEKONOMİ MÜDÜRÜ: Finansman ve Likidite Yönetimi
                            val estimatedProcurementNeeds = neededConsortiumRequirements.entries.sumOf { (prodId, deficit) ->
                                val price = marketPrices.value.find { it.itemId == prodId }?.price ?: 20_000L
                                minOf(deficit, 10) * price
                            }
                            val maintenanceNeeds = businesses.value.filter { it.wearLevel > 0.5f }.sumOf { 15_000L * it.level }
                            val totalNeededLiquidity = (estimatedProcurementNeeds + maintenanceNeeds + 200_000L).coerceAtLeast(300_000L)

                            if (livePlayer.money < totalNeededLiquidity && livePlayer.depositBalance > 0L) {
                                val withdrawAmt = minOf(livePlayer.depositBalance, totalNeededLiquidity - livePlayer.money + (100_000L * mgr.level))
                                if (withdrawAmt > 0L) {
                                    val updatedMoney = livePlayer.money + withdrawAmt
                                    val updatedDeposit = livePlayer.depositBalance - withdrawAmt
                                    val updatedPlayer = livePlayer.copy(money = updatedMoney, depositBalance = updatedDeposit)
                                    repository.updatePlayer(updatedPlayer)
                                    logManagerAction(
                                        "mgr_treasury",
                                        "Hazine & Finansman: Konsorsiyum tedarik ve bakım masrafları için banka mevduatından ₳${com.example.ui.components.formatMoney(withdrawAmt)} nakit çekilerek operasyon bütçesine aktarıldı.",
                                        withdrawAmt
                                    )
                                }
                            } else if (livePlayer.money > 2_000_000L && livePlayer.money > totalNeededLiquidity * 2) {
                                val depositAmt = minOf(150_000L * mgr.level, (livePlayer.money * 0.08).toLong()).coerceAtLeast(20_000L)
                                if (livePlayer.money >= depositAmt) {
                                    val updatedMoney = livePlayer.money - depositAmt
                                    val updatedDeposit = livePlayer.depositBalance + depositAmt
                                    val updatedPlayer = livePlayer.copy(money = updatedMoney, depositBalance = updatedDeposit)
                                    repository.updatePlayer(updatedPlayer)
                                    logManagerAction(
                                        "mgr_treasury",
                                        "Hazine & Fon: Fazla likidite vadeli mevduat hesabına yatırıldı (+₳${com.example.ui.components.formatMoney(depositAmt)})",
                                        depositAmt
                                    )
                                }
                            }
                        }
                        "mgr_maintenance" -> {
                            val livePlayer = player.value ?: return@launch
                            // 2. BAKIM MÜDÜRÜ: Tüm Tesislerin Otomatik Onarımı ve Sıfır Yıpranma
                            val wornBusinesses = businesses.value.filter { it.wearLevel > 0.5f }.sortedByDescending { it.wearLevel }
                            for (target in wornBusinesses) {
                                val currentPlayer = player.value ?: break
                                val repairCost = 15_000L * target.level
                                if (currentPlayer.money >= repairCost) {
                                    val updatedPlayer = currentPlayer.copy(money = currentPlayer.money - repairCost)
                                    repository.updatePlayer(updatedPlayer)
                                    val repaired = target.copy(wearLevel = 0f)
                                    repository.updateBusiness(repaired)
                                    logManagerAction(
                                        "mgr_maintenance",
                                        "Bakım Müdürü: ${target.type} tesisinde bakım tamamlandı (Yıpranma sıfırlandı, üretim kapasitesi korundu).",
                                        -repairCost
                                    )
                                } else {
                                    break
                                }
                            }
                        }
                        "mgr_prod" -> {
                            // 3. ÜRETİM VE OPERASYON MÜDÜRÜ: Tüm Tesislerde Otomatik Üretim, Borsa Otomatik Tedarik & Konsorsiyum Önceliği
                            val myBusinesses = businesses.value
                            if (myBusinesses.isNotEmpty()) {
                                for (b in myBusinesses) {
                                    val prod = com.example.data.Product.values().find { it.facilityId == b.type || it.id == b.type }
                                    if (prod != null && !b.isConstructing && b.getRemainingStorageCapacity() >= 5) {
                                        if (!_productionProgress.value.containsKey(prod.id)) {
                                            val deficit = neededConsortiumRequirements[prod.id] ?: (10 * mgr.level)
                                            val batchQty = minOf(deficit, b.getRemainingStorageCapacity(), (10 * mgr.level)).coerceAtLeast(1)
                                            produce(prod.id, batchQty, isSilent = true, autoProcure = true)
                                            val isConsortiumPriority = neededConsortiumRequirements.containsKey(prod.id)
                                            val titleText = if (isConsortiumPriority) "Üretim Müdürü (Konsorsiyum & Borsa Tedarik)" else "Üretim Müdürü"
                                            logManagerAction(
                                                "mgr_prod",
                                                "$titleText: ${prod.getDisplayName()} üretimi başlatıldı ($batchQty Ton - ${b.type} tesisi, hammadde depodan/borsadan temin edildi)",
                                                0L
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "mgr_logistics" -> {
                            // 4. LOJİSTİK VE PAZAR SATIŞ MÜDÜRÜ: Konsorsiyum Otomatik Sevkiyatı & Fazla Stok Satışı
                            val currentInv = inventory.value.filter { it.quantity > 0 }
                            if (currentInv.isNotEmpty()) {
                                val sellableCandidates = currentInv.mapNotNull { item ->
                                    val neededForConsortium = neededConsortiumRequirements[item.itemId] ?: 0
                                    val surplus = (item.quantity - neededForConsortium).coerceAtLeast(0)
                                    if (surplus > 0) Pair(item, surplus) else null
                                }

                                // Konsorsiyum için gereken ürünleri otomatik sevk et
                                playerConsortiums.forEach { proj ->
                                    proj.slots.filter { isUserSlotLocal(it) && !it.isFullyDelivered }.forEach { slot ->
                                        val deficit = slot.remainingQuantity
                                        val stock = currentInv.find { it.itemId == slot.productId }?.quantity ?: 0
                                        if (deficit > 0 && stock > 0) {
                                            // Ne kadar sevk edebiliriz? Hem stok hem ihtiyaç hem de yöneticinin bir döngüde taşıyabileceği kapasite ile sınırlı
                                            val deliverQty = minOf(deficit, stock, 25 * mgr.level)
                                            if (deliverQty > 0) {
                                                deliverToConsortiumSlot(
                                                    projectId = proj.id,
                                                    slotId = slot.slotId,
                                                    productId = slot.productId,
                                                    quantity = deliverQty,
                                                    partnerId = "local_player",
                                                    partnerName = player.value?.name ?: "Şirket"
                                                )
                                                logManagerAction(
                                                    "mgr_logistics",
                                                    "Lojistik Müdürü: $deliverQty Ton ${slot.productId} konsorsiyum deposuna otomatik sevk edildi.",
                                                    0L
                                                )
                                                // reduce local view of inventory so we don't double deliver in same loop
                                                val remainingStock = stock - deliverQty
                                                // neededConsortiumRequirements is not updated here but it's okay for one pass
                                            }
                                        }
                                    }
                                }

                                if (sellableCandidates.isNotEmpty()) {
                                    val (itemToSell, maxSurplus) = sellableCandidates.random()
                                    val sellQty = minOf(maxSurplus, (10 * mgr.level * mgr.efficiency).toInt().coerceAtLeast(1))
                                    val unitPrice = marketPrices.value.find { it.itemId == itemToSell.itemId }?.price ?: 500L
                                    val revenue = sellQty.toLong() * unitPrice
                                    repository.sellItem(itemToSell.itemId, sellQty, unitPrice)
                                    logManagerAction(
                                        "mgr_logistics",
                                        "Lojistik Müdürü: ${itemToSell.itemId} ihtiyaç fazlası pazar satışı yapıldı ($sellQty Ton -> +₳${com.example.ui.components.formatMoney(revenue)})",
                                        revenue
                                    )
                                }
                            }
                        }
                        "mgr_contracts" -> {
                            // 5. VADELİ SÖZLEŞME VE TEDARİK MÜDÜRÜ: Pazardan En Uygun Fiyatla Bulma / Vadeli Tedarik
                            val livePlayer = player.value ?: return@launch
                            val currentInv = inventory.value
                            val activeContracts = _futuresContracts.value.filter { !it.isFulfilled }
                            val fulfillable = activeContracts.find { c ->
                                val stock = currentInv.find { it.itemId == c.itemId }?.quantity ?: 0
                                stock >= c.quantity
                            }
                            if (fulfillable != null) {
                                val totalVal = fulfillable.lockedPricePerUnit * fulfillable.quantity
                                fulfillFuturesContract(fulfillable.id)
                                logManagerAction(
                                    "mgr_contracts",
                                    "Vadeli Tedarik Müdürü: B2B Sözleşmesi teslim edildi (${fulfillable.itemId})",
                                    totalVal
                                )
                            } else {
                                val missingConsortiumItems = neededConsortiumRequirements.filter { (prodId, deficit) ->
                                    val inStock = currentInv.find { it.itemId == prodId }?.quantity ?: 0
                                    inStock < deficit
                                }
                                if (missingConsortiumItems.isNotEmpty()) {
                                    val targetItem = missingConsortiumItems.keys.first()
                                    val deficit = missingConsortiumItems[targetItem] ?: 5
                                    val unitPrice = marketPrices.value.find { it.itemId == targetItem }?.price ?: 30_000L
                                    val buyQty = minOf(deficit, (10 * mgr.level)).coerceAtLeast(1)
                                    val totalCost = unitPrice * buyQty
                                    if (livePlayer.money >= totalCost) {
                                        val updatedPlayer = livePlayer.copy(money = livePlayer.money - totalCost)
                                        repository.updatePlayer(updatedPlayer)
                                        repository.produceItem(targetItem, buyQty)
                                        logManagerAction(
                                            "mgr_contracts",
                                            "Vadeli Sözleşme & Tedarik: Konsorsiyum üretimi için $targetItem x$buyQty Ton pazardan en uygun maliyetle temin edildi.",
                                            -totalCost
                                        )
                                    }
                                }
                            }
                        }
                        "mgr_rd" -> {
                            // 6. ARAŞTIRMA VE GELİŞTİRME MÜDÜRÜ: Konsorsiyum Gereklilikleri İçin Öncelikli Araştırma
                            val currentActiveSlots = _activeResearches.value.keys.map { it.removePrefix("tech_") }.distinct().size
                            if (currentActiveSlots < 4) {
                                val consortiumTechs = listOf("automation", "heavy_industry", "logistics", "green_energy", "aerospace", "quantum_ai", "nanotech", "biotech_med", "consumer_goods", "petrochem")
                                val livePlayer = player.value ?: return@launch
                                val nextTech = consortiumTechs.firstOrNull { tech ->
                                    val lvl = getTechLevel(tech)
                                    lvl >= 1 && lvl < 5 && !_activeResearches.value.containsKey(tech) && !_activeResearches.value.containsKey("tech_$tech")
                                }
                                if (nextTech != null) {
                                    val currentLvl = getTechLevel(nextTech)
                                    val cost = calculateTechCost(nextTech, currentLvl)
                                    if (livePlayer.money >= cost) {
                                        startResearch(nextTech, cost = cost, spendGems = false)
                                        logManagerAction(
                                            "mgr_rd",
                                            "Ar-Ge Müdürü (Konsorsiyum Önceliği): $nextTech Lvl ${currentLvl + 1} sanayi araştırması başlatıldı.",
                                            -cost
                                        )
                                    }
                                }
                            }
                        }
                        "mgr_borsa" -> {
                            // 7. BORSA VE YATIRIM ANALİSTİ: Depoda Olmayan Malzemeyi Tedarik Müdürü ile Koordineli Borsadan Alma
                            val prices = marketPrices.value
                            val livePlayer = player.value ?: return@launch
                            val currentInv = inventory.value

                            val missingConsortiumItems = neededConsortiumRequirements.filter { (prodId, deficit) ->
                                val inStock = currentInv.find { it.itemId == prodId }?.quantity ?: 0
                                inStock < deficit
                            }

                            if (missingConsortiumItems.isNotEmpty() && prices.isNotEmpty()) {
                                val targetMissing = missingConsortiumItems.keys.first()
                                val priceObj = prices.find { it.itemId == targetMissing }
                                val unitPrice = priceObj?.price ?: com.example.data.Product.values().find { it.id == targetMissing }?.basePrice ?: 50_000L
                                val neededQty = minOf(missingConsortiumItems[targetMissing] ?: 5, 10 * mgr.level).coerceAtLeast(1)
                                val totalCost = unitPrice * neededQty

                                if (livePlayer.money >= totalCost) {
                                    val updatedPlayer = livePlayer.copy(money = livePlayer.money - totalCost)
                                    repository.updatePlayer(updatedPlayer)
                                    repository.produceItem(targetMissing, neededQty)
                                    logManagerAction(
                                        "mgr_borsa",
                                        "Borsa & Yatırım Analisti (Konsorsiyum Spot Alım): $targetMissing x$neededQty Ton borsadan tedarik edilip depoya aktarıldı.",
                                        -totalCost
                                    )
                                } else {
                                    logManagerAction(
                                        "mgr_borsa",
                                        "Borsa Analisti -> Vadeli Müdürü İşbirliği: $targetMissing temini için vadeli alım sözleşmesi koordine edildi.",
                                        0L
                                    )
                                }
                            } else if (prices.isNotEmpty() && livePlayer.money > 1_000_000L) {
                                val bestBargain = prices.minByOrNull { it.price }
                                if (bestBargain != null) {
                                    val buyQty = (5 * mgr.level).coerceAtLeast(1)
                                    val totalCost = bestBargain.price * buyQty
                                    if (livePlayer.money >= totalCost) {
                                        val updatedPlayer = livePlayer.copy(money = livePlayer.money - totalCost)
                                        repository.updatePlayer(updatedPlayer)
                                        repository.produceItem(bestBargain.itemId, buyQty)
                                        logManagerAction(
                                            "mgr_borsa",
                                            "Borsa & Yatırım Analisti: Portföy arbitrajı için ${bestBargain.itemId} x$buyQty Ton hisse/emtia spot alındı.",
                                            -totalCost
                                        )
                                    }
                                }
                            }
                        }
                        "mgr_hr" -> {
                            // 8. İNSAN KAYNAKLARI MÜDÜRÜ: Konsorsiyum İhtiyaçlarını Gözetmeyen Müdürleri Tespit Edip Hizalama & Sevkiyat
                            val livePlayer = player.value ?: return@launch
                            var consortiumActionExecuted = false

                            if (playerConsortiums.isNotEmpty()) {
                                for (proj in playerConsortiums) {
                                    val unmetSlots = proj.slots.filter { isUserSlotLocal(it) && !it.isFullyDelivered }
                                    for (slot in unmetSlots) {
                                        val stock = inventory.value.find { it.itemId == slot.productId }?.quantity ?: 0
                                        if (stock > 0) {
                                            val deliverQty = minOf(stock, slot.remainingQuantity)
                                            deliverMaterialsToConsortium(proj.id, slot.slotId, deliverQty, isSilent = true)
                                            logManagerAction(
                                                "mgr_hr",
                                                "İnsan Kaynakları & Operasyon: ${proj.consortiumName} için $deliverQty Ton ${slot.productName} teslim edildi.",
                                                0L
                                            )
                                            consortiumActionExecuted = true
                                            break
                                        } else {
                                            val unitPrice = marketPrices.value.find { it.itemId == slot.productId }?.price 
                                                ?: com.example.data.Product.values().find { it.id == slot.productId }?.basePrice ?: 50_000L
                                            
                                            val maxAffordableQty = (livePlayer.money / unitPrice).toInt()
                                            val buyQty = minOf(10, slot.remainingQuantity, maxAffordableQty)
                                            
                                            if (buyQty > 0) {
                                                val cost = unitPrice * buyQty
                                                val updatedPlayer = livePlayer.copy(money = livePlayer.money - cost)
                                                repository.updatePlayer(updatedPlayer)
                                                repository.produceItem(slot.productId, buyQty)
                                                deliverMaterialsToConsortium(proj.id, slot.slotId, buyQty, isSilent = true)
                                                logManagerAction(
                                                    "mgr_hr",
                                                    "İnsan Kaynakları & Tedarik: ${proj.consortiumName} için borsa üzerinden $buyQty Ton ${slot.productName} temin edilip sevk edildi.",
                                                    -cost
                                                )
                                                consortiumActionExecuted = true
                                                break
                                            }
                                        }
                                    }
                                    if (consortiumActionExecuted) break

                                    if (proj.isAllStagesFinished && !proj.isMassProductionApproved) {
                                        approveConsortiumMassProduction(proj.id, isSilent = true)
                                        logManagerAction(
                                            "mgr_hr",
                                            "İnsan Kaynakları & Yönetim: ${proj.consortiumName} tüm teslimatları tamamlandı, Seri Üretim onaylandı! 🏭",
                                            0L
                                        )
                                        consortiumActionExecuted = true
                                        break
                                    } else if (proj.isCurrentStageFinished && proj.currentStage != com.example.data.MegaProjectStage.STAGE_4_MASS_PRODUCTION && proj.currentStage != com.example.data.MegaProjectStage.COMPLETED) {
                                        advanceMegaProjectStage(proj.id)
                                        logManagerAction(
                                            "mgr_hr",
                                            "İnsan Kaynakları: ${proj.consortiumName} aşama gereksinimleri tamamlandı, yeni aşamaya geçildi! 🏆",
                                            0L
                                        )
                                        consortiumActionExecuted = true
                                        break
                                    }

                                    if (proj.isAllStagesFinished && !proj.isDividendClaimed) {
                                        claimMegaProjectDividend(proj.id)
                                        logManagerAction(
                                            "mgr_hr",
                                            "İnsan Kaynakları & Finans: ${proj.consortiumName} temettü geliri tahsil edildi.",
                                            0L
                                        )
                                        consortiumActionExecuted = true
                                        break
                                    }
                                }
                            }

                            if (!consortiumActionExecuted) {
                                val hiredManagers = _managers.value.filter { it.isHired && it.level < 5 }
                                val targetMgr = hiredManagers.minByOrNull { it.level }
                                if (targetMgr != null) {
                                    val upgradeCost = targetMgr.dailySalary * 5 * targetMgr.level
                                    if (livePlayer.money >= upgradeCost) {
                                        val updatedPlayer = livePlayer.copy(money = livePlayer.money - upgradeCost)
                                        repository.updatePlayer(updatedPlayer)
                                        val updatedList = _managers.value.map {
                                            if (it.id == targetMgr.id) it.copy(
                                                level = it.level + 1,
                                                efficiency = it.efficiency + 0.25f
                                            ) else it
                                        }
                                        val recalculatedList = recalculateManagerSalaries(updatedList)
                                        _managers.value = recalculatedList
                                        val newLevel = targetMgr.level + 1
                                        logManagerAction(
                                            "mgr_hr",
                                            "İnsan Kaynakları Otomatik Terfi: ${targetMgr.name} (${targetMgr.title}) Seviye $newLevel'e yükseltildi.",
                                            -upgradeCost
                                        )
                                        SmartNotificationManager.show(
                                            "👔 İK Müdürü, ${targetMgr.name} müdürünü Seviye $newLevel'e terfi ettirdi! ⭐",
                                            "👔 HR Director promoted ${targetMgr.name} to Level $newLevel! ⭐",
                                            NotificationType.SUCCESS
                                        )
                                        saveEconomicDataToDataStore(customPlayer = updatedPlayer, immediate = true)
                                        if (_isOnlineRegistered.value) {
                                            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer)
                                        }
                                    } else {
                                        logManagerAction(
                                            "mgr_hr",
                                            "İnsan Kaynakları: Konsorsiyum ihtiyaçlarını gözetmeyen müdürler tespit edildi; terfi ve koçluk bütçesi bekleniyor.",
                                            0L
                                        )
                                    }
                                } else {
                                    logManagerAction(
                                        "mgr_hr",
                                        "İnsan Kaynakları & Koordinasyon: Tüm holding departmanları konsorsiyum hedeflerine tam uyumla çalışıyor.",
                                        0L
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GameViewModel", "Manager duty execution error for ${mgr.id}: ${e.message}")
                }
            }
            saveEconomicDataToDataStore()
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = false)
            }
        }
    }


    /**
     * DİNAMİK İNSAN KAYNAKLARI MAAŞ MOTORU (DYNAMIC MANAGER SALARY ENGINE)
     * Oyun içi kurgusal ekonomi mantığına uygun olarak;
     * 1. Hiyerarşik Taban Maaş (Hazine: 50k, Vadeli: 40k, Borsa: 32k, Lojistik: 25k, Operasyon: 18k)
     * 2. Kıdem/Seviye Çarpanı (Seviye başına %25 artış)
     * 3. Makroekonomik Enflasyon Oranı (globalInflationRate) -> Satın alma gücü koruması
     * 4. Piyasa Ürün Fiyat Değişim Endeksi -> Hammadde ve mamul ürün piyasa bolluk/kıtlık fiyat oranı
     * 5. Müdür Verimlilik ve Disiplin Cezası Faktörü
     * Maaşlar her gün settlement döngüsünde ve makro enflasyon değişimlerinde otomatik güncellenir.
     */
    fun recalculateManagerSalaries(list: List<com.example.data.CompanyManager> = _managers.value): List<com.example.data.CompanyManager> {
        val macroState = gameState.value
        val inflationRate = macroState?.globalInflationRate ?: 0f

        val currentPrices = marketPrices.value
        val marketPriceRatio = if (currentPrices.isNotEmpty()) {
            val totalRatios = com.example.data.Product.values().map { p ->
                val curPrice = currentPrices.find { it.itemId == p.id }?.price ?: p.basePrice
                (curPrice.toFloat() / p.basePrice.toFloat())
            }
            if (totalRatios.isNotEmpty()) (totalRatios.sum() / totalRatios.size).toFloat().coerceIn(0.90f, 1.35f) else 1f
        } else {
            1.0f
        }

        val inflationFactor = (1.0f + (inflationRate / 100f).coerceIn(0f, 0.5f)).coerceIn(1.0f, 1.30f)
        val economicMultiplier = (inflationFactor * marketPriceRatio).coerceIn(0.90f, 1.45f)

        return list.map { mgr ->
            val baseSalary = when (mgr.id) {
                "mgr_treasury" -> 20000L
                "mgr_contracts" -> 16000L
                "mgr_borsa" -> 15000L
                "mgr_logistics" -> 13000L
                "mgr_hr" -> 12000L
                "mgr_rd" -> 11000L
                "mgr_prod" -> 10400L
                "mgr_maintenance" -> 10000L
                else -> 10000L
            }

            // Kıdem/Seviye Çarpanı: Seviye 1 (1.0x), Seviye 2 (1.25x), Seviye 3 (1.55x), Seviye 4 (1.90x), Seviye 5 (2.30x)
            val levelMultiplier = when (mgr.level.coerceIn(1, 5)) {
                1 -> 1.0f
                2 -> 1.25f
                3 -> 1.55f
                4 -> 1.90f
                5 -> 2.30f
                else -> 1.0f
            }
            val levelSalary = baseSalary.toFloat() * levelMultiplier

            val effFactor = mgr.efficiency.coerceIn(0.70f, 1.25f)
            val disciplineFactor = if (mgr.description.contains("Mali Disiplin") || mgr.description.contains("Disiplin")) 0.85f else 1.0f

            // Enflasyon ve 5. seviye dahil hiçbir müdürün maaşı 80.000 TL tavanını aşamaz
            val dynamicSalary = (levelSalary * economicMultiplier * effFactor * disciplineFactor)
                .toLong()
                .coerceIn(10000L, 80000L)

            mgr.copy(dailySalary = dynamicSalary)
        }
    }

    fun sanitizeManagerSalaries(list: List<com.example.data.CompanyManager>): List<com.example.data.CompanyManager> {
        val defaultList = com.example.data.getDefaultCompanyManagers()
        val mergedList = list.toMutableList()

        for (defaultMgr in defaultList) {
            if (mergedList.none { it.id == defaultMgr.id }) {
                mergedList.add(defaultMgr)
            }
        }

        val repairedList = mergedList.map { mgr ->
            val defaultMgr = defaultList.find { it.id == mgr.id }
            val title = mgr.title.ifBlank { defaultMgr?.title ?: "" }
            val specialty = mgr.specialty.ifBlank { defaultMgr?.specialty ?: "" }
            val description = mgr.description.ifBlank { defaultMgr?.description ?: "" }
            val name = if (mgr.isHired && mgr.name.isBlank()) {
                defaultMgr?.name?.ifBlank { generateRandomTurkishManagerName() } ?: generateRandomTurkishManagerName()
            } else if (!mgr.isHired && mgr.name.isBlank()) {
                defaultMgr?.name ?: ""
            } else {
                mgr.name
            }

            mgr.copy(
                name = name,
                title = title,
                specialty = specialty,
                description = description
            )
        }

        return recalculateManagerSalaries(repairedList)
    }

    fun upgradeWarehouseCapacity() {
        val p = player.value ?: return
        // Cost formula: base 25000, then scales up based on current capacity over 5000
        val upgradesDone = (p.inventoryCapacity - 5000) / 2500
        val cost = 25000L + (upgradesDone * 15000L)
        
        if (p.money >= cost) {
            viewModelScope.launch {
                val updatedP = p.copy(
                    money = p.money - cost,
                    inventoryCapacity = p.inventoryCapacity + 2500
                )
                repository.updatePlayer(updatedP)
                saveEconomicDataToDataStore()
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedP)
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.WAREHOUSE_CHANGE)
                SmartNotificationManager.show("Depo Kapasitesi Artırıldı! Yeni Kapasite: ${updatedP.inventoryCapacity}", "Warehouse Capacity Upgraded! New Capacity: ${updatedP.inventoryCapacity}", NotificationType.SUCCESS)
            }
        } else {
            SmartNotificationManager.show("Yetersiz Bakiye! Gerekli: ₳$cost", "Insufficient Balance! Required: ₳$cost", NotificationType.ALERT)
        }
    }

    fun calculateDistancePublic(city1Id: String, city2Id: String): Float = calculateDistance(city1Id, city2Id)

    fun getEstimatedDistanceKm(originCityId: String, targetCityId: String): Int {
        if (originCityId == targetCityId) return 0
        val dist = calculateDistance(originCityId, targetCityId)
        return (dist * 1150f).toInt().coerceAtLeast(40)
    }

    fun getEstimatedLogisticsDurationSeconds(originCityId: String, targetCityId: String): Int {
        return (calculateLogisticsDuration(originCityId, targetCityId) / 1000L).toInt()
    }

    fun calculateLogisticsDuration(originCityId: String, targetCityId: String): Long {
        if (originCityId == targetCityId) return 5_000L
        val distance = calculateDistance(originCityId, targetCityId)
        // Gerçekçi lojistik formülü: Yakın iller ~12-16 sn, Türkiye çapı ~22-38 sn, Kıtalararası ~50-80 sn
        val baseSeconds = 8L + (distance * 18f).toLong()
        val speedManager = _managers.value.find { it.id == "mgr_logistics" }
        val speedFactor = if (speedManager != null && speedManager.isHired && speedManager.isActive) {
            (1.0f - (speedManager.level * 0.08f)).coerceAtLeast(0.5f)
        } else 1.0f
        return ((baseSeconds * 1000L) * speedFactor).toLong().coerceIn(8_000L, 80_000L)
    }

    fun addActiveDelivery(delivery: com.example.data.DeliveryItem) {
        _activeDeliveries.update { it + delivery }
        try {
            val prod = com.example.data.Product.values().find { it.id == delivery.itemId }
            val prodName = prod?.name?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: delivery.itemId
            val targetCityName = com.example.data.cities.find { it.id.equals(delivery.destinationCityId, ignoreCase = true) }?.name ?: delivery.destinationCityId
            val arrivalTimeMs = delivery.startTimeMs + delivery.totalDurationMs
            com.example.notification.LocalGameNotificationManager.scheduleDeliveryArrival(
                itemId = delivery.itemId,
                itemDisplayName = prodName,
                quantity = delivery.quantity,
                targetCityName = targetCityName,
                targetDeliveryTimeMs = arrivalTimeMs,
                deliveryId = delivery.id.hashCode()
            )
        } catch (_: Throwable) {}
    }

    fun calculateLogisticsCost(originCityId: String, targetCityId: String, quantity: Int, baseTariff: Float = 500f): Long {
        if (originCityId == targetCityId) return 5_000L
        val distance = calculateDistance(originCityId, targetCityId)
        
        // Dinamik Taşıma Fiyatı: Doğrudan borsadaki ham petrol ve işlenmiş akaryakıt fiyatına endeksli
        val currentPrices = marketPrices.value
        val crudeOilPrice = currentPrices.find { it.itemId == "crude_oil" }?.price ?: com.example.data.Product.CRUDE_OIL.basePrice
        val refinedFuelPrice = currentPrices.find { it.itemId == "refined_fuel" }?.price ?: com.example.data.Product.REFINED_FUEL.basePrice

        val crudeRatio = crudeOilPrice.toDouble() / com.example.data.Product.CRUDE_OIL.basePrice.toDouble()
        val fuelRatio = refinedFuelPrice.toDouble() / com.example.data.Product.REFINED_FUEL.basePrice.toDouble()
        val oilMultiplier = ((crudeRatio + fuelRatio) / 2.0).coerceIn(0.3, 5.0)

        var rawCost = distance * baseTariff * quantity * oilMultiplier
        
        val crisis = _consortiumCrisisState.value
        if (crisis.isCrisisActive) {
            rawCost *= crisis.logisticsCostMultiplier
        }

        val logManager = _managers.value.find { it.id == "mgr_logistics" }
        if (logManager != null && logManager.isHired && logManager.isActive) {
            val discount = (logManager.level * 0.15f).coerceAtMost(0.9f) // %15 to %75 discount
            rawCost *= (1.0f - discount)
        }
        return rawCost.toLong().coerceAtLeast(1L)
    }

    
    fun addBuyOrder(itemId: String, quantity: Int, pricePerUnit: Long): Boolean {
        val p = player.value ?: return false
        val uid = if (_onlineEmail.value.isNotBlank()) _onlineEmail.value.replace(".", "_") else if (p.id.isNotBlank() && p.id != "local_player") p.id else "trader_${p.name.hashCode()}"
        val totalCost = quantity * pricePerUnit
        if (p.money < totalCost) return false

        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money - totalCost))
            val rawId = java.util.UUID.randomUUID().toString()
            val newOrderId = if (rawId.startsWith("bo_")) rawId else "bo_$rawId"
            val newOrder = com.example.data.BuyOrder(
                id = newOrderId,
                buyerName = p.name,
                buyerId = uid,
                itemId = itemId,
                quantity = quantity,
                pricePerUnit = pricePerUnit,
                destinationCityId = p.currentCity
            )
            _buyOrders.value = _buyOrders.value + newOrder

            // Supabase ve Realtime WebSocket Canlı Yayını
            repository.syncBuyOrderToSupabase(newOrder)
            com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                com.example.data.network.LiveMarketActionEventDto(
                    actionType = "BUY_ORDER_CREATED",
                    id = newOrderId,
                    playerId = uid,
                    playerName = p.name,
                    itemId = itemId,
                    quantity = quantity,
                    price = pricePerUnit,
                    cityId = p.currentCity
                )
            )

            val prodName = com.example.data.Product.values().find { it.id == itemId }?.getDisplayName() ?: itemId
            logManagerAction("mgr_contracts", "Tedarik Talebi açıldı ($quantity Ton $prodName)", -totalCost)
            logManagerAction("mgr_hr", "İnsan Kaynakları: Tedarik Talebi onaylandı ve bakiye aktarıldı.", -totalCost)
            com.example.ui.components.SmartNotificationManager.show("Tedarik talebi bulut ağına yayınlandı!", com.example.ui.components.NotificationType.SUCCESS)
            saveEconomicDataToDataStore(immediate = true)
        }
        return true
    }

    fun cancelBuyOrder(orderId: String): Boolean {
        val p = player.value ?: return false
        val order = _buyOrders.value.find { it.id == orderId } ?: return false
        val refund = order.quantity * order.pricePerUnit

        viewModelScope.launch {
            _buyOrders.value = _buyOrders.value.filter { it.id != orderId }
            repository.updatePlayer(p.copy(money = p.money + refund))

            // Supabase ve Realtime WebSocket Canlı Yayını
            repository.deleteBuyOrderFromSupabase(orderId)
            com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                com.example.data.network.LiveMarketActionEventDto(
                    actionType = "BUY_ORDER_CANCELLED",
                    id = orderId,
                    playerId = p.id,
                    playerName = p.name,
                    itemId = order.itemId,
                    quantity = order.quantity,
                    price = order.pricePerUnit,
                    cityId = order.destinationCityId
                )
            )

            val prodName = com.example.data.Product.values().find { it.id == order.itemId }?.getDisplayName() ?: order.itemId
            logManagerAction("mgr_contracts", "Tedarik Talebi iptal edildi ($prodName, Bakiye İade)", refund)
            logManagerAction("mgr_hr", "İnsan Kaynakları: İptal edilen tedarik talebi bakiyesi hesaba aktarıldı.", refund)
            com.example.ui.components.SmartNotificationManager.show("Tedarik talebi iptal edildi ve ₳${com.example.ui.components.formatMoney(refund)} iade alındı.", com.example.ui.components.NotificationType.INFO)
        }
        return true
    }

    fun sellToBuyOrder(orderId: String) {
        val p = player.value ?: return
        val order = _buyOrders.value.find { it.id == orderId } ?: return
        val invItem = inventory.value.find { it.itemId == order.itemId }

        if (invItem == null || invItem.quantity < order.quantity) {
            com.example.ui.components.SmartNotificationManager.show("Deponuzda bu talebi karşılayacak miktarda (${order.quantity} Ton) ürün yok!", com.example.ui.components.NotificationType.ALERT)
            return
        }

        viewModelScope.launch {
            repository.consumeItem(order.itemId, order.quantity)

            val totalRevenue = order.quantity * order.pricePerUnit
            val stateTax = (totalRevenue * 0.05f).toLong()
            val earnings = totalRevenue - stateTax

            val hrMgr = _managers.value.find { it.id == "mgr_hr" }
            val hrBonus = if (hrMgr != null && hrMgr.isHired && hrMgr.isActive) (earnings * 0.02f).toLong() else 0L
            val finalEarnings = earnings + hrBonus

            repository.updatePlayer(p.copy(money = p.money + finalEarnings))

            _buyOrders.value = _buyOrders.value.filter { it.id != orderId }

            // Supabase ve Realtime WebSocket Canlı Yayını
            repository.deleteBuyOrderFromSupabase(orderId)
            com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                com.example.data.network.LiveMarketActionEventDto(
                    actionType = "BUY_ORDER_FULFILLED",
                    id = orderId,
                    playerId = p.id,
                    playerName = p.name,
                    itemId = order.itemId,
                    quantity = order.quantity,
                    price = order.pricePerUnit,
                    cityId = order.destinationCityId
                )
            )

            if (order.buyerId.isNotEmpty() && order.buyerId != "local" && order.buyerId != p.id) {
                
            } else if (order.buyerId == p.id || order.buyerName == p.name) {
                repository.insertInventory(com.example.data.InventoryEntity(order.itemId, (inventory.value.find { it.itemId == order.itemId }?.quantity ?: 0) + order.quantity))
            }

            val prodName = com.example.data.Product.values().find { it.id == order.itemId }?.getDisplayName() ?: order.itemId
            logManagerAction("mgr_contracts", "B2B Tedarik Talebi Karşılandı (${order.quantity} Ton $prodName)", finalEarnings)
            logManagerAction("mgr_hr", "İnsan Kaynakları: Manuel tedarik satışı tescillendi ve faturalandırıldı.", finalEarnings)

            val bonusMsg = if (hrBonus > 0) " (İK Primi: +₳${com.example.ui.components.formatMoney(hrBonus)})" else ""
            com.example.ui.components.SmartNotificationManager.show(
                "Tedarik işlemi tamamlandı! +₳${com.example.ui.components.formatMoney(finalEarnings)}$bonusMsg (Vergi: ₳${com.example.ui.components.formatMoney(stateTax)})",
                "Supply transaction completed! +₳${com.example.ui.components.formatMoney(finalEarnings)}$bonusMsg (Tax: ₳${com.example.ui.components.formatMoney(stateTax)})",
                com.example.ui.components.NotificationType.SUCCESS
            )
        }
    }

    fun claimContractReward(rewardMoney: Long, rewardXp: Int) {
        viewModelScope.launch {
            val p = player.value ?: return@launch
            val newMoney = p.money + rewardMoney
            val updatedP = p.copy(money = newMoney)
            repository.updatePlayer(updatedP)
            processXpGain(rewardXp, updatedP)
            com.example.ui.components.SmartNotificationManager.show(
                "İhale teslimatı başarıyla tamamlandı! +${com.example.ui.components.formatMoney(rewardMoney)}, +$rewardXp XP",
                "Tender delivery successfully completed! +${com.example.ui.components.formatMoney(rewardMoney)}, +$rewardXp XP",
                com.example.ui.components.NotificationType.SUCCESS
            )
        }
    }

    fun addAuction(itemId: String, quantity: Int, startingBid: Long) {
        val p = player.value ?: return
        val invItem = inventory.value.find { it.itemId == itemId }
        if (invItem != null && invItem.quantity >= quantity) {
            viewModelScope.launch {
                repository.consumeItem(itemId, quantity)
                val newAuction = com.example.data.Auction(
                    id = java.util.UUID.randomUUID().toString(),
                    sellerName = p.name,
                    sellerId = if (p.id.isNotBlank()) p.id else "local",
                    itemId = itemId,
                    quantity = quantity,
                    startingBid = startingBid,
                    currentBid = 0L,
                    originCityId = p.currentCity,
                    expiresAt = System.currentTimeMillis() + 60000 * 10 // 10 minutes
                )
                val currentList = _auctions.value.toMutableList()
                currentList.add(newAuction)
                _auctions.value = currentList
                saveEconomicDataToDataStore(immediate = true)
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
        }
    }
    
    fun placeMuseumAuctionBid(context: android.content.Context, auctionId: String, bidAmount: Long, onResult: () -> Unit) {
        val p = player.value ?: return
        if (p.money < bidAmount) {
            com.example.ui.components.SmartNotificationManager.show("Yetersiz bakiye!", com.example.ui.components.NotificationType.ALERT)
            return
        }
        
        viewModelScope.launch {
            val serverAuction = com.example.data.SupabaseManager.getMuseumAuctionByIdFromSupabase(auctionId)
            if (serverAuction != null && serverAuction.isSettled) {
                com.example.ui.components.SmartNotificationManager.show("Müzayede durumu değişti! Lütfen listeyi yenileyin.", com.example.ui.components.NotificationType.ALERT)
                return@launch
            }
            if (serverAuction != null && (bidAmount <= serverAuction.currentHighestBid || bidAmount < serverAuction.startingBid)) {
                com.example.ui.components.SmartNotificationManager.show("Teklifiniz çok düşük! (Geçerli en yüksek: ₳${com.example.ui.components.formatMoney(serverAuction.currentHighestBid)})", com.example.ui.components.NotificationType.ALERT)
                return@launch
            }
            
            val currentHigh = serverAuction?.currentHighestBid ?: 0L
            val myUid = if (_onlineEmail.value.isNotBlank() && _onlineEmail.value != "misafir_tuccar") _onlineEmail.value.replace(".", "_")
                else if (p.id.isNotBlank()) p.id
                else "local_player"
            val isAlreadyLeader = (serverAuction?.currentHighestBidderId == myUid)
            val stepAmount = if (isAlreadyLeader && currentHigh > 0L) (bidAmount - currentHigh) else bidAmount

            if (p.money < stepAmount) {
                com.example.ui.components.SmartNotificationManager.show("Yetersiz bakiye! (Gereken: ₳${com.example.ui.components.formatMoney(stepAmount)})", com.example.ui.components.NotificationType.ALERT)
                return@launch
            }
            
            val bidderName = p.name.ifBlank { "Holding" }
            val isBanned = com.example.data.MuseumHeritageManager.isPlayerBannedFromArtifact(context, serverAuction?.artifactId ?: "")
            if (isBanned) {
                com.example.ui.components.SmartNotificationManager.show("Bu eserde önceki ödemenizi yapmadığınız için teklif verme cezalısınız!", com.example.ui.components.NotificationType.ALERT)
                return@launch
            }
            
            if (p.gems < 10) {
                com.example.ui.components.SmartNotificationManager.show("Teklif vermek için 10 Elmas'a ihtiyacınız var!", com.example.ui.components.NotificationType.ALERT)
                return@launch
            }
            
            // Deduct gems
            repository.updatePlayer(p.copy(gems = p.gems - 10))



            com.example.data.MuseumHeritageManager.bidOnAuction(context, auctionId, bidAmount, myUid, bidderName)
            com.example.ui.components.SmartNotificationManager.show("Müzayedeye ₳${com.example.ui.components.formatMoney(bidAmount)} pey sürdünüz!", com.example.ui.components.NotificationType.SUCCESS)
            onResult()
        }
    }

    fun placeBid(auctionId: String, bidAmount: Long) {
        val p = player.value ?: return
        if (p.money < bidAmount) return
        
        val auctionIndex = _auctions.value.indexOfFirst { it.id == auctionId }
        if (auctionIndex != -1) {
            val auction = _auctions.value[auctionIndex]
            if (bidAmount > auction.currentBid && bidAmount > auction.startingBid) {
                viewModelScope.launch {
                    repository.updatePlayer(p.copy(money = p.money - bidAmount)) // Escrow
                    
                    val updatedAuction = auction.copy(
                        currentBid = bidAmount,
                        currentBidderId = if (p.id.isNotBlank()) p.id else "local",
                        currentBidderName = p.name
                    )
                    
                    val currentList = _auctions.value.toMutableList()
                    currentList[auctionIndex] = updatedAuction
                    _auctions.value = currentList
                    saveEconomicDataToDataStore(immediate = true)
                    syncCloudSaveToSupabase(force = true, immediate = true)
                    com.example.ui.components.SmartNotificationManager.show("Teklif verildi!", com.example.ui.components.NotificationType.SUCCESS)
                }
            }
        }
    }

    fun updateListingPrice(listingId: String, newPrice: Long) {
        val currentListings = _marketListings.value.toMutableList()
        val index = currentListings.indexOfFirst { it.id == listingId }
        if (index != -1) {
            val listing = currentListings[index]
            if (listing.sellerName == player.value?.name) {
                currentListings[index] = listing.copy(pricePerUnit = newPrice)
                _marketListings.value = currentListings
                com.example.ui.components.SmartNotificationManager.show("İlan fiyatı ₳${com.example.ui.components.formatMoney(newPrice)} olarak güncellendi.", com.example.ui.components.NotificationType.SUCCESS)
            }
        }
    }

    fun addMarketListing(itemId: String, quantity: Int, pricePerUnit: Long, qualityTier: String = "Standart"): Boolean {
        val p = player.value ?: return false
        val pName = p.name
        val pCity = p.currentCity
        val uid = if (_onlineEmail.value.isNotBlank()) _onlineEmail.value.replace(".", "_") else if (p.id.isNotBlank() && p.id != "local_player") p.id else "trader_${pName.hashCode()}"
        if (quantity <= 0 || pricePerUnit <= 0) return false
        val invItem = inventory.value.find { it.itemId == itemId }
        if (invItem != null && invItem.quantity < quantity) {
            SmartNotificationManager.show("Yetersiz stok! Pazara vermek istediğiniz miktarda ürün deponuzda yok.", NotificationType.ALERT)
            return false
        }

        val totalVal = quantity * pricePerUnit
        val brokerFee = (totalVal * 0.015).toLong()
        if (p.money < brokerFee) {
            SmartNotificationManager.show("Komisyon için yetersiz bakiye!", NotificationType.ALERT)
            return false
        }

        val nowMs = System.currentTimeMillis()
        val newListingId = java.util.UUID.randomUUID().toString()
        val newListing = com.example.data.MarketListing(
            id = newListingId,
            sellerName = pName,
            sellerId = uid,
            itemId = itemId,
            quantity = quantity,
            pricePerUnit = pricePerUnit,
            originCityId = pCity,
            qualityTier = qualityTier,
            createdAt = nowMs
        )
        val pendingSale = com.example.data.PendingMarketSaleEntity(
            id = newListingId,
            sellerName = pName,
            sellerId = uid,
            itemId = itemId,
            quantity = quantity,
            pricePerUnit = pricePerUnit,
            originCityId = pCity,
            createdAt = nowMs
        )

        val previousMoney = p.money
        val previousListings = _marketListings.value

        viewModelScope.launch {
            try {
                // 1. Client-Side Prediction: Anında bakiye ve stok düşüşü, yerel pazar listesine ekleme
                repository.updatePlayer(p.copy(money = p.money - brokerFee))
                repository.consumeItem(itemId, quantity)
                _marketListings.value = _marketListings.value + newListing
                repository.insertPendingSale(pendingSale)

                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
                SmartNotificationManager.show("İlan pazara eklendi: $quantity adet $itemId", NotificationType.SUCCESS)

                // 2. Arka Planda Sunucu (Supabase) Senkronizasyonu
                val syncSuccess = com.example.data.SupabaseManager.syncMarketListingToSupabase(newListing)
                if (!syncSuccess) {
                    throw Exception("Supabase senkronizasyon hatası")
                }
                com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                    com.example.data.network.LiveMarketActionEventDto(
                        actionType = "LISTING_CREATED",
                        id = newListingId,
                        playerId = uid,
                        playerName = pName,
                        itemId = itemId,
                        quantity = quantity,
                        price = pricePerUnit,
                        cityId = pCity
                    )
                )
                saveEconomicDataToDataStore(immediate = true)
            } catch (e: Exception) {
                // 3. Rollback Mekanizması: Sunucu hatasında parayı, stoku ve pazar listesini eski haline döndür
                android.util.Log.e("GameViewModel", "addMarketListing server sync failed, rolling back", e)
                repository.updatePlayer(p.copy(money = previousMoney))
                repository.produceItem(itemId, quantity)
                _marketListings.value = previousListings
                repository.deletePendingSale(newListingId)

                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.ERROR)
                SmartNotificationManager.show("⚠️ İlan sunucuya eklenemedi, ürünler ve komisyon iade edildi.", NotificationType.ALERT)
            }
        }
        return true
    }

    fun cancelMarketListing(listingId: String): Boolean {
        val listing = _marketListings.value.find { it.id == listingId } ?: return false
        val previousListings = _marketListings.value

        viewModelScope.launch {
            try {
                // 1. Client-Side Prediction: Anında ilanı listeden kaldır ve ürünleri depoya iade et
                _marketListings.value = _marketListings.value.filterNot { it.id == listingId }
                repository.produceItem(listing.itemId, listing.quantity)
                repository.deletePendingSale(listingId)

                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
                SmartNotificationManager.show("İlan iptal edildi ve ürünler deponuza iade edildi.", NotificationType.SUCCESS)

                // 2. Arka Planda Sunucu (Supabase) Silme İsteği
                val deleteSuccess = com.example.data.SupabaseManager.deleteMarketListingFromSupabase(listingId)
                if (!deleteSuccess) {
                    throw Exception("Supabase silme hatası")
                }
                com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                    com.example.data.network.LiveMarketActionEventDto(
                        actionType = "LISTING_CANCELLED",
                        id = listingId,
                        playerId = player.value?.id ?: "",
                        playerName = player.value?.name ?: "",
                        itemId = listing.itemId,
                        quantity = listing.quantity,
                        price = listing.pricePerUnit,
                        cityId = listing.originCityId
                    )
                )
            } catch (e: Exception) {
                // 3. Rollback Mekanizması: Sunucu hatasında ilanı tekrar listeye koy ve ürünleri depodan geri düş
                android.util.Log.e("GameViewModel", "cancelMarketListing server sync failed, rolling back", e)
                _marketListings.value = previousListings
                repository.consumeItem(listing.itemId, listing.quantity)

                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.ERROR)
                SmartNotificationManager.show("⚠️ İlan iptali sunucu hatası nedeniyle geri alındı.", NotificationType.ALERT)
            }
        }
        return true
    }

    fun buyFromGlobalMarket(listingId: String, requestedQuantity: Int? = null, targetCityId: String? = null) {
        val p = player.value ?: return
        val currentCity = targetCityId ?: p.currentCity
        val inventoryCapacity = p.inventoryCapacity
        val currentInvCount = inventory.value.sumOf { it.quantity }
        val listing = marketListings.value.find { it.id == listingId } ?: run {
            SmartNotificationManager.show("Geçersiz veya süresi dolmuş ilan!", NotificationType.ALERT)
            return
        }

        val actualBuyQty = requestedQuantity?.coerceAtMost(listing.quantity) ?: listing.quantity
        if (currentInvCount + actualBuyQty > inventoryCapacity) {
            SmartNotificationManager.show("Depo kapasitesi yetersiz!", NotificationType.ALERT)
            return
        }

        val logisticsCost = calculateLogisticsCost(listing.originCityId, currentCity, actualBuyQty)
        val totalCost = (listing.pricePerUnit * actualBuyQty) + logisticsCost
        if (p.money < totalCost) {
            SmartNotificationManager.show("Yetersiz bakiye! (₳${com.example.ui.components.formatMoney(totalCost)} gerekli)", NotificationType.ALERT)
            return
        }

        val previousMoney = p.money
        val previousListings = _marketListings.value

        viewModelScope.launch {
            try {
                // 1. Client-Side Prediction: Anında bakiye düşüşü, depoya ürün ekleme ve pazar listesini güncelleme
                repository.updatePlayer(p.copy(money = p.money - totalCost))
                repository.produceItem(listing.itemId, actualBuyQty)

                val remainingQty = listing.quantity - actualBuyQty
                if (remainingQty <= 0) {
                    _marketListings.value = _marketListings.value.filterNot { it.id == listing.id }
                } else {
                    _marketListings.value = _marketListings.value.map {
                        if (it.id == listing.id) it.copy(quantity = remainingQty) else it
                    }
                }

                val product = com.example.data.Product.values().find { it.id == listing.itemId }
                val itemTitle = product?.getDisplayName() ?: listing.itemId.uppercase()

                val originCityId = listing.originCityId.ifBlank { "istanbul" }
                val durationMs = calculateLogisticsDuration(originCityId, currentCity)
                addActiveDelivery(
                    com.example.data.DeliveryItem(
                        itemId = listing.itemId,
                        quantity = actualBuyQty,
                        originCityId = originCityId,
                        destinationCityId = currentCity,
                        pricePerUnit = listing.pricePerUnit,
                        totalCost = totalCost,
                        startTimeMs = System.currentTimeMillis(),
                        totalDurationMs = durationMs,
                        isOutboundSale = false
                    )
                )

                com.example.ui.components.ParticleManager.spawnCelebration()
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
                val originName = com.example.data.cities.find { it.id == originCityId }?.name ?: originCityId.uppercase()
                val destName = com.example.data.cities.find { it.id == currentCity }?.name ?: currentCity.uppercase()
                SmartNotificationManager.show("🚚 Pazar Sevkiyatı Yola Çıktı! Satıcı Depo ($originName) ➔ Merkez Depo ($destName): $actualBuyQty Ton $itemTitle", NotificationType.SUCCESS)
                markFirstTradeCompleted()

                // 2. Arka Planda Sunucu (Supabase) Güncelleme/Silme
                val serverSuccess = if (remainingQty <= 0) {
                    com.example.data.SupabaseManager.deleteMarketListingFromSupabase(listing.id)
                } else {
                    val updatedListing = listing.copy(quantity = remainingQty)
                    com.example.data.SupabaseManager.syncMarketListingToSupabase(updatedListing)
                }

                if (!serverSuccess) {
                    throw Exception("Sunucu işlemi onaylamadı.")
                }
                com.example.data.MultiplayerManager.sendBroadcastMarketAction(
                    com.example.data.network.LiveMarketActionEventDto(
                        actionType = "LISTING_BOUGHT",
                        id = listing.id,
                        playerId = player.value?.id ?: "",
                        playerName = player.value?.name ?: "",
                        itemId = listing.itemId,
                        quantity = actualBuyQty,
                        price = listing.pricePerUnit,
                        cityId = listing.originCityId
                    )
                )
                saveEconomicDataToDataStore(immediate = true)
            } catch (e: Exception) {
                // 3. Rollback Mekanizması: Sunucu hatasında parayı iade et, ürünü geri düş ve pazar listesini eski haline getir
                android.util.Log.e("GameViewModel", "buyFromGlobalMarket failed, rolling back", e)
                repository.updatePlayer(p.copy(money = previousMoney))
                repository.consumeItem(listing.itemId, actualBuyQty)
                _marketListings.value = previousListings

                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.ERROR)
                SmartNotificationManager.show("⚠️ Satın alma işlemi sunucuda başarısız oldu. Bakiye ve envanter iade edildi.", NotificationType.ALERT)
            }
        }
    }

    fun syncGlobalMarketBundle(bundle: com.example.data.SupabaseManager.GlobalMarketBundle) {
        viewModelScope.launch {
            val p = player.value ?: return@launch
            val myUid = if (_onlineEmail.value.isNotBlank()) _onlineEmail.value.replace(".", "_") else if (p.id.isNotBlank() && p.id != "local_player") p.id else "trader_${p.name.hashCode()}"
            val myName = p.name
            var needSave = false

            // 1. Pazarda satılan kendi satış ilanlarımızı kontrol et
            val pendingSales = repository.getPendingSales()
            var totalRevenueEarned = 0L
            val soldDescriptions = mutableListOf<String>()

            if (pendingSales.isNotEmpty()) {
                val remoteListingIds = bundle.listings.map { it.id }.toSet()
                for (sale in pendingSales) {
                    val ageMs = System.currentTimeMillis() - sale.createdAt
                    if (ageMs > 3000L && sale.id !in remoteListingIds) {
                        val revenue = sale.quantity.toLong() * sale.pricePerUnit
                        totalRevenueEarned += revenue
                        val product = com.example.data.Product.values().find { it.id == sale.itemId }
                        val itemName = product?.getDisplayName() ?: sale.itemId
                        soldDescriptions.add("${sale.quantity} Ton $itemName (₳${com.example.ui.components.formatMoney(revenue)})")
                        repository.deletePendingSale(sale.id)
                    }
                }
            }

            if (totalRevenueEarned > 0L) {
                val updatedPlayer = p.copy(
                    money = p.money + totalRevenueEarned,
                    totalProfit = p.totalProfit + totalRevenueEarned
                )
                repository.updatePlayer(updatedPlayer)
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
                com.example.ui.components.ParticleManager.spawnCelebration()
                val details = soldDescriptions.joinToString(", ")
                com.example.ui.components.SmartNotificationManager.show(
                    "🎉 Pazarda İlanınız Satıldı! $details satıldı ve ₳${com.example.ui.components.formatMoney(totalRevenueEarned)} kasanıza eklendi.",
                    com.example.ui.components.NotificationType.SUCCESS
                )
                needSave = true
            }

            // 2. Başka oyuncular tarafından karşılanan Tedarik Taleplerimizi kontrol et
            val remoteBuyOrderIds = bundle.buyOrders.map { it.id }.toSet()
            val deliveredItems = mutableListOf<Pair<String, Int>>()
            val updatedLocalBuyOrders = _buyOrders.value.filter { myOrder ->
                val isMine = myOrder.buyerId == myUid || (myOrder.buyerName.isNotBlank() && myOrder.buyerName.equals(myName, ignoreCase = true))
                if (isMine) {
                    val ageMs = System.currentTimeMillis() - myOrder.createdAt
                    if (ageMs > 3000L && myOrder.id !in remoteBuyOrderIds) {
                        // Siparişimiz başka bir oyuncu tarafından karşılanmış ve teslim edilmiş!
                        deliveredItems.add(myOrder.itemId to myOrder.quantity)
                        return@filter false
                    }
                }
                true
            }

            for ((itemId, qty) in deliveredItems) {
                val currentQty = inventory.value.find { it.itemId == itemId }?.quantity ?: 0
                repository.insertInventory(com.example.data.InventoryEntity(itemId, currentQty + qty))
                val prodName = com.example.data.Product.values().find { it.id == itemId }?.getDisplayName() ?: itemId
                com.example.ui.components.SmartNotificationManager.show(
                    "🎉 Tedarik Talebiniz Karşılandı! $qty Ton $prodName deponuza teslim edildi.",
                    com.example.ui.components.NotificationType.SUCCESS
                )
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
                com.example.ui.components.ParticleManager.spawnCelebration()
                needSave = true
            }

            // 3. Başka oyuncular tarafından karşılanan Vadeli Sözleşmelerimizi kontrol et
            val remoteFuturesIds = bundle.futuresContracts.map { it.id }.toSet()
            val deliveredFuturesItems = mutableListOf<Pair<String, Int>>()
            val updatedLocalFutures = _futuresContracts.value.filter { myContract ->
                val isMine = myContract.creatorId == myUid || (myContract.creatorName.isNotBlank() && myContract.creatorName.equals(myName, ignoreCase = true))
                if (isMine) {
                    val ageMs = System.currentTimeMillis() - myContract.createdAt
                    if (ageMs > 3000L && myContract.id !in remoteFuturesIds) {
                        // Vadeli sözleşmemiz teslim edilmiş!
                        deliveredFuturesItems.add(myContract.itemId to myContract.quantity)
                        return@filter false
                    }
                }
                true
            }

            for ((itemId, qty) in deliveredFuturesItems) {
                val currentQty = inventory.value.find { it.itemId == itemId }?.quantity ?: 0
                repository.insertInventory(com.example.data.InventoryEntity(itemId, currentQty + qty))
                val prodName = com.example.data.Product.values().find { it.id == itemId }?.getDisplayName() ?: itemId
                com.example.ui.components.SmartNotificationManager.show(
                    "🎉 Vadeli Sözleşmeniz Teslim Edildi! $qty Ton $prodName deponuza ulaştı.",
                    com.example.ui.components.NotificationType.SUCCESS
                )
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
                com.example.ui.components.ParticleManager.spawnCelebration()
                needSave = true
            }

            // 4. Pazar listelerini harmanla
            val currentPendingSales = repository.getPendingSales()
            val remoteListingIds = bundle.listings.map { it.id }.toSet()
            val localPendingListings = currentPendingSales
                .filter { it.id !in remoteListingIds }
                .map { sale ->
                    com.example.data.MarketListing(
                        id = sale.id,
                        sellerName = sale.sellerName,
                        sellerId = sale.sellerId,
                        itemId = sale.itemId,
                        quantity = sale.quantity,
                        pricePerUnit = sale.pricePerUnit,
                        originCityId = sale.originCityId,
                        qualityTier = "Standart",
                        createdAt = sale.createdAt
                    )
                }

            _marketListings.value = (bundle.listings + localPendingListings).distinctBy { it.id }

            // 5. Vadeli Sözleşmeler ve Tedarik Taleplerini güncelle
            val myActiveBuyOrders = updatedLocalBuyOrders.filter { it.buyerId == myUid || it.buyerName.equals(myName, ignoreCase = true) }
            _buyOrders.value = (bundle.buyOrders + myActiveBuyOrders).distinctBy { it.id }

            val myActiveFutures = updatedLocalFutures.filter { it.creatorId == myUid || it.creatorName.equals(myName, ignoreCase = true) }
            _futuresContracts.value = (bundle.futuresContracts + myActiveFutures).distinctBy { it.id }

            if (needSave) {
                saveEconomicDataToDataStore(immediate = true)
            }
        }
    }

    fun syncMarketListingsFromRemote(remoteListings: List<com.example.data.MarketListing>) {
        syncGlobalMarketBundle(com.example.data.SupabaseManager.GlobalMarketBundle(remoteListings, _futuresContracts.value, _buyOrders.value))
    }

    internal var lastMarketRefreshTime = 0L
    internal val marketRefreshMutex = kotlinx.coroutines.sync.Mutex()

    fun refreshGlobalMarket() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (!marketRefreshMutex.tryLock()) {
                return@launch
            }
            try {
                val now = System.currentTimeMillis()
                if (now - lastMarketRefreshTime < 1500L) {
                    return@launch
                }
                lastMarketRefreshTime = now

                val bundle = repository.fetchGlobalMarketBundleOnce()
                if (bundle != null) {
                    syncGlobalMarketBundle(bundle)
                }
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Failed to refresh global market", e)
            } finally {
                marketRefreshMutex.unlock()
            }
        }
    }

    fun updateUiState(updater: GameUiState.() -> GameUiState) {
        _uiState.update { current ->
            val updated = current.updater()
            if (updated == current) current else updated
        }
    }


    internal var lastCloudSyncTimeMs: Long = 0L

    internal val localMegaProjectUpdates = mutableMapOf<String, Long>()
    internal var lastMegaProjectSyncMs = 0L

    internal fun syncMegaProjectsFromRemote(remoteProjects: List<com.example.data.MegaProject>) {
        if (remoteProjects.isEmpty()) return
        val currentLocal = _megaProjects.value
        val now = System.currentTimeMillis()
        
        val mergedMap = remoteProjects.associateBy { it.id }.toMutableMap()
        
        currentLocal.forEach { local ->
            val localUpdatedMs = localMegaProjectUpdates[local.id] ?: 0L
            val isRecentlyUpdated = (now - localUpdatedMs) <= 5000L
            val isPlayerOwned = (local.leaderPlayerId == "local_player" || local.leaderPlayerId == player.value?.id)
            
            if (isRecentlyUpdated) {
                // Keep recent local changes to avoid overwriting from stale remote
                mergedMap[local.id] = local
            } else if (!mergedMap.containsKey(local.id) && isPlayerOwned) {
                // Keep player's own projects that aren't in remote yet, and trigger a sync for them
                mergedMap[local.id] = local
                syncMegaProject(local, force = true)
            }
        }
        
        _megaProjects.value = mergedMap.values.toList()
    }

    internal fun syncMegaProject(proj: com.example.data.MegaProject, force: Boolean = false) {
        val now = System.currentTimeMillis()
        localMegaProjectUpdates[proj.id] = now
        viewModelScope.launch(Dispatchers.IO) {
            val ok = com.example.data.SupabaseManager.syncMegaProjectToSupabase(proj)
            if (ok) {
                com.example.data.MultiplayerManager.sendBroadcastConsortiumAction(
                    com.example.data.network.LiveConsortiumActionEventDto(
                        projectId = proj.id,
                        playerId = player.value?.id ?: "local_player",
                        playerName = player.value?.name ?: "Tüccar",
                        moveType = "CONSORTIUM_SYNC",
                        quantityDelivered = proj.totalItemsProduced.toLong(),
                        details = proj.consortiumName
                    )
                )
            }
        }
    }


    fun checkAndClaimMonthlyLeaderboardReward(forceManualCheck: Boolean = false) {
        viewModelScope.launch {
            val p = player.value ?: return@launch
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MONTH, -1)
            val pastMonthKey = "${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.MONTH) + 1}"

            val lastClaimed = repository.getLastClaimedMonthlyRewardKey()
            if (lastClaimed == pastMonthKey && !forceManualCheck) {
                return@launch
            }

            val pastPlayers = com.example.data.MultiplayerManager.pastMonthLeaderboard.value
            val authName = p.name
            val rankIndex = pastPlayers.indexOfFirst {
                it.name.equals(authName, ignoreCase = true) ||
                it.name.equals(p.name, ignoreCase = true) ||
                it.companyName.contains(p.name, ignoreCase = true)
            }

            if (rankIndex in 0..2) {
                val rewardGems = when (rankIndex) {
                    0 -> 2000
                    1 -> 1000
                    2 -> 500
                    else -> 0
                }
                if (rewardGems > 0) {
                    val newGems = p.gems + rewardGems
                    repository.updatePlayer(p.copy(gems = newGems))
                    repository.setLastClaimedMonthlyRewardKey(pastMonthKey)
                    SmartNotificationManager.show(
                        "🎉 TEBRİKLER! Geçmiş ay liginde ${rankIndex + 1}. oldunuz! +$rewardGems 💎 Elmas hesabınıza aktarıldı!",
                        NotificationType.SUCCESS
                    )
                    return@launch
                }
            }
            if (forceManualCheck) {
                if (lastClaimed == pastMonthKey) {
                    SmartNotificationManager.show("Geçmiş ay ($pastMonthKey) ligi ödülünüz daha önce hesabınıza aktarılmıştır.", NotificationType.INFO)
                } else {
                    SmartNotificationManager.show("Geçmiş ay ligi sonuçlarında ilk 3 sıralamasına giremediğiniz için hak edilmiş elmas ödülü bulunmuyor (1. 2000💎, 2. 1000💎, 3. 500💎).", NotificationType.INFO)
                }
            }
        }
    }


    fun addMoneyDirectly(amount: Long) {
        val p = player.value ?: return
        viewModelScope.launch {
            repository.updatePlayer(p.copy(money = p.money + amount))
            saveEconomicDataToDataStore()
        }
    }

    /**
     * Optimistic UI Offer Acceptance Engine:
     * 1. Instantly adds money locally and triggers ParticleManager animation.
     * 2. Dispatches offer payload to server in a background coroutine.
     * 3. Performs state rollback and notifies player via SmartNotificationManager if server fails/rejects.
     */
    fun acceptOfferOptimistic(
        offerId: String,
        baseAmount: Long,
        consortiumId: String? = null
    ) {
        val p = player.value ?: return
        val originalMoney = p.money

        // Step 1: Optimistic Local Money Addition & Particle Effect
        addMoneyDirectly(baseAmount)
        com.example.ui.components.ParticleManager.spawnCelebration()

        // Step 2: Async Server Verification & Synchronization
        viewModelScope.launch {
            try {
                val payload = com.example.data.network.AcceptOfferRequestDto(
                    offerId = offerId,
                    buyerUid = p.id,
                    rawBaseAmount = baseAmount,
                    consortiumId = consortiumId
                )

                val response = repository.processOfferAcceptanceOnServer(payload)

                if (!response.success) {
                    throw IllegalStateException(response.errorMessage ?: "Teklif başkası tarafından alındı veya süresi doldu.")
                }

                // If server computed extra consortium returns (+25% bonus etc.), add difference
                if (response.finalAmount > baseAmount) {
                    val bonusDifference = response.finalAmount - baseAmount
                    addMoneyDirectly(bonusDifference)
                }

                com.example.ui.components.SmartNotificationManager.show(
                    "Teklif başarıyla kabul edildi! (+₳${com.example.ui.components.formatMoney(response.finalAmount)})",
                    com.example.ui.components.NotificationType.SUCCESS
                )

            } catch (e: Exception) {
                // Step 3: Rollback on Server Rejection
                val currentP = player.value
                if (currentP != null) {
                    repository.updatePlayer(currentP.copy(money = originalMoney))
                    saveEconomicDataToDataStore()
                }

                com.example.ui.components.SmartNotificationManager.show(
                    "İşlem Başarısız: ${e.localizedMessage ?: "Teklif onaylanamadı"}",
                    com.example.ui.components.NotificationType.ALERT
                )
            }
        }
    }
    

    private var isClaimingDailyReward = false

    fun checkDailyLoginBonus(context: android.content.Context? = null) {
        if (isClaimingDailyReward) return
        isClaimingDailyReward = true
        viewModelScope.launch {
            try {
                if (!_isOnlineRegistered.value) {
                    SmartNotificationManager.show("Günlük nakit ödülü yalnızca çevrimiçi (online) giriş yapan tüccarlara özeldir! Lütfen giriş yapın.", "Daily cash reward is exclusive to online logged-in merchants! Please log in.", NotificationType.ALERT)
                    return@launch
                }
            val p = player.value ?: return@launch
            val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()

            if (p.lastDailyRewardMs > 0L && now < p.lastDailyRewardMs) {
                SmartNotificationManager.show(
                    "Cihaz saati manipülasyonu tespit edildi! Lütfen telefon saatinizi otomatik ağ saatine ayarlayın.",
                    "Device clock manipulation detected! Please set your device clock to automatic network time.",
                    NotificationType.ALERT
                )
                return@launch
            }

            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayMidnightMs = calendar.timeInMillis

            if (p.lastDailyRewardMs >= todayMidnightMs) {
                val nextMidnightMs = todayMidnightMs + 24 * 60 * 60 * 1000L
                _nextDailyRewardRemainingMs.value = (nextMidnightMs - now).coerceAtLeast(0L)
                SmartNotificationManager.show("Bugünkü günlük nakit ödülünüzü zaten aldınız. Gece 00:00'dan sonra tekrar alabilirsiniz!", "You already claimed today's daily cash reward. You can claim again after 00:00!", NotificationType.INFO)
                return@launch
            }

            val yesterdayMidnightMs = todayMidnightMs - 24 * 60 * 60 * 1000L
            val newStreak = if (p.lastDailyRewardMs >= yesterdayMidnightMs) p.loginStreak + 1 else 1

            val lvlMult = 1.0f + (p.level - 1) * 0.20f
            var moneyReward = (500_000L * newStreak.coerceAtMost(7) * lvlMult).toLong()
            var gemReward = (10 + (p.level / 3).coerceAtMost(30)) / 2
            
            // Calculate museum artifact diamond yield
            val ownedArtifacts = com.example.data.MuseumHeritageManager.getOwnedArtifacts(context)
            var museumGemYield = 0
            if (ownedArtifacts.isNotEmpty()) {
                museumGemYield = ownedArtifacts.sumOf { (it.baseValue / 15_000_000_000L).toInt().coerceIn(1, 10) }
                gemReward += museumGemYield
            }
            
            var notificationText = "Günlük Online Nakit Ödülü (Sv.${p.level} • Gün $newStreak): +₳${com.example.ui.components.formatMoney(moneyReward)}\nGünlük Elmas: +$gemReward💎"
            if (museumGemYield > 0) {
                notificationText += " (Müze Sergisi: +$museumGemYield💎)"
            }


            

            val updatedPlayer = p.copy(
                money = p.money + moneyReward,
                gems = p.gems + gemReward,
                lastDailyRewardMs = now,
                loginStreak = newStreak
            )

            repository.updatePlayer(updatedPlayer)
            saveEconomicDataToDataStore(immediate = true)
            forceSyncCloudSaveToSupabase() // Back up reward log and info to cloud

            _dailyRewardDialogData.value = DailyRewardData(newStreak, notificationText, p.isVip)
            _nextDailyRewardRemainingMs.value = 24 * 60 * 60 * 1000L
            SmartNotificationManager.show("🎁 $notificationText (Buluta Yedeklendi)", "🎁 $notificationText (Backed up to Cloud)", NotificationType.SUCCESS)
        } finally {
            isClaimingDailyReward = false
        }
        }
    }



    internal fun generateDynamicNewsWire(currentSeason: String = "İlkbahar", currentEvent: String = "Normal") {
        val newsItems = mutableListOf<String>()

        // 1. Olay & Sektör Haberleri
        when (currentEvent) {
            "Don (Tarım Düşüşü)" -> newsItems.add("SON DAKİKA: Trakya ve İç Anadolu'da zirai don uyarısı! Tarım emtiaları yükselişte.")
            "Kuraklık (Tahıl Azalır)" -> newsItems.add("KÜRESEL İKLİM: Kuraklık tahıl rekoltesini etkiliyor! Buğday ve un fiyatları hareketlendi.")
            "Ticaret Anlaşması (Fiyat Artışı)" -> newsItems.add("MÜJDE: Avrupa ve Körfez ülkeleriyle yeni ticaret koridoru açıldı. İhracat hacmi artıyor.")
            "Turizm Patlaması" -> newsItems.add("TURİZM REKORU: Akdeniz ve Ege kıyılarına rekor turist akını! Gıda ve hizmet tüketimi katlandı.")
            "Merkez Bankası Müdahalesi" -> newsItems.add("TCMB RAPORU: Merkez Bankası para piyasalarında likidite dengeleme adımı attı.")
            "Ekonomik Büyüme" -> newsItems.add("YÜKSELİŞ: Sanayi üretim endeksi beklentileri aştı! İmalat sektöründe canlılık sürüyor.")
            "Süveyş Navlun Artışı" -> newsItems.add("KÜRESEL LOJİSTİK: Navlun ve konteyner maliyetleri güncellendi, iç pazar üretimi avantaj kazandı.")
            "Yeşil Enerji Teşvik Paketi" -> newsItems.add("SANAYİ HAMLESİ: Yeşil fabrika ve güneş enerjisi yatırımlarına sıfır faizli kredi desteği.")
            "Milli Teknoloji Hamlesi" -> newsItems.add("AR-GE & SAVUNMA: İleri teknoloji çip ve batarya üretiminde stratejik alım anlaşması.")
            "Otomotiv İhracat Rekoru" -> newsItems.add("İHRACAT ŞAMPİYONU: Otomotiv ve beyaz eşya sektörü Avrupa pazarında yeni rekor kırdı.")
            else -> newsItems.add("PİYASA DENGESİ: Anadolu ticaret hatlarında sevkiyatlar kesintisiz sürüyor.")
        }

        // 2. Makro Ekonomi Haberi
        val crisis = _consortiumCrisisState.value
        if (crisis.isCrisisActive) {
            newsItems.add("🚨 ALARM: ${crisis.crisisTitle} - Hammadde maliyetleri +%${((crisis.rawMaterialInflationMultiplier - 1f) * 100).toInt()} arttı!")
            if (crisis.criticalMissingMaterials.isNotEmpty()) {
                val missingNames = crisis.criticalMissingMaterials.mapNotNull { Product.values().find { p -> p.id == it }?.getDisplayName() }.joinToString(", ")
                if (missingNames.isNotBlank()) {
                    newsItems.add("⚠️ KRİTİK KITLIK: Konsorsiyumlarda en çok $missingNames hammaddelerinde tedarik krizi yaşanıyor!")
                }
            }
        } else {
            val macroNews = when (macroState.cycle) {
                com.example.data.EconomicCycle.BOOM -> "EKONOMİ BOĞA: Tüketici talebi ve şirket kârlılıkları zirvede! Lüks tüketim yükselişte."
                com.example.data.EconomicCycle.PEAK -> "PİYASA ZİRVEDE: Enflasyon baskısına karşı TCMB faiz ve likidite tedbirlerini sıkılaştırıyor."
                com.example.data.EconomicCycle.RECESSION -> "RESESYON DÖNGÜSÜ: Temel gıda ve enerji emtiaları güvenli liman olmaya devam ediyor."
                com.example.data.EconomicCycle.DEPRESSION -> "TARİHİ ALIM DÖNEMİ: Emtia fiyatları dipte, tesis yatırımı ve stok toplamak için büyük fırsat."
                com.example.data.EconomicCycle.RECOVERY -> "TOPARLANMA DÖNEMİ: Borsa İstanbul ve emtia piyasalarında güven endeksi hızla yükseliyor."
            }
            newsItems.add(macroNews)
        }

        // 3. Şehir Olayları
        val activeCityNews = _activeCityEvents.value.filter { !it.isExpired }
        if (activeCityNews.isNotEmpty()) {
            val trHaberler = activeCityNews.map { "TR HABER: ${it.cityName} - ${it.headline} (${it.percentFormatted})" }
            newsItems.addAll(trHaberler)
        }

        // 4. Borsa Emtia Flaşları
        val sortedByPriceRatio = marketPrices.value.mapNotNull { price ->
            val product = com.example.data.Product.values().find { it.id == price.itemId }
            if (product != null) Pair(product, price.price.toDouble() / product.basePrice) else null
        }
        val highestSpike = sortedByPriceRatio.maxByOrNull { it.second }
        val lowestDrop = sortedByPriceRatio.minByOrNull { it.second }

        if (highestSpike != null && highestSpike.second > 1.20) {
            newsItems.add("BORSA FLAŞ: ${highestSpike.first.getDisplayName()} fiyatları %${((highestSpike.second - 1.0) * 100).toInt()} prim yaptı!")
        }
        if (lowestDrop != null && lowestDrop.second < 0.85) {
            newsItems.add("PİYASA DİP: ${lowestDrop.first.getDisplayName()} fiyatı geriledi, üreticiler için ucuz hammadde fırsatı!")
        }

        // 5. Merkez Bankası Faiz Oranı
        newsItems.add("TCMB FAİZİ: Politika Faizi %${String.format(java.util.Locale.US, "%.1f", macroState.centralBankInterestRate * 100)} seviyesinde.")

        _newsTickerMessage.value = newsItems.shuffled().joinToString(" | ")
    }


    fun getDynamicProductionCost(product: Product, cityId: String? = null): Long {
        val prices = marketPrices.value
        val smartGridPrice = prices.find { it.itemId == "smart_grid" }?.price ?: Product.SMART_GRID.basePrice
        val solarPanelPrice = prices.find { it.itemId == "solar_panel" }?.price ?: Product.SOLAR_PANEL.basePrice
        val batteryPrice = prices.find { it.itemId == "battery" }?.price ?: Product.BATTERY.basePrice

        val ratioSmartGrid = (smartGridPrice.toDouble() / Product.SMART_GRID.basePrice.toDouble()).toFloat()
        val ratioSolarPanel = (solarPanelPrice.toDouble() / Product.SOLAR_PANEL.basePrice.toDouble()).toFloat()
        val ratioBattery = (batteryPrice.toDouble() / Product.BATTERY.basePrice.toDouble()).toFloat()

        // Akıllı Şehir Enerji Şebekesi, Güneş Paneli ve Lityum Akü Borsa Fiyat Endeksi (0.5x - 3.0x)
        val energyIndexMultiplier = (ratioSmartGrid * 0.40f + ratioSolarPanel * 0.35f + ratioBattery * 0.25f)
            .coerceIn(0.50f, 3.00f)

        val city = cityId?.let { id -> com.example.data.cities.find { it.id == id } }
        val cityCostMultiplier = city?.laborCostMultiplier ?: city?.economicMultiplier ?: 1.0f

        val dynamicCost = (product.productionCost.toDouble() * energyIndexMultiplier * cityCostMultiplier).toLong()
        return dynamicCost.coerceAtLeast(1L)
    }

    fun getDynamicProductionCost(productId: String, cityId: String? = null): Long {
        val product = Product.values().find { it.id == productId } ?: return 0L
        return getDynamicProductionCost(product, cityId)
    }

    fun calculateProductionDuration(productId: String, requestedQuantity: Int = 1, checkFertilizer: Boolean = false): Long {
        val product = Product.values().find { it.id == productId } ?: return 20_000L
        val p = player.value
        val baseDuration = product.baseDurationMs

        // Adet arttıkça üretim süresi orantılı artar
        val quantityFactor = 1.0f + (requestedQuantity - 1).coerceAtLeast(0) * 0.40f
        var computedDurationMs = (baseDuration * quantityFactor).toDouble()

        val producingBusiness = businesses.value.find { it.type == product.facilityId }
        val cityId = producingBusiness?.cityId ?: p?.currentCity ?: "istanbul"
        val cityProfile = com.example.data.cities.find { it.id == cityId }
        val speedMult = cityProfile?.productionSpeedMultiplier ?: 1.0f
        val level = producingBusiness?.level ?: 1

        computedDurationMs /= speedMult
        computedDurationMs *= (1.0f - (level - 1) * 0.07f).coerceAtLeast(0.40f)

        // Lojistik Yazılımı Ar-Ge İndirimi
        val logisticsDiscount = (1.0f - (_techLogistics.value * 0.05f)).coerceAtLeast(0.70f)
        computedDurationMs *= logisticsDiscount

        if (producingBusiness != null) {
            // Yıpranma arttıkça üretim süresi uzar
            computedDurationMs *= (1.0f + producingBusiness.wearLevel * 1.2f)
        }

        val crisis = _consortiumCrisisState.value
        if (crisis.isCrisisActive && (product.tier == ProductTier.TIER_1 || product.tier == ProductTier.TIER_2)) {
            computedDurationMs *= crisis.tier1And2ProductionSlowdownMultiplier
        }

        if (cityProfile != null && product.id in cityProfile.optimalProductIds) {
            computedDurationMs *= 0.75f
        }

        val currentState = gameState.value
        val activeEvent = currentState?.activeEvent ?: "Normal"
        val agriProducts = listOf("wheat", "sugar_beet", "fruit", "olive", "cotton", "rubber_latex", "tea", "sunflower")
        val isAgriProduct = (product.tier == ProductTier.TIER_1 && product.id in agriProducts) ||
                (product.facilityId.endsWith("_farm") || product.facilityId == "orchard" || product.facilityId == "tea_plantation")

        if ((activeEvent.contains("Kuraklık") || activeEvent.contains("Don") || activeEvent == "Don (Tarım)" || activeEvent == "Kuraklık (Tahıl Azalır)") && isAgriProduct) {
            computedDurationMs *= 1.5f
        }

        val prodManager = _managers.value.find { it.id == "mgr_prod" }
        if (prodManager != null && prodManager.isHired && prodManager.isActive) {
            val speedBoost = (prodManager.level * 0.25f) // %25 to %125 boost
            computedDurationMs /= (1.0f + speedBoost)
        }

        if (checkFertilizer && product.id in agriProducts) {
            val hasFertilizer = inventory.value.any { it.itemId == "fertilizer" && it.quantity > 0 }
            if (hasFertilizer) {
                computedDurationMs *= 0.65f
            }
        }

        // 🚨 Milli Üretim Seferberliği / Devlet Teşvik Bonusu: Krizdeki ürünlerde %50 Üretim Hızı Bonusu (Süre yarıya iner)
        val isProductInBorsaCrisis = marketPrices.value.find { it.itemId == productId }?.let {
            it.isCrisis || it.borsaStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
        } ?: false
        if (isProductInBorsaCrisis) {
            computedDurationMs *= 0.50f
        }

        return computedDurationMs.toLong().coerceAtLeast(2000L)
    }

    fun produce(productId: String, requestedQuantity: Int = 1, isSilent: Boolean = false, autoProcure: Boolean = false) {
        if (requestedQuantity <= 0) return
        if (_productionProgress.value.containsKey(productId)) return
        val product = Product.values().find { it.id == productId } ?: return
        val p = player.value ?: return

        val producingBusiness = businesses.value.find { it.type == product.facilityId || it.type == product.id }
        if (producingBusiness != null) {
            if (producingBusiness.isConstructing) {
                if (!isSilent) {
                    val remMs = ((producingBusiness.constructionEndTime ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
                    val m = (remMs / 60000L).coerceAtLeast(1L)
                    SmartNotificationManager.show(
                        "🏗️ Tesis henüz inşaat halinde! (~${m} dk kaldı). ⚡ Elmasla hızlandırabilirsiniz.",
                        "🏗️ Facility is still under construction! (~${m} min left). ⚡ Speed up with gems.",
                        NotificationType.ALERT
                    )
                }
                return
            }
            if (producingBusiness.getRemainingStorageCapacity() < requestedQuantity) {
                if (!isSilent) {
                    val cityName = com.example.data.cities.find { it.id == producingBusiness.cityId }?.name ?: producingBusiness.cityId
                    SmartNotificationManager.show("Hata: Tesis Deposu Dolu! ($cityName tesisi deposunda yeterli alan yok)", "Error: Facility Warehouse Full! (Not enough space in $cityName facility warehouse)", NotificationType.ALERT)
                }
                return
            }
        } else {
            val totalInventoryAmount = inventory.value.sumOf { it.quantity }
            if (totalInventoryAmount + requestedQuantity > p.inventoryCapacity) {
                if (!isSilent) SmartNotificationManager.show("Hata: Merkez Depo Kapasitesi Dolu!", "Error: Central Warehouse Capacity Full!", NotificationType.ALERT)
                return
            }
        }

        val producingCityId = producingBusiness?.cityId ?: p.currentCity
        val producingCity = com.example.data.cities.find { it.id == producingCityId }
        val originCountry = producingCity?.country ?: "Türkiye"

        val dynamicUnitCost = getDynamicProductionCost(product, producingCityId)
        val totalCost = dynamicUnitCost * requestedQuantity
        
        val contractsManager = _managers.value.find { it.id == "mgr_contracts" }
        val treasuryManager = _managers.value.find { it.id == "mgr_treasury" }
        val isContractsActive = contractsManager != null && contractsManager.isHired && contractsManager.isActive
        val isTreasuryActive = treasuryManager != null && treasuryManager.isHired && treasuryManager.isActive

        val currentInventory = inventory.value
        var totalProcurementCost = 0L
        val missingIngredients = mutableListOf<Triple<String, Int, Long>>()
        var procurementSource = "Borsa"

        for (req in product.recipe) {
            val requiredTotal = req.amountPerUnit * requestedQuantity
            val currentStock = currentInventory.find { it.itemId == req.productId }?.quantity ?: 0
            if (currentStock < requiredTotal) {
                val missingQty = requiredTotal - currentStock
                
                val borsaPrice = marketPrices.value.find { it.itemId == req.productId }?.price
                val pazarPrice = Product.values().find { it.id == req.productId }?.basePrice ?: 100L
                
                val unitPrice = if (autoProcure && isContractsActive) {
                    if (borsaPrice != null && borsaPrice < pazarPrice) {
                        procurementSource = "Borsa"
                        borsaPrice
                    } else {
                        procurementSource = "Pazar"
                        pazarPrice
                    }
                } else {
                    borsaPrice ?: pazarPrice
                }
                
                val missingCost = unitPrice * missingQty
                totalProcurementCost += missingCost
                missingIngredients.add(Triple(req.productId, missingQty, unitPrice))
            }
        }

        val grandTotal = totalCost + totalProcurementCost
        
        var workingPlayer = p
        var treasuryFundedAmount = 0L

        if (workingPlayer.money < grandTotal) {
            val deficit = grandTotal - workingPlayer.money
            if (autoProcure && isTreasuryActive && workingPlayer.depositBalance >= deficit) {
                val newDeposit = workingPlayer.depositBalance - deficit
                val newMoney = workingPlayer.money + deficit
                workingPlayer = workingPlayer.copy(money = newMoney, depositBalance = newDeposit)
                treasuryFundedAmount = deficit
            } else {
                if (!isSilent) {
                    val neededFmt = com.example.ui.components.formatCredit(grandTotal)
                    SmartNotificationManager.show(
                        "Hata: Yetersiz Bakiyeniz Var! (Gerekli: $neededFmt)",
                        "Error: Insufficient Balance! (Required: $neededFmt)",
                        NotificationType.ALERT
                    )
                }
                return
            }
        }

        if (missingIngredients.isNotEmpty()) {
            if (!autoProcure) {
                val firstMissing = missingIngredients.first().first
                if (!isSilent) SmartNotificationManager.show("Hata: Yetersiz hammadde ($firstMissing)", "Error: Insufficient raw materials ($firstMissing)", NotificationType.ALERT)
                return
            }
        }

        viewModelScope.launch {
            if (treasuryFundedAmount > 0) {
                repository.updatePlayer(workingPlayer)
                logManagerAction(
                    "mgr_treasury",
                    "Hazine Müdürü: Üretim maliyeti ve eksik hammaddeler için bankadan ₳${com.example.ui.components.formatMoney(treasuryFundedAmount)} çekildi.",
                    treasuryFundedAmount
                )
            }

            var playerMoneyAfterProcurement = workingPlayer.money
            if (missingIngredients.isNotEmpty()) {
                playerMoneyAfterProcurement = (playerMoneyAfterProcurement - totalProcurementCost).coerceAtLeast(0L)
                for ((itemId, qty, _) in missingIngredients) {
                    repository.produceItem(itemId, qty)
                    if (procurementSource == "Borsa") {
                        onBorsaItemBought(itemId, qty, "Global")
                    }
                }
                val mgrName = if (isContractsActive) "Tedarik Müdürü ($procurementSource)" else "Sistem ($procurementSource)"
                logManagerAction(
                    if (isContractsActive) "mgr_contracts" else "mgr_prod",
                    "$mgrName: ${product.getDisplayName()} için eksik hammaddeler (${missingIngredients.joinToString { "${it.first} x${it.second}" }}) en uygun fiyattan temin edildi.",
                    -totalProcurementCost
                )
            }

            // Deduct totalCost BEFORE starting production coroutine work/delays and record dailyExpense
            val newMoney = (playerMoneyAfterProcurement - totalCost).coerceAtLeast(0L)
            val updatedPlayer = p.copy(
                money = newMoney,
                dollarBalance = newMoney,
                dailyExpense = p.dailyExpense + grandTotal
            )
            repository.updatePlayer(updatedPlayer)

            // Consume recipe ingredients
            for (req in product.recipe) {
                repository.consumeItem(req.productId, req.amountPerUnit * requestedQuantity)
            }
            
            val currentState = gameState.value
            val activeEvent = currentState?.activeEvent ?: "Normal"
            val agriProducts = listOf("wheat", "sugar_beet", "fruit", "olive", "cotton", "rubber_latex", "tea", "sunflower")
            val isAgriProduct = (product.tier == ProductTier.TIER_1 && product.id in agriProducts) ||
                    (product.facilityId.endsWith("_farm") || product.facilityId == "orchard" || product.facilityId == "tea_plantation")

            var durationMs = calculateProductionDuration(productId, requestedQuantity)

            // Fertilizer Crop Boost Mechanic
            if (isAgriProduct) {
                val currentInv = inventory.value
                val fertilizerItem = currentInv.find { it.itemId == "fertilizer" }
                if (fertilizerItem != null && fertilizerItem.quantity > 0) {
                    repository.consumeItem("fertilizer", 1)
                    durationMs = (durationMs * 0.65f).toLong().coerceAtLeast(2000L)
                    SmartNotificationManager.show(
                        "Organik Gübre kullanıldı! Üretim %35 hızlandı.", "Organic Fertilizer used! Production sped up by 35%.",
                        NotificationType.SUCCESS
                    )
                }
            }

            val secureNow = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
            val prodEntityId = java.util.UUID.randomUUID().toString()
            val prodEntity = com.example.data.ActiveProduction(
                id = prodEntityId,
                productId = productId,
                quantity = requestedQuantity,
                facilityId = product.facilityId,
                businessId = producingBusiness?.id ?: 0,
                cityId = producingCityId,
                targetCityId = p.currentCity,
                startTimeMs = secureNow,
                totalDurationMs = durationMs,
                endTimeMs = secureNow + durationMs,
                isAgriProduct = isAgriProduct,
                originCountry = originCountry
            )
            _activeProductions.update { it + prodEntity }

            _productionDurations.update { it + (productId to durationMs) }
            _productionProgress.update { it + (productId to 0f) }
            saveEconomicDataToDataStore(immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }

            val intervalMs = 250L
            val steps = (durationMs / intervalMs).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                if (_productionSkip.value.contains(productId)) {
                    _productionSkip.update { it - productId }
                    break
                }
                // Check if already completed by offline engine or background ticker
                if (_activeProductions.value.none { it.id == prodEntityId }) {
                    return@launch
                }
                delay(intervalMs)
                
                // Re-check after delay to prevent race condition when skipped during delay
                if (_productionSkip.value.contains(productId) || _activeProductions.value.none { it.id == prodEntityId }) {
                    continue
                }
                
                val currentNow = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
                val elapsed = (currentNow - secureNow).coerceAtLeast(0L)
                val currentP = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                _productionProgress.update { it + (productId to currentP) }
            }

            val remainingProd = _activeProductions.value.find { it.id == prodEntityId }
            if (remainingProd != null) {
                completeActiveProduction(remainingProd, isOffline = false, isSilent = isSilent)
            }
        }
    }

    /**
     * ⚡ Tek Tuşla Tümünü Hasat Et & Üretimi Yenile (One-Tap Mass Harvest & Produce All)
     * Collects all completed productions across holding facilities, then starts
     * production shifts for all idle, ready facilities with automatic procurement if needed.
     */
    fun massHarvestAndProduceAll() {
        viewModelScope.launch {
            var harvestedCount = 0
            var startedCount = 0
            val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()

            // 1. Collect all completed productions
            val currentProductions = _activeProductions.value.toList()
            val currentBizList = businesses.value.ifEmpty {
                try { repository.getAllBusinessesDirect() } catch (_: Exception) { emptyList() }
            }
            for (prod in currentProductions) {
                if (prod.isCompleted(now)) {
                    completeActiveProduction(prod, isOffline = false, isSilent = true, explicitBizList = currentBizList)
                    harvestedCount++
                }
            }

            // 2. Start production for all idle, ready facilities
            val activeItemIds = _activeProductions.value.map { it.productId }.toSet()
            val inProgressKeys = _productionProgress.value.keys
            val freshBizList = businesses.value.ifEmpty {
                try { repository.getAllBusinessesDirect() } catch (_: Exception) { emptyList() }
            }

            for (biz in freshBizList) {
                if (biz.isConstructing || biz.isUpgrading) continue
                val prod = Product.values().find { it.facilityId == biz.type || it.id == biz.type } ?: continue
                if (activeItemIds.contains(prod.id) || inProgressKeys.contains(prod.id)) continue
                val remainingStorage = biz.getRemainingStorageCapacity()
                if (remainingStorage <= 0) continue

                // Recommended smart batch size based on facility level
                val batchQty = (biz.level * 5).coerceIn(1, remainingStorage.coerceAtLeast(1))
                val isUsd = false
                val activeBal = if (isUsd) player.value?.dollarBalance ?: 0L else player.value?.money ?: 0L
                val cost = prod.productionCost * batchQty
                if (activeBal >= cost) {
                    produce(productId = prod.id, requestedQuantity = batchQty, isSilent = true, autoProcure = true)
                    startedCount++
                }
            }

            if (harvestedCount > 0 || startedCount > 0) {
                SmartNotificationManager.show(
                    "⚡ Toplu Operasyon: $harvestedCount tamamlanan parti depolandı, $startedCount tesiste yeni üretim başlatıldı!",
                    "⚡ Mass Operation: $harvestedCount finished batches collected, $startedCount facilities started production!",
                    NotificationType.SUCCESS
                )
                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
            } else {
                SmartNotificationManager.show(
                    "ℹ️ Tüm tesisler zaten faal durumda veya işlemde.",
                    "ℹ️ All facilities are already actively running or need attention.",
                    NotificationType.INFO
                )
            }
        }
    }

    /**
     * 📦 Akıllı Tedarik (Smart Ingredient Procurement Shortcut)
     * Instantly buys all missing ingredients for a product directly from the market/borsa.
     */
    fun buyMissingIngredients(productId: String, requestedQuantity: Int = 1) {
        val product = Product.values().find { it.id == productId } ?: return
        val currentInventory = inventory.value
        val playerEntity = player.value ?: return
        var totalCost = 0L
        val missingList = mutableListOf<Pair<String, Int>>()

        for (req in product.recipe) {
            val requiredTotal = req.amountPerUnit * requestedQuantity.coerceAtLeast(1)
            val currentStock = currentInventory.find { it.itemId == req.productId }?.quantity ?: 0
            if (currentStock < requiredTotal) {
                val missingQty = requiredTotal - currentStock
                val borsaPrice = marketPrices.value.find { it.itemId == req.productId }?.price
                val pazarPrice = Product.values().find { it.id == req.productId }?.basePrice ?: 100L
                val unitPrice = borsaPrice ?: pazarPrice
                totalCost += (unitPrice * missingQty)
                missingList.add(req.productId to missingQty)
            }
        }

        if (missingList.isEmpty()) {
            SmartNotificationManager.show(
                "✅ Gerekli tüm hammaddeler zaten depoda mevcut!",
                "✅ All required raw materials are already in inventory!",
                NotificationType.INFO
            )
            return
        }

        if (playerEntity.money < totalCost) {
            val neededFmt = com.example.ui.components.formatMoney(totalCost)
            SmartNotificationManager.show(
                "Hata: Yetersiz Bakiye! (Gerekli: $neededFmt)",
                "Error: Insufficient Balance! (Required: $neededFmt)",
                NotificationType.ALERT
            )
            return
        }

        viewModelScope.launch {
            val newMoney = playerEntity.money - totalCost
            repository.updatePlayer(playerEntity.copy(money = newMoney))
            for ((itemId, qty) in missingList) {
                repository.produceItem(itemId, qty)
                onBorsaItemBought(itemId, qty, "Global")
            }
            val count = missingList.size
            SmartNotificationManager.show(
                "📦 Akıllı Tedarik: $count eksik hammadde türü piyasadan temin edildi! (-${com.example.ui.components.formatMoney(totalCost)})",
                "📦 Smart Procurement: $count missing ingredients purchased from market! (-${com.example.ui.components.formatMoney(totalCost)})",
                NotificationType.SUCCESS
            )
            com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.BUY_SELL)
        }
    }

    fun transferFacilityStockToCentral(businessId: Int, itemId: String, quantity: Int) {
        if (quantity <= 0) return
        val biz = businesses.value.find { it.id == businessId } ?: return
        val available = biz.getStoredItemsMap()[itemId] ?: 0
        if (available <= 0) return
        val toMove = minOf(quantity, available)
        val p = player.value ?: return
        val currentCentralInv = inventory.value.sumOf { it.quantity }
        val remainingCentralCap = (p.inventoryCapacity - currentCentralInv).coerceAtLeast(0)
        
        if (remainingCentralCap <= 0) {
            SmartNotificationManager.show("Hata: Merkez Depo Kapasitesi Dolu!", "Error: Central Warehouse Capacity Full!", NotificationType.ALERT)
            return
        }
        
        val actualMoved = minOf(toMove, remainingCentralCap)
        viewModelScope.launch {
            val updatedBiz = biz.withRemovedItem(itemId, actualMoved)
            repository.updateBusiness(updatedBiz)
            
            val originCityId = biz.cityId
            val destCityId = if (p.currentCity.isNotBlank()) p.currentCity else "istanbul"
            val durationMs = calculateLogisticsDuration(originCityId, destCityId)
            
            val delivery = com.example.data.DeliveryItem(
                itemId = itemId,
                quantity = actualMoved,
                originCityId = originCityId,
                destinationCityId = destCityId,
                pricePerUnit = 0L,
                totalCost = 0L,
                startTimeMs = System.currentTimeMillis(),
                totalDurationMs = durationMs,
                isOutboundSale = false
            )
            addActiveDelivery(delivery)
            saveEconomicDataToDataStore()
            
            val prodName = Product.values().find { it.id == itemId || it.facilityId == itemId }?.getDisplayName() ?: itemId.uppercase()
            val originName = com.example.data.cities.find { it.id == originCityId }?.name ?: originCityId.uppercase()
            val destName = com.example.data.cities.find { it.id == destCityId }?.name ?: destCityId.uppercase()
            SmartNotificationManager.show("🚚 $actualMoved Ton $prodName ($originName ➔ $destName) haritada sevkiyata çıkarıldı!", "🚚 $actualMoved Tons of $prodName ($originName ➔ $destName) dispatched on map!", NotificationType.SUCCESS)
        }
    }

    fun transferAllFacilityStockToCentral(businessId: Int? = null, isSilent: Boolean = false) {
        viewModelScope.launch {
            val p = player.value ?: return@launch
            var currentCentralInv = inventory.value.sumOf { it.quantity }
            var remainingCap = (p.inventoryCapacity - currentCentralInv).coerceAtLeast(0)
            
            if (remainingCap <= 0) {
                if (!isSilent) SmartNotificationManager.show("Hata: Merkez Depo Kapasitesi Dolu!", "Error: Central Warehouse Capacity Full!", NotificationType.ALERT)
                return@launch
            }
            
            val targetBusinesses = if (businessId != null) {
                businesses.value.filter { it.id == businessId }
            } else {
                businesses.value
            }
            
            var totalMoved = 0
            val destCityId = if (p.currentCity.isNotBlank()) p.currentCity else "istanbul"
            
            for (biz in targetBusinesses) {
                val items = biz.getStoredItemsMap()
                var currentBiz = biz
                for ((itemId, qty) in items) {
                    if (remainingCap <= 0) break
                    val moveQty = minOf(qty, remainingCap)
                    if (moveQty > 0) {
                        currentBiz = currentBiz.withRemovedItem(itemId, moveQty)
                        remainingCap -= moveQty
                        totalMoved += moveQty
                        
                        val durationMs = calculateLogisticsDuration(biz.cityId, destCityId)
                        val delivery = com.example.data.DeliveryItem(
                            itemId = itemId,
                            quantity = moveQty,
                            originCityId = biz.cityId,
                            destinationCityId = destCityId,
                            pricePerUnit = 0L,
                            totalCost = 0L,
                            startTimeMs = System.currentTimeMillis(),
                            totalDurationMs = durationMs,
                            isOutboundSale = false
                        )
                        addActiveDelivery(delivery)
                    }
                }
                if (currentBiz != biz) {
                    repository.updateBusiness(currentBiz)
                }
            }
            
            if (totalMoved > 0) {
                saveEconomicDataToDataStore()
                if (!isSilent) SmartNotificationManager.show("🚚 Toplam $totalMoved Ton ürün haritada Merkez Depoya aktarılmak üzere yola çıktı.", "🚚 Total $totalMoved Tons of products dispatched to Central Warehouse on map.", NotificationType.SUCCESS)
            } else {
                if (!isSilent) SmartNotificationManager.show("Aktarılacak tesis stoğu bulunamadı veya merkez depo dolu.", "No facility stock found to transfer or central warehouse is full.", NotificationType.ALERT)
            }
        }
    }


    internal var lastBuildTime = 0L

    fun buildBusiness(type: String, cityId: String, cost: Long) {
        val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
        if (now - lastBuildTime < 1000L) return
        lastBuildTime = now

        viewModelScope.launch {
            val p = player.value ?: return@launch

            // Validate prerequisites and city eligibility
            val product = Product.values().find { it.facilityId == type || it.id == type }
            if (product != null) {
                val prereq = getFacilityPrerequisites(product)
                if (!prereq.isMet) {
                    SmartNotificationManager.show(
                        prereq.reasonTr,
                        prereq.reasonEn,
                        NotificationType.ALERT
                    )
                    return@launch
                }
                if (!product.canBeBuiltIn(cityId)) {
                    val allowedNames = product.getAllowedCityIds().mapNotNull { id -> cities.find { it.id == id }?.name }.joinToString(", ")
                    SmartNotificationManager.show(
                        "❌ ${product.getFacilityName(false)} tesisi bu şehre kurulamaz! Yalnızca şu şehirlerde kurulabilir: $allowedNames", "❌ ${product.getFacilityName(false)} facility cannot be built in this city! It can only be built in these cities: $allowedNames",
                        NotificationType.ALERT
                    )
                    return@launch
                }
            }

            val canAfford = p.money >= cost

            if (canAfford) {
                val durationMs = product?.getInitialConstructionDurationMs() ?: (10 * 60 * 1000L)
                val constructionFinishTime = now + durationMs
                val newBiz = com.example.data.BusinessEntity(
                    type = type,
                    level = 1,
                    cityId = cityId,
                    isConstructing = true,
                    constructionEndTime = constructionFinishTime
                )
                repository.updateBusiness(newBiz)
                
                // Instantly sanitize market prices based on new facility location and sync to Supabase
                val updatedBizList = businesses.value + newBiz
                val reSanitizedPrices = sanitizeMarketPrices(marketPrices.value, updatedBizList)
                repository.updateMarketPrices(reSanitizedPrices, syncToRemote = true)

                val facNameTr = product?.getFacilityName(false) ?: type.uppercase()
                val facNameEn = product?.getFacilityName(true) ?: type.uppercase()
                val durationLabelTr = product?.getInitialConstructionDurationLabel(false) ?: "10 Dk"
                val durationLabelEn = product?.getInitialConstructionDurationLabel(true) ?: "10 Min"
                SmartNotificationManager.show(
                    "🏗️ $facNameTr tesisinin temeli atıldı! Kurulum: $durationLabelTr. ⚡ Elmasla hemen tamamlayabilirsiniz.",
                    "🏗️ Groundbreaking for $facNameEn! Construction: $durationLabelEn. ⚡ Complete instantly with gems.",
                    NotificationType.INFO
                )

                if (_tradeTutorialStep.value in 1..2) {
                    _tradeTutorialStep.value = 3
                }
                val currentP = player.value ?: p
                val updatedMoney = currentP.money - cost

                val updatedPlayer = currentP.copy(
                    money = updatedMoney,
                    dailyExpense = currentP.dailyExpense + cost
                )
                repository.updatePlayer(updatedPlayer)
                
                processXpGain(150, updatedPlayer)
                
                // Supabase veritabanıyla gerekli senkronizasyonu sağla
                saveEconomicDataToDataStore(customPlayer = updatedPlayer, customBusinesses = updatedBizList)
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer, customBusinesses = updatedBizList)
            }
        }
    }

    internal var lastUpgradeTime = 0L

    fun upgradeBusiness(business: com.example.data.BusinessEntity) {
        val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
        if (now - lastUpgradeTime < 1000L) return
        lastUpgradeTime = now

        viewModelScope.launch {
            if (business.isUpgrading) {
                SmartNotificationManager.show(
                    "⏳ Bu tesiste zaten inşaat ve seviye yükseltme devam ediyor!",
                    "⏳ Facility upgrade is already in progress!",
                    NotificationType.ALERT
                )
                return@launch
            }

            if (business.level >= 10) {
                SmartNotificationManager.show("Maksimum seviyeye (10) ulaşıldı!", "Maximum level (10) reached!", NotificationType.ALERT)
                return@launch
            }

            val p = player.value ?: return@launch

            // Check progression prerequisites for high-tier upgrades
            if (business.level == 3 && p.level < 3 && getTechLevel("tech_quality_control") < 1) {
                SmartNotificationManager.show(
                    "🔒 Seviye 4 yükseltmesi için Şirket Prestiji Seviye 3 veya Kalite Kontrol Ar-Ge araştırması gerekli!",
                    "🔒 Level 4 upgrade requires Company Prestige Level 3 or Quality Control R&D research!",
                    NotificationType.ALERT
                )
                return@launch
            }
            if (business.level == 4 && p.level < 5 && getTechLevel("tech_automation") < 1) {
                SmartNotificationManager.show(
                    "🔒 Seviye 5 yükseltmesi için Şirket Prestiji Seviye 5 veya Otomasyon Ar-Ge araştırması gerekli!",
                    "🔒 Level 5 upgrade requires Company Prestige Level 5 or Automation R&D research!",
                    NotificationType.ALERT
                )
                return@launch
            }
            
            val product = Product.values().find { it.facilityId == business.type }
            val city = com.example.data.cities.find { it.id == business.cityId }
            val cityMultiplier = city?.economicMultiplier ?: 1.0f
            val baseCost = ((product?.facilityCost ?: 1000000L) * cityMultiplier).toLong()
            val upgradeCost = (baseCost * 0.5 * 1.2.pow(business.level.toDouble())).toLong()

            val canAfford = p.money >= upgradeCost

            if (canAfford) {
                val durationMs = business.getUpgradeDurationMs()
                val endTime = now + durationMs
                val upgradingBiz = business.copy(
                    isUpgrading = true,
                    upgradeEndTime = endTime
                )
                repository.updateBusiness(upgradingBiz)
                
                val updatedMoney = p.money - upgradeCost
                val updatedPlayer = p.copy(
                    money = updatedMoney,
                    dollarBalance = updatedMoney,
                    dailyExpense = p.dailyExpense + upgradeCost
                )
                repository.updatePlayer(updatedPlayer)
                
                val currentBizList = try {
                    val direct = repository.getAllBusinessesDirect()
                    if (direct.isNotEmpty()) direct else businesses.value
                } catch (_: Exception) {
                    businesses.value
                }
                val updatedBizList = currentBizList.map { if (it.id == upgradingBiz.id) upgradingBiz else it }

                val facName = product?.getFacilityName() ?: business.type.uppercase()
                val durationText = when (business.level) {
                    1 -> "30 Dakika"
                    2 -> "2 Saat"
                    3 -> "6 Saat"
                    4 -> "18 Saat"
                    else -> "24 Saat"
                }
                SmartNotificationManager.show(
                    "🏗️ $facName Seviye ${business.level + 1} yükseltme inşaatı başladı! (Süre: $durationText)",
                    "🏗️ $facName upgrade construction started! (Duration: $durationText)",
                    NotificationType.SUCCESS
                )
                
                // Supabase veritabanıyla gerekli senkronizasyonu sağla
                saveEconomicDataToDataStore(customPlayer = updatedPlayer, customBusinesses = updatedBizList)
                syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = updatedPlayer, customBusinesses = updatedBizList)
            } else {
                val formattedCost = com.example.ui.components.formatCredit(upgradeCost)
                SmartNotificationManager.show("Yetersiz bakiye! (Gerekli: $formattedCost)", "Insufficient balance! (Required: $formattedCost)", NotificationType.ALERT)
            }
        }
    }

    fun completeFacilityUpgrade(business: com.example.data.BusinessEntity, isOffline: Boolean = false, currentPlayerState: PlayerEntity? = null) {
        viewModelScope.launch {
            val currentBiz = (businesses.value.find { it.id == business.id }
                ?: try { repository.getAllBusinessesDirect().find { it.id == business.id } } catch (_: Exception) { null })
                ?: business
            if (!currentBiz.isUpgrading) return@launch

            val newLevel = (currentBiz.level + 1).coerceAtMost(10)
            val upgradedBiz = currentBiz.copy(
                level = newLevel,
                isUpgrading = false,
                upgradeEndTime = null
            )
            repository.updateBusiness(upgradedBiz)

            val p = currentPlayerState ?: player.value ?: return@launch
            val xpGain = 100 * newLevel
            processXpGain(xpGain, p)

            val product = Product.values().find { it.facilityId == business.type }
            val facName = product?.getFacilityName() ?: business.type.uppercase()
            if (isOffline) {
                SmartNotificationManager.show(
                    "🎉 Çevrimdışı Yükseltme: $facName Seviye $newLevel tamamlandı! (+$xpGain XP)",
                    "🎉 Offline Upgrade: $facName Level $newLevel completed! (+$xpGain XP)",
                    NotificationType.SUCCESS
                )
            } else {
                SmartNotificationManager.show(
                    "🎉 $facName Seviye $newLevel yükseltmesi tamamlandı! (+$xpGain XP)",
                    "🎉 $facName upgraded to Level $newLevel! (+$xpGain XP)",
                    NotificationType.SUCCESS
                )
            }

            val currentBizList = try {
                val direct = repository.getAllBusinessesDirect()
                if (direct.isNotEmpty()) direct else businesses.value
            } catch (_: Exception) {
                businesses.value
            }
            val updatedBizList = currentBizList.map { if (it.id == upgradedBiz.id) upgradedBiz else it }

            saveEconomicDataToDataStore(customPlayer = p, customBusinesses = updatedBizList)
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = p, customBusinesses = updatedBizList)
        }
    }

    fun speedUpBusinessUpgradeWithGems(businessId: Int) {
        val p = player.value ?: return
        val biz = businesses.value.find { it.id == businessId } ?: return
        if (!biz.isUpgrading || biz.upgradeEndTime == null) return

        val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
        val remainingMs = (biz.upgradeEndTime - now).coerceAtLeast(0L)
        val gemCost = biz.getUpgradeDiamondCost(remainingMs)

        if (p.gems < gemCost) {
            SmartNotificationManager.show(
                "Yetersiz Elmas! ($gemCost 💎 gerekli)",
                "Insufficient Gems! ($gemCost 💎 required)",
                NotificationType.ALERT
            )
            return
        }

        viewModelScope.launch {
            val updatedPlayer = p.copy(gems = p.gems - gemCost)
            repository.updatePlayer(updatedPlayer)
            completeFacilityUpgrade(biz, currentPlayerState = updatedPlayer)
            SmartNotificationManager.show(
                "⚡ Tesis yükseltmesi elmasla anında tamamlandı! (-$gemCost 💎)",
                "⚡ Facility upgrade instantly completed! (-$gemCost 💎)",
                NotificationType.SUCCESS
            )
            saveEconomicDataToDataStore()
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }

    fun checkAndProcessFacilityUpgrades(isOffline: Boolean = false, explicitBizList: List<com.example.data.BusinessEntity>? = null) {
        viewModelScope.launch {
            val currentBizList = explicitBizList ?: businesses.value.ifEmpty {
                try { repository.getAllBusinessesDirect() } catch (_: Exception) { emptyList() }
            }
            if (currentBizList.isEmpty()) return@launch
            val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
            val finished = currentBizList.filter { it.isUpgrading && it.upgradeEndTime != null && now >= it.upgradeEndTime }
            finished.forEach { biz ->
                completeFacilityUpgrade(biz, isOffline = isOffline)
            }
        }
    }

    fun completeFacilityConstruction(business: com.example.data.BusinessEntity, isOffline: Boolean = false, currentPlayerState: PlayerEntity? = null) {
        viewModelScope.launch {
            val currentBiz = (businesses.value.find { it.id == business.id }
                ?: try { repository.getAllBusinessesDirect().find { it.id == business.id } } catch (_: Exception) { null })
                ?: business
            if (!currentBiz.isConstructing) return@launch

            val completedBiz = currentBiz.copy(
                isConstructing = false,
                constructionEndTime = null
            )
            repository.updateBusiness(completedBiz)

            val p = currentPlayerState ?: player.value ?: return@launch
            val xpGain = 150
            processXpGain(xpGain, p)

            val product = Product.values().find { it.facilityId == business.type || it.id == business.type }
            val facNameTr = product?.getFacilityName(false) ?: business.type.uppercase()
            val facNameEn = product?.getFacilityName(true) ?: business.type.uppercase()
            if (isOffline) {
                SmartNotificationManager.show(
                    "🎉 Çevrimdışı Kurulum: $facNameTr tesisinin kurulumu tamamlandı ve üretime hazır! (+$xpGain XP)",
                    "🎉 Offline Construction: $facNameEn completed and ready for production! (+$xpGain XP)",
                    NotificationType.SUCCESS
                )
            } else {
                SmartNotificationManager.show(
                    "🎉 $facNameTr tesisinin inşası tamamlandı ve üretime hazır!",
                    "🎉 $facNameEn construction completed and ready for production!",
                    NotificationType.SUCCESS
                )
            }

            val currentBizList = try {
                val direct = repository.getAllBusinessesDirect()
                if (direct.isNotEmpty()) direct else businesses.value
            } catch (_: Exception) {
                businesses.value
            }
            val updatedBizList = currentBizList.map { if (it.id == completedBiz.id) completedBiz else it }

            saveEconomicDataToDataStore(customPlayer = p, customBusinesses = updatedBizList)
            syncCloudSaveToSupabase(force = true, immediate = true, customPlayer = p, customBusinesses = updatedBizList)
        }
    }

    fun speedUpBusinessConstructionWithGems(businessId: Int) {
        val p = player.value ?: return
        val biz = businesses.value.find { it.id == businessId } ?: return
        if (!biz.isConstructing || biz.constructionEndTime == null) return

        val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
        val remainingMs = (biz.constructionEndTime - now).coerceAtLeast(0L)
        val gemCost = biz.getConstructionDiamondCost(remainingMs)

        if (p.gems < gemCost) {
            SmartNotificationManager.show(
                "Yetersiz Elmas! ($gemCost 💎 gerekli)",
                "Insufficient Gems! ($gemCost 💎 required)",
                NotificationType.ALERT
            )
            return
        }

        viewModelScope.launch {
            val updatedPlayer = p.copy(gems = p.gems - gemCost)
            repository.updatePlayer(updatedPlayer)
            completeFacilityConstruction(biz, currentPlayerState = updatedPlayer)
            SmartNotificationManager.show(
                "⚡ Tesis inşası elmasla anında tamamlandı! (-$gemCost 💎)",
                "⚡ Facility construction instantly completed! (-$gemCost 💎)",
                NotificationType.SUCCESS
            )
            saveEconomicDataToDataStore()
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }

    fun checkAndProcessFacilityConstructions(isOffline: Boolean = false, explicitBizList: List<com.example.data.BusinessEntity>? = null) {
        viewModelScope.launch {
            val currentBizList = explicitBizList ?: businesses.value.ifEmpty {
                try { repository.getAllBusinessesDirect() } catch (_: Exception) { emptyList() }
            }
            if (currentBizList.isEmpty()) return@launch
            val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
            val finished = currentBizList.filter { it.isConstructing && it.constructionEndTime != null && now >= it.constructionEndTime }
            finished.forEach { biz ->
                completeFacilityConstruction(biz, isOffline = isOffline)
            }
        }
    }

    fun completeActiveProduction(item: com.example.data.ActiveProduction, isOffline: Boolean = false, isSilent: Boolean = false, explicitBizList: List<com.example.data.BusinessEntity>? = null, currentPlayerState: PlayerEntity? = null) {
        viewModelScope.launch {
            if (_activeProductions.value.none { it.id == item.id }) return@launch
            _activeProductions.update { list -> list.filter { it.id != item.id } }
            _productionDurations.update { it - item.productId }
            _productionProgress.update { it - item.productId }

            val productId = item.productId
            val requestedQuantity = item.quantity
            val product = Product.values().find { it.id == productId }
            val originCountry = item.originCountry
            val currentBizList = explicitBizList ?: businesses.value.ifEmpty {
                try { repository.getAllBusinessesDirect() } catch (_: Exception) { emptyList() }
            }
            val producingBusiness = currentBizList.find { it.id == item.businessId }
                ?: currentBizList.find { product != null && (it.type == product.facilityId || it.type == product.id) }

            val currentState = gameState.value
            val activeEvent = currentState?.activeEvent ?: "Normal"
            val isAgriProduct = item.isAgriProduct

            var finalHarvestQty = requestedQuantity
            if ((activeEvent.contains("Kuraklık") || activeEvent.contains("Don") || activeEvent == "Don (Tarım)" || activeEvent == "Kuraklık (Tahıl Azalır)") && isAgriProduct) {
                if (Random.nextFloat() < 0.20f) {
                    finalHarvestQty = (requestedQuantity * 0.80f).toInt()
                    val lostQty = requestedQuantity - finalHarvestQty
                    if (lostQty > 0 && !isOffline && !isSilent) {
                        SmartNotificationManager.show(
                            "Şiddetli hava koşulları ($activeEvent) nedeniyle $lostQty Ton mahsul kaybı yaşandı!",
                            "Severe weather conditions ($activeEvent) caused a loss of $lostQty Tons of crop!",
                            NotificationType.ALERT
                        )
                    }
                }
            }

            val prodDisplayName = product?.getDisplayName() ?: productId

            if (producingBusiness != null) {
                val latestBiz = currentBizList.find { it.id == producingBusiness.id } ?: producingBusiness
                var wearIncrease = 0.0005f * requestedQuantity
                val crisis = _consortiumCrisisState.value
                if (crisis.isCrisisActive) {
                    wearIncrease *= crisis.facilityWearRateMultiplier
                }
                val newWear = (latestBiz.wearLevel + wearIncrease).coerceAtMost(1.0f)

                var updatedBiz = latestBiz.copy(wearLevel = newWear)
                if (finalHarvestQty > 0) {
                    updatedBiz = updatedBiz.withAddedItem(productId, finalHarvestQty)
                }
                repository.updateBusiness(updatedBiz)

                if (finalHarvestQty > 0) {
                    val cityName = com.example.data.cities.find { it.id == latestBiz.cityId }?.name ?: latestBiz.cityId
                    if (isOffline) {
                        SmartNotificationManager.show(
                            "🎉 Çevrimdışı Üretim Tamamlandı: $finalHarvestQty Ton $prodDisplayName $cityName tesisi deposuna yerleştirildi! (+$finalHarvestQty XP) 🏭",
                            "🎉 Offline Production Completed: $finalHarvestQty Tons of $prodDisplayName placed in $cityName facility warehouse! (+$finalHarvestQty XP) 🏭",
                            NotificationType.SUCCESS
                        )
                    } else if (!isSilent) {
                        SmartNotificationManager.show(
                            "Üretim Tamamlandı: $finalHarvestQty Ton $prodDisplayName $cityName tesisi deposuna yerleştirildi! 🏭",
                            "Production Completed: $finalHarvestQty Tons of $prodDisplayName placed in $cityName facility warehouse! 🏭",
                            NotificationType.SUCCESS
                        )
                    }
                    onProductProduced(productId, finalHarvestQty, originCountry)
                    processXpGain(finalHarvestQty * 5, currentPlayerState)
                    com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.WAREHOUSE_CHANGE)
                }

                if (!isSilent && !isOffline && newWear >= 0.70f) {
                    SmartNotificationManager.show(
                        "Dikkat: $prodDisplayName üretim tesisinde yıpranma oranı %${(newWear * 100).toInt()}'ye ulaştı. Bakım önerilir!",
                        "Attention: Wear rate reached %${(newWear * 100).toInt()} in $prodDisplayName production facility. Maintenance recommended!",
                        NotificationType.ALERT
                    )
                }
            } else {
                if (finalHarvestQty > 0) {
                    repository.produceItem(productId, finalHarvestQty)
                    if (isOffline) {
                        SmartNotificationManager.show(
                            "🎉 Çevrimdışı Üretim Tamamlandı: $finalHarvestQty Ton $prodDisplayName merkez depoya yerleştirildi! (+$finalHarvestQty XP) 🏭",
                            "🎉 Offline Production Completed: $finalHarvestQty Tons of $prodDisplayName placed in central warehouse! (+$finalHarvestQty XP) 🏭",
                            NotificationType.SUCCESS
                        )
                    } else if (!isSilent) {
                        SmartNotificationManager.show(
                            "Üretim Tamamlandı: $finalHarvestQty Ton $prodDisplayName merkez depoya yerleştirildi! 🏭",
                            "Production Completed: $finalHarvestQty Tons of $prodDisplayName placed in central warehouse! 🏭",
                            NotificationType.SUCCESS
                        )
                    }
                    onProductProduced(productId, finalHarvestQty, originCountry)
                    processXpGain(finalHarvestQty * 5, currentPlayerState)
                    com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.WAREHOUSE_CHANGE)
                }
            }

            saveEconomicDataToDataStore(immediate = true)
            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase(force = true, immediate = true)
            }
        }
    }

    fun checkAndProcessActiveProductions(isOffline: Boolean = false, explicitBizList: List<com.example.data.BusinessEntity>? = null) {
        val currentProductions = _activeProductions.value
        if (currentProductions.isEmpty()) return
        val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()

        val finished = currentProductions.filter { prod -> prod.isCompleted(now) }
        val ongoing = currentProductions.filter { prod -> !prod.isCompleted(now) }

        if (ongoing.isNotEmpty()) {
            val progressMap = _productionProgress.value.toMutableMap()
            val durationsMap = _productionDurations.value.toMutableMap()
            ongoing.forEach { item ->
                durationsMap[item.productId] = item.totalDurationMs
                val fraction = item.getProgress(now)
                progressMap[item.productId] = fraction
            }
            _productionProgress.value = progressMap
            _productionDurations.value = durationsMap
        }

        if (finished.isNotEmpty()) {
            finished.forEach { prod ->
                completeActiveProduction(prod, isOffline = isOffline, explicitBizList = explicitBizList)
            }
        }
    }

    internal var lastSellTime = 0L
    fun sellBusiness(business: com.example.data.BusinessEntity) {
        val now = System.currentTimeMillis()
        if (now - lastSellTime < 1000L) return
        lastSellTime = now
        viewModelScope.launch {
            val activeProducts = com.example.data.Product.values().filter { it.facilityId == business.type }.map { it.id }
            val isProducing = activeProducts.any { _productionProgress.value.containsKey(it) }
            if (isProducing) {
                com.example.ui.components.SmartNotificationManager.show("Bu tesiste üretim devam ediyor! Lütfen işlemin bitmesini bekleyin.", com.example.ui.components.NotificationType.ALERT)
                return@launch
            }

            val currentBusinesses = businesses.value
            if (currentBusinesses.none { it.id == business.id }) return@launch
            val p = player.value ?: return@launch
            val product = Product.values().find { it.facilityId == business.type }
            val city = com.example.data.cities.find { it.id == business.cityId }
            val cityMultiplier = city?.economicMultiplier ?: 1.0f
            val baseCost = ((product?.facilityCost ?: 1000000L) * cityMultiplier).toLong()
            
            var totalUpgradeInvestment = 0L
            for (lvl in 1 until business.level) {
                totalUpgradeInvestment += (baseCost * 0.5 * 1.2.pow(lvl.toDouble())).toLong()
            }
            val totalInvestment = baseCost + totalUpgradeInvestment
            val refundAmount = (totalInvestment * 0.70f).toLong()

            repository.deleteBusiness(business.id)
            val currentP = player.value ?: p
            val newMoney = currentP.money + refundAmount
            repository.updatePlayer(currentP.copy(
                money = newMoney,
                dollarBalance = newMoney
            ))

            val facilityName = product?.getFacilityName() ?: "Tesis"
            val cityName = city?.name ?: business.cityId
            val formattedRefund = com.example.ui.components.formatCredit(refundAmount)
            SmartNotificationManager.show(
                "$cityName şehrindeki Seviye ${business.level} $facilityName tasfiye edilerek satıldı! (+$formattedRefund nakit hesabınıza yatırıldı)", "Level ${business.level} $facilityName in $cityName liquidated and sold! (+$formattedRefund deposited to your cash account)",
                NotificationType.SUCCESS
            )
            syncCloudSaveToSupabase(force = true, immediate = true)
        }
    }

    internal fun calculateDistance(city1Id: String, city2Id: String): Float {
        if (city1Id == city2Id) return 0f
        val city1 = com.example.data.cities.find { it.id == city1Id } ?: return 0.5f
        val city2 = com.example.data.cities.find { it.id == city2Id } ?: return 0.5f

        val isBothDomestic = city1.country == "Türkiye" && city2.country == "Türkiye"
        if (isBothDomestic) {
            val x1 = city1.relativeX
            val y1 = city1.relativeY
            val x2 = city2.relativeX
            val y2 = city2.relativeY
            val rawDist = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
            return if (rawDist > 0f) rawDist.coerceIn(0.12f, 1.25f) else 0.12f
        }

        // Kıtalararası / Transatlantik mesafe (örn: Türkiye ⇄ ABD / New York veya Uzak Doğu)
        val isIntercontinental = (city1.country in listOf("ABD", "Japonya", "Çin", "Brezilya") && city2.country !in listOf("ABD", "Japonya", "Çin", "Brezilya")) ||
                                 (city2.country in listOf("ABD", "Japonya", "Çin", "Brezilya") && city1.country !in listOf("ABD", "Japonya", "Çin", "Brezilya"))
        if (isIntercontinental) {
            return 3.2f
        }

        val isInternational = city1.country != city2.country
        if (isInternational) {
            return 1.8f
        }

        val x1 = city1.relativeX
        val y1 = city1.relativeY
        val x2 = city2.relativeX
        val y2 = city2.relativeY
        val rawDist = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        return if (rawDist > 0f) rawDist.coerceIn(0.2f, 2.0f) else 0.2f
    }

    fun tradeWithCity(targetCityId: String) {
        viewModelScope.launch {
            val p = player.value ?: return@launch
            val logisticsCost = calculateLogisticsCost(p.currentCity, targetCityId, quantity = 1, baseTariff = 50000f)
            val baseRevenue = 5000L

            if (p.money >= logisticsCost) {
                val currentP = player.value ?: p
                val netRevenue = baseRevenue - logisticsCost
                val xpGain = 50

                val updatedPlayer = currentP.copy(
                    money = currentP.money + netRevenue,
                    dailyIncome = currentP.dailyIncome + baseRevenue,
                    dailyExpense = currentP.dailyExpense + logisticsCost
                )
                repository.updatePlayer(updatedPlayer)
                processXpGain(xpGain, updatedPlayer)
            }
        }
    }




    internal fun startRealTimeSyncLoop() {
        viewModelScope.launch {
            val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            var tickCount = 30
            var managerDutyCounter = 0
            while (true) {
                if (!_isAppInForeground.value) {
                    delay(5000)
                    continue
                }
                val now = System.currentTimeMillis()
                _realTimeClockText.value = formatter.format(java.util.Date(now))

                // Determine Real-World Shift (Day Shift: 08:00 - 20:00 / Night Shift: 20:00 - 08:00)
                val cal = java.util.Calendar.getInstance()
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                if (hour in 8..19) {
                    _realTimeShiftName.value = "☀️ Gündüz Vardiyası"
                    _realTimeShiftBonusText.value = "+%10 Verimlilik Primi"
                } else {
                    _realTimeShiftName.value = "🌙 Gece Vardiyası"
                    _realTimeShiftBonusText.value = "-%15 Bakım Maliyeti"
                }

                // Next Daily Reward Countdown Timer (Resets at midnight 00:00)
                val p = player.value
                if (p != null) {
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = now
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val todayMidnight = cal.timeInMillis
                    if (p.lastDailyRewardMs >= todayMidnight) {
                        val nextMidnight = todayMidnight + 24 * 60 * 60 * 1000L
                        _nextDailyRewardRemainingMs.value = (nextMidnight - now).coerceAtLeast(0L)
                    } else {
                        _nextDailyRewardRemainingMs.value = 0L
                    }
                } else {
                    _nextDailyRewardRemainingMs.value = 0L
                }

                // Bank Daily Settlement Countdown and Trigger
                _nextBankSettlementRemainingMs.value = com.example.data.BankDailySettlementManager.getRemainingMillisUntilNextMidnight()
                updateDailyBankingMetrics()
                checkAndPerformBankDailySettlement(showNotification = true)

                // R&D Research Countdown
                val activeResMap = _activeResearches.value
                if (activeResMap.isNotEmpty()) {
                    val completedKeys = mutableListOf<String>()
                    activeResMap.forEach { (techKey, endMs) ->
                        if (endMs > 0L && now >= endMs) {
                            completedKeys.add(techKey.removePrefix("tech_"))
                        }
                    }
                    if (completedKeys.isNotEmpty()) {
                        completedKeys.distinct().forEach { techKey ->
                            completeResearch(techKey)
                        }
                    }
                    val remainingResMap = _activeResearches.value
                    if (remainingResMap.isNotEmpty()) {
                        val firstEntry = remainingResMap.entries.first()
                        _activeResearchTechKey.value = firstEntry.key.removePrefix("tech_")
                        _researchEndTimeMs.value = firstEntry.value
                        _researchRemainingMs.value = (firstEntry.value - now).coerceAtLeast(0L)
                    } else {
                        _activeResearchTechKey.value = null
                        _researchEndTimeMs.value = 0L
                        _researchRemainingMs.value = 0L
                    }
                } else {
                    _activeResearchTechKey.value = null
                    _researchEndTimeMs.value = 0L
                    _researchRemainingMs.value = 0L
                }

                // Borsa Tick Countdown
                tickCount--
                if (tickCount <= 0) {
                    tickCount = 30
                }
                _nextBorsaTickSeconds.value = tickCount

                // Manager Automated Duties & AI Arbitrage Engine
                managerDutyCounter++
                if (managerDutyCounter % 3 == 0) {
                    checkAndExecuteBorsaLimitOrders()
                }
                if (managerDutyCounter >= 10) {
                    managerDutyCounter = 0
                    processManagerAutomatedDuties()
                }

                // Lojistik Sevkiyatların İşlenmesi
                checkAndProcessActiveDeliveries()

                // Tesis Seviye Yükseltme İnşaatlarının İşlenmesi
                checkAndProcessFacilityUpgrades()
                // Tesis İlk Kurulum İnşaatlarının İşlenmesi
                checkAndProcessFacilityConstructions()
                // Aktif Üretimlerin İşlenmesi
                checkAndProcessActiveProductions()

                delay(1000)
            }
        }
    }

    /**
     * KAPSAMLI ÇEVRİMDİŞI VE BULUT İLERLEME HESAPLAMA MOTORU (OFFLINE & CLOUD ENGINE)
     * Oyuncu çevrimdışı kaldığı süre boyunca:
     * 1. Fabrika/Tesis işletme giderleri ve Aktif Yönetici Maaşları düşülür.
     * 2. Vadeli mevduat/Merkez Bankası mevduat faiz getirileri hesaba eklenir.
     * 3. Üretim Müdürü aktifse oto-üretim döngüleri, Lojistik Müdürü aktifse oto-satışlar gerçekleşir.
     * 4. Yoldaki lojistik siparişleri ve teslimat ürünleri stoka aktarılır.
     * 5. Tamamlanan Ar-Ge araştırmaları otomatik sonuçlandırılır.
     * 6. Güncellenen tüm oyuncu bakiye, stok ve durum bilgisi doğrudan buluta (Supabase) senkronize edilir.
     */
    internal fun calculateAndApplyOfflineEarnings(offlineSeconds: Long, offlineMs: Long) {
        viewModelScope.launch {
            val p = player.value ?: return@launch
            val myBusinesses = businesses.value
            val now = System.currentTimeMillis()

            // 0. Fetch remote guilds / mega projects to ensure HR manager operates on latest consortium state
            val remoteGuilds = com.example.data.SupabaseManager.fetchGuildsFromSupabase()
            if (remoteGuilds != null) {
                val remoteProjects = remoteGuilds.mapNotNull { com.example.data.SupabaseManager.guildToMegaProject(it) }
                syncMegaProjectsFromRemote(remoteProjects)
            }

            val pId = p.id
            val pName = p.name
            val cleanEmail = _onlineEmail.value.replace(".", "_")
            val emailPrefix = _onlineEmail.value.substringBefore("@")

            fun isUserSlotLocal(slot: com.example.data.ConsortiumSupplierSlot): Boolean {
                if (slot.assignedPartnerId == "local_player" || slot.assignedPartnerId == pId) return true
                if (pName.isNotBlank() && slot.assignedPartnerName == pName) return true
                if (cleanEmail.isNotBlank() && slot.assignedPartnerId?.contains(cleanEmail) == true) return true
                if (emailPrefix.isNotBlank() && slot.assignedPartnerId?.contains(emailPrefix) == true) return true
                return false
            }

            val activeHiredManagers = _managers.value.filter { it.isHired && it.isActive }
            val prodManager = activeHiredManagers.find { it.id == "mgr_prod" }
            val logManager = activeHiredManagers.find { it.id == "mgr_logistics" }
            val hrManager = activeHiredManagers.find { it.id == "mgr_hr" }
            val contractsManager = activeHiredManagers.find { it.id == "mgr_contracts" }
            val treasuryManager = activeHiredManagers.find { it.id == "mgr_treasury" }
            val maintenanceManager = activeHiredManagers.find { it.id == "mgr_maintenance" }

            val isAutoProd = prodManager != null
            val isAutoSell = logManager != null
            val isAutoHr = hrManager != null

            var totalItemsProduced = 0
            var totalRevenue = 0L
            var totalUpkeepCost = 0L
            var consortiumDeliveriesCount = 0
            var consortiumDividendsEarned = 0L
            val managerActionsSummary = mutableListOf<String>()
            val producedItemsMap = mutableMapOf<String, Int>()

            val offlineDaysFraction = (offlineSeconds / 86400.0f).coerceAtLeast(0.001f)

            // 1. Tesis Günlük Giderleri
            if (myBusinesses.isNotEmpty()) {
                val greenEnergyDiscount = (1.0f - (_techGreenEnergy.value * 0.08f)).coerceAtLeast(0.6f)
                val dailyUpkeepPerDay = myBusinesses.sumOf { b ->
                    val baseCost = com.example.data.Product.values().find { it.facilityId == b.type }?.facilityCost ?: 5000L
                    val cityProfile = com.example.data.cities.find { it.id == b.cityId }
                    val laborMult = cityProfile?.laborCostMultiplier ?: 1.0f
                    (baseCost * b.level * 0.0015f * laborMult * greenEnergyDiscount).toLong()
                }
                totalUpkeepCost += (dailyUpkeepPerDay * offlineDaysFraction).toLong()
            }

            // 2. Aktif İşe Alınmış Yönetici Maaş Giderleri
            val dailyManagerSalaries = activeHiredManagers.sumOf { it.dailySalary }
            val offlineManagerSalaries = (dailyManagerSalaries * offlineDaysFraction).toLong()
            totalUpkeepCost += offlineManagerSalaries

            // 3. Mevduat Faiz Getirisi
            val depositRate = gameState.value?.centralBankDepositRate ?: 0.12f
            val offlineDepositInterest = (p.depositBalance * (depositRate / 365.0f) * offlineDaysFraction).toLong().coerceAtLeast(0L)
            totalRevenue += offlineDepositInterest

            var currentMoney = p.money
            var currentDeposit = p.depositBalance

            // Manager Duty Simulation Cycles (1 cycle per 60 seconds offline, max 720 cycles = 12 hours)
            val managerCycles = (offlineSeconds / 60L).toInt().coerceIn(1, 720)

            // 4a. HR Manager Consortium Automation & Alignment
            if (isAutoHr) {
                val levelMultiplier = hrManager?.level ?: 1
                val eff = hrManager?.efficiency ?: 1.0f

                val activeConsortiums = _megaProjects.value.filter { proj ->
                    proj.leaderPlayerId == "local_player" ||
                    proj.leaderPlayerId == pId ||
                    (pName.isNotBlank() && proj.leaderPlayerName == pName) ||
                    (cleanEmail.isNotBlank() && proj.leaderPlayerId.contains(cleanEmail)) ||
                    (emailPrefix.isNotBlank() && proj.leaderPlayerId.contains(emailPrefix)) ||
                    proj.slots.any { isUserSlotLocal(it) }
                }.toMutableList()

                if (activeConsortiums.isNotEmpty()) {
                    for (cycle in 1..managerCycles) {
                        for (i in activeConsortiums.indices) {
                            val proj = activeConsortiums[i]
                            val unmetSlots = proj.slots.filter { isUserSlotLocal(it) && !it.isFullyDelivered }

                            for (slot in unmetSlots) {
                                val stock = inventory.value.find { it.itemId == slot.productId }?.quantity ?: 0

                                if (stock > 0) {
                                    val deliverQty = minOf(stock, slot.remainingQuantity)
                                    deliverMaterialsToConsortium(proj.id, slot.slotId, deliverQty, isSilent = true)
                                    consortiumDeliveriesCount += deliverQty
                                    break
                                } else {
                                    val unitPrice = marketPrices.value.find { it.itemId == slot.productId }?.price 
                                        ?: com.example.data.Product.values().find { it.id == slot.productId }?.basePrice ?: 50_000L

                                    // If money is low, treasury finances it from deposit
                                    if (currentMoney < unitPrice && currentDeposit > unitPrice) {
                                        val neededFunding = minOf(currentDeposit, 500_000L)
                                        currentDeposit -= neededFunding
                                        currentMoney += neededFunding
                                    }

                                    val maxAffordableQty = (currentMoney / unitPrice).toInt()
                                    val buyQty = minOf(10 * levelMultiplier, slot.remainingQuantity, maxAffordableQty)

                                    if (buyQty > 0) {
                                        val cost = unitPrice * buyQty
                                        currentMoney -= cost
                                        repository.produceItem(slot.productId, buyQty)
                                        deliverMaterialsToConsortium(proj.id, slot.slotId, buyQty, isSilent = true)
                                        consortiumDeliveriesCount += buyQty
                                        break
                                    }
                                }
                            }

                            // Check mass production dividend yield per cycle
                            if (proj.isAllStagesFinished && proj.isMassProductionApproved) {
                                val myShareSlots = proj.slots.filter { isUserSlotLocal(it) }
                                val totalDeliveredMySlots = myShareSlots.sumOf { it.quantityDelivered.toLong() }
                                val dividendPerCycle = (totalDeliveredMySlots * 1500L * eff).toLong()
                                if (dividendPerCycle > 0) {
                                    consortiumDividendsEarned += dividendPerCycle
                                    totalRevenue += dividendPerCycle
                                }
                            } else if (proj.isAllStagesFinished && !proj.isMassProductionApproved) {
                                approveConsortiumMassProduction(proj.id, isSilent = true)
                            }
                        }
                    }
                    if (consortiumDeliveriesCount > 0) {
                        managerActionsSummary.add("İnsan Kaynakları & Lojistik: Konsorsiyumlara $consortiumDeliveriesCount Ton ürün sevk edildi.")
                    }
                    if (consortiumDividendsEarned > 0) {
                        managerActionsSummary.add("Konsorsiyum Seri Üretim: +₳${com.example.ui.components.formatMoney(consortiumDividendsEarned)} temettü kârı tahsil edildi.")
                    }
                }
            }

            // 4b. Logistics Manager Sales (Reserves Consortium Goods)
            if (isAutoSell) {
                val totalSellAttempts = minOf(managerCycles, 50)
                var totalOfflineLogisticsSales = 0L
                val activeConsortiums = _megaProjects.value.filter { proj ->
                    proj.leaderPlayerId == "local_player" || proj.leaderPlayerId == pId || proj.slots.any { isUserSlotLocal(it) }
                }
                val neededMap = mutableMapOf<String, Int>()
                activeConsortiums.forEach { proj ->
                    proj.slots.filter { isUserSlotLocal(it) && !it.isFullyDelivered }.forEach { s ->
                        neededMap[s.productId] = (neededMap[s.productId] ?: 0) + s.remainingQuantity
                    }
                }

                repeat(totalSellAttempts) {
                    val currentInv = inventory.value.filter { it.quantity > 0 }
                    val sellable = currentInv.mapNotNull { item ->
                        val needed = neededMap[item.itemId] ?: 0
                        val surplus = (item.quantity - needed).coerceAtLeast(0)
                        if (surplus > 0) Pair(item, surplus) else null
                    }
                    if (sellable.isNotEmpty()) {
                        val (itemToSell, maxSurplus) = sellable.random()
                        val sellQty = minOf(maxSurplus, (10 * (logManager?.level ?: 1) * (logManager?.efficiency ?: 1f)).toInt().coerceAtLeast(1))
                        val unitPrice = marketPrices.value.find { it.itemId == itemToSell.itemId }?.price ?: 500L
                        val revenue = sellQty.toLong() * unitPrice
                        repository.sellItem(itemToSell.itemId, sellQty, unitPrice)
                        totalRevenue += revenue
                        totalOfflineLogisticsSales += revenue
                    }
                }
                if (totalOfflineLogisticsSales > 0) {
                    managerActionsSummary.add("Lojistik Müdürü: İhtiyaç fazlası pazar satışlarından +₳${com.example.ui.components.formatMoney(totalOfflineLogisticsSales)} elde edildi.")
                }
            }

            // 4c. Contracts Manager Fulfillment
            if (contractsManager != null) {
                val activeContracts = _futuresContracts.value.filter { !it.isFulfilled }
                var totalContractRevenue = 0L
                activeContracts.forEach { c ->
                    val stock = inventory.value.find { it.itemId == c.itemId }?.quantity ?: 0
                    if (stock >= c.quantity) {
                        val totalVal = c.lockedPricePerUnit * c.quantity
                        fulfillFuturesContract(c.id)
                        totalRevenue += totalVal
                        totalContractRevenue += totalVal
                    }
                }
                if (totalContractRevenue > 0) {
                    managerActionsSummary.add("Vadeli Sözleşmeler Müdürü: B2B teslimatlardan +₳${com.example.ui.components.formatMoney(totalContractRevenue)} kazanıldı.")
                }
            }

            // 4d. Treasury Manager Liquidity Balancing
            if (treasuryManager != null) {
                if (currentMoney < 200_000L && currentDeposit > 0L) {
                    val withdrawAmt = minOf(currentDeposit, 500_000L * treasuryManager.level)
                    currentMoney += withdrawAmt
                    currentDeposit -= withdrawAmt
                    managerActionsSummary.add("Hazine Müdürü: Operasyon finansmanı için mevduattan ₳${com.example.ui.components.formatMoney(withdrawAmt)} çekildi.")
                } else if (currentMoney > 2_000_000L) {
                    val depositAmt = minOf(100_000L * treasuryManager.level * (offlineSeconds / 3600L).coerceAtLeast(1L), (currentMoney * 0.1).toLong()).coerceAtLeast(10_000L)
                    if (currentMoney >= depositAmt) {
                        currentMoney -= depositAmt
                        currentDeposit += depositAmt
                        managerActionsSummary.add("Hazine Müdürü: ₳${com.example.ui.components.formatMoney(depositAmt)} likidite vadeli mevduata yatırıldı.")
                    }
                }
            }

            // 4e. Maintenance Manager Repairs
            if (maintenanceManager != null) {
                val wornBusinesses = myBusinesses.filter { it.wearLevel > 0.5f }
                var totalRepairCost = 0L
                wornBusinesses.forEach { target ->
                    val repairCost = 15_000L * target.level
                    if (currentMoney < repairCost && currentDeposit >= repairCost) {
                        currentDeposit -= repairCost
                        currentMoney += repairCost
                    }
                    if (currentMoney >= repairCost) {
                        currentMoney -= repairCost
                        totalRepairCost += repairCost
                        val repaired = target.copy(wearLevel = 0f)
                        repository.updateBusiness(repaired)
                    }
                }
                if (totalRepairCost > 0) {
                    totalUpkeepCost += totalRepairCost
                    managerActionsSummary.add("Bakım Müdürü: Yıpranan tesislerin bakımı tamamlandı (-₳${com.example.ui.components.formatMoney(totalRepairCost)}).")
                }
            }

            // 4f. AI Borsa Arbitraj Botu & Limit Emirleri
            val activeOrders = _borsaLimitOrders.value.filter { it.isActive }
            if (activeOrders.isNotEmpty()) {
                var totalOfflineArbitrageProfit = 0L
                var totalOfflineArbitrageExecutions = 0
                val borsaLevel = activeHiredManagers.find { it.id == "mgr_borsa" }?.level ?: 1

                val simulatedOrders = _borsaLimitOrders.value.toMutableList()
                var ordersChanged = false

                for (idx in simulatedOrders.indices) {
                    val order = simulatedOrders[idx]
                    if (!order.isActive) continue

                    val prod = com.example.data.Product.values().find { it.id == order.itemId } ?: continue
                    val priceEntity = marketPrices.value.find { it.itemId == order.itemId }
                    val currentPrice = priceEntity?.price ?: prod.basePrice

                    val offlineExecs = (offlineSeconds / 1800L).toInt().coerceIn(1, 15)
                    val estSellPrice = if (order.targetSellPrice > 0L) order.targetSellPrice else (currentPrice * 1.2).toLong()
                    val profitPerCycle = ((estSellPrice - prod.basePrice) * order.quantity).coerceAtLeast(500L)
                    val totalProfitForOrder = profitPerCycle * offlineExecs

                    totalOfflineArbitrageProfit += totalProfitForOrder
                    totalOfflineArbitrageExecutions += offlineExecs
                    totalRevenue += totalProfitForOrder

                    val updatedOrder = order.copy(
                        executedCount = order.executedCount + offlineExecs,
                        totalRealizedProfit = order.totalRealizedProfit + totalProfitForOrder,
                        lastExecutedAtMs = System.currentTimeMillis(),
                        isActive = if (order.orderType == com.example.data.LimitOrderType.ARBITRAGE_AUTO) true else order.autoRepeat
                    )
                    simulatedOrders[idx] = updatedOrder
                    ordersChanged = true
                }

                if (ordersChanged) {
                    _borsaLimitOrders.value = simulatedOrders
                    repository.saveBorsaLimitOrders(simulatedOrders)
                }

                if (totalOfflineArbitrageProfit > 0) {
                    managerActionsSummary.add("AI Borsa Arbitraj Botu: $totalOfflineArbitrageExecutions işlem gerçekleştirildi, +₳${com.example.ui.components.formatMoney(totalOfflineArbitrageProfit)} kâr kasaya aktarıldı.")
                }
            }

            // 5. Factory Auto-Production
            if (myBusinesses.isNotEmpty() && isAutoProd) {
                myBusinesses.forEach { b ->
                    val prod = com.example.data.Product.values().find { it.facilityId == b.type } ?: return@forEach
                    val baseDurationSec = (prod.tier.baseDurationMs / 1000L).coerceAtLeast(10L)
                    val prodCycles = (offlineSeconds / baseDurationSec).toInt()
                    val qtyPerCycle = (b.level * 2).coerceAtLeast(1)
                    val producedQty = prodCycles * qtyPerCycle

                    if (producedQty > 0) {
                        totalItemsProduced += producedQty
                        producedItemsMap[prod.id] = (producedItemsMap[prod.id] ?: 0) + producedQty
                        var currentBizState = b.withAddedItem(prod.id, producedQty)

                        if (isAutoSell) {
                            // 100 Ton Kuralı: Bir ürün çeşidinden en az 100 ton biriktiğinde toplu satış yap
                            val sellableBlocks = (producedQty / 100) * 100
                            if (sellableBlocks >= 100) {
                                val marketPrice = marketPrices.value.find { it.itemId == prod.id }?.price ?: prod.basePrice
                                val qualityMult = 1.0f + (_techQualityControl.value * 0.05f)
                                val earned = (sellableBlocks * marketPrice * qualityMult).toLong()
                                totalRevenue += earned
                                currentBizState = currentBizState.withRemovedItem(prod.id, sellableBlocks)
                            }
                        }
                        repository.updateBusiness(currentBizState)
                    }
                }
            }

            // 6. Deliveries in Transit
            val currentDeliveries = _activeDeliveries.value
            if (currentDeliveries.isNotEmpty()) {
                val arrivedDeliveries = currentDeliveries.filter { (now - it.startTimeMs) >= it.totalDurationMs }
                if (arrivedDeliveries.isNotEmpty()) {
                    var deliveryBonus = 0L
                    arrivedDeliveries.forEach { item ->
                        if (!item.isOutboundSale) {
                            repository.produceItem(item.itemId, item.quantity)
                        }
                        deliveryBonus += item.totalCost / 10L
                    }
                    totalRevenue += deliveryBonus
                    _activeDeliveries.value = currentDeliveries.filter { (now - it.startTimeMs) < it.totalDurationMs }
                }
            }

            // 6b. Active Facility Production Queue
            checkAndProcessActiveProductions(isOffline = true)

            // 7. Offline R&D Completions
            val activeResMap = _activeResearches.value
            if (activeResMap.isNotEmpty()) {
                val completedKeys = activeResMap.filter { (_, endMs) -> endMs > 0L && endMs <= now }.keys.distinct()
                completedKeys.forEach { techKey ->
                    completeResearch(techKey)
                }
            } else {
                val activeTechKey = _activeResearchTechKey.value
                val endResearchMs = _researchEndTimeMs.value
                if (!activeTechKey.isNullOrBlank() && endResearchMs > 0L && endResearchMs <= now) {
                    completeResearch(activeTechKey)
                }
            }

            // 7b. Offline Facility Upgrades & Constructions
            checkAndProcessFacilityUpgrades(isOffline = true)
            checkAndProcessFacilityConstructions(isOffline = true)

            // 8. Net Balance Calculation
            val netBalanceChange = totalRevenue - totalUpkeepCost

            if (netBalanceChange < 0) {
                val shortfall = kotlin.math.abs(netBalanceChange)
                if (currentMoney >= shortfall) {
                    currentMoney -= shortfall
                } else {
                    val remainingShortfall = shortfall - currentMoney
                    currentMoney = 0L
                    currentDeposit = (currentDeposit - remainingShortfall).coerceAtLeast(0L)
                }
            } else {
                currentMoney += netBalanceChange
            }

            val xpGain = (totalItemsProduced * 2 + (offlineSeconds / 300L) + consortiumDeliveriesCount).toInt().coerceAtLeast(15)

            val updatedPlayer = p.copy(
                money = currentMoney,
                depositBalance = currentDeposit,
                xp = p.xp + xpGain
            )
            repository.updatePlayer(updatedPlayer)

            val completedDeliveriesCount = if (currentDeliveries.isNotEmpty()) currentDeliveries.count { (now - it.startTimeMs) >= it.totalDurationMs } else 0

            _offlineEarningsData.value = OfflineEarningsData(
                offlineDurationMs = offlineMs,
                netMoneyEarned = netBalanceChange,
                itemsProducedCount = totalItemsProduced,
                xpGained = xpGain,
                grossRevenue = totalRevenue,
                totalUpkeepCost = totalUpkeepCost,
                depositInterestEarned = offlineDepositInterest,
                producedItemsSummary = producedItemsMap,
                activeFacilitiesCount = myBusinesses.size,
                deliveriesCompletedCount = completedDeliveriesCount,
                consortiumDeliveriesCount = consortiumDeliveriesCount,
                consortiumDividendsEarned = consortiumDividendsEarned,
                managerActionsSummary = managerActionsSummary
            )

            saveEconomicDataToDataStore(customPlayer = updatedPlayer)

            val offlineHours = offlineSeconds / 3600
            val offlineMins = (offlineSeconds % 3600) / 60
            val timeStr = if (offlineHours > 0) "${offlineHours} saat ${offlineMins} dk" else "${offlineMins} dakika"

            val consortiumMsg = if (consortiumDeliveriesCount > 0) " | Konsorsiyum: $consortiumDeliveriesCount Ton" else ""

            SmartNotificationManager.show(
                "🌙 Çevrimdışı İlerleme ($timeStr): Net Bakiye: ₳${com.example.ui.components.formatMoney(netBalanceChange)}$consortiumMsg | Bulut Senkronize!",
                "🌙 Offline Progress ($timeStr): Net Balance: ₳${com.example.ui.components.formatMoney(netBalanceChange)}$consortiumMsg | Cloud Synced!",
                NotificationType.SUCCESS
            )

            if (_isOnlineRegistered.value) {
                syncCloudSaveToSupabase()
            }
        }
    }

    init {
        startUiStateSync(viewModelScope)
        checkAndClaimMonthlyLeaderboardReward()

        viewModelScope.launch {
            while (true) {
                delay(15000L)
                val newEvents = com.example.data.CityNewsEventManager.updateEvents(_activeCityEvents.value)
                if (_activeCityEvents.value != newEvents) {
                    _activeCityEvents.value = newEvents
                }
            }
        }
        _managers.value = sanitizeManagerSalaries(_managers.value)
        _megaProjects.value = emptyList()
        viewModelScope.launch {
            val snapshot = repository.initializeGame()
            if (snapshot != null) {
                val dbBusinesses = try {
                    repository.getAllBusinessesDirect()
                } catch (_: Exception) {
                    emptyList()
                }

                applySnapshotToState(snapshot)
                
                checkAndProcessFacilityConstructions(isOffline = true, explicitBizList = dbBusinesses)
                checkAndProcessFacilityUpgrades(isOffline = true, explicitBizList = dbBusinesses)
                checkAndProcessActiveProductions(isOffline = true, explicitBizList = dbBusinesses)
                checkAndProcessActiveDeliveries()

                // Check Real-Time Offline Production with Enterprise Anti-Cheat Time Verification
                val offlineMs = com.example.data.security.TimeSecurityManager.validateAndComputeOfflineDuration(
                    lastSavedTimeMs = snapshot.lastSavedTime,
                    lastSavedElapsedRealtime = snapshot.lastKnownElapsedRealtime
                )
                val offlineSeconds = offlineMs / 1000L
                if (offlineSeconds >= 60) {
                    calculateAndApplyOfflineEarnings(offlineSeconds, offlineMs)
                }

                // Check and settle museum auctions that ended while player was offline
                checkOfflineMuseumAuctions()

                val savedEmail = snapshot.onlineEmail.trim()
                if (savedEmail.isNotBlank() && savedEmail != "misafir_tuccar" && !savedEmail.startsWith("guest")) {
                    _isOnlineRegistered.value = true
                    _onlineEmail.value = savedEmail
                } else if (snapshot.isOnlineRegistered) {
                    _isOnlineRegistered.value = true
                }
            }

            _borsaLimitOrders.value = repository.getBorsaLimitOrders()
            
            repository.startListeningToGlobalMarketBundle { bundle ->
                syncGlobalMarketBundle(bundle)
            }
            repository.startListeningToGlobalBorsaPrices { updatedPrices ->
                updatePriceHistory(updatedPrices)
            }
            repository.startListeningToGlobalMegaProjects { remoteProjects ->
                syncMegaProjectsFromRemote(remoteProjects)
            }

            // Trigger Daily Bonus check after initial load
            kotlinx.coroutines.delay(2000)
            
            // Self-healing: if a beginner player has 0 balance due to the initial warehouse setup bug, restore their 100,000 Anatolian Liras starting capital
            val curP = player.value
            if (curP != null && curP.money <= 0L && curP.level == 1 && curP.depositBalance == 0L && curP.loanAmount == 0L && curP.totalProfit == 0L) {
                val restoredP = curP.copy(money = 100_000L)
                repository.updatePlayer(restoredP)
                saveEconomicDataToDataStore(customPlayer = restoredP, immediate = true)
            }

            checkDailyLoginBonus()
            checkAndPerformBankDailySettlement(showNotification = true)
        }
        viewModelScope.launch {
            val currentInv = repository.inventory.first()
            if (currentInv.isNotEmpty()) {
                Product.values().forEach { product ->
                    if (currentInv.none { it.itemId == product.id }) {
                        repository.insertInventory(InventoryEntity(product.id, 0))
                    }
                }
            }
        }
        // Register direct Billing callback for instantaneous zero-latency diamond crediting
        com.example.data.billing.BillingManager.onPurchaseRewardCallback = { productId, gems, orderId, _ ->
            onIapPurchaseSuccess(productId, gems, orderId)
        }
        // Collect Google Play Billing purchase events
        viewModelScope.launch {
            try {
                com.example.data.billing.BillingManager.purchaseEvents.collect { event ->
                    when (event) {
                        is com.example.data.billing.BillingManager.PurchaseEvent.Success -> {
                            onIapPurchaseSuccess(event.productId, event.gems, event.orderId)
                        }
                        is com.example.data.billing.BillingManager.PurchaseEvent.Error -> {
                            SmartNotificationManager.show(
                                "Satın alma uyarısı: ${event.message}",
                                "Purchase notice: ${event.message}",
                                NotificationType.ALERT
                            )
                        }
                        is com.example.data.billing.BillingManager.PurchaseEvent.Message -> {
                            SmartNotificationManager.show(
                                event.message,
                                event.message,
                                NotificationType.INFO
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.w("GameViewModel", "BillingManager events collect error", e)
            }
        }
        // Collect Consortium Chat Broadcasts
        viewModelScope.launch {
            try {
                com.example.data.MultiplayerManager.broadcastChatFlow.collect { chatMsg ->
                    _consortiumChatMessages.update { current ->
                        val list = current[chatMsg.projectId] ?: emptyList()
                        // Ensure we don't duplicate
                        if (list.none { it.id == chatMsg.id }) {
                            current + (chatMsg.projectId to (list + chatMsg))
                        } else {
                            current
                        }
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.w("GameViewModel", "Consortium Chat collect error", e)
            }
        }

        startBorsaEconomyLoop()
        startRealTimeSyncLoop()
        startAutoLevelCheckLoop()
        startCentralBankSimulation()
        startPublicConsumptionLoop()
        startLogisticsDeliveryLoop()
        startAutomationLoop()
        startConsortiumSupplyLoop()
        startRealTimeEconomyLoop()
        startMidnightResetLoop()
        startDemoPlayerBotLoop()
        startAutoSaveDataStoreLoop()
        startSupabaseAutoSyncLoop()
    }

                }
