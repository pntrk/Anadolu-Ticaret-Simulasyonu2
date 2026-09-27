package com.example.viewmodel

import com.example.data.*

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf


data class PlayerUiState(
    val player: PlayerEntity? = null,
    val totalDividendsPaid: Long = 0L,
    val publicSharePercent: Int = 0,
    val isIpoActive: Boolean = false
)

data class InventoryUiState(
    val items: ImmutableList<InventoryEntity> = persistentListOf(),
    val reservedItems: ImmutableMap<String, Int> = persistentMapOf(),
    val activeDeliveries: ImmutableList<DeliveryItem> = persistentListOf(),
    val activeProductions: ImmutableList<ActiveProduction> = persistentListOf()
)

data class MarketUiState(
    val prices: ImmutableList<MarketPriceEntity> = persistentListOf(),
    val priceHistory: ImmutableMap<String, ImmutableList<Long>> = persistentMapOf(),
    val nextBorsaTickSeconds: Int = 30,
    val activeCityEvents: ImmutableList<CityMarketEvent> = persistentListOf(),
    val marketListings: ImmutableList<MarketListing> = persistentListOf(),
    val buyOrders: ImmutableList<BuyOrder> = persistentListOf(),
    val futuresContracts: ImmutableList<FuturesContract> = persistentListOf(),
    val marketTrends: ImmutableMap<String, Float> = persistentMapOf(),
    val auctions: ImmutableList<com.example.data.Auction> = persistentListOf(),
    val borsaLimitOrders: ImmutableList<com.example.data.BorsaLimitOrder> = persistentListOf()
)

data class ConsortiumUiState(
    val megaProjects: ImmutableList<MegaProject> = persistentListOf(),
    val chatMessages: ImmutableMap<String, ImmutableList<ConsortiumChatMessage>> = persistentMapOf(),
    val unreadChatProjects: ImmutableSet<String> = persistentSetOf(),
    val crisisState: ConsortiumCrisisState = ConsortiumCrisisState()
)

data class SettingsUiState(
    val isOnlineRegistered: Boolean = false,
    val isGoogleSignedIn: Boolean = false,
    val isExpertMode: Boolean = false
)

data class GuildsUiState(
    val guilds: ImmutableList<GuildGroup> = persistentListOf(),
    val playerGuildShares: ImmutableMap<String, Int> = persistentMapOf(),
    val playerGuildBuyPrices: ImmutableMap<String, Double> = persistentMapOf()
)


data class HrUiState(
    val activeResearches: ImmutableMap<String, Long> = persistentMapOf(),
    val researchLevels: ImmutableMap<String, Int> = persistentMapOf()
)

data class GameUiState(
    val isLoading: Boolean = true,
    val tradeTutorialStep: Int = 1,
    val dailyRewardData: com.example.viewmodel.DailyRewardData? = null,
    val offlineEarningsData: com.example.viewmodel.OfflineEarningsData? = null,
    val economicSnapshot: com.example.data.EconomicSnapshot? = null,
    val hasCompletedFirstTrade: Boolean = false,
    val playerState: PlayerUiState = PlayerUiState(),
    val inventoryState: InventoryUiState = InventoryUiState(),
    val marketState: MarketUiState = MarketUiState(),
    val consortiumState: ConsortiumUiState = ConsortiumUiState(),
    val settingsState: SettingsUiState = SettingsUiState(),
    val guildsState: GuildsUiState = GuildsUiState(),
    val hrState: HrUiState = HrUiState(),
    val gameStateObj: GameStateEntity = GameStateEntity(),
    val managers: ImmutableList<CompanyManager> = persistentListOf(),
    val businesses: ImmutableList<BusinessEntity> = persistentListOf(),
    val newsTickerMessage: String = "",
    val realTimeClockText: String = "",
    val realTimeShiftName: String = "",
    val realTimeShiftBonusText: String = "",
    val macroState: MacroEconomyState = MacroEconomyState(),
    val productionProgress: ImmutableMap<String, Float> = persistentMapOf(),
    val productionDurations: ImmutableMap<String, Long> = persistentMapOf(),
    val isEntrepreneurGuideCompleted: Boolean = false,
    
    // Derived overall metrics
    val currentInvCount: Int = 0,
    val inventoryFillPercentage: Float = 0f,
    val topInventory: ImmutableList<InventoryEntity> = persistentListOf(),
    val facilityValuation: Long = 0L,
    val inventoryValuation: Long = 0L,
    val consortiumValuation: Long = 0L,
    val consortiumDeliveredValuation: Long = 0L,
    val guildSharesValuation: Long = 0L,
    val netWorth: Long = 0L,
    val growthHistory: ImmutableList<com.example.data.GrowthPointDto> = persistentListOf()
)
