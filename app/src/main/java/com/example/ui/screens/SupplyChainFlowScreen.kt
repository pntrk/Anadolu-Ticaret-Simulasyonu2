package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.AppButton
import com.example.ui.components.CurrencyText
import com.example.ui.components.GlassCard
import com.example.ui.components.ProductIconBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

/**
 * Kesikli çizgi efekti veren Modifier extension.
 */
fun Modifier.dashedBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color = Color(0xFF64748B),
    cornerRadius: Dp = 12.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f
        )
    )
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = stroke
    )
}

/**
 * Tedarik Zinciri Akış Ekranı (SupplyChainFlowScreen).
 *
 * Soldan sağa kademeli kolonlar halinde (Tier 1 -> Tier 2 -> Tier 3 -> Tier 4)
 * kurulu tesisleri ve eksik tesisleri gösterir, hızlı üretim ve tesis kurma aksiyonlarını sağlar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyChainFlowScreen(
    viewModel: GameViewModel,
    initialProductId: String = "ev",
    onNavigateBack: () -> Unit = {},
    onNavigateToBuild: (String) -> Unit = {},
    onNavigateToWarehouse: (String?) -> Unit = {}
) {
    val businesses by viewModel.businesses.collectAsStateWithLifecycle(emptyList())
    val inventory by viewModel.inventory.collectAsStateWithLifecycle(emptyList())
    val activeProductions by viewModel.activeProductions.collectAsStateWithLifecycle(emptyList())
    val isEnglish = isEnglishLanguage()

    // Öne çıkan karmaşık reçeteli ürünler
    val featuredProductIds = remember {
        listOf(
            "ev",
            "cargo_ship",
            "smartphone",
            "uav",
            "satellite",
            "bullet_train",
            "ai_datacenter",
            "quantum_supercomputer",
            "smart_grid",
            "super_yacht",
            "hydrogen_plant",
            "defense_frigate"
        )
    }

    var selectedProductId by remember { mutableStateOf(initialProductId) }
    var showProductCatalogDropdown by remember { mutableStateOf(false) }

    // Seçili tesis detay ModalBottomSheet durumu
    var selectedNodeForSheet by remember { mutableStateOf<SupplyChainNode?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Tedarik zinciri hiyerarşisi
    val rootChainNode = remember(selectedProductId, businesses, inventory) {
        SupplyChainFlowEngine.buildTreeForProduct(selectedProductId, businesses, inventory)
    }

    // Ağaçtaki tüm düğümler (tekil olarak)
    val allChainNodes = remember(rootChainNode) {
        rootChainNode.flatten().distinctBy { it.product.id }
    }

    // Kademelere göre gruplama (Tier 1 -> Tier 4)
    val tier1Nodes = remember(allChainNodes) { allChainNodes.filter { it.tier == ProductTier.TIER_1 } }
    val tier2Nodes = remember(allChainNodes) { allChainNodes.filter { it.tier == ProductTier.TIER_2 } }
    val tier3Nodes = remember(allChainNodes) { allChainNodes.filter { it.tier == ProductTier.TIER_3 } }
    val tier4Nodes = remember(allChainNodes) { allChainNodes.filter { it.tier == ProductTier.TIER_4 } }

    val activeTiers = remember(tier1Nodes, tier2Nodes, tier3Nodes, tier4Nodes) {
        listOf(
            Pair(ProductTier.TIER_1, tier1Nodes),
            Pair(ProductTier.TIER_2, tier2Nodes),
            Pair(ProductTier.TIER_3, tier3Nodes),
            Pair(ProductTier.TIER_4, tier4Nodes)
        ).filter { it.second.isNotEmpty() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101726))
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = tr("Geri", "Back"),
                            tint = ThemeNeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tr("TEDARİK ZİNCİRİ AKIŞI", "SUPPLY CHAIN FLOW"),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ThemePositiveBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.8.dp, ThemePositive.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "CANLI",
                                    color = ThemePositive,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = tr(
                                "Uçtan Uca Sanayi ve Lojistik Entegrasyon Haritası",
                                "End-to-End Industrial & Logistic Integration Map"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    // Tüm Ürünler Dropdown Butonu
                    Box {
                        FilledTonalButton(
                            onClick = { showProductCatalogDropdown = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = ThemeGold
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Katalog", "Catalog"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showProductCatalogDropdown,
                            onDismissRequest = { showProductCatalogDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF131D2D))
                                .heightIn(max = 380.dp)
                        ) {
                            val complexProducts = remember {
                                Product.values().filter { it.recipe.isNotEmpty() || it.tier >= ProductTier.TIER_2 }
                            }
                            complexProducts.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ProductIconBadge(product = prod, size = 26.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    prod.displayName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    "${prod.tier.name} • ${formatMoney(prod.basePrice)} ₳",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedProductId = prod.id
                                        showProductCatalogDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Row of Target Products
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    featuredProductIds.forEach { prodId ->
                        val product = Product.values().find { it.id == prodId }
                        if (product != null) {
                            val isSelected = selectedProductId.equals(product.id, ignoreCase = true)
                            val hasFinalFacility = businesses.any { it.type == product.facilityId || it.type == product.id }

                            Surface(
                                onClick = { selectedProductId = product.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF1E2E42) else Color(0xFF131B29),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ThemeNeonCyan else Color(0xFF2B3A4F)
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = product.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) ThemeNeonCyan else Color(product.colorTint),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = product.displayName,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (hasFinalFacility) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(ThemePositive, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ana Gövde: Soldan Sağa Kademeli Kolonlar
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                activeTiers.forEachIndexed { index, (tier, nodes) ->
                    // Kolon
                    TierColumn(
                        tier = tier,
                        nodes = nodes,
                        activeProductions = activeProductions,
                        onNodeClick = { selectedNodeForSheet = it },
                        onNavigateToBuild = onNavigateToBuild,
                        modifier = Modifier.width(285.dp)
                    )

                    // Kolonlar arası akış bağlantı göstergesi
                    if (index < activeTiers.size - 1) {
                        FlowConnector(
                            fromTier = tier,
                            toTier = activeTiers[index + 1].first
                        )
                    }
                }
            }

            // Alt Tesis Detay ve Hızlı Yönetim ModalBottomSheet
            if (selectedNodeForSheet != null) {
                val node = selectedNodeForSheet!!
                val business = node.ownedBusiness
                val isNodeStorageFull = (business?.getRemainingStorageCapacity() ?: 0) <= 0 && business != null

                ModalBottomSheet(
                    onDismissRequest = { selectedNodeForSheet = null },
                    sheetState = sheetState,
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                    ) {
                        // Sheet Başlık
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ProductIconBadge(product = node.product, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = node.product.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = node.product.facilityName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemePositive,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (business != null) {
                                Surface(
                                    color = Color(0xFF1F2937),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "SEVİYE ${business.level}",
                                        color = ThemePositive,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tesis Durum İstatistikleri
                        if (business != null) {
                            val storedBiz = business.getStoredTotalQuantity()
                            val capBiz = business.getEffectiveStorageCapacity()

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1F2937),
                                border = BorderStroke(1.dp, if (isNodeStorageFull) ThemeNegative.copy(alpha = 0.7f) else Color(0xFF374151)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tr("Konum", "Location"), color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            business.cityId.replaceFirstChar { it.uppercase() },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tr("Tesis Deposu", "Storage"), color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            if (isNodeStorageFull) "$storedBiz/$capBiz t (DOLU)" else "$storedBiz/$capBiz t",
                                            color = if (isNodeStorageFull) ThemeNegative else ThemePositive,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tr("Sağlamlık", "Wear"), color = Color.Gray, fontSize = 10.sp)
                                        val integrity = (100 - (business.wearLevel * 100).toInt()).coerceIn(0, 100)
                                        Text(
                                            "%$integrity",
                                            color = if (integrity > 50) ThemePositive else ThemeNegative,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(tr("Merkez Depo", "Warehouse"), color = Color.Gray, fontSize = 10.sp)
                                        Text(
                                            "${node.stockInWarehouse} Ton",
                                            color = ThemeGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            if (isNodeStorageFull) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ThemeNegative.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = ThemeNegative, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = tr(
                                                "⚠️ Depo Kapasitesi Dolu: Depo dolunca üretim devam edemez, deponun boşalması beklenir.",
                                                "⚠️ Storage Full: Production cannot continue until storage is cleared."
                                            ),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeNegative
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reçete / Girdiler Özeti
                        if (node.product.recipe.isNotEmpty()) {
                            Text(
                                text = tr("Üretim Reçetesi & Depo Durumu:", "Production Recipe & Inventory:"),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                node.product.recipe.forEach { req ->
                                    val reqProd = Product.values().find { it.id == req.productId }
                                    val reqStock = inventory
                                        .filter { it.baseProductId.equals(req.productId, ignoreCase = true) || it.itemId.equals(req.productId, ignoreCase = true) }
                                        .sumOf { it.quantity }
                                    val isSufficient = reqStock >= req.amountPerUnit

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isSufficient) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                                contentDescription = null,
                                                tint = if (isSufficient) ThemePositive else ThemeNegative,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                reqProd?.displayName ?: req.productId,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            "${req.amountPerUnit} Ton gerekir (Depoda: $reqStock Ton)",
                                            color = if (isSufficient) Color(0xFF94A3B8) else ThemeNegative,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSufficient) FontWeight.Normal else FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Aksiyon Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Hızlı Üret Butonu
                            AppButton(
                                onClick = {
                                    viewModel.produce(node.product.id, requestedQuantity = 1)
                                    selectedNodeForSheet = null
                                },
                                enabled = !isNodeStorageFull,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isNodeStorageFull) Color(0xFF334155) else ThemePositive,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFF1E293B),
                                    disabledContentColor = Color(0xFFEF5350)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(if (isNodeStorageFull) Icons.Rounded.Inventory2 else Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isNodeStorageFull) tr("⚠️ Depo Dolu", "⚠️ Storage Full") else tr("Hızlı Üret", "Fast Produce"),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp
                                )
                            }

                            // Depoyu Gör Butonu
                            OutlinedButton(
                                onClick = {
                                    selectedNodeForSheet = null
                                    onNavigateToWarehouse(node.product.id)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ThemeNeonCyan
                                ),
                                border = BorderStroke(1.2.dp, ThemeNeonCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Depoyu Gör", "View Storage"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Belirli bir Tier kolonunu render eden Composable.
 */
@Composable
private fun TierColumn(
    tier: ProductTier,
    nodes: List<SupplyChainNode>,
    activeProductions: List<ActiveProduction>,
    onNodeClick: (SupplyChainNode) -> Unit,
    onNavigateToBuild: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = when (tier) {
        ProductTier.TIER_1 -> Color(0xFF38BDF8) // Sky Blue
        ProductTier.TIER_2 -> Color(0xFFFB923C) // Amber Orange
        ProductTier.TIER_3 -> Color(0xFFC084FC) // Purple
        ProductTier.TIER_4 -> ThemeGold        // Gold
    }

    val tierTitle = when (tier) {
        ProductTier.TIER_1 -> tr("TIER 1 • HAM MADDE", "TIER 1 • RAW MATERIALS")
        ProductTier.TIER_2 -> tr("TIER 2 • İŞLENMİŞ ARA MAL", "TIER 2 • INTERMEDIATE")
        ProductTier.TIER_3 -> tr("TIER 3 • İLERİ BİLEŞEN", "TIER 3 • ADVANCED PARTS")
        ProductTier.TIER_4 -> tr("TIER 4 • NİHAİ MONTAJ", "TIER 4 • FINAL ASSEMBLY")
    }

    val ownedCount = nodes.count { it.ownedBusiness != null }

    Column(
        modifier = modifier
            .background(Color(0xFF0F1522), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Kolon Başlığı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tierTitle,
                color = tierColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.5.sp,
                fontFamily = RobotoMonoFontFamily,
                letterSpacing = 0.5.sp
            )

            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "$ownedCount / ${nodes.size} Aktif",
                    color = if (ownedCount == nodes.size) ThemePositive else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Düğüm Kartları Listesi
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            nodes.forEach { node ->
                if (node.ownedBusiness != null) {
                    // Kurulu Tesis Kartı (Yeşil Vurgulu)
                    OwnedFacilityCard(
                        node = node,
                        activeProductions = activeProductions,
                        onClick = { onNodeClick(node) }
                    )
                } else {
                    // Olmayan Tesis Kartı (Kesikli Gri Çizgili)
                    MissingFacilityCard(
                        node = node,
                        onBuildClick = { onNavigateToBuild(node.product.id) }
                    )
                }
            }
        }
    }
}

/**
 * Kurulu Tesis Kartı (Yeşil vurgulu):
 * Şehir adı, kademesi ve anlık çalışma durumu yazar.
 */
@Composable
private fun OwnedFacilityCard(
    node: SupplyChainNode,
    activeProductions: List<ActiveProduction>,
    onClick: () -> Unit
) {
    val business = node.ownedBusiness ?: return
    val product = node.product

    val isProducing = activeProductions.any { prod ->
        (prod.businessId != 0 && prod.businessId == business.id) ||
        (prod.facilityId.isNotBlank() && prod.facilityId == business.type) ||
        prod.productId.equals(product.id, ignoreCase = true)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C1F16), // Koyu zümrüt arka plan
        border = BorderStroke(1.5.dp, ThemePositive.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Üst Satır: İkon + İsim + Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductIconBadge(product = product, size = 34.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.facilityName,
                        fontSize = 10.5.sp,
                        color = Color(0xFF86EFAC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val isStorageFull = business.getRemainingStorageCapacity() <= 0

                // Anlık Durum Rozeti
                val statusText = when {
                    isProducing -> "🟢 Üretimde"
                    isStorageFull -> "⚠️ Depo Dolu"
                    node.isBottleneck -> "🔴 Darboğaz"
                    else -> "🟢 Faal"
                }
                val statusColor = when {
                    isProducing -> ThemePositive
                    isStorageFull -> ThemeNegative
                    node.isBottleneck -> ThemeNegative
                    else -> Color(0xFF4ADE80)
                }

                Surface(
                    color = if (isStorageFull) Color(0xFF7F1D1D).copy(alpha = 0.7f) else Color(0xFF064E3B).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.6.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Orta Bilgiler: Şehir, Seviye, Depo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = business.cityId.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "⭐ Seviye ${business.level}",
                    fontSize = 11.sp,
                    color = ThemeGold,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "📦 ${node.stockInWarehouse}t",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Alt yönlendirme ipucu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isStorageFull = business.getRemainingStorageCapacity() <= 0
                Text(
                    text = if (isStorageFull) tr("⚠️ Depo Dolu (Üretim Durdu) ➔", "⚠️ Storage Full (Stopped) ➔") else tr("Hızlı Üret & Yönet ➔", "Fast Produce & Manage ➔"),
                    fontSize = 9.5.sp,
                    color = if (isStorageFull) ThemeNegative else Color(0xFF86EFAC),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Olmayan Tesis Kartı (Kesikli gri çizgili):
 * 'Tesis Yok: [Ürün Adı]' yazar ve üzerinde büyük '➕ Tesis Kur' butonu bulunur.
 */
@Composable
private fun MissingFacilityCard(
    node: SupplyChainNode,
    onBuildClick: () -> Unit
) {
    val product = node.product

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(
                strokeWidth = 1.5.dp,
                color = Color(0xFF64748B),
                cornerRadius = 12.dp
            )
            .background(Color(0x1F1E293B), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF334155), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = product.icon,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tesis Yok: ${product.displayName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tr("Zincir Kesintili", "Supply Broken"),
                        fontSize = 10.sp,
                        color = ThemeNegative,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "📦 ${node.stockInWarehouse}t",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Büyük '➕ Tesis Kur' Butonu
            AppButton(
                onClick = onBuildClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB).copy(alpha = 0.9f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("➕ Tesis Kur", "➕ Build Facility"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Kolonlar arasındaki akış yön göstergesi ve bağlantı çizgisi.
 */
@Composable
private fun FlowConnector(
    fromTier: ProductTier,
    toTier: ProductTier
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF1E293B), CircleShape)
                .border(1.dp, Color(0xFF334155), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = ThemeNeonCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "AKIŞ",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = RobotoMonoFontFamily
        )
    }
}
