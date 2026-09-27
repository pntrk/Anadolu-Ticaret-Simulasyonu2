package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.tr
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.calculateCompanyValuation

@Composable
fun IPOManagementPanel(viewModel: GameViewModel) {
    val snapshot by viewModel.economicSnapshotFlow.collectAsStateWithLifecycle(null)
    val player by viewModel.player.collectAsStateWithLifecycle()
    val isIpoActive = snapshot?.isIpoActive == true
    val publicShare = snapshot?.publicSharePercent ?: 0
    val totalDividendsPaid = snapshot?.totalDividendsPaid ?: 0L

    val p = player
    val snap = snapshot
    val companyValuation = remember(p?.id, p?.money, snap?.publicSharePercent) {
        viewModel.calculateCompanyValuation()
    }
    val dailyIncome = p?.dailyIncome ?: 0L
    val dailyExpense = p?.dailyExpense ?: 0L
    val dailyProfitEst = (dailyIncome - dailyExpense).coerceAtLeast(0L)
    val estimatedDailyDividend = if (isIpoActive && publicShare > 0) {
        (dailyProfitEst * (publicShare / 100.0)).toLong()
    } else {
        0L
    }
    val totalCapitalRaised = (companyValuation * (publicShare / 100.0)).toLong()
    val liquidFunds = (p?.money ?: 0L) + (p?.depositBalance ?: 0L)
    val hasEnoughFundsForDividend = liquidFunds >= estimatedDailyDividend

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, if (isIpoActive) ThemeGold.copy(alpha = 0.6f) else ThemeBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. BAŞLIK VE HALKA ARZ DURUM ŞERİDİ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isIpoActive) ThemeGold.copy(alpha = 0.2f) else ThemeNeonCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isIpoActive) ThemeGold else ThemeNeonCyan),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = if (isIpoActive) ThemeGold else ThemeNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = tr("📈 Halka Arz (IPO) & Temettü Dağıtımı", "📈 Initial Public Offering & Dividends"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            fontSize = 15.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Text(
                            text = if (isIpoActive) tr("Şirket Borsada İşlem Görüyor • Halka Açık", "Listed on Exchange • Public Company")
                            else tr("Kapalı Şirket • %100 Kurucu Sermayesi", "Private Corporation • 100% Founder Equity"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isIpoActive) ThemeGold.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isIpoActive) ThemeGold else Color.Gray)
                ) {
                    Text(
                        text = if (isIpoActive) "%$publicShare " + tr("Halka Arz", "Public") else tr("Kapalı", "Private"),
                        color = if (isIpoActive) ThemeGold else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

            // 2. DETAYLI 4'LÜ FİNANSAL KPI KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kart 1: Halka Arz Payı
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131D31),
                    border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = tr("Halka Açık Pay", "Public Share"),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "%$publicShare",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Text(
                            text = tr("Kalan: %${100 - publicShare}", "Owned: %${100 - publicShare}"),
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Kart 2: Toplanan Arz Sermayesi
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131D31),
                    border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = tr("Toplanan Fon", "Capital Raised"),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "₳${formatCredit(totalCapitalRaised)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Text(
                            text = tr("Piyasa Değeri Üzerinden", "Based on Valuation"),
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kart 3: Gece 00:00 Kesilecek Temettü
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isIpoActive && !hasEnoughFundsForDividend) Color(0xFF2E1515) else Color(0xFF131D31),
                    border = BorderStroke(0.8.dp, if (isIpoActive && !hasEnoughFundsForDividend) Color(0xFFEF5350) else Color(0xFF24344F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = tr("Gece 00:00 Temettü", "00:00 Daily Dividend"),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isIpoActive) "-₳${formatCredit(estimatedDailyDividend)}" else "₳0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIpoActive) (if (hasEnoughFundsForDividend) Color(0xFFFFB74D) else Color(0xFFEF5350)) else Color.Gray,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Text(
                            text = tr("Günlük Kârın %$publicShare'i", "%$publicShare of Daily Profit"),
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Kart 4: Toplam Dağıtılan Temettü
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131D31),
                    border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = tr("Toplam Ödenen", "Total Dividends"),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "₳${formatCredit(totalDividendsPaid)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Text(
                            text = tr("Hissedarlara Dağıtılan", "Distributed to Public"),
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            // 3. GECE 00:00 TEMETTÜ VE GÜVENCE DÖKÜMÜ (AÇIK VE ŞIK KART)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF090E18),
                border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr("📊 Gece 00:00 Temettü Kesinti Hesabı Dökümü:", "📊 Midnight Dividend Settlement Calculation:"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = tr("• Günlük Tahmini Net Kâr:", "• Est. Daily Net Profit:"), fontSize = 10.5.sp, color = Color.LightGray)
                        Text(text = "₳${formatCredit(dailyProfitEst)}", fontSize = 11.sp, color = ThemePositive, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = tr("• Halka Açık Hisse Oranı:", "• Public Shareholding:"), fontSize = 10.5.sp, color = Color.LightGray)
                        Text(text = "%$publicShare", fontSize = 11.sp, color = ThemeNeonCyan, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = tr("• Gece Kesilecek Temettü Payı:", "• Midnight Dividend Deducted:"), fontSize = 10.5.sp, color = Color.LightGray)
                        Text(text = "₳${formatCredit(estimatedDailyDividend)}", fontSize = 11.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = tr("• Kasa ve Mevduat Likiditesi:", "• Cash & Deposit Balance:"), fontSize = 10.5.sp, color = Color.LightGray)
                        Text(text = "₳${formatCredit(liquidFunds)}", fontSize = 11.sp, color = if (hasEnoughFundsForDividend) ThemePositive else Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                    }
                }
            }

            // 4. LİKİDİTE GÜVENCESİ VEYA TEMERRÜT UYARI BANNERI
            if (isIpoActive) {
                if (hasEnoughFundsForDividend) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF063319).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(20.dp))
                            Text(
                                text = tr(
                                    "Temettü Karşılama Güvencesi: Kasa ve banka mevduatınız (₳${formatCredit(liquidFunds)}) gece 00:00'daki ₳${formatCredit(estimatedDailyDividend)} temettü kesintisini güvenle karşılamaktadır.",
                                    "Dividend Coverage Secure: Your cash and bank deposits (₳${formatCredit(liquidFunds)}) safely cover the ₳${formatCredit(estimatedDailyDividend)} midnight dividend."
                                ),
                                fontSize = 11.sp,
                                color = Color(0xFFC8E6C9),
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF330E0E).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFFEF5350)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                            Text(
                                text = tr(
                                    "⚠️ Temerrüt Riski: Kasa likiditeniz (₳${formatCredit(liquidFunds)}) gece 00:00'da yapılacak ₳${formatCredit(estimatedDailyDividend)} temettü kesintisini karşılamaya yetmiyor! Yetersiz kalırsa en değerli tesisiniz haczedilerek iflas masasına devredilir (Tier 4 Mega Tesisler hariç).",
                                    "⚠️ Default Risk: Cash liquidity (₳${formatCredit(liquidFunds)}) is insufficient for the ₳${formatCredit(estimatedDailyDividend)} midnight dividend! If defaulted, your most valuable facility will be foreclosed to the Bankruptcy Desk (Tier 4 Mega Facilities protected)."
                                ),
                                fontSize = 11.sp,
                                color = Color(0xFFFFCDD2),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 5. HALKA ARZ SEÇENEKLERİ (MAKSİMUM %50)
            if (!isIpoActive || publicShare < 50) {
                Text(
                    text = if (isIpoActive) tr("Ek Halka Arz Seçenekleri (Maksimum %50):", "Additional IPO Offerings (Max 50%):")
                    else tr("Şirketinizi Halka Arz Edin (Anında Nakit Girişi):", "Take Your Company Public (Instant Capital Inflow):"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeNeonCyan,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val availableMax = 50 - publicShare

                    val ipoOptions = listOf(10, 20, 30)
                    ipoOptions.forEach { pct ->
                        val isEnabled = availableMax >= pct
                        val expectedFunds = (companyValuation * (pct / 100.0)).toLong()

                        Button(
                            onClick = { viewModel.launchIPO(pct) },
                            enabled = isEnabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeGold,
                                contentColor = Color(0xFF1A1300),
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color.Gray
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%$pct " + tr("Arz Et", "Offer"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "+₳${formatCredit(expectedFunds)}",
                                    fontSize = 9.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnabled) Color(0xFF261900) else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

