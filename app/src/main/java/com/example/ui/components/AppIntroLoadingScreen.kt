package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeBackground
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.utils.GoogleAuthHelper
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.updateCompanyName
import com.example.viewmodel.signInAnonymously
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

private data class MarketTickerItem(
    val nameTr: String,
    val nameEn: String,
    val price: String,
    val change: String,
    val isUp: Boolean
)

@Composable
fun AppIntroLoadingScreen(
    gameViewModel: GameViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAppLanguage = LocalAppLanguage.current
    val isEnglish = currentAppLanguage == AppLanguage.ENGLISH

    var progress by remember { mutableFloatStateOf(0f) }
    var currentTipIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: İpuçları, 1: Güncelleme Notları
    var isDone by remember { mutableStateOf(false) }
    var showAuthChoice by remember { mutableStateOf(false) }
    var showGuestNameModal by remember { mutableStateOf(false) }
    var guestTraderName by remember { mutableStateOf("") }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleStatusMsg by remember { mutableStateOf("") }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Real-time market ticker sample data
    val marketTickers = remember {
        listOf(
            MarketTickerItem("BİST 100", "BIST 100", "9,842.50", "+%1.8", true),
            MarketTickerItem("Çelik", "Steel", "₳1,240", "+%4.2", true),
            MarketTickerItem("İHA / SİHA", "UAV / Drone", "₳84,000", "+%5.6", true),
            MarketTickerItem("Ham Petrol", "Crude Oil", "₳480", "-%0.8", false),
            MarketTickerItem("Buğday", "Wheat", "₳280", "+%2.1", true),
            MarketTickerItem("Otomotiv", "Automotive", "₳62,500", "+%3.4", true),
            MarketTickerItem("Külçe Altın", "Gold Bullion", "₳3,150", "+%0.9", true),
            MarketTickerItem("Krom & Bakır", "Chromium", "₳820", "-%1.2", false)
        )
    }

    val tipsTurkish = remember {
        listOf(
            "🏛️ Canlı Müzayedeler: Kültür Bakanlığı açık artırmalarına katılarak nadir tarihi eserleri toplayın ve holding prestijinizi katlayın.",
            "📈 Borsa & Halka Arz: Holdinginizi Borsa İstanbul'da (BİST) halka arz edin, hisse ihraç ederek devasa sermaye kaynağı yaratın.",
            "🏭 Sanayi Zinciri: Demir ve Kömürden Çelik üreterek Otomotiv, İHA ve Savunma mega fabrikalarına yüksek katma değerli girdi sağlayın.",
            "🤝 Otomatik B2B Tedarik: Vadeli Sözleşme ve Tedarik Müdürü sayesinde şantiyeleriniz hammaddesiz kalmaz, piyasadan anında satın alım yapılır.",
            "👔 Lojistik Operasyonu: Lojistik Müdürünüz depo doluluğu %85'i aştığında konsorsiyum ürünlerine yer açmak için acil stok satışları yapar.",
            "🏦 Merkez Bankası & Altın: Boştaki nakdinizi vadeli mevduatta veya külçe altında değerlendirerek günlük pasif faiz geliri elde edin.",
            "⚡ Hızlı Üretim & Otomasyon: Ar-Ge laboratuvarında teknolojileri geliştirerek üretim sürelerini %50'ye varan oranda kısaltın."
        )
    }

    val tipsEnglish = remember {
        listOf(
            "🏛️ Live Auctions: Bid in Ministry auctions to acquire priceless historical artifacts and boost your holding's national prestige.",
            "📈 Stock Exchange (IPO): Launch an IPO for your holding on BIST to raise massive liquidity and build institutional investor trust.",
            "🏭 Industrial Chain: Smelt Iron & Coal into Steel to feed Automotive, Drone, and Defense mega factories with high added-value goods.",
            "🤝 Automated B2B Supply: The Procurement Director ensures construction sites never run out of raw materials by automatically purchasing missing stock.",
            "👔 Logistics Operations: Your Logistics Manager performs emergency stock sales when warehouse capacity exceeds 85% to prioritize consortium delivery.",
            "🏦 Central Bank Reserves: Deposit spare cash into term savings or gold bullion to earn compound daily passive yields.",
            "⚡ Fast Production & R&D: Upgrade research technologies in the R&D lab to reduce production cycles by up to 50%."
        )
    }

    val patchNotesTurkish = remember {
        listOf(
            "🚀 Konsorsiyum Mega Projeleri: Şehirlerarası dev altyapı projelerine ortak olun ve günlük kar payı temettüsü kazanın.",
            "🏛️ Tarihi Eser Koleksiyonu & Müzayede: 81 ilin tarihi eserlerini toplayıp müzenizde sergileyin veya küresel müzayedede satın.",
            "💼 5 Farklı Holding Müdürü: Hazine, Üretim, Lojistik, İK ve Ar-Ge müdürlerini işe alarak şirketinizi otonom yönetin.",
            "🌐 Çok Oyunculu Canlı Pazar: Diğer oyuncuların satış ilanlarını anlık görün, vadeli sözleşmeler ve alım talepleri yayınlayın."
        )
    }

    val patchNotesEnglish = remember {
        listOf(
            "🚀 Consortium Mega Projects: Partner in massive intercity infrastructure developments and collect daily dividend profits.",
            "🏛️ Heritage Collection & Auctions: Discover rare artifacts across 81 provinces and showcase them in your museum or auction house.",
            "💼 5 Executive C-Suite Managers: Hire Treasury, Production, Logistics, HR, and R&D directors to manage your conglomerate autonomously.",
            "🌐 Real-Time Multiplayer Market: Browse live player listings, negotiate futures contracts, and fulfill B2B supply tenders."
        )
    }

    val activeTips = if (isEnglish) tipsEnglish else tipsTurkish
    val activePatchNotes = if (isEnglish) patchNotesEnglish else patchNotesTurkish

    // Step-by-step background initialization & backup loading logic
    LaunchedEffect(Unit) {
        val durationMs = 2400L
        val startTime = System.currentTimeMillis()

        // Background save loading & cloud integrity check
        scope.launch(Dispatchers.IO) {
            try {
                gameViewModel.repository.initializeGame()
            } catch (_: Throwable) {}
        }

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            progress = fraction
            if (fraction >= 1f) break
            delay(16)
        }

        progress = 1f
        delay(150)
        isDone = true

        val isGoogleUser = gameViewModel.isUserGoogleSignedIn()
        val hasChosenGuest = gameViewModel.isGuestModeChosen()

        if (isGoogleUser || hasChosenGuest) {
            delay(100)
            onFinished()
        } else {
            // Fresh player: show streamlined auth choices
            showAuthChoice = true
        }
    }

    // Auto-cycle tips every 3.0 seconds
    LaunchedEffect(Unit) {
        while (!isDone) {
            delay(3000)
            currentTipIndex = (currentTipIndex + 1) % activeTips.size
        }
    }

    // Ambient cyber animations
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_intro_anim")

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )

    val reverseRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_ring_rot"
    )

    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    // Detailed cyber terminal status log based on progress
    val statusLog = when {
        progress < 0.20f -> if (isEnglish)
            "[01/05] 💾 Verifying local database & asset holdings..."
        else
            "[01/05] 💾 Yerel SQLite veritabanı ve varlıklar doğrulanıyor..."
        progress < 0.45f -> if (isEnglish)
            "[02/05] ☁️ Syncing cloud server time & anti-cheat engine..."
        else
            "[02/05] ☁️ Bulut sunucu zamanı ve güvenlik protokolleri eşitleniyor..."
        progress < 0.70f -> if (isEnglish)
            "[03/05] 📈 Loading BIST stock tickers & live order book..."
        else
            "[03/05] 📈 BİST Pazar derinliği ve canlı fiyat akışları bağlanıyor..."
        progress < 0.90f -> if (isEnglish)
            "[04/05] 🤝 Computing Consortium projects & offline progress..."
        else
            "[04/05] 🤝 Şehir Konsorsiyumu & çevrimdışı ilerleme hesaplanıyor..."
        else -> if (isEnglish)
            "[05/05] ✅ Cloud backups & Conglomerate dashboard ready!"
        else
            "[05/05] ✅ Bulut yedekleri ve Holding yönetim paneli hazırlandı!"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeBackground)
    ) {
        // 1. Background Visual with Cityscape
        Image(
            painter = painterResource(id = R.drawable.bg_city_night),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        // 2. Cyber Gradient & Grid Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ThemeBackground.copy(alpha = 0.92f),
                            Color(0xAA060B14),
                            ThemeBackground.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        // 3. Floating Neon Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val random = Random(1337)

            for (i in 0..32) {
                val seedX = random.nextFloat() * width
                val seedY = random.nextFloat() * height
                val radius = 1.4f + (i % 4) * 1.1f
                val pAlpha = 0.15f + (sin((ringRotation + i * 18) * Math.PI / 180f).toFloat() * 0.25f)
                val color = if (i % 2 == 0) ThemeNeonCyan else ThemeGold

                drawCircle(
                    color = color.copy(alpha = pAlpha.coerceIn(0.08f, 0.75f)),
                    radius = radius,
                    center = Offset(
                        x = (seedX + sin((ringRotation * 0.4f + i * 12) * Math.PI / 180f).toFloat() * 18f) % width,
                        y = (seedY - (ringRotation * 1.1f + i * 18) % height + height) % height
                    )
                )
            }
        }

        // 4. Main Foreground UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Live Market Ticker Marquee Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xCC091122),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE BIST",
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color(0xFFEF4444),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Scrolling items preview
                    val tickerItem = marketTickers[((progress * marketTickers.size).toInt()).coerceIn(0, marketTickers.size - 1)]
                    AnimatedContent(
                        targetState = tickerItem,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(200)) + slideInVertically { it / 2 })
                                .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 2 })
                        },
                        label = "ticker_item_anim"
                    ) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEnglish) item.nameEn else item.nameTr,
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.price,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = ThemeGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.change,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = if (item.isUp) ThemePositive else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // CENTER SECTION: Holographic Conglomerate Emblem & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = logoPulse
                            scaleY = logoPulse
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Rotating Cyber Arc Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.5.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    ThemeNeonCyan.copy(alpha = 0.15f),
                                    ThemeNeonCyan,
                                    ThemeGold,
                                    ThemeNeonCyan.copy(alpha = 0.15f)
                                )
                            ),
                            startAngle = ringRotation,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = ThemeGold.copy(alpha = 0.65f),
                            startAngle = reverseRingRotation,
                            sweepAngle = 130f,
                            useCenter = false,
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Glow Aura
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ThemeNeonCyan.copy(alpha = 0.40f * glowAlpha),
                                        ThemeGold.copy(alpha = 0.20f * glowAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Logo Icon Frame
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0C1322),
                        border = BorderStroke(2.dp, Brush.linearGradient(listOf(ThemeGold, ThemeNeonCyan))),
                        shadowElevation = 18.dp,
                        modifier = Modifier.size(118.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.game_app_icon_1786608157666),
                            contentDescription = "Game Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isEnglish) "ANATOLIAN TRADE SIMULATION" else "ANADOLU TİCARET SİMÜLASYONU",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = if (isEnglish) "MULTIPLAYER CONGLOMERATE & LIVE ECONOMY" else "ÇOK OYUNCULU HOLDİNG & CANLI BORSA",
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = ThemeNeonCyan,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.8.sp
                )
            }

            // MIDDLE SECTION: Interactive Tabs (İpuçları & Güncelleme Yenilikleri)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD0D162A),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.35f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tab Selector Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF080D1A))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab 1: Strategy Tips
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTab = 0
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) ThemeGold.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (selectedTab == 0) BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f)) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) ThemeGold else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) "Strategy Tips" else "Tüccar İpuçları",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (selectedTab == 0) ThemeGold else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Tab 2: Recent Updates & Consortium
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTab = 1
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) ThemeNeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (selectedTab == 1) BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f)) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) ThemeNeonCyan else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) "Patch Updates" else "Son Yenilikler",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (selectedTab == 1) ThemeNeonCyan else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Content Box
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "tab_content_anim"
                    ) { tab ->
                        if (tab == 0) {
                            // Tips list / cycling item
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEnglish) "TİCARİ STRATEJİ REHBERİ" else "STRATEJİK TİCARET REHBERİ",
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold
                                    )
                                    Text(
                                        text = "${currentTipIndex + 1}/${activeTips.size}",
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeTips[currentTipIndex],
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 16.5.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            // Patch notes list
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                activePatchNotes.take(2).forEach { note ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = note,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM SECTION: Progress Bar + Cyber Log Terminal
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Cyber Terminal Log & Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusLog,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF38BDF8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%${(progress * 100).toInt()}",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ThemeGold
                    )
                }

                // Dual-Glow Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, ThemeNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0.01f, 1f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0284C7),
                                        ThemeNeonCyan,
                                        ThemeGold
                                    )
                                )
                            )
                    )
                }

                // Footer Build info & Quick Skip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "v2.6.0 • Cloud Save & Mega Project Sync" else "v2.6.0 • Bulut Senkronizasyon & Mega Projeler",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFD700))
                            .border(0.8.dp, ThemeGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val isGoogleUser = gameViewModel.isUserGoogleSignedIn()
                                val hasChosenGuest = gameViewModel.isGuestModeChosen()
                                if (isGoogleUser || hasChosenGuest) {
                                    onFinished()
                                } else {
                                    progress = 1f
                                    isDone = true
                                    showAuthChoice = true
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Fast Enter ❯" else "Hızlı Giriş ❯",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ThemeGold
                        )
                    }
                }
            }
        }

        // 5. Auth Selection Gate (Only presented for fresh installs who have not chosen yet)
        AnimatedVisibility(
            visible = showAuthChoice,
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(320)),
            exit = fadeOut(animationSpec = tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.activity.compose.BackHandler(enabled = showAuthChoice) {}

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9060B14))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                ThemeGold.copy(alpha = 0.85f),
                                ThemeNeonCyan.copy(alpha = 0.85f)
                            )
                        )
                    ),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThemeNeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isEnglish) "TRADER AUTHENTICATION" else "TÜCCAR KİMLİK DOĞRULAMA",
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeNeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Text(
                            text = if (isEnglish) "ANATOLIAN TRADE SIMULATION" else "ANADOLU TİCARET SİMÜLASYONU",
                            fontFamily = RajdhaniFontFamily,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isEnglish)
                                "Choose how you would like to begin your trading empire:"
                            else
                                "Ticaret imparatorluğunuza nasıl başlamak istersiniz?",
                            fontFamily = PlusJakartaSansFontFamily,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center
                        )

                        // Google Sign-In (Recommended)
                        if (isGoogleLoading) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF131D33),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = ThemeNeonCyan,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = googleStatusMsg.ifEmpty {
                                            if (isEnglish) "Connecting to Google..." else "Google bağlantısı kuruluyor..."
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        color = ThemeNeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF131E35),
                                border = BorderStroke(1.2.dp, ThemeGold.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEnglish) "RECOMMENDED" else "ÖNERİLEN",
                                            fontFamily = RobotoMonoFontFamily,
                                            color = ThemeGold,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = if (isEnglish) "Full Online Access" else "Tam Online Erişim",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            color = ThemeNeonCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            GoogleAuthHelper.launchGoogleSignIn(
                                                context = context,
                                                scope = scope,
                                                viewModel = gameViewModel,
                                                onStart = {
                                                    isGoogleLoading = true
                                                    googleStatusMsg = if (isEnglish) "Starting Google Auth..." else "Google Doğrulaması Başlatılıyor..."
                                                },
                                                onComplete = { success, msg ->
                                                    isGoogleLoading = false
                                                    if (success) {
                                                        gameViewModel.setGuestModePreference(false)
                                                        Toast.makeText(
                                                            context,
                                                            if (isEnglish) "Google Sign-In Successful!" else "Google ile Giriş Yapıldı!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        onFinished()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            msg ?: if (isEnglish) "Sign-in failed" else "Giriş yapılamadı",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4285F4),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isEnglish) "Sign In with Google" else "Google ile Giriş Yap",
                                                fontFamily = RajdhaniFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (isEnglish)
                                            "• Cloud Save • Live Market • Consortium & Leaderboards Active"
                                        else
                                            "• Bulut Kaydı • Canlı Pazar • Konsorsiyum ve Sıralama Aktif",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Guest Mode Option
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0C1322),
                                border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (guestTraderName.isEmpty()) {
                                                guestTraderName = if (isEnglish) "Anatolian Trade Co." else "Ahi Holding"
                                            }
                                            showGuestNameModal = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E293B),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEnglish) "Play as Guest" else "Misafir Olarak Oyna",
                                                fontFamily = RajdhaniFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (isEnglish)
                                            "Offline Single-Player. Online market and consortium will have locked badges."
                                        else
                                            "Çevrimdışı tek kişilik mod. Pazar ve konsorsiyum kilit rozetiyle sunulur.",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Guest Trader Name Input Modal
        AnimatedVisibility(
            visible = showGuestNameModal,
            enter = fadeIn(animationSpec = tween(250)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.activity.compose.BackHandler(enabled = showGuestNameModal) {
                showGuestNameModal = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6050A14))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                ThemeGold.copy(alpha = 0.9f),
                                ThemeNeonCyan.copy(alpha = 0.6f)
                            )
                        )
                    ),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isEnglish) "GUEST TRADER PROFILE" else "MİSAFİR TÜCCAR KAYDI",
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Text(
                            text = if (isEnglish) "ENTER TRADER TITLE" else "TÜCCAR ÜNVANINIZ",
                            fontFamily = RajdhaniFontFamily,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isEnglish)
                                "Specify the merchant title or holding company name for your trading empire:"
                            else
                                "Ticaret odası ve holding sicilinde kullanılacak şirket/tüccar isminizi belirleyin:",
                            fontFamily = PlusJakartaSansFontFamily,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        // Name Text Field
                        OutlinedTextField(
                            value = guestTraderName,
                            onValueChange = { if (it.length <= 28) guestTraderName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = if (isEnglish) "e.g. Anatolian Trade Co." else "Örn: Ahi Holding",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (guestTraderName.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { guestTraderName = "" }
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeGold,
                                unfocusedBorderColor = ThemeNeonCyan.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF080D1A),
                                unfocusedContainerColor = Color(0xFF080D1A),
                                cursorColor = ThemeGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Quick Suggestions Chips
                        val quickNames = if (isEnglish) {
                            listOf("Anatolian Trade Co.", "Silk Road Logistics", "Eurasia Consortium", "Ottoman Trading")
                        } else {
                            listOf("Ahi Holding", "Anadolu Dış Ticaret", "İpek Yolu Lojistik", "Avrasya Konsorsiyum")
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Quick suggestions:" else "Hızlı öneriler:",
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickNames.take(2).forEach { sName ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(0.8.dp, if (guestTraderName == sName) ThemeGold else Color(0xFF334155)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                guestTraderName = sName
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    ) {
                                        Text(
                                            text = sName,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = if (guestTraderName == sName) FontWeight.Bold else FontWeight.Normal,
                                            color = if (guestTraderName == sName) ThemeGold else Color(0xFFCBD5E1),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickNames.drop(2).take(2).forEach { sName ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(0.8.dp, if (guestTraderName == sName) ThemeGold else Color(0xFF334155)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                guestTraderName = sName
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    ) {
                                        Text(
                                            text = sName,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = if (guestTraderName == sName) FontWeight.Bold else FontWeight.Normal,
                                            color = if (guestTraderName == sName) ThemeGold else Color(0xFFCBD5E1),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Action Buttons: Back & Start
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showGuestNameModal = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF94A3B8)
                                )
                            ) {
                                Text(
                                    text = if (isEnglish) "Back" else "Geri",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val finalName = guestTraderName.trim().ifEmpty {
                                        if (isEnglish) "Anatolian Trade Co." else "Ahi Holding"
                                    }
                                    gameViewModel.setGuestModePreference(true)
                                    gameViewModel.updateCompanyName(finalName)
                                    gameViewModel.signInAnonymously { success, _ ->
                                        Toast.makeText(
                                            context,
                                            if (isEnglish) "Welcome, $finalName!" else "Hoş geldiniz, $finalName!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onFinished()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeGold,
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Start Empire" else "İmparatorluğu Başlat",
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
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
