package com.example.ui.screens

import com.example.ui.components.CurrencyText
import com.example.ui.components.UniversalProductIcon
import com.example.ui.components.formatCredit
import com.example.ui.components.AnadoluLiraIcon
import com.example.ui.components.Interactive3DCard
import com.example.ui.components.DynamicParticleField
import com.example.ui.components.ExitSaveLoadingDialog
import androidx.activity.compose.BackHandler

import com.example.ui.theme.trAuto

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.unit.Dp
import com.example.data.MarketPriceEntity
import com.example.ui.theme.ThemePositive
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.*
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationHistoryDialog
import com.example.ui.components.NotificationType
import com.example.ui.components.CityNewsBulletinDialog
import com.example.ui.components.LiveBreakingNewsBanner
import com.example.ui.components.AntiqueMuseumDialog
import com.example.ui.components.TycoonHubCardsSection
import com.example.ui.components.FeatureLockedDialog
import com.example.ui.components.SmartAdvisorHeroCard
import com.example.ui.components.TraderProfileDialog
import com.example.data.GameFeature
import com.example.data.FeatureLockManager
import com.example.data.FeatureLockInfo
import com.example.data.AdvisorRecommendation
import com.example.data.MuseumHeritageManager
import com.example.ui.theme.tr
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.AppThemeOption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.rounded.ChevronRight

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.ThemeBorder

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.viewmodel.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class DashboardMenuButton(
    val label: String,
    val iconRes: Int,
    val feature: GameFeature?,
    val action: () -> Unit,
    val badgeText: String? = null,
    val badgeColor: Color = ThemeGold
)

@Composable
fun DashboardScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    viewModel: GameViewModel, // TODO: Remove in Phase 3 when AntiqueMuseumDialog is migrated
    onNavigateToMarket: () -> Unit,
    onNavigateToBorsa: () -> Unit,
    onNavigateToProduction: (String?) -> Unit,
    onNavigateToFacilityWithCity: ((String?, String?) -> Unit)? = null,
    onNavigateToHr: () -> Unit,
    onNavigateToRd: () -> Unit = {},
    onNavigateToBank: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToMegaProject: () -> Unit = {},
    onNavigateToWeeklyGrowth: () -> Unit = {}
) {
    val themeOption = LocalAppThemeOption.current
    val player = uiState.playerState.player
    val businesses = remember(uiState.businesses) { uiState.businesses.toPersistentList() }
    val inventory = remember(uiState.inventoryState.items) { uiState.inventoryState.items.toPersistentList() }
    val marketPrices = remember(uiState.marketState.prices) { uiState.marketState.prices.toPersistentList() }
    val marketPriceMap = remember(marketPrices) { marketPrices.associateBy { it.itemId } }
    val priceHistory = uiState.marketState.priceHistory
    val megaProjects = uiState.consortiumState.megaProjects
    val activeCityEvents = uiState.marketState.activeCityEvents
    val newsTickerMessage = uiState.newsTickerMessage
    val crisisState = uiState.consortiumState.crisisState
    val macroState = uiState.macroState
    
    var showNewsBulletinDialog by remember { mutableStateOf(false) }
    var showAntiqueMuseumDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showExitSaveDialog by remember { mutableStateOf(false) }
    var lockedFeatureDialogInfo by remember { mutableStateOf<FeatureLockInfo?>(null) }
    var googleAuthRequiredFeature by remember { mutableStateOf<String?>(null) }

    BackHandler {
        showExitSaveDialog = true
    }
    val isOnlineRegistered by viewModel.isOnlineRegistered.collectAsStateWithLifecycle()
    val onlineEmail by viewModel.onlineEmail.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isExpertMode by viewModel.isExpertMode.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val museumOwnedCount = remember(showAntiqueMuseumDialog) { MuseumHeritageManager.getOwnedArtifactIds(context).size }
    val museumUnclaimed = remember(showAntiqueMuseumDialog) { MuseumHeritageManager.calculateUnclaimedVisitorRevenue(context) }

    val marketTrends = uiState.marketState.marketTrends

    var isFacilitiesExpanded by remember { mutableStateOf(false) }

    // Derived values
    val cash = player?.money ?: 0L
    val deposit = player?.depositBalance ?: 0L
    val dailyIncome = player?.dailyIncome ?: 0L
    val xp = player?.xp ?: 0
    val level = player?.level ?: 1
    val inventoryCapacity = player?.inventoryCapacity ?: 5000
    
    val currentInvCount = uiState.currentInvCount
    val inventoryFillPercentage = uiState.inventoryFillPercentage
    val totalFacilityStored = remember(businesses) { businesses.sumOf { it.getStoredTotalQuantity() } }
    val totalFacilityCapacity = remember(businesses) { businesses.sumOf { it.getEffectiveStorageCapacity() } }

    // Consumed directly from GameViewModel StateFlow / UiState layer without UI calculations
    val topInventory = uiState.topInventory
    val inventoryValuation = uiState.inventoryValuation
    val facilityValuation = uiState.facilityValuation
    val netWorth = uiState.netWorth

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_city_night),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dynamic Theme Ambient Overlay - transparent gradient letting the background city art shine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            (if (themeOption.isDark) Color(0xFF040814) else themeOption.backgroundColor).copy(alpha = if (themeOption.isDark) 0.20f else 0.38f),
                            (if (themeOption.isDark) Color(0xFF070E20) else themeOption.backgroundColor).copy(alpha = if (themeOption.isDark) 0.30f else 0.48f)
                        )
                    )
                )
        ) {
            com.example.ui.components.DynamicParticleField(
                particleCount = 30,
                primaryColor = if (themeOption.isDark) ThemeNeonCyan else ThemeNeonCyan.copy(alpha = 0.6f),
                secondaryColor = ThemeGold
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(360.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // -1. Birleşik Oyuncu Profili & Şirket Finansal Kartı (Combined Profile & Company Card)
        if (player != null) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                HeroPlayerCompanyCard(
                    player = player,
                    uiState = uiState,
                    netWorth = netWorth,
                    cash = cash,
                    deposit = deposit,
                    dailyIncome = dailyIncome,
                    onProfileClick = { showProfileDialog = true },
                    onBalanceSheetClick = { onNavigateToStatistics() }
                )
            }
        }

        // 0. Canlı Anadolu Haber Tickerı (Breaking News)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            LiveBreakingNewsBanner(
                events = activeCityEvents,
                newsTickerMessage = newsTickerMessage,
                macroState = macroState,
                onClick = { showNewsBulletinDialog = true }
            )
        }

        // 0.1 KONSORSİYUM VE MAKROEKONOMİK KRİZ BİLDİRİMİ
        if (crisisState.isCrisisActive) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0B0B).copy(alpha = 0.50f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMegaProject() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val isEng = isEnglishLanguage()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(
                                    text = tr(crisisState.crisisTitle, when (crisisState.crisisTitle) {
                                        "Normal Piyasa Dengesi" -> "Normal Market Balance"
                                        "Toparlanma & Normalleşme Başladı" -> "Recovery & Normalization Began"
                                        "Tedarik Zinciri Dengeli" -> "Supply Chain Balanced"
                                        "🚨 AĞIR SANAYİ DARBOĞAZI & MAKRO KRİZ" -> "🚨 HEAVY INDUSTRY BOTTLENECK & MACRO CRISIS"
                                        "⚠️ GELİŞEN TEDARİK SIKIŞIKLIĞI & ENFLASYON BASKISI" -> "⚠️ DEVELOPING SUPPLY TENSION & INFLATION PRESSURE"
                                        "⚠️ BAŞLANGIÇ DÜZEYİ TEDARİK DARALMASI" -> "⚠️ INITIAL LEVEL SUPPLY CONTRACTION"
                                        else -> crisisState.crisisTitle
                                    }),
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF7F1D1D)
                            ) {
                                CurrencyText(
                                    text = tr("KRİZ: %${crisisState.bottleneckPercentage.toInt()}", "CRISIS: %${crisisState.bottleneckPercentage.toInt()}"),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val localizedCrisisDescription = if (isEng) {
                            val desc = crisisState.crisisDescription
                            when {
                                desc.contains("Krize sebep olan konsorsiyumlar tamamlandı") -> "Consortiums causing the crisis were completed or dissolved. Recovery has started in markets."
                                desc.contains("Henüz aktif bir konsorsiyum projesi bulunmuyor") -> "There is no active consortium project yet."
                                desc.contains("Konsorsiyum tedarik hatlarının toparlanmasıyla") -> "Market inflation stabilized with the recovery of consortium supply lines."
                                desc.contains("Piyasadaki konsorsiyumların tedarik hatları") -> "Consortium supply lines in the market operate at normal capacity."
                                desc.contains("hammadde arıyor") -> {
                                    val ratioMatch = Regex("Konsorsiyumların %(\\d+)").find(desc)?.groupValues?.get(1) ?: "0"
                                    val severityMatch = Regex("Kriz Şiddeti: %(\\d+)").find(desc)?.groupValues?.get(1) ?: "0"
                                    val wearMatch = Regex("Tesis yıpranması x([\\d.]+)").find(desc)?.groupValues?.get(1) ?: "1.0"
                                    val logisticsMatch = Regex("lojistik zammı \\+%(\\d+)").find(desc)?.groupValues?.get(1) ?: "0"
                                    val inflationMatch = Regex("Hammadde fiyatları \\+%(\\d+)").find(desc)?.groupValues?.get(1) ?: "0"
                                    "Consortiums %$ratioMatch are looking for raw materials (Crisis Severity: %$severityMatch). Facility wear x$wearMatch, logistics surge +%$logisticsMatch! Raw material prices +%$inflationMatch."
                                }
                                else -> desc
                            }
                        } else {
                            crisisState.crisisDescription
                        }
                        CurrencyText(
                            text = localizedCrisisDescription,
                            color = Color(0xFFFEE2E2),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                CurrencyText(
                                    text = tr("⚙️ ALT SEVİYE & ÜRETİCİ ETKİLERİ", "⚙️ SUB-LEVEL & PRODUCER EFFECTS"),
                                    color = ThemeGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                CurrencyText(
                                    text = tr(
                                        "• Tesis Yıpranması: x${String.format("%.1f", crisisState.facilityWearRateMultiplier)} hızlı aşınma\n• Üretim Yavaşlaması: +%${((crisisState.tier1And2ProductionSlowdownMultiplier - 1f) * 100).toInt()} ek süre\n• Lojistik & Taşıma Zammı: +%${((crisisState.logisticsCostMultiplier - 1f) * 100).toInt()} nakliye maliyeti",
                                        "• Facility Wear: x${String.format("%.1f", crisisState.facilityWearRateMultiplier)} faster degradation\n• Production Slowdown: +%${((crisisState.tier1And2ProductionSlowdownMultiplier - 1f) * 100).toInt()} extra time\n• Logistics & Shipping Surcharge: +%${((crisisState.logisticsCostMultiplier - 1f) * 100).toInt()} transport cost"
                                    ),
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("Hammadde Satış Fiyatı: +%${((crisisState.rawMaterialInflationMultiplier - 1f) * 100).toInt()}", "Raw Material Sale Price: +%${((crisisState.rawMaterialInflationMultiplier - 1f) * 100).toInt()}"),
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyText(
                                    text = tr("Konsorsiyum Hub", "Consortium Hub"),
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 0.2 AKILLI DANIŞMAN (SMART ADVISOR - HERO ACTION HUD)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            val recommendation = remember(
                player?.level,
                player?.money,
                player?.depositBalance,
                player?.currentCity,
                businesses,
                inventory,
                uiState.inventoryState.activeProductions,
                uiState.hrState.researchLevels,
                uiState.hrState.activeResearches
            ) {
                viewModel.getAdvisorRecommendation()
            }

            if (recommendation != null) {
                SmartAdvisorHeroCard(
                    recommendation = recommendation,
                    onActionClick = { rec ->
                        when (rec.targetRoute) {
                            "production" -> onNavigateToProduction(rec.targetCityId)
                            "market" -> onNavigateToMarket()
                            "borsa" -> onNavigateToBorsa()
                            "inventory" -> onNavigateToInventory()
                            "bank" -> onNavigateToBank()
                            "rd" -> onNavigateToRd()
                            "hr" -> onNavigateToHr()
                            "megaproject" -> onNavigateToMegaProject()
                            "map" -> onNavigateToMap()
                            else -> onNavigateToProduction(null)
                        }
                    }
                )
            }
        }



        // 2. Canlı Piyasa Radarı (Şirket Özetinin Hemen Altında - Sadece Seviye 4 ve üstü için)
        if ((player?.level ?: 1) > 3) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1022).copy(alpha = 0.38f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToBorsa() }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.bg_borsa_header),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.22f)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF0F172A).copy(alpha = 0.50f),
                                            Color(0xFF0F172A).copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            LiveMarketRadarHeader()
                            Spacer(modifier = Modifier.height(10.dp))

                            val isEng = isEnglishLanguage()
                            val demirStr = tr("DEMİR", "IRON", isEng)
                            val bakirStr = tr("BAKIR", "COPPER", isEng)
                            val silisyumStr = tr("SİLİSYUM", "SILICON", isEng)
                            val celikStr = tr("ÇELİK", "STEEL", isEng)
                            val cipStr = tr("MİKROÇİP", "CHIP", isEng)

                            data class TickerItemData(
                                val name: String,
                                val price: String,
                                val trend: String,
                                val isUp: Boolean,
                                val sparklinePoints: List<Float>
                            )

                            val tickerItems: List<TickerItemData> = remember(marketPrices, marketTrends, isEng, demirStr, bakirStr, silisyumStr, celikStr, cipStr) {
                                if (marketPrices.isEmpty()) {
                                    listOf(
                                        TickerItemData(demirStr, formatMoney(200L), "▲ +2.5%", true, listOf(185f, 192f, 190f, 196f, 200f)),
                                        TickerItemData(bakirStr, formatMoney(250L), "▲ +1.1%", true, listOf(240f, 242f, 248f, 245f, 250f)),
                                        TickerItemData(silisyumStr, formatMoney(300L), "▼ -1.5%", false, listOf(320f, 315f, 310f, 305f, 300f)),
                                        TickerItemData(celikStr, formatMoney(600L), "▲ +3.2%", true, listOf(570f, 575f, 580f, 590f, 600f)),
                                        TickerItemData(cipStr, formatMoney(2400L), "▲ +4.8%", true, listOf(2200f, 2250f, 2300f, 2350f, 2400f))
                                    )
                                } else {
                                    val sortedPrices = marketPrices.sortedBy { it.price }
                                    sortedPrices.map { price ->
                                        val product = Product.values().find { it.id == price.itemId }
                                        val name = if (product != null) {
                                            product.getDisplayName(isEng).uppercase()
                                        } else {
                                            price.itemId.replace("_", " ").trAuto(isEng).uppercase()
                                        }
                                        val trend = marketTrends[price.itemId]
                                        data class TrendInfo(val arrow: String, val sign: String, val pct: String, val pos: Boolean)
                                        val trendInfo = if (trend != null) {
                                            val pos = trend >= 0f
                                            val a = if (pos) "▲" else "▼"
                                            val s = if (pos) "+" else ""
                                            val pct = String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(trend * 100f))
                                            TrendInfo(a, s, pct, pos)
                                        } else {
                                            val pos = (price.itemId.hashCode() + price.price.toInt()) % 2 == 0
                                            val a = if (pos) "▲" else "▼"
                                            val s = if (pos) "+" else "-"
                                            val pct = String.format(java.util.Locale.US, "%.1f", ((price.price % 15) + 1) * 0.3f)
                                            TrendInfo(a, s, pct, pos)
                                        }
                                        val (arrow, sign, percentStr, isPos) = trendInfo
                                        val p = price.price.toFloat()
                                        val sparkPoints = if (isPos) {
                                            listOf(p * 0.94f, p * 0.96f, p * 0.95f, p * 0.98f, p)
                                        } else {
                                            listOf(p * 1.06f, p * 1.04f, p * 1.05f, p * 1.02f, p)
                                        }
                                        TickerItemData(
                                            name = name,
                                            price = formatMoney(price.price),
                                            trend = "$arrow $sign$percentStr%",
                                            isUp = isPos,
                                            sparklinePoints = sparkPoints
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF060B18).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                    .border(1.dp, ThemeBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .basicMarquee(iterations = Int.MAX_VALUE),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tickerItems.forEach { item ->
                                    val trendColor = if (item.isUp) ThemePositive else ThemeNegative
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText(
                                            text = item.price,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = ThemeNeonCyan
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Sparkline(
                                            data = item.sparklinePoints,
                                            color = trendColor,
                                            strokeWidth = 1.2.dp,
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText(
                                            text = item.trend,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = trendColor
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        CurrencyText(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThemeBorder
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // End of if level > 3

        // 2.5 TÜM MENÜLER & HIZLI ERİŞİM (4x3 DENGELİ BUTON YAPISI - CANLI DURUM ROZETLERİ & KİLİT ENTEGRASYONU)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            val isEng = isEnglishLanguage()
            val researchLevels = uiState.hrState.researchLevels

            // Dynamic badge calculations
            val activeProductionCount = remember(uiState.inventoryState.activeProductions, uiState.productionProgress) {
                val ids = mutableSetOf<String>()
                uiState.inventoryState.activeProductions.forEach { ids.add(it.productId) }
                uiState.productionProgress.forEach { if (it.value > 0f && it.value < 1f) ids.add(it.key) }
                ids.size
            }
            val productionBadge = if (activeProductionCount > 0) "$activeProductionCount" else null

            val warehouseBadge = if (inventoryFillPercentage >= 85) "%${inventoryFillPercentage}" else null
            val warehouseBadgeColor = if (inventoryFillPercentage >= 95) ThemeNegative else ThemeGold

            val activeRdCount = remember(uiState.hrState.activeResearches) {
                if (uiState.hrState.activeResearches.isNotEmpty()) "${uiState.hrState.activeResearches.size}" else null
            }

            val museumBadge = if (museumUnclaimed > 0L) "💰" else null

            val menuItems = remember(isEng, productionBadge, warehouseBadge, activeRdCount, museumBadge) {
                listOf(
                    // Satır 1 (4 Menü)
                    DashboardMenuButton(
                        tr("Üretim", "Production", isEng),
                        R.drawable.bg_menu_production,
                        GameFeature.FACILITIES,
                        { onNavigateToProduction(null) },
                        badgeText = productionBadge,
                        badgeColor = ThemeNeonCyan
                    ),
                    DashboardMenuButton(
                        tr("Depo", "Warehouse", isEng),
                        R.drawable.bg_menu_warehouse,
                        GameFeature.WAREHOUSE,
                        onNavigateToInventory,
                        badgeText = warehouseBadge,
                        badgeColor = warehouseBadgeColor
                    ),
                    DashboardMenuButton(
                        tr("Harita", "Map", isEng),
                        R.drawable.bg_menu_map,
                        null,
                        onNavigateToMap
                    ),
                    DashboardMenuButton(
                        tr("Pazar", "Market", isEng),
                        R.drawable.ic_tab_marketplace,
                        GameFeature.MARKET_P2P,
                        onNavigateToMarket
                    ),

                    // Satır 2 (4 Menü)
                    DashboardMenuButton(
                        tr("Banka", "Bank", isEng),
                        R.drawable.icon_bank_3d,
                        GameFeature.BANKING,
                        onNavigateToBank
                    ),
                    DashboardMenuButton(
                        tr("Konsorsiyum", "Consortium", isEng),
                        R.drawable.bg_consortium_header,
                        GameFeature.CONSORTIUM,
                        onNavigateToMegaProject
                    ),
                    DashboardMenuButton(
                        tr("İ.K.", "HR", isEng),
                        R.drawable.bg_menu_hr,
                        GameFeature.HR_MANAGERS,
                        onNavigateToHr
                    ),
                    DashboardMenuButton(
                        tr("Ar-Ge", "R&D", isEng),
                        R.drawable.bg_ar_ge,
                        GameFeature.RD_LAB,
                        onNavigateToRd,
                        badgeText = activeRdCount,
                        badgeColor = ThemeNeonCyan
                    ),

                    // Satır 3 (2 Menü - 4'lü Düzende Dengeli & Ortalanmış)
                    DashboardMenuButton(
                        tr("Müze", "Museum", isEng),
                        R.drawable.bg_museum_header,
                        GameFeature.MUSEUM,
                        {
                            if (!viewModel.isUserGoogleSignedIn()) {
                                googleAuthRequiredFeature = tr("Müze", "Museum", isEng)
                            } else {
                                showAntiqueMuseumDialog = true
                            }
                        },
                        badgeText = museumBadge,
                        badgeColor = ThemeGold
                    ),
                    DashboardMenuButton(
                        tr("Sıralama", "Leaderboard", isEng),
                        R.drawable.ic_tab_leaderboard,
                        null,
                        {
                            if (!viewModel.isUserGoogleSignedIn()) {
                                googleAuthRequiredFeature = tr("Sıralama", "Leaderboard", isEng)
                            } else {
                                onNavigateToSocial()
                            }
                        }
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (themeOption.isDark) Color(0xFF0A1022).copy(alpha = 0.65f) else themeOption.surfaceColor.copy(alpha = 0.85f),
                border = BorderStroke(1.2.dp, ThemeGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 4x3 Grid (Her Satırda 4 Kolon, Büyük ve Dikkat Çekici İkonlar)
                    val rows = remember(menuItems) { menuItems.chunked(4) }
                    val isGoogleUser = viewModel.isUserGoogleSignedIn()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (rowItems.size == 4) {
                                    rowItems.forEach { item ->
                                        val lockInfo = item.feature?.let { FeatureLockManager.getLockInfo(it, level, researchLevels) }
                                        val isLevelLocked = (lockInfo != null && !lockInfo.isUnlocked)
                                        val isGuestLocked = !isGoogleUser && (
                                            item.feature == GameFeature.MARKET_P2P ||
                                            item.feature == GameFeature.CONSORTIUM ||
                                            item.feature == GameFeature.MUSEUM ||
                                            item.label.contains("Pazar", ignoreCase = true) ||
                                            item.label.contains("Market", ignoreCase = true) ||
                                            item.label.contains("Konsorsiyum", ignoreCase = true) ||
                                            item.label.contains("Consortium", ignoreCase = true) ||
                                            item.label.contains("Müze", ignoreCase = true) ||
                                            item.label.contains("Museum", ignoreCase = true) ||
                                            item.label.contains("Sıralama", ignoreCase = true) ||
                                            item.label.contains("Leaderboard", ignoreCase = true)
                                        )

                                        DashboardMenuItemCard(
                                            item = item,
                                            isLocked = isLevelLocked,
                                            lockInfo = lockInfo,
                                            isGuestLocked = isGuestLocked,
                                            onGuestLockClick = { googleAuthRequiredFeature = item.label },
                                            themeOption = themeOption,
                                            modifier = Modifier.weight(1f),
                                            onLockClick = { lockedFeatureDialogInfo = it }
                                        )
                                    }
                                } else {
                                    // Son satır için 4 sütunlu düzende eşit boşluklarla ortalama
                                    val leadingSpaces = (4 - rowItems.size) / 2f
                                    val trailingSpaces = (4 - rowItems.size) - leadingSpaces
                                    if (leadingSpaces > 0f) {
                                        Spacer(modifier = Modifier.weight(leadingSpaces))
                                    }
                                    rowItems.forEach { item ->
                                        val lockInfo = item.feature?.let { FeatureLockManager.getLockInfo(it, level, researchLevels) }
                                        val isLevelLocked = (lockInfo != null && !lockInfo.isUnlocked)
                                        val isGuestLocked = !isGoogleUser && (
                                            item.feature == GameFeature.MARKET_P2P ||
                                            item.feature == GameFeature.CONSORTIUM ||
                                            item.feature == GameFeature.MUSEUM ||
                                            item.label.contains("Pazar", ignoreCase = true) ||
                                            item.label.contains("Market", ignoreCase = true) ||
                                            item.label.contains("Konsorsiyum", ignoreCase = true) ||
                                            item.label.contains("Consortium", ignoreCase = true) ||
                                            item.label.contains("Müze", ignoreCase = true) ||
                                            item.label.contains("Museum", ignoreCase = true) ||
                                            item.label.contains("Sıralama", ignoreCase = true) ||
                                            item.label.contains("Leaderboard", ignoreCase = true)
                                        )

                                        DashboardMenuItemCard(
                                            item = item,
                                            isLocked = isLevelLocked,
                                            lockInfo = lockInfo,
                                            isGuestLocked = isGuestLocked,
                                            onGuestLockClick = { googleAuthRequiredFeature = item.label },
                                            themeOption = themeOption,
                                            modifier = Modifier.weight(1f),
                                            onLockClick = { lockedFeatureDialogInfo = it }
                                        )
                                    }
                                    if (trailingSpaces > 0f) {
                                        Spacer(modifier = Modifier.weight(trailingSpaces))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    }

    if (showNewsBulletinDialog) {
        CityNewsBulletinDialog(
            events = activeCityEvents,
            newsTickerMessage = newsTickerMessage,
            macroState = macroState,
            bulletinOpportunities = uiState.bulletinOpportunities,
            ownedFacilities = uiState.businesses,
            onClaimReward = { oppId ->
                onIntent(com.example.viewmodel.GameIntent.ClaimBulletinOpportunityReward(oppId))
            },
            onQuickProduce = { oppId ->
                onIntent(com.example.viewmodel.GameIntent.QuickProduceForBulletinOpportunity(oppId))
            },
            onNavigateToFacility = { productId, cityId ->
                showNewsBulletinDialog = false
                if (onNavigateToFacilityWithCity != null) {
                    onNavigateToFacilityWithCity(productId, cityId)
                } else {
                    onNavigateToProduction(productId)
                }
            },
            onNavigateToBorsa = {
                showNewsBulletinDialog = false
                onNavigateToBorsa()
            },
            onNavigateToMarket = {
                showNewsBulletinDialog = false
                onNavigateToMarket()
            },
            onDismiss = { showNewsBulletinDialog = false },
            onNavigateToCity = { targetCityId ->
                showNewsBulletinDialog = false
                onNavigateToMap()
            }
        )
    }

    if (showAntiqueMuseumDialog) {
        AntiqueMuseumDialog(
            viewModel = viewModel,
            onDismiss = { showAntiqueMuseumDialog = false }
        )
    }

    if (showProfileDialog && player != null) {
        TraderProfileDialog(
            player = player,
            onNavigateToBank = onNavigateToBank,
            onDismiss = { showProfileDialog = false },
            isOnlineRegistered = isOnlineRegistered,
            onlineEmail = onlineEmail,
            onLogin = { email, pass -> viewModel.loginOnline(email, pass) },
            onRegister = { email, pass -> viewModel.registerOnline(email, pass, player.name) },
            onLogout = { viewModel.logoutOnline() },
            onGoogleSignIn = { idToken, onResult -> viewModel.signInWithGoogle(idToken, onResult) },
            onGoogleSignInAnon = { onResult -> viewModel.signInAnonymously(onResult) },
            onUpdateTraderName = { newName -> viewModel.updateTraderNameWithDiamond(newName) },
            selectedTheme = selectedTheme,
            onSelectTheme = { themeId -> viewModel.setSelectedTheme(themeId) },
            selectedLanguage = selectedLanguage,
            onSelectLanguage = { langCode -> viewModel.setSelectedLanguage(langCode) },
            isExpertMode = isExpertMode,
            onSelectUiMode = { isExpert -> viewModel.setExpertMode(isExpert) },
            onForceSyncCloud = {
                viewModel.forceSyncCloudSaveToSupabase(context)
            },
            onForceRestoreCloud = {
                viewModel.forceRestoreFromCloud(context)
            },
            viewModel = viewModel
        )
    }

    // Feature Lock Info Dialog
    FeatureLockedDialog(
        lockInfo = lockedFeatureDialogInfo,
        onDismiss = { lockedFeatureDialogInfo = null },
        onNavigateToRd = onNavigateToRd,
        onNavigateToProduction = { onNavigateToProduction(null) }
    )

    googleAuthRequiredFeature?.let { feat ->
        com.example.ui.components.GoogleAuthRequiredModal(
            featureName = feat,
            viewModel = viewModel,
            onDismiss = { googleAuthRequiredFeature = null },
            onSuccess = {
                val target = googleAuthRequiredFeature ?: ""
                googleAuthRequiredFeature = null
                when {
                    target.contains("Pazar", ignoreCase = true) || target.contains("Market", ignoreCase = true) -> onNavigateToMarket()
                    target.contains("Konsorsiyum", ignoreCase = true) || target.contains("Consortium", ignoreCase = true) -> onNavigateToMegaProject()
                    target.contains("Müze", ignoreCase = true) || target.contains("Museum", ignoreCase = true) -> showAntiqueMuseumDialog = true
                    target.contains("Sıralama", ignoreCase = true) || target.contains("Leaderboard", ignoreCase = true) -> onNavigateToSocial()
                }
            }
        )
    }

    if (showExitSaveDialog) {
        ExitSaveLoadingDialog(
            viewModel = viewModel,
            onDismiss = { showExitSaveDialog = false }
        )
    }
}

@Composable
fun ActiveFacilitiesSection(
    uiState: GameUiState,
    themeOption: com.example.ui.theme.AppThemeOption,
    onNavigateToProduction: (String?) -> Unit,
    onIntent: (GameIntent) -> Unit
) {
    val productionProgress = uiState.productionProgress
    val activeProductions = uiState.inventoryState.activeProductions
    val activeProductIds = remember(activeProductions, productionProgress) {
        val list = mutableListOf<String>()
        activeProductions.forEach { if (!list.contains(it.productId)) list.add(it.productId) }
        productionProgress.keys.forEach { if (!list.contains(it)) list.add(it) }
        list
    }
    val businesses = remember(uiState.businesses) { uiState.businesses.toPersistentList() }
    val businessMap = remember(businesses) { businesses.associateBy { it.type } }
    var isFacilitiesExpanded by remember { mutableStateOf(false) }

    // Üreticiler (Aktif Tesisler) Button
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isFacilitiesExpanded = !isFacilitiesExpanded },
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CurrencyText(
                text = "Aktif Tesisler (${activeProductIds.size})".trAuto(), 
                color = themeOption.textPrimaryColor, 
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (isFacilitiesExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Genişlet/Daralt",
                tint = themeOption.primaryColor
            )
        }
    }
    
    AnimatedVisibility(visible = isFacilitiesExpanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            if (activeProductIds.isEmpty()) {
                CurrencyText("Şu an üretim yapan tesisiniz yok.".trAuto(), color = themeOption.textSecondaryColor, fontSize = 14.sp)
            } else {
                activeProductIds.forEach { bId ->
                    val activeProd = activeProductions.find { it.productId == bId || it.facilityId == bId }
                    val prog = activeProd?.getProgress() ?: (productionProgress[bId] ?: 0.05f)
                    val prod = remember(bId) { Product.values().find { it.id == bId || it.facilityId == bId } }
                    val b = prod?.facilityId?.let { businessMap[it] } ?: businesses.find { it.id == activeProd?.businessId }
                    val cityName = b?.cityId?.uppercase() ?: "TESİS"
                    val totalDur = activeProd?.totalDurationMs ?: (uiState.productionDurations[bId] ?: 0L)
                    val remainingTimeMs = activeProd?.getRemainingTimeMs() ?: ((1f - prog) * totalDur).toLong()
                    val skipCost = kotlin.math.max(1, (remainingTimeMs / 3_600_000L).toInt())

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = themeOption.surfaceVariantColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stylish Icon
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = themeOption.primaryColor.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { onNavigateToProduction(prod?.id) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if(prod!=null) UniversalProductIcon(prod, 20.dp, prod?.let { Color(it.colorTint) } ?: themeOption.primaryColor) else Icon(Icons.Rounded.PrecisionManufacturing, null, tint=prod?.let { Color(it.colorTint) } ?: themeOption.primaryColor, modifier=Modifier.size(20.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Details & Progress
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToProduction(prod?.id) }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    CurrencyText("${prod?.displayName ?: "Tesis"} - $cityName", color = themeOption.textPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    CurrencyText("%${(prog * 100).toInt().coerceIn(0, 100)}", color = themeOption.primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { prog },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = themeOption.primaryColor,
                                    trackColor = themeOption.surfaceColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Quick Speed Up Button
                            Surface(
                                onClick = { onIntent(GameIntent.SkipProductionWithGems(bId)) },
                                shape = RoundedCornerShape(6.dp),
                                color = ThemeGold,
                                border = BorderStroke(0.5.dp, ThemeGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FlashOn,
                                        contentDescription = "Hızlandır",
                                        tint = Color(0xFF1A1300),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    CurrencyText(
                                        text = "-$skipCost 💎",
                                        color = Color(0xFF1A1300),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveMarketRadarHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ThemeNeonCyan.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.60f)),
                modifier = Modifier.size(28.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_borsa_header),
                    contentDescription = "Borsa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CurrencyText("CANLI PİYASA RADARI".trAuto(), color = ThemeNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily, letterSpacing = 0.5.sp)
        }
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ThemePositive.copy(alpha = alpha)))
                Spacer(modifier = Modifier.width(4.dp))
                CurrencyText("AKTİF".trAuto(), color = ThemePositive, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CurrencyText("Borsa".trAuto(), color = ThemeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Borsaya Git",
                    tint = ThemeGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LiveNotificationCard(
    modifier: Modifier = Modifier
) {
    val notification by SmartNotificationManager.notification.collectAsStateWithLifecycle()
    val history by SmartNotificationManager.history.collectAsStateWithLifecycle()
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Derived states prevent recomposing card when other unrelated history state ticks occur
    val latestData by remember {
        derivedStateOf { notification ?: history.firstOrNull() }
    }
    val hasActiveNotification by remember {
        derivedStateOf { notification != null }
    }

    LiveNotificationCardContent(
        latestData = latestData,
        hasActiveNotification = hasActiveNotification,
        onClick = { showHistoryDialog = true },
        modifier = modifier
    )

    if (showHistoryDialog) {
        NotificationHistoryDialog(onDismiss = { showHistoryDialog = false })
    }
}

@Composable
private fun LiveNotificationCardContent(
    latestData: com.example.ui.components.NotificationData?,
    hasActiveNotification: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderStroke = remember(hasActiveNotification, latestData?.type) {
        BorderStroke(
            1.2.dp,
            if (hasActiveNotification) {
                when (latestData?.type) {
                    NotificationType.SUCCESS -> ThemeGold
                    NotificationType.ALERT -> ThemeNegative
                    else -> ThemeNeonCyan
                }
            } else Color(0xFF1E293B).copy(alpha = 0.7f)
        )
    }

    val typeColor = remember(latestData?.type) {
        when (latestData?.type) {
            NotificationType.SUCCESS -> ThemeGold
            NotificationType.ALERT -> ThemeNegative
            else -> ThemeNeonCyan
        }
    }

    val typeIcon = remember(latestData?.type) {
        when (latestData?.type) {
            NotificationType.SUCCESS -> Icons.Default.CheckCircle
            NotificationType.ALERT -> Icons.Default.Error
            else -> Icons.Default.Info
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A1022).copy(alpha = 0.38f),
        border = borderStroke,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val isEn = com.example.ui.theme.isEnglishLanguage()
                val resolvedProdId = latestData?.getResolvedProductId()
                val resolvedQuality = latestData?.getResolvedQuality()
                val notifMessage = latestData?.getFormattedMessage(isEn) ?: tr("Tüm sistemler normal ve operasyonlar aktif.", "All systems normal and operations active.")

                if (resolvedProdId != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F1829),
                        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.6f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            com.example.ui.components.UniversalProductIcon(
                                productId = resolvedProdId,
                                size = 24.dp
                            )
                            if (resolvedQuality != null) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 3.dp),
                                    color = resolvedQuality.badgeColor.copy(alpha = 0.95f),
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    Text(
                                        text = "★${resolvedQuality.stars}",
                                        color = Color.Black,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = typeColor.copy(alpha = 0.25f),
                        border = remember(typeColor) { BorderStroke(1.dp, typeColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (latestData != null) typeIcon else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (latestData != null) typeColor else ThemeNeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            text = if (hasActiveNotification) "CANLI BİLDİRİM".trAuto() else "BİLDİRİM AKIŞI".trAuto(),
                            color = if (hasActiveNotification) typeColor else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            letterSpacing = 0.5.sp
                        )
                        if (hasActiveNotification) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = typeColor.copy(alpha = 0.25f),
                                border = BorderStroke(0.6.dp, typeColor.copy(alpha = 0.6f))
                            ) {
                                CurrencyText(
                                    text = "YENİ".trAuto(),
                                    color = typeColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    CurrencyText(
                        text = notifMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF060C1A).copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, ThemeNeonCyan.copy(alpha = 0.40f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CurrencyText(
                        text = "Geçmiş".trAuto(),
                        color = ThemeNeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = ThemeGold,
    strokeWidth: Dp = 2.dp
) {
    if (data.size < 2) return

    val (minVal, maxVal, range) = remember(data) {
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val r = if (max - min == 0f) 1f else max - min
        Triple(min, max, r)
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.cornerPathEffect(6f)
            )
        )
    }
}

@Composable
fun MarketTickerTapeRow(
    marketPrices: List<MarketPriceEntity>,
    marketTrends: Map<String, Float>
) {
    val isEng = isEnglishLanguage()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A1022).copy(alpha = 0.38f),
        border = BorderStroke(1.dp, Color(0xFF1C2A4A).copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ThemeNeonCyan.copy(alpha = 0.20f),
                border = BorderStroke(0.8.dp, ThemeNeonCyan.copy(alpha = 0.40f)),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    CurrencyText(
                        text = "CANLI PİYASA".trAuto(isEng),
                        color = ThemeNeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val demirStr = tr("DEMİR", "IRON", isEng)
                val petrolStr = tr("PETROL", "CRUDE OIL", isEng)
                val celikStr = tr("ÇELİK", "STEEL", isEng)
                val cipStr = tr("MİKROÇİP", "CHIP", isEng)

                val displayList = remember(marketPrices, demirStr, petrolStr, celikStr, cipStr) {
                    if (marketPrices.isNotEmpty()) marketPrices.sortedBy { it.price } else listOf(
                        MarketPriceEntity(itemId = demirStr, price = 200L),
                        MarketPriceEntity(itemId = petrolStr, price = 850L),
                        MarketPriceEntity(itemId = celikStr, price = 600L),
                        MarketPriceEntity(itemId = cipStr, price = 2400L)
                    )
                }

                val productMap = remember { Product.values().associateBy { it.id } }

                displayList.forEach { item ->
                    val product = productMap[item.itemId]
                    val itemName = if (product != null) {
                        product.getDisplayName(isEng)
                    } else {
                        item.itemId.replace("_", " ").trAuto(isEng)
                    }
                    val trend = marketTrends[item.itemId] ?: ((item.itemId.hashCode() % 15 - 5) / 100f)
                    val isPos = trend >= 0
                    val trendText = if (isPos) "+%.1f%%".format(trend * 100) else "%.1f%%".format(trend * 100)
                    val trendColor = if (isPos) ThemePositive else ThemeNegative

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CurrencyText(
                            text = itemName.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily
                        )
                        CurrencyText(
                            text = formatCurrency(item.price, isEnglishLanguage()),
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = trendColor.copy(alpha = 0.20f),
                            border = BorderStroke(0.6.dp, trendColor.copy(alpha = 0.40f))
                        ) {
                            CurrencyText(
                                text = trendText,
                                color = trendColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateTraderTitle(level: Int, isEng: Boolean): String {
    return when {
        level >= 50 -> if (isEng) "Emperor of Trade" else "Ticaret İmparatoru"
        level >= 40 -> if (isEng) "Global Tycoon" else "Global Sanayici"
        level >= 30 -> if (isEng) "Holding Boss" else "Holding Patronu"
        level >= 25 -> if (isEng) "Mega Investor" else "Mega Yatırımcı"
        level >= 20 -> if (isEng) "Industrialist" else "Büyük Sanayici"
        level >= 15 -> if (isEng) "Senior Merchant" else "Kıdemli Tüccar"
        level >= 10 -> if (isEng) "Regional Merchant" else "Bölge Tüccarı"
        level >= 5 -> if (isEng) "Growing Merchant" else "Gelişen Tüccar"
        else -> if (isEng) "Apprentice Trader" else "Çırak Tüccar"
    }
}

private fun formatCompactNumber(value: Long): String {
    return when {
        value >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.1fB", value / 1_000_000_000.0)
        value >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}

@Composable
fun HeroPlayerCompanyCard(
    player: com.example.data.PlayerEntity,
    uiState: GameUiState,
    netWorth: Long,
    cash: Long,
    deposit: Long,
    dailyIncome: Long,
    onProfileClick: () -> Unit,
    onBalanceSheetClick: () -> Unit
) {
    val themeOption = LocalAppThemeOption.current
    val isEng = isEnglishLanguage()

    // Title & Level Calculations from XpLevelEngine
    val levelProgress = remember(player.xp) {
        com.example.data.XpLevelEngine.getProgress(player.xp.toLong())
    }
    val effectiveLevel = remember(player.level, levelProgress.level) {
        maxOf(player.level, levelProgress.level)
    }
    val title = remember(effectiveLevel, isEng) { calculateTraderTitle(effectiveLevel, isEng) }
    val xpInLevel = levelProgress.currentLevelXp
    val xpNeeded = levelProgress.targetLevelXp
    val progressRatio = levelProgress.progressFraction

    val animatedProgressRatio by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "xpAnimatedProgressBar"
    )

    // Date Text
    val dateText = if (uiState.realTimeClockText.isNotBlank()) {
        uiState.realTimeClockText
    } else {
        if (isEng) "Day ${player.loginStreak.coerceAtLeast(1)}" else "Gün ${player.loginStreak.coerceAtLeast(1)}"
    }

    // Pulse animation for avatar ring
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Interactive3DCard(
        shape = RoundedCornerShape(16.dp),
        maxTiltAngle = 8f,
        specularShine = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (themeOption.isDark) Color(0xFF070E20).copy(alpha = 0.78f) else themeOption.surfaceColor.copy(alpha = 0.90f)
            ),
            border = BorderStroke(
                1.2.dp,
                Brush.horizontalGradient(
                    listOf(
                        ThemeGold.copy(alpha = 0.85f),
                        ThemeNeonCyan.copy(alpha = 0.50f),
                        ThemeGold.copy(alpha = 0.35f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient subtle glow overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                ThemeGold.copy(alpha = 0.07f),
                                Color.Transparent,
                                ThemeNeonCyan.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            Image(
                painter = painterResource(id = R.drawable.bg_company_summary),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.08f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // 1. TOP ROW: Player Avatar + Profile Info + Date & Bilanço Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile section (clickable -> Profile Dialog)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onProfileClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Orb with Glowing Border
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF131D33),
                            border = BorderStroke(1.5.dp, ThemeGold.copy(alpha = glowAlpha)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = player.name,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyText(
                                    text = player.name.ifEmpty { tr("Usta Tüccar", "Master Trader", isEng) },
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeGold.copy(alpha = 0.20f),
                                    border = BorderStroke(0.6.dp, ThemeGold.copy(alpha = 0.70f))
                                ) {
                                    CurrencyText(
                                        text = "LVL $effectiveLevel",
                                        color = ThemeGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.15f)
                                ) {
                                    CurrencyText(
                                        text = title,
                                        color = ThemeNeonCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Date & Bilanço Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Date Chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            border = BorderStroke(0.7.dp, Color(0xFF334155).copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(10.dp)
                                )
                                CurrencyText(
                                    text = dateText,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Bilanço Raporu Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF131D33).copy(alpha = 0.90f),
                            border = BorderStroke(0.8.dp, ThemeGold.copy(alpha = 0.65f)),
                            modifier = Modifier.clickable(onClick = onBalanceSheetClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = tr("Bilanço", "Report", isEng),
                                    color = ThemeGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. XP PROGRESS ROW (Minimal & Sleek)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgressRatio.coerceIn(0.02f, 1f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ThemeNeonCyan, ThemeGold)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val percent = (progressRatio * 100).toInt().coerceIn(0, 100)
                    CurrencyText(
                        text = "${formatCompactNumber(xpInLevel)} / ${formatCompactNumber(xpNeeded)} XP (%$percent)",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = RobotoMonoFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. FINANCIAL SUMMARY ROW (Net Worth + Sparkline + Cashflow Velocity)
                val cashAndDeposit = (cash + deposit + player.lockedDepositBalance).coerceAtLeast(0L)
                val facilityVal = uiState.facilityValuation.coerceAtLeast(0L)
                val invVal = uiState.inventoryValuation.coerceAtLeast(0L)
                val rdVal = uiState.rdValuation.coerceAtLeast(0L)
                val consortiumVal = uiState.consortiumValuation.coerceAtLeast(0L)
                val guildSharesVal = remember(uiState.guildsState.playerGuildShares, uiState.guildsState.playerGuildBuyPrices) {
                    uiState.guildsState.playerGuildShares.entries.sumOf { (id, count) ->
                        val p = uiState.guildsState.playerGuildBuyPrices[id] ?: 1000.0
                        (count * p).toLong()
                    }
                }
                val otherVal = (netWorth - (cashAndDeposit + facilityVal + invVal + rdVal)).coerceAtLeast(consortiumVal).coerceAtLeast(guildSharesVal).coerceAtLeast(0L)
                val totalAllocation = (cashAndDeposit + facilityVal + invVal + rdVal + otherVal).coerceAtLeast(1L)

                val pctCash = ((cashAndDeposit.toDouble() / totalAllocation) * 100).toInt().coerceIn(0, 100)
                val pctFac = ((facilityVal.toDouble() / totalAllocation) * 100).toInt().coerceIn(0, 100)
                val pctInv = ((invVal.toDouble() / totalAllocation) * 100).toInt().coerceIn(0, 100)
                val pctRd = ((rdVal.toDouble() / totalAllocation) * 100).toInt().coerceIn(0, 100)
                val pctOther = (100 - (pctCash + pctFac + pctInv + pctRd)).coerceAtLeast(0)

                val weightCash = if (cashAndDeposit > 0L) (cashAndDeposit.toFloat() / totalAllocation).coerceAtLeast(0.04f) else 0f
                val weightFac = if (facilityVal > 0L) (facilityVal.toFloat() / totalAllocation).coerceAtLeast(0.04f) else 0f
                val weightInv = if (invVal > 0L) (invVal.toFloat() / totalAllocation).coerceAtLeast(0.04f) else 0f
                val weightRd = if (rdVal > 0L) (rdVal.toFloat() / totalAllocation).coerceAtLeast(0.04f) else 0f
                val weightOther = if (otherVal > 0L) (otherVal.toFloat() / totalAllocation).coerceAtLeast(0.04f) else 0f

                // Sparkline history & Momentum
                val growthHistory = uiState.growthHistory
                val sparklineHistory: List<Long> = remember(growthHistory, netWorth) {
                    if (growthHistory.size >= 2) {
                        growthHistory.takeLast(8).map { it.netWorth.toLong() }
                    } else if (growthHistory.size == 1) {
                        val p0 = (netWorth * 0.92).toLong().coerceAtLeast(10000L)
                        listOf(p0, (netWorth * 0.95).toLong(), (netWorth * 0.98).toLong(), netWorth)
                    } else {
                        val p0 = (netWorth * 0.88).toLong().coerceAtLeast(10000L)
                        listOf(p0, (p0 * 1.03).toLong(), (p0 * 1.06).toLong(), (p0 * 1.10).toLong(), netWorth)
                    }
                }

                val prevNetWorth: Long = remember(growthHistory, netWorth) {
                    if (growthHistory.isNotEmpty()) {
                        growthHistory.first().netWorth.toLong()
                    } else {
                        (netWorth * 0.922).toLong()
                    }
                }
                val momentumDiff = netWorth - prevNetWorth
                val momentumPct = if (prevNetWorth > 0L) {
                    (momentumDiff.toDouble() / prevNetWorth.toDouble()) * 100.0
                } else 8.4
                val isPositiveTrend = momentumPct >= 0

                val cashflowVelocity = remember(dailyIncome, uiState.businesses.size, player.depositBalance) {
                    if (dailyIncome != 0L) {
                        val perMin = (dailyIncome.toDouble() / 15.0).toLong()
                        if (perMin >= 0) perMin.coerceAtLeast(12450L) else perMin
                    } else {
                        val facilityEst = uiState.businesses.sumOf { b -> ((b.level * 1850L) * (1f - b.wearLevel)).toLong() }
                        val depositInterestEst = (player.depositBalance * 0.0005).toLong()
                        (facilityEst + depositInterestEst).coerceAtLeast(12450L)
                    }
                }

                var showAssetAllocationDialog by remember { mutableStateOf(false) }

                if (showAssetAllocationDialog) {
                    AssetAllocationDetailDialog(
                        player = player,
                        uiState = uiState,
                        cashAndDeposit = cashAndDeposit,
                        facilityVal = facilityVal,
                        invVal = invVal,
                        rdVal = rdVal,
                        otherVal = otherVal,
                        totalNetWorth = netWorth,
                        pctCash = pctCash,
                        pctFac = pctFac,
                        pctInv = pctInv,
                        pctRd = pctRd,
                        pctOther = pctOther,
                        isEng = isEng,
                        onDismiss = { showAssetAllocationDialog = false },
                        onBalanceSheetClick = {
                            showAssetAllocationDialog = false
                            onBalanceSheetClick()
                        }
                    )
                }

                // Row: Net Worth + Sparkline + Cashflow Velocity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Net Worth, Sparkline & Momentum
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .clickable(onClick = onBalanceSheetClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText(
                                text = tr("NET ŞİRKET DEĞERİ", "TOTAL NET WORTH", isEng),
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            // Compact Momentum Badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isPositiveTrend) Color(0xFF052E16) else Color(0xFF3F0B11),
                                border = BorderStroke(0.6.dp, if (isPositiveTrend) ThemePositive.copy(alpha = 0.7f) else ThemeNegative.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPositiveTrend) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isPositiveTrend) ThemePositive else ThemeNegative,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    CurrencyText(
                                        text = "${if (isPositiveTrend) "+" else ""}${String.format(java.util.Locale.US, "%.1f%%", momentumPct)}",
                                        color = if (isPositiveTrend) ThemePositive else ThemeNegative,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyText(
                                text = formatCredit(netWorth),
                                color = ThemeGold,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Mini Canvas Sparkline
                            CompanyMiniSparkline(
                                history = sparklineHistory,
                                isPositive = isPositiveTrend,
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Column: Cashflow Velocity + Daily Income
                    Column(
                        modifier = Modifier.weight(0.85f, fill = false),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            border = BorderStroke(0.7.dp, ThemePositive.copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ThemePositive.copy(alpha = glowAlpha))
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    val sign = if (cashflowVelocity >= 0) "+" else ""
                                    CurrencyText(
                                        text = "$sign${formatCredit(cashflowVelocity)} ₳/${tr("dk", "m", isEng)}",
                                        color = ThemePositive,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Günlük Kâr
                        val profitColor = if (dailyIncome >= 0) ThemePositive else ThemeNegative
                        val sign = if (dailyIncome >= 0) "+" else ""
                        CurrencyText(
                            text = "${tr("Günlük", "Daily", isEng)}: $sign${formatCredit(dailyIncome)}",
                            color = profitColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. ASSET ALLOCATION TILES (Nakit, Tesis, Depo, Ar-Ge 4-Sütunlu Ferah Görünüm)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0A1326).copy(alpha = 0.80f),
                    border = BorderStroke(0.7.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAssetAllocationDialog = true }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nakit & Banka
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = tr("💵 Nakit", "💵 Cash", isEng),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = "${formatCompactNumber(cashAndDeposit)} ₳ (%$pctCash)",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Tesis & Sanayi
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = tr("🏭 Tesis", "🏭 Facilities", isEng),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = "${formatCompactNumber(facilityVal)} ₳ (%$pctFac)",
                                    color = Color(0xFF06B6D4),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Depo & Emtia
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = tr("📦 Depo", "📦 Warehouse", isEng),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = "${formatCompactNumber(invVal)} ₳ (%$pctInv)",
                                    color = Color(0xFF10B981),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Ar-Ge & Teknoloji
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = tr("🔬 Ar-Ge", "🔬 R&D", isEng),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = "${formatCompactNumber(rdVal)} ₳ (%$pctRd)",
                                    color = Color(0xFFA855F7),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Multi-Segment Horizontal Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            if (weightCash > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(weightCash)
                                        .fillMaxHeight()
                                        .background(Color(0xFFF59E0B))
                                )
                            }
                            if (weightFac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(weightFac)
                                        .fillMaxHeight()
                                        .background(Color(0xFF06B6D4))
                                )
                            }
                            if (weightInv > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(weightInv)
                                        .fillMaxHeight()
                                        .background(Color(0xFF10B981))
                                )
                            }
                            if (weightRd > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(weightRd)
                                        .fillMaxHeight()
                                        .background(Color(0xFFA855F7))
                                )
                            }
                            if (weightOther > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(weightOther)
                                        .fillMaxHeight()
                                        .background(Color(0xFF38BDF8))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. OPERATIONAL HEALTH & DIRECTIVE ROW
                val totalBusinesses = uiState.businesses.size
                val activeBusinesses = uiState.businesses.count { !it.isUpgrading && it.wearLevel < 0.90f }
                val avgHealthPct = if (totalBusinesses > 0) {
                    ((1f - uiState.businesses.map { it.wearLevel }.average().toFloat()) * 100f).toInt().coerceIn(0, 100)
                } else 100
                val invFillPct = (uiState.inventoryFillPercentage * 100f).toInt().coerceIn(0, 100)
                val deliveriesCount = uiState.inventoryState.activeDeliveries.size

                val activePolicyDirectiveText = remember(uiState.managers, uiState.hrState.activeResearches, player.level, isEng) {
                    val hiredManager = uiState.managers.firstOrNull { it.isHired && it.isActive }
                    when {
                        hiredManager != null -> {
                            when (hiredManager.specialty) {
                                "rd" -> tr("Ar-Ge Hızlandırma (-%15)", "R&D Acceleration (-15%)", isEng)
                                "treasury" -> tr("Hazine ve Likidite (+%10)", "Treasury (+10%)", isEng)
                                "logistics" -> tr("Lojistik (-%20 Navlun)", "Logistics (-20%)", isEng)
                                "contracts" -> tr("B2B Vadeli Önceliği", "B2B Futures Priority", isEng)
                                "maintenance" -> tr("Tesis Koruma (-%30 Aşınma)", "Maintenance (-30%)", isEng)
                                else -> "${hiredManager.title} ${tr("Direktifi", "Directive", isEng)}"
                            }
                        }
                        uiState.hrState.activeResearches.isNotEmpty() -> {
                            tr("Ar-Ge Teşviki", "R&D Incentive", isEng)
                        }
                        player.level <= 3 -> {
                            tr("Girişimci Teşviki", "Entrepreneur Incentive", isEng)
                        }
                        else -> {
                            tr("Serbest Ticaret", "Free Trade", isEng)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(0.7.dp, Color(0xFF334155).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyText(
                                text = "🏭 $activeBusinesses/$totalBusinesses • %$avgHealthPct",
                                color = Color(0xFFCBD5E1),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily
                            )
                            CurrencyText(
                                text = "📦 %$invFillPct",
                                color = if (invFillPct > 85) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily
                            )
                            CurrencyText(
                                text = "🚚 ${if (deliveriesCount > 0) "$deliveriesCount" else "0"}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = activePolicyDirectiveText,
                                color = Color(0xFFE2E8F0),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AssetLegendItem(
    color: Color,
    label: String,
    pct: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(4.5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        CurrencyText(
            text = "$label %$pct",
            color = Color(0xFF94A3B8),
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = RobotoMonoFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CompanyMiniSparkline(
    history: List<Long>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (isPositive) ThemePositive else ThemeNegative
    val fillColor = if (isPositive) ThemePositive.copy(alpha = 0.22f) else ThemeNegative.copy(alpha = 0.22f)

    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val minVal = history.minOrNull()?.toFloat() ?: 0f
        val maxVal = history.maxOrNull()?.toFloat() ?: 1f
        val range = if (maxVal > minVal) (maxVal - minVal) else 1f

        val stepX = if (history.size > 1) width / (history.size - 1) else width

        val path = Path()
        val fillPath = Path()

        history.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value.toFloat() - minVal) / range).coerceIn(0f, 1f)
            val y = height - (normalizedY * (height - 4.dp.toPx())) - 2.dp.toPx()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Pulsing end point dot
        val lastVal = (history.lastOrNull() ?: 0L).toFloat()
        val lastNorm = ((lastVal - minVal) / range).coerceIn(0f, 1f)
        val lastX = width
        val lastY = height - (lastNorm * (height - 4.dp.toPx())) - 2.dp.toPx()
        drawCircle(
            color = strokeColor,
            radius = 2.2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
        )
    }
}

@Composable
fun AssetAllocationDetailDialog(
    player: com.example.data.PlayerEntity,
    uiState: GameUiState,
    cashAndDeposit: Long,
    facilityVal: Long,
    invVal: Long,
    rdVal: Long = 0L,
    otherVal: Long,
    totalNetWorth: Long,
    pctCash: Int,
    pctFac: Int,
    pctInv: Int,
    pctRd: Int = 0,
    pctOther: Int,
    isEng: Boolean,
    onDismiss: () -> Unit,
    onBalanceSheetClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF070E20),
            border = BorderStroke(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(ThemeGold.copy(alpha = 0.8f), ThemeNeonCyan.copy(alpha = 0.5f), Color(0xFF1E293B))
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PieChart,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            CurrencyText(
                                text = tr("Holding Varlık Dağılımı", "Holding Asset Allocation", isEng),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            CurrencyText(
                                text = tr("Portföy ve Likidite Analizi", "Portfolio & Liquidity Analysis", isEng),
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr("Kapat", "Close", isEng),
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Net Worth Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(0.8.dp, ThemeGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(
                                text = tr("TOPLAM KONSOLİDE NET SERVET", "TOTAL CONSOLIDATED NET WORTH", isEng),
                                color = Color(0xFF94A3B8),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            CurrencyText(
                                text = formatCredit(totalNetWorth),
                                color = ThemeGold,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, ThemeGold.copy(alpha = 0.5f))
                        ) {
                            CurrencyText(
                                text = "AAA ${tr("NOTU", "RATING", isEng)}",
                                color = ThemeGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detailed Asset Breakdown List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssetDetailCard(
                        accentColor = Color(0xFFF59E0B),
                        title = tr("Nakit & Banka Mevduatı", "Cash & Bank Deposit", isEng),
                        percentage = pctCash,
                        value = formatCredit(cashAndDeposit),
                        description = tr("Kasa nakdi, vadesiz banka hesabı ve vadeli mevduat fonları.", "Cash in hand, checking accounts and time deposit funds.", isEng)
                    )

                    AssetDetailCard(
                        accentColor = Color(0xFF06B6D4),
                        title = tr("Sanayi & Tesisler", "Industry & Facilities", isEng),
                        percentage = pctFac,
                        value = formatCredit(facilityVal),
                        description = tr("${uiState.businesses.size} aktif fabrika, çiftlik ve atölye sermaye değeri.", "${uiState.businesses.size} active factories, farms and workshop valuation.", isEng)
                    )

                    AssetDetailCard(
                        accentColor = Color(0xFF10B981),
                        title = tr("Depodaki Emtia & Hammadde", "Warehouse Commodities", isEng),
                        percentage = pctInv,
                        value = formatCredit(invVal),
                        description = tr("Merkezi depo ve fabrika ambarlarındaki stoklar (%80 değerleme).", "Commercial commodities in central and factory warehouses (80% val).", isEng)
                    )

                    if (rdVal > 0L) {
                        AssetDetailCard(
                            accentColor = ThemeNeonCyan,
                            title = tr("🔬 Ar-Ge & Teknoloji Yatırımları", "🔬 R&D & Technology Capital", isEng),
                            percentage = pctRd,
                            value = formatCredit(rdVal),
                            description = tr("Kazanılan ve devam eden teknoloji araştırmaları için harcanan nakit sermaye.", "Cash capital invested in completed and ongoing technology research.", isEng)
                        )
                    }

                    AssetDetailCard(
                        accentColor = Color(0xFFA855F7),
                        title = tr("Tahvil, Borsa & İştirakler", "Bonds, Stocks & Subsidiaries", isEng),
                        percentage = pctOther,
                        value = formatCredit(otherVal),
                        description = tr("Konsorsiyum mega proje payları, hisse senetleri ve döviz fonları.", "Consortium mega-project shares, stocks and foreign exchange.", isEng)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CurrencyText(
                            text = tr("Kapat", "Close", isEng),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onBalanceSheetClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF0F172A)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        CurrencyText(
                            text = tr("Bilanço Raporu", "Balance Sheet", isEng),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetDetailCard(
    accentColor: Color,
    title: String,
    percentage: Int,
    value: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0D1728),
        border = BorderStroke(0.6.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = title,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = "%$percentage",
                        color = accentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        maxLines = 1
                    )
                }
                CurrencyText(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CurrencyText(
                text = value,
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DashboardMenuItemCard(
    item: DashboardMenuButton,
    isLocked: Boolean,
    lockInfo: FeatureLockInfo?,
    isGuestLocked: Boolean = false,
    onGuestLockClick: (() -> Unit)? = null,
    themeOption: AppThemeOption,
    modifier: Modifier = Modifier,
    onLockClick: (FeatureLockInfo) -> Unit
) {
    val showAsLocked = isLocked || isGuestLocked

    Interactive3DCard(
        shape = RoundedCornerShape(14.dp),
        maxTiltAngle = 14f,
        specularShine = true,
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (showAsLocked) Color(0xFF070D18).copy(alpha = 0.85f)
                    else if (themeOption.isDark) Color(0xFF0F182E).copy(alpha = 0.90f)
                    else themeOption.surfaceVariantColor.copy(alpha = 0.90f),
            border = BorderStroke(
                1.2.dp,
                if (showAsLocked) Color(0xFFEF4444).copy(alpha = 0.45f)
                else ThemeGold.copy(alpha = 0.35f)
            ),
            shadowElevation = if (showAsLocked) 0.dp else 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isGuestLocked && onGuestLockClick != null) {
                        onGuestLockClick()
                    } else if (isLocked && lockInfo != null) {
                        onLockClick(lockInfo)
                    } else {
                        item.action()
                    }
                }
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = if (showAsLocked) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFF16213B),
                    border = BorderStroke(1.2.dp, if (showAsLocked) Color(0xFF475569) else ThemeGold.copy(alpha = 0.60f)),
                    shadowElevation = if (showAsLocked) 0.dp else 6.dp,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = if (showAsLocked) 0.30f else 1f
                        )
                        if (showAsLocked) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        } else if (item.badgeText != null) {
                            // Live status indicator badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = item.badgeColor,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                            ) {
                                CurrencyText(
                                    text = item.badgeText,
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                CurrencyText(
                    text = if (isLocked) "🔒 ${item.label}" else item.label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLocked) Color(0xFF94A3B8) else if (themeOption.isDark) Color.White else themeOption.textPrimaryColor,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

