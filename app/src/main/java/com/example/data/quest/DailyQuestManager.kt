package com.example.data.quest

import com.example.data.security.TimeSecurityManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily Quests & Season Pass Domain Engine.
 * Generates dynamic daily mini-goals, tracks player milestones, and calculates Season Pass rewards.
 */
object DailyQuestManager {

    val SEASON_PASS_TIERS = listOf(
        SeasonPassTier(1, 100, "100.000 ₳ Başlangıç Teşviki", "100,000 ₳ Starter Grant", 100_000L, 0, "250.000 ₳ VIP Bonusu", "250,000 ₳ VIP Bonus"),
        SeasonPassTier(2, 250, "150.000 ₳ Ticaret Fonu", "150,000 ₳ Trade Fund", 150_000L, 0, "500.000 ₳ + %5 Navlun İndirimi", "500,000 ₳ + 5% Freight Discount"),
        SeasonPassTier(3, 450, "250.000 ₳", "250,000 ₳", 250_000L, 0, "1.000.000 ₳", "1,000,000 ₳"),
        SeasonPassTier(4, 700, "400.000 ₳ Ar-Ge Hibesi", "400,000 ₳ R&D Grant", 400_000L, 0, "1.500.000 ₳ + Hızlı Üretim Lisansı", "1,500,000 ₳ + Fast Production Permit"),
        SeasonPassTier(5, 1000, "750.000 ₳", "750,000 ₳", 750_000L, 0, "3.000.000 ₳", "3,000,000 ₳"),
        SeasonPassTier(6, 1400, "1.000.000 ₳ Lojistik Teşviki", "1,000,000 ₳ Logistics Grant", 1_000_000L, 0, "5.000.000 ₳ + %10 Depo Kapasitesi", "5,000,000 ₳ + 10% Storage Boost"),
        SeasonPassTier(7, 1900, "1.500.000 ₳", "1,500,000 ₳", 1_500_000L, 0, "7.500.000 ₳", "7,500,000 ₳"),
        SeasonPassTier(8, 2500, "2.500.000 ₳ Yatırım Hibesi", "2,500,000 ₳ Investment Grant", 2_500_000L, 0, "12.000.000 ₳ + VIP Vergi Muafiyeti", "12,000,000 ₳ + VIP Tax Relief"),
        SeasonPassTier(9, 3200, "4.000.000 ₳", "4,000,000 ₳", 4_000_000L, 0, "20.000.000 ₳", "20,000,000 ₳"),
        SeasonPassTier(10, 4000, "10.000.000 ₳ Usta Tüccar Plaketi", "10,000,000 ₳ Master Trader Plaque", 10_000_000L, 0, "50.000.000 ₳ + Anadolu Holding Rozeti", "50,000,000 ₳ + Anatolian Holding Badge")
    )

    /**
     * Returns today's unique date key (e.g. "2026-08-29") based on secure time.
     */
    fun getTodayDateKey(): String {
        val now = TimeSecurityManager.getSecureCurrentTimeMs()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(now))
    }

    /**
     * Generates a fresh balanced set of dynamic daily quests for today based on the player's level.
     */
    fun generateFreshDailyQuests(playerLevel: Int = 1): List<DailyQuest> {
        val lvl = playerLevel.coerceAtLeast(1)
        val todayKey = getTodayDateKey()
        val levelMult = 1.0f + (lvl - 1) * 0.20f

        // Quest 1: Daily Login
        val loginMoney = (250_000L * levelMult).toLong()
        val loginGems = 0
        val loginXp = 100

        // Quest 2: Borsa Trades
        val borsaTarget = (3 + (lvl / 3)).toLong().coerceAtMost(20L)
        val borsaMoney = (450_000L * levelMult).toLong()
        val borsaGems = 0
        val borsaXp = 130 + lvl * 5

        // Quest 3: Sell Commodities
        val sellTarget = (200L * lvl).coerceAtLeast(200L).coerceAtMost(10_000L)
        val sellMoney = (350_000L * levelMult).toLong()
        val sellGems = 0
        val sellXp = 100 + lvl * 5

        // Quest 4: Produce in Facilities
        val produceTarget = (150L * lvl).coerceAtLeast(100L).coerceAtMost(5_000L)
        val produceMoney = (500_000L * levelMult).toLong()
        val produceGems = 0
        val produceXp = 130 + lvl * 5

        // Quest 5: Logistics Deliveries
        val deliveryTarget = (2 + (lvl / 4)).toLong().coerceAtMost(12L)
        val deliveryMoney = (600_000L * levelMult).toLong()
        val deliveryGems = 0
        val deliveryXp = 150 + lvl * 5

        return listOf(
            DailyQuest(
                id = "quest_daily_login",
                title = "Şirket Merkezini Aç & Sermaye Teşvikini Al",
                titleEn = "Open HQ & Claim Capital Grant",
                description = "Şirket merkezini ziyaret et ve ₳${com.example.ui.components.formatMoney(loginMoney)} başlangıç hibesini otomatik olarak al.",
                descriptionEn = "Visit company HQ and auto-claim your ₳${com.example.ui.components.formatMoney(loginMoney)} grant.",
                iconName = "login",
                targetAmount = 1L,
                currentProgress = 1L,
                rewardMoney = loginMoney,
                rewardGems = loginGems,
                rewardSeasonXp = loginXp,
                type = QuestType.DAILY_LOGIN,
                targetRoute = "home"
            ),
            DailyQuest(
                id = "quest_borsa_trades",
                title = "Şehir Borsasında $borsaTarget İşlem Yap",
                titleEn = "Complete $borsaTarget Borsa Exchange Trades",
                description = "Borsa ekranında alım veya satım yaparak $borsaTarget emtia işlemi gerçekleştir. (Dokun: Borsa Ekranına Git)",
                descriptionEn = "Perform $borsaTarget buy or sell commodity transactions on the Borsa Exchange market.",
                iconName = "trending_up",
                targetAmount = borsaTarget,
                currentProgress = 0L,
                rewardMoney = borsaMoney,
                rewardGems = borsaGems,
                rewardSeasonXp = borsaXp,
                type = QuestType.BORSA_TRADE,
                targetRoute = "borsa"
            ),
            DailyQuest(
                id = "quest_sell_commodities",
                title = "Pazarda veya Borsada $sellTarget Ton Mal Sat",
                titleEn = "Sell $sellTarget Tons of Commodities",
                description = "Pazar veya Borsa ekranından toplam $sellTarget ton emtia/ürün satışı gerçekleştir. (Dokun: Pazar Ekranına Git)",
                descriptionEn = "Sell a total of $sellTarget tons of commodities in the Market or Borsa Exchange to boost trade volume.",
                iconName = "storefront",
                targetAmount = sellTarget,
                currentProgress = 0L,
                rewardMoney = sellMoney,
                rewardGems = sellGems,
                rewardSeasonXp = sellXp,
                type = QuestType.SELL_COMMODITY,
                targetRoute = "market"
            ),
            DailyQuest(
                id = "quest_factory_produce",
                title = "Üretim Tesislerinde $produceTarget Birim Üretim Yap",
                titleEn = "Produce $produceTarget Units in Facilities",
                description = "Tesisler ekranındaki fabrika veya madenlerinde toplam $produceTarget birim ürün üretimi tamamla. (Dokun: Üretim Ekranına Git)",
                descriptionEn = "Complete production cycles of $produceTarget total units across your factories, farms, or mines.",
                iconName = "factory",
                targetAmount = produceTarget,
                currentProgress = 0L,
                rewardMoney = produceMoney,
                rewardGems = produceGems,
                rewardSeasonXp = produceXp,
                type = QuestType.FACILITY_PRODUCE,
                targetRoute = "production"
            ),
            DailyQuest(
                id = "quest_logistics_deliveries",
                title = "$deliveryTarget Şehirlerarası Lojistik Sevkiyatı Başlat",
                titleEn = "Complete $deliveryTarget Intercity Cargo Shipments",
                description = "Harita/Lojistik ekranından şehirlerarası $deliveryTarget ürün sevkiyatı gerçekleştir. (Dokun: Harita/Lojistik Ekranına Git)",
                descriptionEn = "Dispatch and complete $deliveryTarget intercity truck shipments using your logistics fleet on the Map screen.",
                iconName = "local_shipping",
                targetAmount = deliveryTarget,
                currentProgress = 0L,
                rewardMoney = deliveryMoney,
                rewardGems = deliveryGems,
                rewardSeasonXp = deliveryXp,
                type = QuestType.LOGISTICS_DELIVERY,
                targetRoute = "map"
            )
        )
    }

    /**
     * Updates progress on quests matching the given [type].
     */
    fun updateQuestProgress(
        currentState: DailyQuestState,
        type: QuestType,
        amount: Long,
        playerLevel: Int = 1
    ): DailyQuestState {
        val todayKey = getTodayDateKey()
        
        // Sadece quest listesi boşsa yeni görevler üret, aksi halde var olanları kullan
        val isFirstTime = currentState.quests.isEmpty()
        val activeQuests = if (isFirstTime) {
            generateFreshDailyQuests(playerLevel)
        } else {
            currentState.quests
        }

        // Görevler artık tek seferlik olduğundan currentClaimedIds'i asla sıfırlamıyoruz
        val currentClaimedIds = currentState.claimedQuestIdsToday

        val updatedQuests = activeQuests.map { quest ->
            val isAlreadyClaimed = quest.isClaimed || currentClaimedIds.contains(quest.id)
            if (quest.type == type && !isAlreadyClaimed) {
                val newProg = (quest.currentProgress + amount).coerceAtMost(quest.targetAmount)
                quest.copy(currentProgress = newProg, isClaimed = false)
            } else {
                quest.copy(isClaimed = isAlreadyClaimed)
            }
        }

        return currentState.copy(
            quests = updatedQuests,
            lastResetDateKey = todayKey,
            claimedQuestIdsToday = currentClaimedIds
        )
    }

    /**
     * Calculates the current Season Pass level based on total earned season XP.
     */
    fun computeSeasonLevel(totalXp: Int): Int {
        var lvl = 1
        for (tier in SEASON_PASS_TIERS) {
            if (totalXp >= tier.requiredXp) {
                lvl = tier.tierLevel
            } else {
                break
            }
        }
        return lvl
    }
}
