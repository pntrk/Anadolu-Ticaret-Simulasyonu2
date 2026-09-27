package com.example.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Global and standardized kotlinx.serialization Json parser instance.
 * Optimized for lenient parsing, ignoring unknown/new fields gracefully,
 * and high-throughput serialization.
 */
val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = false
    coerceInputValues = true
    allowSpecialFloatingPointValues = true
}

/**
 * Converts a generic kotlinx.serialization JsonElement tree to native Kotlin Any/Map/List/Primitives.
 */
fun jsonElementToAny(element: JsonElement): Any? {
    return when (element) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (element.isString) {
                element.content
            } else {
                element.booleanOrNull
                    ?: element.longOrNull
                    ?: element.doubleOrNull
                    ?: element.content
            }
        }
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonObject -> element.mapValues { jsonElementToAny(it.value) }
    }
}

/**
 * Converts native Kotlin Any/Map/List/Primitives into a kotlinx.serialization JsonElement tree.
 */
fun anyToJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to anyToJsonElement(v) })
        is Collection<*> -> JsonArray(value.map { anyToJsonElement(it) })
        is Array<*> -> JsonArray(value.map { anyToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }
}

// =========================================================================
// SUPABASE & PLAYER DTOs
// =========================================================================

@Serializable
data class SupabasePlayerDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "Tüccar",
    @SerialName("company_name") val companyName: String = "Tüccar Holding",
    @SerialName("money") val money: Long = 0L,
    @SerialName("loan_amount") val loanAmount: Long = 0L,
    @SerialName("deposit_balance") val depositBalance: Long = 0L,
    @SerialName("daily_income") val dailyIncome: Long = 0L,
    @SerialName("daily_expense") val dailyExpense: Long = 0L,
    @SerialName("total_profit") val totalProfit: Long = 0L,
    @SerialName("xp") val xp: Int = 0,
    @SerialName("level") val level: Int = 1,
    @SerialName("inventory_capacity") val inventoryCapacity: Int = 5000,
    @SerialName("current_city") val currentCity: String = "istanbul",
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("gems") val gems: Int = 0,
    @SerialName("last_daily_reward_ms") val lastDailyRewardMs: Long = 0L,
    @SerialName("login_streak") val loginStreak: Int = 0,
    @SerialName("dollar_balance") val dollarBalance: Long = 0L,
    @SerialName("dollar_deposit_balance") val dollarDepositBalance: Long = 0L,
    @SerialName("dollar_loan_amount") val dollarLoanAmount: Long = 0L,
    @SerialName("is_online_registered") val isOnlineRegistered: Boolean = true,
    @SerialName("online_email") val onlineEmail: String = "",
    @SerialName("businesses") val businesses: List<BusinessDto> = emptyList(),
    @SerialName("inventory") val inventory: List<InventoryDto> = emptyList(),
    @SerialName("managers") val managers: JsonElement? = null,
    @SerialName("active_researches") val activeResearches: JsonElement? = null,
    @SerialName("research_levels") val researchLevels: JsonElement? = null,
    @SerialName("guild_shares") val guildShares: JsonElement? = null,
    @SerialName("guild_buy_prices") val guildBuyPrices: JsonElement? = null,
    @SerialName("raw_save_json") val rawSaveJson: String? = null,
    @SerialName("monthly_growth") val monthlyGrowth: Long? = null
)

@Serializable
data class BusinessDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("type") val type: String = "",
    @SerialName("level") val level: Int = 1,
    @SerialName("cityId") val cityId: String = "istanbul",
    @SerialName("wearLevel") val wearLevel: Double = 0.0,
    @SerialName("wear_level") val wearLevelSnake: Double? = null,
    @SerialName("storageCapacity") val storageCapacity: Int = 2500,
    @SerialName("storage_capacity") val storageCapacitySnake: Int? = null,
    @SerialName("storedItemsJson") val storedItemsJson: String = "{}",
    @SerialName("stored_items_json") val storedItemsJsonSnake: String? = null,
    @SerialName("isUpgrading") val isUpgrading: Boolean = false,
    @SerialName("is_upgrading") val isUpgradingSnake: Boolean = false,
    @SerialName("upgradeEndTime") val upgradeEndTime: Long? = null,
    @SerialName("upgrade_end_time") val upgradeEndTimeSnake: Long? = null,
    @SerialName("isConstructing") val isConstructing: Boolean = false,
    @SerialName("is_constructing") val isConstructingSnake: Boolean = false,
    @SerialName("constructionEndTime") val constructionEndTime: Long? = null,
    @SerialName("construction_end_time") val constructionEndTimeSnake: Long? = null
) {
    val effectiveWearLevel: Double
        get() = wearLevelSnake ?: wearLevel

    val effectiveStorageCapacity: Int
        get() = storageCapacitySnake ?: storageCapacity

    val effectiveStoredItemsJson: String
        get() = storedItemsJsonSnake ?: storedItemsJson

    val effectiveUpgradeEndTime: Long?
        get() = upgradeEndTime ?: upgradeEndTimeSnake

    val effectiveConstructionEndTime: Long?
        get() = constructionEndTime ?: constructionEndTimeSnake

    val effectiveIsConstructing: Boolean
        get() = isConstructing || isConstructingSnake || (effectiveConstructionEndTime != null && (effectiveConstructionEndTime ?: 0L) > com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs())

    val effectiveIsUpgrading: Boolean
        get() = isUpgrading || isUpgradingSnake || (effectiveUpgradeEndTime != null && (effectiveUpgradeEndTime ?: 0L) > com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs())
}

@Serializable
data class InventoryDto(
    @SerialName("itemId") val itemId: String = "",
    @SerialName("quantity") val quantity: Int = 0,
    @SerialName("quality_level") val qualityLevel: Int = 1,
    @SerialName("quality_tier") val qualityTier: String = "star1"
)

@Serializable
data class DeliveryDto(
    @SerialName("id") val id: String = "",
    @SerialName("itemId") val itemId: String = "",
    @SerialName("quantity") val quantity: Int = 0,
    @SerialName("originCityId") val originCityId: String = "",
    @SerialName("destinationCityId") val destinationCityId: String = "",
    @SerialName("pricePerUnit") val pricePerUnit: Long = 0L,
    @SerialName("totalCost") val totalCost: Long = 0L,
    @SerialName("startTimeMs") val startTimeMs: Long = 0L,
    @SerialName("totalDurationMs") val totalDurationMs: Long = 60000L,
    @SerialName("quality_level") val qualityLevel: Int = 1
)

@Serializable
data class MarketPriceDto(
    @SerialName("item_id") val itemId: String = "",
    @SerialName("item_name") val itemName: String = "",
    @SerialName("base_price") val basePrice: Long = 0L,
    @SerialName("current_price") val currentPrice: Long = 0L,
    @SerialName("quality_level") val qualityLevel: Int = 1,
    @SerialName("price_multiplier") val priceMultiplier: Double = 1.0,
    @SerialName("calculated_price") val calculatedPrice: Long = 0L
)

@Serializable
data class MarketListingDto(
    @SerialName("id") val id: String = "",
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("item_id") val itemId: String = "",
    @SerialName("quantity") val quantity: Int = 0,
    @SerialName("price_per_unit") val pricePerUnit: Long = 0L,
    @SerialName("city") val city: String = "",
    @SerialName("quality_level") val qualityLevel: Int = 1,
    @SerialName("quality_tier") val qualityTier: String = "Standart",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GuildDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("leader_name") val leaderName: String = "",
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("mega_project_title") val megaProjectTitle: String = "",
    @SerialName("mega_project_target") val megaProjectTarget: Long = 0L,
    @SerialName("mega_project_current") val megaProjectCurrent: Long = 0L,
    @SerialName("mega_project_requirements") val megaProjectRequirements: Map<String, Int> = emptyMap(),
    @SerialName("mega_project_contributions") val megaProjectContributions: Map<String, Int> = emptyMap(),
    @SerialName("slot_quality_levels") val slotQualityLevels: Map<String, Int> = emptyMap(),
    @SerialName("average_craftsmanship_score") val averageCraftsmanshipScore: Double = 1.0,
    @SerialName("master_craftsmanship_tier") val masterCraftsmanshipTier: Int = 1,
    @SerialName("perk_description") val perkDescription: String = "",
    @SerialName("bank_balance") val bankBalance: Long = 0L,
    @SerialName("is_ipo_active") val isIpoActive: Boolean = false,
    @SerialName("public_share_percent") val publicSharePercent: Int = 20,
    @SerialName("target_product_name") val targetProductName: String = "",
    @SerialName("target_product_id") val targetProductId: String = "",
    @SerialName("warehouse_stock") val warehouseStock: Int = 0,
    @SerialName("total_items_produced") val totalItemsProduced: Int = 0,
    @SerialName("unit_batch_price") val unitBatchPrice: Long = 0L,
    @SerialName("current_stage") val currentStage: String = "",
    @SerialName("raw_project_json") val rawProjectJson: String = "",
    @SerialName("chat_json") val chatJson: String? = null
)

@Serializable
data class MuseumArtifactDto(
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("owner_name") val ownerName: String = "T.C. Kültür ve Turizm Bakanlığı",
    @SerialName("status") val status: String = "UNCLAIMED_TREASURY",
    @SerialName("active_auction_id") val activeAuctionId: String? = null,
    @SerialName("last_price") val lastPrice: Long = 0L,
    @SerialName("updated_at_ms") val updatedAtMs: Long = 0L,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MuseumAuctionDto(
    @SerialName("id") val id: String,
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("seller_id") val sellerId: String = "treasury",
    @SerialName("seller_name") val sellerName: String = "T.C. Kültür ve Turizm Bakanlığı",
    @SerialName("is_player_seller") val isPlayerSeller: Boolean = false,
    @SerialName("starting_bid") val startingBid: Long = 0L,
    @SerialName("current_highest_bid") val currentHighestBid: Long = 0L,
    @SerialName("current_highest_bidder_id") val currentHighestBidderId: String = "",
    @SerialName("current_highest_bidder_name") val currentHighestBidderName: String = "",
    @SerialName("buyout_price") val buyoutPrice: Long = 0L,
    @SerialName("ends_at_ms") val endsAtMs: Long = 0L,
    @SerialName("bid_count") val bidCount: Int = 0,
    @SerialName("created_at_ms") val createdAtMs: Long = 0L,
    @SerialName("is_settled") val isSettled: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ConsortiumChatMessageDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("message_text") val messageText: String,
    @SerialName("timestamp_ms") val timestampMs: Long,
    @SerialName("is_system_message") val isSystemMessage: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MacroEconomyDto(
    @SerialName("id") val id: String = "current_macro_state",
    @SerialName("cycle") val cycle: String = "RECOVERY",
    @SerialName("global_inflation_rate") val globalInflationRate: Double = 0.08,
    @SerialName("central_bank_interest_rate") val centralBankInterestRate: Double = 0.12,
    @SerialName("central_bank_loan_rate") val centralBankLoanRate: Double = 0.22,
    @SerialName("central_bank_deposit_rate") val centralBankDepositRate: Double = 0.10,
    @SerialName("market_liquidity_multiplier") val marketLiquidityMultiplier: Double = 1.0,
    @SerialName("pe_ratio_base") val peRatioBase: Double = 10.0,
    @SerialName("updated_at") val updatedAt: String? = null
)

// =========================================================================
// LOCAL STORAGE & SAVE DTOs
// =========================================================================

@Serializable
data class ManagerActionLogDto(
    @SerialName("id") val id: String = "",
    @SerialName("timestampMs") val timestampMs: Long = 0L,
    @SerialName("description") val description: String = "",
    @SerialName("financialImpact") val financialImpact: Long = 0L
)

@Serializable
data class CompanyManagerDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("specialty") val specialty: String = "",
    @SerialName("level") val level: Int = 1,
    @SerialName("dailySalary") val dailySalary: Long = 0L,
    @SerialName("efficiency") val efficiency: Double = 1.0,
    @SerialName("isHired") val isHired: Boolean = false,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("description") val description: String = "",
    @SerialName("actionLogs") val actionLogs: List<ManagerActionLogDto> = emptyList()
)

@Serializable
data class AuctionDto(
    @SerialName("id") val id: String = "",
    @SerialName("itemId") val itemId: String = "",
    @SerialName("quantity") val quantity: Int = 1,
    @SerialName("startingBid") val startingBid: Long = 0L,
    @SerialName("currentBid") val currentBid: Long = 0L,
    @SerialName("currentBidderId") val currentBidderId: String = "local",
    @SerialName("currentBidderName") val currentBidderName: String = "Siz",
    @SerialName("sellerId") val sellerId: String = "local",
    @SerialName("sellerName") val sellerName: String = "Tüccar",
    @SerialName("originCityId") val originCityId: String = "istanbul",
    @SerialName("expiresAt") val expiresAt: Long = 0L,
    @SerialName("createdAt") val createdAt: Long = 0L
)

@Serializable
data class MuseumRegistryItemDto(
    @SerialName("artifactId") val artifactId: String,
    @SerialName("ownerId") val ownerId: String? = null,
    @SerialName("ownerName") val ownerName: String = "T.C. Kültür ve Turizm Bakanlığı (Vakıflar Gn. Md.)",
    @SerialName("status") val status: String = "UNCLAIMED_TREASURY",
    @SerialName("activeAuctionId") val activeAuctionId: String? = null,
    @SerialName("lastPrice") val lastPrice: Long = 0L,
    @SerialName("certificateCode") val certificateCode: String = "",
    @SerialName("mintTotal") val mintTotal: Int = 1,
    @SerialName("updatedAtMs") val updatedAtMs: Long = 0L
)

@Serializable
data class MuseumAuctionItemDto(
    @SerialName("id") val id: String,
    @SerialName("artifactId") val artifactId: String,
    @SerialName("sellerId") val sellerId: String = "local_player",
    @SerialName("sellerName") val sellerName: String = "Gerçek Oyuncu",
    @SerialName("isPlayerSeller") val isPlayerSeller: Boolean = true,
    @SerialName("startingBid") val startingBid: Long = 0L,
    @SerialName("currentHighestBid") val currentHighestBid: Long = 0L,
    @SerialName("currentHighestBidderId") val currentHighestBidderId: String = "",
    @SerialName("currentHighestBidder") val currentHighestBidder: String = "",
    @SerialName("buyoutPrice") val buyoutPrice: Long = 0L,
    @SerialName("endsAtMs") val endsAtMs: Long = 0L,
    @SerialName("bidCount") val bidCount: Int = 0,
    @SerialName("createdAt") val createdAt: Long = 0L,
    @SerialName("isSettled") val isSettled: Boolean = false,
    @SerialName("lastBidTimeMs") val lastBidTimeMs: Long = 0L
)

@Serializable
data class RawSavePayloadDto(
    @SerialName("schemaVersion") val schemaVersion: Int = 2,
    @SerialName("saveTimestampMs") val saveTimestampMs: Long = 0L,
    @SerialName("selectedTheme") val selectedTheme: String = "",
    @SerialName("megaProjectsJson") val megaProjectsJson: String = "[]",
    @SerialName("lockedDepositBalance") val lockedDepositBalance: Long = 0L,
    @SerialName("lockedDepositStartTimeMs") val lockedDepositStartTimeMs: Long = 0L,
    @SerialName("lockedDepositDurationMs") val lockedDepositDurationMs: Long = 0L,
)

@Serializable
data class GoogleDriveMetadataDto(
    @SerialName("name") val name: String,
    @SerialName("parents") val parents: List<String>
)

@Serializable
data class GoogleDriveFileListDto(
    @SerialName("files") val files: List<GoogleDriveFileItemDto> = emptyList()
)

@Serializable
data class GoogleDriveFileItemDto(
    @SerialName("id") val id: String
)

@Serializable
data class SupabaseAuthResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("user") val user: SupabaseUserDto? = null
)

@Serializable
data class SupabaseUserDto(
    @SerialName("id") val id: String
)

@Serializable
data class SupabaseAuthIdTokenRequestDto(
    @SerialName("id_token") val idToken: String,
    @SerialName("provider") val provider: String = "google"
)

@Serializable
data class SupabaseAuthEmailRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class PhoenixChannelJoinDto(
    @SerialName("topic") val topic: String,
    @SerialName("event") val event: String,
    @SerialName("payload") val payload: PhoenixChannelJoinPayloadDto,
    @SerialName("ref") val ref: String
)

@Serializable
data class PhoenixChannelLeaveDto(
    @SerialName("topic") val topic: String,
    @SerialName("event") val event: String = "phx_leave",
    @SerialName("payload") val payload: Map<String, String> = emptyMap(),
    @SerialName("ref") val ref: String? = null
)

@Serializable
data class PhoenixChannelJoinPayloadDto(
    @SerialName("config") val config: PhoenixConfigDto
)

@Serializable
data class PhoenixBroadcastConfigDto(
    @SerialName("self") val self: Boolean = true,
    @SerialName("ack") val ack: Boolean = false
)

@Serializable
data class PhoenixPresenceConfigDto(
    @SerialName("key") val key: String = ""
)

@Serializable
data class PhoenixConfigDto(
    @SerialName("broadcast") val broadcast: PhoenixBroadcastConfigDto? = PhoenixBroadcastConfigDto(),
    @SerialName("presence") val presence: PhoenixPresenceConfigDto? = PhoenixPresenceConfigDto(),
    @SerialName("postgres_changes") val postgresChanges: List<PhoenixPostgresChangeDto>? = emptyList()
)

@Serializable
data class PhoenixPostgresChangeDto(
    @SerialName("event") val event: String = "*",
    @SerialName("schema") val schema: String = "public",
    @SerialName("table") val table: String
)

@Serializable
data class PhoenixHeartbeatDto(
    @SerialName("topic") val topic: String = "phoenix",
    @SerialName("event") val event: String = "heartbeat",
    @SerialName("payload") val payload: Map<String, String> = emptyMap(),
    @SerialName("ref") val ref: String
)

@Serializable
data class PhoenixBroadcastOutgoingDto(
    @SerialName("topic") val topic: String,
    @SerialName("event") val event: String = "broadcast",
    @SerialName("payload") val payload: PhoenixBroadcastPayloadDto,
    @SerialName("ref") val ref: String? = null
)

@Serializable
data class PhoenixBroadcastPayloadDto(
    @SerialName("type") val type: String = "broadcast",
    @SerialName("event") val event: String,
    @SerialName("payload") val payload: JsonElement
)

@Serializable
data class PhoenixIncomingMessageDto(
    @SerialName("event") val event: String? = null,
    @SerialName("topic") val topic: String? = null,
    @SerialName("payload") val payload: JsonElement? = null,
    @SerialName("ref") val ref: String? = null
)

@Serializable
data class LiveAuctionBidEventDto(
    @SerialName("auction_id") val auctionId: String,
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("new_bid_amount") val newBidAmount: Long,
    @SerialName("bidder_id") val bidderId: String,
    @SerialName("bidder_name") val bidderName: String,
    @SerialName("timestamp_ms") val timestampMs: Long = System.currentTimeMillis(),
    @SerialName("is_buyout") val isBuyout: Boolean = false
)

@Serializable
data class LiveAuctionListedEventDto(
    @SerialName("auction_id") val auctionId: String,
    @SerialName("artifact_id") val artifactId: String,
    @SerialName("artifact_name") val artifactName: String,
    @SerialName("seller_name") val sellerName: String,
    @SerialName("starting_bid") val startingBid: Long,
    @SerialName("ends_at_ms") val endsAtMs: Long,
    @SerialName("timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class LiveConsortiumCreatedEventDto(
    @SerialName("project_id") val projectId: String,
    @SerialName("consortium_name") val consortiumName: String,
    @SerialName("target_product_id") val targetProductId: String,
    @SerialName("leader_name") val leaderName: String,
    @SerialName("timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class LiveConsortiumActionEventDto(
    @SerialName("project_id") val projectId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("player_name") val playerName: String,
    @SerialName("move_type") val moveType: String,
    @SerialName("slot_index") val slotIndex: Int = -1,
    @SerialName("quantity_delivered") val quantityDelivered: Long = 0L,
    @SerialName("details") val details: String = "",
    @SerialName("timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class LiveMarketActionEventDto(
    @SerialName("action_type") val actionType: String, // "FUTURES_CREATED", "FUTURES_FULFILLED", "FUTURES_CANCELLED", "BUY_ORDER_CREATED", "BUY_ORDER_FULFILLED", "BUY_ORDER_CANCELLED", "LISTING_CREATED", "LISTING_BOUGHT", "LISTING_CANCELLED"
    @SerialName("id") val id: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("player_name") val playerName: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("price") val price: Long,
    @SerialName("city_id") val cityId: String = "",
    @SerialName("duration_days") val durationDays: Int = 0,
    @SerialName("timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class BorsaPriceItemDto(
    @SerialName("item_id") val itemId: String,
    @SerialName("origin_country") val originCountry: String = "Türkiye",
    @SerialName("origin_city_id") val originCityId: String = "istanbul",
    @SerialName("price") val price: Long,
    @SerialName("borsa_stock") val borsaStock: Long = com.example.data.MacroEconomyEngine.DEFAULT_BORSA_STOCK,
    @SerialName("is_usd") val isUsd: Boolean = false
)

@Serializable
data class LiveBorsaPricesSyncDto(
    @SerialName("prices") val prices: List<BorsaPriceItemDto>,
    @SerialName("source_player_id") val sourcePlayerId: String = "",
    @SerialName("timestamp_ms") val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class AcceptOfferRequestDto(
    @SerialName("offer_id") val offerId: String,
    @SerialName("buyer_uid") val buyerUid: String,
    @SerialName("raw_base_amount") val rawBaseAmount: Long,
    @SerialName("consortium_id") val consortiumId: String? = null
)

@Serializable
data class AcceptOfferResponseDto(
    @SerialName("success") val success: Boolean,
    @SerialName("final_amount") val finalAmount: Long,
    @SerialName("applied_consortium_bonus") val appliedConsortiumBonus: Float = 0.0f,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class GameSaveDataDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "Tüccar",
    @SerialName("companyName") val companyName: String = "Tüccar Holding",
    @SerialName("money") val money: Long = 0L,
    @SerialName("depositBalance") val depositBalance: Long = 0L,
    @SerialName("loanAmount") val loanAmount: Long = 0L,
    @SerialName("lockedDepositBalance") val lockedDepositBalance: Long = 0L,
    @SerialName("lockedDepositStartTimeMs") val lockedDepositStartTimeMs: Long = 0L,
    @SerialName("lockedDepositDurationMs") val lockedDepositDurationMs: Long = 0L,
    @SerialName("gems") val gems: Int = 0,
    @SerialName("xp") val xp: Int = 0,
    @SerialName("level") val level: Int = 1,
    @SerialName("dailyIncome") val dailyIncome: Long = 0L,
    @SerialName("dailyExpense") val dailyExpense: Long = 0L,
    @SerialName("totalProfit") val totalProfit: Long = 0L,
    @SerialName("inventoryCapacity") val inventoryCapacity: Int = 5000,
    @SerialName("currentCity") val currentCity: String = "istanbul",
    @SerialName("isVip") val isVip: Boolean = false,
    @SerialName("loginStreak") val loginStreak: Int = 0,
    @SerialName("lastDailyRewardMs") val lastDailyRewardMs: Long = 0L,
    @SerialName("businessesJson") val businessesJson: String = "[]",
    @SerialName("inventoryJson") val inventoryJson: String = "[]",
    @SerialName("managersJson") val managersJson: String = "[]",
    @SerialName("dailyQuestStateJson") val dailyQuestStateJson: String = "{}",
    @SerialName("activeResearchesJson") val activeResearchesJson: String = "{}",
    @SerialName("researchLevelsJson") val researchLevelsJson: String = "{}",
    @SerialName("guildSharesJson") val guildSharesJson: String = "{}",
    @SerialName("guildBuyPricesJson") val guildBuyPricesJson: String = "{}",
    @SerialName("megaProjectsJson") val megaProjectsJson: String = "[]",
    @SerialName("selectedTheme") val selectedTheme: String = "",
    @SerialName("schemaVersion") val schemaVersion: Int = 2,
    @SerialName("saveTimestampMs") val saveTimestampMs: Long = 0L,
    @SerialName("isMigratedToNoSql") val isMigratedToNoSql: Boolean = false,
    @SerialName("player") val player: PlayerSaveSubDto? = null
)

@Serializable
data class PlayerSaveSubDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "Tüccar",
    @SerialName("companyName") val companyName: String = "Tüccar Holding",
    @SerialName("money") val money: Long = 0L,
    @SerialName("depositBalance") val depositBalance: Long = 0L,
    @SerialName("loanAmount") val loanAmount: Long = 0L,
    @SerialName("lockedDepositBalance") val lockedDepositBalance: Long = 0L,
    @SerialName("lockedDepositStartTimeMs") val lockedDepositStartTimeMs: Long = 0L,
    @SerialName("lockedDepositDurationMs") val lockedDepositDurationMs: Long = 0L,
    @SerialName("gems") val gems: Int = 0,
    @SerialName("xp") val xp: Int = 0,
    @SerialName("level") val level: Int = 1,
    @SerialName("dailyIncome") val dailyIncome: Long = 0L,
    @SerialName("dailyExpense") val dailyExpense: Long = 0L,
    @SerialName("totalProfit") val totalProfit: Long = 0L,
    @SerialName("inventoryCapacity") val inventoryCapacity: Int = 5000,
    @SerialName("currentCity") val currentCity: String = "istanbul",
    @SerialName("isVip") val isVip: Boolean = false,
    @SerialName("loginStreak") val loginStreak: Int = 0,
    @SerialName("lastDailyRewardMs") val lastDailyRewardMs: Long = 0L
)

@Serializable
data class ConsortiumSlotDto(
    @SerialName("id") val id: String,
    @SerialName("slot_index") val slotIndex: Int,
    @SerialName("guild_id") val guildId: String? = null,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("price") val price: Long = 100000000L,
    @SerialName("is_purchased") val isPurchased: Boolean = false,
    @SerialName("purchased_at") val purchasedAt: String? = null
)

@Serializable
data class RpcSyncCurrenciesRequestDto(
    @SerialName("p_player_id") val playerId: String,
    @SerialName("p_money") val money: Long,
    @SerialName("p_deposit_balance") val depositBalance: Long,
    @SerialName("p_loan_amount") val loanAmount: Long,
    @SerialName("p_gems") val gems: Int
)

@Serializable
data class RpcConsortiumSlotRequestDto(
    @SerialName("p_slot_id") val slotId: String,
    @SerialName("p_guild_id") val guildId: String,
    @SerialName("p_owner_id") val ownerId: String,
    @SerialName("p_owner_name") val ownerName: String,
    @SerialName("p_price") val price: Long
)
