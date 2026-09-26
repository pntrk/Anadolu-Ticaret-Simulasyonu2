package com.example.data

import com.example.R

/**
 * Oyun içi temel modüllerin ve özelliklerin kademeli kilit durumlarını yöneten merkezi yapı.
 * Yeni başlayan oyuncuların bilişsel aşırı yüklenme yaşamasını engeller;
 * Oyuncu seviyesi ve Ar-Ge teknolojisi tamamlanma durumuna göre modülleri aşamalı olarak açar.
 */
enum class GameFeature(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val defaultMinLevel: Int,
    val requiredTechId: String? = null,
    val requiredTechNameTr: String? = null,
    val requiredTechNameEn: String? = null,
    val iconRes: Int? = null
) {
    FACILITIES(
        id = "feature_facilities",
        titleTr = "Tesisler & Üretim",
        titleEn = "Facilities & Production",
        defaultMinLevel = 1,
        requiredTechId = null,
        requiredTechNameTr = null,
        requiredTechNameEn = null
    ),
    BORSA(
        id = "feature_borsa",
        titleTr = "Borsa & Ticaret",
        titleEn = "Stock Market & Trading",
        defaultMinLevel = 1,
        requiredTechId = null,
        requiredTechNameTr = null,
        requiredTechNameEn = null
    ),
    WAREHOUSE(
        id = "feature_warehouse",
        titleTr = "Depo & Envanter",
        titleEn = "Warehouse & Inventory",
        defaultMinLevel = 1,
        requiredTechId = null,
        requiredTechNameTr = null,
        requiredTechNameEn = null
    ),
    BANKING(
        id = "feature_banking",
        titleTr = "Banka & Mevduat",
        titleEn = "Bank & Deposits",
        defaultMinLevel = 2,
        requiredTechId = null,
        requiredTechNameTr = null,
        requiredTechNameEn = null
    ),
    RD_LAB(
        id = "feature_rd_lab",
        titleTr = "Ar-Ge Laboratuvarı",
        titleEn = "R&D Laboratory",
        defaultMinLevel = 3,
        requiredTechId = null,
        requiredTechNameTr = null,
        requiredTechNameEn = null
    ),
    MARKET_P2P(
        id = "feature_market_p2p",
        titleTr = "İller Arası Pazar",
        titleEn = "Intercity Market",
        defaultMinLevel = 4,
        requiredTechId = "tech_logistics",
        requiredTechNameTr = "Lojistik & Ticaret Ağı",
        requiredTechNameEn = "Logistics & Trade Network"
    ),
    HR_MANAGERS(
        id = "feature_hr_managers",
        titleTr = "İnsan Kaynakları & Otomasyon",
        titleEn = "HR & Automation Managers",
        defaultMinLevel = 5,
        requiredTechId = "tech_automation",
        requiredTechNameTr = "Endüstriyel Otomasyon",
        requiredTechNameEn = "Industrial Automation"
    ),
    CONSORTIUM(
        id = "feature_consortium",
        titleTr = "Mega Konsorsiyum",
        titleEn = "Mega Consortium",
        defaultMinLevel = 6,
        requiredTechId = "tech_heavy_industry",
        requiredTechNameTr = "Ağır Sanayi & İhracat",
        requiredTechNameEn = "Heavy Industry & Export"
    ),
    MUSEUM(
        id = "feature_museum",
        titleTr = "Tarihi Eser Müzesi",
        titleEn = "Antique Heritage Museum",
        defaultMinLevel = 7,
        requiredTechId = "tech_quality_control",
        requiredTechNameTr = "Kalite & Kültürel Miras",
        requiredTechNameEn = "Quality & Cultural Heritage"
    )
}

data class FeatureLockInfo(
    val feature: GameFeature,
    val isUnlocked: Boolean,
    val minRequiredLevel: Int,
    val requiredTechId: String?,
    val requiredTechNameTr: String?,
    val requiredTechNameEn: String?,
    val currentLevel: Int,
    val isTechCompleted: Boolean,
    val lockReasonTr: String,
    val lockReasonEn: String,
    val progressPercent: Float
)

object FeatureLockManager {

    /**
     * Verilen özelliğin oyuncu için açık olup olmadığını denetler.
     * Kural:
     * - Oyuncu minimum seviyeye ulaşmışsa VEYA gerekli Ar-Ge araştırması tamamlanmışsa özellik açılır.
     * - İlk seviyelerdeki özellikler (Tesis, Borsa, Depo) her zaman açıktır.
     */
    fun isUnlocked(
        feature: GameFeature,
        playerLevel: Int,
        completedTechMap: Map<String, Int>
    ): Boolean {
        if (playerLevel <= 0) return feature.defaultMinLevel <= 1
        
        // Seviye 1 özellikleri doğrudan açıktır
        if (feature.defaultMinLevel <= 1) return true

        // 1. Şart: Oyuncu belirtilen şirket seviyesine ulaştıysa açılır
        val isLevelSatisfied = playerLevel >= feature.defaultMinLevel

        // 2. Şart: Gerekli Ar-Ge teknolojisi araştırılmış mı?
        val techId = feature.requiredTechId
        val isTechSatisfied = if (techId != null) {
            val baseId = techId.removePrefix("tech_")
            (completedTechMap[techId] ?: completedTechMap[baseId] ?: completedTechMap["tech_$baseId"] ?: 0) >= 1
        } else {
            false
        }

        // Oyuncu seviyeyi sağladıysa VEYA Ar-Ge'den araştırmayı erkenden tamamladıysa kilit açılır
        return isLevelSatisfied || isTechSatisfied
    }

    /**
     * Özellik kilit detaylarını ve oyuncunun açması için yapması gerekenleri içeren bilgi nesnesi döndürür.
     */
    fun getLockInfo(
        feature: GameFeature,
        playerLevel: Int,
        completedTechMap: Map<String, Int>
    ): FeatureLockInfo {
        val safeLevel = playerLevel.coerceAtLeast(1)
        val techId = feature.requiredTechId
        val baseId = techId?.removePrefix("tech_")
        val isTechCompleted = if (techId != null) {
            (completedTechMap[techId] ?: completedTechMap[baseId] ?: completedTechMap["tech_$baseId"] ?: 0) >= 1
        } else {
            false
        }

        val unlocked = isUnlocked(feature, safeLevel, completedTechMap)

        val progressPercent = if (unlocked) {
            1.0f
        } else {
            (safeLevel.toFloat() / feature.defaultMinLevel.toFloat()).coerceIn(0f, 0.95f)
        }

        val lockReasonTr = when {
            unlocked -> "Kullanıma Açık"
            feature.requiredTechNameTr != null -> "Şirket Seviyesi ${feature.defaultMinLevel} veya Ar-Ge '${feature.requiredTechNameTr}' araştırması gereklidir."
            else -> "Şirket Seviyesi ${feature.defaultMinLevel} gereklidir."
        }

        val lockReasonEn = when {
            unlocked -> "Unlocked"
            feature.requiredTechNameEn != null -> "Requires Company Level ${feature.defaultMinLevel} or R&D '${feature.requiredTechNameEn}' research."
            else -> "Requires Company Level ${feature.defaultMinLevel}."
        }

        return FeatureLockInfo(
            feature = feature,
            isUnlocked = unlocked,
            minRequiredLevel = feature.defaultMinLevel,
            requiredTechId = feature.requiredTechId,
            requiredTechNameTr = feature.requiredTechNameTr,
            requiredTechNameEn = feature.requiredTechNameEn,
            currentLevel = safeLevel,
            isTechCompleted = isTechCompleted,
            lockReasonTr = lockReasonTr,
            lockReasonEn = lockReasonEn,
            progressPercent = progressPercent
        )
    }

    /**
     * Tüm özelliklerin kilit durum listesini döndürür.
     */
    fun getAllFeaturesLockInfo(
        playerLevel: Int,
        completedTechMap: Map<String, Int>
    ): List<FeatureLockInfo> {
        return GameFeature.values().map { getLockInfo(it, playerLevel, completedTechMap) }
    }
}
