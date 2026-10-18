package com.example.data

import com.example.R

/**
 * Oyun içi temel modüllerin ve özelliklerin kademeli kilit durumlarını ve
 * Kademeli Onboarding (Faz 4) kilit açılış motorunu yöneten merkezi yapı.
 * 
 * Çift Koşullu (Seviye VEYA Net Servet / Net Worth) mil taşlarını ve
 * Özellik Kilit Açılış Teşvik Hibelerini (Hibe Nakit & Elmas) kontrol eder.
 */
enum class GameFeature(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val stageNumber: Int,
    val defaultMinLevel: Int,
    val minRequiredNetWorth: Long,
    val unlockRewardMoney: Long,
    val unlockRewardGems: Int,
    val requiredTechId: String? = null,
    val requiredTechNameTr: String? = null,
    val requiredTechNameEn: String? = null,
    val iconRes: Int? = null
) {
    // Stage 1 (Seviye 1 / 0 ₳ Net Servet): Başlangıçta Açık (50.000 ₳ Hibe)
    FACILITIES(
        id = "feature_facilities",
        titleTr = "Tesisler & Üretim",
        titleEn = "Facilities & Production",
        stageNumber = 1,
        defaultMinLevel = 1,
        minRequiredNetWorth = 0L,
        unlockRewardMoney = 50_000L,
        unlockRewardGems = 0
    ),
    WAREHOUSE(
        id = "feature_warehouse",
        titleTr = "Depo & Envanter",
        titleEn = "Warehouse & Inventory",
        stageNumber = 1,
        defaultMinLevel = 1,
        minRequiredNetWorth = 0L,
        unlockRewardMoney = 50_000L,
        unlockRewardGems = 0
    ),
    MARKET_P2P(
        id = "feature_market_p2p",
        titleTr = "Pazar & Ticaret",
        titleEn = "Market & Trading",
        stageNumber = 1,
        defaultMinLevel = 1,
        minRequiredNetWorth = 0L,
        unlockRewardMoney = 50_000L,
        unlockRewardGems = 0
    ),

    // Stage 2 (Seviye 3 VEYA 150.000 ₳ Net Servet): Şehir Haritası & Lojistik -> 25.000 ₳ Hibe
    CITY_MAP(
        id = "feature_city_map",
        titleTr = "Şehir Haritası & Lojistik",
        titleEn = "City Map & Logistics",
        stageNumber = 2,
        defaultMinLevel = 3,
        minRequiredNetWorth = 150_000L,
        unlockRewardMoney = 25_000L,
        unlockRewardGems = 0,
        requiredTechId = "tech_logistics",
        requiredTechNameTr = "Lojistik & Ticaret Ağı",
        requiredTechNameEn = "Logistics & Trade Network"
    ),

    // Stage 3 (Seviye 5 VEYA 500.000 ₳ Net Servet): İK & Banka -> 50.000 ₳ + 10 💎 Elmas
    HR_MANAGERS(
        id = "feature_hr_managers",
        titleTr = "İnsan Kaynakları & Otomasyon",
        titleEn = "HR & Automation Managers",
        stageNumber = 3,
        defaultMinLevel = 5,
        minRequiredNetWorth = 500_000L,
        unlockRewardMoney = 50_000L,
        unlockRewardGems = 10,
        requiredTechId = "tech_automation",
        requiredTechNameTr = "Endüstriyel Otomasyon",
        requiredTechNameEn = "Industrial Automation"
    ),
    BANKING(
        id = "feature_banking",
        titleTr = "Banka & Mevduat",
        titleEn = "Bank & Deposits",
        stageNumber = 3,
        defaultMinLevel = 5,
        minRequiredNetWorth = 500_000L,
        unlockRewardMoney = 50_000L,
        unlockRewardGems = 10
    ),

    // Stage 4 (Seviye 8 VEYA 2.500.000 ₳ Net Servet): Ar-Ge & Borsa -> 100.000 ₳ + 20 💎 Elmas
    RD_LAB(
        id = "feature_rd_lab",
        titleTr = "Ar-Ge Laboratuvarı",
        titleEn = "R&D Laboratory",
        stageNumber = 4,
        defaultMinLevel = 8,
        minRequiredNetWorth = 2_500_000L,
        unlockRewardMoney = 100_000L,
        unlockRewardGems = 20
    ),
    BORSA(
        id = "feature_borsa",
        titleTr = "Borsa & Vadeli İşlemler",
        titleEn = "Stock Market & Futures",
        stageNumber = 4,
        defaultMinLevel = 8,
        minRequiredNetWorth = 2_500_000L,
        unlockRewardMoney = 100_000L,
        unlockRewardGems = 20
    ),

    // Stage 5 (Seviye 12 VEYA 15.000.000 ₳ Net Servet): Konsorsiyum, IPO, Müze & Mega Projeler -> 500.000 ₳ + 50 💎 Elmas
    CONSORTIUM(
        id = "feature_consortium",
        titleTr = "Mega Konsorsiyum & İhracat",
        titleEn = "Mega Consortium & Export",
        stageNumber = 5,
        defaultMinLevel = 12,
        minRequiredNetWorth = 15_000_000L,
        unlockRewardMoney = 500_000L,
        unlockRewardGems = 50,
        requiredTechId = "tech_heavy_industry",
        requiredTechNameTr = "Ağır Sanayi & İhracat",
        requiredTechNameEn = "Heavy Industry & Export"
    ),
    MUSEUM(
        id = "feature_museum",
        titleTr = "Tarihi Eser Müzesi",
        titleEn = "Antique Heritage Museum",
        stageNumber = 5,
        defaultMinLevel = 12,
        minRequiredNetWorth = 15_000_000L,
        unlockRewardMoney = 500_000L,
        unlockRewardGems = 50,
        requiredTechId = "tech_quality_control",
        requiredTechNameTr = "Kalite & Kültürel Miras",
        requiredTechNameEn = "Quality & Cultural Heritage"
    )
}

data class FeatureLockInfo(
    val feature: GameFeature,
    val isUnlocked: Boolean,
    val stageNumber: Int,
    val minRequiredLevel: Int,
    val minRequiredNetWorth: Long,
    val unlockRewardMoney: Long,
    val unlockRewardGems: Int,
    val requiredTechId: String?,
    val requiredTechNameTr: String?,
    val requiredTechNameEn: String?,
    val currentLevel: Int,
    val currentNetWorth: Long,
    val isTechCompleted: Boolean,
    val lockReasonTr: String,
    val lockReasonEn: String,
    val progressPercent: Float,
    val levelProgressPercent: Float,
    val netWorthProgressPercent: Float
)

object FeatureLockManager {

    /**
     * Verilen özelliğin oyuncu için açık olup olmadığını denetler.
     * Kural (Faz 4 Çift Koşullu Kilit Motoru):
     * - Oyuncu minimum seviyeye ulaşmışsa VEYA
     * - Minimum Net Servet (Net Worth) eşiğini geçmişse VEYA
     * - Gerekli Ar-Ge teknolojisi tamamlanmışsa kilit açılır.
     * - Aşama 1 özellikleri (Tesis, Pazar, Depo) her zaman açıktır.
     */
    fun isUnlocked(
        feature: GameFeature,
        playerLevel: Int,
        completedTechMap: Map<String, Int> = emptyMap(),
        netWorth: Long = 0L
    ): Boolean {
        if (playerLevel <= 0) return feature.defaultMinLevel <= 1
        
        // Aşama 1 özellikleri doğrudan açıktır
        if (feature.defaultMinLevel <= 1 || feature.stageNumber <= 1) return true

        // 1. Şart: Oyuncu belirtilen şirket seviyesine ulaştı mı?
        val isLevelSatisfied = playerLevel >= feature.defaultMinLevel

        // 2. Şart: Oyuncu belirtilen Net Servet (Net Worth) miktarına ulaştı mı?
        val isNetWorthSatisfied = feature.minRequiredNetWorth > 0L && netWorth >= feature.minRequiredNetWorth

        // 3. Şart: Gerekli Ar-Ge teknolojisi araştırılmış mı?
        val techId = feature.requiredTechId
        val isTechSatisfied = if (techId != null) {
            val baseId = techId.removePrefix("tech_")
            (completedTechMap[techId] ?: completedTechMap[baseId] ?: completedTechMap["tech_$baseId"] ?: 0) >= 1
        } else {
            false
        }

        // Çift Koşullu kilit açılış: Seviye VEYA Net Servet VEYA Ar-Ge Teknolojisi
        return isLevelSatisfied || isNetWorthSatisfied || isTechSatisfied
    }

    /**
     * Özellik kilit detaylarını, hibe ödül verilerini ve ilerleme yüzdesini döner.
     */
    fun getLockInfo(
        feature: GameFeature,
        playerLevel: Int,
        completedTechMap: Map<String, Int> = emptyMap(),
        netWorth: Long = 0L
    ): FeatureLockInfo {
        val safeLevel = playerLevel.coerceAtLeast(1)
        val safeNetWorth = netWorth.coerceAtLeast(0L)
        val techId = feature.requiredTechId
        val baseId = techId?.removePrefix("tech_")
        val isTechCompleted = if (techId != null) {
            (completedTechMap[techId] ?: completedTechMap[baseId] ?: completedTechMap["tech_$baseId"] ?: 0) >= 1
        } else {
            false
        }

        val unlocked = isUnlocked(feature, safeLevel, completedTechMap, safeNetWorth)

        val levelProgress = (safeLevel.toFloat() / feature.defaultMinLevel.toFloat()).coerceIn(0f, 1f)
        val netWorthProgress = if (feature.minRequiredNetWorth > 0L) {
            (safeNetWorth.toFloat() / feature.minRequiredNetWorth.toFloat()).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val maxProgress = maxOf(levelProgress, netWorthProgress)
        val overallProgress = if (unlocked) 1.0f else maxProgress.coerceIn(0f, 0.99f)

        val formattedNetWorthStr = if (feature.minRequiredNetWorth >= 1_000_000L) {
            "${feature.minRequiredNetWorth / 1_000_000L} Milyon ₳"
        } else {
            "${feature.minRequiredNetWorth / 1_000L} Bin ₳"
        }

        val lockReasonTr = when {
            unlocked -> "Kullanıma Açık"
            feature.requiredTechNameTr != null -> "Aşama ${feature.stageNumber}: Şirket Seviyesi ${feature.defaultMinLevel} VEYA $formattedNetWorthStr Net Servet VEYA '${feature.requiredTechNameTr}' Ar-Ge araştırması gereklidir."
            else -> "Aşama ${feature.stageNumber}: Şirket Seviyesi ${feature.defaultMinLevel} VEYA $formattedNetWorthStr Net Servet gereklidir."
        }

        val lockReasonEn = when {
            unlocked -> "Unlocked"
            feature.requiredTechNameEn != null -> "Stage ${feature.stageNumber}: Requires Company Level ${feature.defaultMinLevel} OR ₳${feature.minRequiredNetWorth} Net Worth OR '${feature.requiredTechNameEn}' R&D."
            else -> "Stage ${feature.stageNumber}: Requires Company Level ${feature.defaultMinLevel} OR ₳${feature.minRequiredNetWorth} Net Worth."
        }

        return FeatureLockInfo(
            feature = feature,
            isUnlocked = unlocked,
            stageNumber = feature.stageNumber,
            minRequiredLevel = feature.defaultMinLevel,
            minRequiredNetWorth = feature.minRequiredNetWorth,
            unlockRewardMoney = feature.unlockRewardMoney,
            unlockRewardGems = feature.unlockRewardGems,
            requiredTechId = feature.requiredTechId,
            requiredTechNameTr = feature.requiredTechNameTr,
            requiredTechNameEn = feature.requiredTechNameEn,
            currentLevel = safeLevel,
            currentNetWorth = safeNetWorth,
            isTechCompleted = isTechCompleted,
            lockReasonTr = lockReasonTr,
            lockReasonEn = lockReasonEn,
            progressPercent = overallProgress,
            levelProgressPercent = levelProgress,
            netWorthProgressPercent = netWorthProgress
        )
    }

    /**
     * Tüm özelliklerin kilit durum listesini döndürür.
     */
    fun getAllFeaturesLockInfo(
        playerLevel: Int,
        completedTechMap: Map<String, Int> = emptyMap(),
        netWorth: Long = 0L
    ): List<FeatureLockInfo> {
        return GameFeature.values().map { getLockInfo(it, playerLevel, completedTechMap, netWorth) }
    }
}
