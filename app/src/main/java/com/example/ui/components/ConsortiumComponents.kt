package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConsortiumSupplierSlot
import com.example.data.MegaProject
import com.example.data.MegaProjectStage
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1. İZOMETRİK PROJE ŞANTİYESİ & RÖNTGEN / X-RAY GÖRÜNÜMÜ
 * Tamamlanan parçalar yeşil holo-çizgilerle, eksik parçalar yanıp sönen kırmızı/turuncu şematik hatlarla gösterilir.
 */
@Composable
fun ConsortiumAssemblyLineCanvas(
    project: MegaProject,
    modifier: Modifier = Modifier
) {
    var isXRayMode by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "assembly_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val warningFlashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warningFlash"
    )
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )

    val stage = project.currentStage
    val slots = project.slots
    val totalRequired = slots.sumOf { it.quantityRequired.toLong() }.coerceAtLeast(1L)
    val totalDelivered = slots.sumOf { it.quantityDelivered.toLong() }
    val progress = (totalDelivered.toFloat() / totalRequired.toFloat()).coerceIn(0f, 1f)
    val isProjectFinished = project.isAllStagesFinished || project.currentStage == MegaProjectStage.COMPLETED

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isXRayMode) Color(0xFF030A14) else Color(0xFF0A1224),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isXRayMode) Color(0xFF06B6D4) else Color(0xFF1E2D4A)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Isometric Canvas with X-Ray schematic rendering
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                val w = size.width
                val h = size.height
                val centerX = w * 0.5f
                val centerY = h * 0.52f

                if (isXRayMode) {
                    // High-tech CAD Matrix Grid Background
                    val cadGridColor = Color(0xFF06B6D4).copy(alpha = 0.12f)
                    val gridSpacing = 20f
                    var gx = 0f
                    while (gx < w) {
                        drawLine(color = cadGridColor, start = Offset(gx, 0f), end = Offset(gx, h), strokeWidth = 0.8f)
                        gx += gridSpacing
                    }
                    var gy = 0f
                    while (gy < h) {
                        drawLine(color = cadGridColor, start = Offset(0f, gy), end = Offset(w, gy), strokeWidth = 0.8f)
                        gy += gridSpacing
                    }

                    // Blueprint base platform
                    val xRayPlatform = Path().apply {
                        moveTo(centerX, centerY - 45f)
                        lineTo(centerX + 140f, centerY)
                        lineTo(centerX, centerY + 45f)
                        lineTo(centerX - 140f, centerY)
                        close()
                    }
                    drawPath(
                        path = xRayPlatform,
                        color = Color(0xFF083344).copy(alpha = 0.35f)
                    )
                    drawPath(
                        path = xRayPlatform,
                        color = Color(0xFF06B6D4).copy(alpha = 0.6f),
                        style = Stroke(width = 1.2f)
                    )

                    // Draw Project Schematic Sub-modules based on product type
                    val s0 = slots.getOrNull(0)
                    val s1 = slots.getOrNull(1)
                    val s2 = slots.getOrNull(2)
                    val s3 = slots.getOrNull(3)

                    val isS0Done = s0?.isFullyDelivered == true
                    val isS1Done = s1?.isFullyDelivered == true
                    val isS2Done = s2?.isFullyDelivered == true
                    val isS3Done = s3?.isFullyDelivered == true

                    val s0Color = if (isS0Done) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = warningFlashAlpha)
                    val s1Color = if (isS1Done) Color(0xFF10B981) else Color(0xFFF97316).copy(alpha = warningFlashAlpha)
                    val s2Color = if (isS2Done) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = warningFlashAlpha)
                    val s3Color = if (isS3Done) Color(0xFF10B981) else Color(0xFFF97316).copy(alpha = warningFlashAlpha)

                    when (project.targetProductId) {
                        "defense_frigate", "cargo_ship", "super_yacht" -> {
                            // TCG Anadolu / Ship Hull & Flight Deck Wireframe
                            // Hull / Keel (Slot 0)
                            val hullPath = Path().apply {
                                moveTo(centerX - 110f, centerY + 5f)
                                lineTo(centerX + 110f, centerY + 5f)
                                lineTo(centerX + 130f, centerY - 10f)
                                lineTo(centerX - 90f, centerY - 10f)
                                close()
                            }
                            drawPath(hullPath, color = s0Color.copy(alpha = 0.2f))
                            drawPath(hullPath, color = s0Color, style = Stroke(width = if (isS0Done) 2.2f else 1.8f))

                            // Propulsion / Engine Shaft (Slot 1)
                            drawCircle(color = s1Color, radius = 9f, center = Offset(centerX - 70f, centerY + 8f), style = Stroke(width = 2f))
                            drawCircle(color = s1Color, radius = 9f, center = Offset(centerX - 40f, centerY + 8f), style = Stroke(width = 2f))
                            drawLine(color = s1Color, start = Offset(centerX - 95f, centerY + 8f), end = Offset(centerX - 20f, centerY + 8f), strokeWidth = 2f)

                            // Superstructure / Bridge & Radar Mast (Slot 2)
                            val bridgePath = Path().apply {
                                moveTo(centerX + 10f, centerY - 10f)
                                lineTo(centerX + 50f, centerY - 10f)
                                lineTo(centerX + 40f, centerY - 40f)
                                lineTo(centerX + 20f, centerY - 40f)
                                close()
                            }
                            drawPath(bridgePath, color = s2Color.copy(alpha = 0.25f))
                            drawPath(bridgePath, color = s2Color, style = Stroke(width = 2f))
                            drawLine(color = s2Color, start = Offset(centerX + 30f, centerY - 40f), end = Offset(centerX + 30f, centerY - 55f), strokeWidth = 2.5f)

                            // Missile Pods / Avionics Flight Deck (Slot 3)
                            drawLine(color = s3Color, start = Offset(centerX - 80f, centerY - 10f), end = Offset(centerX, centerY - 10f), strokeWidth = 3f)
                            drawCircle(color = s3Color, radius = 5f, center = Offset(centerX - 60f, centerY - 20f), style = Stroke(width = 1.5f))
                            drawCircle(color = s3Color, radius = 5f, center = Offset(centerX - 30f, centerY - 20f), style = Stroke(width = 1.5f))
                        }
                        "bullet_train" -> {
                            // High Speed Train Aerodynamic Fuselage
                            // Nose & Chassis (Slot 0)
                            val trainBody = Path().apply {
                                moveTo(centerX - 100f, centerY + 8f)
                                lineTo(centerX + 80f, centerY + 8f)
                                lineTo(centerX + 120f, centerY - 8f)
                                lineTo(centerX - 100f, centerY - 8f)
                                close()
                            }
                            drawPath(trainBody, color = s0Color.copy(alpha = 0.2f))
                            drawPath(trainBody, color = s0Color, style = Stroke(width = 2.2f))

                            // Mag-Traction Bogie Wheels (Slot 1)
                            for (bx in listOf(-80f, -50f, 20f, 60f)) {
                                drawCircle(color = s1Color, radius = 6f, center = Offset(centerX + bx, centerY + 14f), style = Stroke(width = 2f))
                            }

                            // Aerodynamic Cockpit & Windshield (Slot 2)
                            val windshield = Path().apply {
                                moveTo(centerX + 60f, centerY - 8f)
                                lineTo(centerX + 105f, centerY - 8f)
                                lineTo(centerX + 85f, centerY - 22f)
                                lineTo(centerX + 50f, centerY - 22f)
                                close()
                            }
                            drawPath(windshield, color = s2Color, style = Stroke(width = 1.8f))

                            // Pantograph & Power Electronics (Slot 3)
                            val panto = Path().apply {
                                moveTo(centerX - 30f, centerY - 8f)
                                lineTo(centerX - 15f, centerY - 32f)
                                lineTo(centerX + 5f, centerY - 8f)
                            }
                            drawPath(panto, color = s3Color, style = Stroke(width = 2.2f))
                            drawLine(color = s3Color, start = Offset(centerX - 25f, centerY - 32f), end = Offset(centerX - 5f, centerY - 32f), strokeWidth = 2.5f)
                        }
                        "space_rocket" -> {
                            // Orbital Satellite & Heavy Launch Vehicle
                            // Booster Stage 1 (Slot 0)
                            val booster = Path().apply {
                                moveTo(centerX - 25f, centerY + 30f)
                                lineTo(centerX + 25f, centerY + 30f)
                                lineTo(centerX + 20f, centerY - 10f)
                                lineTo(centerX - 20f, centerY - 10f)
                                close()
                            }
                            drawPath(booster, color = s0Color, style = Stroke(width = 2f))

                            // Cryogenic Fuel Tank (Slot 1)
                            val tank = Path().apply {
                                moveTo(centerX - 18f, centerY - 10f)
                                lineTo(centerX + 18f, centerY - 10f)
                                lineTo(centerX + 14f, centerY - 45f)
                                lineTo(centerX - 14f, centerY - 45f)
                                close()
                            }
                            drawPath(tank, color = s1Color, style = Stroke(width = 2f))

                            // Fairing & Payload Avionics (Slot 2)
                            val noseCone = Path().apply {
                                moveTo(centerX - 14f, centerY - 45f)
                                lineTo(centerX + 14f, centerY - 45f)
                                lineTo(centerX, centerY - 72f)
                                close()
                            }
                            drawPath(noseCone, color = s2Color, style = Stroke(width = 2.2f))

                            // Solar Arrays & Thrusters (Slot 3)
                            drawLine(color = s3Color, start = Offset(centerX - 18f, centerY - 30f), end = Offset(centerX - 65f, centerY - 30f), strokeWidth = 2.5f)
                            drawLine(color = s3Color, start = Offset(centerX + 18f, centerY - 30f), end = Offset(centerX + 65f, centerY - 30f), strokeWidth = 2.5f)
                            drawRect(color = s3Color.copy(alpha = 0.3f), topLeft = Offset(centerX - 65f, centerY - 38f), size = Size(40f, 16f))
                            drawRect(color = s3Color.copy(alpha = 0.3f), topLeft = Offset(centerX + 25f, centerY - 38f), size = Size(40f, 16f))
                        }
                        else -> {
                            // Universal High-Tech Megastructure
                            val poly = Path().apply {
                                moveTo(centerX - 70f, centerY + 15f)
                                lineTo(centerX + 70f, centerY + 15f)
                                lineTo(centerX + 85f, centerY - 15f)
                                lineTo(centerX - 55f, centerY - 15f)
                                close()
                            }
                            drawPath(poly, color = s0Color, style = Stroke(width = 2f))
                            drawCircle(color = s1Color, radius = 16f, center = Offset(centerX, centerY), style = Stroke(width = 2f))
                            drawLine(color = s2Color, start = Offset(centerX - 50f, centerY - 15f), end = Offset(centerX - 50f, centerY - 45f), strokeWidth = 2.2f)
                            drawLine(color = s3Color, start = Offset(centerX + 50f, centerY - 15f), end = Offset(centerX + 50f, centerY - 45f), strokeWidth = 2.2f)
                        }
                    }

                    // Dynamic Laser Scan Line in X-Ray
                    val scanY = 10f + (h - 20f) * scanOffset
                    drawLine(
                        color = Color(0xFF06B6D4).copy(alpha = 0.85f),
                        start = Offset(0f, scanY),
                        end = Offset(w, scanY),
                        strokeWidth = 2.2f
                    )
                } else {
                    // Standard Isometric Construction Stage
                    val gridColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                    val step = 28f
                    for (i in -4..4) {
                        val xStart = centerX + (i * step)
                        val yStart = centerY - 35f + (i * 12f)
                        drawLine(
                            color = gridColor,
                            start = Offset(xStart - 100f, yStart + 35f),
                            end = Offset(xStart + 100f, yStart - 35f),
                            strokeWidth = 1f
                        )
                    }

                    val basePlatform = Path().apply {
                        moveTo(centerX, centerY - 38f)
                        lineTo(centerX + 110f, centerY)
                        lineTo(centerX, centerY + 38f)
                        lineTo(centerX - 110f, centerY)
                        close()
                    }
                    drawPath(path = basePlatform, color = Color(0xFF131D33))
                    drawPath(path = basePlatform, color = ThemeNeonCyan.copy(alpha = 0.35f), style = Stroke(width = 1.5f))

                    // Stage 1: Structural Chassis
                    if (stage.ordinal >= 0) {
                        val chassisPath = Path().apply {
                            moveTo(centerX, centerY - 24f)
                            lineTo(centerX + 65f, centerY - 2f)
                            lineTo(centerX, centerY + 20f)
                            lineTo(centerX - 65f, centerY - 2f)
                            close()
                        }
                        drawPath(path = chassisPath, color = Color(0xFF1E2D4A).copy(alpha = 0.8f))
                        drawPath(path = chassisPath, color = Color(0xFF38BDF8).copy(alpha = pulseAlpha), style = Stroke(width = 1.8f))
                    }

                    // Stage 2: Hardware Core
                    if (stage.ordinal >= 1) {
                        val coreRadius = 14f
                        drawCircle(color = ThemeGold.copy(alpha = 0.3f), radius = coreRadius + 4f, center = Offset(centerX, centerY - 2f))
                        drawCircle(color = ThemeGold, radius = coreRadius, center = Offset(centerX, centerY - 2f))
                        drawLine(color = ThemeGold.copy(alpha = pulseAlpha), start = Offset(centerX - 45f, centerY - 2f), end = Offset(centerX + 45f, centerY - 2f), strokeWidth = 2.5f)
                    }

                    // Stage 3: Outer Shell
                    if (stage.ordinal >= 2 || isProjectFinished) {
                        val domePath = Path().apply {
                            moveTo(centerX - 55f, centerY - 2f)
                            cubicTo(centerX - 40f, centerY - 45f, centerX + 40f, centerY - 45f, centerX + 55f, centerY - 2f)
                        }
                        drawPath(path = domePath, color = Color(0xFF34D399).copy(alpha = 0.85f), style = Stroke(width = 2.5f))
                    }

                    val scanY = (centerY - 35f) + (70f * scanOffset)
                    drawLine(
                        color = ThemeNeonCyan.copy(alpha = 0.7f),
                        start = Offset(centerX - 80f, scanY),
                        end = Offset(centerX + 80f, scanY),
                        strokeWidth = 2f
                    )
                }
            }

            // Top Status Bar Overlay with X-Ray Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isXRayMode) Color(0xFF06B6D4) else if (isProjectFinished) Color(0xFF34D399) else ThemeNeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = if (isXRayMode) "🔬 HOLO-RÖNTGEN / CAD GÖRÜNÜMÜ" else if (isProjectFinished) "MONTAJ TAMAMLANDI" else "ŞANTİYE: ${stage.titleTr}",
                        color = if (isXRayMode) Color(0xFF06B6D4) else Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isXRayMode) Color(0xFF06B6D4).copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isXRayMode) Color(0xFF06B6D4) else Color(0xFF334155)
                        ),
                        modifier = Modifier.clickable { isXRayMode = !isXRayMode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isXRayMode) Icons.Rounded.Visibility else Icons.Rounded.Biotech,
                                contentDescription = "X-Ray Mode",
                                tint = if (isXRayMode) Color(0xFF06B6D4) else Color.LightGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = if (isXRayMode) "X-RAY AKTİF" else "RÖNTGEN MODU",
                                color = if (isXRayMode) Color(0xFF06B6D4) else Color.LightGray,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThemeNeonCyan.copy(alpha = 0.15f)
                    ) {
                        val pct = (progress * 100).toInt()
                        CurrencyText(
                            text = "%$pct",
                            color = ThemeNeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom X-Ray Legend Bar when X-Ray is ON
            if (isXRayMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xFF030712).copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText("Holo-Yeşil: Teslim Edildi", color = Color(0xFF34D399), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText("Flaş Kırmızı: Eksik / Bekleniyor", color = Color(0xFFF87171), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 2. KONSORSİYUM ŞEREF TRİBÜNÜ & İTİBAR PODYUMU
 * En çok hammadde teslim eden ("Baş Tedarikçi" 🥇), en hızlı ikmal sağlayan ("Lojistik Kurtarıcısı" ⚡),
 * ve yüksek sermaye koyan ("Sanayi Devi" 🏛️) ortakları taçlandırır.
 */
@Composable
fun ConsortiumHonorPodium(
    project: MegaProject,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val podiumMembers = remember(project) { project.getPodiumStandings(currentUserId) }
    if (podiumMembers.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0D1527),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243354))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "Honor Podium",
                        tint = ThemeGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = "🏆 KONSORSİYUM ŞEREF TRİBÜNÜ & İTİBAR PODYUMU",
                        color = ThemeGold,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ThemeGold.copy(alpha = 0.15f)
                ) {
                    CurrencyText(
                        text = "${podiumMembers.size} Ortak",
                        color = ThemeGold,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1st, 2nd, 3rd Podium layout if >= 2 members
            if (podiumMembers.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val first = podiumMembers.firstOrNull()
                    val second = podiumMembers.getOrNull(1)
                    val third = podiumMembers.getOrNull(2)

                    // 2nd Place
                    if (second != null) {
                        PodiumColumn(
                            member = second,
                            isSelf = second.playerId == currentUserId,
                            pedestalHeight = 65.dp,
                            medalColor = Color(0xFFCBD5E1),
                            medalLabel = "2. LİK",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 1st Place (Highest)
                    if (first != null) {
                        PodiumColumn(
                            member = first,
                            isSelf = first.playerId == currentUserId,
                            pedestalHeight = 85.dp,
                            medalColor = Color(0xFFFFD700),
                            medalLabel = "👑 ŞAMPİYON",
                            modifier = Modifier.weight(1.15f)
                        )
                    }

                    // 3rd Place
                    if (third != null) {
                        PodiumColumn(
                            member = third,
                            isSelf = third.playerId == currentUserId,
                            pedestalHeight = 52.dp,
                            medalColor = Color(0xFFCD7F32),
                            medalLabel = "3. LÜK",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Single member list
                val member = podiumMembers.first()
                PodiumMemberRow(member = member, isSelf = member.playerId == currentUserId)
            }

            // Expanded List of Badges and details
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                podiumMembers.forEach { m ->
                    PodiumMemberRow(member = m, isSelf = m.playerId == currentUserId)
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    member: com.example.data.ConsortiumPodiumMember,
    isSelf: Boolean,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    medalColor: Color,
    medalLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badges icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            member.badges.take(2).forEach { b ->
                CurrencyText(text = b.badgeIcon, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        CurrencyText(
            text = if (isSelf) "Siz ⭐" else member.playerName,
            color = if (isSelf) ThemeGold else Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        CurrencyText(
            text = "${member.totalQuantityDelivered} Ton",
            color = Color(0xFF34D399),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Metallic Pedestal
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, medalColor.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CurrencyText(
                    text = medalLabel,
                    color = medalColor,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black
                )
                CurrencyText(
                    text = "%${member.sharePercentage.toInt()} Pay",
                    color = Color.LightGray,
                    fontSize = 7.sp
                )
            }
        }
    }
}

@Composable
private fun PodiumMemberRow(
    member: com.example.data.ConsortiumPodiumMember,
    isSelf: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelf) Color(0xFF1E293B) else Color(0xFF111C30),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelf) ThemeGold.copy(alpha = 0.5f) else Color(0xFF1E2D4A)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurrencyText(
                            text = "#${member.rank}",
                            color = if (member.rank == 1) ThemeGold else Color.LightGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            text = if (isSelf) "${member.playerName} (Siz)" else member.playerName,
                            color = if (isSelf) ThemeGold else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (member.isLeader) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ) {
                                CurrencyText("BAŞKAN", color = Color(0xFFF59E0B), fontSize = 6.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
                            }
                        }
                    }

                    // Honor Badges list
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        member.badges.forEach { b ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(b.colorHex).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(b.badgeIcon, fontSize = 7.5.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    CurrencyText(b.titleTr, color = Color(b.colorHex), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                CurrencyText(
                    text = "${member.totalQuantityDelivered} Ton Teslimat",
                    color = Color(0xFF34D399),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black
                )
                CurrencyText(
                    text = "%${member.sharePercentage.toInt()} Hisse Payı",
                    color = Color.LightGray,
                    fontSize = 7.5.sp
                )
            }
        }
    }
}

/**
 * 3. KONSORSİYUM ORTAK YÖNETİM KURULU & OYLAMA ODASI
 * Satış Kanalı (Pazar / Borsa / Devlet Savunma İhalesi) ve Kalite vs Hız stratejisi oylamaları.
 */
@Composable
fun ConsortiumBoardVotingCard(
    project: MegaProject,
    currentUserId: String,
    onSetStrategy: (com.example.data.ConsortiumProductionStrategy) -> Unit,
    onChangeSalesChannel: (com.example.data.ConsortiumSalesChannel) -> Unit,
    onVoteProposal: (proposalId: String, voteYes: Boolean) -> Unit,
    onCreateProposal: (titleTr: String, titleEn: String, descTr: String, descEn: String, type: com.example.data.ConsortiumProposalType, proposedValue: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewProposalDialog by remember { mutableStateOf(false) }
    val isLeader = project.leaderPlayerId == currentUserId || project.leaderPlayerId == "local_player"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25334E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = "Board Voting",
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = "🏛️ ORTAK YÖNETİM KURULU & STRATEJİ ODASI",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ThemeNeonCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { showNewProposalDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText("YENİ TASARI SUN", color = ThemeNeonCyan, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Current Active Sales Channel & Strategy Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sales Channel Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        CurrencyText("SATIŞ KANALI", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyText(
                            text = project.salesChannel.getTitle(),
                            color = when (project.salesChannel) {
                                com.example.data.ConsortiumSalesChannel.DEVLET_IHALE -> Color(0xFF38BDF8)
                                com.example.data.ConsortiumSalesChannel.PAZAR -> ThemeGold
                                com.example.data.ConsortiumSalesChannel.BORSA -> Color(0xFF34D399)
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Production Strategy Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        CurrencyText("MONTAJ MODU", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyText(
                            text = project.productionStrategy.getBadge(),
                            color = when (project.productionStrategy) {
                                com.example.data.ConsortiumProductionStrategy.HASSAS_KALITE -> Color(0xFFA855F7)
                                com.example.data.ConsortiumProductionStrategy.HIZLI_MONTAJ -> Color(0xFFF97316)
                                com.example.data.ConsortiumProductionStrategy.DENGELI -> Color(0xFF38BDF8)
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Strategy & Channel Buttons for Board Leader / President
            if (isLeader) {
                Spacer(modifier = Modifier.height(8.dp))
                CurrencyText("BAŞKAN HIZLI YÖNETİMİ (Kanal & Strateji Belirle):", color = ThemeGold, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.example.data.ConsortiumSalesChannel.values().forEach { ch ->
                        val isSelected = project.salesChannel == ch
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onChangeSalesChannel(ch) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                CurrencyText(
                                    text = when (ch) {
                                        com.example.data.ConsortiumSalesChannel.PAZAR -> "🛒 Pazar"
                                        com.example.data.ConsortiumSalesChannel.BORSA -> "📈 Borsa"
                                        com.example.data.ConsortiumSalesChannel.DEVLET_IHALE -> "🏛️ İhale"
                                    },
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.LightGray,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.example.data.ConsortiumProductionStrategy.values().forEach { st ->
                        val isSelected = project.productionStrategy == st
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetStrategy(st) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF7C3AED).copy(alpha = 0.35f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFA78BFA) else Color(0xFF334155))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                CurrencyText(
                                    text = when (st) {
                                        com.example.data.ConsortiumProductionStrategy.DENGELI -> "⚖️ Standart"
                                        com.example.data.ConsortiumProductionStrategy.HIZLI_MONTAJ -> "⚡ +%30 Hız"
                                        com.example.data.ConsortiumProductionStrategy.HASSAS_KALITE -> "💎 1.4x Kalite"
                                    },
                                    color = if (isSelected) Color(0xFFA78BFA) else Color.LightGray,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Active Proposals & Voting List
            Spacer(modifier = Modifier.height(10.dp))
            CurrencyText("AKTİF YÖNETİM KURULU OYLAMALARI:", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            if (project.boardProposals.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF111C30),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        CurrencyText("Henüz açık bir tasarı bulunmuyor. Yeni bir oylama başlatabilirsiniz.", color = Color.Gray, fontSize = 8.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    project.boardProposals.takeLast(4).reversed().forEach { proposal ->
                        ProposalCardItem(
                            proposal = proposal,
                            currentUserId = currentUserId,
                            onVote = { voteYes -> onVoteProposal(proposal.id, voteYes) }
                        )
                    }
                }
            }
        }
    }

    if (showNewProposalDialog) {
        NewProposalModalDialog(
            onDismiss = { showNewProposalDialog = false },
            onSubmit = { titleTr, titleEn, descTr, descEn, type, valStr ->
                onCreateProposal(titleTr, titleEn, descTr, descEn, type, valStr)
                showNewProposalDialog = false
            }
        )
    }
}

@Composable
private fun ProposalCardItem(
    proposal: com.example.data.ConsortiumBoardProposal,
    currentUserId: String,
    onVote: (Boolean) -> Unit
) {
    val userVote = proposal.votes[currentUserId]
    val yesCount = proposal.yesVotesCount
    val noCount = proposal.noVotesCount

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C30),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (proposal.isEnacted) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF1E2D4A)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyText(
                        text = proposal.titleTr,
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (proposal.isEnacted) Color(0xFF065F46) else Color(0xFF1E293B)
                ) {
                    CurrencyText(
                        text = if (proposal.isEnacted) "📜 YÜRÜRLÜKTE" else "🗳️ OYLAMADA",
                        color = if (proposal.isEnacted) Color(0xFF34D399) else ThemeGold,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            CurrencyText(
                text = "${proposal.descriptionTr} (Sunan: ${proposal.proposerPlayerName})",
                color = Color.LightGray,
                fontSize = 7.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyText("👍 $yesCount Evet", color = Color(0xFF34D399), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText("👎 $noCount Hayır", color = Color(0xFFF87171), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                if (!proposal.isEnacted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppButton(
                            onClick = { onVote(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userVote == true) Color(0xFF10B981) else Color(0xFF064E3B),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            CurrencyText("EVET", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }

                        AppButton(
                            onClick = { onVote(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userVote == false) Color(0xFFEF4444) else Color(0xFF7F1D1D),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            CurrencyText("HAYIR", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewProposalModalDialog(
    onDismiss: () -> Unit,
    onSubmit: (titleTr: String, titleEn: String, descTr: String, descEn: String, type: com.example.data.ConsortiumProposalType, proposedVal: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(com.example.data.ConsortiumProposalType.SALES_CHANNEL) }
    var selectedVal by remember { mutableStateOf("DEVLET_IHALE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            CurrencyText("🏛️ Yönetim Kurulu Tasarısı Sun", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyText("Tasarı Konusu:", color = Color.LightGray, fontSize = 8.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedType = com.example.data.ConsortiumProposalType.SALES_CHANNEL
                                selectedVal = "DEVLET_IHALE"
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedType == com.example.data.ConsortiumProposalType.SALES_CHANNEL) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedType == com.example.data.ConsortiumProposalType.SALES_CHANNEL) Color(0xFF38BDF8) else Color(0xFF334155))
                    ) {
                        Box(modifier = Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                            CurrencyText("Satış Kanalı", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedType = com.example.data.ConsortiumProposalType.PRODUCTION_STRATEGY
                                selectedVal = "HASSAS_KALITE"
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedType == com.example.data.ConsortiumProposalType.PRODUCTION_STRATEGY) Color(0xFF7C3AED).copy(alpha = 0.35f) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedType == com.example.data.ConsortiumProposalType.PRODUCTION_STRATEGY) Color(0xFFA78BFA) else Color(0xFF334155))
                    ) {
                        Box(modifier = Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                            CurrencyText("Kalite / Hız", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                CurrencyText("Önerilen Karar:", color = Color.LightGray, fontSize = 8.5.sp)

                if (selectedType == com.example.data.ConsortiumProposalType.SALES_CHANNEL) {
                    listOf(
                        "DEVLET_IHALE" to "Devlet Savunma İhalesi (%40 Teşvik, +100 Prestij)",
                        "PAZAR" to "Küresel Tüketici Pazarı (%25 Marka Primi)",
                        "BORSA" to "Spot Borsa Spot Satışı (Anında Likidite)"
                    ).forEach { (k, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVal = k }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedVal == k, onClick = { selectedVal = k })
                            CurrencyText(label, color = Color.White, fontSize = 8.sp)
                        }
                    }
                } else {
                    listOf(
                        "HASSAS_KALITE" to "Hassas Kalite (1.4x Marka / Borsa Çarpanı)",
                        "HIZLI_MONTAJ" to "Hızlı Montaj (+%30 Seri Hız)",
                        "DENGELI" to "Dengeli Standart Üretim"
                    ).forEach { (k, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVal = k }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedVal == k, onClick = { selectedVal = k })
                            CurrencyText(label, color = Color.White, fontSize = 8.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    val titleTr = if (selectedType == com.example.data.ConsortiumProposalType.SALES_CHANNEL) "Satış Kanalı Değişikliği: $selectedVal" else "Üretim Modu Değişikliği: $selectedVal"
                    val descTr = if (selectedType == com.example.data.ConsortiumProposalType.SALES_CHANNEL) "Üretilen tüm partilerin $selectedVal kanalına yönlendirilmesi." else "Montaj hattının $selectedVal protokolüne geçirilmesi."
                    onSubmit(titleTr, titleTr, descTr, descTr, selectedType, selectedVal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black)
            ) {
                CurrencyText("TASARIYI OYLAMAYA SUN", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("İptal", color = Color.Gray, fontSize = 8.5.sp)
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}

/**
 * 2. CANLI TEDARİKÇİ KATKI GRAFİĞİ (Donut Chart)
 * Hangi ortağın konsorsiyuma yüzde kaç katkı sağladığını ve açık slotları gösterir.
 */
@Composable
fun ConsortiumContributionDonutChart(
    project: MegaProject,
    currentUserId: String,
    userStockForProduct: (String) -> Int,
    onDeliverClick: (ConsortiumSupplierSlot) -> Unit,
    onOneTapDeliver: (ConsortiumSupplierSlot) -> Unit,
    onJoinSlot: (ConsortiumSupplierSlot) -> Unit,
    onNudgePartner: (ConsortiumSupplierSlot) -> Unit,
    onBroadcastSos: (ConsortiumSupplierSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val slots = project.slots
    val totalRequired = slots.sumOf { it.quantityRequired.toLong() }.coerceAtLeast(1L)
    val totalDelivered = slots.sumOf { it.quantityDelivered.toLong() }
    val isFinished = project.isAllStagesFinished || project.currentStage == MegaProjectStage.COMPLETED

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = "📊 ORTAKLAR KATKI DAĞILIMI & HİSSE RADARI",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )

                CurrencyText(
                    text = "$totalDelivered / $totalRequired Ton",
                    color = ThemeGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Visual
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val strokeW = 14f
                        val palette = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFFFBBF24),
                            Color(0xFF34D399),
                            Color(0xFFA78BFA),
                            Color(0xFFF43F5E),
                            Color(0xFF64748B)
                        )

                        var startAngle = -90f
                        if (totalDelivered == 0L) {
                            // Empty track
                            drawArc(
                                color = Color(0xFF1E293B),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeW)
                            )
                        } else {
                            slots.forEachIndexed { idx, s ->
                                if (s.quantityDelivered > 0) {
                                    val sweep = (s.quantityDelivered.toFloat() / totalRequired.toFloat()) * 360f
                                    val arcColor = palette[idx % palette.size]
                                    drawArc(
                                        color = arcColor,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeW)
                                    )
                                    startAngle += sweep
                                }
                            }
                            // Remaining gap
                            val remainingSweep = ((totalRequired - totalDelivered).toFloat() / totalRequired.toFloat()) * 360f
                            if (remainingSweep > 0.5f) {
                                drawArc(
                                    color = Color(0xFF1E293B),
                                    startAngle = startAngle,
                                    sweepAngle = remainingSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeW)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val percent = if (totalRequired > 0) (totalDelivered * 100 / totalRequired).toInt() else 0
                        CurrencyText(
                            text = "%$percent",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        CurrencyText(
                            text = "DOLULUK",
                            color = Color.Gray,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Breakdown Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val palette = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFFFBBF24),
                        Color(0xFF34D399),
                        Color(0xFFA78BFA),
                        Color(0xFFF43F5E),
                        Color(0xFF64748B)
                    )

                    slots.take(4).forEachIndexed { idx, s ->
                        val pColor = palette[idx % palette.size]
                        val partnerName = s.assignedPartnerName ?: "Açık Slot"
                        val userStock = userStockForProduct(s.productId)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                CurrencyText(
                                    text = s.productName,
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            CurrencyText(
                                text = "${s.quantityDelivered}/${s.quantityRequired} T",
                                color = if (s.isFullyDelivered) Color(0xFF34D399) else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (slots.size > 4) {
                        CurrencyText(
                            text = "+${slots.size - 4} diğer parça...",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. TEDARİKÇİ ÇAĞRI TAHTASI & TELSİZ BİLDİRİM BARI (Consortium Radio SOS)
 * Eksik parça olan slotlara tüm konsorsiyum ortaklarına telsiz çağrısı yayınlama.
 */
@Composable
fun ConsortiumRadioSosBroadcastBar(
    project: MegaProject,
    onBroadcastSos: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val incompleteSlots = project.slots.filter { !it.isFullyDelivered }
    val isFinished = project.isAllStagesFinished || project.currentStage == MegaProjectStage.COMPLETED
    if (incompleteSlots.isEmpty() || isFinished) return

    val bottleneckSlot = incompleteSlots.find { it.isBottleneckWarning } ?: incompleteSlots.first()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF7F1D1D).copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Podcasts,
                    contentDescription = "Radio SOS",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    CurrencyText(
                        text = "TELSİZ ÇAĞRI PANOSU",
                        color = Color(0xFFF87171),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black
                    )
                    CurrencyText(
                        text = "Eksik: ${bottleneckSlot.productName} (${bottleneckSlot.remainingQuantity} Ton aranıyor)",
                        color = Color.White,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AppButton(
                onClick = { onBroadcastSos(bottleneckSlot.slotId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                CurrencyText("📻 ÇAĞRI YAP", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/**
 * 4. HIZLI SEVKİYAT ROZETİ (One-Tap Warehouse Sync)
 * Oyuncunun şirket deposunda parça varsa tek tıkla doğrudan konsorsiyuma aktarılmasını sağlar.
 */
@Composable
fun OneTapWarehouseSyncBadge(
    userStock: Int,
    remainingNeeded: Int,
    onOneTapSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canDeliver = userStock > 0 && remainingNeeded > 0
    val transferAmount = minOf(userStock, remainingNeeded)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (canDeliver) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (canDeliver) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF334155)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (canDeliver) Icons.Rounded.Inventory2 else Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = if (canDeliver) Color(0xFF34D399) else Color.Gray,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    CurrencyText(
                        text = if (canDeliver) "Şirket Deponuzda $userStock Ton Hazır" else "Şirket Deponuzda Stok Yok ($userStock Ton)",
                        color = if (canDeliver) Color(0xFF34D399) else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    CurrencyText(
                        text = "Kalan İhtiyaç: $remainingNeeded Ton",
                        color = Color.LightGray,
                        fontSize = 8.sp
                    )
                }
            }

            if (canDeliver) {
                AppButton(
                    onClick = onOneTapSync,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    CurrencyText("⚡ $transferAmount T Aktar", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
