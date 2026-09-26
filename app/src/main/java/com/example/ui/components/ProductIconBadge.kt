package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.Product
import com.example.data.ProductTier
import com.example.ui.theme.ThemeNeonCyan

/**
 * Returns a sector category icon corresponding to the product's industry/sector.
 */
fun getSectorCategoryIcon(product: Product): ImageVector {
    return when {
        product.tier == ProductTier.TIER_1 -> {
            when (product.id) {
                "cotton", "rubber_latex" -> Icons.Rounded.Agriculture
                "iron", "copper", "silicon", "aluminum", "coal", "limestone", "lithium", "titanium", "graphite_ore" -> Icons.Rounded.Terrain
                "timber" -> Icons.Rounded.Forest
                "chemicals" -> Icons.Rounded.Science
                "crude_oil", "natural_gas" -> Icons.Rounded.LocalFireDepartment
                else -> Icons.Rounded.Terrain
            }
        }
        product.tier == ProductTier.TIER_2 -> {
            when (product.id) {
                "fabric" -> Icons.Rounded.Agriculture
                "glass", "steel", "wire", "wood_plank", "cement", "packaging", "aluminum_ingot", "titanium_ingot", "pcb_substrate", "titanium_alloy", "carbon_fiber" -> Icons.Rounded.Factory
                "battery", "semiconductor_wafer", "optical_fiber", "graphene_sheet" -> Icons.Rounded.ElectricBolt
                "plastic", "refined_fuel", "tire", "synthetic_textile", "liquefied_gas" -> Icons.Rounded.Science
                else -> Icons.Rounded.Factory
            }
        }
        product.tier == ProductTier.TIER_3 -> {
            when (product.id) {
                "clothing", "furniture" -> Icons.Rounded.Agriculture
                "chip", "smartphone", "solar_panel", "turbine_engine", "telecom_station", "nano_battery" -> Icons.Rounded.ElectricBolt
                "pharma", "biotech_med", "petrochem" -> Icons.Rounded.Science
                "auto_part", "appliance", "machinery", "building_block", "industrial_container", "composite_structure", "robotics_arm" -> Icons.Rounded.Factory
                else -> Icons.Rounded.Factory
            }
        }
        product.tier == ProductTier.TIER_4 -> {
            when (product.id) {
                "ev", "cargo_ship", "bullet_train", "defense_frigate", "super_yacht", "space_rocket", "hyperloop_capsule", "luxury_aircraft_interior", "smart_skyscraper" -> Icons.Rounded.Factory
                "uav", "satellite", "ai_datacenter", "smart_grid", "fusion_reactor_core", "quantum_supercomputer", "autonomous_drone_swarm" -> Icons.Rounded.ElectricBolt
                "hydrogen_plant" -> Icons.Rounded.Science
                else -> Icons.Rounded.Factory
            }
        }
        product.id == "battery" || product.id == "solar_panel" -> Icons.Rounded.ElectricBolt
        product.id == "pharma" || product.id == "chemicals" -> Icons.Rounded.Science
        else -> Icons.Rounded.Factory
    }
}

/**
 * Modern, dynamic multi-layered icon badge with gradient aura, 3D border,
 * sector badge overlay, animated production gear, glowing pulse dot, and tier accent.
 */
@Composable
fun ProductIconBadge(
    product: Product,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isProducing: Boolean = false,
    showSectorBadge: Boolean = true
) {
    val brandColor = Color(product.colorTint)

    val tierAccent = when (product.tier) {
        ProductTier.TIER_1 -> Color(0xFF00E676) // Vibrant Emerald
        ProductTier.TIER_2 -> ThemeNeonCyan     // Cyber Cyan
        ProductTier.TIER_3 -> Color(0xFFE040FB) // Neon Purple/Magenta
        ProductTier.TIER_4 -> Color(0xFFFFD700) // Plasma Gold
    }
    
    // Smooth infinite transition for pulsing aura and active gear - optimized to run ONLY when isProducing is active
    val activeAngle: Float
    val pulseAlpha: Float
    if (isProducing) {
        val infiniteTransition = rememberInfiniteTransition(label = "badge_anim_${product.id}")
        val internalGearAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
            label = "internal_gear_angle"
        )
        val internalPulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
            label = "badge_pulse_alpha"
        )
        activeAngle = internalGearAngle
        pulseAlpha = internalPulseAlpha
    } else {
        activeAngle = 0f
        pulseAlpha = 0.55f
    }

    val cornerRadius = size * 0.28f
    val iconSize = size * 0.54f
    val sectorBadgeSize = size * 0.36f
    val sectorIconSize = size * 0.22f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        // 1. Dynamic Radial Ambient Energy Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (isProducing) ThemeNeonCyan else brandColor).copy(alpha = if (isProducing) pulseAlpha * 0.55f else 0.35f),
                            tierAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Outer Rotating Ring or Glowing Frame for Active Production
        if (isProducing) {
            Box(
                modifier = Modifier
                    .size(size * 0.96f)
                    .clip(RoundedCornerShape(cornerRadius + 2.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ThemeNeonCyan,
                                brandColor,
                                tierAccent,
                                ThemeNeonCyan
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius + 2.dp)
                    )
                    .rotate(activeAngle * 0.5f)
            )
        }

        // 3. Multi-layered Glass/Metallic Dark Container
        Box(
            modifier = Modifier
                .size(size * 0.88f)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            brandColor.copy(alpha = 0.28f),
                            Color(0xFF141E2D),
                            Color(0xFF0B101B)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = if (isProducing) {
                            listOf(ThemeNeonCyan, brandColor, tierAccent)
                        } else {
                            listOf(brandColor, brandColor.copy(alpha = 0.5f), tierAccent.copy(alpha = 0.7f))
                        }
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            // Glossy Top Reflection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.18f),
                            0.4f to Color.White.copy(alpha = 0.03f),
                            1f to Color.Transparent
                        )
                    )
            )

            // Icon Center
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isProducing) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Üretiliyor",
                        tint = ThemeNeonCyan,
                        modifier = Modifier
                            .size(iconSize)
                            .rotate(activeAngle)
                    )
                } else {
                    UniversalProductIcon(
                        product = product,
                        size = iconSize,
                        tint = brandColor
                    )
                }
            }
        }

        // 4. Corner Sector / Tier Badge Indicator
        if (showSectorBadge) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF090E1A),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, tierAccent),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(sectorBadgeSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = getSectorCategoryIcon(product),
                        contentDescription = null,
                        tint = tierAccent,
                        modifier = Modifier.size(sectorIconSize)
                    )
                }
            }
        }

        // 5. Active Production Pulsing Laser Dot
        if (isProducing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .size(size * 0.24f)
                    .clip(CircleShape)
                    .background(ThemeNeonCyan.copy(alpha = pulseAlpha))
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}
