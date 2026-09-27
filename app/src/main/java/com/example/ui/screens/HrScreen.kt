package com.example.ui.screens

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.components.formatCredit

import androidx.compose.foundation.clickable
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.CompanyManager
import com.example.data.automation.AutopilotEngine
import com.example.data.automation.SmartDirective
import com.example.ui.components.NotificationType
import com.example.ui.components.AppButton
import com.example.ui.components.GlassCard
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch

@Composable
fun HrScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isEng = isEnglishLanguage()
    val player = uiState.playerState.player
    val gameState = uiState.gameStateObj
    val marketPrices = uiState.marketState.prices
    val managers = uiState.managers
    val p = player ?: return
    val state = gameState ?: return

    val liveInventory by viewModel.inventory.collectAsState()
    val liveBusinesses by viewModel.businesses.collectAsState()
    val liveMegaProjects by viewModel.megaProjects.collectAsState()

    LaunchedEffect(p.money, liveInventory, liveBusinesses, liveMegaProjects) {
        AutopilotEngine.evaluateLiveGameContext(viewModel)
    }

    val hiredCount = managers.count { it.isHired }
    val totalDailySalaries = managers.filter { it.isHired && it.isActive }.sumOf { it.dailySalary }
    val averageEfficiency = if (hiredCount > 0) {
        run { val hired = managers.filter { it.isHired }; if (hired.isNotEmpty()) (hired.sumOf { it.efficiency.toDouble() } / hired.size).toFloat() else 0f }
    } else 0f
    val treasuryReserve = viewModel.getTreasuryCashReserve()
    var selectedManagerForLogs by remember { mutableStateOf<com.example.data.CompanyManager?>(null) }
    var isHierarchyExpanded by remember { mutableStateOf(false) }
    
    val marketPriceRatio = remember(marketPrices) {
        if (marketPrices.isNotEmpty()) {
            val totalRatios = com.example.data.Product.values().map { p ->
                val curPrice = marketPrices.find { it.itemId == p.id }?.price ?: p.basePrice
                (curPrice.toFloat() / p.basePrice.toFloat())
            }
            if (totalRatios.isNotEmpty()) (totalRatios.sum() / totalRatios.size).toFloat().coerceIn(0.75f, 2.50f) else 1f
        } else {
            1.0f
        }
    }

    val hierarchyOrder = remember {
        listOf(
            "mgr_treasury",
            "mgr_contracts",
            "mgr_borsa",
            "mgr_logistics",
            "mgr_hr",
            "mgr_rd",
            "mgr_prod",
            "mgr_maintenance"
        )
    }
    val sortedManagers = remember(managers) {
        managers.sortedBy { mgr ->
            val idx = hierarchyOrder.indexOf(mgr.id)
            if (idx != -1) idx else 999
        }
    }

    var selectedFilterIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredManagers = remember(sortedManagers, selectedFilterIndex, searchQuery) {
        val baseList = when (selectedFilterIndex) {
            1 -> sortedManagers.filter { it.isHired }
            2 -> sortedManagers.filter { !it.isHired }
            3 -> sortedManagers.filter { it.id in listOf("mgr_treasury", "mgr_borsa") }
            4 -> sortedManagers.filter { it.id in listOf("mgr_contracts", "mgr_logistics") }
            5 -> sortedManagers.filter { it.id in listOf("mgr_prod", "mgr_maintenance", "mgr_rd", "mgr_hr") }
            else -> sortedManagers
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.name.lowercase().contains(q) ||
                it.title.lowercase().contains(q) ||
                it.specialty.lowercase().contains(q)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp
        val managerChunks = remember(filteredManagers, isWideScreen) {
            filteredManagers.chunked(if (isWideScreen) 2 else 1)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
        ) {
            // 1. HR HEADER HERO BANNER & EXECUTIVE KPI SUMMARY
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(ThemeGold, ThemeNeonCyan, ThemeGold)))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF16233B), Color(0xFF0B1322))
                                )
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = ThemeGold.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ThemeGold),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.People,
                                            contentDescription = null,
                                            tint = ThemeGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    CurrencyText(
                                        tr("İnsan Kaynakları & Yönetim", "Human Resources & Management"),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        tr("Holding Operasyonları & C-Suite Liderleri", "Holding Operations & C-Suite Executives"),
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (hiredCount > 0) ThemeNeonCyan.copy(alpha = 0.15f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (hiredCount > 0) ThemeNeonCyan.copy(alpha = 0.8f) else Color.Gray)
                            ) {
                                CurrencyText(
                                    "$hiredCount/${managers.size} " + tr("Kadro", "Staff"),
                                    color = if (hiredCount > 0) ThemeNeonCyan else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        // 4 KPI Micro Cards Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Bordro
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131D31),
                                border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    CurrencyText(tr("Bordro Yükü", "Payroll Cost"), color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(formatCurrency(totalDailySalaries, isEng), color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Ortalama Verim
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131D31),
                                border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    CurrencyText(tr("Yönetim Verimi", "Efficiency"), color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("%${averageEfficiency.toInt()}", color = ThemePositive, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        LinearProgressIndicator(
                                            progress = (averageEfficiency / 100f).coerceIn(0f, 1f),
                                            color = ThemePositive,
                                            trackColor = Color(0xFF1E293B),
                                            modifier = Modifier.height(4.dp).weight(1f).clip(RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }

                            // Elmas Bütçesi
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131D31),
                                border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    CurrencyText(tr("Elmas Bütçesi", "Gem Budget"), color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Diamond, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        CurrencyText("${p.gems} 💎", color = ThemeNeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Hazine Rezervi
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131D31),
                                border = BorderStroke(0.8.dp, Color(0xFF24344F)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    CurrencyText(tr("Hazine Rezervi", "Treasury Reserve"), color = Color.Gray, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(if (treasuryReserve > 0L) formatCurrency(treasuryReserve, isEng) else tr("Pasif", "Inactive"), color = if (treasuryReserve > 0L) ThemeGold else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. FİLTRELER & YÖNETİM AKSİYONLARI (Arama, Filtre Çipleri ve Resmi Tatil/İşbaşı Butonu)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = {
                            CurrencyText(
                                tr("Yönetici veya departman ara...", "Search director or department..."),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Clear, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeGold,
                            unfocusedBorderColor = Color(0xFF26354D),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    val filterList = listOf(
                        tr("Tümü", "All") to sortedManagers.size,
                        tr("İşe Alınanlar", "Hired") to hiredCount,
                        tr("Açık Kadro", "Vacant") to (sortedManagers.size - hiredCount),
                        tr("Finans & Borsa", "Finance & Exchange") to sortedManagers.count { it.id in listOf("mgr_treasury", "mgr_borsa") },
                        tr("Tedarik & Satış", "Supply & Sales") to sortedManagers.count { it.id in listOf("mgr_contracts", "mgr_logistics") },
                        tr("Operasyon & Ar-Ge", "Operations & R&D") to sortedManagers.count { it.id in listOf("mgr_prod", "mgr_maintenance", "mgr_rd", "mgr_hr") }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filterList.forEachIndexed { index, (label, count) ->
                            val isSelected = selectedFilterIndex == index
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilterIndex = index
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) ThemeGold.copy(alpha = 0.2f) else Color(0xFF131D31),
                                border = BorderStroke(1.dp, if (isSelected) ThemeGold else Color(0xFF26354D))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CurrencyText(
                                        text = label,
                                        color = if (isSelected) ThemeGold else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) ThemeGold else Color(0xFF1E293B)
                                    ) {
                                        CurrencyText(
                                            text = "$count",
                                            color = if (isSelected) Color(0xFF1A1300) else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hiredCount > 0) {
                        val anyActive = managers.filter { it.isHired }.any { it.isActive }
                        val (buttonText, buttonColor, icon) = if (anyActive) {
                            Triple(tr("TÜM MÜDÜRLERİ İZNE ÇIKAR (RESMİ TATİL)", "GRANT ALL DIRECTORS LEAVE (PUBLIC HOLIDAY)"), Color(0xFFE53935), Icons.Rounded.PauseCircleFilled)
                        } else {
                            Triple(tr("TÜM MÜDÜRLERİ GÖREVE ÇAĞIR (İŞBAŞI)", "CALL ALL DIRECTORS TO DUTY (RESUME WORK)"), ThemePositive, Icons.Rounded.PlayCircleFilled)
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.ToggleAllManagersActiveStatus)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = buttonColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, buttonColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = buttonColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                CurrencyText(
                                    text = buttonText,
                                    color = buttonColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. CANLI C-SUITE KARAR AKIŞI
            item {
                val allRecentLogs = sortedManagers.filter { it.isHired }
                    .flatMap { it.actionLogs }
                    .sortedByDescending { it.timestampMs }
                    .take(3)

                if (allRecentLogs.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, ThemeBorder.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ThemePositive))
                                CurrencyText(
                                    text = tr("CANLI C-SUITE KARAR AKIŞI", "LIVE C-SUITE DECISION STREAM"),
                                    color = ThemeGold,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            allRecentLogs.forEach { log ->
                                val logTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestampMs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(
                                        text = "• ${log.description.trAuto(isEng)}",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CurrencyText(
                                        text = logTimeStr,
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. MANAGERS LIST (Empty State or Adaptive Grid for PC & Mobile)
            if (filteredManagers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF131D31),
                        border = BorderStroke(1.dp, Color(0xFF26354D)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            CurrencyText(
                                text = tr("Bu filtreye uygun yönetici bulunamadı.", "No directors found for this filter."),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(managerChunks, key = { chunk -> chunk.joinToString("-") { it.id } }) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunk.forEach { manager ->
                            Box(modifier = Modifier.weight(1f)) {
                                ManagerCard(
                                    manager = manager,
                                    player = p,
                                    onHire = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.handleIntent(com.example.viewmodel.GameIntent.HireManager(manager.id))
                                    },
                                    onFire = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.handleIntent(com.example.viewmodel.GameIntent.FireManager(manager.id))
                                    },
                                    onUpgrade = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.handleIntent(com.example.viewmodel.GameIntent.UpgradeManager(manager.id))
                                    },
                                    onToggleActive = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.handleIntent(com.example.viewmodel.GameIntent.ToggleManagerActive(manager.id))
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedManagerForLogs = manager
                                    },
                                    onResetDiscipline = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.handleIntent(com.example.viewmodel.GameIntent.ResetManagerDisciplineWithGems(manager.id))
                                    }
                                )
                            }
                        }

                        if (chunk.size == 1 && isWideScreen) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 6. HİYERARŞİ VE MAAŞ SKALASI KARTI
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHierarchyExpanded = !isHierarchyExpanded },
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF111A2E),
                    borderWidth = 1.dp,
                    borderColor = ThemeGold.copy(alpha = 0.4f),
                    pulsing = false
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    CurrencyText(tr("Yönetici Hiyerarşisi & Maaş Skalası", "Executive Hierarchy & Salary Scale"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    CurrencyText(if (isHierarchyExpanded) tr("Detayları Gizle", "Hide Details") else tr("Onay Silsilesi ve Maaş Formülünü Gör", "View Approval Chain & Salary Formula"), color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeGold.copy(alpha = 0.15f)
                                ) {
                                    CurrencyText(tr("Kıdem Dereceli", "Seniority Graded"), color = ThemeGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Icon(
                                    imageVector = if (isHierarchyExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Toggle Details",
                                    tint = ThemeGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isHierarchyExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)

                                CurrencyText(
                                    tr("Alttan Üste Onay Silsilesi: Operasyonel talepler (Üretim, Bakım, Ar-Ge) alttan başlayıp Lojistik, Borsa, Sözleşme ve Hazine yetkilerinden sırasıyla geçer. Riski geçen talepler Hazine tarafından bütçelendirilir:", "Bottom-Up Approval Chain: Operational requests (Production, Maintenance, R&D) initiate at base level and pass sequentially through Logistics, Exchange, Contracts, and Treasury authorities. Requests passing risk checks are budgeted by Treasury:"),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText("📈 " + tr("Enflasyon Endeksi", "Inflation Index") + ": %${String.format(java.util.Locale.US, "%.1f", state.globalInflationRate * 100)}", color = ThemeNeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            CurrencyText("🛍️ " + tr("Fiyat Endeksi", "Price Index") + ": ${String.format(java.util.Locale.US, "%.2f", marketPriceRatio)}x", color = ThemeGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        CurrencyText(
                                            tr("Maaş Motoru: Başlangıç maaşları 50.000 ₳ - 100.000 ₳ arasında olup, 5. seviyede ve enflasyon artışlarında dahi tavan maaş 400.000 ₳ ile sınırlandırılmıştır. Şirket bütçesi korunur.", "Salary Engine: Base salaries range from ₳50,000 to ₳100,000; even at Level 5 and during inflation spikes, maximum salary is capped at ₳400,000 to protect corporate budget."),
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                val mgrTreasury = managers.find { it.id == "mgr_treasury" }
                                val mgrContracts = managers.find { it.id == "mgr_contracts" }
                                val mgrBorsa = managers.find { it.id == "mgr_borsa" }
                                val mgrLogistics = managers.find { it.id == "mgr_logistics" }
                                val mgrHr = managers.find { it.id == "mgr_hr" }
                                val opsManagers = managers.filter { it.id in listOf("mgr_rd", "mgr_prod", "mgr_maintenance") }
                                val opsMinSalary = opsManagers.minOfOrNull { it.dailySalary } ?: 10000L
                                val opsMaxSalary = opsManagers.maxOfOrNull { it.dailySalary } ?: 11000L
                                val opsSalaryText = if (opsMinSalary == opsMaxSalary) {
                                    formatCurrency(opsMinSalary, isEng)
                                } else {
                                    "${formatCurrency(opsMinSalary, isEng)} - ${formatCurrency(opsMaxSalary, isEng)}"
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("👑 #1 " + tr("Hazine & Makroekonomi Müdürü", "Treasury & Macroeconomics Director"), color = ThemeGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        CurrencyText(formatCurrency(mgrTreasury?.dailySalary ?: 20000L, isEng), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("🥈 #2 " + tr("Vadeli Sözleşme & Tedarik Müdürü", "Forward Contracts & Procurement Director"), color = ThemeNeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        CurrencyText(formatCurrency(mgrContracts?.dailySalary ?: 16000L, isEng), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("🥉 #3 " + tr("Borsa & Yatırım Analisti", "Equities & Investment Analyst"), color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        CurrencyText(formatCurrency(mgrBorsa?.dailySalary ?: 15000L, isEng), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("🚚 #4 " + tr("Lojistik & Pazar Satış Müdürü", "Logistics & Global Sales Director"), color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        CurrencyText(formatCurrency(mgrLogistics?.dailySalary ?: 13000L, isEng), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("👥 #5 " + tr("İnsan Kaynakları Müdürü", "Human Resources Director"), color = Color(0xFFCE93D8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        CurrencyText(formatCurrency(mgrHr?.dailySalary ?: 12000L, isEng), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        CurrencyText("🛠️ #5 " + tr("Operasyon (Üretim, Ar-Ge, Bakım)", "Operations (Production, R&D, Maintenance)"), color = Color.LightGray, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                                        CurrencyText(opsSalaryText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedManagerForLogs?.let { manager ->
            ManagerLogsDialog(
                manager = manager,
                onDismiss = { selectedManagerForLogs = null }
            )
        }
    }
}

@Composable
fun ManagerLogsDialog(
    manager: com.example.data.CompanyManager,
    onDismiss: () -> Unit
) {
    val isEng = isEnglishLanguage()
    val localizedTitle = if (isEng) manager.title.trAuto() else (if (manager.titleRes != 0) androidx.compose.ui.res.stringResource(id = manager.titleRes) else manager.title)
    val titleName = if (manager.name.isNotBlank()) manager.name else localizedTitle
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 520.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141A29),
            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CurrencyText(
                            text = "$titleName " + tr("İşlem Geçmişi", "Transaction History"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CurrencyText(
                            text = localizedTitle,
                            color = ThemeGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = tr("Kapat", "Close"), tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                if (manager.actionLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HistoryToggleOff,
                                contentDescription = null,
                                tint = ThemeNeonCyan.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CurrencyText(
                                text = tr("Henüz bu pozisyona ait kaydedilmiş bir işlem veya karar kaydı bulunmamaktadır.", "No transaction or decision logs recorded for this position yet."),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(manager.actionLogs, key = { it.id }) { log ->
                            val impactColor = if (log.financialImpact >= 0) com.example.ui.theme.ThemePositive else com.example.ui.theme.ThemeNegative
                            val impactSign = if (log.financialImpact >= 0) "+" else "-"
                            
                            val timeStr = remember(log.timestampMs, isEng) {
                                val locale = if (isEng) java.util.Locale.US else java.util.Locale("tr")
                                java.text.SimpleDateFormat("dd MMM HH:mm:ss", locale).format(java.util.Date(log.timestampMs))
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1A2130),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText(timeStr, color = Color.Gray, fontSize = 10.sp)
                                        CurrencyText(
                                            text = "$impactSign ${formatCredit(kotlin.math.abs(log.financialImpact))}",
                                            color = impactColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    CurrencyText(
                                        text = log.description.trAuto(isEng),
                                        color = Color.LightGray,
                                        fontSize = 12.sp
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

@Composable
fun ManagerCard(
    manager: CompanyManager,
    player: com.example.data.PlayerEntity,
    onHire: () -> Unit,
    onFire: () -> Unit,
    onUpgrade: () -> Unit,
    onToggleActive: () -> Unit,
    onClick: () -> Unit,
    onResetDiscipline: () -> Unit
) {
    val isEng = isEnglishLanguage()
    var isDetailsExpanded by remember { mutableStateOf(false) }

    val specialtyIcon = when (manager.specialty) {
        "production" -> Icons.Rounded.Factory
        "logistics" -> Icons.Rounded.LocalShipping
        "maintenance" -> Icons.Rounded.Build
        "contracts" -> Icons.Rounded.Description
        "rd" -> Icons.Rounded.Science
        "borsa" -> Icons.Rounded.ShowChart
        "hr" -> Icons.Rounded.Group
        else -> Icons.Rounded.AccountBalance
    }

    val localizedTitle = if (isEng) manager.title.trAuto() else (if (manager.titleRes != 0) androidx.compose.ui.res.stringResource(id = manager.titleRes) else manager.title)
    val localizedDesc = if (isEng) manager.description.trAuto() else (if (manager.descriptionRes != 0) androidx.compose.ui.res.stringResource(id = manager.descriptionRes) else manager.description)

    val (hierarchyTag, hierarchyColor) = when (manager.id) {
        "mgr_treasury" -> ("👑 " + tr("Hiyerarşi #1 (Finans)", "Hierarchy #1 (Finance)")) to ThemeGold
        "mgr_contracts" -> ("🥈 " + tr("Hiyerarşi #2 (Tedarik)", "Hierarchy #2 (Procurement)")) to ThemeNeonCyan
        "mgr_borsa" -> ("🥉 " + tr("Hiyerarşi #3 (Borsa)", "Hierarchy #3 (Exchange)")) to Color(0xFFFFB74D)
        "mgr_logistics" -> ("🚚 " + tr("Hiyerarşi #4 (Lojistik)", "Hierarchy #4 (Logistics)")) to Color(0xFF81C784)
        "mgr_hr" -> ("👥 " + tr("Hiyerarşi #5 (İK)", "Hierarchy #5 (HR)")) to Color(0xFFCE93D8)
        else -> ("🛠️ " + tr("Hiyerarşi #5 (Operasyon)", "Hierarchy #5 (Operations)")) to Color(0xFF64B5F6)
    }

    val isHired = manager.isHired
    val containerColor = if (isHired) Color(0xFF131C2E) else Color(0xFF0F172A)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = if (isHired) BorderStroke(1.2.dp, hierarchyColor.copy(alpha = 0.7f)) else BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Icon + Name/Title + Quick Status / Level
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
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHired) hierarchyColor.copy(alpha = 0.15f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isHired) hierarchyColor.copy(alpha = 0.8f) else Color(0xFF334155)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isHired) specialtyIcon else Icons.Rounded.PersonOutline,
                                contentDescription = null,
                                tint = if (isHired) hierarchyColor else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isHired && manager.name.isNotBlank()) {
                                CurrencyText(
                                    manager.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                CurrencyText(
                                    tr("POZİSYON BOŞ", "POSITION VACANT"),
                                    color = ThemeGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CurrencyText(
                                localizedTitle,
                                color = if (isHired) hierarchyColor else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isHired) {
                    // Level stars + Quick Active Switch directly on Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1A2130),
                            border = BorderStroke(0.8.dp, Color(0xFF2E3E5C))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                repeat(manager.level.coerceIn(1, 5)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(11.dp))
                                }
                            }
                        }

                        // Compact Quick Active/Duty Switch
                        Surface(
                            onClick = onToggleActive,
                            shape = RoundedCornerShape(6.dp),
                            color = if (manager.isActive) ThemePositive.copy(alpha = 0.15f) else Color(0xFFE53935).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (manager.isActive) ThemePositive.copy(alpha = 0.6f) else Color(0xFFE53935).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (manager.isActive) ThemePositive else Color(0xFFE53935))
                                )
                                CurrencyText(
                                    text = if (manager.isActive) tr("GÖREVDE", "ACTIVE") else tr("İZİNLİ", "LEAVE"),
                                    color = if (manager.isActive) ThemePositive else Color(0xFFE53935),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        CurrencyText(
                            text = tr("Aday Bekleniyor", "Awaiting Candidate"),
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Financial & Effect Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                    CurrencyText(
                        if (isHired) tr("Günlük Maaş (Nakit)", "Daily Salary (Cash)") else tr("Başlangıç Maaşı", "Starting Salary"),
                        color = Color.Gray,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CurrencyText(
                        formatCurrency(manager.dailySalary, isEnglishLanguage()),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f, fill = false)) {
                    if (!isHired) {
                        CurrencyText(
                            tr("Transfer Bedeli", "Hiring Fee"),
                            color = ThemeNeonCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                "${manager.hireGemCost} 💎",
                                color = ThemeNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val effectText = when (manager.specialty) {
                            "production" -> tr("Üretim Hızı", "Production Speed") + " +%${manager.level * 25}"
                            "logistics" -> tr("Nakliye İndirimi", "Logistics Discount") + " %${(manager.level * 15).coerceAtMost(75)}"
                            "maintenance" -> tr("Bakım İndirimi", "Maintenance Discount") + " %${(manager.level * 15).coerceAtMost(75)}"
                            "treasury" -> tr("Nakit Rezerv Koruma", "Cash Reserve Protection") + " %${20 + (manager.level * 10)}"
                            "rd" -> tr("Ar-Ge Hızı", "R&D Speed") + " +%${manager.level * 20}"
                            "contracts" -> tr("B2B İhale Verimi", "B2B Tender Efficiency") + " +%${manager.level * 10}"
                            "hr" -> tr("Oto-Terfi & Koçluk", "Auto-Promote & Coach")
                            else -> tr("Oto-Borsa Alımı", "Auto-Stock Buying")
                        }
                        CurrencyText(
                            hierarchyTag,
                            color = hierarchyColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CurrencyText(
                            effectText,
                            color = ThemeGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live Recent Action Badge (Front of Card)
            if (isHired && manager.actionLogs.isNotEmpty()) {
                val latestLog = manager.actionLogs.first()
                val logTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(latestLog.timestampMs))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0B1322),
                    border = BorderStroke(0.8.dp, hierarchyColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(hierarchyColor)
                        )
                        CurrencyText(
                            text = "⚡ ${latestLog.description.trAuto(isEng)}",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        CurrencyText(
                            text = logTimeStr,
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }

            // PRIMARY ACTIONS ROW (Directly on the card!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHired) {
                    val hireGemCost = manager.hireGemCost
                    val hasEnoughGems = player.gems >= hireGemCost

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isDetailsExpanded = !isDetailsExpanded }
                        ) {
                            Icon(
                                imageVector = if (isDetailsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                if (isDetailsExpanded) tr("Detayı Gizle", "Hide") else tr("Görev Tanımı", "Job Role"),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onHire,
                            enabled = hasEnoughGems,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeGold,
                                contentColor = Color(0xFF1A1300),
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (hasEnoughGems) Color(0xFF1A1300) else ThemeNeonCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(tr("İşe Al", "Hire") + " ($hireGemCost 💎)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                } else {
                    // Quick Action Strip: Logs Button, Upgrade Button, and Details Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // View Logs Button
                            Surface(
                                onClick = onClick,
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.History, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(13.dp))
                                    CurrencyText(
                                        tr("Loglar", "Logs") + " (${manager.actionLogs.size})",
                                        color = ThemeNeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Details toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isDetailsExpanded = !isDetailsExpanded }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDetailsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                CurrencyText(
                                    if (isDetailsExpanded) tr("Gizle", "Hide") else tr("Detay", "Details"),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Upgrade (Promote) Button
                        if (manager.level < 5) {
                            val upgradeCost = manager.dailySalary * 5 * manager.level
                            val canAffordUpgrade = player.money >= upgradeCost
                            Button(
                                onClick = onUpgrade,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAffordUpgrade) ThemeGold else Color(0xFF1E293B),
                                    contentColor = if (canAffordUpgrade) Color(0xFF1A1300) else Color.Gray
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                CurrencyText(tr("Terfi", "Promote") + " (${formatCredit(upgradeCost)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f))
                            ) {
                                CurrencyText(
                                    tr("Maks Seviye (Lvl 5)", "Max Level (Lvl 5)"),
                                    color = ThemeGold,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Detailed View
            AnimatedVisibility(visible = isDetailsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                    CurrencyText(localizedDesc, color = Color.LightGray, fontSize = 11.5.sp)

                    if (isHired) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                tr("Pozisyonu Sonlandır:", "Terminate Position:"),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            OutlinedButton(
                                onClick = onFire,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.PersonRemove, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(tr("İşten Çıkar (Tazminatsız)", "Fire Manager"), color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
