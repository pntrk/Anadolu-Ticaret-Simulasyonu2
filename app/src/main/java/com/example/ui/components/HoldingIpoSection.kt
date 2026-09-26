package com.example.ui.components

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText
import com.example.ui.theme.RobotoMonoFontFamily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import com.example.ui.theme.*
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.viewmodel.GameViewModel
import com.example.data.TradeMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun HoldingIpoSection(viewModel: GameViewModel) {
    val haptic = LocalHapticFeedback.current

    val initialStampTitle = "PORTFÖY ONAYLANDI".trAuto()
    val initialStampSubtitle = "BORA HALKA ARZ & HİSSE SERTİFİKASI".trAuto()
    var showStampOverlay by remember { mutableStateOf(false) }
    var stampTitle by remember { mutableStateOf(initialStampTitle) }
    var stampSubtitle by remember { mutableStateOf(initialStampSubtitle) }

    val player by viewModel.player.collectAsStateWithLifecycle()
    val isIpoActive by viewModel.isIpoActive.collectAsStateWithLifecycle()
    val publicSharePercent by viewModel.publicSharePercent.collectAsStateWithLifecycle()
    val totalDividendsPaid by viewModel.totalDividendsPaid.collectAsStateWithLifecycle()

    val pLevel = player?.level ?: 1
    val pMoney = player?.money ?: 0L
    val companyValuation = viewModel.calculateCompanyValuation()
    val sharePrice = companyValuation / 1_000_000.0

    var showIpoDialog by remember { mutableStateOf(false) }
    var showDividendDialog by remember { mutableStateOf(false) }
    var showBuybackDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isIpoActive) ThemeGold.copy(alpha = 0.6f) else ThemeBorder)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(ThemeGold.copy(alpha = 0.12f), Color.Transparent),
                        radius = 700f
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThemeGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ThemeGold)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CorporateFare,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        CurrencyText(
                            text = if (isIpoActive) "🏢 BIST HALKA AÇIK HOLDİNG A.Ş.".trAuto() else "🏢 HOLDİNG & HALKA ARZ (IPO)".trAuto(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.White
                        )
                        CurrencyText(
                            text = if (isIpoActive) "Şirket Hisseleriniz Borsa'da İşlem Görmektedir".trAuto() else "Şirketinizi Borsa'da Kote Edin & Sermaye Toplayın".trAuto(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isIpoActive) ThemePositiveBg else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isIpoActive) ThemePositive else Color.Gray)
                ) {
                    CurrencyText(
                        text = if (isIpoActive) "● HALKA AÇIK".trAuto() else "LİMİTET ŞİRKET".trAuto(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (isIpoActive) ThemePositive else Color.Gray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // --- VALUATION METRICS CARD ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF162032),
                border = BorderStroke(1.dp, Color(0xFF2E3D56))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        CurrencyText(
                            text = "ŞİRKET TOPLAM DEĞERLEMESİ".trAuto(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.Gray
                        )
                        CurrencyText(
                            text = formatMoney(companyValuation),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        CurrencyText(
                            text = "BİRİM HİSSE FİYATI".trAuto(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.Gray
                        )
                        CurrencyText(
                            text = tr("₳${String.format(java.util.Locale.US, "%.2f", sharePrice)} / Hisse", "₳${String.format(java.util.Locale.US, "%.2f", sharePrice)} / Share"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan
                        )
                    }
                }
            }

            // --- 📊 BIST ANADOLU 30 (BIST-A30) ENDEKS KARTI ---
            val bistIndexPoints = 2840.50 + ((companyValuation.toDouble() / 1_000_000.0) * 0.045).coerceAtMost(9500.0)
            val indexChangePct = if (companyValuation > 100_000_000L) "+4.85%" else "+1.42%"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0C1424),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = ThemePositive,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = tr("📊 BIST ANADOLU 30 (XU030-A) ENDEKSİ", "📊 BIST ANATOLIA 30 (XU030-A) INDEX"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThemePositiveBg,
                            border = BorderStroke(1.dp, ThemePositive)
                        ) {
                            CurrencyText(
                                text = "▲ $indexChangePct",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive,
                                fontFamily = RobotoMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(
                                text = tr("BIST-A30 PUANI", "BIST-A30 SCORE"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            CurrencyText(
                                text = String.format(java.util.Locale.US, "%,.2f Puan", bistIndexPoints),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            CurrencyText(
                                text = tr("HOLDİNG ENDEKS AĞIRLIĞI", "HOLDING INDEX WEIGHT"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            CurrencyText(
                                text = if (companyValuation > 500_000_000L) "%14.6 (Majör Lider)" else "%3.2 (Sanayi A.Ş.)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }

            // --- IF IPO IS NOT ACTIVE ---
            if (!isIpoActive) {
                val isLevelEligible = pLevel >= 5
                val isValuationEligible = companyValuation >= 1_000_000_000L
                val canIpo = isLevelEligible || isValuationEligible

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF162032), RoundedCornerShape(4.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyText(
                        text = "HALKA ARZ KRİTERLERİ VE ŞARTLARI:".trAuto(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLevelEligible) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = if (isLevelEligible) ThemePositive else ThemeNegative,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(
                            text = tr("En az Seviye 5 Şirket Prestişi (Siz: Lvl $pLevel)", "At least Level 5 Company Prestige (You: Lvl $pLevel)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLevelEligible) Color.White else Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isValuationEligible) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = if (isValuationEligible) ThemePositive else ThemeNegative,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(
                            text = tr("Minimum ₳1.000.000.000 Toplam Şirket Değeri (Siz: ${formatMoney(companyValuation)})", "Minimum ₳1,000,000,000 Total Valuation (You: ${formatMoney(companyValuation)})"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isValuationEligible) Color.White else Color.Gray
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showIpoDialog = true
                    },
                    enabled = canIpo,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeGold,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF334155),
                        disabledContentColor = Color.Gray
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText(
                        text = "🚀 ŞİRKETİ HALKA ARZ ET (IPO BAŞLAT)".trAuto(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            } else {
                // --- IF IPO IS ACTIVE ---
                val playerPercent = 100 - publicSharePercent

                // Ownership Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CurrencyText(
                            text = tr("👤 Şirket Kurucusu: %$playerPercent", "👤 Founder: $playerPercent%"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan
                        )
                        CurrencyText(
                            text = tr("🌐 Borsa Halka Açık: %$publicSharePercent", "🌐 Public Shares: $publicSharePercent%"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(playerPercent.toFloat())
                                .fillMaxHeight()
                                .background(ThemeNeonCyan)
                        )
                        Box(
                            modifier = Modifier
                                .weight(publicSharePercent.toFloat())
                                .fillMaxHeight()
                                .background(ThemeGold)
                        )
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF162032)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            CurrencyText(
                                text = "HALKA AÇIK PORTFÖY".trAuto(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            CurrencyText(
                                text = tr("${publicSharePercent * 10_000} Hisse", "${publicSharePercent * 10_000} Shares"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF162032)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            CurrencyText(
                                text = "ÖDENEN TEMETTÜ".trAuto(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            CurrencyText(
                                text = formatMoney(totalDividendsPaid),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showIpoDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, ThemeGold),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        CurrencyText(
                            text = "📈 " + tr("Hisse Sat", "Sell Shares"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showBuybackDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, ThemeNeonCyan),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        CurrencyText(
                            text = "🏛️ " + tr("Geri Alım", "Buyback"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showDividendDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        CurrencyText(
                            text = "💰 " + tr("Temettü", "Dividend"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // --- IPO DIALOG ---
    if (showIpoDialog) {
        var selectedPercent by remember { mutableIntStateOf(20) }
        val estCapital = (companyValuation * (selectedPercent / 100.0)).toLong()

        AlertDialog(
            onDismissRequest = { showIpoDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                CurrencyText(
                    text = if (isIpoActive) "📈 Ek Hisse İhracı & Sermaye Artırımı".trAuto() else "🚀 Borsa Halka Arz (IPO) Başlat".trAuto(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyText(
                        text = "Borsa yatırımcılarına satılacak halka açıklık oranını seçin:".trAuto(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 20, 30, 40).forEach { pct ->
                            FilterChip(
                                selected = selectedPercent == pct,
                                onClick = { selectedPercent = pct },
                                label = { CurrencyText("%$pct") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeGold,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF162032)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            CurrencyText(
                                text = "TOPLANACAK TAHMİNİ SERMAYE".trAuto(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            CurrencyText(
                                text = formatMoney(estCapital),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIpoDialog = false
                        if (isIpoActive) {
                            viewModel.sellAdditionalShares(selectedPercent)
                        } else {
                            viewModel.launchIpo(selectedPercent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                ) {
                    CurrencyText("ONAYLA VE SERMAYE TOPLA".trAuto(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpoDialog = false }) {
                    CurrencyText("İptal".trAuto(), color = Color.Gray)
                }
            }
        )
    }

    // --- DIVIDEND DIALOG ---
    if (showDividendDialog) {
        var dividendAmountText by remember { mutableStateOf("500000") }
        val amount = dividendAmountText.toLongOrNull() ?: 0L

        AlertDialog(
            onDismissRequest = { showDividendDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                CurrencyText("💰 Hissedarlara Temettü Dağıt".trAuto(), fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyText(
                        text = tr("Dağıtmak istediğiniz toplam temettü miktarını girin. Halka açık hisse oranında (%$publicSharePercent) ödeme borsa yatırımcılarına aktarılır.", "Enter the total dividend amount. Payment corresponding to public share ($publicSharePercent%) will be distributed."),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = dividendAmountText,
                        onValueChange = { dividendAmountText = it },
                        label = { CurrencyText(tr("Miktar (TL)", "Amount (TL)")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDividendDialog = false
                        if (amount > 0) viewModel.distributeDividends(amount)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.Black)
                ) {
                    CurrencyText("DAĞIT".trAuto(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDividendDialog = false }) {
                    CurrencyText("İptal".trAuto(), color = Color.Gray)
                }
            }
        )
    }

    // --- BUYBACK DIALOG ---
    if (showBuybackDialog) {
        var buybackPercent by remember { mutableIntStateOf(5) }
        val cost = (companyValuation * (buybackPercent / 100.0)).toLong()

        AlertDialog(
            onDismissRequest = { showBuybackDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                CurrencyText("🏛️ Hisse Geri Alımı (Buyback)".trAuto(), fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyText(
                        text = "Borsa'daki hisselerinizi geri alarak şirketteki mülkiyet oranınızı artırın.".trAuto(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).filter { it <= publicSharePercent }.forEach { pct ->
                            FilterChip(
                                selected = buybackPercent == pct,
                                onClick = { buybackPercent = pct },
                                label = { CurrencyText("%$pct") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeNeonCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    CurrencyText(
                        text = tr("Toplam Maliyet: ${formatMoney(cost)}", "Total Cost: ${formatMoney(cost)}"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBuybackDialog = false
                        viewModel.buybackShares(buybackPercent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black)
                ) {
                    CurrencyText("GERİ AL".trAuto(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBuybackDialog = false }) {
                    CurrencyText("İptal".trAuto(), color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun OtherCompaniesMarketSection(viewModel: GameViewModel) {
    val haptic = LocalHapticFeedback.current

    val initialTitle = tr("PORTFÖY ONAYLANDI", "PORTFOLIO APPROVED")
    val initialSubtitle = tr("BORA HALKA ARZ & HİSSE SERTİFİKASI", "BIST PUBLIC OFFERING & SHARE CERTIFICATE")
    var showStampOverlay by remember { mutableStateOf(false) }
    var stampTitle by remember { mutableStateOf(initialTitle) }
    var stampSubtitle by remember { mutableStateOf(initialSubtitle) }

    val megaProjects by viewModel.megaProjects.collectAsStateWithLifecycle()
    val playerShares by viewModel.playerGuildShares.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val pMoney = player?.money ?: 0L
    val pDeposit = player?.depositBalance ?: 0L

    val activeProjects = megaProjects

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = ThemeNeonCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyText(
                    text = tr("KONSORSİYUM MARKALARI & ÜRÜN BORSASI", "CONSORTIUM BRANDS & PRODUCT EXCHANGE"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily,
                    color = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyText(tr("💼 Nakit:", "💼 Cash:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    CurrencyText("₳${formatMoney(pMoney)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyText(tr("🏦 Banka:", "🏦 Bank:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    CurrencyText("₳${formatMoney(pDeposit)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeNeonCyan, fontFamily = RobotoMonoFontFamily)
                }
            }
        }

        if (activeProjects.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF101726),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    CurrencyText(
                        text = tr("Henüz Borsaya Arz Edilmiş Ürün Bulunmuyor", "No Publicly Offered Products Yet"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    CurrencyText(
                        text = tr("Hisse senedi ticareti yapabilmek için Mega Proje Yönetim Merkezi'nden en az bir parti üretimi tamamlamanız ve borsaya ürün arz etmeniz gerekir.", "To trade stocks, you must complete at least one production batch from the Mega Project Management Center and supply products to the exchange."),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            activeProjects.forEach { proj ->
                val owned = playerShares[proj.id] ?: 0
                val initialPrice = proj.baseSharePrice
                val sharePrice = initialPrice * (1.0 + (owned * 0.002))
                val totalSharesVolume = (proj.totalItemsProduced.coerceAtLeast(1)) * 1000
                val stampTitle5 = tr("BORA PORTFÖY ONAYLANDI", "BORA PORTFOLIO APPROVED")
                val stampSubtitle5 = tr("${proj.brandName} • 5 ADET HİSSE EDİNİLDİ", "${proj.brandName} • 5 SHARES ACQUIRED")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .luxeShimmerBorder(
                            enabled = owned > 0 || proj.baseSharePrice > 500000,
                            shape = RoundedCornerShape(8.dp),
                            borderWidth = 1.dp
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF101726),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = proj.brandName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                CurrencyText(
                                    text = tr("Arz Edilen Ürün: ${proj.targetProductName}", "Offered Product: ${proj.targetProductName}"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemeNeonCyan)
                            ) {
                                CurrencyText(
                                    text = tr("₳${String.format(java.util.Locale.US, "%,.0f", sharePrice)} / Hisse", "₳${String.format(java.util.Locale.US, "%,.0f", sharePrice)} / Share"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeNeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                CurrencyText(
                                    text = tr("BAŞLANGIÇ", "START PRICE"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = "₳${String.format(java.util.Locale.US, "%,.0f", initialPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                CurrencyText(
                                    text = tr("TOPLAM HACİM", "TOTAL VOLUME"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = tr("$totalSharesVolume Adet", "$totalSharesVolume Units"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                CurrencyText(
                                    text = tr("HİSSELERİNİZ", "YOUR SHARES"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = tr("$owned Adet", "$owned Units"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (owned > 0) ThemePositive else Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.buyGuildShares(proj.id, 1)
                                },
                                enabled = (owned + 1 <= totalSharesVolume && pMoney >= sharePrice),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                CurrencyText(
                                    text = tr("+1 Al", "+1 Buy"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.buyGuildShares(proj.id, 5)
                                    stampTitle = stampTitle5
                                    stampSubtitle = stampSubtitle5
                                    showStampOverlay = true
                                },
                                enabled = (owned + 5 <= totalSharesVolume && pMoney >= sharePrice * 5),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = ThemeNeonCyan),
                                border = BorderStroke(1.dp, ThemeNeonCyan),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                CurrencyText(
                                    text = tr("+5 Al", "+5 Buy"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.sellGuildShares(proj.id, 1)
                                },
                                enabled = owned >= 1,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (owned >= 1) Color(0xFFEF5350) else Color.Gray),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                CurrencyText(
                                    text = tr("-1 Sat", "-1 Sell"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (owned >= 1) Color(0xFFEF5350) else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.sellGuildShares(proj.id, if (owned >= 5) 5 else owned)
                                },
                                enabled = owned >= 1,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (owned >= 1) Color(0xFFEF5350) else Color.Gray),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                val sellQty = if (owned in 1..4) owned else 5
                                CurrencyText(
                                    text = tr("-$sellQty Sat", "-$sellQty Sell"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (owned >= 1) Color(0xFFEF5350) else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CorporateStampOverlay(
        isVisible = showStampOverlay,
        title = stampTitle,
        subtitle = stampSubtitle,
        onDismiss = { showStampOverlay = false }
    )
}

@Composable
fun OtherCompaniesIpoSection(viewModel: GameViewModel) {
    var selectedTradeGuild by remember { mutableStateOf<com.example.data.GuildGroup?>(null) }
    var selectedTradePrice by remember { mutableStateOf(0.0) }
    val haptic = LocalHapticFeedback.current

    val initialTitle = "KÜRESEL IPO ONAYLANDI".trAuto()
    val initialSubtitle = "BORA DÜNYA HİSSE SERTİFİKASI".trAuto()
    var showStampOverlay by remember { mutableStateOf(false) }
    var stampTitle by remember { mutableStateOf(initialTitle) }
    var stampSubtitle by remember { mutableStateOf(initialSubtitle) }

    val guilds by viewModel.guilds.collectAsStateWithLifecycle()
    val playerShares by viewModel.playerGuildShares.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val crisisState by viewModel.consortiumCrisisState.collectAsStateWithLifecycle()
    val activeCityEvents by viewModel.activeCityEvents.collectAsStateWithLifecycle()
    val pMoney = player?.money ?: 0L
    val pDeposit = player?.depositBalance ?: 0L

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = ThemeGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyText(
                    text = "DİĞER ŞİRKETLERİN HALKA ARZLARI (IPO)".trAuto(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily,
                    color = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.6f),
            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyText(tr("💼 Nakit:", "💼 Cash:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    CurrencyText("₳${formatMoney(pMoney)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CurrencyText(tr("🏦 Banka:", "🏦 Bank:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    CurrencyText("₳${formatMoney(pDeposit)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeNeonCyan, fontFamily = RobotoMonoFontFamily)
                }
            }
        }

        CurrencyText(
            text = tr("Halka arz (IPO) işlemlerinde alım yaptığınızda tutar sizin bankanızdan düşer ve şirket kasasına eklenir. Satış yaptığınızda ise şirket kasasından düşer ve sizin bankanıza aktarılır.", "When buying during IPO, the amount is deducted from your bank and added to the company treasury. When selling, it is deducted from the treasury and credited to your bank."),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        val ipoGuilds = guilds.filter { it.isIpoActive }
        if (ipoGuilds.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF101726),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    CurrencyText(
                        text = "Şu anda borsa'da halka arz edilmiş (IPO) başka bir şirket bulunmuyor.".trAuto(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            ipoGuilds.forEach { guild ->
            val macroState = viewModel.macroState
            val sharePrice = com.example.data.MacroEconomyEngine.calculateCompanySharePrice(
                totalValuation = guild.megaProjectTarget,
                xpProgress = guild.memberCount,
                macroState = macroState,
                crisisState = crisisState,
                activeCityEvents = activeCityEvents
            )
            val owned = playerShares[guild.id] ?: 0
            val globalIpoStampTitle = tr("KÜRESEL IPO ONAYLANDI", "GLOBAL IPO APPROVED")
            val globalIpo10Subtitle = tr("${guild.name} • 10 ADET DÜNYA HİSSESİ EDİNİLDİ", "${guild.name} • 10 WORLD SHARES ACQUIRED")
            val globalIpo50Subtitle = tr("${guild.name} • 50 ADET DÜNYA HİSSESİ EDİNİLDİ", "${guild.name} • 50 WORLD SHARES ACQUIRED")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF101726),
                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(
                                text = guild.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            CurrencyText(
                                text = tr("Kurucu: ${guild.leaderName} • Aktif IPO", "Founder: ${guild.leaderName} • Active IPO"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeGold)
                        ) {
                            CurrencyText(
                                text = tr("₳${String.format(java.util.Locale.US, "%.2f", sharePrice)} / IPO Hisse", "₳${String.format(java.util.Locale.US, "%.2f", sharePrice)} / IPO Share"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            CurrencyText(
                                text = "ŞİRKET KASASI".trAuto(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CurrencyText(
                                text = formatMoney(guild.bankBalance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            CurrencyText(
                                text = "PORTFÖYÜNÜZ".trAuto(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CurrencyText(
                                text = tr("$owned Hisse", "$owned Shares"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (owned > 0) ThemePositive else Color.LightGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.buyIpoShares(guild.id, 10)
                                stampTitle = globalIpoStampTitle
                                stampSubtitle = globalIpo10Subtitle
                                showStampOverlay = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            CurrencyText(
                                text = tr("🚀 10 Al", "🚀 10 Buy"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.buyIpoShares(guild.id, 50)
                                stampTitle = globalIpoStampTitle
                                stampSubtitle = globalIpo50Subtitle
                                showStampOverlay = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = ThemeGold),
                            border = BorderStroke(1.dp, ThemeGold),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            CurrencyText(
                                text = tr("🚀 50 Al", "🚀 50 Buy"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.sellIpoShares(guild.id, 10)
                            },
                            enabled = owned > 0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (owned > 0) Color(0xFFEF5350) else Color.Gray),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            val sellText = if (owned in 1..9) tr("-$owned Sat", "-$owned Sell") else tr("-10 Sat", "-10 Sell")
                            CurrencyText(
                                text = sellText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (owned > 0) Color(0xFFEF5350) else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
          }
        }
    }

    CorporateStampOverlay(
        isVisible = showStampOverlay,
        title = stampTitle,
        subtitle = stampSubtitle,
        onDismiss = { showStampOverlay = false }
    )
}


@Composable
fun CompanyShareTradeModal(
    companyName: String,
    price: Double,
    ownedQuantity: Int,
    playerMoney: Long,
    onDismiss: () -> Unit,
    onBuy: (quantity: Int) -> Unit,
    onSell: (quantity: Int) -> Unit
) {
    var quantity by remember { mutableIntStateOf(10) }
    var tradeMode by remember { mutableStateOf(TradeMode.BUY) }
    val totalCost = try { Math.multiplyExact(price.toLong(), quantity.toLong()) } catch(e: Exception) { Long.MAX_VALUE }
    val maxAffordable = if (price > 0.0) (playerMoney / price).toInt().coerceAtLeast(0) else 0
    val maxSellable = ownedQuantity

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        titleContentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            CurrencyText(
                text = tr("$companyName HİSSE İŞLEMİ", "$companyName SHARE TRANSACTION"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ThemeGold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Mod Seçici
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B0F19)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (tradeMode == TradeMode.BUY) ThemePositive else Color.Transparent).clickable { tradeMode = TradeMode.BUY },
                        contentAlignment = Alignment.Center
                    ) {
                        CurrencyText("ALIM YAP".trAuto(), fontWeight = FontWeight.Bold, color = if (tradeMode == TradeMode.BUY) Color.Black else Color.Gray)
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (tradeMode == TradeMode.SELL) ThemeNegative else Color.Transparent).clickable { tradeMode = TradeMode.SELL },
                        contentAlignment = Alignment.Center
                    ) {
                        CurrencyText("SATIŞ YAP".trAuto(), fontWeight = FontWeight.Bold, color = if (tradeMode == TradeMode.SELL) Color.Black else Color.Gray)
                    }
                }
                
                CurrencyText(text = tr("Hisse Fiyatı: ₳${com.example.ui.components.formatMoney(price.toLong())}", "Share Price: ₳${com.example.ui.components.formatMoney(price.toLong())}"), color = Color.White)
                
                // Miktar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { quantity = (quantity - 10).coerceAtLeast(10) },
                        modifier = Modifier.background(Color(0xFF1E293B), CircleShape).size(36.dp)
                    ) {
                        CurrencyText("-", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    CurrencyText(
                        text = tr("$quantity Adet", "$quantity Units"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { quantity += 10 },
                        modifier = Modifier.background(Color(0xFF1E293B), CircleShape).size(36.dp)
                    ) {
                        CurrencyText("+", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Hızlı Seçim
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(10, 50, 100, 500).forEach { amount ->
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.clickable { quantity = amount }.padding(4.dp)
                        ) {
                            CurrencyText(
                                text = "$amount",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = ThemeNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (tradeMode == TradeMode.BUY) {
                    CurrencyText(text = tr("Max Alınabilir: $maxAffordable", "Max Affordable: $maxAffordable"), color = Color.Gray, fontSize = 12.sp)
                } else {
                    CurrencyText(text = tr("Sahip Olunan: $ownedQuantity", "Owned: $ownedQuantity"), color = Color.Gray, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    if (tradeMode == TradeMode.BUY) onBuy(quantity) else onSell(quantity)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (tradeMode == TradeMode.BUY) ThemePositive else ThemeNegative, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                CurrencyText(if (tradeMode == TradeMode.BUY) tr("SATIN AL (-₳${com.example.ui.components.formatMoney(totalCost)})", "BUY (-₳${com.example.ui.components.formatMoney(totalCost)})") else tr("SAT (+₳${com.example.ui.components.formatMoney(totalCost)})", "SELL (+₳${com.example.ui.components.formatMoney(totalCost)})"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("İptal".trAuto(), color = Color.Gray)
            }
        }
    )
}
