package com.example.data

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Zanaatkarlık & Kalite Mirası Sistemi (Synergy Crafting & Quality Heritage Engine)
 * Tesis içi reçetelerde kullanılan hammadde/ara malların kalite seviyeleri,
 * nihai ürünün 1-5 yıldız çıkma şansını (Critical Craft) doğrudan belirler.
 */
object QualityCraftingService {

    /**
     * Tesis seviyesi (1-10) ve Oyuncu seviyesine göre temel kalite olasılıklarını hesaplar.
     */
    fun getQualityProbabilities(
        facilityLevel: Int,
        playerLevel: Int = 1
    ): Map<ProductQuality, Double> {
        val fLevel = facilityLevel.coerceIn(1, 10)
        val pLevel = playerLevel.coerceIn(1, 50)

        // İlerleme katsayısı (0.0: başlangıç, 1.0: maksimum seviye)
        val progress = ((fLevel - 1) / 9.0 * 0.70) + ((pLevel - 1) / 49.0 * 0.30)

        // Seviye 1 Temel Olasılıklar
        val base1Star = 0.70
        val base2Star = 0.20
        val base3Star = 0.08
        val base4Star = 0.02
        val base5Star = 0.00

        // Maksimum Gelişmiş Tesis Olasılıkları
        val max1Star = 0.10
        val max2Star = 0.25
        val max3Star = 0.35
        val max4Star = 0.20
        val max5Star = 0.10

        val p1 = lerp(base1Star, max1Star, progress)
        val p2 = lerp(base2Star, max2Star, progress)
        val p3 = lerp(base3Star, max3Star, progress)
        val p4 = lerp(base4Star, max4Star, progress)
        val p5 = lerp(base5Star, max5Star, progress)

        return mapOf(
            ProductQuality.STANDARD to p1,
            ProductQuality.SELECTED to p2,
            ProductQuality.SPECIAL to p3,
            ProductQuality.MASTERWORK to p4,
            ProductQuality.PALACE_GRAND to p5
        )
    }

    /**
     * 🎯 Ürün Kalite Formülü (Üretim Tamamlandığında Nihai Ürün Kalitesini Belirler):
     * - Reçeteli Üretim (Hammadde Kullanılan):
     *   Formül: (Kullanılan Hammaddelerin Ortalama Kalitesi [1-5] * 0.45) + (Tesis Seviyesi [1-5] * 0.40) + (Rastgele Kritik Zanaat Şansı [0 veya 1] * 0.15)
     * - Hammaddesiz Doğrudan Üretim (Hasat / Çıkarma / Madencilik):
     *   Formül: (Tesis Seviyesi [1-5] * 0.80) + (Rastgele Kritik Zanaat Şansı [0 veya 1] * 0.20)
     */
    /**
     * Tesis Seviyesine Göre Ürün Kalitesi:
     * 1-2 seviye tesis 1 yıldızlı
     * 3-4 seviye tesis 2 yıldızlı
     * 5-6 seviye tesis 3 yıldızlı
     * 7-8 seviye tesis 4 yıldızlı
     * 9-10 seviye tesis 5 yıldızlı kalitede ürün üretebilir.
     */
    fun getQualityByFacilityLevel(facilityLevel: Int): ItemQuality = ItemQuality.fromFacilityLevel(facilityLevel)

    /**
     * Tesis aşınma seviyesine göre kalite yıldız düşüşü (ceza puanı):
     * - Seviye 0 - 20 arası: 0 yıldız düşüşü (kaliteyi etkilemez)
     * - Seviye 20 - 40 arası: 1 yıldız daha düşük kalitede ürün (-1★)
     * - Seviye 40 - 60 arası: 2 yıldız daha düşük kalitede ürün (-2★)
     * - Seviye 60 - 80 arası: 3 yıldız daha düşük kalitede ürün (-3★)
     * - Seviye 80 - 100 arası: 4 yıldız daha düşük kalitede ürün (-4★)
     */
    fun getWearQualityPenaltyStars(wearLevel: Float): Int {
        val wearPercent = if (wearLevel <= 1.0f) {
            kotlin.math.round(wearLevel * 100f).toInt()
        } else {
            kotlin.math.round(wearLevel).toInt()
        }.coerceIn(0, 100)

        return when {
            wearPercent <= 20 -> 0
            wearPercent <= 40 -> 1
            wearPercent <= 60 -> 2
            wearPercent <= 80 -> 3
            else -> 4
        }
    }

    /**
     * Tesis aşınma seviyesi 80-100 arası ise üretim süresi uzar (+%60 süre uzaması)
     */
    fun getWearDurationMultiplier(wearLevel: Float): Float {
        val wearPercent = if (wearLevel <= 1.0f) {
            kotlin.math.round(wearLevel * 100f).toInt()
        } else {
            kotlin.math.round(wearLevel).toInt()
        }.coerceIn(0, 100)
        return if (wearPercent in 80..100) 1.60f else 1.0f
    }

    /**
     * Tesis ve hammadde kalitesine göre beklenen ürün kalitesini hesaplar.
     * Formül: (Tesis Kalitesi [1-5★] + Hammadde Kalitesi [1-5★]) / 2.0 - Aşınma Cezası
     */
    fun calculateExpectedQuality(
        facilityLevel: Int,
        ingredientStars: Double = 1.0,
        wearLevel: Float = 0.0f
    ): ItemQuality {
        val fStars = getQualityByFacilityLevel(facilityLevel).stars.toDouble()
        val avg = (fStars + ingredientStars) / 2.0
        val penalty = getWearQualityPenaltyStars(wearLevel)
        return ItemQuality.fromStars((avg.roundToInt() - penalty).coerceIn(1, 5))
    }

    fun getQualityProductionPreview(
        product: Product,
        facilityLevel: Int,
        ingredientStars: Double = 1.0,
        wearLevel: Float = 0.0f
    ): QualityProductionPreviewData {
        val fQual = getQualityByFacilityLevel(facilityLevel)
        val penalty = getWearQualityPenaltyStars(wearLevel)
        val wearPercent = if (wearLevel <= 1.0f) {
            kotlin.math.round(wearLevel * 100f).toInt()
        } else {
            kotlin.math.round(wearLevel).toInt()
        }.coerceIn(0, 100)

        return if (product.recipe.isEmpty()) {
            val resStars = (fQual.stars - penalty).coerceIn(1, 5)
            val resQual = ItemQuality.fromStars(resStars)
            val explanation = if (penalty > 0) {
                "Hammadde gerektirmez. Tesis seviyesi ${fQual.stars}★ üretir; ancak tesis aşınması (%$wearPercent) sebebiyle -$penalty★ düşerek ${resQual.stars}★ kalitede üretilecektir."
            } else {
                "Hammadde gerektirmez. Tesis seviyesi doğrudan ${fQual.stars}★ kalite üretir."
            }
            QualityProductionPreviewData(
                facilityQuality = fQual,
                ingredientStars = 0.0,
                resultQuality = resQual,
                hasRecipe = false,
                explanation = explanation
            )
        } else {
            val resQual = calculateExpectedQuality(facilityLevel, ingredientStars, wearLevel)
            val avg = (fQual.stars.toDouble() + ingredientStars) / 2.0
            val explanation = if (penalty > 0) {
                "Tesis (${fQual.stars}★) + Hammadde (${ingredientStars.toInt()}★) ➔ ${String.format(java.util.Locale.US, "%.1f", avg)}★ | Aşınma (%$wearPercent) -$penalty★ ➔ ${resQual.stars}★"
            } else {
                "Tesis (${fQual.stars}★) + Hammadde (${ingredientStars.toInt()}★) ➔ Ortalama: ${String.format(java.util.Locale.US, "%.1f", avg)}★ = ${resQual.stars}★"
            }
            QualityProductionPreviewData(
                facilityQuality = fQual,
                ingredientStars = ingredientStars,
                resultQuality = resQual,
                hasRecipe = true,
                explanation = explanation
            )
        }
    }

    fun calculateProducedItemQuality(
        product: Product,
        facilityLevel: Int,
        usedIngredientQualities: List<ItemQuality> = emptyList(),
        isCriticalCraft: Boolean? = null,
        wearLevel: Float = 0.0f
    ): ItemQuality {
        val facilityBaseQuality = getQualityByFacilityLevel(facilityLevel)
        val facilityStars = facilityBaseQuality.stars.toDouble()
        val wearPenalty = getWearQualityPenaltyStars(wearLevel)

        // 1. Reçetesiz Doğrudan Üretim (Hasat, Çiftlik, Madencilik, Petrol Kuyusu):
        // Hammadde kullanılmadığı için tesis seviyesi doğrudan ürün kalitesini belirler.
        if (product.recipe.isEmpty()) {
            val critChance = 0.08 + (facilityLevel * 0.01)
            val isCrit = isCriticalCraft ?: (Random.nextDouble() < critChance)
            val baseStars = if (isCrit && facilityStars < 5.0) {
                facilityStars + 1.0
            } else {
                facilityStars
            }
            val finalStars = (baseStars.roundToInt() - wearPenalty).coerceIn(1, 5)
            return ItemQuality.fromStars(finalStars)
        }

        // 2. Reçeteli Üretim (Hammadde Kullanılan Sanayi & İmalat):
        // Tesis seviyesi ile hammadde kalitesinin aritmetik ortalaması nihai kaliteyi belirler.
        // Pazar veya borsadan satın alınan standart hammadde 1 yıldızlı (STAR_1) kabul edilir.
        val avgIngredientStars = if (usedIngredientQualities.isNotEmpty()) {
            usedIngredientQualities.map { it.stars.toDouble() }.average()
        } else {
            1.0 // Pazar veya borsadan standart alım
        }

        // Tesis ve hammadde ortalaması: (Tesis Yıldızı + Hammadde Yıldızı) / 2
        val averageScore = (facilityStars + avgIngredientStars) / 2.0

        // Kritik Zanaat Şansı: Usta işçilik nadiren +1 yıldız bonus sağlayabilir
        val critChance = 0.08 + (facilityLevel * 0.01)
        val isCrit = isCriticalCraft ?: (Random.nextDouble() < critChance)
        val baseStars = if (isCrit && averageScore < 5.0 && Random.nextDouble() < 0.20) {
            averageScore + 1.0
        } else {
            averageScore
        }

        // Tesis aşınması seviyesi kaliteden yıldız düşürür:
        val finalStars = (baseStars.roundToInt() - wearPenalty).coerceIn(1, 5)
        return ItemQuality.fromStars(finalStars)
    }

data class QualityProductionPreviewData(
    val facilityQuality: ItemQuality,
    val ingredientStars: Double,
    val resultQuality: ItemQuality,
    val hasRecipe: Boolean,
    val explanation: String
)

    /**
     * 🏭 Kalite Mirası (Quality Heritage & Synergy Crafting):
     * Reçetede kullanılan ara malların / hammaddelerin üretildiği kaynak tesislerin
     * seviyeleri (1-10) hesaplanır. Yüksek kaliteli hammadde girdileri (örn. Level 8+ Demir & Silikon)
     * 4★ (Usta İşi) ve 5★ (Saray Kalitesi) çıkma şansını +%40'a kadar artırır!
     */
    fun calculateSynergyQualityProbabilities(
        facilityLevel: Int,
        playerLevel: Int = 1,
        upstreamFacilityLevels: List<Int> = emptyList(),
        luckyBoost: Boolean = false
    ): Map<ProductQuality, Double> {
        val baseProbs = getQualityProbabilities(facilityLevel, playerLevel)
        if (upstreamFacilityLevels.isEmpty()) {
            return if (luckyBoost) applyLuckyBoost(baseProbs) else baseProbs
        }

        // Girdi tesislerinin ortalama seviyesi (1.0 - 10.0)
        val avgUpstreamLevel = upstreamFacilityLevels.average().coerceIn(1.0, 10.0)
        
        // Sinerji çarpanı: Level 1 girdi -> %0 bonus, Level 10 girdi -> +%40 bonus
        val synergyFactor = ((avgUpstreamLevel - 1.0) / 9.0).coerceIn(0.0, 1.0)
        val criticalShift = synergyFactor * 0.40 // %40'a kadar olasılık kaydırma

        var p1 = baseProbs[ProductQuality.STANDARD] ?: 0.50
        var p2 = baseProbs[ProductQuality.SELECTED] ?: 0.25
        var p3 = baseProbs[ProductQuality.SPECIAL] ?: 0.15
        var p4 = baseProbs[ProductQuality.MASTERWORK] ?: 0.08
        var p5 = baseProbs[ProductQuality.PALACE_GRAND] ?: 0.02

        // Standart ve seçme mahsulden olasılık çekilerek Usta İşi ve Saray Kalitesine aktarılır
        val shiftFrom1 = p1 * criticalShift * 0.70
        val shiftFrom2 = p2 * criticalShift * 0.30
        val totalShift = shiftFrom1 + shiftFrom2

        p1 = (p1 - shiftFrom1).coerceAtLeast(0.02)
        p2 = (p2 - shiftFrom2).coerceAtLeast(0.05)
        p3 = p3 + (totalShift * 0.20)
        p4 = p4 + (totalShift * 0.50) // En büyük pay 4 Yıldızlı İhracat kalitesine
        p5 = p5 + (totalShift * 0.30) // Saray / Gurme kalitesine aktarılır

        // Normalizasyon (toplam 1.0)
        val sum = p1 + p2 + p3 + p4 + p5
        val normalized = mapOf(
            ProductQuality.STANDARD to p1 / sum,
            ProductQuality.SELECTED to p2 / sum,
            ProductQuality.SPECIAL to p3 / sum,
            ProductQuality.MASTERWORK to p4 / sum,
            ProductQuality.PALACE_GRAND to p5 / sum
        )

        return if (luckyBoost) applyLuckyBoost(normalized) else normalized
    }

    /**
     * Sinerji bonusu yüzdesini hesaplar (Arayüzde gösterim için: örn. +%35 Sinerji)
     */
    fun calculateSynergyBonusPercent(upstreamFacilityLevels: List<Int>): Int {
        if (upstreamFacilityLevels.isEmpty()) return 0
        val avg = upstreamFacilityLevels.average().coerceIn(1.0, 10.0)
        return (((avg - 1.0) / 9.0) * 40.0).roundToInt().coerceIn(0, 40)
    }

    /**
     * Düşük kaliteli hammadde girdilerinde kusur/hata riskini hesaplar (%0 - %25).
     * Girdi tesisleri Seviye 1-2 ise veya pazardan düşük kalite alınıyorsa kusur riski doğar.
     */
    fun calculateDefectRiskPercent(upstreamFacilityLevels: List<Int>): Int {
        if (upstreamFacilityLevels.isEmpty()) return 5
        val avg = upstreamFacilityLevels.average().coerceIn(1.0, 10.0)
        return when {
            avg >= 5.0 -> 0 // Seviye 5+ tesislerde kusursuz üretim garantisi
            avg >= 3.0 -> 4
            avg >= 2.0 -> 10
            else -> 20 // Seviye 1 girdilerde %20'ye varan kusurlu çıkma riski
        }
    }

    /**
     * Tesis İçi Reçeteler için Kalite Mirası ve Sinerji Analizi üretir.
     */
    fun analyzeSynergyHeritage(
        product: Product,
        businesses: List<BusinessEntity>,
        playerLevel: Int = 1
    ): SynergyHeritageAnalysis {
        val primaryBusiness = businesses.find { it.type == product.facilityId || it.type == product.id }
        val primaryLevel = primaryBusiness?.level ?: 1

        val upstreamItems = product.recipe.map { req ->
            val reqProd = Product.values().find { it.id == req.productId }
            val reqFacilityId = reqProd?.facilityId ?: req.productId
            val ownedBiz = businesses.find { it.type == reqFacilityId || it.type == req.productId }
            val fLevel = ownedBiz?.level
            val quality = if (fLevel != null) {
                evaluateConsortiumSlotCraftsmanship(fLevel, reqProd?.tier ?: ProductTier.TIER_1)
            } else {
                ProductQuality.STANDARD // Pazardan tedarik edilen standart kalite
            }

            UpstreamHeritageItem(
                productId = req.productId,
                productName = reqProd?.getDisplayName() ?: req.productId,
                facilityName = reqProd?.getFacilityName() ?: "Tedarik Tesisi",
                ownedFacilityLevel = fLevel,
                qualityTier = quality,
                qualityStars = quality.starsText,
                isOptimal = (fLevel ?: 1) >= 7
            )
        }

        val upstreamLevels = upstreamItems.map { it.ownedFacilityLevel ?: 1 }
        val synergyBonus = calculateSynergyBonusPercent(upstreamLevels)
        val defectRisk = calculateDefectRiskPercent(upstreamLevels)
        val probs = calculateSynergyQualityProbabilities(
            facilityLevel = primaryLevel,
            playerLevel = playerLevel,
            upstreamFacilityLevels = upstreamLevels
        )

        val avgLvl = if (upstreamLevels.isNotEmpty()) upstreamLevels.average() else 1.0
        val (heritageTitle, summaryTr, summaryEn) = when {
            avgLvl >= 8.0 -> Triple(
                "🏆 5★ Saray & Usta Mirası (+%$synergyBonus)",
                "Tüm hammadde tesisleriniz en üst seviyede! Üretimin 4★ Usta İşi ve 5★ Saray Kalitesi çıkma şansı +%$synergyBonus arttı, kusur riski sıfırlandı.",
                "All input facilities are at master level! 4★ and 5★ craft chance boosted by +%$synergyBonus with zero defect risk."
            )
            avgLvl >= 5.0 -> Triple(
                "⭐ Sinerjik Kalite Mirası (+%$synergyBonus)",
                "Gelişmiş hammadde tesisleri sayesinde nihai ürün kalitesi +%$synergyBonus sinerji primi kazandı.",
                "Advanced upstream facilities provide +%$synergyBonus synergy boost to final product quality."
            )
            avgLvl >= 2.5 -> Triple(
                "📦 Standart Miras (+%$synergyBonus)",
                "Girdi tesislerinizi geliştirerek nihai ürünün 4★ ve 5★ çıkma şansını +%40'a kadar artırabilirsiniz.",
                "Upgrade upstream mining and processing facilities to boost high-tier craft chances up to +40%."
            )
            else -> Triple(
                "⚠️ Düşük Girdi Uyarısı (%$defectRisk Risk)",
                "Düşük seviyeli hammadde tesisleri ürün kalitesini sınırlandırıyor ve %$defectRisk kusur riski oluşturuyor. Başlangıç tesislerini geliştirin!",
                "Low-tier inputs limit final product value and create %$defectRisk defect risk. Upgrade upstream facilities!"
            )
        }

        return SynergyHeritageAnalysis(
            product = product,
            primaryFacilityLevel = primaryLevel,
            upstreamItems = upstreamItems,
            synergyBonusPercent = synergyBonus,
            defectRiskPercent = defectRisk,
            probabilities = probs,
            heritageRating = heritageTitle,
            summaryTextTr = summaryTr,
            summaryTextEn = summaryEn
        )
    }

    /**
     * Konsorsiyum Mega Projeleri için Tedarik ve Kalite Mirası Analizi üretir.
     */
    fun analyzeConsortiumSynergy(project: MegaProject): ConsortiumSynergyAnalysis {
        val slots = project.slots
        if (slots.isEmpty()) {
            return ConsortiumSynergyAnalysis(
                overallCraftsmanshipScore = 1.0,
                totalSynergyBonusPercent = 0,
                dividendMultiplier = 1.0f,
                prestigeMultiplier = 1.0f,
                defectRateReductionPercent = 0,
                tierRating = "📦 C Standart Tedarik",
                summaryTr = "Konsorsiyum henüz standart tedarik döngüsünde.",
                summaryEn = "Consortium is currently on standard supply cycle."
            )
        }

        val avgScore = project.averageCraftsmanshipScore.coerceIn(1.0, 5.0)
        val synergyBonusPct = (((avgScore - 1.0) / 4.0) * 40.0).roundToInt().coerceIn(0, 40)
        val dividendMult = 1.0f + (synergyBonusPct / 100.0f)
        val prestigeMult = 1.0f + ((avgScore - 1.0).toFloat() * 0.125f)
        val defectReduction = (avgScore * 20.0).roundToInt().coerceIn(20, 100)

        val (rating, summaryTr, summaryEn) = when {
            avgScore >= 4.5 -> Triple(
                "🏆 5★ Saray Kalitesi & Zirve Sinerji (+%$synergyBonusPct Kar Payı)",
                "Tüm ortaklar 4★ ve 5★ usta işi malzeme tedarik etti! Konsorsiyum kâr payı çarpanı +%$synergyBonusPct artırıldı ve borsa değeri zirveye taşındı.",
                "All partners contributed masterwork inputs! Dividend payout boosted by +%$synergyBonusPct with maximum stock valuation."
            )
            avgScore >= 3.5 -> Triple(
                "⭐ 4★ İhracat Kalite Mirası (+%$synergyBonusPct Kar Payı)",
                "Yüksek kaliteli tedarik zinciri konsorsiyum ürünlerine +%$synergyBonusPct kâr payı primi kazandırdı.",
                "High quality supply chain unlocked +%$synergyBonusPct dividend bonus."
            )
            avgScore >= 2.5 -> Triple(
                "⚡ 3★ Özel Üretim Mirası (+%$synergyBonusPct Kar Payı)",
                "Dengeli tedarik zinciri. Ortaklar tesislerini geliştirerek kâr payını +%40'a kadar yükseltebilir.",
                "Balanced supply chain. Partners can upgrade facilities to reach up to +40% dividend boost."
            )
            else -> Triple(
                "📦 1-2★ Standart Tedarik Mirası",
                "Temel seviyede tedarik. Yüksek seviyeli tesislerden teslimat yaparak konsorsiyum kârını ve hisse değerini katlayabilirsiniz.",
                "Standard supply. Deliver from higher level facilities to multiply consortium dividends and share price."
            )
        }

        return ConsortiumSynergyAnalysis(
            overallCraftsmanshipScore = avgScore,
            totalSynergyBonusPercent = synergyBonusPct,
            dividendMultiplier = dividendMult,
            prestigeMultiplier = prestigeMult,
            defectRateReductionPercent = defectReduction,
            tierRating = rating,
            summaryTr = summaryTr,
            summaryEn = summaryEn
        )
    }

    /**
     * Tek bir üretim için Kalite Mirası zarı atar.
     */
    fun rollSynergyQuality(
        facilityLevel: Int,
        playerLevel: Int = 1,
        upstreamFacilityLevels: List<Int> = emptyList(),
        luckyBoost: Boolean = false
    ): ProductQuality {
        val probs = calculateSynergyQualityProbabilities(
            facilityLevel = facilityLevel,
            playerLevel = playerLevel,
            upstreamFacilityLevels = upstreamFacilityLevels,
            luckyBoost = luckyBoost
        )

        val roll = Random.nextDouble()
        var cumulative = 0.0
        for ((quality, prob) in probs) {
            cumulative += prob
            if (roll <= cumulative) {
                return quality
            }
        }
        return ProductQuality.STANDARD
    }

    /**
     * Konsorsiyum tedarik slotuna malzeme teslim edildiğinde zanaat kalitesini hesaplar.
     */
    fun evaluateConsortiumSlotCraftsmanship(
        supplierFacilityLevel: Int,
        productTier: ProductTier
    ): ProductQuality {
        val fLevel = supplierFacilityLevel.coerceIn(1, 10)
        return when {
            fLevel >= 9 -> ProductQuality.PALACE_GRAND // 5★ Saray / Coğrafi İşaretli
            fLevel >= 7 -> ProductQuality.MASTERWORK   // 4★ Usta İşi / İhracat
            fLevel >= 5 -> ProductQuality.SPECIAL      // 3★ Özel Üretim
            fLevel >= 3 -> ProductQuality.SELECTED     // 2★ Seçme Mahsul
            else -> ProductQuality.STANDARD            // 1★ Standart
        }
    }

    /**
     * Tek bir üretim için temel kalite zarı atar.
     */
    fun rollQuality(
        facilityLevel: Int,
        playerLevel: Int = 1,
        luckyBoost: Boolean = false
    ): ProductQuality {
        return rollSynergyQuality(
            facilityLevel = facilityLevel,
            playerLevel = playerLevel,
            upstreamFacilityLevels = emptyList(),
            luckyBoost = luckyBoost
        )
    }

    /**
     * Toplu üretim (Offline/Idle veya fabrika serisi) için adetleri optimize dağıtır.
     */
    fun simulateBatchCrafting(
        totalUnits: Int,
        facilityLevel: Int,
        playerLevel: Int = 1,
        upstreamFacilityLevels: List<Int> = emptyList()
    ): Map<ProductQuality, Int> {
        val distribution = mutableMapOf(
            ProductQuality.STANDARD to 0,
            ProductQuality.SELECTED to 0,
            ProductQuality.SPECIAL to 0,
            ProductQuality.MASTERWORK to 0,
            ProductQuality.PALACE_GRAND to 0
        )

        if (totalUnits <= 0) return distribution

        val probs = calculateSynergyQualityProbabilities(
            facilityLevel = facilityLevel,
            playerLevel = playerLevel,
            upstreamFacilityLevels = upstreamFacilityLevels
        )

        if (totalUnits > 40) {
            // Büyük partiler için olasılık dağılımı + deterministik atama
            var remaining = totalUnits
            for ((quality, prob) in probs) {
                val count = (totalUnits * prob).roundToInt()
                distribution[quality] = count
                remaining -= count
            }
            // Yuvarlama farklarını standart kaliteye ekle
            distribution[ProductQuality.STANDARD] = (distribution[ProductQuality.STANDARD] ?: 0) + remaining
        } else {
            // Küçük partiler için tek tek zar atımı
            for (i in 0 until totalUnits) {
                val q = rollSynergyQuality(facilityLevel, playerLevel, upstreamFacilityLevels)
                distribution[q] = (distribution[q] ?: 0) + 1
            }
        }

        return distribution
    }

    /**
     * VIP Alıcı Kontrolü (İstanbul Saray Lokantaları, Körfez İhracatı vs.)
     */
    fun canSellToVipBuyer(
        quality: ProductQuality,
        buyerId: String
    ): Boolean {
        if (buyerId.contains("vip") || buyerId.contains("palace") || buyerId.contains("export")) {
            return quality.tier >= 4 // Sadece 4 (Usta İşi) ve 5 (Saray/Gurme) kabul edilir
        }
        return true
    }

    private fun applyLuckyBoost(probs: Map<ProductQuality, Double>): Map<ProductQuality, Double> {
        val p1 = (probs[ProductQuality.STANDARD] ?: 0.5) * 0.70
        val p2 = (probs[ProductQuality.SELECTED] ?: 0.2) * 0.85
        val p3 = (probs[ProductQuality.SPECIAL] ?: 0.15) * 1.15
        val p4 = (probs[ProductQuality.MASTERWORK] ?: 0.1) * 1.35
        val p5 = (probs[ProductQuality.PALACE_GRAND] ?: 0.05) * 1.50

        val sum = p1 + p2 + p3 + p4 + p5
        return mapOf(
            ProductQuality.STANDARD to p1 / sum,
            ProductQuality.SELECTED to p2 / sum,
            ProductQuality.SPECIAL to p3 / sum,
            ProductQuality.MASTERWORK to p4 / sum,
            ProductQuality.PALACE_GRAND to p5 / sum
        )
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t.coerceIn(0.0, 1.0)
}

/**
 * Reçetede kullanılan girdi hammadde/ara malların kalite mirası durumu
 */
data class UpstreamHeritageItem(
    val productId: String,
    val productName: String,
    val facilityName: String,
    val ownedFacilityLevel: Int?,
    val qualityTier: ProductQuality,
    val qualityStars: String,
    val isOptimal: Boolean
)

/**
 * Tesis üretiminde Kalite Mirası ve Sinerji Raporu
 */
data class SynergyHeritageAnalysis(
    val product: Product,
    val primaryFacilityLevel: Int,
    val upstreamItems: List<UpstreamHeritageItem>,
    val synergyBonusPercent: Int,
    val defectRiskPercent: Int,
    val probabilities: Map<ProductQuality, Double>,
    val heritageRating: String,
    val summaryTextTr: String,
    val summaryTextEn: String
)

/**
 * Konsorsiyum Mega Proje Tedarik & Sinerji Raporu
 */
data class ConsortiumSynergyAnalysis(
    val overallCraftsmanshipScore: Double,
    val totalSynergyBonusPercent: Int,
    val dividendMultiplier: Float,
    val prestigeMultiplier: Float,
    val defectRateReductionPercent: Int,
    val tierRating: String,
    val summaryTr: String,
    val summaryEn: String
)

