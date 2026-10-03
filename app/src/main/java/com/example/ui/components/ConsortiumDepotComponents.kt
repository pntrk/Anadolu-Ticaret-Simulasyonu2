package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

enum class ConsortiumDepotSubTab(val titleTr: String, val titleEn: String) {
    STOCK("Depo & Raflar", "Depot & Shelves"),
    ACTIVE_LISTINGS("Aktif İlanlar", "Active Listings"),
    SETTINGS("Protokol & Ayarlar", "Protocol & Settings")
}

@Composable
fun ConsortiumPresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = if (isSelected) ThemeGold.copy(alpha = 0.25f) else Color(0xFF161F30),
        border = BorderStroke(1.dp, if (isSelected) ThemeGold else ThemeBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            CurrencyText(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontFamily = RobotoMonoFontFamily,
                color = if (isSelected) ThemeGold else Color.LightGray
            )
        }
    }
}

/**
 * 3D / İzometrik Kargo Ambarı Çizimi ve İnteraktif Raf İnceleme
 */
@Composable
fun ConsortiumDepotBayCanvas(
    fillFraction: Float,
    warehouseStock: Int,
    capacity: Int,
    project: MegaProject,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showInspector by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF050A14),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showInspector = !showInspector
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                val w = size.width
                val h = size.height

                // Arka plan ortam ızgarası
                val girderColor = Color(0xFF142033)
                drawLine(color = girderColor, start = Offset(0f, 6f), end = Offset(w, 6f), strokeWidth = 3f)

                for (lamp in 1..4) {
                    val lx = w * (lamp / 5f)
                    drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.7f), radius = 3.5f, center = Offset(lx, 6f))
                }

                // 3 Katlı Çelik Raf Sistemi
                val shelfColor = Color(0xFF1E2D4A)
                val numShelves = 3
                val shelfSpacing = (h - 16f) / (numShelves + 0.6f)

                for (i in 1..numShelves) {
                    val y = 14f + i * shelfSpacing
                    drawLine(color = shelfColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 3f)
                    drawLine(color = Color(0xFF0F172A), start = Offset(0f, y + 2f), end = Offset(w, y + 2f), strokeWidth = 1.5f)

                    for (c in 0..4) {
                        val cx = c * (w / 4f)
                        drawLine(color = Color(0xFF10192A), start = Offset(cx, y - shelfSpacing), end = Offset(cx, y), strokeWidth = 2f)
                    }
                }

                // Sandık ve Paletler
                val maxBoxes = 24
                val filledBoxes = (fillFraction * maxBoxes).toInt().coerceAtLeast(if (warehouseStock > 0) 1 else 0)
                val cols = 8
                val boxWidth = (w - 24f) / cols
                val boxHeight = shelfSpacing * 0.72f

                var count = 0
                for (row in 0 until numShelves) {
                    val shelfY = 14f + (row + 1) * shelfSpacing
                    for (col in 0 until cols) {
                        if (count < filledBoxes) {
                            val bx = 8f + col * boxWidth
                            val by = shelfY - boxHeight - 2f

                            // Ahşap Palet Tabanı
                            drawRoundRect(
                                color = Color(0xFF78350F),
                                topLeft = Offset(bx, shelfY - 3.5f),
                                size = Size(boxWidth * 0.88f, 3.5f),
                                cornerRadius = CornerRadius(1f, 1f)
                            )

                            // 3D Kargo Sandığı
                            val crateColor = if (count % 2 == 0) Color(0xFFD97706) else Color(0xFF0284C7)
                            drawRoundRect(
                                color = crateColor,
                                topLeft = Offset(bx, by),
                                size = Size(boxWidth * 0.88f, boxHeight - 3.5f),
                                cornerRadius = CornerRadius(2.5f, 2.5f)
                            )

                            // Çelik Kuşak Şeridi
                            drawLine(
                                color = Color(0xFF0F172A),
                                start = Offset(bx + (boxWidth * 0.44f), by),
                                end = Offset(bx + (boxWidth * 0.44f), by + boxHeight - 3.5f),
                                strokeWidth = 1.2f
                            )

                            // LED Durum Işığı
                            val ledColor = if (count % 3 == 0) Color(0xFF34D399) else Color(0xFF67E8F9)
                            drawCircle(
                                color = ledColor,
                                radius = 1.8f,
                                center = Offset(bx + 4f, by + 4f)
                            )

                            // Vurgu Çerçevesi
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.25f),
                                topLeft = Offset(bx, by),
                                size = Size(boxWidth * 0.88f, boxHeight - 3.5f),
                                cornerRadius = CornerRadius(2.5f, 2.5f),
                                style = Stroke(width = 0.7f)
                            )
                        }
                        count++
                    }
                }
            }

            // Sektör Etiketi
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFF0B132B).copy(alpha = 0.85f),
                    border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                ) {
                    CurrencyText(
                        text = "BAY-04 // KLİMATİZE",
                        fontSize = 7.5.sp,
                        color = ThemeNeonCyan,
                        fontFamily = RobotoMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Boş Stok Bilgisi
            if (warehouseStock == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0B1424).copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, Color(0xFF1E2D4A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(14.dp))
                            CurrencyText(
                                text = tr("Depoda Henüz Ürün Yok (Üretim Tamamlandığında İstiflenir)", "Warehouse Empty (Stacked upon production completion)"),
                                fontSize = 9.sp,
                                color = Color.LightGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Tıklayınca Açılan Kargo Künyesi
            if (showInspector && warehouseStock > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0A1120).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyText(
                            text = "📦 SERİ: #CONS-${project.id.take(4).uppercase()}",
                            fontSize = 8.sp,
                            color = ThemeGold,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        CurrencyText(
                            text = "• KALİTE: ${project.qualityTier.gradeCode}",
                            fontSize = 8.sp,
                            color = Color(project.qualityTier.badgeColor),
                            fontFamily = RobotoMonoFontFamily
                        )
                        CurrencyText(
                            text = "• STOK: $warehouseStock/$capacity",
                            fontSize = 8.sp,
                            color = ThemeNeonCyan,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * Konsorsiyum Aktif Pazar İlanları Bölümü
 */
@Composable
fun ConsortiumActiveListingsView(
    project: MegaProject,
    isLeader: Boolean,
    listings: List<MarketListing>,
    onCancelListing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    if (listings.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, ThemeBorder),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                CurrencyText(
                    text = tr("Konsorsiyumun Aktif Pazar İlanı Yok", "No Active Consortium Market Listings"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
                CurrencyText(
                    text = tr(
                        "Depodaki konsorsiyum ürünlerini pazarda satışa sunmak için 'Depo & Raflar' sekmesinden 'Pazarda Sat' butonunu kullanabilirsiniz.",
                        "Use the 'Sell on Market' button in the 'Depot & Shelves' tab to list consortium products on the marketplace."
                    ),
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listings.forEach { listing ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF101726),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, ThemeNeonCyan)
                                ) {
                                    CurrencyText(
                                        text = "${listing.quantity} " + tr("Adet", "Units"),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = ThemeNeonCyan,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                                CurrencyText(
                                    text = project.targetProductName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(project.qualityTier.badgeColor).copy(alpha = 0.2f),
                                border = BorderStroke(0.6.dp, Color(project.qualityTier.badgeColor))
                            ) {
                                CurrencyText(
                                    text = project.qualityTier.gradeCode,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(project.qualityTier.badgeColor),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Fiyat & Gelir Detayları
                        val totalListingValue = listing.quantity * listing.pricePerUnit
                        val premiumPct = if (project.unitBatchPrice > 0) {
                            ((listing.pricePerUnit - project.unitBatchPrice).toFloat() / project.unitBatchPrice * 100).toInt()
                        } else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                CurrencyText(
                                    text = tr("Birim Satış Fiyatı: ", "Unit Price: ") + formatCredit(listing.pricePerUnit),
                                    fontSize = 9.5.sp,
                                    color = ThemeGold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                if (premiumPct != 0) {
                                    val prefix = if (premiumPct > 0) "+" else ""
                                    CurrencyText(
                                        text = tr("Piyasa Fiyatına Göre: ", "vs Spot Market: ") + "$prefix%$premiumPct",
                                        fontSize = 8.sp,
                                        color = if (premiumPct >= 0) ThemePositive else ThemeNegative
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                CurrencyText(
                                    text = tr("Toplam Beklenen Gelir", "Total Expected Revenue"),
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                                CurrencyText(
                                    text = formatCredit(totalListingValue),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold
                                )
                            }
                        }

                        // İlan İptal Butonu (Sadece Başkana)
                        if (isLeader) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCancelListing(listing.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(3.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2D1822),
                                    contentColor = Color(0xFFFF6B6B)
                                ),
                                contentPadding = PaddingValues(vertical = 5.dp)
                            ) {
                                Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("İlanı İptal Et ve Depoya İade Al", "Cancel Listing & Return to Warehouse"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
