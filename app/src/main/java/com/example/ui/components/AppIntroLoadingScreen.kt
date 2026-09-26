package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.ThemeSurfaceGlass
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class LoadingParticle(
    var x: Float,
    var y: Float,
    var radius: Float,
    var alpha: Float,
    var speedY: Float,
    var color: Color
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
    var isDone by remember { mutableStateOf(false) }

    val tipsTurkish = remember {
        listOf(
            "🏛️ Müzayedeler ve Tarihi Miras: Bakanlık müzayedelerine katılıp nadir eserleri toplayın veya kendi koleksiyonunuzu 1 saatlik canlı açık artırmalarda global pazara sunun.",
            "📈 Akıllı Finans Yönetimi: Borsa Analistiniz, Hazine tavan limitini aşan boşta duran nakdinizi dinamik olarak diğer şirketlerin halka arz hisselerinde değerlendirir.",
            "🤝 Otomatik B2B Tedarik: Vadeli Sözleşme ve Tedarik Müdürü sayesinde şantiyeleriniz hammaddesiz kalmaz, eksik stoklar piyasadan anında satın alınıp sevk edilir.",
            "🏭 Sanayi Zinciri: Demir ve Kömürden Çelik üreterek Otomotiv, İHA ve Savunma Fabrikalarına yüksek katma değerli girdi sağlayın.",
            "📈 Borsa İstanbul & Halka Arz: Holdinginizin hisselerini BİST'te halka arz ederek devasa sermaye kaynağı yaratın ve yatırımcı çekin.",
            "👔 Lojistik Operasyonu: Lojistik Müdürünüz depo doluluğu %85'i aştığında konsorsiyum ürünlerine yer açmak için acil stok satışları yapar.",
            "🏛️ Merkez Bankası: Boştaki nakdinizi vadeli mevduatta veya külçe altında değerlendirerek günlük pasif bileşik getiri sağlayın."
        )
    }

    val tipsEnglish = remember {
        listOf(
            "🏛️ Auctions & Heritage: Participate in Ministry auctions for rare artifacts or list your own collection in 1-hour live global auctions.",
            "📈 Smart Finance Management: Your Stock Analyst dynamically invests excess cash beyond the Treasury ceiling into IPO shares of other companies.",
            "🤝 Automated B2B Supply: The Procurement Director ensures your construction sites never run out of raw materials by automatically purchasing and shipping missing stock.",
            "🏭 Industrial Chain: Smelt Iron & Coal into Steel to feed Automotive, Drone, and Defense mega factories with high added-value goods.",
            "📈 Stock Exchange (IPO): Launch an IPO for your holding on BIST to raise massive liquidity and build institutional investor trust.",
            "👔 Logistics Operations: Your Logistics Manager performs emergency stock sales when warehouse capacity exceeds 85% to make room for consortium products.",
            "🏛️ Central Bank Reserves: Deposit spare cash into term savings or gold bullion to earn compound daily passive yields."
        )
    }

    val activeTips = if (isEnglish) tipsEnglish else tipsTurkish

    // Animate progress smoothly over 1800ms (1.8 seconds)
    LaunchedEffect(Unit) {
        val durationMs = 1800L
        val startTime = System.currentTimeMillis()
        
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            progress = fraction
            if (fraction >= 1f) {
                break
            }
            delay(16)
        }

        progress = 1f
        delay(100)
        isDone = true
        delay(100)
        onFinished()
    }

    // Cycle gameplay tips every 2.0 seconds (shows 5 rich tips during the 10s intro)
    LaunchedEffect(Unit) {
        while (!isDone) {
            delay(2000)
            currentTipIndex = (currentTipIndex + 1) % activeTips.size
        }
    }

    // Infinite ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "intro_anim")
    
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    val reverseRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_ring_rotation"
    )

    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Milestones text based on progress (7 sequential steps over 10 seconds)
    val statusText = when {
        progress < 0.15f -> if (isEnglish) "🗺️ Loading map..." else "🗺️ Harita yükleniyor..."
        progress < 0.30f -> if (isEnglish) "🏭 Production is starting..." else "🏭 Üretim başlıyor..."
        progress < 0.45f -> if (isEnglish) "🛍️ Market is setting up..." else "🛍️ Pazar kuruluyor..."
        progress < 0.60f -> if (isEnglish) "📈 Stock exchange is opening..." else "📈 Borsa açılıyor..."
        progress < 0.75f -> if (isEnglish) "🏦 Banks are preparing counters..." else "🏦 Bankalar gişelerini hazırlıyor..."
        progress < 0.90f -> if (isEnglish) "👔 Managers are taking office..." else "👔 Müdürler göreve başlıyor..."
        else -> if (isEnglish) "📋 Daily reports are arriving at your desk..." else "📋 Günün raporları masanıza geliyor..."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeBackground)
    ) {
        // 1. Background City Visual
        Image(
            painter = painterResource(id = R.drawable.bg_city_night),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.38f
        )

        // 2. Cyber Gradient & Grid Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ThemeBackground.copy(alpha = 0.85f),
                            Color(0x9907090E),
                            ThemeBackground.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // 3. Floating Neon Dust Particles Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val random = Random(42)
            
            for (i in 0..35) {
                val seedX = random.nextFloat() * width
                val seedY = random.nextFloat() * height
                val radius = 1.5f + (i % 4) * 1.2f
                val pAlpha = 0.2f + (sin((ringRotation + i * 15) * Math.PI / 180f).toFloat() * 0.25f)
                val color = if (i % 2 == 0) ThemeNeonCyan else ThemeGold
                
                drawCircle(
                    color = color.copy(alpha = pAlpha.coerceIn(0.1f, 0.7f)),
                    radius = radius,
                    center = Offset(
                        x = (seedX + sin((ringRotation * 0.5f + i * 10) * Math.PI / 180f).toFloat() * 20f) % width,
                        y = (seedY - (ringRotation * 1.2f + i * 20) % height + height) % height
                    )
                )
            }
        }

        // 4. Main Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3300E5FF))
                    .border(1.dp, ThemeNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = ThemeNeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "ONLINE CLOUD SYNC & SIMULATION" else "BULUT SENKRONİZASYONU & SİMÜLASYON",
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeNeonCyan,
                    letterSpacing = 1.sp
                )
            }

            // Center Visual: Glowing Emblem & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Cyber HUD Emblem
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            scaleX = logoPulse
                            scaleY = logoPulse
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Rotating Cyber Arc Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.5.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        
                        // Cyber segments
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(ThemeNeonCyan.copy(alpha = 0.1f), ThemeNeonCyan, ThemeGold, ThemeNeonCyan.copy(alpha = 0.1f))
                            ),
                            startAngle = ringRotation,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Inner Reverse Dots Ring
                        drawArc(
                            color = ThemeGold.copy(alpha = 0.6f),
                            startAngle = reverseRingRotation,
                            sweepAngle = 140f,
                            useCenter = false,
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Outer Glow Circle
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ThemeNeonCyan.copy(alpha = 0.35f * glowAlpha),
                                        ThemeGold.copy(alpha = 0.15f * glowAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Emblem Image Container
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Brush.linearGradient(listOf(ThemeGold, ThemeNeonCyan))),
                        shadowElevation = 16.dp,
                        modifier = Modifier.size(136.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.game_app_icon_1786608157666),
                            contentDescription = "Game Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = if (isEnglish) "ANATOLIA TRADE SIMULATION" else "ANADOLU TİCARET SİMÜLASYONU",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ThemeGold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = if (isEnglish) "MULTIPLAYER • GLOBAL TRADE • LOGISTICS STRATEGY" else "ÇOK OYUNCULU • KÜRESEL TİCARET • LOJİSTİK STRATEJİ",
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = ThemeNeonCyan,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }

            // Bottom Section: Tips Card + Progress Bar + Skip Option
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cycling Strategy Tip Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xCC0D1424),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4000E5FF)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "STRATEGIC TRADE TIP" else "TİCARİ STRATEJİ & EKONOMİ İPUCU",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ThemeGold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${currentTipIndex + 1}/${activeTips.size}",
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Animated Tip text transition
                        AnimatedContent(
                            targetState = activeTips[currentTipIndex],
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 2 })
                                    .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it / 2 })
                            },
                            label = "tip_transition"
                        ) { tipText ->
                            Text(
                                text = tipText,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Status text & Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%${(progress * 100).toInt()}",
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ThemeNeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High-Tech Neon Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(5.dp))
                ) {
                    // Fill bar
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

                Spacer(modifier = Modifier.height(14.dp))

                // Footer version & Instant Enter if ready
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "v1.20 • 2.5D Industrial Zone & Smart Managers" else "v1.20 • 2.5D Sanayi Bölgesi & Akıllı Yöneticiler",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFD700))
                            .border(1.dp, ThemeGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onFinished()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Enter ❯" else "Hemen Başla ❯",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ThemeGold
                        )
                    }
                }
            }
        }
    }
}
