package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.CityMarketEvent
import com.example.data.MacroEconomyState
import com.example.data.EconomicCycle
import com.example.data.BulletinOpportunity
import com.example.data.BusinessEntity
import com.example.ui.theme.*

/**
 * Real-time Breaking News ticker banner that scrolls news headlines across Anatolia,
 * Central Bank bulletins, macro trends, and commodity radar updates.
 */
@Composable
fun LiveBreakingNewsBanner(
    events: List<CityMarketEvent> = emptyList(),
    newsTickerMessage: String = "",
    macroState: MacroEconomyState? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "news_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isEnglish = isEnglishLanguage()

    // Build comprehensive news feed
    val newsFeed = remember(events, newsTickerMessage, macroState, isEnglish) {
        val items = mutableListOf<NewsTickerItem>()

        // 1. City Events (Highest Priority)
        events.forEach { event ->
            items.add(
                NewsTickerItem(
                    badge = tr("TR HABER", "TR NEWS", isEnglish),
                    badgeColor = Color(0xFFD32F2F),
                    title = "📍 ${event.getCityName(isEnglish)}: ${event.getHeadline(isEnglish)}",
                    detail = "${event.percentFormatted} (${event.getStrategyTip(isEnglish)})",
                    remaining = event.remainingFormatted
                )
            )
        }

        // 2. Macro State & Central Bank
        if (macroState != null) {
            val (cycleName, cycleColor) = when (macroState.cycle) {
                EconomicCycle.BOOM -> tr("BOĞA PİYASASI", "BULL MARKET", isEnglish) to ThemePositive
                EconomicCycle.PEAK -> tr("ZİRVE DÖNEMİ", "PEAK PERIOD", isEnglish) to ThemeGold
                EconomicCycle.RECESSION -> tr("RESESYON", "RECESSION", isEnglish) to ThemeNegative
                EconomicCycle.DEPRESSION -> tr("BUHRAN", "DEPRESSION", isEnglish) to Color(0xFFB71C1C)
                EconomicCycle.RECOVERY -> tr("TOPARLANMA", "RECOVERY", isEnglish) to ThemeNeonCyan
            }
            items.add(
                NewsTickerItem(
                    badge = tr("TCMB & MAKRO", "CBRT & MACRO", isEnglish),
                    badgeColor = Color(0xFF1E88E5),
                    title = "🏛️ $cycleName",
                    detail = tr(
                        "Politika Faizi %${String.format(java.util.Locale.US, "%.1f", macroState.centralBankInterestRate * 100)} | Enflasyon %${String.format(java.util.Locale.US, "%.1f", macroState.globalInflationRate * 100)}",
                        "Policy Interest Rate %${String.format(java.util.Locale.US, "%.1f", macroState.centralBankInterestRate * 100)} | Inflation %${String.format(java.util.Locale.US, "%.1f", macroState.globalInflationRate * 100)}",
                        isEnglish
                    ),
                    remaining = null
                )
            )
        }

        // 3. Dynamic News Messages
        if (newsTickerMessage.isNotBlank()) {
            val parts = newsTickerMessage.split(" | ")
            parts.forEach { part ->
                if (part.isNotBlank()) {
                    val badge = when {
                        part.contains("BORSA") -> tr("BORSA FLAŞ", "EXCHANGE FLASH", isEnglish)
                        part.contains("TCMB") || part.contains("Merkez Bankası") -> tr("MERKEZ BANKASI", "CENTRAL BANK", isEnglish)
                        part.contains("TR HABER") -> tr("TR HABER", "TR NEWS", isEnglish)
                        part.contains("MÜJDE") || part.contains("İHRACAT") -> tr("İHRACAT", "EXPORT", isEnglish)
                        part.contains("TURİZM") -> tr("TURİZM", "TOURISM", isEnglish)
                        part.contains("SANAYİ") || part.contains("YÜKSELİŞ") -> tr("SANAYİ", "INDUSTRY", isEnglish)
                        part.contains("KRİZ") || part.contains("ÇÖKÜŞ") -> tr("PİYASA ALARMI", "MARKET ALERT", isEnglish)
                        else -> tr("EKONOMİ", "ECONOMY", isEnglish)
                    }
                    val badgeColor = when {
                        part.contains("BORSA") -> ThemeGold
                        part.contains("TCMB") || part.contains("Merkez Bankası") -> Color(0xFF1E88E5)
                        part.contains("TR HABER") -> Color(0xFFD32F2F)
                        part.contains("MÜJDE") || part.contains("İHRACAT") || part.contains("SANAYİ") || part.contains("YÜKSELİŞ") -> ThemePositive
                        part.contains("KRİZ") || part.contains("ÇÖKÜŞ") -> ThemeNegative
                        else -> Color(0xFF8B5CF6)
                    }
                    items.add(
                        NewsTickerItem(
                            badge = badge,
                            badgeColor = badgeColor,
                            title = part.replace(Regex("^(BORSA FLAŞ:|TR HABER:|EKONOMİ:|SON DAKİKA:|PİYASA:|KRİZ KAPIDA:|BUHRAN:|TOPARLANMA:|YÜKSELİŞ:|PİYASA ALARMI:|MÜJDE:|TURİZM REKORU:)\\s*"), "").trAuto(isEnglish),
                            detail = "",
                            remaining = null
                        )
                    )
                }
            }
        }

        if (items.isEmpty()) {
            items.add(
                NewsTickerItem(
                    badge = tr("CANLI HABER", "LIVE NEWS", isEnglish),
                    badgeColor = ThemeNeonCyan,
                    title = tr("Türkiye emtia piyasaları ve Borsa İstanbul işlemleri aktif olarak güncelleniyor.", "Türkiye commodity markets and Borsa Istanbul operations are actively updated.", isEnglish),
                    detail = tr("Tüm sektörlerde canlı fiyat takibi devrede.", "Live price tracking is active in all sectors.", isEnglish),
                    remaining = null
                )
            )
        }
        items
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF0A0F1D).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Brush.horizontalGradient(
            listOf(
                Color(0xFFE53935).copy(alpha = 0.85f),
                ThemeGold.copy(alpha = 0.75f),
                ThemeNeonCyan.copy(alpha = 0.85f)
            )
        )),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Header Art with Gradient Mask
            Image(
                painter = painterResource(id = R.drawable.bg_news_ticker_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.35f)
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0F172A).copy(alpha = 0.90f),
                                Color(0xFF0F172A).copy(alpha = 0.65f),
                                Color(0xFF0F172A).copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Live Red Badge with News Icon and Pulsing Dot
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.8.dp, ThemeGold.copy(alpha = 0.7f)),
                    modifier = Modifier.size(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_news_ticker_header),
                        contentDescription = "TR Haber",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF334B).copy(alpha = pulseAlpha))
                )
                Text(
                    text = tr("TR HABER", "TR NEWS", isEnglish),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Scrolling Multi-Item News Wire
            Row(
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                verticalAlignment = Alignment.CenterVertically
            ) {
                newsFeed.forEachIndexed { index, item ->
                    // Mini Category Badge
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = item.badgeColor.copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, item.badgeColor)
                    ) {
                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF1F5F9)
                    )

                    if (item.detail.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${item.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = ThemeGold
                        )
                    }

                    if (item.remaining != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "⏱ ${item.remaining}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = ThemeNeonCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Separator between news
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "⚡",
                        fontSize = 10.sp,
                        color = ThemeGold.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Chevron
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = tr("Tüm Haberler", "All News"),
                tint = ThemeGold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
}

data class NewsTickerItem(
    val badge: String,
    val badgeColor: Color,
    val title: String,
    val detail: String,
    val remaining: String?
)

/**
 * Full News Bulletin, Macroeconomic Status, and Opportunity Radar Dialog.
 */
@Composable
fun CityNewsBulletinDialog(
    events: List<CityMarketEvent> = emptyList(),
    newsTickerMessage: String = "",
    macroState: MacroEconomyState? = null,
    bulletinOpportunities: List<BulletinOpportunity> = emptyList(),
    ownedFacilities: List<BusinessEntity> = emptyList(),
    onClaimReward: ((String) -> Unit)? = null,
    onQuickProduce: ((String) -> Unit)? = null,
    onNavigateToFacility: ((productId: String, cityId: String) -> Unit)? = null,
    onNavigateToBorsa: (() -> Unit)? = null,
    onNavigateToMarket: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onNavigateToCity: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("all") }
    val isEnglish = isEnglishLanguage()
    val activeOppCount = bulletinOpportunities.size
    val tabs = listOf(
        tr("🎯 CANLI FIRSATLAR ($activeOppCount)", "🎯 LIVE OPPORTUNITIES ($activeOppCount)"),
        tr("🏛️ MAKRO & FAİZ", "🏛️ MACRO & INTEREST"),
        tr("📰 ŞEHİR & PİYASA", "📰 CITY & MARKET")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(ThemeGold, ThemeNeonCyan)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE53935).copy(alpha = 0.2f),
                            border = BorderStroke(1.2.dp, Color(0xFFE53935).copy(alpha = 0.6f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.bg_news_ticker_header),
                                contentDescription = "TR Haber",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = tr("ANADOLU EKONOMİ BÜLTENİ", "ANATOLIAN ECONOMY BULLETIN"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = ThemeGold
                            )
                            Text(
                                text = tr("Bölgesel Kâr Fırsatları, Makro Trendler & Görevler", "Regional Profit Opportunities, Macro Trends & Quests"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr("Kapat", "Close"), tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF080C14))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ThemeNeonCyan else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTab = index
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF050B14) else Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Regional Opportunities Tab (Active Events with City/Region Filters)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top Info Banner with Quick Produce info
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚡", fontSize = 13.sp)
                                    Text(
                                        text = tr(
                                            "Bültenden tek tıkla doğrudan tesis üretim emri verin! Tamamlanan görevlerde 1-10 Elmas kazanın.",
                                            "Dispatch 1-tap facility production directly from the bulletin! Earn 1-10 Diamonds on completion."
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = ThemeGold
                                    )
                                }
                            }

                            // Region & City Filter Chips Bar
                            if (bulletinOpportunities.isNotEmpty()) {
                                val turkeyCount = remember(bulletinOpportunities) { bulletinOpportunities.count { !it.isGlobal } }
                                val globalCount = remember(bulletinOpportunities) { bulletinOpportunities.count { it.isGlobal } }
                                val distinctCities = remember(bulletinOpportunities, isEnglish) {
                                    bulletinOpportunities.map { opp ->
                                        Triple(opp.cityId, opp.getCityName(isEnglish), opp.countryFlag)
                                    }.distinctBy { it.first }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // "All" filter
                                    val isAll = selectedFilter == "all"
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isAll) ThemeNeonCyan else Color(0xFF101935),
                                        border = BorderStroke(1.dp, if (isAll) ThemeNeonCyan else Color.White.copy(alpha = 0.15f)),
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedFilter = "all"
                                        }
                                    ) {
                                        Text(
                                            text = tr("🌐 Tümü (${bulletinOpportunities.size})", "🌐 All (${bulletinOpportunities.size})"),
                                            fontSize = 10.sp,
                                            fontWeight = if (isAll) FontWeight.Black else FontWeight.Medium,
                                            color = if (isAll) Color(0xFF050B14) else Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // "Türkiye" filter
                                    if (turkeyCount > 0) {
                                        val isTurkey = selectedFilter == "turkey"
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isTurkey) ThemeGold else Color(0xFF101935),
                                            border = BorderStroke(1.dp, if (isTurkey) ThemeGold else Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedFilter = "turkey"
                                            }
                                        ) {
                                            Text(
                                                text = "🇹🇷 Türkiye ($turkeyCount)",
                                                fontSize = 10.sp,
                                                fontWeight = if (isTurkey) FontWeight.Black else FontWeight.Medium,
                                                color = if (isTurkey) Color(0xFF050B14) else Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // "Global" filter
                                    if (globalCount > 0) {
                                        val isGlobal = selectedFilter == "global"
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isGlobal) Color(0xFF0284C7) else Color(0xFF101935),
                                            border = BorderStroke(1.dp, if (isGlobal) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedFilter = "global"
                                            }
                                        ) {
                                            Text(
                                                text = tr("🌍 Küresel ($globalCount)", "🌍 Global ($globalCount)"),
                                                fontSize = 10.sp,
                                                fontWeight = if (isGlobal) FontWeight.Black else FontWeight.Medium,
                                                color = if (isGlobal) Color.White else Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Individual City filters
                                    distinctCities.forEach { (cityId, cityName, flag) ->
                                        val isCity = selectedFilter == cityId
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isCity) ThemeNeonCyan.copy(alpha = 0.85f) else Color(0xFF101935),
                                            border = BorderStroke(1.dp, if (isCity) ThemeNeonCyan else Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedFilter = cityId
                                            }
                                        ) {
                                            Text(
                                                text = "$flag $cityName",
                                                fontSize = 10.sp,
                                                fontWeight = if (isCity) FontWeight.Black else FontWeight.Normal,
                                                color = if (isCity) Color(0xFF050B14) else Color.White.copy(alpha = 0.75f),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            val filteredOpportunities = remember(bulletinOpportunities, selectedFilter) {
                                when (selectedFilter) {
                                    "all" -> bulletinOpportunities
                                    "turkey" -> bulletinOpportunities.filter { !it.isGlobal }
                                    "global" -> bulletinOpportunities.filter { it.isGlobal }
                                    else -> bulletinOpportunities.filter { it.cityId.equals(selectedFilter, ignoreCase = true) }
                                }
                            }

                            if (filteredOpportunities.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔍", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = tr(
                                                "Seçili bölge/şehir filtresine uygun aktif fırsat bulunamadı.",
                                                "No active opportunities found for the selected region/city filter."
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 420.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredOpportunities, key = { it.id }) { opp ->
                                        BulletinOpportunityCard(
                                            opportunity = opp,
                                            ownedFacilities = ownedFacilities,
                                            onClaimReward = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onClaimReward?.invoke(opp.id)
                                            },
                                            onQuickProduce = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onQuickProduce?.invoke(opp.id)
                                            },
                                            onNavigateToFacility = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDismiss()
                                                onNavigateToFacility?.invoke(opp.targetProductId, opp.cityId)
                                            },
                                            onNavigateToBorsa = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDismiss()
                                                onNavigateToBorsa?.invoke()
                                            },
                                            onNavigateToMarket = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDismiss()
                                                onNavigateToMarket?.invoke()
                                            },
                                            onGoToCity = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDismiss()
                                                onNavigateToCity(opp.cityId)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Macro & Central Bank Tab
                        val state = macroState ?: MacroEconomyState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cycle Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF101935),
                                border = BorderStroke(1.dp, ThemeBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tr("DÖNGÜ FAZI", "CYCLE PHASE"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                                        Text(
                                            text = tr(
                                                state.cycle.name,
                                                when (state.cycle) {
                                                    EconomicCycle.BOOM -> "BULL MARKET"
                                                    EconomicCycle.PEAK -> "PEAK PERIOD"
                                                    EconomicCycle.RECESSION -> "RECESSION"
                                                    EconomicCycle.DEPRESSION -> "DEPRESSION"
                                                    EconomicCycle.RECOVERY -> "RECOVERY"
                                                }
                                            ),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemePositive
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val cycleDesc = when (state.cycle) {
                                        EconomicCycle.BOOM -> tr(
                                            "Ekonomi tam büyüme aşamasında. Tüketici güveni yüksek, Tier 4 ve lüks ürünler en yüksek kârlılığa ulaştı.",
                                            "The economy is in full expansion. Consumer confidence is high, Tier 4 and luxury products have reached peak profitability."
                                        )
                                        EconomicCycle.PEAK -> tr(
                                            "Ekonomi tepe noktasına ulaştı. Yüksek enflasyon riski nedeniyle Merkez Bankası sıkılaşma adımları atabilir.",
                                            "The economy has reached its peak. The Central Bank may take tightening steps due to high inflation risk."
                                        )
                                        EconomicCycle.RECESSION -> tr(
                                            "Resesyon dönemi. Kredi faizleri artabilir, temel gıda ve tarım ürünleri daha güvenli limandır.",
                                            "Recession period. Credit rates may rise, basic foods and agricultural products are safer havens."
                                        )
                                        EconomicCycle.DEPRESSION -> tr(
                                            "Piyasalar dip seviyede. Tesis yatırımı yapmak ve ucuz hammadde toplamak için tarihi alım fırsatı.",
                                            "Markets are at bottom level. Historical buying opportunity to make plant investments and gather cheap raw materials."
                                        )
                                        EconomicCycle.RECOVERY -> tr(
                                            "Ekonomi toparlanma sürecinde. Sanayi üretimi ve borsa işlem hacmi canlanıyor.",
                                            "The economy is in recovery. Industrial production and exchange trading volumes are reviving."
                                        )
                                    }
                                    Text(cycleDesc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                                }
                            }

                            // Key Indicators
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF101935),
                                    border = BorderStroke(1.dp, ThemeBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(tr("TCMB POLİTİKA FAİZİ", "CBRT POLICY INTEREST RATE"), fontSize = 9.5.sp, color = ThemeNeonCyan, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("%${String.format(java.util.Locale.US, "%.1f", state.centralBankInterestRate * 100)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        Text(tr("Mevduat & Kredi Oranı", "Deposit & Loan Rate"), fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF101935),
                                    border = BorderStroke(1.dp, ThemeBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(tr("YILLIK ENFLASYON", "ANNUAL INFLATION"), fontSize = 9.5.sp, color = ThemeGold, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("%${String.format(java.util.Locale.US, "%.1f", state.globalInflationRate * 100)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        Text(tr("Tüketici Fiyat Endeksi", "Consumer Price Index"), fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            // Strategy Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D251D),
                                border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("💡", fontSize = 16.sp)
                                    Text(
                                        tr(
                                            "Merkez Bankası faiz kararlarına göre mevduat faiz getirileri veya kredi maliyetleri anlık olarak banka ekranına yansır.",
                                            "According to Central Bank interest rate decisions, deposit yields or loan costs are reflected instantly on the bank screen."
                                        ),
                                        fontSize = 11.sp,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Combined City Events & Market News
                        val stories = remember(newsTickerMessage, isEnglish) {
                            if (newsTickerMessage.isNotBlank()) {
                                newsTickerMessage.split(" | ").filter { it.isNotBlank() }.map { it.trAuto(isEnglish) }
                            } else {
                                listOf(
                                    tr(
                                        "Borsa İstanbul emtia endeksinde işlem hacmi artışı sürüyor.",
                                        "Trading volume continues to grow in the Borsa Istanbul commodity index.",
                                        isEnglish
                                    ),
                                    tr(
                                        "Türkiye geneli sanayi ve lojistik koridorlarında kesintisiz sevkiyat devam ediyor.",
                                        "Uninterrupted shipment continues in industrial and logistics corridors across Türkiye.",
                                        isEnglish
                                    ),
                                    tr(
                                        "Avrupa ve Orta Doğu ticaret hatlarında yeni ihracat sözleşmeleri imzalandı.",
                                        "New export contracts were signed in European and Middle Eastern trade lines.",
                                        isEnglish
                                    )
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (events.isNotEmpty()) {
                                item {
                                    Text(
                                        text = tr("⚡ BÖLGESEL PİYASA OLAYLARI", "⚡ REGIONAL MARKET EVENTS"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(events, key = { it.id }) { event ->
                                    CityEventCard(
                                        event = event,
                                        onGoToCity = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDismiss()
                                            onNavigateToCity(event.cityId)
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            item {
                                Text(
                                    text = tr("📢 PİYASA GELİŞMELERİ & MANŞETLER", "📢 MARKET NEWS & HEADLINES"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeNeonCyan,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(stories, key = { it }) { story ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF101935),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📢", fontSize = 14.sp)
                                        Text(story, fontSize = 11.5.sp, color = Color(0xFFF1F5F9), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Kapat", "Close"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BulletinOpportunityCard(
    opportunity: BulletinOpportunity,
    ownedFacilities: List<BusinessEntity>,
    onClaimReward: () -> Unit,
    onQuickProduce: () -> Unit,
    onNavigateToFacility: () -> Unit,
    onNavigateToBorsa: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onGoToCity: () -> Unit
) {
    val isEnglish = isEnglishLanguage()
    val isCompleted = opportunity.isCompleted
    val isClaimed = opportunity.isClaimed

    // Check facility ownership
    val ownsFacilityInCity = ownedFacilities.any {
        it.cityId.equals(opportunity.cityId, ignoreCase = true) &&
        (it.type.equals(opportunity.targetFacilityId, ignoreCase = true) || it.type.contains(opportunity.targetProductId, ignoreCase = true))
    }
    val ownsFacilityAnywhere = ownedFacilities.any {
        it.type.equals(opportunity.targetFacilityId, ignoreCase = true) || it.type.contains(opportunity.targetProductId, ignoreCase = true)
    }
    val hasFacility = ownsFacilityInCity || ownsFacilityAnywhere

    val borderColor = when {
        isCompleted && !isClaimed -> ThemeGold
        isClaimed -> Color(0xFF10B981).copy(alpha = 0.6f)
        else -> ThemeNeonCyan.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF101935).copy(alpha = 0.95f),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top badges row: City, Category, Verified Badge, Premium, Diamonds
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F223D),
                        border = BorderStroke(0.8.dp, ThemeNeonCyan.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "${opportunity.countryFlag} ${opportunity.iconEmoji} 📍 ${opportunity.getCityName(isEnglish).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = ThemeGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(opportunity.category.badgeColorHex).copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, Color(opportunity.category.badgeColorHex))
                    ) {
                        Text(
                            text = opportunity.category.getTitle(isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Verified Database Match Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF042F2E).copy(alpha = 0.7f),
                        border = BorderStroke(0.5.dp, Color(0xFF14B8A6))
                    ) {
                        Text(
                            text = tr("🛡️ Onaylı Tesis", "🛡️ Verified"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5EEAD4),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Price Premium
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ThemePositive.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, ThemePositive)
                    ) {
                        Text(
                            text = "${opportunity.pricePremiumText} ${tr("Prim", "Premium")}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemePositive,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Diamond Reward
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ThemeGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ThemeGold)
                    ) {
                        Text(
                            text = "💎 ${opportunity.diamondReward} ${tr("Elmas", "Dia")}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Headline & Description
            Text(
                text = opportunity.getHeadline(isEnglish),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = opportunity.getDescription(isEnglish),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Strategy Tip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F2E22).copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, ThemePositive.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("💡", fontSize = 12.sp)
                    Text(
                        text = opportunity.getStrategyTip(isEnglish),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Progress Section
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0B132B).copy(alpha = 0.8f),
                border = BorderStroke(0.5.dp, ThemeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(
                                "🎯 Hedef: ${opportunity.targetQuantity} Ton ${opportunity.getProductName(isEnglish)} Üret & Sat",
                                "🎯 Goal: Produce & Sell ${opportunity.targetQuantity} Tons of ${opportunity.getProductName(isEnglish)}"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = ThemeNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%${opportunity.overallProgressPercent}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (isCompleted) ThemePositive else ThemeGold,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (opportunity.overallProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isCompleted) Color(0xFF10B981) else ThemeNeonCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(
                                "🏭 Üretim: ${opportunity.producedQuantity}/${opportunity.targetQuantity} Ton",
                                "🏭 Prod: ${opportunity.producedQuantity}/${opportunity.targetQuantity} Tons"
                            ) + if (opportunity.isProductionTargetMet) " ✓" else "",
                            fontSize = 9.sp,
                            fontWeight = if (opportunity.isProductionTargetMet) FontWeight.Bold else FontWeight.Normal,
                            color = if (opportunity.isProductionTargetMet) ThemePositive else Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = tr(
                                "📦 Satış: ${opportunity.soldQuantity}/${opportunity.targetQuantity} Ton",
                                "📦 Sales: ${opportunity.soldQuantity}/${opportunity.targetQuantity} Tons"
                            ) + if (isCompleted) " ✓" else "",
                            fontSize = 9.sp,
                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCompleted) ThemePositive else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Smart Navigation & Action Buttons
            when {
                isCompleted && !isClaimed -> {
                    Button(
                        onClick = onClaimReward,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💎", fontSize = 14.sp)
                            Text(
                                text = tr(
                                    "${opportunity.diamondReward} ELMAS ÖDÜLÜNÜ TOPLA",
                                    "CLAIM ${opportunity.diamondReward} DIAMONDS"
                                ),
                                color = Color(0xFF050B14),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                isCompleted && isClaimed -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = tr(
                                "✅ Görev Tamamlandı & ${opportunity.diamondReward} Elmas Alındı",
                                "✅ Task Completed & ${opportunity.diamondReward} Diamonds Claimed"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                else -> {
                    // In Progress Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (hasFacility) {
                            if (opportunity.isProductionTargetMet) {
                                Button(
                                    onClick = onNavigateToMarket,
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text(
                                        text = tr("🛒 Pazarda Sat", "🛒 Market"),
                                        color = Color(0xFF050B14),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Button(
                                    onClick = onNavigateToBorsa,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text(
                                        text = tr("📈 Borsada Sat", "📈 Borsa"),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                // Direct 1-tap quick produce button!
                                Button(
                                    onClick = onQuickProduce,
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(34.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("⚡", fontSize = 12.sp)
                                        Text(
                                            text = tr("Hızlı Üret (1 Tık)", "Quick Produce"),
                                            color = Color(0xFF050B14),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = onNavigateToFacility,
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text(
                                        text = tr("🏭 Tesis Paneli", "🏭 Facility"),
                                        color = Color(0xFF050B14),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            // Does NOT have facility: Guide to build facility with recommended city
                            Button(
                                onClick = onNavigateToFacility,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5A93C)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(34.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "🏗️ ${opportunity.getCityName(isEnglish)}'de ${opportunity.getFacilityName(isEnglish)} Kur",
                                        "🏗️ Build ${opportunity.getFacilityName(isEnglish)} in ${opportunity.getCityName(isEnglish)}"
                                    ),
                                    color = Color(0xFF050B14),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onGoToCity,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ThemeBorder),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = tr("📍 Şehir", "📍 City"),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityEventCard(
    event: CityMarketEvent,
    onGoToCity: () -> Unit
) {
    val isUp = event.isPriceIncrease
    val isEnglish = isEnglishLanguage()
    val badgeColor = if (isUp) ThemePositive else ThemeNegative
    val borderColor = if (isUp) ThemePositive.copy(alpha = 0.5f) else Color(0xFFE53935).copy(alpha = 0.5f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF101935).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category & City Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(event.category.badgeColorHex).copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, Color(event.category.badgeColorHex))
                    ) {
                        Text(
                            text = event.category.getTitle(isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "📍 ${event.getCityName(isEnglish)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold
                    )
                }

                // Price Multiplier Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        text = tr("${event.percentFormatted} Fiyat", "${event.percentFormatted} Price"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = event.getHeadline(isEnglish),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = event.getDescription(isEnglish),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Strategy Tip Box
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F2E22).copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, ThemePositive.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("💡", fontSize = 14.sp)
                    Text(
                        text = event.getStrategyTip(isEnglish),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arz-Talep Esnekliği ve Pazar Doygunluğu Barı
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0B132B).copy(alpha = 0.8f),
                border = BorderStroke(0.5.dp, ThemeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("⚖️ Pazar Doygunluğu (Arz/Talep)", "⚖️ Market Saturation (Supply/Demand)"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = ThemeNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "%${event.saturationPercentage} (${event.deliveredVolume} / ${event.saturationCapacity} Ton)",
                                "%${event.saturationPercentage} (${event.deliveredVolume} / ${event.saturationCapacity} Tons)"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (event.saturationRatio >= 0.75f) ThemeGold else Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { event.saturationRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (event.saturationRatio >= 0.75f) ThemeGold else ThemeNeonCyan,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr("Başlangıç: ${event.initialPercentFormatted}", "Initial: ${event.initialPercentFormatted}"),
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (event.saturationRatio >= 1.0f) tr("Pazar Tam Doygun (1.00x)", "Market Fully Saturated (1.00x)") else tr("Kalan Prim: ${event.percentFormatted}", "Remaining Premium: ${event.percentFormatted}"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (event.isPriceIncrease) ThemePositive else ThemeGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Products & Navigate Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr("Etkilenen: ${event.affectedProductNames.joinToString(", ")}", "Affected: ${event.getAffectedProductNames(isEnglish).joinToString(", ")}"),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = onGoToCity,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tr("Haritada Gör", "View on Map"), color = Color(0xFF050B14), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
