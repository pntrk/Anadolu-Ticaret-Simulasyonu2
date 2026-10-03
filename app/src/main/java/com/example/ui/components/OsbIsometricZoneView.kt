package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SolarPower
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.ActiveProduction
import com.example.data.BusinessEntity
import com.example.data.CityProfile
import com.example.data.CompanyManager
import com.example.data.InventoryEntity
import com.example.data.Product
import com.example.data.ProductTier
import com.example.data.cities
import com.example.data.facilityDrawableRes
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.viewmodel.GameIntent
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ultra-Realistic 2.5D Isometric Organized Industrial Zone (OSB / Sanayi Bölgesi) Component
 * Features realistic 2.5D architectural factory rendering, structural roofs, silos, solar arrays,
 * security guard checkpoint, active delivery freight animation, realistic 3D isometric trees,
 * multi-puff atmospheric steam smoke, concrete foundation surveyor lots, and deep facility control.
 */
@Composable
fun OsbIsometricZoneView(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onIntent: (GameIntent) -> Unit,
    selectedCityFilter: String?,
    onCityFilterChanged: (String?) -> Unit,
    selectedTierFilter: ProductTier?,
    onTierFilterChanged: (ProductTier?) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit = {},
    onOpenBuildNewFacility: (String?) -> Unit,
    onOpenProduceDialog: (Product) -> Unit,
    onOpenSellDialog: (BusinessEntity) -> Unit,
    onNavigateToRd: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEnglish = isEnglishLanguage()
    val player = uiState.playerState.player
    val businesses = uiState.businesses
    val productionProgress = uiState.productionProgress
    val productionDurations = uiState.productionDurations
    val activeProductions = uiState.inventoryState.activeProductions
    val managers = uiState.managers

    var selectedFacilityDetail by remember { mutableStateOf<Pair<BusinessEntity, Product>?>(null) }

    val hasActiveConstructionOrUpgrade = remember(businesses) {
        businesses.any { it.isConstructing || it.isUpgrading }
    }
    LaunchedEffect(hasActiveConstructionOrUpgrade) {
        if (hasActiveConstructionOrUpgrade) {
            while (isActive) {
                viewModel.checkAndProcessFacilityConstructions()
                viewModel.checkAndProcessFacilityUpgrades()
                delay(1000L)
            }
        }
    }

    // Filter businesses by active city and search query
    val activeCityId = selectedCityFilter ?: player?.currentCity ?: "istanbul"
    val cityProfile = remember(activeCityId) { cities.find { it.id == activeCityId } }

    val filteredBusinesses = remember(businesses, selectedCityFilter, selectedTierFilter, searchQuery) {
        businesses.filter { biz ->
            val matchesCity = selectedCityFilter == null || biz.cityId == selectedCityFilter
            val prod = Product.values().find { it.facilityId == biz.type || it.id == biz.type }
            val cityName = cities.find { it.id == biz.cityId }?.getDisplayName(isEnglish) ?: biz.cityId
            val matchesTier = selectedTierFilter == null || (prod != null && prod.tier == selectedTierFilter)
            val queryClean = searchQuery.trim()
            val matchesSearch = queryClean.isEmpty() || (
                (prod != null && (
                    prod.getFacilityName(isEnglish).contains(queryClean, ignoreCase = true) ||
                    prod.getDisplayName(isEnglish).contains(queryClean, ignoreCase = true) ||
                    prod.id.contains(queryClean, ignoreCase = true)
                )) ||
                cityName.contains(queryClean, ignoreCase = true)
            )
            matchesCity && matchesTier && matchesSearch
        }
    }

    // Sahip olunan tüm tesisleri listele + en son slotta yeni tesis kurulumu için 1 adet boş slot
    val displayedSlots = remember(filteredBusinesses) {
        val list = mutableListOf<BusinessEntity?>()
        list.addAll(filteredBusinesses)
        list.add(null) // En son slot: Yeni Tesis Kurulumu için Boş Parsel
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. City Quick Selector Chips
        OsbCityFilterRow(
            cities = cities,
            activeCityFilter = selectedCityFilter,
            businesses = businesses,
            onSelectCity = { onCityFilterChanged(it) },
            isEnglish = isEnglish
        )

        // 2. Search Bar & Tier Filter Row (Instant filter by facility name, product name, or city)
        OsbSearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            selectedTierFilter = selectedTierFilter,
            onTierFilterChanged = onTierFilterChanged,
            isEnglish = isEnglish
        )

        // 3. One-Tap Mass Harvest & Produce Bar (⚡ Holding Vardiya Komutası)
        OsbMassOperationBanner(
            businesses = businesses,
            activeProductions = activeProductions,
            productionProgress = productionProgress,
            onMassHarvestAndProduceAll = {
                viewModel.massHarvestAndProduceAll()
            },
            isEnglish = isEnglish
        )

        // 4. Live Active & Automated Productions Overview (Shows ALL active productions with NO 5-facility cap)
        OsbActiveAndAutomatedProductionsPreview(
            businesses = businesses,
            productionProgress = productionProgress,
            productionDurations = productionDurations,
            activeProductions = activeProductions,
            managers = managers,
            isEnglish = isEnglish,
            onFacilityClick = { biz, prod ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selectedFacilityDetail = Pair(biz, prod)
            },
            onSpeedUpClick = { prod ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.skipProductionWithGems(prod.id)
            }
        )

        // 4. Ultra-Realistic 2.5D OSB Ground Platform Canvas & Parcel Grid
        OsbGround25DPlatform(
            slots = displayedSlots,
            productionProgress = productionProgress,
            activeProductions = activeProductions,
            managers = managers,
            cityName = if (selectedCityFilter == null) tr("Sanayi Tesisleri", "Industrial Facilities", isEnglish) else (cityProfile?.getDisplayName(isEnglish) ?: "Marmara"),
            isEnglish = isEnglish,
            onSlotClick = { biz, prod ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (biz != null && prod != null) {
                    selectedFacilityDetail = Pair(biz, prod)
                } else {
                    onOpenBuildNewFacility(selectedCityFilter)
                }
            },
            onSpeedUpClick = { prod ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.skipProductionWithGems(prod.id)
            }
        )
    }

    // Detailed Interactive Facility Management Modal Dialog
    if (selectedFacilityDetail != null) {
        val (biz, prod) = selectedFacilityDetail!!
        val latestBiz = uiState.businesses.find { it.id == biz.id } ?: biz
        OsbFacilityDetailDialog(
            business = latestBiz,
            product = prod,
            uiState = uiState,
            viewModel = viewModel,
            isEnglish = isEnglish,
            onDismiss = { selectedFacilityDetail = null },
            onProduceClick = {
                selectedFacilityDetail = null
                onOpenProduceDialog(prod)
            },
            onUpgradeClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.handleIntent(GameIntent.UpgradeBusiness(latestBiz))
            },
            onMaintainClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.handleIntent(GameIntent.MaintainBusiness(latestBiz))
            },
            onSellClick = {
                selectedFacilityDetail = null
                onOpenSellDialog(latestBiz)
            },
            onNavigateToRd = {
                selectedFacilityDetail = null
                onNavigateToRd(it)
            }
        )
    }
}

/**
 * 2.5D OSB Ground Master Platform
 * Renders the industrial park estate with asphalt roads, perimeter green buffers,
 * guard checkpoint security gate, animated freight truck delivery, realistic 3D trees,
 * and the 3x4 parcel grid.
 */
@Composable
fun OsbGround25DPlatform(
    slots: List<BusinessEntity?>,
    productionProgress: Map<String, Float>,
    activeProductions: List<ActiveProduction> = emptyList(),
    managers: List<CompanyManager> = emptyList(),
    cityName: String,
    isEnglish: Boolean,
    onSlotClick: (BusinessEntity?, Product?) -> Unit,
    onSpeedUpClick: ((Product) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val prodManager = managers.find { it.id == "mgr_prod" }
    val isHrAutoActive = prodManager?.let { it.isHired && it.isActive } == true

    val infiniteTransition = rememberInfiniteTransition(label = "osb_master_anim")
    val truckTravel = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "truck_travel"
    )
    val securityBeaconBlink = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "security_beacon"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = ThemeNeonCyan.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C1017), // Deep asphalt ground
        border = BorderStroke(1.5.dp, Brush.verticalGradient(listOf(ThemeNeonCyan.copy(alpha = 0.6f), Color(0xFF1E293B))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // OSB Master Entrance Banner & Security Gate HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF131C28), Color(0xFF1A273A), Color(0xFF131C28))
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .border(0.5.dp, Color(0xFF2A3C52), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Flashing Security Nizamiye Beacon
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer { alpha = securityBeaconBlink.value }
                    ) {}
                    Column {
                        CurrencyText(
                            text = if (cityName.contains("Sanayi", ignoreCase = true) || cityName.contains("Industrial", ignoreCase = true)) {
                                cityName.uppercase()
                            } else {
                                "${cityName.uppercase()} " + tr("ÜRETİM BÖLGESİ", "PRODUCTION ZONE", isEnglish)
                            },
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tr("Ana Giriş Nizamiyesi • 7/24 Güvenlik & Lojistik", "Main Security Gate • 24/7 Freight Hub", isEnglish),
                                fontSize = 8.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0F1724),
                    border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "${slots.size} " + tr("Parsel", "Plots", isEnglish),
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeNeonCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Perimeter Industrial Boulevard with Animated Logistics Truck
            OsbPerimeterAvenueWithTruck(truckProgress = { truckTravel.value }, isEnglish = isEnglish)

            Spacer(modifier = Modifier.height(6.dp))

            // Parcels Grid Layout (3 Columns per row, all owned facilities + 1 empty installation slot)
            val chunkedSlots = remember(slots) { slots.chunked(3) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131822), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunkedSlots.forEachIndexed { rowIndex, rowSlots ->
                    // Industrial Horizontal Street / Tree Verge Separator before rows
                    if (rowIndex > 0) {
                        OsbRoadSeparator(
                            rowIndex = rowIndex,
                            isEnglish = isEnglish
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowSlots.forEachIndexed { colIndex, biz ->
                            val slotIndex = rowIndex * 3 + colIndex + 1
                            val product = if (biz != null) Product.values().find { it.facilityId == biz.type || it.id == biz.type } else null
                            val isProducing = product != null && (
                                productionProgress.containsKey(product.id) ||
                                (productionProgress[product.id] ?: 0f) > 0f ||
                                activeProductions.any { it.productId == product.id || it.facilityId == product.facilityId }
                            )
                            val currentProg = if (product != null) {
                                productionProgress[product.id] ?: if (isProducing) 0.05f else 0f
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.88f)
                            ) {
                                OsbParcelSlotCard(
                                    slotNumber = slotIndex,
                                    business = biz,
                                    product = product,
                                    isProducing = isProducing,
                                    isHrAutomated = isHrAutoActive && isProducing,
                                    productionProgress = currentProg,
                                    isEnglish = isEnglish,
                                    onClick = { onSlotClick(biz, product) },
                                    onSpeedUpClick = { if (product != null) onSpeedUpClick?.invoke(product) }
                                )
                            }
                        }

                        // Satırı 3 kolona tamamlamak için boşluk doldur
                        for (i in 0 until (3 - rowSlots.size)) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.88f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hızlı Yeni Tesis Kurulum Butonu
            Button(
                onClick = { onSlotClick(null, null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F263C),
                    contentColor = ThemeNeonCyan
                ),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddBusiness,
                        contentDescription = null,
                        tint = ThemeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("🏗️ YENİ SANAYİ TESİSİ KURULUMU YAP", "🏗️ CONSTRUCT NEW INDUSTRIAL FACILITY", isEnglish),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Top Perimeter Avenue with asphalt markings, streetlights, and moving freight delivery truck
 */
@Composable
fun OsbPerimeterAvenueWithTruck(
    truckProgress: () -> Float,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        val w = size.width
        val h = size.height

        // 1. Asphalt Roadway
        drawRoundRect(
            color = Color(0xFF18202C),
            size = Size(w, h),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // 2. Yellow & White Road Markings
        drawLine(
            color = Color(0xFFF59E0B).copy(alpha = 0.7f),
            start = Offset(0f, 2.dp.toPx()),
            end = Offset(w, 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFFF59E0B).copy(alpha = 0.7f),
            start = Offset(0f, h - 2.dp.toPx()),
            end = Offset(w, h - 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        // Center dashed white line
        val dashW = 6.dp.toPx()
        val dashGap = 5.dp.toPx()
        var curX = 4.dp.toPx()
        while (curX < w - 4.dp.toPx()) {
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(curX, h / 2f),
                end = Offset(curX + dashW, h / 2f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            curX += dashW + dashGap
        }

        // 3. Animated 2.5D Logistics Delivery Truck
        val truckX = (truckProgress() * (w + 40.dp.toPx())) - 20.dp.toPx()
        val truckY = h / 2f - 4.dp.toPx()

        if (truckX > -25.dp.toPx() && truckX < w + 10.dp.toPx()) {
            // Truck cast shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(truckX - 2.dp.toPx(), truckY + 2.dp.toPx()),
                size = Size(20.dp.toPx(), 7.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )

            // Truck Container Body (Cyan / Blue logistics freight)
            drawRoundRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(truckX, truckY),
                size = Size(12.dp.toPx(), 6.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            // Container Corrugation lines
            drawLine(
                color = Color(0xFF0369A1),
                start = Offset(truckX + 4.dp.toPx(), truckY),
                end = Offset(truckX + 4.dp.toPx(), truckY + 6.dp.toPx()),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF0369A1),
                start = Offset(truckX + 8.dp.toPx(), truckY),
                end = Offset(truckX + 8.dp.toPx(), truckY + 6.dp.toPx()),
                strokeWidth = 1f
            )

            // Truck Cabin (White / Silver with Windshield)
            drawRoundRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(truckX + 12.dp.toPx(), truckY + 0.5.dp.toPx()),
                size = Size(5.dp.toPx(), 5.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            // Windshield
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(truckX + 14.5.dp.toPx(), truckY + 1.dp.toPx()),
                size = Size(2.dp.toPx(), 3.dp.toPx())
            )
            // Headlight beam glow
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.7f),
                radius = 1.5.dp.toPx(),
                center = Offset(truckX + 17.dp.toPx(), truckY + 3.dp.toPx())
            )
        }
    }
}

/**
 * Single 2.5D Parcel Slot Card
 * Features an architecturally rich 2.5D factory representation (Sawtooth/Gable industrial roofs,
 * corrugated walls, cylindrical silos, rooftop solar PV cells, rolling smoke puffs, and wear alerts)
 * or a clean civil surveyor vacant lot.
 */
@Composable
fun OsbParcelSlotCard(
    slotNumber: Int,
    business: BusinessEntity?,
    product: Product?,
    isProducing: Boolean,
    isHrAutomated: Boolean = false,
    productionProgress: Float,
    isEnglish: Boolean,
    onClick: () -> Unit,
    onSpeedUpClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isOccupied = business != null && product != null

    val infiniteTransition = rememberInfiniteTransition(label = "slot_anim")
    val smokeDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "smoke_drift"
    )
    val laserScan by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan"
    )
    val warningBlink = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_blink"
    )
    val isUpgrading = business?.isUpgrading == true
    val isConstructing = business?.isConstructing == true

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isConstructing, isUpgrading, business?.constructionEndTime, business?.upgradeEndTime) {
        if (isConstructing || isUpgrading) {
            while (isActive) {
                currentTimeMs = System.currentTimeMillis()
                delay(500L)
            }
        }
    }

    val upgradeSpin = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "upgrade_spin"
    )

    val wearLevel = business?.wearLevel ?: 0f
    val isNeedsRepair = wearLevel >= 0.7f

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isOccupied) Color(0xFF17202E) else Color(0xFF101622),
        border = BorderStroke(
            1.2.dp,
            if (isOccupied) {
                if (isConstructing || isUpgrading) Color(0xFFF59E0B)
                else if (isNeedsRepair) ThemeNegative // Alpha is handled by a Modifier below
                else if (isProducing) ThemeNeonCyan.copy(alpha = 0.85f)
                else Color(0xFF2B3A4F)
            } else {
                Color(0xFF222E40)
            }
        ),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
            .then(
                if (isNeedsRepair) Modifier.drawWithContent {
                    drawContent()
                    drawRoundRect(
                        color = ThemeNegative.copy(alpha = warningBlink.value),
                        size = size,
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx())
                    )
                }
                else Modifier
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val facilityImageRes = product?.facilityDrawableRes

            if (isOccupied && product != null && facilityImageRes != null) {
                // 1. High-Detail Facility Visual Card
                Image(
                    painter = painterResource(id = facilityImageRes),
                    contentDescription = product.getFacilityName(isEnglish),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Multi-stop Vignette Gradient Scrim (Keeps badges & text ultra-sharp & readable)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xDF090E17),
                                    Color(0x25090E17),
                                    Color(0x35090E17),
                                    Color(0xF0090E17)
                                )
                            )
                        )
                )

                // 3. Active Production Atmospheric Overlay (Cyan worklight pulse & smoke animation)
                if (isProducing) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Top-left Chimney Rising Smoke Particles
                        val chimneyX = w * 0.22f
                        val chimneyY = h * 0.28f
                        for (i in 0..2) {
                            val p = (smokeDrift + (i * 0.33f)) % 1f
                            val sRadius = 2.5.dp.toPx() + (p * 5.dp.toPx())
                            val sAlpha = (1f - p) * 0.60f
                            val sX = chimneyX + sin(p * Math.PI.toFloat() * 2f) * 3.dp.toPx()
                            val sY = chimneyY - (p * 16.dp.toPx())
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE2E8F0).copy(alpha = sAlpha),
                                        Color(0xFF94A3B8).copy(alpha = sAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(sX, sY),
                                    radius = sRadius
                                ),
                                radius = sRadius,
                                center = Offset(sX, sY)
                            )
                        }

                        // Bottom Cyan Production Worklight Line
                        drawLine(
                            color = ThemeNeonCyan.copy(alpha = 0.5f * laserScan),
                            start = Offset(0f, h - 2.dp.toPx()),
                            end = Offset(w, h - 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // 4. Critical Maintenance Flash Overlay (Wear >= 70%)
                if (isNeedsRepair) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ThemeNegative)
                            .graphicsLayer { alpha = 0.22f * warningBlink.value }
                    )
                }
            } else {
                // Background Canvas: Fallback 2.5D Isometric Architectural Factory / Vacant Survey Lot
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Reinforced Concrete Foundation Pad with 3D Depth
                    // Base Drop Shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
                        size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Top Face (Light Concrete)
                    drawRoundRect(
                        color = if (isOccupied) Color(0xFF1E2838) else Color(0xFF131A26),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h - 2.dp.toPx()),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Bottom 3D Edge Bevel (Dark Concrete)
                    drawRoundRect(
                        color = Color(0xFF0F1520),
                        topLeft = Offset(0f, h - 3.dp.toPx()),
                        size = Size(w, 3.dp.toPx()),
                        cornerRadius = CornerRadius(0f, 0f)
                    )

                    if (isOccupied && product != null) {
                        // ==========================================
                        // 2.5D ISOMETRIC FACTORY ARCHITECTURAL MODEL
                        // ==========================================
                        val pColor = Color(product.colorTint)
                        val factoryL = w * 0.15f
                        val factoryR = w * 0.85f
                        val factoryW = factoryR - factoryL
                        val factoryBaseY = h * 0.65f
                        val factoryH = h * 0.32f
                        val roofPeakY = factoryBaseY - factoryH

                        // Factory Building Ground Cast Shadow
                        drawOval(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(factoryL - 4.dp.toPx(), factoryBaseY - 2.dp.toPx()),
                            size = Size(factoryW + 8.dp.toPx(), 12.dp.toPx())
                        )

                        // Factory Main Body (Corrugated Industrial Blue/Graphite Wall)
                        val wallBrush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF2A384C), Color(0xFF1A2433)),
                            startY = roofPeakY + 6.dp.toPx(),
                            endY = factoryBaseY
                        )
                        drawRect(
                            brush = wallBrush,
                            topLeft = Offset(factoryL, roofPeakY + 6.dp.toPx()),
                            size = Size(factoryW, factoryBaseY - (roofPeakY + 6.dp.toPx()))
                        )

                        // Corrugation Vertical Ribs
                        val ribCount = 6
                        val ribStep = factoryW / ribCount
                        for (r in 1 until ribCount) {
                            drawLine(
                                color = Color(0xFF131B26),
                                start = Offset(factoryL + r * ribStep, roofPeakY + 6.dp.toPx()),
                                end = Offset(factoryL + r * ribStep, factoryBaseY),
                                strokeWidth = 1f
                            )
                        }

                        // Sawtooth / Gable Industrial Roof
                        val roofPath = Path().apply {
                            moveTo(factoryL - 2.dp.toPx(), roofPeakY + 6.dp.toPx())
                            lineTo(w * 0.5f, roofPeakY)
                            lineTo(factoryR + 2.dp.toPx(), roofPeakY + 6.dp.toPx())
                            close()
                        }
                        val roofBrush = Brush.verticalGradient(
                            colors = listOf(pColor.copy(alpha = 0.85f), Color(0xFF334155)),
                            startY = roofPeakY,
                            endY = roofPeakY + 6.dp.toPx()
                        )
                        drawPath(path = roofPath, brush = roofBrush)
                        drawPath(path = roofPath, color = pColor.copy(alpha = 0.9f), style = Stroke(width = 1.dp.toPx()))

                        // Rooftop Solar Array / Skylights (On Lv.2+ Facilities)
                        if ((business?.level ?: 1) >= 2) {
                            val solarW = factoryW * 0.5f
                            val solarH = 4.dp.toPx()
                            drawRoundRect(
                                color = Color(0xFF0284C7),
                                topLeft = Offset(w * 0.5f - solarW / 2f, roofPeakY + 2.dp.toPx()),
                                size = Size(solarW, solarH),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                            drawLine(
                                color = Color(0xFF38BDF8),
                                start = Offset(w * 0.5f, roofPeakY + 2.dp.toPx()),
                                end = Offset(w * 0.5f, roofPeakY + 2.dp.toPx() + solarH),
                                strokeWidth = 1f
                            )
                        }

                        // Industrial Silo / Tank on Right Side (For heavy/chemical/food facilities)
                        val siloX = factoryR - 6.dp.toPx()
                        val siloY = roofPeakY - 4.dp.toPx()
                        val siloW = 10.dp.toPx()
                        val siloH = factoryBaseY - siloY
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF64748B))
                            ),
                            topLeft = Offset(siloX, siloY),
                            size = Size(siloW, siloH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                        // Silo Dome Cap
                        drawArc(
                            color = Color(0xFFCBD5E1),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(siloX, siloY - 3.dp.toPx()),
                            size = Size(siloW, 6.dp.toPx())
                        )

                        // Loading Bay Roll-up Door on Ground
                        val doorW = 14.dp.toPx()
                        val doorH = 10.dp.toPx()
                        val doorX = factoryL + 4.dp.toPx()
                        val doorY = factoryBaseY - doorH
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(doorX, doorY),
                            size = Size(doorW, doorH)
                        )
                        // Shutter slats
                        for (s in 1..3) {
                            drawLine(
                                color = Color(0xFF334155),
                                start = Offset(doorX, doorY + s * (doorH / 4)),
                                end = Offset(doorX + doorW, doorY + s * (doorH / 4)),
                                strokeWidth = 1f
                            )
                        }

                        // Active Production Atmospheric Smoke / Laser / Strobe
                        if (isProducing) {
                            // Chimney Exhaust Pipe
                            val chimneyX = factoryL + 8.dp.toPx()
                            val chimneyY = roofPeakY - 8.dp.toPx()
                            drawRect(
                                color = Color(0xFF475569),
                                topLeft = Offset(chimneyX, chimneyY),
                                size = Size(4.dp.toPx(), 14.dp.toPx())
                            )

                            // Multi-layer Rising Smoke Puffs
                            for (i in 0..2) {
                                val p = (smokeDrift + (i * 0.33f)) % 1f
                                val sRadius = 3.dp.toPx() + (p * 6.dp.toPx())
                                val sAlpha = (1f - p) * 0.65f
                                val sX = chimneyX + 2.dp.toPx() + sin(p * Math.PI.toFloat() * 2f) * 4.dp.toPx()
                                val sY = chimneyY - (p * 20.dp.toPx())
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFE2E8F0).copy(alpha = sAlpha), Color(0xFF94A3B8).copy(alpha = sAlpha * 0.3f), Color.Transparent),
                                        center = Offset(sX, sY),
                                        radius = sRadius
                                    ),
                                    radius = sRadius,
                                    center = Offset(sX, sY)
                                )
                            }

                            // Ground Cyan Worklight Glow
                            drawCircle(
                                color = ThemeNeonCyan.copy(alpha = 0.25f),
                                radius = 12.dp.toPx(),
                                center = Offset(doorX + doorW / 2f, factoryBaseY)
                            )
                        }

                        // Roof Warning Strobe Beacon if wear >= 70%
                        if (isNeedsRepair) {
                            drawCircle(
                                color = ThemeNegative.copy(alpha = warningBlink.value),
                                radius = 3.dp.toPx(),
                                center = Offset(w * 0.5f, roofPeakY - 2.dp.toPx())
                            )
                        }
                    } else {
                        // ==========================================
                        // VACANT PARCEL (BOŞ ARSA - İNŞAATA HAZIR)
                        // ==========================================
                        // Surveyor Grid Blueprint Lines
                        val gridCols = 4
                        val colW = w / gridCols
                        for (c in 1 until gridCols) {
                            drawLine(
                                color = Color(0xFF1E2A3C),
                                start = Offset(c * colW, 4.dp.toPx()),
                                end = Offset(c * colW, h - 6.dp.toPx()),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
                            )
                        }
                        val gridRows = 3
                        val rowH = h / gridRows
                        for (r in 1 until gridRows) {
                            drawLine(
                                color = Color(0xFF1E2A3C),
                                start = Offset(4.dp.toPx(), r * rowH),
                                end = Offset(w - 4.dp.toPx(), r * rowH),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
                            )
                        }

                        // Corner Surveyor Stakes with Red/White boundary markers
                        val cornerOffset = 4.dp.toPx()
                        val stakeLen = 6.dp.toPx()
                        val stakeColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                        // Top-Left
                        drawLine(stakeColor, Offset(cornerOffset, cornerOffset), Offset(cornerOffset + stakeLen, cornerOffset), strokeWidth = 1.5f)
                        drawLine(stakeColor, Offset(cornerOffset, cornerOffset), Offset(cornerOffset, cornerOffset + stakeLen), strokeWidth = 1.5f)
                        // Top-Right
                        drawLine(stakeColor, Offset(w - cornerOffset, cornerOffset), Offset(w - cornerOffset - stakeLen, cornerOffset), strokeWidth = 1.5f)
                        drawLine(stakeColor, Offset(w - cornerOffset, cornerOffset), Offset(w - cornerOffset, cornerOffset + stakeLen), strokeWidth = 1.5f)
                        // Bottom-Left
                        drawLine(stakeColor, Offset(cornerOffset, h - cornerOffset - 2.dp.toPx()), Offset(cornerOffset + stakeLen, h - cornerOffset - 2.dp.toPx()), strokeWidth = 1.5f)
                        drawLine(stakeColor, Offset(cornerOffset, h - cornerOffset - 2.dp.toPx()), Offset(cornerOffset, h - cornerOffset - 2.dp.toPx() - stakeLen), strokeWidth = 1.5f)
                        // Bottom-Right
                        drawLine(stakeColor, Offset(w - cornerOffset, h - cornerOffset - 2.dp.toPx()), Offset(w - cornerOffset - stakeLen, h - cornerOffset - 2.dp.toPx()), strokeWidth = 1.5f)
                        drawLine(stakeColor, Offset(w - cornerOffset, h - cornerOffset - 2.dp.toPx()), Offset(w - cornerOffset, h - cornerOffset - 2.dp.toPx() - stakeLen), strokeWidth = 1.5f)
                    }
                }
            }

            // ==========================================
            // FOREGROUND UI HUD ELEMENTS (TEXT & BADGES)
            // ==========================================
            if (isOccupied && product != null) {
                // OCCUPIED PARCEL CONTENT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Parcel Tag & Level Stars / Construction Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bizCity = if (business != null) cities.find { it.id == business.cityId } else null
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF0F1724).copy(alpha = 0.90f),
                            border = BorderStroke(0.5.dp, if (bizCity != null) ThemeNeonCyan.copy(alpha = 0.5f) else Color(0xFF28364A))
                        ) {
                            Text(
                                text = if (bizCity != null) "${bizCity.countryFlag} ${bizCity.getDisplayName(isEnglish)}" else "P#$slotNumber",
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bizCity != null) ThemeNeonCyan else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (isConstructing || isUpgrading) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B),
                                    border = BorderStroke(0.5.dp, Color.White),
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Construction,
                                        contentDescription = null,
                                        tint = Color(0xFF1E1000),
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .graphicsLayer { rotationZ = upgradeSpin.value }
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(product.colorTint).copy(alpha = 0.35f),
                                border = BorderStroke(0.5.dp, Color(product.colorTint))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Star, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(8.dp))
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Text(
                                        text = "Lv.${business?.level ?: 1}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Middle area is kept crystal-clear so the factory 3D building visual is completely unobstructed!
                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Bar: Name & Live Progress / Wear Alert / Stored Stock
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0B101A).copy(alpha = 0.88f),
                        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = product.getDisplayName(isEnglish),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            if (isConstructing) {
                                Spacer(modifier = Modifier.height(2.dp))
                                val remainingMs = ((business?.constructionEndTime ?: 0L) - currentTimeMs).coerceAtLeast(0L)
                                val totalSec = remainingMs / 1000L
                                val m = (totalSec / 60)
                                val s = totalSec % 60
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFFB45309).copy(alpha = 0.95f),
                                    border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Construction,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(7.dp)
                                                .graphicsLayer { rotationZ = upgradeSpin.value }
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (m >= 60) "🏗️ ${m / 60}s ${m % 60}d" else "🏗️ ${m}d ${s}s",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else if (isUpgrading) {
                                Spacer(modifier = Modifier.height(2.dp))
                                val remainingMs = ((business?.upgradeEndTime ?: 0L) - currentTimeMs).coerceAtLeast(0L)
                                val totalSec = remainingMs / 1000L
                                val m = (totalSec / 60)
                                val s = totalSec % 60
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFFD97706).copy(alpha = 0.9f),
                                    border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Construction,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(7.dp)
                                                .graphicsLayer { rotationZ = upgradeSpin.value }
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (m >= 60) "⏳ ${m / 60}s ${m % 60}d" else "⏳ ${m}d ${s}s",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else if (isProducing) {
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { productionProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isHrAutomated) Color(0xFFC084FC) else ThemeNeonCyan,
                                    trackColor = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (isHrAutomated) Color(0xFF7C3AED).copy(alpha = 0.95f) else Color(0xFF0284C7).copy(alpha = 0.95f),
                                    border = BorderStroke(0.5.dp, if (isHrAutomated) Color(0xFFC084FC) else ThemeNeonCyan),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSpeedUpClick?.invoke()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            if (isHrAutomated) Icons.Rounded.SmartToy else Icons.Rounded.FlashOn,
                                            contentDescription = null,
                                            tint = if (isHrAutomated) Color(0xFFE9D5FF) else ThemeGold,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (isHrAutomated) "🤖 ${(productionProgress * 100).toInt()}%" else "⚡ ${(productionProgress * 100).toInt()}%",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else if (isNeedsRepair) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = ThemeNegative, modifier = Modifier.size(8.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = tr("Bakım!", "Repair!", isEnglish),
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeNegative
                                    )
                                }
                            } else {
                                val storedQty = business?.getStoredTotalQuantity() ?: 0
                                val cap = business?.getEffectiveStorageCapacity() ?: 500
                                Text(
                                    text = "$storedQty/${cap}t",
                                    fontSize = 7.5.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            } else {
                // VACANT PARCEL CONTENT (BOŞ ARSA - YENİ TESİS KURULUM SLOTU - VİBRANT & DİKKAT ÇEKİCİ)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F1B2E).copy(alpha = 0.95f),
                                    Color(0xFF09121F).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Lot Tag with Neon Pulsing Beacon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF05233A),
                            border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "P#$slotNumber",
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNeonCyan,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ThemeGold.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, ThemeGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(ThemeGold)
                                )
                                Text(
                                    text = tr("AÇIK PARSEL", "OPEN LOT", isEnglish),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold
                                )
                            }
                        }
                    }

                    // Center Build Action Button (Prominent, Glowing & Vibrant)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7),
                            border = BorderStroke(1.5.dp, Color(0xFF7DD3FC)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AddBusiness,
                                    contentDescription = "Tesis Kur",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr("+ YENİ TESİS KUR", "+ BUILD FACILITY", isEnglish),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom CTA Classification
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFF0F263C),
                        border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr("⚡ Fabrika İnşa Et", "⚡ Construct Factory", isEnglish),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBAE6FD),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Realistic Asphalt Road with 3D Shaded Trees, Sidewalks, and Kerbs between OSB parcel rows
 */
@Composable
fun OsbRoadSeparator(
    rowIndex: Int,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val w = size.width
        val h = size.height

        // 1. Asphalt Roadway
        drawRect(color = Color(0xFF161E2A), size = Size(w, h))

        // 2. Concrete Kerbs on Top and Bottom
        drawLine(
            color = Color(0xFF334155),
            start = Offset(0f, 0.5.dp.toPx()),
            end = Offset(w, 0.5.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF334155),
            start = Offset(0f, h - 0.5.dp.toPx()),
            end = Offset(w, h - 0.5.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        // 3. Center Dashed Lane Divider
        val dashW = 7.dp.toPx()
        val dashGap = 5.dp.toPx()
        var curX = 3.dp.toPx()
        while (curX < w - 3.dp.toPx()) {
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(curX, h / 2f),
                end = Offset(curX + dashW, h / 2f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            curX += dashW + dashGap
        }

        // 4. Realistic 3D Isometric Trees with Drop Shadows on the Roadside Verges
        val treeLocations = when (rowIndex) {
            1 -> listOf(w * 0.12f, w * 0.50f, w * 0.88f)
            2 -> listOf(w * 0.28f, w * 0.72f)
            else -> listOf(w * 0.18f, w * 0.52f, w * 0.82f)
        }

        treeLocations.forEach { tx ->
            // Tree directional ground shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = Offset(tx - 6.dp.toPx(), h / 2f - 2.dp.toPx()),
                size = Size(13.dp.toPx(), 7.dp.toPx())
            )
            // 2.5D Shaded Tree Foliage Canopy (Multi-tone radial gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A), Color(0xFF14532D)),
                    center = Offset(tx - 1.5.dp.toPx(), h / 2f - 1.5.dp.toPx()),
                    radius = 5.5.dp.toPx()
                ),
                radius = 5.dp.toPx(),
                center = Offset(tx, h / 2f)
            )
        }
    }
}

/**
 * OSB Zone KPI & Telemetry Header HUD Card
 */
@Composable
fun OsbZoneHudCard(
    selectedCity: CityProfile?,
    selectedCityFilter: String?,
    totalFacilities: Int,
    activeProducingCount: Int,
    totalStored: Int,
    totalCapacity: Int,
    avgWear: Int,
    powerLoadMw: Float,
    workforceCount: Int,
    isEnglish: Boolean,
    onCityChange: (String?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101726),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Rounded.LocationCity,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    CurrencyText(
                        text = if (selectedCityFilter == null) tr("TÜM SANAYİ BÖLGELERİ", "ALL INDUSTRIAL ZONES", isEnglish)
                               else "${selectedCity?.getDisplayName(isEnglish)?.uppercase()} " + tr("OSB", "OIZ", isEnglish),
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 11.5.sp,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (activeProducingCount > 0) ThemePositive.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (activeProducingCount > 0) ThemePositive else Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (activeProducingCount > 0) ThemePositive else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        CurrencyText(
                            text = "$activeProducingCount " + tr("Aktif Üretim", "Producing", isEnglish),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (activeProducingCount > 0) ThemePositive else Color.Gray
                        )
                    }
                }
            }

            // Quick Metrics Row (4 Metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OsbKpiMiniCard(
                    label = tr("Dolu Parsel", "Occupied", isEnglish),
                    value = "$totalFacilities / 12",
                    icon = Icons.Rounded.Apartment,
                    color = ThemeNeonCyan,
                    modifier = Modifier.weight(1f)
                )
                OsbKpiMiniCard(
                    label = tr("Depo Hacmi", "Storage", isEnglish),
                    value = "$totalStored/${totalCapacity}t",
                    icon = Icons.Rounded.Inventory2,
                    color = ThemeGold,
                    modifier = Modifier.weight(1f)
                )
                OsbKpiMiniCard(
                    label = tr("Şebeke Yükü", "Grid Power", isEnglish),
                    value = "%.1f MW".format(powerLoadMw),
                    icon = Icons.Rounded.Power,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                OsbKpiMiniCard(
                    label = tr("Ort. Aşınma", "Avg Wear", isEnglish),
                    value = "%$avgWear",
                    icon = Icons.Rounded.Build,
                    color = if (avgWear >= 70) ThemeNegative else Color.LightGray,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OsbKpiMiniCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF162030),
        border = BorderStroke(0.5.dp, Color(0xFF26354A)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(9.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = label, fontSize = 7.5.sp, color = Color.Gray, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMonoFontFamily,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

/**
 * City Filter Chips Row
 */
@Composable
fun OsbCityFilterRow(
    cities: List<CityProfile>,
    activeCityFilter: String?,
    businesses: List<BusinessEntity>,
    onSelectCity: (String?) -> Unit,
    isEnglish: Boolean
) {
    val haptic = LocalHapticFeedback.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = activeCityFilter == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectCity(null)
                },
                label = {
                    Text(
                        text = "🇹🇷 " + tr("Tüm Şehirler", "All Cities", isEnglish),
                        fontSize = 11.sp,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = if (activeCityFilter == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ThemeNeonCyan,
                    selectedLabelColor = Color(0xFF002026),
                    containerColor = Color(0xFF101726),
                    labelColor = Color.LightGray
                ),
                shape = RoundedCornerShape(4.dp)
            )
        }

        items(cities, key = { it.id }) { city ->
            val cityBusinessCount = businesses.count { it.cityId == city.id }
            FilterChip(
                selected = activeCityFilter == city.id,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectCity(if (activeCityFilter == city.id) null else city.id)
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${city.countryFlag} ${city.getDisplayName(isEnglish)}",
                            fontSize = 11.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = if (activeCityFilter == city.id) FontWeight.Bold else FontWeight.Normal
                        )
                        if (cityBusinessCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (activeCityFilter == city.id) Color(0xFF002026) else ThemeNeonCyan.copy(alpha = 0.3f),
                                modifier = Modifier.size(14.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$cityBusinessCount",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeCityFilter == city.id) ThemeNeonCyan else Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ThemeNeonCyan,
                    selectedLabelColor = Color(0xFF002026),
                    containerColor = Color(0xFF101726),
                    labelColor = Color.LightGray
                ),
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

/**
 * Interactive Facility & Product Search Bar with Tier Filters
 */
@Composable
fun OsbSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedTierFilter: ProductTier?,
    onTierFilterChanged: (ProductTier?) -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101726),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = tr("🔍 Tesis veya Ürün Ara (örn: Çelik, Rafineri, Otomotiv...)", "🔍 Search Facility or Product (e.g. Steel, Refinery...)"),
                        fontSize = 11.5.sp,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color(0xFF64748B)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Ara",
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSearchQueryChanged("")
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Temizle",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0B1017),
                    unfocusedContainerColor = Color(0xFF0B1017),
                    focusedBorderColor = ThemeNeonCyan,
                    unfocusedBorderColor = Color(0xFF243247),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ThemeNeonCyan
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            // Tier Quick Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedTierFilter == null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTierFilterChanged(null)
                        },
                        label = {
                            Text(
                                text = tr("Tüm Kademeler", "All Tiers", isEnglish),
                                fontSize = 10.sp,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = if (selectedTierFilter == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThemeNeonCyan.copy(alpha = 0.85f),
                            selectedLabelColor = Color(0xFF002026),
                            containerColor = Color(0xFF0B1017),
                            labelColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                }

                items(ProductTier.values()) { tier ->
                    val isSelected = selectedTierFilter == tier
                    val tierLabel = when (tier) {
                        ProductTier.TIER_1 -> tr("🌱 T1 Hammadde", "🌱 T1 Raw Material", isEnglish)
                        ProductTier.TIER_2 -> tr("⚙️ T2 İşlenmiş", "⚙️ T2 Processed", isEnglish)
                        ProductTier.TIER_3 -> tr("💎 T3 Yüksek Teknoloji", "💎 T3 High Tech", isEnglish)
                        ProductTier.TIER_4 -> tr("🚀 T4 Mega Sanayi", "🚀 T4 Mega Industry", isEnglish)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTierFilterChanged(if (isSelected) null else tier)
                        },
                        label = {
                            Text(
                                text = tierLabel,
                                fontSize = 10.sp,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThemeGold,
                            selectedLabelColor = Color(0xFF261800),
                            containerColor = Color(0xFF0B1017),
                            labelColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * ⚡ One-Tap Mass Harvest & Produce Shift Commander
 * Allows the player to harvest all completed batches and start production shifts with 1 tap.
 */
@Composable
fun OsbMassOperationBanner(
    businesses: List<BusinessEntity>,
    activeProductions: List<ActiveProduction>,
    productionProgress: Map<String, Float>,
    onMassHarvestAndProduceAll: () -> Unit,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val totalOwned = businesses.size
    val activeCount = remember(activeProductions, productionProgress) {
        val set = mutableSetOf<String>()
        set.addAll(productionProgress.keys)
        activeProductions.forEach { set.add(it.productId) }
        set.size
    }
    val idleCount = (totalOwned - activeCount).coerceAtLeast(0)

    if (totalOwned == 0) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF10192A),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(ThemeGold.copy(alpha = 0.8f), ThemeNeonCyan.copy(alpha = 0.6f)))),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ThemeGold.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, ThemeGold),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.FlashOn,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    CurrencyText(
                        text = tr("Holding Vardiya Komutası", "Holding Shift Command", isEnglish),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr("🏢 $totalOwned Tesis", "🏢 $totalOwned Facilities", isEnglish),
                        fontSize = 9.5.sp,
                        color = Color.LightGray,
                        fontFamily = RobotoMonoFontFamily
                    )
                    Text(text = "•", fontSize = 9.sp, color = Color.DarkGray)
                    Text(
                        text = tr("⚡ $activeCount Üretimde", "⚡ $activeCount Active", isEnglish),
                        fontSize = 9.5.sp,
                        color = ThemeNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    if (idleCount > 0) {
                        Text(text = "•", fontSize = 9.sp, color = Color.DarkGray)
                        Text(
                            text = tr("💤 $idleCount Hazır", "💤 $idleCount Idle", isEnglish),
                            fontSize = 9.5.sp,
                            color = ThemeGold,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMassHarvestAndProduceAll()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeGold,
                    contentColor = Color(0xFF221100)
                ),
                shape = RoundedCornerShape(5.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF221100),
                        modifier = Modifier.size(15.dp)
                    )
                    CurrencyText(
                        text = tr("⚡ Tümünü Topla & Başlat", "⚡ Harvest & Run All", isEnglish),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Full Live Display of ALL Active and HR-Automated Productions
 * Displays every single active production line without any 5-facility limitation.
 */
@Composable
fun OsbActiveAndAutomatedProductionsPreview(
    businesses: List<BusinessEntity>,
    productionProgress: Map<String, Float>,
    productionDurations: Map<String, Long>,
    activeProductions: List<ActiveProduction>,
    managers: List<CompanyManager>,
    isEnglish: Boolean,
    onFacilityClick: (BusinessEntity, Product) -> Unit,
    onSpeedUpClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val prodManager = managers.find { it.id == "mgr_prod" }
    val isHrAutoActive = prodManager?.let { it.isHired && it.isActive } == true

    // Gather all active producing products
    val activeProductIds = remember(productionProgress, activeProductions) {
        val set = mutableSetOf<String>()
        set.addAll(productionProgress.keys)
        activeProductions.forEach { set.add(it.productId) }
        set.toList()
    }

    if (activeProductIds.isEmpty() && !isHrAutoActive) {
        return
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101926),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(ThemeNeonCyan.copy(alpha = 0.6f), Color(0xFF28364A)))),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThemeNeonCyan.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, ThemeNeonCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = ThemeNeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = "${activeProductIds.size} " + tr("CANLI ÜRETİM HATTI", "ACTIVE PRODUCTION LINES", isEnglish),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                color = ThemeNeonCyan
                            )
                        }
                    }

                    if (isHrAutoActive) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF7C3AED).copy(alpha = 0.25f),
                            border = BorderStroke(0.5.dp, Color(0xFFC084FC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.SmartToy,
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                CurrencyText(
                                    text = tr("🤖 İK Otomasyonu", "🤖 HR Automation", isEnglish),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color(0xFFE9D5FF)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = tr("Tümü Gösteriliyor (${activeProductIds.size})", "Showing All (${activeProductIds.size})", isEnglish),
                    fontSize = 8.5.sp,
                    fontFamily = RobotoMonoFontFamily,
                    color = Color(0xFF94A3B8)
                )
            }

            if (activeProductIds.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0B1017),
                    border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Engineering,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tr(
                                "İnsan Kaynakları & Üretim Müdürü görevde. Depo ve hammadde hazır olduğunda tüm tesislerinizde otomatik üretim başlar.",
                                "HR & Production Manager active. Automated production starts across all facilities when storage and materials permit.",
                                isEnglish
                            ),
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 13.sp
                        )
                    }
                }
            } else {
                // Horizontal scrolling list of ALL active productions (NO 5-item limit)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activeProductIds, key = { it }) { productId ->
                        val product = Product.values().find { it.id == productId }
                        val matchingBiz = businesses.find { it.type == product?.facilityId || it.type == product?.id }
                        val activeProdEntity = activeProductions.find { it.productId == productId }
                        val cityId = activeProdEntity?.cityId ?: matchingBiz?.cityId
                        val cityName = cities.find { it.id == cityId }?.getDisplayName(isEnglish) ?: cityId ?: "OSB"
                        val progress = activeProdEntity?.getProgress() ?: (productionProgress[productId] ?: 0.05f)
                        val duration = activeProdEntity?.totalDurationMs ?: (productionDurations[productId] ?: 60000L)
                        val remainingMs = activeProdEntity?.getRemainingTimeMs() ?: (((1f - progress) * duration).toLong().coerceAtLeast(0L))
                        val remainingSec = (remainingMs / 1000L)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF162030),
                            border = BorderStroke(1.dp, if (isHrAutoActive) Color(0xFF7C3AED).copy(alpha = 0.7f) else ThemeNeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .width(180.dp)
                                .clickable {
                                    if (matchingBiz != null && product != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onFacilityClick(matchingBiz, product)
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Top Row: Icon + Names
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(product?.colorTint ?: 0xFF00E5FF).copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, Color(product?.colorTint ?: 0xFF00E5FF)),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (product != null) {
                                                UniversalProductIcon(product = product, size = 18.dp)
                                            } else {
                                                Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product?.getDisplayName(isEnglish) ?: productId,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "📍 $cityName",
                                            fontSize = 8.sp,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }

                                    if (product != null) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF7C3AED),
                                            modifier = Modifier
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onSpeedUpClick(product)
                                                }
                                        ) {
                                            Text(
                                                text = "⚡1💎",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Progress Line + Percentage & Time
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isHrAutoActive) Color(0xFFC084FC) else ThemeNeonCyan,
                                    trackColor = Color(0xFF0F1724)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${(progress * 100).toInt()}% " + tr("tamamlandı", "completed", isEnglish),
                                        fontSize = 8.sp,
                                        fontFamily = RobotoMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHrAutoActive) Color(0xFFE9D5FF) else ThemeNeonCyan
                                    )
                                    Text(
                                        text = "${remainingSec}s",
                                        fontSize = 8.sp,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.LightGray
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

/**
 * Interactive 2.5D Facility Management Detail Dialog / Card
 * Displayed when user taps on any built facility on the 2.5D OSB ground.
 */
@Composable
fun OsbFacilityDetailDialog(
    business: BusinessEntity,
    product: Product,
    uiState: GameUiState,
    viewModel: GameViewModel,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onProduceClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onMaintainClick: () -> Unit,
    onSellClick: () -> Unit,
    onNavigateToRd: (String?) -> Unit
) {
    val player = uiState.playerState.player
    val playerMoney = player?.money ?: 0L
    val city = cities.find { it.id == business.cityId }
    val haptic = LocalHapticFeedback.current

    val upgradeCost = product.facilityCost * (business.level + 1) / 2
    val canAffordUpgrade = playerMoney >= upgradeCost && business.level < 10

    val wearPercent = (business.wearLevel * 100).toInt()
    val maintenanceCost = (product.facilityCost * 0.05f * business.wearLevel * (business.level * 0.8f)).toLong().coerceAtLeast(500L)
    val canAffordMaintenance = playerMoney >= maintenanceCost && business.wearLevel > 0.01f

    val storedItems = business.getStoredItemsMap()
    val storedTotal = business.getStoredTotalQuantity()
    val capacity = business.getEffectiveStorageCapacity()
    val isStorageFull = storedTotal >= capacity && capacity > 0

    var currentTimeMs by remember { mutableLongStateOf(com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()) }
    val activeProd = uiState.inventoryState.activeProductions.find {
        it.facilityId == product.facilityId || it.productId == product.id || it.businessId == business.id
    }
    val isProducing = activeProd != null || (uiState.productionProgress[product.id] ?: 0f) > 0f
    val currentProgress = activeProd?.getProgress(currentTimeMs) ?: (uiState.productionProgress[product.id] ?: 0f)

    LaunchedEffect(business.isConstructing, business.isUpgrading, business.constructionEndTime, business.upgradeEndTime, isProducing, activeProd?.id) {
        if (business.isConstructing || business.isUpgrading || isProducing || activeProd != null) {
            while (isActive) {
                currentTimeMs = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
                delay(500L)
            }
        }
    }

    LaunchedEffect(currentTimeMs) {
        if (business.isConstructing && business.constructionEndTime != null && currentTimeMs >= business.constructionEndTime) {
            viewModel.checkAndProcessFacilityConstructions()
        }
        if (business.isUpgrading && business.upgradeEndTime != null && currentTimeMs >= business.upgradeEndTime) {
            viewModel.checkAndProcessFacilityUpgrades()
        }
        if (activeProd != null && currentTimeMs >= activeProd.effectiveEndTimeMs) {
            viewModel.checkAndProcessActiveProductions()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF101726),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val facilityImageRes = product.facilityDrawableRes

                if (facilityImageRes != null) {
                    // Hero Image Banner with Gradients and Overlay HUD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    ) {
                        Image(
                            painter = painterResource(id = facilityImageRes),
                            contentDescription = product.getFacilityName(isEnglish),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Vignette Gradients for High Readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xDF090E17),
                                            Color(0x20090E17),
                                            Color(0xF0101726)
                                        )
                                    )
                                )
                        )

                        // Top Header inside Image Box: City Tag + Close button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xDF090E17),
                                border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = city?.countryFlag ?: "🏢", fontSize = 10.sp)
                                    Text(
                                        text = "${city?.getDisplayName(isEnglish)?.uppercase() ?: business.cityId} " + tr("OSB", "OIZ", isEnglish),
                                        fontSize = 9.sp,
                                        fontFamily = RobotoMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xDF090E17),
                                border = BorderStroke(0.5.dp, Color(0xFF28364A)),
                                modifier = Modifier.size(28.dp)
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Bottom Info inside Image Box: Facility Name, Level & Tier, Live Status
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xDF090E17),
                                    border = BorderStroke(1.dp, Color(product.colorTint)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        UniversalProductIcon(product = product, size = 22.dp)
                                    }
                                }
                                Column {
                                    CurrencyText(
                                        text = product.getFacilityName(isEnglish),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = ThemeGold.copy(alpha = 0.25f),
                                            border = BorderStroke(0.5.dp, ThemeGold)
                                        ) {
                                            Text(
                                                text = "★ " + tr("Seviye", "Level", isEnglish) + " ${business.level}",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = ThemeGold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(0.5.dp, Color(0xFF334155))
                                        ) {
                                            Text(
                                                text = product.tier.name.replace("_", " "),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = Color(0xFF94A3B8),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Live status badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    business.isConstructing -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                                    isProducing -> ThemePositive.copy(alpha = 0.25f)
                                    isStorageFull -> Color(0xFFEF5350).copy(alpha = 0.25f)
                                    wearPercent >= 70 -> ThemeNegative.copy(alpha = 0.25f)
                                    else -> Color(0xFF1E293B)
                                },
                                border = BorderStroke(
                                    0.5.dp,
                                    when {
                                        business.isConstructing -> Color(0xFFF59E0B)
                                        isProducing -> ThemePositive
                                        isStorageFull -> Color(0xFFEF5350)
                                        wearPercent >= 70 -> ThemeNegative
                                        else -> Color(0xFF475569)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        when {
                                            business.isConstructing -> Icons.Rounded.Construction
                                            isProducing -> Icons.Rounded.Bolt
                                            isStorageFull -> Icons.Rounded.Inventory2
                                            wearPercent >= 70 -> Icons.Rounded.Warning
                                            else -> Icons.Rounded.Schedule
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            business.isConstructing -> Color(0xFFF59E0B)
                                            isProducing -> ThemePositive
                                            isStorageFull -> Color(0xFFEF5350)
                                            wearPercent >= 70 -> ThemeNegative
                                            else -> Color.LightGray
                                        },
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = when {
                                            business.isConstructing -> tr("İNŞAAT", "CONSTRUCTING", isEnglish)
                                            isProducing -> tr("ÜRETİYOR", "PRODUCING", isEnglish)
                                            isStorageFull -> tr("DEPO DOLU", "STORAGE FULL", isEnglish)
                                            wearPercent >= 70 -> tr("BAKIM!", "REPAIR!", isEnglish)
                                            else -> tr("BOŞTA", "IDLE", isEnglish)
                                        },
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = when {
                                            business.isConstructing -> Color(0xFFF59E0B)
                                            isProducing -> ThemePositive
                                            isStorageFull -> Color(0xFFEF5350)
                                            wearPercent >= 70 -> ThemeNegative
                                            else -> Color.LightGray
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Fallback Header: Product Icon + Facility Name + City + Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UniversalProductIcon(product = product, size = 42.dp)
                            Column {
                                CurrencyText(
                                    text = product.getFacilityName(isEnglish),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${city?.countryFlag ?: "🏢"} ${city?.getDisplayName(isEnglish) ?: business.cityId}",
                                        fontSize = 11.sp,
                                        color = ThemeNeonCyan
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = ThemeGold.copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, ThemeGold)
                                    ) {
                                        Text(
                                            text = "★ Seviye ${business.level}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = ThemeGold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = Color.Gray)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kalite Potansiyeli Barı (Quality Potential Bar)
                    FacilityQualityPotentialBar(
                        facilityLevel = business.level,
                        product = product,
                        isProducing = isProducing,
                        productionProgress = currentProgress,
                        wearLevel = business.wearLevel
                    )

                    // 1. Live Production or Construction Card
                    if (business.isConstructing) {
                        val now = currentTimeMs
                        val endTime = business.constructionEndTime ?: (now + 1000L)
                        val totalDurationMs = business.getConstructionDurationMs()
                        val remainingMs = (endTime - now).coerceAtLeast(0L)
                        val progress = (1f - (remainingMs.toFloat() / totalDurationMs.toFloat())).coerceIn(0f, 1f)
                        val totalSec = remainingMs / 1000L
                        val hours = totalSec / 3600
                        val minutes = (totalSec % 3600) / 60
                        val seconds = totalSec % 60
                        val countdownText = if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
                        val diamondCost = business.getConstructionDiamondCost(remainingMs)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Construction,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tr("🏗️ İnşaat: $countdownText", "🏗️ Constructing: $countdownText", isEnglish),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                    Text(
                                        text = "%${(progress * 100).toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFFF59E0B),
                                    trackColor = Color(0xFF0D131F)
                                )

                                Text(
                                    text = tr("İnşaat tamamlandıktan sonra üretime başlayabilirsiniz.", "You can begin production once construction is complete.", isEnglish),
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.handleIntent(GameIntent.SpeedUpBusinessConstructionWithGems(business.id))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD97706),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.FlashOn,
                                            contentDescription = null,
                                            tint = ThemeGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tr("⚡ $diamondCost 💎 ile Hemen Tamamla", "⚡ Instant Complete ($diamondCost 💎)", isEnglish),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val activeProd = uiState.inventoryState.activeProductions.find {
                            it.facilityId == product.facilityId || it.productId == product.id || it.businessId == business.id
                        }
                        val totalDur = activeProd?.totalDurationMs ?: (uiState.productionDurations[product.id] ?: product.baseDurationMs)
                        val remainingTimeMs = if (activeProd != null) {
                            (activeProd.effectiveEndTimeMs - currentTimeMs).coerceAtLeast(0L)
                        } else {
                            ((1f - currentProgress) * totalDur).toLong().coerceAtLeast(0L)
                        }
                        val totalSec = remainingTimeMs / 1000L
                        val minutes = totalSec / 60
                        val seconds = totalSec % 60
                        val timeText = if (minutes > 0) "${minutes} " + tr("dk", "m", isEnglish) + " ${seconds} " + tr("sn", "s", isEnglish) else "${seconds} " + tr("saniye", "seconds", isEnglish)
                        val producedQty = activeProd?.quantity ?: (10 * business.level)
                        val skipCost = kotlin.math.max(1, (remainingTimeMs / 3_600_000L).toInt())

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF141D2B),
                            border = BorderStroke(1.2.dp, if (isProducing) ThemeNeonCyan.copy(alpha = 0.7f) else Color(0xFF26354A)),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Rounded.PrecisionManufacturing,
                                            contentDescription = null,
                                            tint = if (isProducing) ThemePositive else Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        CurrencyText(
                                            text = if (isProducing) tr("⚡ Üretim Devam Ediyor", "⚡ Production in Progress", isEnglish)
                                                   else tr("🏭 Tesis Boşta (Üretime Hazır)", "🏭 Idle (Ready to Produce)", isEnglish),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProducing) ThemePositive else Color.White
                                        )
                                    }

                                    Button(
                                        onClick = onProduceClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        CurrencyText(
                                            text = tr("Üretim Paneli", "Produce Panel", isEnglish),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }

                                if (isProducing) {
                                    // Highlighted Active Production Info Card (Produced Item, Quantity, Time Countdown)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0A111C),
                                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    UniversalProductIcon(product = product, size = 24.dp)
                                                    Column {
                                                        Text(
                                                            text = "${producedQty} " + tr("Adet", "Units", isEnglish) + " ${product.getDisplayName(isEnglish)}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Black,
                                                            fontFamily = RobotoMonoFontFamily,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = tr("Kalan Süre:", "Remaining Time:", isEnglish) + " $timeText",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ThemeNeonCyan
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = ThemeNeonCyan.copy(alpha = 0.2f),
                                                    border = BorderStroke(0.5.dp, ThemeNeonCyan)
                                                ) {
                                                    Text(
                                                        text = "%${(currentProgress * 100).toInt()}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = RobotoMonoFontFamily,
                                                        color = ThemeNeonCyan,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            LinearProgressIndicator(
                                                progress = { currentProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = ThemeNeonCyan,
                                                trackColor = Color(0xFF1E293B)
                                            )

                                            Text(
                                                text = tr(
                                                    "⏱️ $timeText sonra $producedQty adet ${product.getDisplayName(isEnglish)} üretimi tamamlanıp şirket deposuna aktarılacak.",
                                                    "⏱️ In $timeText, $producedQty units of ${product.getDisplayName(isEnglish)} will finish and transfer to depot.",
                                                    isEnglish
                                                ),
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.skipProductionWithGems(product.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF7C3AED),
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFA78BFA)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FlashOn,
                                                contentDescription = null,
                                                tint = ThemeGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = tr("⚡ ELMASLA ANINDA BİTİR (-$skipCost 💎)", "⚡ INSTANT FINISH WITH GEMS (-$skipCost 💎)", isEnglish),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    // Idle Production Specifications Card
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0A111C),
                                        border = BorderStroke(0.5.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            UniversalProductIcon(product = product, size = 24.dp)
                                            Column {
                                                Text(
                                                    text = tr("Parti Kapasitesi:", "Batch Yield:", isEnglish) + " ${10 * business.level} " + tr("Adet", "Units", isEnglish),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = tr("Üretim Süresi:", "Cycle Duration:", isEnglish) + " ${(product.baseDurationMs / 60000L).coerceAtLeast(1L)} " + tr("dakika", "minutes", isEnglish),
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Health & Maintenance (Aşınma & Bakım)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF162030),
                    border = BorderStroke(1.dp, Color(0xFF26354A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Build,
                                    contentDescription = null,
                                    tint = if (wearPercent >= 70) ThemeNegative else ThemeGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("Tesis Aşınması: %$wearPercent", "Facility Wear: $wearPercent%", isEnglish),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (wearPercent >= 70) ThemeNegative else Color.White
                                )
                            }
                            val wearPenaltyText = when {
                                wearPercent <= 20 -> tr("Kaliteyi etkilemez (0-20%)", "No quality penalty (0-20%)", isEnglish)
                                wearPercent <= 40 -> tr("Kalite: -1★ daha düşük ürün", "Quality: -1★ lower product", isEnglish)
                                wearPercent <= 60 -> tr("Kalite: -2★ daha düşük ürün", "Quality: -2★ lower product", isEnglish)
                                wearPercent <= 80 -> tr("Kalite: -3★ daha düşük ürün", "Quality: -3★ lower product", isEnglish)
                                else -> tr("Kritik: -4★ kalite & üretim yavaşladı!", "Critical: -4★ quality & slow production!", isEnglish)
                            }
                            Text(
                                text = wearPenaltyText,
                                fontSize = 9.sp,
                                fontWeight = if (wearPercent > 20) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    wearPercent > 80 -> ThemeNegative
                                    wearPercent > 40 -> Color(0xFFFFB74D)
                                    wearPercent > 20 -> Color(0xFFFFE082)
                                    else -> Color(0xFF81C784)
                                }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { business.wearLevel },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (wearPercent >= 70) ThemeNegative else ThemeGold,
                                trackColor = Color(0xFF0D131F)
                            )
                        }

                        Button(
                            onClick = onMaintainClick,
                            enabled = canAffordMaintenance,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wearPercent >= 70) ThemeNegative else Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CurrencyText(
                                    text = tr("Bakım Yap", "Maintain", isEnglish),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                CurrencyText(
                                    text = formatMoney(maintenanceCost),
                                    fontSize = 9.sp,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                }

                // 3. Storage & Inventory Breakdown
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF162030),
                    border = BorderStroke(1.dp, Color(0xFF26354A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("Tesis Deposu (Stok)", "Depot (Stock)", isEnglish),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            CurrencyText(
                                text = "$storedTotal / ${capacity}t (%${if (capacity > 0) (storedTotal * 100 / capacity) else 0})",
                                fontSize = 10.sp,
                                fontFamily = RobotoMonoFontFamily,
                                color = Color.LightGray
                            )
                        }

                        if (storedItems.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                storedItems.forEach { (itemId, qty) ->
                                    val itemProd = Product.values().find { it.id == itemId }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF0F1724),
                                        border = BorderStroke(0.5.dp, Color(0xFF2A3A4E))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (itemProd != null) {
                                                UniversalProductIcon(product = itemProd, size = 14.dp)
                                            }
                                            Text(
                                                text = "${itemProd?.getDisplayName(isEnglish) ?: itemId}: ${qty}t",
                                                fontSize = 9.sp,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = tr("Depo boş", "Depot empty", isEnglish),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 3.5 Primary Production & Smart Procurement Card (If Idle)
                if (!business.isConstructing && !business.isUpgrading && !isProducing) {
                    val batchQty = (business.level * 5).coerceAtLeast(1)
                    val missingIngredients = product.recipe.filter { req ->
                        val stock = uiState.inventoryState.items.find { it.itemId == req.productId }?.quantity ?: 0
                        stock < req.amountPerUnit * batchQty
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF131F33),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = tr("Vardiya Üretim Emri", "Shift Production Order", isEnglish),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = tr("Standart: $batchQty Ton", "Standard: $batchQty Tons", isEnglish),
                                    fontSize = 10.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeNeonCyan
                                )
                            }

                            if (missingIngredients.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr("⚠️ ${missingIngredients.size} hammadde eksik", "⚠️ ${missingIngredients.size} materials missing", isEnglish),
                                        fontSize = 10.sp,
                                        color = ThemeGold
                                    )
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.buyMissingIngredients(product.id, batchQty)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Rounded.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Text(
                                                text = tr("📦 Eksikleri Pazardan Al", "📦 Buy Missing Materials", isEnglish),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }
                                    }
                                }
                            }

                            if (isStorageFull) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF5350).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = tr("⚠️ Depo Dolu: Depo dolunca üretim durur, deponun boşalması beklenir.", "⚠️ Storage Full: Production stopped until storage is cleared.", isEnglish),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onProduceClick,
                                enabled = !isStorageFull && !isProducing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isStorageFull || isProducing) Color(0xFF334155) else ThemeNeonCyan,
                                    contentColor = Color(0xFF00222B),
                                    disabledContainerColor = Color(0xFF1E293B),
                                    disabledContentColor = if (isProducing) ThemeNeonCyan else Color(0xFFEF5350)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (isStorageFull) Icons.Rounded.Inventory2 else Icons.Rounded.FlashOn,
                                        contentDescription = null,
                                        tint = if (isProducing) ThemeNeonCyan else if (isStorageFull) Color(0xFFEF5350) else Color(0xFF00222B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isProducing) tr("⚡ Üretim Sürüyor (Yukarıdan Takip Edin)", "⚡ Producing (See Above)", isEnglish)
                                               else if (isStorageFull) tr("⚠️ DEPO DOLU (Boşalması Bekleniyor)", "⚠️ STORAGE FULL (Waiting for Clearance)", isEnglish)
                                               else tr("⚡ Üretim Ayarla & Başlat", "⚡ Configure & Start Production", isEnglish),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Action Buttons (Upgrade Level & Sell/Liquidate)
                if (business.isConstructing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("İnşaat tamamlanana kadar seviye yükseltilemez", "Cannot upgrade level until construction completes", isEnglish),
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onSellClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNegative),
                            border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tr("İptal Et / Sat", "Cancel / Sell", isEnglish),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNegative
                            )
                        }
                    }
                } else if (business.isUpgrading) {
                    val now = currentTimeMs
                    val endTime = business.upgradeEndTime ?: (now + 1000L)
                    val totalDurationMs = business.getUpgradeDurationMs()
                    val remainingMs = (endTime - now).coerceAtLeast(0L)
                    val progress = (1f - (remainingMs.toFloat() / totalDurationMs.toFloat())).coerceIn(0f, 1f)
                    val totalSec = remainingMs / 1000L
                    val hours = totalSec / 3600
                    val minutes = (totalSec % 3600) / 60
                    val seconds = totalSec % 60
                    val countdownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    val diamondCost = business.getUpgradeDiamondCost(remainingMs)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Construction,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr("⏳ Yükseltiliyor: $countdownText", "⏳ Upgrading: $countdownText", isEnglish),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                                Text(
                                    text = "%${(progress * 100).toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFF59E0B),
                                trackColor = Color(0xFF0D131F)
                            )

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.handleIntent(GameIntent.SpeedUpBusinessUpgradeWithGems(business.id))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.FlashOn,
                                        contentDescription = null,
                                        tint = ThemeGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr("⚡ $diamondCost 💎 ile Hemen Tamamla", "⚡ Instant Complete ($diamondCost 💎)", isEnglish),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSellClick,
                            modifier = Modifier.weight(0.9f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNegative),
                            border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = tr("Tesisi Sat", "Sell Facility", isEnglish),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val durationLabel = when (business.level) {
                            1 -> tr("30 Dk", "30 Min", isEnglish)
                            2 -> tr("2 Saat", "2 Hours", isEnglish)
                            3 -> tr("6 Saat", "6 Hours", isEnglish)
                            4 -> tr("18 Saat", "18 Hours", isEnglish)
                            else -> tr("24 Saat", "24 Hours", isEnglish)
                        }

                        Button(
                            onClick = onUpgradeClick,
                            enabled = canAffordUpgrade,
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF2D1600)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    CurrencyText(
                                        text = tr("Yükselt (⏳ $durationLabel)", "Upgrade (⏳ $durationLabel)", isEnglish),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                CurrencyText(
                                    text = formatMoney(upgradeCost),
                                    fontSize = 9.sp,
                                    fontFamily = RobotoMonoFontFamily,
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
