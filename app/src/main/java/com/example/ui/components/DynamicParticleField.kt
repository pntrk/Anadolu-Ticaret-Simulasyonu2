package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import kotlin.math.sin
import kotlin.random.Random

private class AmbientParticle(
    var xRatio: Float,
    var yRatio: Float,
    val baseRadius: Float,
    val speedY: Float,
    val speedXDrift: Float,
    val pulsePhase: Float,
    val color: Color
)

@Composable
fun DynamicParticleField(
    modifier: Modifier = Modifier,
    particleCount: Int = 28,
    primaryColor: Color = ThemeNeonCyan,
    secondaryColor: Color = ThemeGold
) {
    val particles = remember(particleCount) {
        val rand = Random(42)
        List(particleCount) {
            AmbientParticle(
                xRatio = rand.nextFloat(),
                yRatio = rand.nextFloat(),
                baseRadius = rand.nextFloat() * 2.8f + 1.2f,
                speedY = -(rand.nextFloat() * 0.04f + 0.015f),
                speedXDrift = (rand.nextFloat() - 0.5f) * 0.02f,
                pulsePhase = rand.nextFloat() * 6.28f,
                color = if (rand.nextFloat() > 0.4f) primaryColor else secondaryColor
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleLoop")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return@Canvas

        val t = animTime * 0.6f

        particles.forEachIndexed { i, p ->
            // Update relative positions smoothly based on time
            val curY = (p.yRatio + (p.speedY * t)) % 1f
            val normY = if (curY < 0) curY + 1f else curY
            val waveX = sin(t * 0.05f + p.pulsePhase + i) * 0.035f
            val curX = (p.xRatio + waveX + (p.speedXDrift * t * 0.2f)) % 1f
            val normX = if (curX < 0) curX + 1f else curX

            val pulseAlpha = (sin(t * 0.08f + p.pulsePhase) * 0.25f + 0.45f).coerceIn(0.1f, 0.8f)
            val center = Offset(normX * w, normY * h)
            val radius = p.baseRadius * (1f + sin(t * 0.05f + p.pulsePhase) * 0.2f)

            // Soft outer glow
            drawCircle(
                color = p.color.copy(alpha = pulseAlpha * 0.35f),
                radius = radius * 2.2f,
                center = center
            )
            // Core particle
            drawCircle(
                color = p.color.copy(alpha = pulseAlpha),
                radius = radius,
                center = center
            )
        }
    }
}
