package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompanyManager
import com.example.data.automation.AutopilotEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlin.math.abs

@Composable
fun HrScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    viewModel: GameViewModel
) {
    val haptic = LocalHapticFeedback.current
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
        val hired = managers.filter { it.isHired }
        if (hired.isNotEmpty()) (hired.sumOf { it.efficiency.toDouble() } / hired.size).toFloat() else 0f
    } else 0f
    val treasuryReserve = viewModel.getTreasuryCashReserve()

    var guidedStep by remember { mutableStateOf(HrGuidedStep.OVERVIEW) }
    var selectedManagerForLogs by remember { mutableStateOf<CompanyManager?>(null) }
    var showEfficiencyDialog by remember { mutableStateOf(false) }

    val allActionLogs = remember(managers) {
        managers.filter { it.isHired }
            .flatMap { it.actionLogs }
            .sortedByDescending { it.timestampMs }
    }

    val marketPriceRatio = remember(marketPrices) {
        if (marketPrices.isNotEmpty()) {
            val totalRatios = com.example.data.Product.values().map { prod ->
                val curPrice = marketPrices.find { it.itemId == prod.id }?.price ?: prod.basePrice
                (curPrice.toFloat() / prod.basePrice.toFloat())
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
            // 🎯 ADIM ADIM YÖNLENDİREN C-SUITE HUD MENÜSÜ
            item {
                HrGuidedStepBar(
                    currentStep = guidedStep,
                    onStepSelected = { guidedStep = it },
                    hiredCount = hiredCount,
                    totalCount = managers.size,
                    averageEfficiency = averageEfficiency,
                    recentLogsCount = allActionLogs.size
                )
            }

            when (guidedStep) {
                // ================= 1. C-SUITE KARARGAH & GENEL DURUM =================
                HrGuidedStep.OVERVIEW -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // C-Suite Holding Hero Card
                            HrExecutiveHeroCard(
                                hiredCount = hiredCount,
                                totalCount = managers.size,
                                totalDailySalaries = totalDailySalaries,
                                averageEfficiency = averageEfficiency,
                                playerGems = p.gems,
                                treasuryReserve = treasuryReserve,
                                onShowEfficiencyDialog = { showEfficiencyDialog = true }
                            )

                            // Sıradaki İK Eyleminiz Rehber Kartı
                            HrNextActionGuidanceCard(
                                managers = managers,
                                playerGems = p.gems,
                                playerMoney = p.money,
                                onNavigateToStaffing = { guidedStep = HrGuidedStep.STAFFING }
                            )

                            // Canlı C-Suite Karar Akışı Ticker
                            val recentLogs = allActionLogs.take(3)
                            if (recentLogs.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0C1628),
                                    border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34D399)))
                                                CurrencyText(
                                                    text = tr("CANLI C-SUITE KARAR AKIŞI", "LIVE C-SUITE DECISION STREAM"),
                                                    color = ThemeGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = RobotoMonoFontFamily,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.clickable { guidedStep = HrGuidedStep.AUDIT_LOGS }
                                            ) {
                                                CurrencyText(tr("Tümünü Gör ➔", "View All ➔"), fontSize = 8.sp, color = ThemeNeonCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }

                                        recentLogs.forEach { log ->
                                            val logTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestampMs))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CurrencyText(
                                                    text = "⚡ ${log.description.trAuto(isEng)}",
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                CurrencyText(text = logTimeStr, color = Color(0xFF64748B), fontSize = 8.5.sp, fontFamily = RobotoMonoFontFamily)
                                            }
                                        }
                                    }
                                }
                            }

                            // Holding Finansal Güvence & Bordro Sağlığı
                            HrHoldingFinancialPulseCard(
                                playerMoney = p.money,
                                totalDailySalaries = totalDailySalaries,
                                treasuryReserve = treasuryReserve
                            )

                            // C-Suite İnteraktif İcra Kurulu Masası (Interactive Boardroom)
                            HrWarRoomBoardTable(
                                managers = sortedManagers,
                                onSelectManager = { mgr -> selectedManagerForLogs = mgr },
                                onQuickHire = { mgr ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.handleIntent(GameIntent.HireManager(mgr.id))
                                }
                            )

                            // Stratejik Akıllı Direktifler & Emir Masası (Autopilot Directives)
                            HrActiveDirectivesPanel(
                                viewModel = viewModel
                            )
                        }
                    }
                }

                // ================= 2. MÜDÜRLER VE KADRO ATAMA =================
                HrGuidedStep.STAFFING -> {
                    // Filters & Search Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                placeholder = {
                                    CurrencyText(
                                        tr("Yönetici veya departman ara...", "Search director or department..."),
                                        color = Color.Gray,
                                        fontSize = 11.sp
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
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Filter Chips Row
                            val filterList = listOf(
                                tr("Tümü", "All") to sortedManagers.size,
                                tr("İşe Alınanlar", "Hired") to hiredCount,
                                tr("Açık Kadro", "Vacant") to (sortedManagers.size - hiredCount),
                                tr("Finans & Borsa", "Finance") to sortedManagers.count { it.id in listOf("mgr_treasury", "mgr_borsa") },
                                tr("Tedarik & Satış", "Supply") to sortedManagers.count { it.id in listOf("mgr_contracts", "mgr_logistics") },
                                tr("Operasyon", "Operations") to sortedManagers.count { it.id in listOf("mgr_prod", "mgr_maintenance", "mgr_rd", "mgr_hr") }
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
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ThemeGold.copy(alpha = 0.22f) else Color(0xFF101B2E),
                                        border = BorderStroke(1.dp, if (isSelected) ThemeGold else Color(0xFF1E2D4A))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            CurrencyText(
                                                text = label,
                                                color = if (isSelected) ThemeGold else Color.LightGray,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) ThemeGold else Color(0xFF1E293B)
                                            ) {
                                                CurrencyText(
                                                    text = "$count",
                                                    color = if (isSelected) Color(0xFF1A1300) else Color.Gray,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Holiday / Duty Toggle Button
                            if (hiredCount > 0) {
                                val anyActive = managers.filter { it.isHired }.any { it.isActive }
                                val (buttonText, buttonColor, icon) = if (anyActive) {
                                    Triple(tr("TÜM MÜDÜRLERİ İZNE ÇIKAR (RESMİ TATİL)", "GRANT ALL DIRECTORS LEAVE (PUBLIC HOLIDAY)"), Color(0xFFEF4444), Icons.Rounded.PauseCircleFilled)
                                } else {
                                    Triple(tr("TÜM MÜDÜRLERİ GÖREVE ÇAĞIR (İŞBAŞI)", "CALL ALL DIRECTORS TO DUTY (RESUME WORK)"), Color(0xFF10B981), Icons.Rounded.PlayCircleFilled)
                                }

                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.handleIntent(GameIntent.ToggleAllManagersActiveStatus)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = buttonColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, buttonColor.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = null, tint = buttonColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CurrencyText(
                                            text = buttonText,
                                            color = buttonColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Managers List
                    if (filteredManagers.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF101B2E),
                                border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CurrencyText(
                                        text = tr("Bu filtreye uygun yönetici bulunamadı.", "No directors found matching this filter."),
                                        color = Color.Gray,
                                        fontSize = 11.sp,
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
                                        ExecutiveDirectorCard(
                                            manager = manager,
                                            player = p,
                                            onHire = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.handleIntent(GameIntent.HireManager(manager.id))
                                            },
                                            onFire = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.handleIntent(GameIntent.FireManager(manager.id))
                                            },
                                            onUpgrade = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.handleIntent(GameIntent.UpgradeManager(manager.id))
                                            },
                                            onToggleActive = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.handleIntent(GameIntent.ToggleManagerActive(manager.id))
                                            },
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedManagerForLogs = manager
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
                }

                // ================= 3. HİYERARŞİ & ONAY SİLSİLESİ =================
                HrGuidedStep.HIERARCHY -> {
                    item {
                        HrOrgChartDiagram(
                            managers = managers,
                            inflationRate = state.globalInflationRate,
                            marketPriceRatio = marketPriceRatio
                        )
                    }
                }

                // ================= 4. KARAR & DENETİM KAYITLARI =================
                HrGuidedStep.AUDIT_LOGS -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0B1424),
                            border = BorderStroke(1.2.dp, Color(0xFF1E2D4A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.HistoryEdu, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        CurrencyText(
                                            text = tr("C-SUITE KARAR & DENETİM SİCİLİ", "C-SUITE DECISION & AUDIT LOGS"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) {
                                        CurrencyText(
                                            text = "${allActionLogs.size} " + tr("Kayıt", "Records"),
                                            fontSize = 8.5.sp,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                CurrencyText(
                                    text = tr("Holding müdürlerinin otomatik piyasa alımları, sözleşme ihaleleri, bakım ve nakit müdahaleleri anlık olarak denetlenir.", "Automatic market purchases, contracts, maintenance, and liquidity interventions are audited live."),
                                    fontSize = 8.5.sp,
                                    color = Color.Gray
                                )

                                if (allActionLogs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CurrencyText(
                                            text = tr("Henüz kaydedilmiş bir C-Suite müdahalesi bulunmuyor.", "No recorded C-Suite manager interventions yet."),
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        allActionLogs.take(30).forEach { log ->
                                            val impactColor = if (log.financialImpact >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
                                            val impactSign = if (log.financialImpact >= 0) "+" else "-"
                                            val timeStr = remember(log.timestampMs) {
                                                java.text.SimpleDateFormat("dd MMM HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestampMs))
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF101B2E),
                                                border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        CurrencyText(
                                                            text = log.description.trAuto(isEng),
                                                            fontSize = 9.5.sp,
                                                            color = Color.White
                                                        )
                                                        CurrencyText(text = timeStr, fontSize = 7.5.sp, color = Color.Gray)
                                                    }

                                                    CurrencyText(
                                                        text = "$impactSign${formatCredit(abs(log.financialImpact))}",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = impactColor
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

                // ================= 5. YÖNETİM VERİMİ & AKADEMİ =================
                HrGuidedStep.EFFICIENCY -> {
                    item {
                        HrEfficiencyAcademyCard(
                            averageEfficiency = averageEfficiency,
                            managers = managers
                        )
                    }
                }
            }
        }

        // Tıklanan müdürün detaylı sicil ve karne dosyası
        selectedManagerForLogs?.let { manager ->
            ExecutiveDossierDialog(
                manager = manager,
                player = p,
                onDismiss = { selectedManagerForLogs = null },
                onHire = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.handleIntent(GameIntent.HireManager(manager.id))
                    selectedManagerForLogs = null
                },
                onFire = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.handleIntent(GameIntent.FireManager(manager.id))
                    selectedManagerForLogs = null
                },
                onUpgrade = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.handleIntent(GameIntent.UpgradeManager(manager.id))
                },
                onToggleActive = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.handleIntent(GameIntent.ToggleManagerActive(manager.id))
                }
            )
        }

        // Yönetim Verimi Hesaplama Bilgilendirme Diyaloğu
        if (showEfficiencyDialog) {
            AlertDialog(
                onDismissRequest = { showEfficiencyDialog = false },
                confirmButton = {
                    TextButton(onClick = { showEfficiencyDialog = false }) {
                        CurrencyText(tr("Anladım", "Understood"), color = ThemeNeonCyan, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(tr("Yönetim Verimi Hesabı", "Management Efficiency Calculation"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CurrencyText(
                            tr(
                                "Yönetim Verimi, şirket bünyesinde çalışan tüm C-Suite müdürlerin operasyonel gücünü temsil eder:",
                                "Management Efficiency represents the combined operational performance of all C-Suite managers:"
                            ),
                            fontSize = 11.5.sp,
                            color = Color.LightGray
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        CurrencyText(
                            tr(
                                "1. Bireysel Müdür Verimi (%0 - %100):\nHer müdürün kendi uzmanlık alanındaki performans oranıdır.",
                                "1. Individual Manager Efficiency (%0 - %100):\nEach manager's performance rating in their specialized field."
                            ),
                            fontSize = 11.sp,
                            color = ThemeNeonCyan
                        )
                        CurrencyText(
                            tr(
                                "2. Ortalama Hesaplama:\nAktif çalışan tüm müdürlerin verimlilik puanlarının ortalaması holding verimini belirler.",
                                "2. Average Calculation:\nThe overall efficiency is computed as the average score of all active hired managers."
                            ),
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        CurrencyText(
                            tr(
                                "3. Seviye Çarpanları (Terfi Primleri):\nMüdürler terfi ettirildikçe (Lvl 1 ➔ 5) fabrika üretim hızları, lojistik indirimleri ve pazar kazançları katlanır.",
                                "3. Promotion Multipliers:\nAs managers level up (Lvl 1 ➔ 5), production speeds, logistics discounts, and trade profits compound."
                            ),
                            fontSize = 11.sp,
                            color = ThemeGold
                        )
                    }
                },
                containerColor = Color(0xFF0F1B2E),
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }
    }
}
