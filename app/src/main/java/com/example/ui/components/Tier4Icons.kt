package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium realistic custom vector renderers for Tier 4 Mega Projects & Strategic Industries.
 * Accurately designed to match realistic 3D isometric product visuals.
 */
@Composable
fun Tier4PremiumIcon(
    productId: String,
    size: Dp = 64.dp,
    primaryColor: Color = Color(0xFF00E5FF)
) {
    val cornerRadius = (size * 0.18f).coerceIn(4.dp, 16.dp)
    
    Box(
        modifier = Modifier
            .size(size)
            .shadow(elevation = (size * 0.08f).coerceIn(1.dp, 8.dp), shape = RoundedCornerShape(cornerRadius), spotColor = primaryColor)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A0F1D),
                        Color(0xFF131D31),
                        Color(0xFF0D1525)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.25f),
                        Color(0xFF334155)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-tech subtle grid pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val step = w / 6f
            for (i in 1..5) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.05f),
                    start = Offset(0f, i * step),
                    end = Offset(w, i * step),
                    strokeWidth = 1f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.05f),
                    start = Offset(i * step, 0f),
                    end = Offset(i * step, h),
                    strokeWidth = 1f
                )
            }
        }
        
        // Ambient Core Glow
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        val canvasSize = size * 0.82f
        when (productId) {
            "ev" -> AdvancedEVCanvas(canvasSize, primaryColor)
            "space_rocket" -> AdvancedRocketCanvas(canvasSize, primaryColor)
            "bullet_train" -> AdvancedTrainCanvas(canvasSize, primaryColor)
            "smart_skyscraper" -> AdvancedSkyscraperCanvas(canvasSize, primaryColor)
            "autonomous_drone_swarm" -> AdvancedDroneSwarmCanvas(canvasSize, primaryColor)
            "uav" -> AdvancedUAVCanvas(canvasSize, primaryColor)
            "cargo_ship" -> AdvancedCargoShipCanvas(canvasSize, primaryColor)
            "super_yacht" -> AdvancedSuperYachtCanvas(canvasSize, primaryColor)
            "hydrogen_plant" -> AdvancedHydrogenPlantCanvas(canvasSize, primaryColor)
            "defense_frigate" -> AdvancedFrigateCanvas(canvasSize, primaryColor)
            "satellite" -> AdvancedSatelliteCanvas(canvasSize, primaryColor)
            "ai_datacenter" -> AdvancedDataCenterCanvas(canvasSize, primaryColor)
            "smart_grid" -> AdvancedSmartGridCanvas(canvasSize, primaryColor)
            "fusion_reactor_core" -> AdvancedFusionReactorCanvas(canvasSize, primaryColor)
            "quantum_supercomputer" -> AdvancedQuantumComputerCanvas(canvasSize, primaryColor)
            "hyperloop_capsule" -> AdvancedHyperloopCanvas(canvasSize, primaryColor)
            "luxury_aircraft_interior" -> AdvancedAircraftCanvas(canvasSize, primaryColor)
            else -> AdvancedFallbackCanvas(canvasSize, primaryColor)
        }
    }
}

// 1. Electric Vehicle (EV Crossover SUV)
@Composable
fun AdvancedEVCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        
        // Shadow underneath
        drawOval(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(w * 0.1f, h * 0.78f),
            size = Size(w * 0.8f, h * 0.12f)
        )

        // White Pearlescent Car Body (Isometric/3D perspective)
        val carBody = Path().apply {
            moveTo(w * 0.12f, h * 0.58f) // Nose bottom
            cubicTo(w * 0.15f, h * 0.42f, w * 0.28f, h * 0.38f, w * 0.42f, h * 0.38f) // Hood
            cubicTo(w * 0.52f, h * 0.22f, w * 0.72f, h * 0.22f, w * 0.82f, h * 0.38f) // Panoramic Roof
            cubicTo(w * 0.90f, h * 0.45f, w * 0.94f, h * 0.54f, w * 0.92f, h * 0.65f) // Tailgate
            lineTo(w * 0.85f, h * 0.74f) // Rear bumper
            lineTo(w * 0.16f, h * 0.74f) // Front bumper
            close()
        }
        drawPath(carBody, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
        drawPath(carBody, Color(0xFF94A3B8), style = Stroke(1.2f))

        // Glossy Panoramic Tinted Glass Roof & Windows
        val windowPath = Path().apply {
            moveTo(w * 0.44f, h * 0.38f)
            lineTo(w * 0.54f, h * 0.25f)
            lineTo(w * 0.74f, h * 0.25f)
            lineTo(w * 0.80f, h * 0.38f)
            close()
        }
        drawPath(windowPath, Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))))
        drawPath(windowPath, Color(0xFF38BDF8).copy(alpha = 0.6f), style = Stroke(1f))

        // Full-Width Cyan Cyber Lightbar (Headlights)
        val lightbar = Path().apply {
            moveTo(w * 0.14f, h * 0.56f)
            lineTo(w * 0.38f, h * 0.52f)
        }
        drawPath(lightbar, Color(0xFF38BDF8), style = Stroke(2.5f, cap = StrokeCap.Round))
        drawCircle(Color(0xFF00E5FF), radius = w * 0.035f, center = Offset(w * 0.17f, h * 0.56f))
        
        // Rear Red Taillight
        drawLine(Color(0xFFEF4444), Offset(w * 0.84f, h * 0.54f), Offset(w * 0.91f, h * 0.58f), strokeWidth = 2.5f)

        // Aero-Disc Alloy Wheels
        fun drawAeroWheel(cx: Float, cy: Float, r: Float) {
            drawCircle(Color(0xFF0F172A), radius = r, center = Offset(cx, cy))
            drawCircle(Color(0xFF64748B), radius = r * 0.75f, center = Offset(cx, cy), style = Stroke(1.5f))
            drawCircle(Color(0xFFE2E8F0), radius = r * 0.35f, center = Offset(cx, cy))
            for (i in 0..4) {
                val angle = Math.PI * 2 * i / 5
                val x = cx + (r * 0.65f * cos(angle)).toFloat()
                val y = cy + (r * 0.65f * sin(angle)).toFloat()
                drawLine(Color(0xFFE2E8F0), Offset(cx, cy), Offset(x, y), strokeWidth = 1.8f)
            }
        }
        drawAeroWheel(w * 0.30f, h * 0.72f, w * 0.12f)
        drawAeroWheel(w * 0.74f, h * 0.72f, w * 0.12f)
    }
}

// 2. Space Rocket
@Composable
fun AdvancedRocketCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Thrust exhaust flame / plasma
        val flame = Path().apply {
            moveTo(w * 0.42f, h * 0.86f)
            cubicTo(w * 0.35f, h * 0.96f, w * 0.48f, h * 1.05f, w * 0.50f, h * 1.02f)
            cubicTo(w * 0.52f, h * 1.05f, w * 0.65f, h * 0.96f, w * 0.58f, h * 0.86f)
            close()
        }
        drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF7043), Color(0xFF29B6F6).copy(alpha = 0f))))

        // Center Rocket Fuselage
        val rocketCore = Path().apply {
            moveTo(w * 0.50f, h * 0.05f) // Cream Nosecone
            cubicTo(w * 0.58f, h * 0.12f, w * 0.57f, h * 0.25f, w * 0.57f, h * 0.82f)
            lineTo(w * 0.43f, h * 0.82f)
            cubicTo(w * 0.43f, h * 0.25f, w * 0.42f, h * 0.12f, w * 0.50f, h * 0.05f)
            close()
        }
        drawPath(rocketCore, Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFFFFFFF), Color(0xFF94A3B8))))
        drawPath(rocketCore, Color(0xFF64748B), style = Stroke(1f))

        // Thermal protection nosecone (Cream gold)
        val noseCone = Path().apply {
            moveTo(w * 0.50f, h * 0.05f)
            cubicTo(w * 0.56f, h * 0.12f, w * 0.56f, h * 0.22f, w * 0.56f, h * 0.22f)
            lineTo(w * 0.44f, h * 0.22f)
            cubicTo(w * 0.44f, h * 0.12f, w * 0.44f, h * 0.12f, w * 0.50f, h * 0.05f)
            close()
        }
        drawPath(noseCone, Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFE2E8F0))))

        // 4 Side Booster Pods
        fun drawBooster(cx: Float) {
            val bPath = Path().apply {
                moveTo(cx, h * 0.46f)
                cubicTo(cx + w * 0.06f, h * 0.52f, cx + w * 0.06f, h * 0.80f, cx + w * 0.06f, h * 0.80f)
                lineTo(cx - w * 0.06f, h * 0.80f)
                cubicTo(cx - w * 0.06f, h * 0.52f, cx, h * 0.46f, cx, h * 0.46f)
                close()
            }
            drawPath(bPath, Brush.horizontalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFF64748B))))
            drawPath(bPath, Color(0xFF475569), style = Stroke(1f))
            
            // Engine Nozzle bell
            val nozzle = Path().apply {
                moveTo(cx - w * 0.05f, h * 0.80f)
                lineTo(cx - w * 0.07f, h * 0.86f)
                lineTo(cx + w * 0.07f, h * 0.86f)
                lineTo(cx + w * 0.05f, h * 0.80f)
                close()
            }
            drawPath(nozzle, Color(0xFF334155))
            drawPath(nozzle, Color(0xFFF59E0B), style = Stroke(1f))
        }
        drawBooster(w * 0.35f)
        drawBooster(w * 0.65f)

        // Center Rocket Engine Nozzles
        drawRect(Color(0xFF1E293B), Offset(w * 0.44f, h * 0.82f), Size(w * 0.12f, h * 0.05f))
    }
}

// 3. Bullet Train (High-Speed Locomotive)
@Composable
fun AdvancedTrainCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Magnetic / Steel Track
        drawLine(Color(0xFF475569), Offset(0f, h * 0.82f), Offset(w, h * 0.82f), strokeWidth = 3f)
        drawLine(Color(0xFF38BDF8), Offset(0f, h * 0.84f), Offset(w, h * 0.84f), strokeWidth = 1.5f)

        // Aerodynamic Train Body (White EMU)
        val trainBody = Path().apply {
            moveTo(w * 0.05f, h * 0.78f)
            lineTo(w * 0.72f, h * 0.78f)
            cubicTo(w * 0.94f, h * 0.78f, w * 0.96f, h * 0.46f, w * 0.72f, h * 0.32f) // Sleek nose cone
            lineTo(w * 0.05f, h * 0.32f)
            close()
        }
        drawPath(trainBody, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1))))
        drawPath(trainBody, Color(0xFF94A3B8), style = Stroke(1.2f))

        // Blue High-Speed Racing Stripes
        val stripe = Path().apply {
            moveTo(w * 0.05f, h * 0.44f)
            lineTo(w * 0.70f, h * 0.44f)
            cubicTo(w * 0.84f, h * 0.48f, w * 0.88f, h * 0.58f, w * 0.84f, h * 0.62f)
            lineTo(w * 0.05f, h * 0.62f)
            close()
        }
        drawPath(stripe, Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))))

        // Tinted Windows Strip
        for (i in 0..4) {
            val x = w * (0.08f + i * 0.12f)
            drawRoundRect(
                Color(0xFF0F172A),
                Offset(x, h * 0.38f),
                Size(w * 0.08f, h * 0.12f),
                cornerRadius = CornerRadius(2f)
            )
        }

        // Driver Cabin Cockpit Windshield (Wrap-around glass)
        val cockpit = Path().apply {
            moveTo(w * 0.68f, h * 0.34f)
            lineTo(w * 0.78f, h * 0.42f)
            lineTo(w * 0.82f, h * 0.54f)
            lineTo(w * 0.72f, h * 0.54f)
            close()
        }
        drawPath(cockpit, Color(0xFF0F172A))
        drawPath(cockpit, Color(0xFF38BDF8), style = Stroke(1f))

        // Headlight Beam
        drawCircle(Color.White, radius = w * 0.025f, center = Offset(w * 0.86f, h * 0.68f))
    }
}

// 4. Smart Skyscraper (Modern Architecture)
@Composable
fun AdvancedSkyscraperCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Plaza Base
        drawRoundRect(Color(0xFF1E293B), Offset(w * 0.18f, h * 0.84f), Size(w * 0.64f, h * 0.12f), cornerRadius = CornerRadius(4f))
        
        // Landscaped plaza trees
        drawCircle(Color(0xFF10B981), radius = w * 0.04f, center = Offset(w * 0.24f, h * 0.84f))
        drawCircle(Color(0xFF10B981), radius = w * 0.04f, center = Offset(w * 0.76f, h * 0.84f))

        // Main Glass Tower (Blue Shimmering Facade)
        val tower = Path().apply {
            moveTo(w * 0.30f, h * 0.84f)
            lineTo(w * 0.70f, h * 0.84f)
            lineTo(w * 0.66f, h * 0.16f)
            lineTo(w * 0.34f, h * 0.16f)
            close()
        }
        drawPath(tower, Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0369A1))))
        drawPath(tower, Color(0xFFBAE6FD), style = Stroke(1.2f))

        // Floor Grid Lines with glowing cyan neon bands
        for (i in 1..7) {
            val y = h * (0.18f + i * 0.08f)
            val inset = (1f - (y / h)) * w * 0.04f
            drawLine(
                if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFF0F172A).copy(alpha = 0.5f),
                Offset(w * 0.34f + inset, y),
                Offset(w * 0.66f - inset, y),
                strokeWidth = if (i % 2 == 0) 2f else 1f
            )
        }

        // Structural Steel Vertical Columns
        drawLine(Color(0xFFE2E8F0), Offset(w * 0.35f, h * 0.16f), Offset(w * 0.31f, h * 0.84f), strokeWidth = 2.5f)
        drawLine(Color(0xFFE2E8F0), Offset(w * 0.65f, h * 0.16f), Offset(w * 0.69f, h * 0.84f), strokeWidth = 2.5f)

        // Rooftop Smart Crown & Helipad
        drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.38f, h * 0.10f), Size(w * 0.24f, h * 0.06f), cornerRadius = CornerRadius(2f))
        drawLine(Color.White, Offset(w * 0.50f, h * 0.10f), Offset(w * 0.50f, h * 0.04f), strokeWidth = 2f)
        drawCircle(Color(0xFFEF4444), radius = w * 0.02f, center = Offset(w * 0.50f, h * 0.04f))
    }
}

// 5. Autonomous Drone Swarm (4-Pod Interconnected Cluster)
@Composable
fun AdvancedDroneSwarmCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val cx = w * 0.5f
        val cy = h * 0.5f

        // Interconnecting Carbon-Fiber Arms
        drawLine(Color(0xFF334155), Offset(cx - w * 0.28f, cy - h * 0.28f), Offset(cx + w * 0.28f, cy + h * 0.28f), strokeWidth = 5f)
        drawLine(Color(0xFF334155), Offset(cx + w * 0.28f, cy - h * 0.28f), Offset(cx - w * 0.28f, cy + h * 0.28f), strokeWidth = 5f)

        // 4 Modular Drone Pods with Ducted Rotors
        fun drawDronePod(px: Float, py: Float) {
            // White chassis capsule
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1))),
                Offset(px - w * 0.10f, py - h * 0.06f),
                Size(w * 0.20f, h * 0.12f),
                cornerRadius = CornerRadius(6f)
            )
            // Ducted Rotor Rings
            drawCircle(Color(0xFF0F172A), radius = w * 0.07f, center = Offset(px - w * 0.09f, py), style = Stroke(2f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.07f, center = Offset(px + w * 0.09f, py), style = Stroke(2f))
            // LED indicators
            drawCircle(Color(0xFF00E5FF), radius = w * 0.02f, center = Offset(px - w * 0.05f, py + h * 0.04f))
            drawCircle(Color(0xFFFFB300), radius = w * 0.02f, center = Offset(px + w * 0.05f, py + h * 0.04f))
        }

        drawDronePod(cx, cy - h * 0.28f)
        drawDronePod(cx, cy + h * 0.28f)
        drawDronePod(cx - w * 0.28f, cy)
        drawDronePod(cx + w * 0.28f, cy)

        // Central Control Hub (Black Box with Status Core)
        drawRoundRect(Color(0xFF0F172A), Offset(cx - w * 0.11f, cy - h * 0.11f), Size(w * 0.22f, h * 0.22f), cornerRadius = CornerRadius(6f))
        drawRoundRect(Color(0xFF38BDF8), Offset(cx - w * 0.11f, cy - h * 0.11f), Size(w * 0.22f, h * 0.22f), cornerRadius = CornerRadius(6f), style = Stroke(1.5f))
        drawCircle(Color(0xFF00E5FF), radius = w * 0.04f, center = Offset(cx, cy))
    }
}

// 6. UAV (Stealth Combat Drone Jet)
@Composable
fun AdvancedUAVCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Matte Dark Stealth Delta Wing Airframe
        val uavBody = Path().apply {
            moveTo(w * 0.18f, h * 0.20f) // Nose
            lineTo(w * 0.40f, h * 0.15f)
            lineTo(w * 0.90f, h * 0.42f) // Right Wingtip
            lineTo(w * 0.72f, h * 0.85f) // Right Trailing edge
            lineTo(w * 0.54f, h * 0.76f) // Twin exhaust notch
            lineTo(w * 0.46f, h * 0.76f)
            lineTo(w * 0.28f, h * 0.85f) // Left Trailing edge
            lineTo(w * 0.10f, h * 0.72f) // Left Wingtip
            close()
        }
        drawPath(uavBody, Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A))))
        drawPath(uavBody, Color(0xFF64748B), style = Stroke(1.2f))

        // Center Cockpit Sensor Blister
        val sensorBlister = Path().apply {
            moveTo(w * 0.28f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.25f)
            lineTo(w * 0.44f, h * 0.36f)
            lineTo(w * 0.34f, h * 0.40f)
            close()
        }
        drawPath(sensorBlister, Color(0xFF0F172A))
        drawPath(sensorBlister, Color(0xFF38BDF8).copy(alpha = 0.8f), style = Stroke(1f))

        // Twin Jet Exhaust Vector Nozzles
        drawRect(Color(0xFF475569), Offset(w * 0.46f, h * 0.74f), Size(w * 0.08f, h * 0.08f))
        drawCircle(Color(0xFF00E5FF), radius = w * 0.03f, center = Offset(w * 0.50f, h * 0.78f))

        // Stealth panel geometric markings
        drawLine(Color(0xFF475569), Offset(w * 0.35f, h * 0.45f), Offset(w * 0.75f, h * 0.52f), strokeWidth = 1f)
        drawLine(Color(0xFF475569), Offset(w * 0.25f, h * 0.60f), Offset(w * 0.45f, h * 0.65f), strokeWidth = 1f)
    }
}

// 7. Cargo Ship (Container Freight Carrier)
@Composable
fun AdvancedCargoShipCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Water reflection line
        drawLine(Color(0xFF0284C7).copy(alpha = 0.5f), Offset(w * 0.05f, h * 0.82f), Offset(w * 0.95f, h * 0.82f), strokeWidth = 2f)

        // Lower Red/Navy Hull
        val hull = Path().apply {
            moveTo(w * 0.06f, h * 0.56f) // Bow
            lineTo(w * 0.88f, h * 0.42f) // Stern
            lineTo(w * 0.94f, h * 0.74f)
            lineTo(w * 0.14f, h * 0.80f)
            close()
        }
        drawPath(hull, Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFFB91C1C))))
        drawPath(hull, Color(0xFF475569), style = Stroke(1.2f))

        // Bulbous Bow detail
        drawCircle(Color(0xFFB91C1C), radius = w * 0.04f, center = Offset(w * 0.13f, h * 0.78f))

        // Rear Bridge Superstructure & Funnel Stack
        drawRoundRect(Color(0xFFF8FAFC), Offset(w * 0.72f, h * 0.24f), Size(w * 0.14f, h * 0.22f), cornerRadius = CornerRadius(2f))
        drawRect(Color(0xFF1E293B), Offset(w * 0.76f, h * 0.18f), Size(w * 0.06f, h * 0.08f)) // Funnel

        // Multi-Colored Stacked Freight Containers
        val containerColors = listOf(
            Color(0xFF0284C7), Color(0xFFEAB308), Color(0xFFDC2626), 
            Color(0xFF16A34A), Color(0xFFEA580C), Color(0xFF2563EB)
        )
        var cIdx = 0
        for (row in 0..1) {
            for (col in 0..4) {
                val cx = w * (0.16f + col * 0.10f)
                val cy = h * (0.42f - row * 0.09f)
                drawRect(containerColors[cIdx % containerColors.size], Offset(cx, cy), Size(w * 0.09f, h * 0.08f))
                drawRect(Color(0xFF0F172A).copy(alpha = 0.6f), Offset(cx, cy), Size(w * 0.09f, h * 0.08f), style = Stroke(0.8f))
                cIdx++
            }
        }
    }
}

// 8. Super Yacht (Luxury Motor Yacht)
@Composable
fun AdvancedSuperYachtCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Sleek White/Ivory Luxury Hull
        val yachtHull = Path().apply {
            moveTo(w * 0.90f, h * 0.32f) // Sharp bow
            lineTo(w * 0.08f, h * 0.62f) // Stern
            lineTo(w * 0.12f, h * 0.78f)
            lineTo(w * 0.82f, h * 0.52f)
            close()
        }
        drawPath(yachtHull, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1))))
        drawPath(yachtHull, Color(0xFF94A3B8), style = Stroke(1.2f))

        // Realistic Teak Wood Decking (Warm caramel)
        val teakDeck = Path().apply {
            moveTo(w * 0.82f, h * 0.36f)
            lineTo(w * 0.12f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.70f)
            lineTo(w * 0.76f, h * 0.48f)
            close()
        }
        drawPath(teakDeck, Color(0xFFD97706))

        // Upper Tier Deck Cabin & Flybridge
        val flybridge = Path().apply {
            moveTo(w * 0.68f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.42f)
            lineTo(w * 0.34f, h * 0.54f)
            lineTo(w * 0.66f, h * 0.40f)
            close()
        }
        drawPath(flybridge, Color(0xFFFFFFFF))
        drawPath(flybridge, Color(0xFF94A3B8), style = Stroke(1f))

        // Tinted panoramic glass windows
        drawLine(Color(0xFF0F172A), Offset(w * 0.64f, h * 0.36f), Offset(w * 0.36f, h * 0.48f), strokeWidth = 3.5f)

        // Radar mast dome
        drawCircle(Color(0xFFFFFFFF), radius = w * 0.035f, center = Offset(w * 0.48f, h * 0.26f))
        drawCircle(Color(0xFF94A3B8), radius = w * 0.035f, center = Offset(w * 0.48f, h * 0.26f), style = Stroke(1f))
    }
}

// 9. Hydrogen Plant (Fuel Cell & Storage Unit)
@Composable
fun AdvancedHydrogenPlantCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Green Industrial Cylindrical Vessel Body
        val tankRect = Rect(Offset(w * 0.16f, h * 0.22f), Size(w * 0.68f, h * 0.56f))
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFF15803D), Color(0xFF22C55E), Color(0xFF166534))),
            tankRect.topLeft,
            tankRect.size,
            cornerRadius = CornerRadius(w * 0.12f)
        )
        drawRoundRect(Color(0xFF14532D), tankRect.topLeft, tankRect.size, cornerRadius = CornerRadius(w * 0.12f), style = Stroke(2f))

        // Steel Reinforcement Clamping Rings
        drawLine(Color(0xFFCBD5E1), Offset(w * 0.30f, h * 0.22f), Offset(w * 0.30f, h * 0.78f), strokeWidth = 5f)
        drawLine(Color(0xFFCBD5E1), Offset(w * 0.70f, h * 0.22f), Offset(w * 0.70f, h * 0.78f), strokeWidth = 5f)

        // Glowing Cyan Hydrogen Pressure Observation Window
        val gaugeRect = Rect(Offset(w * 0.42f, h * 0.36f), Size(w * 0.20f, h * 0.26f))
        drawRoundRect(Color(0xFF0F172A), gaugeRect.topLeft, gaugeRect.size, cornerRadius = CornerRadius(6f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0284C7))),
            Offset(w * 0.44f, h * 0.40f),
            Size(w * 0.16f, h * 0.18f),
            cornerRadius = CornerRadius(4f)
        )

        // Copper Valves and Neon-Cyan Feed Pipes
        drawLine(Color(0xFFF97316), Offset(w * 0.10f, h * 0.52f), Offset(w * 0.20f, h * 0.52f), strokeWidth = 4f)
        drawLine(Color(0xFF00E5FF), Offset(w * 0.14f, h * 0.52f), Offset(w * 0.14f, h * 0.72f), strokeWidth = 3f)
        drawLine(Color(0xFF00E5FF), Offset(w * 0.80f, h * 0.40f), Offset(w * 0.88f, h * 0.40f), strokeWidth = 3f)
    }
}

// 10. Defense Frigate (Naval Guided-Missile Warship)
@Composable
fun AdvancedFrigateCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Battleship Grey Stealth Hull
        val frigateHull = Path().apply {
            moveTo(w * 0.94f, h * 0.72f) // Sharp bow
            lineTo(w * 0.08f, h * 0.32f) // Stern
            lineTo(w * 0.06f, h * 0.42f)
            lineTo(w * 0.88f, h * 0.82f)
            close()
        }
        drawPath(frigateHull, Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF334155), Color(0xFF1E293B))))
        drawPath(frigateHull, Color(0xFF94A3B8), style = Stroke(1.2f))

        // Forward 76mm Cannon Turret
        drawRoundRect(Color(0xFF475569), Offset(w * 0.70f, h * 0.58f), Size(w * 0.10f, h * 0.08f), cornerRadius = CornerRadius(2f))
        drawLine(Color(0xFFE2E8F0), Offset(w * 0.78f, h * 0.62f), Offset(w * 0.88f, h * 0.68f), strokeWidth = 2.5f) // Barrel

        // Stealth Conning Tower Mast & Radomes
        val conning = Path().apply {
            moveTo(w * 0.48f, h * 0.30f)
            lineTo(w * 0.36f, h * 0.36f)
            lineTo(w * 0.38f, h * 0.48f)
            lineTo(w * 0.56f, h * 0.42f)
            close()
        }
        drawPath(conning, Color(0xFF475569))
        drawCircle(Color(0xFFE2E8F0), radius = w * 0.035f, center = Offset(w * 0.44f, h * 0.30f)) // Radome

        // Helicopter Helipad Flight Deck
        drawCircle(Color(0xFFE2E8F0), radius = w * 0.05f, center = Offset(w * 0.18f, h * 0.38f), style = Stroke(1.5f))
    }
}

// 11. Satellite (Orbital Communications Probe)
@Composable
fun AdvancedSatelliteCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Dual Extended Gold Solar Arrays
        fun drawGoldWing(startX: Float, isLeft: Boolean) {
            val wingRect = if (isLeft) {
                Rect(Offset(w * 0.06f, h * 0.54f), Size(w * 0.32f, h * 0.24f))
            } else {
                Rect(Offset(w * 0.62f, h * 0.22f), Size(w * 0.32f, h * 0.24f))
            }
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFB45309))),
                wingRect.topLeft,
                wingRect.size
            )
            drawRect(Color(0xFFF8FAFC), wingRect.topLeft, wingRect.size, style = Stroke(1.2f))
            // Photovoltaic cell grids
            drawLine(Color(0xFF78350F), Offset(wingRect.left + wingRect.width * 0.5f, wingRect.top), Offset(wingRect.left + wingRect.width * 0.5f, wingRect.bottom), strokeWidth = 1f)
            drawLine(Color(0xFF78350F), Offset(wingRect.left, wingRect.top + wingRect.height * 0.5f), Offset(wingRect.right, wingRect.top + wingRect.height * 0.5f), strokeWidth = 1f)
        }
        drawGoldWing(0f, true)
        drawGoldWing(0f, false)

        // Central Satellite Bus (Metallic Box)
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))),
            Offset(w * 0.36f, h * 0.34f),
            Size(w * 0.28f, h * 0.32f),
            cornerRadius = CornerRadius(4f)
        )
        drawRoundRect(Color(0xFF475569), Offset(w * 0.36f, h * 0.34f), Size(w * 0.28f, h * 0.32f), cornerRadius = CornerRadius(4f), style = Stroke(1.5f))

        // High-Gain Parabolic Dish Antenna
        drawOval(
            Brush.radialGradient(listOf(Color(0xFFCBD5E1), Color(0xFF475569))),
            Offset(w * 0.54f, h * 0.46f),
            Size(w * 0.24f, h * 0.24f)
        )
        drawOval(Color(0xFFF8FAFC), Offset(w * 0.54f, h * 0.46f), Size(w * 0.24f, h * 0.24f), style = Stroke(1.5f))
        drawLine(Color(0xFF00E5FF), Offset(w * 0.66f, h * 0.58f), Offset(w * 0.74f, h * 0.66f), strokeWidth = 2.5f)
    }
}

// 12. AI Data Center (Neon Server Rack)
@Composable
fun AdvancedDataCenterCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Server Rack Cabinet (Dark Obsidian Metal)
        val rackRect = Rect(Offset(w * 0.24f, h * 0.12f), Size(w * 0.52f, h * 0.76f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
            rackRect.topLeft,
            rackRect.size,
            cornerRadius = CornerRadius(8f)
        )
        drawRoundRect(Color(0xFF475569), rackRect.topLeft, rackRect.size, cornerRadius = CornerRadius(8f), style = Stroke(1.5f))

        // Tempered Glass Door with Glowing Violet/Purple Neon Trim
        drawRoundRect(
            Color(0xFFA855F7),
            Offset(w * 0.30f, h * 0.18f),
            Size(w * 0.40f, h * 0.64f),
            cornerRadius = CornerRadius(4f),
            style = Stroke(2f)
        )

        // Server Blades with indicator LEDs
        for (i in 0..4) {
            val by = h * (0.22f + i * 0.11f)
            drawRect(Color(0xFF334155), Offset(w * 0.33f, by), Size(w * 0.34f, h * 0.08f))
            // Violet status glow
            drawLine(Color(0xFFC084FC), Offset(w * 0.35f, by + h * 0.04f), Offset(w * 0.65f, by + h * 0.04f), strokeWidth = 1.5f)
        }

        // Glowing Quantum Core in center
        drawCircle(Color(0xFFE879F9), radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.50f))
    }
}

// 13. Smart Grid (Power Conversion Module)
@Composable
fun AdvancedSmartGridCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Chassis with cooling heatsink ridges
        val chassis = Rect(Offset(w * 0.16f, h * 0.20f), Size(w * 0.68f, h * 0.60f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF0F172A))),
            chassis.topLeft,
            chassis.size,
            cornerRadius = CornerRadius(8f)
        )
        drawRoundRect(Color(0xFF64748B), chassis.topLeft, chassis.size, cornerRadius = CornerRadius(8f), style = Stroke(2f))

        // Top Heat Sink Fins
        for (i in 0..5) {
            drawLine(Color(0xFF475569), Offset(w * (0.24f + i * 0.09f), h * 0.14f), Offset(w * (0.24f + i * 0.09f), h * 0.20f), strokeWidth = 2.5f)
        }

        // Glowing Blue Electric Circuit Window
        drawRoundRect(
            Color(0xFF0F172A),
            Offset(w * 0.32f, h * 0.30f),
            Size(w * 0.44f, h * 0.40f),
            cornerRadius = CornerRadius(4f)
        )
        drawRoundRect(
            Color(0xFF00E5FF),
            Offset(w * 0.32f, h * 0.30f),
            Size(w * 0.44f, h * 0.40f),
            cornerRadius = CornerRadius(4f),
            style = Stroke(1.5f)
        )

        // Circuit Traces inside
        drawLine(Color(0xFF38BDF8), Offset(w * 0.36f, h * 0.40f), Offset(w * 0.52f, h * 0.40f), strokeWidth = 2f)
        drawLine(Color(0xFF38BDF8), Offset(w * 0.52f, h * 0.40f), Offset(w * 0.52f, h * 0.60f), strokeWidth = 2f)
        drawLine(Color(0xFF38BDF8), Offset(w * 0.52f, h * 0.60f), Offset(w * 0.68f, h * 0.60f), strokeWidth = 2f)

        // Side High-Voltage Terminals
        drawCircle(Color(0xFF22C55E), radius = w * 0.035f, center = Offset(w * 0.16f, h * 0.42f))
        drawCircle(Color(0xFFEF4444), radius = w * 0.035f, center = Offset(w * 0.16f, h * 0.58f))
    }
}

// 14. Fusion Reactor Core (Tokamak Plasma Ring)
@Composable
fun AdvancedFusionReactorCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val cx = w * 0.5f
        val cy = h * 0.5f

        // Heavy Copper Outer Electromagnetic Coil Ring
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFF97316), Color(0xFFC2410C), Color(0xFF7C2D12))),
            radius = w * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(w * 0.12f)
        )

        // Inner Steel Vacuum Chamber
        drawCircle(Color(0xFF1E293B), radius = w * 0.32f, center = Offset(cx, cy))
        drawCircle(Color(0xFF64748B), radius = w * 0.32f, center = Offset(cx, cy), style = Stroke(2f))

        // Glowing Cyan Magnetic Confinement Ring
        drawCircle(
            Color(0xFF00E5FF),
            radius = w * 0.24f,
            center = Offset(cx, cy),
            style = Stroke(3f)
        )

        // Burning Solar Fusion Core (Orange/Yellow Plasma)
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFBBF24), Color(0xFFEA580C))),
            radius = w * 0.14f,
            center = Offset(cx, cy)
        )
    }
}

// 15. Quantum Supercomputer (Cryogenic Quantum Processor)
@Composable
fun AdvancedQuantumComputerCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Brushed Gold/Brass Hexagonal Base
        val base = Rect(Offset(w * 0.18f, h * 0.52f), Size(w * 0.64f, h * 0.38f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFB45309))),
            base.topLeft,
            base.size,
            cornerRadius = CornerRadius(8f)
        )
        drawRoundRect(Color(0xFF78350F), base.topLeft, base.size, cornerRadius = CornerRadius(8f), style = Stroke(1.5f))

        // Digital Cyan Diagnostic Status Screen
        drawRoundRect(Color(0xFF0F172A), Offset(w * 0.26f, h * 0.62f), Size(w * 0.20f, h * 0.18f), cornerRadius = CornerRadius(4f))
        drawRoundRect(Color(0xFF00E5FF), Offset(w * 0.26f, h * 0.62f), Size(w * 0.20f, h * 0.18f), cornerRadius = CornerRadius(4f), style = Stroke(1f))

        // 5 Vertical Cryogenic Glass Vacuum Tubes with Glowing Cyan Filaments
        for (i in 0..4) {
            val tx = w * (0.24f + i * 0.13f)
            val tubeRect = Rect(Offset(tx - w * 0.05f, h * 0.16f), Size(w * 0.10f, h * 0.36f))
            
            // Glass tube
            drawRoundRect(Color(0xFFE0F2FE).copy(alpha = 0.8f), tubeRect.topLeft, tubeRect.size, cornerRadius = CornerRadius(w * 0.05f))
            drawRoundRect(Color(0xFFFDE047), tubeRect.topLeft, tubeRect.size, cornerRadius = CornerRadius(w * 0.05f), style = Stroke(1.5f))
            
            // Glowing Quantum Core Filament inside
            drawLine(Color(0xFF00E5FF), Offset(tx, h * 0.20f), Offset(tx, h * 0.48f), strokeWidth = 2.5f)
        }
    }
}

// 16. Hyperloop Capsule (Aerodynamic Passenger Pod)
@Composable
fun AdvancedHyperloopCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Carbon-Fiber Track Skirt
        drawRoundRect(Color(0xFF1E293B), Offset(w * 0.12f, h * 0.70f), Size(w * 0.76f, h * 0.14f), cornerRadius = CornerRadius(6f))

        // Sleek Brushed Metallic Titanium Body
        val pod = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            cubicTo(w * 0.14f, h * 0.50f, w * 0.28f, h * 0.22f, w * 0.48f, h * 0.22f)
            lineTo(w * 0.70f, h * 0.22f)
            cubicTo(w * 0.88f, h * 0.36f, w * 0.92f, h * 0.62f, w * 0.84f, h * 0.68f)
            close()
        }
        drawPath(pod, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF64748B))))
        drawPath(pod, Color(0xFF475569), style = Stroke(1.2f))

        // Panoramic Tinted Glass Canopy
        val glass = Path().apply {
            moveTo(w * 0.54f, h * 0.24f)
            lineTo(w * 0.72f, h * 0.24f)
            cubicTo(w * 0.84f, h * 0.36f, w * 0.86f, h * 0.56f, w * 0.78f, h * 0.58f)
            lineTo(w * 0.52f, h * 0.58f)
            close()
        }
        drawPath(glass, Color(0xFF0F172A))
        drawPath(glass, Color(0xFF38BDF8), style = Stroke(1f))

        // Rear Stabilizer Fin
        val fin = Path().apply {
            moveTo(w * 0.20f, h * 0.22f)
            lineTo(w * 0.18f, h * 0.10f)
            lineTo(w * 0.28f, h * 0.22f)
            close()
        }
        drawPath(fin, Color(0xFF334155))
    }
}

// 17. Luxury Aircraft Interior (VIP Armchair)
@Composable
fun AdvancedAircraftCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Folding Dark Mahogany Executive Side Table
        drawRoundRect(Color(0xFF78350F), Offset(w * 0.56f, h * 0.44f), Size(w * 0.34f, h * 0.40f), cornerRadius = CornerRadius(4f))
        drawRoundRect(Color(0xFFFBBF24), Offset(w * 0.56f, h * 0.44f), Size(w * 0.34f, h * 0.40f), cornerRadius = CornerRadius(4f), style = Stroke(1f))

        // VIP Beige Quilted Leather Armchair
        val backrest = Path().apply {
            moveTo(w * 0.16f, h * 0.54f)
            lineTo(w * 0.20f, h * 0.14f)
            cubicTo(w * 0.30f, h * 0.08f, w * 0.46f, h * 0.08f, w * 0.54f, h * 0.14f)
            lineTo(w * 0.58f, h * 0.54f)
            close()
        }
        drawPath(backrest, Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFD97706))))
        drawPath(backrest, Color(0xFFB45309), style = Stroke(1.2f))

        // Polished Chrome Headrest Trim
        drawArc(
            Color(0xFFE2E8F0),
            -180f,
            180f,
            false,
            Offset(w * 0.20f, h * 0.08f),
            Size(w * 0.34f, h * 0.16f),
            style = Stroke(3f)
        )

        // Quilted Diamond Stitching on Leather
        for (i in 0..2) {
            drawLine(Color(0xFFB45309).copy(alpha = 0.6f), Offset(w * (0.24f + i * 0.08f), h * 0.22f), Offset(w * (0.34f + i * 0.08f), h * 0.44f), strokeWidth = 1f)
            drawLine(Color(0xFFB45309).copy(alpha = 0.6f), Offset(w * (0.44f - i * 0.08f), h * 0.22f), Offset(w * (0.34f - i * 0.08f), h * 0.44f), strokeWidth = 1f)
        }

        // Plush Cushion Seat Base
        drawRoundRect(Color(0xFFFEF3C7), Offset(w * 0.14f, h * 0.52f), Size(w * 0.46f, h * 0.34f), cornerRadius = CornerRadius(8f))
        drawRoundRect(Color(0xFFB45309), Offset(w * 0.14f, h * 0.52f), Size(w * 0.46f, h * 0.34f), cornerRadius = CornerRadius(8f), style = Stroke(1.2f))
    }
}

// Fallback Tech Core
@Composable
fun AdvancedFallbackCanvas(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        drawCircle(color.copy(alpha = 0.25f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f))
        drawRoundRect(color, Offset(w * 0.25f, h * 0.25f), Size(w * 0.5f, h * 0.5f), cornerRadius = CornerRadius(6f), style = Stroke(2f))
        drawCircle(Color.White, radius = w * 0.1f, center = Offset(w * 0.5f, h * 0.5f))
    }
}
