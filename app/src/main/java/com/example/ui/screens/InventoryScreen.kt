package com.example.ui.screens

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText
import com.example.ui.components.UniversalProductIcon
import com.example.ui.components.formatCredit
import androidx.lifecycle.compose.collectAsStateWithLifecycle


import com.example.ui.theme.trAuto
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.isEnglishLanguage

import androidx.compose.animation.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryEntity
import com.example.data.ItemQuality
import com.example.data.Product
import com.example.data.ProductTier
import com.example.ui.components.AppButton
import com.example.ui.components.InventoryScreenSkeleton
import com.example.ui.components.QualityBadge
import com.example.ui.components.NotificationType
import com.example.ui.components.ParticleManager
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.TurkishLiraIcon
import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ThemeBackground
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNegativeBg
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.ThemePositiveBg
import com.example.ui.theme.ThemeSurface
import com.example.viewmodel.GameViewModel
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateToMarket: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val inventory = uiState.inventoryState.items
    val marketPrices = uiState.marketState.prices
    val player = uiState.playerState.player
    val gameState = uiState.gameStateObj
    val isSalesExecHired = uiState.managers.find { it.id == "mgr_sales" }?.isHired == true
    val isSalesExecActive = uiState.managers.find { it.id == "mgr_sales" }?.let { it.isHired && it.isActive } == true
    val reservedInventory = uiState.inventoryState.reservedItems
    val totalReservedTon = reservedInventory.values.sum()
    val isEnglish = isEnglishLanguage()

    val activeDeliveries by viewModel.activeDeliveries.collectAsStateWithLifecycle()

    var selectedItemForSale by remember { mutableStateOf<InventoryEntity?>(null) }
    var selectedFilterTier by remember { mutableStateOf<ProductTier?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("VALUE_DESC") } // VALUE_DESC, QTY_DESC, NAME_ASC
    var activeStorageTab by remember { mutableIntStateOf(0) } // 0 = Merkez Depo, 1 = Tesis Depoları, 2 = Sevkiyatlar

    var isLoadingInventory by remember { mutableStateOf(true) }
    var filteredItems by remember { mutableStateOf<List<InventoryEntity>>(emptyList()) }
    var activeItems by remember { mutableStateOf<List<InventoryEntity>>(emptyList()) }
    var totalTonCount by remember { mutableIntStateOf(0) }
    var totalValuation by remember { mutableLongStateOf(0L) }

    val maxCapacity = player?.inventoryCapacity ?: 5000
    val fillRatio = (totalTonCount.toFloat() / maxCapacity.toFloat()).coerceIn(0f, 1f)
    val netWorth = (player?.money ?: 0L) + totalValuation + (player?.depositBalance ?: 0L) - (player?.loanAmount ?: 0L)

    // Warehouse upgrade cost calculation matching ViewModel formula
    val upgradesDone = ((player?.inventoryCapacity ?: 5000) - 5000) / 2500
    val upgradeCost = 25000L + (upgradesDone * 15000L)

    // Offload inventory calculations, valuation and multi-tier filtering to Dispatchers.IO
    LaunchedEffect(inventory, marketPrices, selectedFilterTier, searchQuery, sortBy) {
        withContext(Dispatchers.IO) {
            val act = inventory.filter { it.quantity > 0 }
            val tons = inventory.sumOf { it.quantity }
            val valuation = act.sumOf { item ->
                val product = Product.values().find { it.id == item.baseProductId }
                val basePrice = marketPrices.find { it.itemId == item.baseProductId }?.price ?: product?.basePrice ?: 100L
                (basePrice * item.quality.priceMultiplier * item.quantity).toLong()
            }

            val filtered = act.filter { item ->
                val product = Product.values().find { it.id == item.baseProductId }
                val matchesTier = selectedFilterTier == null || product?.tier == selectedFilterTier
                val matchesSearch = searchQuery.isEmpty() || item.itemId.contains(searchQuery, ignoreCase = true) || product?.getDisplayName()?.contains(searchQuery, ignoreCase = true) == true
                matchesTier && matchesSearch
            }.sortedWith { a, b ->
                val productA = Product.values().find { it.id == a.baseProductId }
                val productB = Product.values().find { it.id == b.baseProductId }
                val priceA = ((marketPrices.find { it.itemId == a.baseProductId }?.price ?: productA?.basePrice ?: 100L) * a.quality.priceMultiplier).toLong()
                val priceB = ((marketPrices.find { it.itemId == b.baseProductId }?.price ?: productB?.basePrice ?: 100L) * b.quality.priceMultiplier).toLong()
                val valA = priceA * a.quantity
                val valB = priceB * b.quantity

                when (sortBy) {
                    "VALUE_DESC" -> valB.compareTo(valA)
                    "QTY_DESC" -> b.quantity.compareTo(a.quantity)
                    else -> a.itemId.compareTo(b.itemId)
                }
            }

            withContext(Dispatchers.Main) {
                activeItems = act
                totalTonCount = tons
                totalValuation = valuation
                filteredItems = filtered
                isLoadingInventory = false
            }
        }
    }

    var gridColumnCount by remember { mutableIntStateOf(2) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Color(0xFF1E2638), ThemeBackground), radius = 1200f))
    ) {
        AnimatedContent(
            targetState = isLoadingInventory,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "InventorySkeletonCrossfade"
        ) { loading ->
            if (loading) {
                InventoryScreenSkeleton(modifier = Modifier.fillMaxSize())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumnCount),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
                ) {
            // 0. TOP STORAGE MODE SELECTOR (MERKEZ DEPO VS TESİS DEPOLARI VS SEVKİYATLAR)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCenterActive = activeStorageTab == 0
                    val isFacilityActive = activeStorageTab == 1
                    val isShipmentsActive = activeStorageTab == 2

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { activeStorageTab = 0 },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCenterActive) Color(0xFF1E293B) else Color(0xFF101726),
                        border = BorderStroke(1.dp, if (isCenterActive) ThemeNeonCyan else ThemeBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CurrencyText("🏛️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = tr("Merkez Depo", "Central", isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCenterActive) Color.White else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { activeStorageTab = 1 },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFacilityActive) Color(0xFF1E293B) else Color(0xFF101726),
                        border = BorderStroke(1.dp, if (isFacilityActive) ThemeGold else ThemeBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CurrencyText("🏭", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = tr("Tesisler", "Facilities", isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isFacilityActive) Color.White else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { activeStorageTab = 2 },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isShipmentsActive) Color(0xFF1E293B) else Color(0xFF101726),
                        border = BorderStroke(1.dp, if (isShipmentsActive) Color(0xFF38BDF8) else ThemeBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CurrencyText("🚚", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = tr("Sevkiyat", "Shipments", isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isShipmentsActive) Color.White else Color.Gray,
                                maxLines = 1
                            )
                            if (activeDeliveries.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = ThemeGold,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CurrencyText(
                                            text = "${activeDeliveries.size}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeStorageTab == 2) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InventoryDeliveriesView(
                        activeDeliveries = activeDeliveries,
                        isEnglish = isEnglish
                    )
                }
            } else if (activeStorageTab == 1) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FacilityWarehousesView(
                        businesses = uiState.businesses,
                        marketPrices = marketPrices,
                        onIntent = { viewModel.handleIntent(it) },
                        isEnglish = isEnglish
                    )
                }
            } else {
            // 1. SLEEK COMPACT WAREHOUSE CAPACITY HEADER (FULL WIDTH SPAN)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF101726),
                    border = BorderStroke(1.dp, if (fillRatio > 0.85f) ThemeNegative.copy(alpha = 0.6f) else ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Top Info Row: Title & Stock Variety & Upgrade
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Title & Stock Variety
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warehouse,
                                            contentDescription = null,
                                            tint = ThemeNeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    CurrencyText(
                                        text = stringResource(R.string.inv_stock_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    CurrencyText(
                                        text = "${activeItems.size} " + tr("Çeşit Ürün", "Types of Product", isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeNeonCyan,
                                        fontSize = 10.sp,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Compact Upgrade Button
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.UpgradeWarehouseCapacity)
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeNeonCyan,
                                    contentColor = Color(0xFF002026)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Upgrade,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                CurrencyText(
                                    text = "+2.5kT (${formatMoney(upgradeCost)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Capacity Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyText(
                                text = "$totalTonCount / $maxCapacity ${stringResource(R.string.inv_ton)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (fillRatio > 0.85f) ThemeNegative else Color.LightGray,
                                maxLines = 1
                            )

                            val progressColor = if (fillRatio > 0.85f) ThemeNegative else if (fillRatio > 0.6f) ThemeGold else ThemeNeonCyan
                            LinearProgressIndicator(
                                progress = { fillRatio },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = progressColor,
                                trackColor = Color(0xFF1E283A)
                            )

                            CurrencyText(
                                text = "%${(fillRatio * 100).toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = progressColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 1B. AUTOMATIC RESERVED STOCK BANNER
            if (totalReservedTon > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF131F33),
                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, ThemeGold),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AssignmentTurnedIn,
                                        contentDescription = null,
                                        tint = ThemeGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = "📦 " + tr("Depo Müdürü Otomatik Ayrılan Stok", "Warehouse Manager Auto Reserved Stock"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                CurrencyText(
                                    text = tr("Pazardaki sözleşmeler ve tedarik talepleri için toplam", "A total of") + " $totalReservedTon " + tr("Ton stok koruma altında ayrıldı.", "Tons of stock is reserved under protection for contracts and supply requests in the market."),
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // 2. STREAMLINED SEARCH & FILTER CONTROL BAR (FULL WIDTH SPAN)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Search Input & Column Grid Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { CurrencyText(stringResource(R.string.inv_search_hint), color = Color.Gray, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(4.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeNeonCyan,
                                unfocusedBorderColor = ThemeBorder,
                                focusedContainerColor = Color(0xFF101726),
                                unfocusedContainerColor = Color(0xFF101726),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Grid Columns Toggle Button (2-column vs 3-column)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF101726),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(44.dp)
                                .clickable {
                                    gridColumnCount = if (gridColumnCount == 2) 3 else 2
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (gridColumnCount == 2) Icons.Rounded.GridView else Icons.Rounded.ViewComfy,
                                    contentDescription = tr("Sütun", "Columns"),
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = "${gridColumnCount}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }

                        // Sort Selector Button
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF101726),
                            border = BorderStroke(1.dp, ThemeBorder),
                            modifier = Modifier
                                .height(44.dp)
                                .clickable {
                                    sortBy = when (sortBy) {
                                        "VALUE_DESC" -> "QTY_DESC"
                                        "QTY_DESC" -> "NAME_ASC"
                                        else -> "VALUE_DESC"
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = when (sortBy) {
                                        "VALUE_DESC" -> stringResource(R.string.inv_sort_value)
                                        "QTY_DESC" -> stringResource(R.string.inv_sort_qty)
                                        else -> stringResource(R.string.inv_sort_az)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Category Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            com.example.ui.components.GameChip(
                                selected = selectedFilterTier == null,
                                text = tr("Tümü", "All") + " (${activeItems.size})",
                                onClick = { selectedFilterTier = null }
                            )
                        }

                        item {
                            com.example.ui.components.GameChip(
                                selected = selectedFilterTier == ProductTier.TIER_1,
                                text = "🌾 " + tr("Tier 1 Ham", "Tier 1 Raw"),
                                onClick = {
                                    selectedFilterTier = if (selectedFilterTier == ProductTier.TIER_1) null else ProductTier.TIER_1
                                }
                            )
                        }

                        item {
                            com.example.ui.components.GameChip(
                                selected = selectedFilterTier == ProductTier.TIER_2,
                                text = "🏭 " + tr("Tier 2 İşlenmiş", "Tier 2 Processed"),
                                onClick = {
                                    selectedFilterTier = if (selectedFilterTier == ProductTier.TIER_2) null else ProductTier.TIER_2
                                }
                            )
                        }

                        item {
                            com.example.ui.components.GameChip(
                                selected = selectedFilterTier == ProductTier.TIER_3,
                                text = "📱 " + tr("Tier 3 Teknoloji", "Tier 3 Technology"),
                                onClick = {
                                    selectedFilterTier = if (selectedFilterTier == ProductTier.TIER_3) null else ProductTier.TIER_3
                                }
                            )
                        }

                        item {
                            com.example.ui.components.GameChip(
                                selected = selectedFilterTier == ProductTier.TIER_4,
                                text = "🚀 " + tr("Tier 4 Mega Proje", "Tier 4 Mega Project"),
                                onClick = {
                                    selectedFilterTier = if (selectedFilterTier == ProductTier.TIER_4) null else ProductTier.TIER_4
                                }
                            )
                        }
                    }
                }
            }

            // 3. EXPANDED INVENTORY STOCK GRID ITEMS
            if (filteredItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF101726))
                            .border(1.dp, ThemeBorder, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1A2130),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AllInbox,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            CurrencyText(
                                text = if (activeItems.isEmpty()) tr("DEPO ŞU ANDA BOŞ", "DEPOT IS CURRENTLY EMPTY") else tr("EŞLEŞEN STOK BULUNAMADI", "NO MATCHING STOCK FOUND"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CurrencyText(
                                text = if (activeItems.isEmpty())
                                    tr("Tesislerinizde üretim başlatarak veya borsadan emtia satın alarak depoda stok biriktirebilirsiniz.", "You can build up stock in the warehouse by starting production in your facilities or purchasing commodities from the exchange.")
                                else
                                    tr("Arama terimini değiştirerek veya filtreleri sıfırlayarak mevcut ürünlerinizi listeleyebilirsiniz.", "You can list your available products by changing the search query or resetting filters."),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredItems, key = { _, it -> it.itemId }) { index, item ->
                    com.example.ui.components.AnimatedListItem(index = index) {
                        val baseProductId = item.baseProductId
                        val quality = item.quality
                        val product = Product.values().find { it.id == baseProductId }
                        val itemName = product?.displayName ?: baseProductId
                        val itemIcon = product?.icon ?: Icons.Rounded.Inventory2
                        val resolvedCountry = viewModel.resolveNaturalBorsaCountry(baseProductId, player?.currentCity)
                        val priceEntity = marketPrices.find { it.itemId == baseProductId && it.originCountry == resolvedCountry }
                        val baseSpotPrice = priceEntity?.price ?: product?.basePrice ?: 100L
                        val calculatedSpotPrice = (baseSpotPrice * quality.priceMultiplier).toLong()
                        val isUsd = priceEntity?.effectiveIsUsd ?: (resolvedCountry != "Türkiye" && resolvedCountry != "Türkiye")
                        val totalValuationItem = calculatedSpotPrice * item.quantity
                        val brandColor = if (product != null) Color(product.colorTint) else ThemeNeonCyan

                        CommodityStockCard(
                            product = product,
                            itemName = itemName,
                            itemIcon = itemIcon,
                            quantity = item.quantity,
                            reservedQuantity = reservedInventory[item.itemId] ?: 0,
                            spotPrice = calculatedSpotPrice,
                            totalValuation = totalValuationItem,
                            brandColor = brandColor,
                            compact = gridColumnCount >= 3,
                            isUsd = isUsd,
                            quality = quality,
                            onListOnMarket = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedItemForSale = item
                            },
                            onInstantSell = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.SellToBorsa(item.itemId, item.quantity))
                                SmartNotificationManager.show("${item.quantity} " + tr("Ton", "Tons", isEnglish) + " [${quality.starsText}] $itemName " + tr("spot fiyattan (", "sold at spot price (", isEnglish) + formatCredit(totalValuationItem) + tr(") satıldı!", ")!", isEnglish), NotificationType.SUCCESS)
                            }
                        )
                    }
                }
            }
            }
        }
    }
}
}

    // 5. PAZARA İLAN VER DİALOGU
    if (selectedItemForSale != null) {
        val item = selectedItemForSale!!
        val baseId = item.baseProductId
        val product = Product.values().find { it.id == baseId }
        val itemName = product?.displayName ?: baseId
        val resolvedCountry = viewModel.resolveNaturalBorsaCountry(baseId, player?.currentCity)
        val priceEntity = marketPrices.find { it.itemId == baseId && it.originCountry == resolvedCountry }
        val baseSpotPrice = priceEntity?.price ?: product?.basePrice ?: 100L
        val spotPrice = (baseSpotPrice * item.quality.priceMultiplier).toLong()
        val isUsd = priceEntity?.effectiveIsUsd ?: (resolvedCountry != "Türkiye" && resolvedCountry != "Türkiye")

        ListingCreationDialog(
            item = item,
            product = product,
            itemName = itemName,
            spotPrice = spotPrice,
            isUsd = isUsd,
            onDismiss = { selectedItemForSale = null },
            onSubmit = { qty, price ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.CreateMarketListing(item.itemId, qty, price))
                val success = true
                if (success) {
                    SmartNotificationManager.show("$qty " + tr("Ton", "Tons", isEnglish) + " $itemName " + tr("B2B pazarda ilana eklendi!", "added to the B2B market listings!", isEnglish), NotificationType.SUCCESS)
                    ParticleManager.spawnCelebration()
                    selectedItemForSale = null
                } else {
                    SmartNotificationManager.show(tr("Hata: İlan verilirken bir sorun oluştu!", "Error: A problem occurred while listing!", isEnglish), NotificationType.ALERT)
                }
            }
        )
    }
}

// ==========================================
// COMPONENT: COMMODITY STOCK CARD
// ==========================================
@Composable
fun CommodityStockCard(
    product: Product?,
    itemName: String,
    itemIcon: androidx.compose.ui.graphics.vector.ImageVector,
    quantity: Int,
    reservedQuantity: Int = 0,
    spotPrice: Long,
    totalValuation: Long,
    brandColor: Color,
    compact: Boolean = false,
    isUsd: Boolean = false,
    quality: ItemQuality = ItemQuality.STAR_1,
    onListOnMarket: () -> Unit,
    onInstantSell: () -> Unit
) {
    val tierText = when (product?.tier) {
        ProductTier.TIER_1 -> "T1"
        ProductTier.TIER_2 -> "T2"
        ProductTier.TIER_3 -> "T3"
        ProductTier.TIER_4 -> "T4"
        else -> "T1"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF121824),
        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(if (compact) 6.dp else 8.dp)) {
            // Header Row: Quantity Pill + Reserved Badge + Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = brandColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, brandColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(brandColor)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText(
                            text = "$quantity T",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = brandColor,
                            fontSize = if (compact) 8.5.sp else 9.5.sp,
                            maxLines = 1
                        )
                    }
                }

                if (reservedQuantity > 0) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = ThemeGold.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, ThemeGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            CurrencyText(
                                text = "${reservedQuantity}T",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = ThemeGold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E283A)
                ) {
                    CurrencyText(
                        text = tierText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))

            // Icon + Title Block
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (product != null) {
                    com.example.ui.components.ProductIconBadge(
                        product = product,
                        size = if (compact) 34.dp else 40.dp,
                        showSectorBadge = false
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.4f)),
                        modifier = Modifier.size(if (compact) 32.dp else 38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = itemIcon,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                CurrencyText(
                    text = itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = if (compact) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Kalite Rozeti
                QualityBadge(
                    quality = quality.toProductQuality(),
                    size = 9.dp,
                    showLabel = true,
                    fontSize = 8,
                    modifier = Modifier.padding(vertical = 1.dp)
                )

                CurrencyText(
                    text = "Birim: ".trAuto() + formatCredit(spotPrice),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    color = Color.Gray,
                    fontFamily = RobotoMonoFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


            Spacer(modifier = Modifier.height(4.dp))

            // Total Value Display Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = Color(0xFF182030),
                border = BorderStroke(0.5.dp, ThemeBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!compact) {
                        CurrencyText(
                            text = "${stringResource(R.string.inv_stock_value)}:",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = formatCredit(totalValuation),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = if (compact) 8.5.sp else 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons Row (Pazara İlan Ver & Anında Sat)
            if (compact) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppButton(
                        onClick = onListOnMarket,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeNeonCyan,
                            contentColor = Color(0xFF002026)
                        ),
                        contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sell,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onInstantSell,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeGold),
                        contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppButton(
                        onClick = onListOnMarket,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeNeonCyan,
                            contentColor = Color(0xFF002026)
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sell,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        CurrencyText(
                            text = tr("İlan Ver", "List"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onInstantSell,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeGold),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        CurrencyText(
                            text = tr("Sat", "Sell"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: LISTING CREATION DIALOG
// ==========================================
@Composable
fun ListingCreationDialog(
    item: InventoryEntity,
    product: Product?,
    itemName: String,
    spotPrice: Long,
    isUsd: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (quantity: Int, pricePerUnit: Long) -> Unit
) {
    var saleQuantityText by remember { mutableStateOf(item.quantity.toString()) }
    var salePriceText by remember { mutableStateOf(spotPrice.toString()) }
    val isEnglish = isEnglishLanguage()

    val currentQty = saleQuantityText.toIntOrNull() ?: 0
    val currentPrice = salePriceText.toLongOrNull() ?: 0L
    val totalRevenue = currentQty * currentPrice
    val brandColor = if (product != null) Color(product.colorTint) else ThemeNeonCyan

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = brandColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if(product!=null) UniversalProductIcon(product, 18.dp, brandColor) else Icon(Icons.Rounded.Inventory2, null, tint=brandColor, modifier=Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            text = "$itemName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = item.quality.badgeColor.copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, item.quality.badgeColor.copy(alpha = 0.8f))
                        ) {
                            CurrencyText(
                                text = "${item.quality.starsText} ${item.quality.label}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = item.quality.badgeColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    CurrencyText(
                        text = tr("B2B Pazarında İlana Çıkarın", "List on B2B Marketplace"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Stock Availability Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF182030),
                    border = BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("Mevcut Depo Stoğu:", "Current Warehouse Stock:"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        CurrencyText(
                            text = "${item.quantity} " + "Ton".trAuto(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan
                        )
                    }
                }

                // Quantity Input & Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = saleQuantityText,
                        onValueChange = { saleQuantityText = it },
                        label = { CurrencyText(tr("Satış Miktarı (Ton)", "Sales Quantity (Tons)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeNeonCyan,
                            unfocusedBorderColor = ThemeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Percentage Preset Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PresetChip(label = "%25", onClick = { saleQuantityText = (item.quantity * 0.25f).toInt().coerceAtLeast(1).toString() }, modifier = Modifier.weight(1f))
                        PresetChip(label = "%50", onClick = { saleQuantityText = (item.quantity * 0.50f).toInt().coerceAtLeast(1).toString() }, modifier = Modifier.weight(1f))
                        PresetChip(label = "%100", onClick = { saleQuantityText = item.quantity.toString() }, modifier = Modifier.weight(1f))
                    }
                }

                // Price Input & Tweaks
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = salePriceText,
                        onValueChange = { salePriceText = it },
                        label = { CurrencyText(tr("Birim Fiyat (₳ / Ton)", "Unit Price (₳ / Ton)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeGold,
                            unfocusedBorderColor = ThemeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PresetChip(label = "-10%", onClick = { salePriceText = (spotPrice * 0.90f).toLong().toString() }, modifier = Modifier.weight(1f))
                        PresetChip(label = tr("Piyasa", "Market"), onClick = { salePriceText = spotPrice.toString() }, modifier = Modifier.weight(1f))
                        PresetChip(label = "+10%", onClick = { salePriceText = (spotPrice * 1.10f).toLong().toString() }, modifier = Modifier.weight(1f))
                    }
                }

                // Smart Price Assistant & Expected Revenue Box
                val diffPercent = if (spotPrice > 0) ((currentPrice - spotPrice).toFloat() / spotPrice.toFloat() * 100).toInt() else 0
                val color = if (diffPercent > 0) ThemeNegative else if (diffPercent < 0) ThemePositive else Color.Gray
                val prefix = if (diffPercent > 0) "+" else ""
                
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(2.dp), color = Color(0xFF161F33)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        CurrencyText(tr("Piyasa Ortalaması: ", "Market Average: ") + formatCredit(spotPrice), color = Color.Gray, fontSize = 12.sp)
                        val relationshipText = if (diffPercent > 0) tr("üstünde", "above") else if (diffPercent < 0) tr("altında", "below") else tr("tam seviyesinde", "on par")
                        CurrencyText(tr("Sizin Fiyatınız piyasanın %$prefix$diffPercent ", "Your Price is %$prefix$diffPercent ") + relationshipText + tr(".", "."), color = color, fontSize = 11.sp)
                        if (diffPercent > 10) {
                            CurrencyText(tr("Satılma ihtimali düşük.", "Low probability of being sold."), color = ThemeNegative, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val stateTax = (totalRevenue * 0.05f).toLong()
                        val netRevenue = totalRevenue - stateTax
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("Brüt Gelir:", "Gross Revenue:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            CurrencyText(formatCredit(totalRevenue), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("Ticaret Vergisi (%5):", "Trade Tax (%5):"), style = MaterialTheme.typography.labelSmall, color = ThemeNegative)
                            CurrencyText("-${formatCredit(stateTax)}", style = MaterialTheme.typography.labelSmall, color = ThemeNegative)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(tr("NET KAZANÇ:", "NET EARNINGS:"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeGold)
                            CurrencyText(formatCredit(netRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ThemeGold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    if (currentQty <= 0) {
                        SmartNotificationManager.show(tr("Lütfen 0'dan büyük bir miktar giriniz!", "Please enter a quantity greater than 0!", isEnglish), NotificationType.INFO)
                        return@AppButton
                    }
                    if (currentQty > item.quantity) {
                        SmartNotificationManager.show(tr("Depoda yalnızca", "There is only", isEnglish) + " ${item.quantity} " + tr("ton stok var!", "tons of stock in the warehouse!", isEnglish), NotificationType.INFO)
                        return@AppButton
                    }
                    if (currentPrice <= 0) {
                        SmartNotificationManager.show(tr("Lütfen geçerli bir birim fiyat giriniz!", "Please enter a valid unit price!", isEnglish), NotificationType.INFO)
                        return@AppButton
                    }

                    onSubmit(currentQty, currentPrice)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeNeonCyan,
                    contentColor = Color(0xFF002026)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                CurrencyText(tr("İlanı Yayınla", "Publish Listing"), fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText(tr("İptal", "Cancel"), color = Color.Gray)
            }
        }
    )
}

@Composable
fun PresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(2.dp),
        color = Color(0xFF1A2130),
        border = BorderStroke(1.dp, ThemeBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            CurrencyText(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = RobotoMonoFontFamily,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun FacilityWarehousesView(
    businesses: List<com.example.data.BusinessEntity>,
    marketPrices: List<com.example.data.MarketPriceEntity>,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    isEnglish: Boolean
) {
    val totalFacilityStored = businesses.sumOf { it.getStoredTotalQuantity() }
    val totalFacilityCapacity = businesses.sumOf { it.getEffectiveStorageCapacity() }
    val fillRatio = if (totalFacilityCapacity > 0) totalFacilityStored.toFloat() / totalFacilityCapacity.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Global Facility Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF101726),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText("🏭", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            CurrencyText(
                                text = tr("Tesis Depoları Özet", "Facility Storage Summary", isEnglish),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily
                            )
                            CurrencyText(
                                text = "${businesses.size} " + tr("Aktif Tesis Deposu", "Active Facility Depots", isEnglish),
                                fontSize = 11.sp,
                                color = ThemeNeonCyan
                            )
                        }
                    }

                    CurrencyText(
                        text = "$totalFacilityStored / $totalFacilityCapacity Ton",
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { fillRatio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = ThemeNeonCyan,
                    trackColor = Color(0xFF1E283A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bulk Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        onClick = { onIntent(com.example.viewmodel.GameIntent.TransferAllFacilityStock(null)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        CurrencyText("🚚 " + tr("Tümünü Merkez Depoya Aktar", "Transfer All to HQ", isEnglish), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = { onIntent(com.example.viewmodel.GameIntent.SellAllFacilityStockOnBorsa(null)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        CurrencyText("💰 " + tr("Tümünü Borsada Sat", "Sell All on Borsa", isEnglish), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of Facilities and their specific storages
        if (businesses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CurrencyText(
                    text = tr("Henüz açılmış bir tesisiniz bulunmuyor.", "You don't have any open facilities yet.", isEnglish),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            businesses.forEach { biz ->
                val city = com.example.data.cities.find { it.id == biz.cityId }
                val isUsd = city?.country != "Türkiye"
                val flagEmoji = city?.countryFlag ?: "🇹🇷"
                val facName = Product.values().find { it.facilityId == biz.type || it.id == biz.type }?.getFacilityName(isEnglish) ?: biz.type
                val storedMap = biz.getStoredItemsMap()
                val storedTotal = biz.getStoredTotalQuantity()
                val capacity = biz.getEffectiveStorageCapacity()
                val bizFill = if (capacity > 0) storedTotal.toFloat() / capacity.toFloat() else 0f

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131A2B),
                    border = BorderStroke(1.dp, ThemeBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Facility Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyText(flagEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    CurrencyText(
                                        text = "${city?.name ?: biz.cityId} - $facName (Lv.${biz.level})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    CurrencyText(
                                        text = tr("Tesis Deposu: ", "Facility Storage: ", isEnglish) + "$storedTotal / $capacity Ton",
                                        fontSize = 11.sp,
                                        color = if (bizFill >= 0.9f) ThemeNegative else ThemeNeonCyan
                                    )
                                }
                            }

                            if (storedTotal > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onIntent(com.example.viewmodel.GameIntent.TransferAllFacilityStock(biz.id)) },
                                        modifier = Modifier.size(32.dp).background(ThemeNeonCyan.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        CurrencyText("🚚", fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = { onIntent(com.example.viewmodel.GameIntent.SellAllFacilityStockOnBorsa(biz.id)) },
                                        modifier = Modifier.size(32.dp).background(ThemeGold.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        CurrencyText("💰", fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { bizFill.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = if (bizFill >= 0.9f) ThemeNegative else ThemeNeonCyan,
                            trackColor = Color(0xFF1E283A)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stored Items in this Facility
                        if (storedMap.isEmpty()) {
                            CurrencyText(
                                text = tr("Depo boş", "Depot is empty", isEnglish),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                storedMap.forEach { (itemId, qty) ->
                                    if (qty > 0) {
                                        val baseId = com.example.data.ItemQuality.extractBaseProductId(itemId)
                                        val quality = if (itemId.contains("_star")) com.example.data.ItemQuality.extractQuality(itemId) else com.example.data.QualityCraftingService.getQualityByFacilityLevel(biz.level)
                                        val product = Product.values().find { it.id == baseId }
                                        val prodName = product?.getDisplayName(isEnglish) ?: baseId
                                        val rawPrice = marketPrices.find { it.itemId == baseId && it.originCountry == (city?.country ?: "Türkiye") }?.price ?: product?.basePrice ?: 100L
                                        val marketPrice = (rawPrice * quality.priceMultiplier).toLong()
                                        val totalVal = marketPrice * qty
                                        val priceText = formatCredit(totalVal)

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0A1020),
                                            border = BorderStroke(0.6.dp, ThemeBorder.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (product != null) {
                                                        UniversalProductIcon(product = product, size = 24.dp)
                                                    } else {
                                                        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            CurrencyText(prodName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(2.dp),
                                                                color = quality.badgeColor.copy(alpha = 0.2f),
                                                                border = BorderStroke(0.6.dp, quality.badgeColor.copy(alpha = 0.8f))
                                                            ) {
                                                                CurrencyText(
                                                                    text = "${quality.starsText}",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = quality.badgeColor,
                                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                                )
                                                            }
                                                        }
                                                        CurrencyText("$qty Ton • ${quality.label} • Değer: $priceText", fontSize = 10.sp, color = ThemeGold)
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    AppButton(
                                                        onClick = { onIntent(com.example.viewmodel.GameIntent.TransferFacilityStock(biz.id, itemId, qty)) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan.copy(alpha = 0.2f), contentColor = ThemeNeonCyan),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        CurrencyText(tr("Aktar", "Transfer", isEnglish), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    AppButton(
                                                        onClick = { onIntent(com.example.viewmodel.GameIntent.SellFacilityStockOnBorsa(biz.id, itemId, qty)) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold.copy(alpha = 0.2f), contentColor = ThemeGold),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        CurrencyText(tr("Sat", "Sell", isEnglish), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: INVENTORY DELIVERIES VIEW
// ==========================================
@Composable
fun InventoryDeliveriesView(
    activeDeliveries: List<com.example.data.DeliveryItem>,
    isEnglish: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. ÖZET LOJİSTİK KARTI
        val totalTonsInTransit = activeDeliveries.sumOf { it.quantity }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF101726),
            border = BorderStroke(1.dp, ThemeBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText("🚚", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(
                            text = tr("Yoldaki Borsa & Lojistik Sevkiyatları", "Exchange & Logistics In Transit", isEnglish),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (activeDeliveries.isNotEmpty()) ThemeGold.copy(alpha = 0.15f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (activeDeliveries.isNotEmpty()) ThemeGold else Color.Gray)
                    ) {
                        CurrencyText(
                            text = "${activeDeliveries.size} " + tr("Aktif Sevkiyat", "Active Shipments", isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeDeliveries.isNotEmpty()) ThemeGold else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        CurrencyText(
                            text = tr("Yoldaki Toplam Yük", "Total Cargo In Transit", isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        CurrencyText(
                            text = "$totalTonsInTransit " + tr("Ton", "Tons", isEnglish),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        CurrencyText(
                            text = tr("Lojistik Durumu", "Logistics Status", isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        CurrencyText(
                            text = if (activeDeliveries.isNotEmpty()) tr("Sevkiyatlar Sürüyor", "In Transit", isEnglish) else tr("Hatlar Boşta", "Idle", isEnglish),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeDeliveries.isNotEmpty()) ThemePositive else Color.LightGray
                        )
                    }
                }
            }
        }

        // 2. SEVKİYAT LİSTESİ VEYA BOŞ DURUM
        if (activeDeliveries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101726))
                    .border(1.dp, ThemeBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1A2130),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurrencyText("🚚", fontSize = 28.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    CurrencyText(
                        text = tr("YOLDA AKTİF SEVKİYAT BULUNMUYOR", "NO SHIPMENTS CURRENTLY IN TRANSIT", isEnglish),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CurrencyText(
                        text = tr(
                            "Borsadan satın aldığınız veya tesislerinizden borsa depolarına sevk ettiğiniz mallar yoldayken burada anlık olarak takip edilir.",
                            "Purchases from the exchange or goods dispatched to exchange warehouses will be tracked live here.",
                            isEnglish
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 11.5.sp
                    )
                }
            }
        } else {
            activeDeliveries.forEach { delivery ->
                val prod = Product.values().find { it.id == delivery.itemId }
                val prodName = prod?.getDisplayName(isEnglish) ?: delivery.itemId.uppercase()
                val originCityName = if (delivery.originCityId == "istanbul") "🏛️ İstanbul Borsa Deposu"
                    else if (delivery.originCityId == "new_york") "🏛️ New York Borsa Deposu"
                    else "🏙️ " + (com.example.data.cities.find { it.id == delivery.originCityId }?.name ?: delivery.originCityId)

                val destCityName = if (delivery.isConsortiumDelivery) "🏭 ${com.example.data.cities.find { it.id == delivery.destinationCityId }?.name ?: delivery.destinationCityId} (Konsorsiyum)"
                    else if (delivery.destinationCityId == "istanbul") "🏛️ İstanbul Borsa Deposu"
                    else if (delivery.destinationCityId == "new_york") "🏛️ New York Borsa Deposu"
                    else "🏙️ " + (com.example.data.cities.find { it.id == delivery.destinationCityId }?.name ?: delivery.destinationCityId)

                val now = System.currentTimeMillis()
                val elapsed = now - delivery.startTimeMs
                val remainingMs = (delivery.totalDurationMs - elapsed).coerceAtLeast(0L)
                val remainingSeconds = (remainingMs / 1000L).toInt()
                val remMin = remainingSeconds / 60
                val remSec = remainingSeconds % 60
                val timeText = if (remMin > 0) "${remMin}dk ${remSec}sn" else "${remSec}sn"
                val progress = (elapsed.toFloat() / delivery.totalDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
                val percentInt = (progress * 100).toInt()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF111927),
                    border = BorderStroke(1.dp, if (delivery.isOutboundSale) ThemeGold.copy(alpha = 0.5f) else ThemeNeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (prod != null) {
                                    UniversalProductIcon(product = prod, size = 26.dp)
                                } else {
                                    Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    CurrencyText(
                                        text = "${delivery.quantity} Ton $prodName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    CurrencyText(
                                        text = if (delivery.isOutboundSale) tr("Borsaya Satış Sevkiyatı", "Borsa Outbound Sale", isEnglish) else tr("Merkez Depoya Sevkiyat", "Inbound Warehouse Delivery", isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (delivery.isOutboundSale) ThemeGold else ThemePositive,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(0.5.dp, ThemeBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(
                                        text = "⏱️ $timeText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeNeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Güzergah Satırı
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0A101C)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = originCityName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                                CurrencyText(
                                    text = "➔",
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                CurrencyText(
                                    text = destCityName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Canlı İlerleme Çubuğu
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CurrencyText(
                                    text = tr("Sevkiyat İlerlemesi", "Transit Progress", isEnglish),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    color = Color.Gray
                                )
                                CurrencyText(
                                    text = "%$percentInt",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (delivery.isOutboundSale) ThemeGold else ThemePositive
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (delivery.isOutboundSale) ThemeGold else ThemePositive,
                                trackColor = Color(0xFF1E283A)
                            )
                        }
                    }
                }
            }
        }
    }
}
