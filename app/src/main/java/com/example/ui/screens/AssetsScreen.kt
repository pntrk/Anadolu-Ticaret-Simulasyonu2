package com.example.ui.screens

import com.example.ui.components.CurrencyText
import com.example.data.*
import com.example.ui.components.UniversalProductIcon
import com.example.ui.components.formatCredit


import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.theme.RobotoMonoFontFamily

import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.TechTree
import com.example.ui.components.RdCenterSection
import com.example.ui.components.SupplyChainAnalyzerDialog
import com.example.ui.components.AntiqueMuseumSection
import com.example.ui.components.AntiqueMuseumDialog
import com.example.ui.components.OsbIsometricZoneView
import com.example.ui.components.SynergyCraftingPanel
import com.example.ui.components.QualityRatePreviewBar
import com.example.ui.components.FacilityQualityPotentialBar
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BusinessEntity
import com.example.data.InventoryEntity
import com.example.data.Product
import com.example.data.ProductTier
import com.example.data.cities
import com.example.ui.components.AppButton
import com.example.ui.components.MeshBackground
import com.example.ui.components.NotificationType
import com.example.ui.components.ParticleManager
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.bounceClick
import com.example.ui.components.formatMoney
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import kotlin.math.pow

// Data class for recipe status UI rendering
data class RecipeRequirementStatus(
    val productId: String,
    val productName: String,
    val requiredAmount: Int,
    val stockAmount: Int,
    val isSufficient: Boolean
)

// View mode enum for Facilities Screen
enum class FacilityViewMode {
    LIST, // 🏭 Tesislerim (Liste)
    FLOW  // 🔄 Tedarik Zinciri (Akış)
}

@Composable
fun AssetsScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    initialProductId: String? = null,
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToConsortium: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AssetsContent(uiState, onIntent, viewModel, initialProductId, onNavigateToRd, onNavigateToConsortium)
    }
}

@Composable
fun AssetsContent(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    initialProductId: String? = null,
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToConsortium: () -> Unit = {}
) {
    val player = uiState.playerState.player
    val inventory = uiState.inventoryState.items
    val businesses = uiState.businesses
    val isEnglish = com.example.ui.theme.isEnglishLanguage()

    val haptic = LocalHapticFeedback.current

    var selectedViewMode by remember { mutableStateOf(FacilityViewMode.LIST) }
    var showCityDialogFor by remember { mutableStateOf<Product?>(null) }
    var showProduceDialogFor by remember { mutableStateOf<Product?>(null) }
    var showSellDialogForBusiness by remember { mutableStateOf<BusinessEntity?>(null) }
    var showNewFacilityDialog by remember { mutableStateOf(false) }
    var showSupplyChainAnalyzer by remember { mutableStateOf(false) }
    var selectedTierFilter by remember { mutableStateOf<ProductTier?>(null) }
    var selectedCityFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(initialProductId) {
        if (initialProductId != null) {
            val p = Product.values().find { it.id == initialProductId }
            if (p != null) {
                showProduceDialogFor = p
            }
        }
    }

    if (player == null || uiState.gameStateObj == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ThemeNeonCyan)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Kısım: Görünüm Değiştirici Segmented Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1726))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF162032),
                border = BorderStroke(1.dp, Color(0xFF26354D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Tab 1: [ 🏭 Tesislerim (Liste) ]
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedViewMode = FacilityViewMode.LIST
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedViewMode == FacilityViewMode.LIST) Color(0xFF2563EB) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("🏭 Tesislerim (Liste)", "🏭 Facilities (List)", isEnglish),
                                fontWeight = if (selectedViewMode == FacilityViewMode.LIST) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (selectedViewMode == FacilityViewMode.LIST) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Tab 2: [ 🔄 Tedarik Zinciri (Akış) ]
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedViewMode = FacilityViewMode.FLOW
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedViewMode == FacilityViewMode.FLOW) Color(0xFF0D9488) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("🔄 Tedarik Zinciri (Akış)", "🔄 Supply Chain (Flow)", isEnglish),
                                fontWeight = if (selectedViewMode == FacilityViewMode.FLOW) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                color = if (selectedViewMode == FacilityViewMode.FLOW) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            MeshBackground(gameState = uiState.gameStateObj, isCelebrating = false)

            if (selectedViewMode == FacilityViewMode.LIST) {
                OsbIsometricZoneView(
                    uiState = uiState,
                    viewModel = viewModel,
                    onIntent = onIntent,
                    selectedCityFilter = selectedCityFilter,
                    onCityFilterChanged = { selectedCityFilter = it },
                    selectedTierFilter = selectedTierFilter,
                    onTierFilterChanged = { selectedTierFilter = it },
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    onOpenBuildNewFacility = { cityId ->
                        if (cityId != null) selectedCityFilter = cityId
                        showNewFacilityDialog = true
                    },
                    onOpenProduceDialog = { prod ->
                        showProduceDialogFor = prod
                    },
                    onOpenSellDialog = { biz ->
                        showSellDialogForBusiness = biz
                    },
                    onNavigateToRd = onNavigateToRd
                )
            } else {
                SupplyChainFlowScreen(
                    viewModel = viewModel,
                    initialProductId = initialProductId ?: "ev",
                    onNavigateBack = {
                        selectedViewMode = FacilityViewMode.LIST
                    },
                    onNavigateToBuild = { targetProductId ->
                        val prod = Product.values().find {
                            it.id.equals(targetProductId, ignoreCase = true) ||
                            it.facilityId.equals(targetProductId, ignoreCase = true)
                        }
                        if (prod != null) {
                            if (prod.tier == ProductTier.TIER_4) {
                                SmartNotificationManager.show(
                                    "🏛️ Tier 4 Mega Tesisleri doğrudan kurulamaz! Konsorsiyum projesi kurarak veya katılarak inşa edebilirsiniz.",
                                    "🏛️ Tier 4 Mega Facilities cannot be built directly! Must be established via Consortium.",
                                    NotificationType.ALERT
                                )
                                onNavigateToConsortium()
                            } else {
                                showCityDialogFor = prod
                            }
                        } else {
                            showNewFacilityDialog = true
                        }
                    },
                    onNavigateToWarehouse = { _ ->
                        selectedViewMode = FacilityViewMode.LIST
                    },
                    onNavigateToConsortium = onNavigateToConsortium
                )
            }

        // DIALOG: ŞEHİR SEÇİMİ VE TESİS İNŞASI
        if (showCityDialogFor != null) {
            val prod = showCityDialogFor!!
            CitySelectionBuildDialog(
                product = prod,
                playerMoney = player.money,
                playerDollarBalance = player.dollarBalance,
                onDismiss = { showCityDialogFor = null },
                onNavigateToConsortium = {
                    showCityDialogFor = null
                    onNavigateToConsortium()
                },
                onSelectCity = { cityId, cost ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.handleIntent(com.example.viewmodel.GameIntent.BuildBusiness(prod.facilityId, cityId, cost))
                    SmartNotificationManager.show("${prod.getFacilityName(isEnglish)} " + tr("tesisiniz inşa edildi!", "facility constructed!", isEnglish), NotificationType.SUCCESS)
                    ParticleManager.spawnCelebration()
                    showCityDialogFor = null
                }
            )
        }

        // DIALOG: ÜRETİM KONTROL PANELİ
        if (showProduceDialogFor != null) {
            val prod = showProduceDialogFor!!
            val productBusinesses = businesses.filter { it.type == prod.facilityId }
            val producingCityId = productBusinesses.firstOrNull()?.cityId ?: player.currentCity
            ProduceControlDialog(
                product = prod,
                viewModel = viewModel,
                dynamicProductionCost = viewModel.getDynamicProductionCost(prod, producingCityId),
                playerMoney = player.money,
                playerDollarBalance = player.dollarBalance,
                playerCurrentCity = player.currentCity ?: "istanbul",
                inventory = inventory,
                productBusinesses = productBusinesses,
                onDismiss = { showProduceDialogFor = null },
                onConfirm = { qty, autoProcure ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.handleIntent(com.example.viewmodel.GameIntent.Produce(prod.id, qty, autoProcure))
                    SmartNotificationManager.show("${qty} Adet ${prod.getDisplayName()} üretimi başlatıldı!", NotificationType.SUCCESS)
                    showProduceDialogFor = null
                }
            )
        }

        // DIALOG: TESİS TASFİYE / SATIŞ ONAYI
        if (showSellDialogForBusiness != null) {
            val biz = showSellDialogForBusiness!!
            SellFacilityConfirmDialog(
                business = biz,
                viewModel = viewModel,
                onDismiss = { showSellDialogForBusiness = null },
                onConfirmSell = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.handleIntent(com.example.viewmodel.GameIntent.SellBusiness(biz))
                    showSellDialogForBusiness = null
                }
            )
        }

        if (showNewFacilityDialog) {
            NewFacilitySelectionDialog(
                viewModel = viewModel,
                businesses = businesses,
                playerMoney = player.money,
                onDismiss = { showNewFacilityDialog = false },
                onSelectFacility = { product ->
                    showNewFacilityDialog = false
                    if (product.tier == ProductTier.TIER_4) {
                        SmartNotificationManager.show(
                            "🏛️ Tier 4 Mega Tesisleri doğrudan kurulamaz! Konsorsiyum projesi kurarak veya katılarak inşa edebilirsiniz.",
                            "🏛️ Tier 4 Mega Facilities cannot be built directly! Must be established via Consortium.",
                            NotificationType.ALERT
                        )
                        onNavigateToConsortium()
                    } else {
                        showCityDialogFor = product
                    }
                },
                onNavigateToRd = { techId ->
                    showNewFacilityDialog = false
                    onNavigateToRd(techId)
                }
            )
        }

        if (showSupplyChainAnalyzer) {
            SupplyChainAnalyzerDialog(
                viewModel = viewModel,
                onDismiss = { showSupplyChainAnalyzer = false },
                onSelectBuildFacility = { product ->
                    showSupplyChainAnalyzer = false
                    if (product.tier == ProductTier.TIER_4) {
                        SmartNotificationManager.show(
                            "🏛️ Tier 4 Mega Tesisleri doğrudan kurulamaz! Konsorsiyum projesi kurarak veya katılarak inşa edebilirsiniz.",
                            "🏛️ Tier 4 Mega Facilities cannot be built directly! Must be established via Consortium.",
                            NotificationType.ALERT
                        )
                        onNavigateToConsortium()
                    } else {
                        showCityDialogFor = product
                    }
                },
                onSelectProduce = { product ->
                    showSupplyChainAnalyzer = false
                    showProduceDialogFor = product
                },
                onNavigateToRd = { techId ->
                    showSupplyChainAnalyzer = false
                    onNavigateToRd(techId)
                },
                onNavigateToConsortium = {
                    showSupplyChainAnalyzer = false
                    onNavigateToConsortium()
                }
            )
        }
    }
}
}

// ==========================================
// COMPONENT: CITY SELECTION BUILD DIALOG
// ==========================================
@Composable
fun CitySelectionBuildDialog(
    product: Product,
    playerMoney: Long,
    playerDollarBalance: Long,
    onDismiss: () -> Unit,
    onNavigateToConsortium: () -> Unit = {},
    onSelectCity: (cityId: String, cost: Long) -> Unit
) {
    val isEnglish = isEnglishLanguage()
    val brandColor = Color(product.colorTint)
    val eligibleCities = remember(product) {
        val list = cities.filter { it.canBuildProduct(product) }
        if (list.isNotEmpty()) list else cities
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = brandColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        UniversalProductIcon(product = product, size = 20.dp, tint = brandColor)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    CurrencyText(
                        text = "${product.getFacilityName(isEnglish)} " + "Kurulumu".trAuto(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    CurrencyText(
                        text = "Yatırım Yapılacak Şehri Seçin".trAuto() + " (${eligibleCities.size} " + "Uygun Şehir".trAuto() + ")",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeNeonCyan
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CurrencyText(
                    text = "Bu tesis yalnızca jeolojik ve sanayi altyapısı uygun şehirlerde kurulabilir. Her şehrin maliyet çarpanı altyapısına göre değişir.".trAuto(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(eligibleCities, key = { it.id }) { city ->
                        val rawCost = (product.facilityCost * city.economicMultiplier).toLong()
                        val finalCost = rawCost
                        val canAfford = playerMoney >= finalCost

                        Surface(
                            onClick = {
                                if (canAfford) {
                                    onSelectCity(city.id, finalCost)
                                } else {
                                    val formattedCost = com.example.ui.components.formatCredit(finalCost)
                                    SmartNotificationManager.show("Yetersiz Bakiye! Şehir için $formattedCost gerekli.", NotificationType.ALERT)
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF182030),
                            border = BorderStroke(1.dp, if (canAfford) ThemeNeonCyan.copy(alpha = 0.3f) else ThemeNegative.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText(
                                            text = city.getDisplayName(isEnglish),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF222C3E)
                                        ) {
                                            CurrencyText(
                                                text = city.region.trAuto(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (city.isGlobal) Color(0xFF1A365D) else Color(0xFF2D3748)
                                        ) {
                                            CurrencyText(
                                                text = "${city.countryFlag} ${city.getCountryDisplayName(isEnglish)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = if (city.isGlobal) Color(0xFF63B3ED) else Color(0xFFCBD5E0),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(
                                        text = "${tr("Bölgesel Çarpan:", "Regional Multiplier:", isEnglish)} x${city.economicMultiplier}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = ThemeGold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    CurrencyText(
                                        text = com.example.ui.components.formatCredit(finalCost),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = if (canAfford) ThemeGold else ThemeNegative
                                    )
                                    CurrencyText(
                                        text = if (canAfford) tr("İnşa Et →", "Build →", isEnglish) else tr("Yetersiz", "Insufficient", isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canAfford) ThemeNeonCyan else ThemeNegative
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText("İptal".trAuto(), color = Color.Gray)
            }
        }
    )
}

// ==========================================
// COMPONENT: PRODUCE CONTROL DIALOG
// ==========================================
@Composable
fun ProduceControlDialog(
    product: Product,
    viewModel: GameViewModel,
    dynamicProductionCost: Long,
    playerMoney: Long,
    playerDollarBalance: Long,
    playerCurrentCity: String,
    inventory: List<InventoryEntity>,
    productBusinesses: List<BusinessEntity>,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, autoProcure: Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val brandColor = Color(product.colorTint)

    val producingCityId = productBusinesses.firstOrNull()?.cityId ?: playerCurrentCity
    val producingCity = com.example.data.cities.find { it.id == producingCityId }
    val isUsd = false
    val activeBalance = if (isUsd) playerDollarBalance else playerMoney

    val maxByMoney = if (dynamicProductionCost > 0) (activeBalance / dynamicProductionCost) else Int.MAX_VALUE.toLong()

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("game_settings", android.content.Context.MODE_PRIVATE) }
    var autoProcureEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_procure", false)) }

    val maxByIngredients = if (product.recipe.isEmpty()) {
        Int.MAX_VALUE
    } else {
        product.recipe.minOf { req ->
            val stock = inventory.find { it.itemId == req.productId }?.quantity ?: 0
            stock / req.amountPerUnit
        }
    }

    val ownedFacilityCount = productBusinesses.size
    val facilityStorageRemaining = if (productBusinesses.isNotEmpty()) {
        productBusinesses.sumOf { it.getRemainingStorageCapacity() }
    } else {
        val totalCentral = inventory.sumOf { it.quantity }
        ((viewModel.player.value?.inventoryCapacity ?: 1000) - totalCentral).coerceAtLeast(0)
    }
    val isStorageFull = facilityStorageRemaining <= 0

    val maxProducingQuantity = minOf(
        maxByMoney.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        if (autoProcureEnabled) Int.MAX_VALUE else (if (product.recipe.isEmpty()) Int.MAX_VALUE else maxByIngredients),
        facilityStorageRemaining
    ).coerceAtLeast(0)

    var inputQuantityText by remember { mutableStateOf(if (maxProducingQuantity > 0) "1" else "0") }
    val currentQty = inputQuantityText.toIntOrNull() ?: 0
    val totalCost = try { Math.multiplyExact(dynamicProductionCost, currentQty.toLong()) } catch(e: Exception) { Long.MAX_VALUE }

    val canProduce = !isStorageFull && currentQty > 0 && currentQty <= maxProducingQuantity && activeBalance >= totalCost
    val invalidQtyMsg = if (isStorageFull) {
        "Depo dolu olduğu için üretim yapılamaz! Lütfen deponun boşalmasını bekleyin.".trAuto()
    } else {
        "Lütfen geçerli miktar veya yeterli hammadde sağlayın!".trAuto()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = brandColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        UniversalProductIcon(product = product, size = 20.dp, tint = brandColor)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    CurrencyText(
                        text = "${product.getDisplayName().trAuto()} " + "Üretimi".trAuto(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    CurrencyText(
                        text = product.getFacilityName().trAuto(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeNeonCyan
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
                // Capacity Summary Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF182030),
                    border = BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val facilityCityList = productBusinesses.mapNotNull { biz -> com.example.data.cities.find { it.id == biz.cityId } }.distinctBy { it.id }
                        val facilityCityNames = if (facilityCityList.isNotEmpty()) {
                            facilityCityList.joinToString(", ") { "${it.countryFlag} ${it.name}" }
                        } else {
                            producingCity?.let { "${it.countryFlag} ${it.name}" } ?: producingCityId
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Tesis Lokasyonu / Şehir:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(facilityCityNames, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ThemeNeonCyan, fontFamily = RobotoMonoFontFamily)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Birim Üretim Maliyeti (Borsa Endeksli):".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(com.example.ui.components.formatCurrencyByUsd(dynamicProductionCost, isUsd), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                        }

                        val estMs = viewModel.calculateProductionDuration(product.id, currentQty.coerceAtLeast(1))
                        val estSec = estMs / 1000L
                        val estM = estSec / 60
                        val estS = estSec % 60
                        val timeFormatted = if (estM > 0) "${estM} dk ${estS} sn" else "${estS} sn"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Tahmini Üretim Süresi:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(timeFormatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF64FFDA), fontFamily = RobotoMonoFontFamily)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Maksimum Üretim Limiti:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText("$maxProducingQuantity " + "Adet".trAuto(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isStorageFull) ThemeNegative else ThemeNeonCyan, fontFamily = RobotoMonoFontFamily)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Kalan Depo Alanı:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(
                                if (isStorageFull) "0 Ton (DEPO DOLU)".trAuto() else "$facilityStorageRemaining Ton",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isStorageFull) ThemeNegative else ThemePositive,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        val avgWear = if (productBusinesses.isNotEmpty()) (productBusinesses.sumOf { it.wearLevel.toDouble() } / productBusinesses.size).toFloat() else 0f
                        if (avgWear > 0.05f) {
                            val slowPct = ((1.0f - 1.0f / (1.0f + avgWear * 1.5f)) * 100).toInt()
                            val wearPct = (avgWear * 100).toInt()
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(2.dp),
                                color = Color(0xFF382312),
                                border = BorderStroke(0.5.dp, ThemeGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Build, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CurrencyText(
                                        text = "Tesis Yıpranması:".trAuto() + " %$wearPct (" + "Üretim yavaşlıyor".trAuto() + " %$slowPct). " + "Varlıklar ekranından bakım yapabilirsiniz.".trAuto(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = ThemeGold
                                    )
                                }
                            }
                        }
                    }
                }

                // Storage Full Warning Card
                if (isStorageFull) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF5350).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF5350)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                            Column {
                                CurrencyText("⚠️ DEPO DOLU: Üretim Durduruldu".trAuto(), color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                CurrencyText("Tesis deposu tamamen dolduğu için üretim devam edemez. Üretime devam etmek için lütfen deponun boşalmasını (satış veya transfer) bekleyin.".trAuto(), color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Kalite Potansiyeli Barı (Quality Potential Bar)
                val activeFacilityLevel = productBusinesses.maxOfOrNull { it.level } ?: 1
                val activeFacilityWear = productBusinesses.firstOrNull()?.wearLevel ?: 0.0f
                FacilityQualityPotentialBar(
                    facilityLevel = activeFacilityLevel,
                    product = product,
                    wearLevel = activeFacilityWear
                )

                // Recipe Checklist for Selected Quantity
                if (product.recipe.isNotEmpty()) {
                    CurrencyText(
                        text = "🌐 Holografik Montaj & Tedarik Ağı",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val dynamicRecipeDetails = product.recipe.map { req ->
                        val reqProd = Product.values().find { it.id == req.productId }
                        val stock = inventory.filter { it.baseProductId == req.productId }.sumOf { it.quantity }
                        val requiredTotal = req.amountPerUnit * currentQty
                        RecipeRequirementStatus(
                            productId = req.productId,
                            productName = reqProd?.getDisplayName()?.trAuto() ?: req.productId,
                            requiredAmount = requiredTotal,
                            stockAmount = stock,
                            isSufficient = stock >= requiredTotal
                        )
                    }
                    
                    ProductHolographicMatrix(
                        product = product,
                        recipeDetails = dynamicRecipeDetails
                    )

                    // 🏭 Kalite Mirası & Sinerji Üretimi (Synergy Crafting Panel)
                    SynergyCraftingPanel(
                        product = product,
                        businesses = viewModel.businesses.value,
                        playerLevel = viewModel.player.value?.level ?: 1
                    )

                    val hasMissingIngredients = dynamicRecipeDetails.any { !it.isSufficient }
                    if (hasMissingIngredients) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.buyMissingIngredients(product.id, currentQty)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                CurrencyText(
                                    text = "📦 Eksikleri Pazardan Satın Al (Akıllı Tedarik)".trAuto(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CurrencyText(
                                text = "Eksik Hammaddeleri Borsadan Al",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            CurrencyText(
                                text = "Depoda olmayan ürünler anlık kurdan temin edilir",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = autoProcureEnabled,
                            onCheckedChange = { 
                                autoProcureEnabled = it
                                sharedPrefs.edit().putBoolean("auto_procure", it).apply()
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = ThemeNeonCyan,
                                checkedTrackColor = ThemeNeonCyan.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Quantity Input & Quick Slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = inputQuantityText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                val valInt = newValue.toIntOrNull() ?: 0
                                val clamped = valInt.coerceAtMost(maxProducingQuantity)
                                inputQuantityText = if (newValue.isEmpty()) "" else clamped.toString()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        label = { CurrencyText("Üretim Miktarı (Adet)".trAuto()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeNeonCyan,
                            unfocusedBorderColor = ThemeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (maxProducingQuantity > 1) {
                        Slider(
                            value = currentQty.coerceIn(1, maxProducingQuantity).toFloat(),
                            onValueChange = {
                                inputQuantityText = it.toInt().toString()
                            },
                            valueRange = 1f..maxProducingQuantity.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeNeonCyan,
                                activeTrackColor = ThemeNeonCyan,
                                inactiveTrackColor = Color(0xFF1E283A)
                            )
                        )

                        // Quick Presets
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.25f to tr("%25", "25%"), 0.5f to tr("%50", "50%"), 0.75f to tr("%75", "75%"), 1.0f to tr("MAX", "MAX")).forEach { (ratio, label) ->
                                val targetQty = (maxProducingQuantity * ratio).toInt().coerceAtLeast(1)
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        inputQuantityText = targetQty.toString()
                                    },
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFF1A2130),
                                    border = BorderStroke(1.dp, ThemeBorder),
                                    modifier = Modifier.weight(1f)
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
                                            color = ThemeNeonCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Total Cost Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF182030),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = "TOPLAM MALİYET:".trAuto(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.Gray
                        )
                        CurrencyText(
                            text = com.example.ui.components.formatCurrencyByUsd(totalCost, isUsd),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    if (canProduce) {
                        onConfirm(currentQty, autoProcureEnabled)
                    } else {
                        SmartNotificationManager.show(invalidQtyMsg, NotificationType.ALERT)
                    }
                },
                enabled = canProduce,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeNeonCyan,
                    contentColor = Color(0xFF002026)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                CurrencyText(
                    if (isStorageFull) "⚠️ DEPO DOLU (Boşalması Bekleniyor)".trAuto() else "Üretimi Başlat".trAuto(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily
                )
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
// COMPONENT: SELL FACILITY CONFIRM DIALOG
// ==========================================
@Composable
fun SellFacilityConfirmDialog(
    business: BusinessEntity,
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onConfirmSell: () -> Unit
) {
    val product = Product.values().find { it.facilityId == business.type }
    val city = cities.find { it.id == business.cityId }
    val (totalInvestment, refundAmount) = viewModel.calculateFacilityValuationAndRefund(business)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        shape = RoundedCornerShape(4.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = ThemeNegative,
                    modifier = Modifier.size(24.dp)
                )
                CurrencyText(
                    text = "TESİSİ TASFİYE ET".trAuto(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = RobotoMonoFontFamily,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CurrencyText(
                    text = tr(
                        "${city?.name ?: business.cityId} şehrindeki Seviye ${business.level} ${product?.getFacilityName(false) ?: "tesisi"} satmak istediğinize emin misiniz?",
                        "Are you sure you want to sell Level ${business.level} ${product?.getFacilityName(true) ?: "facility"} in ${city?.getDisplayName(true) ?: business.cityId}?"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF141C2B),
                    border = BorderStroke(1.dp, ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Toplam Tesis Yatırımı:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText(formatMoney(totalInvestment), style = MaterialTheme.typography.bodySmall, color = Color.White, fontFamily = RobotoMonoFontFamily)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText("Geri İade Oranı:".trAuto(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            CurrencyText("Nakit İade".trAuto() + " %70", style = MaterialTheme.typography.bodySmall, color = ThemeGold, fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = ThemeBorder, modifier = Modifier.padding(vertical = 2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText("Geri Alınacak Nakit:".trAuto(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ThemePositive)
                            CurrencyText("+${formatMoney(refundAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ThemePositive, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }

                CurrencyText(
                    text = "⚠️ Dikkat: Satış sonrasında tesis haritadan ve bilançonuzdan tamamen silinir.".trAuto(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeNegative,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmSell,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeNegative, contentColor = Color.White),
                shape = RoundedCornerShape(2.dp)
            ) {
                CurrencyText("TESİSİ SAT".trAuto() + " (+${formatMoney(refundAmount)})", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = RobotoMonoFontFamily)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                CurrencyText("VAZGEÇ".trAuto(), color = Color.LightGray, fontSize = 11.sp, fontFamily = RobotoMonoFontFamily)
            }
        }
    )
}

@Composable
fun ActiveDeliveriesSection(
    activeDeliveries: List<com.example.data.DeliveryItem>
) {
    if (activeDeliveries.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
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
                        text = "Yoldaki Lojistik Sevkiyatlar".trAuto() + " (${activeDeliveries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold
                    )
                }
            }

            activeDeliveries.forEach { delivery ->
                val prod = Product.values().find { it.id == delivery.itemId }
                val prodName = prod?.displayName ?: delivery.itemId.uppercase()
                val originCityName = com.example.data.cities.find { it.id == delivery.originCityId }?.name ?: delivery.originCityId
                val destCityName = com.example.data.cities.find { it.id == delivery.destinationCityId }?.name ?: delivery.destinationCityId

                val now = System.currentTimeMillis()
                val elapsed = now - delivery.startTimeMs
                val remainingMs = (delivery.totalDurationMs - elapsed).coerceAtLeast(0L)
                val remainingSeconds = (remainingMs / 1000L).toInt()
                val remMin = remainingSeconds / 60
                val remSec = remainingSeconds % 60
                val timeText = if (remMin > 0) "${remMin}" + "dk".trAuto() + " ${remSec}" + "sn kaldı".trAuto() else "${remSec}" + "sn kaldı".trAuto()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1B2436))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = "${delivery.quantity} " + tr("Ton", "Tons") + " $prodName ($originCityName ➔ $destCityName)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        CurrencyText(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }

                    LinearProgressIndicator(
                        progress = { delivery.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ThemeGold,
                        trackColor = Color(0xFF0F1522)
                    )
                }
            }
        }
    }
}










@Composable
fun KpiSummaryCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = androidx.compose.ui.graphics.Color(0xFF162032),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF26334D))
    ) {
        androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.padding(12.dp)) {
            androidx.compose.material3.Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = androidx.compose.ui.Modifier.size(16.dp))
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
            CurrencyText(label, color = androidx.compose.ui.graphics.Color.Gray, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            CurrencyText(value, color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

// ==========================================
// COMPONENT: NEW FACILITY SELECTION DIALOG
// ==========================================
@Composable
fun NewFacilitySelectionDialog(
    viewModel: GameViewModel,
    businesses: List<BusinessEntity>,
    playerMoney: Long,
    onDismiss: () -> Unit,
    onSelectFacility: (Product) -> Unit,
    onNavigateToRd: (String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEnglish = isEnglishLanguage()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf<ProductTier?>(null) }

    val allProducts = remember { Product.values().filter { it.tier != ProductTier.TIER_4 }.sortedBy { it.facilityCost } }

    val filteredList = remember(searchQuery, selectedTierFilter, isEnglish) {
        allProducts.filter { prod ->
            val matchesTier = selectedTierFilter == null || prod.tier == selectedTierFilter
            val matchesSearch = searchQuery.isEmpty() ||
                    prod.getFacilityName(isEnglish).contains(searchQuery, ignoreCase = true) ||
                    prod.getDisplayName(isEnglish).contains(searchQuery, ignoreCase = true)
            matchesTier && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThemeNeonCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AddBusiness,
                                contentDescription = null,
                                tint = ThemeNeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        CurrencyText(
                            text = "YENİ TESİS KUR".trAuto(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.White
                        )
                        CurrencyText(
                            text = "Sanayi kataloğundan tesis türünü seçin".trAuto(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { CurrencyText("Tesis / Ürün adı ara...".trAuto(), color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeNeonCyan,
                        unfocusedBorderColor = ThemeBorder,
                        focusedContainerColor = Color(0xFF0C101D),
                        unfocusedContainerColor = Color(0xFF0C101D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Tier Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedTierFilter == null,
                            onClick = { selectedTierFilter = null },
                            label = { CurrencyText("Tüm Kademeler".trAuto(), fontSize = 11.sp) },
                            shape = RoundedCornerShape(4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemeNeonCyan,
                                selectedLabelColor = Color(0xFF002026),
                                containerColor = Color(0xFF162032),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTierFilter == ProductTier.TIER_1,
                            onClick = { selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_1) null else ProductTier.TIER_1 },
                            label = { CurrencyText("🌾 Tier 1", fontSize = 11.sp) },
                            shape = RoundedCornerShape(4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemeGold,
                                selectedLabelColor = Color(0xFF1A1300),
                                containerColor = Color(0xFF162032),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTierFilter == ProductTier.TIER_2,
                            onClick = { selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_2) null else ProductTier.TIER_2 },
                            label = { CurrencyText("🏭 Tier 2", fontSize = 11.sp) },
                            shape = RoundedCornerShape(4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemeNeonCyan,
                                selectedLabelColor = Color(0xFF002026),
                                containerColor = Color(0xFF162032),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTierFilter == ProductTier.TIER_3,
                            onClick = { selectedTierFilter = if (selectedTierFilter == ProductTier.TIER_3) null else ProductTier.TIER_3 },
                            label = { CurrencyText("📱 Tier 3", fontSize = 11.sp) },
                            shape = RoundedCornerShape(4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE040FB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF162032),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                // List of Facilities
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { product ->
                        val prereqStatus = remember(product, businesses) { viewModel.getFacilityPrerequisites(product) }
                        val isUnlocked = prereqStatus.isMet
                        val ownedCount = businesses.count { it.type == product.facilityId }
                        val canAfford = playerMoney >= product.facilityCost
                        val brandColor = Color(product.colorTint)
                        val eligibleCities = remember(product) { cities.filter { it.canBuildProduct(product) } }
                        val countryFlags = remember(eligibleCities) { eligibleCities.map { it.countryFlag }.distinct() }
                        val cityCount = eligibleCities.size

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF131B2E),
                            border = BorderStroke(1.dp, if (isUnlocked) brandColor.copy(alpha = 0.35f) else ThemeBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = brandColor.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.45f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        UniversalProductIcon(product = product, size = 26.dp, tint = brandColor)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Facility Details
                                Column(modifier = Modifier.weight(1f)) {
                                    CurrencyText(
                                        text = product.getFacilityName(isEnglish),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    CurrencyText(
                                        text = "${tr("Üretim:", "Production:", isEnglish)} ${product.getDisplayName(isEnglish)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CurrencyText(
                                            text = formatCredit(product.facilityCost),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = if (canAfford) ThemeGold else ThemeNegative,
                                            fontSize = 11.5.sp
                                        )
                                        CurrencyText(
                                            text = "• ⏳ ${product.getInitialConstructionDurationLabel(isEnglish)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFBBF24),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (ownedCount > 0) {
                                            CurrencyText(
                                                text = "• ${tr("Sahip:", "Owned:", isEnglish)} $ownedCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ThemeNeonCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Country Flags & Eligible Cities Badge
                                    if (countryFlags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF090F1C),
                                            border = BorderStroke(0.5.dp, Color(0xFF26354E))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                countryFlags.forEach { flag ->
                                                    CurrencyText(text = flag, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(3.dp))
                                                CurrencyText(
                                                    text = if (isEnglish) "$cityCount ${if (cityCount == 1) "city" else "cities"}" else "$cityCount şehir",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF8E9EB5),
                                                    fontSize = 9.5.sp,
                                                    fontFamily = RobotoMonoFontFamily
                                                )
                                            }
                                        }
                                    }

                                    // Locked Prerequisite Reason Badge
                                    if (!isUnlocked) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isEnglish) prereqStatus.reasonEn else prereqStatus.reasonTr,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFCA5A5),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Select / Build / Research / Locked Action Button
                                if (isUnlocked) {
                                    AppButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectFacility(product)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (canAfford) ThemeNeonCyan else Color(0xFF1E293B),
                                            contentColor = if (canAfford) Color(0xFF002026) else Color.Gray
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .widthIn(min = 84.dp)
                                    ) {
                                        CurrencyText(
                                            text = if (isEnglish) "BUILD" else "ŞEHİR SEÇ",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            fontFamily = RobotoMonoFontFamily,
                                            maxLines = 1
                                        )
                                    }
                                } else if (prereqStatus.requiredTechNode != null) {
                                    AppButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val techNode = prereqStatus.requiredTechNode
                                            onNavigateToRd(techNode.id.removePrefix("tech_"))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF141E30),
                                            contentColor = ThemeNeonCyan
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.45f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .widthIn(min = 84.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Science,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            CurrencyText(
                                                text = if (isEnglish) "RESEARCH" else "AR-GE",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.5.sp,
                                                fontFamily = RobotoMonoFontFamily,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    AppButton(
                                        onClick = {},
                                        enabled = false,
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFF1E293B),
                                            disabledContentColor = Color(0xFF94A3B8)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .widthIn(min = 84.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            CurrencyText(
                                                text = if (isEnglish) "LOCKED" else "KİLİTLİ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.5.sp,
                                                fontFamily = RobotoMonoFontFamily,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}


@Composable
fun AssetsKpiRow(uiState: com.example.viewmodel.GameUiState, viewModel: com.example.viewmodel.GameViewModel, totalOwnedFacilities: Int, activeCitiesCount: Int) {
    
    val activeJobsCount = uiState.productionProgress.count { it.value > 0f }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiSummaryCard(
            label = "TOPLAM TESİS".trAuto(),
            value = "$totalOwnedFacilities " + "Adet".trAuto(),
            icon = Icons.Rounded.Business,
            accentColor = ThemeNeonCyan,
            modifier = Modifier.weight(1f)
        )

        KpiSummaryCard(
            label = "AKTİF ŞEHİR".trAuto(),
            value = "$activeCitiesCount / 81",
            icon = Icons.Rounded.LocationCity,
            accentColor = ThemeGold,
            modifier = Modifier.weight(1f)
        )

        KpiSummaryCard(
            label = "ÜRETİM HATTI".trAuto(),
            value = if (activeJobsCount > 0) "$activeJobsCount " + "Aktif".trAuto() else "Boşta".trAuto(),
            icon = Icons.Rounded.Engineering,
            accentColor = if (activeJobsCount > 0) ThemePositive else Color.Gray,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MarqueeTicker(businesses: List<BusinessEntity>, uiState: com.example.viewmodel.GameUiState, modifier: Modifier = Modifier) {
    val alertTexts = remember(businesses, uiState.productionProgress) {
        val texts = mutableListOf<String>()
        val constructing = businesses.filter { it.isConstructing }
        if (constructing.isNotEmpty()) {
            texts.add("🏗️ ŞANTİYE: ${constructing.size} yeni tesisin kurulum inşaatı devam ediyor.")
        }
        val wornOut = businesses.filter { it.wearLevel >= 0.7f }
        if (wornOut.isNotEmpty()) {
            texts.add("⚠️ UYARI: ${wornOut.size} tesis bakım gerektiriyor! Üretim yavaşladı.")
        }
        val maxLevel = businesses.filter { it.level >= 5 }
        if (maxLevel.isNotEmpty()) {
            texts.add("✅ BİLGİ: ${maxLevel.size} tesisiniz tam kapasiteyle (Lv.5) çalışıyor.")
        }
        val activeJobs = uiState.productionProgress.count { it.value > 0f }
        if (activeJobs > 0) {
            texts.add("⚙️ AKTİF: $activeJobs tesiste şu anda harıl harıl üretim yapılıyor.")
        }
        if (texts.isEmpty()) {
            texts.add("ℹ️ SİSTEM: Tüm tesisler standart operasyonlarına devam ediyor.")
        }
        texts.joinToString("   |   ")
    }

    Surface(
        color = Color(0xFF101828).copy(alpha = 0.8f),
        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.fillMaxWidth().height(28.dp).padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Campaign, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            CurrencyText(
                text = alertTexts,
                color = ThemeGold,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = RobotoMonoFontFamily,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}


@Composable
fun ProductHolographicMatrix(
    product: Product,
    recipeDetails: List<RecipeRequirementStatus>
) {
    var selectedDetail by remember { mutableStateOf<RecipeRequirementStatus?>(null) }
    val brandColor = Color(product.colorTint)
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, brandColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        val width = maxWidth
        val height = maxHeight
        val centerX = width / 2
        val centerY = height / 2

        val radiusX = width * 0.35f
        val radiusY = height * 0.30f

        // Draw connections
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 30.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height))
                x += gridStep
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y))
                y += gridStep
            }

            // Draw lines from center to each ingredient node
            recipeDetails.forEachIndexed { index, req ->
                val angle = (2 * Math.PI * index / recipeDetails.size).toFloat() - (Math.PI / 2).toFloat()
                val nodeX = centerX.toPx() + radiusX.toPx() * cos(angle)
                val nodeY = centerY.toPx() + radiusY.toPx() * sin(angle)
                
                // Draw connecting line
                drawLine(
                    color = if (req.isSufficient) ThemePositive else ThemeNegative.copy(alpha = 0.7f),
                    start = Offset(centerX.toPx(), centerY.toPx()),
                    end = Offset(nodeX, nodeY),
                    strokeWidth = if (req.isSufficient) 3f else 1.5f,
                    pathEffect = if (!req.isSufficient) PathEffect.dashPathEffect(floatArrayOf(15f, 10f)) else null
                )
                
                // Draw a small dot at the end
                drawCircle(
                    color = if (req.isSufficient) ThemePositive else ThemeNegative.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }
        }

        // Center Node
        Box(
            modifier = Modifier
                .offset(x = centerX - 35.dp, y = centerY - 35.dp)
                .size(70.dp)
                .shadow(16.dp, CircleShape, spotColor = brandColor)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))))
                .border(2.dp, brandColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            UniversalProductIcon(product = product, size = 35.dp, tint = brandColor)
        }

        // Orbit Nodes
        recipeDetails.forEachIndexed { index, req ->
            val angle = (2 * Math.PI * index / recipeDetails.size).toFloat() - (Math.PI / 2).toFloat()
            val offsetX = centerX + (radiusX.value * cos(angle)).dp
            val offsetY = centerY + (radiusY.value * sin(angle)).dp
            val isSelected = selectedDetail == req

            Box(
                modifier = Modifier
                    .offset(x = offsetX - 20.dp, y = offsetY - 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
                    .border(
                        width = if (req.isSufficient) 1.5.dp else if (isSelected) 1.5.dp else 1.dp,
                        color = if (req.isSufficient) ThemePositive else if (isSelected) Color.White else ThemeNegative.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .clickable { selectedDetail = if (isSelected) null else req },
                contentAlignment = Alignment.Center
            ) {
                // Find icon for required product
                val reqProduct = Product.values().find { it.id == req.productId }
                if (reqProduct != null) {
                    UniversalProductIcon(
                        product = reqProduct,
                        size = 20.dp,
                        tint = if (req.isSufficient) ThemePositive else ThemeNegative.copy(alpha = 0.8f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Inventory,
                        contentDescription = null,
                        tint = if (req.isSufficient) ThemePositive else ThemeNegative.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                if (!req.isSufficient) {
                    CircularProgressIndicator(
                        progress = { (req.stockAmount.toFloat() / req.requiredAmount.toFloat()).coerceIn(0f, 1f) },
                        color = ThemeNegative,
                        trackColor = Color.Transparent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
            }
        }
        
        // Detailed panel for selected node overlaying at the bottom
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedDetail != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedDetail?.let { detail ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF101828).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, if (detail.isSufficient) ThemePositive else ThemeNegative),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CurrencyText(
                                text = detail.productName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            IconButton(onClick = { selectedDetail = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Rounded.Close, null, tint = Color.LightGray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CurrencyText("Gereksinim: ${detail.requiredAmount} Ton", color = Color.LightGray, fontSize = 11.sp)
                            CurrencyText(
                                "Stok: ${detail.stockAmount} Ton",
                                color = if (detail.isSufficient) ThemePositive else ThemeNegative,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
