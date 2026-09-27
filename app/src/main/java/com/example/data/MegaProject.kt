package com.example.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.util.GameTime
import java.util.UUID

enum class ConsortiumQualityTier(
    val titleTr: String,
    val titleEn: String,
    val multiplier: Float,
    val borsaMultiplier: Float,
    val durationMultiplier: Float,
    val badgeColor: Long,
    val descriptionTr: String,
    val descriptionEn: String = "",
    val gradeCode: String = "C",
    val allowedStars: List<Int> = listOf(1, 2),
    val allowedQualityRangeTextTr: String = "1★ ve 2★ Girdi Kabul",
    val allowedQualityRangeTextEn: String = "1★ and 2★ Inputs Accepted"
) {
    GRADE_C(
        titleTr = "C Kalite (Standart)",
        titleEn = "C Grade (Standard)",
        multiplier = 1.0f,
        borsaMultiplier = 1.0f,
        durationMultiplier = 1.0f,
        badgeColor = 0xFF94A3B8,
        descriptionTr = "Temel sanayi standardı. Yalnızca 1★ (Standart) ve 2★ (Seçme) kalitedeki hammaddeler kabul edilir.",
        descriptionEn = "Standard industrial tier. Only 1★ (Standard) and 2★ (Select) raw materials are accepted.",
        gradeCode = "C",
        allowedStars = listOf(1, 2),
        allowedQualityRangeTextTr = "1★ ve 2★ Girdi Kabul",
        allowedQualityRangeTextEn = "1★ and 2★ Inputs Accepted"
    ),
    GRADE_B(
        titleTr = "B Kalite (Gelişmiş & Usta İşi)",
        titleEn = "B Grade (Advanced & Masterwork)",
        multiplier = 2.0f,
        borsaMultiplier = 2.3f,
        durationMultiplier = 1.3f,
        badgeColor = 0xFF38BDF8,
        descriptionTr = "Gelişmiş mühendislik standardı. Yalnızca 2★ (Seçme), 3★ (Usta İşi) ve 4★ (Seçkin) kalitedeki hammaddeler kabul edilir.",
        descriptionEn = "Advanced engineering tier. Only 2★ (Select), 3★ (Masterwork), and 4★ (Superior) inputs accepted.",
        gradeCode = "B",
        allowedStars = listOf(2, 3, 4),
        allowedQualityRangeTextTr = "2★, 3★ ve 4★ Girdi Kabul",
        allowedQualityRangeTextEn = "2★, 3★ and 4★ Inputs Accepted"
    ),
    GRADE_A(
        titleTr = "A Kalite (Premium & Kusursuz)",
        titleEn = "A Grade (Premium & Flawless)",
        multiplier = 4.0f,
        borsaMultiplier = 5.0f,
        durationMultiplier = 1.8f,
        badgeColor = 0xFFFFD700,
        descriptionTr = "En üst düzey savunma & uzay standardı. Yalnızca 4★ (Seçkin) ve 5★ (Kusursuz) kalitedeki hammaddeler kabul edilir! Maksimum prestij ve 5 kat Borsa değeri!",
        descriptionEn = "Highest aerospace & defense standard. Only 4★ (Superior) and 5★ (Flawless) materials accepted! 5x Market Valuation!",
        gradeCode = "A",
        allowedStars = listOf(4, 5),
        allowedQualityRangeTextTr = "4★ ve 5★ Girdi Kabul",
        allowedQualityRangeTextEn = "4★ and 5★ Inputs Accepted"
    );

    val requirementMultiplier: Float get() = multiplier
    val borsaValueMultiplier: Float get() = borsaMultiplier

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
    fun getDescription(isEnglish: Boolean = false): String = if (isEnglish) descriptionEn else descriptionTr
    fun getAllowedQualityRangeText(isEnglish: Boolean = false): String = if (isEnglish) allowedQualityRangeTextEn else allowedQualityRangeTextTr

    fun isStarsAllowed(stars: Int): Boolean = stars.coerceIn(1, 5) in allowedStars

    fun isQualityAllowed(quality: ItemQuality): Boolean = isStarsAllowed(quality.stars)

    fun isProductQualityAllowed(quality: ProductQuality): Boolean = isStarsAllowed(quality.tier)

    companion object {
        fun fromString(str: String?): ConsortiumQualityTier {
            if (str.isNullOrBlank()) return GRADE_C
            val clean = str.trim().uppercase()
            return when {
                clean == "GRADE_A" || clean == "A" || clean.contains("A KALITE") || clean.contains("GRADE A") -> GRADE_A
                clean == "GRADE_B" || clean == "B" || clean.contains("B KALITE") || clean.contains("GRADE B") -> GRADE_B
                clean == "GRADE_C" || clean == "C" || clean.contains("C KALITE") || clean.contains("GRADE C") -> GRADE_C
                else -> try { valueOf(clean) } catch (_: Exception) { GRADE_C }
            }
        }
    }
}

enum class MegaProjectStage(
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String
) {
    STAGE_1_BODY(
        "Aşama I: Gövde & Altyapı",
        "Stage I: Body & Infrastructure",
        "Temel metaller, çelik, kompozit ve ağır altyapı elemanlarının şantiyeye teslimatı.",
        "Delivery of structural metals, steel, composites, and heavy infrastructure elements to the site."
    ),
    STAGE_2_HARDWARE(
        "Aşama II: Donanım & Sistem",
        "Stage II: Hardware & Systems",
        "Ağır türbinler, mikroçipler, otomasyon, motor ve ileri teknoloji modül entegrasyonu.",
        "Integration of heavy turbines, microchips, automation, motors, and high-tech modules."
    ),
    STAGE_3_TESTING(
        "Aşama III: Test & Lansman",
        "Stage III: Testing & Launch",
        "Yakıt dolumu, yazılım optimizasyonu, dayanıklılık testleri ve lansman.",
        "Fueling, software calibration, stress testing, and launch preparations."
    ),
    STAGE_4_MASS_PRODUCTION(
        "Aşama IV: Seri Üretim Devamı",
        "Stage IV: Mass Production Ongoing",
        "Tam ölçekli seri üretim hattı. Ortaklıkla sürekli ürün üretimi ve rutin kâr payı aktarımı.",
        "Full-scale production line. Continuous joint manufacturing and recurring dividend distribution."
    ),
    COMPLETED(
        "Tamamlandı & Tam Kapasite",
        "Completed & Full Capacity",
        "Tüm aşamalar ve seri üretim döngüsü başarıyla kuruldu. Rutin kâr payı dağıtımı aktif.",
        "All stages and production loops operational. Recurring dividend distribution active."
    );

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
    fun getDescription(isEnglish: Boolean = false): String = if (isEnglish) descriptionEn else descriptionTr
}

data class ConsortiumSupplierSlot(
    val slotId: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    @StringRes val productNameRes: Int = 0,
    val quantityRequired: Int,
    val quantityDelivered: Int = 0,
    val stage: MegaProjectStage,
    val assignedPartnerId: String? = null,
    val assignedPartnerName: String? = null,
    val costContributionValue: Long, // Parasal değer
    val actualCostIncurred: Long = 0L, // Gerçekleşen maliyet
    val totalDividendsEarned: Long = 0L, // Toplam kazanılan kar payı
    val sharePercentage: Float = 0.0f, // Hesaplanmış % Hisse
    val lastDeliveryTimeMs: Long = System.currentTimeMillis(),
    val isBottleneckWarning: Boolean = false,
    val deliveredQualityTier: Int = 1, // 1 to 5 stars
    val deliveredQualityScore: Double = 1.0
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ConsortiumSupplierSlot {
            val stageStr = map["stage"] as? String ?: MegaProjectStage.STAGE_1_BODY.name
            val stageVal = try { MegaProjectStage.valueOf(stageStr) } catch (_: Exception) { MegaProjectStage.STAGE_1_BODY }
            return ConsortiumSupplierSlot(
                slotId = map["slotId"] as? String ?: UUID.randomUUID().toString(),
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                quantityRequired = (map["quantityRequired"] as? Number)?.toInt() ?: 0,
                quantityDelivered = (map["quantityDelivered"] as? Number)?.toInt() ?: 0,
                stage = stageVal,
                assignedPartnerId = map["assignedPartnerId"] as? String,
                assignedPartnerName = map["assignedPartnerName"] as? String,
                costContributionValue = (map["costContributionValue"] as? Number)?.toLong() ?: 0L,
                actualCostIncurred = (map["actualCostIncurred"] as? Number)?.toLong() ?: 0L,
                totalDividendsEarned = (map["totalDividendsEarned"] as? Number)?.toLong() ?: 0L,
                sharePercentage = (map["sharePercentage"] as? Number)?.toFloat() ?: 0f,
                lastDeliveryTimeMs = (map["lastDeliveryTimeMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isBottleneckWarning = map["isBottleneckWarning"] as? Boolean ?: false,
                deliveredQualityTier = (map["deliveredQualityTier"] as? Number)?.toInt() ?: 1,
                deliveredQualityScore = (map["deliveredQualityScore"] as? Number)?.toDouble() ?: 1.0
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "slotId" to slotId,
            "productId" to productId,
            "productName" to productName,
            "quantityRequired" to quantityRequired,
            "quantityDelivered" to quantityDelivered,
            "stage" to stage.name,
            "assignedPartnerId" to assignedPartnerId,
            "assignedPartnerName" to assignedPartnerName,
            "costContributionValue" to costContributionValue,
            "actualCostIncurred" to actualCostIncurred,
            "totalDividendsEarned" to totalDividendsEarned,
            "sharePercentage" to sharePercentage.toDouble(),
            "lastDeliveryTimeMs" to lastDeliveryTimeMs,
            "isBottleneckWarning" to isBottleneckWarning,
            "deliveredQualityTier" to deliveredQualityTier,
            "deliveredQualityScore" to deliveredQualityScore
        )
    }

    val isFullyDelivered: Boolean
        get() = quantityDelivered >= quantityRequired

    val remainingQuantity: Int
        get() = (quantityRequired - quantityDelivered).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (quantityRequired > 0) (quantityDelivered.toFloat() / quantityRequired).coerceIn(0f, 1f) else 1f
}

enum class ConsortiumRole(
    val titleTr: String,
    val titleEn: String,
    val badgeTr: String,
    val badgeEn: String,
    val bonusDescriptionTr: String,
    val bonusDescriptionEn: String
) {
    PRESIDENT(
        "Konsorsiyum Başkanı",
        "Consortium President",
        "👑 Başkan",
        "👑 President",
        "Satış kanalı seçimi, üye uyarıları ve projeyi duraklatma/yönetme yetkisi.",
        "Authority to select sales channel, warn/nudge members, and pause/manage project."
    ),
    CHIEF_ENGINEER(
        "Baş Mühendis (Ar-Ge)",
        "Chief Engineer (R&D)",
        "⚙️ Baş Mühendis",
        "⚙️ Chief Engineer",
        "Mega proje üretim döngüsünü %15 hızlandıran pasif Ar-Ge optimizasyonu sağlar.",
        "Grants +15% production speed bonus to all mega project production loops."
    ),
    LOGISTICS_CHIEF(
        "Lojistik Sorumlusu",
        "Logistics Director",
        "🚚 Lojistik Sorumlusu",
        "🚚 Logistics Director",
        "Sevkiyat ve hammadde tedarik yol sürelerini %20 kısaltan filo lojistik bonusu sağlar.",
        "Reduces material delivery transport and transit duration by 20%."
    ),
    SUPPLIER(
        "Tedarikçi Ortak",
        "Supplier Partner",
        "🏭 Tedarikçi",
        "🏭 Supplier",
        "Hammadde tedariki yaparak kâr payı ve hisse ortaklığı kazanır.",
        "Supplies industrial resources to earn equity shares and production dividends."
    );

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
    fun getBadge(isEnglish: Boolean = false): String = if (isEnglish) badgeEn else badgeTr
    fun getBonusDescription(isEnglish: Boolean = false): String = if (isEnglish) bonusDescriptionEn else bonusDescriptionTr
}

data class ConsortiumExportTender(
    val id: String = UUID.randomUUID().toString(),
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val destinationCountry: String,
    val countryFlag: String,
    val targetProductId: String,
    val requiredQuantity: Int,
    val deliveredQuantity: Int = 0,
    val cashReward: Long,
    val reputationReward: Int,
    val badgeRewardTr: String,
    val badgeRewardEn: String,
    val expiresAtMs: Long,
    val isCompleted: Boolean = false,
    val completedAtMs: Long? = null
) {
    val remainingQuantity: Int get() = (requiredQuantity - deliveredQuantity).coerceAtLeast(0)
    val progressFraction: Float get() = if (requiredQuantity > 0) (deliveredQuantity.toFloat() / requiredQuantity).coerceIn(0f, 1f) else 1f
    val isExpired: Boolean get() = !isCompleted && System.currentTimeMillis() > expiresAtMs
    val remainingTimeMs: Long get() = (expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)

    companion object {
        fun fromMap(map: Map<String, Any?>): ConsortiumExportTender {
            return ConsortiumExportTender(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                titleTr = map["titleTr"] as? String ?: "",
                titleEn = map["titleEn"] as? String ?: "",
                descriptionTr = map["descriptionTr"] as? String ?: "",
                descriptionEn = map["descriptionEn"] as? String ?: "",
                destinationCountry = map["destinationCountry"] as? String ?: "Küresel Pazar",
                countryFlag = map["countryFlag"] as? String ?: "🌐",
                targetProductId = map["targetProductId"] as? String ?: "",
                requiredQuantity = (map["requiredQuantity"] as? Number)?.toInt() ?: 10,
                deliveredQuantity = (map["deliveredQuantity"] as? Number)?.toInt() ?: 0,
                cashReward = (map["cashReward"] as? Number)?.toLong() ?: 50_000_000L,
                reputationReward = (map["reputationReward"] as? Number)?.toInt() ?: 100,
                badgeRewardTr = map["badgeRewardTr"] as? String ?: "İhracat Şampiyonu",
                badgeRewardEn = map["badgeRewardEn"] as? String ?: "Export Champion",
                expiresAtMs = (map["expiresAtMs"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 86400000L),
                isCompleted = map["isCompleted"] as? Boolean ?: false,
                completedAtMs = (map["completedAtMs"] as? Number)?.toLong()
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "titleTr" to titleTr,
            "titleEn" to titleEn,
            "descriptionTr" to descriptionTr,
            "descriptionEn" to descriptionEn,
            "destinationCountry" to destinationCountry,
            "countryFlag" to countryFlag,
            "targetProductId" to targetProductId,
            "requiredQuantity" to requiredQuantity,
            "deliveredQuantity" to deliveredQuantity,
            "cashReward" to cashReward,
            "reputationReward" to reputationReward,
            "badgeRewardTr" to badgeRewardTr,
            "badgeRewardEn" to badgeRewardEn,
            "expiresAtMs" to expiresAtMs,
            "isCompleted" to isCompleted,
            "completedAtMs" to completedAtMs
        )
    }
}

object ConsortiumExportTenderFactory {
    fun generateTendersForProject(project: MegaProject): List<ConsortiumExportTender> {
        val now = System.currentTimeMillis()
        val duration48h = 48 * 3600 * 1000L
        val duration72h = 72 * 3600 * 1000L

        return when (project.targetProductId) {
            "defense_frigate" -> listOf(
                ConsortiumExportTender(
                    id = "tender_frigate_gulf_${project.id.take(6)}",
                    titleTr = "Körfez Donanma Savunma İhalesi",
                    titleEn = "Gulf Naval Defense Procurement",
                    descriptionTr = "Birleşik Arap Emirlikleri ve Umman Donanması için fırkateyn ihracat anlaşması.",
                    descriptionEn = "Export contract for advanced naval frigates to the UAE and Oman Navy.",
                    destinationCountry = "BAE & Umman",
                    countryFlag = "🇦🇪",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 5,
                    cashReward = 1_500_000_000L,
                    reputationReward = 350,
                    badgeRewardTr = "Körfez Güvenlik Muhafızı",
                    badgeRewardEn = "Gulf Security Guardian",
                    expiresAtMs = now + duration72h
                ),
                ConsortiumExportTender(
                    id = "tender_frigate_baltic_${project.id.take(6)}",
                    titleTr = "Baltık Devriye Filosu İhalesi",
                    titleEn = "Baltic Patrol Fleet Procurement",
                    descriptionTr = "Kuzey Avrupa müttefikleri için radar uyumlu fırkateyn tedariki.",
                    descriptionEn = "Radar-stealth frigate delivery for North European maritime security.",
                    destinationCountry = "Polonya & Norveç",
                    countryFlag = "🇵🇱",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 8,
                    cashReward = 2_400_000_000L,
                    reputationReward = 500,
                    badgeRewardTr = "Mavi Vatan İhracat Yıldızı",
                    badgeRewardEn = "Naval Export Star",
                    expiresAtMs = now + duration72h
                )
            )
            "bullet_train" -> listOf(
                ConsortiumExportTender(
                    id = "tender_train_gulf_${project.id.take(6)}",
                    titleTr = "Körfez Yüksek Hızlı Tren Hat İhalesi",
                    titleEn = "Gulf High-Speed Railway Network",
                    descriptionTr = "Riyad - Dubai hattı için 15 adet çöl iklimine dayanıklı yüksek hızlı tren seti.",
                    descriptionEn = "15 heat-resistant high-speed train trainsets for the Riyadh - Dubai corridor.",
                    destinationCountry = "Suudi Arabistan",
                    countryFlag = "🇸🇦",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 15,
                    cashReward = 1_800_000_000L,
                    reputationReward = 400,
                    badgeRewardTr = "Çöl Şimşeği İhracatçısı",
                    badgeRewardEn = "Desert Lightning Exporter",
                    expiresAtMs = now + duration48h
                ),
                ConsortiumExportTender(
                    id = "tender_train_eu_${project.id.take(6)}",
                    titleTr = "Avrupa Yeşil Ulaşım Koridoru İhalesi",
                    titleEn = "European Green Transit Corridor",
                    descriptionTr = "Berlin - Viyana - Budapeşte hattı için 20 adet çevre dostu cer sistemli hızlı tren.",
                    descriptionEn = "20 eco-friendly traction high-speed trains for the Central European corridor.",
                    destinationCountry = "Almanya & Avusturya",
                    countryFlag = "🇩🇪",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 20,
                    cashReward = 2_600_000_000L,
                    reputationReward = 600,
                    badgeRewardTr = "Kıta Rayları Lideri",
                    badgeRewardEn = "Continental Rail Pioneer",
                    expiresAtMs = now + duration72h
                )
            )
            "space_rocket" -> listOf(
                ConsortiumExportTender(
                    id = "tender_rocket_esa_${project.id.take(6)}",
                    titleTr = "Küresel Alçak Yörünge İletişim İhalesi",
                    titleEn = "Global LEO Constellation Launch",
                    descriptionTr = "Avrupa Uzay Ajansı ve telekom ortaklığı için 6 adet ağır yük taşıyıcı roket.",
                    descriptionEn = "6 heavy-lift launch vehicles for European broadband satellite constellation.",
                    destinationCountry = "Fransa & Birleşik Krallık",
                    countryFlag = "🇫🇷",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 6,
                    cashReward = 3_600_000_000L,
                    reputationReward = 750,
                    badgeRewardTr = "Gökyüzü Fatihi",
                    badgeRewardEn = "Sky Conqueror",
                    expiresAtMs = now + duration72h
                )
            )
            "quantum_supercomputer" -> listOf(
                ConsortiumExportTender(
                    id = "tender_quantum_asia_${project.id.take(6)}",
                    titleTr = "Doğu Asya Finans & Kuantum Ağı İhalesi",
                    titleEn = "East Asia Quantum AI Backbone",
                    descriptionTr = "Tokyo ve Singapur merkez bankaları için 10 adet kuantum süperbilgisayar kümesi.",
                    descriptionEn = "10 quantum supercomputing clusters for Tokyo and Singapore central finance hubs.",
                    destinationCountry = "Japonya & Singapur",
                    countryFlag = "🇯🇵",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 10,
                    cashReward = 3_000_000_000L,
                    reputationReward = 650,
                    badgeRewardTr = "Kuantum Çağı Mimarı",
                    badgeRewardEn = "Quantum Era Architect",
                    expiresAtMs = now + duration72h
                )
            )
            "fusion_reactor_core" -> listOf(
                ConsortiumExportTender(
                    id = "tender_fusion_iter_${project.id.take(6)}",
                    titleTr = "Uluslararası Temiz Enerji Şebekesi İhalesi",
                    titleEn = "International Clean Energy Grid",
                    descriptionTr = "ITER ve küresel şebekeler için 4 adet manyetik hapseden füzyon çekirdeği.",
                    descriptionEn = "4 magnetic confinement fusion cores for global clean energy powerplants.",
                    destinationCountry = "Uluslararası Konsorsiyum",
                    countryFlag = "🌐",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 4,
                    cashReward = 4_500_000_000L,
                    reputationReward = 850,
                    badgeRewardTr = "Yıldız Enerjisi İhracatçısı",
                    badgeRewardEn = "Star Power Exporter",
                    expiresAtMs = now + duration72h
                )
            )
            "super_yacht" -> listOf(
                ConsortiumExportTender(
                    id = "tender_yacht_med_${project.id.take(6)}",
                    titleTr = "Akdeniz & Monako Prestij Filosu İhalesi",
                    titleEn = "Mediterranean Luxury Yacht Fleet",
                    descriptionTr = "Monako ve Cote d'Azur marinaları için 10 adet ultra lüks hibrit süper yat.",
                    descriptionEn = "10 ultra-luxury hybrid super yachts for Monaco and Riviera maritime fleets.",
                    destinationCountry = "Monako & İtalya",
                    countryFlag = "🇲🇨",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 10,
                    cashReward = 1_500_000_000L,
                    reputationReward = 300,
                    badgeRewardTr = "Akdeniz Prestij Şampiyonu",
                    badgeRewardEn = "Mediterranean Prestige Champion",
                    expiresAtMs = now + duration48h
                )
            )
            "cargo_ship" -> listOf(
                ConsortiumExportTender(
                    id = "tender_cargo_panama_${project.id.take(6)}",
                    titleTr = "Trans-Pasifik Mega Konteyner Taşımacılığı",
                    titleEn = "Trans-Pacific Mega Freight Contract",
                    descriptionTr = "Panama Kanalı genişletilmiş hatları için 10 adet LNG motorlu mega kargo gemisi.",
                    descriptionEn = "10 LNG-powered mega container ships for international Pacific routes.",
                    destinationCountry = "Güney Kore & Panama",
                    countryFlag = "🇰🇷",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 10,
                    cashReward = 1_750_000_000L,
                    reputationReward = 350,
                    badgeRewardTr = "Küresel Ticaret Omurgası",
                    badgeRewardEn = "Global Trade Backbone",
                    expiresAtMs = now + duration48h
                )
            )
            "ai_datacenter" -> listOf(
                ConsortiumExportTender(
                    id = "tender_ai_nordic_${project.id.take(6)}",
                    titleTr = "İskandinav Yeşil Bulut Altyapı İhalesi",
                    titleEn = "Nordic Green Cloud Infrastructure",
                    descriptionTr = "İsveç ve Norveç jeotermal santralleri ile entegre 8 adet modüler AI Veri Merkezi.",
                    descriptionEn = "8 modular AI Data Centers integrated with Nordic geothermal power grids.",
                    destinationCountry = "İsveç & Norveç",
                    countryFlag = "🇸🇪",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 8,
                    cashReward = 2_200_000_000L,
                    reputationReward = 450,
                    badgeRewardTr = "Kutup Veri Devi",
                    badgeRewardEn = "Nordic Data Giant",
                    expiresAtMs = now + duration72h
                )
            )
            else -> listOf(
                ConsortiumExportTender(
                    id = "tender_generic_${project.id.take(6)}",
                    titleTr = "Uluslararası Sanayi İhracat İhalesi",
                    titleEn = "International Industrial Export Tender",
                    descriptionTr = "${project.targetProductName} için küresel sanayi tedarik anlaşması.",
                    descriptionEn = "Global industrial supply contract for ${project.targetProductName}.",
                    destinationCountry = "Küresel Pazar",
                    countryFlag = "🌐",
                    targetProductId = project.targetProductId,
                    requiredQuantity = 10,
                    cashReward = 1_000_000_000L,
                    reputationReward = 250,
                    badgeRewardTr = "İhracat Şampiyonu",
                    badgeRewardEn = "Export Champion",
                    expiresAtMs = now + duration48h
                )
            )
        }
    }
}

enum class ConsortiumSalesChannel(
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val priceMultiplier: Float = 1.0f,
    val reputationBonus: Int = 0
) {
    PAZAR(
        "Küresel Pazar (B2C)",
        "Global Consumer Market (B2C)",
        "Ürünler küresel tüketici pazarında %25 Marka Primi fiyatı ile satılır.",
        "Products are sold on the global consumer market with a +25% Brand Premium price.",
        1.25f,
        25
    ),
    BORSA(
        "Spot Borsa (B2B)",
        "Spot Commodity Exchange (B2B)",
        "Ürünler Borsa Spot piyasasında kurumsal şirketlere satılarak anında nakit ve itibar sağlar.",
        "Products are sold on the B2B Spot Exchange to institutions for instant cash liquidity and prestige.",
        1.0f,
        50
    ),
    DEVLET_IHALE(
        "Devlet Savunma & Sanayi İhalesi",
        "State Defense & Strategic Tender",
        "Savunma Sanayii ve Kamu ihale sözleşmesiyle %40 Kamu Destek Teşviki ve +100 Prestij İtibarı.",
        "Sold via State Defense and Industry Procurement with +40% Government Subsidy and +100 Prestige.",
        1.40f,
        100
    );

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
    fun getDescription(isEnglish: Boolean = false): String = if (isEnglish) descriptionEn else descriptionTr
}

enum class ConsortiumProductionStrategy(
    val titleTr: String,
    val titleEn: String,
    val badgeTr: String,
    val badgeEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val speedMultiplier: Float,
    val brandBonusMultiplier: Float,
    val riskDescriptionTr: String = ""
) {
    DENGELI(
        "Dengeli Sanayi Standardı",
        "Balanced Industry Standard",
        "⚖️ Dengeli",
        "⚖️ Balanced",
        "Standart montaj hızı ve dengeli kalite kontrol protokolleri.",
        "Standard production speed and balanced quality assurance.",
        1.0f,
        1.0f,
        "Normal süre ve dengeli kâr marjı."
    ),
    HIZLI_MONTAJ(
        "Hızlı Montaj & Seri İvme",
        "Turbo Fast Assembly",
        "⚡ Hızlı Montaj (+%30 Hız)",
        "⚡ Turbo Assembly (+30% Speed)",
        "Üretim süresi %30 kısalır; montaj hatları maksimum hızda çalışır.",
        "Production cycle is 30% faster; assembly lines operate at maximum throughput.",
        0.70f,
        0.95f,
        "%30 daha hızlı parti tamamlama, hafif marka çarpanı."
    ),
    HASSAS_KALITE(
        "Hassas Askeri / Havacılık Kalitesi",
        "Precision Military / Aero Grade",
        "💎 Hassas Kalite (1.4x Marka Çarpanı)",
        "💎 Precision Grade (1.4x Brand Multiplier)",
        "Ultra titiz kalite denetimi; parti başına +%40 Marka & Borsa Değeri çarpanı kazandırır.",
        "Rigorous quality calibration; grants +40% Brand & Market Valuation multiplier.",
        1.20f,
        1.40f,
        "+%40 daha yüksek satış geliri ve prestij, %20 daha uzun montaj süresi."
    );

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
    fun getBadge(isEnglish: Boolean = false): String = if (isEnglish) badgeEn else badgeTr
    fun getDescription(isEnglish: Boolean = false): String = if (isEnglish) descriptionEn else descriptionTr

    companion object {
        fun fromString(str: String?): ConsortiumProductionStrategy {
            return try {
                if (str != null) valueOf(str) else DENGELI
            } catch (_: Exception) {
                DENGELI
            }
        }
    }
}

enum class ConsortiumHonorBadge(
    val titleTr: String,
    val titleEn: String,
    val badgeIcon: String,
    val colorHex: Long,
    val descriptionTr: String
) {
    CHIEF_SUPPLIER(
        "Baş Tedarikçi",
        "Chief Supplier",
        "🥇",
        0xFFFFD700,
        "Konsorsiyuma en yüksek hacimde hammadde ve parça teslim eden ana sanayi ortağı."
    ),
    LOGISTICS_SAVIOR(
        "Lojistik Kurtarıcısı",
        "Logistics Savior",
        "⚡",
        0xFF38BDF8,
        "Darboğaz ve kriz anında en hızlı acil ikmali sağlayan lojistik kahramanı."
    ),
    INDUSTRIAL_TITAN(
        "Sanayi Devi",
        "Industrial Titan",
        "🏛️",
        0xFFA855F7,
        "En yüksek sermaye ve en çok üretim kotası taahhüt eden ağır sanayi devi."
    ),
    FOUNDING_VISIONARY(
        "Kurucu Vizyoner",
        "Founding Visionary",
        "👑",
        0xFFF59E0B,
        "Konsorsiyum lisansını başlatan ve projeyi hayata geçiren lider kurucu."
    );

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
}

data class ConsortiumPodiumMember(
    val playerId: String,
    val playerName: String,
    val rank: Int,
    val totalQuantityDelivered: Int,
    val totalValueContributed: Long,
    val slotsOwned: Int,
    val sharePercentage: Float,
    val badges: List<ConsortiumHonorBadge>,
    val isLeader: Boolean = false
)

enum class ConsortiumProposalType(
    val titleTr: String,
    val titleEn: String
) {
    SALES_CHANNEL("Satış Kanalı Tercihi", "Sales Channel Strategy"),
    PRODUCTION_STRATEGY("Kalite vs Hız Modu", "Quality vs Speed Mode"),
    DIVIDEND_REINVESTMENT("Kâr Payı Dağıtım Politikası", "Dividend Policy");

    fun getTitle(isEnglish: Boolean = false): String = if (isEnglish) titleEn else titleTr
}

data class ConsortiumBoardProposal(
    val id: String = UUID.randomUUID().toString(),
    val proposerPlayerId: String,
    val proposerPlayerName: String,
    val titleTr: String,
    val titleEn: String,
    val descriptionTr: String,
    val descriptionEn: String,
    val proposalType: ConsortiumProposalType,
    val proposedValue: String,
    val votes: Map<String, Boolean> = emptyMap(),
    val createdAtMs: Long = System.currentTimeMillis(),
    val expiresAtMs: Long = System.currentTimeMillis() + 48 * 3600 * 1000L,
    val isEnacted: Boolean = false
) {
    val yesVotesCount: Int get() = votes.values.count { it }
    val noVotesCount: Int get() = votes.values.count { !it }
    val totalVotesCount: Int get() = votes.size
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAtMs

    companion object {
        fun fromMap(map: Map<String, Any?>): ConsortiumBoardProposal {
            val typeStr = map["proposalType"] as? String ?: ConsortiumProposalType.SALES_CHANNEL.name
            val typeVal = try { ConsortiumProposalType.valueOf(typeStr) } catch (_: Exception) { ConsortiumProposalType.SALES_CHANNEL }
            val rawVotes = (map["votes"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                if (k is String && v is Boolean) k to v else null
            }?.toMap() ?: emptyMap()

            return ConsortiumBoardProposal(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                proposerPlayerId = map["proposerPlayerId"] as? String ?: "",
                proposerPlayerName = map["proposerPlayerName"] as? String ?: "",
                titleTr = map["titleTr"] as? String ?: "",
                titleEn = map["titleEn"] as? String ?: "",
                descriptionTr = map["descriptionTr"] as? String ?: "",
                descriptionEn = map["descriptionEn"] as? String ?: "",
                proposalType = typeVal,
                proposedValue = map["proposedValue"] as? String ?: "",
                votes = rawVotes,
                createdAtMs = (map["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                expiresAtMs = (map["expiresAtMs"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 48 * 3600 * 1000L),
                isEnacted = map["isEnacted"] as? Boolean ?: false
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "proposerPlayerId" to proposerPlayerId,
            "proposerPlayerName" to proposerPlayerName,
            "titleTr" to titleTr,
            "titleEn" to titleEn,
            "descriptionTr" to descriptionTr,
            "descriptionEn" to descriptionEn,
            "proposalType" to proposalType.name,
            "proposedValue" to proposedValue,
            "votes" to votes,
            "createdAtMs" to createdAtMs,
            "expiresAtMs" to expiresAtMs,
            "isEnacted" to isEnacted
        )
    }
}

data class MegaProject(
    val id: String = UUID.randomUUID().toString(),
    val consortiumName: String,
    val brandName: String,
    val targetProductId: String, // Tier 4 Ürün ID
    val targetProductName: String,
    val leaderPlayerId: String, // Kurucu Admin (Founder)
    val leaderPlayerName: String,
    val slots: List<ConsortiumSupplierSlot>, // 4 Çeşit Tedarik Parçası (d, e, f, g)
    val currentStage: MegaProjectStage = MegaProjectStage.STAGE_1_BODY,
    val totalProjectValue: Long, // Satış / İhale Bütçesi
    val brandReputationGain: Int = 50,
    val brandMultiplier: Float = 1.25f, // %25 Premium Marka Fiyatı
    val createdAtMs: Long = System.currentTimeMillis(),
    val completedAtMs: Long? = null,
    val isDividendClaimed: Boolean = false,
    val salesChannel: ConsortiumSalesChannel = ConsortiumSalesChannel.BORSA,
    val warehouseStock: Int = 0,
    val warehouseCapacity: Int = 1000,
    val isProductionPaused: Boolean = false,
    val isTestProductProduced: Boolean = false,
    val testProducedAtMs: Long? = null,
    val isMassProductionApproved: Boolean = false,
    val massProductionApprovedAtMs: Long? = null,
    val contractSignedAtMs: Long? = null,
    val preparationCountdownDurationMs: Long = 24 * 3600 * 1000L, // 1 Günlük Hazırlık/Tedarik Geri Sayımı
    val totalItemsProduced: Int = 0,
    val isAutoSellActive: Boolean = true,
    val lastBatchStartTimeMs: Long = System.currentTimeMillis(),
    val lastBatchDurationMs: Long = 0L,
    val previousSharePrice: Double = 0.0,
    val sharePriceMultiplier: Double = 1.0,
    val batchProductionDurationSeconds: Int = 450,
    val isBatchInProduction: Boolean = false,
    val batchProductionStartTimeMs: Long = 0L,
    val qualityTier: ConsortiumQualityTier = ConsortiumQualityTier.GRADE_C,
    val cityId: String = "istanbul",
    val maxPartnerQuotaPercent: Float = 40.0f,
    val chiefEngineerId: String? = null,
    val chiefEngineerName: String? = null,
    val logisticsChiefId: String? = null,
    val logisticsChiefName: String? = null,
    val lastNudgeTimeMs: Long = 0L,
    val activeTenders: List<ConsortiumExportTender> = emptyList(),
    val completedTenderBadges: List<String> = emptyList(),
    val productionStrategy: ConsortiumProductionStrategy = ConsortiumProductionStrategy.DENGELI,
    val boardProposals: List<ConsortiumBoardProposal> = emptyList(),
    val averageCraftsmanshipScore: Double = 1.0 // 1.0 to 5.0 zanaatkarlık & kalite mirası
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): MegaProject {
            val stageStr = map["currentStage"] as? String ?: MegaProjectStage.STAGE_1_BODY.name
            val stageVal = try { MegaProjectStage.valueOf(stageStr) } catch (_: Exception) { MegaProjectStage.STAGE_1_BODY }

            val channelStr = map["salesChannel"] as? String ?: ConsortiumSalesChannel.BORSA.name
            val channelVal = try { ConsortiumSalesChannel.valueOf(channelStr) } catch (_: Exception) { ConsortiumSalesChannel.BORSA }

            val strategyStr = map["productionStrategy"] as? String
            val strategyVal = ConsortiumProductionStrategy.fromString(strategyStr)

            val cityIdStr = map["cityId"] as? String ?: "istanbul"

            val qualityStr = map["qualityTier"] as? String
            val qualityVal = ConsortiumQualityTier.fromString(qualityStr)

            val rawSlots = map["slots"] as? List<*> ?: emptyList<Any>()
            val slotList = rawSlots.mapNotNull { item ->
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    ConsortiumSupplierSlot.fromMap(item as Map<String, Any?>)
                } else null
            }

            val targetProd = map["targetProductId"] as? String ?: ""
            val defaultDuration = when (targetProd) {
                "defense_frigate" -> 600
                "space_rocket" -> 900
                "quantum_supercomputer" -> 450
                "fusion_reactor_core" -> 750
                "super_yacht" -> 600
                "ai_datacenter" -> 450
                "cargo_ship" -> 450
                "bullet_train" -> 450
                else -> 450
            }

            val rawDuration = (map["batchProductionDurationSeconds"] as? Number)?.toInt() ?: defaultDuration
            val durationSec = if (rawDuration in 1..90) rawDuration * 10 else rawDuration

            val rawProposals = map["boardProposals"] as? List<*> ?: emptyList<Any>()
            val proposalList = rawProposals.mapNotNull { item ->
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    ConsortiumBoardProposal.fromMap(item as Map<String, Any?>)
                } else null
            }

            return MegaProject(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                consortiumName = map["consortiumName"] as? String ?: "",
                brandName = map["brandName"] as? String ?: "",
                targetProductId = targetProd,
                targetProductName = map["targetProductName"] as? String ?: "",
                leaderPlayerId = map["leaderPlayerId"] as? String ?: "",
                leaderPlayerName = map["leaderPlayerName"] as? String ?: "",
                slots = slotList,
                currentStage = stageVal,
                totalProjectValue = (map["totalProjectValue"] as? Number)?.toLong() ?: 0L,
                brandReputationGain = (map["brandReputationGain"] as? Number)?.toInt() ?: 50,
                brandMultiplier = (map["brandMultiplier"] as? Number)?.toFloat() ?: 1.25f,
                createdAtMs = (map["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completedAtMs = (map["completedAtMs"] as? Number)?.toLong(),
                isDividendClaimed = map["isDividendClaimed"] as? Boolean ?: false,
                salesChannel = channelVal,
                cityId = cityIdStr,
                warehouseStock = (map["warehouseStock"] as? Number)?.toInt() ?: 0,
                warehouseCapacity = (map["warehouseCapacity"] as? Number)?.toInt() ?: 1000,
                isProductionPaused = map["isProductionPaused"] as? Boolean ?: false,
                isTestProductProduced = map["isTestProductProduced"] as? Boolean ?: false,
                testProducedAtMs = (map["testProducedAtMs"] as? Number)?.toLong(),
                isMassProductionApproved = map["isMassProductionApproved"] as? Boolean ?: false,
                massProductionApprovedAtMs = (map["massProductionApprovedAtMs"] as? Number)?.toLong(),
                contractSignedAtMs = (map["contractSignedAtMs"] as? Number)?.toLong(),
                preparationCountdownDurationMs = (map["preparationCountdownDurationMs"] as? Number)?.toLong() ?: (24 * 3600 * 1000L),
                totalItemsProduced = (map["totalItemsProduced"] as? Number)?.toInt() ?: 0,
                isAutoSellActive = map["isAutoSellActive"] as? Boolean ?: true,
                lastBatchStartTimeMs = (map["lastBatchStartTimeMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastBatchDurationMs = (map["lastBatchDurationMs"] as? Number)?.toLong() ?: 0L,
                previousSharePrice = (map["previousSharePrice"] as? Number)?.toDouble() ?: 0.0,
                sharePriceMultiplier = (map["sharePriceMultiplier"] as? Number)?.toDouble() ?: 1.0,
                batchProductionDurationSeconds = durationSec,
                isBatchInProduction = map["isBatchInProduction"] as? Boolean ?: false,
                batchProductionStartTimeMs = (map["batchProductionStartTimeMs"] as? Number)?.toLong() ?: 0L,
                qualityTier = qualityVal,
                maxPartnerQuotaPercent = (map["maxPartnerQuotaPercent"] as? Number)?.toFloat() ?: 40.0f,
                chiefEngineerId = map["chiefEngineerId"] as? String,
                chiefEngineerName = map["chiefEngineerName"] as? String,
                logisticsChiefId = map["logisticsChiefId"] as? String,
                logisticsChiefName = map["logisticsChiefName"] as? String,
                lastNudgeTimeMs = (map["lastNudgeTimeMs"] as? Number)?.toLong() ?: 0L,
                activeTenders = (map["activeTenders"] as? List<*>)?.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        ConsortiumExportTender.fromMap(item as Map<String, Any?>)
                    } else null
                } ?: emptyList(),
                completedTenderBadges = (map["completedTenderBadges"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                productionStrategy = strategyVal,
                boardProposals = proposalList,
                averageCraftsmanshipScore = (map["averageCraftsmanshipScore"] as? Number)?.toDouble() ?: 1.0
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "consortiumName" to consortiumName,
            "brandName" to brandName,
            "targetProductId" to targetProductId,
            "targetProductName" to targetProductName,
            "leaderPlayerId" to leaderPlayerId,
            "leaderPlayerName" to leaderPlayerName,
            "slots" to slots.map { it.toMap() },
            "currentStage" to currentStage.name,
            "totalProjectValue" to totalProjectValue,
            "brandReputationGain" to brandReputationGain,
            "brandMultiplier" to brandMultiplier.toDouble(),
            "createdAtMs" to createdAtMs,
            "completedAtMs" to completedAtMs,
            "isDividendClaimed" to isDividendClaimed,
            "salesChannel" to salesChannel.name,
            "cityId" to cityId,
            "warehouseStock" to warehouseStock,
            "warehouseCapacity" to warehouseCapacity,
            "isProductionPaused" to isProductionPaused,
            "isTestProductProduced" to isTestProductProduced,
            "testProducedAtMs" to testProducedAtMs,
            "isMassProductionApproved" to isMassProductionApproved,
            "massProductionApprovedAtMs" to massProductionApprovedAtMs,
            "contractSignedAtMs" to contractSignedAtMs,
            "preparationCountdownDurationMs" to preparationCountdownDurationMs,
            "totalItemsProduced" to totalItemsProduced,
            "isAutoSellActive" to isAutoSellActive,
            "lastBatchStartTimeMs" to lastBatchStartTimeMs,
            "lastBatchDurationMs" to lastBatchDurationMs,
            "previousSharePrice" to previousSharePrice,
            "sharePriceMultiplier" to sharePriceMultiplier,
            "batchProductionDurationSeconds" to standardBatchDurationSeconds,
            "isBatchInProduction" to isBatchInProduction,
            "batchProductionStartTimeMs" to batchProductionStartTimeMs,
            "qualityTier" to qualityTier.name,
            "maxPartnerQuotaPercent" to maxPartnerQuotaPercent.toDouble(),
            "chiefEngineerId" to chiefEngineerId,
            "chiefEngineerName" to chiefEngineerName,
            "logisticsChiefId" to logisticsChiefId,
            "logisticsChiefName" to logisticsChiefName,
            "lastNudgeTimeMs" to lastNudgeTimeMs,
            "activeTenders" to activeTenders.map { it.toMap() },
            "completedTenderBadges" to completedTenderBadges,
            "productionStrategy" to productionStrategy.name,
            "boardProposals" to boardProposals.map { it.toMap() },
            "averageCraftsmanshipScore" to averageCraftsmanshipScore
        )
    }

    val signedContractCount: Int
        get() = slots.count { it.assignedPartnerId != null }

    val isMinContractsSigned: Boolean
        get() = signedContractCount >= 2

    val isAllContractsSigned: Boolean
        get() = signedContractCount == slots.size

    val countdownEndMs: Long
        get() = (contractSignedAtMs ?: createdAtMs) + preparationCountdownDurationMs

    val remainingPreparationCountdownMs: Long
        get() {
            if (isTestProductProduced || isMassProductionApproved) return 0L
            if (!isMinContractsSigned) return preparationCountdownDurationMs
            return (countdownEndMs - GameTime.now()).coerceAtLeast(0L)
        }

    val isPreparationCountdownActive: Boolean
        get() = isMinContractsSigned && !isTestProductProduced && remainingPreparationCountdownMs > 0L

    val isTestProductionReady: Boolean
        get() = slots.all { it.isFullyDelivered }

    val founderFeePercent: Float
        get() = 10.0f // Kurucu Yönetim Payı: %10

    val supplierPoolPercent: Float
        get() = 90.0f // Tedarikçi Katma Değer Havuzu: %90

    fun calculateSlotSupplierShare(slot: ConsortiumSupplierSlot): Float {
        val totalCost = slots.sumOf { it.costContributionValue }.coerceAtLeast(1L)
        val weight = slot.costContributionValue.toFloat() / totalCost
        return (weight * supplierPoolPercent)
    }

    val standardBatchDurationSeconds: Int
        get() {
            if (batchProductionDurationSeconds > 0) {
                if (batchProductionDurationSeconds in 1..90) {
                    return batchProductionDurationSeconds * 10
                }
                return batchProductionDurationSeconds
            }
            return when (targetProductId) {
                "defense_frigate" -> 600
                "space_rocket" -> 900
                "quantum_supercomputer" -> 450
                "fusion_reactor_core" -> 750
                "super_yacht" -> 600
                "ai_datacenter" -> 450
                "cargo_ship" -> 450
                "bullet_train" -> 450
                else -> 450
            }
        }

    val effectiveBatchProductionDurationSeconds: Int
        get() {
            val base = (standardBatchDurationSeconds * productionStrategy.speedMultiplier).toInt()
            return if (chiefEngineerId != null) {
                (base * 0.85f).toInt().coerceAtLeast(20) // %15 Hızlandırılmış Ar-Ge Üretim Süresi
            } else {
                base.coerceAtLeast(20)
            }
        }

    val logisticsTransitSpeedMultiplier: Float
        get() = if (logisticsChiefId != null) 0.80f else 1.0f // Lojistik Sorumlusu sevkiyat süresini %20 hızlandırır

    fun getRoleForPlayer(playerId: String): ConsortiumRole {
        val pId = playerId.ifBlank { "local_player" }
        return when {
            pId == leaderPlayerId -> ConsortiumRole.PRESIDENT
            pId == chiefEngineerId -> ConsortiumRole.CHIEF_ENGINEER
            pId == logisticsChiefId -> ConsortiumRole.LOGISTICS_CHIEF
            slots.any { it.assignedPartnerId == pId } -> ConsortiumRole.SUPPLIER
            else -> ConsortiumRole.SUPPLIER
        }
    }

    fun canPlayerClaimSlot(playerId: String): Pair<Boolean, String> {
        val pId = playerId.ifBlank { "local_player" }
        val currentClaimedCount = slots.count { it.assignedPartnerId == pId }
        // 4 slotluk konsorsiyumda tek bir ortak azami 2 slot alabilir (%40-%50 tavan kotası)
        if (currentClaimedCount >= 2) {
            return Pair(false, "Adil Dağıtım Kuralı: Bir ortak azami 2 tedarikçi slotu (%40 kota tavanı) üstlenebilir. Diğer slotlar farklı sanayi ortaklarına ayrılmıştır.")
        }
        return Pair(true, "")
    }

    fun canPlayerDeliver(playerId: String, slotId: String, requestedQuantity: Int, itemQualityStars: Int = 1): Pair<Boolean, String> {
        val pId = playerId.ifBlank { "local_player" }
        val targetSlot = slots.find { it.slotId == slotId }
            ?: return Pair(false, "Geçersiz tedarik slotu.")

        if (targetSlot.assignedPartnerId != null && targetSlot.assignedPartnerId != pId) {
            return Pair(false, "Bu slot başka bir ortağa atanmış.")
        }

        if (targetSlot.quantityDelivered + requestedQuantity > targetSlot.quantityRequired) {
            return Pair(false, "Bu slot için talep edilen miktardan fazlası teslim edilemez.")
        }

        if (!qualityTier.isStarsAllowed(itemQualityStars)) {
            return Pair(false, "Teslim edilmek istenen ürün kalitesi (${itemQualityStars}★) bu konsorsiyum projesinin ${qualityTier.titleTr} (${qualityTier.allowedQualityRangeTextTr}) standardına kesinlikle uymuyor!")
        }

        return Pair(true, "")
    }

    val remainingProductionTimeMs: Long
        get() {
            if (!isBatchInProduction || batchProductionStartTimeMs <= 0L) return 0L
            val endTime = batchProductionStartTimeMs + (effectiveBatchProductionDurationSeconds * 1000L)
            return (endTime - GameTime.now()).coerceAtLeast(0L)
        }

    val isProductionTimeCompleted: Boolean
        get() = isBatchInProduction && remainingProductionTimeMs <= 0L

    val productionProgressFraction: Float
        get() {
            if (!isBatchInProduction || batchProductionStartTimeMs <= 0L) return 0f
            val durationMs = effectiveBatchProductionDurationSeconds * 1000f
            if (durationMs <= 0f) return 1f
            val elapsedMs = (GameTime.now() - batchProductionStartTimeMs).coerceAtLeast(0L).toFloat()
            return (elapsedMs / durationMs).coerceIn(0f, 1f)
        }

    val masterCraftsmanshipTier: ProductQuality
        get() {
            val deliveredSlots = slots.filter { it.quantityDelivered > 0 }
            val avgScore = if (deliveredSlots.isNotEmpty()) {
                deliveredSlots.map { it.deliveredQualityTier.toDouble() }.average()
            } else {
                averageCraftsmanshipScore
            }
            return when {
                avgScore >= 4.2 -> ProductQuality.PALACE_GRAND
                avgScore >= 3.2 -> ProductQuality.MASTERWORK
                avgScore >= 2.2 -> ProductQuality.SPECIAL
                avgScore >= 1.5 -> ProductQuality.SELECTED
                else -> ProductQuality.STANDARD
            }
        }

    val baseSharePrice: Double
        get() {
            val base = (unitBatchPrice.toDouble() / 1000.0) * qualityTier.borsaMultiplier
            return if (isProductionPaused) base * 0.15 else base
        }

    val currentSharePrice: Double
        get() {
            val base = baseSharePrice
            val craftsmanshipBonus = (masterCraftsmanshipTier.tier - 1) * 0.15 // 1★: +%0, 2★: +%15, 3★: +%30, 4★: +%45, 5★: +%60
            val mult = if (sharePriceMultiplier > 0.0) sharePriceMultiplier else 1.0
            return (base * mult * (1.0 + craftsmanshipBonus)).coerceAtLeast(10.0)
        }

    val sharePriceChangePercent: Double
        get() {
            val prev = if (previousSharePrice > 0.0) previousSharePrice else baseSharePrice
            if (prev <= 0.0) return 0.0
            return (((currentSharePrice - prev) / prev) * 100.0).coerceIn(-90.0, 500.0)
        }

    val dynamicBrandMultiplier: Float
        get() = (brandMultiplier + (totalItemsProduced * 0.05f)).coerceAtMost(3.5f)

    val unitBatchCost: Long
        get() = slots.sumOf { it.costContributionValue }.coerceAtLeast(10_000_000L)

    val craftsmanshipMultiplier: Float
        get() {
            val bonusPct = (((masterCraftsmanshipTier.tier - 1).toFloat() / 4.0f) * 0.40f) // 1★: 1.0x, 5★: 1.40x (+%40 Sinerji Bonusu)
            return 1.0f + bonusPct
        }

    val unitBatchPrice: Long
        get() {
            val baseCost = unitBatchCost
            val baseVal = (baseCost * 1.50f).toLong()
            val channelMult = when (salesChannel) {
                ConsortiumSalesChannel.DEVLET_IHALE -> dynamicBrandMultiplier * 1.40f
                ConsortiumSalesChannel.PAZAR -> dynamicBrandMultiplier * 1.25f
                ConsortiumSalesChannel.BORSA -> dynamicBrandMultiplier * 0.95f
            }
            val strategyMult = productionStrategy.brandBonusMultiplier
            return (baseVal * channelMult * qualityTier.borsaMultiplier * strategyMult * craftsmanshipMultiplier).toLong()
        }

    fun getPodiumStandings(currentUserId: String = ""): List<ConsortiumPodiumMember> {
        val assignedSlots = slots.filter { it.assignedPartnerId != null }
        val partnerIds = (assignedSlots.mapNotNull { it.assignedPartnerId } + leaderPlayerId).distinct()
        if (partnerIds.isEmpty()) return emptyList()

        val totalValue = slots.sumOf { it.costContributionValue }.coerceAtLeast(1L)

        val rawMembers = partnerIds.map { pid ->
            val pSlots = slots.filter { it.assignedPartnerId == pid }
            val name = pSlots.firstOrNull()?.assignedPartnerName 
                ?: if (pid == leaderPlayerId) leaderPlayerName else if (pid == currentUserId) "Siz (Yerel Şirket)" else "Sanayi Ortağı"
            val deliveredQty = pSlots.sumOf { it.quantityDelivered }
            val contributedVal = pSlots.sumOf { it.actualCostIncurred.takeIf { c -> c > 0 } ?: (it.quantityDelivered * (it.costContributionValue / it.quantityRequired.coerceAtLeast(1))) }
            val sharePct = (pSlots.sumOf { it.costContributionValue }.toFloat() / totalValue.toFloat()) * 100f
            val isLeaderMember = pid == leaderPlayerId

            val badges = mutableListOf<ConsortiumHonorBadge>()
            if (isLeaderMember) badges.add(ConsortiumHonorBadge.FOUNDING_VISIONARY)
            if (pSlots.size >= 2) badges.add(ConsortiumHonorBadge.INDUSTRIAL_TITAN)
            if (pSlots.any { it.isBottleneckWarning && it.quantityDelivered > 0 }) badges.add(ConsortiumHonorBadge.LOGISTICS_SAVIOR)

            ConsortiumPodiumMember(
                playerId = pid,
                playerName = name,
                rank = 0,
                totalQuantityDelivered = deliveredQty,
                totalValueContributed = contributedVal,
                slotsOwned = pSlots.size,
                sharePercentage = sharePct,
                badges = badges,
                isLeader = isLeaderMember
            )
        }

        // Sort by total quantity delivered and then contributed value
        val sorted = rawMembers.sortedWith(
            compareByDescending<ConsortiumPodiumMember> { it.totalQuantityDelivered }
                .thenByDescending { it.totalValueContributed }
                .thenByDescending { it.isLeader }
        )

        val topDelivererId = sorted.firstOrNull { it.totalQuantityDelivered > 0 }?.playerId

        return sorted.mapIndexed { index, m ->
            val updatedBadges = m.badges.toMutableList()
            if (m.playerId == topDelivererId && !updatedBadges.contains(ConsortiumHonorBadge.CHIEF_SUPPLIER)) {
                updatedBadges.add(0, ConsortiumHonorBadge.CHIEF_SUPPLIER)
            }
            m.copy(rank = index + 1, badges = updatedBadges)
        }
    }

    val hasEmptySlots: Boolean
        get() = slots.any { it.assignedPartnerId == null }

    val isMassProductionActive: Boolean
        get() = (currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION || currentStage == MegaProjectStage.COMPLETED) && !hasEmptySlots && slots.all { it.isFullyDelivered }

    val totalRequiredItems: Int
        get() = slots.sumOf { it.quantityRequired }

    val totalDeliveredItems: Int
        get() = slots.sumOf { it.quantityDelivered }

    val overallProgressFraction: Float
        get() = if (totalRequiredItems > 0) totalDeliveredItems.toFloat() / totalRequiredItems else 0f

    val currentStageProgressFraction: Float
        get() {
            val stageSlots = slots.filter { it.stage == currentStage }
            if (stageSlots.isEmpty()) return 1f
            val req = stageSlots.sumOf { it.quantityRequired }
            val del = stageSlots.sumOf { it.quantityDelivered }
            return if (req > 0) del.toFloat() / req else 1f
        }

    val isCurrentStageFinished: Boolean
        get() = slots.filter { it.stage == currentStage }.all { it.isFullyDelivered }

    val isAllStagesFinished: Boolean
        get() = slots.all { it.isFullyDelivered }

    val participatingPartnerIds: List<String>
        get() = (listOf(leaderPlayerId) + slots.mapNotNull { it.assignedPartnerId }).distinct()

    fun getIconForProduct(): ImageVector {
        return when (targetProductId) {
            "defense_frigate" -> Icons.Rounded.Security
            "space_rocket" -> Icons.Rounded.RocketLaunch
            "quantum_supercomputer" -> Icons.Rounded.Memory
            "fusion_reactor_core" -> Icons.Rounded.Bolt
            "super_yacht" -> Icons.Rounded.DirectionsBoat
            "ai_datacenter" -> Icons.Rounded.Dns
            "cargo_ship" -> Icons.Rounded.DirectionsBoat
            "bullet_train" -> Icons.Rounded.Train
            else -> Icons.Rounded.PrecisionManufacturing
        }
    }

    fun getProductPrimaryColor(): Color {
        return when (targetProductId) {
            "defense_frigate" -> Color(0xFF10B981) // Emerald Neon
            "space_rocket" -> Color(0xFFA855F7) // Cosmic Violet
            "quantum_supercomputer" -> Color(0xFF06B6D4) // Quantum Cyan
            "fusion_reactor_core" -> Color(0xFFF59E0B) // Plasma Gold
            "super_yacht" -> Color(0xFF38BDF8) // Sky Marine
            "ai_datacenter" -> Color(0xFF3B82F6) // Electric Blue
            "cargo_ship" -> Color(0xFF0284C7) // Ocean Navy
            "bullet_train" -> Color(0xFFF97316) // Hyper Orange
            else -> Color(0xFFEC4899) // Hot Magenta
        }
    }

    fun getProductSecondaryColor(): Color {
        return when (targetProductId) {
            "defense_frigate" -> Color(0xFF047857)
            "space_rocket" -> Color(0xFF6D28D9)
            "quantum_supercomputer" -> Color(0xFF0284C7)
            "fusion_reactor_core" -> Color(0xFFD97706)
            "super_yacht" -> Color(0xFF0369A1)
            "ai_datacenter" -> Color(0xFF1D4ED8)
            "cargo_ship" -> Color(0xFF075985)
            "bullet_train" -> Color(0xFFC2410C)
            else -> Color(0xFFBE185D)
        }
    }
}

/**
 * Factory helper to generate default Mega Projects for Tier 4 items
 */
object MegaProjectFactory {

    fun getRequiredIngredientsForProduct(
        targetProductId: String,
        qualityTier: ConsortiumQualityTier,
        allProducts: List<Product>
    ): List<ConsortiumSupplierSlot> {
        val targetProduct = allProducts.find { it.id == targetProductId }
            ?: return emptyList()

        val slots = mutableListOf<ConsortiumSupplierSlot>()
        val recipe = targetProduct.recipe

        if (recipe.size >= 4) {
            recipe.take(4).forEachIndexed { idx, req ->
                val ingredientProduct = allProducts.find { it.id == req.productId }
                val ingName = ingredientProduct?.getDisplayName() ?: req.productId
                val unitPrice = ingredientProduct?.basePrice ?: 50_000L
                val baseAmount = (req.amountPerUnit * 50).coerceAtLeast(10)
                val reqAmount = (baseAmount * qualityTier.multiplier).toInt()
                val totalCost = reqAmount * unitPrice

                val stage = when (idx) {
                    0 -> MegaProjectStage.STAGE_1_BODY
                    1 -> MegaProjectStage.STAGE_2_HARDWARE
                    2 -> MegaProjectStage.STAGE_3_TESTING
                    else -> MegaProjectStage.STAGE_4_MASS_PRODUCTION
                }

                slots.add(
                    ConsortiumSupplierSlot(
                        productId = req.productId,
                        productName = ingName,
                        quantityRequired = reqAmount,
                        quantityDelivered = 0,
                        stage = stage,
                        costContributionValue = totalCost
                    )
                )
            }
        } else if (recipe.isNotEmpty()) {
            recipe.forEachIndexed { idx, req ->
                val ingredientProduct = allProducts.find { it.id == req.productId }
                val ingName = ingredientProduct?.getDisplayName() ?: req.productId
                val unitPrice = ingredientProduct?.basePrice ?: 50_000L
                val baseAmount = (req.amountPerUnit * 50).coerceAtLeast(10)
                val reqAmount = (baseAmount * qualityTier.multiplier).toInt()
                val totalCost = reqAmount * unitPrice
                slots.add(
                    ConsortiumSupplierSlot(
                        productId = req.productId,
                        productName = ingName,
                        quantityRequired = reqAmount,
                        quantityDelivered = 0,
                        stage = when (idx) { 0 -> MegaProjectStage.STAGE_1_BODY 1 -> MegaProjectStage.STAGE_2_HARDWARE 2 -> MegaProjectStage.STAGE_3_TESTING else -> MegaProjectStage.STAGE_4_MASS_PRODUCTION },
                        costContributionValue = totalCost
                    )
                )
            }
            // 4 parçaya tamamla
            val fallbackIngredients = listOf(
                Triple("steel", "Ağır Gövde Çeliği", 200),
                Triple("chip", "Mikroçip & Kontrol Kartı", 100),
                Triple("fuel", "Havacılık / Sanayi Yakıtı", 500),
                Triple("plastic", "Kompozit & Ambalaj Modülü", 250)
            )
            for (fallback in fallbackIngredients) {
                if (slots.size >= 4) break
                if (slots.none { it.productId == fallback.first }) {
                    val prod = allProducts.find { it.id == fallback.first }
                    val price = prod?.basePrice ?: 100_000L
                    val reqAmount = (fallback.third * qualityTier.multiplier).toInt()
                    slots.add(
                        ConsortiumSupplierSlot(
                            productId = fallback.first,
                            productName = fallback.second,
                            quantityRequired = reqAmount,
                            stage = when (slots.size) { 0 -> MegaProjectStage.STAGE_1_BODY 1 -> MegaProjectStage.STAGE_2_HARDWARE 2 -> MegaProjectStage.STAGE_3_TESTING else -> MegaProjectStage.STAGE_4_MASS_PRODUCTION },
                            costContributionValue = reqAmount * price
                        )
                    )
                }
            }
        } else {
            val req1 = (300 * qualityTier.multiplier).toInt()
            val req2 = (40 * qualityTier.multiplier).toInt()
            val req3 = (150 * qualityTier.multiplier).toInt()
            val req4 = (600 * qualityTier.multiplier).toInt()
            slots.add(
                ConsortiumSupplierSlot(
                    productId = "steel",
                    productName = "Ağır Şasi & Zırh Çeliği",
                    productNameRes = R.string.supplier_slot_steel_composite,
                    quantityRequired = req1,
                    stage = MegaProjectStage.STAGE_1_BODY,
                    costContributionValue = req1 * 100_000L
                )
            )
            slots.add(
                ConsortiumSupplierSlot(
                    productId = "turbine_engine",
                    productName = "Güç Aktarımı & Motor Ünitesi",
                    productNameRes = R.string.supplier_slot_turbine_engine,
                    quantityRequired = req2,
                    stage = MegaProjectStage.STAGE_2_HARDWARE,
                    costContributionValue = req2 * 5_000_000L
                )
            )
            slots.add(
                ConsortiumSupplierSlot(
                    productId = "chip",
                    productName = "Yapay Zeka & Otonom Mikroçip",
                    productNameRes = R.string.supplier_slot_ai_microprocessors,
                    quantityRequired = req3,
                    stage = MegaProjectStage.STAGE_2_HARDWARE,
                    costContributionValue = req3 * 2_000_000L
                )
            )
            slots.add(
                ConsortiumSupplierSlot(
                    productId = "fuel",
                    productName = "Yüksek Enerji Bataryası / Yakıt",
                    productNameRes = R.string.supplier_slot_aviation_fuel,
                    quantityRequired = req4,
                    stage = MegaProjectStage.STAGE_3_TESTING,
                    costContributionValue = req4 * 50_000L
                )
            )
        }

        val totalCostSum = slots.sumOf { it.costContributionValue }.coerceAtLeast(1L)
        return slots.take(4).map { slot ->
            val weight = slot.costContributionValue.toFloat() / totalCostSum
            val supplierPoolShare = weight * 90.0f
            slot.copy(sharePercentage = (supplierPoolShare * 10).toInt() / 10f)
        }
    }

    fun createMegaProject(
        consortiumName: String,
        brandName: String,
        targetProductId: String,
        leaderPlayerId: String,
        leaderPlayerName: String,
        allProducts: List<Product>,
        qualityTier: ConsortiumQualityTier = ConsortiumQualityTier.GRADE_C,
        founderClaimedProductIds: Set<String> = emptySet(),
        cityId: String = "istanbul"
    ): MegaProject {
        val targetProduct = allProducts.find { it.id == targetProductId }
            ?: throw IllegalArgumentException("Product $targetProductId not found")

        val generatedSlots = getRequiredIngredientsForProduct(targetProductId, qualityTier, allProducts)

        val finalSlots = generatedSlots.map { slot ->
            if (founderClaimedProductIds.contains(slot.productId)) {
                slot.copy(
                    assignedPartnerId = leaderPlayerId,
                    assignedPartnerName = leaderPlayerName
                )
            } else {
                slot.copy(
                    assignedPartnerId = null,
                    assignedPartnerName = null
                )
            }
        }

        val totalCostSum = finalSlots.sumOf { it.costContributionValue }.coerceAtLeast(1L)
        val totalValue = (totalCostSum * 1.8f * qualityTier.borsaMultiplier).toLong()

        val baseBatchDuration = when (targetProductId) {
            "defense_frigate" -> 600
            "space_rocket" -> 900
            "quantum_supercomputer" -> 450
            "fusion_reactor_core" -> 750
            "super_yacht" -> 600
            "ai_datacenter" -> 450
            "cargo_ship" -> 450
            "bullet_train" -> 450
            else -> 450
        }
        val actualBatchDuration = (baseBatchDuration * qualityTier.durationMultiplier).toInt()

        return MegaProject(
            consortiumName = consortiumName,
            brandName = brandName,
            targetProductId = targetProductId,
            targetProductName = targetProduct.getDisplayName(),
            leaderPlayerId = leaderPlayerId,
            leaderPlayerName = leaderPlayerName,
            slots = finalSlots,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            totalProjectValue = totalValue,
            brandReputationGain = (50 * qualityTier.multiplier).toInt(),
            brandMultiplier = 1.30f * qualityTier.borsaMultiplier,
            warehouseCapacity = 1000,
            batchProductionDurationSeconds = actualBatchDuration,
            qualityTier = qualityTier,
            cityId = cityId
        )
    }
}
