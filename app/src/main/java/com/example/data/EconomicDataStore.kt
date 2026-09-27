package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.example.data.network.AppJson
import com.example.data.network.AuctionDto
import com.example.data.network.BusinessDto
import com.example.data.network.CompanyManagerDto
import com.example.data.network.DeliveryDto
import com.example.data.network.InventoryDto
import com.example.data.network.ManagerActionLogDto
import com.example.data.network.anyToJsonElement
import com.example.data.network.jsonElementToAny
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

val Context.economicDataStore: DataStore<Preferences> by preferencesDataStore(name = "economic_data_store")

data class EconomicSnapshot(
    val name: String = "Tüccar",
    val money: Long = 100_000L,
    val loanAmount: Long = 0L,
    val depositBalance: Long = 0L,
    val lockedDepositBalance: Long = 0L,
    val lockedDepositStartTimeMs: Long = 0L,
    val lockedDepositDurationMs: Long = 0L,
    val dailyIncome: Long = 0L,
    val dailyExpense: Long = 0L,
    val totalProfit: Long = 0L,
    val xp: Int = 0,
    val level: Int = 1,
    val inventoryCapacity: Int = 5000,
    val currentCity: String = "istanbul",
    val isUsdAccount: Boolean = false,
    val isVip: Boolean = false,
    val gems: Int = 0,
    val lastDailyRewardMs: Long = 0L,
    val loginStreak: Int = 0,
    val dollarBalance: Long = 0L,
    val dollarDepositBalance: Long = 0L,
    val dollarLoanAmount: Long = 0L,
    val usdTryRate: Double = 50.0,
    
    val onlineEmail: String = "",
    val onlinePassword: String = "",
    val isOnlineRegistered: Boolean = false,

    val season: String = "İlkbahar",
    val activeEvent: String = "Normal",
    val globalInflationRate: Float = 0.0f,
    val centralBankLoanRate: Float = 0.15f,
    val centralBankDepositRate: Float = 0.05f,
    val totalMarketLiquidity: Long = 10000000L,
    
    val isAutoSellActive: Boolean = false,
    val isAutoBuyActive: Boolean = false,
    val isAutoProduceActive: Boolean = false,
    val isProdManagerHired: Boolean = false,
    
    val isEngineerHired: Boolean = false,
    val isEngineerActive: Boolean = false,
    val isSalesExecHired: Boolean = false,
    val isSalesExecActive: Boolean = false,

    val techGreenEnergy: Int = 0,
    val techQualityControl: Int = 0,
    val techLogistics: Int = 0,
    val techAutomation: Int = 0,
    val techQuantumAi: Int = 0,
    val techNanotech: Int = 0,
    val techCyberSecurity: Int = 0,
    val techBiotechCloning: Int = 0,
    val techAerospace: Int = 0,
    val techHeavyIndustry: Int = 0,
    val techConsumerGoods: Int = 0,
    val techPetrochem: Int = 0,
    val activeResearchTechKey: String = "",
    val researchEndTimeMs: Long = 0L,
    val activeResearchesJson: String = "{}",
    val researchLevelsJson: String = "{}",

    val isIpoActive: Boolean = false,
    val publicSharePercent: Int = 0,
    val totalDividendsPaid: Long = 0L,
    val dividendDebt: Long = 0L,
    
    val businessesJson: String = "[]",
    val inventoryJson: String = "[]",
    val activeDeliveriesJson: String = "[]",
    val activeProductionsJson: String = "[]",
    val marketPricesJson: String = "[]",
    val marketListingsJson: String = "[]",
    val playerGuildSharesJson: String = "{}",
    val playerGuildBuyPricesJson: String = "{}",
    val managersJson: String = "[]",
    val auctionsJson: String = "[]",
    val museumHeritageJson: String = "{}",
    
    val lastSavedTime: Long = 0L,
    val lastKnownElapsedRealtime: Long = 0L,
    val isDataSaved: Boolean = false,
    val hasSetWarehouse: Boolean = false,
    val hasCompletedFirstTrade: Boolean = false,
    val dailyQuestStateJson: String = "{}",
    val megaProjectsJson: String = "[]",
    val growthHistoryJson: String = "[]",
    val selectedTheme: String = "cyber_blue",
    val selectedLanguage: String = "tr"
)

class EconomicDataStore(val context: Context) {

    companion object {
        val KEY_NAME = stringPreferencesKey("player_name")
        val KEY_MONEY = longPreferencesKey("money")
        val KEY_USD_BALANCE = longPreferencesKey("usd_balance")
        val KEY_IS_USD_ACCOUNT = booleanPreferencesKey("is_usd_account")
        val KEY_LOAN_AMOUNT = longPreferencesKey("loan_amount")
        val KEY_DEPOSIT_BALANCE = longPreferencesKey("deposit_balance")
        val KEY_LOCKED_DEPOSIT_BALANCE = longPreferencesKey("locked_deposit_balance")
        val KEY_LOCKED_DEPOSIT_START_TIME_MS = longPreferencesKey("locked_deposit_start_time_ms")
        val KEY_LOCKED_DEPOSIT_DURATION_MS = longPreferencesKey("locked_deposit_duration_ms")
        val KEY_DAILY_INCOME = longPreferencesKey("daily_income")
        val KEY_DAILY_EXPENSE = longPreferencesKey("daily_expense")
        val KEY_TOTAL_PROFIT = longPreferencesKey("total_profit")
        val KEY_XP = intPreferencesKey("xp")
        val KEY_LEVEL = intPreferencesKey("level")
        val KEY_INVENTORY_CAPACITY = intPreferencesKey("inventory_capacity")
        val KEY_CURRENT_CITY = stringPreferencesKey("current_city")
        val KEY_IS_VIP = booleanPreferencesKey("is_vip")
        val KEY_GEMS = intPreferencesKey("gems")
        val KEY_LAST_DAILY_REWARD_MS = longPreferencesKey("last_daily_reward_ms")
        val KEY_LOGIN_STREAK = intPreferencesKey("login_streak")
        val KEY_DOLLAR_BALANCE = longPreferencesKey("dollar_balance")
        val KEY_DOLLAR_DEPOSIT_BALANCE = longPreferencesKey("dollar_deposit_balance")
        val KEY_DOLLAR_LOAN_AMOUNT = longPreferencesKey("dollar_loan_amount")
        val KEY_USD_TRY_RATE = doublePreferencesKey("usd_try_rate")

        val KEY_ONLINE_EMAIL = stringPreferencesKey("online_email")
        val KEY_ONLINE_PASSWORD = stringPreferencesKey("online_password")
        val KEY_IS_ONLINE_REGISTERED = booleanPreferencesKey("is_online_registered")

        val KEY_SEASON = stringPreferencesKey("season")
        val KEY_ACTIVE_EVENT = stringPreferencesKey("active_event")
        val KEY_GLOBAL_INFLATION = floatPreferencesKey("global_inflation")
        val KEY_CENTRAL_BANK_LOAN_RATE = floatPreferencesKey("central_bank_loan_rate")
        val KEY_CENTRAL_BANK_DEPOSIT_RATE = floatPreferencesKey("central_bank_deposit_rate")
        val KEY_TOTAL_MARKET_LIQUIDITY = longPreferencesKey("total_market_liquidity")

        val KEY_AUTO_SELL = booleanPreferencesKey("is_auto_sell_active")
        val KEY_AUTO_BUY = booleanPreferencesKey("is_auto_buy_active")
        val KEY_AUTO_PRODUCE = booleanPreferencesKey("is_auto_produce_active")
        val KEY_PROD_MANAGER_HIRED = booleanPreferencesKey("is_prod_manager_hired")

        val KEY_ENGINEER_HIRED = booleanPreferencesKey("is_engineer_hired")
        val KEY_ENGINEER_ACTIVE = booleanPreferencesKey("is_engineer_active")
        val KEY_SALES_EXEC_HIRED = booleanPreferencesKey("is_sales_exec_hired")
        val KEY_SALES_EXEC_ACTIVE = booleanPreferencesKey("is_sales_exec_active")

        val KEY_TECH_GREEN_ENERGY = intPreferencesKey("tech_green_energy")
        val KEY_TECH_QUALITY_CONTROL = intPreferencesKey("tech_quality_control")
        val KEY_TECH_LOGISTICS = intPreferencesKey("tech_logistics")
        val KEY_TECH_AUTOMATION = intPreferencesKey("tech_automation")
        val KEY_TECH_QUANTUM_AI = intPreferencesKey("tech_quantum_ai")
        val KEY_TECH_NANOTECH = intPreferencesKey("tech_nanotech")
        val KEY_TECH_CYBER_SECURITY = intPreferencesKey("tech_cyber_security")
        val KEY_TECH_BIOTECH_CLONING = intPreferencesKey("tech_biotech_cloning")
        val KEY_TECH_AEROSPACE = intPreferencesKey("tech_aerospace")
        val KEY_TECH_HEAVY_INDUSTRY = intPreferencesKey("tech_heavy_industry")
        val KEY_TECH_CONSUMER_GOODS = intPreferencesKey("tech_consumer_goods")
        val KEY_TECH_PETROCHEM = intPreferencesKey("tech_petrochem")
        val KEY_ACTIVE_RESEARCH_TECH_KEY = stringPreferencesKey("active_research_tech_key")
        val KEY_RESEARCH_END_TIME_MS = longPreferencesKey("research_end_time_ms")
        val KEY_ACTIVE_RESEARCHES_JSON = stringPreferencesKey("active_researches_json")
        val KEY_RESEARCH_LEVELS_JSON = stringPreferencesKey("research_levels_json")

        val KEY_IS_IPO_ACTIVE = booleanPreferencesKey("is_ipo_active")
        val KEY_PUBLIC_SHARE_PERCENT = intPreferencesKey("public_share_percent")
        val KEY_TOTAL_DIVIDENDS_PAID = longPreferencesKey("total_dividends_paid")
        val KEY_DIVIDEND_DEBT = longPreferencesKey("dividend_debt")

        val KEY_BUSINESSES_JSON = stringPreferencesKey("businesses_json")
        val KEY_INVENTORY_JSON = stringPreferencesKey("inventory_json")
        val KEY_ACTIVE_DELIVERIES_JSON = stringPreferencesKey("active_deliveries_json")
        val KEY_ACTIVE_PRODUCTIONS_JSON = stringPreferencesKey("active_productions_json")
        val KEY_MARKET_PRICES_JSON = stringPreferencesKey("market_prices_json")
        val KEY_MARKET_LISTINGS_JSON = stringPreferencesKey("market_listings_json")
        val KEY_PLAYER_GUILD_SHARES_JSON = stringPreferencesKey("player_guild_shares_json")
        val KEY_PLAYER_GUILD_BUY_PRICES_JSON = stringPreferencesKey("player_guild_buy_prices_json")
        val KEY_MANAGERS_JSON = stringPreferencesKey("managers_json")
        val KEY_AUCTIONS_JSON = stringPreferencesKey("auctions_json")
        val KEY_MUSEUM_HERITAGE_JSON = stringPreferencesKey("museum_heritage_json")
        val KEY_BORSA_LIMIT_ORDERS_JSON = stringPreferencesKey("borsa_limit_orders_json")

        val KEY_LAST_SAVED_TIME = longPreferencesKey("last_saved_time")
    val KEY_LAST_KNOWN_ELAPSED_REALTIME = longPreferencesKey("last_known_elapsed_realtime")
        val KEY_IS_DATA_SAVED = booleanPreferencesKey("is_data_saved")
        val KEY_HAS_SET_WAREHOUSE = booleanPreferencesKey("has_set_warehouse")
        val KEY_HAS_COMPLETED_FIRST_TRADE = booleanPreferencesKey("has_completed_first_trade")
        val KEY_DAILY_QUEST_STATE_JSON = stringPreferencesKey("daily_quest_state_json")
        val KEY_MEGA_PROJECTS_JSON = stringPreferencesKey("mega_projects_json")
        val KEY_GROWTH_HISTORY_JSON = stringPreferencesKey("growth_history_json")
        val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme")
        val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val KEY_LAST_CLAIMED_MONTHLY_REWARD_KEY = stringPreferencesKey("last_claimed_monthly_reward_key")
        val KEY_IS_EXPERT_MODE = booleanPreferencesKey("is_expert_mode")
        val KEY_IS_NEW_PLAYER_GUIDE_COMPLETED = booleanPreferencesKey("is_new_player_guide_completed")
        val KEY_IS_ENTREPRENEUR_GUIDE_COMPLETED = booleanPreferencesKey("is_entrepreneur_guide_completed")
        val KEY_ONBOARDING_CLAIMED_STEPS = stringPreferencesKey("onboarding_claimed_steps")
        val KEY_SINGLE_CURRENCY_MIGRATED = booleanPreferencesKey("is_single_currency_migrated")
        val KEY_LOCAL_INTEGRITY_SIGNATURE = stringPreferencesKey("local_integrity_signature")
    }

    private fun androidx.datastore.preferences.core.Preferences.getLongSafe(keyName: String, default: Long): Long {
        val entry = this.asMap().entries.find { it.key.name == keyName }
        val raw = entry?.value
        return when (raw) {
            is Long -> raw
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.getIntSafe(keyName: String, default: Int): Int {
        val entry = this.asMap().entries.find { it.key.name == keyName }
        val raw = entry?.value
        return when (raw) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> default
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.getDoubleSafe(keyName: String, default: Double): Double {
        val entry = this.asMap().entries.find { it.key.name == keyName }
        val raw = entry?.value
        return when (raw) {
            is Double -> raw
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.getFloatSafe(keyName: String, default: Float): Float {
        val entry = this.asMap().entries.find { it.key.name == keyName }
        val raw = entry?.value
        return when (raw) {
            is Float -> raw
            is Number -> raw.toFloat()
            is String -> raw.toFloatOrNull() ?: default
            else -> default
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.getBooleanSafe(keyName: String, default: Boolean): Boolean {
        val entry = this.asMap().entries.find { it.key.name == keyName }
        val raw = entry?.value
        return when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.toBooleanStrictOrNull() ?: (raw == "1" || raw.equals("true", ignoreCase = true))
            else -> default
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.getStringSafe(keyName: String, default: String): String {
        val entry = this.asMap().entries.find { it.key.name == keyName }
        val raw = entry?.value
        return when (raw) {
            is String -> raw
            null -> default
            else -> raw.toString()
        }
    }

    val economicSnapshotFlow: Flow<EconomicSnapshot> = context.economicDataStore.data.map { prefs ->
        val rawMoney = prefs.getLongSafe("money", 100_000L).coerceAtLeast(0L).coerceAtMost(Long.MAX_VALUE - 1_000_000_000L)
        val rawLoan = prefs.getLongSafe("loan_amount", 0L).coerceAtLeast(0L)
        val rawDeposit = prefs.getLongSafe("deposit_balance", 0L).coerceAtLeast(0L)
        val rawTotalProfit = prefs.getLongSafe("total_profit", 0L)
        val rawGems = prefs.getIntSafe("gems", 0).coerceIn(0, 1_000_000)
        val rawLevel = prefs.getIntSafe("level", 1).coerceIn(1, 100)
        val rawXp = prefs.getIntSafe("xp", 0).coerceAtLeast(0)

        EconomicSnapshot(
            name = prefs.getStringSafe("player_name", "Tüccar"),
            money = rawMoney,
            loanAmount = rawLoan,
            depositBalance = rawDeposit,
            lockedDepositBalance = prefs.getLongSafe("locked_deposit_balance", 0L).coerceAtLeast(0L),
            lockedDepositStartTimeMs = prefs.getLongSafe("locked_deposit_start_time_ms", 0L),
            lockedDepositDurationMs = prefs.getLongSafe("locked_deposit_duration_ms", 0L),
            dailyIncome = prefs.getLongSafe("daily_income", 0L),
            dailyExpense = prefs.getLongSafe("daily_expense", 0L),
            totalProfit = rawTotalProfit,
            xp = rawXp,
            level = rawLevel,
            inventoryCapacity = prefs.getIntSafe("inventory_capacity", 5000).coerceIn(100, 1_000_000_000),
            currentCity = migrateLegacyCity(prefs.getStringSafe("current_city", "istanbul")),
            isUsdAccount = false,
            isVip = prefs.getBooleanSafe("is_vip", false),
            gems = rawGems,
            lastDailyRewardMs = prefs.getLongSafe("last_daily_reward_ms", 0L),
            loginStreak = prefs.getIntSafe("login_streak", 0),
            dollarBalance = rawMoney,
            dollarDepositBalance = rawDeposit,
            dollarLoanAmount = rawLoan,
            usdTryRate = 1.0,
            
            onlineEmail = prefs.getStringSafe("online_email", ""),
            onlinePassword = prefs.getStringSafe("online_password", ""),
            isOnlineRegistered = prefs.getBooleanSafe("is_online_registered", false),

            season = prefs.getStringSafe("season", "İlkbahar"),
            activeEvent = prefs.getStringSafe("active_event", "Normal"),
            globalInflationRate = prefs.getFloatSafe("global_inflation", 0.0f),
            centralBankLoanRate = prefs.getFloatSafe("central_bank_loan_rate", 0.15f),
            centralBankDepositRate = prefs.getFloatSafe("central_bank_deposit_rate", 0.05f),
            totalMarketLiquidity = prefs.getLongSafe("total_market_liquidity", 10000000L),
            
            isAutoSellActive = prefs.getBooleanSafe("is_auto_sell_active", false),
            isAutoBuyActive = prefs.getBooleanSafe("is_auto_buy_active", false),
            isAutoProduceActive = prefs.getBooleanSafe("is_auto_produce_active", false),
            isProdManagerHired = prefs.getBooleanSafe("is_prod_manager_hired", false),
            
            isEngineerHired = prefs.getBooleanSafe("is_engineer_hired", false),
            isEngineerActive = prefs.getBooleanSafe("is_engineer_active", false),
            isSalesExecHired = prefs.getBooleanSafe("is_sales_exec_hired", false),
            isSalesExecActive = prefs.getBooleanSafe("is_sales_exec_active", false),

            techGreenEnergy = prefs.getIntSafe("tech_green_energy", 0),
            techQualityControl = prefs.getIntSafe("tech_quality_control", 0),
            techLogistics = prefs.getIntSafe("tech_logistics", 0),
            techAutomation = prefs.getIntSafe("tech_automation", 0),
            techQuantumAi = prefs.getIntSafe("tech_quantum_ai", 0),
            techNanotech = prefs.getIntSafe("tech_nanotech", 0),
            techCyberSecurity = prefs.getIntSafe("tech_cyber_security", 0),
            techBiotechCloning = prefs.getIntSafe("tech_biotech_cloning", 0),
            techAerospace = prefs.getIntSafe("tech_aerospace", 0),
            techHeavyIndustry = prefs.getIntSafe("tech_heavy_industry", 0),
            techConsumerGoods = prefs.getIntSafe("tech_consumer_goods", 0),
            techPetrochem = prefs.getIntSafe("tech_petrochem", 0),
            activeResearchTechKey = prefs.getStringSafe("active_research_tech_key", ""),
            researchEndTimeMs = prefs.getLongSafe("research_end_time_ms", 0L),
            activeResearchesJson = prefs.getStringSafe("active_researches_json", "{}"),
            researchLevelsJson = prefs.getStringSafe("research_levels_json", "{}"),

            isIpoActive = prefs.getBooleanSafe("is_ipo_active", false),
            publicSharePercent = prefs.getIntSafe("public_share_percent", 0),
            totalDividendsPaid = prefs.getLongSafe("total_dividends_paid", 0L),
            dividendDebt = prefs.getLongSafe("dividend_debt", 0L),
            
            businessesJson = prefs.getStringSafe("businesses_json", "[]"),
            inventoryJson = prefs.getStringSafe("inventory_json", "[]"),
            activeDeliveriesJson = prefs.getStringSafe("active_deliveries_json", "[]"),
            activeProductionsJson = prefs.getStringSafe("active_productions_json", "[]"),
            marketPricesJson = prefs.getStringSafe("market_prices_json", "[]"),
            marketListingsJson = prefs.getStringSafe("market_listings_json", "[]"),
            playerGuildSharesJson = prefs.getStringSafe("player_guild_shares_json", "{}"),
            playerGuildBuyPricesJson = prefs.getStringSafe("player_guild_buy_prices_json", "{}"),
            managersJson = prefs.getStringSafe("managers_json", "[]"),
            auctionsJson = prefs.getStringSafe("auctions_json", "[]"),
            dailyQuestStateJson = prefs.getStringSafe("daily_quest_state_json", "{}"),
            megaProjectsJson = prefs.getStringSafe("mega_projects_json", "[]"),
            museumHeritageJson = prefs.getStringSafe("museum_heritage_json", "{}"),
            
            lastSavedTime = prefs.getLongSafe("last_saved_time", 0L),
            lastKnownElapsedRealtime = prefs.getLongSafe("last_known_elapsed_realtime", 0L),
            isDataSaved = prefs.getBooleanSafe("is_data_saved", false),
            hasSetWarehouse = prefs.getBooleanSafe("has_set_warehouse", false),
            hasCompletedFirstTrade = prefs.getBooleanSafe("has_completed_first_trade", false),
            growthHistoryJson = prefs.getStringSafe("growth_history_json", "[]"),
            selectedTheme = prefs.getStringSafe("selected_theme", "cyber_blue"),
            selectedLanguage = prefs.getStringSafe("selected_language", "tr")
        )
    }

    suspend fun getEconomicSnapshot(): EconomicSnapshot {
        return economicSnapshotFlow.first()
    }

    suspend fun saveEconomicData(
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
        dividendDebt: Long = 0L,
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
        context.economicDataStore.edit { prefs ->
            // Player & Finances
            prefs[KEY_NAME] = player.name
            prefs[KEY_MONEY] = player.money
            prefs[KEY_LOAN_AMOUNT] = player.loanAmount
            prefs[KEY_DEPOSIT_BALANCE] = player.depositBalance
            prefs[KEY_LOCKED_DEPOSIT_BALANCE] = player.lockedDepositBalance
            prefs[KEY_LOCKED_DEPOSIT_START_TIME_MS] = player.lockedDepositStartTimeMs
            prefs[KEY_LOCKED_DEPOSIT_DURATION_MS] = player.lockedDepositDurationMs
            prefs[KEY_DAILY_INCOME] = player.dailyIncome
            prefs[KEY_DAILY_EXPENSE] = player.dailyExpense
            prefs[KEY_TOTAL_PROFIT] = player.totalProfit
            prefs[KEY_XP] = player.xp
            prefs[KEY_LEVEL] = player.level
            prefs[KEY_INVENTORY_CAPACITY] = player.inventoryCapacity
            prefs[KEY_CURRENT_CITY] = migrateLegacyCity(player.currentCity)
            prefs[KEY_IS_USD_ACCOUNT] = player.isUsdAccount
            prefs[KEY_IS_VIP] = player.isVip
            prefs[KEY_GEMS] = player.gems
            prefs[KEY_LAST_DAILY_REWARD_MS] = player.lastDailyRewardMs
            prefs[KEY_LOGIN_STREAK] = player.loginStreak
            prefs[KEY_DOLLAR_BALANCE] = player.dollarBalance
            prefs[KEY_DOLLAR_DEPOSIT_BALANCE] = player.dollarDepositBalance
            prefs[KEY_DOLLAR_LOAN_AMOUNT] = player.dollarLoanAmount

            // Game & Market State
            prefs[KEY_SEASON] = gameState.season
            prefs[KEY_ACTIVE_EVENT] = gameState.activeEvent
            prefs[KEY_GLOBAL_INFLATION] = gameState.globalInflationRate
            prefs[KEY_CENTRAL_BANK_LOAN_RATE] = gameState.centralBankLoanRate
            prefs[KEY_CENTRAL_BANK_DEPOSIT_RATE] = gameState.centralBankDepositRate
            prefs[KEY_TOTAL_MARKET_LIQUIDITY] = gameState.totalMarketLiquidity
            prefs[KEY_USD_TRY_RATE] = gameState.usdTryRate

            // Automation & Staff flags
            prefs[KEY_AUTO_SELL] = isAutoSell
            prefs[KEY_AUTO_BUY] = isAutoBuy
            prefs[KEY_AUTO_PRODUCE] = isAutoProduce
            prefs[KEY_PROD_MANAGER_HIRED] = isProdManagerHired
            prefs[KEY_ENGINEER_HIRED] = isEngineerHired
            prefs[KEY_ENGINEER_ACTIVE] = isEngineerActive
            prefs[KEY_SALES_EXEC_HIRED] = isSalesExecHired
            prefs[KEY_SALES_EXEC_ACTIVE] = isSalesExecActive

            // Tech & IPO
            prefs[KEY_TECH_GREEN_ENERGY] = techGreenEnergy
            prefs[KEY_TECH_QUALITY_CONTROL] = techQualityControl
            prefs[KEY_TECH_LOGISTICS] = techLogistics
            prefs[KEY_TECH_AUTOMATION] = techAutomation
            prefs[KEY_TECH_QUANTUM_AI] = techQuantumAi
            prefs[KEY_TECH_NANOTECH] = techNanotech
            prefs[KEY_TECH_CYBER_SECURITY] = techCyberSecurity
            prefs[KEY_TECH_BIOTECH_CLONING] = techBiotechCloning
            prefs[KEY_TECH_AEROSPACE] = techAerospace
            prefs[KEY_TECH_HEAVY_INDUSTRY] = techHeavyIndustry
            prefs[KEY_TECH_CONSUMER_GOODS] = techConsumerGoods
            prefs[KEY_TECH_PETROCHEM] = techPetrochem
            prefs[KEY_ACTIVE_RESEARCH_TECH_KEY] = activeResearchTechKey
            prefs[KEY_RESEARCH_END_TIME_MS] = researchEndTimeMs
            prefs[KEY_ACTIVE_RESEARCHES_JSON] = serializeActiveResearches(activeResearches)
            prefs[KEY_RESEARCH_LEVELS_JSON] = serializeResearchLevels(researchLevels)

            prefs[KEY_IS_IPO_ACTIVE] = isIpoActive
            prefs[KEY_PUBLIC_SHARE_PERCENT] = publicSharePercent
            prefs[KEY_TOTAL_DIVIDENDS_PAID] = totalDividendsPaid
            prefs[KEY_DIVIDEND_DEBT] = dividendDebt

            // JSON Serializations for Collections
            prefs[KEY_BUSINESSES_JSON] = serializeBusinesses(businesses)
            prefs[KEY_INVENTORY_JSON] = serializeInventory(inventory)
            prefs[KEY_ACTIVE_DELIVERIES_JSON] = serializeDeliveries(deliveries)
            prefs[KEY_ACTIVE_PRODUCTIONS_JSON] = serializeActiveProductions(activeProductions)
            prefs[KEY_MARKET_PRICES_JSON] = serializeMarketPrices(marketPrices)
            prefs[KEY_MARKET_LISTINGS_JSON] = serializeMarketListings(marketListings)
            prefs[KEY_PLAYER_GUILD_SHARES_JSON] = serializePlayerGuildShares(playerGuildShares)
            prefs[KEY_PLAYER_GUILD_BUY_PRICES_JSON] = serializePlayerGuildBuyPrices(playerGuildBuyPrices)
            prefs[KEY_MANAGERS_JSON] = serializeManagers(managers)
            
            val megaJsonElements = megaProjects.map { proj -> anyToJsonElement(proj.toMap()) }
            prefs[KEY_DAILY_QUEST_STATE_JSON] = dailyQuestStateJson
            prefs[KEY_MEGA_PROJECTS_JSON] = AppJson.encodeToString(megaJsonElements)
            
            prefs[KEY_AUCTIONS_JSON] = serializeAuctions(auctions)
            prefs[KEY_MUSEUM_HERITAGE_JSON] = MuseumHeritageManager.exportToJson(context)
            prefs[KEY_GROWTH_HISTORY_JSON] = growthHistoryJson

            // Metadata & Anti-Cheat Cryptographic Signing
            val secureSaveTime = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
            prefs[KEY_LAST_SAVED_TIME] = secureSaveTime
            prefs[KEY_LAST_KNOWN_ELAPSED_REALTIME] = android.os.SystemClock.elapsedRealtime()
            prefs[KEY_IS_DATA_SAVED] = true
            prefs[KEY_SINGLE_CURRENCY_MIGRATED] = true

            val integritySignature = com.example.data.security.AntiCheatEngine.computeIntegrityHash(
                playerId = player.id.ifBlank { "local_player" },
                money = player.money,
                gems = player.gems,
                depositBalance = player.depositBalance,
                loanAmount = player.loanAmount,
                level = player.level,
                xp = player.xp,
                timestampMs = secureSaveTime
            )
            prefs[KEY_LOCAL_INTEGRITY_SIGNATURE] = integritySignature
        }
    }

    suspend fun getLastSavedTime(): Long {
        return try {
            val prefs = context.economicDataStore.data.first()
            prefs[KEY_LAST_SAVED_TIME] ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun serializeGrowthHistory(list: List<GrowthPointDto>): String {
        return try {
            AppJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(GrowthPointDto.serializer()), list)
        } catch (_: Exception) {
            "[]"
        }
    }

    fun deserializeGrowthHistory(json: String): List<GrowthPointDto> {
        if (json.isBlank() || json == "[]" || json == "{}") return emptyList()
        return try {
            AppJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(GrowthPointDto.serializer()), json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeManagers(managers: List<CompanyManager>): String {
        val dtos = managers.map { m ->
            CompanyManagerDto(
                id = m.id,
                name = m.name,
                title = m.title,
                specialty = m.specialty,
                level = m.level,
                dailySalary = m.dailySalary,
                efficiency = m.efficiency.toDouble(),
                isHired = m.isHired,
                isActive = m.isActive,
                description = m.description,
                actionLogs = m.actionLogs.map { log ->
                    ManagerActionLogDto(
                        id = log.id,
                        timestampMs = log.timestampMs,
                        description = log.description,
                        financialImpact = log.financialImpact
                    )
                }
            )
        }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeManagers(json: String): List<CompanyManager> {
        val defaultList = getDefaultCompanyManagers()
        if (json.isBlank() || json == "[]" || json == "{}" || json == "null") return defaultList

        val savedMap = mutableMapOf<String, CompanyManagerDto>()
        try {
            val dtos = AppJson.decodeFromString<List<CompanyManagerDto>>(json)
            for (dto in dtos) {
                if (dto.id.isNotBlank()) {
                    savedMap[dto.id] = dto
                }
            }
        } catch (e: Exception) {
            try {
                val elem = AppJson.parseToJsonElement(json)
                if (elem is kotlinx.serialization.json.JsonArray) {
                    for (item in elem) {
                        val obj = item as? kotlinx.serialization.json.JsonObject ?: continue
                        val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: continue
                        val isHired = (obj["isHired"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
                            ?: (obj["is_hired"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
                            ?: false
                        val level = (obj["level"] as? kotlinx.serialization.json.JsonPrimitive)?.intOrNull ?: 1
                        val eff = (obj["efficiency"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull ?: 1.0
                        val sal = (obj["dailySalary"] as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull
                            ?: (obj["daily_salary"] as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull ?: 0L
                        val name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                        val title = (obj["title"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                        val spec = (obj["specialty"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                        val desc = (obj["description"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                        savedMap[id] = CompanyManagerDto(
                            id = id,
                            name = name,
                            title = title,
                            specialty = spec,
                            level = level,
                            dailySalary = sal,
                            efficiency = eff,
                            isHired = isHired,
                            isActive = true,
                            description = desc
                        )
                    }
                }
            } catch (_: Exception) {
                return defaultList
            }
        }

        return defaultList.map { defaultMgr ->
            val obj = savedMap[defaultMgr.id] ?: return@map defaultMgr
            val level = obj.level.coerceIn(1, 5)
            val savedName = obj.name.trim()
            val hasHireEvidence = obj.isHired || obj.level > 1 || obj.actionLogs.isNotEmpty() || (savedName.isNotBlank() && savedName != defaultMgr.name)
            val isHired = hasHireEvidence
            val name = if (savedName.isNotBlank()) savedName else if (isHired) defaultMgr.name else ""
            val title = obj.title.ifBlank { defaultMgr.title }
            val specialty = obj.specialty.ifBlank { defaultMgr.specialty }
            val description = obj.description.ifBlank { defaultMgr.description }

            var expectedSalary = defaultMgr.dailySalary
            for (lvl in 2..level) {
                expectedSalary = (expectedSalary * 1.25f).toLong()
            }
            var sal = obj.dailySalary
            if (sal <= 0L) sal = expectedSalary

            val actionLogs = obj.actionLogs.map { log ->
                ManagerActionLog(
                    id = log.id.ifBlank { java.util.UUID.randomUUID().toString() },
                    timestampMs = if (log.timestampMs > 0) log.timestampMs else System.currentTimeMillis(),
                    description = log.description,
                    financialImpact = log.financialImpact
                )
            }

            defaultMgr.copy(
                name = name,
                title = title,
                specialty = specialty,
                level = level,
                dailySalary = sal,
                efficiency = obj.efficiency.toFloat(),
                isHired = isHired,
                isActive = obj.isActive,
                description = description,
                actionLogs = actionLogs
            )
        }
    }

    private fun mergeDailyQuestStatesJson(existingJson: String, importedJson: String): String {
        val imported = try {
            AppJson.decodeFromString<com.example.data.quest.DailyQuestState>(importedJson)
        } catch (e: Exception) {
            return importedJson
        }
        val existing = try {
            AppJson.decodeFromString<com.example.data.quest.DailyQuestState>(existingJson)
        } catch (e: Exception) {
            null
        }

        if (existing == null) return importedJson

        val mergedFree = (existing.claimedFreeTiers + imported.claimedFreeTiers).toSet()
        val mergedVip = (existing.claimedVipTiers + imported.claimedVipTiers).toSet()
        val todayKey = com.example.data.quest.DailyQuestManager.getTodayDateKey()

        val mergedClaimedQuestIds = if (existing.lastResetDateKey == todayKey && imported.lastResetDateKey == todayKey) {
            (existing.claimedQuestIdsToday + imported.claimedQuestIdsToday).toSet()
        } else if (existing.lastResetDateKey == todayKey) {
            existing.claimedQuestIdsToday
        } else if (imported.lastResetDateKey == todayKey) {
            imported.claimedQuestIdsToday
        } else {
            emptySet()
        }

        val mergedQuests: List<com.example.data.quest.DailyQuest>
        val finalResetKey: String

        if (existing.lastResetDateKey == todayKey && imported.lastResetDateKey == todayKey && existing.quests.isNotEmpty() && imported.quests.isNotEmpty()) {
            val existingQuestsMap = existing.quests.associateBy { it.id }
            mergedQuests = imported.quests.map { q ->
                val ex = existingQuestsMap[q.id]
                val wasClaimed = q.isClaimed || (ex?.isClaimed == true) || mergedClaimedQuestIds.contains(q.id)
                val prog = if (ex != null) maxOf(q.currentProgress, ex.currentProgress) else q.currentProgress
                q.copy(
                    currentProgress = prog,
                    isClaimed = wasClaimed
                )
            }
            finalResetKey = todayKey
        } else if (existing.lastResetDateKey == todayKey && existing.quests.isNotEmpty()) {
            mergedQuests = existing.quests.map { q ->
                q.copy(isClaimed = q.isClaimed || mergedClaimedQuestIds.contains(q.id))
            }
            finalResetKey = todayKey
        } else if (imported.lastResetDateKey == todayKey && imported.quests.isNotEmpty()) {
            mergedQuests = imported.quests.map { q ->
                q.copy(isClaimed = q.isClaimed || mergedClaimedQuestIds.contains(q.id))
            }
            finalResetKey = todayKey
        } else {
            mergedQuests = com.example.data.quest.DailyQuestManager.generateFreshDailyQuests()
            finalResetKey = todayKey
        }

        val mergedSeasonXp = maxOf(existing.seasonXp, imported.seasonXp)
        val mergedSeasonLevel = com.example.data.quest.DailyQuestManager.computeSeasonLevel(mergedSeasonXp)

        val merged = imported.copy(
            claimedFreeTiers = mergedFree,
            claimedVipTiers = mergedVip,
            claimedQuestIdsToday = mergedClaimedQuestIds,
            quests = mergedQuests,
            seasonXp = mergedSeasonXp,
            seasonLevel = mergedSeasonLevel,
            isVipPassUnlocked = existing.isVipPassUnlocked || imported.isVipPassUnlocked,
            lastResetDateKey = finalResetKey
        )
        return try {
            AppJson.encodeToString(merged)
        } catch (e: Exception) {
            importedJson
        }
    }

    suspend fun saveOnlineAuth(email: String, passwordHash: String, isRegistered: Boolean) {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_ONLINE_EMAIL] = email
            prefs[KEY_ONLINE_PASSWORD] = passwordHash
            prefs[KEY_IS_ONLINE_REGISTERED] = isRegistered
        }
    }

    suspend fun markWarehouseSet() {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_HAS_SET_WAREHOUSE] = true
            prefs[KEY_IS_DATA_SAVED] = true
        }
    }

    suspend fun markFirstTradeCompleted() {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_HAS_COMPLETED_FIRST_TRADE] = true
            prefs[KEY_IS_DATA_SAVED] = true
        }
    }

    // JSON serialization helpers using kotlinx.serialization
    @Serializable
    private data class LocalMarketPriceJsonDto(
        val itemId: String = "",
        val originCountry: String = "Türkiye",
        val originCityId: String = "istanbul",
        val price: Long = 100L,
        val borsaStock: Long = MacroEconomyEngine.DEFAULT_BORSA_STOCK,
        val isUsd: Boolean = false
    )

    @Serializable
    private data class LocalMarketListingJsonDto(
        val id: String = "",
        val sellerName: String = "Tüccar",
        val itemId: String = "",
        val quantity: Int = 0,
        val pricePerUnit: Long = 100L,
        val originCityId: String = "istanbul"
    )

    fun serializeBusinesses(list: List<BusinessEntity>): String {
        val dtos = list.map { b ->
            BusinessDto(
                id = b.id,
                type = b.type,
                level = b.level,
                cityId = migrateLegacyCity(b.cityId),
                wearLevel = b.wearLevel.toDouble(),
                wearLevelSnake = b.wearLevel.toDouble(),
                storageCapacity = b.getEffectiveStorageCapacity(),
                storageCapacitySnake = b.getEffectiveStorageCapacity(),
                storedItemsJson = b.storedItemsJson,
                storedItemsJsonSnake = b.storedItemsJson,
                isUpgrading = b.isUpgrading,
                isUpgradingSnake = b.isUpgrading,
                upgradeEndTime = b.upgradeEndTime,
                upgradeEndTimeSnake = b.upgradeEndTime,
                isConstructing = b.isConstructing,
                isConstructingSnake = b.isConstructing,
                constructionEndTime = b.constructionEndTime,
                constructionEndTimeSnake = b.constructionEndTime
            )
        }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeBusinesses(json: String): List<BusinessEntity> {
        if (json.isBlank() || json == "[]" || json == "null") return emptyList()
        return try {
            val dtos = AppJson.decodeFromString<List<BusinessDto>>(json)
            dtos.map {
                BusinessEntity(
                    id = it.id,
                    type = it.type,
                    level = it.level,
                    cityId = migrateLegacyCity(it.cityId),
                    wearLevel = it.effectiveWearLevel.toFloat(),
                    storageCapacity = if (it.effectiveStorageCapacity > 0) it.effectiveStorageCapacity else 2500,
                    storedItemsJson = if (it.effectiveStoredItemsJson.isNotBlank()) it.effectiveStoredItemsJson else "{}",
                    isUpgrading = it.effectiveIsUpgrading,
                    upgradeEndTime = it.effectiveUpgradeEndTime,
                    isConstructing = it.effectiveIsConstructing,
                    constructionEndTime = it.effectiveConstructionEndTime
                )
            }
        } catch (e: Exception) {
            try {
                val array = AppJson.parseToJsonElement(json) as? JsonArray ?: return emptyList()
                array.mapNotNull { item ->
                    val obj = item as? JsonObject ?: return@mapNotNull null
                    val id = (obj["id"] as? JsonPrimitive)?.intOrNull ?: 0
                    val type = (obj["type"] as? JsonPrimitive)?.content ?: ""
                    val level = (obj["level"] as? JsonPrimitive)?.intOrNull ?: 1
                    val cityId = (obj["cityId"] as? JsonPrimitive)?.content
                        ?: (obj["city_id"] as? JsonPrimitive)?.content ?: "istanbul"
                    val wear = (obj["wearLevel"] as? JsonPrimitive)?.doubleOrNull
                        ?: (obj["wear_level"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
                    val cap = (obj["storageCapacity"] as? JsonPrimitive)?.intOrNull
                        ?: (obj["storage_capacity"] as? JsonPrimitive)?.intOrNull ?: 2500
                    val stored = (obj["storedItemsJson"] as? JsonPrimitive)?.content
                        ?: (obj["stored_items_json"] as? JsonPrimitive)?.content ?: "{}"
                    val isUpg = (obj["isUpgrading"] as? JsonPrimitive)?.booleanOrNull
                        ?: (obj["is_upgrading"] as? JsonPrimitive)?.booleanOrNull ?: false
                    val upgEnd = (obj["upgradeEndTime"] as? JsonPrimitive)?.longOrNull
                        ?: (obj["upgrade_end_time"] as? JsonPrimitive)?.longOrNull
                    val isCons = (obj["isConstructing"] as? JsonPrimitive)?.booleanOrNull
                        ?: (obj["is_constructing"] as? JsonPrimitive)?.booleanOrNull ?: false
                    val consEnd = (obj["constructionEndTime"] as? JsonPrimitive)?.longOrNull
                        ?: (obj["construction_end_time"] as? JsonPrimitive)?.longOrNull

                    val secureNow = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
                    val finalIsCons = isCons || (consEnd != null && consEnd > secureNow)
                    val finalIsUpg = isUpg || (upgEnd != null && upgEnd > secureNow)

                    BusinessEntity(
                        id = id,
                        type = type,
                        level = level,
                        cityId = migrateLegacyCity(cityId),
                        wearLevel = wear.toFloat(),
                        storageCapacity = if (cap > 0) cap else 2500,
                        storedItemsJson = if (stored.isNotBlank()) stored else "{}",
                        isUpgrading = finalIsUpg,
                        upgradeEndTime = upgEnd,
                        isConstructing = finalIsCons,
                        constructionEndTime = consEnd
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun serializeDeliveries(list: List<DeliveryItem>): String {
        val dtos = list.map { d ->
            DeliveryDto(
                id = d.id,
                itemId = d.itemId,
                quantity = d.quantity,
                originCityId = migrateLegacyCity(d.originCityId),
                destinationCityId = migrateLegacyCity(d.destinationCityId),
                pricePerUnit = d.pricePerUnit,
                totalCost = d.totalCost,
                startTimeMs = d.startTimeMs,
                totalDurationMs = d.totalDurationMs
            )
        }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeDeliveries(json: String): List<DeliveryItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val dtos = AppJson.decodeFromString<List<DeliveryDto>>(json)
            dtos.map {
                DeliveryItem(
                    id = it.id.ifBlank { java.util.UUID.randomUUID().toString() },
                    itemId = it.itemId,
                    quantity = it.quantity,
                    originCityId = migrateLegacyCity(it.originCityId),
                    destinationCityId = migrateLegacyCity(it.destinationCityId),
                    pricePerUnit = it.pricePerUnit,
                    totalCost = it.totalCost,
                    startTimeMs = if (it.startTimeMs > 0) it.startTimeMs else System.currentTimeMillis(),
                    totalDurationMs = it.totalDurationMs
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeActiveProductions(list: List<ActiveProduction>): String {
        return try {
            AppJson.encodeToString(list.map { it.copy(cityId = migrateLegacyCity(it.cityId)) })
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeActiveProductions(json: String): List<ActiveProduction> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val list = AppJson.decodeFromString<List<ActiveProduction>>(json)
            list.map { it.copy(cityId = migrateLegacyCity(it.cityId)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeInventory(list: List<InventoryEntity>): String {
        val dtos = list.map { InventoryDto(itemId = it.itemId, quantity = it.quantity) }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeInventory(json: String): List<InventoryEntity> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val dtos = AppJson.decodeFromString<List<InventoryDto>>(json)
            dtos.map { InventoryEntity(itemId = it.itemId, quantity = it.quantity) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeMarketPrices(list: List<MarketPriceEntity>): String {
        val dtos = list.map { LocalMarketPriceJsonDto(
            itemId = it.itemId, 
            originCountry = it.originCountry,
            originCityId = it.originCityId,
            price = it.price, 
            borsaStock = it.borsaStock,
            isUsd = it.isUsd
        ) }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeMarketPrices(json: String): List<MarketPriceEntity> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val dtos = AppJson.decodeFromString<List<LocalMarketPriceJsonDto>>(json)
            val list = dtos.map { MarketPriceEntity(
                itemId = it.itemId, 
                originCountry = it.originCountry,
                originCityId = it.originCityId,
                price = it.price, 
                borsaStock = it.borsaStock,
                isUsd = it.isUsd
            ) }
            sanitizeMarketPrices(list)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeMarketListings(list: List<MarketListing>): String {
        val dtos = list.map {
            LocalMarketListingJsonDto(
                id = it.id,
                sellerName = it.sellerName,
                itemId = it.itemId,
                quantity = it.quantity,
                pricePerUnit = it.pricePerUnit,
                originCityId = it.originCityId
            )
        }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeMarketListings(json: String): List<MarketListing> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val dtos = AppJson.decodeFromString<List<LocalMarketListingJsonDto>>(json)
            dtos.map {
                MarketListing(
                    id = it.id.ifBlank { java.util.UUID.randomUUID().toString() },
                    sellerName = it.sellerName,
                    itemId = it.itemId,
                    quantity = it.quantity,
                    pricePerUnit = it.pricePerUnit,
                    originCityId = it.originCityId
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializePlayerGuildShares(map: Map<String, Int>): String {
        return AppJson.encodeToString(map)
    }

    fun deserializePlayerGuildShares(json: String): Map<String, Int> {
        if (json.isBlank() || json == "{}" || json == "[]") return emptyMap()
        return try {
            AppJson.decodeFromString<Map<String, Int>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializePlayerGuildBuyPrices(map: Map<String, Double>): String {
        return AppJson.encodeToString(map)
    }

    fun deserializePlayerGuildBuyPrices(json: String): Map<String, Double> {
        if (json.isBlank() || json == "{}" || json == "[]") return emptyMap()
        return try {
            AppJson.decodeFromString<Map<String, Double>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeActiveResearches(map: Map<String, Long>): String {
        val clean = mutableMapOf<String, Long>()
        map.forEach { (k, v) ->
            val base = k.removePrefix("tech_")
            clean[base] = maxOf(clean[base] ?: 0L, v)
        }
        return AppJson.encodeToString(clean)
    }

    fun deserializeActiveResearches(json: String): Map<String, Long> {
        if (json.isBlank() || json == "{}" || json == "[]") return emptyMap()
        return try {
            val raw = AppJson.decodeFromString<Map<String, Long>>(json).filterValues { it > 0L }
            val clean = mutableMapOf<String, Long>()
            raw.forEach { (k, v) ->
                val base = k.removePrefix("tech_")
                clean[base] = maxOf(clean[base] ?: 0L, v)
            }
            clean
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeResearchLevels(map: Map<String, Int>): String {
        return AppJson.encodeToString(map)
    }

    fun deserializeResearchLevels(json: String): Map<String, Int> {
        if (json.isBlank() || json == "{}" || json == "[]" || json == "null") return emptyMap()
        val clean = mutableMapOf<String, Int>()
        try {
            val raw = AppJson.decodeFromString<Map<String, Int>>(json).filterValues { it > 0 }
            raw.forEach { (k, v) ->
                val base = k.removePrefix("tech_")
                clean[base] = maxOf(clean[base] ?: 0, v).coerceAtMost(5)
                clean["tech_$base"] = maxOf(clean["tech_$base"] ?: 0, v).coerceAtMost(5)
            }
        } catch (e: Exception) {
            try {
                val elem = AppJson.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject
                elem?.forEach { (k, vElem) ->
                    val lvl = when (vElem) {
                        is kotlinx.serialization.json.JsonPrimitive -> vElem.intOrNull ?: vElem.content.toIntOrNull() ?: 0
                        else -> 0
                    }
                    if (lvl > 0) {
                        val base = k.removePrefix("tech_")
                        clean[base] = maxOf(clean[base] ?: 0, lvl).coerceAtMost(5)
                        clean["tech_$base"] = maxOf(clean["tech_$base"] ?: 0, lvl).coerceAtMost(5)
                    }
                }
            } catch (_: Exception) {}
        }
        return clean
    }

    fun serializeAuctions(auctions: List<Auction>): String {
        val dtos = auctions.map { a ->
            AuctionDto(
                id = a.id,
                itemId = a.itemId,
                quantity = a.quantity,
                startingBid = a.startingBid,
                currentBid = a.currentBid,
                currentBidderId = a.currentBidderId,
                currentBidderName = a.currentBidderName,
                sellerId = a.sellerId,
                sellerName = a.sellerName,
                originCityId = a.originCityId,
                expiresAt = a.expiresAt,
                createdAt = a.createdAt
            )
        }
        return AppJson.encodeToString(dtos)
    }

    fun deserializeAuctions(jsonStr: String): List<Auction> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            val dtos = AppJson.decodeFromString<List<AuctionDto>>(jsonStr)
            dtos.map { a ->
                Auction(
                    id = a.id.ifBlank { java.util.UUID.randomUUID().toString() },
                    itemId = a.itemId,
                    quantity = a.quantity,
                    startingBid = a.startingBid,
                    currentBid = a.currentBid,
                    currentBidderId = a.currentBidderId,
                    currentBidderName = a.currentBidderName,
                    sellerId = a.sellerId,
                    sellerName = a.sellerName,
                    originCityId = a.originCityId,
                    expiresAt = a.expiresAt,
                    createdAt = a.createdAt
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveSelectedTheme(themeId: String) {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME] = themeId
        }
    }

    suspend fun saveSelectedLanguage(languageCode: String) {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_SELECTED_LANGUAGE] = languageCode
        }
    }

    suspend fun getLastClaimedMonthlyRewardKey(): String {
        return context.economicDataStore.data.map { prefs ->
            prefs.getStringSafe("last_claimed_monthly_reward_key", "")
        }.first()
    }

    suspend fun setLastClaimedMonthlyRewardKey(key: String) {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_LAST_CLAIMED_MONTHLY_REWARD_KEY] = key
        }
    }

    suspend fun saveUiMode(isExpert: Boolean) {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_IS_EXPERT_MODE] = isExpert
        }
    }

    suspend fun getUiMode(): Boolean {
        return context.economicDataStore.data.map { prefs ->
            prefs.getBooleanSafe("is_expert_mode", false)
        }.first()
    }

    suspend fun getIsEntrepreneurGuideCompleted(): Boolean {
        return context.economicDataStore.data.map { prefs ->
            prefs.getBooleanSafe("is_entrepreneur_guide_completed", false)
        }.first()
    }

    suspend fun setIsEntrepreneurGuideCompleted(completed: Boolean) {
        context.economicDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("is_entrepreneur_guide_completed"))
            prefs[KEY_IS_ENTREPRENEUR_GUIDE_COMPLETED] = completed
        }
    }

    suspend fun saveBorsaLimitOrders(orders: List<BorsaLimitOrder>) {
        context.economicDataStore.edit { prefs ->
            prefs[KEY_BORSA_LIMIT_ORDERS_JSON] = BorsaLimitOrderSerializer.serializeList(orders)
        }
    }

    suspend fun getBorsaLimitOrders(): List<BorsaLimitOrder> {
        return context.economicDataStore.data.map { prefs ->
            val raw = prefs.getStringSafe("borsa_limit_orders_json", "[]")
            BorsaLimitOrderSerializer.deserializeList(raw)
        }.first()
    }

    private val HMAC_SECRET_KEY = "ANADOLU_SECRET_KEY_2026_!@#"

    private fun generateSaveSignature(jsonObj: kotlinx.serialization.json.JsonObject): String {
        return try {
            val money = jsonObj["money"]?.jsonPrimitive?.content ?: "0"
            val loan = jsonObj["loan_amount"]?.jsonPrimitive?.content ?: "0"
            val deposit = jsonObj["deposit_balance"]?.jsonPrimitive?.content ?: "0"
            val dollarBalance = jsonObj["dollar_balance"]?.jsonPrimitive?.content ?: "0"
            val inventory = jsonObj["inventory_json"]?.jsonPrimitive?.content ?: "[]"
            
            val dataToSign = "$money|$loan|$deposit|$dollarBalance|$inventory"
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKeySpec = javax.crypto.spec.SecretKeySpec(HMAC_SECRET_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val hmacBytes = mac.doFinal(dataToSign.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(hmacBytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun exportSaveJson(): String {
        val prefs = context.economicDataStore.data.first()
        val map = mutableMapOf<String, Any?>()
        val excludedKeys = setOf(
            KEY_MARKET_PRICES_JSON.name,
            KEY_MARKET_LISTINGS_JSON.name,
            KEY_MEGA_PROJECTS_JSON.name,
            KEY_AUCTIONS_JSON.name
        )
        for ((key, value) in prefs.asMap()) {
            if (key.name !in excludedKeys) {
                map[key.name] = value
            }
        }
        map["exportedAtMs"] = System.currentTimeMillis()
        
        val jsonObj = anyToJsonElement(map) as? JsonObject
        if (jsonObj != null) {
            map["save_signature"] = generateSaveSignature(jsonObj)
        }
        
        return AppJson.encodeToString(anyToJsonElement(map))
    }

    suspend fun importSaveJson(jsonString: String, force: Boolean = false): Boolean {
        return try {
            val jsonElement = AppJson.parseToJsonElement(jsonString) as? JsonObject ?: return false
            
            // Check signature for tampering logging, but allow admin/server edits
            val storedSignature = jsonElement["save_signature"]?.jsonPrimitive?.content
            if (storedSignature != null) {
                val expectedSignature = generateSaveSignature(jsonElement)
                if (storedSignature != expectedSignature) {
                    android.util.Log.w("EconomicDataStore", "Save file signature updated from cloud/admin modification.")
                }
            }

            // Extract incoming last saved time
            val incomingLastSavedTime = jsonElement["last_saved_time"]?.jsonPrimitive?.longOrNull 
                ?: jsonElement["exportedAtMs"]?.jsonPrimitive?.longOrNull 
                ?: 0L
            
            val currentPrefs = context.economicDataStore.data.first()
            val localLastSavedTime = currentPrefs[KEY_LAST_SAVED_TIME] ?: 0L
            val localMoney = currentPrefs[KEY_MONEY] ?: 0L
            val localLevel = currentPrefs[KEY_LEVEL] ?: 1
            val localProfit = currentPrefs[KEY_TOTAL_PROFIT] ?: 0L
            val localXp = currentPrefs[KEY_XP] ?: 0
            
            // Check if local save is strictly newer and has some actual substantial progress (not default startup state)
            val isLocalNewer = localLastSavedTime > incomingLastSavedTime
            val hasLocalProgress = localLevel > 1 || localMoney > 300_000L || localProfit > 0L || localXp > 100
            
            if (isLocalNewer && hasLocalProgress && !force) {
                android.util.Log.i("EconomicDataStore", "Skipping import: local save is newer ($localLastSavedTime) than incoming cloud save ($incomingLastSavedTime).")
                return false
            }

            context.economicDataStore.edit { prefs ->
                // Do NOT clear prefs to preserve live multiplayer state caches
                // prefs.clear()

                val longKeys = mapOf(
                    "money" to KEY_MONEY,
                    "loan_amount" to KEY_LOAN_AMOUNT,
                    "deposit_balance" to KEY_DEPOSIT_BALANCE,
                    "locked_deposit_balance" to KEY_LOCKED_DEPOSIT_BALANCE,
                    "locked_deposit_start_time_ms" to KEY_LOCKED_DEPOSIT_START_TIME_MS,
                    "locked_deposit_duration_ms" to KEY_LOCKED_DEPOSIT_DURATION_MS,
                    "daily_income" to KEY_DAILY_INCOME,
                    "daily_expense" to KEY_DAILY_EXPENSE,
                    "total_profit" to KEY_TOTAL_PROFIT,
                    "last_daily_reward_ms" to KEY_LAST_DAILY_REWARD_MS,
                    "total_market_liquidity" to KEY_TOTAL_MARKET_LIQUIDITY,
                    "research_end_time_ms" to KEY_RESEARCH_END_TIME_MS,
                    "total_dividends_paid" to KEY_TOTAL_DIVIDENDS_PAID,
                    "dollar_balance" to KEY_DOLLAR_BALANCE,
                    "dollar_deposit_balance" to KEY_DOLLAR_DEPOSIT_BALANCE,
                    "dollar_loan_amount" to KEY_DOLLAR_LOAN_AMOUNT,
                    "last_saved_time" to KEY_LAST_SAVED_TIME
                )

                val intKeys = mapOf(
                    "xp" to KEY_XP,
                    "level" to KEY_LEVEL,
                    "inventory_capacity" to KEY_INVENTORY_CAPACITY,
                    "gems" to KEY_GEMS,
                    "login_streak" to KEY_LOGIN_STREAK,
                    "tech_green_energy" to KEY_TECH_GREEN_ENERGY,
                    "tech_quality_control" to KEY_TECH_QUALITY_CONTROL,
                    "tech_logistics" to KEY_TECH_LOGISTICS,
                    "tech_automation" to KEY_TECH_AUTOMATION,
                    "tech_quantum_ai" to KEY_TECH_QUANTUM_AI,
                    "tech_nanotech" to KEY_TECH_NANOTECH,
                    "tech_cyber_security" to KEY_TECH_CYBER_SECURITY,
                    "tech_biotech_cloning" to KEY_TECH_BIOTECH_CLONING,
                    "tech_aerospace" to KEY_TECH_AEROSPACE,
                    "tech_heavy_industry" to KEY_TECH_HEAVY_INDUSTRY,
                    "tech_consumer_goods" to KEY_TECH_CONSUMER_GOODS,
                    "tech_petrochem" to KEY_TECH_PETROCHEM,
                    "public_share_percent" to KEY_PUBLIC_SHARE_PERCENT
                )

                val floatKeys = mapOf(
                    "global_inflation" to KEY_GLOBAL_INFLATION,
                    "central_bank_loan_rate" to KEY_CENTRAL_BANK_LOAN_RATE,
                    "central_bank_deposit_rate" to KEY_CENTRAL_BANK_DEPOSIT_RATE
                )

                val boolKeys = mapOf(
                    "is_single_currency_migrated" to KEY_SINGLE_CURRENCY_MIGRATED,
                    "is_vip" to KEY_IS_VIP,
                    "is_online_registered" to KEY_IS_ONLINE_REGISTERED,
                    "is_auto_sell_active" to KEY_AUTO_SELL,
                    "is_auto_buy_active" to KEY_AUTO_BUY,
                    "is_auto_produce_active" to KEY_AUTO_PRODUCE,
                    "is_prod_manager_hired" to KEY_PROD_MANAGER_HIRED,
                    "is_engineer_hired" to KEY_ENGINEER_HIRED,
                    "is_engineer_active" to KEY_ENGINEER_ACTIVE,
                    "is_sales_exec_hired" to KEY_SALES_EXEC_HIRED,
                    "is_sales_exec_active" to KEY_SALES_EXEC_ACTIVE,
                    "is_ipo_active" to KEY_IS_IPO_ACTIVE,
                    "is_data_saved" to KEY_IS_DATA_SAVED,
                    "has_set_warehouse" to KEY_HAS_SET_WAREHOUSE,
                    "has_completed_first_trade" to KEY_HAS_COMPLETED_FIRST_TRADE,
                    "is_expert_mode" to KEY_IS_EXPERT_MODE,
                    "is_new_player_guide_completed" to KEY_IS_NEW_PLAYER_GUIDE_COMPLETED,
                    "is_entrepreneur_guide_completed" to KEY_IS_ENTREPRENEUR_GUIDE_COMPLETED
                )

                val stringKeys = mapOf(
                    "name" to KEY_NAME,
                    "player_name" to KEY_NAME,
                    "current_city" to KEY_CURRENT_CITY,
                    "online_email" to KEY_ONLINE_EMAIL,
                    "online_password" to KEY_ONLINE_PASSWORD,
                    "season" to KEY_SEASON,
                    "active_event" to KEY_ACTIVE_EVENT,
                    "active_research_tech_key" to KEY_ACTIVE_RESEARCH_TECH_KEY,
                    "active_researches_json" to KEY_ACTIVE_RESEARCHES_JSON,
                    "active_researches" to KEY_ACTIVE_RESEARCHES_JSON,
                    "research_levels_json" to KEY_RESEARCH_LEVELS_JSON,
                    "research_levels" to KEY_RESEARCH_LEVELS_JSON,
                    "businesses_json" to KEY_BUSINESSES_JSON,
                    "businesses" to KEY_BUSINESSES_JSON,
                    "inventory_json" to KEY_INVENTORY_JSON,
                    "inventory" to KEY_INVENTORY_JSON,
                    "active_deliveries_json" to KEY_ACTIVE_DELIVERIES_JSON,
                    "active_deliveries" to KEY_ACTIVE_DELIVERIES_JSON,
                    "active_productions_json" to KEY_ACTIVE_PRODUCTIONS_JSON,
                    "active_productions" to KEY_ACTIVE_PRODUCTIONS_JSON,
                    "player_guild_shares_json" to KEY_PLAYER_GUILD_SHARES_JSON,
                    "guild_shares" to KEY_PLAYER_GUILD_SHARES_JSON,
                    "player_guild_buy_prices_json" to KEY_PLAYER_GUILD_BUY_PRICES_JSON,
                    "guild_buy_prices" to KEY_PLAYER_GUILD_BUY_PRICES_JSON,
                    "managers_json" to KEY_MANAGERS_JSON,
                    "managers" to KEY_MANAGERS_JSON,
                    "daily_quest_state_json" to KEY_DAILY_QUEST_STATE_JSON,
                    "daily_quest_state" to KEY_DAILY_QUEST_STATE_JSON,
                    "museum_heritage_json" to KEY_MUSEUM_HERITAGE_JSON,
                    "borsa_limit_orders_json" to KEY_BORSA_LIMIT_ORDERS_JSON,
                    "growth_history_json" to KEY_GROWTH_HISTORY_JSON,
                    "selected_theme" to KEY_SELECTED_THEME,
                    "selected_language" to KEY_SELECTED_LANGUAGE,
                    "last_claimed_monthly_reward_key" to KEY_LAST_CLAIMED_MONTHLY_REWARD_KEY,
                    "onboarding_claimed_steps" to KEY_ONBOARDING_CLAIMED_STEPS
                )

                for ((k, elem) in jsonElement) {
                    if (k == "exportedAtMs") continue
                    if (longKeys.containsKey(k)) {
                        val longVal = when (elem) {
                            is JsonPrimitive -> elem.longOrNull ?: elem.content.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        prefs[longKeys[k]!!] = longVal
                    } else if (intKeys.containsKey(k)) {
                        val intVal = when (elem) {
                            is JsonPrimitive -> elem.longOrNull?.toInt() ?: elem.content.toIntOrNull() ?: 0
                            else -> 0
                        }
                        val prefKey = intKeys[k]!!
                        if (k.startsWith("tech_")) {
                            val currentLvl = prefs[prefKey] ?: 0
                            prefs[prefKey] = maxOf(currentLvl, intVal).coerceIn(0, 5)
                        } else {
                            prefs[prefKey] = intVal
                        }
                    } else if (floatKeys.containsKey(k)) {
                        val floatVal = when (elem) {
                            is JsonPrimitive -> elem.doubleOrNull?.toFloat() ?: elem.content.toFloatOrNull() ?: 0f
                            else -> 0f
                        }
                        prefs[floatKeys[k]!!] = floatVal
                    } else if (boolKeys.containsKey(k)) {
                        val boolVal = when (elem) {
                            is JsonPrimitive -> elem.booleanOrNull ?: elem.content.toBooleanStrictOrNull() ?: false
                            else -> false
                        }
                        prefs[boolKeys[k]!!] = boolVal
                    } else if (stringKeys.containsKey(k)) {
                        val rawStr = when (elem) {
                            is JsonPrimitive -> elem.content
                            else -> elem.toString()
                        }
                        val prefKey = stringKeys[k]!!
                        val sanitizedStr = when (prefKey) {
                            KEY_CURRENT_CITY -> migrateLegacyCity(rawStr)
                            KEY_BUSINESSES_JSON -> serializeBusinesses(deserializeBusinesses(rawStr))
                            KEY_ACTIVE_DELIVERIES_JSON -> serializeDeliveries(deserializeDeliveries(rawStr))
                            KEY_ACTIVE_PRODUCTIONS_JSON -> serializeActiveProductions(deserializeActiveProductions(rawStr))
                            KEY_DAILY_QUEST_STATE_JSON -> mergeDailyQuestStatesJson(prefs[KEY_DAILY_QUEST_STATE_JSON] ?: "{}", rawStr)
                            KEY_MANAGERS_JSON -> {
                                val existingManagersJson = prefs[KEY_MANAGERS_JSON] ?: "[]"
                                val existingList = deserializeManagers(existingManagersJson)
                                val incomingList = deserializeManagers(rawStr)
                                val defaultList = getDefaultCompanyManagers()

                                val mergedList = defaultList.map { def ->
                                    val ex = existingList.find { it.id == def.id }
                                    val inc = incomingList.find { it.id == def.id }
                                    val hasHire = (inc?.isHired == true) || (ex?.isHired == true) || 
                                                  ((inc?.level ?: 1) > 1) || ((ex?.level ?: 1) > 1) ||
                                                  (inc?.actionLogs?.isNotEmpty() == true) || (ex?.actionLogs?.isNotEmpty() == true) ||
                                                  (inc?.name?.isNotBlank() == true && inc.name != def.name) ||
                                                  (ex?.name?.isNotBlank() == true && ex.name != def.name)
                                    val isHired = hasHire
                                    val level = maxOf(ex?.level ?: 1, inc?.level ?: 1).coerceIn(1, 5)
                                    val eff = maxOf(ex?.efficiency ?: 1.0f, inc?.efficiency ?: 1.0f)
                                    val name = when {
                                        inc != null && inc.name.isNotBlank() && inc.name != def.name -> inc.name
                                        ex != null && ex.name.isNotBlank() && ex.name != def.name -> ex.name
                                        inc != null && inc.name.isNotBlank() -> inc.name
                                        ex != null && ex.name.isNotBlank() -> ex.name
                                        else -> def.name
                                    }
                                    val logs = if (inc != null && inc.actionLogs.isNotEmpty()) inc.actionLogs else (ex?.actionLogs ?: emptyList())
                                    val sal = maxOf(ex?.dailySalary ?: 0L, inc?.dailySalary ?: 0L).let {
                                        if (it > 0L) it else def.dailySalary
                                    }
                                    def.copy(
                                        isHired = isHired,
                                        level = level,
                                        efficiency = eff,
                                        name = if (isHired) name else "",
                                        dailySalary = sal,
                                        actionLogs = logs,
                                        isActive = inc?.isActive ?: ex?.isActive ?: true
                                    )
                                }
                                serializeManagers(mergedList)
                            }
                            KEY_RESEARCH_LEVELS_JSON -> {
                                val existingLevels = deserializeResearchLevels(prefs[KEY_RESEARCH_LEVELS_JSON] ?: "{}").toMutableMap()
                                val incomingLevels = deserializeResearchLevels(rawStr)
                                val techKeys = listOf(
                                    "green_energy", "quality_control", "logistics", "automation",
                                    "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
                                    "aerospace", "heavy_industry", "consumer_goods", "petrochem",
                                    "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics"
                                )
                                val techPrefMap = mapOf(
                                    "green_energy" to KEY_TECH_GREEN_ENERGY,
                                    "quality_control" to KEY_TECH_QUALITY_CONTROL,
                                    "logistics" to KEY_TECH_LOGISTICS,
                                    "automation" to KEY_TECH_AUTOMATION,
                                    "quantum_ai" to KEY_TECH_QUANTUM_AI,
                                    "nanotech" to KEY_TECH_NANOTECH,
                                    "cyber_security" to KEY_TECH_CYBER_SECURITY,
                                    "biotech_cloning" to KEY_TECH_BIOTECH_CLONING,
                                    "biotech_med" to KEY_TECH_BIOTECH_CLONING,
                                    "aerospace" to KEY_TECH_AEROSPACE,
                                    "heavy_industry" to KEY_TECH_HEAVY_INDUSTRY,
                                    "consumer_goods" to KEY_TECH_CONSUMER_GOODS,
                                    "petrochem" to KEY_TECH_PETROCHEM
                                )
                                val merged = mutableMapOf<String, Int>()
                                techKeys.forEach { tech ->
                                    val prefKey = techPrefMap[tech]
                                    val prefVal = if (prefKey != null) prefs[prefKey] ?: 0 else 0
                                    val exLvl = maxOf(existingLevels[tech] ?: 0, existingLevels["tech_$tech"] ?: 0, prefVal)
                                    val directLvl = (jsonElement["tech_$tech"] as? JsonPrimitive)?.longOrNull?.toInt() 
                                        ?: (jsonElement[tech] as? JsonPrimitive)?.longOrNull?.toInt() 
                                        ?: 0
                                    val incLvl = maxOf(incomingLevels[tech] ?: 0, incomingLevels["tech_$tech"] ?: 0, directLvl)
                                    val finalLvl = maxOf(exLvl, incLvl).coerceIn(0, 5)
                                    merged[tech] = finalLvl
                                    merged["tech_$tech"] = finalLvl
                                    if (prefKey != null && finalLvl > 0) {
                                        prefs[prefKey] = finalLvl
                                    }
                                }
                                serializeResearchLevels(merged)
                            }
                            KEY_ACTIVE_RESEARCHES_JSON -> {
                                val existingMap = try {
                                    AppJson.decodeFromString<Map<String, Long>>(prefs[KEY_ACTIVE_RESEARCHES_JSON] ?: "{}")
                                } catch (_: Exception) { emptyMap() }
                                val incomingMap = try {
                                    AppJson.decodeFromString<Map<String, Long>>(rawStr)
                                } catch (_: Exception) { emptyMap() }
                                val now = System.currentTimeMillis()
                                val mergedActive = mutableMapOf<String, Long>()
                                val offlineCompletedTechs = mutableSetOf<String>()

                                (existingMap.keys + incomingMap.keys).forEach { k ->
                                    val base = k.removePrefix("tech_")
                                    val exTime = maxOf(existingMap[base] ?: 0L, existingMap["tech_$base"] ?: 0L)
                                    val incTime = maxOf(incomingMap[base] ?: 0L, incomingMap["tech_$base"] ?: 0L)
                                    val chosenTime = maxOf(exTime, incTime)
                                    if (chosenTime > now) {
                                        mergedActive[base] = chosenTime
                                    } else if (chosenTime > 0L) {
                                        offlineCompletedTechs.add(base)
                                    }
                                }

                                val singleKey = (jsonElement["active_research_tech_key"] as? JsonPrimitive)?.content?.removePrefix("tech_")
                                val singleEnd = (jsonElement["research_end_time_ms"] as? JsonPrimitive)?.longOrNull ?: 0L
                                if (!singleKey.isNullOrBlank() && singleEnd > 0L) {
                                    if (singleEnd > now) {
                                        mergedActive[singleKey] = maxOf(mergedActive[singleKey] ?: 0L, singleEnd)
                                    } else {
                                        offlineCompletedTechs.add(singleKey)
                                    }
                                }

                                // If any research finished while offline, advance tech level
                                if (offlineCompletedTechs.isNotEmpty()) {
                                    val currentLevels = deserializeResearchLevels(prefs[KEY_RESEARCH_LEVELS_JSON] ?: "{}").toMutableMap()
                                    val techPrefMap = mapOf(
                                        "green_energy" to KEY_TECH_GREEN_ENERGY,
                                        "quality_control" to KEY_TECH_QUALITY_CONTROL,
                                        "logistics" to KEY_TECH_LOGISTICS,
                                        "automation" to KEY_TECH_AUTOMATION,
                                        "quantum_ai" to KEY_TECH_QUANTUM_AI,
                                        "nanotech" to KEY_TECH_NANOTECH,
                                        "cyber_security" to KEY_TECH_CYBER_SECURITY,
                                        "biotech_cloning" to KEY_TECH_BIOTECH_CLONING,
                                        "biotech_med" to KEY_TECH_BIOTECH_CLONING,
                                        "aerospace" to KEY_TECH_AEROSPACE,
                                        "heavy_industry" to KEY_TECH_HEAVY_INDUSTRY,
                                        "consumer_goods" to KEY_TECH_CONSUMER_GOODS,
                                        "petrochem" to KEY_TECH_PETROCHEM
                                    )
                                    offlineCompletedTechs.forEach { tech ->
                                        val cur = currentLevels[tech] ?: 0
                                        val newLvl = (cur + 1).coerceAtMost(5)
                                        currentLevels[tech] = newLvl
                                        currentLevels["tech_$tech"] = newLvl
                                        techPrefMap[tech]?.let { pKey ->
                                            prefs[pKey] = newLvl
                                        }
                                    }
                                    prefs[KEY_RESEARCH_LEVELS_JSON] = serializeResearchLevels(currentLevels)
                                }

                                val firstOngoing = mergedActive.entries.firstOrNull { it.value > now }
                                if (firstOngoing != null) {
                                    prefs[KEY_ACTIVE_RESEARCH_TECH_KEY] = firstOngoing.key
                                    prefs[KEY_RESEARCH_END_TIME_MS] = firstOngoing.value
                                } else {
                                    prefs[KEY_ACTIVE_RESEARCH_TECH_KEY] = ""
                                    prefs[KEY_RESEARCH_END_TIME_MS] = 0L
                                }
                                AppJson.encodeToString(mergedActive)
                            }
                            else -> rawStr
                        }
                        prefs[prefKey] = sanitizedStr
                    } else {
                        val strVal = when (elem) {
                            is JsonPrimitive -> elem.content
                            else -> elem.toString()
                        }
                        prefs[stringPreferencesKey(k)] = strVal
                    }
                }
                prefs[KEY_SINGLE_CURRENCY_MIGRATED] = true
                prefs[KEY_IS_DATA_SAVED] = true
                prefs[KEY_LAST_SAVED_TIME] = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
            prefs[KEY_LAST_KNOWN_ELAPSED_REALTIME] = android.os.SystemClock.elapsedRealtime()
            }
            val museumElem = jsonElement["museum_heritage_json"]
            if (museumElem != null) {
                val museumStr = if (museumElem is JsonPrimitive) museumElem.content else museumElem.toString()
                MuseumHeritageManager.importFromJson(context, museumStr)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("EconomicDataStore", "Failed to import save json", e)
            false
        }
    }

    fun deserializeMegaProjects(jsonStr: String): List<MegaProject> {
        if (jsonStr.isBlank() || jsonStr == "[]" || jsonStr == "null") return emptyList()
        return try {
            val element = AppJson.parseToJsonElement(jsonStr)
            if (element is JsonArray) {
                element.mapNotNull { item ->
                    val map = jsonElementToAny(item) as? Map<String, Any?>
                    if (map != null) MegaProject.fromMap(map) else null
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("EconomicDataStore", "Error parsing MegaProjects JSON", e)
            emptyList()
        }
    }

    suspend fun resetToFreshGame() {
        context.economicDataStore.edit { prefs ->
            val currentTheme = prefs[KEY_SELECTED_THEME] ?: "cyber_blue"
            val currentLang = prefs[KEY_SELECTED_LANGUAGE] ?: "tr"
            val isExpert = prefs[KEY_IS_EXPERT_MODE] ?: false

            prefs.clear()

            // Fresh starting state
            prefs[KEY_NAME] = "Tüccar"
            prefs[KEY_MONEY] = 0L
            prefs[KEY_LOAN_AMOUNT] = 0L
            prefs[KEY_DEPOSIT_BALANCE] = 0L
            prefs[KEY_DAILY_INCOME] = 0L
            prefs[KEY_DAILY_EXPENSE] = 0L
            prefs[KEY_TOTAL_PROFIT] = 0L
            prefs[KEY_XP] = 0
            prefs[KEY_LEVEL] = 1
            prefs[KEY_INVENTORY_CAPACITY] = 5000
            prefs[KEY_CURRENT_CITY] = "istanbul"
            prefs[KEY_IS_USD_ACCOUNT] = false
            prefs[KEY_IS_VIP] = false
            prefs[KEY_GEMS] = 0
            prefs[KEY_LAST_DAILY_REWARD_MS] = 0L
            prefs[KEY_LOGIN_STREAK] = 0
            prefs[KEY_DOLLAR_BALANCE] = 0L
            prefs[KEY_DOLLAR_DEPOSIT_BALANCE] = 0L
            prefs[KEY_DOLLAR_LOAN_AMOUNT] = 0L
            prefs[KEY_USD_TRY_RATE] = 50.0
            prefs[KEY_SEASON] = "İlkbahar"
            prefs[KEY_ACTIVE_EVENT] = "Normal"
            prefs[KEY_GLOBAL_INFLATION] = 0.0f
            prefs[KEY_CENTRAL_BANK_LOAN_RATE] = 0.15f
            prefs[KEY_CENTRAL_BANK_DEPOSIT_RATE] = 0.05f
            prefs[KEY_TOTAL_MARKET_LIQUIDITY] = 10000000L
            prefs[KEY_BUSINESSES_JSON] = "[]"
            prefs[KEY_INVENTORY_JSON] = "[]"
            prefs[KEY_ACTIVE_DELIVERIES_JSON] = "[]"
            prefs[KEY_ACTIVE_PRODUCTIONS_JSON] = "[]"
            prefs[KEY_MARKET_PRICES_JSON] = "[]"
            prefs[KEY_MARKET_LISTINGS_JSON] = "[]"
            prefs[KEY_PLAYER_GUILD_SHARES_JSON] = "{}"
            prefs[KEY_PLAYER_GUILD_BUY_PRICES_JSON] = "{}"
            prefs[KEY_MANAGERS_JSON] = "[]"
            prefs[KEY_AUCTIONS_JSON] = "[]"
            prefs[KEY_MUSEUM_HERITAGE_JSON] = "{}"
            prefs[KEY_IS_DATA_SAVED] = true
            prefs[KEY_LAST_SAVED_TIME] = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
            prefs[KEY_LAST_KNOWN_ELAPSED_REALTIME] = android.os.SystemClock.elapsedRealtime()
            prefs[KEY_SELECTED_THEME] = currentTheme
            prefs[KEY_SELECTED_LANGUAGE] = currentLang
            prefs[KEY_IS_EXPERT_MODE] = isExpert
            prefs[KEY_HAS_SET_WAREHOUSE] = false
            prefs[KEY_HAS_COMPLETED_FIRST_TRADE] = false
            prefs[KEY_IS_NEW_PLAYER_GUIDE_COMPLETED] = false
            prefs[KEY_IS_ENTREPRENEUR_GUIDE_COMPLETED] = false
        }
    }

}
