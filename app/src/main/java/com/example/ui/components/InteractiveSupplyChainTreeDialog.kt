package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

/**
 * Tedarik Ağacı Düğümü Modeli (Supply Tree Node)
 */
data class SupplyChainNode(
    val product: Product,
    val requiredPerUnit: Int,
    val totalRequiredForTarget: Int,
    val level: Int,
    val parentId: String?,
    val children: List<SupplyChainNode>
)

/**
 * Tedarik Bağı Çizimi (Görsel Ağaç - Interactive Visual Supply Chain Tree Flow)
 * Bir ürün (özellikle Tier 2, 3 ve 4) seçildiğinde, ona ait tüm alt hammadde ve bileşen
 * ağacını dallı budaklı interaktif bir akış şeması şeklinde gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveSupplyChainTreeDialog(
    targetProduct: Product,
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onNavigateToFacility: ((Product) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()

    var targetBatchSize by remember { mutableIntStateOf(1) }
    var selectedNodeProduct by remember { mutableStateOf<Product?>(targetProduct) }

    // Reçete Ağacını Özyinelemeli (Recursive) Olarak Oluştur
    fun buildNode(product: Product, reqPerUnit: Int, level: Int, parentId: String?, multiplier: Int): SupplyChainNode {
        val totalNeeded = reqPerUnit * multiplier
        val children = product.recipe.map { req ->
            val childProd = Product.values().find { it.id == req.productId }
            if (childProd != null) {
                buildNode(childProd, req.amountPerUnit, level + 1, product.id, totalNeeded)
            } else null
        }.filterNotNull()

        return SupplyChainNode(
            product = product,
            requiredPerUnit = reqPerUnit,
            totalRequiredForTarget = totalNeeded,
            level = level,
            parentId = parentId,
            children = children
        )
    }

    val rootNode = remember(targetProduct, targetBatchSize) {
        buildNode(targetProduct, 1, 0, null, targetBatchSize)
    }

    // Ağaçtaki tüm eksik malzemeleri topla
    fun collectMissingRequirements(node: SupplyChainNode, accumulated: MutableMap<String, Int>) {
        if (node.level > 0) {
            val stock = inventory.find { it.itemId == node.product.id }?.quantity ?: 0
            val missing = (node.totalRequiredForTarget - stock).coerceAtLeast(0)
            if (missing > 0) {
                val current = accumulated.getOrDefault(node.product.id, 0)
                accumulated[node.product.id] = current + missing
            }
        }
        node.children.forEach { collectMissingRequirements(it, accumulated) }
    }

    val allMissingMap = remember(rootNode, inventory) {
        val map = mutableMapOf<String, Int>()
        collectMissingRequirements(rootNode, map)
        map
    }

    val totalMissingCount = allMissingMap.values.sum()
    val totalEstimatedCost = allMissingMap.entries.sumOf { (itemId, qty) ->
        val price = marketPrices.find { it.itemId == itemId }?.price ?: (Product.values().find { it.id == itemId }?.basePrice ?: 0L)
        price * qty
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .shadow(24.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0B101B),
            border = BorderStroke(1.5.dp, Brush.verticalGradient(listOf(ThemeNeonCyan, Color(0xFF1E293B))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(targetProduct.colorTint).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(targetProduct.colorTint))
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                UniversalProductIcon(
                                    product = targetProduct,
                                    size = 24.dp
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurrencyText(
                                    text = targetProduct.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeGold.copy(alpha = 0.15f)
                                ) {
                                    CurrencyText(
                                        text = targetProduct.tier.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            CurrencyText(
                                text = tr("İnteraktif Tedarik Bağı & Reçete Ağacı", "Interactive Supply Chain & Recipe Tree"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Üretim Adedi Seçici & Durum Özeti
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131B2E),
                    border = BorderStroke(1.dp, Color(0xFF23314E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyText(
                                text = tr("Hedef Üretim:", "Target Output:"),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray
                            )

                            // 1x, 5x, 10x Butonları
                            listOf(1, 5, 10).forEach { size ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        targetBatchSize = size
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (targetBatchSize == size) ThemeNeonCyan else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (targetBatchSize == size) ThemeNeonCyan else Color(0xFF334155))
                                ) {
                                    CurrencyText(
                                        text = "${size}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (targetBatchSize == size) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Tedarik Durumu Rozeti
                        if (totalMissingCount == 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemePositive.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, ThemePositive)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(12.dp))
                                    CurrencyText(
                                        text = tr("Tüm Girdiler Hazır", "All Materials Ready"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemePositive,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF5252).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFFFF5252))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(12.dp))
                                    CurrencyText(
                                        text = tr("$totalMissingCount Ton Eksik", "$totalMissingCount Tons Missing"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tedarik Zinciri Ağacı (Scrollable Flow View)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF080C14))
                        .border(1.dp, Color(0xFF1B2438), RoundedCornerShape(8.dp))
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SupplyChainNodeView(
                            node = rootNode,
                            inventory = inventory,
                            selectedProduct = selectedNodeProduct,
                            onSelect = { product ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedNodeProduct = product
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Seçili Ürün Hızlı Aksiyon & Eksik Satın Alma Barı
                val activeNode = selectedNodeProduct ?: targetProduct
                val activeStock = inventory.find { it.itemId == activeNode.id }?.quantity ?: 0
                val activeEntity = marketPrices.find { it.itemId == activeNode.id }
                val activePrice = activeEntity?.price ?: activeNode.basePrice
                val isActiveUsd = activeEntity?.effectiveIsUsd ?: (activeNode.getEligibleCities(cities).firstOrNull()?.country != "Türkiye")
                val activeCountry = activeEntity?.originCountry ?: (activeNode.getEligibleCities(cities).firstOrNull()?.country ?: "Türkiye")
                val activeFlag = getCountryFlagEmoji(activeCountry)
                val activeFormattedPrice = formatCredit(activePrice)
                val activeMissing = allMissingMap.getOrDefault(activeNode.id, 0)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131B2E),
                    border = BorderStroke(1.dp, Color(0xFF263553))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Seçili Ürün Bilgisi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UniversalProductIcon(
                                    product = activeNode,
                                    size = 18.dp
                                )
                                CurrencyText(
                                    text = activeNode.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                CurrencyText(
                                    text = "• $activeFlag $activeFormattedPrice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActiveUsd) ThemeGold else ThemeNeonCyan,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }

                            CurrencyText(
                                text = tr("Depo: $activeStock Ton", "Stock: $activeStock Tons"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeStock > 0) ThemePositive else Color.Gray,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        // Aksiyon Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1x Borsadan Satın Al
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromBorsa(activeNode.id, 1))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = tr("+1 Al", "+1 Buy"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Eksik Varsa Eksik Kadar Al Butonu
                            if (activeMissing > 0) {
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromBorsa(activeNode.id, activeMissing))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ThemeGold,
                                        contentColor = Color(0xFF2B1700)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.3f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(
                                        text = tr("Eksiği Al ($activeMissing T)", "Buy Missing ($activeMissing T)"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Toplu Tüm Eksikleri Borsadan Al
                            if (totalMissingCount > 0) {
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        allMissingMap.forEach { (itemId, qty) ->
                                            if (qty > 0) {
                                                viewModel.handleIntent(com.example.viewmodel.GameIntent.BuyFromBorsa(itemId, qty))
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ThemePositive,
                                        contentColor = Color(0xFF002B14)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.5f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(
                                        text = tr("Tüm Eksikleri Al (${formatMoney(totalEstimatedCost)})", "Buy All Missing (${formatMoney(totalEstimatedCost)})"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
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
    }
}

/**
 * Özyinelemeli Tedarik Ağacı Bileşeni (Branching Tree Composable)
 */
@Composable
private fun SupplyChainNodeView(
    node: SupplyChainNode,
    inventory: List<InventoryEntity>,
    selectedProduct: Product?,
    onSelect: (Product) -> Unit
) {
    val stock = inventory.find { it.itemId == node.product.id }?.quantity ?: 0
    val isFulfilled = stock >= node.totalRequiredForTarget
    val isSelected = selectedProduct?.id == node.product.id

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Düğüm Kartı
        Surface(
            modifier = Modifier
                .clickable { onSelect(node.product) }
                .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) Color(0xFF192841) else Color(0xFF101726),
            border = BorderStroke(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) ThemeNeonCyan else if (isFulfilled) ThemePositive.copy(alpha = 0.6f) else Color(0xFFFF5252).copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // İkon & Renk
                Surface(
                    shape = CircleShape,
                    color = Color(node.product.colorTint).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(node.product.colorTint))
                ) {
                    Box(modifier = Modifier.padding(4.dp)) {
                        UniversalProductIcon(
                            product = node.product,
                            size = 20.dp
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CurrencyText(
                            text = node.product.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        CurrencyText(
                            text = "[${node.product.tier.name}]",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyText(
                            text = tr("Gerekli: ${node.totalRequiredForTarget} T", "Req: ${node.totalRequiredForTarget} T"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                        CurrencyText(
                            text = if (isFulfilled) tr("• Depo: $stock T (✓ Hazır)", "• Stock: $stock T (✓ Ready)")
                            else tr("• Depo: $stock T (⚠️ Eksik: ${node.totalRequiredForTarget - stock} T)", "• Stock: $stock T (⚠️ Missing: ${node.totalRequiredForTarget - stock} T)"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFulfilled) ThemePositive else Color(0xFFFF5252)
                        )
                    }
                }
            }
        }

        // Alt Reçete Dalları (Children Branches)
        if (node.children.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Görsel Bağlantı Çizgisi
                Canvas(
                    modifier = Modifier
                        .width(16.dp)
                        .height(32.dp)
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        cubicTo(0f, size.height * 0.6f, size.width, size.height * 0.4f, size.width, size.height)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF334155),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    node.children.forEach { childNode ->
                        SupplyChainNodeView(
                            node = childNode,
                            inventory = inventory,
                            selectedProduct = selectedProduct,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}
