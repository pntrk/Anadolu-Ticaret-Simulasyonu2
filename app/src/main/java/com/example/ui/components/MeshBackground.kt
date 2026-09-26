package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.data.GameStateEntity
import com.example.ui.theme.LocalAppThemeOption
import com.example.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun MeshBackground(gameState: GameStateEntity?, isCelebrating: Boolean) {
    val themeOption = LocalAppThemeOption.current
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_anim")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "phase1"
    )
    
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "phase2"
    )

    val currentHour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }

    val isDroughtOrFrost = gameState?.activeEvent == "Don (Tarım Düşüşü)" || gameState?.activeEvent == "Kuraklık (Tahıl Azalır)"
    
    val (timeColor1, timeColor2) = remember(currentHour, isCelebrating, isDroughtOrFrost) {
        when {
            isCelebrating -> Color(0xFF00C853) to Color(0xFFFFD700)
            isDroughtOrFrost -> Color(0xFFE65100) to Color(0xFFBF360C)
            currentHour in 6..17 -> Color(0xFF00B0FF).copy(alpha = 0.28f) to Color(0xFFFFAB00).copy(alpha = 0.25f) // Day: Bright Sky & Warm Sun
            currentHour in 18..20 -> Color(0xFFFF6D00).copy(alpha = 0.35f) to Color(0xFFD50000).copy(alpha = 0.30f) // Sunset: Golden Amber & Crimson
            else -> Color(0xFF00E5FF).copy(alpha = 0.25f) to Color(0xFF7C4DFF).copy(alpha = 0.25f) // Night: Cyberpunk Electric Cyan & Violet
        }
    }

    val baseColor1 = if (!themeOption.isDark && !isCelebrating && !isDroughtOrFrost) timeColor1.copy(alpha = 0.18f) else timeColor1
    val baseColor2 = if (!themeOption.isDark && !isCelebrating && !isDroughtOrFrost) timeColor2.copy(alpha = 0.18f) else timeColor2

    val c1 by animateColorAsState(targetValue = baseColor1, tween(1200), label = "c1")
    val c2 by animateColorAsState(targetValue = baseColor2, tween(1200), label = "c2")

    Box(modifier = Modifier.fillMaxSize().background(themeOption.backgroundColor)) {
        // High quality background image (subtle overlay in light mode, atmospheric in dark mode)
        Image(
            painter = painterResource(id = R.drawable.bg_city_night),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (themeOption.isDark) 0.55f else 0.08f
        )
        
        // Animated gradient overlays reflecting current theme identity
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Isolate graphics layer for ultra-smooth 60fps hardware accelerated rendering
                    clip = true
                }
        ) {
            val width = size.width
            val height = size.height

            val isAnim = com.example.utils.HapticManager.isAnimationsEnabled
            val cx1 = if (isAnim) {
                width * 0.5f + width * 0.3f * kotlin.math.sin(phase1.toDouble()).toFloat()
            } else {
                width * 0.5f
            }
            val cy1 = if (isAnim) {
                height * 0.5f + height * 0.3f * kotlin.math.cos(phase1.toDouble()).toFloat()
            } else {
                height * 0.5f
            }
            
            val cx2 = if (isAnim) {
                width * 0.5f + width * 0.4f * kotlin.math.cos(phase2.toDouble()).toFloat()
            } else {
                width * 0.6f
            }
            val cy2 = if (isAnim) {
                height * 0.5f + height * 0.4f * kotlin.math.sin(phase2.toDouble()).toFloat()
            } else {
                height * 0.4f
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(c1, Color.Transparent),
                    center = Offset(cx1, cy1),
                    radius = width * 0.85f
                ),
                size = Size(width, height)
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(c2, Color.Transparent),
                    center = Offset(cx2, cy2),
                    radius = width * 0.95f
                ),
                size = Size(width, height)
            )
            
            // Soft vignette tuned for readability across both dark & light themes
            val vignetteColor = if (themeOption.isDark) Color(0xCC000000) else themeOption.backgroundColor.copy(alpha = 0.65f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, vignetteColor),
                    center = Offset(width / 2, height / 2),
                    radius = width * 1.25f
                ),
                size = Size(width, height)
            )
        }
    }
}
