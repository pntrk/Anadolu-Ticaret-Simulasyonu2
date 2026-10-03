package com.example.data

import com.example.ui.theme.Dictionary
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Anadolu Ekonomi Bülteni: Bölgesel Kâr Fırsatları & Görev Modeli.
 * Oyuncunun üretim ve satış aşamalarını takip eder, akıllı yönlendirme ve 1-10 Elmas ödülü sunar.
 */
data class BulletinOpportunity(
    val id: String,
    val cityId: String,
    val cityNameTr: String,
    val cityNameEn: String,
    val category: CityEventCategory,
    val targetProductId: String,
    val targetProductNameTr: String,
    val targetProductNameEn: String,
    val targetFacilityId: String,
    val targetFacilityNameTr: String,
    val targetFacilityNameEn: String,
    val headlineTr: String,
    val headlineEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val strategyTipTr: String,
    val strategyTipEn: String,
    val priceMultiplier: Float, // e.g. 1.45f (+45%)
    val targetQuantity: Int, // e.g. 50 Ton
    val producedQuantity: Int = 0,
    val soldQuantity: Int = 0,
    val diamondReward: Int = 5, // 1 - 10 Elmas
    val isClaimed: Boolean = false,
    val iconEmoji: String = "🌾"
) {
    val isProductionTargetMet: Boolean
        get() = producedQuantity >= targetQuantity

    val isCompleted: Boolean
        get() = soldQuantity >= targetQuantity

    val productionRatio: Float
        get() = if (targetQuantity > 0) (producedQuantity.toFloat() / targetQuantity.toFloat()).coerceIn(0f, 1f) else 0f

    val salesRatio: Float
        get() = if (targetQuantity > 0) (soldQuantity.toFloat() / targetQuantity.toFloat()).coerceIn(0f, 1f) else 0f

    val overallProgressPercent: Int
        get() {
            // %50 Üretim, %50 Satış
            val pScore = productionRatio * 50f
            val sScore = salesRatio * 50f
            return (pScore + sScore).toInt().coerceIn(0, 100)
        }

    val pricePremiumText: String
        get() {
            val diff = (priceMultiplier - 1.0f) * 100f
            val sign = if (diff >= 0) "+" else ""
            return "$sign%${"%.0f".format(diff)}"
        }

    val countryFlag: String
        get() = cities.find { it.id.equals(cityId, ignoreCase = true) }?.countryFlag ?: "🇹🇷"

    val isGlobal: Boolean
        get() = cities.find { it.id.equals(cityId, ignoreCase = true) }?.isGlobal ?: false

    fun getCityName(isEnglish: Boolean): String = if (isEnglish) cityNameEn else cityNameTr
    fun getProductName(isEnglish: Boolean): String = if (isEnglish) targetProductNameEn else targetProductNameTr
    fun getFacilityName(isEnglish: Boolean): String = if (isEnglish) targetFacilityNameEn else targetFacilityNameTr
    fun getHeadline(isEnglish: Boolean): String = if (isEnglish) headlineEn else headlineTr
    fun getDescription(isEnglish: Boolean): String = if (isEnglish) descriptionEn else descriptionTr
    fun getStrategyTip(isEnglish: Boolean): String = if (isEnglish) strategyTipEn else strategyTipTr
}

object BulletinOpportunityManager {

    /**
     * Oyun veritabanındaki (CityFacilityRegistry.productAllowedCities) kurallara göre
     * tesisin gerçekten kurulabildiği şehirlerle %100 uyumlu kâr fırsatları havuzu.
     */
    val OPPORTUNITY_TEMPLATES = listOf(
        // 1. Artvin - Kereste (timber) -> İzinli: kahramanmaras, artvin, mugla, bursa, kuala_lumpur
        BulletinOpportunity(
            id = "opp_artvin_timber",
            cityId = "artvin",
            cityNameTr = "Artvin",
            cityNameEn = "Artvin",
            category = CityEventCategory.AGRICULTURE,
            targetProductId = "timber",
            targetProductNameTr = "Kereste",
            targetProductNameEn = "Timber",
            targetFacilityId = "timber_camp",
            targetFacilityNameTr = "Kereste Kampı",
            targetFacilityNameEn = "Timber Camp",
            headlineTr = "🌲 Artvin Karadeniz Dağları Kereste İhracatı Seferberliği!",
            headlineEn = "🌲 Artvin Black Sea Mountains Timber Export Boom!",
            descriptionTr = "İnşaat ve mobilya sanayii yüksek kaliteli Artvin kerestesi için alım rekoru kırdı.",
            descriptionEn = "Construction and timber yards are buying up Artvin forestry produce at record prices.",
            strategyTipTr = "Artvin'de kereste kampı kurup tomrukları pazara arz ederek elmas kazanın!",
            strategyTipEn = "Establish timber camps in Artvin and supply lumber for high rewards!",
            priceMultiplier = 1.40f,
            targetQuantity = 60,
            diamondReward = 8,
            iconEmoji = "🌲"
        ),

        // 2. Artvin - Bakır (copper) -> İzinli: artvin, elazig, santiago, johannesburg
        BulletinOpportunity(
            id = "opp_artvin_copper",
            cityId = "artvin",
            cityNameTr = "Artvin",
            cityNameEn = "Artvin",
            category = CityEventCategory.MINING,
            targetProductId = "copper",
            targetProductNameTr = "Bakır",
            targetProductNameEn = "Copper",
            targetFacilityId = "copper_mine",
            targetFacilityNameTr = "Bakır Madeni",
            targetFacilityNameEn = "Copper Mine",
            headlineTr = "⛏️ Artvin Murgul Bakır Madenlerinde Rekor İhracat Talebi!",
            headlineEn = "⛏️ Artvin Murgul Copper Mines Global Demand Surge!",
            descriptionTr = "Elektrik-elektronik sanayii için yüksek saflıkta Artvin bakır cevheri aranıyor.",
            descriptionEn = "Electronics and cable industries demand high-grade copper ore from Artvin.",
            strategyTipTr = "Artvin'de bakır madeni açıp cevheri pazara veya haddehanelere teslim edin!",
            strategyTipEn = "Operate copper mines in Artvin and supply ore for massive margins!",
            priceMultiplier = 1.45f,
            targetQuantity = 50,
            diamondReward = 7,
            iconEmoji = "⛏️"
        ),

        // 3. Kahramanmaraş - Pamuk (cotton) -> İzinli: kahramanmaras, gaziantep, mersin, izmir
        BulletinOpportunity(
            id = "opp_kahramanmaras_cotton",
            cityId = "kahramanmaras",
            cityNameTr = "Kahramanmaraş",
            cityNameEn = "Kahramanmaras",
            category = CityEventCategory.AGRICULTURE,
            targetProductId = "cotton",
            targetProductNameTr = "Pamuk",
            targetProductNameEn = "Cotton",
            targetFacilityId = "cotton_farm",
            targetFacilityNameTr = "Pamuk Tarlası",
            targetFacilityNameEn = "Cotton Farm",
            headlineTr = "🧵 Kahramanmaraş Pamuk Hasadında Küresel Talep!",
            headlineEn = "🧵 Global Cotton Boom in Kahramanmaras!",
            descriptionTr = "Tekstil devleri Maraş pamuğu için rekor alım sözleşmeleri imzalıyor.",
            descriptionEn = "Textile giants are signing record purchase contracts for Maras cotton.",
            strategyTipTr = "Kahramanmaraş'ta pamuk üretin ve doğrudan pazarda rekor fiyata satın!",
            strategyTipEn = "Produce cotton in Kahramanmaras and sell on the market at a record premium!",
            priceMultiplier = 1.45f,
            targetQuantity = 50,
            diamondReward = 6,
            iconEmoji = "🧵"
        ),

        // 4. Kahramanmaraş - Dokuma Kumaş (fabric) -> İzinli: gaziantep, bursa, kahramanmaras, istanbul, izmir
        BulletinOpportunity(
            id = "opp_kahramanmaras_fabric",
            cityId = "kahramanmaras",
            cityNameTr = "Kahramanmaraş",
            cityNameEn = "Kahramanmaras",
            category = CityEventCategory.EXPORT,
            targetProductId = "fabric",
            targetProductNameTr = "Kumaş",
            targetProductNameEn = "Fabric",
            targetFacilityId = "textile_workshop",
            targetFacilityNameTr = "Dokuma Atölyesi",
            targetFacilityNameEn = "Textile Workshop",
            headlineTr = "🧶 Kahramanmaraş İplik ve Dokuma Sanayii Hamlesi!",
            headlineEn = "🧶 Kahramanmaras Yarn & Fabric Industrial Boom!",
            descriptionTr = "İhracatçı hazır giyim firmaları Maraş kumaşına yüksek alım primi tanımladı.",
            descriptionEn = "Apparel exporters have placed premium procurement contracts for Maras fabric.",
            strategyTipTr = "Kahramanmaraş'ta dokuma atölyesi kurup pamuğu kumaşa dönüştürün!",
            strategyTipEn = "Run weaving workshops in Maras and convert raw cotton into high-value fabric!",
            priceMultiplier = 1.40f,
            targetQuantity = 45,
            diamondReward = 7,
            iconEmoji = "🧶"
        ),

        // 5. Zonguldak - Taş Kömürü (coal) -> İzinli: zonguldak, sivas, essen, kirikkale
        BulletinOpportunity(
            id = "opp_zonguldak_coal",
            cityId = "zonguldak",
            cityNameTr = "Zonguldak",
            cityNameEn = "Zonguldak",
            category = CityEventCategory.MINING,
            targetProductId = "coal",
            targetProductNameTr = "Taş Kömürü",
            targetProductNameEn = "Hard Coal",
            targetFacilityId = "coal_mine",
            targetFacilityNameTr = "Kömür Madeni",
            targetFacilityNameEn = "Coal Mine",
            headlineTr = "⛏️ Zonguldak Ağır Sanayi Kömür Seferberliği!",
            headlineEn = "⛏️ Zonguldak Industrial Coal Rush!",
            descriptionTr = "Demir-çelik fırınları için yüksek kalorili Zonguldak taş kömürü aranıyor.",
            descriptionEn = "High-calorie Zonguldak hard coal is needed for steel blast furnaces.",
            strategyTipTr = "Zonguldak'ta kömür madeni işletip cevheri pazara ulaştırarak elmas kazanın!",
            strategyTipEn = "Operate coal mines in Zonguldak and deliver ore to market for diamond rewards!",
            priceMultiplier = 1.35f,
            targetQuantity = 80,
            diamondReward = 8,
            iconEmoji = "⛏️"
        ),

        // 6. Zonguldak - Çelik (steel) -> İzinli: istanbul, essen, zonguldak, sivas, london, rotterdam, kocaeli, bursa
        BulletinOpportunity(
            id = "opp_zonguldak_steel",
            cityId = "zonguldak",
            cityNameTr = "Zonguldak",
            cityNameEn = "Zonguldak",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "steel",
            targetProductNameTr = "Çelik",
            targetProductNameEn = "Steel",
            targetFacilityId = "steel_mill",
            targetFacilityNameTr = "Çelik Haddehanesi",
            targetFacilityNameEn = "Steel Mill",
            headlineTr = "🔩 Zonguldak Ereğli Çelik Üretim Rallisi!",
            headlineEn = "🔩 Zonguldak Eregli Steel Blast Furnace Surge!",
            descriptionTr = "Tersaneler ve mega inşaat projeleri için Zonguldak haddehanelerinden çelik aranıyor.",
            descriptionEn = "Shipyards and infrastructure mega-projects require thick steel plating from Zonguldak.",
            strategyTipTr = "Zonguldak'ta çelik haddehanesi açarak kâr fırsatını değerlendirin!",
            strategyTipEn = "Run steel rolling mills in Zonguldak for maximum margins!",
            priceMultiplier = 1.45f,
            targetQuantity = 40,
            diamondReward = 9,
            iconEmoji = "🔩"
        ),

        // 7. Batman - Ham Petrol (crude_oil) -> İzinli: batman, basra, houston, mersin, rotterdam, kocaeli
        BulletinOpportunity(
            id = "opp_batman_petroleum",
            cityId = "batman",
            cityNameTr = "Batman",
            cityNameEn = "Batman",
            category = CityEventCategory.MINING,
            targetProductId = "crude_oil",
            targetProductNameTr = "Ham Petrol",
            targetProductNameEn = "Crude Oil",
            targetFacilityId = "oil_well",
            targetFacilityNameTr = "Petrol Kuyusu",
            targetFacilityNameEn = "Oil Well",
            headlineTr = "🛢️ Batman Petrol Sahalarında Rekor Talep!",
            headlineEn = "🛢️ Crude Oil Rally in Batman Oilfields!",
            descriptionTr = "Tüpraş rafinerileri için Batman ham petrol sevkiyatı iki katına çıkarılıyor.",
            descriptionEn = "Refineries are doubling crude oil procurement orders from Batman.",
            strategyTipTr = "Batman'da petrol kuyusu kurup üretimi pazara veya rafinerilere satın!",
            strategyTipEn = "Drill oil wells in Batman and sell petroleum at high margins!",
            priceMultiplier = 1.40f,
            targetQuantity = 60,
            diamondReward = 9,
            iconEmoji = "🛢️"
        ),

        // 8. Batman - Rafine Yakıt (refined_fuel) -> İzinli: kocaeli, batman, sao_paulo, basra, mersin, rotterdam, houston, kirikkale
        BulletinOpportunity(
            id = "opp_batman_refined_fuel",
            cityId = "batman",
            cityNameTr = "Batman",
            cityNameEn = "Batman",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "refined_fuel",
            targetProductNameTr = "Rafine Yakıt",
            targetProductNameEn = "Refined Fuel",
            targetFacilityId = "petroleum_refinery",
            targetFacilityNameTr = "Petrol Rafinerisi",
            targetFacilityNameEn = "Petroleum Refinery",
            headlineTr = "⛽ Batman Rafinerisinde Akaryakıt Üretim Artışı!",
            headlineEn = "⛽ Batman Refinery Fuel Production Surge!",
            descriptionTr = "Güneydoğu lojistik koridoru için Batman rafinerisinden dizel ve benzin talep ediliyor.",
            descriptionEn = "Transit fleets require diesel and motor fuel batches directly from Batman refinery.",
            strategyTipTr = "Batman'da rafineri işletin ve akaryakıtı pazara sevk edin!",
            strategyTipEn = "Operate refineries in Batman and supply refined fuels for high diamond payouts!",
            priceMultiplier = 1.40f,
            targetQuantity = 40,
            diamondReward = 8,
            iconEmoji = "⛽"
        ),

        // 9. Sivas - Demir Cevheri (iron) -> İzinli: sivas, elazig, zonguldak, essen, johannesburg
        BulletinOpportunity(
            id = "opp_sivas_iron",
            cityId = "sivas",
            cityNameTr = "Sivas",
            cityNameEn = "Sivas",
            category = CityEventCategory.MINING,
            targetProductId = "iron",
            targetProductNameTr = "Demir",
            targetProductNameEn = "Iron",
            targetFacilityId = "iron_mine",
            targetFacilityNameTr = "Demir Madeni",
            targetFacilityNameEn = "Iron Mine",
            headlineTr = "🏗️ Sivas Madenlerinde Demir Çıkarma Rallisi!",
            headlineEn = "🏗️ Sivas Iron Mining Rush!",
            descriptionTr = "İnşaat ve altyapı projeleri için Sivas demir cevherine acil ihtiyaç var.",
            descriptionEn = "Massive infrastructure projects demand high quantities of Sivas iron ore.",
            strategyTipTr = "Sivas'ta demir madeni açarak yüksek fiyat çarpanından faydalanın!",
            strategyTipEn = "Open iron mines in Sivas to capitalize on high price premiums!",
            priceMultiplier = 1.40f,
            targetQuantity = 75,
            diamondReward = 7,
            iconEmoji = "🏗️"
        ),

        // 10. Sivas - Çimento (cement) -> İzinli: sivas, istanbul, essen, mersin, elazig, zonguldak
        BulletinOpportunity(
            id = "opp_sivas_cement",
            cityId = "sivas",
            cityNameTr = "Sivas",
            cityNameEn = "Sivas",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "cement",
            targetProductNameTr = "Çimento",
            targetProductNameEn = "Cement",
            targetFacilityId = "cement_factory",
            targetFacilityNameTr = "Çimento Fabrikası",
            targetFacilityNameEn = "Cement Factory",
            headlineTr = "🧱 Sivas Altyapı Yatırımları İçin Çimento Talebi!",
            headlineEn = "🧱 Sivas Infrastructure Boom Cement Demand!",
            descriptionTr = "Yüksek hızlı tren ve otoyol şantiyeleri için Sivas kireçtaşıyla üretilen çimento aranıyor.",
            descriptionEn = "High-speed rail and highway worksites contract large cement volumes from Sivas.",
            strategyTipTr = "Sivas'ta çimento fabrikası açıp üretimi şantiyelere ulaştırın!",
            strategyTipEn = "Run cement works in Sivas and supply ongoing infrastructure works!",
            priceMultiplier = 1.35f,
            targetQuantity = 70,
            diamondReward = 6,
            iconEmoji = "🧱"
        ),

        // 11. Konya - Alüminyum & Boksit (aluminum) -> İzinli: konya, eskisehir, santiago, essen
        BulletinOpportunity(
            id = "opp_konya_aluminum",
            cityId = "konya",
            cityNameTr = "Konya",
            cityNameEn = "Konya",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "aluminum",
            targetProductNameTr = "Alüminyum",
            targetProductNameEn = "Aluminum",
            targetFacilityId = "aluminum_mine",
            targetFacilityNameTr = "Alüminyum Madeni",
            targetFacilityNameEn = "Aluminum Mine",
            headlineTr = "⚙️ Konya Seydişehir Alüminyum İhracat Siparişi!",
            headlineEn = "⚙️ Konya Aluminum Export Drive!",
            descriptionTr = "Otomotiv ve raylı sistemler Konya alüminyum üreticilerine yöneldi.",
            descriptionEn = "Automotive and rail systems turn to Konya aluminum producers.",
            strategyTipTr = "Konya madenlerinden alüminyum üretip pazarda yüksek kârla satın!",
            strategyTipEn = "Produce aluminum in Konya and sell on the market for great margins!",
            priceMultiplier = 1.35f,
            targetQuantity = 60,
            diamondReward = 6,
            iconEmoji = "⚙️"
        ),

        // 12. Konya - Güneş Paneli (solar_panel) -> İzinli: konya, shanghai, ankara, rotterdam, mersin, izmir
        BulletinOpportunity(
            id = "opp_konya_solar",
            cityId = "konya",
            cityNameTr = "Konya",
            cityNameEn = "Konya",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "solar_panel",
            targetProductNameTr = "Güneş Paneli",
            targetProductNameEn = "Solar Panel",
            targetFacilityId = "solar_panel_factory",
            targetFacilityNameTr = "Güneş Paneli Fabrikası",
            targetFacilityNameEn = "Solar Panel Factory",
            headlineTr = "☀️ Konya Karapınar Temiz Enerji Güneş Paneli Hamlesi!",
            headlineEn = "☀️ Konya Clean Energy Solar Panel Expansion!",
            descriptionTr = "Güneş tarlaları için yüksek verimli fotovoltaik Konya güneş panelleri aranıyor.",
            descriptionEn = "Renewable megaprojects require high-efficiency photovoltaic panels from Konya.",
            strategyTipTr = "Konya'da güneş paneli fabrikası kurun ve temiz enerji priminden kazanın!",
            strategyTipEn = "Establish solar panel facilities in Konya to profit from energy subsidies!",
            priceMultiplier = 1.50f,
            targetQuantity = 30,
            diamondReward = 10,
            iconEmoji = "☀️"
        ),

        // 13. Muğla - Silisyum & Kuvars (silicon) -> İzinli: mugla, izmir, new_york, santiago, tokyo
        BulletinOpportunity(
            id = "opp_mugla_silicon",
            cityId = "mugla",
            cityNameTr = "Muğla",
            cityNameEn = "Mugla",
            category = CityEventCategory.MINING,
            targetProductId = "silicon",
            targetProductNameTr = "Silisyum",
            targetProductNameEn = "Silicon",
            targetFacilityId = "silicon_mine",
            targetFacilityNameTr = "Silisyum Madeni",
            targetFacilityNameEn = "Silicon Mine",
            headlineTr = "⚡ Muğla Yatağan Saf Silisyum & Teknoloji Cevheri!",
            headlineEn = "⚡ Mugla Pure Silicon Tech Ore Demand!",
            descriptionTr = "Mikroelektronik ve yarıiletken üreticileri Muğla silisyumu için sıraya girdi.",
            descriptionEn = "Semiconductor fabricators queue up for high-purity Mugla quartz and silicon ore.",
            strategyTipTr = "Muğla'da silisyum madeni işletip teknoloji sanayisine yüksek kârla satın!",
            strategyTipEn = "Mine silicon in Mugla and export to global chipmakers!",
            priceMultiplier = 1.45f,
            targetQuantity = 50,
            diamondReward = 8,
            iconEmoji = "⚡"
        ),

        // 14. Muğla - Cam Sanayii (glass) -> İzinli: bursa, mugla, izmir, london, istanbul, mersin
        BulletinOpportunity(
            id = "opp_mugla_glass",
            cityId = "mugla",
            cityNameTr = "Muğla",
            cityNameEn = "Mugla",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "glass",
            targetProductNameTr = "Cam",
            targetProductNameEn = "Glass",
            targetFacilityId = "glass_factory",
            targetFacilityNameTr = "Cam Fabrikası",
            targetFacilityNameEn = "Glass Factory",
            headlineTr = "🪟 Muğla Ege Cam Ambalaj ve Mimari Cam İhracatı!",
            headlineEn = "🪟 Mugla Architectural Glass Export Deal!",
            descriptionTr = "Akdeniz ve Ege turizm tesisleri mimari cam ihtiyacı için Muğla üreticilerini seçti.",
            descriptionEn = "Resorts and commercial projects purchase structural glass from Mugla.",
            strategyTipTr = "Muğla'da cam fabrikası işletip silisyumu işlenmiş cama dönüştürün!",
            strategyTipEn = "Turn local quartz into tempered glass in Mugla for prime returns!",
            priceMultiplier = 1.35f,
            targetQuantity = 50,
            diamondReward = 6,
            iconEmoji = "🪟"
        ),

        // 15. Gaziantep - Dokuma Kumaş (fabric) -> İzinli: gaziantep, bursa, kahramanmaras, istanbul, izmir
        BulletinOpportunity(
            id = "opp_gaziantep_fabric",
            cityId = "gaziantep",
            cityNameTr = "Gaziantep",
            cityNameEn = "Gaziantep",
            category = CityEventCategory.EXPORT,
            targetProductId = "fabric",
            targetProductNameTr = "Kumaş",
            targetProductNameEn = "Fabric",
            targetFacilityId = "textile_workshop",
            targetFacilityNameTr = "Dokuma Atölyesi",
            targetFacilityNameEn = "Textile Workshop",
            headlineTr = "🧶 Gaziantep Uluslararası Tekstil & Kumaş Sevkiyatı!",
            headlineEn = "🧶 Gaziantep Textile & Fabric Export Deal!",
            descriptionTr = "Ortadoğu ve Avrupa için Gaziantep dokuma kumaş siparişleri tırmandı.",
            descriptionEn = "European apparel brands have increased Gaziantep fabric orders.",
            strategyTipTr = "Gaziantep'te dokuma atölyesi kurup pamukları kumaşa dönüştürün!",
            strategyTipEn = "Run textile workshops in Gaziantep to turn cotton into high-value fabric!",
            priceMultiplier = 1.40f,
            targetQuantity = 45,
            diamondReward = 7,
            iconEmoji = "🧶"
        ),

        // 16. Kocaeli - Kimyasallar (chemicals) -> İzinli: kirikkale, gaziantep, kocaeli, new_york, mersin, rotterdam, sao_paulo, basra
        BulletinOpportunity(
            id = "opp_kocaeli_chemicals",
            cityId = "kocaeli",
            cityNameTr = "Kocaeli",
            cityNameEn = "Kocaeli",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "chemicals",
            targetProductNameTr = "Kimyasallar",
            targetProductNameEn = "Chemicals",
            targetFacilityId = "chemical_plant",
            targetFacilityNameTr = "Kimya Tesisi",
            targetFacilityNameEn = "Chemical Plant",
            headlineTr = "🧪 Kocaeli Sanayi Limanında Temel Kimyasal Madde Talebi!",
            headlineEn = "🧪 Kocaeli Industrial Chemical Boom!",
            descriptionTr = "Sanayi tesisleri için temel endüstriyel kimyasallar yüksek primle satın alınıyor.",
            descriptionEn = "Manufacturing plants are paying high premiums for Kocaeli industrial chemicals.",
            strategyTipTr = "Kocaeli'de kimya tesisi kurun ve üretimi pazara sürün!",
            strategyTipEn = "Establish chemical plants in Kocaeli and supply industrial parks!",
            priceMultiplier = 1.40f,
            targetQuantity = 60,
            diamondReward = 8,
            iconEmoji = "🧪"
        ),

        // 17. Kocaeli - Plastik (plastic) -> İzinli: kocaeli, sao_paulo, gaziantep, mersin, london, rotterdam, istanbul, batman
        BulletinOpportunity(
            id = "opp_kocaeli_plastic",
            cityId = "kocaeli",
            cityNameTr = "Kocaeli",
            cityNameEn = "Kocaeli",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "plastic",
            targetProductNameTr = "Plastik",
            targetProductNameEn = "Plastic",
            targetFacilityId = "plastic_factory",
            targetFacilityNameTr = "Plastik Fabrikası",
            targetFacilityNameEn = "Plastic Factory",
            headlineTr = "🧴 Kocaeli Körfez Plastik ve Polimer Üretim Patlaması!",
            headlineEn = "🧴 Kocaeli Polymers & Plastics Surge!",
            descriptionTr = "Ambalaj ve otomotiv yedek parça kalıpları için Kocaeli polimerlerine ihtiyaç duyuluyor.",
            descriptionEn = "Packaging and automotive injection molders demand large plastic supplies.",
            strategyTipTr = "Kocaeli'de plastik fabrikası işletip üretimi pazara veya fabrikalara satın!",
            strategyTipEn = "Operate plastic factories in Kocaeli to fulfill high-volume orders!",
            priceMultiplier = 1.35f,
            targetQuantity = 50,
            diamondReward = 6,
            iconEmoji = "🧴"
        ),

        // 18. Bursa - Ağır Çelik Sanayii (steel) -> İzinli: istanbul, essen, zonguldak, sivas, london, rotterdam, kocaeli, bursa
        BulletinOpportunity(
            id = "opp_bursa_steel",
            cityId = "bursa",
            cityNameTr = "Bursa",
            cityNameEn = "Bursa",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "steel",
            targetProductNameTr = "Çelik",
            targetProductNameEn = "Steel",
            targetFacilityId = "steel_mill",
            targetFacilityNameTr = "Çelik Haddehanesi",
            targetFacilityNameEn = "Steel Mill",
            headlineTr = "🚗 Bursa Otomotiv Sanayi Özel Çelik Tedarik Çağrısı!",
            headlineEn = "🚗 Bursa Automotive Steel Procurement!",
            descriptionTr = "Otomotiv montaj hatları için yüksek mukavemetli Bursa çeliği talep ediliyor.",
            descriptionEn = "Automotive assembly lines require high-strength steel sheets from Bursa.",
            strategyTipTr = "Bursa'da çelik haddehanesi işletin ve sanayiye çelik satın!",
            strategyTipEn = "Operate a steel mill in Bursa and deliver steel for high diamond payouts!",
            priceMultiplier = 1.45f,
            targetQuantity = 40,
            diamondReward = 9,
            iconEmoji = "🚗"
        ),

        // 19. Bursa - Otomotiv Yan Sanayi (auto_part) -> İzinli: bursa, frankfurt, tokyo, kocaeli, london, eskisehir
        BulletinOpportunity(
            id = "opp_bursa_autoparts",
            cityId = "bursa",
            cityNameTr = "Bursa",
            cityNameEn = "Bursa",
            category = CityEventCategory.EXPORT,
            targetProductId = "auto_part",
            targetProductNameTr = "Oto Parça",
            targetProductNameEn = "Auto Parts",
            targetFacilityId = "auto_part_factory",
            targetFacilityNameTr = "Otomotiv Parça Fabrikası",
            targetFacilityNameEn = "Auto Parts Factory",
            headlineTr = "🚘 Bursa Otomotiv Üssü Parça Üretim Seferberliği!",
            headlineEn = "🚘 Bursa Automotive Hub Component Export Deal!",
            descriptionTr = "Avrupa ana otomotiv üreticileri Bursa menşeili fren ve şasi aksamları için sipariş verdi.",
            descriptionEn = "European carmakers contracted brake and chassis components from Bursa.",
            strategyTipTr = "Bursa'da oto parça tesisi kurup çelikleri parçaya dönüştürün!",
            strategyTipEn = "Manufacture auto components in Bursa and export to automotive brands!",
            priceMultiplier = 1.45f,
            targetQuantity = 35,
            diamondReward = 9,
            iconEmoji = "🚘"
        ),

        // 20. Eskişehir - Ağır Makine (machinery) -> İzinli: london, frankfurt, eskisehir, bursa, essen, ankara, kocaeli
        BulletinOpportunity(
            id = "opp_eskisehir_machinery",
            cityId = "eskisehir",
            cityNameTr = "Eskişehir",
            cityNameEn = "Eskisehir",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "machinery",
            targetProductNameTr = "Sanayi Makinesi",
            targetProductNameEn = "Machinery",
            targetFacilityId = "machinery_factory",
            targetFacilityNameTr = "Makine Fabrikası",
            targetFacilityNameEn = "Machinery Factory",
            headlineTr = "🚜 Eskişehir Raylı Sistemler & Ağır Makine Siparişi!",
            headlineEn = "🚜 Eskisehir Industrial Machinery & Rail Order!",
            descriptionTr = "Lokomotif ve iş makineleri için Eskişehir menşeili sanayi motor ve mekanizmaları aranıyor.",
            descriptionEn = "Locomotive and construction builders contract machinery sets from Eskisehir.",
            strategyTipTr = "Eskişehir'de makine fabrikası kurup çelikleri ağır sanayi makinelerine dönüştürün!",
            strategyTipEn = "Produce industrial machinery in Eskisehir for premium diamond rewards!",
            priceMultiplier = 1.50f,
            targetQuantity = 25,
            diamondReward = 10,
            iconEmoji = "🚜"
        ),

        // 21. Mersin - Rafine Yakıt (refined_fuel) -> İzinli: kocaeli, batman, sao_paulo, basra, mersin, rotterdam, houston, kirikkale
        BulletinOpportunity(
            id = "opp_mersin_refined_fuel",
            cityId = "mersin",
            cityNameTr = "Mersin",
            cityNameEn = "Mersin",
            category = CityEventCategory.LOGISTICS,
            targetProductId = "refined_fuel",
            targetProductNameTr = "Rafine Yakıt",
            targetProductNameEn = "Refined Fuel",
            targetFacilityId = "petroleum_refinery",
            targetFacilityNameTr = "Petrol Rafinerisi",
            targetFacilityNameEn = "Petroleum Refinery",
            headlineTr = "⛽ Mersin Uluslararası Akdeniz Limanı Yakıt İkmali!",
            headlineEn = "⛽ Mersin Mediterranean Hub Marine Bunkering!",
            descriptionTr = "Transit gemiler ve konteyner filoları Mersin limanından rafine yakıt ikmali talep ediyor.",
            descriptionEn = "Container carriers and freighters contract marine fuels from Mersin terminal.",
            strategyTipTr = "Mersin'de rafineri işletin ve deniz ticaretine akaryakıt sağlayın!",
            strategyTipEn = "Operate refineries in Mersin to supply bunker fuels to shipping lines!",
            priceMultiplier = 1.40f,
            targetQuantity = 45,
            diamondReward = 8,
            iconEmoji = "⛽"
        ),

        // 22. Essen (Almanya - 🇩🇪) - Taş Kömürü (coal) -> İzinli: zonguldak, sivas, essen, kirikkale
        BulletinOpportunity(
            id = "opp_essen_coal",
            cityId = "essen",
            cityNameTr = "Essen",
            cityNameEn = "Essen",
            category = CityEventCategory.MINING,
            targetProductId = "coal",
            targetProductNameTr = "Taş Kömürü",
            targetProductNameEn = "Hard Coal",
            targetFacilityId = "coal_mine",
            targetFacilityNameTr = "Kömür Madeni",
            targetFacilityNameEn = "Coal Mine",
            headlineTr = "⛏️ Essen Ruhr Havzası Taş Kömürü İkmal Çağrısı!",
            headlineEn = "⛏️ Essen Ruhr Basin Coal Procurement Drive!",
            descriptionTr = "Alman sanayi fırınları için Ruhr kömür sahalarından yüksek kalorili ikmal aranıyor.",
            descriptionEn = "German heavy industry contracts high-calorie coking coal from Essen.",
            strategyTipTr = "Essen'de kömür madeni açıp Ruhr sanayi devlerine tedarik sağlayın!",
            strategyTipEn = "Establish coal mines in Essen and supply German steel producers!",
            priceMultiplier = 1.35f,
            targetQuantity = 80,
            diamondReward = 7,
            iconEmoji = "⛏️"
        ),

        // 23. Rotterdam (Hollanda - 🇳🇱) - Rafine Yakıt (refined_fuel) -> İzinli: kocaeli, batman, sao_paulo, basra, mersin, rotterdam, houston, kirikkale
        BulletinOpportunity(
            id = "opp_rotterdam_fuel",
            cityId = "rotterdam",
            cityNameTr = "Rotterdam",
            cityNameEn = "Rotterdam",
            category = CityEventCategory.LOGISTICS,
            targetProductId = "refined_fuel",
            targetProductNameTr = "Rafine Yakıt",
            targetProductNameEn = "Refined Fuel",
            targetFacilityId = "petroleum_refinery",
            targetFacilityNameTr = "Petrol Rafinerisi",
            targetFacilityNameEn = "Petroleum Refinery",
            headlineTr = "⛽ Rotterdam Kuzey Denizi Denizcilik Yakıtı Sözleşmesi!",
            headlineEn = "⛽ Rotterdam North Sea Marine Bunkering Surge!",
            descriptionTr = "Avrupa'nın en büyük limanında dev yük gemileri için rafine akaryakıt ikmali yapılıyor.",
            descriptionEn = "Europe's biggest mega-port requires ultra-low sulfur marine fuels.",
            strategyTipTr = "Rotterdam'da rafineri işletin ve küresel tanker filolarına yakıt satın!",
            strategyTipEn = "Operate refineries in Rotterdam for lucrative international margins!",
            priceMultiplier = 1.45f,
            targetQuantity = 50,
            diamondReward = 9,
            iconEmoji = "⛽"
        ),

        // 24. Santiago (Şili - 🇨🇱) - Lityum & Bakır (lithium) -> İzinli: santiago, konya, johannesburg
        BulletinOpportunity(
            id = "opp_santiago_lithium",
            cityId = "santiago",
            cityNameTr = "Santiago",
            cityNameEn = "Santiago",
            category = CityEventCategory.MINING,
            targetProductId = "lithium",
            targetProductNameTr = "Lityum",
            targetProductNameEn = "Lithium",
            targetFacilityId = "lithium_extraction_facility",
            targetFacilityNameTr = "Lityum Tesisi",
            targetFacilityNameEn = "Lithium Plant",
            headlineTr = "🔋 Santiago Atacama Lityum Batarya Hammaddesi Çağrısı!",
            headlineEn = "🔋 Santiago Atacama Lithium Battery Materials Rush!",
            descriptionTr = "Küresel elektrikli araç üreticileri Santiago lityum rezervleri için alım sözleşmesi açtı.",
            descriptionEn = "Global EV battery manufacturers contract high-purity lithium from Santiago.",
            strategyTipTr = "Santiago'da lityum çıkarma tesisi kurup batarya üreticilerine ulaştırın!",
            strategyTipEn = "Extract lithium in Santiago to fuel the global green transition!",
            priceMultiplier = 1.50f,
            targetQuantity = 40,
            diamondReward = 10,
            iconEmoji = "🔋"
        ),

        // 25. Houston (ABD - 🇺🇸) - Ham Petrol (crude_oil) -> İzinli: batman, basra, houston, mersin, rotterdam, kocaeli
        BulletinOpportunity(
            id = "opp_houston_crude_oil",
            cityId = "houston",
            cityNameTr = "Houston",
            cityNameEn = "Houston",
            category = CityEventCategory.MINING,
            targetProductId = "crude_oil",
            targetProductNameTr = "Ham Petrol",
            targetProductNameEn = "Crude Oil",
            targetFacilityId = "oil_well",
            targetFacilityNameTr = "Petrol Kuyusu",
            targetFacilityNameEn = "Oil Well",
            headlineTr = "🛢️ Houston Teksas Körfezi Ham Petrol Üretim Rallisi!",
            headlineEn = "🛢️ Houston Texas Gulf Coast Crude Oil Boom!",
            descriptionTr = "Teksas kıyı terminalleri ve küresel rafineriler için Houston petrolü alınıyor.",
            descriptionEn = "Gulf coast refineries are procuring heavy crude batches at premium pricing.",
            strategyTipTr = "Houston'da petrol kuyusu açıp küresel enerji borsasında satın!",
            strategyTipEn = "Drill in Houston and sell crude on international commodity desks!",
            priceMultiplier = 1.45f,
            targetQuantity = 60,
            diamondReward = 9,
            iconEmoji = "🛢️"
        ),

        // 26. Kuala Lumpur (Malezya - 🇲🇾) - Doğal Kauçuk (rubber_latex) -> İzinli: kuala_lumpur, sao_paulo, kahramanmaras
        BulletinOpportunity(
            id = "opp_kuala_lumpur_rubber",
            cityId = "kuala_lumpur",
            cityNameTr = "Kuala Lumpur",
            cityNameEn = "Kuala Lumpur",
            category = CityEventCategory.AGRICULTURE,
            targetProductId = "rubber_latex",
            targetProductNameTr = "Doğal Kauçuk",
            targetProductNameEn = "Natural Rubber",
            targetFacilityId = "rubber_plantation",
            targetFacilityNameTr = "Kauçuk Plantasyonu",
            targetFacilityNameEn = "Rubber Plantation",
            headlineTr = "🌳 Kuala Lumpur Malakka Boğazı Kauçuk İhracat Hamlesi!",
            headlineEn = "🌳 Kuala Lumpur Malacca Strait Rubber Export Boom!",
            descriptionTr = "Otomotiv lastik ve polimer sanayii için Güneydoğu Asya'dan rekor lateks alımı yapılıyor.",
            descriptionEn = "Global tire and polymer industries contract record natural rubber batches from Kuala Lumpur.",
            strategyTipTr = "Kuala Lumpur'da kauçuk plantasyonu kurup ham lateksi pazara teslim edin!",
            strategyTipEn = "Run rubber plantations in Kuala Lumpur and export raw latex for premium diamond returns!",
            priceMultiplier = 1.45f,
            targetQuantity = 50,
            diamondReward = 8,
            iconEmoji = "🌳"
        ),

        // 27. Johannesburg (Güney Afrika - 🇿🇦) - Grafit & Değerli Metal (graphite_ore) -> İzinli: johannesburg, new_york, eskisehir, shanghai
        BulletinOpportunity(
            id = "opp_johannesburg_graphite",
            cityId = "johannesburg",
            cityNameTr = "Johannesburg",
            cityNameEn = "Johannesburg",
            category = CityEventCategory.MINING,
            targetProductId = "graphite_ore",
            targetProductNameTr = "Grafit Cevheri",
            targetProductNameEn = "Graphite Ore",
            targetFacilityId = "graphite_mine",
            targetFacilityNameTr = "Grafit Madeni",
            targetFacilityNameEn = "Graphite Mine",
            headlineTr = "⛏️ Johannesburg Witwatersrand Grafit & Nadir Element Seferberliği!",
            headlineEn = "⛏️ Johannesburg Graphite & Rare Ore Mining Drive!",
            descriptionTr = "Küresel batarya ve havacılık sanayii Johannesburg yüksek saflıktaki grafit rezervlerine yöneldi.",
            descriptionEn = "Battery and aerospace giants procure high-purity graphite directly from Johannesburg mines.",
            strategyTipTr = "Johannesburg'da grafit madeni işletip küresel emtia pazarına ulaştırın!",
            strategyTipEn = "Operate graphite mines in Johannesburg and supply high-tech battery makers!",
            priceMultiplier = 1.50f,
            targetQuantity = 45,
            diamondReward = 9,
            iconEmoji = "⛏️"
        ),

        // 28. Basra (Irak - 🇮🇶) - Doğal Gaz & Enerji (natural_gas) -> İzinli: batman, basra, houston, new_york, rotterdam, mersin
        BulletinOpportunity(
            id = "opp_basra_gas",
            cityId = "basra",
            cityNameTr = "Basra",
            cityNameEn = "Basra",
            category = CityEventCategory.MINING,
            targetProductId = "natural_gas",
            targetProductNameTr = "Doğal Gaz",
            targetProductNameEn = "Natural Gas",
            targetFacilityId = "gas_field",
            targetFacilityNameTr = "Gaz Sondaj Sahası",
            targetFacilityNameEn = "Gas Extraction Facility",
            headlineTr = "🔥 Basra Körfez Gaz Sahalarında Dev İhracat Anlaşması!",
            headlineEn = "🔥 Basra Gulf Gas Extraction Export Surge!",
            descriptionTr = "Boru hatları ve sıvılaştırılmış gaz terminalleri Basra gaz üretimini sonuna kadar çekiyor.",
            descriptionEn = "Pipeline corridors and LNG terminals are contracting full Basra gas production capacity.",
            strategyTipTr = "Basra'da gaz sondaj sahası kurup enerji borsasına sevk edin!",
            strategyTipEn = "Develop gas extraction in Basra to profit from surging international energy markets!",
            priceMultiplier = 1.40f,
            targetQuantity = 70,
            diamondReward = 8,
            iconEmoji = "🔥"
        ),

        // 29. Tokyo (Japonya - 🇯🇵) - Robotik Kol & Otomasyon (robotics_arm) -> İzinli: tokyo, frankfurt, shanghai, izmir, london, ankara
        BulletinOpportunity(
            id = "opp_tokyo_robotics",
            cityId = "tokyo",
            cityNameTr = "Tokyo",
            cityNameEn = "Tokyo",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "robotics_arm",
            targetProductNameTr = "Robotik Kol",
            targetProductNameEn = "Robotic Arm",
            targetFacilityId = "robotics_factory",
            targetFacilityNameTr = "Robotik Fabrikası",
            targetFacilityNameEn = "Robotics Factory",
            headlineTr = "🤖 Tokyo Hassas Otomasyon & Sanayi Robotu Sözleşmesi!",
            headlineEn = "🤖 Tokyo Precision Industrial Robotics Mega-Order!",
            descriptionTr = "Gelişmiş yarıiletken ve akıllı fabrika hatları Tokyo üretimi robot kollarla donatılıyor.",
            descriptionEn = "Next-gen semiconductor fabs and automotive lines require Tokyo precision robotic systems.",
            strategyTipTr = "Tokyo'da robotik tesisi kurup küresel fabrikalara yüksek kârla satın!",
            strategyTipEn = "Build robotics manufacturing in Tokyo and sell high-tier systems for massive rewards!",
            priceMultiplier = 1.50f,
            targetQuantity = 20,
            diamondReward = 10,
            iconEmoji = "🤖"
        ),

        // 30. Frankfurt (Almanya - 🇩🇪) - Hassas Otomotiv Parça (auto_part) -> İzinli: bursa, frankfurt, tokyo, kocaeli, london, eskisehir
        BulletinOpportunity(
            id = "opp_frankfurt_autoparts",
            cityId = "frankfurt",
            cityNameTr = "Frankfurt",
            cityNameEn = "Frankfurt",
            category = CityEventCategory.EXPORT,
            targetProductId = "auto_part",
            targetProductNameTr = "Oto Parça",
            targetProductNameEn = "Auto Parts",
            targetFacilityId = "auto_part_factory",
            targetFacilityNameTr = "Otomotiv Parça Fabrikası",
            targetFacilityNameEn = "Auto Parts Factory",
            headlineTr = "🏎️ Frankfurt Otoban & Yürür Aksam Parça Tedarik Hamlesi!",
            headlineEn = "🏎️ Frankfurt Autobahn Chassis & Components Contract!",
            descriptionTr = "Alman premium otomobil üreticileri Frankfurt tesislerinden yürür aksam siparişi verdi.",
            descriptionEn = "German car manufacturers have opened high-margin orders for Frankfurt chassis components.",
            strategyTipTr = "Frankfurt'ta parça tesisi işletip Avrupa otomotiv hatlarına ulaştırın!",
            strategyTipEn = "Produce specialized auto components in Frankfurt for premium returns!",
            priceMultiplier = 1.45f,
            targetQuantity = 35,
            diamondReward = 9,
            iconEmoji = "🏎️"
        ),

        // 31. Şanghay (Çin - 🇨🇳) - Giga Batarya (battery) -> İzinli: shanghai, santiago, kocaeli, tokyo, new_york, london, konya, ankara
        BulletinOpportunity(
            id = "opp_shanghai_battery",
            cityId = "shanghai",
            cityNameTr = "Şanghay",
            cityNameEn = "Shanghai",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "battery",
            targetProductNameTr = "Batarya Paketi",
            targetProductNameEn = "Battery Pack",
            targetFacilityId = "battery_gigafactory",
            targetFacilityNameTr = "Batarya Fabrikası",
            targetFacilityNameEn = "Battery Factory",
            headlineTr = "🔋 Şanghay Küresel Elektrikli Araç Batarya Sipariş Rekoru!",
            headlineEn = "🔋 Shanghai EV Battery Pack Mega-Procurement!",
            descriptionTr = "Liman terminalleri ve küresel EV konsorsiyumları Şanghay üretimi bataryalar için sıraya girdi.",
            descriptionEn = "EV manufacturers contract high-density lithium battery cells from Shanghai gigafactories.",
            strategyTipTr = "Şanghay'da batarya fabrikası işletin ve küresel temiz enerji devriminden kazanın!",
            strategyTipEn = "Produce battery packs in Shanghai to fulfill massive international automotive demand!",
            priceMultiplier = 1.45f,
            targetQuantity = 40,
            diamondReward = 9,
            iconEmoji = "🔋"
        ),

        // 32. Londra (İngiltere - 🇬🇧) - Ağır Sanayi Makinesi (machinery) -> İzinli: london, frankfurt, eskisehir, bursa, essen, ankara, kocaeli
        BulletinOpportunity(
            id = "opp_london_machinery",
            cityId = "london",
            cityNameTr = "Londra",
            cityNameEn = "London",
            category = CityEventCategory.INDUSTRY,
            targetProductId = "machinery",
            targetProductNameTr = "Sanayi Makinesi",
            targetProductNameEn = "Machinery",
            targetFacilityId = "machinery_factory",
            targetFacilityNameTr = "Makine Fabrikası",
            targetFacilityNameEn = "Machinery Factory",
            headlineTr = "⚙️ Londra Thames Lojistik Hattı Ağır Makine Tedariki!",
            headlineEn = "⚙️ London Heavy Engineering Machinery Export Deal!",
            descriptionTr = "Uluslararası altyapı müteahhitleri Londra mühendislik fabrikalarından iş makinesi alıyor.",
            descriptionEn = "International infrastructure contractors place bulk machinery orders with London plants.",
            strategyTipTr = "Londra'da makine tesisi kurup ağır sanayi ürünlerini ihraç edin!",
            strategyTipEn = "Manufacture heavy industrial machinery in London for top diamond payouts!",
            priceMultiplier = 1.50f,
            targetQuantity = 25,
            diamondReward = 10,
            iconEmoji = "⚙️"
        )
    )

    /**
     * Veritabanı tutarlılık kontrolü:
     * 1. Şehir, CityData.cities listesinde mevcut mu? (Rize gibi legacy/olmayan şehirleri eler)
     * 2. Ürün, CityFacilityRegistry.productAllowedCities tablosuna göre bu şehirde kurulabilir mi?
     * Kesin olarak Rize elenir, Artvin kereste/bakır, Zonguldak kömür/çelik vb. onaylanır.
     */
    fun isTemplateValid(opp: BulletinOpportunity): Boolean {
        val cityIdClean = opp.cityId.lowercase().trim()
        val cityExists = cities.any { it.id.equals(cityIdClean, ignoreCase = true) }
        if (!cityExists) return false

        val allowedCities = CityFacilityRegistry.productAllowedCities[opp.targetProductId] ?: return true
        return allowedCities.isEmpty() || allowedCities.contains(cityIdClean)
    }

    /**
     * Geliştirici & test doğrulaması: Tüm şablonların veritabanı kurallarına uyduğunu teyit eder.
     */
    fun validateAllTemplates(): Boolean {
        return OPPORTUNITY_TEMPLATES.all { isTemplateValid(it) }
    }

    /**
     * Oyuna girişte 3 farklı şehirden aktif fırsat seti oluşturur.
     */
    fun generateInitialOpportunities(): List<BulletinOpportunity> {
        val validTemplates = OPPORTUNITY_TEMPLATES.filter { isTemplateValid(it) }
        val shuffled = validTemplates.shuffled()
        val distinctCities = mutableListOf<BulletinOpportunity>()
        val seenCities = mutableSetOf<String>()

        for (item in shuffled) {
            if (item.cityId !in seenCities) {
                seenCities.add(item.cityId)
                distinctCities.add(item)
                if (distinctCities.size == 3) break
            }
        }

        // Eğer 3 farklı şehir çıkmazsa ilk 3 tanesini al
        val selected = if (distinctCities.size == 3) distinctCities else shuffled.take(3)
        return selected.map { opp ->
            // Randomize target quantity slightly for replayability
            val randomizedQty = (opp.targetQuantity * Random.nextDouble(0.8, 1.2)).toInt().coerceIn(25, 120)
            val randomizedDiamonds = Random.nextInt(3, 11) // 3 ile 10 elmas
            opp.copy(
                targetQuantity = randomizedQty,
                producedQuantity = 0,
                soldQuantity = 0,
                diamondReward = randomizedDiamonds,
                isClaimed = false
            )
        }
    }

    /**
     * Oyuncunun tesisinde üretim tamamlandığında bülten fırsatındaki üretim ilerlemesini günceller.
     */
    fun onProductionCompleted(
        productId: String,
        cityId: String,
        quantity: Int,
        current: List<BulletinOpportunity>
    ): List<BulletinOpportunity> {
        if (quantity <= 0) return current
        var modified = false
        val updated = current.map { opp ->
            val matchesProduct = opp.targetProductId.equals(productId, ignoreCase = true)
            val matchesCity = opp.cityId.equals(cityId, ignoreCase = true) || opp.cityId.isBlank()
            if (matchesProduct && matchesCity && !opp.isCompleted && !opp.isClaimed) {
                modified = true
                val newProd = (opp.producedQuantity + quantity).coerceAtMost(opp.targetQuantity)
                opp.copy(producedQuantity = newProd)
            } else {
                opp
            }
        }
        return if (modified) updated else current
    }

    /**
     * Oyuncu pazarda veya borsada mal sattığında satış ilerlemesini günceller.
     */
    fun onGoodsSold(
        productId: String,
        cityId: String,
        quantity: Int,
        current: List<BulletinOpportunity>
    ): List<BulletinOpportunity> {
        if (quantity <= 0) return current
        var modified = false
        val updated = current.map { opp ->
            val matchesProduct = opp.targetProductId.equals(productId, ignoreCase = true)
            // Satış şehri eşleşiyorsa veya global pazar ise
            val matchesCity = opp.cityId.equals(cityId, ignoreCase = true) || cityId.isBlank() || cityId == "global"
            if (matchesProduct && matchesCity && !opp.isCompleted && !opp.isClaimed) {
                modified = true
                val newSold = (opp.soldQuantity + quantity).coerceAtMost(opp.targetQuantity)
                // Satış miktarı üretilmiş olandan yüksekse üretim miktarı da otomatik olarak en az satılan kadar sayılır
                val newProd = maxOf(opp.producedQuantity, newSold)
                opp.copy(soldQuantity = newSold, producedQuantity = newProd)
            } else {
                opp
            }
        }
        return if (modified) updated else current
    }

    /**
     * Tamamlanan fırsatın ödülünü verir ve yerine başka bir şehirden yeni bir fırsat getirir.
     * Döndürülen: Pair(güncellenmiş liste, kazanılan elmas miktarı)
     */
    fun claimOpportunityReward(
        opportunityId: String,
        current: List<BulletinOpportunity>
    ): Pair<List<BulletinOpportunity>, Int> {
        val target = current.find { it.id == opportunityId } ?: return Pair(current, 0)
        if (!target.isCompleted || target.isClaimed) return Pair(current, 0)

        val earnedDiamonds = target.diamondReward.coerceIn(1, 10)

        // Mevcut aktif şehirlerin listesini al
        val activeCityIds = current.map { it.cityId }.toSet()
        val validTemplates = OPPORTUNITY_TEMPLATES.filter { isTemplateValid(it) }

        // Henüz listede olmayan şehirlerden yeni bir fırsat seç
        val candidate = validTemplates.filter { it.cityId !in activeCityIds }
            .randomOrNull() ?: validTemplates.filter { it.id != opportunityId }.random()

        val randomizedQty = (candidate.targetQuantity * Random.nextDouble(0.8, 1.2)).toInt().coerceIn(25, 120)
        val randomizedDiamonds = Random.nextInt(2, 11) // 2 ile 10 elmas

        val newOpportunity = candidate.copy(
            id = "${candidate.id}_${System.currentTimeMillis() % 10000}",
            targetQuantity = randomizedQty,
            producedQuantity = 0,
            soldQuantity = 0,
            diamondReward = randomizedDiamonds,
            isClaimed = false
        )

        val updatedList = current.map { if (it.id == opportunityId) newOpportunity else it }
        return Pair(updatedList, earnedDiamonds)
    }

    /**
     * JSON serileştirme (DataStore ve Çıkışta Supabase kaydı için)
     */
    fun toJson(list: List<BulletinOpportunity>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("cityId", item.cityId)
                put("cityNameTr", item.cityNameTr)
                put("cityNameEn", item.cityNameEn)
                put("category", item.category.name)
                put("targetProductId", item.targetProductId)
                put("targetProductNameTr", item.targetProductNameTr)
                put("targetProductNameEn", item.targetProductNameEn)
                put("targetFacilityId", item.targetFacilityId)
                put("targetFacilityNameTr", item.targetFacilityNameTr)
                put("targetFacilityNameEn", item.targetFacilityNameEn)
                put("headlineTr", item.headlineTr)
                put("headlineEn", item.headlineEn)
                put("descriptionTr", item.descriptionTr)
                put("descriptionEn", item.descriptionEn)
                put("strategyTipTr", item.strategyTipTr)
                put("strategyTipEn", item.strategyTipEn)
                put("priceMultiplier", item.priceMultiplier.toDouble())
                put("targetQuantity", item.targetQuantity)
                put("producedQuantity", item.producedQuantity)
                put("soldQuantity", item.soldQuantity)
                put("diamondReward", item.diamondReward)
                put("isClaimed", item.isClaimed)
                put("iconEmoji", item.iconEmoji)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    /**
     * JSON ayrıştırma (Girişte DataStore / Bulut üzerinden geri yükleme)
     */
    fun fromJson(json: String?): List<BulletinOpportunity> {
        if (json.isNullOrBlank()) return generateInitialOpportunities()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<BulletinOpportunity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catName = obj.optString("category", CityEventCategory.AGRICULTURE.name)
                val cat = try { CityEventCategory.valueOf(catName) } catch (_: Exception) { CityEventCategory.AGRICULTURE }
                list.add(
                    BulletinOpportunity(
                        id = obj.getString("id"),
                        cityId = obj.getString("cityId"),
                        cityNameTr = obj.optString("cityNameTr", "Şehir"),
                        cityNameEn = obj.optString("cityNameEn", "City"),
                        category = cat,
                        targetProductId = obj.getString("targetProductId"),
                        targetProductNameTr = obj.optString("targetProductNameTr", "Ürün"),
                        targetProductNameEn = obj.optString("targetProductNameEn", "Product"),
                        targetFacilityId = obj.optString("targetFacilityId", ""),
                        targetFacilityNameTr = obj.optString("targetFacilityNameTr", "Tesis"),
                        targetFacilityNameEn = obj.optString("targetFacilityNameEn", "Facility"),
                        headlineTr = obj.optString("headlineTr", ""),
                        headlineEn = obj.optString("headlineEn", ""),
                        descriptionTr = obj.optString("descriptionTr", ""),
                        descriptionEn = obj.optString("descriptionEn", ""),
                        strategyTipTr = obj.optString("strategyTipTr", ""),
                        strategyTipEn = obj.optString("strategyTipEn", ""),
                        priceMultiplier = obj.optDouble("priceMultiplier", 1.35).toFloat(),
                        targetQuantity = obj.optInt("targetQuantity", 50),
                        producedQuantity = obj.optInt("producedQuantity", 0),
                        soldQuantity = obj.optInt("soldQuantity", 0),
                        diamondReward = obj.optInt("diamondReward", 5),
                        isClaimed = obj.optBoolean("isClaimed", false),
                        iconEmoji = obj.optString("iconEmoji", "🌾")
                    )
                )
            }
            val validList = list.filter { isTemplateValid(it) }
            if (validList.size >= 3) validList.take(3) else generateInitialOpportunities()
        } catch (_: Exception) {
            generateInitialOpportunities()
        }
    }
}
