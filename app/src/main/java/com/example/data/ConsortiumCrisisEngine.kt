package com.example.data

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Konsorsiyum Hammadde Darboğazı & Makroekonomik Kriz Durumu (System Dynamics Entegrasyonlu).
 *
 * Milyonlarca eşzamanlı oyuncu ve binlerce mega proje için Sistem Dinamikleri (System Dynamics)
 * prensipleriyle modellenmiştir:
 * - Azalan Getiri (Diminishing Returns) ve Lojistik Doygunluk (Sigmoid Saturation)
 * - Stok-Akış Dengeleme Döngüleri (Balancing Feedback Loops)
 * - Ölçek Direnci ve Şok Soğurma Kapasitesi (Market Resilience & Shock Absorption)
 */
data class ConsortiumCrisisState(
    val isCrisisActive: Boolean = false,
    val totalConsortiums: Int = 0,
    val bottleneckConsortiumsCount: Int = 0,
    val bottleneckPercentage: Float = 0f,
    val consecutiveCrisisTicks: Int = 0,
    val crisisTitle: String = "Normal Piyasa Dengesi",
    val crisisDescription: String = "",
    val rawMaterialInflationMultiplier: Float = 1.0f, // Krizde hammadde fiyatları +%35 - +%65 (azalan getiri ile sınırlı)
    val stockMarketDropMultiplier: Float = 1.0f,     // Borsa hisseleri -%20 - -%38 düşer
    val loanInterestSurge: Float = 0.0f,             // Banka kredi faizlerine ek faiz artışı (+%5 - +%12)
    val tier1And2ProductionSlowdownMultiplier: Float = 1.0f, // Alt seviye üretim yavaşlaması (+%40 - +%50)
    val facilityWearRateMultiplier: Float = 1.0f,             // Ekipman yıpranma hızı çarpanı (2.0x - 3.2x)
    val logisticsCostMultiplier: Float = 1.0f,                // Nakliye & Lojistik maliyet artışı (+%40 - +%60)
    val criticalMissingMaterials: List<String> = emptyList(), // En çok eksik olan hammaddeler
    // Sistem Dinamikleri Metrikleri (System Dynamics Metrics):
    val systemStrainIndex: Float = 0.0f,                      // Tedarik zinciri gerilim endeksi [0.0 .. 1.0]
    val marketResilienceFactor: Float = 1.0f,                 // Piyasa derinliği ve öz-dengeleme faktörü
    val shockAbsorptionCapacity: Float = 1.0f                 // Dışsal şokları soğurma katsayısı
)

object ConsortiumCrisisEngine {

    // Temel sanayi & üretim hammaddeleri (Darboğazda hiper-enflasyona uğrayacak kalemler)
    val RAW_MATERIAL_IDS = setOf(
        "iron", "copper", "silicon", "aluminum", "coal", "limestone", "lithium", "timber",
        "chemicals", "crude_oil", "natural_gas", "titanium", "graphite_ore", "rubber_latex", "cotton",
        "steel", "wire", "glass", "fabric", "battery", "plastic", "wood_plank", "cement",
        "refined_fuel", "tire", "packaging", "aluminum_ingot", "liquefied_gas", "titanium_ingot",
        "pcb_substrate", "synthetic_textile", "titanium_alloy", "carbon_fiber", "semiconductor_wafer",
        "optical_fiber", "graphene_sheet"
    )

    /**
     * Evaluating Consortium Crisis and Supply Chain Bottlenecks dynamically using System Dynamics.
     */
    fun evaluateConsortiumCrisis(
        consortiums: List<MegaProject>,
        currentCrisisState: ConsortiumCrisisState
    ): ConsortiumCrisisState {
        val activeConsortiums = consortiums.filter { it.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION }
        val totalActive = activeConsortiums.size
        if (totalActive == 0) {
            return ConsortiumCrisisState()
        }

        val bottleneckConsortiums = activeConsortiums.filter { it.slots.any { slot -> slot.isBottleneckWarning } }
        val bottleneckCount = bottleneckConsortiums.size
        val bottleneckPercentage = bottleneckCount.toFloat() / totalActive
        val isCrisisActive = bottleneckCount > 0

        val nextConsecutiveTicks = if (isCrisisActive) currentCrisisState.consecutiveCrisisTicks + 1 else 0

        // Calculate Sigmoid-like saturation system strain index:
        val strain = (1.0f - exp(-0.25f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage
        val finalStrain = strain.coerceIn(0f, 1f)

        // Calculate Dynamic Multipliers:
        val rawMaterialInflationMultiplier = 1.0f + 2.0f * (1.0f - exp(-0.15f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage
        val stockMarketDropMultiplier = 1.0f - 0.6f * (1.0f - exp(-0.1f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage
        val loanInterestSurge = 0.12f * (1.0f - exp(-0.1f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage
        val tier1And2ProductionSlowdownMultiplier = 1.0f + 0.5f * (1.0f - exp(-0.1f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage
        val facilityWearRateMultiplier = 1.0f + 2.2f * (1.0f - exp(-0.1f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage
        val logisticsCostMultiplier = 1.0f + 0.6f * (1.0f - exp(-0.1f * nextConsecutiveTicks)).toFloat() * bottleneckPercentage

        val marketResilienceFactor = (1.0f - 0.5f * finalStrain).coerceIn(0.1f, 1.0f)
        val shockAbsorptionCapacity = (1.0f - 0.4f * finalStrain).coerceIn(0.1f, 1.0f)

        // Identify critical missing materials based on bottleneck slots
        val bottleneckSlots = activeConsortiums.flatMap { it.slots }.filter { it.isBottleneckWarning }
        val missingMaterialsMap = mutableMapOf<String, Long>()
        bottleneckSlots.forEach { slot ->
            val deficit = (slot.quantityRequired - slot.quantityDelivered).toLong().coerceAtLeast(0L)
            missingMaterialsMap[slot.productId] = (missingMaterialsMap[slot.productId] ?: 0L) + deficit
        }
        val criticalMissingMaterials = missingMaterialsMap.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val title = if (isCrisisActive) "🚨 TEDARİK ZİNCİRİ VE HAMMADDE KRİZİ" else "Normal Piyasa Dengesi"
        val desc = if (isCrisisActive) {
            "Konsorsiyum projelerindeki darboğazlar sebebiyle piyasada hammadde fiyatları artıyor."
        } else {
            "Piyasa dengesi stabil ve tedarik zincirleri normal işliyor."
        }

        return ConsortiumCrisisState(
            isCrisisActive = isCrisisActive,
            totalConsortiums = totalActive,
            bottleneckConsortiumsCount = bottleneckCount,
            bottleneckPercentage = bottleneckPercentage,
            consecutiveCrisisTicks = nextConsecutiveTicks,
            crisisTitle = title,
            crisisDescription = desc,
            rawMaterialInflationMultiplier = rawMaterialInflationMultiplier,
            stockMarketDropMultiplier = stockMarketDropMultiplier,
            loanInterestSurge = loanInterestSurge,
            tier1And2ProductionSlowdownMultiplier = tier1And2ProductionSlowdownMultiplier,
            facilityWearRateMultiplier = facilityWearRateMultiplier,
            logisticsCostMultiplier = logisticsCostMultiplier,
            criticalMissingMaterials = criticalMissingMaterials,
            systemStrainIndex = finalStrain,
            marketResilienceFactor = marketResilienceFactor,
            shockAbsorptionCapacity = shockAbsorptionCapacity
        )
    }
}
