package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurrencyText
import com.example.ui.components.formatCredit
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class GrowthPoint(
    val dayLabel: String,
    val netWorth: Double,
    val cashBalance: Double,
    val totalAssets: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class GrowthScreenTab {
    GROWTH_TREND,
    ASSET_ALLOCATION,
    FINANCIAL_HEALTH
}

data class AssetSlice(
    val id: String,
    val title: String,
    val subtitle: String,
    val value: Double,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val percentage: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGrowthChartScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val theme = LocalAppThemeOption.current
    val haptic = LocalHapticFeedback.current
    val player = uiState.playerState.player ?: return
    val isEnglish = isEnglishLanguage()

    var activeTab by remember { mutableStateOf(GrowthScreenTab.GROWTH_TREND) }
    var selectedTimeframe by remember { mutableStateOf("7G") }
    var selectedMetric by remember { mutableStateOf("NET_DEGER") }

    // Core valuations
    val currentNetWorth = uiState.netWorth.toDouble().coerceAtLeast(0.0)
    val cashVal = player.money.toDouble().coerceAtLeast(0.0)
    val depositVal = (player.depositBalance + player.lockedDepositBalance).toDouble().coerceAtLeast(0.0)
    val facilityVal = uiState.facilityValuation.toDouble().coerceAtLeast(0.0)
    val inventoryVal = uiState.inventoryValuation.toDouble().coerceAtLeast(0.0)
    val loanDebt = player.loanAmount.toDouble().coerceAtLeast(0.0)
    val currentAssets = (cashVal + depositVal + facilityVal + inventoryVal).coerceAtLeast(0.0)

    val rawHistory = uiState.growthHistory

    // Compute Historical Points
    val points = remember(rawHistory, currentNetWorth, currentAssets, cashVal, selectedTimeframe, isEnglish) {
        val liveDayLabel = if (rawHistory.size <= 1) {
            if (isEnglish) "Day 1 (Now)" else "1. Gün (Bugün)"
        } else {
            if (isEnglish) "Today" else "Bugün"
        }

        val livePoint = GrowthPoint(
            dayLabel = liveDayLabel,
            netWorth = currentNetWorth,
            cashBalance = cashVal,
            totalAssets = currentAssets
        )

        if (rawHistory.isEmpty()) {
            val startNetWorth = if (currentNetWorth > 0) (currentNetWorth * 0.92) else 100000.0
            val startCash = cashVal.coerceAtLeast(10000.0)
            val startAssets = currentAssets.coerceAtLeast(10000.0)

            listOf(
                GrowthPoint(
                    dayLabel = if (isEnglish) "Start" else "Başlangıç",
                    netWorth = startNetWorth,
                    cashBalance = startCash,
                    totalAssets = startAssets
                ),
                livePoint
            )
        } else if (rawHistory.size == 1) {
            val firstDto = rawHistory.first()
            val startNetWorth = if (firstDto.netWorth > 0 && firstDto.netWorth != currentNetWorth) firstDto.netWorth else (currentNetWorth * 0.92)
            val startCash = if (firstDto.cashBalance > 0 && firstDto.cashBalance != cashVal) firstDto.cashBalance else cashVal
            val startAssets = if (firstDto.totalAssets > 0 && firstDto.totalAssets != currentAssets) firstDto.totalAssets else currentAssets

            listOf(
                GrowthPoint(
                    dayLabel = if (isEnglish) "Start" else "Başlangıç",
                    netWorth = startNetWorth,
                    cashBalance = startCash,
                    totalAssets = startAssets
                ),
                livePoint
            )
        } else {
            val limit = when (selectedTimeframe) {
                "7G" -> 7
                "30G" -> 30
                else -> rawHistory.size
            }
            val sliced = rawHistory.takeLast(limit)
            val mappedList = sliced.mapIndexed { idx, dto ->
                val label = dto.dayLabel.ifBlank {
                    if (isEnglish) "Day ${idx + 1}" else "${idx + 1}. Gün"
                }
                GrowthPoint(
                    dayLabel = label,
                    netWorth = dto.netWorth,
                    cashBalance = dto.cashBalance,
                    totalAssets = dto.totalAssets,
                    timestampMs = dto.timestampMs
                )
            }.toMutableList()

            if (mappedList.isNotEmpty()) {
                mappedList[mappedList.size - 1] = livePoint
            }
            mappedList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurrencyText(
                                text = tr("Büyüme & Varlık Analizi", "Growth & Asset Analysis"),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = theme.textPrimaryColor
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ThemePositive)
                            )
                        }
                        CurrencyText(
                            text = tr("Canlı Portföy, Trend & Hazine Raporu", "Live Portfolio, Trend & Treasury Report"),
                            fontSize = 11.sp,
                            color = theme.textSecondaryColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Geri", "Back"),
                            tint = theme.textPrimaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.surfaceColor)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Master Navigation Tabs
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.surfaceColor,
                border = BorderStroke(0.5.dp, theme.borderColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPillButton(
                        title = tr("Büyüme Eğrisi", "Growth Curve"),
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        isSelected = activeTab == GrowthScreenTab.GROWTH_TREND,
                        tint = ThemeGold,
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeTab = GrowthScreenTab.GROWTH_TREND
                        }
                    )
                    TabPillButton(
                        title = tr("Varlık Portföyü", "Asset Portfolio"),
                        icon = Icons.Rounded.PieChart,
                        isSelected = activeTab == GrowthScreenTab.ASSET_ALLOCATION,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeTab = GrowthScreenTab.ASSET_ALLOCATION
                        }
                    )
                    TabPillButton(
                        title = tr("Hazine Sağlığı", "Treasury Health"),
                        icon = Icons.Rounded.Security,
                        isSelected = activeTab == GrowthScreenTab.FINANCIAL_HEALTH,
                        tint = ThemePositive,
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeTab = GrowthScreenTab.FINANCIAL_HEALTH
                        }
                    )
                }
            }

            // Tab Content
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                },
                modifier = Modifier.fillMaxSize(),
                label = "GrowthScreenTabTransition"
            ) { tab ->
                when (tab) {
                    GrowthScreenTab.GROWTH_TREND -> {
                        GrowthTrendView(
                            points = points,
                            selectedTimeframe = selectedTimeframe,
                            onTimeframeChange = { selectedTimeframe = it },
                            selectedMetric = selectedMetric,
                            onMetricChange = { selectedMetric = it },
                            theme = theme
                        )
                    }
                    GrowthScreenTab.ASSET_ALLOCATION -> {
                        AssetAllocationView(
                            totalAssets = currentAssets,
                            cash = cashVal,
                            deposit = depositVal,
                            facilities = facilityVal,
                            inventory = inventoryVal,
                            loanDebt = loanDebt,
                            netWorth = currentNetWorth,
                            theme = theme
                        )
                    }
                    GrowthScreenTab.FINANCIAL_HEALTH -> {
                        FinancialHealthView(
                            netWorth = currentNetWorth,
                            totalAssets = currentAssets,
                            cash = cashVal,
                            deposit = depositVal,
                            loanDebt = loanDebt,
                            dailyIncome = player.dailyIncome.toDouble(),
                            dailyExpense = player.dailyExpense.toDouble(),
                            theme = theme
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 1: BÜYÜME EĞRİSİ (GROWTH TREND VIEW)
// -----------------------------------------------------------------------------------------
@Composable
private fun GrowthTrendView(
    points: List<GrowthPoint>,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit,
    selectedMetric: String,
    onMetricChange: (String) -> Unit,
    theme: AppThemeOption
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    val startValue = points.firstOrNull()?.let { pt ->
        when (selectedMetric) {
            "NET_DEGER" -> pt.netWorth
            "NAKIT" -> pt.cashBalance
            else -> pt.totalAssets
        }
    } ?: 0.0

    val endValue = points.lastOrNull()?.let { pt ->
        when (selectedMetric) {
            "NET_DEGER" -> pt.netWorth
            "NAKIT" -> pt.cashBalance
            else -> pt.totalAssets
        }
    } ?: 0.0

    val growthDelta = endValue - startValue
    val growthPercent = if (startValue > 0) (growthDelta / startValue) * 100.0 else 0.0
    val daysCount = points.size.coerceAtLeast(1)
    val dailyAvgVelocity = growthDelta / daysCount

    var activeScrubIndex by remember { mutableStateOf<Int?>(null) }
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(selectedTimeframe, selectedMetric) {
        transitionAnim.snapTo(0f)
        transitionAnim.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    val chartColor = when (selectedMetric) {
        "NET_DEGER" -> ThemeGold
        "NAKIT" -> ThemePositive
        else -> ThemeNeonCyan
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Momentum Summary Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, chartColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(
                                text = when (selectedMetric) {
                                    "NET_DEGER" -> tr("GÜNCEL ŞİRKET DEĞERİ", "CURRENT NET WORTH")
                                    "NAKIT" -> tr("TOPLAM LİKİT NAKİT", "TOTAL LIQUID CASH")
                                    else -> tr("TOPLAM BRÜT VARLIK", "TOTAL GROSS ASSETS")
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textSecondaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = formatCredit(endValue.toLong()),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                color = chartColor
                            )
                        }

                        // Growth Percentage Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (growthPercent >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, (if (growthPercent >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (growthPercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (growthPercent >= 0) ThemePositive else ThemeNegative,
                                    modifier = Modifier.size(16.dp)
                                )
                                CurrencyText(
                                    text = "${if (growthPercent >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f%%", growthPercent)}",
                                    fontWeight = FontWeight.Black,
                                    color = if (growthPercent >= 0) ThemePositive else ThemeNegative,
                                    fontSize = 13.sp,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = theme.borderColor.copy(alpha = 0.4f), thickness = 0.8.dp)

                    // Secondary Sub-stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(
                                text = tr("Dönemsel Net Değişim", "Period Net Delta"),
                                fontSize = 9.sp,
                                color = theme.textSecondaryColor
                            )
                            val sign = if (growthDelta >= 0) "+" else ""
                            CurrencyText(
                                text = "$sign${formatCredit(growthDelta.toLong())}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (growthDelta >= 0) ThemePositive else ThemeNegative,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            CurrencyText(
                                text = tr("Günlük Büyüme İvmesi", "Daily Growth Velocity"),
                                fontSize = 9.sp,
                                color = theme.textSecondaryColor
                            )
                            val velSign = if (dailyAvgVelocity >= 0) "+" else ""
                            CurrencyText(
                                text = "$velSign${formatCredit(dailyAvgVelocity.toLong())} / ${tr("gün", "day")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dailyAvgVelocity >= 0) ThemePositive else ThemeNegative,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }
        }

        // Filter Controls: Timeframes & Metrics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Timeframe Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "7G" to tr("7 Gün", "7 Days"),
                        "30G" to tr("30 Gün", "30 Days"),
                        "ALL" to tr("Tüm Geçmiş", "All Time")
                    ).forEach { (key, label) ->
                        val isSelected = selectedTimeframe == key
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTimeframeChange(key)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) ThemeGold else theme.surfaceVariantColor,
                            border = BorderStroke(1.dp, if (isSelected) ThemeGold else theme.borderColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            CurrencyText(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else theme.textSecondaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                    }
                }

                // Metric Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "NET_DEGER" to (tr("Net Değer", "Net Worth") to ThemeGold),
                        "NAKIT" to (tr("Nakit", "Cash") to ThemePositive),
                        "VARLIK" to (tr("Brüt Varlık", "Gross Assets") to ThemeNeonCyan)
                    ).forEach { (key, pair) ->
                        val (label, tint) = pair
                        val isSelected = selectedMetric == key
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMetricChange(key)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) tint.copy(alpha = 0.2f) else theme.surfaceVariantColor,
                            border = BorderStroke(1.dp, if (isSelected) tint else theme.borderColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) tint else theme.textSecondaryColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                CurrencyText(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) tint else theme.textSecondaryColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Spline Line Chart Canvas
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header & Active Inspection Tooltip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("ZAMAN SERİSİ TREND EĞRİSİ", "TIME SERIES TREND CURVE"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textSecondaryColor,
                            letterSpacing = 0.5.sp
                        )

                        activeScrubIndex?.let { idx ->
                            val pt = points.getOrNull(idx)
                            if (pt != null) {
                                val valToShow = when (selectedMetric) {
                                    "NET_DEGER" -> pt.netWorth
                                    "NAKIT" -> pt.cashBalance
                                    else -> pt.totalAssets
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = chartColor.copy(alpha = 0.15f),
                                    border = BorderStroke(0.6.dp, chartColor)
                                ) {
                                    CurrencyText(
                                        text = "${pt.dayLabel}: ${formatCredit(valToShow.toLong())}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chartColor,
                                        fontFamily = RobotoMonoFontFamily,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        } ?: CurrencyText(
                            text = tr("Dokunarak / Sürükleyerek inceleyin", "Touch / Drag to inspect"),
                            fontSize = 9.sp,
                            color = theme.textSecondaryColor
                        )
                    }

                    val values = points.map {
                        when (selectedMetric) {
                            "NET_DEGER" -> it.netWorth
                            "NAKIT" -> it.cashBalance
                            else -> it.totalAssets
                        }
                    }
                    val rawMin = values.minOrNull() ?: 0.0
                    val rawMax = values.maxOrNull() ?: 1.0
                    val minVal = (rawMin * 0.95).coerceAtLeast(0.0)
                    val maxVal = (rawMax * 1.05).coerceAtLeast(minVal + 10.0)
                    val range = (maxVal - minVal).coerceAtLeast(1.0)
                    val animProgress = transitionAnim.value
                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x / stepX)).roundToInt().coerceIn(0, points.size - 1)
                                    activeScrubIndex = index
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            .pointerInput(points) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                        val index = ((offset.x / stepX)).roundToInt().coerceIn(0, points.size - 1)
                                        activeScrubIndex = index
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                        val index = ((change.position.x / stepX)).roundToInt().coerceIn(0, points.size - 1)
                                        if (index != activeScrubIndex) {
                                            activeScrubIndex = index
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 1. Grid Horizontal Guidelines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = h * (i.toFloat() / gridLines)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.06f),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = with(density) { 0.8.dp.toPx() }
                                )
                            }

                            if (points.size < 2) return@Canvas

                            val stepX = w / (points.size - 1)
                            val normalizedPoints = points.mapIndexed { index, pt ->
                                val v = when (selectedMetric) {
                                    "NET_DEGER" -> pt.netWorth
                                    "NAKIT" -> pt.cashBalance
                                    else -> pt.totalAssets
                                }
                                val normY = (1f - ((v - minVal) / range).toFloat()).coerceIn(0f, 1f) * h
                                val animatedY = h - (h - normY) * animProgress
                                Offset(index * stepX, animatedY)
                            }

                            // 2. Build Smooth Cubic Bezier Spline Path
                            val path = Path()
                            val fillPath = Path()

                            fillPath.moveTo(0f, h)
                            path.moveTo(normalizedPoints[0].x, normalizedPoints[0].y)
                            fillPath.lineTo(normalizedPoints[0].x, normalizedPoints[0].y)

                            for (i in 0 until normalizedPoints.size - 1) {
                                val p0 = normalizedPoints[i]
                                val p1 = normalizedPoints[i + 1]
                                val cx1 = p0.x + (p1.x - p0.x) / 2f
                                val cy1 = p0.y
                                val cx2 = p0.x + (p1.x - p0.x) / 2f
                                val cy2 = p1.y

                                path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                                fillPath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                            }

                            fillPath.lineTo(w, h)
                            fillPath.close()

                            // 3. Draw Vertical Glow Gradient Fill
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        chartColor.copy(alpha = 0.32f),
                                        chartColor.copy(alpha = 0.02f)
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // 4. Draw Main Spline Stroke
                            drawPath(
                                path = path,
                                color = chartColor,
                                style = Stroke(
                                    width = with(density) { 3.dp.toPx() },
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // 5. Draw Active Touch Scrubber / Crosshair
                            activeScrubIndex?.let { scrubIdx ->
                                if (scrubIdx in normalizedPoints.indices) {
                                    val activePt = normalizedPoints[scrubIdx]

                                    // Vertical dashed guide line
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.45f),
                                        start = Offset(activePt.x, 0f),
                                        end = Offset(activePt.x, h),
                                        strokeWidth = with(density) { 1.2.dp.toPx() },
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )

                                    // Pulse Halo Ring
                                    drawCircle(
                                        color = chartColor.copy(alpha = 0.25f),
                                        radius = with(density) { 14.dp.toPx() },
                                        center = activePt
                                    )
                                    // Outer ring
                                    drawCircle(
                                        color = Color.White,
                                        radius = with(density) { 6.5.dp.toPx() },
                                        center = activePt
                                    )
                                    // Inner dot
                                    drawCircle(
                                        color = chartColor,
                                        radius = with(density) { 4.dp.toPx() },
                                        center = activePt
                                    )
                                }
                            }

                            // 6. Draw Key Node Points
                            normalizedPoints.forEachIndexed { index, ptOffset ->
                                if (activeScrubIndex != index) {
                                    drawCircle(
                                        color = Color(0xFF0F172A),
                                        radius = with(density) { 4.dp.toPx() },
                                        center = ptOffset
                                    )
                                    drawCircle(
                                        color = chartColor,
                                        radius = with(density) { 2.5.dp.toPx() },
                                        center = ptOffset
                                    )
                                }
                            }
                        }
                    }

                    // X-Axis Day Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEachIndexed { idx, pt ->
                            val isSelected = activeScrubIndex == idx
                            CurrencyText(
                                text = pt.dayLabel,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) chartColor else theme.textSecondaryColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Milestone Statistics Grid (ATH, Drawdown, Average)
        item {
            val maxVal = points.maxOfOrNull {
                when (selectedMetric) {
                    "NET_DEGER" -> it.netWorth
                    "NAKIT" -> it.cashBalance
                    else -> it.totalAssets
                }
            } ?: 0.0
            val minVal = points.minOfOrNull {
                when (selectedMetric) {
                    "NET_DEGER" -> it.netWorth
                    "NAKIT" -> it.cashBalance
                    else -> it.totalAssets
                }
            } ?: 0.0
            val avgVal = if (points.isNotEmpty()) {
                points.sumOf {
                    when (selectedMetric) {
                        "NET_DEGER" -> it.netWorth
                        "NAKIT" -> it.cashBalance
                        else -> it.totalAssets
                    }
                } / points.size
            } else 0.0

            val drawdownPct = if (maxVal > 0) ((endValue - maxVal) / maxVal) * 100.0 else 0.0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CurrencyText(
                        text = tr("DÖNEMSEL İSTATİSTİK & REKORLAR", "PERIOD STATISTICS & RECORDS"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricTile(
                            label = tr("Dönem Zirvesi (ATH)", "Period Peak (ATH)"),
                            value = formatCredit(maxVal.toLong()),
                            subtext = tr("En Yüksek Seviye", "Highest Level"),
                            color = ThemePositive,
                            modifier = Modifier.weight(1f),
                            theme = theme
                        )
                        StatMetricTile(
                            label = tr("Dönem Dibi", "Period Low"),
                            value = formatCredit(minVal.toLong()),
                            subtext = tr("En Düşük Seviye", "Lowest Level"),
                            color = ThemeNegative,
                            modifier = Modifier.weight(1f),
                            theme = theme
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricTile(
                            label = tr("Dönem Ortalaması", "Period Average"),
                            value = formatCredit(avgVal.toLong()),
                            subtext = tr("Ağırlıklı Ortalama", "Weighted Avg"),
                            color = ThemeNeonCyan,
                            modifier = Modifier.weight(1f),
                            theme = theme
                        )
                        StatMetricTile(
                            label = tr("Zirveye Uzaklık", "Drawdown from ATH"),
                            value = "${if (drawdownPct >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f%%", drawdownPct)}",
                            subtext = if (drawdownPct >= -0.1) tr("Zirvede!", "At Peak!") else tr("Geri Çekilme", "Pullback"),
                            color = if (drawdownPct >= -0.1) ThemePositive else ThemeNegative,
                            modifier = Modifier.weight(1f),
                            theme = theme
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 2: VARLIK PORTFÖYÜ VE HALKA GRAFİĞİ (ASSET ALLOCATION & DONUT CHART)
// -----------------------------------------------------------------------------------------
@Composable
private fun AssetAllocationView(
    totalAssets: Double,
    cash: Double,
    deposit: Double,
    facilities: Double,
    inventory: Double,
    loanDebt: Double,
    netWorth: Double,
    theme: AppThemeOption
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    val safeTotal = totalAssets.coerceAtLeast(1.0)

    val slices = remember(cash, deposit, facilities, inventory, safeTotal) {
        listOf(
            AssetSlice(
                id = "CASH",
                title = if (isEng) "Liquid Cash" else "Nakit & Cüzdan",
                subtitle = if (isEng) "Wallet balance & petty cash" else "Kasa & cüzdan hazır nakit",
                value = cash,
                color = ThemeGold,
                icon = Icons.Rounded.AccountBalanceWallet,
                percentage = ((cash / safeTotal) * 100f).toFloat()
            ),
            AssetSlice(
                id = "DEPOSIT",
                title = if (isEng) "Bank Deposits" else "Banka Mevduatları",
                subtitle = if (isEng) "Term & demand bank interest" else "Vadeli & vadesiz faizli mevduat",
                value = deposit,
                color = ThemeNeonCyan,
                icon = Icons.Rounded.AccountBalance,
                percentage = ((deposit / safeTotal) * 100f).toFloat()
            ),
            AssetSlice(
                id = "FACILITIES",
                title = if (isEng) "Industrial Facilities" else "Sanayi & Tesisler",
                subtitle = if (isEng) "Factories, farms & infrastructure" else "Fabrika, çiftlik ve üretim tesisleri",
                value = facilities,
                color = Color(0xFF38BDF8),
                icon = Icons.Rounded.Business,
                percentage = ((facilities / safeTotal) * 100f).toFloat()
            ),
            AssetSlice(
                id = "INVENTORY",
                title = if (isEng) "Warehouse Goods" else "Depo Emtia & Stok",
                subtitle = if (isEng) "Raw materials & market inventory" else "Hammadde, yarı mamul ve stoklar",
                value = inventory,
                color = ThemePositive,
                icon = Icons.Rounded.Inventory2,
                percentage = ((inventory / safeTotal) * 100f).toFloat()
            )
        )
    }

    var selectedSliceId by remember { mutableStateOf<String?>(null) }
    val selectedSlice = slices.find { it.id == selectedSliceId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Interactive Donut Chart Container
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("PORTFÖY VARLIK DAĞILIMI (HALKA)", "PORTFOLIO ALLOCATION (DONUT)"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            letterSpacing = 0.5.sp
                        )
                        CurrencyText(
                            text = tr("4 Varlık Sınıfı", "4 Asset Classes"),
                            fontSize = 9.sp,
                            color = theme.textSecondaryColor
                        )
                    }

                    // Canvas Donut with Central Readout
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .pointerInput(slices) {
                                detectTapGestures { offset ->
                                    val centerX = size.width / 2.0
                                    val centerY = size.height / 2.0
                                    val dx = offset.x.toDouble() - centerX
                                    val dy = offset.y.toDouble() - centerY
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val outerR = kotlin.math.min(size.width, size.height) / 2.0
                                    val innerR = outerR * 0.55

                                    if (dist in innerR..outerR) {
                                        var angle = Math.toDegrees(atan2(dy, dx)).toFloat()
                                        if (angle < 0f) angle += 360f

                                        var accumulated = 0f
                                        var matched: String? = null
                                        for (slice in slices) {
                                            val sweep = (slice.value / safeTotal).toFloat() * 360f
                                            if (angle >= accumulated && angle < accumulated + sweep) {
                                                matched = slice.id
                                                break
                                            }
                                            accumulated += sweep
                                        }
                                        selectedSliceId = if (selectedSliceId == matched) null else matched
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        selectedSliceId = null
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = with(density) { 26.dp.toPx() }
                            val baseRadius = (size.minDimension - strokeWidth) / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            var currentStartAngle = -90f

                            slices.forEach { slice ->
                                val sweep = (slice.value / safeTotal).toFloat() * 360f
                                val isSelected = selectedSliceId == slice.id
                                val sliceStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                                if (sweep > 0.5f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = currentStartAngle,
                                        sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                                        useCenter = false,
                                        topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                                        size = Size(baseRadius * 2, baseRadius * 2),
                                        style = Stroke(width = sliceStroke, cap = StrokeCap.Round)
                                    )
                                }
                                currentStartAngle += sweep
                            }
                        }

                        // Donut Center Hub Info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (selectedSlice != null) {
                                CurrencyText(
                                    text = selectedSlice.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = selectedSlice.color,
                                    maxLines = 1
                                )
                                CurrencyText(
                                    text = formatCredit(selectedSlice.value.toLong()),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = theme.textPrimaryColor
                                )
                                CurrencyText(
                                    text = "%${String.format(java.util.Locale.US, "%.1f", selectedSlice.percentage)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = selectedSlice.color
                                )
                            } else {
                                CurrencyText(
                                    text = tr("TOPLAM VARLIK", "TOTAL ASSETS"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textSecondaryColor
                                )
                                CurrencyText(
                                    text = formatCredit(totalAssets.toLong()),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold
                                )
                                CurrencyText(
                                    text = tr("Tıkla & İncele", "Tap to inspect"),
                                    fontSize = 8.5.sp,
                                    color = theme.textSecondaryColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Interactive Filter Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(slices) { slice ->
                            val isSelected = selectedSliceId == slice.id
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSliceId = if (isSelected) null else slice.id
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) slice.color.copy(alpha = 0.2f) else theme.surfaceVariantColor,
                                border = BorderStroke(1.dp, if (isSelected) slice.color else theme.borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(slice.color))
                                    CurrencyText(
                                        text = slice.title,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) slice.color else theme.textSecondaryColor
                                    )
                                    CurrencyText(
                                        text = "%${slice.percentage.toInt()}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = if (isSelected) slice.color else theme.textPrimaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Asset Class Detailed Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyText(
                    text = tr("VARLIK SINIFI DÖKÜMÜ & DETAYLARI", "ASSET CLASS BREAKDOWN & DETAILS"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeGold,
                    letterSpacing = 0.5.sp
                )

                slices.forEach { slice ->
                    val isSelected = selectedSliceId == slice.id
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedSliceId = if (isSelected) null else slice.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) slice.color.copy(alpha = 0.08f) else theme.surfaceColor,
                        border = BorderStroke(1.dp, if (isSelected) slice.color else theme.borderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = slice.color.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, slice.color.copy(alpha = 0.5f)),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = slice.icon, contentDescription = null, tint = slice.color, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Column {
                                        CurrencyText(text = slice.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textPrimaryColor)
                                        CurrencyText(text = slice.subtitle, fontSize = 8.5.sp, color = theme.textSecondaryColor, maxLines = 1)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    CurrencyText(
                                        text = formatCredit(slice.value.toLong()),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = slice.color
                                    )
                                    CurrencyText(
                                        text = "%${String.format(java.util.Locale.US, "%.1f", slice.percentage)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textSecondaryColor
                                    )
                                }
                            }

                            // Colored Proportion Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(slice.percentage / 100f)
                                        .fillMaxHeight()
                                        .background(slice.color)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Solvency & Debt Health Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyText(
                        text = tr("ÖZKAYNAK & BORÇ DENGESİ", "EQUITY & DEBT BALANCE"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(tr("Net Şirket Özkaynağı", "Net Company Equity"), fontSize = 9.sp, color = theme.textSecondaryColor)
                            CurrencyText(text = formatCredit(netWorth.toLong()), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily, color = ThemePositive)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            CurrencyText(tr("Toplam Kredi Borcu", "Total Loan Debt"), fontSize = 9.sp, color = theme.textSecondaryColor)
                            CurrencyText(
                                text = formatCredit(loanDebt.toLong()),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                color = if (loanDebt > 0) ThemeNegative else theme.textSecondaryColor
                            )
                        }
                    }

                    val debtRatio = if (totalAssets > 0) (loanDebt / totalAssets) * 100.0 else 0.0
                    CurrencyText(
                        text = "${tr("Borçluluk Oranı (Kaldıraç)", "Leverage Ratio")}: %${String.format(java.util.Locale.US, "%.1f", debtRatio)} (${if (debtRatio < 20.0) tr("Güvenli", "Safe") else if (debtRatio < 50.0) tr("Dengeli", "Moderate") else tr("Yüksek Risk", "High Risk")})",
                        fontSize = 9.sp,
                        color = if (debtRatio < 20.0) ThemePositive else if (debtRatio < 50.0) ThemeGold else ThemeNegative,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 3: HAZİNE SAĞLIĞI VE ORANLAR (FINANCIAL HEALTH & KPIS)
// -----------------------------------------------------------------------------------------
@Composable
private fun FinancialHealthView(
    netWorth: Double,
    totalAssets: Double,
    cash: Double,
    deposit: Double,
    loanDebt: Double,
    dailyIncome: Double,
    dailyExpense: Double,
    theme: AppThemeOption
) {
    val isEng = isEnglishLanguage()

    // Key Ratio Calculations
    val totalLiquid = cash + deposit
    val liquidityRatio = if (totalAssets > 0) (totalLiquid / totalAssets) * 100.0 else 0.0
    val debtRatio = if (totalAssets > 0) (loanDebt / totalAssets) * 100.0 else 0.0
    val netDailyFlow = dailyIncome - dailyExpense
    val coverageRatio = if (dailyExpense > 0) dailyIncome / dailyExpense else 5.0

    // Compute Tycoon Rating Grade
    val healthGrade = when {
        debtRatio < 10.0 && liquidityRatio >= 25.0 && netDailyFlow >= 0 -> "AAA"
        debtRatio < 25.0 && liquidityRatio >= 18.0 && netDailyFlow >= 0 -> "AA"
        debtRatio < 40.0 && liquidityRatio >= 12.0 -> "A"
        debtRatio < 60.0 && liquidityRatio >= 8.0 -> "BBB"
        else -> "B"
    }

    val healthTitle = when (healthGrade) {
        "AAA" -> tr("Mükemmel Hazine Yapısı", "Prime Treasury Solvency")
        "AA" -> tr("Çok Güçlü & Dengeli", "Very Strong & Balanced")
        "A" -> tr("İstikrarlı Finansal Sağlık", "Stable Financial Health")
        "BBB" -> tr("Orta Düzey Risk", "Moderate Leverage Risk")
        else -> tr("Yüksek Borç / Düşük Likidite", "High Debt / Low Liquidity")
    }

    val healthColor = when (healthGrade) {
        "AAA", "AA" -> ThemePositive
        "A" -> ThemeGold
        "BBB" -> Color(0xFFF97316)
        else -> ThemeNegative
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Solvency Rating Hero Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, healthColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        CurrencyText(
                            text = tr("ŞİRKET HAZİNE DERECESİ", "TREASURY CREDIT RATING"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textSecondaryColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyText(
                            text = healthTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = healthColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyText(
                            text = tr("Kaldıraç, likidite ve nakit akışı gücüne göre belirlenir.", "Determined by leverage, liquidity and cash flow strength."),
                            fontSize = 8.5.sp,
                            color = theme.textSecondaryColor
                        )
                    }

                    // Rating Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = healthColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, healthColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CurrencyText(
                                text = healthGrade,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                color = healthColor
                            )
                            CurrencyText(
                                text = tr("RATING", "RATING"),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = healthColor
                            )
                        }
                    }
                }
            }
        }

        // Financial KPI Ratios Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyText(
                    text = tr("KRİTİK HAZİNE & PERFORMANS ORANLARI", "KEY TREASURY & PERFORMANCE RATIOS"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeGold,
                    letterSpacing = 0.5.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiRatioCard(
                        title = tr("Likidite Oranı", "Liquidity Ratio"),
                        value = "%${String.format(java.util.Locale.US, "%.1f", liquidityRatio)}",
                        status = if (liquidityRatio >= 20.0) tr("Yüksek Nakit", "High Liquidity") else tr("Sıkışık", "Tight"),
                        color = if (liquidityRatio >= 20.0) ThemePositive else ThemeNegative,
                        description = tr("Nakit & Banka / Toplam Varlık", "Liquid Cash / Total Assets"),
                        modifier = Modifier.weight(1f),
                        theme = theme
                    )

                    KpiRatioCard(
                        title = tr("Borç Kaldıracı", "Debt Leverage"),
                        value = "%${String.format(java.util.Locale.US, "%.1f", debtRatio)}",
                        status = if (debtRatio < 25.0) tr("Düşük Risk", "Low Risk") else tr("Dikkat", "Caution"),
                        color = if (debtRatio < 25.0) ThemePositive else ThemeNegative,
                        description = tr("Kredi Borcu / Toplam Varlık", "Loan Debt / Total Assets"),
                        modifier = Modifier.weight(1f),
                        theme = theme
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiRatioCard(
                        title = tr("Gider Karşılama", "Expense Coverage"),
                        value = "${String.format(java.util.Locale.US, "%.1f", coverageRatio)}x",
                        status = if (coverageRatio >= 1.2) tr("Kârlı Akış", "Profitable") else tr("Açık Risk", "Deficit Risk"),
                        color = if (coverageRatio >= 1.2) ThemePositive else ThemeNegative,
                        description = tr("Günlük Gelir / Günlük Gider", "Daily Income / Expense"),
                        modifier = Modifier.weight(1f),
                        theme = theme
                    )

                    KpiRatioCard(
                        title = tr("Net Günlük Akış", "Net Cash Velocity"),
                        value = "${if (netDailyFlow >= 0) "+" else ""}${formatCredit(netDailyFlow.toLong())}",
                        status = if (netDailyFlow >= 0) tr("Pozitif", "Positive") else tr("Bütçe Açığı", "Deficit"),
                        color = if (netDailyFlow >= 0) ThemePositive else ThemeNegative,
                        description = tr("Kasaya Net Günlük Giriş", "Net Inflow to Treasury"),
                        modifier = Modifier.weight(1f),
                        theme = theme
                    )
                }
            }
        }

        // Strategic Advisor Insights
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Rounded.Lightbulb, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                        CurrencyText(
                            text = tr("STRATEJİK TİCARET VE BÜYÜME TAVSİYESİ", "STRATEGIC TRADE & GROWTH ADVICE"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    val adviceText = when {
                        liquidityRatio > 65.0 -> tr(
                            "Kasanızda çok yüksek oranda nakit atıl bekliyor. Bu sermayeyi yeni sanayi tesislerine yatırarak veya faiz getirisi için bankaya mevduat açarak büyümenizi hızlandırabilirsiniz.",
                            "Your cash reserves are excessively high and idle. Consider investing in industrial facilities or high-yield deposits to accelerate expansion."
                        )
                        liquidityRatio < 10.0 -> tr(
                            "Likidite tamponunuz tehlikeli seviyede düşük. Ani masraflar ve piyasa düşüşlerine karşı korunmak için biraz stok satarak veya kâr payı biriktirerek nakit rezervi oluşturun.",
                            "Your liquidity buffer is dangerously low. Sell excess inventory or pause facility upgrades to build a safe cash cushion."
                        )
                        debtRatio > 45.0 -> tr(
                            "Kredi borcunuz varlıklarınıza oranla yüksek seyrediyor. Faiz yükünü azaltmak için kârınızın bir kısmını öncelikle borç kapatmaya ayırmanız önerilir.",
                            "Your loan debt is high relative to assets. Prioritize paying down principal to reduce daily interest payments."
                        )
                        else -> tr(
                            "Şirketinizin varlık dağılımı ve nakit dengesi son derece sağlıklı. Üretim tesisleriniz ve nakit rezervleriniz dengeli büyümeyi destekliyor.",
                            "Your company portfolio and liquidity balance are in an optimal state. Maintain current expansion rhythm."
                        )
                    }

                    CurrencyText(
                        text = adviceText,
                        fontSize = 11.5.sp,
                        color = theme.textPrimaryColor,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// REUSABLE HELPER UI COMPONENTS
// -----------------------------------------------------------------------------------------

@Composable
private fun TabPillButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    theme: AppThemeOption,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) tint.copy(alpha = 0.2f) else theme.surfaceVariantColor,
        border = BorderStroke(1.dp, if (isSelected) tint else theme.borderColor)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) tint else theme.textSecondaryColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            CurrencyText(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) tint else theme.textSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatMetricTile(
    label: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier,
    theme: AppThemeOption
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = theme.surfaceVariantColor,
        border = BorderStroke(0.8.dp, theme.borderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CurrencyText(text = label, fontSize = 9.sp, color = theme.textSecondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CurrencyText(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily, color = color, maxLines = 1)
            CurrencyText(text = subtext, fontSize = 8.sp, color = theme.textSecondaryColor.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

@Composable
private fun KpiRatioCard(
    title: String,
    value: String,
    status: String,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    theme: AppThemeOption
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = theme.surfaceColor,
        border = BorderStroke(0.8.dp, theme.borderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(text = title, fontSize = 9.5.sp, color = theme.textSecondaryColor, maxLines = 1)
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = color.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
                ) {
                    CurrencyText(
                        text = status,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        maxLines = 1
                    )
                }
            }
            CurrencyText(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                color = color,
                maxLines = 1
            )
            CurrencyText(
                text = description,
                fontSize = 8.sp,
                color = theme.textSecondaryColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
