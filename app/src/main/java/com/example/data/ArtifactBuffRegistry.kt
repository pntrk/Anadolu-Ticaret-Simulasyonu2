package com.example.data

enum class ArtifactBuffType {
    LOAN_INTEREST_DISCOUNT, DEPOSIT_INTEREST_BONUS, LOGISTICS_COST_DISCOUNT,
    LOGISTICS_SPEED_BONUS, CONSTRUCTION_SPEED_BONUS, UPGRADE_COST_DISCOUNT,
    WEAR_LEVEL_REDUCTION, MAINTENANCE_COST_DISCOUNT, TIER1_PRODUCTION_BONUS,
    TIER4_PRODUCTION_BONUS, BORSA_SELL_BONUS, BORSA_BUY_DISCOUNT,
    MANAGER_SALARY_DISCOUNT, RD_RESEARCH_SPEED, CONSORTIUM_PRESTIGE_BONUS
}

data class ArtifactBuff(
    val artifactId: String,
    val buffType: ArtifactBuffType,
    val buffValue: Float,
    val loreDescription: String
)

object ArtifactBuffRegistry {
    val buffs: List<ArtifactBuff> = listOf(
        ArtifactBuff("art_ahievran_leather_anvil", ArtifactBuffType.WEAR_LEVEL_REDUCTION, 0.20f, "Tesis aşınma hızını %20 yavaşlatır."),
        ArtifactBuff("art_ahilik_seddi", ArtifactBuffType.MAINTENANCE_COST_DISCOUNT, 0.30f, "Tesis bakım maliyetlerinde %30 indirim sağlar."),
        ArtifactBuff("art_damascus_grand_sword", ArtifactBuffType.TIER4_PRODUCTION_BONUS, 0.15f, "Tier 4 Ağır Sanayi üretim süresini %15 kısaltır."),
        ArtifactBuff("art_divani_lugati_turk", ArtifactBuffType.MANAGER_SALARY_DISCOUNT, 0.20f, "Yönetici günlük maaşlarında %20 indirim sağlar."),
        ArtifactBuff("art_gobeklitepe_symbol", ArtifactBuffType.CONSTRUCTION_SPEED_BONUS, 0.25f, "Tesis kurulum sürelerini %25 hızlandırır."),
        ArtifactBuff("art_hereke_silk_carpet", ArtifactBuffType.BORSA_SELL_BONUS, 0.05f, "Borsa spot satışlarında %5 ekstra kâr marjı ekler."),
        ArtifactBuff("art_hittite_sun_disc", ArtifactBuffType.TIER1_PRODUCTION_BONUS, 0.20f, "Tier 1 Hammadde üretim hızını %20 artırır."),
        ArtifactBuff("art_kutahya_tulip_vase", ArtifactBuffType.CONSORTIUM_PRESTIGE_BONUS, 0.10f, "Konsorsiyum teslimatlarında %10 ekstra prestij puanı sağlar."),
        ArtifactBuff("art_ottoman_trade_ferman", ArtifactBuffType.LOGISTICS_COST_DISCOUNT, 0.15f, "Tüm lojistik nakliye maliyetlerini %15 düşürür."),
        ArtifactBuff("art_piri_reis_chart", ArtifactBuffType.LOGISTICS_SPEED_BONUS, 0.20f, "Deniz aşırı lojistik seferlerini %20 hızlandırır."),
        ArtifactBuff("art_seljuk_gold_dinar", ArtifactBuffType.LOAN_INTEREST_DISCOUNT, 0.02f, "Banka kredi faiz oranını net %2 düşürür."),
        ArtifactBuff("art_shahi_cannon_mold", ArtifactBuffType.UPGRADE_COST_DISCOUNT, 0.15f, "Tesis yükseltme maliyetlerini %15 ucuzlatır."),
        ArtifactBuff("art_suleymaniye_defteri", ArtifactBuffType.CONSTRUCTION_SPEED_BONUS, 0.15f, "Tesis inşaat ve yükseltme hızına %15 katkı sağlar."),
        
        ArtifactBuff("art_kultepe_clay_tablet", ArtifactBuffType.BORSA_BUY_DISCOUNT, 0.05f, "Borsa spot alımlarında %5 indirim sağlar."),
        ArtifactBuff("art_cezeri_elephant_clock", ArtifactBuffType.MAINTENANCE_COST_DISCOUNT, 0.20f, "Fabrika günlük bakım maliyetlerini %20 düşürür."),
        ArtifactBuff("art_galata_bankers_ledger", ArtifactBuffType.DEPOSIT_INTEREST_BONUS, 0.015f, "Banka mevduat faiz oranını net %1.5 artırır."),
        ArtifactBuff("art_ulug_bey_astrolabe", ArtifactBuffType.RD_RESEARCH_SPEED, 0.25f, "Ar-Ge teknoloji araştırma sürelerini %25 kısaltır."),
        ArtifactBuff("art_mimar_sinan_compass", ArtifactBuffType.CONSTRUCTION_SPEED_BONUS, 0.30f, "Tesis inşaatlarında %30 mimari hız bonusu sağlar."),
        ArtifactBuff("art_barbaros_compass", ArtifactBuffType.LOGISTICS_SPEED_BONUS, 0.25f, "Lojistik taşıma sürelerini %25 hızlandırır."),
        ArtifactBuff("art_troy_gold_diadem", ArtifactBuffType.BORSA_SELL_BONUS, 0.08f, "Borsa satışlarından elde edilen geliri %8 artırır.")
    )

    fun getActiveBuffs(ownedArtifactIds: List<String>): Map<ArtifactBuffType, Float> {
        val activeBuffs = mutableMapOf<ArtifactBuffType, Float>()
        ownedArtifactIds.forEach { id ->
            val artifactBuff = buffs.find { it.artifactId == id }
            if (artifactBuff != null) {
                val currentVal = activeBuffs[artifactBuff.buffType] ?: 0f
                activeBuffs[artifactBuff.buffType] = currentVal + artifactBuff.buffValue
            }
        }
        return activeBuffs
    }
}
