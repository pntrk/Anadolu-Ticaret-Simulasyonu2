package com.example.ui.screens

import com.example.ui.components.CurrencyText

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.TechTree
import com.example.ui.components.AppButton
import com.example.ui.components.GlassCard
import com.example.ui.components.ProductIconBadge
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.viewmodel.GameViewModel
import androidx.compose.ui.unit.max

data class TreeNode(
    val product: Product,
    val amount: Int,
    val children: List<TreeNode>,
    var width: Float = 1f,
    var x: Float = 0f,
    var y: Float = 0f
) {
    val maxDepth: Int
        get() = 1 + (children.maxOfOrNull { it.maxDepth } ?: 0)
}

fun buildTree(product: Product, amount: Int): TreeNode {
    val children = product.recipe.mapNotNull { req ->
        Product.values().find { it.id == req.productId }?.let {
            buildTree(it, req.amountPerUnit)
        }
    }
    return TreeNode(product, amount, children)
}

fun computeWidth(node: TreeNode): Float {
    if (node.children.isEmpty()) {
        node.width = 1f
        return 1f
    }
    var sum = 0f
    for (child in node.children) {
        sum += computeWidth(child)
    }
    node.width = sum
    return sum
}

fun computePositions(node: TreeNode, xOffset: Float, depth: Int, xStep: Float, yStep: Float) {
    node.x = xOffset + (node.width * xStep) / 2f
    node.y = depth * yStep

    var currentX = xOffset
    for (child in node.children) {
        computePositions(child, currentX, depth + 1, xStep, yStep)
        currentX += child.width * xStep
    }
}

@Composable
fun SupplyChainTreeScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    initialProduct: Product = Product.EV,
    onNavigateBack: () -> Unit,
    onNavigateToRd: (String?) -> Unit
) {
    var selectedProductForTree by remember { mutableStateOf(initialProduct) }
    var expandedProductDropdown by remember { mutableStateOf(false) }

    val treeRoot = remember(selectedProductForTree) {
        val root = buildTree(selectedProductForTree, 1)
        computeWidth(root)
        // xStep: horizontal distance per leaf node
        // yStep: vertical distance between tiers
        computePositions(root, 0f, 0, 140f, 160f)
        root
    }

    val treeWidth = remember(treeRoot) { (treeRoot.width * 140f).dp }
    val treeHeight = remember(treeRoot) { (treeRoot.maxDepth * 160f).dp }

    var selectedNode by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B101D))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131A2A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = tr("Geri", "Back"), tint = ThemeNeonCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    CurrencyText(
                        text = tr("TEDARİK ZİNCİRİ AĞACI", "SUPPLY CHAIN TREE"),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 12.5.sp
                    )
                    CurrencyText(
                        text = tr("Gelişmiş Üretim Planlama Modülü", "Advanced Production Planning Module"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }

            // Dropdown to select root product
            Box {
                AppButton(
                    onClick = { expandedProductDropdown = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2840), contentColor = ThemeNeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(selectedProductForTree.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText(selectedProductForTree.getDisplayName().trAuto(), fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = expandedProductDropdown,
                    onDismissRequest = { expandedProductDropdown = false },
                    modifier = Modifier.background(Color(0xFF131A2A))
                ) {
                    Product.values().filter { it.recipe.isNotEmpty() }.sortedBy { it.tier }.forEach { p ->
                        DropdownMenuItem(
                            text = { CurrencyText(p.getDisplayName().trAuto(), color = Color.White) },
                            onClick = {
                                selectedProductForTree = p
                                expandedProductDropdown = false
                            },
                            leadingIcon = {
                                Icon(p.icon, contentDescription = null, tint = Color(p.colorTint))
                            }
                        )
                    }
                }
            }
        }

        // Tree View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            // Container with computed sizes
            Box(
                modifier = Modifier
                    .width(max(treeWidth, 300.dp))
                    .height(max(treeHeight, 300.dp))
            ) {
                // Draw connecting lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawTreeLines(treeRoot, 60f) // 60f is half of node size for center offset
                }

                // Draw nodes
                DrawTreeNodes(
                    node = treeRoot,
                    onNodeClick = { selectedNode = it }
                )
            }
        }
    }

    // Modal Detailed Card
    if (selectedNode != null) {
        ProductDetailModal(
            uiState = uiState,
            onIntent = onIntent,
            product = selectedNode!!,
            viewModel = viewModel,
            onDismiss = { selectedNode = null },
            onNavigateToRd = onNavigateToRd
        )
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTreeLines(node: TreeNode, nodeRadius: Float) {
    for (child in node.children) {
        val startX = node.x.dp.toPx()
        val startY = node.y.dp.toPx() + nodeRadius
        val endX = child.x.dp.toPx()
        val endY = child.y.dp.toPx() - nodeRadius + 20f

        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                startX, startY + 50f,
                endX, endY - 50f,
                endX, endY
            )
        }

        // Glow effect
        drawPath(
            path = path,
            color = ThemeNeonCyan.copy(alpha = 0.2f),
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Main line
        drawPath(
            path = path,
            color = ThemeNeonCyan,
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw children lines recursively
        drawTreeLines(child, nodeRadius)
    }
}

@Composable
fun DrawTreeNodes(node: TreeNode, onNodeClick: (Product) -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = (node.x - 30).dp, y = (node.y).dp) // Center node (assuming 60dp total width)
            .size(60.dp)
            .clickable { onNodeClick(node.product) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProductIconBadge(
                product = node.product,
                size = 48.dp,
                showSectorBadge = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color(0xFF131A2A).copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(node.product.colorTint).copy(alpha = 0.5f))
            ) {
                CurrencyText(
                    text = "${node.amount}x",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }

    for (child in node.children) {
        DrawTreeNodes(child, onNodeClick)
    }
}

@Composable
fun ProductDetailModal(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    product: Product,
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onNavigateToRd: (String?) -> Unit
) {
    val inventory = uiState.inventoryState.items
    val businesses = uiState.businesses
    val isUnlocked = viewModel.isProductUnlocked(product.id)
    val hasFacility = businesses.any { it.type == product.facilityId }
    val currentStock = inventory.find { it.itemId == product.id }?.quantity ?: 0
    
    val requiredTechId = TechTree.nodes.find { it.unlockedProductIds.contains(product.id) }?.id
    val requiredTech = TechTree.nodes.find { it.id == requiredTechId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() }
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable(enabled = false) {}, // prevent click-through
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F1522).copy(alpha = 0.95f),
                borderColor = Color(product.colorTint)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProductIconBadge(product = product, size = 48.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    CurrencyText(
                                        text = product.getDisplayName().trAuto().uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = product.tier.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(product.colorTint),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, contentDescription = tr("Kapat", "Close"), tint = Color.Gray)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1B2840))

                        // Recipe Section
                        if (product.recipe.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CurrencyText(
                                    text = tr("ÜRETİM REÇETESİ", "PRODUCTION RECIPE"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold
                                )
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    product.recipe.forEach { req ->
                                        val reqProd = Product.values().firstOrNull { it.id == req.productId }
                                        val reqStock = inventory.firstOrNull { it.itemId == req.productId }?.quantity ?: 0
                                        val hasEnough = reqStock >= req.amountPerUnit
                                        
                                        Surface(
                                            color = Color(0xFF162032),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (hasEnough) ThemeNeonCyan else ThemeNegative.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    reqProd?.icon ?: Icons.Rounded.Inventory,
                                                    contentDescription = null,
                                                    tint = if (hasEnough) ThemeNeonCyan else ThemeNegative,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                CurrencyText(
                                                    text = "${req.amountPerUnit}x ${reqProd?.getDisplayName()?.trAuto() ?: req.productId}",
                                                    fontSize = 10.5.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            CurrencyText(
                                text = tr("🌱 Doğrudan Üretim (Girdi Gerektirmez)", "🌱 Direct Production (Requires No Inputs)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }

                        // Tech/R&D Section
                        if (!isUnlocked && requiredTech != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF2E1B2E),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE040FB))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Science, contentDescription = null, tint = Color(0xFFE040FB), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            CurrencyText(
                                                text = tr("AR-GE GEREKLİ", "RESEARCH REQUIRED"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE040FB)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        CurrencyText(
                                            text = androidx.compose.ui.res.stringResource(requiredTech.nameRes),
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                    AppButton(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToRd(requiredTech.id.removePrefix("tech_"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = Color.White)
                                    ) {
                                        CurrencyText(tr("AR-GE'YE GİT", "GO TO R&D"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Logistics/Storage Mockup
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0B101D),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1B2840))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Inventory, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        CurrencyText(tr("Stok Durumu", "Stock Status"), fontSize = 9.5.sp, color = Color.Gray)
                                        CurrencyText("$currentStock " + tr("Ton", "Tons"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        CurrencyText(tr("Bağlı Depo", "Connected Depot"), fontSize = 9.5.sp, color = Color.Gray)
                                        CurrencyText(tr("Merkez Lojistik", "Central Logistics"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ThemeNeonCyan)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF1B2840))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Fixed Sticky Bottom Facility & Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CurrencyText(
                                text = tr("TESİS", "FACILITY"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 9.5.sp
                            )
                            CurrencyText(
                                text = product.getFacilityName(com.example.ui.theme.isEnglishLanguage()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (isUnlocked) {
                            if (!hasFacility) {
                                AppButton(
                                    onClick = { /* Action to build facility */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026))
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(tr("KUR", "BUILD") + " (₳${product.facilityCost / 1_000_000}M)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                AppButton(
                                    onClick = { /* Action to manage/produce */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(tr("YÖNET", "MANAGE"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
