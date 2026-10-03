package com.example.ui.screens

import com.example.ui.components.CurrencyText
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.components.formatCredit

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.components.OdometerText
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StatisticsScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateToInventory: () -> Unit = {},
    onNavigateToWeeklyGrowth: () -> Unit = {},
    onNavigateToProduction: (String?) -> Unit = {},
    onNavigateToMarket: () -> Unit = {},
    onNavigateToBorsa: () -> Unit = {},
    onNavigateToBank: () -> Unit = {},
    onNavigateToHr: () -> Unit = {},
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showLoanDialog by remember { mutableStateOf(false) }
    var showProfitDialog by remember { mutableStateOf(false) }

    val player = uiState.playerState.player
    val gameState = uiState.gameStateObj
    val businesses = uiState.businesses
    val managers = uiState.managers
    val inventory = uiState.inventoryState.items
    val marketPrices = uiState.marketState.prices
    val activeDeliveries = uiState.inventoryState.activeDeliveries
    val marketListings = uiState.marketState.marketListings
    val buyOrders = uiState.marketState.buyOrders
    val notificationHistory by SmartNotificationManager.history.collectAsStateWithLifecycle()

    val p = player ?: return
    val state = gameState ?: return
    val theme = LocalAppThemeOption.current

    // Analytics Calculations
    val activeItems = inventory.filter { it.quantity > 0 }
    val totalInventoryQuantity = activeItems.sumOf { it.quantity }
    val totalInventoryValuation = uiState.inventoryValuation
    val totalFacilityValuation = uiState.facilityValuation
    val totalConsortiumValuation = uiState.consortiumValuation

    val totalAssets = p.money + p.depositBalance + p.lockedDepositBalance + totalInventoryValuation + totalFacilityValuation + totalConsortiumValuation
    val netWorth = uiState.netWorth.coerceAtLeast(0L)

    val netDailyIncome = p.dailyIncome - p.dailyExpense

    val myListingsCount = marketListings.count { it.sellerName == p.name }
    val myBuyOrdersCount = buyOrders.count { it.buyerName == p.name }

    val warehouseFillRatio = if (p.inventoryCapacity > 0) {
        (totalInventoryQuantity.toFloat() / p.inventoryCapacity.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val averageBusinessWear = if (businesses.isNotEmpty()) {
        (businesses.sumOf { it.wearLevel.toDouble() } / businesses.size).toFloat()
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Growth Chart Quick Banner Card
            item {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToWeeklyGrowth()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = theme.surfaceColor,
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ThemeGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            CurrencyText(
                                text = tr("BÜYÜME VE VARLIK GRAFİĞİ", "GROWTH AND ASSET CHART"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = tr("Şirketinizin kuruluşundan bugüne finansal gelişimi", "Financial progress of your company since launch"),
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondaryColor
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ThemeGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Net Worth & Financial Assets Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
                    border = BorderStroke(1.dp, theme.borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CurrencyText(
                                text = tr("ŞİRKET TOPLAM NET DEĞERİ", "TOTAL COMPANY NET WORTH"),
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.textSecondaryColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OdometerText(
                                value = formatCurrency(netWorth, isEng),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 28.sp
                                ),
                                color = ThemeGold
                            )
                        }

                        HorizontalDivider(color = theme.borderColor)

                        // Asset Distribution Stacked Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CurrencyText(tr("Varlık Dağılımı", "Asset Distribution"), fontSize = 11.sp, color = theme.textSecondaryColor, fontWeight = FontWeight.Bold)
                                CurrencyText("${tr("Toplam Brüt Varlık", "Total Gross Assets")}: ${formatCurrency(totalAssets, isEng)}", fontSize = 11.sp, color = theme.textPrimaryColor)
                            }

                            val cashRatio = if (totalAssets > 0) (p.money.toFloat() / totalAssets.toFloat()).coerceIn(0f, 1f) else 0f
                            val depositRatio = if (totalAssets > 0) ((p.depositBalance + p.lockedDepositBalance).toFloat() / totalAssets.toFloat()).coerceIn(0f, 1f) else 0f
                            val inventoryRatio = if (totalAssets > 0) (totalInventoryValuation.toFloat() / totalAssets.toFloat()).coerceIn(0f, 1f) else 0f
                            val consortiumRatio = if (totalAssets > 0) (totalConsortiumValuation.toFloat() / totalAssets.toFloat()).coerceIn(0f, 1f) else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(theme.surfaceVariantColor)
                            ) {
                                if (cashRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(cashRatio)
                                            .fillMaxHeight()
                                            .background(ThemePositive)
                                    )
                                }
                                if (depositRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(depositRatio)
                                            .fillMaxHeight()
                                            .background(ThemeNeonCyan)
                                    )
                                }
                                if (inventoryRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(inventoryRatio)
                                            .fillMaxHeight()
                                            .background(ThemeGold)
                                    )
                                }
                                if (consortiumRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(consortiumRatio)
                                            .fillMaxHeight()
                                            .background(Color(0xFFA855F7))
                                    )
                                }
                            }

                            // Asset Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendItem(color = ThemePositive, label = tr("Nakit", "Cash"), value = formatCredit(p.money), theme = theme, modifier = Modifier.weight(1f))
                                LegendItem(color = ThemeNeonCyan, label = tr("Mevduat", "Deposit"), value = formatCredit(p.depositBalance + p.lockedDepositBalance), theme = theme, modifier = Modifier.weight(1f))
                                LegendItem(color = ThemeGold, label = tr("Stok", "Stock"), value = formatCredit(totalInventoryValuation), theme = theme, modifier = Modifier.weight(1f))
                                if (totalConsortiumValuation > 0L) {
                                    LegendItem(color = Color(0xFFA855F7), label = tr("Konsorsiyum", "Consortium"), value = formatCredit(totalConsortiumValuation), theme = theme, modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Detailed Sub-cards Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Loan Debt Card
                            SubMetricCard(
                                title = tr("Kredi Borcu", "Loan Debt"),
                                value = formatCredit(p.loanAmount),
                                icon = Icons.Default.AccountBalance,
                                tint = if (p.loanAmount > 0) ThemeNegative else theme.textSecondaryColor,
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                onClick = { showLoanDialog = true }
                            )

                            // Cumulative Profit Card
                            SubMetricCard(
                                title = tr("Toplam Kâr", "Total Profit"),
                                value = formatCredit(p.totalProfit),
                                icon = Icons.AutoMirrored.Filled.ShowChart,
                                tint = if (p.totalProfit >= 0) ThemePositive else ThemeNegative,
                                modifier = Modifier.weight(1f),
                                theme = theme,
                                onClick = { showProfitDialog = true }
                            )
                        }
                    }
                }
            }

            // 3. Profitability & Daily Cash Flow (Bilanço ve Nakit Akışı)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("NAKİT AKIŞI VE BİLANÇO", "CASH FLOW AND BALANCE SHEET"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = (if (netDailyIncome >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, (if (netDailyIncome >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.45f))
                        ) {
                            CurrencyText(
                                text = if (netDailyIncome >= 0) tr("Pozitif Denge", "Positive Balance") else tr("Bütçe Açığı", "Deficit"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netDailyIncome >= 0) ThemePositive else ThemeNegative,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Adaptive Layout for Mobile and Tablets
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWideScreen = maxWidth >= 540.dp
                        if (isWideScreen) {
                            // Wide screens (Tablets / Landscape): 3 Equal-width columns in a single row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Daily Income Card
                                CashFlowCard(
                                    title = tr("GÜNLÜK GELİR", "DAILY INCOME"),
                                    amount = "+${formatCredit(p.dailyIncome)}",
                                    icon = Icons.Default.TrendingUp,
                                    tint = ThemePositive,
                                    actionText = tr("Rapor ↗", "Report ↗"),
                                    subtitle = tr("Tesisler & Satışlar", "Facilities & Sales"),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showIncomeDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    theme = theme
                                )

                                // 2. Daily Expense Card
                                CashFlowCard(
                                    title = tr("GÜNLÜK GİDER", "DAILY EXPENSE"),
                                    amount = "-${formatCredit(p.dailyExpense)}",
                                    icon = Icons.Default.TrendingDown,
                                    tint = ThemeNegative,
                                    actionText = tr("Rapor ↗", "Report ↗"),
                                    subtitle = tr("Maaşlar & Vergiler", "Salaries & Taxes"),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showExpenseDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    theme = theme
                                )

                                // 3. Net Cash Flow Card
                                CashFlowCard(
                                    title = tr("NET NAKİT AKIŞI", "NET CASH FLOW"),
                                    amount = "${if (netDailyIncome >= 0) "+" else ""}${formatCredit(netDailyIncome)}",
                                    icon = Icons.Default.Analytics,
                                    tint = if (netDailyIncome >= 0) ThemePositive else ThemeNegative,
                                    actionText = tr("Büyüme ↗", "Growth ↗"),
                                    subtitle = tr("Günlük Hazine Değişimi", "Daily Treasury Delta"),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToWeeklyGrowth()
                                    },
                                    modifier = Modifier.weight(1f),
                                    theme = theme
                                )
                            }
                        } else {
                            // Mobile Phones: 2-Tier Hierarchy (Net Cash Flow Hero Banner + 2 Columns Income/Expense)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Hero Net Cash Flow Card
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onNavigateToWeeklyGrowth()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = theme.surfaceColor,
                                    border = BorderStroke(1.dp, (if (netDailyIncome >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Analytics,
                                                    contentDescription = null,
                                                    tint = if (netDailyIncome >= 0) ThemePositive else ThemeNegative,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                CurrencyText(
                                                    text = tr("NET GÜNLÜK NAKİT AKIŞI", "NET DAILY CASH FLOW"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textSecondaryColor,
                                                    letterSpacing = 0.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = theme.surfaceVariantColor,
                                                border = BorderStroke(0.5.dp, theme.borderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    CurrencyText(
                                                        text = tr("Büyüme Grafiği", "Growth Chart"),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ThemeNeonCyan,
                                                        maxLines = 1
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                        tint = ThemeNeonCyan,
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            val netFormatted = "${if (netDailyIncome >= 0) "+" else ""}${formatCredit(netDailyIncome)}"
                                            CurrencyText(
                                                text = netFormatted,
                                                fontSize = if (netFormatted.length > 14) 16.sp else 19.sp,
                                                color = if (netDailyIncome >= 0) ThemePositive else ThemeNegative,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = RobotoMonoFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            CurrencyText(
                                                text = tr("Kasaya Net Yansıyan", "Net Treasury Change"),
                                                fontSize = 9.sp,
                                                color = theme.textSecondaryColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // 2 Equal Columns: Daily Income & Daily Expense
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Daily Income Box
                                    CashFlowCard(
                                        title = tr("GÜNLÜK GELİR", "DAILY INCOME"),
                                        amount = "+${formatCredit(p.dailyIncome)}",
                                        icon = Icons.Default.TrendingUp,
                                        tint = ThemePositive,
                                        actionText = tr("Rapor ↗", "Report ↗"),
                                        subtitle = tr("Tesis & Pazar", "Facility & Market"),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showIncomeDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        theme = theme
                                    )

                                    // Daily Expense Box
                                    CashFlowCard(
                                        title = tr("GÜNLÜK GİDER", "DAILY EXPENSE"),
                                        amount = "-${formatCredit(p.dailyExpense)}",
                                        icon = Icons.Default.TrendingDown,
                                        tint = ThemeNegative,
                                        actionText = tr("Rapor ↗", "Report ↗"),
                                        subtitle = tr("Maaş & Bakım", "Salary & Maint."),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showExpenseDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        theme = theme
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Warehouse & Operations Analytics
            item {
                Surface(
                    onClick = onNavigateToInventory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = theme.surfaceColor,
                    border = BorderStroke(1.dp, theme.borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CurrencyText(
                            text = tr("DEPO VE LOJİSTİK OPERASYONLARI", "WAREHOUSE & LOGISTICS OPERATIONS"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan,
                            letterSpacing = 1.sp
                        )

                        // Warehouse Fill Status
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                                    CurrencyText(tr("Depo Doluluğu", "Warehouse Capacity"), fontSize = 12.sp, color = theme.textPrimaryColor, fontWeight = FontWeight.Bold)
                                }
                                CurrencyText(
                                    text = "$totalInventoryQuantity / ${p.inventoryCapacity} ${tr("Ton", "Tons")} (%${(warehouseFillRatio * 100).toInt()})",
                                    fontSize = 12.sp,
                                    color = if (warehouseFillRatio > 0.9f) ThemeNegative else ThemeGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { warehouseFillRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (warehouseFillRatio > 0.9f) ThemeNegative else ThemeGold,
                                trackColor = theme.surfaceVariantColor
                            )
                        }

                        HorizontalDivider(color = theme.borderColor)

                        // Logistics & Market Activity Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBadge(
                                icon = Icons.Default.LocalShipping,
                                label = tr("Yoldaki Nakliyat", "In-Transit Shipping"),
                                value = "${activeDeliveries.size} ${tr("Sevkiyat", "Shipments")}",
                                color = ThemeNeonCyan,
                                theme = theme
                            )
                            MetricBadge(
                                icon = Icons.Default.Store,
                                label = tr("İlanlarım", "My Listings"),
                                value = "$myListingsCount ${tr("İlan", "Listings")}",
                                color = ThemePositive,
                                theme = theme
                            )
                            MetricBadge(
                                icon = Icons.Default.ShoppingBag,
                                label = tr("Alım Sözleşmelerim", "My Purchase Contracts"),
                                value = "$myBuyOrdersCount ${tr("Sözleşme", "Contracts")}",
                                color = ThemeGold,
                                theme = theme
                            )
                        }

                        HorizontalDivider(color = theme.borderColor)

                        // Facilities summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = theme.textSecondaryColor, modifier = Modifier.size(16.dp))
                                CurrencyText(tr("Sahip Olunan Tesisler:", "Owned Facilities:"), fontSize = 12.sp, color = theme.textSecondaryColor)
                            }
                            CurrencyText(
                                text = "${businesses.size} ${tr("Tesis", "Facilities")} (${tr("Ort. Aşınma", "Avg. Wear")}: %${(averageBusinessWear * 100).toInt()})",
                                fontSize = 12.sp,
                                color = theme.textPrimaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. Macroeconomic Environment & Central Bank Rates
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                CurrencyText(
                                    text = tr("MAKROEKONOMİK GÖSTERGELER", "MACROECONOMIC INDICATORS"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ThemePositive.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ThemePositive)
                                    )
                                    CurrencyText(
                                        text = tr("CANLI PİYASA", "LIVE MARKET"),
                                        fontSize = 10.sp,
                                        color = ThemePositive,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroMetricItem(
                                icon = Icons.Rounded.TrendingUp,
                                label = tr("Enflasyon", "Inflation"),
                                value = "%${(state.globalInflationRate * 100).toInt()}",
                                subtitle = tr("Maliyet Endeksi", "Cost Volatility"),
                                color = ThemeNegative,
                                modifier = Modifier.weight(1f),
                                theme = theme
                            )
                            MacroMetricItem(
                                icon = Icons.Rounded.CreditCard,
                                label = tr("MB Kredi Faizi", "CB Loan Rate"),
                                value = "%${(state.centralBankLoanRate * 100).toInt()}",
                                subtitle = tr("Borçlanma Faizi", "Borrowing Rate"),
                                color = ThemeGold,
                                modifier = Modifier.weight(1f),
                                theme = theme
                            )
                            MacroMetricItem(
                                icon = Icons.Rounded.Savings,
                                label = tr("MB Mevduat", "CB Deposit Rate"),
                                value = "%${(state.centralBankDepositRate * 100).toInt()}",
                                subtitle = tr("Risksiz Getiri", "Risk-Free Yield"),
                                color = ThemePositive,
                                modifier = Modifier.weight(1f),
                                theme = theme
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = theme.surfaceVariantColor.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, theme.borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = theme.textSecondaryColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                CurrencyText(
                                    text = tr(
                                        "Merkez Bankası göstergeleri tüm piyasa emtia fiyatlarını, şirket kâr marjlarını ve banka oranlarını doğrudan etkiler.",
                                        "Central Bank metrics directly influence commodity prices, corporate margins, and bank loan/deposit rates."
                                    ),
                                    fontSize = 10.sp,
                                    color = theme.textSecondaryColor,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 7. Recent Financial Activity Feed
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyText(
                        text = tr("SON İŞLEM VE TİCARİ HAREKETLER", "RECENT TRANSACTIONS & COMMERCE"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = theme.textSecondaryColor,
                        letterSpacing = 1.sp
                    )

                    if (notificationHistory.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = theme.surfaceColor,
                            border = BorderStroke(1.dp, theme.borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CurrencyText(
                                text = tr("Henüz kayıtlı bir işlem hareketi bulunmuyor.", "There are no recorded transaction movements yet."),
                                color = theme.textSecondaryColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isEn = com.example.ui.theme.isEnglishLanguage()
                            notificationHistory.take(5).forEach { item ->
                                val resolvedProdId = item.getResolvedProductId()
                                val resolvedQuality = item.getResolvedQuality()
                                val msgText = item.getFormattedMessage(isEn)

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.surfaceColor,
                                    border = BorderStroke(1.dp, theme.borderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (resolvedProdId != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF0F1829),
                                                border = BorderStroke(0.8.dp, theme.borderColor),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    com.example.ui.components.UniversalProductIcon(
                                                        productId = resolvedProdId,
                                                        size = 20.dp
                                                    )
                                                    if (resolvedQuality != null) {
                                                        Surface(
                                                            shape = RoundedCornerShape(topStart = 3.dp),
                                                            color = resolvedQuality.badgeColor.copy(alpha = 0.95f),
                                                            modifier = Modifier.align(Alignment.BottomEnd)
                                                        ) {
                                                            Text(
                                                                text = "★${resolvedQuality.stars}",
                                                                color = Color.Black,
                                                                fontSize = 6.5.sp,
                                                                fontWeight = FontWeight.Black,
                                                                modifier = Modifier.padding(horizontal = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (item.type) {
                                                            com.example.ui.components.NotificationType.SUCCESS -> ThemePositive
                                                            com.example.ui.components.NotificationType.ALERT -> ThemeNegative
                                                            else -> ThemeNeonCyan
                                                        }
                                                    )
                                            )
                                        }

                                        CurrencyText(
                                            text = msgText,
                                            fontSize = 11.sp,
                                            color = theme.textPrimaryColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIncomeDialog) {
        com.example.ui.components.DailyIncomeBreakdownDialog(
            player = p,
            gameState = state,
            businesses = businesses,
            managers = managers,
            marketListings = marketListings,
            activeDeliveries = activeDeliveries,
            onNavigateToProduction = onNavigateToProduction,
            onNavigateToMarket = onNavigateToMarket,
            onNavigateToBorsa = onNavigateToBorsa,
            onNavigateToBank = onNavigateToBank,
            onNavigateToInventory = onNavigateToInventory,
            onNavigateToMap = onNavigateToMap,
            onNavigateToRd = onNavigateToRd,
            onDismiss = { showIncomeDialog = false }
        )
    }
    
    if (showExpenseDialog) {
        com.example.ui.components.DailyExpenseBreakdownDialog(
            player = p,
            gameState = state,
            businesses = businesses,
            managers = managers,
            onNavigateToProduction = onNavigateToProduction,
            onNavigateToHr = onNavigateToHr,
            onNavigateToBank = onNavigateToBank,
            onNavigateToMarket = onNavigateToMarket,
            onNavigateToInventory = onNavigateToInventory,
            onNavigateToMap = onNavigateToMap,
            onNavigateToRd = onNavigateToRd,
            onDismiss = { showExpenseDialog = false }
        )
    }

    if (showLoanDialog) {
        InfoDialog(
            title = tr("Kredi Borcu", "Loan Debt"),
            description = tr("Bankadan çektiğiniz kredilerin toplam ana para borcunu gösterir. Her makroekonomi döngüsünde bu tutar üzerinden faiz ödemesi tahsil edilir.\n\nKredi Borcu: ${formatCredit(p.loanAmount)}", "Shows the total principal amount of loans taken from the bank. Interest payments are collected on this amount every macroeconomic cycle.\n\nLoan Debt: ${formatCredit(p.loanAmount)}"),
            icon = Icons.Default.AccountBalance,
            color = if (p.loanAmount > 0) ThemeNegative else theme.textSecondaryColor,
            theme = theme,
            onDismiss = { showLoanDialog = false }
        )
    }

    if (showProfitDialog) {
        InfoDialog(
            title = tr("Toplam Kâr", "Total Profit"),
            description = tr("Şirketinizin kuruluşundan bu yana pazar ve ticari faaliyetlerden elde ettiği kümülatif net kârı temsil eder.\n\nToplam Kâr: ${formatCredit(p.totalProfit)}", "Represents the cumulative net profit that your company has obtained from market and commercial activities since its establishment.\n\nTotal Profit: ${formatCredit(p.totalProfit)}"),
            icon = Icons.AutoMirrored.Filled.ShowChart,
            color = if (p.totalProfit >= 0) ThemePositive else ThemeNegative,
            theme = theme,
            onDismiss = { showProfitDialog = false }
        )
    }
}

@Composable
private fun InfoDialog(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    theme: AppThemeOption,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = theme.surfaceColor,
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
                CurrencyText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimaryColor,
                    textAlign = TextAlign.Center
                )
                CurrencyText(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textSecondaryColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CurrencyText(tr("TAMAM", "OK"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun safeAmountFontSize(text: String): androidx.compose.ui.unit.TextUnit {
    return when {
        text.length >= 17 -> 11.sp
        text.length >= 13 -> 12.sp
        text.length >= 10 -> 13.5.sp
        else -> 15.sp
    }
}

@Composable
private fun CashFlowCard(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    actionText: String?,
    subtitle: String,
    modifier: Modifier = Modifier,
    theme: AppThemeOption,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = theme.surfaceColor,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(13.dp)
                    )
                    CurrencyText(
                        text = title,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (actionText != null) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = tint.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, tint.copy(alpha = 0.4f))
                    ) {
                        CurrencyText(
                            text = actionText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                            modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 1.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Amount with Auto-scaling and Anti-shift mechanism
            CurrencyText(
                text = amount,
                fontSize = safeAmountFontSize(amount),
                color = tint,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle / context
            CurrencyText(
                text = subtitle,
                fontSize = 8.5.sp,
                color = theme.textSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String,
    theme: AppThemeOption,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        CurrencyText(
            text = "$label: ",
            fontSize = 9.sp,
            color = theme.textSecondaryColor,
            maxLines = 1
        )
        CurrencyText(
            text = value,
            fontSize = 9.5.sp,
            color = theme.textPrimaryColor,
            fontWeight = FontWeight.Bold,
            fontFamily = RobotoMonoFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun SubMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    theme: AppThemeOption,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = theme.surfaceVariantColor,
        border = BorderStroke(1.dp, theme.borderColor)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
                CurrencyText(
                    text = title,
                    fontSize = 9.5.sp,
                    color = theme.textSecondaryColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            CurrencyText(
                text = value,
                fontSize = safeAmountFontSize(value),
                fontWeight = FontWeight.Bold,
                color = theme.textPrimaryColor,
                fontFamily = RobotoMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    theme: AppThemeOption
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            CurrencyText(text = label, fontSize = 9.5.sp, color = theme.textSecondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CurrencyText(text = value, fontSize = 11.5.sp, color = theme.textPrimaryColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MacroMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    theme: AppThemeOption
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = theme.surfaceVariantColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            CurrencyText(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimaryColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            CurrencyText(
                text = value,
                fontSize = 16.sp,
                color = color,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            CurrencyText(
                text = subtitle,
                fontSize = 8.5.sp,
                color = theme.textSecondaryColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

