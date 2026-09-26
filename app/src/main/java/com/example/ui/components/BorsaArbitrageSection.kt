package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BorsaLimitOrder
import com.example.data.LimitOrderType
import com.example.data.Product
import com.example.ui.theme.*
import com.example.viewmodel.*

@Composable
fun BorsaArbitrageSection(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val limitOrders by viewModel.borsaLimitOrders.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val managers by viewModel.managers.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()

    val borsaMgr = managers.find { it.id == "mgr_borsa" }
    val isMgrHired = borsaMgr != null && borsaMgr.isHired
    val mgrLevel = borsaMgr?.level ?: 0

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedProductForNewOrder by remember { mutableStateOf<Product?>(null) }

    val activeOrders = limitOrders.filter { it.isActive }
    val totalRealizedProfit = limitOrders.sumOf { it.totalRealizedProfit }
    val totalExecutions = limitOrders.sumOf { it.executedCount }
    val maxCapacity = if (isMgrHired) 3 + (mgrLevel * 3) else 3

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Manager & AI Status Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = ThemeNeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyText(
                                    text = tr("🤖 AI BORSA ARBİTRAJ BOTU", "🤖 AI BORSA ARBITRAGE BOT"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = RobotoMonoFontFamily
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (activeOrders.isNotEmpty()) Color(0xFF00381B) else Color(0xFF3B2A10))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    CurrencyText(
                                        text = if (activeOrders.isNotEmpty()) tr("● 7/24 AKTİF", "● 24/7 ACTIVE") else tr("○ BEKLEMEDE", "○ IDLE"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeOrders.isNotEmpty()) ThemePositive else ThemeGold
                                    )
                                }
                            }
                            CurrencyText(
                                text = if (isMgrHired) {
                                    tr("Borsa Müdürü: Burak Koç (Lvl $mgrLevel) • Kapasite: ${limitOrders.size}/$maxCapacity Emir",
                                       "Borsa Manager: Burak Koç (Lvl $mgrLevel) • Capacity: ${limitOrders.size}/$maxCapacity Orders")
                                } else {
                                    tr("Temel Mod (${limitOrders.size}/$maxCapacity Emir) • Müdürü İşe Alarak Kapasiteyi Artır",
                                       "Basic Mode (${limitOrders.size}/$maxCapacity Orders) • Hire Manager to Expand")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Stats Dashboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KPI 1: Realized Profit
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF162032),
                        border = BorderStroke(0.5.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            CurrencyText(
                                text = tr("TOPLAM BOT KÂRI", "TOTAL BOT PROFIT"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = "+₳${formatCredit(totalRealizedProfit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }

                    // KPI 2: Total Executions
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF162032),
                        border = BorderStroke(0.5.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            CurrencyText(
                                text = tr("TOPLAM İŞLEM", "TOTAL TRADES"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = "$totalExecutions ${tr("İşlem", "Trades")}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }

                    // KPI 3: Active Bots
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF162032),
                        border = BorderStroke(0.5.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            CurrencyText(
                                text = tr("AKTİF EMİR", "ACTIVE ORDERS"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = "${activeOrders.size} / $maxCapacity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeOrders.size >= maxCapacity) ThemeGold else Color.White,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }

                // Action Button: Add New Bot / Order
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showCreateDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText(
                        text = tr("YENİ AI ARBİTRAJ / LİMİT EMRİ KUR", "CREATE NEW AI ARBITRAGE / LIMIT ORDER"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }

        // 2. Fast 1-Click Recommended AI Arbitrage Bots
        CurrencyText(
            text = tr("⚡ HIZLI 1-TIK AI ARBİTRAJ STRATEJİLERİ", "⚡ FAST 1-CLICK AI ARBITRAGE STRATEGIES"),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ThemeGold,
            fontFamily = RobotoMonoFontFamily
        )

        val quickBots = listOf(
            Product.COAL,
            Product.CRUDE_OIL,
            Product.STEEL,
            Product.IRON,
            Product.REFINED_FUEL
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickBots.take(3).forEach { prod ->
                val curPrice = marketPrices.find { it.itemId == prod.id }?.price ?: prod.basePrice
                val alreadyHasBot = limitOrders.any { it.itemId == prod.id && it.isActive }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.createSmartArbitrageBot(prod.id, quantity = 25)
                        },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF131D2D),
                    border = BorderStroke(1.dp, if (alreadyHasBot) ThemeNeonCyan else Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = prod.icon,
                            contentDescription = null,
                            tint = Color(prod.colorTint),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencyText(
                            text = prod.getDisplayName(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        CurrencyText(
                            text = "₳${formatCredit(curPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = ThemeNeonCyan,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (alreadyHasBot) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CurrencyText(
                                text = if (alreadyHasBot) tr("✓ ÇALIŞIYOR", "✓ RUNNING") else tr("+ BOTU BAŞLAT", "+ START BOT"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alreadyHasBot) ThemeNeonCyan else Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        // 3. Active Orders & Bots List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyText(
                text = tr("📋 AKTİF EMİR & BOT LİSTESİ (${limitOrders.size})", "📋 ACTIVE ORDERS & BOTS (${limitOrders.size})"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = RobotoMonoFontFamily
            )
        }

        if (limitOrders.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(0.5.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                    CurrencyText(
                        text = tr("Henüz Aktif Arbitraj Botu veya Limit Emri Yok", "No Active Arbitrage Bot or Limit Orders"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    CurrencyText(
                        text = tr(
                            "AI Arbitraj Botu sayesinde piyasa fiyatları düştüğünde otomatik alım yapılır, tavan fiyata veya kriz seviyesine çıktığında otomatik satılarak pasif kâr üretilir.",
                            "With the AI Arbitrage Bot, orders automatically buy when prices drop and sell when prices surge, generating passive profit 24/7."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                limitOrders.forEach { order ->
                    val prod = Product.values().find { it.id == order.itemId }
                    val curPrice = marketPrices.find { it.itemId == order.itemId }?.price ?: (prod?.basePrice ?: 100L)
                    val inStock = inventory.find { it.itemId == order.itemId }?.quantity ?: 0

                    LimitOrderCard(
                        order = order,
                        product = prod,
                        currentPrice = curPrice,
                        inventoryStock = inStock,
                        onToggleActive = { isActive ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleBorsaLimitOrder(order.id, isActive)
                        },
                        onDelete = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.removeBorsaLimitOrder(order.id)
                        }
                    )
                }
            }
        }
    }

    // Modal Dialog to Create Custom Limit Order / AI Bot
    if (showCreateDialog) {
        CreateLimitOrderDialog(
            viewModel = viewModel,
            marketPrices = marketPrices,
            initialProduct = selectedProductForNewOrder,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun LimitOrderCard(
    order: BorsaLimitOrder,
    product: Product?,
    currentPrice: Long,
    inventoryStock: Int,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isBuy = order.orderType == LimitOrderType.BUY_BELOW
    val isSell = order.orderType == LimitOrderType.SELL_ABOVE
    val isArbitrage = order.orderType == LimitOrderType.ARBITRAGE_AUTO

    val statusColor = if (!order.isActive) Color.Gray else if (isArbitrage) ThemeNeonCyan else if (isBuy) ThemePositive else ThemeGold

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, statusColor.copy(alpha = if (order.isActive) 0.6f else 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Product + Order Type Badge + Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product != null) {
                        Icon(
                            imageVector = product.icon,
                            contentDescription = null,
                            tint = Color(product.colorTint),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            tint = ThemeNeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = product?.getDisplayName() ?: order.itemId,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                CurrencyText(
                                    text = when (order.orderType) {
                                        LimitOrderType.BUY_BELOW -> tr("LİMİT ALIM", "LIMIT BUY")
                                        LimitOrderType.SELL_ABOVE -> tr("LİMİT SATIM", "LIMIT SELL")
                                        LimitOrderType.ARBITRAGE_AUTO -> tr("AI ARBİTRAJ", "AI ARBITRAGE")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                        CurrencyText(
                            text = "${tr("Mevcut Borsa Fiyatı", "Current Price")}: ₳${formatCredit(currentPrice)} • ${tr("Depo", "Stock")}: $inventoryStock Ton",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleActive(!order.isActive) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (order.isActive) Icons.Rounded.PauseCircleOutline else Icons.Rounded.PlayCircleOutline,
                            contentDescription = if (order.isActive) "Duraklat" else "Başlat",
                            tint = if (order.isActive) ThemeGold else ThemePositive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Sil",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)

            // Price Targets & Logic Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (isArbitrage) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyText(
                                text = "🟢 ${tr("Alış Hedefi", "Buy Target")}: <= ₳${formatCredit(order.targetPrice)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive,
                                fontFamily = RobotoMonoFontFamily
                            )
                            CurrencyText(
                                text = "🔴 ${tr("Satış Hedefi", "Sell Target")}: >= ₳${formatCredit(order.targetSellPrice)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    } else if (isBuy) {
                        CurrencyText(
                            text = "🟢 ${tr("Alış Eşiği", "Buy Trigger")}: <= ₳${formatCredit(order.targetPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemePositive,
                            fontFamily = RobotoMonoFontFamily
                        )
                    } else {
                        CurrencyText(
                            text = "🔴 ${tr("Satış Eşiği", "Sell Trigger")}: >= ₳${formatCredit(order.targetPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }

                CurrencyText(
                    text = "${order.quantity} Ton / ${tr("işlem", "trade")}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = RobotoMonoFontFamily
                )
            }

            // Realized Profit & Executed Count Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF131D2E))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Autorenew,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    CurrencyText(
                        text = "${order.executedCount} ${tr("kez uygulandı", "times executed")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                CurrencyText(
                    text = "${tr("Kazanılan Kâr", "Realized Profit")}: +₳${formatCredit(order.totalRealizedProfit)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (order.totalRealizedProfit > 0) ThemePositive else Color.Gray,
                    fontFamily = RobotoMonoFontFamily
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLimitOrderDialog(
    viewModel: GameViewModel,
    marketPrices: List<com.example.data.MarketPriceEntity>,
    initialProduct: Product?,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedProduct by remember { mutableStateOf(initialProduct ?: Product.COAL) }
    var orderType by remember { mutableStateOf(LimitOrderType.ARBITRAGE_AUTO) }
    var quantity by remember { mutableIntStateOf(50) }

    val curPrice = marketPrices.find { it.itemId == selectedProduct.id }?.price ?: selectedProduct.basePrice

    var buyPriceInput by remember(selectedProduct, curPrice) {
        mutableStateOf(((curPrice * 0.85).toLong()).toString())
    }
    var sellPriceInput by remember(selectedProduct, curPrice) {
        mutableStateOf(((curPrice * 1.25).toLong()).toString())
    }
    var autoRepeat by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0D1526),
            border = BorderStroke(1.dp, ThemeNeonCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Rounded.SmartToy, contentDescription = null, tint = ThemeNeonCyan)
                        CurrencyText(
                            text = tr("AI EMİR & ARBİTRAJ KURUCU", "AI ORDER & ARBITRAGE BUILDER"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                Divider(color = Color(0xFF1E293B))

                // Product Selector
                CurrencyText(
                    text = tr("İşlem Görecek Emtia:", "Target Commodity:"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(Product.values().toList()) { prod ->
                        val isSelected = selectedProduct.id == prod.id
                        Surface(
                            modifier = Modifier
                                .width(64.dp)
                                .clickable {
                                    selectedProduct = prod
                                    val price = marketPrices.find { it.itemId == prod.id }?.price ?: prod.basePrice
                                    buyPriceInput = ((price * 0.85).toLong()).toString()
                                    sellPriceInput = ((price * 1.25).toLong()).toString()
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF162032),
                            border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = prod.icon,
                                    contentDescription = null,
                                    tint = Color(prod.colorTint),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                CurrencyText(
                                    text = prod.getDisplayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Order Type Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF162032))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { orderType = LimitOrderType.ARBITRAGE_AUTO },
                        shape = RoundedCornerShape(4.dp),
                        color = if (orderType == LimitOrderType.ARBITRAGE_AUTO) ThemeNeonCyan else Color.Transparent
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = tr("🤖 AI ARBİTRAJ", "🤖 ARBITRAGE"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (orderType == LimitOrderType.ARBITRAGE_AUTO) Color.Black else Color.LightGray
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).clickable { orderType = LimitOrderType.BUY_BELOW },
                        shape = RoundedCornerShape(4.dp),
                        color = if (orderType == LimitOrderType.BUY_BELOW) ThemePositive else Color.Transparent
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = tr("🟢 LİMİT ALIM", "🟢 BUY LIMIT"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (orderType == LimitOrderType.BUY_BELOW) Color.Black else Color.LightGray
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).clickable { orderType = LimitOrderType.SELL_ABOVE },
                        shape = RoundedCornerShape(4.dp),
                        color = if (orderType == LimitOrderType.SELL_ABOVE) ThemeGold else Color.Transparent
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = tr("🔴 LİMİT SATIM", "🔴 SELL LIMIT"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (orderType == LimitOrderType.SELL_ABOVE) Color.Black else Color.LightGray
                            )
                        }
                    }
                }

                // Price Inputs
                if (orderType == LimitOrderType.ARBITRAGE_AUTO || orderType == LimitOrderType.BUY_BELOW) {
                    OutlinedTextField(
                        value = buyPriceInput,
                        onValueChange = { buyPriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Alış Hedef Fiyatı (<= ₳)", "Buy Target Price (<= ₳)")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePositive,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                if (orderType == LimitOrderType.ARBITRAGE_AUTO || orderType == LimitOrderType.SELL_ABOVE) {
                    OutlinedTextField(
                        value = sellPriceInput,
                        onValueChange = { sellPriceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Satış Hedef Fiyatı (>= ₳)", "Sell Target Price (>= ₳)")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeGold,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Quantity Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = tr("İşlem Miktarı: $quantity Ton", "Trade Quantity: $quantity Tons"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(10, 50, 100, 500).forEach { q ->
                            Surface(
                                modifier = Modifier.clickable { quantity = q },
                                shape = RoundedCornerShape(4.dp),
                                color = if (quantity == q) ThemeNeonCyan else Color(0xFF1E293B)
                            ) {
                                CurrencyText(
                                    text = "$q",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quantity == q) Color.Black else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val buyTarget = buyPriceInput.toLongOrNull() ?: (curPrice * 0.85).toLong()
                        val sellTarget = sellPriceInput.toLongOrNull() ?: (curPrice * 1.25).toLong()

                        val order = BorsaLimitOrder(
                            itemId = selectedProduct.id,
                            orderType = orderType,
                            targetPrice = if (orderType == LimitOrderType.SELL_ABOVE) sellTarget else buyTarget,
                            targetSellPrice = if (orderType == LimitOrderType.ARBITRAGE_AUTO) sellTarget else 0L,
                            quantity = quantity,
                            autoRepeat = autoRepeat,
                            note = "🤖 AI Arbitraj: ${selectedProduct.getDisplayName()} x$quantity Ton"
                        )
                        viewModel.addBorsaLimitOrder(order)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan)
                ) {
                    CurrencyText(
                        text = tr("🚀 EMİRİ SİSTEME GÖNDER & BAŞLAT", "🚀 SUBMIT & ACTIVATE ORDER"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }
    }
}
