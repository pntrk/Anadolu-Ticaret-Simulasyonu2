package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductQuality
import com.example.data.QualityCraftingService
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr

/**
 * 1 - 5 Yıldız Üretim Kalite Rozeti
 */
@Composable
fun QualityBadge(
    quality: ProductQuality,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    showLabel: Boolean = false,
    fontSize: Int = 10
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF101726),
        border = androidx.compose.foundation.BorderStroke(1.dp, quality.primaryColor.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            quality.primaryColor.copy(alpha = 0.25f),
                            Color(0xFF0F172A).copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            repeat(quality.tier) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = quality.primaryColor,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

/**
 * Tesis Detayı İçin Kalite İhtimal Dağılım Çubuğu
 */
@Composable
fun QualityRatePreviewBar(
    facilityLevel: Int,
    playerLevel: Int = 1,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val probs = remember(facilityLevel, playerLevel) {
        QualityCraftingService.getQualityProbabilities(facilityLevel, playerLevel)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("ZANAAT & KALİTE İHTİMALLERİ", "CRAFTSMANSHIP & QUALITY ODDS"),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeGold
            )
            Text(
                text = tr("Tesis Sv.$facilityLevel", "Facility Lvl.$facilityLevel"),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = RobotoMonoFontFamily,
                color = Color.LightGray
            )
        }

        // Multi-segment progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B))
        ) {
            probs.forEach { (quality, prob) ->
                if (prob > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(prob.toFloat())
                            .fillMaxHeight()
                            .background(quality.primaryColor)
                    )
                }
            }
        }

        // Mini legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            probs.forEach { (quality, prob) ->
                val pct = (prob * 100).toInt()
                if (pct > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${quality.tier}★",
                            color = quality.primaryColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " %$pct",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tesis İçi Reçeteler & Birleşik Üretim Zinciri (Synergy Crafting & Quality Heritage) Paneli
 */
@Composable
fun SynergyCraftingPanel(
    product: com.example.data.Product,
    businesses: List<com.example.data.BusinessEntity>,
    playerLevel: Int = 1,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val analysis = remember(product, businesses, playerLevel) {
        QualityCraftingService.analyzeSynergyHeritage(product, businesses, playerLevel)
    }

    if (product.recipe.isEmpty()) {
        // Doğrudan hammadde üreten temel tesisler (Maden, Çiftlik vb.)
        QualityRatePreviewBar(
            facilityLevel = analysis.primaryFacilityLevel,
            playerLevel = playerLevel,
            modifier = modifier
        )
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (analysis.synergyBonusPercent > 0) Color(0xFFFFB300).copy(alpha = 0.6f) else ThemeBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🏭",
                        fontSize = 14.sp
                    )
                    Column {
                        Text(
                            text = tr("KALİTE MİRASI & SİNERJİ", "QUALITY HERITAGE & SYNERGY"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold
                        )
                        Text(
                            text = if (isEng) analysis.product.getDisplayName(true) else analysis.product.getDisplayName(false),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Sinerji Bonusu Rozeti
                if (analysis.synergyBonusPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE65100).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = "+%${analysis.synergyBonusPercent} " + tr("Sinerji", "Synergy"),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                } else if (analysis.defectRiskPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFB71C1C).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
                    ) {
                        Text(
                            text = "%${analysis.defectRiskPercent} " + tr("Kusur Riski", "Defect Risk"),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }
            }

            // Girdi Hammadde Kalite Mirası Listesi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = tr("Kullanılan Ara Mallar & Girdi Kalitesi:", "Input Materials & Heritage Quality:"),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )

                analysis.upstreamItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• ${item.productName}",
                                fontSize = 10.5.sp,
                                color = Color.White
                            )
                            if (item.ownedFacilityLevel != null) {
                                Text(
                                    text = "(Sv.${item.ownedFacilityLevel})",
                                    fontSize = 9.5.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = if (item.isOptimal) Color(0xFF4CAF50) else ThemeGold
                                )
                            } else {
                                Text(
                                    text = tr("(Pazar Alımı)", "(Market Buy)"),
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        QualityBadge(
                            quality = item.qualityTier,
                            size = 10.dp,
                            showLabel = true,
                            fontSize = 8
                        )
                    }
                }
            }

            // Dağılım Çubuğu
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    analysis.probabilities.forEach { (quality, prob) ->
                        if (prob > 0.0) {
                            Box(
                                modifier = Modifier
                                    .weight(prob.toFloat())
                                    .fillMaxHeight()
                                    .background(quality.primaryColor)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    analysis.probabilities.forEach { (quality, prob) ->
                        val pct = (prob * 100).toInt()
                        if (pct > 0) {
                            Text(
                                text = "${quality.tier}★ %$pct",
                                color = quality.primaryColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Açıklayıcı Tavsiye Metni
            Text(
                text = if (isEng) analysis.summaryTextEn else analysis.summaryTextTr,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = if (analysis.synergyBonusPercent > 0) Color(0xFFFFD54F) else Color.LightGray,
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * Konsorsiyum Mega Proje Tedarik & Kalite Mirası Paneli
 */
@Composable
fun ConsortiumSynergyPanel(
    project: com.example.data.MegaProject,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val analysis = remember(project) {
        QualityCraftingService.analyzeConsortiumSynergy(project)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (analysis.totalSynergyBonusPercent > 0) Color(0xFFFFD700).copy(alpha = 0.7f) else ThemeBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "👑", fontSize = 16.sp)
                    Column {
                        Text(
                            text = tr("KONSORSİYUM KALİTE MİRASI", "CONSORTIUM QUALITY HERITAGE"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold
                        )
                        Text(
                            text = analysis.tierRating,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFB45309).copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Text(
                        text = "+%${analysis.totalSynergyBonusPercent} " + tr("Kâr Payı", "Dividend"),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tr("Ortalama Zanaat", "Avg Craftsmanship"),
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "⭐ ${String.format(java.util.Locale.US, "%.1f", analysis.overallCraftsmanshipScore)} / 5.0",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tr("Kâr Çarpanı", "Dividend Multiplier"),
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", analysis.dividendMultiplier)}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tr("Kusur Önleme", "Defect Shield"),
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "%${analysis.defectRateReductionPercent}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }

            // Summary text
            Text(
                text = if (isEng) analysis.summaryEn else analysis.summaryTr,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Color.LightGray,
                lineHeight = 13.sp
            )
        }
    }
}

