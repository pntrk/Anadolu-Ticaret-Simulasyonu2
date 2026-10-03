package com.example.ui.screens

import android.preference.PreferenceManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CityProfile
import com.example.data.cities
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OnboardingScreen(
    viewModel: GameViewModel,
    onComplete: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isEnglish = selectedLanguage == "en"
    val currentAppLanguage = if (isEnglish) AppLanguage.ENGLISH else AppLanguage.TURKISH

    // State for initial warehouse city
    var selectedCityId by remember { mutableStateOf("istanbul") }
    var viewMode by remember { mutableIntStateOf(0) } // 0: Interactive Map, 1: Strategic List
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("all") } // all, turkey, global, industry, port, speed

    val allCities = remember { cities }
    val selectedCity = remember(selectedCityId) {
        cities.find { it.id == selectedCityId } ?: cities.first()
    }

    // Intercept back button during onboarding
    BackHandler(enabled = true) { /* Must choose an HQ to start */ }

    CompositionLocalProvider(LocalAppLanguage provides currentAppLanguage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF060D1A),
                            Color(0xFF091428),
                            Color(0xFF040812)
                        )
                    )
                )
        ) {
            // Background subtle animated particles
            val infiniteTransition = rememberInfiniteTransition(label = "onboard_ambient")
            val particlePhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ambient_phase"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val rand = Random(42)
                for (i in 0..16) {
                    val baseX = rand.nextFloat() * width
                    val baseY = rand.nextFloat() * height
                    val offsetAngle = (particlePhase + i * 20) * Math.PI / 180f
                    val currentY = (baseY - (particlePhase * 1.2f + i * 18f) % height + height) % height
                    val currentX = (baseX + sin(offsetAngle).toFloat() * 12f) % width
                    val alpha = (0.08f + sin(offsetAngle).toFloat() * 0.12f).coerceIn(0.03f, 0.25f)
                    drawCircle(
                        color = if (i % 2 == 0) ThemeNeonCyan.copy(alpha = alpha) else ThemeGold.copy(alpha = alpha),
                        radius = 1.4f + (i % 3) * 1.0f,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            // Main Structural Column: Top Header + Middle Viewport + Docked Bottom Action Bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 1. TOP HEADER & BADGES
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header Badge & Starter Grants
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = tr("MERKEZ DEPO KAYDI", "HQ DEPOT SETUP"),
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        // Starter Grants Mini Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCC0D1B33),
                                border = BorderStroke(0.8.dp, ThemeGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("💰", fontSize = 10.sp)
                                    Text("100.000 ₳", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeGold, fontFamily = RajdhaniFontFamily)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCC0D1B33),
                                border = BorderStroke(0.8.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("🏢", fontSize = 10.sp)
                                    Text(tr("1. Kademe", "Tier 1"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeNeonCyan, fontFamily = RajdhaniFontFamily)
                                }
                            }
                        }
                    }

                    // Mode Selector Tab (Map vs Directory)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF080D1A))
                            .padding(2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewMode = 0
                                },
                            shape = RoundedCornerShape(7.dp),
                            color = if (viewMode == 0) ThemeNeonCyan.copy(alpha = 0.22f) else Color.Transparent,
                            border = if (viewMode == 0) BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.7f)) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = if (viewMode == 0) ThemeNeonCyan else Color(0xFF64748B),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr("İnteraktif Harita (Türkiye & Dünya)", "Interactive Map (Global)"),
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (viewMode == 0) ThemeNeonCyan else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewMode = 1
                                },
                            shape = RoundedCornerShape(7.dp),
                            color = if (viewMode == 1) ThemeGold.copy(alpha = 0.22f) else Color.Transparent,
                            border = if (viewMode == 1) BorderStroke(1.dp, ThemeGold.copy(alpha = 0.7f)) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = null,
                                    tint = if (viewMode == 1) ThemeGold else Color(0xFF64748B),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr("Şehir Rehberi & Filtre", "City Directory & Filter"),
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (viewMode == 1) ThemeGold else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // 2. MIDDLE VIEWPORT (Map or List - Takes all dynamic height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    if (viewMode == 0) {
                        // INTERACTIVE MAP MODE (Türkiye & World)
                        OnboardingInteractiveMapSection(
                            allCities = allCities,
                            selectedCityId = selectedCityId,
                            onSelectCity = { selectedCityId = it },
                            isEnglish = isEnglish
                        )
                    } else {
                        // STRATEGIC LIST & DIRECTORY MODE (All Cities)
                        OnboardingCityDirectorySection(
                            allCities = allCities,
                            selectedCityId = selectedCityId,
                            onSelectCity = { selectedCityId = it },
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { searchQuery = it },
                            categoryFilter = selectedCategoryFilter,
                            onCategoryFilterChanged = { selectedCategoryFilter = it },
                            isEnglish = isEnglish
                        )
                    }
                }

                // 3. PINNED BOTTOM DOCK (GUARANTEED VISIBLE, 100% RELIABLE)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = Color(0xF8080F1E),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                ThemeGold.copy(alpha = 0.7f),
                                Color(0xFF1E2D4A)
                            )
                        )
                    ),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Compact Selected City Info Banner
                        CompactCitySelectionBanner(
                            city = selectedCity,
                            isEnglish = isEnglish
                        )

                        // Main Big Start Button (Always Clickable & High-Contrast)
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onComplete(selectedCityId)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeGold,
                                contentColor = Color(0xFF080F1E)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = Color(0xFF080F1E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr(
                                        "${selectedCity.name.uppercase()} İLE İMPARATORLUĞU BAŞLAT ❯",
                                        "START EMPIRE AT ${selectedCity.name.uppercase()} ❯"
                                    ),
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF080F1E),
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 🗺️ INTERACTIVE MAP SECTION (TURKEY & GLOBAL CITIES)
// =========================================================================

@Composable
fun OnboardingInteractiveMapSection(
    allCities: List<CityProfile>,
    selectedCityId: String,
    onSelectCity: (String) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Initialize osmdroid configuration
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = context.packageName
    try {
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 25L * 1024L * 1024L
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 15L * 1024L * 1024L
    } catch (_: Throwable) {}

    val regions = remember {
        listOf("Tümü / All", "Türkiye", "Avrupa", "Kuzey Amerika", "Güney Amerika", "Asya & Pasifik", "Orta Doğu & Afrika")
    }
    var currentRegionFilter by remember { mutableStateOf("Tümü / All") }

    val onboardingMapRef = remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onboardingMapRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> onboardingMapRef.value?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    onboardingMapRef.value?.onDetach()
                    onboardingMapRef.value = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onboardingMapRef.value?.onDetach()
            onboardingMapRef.value = null
        }
    }

    // Key Top Facility Cities (Turkey & World)
    val topFacilityCityIds = remember {
        setOf(
            "istanbul", "ankara", "izmir", "bursa", "kocaeli", "mersin", "gaziantep",
            "new_york", "london", "tokyo", "shanghai", "frankfurt", "rotterdam", "houston", "sao_paulo", "johannesburg", "basra"
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Quick Focus / Region Filter Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Quick Focus Buttons on Key Global Hubs
            item {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = ThemeGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.7f)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCity("istanbul")
                        cityCoordinates["istanbul"]?.let { onboardingMapRef.value?.controller?.animateTo(it) }
                    }
                ) {
                    Text(
                        text = "🇹🇷 İstanbul",
                        color = ThemeGold,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = ThemeNeonCyan.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.7f)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCity("new_york")
                        cityCoordinates["new_york"]?.let { onboardingMapRef.value?.controller?.animateTo(it) }
                    }
                ) {
                    Text(
                        text = "🇺🇸 New York",
                        color = ThemeNeonCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCity("tokyo")
                        cityCoordinates["tokyo"]?.let { onboardingMapRef.value?.controller?.animateTo(it) }
                    }
                ) {
                    Text(
                        text = "🇯🇵 Tokyo",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCity("frankfurt")
                        cityCoordinates["frankfurt"]?.let { onboardingMapRef.value?.controller?.animateTo(it) }
                    }
                ) {
                    Text(
                        text = "🇩🇪 Frankfurt",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCity("shanghai")
                        cityCoordinates["shanghai"]?.let { onboardingMapRef.value?.controller?.animateTo(it) }
                    }
                ) {
                    Text(
                        text = "🇨🇳 Şanghay",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // OsmDroid Map Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.2.dp, ThemeNeonCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .background(Color(0xFF0A1220))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(5.2)
                        val startGeo = cityCoordinates[selectedCityId] ?: GeoPoint(39.0, 35.0)
                        controller.setCenter(startGeo)
                        onboardingMapRef.value = this
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                        override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
                            if (shadow) return
                            val proj = mapView.projection
                            val pinPaint = android.graphics.Paint().apply { isAntiAlias = true }
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 24f
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                setShadowLayer(5f, 0f, 2f, android.graphics.Color.BLACK)
                            }
                            val facilityBadgePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(255, 215, 0)
                                textSize = 18f
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                            }

                            // 1. Draw Global & Turkey Cities with Enhanced Markers
                            allCities.forEach { city ->
                                val geo = cityCoordinates[city.id] ?: return@forEach
                                val pt = android.graphics.Point()
                                proj.toPixels(geo, pt)

                                val isSel = city.id == selectedCityId
                                val isTopFacilityHub = city.id in topFacilityCityIds || city.isMajorCity

                                if (isSel) {
                                    // Outer pulsating glowing rings for selected city
                                    pinPaint.color = android.graphics.Color.argb(70, 255, 215, 0)
                                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 42f, pinPaint)
                                    pinPaint.color = android.graphics.Color.argb(180, 0, 229, 255)
                                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 24f, pinPaint)
                                } else if (isTopFacilityHub) {
                                    // Highlight top production facility cities with glowing aura
                                    pinPaint.color = if (city.isGlobal) android.graphics.Color.argb(60, 0, 229, 255) else android.graphics.Color.argb(60, 255, 215, 0)
                                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 18f, pinPaint)
                                }

                                // Pin Base Color
                                pinPaint.color = when {
                                    isSel -> android.graphics.Color.rgb(255, 215, 0)
                                    city.isGlobal -> android.graphics.Color.rgb(56, 189, 248) // Sky Blue for Global
                                    isTopFacilityHub -> android.graphics.Color.rgb(255, 183, 77) // Gold-Orange for Turkey Hubs
                                    else -> android.graphics.Color.rgb(148, 163, 184)
                                }

                                val pinRadius = when {
                                    isSel -> 13f
                                    isTopFacilityHub -> 9.5f
                                    else -> 6.5f
                                }
                                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), pinRadius, pinPaint)

                                // Pin Center Core Dot
                                pinPaint.color = when {
                                    isSel -> android.graphics.Color.BLACK
                                    city.isGlobal -> android.graphics.Color.rgb(0, 20, 40)
                                    else -> android.graphics.Color.WHITE
                                }
                                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), if (isSel) 4f else 2.5f, pinPaint)

                                // Facility Marker Badges for Key Industrial Hubs
                                if (isTopFacilityHub && !isSel) {
                                    val iconSymbol = when {
                                        city.isGlobal -> "🌐"
                                        city.logistics.any { it.contains("Liman", true) } -> "⚓"
                                        city.productionSpeedMultiplier >= 1.2f -> "⚡"
                                        else -> "🏭"
                                    }
                                    canvas.drawText(iconSymbol, pt.x.toFloat() - 7f, pt.y.toFloat() - 12f, facilityBadgePaint)
                                }

                                // City Labels: Draw for Selected City or Major Industrial Hubs
                                if (isSel || isTopFacilityHub) {
                                    val cityName = city.name
                                    textPaint.textSize = if (isSel) 26f else if (city.isMajorCity) 20f else 17f
                                    textPaint.color = if (isSel) android.graphics.Color.rgb(255, 215, 0) else android.graphics.Color.WHITE
                                    canvas.drawText(
                                        cityName,
                                        pt.x.toFloat() - (cityName.length * (textPaint.textSize * 0.26f)),
                                        pt.y.toFloat() + (if (isSel) 30f else 22f),
                                        textPaint
                                    )
                                }
                            }
                        }

                        override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                            val proj = mapView.projection
                            val touchPt = android.graphics.Point(e.x.toInt(), e.y.toInt())
                            var tappedCity: CityProfile? = null
                            var minDistance = Double.MAX_VALUE

                            allCities.forEach { city ->
                                val geo = cityCoordinates[city.id] ?: return@forEach
                                val pt = android.graphics.Point()
                                proj.toPixels(geo, pt)
                                val dist = kotlin.math.hypot((touchPt.x - pt.x).toDouble(), (touchPt.y - pt.y).toDouble())
                                if (dist < 75.0 && dist < minDistance) {
                                    minDistance = dist
                                    tappedCity = city
                                }
                            }

                            if (tappedCity != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectCity(tappedCity!!.id)
                                val geo = cityCoordinates[tappedCity!!.id]
                                if (geo != null) {
                                    mapView.controller.animateTo(geo)
                                }
                                return true
                            }
                            return false
                        }
                    })

                    val targetGeo = cityCoordinates[selectedCityId]
                    if (targetGeo != null) {
                        mapView.controller.animateTo(targetGeo)
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Map Legend Banner
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                shape = RoundedCornerShape(7.dp),
                color = Color(0xEE0B192C),
                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🌐", fontSize = 9.sp)
                    Text(
                        text = tr("Türkiye & Küresel Şehirler Aktif", "Turkey & Global Cities Active"),
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Map Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onboardingMapRef.value?.controller?.zoomIn()
                        },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xDD0F172A),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onboardingMapRef.value?.controller?.zoomOut()
                        },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xDD0F172A),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// 🏢 STRATEGIC CITY DIRECTORY & GRID SECTION (TURKEY & WORLD)
// =========================================================================

@Composable
fun OnboardingCityDirectorySection(
    allCities: List<CityProfile>,
    selectedCityId: String,
    onSelectCity: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    categoryFilter: String,
    onCategoryFilterChanged: (String) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Filter cities based on search and category
    val filteredCities = remember(allCities, searchQuery, categoryFilter) {
        allCities.filter { city ->
            val matchesQuery = searchQuery.isBlank() ||
                    city.name.contains(searchQuery, ignoreCase = true) ||
                    city.region.contains(searchQuery, ignoreCase = true) ||
                    city.country.contains(searchQuery, ignoreCase = true) ||
                    city.primaryProducts.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesCategory = when (categoryFilter) {
                "turkey" -> !city.isGlobal
                "global" -> city.isGlobal
                "featured" -> city.isMajorCity
                "port" -> city.logistics.any { it.contains("Liman", ignoreCase = true) || it.contains("Port", ignoreCase = true) }
                "industry" -> city.economicMultiplier >= 1.2f || city.productionSpeedMultiplier >= 1.15f
                "speed" -> city.productionSpeedMultiplier >= 1.2f
                else -> true
            }

            matchesQuery && matchesCategory
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            placeholder = {
                Text(
                    text = tr("Şehir, ülke veya sektör ara...", "Search city, country or sector..."),
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(15.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onSearchQueryChanged("") }
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeGold,
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF080D1A),
                unfocusedContainerColor = Color(0xFF080D1A),
                cursorColor = ThemeGold
            ),
            shape = RoundedCornerShape(9.dp),
            singleLine = true
        )

        // Category Filter Chips
        val filterOptions = listOf(
            "all" to tr("Tümü", "All"),
            "turkey" to tr("🇹🇷 Türkiye", "🇹🇷 Turkey"),
            "global" to tr("🌍 Dünya", "🌍 Global"),
            "featured" to tr("⭐ Önerilenler", "⭐ Featured"),
            "industry" to tr("🏭 Ağır Sanayi", "🏭 Industry Hub"),
            "port" to tr("⚓ Limanlar", "⚓ Port Hubs")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions, key = { it.first }) { (key, label) ->
                val isSelected = categoryFilter == key
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSelected) ThemeGold.copy(alpha = 0.25f) else Color(0xFF0F172A),
                    border = BorderStroke(1.dp, if (isSelected) ThemeGold else Color(0xFF1E293B)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCategoryFilterChanged(key)
                    }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) ThemeGold else Color(0xFFCBD5E1),
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // City List Cards
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredCities, key = { it.id }) { city ->
                val isSel = city.id == selectedCityId
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSel) Color(0xFF162544) else Color(0xFF0D172A),
                    border = BorderStroke(1.2.dp, if (isSel) ThemeGold else Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectCity(city.id)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${city.countryFlag} ${city.name.trAuto()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSel) ThemeGold else Color.White
                                )
                                Text(
                                    text = "• ${city.region.trAuto()}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                if (city.isMajorCity) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ThemeGold.copy(alpha = 0.2f),
                                        border = BorderStroke(0.8.dp, ThemeGold)
                                    ) {
                                        Text(
                                            text = if (city.isGlobal) "🌍 GLOBAL" else "⭐ TOP",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = city.primaryProducts.take(3).map { it.trAuto() }.joinToString(", "),
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.1fx".format(city.economicMultiplier) + " " + tr("Canlılık", "Vitality"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive,
                                fontFamily = RobotoMonoFontFamily
                            )
                            Text(
                                text = "%.1fx".format(city.productionSpeedMultiplier) + " " + tr("Hız", "Speed"),
                                fontSize = 9.sp,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 🏢 COMPACT CITY SELECTION BANNER (INSIDE PINNED BOTTOM DOCK)
// =========================================================================

@Composable
fun CompactCitySelectionBanner(
    city: CityProfile,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xEE0D192E),
        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Header Row: Flag + City Name + Multipliers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(city.countryFlag, fontSize = 12.sp)
                    Text(
                        text = "${city.name.trAuto()} (${city.country})",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = RajdhaniFontFamily,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "%.2fx".format(city.economicMultiplier) + " " + tr("Pazar", "Market"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemePositive
                    )
                    Text(
                        text = "%.2fx".format(city.productionSpeedMultiplier) + " " + tr("Hız", "Speed"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeNeonCyan
                    )
                }
            }

            // Description / Strategy Advice snippet
            Text(
                text = city.description.trAuto(),
                fontSize = 9.5.sp,
                fontFamily = PlusJakartaSansFontFamily,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
