package com.example.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemeSurface
import com.example.ui.theme.ThemeSurfaceGlass

fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(8.dp),
    color: Color = ThemeSurfaceGlass,
    blurRadius: Dp = 12.dp
) = this
    .clip(shape)
    .background(color)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    color: Color = ThemeSurfaceGlass,
    blurRadius: Dp = 12.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = ThemeNeonCyan,
    pulsing: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassmorphism(shape = shape, color = color, blurRadius = blurRadius)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.neonBorder(color = borderColor, shape = shape, borderWidth = borderWidth, pulsing = pulsing)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

fun Modifier.neonBorder(
    color: Color = ThemeNeonCyan,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 1.dp,
    pulsing: Boolean = true
) = composed {
    val alpha by if (pulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "border_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "border_alpha"
        )
    } else {
        androidx.compose.runtime.mutableStateOf(1f)
    }

    this.border(
        width = borderWidth,
        brush = Brush.horizontalGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = (alpha * 0.5f).coerceIn(0.2f, 1f)),
                ThemeGold.copy(alpha = alpha * 0.7f)
            )
        ),
        shape = shape
    )
}

fun Modifier.pulse(
    minAlpha: Float = 0.6f,
    maxAlpha: Float = 1.0f,
    durationMillis: Int = 1200
) = composed {
    val alpha by rememberInfiniteTransition(label = "pulse_anim").animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    this.graphicsLayer {
        this.alpha = alpha
    }
}

