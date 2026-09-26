package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan

/**
 * Animated Shimmer Glow Border for Luxury Cards (T4 Products, VIP Traders, Mega Projects, IPO Stocks)
 */
fun Modifier.luxeShimmerBorder(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.5.dp,
    shimmerColor: Color = ThemeGold
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "luxe_shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    this
        .border(
            BorderStroke(
                borderWidth,
                Brush.linearGradient(
                    colors = listOf(
                        shimmerColor.copy(alpha = 0.3f),
                        shimmerColor,
                        ThemeNeonCyan,
                        shimmerColor.copy(alpha = 0.3f)
                    ),
                    start = Offset(translateAnim - 300f, translateAnim - 300f),
                    end = Offset(translateAnim, translateAnim)
                )
            ),
            shape = shape
        )
}

/**
 * Shimmer Sweep Overlay on top of Card Content
 */
fun Modifier.luxeShimmerContent(
    enabled: Boolean = true,
    highlightColor: Color = Color.White.copy(alpha = 0.15f)
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_content")
    val xAnim by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "x_anim"
    )

    this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    highlightColor,
                    Color.Transparent
                ),
                start = Offset(xAnim, 0f),
                end = Offset(xAnim + 160f, size.height)
            )
        )
    }
}
