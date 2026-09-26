package com.example.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class GameRepository(
    private val gameDao: GameDao,
    val economicDataStore: EconomicDataStore? = null
) {
    companion object {
        private const val TAG = "GameRepository"
    }

    val player: Flow<PlayerEntity?> = gameDao.getPlayer()
    val inventory: Flow<List<InventoryEntity>> = gameDao.getInventory()
    val businesses: Flow<List<BusinessEntity>> = gameDao.getBusinesses()
    val marketPrices: Flow<List<MarketPriceEntity>> = gameDao.getMarketPrices().map { list -> sanitizeMarketPrices(list) }
    val gameState: Flow<GameStateEntity?> = gameDao.getGameState()
    val economicSnapshotFlow: Flow<EconomicSnapshot?> = economicDataStore?.economicSnapshotFlow ?: flowOf(null)

    // StateFlow for network/repository error observability
    private val _networkErrorState = MutableStateFlow<String?>(null)
    val networkErrorState: StateFlow<String?> = _networkErrorState.asStateFlow()

    // High Frequency Supabase Realtime Batching Engine (~16ms frame target for ~6ms latency event streams)
    val marketPriceBatchProcessor = com.example.data.remote.supabase.RealtimeBatchProcessor<String, com.example.data.remote.supabase.MarketPriceDto>(
        frameIntervalMs = 16L,
        keySelector = { it.symbol }
    )
    val batchedMarketPrices: Flow<Map<String, com.example.data.remote.supabase.MarketPriceDto>> = marketPriceBatchProcessor.batchedState

    // Managed jobs to prevent uncontrolled background execution
    private var globalMarketJob: Job? = null
    private var globalMacroJob: Job? = null
    private val consortiumChatJobs = ConcurrentHashMap<String, Job>()

    fun startListeningToGlobalMarketBundle(onUpdate: (SupabaseManager.GlobalMarketBundle) -> Unit): Job {
        globalMarketJob?.cancel()
        val job = CoroutineScope(Dispatchers.IO).launch {
            // 1. Canlı Realtime WebSocket yayınlarını dinle ve anında pazar paketini güncelle
            launch {
                MultiplayerManager.broadcastMarketActionFlow.collect { event ->
                    try {
                        val bundle = SupabaseManager.fetchGlobalMarketBundleFromSupabase()
                        if (bundle != null && isActive) {
                            withContext(Dispatchers.Main) {
                                onUpdate(bundle)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Broadcast tetiklemeli pazar paketi senkronizasyonu hatası: ${e.message}", e)
                    }
                }
            }

            // 2. Periyodik olarak (her 10 saniyede bir) Supabase küresel pazar paketini sorgula
            while (isActive) {
                try {
                    val bundle = SupabaseManager.fetchGlobalMarketBundleFromSupabase()
                    if (bundle != null && isActive) {
                        withContext(Dispatchers.Main) {
                            onUpdate(bundle)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch global market bundle: ${e.message}", e)
                }
                kotlinx.coroutines.delay(10000)
            }
        }
        globalMarketJob = job
        return job
    }

    fun startListeningToGlobalMarket(onUpdate: (List<MarketListing>) -> Unit): Job {
        return startListeningToGlobalMarketBundle { bundle ->
            onUpdate(bundle.listings)
        }
    }

    suspend fun fetchGlobalMarketOnce(): List<MarketListing>? {
        return SupabaseManager.fetchGlobalMarketFromSupabase()
    }

    suspend fun fetchGlobalMarketBundleOnce(): SupabaseManager.GlobalMarketBundle? {
        return SupabaseManager.fetchGlobalMarketBundleFromSupabase()
    }

    fun syncFuturesContractToSupabase(contract: FuturesContract) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.syncFuturesContractToSupabase(contract)
        }
    }

    fun deleteFuturesContractFromSupabase(contractId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.deleteFuturesContractFromSupabase(contractId)
        }
    }

    fun syncBuyOrderToSupabase(order: BuyOrder) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.syncBuyOrderToSupabase(order)
        }
    }

    fun deleteBuyOrderFromSupabase(orderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.deleteBuyOrderFromSupabase(orderId)
        }
    }

    fun startListeningToGlobalFutures(onUpdate: (List<FuturesContract>) -> Unit): Job {
        return startListeningToGlobalMarketBundle { bundle ->
            onUpdate(bundle.futuresContracts)
        }
    }

    fun startListeningToGlobalBuyOrders(onUpdate: (List<BuyOrder>) -> Unit): Job {
        return startListeningToGlobalMarketBundle { bundle ->
            onUpdate(bundle.buyOrders)
        }
    }

    fun startListeningToGlobalMegaProjects(onUpdate: (List<MegaProject>) -> Unit): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            MultiplayerManager.globalGuilds.collect { guilds ->
                val megaProjects = guilds.mapNotNull { guild ->
                    SupabaseManager.guildToMegaProject(guild)
                }
                if (megaProjects.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onUpdate(megaProjects)
                    }
                }
            }
        }
    }

    private var borsaSyncJob: Job? = null
    private var lastBorsaSyncMs: Long = 0L

    /**
     * Real-time listener for universal global Borsa market prices across all players via Supabase.
     */
    fun startListeningToGlobalBorsaPrices(onUpdate: (List<MarketPriceEntity>) -> Unit): Job {
        borsaSyncJob?.cancel()
        val job = CoroutineScope(Dispatchers.IO).launch {
            // 1. Initial fetch from Supabase
            try {
                val remotePrices = SupabaseManager.fetchMarketPrices()
                if (!remotePrices.isNullOrEmpty()) {
                    updateMarketPrices(remotePrices, syncToRemote = false)
                    withContext(Dispatchers.Main) {
                        onUpdate(remotePrices)
                    }
                } else {
                    val defaultPrices = sanitizeMarketPrices(emptyList())
                    updateMarketPrices(defaultPrices, syncToRemote = true)
                    withContext(Dispatchers.Main) {
                        onUpdate(defaultPrices)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching initial global Borsa prices", e)
                val defaultPrices = sanitizeMarketPrices(emptyList())
                updateMarketPrices(defaultPrices, syncToRemote = true)
                withContext(Dispatchers.Main) {
                    onUpdate(defaultPrices)
                }
            }

            // 2. Real-time broadcast channel from other players
            launch {
                MultiplayerManager.broadcastBorsaPricesFlow.collect { incomingPrices ->
                    if (incomingPrices.isNotEmpty()) {
                        updateMarketPrices(incomingPrices, syncToRemote = false)
                        withContext(Dispatchers.Main) {
                            onUpdate(incomingPrices)
                        }
                    }
                }
            }

            // 3. Periodic background synchronization from Supabase table
            launch {
                while (isActive) {
                    delay(12_000L)
                    try {
                        val remotePrices = SupabaseManager.fetchMarketPrices()
                        if (!remotePrices.isNullOrEmpty()) {
                            updateMarketPrices(remotePrices, syncToRemote = false)
                            withContext(Dispatchers.Main) {
                                onUpdate(remotePrices)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in periodic global Borsa poll", e)
                    }
                }
            }
        }
        borsaSyncJob = job
        return job
    }

    fun syncGlobalBorsaPricesToSupabase(prices: List<MarketPriceEntity>, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastBorsaSyncMs < 2000L) return
        lastBorsaSyncMs = now

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Instantly broadcast to all online players
                MultiplayerManager.sendBroadcastBorsaPrices(prices)
                // Persist asynchronously in Supabase database
                SupabaseManager.syncMarketPrices(prices)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing global Borsa prices to Supabase", e)
            }
        }
    }

    private val localConsortiumChatCache = ConcurrentHashMap<String, MutableList<ConsortiumChatMessage>>()

    /**
     * Yerel Konsorsiyum Sohbet Akışı
     */
    fun consortiumChatFlow(projectId: String): Flow<List<ConsortiumChatMessage>> = flow {
        val cached = localConsortiumChatCache.getOrPut(projectId) { mutableListOf() }
        emit(cached.toList())
    }

    /**
     * Yerel Konsorsiyum Sohbet Dinleyicisi
     */
    fun startListeningToConsortiumChat(projectId: String, onUpdate: (List<ConsortiumChatMessage>) -> Unit): Job {
        consortiumChatJobs[projectId]?.cancel()
        val cached = localConsortiumChatCache.getOrPut(projectId) { mutableListOf() }
        onUpdate(cached.toList())

        val job = CoroutineScope(Dispatchers.Main).launch {
            onUpdate(cached.toList())
        }
        consortiumChatJobs[projectId] = job
        return job
    }

    fun stopListeningToConsortiumChat(projectId: String) {
        consortiumChatJobs.remove(projectId)?.cancel()
    }

    fun sendConsortiumChatMessage(projectId: String, message: ConsortiumChatMessage) {
        val currentList = localConsortiumChatCache.getOrPut(projectId) { mutableListOf() }
        if (currentList.none { it.id == message.id }) {
            currentList.add(message)
        }
        MultiplayerManager.sendBroadcastChatMessage(message)
    }

    fun deleteMegaProjectFromSupabase(projectId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.deleteMegaProjectFromSupabase(projectId)
        }
    }

    fun syncMegaProjectToSupabase(megaProject: MegaProject) {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseManager.syncMegaProjectToSupabase(megaProject)
        }
    }

    fun syncMegaProjectsBatchToSupabase(megaProjects: List<MegaProject>) {
        CoroutineScope(Dispatchers.IO).launch {
            megaProjects.forEach { proj ->
                SupabaseManager.syncMegaProjectToSupabase(proj)
            }
        }
    }

    fun stopListeningToGlobalMarket() {
        globalMarketJob?.cancel()
        globalMarketJob = null
    }

    private var lastMacroSyncMs: Long = 0L

    /**
     * Managed lifecycle listener for universal global Macro Economy state returning a cancelable Job.
     */
    fun startListeningToGlobalMacroState(onUpdate: (MacroEconomyState, Float, Float, Double) -> Unit): Job {
        globalMacroJob?.cancel()
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val map = SupabaseManager.fetchMacroStateFromSupabase()
                    if (map != null && isActive) {
                        val cycleStr = map["cycle"] as? String ?: "RECOVERY"
                        val cycle = try { EconomicCycle.valueOf(cycleStr) } catch (_: Exception) { EconomicCycle.RECOVERY }
                        val inflationRate = (map["globalInflationRate"] as? Double ?: 0.08).toFloat()
                        val interestRate = (map["centralBankInterestRate"] as? Double ?: 0.12).toFloat()
                        val loanRate = (map["centralBankLoanRate"] as? Double ?: 0.22).toFloat()
                        val depositRate = (map["centralBankDepositRate"] as? Double ?: 0.10).toFloat()
                        val liquidity = (map["marketLiquidityMultiplier"] as? Double ?: 1.0).toFloat()
                        val peBase = (map["peRatioBase"] as? Double ?: 10.0).toFloat()
                        val usdTryRate = (map["usdTryRate"] as? Double ?: ForexRateManager.currentUsdRate)
                        
                        val macroState = MacroEconomyState(
                            cycle = cycle,
                            globalInflationRate = inflationRate,
                            centralBankInterestRate = interestRate,
                            marketLiquidityMultiplier = liquidity,
                            peRatioBase = peBase
                        )
                        withContext(Dispatchers.Main) {
                            onUpdate(macroState, loanRate, depositRate, usdTryRate)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Macro state update exception: ${e.message}")
                    _networkErrorState.value = "Makro ekonomi verisi alınamadı."
                }
                delay(300_000) // 5 dakikada bir kontrol et (Tasarruf)
            }
        }
        globalMacroJob = job
        return job
    }

    fun stopListeningToGlobalMacroState() {
        globalMacroJob?.cancel()
        globalMacroJob = null
    }

    fun clearAllListeners() {
        globalMarketJob?.cancel()
        globalMarketJob = null
        globalMacroJob?.cancel()
        globalMacroJob = null
        consortiumChatJobs.values.forEach { it.cancel() }
        consortiumChatJobs.clear()
    }

    fun syncGlobalMacroStateToSupabase(
        macroState: MacroEconomyState,
        centralBankLoanRate: Float,
        centralBankDepositRate: Float,
        usdTryRate: Double = ForexRateManager.currentUsdRate,
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastMacroSyncMs) < 30_000L) {
            return
        }
        lastMacroSyncMs = now
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            SupabaseManager.syncMacroStateToSupabase(
                macroState = macroState,
                centralBankLoanRate = centralBankLoanRate,
                centralBankDepositRate = centralBankDepositRate,
                usdTryRate = usdTryRate
            )
        }
    }

    suspend fun markWarehouseSet() {
        economicDataStore?.markWarehouseSet()
    }

    suspend fun markFirstTradeCompleted() {
        economicDataStore?.markFirstTradeCompleted()
    }

    suspend fun saveOnlineAuth(email: String, passwordHash: String, isRegistered: Boolean) {
        economicDataStore?.saveOnlineAuth(email, passwordHash, isRegistered)
    }

    suspend fun initializeGame(customPlayerId: String? = null): EconomicSnapshot? {
        return withContext(Dispatchers.IO) {
            // 1. Önce cihazdaki DataStore'dan kayıtlı Google giriş durumunu ve son ekonomik durumu oku
            val initialSnapshot = economicDataStore?.getEconomicSnapshot()
            val onlineEmail = initialSnapshot?.onlineEmail.orEmpty().trim()
            val isGoogleAuthed = initialSnapshot?.isOnlineRegistered == true && onlineEmail.isNotBlank() && onlineEmail != "misafir_tuccar"

            val resolvedPlayerId = customPlayerId ?: if (isGoogleAuthed) {
                onlineEmail.replace(".", "_")
            } else {
                "local_player"
            }

            // 2. Eğer Google hesabı ile giriş yapılmışsa, açılışta EN GÜNCEL Supabase yedeğini cihaza çek (Cloud-First)
            if (isGoogleAuthed) {
                val cloudSaveJson = SupabaseManager.fetchPlayerSaveData(onlineEmail)
                    ?: SupabaseManager.fetchPlayerSaveData(resolvedPlayerId)
                
                if (!cloudSaveJson.isNullOrBlank()) {
                    android.util.Log.i("GameRepository", "Cloud save found for Google account $onlineEmail. Restoring directly.")
                    economicDataStore?.importSaveJson(cloudSaveJson, force = true)
                }

                // Supabase'de eskiden kalan geçersiz "local_player" kaydını temizle
                try {
                    SupabaseManager.deletePlayerFromSupabase("local_player")
                } catch (e: Exception) {
                    // ignore
                }
            }

            val snapshot = economicDataStore?.getEconomicSnapshot()
            
            if (snapshot != null && snapshot.isDataSaved) {
                // 3. Buluttan veri başarıyla aktarıldıktan sonra veya yerelde zaten güncel bir veri varsa Room veritabanına aktar.
                val playerEntity = PlayerEntity(
                    id = resolvedPlayerId,
                    name = snapshot.name,
                    money = snapshot.money,
                    loanAmount = snapshot.loanAmount,
                    depositBalance = snapshot.depositBalance,
                    dailyIncome = snapshot.dailyIncome,
                    dailyExpense = snapshot.dailyExpense,
                    totalProfit = snapshot.totalProfit,
                    xp = snapshot.xp,
                    level = snapshot.level,
                    inventoryCapacity = snapshot.inventoryCapacity,
                    currentCity = snapshot.currentCity,
                    isUsdAccount = snapshot.isUsdAccount,
                    isVip = snapshot.isVip,
                    gems = snapshot.gems,
                    lastDailyRewardMs = snapshot.lastDailyRewardMs,
                    loginStreak = snapshot.loginStreak,
                    lockedDepositBalance = snapshot.lockedDepositBalance,
                    lockedDepositStartTimeMs = snapshot.lockedDepositStartTimeMs,
                    lockedDepositDurationMs = snapshot.lockedDepositDurationMs,
                    dollarBalance = snapshot.dollarBalance,
                    dollarDepositBalance = snapshot.dollarDepositBalance,
                    dollarLoanAmount = snapshot.dollarLoanAmount
                )
                gameDao.insertPlayer(playerEntity)

                val gameStateEntity = GameStateEntity(
                    id = "global_state",
                    season = snapshot.season,
                    activeEvent = snapshot.activeEvent,
                    globalInflationRate = snapshot.globalInflationRate,
                    centralBankLoanRate = snapshot.centralBankLoanRate,
                    centralBankDepositRate = snapshot.centralBankDepositRate,
                    totalMarketLiquidity = snapshot.totalMarketLiquidity,
                    usdTryRate = snapshot.usdTryRate
                )
                gameDao.insertGameState(gameStateEntity)

                val restoredBusinesses = economicDataStore.deserializeBusinesses(snapshot.businessesJson)
                val restoredInventory = economicDataStore.deserializeInventory(snapshot.inventoryJson)
                val finalInventory = if (restoredInventory.isNotEmpty()) {
                    restoredInventory
                } else {
                    Product.values().map { product -> InventoryEntity(product.id, 0) }
                }

                val restoredPrices = economicDataStore.deserializeMarketPrices(snapshot.marketPricesJson)
                val finalPrices = if (restoredPrices.isNotEmpty()) {
                    restoredPrices
                } else {
                    generateDefaultMarketPrices()
                }

                gameDao.restoreFullPlayerStateTransaction(
                    player = playerEntity,
                    state = gameStateEntity,
                    businesses = restoredBusinesses,
                    inventory = finalInventory,
                    prices = finalPrices
                )

                return@withContext snapshot
            } else {
                // 4. Sadece ve sadece bulutta hiçbir kayıt yoksa ve yerel cihaz da tamamen boşsa 100.000 TL başlangıç bakiyesi ile yeni profil oluştur.
                val defaultInventory = Product.values().map { product -> InventoryEntity(product.id, 0) }
                val defaultPrices = generateDefaultMarketPrices()
                val initialPlayer = PlayerEntity(
                    id = resolvedPlayerId,
                    name = "Tüccar",
                    money = 100_000L
                )

                gameDao.restoreFullPlayerStateTransaction(
                    player = initialPlayer,
                    state = GameStateEntity(),
                    businesses = emptyList(),
                    inventory = defaultInventory,
                    prices = defaultPrices
                )
                economicDataStore?.saveEconomicData(
                    player = initialPlayer,
                    gameState = GameStateEntity(),
                    businesses = emptyList(),
                    inventory = defaultInventory,
                    marketPrices = defaultPrices,
                    marketListings = emptyList(),
                    isAutoSell = false,
                    isAutoBuy = false,
                    isAutoProduce = false
                )
                return@withContext null
            }
        }
    }

    suspend fun saveEconomicStateToDataStore(
        player: PlayerEntity,
        gameState: GameStateEntity,
        businesses: List<BusinessEntity>,
        inventory: List<InventoryEntity>,
        marketPrices: List<MarketPriceEntity>,
        marketListings: List<MarketListing>,
        isAutoSell: Boolean,
        isAutoBuy: Boolean,
        isAutoProduce: Boolean,
        isProdManagerHired: Boolean = false,
        isEngineerHired: Boolean = false,
        isEngineerActive: Boolean = false,
        isSalesExecHired: Boolean = false,
        isSalesExecActive: Boolean = false,
        techGreenEnergy: Int = 0,
        techQualityControl: Int = 0,
        techLogistics: Int = 0,
        techAutomation: Int = 0,
        techQuantumAi: Int = 0,
        techNanotech: Int = 0,
        techCyberSecurity: Int = 0,
        techBiotechCloning: Int = 0,
        techAerospace: Int = 0,
        techHeavyIndustry: Int = 0,
        techConsumerGoods: Int = 0,
        techPetrochem: Int = 0,
        activeResearchTechKey: String = "",
        researchEndTimeMs: Long = 0L,
        activeResearches: Map<String, Long> = emptyMap(),
        researchLevels: Map<String, Int> = emptyMap(),
        isIpoActive: Boolean = false,
        publicSharePercent: Int = 0,
        totalDividendsPaid: Long = 0L,
        playerGuildShares: Map<String, Int> = emptyMap(),
        playerGuildBuyPrices: Map<String, Double> = emptyMap(),
        managers: List<CompanyManager> = emptyList(),
        auctions: List<Auction> = emptyList(),
        megaProjects: List<MegaProject> = emptyList(),
        deliveries: List<DeliveryItem> = emptyList(),
        activeProductions: List<ActiveProduction> = emptyList(),
        growthHistoryJson: String = "[]",
        dailyQuestStateJson: String = "{}"
    ) {
        economicDataStore?.saveEconomicData(
            player = player,
            gameState = gameState,
            businesses = businesses,
            inventory = inventory,
            marketPrices = marketPrices,
            marketListings = marketListings,
            isAutoSell = isAutoSell,
            isAutoBuy = isAutoBuy,
            isAutoProduce = isAutoProduce,
            isProdManagerHired = isProdManagerHired,
            isEngineerHired = isEngineerHired,
            isEngineerActive = isEngineerActive,
            isSalesExecHired = isSalesExecHired,
            isSalesExecActive = isSalesExecActive,
            techGreenEnergy = techGreenEnergy,
            techQualityControl = techQualityControl,
            techLogistics = techLogistics,
            techAutomation = techAutomation,
            techQuantumAi = techQuantumAi,
            techNanotech = techNanotech,
            techCyberSecurity = techCyberSecurity,
            techBiotechCloning = techBiotechCloning,
            techAerospace = techAerospace,
            techHeavyIndustry = techHeavyIndustry,
            techConsumerGoods = techConsumerGoods,
            techPetrochem = techPetrochem,
            activeResearchTechKey = activeResearchTechKey,
            researchEndTimeMs = researchEndTimeMs,
            activeResearches = activeResearches,
            researchLevels = researchLevels,
            isIpoActive = isIpoActive,
            publicSharePercent = publicSharePercent,
            totalDividendsPaid = totalDividendsPaid,
            playerGuildShares = playerGuildShares,
            playerGuildBuyPrices = playerGuildBuyPrices,
            managers = managers,
            auctions = auctions,
            megaProjects = megaProjects,
            deliveries = deliveries,
            activeProductions = activeProductions,
            growthHistoryJson = growthHistoryJson,
            dailyQuestStateJson = dailyQuestStateJson
        )
    }

    suspend fun insertInventory(inventory: InventoryEntity) {
        gameDao.insertInventory(inventory)
    }

    suspend fun updateMarketPrices(prices: List<MarketPriceEntity>, syncToRemote: Boolean = true) {
        val sanitized = sanitizeMarketPrices(prices)
        gameDao.insertMarketPrices(sanitized)
        if (syncToRemote) {
            syncGlobalBorsaPricesToSupabase(sanitized)
        }
    }

    suspend fun updateGameState(state: GameStateEntity) {
        gameDao.insertGameState(state)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        gameDao.insertPlayer(player)
    }

    suspend fun processOfferAcceptanceOnServer(
        request: com.example.data.network.AcceptOfferRequestDto
    ): com.example.data.network.AcceptOfferResponseDto {
        // Backend server payload processor:
        // Calculates critical consortium multipliers (e.g., +25% return) on server side
        val consortiumBonusMultiplier = if (!request.consortiumId.isNullOrEmpty()) 1.25f else 1.0f
        val calculatedFinalAmount = (request.rawBaseAmount * consortiumBonusMultiplier).toLong()

        return com.example.data.network.AcceptOfferResponseDto(
            success = true,
            finalAmount = calculatedFinalAmount,
            appliedConsortiumBonus = consortiumBonusMultiplier - 1.0f
        )
    }

    suspend fun sellItem(itemId: String, quantity: Int, pricePerUnit: Long) {
        val totalRevenue = quantity * pricePerUnit
        gameDao.sellItemTransaction(itemId, quantity, totalRevenue, quantity * 2)
    }

    suspend fun produceItem(itemId: String, quantity: Int) {
        gameDao.addOrUpdateInventoryTransaction(itemId, quantity, quantity)
    }
    
    suspend fun consumeItem(itemId: String, quantity: Int) {
        gameDao.updateInventoryQuantity(itemId, -quantity)
    }

    suspend fun buildBusiness(type: String, cityId: String, cost: Long) {
        gameDao.buyBusinessTransaction(cost, BusinessEntity(type = type, level = 1, cityId = cityId), 50)
    }
    
    suspend fun updateBusiness(business: BusinessEntity) {
        gameDao.insertBusiness(business)
    }

    suspend fun getAllBusinessesDirect(): List<BusinessEntity> {
        return gameDao.getBusinessesDirect()
    }

    suspend fun clearAndRestoreBusinesses(newList: List<BusinessEntity>) {
        gameDao.clearAndReplaceBusinessesTransaction(newList)
    }

    suspend fun deleteBusiness(id: Int) {
        gameDao.deleteBusinessById(id)
    }
    
    suspend fun getAllMuseumAuctions(): List<MuseumAuctionEntity> {
        return gameDao.getAllMuseumAuctions()
    }

    suspend fun getPendingSales(): List<PendingMarketSaleEntity> {
        return gameDao.getPendingSales()
    }

    suspend fun insertPendingSale(sale: PendingMarketSaleEntity) {
        gameDao.insertPendingSale(sale)
    }

    suspend fun deletePendingSale(id: String) {
        gameDao.deletePendingSale(id)
    }

    suspend fun processTrade(logisticsCost: Long, revenue: Long) {
        gameDao.processTradeTransaction(logisticsCost, revenue, 25)
    }

    suspend fun saveSelectedTheme(themeId: String) {
        economicDataStore?.saveSelectedTheme(themeId)
    }

    suspend fun saveSelectedLanguage(languageCode: String) {
        economicDataStore?.saveSelectedLanguage(languageCode)
    }

    suspend fun getLastClaimedMonthlyRewardKey(): String {
        return economicDataStore?.getLastClaimedMonthlyRewardKey() ?: ""
    }

    suspend fun setLastClaimedMonthlyRewardKey(key: String) {
        economicDataStore?.setLastClaimedMonthlyRewardKey(key)
    }

    suspend fun saveUiMode(isExpert: Boolean) {
        economicDataStore?.saveUiMode(isExpert)
    }

    suspend fun getUiMode(): Boolean {
        return economicDataStore?.getUiMode() ?: false
    }

    suspend fun saveBorsaLimitOrders(orders: List<BorsaLimitOrder>) {
        economicDataStore?.saveBorsaLimitOrders(orders)
    }

    suspend fun getBorsaLimitOrders(): List<BorsaLimitOrder> {
        return economicDataStore?.getBorsaLimitOrders() ?: emptyList()
    }

    suspend fun exportSaveJson(): String {
        return economicDataStore?.exportSaveJson() ?: "{}"
    }

    suspend fun importSaveJson(jsonString: String): Boolean {
        val success = economicDataStore?.importSaveJson(jsonString) ?: false
        if (success) {
            val snapshot = economicDataStore?.getEconomicSnapshot()
            val onlineEmail = snapshot?.onlineEmail.orEmpty().trim()
            val isGoogleAuthed = snapshot?.isOnlineRegistered == true && onlineEmail.isNotBlank() && onlineEmail != "misafir_tuccar"
            val resolvedPlayerId = if (isGoogleAuthed) onlineEmail.replace(".", "_") else "local_player"

            if (snapshot != null) {
                val playerEntity = PlayerEntity(
                    id = resolvedPlayerId,
                    name = snapshot.name,
                    money = snapshot.money,
                    loanAmount = snapshot.loanAmount,
                    depositBalance = snapshot.depositBalance,
                    dailyIncome = snapshot.dailyIncome,
                    dailyExpense = snapshot.dailyExpense,
                    totalProfit = snapshot.totalProfit,
                    xp = snapshot.xp,
                    level = snapshot.level,
                    inventoryCapacity = snapshot.inventoryCapacity,
                    currentCity = snapshot.currentCity,
                    isUsdAccount = snapshot.isUsdAccount,
                    isVip = snapshot.isVip,
                    gems = snapshot.gems,
                    lastDailyRewardMs = snapshot.lastDailyRewardMs,
                    loginStreak = snapshot.loginStreak,
                    lockedDepositBalance = snapshot.lockedDepositBalance,
                    lockedDepositStartTimeMs = snapshot.lockedDepositStartTimeMs,
                    lockedDepositDurationMs = snapshot.lockedDepositDurationMs,
                    dollarBalance = snapshot.dollarBalance,
                    dollarDepositBalance = snapshot.dollarDepositBalance,
                    dollarLoanAmount = snapshot.dollarLoanAmount
                )

                val gameStateEntity = GameStateEntity(
                    id = "global_state",
                    season = snapshot.season,
                    activeEvent = snapshot.activeEvent,
                    globalInflationRate = snapshot.globalInflationRate,
                    centralBankLoanRate = snapshot.centralBankLoanRate,
                    centralBankDepositRate = snapshot.centralBankDepositRate,
                    totalMarketLiquidity = snapshot.totalMarketLiquidity
                )

                val restoredBusinesses = economicDataStore.deserializeBusinesses(snapshot.businessesJson)
                val restoredInventory = economicDataStore.deserializeInventory(snapshot.inventoryJson)
                val finalInventory = if (restoredInventory.isNotEmpty()) {
                    restoredInventory
                } else {
                    Product.values().map { product -> InventoryEntity(product.id, 0) }
                }

                val restoredPrices = economicDataStore.deserializeMarketPrices(snapshot.marketPricesJson)
                val finalPrices = if (restoredPrices.isNotEmpty()) {
                    restoredPrices
                } else {
                    generateDefaultMarketPrices()
                }

                gameDao.restoreFullPlayerStateTransaction(
                    player = playerEntity,
                    state = gameStateEntity,
                    businesses = restoredBusinesses,
                    inventory = finalInventory,
                    prices = finalPrices
                )
            }
        }
        return success
    }


    private fun generateDefaultMarketPrices(): List<MarketPriceEntity> {
        val list = mutableListOf<MarketPriceEntity>()
        for (product in Product.values()) {
            list.add(MarketPriceEntity(
                itemId = product.id,
                price = product.basePrice,
                originCountry = "Türkiye",
                originCityId = "istanbul",
                isUsd = false,
                borsaStock = MacroEconomyEngine.DEFAULT_BORSA_STOCK
            ))
        }
        return list
    }
}
