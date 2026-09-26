package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun OrderBookDepthBar(
    demandQuantity: Long,
    supplyQuantity: Long,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    compact: Boolean = false
) {
    val total = (demandQuantity + supplyQuantity).coerceAtLeast(1L).toDouble()
    val rawBuyRatio = (demandQuantity.toDouble() / total).toFloat().coerceIn(0.08f, 0.92f)

    val animatedBuyRatio by animateFloatAsState(
        targetValue = rawBuyRatio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "order_book_depth_ratio"
    )

    val buyPercent = (animatedBuyRatio * 100).toInt()
    val sellPercent = 100 - buyPercent

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alış (Talep) Tarafı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(ThemePositive)
                    )
                    Text(
                        text = if (compact) tr("Alış %$buyPercent", "Bid %$buyPercent")
                        else tr("Alış: ${formatStockTons(demandQuantity)} (%$buyPercent)", "Bid: ${formatStockTons(demandQuantity, true)} (%$buyPercent)"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemePositive
                    )
                }

                // Satış (Arz) Tarafı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (compact) tr("Satış %$sellPercent", "Ask %$sellPercent")
                        else tr("Satış: ${formatStockTons(supplyQuantity)} (%$sellPercent)", "Ask: ${formatStockTons(supplyQuantity, true)} (%$sellPercent)"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color(0xFFFF5252)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFFF5252))
                    )
                }
            }
        }

        // İki Renkli Görsel Derinlik Çubuğu (Canvas ile çizilir, layout invalidation yapmaz)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 4.dp else 6.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            val width = size.width
            val height = size.height
            
            // Arka plan
            drawRect(color = Color(0xFF0F172A), size = size)
            
            val buyWidth = width * animatedBuyRatio
            
            // Yeşil (Alış / Talep)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00E676), Color(0xFF00C853)),
                    startX = 0f,
                    endX = buyWidth
                ),
                size = Size(buyWidth, height)
            )
            
            // Ayırıcı çizgi
            drawRect(
                color = Color(0xFF0A0E17),
                topLeft = Offset(buyWidth, 0f),
                size = Size(1.dp.toPx(), height)
            )
            
            // Kırmızı (Satış / Arz)
            val sellStartX = buyWidth + 1.dp.toPx()
            val sellWidth = (width - sellStartX).coerceAtLeast(0f)
            if (sellWidth > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF5252), Color(0xFFFF1744)),
                        startX = sellStartX,
                        endX = width
                    ),
                    topLeft = Offset(sellStartX, 0f),
                    size = Size(sellWidth, height)
                )
            }
        }
    }
}
