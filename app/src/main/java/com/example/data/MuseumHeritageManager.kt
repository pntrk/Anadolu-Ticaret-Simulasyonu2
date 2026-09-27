package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import com.example.data.network.AppJson
import com.example.data.network.MuseumRegistryItemDto
import com.example.data.network.MuseumAuctionItemDto
import com.example.data.network.anyToJsonElement
import com.example.data.network.jsonElementToAny
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.random.Random
import com.example.ui.theme.tr

enum class ArtifactRarity(val displayName: String, val colorHex: Long, val badgeEmoji: String) {
    HISTORIC("Tarihi Miras", 0xFF60A5FA, "📜"),
    RARE("Nadir Koleksiyon", 0xFF34D399, "✨"),
    EPIC("Efsanevi Yadigar", 0xFFA78BFA, "🔮"),
    MASTERPIECE("Müze Şaheseri", 0xFFF59E0B, "👑");

    fun getLocalizedName(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                HISTORIC -> "Historic Heritage"
                RARE -> "Rare Collection"
                EPIC -> "Legendary Relic"
                MASTERPIECE -> "Museum Masterpiece"
            }
        } else {
            this.displayName
        }
    }
}

data class AntiqueArtifact(
    val id: String,
    val name: String,
    val era: String,
    val originCity: String,
    val description: String,
    val rarity: ArtifactRarity,
    val baseValue: Long,
    val hourlyVisitorIncome: Long,
    val prestigeScore: Int,
    val iconEmoji: String,
    val historicalLore: String
) {
    val artifactId: String get() = id

    fun getLocalizedName(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (id) {
                "art_seljuk_gold_dinar" -> "12th Century Seljuk Gold Dinar"
                "art_ottoman_trade_ferman" -> "Ottoman Silk Road Trade Decree"
                "art_hereke_silk_carpet" -> "19th Century Hereke Palace Carpet"
                "art_hittite_sun_disc" -> "Early Bronze Age Hittite Sun Disc"
                "art_damascus_grand_sword" -> "Grand Vizier Damascus Steel Sword"
                "art_kutahya_tulip_vase" -> "Palace Work Kutahya Tulip Tile Vase"
                "art_piri_reis_chart" -> "Piri Reis Mediterranean Sea Navigation Chart"
                "art_ahievran_leather_anvil" -> "Ahi Evran's Leather Tool Anvil"
                "art_ahilik_seddi" -> "Mastery Ceremony Ahilik Belt (Peshtamal)"
                "art_divani_lugati_turk" -> "Divan-ı Lügati't-Türk Original Manuscript"
                "art_suleymaniye_defteri" -> "Suleymaniye Mosque Construction Book"
                "art_shahi_cannon_mold" -> "Fatih's Shahi Cannon Mold"
                "art_gobeklitepe_symbol" -> "Göbeklitepe T-Shaped Pillar Fragment"
                else -> name
            }
        } else name
    }

    fun getLocalizedEra(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (id) {
                "art_seljuk_gold_dinar" -> "Anatolian Seljuk Sultanate (1185)"
                "art_ottoman_trade_ferman" -> "Ottoman Empire (1574)"
                "art_hereke_silk_carpet" -> "Ottoman Palace Weaving (1888)"
                "art_hittite_sun_disc" -> "Hittite / Hatti Civilization (2000 BC)"
                "art_damascus_grand_sword" -> "Classical Period (1640)"
                "art_kutahya_tulip_vase" -> "Tulip Era (1720)"
                "art_piri_reis_chart" -> "Maritime Heritage (1513)"
                "art_ahievran_leather_anvil" -> "Anatolian Seljuk (13th Century)"
                "art_ahilik_seddi" -> "Ahilik Guild Period (14th Century)"
                "art_divani_lugati_turk" -> "Kara-Khanid Khanate (1072-1074)"
                "art_suleymaniye_defteri" -> "Classical Architecture Period (1557)"
                "art_shahi_cannon_mold" -> "Ottoman Industry (1453)"
                "art_gobeklitepe_symbol" -> "Neolithic Era (9600 BC)"
                else -> era
            }
        } else era
    }

    fun getLocalizedDescription(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (id) {
                "art_seljuk_gold_dinar" -> "A mint-sealed pure gold trade coin from the reign of Alaeddin Kayqubad I."
                "art_ottoman_trade_ferman" -> "An illuminated decree bearing the sultan's tughra, granting special customs exemptions to Bursa silk guilds."
                "art_hereke_silk_carpet" -> "A palace carpet woven with pure silk and gold-wrapped threads, featuring 100 double knots per square centimeter."
                "art_hittite_sun_disc" -> "A bronze cast sacred astronomical ritual disc belonging to the most ancient kingdoms of Anatolia."
                "art_damascus_grand_sword" -> "A ceremonial sword hand-forged by Gaziantep master craftsmen from genuine Damascus steel and adorned with rubies."
                "art_kutahya_tulip_vase" -> "A rare tile masterpiece decorated with cobalt blue and coral red underglaze techniques."
                "art_piri_reis_chart" -> "An original maritime chart drawn on gazelle skin, including wind roses and port coordinates."
                "art_ahievran_leather_anvil" -> "The sacred anvil of the tannery (leatherwork) profession, personally used by Ahi Evran-ı Veli, founder of the Ahilik organization."
                "art_ahilik_seddi" -> "A gold-embroidered silk belt tied at the 'Shed Kuşanma' (Girding of the Belt) ceremony organized for artisans promoted to master."
                "art_divani_lugati_turk" -> "The most unique linguistic treasure in world history, which is the first known dictionary and grammar book of the Turkish language."
                "art_suleymaniye_defteri" -> "A financial ledger showing material costs, worker wages, and stone quarry expenses used in the construction of Mimar Sinan's masterpiece."
                "art_shahi_cannon_mold" -> "A bronze mold of the greatest military innovation of its era, forged by the Hungarian master Urban and Ottoman engineers."
                "art_gobeklitepe_symbol" -> "A sacred monolith belonging to the ground zero of human history, featuring reliefs of foxes, bulls, and cranes."
                else -> description
            }
        } else description
    }

    fun getLocalizedOriginCity(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (originCity) {
                "Konya" -> "Konya"
                "Bursa" -> "Bursa"
                "Kocaeli" -> "Kocaeli"
                "Ankara" -> "Ankara"
                "Gaziantep" -> "Gaziantep"
                "Kütahya" -> "Kutahya"
                "Çanakkale" -> "Canakkale"
                "Kırşehir" -> "Kirsehir"
                "Kayseri" -> "Kayseri"
                "Bağdat (Kâşgarlı Mahmud)" -> "Baghdad (Mahmud of Kashgar)"
                "İstanbul" -> "Istanbul"
                "Edirne" -> "Edirne"
                "Şanlıurfa" -> "Sanliurfa"
                else -> originCity
            }
        } else originCity
    }

    fun getLocalizedHistoricalLore(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (id) {
                "art_seljuk_gold_dinar" -> "Used as a guarantee of trust among merchants during Silk Road caravan crossings through Anatolia, this gold dinar represents the superior metallurgical mastery of Seljuk mints."
                "art_ottoman_trade_ferman" -> "This historic decree, which monopolized the silk trade between Bursa and Istanbul, has reached the present day with gold-gilded illumination work and the sultan's tughra."
                "art_hereke_silk_carpet" -> "Baked and woven for palace protocols by master weavers at the Hereke Imperial Factory, this masterpiece is widely recognized as the peak of carpet art worldwide."
                "art_hittite_sun_disc" -> "Discovered in Alacahöyük excavations, this symbolic artifact is the most magnificent archaeological monument of Anatolia's thousands of years of trade and faith history dedicated to the sun and fertility."
                "art_damascus_grand_sword" -> "Forged using the wavy-patterned Damascus steel technology and equipped with rubies on its hilt, this sword is a symbol of both military and commercial power."
                "art_kutahya_tulip_vase" -> "Fired in the most brilliant era of Kutahya tile workshops, this vase has defied centuries with its vivid cobalt tones on its glazed surface."
                "art_piri_reis_chart" -> "This chart, in which Admiral Piri Reis depicted the Mediterranean and Gibraltar routes with millimeter calculations, is the most valuable cartographic treasure in world maritime trade history."
                "art_ahievran_leather_anvil" -> "This ancient anvil, where honest production and ethical trade (the foundations of the Ahilik philosophy) were born, is accepted as the most sacred relic of the Ahi guilds."
                "art_ahilik_seddi" -> "A physical symbol of the 'own your hand, your waist, and your tongue' philosophy, this belt represents the peak of merit, quality, and integrity in Ahilik culture."
                "art_divani_lugati_turk" -> "Written by Mahmud of Kashgar, this masterpiece is not just a dictionary, but also an encyclopedia of the trade, culture, geography, and social life of 11th-century Turkish tribes."
                "art_suleymaniye_defteri" -> "Reflecting the genius of the Ottoman economic and engineering management of the period, this book is the document of which logistics and supply networks managed tons of gold."
                "art_shahi_cannon_mold" -> "The symbolic cast piece of industrial military production and technological superiority that opened and closed eras during the conquest of Istanbul."
                "art_gobeklitepe_symbol" -> "As evidence of the transition to settled life and the creation of humanity's first known temple, Göbeklitepe is the most shocking discovery in the world of archaeology."
                else -> historicalLore
            }
        } else historicalLore
    }
}

data class LiveAuctionState(
    val artifactId: String,
    val currentHighestBid: Long,
    val currentHighestBidder: String,
    val buyoutPrice: Long,
    val endsAtMs: Long,
    val bidCount: Int
)

data class MuseumArtifactRegistryItem(
    val artifactId: String,
    val ownerId: String?,
    val ownerName: String,
    val status: String, // "OWNED_BY_PLAYER", "ON_AUCTION", "UNCLAIMED_TREASURY"
    val activeAuctionId: String? = null,
    val lastPrice: Long = 0L,
    val certificateCode: String = "KLT-VKF-001",
    val mintTotal: Int = 1,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class MuseumAuctionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val artifactId: String,
    val sellerId: String = "local_player",
    val sellerName: String,
    val isPlayerSeller: Boolean,
    val startingBid: Long,
    val currentHighestBid: Long,
    val currentHighestBidderId: String = "",
    val currentHighestBidder: String = "",
    val buyoutPrice: Long,
    val endsAtMs: Long,
    val bidCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false,
    val lastBidTimeMs: Long = System.currentTimeMillis()
)

object MuseumHeritageManager {
    private const val PREFS_NAME = "heritage_museum_prefs"
    private const val KEY_OWNED_ARTIFACTS = "owned_artifact_ids"
    private const val KEY_LAST_CLAIM_TIME = "last_ticket_income_claim"
    private const val KEY_ACTIVE_AUCTION_ID = "active_auction_id"
    private const val KEY_AUCTION_CURRENT_BID = "auction_current_bid"
    private const val KEY_AUCTION_HIGHEST_BIDDER = "auction_highest_bidder"
    private const val KEY_AUCTION_ENDS_AT = "auction_ends_at"
    private const val KEY_AUCTION_BID_COUNT = "auction_bid_count"
    private const val KEY_MUSEUM_AUCTIONS_LIST = "museum_auctions_list_v2"
    private const val KEY_PROFILE_VISIT_REVENUE = "profile_visit_ticket_revenue"
    private const val KEY_TOTAL_PROFILE_INSPECTIONS = "total_profile_inspections"
    private const val KEY_GLOBAL_REGISTRY_JSON = "museum_global_registry_v2"
    private const val KEY_UNIQUE_PLAYER_UID = "museum_unique_player_uid"

    private var storedContext: Context? = null

    fun registerContext(context: Context) {
        storedContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("was_reset_for_supabase_wipe_v1", false)) {
            prefs.edit()
                .clear()
                .putBoolean("was_reset_for_supabase_wipe_v1", true)
                .apply()
        }
    }

    val allArtifacts = listOf(
        AntiqueArtifact(
            id = "art_seljuk_gold_dinar",
            name = "12. Yüzyıl Selçuklu Altın Dinarı",
            era = "Anadolu Selçuklu Devleti (1185)",
            originCity = "Konya",
            description = "I. Alaeddin Keykubad dönemine ait, darphane mühürlü saf altın ticaret sikkesi.",
            rarity = ArtifactRarity.RARE,
            baseValue = 120_000_00000L,
            hourlyVisitorIncome = 450_000L,
            prestigeScore = 850,
            iconEmoji = "🪙",
            historicalLore = "İpek Yolu kervanlarının Anadolu geçişlerinde tüccarlar arasında güven teminatı olarak kullanılan bu altın dinar, Selçuklu darphanelerinin üstün metalurji ustalığını temsil eder."
        ),
        AntiqueArtifact(
            id = "art_ottoman_trade_ferman",
            name = "Osmanlı İpek Yolu Ticaret Fermanı",
            era = "Osmanlı İmparatorluğu (1574)",
            originCity = "Bursa",
            description = "Sultan tuğralı, Bursa ipek loncalarına özel gümrük muafiyeti tanıyan tezhip süslemeli ferman.",
            rarity = ArtifactRarity.EPIC,
            baseValue = 280_000_00000L,
            hourlyVisitorIncome = 1_200_000L,
            prestigeScore = 1800,
            iconEmoji = "📜",
            historicalLore = "Bursa ve İstanbul arasındaki ipek ticaretini tekel altına alan Ahilik loncalarına verilen bu tarihi ferman, altın yaldızlı tezhip işlemeleri ve padişah tuğrasıyla günümüze ulaşmıştır."
        ),
        AntiqueArtifact(
            id = "art_hereke_silk_carpet",
            name = "19. Yüzyıl Hereke Saray Halısı",
            era = "Osmanlı Saray Dokuması (1888)",
            originCity = "Kocaeli",
            description = "Santimetrekareye 100 çift düğüm düşen, saf ipek ve altın sim ipliklerle dokunmuş saray halısı.",
            rarity = ArtifactRarity.HISTORIC,
            baseValue = 95_000_00000L,
            hourlyVisitorIncome = 380_000L,
            prestigeScore = 720,
            iconEmoji = "🧶",
            historicalLore = "Hereke Dokumahane-i Hümayunu'nda saray protokolleri için özel üretilen bu halı, dünyadaki halıcılık sanatının zirve noktası kabul edilmektedir."
        ),
        AntiqueArtifact(
            id = "art_hittite_sun_disc",
            name = "Erken Tunç Çağı Hitit Güneş Kursu",
            era = "Hitit / Hatti Uygarlığı (MÖ 2000)",
            originCity = "Ankara",
            description = "Anadolu'nun en köklü krallıklarına ait, bronz döküm kutsal astronomik tören sembolü.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 750_000_00000L,
            hourlyVisitorIncome = 3_500_000L,
            prestigeScore = 4500,
            iconEmoji = "☀️",
            historicalLore = "Alacahöyük kazılarında bulunan bu simgesel eser, Anadolu'nun binlerce yıllık ticaret ve inanç tarihinin güneşe ve berekete adanmış en görkemli arkeolojik anıtıdır."
        ),
        AntiqueArtifact(
            id = "art_damascus_grand_sword",
            name = "Sadrazam Şam Çeliği (Damascus) Kılıcı",
            era = "Klasik Dönem (1640)",
            originCity = "Gaziantep",
            description = "Gaziantep ustalarının ürettiği, hakiki Şam çeliğinden el dövmesi, yakut kakmalı tören kılıcı.",
            rarity = ArtifactRarity.EPIC,
            baseValue = 340_000_00000L,
            hourlyVisitorIncome = 1_450_000L,
            prestigeScore = 2100,
            iconEmoji = "⚔️",
            historicalLore = "Dalgalı desenli Damascus çelik teknolojisiyle dövülen ve kabzası yakutlarla donatılan bu kılıç, hem askeri hem de ticari nüfuzun sembolüdür."
        ),
        AntiqueArtifact(
            id = "art_kutahya_tulip_vase",
            name = "Saray İşi Kütahya Lale Çini Vazosu",
            era = "Lale Devri (1720)",
            originCity = "Kütahya",
            description = "Kobalt mavisi ve mercan kırmızısı sır altı tekniğiyle bezenmiş nadide çini şaheseri.",
            rarity = ArtifactRarity.RARE,
            baseValue = 140_000_00000L,
            hourlyVisitorIncome = 520_000L,
            prestigeScore = 950,
            iconEmoji = "🏺",
            historicalLore = "Kütahya çini atölyelerinin en parlak döneminde fırınlanan bu vazo, sırlı yüzeyindeki canlı kobalt tonlarıyla yüzyıllara meydan okumuştur."
        ),
        AntiqueArtifact(
            id = "art_piri_reis_chart",
            name = "Piri Reis Akdeniz Seyir Haritası",
            era = "Denizcilik Mirası (1513)",
            originCity = "Çanakkale",
            description = "Ceylan derisi üzerine çizilmiş, rüzgar gülü ve liman koordinatlarını içeren orijinal harita parçası.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 890_000_00000L,
            hourlyVisitorIncome = 4_200_000L,
            prestigeScore = 5200,
            iconEmoji = "🗺️",
            historicalLore = "Piri Reis'in Akdeniz ve Cebelitarık rotalarını milimetrik hesaplarla çizdiği bu harita, dünya deniz ticareti tarihinin en değerli kartografya hazinesidir."
        ),
        AntiqueArtifact(
            id = "art_ahievran_leather_anvil",
            name = "Ahi Evran Deri Örsü",
            era = "Anadolu Selçuklu (13. Yüzyıl)",
            originCity = "Kırşehir",
            description = "Ahilik teşkilatının kurucusu Ahi Evran-ı Veli'nin bizzat kullandığı, debbağlık mesleğinin kutsal örsü.",
            rarity = ArtifactRarity.HISTORIC,
            baseValue = 85_000_00000L,
            hourlyVisitorIncome = 320_000L,
            prestigeScore = 650,
            iconEmoji = "⚒️",
            historicalLore = "Ahilik felsefesinin dürüst üretim ve ahlaklı ticaret ilkelerinin doğduğu bu kadim örs, Ahi loncalarının en kutsal yadigarı sayılır."
        ),
        AntiqueArtifact(
            id = "art_ahilik_seddi",
            name = "Ahi Usta Şed Kuşanma Kemeri",
            era = "Ahilik Loncaları (14. Yüzyıl)",
            originCity = "Kırşehir",
            description = "Kalfalıktan ustalığa geçen zanaatkarlara düzenlenen törende bağlanan, altın işlemeli ipek şed.",
            rarity = ArtifactRarity.RARE,
            baseValue = 110_000_00000L,
            hourlyVisitorIncome = 410_000L,
            prestigeScore = 800,
            iconEmoji = "🎗️",
            historicalLore = "Ahilik kültüründe 'eline, beline, diline sahip ol' felsefesinin somut sembolü olan bu kemir, zanaat dünyasında liyakatin en yüksek tescilidir."
        ),
        AntiqueArtifact(
            id = "art_divani_lugati_turk",
            name = "Divan-ı Lügati't-Türk Özgün El Yazması",
            era = "Karahanlılar Dönemi (1072-1074)",
            originCity = "Bağdat (Kâşgarlı Mahmud)",
            description = "Türk dilinin ve kültürünün ilk sözlüğü, dünya haritası içeren en büyük dil hazinesi.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 1_200_000_00000L,
            hourlyVisitorIncome = 5_800_000L,
            prestigeScore = 7500,
            iconEmoji = "📖",
            historicalLore = "Kâşgarlı Mahmud tarafından yazılan bu başyapıt, yalnızca bir sözlük değil, 11. yüzyıl Türk boylarının ticaret, kültür ve coğrafyasının tek ansiklopedisidir."
        ),
        AntiqueArtifact(
            id = "art_suleymaniye_defteri",
            name = "Süleymaniye Camii İnşaat Şantiye Defteri",
            era = "Klasik Mimari Dönemi (1557)",
            originCity = "İstanbul",
            description = "Mimar Sinan'ın başyapıtında kullanılan taş, amele ve malzeme harcamalarının tutulduğu resmi muhasebe defteri.",
            rarity = ArtifactRarity.EPIC,
            baseValue = 420_000_00000L,
            hourlyVisitorIncome = 1_900_000L,
            prestigeScore = 2600,
            iconEmoji = "📐",
            historicalLore = "Dönemin Osmanlı iktisat ve mühendislik yönetiminin dehasını yansıtan bu defter, tonlarca altının hangi lojistik ve tedarik ağlarıyla yönetildiğinin belgesidir."
        ),
        AntiqueArtifact(
            id = "art_shahi_cannon_mold",
            name = "Fatih Şahi Topu Bronz Döküm Kalıbı",
            era = "Osmanlı Sanayii (1453)",
            originCity = "Edirne",
            description = "İstanbul'un fethinde surları yıkan devasa Şahi toplarının dökümünde kullanılan bronz kalıp parçası.",
            rarity = ArtifactRarity.EPIC,
            baseValue = 510_000_00000L,
            hourlyVisitorIncome = 2_200_000L,
            prestigeScore = 3100,
            iconEmoji = "💣",
            historicalLore = "Macar Urban Usta ve Osmanlı mühendislerinin ortak çabasıyla Edirne dökümhanesinde hazırlanan, çağ kapatıp çağ açan teknolojik üstünlüğün sembolüdür."
        ),
        AntiqueArtifact(
            id = "art_gobeklitepe_symbol",
            name = "Göbeklitepe T-Biçimli Dikilitaş Parçası",
            era = "Cilalı Taş Çağı (MÖ 9600)",
            originCity = "Şanlıurfa",
            description = "İnsanlık tarihinin sıfır noktasında inşaa edilmiş, kabartma hayvan figürlü kutsal mabet taşı.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 1_500_000_00000L,
            hourlyVisitorIncome = 7_000_000L,
            prestigeScore = 9000,
            iconEmoji = "🗿",
            historicalLore = "Tarım öncesi avcı-toplayıcı toplulukların inanç merkezi olarak inşa ettiği Göbeklitepe, dünya arkeolojisindeki tüm ezberleri bozan en eski mabet parçasıdır."
        ),
        AntiqueArtifact(
            id = "art_kultepe_clay_tablet",
            name = "Kültepe Asur Ticaret Tableti",
            era = "Eski Asur Ticaret Kolonileri (MÖ 1950)",
            originCity = "Kayseri",
            description = "Anadolu'nun ilk yazılı belgesi; kervan borçları, gümüş faizleri ve kâr paylarını belgeleyen çivi yazılı kil tablet.",
            rarity = ArtifactRarity.HISTORIC,
            baseValue = 160_000_00000L,
            hourlyVisitorIncome = 580_000L,
            prestigeScore = 1100,
            iconEmoji = "🏺",
            historicalLore = "Kaniş Karumu'nda bulunan bu tablet, Anadolu'da serbest piyasa ve sözleşme hukukunun en kadim köküdür."
        ),
        AntiqueArtifact(
            id = "art_cezeri_elephant_clock",
            name = "El-Cezeri Mekanik Fil Saati Parçası",
            era = "Artuklu Mühendislik Çağı (1206)",
            originCity = "Diyarbakır",
            description = "Sibernetiğin babası Cezeri'nin su gücüyle çalışan efsanevi otomat fil saatinin bronz dişli mekanizması.",
            rarity = ArtifactRarity.EPIC,
            baseValue = 480_000_00000L,
            hourlyVisitorIncome = 2_100_000L,
            prestigeScore = 2900,
            iconEmoji = "🐘",
            historicalLore = "Mekanik saat ve otomasyon mühendisliğinin dünyadaki ilk zirve örneği olan bu şaheser, İslam altın çağının bilim mirasıdır."
        ),
        AntiqueArtifact(
            id = "art_galata_bankers_ledger",
            name = "Galata Bankerleri Kredi Defteri",
            era = "Tanzimat Finans Dönemi (1860)",
            originCity = "İstanbul",
            description = "Havyar Hanı bankerlerinin Osmanlı borçlanma senetlerini ve mevduat faizlerini kaydettiği altın yaldızlı deri defter.",
            rarity = ArtifactRarity.RARE,
            baseValue = 190_000_00000L,
            hourlyVisitorIncome = 690_000L,
            prestigeScore = 1250,
            iconEmoji = "📖",
            historicalLore = "Galata rıhtımında Avrupa ve Doğu sermayesinin kesiştiği modern bankacılığın ve borsa spekülasyonlarının ilk kayıt defteridir."
        ),
        AntiqueArtifact(
            id = "art_ulug_bey_astrolabe",
            name = "Uluğ Bey Semerkand Usturlabı",
            era = "Timur Rönesansı (1428)",
            originCity = "Semerkand (Anadolu Mirası)",
            description = "Yıldız haritaları ve enlem-boylam hesapları için pirinçten oyulmuş milimetrik astronomik gözlem cihazı.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 950_000_00000L,
            hourlyVisitorIncome = 4_600_000L,
            prestigeScore = 6000,
            iconEmoji = "🔭",
            historicalLore = "Zîc-i Uluğ Bey kataloğunun hazırlanmasında kullanılan bu usturlap, Doğu'nun gök bilimindeki eşsiz dehasını temsil eder."
        ),
        AntiqueArtifact(
            id = "art_mimar_sinan_compass",
            name = "Mimar Sinan Pergel-i Mimari",
            era = "Klasik Osmanlı Mimarisi (1560)",
            originCity = "İstanbul",
            description = "Koca Sinan'ın Selimiye ve Süleymaniye kubbelerinin statik dengesini çizdiği pirinç ve çelik usta pergeli.",
            rarity = ArtifactRarity.EPIC,
            baseValue = 540_000_00000L,
            hourlyVisitorIncome = 2_400_000L,
            prestigeScore = 3400,
            iconEmoji = "📐",
            historicalLore = "Yüzyıllardır dimdik ayakta duran kubbelerin harcındaki altın oran ve matematik dehası bu pergelin ucundan doğmuştur."
        ),
        AntiqueArtifact(
            id = "art_barbaros_compass",
            name = "Barbaros Hayreddin Akdeniz Pusulası",
            era = "Preveze Zaferi Dönemi (1538)",
            originCity = "İstanbul",
            description = "Kaptan-ı Derya Barbaros'un kadırgasında Akdeniz'e hükmederken yön bulduğu bronz mahfazalı denizci pusulası.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 820_000_00000L,
            hourlyVisitorIncome = 3_900_000L,
            prestigeScore = 4900,
            iconEmoji = "🧭",
            historicalLore = "Akdeniz'i bir Türk gölüne dönüştüren deniz zaferlerinin, açık deniz lojistiğinin ve cesaretin somut anıtıdır."
        ),
        AntiqueArtifact(
            id = "art_troy_gold_diadem",
            name = "Truva Kraliyet Altın Diyademi",
            era = "Homeros / Troya II Dönemi (MÖ 2500)",
            originCity = "Çanakkale",
            description = "Saf altından binlerce yaprak ve halka zincirle işlenmiş, efsanevi Troya hazinesinin en görkemli tacı.",
            rarity = ArtifactRarity.MASTERPIECE,
            baseValue = 1_350_000_00000L,
            hourlyVisitorIncome = 6_400_000L,
            prestigeScore = 8200,
            iconEmoji = "👑",
            historicalLore = "İlyada destanına konu olan Çanakkale Boğazı'nın kadim ticaret ve deniz hakimiyeti gücünü simgeleyen paha biçilmez altın taçtır."
        )
    )

    suspend fun seedMissingArtifactsToSupabase() {
        val allBuffs = ArtifactBuffRegistry.buffs.map { it.artifactId }
        val currentArtifacts = SupabaseManager.fetchMuseumArtifactsFromSupabase() // Mevcut eserleri çeken Supabase çağrısı
        val currentIds = currentArtifacts.map { it.artifactId }
        
        val missingIds = allBuffs.filterNot { currentIds.contains(it) }
        
        missingIds.forEach { missingId ->
            val newArtifactName = missingId.replace("art_", "").replace("_", " ").uppercase()
            val newArtifact = MuseumArtifactOwnershipEntity(
                artifactId = missingId,
                ownerId = null,
                ownerName = "Hazine-i Amire",
                status = "UNCLAIMED_TREASURY",
                activeAuctionId = null,
                lastPrice = 250000L,
                updatedAtMs = System.currentTimeMillis()
            )
            SupabaseManager.syncMuseumArtifactToSupabase(newArtifact)
        }
    }

    fun getUniquePlayerId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uid = prefs.getString(KEY_UNIQUE_PLAYER_UID, null)
        if (uid.isNullOrBlank()) {
            val randomHex = java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            uid = "TR-VKF-$randomHex"
            prefs.edit().putString(KEY_UNIQUE_PLAYER_UID, uid).apply()
        }
        return uid
    }

    fun getArtifactCertificateCode(artifactId: String): String {
        val index = allArtifacts.indexOfFirst { it.id == artifactId }
        val num = if (index >= 0) String.format("%03d", index + 1) else "001"
        return "KLT-VKF-$num"
    }

    fun isPlayerBannedFromArtifact(context: Context, artifactId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("banned_artifact_$artifactId", false)
    }

    fun isLocalPlayer(context: Context, id: String?, onlineUid: String = ""): Boolean {
        if (id == null) return false
        if (id == "local_player" || id == "local" || id == "player" || id == "Siz" || id == "Holding") return true
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val myUid = getUniquePlayerId(context)
        if (id == myUid) return true
        
        val cachedUid = prefs.getString("last_online_uid", null)
        if (cachedUid != null && id == cachedUid) return true

        val cachedTrueUid = prefs.getString("last_true_online_uid", null)
        if (cachedTrueUid != null && id == cachedTrueUid) return true

        val cachedPlayerName = prefs.getString("last_player_name", null)
        if (cachedPlayerName != null && id.equals(cachedPlayerName, ignoreCase = true)) return true
        
        if (onlineUid.isNotBlank() && (id == onlineUid || id == onlineUid.replace(".", "_"))) {
            prefs.edit().putString("last_true_online_uid", onlineUid).apply()
            return true
        }
        
        return false
    }

    private fun getSafeLong(prefs: SharedPreferences, key: String, default: Long): Long {
        return try {
            prefs.getLong(key, default)
        } catch (e: ClassCastException) {
            try {
                prefs.getInt(key, default.toInt()).toLong()
            } catch (e2: Exception) {
                default
            }
        }
    }

    private fun getSafeInt(prefs: SharedPreferences, key: String, default: Int): Int {
        return try {
            prefs.getInt(key, default)
        } catch (e: ClassCastException) {
            try {
                prefs.getLong(key, default.toLong()).toInt()
            } catch (e2: Exception) {
                default
            }
        }
    }

    suspend fun getGlobalRegistry(context: Context): Map<String, MuseumArtifactRegistryItem> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ownedLocal = getOwnedArtifactIds(context)
        val jsonStr = prefs.getString(KEY_GLOBAL_REGISTRY_JSON, "") ?: ""
        val registry = mutableMapOf<String, MuseumArtifactRegistryItem>()
        val myUid = getUniquePlayerId(context)

        // Read from Room Database for strict 1-of-1 uniqueness
        try {
            val dao = AppDatabase.getDatabase(context).gameDao()
            val roomEntities = dao.getAllMuseumArtifacts()
            for (entity in roomEntities) {
                val certCode = getArtifactCertificateCode(entity.artifactId)
                registry[entity.artifactId] = MuseumArtifactRegistryItem(
                    artifactId = entity.artifactId,
                    ownerId = entity.ownerId?.ifBlank { null },
                    ownerName = entity.ownerName,
                    status = entity.status,
                    activeAuctionId = entity.activeAuctionId?.ifBlank { null },
                    lastPrice = entity.lastPrice,
                    certificateCode = certCode,
                    mintTotal = 1,
                    updatedAtMs = entity.updatedAtMs
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MuseumHeritageManager", "Error reading registry from Room", e)
        }

        // Fallback to SharedPreferences if Room is empty
        if (registry.isEmpty() && !jsonStr.isNullOrBlank()) {
            try {
                val dtos = AppJson.decodeFromString<List<MuseumRegistryItemDto>>(jsonStr)
                for (dto in dtos) {
                    val id = dto.artifactId
                    val rawOwnerId = dto.ownerId?.ifBlank { null }
                    val certCode = dto.certificateCode.ifBlank { getArtifactCertificateCode(id) }
                    registry[id] = MuseumArtifactRegistryItem(
                        artifactId = id,
                        ownerId = rawOwnerId,
                        ownerName = dto.ownerName,
                        status = dto.status,
                        activeAuctionId = dto.activeAuctionId?.ifBlank { null },
                        lastPrice = dto.lastPrice,
                        certificateCode = certCode,
                        mintTotal = 1,
                        updatedAtMs = dto.updatedAtMs
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MuseumHeritageManager", "Error parsing registry JSON", e)
            }
        }

        // Ensure all artifacts exist in the registry with strict 1-of-1 uniqueness
        for (artifact in allArtifacts) {
            val certCode = getArtifactCertificateCode(artifact.id)
            if (!registry.containsKey(artifact.id)) {
                val isLocalOwned = artifact.id in ownedLocal
                val item = MuseumArtifactRegistryItem(
                    artifactId = artifact.id,
                    ownerId = if (isLocalOwned) myUid else null,
                    ownerName = if (isLocalOwned) "Siz (Holding)" else "T.C. Kültür ve Turizm Bakanlığı (Vakıflar Gn. Md.)",
                    status = if (isLocalOwned) "OWNED_BY_PLAYER" else "UNCLAIMED_TREASURY",
                    activeAuctionId = null,
                    lastPrice = artifact.baseValue,
                    certificateCode = certCode,
                    mintTotal = 1,
                    updatedAtMs = System.currentTimeMillis()
                )
                registry[artifact.id] = item
            } else if (artifact.id in ownedLocal) {
                val existing = registry[artifact.id]!!
                if (existing.ownerId != myUid && existing.ownerId != "local_player") {
                    registry[artifact.id] = existing.copy(
                        ownerId = myUid,
                        ownerName = "Siz (Holding)",
                        status = "OWNED_BY_PLAYER",
                        certificateCode = certCode,
                        mintTotal = 1
                    )
                }
            }
        }

        return@withContext registry
    }

    private suspend fun saveGlobalRegistry(context: Context, registry: Map<String, MuseumArtifactRegistryItem>) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dtos = registry.values.map { item ->
            MuseumRegistryItemDto(
                artifactId = item.artifactId,
                ownerId = item.ownerId,
                ownerName = item.ownerName,
                status = item.status,
                activeAuctionId = item.activeAuctionId,
                lastPrice = item.lastPrice,
                certificateCode = item.certificateCode,
                mintTotal = 1,
                updatedAtMs = item.updatedAtMs
            )
        }

        val jsonStr = AppJson.encodeToString(dtos)
        prefs.edit().putString(KEY_GLOBAL_REGISTRY_JSON, jsonStr).apply()

        // Persist to Room
        try {
            val dao = AppDatabase.getDatabase(context).gameDao()
            val entities = registry.values.map {
                MuseumArtifactOwnershipEntity(
                    artifactId = it.artifactId,
                    ownerId = it.ownerId,
                    ownerName = it.ownerName,
                    status = it.status,
                    activeAuctionId = it.activeAuctionId,
                    lastPrice = it.lastPrice,
                    updatedAtMs = it.updatedAtMs
                )
            }
            dao.insertAllMuseumArtifacts(entities)
        } catch (e: Exception) {
            android.util.Log.e("MuseumHeritageManager", "Room sync error", e)
        }
    }

    fun getOwnedArtifactIds(context: Context? = storedContext): Set<String> {
        val targetContext = context ?: storedContext ?: return emptySet()
        val prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            prefs.getStringSet(KEY_OWNED_ARTIFACTS, emptySet()) ?: emptySet()
        } catch (e: Exception) {
            val badString = prefs.getString(KEY_OWNED_ARTIFACTS, "")
            val recoveredSet = if (!badString.isNullOrBlank()) {
                val cleanStr = badString.removePrefix("[").removeSuffix("]")
                if (cleanStr.isNotBlank()) {
                    cleanStr.split(",").map { it.trim().removeSurrounding("\"") }.toSet()
                } else emptySet()
            } else emptySet()

            prefs.edit().remove(KEY_OWNED_ARTIFACTS).putStringSet(KEY_OWNED_ARTIFACTS, recoveredSet).apply()
            recoveredSet
        }
    }

    fun getOwnedArtifacts(context: Context? = storedContext): List<AntiqueArtifact> {
        val owned = getOwnedArtifactIds(context)
        return allArtifacts.filter { it.id in owned }
    }

    fun isArtifactOwned(context: Context, artifactId: String): Boolean {
        return artifactId in getOwnedArtifactIds(context)
    }

    suspend fun addArtifactToMuseum(
        context: Context,
        artifactId: String,
        ownerName: String = "Siz",
        pricePaid: Long = 0L,
        customOwnerId: String? = null
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val owned = getOwnedArtifactIds(context).toMutableSet()
        owned.add(artifactId)
        prefs.edit().putStringSet(KEY_OWNED_ARTIFACTS, owned).apply()

        val myUid = customOwnerId ?: getUniquePlayerId(context)
        val registry = getGlobalRegistry(context).toMutableMap()
        val certCode = getArtifactCertificateCode(artifactId)

        registry[artifactId] = MuseumArtifactRegistryItem(
            artifactId = artifactId,
            ownerId = myUid,
            ownerName = ownerName,
            status = "OWNED_BY_PLAYER",
            activeAuctionId = null,
            lastPrice = pricePaid,
            certificateCode = certCode,
            mintTotal = 1,
            updatedAtMs = System.currentTimeMillis()
        )

        saveGlobalRegistry(context, registry)
    }

    fun removeArtifactFromMuseum(context: Context, artifactId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val owned = getOwnedArtifactIds(context).toMutableSet()
        owned.remove(artifactId)
        prefs.edit().putStringSet(KEY_OWNED_ARTIFACTS, owned).apply()
    }

    fun getTotalMuseumPrestige(context: Context): Int {
        return getOwnedArtifacts(context).sumOf { it.prestigeScore }
    }

    fun getTotalHourlyVisitorIncome(context: Context): Long {
        return getOwnedArtifacts(context).sumOf { it.hourlyVisitorIncome }
    }

    fun getUnclaimedProfileVisitRevenue(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return getSafeLong(prefs, KEY_PROFILE_VISIT_REVENUE, 0L)
    }

    fun getTotalProfileInspections(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return getSafeInt(prefs, KEY_TOTAL_PROFILE_INSPECTIONS, 0)
    }

    fun recordProfileInspection(context: Context, visitorName: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prestige = getTotalMuseumPrestige(context)
        val currentRevenue = getSafeLong(prefs, KEY_PROFILE_VISIT_REVENUE, 0L)
        val currentInspections = getSafeInt(prefs, KEY_TOTAL_PROFILE_INSPECTIONS, 0)

        val ticketPrice = (prestige * 5000L).coerceAtLeast(25_000L)
        val newRevenue = currentRevenue + ticketPrice
        val newInspections = currentInspections + 1

        prefs.edit()
            .putLong(KEY_PROFILE_VISIT_REVENUE, newRevenue)
            .putInt(KEY_TOTAL_PROFILE_INSPECTIONS, newInspections)
            .apply()

        return ticketPrice
    }

    fun calculateUnclaimedVisitorRevenue(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastClaim = getSafeLong(prefs, KEY_LAST_CLAIM_TIME, System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val elapsedHours = ((now - lastClaim) / 3600_000L).coerceIn(0L, 24L)

        val hourly = getTotalHourlyVisitorIncome(context)
        val ticketRev = getUnclaimedProfileVisitRevenue(context)

        return (elapsedHours * hourly) + ticketRev
    }

    fun claimVisitorRevenue(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val amount = calculateUnclaimedVisitorRevenue(context)

        prefs.edit()
            .putLong(KEY_LAST_CLAIM_TIME, System.currentTimeMillis())
            .putLong(KEY_PROFILE_VISIT_REVENUE, 0L)
            .apply()

        return amount
    }

    suspend fun getArtifactsForOnlinePlayer(context: Context, playerId: String): List<AntiqueArtifact> = withContext(Dispatchers.IO) {
        val registry = getGlobalRegistry(context)
        val ownedIds = registry.values
            .filter { it.ownerId == playerId && it.status == "OWNED_BY_PLAYER" }
            .map { it.artifactId }
            .toSet()

        return@withContext allArtifacts.filter { it.id in ownedIds }
    }

    fun getArtifactsForOnlinePlayer(playerId: String, level: Int = 1): List<AntiqueArtifact> {
        val hash = (playerId.hashCode() and 0x7FFFFFFF) + level
        val count = (hash % 3) + 1
        return allArtifacts.shuffled(Random(hash.toLong())).take(count)
    }

    private fun serializeAuctionList(list: List<MuseumAuctionItem>): String {
        return try {
            val dtos = list.map { item ->
                MuseumAuctionItemDto(
                    id = item.id,
                    artifactId = item.artifactId,
                    sellerId = item.sellerId,
                    sellerName = item.sellerName,
                    isPlayerSeller = item.isPlayerSeller,
                    startingBid = item.startingBid,
                    currentHighestBid = item.currentHighestBid,
                    currentHighestBidderId = item.currentHighestBidderId,
                    currentHighestBidder = item.currentHighestBidder,
                    buyoutPrice = item.buyoutPrice,
                    endsAtMs = item.endsAtMs,
                    bidCount = item.bidCount,
                    createdAt = item.createdAt,
                    isSettled = item.isSettled,
                    lastBidTimeMs = item.lastBidTimeMs
                )
            }
            AppJson.encodeToString(dtos)
        } catch (e: Exception) {
            "[]"
        }
    }

    private fun deserializeAuctionList(jsonStr: String): MutableList<MuseumAuctionItem> {
        if (jsonStr.isBlank()) return mutableListOf()
        return try {
            val dtos = AppJson.decodeFromString<List<MuseumAuctionItemDto>>(jsonStr)
            dtos.map { dto ->
                MuseumAuctionItem(
                    id = dto.id,
                    artifactId = dto.artifactId,
                    sellerId = dto.sellerId,
                    sellerName = dto.sellerName,
                    isPlayerSeller = dto.isPlayerSeller,
                    startingBid = dto.startingBid,
                    currentHighestBid = dto.currentHighestBid,
                    currentHighestBidderId = dto.currentHighestBidderId,
                    currentHighestBidder = dto.currentHighestBidder,
                    buyoutPrice = dto.buyoutPrice,
                    endsAtMs = dto.endsAtMs,
                    bidCount = dto.bidCount,
                    createdAt = dto.createdAt,
                    isSettled = dto.isSettled,
                    lastBidTimeMs = dto.lastBidTimeMs
                )
            }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    suspend fun getAllMuseumAuctions(context: Context, playerName: String = "Siz", onlineUid: String = ""): List<MuseumAuctionItem> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Cache current player IDs for synchronous isLocalPlayer checks
        val dao = com.example.data.AppDatabase.getDatabase(context).gameDao()
        val p = dao.getPlayer().firstOrNull()
        if (p != null) {
            prefs.edit()
                .putString("last_online_uid", p.id)
                .putString("last_player_name", p.name)
                .apply()
        }
        if (onlineUid.isNotBlank()) {
            prefs.edit().putString("last_true_online_uid", onlineUid).apply()
        }
        
        val jsonStr = prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: ""
        var list = deserializeAuctionList(jsonStr)

        val registry = getGlobalRegistry(context).toMutableMap()
        val myUid = getUniquePlayerId(context)
        val now = System.currentTimeMillis()
        var modified = false

        // 1. Check and settle expired auctions
        val remainingList = mutableListOf<MuseumAuctionItem>()
        for (auction in list) {
            if (!auction.isSettled && auction.endsAtMs <= now) {
                if (auction.bidCount > 0 && (auction.currentHighestBidderId.isNotBlank() || auction.currentHighestBidder.isNotBlank())) {
                    val winnerId = if (auction.currentHighestBidderId.isNotBlank()) auction.currentHighestBidderId else myUid
                    val winnerName = auction.currentHighestBidder.ifBlank { "Koleksiyoner" }
                    val finalPrice = auction.currentHighestBid
                    val artId = auction.artifactId
                    val artifactObj = allArtifacts.find { it.id == artId }
                    val artifactName = artifactObj?.name ?: artId
                    
                    val isWinnerLocal = isLocalPlayer(context, winnerId, onlineUid) ||
                            (p != null && (winnerId == p.id || winnerName.equals(p.name, ignoreCase = true))) ||
                            winnerId == "local_player" ||
                            winnerName.equals("Siz", ignoreCase = true)
                    
                    if (isWinnerLocal) {
                        // Winner is local player! Award artifact & deduct cash
                        if (p != null) {
                            val newMoney = (p.money - finalPrice).coerceAtLeast(0L)
                            dao.insertPlayer(p.copy(money = newMoney))
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    com.example.data.SupabaseManager.patchPlayerMoney(p.id, newMoney)
                                } catch (_: Exception) {}
                            }
                        }
                        
                        // Add to player's owned artifacts
                        val owned = getOwnedArtifactIds(context).toMutableSet()
                        owned.add(artId)
                        prefs.edit().putStringSet(KEY_OWNED_ARTIFACTS, owned).apply()

                        // Update registry to OWNED_BY_PLAYER
                        val certCode = getArtifactCertificateCode(artId)
                        registry[artId] = MuseumArtifactRegistryItem(
                            artifactId = artId,
                            ownerId = if (p != null) p.id else winnerId,
                            ownerName = if (p != null) p.name else winnerName,
                            status = "OWNED_BY_PLAYER",
                            activeAuctionId = null,
                            lastPrice = finalPrice,
                            certificateCode = certCode,
                            mintTotal = 1,
                            updatedAtMs = now
                        )

                        // Clear any ban
                        prefs.edit().remove("banned_artifact_$artId").apply()

                        // Mark auction settled
                        val settledAuction = auction.copy(
                            isSettled = true,
                            currentHighestBidderId = if (p != null) p.id else winnerId,
                            currentHighestBidder = if (p != null) p.name else winnerName
                        )
                        remainingList.add(settledAuction)
                        modified = true

                        // UI Notification & Haptic
                        withContext(Dispatchers.Main) {
                            try {
                                com.example.utils.HapticManager.performHaptic(com.example.utils.HapticManager.HapticType.CONSORTIUM_APPROVAL)
                                com.example.ui.components.SmartNotificationManager.show(
                                    message = "🏛️ TEBRİKLER! Müzayedeyi kazandınız: '$artifactName' eseri ₳${com.example.ui.components.formatMoney(finalPrice)} bedelle müzenize devredildi!",
                                    enMessage = "🏛️ CONGRATULATIONS! You won the auction: '$artifactName' transferred to your museum for ₳${com.example.ui.components.formatMoney(finalPrice)}!",
                                    type = com.example.ui.components.NotificationType.SUCCESS,
                                    durationMs = 4000L
                                )
                            } catch (_: Exception) {}
                        }

                        // Background sync to Supabase
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                com.example.data.SupabaseManager.syncMuseumArtifactOwnershipToSupabase(
                                    artifactId = artId,
                                    ownerId = if (p != null) p.id else winnerId,
                                    ownerName = if (p != null) p.name else winnerName,
                                    status = "OWNED_BY_PLAYER",
                                    activeAuctionId = null,
                                    lastPrice = finalPrice
                                )
                                com.example.data.SupabaseManager.publishMuseumAuctionToSupabase(
                                    com.example.data.MuseumAuctionEntity(
                                        id = auction.id,
                                        artifactId = auction.artifactId,
                                        sellerId = auction.sellerId,
                                        sellerName = auction.sellerName,
                                        isPlayerSeller = auction.isPlayerSeller,
                                        startingBid = auction.startingBid,
                                        currentHighestBid = finalPrice,
                                        currentHighestBidderId = if (p != null) p.id else winnerId,
                                        currentHighestBidderName = if (p != null) p.name else winnerName,
                                        buyoutPrice = 0L,
                                        endsAtMs = auction.endsAtMs,
                                        bidCount = auction.bidCount,
                                        createdAtMs = auction.createdAt,
                                        isSettled = true
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MuseumHeritageManager", "Settlement Supabase sync error", e)
                            }
                        }
                    } else {
                        // Winner is NPC or another collector
                        val certCode = getArtifactCertificateCode(artId)
                        registry[artId] = MuseumArtifactRegistryItem(
                            artifactId = artId,
                            ownerId = winnerId,
                            ownerName = winnerName,
                            status = "OWNED_BY_PLAYER",
                            activeAuctionId = null,
                            lastPrice = finalPrice,
                            certificateCode = certCode,
                            mintTotal = 1,
                            updatedAtMs = now
                        )
                        
                        // If seller was local player, pay the player
                        val isSellerLocal = isLocalPlayer(context, auction.sellerId, onlineUid) || (p != null && auction.sellerId == p.id)
                        if (isSellerLocal && p != null) {
                            val newMoney = p.money + finalPrice
                            dao.insertPlayer(p.copy(money = newMoney))
                            withContext(Dispatchers.Main) {
                                try {
                                    com.example.ui.components.SmartNotificationManager.show(
                                        message = "💰 Müzayedeye koyduğunuz '$artifactName' eseri $winnerName tarafından ₳${com.example.ui.components.formatMoney(finalPrice)} fiyata satın alındı!",
                                        enMessage = "💰 Your auctioned artifact '$artifactName' was purchased by $winnerName for ₳${com.example.ui.components.formatMoney(finalPrice)}!",
                                        type = com.example.ui.components.NotificationType.SUCCESS,
                                        durationMs = 4000L
                                    )
                                } catch (_: Exception) {}
                            }
                        }

                        val settledAuction = auction.copy(isSettled = true)
                        remainingList.add(settledAuction)
                        modified = true

                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                com.example.data.SupabaseManager.syncMuseumArtifactOwnershipToSupabase(
                                    artifactId = artId,
                                    ownerId = winnerId,
                                    ownerName = winnerName,
                                    status = "OWNED_BY_PLAYER",
                                    activeAuctionId = null,
                                    lastPrice = finalPrice
                                )
                                com.example.data.SupabaseManager.publishMuseumAuctionToSupabase(
                                    com.example.data.MuseumAuctionEntity(
                                        id = auction.id,
                                        artifactId = auction.artifactId,
                                        sellerId = auction.sellerId,
                                        sellerName = auction.sellerName,
                                        isPlayerSeller = auction.isPlayerSeller,
                                        startingBid = auction.startingBid,
                                        currentHighestBid = finalPrice,
                                        currentHighestBidderId = winnerId,
                                        currentHighestBidderName = winnerName,
                                        buyoutPrice = 0L,
                                        endsAtMs = auction.endsAtMs,
                                        bidCount = auction.bidCount,
                                        createdAtMs = auction.createdAt,
                                        isSettled = true
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                } else if (!auction.isPlayerSeller) {
                    // Treasury auction with 0 bids: roll over into the next 1-hour live auction cycle!
                    val newAuction = auction.copy(
                        endsAtMs = now + 3600_000L,
                        createdAt = now,
                        bidCount = 0,
                        currentHighestBid = auction.startingBid,
                        currentHighestBidderId = "",
                        currentHighestBidder = ""
                    )
                    remainingList.add(newAuction)
                    modified = true
                    
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val artifactObj = allArtifacts.find { it.id == newAuction.artifactId }
                        com.example.data.MultiplayerManager.sendBroadcastAuctionListed(
                            com.example.data.network.LiveAuctionListedEventDto(
                                auctionId = newAuction.id,
                                artifactId = newAuction.artifactId,
                                artifactName = artifactObj?.name ?: newAuction.artifactId,
                                sellerName = "T.C. Kültür ve Turizm Bakanlığı",
                                startingBid = newAuction.startingBid,
                                endsAtMs = now + 3600_000L
                            )
                        )
                    }
                    
                    pushAuctionToSupabaseBg(newAuction)
                } else {
                    // Player auction with 0 bids expired -> keep settled/completed so player can reclaim
                    val expiredPlayerAuction = auction.copy(isSettled = true)
                    remainingList.add(expiredPlayerAuction)
                    modified = true
                }
            } else {
                remainingList.add(auction)
            }
        }
        list = remainingList

        // 2. Ensure all 13 artifacts that are still in UNCLAIMED_TREASURY are active in live 1-hour auctions
        for (art in allArtifacts) {
            val reg = registry[art.id]
            val isTreasury = reg == null || reg.status == "UNCLAIMED_TREASURY" || reg.ownerId == null || reg.ownerId == "treasury"
            if (isTreasury) {
                val hasActiveAuction = list.any { it.artifactId == art.id && !it.isSettled }
                val hasBeenBoughtRecently = list.any { it.artifactId == art.id && it.isSettled && it.bidCount > 0 }
                if (!hasActiveAuction && !hasBeenBoughtRecently) {
                    val auctionId = "auction_${art.id}"
                    val startingBid = (art.baseValue * 0.8).toLong()
                    val newAuction = MuseumAuctionItem(
                        id = auctionId,
                        artifactId = art.id,
                        sellerId = "treasury",
                        sellerName = "T.C. Kültür ve Turizm Bakanlığı",
                        isPlayerSeller = false,
                        startingBid = startingBid,
                        currentHighestBid = startingBid,
                        buyoutPrice = 0L,
                        endsAtMs = now + (3600_000L), // 1 hour live auction
                        bidCount = 0,
                        createdAt = now,
                        isSettled = false
                    )
                    list.add(newAuction)
                    registry[art.id] = MuseumArtifactRegistryItem(
                        artifactId = art.id,
                        ownerId = null,
                        ownerName = "T.C. Kültür ve Turizm Bakanlığı (Müzayede)",
                        status = "ON_AUCTION",
                        activeAuctionId = auctionId,
                        lastPrice = art.baseValue,
                        certificateCode = getArtifactCertificateCode(art.id),
                        mintTotal = 1,
                        updatedAtMs = now
                    )
                    modified = true
                    
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        com.example.data.MultiplayerManager.sendBroadcastAuctionListed(
                            com.example.data.network.LiveAuctionListedEventDto(
                                auctionId = auctionId,
                                artifactId = art.id,
                                artifactName = art.name,
                                sellerName = "T.C. Kültür ve Turizm Bakanlığı",
                                startingBid = startingBid,
                                endsAtMs = now + 3600_000L
                            )
                        )
                    }
                    
                    pushAuctionToSupabaseBg(newAuction)
                }
            }
        }

        if (modified) {
            prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).apply()
            saveGlobalRegistry(context, registry)
        }

        return@withContext list
    }

    suspend fun healAndRecoverStuckArtifacts(context: Context, playerName: String = "Siz") = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: ""
        val auctions = deserializeAuctionList(jsonStr)
        val registry = getGlobalRegistry(context).toMutableMap()
        val owned = getOwnedArtifactIds(context).toMutableSet()
        val myUid = getUniquePlayerId(context)

        var modifiedRegistry = false
        var modifiedAuctions = false

        for ((artId, regItem) in registry) {
            if (regItem.status == "ON_AUCTION") {
                val activeAuction = auctions.find { it.id == regItem.activeAuctionId && !it.isSettled }
                val settledAuction = auctions.find { it.id == regItem.activeAuctionId && it.isSettled }
                
                if (activeAuction == null) {
                    if (settledAuction != null && settledAuction.bidCount > 0 && settledAuction.currentHighestBidderId.isNotBlank()) {
                        // Recover ownership to the auction winner
                        val winnerId = settledAuction.currentHighestBidderId
                        val winnerName = settledAuction.currentHighestBidder.ifBlank { "Koleksiyoner" }
                        val isLocal = isLocalPlayer(context, winnerId)
                        if (isLocal) {
                            owned.add(artId)
                        }
                        registry[artId] = regItem.copy(
                            ownerId = winnerId,
                            ownerName = winnerName,
                            status = "OWNED_BY_PLAYER",
                            activeAuctionId = null,
                            lastPrice = settledAuction.currentHighestBid,
                            updatedAtMs = System.currentTimeMillis()
                        )
                        modifiedRegistry = true
                    } else {
                        // Recover stuck artifact back to treasury or player
                        val newOwnerId = if (artId in owned) myUid else null
                        val newOwnerName = if (artId in owned) "Siz (Holding)" else "T.C. Kültür ve Turizm Bakanlığı (Vakıflar Gn. Md.)"
                        val newStatus = if (artId in owned) "OWNED_BY_PLAYER" else "UNCLAIMED_TREASURY"

                        registry[artId] = regItem.copy(
                            ownerId = newOwnerId,
                            ownerName = newOwnerName,
                            status = newStatus,
                            activeAuctionId = null,
                            updatedAtMs = System.currentTimeMillis()
                        )
                        modifiedRegistry = true
                    }
                }
            }
        }

        if (modifiedRegistry) {
            saveGlobalRegistry(context, registry)
            prefs.edit().putStringSet(KEY_OWNED_ARTIFACTS, owned).apply()
        }
        if (modifiedAuctions) {
            prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(auctions)).apply()
        }
    }

    suspend fun syncWithSupabaseSafe(): Boolean {
        val ctx = storedContext ?: return false
        return syncWithSupabase(ctx)
    }

    suspend fun checkAndSettleAuctionsSafe(playerName: String = "Siz", onlineUid: String = ""): List<MuseumAuctionItem> {
        val ctx = storedContext ?: return emptyList()
        return getAllMuseumAuctions(ctx, playerName, onlineUid)
    }

    suspend fun syncWithSupabase(context: Context, playerName: String = "Siz", onlineUid: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val myUid = getUniquePlayerId(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. Remote Registry Fetch
            val remoteRegistry = SupabaseManager.fetchMuseumRegistryFromSupabase()
            val localRegistry = getGlobalRegistry(context).toMutableMap()
            val localOwned = getOwnedArtifactIds(context).toMutableSet()

            if (!remoteRegistry.isNullOrEmpty()) {
                for (remoteItem in remoteRegistry) {
                    val currentLocal = localRegistry[remoteItem.artifactId]
                    if (currentLocal == null || remoteItem.updatedAtMs >= currentLocal.updatedAtMs) {
                        localRegistry[remoteItem.artifactId] = MuseumArtifactRegistryItem(
                            artifactId = remoteItem.artifactId,
                            ownerId = remoteItem.ownerId,
                            ownerName = remoteItem.ownerName,
                            status = remoteItem.status,
                            activeAuctionId = remoteItem.activeAuctionId,
                            lastPrice = remoteItem.lastPrice,
                            certificateCode = getArtifactCertificateCode(remoteItem.artifactId),
                            mintTotal = 1,
                            updatedAtMs = remoteItem.updatedAtMs
                        )
                        if (isLocalPlayer(context, remoteItem.ownerId, onlineUid) && remoteItem.status == "OWNED_BY_PLAYER") {
                            localOwned.add(remoteItem.artifactId)
                        } else {
                            localOwned.remove(remoteItem.artifactId)
                        }
                    }
                }
            }

            // 2. Remote Auctions Fetch (Realtime Server State)
            val remoteAuctions = SupabaseManager.fetchMuseumAuctionsFromSupabase()
            if (remoteAuctions != null) {
                val currentLocalList = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
                val mergedList = mutableListOf<MuseumAuctionItem>()
                val remoteIds = remoteAuctions.map { it.id }.toSet()
                for (remote in remoteAuctions) {
                    val localMatch = currentLocalList.find { it.id == remote.id }
                    val isLocallySettled = localMatch?.isSettled == true
                    mergedList.add(
                        MuseumAuctionItem(
                            id = remote.id,
                            artifactId = remote.artifactId,
                            sellerId = remote.sellerId,
                            sellerName = remote.sellerName,
                            isPlayerSeller = remote.isPlayerSeller,
                            startingBid = remote.startingBid,
                            currentHighestBid = if (localMatch != null && localMatch.currentHighestBid > remote.currentHighestBid) localMatch.currentHighestBid else remote.currentHighestBid,
                            currentHighestBidderId = if (localMatch != null && localMatch.currentHighestBid > remote.currentHighestBid) localMatch.currentHighestBidderId else remote.currentHighestBidderId,
                            currentHighestBidder = if (localMatch != null && localMatch.currentHighestBid > remote.currentHighestBid) localMatch.currentHighestBidder else remote.currentHighestBidderName,
                            buyoutPrice = remote.buyoutPrice,
                            endsAtMs = if (localMatch != null && localMatch.endsAtMs > remote.endsAtMs) localMatch.endsAtMs else remote.endsAtMs,
                            bidCount = if (localMatch != null && localMatch.bidCount > remote.bidCount) localMatch.bidCount else remote.bidCount,
                            createdAt = if (localMatch != null && localMatch.createdAt > remote.createdAtMs) localMatch.createdAt else remote.createdAtMs,
                            isSettled = isLocallySettled || remote.isSettled,
                            lastBidTimeMs = if (localMatch != null && localMatch.lastBidTimeMs > remote.createdAtMs) localMatch.lastBidTimeMs else remote.createdAtMs
                        )
                    )
                }
                
                val now = System.currentTimeMillis()
                for (local in currentLocalList) {
                    if (local.id !in remoteIds) {
                        // Keep settled auctions for 24 hours to prevent ghost treasury recreations
                        if (now - local.endsAtMs < 86400_000L) {
                            if (local.isSettled) {
                                mergedList.add(local)
                            } else {
                                // Grace period of 120 seconds for newly created local auctions to appear on Supabase
                                if (now - local.createdAt < 120_000L) {
                                    mergedList.add(local)
                                } else {
                                    mergedList.add(local.copy(isSettled = true))
                                }
                            }
                        }
                    }
                }
                prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(mergedList)).apply()
            }

            prefs.edit().putStringSet(KEY_OWNED_ARTIFACTS, localOwned).apply()
            saveGlobalRegistry(context, localRegistry)
            healAndRecoverStuckArtifacts(context, playerName)
            // Immediately evaluate expired auctions
            getAllMuseumAuctions(context, playerName, onlineUid)
            true
        } catch (e: Exception) {
            android.util.Log.e("MuseumHeritageManager", "Supabase sync error", e)
            false
        }
    }

    suspend fun createPlayerMuseumAuction(
        context: Context,
        artifactId: String,
        startingBid: Long,
        buyoutPrice: Long = 0L,
        durationHours: Int = 1,
        durationMinutes: Int = durationHours * 60,
        sellerName: String = "Siz"
    ): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val owned = getOwnedArtifactIds(context)

        if (artifactId !in owned) return@withContext false
        
        // Prevent duplicate player auctions
        val currentList = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        if (currentList.any { it.artifactId == artifactId && !it.isSettled }) {
            return@withContext false
        }

        val registry = getGlobalRegistry(context).toMutableMap()
        val regItem = registry[artifactId] ?: return@withContext false
        val myUid = getUniquePlayerId(context)

        val auctionId = "auc_p_" + java.util.UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()
        val effectiveMinutes = if (durationMinutes > 0) durationMinutes else (durationHours * 60)

        val auctionItem = MuseumAuctionItem(
            id = auctionId,
            artifactId = artifactId,
            sellerId = myUid,
            sellerName = sellerName,
            isPlayerSeller = true,
            startingBid = startingBid,
            currentHighestBid = startingBid,
            buyoutPrice = 0L,
            endsAtMs = now + (effectiveMinutes * 60_000L),
            bidCount = 0,
            createdAt = now
        )

        val list = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        list.add(auctionItem)
        prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).apply()

        // Remove from local owned set while on auction
        removeArtifactFromMuseum(context, artifactId)

        // Update global registry
        registry[artifactId] = regItem.copy(
            ownerId = myUid,
            ownerName = sellerName,
            status = "ON_AUCTION",
            activeAuctionId = auctionId,
            updatedAtMs = now
        )
        saveGlobalRegistry(context, registry)

        // Publish to Supabase
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseManager.publishMuseumAuctionToSupabase(
                    MuseumAuctionEntity(
                        id = auctionItem.id,
                        artifactId = auctionItem.artifactId,
                        sellerId = auctionItem.sellerId,
                        sellerName = auctionItem.sellerName,
                        isPlayerSeller = true,
                        startingBid = auctionItem.startingBid,
                        currentHighestBid = auctionItem.currentHighestBid,
                        currentHighestBidderId = "",
                        currentHighestBidderName = "",
                        buyoutPrice = 0L,
                        endsAtMs = auctionItem.endsAtMs,
                        bidCount = 0,
                        createdAtMs = auctionItem.createdAt,
                        isSettled = false
                    )
                )
                SupabaseManager.syncMuseumArtifactOwnershipToSupabase(
                    artifactId = artifactId,
                    ownerId = myUid,
                    ownerName = sellerName,
                    status = "ON_AUCTION",
                    activeAuctionId = auctionId,
                    lastPrice = startingBid
                )
                
                val artifactObj = allArtifacts.find { it.id == artifactId }
                com.example.data.MultiplayerManager.sendBroadcastAuctionListed(
                    com.example.data.network.LiveAuctionListedEventDto(
                        auctionId = auctionId,
                        artifactId = artifactId,
                        artifactName = artifactObj?.name ?: artifactId,
                        sellerName = sellerName,
                        startingBid = startingBid,
                        endsAtMs = auctionItem.endsAtMs
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("MuseumHeritageManager", "Failed to sync player auction to Supabase", e)
            }
        }

        true
    }

    fun cancelPlayerMuseumAuction(context: Context, auctionId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        val index = list.indexOfFirst { it.id == auctionId && it.isPlayerSeller && !it.isSettled }

        if (index < 0) return false

        val auction = list[index]
        if (auction.bidCount > 0) return false // Cannot cancel if bids exist

        list.removeAt(index)
        prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).apply()

        // Restore ownership back to player
        val owned = getOwnedArtifactIds(context).toMutableSet()
        owned.add(auction.artifactId)
        prefs.edit().putStringSet(KEY_OWNED_ARTIFACTS, owned).apply()

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val myUid = getUniquePlayerId(context)
                SupabaseManager.syncMuseumArtifactOwnershipToSupabase(
                    artifactId = auction.artifactId,
                    ownerId = myUid,
                    ownerName = "Siz (Holding)",
                    status = "OWNED_BY_PLAYER",
                    activeAuctionId = null,
                    lastPrice = auction.startingBid
                )
            } catch (e: Exception) {
                android.util.Log.e("MuseumHeritageManager", "Cancel auction Supabase sync error", e)
            }
        }

        return true
    }

    fun claimPlayerAuctionProceeds(context: Context, auctionId: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        val index = list.indexOfFirst { it.id == auctionId && it.isPlayerSeller }

        if (index < 0) return 0L

        val auction = list[index]
        list.removeAt(index)
        prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).apply()

        return auction.currentHighestBid
    }

    suspend fun bidOnAuction(
        context: Context,
        auctionId: String,
        bidAmount: Long,
        bidderId: String = "",
        bidderName: String = "Siz"
    ): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        val index = list.indexOfFirst { it.id == auctionId && !it.isSettled }

        val myUid = if (bidderId.isNotBlank()) bidderId else getUniquePlayerId(context)

        if (index >= 0) {
            val auction = list[index]
            val updated = auction.copy(
                currentHighestBid = bidAmount,
                currentHighestBidderId = myUid,
                currentHighestBidder = bidderName,
                bidCount = auction.bidCount + 1,
                lastBidTimeMs = System.currentTimeMillis()
            )
            list[index] = updated
            prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).commit()
        }

        // 1. Direct PATCH update to Supabase
        val patchOk = SupabaseManager.updateMuseumAuctionBidInSupabase(
            auctionId = auctionId,
            newBid = bidAmount,
            bidderId = myUid,
            bidderName = bidderName
        )

        // Hızlı sekronizasyon için Realtime kanalına broadcast yayınla
        if (index >= 0) {
            com.example.data.MultiplayerManager.sendBroadcastAuctionBid(
                com.example.data.network.LiveAuctionBidEventDto(
                    auctionId = auctionId,
                    artifactId = list[index].artifactId,
                    newBidAmount = bidAmount,
                    bidderId = myUid,
                    bidderName = bidderName
                )
            )
        }

        // 2. Immediate Registry Sync
        syncWithSupabase(context, bidderName, myUid)
        return@withContext true
    }

    fun applyLiveAuctionBidFromBroadcast(
        context: Context? = storedContext,
        auctionId: String = "",
        newBid: Long = 0L,
        bidderName: String = "",
        event: com.example.data.network.LiveAuctionBidEventDto? = null
    ) {
        val targetContext = context ?: storedContext ?: return
        val targetAuctionId = event?.auctionId ?: auctionId
        val targetBid = event?.newBidAmount ?: newBid
        val targetBidder = event?.bidderName ?: bidderName
        if (targetAuctionId.isBlank()) return

        val prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        val index = list.indexOfFirst { it.id == targetAuctionId }

        if (index >= 0) {
            val auction = list[index]
            if (targetBid > auction.currentHighestBid) {
                list[index] = auction.copy(
                    currentHighestBid = targetBid,
                    currentHighestBidder = targetBidder,
                    bidCount = auction.bidCount + 1,
                    lastBidTimeMs = System.currentTimeMillis()
                )
                prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).apply()
            }
        }
    }

    suspend fun buyoutAuctionItem(
        context: Context,
        auctionId: String,
        buyerId: String = "",
        buyerName: String = "Siz"
    ): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = deserializeAuctionList(prefs.getString(KEY_MUSEUM_AUCTIONS_LIST, "") ?: "")
        val index = list.indexOfFirst { it.id == auctionId && !it.isSettled }

        if (index < 0) return@withContext false

        val auction = list[index]
        val myUid = if (buyerId.isNotBlank()) buyerId else getUniquePlayerId(context)

        // Settle auction
        list[index] = auction.copy(
            currentHighestBid = auction.buyoutPrice,
            currentHighestBidderId = myUid,
            currentHighestBidder = buyerName,
            isSettled = true,
            bidCount = auction.bidCount + 1
        )
        prefs.edit().putString(KEY_MUSEUM_AUCTIONS_LIST, serializeAuctionList(list)).apply()

        // Add artifact to buyer's museum
        addArtifactToMuseum(
            context = context,
            artifactId = auction.artifactId,
            ownerName = buyerName,
            pricePaid = auction.buyoutPrice,
            customOwnerId = myUid
        )

        true
    }

    fun exportToJson(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val map = mutableMapOf<String, Any?>()
            
            // Only export personal data, NEVER global registries or auctions
            val keysToExport = listOf(
                KEY_OWNED_ARTIFACTS,
                KEY_LAST_CLAIM_TIME,
                KEY_PROFILE_VISIT_REVENUE,
                KEY_TOTAL_PROFILE_INSPECTIONS
            )
            
            keysToExport.forEach { key ->
                if (prefs.contains(key)) {
                    val value = prefs.all[key]
                    if (value is Set<*>) {
                        map[key] = value.filterNotNull()
                    } else {
                        map[key] = value
                    }
                }
            }
            AppJson.encodeToString(anyToJsonElement(map))
        } catch (e: Exception) {
            "{}"
        }
    }

    fun importFromJson(context: Context, jsonStr: String) {
        if (jsonStr.isBlank()) return
        try {
            val jsonElement = AppJson.parseToJsonElement(jsonStr) as? JsonObject ?: return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Do NOT clear the editor. We want to keep the multiplayer global registry intact.
            val keysToImport = listOf(
                KEY_OWNED_ARTIFACTS,
                KEY_LAST_CLAIM_TIME,
                KEY_PROFILE_VISIT_REVENUE,
                KEY_TOTAL_PROFILE_INSPECTIONS
            )

            jsonElement.forEach { (key, element) ->
                if (key in keysToImport) {
                    when (element) {
                        is JsonPrimitive -> {
                            element.longOrNull?.let { editor.putLong(key, it); return@forEach }
                            element.doubleOrNull?.let { editor.putFloat(key, it.toFloat()); return@forEach }
                            element.booleanOrNull?.let { editor.putBoolean(key, it); return@forEach }
                            editor.putString(key, element.content)
                        }
                        is JsonArray -> {
                            val set = element.mapNotNull { (it as? JsonPrimitive)?.content }.toSet()
                            editor.putStringSet(key, set)
                        }
                        else -> {}
                    }
                }
            }
            editor.apply()
        } catch (e: Exception) {
            android.util.Log.e("MuseumHeritageManager", "Failed to import museum JSON", e)
        }
    }

    private fun pushAuctionToSupabaseBg(auction: MuseumAuctionItem) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            com.example.data.SupabaseManager.publishMuseumAuctionToSupabase(
                com.example.data.MuseumAuctionEntity(
                    id = auction.id,
                    artifactId = auction.artifactId,
                    sellerId = auction.sellerId,
                    sellerName = auction.sellerName,
                    isPlayerSeller = auction.isPlayerSeller,
                    startingBid = auction.startingBid,
                    currentHighestBid = auction.currentHighestBid,
                    currentHighestBidderId = auction.currentHighestBidderId,
                    currentHighestBidderName = auction.currentHighestBidder,
                    buyoutPrice = auction.buyoutPrice,
                    endsAtMs = auction.endsAtMs,
                    bidCount = auction.bidCount,
                    createdAtMs = auction.createdAt,
                    isSettled = auction.isSettled
                )
            )
        }
    }

    suspend fun resetAllMuseumProgress(context: Context? = storedContext) = withContext(Dispatchers.IO) {
        val targetContext = context ?: storedContext ?: return@withContext
        val prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val myUid = getUniquePlayerId(targetContext)
        val ownedArtifacts = getOwnedArtifactIds(targetContext)

        // Release player's artifacts in the registry back to unowned state
        val registry = getGlobalRegistry(targetContext).toMutableMap()
        ownedArtifacts.forEach { artId ->
            val current = registry[artId]
            if (current != null && (current.ownerId == myUid || current.ownerId == "local_player")) {
                registry[artId] = current.copy(
                    ownerId = "UNOWNED",
                    ownerName = "Devlet / Arkeoloji Müzesi",
                    status = "UNCLAIMED",
                    activeAuctionId = null,
                    updatedAtMs = System.currentTimeMillis()
                )
            }
        }
        saveGlobalRegistry(targetContext, registry)

        // Clear local preferences
        prefs.edit()
            .remove(KEY_OWNED_ARTIFACTS)
            .remove(KEY_LAST_CLAIM_TIME)
            .remove(KEY_PROFILE_VISIT_REVENUE)
            .remove(KEY_TOTAL_PROFILE_INSPECTIONS)
            .apply()
    }
}
