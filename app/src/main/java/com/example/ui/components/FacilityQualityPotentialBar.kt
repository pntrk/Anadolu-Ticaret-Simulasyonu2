package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ItemQuality
import com.example.data.Product
import com.example.data.QualityCraftingService
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr

/**
 * Tesis Detay Ekranı - Kalite Potansiyeli Barı (Quality Potential Bar)
 *
 * Tesis seviyesine göre ürünün hangi kalite aralığında (1★ - 5★) üretilebileceğini
 * görselleştirir. 9-10 seviye tesislerde 5 yıldız (Kusursuz / Saray Kalitesi) olasılığını
 * simgeleyen dinamik altın ışıltılı (Golden Shimmer & Aura) efekt barındırır.
 *
 * Seviye Kademeleri:
 * - 1-2 Seviye Tesis -> 1 Yıldız (Standart - x1.0)
 * - 3-4 Seviye Tesis -> 2 Yıldız (Seçme - x1.35)
 * - 5-6 Seviye Tesis -> 3 Yıldız (Usta İşi - x1.90)
 * - 7-8 Seviye Tesis -> 4 Yıldız (Seçkin - x2.80)
 * - 9-10 Seviye Tesis -> 5 Yıldız (Kusursuz / Altın Seri - x4.50)
 */
@Composable
fun FacilityQualityPotentialBar(
    facilityLevel: Int,
    product: Product? = null,
    isProducing: Boolean = false,
    productionProgress: Float = 0f,
    wearLevel: Float = 0.0f,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val clampedLevel = facilityLevel.coerceIn(1, 10)
    val maxQuality = ItemQuality.fromFacilityLevel(clampedLevel)
    val isMaxTierGolden = clampedLevel >= 9
    val wearPenalty = QualityCraftingService.getWearQualityPenaltyStars(wearLevel)
    val wearPercent = if (wearLevel <= 1.0f) kotlin.math.round(wearLevel * 100f).toInt() else kotlin.math.round(wearLevel).toInt()

    // Infinite transition for animations
    val infiniteTransition = rememberInfiniteTransition(label = "QualityPotentialAnim")

    // Golden shimmer animation for 9-10 level facilities
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GoldShimmerSweep"
    )

    // Golden pulse glow
    val goldPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GoldGlowPulse"
    )

    // Pulse for active production node
    val productionPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ProductionPulse"
    )

    // Golden Shimmer Brush
    val goldenShimmerBrush = remember(shimmerTranslate) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = 0.5f),
                Color(0xFFFFF9C4).copy(alpha = 0.95f),
                Color(0xFFFFA000).copy(alpha = 0.6f),
                Color(0xFFFFD700).copy(alpha = 0.5f)
            ),
            start = Offset(shimmerTranslate, 0f),
            end = Offset(shimmerTranslate + 250f, 150f)
        )
    }

    // Border and background styling based on whether 5-star golden effect is active
    val cardBackground = if (isMaxTierGolden) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1705),
                Color(0xFF131A26),
                Color(0xFF0F1522)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF141D2B),
                Color(0xFF0E1624)
            )
        )
    }

    val cardBorder = if (isMaxTierGolden) {
        BorderStroke(1.2.dp, Color(0xFFFFD700).copy(alpha = goldPulseAlpha))
    } else {
        BorderStroke(1.dp, maxQuality.badgeColor.copy(alpha = 0.45f))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isMaxTierGolden) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = Color(0xFFFFD700),
                        spotColor = Color(0xFFFFC107)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = cardBorder
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .then(
                    if (isMaxTierGolden) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.04f),
                                    Color(0xFFFFA000).copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                    } else Modifier
                )
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row: Title, Facility Level, and Highest Potential Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isMaxTierGolden) Color(0xFFFFD700).copy(alpha = 0.25f) else maxQuality.badgeColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (isMaxTierGolden) Color(0xFFFFD700) else maxQuality.badgeColor),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMaxTierGolden) Icons.Rounded.AutoAwesome else Icons.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isMaxTierGolden) Color(0xFFFFD700) else maxQuality.badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = tr("KALİTE POTANSİYELİ BARI", "QUALITY POTENTIAL BAR", isEng),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = if (isMaxTierGolden) Color(0xFFFFE082) else Color.White
                                )
                                if (isMaxTierGolden) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, Color(0xFFFFD700))
                                    ) {
                                        Text(
                                            text = "✨ " + tr("EFSANEVİ", "LEGENDARY", isEng),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = Color(0xFFFFD700),
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = tr(
                                    "Tesis Seviyesi ${clampedLevel}/10 • Üretim Gücü",
                                    "Facility Level ${clampedLevel}/10 • Production Output",
                                    isEng
                                ),
                                fontSize = 9.sp,
                                fontFamily = RobotoMonoFontFamily,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Maximum Potential Quality Badge
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = if (isMaxTierGolden) Color(0xFF2E2204) else Color(0xFF142032),
                        border = BorderStroke(
                            1.dp,
                            if (isMaxTierGolden) Color(0xFFFFD700).copy(alpha = goldPulseAlpha) else maxQuality.badgeColor.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .then(
                                    if (isMaxTierGolden) Modifier.background(brush = goldenShimmerBrush, alpha = 0.15f) else Modifier
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = maxQuality.starsText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMaxTierGolden) Color(0xFFFFD700) else maxQuality.badgeColor
                            )
                            Text(
                                text = maxQuality.label.replace("⭐", "").trim(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                color = if (isMaxTierGolden) Color(0xFFFFF9C4) else Color.White
                            )
                        }
                    }
                }

                // 5-Tier Segmented Quality Potential Track Bar
                // Shows the 5 levels (1★ to 5★) with unlock states and progress
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF0A0F1A))
                            .border(0.5.dp, Color(0xFF223046), RoundedCornerShape(7.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (tier in 1..5) {
                            val tierQuality = ItemQuality.fromStars(tier)
                            val isUnlocked = maxQuality.stars >= tier
                            val isCurrentTier = maxQuality.stars == tier
                            val isTier5Golden = tier == 5 && clampedLevel >= 9

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isTier5Golden -> goldenShimmerBrush
                                            isCurrentTier -> Brush.horizontalGradient(tierQuality.gradientColors)
                                            isUnlocked -> Brush.horizontalGradient(
                                                tierQuality.gradientColors.map { it.copy(alpha = 0.55f) }
                                            )
                                            else -> Brush.horizontalGradient(
                                                listOf(Color(0xFF161F2E), Color(0xFF1A2436))
                                            )
                                        }
                                    )
                                    .then(
                                        if (isCurrentTier) {
                                            Modifier.border(
                                                1.dp,
                                                if (isTier5Golden) Color(0xFFFFD700).copy(alpha = goldPulseAlpha)
                                                else Color.White.copy(alpha = 0.8f),
                                                RoundedCornerShape(3.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(8.dp)
                                    )
                                } else if (isCurrentTier) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = if (isTier5Golden) Color(0xFF2A1B00) else Color.White,
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tiers Labels and Level Requirements Legend (1★ ... 5★)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val tiersMeta = listOf(
                            Triple(1, "Sv.1-2", "Standart"),
                            Triple(2, "Sv.3-4", "Seçme"),
                            Triple(3, "Sv.5-6", "Usta İşi"),
                            Triple(4, "Sv.7-8", "Seçkin"),
                            Triple(5, "Sv.9-10", "Kusursuz")
                        )

                        tiersMeta.forEach { (stars, levelRange, nameTr) ->
                            val quality = ItemQuality.fromStars(stars)
                            val isUnlocked = maxQuality.stars >= stars
                            val isCurrent = maxQuality.stars == stars
                            val isFiveStarGolden = stars == 5 && clampedLevel >= 9

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${stars}★",
                                    fontSize = 9.sp,
                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                    color = when {
                                        isFiveStarGolden -> Color(0xFFFFD700)
                                        isCurrent -> quality.badgeColor
                                        isUnlocked -> quality.badgeColor.copy(alpha = 0.7f)
                                        else -> Color(0xFF64748B)
                                    }
                                )
                                Text(
                                    text = levelRange,
                                    fontSize = 7.5.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = if (isCurrent) Color(0xFFE2E8F0) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Golden Effect Feature Banner for Level 9-10
                if (isMaxTierGolden) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF281C04).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = goldPulseAlpha)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .background(brush = goldenShimmerBrush, alpha = 0.12f)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "👑",
                                fontSize = 16.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = tr("5★ KUSURSUZ KALİTE POTANSİYELİ", "5★ FLAWLESS QUALITY POTENTIAL", isEng),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color(0xFFFFD700)
                                    )
                                    Text(
                                        text = "(+350% " + tr("Değer", "Value", isEng) + ")",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color(0xFFFFF59D)
                                    )
                                }
                                Text(
                                    text = tr(
                                        "Bu tesiste üretilen ürünler altın seri 5★ kalitesinde çıkar; borsa ve konsorsiyumda x4.5 çarpanla işlem görür!",
                                        "Items produced here achieve gold tier 5★ quality; traded on exchange & consortium with a 4.5x multiplier!",
                                        isEng
                                    ),
                                    fontSize = 8.5.sp,
                                    color = Color(0xFFFFECB3),
                                    lineHeight = 11.5.sp
                                )
                            }
                        }
                    }
                } else {
                    // Next Quality Unlock Guidance
                    val nextTierLevel = when {
                        clampedLevel < 3 -> 3
                        clampedLevel < 5 -> 5
                        clampedLevel < 7 -> 7
                        else -> 9
                    }
                    val nextQuality = ItemQuality.fromFacilityLevel(nextTierLevel)

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F1726),
                        border = BorderStroke(0.5.dp, Color(0xFF223046)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "💡",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = tr(
                                        "Seviye $nextTierLevel yükseltmesi:",
                                        "Level $nextTierLevel upgrade:",
                                        isEng
                                    ),
                                    fontSize = 9.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "${nextQuality.starsText} ${nextQuality.label.replace("⭐", "").trim()}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nextTierLevel == 9) Color(0xFFFFD700) else nextQuality.badgeColor
                                )
                            }

                            Text(
                                text = "x${nextQuality.priceMultiplier} " + tr("Fiyat", "Price", isEng),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = ThemePositive
                            )
                        }
                    }
                }

                // ⚖️ Hammadde & Tesis Kalitesi Denge Paneli (Tesis + Hammadde ortalaması = Nihai Kalite ve Satış Fiyatı)
                if (product != null) {
                    if (product.recipe.isNotEmpty()) {
                        val expectedQual = QualityCraftingService.calculateExpectedQuality(clampedLevel, 1.0, wearLevel)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0D1726),
                            border = BorderStroke(1.dp, Color(0xFF1E2F46)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚖️ " + tr("HAMMADDE & KALİTE DENGESİ", "RAW MATERIAL & QUALITY BALANCE", isEng),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = ThemeNeonCyan
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (wearPenalty > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (wearPercent in 80..100) ThemeNegative.copy(alpha = 0.25f) else Color(0xFFE65100).copy(alpha = 0.25f),
                                                border = BorderStroke(0.5.dp, if (wearPercent in 80..100) ThemeNegative else Color(0xFFFFB74D))
                                            ) {
                                                Text(
                                                    text = "-$wearPenalty★ " + tr("Aşınma", "Wear", isEng),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = RobotoMonoFontFamily,
                                                    color = if (wearPercent in 80..100) ThemeNegative else Color(0xFFFFB74D),
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = expectedQual.badgeColor.copy(alpha = 0.2f),
                                            border = BorderStroke(0.5.dp, expectedQual.badgeColor)
                                        ) {
                                            Text(
                                                text = "${expectedQual.starsText} ${expectedQual.label.replace("⭐", "").trim()}",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = expectedQual.badgeColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(
                                            "Tesis Gücü (${maxQuality.stars}★) + Borsa/Pazar Hammaddesi (1★)",
                                            "Facility Power (${maxQuality.stars}★) + Market Ingredient (1★)",
                                            isEng
                                        ),
                                        fontSize = 8.5.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "Ortalama: ${expectedQual.stars}★",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(
                                            "Nihai Ürün Borsa / Satış Fiyat Çarpanı:",
                                            "Final Product Borsa / Sale Multiplier:",
                                            isEng
                                        ),
                                        fontSize = 8.5.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "x${expectedQual.priceMultiplier} " + tr("Fiyat Primi", "Price Multiplier", isEng),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = ThemePositive
                                    )
                                }

                                if (clampedLevel >= 7 && expectedQual.stars < 5) {
                                    Text(
                                        text = tr(
                                            "💡 İpucu: 5★ nihai ürün üretmek için pazardan veya gelişmiş tesislerinizden 5★ kaliteli hammadde kullanın!",
                                            "💡 Tip: To produce 5★ final products, use 5★ high-quality ingredients from market or advanced facilities!",
                                            isEng
                                        ),
                                        fontSize = 8.sp,
                                        color = Color(0xFFFFD54F),
                                        lineHeight = 10.5.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0D1726),
                            border = BorderStroke(1.dp, Color(0xFF1E2F46)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🌾", fontSize = 12.sp)
                                Text(
                                    text = tr(
                                        "Doğrudan hasat/çıkarma tesisi: Hammadde kullanılmaz, mahsul doğrudan tesis seviyesine göre ${maxQuality.stars}★ (${maxQuality.label}) kalitede çıkar! (x${maxQuality.priceMultiplier} Fiyat)",
                                        "Direct extraction facility: No ingredients required, products output directly at facility tier ${maxQuality.stars}★ (${maxQuality.label}) quality! (x${maxQuality.priceMultiplier} Price)",
                                        isEng
                                    ),
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF81C784),
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }

                // If currently producing, display the live cycle potential status
                if (isProducing) {
                    val activeTargetQual = if (product?.recipe?.isNotEmpty() == true) {
                        QualityCraftingService.calculateExpectedQuality(clampedLevel, 1.0, wearLevel)
                    } else {
                        val baseStars = maxQuality.stars
                        ItemQuality.fromStars((baseStars - wearPenalty).coerceIn(1, 5))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF07111E))
                            .border(0.5.dp, ThemeNeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ThemePositive.copy(alpha = productionPulse),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Text(
                                text = tr("Aktif Üretim Süreci:", "Active Production Cycle:", isEng),
                                fontSize = 8.5.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "${activeTargetQual.starsText} " + tr("Hedefleniyor", "Targeted", isEng),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = if (activeTargetQual.stars == 5) Color(0xFFFFD700) else ThemeNeonCyan
                            )
                        }

                        Text(
                            text = "%${(productionProgress * 100).toInt()} " + tr("Tamamlandı", "Done", isEng),
                            fontSize = 8.5.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan
                        )
                    }
                }
            }
        }
    }
}
