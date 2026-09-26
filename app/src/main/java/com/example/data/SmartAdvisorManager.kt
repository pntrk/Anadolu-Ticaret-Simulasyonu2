package com.example.data

import com.example.R

/**
 * Akıllı Danışman (Smart Advisor) Eylem Tipleri
 */
enum class AdvisorGoalType {
    BUILD_FIRST_FACILITY,      // 1. İlk Tesisi Kur
    START_PRODUCTION,          // 2. Üretimi Başlat
    WAIT_PRODUCTION,           // 2.5 Üretimin Bitmesini Bekle
    TRANSFER_TO_CENTRAL_WAREHOUSE, // 2.75 Merkez Depoya Aktar
    SELL_ON_BORSA,             // 3. Borsada Sat & Kâr Et
    DEPOSIT_IDLE_CASH,         // 4. Parayı Bankada Değerlendir (Mevduat)
    START_FIRST_RESEARCH,      // 5. Ar-Ge Laboratuvarında Araştırma Başlat
    OPEN_MARKET_DELIVERY,      // 6. İller Arası Pazar & Lojistik Sevkiyatı Yap
    HIRE_AUTOMATION_MANAGER,   // 7. İK Müdürü Kirala (Otomasyon)
    SUPPLY_MEGA_CONSORTIUM,    // 8. Mega Proje Konsorsiyumuna Tedarik Sağla
    EXPAND_EMPIRE              // 9. Şirketini Büyüt (Holding & İhracat)
}

/**
 * Danışmanın oyuncuya sunduğu anlık rehber kartı modeli.
 */
data class AdvisorRecommendation(
    val goalType: AdvisorGoalType,
    val stepOrder: Int,
    val titleTr: String,
    val titleEn: String,
    val messageTr: String,
    val messageEn: String,
    val actionTextTr: String,
    val actionTextEn: String,
    val targetRoute: String,
    val targetCityId: String? = null,
    val targetFacilityType: String? = null,
    val targetTechId: String? = null,
    val iconRes: Int = R.drawable.ic_tab_headquarters,
    val badgeTr: String = "ÖNERİLEN HAMLE",
    val badgeEn: String = "RECOMMENDED MOVE",
    val progressRatio: Float = 0f
)

/**
 * Oyuncunun durumunu (seviye, tesisler, nakit, envanter, üretim, ar-ge) analiz edip
 * tek bir kristal netliğinde eylem üreten Akıllı Danışman Karar Motoru.
 */
object SmartAdvisorManager {

    fun analyzeAndRecommend(
        playerLevel: Int,
        money: Long,
        businesses: List<BusinessEntity>,
        inventoryItems: List<InventoryEntity>,
        activeProductions: List<ActiveProduction>,
        researchLevels: Map<String, Int>,
        activeResearches: Map<String, Long>,
        depositBalance: Long = 0L,
        currentCityId: String = "istanbul"
    ): AdvisorRecommendation? {
        val totalInventoryCount = inventoryItems.sumOf { it.quantity }
        val facilityStoredTotal = businesses.sumOf { b ->
            b.getStoredItemsMap().values.sum()
        }
        val hasActiveProduction = activeProductions.isNotEmpty()
        val hasBusinesses = businesses.isNotEmpty()

        // -------------------------------------------------------------
        // DURUM 1: Oyuncunun hiç tesisi yok -> İlk tesisi kurma adımı
        // -------------------------------------------------------------
        if (!hasBusinesses) {
            val suggestedCity = if (currentCityId.isNotBlank()) currentCityId else "konya"
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.BUILD_FIRST_FACILITY,
                stepOrder = 1,
                titleTr = "İlk Fabrikanı Kur 🏭",
                titleEn = "Build Your First Facility 🏭",
                messageTr = "Ticarete başlamak için bir üretim tesisi kurmalısınız. Hemen fabrikanızı inşa edin ve ilk üretimi başlatın!",
                messageEn = "You need a production facility to start trading. Build your first facility now and start producing!",
                actionTextTr = "Fabrika İnşa Et 🚀",
                actionTextEn = "Build Facility 🚀",
                targetRoute = "production",
                targetCityId = suggestedCity,
                badgeTr = "1. ADIM: TEMEL SANAYİ",
                badgeEn = "STEP 1: BASIC INDUSTRY",
                progressRatio = 0.10f
            )
        }

        // -------------------------------------------------------------
        // DURUM 2: Tesisi var ama üretim çalışmıyor ve stok yok -> Üretimi Başlat
        // -------------------------------------------------------------
        if (!hasActiveProduction && totalInventoryCount == 0 && facilityStoredTotal == 0) {
            val firstBiz = businesses.first()
            val isAdvanced = playerLevel >= 3
            val isGrinding = playerLevel < 3 && money > 15_000 // if they already sold once, they have money
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.START_PRODUCTION,
                stepOrder = if (isAdvanced) 4 else if (isGrinding) 3 else 2,
                titleTr = if (isGrinding) "Seviye Atlamak İçin Üret ⚙️" else "Üretim Çarklarını Döndür ⚙️",
                titleEn = if (isGrinding) "Produce to Level Up ⚙️" else "Start Facility Production ⚙️",
                messageTr = if (isGrinding) "Seviye 3'e ulaşıp Ar-Ge'yi açmak için üretmeye ve satmaya devam etmelisiniz!" else "${firstBiz.cityId.uppercase()} şehrindeki tesisiniz boşta bekliyor. Şirketinizi büyütmek için üretime ara vermeyin!",
                messageEn = if (isGrinding) "Keep producing and selling to reach Level 3 and unlock R&D!" else "Your facility in ${firstBiz.cityId.uppercase()} is idle. Don't stop producing to grow your company!",
                actionTextTr = "Üretime Başla ⚡",
                actionTextEn = "Start Production ⚡",
                targetRoute = "production",
                targetCityId = firstBiz.cityId,
                targetFacilityType = firstBiz.type,
                badgeTr = if (isAdvanced) "ÜRETİM TAVSİYESİ" else if (isGrinding) "DENEYİM KAZAN" else "2. ADIM: ÜRETİM",
                badgeEn = if (isAdvanced) "PRODUCTION ADVICE" else if (isGrinding) "GAIN EXPERIENCE" else "STEP 2: PRODUCTION",
                progressRatio = if (isGrinding) 0.45f else 0.25f
            )
        }

        // -------------------------------------------------------------
        // DURUM 2.5: Üretim var ama bitmemiş ve stok yok -> Üretimi Bekle
        // -------------------------------------------------------------
        if (hasActiveProduction && totalInventoryCount == 0 && facilityStoredTotal == 0 && playerLevel < 3) {
            val firstBiz = businesses.first()
            val isGrinding = playerLevel < 3 && money > 15_000
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.WAIT_PRODUCTION,
                stepOrder = if (isGrinding) 3 else 2,
                titleTr = if (isGrinding) "Üretim Bekleniyor (XP Kasılıyor) ⏳" else "Üretim Sürüyor ⏳",
                titleEn = if (isGrinding) "Waiting for Production (Grinding XP) ⏳" else "Production Ongoing ⏳",
                messageTr = "Tesisiniz harıl harıl çalışıyor. Üretim tamamlandığında mallar deponuza eklenecektir. Lütfen bitmesini bekleyin.",
                messageEn = "Your facility is working hard. Goods will be added to your warehouse when production is finished. Please wait.",
                actionTextTr = "Tesise Bak 👀",
                actionTextEn = "View Facility 👀",
                targetRoute = "production",
                targetCityId = firstBiz.cityId,
                targetFacilityType = firstBiz.type,
                badgeTr = if (isGrinding) "DENEYİM KAZAN" else "2. ADIM: BEKLEME",
                badgeEn = if (isGrinding) "GAIN EXPERIENCE" else "STEP 2: WAITING",
                progressRatio = if (isGrinding) 0.45f else 0.30f
            )
        }

        // -------------------------------------------------------------
        // DURUM 2.75: Üretim bitmiş ve tesis deposunda mal var ama merkez depoda yok -> Merkez Depoya Aktar
        // -------------------------------------------------------------
        if (facilityStoredTotal > 0 && totalInventoryCount == 0 && playerLevel < 3) {
            val isGrinding = playerLevel < 3 && money > 15_000
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.TRANSFER_TO_CENTRAL_WAREHOUSE,
                stepOrder = if (isGrinding) 3 else 2,
                titleTr = "Ürünleri Merkeze Çek 🚚",
                titleEn = "Transfer to HQ 🚚",
                messageTr = "Tesisiniz üretimini tamamladı! Ürünler tesis deposunda bekliyor. Satabilmek için ürünleri Merkez Depo'ya aktarın.",
                messageEn = "Your facility finished production! Goods are in the facility warehouse. Transfer them to Central Warehouse to sell.",
                actionTextTr = "Depo Ekranına Git 👀",
                actionTextEn = "Go to Inventory 👀",
                targetRoute = "inventory",
                badgeTr = if (isGrinding) "DENEYİM KAZAN" else "3. ADIM: LOJİSTİK",
                badgeEn = if (isGrinding) "GAIN EXPERIENCE" else "STEP 3: LOGISTICS",
                progressRatio = if (isGrinding) 0.45f else 0.35f
            )
        }

        // -------------------------------------------------------------
        // DURUM 3: Merkez depoda mal var -> Borsada Sat & Kâr Et
        // -------------------------------------------------------------
        if (totalInventoryCount > 0 && playerLevel < 3) {
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.SELL_ON_BORSA,
                stepOrder = 3,
                titleTr = "Borsada Sat & Kârı Topla 📈",
                titleEn = "Sell on Exchange & Profit 📈",
                messageTr = "Merkez deponuzda $totalInventoryCount ton hazır malınız var! Merkez Depoya giderek bu malları 'Borsada Sat' butonuyla anında nakite çevirin ve XP kazanın.",
                messageEn = "You have $totalInventoryCount tons of goods in Central Warehouse! Go to the Warehouse and use the 'Sell' button to convert them to cash and earn XP.",
                actionTextTr = "Merkez Depoyu Aç 🏢",
                actionTextEn = "Open Central Warehouse 🏢",
                targetRoute = "inventory",
                badgeTr = "3. ADIM: SATIŞ & KÂR",
                badgeEn = "STEP 3: TRADING & PROFIT",
                progressRatio = 0.40f
            )
        }

        // -------------------------------------------------------------
        // DURUM 4: Seviye 3+ ve Ar-Ge araştırması başlatılmamış -> Ar-Ge'ye Başla
        // -------------------------------------------------------------
        val isRdUnlocked = FeatureLockManager.isUnlocked(GameFeature.RD_LAB, playerLevel, researchLevels)
        val activeResCount = activeResearches.size
        val totalCompletedTech = researchLevels.values.sum()

        if (isRdUnlocked && activeResCount == 0 && totalCompletedTech == 0) {
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.START_FIRST_RESEARCH,
                stepOrder = 4,
                titleTr = "Ar-Ge ile Yeni Piyasaları Aç 🔬",
                titleEn = "Unlock New Markets with R&D 🔬",
                messageTr = "Şirketiniz Seviye 3 oldu! İller arası pazarı ve otomasyon müdürlerini açmak için ilk Ar-Ge araştırmanızı başlatın.",
                messageEn = "You reached Level 3! Start your first R&D research to unlock intercity trade and automation managers.",
                actionTextTr = "Ar-Ge'yi Aç 🔬",
                actionTextEn = "Open R&D 🔬",
                targetRoute = "rd",
                targetTechId = "tech_logistics",
                badgeTr = "4. ADIM: AR-GE GELİŞİMİ",
                badgeEn = "STEP 4: R&D DEVELOPMENT",
                progressRatio = 0.55f
            )
        }

        // -------------------------------------------------------------
        // DURUM 7: Yüksek Boşta Nakit (>₳250.000) ve Mevduat Yoksa -> Bankada Değerlendir
        // -------------------------------------------------------------
        val isBankUnlocked = FeatureLockManager.isUnlocked(GameFeature.BANKING, playerLevel, researchLevels)
        if (isBankUnlocked && money > 250_000L && depositBalance == 0L) {
            return AdvisorRecommendation(
                goalType = AdvisorGoalType.DEPOSIT_IDLE_CASH,
                stepOrder = 7,
                titleTr = "Boştaki Paranı Faizle Büyüt 🏦",
                titleEn = "Grow Idle Cash with Bank Deposit 🏦",
                messageTr = "Kasanızda ₳${com.example.ui.components.formatMoney(money)} nakit var. Vadeli mevduat hesabı açarak günlük pasif faiz geliri elde edin!",
                messageEn = "You have ₳${com.example.ui.components.formatMoney(money)} idle cash. Open a deposit account for daily passive income!",
                actionTextTr = "Mevduat Aç 🏦",
                actionTextEn = "Open Deposit 🏦",
                targetRoute = "bank",
                badgeTr = "FİNANSAL FIRSAT",
                badgeEn = "FINANCIAL OPPORTUNITY",
                progressRatio = 0.90f
            )
        }

        // -------------------------------------------------------------
        // VARSAYILAN / GENEL GELİŞİM
        // -------------------------------------------------------------
        // Seviye 3 ve sonrasında özel bir aksiyon yoksa (üretim/satış/banka vs. uyarısı yoksa)
        // kalabalık yapmamak adına danışmanı gizliyoruz (null dönüyoruz).
        if (playerLevel >= 3) {
            return null
        }

        return AdvisorRecommendation(
            goalType = AdvisorGoalType.EXPAND_EMPIRE,
            stepOrder = 9,
            titleTr = "Ticaret İmparatorluğunu Büyüt 👑",
            titleEn = "Expand Your Trading Empire 👑",
            messageTr = "Yeni şehirlerde fabrikalar kurun, borsada arbitraj yapın ve Türkiye'nin en zengin holdingi olma yolunda ilerleyin!",
            messageEn = "Establish new factories across cities, trade on the exchange and build Turkey's biggest conglomerate!",
            actionTextTr = "Üretime Git 🏭",
            actionTextEn = "Go to Production 🏭",
            targetRoute = "production",
            badgeTr = "HOLDİNG STRATEJİSİ",
            badgeEn = "CONGLOMERATE STRATEGY",
            progressRatio = 1.0f
        )
    }
}
