package com.example.ui.screens

import com.example.viewmodel.*

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

import android.app.Activity
import android.preference.PreferenceManager
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CityProfile
import com.example.data.cities
import com.example.ui.components.AppButton
import com.example.ui.components.LanguagePickerSection
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    viewModel: GameViewModel,
    onComplete: (String) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    val currentAppLanguage = if (selectedLanguage == "en") AppLanguage.ENGLISH else AppLanguage.TURKISH

    // State for inputs
    var companyName by remember { mutableStateOf("") }
    var selectedCityId by remember { mutableStateOf("istanbul") }

    val selectedCity = remember(selectedCityId) {
        cities.find { it.id == selectedCityId } ?: cities.first()
    }

    CompositionLocalProvider(LocalAppLanguage provides currentAppLanguage) {
        Dialog(
            onDismissRequest = { /* Force interaction */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF208101E)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step Progress Indicator Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val isDone = index < currentStep
                            val isCurrent = index == currentStep
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> ThemeGold
                                            isDone -> ThemeNeonCyan
                                            else -> Color.DarkGray
                                        }
                                    )
                            )
                            if (index < 2) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(2.dp)
                                        .background(if (index < currentStep) ThemeNeonCyan else Color.DarkGray)
                                )
                            }
                        }
                    }

                    Crossfade(targetState = currentStep, label = "onboarding_steps") { step ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (step) {
                                0 -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Language,
                                            contentDescription = null,
                                            tint = ThemeGold,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = tr("DİL SEÇİMİ", "LANGUAGE SELECTION"),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeGold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = tr("Lütfen oyunu kullanmak istediğiniz dili seçin:", "Please select your preferred game language:"),
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        LanguagePickerSection(
                                            selectedLanguageCode = selectedLanguage,
                                            onSelectLanguage = { code ->
                                                viewModel.setSelectedLanguage(code)
                                            }
                                        )
                                    }
                                }
                                1 -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = ThemeGold,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = tr("TÜCCAR İSMİ", "MERCHANT NAME"),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeGold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = tr("Anadolu ticaret pazarında tanınacağınız unvanınızı belirleyin:", "Choose your trade title to be recognized in the market:"),
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(
                                            value = companyName,
                                            onValueChange = { if (it.length <= 25) companyName = it },
                                            label = { Text(tr("Ticaret unvanınızı girin (Örn: Ahi Holding)", "Enter trade name (e.g. Ahi Holding)"), color = Color.Gray) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ThemeNeonCyan,
                                                unfocusedBorderColor = ThemeBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                2 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Map,
                                                contentDescription = null,
                                                tint = ThemeGold,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = tr("MERKEZ DEPO LOKASYONU", "CENTRAL WAREHOUSE LOCATION"),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = ThemeGold
                                            )
                                        }

                                        Text(
                                            text = tr("Lojistik üssünüzü haritadan doğrudan dokunarak seçin:", "Select your logistics hub directly on the map:"),
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0x22FFD700),
                                            border = BorderStroke(1.dp, Color(0x55FFD700))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = tr("💰 Başlangıç Hibesi: 100.000 ₳ Hesabınıza Tanımlandı", "💰 Starting Capital: 100,000 ₳ Credited to Your Account"),
                                                    color = ThemeGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Interactive Map Picker
                                        OnboardingCityMapPicker(
                                            selectedCityId = selectedCityId,
                                            onSelectCity = { selectedCityId = it }
                                        )

                                        // Helper City Analysis Card
                                        CityHelperAnalysisCard(city = selectedCity)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            TextButton(onClick = { currentStep-- }) {
                                Text(tr("GERİ", "BACK"), color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (currentStep < 2) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (currentStep == 1) {
                                        if (companyName.isNotBlank()) {
                                            viewModel.updateCompanyName(companyName)
                                            currentStep++
                                        }
                                    } else {
                                        currentStep++
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black)
                            ) {
                                Text(tr("İLERİ", "NEXT"), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onComplete(selectedCityId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(tr("BAŞLA & REHBERE GEÇ", "START & OPEN GUIDE"), fontWeight = FontWeight.Black)
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
fun OnboardingCityMapPicker(
    selectedCityId: String,
    onSelectCity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Initialize osmdroid Configuration before creating the map view
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = context.packageName
    try {
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 10L * 1024L * 1024L
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 8L * 1024L * 1024L
    } catch (_: Throwable) {}

    var selectedRegion by remember { mutableStateOf("Marmara") }
    val regions = remember { cities.map { it.region }.distinct() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Region Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(regions, key = { it }) { reg ->
                val isRegSelected = reg == selectedRegion
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRegSelected) ThemeNeonCyan else Color(0xFF16233B),
                    border = BorderStroke(1.dp, if (isRegSelected) ThemeGold else ThemeBorder),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRegion = reg
                        val firstInReg = cities.firstOrNull { it.region == reg }
                        if (firstInReg != null) {
                            onSelectCity(firstInReg.id)
                        }
                    }
                ) {
                    Text(
                        text = reg.trAuto(),
                        color = if (isRegSelected) Color(0xFF0B192C) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isRegSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Osmdroid Map View Box
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, ThemeGold.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .background(Color(0xFF0B1422))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(6.2)
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
                            }

                            cities.forEach { city ->
                                val geo = cityCoordinates[city.id] ?: return@forEach
                                val pt = android.graphics.Point()
                                proj.toPixels(geo, pt)

                                val isSel = city.id == selectedCityId

                                if (isSel) {
                                    pinPaint.color = android.graphics.Color.argb(90, 255, 215, 0)
                                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 36f, pinPaint)
                                    pinPaint.color = android.graphics.Color.argb(180, 0, 229, 255)
                                    canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 22f, pinPaint)
                                }

                                pinPaint.color = if (isSel) android.graphics.Color.YELLOW else if (city.region == selectedRegion) android.graphics.Color.CYAN else android.graphics.Color.LTGRAY
                                canvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), 11f, pinPaint)

                                if (isSel) {
                                    canvas.drawText(city.name, pt.x.toFloat() - (city.name.length * 5f), pt.y.toFloat() - 24f, textPaint)
                                }
                            }
                        }

                        override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                            val proj = mapView.projection
                            val touchPt = android.graphics.Point(e.x.toInt(), e.y.toInt())
                            var tappedCity: CityProfile? = null
                            var minDistance = Double.MAX_VALUE

                            cities.forEach { city ->
                                val geo = cityCoordinates[city.id] ?: return@forEach
                                val pt = android.graphics.Point()
                                proj.toPixels(geo, pt)
                                val dist = kotlin.math.hypot((touchPt.x - pt.x).toDouble(), (touchPt.y - pt.y).toDouble())
                                if (dist < 60.0 && dist < minDistance) {
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

            // Map instruction chip
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xD90B192C),
                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(12.dp))
                    Text(
                        text = tr("Haritadan şehre dokunun", "Tap a city on the map"),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Region Cities Chips
        val regionCities = remember(selectedRegion) { cities.filter { it.region == selectedRegion } }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(regionCities, key = { it.id }) { city ->
                val isSel = city.id == selectedCityId
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSel) ThemeGold.copy(alpha = 0.25f) else Color(0xFF16233B),
                    border = BorderStroke(1.dp, if (isSel) ThemeGold else ThemeBorder),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCity(city.id)
                    }
                ) {
                    Text(
                        text = city.name.trAuto(),
                        color = if (isSel) ThemeGold else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CityHelperAnalysisCard(
    city: CityProfile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF111C2E),
        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${city.name.trAuto()} (${city.region.trAuto()})",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                if (city.isMajorCity || city.id in listOf("istanbul", "ankara", "izmir", "kocaeli", "bursa", "eskisehir", "mersin")) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThemeGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ThemeGold)
                    ) {
                        Text(
                            text = tr("⭐ ÖNERİLEN ÜS", "⭐ RECOMMENDED BASE"),
                            color = ThemeGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Key Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(tr("Ekonomik Canlılık", "Economic Vitality"), fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "%.1fx".format(city.economicMultiplier),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemePositive
                    )
                }
                Column {
                    Text(tr("Üretim Hızı", "Production Speed"), fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "%.1fx".format(city.productionSpeedMultiplier),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeNeonCyan
                    )
                }
                Column {
                    Text(tr("Lojistik", "Logistics"), fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = city.logistics.take(2).map { it.trAuto() }.joinToString(", "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Primary Goods Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(tr("Sektörler:", "Sectors:"), fontSize = 9.sp, color = Color.Gray)
                city.primaryProducts.take(3).forEach { prod ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2D4A)
                    ) {
                        Text(
                            text = prod.trAuto(),
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Description
            Text(
                text = city.description.trAuto(),
                fontSize = 10.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 13.sp
            )
        }
    }
}
