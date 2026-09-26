package com.example.data

import kotlin.random.Random

enum class CityEventCategory(val title: String, val badgeColorHex: Long) {
    AGRICULTURE("Tarım & Hasat", 0xFF2E7D32),
    CRISIS("Kriz & Kuraklık", 0xFFC62828),
    INDUSTRY("Sanayi & Üretim", 0xFF1565C0),
    MINING("Madencilik & Enerji", 0xFFE65100),
    EXPORT("İhracat & Ticaret", 0xFFF57F17),
    TOURISM("Turizm & Talep", 0xFF6A1B9A),
    LOGISTICS("Lojistik & Ulaşım", 0xFF00838F);

    fun getTitle(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                AGRICULTURE -> "Agriculture & Harvest"
                CRISIS -> "Crisis & Drought"
                INDUSTRY -> "Industry & Production"
                MINING -> "Mining & Energy"
                EXPORT -> "Export & Trade"
                TOURISM -> "Tourism & Demand"
                LOGISTICS -> "Logistics & Transport"
            }
        } else {
            title
        }
    }
}

data class CityMarketEvent(
    val id: String,
    val cityId: String,
    val cityName: String,
    val affectedProductIds: List<String>,
    val affectedProductNames: List<String>,
    val initialPriceMultiplier: Float = 1.0f, // e.g. 1.45f (+45%), 0.70f (-30%)
    val headline: String,
    val description: String,
    val strategyTip: String,
    val category: CityEventCategory,
    val iconEmoji: String,
    val startTimeMs: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 15,
    val deliveredVolume: Int = 0, // Bu şehre satılan/teslim edilen toplam ürün miktarı (Ton)
    val saturationCapacity: Int = 8000 // Pazar doygunluk eşiği (8000 ton teslimatta kriz primi sönümlenir)
) {
    /**
     * Pazar Doygunluk Oranı (Market Saturation Ratio) [0.0 .. 1.0]
     */
    val saturationRatio: Float
        get() = (deliveredVolume.toFloat() / saturationCapacity.coerceAtLeast(1).toFloat()).coerceIn(0.0f, 1.0f)

    val saturationPercentage: Int
        get() = (saturationRatio * 100f).toInt()

    val remainingCapacity: Int
        get() = (saturationCapacity - deliveredVolume).coerceAtLeast(0)

    /**
     * ARZ-TALEP ESNEKLİĞİ (SUPPLY/DEMAND ELASTICITY) DİNAMİK ÇARPANI:
     * - Grev ve Talep Patlaması (initialPriceMultiplier > 1.0f):
     *   Örneğin Kocaeli Sanayi Grevi (%40 zam, 1.40f).
     *   Oyuncular o şehre mal sattıkça (her 1000 tonda %5 düşerek) %35, %30, %25 şeklinde kademeli olarak
     *   normal piyasa fiyatına (1.0f) doğru sönümlenir.
     *
     * - Hasat ve Bolluk İndirimi (initialPriceMultiplier < 1.0f):
     *   Örneğin İzmir Zeytin Bolluğu (%30 indirim, 0.70f).
     *   Pazardaki fazla arz oyuncular tarafından satın alınıp/doyuldukça indirim kademeli olarak 1.0f'e yükselir.
     */
    val priceMultiplier: Float
        get() {
            val remainingFactor = (1.0f - saturationRatio).coerceIn(0.0f, 1.0f)
            return if (initialPriceMultiplier >= 1.0f) {
                val premium = initialPriceMultiplier - 1.0f
                (1.0f + (premium * remainingFactor)).coerceAtLeast(1.0f)
            } else {
                val discount = 1.0f - initialPriceMultiplier
                (1.0f - (discount * remainingFactor)).coerceIn(initialPriceMultiplier, 1.0f)
            }
        }

    val expiryTimeMs: Long
        get() = startTimeMs + (durationMinutes * 60 * 1000L)

    val isExpired: Boolean
        get() = com.example.util.GameTime.isExpired(expiryTimeMs)

    val remainingSeconds: Long
        get() = com.example.util.GameTime.remainingSeconds(expiryTimeMs)

    val isPriceIncrease: Boolean
        get() = priceMultiplier > 1.0f

    val percentFormatted: String
        get() {
            val diff = (priceMultiplier - 1.0f) * 100f
            val sign = if (diff >= 0) "+" else ""
            return "$sign%${"%.0f".format(diff)}"
        }

    val initialPercentFormatted: String
        get() {
            val diff = (initialPriceMultiplier - 1.0f) * 100f
            val sign = if (diff >= 0) "+" else ""
            return "$sign%${"%.0f".format(diff)}"
        }

    val remainingFormatted: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }

    fun getHeadline(isEnglish: Boolean): String {
        if (!isEnglish) return headline
        return when (id) {
            "evt_eskisehir_boron_rush" -> "⚡ Boron & Jet Engine Boom in Eskisehir!"
            "evt_batman_petroleum_surge" -> "🛢️ Crude Oil Production Surge in Batman!"
            "evt_zonguldak_coal_rally" -> "⛏️ Hard Coal Energy Demand in Zonguldak!"
            "evt_sivas_iron_demand" -> "🏗️ Iron Ore Mining Explosion in Sivas!"
            "evt_kahramanmaras_cotton_boom" -> "🧵 Industrial Cotton Harvest in Kahramanmaras!"
            "evt_konya_aluminum_surge" -> "⚙️ Aluminum Casting & Bauxite Rush in Konya!"
            "evt_kocaeli_industry_strike" -> "🏭 Petrochemical & Synthetic Boom in Kocaeli!"
            "evt_bursa_automotive_deal" -> "🚗 Cable & Profile Export Wave in Bursa!"
            "evt_izmir_chip_rally" -> "🔬 Microchip & Semiconductor Rally in Izmir!"
            "evt_ankara_defense_boom" -> "🛡️ Defense & Radar System Contracts in Ankara!"
            "evt_gaziantep_chemistry_export" -> "🧪 Industrial Chemistry & Weaving in Gaziantep!"
            else -> headline
        }
    }

    fun getDescription(isEnglish: Boolean): String {
        if (!isEnglish) return description
        return when (id) {
            "evt_eskisehir_boron_rush" -> "Huge orders for jet engines and boron processing arrived. Industrialists are aggressively buying raw materials."
            "evt_batman_petroleum_surge" -> "Refineries around Batman are working at full capacity. Crude oil and fuel trade has surged."
            "evt_zonguldak_coal_rally" -> "Heavy steel plants boosted demand for hard coal. Local mining hubs offer great profit margins."
            "evt_sivas_iron_demand" -> "Steel mills increased their demand for Sivas iron ore. Mining output is trading at peak rates."
            "evt_kahramanmaras_cotton_boom" -> "Global textile supply chains turned to Kahramanmaras industrial cotton."
            "evt_konya_aluminum_surge" -> "Konya bauxite processing is scaling up. Aluminum ingot demand is high."
            "evt_kocaeli_industry_strike" -> "Petrochemical and synthetic fiber clusters are producing around the clock."
            "evt_bursa_automotive_deal" -> "European auto and aerospace contracts boosted demand for specialized cables and profiles."
            "evt_izmir_chip_rally" -> "New semiconductor cleanrooms in Izmir are purchasing pure silicon and quartz at high prices."
            "evt_ankara_defense_boom" -> "High-tech defense systems and composite body plants have opened major supply tenders."
            "evt_gaziantep_chemistry_export" -> "Weaving and industrial chemicals sectors in Gaziantep are breaking regional export records."
            else -> description
        }
    }

    fun getStrategyTip(isEnglish: Boolean): String {
        if (!isEnglish) return strategyTip
        return when (id) {
            "evt_eskisehir_boron_rush" -> "Transport boron and precision alloys to Eskisehir for maximum profit margins!"
            "evt_batman_petroleum_surge" -> "Load crude oil from Batman and supply it to Kocaeli or Houston refineries!"
            "evt_zonguldak_coal_rally" -> "Deliver hard coal from Zonguldak to heavy industry centers in Kocaeli and Essen!"
            "evt_sivas_iron_demand" -> "Ship iron ore from Sivas to alloy and steel plants across the country!"
            "evt_kahramanmaras_cotton_boom" -> "Supply industrial cotton from Maras to Gaziantep weaving facilities!"
            "evt_konya_aluminum_surge" -> "Process bauxite into aluminum ingots in Konya and distribute to aerospace hubs!"
            "evt_kocaeli_industry_strike" -> "Bring chemicals to Kocaeli and trade synthetic polymers for high returns!"
            "evt_bursa_automotive_deal" -> "Deliver copper cables and steel castings to Bursa manufacturers!"
            "evt_izmir_chip_rally" -> "Transport refined silicon from Mugla to Izmir cleanrooms for high returns!"
            "evt_ankara_defense_boom" -> "Provide microchips and composites to Ankara defense facilities!"
            "evt_gaziantep_chemistry_export" -> "Trade industrial chemicals and yarns in Gaziantep to capitalize on market demand!"
            else -> strategyTip
        }
    }

    fun getAffectedProductNames(isEnglish: Boolean): List<String> {
        if (!isEnglish) return affectedProductNames
        return affectedProductNames.map { name ->
            when (name) {
                "Bor Madeni" -> "Boron Ore"
                "Jet Motoru" -> "Jet Engine"
                "Ham Petrol" -> "Crude Oil"
                "Taş Kömürü" -> "Hard Coal"
                "Demir Cevheri" -> "Iron Ore"
                "Pamuk" -> "Cotton"
                "Boksit" -> "Bauxite"
                "Alüminyum" -> "Aluminum"
                "Petrokimya" -> "Petrochemicals"
                "Kablo & Profil" -> "Cables & Profiles"
                "Çip & Sensör" -> "Chips & Sensors"
                "Radar & Kompozit" -> "Radar & Composites"
                "Endüstriyel Kimya" -> "Industrial Chemistry"
                "Silisyum" -> "Silicon"
                "Krom" -> "Chrome"
                "Bakır" -> "Copper"
                else -> name
            }
        }
    }

    fun getCityName(isEnglish: Boolean): String {
        if (!isEnglish) return cityName
        return when (cityName) {
            "Eskişehir" -> "Eskisehir"
            "Batman" -> "Batman"
            "Zonguldak" -> "Zonguldak"
            "Sivas" -> "Sivas"
            "Kahramanmaraş" -> "Kahramanmaras"
            "Konya" -> "Konya"
            "Kocaeli" -> "Kocaeli"
            "Bursa" -> "Bursa"
            "İzmir" -> "Izmir"
            "Ankara" -> "Ankara"
            "Gaziantep" -> "Gaziantep"
            "İstanbul" -> "Istanbul"
            "Mersin" -> "Mersin"
            "Artvin" -> "Artvin"
            "Muğla" -> "Mugla"
            "Elazığ" -> "Elazig"
            "Kırıkkale" -> "Kirikkale"
            "Kayseri" -> "Kayseri"
            else -> cityName
        }
    }
}

object CityNewsEventManager {

    private val eventTemplates = listOf(
        CityMarketEvent(
            id = "evt_eskisehir_boron_rush",
            cityId = "eskisehir",
            cityName = "Eskişehir",
            affectedProductIds = listOf("boron", "jet_engine", "turbine"),
            affectedProductNames = listOf("Bor Madeni", "Jet Motoru"),
            initialPriceMultiplier = 1.45f,
            headline = "⚡ Eskişehir'de Havacılık & Bor İşleme Rallisi!",
            description = "Jet motoru ve bor işleme tesislerinden dev siparişler geldi. Sanayiciler hammadde topluyor.",
            strategyTip = "Eskişehir'e bor ve hassas alaşım taşıyarak maksimum kâr marjı yakalayın!",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "⚡",
            durationMinutes = 14,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_batman_petroleum_surge",
            cityId = "batman",
            cityName = "Batman",
            affectedProductIds = listOf("crude_oil", "fuel", "plastic"),
            affectedProductNames = listOf("Ham Petrol", "Petrokimya"),
            initialPriceMultiplier = 1.40f,
            headline = "🛢️ Batman Kuyularında Üretim ve İhracat Patlaması!",
            description = "Rafineriler ve petrokimya tesisleri tam kapasite çalışıyor, petrol ticareti rekor kırdı.",
            strategyTip = "Batman'dan ham petrol çekip Kocaeli veya küresel merkezlere yüksek kârla sevk edin!",
            category = CityEventCategory.EXPORT,
            iconEmoji = "🛢️",
            durationMinutes = 15,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_zonguldak_coal_rally",
            cityId = "zonguldak",
            cityName = "Zonguldak",
            affectedProductIds = listOf("hard_coal", "coke_coal", "energy"),
            affectedProductNames = listOf("Taş Kömürü"),
            initialPriceMultiplier = 1.35f,
            headline = "⛏️ Zonguldak Taş Kömürü Havzasında Enerji Talebi!",
            description = "Ağır sanayi fırınları için taş kömürü talebi tırmandı. Ocaklardan doğrudan alımlar hızlandı.",
            strategyTip = "Zonguldak'tan taş kömürü alıp Kocaeli ve Essen sanayi fırınlarına ulaştırın!",
            category = CityEventCategory.MINING,
            iconEmoji = "⛏️",
            durationMinutes = 12,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_sivas_iron_demand",
            cityId = "sivas",
            cityName = "Sivas",
            affectedProductIds = listOf("iron_ore", "steel_alloy", "cast_iron"),
            affectedProductNames = listOf("Demir Cevheri"),
            initialPriceMultiplier = 1.40f,
            headline = "🏗️ Sivas Madenlerinde Rekor Sevkiyat!",
            description = "Çelik fabrikaları Sivas demir cevheri alımlarını hızlandırdı.",
            strategyTip = "Sivas madenlerinden demir cevheri çekip sanayi kentlerine taşıyın!",
            category = CityEventCategory.MINING,
            iconEmoji = "🏗️",
            durationMinutes = 15,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_kahramanmaras_cotton_boom",
            cityId = "kahramanmaras",
            cityName = "Kahramanmaraş",
            affectedProductIds = listOf("cotton", "yarn", "fabric"),
            affectedProductNames = listOf("Pamuk"),
            initialPriceMultiplier = 1.35f,
            headline = "🧵 Kahramanmaraş Pamuk Hasadında Küresel Talep!",
            description = "Endüstriyel pamuk tarlalarından çıkan ürünler tekstil devleri tarafından kapışılıyor.",
            strategyTip = "Maraş'tan pamuk temin edip Gaziantep dokuma tesislerine sevk edin!",
            category = CityEventCategory.AGRICULTURE,
            iconEmoji = "🧵",
            durationMinutes = 12,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_konya_aluminum_surge",
            cityId = "konya",
            cityName = "Konya",
            affectedProductIds = listOf("bauxite", "aluminum_ingot", "aluminum_profile"),
            affectedProductNames = listOf("Boksit", "Alüminyum"),
            initialPriceMultiplier = 1.40f,
            headline = "⚙️ Konya'da Alüminyum & Boksit Rüzgarı!",
            description = "Otomotiv ve havacılık sektörünün alüminyum talebi Konya tesislerini hareketlendirdi.",
            strategyTip = "Konya'dan boksit ve alüminyum alıp savunma ve otomotiv üslerine satın!",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "⚙️",
            durationMinutes = 14,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_kocaeli_industry_strike",
            cityId = "kocaeli",
            cityName = "Kocaeli",
            affectedProductIds = listOf("petrochem", "synthetic_fiber", "polymer"),
            affectedProductNames = listOf("Petrokimya"),
            initialPriceMultiplier = 1.40f,
            headline = "🏭 Kocaeli Petrokimya ve Sentetik Sanayiinde Canlanma!",
            description = "Sentetik elyaf ve polimer talebi tavan yaptı, fabrikalar aralıksız hammadde çekiyor.",
            strategyTip = "Batman'dan ham madde getirip Kocaeli kimya devlerine rekor fiyata teslim edin!",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "🏭",
            durationMinutes = 15,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_bursa_automotive_deal",
            cityId = "bursa",
            cityName = "Bursa",
            affectedProductIds = listOf("cable", "casting", "profile", "automobile_chassis"),
            affectedProductNames = listOf("Kablo & Profil"),
            initialPriceMultiplier = 1.50f,
            headline = "🚗 Bursa Kablo ve Döküm Sanayiinde Dev İhracat!",
            description = "Avrupa'ya yapılan yeni filo sözleşmeleriyle döküm ve kablo sanayii atağa geçti.",
            strategyTip = "Bursa fabrikalarına bakır ve çelik profiller teslim ederek yüksek marj kazanın!",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "🚗",
            durationMinutes = 16,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_izmir_chip_rally",
            cityId = "izmir",
            cityName = "İzmir",
            affectedProductIds = listOf("microchip", "semiconductor", "sensor", "silicon"),
            affectedProductNames = listOf("Çip & Sensör", "Silisyum"),
            initialPriceMultiplier = 1.45f,
            headline = "🔬 İzmir Yarı İletken ve Sensör Sanayiinde Büyük Atılım!",
            description = "Temiz oda üretim hatları Muğla'dan gelen yüksek saflıktaki silisyumu işliyor.",
            strategyTip = "Muğla'dan silisyum alıp İzmir çip tesislerine satarak devasa getiri elde edin!",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "🔬",
            durationMinutes = 15,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_ankara_defense_boom",
            cityId = "ankara",
            cityName = "Ankara",
            affectedProductIds = listOf("radar_system", "composite_body", "avionics"),
            affectedProductNames = listOf("Radar & Kompozit"),
            initialPriceMultiplier = 1.50f,
            headline = "🛡️ Ankara Savunma ve Kompozit Gövde İhalesi Açıldı!",
            description = "Havacılık ve savunma devleri yüksek teknoloji bileşenleri için tedarik yarışında.",
            strategyTip = "İzmir'den çip ve Eskişehir'den jet motoru parçalarını Ankara'ya teslim edin!",
            category = CityEventCategory.INDUSTRY,
            iconEmoji = "🛡️",
            durationMinutes = 16,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_gaziantep_chemistry_export",
            cityId = "gaziantep",
            cityName = "Gaziantep",
            affectedProductIds = listOf("industrial_chem", "synthetic_yarn", "fabric"),
            affectedProductNames = listOf("Endüstriyel Kimya"),
            initialPriceMultiplier = 1.40f,
            headline = "🧪 Gaziantep Kimya ve Dokuma İhracatında Zirve!",
            description = "Ortadoğu ve Avrupa pazarına yönelik endüstriyel kimya ve dokuma siparişleri katlandı.",
            strategyTip = "Gaziantep'e pamuk ve kimyasal hammadde taşıyarak pazar çarpanından yararlanın!",
            category = CityEventCategory.EXPORT,
            iconEmoji = "🧪",
            durationMinutes = 12,
            saturationCapacity = 8000
        ),
        CityMarketEvent(
            id = "evt_new_york_wall_street_rally",
            cityId = "new_york",
            cityName = "New York",
            affectedProductIds = listOf("quantum_supercomputer", "ai_datacenter", "optical_fiber", "pharma"),
            affectedProductNames = listOf("Kuantum Bilgisayar", "Yapay Zeka"),
            initialPriceMultiplier = 1.55f,
            headline = "🗽 Wall Street & Manhattan'da Kuantum & AI Yatırım Rallisi!",
            description = "New York finans ve teknoloji devleri, kuantum hesaplama ve yapay zeka veri merkezleri için devasa bütçe ayırdı.",
            strategyTip = "New York'a kuantum bileşenleri, optik fiber ve biyofarmasötik ürünleri sevk ederek dolar bazında yüksek kâr marjı yakalayın!",
            category = CityEventCategory.EXPORT,
            iconEmoji = "🗽",
            durationMinutes = 18,
            saturationCapacity = 10000
        )
    )

    /**
     * Generates a fresh random set of 2 to 3 active unique events across Anatolian cities.
     */
    fun generateInitialEvents(): List<CityMarketEvent> {
        val shuffled = eventTemplates.shuffled()
        val count = Random.nextInt(2, 4) // 2 or 3 active events
        val now = System.currentTimeMillis()
        return shuffled.take(count).mapIndexed { idx, evt ->
            evt.copy(
                startTimeMs = now - (idx * 60 * 1000L), // Staggered start times
                durationMinutes = Random.nextInt(10, 18)
            )
        }
    }

    /**
     * Refreshes expired events and brings in new breaking news.
     */
    fun updateEvents(currentEvents: List<CityMarketEvent>): List<CityMarketEvent> {
        val now = System.currentTimeMillis()
        val validEvents = currentEvents.filter { !it.isExpired }.toMutableList()

        // If we have less than 2 events, pick new ones that are not currently active
        while (validEvents.size < 2) {
            val activeIds = validEvents.map { it.id }.toSet()
            val available = eventTemplates.filter { it.id !in activeIds }
            if (available.isNotEmpty()) {
                val picked = available.random()
                validEvents.add(
                    picked.copy(
                        startTimeMs = now,
                        durationMinutes = Random.nextInt(10, 18),
                        deliveredVolume = 0
                    )
                )
            } else {
                break
            }
        }
        return validEvents
    }

    /**
     * Oyuncular o şehre mal sattıkça pazar doygunluğunu artıran ve çarpanı kademeli olarak
     * normal seviyelere doğru sönümleyen Arz-Talep Esnekliği fonksiyonu.
     */
    fun recordDeliveredGoods(
        cityId: String,
        productId: String,
        quantity: Int,
        currentEvents: List<CityMarketEvent>
    ): List<CityMarketEvent> {
        if (quantity <= 0) return currentEvents
        var modified = false
        val updated = currentEvents.map { event ->
            val isTargetCity = event.cityId.equals(cityId, ignoreCase = true)
            val affectsProduct = productId in event.affectedProductIds ||
                event.affectedProductIds.any { it.contains(productId, ignoreCase = true) || productId.contains(it, ignoreCase = true) }

            if (isTargetCity && affectsProduct && !event.isExpired) {
                modified = true
                val newDelivered = (event.deliveredVolume + quantity).coerceAtMost(event.saturationCapacity * 2)
                event.copy(deliveredVolume = newDelivered)
            } else {
                event
            }
        }
        return if (modified) updated else currentEvents
    }

    /**
     * Calculates the city price multiplier for a specific product in a specific city.
     * Returns 1.0f if no event affects it, or the dynamic elastic multiplier (e.g. 1.40f -> 1.35f -> 1.30f).
     */
    fun getEventPriceMultiplier(
        cityId: String,
        productId: String,
        activeEvents: List<CityMarketEvent>
    ): Float {
        for (event in activeEvents) {
            if (event.cityId.equals(cityId, ignoreCase = true) && !event.isExpired) {
                if (productId in event.affectedProductIds || event.affectedProductIds.any { it.contains(productId, ignoreCase = true) || productId.contains(it, ignoreCase = true) }) {
                    return event.priceMultiplier
                }
            }
        }
        return 1.0f
    }
}
