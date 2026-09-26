package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.CurrencyText
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.tr
import com.example.viewmodel.GameViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.roundToInt

data class TradeArbitrageInfo(
    val productName: String,
    val productIcon: ImageVector,
    val profitMarginPercent: Int,
    val isOutboundFromHq: Boolean,
    val description: String
)

data class MapLayersState(
    val showDeliveries: Boolean = true,
    val showFacilities: Boolean = true,
    val showTradeHotspots: Boolean = true,
    val showMegaProjects: Boolean = true
)

// =========================================================================
// 📐 DISTANCE & ARBITRAGE UTILITIES
// =========================================================================

fun calculateDistanceKm(city1Id: String, city2Id: String): Int {
    val p1 = cityCoordinates[city1Id] ?: return 0
    val p2 = cityCoordinates[city2Id] ?: return 0
    val meters = p1.distanceToAsDouble(p2)
    return (meters / 1000.0).roundToInt()
}

fun calculateTransitTimeFormatted(distanceKm: Int, isAir: Boolean = false, isSea: Boolean = false): String {
    val speed = when {
        isAir -> 650.0
        isSea -> 35.0
        else -> 80.0
    }
    val hours = distanceKm / speed
    val totalMinutes = (hours * 60).roundToInt().coerceAtLeast(1)
    return if (totalMinutes < 60) {
        "$totalMinutes dk"
    } else {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        if (m == 0) "${h} sa" else "${h} sa ${m} dk"
    }
}

fun calculateTradeArbitrage(
    hqCityId: String,
    targetCity: CityProfile,
    marketPrices: List<MarketPriceEntity>,
    isEnglish: Boolean
): TradeArbitrageInfo? {
    if (hqCityId == targetCity.id) return null
    val targetMultiplier = targetCity.economicMultiplier
    val products = Product.values().filter { it.canBeBuiltIn(targetCity.id) || targetCity.primaryProducts.contains(it.id) }
    val featuredProduct = products.firstOrNull() ?: Product.values().firstOrNull() ?: return null

    val baseMargin = ((targetMultiplier - 1.0f) * 100).toInt().coerceAtLeast(12)
    val bonusMargin = if (targetCity.isGlobal) baseMargin + 18 else baseMargin + 8
    val finalMargin = bonusMargin.coerceIn(15, 95)

    val desc = if (isEnglish) {
        "High profit potential: Sourcing ${featuredProduct.getDisplayName(true)} from ${targetCity.getDisplayName(true)} yields approx. +%$finalMargin arbitrage margin."
    } else {
        "Yüksek kâr fırsatı: ${targetCity.getDisplayName(false)} bölgesinden temin edilen ${featuredProduct.getDisplayName(false)}, Merkezde yaklaşık %%$finalMargin arbitraj kâr marjı sağlar."
    }

    return TradeArbitrageInfo(
        productName = featuredProduct.getDisplayName(isEnglish),
        productIcon = featuredProduct.icon,
        profitMarginPercent = finalMargin,
        isOutboundFromHq = targetMultiplier > 1.15f,
        description = desc
    )
}

fun getDeliveryCurrentGeo(
    originId: String,
    destId: String,
    progress: Float
): GeoPoint? {
    val originGeo = cityCoordinates[originId] ?: return null
    val destGeo = cityCoordinates[destId] ?: return null
    val clampedProg = progress.coerceIn(0f, 1f)

    val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(originId)
    val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(destId)
    val isOriginRailway = GeoRoutePlanner.railwayCities.contains(originId)
    val isDestRailway = GeoRoutePlanner.railwayCities.contains(destId)
    val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(originId)
    val isDestGlobal = !GeoRoutePlanner.isTurkishCity(destId)
    val distMeters = originGeo.distanceToAsDouble(destGeo)
    val isGlobal = isOriginGlobal || isDestGlobal
    val isMaritime = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(originId, destId)))
    val isAir = !isMaritime && ((isOriginGlobal || isDestGlobal) || (distMeters >= 450_000.0 && GeoRoutePlanner.isAirHub(originId, destId)))
    val isRail = !isMaritime && !isAir && (isOriginRailway && isDestRailway)

    val routePoints: List<GeoPoint> = when {
        isMaritime -> GeoRoutePlanner.getMaritimeRoute(originId, destId, originGeo, destGeo)
        isAir -> GeoRoutePlanner.getAirRoute(originId, destId, originGeo, destGeo)
        isRail -> GeoRoutePlanner.getRailwayRoute(originId, destId, originGeo, destGeo)
        else -> GeoRoutePlanner.getHighwayRoute(originId, destId, originGeo, destGeo)
    }

    if (routePoints.isEmpty()) return GeoPoint(
        originGeo.latitude + (destGeo.latitude - originGeo.latitude) * clampedProg,
        originGeo.longitude + (destGeo.longitude - originGeo.longitude) * clampedProg
    )

    if (routePoints.size == 1) return routePoints.first()

    // Calculate segments
    var totalDist = 0.0
    val dists = mutableListOf<Double>()
    for (i in 0 until routePoints.size - 1) {
        val d = routePoints[i].distanceToAsDouble(routePoints[i + 1])
        dists.add(d)
        totalDist += d
    }

    if (totalDist <= 0.0) return routePoints.first()
    val targetDist = clampedProg * totalDist
    var accumulated = 0.0

    for (i in dists.indices) {
        val segDist = dists[i]
        if (targetDist <= accumulated + segDist || i == dists.lastIndex) {
            val segProg = if (segDist > 0.0) ((targetDist - accumulated) / segDist).coerceIn(0.0, 1.0) else 0.0
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]
            val lat = p1.latitude + (p2.latitude - p1.latitude) * segProg
            val lon = p1.longitude + (p2.longitude - p1.longitude) * segProg
            return GeoPoint(lat, lon)
        }
        accumulated += segDist
    }

    return routePoints.last()
}

// =========================================================================
// 🎛️ FLOATING PROFESSIONAL MAP CONTROL DOCK
// =========================================================================

@Composable
fun MapFloatingControlDock(
    modifier: Modifier = Modifier,
    themeOption: AppThemeOption,
    playerHqCityId: String?,
    mapView: MapView?,
    isEnglish: Boolean,
    layersState: MapLayersState,
    onLayersChanged: (MapLayersState) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showLayersDialog by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "hq_pulse")
    val hqGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hqGlow"
    )

    Column(
        modifier = modifier
            .padding(end = 12.dp)
            .width(52.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. HQ Center Button
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val hqId = playerHqCityId ?: "istanbul"
                    val hqGeo = cityCoordinates[hqId] ?: GeoPoint(41.0082, 28.9784)
                    mapView?.controller?.apply {
                        setZoom(8.6)
                        animateTo(hqGeo)
                    }
                },
            shape = CircleShape,
            color = themeOption.surfaceColor.copy(alpha = 0.94f),
            border = BorderStroke(1.5.dp, ThemePositive.copy(alpha = hqGlowAlpha)),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.HomeWork,
                    contentDescription = tr("Merkez Depo", "HQ Center", isEnglish),
                    tint = ThemePositive,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Region Toggle Button (Anatolia vs World)
        var isWorldView by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isWorldView = !isWorldView
                    if (isWorldView) {
                        mapView?.controller?.apply {
                            setZoom(3.4)
                            animateTo(GeoPoint(30.0, 30.0))
                        }
                    } else {
                        mapView?.controller?.apply {
                            setZoom(6.4)
                            animateTo(GeoPoint(39.0, 35.0))
                        }
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, if (isWorldView) themeOption.secondaryColor else themeOption.borderColor),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isWorldView) Icons.Default.Public else Icons.Default.Map,
                    contentDescription = tr("Bölge Değiştir", "Toggle View", isEnglish),
                    tint = if (isWorldView) themeOption.secondaryColor else themeOption.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Zoom In (+)
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    mapView?.controller?.zoomIn()
                },
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, themeOption.borderColor),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = tr("Yakınlaştır", "Zoom In", isEnglish),
                    tint = themeOption.textPrimaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 4. Zoom Out (-)
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    mapView?.controller?.zoomOut()
                },
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, themeOption.borderColor),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = tr("Uzaklaştır", "Zoom Out", isEnglish),
                    tint = themeOption.textPrimaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 5. Map Layers Menu Button
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showLayersDialog = true
                },
            shape = RoundedCornerShape(12.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, themeOption.primaryColor.copy(alpha = 0.6f)),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = tr("Katmanlar", "Layers", isEnglish),
                    tint = themeOption.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    if (showLayersDialog) {
        AlertDialog(
            onDismissRequest = { showLayersDialog = false },
            containerColor = themeOption.surfaceColor,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = themeOption.primaryColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText(
                        tr("Harita Katmanları & Filtreler", "Map Layers & Overlays", isEnglish),
                        fontWeight = FontWeight.Bold,
                        color = themeOption.primaryColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LayerToggleRow(
                        title = tr("🚚 Canlı Lojistik Hatları & Araçlar", "🚚 Live Logistics Routes & Vehicles", isEnglish),
                        subtitle = tr("Yoldaki kamyon, gemi, uçak ve trenleri gösterir.", "Shows trucks, ships, planes and trains in transit.", isEnglish),
                        checked = layersState.showDeliveries,
                        onCheckedChange = { onLayersChanged(layersState.copy(showDeliveries = it)) },
                        themeOption = themeOption
                    )
                    LayerToggleRow(
                        title = tr("🏭 Fabrika & Üretim Tesisleri", "🏭 Factories & Production Facilities", isEnglish),
                        subtitle = tr("Şehirlerde kurulu tesis rozetlerini gösterir.", "Shows badges for owned industrial facilities.", isEnglish),
                        checked = layersState.showFacilities,
                        onCheckedChange = { onLayersChanged(layersState.copy(showFacilities = it)) },
                        themeOption = themeOption
                    )
                    LayerToggleRow(
                        title = tr("👑 Konsorsiyum Mega Projeleri", "👑 Consortium Mega Projects", isEnglish),
                        subtitle = tr("Dev şehir kalkınma projelerini gösterir.", "Highlights active consortium megaprojects.", isEnglish),
                        checked = layersState.showMegaProjects,
                        onCheckedChange = { onLayersChanged(layersState.copy(showMegaProjects = it)) },
                        themeOption = themeOption
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLayersDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeOption.primaryColor)
                ) {
                    CurrencyText(tr("Tamam", "Done", isEnglish), color = themeOption.surfaceColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun LayerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeOption: AppThemeOption
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CurrencyText(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = themeOption.textPrimaryColor)
            CurrencyText(text = subtitle, style = MaterialTheme.typography.labelSmall, color = themeOption.textSecondaryColor, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = themeOption.surfaceColor,
                checkedTrackColor = themeOption.primaryColor,
                uncheckedThumbColor = themeOption.textSecondaryColor,
                uncheckedTrackColor = themeOption.surfaceVariantColor
            )
        )
    }
}

// =========================================================================
// 📡 LIVE LOGISTICS & FLEET TRACKER RADAR
// =========================================================================

@Composable
fun LiveLogisticsRadarBar(
    modifier: Modifier = Modifier,
    themeOption: AppThemeOption,
    deliveries: List<DeliveryItem>,
    productions: List<ActiveProduction>,
    playerHqCityId: String?,
    cities: List<CityProfile>,
    isEnglish: Boolean,
    onFocusLocation: (GeoPoint) -> Unit,
    onOpenMarket: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    val totalTransits = deliveries.size + productions.size

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val radarPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "radarPulse"
    )

    Column(
        modifier = modifier
            .widthIn(max = 440.dp)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Collapsed / Main Compact Floating Pill
        Surface(
            modifier = Modifier
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                },
            shape = RoundedCornerShape(28.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.94f),
            border = BorderStroke(1.2.dp, if (totalTransits > 0) ThemePositive.copy(alpha = 0.8f) else themeOption.borderColor.copy(alpha = 0.6f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (totalTransits > 0) ThemePositive.copy(alpha = radarPulseAlpha) else themeOption.textSecondaryColor,
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (totalTransits > 0) ThemePositive else themeOption.textSecondaryColor,
                    modifier = Modifier.size(17.dp)
                )
                CurrencyText(
                    text = if (totalTransits > 0) {
                        tr("Lojistik Radarı ($totalTransits)", "Logistics Radar ($totalTransits)", isEnglish)
                    } else {
                        tr("Lojistik Radarı (0)", "Logistics Radar (0)", isEnglish)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (totalTransits > 0) themeOption.textPrimaryColor else themeOption.textSecondaryColor,
                    fontSize = 11.sp
                )

                if (totalTransits > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ThemePositive.copy(alpha = 0.18f)
                    ) {
                        CurrencyText(
                            text = "$totalTransits " + tr("SEFER", "TRIPS", isEnglish),
                            color = ThemePositive,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = themeOption.textSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Transit Details Drawer
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(18.dp),
                color = themeOption.surfaceColor.copy(alpha = 0.96f),
                border = BorderStroke(1.2.dp, themeOption.borderColor.copy(alpha = 0.7f)),
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(16.dp))
                            CurrencyText(
                                text = tr("AKTİF SEVKİYAT VE KONVOYLAR", "ACTIVE SHIPMENTS & CONVOYS", isEnglish),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isExpanded = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = themeOption.textSecondaryColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (totalTransits == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = themeOption.textSecondaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                            CurrencyText(
                                tr("Şu anda karayolu veya denizyolunda hareket eden sevkiyatınız bulunmuyor.", "No shipments currently moving across highways or sea lanes.", isEnglish),
                                style = MaterialTheme.typography.bodySmall,
                                color = themeOption.textSecondaryColor,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp
                            )
                            Button(
                                onClick = onOpenMarket,
                                colors = ButtonDefaults.buttonColors(containerColor = themeOption.primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeOption.surfaceColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(tr("Pazardan Sevkiyat Başlat", "Start Shipment from Market", isEnglish), color = themeOption.surfaceColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(deliveries, key = { it.id }) { delivery ->
                                val elapsed = (System.currentTimeMillis() - delivery.startTimeMs).coerceAtLeast(0L)
                                val prog = if (delivery.totalDurationMs > 0) (elapsed.toFloat() / delivery.totalDurationMs.toFloat()).coerceIn(0f, 1f) else 1f
                                val remainingSecs = (((1f - prog) * delivery.totalDurationMs) / 1000f).toInt().coerceAtLeast(0)
                                val timeStr = String.format("%02d:%02d", remainingSecs / 60, remainingSecs % 60)

                                val originCity = cities.find { it.id == delivery.originCityId }
                                val destCity = cities.find { it.id == delivery.destinationCityId }
                                val prod = Product.values().find { it.id == delivery.itemId }
                                val prodName = prod?.getDisplayName(isEnglish) ?: delivery.itemId.uppercase()

                                val isOriginCoastal = GeoRoutePlanner.coastalPorts.contains(delivery.originCityId)
                                val isDestCoastal = GeoRoutePlanner.coastalPorts.contains(delivery.destinationCityId)
                                val isOriginRailway = GeoRoutePlanner.railwayCities.contains(delivery.originCityId)
                                val isDestRailway = GeoRoutePlanner.railwayCities.contains(delivery.destinationCityId)
                                val isOriginGlobal = !GeoRoutePlanner.isTurkishCity(delivery.originCityId)
                                val isDestGlobal = !GeoRoutePlanner.isTurkishCity(delivery.destinationCityId)
                                val isGlobal = isOriginGlobal || isDestGlobal
                                val isMaritime = isGlobal && ((isOriginCoastal && isDestCoastal) || ((isOriginCoastal || isDestCoastal) && !GeoRoutePlanner.isAirHub(delivery.originCityId, delivery.destinationCityId)))
                                val isRail = !isMaritime && (isOriginRailway && isDestRailway)

                                val vehicleEmoji = when {
                                    isMaritime -> "🚢"
                                    isRail -> "🚂"
                                    isGlobal -> "✈️"
                                    else -> "🚚"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeOption.surfaceVariantColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CurrencyText(vehicleEmoji, fontSize = 16.sp)
                                            CurrencyText(
                                                text = "${originCity?.countryFlag ?: ""} ${originCity?.getDisplayName(isEnglish) ?: delivery.originCityId} ➔ ${destCity?.countryFlag ?: ""} ${destCity?.getDisplayName(isEnglish) ?: delivery.destinationCityId}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = themeOption.textPrimaryColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        CurrencyText(
                                            text = "${delivery.quantity}T $prodName • ${if (delivery.isOutboundSale) tr("Satış Sevkiyatı", "Sale Dispatch", isEnglish) else tr("Hammadde Alımı", "Material Supply", isEnglish)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = themeOption.textSecondaryColor,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            LinearProgressIndicator(
                                                progress = { prog },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = if (delivery.isOutboundSale) ThemePositive else themeOption.secondaryColor,
                                                trackColor = themeOption.borderColor
                                            )
                                            CurrencyText(
                                                text = "%${(prog * 100).toInt()} ($timeStr)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = themeOption.textPrimaryColor,
                                                fontFamily = RobotoMonoFontFamily,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Quick Focus Camera Button
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val currentPos = getDeliveryCurrentGeo(delivery.originCityId, delivery.destinationCityId, prog)
                                            if (currentPos != null) {
                                                onFocusLocation(currentPos)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(themeOption.primaryColor.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = tr("Konvoya Odaklan", "Focus Convoy", isEnglish),
                                            tint = themeOption.primaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            items(productions, key = { it.id }) { prod ->
                                val effectiveTarget = if (prod.targetCityId.isNotBlank()) prod.targetCityId else (playerHqCityId ?: "istanbul")
                                val elapsed = (System.currentTimeMillis() - prod.startTimeMs).coerceAtLeast(0L)
                                val prog = if (prod.totalDurationMs > 0) (elapsed.toFloat() / prod.totalDurationMs.toFloat()).coerceIn(0f, 1f) else 1f
                                val remainingSecs = (((1f - prog) * prod.totalDurationMs) / 1000f).toInt().coerceAtLeast(0)
                                val timeStr = String.format("%02d:%02d", remainingSecs / 60, remainingSecs % 60)

                                val originCity = cities.find { it.id == prod.cityId }
                                val destCity = cities.find { it.id == effectiveTarget }
                                val productObj = Product.values().find { it.id == prod.productId || it.facilityId == prod.productId }
                                val prodName = productObj?.getDisplayName(isEnglish) ?: prod.productId.uppercase()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeOption.surfaceVariantColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CurrencyText("🏭", fontSize = 16.sp)
                                            CurrencyText(
                                                text = "${originCity?.countryFlag ?: ""} ${originCity?.getDisplayName(isEnglish) ?: prod.cityId} ➔ 🏠 MERKEZ",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = themeOption.textPrimaryColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        CurrencyText(
                                            text = "${prod.quantity}T $prodName • " + tr("Tesis Üretim Sevkiyatı", "Factory Production Dispatch", isEnglish),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = themeOption.textSecondaryColor,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            LinearProgressIndicator(
                                                progress = { prog },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = themeOption.primaryColor,
                                                trackColor = themeOption.borderColor
                                            )
                                            CurrencyText(
                                                text = "%${(prog * 100).toInt()} ($timeStr)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = themeOption.textPrimaryColor,
                                                fontFamily = RobotoMonoFontFamily,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val currentPos = getDeliveryCurrentGeo(prod.cityId, effectiveTarget, prog)
                                            if (currentPos != null) {
                                                onFocusLocation(currentPos)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(themeOption.secondaryColor.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = tr("Üretime Odaklan", "Focus Production", isEnglish),
                                            tint = themeOption.secondaryColor,
                                            modifier = Modifier.size(20.dp)
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
}

// =========================================================================
// 🏛️ ENHANCED CITY DOSSIER HUD & TRADE RADAR SHEET
// =========================================================================

@Composable
fun EnhancedCityDossierSheet(
    modifier: Modifier = Modifier,
    city: CityProfile,
    isCurrentHq: Boolean,
    playerHqCityId: String?,
    cityBusinesses: List<BusinessEntity>,
    themeOption: AppThemeOption,
    context: Context,
    isEnglish: Boolean,
    arbitrageInfo: TradeArbitrageInfo?,
    onClose: () -> Unit,
    onTravel: () -> Unit,
    onOpenMarket: () -> Unit,
    onBuildFacility: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val distanceKm = remember(city.id, playerHqCityId) {
        if (playerHqCityId != null && playerHqCityId != city.id) {
            calculateDistanceKm(playerHqCityId, city.id)
        } else 0
    }
    val transitTimeStr = remember(distanceKm) {
        if (distanceKm > 0) calculateTransitTimeFormatted(distanceKm) else ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 92.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = themeOption.surfaceColor.copy(alpha = 0.95f),
            borderWidth = 1.5.dp,
            borderColor = if (isCurrentHq) ThemePositive.copy(alpha = 0.85f) else themeOption.primaryColor.copy(alpha = 0.6f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyText(city.countryFlag, fontSize = 24.sp)
                            CurrencyText(
                                text = city.getDisplayName(context).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = themeOption.primaryColor,
                                letterSpacing = 1.5.sp
                            )
                            if (isCurrentHq) {
                                Surface(
                                    color = ThemePositive.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, ThemePositive)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText(
                                            text = tr("MERKEZ", "HQ", isEnglish),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThemePositive,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyText(
                                text = "${city.getRegionDisplayName(context)} • ${tr("Çarpan:", "Multiplier:", isEnglish)} ${city.economicMultiplier}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = themeOption.textSecondaryColor,
                                fontFamily = RobotoMonoFontFamily
                            )

                            if (distanceKm > 0) {
                                Surface(
                                    color = themeOption.surfaceVariantColor,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    CurrencyText(
                                        text = "📏 $distanceKm km • ⏱️ $transitTimeStr",
                                        color = themeOption.secondaryColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = tr("Kapat", "Close", isEnglish), tint = themeOption.textPrimaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Logistics & Connectivity Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCoastal = GeoRoutePlanner.coastalPorts.contains(city.id)
                    val isRail = GeoRoutePlanner.railwayCities.contains(city.id)
                    val isAir = GeoRoutePlanner.airHubCities.contains(city.id)

                    InfrastructureBadge(icon = Icons.Default.AltRoute, label = tr("Karayolu", "Highway", isEnglish), active = true, themeOption = themeOption)
                    InfrastructureBadge(icon = Icons.Default.DirectionsTransit, label = tr("Demiryolu", "Railway", isEnglish), active = isRail, themeOption = themeOption)
                    InfrastructureBadge(icon = Icons.Default.DirectionsBoat, label = tr("Liman", "Port", isEnglish), active = isCoastal, themeOption = themeOption)
                    InfrastructureBadge(icon = Icons.Default.Flight, label = tr("Havalimanı", "Airport", isEnglish), active = isAir, themeOption = themeOption)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Arbitrage / Economic Opportunity Card
                if (arbitrageInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = themeOption.secondaryColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, themeOption.secondaryColor.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(arbitrageInfo.productIcon, contentDescription = null, tint = themeOption.secondaryColor, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    CurrencyText(
                                        text = "💡 " + tr("TİCARET ARBİTRAJI:", "TRADE ARBITRAGE:", isEnglish),
                                        fontWeight = FontWeight.Bold,
                                        color = themeOption.secondaryColor,
                                        fontSize = 10.sp
                                    )
                                    CurrencyText(
                                        text = "+%${arbitrageInfo.profitMarginPercent} " + tr("KÂR", "PROFIT", isEnglish),
                                        fontWeight = FontWeight.Black,
                                        color = ThemePositive,
                                        fontSize = 10.sp
                                    )
                                }
                                CurrencyText(
                                    text = arbitrageInfo.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = themeOption.textPrimaryColor,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Quick Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = if (isCurrentHq) Icons.Default.CheckCircle else Icons.Default.DirectionsCar,
                        title = if (isCurrentHq) tr("Mevcut", "Current", isEnglish) else tr("Seyahat Et", "Travel", isEnglish),
                        subtitle = if (isCurrentHq) tr("Merkez", "HQ", isEnglish) else "10 💎",
                        accentColor = if (isCurrentHq) ThemePositive else themeOption.secondaryColor,
                        isHighlighted = isCurrentHq,
                        modifier = Modifier.weight(1f),
                        onClick = onTravel
                    )

                    QuickActionButton(
                        icon = Icons.Default.Storefront,
                        title = tr("Yerel Pazar", "Local Market", isEnglish),
                        subtitle = tr("Borsa & Fiyat", "Exchange & Prices", isEnglish),
                        accentColor = themeOption.primaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMarket
                    )

                    QuickActionButton(
                        icon = Icons.Default.AddBusiness,
                        title = tr("TESİS KUR", "BUILD FACILITY", isEnglish),
                        subtitle = tr("Üretim", "Production", isEnglish),
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        onClick = onBuildFacility
                    )
                }

                // Key Products & Logistics Network
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencyText(tr("ÖNE ÇIKAN ÜRÜNLER", "KEY PRODUCTS", isEnglish), style = MaterialTheme.typography.labelSmall, color = themeOption.secondaryColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        CurrencyText(city.getPrimaryProductsDisplay().joinToString(", "), style = MaterialTheme.typography.bodySmall, color = themeOption.textPrimaryColor, fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencyText(tr("LOJİSTİK AĞI", "LOGISTICS NETWORK", isEnglish), style = MaterialTheme.typography.labelSmall, color = themeOption.secondaryColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        CurrencyText(city.getLogisticsDisplay().joinToString(", "), style = MaterialTheme.typography.bodySmall, color = themeOption.textPrimaryColor, fontSize = 11.sp)
                    }
                }

                // Owned Businesses List in City
                if (cityBusinesses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CurrencyText(
                        "${tr("BU ŞEHİRDEKİ TESİSLERİM", "MY FACILITIES IN THIS CITY", isEnglish)} (${cityBusinesses.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeOption.secondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cityBusinesses.forEach { business ->
                            val prod = Product.values().find { it.facilityId == business.type }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeOption.surfaceVariantColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = prod?.icon ?: Icons.Default.Business,
                                        contentDescription = null,
                                        tint = if (prod != null) Color(prod.colorTint) else themeOption.primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    CurrencyText(
                                        text = prod?.getFacilityName(isEnglish)?.uppercase() ?: business.type,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = themeOption.textPrimaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Surface(
                                    color = themeOption.primaryColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    CurrencyText(
                                        text = "LVL ${business.level}",
                                        color = themeOption.primaryColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
fun InfrastructureBadge(
    icon: ImageVector,
    label: String,
    active: Boolean,
    themeOption: AppThemeOption
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (active) themeOption.surfaceVariantColor else themeOption.surfaceVariantColor.copy(alpha = 0.35f),
        border = BorderStroke(0.8.dp, if (active) themeOption.primaryColor.copy(alpha = 0.5f) else themeOption.borderColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) themeOption.primaryColor else themeOption.textSecondaryColor.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
            CurrencyText(
                text = label,
                color = if (active) themeOption.textPrimaryColor else themeOption.textSecondaryColor.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
