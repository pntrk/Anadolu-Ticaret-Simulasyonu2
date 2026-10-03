package com.example.ui.screens

import com.example.ui.components.CurrencyText
import com.example.ui.components.UniversalProductIcon
import com.example.ui.components.formatCredit


import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.ui.theme.isEnglishLanguage
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketListing
import com.example.data.Product
import com.example.data.ProductTier
import com.example.data.cities
import com.example.ui.components.AppButton
import com.example.ui.components.BloombergSparkline
import com.example.ui.components.CityNewsBulletinDialog
import com.example.ui.components.LiveBreakingNewsBanner
import com.example.ui.components.NotificationType
import com.example.ui.components.ParticleManager
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.TurkishLiraIcon
import com.example.ui.components.GameUnifiedDropdownMenu
import com.example.ui.components.GameUnifiedDropdownMenuItem
import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.isEnglishLanguage
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
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.viewmodel.GameViewModel
import com.example.ui.theme.RobotoMonoFontFamily
import androidx.compose.material.icons.filled.Lock
import com.example.ui.components.GlassCard
import com.example.ui.components.neonBorder
import com.example.ui.components.glassmorphism

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateHome: () -> Unit = {},
    onNavigateToProduction: ((productId: String?, cityId: String?) -> Unit)? = null,
    onNavigateToBorsa: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val theme = LocalAppThemeOption.current
    val isOnlineRegistered by viewModel.isOnlineRegistered.collectAsStateWithLifecycle()

    if (!isOnlineRegistered) {
        com.example.ui.components.OnlineFeatureLockGate(
            feature = com.example.ui.components.LockFeatureType.MARKET,
            viewModel = viewModel,
            onNavigateHome = onNavigateHome
        )
        return
    }

    DisposableEffect(Unit) {
        viewModel.setMarketScreenActive(true)
        onDispose {
            viewModel.setMarketScreenActive(false)
        }
    }

    val inventory = uiState.inventoryState.items
    val allListings = uiState.marketState.marketListings.filter { !it.isExpired }
    val player = uiState.playerState.player
    val marketPrices = uiState.marketState.prices
    val isExpertMode = uiState.settingsState.isExpertMode
    
    val activeDeliveries = uiState.inventoryState.activeDeliveries
    val buyOrders = uiState.marketState.buyOrders
    val auctions = uiState.marketState.auctions
    val priceHistory = uiState.marketState.priceHistory
    val macroState = uiState.macroState
    val newsTickerMessage = uiState.newsTickerMessage


    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateBuyOrderDialog by remember { mutableStateOf(false) }
    var showCreateAuctionDialog by remember { mutableStateOf(false) }
    var showCreateFuturesDialog by remember { mutableStateOf(false) }
    var showNewsBulletinDialog by remember { mutableStateOf(false) }
    val activeCityEvents = uiState.marketState.activeCityEvents
    val futuresContracts = uiState.marketState.futuresContracts
    var selectedListingForBuy by remember { mutableStateOf<MarketListing?>(null) }
    var selectedListingForEdit by remember { mutableStateOf<MarketListing?>(null) }
    var selectedListingForDetail by remember { mutableStateOf<MarketListing?>(null) }

    // Tab state: 0 = Tüm Pazar İlanları, 1 = Benim İlanlarım, 2 = Fırsat & Lojistik
    var selectedTab by remember { mutableIntStateOf(0) }

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf<ProductTier?>(null) }
    var selectedQualityFilter by remember { mutableStateOf<Int?>(null) }
    var selectedProductVarietyFilter by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("PRICE_ASC") } // PRICE_ASC, PRICE_DESC, QTY_DESC, LOGISTICS_ASC

    val myName = player?.name ?: ""
    val myUid = if (viewModel._onlineEmail.value.isNotBlank()) viewModel._onlineEmail.value.replace(".", "_") else if (player?.id?.isNotBlank() == true && player.id != "local_player") player.id else "trader_${myName.hashCode()}"
    val myCurrentCity = player?.currentCity ?: "istanbul"
    val myActiveListings = allListings.filter { it.sellerName.equals(myName, ignoreCase = true) || it.sellerId == myUid }
    val peerListings = allListings

    // Total market stats
    val totalVolume = allListings.sumOf { it.pricePerUnit * it.quantity }
    val totalListingsCount = allListings.size

    LaunchedEffect(Unit) {
        viewModel.refreshGlobalMarket()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO HEADER & B2B MARKET HUB BANNER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Live Anatolian Breaking News Banner (Ticker)
                    LiveBreakingNewsBanner(
                        events = activeCityEvents,
                        newsTickerMessage = newsTickerMessage,
                        macroState = macroState,
                        onClick = { showNewsBulletinDialog = true }
                    )




                    // Header Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Storefront,
                                            contentDescription = null,
                                            tint = ThemeNeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    CurrencyText(
                                        text = stringResource(R.string.market_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White
                                    )
                                    CurrencyText(
                                        text = stringResource(R.string.market_subtitle),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeSurface,
                                border = BorderStroke(1.dp, ThemeBorder),
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.refreshGlobalMarket()
                                        SmartNotificationManager.show("Pazar verileri güncelleniyor...", NotificationType.INFO)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Yenile",
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Create Listing Quick Action
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showCreateDialog = true
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeGold,
                                    contentColor = Color(0xFF1A1300)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddBusiness,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = stringResource(R.string.market_btn_new_listing),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Strategic Overview Stats Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 1: Player Balance
                        MarketOverviewStatCard(
                            title = "ŞİRKET KASASI".trAuto(),
                            value = formatMoney(player?.money ?: 0L),
                            subtitle = "Mevcut Bakiye".trAuto(),
                            customIcon = { CurrencyText("₳", color = ThemeGold, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            accentColor = ThemeGold,
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 2: Active Market Volume
                        MarketOverviewStatCard(
                            title = "PAZAR HACMİ".trAuto(),
                            value = formatMoney(totalVolume),
                            subtitle = "$totalListingsCount " + "Aktif Teklif".trAuto(),
                            customIcon = { CurrencyText("₳", color = ThemeNeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            accentColor = ThemeNeonCyan,
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 3: Player Active Listings
                        MarketOverviewStatCard(
                            title = "İLANLARINIZ".trAuto(),
                            value = "${myActiveListings.size} " + "İlan".trAuto(),
                            subtitle = "Yayındaki Teklif".trAuto(),
                            icon = Icons.Rounded.Sell,
                            accentColor = ThemePositive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. NAVIGATION TABS
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MarketTabButton(
                            title = "${stringResource(R.string.market_tab_global)} (${peerListings.size})",
                            icon = Icons.Rounded.ShoppingBag,
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                    }
                    item {
                        MarketTabButton(
                            title = "${stringResource(R.string.market_tab_my_listings)} (${myActiveListings.size})",
                            icon = Icons.Rounded.Sell,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                    item {
                        MarketTabButton(
                            title = tr("Vadeli Sözleşmeler (${futuresContracts.size})", "Futures Contracts (${futuresContracts.size})"),
                            icon = Icons.Rounded.Description,
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }
                    item {
                        MarketTabButton(
                            title = tr("Tedarik Talepleri (${buyOrders.size})", "Supply Requests (${buyOrders.size})"),
                            icon = Icons.Rounded.Assignment,
                            isSelected = selectedTab == 3,
                            onClick = { selectedTab = 3 }
                        )
                    }
                }
            }
            // 3. SEARCH & FILTERS SECTION (Dropdown Yapısına Dönüştürülmüş Filtre Grubu)
            if (selectedTab == 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Arama Çubuğu
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                CurrencyText("Ürün, şirket veya şehir ara...".trAuto(), color = Color.Gray, fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = ThemeNeonCyan)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
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

                        // Açılır Menü (Dropdown) Şeklinde Filtre Butonları (2x2 Düzeni: Kalite, Tier, Ürün Çeşidi, Sıralama)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // SATIR 1: KALİTE & TİER FİLTRELERİ
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. KALİTE FİLTRESİ DROPDOWN
                                var showQualityMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    val activeQualityText = when (selectedQualityFilter) {
                                        1 -> "⭐ 1★ Standart"
                                        2 -> "⭐⭐ 2★ İyi"
                                        3 -> "⭐⭐⭐ 3★ Premium"
                                        4 -> "⭐⭐⭐⭐ 4★ Lüks"
                                        5 -> "⭐⭐⭐⭐⭐ 5★ Ultra"
                                        else -> "⭐ Kalite: Tümü"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedQualityFilter != null) ThemeNeonCyan.copy(alpha = 0.18f) else Color(0xFF101726),
                                        border = BorderStroke(1.dp, if (selectedQualityFilter != null) ThemeNeonCyan else ThemeBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showQualityMenu = !showQualityMenu }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CurrencyText(
                                                text = activeQualityText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedQualityFilter != null) ThemeNeonCyan else Color.White,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = null,
                                                tint = if (selectedQualityFilter != null) ThemeNeonCyan else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    GameUnifiedDropdownMenu(
                                        expanded = showQualityMenu,
                                        onDismissRequest = { showQualityMenu = false },
                                        widthMin = 180.dp
                                    ) {
                                        GameUnifiedDropdownMenuItem(
                                            text = "Tüm Kaliteler".trAuto(),
                                            leadingIcon = Icons.Rounded.FilterList,
                                            iconTint = ThemeNeonCyan,
                                            isSelected = selectedQualityFilter == null,
                                            onClick = {
                                                selectedQualityFilter = null
                                                showQualityMenu = false
                                            }
                                        )
                                        HorizontalDivider(color = Color(0xFF26334D), thickness = 1.dp)
                                        listOf(1, 2, 3, 4, 5).forEach { stars ->
                                            val qObj = com.example.data.ItemQuality.fromStars(stars)
                                            GameUnifiedDropdownMenuItem(
                                                text = qObj.label,
                                                isSelected = selectedQualityFilter == stars,
                                                selectedColor = qObj.badgeColor,
                                                iconTint = qObj.badgeColor,
                                                onClick = {
                                                    selectedQualityFilter = stars
                                                    showQualityMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. KATEGORİ / TİER FİLTRESİ DROPDOWN
                                var showTierMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    val activeTierText = when (selectedTierFilter) {
                                        ProductTier.TIER_1 -> "🌾 Tier 1"
                                        ProductTier.TIER_2 -> "🏭 Tier 2"
                                        ProductTier.TIER_3 -> "📱 Tier 3"
                                        ProductTier.TIER_4 -> "🚀 Tier 4"
                                        else -> "📦 Tier: Tümü"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedTierFilter != null) ThemeGold.copy(alpha = 0.18f) else Color(0xFF101726),
                                        border = BorderStroke(1.dp, if (selectedTierFilter != null) ThemeGold else ThemeBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showTierMenu = !showTierMenu }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CurrencyText(
                                                text = activeTierText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTierFilter != null) ThemeGold else Color.White,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = null,
                                                tint = if (selectedTierFilter != null) ThemeGold else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    GameUnifiedDropdownMenu(
                                        expanded = showTierMenu,
                                        onDismissRequest = { showTierMenu = false },
                                        widthMin = 200.dp
                                    ) {
                                        GameUnifiedDropdownMenuItem(
                                            text = "Tüm Kategori & Tier'lar".trAuto(),
                                            leadingIcon = Icons.Rounded.Category,
                                            iconTint = ThemeGold,
                                            isSelected = selectedTierFilter == null,
                                            onClick = {
                                                selectedTierFilter = null
                                                showTierMenu = false
                                            }
                                        )
                                        HorizontalDivider(color = Color(0xFF26334D), thickness = 1.dp)
                                        GameUnifiedDropdownMenuItem(
                                            text = "🌾 Tier 1 - Ham Madde & Tarım",
                                            isSelected = selectedTierFilter == ProductTier.TIER_1,
                                            selectedColor = ThemeGold,
                                            onClick = { selectedTierFilter = ProductTier.TIER_1; showTierMenu = false }
                                        )
                                        GameUnifiedDropdownMenuItem(
                                            text = "🏭 Tier 2 - İşlenmiş Sanayi",
                                            isSelected = selectedTierFilter == ProductTier.TIER_2,
                                            selectedColor = ThemeNeonCyan,
                                            onClick = { selectedTierFilter = ProductTier.TIER_2; showTierMenu = false }
                                        )
                                        GameUnifiedDropdownMenuItem(
                                            text = "📱 Tier 3 - İleri Teknoloji",
                                            isSelected = selectedTierFilter == ProductTier.TIER_3,
                                            selectedColor = Color(0xFFE040FB),
                                            onClick = { selectedTierFilter = ProductTier.TIER_3; showTierMenu = false }
                                        )
                                        GameUnifiedDropdownMenuItem(
                                            text = "🚀 Tier 4 - Mega Projeler",
                                            isSelected = selectedTierFilter == ProductTier.TIER_4,
                                            selectedColor = Color(0xFF00E676),
                                            onClick = { selectedTierFilter = ProductTier.TIER_4; showTierMenu = false }
                                        )
                                    }
                                }
                            }

                            // SATIR 2: ÜRÜN ÇEŞİDİ & SIRALAMA FİLTRELERİ
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 3. ÜRÜN ÇEŞİDİ FİLTRESİ DROPDOWN
                                var showProductMenu by remember { mutableStateOf(false) }
                                val activeProductObj = Product.values().find { it.id == selectedProductVarietyFilter }
                                val activeProductText = activeProductObj?.let { it.getDisplayName() } ?: "Ürün Çeşidi: Tümü"

                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedProductVarietyFilter != null) Color(0xFFE040FB).copy(alpha = 0.18f) else Color(0xFF101726),
                                        border = BorderStroke(1.dp, if (selectedProductVarietyFilter != null) Color(0xFFE040FB) else ThemeBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showProductMenu = !showProductMenu }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                if (activeProductObj != null) {
                                                    UniversalProductIcon(product = activeProductObj, size = 18.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                CurrencyText(
                                                    text = activeProductText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selectedProductVarietyFilter != null) Color(0xFFE040FB) else Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = null,
                                                tint = if (selectedProductVarietyFilter != null) Color(0xFFE040FB) else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    GameUnifiedDropdownMenu(
                                        expanded = showProductMenu,
                                        onDismissRequest = { showProductMenu = false },
                                        widthMin = 220.dp
                                    ) {
                                        GameUnifiedDropdownMenuItem(
                                            text = "Tüm Ürün Çeşitleri".trAuto(),
                                            leadingIcon = Icons.Rounded.Inventory2,
                                            iconTint = Color(0xFFE040FB),
                                            isSelected = selectedProductVarietyFilter == null,
                                            onClick = {
                                                selectedProductVarietyFilter = null
                                                showProductMenu = false
                                            }
                                        )
                                        HorizontalDivider(color = Color(0xFF26334D), thickness = 1.dp)

                                        // Filtrelenmiş veya tüm ürün listesi (Gerçek Ürün Görselleri ile)
                                        val availableProducts = Product.values().filter { p ->
                                            selectedTierFilter == null || p.tier == selectedTierFilter
                                        }
                                        availableProducts.forEach { prod ->
                                            DropdownMenuItem(
                                                text = { CurrencyText(prod.getDisplayName(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { UniversalProductIcon(product = prod, size = 22.dp) },
                                                onClick = {
                                                    selectedProductVarietyFilter = prod.id
                                                    showProductMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 4. SIRALAMA DROPDOWN
                                var showSortMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    val activeSortText = when (sortBy) {
                                        "PRICE_ASC" -> "📈 Fiyat ↑"
                                        "PRICE_DESC" -> "📉 Fiyat ↓"
                                        "QTY_DESC" -> "📦 Miktar ↓"
                                        else -> "🚛 Lojistik ↑"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF101726),
                                        border = BorderStroke(1.dp, ThemeBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showSortMenu = !showSortMenu }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CurrencyText(
                                                text = activeSortText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    GameUnifiedDropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        widthMin = 200.dp
                                    ) {
                                        GameUnifiedDropdownMenuItem(
                                            text = "📈 Fiyat (Düşükten Yükseğe)",
                                            isSelected = sortBy == "PRICE_ASC",
                                            onClick = { sortBy = "PRICE_ASC"; showSortMenu = false }
                                        )
                                        GameUnifiedDropdownMenuItem(
                                            text = "📉 Fiyat (Yüksekten Düşüğe)",
                                            isSelected = sortBy == "PRICE_DESC",
                                            onClick = { sortBy = "PRICE_DESC"; showSortMenu = false }
                                        )
                                        GameUnifiedDropdownMenuItem(
                                            text = "📦 Miktar (En Yüksek)",
                                            isSelected = sortBy == "QTY_DESC",
                                            onClick = { sortBy = "QTY_DESC"; showSortMenu = false }
                                        )
                                        GameUnifiedDropdownMenuItem(
                                            text = "🚛 Lojistik (En Yakın Şehir)",
                                            isSelected = sortBy == "LOGISTICS_ASC",
                                            onClick = { sortBy = "LOGISTICS_ASC"; showSortMenu = false }
                                        )
                                    }
                                }
                            }

                            // Aktif Filtre Sıfırlama Butonu (eğer herhangi bir filtre aktifse)
                            if (selectedQualityFilter != null || selectedTierFilter != null || selectedProductVarietyFilter != null || searchQuery.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF261010),
                                    border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedQualityFilter = null
                                            selectedTierFilter = null
                                            selectedProductVarietyFilter = null
                                            searchQuery = ""
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Rounded.FilterAltOff, contentDescription = null, tint = ThemeNegative, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CurrencyText("Tüm Filtreleri Temizle", color = ThemeNegative, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. TAB CONTENTS
            when (selectedTab) {
                0 -> {
                    val filteredPeerListings = peerListings.filter { offer ->
                        val baseId = com.example.data.ItemQuality.extractBaseProductId(offer.itemId)
                        val product = Product.values().find { it.id == baseId }
                        val originCityName = cities.find { it.id == offer.originCityId }?.name ?: ""
                        val matchesSearch = searchQuery.isEmpty() ||
                                (product?.getDisplayName()?.contains(searchQuery, ignoreCase = true) == true) ||
                                offer.sellerName.contains(searchQuery, ignoreCase = true) ||
                                originCityName.contains(searchQuery, ignoreCase = true)
                        val matchesTier = selectedTierFilter == null || product?.tier == selectedTierFilter
                        val matchesQuality = selectedQualityFilter == null || offer.quality.stars == selectedQualityFilter
                        val matchesVariety = selectedProductVarietyFilter == null || baseId == selectedProductVarietyFilter
                        matchesSearch && matchesTier && matchesQuality && matchesVariety
                    }.sortedWith { a, b ->
                        val logCostA = viewModel.calculateLogisticsCost(a.originCityId, myCurrentCity, a.quantity)
                        val logCostB = viewModel.calculateLogisticsCost(b.originCityId, myCurrentCity, b.quantity)
                        
                        when (sortBy) {
                            "PRICE_ASC" -> a.pricePerUnit.compareTo(b.pricePerUnit)
                            "PRICE_DESC" -> b.pricePerUnit.compareTo(a.pricePerUnit)
                            "QTY_DESC" -> b.quantity.compareTo(a.quantity)
                            else -> logCostA.compareTo(logCostB)
                        }
                     
                        }

                    if (filteredPeerListings.isEmpty()) {
                        item {
                            EmptyMarketStateCard(
                                title = if (peerListings.isEmpty()) "Pazarda Teklif Bulunmuyor".trAuto() else "Aramaya Uygun İlan Yok".trAuto(),
                                description = if (peerListings.isEmpty())
                                    "Şu anda diğer üreticiler tarafından yayınlanmış aktif teklif yok. Kendi ürünlerinizi pazara sunabilirsiniz!".trAuto()
                                else
                                    "Arama kriterlerinizi değiştirerek veya filtreleri temizleyerek tekrar deneyebilirsiniz.".trAuto(),
                                icon = Icons.Rounded.SearchOff,
                                onCreateClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        itemsIndexed(filteredPeerListings, key = { _, it -> it.id }) { index, offer ->
                            com.example.ui.components.AnimatedListItem(index = index) {
                                val baseId = com.example.data.ItemQuality.extractBaseProductId(offer.itemId)
                                val product = Product.values().find { it.id == baseId }
                                val originCity = cities.find { it.id == offer.originCityId }
                                val originCityName = originCity?.name ?: offer.originCityId.replaceFirstChar { it.uppercase() }
                                val logisticsCost = viewModel.calculateLogisticsCost(offer.originCityId, myCurrentCity, offer.quantity)

                                val rawSpot = marketPrices.find { it.itemId == baseId }?.price ?: product?.basePrice ?: 0L
                                val spotPrice = (rawSpot * offer.quality.priceMultiplier).toLong()
                                val isMyListing = offer.sellerName.equals(myName, ignoreCase = true) || (offer.sellerId.isNotBlank() && offer.sellerId == myUid)

                                PeerListingCard(
                                    offer = offer,
                                    product = product,
                                    originCityName = originCityName,
                                    logisticsCost = logisticsCost,
                                    marketSpotPrice = spotPrice,
                                    isExpertMode = isExpertMode,
                                    isMyListing = isMyListing,
                                    onCardClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedListingForDetail = offer
                                    },
                                    onBuyClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isMyListing) {
                                            selectedListingForEdit = offer
                                        } else {
                                            selectedListingForDetail = offer
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // TAB 1: MY ACTIVE LISTINGS
                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("SATIŞTAKİ TEKLİFLERİNİZ (${myActiveListings.size})", "YOUR LISTINGS FOR SALE (${myActiveListings.size})"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = ThemeNeonCyan
                            )

                            CurrencyText(
                                text = "Pazara sunulan stoğunuz depolarınızdan düşülür".trAuto(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (myActiveListings.isEmpty()) {
                        item {
                            EmptyMarketStateCard(
                                title = "Aktif Satış İlanınız Yok".trAuto(),
                                description = "Depolarınızdaki fazla ham madde veya işlenmiş ürünleri B2B pazarda diğer oyunculara satarak nakit akışı sağlayabilirsiniz.".trAuto(),
                                icon = Icons.Rounded.Sell,
                                onCreateClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(myActiveListings, key = { it.id }) { myListing ->
                            val baseId = com.example.data.ItemQuality.extractBaseProductId(myListing.itemId)
                            val product = Product.values().find { it.id == baseId }
                            val cancelMsg = "İlan başarıyla iptal edildi ve ürünler depoya iade edildi.".trAuto()

                            MyActiveListingCard(
                                listing = myListing,
                                product = product,
                                onCardClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedListingForDetail = myListing
                                },
                                onCancelClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.CancelMarketListing(myListing.id))
                                    SmartNotificationManager.show(cancelMsg, NotificationType.INFO)
                                },
                                onEditClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedListingForEdit = myListing
                                }
                            )
                        }
                    }
                }
                2 -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = ThemeSurface,
                            borderWidth = 1.dp,
                            borderColor = ThemeGold,
                            pulsing = false
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            CurrencyText("Vadeli B2B Sözleşmeleri".trAuto(), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                                            CurrencyText("Fiyat dalgalanmalarına karşı sigortalı tedarik zinciri".trAuto(), color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                    AppButton(
                                        onClick = { showCreateFuturesDialog = true },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF1A1300))
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText("Yeni Sözleşme".trAuto(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (futuresContracts.isEmpty()) {
                        item {
                            EmptyMarketStateCard(
                                title = "Aktif Vadeli Sözleşme Yok".trAuto(),
                                description = "Piyasada henüz yayınlanmış bir vadeli sözleşme bulunmuyor. İlk sözleşmeyi siz imzalayabilirsiniz.".trAuto(),
                                icon = Icons.Rounded.Assignment,
                                onCreateClick = { showCreateFuturesDialog = true }
                            )
                        }
                    } else {
                        itemsIndexed(futuresContracts, key = { _, it -> it.id }) { index, contract ->
                            val product = Product.values().find { it.id == contract.itemId }
                            val prodName = product?.displayName ?: contract.itemId
                            val myUid = player?.id ?: "local"
                            val isMyContract = contract.creatorId == myUid || (player != null && contract.creatorName == player!!.name)

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeSurface,
                                borderWidth = 1.dp,
                                borderColor = if (isMyContract) ThemeGold.copy(alpha = 0.5f) else ThemeBorder,
                                pulsing = false
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(product?.icon ?: Icons.Rounded.Inventory2, contentDescription = null, tint = Color(product?.colorTint ?: 0xFF00E5FF), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CurrencyText("$prodName (${contract.quantity} Ton)", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Surface(shape = RoundedCornerShape(2.dp), color = ThemeGold.copy(alpha = 0.2f)) {
                                            CurrencyText(tr("${contract.durationDays} Gün Vade • ${contract.creatorName}", "${contract.durationDays} Days Term • ${contract.creatorName}"), color = ThemeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText("Kilitli Birim Fiyat:".trAuto(), color = Color.Gray, fontSize = 12.sp)
                                        CurrencyText("${formatCurrency(contract.lockedPricePerUnit, isEnglishLanguage())} / Ton", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    val invQty = inventory.find { it.itemId == contract.itemId }?.quantity ?: 0
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = if (invQty >= contract.quantity) ThemePositive.copy(alpha = 0.15f) else ThemeGold.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, if (invQty >= contract.quantity) ThemePositive else ThemeGold)
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (invQty >= contract.quantity) Icons.Rounded.CheckCircle else Icons.Rounded.Warehouse,
                                                contentDescription = null,
                                                tint = if (invQty >= contract.quantity) ThemePositive else ThemeGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            CurrencyText(
                                                text = if (invQty >= contract.quantity) tr("Depoda Hazır ($invQty / ${contract.quantity} T)", "Ready in Warehouse ($invQty / ${contract.quantity} T)") else tr("Depo Müdürü Ayarlıyor ($invQty / ${contract.quantity} T)", "Warehouse Manager Preparing ($invQty / ${contract.quantity} T)"),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (invQty >= contract.quantity) ThemePositive else ThemeGold
                                            )
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            CurrencyText("Toplam Değer:".trAuto(), color = Color.Gray, fontSize = 12.sp)
                                            CurrencyText(formatCurrency(contract.quantity * contract.lockedPricePerUnit, isEnglishLanguage()), color = ThemeGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        if (isMyContract) {
                                            AppButton(
                                                onClick = { viewModel.handleIntent(com.example.viewmodel.GameIntent.CancelFuturesContract(contract.id)) },
                                                shape = RoundedCornerShape(2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNegative.copy(alpha = 0.8f), contentColor = Color.White)
                                            ) {
                                                CurrencyText("İptal Et".trAuto(), fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            AppButton(
                                                onClick = { viewModel.handleIntent(com.example.viewmodel.GameIntent.FulfillFuturesContract(contract.id)) },
                                                shape = RoundedCornerShape(2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                                            ) {
                                                CurrencyText("Sözleşmeyi Karşıla".trAuto(), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = ThemeSurface,
                            borderWidth = 1.dp,
                            borderColor = ThemeNeonCyan,
                            pulsing = false
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            CurrencyText("Tedarik Talepleri (Alım Emirleri)".trAuto(), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                                            CurrencyText("Sıcak nakit için holdinglere ürün satın".trAuto(), color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                    AppButton(
                                        onClick = { showCreateBuyOrderDialog = true },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText("Talep Aç".trAuto(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (buyOrders.isEmpty()) {
                        item {
                            EmptyMarketStateCard(
                                title = "Aktif Tedarik Talebi Yok".trAuto(),
                                description = "Şu anda piyasada herhangi bir holding ürün talebinde bulunmuyor.".trAuto(),
                                icon = Icons.Rounded.Assignment,
                                onCreateClick = { showCreateBuyOrderDialog = true }
                            )
                        }
                    } else {
                        itemsIndexed(buyOrders, key = { _, it -> it.id }) { _, order ->
                            val product = Product.values().find { it.id == order.itemId }
                            val prodName = product?.displayName ?: order.itemId
                            val myUid = player?.id ?: "local"
                            val isMyOrder = order.buyerId == myUid || (player != null && order.buyerName == player!!.name)

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeSurface,
                                borderWidth = 1.dp,
                                borderColor = if (isMyOrder) ThemeNeonCyan.copy(alpha = 0.5f) else ThemeBorder,
                                pulsing = false
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(product?.icon ?: Icons.Rounded.Inventory2, contentDescription = null, tint = Color(product?.colorTint ?: 0xFF00E5FF), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CurrencyText("$prodName (${order.quantity} Ton)", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Surface(shape = RoundedCornerShape(2.dp), color = ThemeNeonCyan.copy(alpha = 0.2f)) {
                                            CurrencyText(tr("Alıcı: ${order.buyerName}", "Buyer: ${order.buyerName}"), color = ThemeNeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText("Talep Edilen Birim Fiyat:".trAuto(), color = Color.Gray, fontSize = 12.sp)
                                        CurrencyText("${formatCurrency(order.pricePerUnit, isEnglishLanguage())} / Ton", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    val orderInvQty = inventory.find { it.itemId == order.itemId }?.quantity ?: 0
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = if (orderInvQty >= order.quantity) ThemePositive.copy(alpha = 0.15f) else ThemeNeonCyan.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, if (orderInvQty >= order.quantity) ThemePositive else ThemeNeonCyan)
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (orderInvQty >= order.quantity) Icons.Rounded.CheckCircle else Icons.Rounded.Warehouse,
                                                contentDescription = null,
                                                tint = if (orderInvQty >= order.quantity) ThemePositive else ThemeNeonCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            CurrencyText(
                                                text = if (orderInvQty >= order.quantity) tr("Depoda Hazır ($orderInvQty / ${order.quantity} T)", "Ready in Warehouse ($orderInvQty / ${order.quantity} T)") else tr("Depo Müdürü Ayarlıyor ($orderInvQty / ${order.quantity} T)", "Warehouse Manager Preparing ($orderInvQty / ${order.quantity} T)"),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (orderInvQty >= order.quantity) ThemePositive else ThemeNeonCyan
                                            )
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            CurrencyText("Toplam Ödeme:".trAuto(), color = Color.Gray, fontSize = 12.sp)
                                            CurrencyText(formatCurrency(order.quantity * order.pricePerUnit, isEnglishLanguage()), color = ThemePositive, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        if (isMyOrder) {
                                            AppButton(
                                                onClick = { viewModel.handleIntent(com.example.viewmodel.GameIntent.CancelBuyOrder(order.id)) },
                                                shape = RoundedCornerShape(2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNegative.copy(alpha = 0.8f), contentColor = Color.White)
                                            ) {
                                                CurrencyText("Talebi İptal Et".trAuto(), fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            AppButton(
                                                onClick = { viewModel.handleIntent(com.example.viewmodel.GameIntent.SellToBuyOrder(order.id)) },
                                                shape = RoundedCornerShape(2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.White)
                                            ) {
                                                CurrencyText("Tedarik Et".trAuto(), fontWeight = FontWeight.Bold)
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

    // 5. CREATE LISTING DIALOG
    if (showCreateDialog) {
        val successMsg = "İlanınız B2B pazarda yayınlandı!".trAuto()
        val errorMsg = "Hata: Depoda yeterli stok yok veya miktar geçersiz!".trAuto()
        CreateListingDialog(
            inventory = inventory,
            marketPrices = marketPrices,
            onDismiss = { showCreateDialog = false },
            onSubmit = { itemId, qty, price ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.CreateMarketListing(itemId, qty, price))
                val success = true
                if (success) {
                    SmartNotificationManager.show(successMsg, NotificationType.SUCCESS)
                    ParticleManager.spawnCelebration()
                    showCreateDialog = false
                } else {
                    SmartNotificationManager.show(errorMsg, NotificationType.ALERT)
                }
            }
        )
    }

    if (showCreateFuturesDialog) {
        CreateFuturesContractDialog(
            inventory = inventory,
            marketPrices = marketPrices,
            onDismiss = { showCreateFuturesDialog = false },
            onSubmit = { itemId, qty, price, days ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.CreateFuturesContract(itemId, qty, price, days, true))
                val success = true
                if (success) {
                    showCreateFuturesDialog = false
                }
            }
        )
    }

    // CREATE BUY ORDER DIALOG
    if (showCreateBuyOrderDialog) {
        val successMsg = "Alım emri pazara başarıyla eklendi!".trAuto()
        val errorMsg = "Yetersiz bakiye veya geçersiz miktar!".trAuto()
        CreateBuyOrderDialog(
            marketPrices = marketPrices,
            playerMoney = player?.money ?: 0L,
            onDismiss = { showCreateBuyOrderDialog = false },
            onSubmit = { itemId, qty, pricePerUnit ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.CreateBuyOrder(itemId, qty, pricePerUnit))
                val success = true
                if (success) {
                    SmartNotificationManager.show(successMsg, NotificationType.SUCCESS)
                    ParticleManager.spawnCelebration()
                    showCreateBuyOrderDialog = false
                } else {
                    SmartNotificationManager.show(errorMsg, NotificationType.ALERT)
                }
            }
        )
    }

    if (showNewsBulletinDialog) {
        val isEng = isEnglishLanguage()
        CityNewsBulletinDialog(
            events = activeCityEvents,
            newsTickerMessage = newsTickerMessage,
            macroState = macroState,
            bulletinOpportunities = uiState.bulletinOpportunities,
            ownedFacilities = uiState.businesses,
            onClaimReward = { oppId ->
                onIntent(com.example.viewmodel.GameIntent.ClaimBulletinOpportunityReward(oppId))
            },
            onQuickProduce = { oppId ->
                onIntent(com.example.viewmodel.GameIntent.QuickProduceForBulletinOpportunity(oppId))
            },
            onNavigateToFacility = { prodId, cityId ->
                showNewsBulletinDialog = false
                onNavigateToProduction?.invoke(prodId, cityId)
            },
            onNavigateToBorsa = {
                showNewsBulletinDialog = false
                onNavigateToBorsa?.invoke()
            },
            onNavigateToMarket = {
                showNewsBulletinDialog = false
            },
            onDismiss = { showNewsBulletinDialog = false },
            onNavigateToCity = { targetCityId ->
                showNewsBulletinDialog = false
                val cityUpper = targetCityId.uppercase()
                val msg = if (isEng) "Target city selected: $cityUpper" else "Hedef şehir seçildi: $cityUpper"
                SmartNotificationManager.show(msg, NotificationType.INFO)
            }
        )
    }

    // CREATE AUCTION DIALOG
    if (showCreateAuctionDialog) {
        val auctionStartedMsg = "Müzayede başlatıldı!".trAuto()
        CreateAuctionDialog(
            inventory = inventory,
            marketPrices = marketPrices,
            onDismiss = { showCreateAuctionDialog = false },
            onSubmit = { itemId, qty, startingBid ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.AddAuction(itemId, qty, startingBid))
                SmartNotificationManager.show(auctionStartedMsg, NotificationType.SUCCESS)
                ParticleManager.spawnCelebration()
                showCreateAuctionDialog = false
            }
        )
    }

    val activeListingForDetail = selectedListingForDetail ?: selectedListingForBuy
    if (activeListingForDetail != null) {
        val listing = activeListingForDetail
        val baseId = com.example.data.ItemQuality.extractBaseProductId(listing.itemId)
        val product = Product.values().find { it.id == baseId }
        val isMyListing = listing.sellerName.equals(myName, ignoreCase = true) || (listing.sellerId.isNotBlank() && listing.sellerId == myUid)

        MarketListingDetailsDialog(
            listing = listing,
            product = product,
            playerMoney = player?.money ?: 0L,
            currentCity = myCurrentCity,
            viewModel = viewModel,
            isMyListing = isMyListing,
            onDismiss = {
                selectedListingForDetail = null
                selectedListingForBuy = null
            },
            onConfirmBuy = { buyQty ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromGlobalMarket(listing.id, buyQty))
                selectedListingForDetail = null
                selectedListingForBuy = null
            },
            onEditPrice = {
                selectedListingForDetail = null
                selectedListingForBuy = null
                selectedListingForEdit = listing
            },
            onCancelListing = {
                selectedListingForDetail = null
                selectedListingForBuy = null
                viewModel.handleIntent(com.example.viewmodel.GameIntent.CancelMarketListing(listing.id))
                com.example.ui.components.SmartNotificationManager.show("İlan başarıyla iptal edildi ve ürünler depoya iade edildi.", com.example.ui.components.NotificationType.INFO)
            }
        )
    }

    if (selectedListingForEdit != null) {
        val listing = selectedListingForEdit!!
        val baseId = com.example.data.ItemQuality.extractBaseProductId(listing.itemId)
        val product = Product.values().find { it.id == baseId }
        EditListingDialog(
            uiState = uiState,
            listing = listing,
            product = product,
            viewModel = viewModel,
            onDismiss = { selectedListingForEdit = null }
        )
    }
}

// ==========================================
// COMPONENT: OVERVIEW STAT CARD
// ==========================================
@Composable
fun MarketOverviewStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = ThemeSurface,
        borderWidth = 1.dp,
        borderColor = accentColor,
        pulsing = false
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = title.trAuto(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            CurrencyText(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            CurrencyText(
                text = subtitle.trAuto(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// COMPONENT: TAB BUTTON
// ==========================================
@Composable
fun MarketTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.15f) else Color(0xFF141A29),
        border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ThemeNeonCyan else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            CurrencyText(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = RobotoMonoFontFamily,
                color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// COMPONENT: PEER LISTING CARD (MARKET OFFER)
// ==========================================
@Composable
fun PeerListingCard(
    offer: MarketListing,
    product: Product?,
    originCityName: String,
    logisticsCost: Long,
    marketSpotPrice: Long = 0L,
    isExpertMode: Boolean = false,
    isMyListing: Boolean = false,
    onCardClick: () -> Unit = {},
    onBuyClick: () -> Unit = {}
) {
    val baseId = com.example.data.ItemQuality.extractBaseProductId(offer.itemId)
    val itemName = product?.displayName ?: baseId
    val brandColor = if (product != null) Color(product.colorTint) else ThemeNeonCyan
    val offerQuality = offer.quality
    val qualityBorderColor = offerQuality.badgeColor

    val totalProductPrice = offer.pricePerUnit * offer.quantity
    val totalDeliveredCost = totalProductPrice + logisticsCost

    val avgCostPerUnit = if (offer.quantity > 0) totalDeliveredCost / offer.quantity else offer.pricePerUnit
    val isOpportunity = !isMyListing && marketSpotPrice > 0 && avgCostPerUnit < (marketSpotPrice * 0.88)
    val discountPercent = if (marketSpotPrice > 0) ((marketSpotPrice - avgCostPerUnit).toFloat() / marketSpotPrice.toFloat() * 100).toInt().coerceAtLeast(0) else 0

    val tierText = when (product?.tier) {
        ProductTier.TIER_1 -> tr("T1 Ham", "T1 Raw")
        ProductTier.TIER_2 -> tr("T2 İşlenmiş", "T2 Processed")
        ProductTier.TIER_3 -> tr("T3 Nihai", "T3 Final")
        ProductTier.TIER_4 -> tr("T4 Mega", "T4 Mega")
        else -> "T1"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF111726),
        borderWidth = if (isOpportunity) 2.dp else if (isMyListing) 1.5.dp else 1.dp,
        borderColor = if (isOpportunity) ThemePositive else if (isMyListing) ThemeGold else qualityBorderColor.copy(alpha = 0.5f),
        pulsing = isOpportunity
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. HEADER ROW: Seller, Origin City, Quality Badge & Owner Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isMyListing) ThemeGold.copy(alpha = 0.2f) else ThemeNeonCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMyListing) Icons.Rounded.Person else Icons.Rounded.Business,
                                contentDescription = null,
                                tint = if (isMyListing) ThemeGold else ThemeNeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = if (isMyListing) tr("Siz (Kendi İlanınız)", "You (Your Listing)") else offer.sellerName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMyListing) ThemeGold else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = ThemeGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        CurrencyText(
                            text = originCityName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeGold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Quality Badge (Prominently displayed)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isMyListing) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeGold)
                        ) {
                            CurrencyText(
                                text = "SİZİN İLANINIZ".trAuto(),
                                fontSize = 9.sp,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = offerQuality.badgeColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, offerQuality.badgeColor.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = offerQuality.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = offerQuality.badgeColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = "x${offerQuality.priceMultiplier}",
                                fontSize = 10.sp,
                                fontFamily = RobotoMonoFontFamily,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // 3-Day Maximum Expiration Countdown Pill Badge
                    val remMs = offer.remainingMs
                    val remDays = remMs / (24 * 3600 * 1000L)
                    val remHours = (remMs % (24 * 3600 * 1000L)) / (3600 * 1000L)
                    val remTimeText = if (remDays > 0) "${remDays}g ${remHours}sa" else "${remHours}sa"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF161F30),
                        border = BorderStroke(1.dp, if (remDays == 0L) ThemeNegative.copy(alpha = 0.6f) else Color(0xFF3B4861))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = "⏳ $remTimeText",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remDays == 0L) ThemeNegative else Color(0xFFCBD5E1),
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }

            if (isOpportunity && discountPercent >= 10) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = ThemePositive.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, ThemePositive.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(
                            text = tr("PİYASA FIRSATI: Spot ortalamasından %${discountPercent} daha uygun!", "MARKET OPPORTUNITY: ${discountPercent}% cheaper than spot!"),
                            color = ThemePositive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. MAIN BODY ROW: Product Icon, Quantity & Unit Price, Delivered Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = brandColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (product != null) UniversalProductIcon(product, 24.dp, brandColor) else Icon(Icons.Rounded.ShoppingCart, null, tint = brandColor, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = itemName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isExpertMode) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(
                                    text = "[$tierText]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = brandColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = "${offer.quantity} Ton",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                color = ThemeNeonCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = "(${formatMoney(offer.pricePerUnit)} / Ton)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Price Summary Column (Right-aligned, balanced)
                Column(horizontalAlignment = Alignment.End) {
                    CurrencyText(
                        text = formatMoney(totalDeliveredCost),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold
                    )
                    CurrencyText(
                        text = if (logisticsCost == 0L) tr("Aynı Şehir Teslimat", "Same City Delivery") else tr("Teslimat: +${formatMoney(logisticsCost)}", "Delivery: +${formatMoney(logisticsCost)}"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (logisticsCost == 0L) ThemePositive else Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }

            // 3. ACTION ROW: Shipping Route Info + Inspect / Buy Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(0.5.dp, ThemeBorder),
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        CurrencyText(
                            text = if (logisticsCost == 0L) "Aynı Şehir (Ücretsiz Nakliye)".trAuto() else tr("$originCityName ➔ Deponuz", "$originCityName ➔ Warehouse"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AppButton(
                    onClick = onBuyClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMyListing) ThemeGold else ThemeNeonCyan,
                        contentColor = if (isMyListing) Color(0xFF1A1300) else Color(0xFF002026)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isMyListing) Icons.Rounded.Edit else Icons.Rounded.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = if (isMyListing) "Yönet / Fiyat".trAuto() else "İncele / Al".trAuto(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: MY ACTIVE LISTING CARD
// ==========================================
@Composable
fun MyActiveListingCard(
    listing: MarketListing,
    product: Product?,
    onCardClick: () -> Unit = {},
    onCancelClick: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    val itemName = product?.displayName ?: listing.itemId
    val brandColor = if (product != null) Color(product.colorTint) else ThemeNeonCyan
    val totalRevenue = listing.pricePerUnit * listing.quantity

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(4.dp),
        color = ThemeSurface,
        borderWidth = 1.dp,
        borderColor = ThemeNeonCyan.copy(alpha = 0.4f),
        pulsing = false
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if(product!=null) UniversalProductIcon(product, 20.dp, brandColor) else Icon(Icons.Rounded.ShoppingCart, null, tint=brandColor, modifier=Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        val listingQuality = listing.quality
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = itemName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = listingQuality.badgeColor.copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, listingQuality.badgeColor.copy(alpha = 0.8f))
                            ) {
                                CurrencyText(
                                    text = listingQuality.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = listingQuality.badgeColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        CurrencyText(
                            text = "${stringResource(R.string.market_quantity)} ${listing.quantity} Ton",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeNeonCyan,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Price details
                Column(horizontalAlignment = Alignment.End) {
                    val stateTax = (totalRevenue * 0.05f).toLong()
                    val netRevenue = totalRevenue - stateTax
                    CurrencyText(
                        text = formatMoney(netRevenue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CurrencyText(
                        text = "Net Kazanç (Vergi Düşüldü)".trAuto(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CurrencyText(
                        text = "${stringResource(R.string.market_price_per_ton)} ${formatMoney(listing.pricePerUnit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = ThemePositiveBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ThemePositive)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CurrencyText(
                            text = "Pazarda Yayında".trAuto(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemePositive,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, ThemeNeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNeonCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(text = "Fiyatı Güncelle".trAuto(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, ThemeNegative),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNegative),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(
                            text = "İptal Et".trAuto(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: EMPTY STATE CARD
// ==========================================
@Composable
fun EmptyMarketStateCard(
    title: String,
    description: String,
    icon: ImageVector,
    onCreateClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = ThemeSurface,
        borderWidth = 1.dp,
        borderColor = ThemeBorder,
        pulsing = false
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ThemeNeonCyan.copy(alpha = 0.2f),
                    modifier = Modifier.size(72.dp).blur(4.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = ThemeNeonCyan.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha=0.3f)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ThemeNeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CurrencyText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            CurrencyText(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                onClick = onCreateClick,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeGold,
                    contentColor = Color(0xFF1A1300)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddBusiness,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CurrencyText(
                    text = "Pazara İlan Ver".trAuto(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily
                )
            }
        }
    }
}

// ==========================================
// DIALOG: CREATE LISTING DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingDialog(
    inventory: List<com.example.data.InventoryEntity>,
    marketPrices: List<com.example.data.MarketPriceEntity>,
    onDismiss: () -> Unit,
    onSubmit: (itemId: String, quantity: Int, pricePerUnit: Long) -> Unit
) {
    val availableInventory = inventory.filter { it.quantity > 0 }
    var selectedItemId by remember { mutableStateOf(availableInventory.firstOrNull()?.itemId ?: "") }

    val currentInv = availableInventory.find { it.itemId == selectedItemId }
    val currentBaseId = currentInv?.baseProductId ?: com.example.data.ItemQuality.extractBaseProductId(selectedItemId)
    val currentQuality = currentInv?.quality ?: com.example.data.ItemQuality.extractQuality(selectedItemId)
    val maxStock = currentInv?.quantity ?: 0

    var customQuantityText by remember { mutableStateOf((maxStock.coerceAtMost(10)).toString()) }
    val spotPrice = remember(selectedItemId, marketPrices) {
        val prod = Product.values().find { it.id == currentBaseId }
        val rawBase = marketPrices.find { it.itemId == currentBaseId }?.price ?: prod?.basePrice ?: 100L
        (rawBase * currentQuality.priceMultiplier).toLong()
    }
    val minPrice = (spotPrice * 0.60f).toLong()
    val maxPrice = (spotPrice * 1.60f).toLong()

    var customPriceText by remember { mutableStateOf(spotPrice.toString()) }

    val parsedQty = customQuantityText.toIntOrNull() ?: 0
    val parsedPrice = customPriceText.toLongOrNull() ?: 0L
    val isPriceValid = parsedPrice in minPrice..maxPrice
    val totalRevenue = parsedQty * parsedPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121824),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AddBusiness,
                    contentDescription = null,
                    tint = ThemeGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyText(
                    text = "Pazara İlan Ver".trAuto(),
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (availableInventory.isEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CurrencyText("Deponuzda satışa çıkarılabilecek ham madde veya işlenmiş ürün bulunmuyor.".trAuto())
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Product Selection Dropdown
                    CurrencyText(
                        text = "Satılacak Ürün:".trAuto(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ThemeNeonCyan,
                        fontWeight = FontWeight.Bold
                    )

                    var expanded by remember { mutableStateOf(false) }
                    val currentProduct = Product.values().find { it.id == currentBaseId }
                    val brandColor = if (currentProduct != null) Color(currentProduct.colorTint) else ThemeNeonCyan

                    Box {
                        Surface(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1A2130),
                            border = BorderStroke(1.dp, ThemeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if(currentProduct!=null) UniversalProductIcon(currentProduct, 20.dp, brandColor) else Icon(Icons.Rounded.ShoppingCart, null, tint=brandColor, modifier=Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    CurrencyText(
                                        text = "${currentProduct?.displayName ?: currentBaseId} [${currentQuality.label}] (Stok: $maxStock Ton)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(Color(0xFF1A2130))
                                .heightIn(max = 280.dp)
                        ) {
                            availableInventory.forEach { invItem ->
                                val bId = invItem.baseProductId
                                val prod = Product.values().find { it.id == bId }
                                val q = invItem.quality
                                val pColor = if (prod != null) Color(prod.colorTint) else ThemeNeonCyan

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if(prod!=null) UniversalProductIcon(prod, 18.dp, pColor) else Icon(Icons.Rounded.ShoppingCart, null, tint=pColor, modifier=Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            CurrencyText(
                                                text = "${prod?.displayName ?: bId} [${q.label}] - ${invItem.quantity} Ton",
                                                color = Color.White
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedItemId = invItem.itemId
                                        val newMax = invItem.quantity
                                        customQuantityText = (newMax.coerceAtMost(10)).toString()
                                        val rawBase = marketPrices.find { it.itemId == bId }?.price ?: prod?.basePrice ?: 100L
                                        val recP = (rawBase * q.priceMultiplier).toLong()
                                        customPriceText = recP.toString()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Slider & Quick Quantity Presets
                    val currentQtyVal = (customQuantityText.toIntOrNull() ?: 1).coerceIn(1, maxStock.coerceAtLeast(1))
                    if (maxStock > 1) {
                        Slider(
                            value = currentQtyVal.toFloat(),
                            onValueChange = { customQuantityText = it.toInt().toString() },
                            valueRange = 1f..maxStock.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeNeonCyan,
                                activeTrackColor = ThemeNeonCyan,
                                inactiveTrackColor = Color(0xFF1E2838)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "%25" to 0.25f,
                            "%50" to 0.50f,
                            "%75" to 0.75f,
                            tr("Maks (%100)", "Max (100%)") to 1.0f
                        ).forEach { (label, ratio) ->
                            val presetVal = (maxStock * ratio).toInt().coerceAtLeast(1)
                            val isSelected = currentQtyVal == presetVal

                            Surface(
                                onClick = { customQuantityText = presetVal.toString() },
                                shape = RoundedCornerShape(2.dp),
                                color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1A2130),
                                border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else ThemeBorder),
                                modifier = Modifier.weight(if (ratio == 1.0f) 1.3f else 1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CurrencyText(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // Quantity Input
                    OutlinedTextField(
                        value = customQuantityText,
                        onValueChange = { customQuantityText = it },
                        label = { CurrencyText(tr("Miktar (Ton) - Max: $maxStock", "Quantity (Tons) - Max: $maxStock")) },
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

                    // Price Input & Market Reference
                    OutlinedTextField(
                        value = customPriceText,
                        onValueChange = { customPriceText = it },
                        label = { CurrencyText("Birim Fiyat (₳/Ton)".trAuto()) },
                        isError = !isPriceValid && customPriceText.isNotEmpty(),
                        supportingText = {
                            if (!isPriceValid && customPriceText.isNotEmpty()) {
                                CurrencyText(
                                    text = tr("TCMB Regülasyonu: Fiyat ${formatMoney(minPrice)} ile ${formatMoney(maxPrice)} arasında olmalıdır.", "CBRT Regulation: Price must be between ${formatMoney(minPrice)} and ${formatMoney(maxPrice)}."),
                                    color = ThemeNegative,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                CurrencyText(tr("Borsa Referans Fiyatı: ${formatMoney(spotPrice)}", "Stock Exchange Reference Price: ${formatMoney(spotPrice)}"), color = ThemeGold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isPriceValid) ThemeGold else ThemeNegative,
                            unfocusedBorderColor = if (isPriceValid) ThemeBorder else ThemeNegative,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Financial Calculation Preview
                    val stateTax = (totalRevenue * 0.05f).toLong()
                    val netRevenue = totalRevenue - stateTax
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1A2130),
                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = "Brüt Satış Geliri:".trAuto(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                CurrencyText(
                                    text = formatMoney(totalRevenue),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color.White
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = "${stringResource(R.string.trade_tax)} (%5):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeNegative
                                )
                                CurrencyText(
                                    text = "-${formatMoney(stateTax)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeNegative
                                )
                            }
                            HorizontalDivider(color = ThemeBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = "Tahmini Net Kazanç:".trAuto(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold
                                )
                                CurrencyText(
                                    text = formatMoney(netRevenue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availableInventory.isNotEmpty()) {
                val isFormValid = parsedQty in 1..maxStock && isPriceValid
                val regulationErrorMsg = tr("TCMB Regülasyonu: Fiyat ${formatMoney(minPrice)} ile ${formatMoney(maxPrice)} arasında olmalıdır.", "CBRT Regulation: Price must be between ${formatMoney(minPrice)} and ${formatMoney(maxPrice)}.")
                val invalidFormErrorMsg = "Geçersiz miktar veya fiyat!".trAuto()
                AppButton(
                    onClick = {
                        if (isFormValid) {
                            onSubmit(selectedItemId, parsedQty, parsedPrice)
                        } else {
                            if (!isPriceValid) {
                                SmartNotificationManager.show(regulationErrorMsg, NotificationType.ALERT)
                            } else {
                                SmartNotificationManager.show(invalidFormErrorMsg, NotificationType.ALERT)
                            }
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeGold,
                        contentColor = Color(0xFF1A1300),
                        disabledContainerColor = Color(0xFF2A2A38),
                        disabledContentColor = Color.Gray
                    )
                ) {
                    CurrencyText("İlanı Yayınla".trAuto(), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("İptal".trAuto(), color = Color.Gray)
            }
        }
    )
}

// ==========================================
// DIALOG: CREATE BUY ORDER & AUCTION DIALOGS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBuyOrderDialog(
    marketPrices: List<com.example.data.MarketPriceEntity>,
    playerMoney: Long,
    onDismiss: () -> Unit,
    onSubmit: (itemId: String, quantity: Int, pricePerUnit: Long) -> Unit
) {
    val allProducts = Product.values()
    var selectedItemId by remember { mutableStateOf(allProducts.firstOrNull()?.id ?: "") }
    var quantityText by remember { mutableStateOf("10") }
    
    val spotPrice = remember(selectedItemId, marketPrices) {
        val prod = Product.values().find { it.id == selectedItemId }
        marketPrices.find { it.itemId == selectedItemId }?.price ?: prod?.basePrice ?: 100L
    }
    var priceText by remember { mutableStateOf(spotPrice.toString()) }

    val qty = quantityText.toIntOrNull() ?: 0
    val pricePerUnit = priceText.toLongOrNull() ?: 0L
    val totalCost = try { Math.multiplyExact(qty.toLong(), pricePerUnit) } catch(e: Exception) { Long.MAX_VALUE }
    val canAfford = playerMoney >= totalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121824),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyText("Alım Emri Ver".trAuto(), fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CurrencyText("Ürün Seçin:".trAuto(), style = MaterialTheme.typography.labelMedium, color = ThemeNeonCyan, fontWeight = FontWeight.Bold)
                
                var expanded by remember { mutableStateOf(false) }
                val currentProduct = Product.values().find { it.id == selectedItemId }
                val brandColor = if (currentProduct != null) Color(currentProduct.colorTint) else ThemeNeonCyan

                Box {
                    Surface(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1A2130),
                        border = BorderStroke(1.dp, ThemeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(currentProduct?.icon ?: Icons.Rounded.Inventory2, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                CurrencyText(currentProduct?.displayName ?: selectedItemId, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF162032))
                    ) {
                        Product.values().forEach { prod ->
                            DropdownMenuItem(
                                text = { CurrencyText(prod.displayName, color = Color.White) },
                                onClick = {
                                    selectedItemId = prod.id
                                    val newSpot = marketPrices.find { it.itemId == prod.id }?.price ?: prod.basePrice
                                    priceText = newSpot.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Quantity Percentage Presets
                val maxAffordable = if (pricePerUnit > 0) (playerMoney / pricePerUnit).toInt().coerceIn(1, 10000) else 100
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "%25" to 0.25f,
                        "%50" to 0.50f,
                        "%75" to 0.75f,
                        tr("Maks (%100)", "Max (100%)") to 1.0f
                    ).forEach { (label, ratio) ->
                        val presetVal = (maxAffordable * ratio).toInt().coerceAtLeast(1)
                        val isSelected = qty == presetVal

                        Surface(
                            onClick = { quantityText = presetVal.toString() },
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1A2130),
                            border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else ThemeBorder),
                            modifier = Modifier.weight(if (ratio == 1.0f) 1.3f else 1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CurrencyText(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantityText = it },
                    label = { CurrencyText("Miktar (Ton)".trAuto(), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonCyan, unfocusedBorderColor = ThemeBorder)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) priceText = it },
                    label = { CurrencyText("Birim Fiyat (₳)".trAuto(), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonCyan, unfocusedBorderColor = ThemeBorder)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CurrencyText("Toplam Maliyet:".trAuto(), color = Color.Gray, fontSize = 12.sp)
                    CurrencyText(formatMoney(totalCost), color = if (canAfford) ThemeGold else ThemeNegative, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CurrencyText("Cüzdan Bakiye:".trAuto(), color = Color.Gray, fontSize = 12.sp)
                    CurrencyText(formatMoney(playerMoney), color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { if (qty > 0 && pricePerUnit > 0 && canAfford) onSubmit(selectedItemId, qty, pricePerUnit) },
                enabled = qty > 0 && pricePerUnit > 0 && canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
            ) {
                CurrencyText("Alım Emri Yayınla".trAuto(), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { CurrencyText("İptal".trAuto(), color = Color.Gray) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAuctionDialog(
    inventory: List<com.example.data.InventoryEntity>,
    marketPrices: List<com.example.data.MarketPriceEntity>,
    onDismiss: () -> Unit,
    onSubmit: (itemId: String, quantity: Int, startingBid: Long) -> Unit
) {
    val availableInventory = inventory.filter { it.quantity > 0 }
    var selectedItemId by remember { mutableStateOf(availableInventory.firstOrNull()?.itemId ?: "") }

    val currentInv = availableInventory.find { it.itemId == selectedItemId }
    val maxStock = currentInv?.quantity ?: 0

    var quantityText by remember { mutableStateOf((maxStock.coerceAtMost(5)).toString()) }

    val spotPrice = remember(selectedItemId, marketPrices) {
        val prod = Product.values().find { it.id == selectedItemId }
        marketPrices.find { it.itemId == selectedItemId }?.price ?: prod?.basePrice ?: 100L
    }
    var startingBidText by remember { mutableStateOf((spotPrice * 0.8f).toLong().toString()) }

    val qty = quantityText.toIntOrNull() ?: 0
    val startingBid = startingBidText.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121824),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Gavel, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyText("Müzayede Başlat".trAuto(), fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (availableInventory.isEmpty()) {
                CurrencyText("Müzayedeye çıkarılacak deponuzda ürün bulunmuyor.".trAuto())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CurrencyText("Ürün Seçin:".trAuto(), style = MaterialTheme.typography.labelMedium, color = ThemeNeonCyan, fontWeight = FontWeight.Bold)

                    var expanded by remember { mutableStateOf(false) }
                    val currentProduct = Product.values().find { it.id == selectedItemId }
                    val brandColor = if (currentProduct != null) Color(currentProduct.colorTint) else ThemeNeonCyan

                    Box {
                        Surface(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1A2130),
                            border = BorderStroke(1.dp, ThemeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(currentProduct?.icon ?: Icons.Rounded.Inventory2, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    CurrencyText(tr("${currentProduct?.displayName ?: selectedItemId} (Stok: $maxStock Ton)", "${currentProduct?.displayName ?: selectedItemId} (Stock: $maxStock Tons)"), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF162032))
                        ) {
                            availableInventory.forEach { inv ->
                                val prod = Product.values().find { it.id == inv.itemId }
                                DropdownMenuItem(
                                    text = { CurrencyText(tr("${prod?.displayName ?: inv.itemId} (${inv.quantity} Ton)", "${prod?.displayName ?: inv.itemId} (${inv.quantity} Tons)"), color = Color.White) },
                                    onClick = {
                                        selectedItemId = inv.itemId
                                        quantityText = (inv.quantity.coerceAtMost(5)).toString()
                                        val newSpot = marketPrices.find { it.itemId == inv.itemId }?.price ?: prod?.basePrice ?: 100L
                                        startingBidText = (newSpot * 0.8f).toLong().toString()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantityText = it },
                        label = { CurrencyText(tr("Miktar (Max: $maxStock)", "Quantity (Max: $maxStock)"), color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonCyan, unfocusedBorderColor = ThemeBorder)
                    )

                    OutlinedTextField(
                        value = startingBidText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) startingBidText = it },
                        label = { CurrencyText("Açılış Teklifi (₳)".trAuto(), color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemeNeonCyan, unfocusedBorderColor = ThemeBorder)
                    )
                }
            }
        },
        confirmButton = {
            if (availableInventory.isNotEmpty()) {
                AppButton(
                    onClick = { if (qty > 0 && qty <= maxStock && startingBid > 0) onSubmit(selectedItemId, qty, startingBid) },
                    enabled = qty > 0 && qty <= maxStock && startingBid > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black)
                ) {
                    CurrencyText("Müzayedeyi Başlat".trAuto(), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { CurrencyText("İptal".trAuto(), color = Color.Gray) }
        }
    )
}

// ==========================================
// DIALOG: MARKET LISTING DETAILS & BUY DIALOG
// ==========================================
@Composable
fun MarketListingDetailsDialog(
    listing: MarketListing,
    product: Product?,
    playerMoney: Long,
    currentCity: String,
    viewModel: GameViewModel,
    isMyListing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmBuy: (buyQuantity: Int) -> Unit,
    onEditPrice: () -> Unit = {},
    onCancelListing: () -> Unit = {}
) {
    val baseId = com.example.data.ItemQuality.extractBaseProductId(listing.itemId)
    val itemName = product?.displayName ?: baseId
    val brandColor = if (product != null) Color(product.colorTint) else ThemeNeonCyan
    val offerQuality = listing.quality
    val qualityBorderColor = offerQuality.badgeColor

    val originCity = cities.find { it.id == listing.originCityId }
    val originCityName = originCity?.name ?: listing.originCityId.replaceFirstChar { it.uppercase() }
    val destCity = cities.find { it.id == currentCity }
    val destCityName = destCity?.name ?: currentCity.replaceFirstChar { it.uppercase() }

    val rawSpot = viewModel.marketPrices.value.find { it.itemId == baseId }?.price ?: product?.basePrice ?: 0L
    val spotPrice = (rawSpot * offerQuality.priceMultiplier).toLong()

    var buyQuantityText by remember { mutableStateOf(listing.quantity.toString()) }
    val buyQty = (buyQuantityText.toIntOrNull() ?: listing.quantity).coerceIn(1, listing.quantity)
    val productCost = try { Math.multiplyExact(listing.pricePerUnit, buyQty.toLong()) } catch (e: Exception) { Long.MAX_VALUE }
    val logisticsCost = viewModel.calculateLogisticsCost(listing.originCityId, currentCity, buyQty)
    val totalCost = try { Math.addExact(productCost, logisticsCost) } catch (e: Exception) { Long.MAX_VALUE }

    val canAfford = playerMoney >= totalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.6f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (product != null) UniversalProductIcon(product, 20.dp, brandColor) else Icon(Icons.Rounded.ShoppingCart, null, tint = brandColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        CurrencyText(
                            text = itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        CurrencyText(
                            text = if (isMyListing) tr("Sizin Satış İlanınız", "Your Market Listing") else tr("Satıcı: ${listing.sellerName}", "Seller: ${listing.sellerName}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMyListing) ThemeGold else Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = qualityBorderColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, qualityBorderColor)
                ) {
                    CurrencyText(
                        text = offerQuality.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = qualityBorderColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. QUALITY SPECIFICATION & FACILITY CRAFTING IMPACT
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF161F33),
                    border = BorderStroke(1.dp, qualityBorderColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = qualityBorderColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(
                                    text = tr("Kalite Derecesi:", "Quality Rating:"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            CurrencyText(
                                text = "${offerQuality.stars} / 5 Yıldız (x${offerQuality.priceMultiplier} Fiyat Çarpanı)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = qualityBorderColor
                            )
                        }

                        CurrencyText(
                            text = tr(
                                "🏭 Tesis Kalite Etkisi: Bu hammadde tesislerinizde işlendiğinde, çıkan nihai ürünün kalitesi tesisinizin seviyesi (1-5★) ile bu malzemenin kalitesinin ortalaması olarak hesaplanır. (Örnek: 9. Seviye Tesis [5★] + 1★ Hammadde = 3★ Ürün). Kaliteli ürünler borsada ve pazarda çok daha yüksek fiyata satılır.",
                                "🏭 Facility Quality Impact: When used in facility crafting, the produced item's quality equals the average between your facility level (1-5★) and this material's quality (1-5★). (e.g. Level 9 Facility [5★] + 1★ Material = 3★ Product). High-quality products yield much higher revenue on the market and bourse."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.9f),
                            lineHeight = 15.sp
                        )
                    }
                }

                // 2. PRICE & LOGISTICS BREAKDOWN CARD
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF141C2B),
                    border = BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText("İlan Birim Fiyatı:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText("${formatMoney(listing.pricePerUnit)} / Ton", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (spotPrice > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CurrencyText("Spot / Borsa Referans Fiyatı:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                CurrencyText("${formatMoney(spotPrice)} / Ton", style = MaterialTheme.typography.bodySmall, color = ThemeNeonCyan)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText("Mevcut İlan Stoğu:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText("${listing.quantity} Ton", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ThemeGold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText("Menşei Şehir:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(originCityName, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText("Teslim Şehri (Deponuz):".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(destCityName, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        
                        val remMs = listing.remainingMs
                        val remDays = remMs / (24 * 3600 * 1000L)
                        val remHours = (remMs % (24 * 3600 * 1000L)) / (3600 * 1000L)
                        val remMinutes = (remMs % (3600 * 1000L)) / (60 * 1000L)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText("Maksimum Yayın Süresi:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText("⏳ 3 Gün (Kalan: ${remDays}g ${remHours}sa ${remMinutes}dk)", style = MaterialTheme.typography.bodySmall, color = ThemeGold, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }

                if (!isMyListing) {
                    // 3. PURCHASE QUANTITY SELECTOR (SLIDER & PRESETS)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CurrencyText(
                            text = tr("Satın Alınacak Miktar:", "Quantity to Purchase:"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan
                        )

                        val currentQtyVal = (buyQuantityText.toIntOrNull() ?: 1).coerceIn(1, listing.quantity)
                        if (listing.quantity > 1) {
                            Slider(
                                value = currentQtyVal.toFloat(),
                                onValueChange = { buyQuantityText = it.toInt().toString() },
                                valueRange = 1f..listing.quantity.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = ThemeNeonCyan,
                                    activeTrackColor = ThemeNeonCyan,
                                    inactiveTrackColor = Color(0xFF1E2838)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "%25" to 0.25f,
                                "%50" to 0.50f,
                                "%75" to 0.75f,
                                tr("Maks (%100)", "Max (100%)") to 1.0f
                            ).forEach { (label, ratio) ->
                                val qtyPreset = (listing.quantity * ratio).toInt().coerceAtLeast(1)
                                val isSelected = currentQtyVal == qtyPreset

                                Surface(
                                    onClick = { buyQuantityText = qtyPreset.toString() },
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1A2130),
                                    border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else ThemeBorder),
                                    modifier = Modifier.weight(if (ratio == 1.0f) 1.3f else 1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CurrencyText(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = buyQuantityText,
                            onValueChange = { buyQuantityText = it },
                            label = { CurrencyText(tr("Miktar (1 - ${listing.quantity} Ton)", "Quantity (1 - ${listing.quantity} Tons)")) },
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
                    }

                    // 4. TOTAL COST BREAKDOWN PANEL
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141C2B),
                        border = BorderStroke(1.dp, if (canAfford) ThemeGold.copy(alpha = 0.5f) else ThemeNegative)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CurrencyText(tr("Ürün Tutarı ($buyQty Ton):", "Product Amount ($buyQty Tons):"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                CurrencyText(formatMoney(productCost), style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CurrencyText(tr("Lojistik & Taşıma Bedeli:", "Logistics & Freight:"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                CurrencyText(
                                    text = if (logisticsCost == 0L) tr("Ücretsiz (Aynı Şehir)", "Free (Same City)") else "+${formatMoney(logisticsCost)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (logisticsCost == 0L) ThemePositive else ThemeNegative
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = ThemeBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(text = "TOPLAM ÖDEME:".trAuto(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                CurrencyText(text = formatMoney(totalCost), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily, color = ThemeGold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CurrencyText("Şirket Kasası:".trAuto(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                CurrencyText(
                                    text = formatMoney(playerMoney),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) ThemePositive else ThemeNegative
                                )
                            }
                        }
                    }
                } else {
                    // OWNER MANAGEMENT BANNER
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = ThemeGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText("Bu İlan Size Aittir".trAuto(), fontWeight = FontWeight.Bold, color = ThemeGold)
                            }
                            CurrencyText(
                                text = "İlanınız şu anda tüm Türkiye ve dünya pazarında aktiftir. Dilediğiniz zaman satış fiyatını güncelleyebilir veya ilanı iptal ederek ürünlerinizi anında deponuza geri alabilirsiniz.".trAuto(),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isMyListing) {
                AppButton(
                    onClick = { onConfirmBuy(buyQty) },
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeNeonCyan,
                        contentColor = Color(0xFF002026)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText("Satın Alımı Onayla".trAuto(), fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEditPrice,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, ThemeNeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeNeonCyan)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText("Fiyatı Düzenle".trAuto(), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCancelListing,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNegative, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText("İlanı Kaldır".trAuto(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("Kapat".trAuto(), color = Color.Gray)
            }
        }
    )
}

// Backward-compatible wrapper for existing BuyListingDialog callers
@Composable
fun BuyListingDialog(
    listing: MarketListing,
    product: Product?,
    playerMoney: Long,
    currentCity: String,
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onConfirmBuy: (buyQuantity: Int) -> Unit
) {
    MarketListingDetailsDialog(
        listing = listing,
        product = product,
        playerMoney = playerMoney,
        currentCity = currentCity,
        viewModel = viewModel,
        isMyListing = false,
        onDismiss = onDismiss,
        onConfirmBuy = onConfirmBuy
    )
}

@Composable
fun BuyOrderCard(order: com.example.data.BuyOrder, playerCity: String, onSell: () -> Unit) {
    val p = com.example.data.Product.values().find { it.id == order.itemId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
        border = BorderStroke(1.dp, ThemeBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                CurrencyText(p?.name ?: order.itemId, fontWeight = FontWeight.Bold, color = Color.White)
                CurrencyText(formatMoney(order.pricePerUnit), color = ThemeGold, fontWeight = FontWeight.Bold)
            }
            CurrencyText(tr("Alıcı: ${order.buyerName} - ${order.destinationCityId.uppercase()}", "Buyer: ${order.buyerName} - ${order.destinationCityId.uppercase()}"), color = Color.Gray, fontSize = 12.sp)
            CurrencyText(tr("Talep: ${order.quantity} Adet", "Demand: ${order.quantity} Units"), color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AppButton(onClick = onSell, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ThemePositive)) {
                CurrencyText("Sözleşmeye Satış Yap".trAuto())
            }
        }
    }
}

@Composable
fun AuctionCard(auction: com.example.data.Auction, myName: String, onBid: (Long) -> Unit) {
    val p = com.example.data.Product.values().find { it.id == auction.itemId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                CurrencyText(p?.name ?: auction.itemId, fontWeight = FontWeight.Bold, color = Color.White)
                CurrencyText(if (auction.currentBid > 0) formatMoney(auction.currentBid) else formatMoney(auction.startingBid), color = ThemeGold, fontWeight = FontWeight.Bold)
            }
            CurrencyText(tr("Satıcı: ${auction.sellerName}", "Seller: ${auction.sellerName}"), color = Color.Gray, fontSize = 12.sp)
            CurrencyText(tr("Miktar: ${auction.quantity} Adet", "Quantity: ${auction.quantity} Units"), color = Color.LightGray, fontSize = 12.sp)
            if (auction.currentBidderName.isNotEmpty()) {
                CurrencyText(tr("En Yüksek Teklif: ${auction.currentBidderName}", "Highest Bid: ${auction.currentBidderName}"), color = ThemeNeonCyan, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            var bidAmount by remember { mutableStateOf((if (auction.currentBid > 0) auction.currentBid else auction.startingBid).toString()) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bidAmount,
                    onValueChange = { bidAmount = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                AppButton(onClick = { onBid(bidAmount.toLongOrNull() ?: 0L) }, colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan)) {
                    CurrencyText("Teklif Ver".trAuto(), color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun MarketAnalyticsSection(priceHistory: Map<String, List<Long>>, marketPrices: List<com.example.data.MarketPriceEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CurrencyText("Pazar Trendleri (Son 7 Gün)".trAuto(), color = Color.White, fontWeight = FontWeight.Bold)
        marketPrices.take(5).forEach { mp ->
            val p = com.example.data.Product.values().find { it.id == mp.itemId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
                border = BorderStroke(1.dp, ThemeBorder)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        CurrencyText(p?.name ?: mp.itemId, color = Color.White, fontWeight = FontWeight.Bold)
                        CurrencyText(tr("Güncel: ${formatMoney(mp.price)}", "Current: ${formatMoney(mp.price)}"), color = ThemeGold, fontSize = 12.sp)
                    }
                    // Simple trend indicator
                    val history = priceHistory["${mp.itemId}_${mp.originCountry}"] ?: priceHistory[mp.itemId] ?: emptyList()
                    if (history.size >= 2) {
                        val first = history.first()
                        val last = history.last()
                        if (last > first) {
                            Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = ThemePositive)
                        } else if (last < first) {
                            Icon(Icons.Rounded.TrendingDown, contentDescription = null, tint = ThemeNegative)
                        } else {
                            Icon(Icons.Rounded.TrendingFlat, contentDescription = null, tint = Color.Gray)
                        }
                    } else {
                        CurrencyText("Veri Yetersiz".trAuto(), color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFuturesContractDialog(
    inventory: List<com.example.data.InventoryEntity>,
    marketPrices: List<com.example.data.MarketPriceEntity>,
    onDismiss: () -> Unit,
    onSubmit: (itemId: String, quantity: Int, lockedPricePerUnit: Long, durationDays: Int) -> Unit
) {
    var selectedItemId by remember { mutableStateOf("wheat") }
    var quantityText by remember { mutableStateOf("50") }
    val spotPrice = remember(selectedItemId, marketPrices) {
        val prod = Product.values().find { it.id == selectedItemId }
        marketPrices.find { it.itemId == selectedItemId }?.price ?: prod?.basePrice ?: 100L
    }
    var priceText by remember { mutableStateOf((spotPrice * 1.05f).toLong().toString()) }
    var durationDays by remember { mutableIntStateOf(30) }

    val qty = quantityText.toIntOrNull() ?: 1
    val price = priceText.toLongOrNull() ?: spotPrice
    val totalCost = try { Math.multiplyExact(qty.toLong(), price) } catch(e: Exception) { Long.MAX_VALUE }
    val commission = (totalCost * 0.015).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121824),
        titleContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyText("Yeni Vadeli B2B Sözleşme".trAuto(), fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CurrencyText("Vadeli sözleşmeler ile fiyat dalgalanmalarına karşı sabit fiyat garantisi sağlar ve uzun vadeli tedarik zinciri planlarsınız.".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                CurrencyText("Ürün Seçin:".trAuto(), style = MaterialTheme.typography.labelMedium, color = ThemeNeonCyan, fontWeight = FontWeight.Bold)
                var expanded by remember { mutableStateOf(false) }
                val currentProduct = Product.values().find { it.id == selectedItemId }

                Box {
                    Surface(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1A2130),
                        border = BorderStroke(1.dp, ThemeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(currentProduct?.displayName ?: selectedItemId, color = Color.White, fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1A2130))) {
                        Product.values().forEach { prod ->
                            DropdownMenuItem(
                                text = { CurrencyText(prod.displayName, color = Color.White) },
                                onClick = {
                                    selectedItemId = prod.id
                                    val p = marketPrices.find { it.itemId == prod.id }?.price ?: prod.basePrice
                                    priceText = p.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { CurrencyText("Sözleşme Miktarı (Ton)".trAuto()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { CurrencyText("Sabit Birim Fiyat (₳)".trAuto()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30).forEach { days ->
                        val isSelected = durationDays == days
                        Surface(
                            onClick = { durationDays = days },
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) ThemeGold.copy(alpha = 0.25f) else Color(0xFF1A2130),
                            border = BorderStroke(1.dp, if (isSelected) ThemeGold else ThemeBorder),
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                CurrencyText(tr("$days Günlük Vade", "$days Days Term"), color = if (isSelected) ThemeGold else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), color = Color(0xFF161F33)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText("Sözleşme Toplamı:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(formatCurrency(totalCost, isEnglishLanguage()), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ThemeGold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("Borsa Komisyonu (%1.5):", "Exchange Commission (1.5%):"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(formatCurrency(commission, isEnglishLanguage()), style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onSubmit(selectedItemId, qty, price, durationDays) },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF1A1300))
            ) {
                CurrencyText("Sözleşmeyi İmzala".trAuto(), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("İptal".trAuto(), color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingDialog(
    uiState: com.example.viewmodel.GameUiState,
    listing: MarketListing,
    product: Product?,
    viewModel: com.example.viewmodel.GameViewModel,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(listing.pricePerUnit.toString()) }
    val currentPrice = priceText.toLongOrNull() ?: listing.pricePerUnit
    val haptic = LocalHapticFeedback.current
    
    val spotPrice = uiState.marketState.prices.find { it.itemId == listing.itemId }?.price ?: product?.basePrice ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeSurface,
        title = {
            CurrencyText(
                text = "İlan Fiyatını Güncelle".trAuto(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = RobotoMonoFontFamily
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CurrencyText(
                    text = tr("${listing.quantity} Ton ${product?.displayName ?: listing.itemId}", "${listing.quantity} Tons ${product?.displayName ?: listing.itemId}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { CurrencyText(tr("Yeni Birim Fiyat (₳/Ton)", "New Unit Price (₳/Ton)"), color = Color.Gray) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                )
                
                val diffPercent = if (spotPrice > 0) ((currentPrice - spotPrice).toFloat() / spotPrice.toFloat() * 100).toInt() else 0
                val color = if (diffPercent > 0) ThemeNegative else if (diffPercent < 0) ThemePositive else Color.Gray
                val prefix = if (diffPercent > 0) "+" else ""
                
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(2.dp), color = Color(0xFF161F33)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        CurrencyText(tr("Piyasa Ortalaması: ${formatCredit(spotPrice)}", "Market Average: ${formatCredit(spotPrice)}"), color = Color.Gray, fontSize = 12.sp)
                        CurrencyText(tr("Sizin Fiyatınız piyasanın %$prefix$diffPercent ${if(diffPercent > 0) "üstünde" else if(diffPercent < 0) "altında" else "tam seviyesinde"}.", "Your price is $prefix$diffPercent% ${if(diffPercent > 0) "above" else if(diffPercent < 0) "below" else "at par with"} the market."), color = color, fontSize = 11.sp)
                        if (diffPercent > 10) {
                            CurrencyText(tr("Satılma ihtimali düşük.", "Low probability of being sold."), color = ThemeNegative, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val invalidPriceMsg = "Geçerli bir fiyat girin.".trAuto()
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newPrice = priceText.toLongOrNull()
                    if (newPrice != null && newPrice > 0) {
                        viewModel.handleIntent(com.example.viewmodel.GameIntent.UpdateListingPrice(listing.id, newPrice))
                        onDismiss()
                    } else {
                        com.example.ui.components.SmartNotificationManager.show(invalidPriceMsg, com.example.ui.components.NotificationType.INFO)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan),
                shape = RoundedCornerShape(2.dp)
            ) {
                CurrencyText("Güncelle".trAuto(), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("İptal".trAuto(), color = Color.Gray)
            }
        }
    )
}
