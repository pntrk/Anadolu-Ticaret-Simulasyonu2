package com.example.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.R

/**
 * Standard Trade Mode enum for transactional operations (Exchange, IPO, Holdings)
 */
enum class TradeMode {
    BUY,
    SELL
}

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String = "local_player",
    val name: String = "Tüccar",
    val money: Long = 100_000L,
    val xp: Int = 0,
    val level: Int = 1,
    val currentCity: String = "istanbul", // Default
    val isUsdAccount: Boolean = false,
    val loanAmount: Long = 0L,
    val interestRate: Float = 0.15f,
    val depositBalance: Long = 0L,
    val depositInterestRate: Float = 0.05f,
    val dailyIncome: Long = 0L,
    val dailyExpense: Long = 0L,
    val totalProfit: Long = 0L,
    val inventoryCapacity: Int = 5000,
    val isVip: Boolean = false,
    val gems: Int = 0,
    val lastDailyRewardMs: Long = 0L,
    val loginStreak: Int = 0,
    val lockedDepositBalance: Long = 0L,
    val lockedDepositStartTimeMs: Long = 0L,
    val lockedDepositDurationMs: Long = 0L,
    val dollarBalance: Long = 0L,
    val dollarDepositBalance: Long = 0L,
    val dollarLoanAmount: Long = 0L
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemId: String, // "olive", "wheat_star3", "seafood"
    val quantity: Int
) {
    val baseProductId: String
        get() = ItemQuality.extractBaseProductId(itemId)

    val quality: ItemQuality
        get() = ItemQuality.extractQuality(itemId)

    fun getCalculatedPrice(basePrice: Long): Long {
        return (basePrice * quality.priceMultiplier).toLong()
    }
}


@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "olive_farm", "wheat_farm", "port"
    val level: Int,
    val cityId: String,
    val wearLevel: Float = 0.0f, // 0.0f (0% wear / %100 bakımlı) to 1.0f (100% worn out / eskimiş)
    val storageCapacity: Int = 500, // Facility-specific storage capacity (Level 1: 500t -> Level 10: 5000t)
    val storedItemsJson: String = "{}", // JSON string storing map of productId -> quantity e.g. {"crude_oil": 500}
    val isUpgrading: Boolean = false,
    val upgradeEndTime: Long? = null,
    val isConstructing: Boolean = false,
    val constructionEndTime: Long? = null
) {
    fun getConstructionDurationMs(): Long {
        val product = Product.values().find { it.facilityId == type || it.id == type }
        return product?.getInitialConstructionDurationMs() ?: (10 * 60 * 1000L)
    }

    fun getConstructionDiamondCost(remainingMs: Long): Int {
        val product = Product.values().find { it.facilityId == type || it.id == type }
        return product?.getInitialConstructionDiamondCost(remainingMs) ?: 1
    }

    fun getUpgradeDurationMs(): Long = when (level) {
        1 -> 30 * 60 * 1000L // 30 mins
        2 -> 2 * 3600 * 1000L // 2 hours
        3 -> 6 * 3600 * 1000L // 6 hours
        4 -> 18 * 3600 * 1000L // 18 hours
        else -> 24 * 3600 * 1000L // 24 hours
    }

    fun getUpgradeDiamondCost(remainingMs: Long): Int {
        val baseGems = when (level) {
            1 -> 1
            2 -> 2
            3 -> 6
            4 -> 18
            else -> 24
        }
        val hoursLeft = kotlin.math.ceil(remainingMs / 3600_000.0).toInt().coerceAtLeast(1)
        return minOf(baseGems, hoursLeft).coerceAtLeast(1)
    }

    fun getEffectiveStorageCapacity(): Int = level.coerceIn(1, 10) * 500

    fun getStoredItemsMap(): Map<String, Int> {
        if (storedItemsJson.isBlank() || storedItemsJson == "{}") return emptyMap()
        return try {
            val jsonObj = org.json.JSONObject(storedItemsJson)
            val map = mutableMapOf<String, Int>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val valInt = jsonObj.optInt(key, 0)
                if (key.isNotBlank() && valInt > 0) {
                    map[key] = valInt
                }
            }
            map
        } catch (e: Exception) {
            try {
                val result = mutableMapOf<String, Int>()
                val clean = storedItemsJson.trim().removePrefix("{").removeSuffix("}")
                if (clean.isBlank()) return emptyMap()
                clean.split(",").forEach { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                        val value = parts[1].trim().toIntOrNull() ?: 0
                        if (key.isNotBlank() && value > 0) {
                            result[key] = value
                        }
                    }
                }
                result
            } catch (ex: Exception) {
                emptyMap()
            }
        }
    }

    fun getStoredTotalQuantity(): Int = getStoredItemsMap().values.sum()

    fun getRemainingStorageCapacity(): Int = (getEffectiveStorageCapacity() - getStoredTotalQuantity()).coerceAtLeast(0)

    fun withAddedItem(itemId: String, qty: Int): BusinessEntity {
        if (qty <= 0) return this
        val map = getStoredItemsMap().toMutableMap()
        val current = map[itemId] ?: 0
        map[itemId] = current + qty
        val json = mapToJson(map)
        return this.copy(storedItemsJson = json)
    }

    fun withAddedItemWithQuality(productId: String, quality: ItemQuality, qty: Int): BusinessEntity {
        val key = ItemQuality.makeKey(productId, quality)
        return withAddedItem(key, qty)
    }

    fun withRemovedItem(itemId: String, qty: Int): BusinessEntity {
        if (qty <= 0) return this
        val map = getStoredItemsMap().toMutableMap()
        val current = map[itemId] ?: 0
        val remaining = current - qty
        if (remaining > 0) {
            map[itemId] = remaining
        } else {
            map.remove(itemId)
        }
        val json = mapToJson(map)
        return this.copy(storedItemsJson = json)
    }

    fun withClearedItems(): BusinessEntity {
        return this.copy(storedItemsJson = "{}")
    }

    private fun mapToJson(map: Map<String, Int>): String {
        if (map.isEmpty()) return "{}"
        val entries = map.entries.map { "\"${it.key}\":${it.value}" }
        return "{${entries.joinToString(",")}}"
    }
}

@kotlinx.serialization.Serializable
data class DeliveryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemId: String,
    val quantity: Int,
    val originCityId: String,
    val destinationCityId: String,
    val pricePerUnit: Long,
    val totalCost: Long,
    val startTimeMs: Long = System.currentTimeMillis(),
    val totalDurationMs: Long,
    val progress: Float = 0.0f,
    val isOutboundSale: Boolean = false,
    val isConsortiumDelivery: Boolean = false
)

@kotlinx.serialization.Serializable
enum class ProductionStatus {
    IN_PROGRESS,
    COMPLETED
}

@kotlinx.serialization.Serializable
data class ActiveProduction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val productId: String,
    val quantity: Int = 1,
    val facilityId: String = "",
    val businessId: Int = 0,
    val cityId: String = "istanbul",
    val targetCityId: String = "istanbul",
    val startTimeMs: Long = System.currentTimeMillis(),
    val totalDurationMs: Long = 10_000L,
    val endTimeMs: Long = 0L,
    @kotlinx.serialization.SerialName("end_time_ms") val endTimeMsSnake: Long? = null,
    val isAgriProduct: Boolean = false,
    val originCountry: String = "Türkiye",
    val usedIngredientQualityStars: Double = 1.0
) {
    val effectiveEndTimeMs: Long
        get() = when {
            endTimeMs > 0L -> endTimeMs
            (endTimeMsSnake ?: 0L) > 0L -> endTimeMsSnake!!
            else -> startTimeMs + totalDurationMs
        }

    fun getProgress(now: Long = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()): Float {
        val total = if (totalDurationMs > 0L) totalDurationMs else (effectiveEndTimeMs - startTimeMs).coerceAtLeast(1L)
        val elapsed = (now - startTimeMs).coerceAtLeast(0L)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun isCompleted(now: Long = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()): Boolean {
        return now >= effectiveEndTimeMs
    }

    fun getRemainingTimeMs(now: Long = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()): Long {
        return (effectiveEndTimeMs - now).coerceAtLeast(0L)
    }

    val status: ProductionStatus
        get() = if (isCompleted()) ProductionStatus.COMPLETED else ProductionStatus.IN_PROGRESS

    val isInProgress: Boolean
        get() = status == ProductionStatus.IN_PROGRESS

    val isPendingProduction: Boolean
        get() = status == ProductionStatus.IN_PROGRESS
}

@Entity(tableName = "market_prices", primaryKeys = ["itemId", "originCountry"])
data class MarketPriceEntity(
    val itemId: String,
    val originCountry: String = "Global", // Unified Global Exchange
    val originCityId: String = "new_york", // Global New York Borsası Ana Merkezi & Deposu
    val price: Long,
    val borsaStock: Long = MacroEconomyEngine.DEFAULT_BORSA_STOCK,
    val isUsd: Boolean = false
) {
    val effectiveIsUsd: Boolean
        get() = false

    val isCrisis: Boolean
        get() = borsaStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD

    val countryFlag: String
        get() = "₳"
}

fun sanitizeMarketPrices(
    prices: List<MarketPriceEntity>,
    businesses: List<BusinessEntity> = emptyList()
): List<MarketPriceEntity> {
    val result = mutableListOf<MarketPriceEntity>()
    val priceMap = prices.associateBy { it.itemId }
    
    Product.values().forEach { product ->
        val existing = priceMap[product.id]
        
        val rawBasePrice = product.basePrice.coerceAtLeast(10L)
        // Eğer stok tanımlı değilse, <= 0L veya eski 999.999.999L / 50.000L kalıntısı ise standart 999.999L stok ile başlatılır.
        val finalStock = if (existing != null && existing.borsaStock > 0L && existing.borsaStock != 999_999_999L && existing.borsaStock != 5000L && existing.borsaStock != 50_000L) {
            existing.borsaStock
        } else {
            MacroEconomyEngine.DEFAULT_BORSA_STOCK
        }
        
        // Fiyatlama HER ZAMAN güncel borsa stok seviyesine göre dinamik hesaplanır (Stok düştükçe fiyat yükselir, stok arttıkça düşer)
        val dynamicPrice = MacroEconomyEngine.calculatePriceFromStock(finalStock, rawBasePrice)
        
        result.add(
            MarketPriceEntity(
                itemId = product.id,
                originCountry = "Global",
                originCityId = "new_york",
                price = dynamicPrice,
                borsaStock = finalStock,
                isUsd = false
            )
        )
    }
    return result
}

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: String = "global_state",
    val season: String = "İlkbahar",
    val activeEvent: String = "Normal",
    val globalInflationRate: Float = 0.0f,
    val centralBankLoanRate: Float = 0.15f,
    val centralBankDepositRate: Float = 0.05f,
    val totalMarketLiquidity: Long = 10000000L,
    val usdTryRate: Double = 50.0
)

data class MarketListing(
    val id: String = "",
    val sellerName: String = "",
    val sellerId: String = "",
    val itemId: String = "", // "olive", "wheat", etc.
    val quantity: Int = 0,
    val pricePerUnit: Long = 0L,
    val originCityId: String = "",
    val qualityLevel: Int = 1,
    val qualityTier: String = "Standart",
    val createdAt: Long = System.currentTimeMillis()
) {
    val quality: ItemQuality
        get() = if (itemId.contains("_star")) ItemQuality.extractQuality(itemId) else ItemQuality.fromStars(qualityLevel)

    val baseProductId: String
        get() = ItemQuality.extractBaseProductId(itemId)

    val effectiveInventoryKey: String
        get() = ItemQuality.makeKey(baseProductId, quality)

    val isBotListing: Boolean
        get() = sellerId.startsWith("BOT-") || id.startsWith("BOT_LISTING_")

    val remainingMs: Long
        get() {
            val totalDurationMs = 24 * 60 * 60 * 1000L // 24 Saat
            val elapsed = System.currentTimeMillis() - createdAt
            return (totalDurationMs - elapsed).coerceAtLeast(0L)
        }

    val isExpired: Boolean
        get() = isBotListing && remainingMs <= 0L
}

data class FuturesContract(
    val id: String = "",
    val creatorName: String = "",
    val creatorId: String = "",
    val itemId: String = "",
    val quantity: Int = 0,
    val lockedPricePerUnit: Long = 0L,
    val durationDays: Int = 30,
    val cityId: String = "",
    val qualityLevel: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val isFulfilled: Boolean = false
) {
    val quality: ItemQuality
        get() = ItemQuality.fromStars(qualityLevel)
}

data class BuyOrder(
    val id: String = "",
    val buyerName: String = "",
    val buyerId: String = "",
    val itemId: String = "",
    val quantity: Int = 0,
    val pricePerUnit: Long = 0L,
    val destinationCityId: String = "",
    val qualityLevel: Int = 1,
    val minQualityLevel: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    val quality: ItemQuality
        get() = ItemQuality.fromStars(qualityLevel)
}

data class Auction(
    val id: String = "",
    val sellerName: String = "",
    val sellerId: String = "",
    val itemId: String = "",
    val quantity: Int = 0,
    val startingBid: Long = 0L,
    val currentBid: Long = 0L,
    val currentBidderId: String = "",
    val currentBidderName: String = "",
    val originCityId: String = "",
    val qualityLevel: Int = 1,
    val expiresAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val quality: ItemQuality
        get() = ItemQuality.fromStars(qualityLevel)
}

data class OutbidAlertData(
    val auctionId: String,
    val itemName: String,
    val newBidAmount: Long,
    val bidderName: String
)

data class GuildGroup(
    val id: String = "",
    val name: String = "",
    val leaderName: String = "",
    val memberCount: Int = 1,
    val megaProjectTitle: String = "",
    @StringRes val megaProjectTitleRes: Int = 0,
    val megaProjectTarget: Long = 0L,
    val megaProjectCurrent: Long = 0L,
    val perkDescription: String = "",
    @StringRes val perkDescriptionRes: Int = 0,
    val isJoined: Boolean = false,
    val megaProjectRequirements: Map<String, Int> = mapOf("cement" to 500000, "steel" to 250000, "aluminum" to 100000),
    val megaProjectContributions: Map<String, Int> = mapOf("cement" to 120000, "steel" to 80000, "aluminum" to 30000),
    val slotQualityLevels: Map<String, Int> = emptyMap(),
    val averageCraftsmanshipScore: Double = 1.0,
    val masterCraftsmanshipTier: Int = 1,
    val bankBalance: Long = 0L,
    val isIpoActive: Boolean = false,
    val publicSharePercent: Int = 20,
    val targetProductName: String = "",
    @StringRes val targetProductNameRes: Int = 0,
    val targetProductId: String = "",
    val warehouseStock: Int = 0,
    val totalItemsProduced: Int = 0,
    val unitBatchPrice: Long = 0L,
    val currentStage: String = "",
    val cityId: String = "istanbul",
    val rawProjectJson: String = ""
)

@Entity(tableName = "pending_sales")
data class PendingMarketSaleEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val sellerName: String,
    val sellerId: String,
    val itemId: String,
    val quantity: Int,
    val pricePerUnit: Long,
    val originCityId: String,
    val qualityLevel: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 1-of-1 Unique Global Museum Artifact Registry Entity.
 * In the entire game database, exactly 1 instance of each artifact exists globally.
 */
@Entity(tableName = "museum_artifacts")
data class MuseumArtifactOwnershipEntity(
    @PrimaryKey val artifactId: String, // e.g., "art_seljuk_gold_dinar"
    val ownerId: String?, // Player UID if owned by a real player; null if unowned/in treasury
    val ownerName: String, // Name of the owner / company
    val status: String, // "OWNED_BY_PLAYER", "ON_AUCTION", "UNCLAIMED_TREASURY"
    val activeAuctionId: String? = null,
    val lastPrice: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
)

/**
 * Real Player Museum Auction Entity.
 * Only real players can place bids or buy out. No bots.
 */
@Entity(tableName = "museum_auctions")
data class MuseumAuctionEntity(
    @PrimaryKey val id: String,
    val artifactId: String,
    val sellerId: String,
    val sellerName: String,
    val isPlayerSeller: Boolean,
    val startingBid: Long,
    val currentHighestBid: Long,
    val currentHighestBidderId: String,
    val currentHighestBidderName: String,
    val buyoutPrice: Long,
    val endsAtMs: Long,
    val bidCount: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
)


@kotlinx.serialization.Serializable data class ManagerActionLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long,
    val description: String,
    val financialImpact: Long
)


@kotlinx.serialization.Serializable data class CompanyManager(
    val id: String,
    val name: String,
    val title: String,
    @StringRes val titleRes: Int = 0,
    val specialty: String, // "production", "logistics", "maintenance", "contracts", "rd", "borsa", "treasury"
    val level: Int = 1, // 1 to 5 stars
    val dailySalary: Long,
    val efficiency: Float = 1.0f,
    val isHired: Boolean = false,
    val isActive: Boolean = true,
    val description: String,
    @StringRes val descriptionRes: Int = 0,
    val actionLogs: List<ManagerActionLog> = emptyList()
) {
    val hireGemCost: Int
        get() = getManagerHiringGemCost(id)
}

fun getManagerHiringGemCost(managerId: String): Int {
    return when (managerId) {
        "mgr_treasury" -> 50     // Hiyerarşi #1: Finans & Hazine Lideri
        "mgr_contracts" -> 40    // Hiyerarşi #2: Vadeli B2B & Tedarik Yetkilisi
        "mgr_borsa" -> 35        // Hiyerarşi #3: Borsa & Hisse Analisti
        "mgr_logistics" -> 30    // Hiyerarşi #4: Lojistik & Satış Müdürü
        "mgr_hr" -> 25           // Hiyerarşi #5: İnsan Kaynakları Müdürü
        "mgr_rd" -> 20           // Hiyerarşi #5: Ar-Ge Müdürü
        "mgr_prod" -> 15         // Hiyerarşi #5: Üretim & Operasyon Müdürü
        "mgr_maintenance" -> 10  // Hiyerarşi #5: Tesis Bakım Müdürü
        else -> 15
    }
}

fun getDefaultCompanyManagers(): List<CompanyManager> = listOf(
    CompanyManager(
        id = "mgr_treasury",
        name = "Ayşe Kaya, CFA",
        title = "Hazine ve Makroekonomi Müdürü",
        titleRes = R.string.mgr_treasury_title,
        specialty = "treasury",
        level = 1,
        dailySalary = 20000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #1: Finans Lideri. Müdürlerin konsorsiyum operasyonları, borsa tedarikleri ve bakım masrafları için banka mevduatından nakit sağlar ve holding likiditesini finanse eder.",
        descriptionRes = R.string.mgr_treasury_desc
    ),
    CompanyManager(
        id = "mgr_contracts",
        name = "Canan Çelik",
        title = "Vadeli Sözleşme ve Tedarik Müdürü",
        titleRes = R.string.mgr_contracts_title,
        specialty = "contracts",
        level = 1,
        dailySalary = 16000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #2: Tedarik Yetkilisi. Konsorsiyum üretimi ve teslimatı için gerekli malzemeleri pazardan en uygun fiyata otomatik bulur, bulamazsa borsa vadeli sözleşmesiyle tedarik eder.",
        descriptionRes = R.string.mgr_contracts_desc
    ),
    CompanyManager(
        id = "mgr_borsa",
        name = "Burak Koç",
        title = "Borsa ve Yatırım Analisti",
        titleRes = R.string.mgr_borsa_title,
        specialty = "borsa",
        level = 1,
        dailySalary = 15000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #3: Yatırım Yetkilisi. Konsorsiyum için gereken tedarik malzemesi depoda yoksa Tedarik Müdürü ile koordineli çalışarak borsadan spot alım yapar veya vadeli talep açar.",
        descriptionRes = R.string.mgr_borsa_desc
    ),
    CompanyManager(
        id = "mgr_logistics",
        name = "Zeynep Demir",
        title = "Lojistik ve Depo Müdürü",
        titleRes = R.string.mgr_logistics_title,
        specialty = "logistics",
        level = 1,
        dailySalary = 13000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #4: Lojistik ve Depo Müdürü. Tesis ambarlarında üretilen ürünleri otomatik olarak merkez depoya sevk eder; konsorsiyum teslimatlarını yapar ve ihtiyaç fazlası ürünleri satarak nakit sağlar.",
        descriptionRes = R.string.mgr_logistics_desc
    ),
    CompanyManager(
        id = "mgr_hr",
        name = "Banu Aydın, MBA",
        title = "İnsan Kaynakları ve Operasyon Müdürü",
        titleRes = R.string.mgr_hr_title,
        specialty = "hr",
        level = 1,
        dailySalary = 12000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #5: İK ve Operasyon Lideri. Tesis depolarındaki ürünlerin merkez depoya otomatik aktarılmasını koordine eder; konsorsiyum ihtiyaçlarını gözetmeyen müdürleri hizalar, terfi ve performans yönetimini yürütür.",
        descriptionRes = R.string.mgr_hr_desc
    ),
    CompanyManager(
        id = "mgr_rd",
        name = "Dr. Selim Arslan",
        title = "Araştırma ve Geliştirme Müdürü",
        titleRes = R.string.mgr_rd_title,
        specialty = "rd",
        level = 1,
        dailySalary = 11000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #5: Ar-Ge Müdürü. Öncelikle konsorsiyum projeleri ve endüstriyel üretim hatları için kritik olan teknolojik araştırmaları geliştirir.",
        descriptionRes = R.string.mgr_rd_desc
    ),
    CompanyManager(
        id = "mgr_prod",
        name = "Ahmet Yılmaz",
        title = "Üretim ve Operasyon Müdürü",
        titleRes = R.string.mgr_prod_title,
        specialty = "production",
        level = 1,
        dailySalary = 10400L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #5: Operasyon Müdürü. Oyuncunun tesislerinde öncelikli olarak konsorsiyum mega projeleri ve teslimat yuvaları için gereken ürünleri otomatik olarak üretir.",
        descriptionRes = R.string.mgr_prod_desc
    ),
    CompanyManager(
        id = "mgr_maintenance",
        name = "Mehmet Öz",
        title = "Tesis Bakım Müdürü",
        titleRes = R.string.mgr_maintenance_title,
        specialty = "maintenance",
        level = 1,
        dailySalary = 10000L,
        efficiency = 1.0f,
        isHired = false,
        isActive = true,
        description = "Hiyerarşi #5: Bakım Yetkilisi. Yıpranan tesisleri otomatik tespit edip onarır; üretimin ve konsorsiyum tedarikinin kesintiye uğramasını önler.",
        descriptionRes = R.string.mgr_maintenance_desc
    )
)

@kotlinx.serialization.Serializable
data class GrowthPointDto(
    val timestampMs: Long = System.currentTimeMillis(),
    val dayLabel: String = "",
    val netWorth: Double = 0.0,
    val cashBalance: Double = 0.0,
    val totalAssets: Double = 0.0,
    val depositBalance: Double = 0.0,
    val facilityValuation: Double = 0.0,
    val inventoryValuation: Double = 0.0,
    val consortiumValuation: Double = 0.0
)

