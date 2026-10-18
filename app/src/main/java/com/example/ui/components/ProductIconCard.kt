package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.R
import com.example.data.Product
import com.example.data.ProductQuality
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan

/**
 * ProductIconCard (Faz 1 Depo & Malzeme Şablon Entegrasyonu)
 * 
 * Yeni boş şablon (depo_urun.webp) kullanılarak ürün ikonları üst kare yuvaya,
 * kalite yıldızları (QualityBadge), birim fiyat ve stok değerleri ilgili alanlara
 * dinamik olarak enjekte edilir.
 */
@Composable
fun ProductIconCard(
    product: Product,
    modifier: Modifier = Modifier,
    stockCount: Int? = null,
    unitPrice: Long = 0L,
    quality: ProductQuality = ProductQuality.STANDARD,
    ownedFacilitiesCount: Int = 0,
    isProducing: Boolean = false,
    progress: Float = 0f,
    actionButtonText: String? = null,
    actionButtonColor: Color = ThemeGold,
    useDepoTemplate: Boolean = true,
    onCardClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null
) {
    if (useDepoTemplate) {
        // depo_urun.webp plate template architecture
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = onCardClick != null) { onCardClick?.invoke() }
        ) {
            // 1. Background Plate Template Image (depo_urun.webp)
            Image(
                painter = painterResource(id = R.drawable.depo_urun),
                contentDescription = product.displayName,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // 2. ConstraintLayout mapping values to depo_urun.webp slots
            ConstraintLayout(
                modifier = Modifier.fillMaxSize()
            ) {
                val (stockRef, tierRef, iconRef, starsRef, titleRef, statsRef, buttonRef) = createRefs()

                val glTopStock = createGuidelineFromTop(0.040f)
                val glBottomStock = createGuidelineFromTop(0.118f)
                val glStartStock = createGuidelineFromStart(0.065f)
                val glEndStock = createGuidelineFromStart(0.340f)

                val glTopShield = createGuidelineFromTop(0.040f)
                val glBottomShield = createGuidelineFromTop(0.140f)
                val glStartShield = createGuidelineFromStart(0.835f)
                val glEndShield = createGuidelineFromStart(0.950f)

                val glTopShowcase = createGuidelineFromTop(0.125f)
                val glBottomShowcase = createGuidelineFromTop(0.315f)
                val glStartShowcase = createGuidelineFromStart(0.360f)
                val glEndShowcase = createGuidelineFromStart(0.640f)

                val glTopStars = createGuidelineFromTop(0.332f)
                val glBottomStars = createGuidelineFromTop(0.360f)
                val glStartStars = createGuidelineFromStart(0.394f)
                val glEndStars = createGuidelineFromStart(0.618f)

                val glTopTitle = createGuidelineFromTop(0.375f)
                val glBottomTitle = createGuidelineFromTop(0.445f)

                val glTopStats = createGuidelineFromTop(0.455f)
                val glBottomStats = createGuidelineFromTop(0.715f)

                val glTopButtons = createGuidelineFromTop(0.725f)
                val glBottomButtons = createGuidelineFromTop(0.930f)

                val glStartCard = createGuidelineFromStart(0.055f)
                val glEndCard = createGuidelineFromEnd(0.055f)

                // A1. Stock Count in Top-Left Slot
                Box(
                    modifier = Modifier.constrainAs(stockRef) {
                        top.linkTo(glTopStock)
                        bottom.linkTo(glBottomStock)
                        start.linkTo(glStartStock)
                        end.linkTo(glEndStock)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.Center
                ) {
                    val countVal = stockCount ?: 0
                    CurrencyText(
                        text = "$countVal T",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (countVal > 0) Color(0xFF34D399) else Color.LightGray,
                        maxLines = 1
                    )
                }

                // A2. Tier Emblem in Top-Right Shield Slot
                Box(
                    modifier = Modifier.constrainAs(tierRef) {
                        top.linkTo(glTopShield)
                        bottom.linkTo(glBottomShield)
                        start.linkTo(glStartShield)
                        end.linkTo(glEndShield)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.Center
                ) {
                    CurrencyText(
                        text = "T${product.tier.ordinal + 1}",
                        fontSize = 11.5.sp,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Black,
                        color = Color(product.colorTint)
                    )
                }

                // B. Product Icon in Top Square Frame Slot
                Box(
                    modifier = Modifier
                        .constrainAs(iconRef) {
                            top.linkTo(glTopShowcase)
                            bottom.linkTo(glBottomShowcase)
                            start.linkTo(glStartShowcase)
                            end.linkTo(glEndShowcase)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProductIconBadge(
                        product = product,
                        size = 48.dp,
                        isProducing = isProducing,
                        showSectorBadge = false
                    )
                }

                // C. Quality Badge Widget in Star Slot
                Box(
                    modifier = Modifier.constrainAs(starsRef) {
                        top.linkTo(glTopStars)
                        bottom.linkTo(glBottomStars)
                        start.linkTo(glStartStars)
                        end.linkTo(glEndStars)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.Center
                ) {
                    QualityBadge(
                        quality = quality,
                        size = 9.dp,
                        showLabel = false
                    )
                }

                // D. Product Name
                Box(
                    modifier = Modifier.constrainAs(titleRef) {
                        top.linkTo(glTopTitle)
                        bottom.linkTo(glBottomTitle)
                        start.linkTo(glStartCard)
                        end.linkTo(glEndCard)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.Center
                ) {
                    CurrencyText(
                        text = product.displayName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // E. Unit Price & Stats Box
                Box(
                    modifier = Modifier.constrainAs(statsRef) {
                        top.linkTo(glTopStats)
                        bottom.linkTo(glBottomStats)
                        start.linkTo(glStartCard)
                        end.linkTo(glEndCard)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (unitPrice > 0L) {
                            CurrencyText(
                                text = "₳${com.example.ui.components.formatMoney(unitPrice)} / Ton",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = ThemeGold
                            )
                        }
                        if (ownedFacilitiesCount > 0) {
                            CurrencyText(
                                text = "$ownedFacilitiesCount Aktif Tesis",
                                fontSize = 9.5.sp,
                                color = ThemeNeonCyan
                            )
                        }
                    }
                }

                // F. Action Button
                if (!actionButtonText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier.constrainAs(buttonRef) {
                            top.linkTo(glTopButtons)
                            bottom.linkTo(glBottomButtons)
                            start.linkTo(glStartCard)
                            end.linkTo(glEndCard)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onActionClick?.invoke() },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionButtonColor,
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .fillMaxSize()
                        ) {
                            CurrencyText(
                                text = actionButtonText.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Standard Light Card Layout Fallback
        Card(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = onCardClick != null) { onCardClick?.invoke() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2E)),
            border = BorderStroke(1.dp, Color(0xFF1E2D4A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProductIconBadge(
                    product = product,
                    size = 52.dp,
                    isProducing = isProducing
                )
                Spacer(modifier = Modifier.height(6.dp))
                CurrencyText(
                    text = product.displayName.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
