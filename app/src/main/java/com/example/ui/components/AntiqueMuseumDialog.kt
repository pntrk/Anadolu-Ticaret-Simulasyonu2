package com.example.ui.components

import com.example.ui.components.CurrencyText

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.AntiqueArtifact
import com.example.data.ArtifactRarity
import com.example.data.MuseumHeritageManager
import com.example.data.MuseumAuctionItem
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class MuseumTabType {
    VITRINE,
    LIVE_AUCTION,
    MUSEUM_EXHIBITS,
    TICKET_REVENUE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiqueMuseumDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isOnlineRegistered by viewModel.isOnlineRegistered.collectAsStateWithLifecycle()
    val onlineEmail by viewModel.onlineEmail.collectAsStateWithLifecycle()
    val onlineUid = remember(onlineEmail) { if (onlineEmail.isNotBlank() && onlineEmail != "misafir_tuccar") onlineEmail.replace(".", "_") else "" }

    if (!isOnlineRegistered) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OnlineFeatureLockGate(
                feature = LockFeatureType.MUSEUM,
                viewModel = viewModel,
                onNavigateHome = onDismiss,
                isDialog = true
            )
        }
        return
    }

    val player by viewModel.player.collectAsStateWithLifecycle()
    val p = player ?: return

    var selectedArtifactForDetail by remember { mutableStateOf<AntiqueArtifact?>(null) }
    var showCreateAuctionDialog by remember { mutableStateOf(false) }
    var selectedArtifactToAuction by remember { mutableStateOf<AntiqueArtifact?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var isLoadingData by remember { mutableStateOf(true) }
    var ownedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ownedArtifacts by remember { mutableStateOf<List<AntiqueArtifact>>(emptyList()) }
    var totalPrestige by remember { mutableIntStateOf(0) }
    var hourlyIncome by remember { mutableLongStateOf(0L) }
    var unclaimedRevenue by remember { mutableLongStateOf(0L) }
    var totalDailyGems by remember { mutableIntStateOf(0) }
    var allAuctions by remember { mutableStateOf<List<MuseumAuctionItem>>(emptyList()) }
    var globalRegistry by remember { mutableStateOf<Map<String, com.example.data.MuseumArtifactRegistryItem>>(emptyMap()) }

    val hasOwnedArtifacts = ownedIds.isNotEmpty()
    val availableTabs = remember(hasOwnedArtifacts, ownedIds.size) {
        if (hasOwnedArtifacts) {
            listOf(
                MuseumTabType.VITRINE,
                MuseumTabType.LIVE_AUCTION,
                MuseumTabType.MUSEUM_EXHIBITS,
                MuseumTabType.TICKET_REVENUE
            )
        } else {
            listOf(
                MuseumTabType.LIVE_AUCTION,
                MuseumTabType.MUSEUM_EXHIBITS
            )
        }
    }

    var currentTab by remember { mutableStateOf(if (hasOwnedArtifacts) MuseumTabType.VITRINE else MuseumTabType.LIVE_AUCTION) }

    // Synchronize tab state with artifact ownership
    var hasInitializedTab by remember { mutableStateOf(false) }
    LaunchedEffect(hasOwnedArtifacts) {
        if (!hasInitializedTab) {
            currentTab = if (hasOwnedArtifacts) MuseumTabType.VITRINE else MuseumTabType.LIVE_AUCTION
            hasInitializedTab = true
        } else if (!hasOwnedArtifacts && (currentTab == MuseumTabType.VITRINE || currentTab == MuseumTabType.TICKET_REVENUE)) {
            currentTab = MuseumTabType.LIVE_AUCTION
        }
    }

    // Run cloud synchronization with Supabase and load state off the UI thread via Dispatchers.IO
    LaunchedEffect(Unit) {
        launch {
            com.example.data.MultiplayerManager.broadcastAuctionBidFlow.collect {
                // Short delay to ensure SharedPreferences is written by GameViewModel
                kotlinx.coroutines.delay(100)
                refreshTrigger++
            }
        }
        launch {
            com.example.data.MultiplayerManager.broadcastAuctionListedFlow.collect {
                kotlinx.coroutines.delay(100)
                refreshTrigger++
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        isLoadingData = true
        var isFirstLoad = true
        while (true) {
            withContext(Dispatchers.IO) {
                MuseumHeritageManager.syncWithSupabase(context, p.name, onlineUid)
                val ids = MuseumHeritageManager.getOwnedArtifactIds(context)
                val arts = MuseumHeritageManager.getOwnedArtifacts(context)
                val prestige = MuseumHeritageManager.getTotalMuseumPrestige(context)
                val income = MuseumHeritageManager.getTotalHourlyVisitorIncome(context)
                val unclaimed = MuseumHeritageManager.calculateUnclaimedVisitorRevenue(context)
                val gemYield = arts.sumOf { (it.baseValue / 15_000_000_000L).toInt().coerceIn(1, 10) }
                val auctions = MuseumHeritageManager.getAllMuseumAuctions(context, p.name, onlineUid)
                val registry = MuseumHeritageManager.getGlobalRegistry(context)

                withContext(Dispatchers.Main) {
                    ownedIds = ids
                    ownedArtifacts = arts
                    totalPrestige = prestige
                    hourlyIncome = income
                    totalDailyGems = gemYield
                    allAuctions = auctions
                    globalRegistry = registry
                    if (isFirstLoad) {
                        isLoadingData = false
                        isFirstLoad = false
                    }
                }
            }
            kotlinx.coroutines.delay(3000L)
        }
    }

    // Detail Lore Dialog
    selectedArtifactForDetail?.let { artifact ->
        ArtifactDetailDialog(
            artifact = artifact,
            isOwned = artifact.id in ownedIds,
            onDismiss = { selectedArtifactForDetail = null },
            onListForAuction = {
                selectedArtifactToAuction = artifact
                selectedArtifactForDetail = null
                showCreateAuctionDialog = true
            }
        )
    }

    // Create Auction Modal
    if (showCreateAuctionDialog) {
        CreateMuseumAuctionDialog(
            ownedArtifacts = ownedArtifacts,
            preSelectedArtifact = selectedArtifactToAuction,
            onDismiss = {
                showCreateAuctionDialog = false
                selectedArtifactToAuction = null
            },
            onAuctionCreated = { artifact, startBid, buyout, durationMins ->
                coroutineScope.launch {
                    val success = MuseumHeritageManager.createPlayerMuseumAuction(
                        context = context,
                        artifactId = artifact.id,
                        sellerName = p.name.ifBlank { "Holding" },
                        startingBid = startBid,
                        buyoutPrice = buyout,
                        durationMinutes = durationMins
                    )
                    if (success) {
                        SmartNotificationManager.show("🔨 '${artifact.name}' canlı müzayedeye çıkarıldı!", NotificationType.SUCCESS)
                        currentTab = MuseumTabType.LIVE_AUCTION
                        refreshTrigger++
                    } else {
                        SmartNotificationManager.show("Eser müzayedeye çıkarılamadı.", NotificationType.ALERT)
                    }
                }
                showCreateAuctionDialog = false
                selectedArtifactToAuction = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(4.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF070E1A),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF59E0B),
                        Color(0xFF3B82F6),
                        Color(0xFF0F172A)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // ==========================================
                // 1. TOP HEADER
                // ==========================================
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
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.bg_museum_header),
                                contentDescription = tr("Müze", "Museum"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            CurrencyText(
                                text = tr("AHİLİK MİRASI & ANTİKA MÜZESİ", "AHILIK HERITAGE & ANTIQUE MUSEUM"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily,
                                letterSpacing = 0.5.sp
                            )
                            CurrencyText(
                                text = tr("Anadolu Tarihi Eserleri & Canlı Müzayede", "Anatolian Historical Artifacts & Live Auction"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr("Kapat", "Close"),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 2. SUMMARY PRESTIGE & VISITOR STATS
                // ==========================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1B2E),
                    border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasOwnedArtifacts) {
                            // 1. Müze Statüsü / Eser Sayısı
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF14223A),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CurrencyText(
                                        text = tr("MÜZE STATÜSÜ", "MUSEUM STATUS"),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(
                                        text = "${ownedIds.size}/${MuseumHeritageManager.allArtifacts.size} " + tr("Eser", "Artifacts"),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 2. Müze Prestiji
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF14223A),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CurrencyText(
                                        text = tr("PRESTİJ", "PRESTIGE"),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(
                                        text = "+$totalPrestige ⭐",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFBBF24),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 3. Bilet Hasılatı
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ThemePositive.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, ThemePositive.copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1.15f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CurrencyText(
                                        text = tr("BİLET HASILATI", "TICKET REVENUE"),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemePositive.copy(alpha = 0.9f),
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(
                                        text = "+₳${formatMoney(hourlyIncome)}" + tr("/saat", "/hr"),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ThemePositive,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            // Player without artifacts: Explanatory summary bar
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF14223A),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CurrencyText(
                                        text = tr("MÜZE STATÜSÜ", "MUSEUM STATUS"),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(
                                        text = tr("0/13 (Vitrin Kilitli)", "0/13 (Vitrine Locked)"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                                border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1.8f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CurrencyText("💡", fontSize = 15.sp)
                                    CurrencyText(
                                        text = tr("Müzayededen eser kazanarak kendi müze vitrininizi ve bilet gelirinizi açın!", "Win an artifact at auction to unlock your museum showcase & ticket revenue!"),
                                        fontSize = 9.sp,
                                        color = Color(0xFFFBBF24),
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 3. DYNAMIC NAVIGATION TABS (Strict Ownership Scope)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1626))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableTabs.forEach { tabType ->
                        val isSelected = currentTab == tabType
                        val title = when (tabType) {
                            MuseumTabType.VITRINE -> tr("🏛️ Cam Vitrin (${ownedIds.size})", "🏛️ Showcase (${ownedIds.size})")
                            MuseumTabType.LIVE_AUCTION -> tr("🔨 Canlı Müzayede", "🔨 Live Auction")
                            MuseumTabType.MUSEUM_EXHIBITS -> tr("🏛️ Müzedeki Eserler", "🏛️ Museum Exhibits")
                            MuseumTabType.TICKET_REVENUE -> tr("🎟️ Bilet Geliri", "🎟️ Ticket Income")
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentTab = tabType
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFFF59E0B) else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                            ) {
                                CurrencyText(
                                    text = title,
                                    fontSize = if (availableTabs.size > 2) 9.5.sp else 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F172A) else Color.LightGray,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 4. MAIN TAB CONTENT ROUTING
                // ==========================================
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = isLoadingData,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "auctionSkeletonCrossfade"
                    ) { loading ->
                        if (loading) {
                            AntiqueAuctionSkeleton(modifier = Modifier.fillMaxSize())
                        } else {
                            when (currentTab) {
                                MuseumTabType.VITRINE -> {
                                    // TAB 0: GLASS VITRINE SHOWCASE
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(MuseumHeritageManager.allArtifacts, key = { it.id }) { artifact ->
                                            val isOwned = artifact.id in ownedIds
                                            ArtifactGlassCard(
                                                artifact = artifact,
                                                isOwned = isOwned,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedArtifactForDetail = artifact
                                                }
                                            )
                                        }
                                    }
                                }

                                MuseumTabType.LIVE_AUCTION -> {
                                    // TAB 1: LIVE AUCTION HOUSE (Canlı Müzayede Masası - Satılmış Eserler Gösterilmez)
                                    LiveAuctionSection(
                                        viewModel = viewModel,
                                        auctions = allAuctions,
                                        ownedIds = ownedIds,
                                        onlineUid = onlineUid,
                                        onCreateAuctionClick = {
                                            selectedArtifactToAuction = null
                                            showCreateAuctionDialog = true
                                        },
                                        onActionCompleted = {
                                            refreshTrigger++
                                        }
                                    )
                                }

                                MuseumTabType.MUSEUM_EXHIBITS -> {
                                    // TAB 2: MUSEUM EXHIBITS (Müzedeki Eserler - Hangi Eser Hangi Tüccarın Müzesinde)
                                    MuseumExhibitsSection(
                                        artifacts = MuseumHeritageManager.allArtifacts,
                                        registry = globalRegistry,
                                        ownedIds = ownedIds,
                                        auctions = allAuctions,
                                        onlineUid = onlineUid,
                                        onArtifactClick = { artifact ->
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedArtifactForDetail = artifact
                                        },
                                        onGoToAuctionClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentTab = MuseumTabType.LIVE_AUCTION
                                        }
                                    )
                                }

                                MuseumTabType.TICKET_REVENUE -> {
                                    // TAB 3: VISITOR REVENUE & TICKETS
                                    MuseumVisitorRevenueSection(
                                        viewModel = viewModel,
                                        hourlyIncome = hourlyIncome,
                                        unclaimedRevenue = unclaimedRevenue,
                                        totalDailyGems = totalDailyGems,
                                        ownedCount = ownedIds.size,
                                        totalPrestige = totalPrestige,
                                        onClaimed = {
                                            refreshTrigger++
                                        }
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
 * 3D Glass Vitrine artifact card.
 */
@Composable
private fun ArtifactGlassCard(
    artifact: AntiqueArtifact,
    isOwned: Boolean,
    onClick: () -> Unit
) {
    // Add pulsing animation for Masterpiece artifacts
    val isMasterpiece = artifact.rarity == ArtifactRarity.MASTERPIECE
    val infiniteTransition = rememberInfiniteTransition(label = "MasterpiecePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val rarityColor = Color(artifact.rarity.colorHex)
    val borderColor = if (isOwned) {
        if (isMasterpiece) rarityColor.copy(alpha = pulseAlpha) else rarityColor
    } else {
        Color(0xFF1E293B)
    }

    val backgroundBrush = if (isOwned) {
        Brush.verticalGradient(
            listOf(
                rarityColor.copy(alpha = if (isMasterpiece) 0.25f else 0.15f),
                Color(0xFF0F1B2E)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0B1320),
                Color(0xFF070C14)
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(if (isOwned && isMasterpiece) Modifier.shadow(8.dp, RoundedCornerShape(12.dp), spotColor = rarityColor, ambientColor = rarityColor) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(if (isOwned && isMasterpiece) 1.5.dp else 1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glass Vitrine Lighting Display
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isOwned) rarityColor.copy(alpha = if (isMasterpiece) 0.5f else 0.35f) else Color(0xFF1E293B),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            if (isOwned && isMasterpiece) 1.5.dp else 1.dp,
                            if (isOwned) rarityColor.copy(alpha = if (isMasterpiece) pulseAlpha else 0.5f) else Color(0xFF233044),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOwned) {
                        CurrencyText(
                            text = artifact.iconEmoji,
                            fontSize = 38.sp,
                            modifier = Modifier.shadow(if (isMasterpiece) 12.dp else 4.dp, shape = CircleShape, spotColor = rarityColor)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(tr("KİLİTLİ", "LOCKED"), fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }

                // Rarity & Certificate Tag
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val certCode = MuseumHeritageManager.getArtifactCertificateCode(artifact.id)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = rarityColor.copy(alpha = if (isOwned) 0.2f else 0.1f),
                        border = BorderStroke(0.5.dp, rarityColor.copy(alpha = if (isOwned) 1f else 0.5f))
                    ) {
                        CurrencyText(
                            text = "${artifact.rarity.badgeEmoji} ${artifact.rarity.getLocalizedName(isEnglishLanguage())}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = rarityColor.copy(alpha = if (isOwned) 1f else 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8))
                    ) {
                        CurrencyText(
                            text = "$certCode • 1/1",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFBAE6FD),
                            fontFamily = RobotoMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                CurrencyText(
                    text = artifact.getLocalizedName(isEnglishLanguage()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOwned) Color.White else Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                CurrencyText(
                    text = "📍 ${artifact.getLocalizedOriginCity(isEnglishLanguage())}",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )

                if (isOwned) {
                    Divider(color = Color(0xFF1E293B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText("⭐ +${artifact.prestigeScore}", fontSize = 9.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Black)
                        CurrencyText("+₳${formatMoney(artifact.hourlyVisitorIncome)}" + tr("/s", "/h"), fontSize = 9.sp, color = ThemePositive, fontWeight = FontWeight.Black)
                    }
                    val dailyGems = (artifact.baseValue / 15_000_000_000L).toInt().coerceIn(1, 10)
                    CurrencyText("+💎$dailyGems ${tr("Günlük", "Daily")}", fontSize = 9.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Black)
                } else {
                    CurrencyText(
                        text = tr("Müzayedede Bekliyor", "Awaiting Auction"),
                        fontSize = 9.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Live Auction Component supporting both Curator & Player auctions with interactive bidding, buyout, and listing actions.
 * Sold/settled auctions are strictly excluded from general live auction views to prevent invalid bidding.
 */
@Composable
private fun LiveAuctionSection(
    viewModel: GameViewModel,
    auctions: List<com.example.data.MuseumAuctionItem>,
    ownedIds: Set<String>,
    onlineUid: String,
    onCreateAuctionClick: () -> Unit,
    onActionCompleted: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val player by viewModel.player.collectAsStateWithLifecycle()
    val p = player ?: return

    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Tümü, 1: Hazine & Küratör, 2: İlanlarım, 3: Diğer Oyuncular

    val now = System.currentTimeMillis()
    val activeLiveAuctions = remember(auctions) {
        auctions.filter { !it.isSettled && it.endsAtMs > now }
    }
    val myAuctions = remember(auctions) {
        auctions.filter { it.isPlayerSeller && MuseumHeritageManager.isLocalPlayer(context, it.sellerId, onlineUid) }
    }
    val otherPlayersAuctions = remember(auctions) {
        auctions.filter { it.isPlayerSeller && !MuseumHeritageManager.isLocalPlayer(context, it.sellerId, onlineUid) && !it.isSettled && it.endsAtMs > now }
    }
    val treasuryAuctions = remember(auctions) {
        auctions.filter { !it.isPlayerSeller && !it.isSettled && it.endsAtMs > now }
    }

    val filteredAuctions = remember(auctions, selectedFilter) {
        when (selectedFilter) {
            1 -> treasuryAuctions
            2 -> myAuctions
            3 -> otherPlayersAuctions
            else -> {
                // Tümü: Strictly ongoing live auctions + Player's own listings if settled and awaiting cash claim
                auctions.filter {
                    (!it.isSettled && it.endsAtMs > now) ||
                    (it.isPlayerSeller && MuseumHeritageManager.isLocalPlayer(context, it.sellerId, onlineUid) && it.isSettled && it.currentHighestBidder.isNotBlank())
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AuctionPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Banner with Create Auction CTA (Only for artifact owners)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF131D30),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFF3B82F6))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Gavel, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                        Column {
                            CurrencyText(tr("CANLI ANADOLU MÜZAYEDESİ", "LIVE ANATOLIAN AUCTION"), fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White, fontFamily = RobotoMonoFontFamily)
                            CurrencyText(tr("Küratörler, Ahilik Loncaları & Holding İlanları", "Curators, Ahilik Guilds & Holding Listings"), fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    // Pulse indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = pulseAlpha))
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
                                    .background(Color(0xFF10B981))
                            )
                            CurrencyText(tr("CANLI", "LIVE"), color = Color(0xFF6EE7B7), fontWeight = FontWeight.Black, fontSize = 9.sp, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }

                if (ownedIds.isNotEmpty()) {
                    // Add Artifact To Auction Button
                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCreateAuctionClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = "+ " + tr("ESERİMİ MÜZAYEDEYE ÇIKAR (AÇIK ARTIRMA)", "LIST MY ARTIFACT FOR AUCTION"),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText("💡", fontSize = 13.sp)
                            CurrencyText(
                                text = tr("Aşağıdaki canlı müzayedelere teklif vererek ilk tarihi eserinizi müzenize kazandırabilirsiniz.", "Place bids on the live auctions below to win your first historical masterpiece."),
                                fontSize = 9.5.sp,
                                color = Color(0xFF93C5FD),
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val totalDisplayCount = filteredAuctions.size
            val myAuctionsCount = myAuctions.size
            val otherPlayersAuctionsCount = otherPlayersAuctions.size
            val treasuryCount = treasuryAuctions.size

            val filters = if (ownedIds.isNotEmpty() || myAuctionsCount > 0) {
                listOf(
                    tr("🔥 Canlı Müzayedeler", "🔥 Live Auctions") + " (${activeLiveAuctions.size})",
                    tr("👑 Hazine & Küratör", "👑 Treasury & Curator") + " ($treasuryCount)",
                    tr("🏷️ İlanlarım", "🏷️ My Listings") + " ($myAuctionsCount)",
                    tr("🌐 Oyuncu Müzayedeleri", "🌐 Player Auctions") + " ($otherPlayersAuctionsCount)"
                )
            } else {
                listOf(
                    tr("🔥 Canlı Müzayedeler", "🔥 Live Auctions") + " (${activeLiveAuctions.size})",
                    tr("👑 Hazine & Küratör", "👑 Treasury & Curator") + " ($treasuryCount)",
                    tr("🌐 Oyuncu Müzayedeleri", "🌐 Player Auctions") + " ($otherPlayersAuctionsCount)"
                )
            }

            filters.forEachIndexed { index, title ->
                val actualIndex = if (ownedIds.isEmpty() && myAuctionsCount == 0 && index == 2) 3 else index
                val isSelected = selectedFilter == actualIndex
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFilter = actualIndex
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155))
                ) {
                    CurrencyText(
                        text = title,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (filteredAuctions.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F1B2E),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyText("🏷️", fontSize = 32.sp)
                    CurrencyText(
                        text = if (selectedFilter == 2) tr("Henüz müzayedeye çıkardığınız bir eser bulunmuyor.", "You don't have any artifacts listed for auction yet.") else tr("Şu an bu kategoride aktif canlı müzayede bulunmuyor.", "No active live auctions found in this category."),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    if (selectedFilter == 2 && ownedIds.isNotEmpty()) {
                        AppButton(
                            onClick = onCreateAuctionClick,
                            modifier = Modifier.padding(top = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                        ) {
                            CurrencyText(tr("+ Hemen Eser Çıkar", "+ List Artifact Now"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Render Auction Cards
            filteredAuctions.forEach { auctionItem ->
                key(auctionItem.id) {
                    val artifact = MuseumHeritageManager.allArtifacts.find { it.id == auctionItem.artifactId }
                    if (artifact != null) {
                        AuctionCardItem(
                            viewModel = viewModel,
                            auction = auctionItem,
                            artifact = artifact,
                            isPlayerMoney = p.money,
                            isPlayerGems = p.gems,
                            isOwned = artifact.id in ownedIds,
                            pulseAlpha = pulseAlpha,
                            onlineUid = onlineUid,
                            onActionCompleted = onActionCompleted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuctionCardItem(
    viewModel: GameViewModel,
    auction: com.example.data.MuseumAuctionItem,
    artifact: AntiqueArtifact,
    isPlayerMoney: Long,
    isPlayerGems: Int,
    isOwned: Boolean,
    pulseAlpha: Float,
    onlineUid: String,
    onActionCompleted: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val player by viewModel.player.collectAsStateWithLifecycle()
    val p = player ?: return
    val isEnglish = isEnglishLanguage()
    val scope = rememberCoroutineScope()

    val isMasterpiece = artifact.rarity == ArtifactRarity.MASTERPIECE
    val rarityColor = Color(artifact.rarity.colorHex)

    var remainingSeconds by remember(auction.endsAtMs, auction.isSettled) { mutableLongStateOf(0L) }

    LaunchedEffect(auction.endsAtMs, auction.isSettled) {
        while (!auction.isSettled) {
            val rem = ((auction.endsAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            remainingSeconds = rem
            if (rem <= 0L) {
                onActionCompleted()
                break
            }
            delay(1000)
        }
    }

    val mins = (remainingSeconds / 60) % 60
    val secs = remainingSeconds % 60
    val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

    LaunchedEffect(auction.currentHighestBid) {
        if (auction.bidCount > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .then(if (isMasterpiece) Modifier.shadow(12.dp, RoundedCornerShape(12.dp), spotColor = rarityColor) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F1B2E),
        border = BorderStroke(
            if (auction.isPlayerSeller) 1.5.dp else 1.dp,
            if (auction.isPlayerSeller) Color(0xFFF59E0B) else rarityColor.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Seller & Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val certCode = MuseumHeritageManager.getArtifactCertificateCode(artifact.id)
                val isMyListing = auction.isPlayerSeller && MuseumHeritageManager.isLocalPlayer(context, auction.sellerId, onlineUid)

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isMyListing) Color(0xFFF59E0B).copy(alpha = 0.2f) else if (auction.isPlayerSeller) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isMyListing) Color(0xFFF59E0B) else if (auction.isPlayerSeller) Color(0xFF34D399) else Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CurrencyText(
                            text = if (isMyListing) tr("👑 SİZİN İLANINIZ", "👑 YOUR LISTING") + " ($certCode • 1/1)" else if (auction.isPlayerSeller) "🌐 ${auction.sellerName} ($certCode • 1/1)" else "🏛️ ${auction.sellerName} ($certCode • 1/1)",
                            color = if (isMyListing) Color(0xFFFBBF24) else if (auction.isPlayerSeller) Color(0xFF6EE7B7) else Color(0xFF93C5FD),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (auction.isSettled) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ThemePositive.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ThemePositive)
                    ) {
                        CurrencyText(
                            text = tr("✅ SATILDI", "✅ SOLD"),
                            color = ThemePositive,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            fontFamily = RobotoMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = pulseAlpha))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            CurrencyText("⏳", fontSize = 9.sp)
                            CurrencyText(timeStr, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Artifact Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rarityColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, rarityColor),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurrencyText(artifact.iconEmoji, fontSize = 24.sp)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = rarityColor.copy(alpha = 0.2f)
                        ) {
                            CurrencyText(
                                text = "${artifact.rarity.badgeEmoji} ${artifact.rarity.getLocalizedName(isEnglishLanguage())}",
                                color = rarityColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFF10B981))
                        ) {
                            CurrencyText(
                                text = tr("1/1 TEK ESER", "1/1 UNIQUE ARTIFACT"),
                                color = Color(0xFF6EE7B7),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    CurrencyText(artifact.getLocalizedName(isEnglishLanguage()), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    CurrencyText("🏛️ ${artifact.getLocalizedEra(isEnglishLanguage())} • 📍 ${artifact.getLocalizedOriginCity(isEnglishLanguage())}", color = Color.Gray, fontSize = 9.sp)
                }
            }

            Divider(color = Color(0xFF1E3A5F))

            // Bidding Details with AnimatedContent for smooth price transitions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    CurrencyText(if (auction.bidCount > 0) tr("EN YÜKSEK PEY", "HIGHEST BID") else tr("BAŞLANGIÇ FİYATI", "STARTING BID"), fontSize = 8.sp, color = Color.Gray, fontFamily = RobotoMonoFontFamily)
                    AnimatedContent(
                        targetState = auction.currentHighestBid,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                        },
                        label = "bid_price_anim"
                    ) { bidPrice ->
                        CurrencyText(
                            text = "₳${formatMoney(bidPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24),
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    CurrencyText(
                        text = if (auction.bidCount > 0) "${auction.bidCount} " + tr("Pey • Lider", "Bids • Leader") else tr("Pey Durumu", "Bid Status"),
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontFamily = RobotoMonoFontFamily
                    )
                    AnimatedContent(
                        targetState = auction.currentHighestBidder,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                        },
                        label = "bidder_name_anim"
                    ) { bidderName ->
                        CurrencyText(
                            text = if (bidderName.isNotBlank()) bidderName else tr("Henüz Teklif Yok", "No Bids Yet"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bidderName.isNotBlank()) ThemeNeonCyan else Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }

            // Action Buttons
            val isMyListing = auction.isPlayerSeller && MuseumHeritageManager.isLocalPlayer(context, auction.sellerId, onlineUid)
            if (isMyListing) {
                // ==========================================
                // PLAYER'S OWN LISTING CONTROLS
                // ==========================================
                if (auction.isSettled || (remainingSeconds <= 0L && auction.currentHighestBidder.isNotBlank())) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = ThemePositive.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ThemePositive)
                    ) {
                        CurrencyText(
                            text = tr("🎉 Eseriniz ", "🎉 Your artifact was purchased by ") + auction.currentHighestBidder + tr(" bedelle satın alındı!", " for ₳" + formatMoney(auction.currentHighestBid) + "!"),
                            color = ThemePositive,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val earned = MuseumHeritageManager.claimPlayerAuctionProceeds(context, auction.id)
                            if (earned > 0L) {
                                viewModel.addMoneyDirectly(earned)
                                SmartNotificationManager.show(tr("💰 +₳${formatMoney(earned)} müzayede hasılatı holding kasasına aktarıldı!", "💰 +₳${formatMoney(earned)} auction revenue transferred to treasury!", isEnglish), NotificationType.SUCCESS)
                                onActionCompleted()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.Black)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(tr("HASILATI TAHSİL ET (+₳", "CLAIM REVENUE (+₳") + "${formatMoney(auction.currentHighestBid)})", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                } else if (auction.bidCount > 0 && auction.currentHighestBidder.isNotBlank()) {
                    // Active player auction with incoming bids
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val earned = MuseumHeritageManager.claimPlayerAuctionProceeds(context, auction.id)
                                if (earned > 0L) {
                                    viewModel.addMoneyDirectly(earned)
                                    SmartNotificationManager.show(tr("✅ Teklif kabul edildi! +₳${formatMoney(earned)} kasaya eklendi.", "✅ Bid accepted! +₳${formatMoney(earned)} added to treasury.", isEnglish), NotificationType.SUCCESS)
                                    onActionCompleted()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                        ) {
                            CurrencyText(tr("✅ TEKLİFİ KABUL ET", "✅ ACCEPT BID"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                SmartNotificationManager.show(tr("⏳ Müzayede devam ediyor, koleksiyonerler yeni peyler sürebilir.", "⏳ Auction is ongoing, collectors can place new bids.", isEnglish), NotificationType.INFO)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F), contentColor = Color.White)
                        ) {
                            CurrencyText(tr("⏳ SÜREYİ BEKLE", "⏳ WAIT FOR TIME"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Active player auction with NO bids yet -> Can cancel and get artifact back
                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val canceled = MuseumHeritageManager.cancelPlayerMuseumAuction(context, auction.id)
                            if (canceled) {
                                SmartNotificationManager.show(tr("İlan iptal edildi, '", "Listing canceled, '", isEnglish) + artifact.getLocalizedName(isEnglish) + tr("' müze vitrininize geri döndü.", "' returned to your museum showcase.", isEnglish), NotificationType.INFO)
                                onActionCompleted()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color(0xFFFCA5A5))
                    ) {
                        CurrencyText(tr("❌ İLANI İPTAL ET (ESERİ VİTRİNE GERİ AL)", "❌ CANCEL LISTING (RECLAIM TO VITRINE)"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // ==========================================
                // NPC / CURATOR AUCTION BIDDING CONTROLS
                // ==========================================
                if (isOwned) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = ThemePositive.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ThemePositive)
                    ) {
                        CurrencyText(
                            text = tr("✅ Bu eser zaten müzenizde sergilenmektedir.", "✅ This artifact is already exhibited in your museum."),
                            color = ThemePositive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                } else {
                    val playerUniqueId = MuseumHeritageManager.getUniquePlayerId(context)
                    val playerName = if (isEnglish) "You (${p.name.ifBlank { "Holding" }})" else "Siz (${p.name.ifBlank { "Holding" }})"
                    val isCurrentLeader = auction.currentHighestBidderId == playerUniqueId

                    if (isCurrentLeader) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurrencyText("👑", fontSize = 14.sp)
                                CurrencyText(
                                    text = tr("Şu an en yüksek teklif sizde! Süre bittiğinde eser müzenize teslim edilecek.", "You currently have the highest bid! Artifact will be transferred to your museum when time ends."),
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    CurrencyText(
                        text = tr("CANLI MÜZAYEDEYE KATIL (PEY SÜR):", "PARTICIPATE IN LIVE AUCTION (PLACE BID):"),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = RobotoMonoFontFamily,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    val currentPrice = auction.currentHighestBid
                    val step1 = (currentPrice * 0.02).toLong().coerceAtLeast(10_000_000L)
                    val step2 = (currentPrice * 0.05).toLong().coerceAtLeast(25_000_000L)
                    val step3 = (currentPrice * 0.10).toLong().coerceAtLeast(50_000_000L)
                    val step4 = (currentPrice * 0.25).toLong().coerceAtLeast(100_000_000L)
                    val bidSteps = listOf(step1, step2, step3, step4)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // İlk satır: 10M ve 25M
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            bidSteps.take(2).forEach { step ->
                                val newBid = auction.currentHighestBid + step
                                var isProcessingBid by remember { mutableStateOf(false) }
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isPlayerMoney >= newBid && isPlayerGems >= 10) {
                                            isProcessingBid = true
                                            viewModel.placeMuseumAuctionBid(context, auction.id, newBid) {
                                                isProcessingBid = false
                                                onActionCompleted()
                                            }
                                        } else {
                                            SmartNotificationManager.show(tr("Yetersiz bakiye veya elmas (10💎 gerekli)!", "Insufficient balance or diamonds (10💎 required)!", isEnglish), NotificationType.ALERT)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E3A5F),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = !isProcessingBid
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        CurrencyText("+₳${com.example.ui.components.formatMoney(step)} PEY", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                                        CurrencyText("Ücret: 10💎", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                                    }
                                }
                            }
                        }
                        // İkinci satır: 50M ve 100M
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            bidSteps.drop(2).forEach { step ->
                                val newBid = auction.currentHighestBid + step
                                var isProcessingBid by remember { mutableStateOf(false) }
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isPlayerMoney >= newBid && isPlayerGems >= 10) {
                                            isProcessingBid = true
                                            viewModel.placeMuseumAuctionBid(context, auction.id, newBid) {
                                                isProcessingBid = false
                                                onActionCompleted()
                                            }
                                        } else {
                                            SmartNotificationManager.show(tr("Yetersiz bakiye veya elmas (10💎 gerekli)!", "Insufficient balance or diamonds (10💎 required)!", isEnglish), NotificationType.ALERT)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = !isProcessingBid
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        CurrencyText("+₳${com.example.ui.components.formatMoney(step)} PEY!", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                                        CurrencyText("Ücret: 10💎", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    }
                                }
                            }
                        }
                    }

                    // Live auction rule banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        border = BorderStroke(0.5.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CurrencyText("⏱️", fontSize = 11.sp)
                            CurrencyText(
                                text = tr(
                                    "1 Saatlik Canlı Tur • Süre bitiminde en yüksek teklif sahibi eseri kazanır.",
                                    "1-Hour Live Round • Highest bidder wins the 1/1 artifact when timer ends."
                                ),
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Museum Visitor Revenue collection panel.
 */
@Composable
private fun MuseumVisitorRevenueSection(
    viewModel: GameViewModel,
    hourlyIncome: Long,
    unclaimedRevenue: Long,
    ownedCount: Int,
    totalPrestige: Int,
    totalDailyGems: Int,
    onClaimed: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val profileBonusRevenue = remember(unclaimedRevenue) { MuseumHeritageManager.getUnclaimedProfileVisitRevenue(context) }
    val totalInspections = remember(unclaimedRevenue) { MuseumHeritageManager.getTotalProfileInspections(context) }
    val isEnglish = isEnglishLanguage()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vault Cashout Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF102035),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemePositive, ThemeNeonCyan)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CurrencyText(tr("🎟️ BİRİKEN MÜZE BİLET GELİRİ", "🎟️ ACCUMULATED MUSEUM TICKET REVENUE"), fontSize = 11.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan, fontFamily = RobotoMonoFontFamily)

                CurrencyText(
                    text = "₳${formatMoney(unclaimedRevenue)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemePositive,
                    fontFamily = RobotoMonoFontFamily
                )

                CurrencyText(
                    text = tr(
                        "Müzenizdeki $ownedCount adet tarihi şaheser, her saat yerli ve yabancı ziyaretçilerden +${formatCredit(hourlyIncome)} bilet geliri üretir ve günlük +💎$totalDailyGems elmas kazandırır.",
                        "The $ownedCount historical masterpieces in your museum generate +${formatCredit(hourlyIncome)} in ticket revenue from local and foreign visitors every hour and yield +💎$totalDailyGems daily gems."
                    ),
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                AppButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (unclaimedRevenue > 0L) {
                            val earned = MuseumHeritageManager.claimVisitorRevenue(context)
                            viewModel.addMoneyDirectly(earned)
                            SmartNotificationManager.show(tr("🎟️ +${formatCredit(earned)} müze hasılatı holding kasasına aktarıldı!", "🎟️ +${formatCredit(earned)} museum revenue transferred to treasury!", isEnglish), NotificationType.SUCCESS)
                            onClaimed()
                        } else {
                            SmartNotificationManager.show(tr("Henüz birikmiş bilet hasılatı bulunmuyor.", "There is no accumulated ticket revenue yet.", isEnglish), NotificationType.INFO)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.Black)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        CurrencyText(tr("HASILATI KASAYA AKTAR", "COLLECT REVENUE TO VAULT"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // Leaderboard & Profile Views Revenue Explanation Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F1E33),
            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyText("💡", fontSize = 18.sp)
                    CurrencyText(
                        text = tr("BİLET HASILATI & SIRALAMA ZİYARETÇİLERİ", "TICKET REVENUE & RANKING VISITORS"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ThemeNeonCyan,
                        fontFamily = RobotoMonoFontFamily
                    )
                }

                CurrencyText(
                    text = stringResource(R.string.museum_ticket_lore),
                    fontSize = 11.sp,
                    color = Color.White,
                    lineHeight = 16.sp
                )

                Divider(color = Color(0xFF1E3A5F))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CurrencyText(stringResource(R.string.museum_passive_hourly_income_label), fontSize = 11.sp, color = Color.Gray)
                    CurrencyText(stringResource(R.string.museum_hourly_income_format, formatMoney(hourlyIncome)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemePositive)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CurrencyText(stringResource(R.string.museum_profile_revenue_label), fontSize = 11.sp, color = Color.Gray)
                    CurrencyText(stringResource(R.string.museum_profile_revenue_format, formatMoney(profileBonusRevenue), totalInspections), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                }

                CurrencyText(
                    text = stringResource(R.string.museum_strategy_tip),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Prestige Rank Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyText(stringResource(R.string.museum_curator_level_title), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                CurrencyText(
                    text = when {
                        totalPrestige >= 10000 -> tr("👑 İMPARATORLUK BAŞ MÜZESİ (En Üst Düzey)", "👑 IMPERIAL CHIEF MUSEUM (Top Tier)")
                        totalPrestige >= 5000 -> tr("🏛️ BÜYÜK ANADOLU MİRAS MÜZESİ", "🏛️ GRAND ANATOLIAN HERITAGE MUSEUM")
                        totalPrestige >= 2000 -> tr("✨ SAYGIN HANEDAN VİTRİNİ", "✨ ESTEEMED DYNASTY VITRINE")
                        totalPrestige > 0 -> tr("📜 YEREL ANTİKA GALERİSİ", "📜 LOCAL ANTIQUE GALLERY")
                        else -> tr("📦 HENÜZ ESER BULUNMUYOR (Müzayedelere Katılın)", "📦 NO ARTIFACTS YET (Participate in Auctions)")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                CurrencyText(
                    text = stringResource(R.string.museum_prestige_lore),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Museum Exhibits Section (Müzedeki Eserler) displaying all 13 Anatolian heritage artifacts
 * and indicating which merchant / collector / ministry museum currently exhibits each one.
 */
@Composable
private fun MuseumExhibitsSection(
    artifacts: List<AntiqueArtifact>,
    registry: Map<String, com.example.data.MuseumArtifactRegistryItem>,
    ownedIds: Set<String>,
    auctions: List<MuseumAuctionItem>,
    onlineUid: String,
    onArtifactClick: (AntiqueArtifact) -> Unit,
    onGoToAuctionClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isEnglish = isEnglishLanguage()

    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Tümü (13), 1: Sizin Müzenizde, 2: Oyuncu Müzelerinde, 3: Canlı Müzayedede, 4: Bakanlık Envanterinde

    val now = System.currentTimeMillis()

    // Helper classification for each artifact
    val categorizedArtifacts = remember(artifacts, registry, ownedIds, auctions) {
        artifacts.map { art ->
            val reg = registry[art.id]
            val activeAuction = auctions.find { it.artifactId == art.id && !it.isSettled && it.endsAtMs > now }
            val isLocalOwned = art.id in ownedIds || (reg != null && reg.status == "OWNED_BY_PLAYER" && MuseumHeritageManager.isLocalPlayer(context, reg.ownerId ?: "", onlineUid))
            val isOtherOwned = !isLocalOwned && reg != null && reg.status == "OWNED_BY_PLAYER" && !reg.ownerName.contains("Bakanlığı", ignoreCase = true)
            val isOnAuction = activeAuction != null
            val isTreasury = !isLocalOwned && !isOtherOwned && !isOnAuction

            val category = when {
                isLocalOwned -> 1
                isOtherOwned -> 2
                isOnAuction -> 3
                else -> 4
            }

            val ownerDisplayName = when {
                isLocalOwned -> reg?.ownerName?.ifBlank { "Siz (Holding)" } ?: "Siz (Holding)"
                isOtherOwned -> reg?.ownerName ?: "Tüccar"
                isOnAuction -> activeAuction?.sellerName ?: "Küratör"
                else -> "T.C. Kültür ve Turizm Bakanlığı"
            }

            MuseumExhibitItemData(
                artifact = art,
                reg = reg,
                activeAuction = activeAuction,
                category = category,
                ownerDisplayName = ownerDisplayName
            )
        }
    }

    val filteredList = remember(categorizedArtifacts, selectedFilter) {
        when (selectedFilter) {
            1 -> categorizedArtifacts.filter { it.category == 1 }
            2 -> categorizedArtifacts.filter { it.category == 2 }
            3 -> categorizedArtifacts.filter { it.category == 3 }
            4 -> categorizedArtifacts.filter { it.category == 4 }
            else -> categorizedArtifacts
        }
    }

    val localCount = categorizedArtifacts.count { it.category == 1 }
    val otherCount = categorizedArtifacts.count { it.category == 2 }
    val auctionCount = categorizedArtifacts.count { it.category == 3 }
    val treasuryCount = categorizedArtifacts.count { it.category == 4 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Lore Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF101B2E),
            border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFFF59E0B))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyText("🏛️", fontSize = 22.sp)
                    Column {
                        CurrencyText(
                            text = tr("MÜZEDEKİ ESERLER & ENVANTER", "MUSEUM EXHIBITS & INVENTORY"),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFamily = RobotoMonoFontFamily
                        )
                        CurrencyText(
                            text = tr("13 Eşsiz 1/1 Tarihi Eserin Güncel Sahipleri", "Current Owners of the 13 Unique 1/1 Masterpieces"),
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                CurrencyText(
                    text = tr(
                        "Anadolu'nun kadim medeniyetlerine ait 13 benzersiz tarihi eser; holding müzelerinde, koleksiyoner vitrinlerinde veya bakanlık korumasında sergilenmektedir.",
                        "The 13 unique historical masterpieces of ancient Anatolian civilizations are exhibited in holding museums, collector vitrines, or ministry heritage registries."
                    ),
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    lineHeight = 14.sp
                )
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                tr("🔥 Tümü", "🔥 All") + " (${artifacts.size})",
                tr("👑 Sizin Müzenizde", "👑 Your Museum") + " ($localCount)",
                tr("🏛️ Oyuncu Müzelerinde", "🏛️ Trader Museums") + " ($otherCount)",
                tr("🔨 Canlı Müzayedede", "🔨 On Auction") + " ($auctionCount)",
                tr("📜 Bakanlık Envanteri", "📜 Ministry Registry") + " ($treasuryCount)"
            )

            filters.forEachIndexed { index, label ->
                val isSelected = selectedFilter == index
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFilter = index
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155))
                ) {
                    CurrencyText(
                        text = label,
                        color = if (isSelected) Color(0xFF0F172A) else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F1B2E),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyText("🔍", fontSize = 28.sp)
                    CurrencyText(
                        text = tr("Bu filtreye uygun eser bulunamadı.", "No artifacts found for this filter."),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            filteredList.forEach { item ->
                val artifact = item.artifact
                val reg = item.reg
                val activeAuction = item.activeAuction
                val category = item.category
                val ownerDisplayName = item.ownerDisplayName
                val rarityColor = Color(artifact.rarity.colorHex)
                val certCode = MuseumHeritageManager.getArtifactCertificateCode(artifact.id)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onArtifactClick(artifact) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1B2E),
                    border = BorderStroke(
                        1.2.dp,
                        when (category) {
                            1 -> Color(0xFFF59E0B)
                            2 -> Color(0xFF38BDF8)
                            3 -> Color(0xFF34D399)
                            else -> Color(0xFF334155)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Location & Owner Badge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (category) {
                                    1 -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    2 -> Color(0xFF0284C7).copy(alpha = 0.2f)
                                    3 -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    else -> Color(0xFF334155)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (category) {
                                        1 -> Color(0xFFF59E0B)
                                        2 -> Color(0xFF38BDF8)
                                        3 -> Color(0xFF34D399)
                                        else -> Color(0xFF475569)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CurrencyText(
                                        text = when (category) {
                                            1 -> tr("👑 SİZİN MÜZENİZDE SERGİLENİYOR", "👑 EXHIBITED IN YOUR MUSEUM")
                                            2 -> "🏛️ $ownerDisplayName " + tr("MÜZESİNDE SERGİLENİYOR", "MUSEUM EXHIBIT")
                                            3 -> tr("🔨 CANLI MÜZAYEDEDE AÇIK ARTIRMADA", "🔨 LIVE ON AUCTION")
                                            else -> tr("🏛️ T.C. KÜLTÜR VE TURİZM BAKANLIĞI", "🏛️ MINISTRY OF CULTURE HERITAGE")
                                        },
                                        color = when (category) {
                                            1 -> Color(0xFFFBBF24)
                                            2 -> Color(0xFF93C5FD)
                                            3 -> Color(0xFF6EE7B7)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 1/1 Unique tag
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                CurrencyText(
                                    text = "$certCode • 1/1",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Artifact Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = rarityColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, rarityColor),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CurrencyText(artifact.iconEmoji, fontSize = 24.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = rarityColor.copy(alpha = 0.2f)
                                    ) {
                                        CurrencyText(
                                            text = "${artifact.rarity.badgeEmoji} ${artifact.rarity.getLocalizedName(isEnglish)}",
                                            color = rarityColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                CurrencyText(artifact.getLocalizedName(isEnglish), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                CurrencyText("🏛️ ${artifact.getLocalizedEra(isEnglish)} • 📍 ${artifact.getLocalizedOriginCity(isEnglish)}", color = Color.Gray, fontSize = 9.sp)
                            }
                        }

                        Divider(color = Color(0xFF1E3A5F))

                        // Stats and Lore Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                CurrencyText(
                                    text = when (category) {
                                        1 -> tr("Bilet Geliri: +${formatCredit(artifact.hourlyVisitorIncome)}/saat", "Ticket Revenue: +${formatCredit(artifact.hourlyVisitorIncome)}/hr")
                                        2 -> tr("Son Değerleme: ${formatCredit(reg?.lastPrice?.coerceAtLeast(artifact.baseValue) ?: artifact.baseValue)}", "Valuation: ${formatCredit(reg?.lastPrice?.coerceAtLeast(artifact.baseValue) ?: artifact.baseValue)}")
                                        3 -> tr("En Yüksek Pey: ${formatCredit(activeAuction?.currentHighestBid ?: artifact.baseValue)}", "Highest Bid: ${formatCredit(activeAuction?.currentHighestBid ?: artifact.baseValue)}")
                                        else -> tr("Taban Değer: ${formatCredit(artifact.baseValue)}", "Base Value: ${formatCredit(artifact.baseValue)}")
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (category) {
                                        1 -> ThemePositive
                                        2 -> Color(0xFF93C5FD)
                                        3 -> Color(0xFFFBBF24)
                                        else -> Color.LightGray
                                    },
                                    fontFamily = RobotoMonoFontFamily
                                )
                                CurrencyText("⭐ +${artifact.prestigeScore} " + tr("Müze Prestiji", "Prestige"), fontSize = 8.5.sp, color = Color(0xFFFBBF24))
                            }

                            if (category == 3) {
                                AppButton(
                                    onClick = onGoToAuctionClick,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                                ) {
                                    CurrencyText(tr("🔨 Müzayedeye Git", "🔨 Go to Auction"), fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    CurrencyText(
                                        text = tr("🔍 Detay & Tarihçe", "🔍 Lore & Details"),
                                        color = Color.LightGray,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

private data class MuseumExhibitItemData(
    val artifact: AntiqueArtifact,
    val reg: com.example.data.MuseumArtifactRegistryItem?,
    val activeAuction: MuseumAuctionItem?,
    val category: Int,
    val ownerDisplayName: String
)
@Composable
private fun ArtifactDetailDialog(
    artifact: AntiqueArtifact,
    isOwned: Boolean,
    onDismiss: () -> Unit,
    onListForAuction: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rarityColor = remember(artifact.rarity.colorHex) { Color(artifact.rarity.colorHex) }
    val borderStroke = remember(rarityColor) { BorderStroke(1.5.dp, rarityColor) }
    val certCode = remember(artifact.id) { MuseumHeritageManager.getArtifactCertificateCode(artifact.id) }
    val formattedIncome = remember(artifact.hourlyVisitorIncome) { formatMoney(artifact.hourlyVisitorIncome) }
    val scrollState = rememberScrollState()

    val actionButtonColors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
    val dismissButtonColors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0D1524),
            border = borderStroke
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyText(artifact.iconEmoji, fontSize = 28.sp)
                        Column {
                            CurrencyText(artifact.getLocalizedName(isEnglishLanguage()), fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                            CurrencyText(artifact.getLocalizedEra(isEnglishLanguage()), fontSize = 10.sp, color = rarityColor)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFF1E3A5F))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F2338),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText("🏛️", fontSize = 14.sp)
                            CurrencyText(
                                tr("T.C. VAKIFLAR GENEL MÜDÜRLÜĞÜ", "REPUBLIC OF TÜRKİYE GENERAL DIRECTORATE OF FOUNDATIONS"),
                                color = Color(0xFF38BDF8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        CurrencyText(
                            tr("1/1 TESCİLLİ ULUSAL KÜLTÜR MİRASI BELGESİ", "1/1 REGISTERED NATIONAL CULTURAL HERITAGE CERTIFICATE"),
                            color = Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText(tr("Sertifika Seri No:", "Certificate Serial No:"), color = Color.Gray, fontSize = 9.sp, fontFamily = RobotoMonoFontFamily)
                            CurrencyText(certCode, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurrencyText(tr("Toplam Üretim / Adet:", "Total Production / Quantity:"), color = Color.Gray, fontSize = 9.sp, fontFamily = RobotoMonoFontFamily)
                            CurrencyText(tr("1 ADET (BENZERSİZ TEK ESER)", "1 UNIT (UNIQUE MASTERPIECE)"), color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }

                CurrencyText(tr("📜 TARİHİ VE KÜLTÜREL ÖNEMİ:", "📜 HISTORICAL & CULTURAL SIGNIFICANCE:"), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24), fontFamily = RobotoMonoFontFamily)
                CurrencyText(artifact.getLocalizedHistoricalLore(isEnglishLanguage()), fontSize = 11.sp, color = Color.LightGray, lineHeight = 16.sp)

                Divider(color = Color(0xFF1E3A5F))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CurrencyText(tr("Menşe Şehir:", "City of Origin:"), fontSize = 11.sp, color = Color.Gray)
                    CurrencyText("📍 ${artifact.getLocalizedOriginCity(isEnglishLanguage())}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CurrencyText(tr("Kalıcı Müze Prestiji:", "Permanent Museum Prestige:"), fontSize = 11.sp, color = Color.Gray)
                    CurrencyText("⭐ +${artifact.prestigeScore} " + tr("Puan", "Points"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CurrencyText(tr("Saatlik Bilet Getirisi:", "Hourly Ticket Revenue:"), fontSize = 11.sp, color = Color.Gray)
                    CurrencyText("+${formatCredit(artifact.hourlyVisitorIncome)}" + tr("/saat", "/hour"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemePositive)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isOwned) {
                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onListForAuction()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = actionButtonColors
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(tr("🔨 MÜZAYEDEDE SATIŞA ÇIKAR", "🔨 SELL ON AUCTION"), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }

                AppButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = dismissButtonColors
                ) {
                    CurrencyText(tr("KAPAT", "CLOSE"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Interactive Dialog for listing an owned artifact for live auction.
 */
@Composable
private fun CreateMuseumAuctionDialog(
    ownedArtifacts: List<AntiqueArtifact>,
    preSelectedArtifact: AntiqueArtifact? = null,
    onDismiss: () -> Unit,
    onAuctionCreated: (AntiqueArtifact, Long, Long, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedArtifact by remember {
        mutableStateOf(preSelectedArtifact ?: ownedArtifacts.firstOrNull())
    }

    val currentArtifact = selectedArtifact

    var startingPricePercent by remember { mutableFloatStateOf(0.75f) } // 75%
    var selectedDurationMinutes by remember { mutableIntStateOf(60) } // 60 mins (1 Hour default)

    val baseValue = currentArtifact?.baseValue ?: 100_000_000L
    val calculatedStartBid = (baseValue * startingPricePercent).toLong()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0D1524),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFF3B82F6))))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyText("🔨", fontSize = 22.sp)
                        Column {
                            CurrencyText(tr("CANLI MÜZAYEDE İLANI VER", "CREATE LIVE AUCTION LISTING"), fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White, fontFamily = RobotoMonoFontFamily)
                            CurrencyText(tr("Eserinizi 1 Saatlik Canlı Açık Artırmaya Sunun", "Offer your artifact for a 1-hour live auction"), fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = Color(0xFF1E3A5F))

                if (ownedArtifacts.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText("🏛️", fontSize = 28.sp)
                            CurrencyText(tr("Müzenizde henüz sergilenen bir eser bulunmuyor.", "There is no artifact exhibited in your museum yet."), color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                            CurrencyText(tr("Önce Canlı Müzayede'den bir eser kazanarak koleksiyonunuza eklemelisiniz.", "You must first win an artifact from the Live Auction to add to your collection."), color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    // Artifact Selector Chips if multiple owned
                    if (ownedArtifacts.size > 1) {
                        CurrencyText(tr("SATIŞA SUNULACAK ESERİ SEÇİN:", "SELECT ARTIFACT FOR SALE:"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = RobotoMonoFontFamily)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ownedArtifacts.forEach { item ->
                                val isSelected = currentArtifact?.id == item.id
                                val rColor = Color(item.rarity.colorHex)
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedArtifact = item
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) rColor.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) rColor else Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CurrencyText(item.iconEmoji, fontSize = 14.sp)
                                        CurrencyText(
                                            text = item.getLocalizedName(isEnglishLanguage()),
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected Artifact Preview Card
                    if (currentArtifact != null) {
                         val rColor = Color(currentArtifact.rarity.colorHex)
                         Surface(
                             modifier = Modifier.fillMaxWidth(),
                             shape = RoundedCornerShape(10.dp),
                             color = Color(0xFF0F1B2E),
                             border = BorderStroke(1.dp, rColor.copy(alpha = 0.6f))
                         ) {
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(10.dp),
                                 horizontalArrangement = Arrangement.spacedBy(10.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Surface(
                                     shape = RoundedCornerShape(8.dp),
                                     color = rColor.copy(alpha = 0.2f),
                                     border = BorderStroke(1.dp, rColor),
                                     modifier = Modifier.size(44.dp)
                                 ) {
                                     Box(contentAlignment = Alignment.Center) {
                                         CurrencyText(currentArtifact.iconEmoji, fontSize = 22.sp)
                                     }
                                 }
                                 Column(modifier = Modifier.weight(1f)) {
                                     CurrencyText(currentArtifact.getLocalizedName(isEnglishLanguage()), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                     CurrencyText(tr("Eser Taban Değeri: ", "Artifact Base Value: ") + formatCredit(currentArtifact.baseValue), color = Color(0xFFFBBF24), fontSize = 10.sp, fontFamily = RobotoMonoFontFamily)
                                 }
                             }
                         }

                        // Starting Bid Presets
                        CurrencyText(tr("AÇILIŞ PEYİ (BAŞLANGIÇ FİYATI):", "STARTING BID (OPENING PRICE):"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = RobotoMonoFontFamily)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val startPresets = listOf(
                                0.50f to "%50",
                                0.75f to "%75",
                                1.00f to "%100",
                                1.25f to "%125"
                            )
                            startPresets.forEach { (pct, label) ->
                                val isSel = startingPricePercent == pct
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        startingPricePercent = pct
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) Color(0xFF3B82F6) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF60A5FA) else Color(0xFF334155))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CurrencyText(label, color = if (isSel) Color.White else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        CurrencyText(formatCredit((baseValue * pct).toLong()), color = if (isSel) Color(0xFFBFDBFE) else Color.Gray, fontSize = 8.sp, fontFamily = RobotoMonoFontFamily)
                                    }
                                }
                            }
                        }

                        // Auction Duration
                        CurrencyText(tr("MÜZAYEDE SÜRESİ:", "AUCTION DURATION:"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = RobotoMonoFontFamily)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val durations = listOf(
                                15 to tr("⏱️ 15 Dk", "⏱️ 15 Min"),
                                30 to tr("⏱️ 30 Dk", "⏱️ 30 Min"),
                                60 to tr("⏱️ 1 Saat", "⏱️ 1 Hour")
                            )
                            durations.forEach { (dur, label) ->
                                val isSel = selectedDurationMinutes == dur
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedDurationMinutes = dur
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) Color(0xFF10B981) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF34D399) else Color(0xFF334155))
                                ) {
                                    CurrencyText(
                                        text = label,
                                        color = if (isSel) Color.Black else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Info Note
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E3A5F).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CurrencyText("💡", fontSize = 16.sp)
                                CurrencyText(
                                    text = tr(
                                        "Eseriniz canlı açık artırmaya sunulduğunda, diğer oyuncular ve koleksiyonerler canlı teklifler vermeye başlayacaktır. Süre bitiminde en yüksek teklif sahibine otomatik devredilir ve hasılat kasanıza aktarılır.",
                                        "When your artifact is listed for live auction, players and collectors will place live bids. When timer ends, it is automatically transferred to the highest bidder and proceeds go to your treasury."
                                    ),
                                    fontSize = 10.sp,
                                    color = Color(0xFF93C5FD),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Confirm Button
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAuctionCreated(
                                    currentArtifact,
                                    calculatedStartBid,
                                    0L,
                                    selectedDurationMinutes
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(tr("🔨 CANLI MÜZAYEDEYİ BAŞLAT", "🔨 START LIVE AUCTION"), fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = RobotoMonoFontFamily)
                            }
                        }
                    }
                }
            }
        }
    }
}
