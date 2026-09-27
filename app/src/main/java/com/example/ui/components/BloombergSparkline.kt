package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import kotlin.math.abs

/**
 * Modern Bloomberg / Tycoon style Sparkline component with animated glowing pulse dot,
 * translucent area fill gradient, and smooth Bezier curve.
 */
@Composable
fun BloombergSparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true,
    lineColor: Color? = null,
    strokeWidth: Dp = 2.dp,
    showAreaFill: Boolean = true,
    showPulseDot: Boolean = true,
    showGridLines: Boolean = false
) {
    if (data.size < 2) {
        // Fallback for minimal data
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2f
                )
            }
        }
        return
    }

    val baseColor = lineColor ?: if (isPositive) ThemePositive else ThemeNegative
    val areaColor = baseColor.copy(alpha = 0.22f)

    // Ultra-optimized static glowing values to prevent CPU/GPU recomposition spikes and gralloc Binder transaction overflow
    val pulseScale = 1.15f
    val pulseAlpha = 0.75f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val minVal = (data.minOrNull() ?: 0f) * 0.98f
        val maxVal = (data.maxOrNull() ?: 1f) * 1.02f
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * height).coerceIn(4f, height - 4f)
            Offset(x, y)
        }

        // Draw Bloomberg subtle grid lines if requested
        if (showGridLines) {
            val gridColor = Color.White.copy(alpha = 0.06f)
            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height * 0.50f), Offset(width, height * 0.50f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1f)
        }

        // Build smooth Bezier path
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            if (i == 0) {
                path.moveTo(p.x, p.y)
                fillPath.moveTo(p.x, height)
                fillPath.lineTo(p.x, p.y)
            } else {
                val prev = points[i - 1]
                val controlX1 = (prev.x + p.x) / 2f
                val controlY1 = prev.y
                val controlX2 = (prev.x + p.x) / 2f
                val controlY2 = p.y
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p.x, p.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p.x, p.y)
            }
        }

        fillPath.lineTo(points.last().x, height)
        fillPath.close()

        // 1. Draw glowing gradient fill under curve
        if (showAreaFill) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(areaColor, areaColor.copy(alpha = 0.01f)),
                    startY = 0f,
                    endY = height
                )
            )
        }

        // 2. Draw glowing background blur stroke
        drawPath(
            path = path,
            color = baseColor.copy(alpha = 0.35f),
            style = Stroke(width = strokeWidth.toPx() + 3f, cap = StrokeCap.Round)
        )

        // 3. Draw main crisp sharp stroke
        drawPath(
            path = path,
            color = baseColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )

        // 4. Draw pulsating leading dot at the latest price
        if (showPulseDot && points.isNotEmpty()) {
            val lastPoint = points.last()
            // Outer pulse halo
            drawCircle(
                color = baseColor.copy(alpha = pulseAlpha * 0.45f),
                radius = 7.dp.toPx() * pulseScale,
                center = lastPoint
            )
            // Core solid dot
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = lastPoint
            )
            drawCircle(
                color = baseColor,
                radius = 2.dp.toPx(),
                center = lastPoint
            )
        }
    }
}

/**
 * Compact Bloomberg price badge showing real-time price, % change, and mini sparkline.
 */
@Composable
fun BloombergPriceBadge(
    currentPrice: Long,
    priceHistory: List<Long>,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val floatData = remember(priceHistory, currentPrice) {
        if (priceHistory.size >= 2) {
            priceHistory.map { it.toFloat() }
        } else {
            listOf(currentPrice * 0.95f, currentPrice * 0.98f, currentPrice.toFloat())
        }
    }

    val isPositive = remember(floatData) {
        if (floatData.size >= 2) floatData.last() >= floatData.first() else true
    }

    val percentChange = remember(floatData) {
        if (floatData.size >= 2 && floatData.first() > 0f) {
            (((floatData.last() - floatData.first()) / floatData.first()) * 100f).coerceIn(-90f, 500f)
        } else 0f
    }

    val badgeColor = if (isPositive) ThemePositive else ThemeNegative
    val bgGlass = Color(0xFF070E1B).copy(alpha = 0.85f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgGlass)
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            if (label != null) {
                CurrencyText(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            CurrencyText(
                text = formatMoney(currentPrice),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                color = ThemeGold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                CurrencyText(
                    text = "${if (isPositive) "+" else ""}%${"%.1f".format(abs(percentChange))}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily,
                    color = badgeColor
                )
            }
        }

        // Mini sparkline canvas
        BloombergSparkline(
            data = floatData,
            modifier = Modifier
                .width(60.dp)
                .height(26.dp),
            isPositive = isPositive,
            strokeWidth = 1.5.dp,
            showPulseDot = true
        )
    }
}
