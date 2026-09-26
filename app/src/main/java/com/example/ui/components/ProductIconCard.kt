package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive

/**
 * Product & Facility Icon Card modeled after classic Kapitalizm trading game UI.
 * Features a high-contrast white/light or HUD card, a vibrant central icon badge,
 * bold uppercase title, stock/facility info, and prominent action button.
 */
@Composable
fun ProductIconCard(
    product: Product,
    modifier: Modifier = Modifier,
    stockCount: Int? = null,
    ownedFacilitiesCount: Int = 0,
    isProducing: Boolean = false,
    progress: Float = 0f,
    actionButtonText: String? = null,
    actionButtonColor: Color = ThemeGold,
    onCardClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null
) {
    val brandColor = Color(product.colorTint)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = onCardClick != null) { onCardClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFBFD) // Clean light card background like user's screenshot
        ),
        border = BorderStroke(
            1.5.dp,
            if (isProducing) ThemeNeonCyan else brandColor.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Tier Badge / Owned Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = brandColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, brandColor.copy(alpha = 0.4f))
                ) {
                    CurrencyText(
                        text = product.tier.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (ownedFacilitiesCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = Color(0xFF101726),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        CurrencyText(
                            text = "$ownedFacilitiesCount Tesis",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (stockCount != null) {
                    CurrencyText(
                        text = "Stok: $stockCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A5568)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Central Dynamic Icon Badge
            ProductIconBadge(
                product = product,
                size = 60.dp,
                isProducing = isProducing,
                
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Product Name in Uppercase Bold
            CurrencyText(
                text = product.displayName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF1A202C),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Facility Subtitle
            CurrencyText(
                text = product.facilityName,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar if Producing
            if (isProducing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ThemeNeonCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CurrencyText(
                        text = "ÜRETİLİYOR... %${(progress * 100).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeNeonCyan
                    )
                }
            } else if (!actionButtonText.isNullOrEmpty()) {
                // Action Button like in user screenshot "BÜYÜT (₳500)" or "ÜRET"
                Button(
                    onClick = { onActionClick?.invoke() },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionButtonColor,
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    CurrencyText(
                        text = actionButtonText.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
