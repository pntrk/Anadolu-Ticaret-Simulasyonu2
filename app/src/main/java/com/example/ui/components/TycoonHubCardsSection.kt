package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BusinessEntity
import com.example.data.PlayerEntity
import com.example.ui.theme.*

@Composable
fun TycoonHubCardsSection(
    player: PlayerEntity?,
    businesses: List<BusinessEntity>,
    inventoryTotalCount: Int,
    inventoryMaxCapacity: Int,
    activeProductionsCount: Int,
    activeDeliveriesCount: Int,
    unlockedTechCount: Int = 2,
    hiredManagersCount: Int = 0,
    currentCityName: String = "Çanakkale",
    averageStaffEfficiency: Float = 1.0f,
    onNavigateToProduction: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToRd: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToMarket: () -> Unit,
    onNavigateToHr: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = LocalAppThemeOption.current
    val scrollState = rememberScrollState()

    val level = player?.level ?: 1
    val warehouseLevel = (inventoryMaxCapacity / 5000).coerceIn(1, 10)
    val usedRatio = if (inventoryMaxCapacity > 0) (inventoryTotalCount.toFloat() / inventoryMaxCapacity.toFloat()).coerceIn(0f, 1f) else 0f
    val usedPercent = (usedRatio * 100).toInt()

    val primaryFacility = businesses.firstOrNull()
    val facilityLevel = primaryFacility?.level ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.20f),
                    border = BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tab_headquarters),
                        contentDescription = tr("YÖNETİM", "MANAGEMENT"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                CurrencyText(
                    text = tr("YÖNETİM", "MANAGEMENT"),
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = RobotoMonoFontFamily,
                    letterSpacing = 0.8.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.8f),
                border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    CurrencyText(
                        text = tr("SİSTEM AKTİF", "SYSTEM ONLINE"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7),
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // HORIZONTAL SCROLLING TYCOON CAROUSEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CARD 1: PRODUCTION
            TycoonCard(
                title = tr("ÜRETİM", "PRODUCTION"),
                level = facilityLevel,
                maxLevel = 5,
                statusTitle = if (businesses.isNotEmpty()) tr("AKTİF ÜRETİM VE İŞLEME", "ACTIVE PROCESSING") else tr("YENİ FABRİKA BEKLENİYOR", "READY TO CONSTRUCT"),
                statusDetail = if (businesses.isNotEmpty()) "${businesses.size} " + tr("Tesis devrede", "Facilities online") else tr("Yeni hat kur", "Build first line"),
                progress = if (businesses.isNotEmpty()) 0.75f else 0.1f,
                progressColor = Color(0xFF10B981),
                accentColor = Color(0xFF00E5FF),
                buttonText = tr("TESİSİ YÖNET", "MANAGE PLANT"),
                illustrationType = BuildingArtType.FACTORY,
                imageRes = R.drawable.bg_menu_production,
                onButtonClick = onNavigateToProduction
            )

            // CARD 2: STORAGE / WAREHOUSE
            TycoonCard(
                title = tr("DEPO", "WAREHOUSE"),
                level = warehouseLevel,
                maxLevel = 5,
                statusTitle = tr("KAPASİTE DOLULUK", "CAPACITY USAGE"),
                statusDetail = "%$usedPercent ($inventoryTotalCount/${inventoryMaxCapacity}T)",
                progress = usedRatio,
                progressColor = Color(0xFF38BDF8),
                accentColor = Color(0xFF38BDF8),
                buttonText = tr("DEPOYU AÇ", "VIEW STORAGE"),
                illustrationType = BuildingArtType.WAREHOUSE,
                imageRes = R.drawable.bg_menu_warehouse,
                onButtonClick = onNavigateToInventory
            )

            // CARD 3: MAP
            TycoonCard(
                title = tr("HARİTA", "MAP"),
                level = level.coerceIn(1, 5),
                maxLevel = 5,
                statusTitle = tr("81 İL SEVKİYAT & ROTA", "81 CITIES & LOGISTICS"),
                statusDetail = "$currentCityName • " + tr("81 İl Aktif Rota", "81 Cities Active"),
                progress = 0.92f,
                progressColor = Color(0xFFEC4899),
                accentColor = Color(0xFFF43F5E),
                buttonText = tr("HARİTAYI AÇ", "OPEN MAP"),
                illustrationType = BuildingArtType.MAP,
                imageRes = R.drawable.bg_menu_map,
                onButtonClick = onNavigateToMap
            )

            // CARD 4: HUMAN RESOURCES
            val hrLevel = if (hiredManagersCount > 0) hiredManagersCount.coerceIn(1, 8) else 1
            val hrEffPercent = (averageStaffEfficiency * 100).toInt()
            TycoonCard(
                title = tr("İNSAN KAYNAKLARI", "HUMAN RESOURCES"),
                level = hrLevel,
                maxLevel = 8,
                statusTitle = tr("YÖNETİCİ & VERİMLİLİK", "STAFF & EFFICIENCY"),
                statusDetail = "$hiredManagersCount/8 " + tr("Müdür", "Managers") + " (%$hrEffPercent " + tr("Verim", "Eff.") + ")",
                progress = if (hiredManagersCount > 0) (hiredManagersCount / 8f).coerceIn(0.1f, 1f) else 0.15f,
                progressColor = Color(0xFF8B5CF6),
                accentColor = Color(0xFFA78BFA),
                buttonText = tr("KADROYU YÖNET", "MANAGE HR"),
                illustrationType = BuildingArtType.HR_HQ,
                imageRes = R.drawable.bg_menu_hr,
                onButtonClick = onNavigateToHr
            )
        }
    }
}

enum class BuildingArtType {
    FACTORY,
    WAREHOUSE,
    CARGO_JET,
    LAB,
    MAP,
    HR_HQ
}

@Composable
private fun TycoonCard(
    title: String,
    level: Int,
    maxLevel: Int,
    statusTitle: String,
    statusDetail: String,
    progress: Float,
    progressColor: Color,
    accentColor: Color,
    buttonText: String,
    illustrationType: BuildingArtType,
    imageRes: Int? = null,
    onButtonClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_border")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        modifier = Modifier
            .width(268.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.92f),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = borderGlow)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // TOP HEADER: ICON, TITLE & LEVEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (imageRes != null) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = accentColor.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    CurrencyText(
                        text = title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = "LV. $level",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = accentColor
                        )
                    }
                }
            }

            // LEVEL DOTS & INFO
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (i in 1..maxLevel) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (i <= level) accentColor else Color(0xFF334155))
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
            }

            // CENTER ARTWORK (Exact 3:2 Aspect Ratio - 1248x832 WebP Full Visual Display)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E293B).copy(alpha = 0.6f),
                                Color(0xFF0F172A).copy(alpha = 0.9f)
                            )
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    IsometricBuildingCanvas(type = illustrationType, accentColor = accentColor)
                }
            }

            // STATUS & PROGRESS BAR
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = statusTitle,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.4.sp
                )
                CurrencyText(
                    text = statusDetail,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    progressColor.copy(alpha = 0.7f),
                                    progressColor
                                )
                            )
                        )
                )
            }

            // ACTION BUTTON
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CurrencyText(
                        text = buttonText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IsometricBuildingCanvas(
    type: BuildingArtType,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + 10f

        // Isometric Ground Hologram Base Plate
        val basePath = Path().apply {
            moveTo(cx, cy - 24f)
            lineTo(cx + 65f, cy)
            lineTo(cx, cy + 24f)
            lineTo(cx - 65f, cy)
            close()
        }
        drawPath(
            path = basePath,
            color = Color(0xFF1E293B).copy(alpha = 0.8f)
        )
        drawPath(
            path = basePath,
            color = accentColor.copy(alpha = 0.5f),
            style = Stroke(width = 1.5f)
        )

        // Glowing center node
        drawCircle(
            color = accentColor.copy(alpha = 0.25f),
            radius = 18f,
            center = Offset(cx, cy)
        )

        when (type) {
            BuildingArtType.FACTORY -> drawIsometricFactory(cx, cy, accentColor)
            BuildingArtType.WAREHOUSE -> drawIsometricWarehouse(cx, cy, accentColor)
            BuildingArtType.CARGO_JET -> drawIsometricCargoJet(cx, cy, accentColor)
            BuildingArtType.LAB -> drawIsometricLab(cx, cy, accentColor)
            BuildingArtType.MAP -> drawIsometricMap(cx, cy, accentColor)
            BuildingArtType.HR_HQ -> drawIsometricHrHq(cx, cy, accentColor)
        }
    }
}

private fun DrawScope.drawIsometricMap(cx: Float, cy: Float, accent: Color) {
    // 3D Isometric Map Hologram Terrain Platform
    val terrainPath = Path().apply {
        moveTo(cx - 36f, cy - 8f)
        lineTo(cx - 15f, cy - 26f)
        lineTo(cx + 25f, cy - 24f)
        lineTo(cx + 38f, cy - 5f)
        lineTo(cx + 20f, cy + 10f)
        lineTo(cx - 24f, cy + 8f)
        close()
    }
    drawPath(terrainPath, Color(0xFF1E293B).copy(alpha = 0.9f))
    drawPath(terrainPath, accent.copy(alpha = 0.8f), style = Stroke(1.5f))

    // Grid radar rings & scan lines
    drawLine(
        color = accent.copy(alpha = 0.35f),
        start = Offset(cx - 30f, cy - 6f),
        end = Offset(cx + 30f, cy - 6f),
        strokeWidth = 1f
    )
    drawLine(
        color = accent.copy(alpha = 0.35f),
        start = Offset(cx - 10f, cy - 22f),
        end = Offset(cx + 5f, cy + 6f),
        strokeWidth = 1f
    )

    // Holographic City Hub Pins (Glowing Beacons)
    val pin1 = Offset(cx - 20f, cy - 14f) // West / Marmara
    val pin2 = Offset(cx, cy - 8f)        // Central / Anatolia
    val pin3 = Offset(cx + 22f, cy - 12f) // East

    // Connecting Trade Route Laser Lines
    val route1 = Path().apply {
        moveTo(pin1.x, pin1.y)
        quadraticTo(cx - 10f, cy - 24f, pin2.x, pin2.y)
        quadraticTo(cx + 12f, cy - 20f, pin3.x, pin3.y)
    }
    drawPath(route1, Color(0xFFFBBF24).copy(alpha = 0.85f), style = Stroke(1.5f))

    // City Pins
    for (pin in listOf(pin1, pin2, pin3)) {
        // Vertical Beacon Light
        drawLine(
            color = accent.copy(alpha = 0.9f),
            start = pin,
            end = Offset(pin.x, pin.y - 16f),
            strokeWidth = 1.5f
        )
        // Beacon Head
        drawCircle(accent, radius = 3.5f, center = Offset(pin.x, pin.y - 16f))
        drawCircle(Color.White, radius = 1.8f, center = Offset(pin.x, pin.y - 16f))
        // Ground Radar Pulse
        drawCircle(accent.copy(alpha = 0.3f), radius = 6f, center = pin)
    }
}

private fun DrawScope.drawIsometricHrHq(cx: Float, cy: Float, accent: Color) {
    // Executive Corporate Skyscraper Tower (Isometric Multi-Wing)
    // Left Wing
    val leftWing = Path().apply {
        moveTo(cx - 28f, cy + 2f)
        lineTo(cx, cy + 14f)
        lineTo(cx, cy - 25f)
        lineTo(cx - 28f, cy - 37f)
        close()
    }
    drawPath(leftWing, Color(0xFF1E1B4B)) // Deep Indigo
    drawPath(leftWing, accent.copy(alpha = 0.4f), style = Stroke(1f))

    // Right Wing
    val rightWing = Path().apply {
        moveTo(cx, cy + 14f)
        lineTo(cx + 28f, cy + 2f)
        lineTo(cx + 28f, cy - 37f)
        lineTo(cx, cy - 25f)
        close()
    }
    drawPath(rightWing, Color(0xFF312E81))
    drawPath(rightWing, accent.copy(alpha = 0.5f), style = Stroke(1f))

    // Center Skyscraper Top Tower
    val centerTowerLeft = Path().apply {
        moveTo(cx - 14f, cy - 20f)
        lineTo(cx, cy - 14f)
        lineTo(cx, cy - 50f)
        lineTo(cx - 14f, cy - 56f)
        close()
    }
    drawPath(centerTowerLeft, Color(0xFF4338CA))

    val centerTowerRight = Path().apply {
        moveTo(cx, cy - 14f)
        lineTo(cx + 14f, cy - 20f)
        lineTo(cx + 14f, cy - 56f)
        lineTo(cx, cy - 50f)
        close()
    }
    drawPath(centerTowerRight, Color(0xFF4F46E5))

    // Spire & Boardroom Hologram Ring
    drawLine(
        color = accent,
        start = Offset(cx, cy - 50f),
        end = Offset(cx, cy - 65f),
        strokeWidth = 2f
    )
    drawCircle(accent.copy(alpha = 0.5f), radius = 9f, center = Offset(cx, cy - 50f))
    drawCircle(accent, radius = 3.5f, center = Offset(cx, cy - 65f))
    drawCircle(Color.White, radius = 1.5f, center = Offset(cx, cy - 65f))

    // Glowing Office Windows
    for (i in 0..2) {
        val yOff = cy - 28f - (i * 8f)
        drawLine(Color(0xFF67E8F9).copy(alpha = 0.8f), Offset(cx - 10f, yOff), Offset(cx - 4f, yOff + 2.5f), strokeWidth = 1.5f)
        drawLine(Color(0xFF67E8F9).copy(alpha = 0.8f), Offset(cx + 4f, yOff + 2.5f), Offset(cx + 10f, yOff), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawIsometricFactory(cx: Float, cy: Float, accent: Color) {
    // Left & Right Building Blocks
    val block1 = Path().apply {
        moveTo(cx - 35f, cy - 10f)
        lineTo(cx, cy + 5f)
        lineTo(cx, cy - 25f)
        lineTo(cx - 35f, cy - 40f)
        close()
    }
    drawPath(block1, Color(0xFF334155))

    val block2 = Path().apply {
        moveTo(cx, cy + 5f)
        lineTo(cx + 35f, cy - 10f)
        lineTo(cx + 35f, cy - 40f)
        lineTo(cx, cy - 25f)
        close()
    }
    drawPath(block2, Color(0xFF475569))

    // Roof
    val roof = Path().apply {
        moveTo(cx, cy - 25f)
        lineTo(cx + 35f, cy - 40f)
        lineTo(cx, cy - 55f)
        lineTo(cx - 35f, cy - 40f)
        close()
    }
    drawPath(roof, Color(0xFF0F172A))
    drawPath(roof, accent.copy(alpha = 0.8f), style = Stroke(width = 1.2f))

    // Chimneys
    drawRect(Color(0xFF64748B), topLeft = Offset(cx - 20f, cy - 65f), size = Size(8f, 20f))
    drawRect(Color(0xFF64748B), topLeft = Offset(cx + 10f, cy - 65f), size = Size(8f, 20f))
    drawCircle(accent, radius = 4f, center = Offset(cx - 16f, cy - 65f))
    drawCircle(accent, radius = 4f, center = Offset(cx + 14f, cy - 65f))
}

private fun DrawScope.drawIsometricWarehouse(cx: Float, cy: Float, accent: Color) {
    // Dual warehouse hangar roofs
    for (offset in listOf(-20f, 15f)) {
        val ox = cx + offset
        val oy = cy - 15f
        val wRoof = Path().apply {
            moveTo(ox, oy - 20f)
            lineTo(ox + 22f, oy - 10f)
            lineTo(ox, oy)
            lineTo(ox - 22f, oy - 10f)
            close()
        }
        drawPath(wRoof, Color(0xFF1E293B))
        drawPath(wRoof, accent.copy(alpha = 0.7f), style = Stroke(1.2f))

        // Front wall
        val front = Path().apply {
            moveTo(ox - 22f, oy - 10f)
            lineTo(ox, oy)
            lineTo(ox, oy + 18f)
            lineTo(ox - 22f, oy + 8f)
            close()
        }
        drawPath(front, Color(0xFF334155))
    }

    // Cargo Pallet Crates
    drawRect(Color(0xFFF59E0B), topLeft = Offset(cx - 12f, cy + 4f), size = Size(8f, 8f))
    drawRect(Color(0xFFFBBF24), topLeft = Offset(cx + 4f, cy + 2f), size = Size(8f, 8f))
}

private fun DrawScope.drawIsometricCargoJet(cx: Float, cy: Float, accent: Color) {
    // Modern High-tech Cargo Aircraft
    val fuselage = Path().apply {
        moveTo(cx - 35f, cy - 35f)
        lineTo(cx + 35f, cy + 5f)
        lineTo(cx + 25f, cy + 12f)
        lineTo(cx - 45f, cy - 25f)
        close()
    }
    drawPath(fuselage, Color(0xFFE2E8F0))

    // Wings
    val leftWing = Path().apply {
        moveTo(cx - 10f, cy - 18f)
        lineTo(cx - 40f, cy + 10f)
        lineTo(cx - 28f, cy + 12f)
        lineTo(cx, cy - 10f)
        close()
    }
    drawPath(leftWing, Color(0xFF94A3B8))

    val rightWing = Path().apply {
        moveTo(cx - 5f, cy - 22f)
        lineTo(cx + 25f, cy - 45f)
        lineTo(cx + 30f, cy - 38f)
        lineTo(cx + 5f, cy - 12f)
        close()
    }
    drawPath(rightWing, Color(0xFF94A3B8))

    // Jet Engine Glow
    drawCircle(accent, radius = 5f, center = Offset(cx - 25f, cy + 6f))
    drawCircle(accent, radius = 5f, center = Offset(cx + 20f, cy - 35f))
}

private fun DrawScope.drawIsometricLab(cx: Float, cy: Float, accent: Color) {
    // High-tech curved lab dome with hologram core
    val labBody = Path().apply {
        moveTo(cx - 30f, cy - 15f)
        lineTo(cx, cy)
        lineTo(cx + 30f, cy - 15f)
        lineTo(cx, cy - 35f)
        close()
    }
    drawPath(labBody, Color(0xFF1E293B))
    drawPath(labBody, accent, style = Stroke(1.5f))

    // Upper Hologram Ring
    drawCircle(accent.copy(alpha = 0.4f), radius = 14f, center = Offset(cx, cy - 35f))
    drawCircle(accent, radius = 6f, center = Offset(cx, cy - 35f))

    // Vertical holographic beam
    drawLine(
        color = accent.copy(alpha = 0.7f),
        start = Offset(cx, cy),
        end = Offset(cx, cy - 45f),
        strokeWidth = 2f
    )
}
