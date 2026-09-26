package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.ProductTier
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced High-Tech Live Facility Visual Component:
 * - Dynamic smoking industrial chimneys with fluid smoke drift & curl
 * - Hydraulic stamping piston / press mechanism with impact shockwave & sparks
 * - Counter-rotating mechanical gear train with level-adaptive speed & planetary turbines
 * - Laser / Welding arc particle generator with intermittent flash bursts
 * - Animated assembly conveyor with product cargo crates & scanning laser beam
 * - Roof hazard strobe / beacon light with rotating warning beam
 * - Industry-specific visual themes (Heavy Industry, High-Tech, Bio/Agri, Petrochem)
 */
@Composable
fun AnimatedFacilityVisual(
    product: Product,
    level: Int = 1,
    isProducing: Boolean = true,
    height: Dp = 130.dp,
    modifier: Modifier = Modifier
) {
    val brandColor = Color(product.colorTint)
    val clampedLevel = level.coerceIn(1, 10)
    
    // Facility Type / Sector Classification for specialized animations & visuals
    val isHighTech = product.tier in listOf(ProductTier.TIER_3, ProductTier.TIER_4) || 
                     product.facilityId.contains("semiconductor") || product.facilityId.contains("telecom") || 
                     product.facilityId.contains("quantum") || product.facilityId.contains("ai") || product.facilityId.contains("battery")
    val isHeavyIndustry = product.facilityId.contains("steel") || product.facilityId.contains("mine") || 
                          product.facilityId.contains("quarry") || product.facilityId.contains("heavy") || product.facilityId.contains("defense")
    val isAgriBio = product.facilityId.contains("farm") || product.facilityId.contains("orchard") || 
                    product.facilityId.contains("plantation") || product.facilityId.contains("greenhouse")

    val isAnim = com.example.utils.HapticManager.isAnimationsEnabled

    // Master Animation Transition
    val infiniteTransition = rememberInfiniteTransition(label = "facility_master_engine")
    
    // 1. Chimney Smoke & Steam Plume Animation
    val smokePhaseRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProducing) 1700 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "smoke_phase"
    )
    val smokePhase = if (isAnim) smokePhaseRaw.value else 0f

    // 2. Primary Mechanical Gear Rotation
    val gearDuration = when {
        !isProducing -> 9000
        clampedLevel >= 6 -> 1000
        clampedLevel >= 3 -> 1600
        else -> 2400
    }
    val gearAngleRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(gearDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_angle"
    )
    val gearAngle = if (isAnim) gearAngleRaw.value else 0f

    // 3. Counter-rotating Secondary Gear
    val secondaryGearAngleRaw = infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween((gearDuration * 0.65f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sec_gear_angle"
    )
    val secondaryGearAngle = if (isAnim) secondaryGearAngleRaw.value else 0f

    // 4. Hydraulic Stamping Piston Animation (Down-Stroke -> Hammer -> Up-Stroke)
    val pistonCycleRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProducing) 1200 else 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "piston_cycle"
    )
    val pistonCycle = if (isAnim) pistonCycleRaw.value else 0f

    // 5. Welding Arc / Laser Spark Burst Phase
    val sparkPhaseRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProducing) 750 else 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spark_phase"
    )
    val sparkPhase = if (isAnim) sparkPhaseRaw.value else 0f

    // 6. Roof Safety Hazard Strobe / Beacon Light
    val beaconPhaseRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProducing) 900 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_phase"
    )
    val beaconPhase = if (isAnim) beaconPhaseRaw.value else 0f

    // 7. Assembly Conveyor Movement
    val conveyorPhaseRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProducing) 1900 else 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "conveyor_phase"
    )
    val conveyorPhase = if (isAnim) conveyorPhaseRaw.value else 0f

    // 8. Laser Quality Scanner Beam Movement
    val laserScannerPhaseRaw = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner_phase"
    )
    val laserScannerPhase = if (isAnim) laserScannerPhaseRaw.value else 0f

    // 9. Reactor Core / Smelter Pulse
    val corePulseRaw = infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )
    val corePulse = if (isAnim) corePulseRaw.value else 0.5f

    // Dynamic Designation Title & Accent based on level and sector
    val (levelTitle, sectorIcon) = when {
        clampedLevel >= 7 -> Pair("⚡ SİBER MEGA KOMPLEKS", Icons.Rounded.ElectricBolt)
        clampedLevel >= 5 -> Pair("🏭 AĞIR SANAYİ ENTEGRE", Icons.Rounded.Factory)
        clampedLevel >= 3 -> Pair("⚙️ OTOMASYON FABRİKASI", Icons.Rounded.PrecisionManufacturing)
        else -> Pair("🔧 HASSAS ATÖLYE TESİSİ", Icons.Rounded.Build)
    }

    val levelAccentColor = when {
        clampedLevel >= 7 -> ThemeGold
        clampedLevel >= 5 -> ThemeNeonCyan
        clampedLevel >= 3 -> Color(0xFF00E676)
        else -> Color(0xFF90CAF9)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF070C18),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    brandColor.copy(alpha = 0.75f),
                    levelAccentColor.copy(alpha = 0.85f),
                    brandColor.copy(alpha = 0.45f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // LAYER 1: Custom Industrial Animated Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // 1.1 Cyber Industrial Grid
                drawCyberGrid(w, h, brandColor)

                // 1.2 Multi-tiered Factory Silhouette with Windows, Core & Chimneys
                drawFactoryArchitecture(
                    w = w,
                    h = h,
                    level = clampedLevel,
                    brandColor = brandColor,
                    accentColor = levelAccentColor,
                    smokePhase = smokePhase,
                    beaconPhase = beaconPhase,
                    isProducing = isProducing,
                    corePulse = corePulse,
                    isHighTech = isHighTech,
                    isHeavyIndustry = isHeavyIndustry
                )

                // 1.3 Animated Hydraulic Stamping Piston Press & Welding Sparks
                if (isProducing) {
                    drawHydraulicPistonAndSparks(
                        w = w,
                        h = h,
                        pistonCycle = pistonCycle,
                        sparkPhase = sparkPhase,
                        accentColor = levelAccentColor,
                        brandColor = brandColor,
                        isHighTech = isHighTech
                    )
                }

                // 1.4 Bottom Conveyor Belt with Moving Cargo Packages & Laser Scanner
                drawConveyorBeltAndPackages(
                    w = w,
                    h = h,
                    conveyorPhase = conveyorPhase,
                    scannerPhase = laserScannerPhase,
                    brandColor = brandColor,
                    accentColor = levelAccentColor,
                    isProducing = isProducing
                )
            }

            // LAYER 2: Overlay Mechanical Gear Train in Right Corner
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Driving Gear
                Box(contentAlignment = Alignment.Center) {
                    // Gear Glow Ambient Halo
                    Box(
                        modifier = Modifier
                            .size(if (clampedLevel >= 4) 54.dp else 44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        levelAccentColor.copy(alpha = if (isProducing) corePulse * 0.4f else 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Ana Tahrik Çarkı",
                        tint = if (isProducing) levelAccentColor else Color.Gray,
                        modifier = Modifier
                            .size(if (clampedLevel >= 4) 48.dp else 40.dp)
                            .graphicsLayer { rotationZ = gearAngle }
                    )
                    // Central Metallic Axle Rivet
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, brandColor, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width((-6).dp))

                // Secondary Pinion Gear (Counter-rotating)
                if (clampedLevel >= 2) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Pinyon Dişli",
                            tint = if (isProducing) brandColor else Color.DarkGray,
                            modifier = Modifier
                                .size(if (clampedLevel >= 5) 36.dp else 28.dp)
                                .graphicsLayer { rotationZ = secondaryGearAngle }
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        )
                    }
                }

                // Tertiary Turbo Turbine (Unlocked at high tiers)
                if (clampedLevel >= 4) {
                    Spacer(modifier = Modifier.width((-5).dp))
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isHighTech) Icons.Rounded.Memory else Icons.Rounded.PrecisionManufacturing,
                            contentDescription = "Yüksek Hızlı Türbin",
                            tint = if (isProducing) ThemeGold else Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = gearAngle * 1.8f }
                        )
                    }
                }
            }

            // LAYER 3: Top-Left Heads-Up Badge: Level Designation & Status
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF060B16).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, levelAccentColor.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isProducing) ThemePositive else Color.Gray)
                        )
                        Text(
                            text = "SEVİYE $clampedLevel",
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = levelAccentColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0A0F1D).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = sectorIcon,
                            contentDescription = null,
                            tint = levelAccentColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = levelTitle,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // LAYER 4: Bottom-Left Real-time Performance HUD
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF050914).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, brandColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProducing) Icons.Rounded.Speed else Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isProducing) ThemeNeonCyan else Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isProducing) "ÇALIŞMA HIZI: %${100 + (clampedLevel - 1) * 30} • ${120 + clampedLevel * 25} RPM" else "TESİS BEKLEMEDE",
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = if (isProducing) ThemeNeonCyan else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws cyber background mesh
 */
private fun DrawScope.drawCyberGrid(w: Float, h: Float, brandColor: Color) {
    val step = 18.dp.toPx()
    var x = 0f
    while (x <= w) {
        drawLine(
            color = brandColor.copy(alpha = 0.04f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
        x += step
    }
    var y = 0f
    while (y <= h) {
        drawLine(
            color = brandColor.copy(alpha = 0.04f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
        y += step
    }
}

/**
 * Draws multi-layer factory building with glowing core, smoking chimneys and beacon strobe
 */
private fun DrawScope.drawFactoryArchitecture(
    w: Float,
    h: Float,
    level: Int,
    brandColor: Color,
    accentColor: Color,
    smokePhase: Float,
    beaconPhase: Float,
    isProducing: Boolean,
    corePulse: Float,
    isHighTech: Boolean,
    isHeavyIndustry: Boolean
) {
    val groundY = h - 22.dp.toPx()
    val factoryLeft = 14.dp.toPx()
    val factoryWidth = (w * 0.44f).coerceAtMost(210.dp.toPx())
    val factoryTop = groundY - 56.dp.toPx()

    // 1. Factory Main Hull
    drawRoundRect(
        color = Color(0xFF111929),
        topLeft = Offset(factoryLeft, factoryTop),
        size = Size(factoryWidth, groundY - factoryTop),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = brandColor.copy(alpha = 0.5f),
        topLeft = Offset(factoryLeft, factoryTop),
        size = Size(factoryWidth, groundY - factoryTop),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.5f)
    )

    // 2. Industrial Sawtooth Gable Roof
    val gablePath = Path().apply {
        val gableWidth = factoryWidth / 3f
        for (i in 0..2) {
            val gx = factoryLeft + i * gableWidth
            moveTo(gx, factoryTop)
            lineTo(gx + gableWidth * 0.62f, factoryTop - 12.dp.toPx())
            lineTo(gx + gableWidth, factoryTop)
        }
    }
    drawPath(path = gablePath, color = Color(0xFF1B2436))
    drawPath(path = gablePath, color = accentColor.copy(alpha = 0.6f), style = Stroke(width = 1f))

    // 3. Roof Safety Warning Beacon (Amber/Neon Flashing Strobe)
    val beaconX = factoryLeft + factoryWidth * 0.5f
    val beaconY = factoryTop - 13.dp.toPx()
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(beaconX - 3.dp.toPx(), beaconY),
        size = Size(6.dp.toPx(), 4.dp.toPx())
    )
    val beaconAlpha = if (isProducing) (sin(beaconPhase * Math.PI.toFloat() * 2f).coerceAtLeast(0f) * 0.85f + 0.15f) else 0.2f
    drawCircle(
        color = Color(0xFFFF9100).copy(alpha = beaconAlpha),
        radius = 4.dp.toPx(),
        center = Offset(beaconX, beaconY)
    )
    if (isProducing && beaconAlpha > 0.5f) {
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = beaconAlpha * 0.35f),
            radius = 12.dp.toPx(),
            center = Offset(beaconX, beaconY)
        )
    }

    // 4. Illuminated Factory Windows & Power Core
    val windowCols = 3
    val windowRows = 2
    val winW = 10.dp.toPx()
    val winH = 8.dp.toPx()
    for (row in 0 until windowRows) {
        for (col in 0 until windowCols) {
            val wx = factoryLeft + 8.dp.toPx() + col * (winW + 7.dp.toPx())
            val wy = factoryTop + 14.dp.toPx() + row * (winH + 8.dp.toPx())
            val isWindowLit = isProducing && ((col + row + level) % 2 == 0 || corePulse > 0.6f)
            
            drawRoundRect(
                color = if (isWindowLit) accentColor.copy(alpha = if (isProducing) corePulse * 0.8f else 0.25f) else Color(0xFF0F172A),
                topLeft = Offset(wx, wy),
                size = Size(winW, winH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }

    // 5. Smelting Furnace / High-Tech Reactor Core Portal on Right Side of Factory
    val coreW = 16.dp.toPx()
    val coreH = 24.dp.toPx()
    val coreX = factoryLeft + factoryWidth - coreW - 8.dp.toPx()
    val coreY = factoryTop + 14.dp.toPx()
    
    drawRoundRect(
        color = Color(0xFF080D1A),
        topLeft = Offset(coreX, coreY),
        size = Size(coreW, coreH),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    val coreColor = if (isHighTech) ThemeNeonCyan else if (isHeavyIndustry) Color(0xFFFF5722) else ThemeGold
    drawCircle(
        color = coreColor.copy(alpha = if (isProducing) corePulse * 0.9f else 0.3f),
        radius = coreW * 0.4f,
        center = Offset(coreX + coreW / 2f, coreY + coreH / 2f)
    )

    // 6. Industrial Smoking Chimneys
    val chimneyCount = when {
        level >= 5 -> 3
        level >= 3 -> 2
        else -> 1
    }
    val chimneyWidth = 8.dp.toPx()
    val chimneyHeight = 22.dp.toPx()
    for (c in 0 until chimneyCount) {
        val cx = factoryLeft + 12.dp.toPx() + c * 20.dp.toPx()
        val cy = factoryTop - chimneyHeight - 4.dp.toPx()

        // Chimney Base Pipe
        drawRect(
            color = Color(0xFF172033),
            topLeft = Offset(cx, cy),
            size = Size(chimneyWidth, chimneyHeight + 4.dp.toPx())
        )
        // Red/Gold Warning Stripe Ring on Top
        drawRect(
            color = if (c % 2 == 0) ThemeGold else Color(0xFFFF3D00),
            topLeft = Offset(cx, cy),
            size = Size(chimneyWidth, 3.5.dp.toPx())
        )
        drawRect(
            color = brandColor.copy(alpha = 0.8f),
            topLeft = Offset(cx, cy),
            size = Size(chimneyWidth, chimneyHeight + 4.dp.toPx()),
            style = Stroke(width = 1f)
        )

        // Fluid Multi-Layer Puffing Smoke Particles
        val puffCount = 5
        for (p in 0 until puffCount) {
            val pPhase = (smokePhase + (p.toFloat() / puffCount)) % 1.0f
            val smokeDist = pPhase * 38.dp.toPx()
            val smokeX = cx + (chimneyWidth / 2f) + (sin(pPhase * 4.5f + c * 1.5f) * 7.dp.toPx()) + (pPhase * 16.dp.toPx())
            val smokeY = cy - smokeDist
            val smokeRadius = (3.5.dp.toPx() + pPhase * 11.dp.toPx()) * (1f + (level - 1) * 0.12f)
            val alpha = ((1.0f - pPhase) * (if (isProducing) 0.65f else 0.2f)).coerceIn(0f, 1f)

            val smokeColor = when {
                isHighTech && level >= 5 -> ThemeNeonCyan.copy(alpha = alpha)
                isHeavyIndustry -> Color(0xFF9E9E9E).copy(alpha = alpha)
                else -> Color.LightGray.copy(alpha = alpha)
            }

            drawCircle(
                color = smokeColor,
                radius = smokeRadius,
                center = Offset(smokeX, smokeY)
            )
        }
    }
}

/**
 * Draws animated hydraulic stamping piston press with impact spark particles
 */
private fun DrawScope.drawHydraulicPistonAndSparks(
    w: Float,
    h: Float,
    pistonCycle: Float,
    sparkPhase: Float,
    accentColor: Color,
    brandColor: Color,
    isHighTech: Boolean
) {
    val groundY = h - 22.dp.toPx()
    val pistonX = (w * 0.52f).coerceAtMost(250.dp.toPx())
    val pistonTop = groundY - 48.dp.toPx()
    val pistonStroke = 14.dp.toPx()

    // Smooth mechanical impact curve: moves down fast, pauses at bottom for weld/press, rises up
    val strokeProg = sin(pistonCycle * Math.PI.toFloat())
    val rodExtension = strokeProg * pistonStroke

    // Hydraulic Housing Frame
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(pistonX - 6.dp.toPx(), pistonTop),
        size = Size(12.dp.toPx(), 18.dp.toPx())
    )
    drawRect(
        color = brandColor.copy(alpha = 0.7f),
        topLeft = Offset(pistonX - 6.dp.toPx(), pistonTop),
        size = Size(12.dp.toPx(), 18.dp.toPx()),
        style = Stroke(width = 1f)
    )

    // Moving Piston Rod
    val rodY = pistonTop + 16.dp.toPx() + rodExtension
    drawLine(
        color = Color.LightGray,
        start = Offset(pistonX, pistonTop + 14.dp.toPx()),
        end = Offset(pistonX, rodY),
        strokeWidth = 3.dp.toPx()
    )

    // Piston Stamping Head / Welder Tip
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(pistonX - 5.dp.toPx(), rodY),
        size = Size(10.dp.toPx(), 5.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )

    // Impact / Welding Spark Burst when piston is near bottom extension
    if (strokeProg > 0.7f) {
        val sparkOriginX = pistonX
        val sparkOriginY = rodY + 5.dp.toPx()
        val sparkCount = 6

        // Central Flash Glow
        drawCircle(
            color = if (isHighTech) ThemeNeonCyan else Color(0xFFFFD700),
            radius = 5.dp.toPx(),
            center = Offset(sparkOriginX, sparkOriginY)
        )

        for (s in 0 until sparkCount) {
            val angle = ((s.toFloat() / sparkCount) * Math.PI + Math.PI * 0.8).toFloat() + (sparkPhase * 0.5f)
            val dist = (sparkPhase * 16.dp.toPx()) * (0.8f + (s % 3) * 0.3f)
            val sx = sparkOriginX + cos(angle) * dist
            val sy = sparkOriginY + sin(angle) * dist + (sparkPhase * sparkPhase * 6.dp.toPx()) // Gravity drop
            val sparkAlpha = (1f - sparkPhase).coerceIn(0f, 1f)

            drawCircle(
                color = if (s % 2 == 0) Color(0xFFFF9100).copy(alpha = sparkAlpha) else Color(0xFFFFFF00).copy(alpha = sparkAlpha),
                radius = 1.5.dp.toPx(),
                center = Offset(sx, sy)
            )
        }
    }
}

/**
 * Draws animated bottom conveyor belt with cargo boxes and laser barcode scanner beam
 */
private fun DrawScope.drawConveyorBeltAndPackages(
    w: Float,
    h: Float,
    conveyorPhase: Float,
    scannerPhase: Float,
    brandColor: Color,
    accentColor: Color,
    isProducing: Boolean
) {
    val beltY = h - 14.dp.toPx()
    val beltHeight = 8.dp.toPx()

    // 1. Conveyor Belt Track Foundation
    drawRoundRect(
        color = Color(0xFF0C1322),
        topLeft = Offset(0f, beltY),
        size = Size(w, beltHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    drawLine(
        color = brandColor.copy(alpha = 0.65f),
        start = Offset(0f, beltY),
        end = Offset(w, beltY),
        strokeWidth = 1.5f
    )

    // 2. Moving Conveyor Rollers / Teeth
    val rollerSpacing = 16.dp.toPx()
    val offset = conveyorPhase * rollerSpacing
    var rx = offset
    while (rx < w) {
        drawLine(
            color = Color.Gray.copy(alpha = 0.45f),
            start = Offset(rx, beltY + 1f),
            end = Offset(rx, beltY + beltHeight - 1f),
            strokeWidth = 1.5f
        )
        rx += rollerSpacing
    }

    // 3. Sliding Product Cargo Packages
    val crateSpacing = 68.dp.toPx()
    val crateWidth = 13.dp.toPx()
    val crateHeight = 9.dp.toPx()
    val crateOffset = conveyorPhase * crateSpacing

    var cx = crateOffset
    while (cx < w + crateSpacing) {
        val actualX = cx - crateWidth
        if (actualX in 0f..w) {
            // Cargo Box Container
            drawRoundRect(
                color = Color(0xFF223047),
                topLeft = Offset(actualX, beltY - crateHeight),
                size = Size(crateWidth, crateHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
            drawRoundRect(
                color = if (isProducing) accentColor else Color.Gray,
                topLeft = Offset(actualX, beltY - crateHeight),
                size = Size(crateWidth, crateHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                style = Stroke(width = 1f)
            )
            // Color product band / badge
            drawLine(
                color = brandColor,
                start = Offset(actualX + crateWidth / 2f, beltY - crateHeight),
                end = Offset(actualX + crateWidth / 2f, beltY),
                strokeWidth = 1.5f
            )
        }
        cx += crateSpacing
    }

    // 4. Overhead Laser Quality Scanner Beam
    if (isProducing) {
        val scannerX = (w * 0.35f) + (scannerPhase * 30.dp.toPx())
        // Top sensor emitter dot
        drawCircle(
            color = ThemeNeonCyan,
            radius = 2.dp.toPx(),
            center = Offset(scannerX, beltY - crateHeight - 12.dp.toPx())
        )
        // Laser Scan Fan Beam
        drawLine(
            color = ThemeNeonCyan.copy(alpha = 0.6f),
            start = Offset(scannerX, beltY - crateHeight - 12.dp.toPx()),
            end = Offset(scannerX - 6.dp.toPx(), beltY),
            strokeWidth = 1f
        )
        drawLine(
            color = ThemeNeonCyan.copy(alpha = 0.6f),
            start = Offset(scannerX, beltY - crateHeight - 12.dp.toPx()),
            end = Offset(scannerX + 6.dp.toPx(), beltY),
            strokeWidth = 1f
        )
    }
}
