package com.example.data

import android.util.Log
import com.example.ui.components.NotificationType
import com.example.ui.components.ParticleManager
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.formatMoney
import com.example.utils.HapticManager
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.onBorsaItemBought
import com.example.viewmodel.onBorsaItemSold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * 5 Karakterli Canlı Bot Tycoon Ekosistemi
 * Her botun kendine has şirketi, uzmanlık alanı, üretim hattı, pazar alış/satış stratejisi,
 * konsorsiyum katılımı ve Ar-Ge odağı bulunmaktadır.
 */
data class BotTycoonProfile(
    val id: String,
    val name: String,
    val companyName: String,
    val cityId: String,
    val cityName: String,
    val badge: String,
    val avatarColor: Long,
    val specialtyTr: String,
    val specialtyEn: String,
    var level: Int,
    var xp: Int,
    var netWorth: Long,
    var currentRdTitleTr: String,
    var currentRdTitleEn: String,
    var rdProgressPercent: Int,
    val buyProductIds: List<String>,      // Pazardan satın alacağı ürünler (Hammadde/Girdi)
    val sellProductIds: List<String>,     // Pazara ilan vereceği ürünler (Ürettiği Çıktılar)
    val facilities: List<PlayerFacilityInfo>,
    var completedTradeCount: Int = 0,
    var totalMoneySpentInMarket: Long = 0L
)

object BotTycoonManager {
    private const val TAG = "BotTycoonManager"

    // 5 Ana Bot Oyuncu Profili
    private val botList = listOf(
        BotTycoonProfile(
            id = "BOT-KAYA-01",
            name = "Selim Kaya",
            companyName = "Kaya Ağır Sanayi & Metalurji A.Ş.",
            cityId = "karabuk",
            cityName = "Karabük",
            badge = "SANAYİCİ BARON",
            avatarColor = 0xFF546E7A,
            specialtyTr = "Ağır Sanayi, Çelik Döküm & Otomotiv Sacı",
            specialtyEn = "Heavy Industry, Steel Casting & Automotive Metal",
            level = 18,
            xp = 52400,
            netWorth = 16800000L,
            currentRdTitleTr = "Yüksek Mukavemetli Çelik Alaşımları",
            currentRdTitleEn = "High-Tensile Steel Alloys",
            rdProgressPercent = 75,
            buyProductIds = listOf("iron", "coal", "limestone", "scrap_metal"),
            sellProductIds = listOf("steel", "auto_part", "building_block", "machinery"),
            facilities = listOf(
                PlayerFacilityInfo("Karabük Entegre Yüksek Fırınları", "Karabük", 4, "Ağır Metal"),
                PlayerFacilityInfo("İzmir Çelik Haddehanesi", "İzmir", 3, "Metal"),
                PlayerFacilityInfo("Bursa Otomotiv Pres & Döküm", "Bursa", 3, "Otomotiv")
            )
        ),
        BotTycoonProfile(
            id = "BOT-NOVA-02",
            name = "Dr. Aylin Soylu",
            companyName = "Nova Tech Dynamics & Çip A.Ş.",
            cityId = "ankara",
            cityName = "Ankara",
            badge = "TEKNOLOJİ ÖNCÜSÜ",
            avatarColor = 0xFF00E5FF,
            specialtyTr = "Mikroçip, Robotik Kol & İleri Batarya Sistemleri",
            specialtyEn = "Microchips, Robotic Arms & Advanced Battery Systems",
            level = 21,
            xp = 64200,
            netWorth = 22400000L,
            currentRdTitleTr = "Kuantum Çip Mimarisi & Otonom Sürü",
            currentRdTitleEn = "Quantum Chip Architecture & Autonomous Swarm",
            rdProgressPercent = 88,
            buyProductIds = listOf("silicon", "copper", "lithium", "chemicals"),
            sellProductIds = listOf("chip", "robotics_arm", "battery", "smartphone", "ai_datacenter"),
            facilities = listOf(
                PlayerFacilityInfo("ODTÜ Teknokent Silikon Laboratuvarı", "Ankara", 4, "Yüksek Teknoloji"),
                PlayerFacilityInfo("İstanbul Mikroçip Fabrikası", "İstanbul", 4, "Elektronik"),
                PlayerFacilityInfo("Kocaeli Batarya & Güç İstasyonu", "Kocaeli", 3, "Enerji")
            )
        ),
        BotTycoonProfile(
            id = "BOT-TOROS-03",
            name = "Burak Demirci",
            companyName = "Toros Petrokimya & Enerji Holding",
            cityId = "adana",
            cityName = "Adana",
            badge = "ENERJİ LİDERİ",
            avatarColor = 0xFFFF7043,
            specialtyTr = "Petrokimya, Sanayi Polimeri & Yeşil Enerji",
            specialtyEn = "Petrochemicals, Industrial Polymers & Green Energy",
            level = 16,
            xp = 46800,
            netWorth = 13900000L,
            currentRdTitleTr = "Biyo-Bozunur Polimer Sentezi",
            currentRdTitleEn = "Biodegradable Polymer Synthesis",
            rdProgressPercent = 60,
            buyProductIds = listOf("crude_oil", "natural_gas", "chemicals"),
            sellProductIds = listOf("plastic", "refined_fuel", "petrochem", "liquefied_gas", "hydrogen_plant"),
            facilities = listOf(
                PlayerFacilityInfo("Ceyhan Petrol Rafinerisi", "Adana", 4, "Petrokimya"),
                PlayerFacilityInfo("Mersin Polimer & Plastik Tesisi", "Mersin", 3, "Kimya"),
                PlayerFacilityInfo("Batman Ham Petrol Depolama", "Batman", 2, "Enerji")
            )
        ),
        BotTycoonProfile(
            id = "BOT-EGE-04",
            name = "Zehra Aydın",
            companyName = "Ege Global Tarım & Elyaf Entegre",
            cityId = "izmir",
            cityName = "İzmir",
            badge = "İHRACAT ŞAMPİYONU",
            avatarColor = 0xFF66BB6A,
            specialtyTr = "Tekstil, Dokuma, Zeytinyağı & Paketleme",
            specialtyEn = "Textiles, Weaving, Olive Oil & Packaging",
            level = 14,
            xp = 39100,
            netWorth = 11200000L,
            currentRdTitleTr = "Akıllı Nanoteknolojik Kumaş Lifleri",
            currentRdTitleEn = "Smart Nanotech Fabric Fibers",
            rdProgressPercent = 82,
            buyProductIds = listOf("cotton", "timber", "rubber_latex"),
            sellProductIds = listOf("fabric", "clothing", "packaging", "furniture"),
            facilities = listOf(
                PlayerFacilityInfo("İzmir Modern Dokuma & İplik", "İzmir", 3, "Tekstil"),
                PlayerFacilityInfo("Denizli Konfeksiyon Fabrikası", "Denizli", 3, "Hazır Giyim"),
                PlayerFacilityInfo("Aydın Ambalaj & Kutu Entegre", "Aydın", 2, "Paketleme")
            )
        ),
        BotTycoonProfile(
            id = "BOT-AVRASYA-05",
            name = "Hakan Erkin",
            companyName = "Avrasya Savunma, Havacılık & Lojistik",
            cityId = "eskisehir",
            cityName = "Eskişehir",
            badge = "SAVUNMA & HAVACILIK",
            avatarColor = 0xFFBA68C8,
            specialtyTr = "Titanyum Alaşım, Havacılık Motoru & Kompozit",
            specialtyEn = "Titanium Alloy, Aero Engines & Composites",
            level = 19,
            xp = 58900,
            netWorth = 19500000L,
            currentRdTitleTr = "Hipersonik İtki & Karbon Kompozit Panel",
            currentRdTitleEn = "Hypersonic Propulsion & Carbon Composite Panels",
            rdProgressPercent = 90,
            buyProductIds = listOf("aluminum", "titanium", "graphite_ore"),
            sellProductIds = listOf("titanium_alloy", "carbon_fiber", "turbine_engine", "uav", "composite_structure"),
            facilities = listOf(
                PlayerFacilityInfo("Eskişehir Havacılık & Türbin Fabrikası", "Eskişehir", 4, "Havacılık"),
                PlayerFacilityInfo("Kayseri Kompozit & Gövde Atölyesi", "Kayseri", 3, "Savunma"),
                PlayerFacilityInfo("Konya Ağır Lojistik İkmal Merkezi", "Konya", 3, "Lojistik")
            )
        ),
        // --- 5 YENİ TIER-1 AĞIRLIKLI BOT OYUNCU ---
        BotTycoonProfile(
            id = "BOT-MADEN-06",
            name = "Cevdet Madenci",
            companyName = "Anadolu Maden & Kömür İşletmeleri A.Ş.",
            cityId = "zonguldak",
            cityName = "Zonguldak",
            badge = "MADEN KRALI",
            avatarColor = 0xFF78909C,
            specialtyTr = "Kömür, Demir, Kireçtaşı & Ağır Cevher Madenciliği",
            specialtyEn = "Coal, Iron, Limestone & Heavy Ores",
            level = 15,
            xp = 42000,
            netWorth = 12500000L,
            currentRdTitleTr = "Derin Damar Kömür & Manyetik Demir Ayrıştırma",
            currentRdTitleEn = "Deep Vein Coal & Magnetic Iron Separation",
            rdProgressPercent = 55,
            buyProductIds = listOf("coal", "iron", "timber", "chemicals"),
            sellProductIds = listOf("coal", "iron", "limestone", "steel"),
            facilities = listOf(
                PlayerFacilityInfo("Zonguldak Taşkömürü Ocağı", "Zonguldak", 4, "Maden"),
                PlayerFacilityInfo("Divriği Demir Madeni", "Sivas", 3, "Maden"),
                PlayerFacilityInfo("Konya Kireçtaşı Taşocağı", "Konya", 2, "Taşocağı")
            )
        ),
        BotTycoonProfile(
            id = "BOT-BAKIR-07",
            name = "Meltem Karahan",
            companyName = "Küre Bakır & Metalurji Holding",
            cityId = "kastamonu",
            cityName = "Kastamonu",
            badge = "BAKIR & METAL BARONU",
            avatarColor = 0xFFFF7043,
            specialtyTr = "Bakır, Alüminyum & İletken Madenleri",
            specialtyEn = "Copper, Aluminum & Conductive Ores",
            level = 17,
            xp = 48500,
            netWorth = 15100000L,
            currentRdTitleTr = "Yüksek İletkenlikli Bakır Flotasyonu",
            currentRdTitleEn = "High Conductivity Copper Flotation",
            rdProgressPercent = 68,
            buyProductIds = listOf("copper", "aluminum", "chemicals", "scrap_metal"),
            sellProductIds = listOf("copper", "aluminum", "wire"),
            facilities = listOf(
                PlayerFacilityInfo("Küre Bakır Zenginleştirme Tesisi", "Kastamonu", 4, "Maden"),
                PlayerFacilityInfo("Seydişehir Alüminyum Fabrikası", "Konya", 3, "Metal"),
                PlayerFacilityInfo("Denizli Bakır Tel Dökümhanesi", "Denizli", 2, "Metalurji")
            )
        ),
        BotTycoonProfile(
            id = "BOT-ORMAN-08",
            name = "Kemal Ormancı",
            companyName = "Karadeniz Kereste & Kauçuk Sanayi",
            cityId = "trabzon",
            cityName = "Trabzon",
            badge = "ORMAN & KERESTE LİDERİ",
            avatarColor = 0xFF8D6E63,
            specialtyTr = "Kereste, Doğal Kauçuk & Selüloz Lifleri",
            specialtyEn = "Timber, Natural Rubber & Cellulose Fibers",
            level = 13,
            xp = 36200,
            netWorth = 9800000L,
            currentRdTitleTr = "Sürdürülebilir Orman Hasatı & Kauçuk Polimerizasyonu",
            currentRdTitleEn = "Sustainable Forest Harvesting & Rubber Polymerization",
            rdProgressPercent = 42,
            buyProductIds = listOf("timber", "rubber_latex", "chemicals"),
            sellProductIds = listOf("timber", "rubber_latex", "packaging", "furniture"),
            facilities = listOf(
                PlayerFacilityInfo("Trabzon Orman Kereste Kampları", "Trabzon", 3, "Ormancılık"),
                PlayerFacilityInfo("Rize Doğal Kauçuk Plantasyonu", "Rize", 3, "Tarım"),
                PlayerFacilityInfo("Bolu Masif Ahşap Atölyesi", "Bolu", 2, "Ağaç İşleri")
            )
        ),
        BotTycoonProfile(
            id = "BOT-PAMUK-09",
            name = "Fatma Güneydoğu",
            companyName = "Harran Tarım & Doğal Pamuk Entegre",
            cityId = "sanliurfa",
            cityName = "Şanlıurfa",
            badge = "BEYAZ ALTIN ÖNCÜSÜ",
            avatarColor = 0xFFE0E0E0,
            specialtyTr = "Ham Pamuk, Doğal Lif & İplik Hammaddeleri",
            specialtyEn = "Raw Cotton, Natural Fibers & Yarn Materials",
            level = 16,
            xp = 44900,
            netWorth = 14200000L,
            currentRdTitleTr = "Kuraklığa Dayanıklı Organik Pamuk Islahı",
            currentRdTitleEn = "Drought-Resistant Organic Cotton Breeding",
            rdProgressPercent = 78,
            buyProductIds = listOf("cotton", "chemicals", "rubber_latex"),
            sellProductIds = listOf("cotton", "fabric", "clothing"),
            facilities = listOf(
                PlayerFacilityInfo("Harran Ovası Pamuk Çiftlikleri", "Şanlıurfa", 4, "Tarım"),
                PlayerFacilityInfo("Adana Çırçır & İplik Entegre", "Adana", 3, "Tekstil"),
                PlayerFacilityInfo("Gaziantep Ham Elyaf Deposu", "Gaziantep", 3, "Lojistik")
            )
        ),
        BotTycoonProfile(
            id = "BOT-LITYUM-10",
            name = "Oğuzhan Tekin",
            companyName = "Anadolu Nadir Elementler & Lityum A.Ş.",
            cityId = "eskisehir",
            cityName = "Eskişehir",
            badge = "STRATEJİK CEVHER UZMANI",
            avatarColor = 0xFF26A69A,
            specialtyTr = "Lityum, Grafit, Silisyum & Titanyum",
            specialtyEn = "Lithium, Graphite, Silicon & Titanium",
            level = 20,
            xp = 61200,
            netWorth = 21000000L,
            currentRdTitleTr = "Pil Sınıfı Lityum Karbonat Saflaştırma",
            currentRdTitleEn = "Battery-Grade Lithium Carbonate Purification",
            rdProgressPercent = 84,
            buyProductIds = listOf("lithium", "graphite_ore", "silicon", "titanium", "chemicals"),
            sellProductIds = listOf("lithium", "graphite_ore", "silicon", "titanium", "battery"),
            facilities = listOf(
                PlayerFacilityInfo("Kırka Lityum & Bor Rafinerisi", "Eskişehir", 4, "Stratejik Maden"),
                PlayerFacilityInfo("Kütahya Grafit Zenginleştirme", "Kütahya", 3, "Cevher"),
                PlayerFacilityInfo("Antalya Silikon & Kuvars Ocağı", "Antalya", 3, "Maden")
            )
        )
    )

    private val _botsState = MutableStateFlow<List<BotTycoonProfile>>(botList)
    val botsState: StateFlow<List<BotTycoonProfile>> = _botsState.asStateFlow()

    fun getAllBots(): List<BotTycoonProfile> = _botsState.value

    /**
     * Botlar pazar, icra ve konsorsiyum döngülerinde simüle edilir;
     * Sıralama (Liderlik) tablosunda adil rekabet için botlar yer almaz.
     */
    fun syncBotsToMultiplayerLeaderboard() {
        // Botlar sıralama tablosundan tamamen çıkarılmıştır.
    }

    /**
     * 1. PAZAR ALIŞVERİŞİ DÖNGÜSÜ:
     * - Botlar pazar ilanlarını tarar. Oyuncunun (veya pazarın) makul fiyattaki ilanlarını satın alır.
     * - Botlar düzenli olarak pazara yeni, dengeli ve kaliteli ürünler sunar.
     */
    suspend fun processMarketTradingCycle(viewModel: GameViewModel) {
        val p = viewModel.player.value ?: return
        val currentListings = viewModel.marketListings.value
        val myUid = if (viewModel._onlineEmail.value.isNotBlank()) viewModel._onlineEmail.value.replace(".", "_") else if (p.id.isNotBlank() && p.id != "local_player") p.id else "trader_${p.name.hashCode()}"
        val myName = p.name

        val now = System.currentTimeMillis()
        val twentyFourHoursMs = 24 * 60 * 60 * 1000L // 24 saat = 86.400.000 ms

        // A) SÜRESİ DOLMUŞ BOT İLANLARINI TEMİZLE (24 Saat Kuralı - Gerçek Dünya Saatine Göre)
        // Gerçek kullanıcılar 24 saat boyunca satın almazsa, 24 saat sonunda ilan yayından kaldırılır ve Supabase'den silinir.
        val expiredBotListings = currentListings.filter { listing ->
            val isBot = listing.sellerId.startsWith("BOT-") || listing.id.startsWith("BOT_LISTING_")
            isBot && listing.createdAt > 0L && (now - listing.createdAt) >= twentyFourHoursMs
        }
        if (expiredBotListings.isNotEmpty()) {
            viewModel._marketListings.value = viewModel._marketListings.value.filterNot { it in expiredBotListings }
            expiredBotListings.forEach { expired ->
                com.example.data.SupabaseManager.deleteMarketListingFromSupabase(expired.id)
            }
        }

        // B) BOTLARIN OYUNCU İLANLARINI SATIN ALMASI KURALI:
        // 1) İlk 24 saat boyunca botlar GERÇEK OYUNCU İLANLARINI KESİNLİKLE ALAMAZ. (Sadece gerçek kullanıcılar alabilir).
        // 2) 24 saatlik süre dolduğunda, gerçek kullanıcılar almamışsa botlar ilanı otomatik satın alabilir.
        // 3) Haksız zenginleşmeyi önlemek için: İlan fiyatı piyasa ortalamasından %20 ve fazlası pahalıysa (pricePerUnit > spotPrice * 1.20), botlar 24 saat dolduktan sonra bile ALAMAZ.
        val candidatePlayerListings = viewModel.marketListings.value.filter { listing ->
            val isMyListing = listing.sellerId == myUid || listing.sellerName.equals(myName, ignoreCase = true)
            val isRealPlayerListing = !listing.isBotListing
            val isOlderThan24Hours = (listing.createdAt > 0L) && ((now - listing.createdAt) >= twentyFourHoursMs)

            // Yalnızca 24 saati doldurmuş, miktar kalmış olan gerçek oyuncu ilanları
            if (!isMyListing && !isRealPlayerListing) return@filter false
            if (!isOlderThan24Hours || listing.quantity <= 0) return@filter false

            // Piyasa ortalaması / spot fiyat kontrolü (%20 ve üstü pahalıysa alım engellenir)
            val baseId = ItemQuality.extractBaseProductId(listing.itemId)
            val prod = Product.values().find { it.id == baseId }
            val rawSpotPrice = viewModel.marketPrices.value.find { it.itemId == baseId }?.price ?: prod?.basePrice ?: listing.pricePerUnit
            val qualityMultiplier = ItemQuality.fromStars(listing.qualityLevel).priceMultiplier
            val marketSpotPrice = (rawSpotPrice * qualityMultiplier).toLong()
            val maxAllowedBotPrice = (marketSpotPrice * 1.20).toLong()

            // İlan birim fiyatı piyasa ortalamasının %120'sinden küçük veya eşitse bot alabilir
            listing.pricePerUnit <= maxAllowedBotPrice
        }

        if (candidatePlayerListings.isNotEmpty()) {
            // İhtiyacı olan uygun bir bot seç
            for (listing in candidatePlayerListings) {
                val baseId = ItemQuality.extractBaseProductId(listing.itemId)
                val matchingBot = _botsState.value.find { bot ->
                    bot.buyProductIds.contains(baseId) || Random.nextFloat() < 0.35f
                } ?: _botsState.value.random()

                val prod = Product.values().find { it.id == baseId }
                val qualityMultiplier = ItemQuality.fromStars(listing.qualityLevel).priceMultiplier
                val rawSpotPrice = viewModel.marketPrices.value.find { it.itemId == baseId }?.price ?: prod?.basePrice ?: listing.pricePerUnit
                val marketSpotPrice = (rawSpotPrice * qualityMultiplier).toLong()
                val maxAllowedBotPrice = (marketSpotPrice * 1.20).toLong()

                // Makul fiyat kriteri (%20 tavan fiyat)
                if (listing.pricePerUnit <= maxAllowedBotPrice) {
                    val purchaseQty = if (listing.quantity <= 50) listing.quantity else Random.nextInt(15, listing.quantity + 1)
                    val totalPaid = purchaseQty.toLong() * listing.pricePerUnit

                    // Oyuncunun ilanını al ve parayı oyuncuya ver
                    val remainingQty = listing.quantity - purchaseQty
                    if (remainingQty <= 0) {
                        viewModel._marketListings.value = viewModel._marketListings.value.filterNot { it.id == listing.id }
                        com.example.data.SupabaseManager.deleteMarketListingFromSupabase(listing.id)
                    } else {
                        val updatedListing = listing.copy(quantity = remainingQty)
                        viewModel._marketListings.value = viewModel._marketListings.value.map {
                            if (it.id == listing.id) updatedListing else it
                        }
                        com.example.data.SupabaseManager.syncMarketListingToSupabase(updatedListing)
                    }

                    // Oyuncu bakiyesini artır
                    val updatedPlayer = p.copy(
                        money = p.money + totalPaid,
                        totalProfit = p.totalProfit + totalPaid
                    )
                    viewModel.repository.updatePlayer(updatedPlayer)

                    // İlgili botun istatistiklerini güncelle
                    matchingBot.completedTradeCount += 1
                    matchingBot.totalMoneySpentInMarket += totalPaid
                    matchingBot.xp += 180

                    // Oyuncuya bildirim gönder
                    val prodTitle = prod?.getDisplayName() ?: baseId.uppercase()
                    val qualityBadge = listing.quality.label
                    SmartNotificationManager.show(
                        "🏢 24 Saati Dolan İlanınız Alındı: ${matchingBot.name} (${matchingBot.companyName}), $purchaseQty Ton $qualityBadge $prodTitle ilanınızı ₳${formatMoney(totalPaid)} karşılığında satın aldı! 💰",
                        NotificationType.SUCCESS
                    )
                    HapticManager.performHaptic(HapticManager.HapticType.CONSORTIUM_APPROVAL)
                    ParticleManager.spawnCelebration()
                    break // Tek döngüde bir satış yeterli, canlı akış hissi verir
                }
            }
        }

        // C) BOTLARIN PAZARA YENİ ÜRÜN İLANI EKLEMESİ (24 Saat Yayında Kalacak Şekilde)
        val activeBotListings = viewModel.marketListings.value.filter {
            (it.sellerId.startsWith("BOT-") || it.id.startsWith("BOT_LISTING_")) && (now - it.createdAt) < twentyFourHoursMs
        }
        if (activeBotListings.size < 50) {
            val randomBot = _botsState.value.random()
            val productIdToSell = randomBot.sellProductIds.random()
            val prod = Product.values().find { it.id == productIdToSell }
            if (prod != null) {
                val qualityLevel = when {
                    randomBot.level >= 25 -> if (Random.nextFloat() < 0.40f) 5 else Random.nextInt(3, 5)
                    randomBot.level >= 18 -> Random.nextInt(2, 5)
                    randomBot.level >= 10 -> Random.nextInt(1, 4)
                    else -> if (Random.nextFloat() < 0.70f) 1 else 2
                }
                val qualityObj = ItemQuality.fromStars(qualityLevel)
                val baseWithQuality = (prod.basePrice * qualityObj.priceMultiplier).toLong()

                val priceVariation = Random.nextDouble(0.95, 1.12)
                val unitPrice = (baseWithQuality * priceVariation).toLong().coerceAtLeast(10L)
                val quantity = Random.nextInt(25, 120)

                val qualityItemKey = ItemQuality.makeKey(productIdToSell, qualityObj)
                val newBotListing = MarketListing(
                    id = "BOT_LISTING_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}",
                    sellerName = "${randomBot.name} (${randomBot.companyName})",
                    sellerId = randomBot.id,
                    itemId = qualityItemKey,
                    quantity = quantity,
                    pricePerUnit = unitPrice,
                    originCityId = randomBot.cityId,
                    qualityLevel = qualityLevel,
                    qualityTier = qualityObj.label,
                    createdAt = System.currentTimeMillis()
                )

                // 24 saati dolmamış bot ilanları ve oyuncu ilanlarını koru
                val preservedListings = viewModel._marketListings.value.filter {
                    val isBot = it.sellerId.startsWith("BOT-") || it.id.startsWith("BOT_LISTING_")
                    !isBot || (now - it.createdAt) < twentyFourHoursMs
                }
                viewModel._marketListings.value = (preservedListings + newBotListing).takeLast(80)

                // Supabase ile Senkronize Et
                com.example.data.SupabaseManager.syncMarketListingToSupabase(newBotListing)
            }
        }

        // C) BOTLARIN OYUNCUNUN TEDARİK TALEPLERİNİ (BUY ORDERS) KARŞILAMASI
        val currentBuyOrders = viewModel.buyOrders.value
        val myBuyOrders = currentBuyOrders.filter { order ->
            order.buyerId == myUid || order.buyerName.equals(myName, ignoreCase = true)
        }
        if (myBuyOrders.isNotEmpty() && Random.nextFloat() < 0.40f) {
            val targetOrder = myBuyOrders.random()
            val matchingBot = _botsState.value.find { it.sellProductIds.contains(targetOrder.itemId) } ?: _botsState.value.random()
            val prod = Product.values().find { it.id == targetOrder.itemId }
            val prodTitle = prod?.getDisplayName() ?: targetOrder.itemId.uppercase()

            // Siparişi teslim et
            viewModel.repository.produceItem(targetOrder.itemId, targetOrder.quantity)
            viewModel._buyOrders.value = viewModel._buyOrders.value.filterNot { it.id == targetOrder.id }

            SmartNotificationManager.show(
                "🚚 ${matchingBot.name} (${matchingBot.companyName}), ${targetOrder.quantity} Ton $prodTitle tedarik talebinizi karşıladı ve deponuza teslim etti! ✨",
                NotificationType.SUCCESS
            )
            HapticManager.performHaptic(HapticManager.HapticType.CONSORTIUM_APPROVAL)
            ParticleManager.spawnCelebration()
        }
    }

    /**
     * 2. KONSORSİYUM & MEGA PROJE KATILIMI DÖNGÜSÜ:
     * - Botlar açık olan mega projelerdeki tedarikçi kotalarını doldurur.
     */
    fun processConsortiumCycle(viewModel: GameViewModel) {
        val currentProjects = viewModel.megaProjects.value
        if (currentProjects.isEmpty()) return

        var hasChanges = false
        val updatedProjects = currentProjects.map { proj ->
            // Tamamlanmamış slotları bul
            val unmetSlots = proj.slots.filter { !it.isFullyDelivered }
            if (unmetSlots.isNotEmpty() && Random.nextFloat() < 0.30f) {
                val targetSlot = unmetSlots.random()
                val candidateBots = _botsState.value.filter { bot ->
                    val isQualityQualified = when (proj.qualityTier) {
                        ConsortiumQualityTier.GRADE_A -> bot.rdProgressPercent >= 60 || bot.level >= 12
                        ConsortiumQualityTier.GRADE_B -> bot.rdProgressPercent >= 25 || bot.level >= 6
                        ConsortiumQualityTier.GRADE_C -> true
                    }
                    isQualityQualified && (bot.sellProductIds.contains(targetSlot.productId) || bot.buyProductIds.contains(targetSlot.productId) || Random.nextFloat() < 0.5f)
                }
                val matchingBot = candidateBots.randomOrNull()

                if (matchingBot != null) {
                    val deliverQty = Random.nextInt(5, 25).coerceAtMost(targetSlot.quantityRequired - targetSlot.quantityDelivered)
                    if (deliverQty > 0) {
                        val unitCost = Product.values().find { it.id == targetSlot.productId }?.basePrice ?: 500L
                        val newDelivered = targetSlot.quantityDelivered + deliverQty

                        val botStars = when (proj.qualityTier) {
                            ConsortiumQualityTier.GRADE_A -> if (matchingBot.level >= 15) 5 else 4
                            ConsortiumQualityTier.GRADE_B -> if (matchingBot.level >= 10) 4 else if (matchingBot.level >= 6) 3 else 2
                            ConsortiumQualityTier.GRADE_C -> if (matchingBot.level >= 8) 2 else 1
                        }

                        val updatedSlot = targetSlot.copy(
                            quantityDelivered = newDelivered,
                            assignedPartnerId = targetSlot.assignedPartnerId ?: matchingBot.id,
                            assignedPartnerName = targetSlot.assignedPartnerName ?: "${matchingBot.name} (${matchingBot.companyName})",
                            costContributionValue = targetSlot.costContributionValue + (unitCost * deliverQty),
                            deliveredQualityTier = botStars,
                            deliveredQualityScore = botStars.toDouble()
                        )

                        hasChanges = true
                        proj.copy(
                            slots = proj.slots.map { if (it.slotId == targetSlot.slotId) updatedSlot else it }
                        )
                    } else proj
                } else proj
            } else if (Random.nextFloat() < 0.20f) {
                // Botların hisse piyasasında işlem yapması ve fiyatı canlı tutması
                val isBuy = Random.nextFloat() < 0.65f
                val curPrice = proj.currentSharePrice
                val curMult = if (proj.sharePriceMultiplier > 0.0) proj.sharePriceMultiplier else 1.0
                val delta = if (isBuy) Random.nextDouble(0.003, 0.008) else -Random.nextDouble(0.002, 0.006)
                val newMult = (curMult * (1.0 + delta)).coerceIn(0.30, 4.0)
                hasChanges = true
                proj.copy(
                    previousSharePrice = curPrice,
                    sharePriceMultiplier = newMult
                )
            } else proj
        }

        if (hasChanges) {
            viewModel._megaProjects.value = updatedProjects
        }
    }

    /**
     * 3. AR-GE, GELİŞİM VE NET VARLIK DÖNGÜSÜ:
     * - Botların Ar-Ge seviyeleri, XP'leri ve Net Varlıkları canlı olarak büyür.
     */
    fun processBotProgressionCycle() {
        val updatedBots = _botsState.value.map { bot ->
            // Rastgele Ar-Ge ilerlemesi
            var newProgress = bot.rdProgressPercent + Random.nextInt(1, 4)
            var newRdTitleTr = bot.currentRdTitleTr
            var newRdTitleEn = bot.currentRdTitleEn
            var newLevel = bot.level
            var newXp = bot.xp + Random.nextInt(50, 150)

            if (newProgress >= 100) {
                newProgress = 15
                newLevel += 1
                // Yeni Ar-Ge projesine geçiş
                when (bot.id) {
                    "BOT-KAYA-01" -> {
                        newRdTitleTr = "Ultra Hafif Titanyum-Çelik Hibrit Gövde"
                        newRdTitleEn = "Ultra-Light Titanium-Steel Hybrid Body"
                    }
                    "BOT-NOVA-02" -> {
                        newRdTitleTr = "1.4nm Nöromorfik Yapay Zeka Çipleri"
                        newRdTitleEn = "1.4nm Neuromorphic AI Chips"
                    }
                    "BOT-TOROS-03" -> {
                        newRdTitleTr = "Sıfır Emisyonlu Yeşil Amonyak & Hidrojen"
                        newRdTitleEn = "Zero Emission Green Ammonia & Hydrogen"
                    }
                    "BOT-EGE-04" -> {
                        newRdTitleTr = "Biyo-Mühendislik Akıllı Termal Kumaşlar"
                        newRdTitleEn = "Bio-Engineered Smart Thermal Fabrics"
                    }
                    "BOT-AVRASYA-05" -> {
                        newRdTitleTr = "Plazma Korumalı Havacılık Türbin Kanadı"
                        newRdTitleEn = "Plasma-Shielded Aviation Turbine Blades"
                    }
                    "BOT-MADEN-06" -> {
                        newRdTitleTr = "Otomasyonlu Derin Damar Maden Galerileri"
                        newRdTitleEn = "Automated Deep-Seam Mine Galleries"
                    }
                    "BOT-BAKIR-07" -> {
                        newRdTitleTr = "Süper İletken Oksijensiz Bakır Çubuk Dökümü"
                        newRdTitleEn = "Superconducting Oxygen-Free Copper Rod Casting"
                    }
                    "BOT-ORMAN-08" -> {
                        newRdTitleTr = "Eko-Döngüsel Kereste & Yüksek Esneklikli Kauçuk"
                        newRdTitleEn = "Eco-Circular Timber & High-Elasticity Rubber"
                    }
                    "BOT-PAMUK-09" -> {
                        newRdTitleTr = "Ultra İnce Lifli Organik Ege-Harran Pamuğu"
                        newRdTitleEn = "Ultra-Fine Fiber Organic Aegean-Harran Cotton"
                    }
                    "BOT-LITYUM-10" -> {
                        newRdTitleTr = "Solid-State Batarya Sınıfı Nano Lityum Katot"
                        newRdTitleEn = "Solid-State Battery Grade Nano Lithium Cathode"
                    }
                }
            }

            // Net varlık organik büyümesi (+%0.2 - %0.6)
            val netWorthGain = (bot.netWorth * Random.nextDouble(0.002, 0.006)).toLong()

            bot.copy(
                level = newLevel,
                xp = newXp,
                netWorth = bot.netWorth + netWorthGain,
                currentRdTitleTr = newRdTitleTr,
                currentRdTitleEn = newRdTitleEn,
                rdProgressPercent = newProgress
            )
        }

        _botsState.value = updatedBots
        syncBotsToMultiplayerLeaderboard()
    }

    /**
     * 4. CANLI BORSA İŞLEM DÖNGÜSÜ (Tier-1 Ağırlıklı):
     * - Botlar (özellikle yeni eklenen Tier 1 maden, orman, tarım ve enerji baronları)
     *   borsa menüsünü de bir oyuncu alıcı/satıcı gibi kullanarak canlı alım satım yapar.
     * - Alım yapıldığında borsa stoğu azalır (örn. 999999 -> 999980) ve borsa fiyatı artar.
     * - Satış yapıldığında borsa stoğu artar ve fiyat düşer.
     * - Supabase ve tüm online oyunculara anlık iletilir.
     */
    fun processBorsaTradingCycle(viewModel: GameViewModel) {
        if (_botsState.value.isEmpty()) return

        // Tier 1 ağırlıklı seçim: %75 ihtimalle Tier 1 uzmanı 5 yeni bottan biri seçilir
        val tier1Bots = _botsState.value.filter {
            it.id.startsWith("BOT-MADEN") || it.id.startsWith("BOT-BAKIR") ||
            it.id.startsWith("BOT-ORMAN") || it.id.startsWith("BOT-PAMUK") ||
            it.id.startsWith("BOT-LITYUM")
        }
        val bot = if (tier1Bots.isNotEmpty() && Random.nextFloat() < 0.75f) {
            tier1Bots.random()
        } else {
            _botsState.value.random()
        }

        val isBuyAction = Random.nextBoolean()
        if (isBuyAction) {
            // Bot borsadan hammadde/girdi satın alır
            val candidateItems = bot.buyProductIds.filter { id ->
                val prod = Product.values().find { it.id == id }
                prod?.tier == ProductTier.TIER_1 || Random.nextFloat() < 0.30f
            }
            val targetItemId = if (candidateItems.isNotEmpty()) candidateItems.random() else (bot.buyProductIds.randomOrNull() ?: "iron")
            val qty = Random.nextInt(5, 25)

            viewModel.onBorsaItemBought(targetItemId, qty, "Global", syncToRemote = false)
            bot.completedTradeCount += 1
            bot.xp += 30
        } else {
            // Bot borsaya ürettiği çıktıyı/cevheri satar
            val candidateItems = bot.sellProductIds.filter { id ->
                val prod = Product.values().find { it.id == id }
                prod?.tier == ProductTier.TIER_1 || Random.nextFloat() < 0.30f
            }
            val targetItemId = if (candidateItems.isNotEmpty()) candidateItems.random() else (bot.sellProductIds.randomOrNull() ?: "coal")
            val qty = Random.nextInt(5, 30)

            viewModel.onBorsaItemSold(targetItemId, qty, "Global", syncToRemote = false)
            bot.completedTradeCount += 1
            bot.xp += 30
        }
    }
}
