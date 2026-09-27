package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.theme.tr
import com.example.viewmodel.GameViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SupplyChainAnalyzerDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onSelectBuildFacility: (Product) -> Unit,
    onSelectProduce: (Product) -> Unit,
    onNavigateToRd: (String?) -> Unit,
    onNavigateToConsortium: (() -> Unit)? = null
) {
    val theme = LocalAppThemeOption.current
    val haptic = LocalHapticFeedback.current

    var selectedTierTab by remember { mutableIntStateOf(0) } // 0: Reçete Ağacı Haritası, 1: T1, 2: T2, 3: T3, 4: T4, 5: Reçete Lab

    // Selected product node for supply chain highlighting
    var selectedProductNode by remember { mutableStateOf<Product?>(null) }

    // Collected states from GameViewModel
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0C101D),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Ultra Compact Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF101726),
                    border = BorderStroke(1.dp, Color(0xFF1E2C44))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountTree,
                                        contentDescription = null,
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CurrencyText(
                                        text = tr("TEDARİK ZİNCİRİ", "SUPPLY CHAIN"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White,
                                        fontSize = 11.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = ThemeGold.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f))
                                    ) {
                                        CurrencyText(
                                            text = tr("HARİTA", "MAP"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            color = ThemeGold,
                                            fontSize = 7.5.sp,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                CurrencyText(
                                    text = tr("Tier 1 ➔ Tier 4 Dönüşüm & Reçeteler", "Tier 1 ➔ Tier 4 Conversion & Recipes"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Return to Production Menu Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A263B),
                                contentColor = ThemeNeonCyan
                            ),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.West,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("ÜRETİM MENÜSÜNE DÖN", "RETURN TO PRODUCTION"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTierTab,
                    containerColor = Color(0xFF121B2E),
                    contentColor = ThemeNeonCyan,
                    edgePadding = 0.dp,
                    indicator = {},
                    divider = {}
                ) {
                    val tabs = listOf(
                        tr("🌳 TEDARİK ZİNCİRİ (HARİTA)", "🌳 SUPPLY CHAIN (MAP)"),
                        tr("🌾 Tier 1 Ham Madde", "🌾 Tier 1 Raw Material"),
                        tr("🏭 Tier 2 Ara Mal", "🏭 Tier 2 Intermediate Product"),
                        tr("📱 Tier 3 Nihai Ürün", "📱 Tier 3 Final Product"),
                        tr("🚀 Tier 4 Mega Sanayi", "🚀 Tier 4 Mega Industry")
                    )
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTierTab == index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTierTab = index
                            },
                            modifier = Modifier
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selectedTierTab == index)
                                        Brush.horizontalGradient(listOf(ThemeNeonCyan.copy(alpha = 0.25f), Color(0xFF132A3E)))
                                    else androidx.compose.ui.graphics.SolidColor(Color(0xFF162032))
                                )
                                .border(
                                    1.dp,
                                    if (selectedTierTab == index) ThemeNeonCyan else Color(0xFF26334D),
                                    RoundedCornerShape(4.dp)
                                ),
                            text = {
                                CurrencyText(
                                    text = title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTierTab == index) FontWeight.Black else FontWeight.Bold,
                                    color = if (selectedTierTab == index) ThemeNeonCyan else Color.Gray,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Content View based on Tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF080C17))
                        .border(1.dp, Color(0xFF1B263B), RoundedCornerShape(8.dp))
                ) {
                    when (selectedTierTab) {
                        0 -> InteractiveRecipeTreeTab(
                            viewModel = viewModel,
                            businesses = businesses,
                            inventory = inventory,
                            selectedNode = selectedProductNode,
                            onSelectNode = { product ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedProductNode = if (selectedProductNode == product) null else product
                            },
                            onSelectBuildFacility = onSelectBuildFacility,
                            onSelectProduce = onSelectProduce,
                            onNavigateToRd = onNavigateToRd,
                            onNavigateToConsortium = onNavigateToConsortium
                        )
                        1 -> TierProductsList(viewModel, ProductTier.TIER_1, businesses, inventory, onSelectBuildFacility, onSelectProduce, onNavigateToRd, onNavigateToConsortium)
                        2 -> TierProductsList(viewModel, ProductTier.TIER_2, businesses, inventory, onSelectBuildFacility, onSelectProduce, onNavigateToRd, onNavigateToConsortium)
                        3 -> TierProductsList(viewModel, ProductTier.TIER_3, businesses, inventory, onSelectBuildFacility, onSelectProduce, onNavigateToRd, onNavigateToConsortium)
                        4 -> TierProductsList(viewModel, ProductTier.TIER_4, businesses, inventory, onSelectBuildFacility, onSelectProduce, onNavigateToRd, onNavigateToConsortium)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 0: INTERACTIVE BRANCHING RECIPE TREE
// ==========================================
fun getAllDescendantProductIds(startProduct: Product, allProducts: List<Product> = Product.values().toList()): Set<String> {
    val result = mutableSetOf<String>()
    fun dfs(prod: Product) {
        prod.recipe.forEach { req ->
            if (result.add(req.productId)) {
                allProducts.find { it.id == req.productId }?.let { child ->
                    dfs(child)
                }
            }
        }
    }
    dfs(startProduct)
    return result
}

fun getAllAncestorProductIds(startProduct: Product, allProducts: List<Product> = Product.values().toList()): Set<String> {
    val result = mutableSetOf<String>()
    fun dfs(productId: String) {
        allProducts.forEach { candidate ->
            if (candidate.recipe.any { it.productId == productId }) {
                if (result.add(candidate.id)) {
                    dfs(candidate.id)
                }
            }
        }
    }
    dfs(startProduct.id)
    return result
}

@Composable
fun ConnectedChainBar(
    selectedNode: Product,
    allConnectedProducts: List<Product>,
    businesses: List<BusinessEntity>,
    onSelectNode: (Product) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF0E182A),
        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = tr("🔗 BAĞLANTILI TERSİ VE TESİSLER ZİNCİRİ (Tier 1 ➔ Tier 4)", "🔗 CONNECTED INVERSE AND FACILITIES CHAIN (Tier 1 ➔ Tier 4)"),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemeNeonCyan,
                    fontFamily = RobotoMonoFontFamily
                )
                CurrencyText(
                    text = "${allConnectedProducts.size} " + tr("Bağlantılı Unsur", "Connected Elements"),
                    fontSize = 8.5.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(allConnectedProducts, key = { it.id }) { product ->
                    val isSelected = product.id == selectedNode.id
                    val isOwned = businesses.any { it.type == product.facilityId }
                    val isDescendant = getAllDescendantProductIds(selectedNode).contains(product.id)
                    val isAncestor = getAllAncestorProductIds(selectedNode).contains(product.id)

                    val chipBorderColor = when {
                        isSelected -> ThemeNeonCyan
                        isDescendant -> ThemeGold
                        isAncestor -> Color(0xFFE040FB)
                        else -> Color(0xFF23324A)
                    }

                    val chipBgColor = when {
                        isSelected -> Color(0xFF132A3E)
                        isDescendant -> Color(0xFF261D0C)
                        isAncestor -> Color(0xFF22112E)
                        else -> Color(0xFF111A2C)
                    }

                    val tierLabel = when (product.tier) {
                        ProductTier.TIER_1 -> "T1"
                        ProductTier.TIER_2 -> "T2"
                        ProductTier.TIER_3 -> "T3"
                        ProductTier.TIER_4 -> "T4"
                    }

                    Surface(
                        modifier = Modifier.clickable { onSelectNode(product) },
                        shape = RoundedCornerShape(6.dp),
                        color = chipBgColor,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, chipBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = chipBorderColor.copy(alpha = 0.2f)
                            ) {
                                CurrencyText(
                                    text = tierLabel,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = chipBorderColor,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            UniversalProductIcon(product = product, size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                CurrencyText(
                                    text = product.getDisplayName(),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                CurrencyText(
                                    text = if (isOwned) "🏭 ${product.getFacilityName()}" else "❌ ${product.getFacilityName()}",
                                    fontSize = 7.5.sp,
                                    color = if (isOwned) ThemeNeonCyan else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveRecipeTreeTab(
    viewModel: GameViewModel,
    businesses: List<BusinessEntity>,
    inventory: List<InventoryEntity>,
    selectedNode: Product?,
    onSelectNode: (Product?) -> Unit,
    onSelectBuildFacility: (Product) -> Unit,
    onSelectProduce: (Product) -> Unit,
    onNavigateToRd: (String?) -> Unit,
    onNavigateToConsortium: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var onlyOwnedFilter by remember { mutableStateOf(false) }
    var onlyBottleneckFilter by remember { mutableStateOf(false) }
    var onlyConnectedFilter by remember { mutableStateOf(false) }

    val allProducts = remember { Product.values().toList() }

    // Multi-tier recursive connections
    val allDescendantIds = remember(selectedNode) {
        if (selectedNode == null) emptySet()
        else getAllDescendantProductIds(selectedNode, allProducts)
    }

    val allAncestorIds = remember(selectedNode) {
        if (selectedNode == null) emptySet()
        else getAllAncestorProductIds(selectedNode, allProducts)
    }

    val allConnectedIds = remember(selectedNode, allDescendantIds, allAncestorIds) {
        if (selectedNode == null) emptySet()
        else setOf(selectedNode.id) + allDescendantIds + allAncestorIds
    }

    val allConnectedProducts = remember(allConnectedIds) {
        if (selectedNode == null) emptyList()
        else allProducts.filter { it.id in allConnectedIds }.sortedBy { it.tier }
    }

    // Direct 1-level relations for NodeInspectionDrawer
    val ingredientProducts = remember(selectedNode) {
        selectedNode?.recipe?.mapNotNull { req ->
            allProducts.find { it.id == req.productId }?.let { prod -> prod to req.amountPerUnit }
        } ?: emptyList()
    }

    val downstreamTargetProducts = remember(selectedNode) {
        if (selectedNode == null) emptyList()
        else allProducts.filter { target ->
            target.recipe.any { req -> req.productId == selectedNode.id }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { CurrencyText(tr("Ürün veya tesis adı ara...", "Search product or facility..."), color = Color.Gray, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(4.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeNeonCyan,
                    unfocusedBorderColor = Color(0xFF233044),
                    focusedContainerColor = Color(0xFF090D16),
                    unfocusedContainerColor = Color(0xFF090D16),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Owned Only Filter Chip
            FilterChip(
                selected = onlyOwnedFilter,
                onClick = {
                    onlyOwnedFilter = !onlyOwnedFilter
                    if (onlyOwnedFilter) onlyBottleneckFilter = false
                },
                label = { CurrencyText(tr("🏭 Tesislerim", "🏭 My Facilities"), fontSize = 10.sp) },
                shape = RoundedCornerShape(4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ThemeNeonCyan,
                    selectedLabelColor = Color(0xFF002026),
                    containerColor = Color(0xFF162032),
                    labelColor = Color.LightGray
                )
            )

            // Bottleneck Only Filter Chip
            FilterChip(
                selected = onlyBottleneckFilter,
                onClick = {
                    onlyBottleneckFilter = !onlyBottleneckFilter
                    if (onlyBottleneckFilter) onlyOwnedFilter = false
                },
                label = { CurrencyText(tr("⚠️ Darboğazlar", "⚠️ Bottlenecks"), fontSize = 10.sp) },
                shape = RoundedCornerShape(4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ThemeNegative,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF2B1919),
                    labelColor = Color.LightGray
                )
            )

            // Connected Only Filter Chip (Visible when node is focused)
            if (selectedNode != null) {
                FilterChip(
                    selected = onlyConnectedFilter,
                    onClick = { onlyConnectedFilter = !onlyConnectedFilter },
                    label = { CurrencyText(tr("🔗 Sadece Bağlantılılar", "🔗 Only Connected") + " (${allConnectedIds.size})", fontSize = 10.sp) },
                    shape = RoundedCornerShape(4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThemeGold,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF261D0C),
                        labelColor = ThemeGold
                    )
                )
            }

            // Clear Focus Button if node selected
            if (selectedNode != null) {
                AppButton(
                    onClick = { onSelectNode(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1C1C), contentColor = ThemeNegative),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyText("ODAK SIFIRLA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Focus Banner (if node selected)
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedNode?.let { node ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF112233),
                    border = BorderStroke(1.dp, ThemeNeonCyan)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = node.icon,
                                contentDescription = null,
                                tint = Color(node.colorTint),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CurrencyText(
                                text = "ODAK: ${node.getDisplayName().uppercase()} (${node.getFacilityName()})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CurrencyText(
                                text = "(${node.tier})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("Tüm Bağlantılı Zincir: ", "All Connected Chain: ") + "${allDescendantIds.size} " + tr("Girdi Tesis", "Input Facility") + " ➔ ${allAncestorIds.size} " + tr("Hedef Tesis", "Target Facility"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                            CurrencyText(
                                text = tr("Girdiler (Sarı) | Hedefler (Mor)", "Inputs (Yellow) | Targets (Purple)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Connected Chain Bar
                        ConnectedChainBar(
                            selectedNode = node,
                            allConnectedProducts = allConnectedProducts,
                            businesses = businesses,
                            onSelectNode = { onSelectNode(it) }
                        )
                    }
                }
            }
        }

        // 4 Horizontal Columns Layout (Tier 1 ➔ Tier 2 ➔ Tier 3 ➔ Tier 4)
        Row(
            modifier = Modifier
                .weight(if (selectedNode != null) 0.50f else 1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProductTier.entries.forEach { tier ->
                val tierProducts = remember(searchQuery, onlyOwnedFilter, onlyBottleneckFilter, onlyConnectedFilter, selectedNode, tier, inventory, businesses, allConnectedIds) {
                    allProducts.filter { prod ->
                        val matchesSearch = searchQuery.isEmpty() ||
                                prod.getDisplayName().contains(searchQuery, ignoreCase = true) ||
                                prod.getFacilityName().contains(searchQuery, ignoreCase = true)
                        val matchesOwned = !onlyOwnedFilter || businesses.any { it.type == prod.facilityId }
                        val isBottleneck = prod.recipe.isNotEmpty() && prod.recipe.any { req ->
                            (inventory.find { inv -> inv.itemId == req.productId }?.quantity ?: 0) < req.amountPerUnit
                        }
                        val matchesBottleneck = !onlyBottleneckFilter || isBottleneck
                        val matchesConnected = !onlyConnectedFilter || selectedNode == null || prod.id in allConnectedIds

                        prod.tier == tier && matchesSearch && matchesOwned && matchesBottleneck && matchesConnected
                    }
                }

                RecipeTierColumn(
                    tier = tier,
                    products = tierProducts,
                    viewModel = viewModel,
                    businesses = businesses,
                    inventory = inventory,
                    selectedNode = selectedNode,
                    ingredientIds = allDescendantIds,
                    downstreamIds = allAncestorIds,
                    onSelectNode = onSelectNode
                )
            }
        }

        // Bottom Selected Node Inspection Bar
        if (selectedNode != null) {
            Box(modifier = Modifier.weight(1.50f).fillMaxWidth()) {
                NodeInspectionDrawer(
                    product = selectedNode,
                    viewModel = viewModel,
                    businesses = businesses,
                    inventory = inventory,
                    downstreamTargets = downstreamTargetProducts,
                    onSelectNode = onSelectNode,
                    onCloseNode = { onSelectNode(null) },
                    onSelectBuildFacility = onSelectBuildFacility,
                    onSelectProduce = onSelectProduce,
                    onNavigateToRd = onNavigateToRd,
                    onNavigateToConsortium = onNavigateToConsortium
                )
            }
        }
    }
}

// ==========================================
// COLUMN FOR A SINGLE TIER IN TREE
// ==========================================
@Composable
fun RecipeTierColumn(
    tier: ProductTier,
    products: List<Product>,
    viewModel: GameViewModel,
    businesses: List<BusinessEntity>,
    inventory: List<InventoryEntity>,
    selectedNode: Product?,
    ingredientIds: Set<String>,
    downstreamIds: Set<String>,
    onSelectNode: (Product) -> Unit
) {
    val tierHeaderTitle = when (tier) {
        ProductTier.TIER_1 -> tr("🌾 TIER 1: HAM MADDELER", "🌾 TIER 1: RAW MATERIALS")
        ProductTier.TIER_2 -> tr("🏭 TIER 2: İŞLENMİŞ ARA MALLAR", "🏭 TIER 2: INTERMEDIATE GOODS")
        ProductTier.TIER_3 -> tr("📱 TIER 3: NİHAİ ÜRÜNLER", "📱 TIER 3: FINAL PRODUCTS")
        ProductTier.TIER_4 -> tr("🚀 TIER 4: MEGA SANAYİ", "🚀 TIER 4: MEGA INDUSTRY")
    }

    val tierHeaderColor = when (tier) {
        ProductTier.TIER_1 -> ThemeGold
        ProductTier.TIER_2 -> ThemeNeonCyan
        ProductTier.TIER_3 -> Color(0xFFE040FB)
        ProductTier.TIER_4 -> Color(0xFF00E676)
    }

    Column(
        modifier = Modifier
            .width(230.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1424))
            .border(1.dp, Color(0xFF1B2840), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Tier Header
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = tierHeaderColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, tierHeaderColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = tierHeaderTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = tierHeaderColor,
                    fontSize = 10.sp,
                    fontFamily = RobotoMonoFontFamily
                )
                CurrencyText(
                    text = "${products.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Product Cards Scrollable List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { product ->
                val isSelected = selectedNode?.id == product.id
                val isIngredient = ingredientIds.contains(product.id)
                val isDownstream = downstreamIds.contains(product.id)

                val ownedCount = businesses.count { it.type == product.facilityId }
                val currentStock = inventory.find { it.itemId == product.id }?.quantity ?: 0
                val isUnlocked = viewModel.isProductUnlocked(product.id)
                val isBottleneck = isUnlocked && product.recipe.isNotEmpty() && product.recipe.any { req ->
                    (inventory.find { inv -> inv.itemId == req.productId }?.quantity ?: 0) < req.amountPerUnit
                }

                // Visual State Styling
                val alpha = if (selectedNode == null || isSelected || isIngredient || isDownstream) 1f else 0.35f

                val borderColor = when {
                    isSelected -> ThemeNeonCyan
                    isBottleneck -> ThemeNegative
                    isIngredient -> ThemeGold
                    isDownstream -> Color(0xFFE040FB)
                    ownedCount > 0 -> Color(0xFF263D5C)
                    else -> Color(0xFF1A2436)
                }

                val borderWidth = if (isSelected || isIngredient || isDownstream || isBottleneck) 2.dp else 1.dp

                val containerColor = when {
                    isSelected -> Color(0xFF12283A)
                    isBottleneck -> Color(0xFF2A1515)
                    isIngredient -> Color(0xFF261D0C)
                    isDownstream -> Color(0xFF22112E)
                    ownedCount > 0 -> Color(0xFF101B2B)
                    else -> Color(0xFF0F1726)
                }

                val brandColor = Color(product.colorTint)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .clickable { onSelectNode(product) },
                    shape = RoundedCornerShape(6.dp),
                    color = containerColor,
                    border = BorderStroke(borderWidth, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Badge Tags Row (Left: Role / Status Tag, Right: Ownership Pill)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isBottleneck) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeNegative.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ThemeNegative)
                                ) {
                                    CurrencyText(
                                        text = tr("⚠️ DARBOĞAZ", "⚠️ BOTTLENECK"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeNegative,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (isIngredient) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeGold.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ThemeGold)
                                ) {
                                    CurrencyText(
                                        text = tr("📥 REÇETE GİRDİSİ", "📥 RECIPE INPUT"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeGold,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (isDownstream) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFFE040FB).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFE040FB))
                                ) {
                                    CurrencyText(
                                        text = tr("📤 HEDEF KULLANIM", "📤 TARGET USE"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE040FB),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan)
                                ) {
                                    CurrencyText(
                                        text = tr("⭐ SEÇİLİ ODAK", "⭐ SELECTED FOCUS"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeNeonCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            // Ownership Pill
                            if (ownedCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                                ) {
                                    CurrencyText(
                                        text = "x$ownedCount Tesis",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeNeonCyan,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (!isUnlocked) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2B1919),
                                    border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.5f))
                                ) {
                                    CurrencyText(
                                        text = "🔒 Kilitli",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeNegative,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF162032),
                                    border = BorderStroke(1.dp, Color(0xFF26334D))
                                ) {
                                    CurrencyText(
                                        text = "Tesis Yok",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 8.5.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product Icon
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = brandColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    UniversalProductIcon(
                                        product = product,
                                        size = 22.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = product.getDisplayName(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = product.getFacilityName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Live Warehouse Stock Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (currentStock > 0) ThemeGold.copy(alpha = 0.15f) else Color(0xFF162032),
                                border = BorderStroke(1.dp, if (currentStock > 0) ThemeGold.copy(alpha = 0.5f) else Color(0xFF233044))
                            ) {
                                CurrencyText(
                                    text = "📦 " + tr("DEPO STOĞU: ", "WAREHOUSE STOCK: ") + "$currentStock " + tr("Ton", "Tons"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentStock > 0) ThemeGold else Color.Gray,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Recipe Summary
                        if (product.recipe.isNotEmpty()) {
                            val reqSummary = product.recipe.joinToString(" + ") { req ->
                                val reqName = Product.values().toList().find { it.id == req.productId }?.getDisplayName() ?: req.productId
                                "${req.amountPerUnit}x $reqName"
                            }
                            CurrencyText(
                                text = "📥 $reqSummary ➔ 1x ${product.getDisplayName()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD54F),
                                fontSize = 8.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 11.sp
                            )
                        } else {
                            CurrencyText(
                                text = tr("🌱 Doğrudan Üretim (Girdi Yok)", "🌱 Direct Production (No Inputs)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// BOTTOM INSPECTION DRAWER FOR SELECTED NODE
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NodeInspectionDrawer(
    product: Product,
    viewModel: GameViewModel,
    businesses: List<BusinessEntity>,
    inventory: List<InventoryEntity>,
    downstreamTargets: List<Product>,
    onSelectNode: (Product) -> Unit,
    onCloseNode: () -> Unit,
    onSelectBuildFacility: (Product) -> Unit,
    onSelectProduce: (Product) -> Unit,
    onNavigateToRd: (String?) -> Unit,
    onNavigateToConsortium: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isUnlocked = viewModel.isProductUnlocked(product.id)
    val ownedCount = businesses.count { it.type == product.facilityId }
    val currentStock = inventory.find { it.itemId == product.id }?.quantity ?: 0
    val playerMoney = viewModel.player.collectAsStateWithLifecycle().value?.money ?: 0L

    val brandColor = Color(product.colorTint)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B1220),
        border = BorderStroke(1.dp, ThemeNeonCyan),
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            // Header Row: Product Icon, Name, Tier, Facility Info + Stats Pills + X Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.6f)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            UniversalProductIcon(
                                product = product,
                                size = 20.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = product.getDisplayName(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = brandColor.copy(alpha = 0.2f)
                            ) {
                                CurrencyText(
                                    text = product.tier.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brandColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                        CurrencyText(
                            text = "Tesis: ${product.getFacilityName()} • Kurulum: ${formatCredit(product.facilityCost)} • Birim: ${formatCredit(product.basePrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Side: Stats Pills & Close 'X' Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF162238)
                    ) {
                        CurrencyText(
                            text = "Tesis: $ownedCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeNeonCyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF162238)
                    ) {
                        CurrencyText(
                            text = "Stok: $currentStock Ton",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeGold,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Top-Right Close "X" Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF233047),
                        border = BorderStroke(1.dp, Color(0xFF3A4D6C))
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCloseNode()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = tr("Odak Kartını Kapat", "Close Focus Card"),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // =========================================================================
            // ACTION BUTTONS BAR (MOVED HIGH UP - ABOVE SCROLL DETAILS & DEVICE BOTTOM)
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121B2B),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUnlocked) {
                        val isT4 = product.tier == ProductTier.TIER_4
                        if (isT4 && ownedCount == 0) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (onNavigateToConsortium != null) onNavigateToConsortium() else onSelectBuildFacility(product)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF1E1400)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("🏛️ KONSORSİYUM İLE KUR", "🏛️ BUILD VIA CONSORTIUM"),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else if (ownedCount == 0) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectBuildFacility(product)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Rounded.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("TESİS KUR", "BUILD FACILITY") + " (${formatCredit(product.facilityCost)})",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (ownedCount > 0) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectProduce(product)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("ÜRETİMİ BAŞLAT", "START PRODUCTION"),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val techId = com.example.data.TechTree.nodes.find { it.unlockedProductIds.contains(product.id) }?.id?.removePrefix("tech_")
                                onNavigateToRd(techId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381C38), contentColor = Color(0xFFF06292)),
                            border = BorderStroke(1.dp, Color(0xFFE040FB)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFE040FB), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = tr("🔒 KİLİTLİ - AR-GE ARAŞTIRMASINA GİT", "🔒 LOCKED - GO TO RESEARCH"),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Scrollable Details Section (Bottleneck Banner, Recipe, Uses, Properties)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val missingRequirements = product.recipe.filter { req ->
                    val reqStock = inventory.firstOrNull { it.itemId == req.productId }?.quantity ?: 0
                    reqStock < req.amountPerUnit
                }
                val hasBottleneck = isUnlocked && missingRequirements.isNotEmpty()

                // Bottleneck Overall Status Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        !isUnlocked -> Color(0xFF261D1D)
                        product.recipe.isEmpty() -> Color(0xFF12281D)
                        hasBottleneck -> Color(0xFF331616)
                        else -> Color(0xFF10281C)
                    },
                    border = BorderStroke(1.dp, when {
                        !isUnlocked -> ThemeNegative.copy(alpha = 0.5f)
                        product.recipe.isEmpty() -> Color(0xFF2E7D32)
                        hasBottleneck -> ThemeNegative
                        else -> Color(0xFF2E7D32)
                    })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                !isUnlocked -> Icons.Rounded.Lock
                                product.recipe.isEmpty() -> Icons.Rounded.CheckCircle
                                hasBottleneck -> Icons.Rounded.Warning
                                else -> Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            tint = when {
                                !isUnlocked -> ThemeNegative
                                product.recipe.isEmpty() -> Color(0xFF81C784)
                                hasBottleneck -> ThemeNegative
                                else -> Color(0xFF81C784)
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        CurrencyText(
                            text = when {
                                !isUnlocked -> tr("🔒 DÜĞÜM KİLİTLİ: Üretim ve tesis için AR-GE araştırması gereklidir.", "🔒 NODE LOCKED: R&D research is required for production and facility.")
                                product.recipe.isEmpty() -> tr("🌱 DOĞRUDAN ÜRETİM: Temel ham maddedir, girdi gerektirmez.", "🌱 DIRECT PRODUCTION: Basic raw material, requires no inputs.")
                                hasBottleneck -> tr("⚠️ DARBOĞAZ: ", "⚠️ BOTTLENECK: ") + "${missingRequirements.size} " + tr("hammadde bileşeni eksik! Stok veya tesis kurun.", "raw material components missing! Buy stock or build facility.")
                                else -> tr("✅ STOK YETERLİ: Depodaki hammaddeler üretime tam hazır.", "✅ STOCK SUFFICIENT: Raw materials in depot are fully ready for production.")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                !isUnlocked -> ThemeNegative
                                product.recipe.isEmpty() -> Color(0xFFA5D6A7)
                                hasBottleneck -> Color(0xFFFF8A80)
                                else -> Color(0xFFA5D6A7)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Left Column: Recipe Ingredients Stock Status
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CurrencyText(
                            text = tr("📥 GİRDİ REÇETESİ & DURUM:", "📥 INPUT RECIPE & STATUS:"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            fontSize = 8.5.sp
                        )

                        if (product.recipe.isEmpty()) {
                            CurrencyText(
                                text = "🌱 Girdi gerektirmez.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF81C784),
                                fontSize = 8.5.sp
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                product.recipe.forEach { req ->
                                    val reqProd = Product.values().toList().firstOrNull { it.id == req.productId }
                                    val reqStock = inventory.firstOrNull { it.itemId == req.productId }?.quantity ?: 0
                                    val hasEnough = reqStock >= req.amountPerUnit

                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = if (hasEnough) Color(0xFF12241A) else Color(0xFF2E1515),
                                        border = BorderStroke(1.dp, if (hasEnough) Color(0xFF2E7D32) else ThemeNegative)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (hasEnough) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                                contentDescription = null,
                                                tint = if (hasEnough) Color(0xFF81C784) else ThemeNegative,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            CurrencyText(
                                                text = if (hasEnough)
                                                    "✅ ${req.amountPerUnit}x ${reqProd?.getDisplayName() ?: req.productId} ($reqStock)"
                                                else
                                                    "⚠️ ${req.amountPerUnit}x ${reqProd?.getDisplayName() ?: req.productId} (Eksik: ${req.amountPerUnit - reqStock})",
                                                fontSize = 8.sp,
                                                color = if (hasEnough) Color.White else ThemeNegative,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Downstream Uses
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CurrencyText(
                            text = tr("📤 HEDEF KULLANIM ALANLARI", "📤 TARGET USE AREAS") + " (${downstreamTargets.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE040FB),
                            fontSize = 8.5.sp
                        )

                        if (downstreamTargets.isEmpty()) {
                            CurrencyText(
                                text = tr("Ağacın en üst kademesindedir. Doğrudan pazarda satılır.", "It is at the top level of the tree. Sold directly on the market."),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 8.5.sp
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                downstreamTargets.take(6).forEach { target ->
                                    Surface(
                                        modifier = Modifier.clickable { onSelectNode(target) },
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(0xFF1E142B),
                                        border = BorderStroke(1.dp, Color(0xFF8E24AA))
                                    ) {
                                        CurrencyText(
                                            text = target.getDisplayName(),
                                            fontSize = 8.5.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (downstreamTargets.size > 6) {
                                    CurrencyText(
                                        text = "+${downstreamTargets.size - 6} daha",
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Facility Properties
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF161A25),
                    border = BorderStroke(1.dp, Color(0xFF26334D))
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CurrencyText(
                            text = "📋 " + tr("TESİS VE ÜRETİM ÖZELLİKLERİ", "FACILITY & PRODUCTION SPECIFICATIONS"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan,
                            fontSize = 8.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText(tr("Tesis Adı:", "Facility Name:"), color = Color.Gray, fontSize = 8.5.sp)
                            CurrencyText(product.getFacilityName(), color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText(tr("Taban Fiyat / Maliyet:", "Base Price / Cost:"), color = Color.Gray, fontSize = 8.5.sp)
                            CurrencyText("${formatCredit(product.basePrice)} / ${formatCredit(viewModel.getDynamicProductionCost(product, businesses.find { it.type == product.facilityId }?.cityId))}", color = ThemeGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText(tr("Kurulum Maliyeti / Süre:", "Build Cost / Time:"), color = Color.Gray, fontSize = 8.5.sp)
                            CurrencyText("${formatCredit(product.facilityCost)} (${product.baseDurationMs / 1000}s)", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DETAILED LIST TAB FOR INDIVIDUAL TIERS
// ==========================================
@Composable
fun TierProductsList(
    viewModel: GameViewModel,
    tier: ProductTier,
    businesses: List<BusinessEntity>,
    inventory: List<InventoryEntity>,
    onSelectBuildFacility: (Product) -> Unit,
    onSelectProduce: (Product) -> Unit,
    onNavigateToRd: (String?) -> Unit,
    onNavigateToConsortium: (() -> Unit)? = null
) {
    val products = remember(tier) { Product.values().toList().filter { it.tier == tier } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (tier == ProductTier.TIER_4) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF261A08),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            CurrencyText(
                                text = tr("🏛️ KONSORSİYUM MEGA PROJELERİ", "🏛️ CONSORTIUM MEGA PROJECTS"),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = ThemeGold
                            )
                            CurrencyText(
                                text = tr(
                                    "Tier 4 Mega Tesisler devasa sanayi ortaklığı gerektirir ve doğrudan kurulamaz. Konsorsiyum kurarak veya ortak projelere katılarak inşa edilir.",
                                    "Tier 4 Mega Facilities require collaborative industrial partnership and cannot be built directly. Built by launching or joining Consortium projects."
                                ),
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        items(products, key = { it.id }) { prod ->
            val isUnlocked = viewModel.isProductUnlocked(prod.id)
            val ownedCount = businesses.count { it.type == prod.facilityId }
            val currentStock = inventory.find { it.itemId == prod.id }?.quantity ?: 0
            val brandColor = Color(prod.colorTint)

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F1726),
                border = BorderStroke(1.dp, Color(0xFF1B2840))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = brandColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    UniversalProductIcon(
                                        product = prod,
                                        size = 24.dp
                                    )
                                }
                            }
                            Column {
                                CurrencyText(
                                    text = prod.getDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                CurrencyText(
                                    text = "Tesis: ${prod.getFacilityName()} • Sahip: $ownedCount • Stok: $currentStock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isUnlocked) {
                                if (prod.tier == ProductTier.TIER_4 && ownedCount == 0) {
                                    AppButton(
                                        onClick = {
                                            if (onNavigateToConsortium != null) onNavigateToConsortium() else onSelectBuildFacility(prod)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF1E1400)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText(tr("KONSORSİYUM", "CONSORTIUM"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                } else if (ownedCount == 0) {
                                    AppButton(
                                        onClick = { onSelectBuildFacility(prod) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        CurrencyText(tr("TESİS KUR", "BUILD FACILITY"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                                if (ownedCount > 0) {
                                    AppButton(
                                        onClick = { onSelectProduce(prod) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        CurrencyText(tr("ÜRETİM YAP", "PRODUCE"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                AppButton(
                                    onClick = { 
                                        val techId = com.example.data.TechTree.nodes.find { it.unlockedProductIds.contains(prod.id) }?.id?.removePrefix("tech_")
                                        onNavigateToRd(techId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1B2E), contentColor = Color(0xFFE040FB)),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(
                                        text = tr("AR-GE GİT", "GO TO R&D"),
                                        color = Color(0xFFE040FB),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (prod.recipe.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF090E18), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CurrencyText(
                                text = "📥 " + tr("REÇETE (1x ${prod.getDisplayName()} Elde Etmek İçin):", "RECIPE (To Obtain 1x ${prod.getDisplayName()}):"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold
                            )
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                prod.recipe.forEach { req ->
                                    val inputProd = Product.values().toList().find { it.id == req.productId }
                                    val reqName = inputProd?.getDisplayName() ?: req.productId
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF162032),
                                        border = BorderStroke(1.dp, Color(0xFF233044))
                                    ) {
                                        CurrencyText(
                                            text = "${req.amountPerUnit}x $reqName",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                                ) {
                                    CurrencyText(
                                        text = "➔ 1x ${prod.getDisplayName()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ThemeNeonCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

// ==========================================
// CUSTOM RECIPE ENGINE TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomRecipeCreatorEngineTab() {
    val theme = LocalAppThemeOption.current
    var customProductName by remember { mutableStateOf("") }
    var customFacilityName by remember { mutableStateOf("") }
    var targetTier by remember { mutableStateOf(ProductTier.TIER_2) }

    val selectedIngredients = remember { mutableStateListOf<RecipeRequirement>() }
    var showIngredientPicker by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    var customRecipes by remember { mutableStateOf(CustomRecipeManager.getCustomRecipes()) }

    val validationResult = remember(targetTier, selectedIngredients.toList()) {
        CustomRecipeManager.validateRecipe(targetTier, selectedIngredients)
    }

    val estimatedBasePrice = remember(selectedIngredients.toList(), targetTier) {
        val totalIngredientCost = selectedIngredients.sumOf { req ->
            val p = Product.values().toList().find { it.id == req.productId }
            (p?.basePrice ?: 10000L) * req.amountPerUnit
        }
        val tierMultiplier = when (targetTier) {
            ProductTier.TIER_1 -> 1.2f
            ProductTier.TIER_2 -> 1.5f
            ProductTier.TIER_3 -> 2.2f
            ProductTier.TIER_4 -> 3.5f
        }
        ((totalIngredientCost.coerceAtLeast(10000L)) * tierMultiplier).toLong()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CurrencyText(
            text = "⚡ OMEGACRAFT SANAYİ REÇETE MOTORU",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ThemeNeonCyan
        )
        CurrencyText(
            text = "Kendi sanayi ürünlerinizi tasarlayın ve sanayi veri kütüphanesine kaydedin.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = customProductName,
            onValueChange = { customProductName = it },
            label = { CurrencyText(tr("Yeni Ürün Adı", "New Product Name")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeNeonCyan,
                unfocusedBorderColor = Color(0xFF233044),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        OutlinedTextField(
            value = customFacilityName,
            onValueChange = { customFacilityName = it },
            label = { CurrencyText(tr("Üretilecek Tesis Adı", "Manufacturing Facility Name")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeNeonCyan,
                unfocusedBorderColor = Color(0xFF233044),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Tier Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductTier.entries.forEach { tier ->
                FilterChip(
                    selected = targetTier == tier,
                    onClick = { targetTier = tier },
                    label = { CurrencyText(tier.toString(), fontSize = 11.sp) },
                    shape = RoundedCornerShape(4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThemeNeonCyan,
                        selectedLabelColor = Color(0xFF002026),
                        containerColor = Color(0xFF162032),
                        labelColor = Color.LightGray
                    )
                )
            }
        }

        // Ingredient Picker Area
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0F1726),
            border = BorderStroke(1.dp, Color(0xFF1B2840)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(tr("Reçete Bileşenleri", "Recipe Ingredients") + " (${selectedIngredients.size})", fontWeight = FontWeight.Bold, color = ThemeGold, fontSize = 12.sp)
                    AppButton(
                        onClick = { showIngredientPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(tr("Bileşen Ekle", "Add Ingredient"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedIngredients.isEmpty()) {
                    CurrencyText(tr("Henüz bileşen eklenmedi.", "No ingredients added yet."), fontSize = 11.sp, color = Color.Gray)
                } else {
                    selectedIngredients.forEachIndexed { idx, req ->
                        val p = Product.values().toList().find { it.id == req.productId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText("${req.amountPerUnit}x ${p?.getDisplayName() ?: req.productId}", color = Color.White, fontSize = 11.sp)
                            IconButton(onClick = { selectedIngredients.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = ThemeNegative, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Save Custom Recipe Button
        AppButton(
            onClick = {
                if (customProductName.isNotBlank() && customFacilityName.isNotBlank()) {
                    val newRecipe = CustomRecipe(
                        id = "custom_${System.currentTimeMillis()}",
                        name = customProductName,
                        facilityName = customFacilityName,
                        tier = targetTier,
                        recipeRequirements = selectedIngredients.toList(),
                        productionCost = estimatedBasePrice / 2L,
                        basePrice = estimatedBasePrice,
                        facilityCost = estimatedBasePrice * 100L
                    )
                    if (CustomRecipeManager.addCustomRecipe(newRecipe)) {
                        customRecipes = CustomRecipeManager.getCustomRecipes()
                        customProductName = ""
                        customFacilityName = ""
                        selectedIngredients.clear()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
            enabled = customProductName.isNotBlank() && customFacilityName.isNotBlank() && validationResult.isValid,
            colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            CurrencyText(tr("🚀 YENİ REÇETEYİ KÜTÜPHANEYE KAYDET", "🚀 SAVE NEW RECIPE TO LIBRARY"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Divider(color = Color(0xFF1B2840), thickness = 1.dp)

        CurrencyText(
            text = "📋 " + tr("Özel Kayıtlı Reçeteleriniz", "Your Custom Registered Recipes") + " (${customRecipes.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        customRecipes.forEach { cr ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0F1726),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = "${cr.name} (${cr.tier})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan
                        )
                        CurrencyText(
                            text = formatCredit(cr.basePrice),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold
                        )
                    }
                    CurrencyText(
                        text = tr("Tesis: ", "Facility: ") + cr.facilityName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // Ingredient Picker Dialog
    if (showIngredientPicker) {
        val maxAllowedOrdinal = targetTier.ordinal - 1
        val availableProducts = Product.values().toList().filter { it.tier.ordinal <= maxAllowedOrdinal }

        AlertDialog(
            onDismissRequest = { showIngredientPicker = false },
            title = { CurrencyText(tr("Bileşen Seçin", "Select Ingredient") + " (${targetTier} " + tr("için Max Tier", "for Max Tier") + " ${maxAllowedOrdinal + 1})") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableProducts, key = { it.id }) { prod ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF121B2E),
                            border = BorderStroke(1.dp, Color(0xFF1B2840)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val existing = selectedIngredients.find { it.productId == prod.id }
                                    if (existing == null) {
                                        selectedIngredients.add(RecipeRequirement(prod.id, 1))
                                    } else {
                                        val idx = selectedIngredients.indexOf(existing)
                                        selectedIngredients[idx] = existing.copy(amountPerUnit = existing.amountPerUnit + 1)
                                    }
                                    showIngredientPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    CurrencyText(prod.getDisplayName(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    CurrencyText("${prod.tier} • ${formatCredit(prod.basePrice)}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = ThemeNeonCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIngredientPicker = false }) {
                    CurrencyText(tr("Kapat", "Close"))
                }
            },
            containerColor = Color(0xFF101726)
        )
    }
}
