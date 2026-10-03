package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MacroEconomyEngine
import com.example.data.Product
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.sell

@Composable
fun BoardroomBriefingDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    val player by viewModel.player.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()

    // 1. Üretim Müdürü Analizi (Krizdeki Ürünler)
    val crisisItems = marketPrices.filter {
        it.isCrisis || it.borsaStock <= MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
    }
    val crisisProductNames = crisisItems.mapNotNull { item ->
        Product.values().find { it.id == item.itemId }?.getDisplayName()
    }

    // 2. Lojistik ve Satış Analizi (Depolardaki toplam stok ve satılabilir değer)
    val totalInventoryCount = inventory.sumOf { it.quantity }
    val readyToSellCrisisItems = inventory.filter { inv ->
        inv.quantity > 0 && crisisItems.any { it.itemId == inv.itemId }
    }

    // 3. Hazine Analizi
    val currentMoney = player?.money ?: 0L
    val depositBalance = player?.depositBalance ?: 0L
    val loanAmount = player?.loanAmount ?: 0L

    // 4. Operasyon & Bakım Analizi
    val wornBusinesses = businesses.filter { it.wearLevel > 0.15f }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0B132B),
            border = BorderStroke(1.5.dp, ThemeGold.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F1E3D), Color(0xFF0B132B))
                        )
                    )
            ) {
                // --- HEADER ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF14244B),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = ThemeGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, ThemeGold),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MeetingRoom,
                                        contentDescription = null,
                                        tint = ThemeGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                CurrencyText(
                                    text = tr("🏛️ MÜDÜRLER KURULU BRİFİNGİ", "🏛️ BOARD OF DIRECTORS BRIEFING"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = RobotoMonoFontFamily
                                )
                                CurrencyText(
                                    text = tr("Yönetim Kurulu Canlı Durum & Strateji Raporu", "Executive Live Strategy & Action Plan"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeNeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }

                // --- CONTENT ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. ÜRETİM MÜDÜRÜ RAPORU
                    DirectorReportCard(
                        directorTitle = tr("🏭 Üretim & Sanayi Müdürü", "🏭 Production & Industry Director"),
                        directorRole = tr("Kriz Ürünleri & Milli Üretim Seferberliği", "Crisis Commodities & National Mobilization"),
                        directorBadge = if (crisisItems.isNotEmpty()) "🚨 KRİZ ALARMI" else "✅ NORMAL",
                        badgeColor = if (crisisItems.isNotEmpty()) ThemeNegative else ThemePositive,
                        icon = Icons.Rounded.Factory,
                        reportText = if (crisisItems.isNotEmpty()) {
                            tr(
                                "🚨 Borsa rezervlerinde ${crisisProductNames.joinToString(", ")} kritik seviyenin altında (≤ 999 Ton)! Hükümet Milli Üretim Seferberliği ilan etti. Bu ürünlerde tesislerimize %50 Üretim Hızı Bonusu ve borsaya satışta %25 Devlet Teşvik Primi veriliyor. Maden ve tesisleri acilen tam kapasiteye almalıyız!",
                                "🚨 ${crisisProductNames.joinToString(", ")} reserves on exchange are critical (≤ 999 Tons)! National Mobilization declared. Factories receive 50% Speed Bonus & 25% State Subsidy Bonus for selling to exchange. Mobilize production capacity now!"
                            )
                        } else {
                            tr(
                                "Tüm borsa emtia rezervleri olağan seviyelerde. Tesislerimiz standart verimlilikte çalışmaya devam ediyor.",
                                "All exchange commodity reserves are normal. Factories are operating at standard efficiency."
                            )
                        },
                        actionButtonText = if (crisisItems.isNotEmpty()) tr("⚡ Kriz Ürünlerini Üretime Al", "⚡ Put Crisis Goods in Production") else null,
                        onAction = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            crisisItems.firstOrNull()?.let { target ->
                                viewModel.produce(target.itemId, 5)
                            }
                        }
                    )

                    // 2. LOJİSTİK & SATIŞ MÜDÜRÜ RAPORU
                    DirectorReportCard(
                        directorTitle = tr("🚚 Lojistik & Satış Müdürü", "🚚 Logistics & Sales Director"),
                        directorRole = tr("Stok Yönetimi & Spot Borsa Likiditesi", "Inventory Management & Exchange Liquidity"),
                        directorBadge = if (readyToSellCrisisItems.isNotEmpty()) "💰 YÜKSEK KÂR FIRSATI" else "📦 STOKLAR DÜZENLİ",
                        badgeColor = if (readyToSellCrisisItems.isNotEmpty()) ThemeGold else ThemeNeonCyan,
                        icon = Icons.Rounded.LocalShipping,
                        reportText = if (readyToSellCrisisItems.isNotEmpty()) {
                            val itemsList = readyToSellCrisisItems.map { "${it.quantity} Ton ${Product.values().find { p -> p.id == it.itemId }?.getDisplayName() ?: it.itemId}" }.joinToString(", ")
                            tr(
                                "Depolarımızda kriz primine uygun $itemsList hazır bekliyor! Bu malları New York Emtia Borsası'na spotta satarak +%25 Devlet Teşvik Primi ile rekor kâr elde edebiliriz.",
                                "We have $itemsList in inventory eligible for crisis subsidies! Selling these to the exchange will yield 25% state cash bonuses."
                            )
                        } else {
                            tr(
                                "Merkez depomuzda toplam $totalInventoryCount Ton ürün bulunuyor. Lojistik filolarımız sevkiyatları aksamadan yürütüyor.",
                                "Central warehouse contains $totalInventoryCount Tons of goods. Logistics fleets are running smoothly."
                            )
                        },
                        actionButtonText = if (readyToSellCrisisItems.isNotEmpty()) tr("💰 Kriz Mallarını Borsada Sat (+%25 Prim)", "💰 Sell Crisis Stock (+25% Bonus)") else null,
                        onAction = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            readyToSellCrisisItems.forEach { item ->
                                viewModel.sell(item.itemId, item.quantity)
                            }
                        }
                    )

                    // 3. HAZİNE & FİNANS MÜDÜRÜ RAPORU
                    val hasExcessCash = currentMoney > 10_000_000L && depositBalance < currentMoney
                    DirectorReportCard(
                        directorTitle = tr("👑 Hazine & Finans Müdürü", "👑 Treasury & Finance Director"),
                        directorRole = tr("Likidite, Faiz & Kredi Yönetimi", "Liquidity, Interest & Debt Management"),
                        directorBadge = if (loanAmount > 0L) "⚠️ KREDİ BORCU VAR" else if (hasExcessCash) "💡 FAİZ FIRSATI" else "💎 DENGELİ",
                        badgeColor = if (loanAmount > 0L) Color(0xFFFFB74D) else if (hasExcessCash) ThemeNeonCyan else ThemePositive,
                        icon = Icons.Rounded.AccountBalance,
                        reportText = if (loanAmount > 0L) {
                            tr(
                                "Şirketimizin bankaya ₳${formatCredit(loanAmount)} kredi borcu bulunuyor. Faiz giderlerini sıfırlamak için ilk etapta borcu kapatmayı öneriyorum.",
                                "The holding has ₳${formatCredit(loanAmount)} in outstanding bank loans. I recommend paying off loans to stop interest expenses."
                            )
                        } else if (hasExcessCash) {
                            tr(
                                "Kasada ₳${formatCredit(currentMoney)} nakit atıl bekliyor. Günlük risksiz mevduat faizi kazanmak için bu parayı banka hesabına bağlamalıyız.",
                                "We have ₳${formatCredit(currentMoney)} sitting idle in cash. We should deposit this to generate daily compounding interest."
                            )
                        } else {
                            tr(
                                "Hazine likiditemiz ve banka mevduatlarımız dengeli oranda yönetiliyor. Günlük nakit akışımız pozitif seyrediyor.",
                                "Treasury liquidity and bank deposits are balanced. Daily cash flow is positive."
                            )
                        },
                        actionButtonText = if (loanAmount > 0L && currentMoney >= loanAmount) {
                            tr("💵 Kredi Borcunu Kapat", "💵 Settle Bank Loan")
                        } else if (hasExcessCash) {
                            tr("🏦 ₳${formatCredit(currentMoney / 2)} Mevduata Yatır", "🏦 Deposit ₳${formatCredit(currentMoney / 2)}")
                        } else null,
                        onAction = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (loanAmount > 0L && currentMoney >= loanAmount) {
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.RepayLoan(loanAmount))
                            } else if (hasExcessCash) {
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.DepositMoney(currentMoney / 2))
                            }
                        }
                    )

                    // 4. İNSAN KAYNAKLARI & OPERASYON MÜDÜRÜ RAPORU
                    DirectorReportCard(
                        directorTitle = tr("👥 İnsan Kaynakları & Operasyon Müdürü", "👥 HR & Operations Director"),
                        directorRole = tr("Tesis Bakımı, Personel & Verimlilik", "Plant Maintenance, Staff & Efficiency"),
                        directorBadge = if (wornBusinesses.isNotEmpty()) "🔧 BAKIM GEREKİYOR (${wornBusinesses.size})" else "⭐ TESİSLER MÜKEMMEL",
                        badgeColor = if (wornBusinesses.isNotEmpty()) Color(0xFFFF7043) else ThemePositive,
                        icon = Icons.Rounded.Engineering,
                        reportText = if (wornBusinesses.isNotEmpty()) {
                            tr(
                                "${wornBusinesses.size} adet üretim tesisimizde aşınma seviyesi yüksek! Aşınan fabrikalarda üretim süresi uzar ve arıza riski doğar. Tesisleri derhal bakıma almalıyız.",
                                "${wornBusinesses.size} factories have high wear and tear! Worn facilities slow down production times. Perform general overhaul immediately."
                            )
                        } else {
                            tr(
                                "Tüm sanayi tesislerimizin makineleri ve personeli en yüksek kondisyonda çalışıyor. Yıpranma oranı kontrol altında.",
                                "All industrial facilities and workforce are in peak condition. Wear and tear is under control."
                            )
                        },
                        actionButtonText = if (wornBusinesses.isNotEmpty()) tr("🔧 Tüm Tesisleri Bakıma Al", "🔧 Repair All Facilities") else null,
                        onAction = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            wornBusinesses.forEach { biz ->
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.MaintainBusiness(biz))
                            }
                        }
                    )
                }

                // --- FOOTER CLOSE BUTTON ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF14244B),
                    border = BorderStroke(1.dp, ThemeBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGold),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            CurrencyText(
                                text = tr("BRİFİNGİ TAMAMLA & GÖREVE DÖN", "COMPLETE BRIEFING & RETURN TO WORK"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1A1202),
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectorReportCard(
    directorTitle: String,
    directorRole: String,
    directorBadge: String,
    badgeColor: Color,
    icon: ImageVector,
    reportText: String,
    actionButtonText: String?,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C38),
        border = BorderStroke(1.dp, Color(0xFF1E325A))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ThemeGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        CurrencyText(
                            text = directorTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        CurrencyText(
                            text = directorRole,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    CurrencyText(
                        text = directorBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0B1428),
                border = BorderStroke(1.dp, Color(0xFF162544))
            ) {
                CurrencyText(
                    text = reportText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (actionButtonText != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    CurrencyText(
                        text = actionButtonText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001A24),
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }
    }
}
