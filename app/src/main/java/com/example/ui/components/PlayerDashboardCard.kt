package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Cyberpunk / Sci-Fi Theme Color Palette
private val CyberNeonCyan = Color(0xFF00E5FF)
private val CyberGold = Color(0xFFFFD54F)
private val CyberGreen = Color(0xFF00E676)
private val CyberGreenBg = Color(0xFF0A2E1C)
private val CyberRed = Color(0xFFFF5252)
private val CyberOrange = Color(0xFFFF9800)
private val CyberPurple = Color(0xFFE040FB)
private val CyberGrayText = Color(0xFF90A4AE)
private val CyberLightBlueText = Color(0xFF90CAF9)

/**
 * Cyberpunk / Sci-Fi HUD Player Company Overview Dashboard Card.
 * Uses `bg_dashboard_plate.webp` directly as the background plate,
 * precisely overlaying dynamic metrics, live text, and interactive touch targets
 * into their exact visual slots.
 */
@Composable
fun PlayerDashboardCard(
    modifier: Modifier = Modifier,
    playerName: String = "Kumcu",
    playerLevel: Int = 16,
    traderTitle: String = "Kıdemli Tüccar",
    dayText: String = "Gün 4",
    xpCurrentText: String = "224.7K",
    xpTargetText: String = "275.0K",
    xpPercent: Int = 81,
    netWorthText: String = "1.24 T",
    growthRateText: String = "+%0.0",
    incomePerMinText: String = "+₳ 440.4 K / dk",
    dailyIncomeText: String = "Günlük: +₳ 6.61 M",
    // 4 Portfolio Cards
    cashText: String = "616.1B (%9)",
    facilityValueText: String = "261.3M (%)",
    depotValueText: String = "0 (%)",
    rdValueText: String = "6.8B (%)",
    // Bottom Status Pills
    capacityText: String = "23/24 Kapasite (%74)",
    reactorEfficiencyText: String = "%24",
    reactorStockText: String = "0",
    fleetCountText: String = "0",
    // Interactive Callbacks
    onAvatarClick: (() -> Unit)? = null,
    onBalanceSheetClick: (() -> Unit)? = null,
    onNetWorthClick: (() -> Unit)? = null,
    onCashClick: (() -> Unit)? = null,
    onFacilityClick: (() -> Unit)? = null,
    onDepotClick: (() -> Unit)? = null,
    onRdClick: (() -> Unit)? = null,
    onCapacityClick: (() -> Unit)? = null,
    onReactorClick: (() -> Unit)? = null,
    onFleetClick: (() -> Unit)? = null,
    onResearchIncentiveClick: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null
) {
    // Proportional HUD Container with exact 4:3 / 1.34 aspect ratio to lock slot coordinates
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.34f)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // 1. Zemin: Orijinal bg_dashboard_plate.webp Zırh Plakası
        Image(
            painter = painterResource(id = R.drawable.bg_dashboard_plate),
            contentDescription = "Şirket Özeti Zırh Plakası",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Katman: Canlı Dinamik Veriler ve Tıklama Alanları

        // --- 2.1 Üst Satır (Profil, LVL, Ünvan, Gün, Bilanço) ---
        // Avatar Dokunma Alanı (Sol Üst)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.18f)
                .fillMaxHeight(0.24f)
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.05f, y = totalHeight * 0.05f)
                .clip(CircleShape)
                .then(if (onAvatarClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAvatarClick() } else Modifier)
        )

        // İsim, LVL ve Ünvan
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.20f, y = totalHeight * 0.08f)
                .fillMaxWidth(0.42f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = playerName,
                    color = Color.White,
                    fontSize = (totalWidth.value * 0.042f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0x33FFD54F)
                ) {
                    Text(
                        text = "LVL $playerLevel",
                        color = CyberGold,
                        fontSize = (totalWidth.value * 0.024f).sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = traderTitle,
                color = CyberLightBlueText,
                fontSize = (totalWidth.value * 0.027f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Gün Slotu (Takvim Yanındaki Koyu Alan)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.705f, y = totalHeight * 0.115f)
                .width(totalWidth * 0.09f)
                .height(totalHeight * 0.08f)
        ) {
            Text(
                text = dayText,
                color = Color.White,
                fontSize = (totalWidth.value * 0.028f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Bilanço Butonu Dokunma Alanı (Sağ Üst)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.81f, y = totalHeight * 0.11f)
                .width(totalWidth * 0.14f)
                .height(totalHeight * 0.09f)
                .clip(RoundedCornerShape(8.dp))
                .then(if (onBalanceSheetClick != null) Modifier.clickable { onBalanceSheetClick() } else Modifier)
        )

        // --- 2.2 XP İlerleme Barı ve Metni ---
        // XP Metni
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.05f, y = totalHeight * 0.255f)
                .fillMaxWidth(0.90f)
        ) {
            Text(
                text = "$xpCurrentText / $xpTargetText XP (%$xpPercent)",
                color = CyberLightBlueText,
                fontSize = (totalWidth.value * 0.025f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Dinamik XP Bar Dolgusu
        val clampedProgress = (xpPercent / 100f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.055f, y = totalHeight * 0.325f)
                .width(totalWidth * 0.89f * clampedProgress)
                .height(totalHeight * 0.035f)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            CyberNeonCyan,
                            CyberNeonCyan,
                            CyberGold
                        )
                    )
                )
        )

        // --- 2.3 Finansal Özet (Net Değer & Dakikalık / Günlük Gelir) ---
        // Sol Reticle Merkezi: '₳' Simgesi
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.055f, y = totalHeight * 0.40f)
                .size(totalWidth * 0.14f)
                .then(if (onNetWorthClick != null) Modifier.clickable { onNetWorthClick() } else Modifier)
        ) {
            Text(
                text = "₳",
                color = CyberGold,
                fontSize = (totalWidth.value * 0.055f).sp,
                fontWeight = FontWeight.Black
            )
        }

        // Net Şirket Değeri + Büyüme Rozeti
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.21f, y = totalHeight * 0.42f)
                .fillMaxWidth(0.40f)
                .then(if (onNetWorthClick != null) Modifier.clickable { onNetWorthClick() } else Modifier),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Net Şirket Değeri",
                color = CyberGrayText,
                fontSize = (totalWidth.value * 0.026f).sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = netWorthText,
                    color = CyberGold,
                    fontSize = (totalWidth.value * 0.048f).sp,
                    fontWeight = FontWeight.Black
                )

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = CyberGreenBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropUp,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size((totalWidth.value * 0.030f).dp)
                        )
                        Text(
                            text = growthRateText,
                            color = CyberGreen,
                            fontSize = (totalWidth.value * 0.024f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sağ Gelir Kartı İçeriği (Dakikalık ve Günlük Gelir)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.725f, y = totalHeight * 0.435f)
                .fillMaxWidth(0.22f)
                .then(if (onNetWorthClick != null) Modifier.clickable { onNetWorthClick() } else Modifier),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = incomePerMinText,
                color = CyberGreen,
                fontSize = (totalWidth.value * 0.026f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dailyIncomeText,
                color = CyberGrayText,
                fontSize = (totalWidth.value * 0.022f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- 2.4 Portföy Kutuları Satırı (Nakit, Tesis, Depo, Ar-Ge) ---
        val boxWidth = totalWidth * 0.218f
        val boxHeight = totalHeight * 0.15f
        val boxSpacing = totalWidth * 0.015f
        val boxStartX = totalWidth * 0.055f
        val boxY = totalHeight * 0.635f

        // 1. Kutu (Nakit)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = boxStartX, y = boxY)
                .width(boxWidth)
                .height(boxHeight)
                .clip(RoundedCornerShape(8.dp))
                .then(if (onCashClick != null) Modifier.clickable { onCashClick() } else Modifier)
        ) {
            // Sağ alt koyu yuvaya değer yerleştirme
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = totalWidth * 0.015f, bottom = totalHeight * 0.012f)
                    .width(boxWidth * 0.46f)
                    .height(boxHeight * 0.38f)
            ) {
                Text(
                    text = cashText,
                    color = CyberGold,
                    fontSize = (totalWidth.value * 0.022f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Kutu (Tesis)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = boxStartX + boxWidth + boxSpacing, y = boxY)
                .width(boxWidth)
                .height(boxHeight)
                .clip(RoundedCornerShape(8.dp))
                .then(if (onFacilityClick != null) Modifier.clickable { onFacilityClick() } else Modifier)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = totalWidth * 0.015f, bottom = totalHeight * 0.012f)
                    .width(boxWidth * 0.46f)
                    .height(boxHeight * 0.38f)
            ) {
                Text(
                    text = facilityValueText,
                    color = CyberRed,
                    fontSize = (totalWidth.value * 0.022f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3. Kutu (Depo)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = boxStartX + (boxWidth + boxSpacing) * 2, y = boxY)
                .width(boxWidth)
                .height(boxHeight)
                .clip(RoundedCornerShape(8.dp))
                .then(if (onDepotClick != null) Modifier.clickable { onDepotClick() } else Modifier)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = totalWidth * 0.015f, bottom = totalHeight * 0.012f)
                    .width(boxWidth * 0.46f)
                    .height(boxHeight * 0.38f)
            ) {
                Text(
                    text = depotValueText,
                    color = CyberOrange,
                    fontSize = (totalWidth.value * 0.022f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Kutu (Ar-Ge)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = boxStartX + (boxWidth + boxSpacing) * 3, y = boxY)
                .width(boxWidth)
                .height(boxHeight)
                .clip(RoundedCornerShape(8.dp))
                .then(if (onRdClick != null) Modifier.clickable { onRdClick() } else Modifier)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = totalWidth * 0.015f, bottom = totalHeight * 0.012f)
                    .width(boxWidth * 0.46f)
                    .height(boxHeight * 0.38f)
            ) {
                Text(
                    text = rdValueText,
                    color = CyberPurple,
                    fontSize = (totalWidth.value * 0.022f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- 2.5 Alt Durum Hapları & Ar-Ge Teşviki ---
        val bottomY = totalHeight * 0.835f

        // 1. Hap (Kapasite)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.12f, y = bottomY)
                .width(totalWidth * 0.23f)
                .height(totalHeight * 0.08f)
                .then(if (onCapacityClick != null) Modifier.clickable { onCapacityClick() } else Modifier)
        ) {
            Text(
                text = capacityText,
                color = Color.White,
                fontSize = (totalWidth.value * 0.023f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 2. Hap (Reaktör / Enerji)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.425f, y = bottomY)
                .width(totalWidth * 0.11f)
                .height(totalHeight * 0.08f)
                .then(if (onReactorClick != null) Modifier.clickable { onReactorClick() } else Modifier)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = reactorEfficiencyText,
                    color = CyberLightBlueText,
                    fontSize = (totalWidth.value * 0.023f).sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = reactorStockText,
                    color = Color.White,
                    fontSize = (totalWidth.value * 0.023f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Hap (Lojistik Filo)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.655f, y = bottomY)
                .width(totalWidth * 0.065f)
                .height(totalHeight * 0.08f)
                .then(if (onFleetClick != null) Modifier.clickable { onFleetClick() } else Modifier)
        ) {
            Text(
                text = fleetCountText,
                color = Color.White,
                fontSize = (totalWidth.value * 0.025f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 4. Hap (Ar-Ge Teşviki Butonu Dokunma Alanı)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = totalWidth * 0.735f, y = bottomY)
                .width(totalWidth * 0.215f)
                .height(totalHeight * 0.09f)
                .clip(RoundedCornerShape(8.dp))
                .then(if (onResearchIncentiveClick != null) Modifier.clickable { onResearchIncentiveClick() } else Modifier)
        )
    }
}

/**
 * Preview Composable - Test in Jetpack Compose Preview tool with Dark Background
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PlayerDashboardCardPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        PlayerDashboardCard(
            playerName = "Kumcu",
            playerLevel = 16,
            traderTitle = "Kıdemli Tüccar",
            dayText = "Gün 4",
            xpCurrentText = "224.7K",
            xpTargetText = "275.0K",
            xpPercent = 81,
            netWorthText = "1.24 T",
            growthRateText = "+%0.0",
            incomePerMinText = "+₳ 440.4 K / dk",
            dailyIncomeText = "Günlük: +₳ 6.61 M",
            cashText = "616.1B (%9)",
            facilityValueText = "261.3M (%)",
            depotValueText = "0 (%)",
            rdValueText = "6.8B (%)",
            capacityText = "23/24 Kapasite (%74)",
            reactorEfficiencyText = "%24",
            reactorStockText = "0",
            fleetCountText = "0"
        )
    }
}
