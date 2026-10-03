package com.example.data

import com.example.data.network.anyToJsonElement
import java.util.UUID

/**
 * Türkiye'nin geliştirdiği güncel sanayi markaları ve milli teknoloji hamlesi
 * vizyonuna uygun, oyun dengesi ve kurgusal yapısıyla tam uyumlu 20 adet Bot Konsorsiyum Çeşidi.
 */
object ConsortiumBotRegistry {

    private data class BotSlotSetup(
        val botId: String? = null,
        val botName: String? = null,
        val deliveryProgress: Float = 0f // 0f = 0%, 0.5f = 50%, 1f = 100%
    )

    private data class BotConsortiumDef(
        val id: String,
        val consortiumName: String,
        val brandName: String,
        val leaderName: String,
        val targetProductId: String,
        val targetProductName: String,
        val cityId: String,
        val qualityTier: ConsortiumQualityTier,
        val currentStage: MegaProjectStage,
        val memberCount: Int,
        val warehouseStock: Int = 0,
        val totalItemsProduced: Int = 0,
        val slotSetups: List<BotSlotSetup> // Exactly 4 slots
    )

    private val BOT_DEFS = listOf(
        // 1. TOGG - Otomotiv & Elektrikli Mobilite
        BotConsortiumDef(
            id = "bot-guild-v5-1",
            consortiumName = "Türkiye Otomobil Girişim Grubu (TOGG)",
            brandName = "TOGG T10X & T10F Akıllı Mobilite",
            leaderName = "TOGG Bilişim Vadisi & Gemlik Kampüsü (Bursa)",
            targetProductId = "ev",
            targetProductName = "TOGG T10X Akıllı Cihaz",
            cityId = "bursa",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_2_HARDWARE,
            memberCount = 54,
            warehouseStock = 12,
            totalItemsProduced = 48,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Selim Kaya (Kaya Ağır Sanayi A.Ş.)", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 2. Baykar & TUSAŞ - Havacılık & SİHA
        BotConsortiumDef(
            id = "bot-guild-v5-2",
            consortiumName = "Baykar & TUSAŞ Milli Havacılık",
            brandName = "Bayraktar Kızılelma & KAAN Muharip Uçak",
            leaderName = "Özdemir Bayraktar Teknoloji Merkezi (İstanbul)",
            targetProductId = "uav",
            targetProductName = "Bayraktar Kızılelma & TB3",
            cityId = "istanbul",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
            memberCount = 68,
            warehouseStock = 28,
            totalItemsProduced = 114,
            slotSetups = listOf(
                BotSlotSetup("BOT-AVRASYA-05", "Hakan Erkin (Avrasya Savunma & Havacılık)", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 3. ROKETSAN & TUA - Uzay Sistemleri & Roket
        BotConsortiumDef(
            id = "bot-guild-v5-3",
            consortiumName = "ROKETSAN & TUA Uzay Sistemleri",
            brandName = "ROKETSAN Şimşek & MUFS Fırlatıcı",
            leaderName = "Türkiye Uzay Ajansı Başkanlığı (Ankara)",
            targetProductId = "space_rocket",
            targetProductName = "Şimşek-1 Yörünge Fırlatma Roketi",
            cityId = "ankara",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            memberCount = 38,
            warehouseStock = 2,
            totalItemsProduced = 6,
            slotSetups = listOf(
                BotSlotSetup("BOT-AVRASYA-05", "Avrasya Savunma & Havacılık Sanayii", 0.65f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 4. Sedef & Mavi Vatan - Gemi İnşa & Konteyner
        BotConsortiumDef(
            id = "bot-guild-v5-4",
            consortiumName = "Mavi Vatan Tersanecilik Konsorsiyumu",
            brandName = "Piri Reis Mega Konteyner & LHD",
            leaderName = "Tuzla Sedef Gemi İnşa Sanayii (İstanbul)",
            targetProductId = "cargo_ship",
            targetProductName = "Piri Reis 24.000 TEU Konteyner Gemisi",
            cityId = "istanbul",
            qualityTier = ConsortiumQualityTier.GRADE_C,
            currentStage = MegaProjectStage.STAGE_2_HARDWARE,
            memberCount = 45,
            warehouseStock = 5,
            totalItemsProduced = 22,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Kaya Ağır Sanayi & Metalurji A.Ş.", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 5. TÜRASAŞ - Raylı Sistemler & Hızlı Tren
        BotConsortiumDef(
            id = "bot-guild-v5-5",
            consortiumName = "TÜRASAŞ Milli Raylı Sistemler A.Ş.",
            brandName = "TÜRASAŞ Anadolu Ekspresi Milli Hızlı Tren",
            leaderName = "Milli Hızlı Tren Fabrikaları (Eskişehir)",
            targetProductId = "bullet_train",
            targetProductName = "Anadolu Hızlı Tren Seti",
            cityId = "eskisehir",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_3_TESTING,
            memberCount = 58,
            warehouseStock = 16,
            totalItemsProduced = 52,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Kaya Ağır Ray ve Çelik Döküm", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 6. HAVELSAN & Bilişim Vadisi - Yapay Zeka Veri Merkezi
        BotConsortiumDef(
            id = "bot-guild-v5-6",
            consortiumName = "HAVELSAN & Bilişim Vadisi İnovasyon",
            brandName = "HAVELSAN MAIN & Ulusal Kuantum Veri Çekirdeği",
            leaderName = "Bilişim Vadisi Yapay Zeka Direktörlüğü (Kocaeli)",
            targetProductId = "ai_datacenter",
            targetProductName = "HAVELSAN Milli AI Veri Merkezi",
            cityId = "kocaeli",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            memberCount = 42,
            warehouseStock = 4,
            totalItemsProduced = 15,
            slotSetups = listOf(
                BotSlotSetup("BOT-NOVA-02", "Nova Tech Dynamics Silikon Lab", 0.55f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 7. MİLGEM & STM - Savunma Fırkateyni
        BotConsortiumDef(
            id = "bot-guild-v5-7",
            consortiumName = "MİLGEM Türk Donanma Konsorsiyumu",
            brandName = "TCG İstanbul (F-515) İstif Sınıfı Fırkateyn",
            leaderName = "STM Savunma & Gölcük Askeri Tersanesi (Kocaeli)",
            targetProductId = "defense_frigate",
            targetProductName = "MİLGEM F-515 Savunma Fırkateyni",
            cityId = "kocaeli",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
            memberCount = 61,
            warehouseStock = 18,
            totalItemsProduced = 84,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Kaya Ağır Zırh Çeliği A.Ş.", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 8. TÜRKSAT & TÜBİTAK UZAY - Uydu Sistemleri
        BotConsortiumDef(
            id = "bot-guild-v5-8",
            consortiumName = "TÜRKSAT & TÜBİTAK UZAY Konsorsiyumu",
            brandName = "TÜRKSAT 6A & İMECE-2 Gözlem Uydusu",
            leaderName = "Milli Uydu Entegrasyon ve Test Merkezi (Ankara)",
            targetProductId = "satellite",
            targetProductName = "TÜRKSAT 6A Milli Haberleşme Uydusu",
            cityId = "ankara",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_3_TESTING,
            memberCount = 36,
            warehouseStock = 8,
            totalItemsProduced = 32,
            slotSetups = listOf(
                BotSlotSetup("BOT-NOVA-02", "Nova Tech Uzay Dereceli Mikroçip", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 9. TÜPRAŞ & ETİMADEN - Yeşil Hidrojen & Bor
        BotConsortiumDef(
            id = "bot-guild-v5-9",
            consortiumName = "TÜPRAŞ & ETİMADEN Yeşil Hidrojen Rafinerisi",
            brandName = "ETİ Bor-Hidrojen Sıfır Karbon Kompleksi",
            leaderName = "Bandırma Bor Teknolojileri & TÜPRAŞ Yeşil Dönüşüm (Bursa)",
            targetProductId = "hydrogen_plant",
            targetProductName = "ETİ Bor Yeşil Hidrojen Reaktörü",
            cityId = "bursa",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_2_HARDWARE,
            memberCount = 49,
            warehouseStock = 9,
            totalItemsProduced = 38,
            slotSetups = listOf(
                BotSlotSetup("BOT-TOROS-03", "Toros Sıvılaştırılmış Gaz İşleme", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 10. ASELSAN & TEİAŞ - Akıllı Enerji Şebekesi
        BotConsortiumDef(
            id = "bot-guild-v5-10",
            consortiumName = "ASELSAN & TEİAŞ Akıllı Şebeke Sistemleri",
            brandName = "ASELSAN MİRAK Siber Dayanıklı Enerji Ağı",
            leaderName = "ASELSAN Güç Elektroniği ve Otomasyon (Ankara)",
            targetProductId = "smart_grid",
            targetProductName = "ASELSAN MİRAK Ulusal Akıllı Şebeke",
            cityId = "ankara",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            memberCount = 52,
            warehouseStock = 7,
            totalItemsProduced = 29,
            slotSetups = listOf(
                BotSlotSetup("BOT-NOVA-02", "Nova Tech SCADA Kontrol Kartları", 0.4f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 11. Türk Ege & Akdeniz - Lüks Mega Yat
        BotConsortiumDef(
            id = "bot-guild-v5-11",
            consortiumName = "Türk Ege & Akdeniz Mega Yat Tersaneleri",
            brandName = "Turquoise BlueVoyage 85M Hibrit Mega Yat",
            leaderName = "Mengi Yay & Turquoise Gemi İnşa Sanayii (Muğla)",
            targetProductId = "super_yacht",
            targetProductName = "Mavi Vatan 85M Hibrit Lüks Mega Yat",
            cityId = "mugla",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_2_HARDWARE,
            memberCount = 34,
            warehouseStock = 6,
            totalItemsProduced = 19,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Kaya Özel Marin Çelik Döküm", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 12. TENMAK & Akkuyu - Toryum & İleri Nükleer
        BotConsortiumDef(
            id = "bot-guild-v5-12",
            consortiumName = "TENMAK & Akkuyu İleri Nükleer Grubu",
            brandName = "TENMAK Toryum Çekirdeği & SMR Kompleksi",
            leaderName = "Türkiye Enerji ve Nükleer Araştırma Kurumu (Mersin)",
            targetProductId = "fusion_reactor_core",
            targetProductName = "Akkuyu Toryum Modüler Reaktör Çekirdeği",
            cityId = "mersin",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            memberCount = 40,
            warehouseStock = 3,
            totalItemsProduced = 8,
            slotSetups = listOf(
                BotSlotSetup("BOT-AVRASYA-05", "Avrasya Titanyum Nükleer Kalkan", 0.5f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 13. TÜBİTAK BİLGEM - Kuantum Süperbilgisayarı
        BotConsortiumDef(
            id = "bot-guild-v5-13",
            consortiumName = "TÜBİTAK BİLGEM Kuantum Teknolojileri",
            brandName = "BİLGEM ATA Kuantum Süperbilgisayarı",
            leaderName = "Gebze Ulusal Kuantum ve Kriptoloji Merkezi (Kocaeli)",
            targetProductId = "quantum_supercomputer",
            targetProductName = "BİLGEM ATA Süper İletken Kuantum Bilgisayarı",
            cityId = "kocaeli",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_3_TESTING,
            memberCount = 29,
            warehouseStock = 5,
            totalItemsProduced = 14,
            slotSetups = listOf(
                BotSlotSetup("BOT-NOVA-02", "Nova Kuantum Mantık Kapıları", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 14. STM & ASELSAN - Sürü Robotik & Otonom İHA
        BotConsortiumDef(
            id = "bot-guild-v5-14",
            consortiumName = "STM & Aselsan Sürü Robotik Teknolojileri",
            brandName = "KARGU-S & ALBATROS Taktik Sürü İHA Ağı",
            leaderName = "Otonom Savunma Robotik Sistemler Direktörlüğü (Ankara)",
            targetProductId = "autonomous_drone_swarm",
            targetProductName = "KARGU-S Yapay Zeka Sürü İHA Ağı",
            cityId = "ankara",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_2_HARDWARE,
            memberCount = 44,
            warehouseStock = 14,
            totalItemsProduced = 45,
            slotSetups = listOf(
                BotSlotSetup("BOT-ANKARA-14", "Ankara Taktik Telekom İstasyonu", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 15. TÜBİTAK RUTE - Manyetik Hyperloop
        BotConsortiumDef(
            id = "bot-guild-v5-15",
            consortiumName = "TÜBİTAK RUTE Manyetik Hyperloop İttifakı",
            brandName = "RUTE ŞİMŞEK 1000 km/s Manyetik Kapsül",
            leaderName = "Raylı Ulaşım Teknolojileri Enstitüsü (Konya)",
            targetProductId = "hyperloop_capsule",
            targetProductName = "RUTE ŞİMŞEK MagLev Manyetik Kapsülü",
            cityId = "konya",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            memberCount = 31,
            warehouseStock = 3,
            totalItemsProduced = 11,
            slotSetups = listOf(
                BotSlotSetup("BOT-KORDSA-15", "Kordsa Karbon Kompozit Gövde Grubu", 0.45f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 16. TCI & Turkish Technic - VIP Uçak Kabini
        BotConsortiumDef(
            id = "bot-guild-v5-16",
            consortiumName = "TCI & Turkish Technic Havacılık Sistemleri",
            brandName = "Anadolu SkySuite VIP Geniş Gövde Kabini",
            leaderName = "Türk Havacılık Kabin İçi Sanayii A.Ş. (İstanbul)",
            targetProductId = "luxury_aircraft_interior",
            targetProductName = "TCI SkySuite VIP Uçak Kabini",
            cityId = "istanbul",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
            memberCount = 37,
            warehouseStock = 32,
            totalItemsProduced = 96,
            slotSetups = listOf(
                BotSlotSetup("BOT-KORDSA-15", "Kordsa Havacılık Kompozit Panelleri", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 17. İstanbul Finans Merkezi - Akıllı Kuleler
        BotConsortiumDef(
            id = "bot-guild-v5-17",
            consortiumName = "İstanbul Finans Merkezi Akıllı Kule Grubu",
            brandName = "İFM Zirve LEED Platin Yeşil Gökdelen",
            leaderName = "Emlak Konut GYO & Çevre Şehircilik (İstanbul)",
            targetProductId = "smart_skyscraper",
            targetProductName = "İFM Zirve Karbon-Nötr Akıllı Kulesi",
            cityId = "istanbul",
            qualityTier = ConsortiumQualityTier.GRADE_B,
            currentStage = MegaProjectStage.STAGE_1_BODY,
            memberCount = 47,
            warehouseStock = 5,
            totalItemsProduced = 16,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Kaya Depreme Dayanıklı Çelik Blok", 0.65f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 18. TEI TUSAŞ Motor Sanayii - Milli Turbofan
        BotConsortiumDef(
            id = "bot-guild-v5-18",
            consortiumName = "TEI Havacılık ve Motor Sanayii A.Ş.",
            brandName = "TEI-TF6000 & TF10000 Askeri Turbofan",
            leaderName = "TEI Milli Uçak Motoru Tasarım Üssü (Eskişehir)",
            targetProductId = "turbine_engine",
            targetProductName = "TEI-TF6000 Askeri Turbofan Motoru",
            cityId = "eskisehir",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_4_MASS_PRODUCTION,
            memberCount = 63,
            warehouseStock = 45,
            totalItemsProduced = 142,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYA-01", "Kaya Yüksek Mukavemetli Havacılık Çeliği", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 19. ASPİLSAN Enerji - Lityum & Grafen Batarya
        BotConsortiumDef(
            id = "bot-guild-v5-19",
            consortiumName = "ASPİLSAN Enerji Batarya Sanayii A.Ş.",
            brandName = "ASPİLSAN Lityum-İyon & Grafen Güç Hücresi",
            leaderName = "ASPİLSAN Silindirik Pil & Batarya Kompleksi (Kayseri)",
            targetProductId = "nano_battery",
            targetProductName = "ASPİLSAN Silindirik Li-İyon Pil Hücresi",
            cityId = "kayseri",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_2_HARDWARE,
            memberCount = 56,
            warehouseStock = 22,
            totalItemsProduced = 76,
            slotSetups = listOf(
                BotSlotSetup("BOT-KAYSERI-19", "ASPİLSAN İleri Grafen Levha Ünitesi", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        ),

        // 20. TÜBİTAK BİLGEM & Yongatek - Milli Mikroçip
        BotConsortiumDef(
            id = "bot-guild-v5-20",
            consortiumName = "TÜBİTAK BİLGEM & Yongatek Çip Sanayii",
            brandName = "ÇAKIL & ÇAĞRI Milli Güvenlik Mikroçipi",
            leaderName = "Milli Yarı İletken ve Entegre Devre Üssü (Kocaeli)",
            targetProductId = "chip",
            targetProductName = "ÇAKIL Milli Güvenlik Mikroçipi",
            cityId = "kocaeli",
            qualityTier = ConsortiumQualityTier.GRADE_A,
            currentStage = MegaProjectStage.STAGE_3_TESTING,
            memberCount = 33,
            warehouseStock = 38,
            totalItemsProduced = 120,
            slotSetups = listOf(
                BotSlotSetup("BOT-NOVA-02", "Nova Silikon Yarı İletken Wafer", 1.0f),
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f), // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
                BotSlotSetup(null, null, 0f)  // AÇIK SLOT: Canlı Oyuncu Tedarikçisi Bekleniyor
            )
        )
    )

    /**
     * Tüm 20 Bot Konsorsiyumunun canlı, eksiksiz MegaProject nesnelerini üretir.
     */
    fun getDefaultBotMegaProjects(): List<MegaProject> {
        val allProducts = Product.values().toList()
        return BOT_DEFS.map { def ->
            buildMegaProjectFromDef(def, allProducts)
        }
    }

    /**
     * Tüm 20 Bot Konsorsiyumunun Supabase / Multiplayer için GuildGroup temsillerini üretir.
     * rawProjectJson alanı MegaProject'in eksiksiz JSON çıktısıyla doldurulur.
     */
    fun getDefaultBotGuilds(): List<GuildGroup> {
        val megaProjects = getDefaultBotMegaProjects()
        return BOT_DEFS.zip(megaProjects).map { (def, megaProject) ->
            buildGuildGroup(def, megaProject)
        }
    }

    /**
     * ID'ye göre tekil Bot MegaProject getirir.
     */
    fun getBotMegaProject(id: String): MegaProject? {
        val allProducts = Product.values().toList()
        val def = BOT_DEFS.find { it.id == id } ?: return null
        return buildMegaProjectFromDef(def, allProducts)
    }

    /**
     * ID'ye göre Bot GuildGroup getirir.
     */
    fun getBotGuild(id: String): GuildGroup? {
        val def = BOT_DEFS.find { it.id == id } ?: return null
        val allProducts = Product.values().toList()
        val proj = buildMegaProjectFromDef(def, allProducts)
        return buildGuildGroup(def, proj)
    }

    private fun buildMegaProjectFromDef(def: BotConsortiumDef, allProducts: List<Product>): MegaProject {
        val generatedSlots = MegaProjectFactory.getRequiredIngredientsForProduct(def.targetProductId, def.qualityTier, allProducts)
        val finalSlots = generatedSlots.mapIndexed { idx, slot ->
            val setup = def.slotSetups.getOrNull(idx) ?: BotSlotSetup(null, null, 0f)
            val reqQty = slot.quantityRequired
            val delQty = if (setup.botId != null) {
                (reqQty * setup.deliveryProgress).toInt().coerceIn(0, reqQty)
            } else 0

            val costPerUnit = if (reqQty > 0) slot.costContributionValue / reqQty else 1000L
            val actualDeliveredCost = delQty * costPerUnit

            slot.copy(
                assignedPartnerId = setup.botId,
                assignedPartnerName = setup.botName,
                quantityDelivered = delQty,
                actualCostIncurred = actualDeliveredCost,
                deliveredQualityTier = when (def.qualityTier) {
                    ConsortiumQualityTier.GRADE_A -> 4
                    ConsortiumQualityTier.GRADE_B -> 3
                    ConsortiumQualityTier.GRADE_C -> 2
                },
                deliveredQualityScore = when (def.qualityTier) {
                    ConsortiumQualityTier.GRADE_A -> 4.2
                    ConsortiumQualityTier.GRADE_B -> 3.1
                    ConsortiumQualityTier.GRADE_C -> 2.0
                }
            )
        }

        val totalCostSum = finalSlots.sumOf { it.costContributionValue }.coerceAtLeast(1L)
        val totalVal = (totalCostSum * 1.8f * def.qualityTier.borsaMultiplier).toLong()

        val baseBatchDuration = when (def.targetProductId) {
            "space_rocket" -> 450
            "fusion_reactor_core" -> 375
            "defense_frigate" -> 300
            "super_yacht" -> 300
            "cargo_ship" -> 275
            "bullet_train" -> 250
            "quantum_supercomputer" -> 250
            "smart_skyscraper" -> 250
            "hyperloop_capsule" -> 225
            "ai_datacenter" -> 225
            "satellite" -> 225
            "hydrogen_plant" -> 200
            "smart_grid" -> 200
            "autonomous_drone_swarm" -> 200
            "luxury_aircraft_interior" -> 200
            "ev" -> 200
            "uav" -> 200
            "turbine_engine" -> 180
            "chip" -> 180
            "nano_battery" -> 180
            else -> 225
        }
        val actualBatchDuration = (baseBatchDuration * def.qualityTier.durationMultiplier).toInt()

        return MegaProject(
            id = def.id,
            consortiumName = def.consortiumName,
            brandName = def.brandName,
            targetProductId = def.targetProductId,
            targetProductName = def.targetProductName,
            leaderPlayerId = "${def.id}_leader",
            leaderPlayerName = def.leaderName,
            slots = finalSlots,
            currentStage = def.currentStage,
            totalProjectValue = totalVal,
            brandReputationGain = (60 * def.qualityTier.multiplier).toInt(),
            brandMultiplier = 1.35f * def.qualityTier.borsaMultiplier,
            warehouseStock = def.warehouseStock,
            warehouseCapacity = if (def.qualityTier == ConsortiumQualityTier.GRADE_A) 1500 else 1000,
            batchProductionDurationSeconds = actualBatchDuration,
            qualityTier = def.qualityTier,
            cityId = def.cityId,
            totalItemsProduced = def.totalItemsProduced,
            isTestProductProduced = (def.currentStage >= MegaProjectStage.STAGE_3_TESTING),
            isMassProductionApproved = (def.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION),
            salesChannel = if (def.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION) ConsortiumSalesChannel.BORSA else ConsortiumSalesChannel.PAZAR
        )
    }

    private fun buildGuildGroup(def: BotConsortiumDef, megaProject: MegaProject): GuildGroup {
        val reqsMap = megaProject.slots.associate { it.productId to it.quantityRequired }
        val contribsMap = megaProject.slots.associate { it.productId to it.quantityDelivered }
        val rawJson = try {
            anyToJsonElement(megaProject.toMap()).toString()
        } catch (_: Exception) { "" }

        return GuildGroup(
            id = def.id,
            name = def.consortiumName,
            leaderName = def.leaderName,
            memberCount = def.memberCount,
            megaProjectTitle = def.targetProductName,
            megaProjectTarget = megaProject.totalProjectValue,
            megaProjectCurrent = megaProject.slots.sumOf { it.costContributionValue },
            megaProjectRequirements = reqsMap,
            megaProjectContributions = contribsMap,
            perkDescription = "Marka: ${def.brandName} • Marka Çarpanı: %${((megaProject.brandMultiplier - 1f) * 100).toInt().coerceAtLeast(0)}",
            bankBalance = (megaProject.warehouseStock.toLong() * megaProject.unitBatchPrice).coerceAtLeast(0L),
            isIpoActive = (def.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION),
            publicSharePercent = 20,
            targetProductName = def.targetProductName,
            targetProductId = def.targetProductId,
            warehouseStock = def.warehouseStock,
            totalItemsProduced = def.totalItemsProduced,
            unitBatchPrice = megaProject.unitBatchPrice,
            currentStage = def.currentStage.name,
            cityId = def.cityId,
            rawProjectJson = rawJson
        )
    }
}
