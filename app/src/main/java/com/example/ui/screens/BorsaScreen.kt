package com.example.ui.screens

import com.example.ui.components.CurrencyText

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationType

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.MarketPriceEntity
import com.example.data.Product
import com.example.data.ProductTier
import com.example.data.cities
import com.example.data.getEligibleCities
import com.example.data.getCountryFlagEmoji
import com.example.data.ForexRateManager
import com.example.ui.components.formatCurrencyByUsd
import com.example.ui.components.formatDollar
import com.example.ui.components.formatLira
import com.example.ui.components.formatCredit
import com.example.ui.components.luxeShimmerBorder
import androidx.compose.animation.core.Animatable
import com.example.viewmodel.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import com.example.ui.components.AppButton
import com.example.ui.components.formatStockTons
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.ui.theme.ThemeBackground
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNegativeBg
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.ThemePositiveBg
import com.example.ui.theme.ThemeSurface
import com.example.viewmodel.GameViewModel

import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.components.GlassCard
import com.example.ui.components.neonBorder
import com.example.ui.components.glassmorphism
import androidx.compose.material.icons.filled.Lock
import com.example.data.TradeMode


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorsaScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel // Temporary for phase 3/4 remaining functions
) {
    val pricesState = uiState.marketState.prices
    val guildsState = uiState.guildsState.guilds
    val inventoryState = uiState.inventoryState.items
    val gameStateObj = uiState.gameStateObj
    val playerState = uiState.playerState.player
    
    val isAutoSellActiveState = uiState.managers.find { it.id == "mgr_logistics" }?.let { it.isHired && it.isActive } == true
    val isAutoBuyActiveState = uiState.managers.find { it.id == "mgr_borsa" }?.let { it.isHired && it.isActive } == true
    val isAutoProduceActiveState = uiState.managers.find { it.id == "mgr_prod" }?.let { it.isHired && it.isActive } == true
    
    val isOnlineRegisteredState = uiState.settingsState.isOnlineRegistered
    val megaProjectsState = uiState.consortiumState.megaProjects
    val playerSharesState = uiState.guildsState.playerGuildShares
    val priceHistoryState = uiState.marketState.priceHistory
    val crisisState = uiState.consortiumState.crisisState
    val isExpertModeState = uiState.settingsState.isExpertMode
    val activeDeliveries by viewModel.activeDeliveries.collectAsStateWithLifecycle()
    val limitOrders by viewModel.borsaLimitOrders.collectAsStateWithLifecycle()
    val activeLimitOrdersCount = remember(limitOrders) { limitOrders.count { it.isActive } }
    val nextHourlySyncMs by viewModel.nextHourlyBorsaSyncRemainingMs.collectAsStateWithLifecycle()
    val nextHourlyMinutes = (nextHourlySyncMs / 60000L).coerceAtLeast(0L)
    val nextHourlySeconds = ((nextHourlySyncMs % 60000L) / 1000L).coerceAtLeast(0L)
    val nextHourlyCountdownStr = String.format(java.util.Locale.US, "%02d:%02d", nextHourlyMinutes, nextHourlySeconds)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf<ProductTier?>(null) } // null = All
    var showOnlyInStock by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("PRICE_DESC") } // PRICE_DESC, PRICE_ASC, NAME, STOCK
    var selectedProductForTrade by remember { mutableStateOf<Product?>(null) }
    var selectedOriginCountryForTrade by remember { mutableStateOf<String?>(null) }
    var selectedProductForSupplyTree by remember { mutableStateOf<Product?>(null) }
    var selectedBorsaTab by remember { mutableIntStateOf(0) } // 0 = Emtia Borsası, 1 = Şirket Borsası, 2 = AI Arbitraj

    val haptic = LocalHapticFeedback.current
    val theme = LocalAppThemeOption.current
     

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWidePcScreen = maxWidth >= 720.dp
        val itemsPerRow = if (isWidePcScreen) 3 else 2

        Column(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = if (theme.isDark) listOf(Color(0xFF1E2638), theme.backgroundColor) else listOf(Color(0xFFE2E8F0), theme.backgroundColor), radius = 1200f))) {
        // Top Live Marquee Ticker (Uzman Modda Aktif)
        if (isExpertModeState) {
            if (selectedBorsaTab == 0) {
                MarketTickerTape(uiState = uiState)
            } else if (selectedBorsaTab == 1) {
                CompanyTickerTape(uiState = uiState)
            }
        }



        // Top Tab Selector: Emtia Borsası vs Şirketler vs AI Arbitraj
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isCommodityActive = selectedBorsaTab == 0
            val isSirketActive = selectedBorsaTab == 1
            val isArbitrageActive = selectedBorsaTab == 2

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedBorsaTab = 0
                    },
                shape = RoundedCornerShape(6.dp),
                color = if (isCommodityActive) Color(0xFF1E293B) else Color(0xFF101726),
                border = BorderStroke(1.dp, if (isCommodityActive) ThemeNeonCyan else ThemeBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = if (isCommodityActive) ThemeNeonCyan else Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = tr("EMTİA", "COMMODITIES"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isCommodityActive) Color.White else Color.Gray
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedBorsaTab = 1
                    },
                shape = RoundedCornerShape(6.dp),
                color = if (isSirketActive) Color(0xFF1E293B) else Color(0xFF101726),
                border = BorderStroke(1.dp, if (isSirketActive) ThemeGold else ThemeBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CorporateFare,
                        contentDescription = null,
                        tint = if (isSirketActive) ThemeGold else Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = tr("ŞİRKETLER", "COMPANIES"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSirketActive) Color.White else Color.Gray
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1.15f)
                    .height(42.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedBorsaTab = 2
                    },
                shape = RoundedCornerShape(6.dp),
                color = if (isArbitrageActive) Color(0xFF1E293B) else Color(0xFF101726),
                border = BorderStroke(1.dp, if (isArbitrageActive) ThemeNeonCyan else ThemeBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = null,
                        tint = if (isArbitrageActive) ThemeNeonCyan else ThemeGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = if (activeLimitOrdersCount > 0) "AI ARBİTRAJ ($activeLimitOrdersCount)" else tr("AI ARBİTRAJ", "AI ARBITRAGE"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isArbitrageActive) Color.White else Color.LightGray
                    )
                }
            }
        }

        if (selectedBorsaTab == 1) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    com.example.ui.components.OtherCompaniesMarketSection(viewModel = viewModel)
                }
            }
        } else if (selectedBorsaTab == 2) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    com.example.ui.components.BorsaArbitrageSection(viewModel = viewModel)
                }
            }
        } else {
            // FILTER & SORT PRODUCTS with optimized derivedStateOf
            val filteredProducts by remember(searchQuery, selectedTierFilter, showOnlyInStock, sortBy, pricesState, inventoryState, selectedBorsaTab) {
                derivedStateOf {
                    pricesState.mapNotNull { priceEntity -> val prod = Product.values().find { it.id == priceEntity.itemId }; if (prod != null) Pair(prod, priceEntity) else null }.filter { (prod, priceEntity) ->
                        val matchesSearch = searchQuery.isEmpty() || prod.getDisplayName().contains(searchQuery, ignoreCase = true) || prod.id.contains(searchQuery, ignoreCase = true)
                        val matchesTier = selectedTierFilter == null || prod.tier == selectedTierFilter
                        val stock = inventoryState.filter { it.baseProductId == prod.id }.sumOf { it.quantity }
                        val matchesStock = !showOnlyInStock || stock > 0
                        matchesSearch && matchesTier && matchesStock
                    }.sortedWith { a, b ->
                        val (prodA, priceEntityA) = a
                        val (prodB, priceEntityB) = b
                        val priceA = priceEntityA.price
                        val priceB = priceEntityB.price
                        val stockA = inventoryState.filter { it.baseProductId == prodA.id }.sumOf { it.quantity }
                        val stockB = inventoryState.filter { it.baseProductId == prodB.id }.sumOf { it.quantity }

                        when (sortBy) {
                            "PRICE_DESC" -> priceB.compareTo(priceA)
                            "PRICE_ASC" -> priceA.compareTo(priceB)
                            "STOCK" -> stockB.compareTo(stockA)
                            else -> prodA.getDisplayName().compareTo(prodB.getDisplayName())
                        }
                    }
                }
            }

            val chunkedFiltered by remember(filteredProducts, itemsPerRow) {
                derivedStateOf { filteredProducts.chunked(itemsPerRow) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 3. SEARCH & CATEGORY FILTER BAR
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search Text Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { CurrencyText(stringResource(R.string.borsa_search_placeholder), color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ThemeNeonCyan) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = tr("Temizle", "Clear"), tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
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

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTierFilter == null && !showOnlyInStock,
                                onClick = {
                                    selectedTierFilter = null
                                    showOnlyInStock = false
                                },
                                label = { CurrencyText(stringResource(R.string.borsa_filter_all_products, Product.values().size)) },
                                leadingIcon = { Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeNeonCyan,
                                    selectedLabelColor = Color(0xFF002026),
                                    containerColor = Color(0xFF101726),
                                    labelColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedTierFilter == ProductTier.TIER_1,
                                onClick = {
                                    selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_1) null else ProductTier.TIER_1
                                    showOnlyInStock = false
                                },
                                label = { CurrencyText(stringResource(R.string.borsa_filter_tier_1)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeGold,
                                    selectedLabelColor = Color(0xFF1A1300),
                                    containerColor = Color(0xFF101726),
                                    labelColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedTierFilter == ProductTier.TIER_2,
                                onClick = {
                                    selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_2) null else ProductTier.TIER_2
                                    showOnlyInStock = false
                                },
                                label = { CurrencyText(stringResource(R.string.borsa_filter_tier_2)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeNeonCyan,
                                    selectedLabelColor = Color(0xFF002026),
                                    containerColor = Color(0xFF101726),
                                    labelColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedTierFilter == ProductTier.TIER_3,
                                onClick = {
                                    selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_3) null else ProductTier.TIER_3
                                    showOnlyInStock = false
                                },
                                label = { CurrencyText(stringResource(R.string.borsa_filter_tier_3)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE040FB),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF101726),
                                    labelColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedTierFilter == ProductTier.TIER_4,
                                onClick = {
                                    selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_4) null else ProductTier.TIER_4
                                    showOnlyInStock = false
                                },
                                label = { CurrencyText(stringResource(R.string.borsa_filter_tier_4)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E676),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF101726),
                                    labelColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = showOnlyInStock,
                                onClick = {
                                    showOnlyInStock = !showOnlyInStock
                                    if (showOnlyInStock) selectedTierFilter = null
                                },
                                label = { CurrencyText(stringResource(R.string.borsa_filter_in_stock)) },
                                leadingIcon = { Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemePositive,
                                    selectedLabelColor = Color(0xFF002B14),
                                    containerColor = Color(0xFF101726),
                                    labelColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }

                    // Sort Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("EMTİA PİYASASI LİSTESİ", "COMMODITY MARKET LIST"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SortPill(
                                label = tr("Fiyat ↓", "Price ↓"),
                                isSelected = sortBy == "PRICE_DESC",
                                onClick = { sortBy = "PRICE_DESC" }
                            )
                            SortPill(
                                label = tr("Fiyat ↑", "Price ↑"),
                                isSelected = sortBy == "PRICE_ASC",
                                onClick = { sortBy = "PRICE_ASC" }
                            )
                            SortPill(
                                label = tr("Stok", "Stock"),
                                isSelected = sortBy == "STOCK",
                                onClick = { sortBy = "STOCK" }
                            )
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.FilterAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            CurrencyText(tr("Aramanıza uygun emtia bulunamadı.", "No commodities found matching your search."), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // 4. COMMODITY CARDS (2-Column Grid Rows)
            itemsIndexed(
                items = chunkedFiltered,
                key = { _, rowProducts -> rowProducts.joinToString("-") { (prod, priceEntity) -> "${prod.id}-${priceEntity.originCountry}" } }
            ) { index, rowProducts ->
                com.example.ui.components.AnimatedListItem(index = index) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowProducts.forEach { (product, priceEntity) ->
                            val currentPrice = priceEntity.price
                            val ownedQuantity = inventoryState.filter { it.baseProductId == product.id }.sumOf { it.quantity }
                            val borsaStock = priceEntity.borsaStock
                            val demandStock = ((borsaStock * 1.15) + 120.0).toLong().coerceAtMost(com.example.data.MacroEconomyEngine.MAX_BORSA_STOCK)

                            CommodityMarketCard(
                                product = product,
                                price = currentPrice,
                                ownedQuantity = ownedQuantity,
                                borsaStock = borsaStock,
                                demandStock = demandStock,
                                originCountry = priceEntity.originCountry,
                                isUsd = priceEntity.effectiveIsUsd,
                                onBuyOne = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromBorsa(product.id, 1, priceEntity.originCountry))
                                },
                                onBuyTen = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromBorsa(product.id, 10, priceEntity.originCountry))
                                },
                                onSellOne = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.SellToBorsa(product.id, 1, priceEntity.originCountry))
                                },
                                onSellAll = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.SellToBorsa(product.id, ownedQuantity, priceEntity.originCountry))
                                },
                                onOpenTradeModal = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedProductForTrade = product
                                    selectedOriginCountryForTrade = priceEntity.originCountry
                                },
                                onOpenSupplyTree = if (product.recipe.isNotEmpty()) {
                                    {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedProductForSupplyTree = product
                                    }
                                } else null,
                                isExpertMode = isExpertModeState,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (rowProducts.size < itemsPerRow) {
                            repeat(itemsPerRow - rowProducts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

        // Borsa Trade Modal Dialog
        val tradeProduct = selectedProductForTrade
        if (tradeProduct != null) {
            val pStock = inventoryState.filter { it.baseProductId == tradeProduct.id }.sumOf { it.quantity }
            val pMoney = playerState?.money ?: 0L
            val pCap = playerState?.inventoryCapacity ?: 5000
            val currentTotalInv = inventoryState.sumOf { it.quantity }

            BorsaTradeModal(
                product = tradeProduct,
                originCountry = selectedOriginCountryForTrade ?: "Türkiye",
                pricesState = pricesState,
                priceHistoryState = priceHistoryState,
                crisisState = crisisState,
                ownedQuantity = pStock,
                playerMoney = pMoney,
                playerDollarBalance = playerState?.dollarBalance ?: 0L,
                inventoryCapacity = pCap,
                currentInventoryTotal = currentTotalInv,
                playerCurrentCity = playerState?.currentCity ?: "istanbul",
                viewModel = viewModel,
                onDismiss = { 
                    selectedProductForTrade = null 
                    selectedOriginCountryForTrade = null
                },
                onOpenSupplyTree = {
                    selectedProductForTrade = null
                    selectedOriginCountryForTrade = null
                    selectedProductForSupplyTree = tradeProduct
                },
                onBuy = { qty: Int -> viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromBorsa(tradeProduct.id, qty, selectedOriginCountryForTrade ?: "Türkiye")) },
                onSell = { qty: Int -> viewModel.handleIntent(com.example.viewmodel.GameIntent.SellToBorsa(tradeProduct.id, qty, selectedOriginCountryForTrade ?: "Türkiye")) }
            )
        }

        // İnteraktif Tedarik Bağı & Reçete Ağacı Görsel Diyaloğu
        val treeProduct = selectedProductForSupplyTree
        if (treeProduct != null) {
            com.example.ui.components.InteractiveSupplyChainTreeDialog(
                targetProduct = treeProduct,
                viewModel = viewModel,
                onDismiss = { selectedProductForSupplyTree = null }
            )
        }
    }
}
}

@Composable
fun BotToggleChip(
    title: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (isActive) activeColor.copy(alpha = 0.15f) else Color(0xFF161E2E),
        border = BorderStroke(1.dp, if (isActive) activeColor else ThemeBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            CurrencyText(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isActive) activeColor else Color(0xFF2E384D)
            ) {
                CurrencyText(
                    text = if (isActive) tr("AKTİF", "ACTIVE") else tr("PASİF", "PASSIVE"),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.Black else Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun SortPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(2.dp),
        color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF101726),
        border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else ThemeBorder)
    ) {
        CurrencyText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ThemeNeonCyan else Color.Gray,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CommodityMarketCard(
    product: Product,
    price: Long,
    ownedQuantity: Int,
    borsaStock: Long = com.example.data.MacroEconomyEngine.DEFAULT_BORSA_STOCK,
    demandStock: Long = 60000L,
    originCountry: String = "Türkiye",
    isUsd: Boolean = false,
    onBuyOne: () -> Unit,
    onBuyTen: () -> Unit,
    onSellOne: () -> Unit,
    onSellAll: () -> Unit,
    onOpenTradeModal: () -> Unit,
    onOpenSupplyTree: (() -> Unit)? = null,
    isExpertMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val effectiveBasePrice = product.basePrice.coerceAtLeast(10L)
    val priceDiff = price - effectiveBasePrice
    val isUp = priceDiff > 0
    val isDown = priceDiff < 0
    val trendColor = remember(isUp, isDown) { if (isUp) ThemePositive else if (isDown) ThemeNegative else Color.Gray }
    val trendBg = remember(isUp, isDown) { if (isUp) ThemePositiveBg else if (isDown) ThemeNegativeBg else Color.Gray.copy(alpha = 0.12f) }
    val percentText = remember(price, effectiveBasePrice, isUp, isDown) {
        val pct = ((priceDiff.toDouble() / effectiveBasePrice.toDouble()) * 100.0).coerceIn(-90.0, 500.0)
        if (isUp) "+${String.format(java.util.Locale.US, "%.1f", pct)}%"
        else if (isDown) "${String.format(java.util.Locale.US, "%.1f", pct)}%"
        else "0.0%"
    }

    val brandColor = remember(product.colorTint) { Color(product.colorTint) }
    val countryFlagEmoji = remember(originCountry) { getCountryFlagEmoji(originCountry) }
    
    val badgePulse = remember { Animatable(1f) }
    LaunchedEffect(price) {
        badgePulse.animateTo(1.22f, tween(100, easing = FastOutSlowInEasing))
        badgePulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    GlassCard(
        modifier = modifier
            .luxeShimmerBorder(
                enabled = product.tier == ProductTier.TIER_4 || product.tier == ProductTier.TIER_3,
                shape = RoundedCornerShape(8.dp),
                borderWidth = 1.dp
            )
            .clickable { onOpenTradeModal() },
        shape = RoundedCornerShape(8.dp),
        color = ThemeSurface,
        borderWidth = 1.dp,
        borderColor = if (ownedQuantity > 0) brandColor else ThemeBorder,
        pulsing = ownedQuantity > 0
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Product Icon + Country Flag + Tree Pill + Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Icon on the left
                com.example.ui.components.ProductIconBadge(
                    product = product,
                    size = 32.dp,
                    showSectorBadge = false
                )

                // Badges on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Country Origin Flag Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(0.5.dp, if (isUsd) ThemeGold.copy(alpha = 0.5f) else Color(0xFF475569))
                    ) {
                        CurrencyText(
                            text = "$countryFlagEmoji $originCountry",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUsd) ThemeGold else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Reçete / Tedarik Bağı Butonu
                    if (product.recipe.isNotEmpty() && onOpenSupplyTree != null) {
                        Surface(
                            modifier = Modifier.clickable { onOpenSupplyTree() },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.5.dp, Color(0xFF38BDF8))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountTree,
                                    contentDescription = "Tedarik Bağı",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                CurrencyText(
                                    text = tr("Ağaç", "Tree"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    // Tier Pill
                    val tierText = when (product.tier) {
                        ProductTier.TIER_1 -> "T1"
                        ProductTier.TIER_2 -> "T2"
                        ProductTier.TIER_3 -> "T3"
                        ProductTier.TIER_4 -> "T4"
                    }
                    val tierColor = when (product.tier) {
                        ProductTier.TIER_1 -> ThemeGold
                        ProductTier.TIER_2 -> ThemeNeonCyan
                        ProductTier.TIER_3 -> Color(0xFFE040FB)
                        ProductTier.TIER_4 -> Color(0xFF00E676)
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tierColor.copy(alpha = 0.15f)
                    ) {
                        CurrencyText(
                            text = tierText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated, high-visibility line for Product Name + Crisis Mode Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = product.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        CurrencyText(
                            text = if (isEng) "🚨 CRISIS" else "🚨 KRİZ",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price & Trend Display (with Mini Sparkline Chart)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    CurrencyText(
                        text = formatCredit(price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val deltaPerTon = product.basePrice.toDouble() / 999999.0
                    val deltaStr = String.format(java.util.Locale.US, "%.5f", deltaPerTon)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            text = tr("Kuru", "Rate"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText(
                            text = "±₳$deltaStr/T",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = ThemeNeonCyan,
                            fontFamily = RobotoMonoFontFamily,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Mini Sparkline Graph
                MiniSparklineGraph(
                    price = price,
                    basePrice = effectiveBasePrice,
                    productId = product.id
                )

                Spacer(modifier = Modifier.width(2.dp))

                // Trend Badge with Pulse Bounce Effect
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = trendBg,
                    modifier = Modifier.scale(badgePulse.value)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        CurrencyText(
                            text = percentText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = trendColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Inventory & Borsa Warehouse Stock Badges
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = if (ownedQuantity > 0) ThemePositiveBg.copy(alpha = 0.5f) else Color(0xFF1A2130),
                border = BorderStroke(0.5.dp, if (ownedQuantity > 0) ThemePositive.copy(alpha = 0.4f) else ThemeBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = if (ownedQuantity > 0) ThemePositive else Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText(
                            text = tr("Kendi Deponuz", "Warehouse"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    CurrencyText(
                        text = tr("$ownedQuantity T", "$ownedQuantity T"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (ownedQuantity > 0) ThemePositive else Color.Gray,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Borsa Ana Deposu Bilgi Satırı
            val borsaWarehouseTitle = if (isUsd) "🏛️ Borsa Depo" else "🏛️ Borsa Depo"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) Color(0xFF3B1212) else Color(0xFF0E1624),
                border = BorderStroke(0.5.dp, if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) Color(0xFFFF5252) else ThemeBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = borsaWarehouseTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) Color(0xFFFF8A80) else Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) (if (isEng) "${formatStockTons(borsaStock, isEng)} (CRISIS)" else "${formatStockTons(borsaStock, isEng)} (KRİZ)") else formatStockTons(borsaStock, isEng),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) Color(0xFFFF5252) else ThemeNeonCyan,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Canlı Alış / Satış Derinlik Çubuğu (Order Book Depth Bar)
            com.example.ui.components.OrderBookDepthBar(
                demandQuantity = demandStock,
                supplyQuantity = borsaStock,
                showLabels = true,
                compact = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dual Action Section: BUY & SELL
            if (!isExpertMode) {
                // BASİT GÖRÜNÜM: Clean 2-Button Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppButton(
                        onClick = onBuyOne,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemePositive,
                            contentColor = Color(0xFF002810)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        CurrencyText(
                            text = tr("+1 Al", "+1 Buy"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (ownedQuantity > 0) {
                        AppButton(
                            onClick = onSellAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            CurrencyText(
                                text = tr("Tümünü Sat", "Sell All"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        AppButton(
                            onClick = onOpenTradeModal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            CurrencyText(
                                text = tr("Detay", "Details"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // UZMAN GÖRÜNÜM: Granular 4-Button Grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Row 1: ALIM (BUY) BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppButton(
                            onClick = onBuyOne,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemePositive,
                                contentColor = Color(0xFF002810)
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            CurrencyText(
                                text = "+1 ${stringResource(R.string.borsa_buy)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        AppButton(
                            onClick = onBuyTen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeNeonCyan,
                                contentColor = Color(0xFF002026)
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            CurrencyText(
                                text = "+10 ${stringResource(R.string.borsa_buy)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Row 2: SATIŞ (SELL) BUTTONS OR TRADE MODAL TRIGGER
                    if (ownedQuantity > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppButton(
                                onClick = onSellOne,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeGold,
                                    contentColor = Color(0xFF1A1300)
                                ),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                CurrencyText(
                                    text = "1 ${stringResource(R.string.borsa_sell)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            AppButton(
                                onClick = onSellAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                CurrencyText(
                                    text = "${stringResource(R.string.borsa_sell)} ($ownedQuantity)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenTradeModal,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNeonCyan),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = tr("Alım / Satım Detay", "Buy / Sell Details"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
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

@Composable
fun BorsaTradeModal(
    product: Product,
    originCountry: String,
    pricesState: List<MarketPriceEntity>,
    priceHistoryState: Map<String, List<Long>>,
    crisisState: com.example.data.ConsortiumCrisisState,
    ownedQuantity: Int,
    playerMoney: Long,
    playerDollarBalance: Long,
    inventoryCapacity: Int,
    currentInventoryTotal: Int,
    playerCurrentCity: String = "istanbul",
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onOpenSupplyTree: (() -> Unit)? = null,
    onBuy: (quantity: Int) -> Unit,
    onSell: (quantity: Int) -> Unit
) {
    val isEng = isEnglishLanguage()
    val priceEntity = pricesState.find { it.itemId == product.id && it.originCountry == originCountry }
    val price = priceEntity?.price ?: product.basePrice
    val isUsd = false
    val countryFlag = getCountryFlagEmoji(originCountry)

    val history = priceHistoryState["${product.id}_$originCountry"] ?: priceHistoryState[product.id] ?: emptyList()
    val borsaStock = priceEntity?.borsaStock ?: com.example.data.MacroEconomyEngine.DEFAULT_BORSA_STOCK
    val demandStock = ((borsaStock * 1.15) + 200.0).toLong().coerceAtMost(com.example.data.MacroEconomyEngine.MAX_BORSA_STOCK)
    val criticalMissingMaterials = crisisState.criticalMissingMaterials
    val isCrisisActive = crisisState.isCrisisActive
    val stockMarketDropMultiplier = crisisState.stockMarketDropMultiplier
    val rawMaterialInflationMultiplier = crisisState.rawMaterialInflationMultiplier

    var quantity by remember { mutableIntStateOf(10) }
    var tradeMode by remember { mutableStateOf(TradeMode.BUY) } // "BUY" or "SELL"
    var chartType by remember { mutableStateOf("CANDLE") } // "CANDLE" or "LINE"
    
    val player by viewModel.player.collectAsStateWithLifecycle()
    val playerLevel = player?.level ?: 1

    val brandColor = Color(product.colorTint)
    val usdRate = ForexRateManager.currentUsdRate.coerceAtLeast(1.0)
    val priceInTry = if (isUsd) (price * usdRate).toLong() else price
    val maxAffordable = if (isUsd) {
        (playerDollarBalance / price.coerceAtLeast(1L)).toInt().coerceAtLeast(0)
    } else {
        (playerMoney / price.coerceAtLeast(1L)).toInt().coerceAtLeast(0)
    }
    val maxSellable = ownedQuantity

    val effectiveBasePrice = product.basePrice.coerceAtLeast(10L)
    val priceDiff = price - effectiveBasePrice
    val isUp = priceDiff > 0
    val isDown = priceDiff < 0
    val pct = ((priceDiff.toDouble() / effectiveBasePrice.toDouble()) * 100.0).coerceIn(-90.0, 500.0)
    val percentText = if (isUp) "+${String.format(java.util.Locale.US, "%.1f", pct)}%" else if (isDown) "${String.format(java.util.Locale.US, "%.1f", pct)}%" else "0.0%"

    val high24h = if (history.isNotEmpty()) maxOf(price, history.maxOrNull() ?: price) else (price * 1.05).toLong()
    val low24h = if (history.isNotEmpty()) minOf(price, history.minOrNull() ?: price) else (price * 0.95).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, brandColor),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(product.icon, contentDescription = null, tint = brandColor, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurrencyText(
                                text = product.displayName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RajdhaniFontFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(0.5.dp, if (isUsd) ThemeGold else ThemeBorder)
                            ) {
                                CurrencyText(
                                    text = "$countryFlag $originCountry",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = formatCredit(price),
                                style = MaterialTheme.typography.labelLarge,
                                color = ThemeGold,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isUp) ThemePositiveBg else ThemeNegativeBg
                            ) {
                                CurrencyText(
                                    text = percentText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUp) ThemePositive else ThemeNegative,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = tr("Kapat", "Close"), tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Kriz & Piyasa Durumu Rozet / Banner
                val crisis = crisisState
                if (crisis.isCrisisActive) {
                    val isRaw = product.id in com.example.data.ConsortiumCrisisEngine.RAW_MATERIAL_IDS || product.id in crisis.criticalMissingMaterials
                    if (isRaw) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF3B1219),
                            border = BorderStroke(1.dp, Color(0xFFFF4D4D))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF4D4D), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    CurrencyText(tr("🚨 KRİZ DARBOĞAZI & YÜKSEK ENFLASYON", "🚨 CRISIS BOTTLENECK & HIGH INFLATION"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D4D))
                                    CurrencyText(
                                        tr("Sanayi hammadde krizinden dolayı borsada kıtlık ve +%${((crisis.rawMaterialInflationMultiplier - 1f) * 100).toInt()} fiyat zam baskısı yaşanmaktadır.", "Due to raw material crisis, scarcity and +${((crisis.rawMaterialInflationMultiplier - 1f) * 100).toInt()}% inflation pressure on exchange."),
                                        style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFFFC1C1)
                                    )
                                }
                            }
                        }
                    } else if (product.tier == ProductTier.TIER_3 || product.tier == ProductTier.TIER_4) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2A1C08),
                            border = BorderStroke(1.dp, ThemeGold)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.TrendingDown, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    CurrencyText(tr("📉 KRİZ RESESYONU: NİHAİ MALI BORSASINDA DÜŞÜŞ", "📉 CRISIS RECESSION: DOWNTURN IN END-PRODUCT MARKETS"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ThemeGold)
                                    CurrencyText(
                                        tr("Ekonomik durgunluk nedeniyle üst düzey ürünlerde talep daralması var (-%${((1f - crisis.stockMarketDropMultiplier) * 100).toInt()}).", "Demand contraction in high-tier products due to economic recession (-${((1f - crisis.stockMarketDropMultiplier) * 100).toInt()}%)."),
                                        style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFFFE8B3)
                                    )
                                }
                            }
                        }
                    }
                } else if (product.id == "crude_oil" || product.id == "refined_fuel") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F2232),
                        border = BorderStroke(1.dp, ThemeNeonCyan)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalGasStation, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                CurrencyText(tr("⛽ DİNAMİK LOJİSTİK AKARYAKIT ENDEKSİ", "⛽ DYNAMIC LOGISTICS FUEL INDEX"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ThemeNeonCyan)
                                CurrencyText(
                                    tr("Bu ürünün borsa fiyatı şehirlerarası nakliye, fabrika lojistiği ve seyahat maliyetlerini doğrudan belirler.", "The exchange price of this product directly determines intercity transport, factory logistics, and travel costs."),
                                    style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFFB3ECFF)
                                )
                            }
                        }
                    }
                }

                // 🚨 KRİZ ORTAMI VE DEVLET TEŞVİK PRİMİ BANNERI (Borsa Stoğu <= 999 Ton ise)
                if (borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF3E1212),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                CurrencyText(
                                    text = tr("🚨 KRİZ ORTAMI & MİLLİ ÜRETİM SEFERBERLİĞİ", "🚨 CRISIS & NATIONAL PRODUCTION MOBILIZATION"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            CurrencyText(
                                text = tr(
                                    "Borsa rezervleri kritik eşiğin altına indi ($borsaStock Ton <= 999)! Fiyat 2 katına fırladı. Devlet Hazine Teşviki ile bu üründe: ⚡ %50 Üretim Hızı Bonusu ve 🏛️ %25 Nakit Teşvik Primi aktiftir!",
                                    "Reserves dropped below critical threshold ($borsaStock Tons <= 999)! Price doubled. State Treasury Incentive active: ⚡ 50% Production Speed Bonus & 🏛️ 25% Cash Subsidy Bonus!"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFFFFCDD2)
                            )
                        }
                    }
                }

                // 2. 24s Borsa Metrikleri (4 Kutulu Grid)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF162032),
                    border = BorderStroke(1.dp, ThemeBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(tr("Taban Fiyat", "Base Price"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                CurrencyText(formatCredit(effectiveBasePrice), style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(tr("Piyasa Stok", "Market Supply"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                CurrencyText(com.example.ui.components.formatStockTonsExact(borsaStock, isEng), style = MaterialTheme.typography.bodyMedium, color = ThemeNeonCyan, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                            }
                        }
                        HorizontalDivider(color = ThemeBorder.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(tr("24s Zirve (High)", "24h High"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                CurrencyText(formatCredit(high24h), style = MaterialTheme.typography.bodyMedium, color = ThemePositive, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(tr("24s Dip (Low)", "24h Low"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                CurrencyText(formatCredit(low24h), style = MaterialTheme.typography.bodyMedium, color = ThemeNegative, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                            }
                        }
                    }
                }

                // Canlı Alış / Satış Derinlik Çubuğu (Sipariş Defteri)
                if (playerLevel > 3) {
                    com.example.ui.components.OrderBookDepthBar(
                        demandQuantity = demandStock,
                        supplyQuantity = borsaStock,
                        showLabels = true,
                        compact = false
                    )
                }

                // Tedarik Bağı Çizimi (Görsel Ağaç) Butonu
                if (product.recipe.isNotEmpty() && onOpenSupplyTree != null) {
                    AppButton(
                        onClick = onOpenSupplyTree,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F2B48),
                            contentColor = Color(0xFF38BDF8)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(
                            text = tr("🌿 Tedarik Bağı ve Reçete Ağacını İncele", "🌿 View Interactive Supply Chain Tree"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                if (playerLevel > 3) {
                    // 3. Canlı Grafik Türü Seçici & Grafik Canvas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(text = tr("CANLI BORSA GRAFİĞİ", "LIVE COMMODITY CHART"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = RobotoMonoFontFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { chartType = "CANDLE" },
                                shape = RoundedCornerShape(4.dp),
                                color = if (chartType == "CANDLE") ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF162032),
                                border = BorderStroke(1.dp, if (chartType == "CANDLE") ThemeNeonCyan else ThemeBorder)
                            ) {
                                CurrencyText(
                                    text = tr("🕯️ MUM", "🕯️ CANDLE"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chartType == "CANDLE") ThemeNeonCyan else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                onClick = { chartType = "LINE" },
                                shape = RoundedCornerShape(4.dp),
                                color = if (chartType == "LINE") ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF162032),
                                border = BorderStroke(1.dp, if (chartType == "LINE") ThemeNeonCyan else ThemeBorder)
                            ) {
                                CurrencyText(
                                    text = tr("📈 ÇİZGİ", "📈 LINE"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chartType == "LINE") ThemeNeonCyan else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (chartType == "CANDLE") {
                        StockCandlestickChart(
                            history = history,
                            currentPrice = price,
                            basePrice = effectiveBasePrice,
                            productId = product.id,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        StockLineChart(
                            history = history,
                            currentPrice = price,
                            basePrice = effectiveBasePrice,
                            productId = product.id,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(6.dp))
                        )
                    }
                }

                // 4. AL / SAT İşlem Modu Tabı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF182234))
                        .padding(4.dp)
                ) {
                    Surface(
                        onClick = {
                            tradeMode = TradeMode.BUY
                            quantity = 10.coerceAtMost(maxAffordable.coerceAtLeast(1))
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (tradeMode == TradeMode.BUY) ThemePositive else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = tr("BORSADAN AL", "BUY FROM EXCHANGE"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (tradeMode == TradeMode.BUY) Color(0xFF002010) else Color.Gray,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            tradeMode = TradeMode.SELL
                            quantity = maxSellable.coerceAtLeast(1)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (tradeMode == TradeMode.SELL) ThemeGold else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = tr("BORSADA SAT", "SELL ON EXCHANGE"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (tradeMode == TradeMode.SELL) Color(0xFF1A1300) else Color.Gray,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }

                // 🚚 Borsa Deposu Ticaret & Lojistik Güzergah Paneli
                val playerCityId = playerCurrentCity
                val playerCityName = com.example.data.cities.find { it.id == playerCityId }?.name ?: playerCityId
                val borsaHubCityId = "new_york"
                val borsaHubTitle = "🗽 New York Borsa Merkez Deposu"
                val playerWarehouseTitle = "🏙️ $playerCityName Merkez Deposu"

                val originRouteCityId = if (tradeMode == TradeMode.BUY) borsaHubCityId else playerCityId
                val destRouteCityId = if (tradeMode == TradeMode.BUY) playerCityId else borsaHubCityId
                val isSameCity = originRouteCityId == destRouteCityId

                val distanceKm = viewModel.getEstimatedDistanceKm(originRouteCityId, destRouteCityId)
                val durationSeconds = viewModel.getEstimatedLogisticsDurationSeconds(originRouteCityId, destRouteCityId)
                val logisticsCostTry = if (tradeMode == TradeMode.BUY && !isSameCity) {
                    viewModel.calculateLogisticsCost(originRouteCityId, destRouteCityId, quantity)
                } else 0L

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF101928),
                    border = BorderStroke(1.dp, if (tradeMode == TradeMode.BUY) ThemePositive.copy(alpha = 0.6f) else ThemeGold.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (tradeMode == TradeMode.BUY) Icons.Rounded.LocalShipping else Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = if (tradeMode == TradeMode.BUY) ThemePositive else ThemeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(
                                    text = if (tradeMode == TradeMode.BUY) tr("SEVKİYAT GÜZERGAHI (ALIM)", "DELIVERY ROUTE (BUY)") else tr("SEVKİYAT GÜZERGAHI (SATIŞ)", "DELIVERY ROUTE (SELL)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tradeMode == TradeMode.BUY) ThemePositive else ThemeGold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSameCity) ThemePositiveBg else Color(0xFF1E293B)
                            ) {
                                CurrencyText(
                                    text = if (isSameCity) tr("⚡ Anında Teslim", "⚡ Instant Delivery") else tr("⏱️ ~${durationSeconds} sn", "⏱️ ~${durationSeconds} sec"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isSameCity) ThemePositive else ThemeNeonCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }

                        CurrencyText(
                            text = if (tradeMode == TradeMode.BUY) "$borsaHubTitle ➔ $playerWarehouseTitle" else "$playerWarehouseTitle ➔ $borsaHubTitle",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = if (isSameCity) tr("Mesafe: 0 km (Aynı Merkez)", "Distance: 0 km (Same Hub)") else tr("Mesafe: ~$distanceKm km", "Distance: ~$distanceKm km"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                fontFamily = RobotoMonoFontFamily
                            )
                            if (tradeMode == TradeMode.BUY && !isSameCity) {
                                CurrencyText(
                                    text = tr("Taşıma Maliyeti: +${formatCredit(logisticsCostTry)}", "Transport: +${formatCredit(logisticsCostTry)}"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = ThemeGold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Bakiye ve Stok Bilgisi
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0A0F1A),
                    border = BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            CurrencyText(tr("Bakiye:", "Balance:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            CurrencyText(
                                formatCredit(playerMoney), 
                                style = MaterialTheme.typography.labelMedium, 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            CurrencyText(tr("Mevcut Stokunuz:", "Your Current Stock:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            CurrencyText(tr("$ownedQuantity Ton", "$ownedQuantity Tons"), style = MaterialTheme.typography.labelMedium, color = ThemeNeonCyan, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }

                // Miktar Ayarlayıcı
                val maxAvailable = if (tradeMode == TradeMode.BUY) maxAffordable.coerceAtLeast(1) else maxSellable.coerceAtLeast(1)

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(text = tr("İŞLEM MİKTARI (TON)", "TRANSACTION AMOUNT (TONS)"), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = RobotoMonoFontFamily)
                        CurrencyText(text = tr("Üst Limit: $maxAvailable Ton", "Max Limit: $maxAvailable Tons"), style = MaterialTheme.typography.labelSmall, color = ThemeNeonCyan, fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 10) quantity -= 10 else if (quantity > 1) quantity -= 1 },
                            modifier = Modifier.background(Color(0xFF1A2436), CircleShape)
                        ) {
                            CurrencyText("-10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF182234),
                            border = BorderStroke(1.dp, ThemeNeonCyan),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            CurrencyText(
                                text = tr("$quantity Ton", "$quantity Tons"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { quantity = (quantity + 10).coerceAtMost(maxAvailable) },
                            modifier = Modifier.background(Color(0xFF1A2436), CircleShape)
                        ) {
                            CurrencyText("+10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Hızlı Miktar Seçim Çipleri (%25, %50, %75, %100 Maks)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            tr("%25", "25%") to 0.25f,
                            tr("%50", "50%") to 0.50f,
                            tr("%75", "75%") to 0.75f,
                            tr("Maks (%100)", "Max (100%)") to 1.00f
                        ).forEach { (label, ratio) ->
                            val targetQty = (maxAvailable * ratio).toInt().coerceAtLeast(1)
                            val isSelected = quantity == targetQty

                            Surface(
                                onClick = { quantity = targetQty },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1A2436),
                                border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else ThemeBorder),
                                modifier = Modifier.weight(if (ratio == 1.0f) 1.3f else 1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    CurrencyText(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // Toplam Tutar ve İşlem Butonu (Satış modunda envanterdeki ürünlerin kalite çarpanı uygulanır)
                val ownedItems = viewModel.inventory.value.filter { it.baseProductId == product.id && it.quantity > 0 }
                val totalCost = if (tradeMode == TradeMode.SELL && ownedItems.isNotEmpty()) {
                    var rem = quantity
                    var income = 0L
                    for (item in ownedItems) {
                        if (rem <= 0) break
                        val take = minOf(item.quantity, rem)
                        val unitP = (price * item.quality.priceMultiplier).toLong()
                        income += unitP * take
                        rem -= take
                    }
                    if (rem > 0) income += (price * rem)
                    income
                } else {
                    try { Math.multiplyExact(price.toLong(), quantity.toLong()) } catch(e: Exception) { Long.MAX_VALUE }
                }

                val maxOwnedQuality = ownedItems.maxByOrNull { it.quality.stars }?.quality
                val isCrisisActive = borsaStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
                val subsidyBonus = if (tradeMode == TradeMode.SELL && isCrisisActive) (totalCost * 0.25).toLong() else 0L
                val grandTotal = if (tradeMode == TradeMode.BUY) totalCost + logisticsCostTry else (totalCost + subsidyBonus)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (tradeMode == TradeMode.BUY) ThemePositiveBg else ThemeGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (tradeMode == TradeMode.BUY) ThemePositive else ThemeGold)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = if (tradeMode == TradeMode.BUY) tr("TOPLAM MALİYET:", "TOTAL COST:") else tr("NET TOPLAM GELİR:", "NET TOTAL REVENUE:"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (tradeMode == TradeMode.BUY) ThemePositive else ThemeGold
                            )
                            CurrencyText(
                                text = formatCredit(grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        // Canlı Borsa Havuzu & Stok Etki Göstergesi (Reel Supabase Değişimi)
                        HorizontalDivider(color = ThemeBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
                        val resultingStock = if (tradeMode == TradeMode.BUY) (borsaStock - quantity).coerceAtLeast(0L) else (borsaStock + quantity)
                        val isCrisisWarning = resultingStock <= com.example.data.MacroEconomyEngine.CRISIS_STOCK_THRESHOLD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = if (tradeMode == TradeMode.BUY) tr("Borsa Rezerv Etkisi:", "Borsa Reserve Impact:") else tr("Borsa Havuzuna Giriş:", "Borsa Inflow Impact:"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                            CurrencyText(
                                text = "${formatStockTons(borsaStock, isEng)} ➔ ${formatStockTons(resultingStock, isEng)}${if (isCrisisWarning) (if (isEng) " (CRISIS!)" else " (KRİZ!)") else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCrisisWarning) Color(0xFFFF5252) else ThemeNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 10.5.sp
                            )
                        }

                        // Fiyat Değişim Etkisi (Otomatik Stok-Fiyat Formülü)
                        val resultingPrice = com.example.data.MacroEconomyEngine.calculatePriceFromStock(resultingStock, product.basePrice)
                        val priceImpact = resultingPrice - price
                        val priceImpactPct = if (price > 0) ((priceImpact.toDouble() / price.toDouble()) * 100.0) else 0.0
                        val impactPrefix = if (priceImpact >= 0) "+" else ""
                        val impactColor = if (priceImpact > 0) Color(0xFFFF8A80) else if (priceImpact < 0) Color(0xFF69F0AE) else Color.Gray
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("Borsa Fiyat Etkisi:", "Borsa Price Impact:"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                            CurrencyText(
                                text = "$impactPrefix₳${com.example.ui.components.formatCredit(priceImpact)} (${impactPrefix}${String.format(java.util.Locale.US, "%.2f", priceImpactPct)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = impactColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 10.sp
                            )
                        }

                        if (tradeMode == TradeMode.SELL && maxOwnedQuality != null && maxOwnedQuality.stars > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = tr("💎 Kalite Fiyat Primi:", "💎 Quality Price Bonus:"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = maxOwnedQuality.badgeColor,
                                    fontSize = 11.sp
                                )
                                CurrencyText(
                                    text = "${maxOwnedQuality.starsText} ${maxOwnedQuality.label} (x${maxOwnedQuality.priceMultiplier})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = maxOwnedQuality.badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (tradeMode == TradeMode.SELL && subsidyBonus > 0L) {
                            HorizontalDivider(color = ThemeGold.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = tr("🏛️ Devlet Teşvik Primi (%25):", "🏛️ State Subsidy Bonus (25%):"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeGold,
                                    fontSize = 11.sp
                                )
                                CurrencyText(
                                    text = "+${formatCredit(subsidyBonus)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (tradeMode == TradeMode.BUY && logisticsCostTry > 0L) {
                            HorizontalDivider(color = ThemeBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = tr("+ Lojistik & Taşıma:", "+ Logistics & Transport:"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeGold,
                                    fontSize = 11.sp
                                )
                                CurrencyText(
                                    text = "+${formatCredit(logisticsCostTry)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (tradeMode == TradeMode.BUY) {
                            onBuy(quantity)
                        } else {
                            onSell(quantity)
                        }
                        onDismiss()
                    },
                    enabled = if (tradeMode == TradeMode.BUY) (quantity > 0 && playerMoney >= grandTotal) else (quantity > 0 && ownedQuantity >= quantity),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tradeMode == TradeMode.BUY) ThemePositive else ThemeGold,
                        disabledContainerColor = Color(0xFF2E384D)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    CurrencyText(
                        text = if (tradeMode == TradeMode.BUY) tr("BORSADAN SATIN AL", "BUY FROM EXCHANGE") else tr("BORSADA SAT", "SELL ON EXCHANGE"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (tradeMode == TradeMode.BUY) Color(0xFF002010) else Color(0xFF1A1300),
                        fontFamily = RobotoMonoFontFamily
                    )
                }

                // 🤖 AI Arbitraj Başlat Butonu
                OutlinedButton(
                    onClick = {
                        viewModel.createSmartArbitrageBot(product.id, quantity = quantity)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ThemeNeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = tr("🤖 AI ARBİTRAJ BOTUNU BU ÜRÜNDE BAŞLAT", "🤖 START AI ARBITRAGE BOT ON COMMODITY"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeNeonCyan,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MarketTickerTape(uiState: com.example.viewmodel.GameUiState) {
    val marketPrices = uiState.marketState.prices
    val marketTrends = uiState.marketState.marketTrends
    val isEng = isEnglishLanguage()

    val demirStr = tr("DEMİR", "IRON", isEng)
    val bakirStr = tr("BAKIR", "COPPER", isEng)
    val silisyumStr = tr("SİLİSYUM", "SILICON", isEng)
    val celikStr = tr("ÇELİK", "STEEL", isEng)
    val cipStr = tr("MİKROÇİP", "CHIP", isEng)

    val tickerItems: List<Triple<String, String, String>> = remember(marketPrices, marketTrends, demirStr, bakirStr, silisyumStr, celikStr, cipStr, isEng) {
        if (marketPrices.isEmpty()) {
            listOf(
                Triple(demirStr, com.example.ui.components.formatMoney(200L), "▲ +2.5%"),
                Triple(bakirStr, com.example.ui.components.formatMoney(250L), "▲ +1.1%"),
                Triple(silisyumStr, com.example.ui.components.formatMoney(300L), "▼ -1.5%"),
                Triple(celikStr, com.example.ui.components.formatMoney(600L), "▲ +3.2%"),
                Triple(cipStr, com.example.ui.components.formatMoney(2400L), "▲ +4.8%")
            )
        } else {
            // En düşük fiyatlı üründen en yüksek fiyatlı ürüne doğru sıralama (Artan Fiyat)
            val sortedPrices = marketPrices.sortedBy { it.price }
            sortedPrices.map { price ->
                val product = Product.values().find { it.id == price.itemId }
                val name = if (product != null) {
                    product.getDisplayName(isEng).uppercase()
                } else {
                    price.itemId.replace("_", " ").trAuto(isEng).uppercase()
                }
                val trend = marketTrends[price.itemId]
                val (arrow, sign, percentStr) = if (trend != null) {
                    val isPos = trend >= 0f
                    val a = if (isPos) "▲" else "▼"
                    val s = if (isPos) "+" else ""
                    val pct = String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(trend * 100f))
                    Triple(a, s, pct)
                } else {
                    val isPos = (price.itemId.hashCode() + price.price.toInt()) % 2 == 0
                    val a = if (isPos) "▲" else "▼"
                    val s = if (isPos) "+" else "-"
                    val pct = String.format(java.util.Locale.US, "%.1f", ((price.price % 15) + 1) * 0.3f)
                    Triple(a, s, pct)
                }
                Triple(name, com.example.ui.components.formatMoney(price.price), "$arrow $sign$percentStr%")
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        color = Color(0xFF080C14),
        border = BorderStroke(1.dp, ThemeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .basicMarquee(iterations = Int.MAX_VALUE),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tickerItems.forEach { (name, price, trend) ->
                val isUp = trend.contains("▲")
                val trendColor = if (isUp) ThemePositive else ThemeNegative

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyText(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = price,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeNeonCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText(
                        text = trend,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = trendColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    CurrencyText(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeBorder
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
fun MiniSparklineGraph(
    price: Long,
    basePrice: Long,
    productId: String,
    modifier: Modifier = Modifier
) {
    val historyPoints = remember(price, basePrice, productId) {
        val seed = productId.hashCode()
        val list = mutableListOf<Float>()
        var curr = basePrice.toFloat()
        val step = (price - basePrice).toFloat() / 7f
        for (i in 0..6) {
            val noise = (Math.sin((seed + i * 33).toDouble()).toFloat()) * (basePrice * 0.04f)
            list.add((curr + noise).coerceAtLeast(1f))
            curr += step
        }
        list.add(price.toFloat())
        list
    }

    val isUp = price >= (historyPoints.firstOrNull() ?: price.toFloat())
    val lineColor = if (isUp) ThemePositive else ThemeNegative
    val bgGradient = Brush.verticalGradient(
        colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
    )

    Canvas(modifier = modifier.height(26.dp).width(64.dp)) {
        if (historyPoints.size < 2) return@Canvas
        val w = size.width
        val h = size.height

        val minP = historyPoints.minOrNull() ?: 1f
        val maxP = historyPoints.maxOrNull() ?: (minP + 1f)
        val rangeP = (maxP - minP).coerceAtLeast(1f)

        val points = historyPoints.mapIndexed { idx, p ->
            val x = (idx.toFloat() / (historyPoints.size - 1)) * w
            val y = h - (((p - minP) / rangeP) * (h - 8.dp.toPx())) - 4.dp.toPx()
            Offset(x, y)
        }

        // 1. Smooth Bezier Fill path under curve
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            if (i == 0) {
                path.moveTo(p.x, p.y)
                fillPath.moveTo(p.x, h)
                fillPath.lineTo(p.x, p.y)
            } else {
                val prev = points[i - 1]
                val cx1 = (prev.x + p.x) / 2f
                val cy1 = prev.y
                val cx2 = (prev.x + p.x) / 2f
                val cy2 = p.y
                path.cubicTo(cx1, cy1, cx2, cy2, p.x, p.y)
                fillPath.cubicTo(cx1, cy1, cx2, cy2, p.x, p.y)
            }
        }
        fillPath.lineTo(points.last().x, h)
        fillPath.close()

        drawPath(fillPath, brush = bgGradient)

        // 2. Stroke line with subtle glow
        drawPath(path, color = lineColor.copy(alpha = 0.35f), style = Stroke(width = 4.dp.toPx()))
        drawPath(path, color = lineColor, style = Stroke(width = 1.8.dp.toPx()))

        // 3. Glowing end point dot
        val lastPoint = points.last()
        drawCircle(color = lineColor.copy(alpha = 0.5f), radius = 5.dp.toPx(), center = lastPoint)
        drawCircle(color = lineColor, radius = 3.dp.toPx(), center = lastPoint)
        drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = lastPoint)
    }
}

@Composable
fun StockCandlestickChart(
    history: List<Long>,
    currentPrice: Long,
    basePrice: Long,
    productId: String,
    modifier: Modifier = Modifier
) {
    val points = if (history.size >= 2) history else {
        listOf((currentPrice * 0.95).toLong(), (currentPrice * 0.98).toLong(), currentPrice)
    }

    val candles = remember(points, currentPrice, basePrice, productId) {
        val list = mutableListOf<FloatArray>()
        for (i in 0 until points.size - 1) {
            val open = points[i].toFloat()
            val close = points[i + 1].toFloat()
            val diff = kotlin.math.abs(close - open)
            val noise = (diff * 0.25f).coerceAtLeast(basePrice * 0.01f)
            val high = maxOf(open, close) + noise
            val low = maxOf(1f, minOf(open, close) - noise)
            list.add(floatArrayOf(open, high, low, close))
        }
        list
    }

    val maxPrice = maxOf(currentPrice.toFloat(), candles.maxOfOrNull { it[1] } ?: (currentPrice * 1.05f))
    val minPrice = minOf(currentPrice.toFloat(), candles.minOfOrNull { it[2] } ?: (currentPrice * 0.95f))
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1f)

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .background(Color(0xFF0D121B))
            .border(1.dp, ThemeBorder, RoundedCornerShape(6.dp))
    ) {
        val w = size.width
        val h = size.height

        val paddingX = 12.dp.toPx()
        val paddingY = 20.dp.toPx()
        val chartWidth = w - paddingX * 2 - 45.dp.toPx()
        val chartHeight = h - paddingY * 2

        // Grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = paddingY + (chartHeight * i / gridLines)
            drawLine(
                color = ThemeBorder.copy(alpha = 0.3f),
                start = Offset(paddingX, y),
                end = Offset(w - paddingX, y),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            val priceLabel = maxPrice - (priceRange * i / gridLines)
            drawText(
                textMeasurer = textMeasurer,
                text = "${priceLabel.toInt()}",
                topLeft = Offset(w - 48.dp.toPx(), y - 6.dp.toPx()),
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = RobotoMonoFontFamily
                )
            )
        }

        val numCandles = candles.size.coerceAtLeast(1)
        val candleWidth = chartWidth / numCandles
        val bodyWidth = (candleWidth * 0.7f).coerceAtLeast(2f)

        candles.forEachIndexed { i, c ->
            val open = c[0]
            val high = c[1]
            val low = c[2]
            val close = c[3]

            val isBullish = close >= open
            val candleColor = if (isBullish) ThemePositive else ThemeNegative

            val x = paddingX + (i * candleWidth) + candleWidth / 2f

            val highY = paddingY + chartHeight - ((high - minPrice) / priceRange) * chartHeight
            val lowY = paddingY + chartHeight - ((low - minPrice) / priceRange) * chartHeight
            val openY = paddingY + chartHeight - ((open - minPrice) / priceRange) * chartHeight
            val closeY = paddingY + chartHeight - ((close - minPrice) / priceRange) * chartHeight

            // Draw wick
            drawLine(
                color = candleColor,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 2f
            )

            // Draw body
            val topY = minOf(openY, closeY)
            val bottomY = maxOf(openY, closeY)
            val bodyHeight = maxOf(2f, bottomY - topY)

            drawRect(
                color = candleColor,
                topLeft = Offset(x - bodyWidth / 2f, topY),
                size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight)
            )
        }
    }
}

@Composable
fun StockLineChart(
    history: List<Long>,
    currentPrice: Long,
    basePrice: Long,
    productId: String,
    modifier: Modifier = Modifier
) {
    val points = if (history.size >= 2) history else {
        listOf((currentPrice * 0.95).toLong(), (currentPrice * 0.98).toLong(), currentPrice)
    }

    val maxPrice = maxOf(currentPrice.toFloat(), points.maxOrNull()?.toFloat() ?: (currentPrice * 1.05f))
    val minPrice = minOf(currentPrice.toFloat(), points.minOrNull()?.toFloat() ?: (currentPrice * 0.95f))
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1f)

    val isUp = currentPrice >= basePrice
    val lineColor = if (isUp) ThemePositive else ThemeNegative
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .background(Color(0xFF0D121B))
            .border(1.dp, ThemeBorder, RoundedCornerShape(6.dp))
    ) {
        val w = size.width
        val h = size.height

        val paddingX = 12.dp.toPx()
        val paddingY = 20.dp.toPx()
        val chartWidth = w - paddingX * 2 - 45.dp.toPx()
        val chartHeight = h - paddingY * 2

        // Grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = paddingY + (chartHeight * i / gridLines)
            drawLine(
                color = ThemeBorder.copy(alpha = 0.3f),
                start = Offset(paddingX, y),
                end = Offset(w - paddingX, y),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            val priceLabel = maxPrice - (priceRange * i / gridLines)
            drawText(
                textMeasurer = textMeasurer,
                text = "${priceLabel.toInt()}",
                topLeft = Offset(w - 48.dp.toPx(), y - 6.dp.toPx()),
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = RobotoMonoFontFamily
                )
            )
        }

        val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        var lastX = paddingX
        var lastY = paddingY + chartHeight

        points.forEachIndexed { i, pt ->
            val x = paddingX + i * stepX
            val y = paddingY + chartHeight - ((pt.toFloat() - minPrice) / priceRange) * chartHeight

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, paddingY + chartHeight)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            lastX = x
            lastY = y
        }

        fillPath.lineTo(lastX, paddingY + chartHeight)
        fillPath.close()

        // Translucent Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        // Line Stroke
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.5f)
        )

        // Glowing Current Value Dot
        drawCircle(
            color = lineColor.copy(alpha = 0.3f),
            radius = 10f,
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = lineColor,
            radius = 5f,
            center = Offset(lastX, lastY)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CompanyTickerTape(uiState: com.example.viewmodel.GameUiState) {
    val megaProjectsState = uiState.consortiumState.megaProjects
    val playerSharesState = uiState.guildsState.playerGuildShares
    val isEng = isEnglishLanguage()
    val activeProjects = remember(megaProjectsState) {
        megaProjectsState.filter { it.totalItemsProduced > 0 }
    }
    val sortedProjects = remember(activeProjects, playerSharesState) {
        activeProjects.map { proj ->
            val sharePrice = proj.currentSharePrice
            val changeVal = proj.sharePriceChangePercent
            Pair(proj, Pair(changeVal, sharePrice))
        }.sortedByDescending { it.second.first }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        color = Color(0xFF080C14),
        border = BorderStroke(1.dp, ThemeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .basicMarquee(iterations = Int.MAX_VALUE),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sortedProjects.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyText(
                        text = tr("AKTİF KONSORSİYUM ÜRÜNLERİ BEKLENİYOR...", "WAITING FOR ACTIVE CONSORTIUM PRODUCTS...", isEng),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color.Gray
                    )
                }
            } else {
                sortedProjects.forEach { (proj, pair) ->
                    val changeVal = pair.first
                    val sharePrice = pair.second
                    val isUp = changeVal >= 0
                    val sign = if (isUp) "+" else ""
                    val percentStr = String.format(java.util.Locale.US, "%.1f", changeVal)
                    val arrow = if (isUp) "▲" else "▼"
                    val trendColor = if (isUp) ThemePositive else ThemeNegative

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            text = proj.brandName.trAuto(isEng).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(
                            text = formatCurrency(sharePrice),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(
                            text = "$arrow $sign$percentStr%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = trendColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        CurrencyText(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeBorder
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BorsaActiveDeliveriesCard(
    activeDeliveries: List<com.example.data.DeliveryItem>
) {
    val isEng = isEnglishLanguage()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111927),
        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f))
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
                        imageVector = Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        tint = ThemeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = tr("Yoldaki Borsa & Lojistik Sevkiyatları", "Active Exchange Logistics", isEng) + " (${activeDeliveries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold,
                        fontFamily = RajdhaniFontFamily
                    )
                }
            }

            activeDeliveries.forEach { delivery ->
                val prod = Product.values().find { it.id == delivery.itemId }
                val prodName = prod?.getDisplayName() ?: delivery.itemId.uppercase()
                val originCityName = if (delivery.originCityId == "istanbul") "🏛️ İstanbul Borsa Deposu"
                    else if (delivery.originCityId == "new_york") "🏛️ New York Borsa Deposu"
                    else "🏙️ " + (com.example.data.cities.find { it.id == delivery.originCityId }?.name ?: delivery.originCityId)

                val destCityName = if (delivery.destinationCityId == "istanbul") "🏛️ İstanbul Borsa Deposu"
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF182234))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = "${delivery.quantity} Ton $prodName",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            CurrencyText(
                                text = "⏱️ $timeText",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    CurrencyText(
                        text = "$originCityName ➔ $destCityName",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (delivery.isOutboundSale) ThemeGold else ThemePositive,
                        trackColor = Color(0xFF283548),
                    )
                }
            }
        }
    }
}

