package com.example.ui.screens

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

import com.example.ui.components.CurrencyText

import com.example.data.*
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.ui.theme.isEnglishLanguage
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.view.MotionEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.CityProfile
import com.example.data.Product
import com.example.data.cities
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.ThemeGold
import com.example.ui.components.CaravanTravelAnimationDialog
import com.example.viewmodel.GameViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.Projection
import kotlin.math.pow
import kotlin.math.sqrt

val cityCoordinates = mapOf(
    // 🇹🇷 TÜRKİYE ŞEHİRLERİ (18 Şehir)
    "eskisehir" to GeoPoint(39.7767, 30.5206),
    "sivas" to GeoPoint(39.7505, 37.0150),
    "batman" to GeoPoint(37.8812, 41.1293),
    "zonguldak" to GeoPoint(41.4564, 31.7987),
    "artvin" to GeoPoint(41.1828, 41.8183),
    "mugla" to GeoPoint(37.2153, 28.3636),
    "elazig" to GeoPoint(38.6748, 39.2225),
    "kirikkale" to GeoPoint(39.8468, 33.5153),
    "kahramanmaras" to GeoPoint(37.5753, 36.9228),
    "konya" to GeoPoint(37.8746, 32.4932),
    "kocaeli" to GeoPoint(40.8533, 29.8815),
    "bursa" to GeoPoint(40.1828, 29.0667),
    "gaziantep" to GeoPoint(37.0662, 37.3833),
    "izmir" to GeoPoint(38.4237, 27.1428),
    "ankara" to GeoPoint(39.9334, 32.8597),
    "istanbul" to GeoPoint(41.0082, 28.9784),
    "mersin" to GeoPoint(36.8121, 34.6415),

    // 🌐 DÜNYA ŞEHİRLERİ (13 Küresel Merkez)
    "santiago" to GeoPoint(-33.4489, -70.6693),
    "kuala_lumpur" to GeoPoint(3.1390, 101.6869),
    "johannesburg" to GeoPoint(-26.2041, 28.0473),
    "basra" to GeoPoint(30.5085, 47.7835),
    "sao_paulo" to GeoPoint(-23.5505, -46.6333),
    "essen" to GeoPoint(51.4556, 7.0116),
    "frankfurt" to GeoPoint(50.1109, 8.6821),
    "shanghai" to GeoPoint(31.2304, 121.4737),
    "tokyo" to GeoPoint(35.6762, 139.6503),
    "houston" to GeoPoint(29.7604, -95.3698),
    "rotterdam" to GeoPoint(51.9244, 4.4777),
    "london" to GeoPoint(51.5074, -0.1278),
    "new_york" to GeoPoint(40.7128, -74.0060)
)

// ==========================================
// 🌐 REALISTIC GLOBAL LOGISTICS, HIGHWAY, RAILWAY & AIR ROUTE PLANNER
// ==========================================
object GeoRoutePlanner {
    private val highwayCache = java.util.concurrent.ConcurrentHashMap<String, List<GeoPoint>>()
    private val railwayCache = java.util.concurrent.ConcurrentHashMap<String, List<GeoPoint>>()
    private val maritimeCache = java.util.concurrent.ConcurrentHashMap<String, List<GeoPoint>>()

    val coastalPorts = setOf(
        "istanbul", "kocaeli", "bursa", "izmir", "mugla", "mersin", "zonguldak", "artvin",
        "rotterdam", "london", "basra", "shanghai", "tokyo", "santiago", "sao_paulo", "kuala_lumpur", "houston", "new_york"
    )

    val railwayCities = setOf(
        "ankara", "istanbul", "kocaeli", "konya", "gaziantep", "elazig", "eskisehir",
        "sivas", "kirikkale", "kahramanmaras", "batman", "zonguldak", "izmir", "essen", "frankfurt", "johannesburg"
    )

    val airHubCities = setOf(
        "istanbul", "ankara", "izmir", "eskisehir", "elazig", "gaziantep",
        "frankfurt", "tokyo", "shanghai", "santiago", "houston", "johannesburg", "kuala_lumpur", "london", "new_york"
    )

    fun isAirHub(city1: String, city2: String): Boolean {
        return airHubCities.contains(city1) || airHubCities.contains(city2)
    }

    // 🛣️ REALISTIC HIGHWAY NETWORK GRAPH (Karayolları, Otoyollar, Köprüler ve Dağ Geçitleri)
    private val highwayNodes = mapOf(
        "istanbul" to GeoPoint(41.0082, 28.9784),
        "gebze" to GeoPoint(40.8027, 29.4307),
        "osmangazi_bridge" to GeoPoint(40.7550, 29.5180),
        "yalova" to GeoPoint(40.6550, 29.2700),
        "bursa" to GeoPoint(40.1828, 29.0667),
        "karacabey" to GeoPoint(40.2150, 28.3600),
        "balikesir" to GeoPoint(39.6484, 27.8826),
        "akhisar" to GeoPoint(38.9244, 27.8400),
        "manisa" to GeoPoint(38.6191, 27.4289),
        "izmir" to GeoPoint(38.4237, 27.1428),
        "torbali" to GeoPoint(38.1500, 27.3600),
        "aydin" to GeoPoint(37.8450, 27.8450),
        "cine_pass" to GeoPoint(37.6000, 28.0600),
        "mugla" to GeoPoint(37.2153, 28.3636),
        "denizli" to GeoPoint(37.7765, 29.0864),
        "dinar" to GeoPoint(38.0650, 30.1650),
        "inegol" to GeoPoint(40.0780, 29.5130),
        "bozuyuk" to GeoPoint(39.9070, 30.0400),
        "eskisehir" to GeoPoint(39.7767, 30.5206),
        "kocaeli" to GeoPoint(40.8533, 29.8815),
        "sakarya" to GeoPoint(40.7569, 30.3783),
        "duzce" to GeoPoint(40.8438, 31.1565),
        "bolu_tunnel" to GeoPoint(40.7500, 31.4200),
        "bolu" to GeoPoint(40.7350, 31.6050),
        "gerede" to GeoPoint(40.8000, 32.2000),
        "devrek" to GeoPoint(41.2200, 31.9500),
        "zonguldak" to GeoPoint(41.4564, 31.7987),
        "kizilcahamam" to GeoPoint(40.4700, 32.6500),
        "ankara" to GeoPoint(39.9334, 32.8597),
        "polatli" to GeoPoint(39.5800, 32.1400),
        "sivrihisar" to GeoPoint(39.4500, 31.5300),
        "elmadag" to GeoPoint(39.9200, 33.2300),
        "kirikkale" to GeoPoint(39.8468, 33.5153),
        "yozgat" to GeoPoint(39.8200, 34.8100),
        "sorgun" to GeoPoint(39.8100, 35.1800),
        "yildizeli" to GeoPoint(39.8600, 36.6500),
        "sivas" to GeoPoint(39.7505, 37.0150),
        "kirsehir" to GeoPoint(39.1458, 34.1639),
        "kulu_junction" to GeoPoint(39.0800, 33.0800),
        "aksaray" to GeoPoint(38.3700, 34.0300),
        "konya" to GeoPoint(37.8746, 32.4932),
        "nigde" to GeoPoint(37.9650, 34.6800),
        "pozanti_pass" to GeoPoint(37.4200, 34.8700),
        "tarsus" to GeoPoint(36.9150, 34.8950),
        "mersin" to GeoPoint(36.8121, 34.6415),
        "adana" to GeoPoint(36.9914, 35.3308),
        "osmaniye" to GeoPoint(37.0746, 36.2464),
        "bahce_pass" to GeoPoint(37.2000, 36.5700),
        "nurdagi" to GeoPoint(37.1700, 36.7300),
        "gaziantep" to GeoPoint(37.0662, 37.3833),
        "pinarbasi" to GeoPoint(38.7200, 36.3900),
        "goksun_tunnels" to GeoPoint(38.0200, 36.4900),
        "kahramanmaras" to GeoPoint(37.5753, 36.9228),
        "birecik_bridge" to GeoPoint(37.0300, 37.9800),
        "sanliurfa" to GeoPoint(37.1674, 38.7955),
        "siverek" to GeoPoint(37.7500, 39.3100),
        "diyarbakir" to GeoPoint(37.9144, 40.2306),
        "batman" to GeoPoint(37.8812, 41.1293),
        "maden_canyon" to GeoPoint(38.3900, 39.6600),
        "sivrice" to GeoPoint(38.4500, 39.3100),
        "elazig" to GeoPoint(38.6748, 39.2225),
        "malatya" to GeoPoint(38.3552, 38.3095),
        "kangal" to GeoPoint(39.2300, 37.2300),
        "erzincan" to GeoPoint(39.7500, 39.4900),
        "erzurum" to GeoPoint(39.9043, 41.2679),
        "tortum_valley" to GeoPoint(40.2900, 41.5500),
        "artvin" to GeoPoint(41.1828, 41.8183),
        "edirne" to GeoPoint(41.6700, 26.5600),
        "sofia" to GeoPoint(42.6900, 23.3200),
        "belgrade" to GeoPoint(44.8100, 20.4600),
        "frankfurt" to GeoPoint(50.1109, 8.6821),
        "essen" to GeoPoint(51.4556, 7.0116),
        "cizre" to GeoPoint(37.3300, 42.1800),
        "mosul" to GeoPoint(36.3400, 43.1300),
        "baghdad" to GeoPoint(33.3152, 44.3661),
        "basra" to GeoPoint(30.5085, 47.7835)
    )

    private val highwayEdges = mapOf(
        "istanbul" to listOf("gebze", "edirne"),
        "gebze" to listOf("istanbul", "kocaeli", "osmangazi_bridge"),
        "osmangazi_bridge" to listOf("gebze", "yalova"),
        "yalova" to listOf("osmangazi_bridge", "bursa"),
        "bursa" to listOf("yalova", "karacabey", "inegol"),
        "karacabey" to listOf("bursa", "balikesir"),
        "balikesir" to listOf("karacabey", "akhisar"),
        "akhisar" to listOf("balikesir", "manisa"),
        "manisa" to listOf("akhisar", "izmir"),
        "izmir" to listOf("manisa", "torbali"),
        "torbali" to listOf("izmir", "aydin"),
        "aydin" to listOf("torbali", "cine_pass", "denizli"),
        "cine_pass" to listOf("aydin", "mugla"),
        "mugla" to listOf("cine_pass", "denizli"),
        "denizli" to listOf("aydin", "mugla", "dinar"),
        "dinar" to listOf("denizli", "konya"),
        "inegol" to listOf("bursa", "bozuyuk"),
        "bozuyuk" to listOf("inegol", "eskisehir"),
        "eskisehir" to listOf("bozuyuk", "sivrihisar"),
        "kocaeli" to listOf("gebze", "sakarya"),
        "sakarya" to listOf("kocaeli", "duzce"),
        "duzce" to listOf("sakarya", "bolu_tunnel"),
        "bolu_tunnel" to listOf("duzce", "bolu"),
        "bolu" to listOf("bolu_tunnel", "gerede"),
        "gerede" to listOf("bolu", "devrek", "kizilcahamam"),
        "devrek" to listOf("gerede", "zonguldak"),
        "zonguldak" to listOf("devrek"),
        "kizilcahamam" to listOf("gerede", "ankara"),
        "ankara" to listOf("kizilcahamam", "polatli", "elmadag", "kulu_junction"),
        "polatli" to listOf("ankara", "sivrihisar"),
        "sivrihisar" to listOf("eskisehir", "polatli", "konya"),
        "elmadag" to listOf("ankara", "kirikkale"),
        "kirikkale" to listOf("elmadag", "yozgat", "kirsehir"),
        "yozgat" to listOf("kirikkale", "sorgun"),
        "sorgun" to listOf("yozgat", "yildizeli"),
        "yildizeli" to listOf("sorgun", "sivas"),
        "sivas" to listOf("yildizeli", "kangal", "erzincan"),
        "kirsehir" to listOf("kirikkale", "nigde"),
        "kulu_junction" to listOf("ankara", "aksaray", "konya"),
        "aksaray" to listOf("kulu_junction", "nigde"),
        "konya" to listOf("sivrihisar", "dinar", "kulu_junction", "aksaray", "nigde"),
        "nigde" to listOf("aksaray", "kirsehir", "konya", "pozanti_pass"),
        "pozanti_pass" to listOf("nigde", "tarsus"),
        "tarsus" to listOf("pozanti_pass", "mersin", "adana"),
        "mersin" to listOf("tarsus"),
        "adana" to listOf("tarsus", "osmaniye"),
        "osmaniye" to listOf("adana", "bahce_pass"),
        "bahce_pass" to listOf("osmaniye", "nurdagi"),
        "nurdagi" to listOf("bahce_pass", "gaziantep", "kahramanmaras"),
        "gaziantep" to listOf("nurdagi", "birecik_bridge"),
        "pinarbasi" to listOf("kirsehir", "goksun_tunnels"),
        "goksun_tunnels" to listOf("pinarbasi", "kahramanmaras"),
        "kahramanmaras" to listOf("goksun_tunnels", "nurdagi"),
        "birecik_bridge" to listOf("gaziantep", "sanliurfa"),
        "sanliurfa" to listOf("birecik_bridge", "siverek"),
        "siverek" to listOf("sanliurfa", "diyarbakir"),
        "diyarbakir" to listOf("siverek", "batman", "maden_canyon"),
        "batman" to listOf("diyarbakir", "cizre"),
        "maden_canyon" to listOf("diyarbakir", "sivrice"),
        "sivrice" to listOf("maden_canyon", "elazig"),
        "elazig" to listOf("sivrice", "malatya"),
        "malatya" to listOf("elazig", "kangal"),
        "kangal" to listOf("sivas", "malatya"),
        "erzincan" to listOf("sivas", "erzurum"),
        "erzurum" to listOf("erzincan", "tortum_valley"),
        "tortum_valley" to listOf("erzurum", "artvin"),
        "artvin" to listOf("tortum_valley"),
        "edirne" to listOf("istanbul", "sofia"),
        "sofia" to listOf("edirne", "belgrade"),
        "belgrade" to listOf("sofia", "frankfurt"),
        "frankfurt" to listOf("belgrade", "essen"),
        "essen" to listOf("frankfurt"),
        "cizre" to listOf("batman", "mosul"),
        "mosul" to listOf("cizre", "baghdad"),
        "baghdad" to listOf("mosul", "basra"),
        "basra" to listOf("baghdad")
    )

    // 🚆 REALISTIC RAILWAY NETWORK GRAPH (TCDD Ana Hatları & YHT Koridorları)
    private val railwayNodes = mapOf(
        "istanbul" to GeoPoint(40.9800, 29.0500),
        "gebze_gar" to GeoPoint(40.7950, 29.4250),
        "kocaeli" to GeoPoint(40.7600, 29.9300),
        "arifiye_gar" to GeoPoint(40.7100, 30.3600),
        "bilecik_yht" to GeoPoint(40.1400, 29.9700),
        "bozuyuk_gar" to GeoPoint(39.9050, 30.0380),
        "eskisehir" to GeoPoint(39.7767, 30.5206),
        "polatli_yht" to GeoPoint(39.5800, 32.1400),
        "ankara" to GeoPoint(39.9360, 32.8440),
        "elmadag_rail" to GeoPoint(39.9200, 33.2300),
        "kirikkale" to GeoPoint(39.8468, 33.5153),
        "yerkoy_yht" to GeoPoint(39.6300, 34.4600),
        "yozgat_yht" to GeoPoint(39.7800, 34.8200),
        "sorgun_yht" to GeoPoint(39.8100, 35.1800),
        "akdagmadeni_yht" to GeoPoint(39.6600, 35.8800),
        "yildizeli_yht" to GeoPoint(39.8600, 36.6500),
        "sivas" to GeoPoint(39.7505, 37.0150),
        "irmak_junction" to GeoPoint(39.9100, 33.4500),
        "cankiri_rail" to GeoPoint(40.6000, 33.6100),
        "karabuk_rail" to GeoPoint(41.2000, 32.6200),
        "zonguldak" to GeoPoint(41.4564, 31.7987),
        "kutahya_rail" to GeoPoint(39.4200, 29.9800),
        "afyon_rail" to GeoPoint(38.7500, 30.5400),
        "usak_rail" to GeoPoint(38.6800, 29.4000),
        "alasehir_rail" to GeoPoint(38.3500, 28.5200),
        "manisa_rail" to GeoPoint(38.6191, 27.4289),
        "izmir" to GeoPoint(38.4237, 27.1428),
        "aksehir_rail" to GeoPoint(38.3500, 31.4100),
        "konya" to GeoPoint(37.8680, 32.4760),
        "karaman_rail" to GeoPoint(37.1800, 33.2200),
        "eregli_rail" to GeoPoint(37.5100, 34.0500),
        "ulukisla_rail" to GeoPoint(37.5400, 34.4800),
        "pozanti_rail" to GeoPoint(37.4200, 34.8700),
        "yenice_junction" to GeoPoint(36.9600, 35.0500),
        "mersin" to GeoPoint(36.8000, 34.6300),
        "adana_rail" to GeoPoint(36.9980, 35.3200),
        "osmaniye_rail" to GeoPoint(37.0700, 36.1400),
        "fevzipasa_junction" to GeoPoint(37.1000, 36.6500),
        "narli_junction" to GeoPoint(37.3800, 37.1400),
        "kahramanmaras" to GeoPoint(37.5753, 36.9228),
        "gaziantep" to GeoPoint(37.0662, 37.3833),
        "sarkisla_rail" to GeoPoint(39.3500, 36.4100),
        "kangal_rail" to GeoPoint(39.2500, 37.5000),
        "malatya_rail" to GeoPoint(38.3552, 38.3095),
        "elazig" to GeoPoint(38.6748, 39.2225),
        "maden_rail" to GeoPoint(38.3900, 39.6600),
        "ergani_rail" to GeoPoint(38.2700, 39.7600),
        "diyarbakir_rail" to GeoPoint(37.9144, 40.2306),
        "batman" to GeoPoint(37.8812, 41.1293),
        "edirne_rail" to GeoPoint(41.6700, 26.5600),
        "sofia_rail" to GeoPoint(42.6900, 23.3200),
        "belgrade_rail" to GeoPoint(44.8100, 20.4600),
        "budapest_rail" to GeoPoint(47.4900, 19.0400),
        "vienna_rail" to GeoPoint(48.2000, 16.3700),
        "frankfurt" to GeoPoint(50.1109, 8.6821),
        "essen" to GeoPoint(51.4556, 7.0116)
    )

    private val railwayEdges = mapOf(
        "istanbul" to listOf("gebze_gar", "edirne_rail"),
        "gebze_gar" to listOf("istanbul", "kocaeli"),
        "kocaeli" to listOf("gebze_gar", "arifiye_gar"),
        "arifiye_gar" to listOf("kocaeli", "bilecik_yht"),
        "bilecik_yht" to listOf("arifiye_gar", "bozuyuk_gar"),
        "bozuyuk_gar" to listOf("bilecik_yht", "eskisehir"),
        "eskisehir" to listOf("bozuyuk_gar", "polatli_yht", "kutahya_rail"),
        "polatli_yht" to listOf("eskisehir", "ankara", "konya"),
        "ankara" to listOf("polatli_yht", "elmadag_rail"),
        "elmadag_rail" to listOf("ankara", "kirikkale"),
        "kirikkale" to listOf("elmadag_rail", "yerkoy_yht", "irmak_junction"),
        "yerkoy_yht" to listOf("kirikkale", "yozgat_yht"),
        "yozgat_yht" to listOf("yerkoy_yht", "sorgun_yht"),
        "sorgun_yht" to listOf("yozgat_yht", "akdagmadeni_yht"),
        "akdagmadeni_yht" to listOf("sorgun_yht", "yildizeli_yht"),
        "yildizeli_yht" to listOf("akdagmadeni_yht", "sivas"),
        "sivas" to listOf("yildizeli_yht", "sarkisla_rail", "kangal_rail"),
        "irmak_junction" to listOf("kirikkale", "cankiri_rail"),
        "cankiri_rail" to listOf("irmak_junction", "karabuk_rail"),
        "karabuk_rail" to listOf("cankiri_rail", "zonguldak"),
        "zonguldak" to listOf("karabuk_rail"),
        "kutahya_rail" to listOf("eskisehir", "afyon_rail"),
        "afyon_rail" to listOf("kutahya_rail", "usak_rail", "aksehir_rail"),
        "usak_rail" to listOf("afyon_rail", "alasehir_rail"),
        "alasehir_rail" to listOf("usak_rail", "manisa_rail"),
        "manisa_rail" to listOf("alasehir_rail", "izmir"),
        "izmir" to listOf("manisa_rail"),
        "aksehir_rail" to listOf("afyon_rail", "konya"),
        "konya" to listOf("polatli_yht", "aksehir_rail", "karaman_rail"),
        "karaman_rail" to listOf("konya", "eregli_rail"),
        "eregli_rail" to listOf("karaman_rail", "ulukisla_rail"),
        "ulukisla_rail" to listOf("eregli_rail", "pozanti_rail"),
        "pozanti_rail" to listOf("ulukisla_rail", "yenice_junction"),
        "yenice_junction" to listOf("pozanti_rail", "mersin", "adana_rail"),
        "mersin" to listOf("yenice_junction"),
        "adana_rail" to listOf("yenice_junction", "osmaniye_rail"),
        "osmaniye_rail" to listOf("adana_rail", "fevzipasa_junction"),
        "fevzipasa_junction" to listOf("osmaniye_rail", "narli_junction"),
        "narli_junction" to listOf("fevzipasa_junction", "kahramanmaras", "gaziantep"),
        "kahramanmaras" to listOf("narli_junction"),
        "gaziantep" to listOf("narli_junction"),
        "sarkisla_rail" to listOf("sivas"),
        "kangal_rail" to listOf("sivas", "malatya_rail"),
        "malatya_rail" to listOf("kangal_rail", "elazig"),
        "elazig" to listOf("malatya_rail", "maden_rail"),
        "maden_rail" to listOf("elazig", "ergani_rail"),
        "ergani_rail" to listOf("maden_rail", "diyarbakir_rail"),
        "diyarbakir_rail" to listOf("ergani_rail", "batman"),
        "batman" to listOf("diyarbakir_rail"),
        "edirne_rail" to listOf("istanbul", "sofia_rail"),
        "sofia_rail" to listOf("edirne_rail", "belgrade_rail"),
        "belgrade_rail" to listOf("sofia_rail", "budapest_rail"),
        "budapest_rail" to listOf("belgrade_rail", "vienna_rail"),
        "vienna_rail" to listOf("budapest_rail", "frankfurt"),
        "frankfurt" to listOf("vienna_rail", "essen"),
        "essen" to listOf("frankfurt")
    )

    private fun findClosestNode(cityId: String, geo: GeoPoint, nodes: Map<String, GeoPoint>): String {
        if (nodes.containsKey(cityId)) return cityId
        var closestKey = nodes.keys.first()
        var minDist = Double.MAX_VALUE
        for ((key, pos) in nodes) {
            val d = geo.distanceToAsDouble(pos)
            if (d < minDist) {
                minDist = d
                closestKey = key
            }
        }
        return closestKey
    }

    private fun findShortestPath(
        startNode: String,
        endNode: String,
        nodePositions: Map<String, GeoPoint>,
        adjacency: Map<String, List<String>>
    ): List<GeoPoint> {
        if (startNode == endNode) return listOfNotNull(nodePositions[startNode])
        val queue = java.util.PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        val distances = mutableMapOf<String, Double>()
        val previous = mutableMapOf<String, String>()

        distances[startNode] = 0.0
        queue.add(startNode to 0.0)

        while (queue.isNotEmpty()) {
            val (current, currentDist) = queue.poll()
            if (current == endNode) break
            if (currentDist > (distances[current] ?: Double.MAX_VALUE)) continue

            val currPos = nodePositions[current] ?: continue
            for (neighbor in adjacency[current] ?: emptyList()) {
                val neighborPos = nodePositions[neighbor] ?: continue
                val edgeDist = currPos.distanceToAsDouble(neighborPos)
                val newDist = currentDist + edgeDist
                if (newDist < (distances[neighbor] ?: Double.MAX_VALUE)) {
                    distances[neighbor] = newDist
                    previous[neighbor] = current
                    queue.add(neighbor to newDist)
                }
            }
        }

        if (!previous.containsKey(endNode) && startNode != endNode) {
            return listOfNotNull(nodePositions[startNode], nodePositions[endNode])
        }

        val path = mutableListOf<GeoPoint>()
        var curr: String? = endNode
        while (curr != null) {
            nodePositions[curr]?.let { path.add(0, it) }
            curr = previous[curr]
        }
        return path
    }

    // 🛣️ HIGHWAY ROUTING (For Trucks)
    fun getHighwayRoute(originId: String, destId: String, originGeo: GeoPoint, destGeo: GeoPoint): List<GeoPoint> {
        val cacheKey = "${originId}_${destId}"
        return highwayCache.getOrPut(cacheKey) {
            val startNode = findClosestNode(originId, originGeo, highwayNodes)
            val endNode = findClosestNode(destId, destGeo, highwayNodes)
            val path = findShortestPath(startNode, endNode, highwayNodes, highwayEdges)
            val result = mutableListOf<GeoPoint>()
            result.add(originGeo)
            path.forEach { pt ->
                if (result.last().distanceToAsDouble(pt) > 5000.0) {
                    result.add(pt)
                }
            }
            if (result.last().distanceToAsDouble(destGeo) > 5000.0) {
                result.add(destGeo)
            }
            result
        }
    }

    // 🚆 RAILWAY ROUTING (For Trains)
    fun getRailwayRoute(originId: String, destId: String, originGeo: GeoPoint, destGeo: GeoPoint): List<GeoPoint> {
        val cacheKey = "${originId}_${destId}"
        return railwayCache.getOrPut(cacheKey) {
            val startNode = findClosestNode(originId, originGeo, railwayNodes)
            val endNode = findClosestNode(destId, destGeo, railwayNodes)
            val path = findShortestPath(startNode, endNode, railwayNodes, railwayEdges)
            val result = mutableListOf<GeoPoint>()
            result.add(originGeo)
            path.forEach { pt ->
                if (result.last().distanceToAsDouble(pt) > 5000.0) {
                    result.add(pt)
                }
            }
            if (result.last().distanceToAsDouble(destGeo) > 5000.0) {
                result.add(destGeo)
            }
            result
        }
    }

    // ✈️ DIRECT AIR ROUTING (For Planes - Straight flight vector)
    fun getAirRoute(originId: String, destId: String, originGeo: GeoPoint, destGeo: GeoPoint): List<GeoPoint> {
        return listOf(originGeo, destGeo)
    }

    // 🌊 Precise Maritime Corridors & Sea Waypoints (strictly in deep water)
    // 1. Arabian Peninsula & Persian Gulf
    private val BASRA_PORT = GeoPoint(30.00, 48.30)
    private val SHATT_AL_ARAB_MOUTH = GeoPoint(29.20, 49.00)
    private val PERSIAN_GULF_NORTH = GeoPoint(28.20, 50.50)
    private val PERSIAN_GULF_MID = GeoPoint(26.80, 52.20)
    private val PERSIAN_GULF_SOUTH = GeoPoint(26.00, 54.50)
    private val STRAIT_OF_HORMUZ = GeoPoint(26.35, 56.45)
    private val GULF_OF_OMAN = GeoPoint(24.50, 58.50)
    private val ARABIAN_SEA_RAS_AL_HADD = GeoPoint(22.00, 60.00)
    private val ARABIAN_SEA_OMAN_SOUTH = GeoPoint(18.50, 57.50)
    private val ARABIAN_SEA_YEMEN_EAST = GeoPoint(15.00, 53.50)
    private val GULF_OF_ADEN_EAST = GeoPoint(13.50, 49.50)
    private val GULF_OF_ADEN_MID = GeoPoint(12.50, 46.00)
    private val BAB_EL_MANDEB = GeoPoint(12.60, 43.35)
    private val RED_SEA_SOUTH = GeoPoint(16.00, 41.50)
    private val RED_SEA_MID = GeoPoint(20.50, 38.50)
    private val RED_SEA_NORTH = GeoPoint(25.00, 35.80)
    private val RED_SEA_UPPER = GeoPoint(27.40, 34.20)
    private val GULF_OF_SUEZ = GeoPoint(28.80, 33.00)
    private val SUEZ_CANAL_SOUTH = GeoPoint(29.95, 32.55)
    private val SUEZ_CANAL_NORTH = GeoPoint(31.25, 32.30)

    // 2. Mediterranean Sea Corridors
    private val MED_LEVANT = GeoPoint(33.50, 33.50)
    private val MERSIN_SEA = GeoPoint(36.40, 34.80)
    private val MED_CYPRUS_SOUTH = GeoPoint(34.20, 32.00)
    private val MED_CRETE_SOUTH = GeoPoint(34.60, 24.50)
    private val MED_IONIAN = GeoPoint(36.20, 18.00)
    private val MED_SICILY_STRAIT = GeoPoint(37.40, 11.50)
    private val MED_BALEARIC = GeoPoint(38.00, 3.50)
    private val MED_ALBORAN = GeoPoint(36.10, -2.50)
    private val STRAIT_OF_GIBRALTAR = GeoPoint(35.95, -5.60)

    // 3. Atlantic & Western Europe
    private val GULF_OF_CADIZ = GeoPoint(36.20, -7.50)
    private val CAPE_ST_VINCENT = GeoPoint(36.90, -9.30)
    private val PORTUGAL_OFFSHORE = GeoPoint(39.50, -10.00)
    private val CAPE_FINISTERRE = GeoPoint(43.20, -9.80)
    private val BAY_OF_BISCAY = GeoPoint(46.00, -6.50)
    private val ENGLISH_CHANNEL_WEST = GeoPoint(49.20, -4.50)
    private val ENGLISH_CHANNEL_MID = GeoPoint(50.20, -1.00)
    private val STRAIT_OF_DOVER = GeoPoint(51.10, 1.50)
    private val THAMES_ESTUARY = GeoPoint(51.50, 0.80)
    private val ROTTERDAM_APPROACH = GeoPoint(52.00, 3.80)

    // 4. South America & Atlantic Ocean
    private val SAO_PAULO_COAST = GeoPoint(-24.10, -46.20)
    private val BRAZIL_CABO_FRIO = GeoPoint(-23.00, -41.50)
    private val BRAZIL_BAHIA = GeoPoint(-14.00, -37.50)
    private val BRAZIL_EAST_CAPE = GeoPoint(-7.50, -33.50)
    private val EQUATOR_ATLANTIC = GeoPoint(2.50, -29.00)
    private val MID_NORTH_ATLANTIC = GeoPoint(18.00, -22.00)
    private val CANARY_ISLANDS_SEA = GeoPoint(28.50, -15.50)
    private val MOROCCO_OFFSHORE = GeoPoint(33.00, -10.00)

    // 5. Pacific / Santiago (Chile) & Panama Canal
    private val VALPARAISO_SEA = GeoPoint(-33.00, -72.50)
    private val CHILE_NORTH = GeoPoint(-20.00, -71.50)
    private val PERU_SEA = GeoPoint(-10.00, -78.50)
    private val ECUADOR_SEA = GeoPoint(0.50, -81.00)
    private val PANAMA_PACIFIC = GeoPoint(7.50, -79.80)
    private val PANAMA_CANAL = GeoPoint(9.10, -79.70)
    private val CARIBBEAN_WEST = GeoPoint(12.50, -77.00)
    private val CARIBBEAN_MID = GeoPoint(16.50, -68.00)
    private val ATLANTIC_AZORES_SOUTH = GeoPoint(28.00, -35.00)

    // 6. North America (Houston & New York)
    private val HOUSTON_SEA = GeoPoint(28.50, -94.50)
    private val GULF_OF_MEXICO_MID = GeoPoint(25.50, -88.00)
    private val FLORIDA_STRAITS = GeoPoint(24.20, -81.50)
    private val ATLANTIC_BERMUDA = GeoPoint(29.00, -65.00)
    private val NEW_YORK_SEA = GeoPoint(40.45, -73.85)
    private val ATLANTIC_NY_OFFSHORE = GeoPoint(39.00, -70.00)
    private val ATLANTIC_NORTH_MID = GeoPoint(38.00, -45.00)
    private val ATLANTIC_AZORES_NORTH = GeoPoint(38.50, -28.00)

    // 7. East Asia & Indian Ocean
    private val TOKYO_BAY = GeoPoint(35.20, 139.80)
    private val PACIFIC_JAPAN_SOUTH = GeoPoint(30.00, 133.00)
    private val SHANGHAI_SEA = GeoPoint(31.20, 122.50)
    private val TAIWAN_STRAIT = GeoPoint(24.00, 120.00)
    private val SOUTH_CHINA_SEA = GeoPoint(12.00, 112.00)
    private val SINGAPORE_STRAIT = GeoPoint(1.25, 103.80)
    private val STRAIT_OF_MALACCA = GeoPoint(3.00, 100.50)
    private val ANDAMAN_SEA = GeoPoint(6.00, 95.50)
    private val SRI_LANKA_SOUTH = GeoPoint(5.50, 83.00)
    private val ARABIAN_SEA_MID = GeoPoint(10.00, 65.00)

    // 8. Turkish Seas & Straits
    private val AEGEAN_SOUTH = GeoPoint(36.50, 27.20)
    private val AEGEAN_MID = GeoPoint(38.20, 25.80)
    private val AEGEAN_NORTH = GeoPoint(39.80, 25.80)
    private val DARDANELLES_SOUTH = GeoPoint(40.00, 26.20)
    private val DARDANELLES_NORTH = GeoPoint(40.40, 26.70)
    private val MARMARA_MID = GeoPoint(40.75, 28.50)
    private val BOSPORUS_SOUTH = GeoPoint(41.00, 28.98)
    private val BOSPORUS_MID = GeoPoint(41.12, 29.06)
    private val BOSPORUS_NORTH = GeoPoint(41.25, 29.13)
    private val BLACK_SEA_WEST = GeoPoint(41.80, 31.00)
    private val BLACK_SEA_MID = GeoPoint(42.30, 36.00)
    private val BLACK_SEA_EAST = GeoPoint(41.60, 41.20)

    private fun getTurkishSeaApproach(cityId: String, fromAegean: Boolean): List<GeoPoint> {
        return when (cityId) {
            "mersin" -> if (fromAegean) listOf(MED_CRETE_SOUTH, MED_CYPRUS_SOUTH, MED_LEVANT, MERSIN_SEA) else listOf(MED_LEVANT, MERSIN_SEA)
            "mugla" -> listOf(AEGEAN_SOUTH)
            "izmir" -> listOf(AEGEAN_SOUTH, AEGEAN_MID)
            "istanbul" -> listOf(AEGEAN_SOUTH, AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH, MARMARA_MID, BOSPORUS_SOUTH)
            "kocaeli", "bursa" -> listOf(AEGEAN_SOUTH, AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH, MARMARA_MID)
            "zonguldak" -> listOf(AEGEAN_SOUTH, AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH, MARMARA_MID, BOSPORUS_SOUTH, BOSPORUS_MID, BOSPORUS_NORTH, BLACK_SEA_WEST)
            "artvin" -> listOf(AEGEAN_SOUTH, AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH, MARMARA_MID, BOSPORUS_SOUTH, BOSPORUS_MID, BOSPORUS_NORTH, BLACK_SEA_WEST, BLACK_SEA_MID, BLACK_SEA_EAST)
            else -> listOf(MED_CRETE_SOUTH, AEGEAN_SOUTH, AEGEAN_MID, DARDANELLES_SOUTH, MARMARA_MID)
        }
    }

    // 🌊 MARITIME ROUTING (For Cargo Ships)
    fun getMaritimeRoute(originId: String, destId: String, originGeo: GeoPoint, destGeo: GeoPoint): List<GeoPoint> {
        val cacheKey = "${originId}_${destId}"
        return maritimeCache.getOrPut(cacheKey) {
            val isOriginPort = coastalPorts.contains(originId)
            val isDestPort = coastalPorts.contains(destId)
            val isOriginGlobal = !isTurkishCity(originId)
            val isDestGlobal = !isTurkishCity(destId)

            if (isOriginGlobal && !isDestGlobal) {
                val effectiveTürkiyeDest = if (isDestPort) destId else "mersin"
                val route = getGlobalToTürkiyeMaritimeRoute(originId, effectiveTürkiyeDest, originGeo, destGeo).toMutableList()
                if (!isDestPort) {
                    route.add(destGeo)
                }
                route
            } else if (!isOriginGlobal && isDestGlobal) {
                val effectiveTürkiyeOrigin = if (isOriginPort) originId else "mersin"
                val route = getGlobalToTürkiyeMaritimeRoute(destId, effectiveTürkiyeOrigin, destGeo, originGeo).reversed().toMutableList()
                if (!isOriginPort) {
                    route.add(0, originGeo)
                }
                route
            } else if (isOriginPort && isDestPort) {
                getInterTürkiyeMaritimeRoute(originId, destId, originGeo, destGeo)
            } else {
                listOf(originGeo, destGeo)
            }
        }
    }

    fun isTurkishCity(cityId: String): Boolean {
        val globalCities = setOf("santiago", "kuala_lumpur", "johannesburg", "basra", "sao_paulo", "essen", "frankfurt", "shanghai", "tokyo", "houston", "rotterdam", "london", "new_york")
        return !globalCities.contains(cityId)
    }

    private fun getGlobalToTürkiyeMaritimeRoute(globalId: String, türkiyeId: String, originGeo: GeoPoint, destGeo: GeoPoint): List<GeoPoint> {
        val path = mutableListOf<GeoPoint>()
        path.add(originGeo)

        when (globalId) {
            "basra" -> {
                // 🚢 Persian Gulf -> Hormuz -> Gulf of Oman -> Arabian Sea -> Gulf of Aden -> Red Sea -> Suez -> Med
                path.addAll(listOf(
                    BASRA_PORT, SHATT_AL_ARAB_MOUTH, PERSIAN_GULF_NORTH, PERSIAN_GULF_MID, PERSIAN_GULF_SOUTH,
                    STRAIT_OF_HORMUZ, GULF_OF_OMAN, ARABIAN_SEA_RAS_AL_HADD, ARABIAN_SEA_OMAN_SOUTH,
                    ARABIAN_SEA_YEMEN_EAST, GULF_OF_ADEN_EAST, GULF_OF_ADEN_MID, BAB_EL_MANDEB,
                    RED_SEA_SOUTH, RED_SEA_MID, RED_SEA_NORTH, RED_SEA_UPPER,
                    GULF_OF_SUEZ, SUEZ_CANAL_SOUTH, SUEZ_CANAL_NORTH, MED_LEVANT
                ))
                if (türkiyeId == "mersin") {
                    path.add(MERSIN_SEA)
                } else {
                    path.addAll(listOf(MED_CYPRUS_SOUTH, MED_CRETE_SOUTH))
                    path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
                }
            }

            "sao_paulo" -> {
                // 🚢 Brazil Coast -> Equator Atlantic -> Gibraltar -> Mediterranean
                path.addAll(listOf(
                    SAO_PAULO_COAST, BRAZIL_CABO_FRIO, BRAZIL_BAHIA, BRAZIL_EAST_CAPE,
                    EQUATOR_ATLANTIC, MID_NORTH_ATLANTIC, CANARY_ISLANDS_SEA, MOROCCO_OFFSHORE,
                    GULF_OF_CADIZ, STRAIT_OF_GIBRALTAR, MED_ALBORAN, MED_BALEARIC,
                    MED_SICILY_STRAIT, MED_IONIAN
                ))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }

            "santiago" -> {
                // 🚢 Pacific -> Panama Canal -> Caribbean -> Gibraltar -> Mediterranean
                path.addAll(listOf(
                    VALPARAISO_SEA, CHILE_NORTH, PERU_SEA, ECUADOR_SEA,
                    PANAMA_PACIFIC, PANAMA_CANAL, CARIBBEAN_WEST, CARIBBEAN_MID,
                    ATLANTIC_AZORES_SOUTH, GULF_OF_CADIZ, STRAIT_OF_GIBRALTAR,
                    MED_ALBORAN, MED_BALEARIC, MED_SICILY_STRAIT, MED_IONIAN
                ))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }

            "rotterdam" -> {
                // 🚢 North Sea -> English Channel -> Biscay -> Portugal -> Gibraltar -> Mediterranean
                path.addAll(listOf(
                    ROTTERDAM_APPROACH, STRAIT_OF_DOVER, ENGLISH_CHANNEL_MID, ENGLISH_CHANNEL_WEST,
                    BAY_OF_BISCAY, CAPE_FINISTERRE, PORTUGAL_OFFSHORE, CAPE_ST_VINCENT,
                    GULF_OF_CADIZ, STRAIT_OF_GIBRALTAR, MED_ALBORAN, MED_BALEARIC,
                    MED_SICILY_STRAIT, MED_IONIAN
                ))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }

            "london" -> {
                // 🚢 Thames Estuary -> English Channel -> Biscay -> Portugal -> Gibraltar -> Mediterranean
                path.addAll(listOf(
                    THAMES_ESTUARY, STRAIT_OF_DOVER, ENGLISH_CHANNEL_MID, ENGLISH_CHANNEL_WEST,
                    BAY_OF_BISCAY, CAPE_FINISTERRE, PORTUGAL_OFFSHORE, CAPE_ST_VINCENT,
                    GULF_OF_CADIZ, STRAIT_OF_GIBRALTAR, MED_ALBORAN, MED_BALEARIC,
                    MED_SICILY_STRAIT, MED_IONIAN
                ))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }

            "houston" -> {
                // 🚢 Gulf of Mexico -> Florida Straits -> Atlantic -> Gibraltar -> Mediterranean
                path.addAll(listOf(
                    HOUSTON_SEA, GULF_OF_MEXICO_MID, FLORIDA_STRAITS, ATLANTIC_BERMUDA,
                    MID_NORTH_ATLANTIC, GULF_OF_CADIZ, STRAIT_OF_GIBRALTAR, MED_ALBORAN,
                    MED_BALEARIC, MED_SICILY_STRAIT, MED_IONIAN
                ))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }

            "new_york" -> {
                // 🚢 New York Harbor -> US East Coast Offshore -> Mid North Atlantic -> Azores -> Gibraltar -> Mediterranean
                path.addAll(listOf(
                    NEW_YORK_SEA, ATLANTIC_NY_OFFSHORE, ATLANTIC_NORTH_MID, ATLANTIC_AZORES_NORTH,
                    GULF_OF_CADIZ, STRAIT_OF_GIBRALTAR, MED_ALBORAN, MED_BALEARIC,
                    MED_SICILY_STRAIT, MED_IONIAN
                ))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }

            "shanghai", "tokyo", "kuala_lumpur" -> {
                // 🚢 East Asia -> Malacca -> Indian Ocean -> Red Sea -> Suez Canal -> Mediterranean
                if (globalId == "tokyo") path.addAll(listOf(TOKYO_BAY, PACIFIC_JAPAN_SOUTH))
                if (globalId == "shanghai" || globalId == "tokyo") path.addAll(listOf(SHANGHAI_SEA, TAIWAN_STRAIT, SOUTH_CHINA_SEA))
                path.addAll(listOf(
                    SINGAPORE_STRAIT, STRAIT_OF_MALACCA, ANDAMAN_SEA, SRI_LANKA_SOUTH,
                    ARABIAN_SEA_MID, GULF_OF_ADEN_EAST, GULF_OF_ADEN_MID, BAB_EL_MANDEB,
                    RED_SEA_SOUTH, RED_SEA_MID, RED_SEA_NORTH, RED_SEA_UPPER,
                    GULF_OF_SUEZ, SUEZ_CANAL_SOUTH, SUEZ_CANAL_NORTH, MED_LEVANT
                ))
                if (türkiyeId == "mersin") {
                    path.add(MERSIN_SEA)
                } else {
                    path.addAll(listOf(MED_CYPRUS_SOUTH, MED_CRETE_SOUTH))
                    path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
                }
            }

            else -> {
                path.addAll(listOf(STRAIT_OF_GIBRALTAR, MED_ALBORAN, MED_BALEARIC, MED_SICILY_STRAIT, MED_IONIAN))
                path.addAll(getTurkishSeaApproach(türkiyeId, fromAegean = true))
            }
        }

        path.add(destGeo)
        return path
    }

    private fun getInterTürkiyeMaritimeRoute(originId: String, destId: String, originGeo: GeoPoint, destGeo: GeoPoint): List<GeoPoint> {
        val path = mutableListOf<GeoPoint>()
        path.add(originGeo)

        val isBlackSeaOrigin = originId == "zonguldak" || originId == "artvin"
        val isBlackSeaDest = destId == "zonguldak" || destId == "artvin"
        val isAegeanMarmaraOrigin = originId == "istanbul" || originId == "kocaeli" || originId == "bursa" || originId == "izmir" || originId == "mugla"
        val isAegeanMarmaraDest = destId == "istanbul" || destId == "kocaeli" || destId == "bursa" || destId == "izmir" || destId == "mugla"

        if (isBlackSeaOrigin && isAegeanMarmaraDest) {
            if (originId == "artvin") path.addAll(listOf(BLACK_SEA_EAST, BLACK_SEA_MID, BLACK_SEA_WEST))
            path.addAll(listOf(BOSPORUS_NORTH, BOSPORUS_MID, BOSPORUS_SOUTH, MARMARA_MID))
            if (destId == "izmir" || destId == "mugla") {
                path.addAll(listOf(DARDANELLES_NORTH, DARDANELLES_SOUTH, AEGEAN_NORTH, AEGEAN_MID))
                if (destId == "mugla") path.add(AEGEAN_SOUTH)
            }
        } else if (isAegeanMarmaraOrigin && isBlackSeaDest) {
            if (originId == "mugla") path.addAll(listOf(AEGEAN_SOUTH, AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH))
            else if (originId == "izmir") path.addAll(listOf(AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH))
            path.addAll(listOf(MARMARA_MID, BOSPORUS_SOUTH, BOSPORUS_MID, BOSPORUS_NORTH, BLACK_SEA_WEST))
            if (destId == "artvin") path.addAll(listOf(BLACK_SEA_MID, BLACK_SEA_EAST))
        } else if (originId == "mersin" || destId == "mersin") {
            val otherId = if (originId == "mersin") destId else originId
            val intermediate = listOf(MERSIN_SEA, MED_LEVANT, MED_CYPRUS_SOUTH, MED_CRETE_SOUTH, AEGEAN_SOUTH)
            if (originId == "mersin") {
                path.addAll(intermediate)
                if (otherId == "izmir") path.add(AEGEAN_MID)
                else if (otherId == "istanbul" || otherId == "kocaeli" || otherId == "bursa") {
                    path.addAll(listOf(AEGEAN_MID, AEGEAN_NORTH, DARDANELLES_SOUTH, DARDANELLES_NORTH, MARMARA_MID))
                }
            } else {
                if (otherId == "istanbul" || otherId == "kocaeli" || otherId == "bursa") {
                    path.addAll(listOf(MARMARA_MID, DARDANELLES_NORTH, DARDANELLES_SOUTH, AEGEAN_NORTH, AEGEAN_MID))
                } else if (otherId == "izmir") {
                    path.add(AEGEAN_MID)
                }
                path.addAll(listOf(AEGEAN_SOUTH, MED_CRETE_SOUTH, MED_CYPRUS_SOUTH, MED_LEVANT, MERSIN_SEA))
            }
        } else {
            path.add(GeoPoint((originGeo.latitude + destGeo.latitude) / 2.0, kotlin.math.min(originGeo.longitude, destGeo.longitude) - 0.7))
        }

        path.add(destGeo)
        return path
    }
}

class TycoonMapOverlay(
    private val context: android.content.Context,
    private val cities: List<CityProfile>,
    private val cityCoordinates: Map<String, GeoPoint>,
    var businesses: List<com.example.data.BusinessEntity>,
    var playerCurrentCity: String?,
    var playerId: String?,
    var selectedCityId: String?,
    var selectedCategoryFilter: String,
    var themePrimaryColor: Int,
    var themeSecondaryColor: Int,
    var themeSurfaceColor: Int,
    var themeTextColor: Int,
    var isEnglish: Boolean = false,
    var productionProgress: Map<String, Float> = emptyMap(),
    var productionDurations: Map<String, Long> = emptyMap(),
    var activeDeliveries: List<com.example.data.DeliveryItem> = emptyList(),
    var activeProductions: List<com.example.data.ActiveProduction> = emptyList(),
    var megaProjects: List<com.example.data.MegaProject> = emptyList(),
    var layersState: MapLayersState = MapLayersState(),
    var onCityTap: (CityProfile) -> Unit,
    var onFacilityTap: (com.example.data.BusinessEntity) -> Unit,
    var onEmptyCityBuildTap: (CityProfile) -> Unit
) : Overlay() {


    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = themeSecondaryColor
        isAntiAlias = true
    }

    private val inactiveLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = AndroidColor.argb(100, AndroidColor.red(themePrimaryColor), AndroidColor.green(themePrimaryColor), AndroidColor.blue(themePrimaryColor))
        isAntiAlias = true
    }

    private val pinPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = themeTextColor
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val facilityTextPaint = Paint().apply {
        color = themePrimaryColor
        textSize = 22f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val facilityBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val facilityBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // =========================================================================
    // 🎨 REALISTIC VECTOR GRAPHICS FOR LOGISTICS VEHICLES
    // =========================================================================

    private fun drawRealisticTruck(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        angleRad: Double,
        pulseProgress: Float,
        scale: Float = 1.0f
    ) {
        canvas.save()
        canvas.translate(cx, cy)
        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }
        canvas.rotate(Math.toDegrees(angleRad).toFloat())

        // 1. Shadow
        pinPaint.color = AndroidColor.argb(100, 10, 15, 25)
        canvas.drawRoundRect(RectF(-22f, -8f, 16f, 10f), 3f, 3f, pinPaint)

        // 2. Wheels
        pinPaint.color = AndroidColor.rgb(30, 30, 30)
        // Trailer rear
        canvas.drawRoundRect(RectF(-18f, -7.5f, -12f, -5f), 1f, 1f, pinPaint)
        canvas.drawRoundRect(RectF(-18f, 5f, -12f, 7.5f), 1f, 1f, pinPaint)
        // Trailer front
        canvas.drawRoundRect(RectF(-6f, -7.5f, -2f, -5f), 1f, 1f, pinPaint)
        canvas.drawRoundRect(RectF(-6f, 5f, -2f, 7.5f), 1f, 1f, pinPaint)
        // Cab wheels
        canvas.drawRoundRect(RectF(4f, -7f, 8f, -4f), 1f, 1f, pinPaint)
        canvas.drawRoundRect(RectF(4f, 4f, 8f, 7f), 1f, 1f, pinPaint)
        canvas.drawRoundRect(RectF(11f, -7f, 14f, -4f), 1f, 1f, pinPaint)
        canvas.drawRoundRect(RectF(11f, 4f, 14f, 7f), 1f, 1f, pinPaint)

        // 3. Trailer
        pinPaint.color = AndroidColor.rgb(236, 239, 241) // Light gray trailer
        canvas.drawRoundRect(RectF(-20f, -6f, 2f, 6f), 2f, 2f, pinPaint)
        // Trailer details
        pinPaint.color = themePrimaryColor
        canvas.drawRect(RectF(-18f, -2f, 0f, 2f), pinPaint) // Branding stripe
        strokePaint.color = AndroidColor.rgb(207, 216, 220)
        strokePaint.strokeWidth = 1f
        for (x in -18..0 step 3) {
            canvas.drawLine(x.toFloat(), -5.5f, x.toFloat(), 5.5f, strokePaint)
        }

        // 4. Cab
        pinPaint.color = themeSecondaryColor
        canvas.drawRoundRect(RectF(3f, -5.5f, 15f, 5.5f), 2.5f, 2.5f, pinPaint)
        // Cab details (Windshield)
        pinPaint.color = AndroidColor.rgb(33, 33, 33)
        canvas.drawRoundRect(RectF(9f, -4.5f, 13f, 4.5f), 1.5f, 1.5f, pinPaint)
        // Headlights beam
        val beamPath = android.graphics.Path().apply {
            moveTo(15f, -3f)
            lineTo(45f, -16f)
            lineTo(45f, 16f)
            moveTo(15f, 3f)
            close()
        }
        pinPaint.color = AndroidColor.argb(80, 255, 245, 157)
        canvas.drawPath(beamPath, pinPaint)
        // Headlights lenses
        pinPaint.color = AndroidColor.rgb(255, 255, 255)
        canvas.drawCircle(14f, -4f, 1.5f, pinPaint)
        canvas.drawCircle(14f, 4f, 1.5f, pinPaint)

        // Animated exhaust smoke puff
        val puffOffset = (pulseProgress * 12f)
        pinPaint.color = AndroidColor.argb((120 * (1f - pulseProgress)).toInt(), 200, 200, 200)
        canvas.drawCircle(3f - puffOffset, -7.5f - (puffOffset * 0.4f), 2f + (puffOffset * 0.5f), pinPaint)

        canvas.restore()
    }

    private fun drawRealisticShip(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        angleRad: Double,
        pulseProgress: Float,
        scale: Float = 1.0f
    ) {
        canvas.save()
        canvas.translate(cx, cy)
        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }
        canvas.rotate(Math.toDegrees(angleRad).toFloat())

        // 1. Wake
        val wakePath = android.graphics.Path().apply {
            moveTo(-18f, 0f)
            lineTo(-45f, -18f)
            lineTo(-38f, 0f)
            lineTo(-45f, 18f)
            close()
        }
        pinPaint.color = AndroidColor.argb(90, 178, 235, 242)
        canvas.drawPath(wakePath, pinPaint)
        
        // Expanding circular ripple wave pulses
        val rippleRadius = 16f + pulseProgress * 20f
        strokePaint.color = AndroidColor.argb(((1f - pulseProgress) * 150).toInt().coerceIn(0, 255), 224, 247, 250)
        strokePaint.strokeWidth = 2f
        canvas.drawCircle(-26f, 0f, rippleRadius, strokePaint)

        // 2. Hull
        val hullPath = android.graphics.Path().apply {
            moveTo(26f, 0f)
            lineTo(16f, 9f)
            lineTo(-22f, 8f)
            lineTo(-24f, 0f)
            lineTo(-22f, -8f)
            lineTo(16f, -9f)
            close()
        }
        pinPaint.color = AndroidColor.rgb(26, 35, 126) // Deep Navy
        canvas.drawPath(hullPath, pinPaint)

        // 3. Deck outline / Inner hull
        val deckPath = android.graphics.Path().apply {
            moveTo(22f, 0f)
            lineTo(14f, 7f)
            lineTo(-20f, 6f)
            lineTo(-21f, 0f)
            lineTo(-20f, -6f)
            lineTo(14f, -7f)
            close()
        }
        pinPaint.color = AndroidColor.rgb(176, 190, 197) // Gray deck
        canvas.drawPath(deckPath, pinPaint)

        // 4. Containers
        val colors = listOf(themePrimaryColor, themeSecondaryColor, AndroidColor.rgb(255, 87, 34), AndroidColor.rgb(76, 175, 80))
        for (i in -16..10 step 6) {
            for (j in -4..3 step 3) {
                pinPaint.color = colors[((i * 17 + j * 13) % colors.size + colors.size) % colors.size]
                canvas.drawRect(RectF(i.toFloat(), j.toFloat(), i + 5.5f, j + 2.5f), pinPaint)
            }
        }

        // 5. Bridge / Superstructure
        pinPaint.color = AndroidColor.rgb(250, 250, 250)
        canvas.drawRoundRect(RectF(-18f, -5f, -10f, 5f), 1f, 1f, pinPaint)
        pinPaint.color = AndroidColor.rgb(33, 150, 243) // Windows
        canvas.drawRect(RectF(-13f, -4f, -11f, 4f), pinPaint)

        // Radar
        val radarAng = pulseProgress * 2 * Math.PI
        val rx = -15f + Math.cos(radarAng).toFloat() * 2f
        val ry = Math.sin(radarAng).toFloat() * 2f
        pinPaint.color = AndroidColor.rgb(255, 235, 59)
        canvas.drawCircle(rx, ry, 1.5f, pinPaint)

        canvas.restore()
    }

    private fun drawRealisticTrain(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        angleRad: Double,
        pulseProgress: Float,
        trailingWagons: List<Pair<Float, Float>>,
        scale: Float = 1.0f
    ) {
        // Draw wagons first
        for (i in trailingWagons.indices) {
            val (wx, wy) = trailingWagons[i]
            canvas.save()
            canvas.translate(wx, wy)
            if (scale != 1.0f) {
                canvas.scale(scale, scale)
            }
            val wagonAngle = if (i == 0) angleRad else {
                val (prevX, prevY) = trailingWagons[i - 1]
                kotlin.math.atan2((wy - prevY).toDouble(), (wx - prevX).toDouble()) + Math.PI
            }
            canvas.rotate(Math.toDegrees(wagonAngle).toFloat())

            // Wagon Shadow & Tracks hint
            pinPaint.color = AndroidColor.argb(80, 10, 10, 10)
            canvas.drawRoundRect(RectF(-12f, -7f, 12f, 7f), 2f, 2f, pinPaint)

            // Wagon body
            pinPaint.color = if (i % 2 == 0) AndroidColor.rgb(141, 110, 99) else AndroidColor.rgb(120, 144, 156)
            canvas.drawRoundRect(RectF(-10f, -5.5f, 10f, 5.5f), 2f, 2f, pinPaint)

            // Cargo/Roof
            pinPaint.color = AndroidColor.rgb(55, 71, 79)
            canvas.drawRect(RectF(-8f, -4f, 8f, 4f), pinPaint)

            // Couplers
            strokePaint.color = AndroidColor.rgb(189, 189, 189)
            strokePaint.strokeWidth = 2.5f
            canvas.drawLine(-13f, 0f, -10f, 0f, strokePaint)
            canvas.drawLine(10f, 0f, 13f, 0f, strokePaint)
            
            canvas.restore()
        }

        // Draw Locomotive
        canvas.save()
        canvas.translate(cx, cy)
        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }
        canvas.rotate(Math.toDegrees(angleRad).toFloat())

        // Shadow & Tracks hint
        pinPaint.color = AndroidColor.argb(80, 10, 10, 10)
        canvas.drawRoundRect(RectF(-14f, -7f, 18f, 7f), 2f, 2f, pinPaint)

        // Locomotive Body
        pinPaint.color = themePrimaryColor
        canvas.drawRoundRect(RectF(-14f, -5.5f, 16f, 5.5f), 3f, 3f, pinPaint)

        // Hazard Stripes / Nose
        pinPaint.color = AndroidColor.rgb(255, 193, 7) // Amber
        canvas.drawRect(RectF(12f, -4f, 15.5f, 4f), pinPaint)

        // Cab Roof
        pinPaint.color = AndroidColor.rgb(69, 90, 100)
        canvas.drawRoundRect(RectF(-2f, -5f, 8f, 5f), 1f, 1f, pinPaint)
        
        // Cab Windows (cyan tint)
        pinPaint.color = AndroidColor.rgb(128, 222, 234)
        canvas.drawRect(RectF(4f, -4f, 7f, 4f), pinPaint) // Front windshield

        // Front Searchlight Beam
        val searchlightPath = android.graphics.Path().apply {
            moveTo(16f, 0f)
            lineTo(55f, -15f)
            lineTo(55f, 15f)
            close()
        }
        pinPaint.color = AndroidColor.argb(80, 255, 235, 59)
        canvas.drawPath(searchlightPath, pinPaint)

        // Exhaust Smoke
        pinPaint.color = AndroidColor.argb((150 * (1f - pulseProgress)).toInt(), 220, 220, 220)
        val puffX = -4f - (pulseProgress * 18f)
        val puffSize = 3f + (pulseProgress * 6f)
        canvas.drawCircle(puffX, -8f, puffSize, pinPaint)

        canvas.restore()
    }

    private fun drawRealisticJet(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        angleRad: Double,
        pulseProgress: Float,
        scale: Float = 1.0f
    ) {
        // High Altitude Elevation & Ground Shadow
        canvas.save()
        canvas.translate(cx + 12f * scale, cy + 18f * scale)
        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }
        canvas.rotate(Math.toDegrees(angleRad).toFloat())
        pinPaint.color = AndroidColor.argb(70, 15, 20, 30)
        
        val shadowWings = android.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(-12f, -22f)
            lineTo(-20f, -22f)
            lineTo(-10f, -4f)
            lineTo(-24f, -8f)
            lineTo(-26f, 0f)
            lineTo(-24f, 8f)
            lineTo(-10f, 4f)
            lineTo(-20f, 22f)
            lineTo(-12f, 22f)
            close()
        }
        canvas.drawPath(shadowWings, pinPaint)
        canvas.drawRoundRect(RectF(-22f, -4f, 20f, 4f), 4f, 4f, pinPaint)
        canvas.restore()

        // Jet Body
        canvas.save()
        canvas.translate(cx, cy)
        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }
        canvas.rotate(Math.toDegrees(angleRad).toFloat())

        // Main Wings & Stabilizers
        val wingsPath = android.graphics.Path().apply {
            moveTo(2f, 0f)
            lineTo(-12f, -24f)
            lineTo(-20f, -24f)
            lineTo(-10f, -4f)
            
            // Tail stabilizers
            lineTo(-24f, -10f)
            lineTo(-28f, -10f)
            lineTo(-26f, 0f)
            lineTo(-28f, 10f)
            lineTo(-24f, 10f)
            
            lineTo(-10f, 4f)
            lineTo(-20f, 24f)
            lineTo(-12f, 24f)
            close()
        }
        pinPaint.color = AndroidColor.rgb(236, 239, 241) // Jet White
        canvas.drawPath(wingsPath, pinPaint)
        strokePaint.color = AndroidColor.rgb(176, 190, 197)
        strokePaint.strokeWidth = 1f
        canvas.drawPath(wingsPath, strokePaint)

        // Engines (Jet Turbines)
        pinPaint.color = AndroidColor.rgb(69, 90, 100)
        canvas.drawRoundRect(RectF(-12f, -12f, -4f, -8f), 2f, 2f, pinPaint)
        canvas.drawRoundRect(RectF(-12f, 8f, -4f, 12f), 2f, 2f, pinPaint)
        
        // Thrust flame (Animated)
        pinPaint.color = AndroidColor.argb(200, 3, 169, 244)
        val flameLen = 4f + pulseProgress * 4f
        canvas.drawRoundRect(RectF(-12f - flameLen, -11.5f, -12f, -8.5f), 1f, 1f, pinPaint)
        canvas.drawRoundRect(RectF(-12f - flameLen, 8.5f, -12f, 11.5f), 1f, 1f, pinPaint)

        // Fuselage
        val fuselagePath = android.graphics.Path().apply {
            moveTo(24f, 0f) // Pointy nose
            lineTo(14f, 4.5f)
            lineTo(-26f, 4f)
            lineTo(-28f, 0f)
            lineTo(-26f, -4f)
            lineTo(14f, -4.5f)
            close()
        }
        pinPaint.color = AndroidColor.rgb(255, 255, 255)
        canvas.drawPath(fuselagePath, pinPaint)

        // Livery stripe
        pinPaint.color = themePrimaryColor
        canvas.drawRect(RectF(-20f, -1.5f, 12f, 1.5f), pinPaint)

        // Cockpit
        pinPaint.color = AndroidColor.rgb(33, 33, 33)
        canvas.drawRoundRect(RectF(14f, -2.5f, 19f, 2.5f), 1f, 1f, pinPaint)

        // Flashing beacon
        if (pulseProgress < 0.2f) {
            pinPaint.color = AndroidColor.rgb(255, 0, 0)
            canvas.drawCircle(0f, 0f, 2f, pinPaint)
        }

        canvas.restore()
    }

    private fun trLocal(trText: String, enText: String): String = if (isEnglish) enText else trText

    private fun drawLogisticsRoute(
        canvas: android.graphics.Canvas,
        proj: Projection,
        originCityId: String,
        destCityId: String,
        originGeo: GeoPoint,
        destGeo: GeoPoint,
        progress: Float,
        tagText: String,
        isConsortiumRoute: Boolean,
        isBorsaRoute: Boolean = false,
        pulseCycle: Float,
        dashPhase: Float,
        vehicleScale: Float = 1.0f
    ) {
        val originPt = Point()
        proj.toPixels(originGeo, originPt)
        val destPt = Point()
        proj.toPixels(destGeo, destPt)

        val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(originCityId)
        val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(destCityId)
        val isOriginRailway = GeoRoutePlanner.railwayCities.contains(originCityId)
        val isDestRailway = GeoRoutePlanner.railwayCities.contains(destCityId)
        val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(originCityId)
        val isDestGlobal = !GeoRoutePlanner.isTurkishCity(destCityId)
        val distMeters = originGeo.distanceToAsDouble(destGeo)

        val isGlobal = isOriginGlobal || isDestGlobal
        val isMaritimeRoute = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(originCityId, destCityId)))

        val isAirRoute = !isMaritimeRoute && (
                (isOriginGlobal || isDestGlobal) ||
                (distMeters >= 450_000.0 && GeoRoutePlanner.isAirHub(originCityId, destCityId))
        )

        val isRailwayRoute = !isMaritimeRoute && !isAirRoute && (isOriginRailway && isDestRailway)

        val cargoX: Float
        val cargoY: Float
        val vehicleAngle: Double

        when {
            isMaritimeRoute -> {
                val waypoints = GeoRoutePlanner.getMaritimeRoute(originCityId, destCityId, originGeo, destGeo)
                val screenPoints = waypoints.map { wp ->
                    val p = Point()
                    proj.toPixels(wp, p)
                    p
                }

                linePaint.color = when {
                    isBorsaRoute -> AndroidColor.rgb(0, 230, 118)
                    isConsortiumRoute -> AndroidColor.rgb(255, 215, 0)
                    else -> AndroidColor.rgb(0, 229, 255)
                }
                linePaint.strokeWidth = if (isConsortiumRoute || isBorsaRoute) (6.5f * vehicleScale.coerceIn(0.9f, 1.5f)) else (5f * vehicleScale.coerceIn(0.9f, 1.3f))
                linePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 18f), dashPhase)

                val seaPath = android.graphics.Path().apply {
                    if (screenPoints.isNotEmpty()) {
                        moveTo(screenPoints.first().x.toFloat(), screenPoints.first().y.toFloat())
                        for (i in 1 until screenPoints.size) {
                            val prev = screenPoints[i - 1]
                            val curr = screenPoints[i]
                            val midX = (prev.x + curr.x) / 2f
                            val midY = (prev.y + curr.y) / 2f
                            quadTo(prev.x.toFloat(), prev.y.toFloat(), midX, midY)
                        }
                        lineTo(screenPoints.last().x.toFloat(), screenPoints.last().y.toFloat())
                    }
                }
                canvas.drawPath(seaPath, linePaint)

                val segmentLengths = mutableListOf<Float>()
                var totalLength = 0f
                for (i in 0 until screenPoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    val dx = (p2.x - p1.x).toFloat()
                    val dy = (p2.y - p1.y).toFloat()
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    segmentLengths.add(len)
                    totalLength += len
                }

                fun getPointAndAngle(prog: Float): Triple<Float, Float, Double> {
                    if (screenPoints.size < 2 || totalLength <= 0f) {
                        return Triple(destPt.x.toFloat(), destPt.y.toFloat(), 0.0)
                    }
                    val targetDist = (prog.coerceIn(0f, 1f)) * totalLength
                    var accumulated = 0f
                    for (i in segmentLengths.indices) {
                        val segLen = segmentLengths[i]
                        if (targetDist <= accumulated + segLen || i == segmentLengths.lastIndex) {
                            val segProg = ((targetDist - accumulated) / segLen).coerceIn(0f, 1f)
                            val p1 = screenPoints[i]
                            val p2 = screenPoints[i + 1]
                            val x = p1.x + (p2.x - p1.x) * segProg
                            val y = p1.y + (p2.y - p1.y) * segProg
                            val ang = kotlin.math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                            return Triple(x, y, ang)
                        }
                        accumulated += segLen
                    }
                    val last = screenPoints.last()
                    return Triple(last.x.toFloat(), last.y.toFloat(), 0.0)
                }

                val currentPos = getPointAndAngle(progress)
                cargoX = currentPos.first
                cargoY = currentPos.second
                vehicleAngle = currentPos.third

                drawRealisticShip(canvas, cargoX, cargoY, vehicleAngle, pulseCycle, vehicleScale)
            }

            isRailwayRoute -> {
                val waypoints = GeoRoutePlanner.getRailwayRoute(originCityId, destCityId, originGeo, destGeo)
                val screenPoints = waypoints.map { wp ->
                    val p = Point()
                    proj.toPixels(wp, p)
                    p
                }

                linePaint.color = when {
                    isBorsaRoute -> AndroidColor.rgb(0, 230, 118)
                    isConsortiumRoute -> AndroidColor.rgb(255, 215, 0)
                    else -> AndroidColor.rgb(255, 179, 0)
                }
                linePaint.strokeWidth = if (isConsortiumRoute || isBorsaRoute) (6.5f * vehicleScale.coerceIn(0.9f, 1.5f)) else (5f * vehicleScale.coerceIn(0.9f, 1.3f))
                linePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), dashPhase)

                val railPath = android.graphics.Path().apply {
                    if (screenPoints.isNotEmpty()) {
                        moveTo(screenPoints.first().x.toFloat(), screenPoints.first().y.toFloat())
                        for (i in 1 until screenPoints.size) {
                            lineTo(screenPoints[i].x.toFloat(), screenPoints[i].y.toFloat())
                        }
                    }
                }
                canvas.drawPath(railPath, linePaint)

                val segmentLengths = mutableListOf<Float>()
                var totalLength = 0f
                for (i in 0 until screenPoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    val dx = (p2.x - p1.x).toFloat()
                    val dy = (p2.y - p1.y).toFloat()
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    segmentLengths.add(len)
                    totalLength += len
                }

                fun getRailPointAndAngle(prog: Float): Triple<Float, Float, Double> {
                    if (screenPoints.size < 2 || totalLength <= 0f) return Triple(destPt.x.toFloat(), destPt.y.toFloat(), 0.0)
                    val targetDist = (prog.coerceIn(0f, 1f)) * totalLength
                    var accumulated = 0f
                    for (i in segmentLengths.indices) {
                        val segLen = segmentLengths[i]
                        if (targetDist <= accumulated + segLen || i == segmentLengths.lastIndex) {
                            val segProg = ((targetDist - accumulated) / segLen).coerceIn(0f, 1f)
                            val p1 = screenPoints[i]
                            val p2 = screenPoints[i + 1]
                            val x = p1.x + (p2.x - p1.x) * segProg
                            val y = p1.y + (p2.y - p1.y) * segProg
                            val ang = kotlin.math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                            return Triple(x, y, ang)
                        }
                        accumulated += segLen
                    }
                    return Triple(screenPoints.last().x.toFloat(), screenPoints.last().y.toFloat(), 0.0)
                }

                val currentPos = getRailPointAndAngle(progress)
                cargoX = currentPos.first
                cargoY = currentPos.second
                vehicleAngle = currentPos.third

                val trailingWagons = (1..3).map { wagonIndex ->
                    val wProg = (progress - wagonIndex * 0.026f).let { if (it < 0f) it + 1f else it }
                    val wPoint = getRailPointAndAngle(wProg)
                    Pair(wPoint.first, wPoint.second)
                }

                drawRealisticTrain(canvas, cargoX, cargoY, vehicleAngle, pulseCycle, trailingWagons, vehicleScale)
            }

            isAirRoute -> {
                linePaint.color = when {
                    isBorsaRoute -> AndroidColor.rgb(0, 230, 118)
                    isConsortiumRoute -> AndroidColor.rgb(224, 64, 251)
                    else -> AndroidColor.rgb(129, 212, 250)
                }
                linePaint.strokeWidth = if (isConsortiumRoute || isBorsaRoute) (6.5f * vehicleScale.coerceIn(0.9f, 1.5f)) else (5f * vehicleScale.coerceIn(0.9f, 1.3f))
                linePaint.pathEffect = DashPathEffect(floatArrayOf(24f, 16f), dashPhase)
                
                val startX = originPt.x.toFloat()
                val startY = originPt.y.toFloat()
                val endX = destPt.x.toFloat()
                val endY = destPt.y.toFloat()
                
                // Calculate control point for a wide arc (offsetting perpendicular to the line)
                val dx = endX - startX
                val dy = endY - startY
                val ctrlX = startX + dx / 2f - dy * 0.25f
                val ctrlY = startY + dy / 2f + dx * 0.25f
                
                val airPath = android.graphics.Path().apply {
                    moveTo(startX, startY)
                    quadTo(ctrlX, ctrlY, endX, endY)
                }
                canvas.drawPath(airPath, linePaint)
                
                // Quadratic Bezier interpolation for position
                val t = progress.coerceIn(0f, 1f)
                val u = 1f - t
                cargoX = u * u * startX + 2f * u * t * ctrlX + t * t * endX
                cargoY = u * u * startY + 2f * u * t * ctrlY + t * t * endY
                
                // Derivative of Quadratic Bezier for tangent (angle)
                val vx = 2f * u * (ctrlX - startX) + 2f * t * (endX - ctrlX)
                val vy = 2f * u * (ctrlY - startY) + 2f * t * (endY - ctrlY)
                vehicleAngle = kotlin.math.atan2(vy.toDouble(), vx.toDouble())
                
                drawRealisticJet(canvas, cargoX, cargoY, vehicleAngle, pulseCycle, vehicleScale)
            }

            else -> {
                val waypoints = GeoRoutePlanner.getHighwayRoute(originCityId, destCityId, originGeo, destGeo)
                val screenPoints = waypoints.map { wp ->
                    val p = Point()
                    proj.toPixels(wp, p)
                    p
                }

                linePaint.color = when {
                    isBorsaRoute -> AndroidColor.rgb(0, 230, 118)
                    isConsortiumRoute -> AndroidColor.rgb(255, 215, 0)
                    else -> AndroidColor.rgb(255, 183, 77)
                }
                linePaint.strokeWidth = if (isConsortiumRoute || isBorsaRoute) (6.5f * vehicleScale.coerceIn(0.9f, 1.5f)) else (5f * vehicleScale.coerceIn(0.9f, 1.3f))
                linePaint.pathEffect = DashPathEffect(floatArrayOf(16f, 12f), dashPhase)

                val roadPath = android.graphics.Path().apply {
                    if (screenPoints.isNotEmpty()) {
                        moveTo(screenPoints.first().x.toFloat(), screenPoints.first().y.toFloat())
                        for (i in 1 until screenPoints.size) {
                            val prev = screenPoints[i - 1]
                            val curr = screenPoints[i]
                            val midX = (prev.x + curr.x) / 2f
                            val midY = (prev.y + curr.y) / 2f
                            quadTo(prev.x.toFloat(), prev.y.toFloat(), midX, midY)
                        }
                        lineTo(screenPoints.last().x.toFloat(), screenPoints.last().y.toFloat())
                    }
                }
                canvas.drawPath(roadPath, linePaint)

                val segmentLengths = mutableListOf<Float>()
                var totalLength = 0f
                for (i in 0 until screenPoints.size - 1) {
                    val p1 = screenPoints[i]
                    val p2 = screenPoints[i + 1]
                    val dx = (p2.x - p1.x).toFloat()
                    val dy = (p2.y - p1.y).toFloat()
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    segmentLengths.add(len)
                    totalLength += len
                }

                fun getRoadPointAndAngle(prog: Float): Triple<Float, Float, Double> {
                    if (screenPoints.size < 2 || totalLength <= 0f) return Triple(destPt.x.toFloat(), destPt.y.toFloat(), 0.0)
                    val targetDist = (prog.coerceIn(0f, 1f)) * totalLength
                    var accumulated = 0f
                    for (i in segmentLengths.indices) {
                        val segLen = segmentLengths[i]
                        if (targetDist <= accumulated + segLen || i == segmentLengths.lastIndex) {
                            val segProg = ((targetDist - accumulated) / segLen).coerceIn(0f, 1f)
                            val p1 = screenPoints[i]
                            val p2 = screenPoints[i + 1]
                            val x = p1.x + (p2.x - p1.x) * segProg
                            val y = p1.y + (p2.y - p1.y) * segProg
                            val ang = kotlin.math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                            return Triple(x, y, ang)
                        }
                        accumulated += segLen
                    }
                    return Triple(screenPoints.last().x.toFloat(), screenPoints.last().y.toFloat(), 0.0)
                }

                val currentPos = getRoadPointAndAngle(progress)
                cargoX = currentPos.first
                cargoY = currentPos.second
                vehicleAngle = currentPos.third

                drawRealisticTruck(canvas, cargoX, cargoY, vehicleAngle, pulseCycle, vehicleScale)
            }
        }

        // Pulsing Radar Ring around vehicle badge
        val ringColor = when {
            isBorsaRoute -> AndroidColor.rgb(0, 230, 118)
            isConsortiumRoute -> AndroidColor.rgb(255, 215, 0)
            else -> themeSecondaryColor
        }
        pinPaint.alpha = 255
        pinPaint.color = AndroidColor.argb(80, AndroidColor.red(ringColor), AndroidColor.green(ringColor), AndroidColor.blue(ringColor))
        canvas.drawCircle(cargoX, cargoY, 16f * vehicleScale, pinPaint)

        // Dynamic Floating HUD Badge (Scales smoothly with zoom)
        val badgeTextSize = (18f * (0.85f + vehicleScale * 0.15f)).coerceIn(16f, 26f)
        facilityTextPaint.textSize = badgeTextSize
        val tagWidth = facilityTextPaint.measureText(tagText)
        val padH = 12f * vehicleScale.coerceIn(0.9f, 1.4f)
        val padV = 14f * vehicleScale.coerceIn(0.9f, 1.4f)
        val offsetY = 32f * vehicleScale.coerceIn(0.9f, 1.5f)

        val badgeRect = RectF(
            cargoX - tagWidth / 2 - padH,
            cargoY - offsetY - padV,
            cargoX + tagWidth / 2 + padH,
            cargoY - offsetY + padV
        )
        facilityBgPaint.color = themeSurfaceColor
        canvas.drawRoundRect(badgeRect, 8f, 8f, facilityBgPaint)
        facilityBorderPaint.color = when {
            isBorsaRoute -> AndroidColor.rgb(0, 230, 118)
            isConsortiumRoute -> AndroidColor.rgb(255, 215, 0)
            else -> themePrimaryColor
        }
        facilityBorderPaint.strokeWidth = 2.5f * vehicleScale.coerceIn(0.9f, 1.4f)
        canvas.drawRoundRect(badgeRect, 8f, 8f, facilityBorderPaint)
        facilityTextPaint.color = themeTextColor
        canvas.drawText(tagText, cargoX - tagWidth / 2, cargoY - offsetY + (badgeTextSize * 0.35f), facilityTextPaint)
    }

    override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val time = System.currentTimeMillis()
        val pulseCycle = (time % 2400L) / 2400f
        val pulseAlpha = if (pulseCycle < 0.5f) {
            0.3f + (pulseCycle * 2f) * 0.55f
        } else {
            0.85f - ((pulseCycle - 0.5f) * 2f) * 0.55f
        }
        val dashPhase = 100f - ((time % 4500L) / 4500f) * 100f
        val proj = mapView.projection

        // Smooth dynamic vehicle scaling as user zooms in
        // Zoom 5.0 -> scale ~0.75x
        // Zoom 6.0 -> scale 1.0x
        // Zoom 8.0 -> scale 1.55x
        // Zoom 10.0 -> scale 2.1x
        // Zoom 12.0 -> scale 2.7x
        // Zoom 14.0+ -> scale 3.2x
        val currentZoom = mapView.zoomLevelDouble.toFloat()
        val vehicleScale = (1.0f + ((currentZoom - 6.0f) * 0.28f)).coerceIn(0.75f, 3.4f)

        // 1. Draw logistics routes and vehicles
        if (layersState.showDeliveries) {
            val effectivePlayerCity = if (!playerCurrentCity.isNullOrBlank()) playerCurrentCity!! else "istanbul"
        val centerGeo = cityCoordinates[effectivePlayerCity] ?: cityCoordinates["istanbul"]
        if (centerGeo != null) {
            val centerPt = Point()
            proj.toPixels(centerGeo, centerPt)

            // A. TIER 1, 2, 3 FACILITIES ➔ DELIVER TO CENTRAL WAREHOUSE (MERKEZ DEPO)
            businesses.forEach { facilityInCity ->
                if (facilityInCity.cityId != effectivePlayerCity) {
                    val originGeo = cityCoordinates[facilityInCity.cityId]
                    if (originGeo != null) {
                        val product = Product.values().find { it.facilityId == facilityInCity.type || it.id == facilityInCity.type }
                        val prodDurationMs = (product?.let { productionDurations[it.id] })
                            ?: productionDurations[facilityInCity.type]
                            ?: productionDurations[facilityInCity.id.toString()]
                            ?: (product?.tier?.baseDurationMs ?: 45_000L)

                        val rawProgress = (product?.let { productionProgress[it.id] })
                            ?: productionProgress[facilityInCity.id.toString()]
                            ?: productionProgress[facilityInCity.type]
                            ?: ((time % prodDurationMs.coerceAtLeast(5000L)).toFloat() / prodDurationMs.coerceAtLeast(5000L).toFloat())

                        val syncedProgress = rawProgress.coerceIn(0f, 1f)
                        val remainingSecs = (((1f - syncedProgress) * prodDurationMs) / 1000f).toInt().coerceAtLeast(0)
                        val timeFormatted = String.format("%02d:%02d", remainingSecs / 60, remainingSecs % 60)
                        val prodDisplayName = product?.getDisplayName(isEnglish)?.take(14) ?: facilityInCity.type.uppercase().take(12)
                        val tierNum = when (product?.tier) {
                            ProductTier.TIER_1 -> 1
                            ProductTier.TIER_2 -> 2
                            ProductTier.TIER_3 -> 3
                            ProductTier.TIER_4 -> 4
                            null -> 1
                        }

                        // Determine transport emoji
                        val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(facilityInCity.cityId)
                        val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(effectivePlayerCity)
                        val isOriginRailway = GeoRoutePlanner.railwayCities.contains(facilityInCity.cityId)
                        val isDestRailway = GeoRoutePlanner.railwayCities.contains(effectivePlayerCity)
                        val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(facilityInCity.cityId)
                        val isDestGlobal = !GeoRoutePlanner.isTurkishCity(effectivePlayerCity)
                        val distMeters = originGeo.distanceToAsDouble(centerGeo)
                        val isGlobal = isOriginGlobal || isDestGlobal
                        val isMaritime = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(facilityInCity.cityId, effectivePlayerCity)))
                        val isAir = !isMaritime && ((isOriginGlobal || isDestGlobal) || (distMeters >= 450_000.0 && GeoRoutePlanner.isAirHub(facilityInCity.cityId, effectivePlayerCity)))
                        val isRail = !isMaritime && !isAir && (isOriginRailway && isDestRailway)
                        val emoji = when {
                            isMaritime -> "🚢"
                            isAir -> "✈️"
                            isRail -> "🚂"
                            else -> "🚚"
                        }

                        val tagText = "$emoji $prodDisplayName (K$tierNum) ➔ 🏠 MERKEZ DEPO • %${(syncedProgress * 100).toInt()} ($timeFormatted)"

                        drawLogisticsRoute(
                            canvas = canvas,
                            proj = proj,
                            originCityId = facilityInCity.cityId,
                            destCityId = effectivePlayerCity,
                            originGeo = originGeo,
                            destGeo = centerGeo,
                            progress = syncedProgress,
                            tagText = tagText,
                            isConsortiumRoute = false,
                            isBorsaRoute = false,
                            pulseCycle = pulseCycle,
                            dashPhase = dashPhase,
                            vehicleScale = vehicleScale
                        )
                    }
                }
            }

            // B. CENTRAL WAREHOUSE (MERKEZ DEPO) ➔ DELIVER TO CONSORTIUMS (KONSORSİYUM / TIER 4)
            megaProjects.forEach { projItem ->
                val isPlayerInvolved = playerId != null && (projItem.leaderPlayerId == playerId || projItem.slots.any { it.assignedPartnerId == playerId })
                if (projItem.cityId != effectivePlayerCity && isPlayerInvolved) {
                    val destGeo = cityCoordinates[projItem.cityId]
                    if (destGeo != null) {
                        val durationMs = (projItem.standardBatchDurationSeconds * 1000L).coerceAtLeast(10000L)
                        val prog = if (projItem.isBatchInProduction) {
                            projItem.productionProgressFraction
                        } else {
                            ((time % durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        }
                        val remainingSecs = (((1f - prog) * durationMs) / 1000f).toInt().coerceAtLeast(0)
                        val timeFormatted = String.format("%02d:%02d", remainingSecs / 60, remainingSecs % 60)
                        val projName = projItem.targetProductName.take(12)

                        // Determine transport emoji
                        val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(effectivePlayerCity)
                        val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(projItem.cityId)
                        val isOriginRailway = GeoRoutePlanner.railwayCities.contains(effectivePlayerCity)
                        val isDestRailway = GeoRoutePlanner.railwayCities.contains(projItem.cityId)
                        val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(effectivePlayerCity)
                        val isDestGlobal = !GeoRoutePlanner.isTurkishCity(projItem.cityId)
                        val distMeters = centerGeo.distanceToAsDouble(destGeo)
                        val isGlobal = isOriginGlobal || isDestGlobal
                        val isMaritime = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(effectivePlayerCity, projItem.cityId)))
                        val isAir = !isMaritime && ((isOriginGlobal || isDestGlobal) || (distMeters >= 450_000.0 && GeoRoutePlanner.isAirHub(effectivePlayerCity, projItem.cityId)))
                        val isRail = !isMaritime && !isAir && (isOriginRailway && isDestRailway)
                        val emoji = when {
                            isMaritime -> "🚢"
                            isAir -> "✈️"
                            isRail -> "🚂"
                            else -> "🚚"
                        }

                        val tagText = "🚀 🏠 MERKEZ DEPO ➔ 👑 $projName • %${(prog * 100).toInt()} ($timeFormatted)"

                        drawLogisticsRoute(
                            canvas = canvas,
                            proj = proj,
                            originCityId = effectivePlayerCity,
                            destCityId = projItem.cityId,
                            originGeo = centerGeo,
                            destGeo = destGeo,
                            progress = prog,
                            tagText = tagText,
                            isConsortiumRoute = true,
                            isBorsaRoute = false,
                            pulseCycle = pulseCycle,
                            dashPhase = dashPhase,
                            vehicleScale = vehicleScale
                        )
                    }
                }
            }

            // C. Inactive background connection lines to unconnected cities
            cities.forEach { otherCity ->
                if (otherCity.id != effectivePlayerCity && businesses.none { it.cityId == otherCity.id } && megaProjects.none { it.cityId == otherCity.id && (playerId != null && (it.leaderPlayerId == playerId || it.slots.any { slot -> slot.assignedPartnerId == playerId })) }) {
                    val otherGeo = cityCoordinates[otherCity.id]
                    if (otherGeo != null) {
                        val otherPt = Point()
                        proj.toPixels(otherGeo, otherPt)
                        canvas.drawLine(centerPt.x.toFloat(), centerPt.y.toFloat(), otherPt.x.toFloat(), otherPt.y.toFloat(), inactiveLinePaint)
                    }
                }
            }
        }

        // 2. BORSA DEPOLARI VE MERKEZ DEPO ARASI LOJİSTİK HAREKETLER (BORSA SEVKİYATLARI)
        activeDeliveries.forEach { delivery ->
            val originGeo = cityCoordinates[delivery.originCityId]
            val destGeo = cityCoordinates[delivery.destinationCityId]
            if (originGeo != null && destGeo != null) {
                val elapsed = (time - delivery.startTimeMs).coerceAtLeast(0L)
                val deliveryProg = if (delivery.totalDurationMs > 0) {
                    (elapsed.toFloat() / delivery.totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 1f

                val remainingSecs = (((1f - deliveryProg) * delivery.totalDurationMs) / 1000f).toInt().coerceAtLeast(0)
                val timeFormatted = String.format("%02d:%02d", remainingSecs / 60, remainingSecs % 60)
                val product = Product.values().find { it.id == delivery.itemId || it.facilityId == delivery.itemId }
                val prodDisplayName = product?.getDisplayName(isEnglish)?.take(12) ?: delivery.itemId.uppercase().take(10)

                val isBorsaDelivery = delivery.originCityId == "istanbul" || delivery.originCityId == "new_york" ||
                        delivery.destinationCityId == "istanbul" || delivery.destinationCityId == "new_york"

                val originName = when (delivery.originCityId) {
                    "istanbul" -> "🏛️ BIST"
                    "new_york" -> "🏛️ NYSE"
                    playerCurrentCity -> "🏠 MERKEZ"
                    else -> cities.find { it.id == delivery.originCityId }?.getDisplayName(context)?.take(8) ?: delivery.originCityId.uppercase().take(8)
                }

                val destName = when {
                    delivery.isConsortiumDelivery -> "🏭 KONSORSİYUM"
                    delivery.destinationCityId == "istanbul" -> "🏛️ BIST"
                    delivery.destinationCityId == "new_york" -> "🏛️ NYSE"
                    delivery.destinationCityId == playerCurrentCity -> "🏠 MERKEZ"
                    else -> cities.find { it.id == delivery.destinationCityId }?.getDisplayName(context)?.take(8) ?: delivery.destinationCityId.uppercase().take(8)
                }

                // Determine transport emoji
                val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(delivery.originCityId)
                val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(delivery.destinationCityId)
                val isOriginRailway = GeoRoutePlanner.railwayCities.contains(delivery.originCityId)
                val isDestRailway = GeoRoutePlanner.railwayCities.contains(delivery.destinationCityId)
                val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(delivery.originCityId)
                val isDestGlobal = !GeoRoutePlanner.isTurkishCity(delivery.destinationCityId)
                val distMeters = originGeo.distanceToAsDouble(destGeo)
                val isGlobal = isOriginGlobal || isDestGlobal
                val isMaritime = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(delivery.originCityId, delivery.destinationCityId)))
                val isAir = !isMaritime && ((isOriginGlobal || isDestGlobal) || (distMeters >= 450_000.0 && GeoRoutePlanner.isAirHub(delivery.originCityId, delivery.destinationCityId)))
                val isRail = !isMaritime && !isAir && (isOriginRailway && isDestRailway)
                val emoji = when {
                    isMaritime -> "🚢"
                    isAir -> "✈️"
                    isRail -> "🚂"
                    else -> "🚚"
                }

                val actionIcon = if (delivery.isOutboundSale) "💰 SATIŞ" else "📦 ALIŞ"
                val tagText = "$actionIcon $emoji $originName ➔ $destName: ${delivery.quantity}T $prodDisplayName • %${(deliveryProg * 100).toInt()} ($timeFormatted)"

                drawLogisticsRoute(
                    canvas = canvas,
                    proj = proj,
                    originCityId = delivery.originCityId,
                    destCityId = delivery.destinationCityId,
                    originGeo = originGeo,
                    destGeo = destGeo,
                    progress = deliveryProg,
                    tagText = tagText,
                    isConsortiumRoute = false,
                    isBorsaRoute = isBorsaDelivery,
                    pulseCycle = pulseCycle,
                    dashPhase = dashPhase,
                    vehicleScale = vehicleScale
                )
            }
        }

        // 3. TESİSLERDEKİ AKTİF ÜRETİM LOJİSTİK SİMÜLASYONU (ÜRETİLEN ŞEHİRDEN MERKEZ DEPOYA)
        activeProductions.forEach { prod ->
            val effectiveTargetCity = if (prod.targetCityId.isNotBlank()) prod.targetCityId else (playerCurrentCity ?: "istanbul")
            val originGeo = cityCoordinates[prod.cityId]
            val destGeo = cityCoordinates[effectiveTargetCity]
            if (originGeo != null && destGeo != null) {
                val elapsed = (time - prod.startTimeMs).coerceAtLeast(0L)
                val prodProg = if (prod.totalDurationMs > 0) {
                    (elapsed.toFloat() / prod.totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 1f

                val remainingSecs = (((1f - prodProg) * prod.totalDurationMs) / 1000f).toInt().coerceAtLeast(0)
                val timeFormatted = String.format("%02d:%02d", remainingSecs / 60, remainingSecs % 60)
                val product = Product.values().find { it.id == prod.productId || it.facilityId == prod.productId }
                val prodDisplayName = product?.getDisplayName(isEnglish)?.take(12) ?: prod.productId.uppercase().take(10)

                val originName = cities.find { it.id == prod.cityId }?.getDisplayName(context)?.take(8) ?: prod.cityId.uppercase().take(8)
                val destName = if (effectiveTargetCity == playerCurrentCity) "🏠 MERKEZ" else (cities.find { it.id == effectiveTargetCity }?.getDisplayName(context)?.take(8) ?: effectiveTargetCity.uppercase().take(8))

                val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(prod.cityId)
                val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(effectiveTargetCity)
                val isOriginRailway = GeoRoutePlanner.railwayCities.contains(prod.cityId)
                val isDestRailway = GeoRoutePlanner.railwayCities.contains(effectiveTargetCity)
                val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(prod.cityId)
                val isDestGlobal = !GeoRoutePlanner.isTurkishCity(effectiveTargetCity)
                val distMeters = originGeo.distanceToAsDouble(destGeo)
                val isGlobal = isOriginGlobal || isDestGlobal
                val isMaritime = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(prod.cityId, effectiveTargetCity)))
                val isAir = !isMaritime && ((isOriginGlobal || isDestGlobal) || (distMeters >= 450_000.0 && GeoRoutePlanner.isAirHub(prod.cityId, effectiveTargetCity)))
                val isRail = !isMaritime && !isAir && (isOriginRailway && isDestRailway)
                val emoji = when {
                    isMaritime -> "🚢"
                    isAir -> "✈️"
                    isRail -> "🚂"
                    else -> "🚚"
                }

                val tagText = "🏭 $emoji $originName ➔ $destName: ${prod.quantity}T $prodDisplayName • %${(prodProg * 100).toInt()} ($timeFormatted)"

                drawLogisticsRoute(
                    canvas = canvas,
                    proj = proj,
                    originCityId = prod.cityId,
                    destCityId = effectiveTargetCity,
                    originGeo = originGeo,
                    destGeo = destGeo,
                    progress = prodProg,
                    tagText = tagText,
                    isConsortiumRoute = false,
                    isBorsaRoute = false,
                    pulseCycle = pulseCycle,
                    dashPhase = dashPhase,
                    vehicleScale = vehicleScale
                )
            }
        }
        }

        // Draw cities
        cities.forEach { city ->
            val geo = cityCoordinates[city.id] ?: return@forEach
            val pt = Point()
            proj.toPixels(geo, pt)

            val isSelected = city.id == selectedCityId
            val isCenter = city.id == playerCurrentCity
            val activeConsortiumInCity = if (layersState.showMegaProjects) megaProjects.firstOrNull { it.cityId == city.id } else null

            val matchesFilter = when (selectedCategoryFilter) {
                "my_facilities" -> businesses.any { it.cityId == city.id }
                "turkey" -> !city.isGlobal
                "global" -> city.isGlobal
                "tier_1" -> city.maxFacilityTier == 1
                "tier_2" -> city.maxFacilityTier == 2
                "tier_3" -> city.maxFacilityTier == 3
                "tier_4" -> city.maxFacilityTier == 4 || activeConsortiumInCity != null
                else -> true
            }

            val baseAlpha = if (matchesFilter) 255 else 80

            if (isCenter) {
                // 🏠 Central Warehouse (Merkez Depo) Beacon
                pinPaint.color = AndroidColor.argb((pulseAlpha * 140).toInt(), 0, 230, 118)
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 34f, pinPaint)
                
                pinPaint.color = AndroidColor.argb(baseAlpha, 0, 230, 118)
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 18f, pinPaint)
                pinPaint.color = AndroidColor.rgb(255, 255, 255)
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 8f, pinPaint)
            } else if (activeConsortiumInCity != null) {
                // 🚀 Active Consortium City Badge
                pinPaint.color = AndroidColor.argb((pulseAlpha * 180).toInt(), 255, 215, 0)
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 32f, pinPaint)
                
                pinPaint.color = AndroidColor.argb(baseAlpha, 255, 215, 0)
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 17f, pinPaint)
                pinPaint.color = AndroidColor.rgb(255, 255, 255)
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 7f, pinPaint)
            } else if (isSelected) {
                pinPaint.color = AndroidColor.argb((pulseAlpha * 180).toInt(), AndroidColor.red(themeSecondaryColor), AndroidColor.green(themeSecondaryColor), AndroidColor.blue(themeSecondaryColor))
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 30f, pinPaint)
                
                pinPaint.color = AndroidColor.argb(baseAlpha, AndroidColor.red(themeSecondaryColor), AndroidColor.green(themeSecondaryColor), AndroidColor.blue(themeSecondaryColor))
                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 16f, pinPaint)
            } else {
                val cityBusinesses = businesses.filter { it.cityId == city.id }
                if (cityBusinesses.isNotEmpty()) {
                    pinPaint.color = AndroidColor.argb(baseAlpha, AndroidColor.red(themePrimaryColor), AndroidColor.green(themePrimaryColor), AndroidColor.blue(themePrimaryColor))
                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 18f, pinPaint)
                    pinPaint.color = AndroidColor.rgb(255, 215, 0)
                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 10f, pinPaint)
                    pinPaint.color = AndroidColor.rgb(255, 255, 255)
                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 4f, pinPaint)
                } else {
                    pinPaint.color = AndroidColor.argb(baseAlpha, 150, 150, 150)
                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 9f, pinPaint)
                }
            }

            // City Label
            textPaint.alpha = baseAlpha
            textPaint.textSize = if (isSelected || isCenter || activeConsortiumInCity != null) 34f else 26f
            val cityBusinesses = businesses.filter { it.cityId == city.id }
            val name = when {
                isCenter && cityBusinesses.isNotEmpty() -> "🏠 ${city.getDisplayName(context)} (" + trLocal("MERKEZ DEPO", "CENTRAL WAREHOUSE") + ") [🏭 ${cityBusinesses.size}]"
                isCenter -> "🏠 ${city.getDisplayName(context)} (" + trLocal("MERKEZ DEPO", "CENTRAL WAREHOUSE") + ")"
                activeConsortiumInCity != null -> "🚀 ${city.getDisplayName(context)} [👑 " + trLocal("KONSORSİYUM", "CONSORTIUM") + "]"
                cityBusinesses.isNotEmpty() -> "🏭 ${city.getDisplayName(context)} [${cityBusinesses.size} " + trLocal("Tesis", "Fac.") + "]"
                else -> city.getDisplayName(context)
            }
            canvas.drawText(name, pt.x.toFloat() + 16f, pt.y.toFloat() + 8f, textPaint)
        }

        // Draw Facility Icons & Add Facility Buttons when zoomed in
        val zoom = mapView.zoomLevelDouble
        val facilityZoomThreshold = 5.2
        if (layersState.showFacilities && zoom >= facilityZoomThreshold) {
            cities.forEach { city ->
                val geo = cityCoordinates[city.id] ?: return@forEach
                val pt = Point()
                proj.toPixels(geo, pt)

                val cityBusinesses = businesses.filter { it.cityId == city.id }
                if (cityBusinesses.isNotEmpty()) {
                    val radius = 70f + ((zoom - facilityZoomThreshold) * 30f).toFloat()
                    cityBusinesses.forEachIndexed { index, business ->
                        val angle = index * (2 * Math.PI / cityBusinesses.size) - (Math.PI / 2)
                        val bx = pt.x + (radius * Math.cos(angle)).toFloat()
                        val by = pt.y + (radius * Math.sin(angle)).toFloat()

                        canvas.drawLine(pt.x.toFloat(), pt.y.toFloat(), bx, by, inactiveLinePaint)

                        val product = Product.values().find { it.facilityId == business.type }
                        val name = product?.getFacilityName(isEnglish)?.uppercase() ?: business.type.uppercase()
                        val label = "🏭 $name LVL ${business.level}"
                        val textWidth = facilityTextPaint.measureText(label)

                        facilityBgPaint.color = themeSurfaceColor
                        facilityBorderPaint.color = themePrimaryColor

                        canvas.drawRoundRect(
                            RectF(bx - textWidth / 2 - 16f, by - 26f, bx + textWidth / 2 + 16f, by + 12f),
                            12f, 12f, facilityBgPaint
                        )
                        canvas.drawRoundRect(
                            RectF(bx - textWidth / 2 - 16f, by - 26f, bx + textWidth / 2 + 16f, by + 12f),
                            12f, 12f, facilityBorderPaint
                        )
                        
                        facilityTextPaint.color = themeSecondaryColor
                        canvas.drawText(label, bx - textWidth / 2, by, facilityTextPaint)
                    }
                } else {
                    val label = "➕ " + trLocal("TESİS KUR", "BUILD FACILITY")
                    val textWidth = facilityTextPaint.measureText(label)
                    val bx = pt.x.toFloat()
                    val by = pt.y.toFloat() - 50f

                    facilityBgPaint.color = themeSurfaceColor
                    facilityBorderPaint.color = themeSecondaryColor

                    canvas.drawRoundRect(
                        RectF(bx - textWidth / 2 - 16f, by - 26f, bx + textWidth / 2 + 16f, by + 12f),
                        12f, 12f, facilityBgPaint
                    )
                    canvas.drawRoundRect(
                        RectF(bx - textWidth / 2 - 16f, by - 26f, bx + textWidth / 2 + 16f, by + 12f),
                        12f, 12f, facilityBorderPaint
                    )
                    
                    facilityTextPaint.color = themeSecondaryColor
                    canvas.drawText(label, bx - textWidth / 2, by, facilityTextPaint)
                }
            }
        }
        
        if (activeDeliveries.isNotEmpty()) {
            mapView.postInvalidateDelayed(150L)
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val proj = mapView.projection
        val zoom = mapView.zoomLevelDouble
        val facilityZoomThreshold = 5.2
        
        if (zoom >= facilityZoomThreshold) {
            for (city in cities) {
                val geo = cityCoordinates[city.id] ?: continue
                val pt = Point()
                proj.toPixels(geo, pt)
                
                val cityBusinesses = businesses.filter { it.cityId == city.id }
                if (cityBusinesses.isNotEmpty()) {
                    val radius = 70f + ((zoom - facilityZoomThreshold) * 30f).toFloat()
                    for ((index, business) in cityBusinesses.withIndex()) {
                        val angle = index * (2 * Math.PI / cityBusinesses.size) - (Math.PI / 2)
                        val bx = pt.x + (radius * Math.cos(angle)).toFloat()
                        val by = pt.y + (radius * Math.sin(angle)).toFloat()
                        
                        val product = Product.values().find { it.facilityId == business.type }
                        val name = product?.getFacilityName(isEnglish)?.uppercase() ?: business.type.uppercase()
                        val label = "🏭 $name LVL ${business.level}"
                        val textWidth = facilityTextPaint.measureText(label)
                        
                        val rect = RectF(bx - textWidth / 2 - 16f, by - 26f, bx + textWidth / 2 + 16f, by + 12f)
                        if (rect.contains(e.x, e.y)) {
                            onFacilityTap(business)
                            return true
                        }
                    }
                } else {
                    val label = "➕ " + tr("TESİS KUR", "BUILD FACILITY", isEnglish)
                    val textWidth = facilityTextPaint.measureText(label)
                    val bx = pt.x.toFloat()
                    val by = pt.y.toFloat() - 50f
                    val rect = RectF(bx - textWidth / 2 - 16f, by - 26f, bx + textWidth / 2 + 16f, by + 12f)
                    if (rect.contains(e.x, e.y)) {
                        onEmptyCityBuildTap(city)
                        return true
                    }
                }
            }
        }
        
        for (city in cities) {
            val geo = cityCoordinates[city.id] ?: continue
            val pt = Point()
            proj.toPixels(geo, pt)
            val dx = e.x - pt.x
            val dy = e.y - pt.y
            if (dx * dx + dy * dy < 2500) { // 50px radius
                onCityTap(city)
                return true
            }
        }
        return super.onSingleTapConfirmed(e, mapView)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityMapScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateToMarket: () -> Unit = {},
    onNavigateToBorsa: () -> Unit = {},
    onNavigateToProduction: () -> Unit = {},
    onNavigateToHr: () -> Unit = {},
    onNavigateToBank: () -> Unit = {}
) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = context.packageName
    try {
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 10L * 1024L * 1024L
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 8L * 1024L * 1024L
    } catch (_: Throwable) {}
    val haptic = LocalHapticFeedback.current
    val themeOption = LocalAppThemeOption.current

    var selectedCity by remember { mutableStateOf<CityProfile?>(null) }
    var selectedFacility by remember { mutableStateOf<com.example.data.BusinessEntity?>(null) }
    var showBuildDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var travelOriginAndDest by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingTravelCityId by remember { mutableStateOf<String?>(null) }
    var mapLayersState by remember { mutableStateOf(MapLayersState()) }
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    var dropdownQuery by remember { mutableStateOf("") }
    val isEnglish = isEnglishLanguage()

    val businesses = uiState.businesses
    val player = uiState.playerState.player
    val productionProgress = uiState.productionProgress
    val productionDurations = uiState.productionDurations
    val activeDeliveries = uiState.inventoryState.activeDeliveries
    val activeProductions = uiState.inventoryState.activeProductions
    val marketPrices = uiState.marketState.prices
    val inventory = uiState.inventoryState.items
    val tradeTutorialStep = uiState.tradeTutorialStep
    val megaProjects = uiState.consortiumState.megaProjects

    val categoryFilters = listOf(
        "all" to ("🌐 " + tr("TÜMÜ (31)", "ALL (31)")),
        "turkey" to ("🇹🇷 " + tr("TÜRKİYE (18)", "TÜRKİYE (18)")),
        "global" to ("🌍 " + tr("DÜNYA (13)", "GLOBAL (13)")),
        "tier_1" to ("⛏️ " + tr("KADEME 1", "TIER 1")),
        "tier_2" to ("⚙️ " + tr("KADEME 2", "TIER 2")),
        "tier_3" to ("⚡ " + tr("KADEME 3", "TIER 3")),
        "tier_4" to ("🚀 " + tr("KADEME 4", "TIER 4")),
        "my_facilities" to ("🏭 " + tr("TESİSLERİM", "MY FACILITIES"))
    )

    val filteredCities = remember(dropdownQuery, selectedCategoryFilter, businesses) {
        cities.filter { city ->
            val matchesQuery = dropdownQuery.isBlank() || 
                city.name.contains(dropdownQuery, ignoreCase = true) || 
                city.country.contains(dropdownQuery, ignoreCase = true) || 
                city.primaryProducts.any { p -> p.contains(dropdownQuery, ignoreCase = true) }
            val matchesCategory = when (selectedCategoryFilter) {
                "my_facilities" -> businesses.any { it.cityId == city.id }
                "turkey" -> !city.isGlobal
                "global" -> city.isGlobal
                "tier_1" -> city.maxFacilityTier == 1
                "tier_2" -> city.maxFacilityTier == 2
                "tier_3" -> city.maxFacilityTier == 3
                "tier_4" -> city.maxFacilityTier == 4
                else -> true
            }
            matchesQuery && matchesCategory
        }
    }

    LaunchedEffect(selectedCity) {
        if (selectedCity != null && tradeTutorialStep == 1) {
            viewModel.advanceTradeTutorialStep(2)
        }
    }

    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    mapViewRef.value?.onDetach()
                    mapViewRef.value = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
            mapViewRef.value = null
        }
    }
    val blurAmount = if (showBuildDialog || dropdownExpanded) 12.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize().background(themeOption.backgroundColor)) {
        
        AndroidView(
            modifier = Modifier.fillMaxSize().blur(blurAmount),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    val startCityId = player?.currentCity
                    val startGeo = if (!startCityId.isNullOrBlank()) {
                        cityCoordinates[startCityId] ?: GeoPoint(39.0, 35.0)
                    } else {
                        GeoPoint(39.0, 35.0)
                    }
                    val initialZoom = if (!startCityId.isNullOrBlank()) 9.0 else 6.0
                    controller.setZoom(initialZoom)
                    controller.setCenter(startGeo)
                    mapViewRef.value = this
                    
                    val cm = android.graphics.ColorMatrix(floatArrayOf(
                        -1f,  0f,  0f,  0f, 255f,
                         0f, -1f,  0f,  0f, 255f,
                         0f,  0f, -1f,  0f, 255f,
                         0f,  0f,  0f,  1f,   0f
                    ))
                    if (themeOption.isDark) {
                        overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(cm))
                    }

                    val overlay = TycoonMapOverlay(
                        context = ctx,
                        cities = cities,
                        cityCoordinates = cityCoordinates,
                        businesses = businesses,
                        playerCurrentCity = player?.currentCity,
                        playerId = player?.id,
                        selectedCityId = selectedCity?.id,
                        selectedCategoryFilter = selectedCategoryFilter,
                        themePrimaryColor = themeOption.primaryColor.toArgb(),
                        themeSecondaryColor = themeOption.secondaryColor.toArgb(),
                        themeSurfaceColor = themeOption.surfaceColor.toArgb(),
                        themeTextColor = themeOption.textPrimaryColor.toArgb(),
                        isEnglish = isEnglish,
                        productionProgress = productionProgress,
                        productionDurations = productionDurations,
                        activeDeliveries = activeDeliveries,
                        activeProductions = activeProductions,
                        megaProjects = megaProjects,
                        layersState = mapLayersState,
                        onCityTap = { },
                        onFacilityTap = { },
                        onEmptyCityBuildTap = { }
                    )
                    overlays.add(overlay)
                    mapViewRef.value = this
                }
            },
            update = { mapView ->
                val overlay = mapView.overlays.filterIsInstance<TycoonMapOverlay>().firstOrNull()
                if (overlay != null) {
                    overlay.businesses = businesses
                    overlay.playerCurrentCity = player?.currentCity
                    overlay.playerId = player?.id
                    overlay.selectedCityId = selectedCity?.id
                    overlay.selectedCategoryFilter = selectedCategoryFilter
                    overlay.themePrimaryColor = themeOption.primaryColor.toArgb()
                    overlay.themeSecondaryColor = themeOption.secondaryColor.toArgb()
                    overlay.themeSurfaceColor = themeOption.surfaceColor.toArgb()
                    overlay.themeTextColor = themeOption.textPrimaryColor.toArgb()
                    overlay.isEnglish = isEnglish
                    overlay.productionProgress = productionProgress
                    overlay.productionDurations = productionDurations
                    overlay.activeDeliveries = activeDeliveries
                    overlay.activeProductions = activeProductions
                    overlay.megaProjects = megaProjects
                    overlay.layersState = mapLayersState
                    overlay.onCityTap = { city ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedCity = city
                        selectedFacility = null
                        dropdownQuery = city.getDisplayName(context)
                        val pt = cityCoordinates[city.id]
                        if (pt != null) {
                            mapView.controller.animateTo(pt)
                        }
                    }
                    overlay.onFacilityTap = { business ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFacility = business
                        selectedCity = null
                    }
                    overlay.onEmptyCityBuildTap = { city ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedCity = city
                        selectedFacility = null
                        showBuildDialog = true
                    }
                    mapView.invalidate()
                }
            }
        )

        // Top Search Bar & Category Filter Chips Row + Smart Action Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 14.dp, end = 14.dp)
                .blur(blurAmount),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.example.ui.components.SmartActionCard(
                viewModel = viewModel,
                onNavigateToMarket = onNavigateToMarket,
                onNavigateToBorsa = onNavigateToBorsa,
                onNavigateToProduction = onNavigateToProduction,
                onNavigateToHr = onNavigateToHr,
                onNavigateToBank = onNavigateToBank
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = dropdownQuery,
                    onValueChange = { dropdownQuery = it; dropdownExpanded = true },
                    placeholder = { CurrencyText(stringResource(R.string.map_search_city), color = themeOption.textSecondaryColor, fontSize = 12.5.sp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (dropdownQuery.isNotEmpty()) ThemeGold else themeOption.textSecondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dropdownQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { dropdownQuery = ""; dropdownExpanded = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = themeOption.textSecondaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = themeOption.surfaceColor.copy(alpha = 0.94f),
                        unfocusedContainerColor = themeOption.surfaceColor.copy(alpha = 0.90f),
                        focusedTextColor = themeOption.textPrimaryColor,
                        unfocusedTextColor = themeOption.textPrimaryColor,
                        focusedIndicatorColor = ThemeGold,
                        unfocusedIndicatorColor = themeOption.borderColor.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(themeOption.surfaceColor).heightIn(max = 280.dp)
                ) {
                    filteredCities.forEach { city ->
                        androidx.compose.runtime.key(city.id) {
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CurrencyText(city.countryFlag, fontSize = 14.sp)
                                    CurrencyText(city.getDisplayName(context), color = themeOption.textPrimaryColor, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                selectedCity = city
                                dropdownQuery = city.getDisplayName(context)
                                dropdownExpanded = false
                                mapViewRef.value?.controller?.animateTo(cityCoordinates[city.id])
                            }
                        )
                        }
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categoryFilters, key = { it.first }) { (filterKey, label) ->
                    val isChipSelected = selectedCategoryFilter == filterKey
                    FilterChip(
                        selected = isChipSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategoryFilter = filterKey
                        },
                        shape = RoundedCornerShape(12.dp),
                        label = {
                            CurrencyText(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.5.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeOption.primaryColor.copy(alpha = 0.30f),
                            selectedLabelColor = themeOption.primaryColor,
                            containerColor = themeOption.surfaceColor.copy(alpha = 0.90f),
                            labelColor = themeOption.textSecondaryColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isChipSelected,
                            borderColor = themeOption.borderColor.copy(alpha = 0.6f),
                            selectedBorderColor = themeOption.primaryColor
                        )
                    )
                }
            }
        }

        // Right Floating Map Control Dock
        MapFloatingControlDock(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = if (selectedCity != null || selectedFacility != null) 220.dp else 20.dp),
            themeOption = themeOption,
            playerHqCityId = player?.currentCity,
            mapView = mapViewRef.value,
            isEnglish = isEnglish,
            layersState = mapLayersState,
            onLayersChanged = { mapLayersState = it }
        )

        // Live Logistics & Fleet Radar Bar (Clean Floating Pill - only shown when city sheet is not blocking)
        if (selectedCity == null && selectedFacility == null) {
            LiveLogisticsRadarBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                themeOption = themeOption,
                deliveries = activeDeliveries,
                productions = activeProductions,
                playerHqCityId = player?.currentCity,
                cities = cities,
                isEnglish = isEnglish,
                onFocusLocation = { geoPoint ->
                    mapViewRef.value?.controller?.apply {
                        setZoom(9.5)
                        animateTo(geoPoint)
                    }
                },
                onOpenMarket = onNavigateToMarket
            )
        }

        // Bottom Dossier HUD (Enhanced City Action Sheet & Trade Radar)
        if (selectedCity != null) {
            val city = selectedCity!!
            val isCurrentCity = player?.currentCity == city.id
            val cityBusinesses = businesses.filter { it.cityId == city.id }
            val currentCityId = player?.currentCity ?: "istanbul"

            EnhancedCityDossierSheet(
                modifier = Modifier.align(Alignment.BottomCenter),
                city = city,
                isCurrentHq = isCurrentCity,
                playerHqCityId = currentCityId,
                cityBusinesses = cityBusinesses,
                themeOption = themeOption,
                context = context,
                isEnglish = isEnglish,
                arbitrageInfo = calculateTradeArbitrage(currentCityId, city, uiState.marketState.prices, isEnglish),
                onClose = { selectedCity = null },
                onTravel = {
                    if (!isCurrentCity) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val currentCityObj = cities.find { it.id == currentCityId }
                        val currentCityName = currentCityObj?.getDisplayName(context) ?: "Istanbul"
                        travelOriginAndDest = Pair(currentCityName, city.getDisplayName(context))
                        pendingTravelCityId = city.id
                    }
                },
                onOpenMarket = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToMarket()
                },
                onBuildFacility = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showBuildDialog = true
                }
            )
        }
        

        if (selectedFacility != null) {
            FacilityBottomSheet(
                uiState = uiState,
                modifier = Modifier.align(Alignment.BottomCenter),
                viewModel = viewModel,
                selectedFacility = selectedFacility!!,
                cities = cities,
                themeOption = themeOption,
                context = context,
                haptic = haptic,
                onClose = { selectedFacility = null },
                onNavigateToProduction = onNavigateToProduction
            )
        }


        if (showBuildDialog && selectedCity != null) {
            val isEnglish = isEnglishLanguage()
            val city = selectedCity!!
            var buildSearchQuery by remember { mutableStateOf("") }
            
            val cityFacilities = Product.values().filter { 
                it.canBeBuiltIn(city.id) &&
                (it.getFacilityName(isEnglish).contains(buildSearchQuery, ignoreCase = true) ||
                 it.getDisplayName(isEnglish).contains(buildSearchQuery, ignoreCase = true))
            }.distinctBy { it.facilityId }.sortedBy { it.facilityCost }
            
            AlertDialog(
                onDismissRequest = { showBuildDialog = false },
                containerColor = themeOption.surfaceColor,
                title = { 
                    Column {
                        CurrencyText(
                            text = "${city.countryFlag} ${tr("Tesis Kur:", "Build Facility:")} ${city.getDisplayName(isEnglish)}", 
                            color = themeOption.primaryColor, 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        CurrencyText(
                            text = tr("Bu şehre özgü kurulabilir tesisler ve kademe gereksinimleri.", "Facilities eligible for this city's industrial profile and tier requirements."),
                            color = themeOption.textSecondaryColor,
                            fontSize = 11.sp
                        )
                    }
                },
                text = { 
                    Column(modifier = Modifier.fillMaxHeight(0.75f)) {
                        OutlinedTextField(
                            value = buildSearchQuery,
                            onValueChange = { buildSearchQuery = it },
                            placeholder = { CurrencyText(tr("Tesis veya ürün ara...", "Search facility or product..."), color = themeOption.textSecondaryColor) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeOption.primaryColor,
                                unfocusedBorderColor = themeOption.borderColor,
                                focusedTextColor = themeOption.textPrimaryColor,
                                unfocusedTextColor = themeOption.textPrimaryColor,
                                unfocusedContainerColor = themeOption.surfaceVariantColor,
                                focusedContainerColor = themeOption.surfaceVariantColor
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = tr("Ara", "Search"), tint = themeOption.textSecondaryColor)
                            },
                            trailingIcon = {
                                if (buildSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { buildSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = tr("Temizle", "Clear"), tint = themeOption.textSecondaryColor)
                                    }
                                }
                            }
                        )

                        if (cityFacilities.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.DomainDisabled,
                                        contentDescription = null,
                                        tint = themeOption.textSecondaryColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CurrencyText(
                                        text = if (buildSearchQuery.isNotBlank()) {
                                            tr("Aramanıza uygun tesis bulunamadı.", "No matching facility found.")
                                        } else {
                                            tr(
                                                "Bu şehirde kurulabilecek tesis tanımlanmamış.",
                                                "No facilities defined for this city."
                                            )
                                        },
                                        color = themeOption.textSecondaryColor,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cityFacilities, key = { it.id }) { product ->
                                    val prereq = viewModel.getFacilityPrerequisites(product)
                                    val isUnlocked = prereq.isMet
                                    val rawCost = (product.facilityCost * city.economicMultiplier).toLong()
                                    val finalCost = rawCost
                                    val canAfford = (player?.money ?: 0L) >= finalCost
                                    val isClickable = isUnlocked && canAfford

                                    Surface(
                                        color = if (isUnlocked) themeOption.surfaceVariantColor else themeOption.surfaceVariantColor.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp, 
                                            when {
                                                !isUnlocked -> Color.Gray.copy(alpha = 0.4f)
                                                canAfford -> themeOption.primaryColor
                                                else -> themeOption.borderColor
                                            }
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = isClickable) {
                                            viewModel.handleIntent(com.example.viewmodel.GameIntent.BuildBusiness(product.facilityId, city.id, finalCost))
                                            showBuildDialog = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            if (isUnlocked) Color(product.colorTint).copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        product.icon, 
                                                        contentDescription = null, 
                                                        tint = if (isUnlocked) Color(product.colorTint) else Color.Gray, 
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        CurrencyText(
                                                            product.getFacilityName(isEnglish), 
                                                            style = MaterialTheme.typography.labelMedium, 
                                                            color = if (isUnlocked) themeOption.textPrimaryColor else themeOption.textSecondaryColor, 
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Surface(
                                                            color = when (product.tier) {
                                                                ProductTier.TIER_1 -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                                ProductTier.TIER_2 -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                                ProductTier.TIER_3 -> Color(0xFFA855F7).copy(alpha = 0.2f)
                                                                ProductTier.TIER_4 -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                            },
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            CurrencyText(
                                                                text = "K${product.tier.ordinal + 1}",
                                                                color = when (product.tier) {
                                                                    ProductTier.TIER_1 -> Color(0xFF10B981)
                                                                    ProductTier.TIER_2 -> Color(0xFF38BDF8)
                                                                    ProductTier.TIER_3 -> Color(0xFFA855F7)
                                                                    ProductTier.TIER_4 -> Color(0xFFF59E0B)
                                                                },
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    CurrencyText(
                                                        "${tr("Üretim:", "Production:")} ${product.getDisplayName(isEnglish)}", 
                                                        style = MaterialTheme.typography.labelSmall, 
                                                        color = themeOption.textSecondaryColor,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    CurrencyText(
                                                        com.example.ui.components.formatCredit(finalCost), 
                                                        style = MaterialTheme.typography.labelMedium, 
                                                        color = if(!isUnlocked) Color.Gray else if(canAfford) ThemePositive else themeOption.secondaryColor, 
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (!isUnlocked) {
                                                        CurrencyText(
                                                            tr("🔒 Kilitli", "🔒 Locked"),
                                                            color = Color(0xFFEF4444),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            if (!isUnlocked) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(
                                                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    CurrencyText(
                                                        text = if (isEnglish) prereq.reasonEn else prereq.reasonTr,
                                                        color = Color(0xFFFCA5A5),
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBuildDialog = false }) {
                        CurrencyText(tr("Kapat", "Close"), color = themeOption.textPrimaryColor)
                    }
                }
            )
        }

        if (travelOriginAndDest != null) {
            CaravanTravelAnimationDialog(
                originCityName = travelOriginAndDest!!.first,
                destinationCityName = travelOriginAndDest!!.second,
                onArrivalCompleted = {
                    pendingTravelCityId?.let { targetId ->
                        viewModel.handleIntent(com.example.viewmodel.GameIntent.RelocateWarehouse(targetId))
                    }
                    travelOriginAndDest = null
                    pendingTravelCityId = null
                }
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val themeOption = LocalAppThemeOption.current
    Surface(
        color = if (isHighlighted) accentColor.copy(alpha = 0.1f) else themeOption.surfaceVariantColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isHighlighted) accentColor else themeOption.borderColor),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            CurrencyText(title, style = MaterialTheme.typography.labelSmall, color = themeOption.textPrimaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CurrencyText(subtitle, style = MaterialTheme.typography.labelSmall, color = themeOption.textSecondaryColor, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CityBusinessRow(
    uiState: com.example.viewmodel.GameUiState,
    viewModel: GameViewModel,
    business: com.example.data.BusinessEntity,
    themeOption: com.example.ui.theme.AppThemeOption
) {
    val productionProgress = uiState.productionProgress
    val product = Product.values().find { it.facilityId == business.type }
    val progress = if (product != null) productionProgress[product.id] ?: 0f else 0f
    val isProducing = progress > 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeOption.surfaceVariantColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = product?.icon ?: Icons.Default.Business,
            contentDescription = null,
            tint = if (product != null) Color(product.colorTint) else themeOption.primaryColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CurrencyText(text = product?.facilityName?.uppercase() ?: business.type, style = MaterialTheme.typography.labelMedium, color = themeOption.textPrimaryColor, fontWeight = FontWeight.Bold)
                CurrencyText(text = "LVL ${business.level}", style = MaterialTheme.typography.labelSmall, color = themeOption.secondaryColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (isProducing) {
                LinearProgressIndicator(
                    progress = { (progress as? Float ?: 0f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = themeOption.primaryColor,
                    trackColor = themeOption.borderColor
                )
            } else {
                CurrencyText(tr("Üretim Hazır / Beklemede", "Production Ready / Idle"), style = MaterialTheme.typography.labelSmall, color = themeOption.textSecondaryColor, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun FacilityBottomSheet(
    uiState: com.example.viewmodel.GameUiState,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel,
    selectedFacility: com.example.data.BusinessEntity,
    cities: List<CityProfile>,
    themeOption: com.example.ui.theme.AppThemeOption,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onClose: () -> Unit,
    onNavigateToProduction: () -> Unit
) {
    val isEnglish = isEnglishLanguage()
    val productionProgress = uiState.productionProgress
    
    val city = cities.find { it.id == selectedFacility.cityId }
    val product = Product.values().find { it.facilityId == selectedFacility.type }
    val progress = if (product != null) productionProgress[product.id] ?: 0f else 0f
    val isProducing = progress > 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 95.dp)
    ) {
        com.example.ui.components.GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.94f),
            borderWidth = 1.5.dp,
            borderColor = themeOption.primaryColor
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = product?.icon ?: Icons.Default.Business,
                            contentDescription = null,
                            tint = if (product != null) Color(product.colorTint) else themeOption.primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            CurrencyText(
                                text = product?.getFacilityName(isEnglish)?.uppercase() ?: selectedFacility.type.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = themeOption.primaryColor
                            )
                            CurrencyText(
                                text = "${city?.getDisplayName(isEnglish) ?: ""} • ${tr("Seviye", "Level")} ${selectedFacility.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = themeOption.textSecondaryColor,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = tr("Kapat", "Close"), tint = themeOption.textPrimaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                if (product != null) {
                    com.example.ui.components.AnimatedFacilityVisual(
                        product = product,
                        level = selectedFacility.level,
                        isProducing = isProducing,
                        height = 100.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                CurrencyText(tr("ÜRETİM DURUMU", "PRODUCTION STATUS"), style = MaterialTheme.typography.labelSmall, color = themeOption.secondaryColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isProducing) {
                    val percent = ((progress as? Float ?: 0f) * 100f).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { (progress as? Float ?: 0f) },
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = themeOption.primaryColor,
                            trackColor = themeOption.borderColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        CurrencyText("%$percent", style = MaterialTheme.typography.labelMedium, color = themeOption.primaryColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrencyText("${tr("Üretilen:", "Produced:")} ${product?.getDisplayName(context)}", style = MaterialTheme.typography.labelSmall, color = themeOption.textPrimaryColor)
                } else {
                    Surface(
                        color = themeOption.secondaryColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, themeOption.secondaryColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = themeOption.secondaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            CurrencyText(tr("Üretim Beklemede", "Production Idle"), style = MaterialTheme.typography.labelMedium, color = themeOption.secondaryColor)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onNavigateToProduction() 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeOption.primaryColor, contentColor = themeOption.surfaceColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    CurrencyText(tr("ÜRETİME GİT", "GO TO PRODUCTION"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
