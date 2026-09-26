package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    onDismiss: () -> Unit,
    onNavigateToCity: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val isEnglish = isEnglishLanguage()
    val tabs = listOf(
        tr("ŞEHİR FIRSATLARI", "CITY OPPORTUNITIES"),
        tr("MAKRO & FAİZ", "MACRO & INTEREST"),
        tr("PİYASA BÜLTENİ", "MARKET BULLETIN")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
                                text = tr("Canlı Piyasa Olayları, Makro & Fırsat Radarı", "Live Market Events, Macro & Opportunity Radar"),
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
                                color = if (isSelected) Color(0xFF050B14) else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // City Events Tab
                        if (events.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🌾", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tr(
                                            "Şu anda bölgesel olağanüstü olay bulunmuyor.\nŞehirlerde fiyatlar dengeli seyrediyor.",
                                            "There are currently no regional extraordinary events.\nPrices in cities are stable."
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                            }
                        }
                    }
                    1 -> {
                        // Macro & Central Bank Tab
                        val state = macroState ?: MacroEconomyState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
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
                        // General News Bulletin Tab
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
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
