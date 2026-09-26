package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.isEnglishLanguage
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

const val GAME_CURRENCY_NAME = "Anadolu Lirası"
const val GAME_CURRENCY_NAME_FULL = "Anadolu Lirası"
const val GAME_CURRENCY_SYMBOL = "₳"
const val GAME_CURRENCY_CODE = "AL"
const val EXCHANGE_RATE_TL_PER_USD = 1.0

fun getLiveUsdRate(): Double = 1.0

fun getCurrencySymbol(isUsd: Boolean = false): String = GAME_CURRENCY_SYMBOL
fun getCurrencyCode(isUsd: Boolean = false): String = GAME_CURRENCY_CODE

fun formatCredit(amount: Long): String {
    val isNegative = amount < 0
    val absVal = kotlin.math.abs(amount)
    val formatted = when {
        absVal >= 1_000_000_000_000_000_000L -> String.format(Locale.US, "%.2f E", absVal / 1e18)
        absVal >= 1_000_000_000_000_000L -> String.format(Locale.US, "%.2f P", absVal / 1e15)
        absVal >= 1_000_000_000_000L -> String.format(Locale.US, "%.2f T", absVal / 1e12)
        absVal >= 1_000_000_000L -> String.format(Locale.US, "%.2f B", absVal / 1e9)
        absVal >= 1_000_000L -> String.format(Locale.US, "%.2f M", absVal / 1e6)
        absVal >= 1_000L -> String.format(Locale.US, "%.1f K", absVal / 1e3)
        else -> NumberFormat.getNumberInstance(Locale.US).format(absVal)
    }
    return if (isNegative) "-₳$formatted" else "₳$formatted"
}

fun formatCurrency(amount: Long): String = formatCredit(amount)
fun formatCurrency(amount: Double): String = formatCredit(amount.toLong())
fun formatCurrency(amount: Long, isUsd: Boolean): String = formatCredit(amount)
fun formatCurrency(amount: Double, isUsd: Boolean): String = formatCredit(amount.toLong())

fun formatDollar(usdAmount: Long): String = formatCredit(usdAmount)
fun formatLira(tryAmount: Long): String = formatCredit(tryAmount)

fun formatCurrencyByUsd(amount: Long, isUsd: Boolean): String = formatCredit(amount)

fun formatMoney(amount: Long): String {
    val isNegative = amount < 0
    val absVal = kotlin.math.abs(amount)
    val formatted = when {
        absVal >= 1_000_000_000_000_000_000L -> String.format(Locale.US, "%.2f E", absVal / 1e18)
        absVal >= 1_000_000_000_000_000L -> String.format(Locale.US, "%.2f P", absVal / 1e15)
        absVal >= 1_000_000_000_000L -> String.format(Locale.US, "%.2f T", absVal / 1e12)
        absVal >= 1_000_000_000L -> String.format(Locale.US, "%.2f B", absVal / 1e9)
        absVal >= 1_000_000L -> String.format(Locale.US, "%.2f M", absVal / 1e6)
        absVal >= 1_000L -> String.format(Locale.US, "%.1f K", absVal / 1e3)
        else -> NumberFormat.getNumberInstance(Locale.US).format(absVal)
    }
    return if (isNegative) "-$formatted" else formatted
}

fun formatStockTons(ton: Long, isEnglish: Boolean = false): String {
    if (ton <= 0L) return if (isEnglish) "0 Ton (CRISIS)" else "0 Ton (KRİZ)"
    val absVal = kotlin.math.abs(ton)
    val unit = if (isEnglish) "Tons" else "Ton"
    val formatted = when {
        absVal >= 1_000_000_000_000_000L -> String.format(Locale.US, "%.2fQ", absVal / 1e15)
        absVal >= 1_000_000_000_000L -> String.format(Locale.US, "%.2fT", absVal / 1e12)
        absVal >= 1_000_000_000L -> String.format(Locale.US, "%.2fB", absVal / 1e9)
        absVal >= 1_000_000L -> String.format(Locale.US, "%.2fM", absVal / 1e6)
        absVal >= 100_000L -> String.format(Locale.US, "%.1fK", absVal / 1e3)
        else -> NumberFormat.getNumberInstance(Locale.US).format(absVal)
    }
    return "$formatted $unit"
}

fun formatStockTonsExact(ton: Long, isEnglish: Boolean = false): String {
    if (ton <= 0L) return if (isEnglish) "0 Ton (CRISIS)" else "0 Ton (KRİZ)"
    val unit = if (isEnglish) "Tons" else "Ton"
    return "${NumberFormat.getNumberInstance(Locale.US).format(kotlin.math.abs(ton))} $unit"
}

fun formatMoney(amount: Double): String = formatMoney(amount.toLong())
fun formatMoney(amount: Long, unusedIsEnglish: Boolean): String = formatMoney(amount)
fun formatMoney(amount: Double, unusedIsEnglish: Boolean): String = formatMoney(amount.toLong())

/**
 * Modern, Chic & Vibrant Anatolian Lira (Anadolu Lirası ₳) Minted Golden Coin Vector Graphic
 * Renders a lifelike metallic minted coin with beveled rims, metallic highlights,
 * security coin ridges/beads, and an embossed Anatolian Lira "₳" symbol.
 */
@Composable
fun AnatolianLiraCoinCanvas(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFFFFB800), // Vibrant Gold
    accentColor: Color = Color(0xFFFFF3A1), // Shimmer Bright Gold
    deepColor: Color = Color(0xFF995800) // Deep Bronze-Gold Shadow
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val radius = kotlin.math.min(w, h) / 2f
        val center = Offset(w / 2f, h / 2f)

        if (radius <= 0) return@Canvas

        // 1. Drop shadow & Outer Rim Edge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(deepColor.copy(alpha = 0.8f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // 2. Outer Minted Gold Coin Body (Linear metallic slant sheen)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    accentColor,
                    baseColor,
                    Color(0xFFE5A100),
                    deepColor,
                    baseColor,
                    accentColor
                ),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y + radius)
            ),
            radius = radius * 0.96f,
            center = center
        )

        // 3. Inner Bevel Ring (Minted Coin Rim)
        drawCircle(
            color = deepColor.copy(alpha = 0.6f),
            radius = radius * 0.86f,
            center = center,
            style = Stroke(width = kotlin.math.max(1f, radius * 0.08f))
        )

        drawCircle(
            color = accentColor.copy(alpha = 0.7f),
            radius = radius * 0.80f,
            center = center,
            style = Stroke(width = kotlin.math.max(0.8f, radius * 0.04f))
        )

        // 4. Coin Security Milled Beads around the inner circumference (when size is sufficient)
        if (radius >= 10f) {
            val beadCount = 12
            val beadRadius = radius * 0.74f
            val dotSize = kotlin.math.max(1f, radius * 0.035f)
            for (i in 0 until beadCount) {
                val angle = (i * (2 * Math.PI / beadCount)).toFloat()
                val bx = center.x + beadRadius * cos(angle)
                val by = center.y + beadRadius * sin(angle)
                drawCircle(
                    color = accentColor.copy(alpha = 0.65f),
                    radius = dotSize,
                    center = Offset(bx, by)
                )
            }
        }

        // 5. Inner Core Plate (Subtle dark-gold contrast to make symbol pop)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF804500),
                    Color(0xFF4A2500)
                ),
                center = Offset(center.x - radius * 0.15f, center.y - radius * 0.2f),
                radius = radius * 0.68f
            ),
            radius = radius * 0.68f,
            center = center
        )

        // 6. Draw Embossed Anatolian Lira "₳" (High-precision drawn path)
        val symbolScale = radius * 0.58f
        val strokeW = kotlin.math.max(1.8f, radius * 0.12f)
        val cx = center.x
        val cy = center.y - radius * 0.06f

        // Coordinates for the stylized Anatolian Lira '₳'
        val topY = cy - symbolScale * 0.72f
        val botY = cy + symbolScale * 0.72f
        val leftX = cx - symbolScale * 0.55f
        val rightX = cx + symbolScale * 0.55f
                val bar1Y = cy - symbolScale * 0.08f
        val bar2Y = cy + symbolScale * 0.24f
        val bar1W = symbolScale * 0.32f
        val bar2W = symbolScale * 0.44f

        // Shadow under symbol (3D depth)
        val shadowOffset = kotlin.math.max(1f, radius * 0.04f)
        val shadowPath = Path().apply {
            // Main 'A' legs
            moveTo(cx, topY + shadowOffset)
            lineTo(leftX, botY + shadowOffset)
            moveTo(cx, topY + shadowOffset)
            lineTo(rightX, botY + shadowOffset)
            // Upper Bar
            moveTo(cx - bar1W, bar1Y + shadowOffset)
            lineTo(cx + bar1W, bar1Y + shadowOffset)
            // Lower Bar
            moveTo(cx - bar2W, bar2Y + shadowOffset)
            lineTo(cx + bar2W, bar2Y + shadowOffset)
        }
        drawPath(
            path = shadowPath,
            color = Color.Black.copy(alpha = 0.65f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Front Golden Embossed Symbol
        val mainPath = Path().apply {
            // Main 'A' apex & legs
            moveTo(cx, topY)
            lineTo(leftX, botY)
            moveTo(cx, topY)
            lineTo(rightX, botY)
            // Upper Bar
            moveTo(cx - bar1W, bar1Y)
            lineTo(cx + bar1W, bar1Y)
            // Lower Bar
            moveTo(cx - bar2W, bar2Y)
            lineTo(cx + bar2W, bar2Y)
        }
        drawPath(
            path = mainPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFBE0), Color(0xFFFFD54F), Color(0xFFFF9800)),
                start = Offset(cx, topY),
                end = Offset(cx, botY)
            ),
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 7. Specular Gloss Highlight Arc (Curved glass sheen at top-left)
        val glossPath = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center.x - radius * 0.88f,
                    center.y - radius * 0.88f,
                    center.x + radius * 0.88f,
                    center.y + radius * 0.88f
                ),
                startAngleDegrees = 190f,
                sweepAngleDegrees = 110f,
                forceMoveTo = false
            )
        }
        drawPath(
            path = glossPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                start = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
                end = Offset(center.x + radius * 0.2f, center.y)
            ),
            style = Stroke(width = kotlin.math.max(1.2f, radius * 0.10f), cap = StrokeCap.Round)
        )
    }
}

/**
 * Custom Currency Symbol Composable (Renders ₳ Game Currency)
 */
@Composable
fun GameCurrencySymbol(
    modifier: Modifier = Modifier,
    tint: Color = ThemeGold,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    CurrencyText(
        text = GAME_CURRENCY_SYMBOL,
        style = MaterialTheme.typography.titleMedium,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = RajdhaniFontFamily,
        color = tint,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun TurkishLiraSymbol(
    modifier: Modifier = Modifier,
    tint: Color = ThemeGold,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    GameCurrencySymbol(modifier = modifier, tint = tint, fontSize = fontSize, fontWeight = fontWeight)
}

/**
 * Modern, Chic & Vibrant Anadolu Lirası Coin Icon Component
 * Clearly conveys money, wealth, and prestige.
 */
@Composable
fun AnadoluLiraIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    AnatolianLiraCoinCanvas(
        modifier = modifier
            .size(size)
    )
}

@Composable
fun GameCurrencyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = ThemeGold,
    backgroundColor: Color = ThemeGold.copy(alpha = 0.15f),
    borderColor: Color = ThemeGold.copy(alpha = 0.6f)
) {
    AnatolianLiraCoinCanvas(
        modifier = modifier.size(size)
    )
}

@Composable
fun TurkishLiraIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = ThemeGold,
    backgroundColor: Color = ThemeGold.copy(alpha = 0.15f),
    borderColor: Color = ThemeGold.copy(alpha = 0.6f)
) {
    AnatolianLiraCoinCanvas(
        modifier = modifier.size(size)
    )
}

/**
 * Full Currency Money Badge Component
 */
@Composable
fun GameCurrencyBadge(
    amount: Long,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    badgeColor: Color = ThemeGold,
    showBg: Boolean = true
) {
    val isEng = isEnglishLanguage()
    val content = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            AnadoluLiraIcon(size = 18.dp)
            CurrencyText(
                text = formatMoney(amount, isEng).removePrefix("₺").removePrefix("$").removePrefix("₳").removePrefix("◈"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMonoFontFamily,
                color = textColor
            )
        }
    }

    if (showBg) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF141926),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
            modifier = modifier
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) { content() }
    }
}

@Composable
fun TurkishLiraBadge(
    amount: Long,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    badgeColor: Color = ThemeGold,
    showBg: Boolean = true
) {
    GameCurrencyBadge(
        amount = amount,
        modifier = modifier,
        textColor = textColor,
        badgeColor = badgeColor,
        showBg = showBg
    )
}
