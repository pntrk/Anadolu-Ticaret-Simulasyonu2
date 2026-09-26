package com.example.data

import androidx.compose.runtime.Composable
import com.example.ui.theme.Dictionary
import com.example.ui.theme.isEnglishLanguage

data class CityProfile(
    val id: String,
    val name: String,
    val region: String,
    val primaryProducts: List<String>,
    val logistics: List<String>,
    val description: String,
    val imageRes: Int = 0,
    val pathData: String = "",
    val relativeX: Float = 0.5f,
    val relativeY: Float = 0.5f,
    val economicMultiplier: Float = 1.0f,
    val connectedCityIds: List<String> = emptyList(),
    val laborCostMultiplier: Float = 1.0f,
    val productionSpeedMultiplier: Float = 1.0f,
    val maxFacilityTier: Int = 3,
    val optimalProductIds: List<String> = emptyList(),
    val isMajorCity: Boolean = false,
    val isGlobal: Boolean = false,
    val country: String = "Türkiye"
) {
    val countryFlag: String
        get() = getCountryFlagEmoji(country, isGlobal)

    val displayName: String
        @Composable
        get() = getDisplayName(isEnglishLanguage())

    val regionDisplayName: String
        @Composable
        get() = getRegionDisplayName(isEnglishLanguage())

    val descriptionDisplay: String
        @Composable
        get() = getDescriptionDisplay(isEnglishLanguage())

    val countryDisplayName: String
        @Composable
        get() = getCountryDisplayName(isEnglishLanguage())

    val primaryProductsDisplay: List<String>
        @Composable
        get() = getPrimaryProductsDisplay(isEnglishLanguage())

    val logisticsDisplay: List<String>
        @Composable
        get() = getLogisticsDisplay(isEnglishLanguage())

    fun getDisplayName(isEnglish: Boolean = false): String {
        return if (isEnglish) (Dictionary[name] ?: name) else name
    }

    fun getDisplayName(context: android.content.Context? = null): String {
        return Dictionary[name] ?: name
    }

    fun getRegionDisplayName(isEnglish: Boolean = false): String {
        return if (isEnglish) (Dictionary[region] ?: region) else region
    }

    fun getRegionDisplayName(context: android.content.Context? = null): String {
        return Dictionary[region] ?: region
    }

    fun getCountryDisplayName(isEnglish: Boolean = false): String {
        return if (isEnglish) (Dictionary[country] ?: country) else country
    }

    fun getCountryDisplayName(context: android.content.Context? = null): String {
        return Dictionary[country] ?: country
    }

    fun getPrimaryProductsDisplay(isEnglish: Boolean): List<String> {
        return if (isEnglish) primaryProducts.map { Dictionary[it] ?: it } else primaryProducts
    }

    fun getPrimaryProductsDisplay(context: android.content.Context? = null): List<String> {
        return primaryProducts.map { Dictionary[it] ?: it }
    }

    fun getLogisticsDisplay(isEnglish: Boolean): List<String> {
        return if (isEnglish) logistics.map { Dictionary[it] ?: it } else logistics
    }

    fun getLogisticsDisplay(context: android.content.Context? = null): List<String> {
        return logistics.map { Dictionary[it] ?: it }
    }

    fun getDescriptionDisplay(isEnglish: Boolean = false): String {
        return if (isEnglish) (Dictionary[description] ?: description) else description
    }

    fun getDescriptionDisplay(context: android.content.Context? = null): String {
        return Dictionary[description] ?: description
    }
}

fun migrateLegacyCity(cityId: String?): String {
    if (cityId.isNullOrBlank()) return "istanbul"
    val clean = cityId.trim().lowercase()
    return when (clean) {
        "canakkale", "çanakkale", "rize", "kayseri" -> "istanbul"
        else -> {
            val exists = cities.any { it.id.equals(clean, ignoreCase = true) }
            if (exists) clean else "istanbul"
        }
    }
}

val cities = listOf(
    // ==========================================
    // 🇹🇷 TÜRKİYE ŞEHİRLERİ (18 STRATEJİK ŞEHİR)
    // ==========================================
    
    // 1. Eskişehir (Kademe 1: Bor Madeni & Kademe 3: Jet Motoru/OSB)
    CityProfile(
        id = "eskisehir",
        name = "Eskişehir",
        region = "İç Anadolu",
        primaryProducts = listOf("Bor Madeni", "Jet Motoru / OSB"),
        logistics = listOf("Yüksek Hızlı Tren", "Otoyol", "Havalimanı"),
        description = "Türkiye'nin bor rezervi merkezi ve havacılık/jet motoru sanayii üssü.",
        imageRes = 0,
        pathData = "M 0.24 0.28 L 0.32 0.26 L 0.34 0.34 L 0.26 0.36 Z",
        relativeX = 0.28f,
        relativeY = 0.28f,
        economicMultiplier = 1.2f,
        connectedCityIds = listOf("ankara", "bursa", "kocaeli", "konya"),
        laborCostMultiplier = 1.1f,
        productionSpeedMultiplier = 1.1f,
        maxFacilityTier = 3,
        optimalProductIds = listOf("titanium", "turbine_engine", "machinery", "composite_structure"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 2. Sivas (Kademe 1: Demir Cevheri Madeni)
    CityProfile(
        id = "sivas",
        name = "Sivas",
        region = "İç Anadolu",
        primaryProducts = listOf("Demir Cevheri Madeni", "Ağır Madencilik"),
        logistics = listOf("Demiryolu", "Karayolu"),
        description = "Sivas demir yatakları ve Cumhuriyet'in demiryolu/ağır sanayi kavşağı.",
        imageRes = 0,
        pathData = "M 0.58 0.30 L 0.66 0.29 L 0.68 0.37 L 0.60 0.39 Z",
        relativeX = 0.62f,
        relativeY = 0.32f,
        economicMultiplier = 0.9f,
        connectedCityIds = listOf("istanbul", "elazig", "ankara", "kirikkale"),
        laborCostMultiplier = 0.85f,
        productionSpeedMultiplier = 0.95f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("iron", "coal", "limestone"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 3. Batman (Kademe 1: Ham Petrol Kuyuları)
    CityProfile(
        id = "batman",
        name = "Batman",
        region = "Güneydoğu Anadolu",
        primaryProducts = listOf("Ham Petrol Kuyuları", "Petrol Sondajı"),
        logistics = listOf("Boru Hattı", "Demiryolu", "Karayolu"),
        description = "Raman Dağı petrol sahaları ve Türkiye'nin ilk petrol rafineri merkezi.",
        imageRes = 0,
        pathData = "M 0.74 0.56 L 0.82 0.55 L 0.84 0.63 L 0.76 0.65 Z",
        relativeX = 0.78f,
        relativeY = 0.58f,
        economicMultiplier = 0.85f,
        connectedCityIds = listOf("elazig", "gaziantep", "kahramanmaras", "basra"),
        laborCostMultiplier = 0.8f,
        productionSpeedMultiplier = 0.9f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("crude_oil", "natural_gas"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 4. Zonguldak (Kademe 1: Taş Kömürü Madeni)
    CityProfile(
        id = "zonguldak",
        name = "Zonguldak",
        region = "Karadeniz",
        primaryProducts = listOf("Taş Kömürü Madeni", "Ağır Metalürji"),
        logistics = listOf("Liman", "Demiryolu"),
        description = "Karaelmas havzası. Çelik endüstrisinin can damarı taş kömürü rezervleri.",
        imageRes = 0,
        pathData = "M 0.28 0.10 L 0.36 0.09 L 0.38 0.17 L 0.30 0.19 Z",
        relativeX = 0.32f,
        relativeY = 0.12f,
        economicMultiplier = 0.95f,
        connectedCityIds = listOf("kocaeli", "ankara", "kirikkale"),
        laborCostMultiplier = 0.9f,
        productionSpeedMultiplier = 1.0f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("coal", "iron"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 5. Artvin (Kademe 1: Bakır Cevheri Madeni)
    CityProfile(
        id = "artvin",
        name = "Artvin",
        region = "Karadeniz",
        primaryProducts = listOf("Bakır Cevheri Madeni", "Maden Havzası"),
        logistics = listOf("Karayolu", "Liman Bağlantısı"),
        description = "Artvin zengin bakır yatakları ve Doğu Karadeniz maden havzası.",
        imageRes = 0,
        pathData = "M 0.78 0.10 L 0.86 0.09 L 0.88 0.17 L 0.80 0.19 Z",
        relativeX = 0.82f,
        relativeY = 0.12f,
        economicMultiplier = 0.85f,
        connectedCityIds = listOf("sivas", "elazig"),
        laborCostMultiplier = 0.85f,
        productionSpeedMultiplier = 0.95f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("copper", "limestone"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 6. Muğla (Kademe 1: Kuvars / Silisyum Ocağı)
    CityProfile(
        id = "mugla",
        name = "Muğla",
        region = "Ege",
        primaryProducts = listOf("Kuvars / Silisyum Ocağı", "Kireçtaşı"),
        logistics = listOf("Liman", "Karayolu"),
        description = "Muğla kuvars ve silisyum ocakları; cam ve yarı iletken hammaddesi kaynağı.",
        imageRes = 0,
        pathData = "M 0.10 0.62 L 0.18 0.60 L 0.20 0.68 L 0.12 0.70 Z",
        relativeX = 0.14f,
        relativeY = 0.65f,
        economicMultiplier = 1.0f,
        connectedCityIds = listOf("izmir", "konya", "mersin"),
        laborCostMultiplier = 0.95f,
        productionSpeedMultiplier = 1.0f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("silicon", "limestone"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 7. Elazığ (Kademe 1: Krom Cevheri Madeni)
    CityProfile(
        id = "elazig",
        name = "Elazığ",
        region = "Doğu Anadolu",
        primaryProducts = listOf("Krom Cevheri Madeni", "Metal Madenciliği"),
        logistics = listOf("Demiryolu", "Havalimanı", "Karayolu"),
        description = "Elazığ krom sahaları ve zengin yer altı madencilik kompleksi.",
        imageRes = 0,
        pathData = "M 0.64 0.47 L 0.72 0.46 L 0.74 0.54 L 0.66 0.56 Z",
        relativeX = 0.68f,
        relativeY = 0.50f,
        economicMultiplier = 0.85f,
        connectedCityIds = listOf("sivas", "batman", "gaziantep", "istanbul"),
        laborCostMultiplier = 0.8f,
        productionSpeedMultiplier = 0.9f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("iron", "titanium", "limestone"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 8. Kırıkkale (Kademe 1: Kükürt ve Sanayi Tuzu)
    CityProfile(
        id = "kirikkale",
        name = "Kırıkkale",
        region = "İç Anadolu",
        primaryProducts = listOf("Kükürt & Sanayi Kimyası", "Kimyasal Hammadde"),
        logistics = listOf("Demiryolu", "Otoyol"),
        description = "Kimya ve hammadde endüstrisi, sanayi tuzu ve kükürt üretim merkezi.",
        imageRes = 0,
        pathData = "M 0.38 0.27 L 0.46 0.26 L 0.48 0.34 L 0.40 0.36 Z",
        relativeX = 0.42f,
        relativeY = 0.30f,
        economicMultiplier = 0.95f,
        connectedCityIds = listOf("ankara", "zonguldak", "sivas", "istanbul"),
        laborCostMultiplier = 0.9f,
        productionSpeedMultiplier = 1.0f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("chemicals", "coal"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 9. Kahramanmaraş (Kademe 1: Endüstriyel Pamuk Tarlaları)
    CityProfile(
        id = "kahramanmaras",
        name = "Kahramanmaraş",
        region = "Akdeniz",
        primaryProducts = listOf("Endüstriyel Pamuk Tarlaları", "Tekstil Hammaddesi"),
        logistics = listOf("Demiryolu", "Karayolu"),
        description = "Verimli tarım ovaları ve Türkiye'nin en büyük pamuk üretim havzalarından biri.",
        imageRes = 0,
        pathData = "M 0.56 0.59 L 0.64 0.58 L 0.66 0.66 L 0.58 0.68 Z",
        relativeX = 0.60f,
        relativeY = 0.62f,
        economicMultiplier = 0.9f,
        connectedCityIds = listOf("gaziantep", "istanbul", "mersin", "batman"),
        laborCostMultiplier = 0.85f,
        productionSpeedMultiplier = 0.95f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("cotton", "timber"),
        isMajorCity = false,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 10. Konya (Tier 1: Boksit Madeni & Tier 2: Alüminyum Döküm)
    CityProfile(
        id = "konya",
        name = "Konya",
        region = "İç Anadolu",
        primaryProducts = listOf("Boksit Madeni", "Alüminyum Döküm"),
        logistics = listOf("Demiryolu", "Otoyol"),
        description = "Konya boksit yatakları ve entegre alüminyum izabe/döküm tesisleri.",
        imageRes = 0,
        pathData = "M 0.34 0.49 L 0.42 0.48 L 0.44 0.56 L 0.36 0.58 Z",
        relativeX = 0.38f,
        relativeY = 0.52f,
        economicMultiplier = 1.0f,
        connectedCityIds = listOf("eskisehir", "ankara", "mugla", "mersin", "istanbul"),
        laborCostMultiplier = 0.9f,
        productionSpeedMultiplier = 1.0f,
        maxFacilityTier = 2,
        optimalProductIds = listOf("aluminum", "aluminum_ingot", "solar_panel"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 11. Kocaeli (Kademe 2: Petrokimya ve Sentetik Elyaf Sanayii)
    CityProfile(
        id = "kocaeli",
        name = "Kocaeli",
        region = "Marmara",
        primaryProducts = listOf("Petrokimya", "Sentetik Elyaf Sanayii"),
        logistics = listOf("Liman", "Otoyol", "Demiryolu"),
        description = "Tüpraş rafinerisi, polimer tesisleri ve ileri sentetik dokuma kimyası.",
        imageRes = 0,
        pathData = "M 0.23 0.14 L 0.29 0.15 L 0.28 0.22 L 0.24 0.24 L 0.21 0.19 Z",
        relativeX = 0.25f,
        relativeY = 0.19f,
        economicMultiplier = 1.3f,
        connectedCityIds = listOf("istanbul", "bursa", "zonguldak", "eskisehir"),
        laborCostMultiplier = 1.25f,
        productionSpeedMultiplier = 1.2f,
        maxFacilityTier = 2,
        optimalProductIds = listOf("petrochem", "plastic", "synthetic_textile", "refined_fuel"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 12. Bursa (Kademe 2: Kablo, Döküm & Profil Sanayii)
    CityProfile(
        id = "bursa",
        name = "Bursa",
        region = "Marmara",
        primaryProducts = listOf("Kablo, Döküm & Profil Sanayii", "Otomotiv Yan Sanayi"),
        logistics = listOf("Otoyol", "Liman"),
        description = "Kablo üretimi, metal profil döküm ve otomotiv yedek parça üssü.",
        imageRes = 0,
        pathData = "M 0.14 0.27 L 0.22 0.26 L 0.24 0.35 L 0.17 0.38 L 0.12 0.32 Z",
        relativeX = 0.18f,
        relativeY = 0.32f,
        economicMultiplier = 1.25f,
        connectedCityIds = listOf("istanbul", "kocaeli", "izmir", "eskisehir"),
        laborCostMultiplier = 1.2f,
        productionSpeedMultiplier = 1.15f,
        maxFacilityTier = 2,
        optimalProductIds = listOf("wire", "auto_part", "glass", "packaging"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 13. Gaziantep (Kademe 2: Endüstriyel Kimya & Dokuma Sanayii)
    CityProfile(
        id = "gaziantep",
        name = "Gaziantep",
        region = "Güneydoğu Anadolu",
        primaryProducts = listOf("Endüstriyel Kimya & Dokuma Sanayii", "Ambalaj"),
        logistics = listOf("Otoyol", "Havalimanı", "Demiryolu"),
        description = "Dokuma, endüstriyel iplik, polimerik ambalaj ve ihracat sanayii.",
        imageRes = 0,
        pathData = "M 0.56 0.62 L 0.62 0.60 L 0.64 0.69 L 0.58 0.75 L 0.55 0.68 Z",
        relativeX = 0.65f,
        relativeY = 0.68f,
        economicMultiplier = 1.1f,
        connectedCityIds = listOf("kahramanmaras", "elazig", "batman", "mersin"),
        laborCostMultiplier = 0.9f,
        productionSpeedMultiplier = 1.05f,
        maxFacilityTier = 2,
        optimalProductIds = listOf("fabric", "clothing", "packaging", "chemicals"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 15. İzmir (Kademe 3: Çip, Yarı İletken & Sensör Sanayii)
    CityProfile(
        id = "izmir",
        name = "İzmir",
        region = "Ege",
        primaryProducts = listOf("Çip, Yarı İletken & Sensör Sanayii", "Fiber Optik"),
        logistics = listOf("Liman", "Havalimanı", "Otoyol"),
        description = "Yarı iletken çip üretim tesisleri, silisyum wafer ve optik sensör kompleksi.",
        imageRes = 0,
        pathData = "M 0.02 0.38 L 0.09 0.37 L 0.11 0.45 L 0.06 0.50 L 0.01 0.44 Z",
        relativeX = 0.08f,
        relativeY = 0.44f,
        economicMultiplier = 1.35f,
        connectedCityIds = listOf("bursa", "mugla", "eskisehir", "frankfurt"),
        laborCostMultiplier = 1.25f,
        productionSpeedMultiplier = 1.15f,
        maxFacilityTier = 3,
        optimalProductIds = listOf("semiconductor_wafer", "chip", "pcb_substrate", "optical_fiber", "smartphone"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 16. Ankara (Tier 3: Savunma Sanayii, Radar & Kompozit Gövde)
    CityProfile(
        id = "ankara",
        name = "Ankara",
        region = "İç Anadolu",
        primaryProducts = listOf("Savunma Sanayii, Radar & Kompozit Gövde", "Havacılık"),
        logistics = listOf("Havalimanı", "Demiryolu", "Otoyol"),
        description = "Milli savunma sanayiinin kalbi. Radar, karbon kompozit ve İHA gövdeleri.",
        imageRes = 0,
        pathData = "M 0.31 0.24 L 0.40 0.23 L 0.44 0.31 L 0.38 0.38 L 0.30 0.34 Z",
        relativeX = 0.38f,
        relativeY = 0.30f,
        economicMultiplier = 1.35f,
        connectedCityIds = listOf("istanbul", "eskisehir", "sivas", "zonguldak", "kirikkale", "essen"),
        laborCostMultiplier = 1.25f,
        productionSpeedMultiplier = 1.15f,
        maxFacilityTier = 3,
        optimalProductIds = listOf("carbon_fiber", "composite_structure", "telecom_station", "uav", "defense_frigate"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 17. İstanbul (Kademe 4: Elektrikli Araç, Uydu/İHA ve Borsa Merkezi)
    CityProfile(
        id = "istanbul",
        name = "İstanbul",
        region = "Marmara",
        primaryProducts = listOf("Elektrikli Araç, Uydu/İHA ve Borsa Merkezi", "Mega Teknoloji"),
        logistics = listOf("Havalimanı", "Liman", "Otoyol"),
        description = "Borsa İstanbul finans üssü, elektrikli otomobil fabrikaları ve uydu geliştirme merkezi.",
        imageRes = 0,
        pathData = "M 0.15 0.14 L 0.20 0.13 L 0.22 0.18 L 0.17 0.21 L 0.14 0.17 Z",
        relativeX = 0.18f,
        relativeY = 0.17f,
        economicMultiplier = 1.5f,
        connectedCityIds = listOf("kocaeli", "bursa", "ankara", "london", "rotterdam"),
        laborCostMultiplier = 1.5f,
        productionSpeedMultiplier = 1.25f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("ev", "satellite", "ai_datacenter", "quantum_supercomputer", "smart_skyscraper"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // 18. Mersin (Kademe 1-4: Liman, Petrokimya, Konteyner & Mega Tersane Kompleksi)
    CityProfile(
        id = "mersin",
        name = "Mersin",
        region = "Akdeniz",
        primaryProducts = listOf("Mega Akdeniz Tersanesi & Lojistik Kapısı", "Petrokimya & Rafineri", "Endüstriyel Konteyner"),
        logistics = listOf("Mega Liman", "Demiryolu", "Otoyol"),
        description = "Akdeniz'in en büyük konteyner limanı, petrokimya ve LNG terminalleri, kireçtaşı ocakları ve kargo gemisi & süperyat tersaneleri.",
        imageRes = 0,
        pathData = "M 0.42 0.68 L 0.50 0.67 L 0.52 0.75 L 0.44 0.77 Z",
        relativeX = 0.46f,
        relativeY = 0.72f,
        economicMultiplier = 1.4f,
        connectedCityIds = listOf("konya", "kahramanmaras", "gaziantep", "mugla", "basra", "rotterdam"),
        laborCostMultiplier = 1.2f,
        productionSpeedMultiplier = 1.2f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("cargo_ship", "super_yacht", "defense_frigate", "industrial_container", "hydrogen_plant", "petrochem", "refined_fuel", "liquefied_gas", "packaging", "plastic", "cement", "cotton", "limestone", "chemicals", "solar_panel"),
        isMajorCity = true,
        isGlobal = false,
        country = "Türkiye"
    ),

    // ==============================================================
    // 🌐 DÜNYA ŞEHİRLERİ (12 KÜRESEL MERKEZ - [$] USD BÖLGESİ)
    // ==============================================================

    // 19. Santiago (Kademe 1: Lityum Ekstraksiyon Tesisi)
    CityProfile(
        id = "santiago",
        name = "Santiago",
        region = "Güney Amerika",
        primaryProducts = listOf("Lityum Ekstraksiyon Tesisi", "Maden"),
        logistics = listOf("Havalimanı", "Pasifik Liman Bağlantısı"),
        description = "And Dağları eteklerindeki dünyanın en büyük lityum havzası ve ekstraksiyon tesisi.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.25f,
        relativeY = 0.85f,
        economicMultiplier = 1.3f,
        connectedCityIds = listOf("sao_paulo", "houston"),
        laborCostMultiplier = 1.1f,
        productionSpeedMultiplier = 1.0f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("lithium", "battery"),
        isMajorCity = true,
        isGlobal = true,
        country = "Şili"
    ),

    // 20. Kuala Lumpur (Tier 1: Doğal Kauçuk Plantasyonu)
    CityProfile(
        id = "kuala_lumpur",
        name = "Kuala Lumpur",
        region = "Güneydoğu Asya",
        primaryProducts = listOf("Doğal Kauçuk Plantasyonu", "Lateks İşleme"),
        logistics = listOf("Havalimanı", "Malakka Boğazı Limanı"),
        description = "Güneydoğu Asya'nın devasa doğal kauçuk plantasyonları ve polimer merkezleri.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.78f,
        relativeY = 0.62f,
        economicMultiplier = 1.2f,
        connectedCityIds = listOf("shanghai", "tokyo", "basra"),
        laborCostMultiplier = 1.0f,
        productionSpeedMultiplier = 1.05f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("rubber_latex", "tire"),
        isMajorCity = true,
        isGlobal = true,
        country = "Malezya"
    ),

    // 21. Johannesburg (Kademe 1: Platin & Değerli Metal Madeni)
    CityProfile(
        id = "johannesburg",
        name = "Johannesburg",
        region = "Afrika",
        primaryProducts = listOf("Platin & Değerli Metal Madeni", "Grafit & Nadir Element"),
        logistics = listOf("Demiryolu", "Havalimanı"),
        description = "Afrika kıtasının en zengin platin, grafit ve nadir toprak elementleri madenleri.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.54f,
        relativeY = 0.82f,
        economicMultiplier = 1.25f,
        connectedCityIds = listOf("basra", "london", "sao_paulo"),
        laborCostMultiplier = 0.95f,
        productionSpeedMultiplier = 1.0f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("graphite_ore", "graphene_sheet"),
        isMajorCity = true,
        isGlobal = true,
        country = "Güney Afrika"
    ),

    // 22. Basra (Kademe 1: Ağır Ham Petrol Sondaj Kulesi)
    CityProfile(
        id = "basra",
        name = "Basra",
        region = "Orta Doğu",
        primaryProducts = listOf("Ağır Ham Petrol Sondaj Kulesi", "Doğal Gaz"),
        logistics = listOf("Körfez Limanı", "Boru Hatları"),
        description = "Basra Körfezi mega petrol sahaları ve ağır ham petrol sondaj kuleleri.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.60f,
        relativeY = 0.48f,
        economicMultiplier = 1.35f,
        connectedCityIds = listOf("mersin", "batman", "johannesburg", "kuala_lumpur"),
        laborCostMultiplier = 1.0f,
        productionSpeedMultiplier = 1.1f,
        maxFacilityTier = 1,
        optimalProductIds = listOf("crude_oil", "natural_gas"),
        isMajorCity = true,
        isGlobal = true,
        country = "Irak"
    ),

    // 23. Sao Paulo (Kademe 1: Biyo-Etanol ve Kademe 2: Biyo-Plastik)
    CityProfile(
        id = "sao_paulo",
        name = "Sao Paulo",
        region = "Güney Amerika",
        primaryProducts = listOf("Biyo-Etanol", "Biyo-Plastik & Polimer"),
        logistics = listOf("Santos Limanı", "Havalimanı"),
        description = "Latin Amerika'nın biyo-yakıt ve sürdürülebilir biyo-plastik polimer sanayi devi.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.35f,
        relativeY = 0.78f,
        economicMultiplier = 1.3f,
        connectedCityIds = listOf("santiago", "houston", "johannesburg"),
        laborCostMultiplier = 1.05f,
        productionSpeedMultiplier = 1.1f,
        maxFacilityTier = 2,
        optimalProductIds = listOf("plastic", "refined_fuel", "synthetic_textile"),
        isMajorCity = true,
        isGlobal = true,
        country = "Brezilya"
    ),

    // 24. Essen (Kademe 2: Ağır Sanayi Alman Çeliği Fabrikası)
    CityProfile(
        id = "essen",
        name = "Essen",
        region = "Avrupa",
        primaryProducts = listOf("Ağır Sanayi Alman Çeliği Fabrikası", "Metalürji"),
        logistics = listOf("Ren Nehri Limanı", "Demiryolu Ağı"),
        description = "Essen bölgesi ağır endüstrisi, yüksek dayanımlı Alman çeliği ve alaşım dökümhaneleri.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.48f,
        relativeY = 0.28f,
        economicMultiplier = 1.5f,
        connectedCityIds = listOf("frankfurt", "rotterdam", "ankara", "london"),
        laborCostMultiplier = 1.4f,
        productionSpeedMultiplier = 1.3f,
        maxFacilityTier = 2,
        optimalProductIds = listOf("steel", "titanium_alloy", "cement"),
        isMajorCity = true,
        isGlobal = true,
        country = "Almanya"
    ),

    // 25. Frankfurt (Kademe 3: Otomotiv Yürür Aksam & Şasi Tesisi)
    CityProfile(
        id = "frankfurt",
        name = "Frankfurt",
        region = "Avrupa",
        primaryProducts = listOf("Otomotiv Yürür Aksam & Şasi Tesisi", "Mekanik"),
        logistics = listOf("Havalimanı Hub", "Otoyol Kavşağı"),
        description = "Avrupa otomotiv yan sanayii omurgası, yürür aksam, şasi ve robotik montaj hatları.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.49f,
        relativeY = 0.30f,
        economicMultiplier = 1.55f,
        connectedCityIds = listOf("essen", "rotterdam", "izmir", "london"),
        laborCostMultiplier = 1.45f,
        productionSpeedMultiplier = 1.3f,
        maxFacilityTier = 3,
        optimalProductIds = listOf("auto_part", "machinery", "robotics_arm"),
        isMajorCity = true,
        isGlobal = true,
        country = "Almanya"
    ),

    // 26. Şanghay (Tier 3: Giga Batarya Fabrikası)
    CityProfile(
        id = "shanghai",
        name = "Şanghay",
        region = "Doğu Asya",
        primaryProducts = listOf("Giga Batarya Fabrikası", "Enerji Depolama"),
        logistics = listOf("Dünyanın En Büyük Limanı", "Havalimanı"),
        description = "Küresel enerji devrimi merkezi. Lityum-iyon ve nano grafen giga batarya fabrikaları.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.82f,
        relativeY = 0.42f,
        economicMultiplier = 1.6f,
        connectedCityIds = listOf("tokyo", "kuala_lumpur", "houston"),
        laborCostMultiplier = 1.2f,
        productionSpeedMultiplier = 1.4f,
        maxFacilityTier = 3,
        optimalProductIds = listOf("battery", "nano_battery", "solar_panel"),
        isMajorCity = true,
        isGlobal = true,
        country = "Çin"
    ),

    // 27. Tokyo (Tier 3: Hassas Robotik Kol & Tier 4: Otomasyon Sanayii)
    CityProfile(
        id = "tokyo",
        name = "Tokyo",
        region = "Doğu Asya",
        primaryProducts = listOf("Hassas Robotik Kol", "Otomasyon Sanayii"),
        logistics = listOf("Tokyo Körfezi Limanı", "Havalimanı"),
        description = "Hassas robotik kollar, endüstriyel otomasyon ve otonom drone sürü sistemleri.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.90f,
        relativeY = 0.38f,
        economicMultiplier = 1.7f,
        connectedCityIds = listOf("shanghai", "kuala_lumpur", "houston"),
        laborCostMultiplier = 1.55f,
        productionSpeedMultiplier = 1.4f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("robotics_arm", "autonomous_drone_swarm", "ai_datacenter"),
        isMajorCity = true,
        isGlobal = true,
        country = "Japonya"
    ),

    // 28. Houston (Tier 1: Kaya Gazı & Tier 4: Uzay Roketi Sanayii)
    CityProfile(
        id = "houston",
        name = "Houston",
        region = "Kuzey Amerika",
        primaryProducts = listOf("Kaya Gazı", "Uzay Roketi Sanayii"),
        logistics = listOf("Houston Uzay Üssü", "Meksika Körfezi Limanı"),
        description = "NASA Uzay Merkezi ve derin uzay roket teknolojileri, kaya gazı işleme kompleksleri.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.20f,
        relativeY = 0.42f,
        economicMultiplier = 1.75f,
        connectedCityIds = listOf("new_york", "london", "santiago", "sao_paulo", "tokyo"),
        laborCostMultiplier = 1.6f,
        productionSpeedMultiplier = 1.35f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("natural_gas", "space_rocket", "fusion_reactor_core"),
        isMajorCity = true,
        isGlobal = true,
        country = "ABD"
    ),

    // 29. Rotterdam (Kademe 4: Avrupa Lojistik Hub'ı, Petrokimya Rafinerisi & Yeşil Enerji)
    CityProfile(
        id = "rotterdam",
        name = "Rotterdam",
        region = "Avrupa",
        primaryProducts = listOf("Avrupa Lojistik & Konteyner Hub'ı", "Kuzey Denizi Rüzgar Türbini Kompleksi", "Mega Petrol Rafinerisi & Petrokimya", "Hidrojen Elektroliz Tesisleri", "Yeşil Şebeke & Mega Tersane"),
        logistics = listOf("Avrupa'nın En Büyük Limanı (Port of Rotterdam)", "Maasvlakte Deepwater Terminali", "Ren-Maas Kanal Ağı"),
        description = "Avrupa'nın en büyük konteyner ve petrokimya limanı. Kuzey Denizi açık deniz (offshore) rüzgar türbinleri, yeşil hidrojen rafinasyon santralleri ve dev tersaneleriyle kıtanın lojistik ve enerji kalbi.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.47f,
        relativeY = 0.26f,
        economicMultiplier = 1.6f,
        connectedCityIds = listOf("london", "essen", "frankfurt", "istanbul", "mersin", "new_york"),
        laborCostMultiplier = 1.5f,
        productionSpeedMultiplier = 1.3f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("refined_fuel", "petrochem", "industrial_container", "turbine_engine", "hydrogen_plant", "cargo_ship", "smart_grid", "composite_structure", "liquefied_gas", "defense_frigate", "super_yacht"),
        isMajorCity = true,
        isGlobal = true,
        country = "Hollanda"
    ),

    // 30. Londra (Kademe 4: Finans/Borsa, İlaç/Biyoteknoloji, Ağır Sanayi & Havacılık-Mühendislik)
    CityProfile(
        id = "london",
        name = "Londra",
        region = "Avrupa",
        primaryProducts = listOf("Dünya Borsa & Finans Merkezi", "Ağır İş Makinesi Fabrikası", "Biyomedikal & İlaç Laboratuvarı", "Yüksek Hızlı Tren & Aeroseyir Üssü", "Kuantum Bilişim & AI Veri Merkezi"),
        logistics = listOf("Heathrow Air Cargo Hub", "Thames Lojistik Koridoru", "London Gateway Limanı"),
        description = "Küresel finans ve borsa başkenti; ilaç, biyoteknoloji, kuantum veri merkezleri, ağır sanayi makineleri ve yüksek hızlı havacılık-demiryolu mühendisliği merkezi.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.45f,
        relativeY = 0.25f,
        economicMultiplier = 1.75f,
        connectedCityIds = listOf("new_york", "rotterdam", "essen", "frankfurt", "istanbul", "houston", "johannesburg"),
        laborCostMultiplier = 1.6f,
        productionSpeedMultiplier = 1.35f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("machinery", "pharma", "biotech_med", "bullet_train", "hyperloop_capsule", "luxury_aircraft_interior", "ai_datacenter", "quantum_supercomputer", "smart_grid"),
        isMajorCity = true,
        isGlobal = true,
        country = "İngiltere"
    ),

    // 31. New York (Kademe 1-4: Wall Street Dünya Borsası, Kuantum Bilişim, Yapay Zeka & Biyofarmasötik)
    CityProfile(
        id = "new_york",
        name = "New York",
        region = "Kuzey Amerika",
        primaryProducts = listOf("Wall Street Finans & Dünya Borsası", "Kuantum Bilişim & AI Veri Merkezi", "Biyofarmasötik Laboratuvarı"),
        logistics = listOf("JFK Havalimanı Hub", "New York & New Jersey Limanı", "Atlantik Deniz Koridoru"),
        description = "Dünya finans ve ticaretinin küresel merkezi. Wall Street borsası, Manhattan kuantum süper bilgisayarları, AI veri merkezleri ve ileri biyofarmasötik sanayii.",
        imageRes = 0,
        pathData = "",
        relativeX = 0.26f,
        relativeY = 0.36f,
        economicMultiplier = 1.85f,
        connectedCityIds = listOf("houston", "london", "rotterdam", "tokyo", "frankfurt", "istanbul"),
        laborCostMultiplier = 1.65f,
        productionSpeedMultiplier = 1.4f,
        maxFacilityTier = 4,
        optimalProductIds = listOf("quantum_supercomputer", "ai_datacenter", "smart_skyscraper", "pharma", "optical_fiber", "chemicals"),
        isMajorCity = true,
        isGlobal = true,
        country = "ABD"
    )
)

fun CityProfile.getAllowedProducts(): List<Product> {
    return Product.values().filter { it.canBeBuiltIn(this.id) }
}

fun CityProfile.canBuildProduct(product: Product): Boolean {
    return product.canBeBuiltIn(this.id)
}

fun Product.getEligibleCities(citiesList: List<CityProfile> = cities): List<CityProfile> {
    val allowed = getAllowedCityIds()
    return if (allowed.isEmpty()) citiesList else citiesList.filter { allowed.contains(it.id) }
}

fun Product.getEligibleCountryFlags(citiesList: List<CityProfile> = cities): List<String> {
    return getEligibleCities(citiesList).map { it.countryFlag }.distinct()
}

fun Product.getEligibleCountryDisplayNames(isEnglish: Boolean = false, citiesList: List<CityProfile> = cities): List<String> {
    return getEligibleCities(citiesList).map { it.getCountryDisplayName(isEnglish) }.distinct()
}

fun getCountryFlagEmoji(country: String, isGlobal: Boolean = false): String {
    return when (country.trim()) {
        "Türkiye", "Türkiye" -> "🇹🇷"
        "Almanya", "Germany" -> "🇩🇪"
        "ABD", "USA", "United States" -> "🇺🇸"
        "Çin", "China" -> "🇨🇳"
        "Japonya", "Japan" -> "🇯🇵"
        "İngiltere", "UK", "United Kingdom" -> "🇬🇧"
        "Hollanda", "Netherlands" -> "🇳🇱"
        "Brezilya", "Brazil" -> "🇧🇷"
        "Güney Afrika", "South Africa" -> "🇿🇦"
        "Şili", "Chile" -> "🇨🇱"
        "Malezya", "Malaysia" -> "🇲🇾"
        "Irak", "Iraq" -> "🇮🇶"
        else -> if (isGlobal) "🌐" else if (country.isNotBlank()) "🌐" else "🇹🇷"
    }
}

