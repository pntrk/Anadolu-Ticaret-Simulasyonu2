package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Product
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

@Composable
fun RdLabCenterVisualView(
    uiState: GameUiState,
    viewModel: GameViewModel,
    allTechs: List<TechItemData>,
    highlightedTechKey: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val isEnglish = isEnglishLanguage()
    val player = uiState.playerState.player
    val activeResearches = uiState.hrState.activeResearches
    val researchLevels = uiState.hrState.researchLevels

    val pLevel = player?.level ?: 1
    val pMoney = player?.money ?: 0L
    val pGems = player?.gems ?: 0

    // Strictly sanitize and deduplicate active researches to prevent 1 research occupying multiple slots
    val cleanActiveResearches = remember(activeResearches) {
        val clean = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()
        activeResearches.forEach { (k, v) ->
            val base = k.removePrefix("tech_")
            if (v > now && !clean.containsKey(base)) {
                clean[base] = v
            }
        }
        clean
    }

    // Active entries strictly deduplicated (max 4 distinct active researches)
    val activeEntries = remember(cleanActiveResearches) {
        cleanActiveResearches.entries.take(4).toList()
    }

    // Selected active slot index (0..3)
    var selectedActiveSlotIndex by remember { mutableIntStateOf(0) }

    val currentActiveEntry = if (activeEntries.isNotEmpty()) {
        val safeIndex = selectedActiveSlotIndex.coerceIn(0, activeEntries.size - 1)
        activeEntries[safeIndex]
    } else null

    val currentActiveTechKey = currentActiveEntry?.key?.removePrefix("tech_")
    val currentActiveTech = remember(currentActiveTechKey, allTechs) {
        allTechs.find { it.techKey == currentActiveTechKey }
    }

    // Selected tech for catalog inspection
    var selectedTechKey by remember(highlightedTechKey, cleanActiveResearches) {
        mutableStateOf(
            highlightedTechKey
                ?: cleanActiveResearches.keys.firstOrNull()
                ?: allTechs.firstOrNull()?.techKey
                ?: "quantum_ai"
        )
    }

    val selectedTech = remember(selectedTechKey, allTechs) {
        allTechs.find { it.techKey == selectedTechKey } ?: allTechs.first()
    }

    // Category Filter: 0: Tümü, 1: Tier 4 Mega, 2: Tier 3 Nihai, 3: Destek
    var selectedCategoryTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTechs = remember(allTechs, selectedCategoryTab, searchQuery, isEnglish) {
        allTechs.filter { tech ->
            val matchesCategory = when (selectedCategoryTab) {
                1 -> tech.tierPriority == 4
                2 -> tech.tierPriority == 3
                3 -> tech.tierPriority == 2
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                tech.techKey.lowercase().contains(q)
            }
            matchesCategory && matchesSearch
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWideScreen = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 1. SCI-FI LAB CENTER BANNER ---
            SciFiLabCenterTopBanner(
                totalTechLevel = researchLevels.values.sum(),
                activeSlotsCount = activeEntries.size,
                isEnglish = isEnglish
            )

            // --- 2. 4 AR-GE ARAŞTIRMA İSTASYONU (4 RESEARCH BAYS) ---
            RdFourSlotsBay(
                activeEntries = activeEntries,
                allTechs = allTechs,
                researchLevels = researchLevels,
                selectedActiveSlotIndex = selectedActiveSlotIndex,
                isWideScreen = isWideScreen,
                isEnglish = isEnglish,
                onSelectActiveSlot = { index ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedActiveSlotIndex = index
                    activeEntries.getOrNull(index)?.key?.removePrefix("tech_")?.let { key ->
                        selectedTechKey = key
                    }
                },
                onSelectEmptySlot = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val candidateTech = allTechs.firstOrNull { tech ->
                        val lvl = researchLevels[tech.techKey] ?: 0
                        lvl < 5 && !cleanActiveResearches.containsKey(tech.techKey)
                    }
                    if (candidateTech != null) {
                        selectedTechKey = candidateTech.techKey
                    }
                }
            )

            // --- 3. AKTİF ARAŞTIRMA HUD PANELİ ---
            if (currentActiveEntry != null && currentActiveTech != null) {
                val currentLevel = viewModel.getTechLevel(currentActiveTech.techKey)
                val endTime = currentActiveEntry.value
                val remainingMs = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                val baseDuration = currentActiveTech.baseDurationMs
                val totalDuration = (baseDuration * (currentLevel + 1) * 1.5).toLong().coerceAtLeast(60_000L)
                val progress = if (remainingMs <= 0) 1f else {
                    (1f - (remainingMs.toFloat() / totalDuration.toFloat())).coerceIn(0.05f, 1f)
                }
                val gemSkipCost = viewModel.calculateTechGemCost(currentActiveTech.techKey, currentLevel)

                ActiveResearchHudCard(
                    tech = currentActiveTech,
                    currentLevel = currentLevel,
                    remainingMs = remainingMs,
                    progress = progress,
                    gemCost = gemSkipCost,
                    slotNumber = (selectedActiveSlotIndex.coerceIn(0, (activeEntries.size - 1).coerceAtLeast(0))) + 1,
                    isEnglish = isEnglish,
                    onSkipWithGems = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.skipResearchWithGems(currentActiveTech.techKey)
                    }
                )
            }

            // --- 4. TEKNOLOJİ KATALOĞU (KULLANICI DOSTU, MOBİL & WEB UYUMLU) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF071224).copy(alpha = 0.92f),
                border = BorderStroke(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header & Count Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(18.dp))
                            CurrencyText(
                                text = tr("TEKNOLOJİ VE GELİŞTİRME KATALOĞU", "TECHNOLOGY & R&D CATALOG"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThemeNeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                        ) {
                            CurrencyText(
                                text = "${allTechs.size} " + tr("TEKNOLOJİ", "TECHS"),
                                color = ThemeNeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = tr("Teknoloji veya sanayi dalı ara...", "Search tech or industry..."),
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Temizle", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeNeonCyan,
                            unfocusedBorderColor = Color(0xFF1E3A5F),
                            focusedContainerColor = Color(0xFF091426),
                            unfocusedContainerColor = Color(0xFF091426),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Category Selector Tabs
                    val categories = listOf(
                        if (isEnglish) "ALL (${allTechs.size})" else "TÜMÜ (${allTechs.size})",
                        if (isEnglish) "🚀 TIER 4 MEGA (4)" else "🚀 TİER 4 MEGA (4)",
                        if (isEnglish) "⚡ TIER 3 FINAL (4)" else "⚡ TİER 3 NİHAİ (4)",
                        if (isEnglish) "🛡️ SUPPORT (5)" else "🛡️ DESTEK (5)"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories.size) { index ->
                            val isSelected = (selectedCategoryTab == index)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.22f) else Color(0xFF0D1C33),
                                border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else Color(0xFF1E3A5F)),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategoryTab = index
                                }
                            ) {
                                CurrencyText(
                                    text = categories[index],
                                    color = if (isSelected) ThemeNeonCyan else Color(0xFF94A3B8),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Responsive Tech Cards (Grid on wide screens, single column on compact)
                    if (isWideScreen) {
                        val chunkedTechs = filteredTechs.chunked(2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunkedTechs.forEach { rowTechs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowTechs.forEach { tech ->
                                        val currentLevel = viewModel.getTechLevel(tech.techKey)
                                        val isActive = cleanActiveResearches.containsKey(tech.techKey)
                                        val isSelected = (tech.techKey == selectedTechKey)
                                        val cost = viewModel.calculateTechCost(tech.techKey, currentLevel)
                                        val gemCost = viewModel.calculateTechGemCost(tech.techKey, currentLevel)

                                        Box(modifier = Modifier.weight(1f)) {
                                            TechCatalogCardItem(
                                                tech = tech,
                                                currentLevel = currentLevel,
                                                isActive = isActive,
                                                isSelected = isSelected,
                                                cost = cost,
                                                gemCost = gemCost,
                                                playerMoney = pMoney,
                                                playerGems = pGems,
                                                activeCount = activeEntries.size,
                                                isEnglish = isEnglish,
                                                onCardClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedTechKey = tech.techKey
                                                },
                                                onStartResearch = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedTechKey = tech.techKey
                                                    viewModel.startResearch(tech.techKey, cost)
                                                },
                                                onSkipWithGems = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.skipResearchWithGems(tech.techKey)
                                                }
                                            )
                                        }
                                    }
                                    if (rowTechs.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredTechs.forEach { tech ->
                                val currentLevel = viewModel.getTechLevel(tech.techKey)
                                val isActive = cleanActiveResearches.containsKey(tech.techKey)
                                val isSelected = (tech.techKey == selectedTechKey)
                                val cost = viewModel.calculateTechCost(tech.techKey, currentLevel)
                                val gemCost = viewModel.calculateTechGemCost(tech.techKey, currentLevel)

                                TechCatalogCardItem(
                                    tech = tech,
                                    currentLevel = currentLevel,
                                    isActive = isActive,
                                    isSelected = isSelected,
                                    cost = cost,
                                    gemCost = gemCost,
                                    playerMoney = pMoney,
                                    playerGems = pGems,
                                    activeCount = activeEntries.size,
                                    isEnglish = isEnglish,
                                    onCardClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTechKey = tech.techKey
                                    },
                                    onStartResearch = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTechKey = tech.techKey
                                        viewModel.startResearch(tech.techKey, cost)
                                    },
                                    onSkipWithGems = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.skipResearchWithGems(tech.techKey)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- 5. SEÇİLİ TEKNOLOJİ BİLGİ VE DETAY AKSİYON PANELİ ---
            val selectedCurrentLevel = viewModel.getTechLevel(selectedTech.techKey)
            val isSelectedActive = cleanActiveResearches.containsKey(selectedTech.techKey)
            val cost = viewModel.calculateTechCost(selectedTech.techKey, selectedCurrentLevel)
            val gemCost = viewModel.calculateTechGemCost(selectedTech.techKey, selectedCurrentLevel)

            TechInfoActionPanel(
                tech = selectedTech,
                currentLevel = selectedCurrentLevel,
                cost = cost,
                gemCost = gemCost,
                playerMoney = pMoney,
                playerGems = pGems,
                playerLevel = pLevel,
                isThisActive = isSelectedActive,
                activeCount = activeEntries.size,
                isEnglish = isEnglish,
                onStartResearch = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.startResearch(selectedTech.techKey, cost)
                },
                onSkipWithGems = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.skipResearchWithGems(selectedTech.techKey)
                }
            )
        }
    }
}

// ============================================================================
// 1. TOP SCI-FI TITLE BANNER
// ============================================================================
@Composable
private fun SciFiLabCenterTopBanner(
    totalTechLevel: Int,
    activeSlotsCount: Int,
    isEnglish: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF081224).copy(alpha = 0.90f),
        border = BorderStroke(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.70f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    CurrencyText(
                        text = tr("AR-GE ve İNOVASYON MERKEZİ", "R&D & INNOVATION LAB"),
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 12.5.sp
                    )
                    CurrencyText(
                        text = tr("Gelişmiş Sanayi ve Teknoloji Araştırmaları", "Advanced Industrial & Tech Engineering"),
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (activeSlotsCount >= 4) Color(0xFFE11D48).copy(alpha = 0.20f) else Color(0xFF0284C7).copy(alpha = 0.20f),
                border = BorderStroke(1.dp, if (activeSlotsCount >= 4) Color(0xFFF43F5E) else ThemeNeonCyan)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (activeSlotsCount > 0) ThemePositive else Color(0xFF64748B))
                    )
                    CurrencyText(
                        text = "$activeSlotsCount/4 " + tr("SLOT", "SLOTS"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (activeSlotsCount >= 4) Color(0xFFFDA4AF) else ThemeNeonCyan
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. 4 RESEARCH SLOTS BAY (Ar-Ge İstasyonları - 4 Eşzamanlı Slot)
// ============================================================================
@Composable
private fun RdFourSlotsBay(
    activeEntries: List<Map.Entry<String, Long>>,
    allTechs: List<TechItemData>,
    researchLevels: Map<String, Int>,
    selectedActiveSlotIndex: Int,
    isWideScreen: Boolean,
    isEnglish: Boolean,
    onSelectActiveSlot: (Int) -> Unit,
    onSelectEmptySlot: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF060E1A).copy(alpha = 0.95f),
        border = BorderStroke(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.60f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Science,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    CurrencyText(
                        text = tr("AR-GE ARAŞTIRMA İSTASYONLARI (4 SLOT)", "R&D RESEARCH BAYS (4 SLOTS)"),
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 11.5.sp
                    )
                }
                CurrencyText(
                    text = "${activeEntries.size}/4 " + tr("Aktif", "Active"),
                    color = if (activeEntries.size == 4) Color(0xFFFDA4AF) else ThemeNeonCyan,
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            // 4 Distinct Slots (Exactly 1 slot per active research, with 0 overlap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..3).forEach { slotIndex ->
                    val entry = activeEntries.getOrNull(slotIndex)
                    val isOccupied = entry != null
                    val isSelected = isOccupied && (slotIndex == selectedActiveSlotIndex)

                    val tKey = entry?.key?.removePrefix("tech_")
                    val tItem = tKey?.let { k -> allTechs.find { it.techKey == k } }
                    val currentLvl = tKey?.let { researchLevels[it] ?: 0 } ?: 0

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(86.dp)
                            .clickable {
                                if (isOccupied) {
                                    onSelectActiveSlot(slotIndex)
                                } else {
                                    onSelectEmptySlot()
                                }
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> Color(0xFF0C2A4A)
                            isOccupied -> Color(0xFF0B192E)
                            else -> Color(0xFF08121E).copy(alpha = 0.8f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> ThemeNeonCyan
                                isOccupied -> Color(0xFF0284C7).copy(alpha = 0.7f)
                                else -> Color(0xFF334155).copy(alpha = 0.45f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Row: Slot Number + Status Dot
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = "SLOT ${slotIndex + 1}",
                                    fontSize = 8.5.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ThemeNeonCyan else if (isOccupied) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOccupied) Color(0xFF22C55E) else Color(0xFF475569))
                                )
                            }

                            // Middle: Tech content or Empty button
                            if (isOccupied && tItem != null) {
                                Icon(
                                    imageVector = tItem.icon,
                                    contentDescription = null,
                                    tint = tItem.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                CurrencyText(
                                    text = stringResource(id = tItem.titleRes),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                val remainingMs = (entry.value - System.currentTimeMillis()).coerceAtLeast(0L)
                                CurrencyText(
                                    text = "Lv.${currentLvl + 1} • ${formatResearchTime(remainingMs)}",
                                    fontSize = 7.5.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF38BDF8),
                                    maxLines = 1
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Empty",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                CurrencyText(
                                    text = tr("+ BOŞ SLOT", "+ EMPTY"),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                                CurrencyText(
                                    text = tr("Seç", "Select"),
                                    fontSize = 7.5.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. AKTİF ARAŞTIRMA HUD KARTI
// ============================================================================
@Composable
private fun ActiveResearchHudCard(
    tech: TechItemData,
    currentLevel: Int,
    remainingMs: Long,
    progress: Float,
    gemCost: Int,
    slotNumber: Int,
    isEnglish: Boolean,
    onSkipWithGems: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071224).copy(alpha = 0.95f),
        border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = tech.icon,
                        contentDescription = null,
                        tint = tech.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    CurrencyText(
                        text = "${tr("AKTİF ARAŞTIRMA", "ACTIVE RESEARCH")} (SLOT $slotNumber/4):",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF22C55E))
                ) {
                    CurrencyText(
                        text = "Lvl $currentLevel ➔ ${currentLevel + 1}",
                        color = Color(0xFF4ADE80),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            CurrencyText(
                text = "${stringResource(id = tech.titleRes)} (${tr("Seviye", "Level")} ${currentLevel + 1})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            // Progress Bar & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ThemeNeonCyan,
                    trackColor = Color(0xFF0F172A)
                )
                CurrencyText(
                    text = "%${(progress * 100).toInt()}",
                    color = Color.White,
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = "${tr("Kalan Süre:", "Remaining Time:")} ${formatResearchTime(remainingMs)}",
                    color = Color(0xFFE2E8F0),
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )

                Button(
                    onClick = onSkipWithGems,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Diamond, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        CurrencyText(
                            text = "$gemCost 💎 " + tr("Hızlandır", "Speed Up"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. TECH CATALOG CARD ITEM (Responsive Single/Grid Card)
// ============================================================================
@Composable
private fun TechCatalogCardItem(
    tech: TechItemData,
    currentLevel: Int,
    isActive: Boolean,
    isSelected: Boolean,
    cost: Long,
    gemCost: Int,
    playerMoney: Long,
    playerGems: Int,
    activeCount: Int,
    isEnglish: Boolean,
    onCardClick: () -> Unit,
    onStartResearch: () -> Unit,
    onSkipWithGems: () -> Unit
) {
    val isMax = currentLevel >= 5
    val isGemsRequired = (currentLevel == 0)
    val isMoneyMet = playerMoney >= cost
    val isGemsMet = !isGemsRequired || (playerGems >= gemCost)
    val canUpgrade = !isMax && !isActive && isMoneyMet && isGemsMet && activeCount < 4

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF10233E) else Color(0xFF091426),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) ThemeNeonCyan else if (isActive) Color(0xFF0284C7).copy(alpha = 0.6f) else Color(0xFF1E3A5F).copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Icon + Title + Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(tech.accentColor.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = tech.icon, contentDescription = null, tint = tech.accentColor, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        CurrencyText(
                            text = stringResource(id = tech.titleRes),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CurrencyText(
                            text = stringResource(id = tech.subtitleRes),
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Level Dots (● ● ● ○ ○)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF070E1A),
                    border = BorderStroke(0.5.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        (1..5).forEach { dotIndex ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (dotIndex <= currentLevel) ThemePositive else Color(0xFF334155))
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText(
                            text = "Lv.$currentLevel",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (isMax) ThemePositive else Color.White
                        )
                    }
                }
            }

            // Row 2: Unlocks or Perks preview
            if (tech.unlockedProductIds.isNotEmpty()) {
                val prodNames = tech.unlockedProductIds.take(4).mapNotNull { prodId ->
                    Product.values().find { it.id == prodId }?.getDisplayName() ?: prodId
                }.joinToString(", ")

                CurrencyText(
                    text = "🔓 " + tr("Açılan:", "Unlocks:") + " $prodNames",
                    fontSize = 9.5.sp,
                    color = Color(0xFF38BDF8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val perkText = when (tech.techKey) {
                    "logistics" -> tr("Lojistik & Taşıma Masraflarında -%5 İndirim", "Logistics Cost -5% Discount")
                    "quality_control" -> tr("Ürün Satış Gelirlerinde +%5 Bonus", "Product Sales Revenue +5% Bonus")
                    "green_energy" -> tr("Tesis Enerji Tüketiminde -%8 Tasarruf", "Energy Consumption -8% Savings")
                    "automation" -> tr("Tesis Bakım Masraflarında -%6 İndirim", "Maintenance Cost -6% Discount")
                    "cyber_security" -> tr("Kredi & Borsa Faizlerinde -%2 İndirim", "Interest Rate -2% Discount")
                    "global_finance" -> tr("Uluslararası Ticaret ve Banka Verimliliği +%10", "Global Finance Efficiency +10%")
                    "cultural_heritage" -> tr("Turizm ve Prestij Gelirlerinde +%8 Artış", "Tourism & Prestige Revenue +8%")
                    else -> tr("Şirket Değerlemesinde +₳5M Prestij Bonusu", "Company Valuation +₳5M Prestige Bonus")
                }
                CurrencyText(
                    text = "⭐ $perkText",
                    fontSize = 9.5.sp,
                    color = Color(0xFFFBBF24),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Cost & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMax && !isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CurrencyText(
                            text = "${com.example.ui.components.formatCredit(cost)}₺",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (isMoneyMet) Color.White else Color(0xFFF43F5E)
                        )
                        if (isGemsRequired) {
                            CurrencyText(
                                text = "• $gemCost💎",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGemsMet) Color(0xFFFBBF24) else Color(0xFFF43F5E)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                when {
                    isMax -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF22C55E).copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, Color(0xFF22C55E))
                        ) {
                            CurrencyText(
                                text = "✅ " + tr("TAMAMLANDI (Lv 5)", "COMPLETED (Lv 5)"),
                                color = Color(0xFF4ADE80),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    isActive -> {
                        Button(
                            onClick = onSkipWithGems,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            CurrencyText(
                                text = "🔥 $gemCost💎 " + tr("Bitir", "Finish"),
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    canUpgrade -> {
                        Button(
                            onClick = onStartResearch,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            CurrencyText(
                                text = tr("🔬 Araştır", "🔬 Research"),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    activeCount >= 4 -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.8.dp, Color(0xFF64748B))
                        ) {
                            CurrencyText(
                                text = tr("⏳ Slotlar Dolu (4/4)", "⏳ Slots Full (4/4)"),
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.8.dp, Color(0xFF64748B))
                        ) {
                            CurrencyText(
                                text = tr("🔒 Yetersiz Bakiye", "🔒 Insufficient"),
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. TECH INFO & ACTION PANEL (BOTTOM INSPECTOR)
// ============================================================================
@Composable
private fun TechInfoActionPanel(
    tech: TechItemData,
    currentLevel: Int,
    cost: Long,
    gemCost: Int,
    playerMoney: Long,
    playerGems: Int,
    playerLevel: Int,
    isThisActive: Boolean,
    activeCount: Int,
    isEnglish: Boolean,
    onStartResearch: () -> Unit,
    onSkipWithGems: () -> Unit
) {
    val isMax = currentLevel >= 5
    val isGemsRequired = (currentLevel == 0)
    val isMoneyMet = playerMoney >= cost
    val isGemsMet = !isGemsRequired || (playerGems >= gemCost)
    val canUpgrade = !isMax && !isThisActive && isMoneyMet && isGemsMet && activeCount < 4

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071224).copy(alpha = 0.90f),
        border = BorderStroke(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Rounded.Equalizer, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    CurrencyText(
                        text = tr("SEÇİLİ TEKNOLOJİ BİLGİ VE YÖNETİM", "SELECTED TECHNOLOGY MANAGEMENT"),
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 11.5.sp
                    )
                }
                CurrencyText(
                    text = stringResource(id = tech.tierLabelRes),
                    color = Color(0xFFFBBF24),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tech Name & Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyText(
                    text = stringResource(id = tech.titleRes),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                CurrencyText(
                    text = "${tr("Mevcut:", "Current:")} Lv.$currentLevel / 5",
                    color = if (isMax) ThemePositive else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily
                )
            }

            // Requirements
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyText(
                    text = "${tr("Yükseltme Bedeli:", "Upgrade Cost:")} ",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                CurrencyText(
                    text = "${com.example.ui.components.formatCredit(cost)}₺" + (if (isGemsRequired) " & $gemCost 💎" else ""),
                    color = if (isMoneyMet && isGemsMet) Color.White else Color(0xFFF43F5E),
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 11.5.sp
                )
            }

            // Action Button
            when {
                isMax -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = "✅ " + tr("MAKSİMUM SEVİYEYE ULAŞILDI (Lv 5/5)", "MAXIMUM LEVEL REACHED (Lv 5/5)"),
                                color = Color(0xFF22C55E),
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                isThisActive -> {
                    Button(
                        onClick = onSkipWithGems,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Diamond, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            CurrencyText(
                                text = "$gemCost 💎 " + tr("İle Anında Tamamla", "Complete Instantly"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                canUpgrade -> {
                    Button(
                        onClick = onStartResearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Science, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            CurrencyText(
                                text = tr("ARAŞTIRMAYI BAŞLAT", "START RESEARCH") + " (Lv ${currentLevel + 1})",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
                activeCount >= 4 -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = "⏳ " + tr("TÜM AR-GE İSTASYONLARI DOLU (4/4)", "ALL R&D BAYS FULL (4/4)"),
                                color = Color(0xFFFDA4AF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
                else -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = "🔒 " + tr("YETERSİZ BAKİYE VEYA ELMAS", "INSUFFICIENT BALANCE OR GEMS"),
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatResearchTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
