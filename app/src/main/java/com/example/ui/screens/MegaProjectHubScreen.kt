package com.example.ui.screens

import com.example.viewmodel.*
import com.example.R

import com.example.ui.components.UniversalProductIcon
import com.example.ui.components.ProductDrawables
import com.example.ui.components.CurrencyText

import com.example.ui.components.Tier4PremiumIcon
import com.example.ui.components.formatCredit
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemeGold


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.MegaProjectHubSkeleton
import com.example.ui.components.luxeShimmerBorder
import com.example.ui.components.CorporateStampOverlay
import com.example.ui.components.AppButton
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationType
import com.example.ui.components.formatMoney
import com.example.ui.components.ConsortiumAssemblyLineCanvas
import com.example.ui.components.ConsortiumContributionDonutChart
import com.example.ui.components.ConsortiumRadioSosBroadcastBar
import com.example.ui.components.OneTapWarehouseSyncBadge
import com.example.ui.components.ConsortiumHonorPodium
import com.example.ui.components.ConsortiumBoardVotingCard
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.tr
import com.example.ui.theme.isEnglishLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MegaProjectHubScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToFacilities: () -> Unit = {},
    onNavigateToBorsa: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val megaProjects = uiState.consortiumState.megaProjects
    val inventory = uiState.inventoryState.items
    val playerEntity = uiState.playerState.player
    val consortiumChatMessages = uiState.consortiumState.chatMessages
    val crisisState = uiState.consortiumState.crisisState
    val allProducts = Product.values()
    val tier4Products = remember(allProducts) { allProducts.filter { it.tier == ProductTier.TIER_4 } }
    val isEnglish = isEnglishLanguage()
    val isOnlineRegistered by viewModel.isOnlineRegistered.collectAsStateWithLifecycle()

    if (!isOnlineRegistered) {
        com.example.ui.components.OnlineFeatureLockGate(
            feature = com.example.ui.components.LockFeatureType.CONSORTIUM,
            viewModel = viewModel,
            onNavigateHome = onNavigateBack
        )
        return
    }

    var viewMode by remember { mutableStateOf(ConsortiumViewMode.HUB) }
    var selectedTabFilter by remember { mutableStateOf(0) } // 0: Tümü, 1: Ortak Arayanlar/Kurulum, 2: Ortaklıklarım, 3: Tamamlanan
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDeliverySlotProject by remember { mutableStateOf<Pair<MegaProject, ConsortiumSupplierSlot>?>(null) }
    var activeChatProject by remember { mutableStateOf<MegaProject?>(null) }
    var selectedGridProject by remember { mutableStateOf<MegaProject?>(null) } // Used for detailed modal view

    var isLoadingProjects by remember { mutableStateOf(true) }
    var hubProjects by remember { mutableStateOf<List<MegaProject>>(emptyList()) }
    var managedProjects by remember { mutableStateOf<List<MegaProject>>(emptyList()) }
    var openSlotsProjectsCount by remember { mutableIntStateOf(0) }
    var myProjectsCount by remember { mutableIntStateOf(0) }
    var completedProjectsCount by remember { mutableIntStateOf(0) }
    var totalBrandReputation by remember { mutableIntStateOf(0) }

    // Poll Supabase periodically while screen is open to reflect instant changes
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                com.example.data.MultiplayerManager.refreshGuildsFromSupabase()
            }
            kotlinx.coroutines.delay(3000L)
        }
    }

    // Offload project categorization, statistics and filtering to Dispatchers.IO
    LaunchedEffect(megaProjects, selectedTabFilter, viewMode) {
        withContext(Dispatchers.IO) {
            val pId = playerEntity?.id ?: "local_player"
            val pName = playerEntity?.name ?: ""
            val pEmail = viewModel.onlineEmail.value
            val cleanEmail = pEmail.replace(".", "_")
            val emailPrefix = pEmail.substringBefore("@")

            fun isUserManaged(proj: MegaProject): Boolean {
                if (proj.leaderPlayerId == "local_player" || proj.leaderPlayerId == pId) return true
                if (pName.isNotBlank() && proj.leaderPlayerName == pName) return true
                if (cleanEmail.isNotBlank() && proj.leaderPlayerId.contains(cleanEmail)) return true
                if (emailPrefix.isNotBlank() && proj.leaderPlayerId.contains(emailPrefix)) return true
                if (proj.participatingPartnerIds.contains("local_player") || proj.participatingPartnerIds.contains(pId)) return true
                if (proj.slots.any { slot ->
                    slot.assignedPartnerId == "local_player" ||
                    slot.assignedPartnerId == pId ||
                    (pName.isNotBlank() && slot.assignedPartnerName == pName) ||
                    (cleanEmail.isNotBlank() && slot.assignedPartnerId?.contains(cleanEmail) == true)
                }) return true
                return false
            }

            val hub = if (selectedTabFilter == 0) megaProjects else megaProjects.filter { proj -> proj.slots.any { it.assignedPartnerId == null } && !proj.isAllStagesFinished }
            val managed = megaProjects.filter { proj -> isUserManaged(proj) }
            val open = megaProjects.count { proj -> proj.slots.any { it.assignedPartnerId == null } }
            val my = managed.size
            val comp = megaProjects.count { it.isAllStagesFinished }
            val rep = megaProjects.filter { it.isAllStagesFinished }.sumOf { it.brandReputationGain }

            withContext(Dispatchers.Main) {
                hubProjects = hub
                managedProjects = managed
                openSlotsProjectsCount = open
                myProjectsCount = my
                completedProjectsCount = comp
                totalBrandReputation = rep
                isLoadingProjects = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // ================= NEW HEADER WITH BUTTONS =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (viewMode == ConsortiumViewMode.HUB) ThemeGold.copy(alpha=0.15f) else ThemeNeonCyan.copy(alpha=0.15f),
                                border = BorderStroke(1.dp, if (viewMode == ConsortiumViewMode.HUB) ThemeGold.copy(alpha=0.5f) else ThemeNeonCyan.copy(alpha=0.5f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                if (viewMode == ConsortiumViewMode.HUB) {
                                    Image(
                                        painter = painterResource(id = R.drawable.bg_consortium_header),
                                        contentDescription = tr("Konsorsiyum Merkezi", "Consortium Hub"),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.DashboardCustomize,
                                            contentDescription = null,
                                            tint = ThemeNeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                CurrencyText(
                                    text = if (viewMode == ConsortiumViewMode.HUB) tr("Konsorsiyum Merkezi", "Consortium Hub") else tr("Yönetim Paneli", "Management Board"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                CurrencyText(
                                    text = if (viewMode == ConsortiumViewMode.HUB) tr("Global ortaklık fırsatları", "Global partnership opportunities") else tr("Sahibi ve ortağı olduğun projeler", "Projects you own or partner in"),
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        if (viewMode == ConsortiumViewMode.MANAGEMENT) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { viewMode = ConsortiumViewMode.HUB }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    
                    if (viewMode == ConsortiumViewMode.HUB) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(tr("Konsorsiyum Kur", "Establish Consortium"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            
                            AppButton(
                                onClick = { viewMode = ConsortiumViewMode.MANAGEMENT },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan.copy(alpha = 0.15f), contentColor = ThemeNeonCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(tr("Yönetim Paneli", "Management Board"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                onClick = { selectedTabFilter = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTabFilter == 0) Color(0xFF1E293B) else Color.Transparent,
                                    contentColor = if (selectedTabFilter == 0) Color.White else Color.Gray
                                ),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                CurrencyText(tr("Tüm Konsorsiyumlar", "All Consortiums"), fontWeight = if (selectedTabFilter == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
                            }
                            AppButton(
                                onClick = { selectedTabFilter = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTabFilter == 1) Color(0xFF1E293B) else Color.Transparent,
                                    contentColor = if (selectedTabFilter == 1) Color.White else Color.Gray
                                ),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                CurrencyText(tr("Açık İlanlar", "Open Bids"), fontWeight = if (selectedTabFilter == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
                            }
                        }

                        // ================= 3-STEP QUICK GUIDE ACCORDION =================
                        var showConsortiumGuide by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF131D31),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().clickable { showConsortiumGuide = !showConsortiumGuide }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CurrencyText(
                                            text = tr("💡 Konsorsiyum Nasıl Çalışır? (3 Adımda Rehber)", "💡 How Consortiums Work (3 Steps Guide)"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showConsortiumGuide) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                AnimatedVisibility(visible = showConsortiumGuide) {
                                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                        Row(verticalAlignment = Alignment.Top) {
                                            CurrencyText("1️⃣ ", fontSize = 10.sp)
                                            Column {
                                                CurrencyText(tr("Açık Kotaya Katıl:", "Join an Open Quota:"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemeNeonCyan)
                                                CurrencyText(tr("Projedeki 4 hammadde slotundan birini seçerek %25 kâr payı ortağı olursun.", "Select one of the 4 raw material slots to become a 25% profit partner."), fontSize = 9.sp, color = Color.LightGray)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.Top) {
                                            CurrencyText("2️⃣ ", fontSize = 10.sp)
                                            Column {
                                                CurrencyText(tr("Malzemelerini Teslim Et:", "Deliver Your Materials:"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                                                CurrencyText(tr("Depondaki malları tek tuşla ver, eksikleri pazardan al veya tesisinde üret.", "Deliver inventory items with one tap, buy missing from market or produce in facilities."), fontSize = 9.sp, color = Color.LightGray)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.Top) {
                                            CurrencyText("3️⃣ ", fontSize = 10.sp)
                                            Column {
                                                CurrencyText(tr("Otomatik Temettü Kazan:", "Earn Automatic Dividends:"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                                CurrencyText(tr("Montaj tamamlandığında satılan her partiden payın anında banka hesabına yatar!", "When assembly is complete, your share from every sold batch is paid automatically!"), fontSize = 9.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= CONSORTIUM CRISIS BANNER =================
            AnimatedVisibility(
                visible = crisisState.isCrisisActive,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(250)) + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    ConsortiumCrisisBanner(
                        crisisState = crisisState,
                        allProducts = allProducts
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = isLoadingProjects,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ConsortiumLoadingSkeletonTransition",
                modifier = Modifier.weight(1f)
            ) { loading ->
                if (loading) {
                    MegaProjectHubSkeleton(modifier = Modifier.fillMaxSize())
                } else {
                    AnimatedContent(
                        targetState = viewMode,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.96f, animationSpec = tween(300)))
                                .togetherWith(fadeOut(animationSpec = tween(200)))
                        },
                        label = "ConsortiumViewModeTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { currentMode ->
                        if (currentMode == ConsortiumViewMode.HUB) {
                            // Sadece açık slotu olan veya kurulumdaki projeleri göster
                            if (hubProjects.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF101828).copy(alpha = 0.7f)
                                        ),
                                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(ThemeGold.copy(alpha = 0.6f), ThemeNeonCyan.copy(alpha = 0.6f)))),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape)
                                                        .background(ThemeGold.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Handshake,
                                                        contentDescription = null,
                                                        tint = ThemeGold,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))
                                                CurrencyText(
                                                    text = tr("Piyasada Aktif Proje Bulunmuyor", "No Active Projects on the Market"),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = Color.White,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                CurrencyText(
                                                    text = tr("Şu an piyasada proje yok, ilk Konsorsiyumu sen kur!", "There are no projects on the market right now, establish the first Consortium!"),
                                                    fontSize = 13.sp,
                                                    color = Color.LightGray,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(18.dp))
                                                AppButton(
                                                    onClick = { showCreateDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                                                ) {
                                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    CurrencyText(tr("İlk Konsorsiyumu Sen Kur", "Establish the First Consortium"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                        val minCellSize = if (maxWidth < 600.dp) 300.dp else 340.dp
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = minCellSize),
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(hubProjects, key = { it.id }) { project ->
                                                MegaProjectGridItem(
                                                    project = project,
                                                    onClick = { selectedGridProject = project }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ================= MANAGEMENT VIEW =================
                                // Sadece oyuncunun dahil olduğu projeler
                                if (managedProjects.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF101828).copy(alpha = 0.7f)
                                            ),
                                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(androidx.compose.foundation.shape.CutCornerShape(20.dp))
                                                        .background(Brush.linearGradient(listOf(ThemeNeonCyan.copy(alpha=0.15f), ThemeGold.copy(alpha=0.15f)))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.BusinessCenter,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))
                                                CurrencyText(
                                                    text = tr("Henüz Katıldığınız Bir Konsorsiyum Yok", "You Haven't Joined Any Consortium Yet"),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = Color.White,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                CurrencyText(
                                                    text = tr("Pazarda tedarik kotası arayan konsorsiyumlara katılarak veya kendi projeni başlatarak holdingini büyüt.", "Grow your holding by joining consortiums seeking supply quotas in the market or starting your own project."),
                                                    fontSize = 13.sp,
                                                    color = Color.LightGray,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(18.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    AppButton(
                                                        onClick = { viewMode = ConsortiumViewMode.HUB },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan.copy(alpha = 0.2f), contentColor = ThemeNeonCyan)
                                                    ) {
                                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        CurrencyText(tr("Konsorsiyum Bul", "Find Consortium"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    AppButton(
                                                        onClick = { showCreateDialog = true },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                                                    ) {
                                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        CurrencyText(tr("Konsorsiyum Kur", "Establish Consortium"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                        val minCellSize = if (maxWidth < 600.dp) 300.dp else 340.dp
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Executive Management Summary Bar
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF0F1E36).copy(alpha = 0.8f),
                                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp))
                                                        CurrencyText(
                                                            text = tr("YÖNETİLEN KONSORSİYUMLAR", "MANAGED CONSORTIUMS"),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    CurrencyText(
                                                        text = "${managedProjects.size} " + tr("Aktif Ortaklık", "Active Holdings"),
                                                        color = ThemeNeonCyan,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            }

                                            LazyVerticalGrid(
                                                columns = GridCells.Adaptive(minSize = minCellSize),
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(managedProjects, key = { it.id }) { project ->
                                                    MegaProjectGridItem(
                                                        project = project,
                                                        onClick = { selectedGridProject = project }
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

    // ================= GRID PROJECT DIALOG =================
    selectedGridProject?.let { proj ->
        val currentProjState = megaProjects.find { it.id == proj.id }
        if (currentProjState == null) {
            selectedGridProject = null
        } else {
            // Ortak arayan projeyi modal içinde MegaProjectCard olarak göster
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { selectedGridProject = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF090D16).copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(tr("Konsorsiyum İncelemesi", "Consortium Details"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { selectedGridProject = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            MegaProjectCard(
                                uiState = uiState,
                                onIntent = onIntent,
                                project = currentProjState,
                                viewModel = viewModel,
                                onNavigateToRd = onNavigateToRd,
                                onNavigateToFacilities = onNavigateToFacilities,
                                onNavigateToBorsa = onNavigateToBorsa,
                                onNavigateToMarket = onNavigateToMarket,
                                isExpanded = true,
                                onToggleExpand = {},
                                onJoinSlot = { slot -> viewModel.handleIntent(com.example.viewmodel.GameIntent.JoinConsortiumSlot(currentProjState.id, slot.slotId, "local_player", playerEntity?.name ?: "Tüccar")) },
                                onDeliverClick = { slot -> selectedDeliverySlotProject = Pair(currentProjState, slot) },
                                onLeaveSlot = { slot -> viewModel.handleIntent(com.example.viewmodel.GameIntent.LeaveConsortiumSlot(currentProjState.id, slot.slotId)) },
                                onLeaveProject = {
                                    selectedGridProject = null
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.LeaveEntireConsortium(currentProjState.id))
                                },
                                onTakeoverBottleneck = { slot -> viewModel.handleIntent(com.example.viewmodel.GameIntent.TakeoverBottleneckSlot(currentProjState.id, slot.slotId, "local_player", playerEntity?.name ?: "Tüccar")) },
                                onKickPartnerFromSlot = { slot -> viewModel.handleIntent(com.example.viewmodel.GameIntent.KickPartnerFromConsortiumSlot(currentProjState.id, slot.slotId)) },
                                onSellWarehouseStock = { viewModel.handleIntent(com.example.viewmodel.GameIntent.SellConsortiumWarehouseStock(currentProjState.id)) },
                                onAdvanceStage = { viewModel.handleIntent(com.example.viewmodel.GameIntent.AdvanceMegaProjectStage(currentProjState.id)) },
                                onClaimDividend = { viewModel.handleIntent(com.example.viewmodel.GameIntent.ClaimMegaProjectDividend(currentProjState.id)) },
                                onProduceBrandItem = { viewModel.handleIntent(com.example.viewmodel.GameIntent.ProduceConsortiumBrandItem(currentProjState.id)) },
                                onStartNewBatch = { viewModel.handleIntent(com.example.viewmodel.GameIntent.ResetConsortiumNewBatch(currentProjState.id)) },
                                onOpenChat = { activeChatProject = currentProjState; viewModel.handleIntent(com.example.viewmodel.GameIntent.ListenToConsortiumChat(currentProjState.id)) },
                                onDisbandConsortium = {
                                    selectedGridProject = null
                                    if (activeChatProject?.id == currentProjState.id) activeChatProject = null
                                    if (selectedDeliverySlotProject?.first?.id == currentProjState.id) selectedDeliverySlotProject = null
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.DisbandConsortium(currentProjState.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ================= CONSORTIUM CHAT DIALOG =================
    activeChatProject?.let { proj ->
        val chatMsgs = consortiumChatMessages[proj.id] ?: emptyList()
        ConsortiumChatDialog(
            project = proj,
            messages = chatMsgs,
            onSendMessage = { text ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.SendConsortiumChatMessage(proj.id, text))
            },
            onDismiss = { activeChatProject = null }
        )
    }

    // ================= CREATE CONSORTIUM DIALOG =================
    if (showCreateDialog) {
        CreateConsortiumDialog(
            viewModel = viewModel,
            tier4Products = tier4Products,
            onDismiss = { showCreateDialog = false },
            onNavigateToRd = onNavigateToRd,
            onCreate = { consortiumName, brandName, targetProductId, qualityTier, claimedProductIds, cityId ->
                viewModel.handleIntent(
                    com.example.viewmodel.GameIntent.CreateNewMegaProject(
                        consortiumName = consortiumName,
                        brandName = brandName,
                        targetProductId = targetProductId,
                        qualityTier = qualityTier,
                        founderClaimedProductIds = claimedProductIds,
                        cityId = cityId
                    )
                )
                showCreateDialog = false
            }
        )
    }

    // ================= DELIVER MATERIAL DIALOG =================
    selectedDeliverySlotProject?.let { (project, slot) ->
        val userStock = inventory.find { it.itemId == slot.productId }?.quantity ?: 0
        DeliverMaterialDialog(
            uiState = uiState,
            onIntent = onIntent,
            project = project,
            slot = slot,
            userStock = userStock,
            onDismiss = { selectedDeliverySlotProject = null },
            onDeliver = { deliverQty ->
                viewModel.handleIntent(com.example.viewmodel.GameIntent.DeliverMaterialsToConsortium(project.id, slot.slotId, deliverQty))
                selectedDeliverySlotProject = null
            }
        )
    }
        }
    }
}

// ==========================================
// COMPONENT: CONSORTIUM SUPPLIER SLOT ITEM CARD
// ==========================================
@Composable
fun ConsortiumSlotItemCard(
    project: MegaProject,
    slot: ConsortiumSupplierSlot,
    uiState: com.example.viewmodel.GameUiState,
    viewModel: GameViewModel,
    isLeader: Boolean,
    onJoinSlot: (ConsortiumSupplierSlot) -> Unit,
    onDeliverClick: (ConsortiumSupplierSlot) -> Unit,
    onLeaveSlot: (ConsortiumSupplierSlot) -> Unit,
    onTakeoverBottleneck: (ConsortiumSupplierSlot) -> Unit,
    onKickPartnerFromSlot: (ConsortiumSupplierSlot) -> Unit,
    onNavigateToMarket: () -> Unit,
    onNavigateToFacilities: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pId = uiState.playerState.player?.id ?: "local_player"
    val pName = uiState.playerState.player?.name ?: ""
    val isMySlot = slot.assignedPartnerId == pId || slot.assignedPartnerId == "local_player" || (pName.isNotBlank() && slot.assignedPartnerName == pName)
    val isUnassigned = slot.assignedPartnerId == null
    val isSlotFinished = slot.isFullyDelivered

    val invItem = uiState.inventoryState.items.find { it.itemId == slot.productId }
    val availableInInventory = invItem?.quantity ?: 0
    val remainingRequired = (slot.quantityRequired - slot.quantityDelivered).coerceAtLeast(0)
    val oneTapAmount = minOf(availableInInventory, remainingRequired)

    val cardBorderColor = when {
        isSlotFinished -> Color(0xFF10B981)
        isMySlot -> ThemeNeonCyan
        isUnassigned -> Color(0xFF3B82F6)
        else -> Color(0xFF334155)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isSlotFinished -> Color(0xFF062D24).copy(alpha = 0.7f)
            isMySlot -> Color(0xFF0C2238).copy(alpha = 0.8f)
            isUnassigned -> Color(0xFF0F1E36).copy(alpha = 0.7f)
            else -> Color(0xFF0F172A).copy(alpha = 0.6f)
        },
        border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row: Product info & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            UniversalProductIcon(
                                productId = slot.productId,
                                displayName = slot.productName,
                                fallbackVector = getSlotProductIcon(slot.productId),
                                size = 24.dp,
                                tint = if (isSlotFinished) Color(0xFF34D399) else ThemeNeonCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        CurrencyText(
                            text = slot.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CurrencyText(
                            text = "Kota: ${slot.quantityDelivered} / ${slot.quantityRequired} Adet",
                            fontSize = 9.5.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Partner / Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isSlotFinished -> Color(0xFF065F46)
                        isMySlot -> ThemeNeonCyan.copy(alpha = 0.2f)
                        isUnassigned -> Color(0xFF1D4ED8).copy(alpha = 0.3f)
                        else -> Color(0xFF334155)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isSlotFinished -> Color(0xFF34D399)
                            isMySlot -> ThemeNeonCyan
                            isUnassigned -> Color(0xFF60A5FA)
                            else -> Color(0xFF64748B)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isSlotFinished -> Icons.Rounded.CheckCircle
                                isMySlot -> Icons.Rounded.VerifiedUser
                                isUnassigned -> Icons.Rounded.AddCircleOutline
                                else -> Icons.Rounded.Person
                            },
                            contentDescription = null,
                            tint = when {
                                isSlotFinished -> Color(0xFF34D399)
                                isMySlot -> ThemeNeonCyan
                                isUnassigned -> Color(0xFF93C5FD)
                                else -> Color.LightGray
                            },
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText(
                            text = when {
                                isSlotFinished -> tr("Tamamlandı", "Completed")
                                isMySlot -> tr("Senin Kotan (%${slot.sharePercentage.toInt()})", "Your Quota (%${slot.sharePercentage.toInt()})")
                                isUnassigned -> tr("Açık Kota (%${slot.sharePercentage.toInt()})", "Open Quota (%${slot.sharePercentage.toInt()})")
                                else -> slot.assignedPartnerName ?: tr("Ortak", "Partner")
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CurrencyText(
                        text = if (isSlotFinished) tr("Montaja Hazır", "Ready for Assembly") else tr("Teslimat İlerlemesi", "Delivery Progress"),
                        fontSize = 8.5.sp,
                        color = Color.Gray
                    )
                    CurrencyText(
                        text = "%${(slot.progressFraction * 100).toInt()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSlotFinished) Color(0xFF34D399) else ThemeNeonCyan
                    )
                }
                LinearProgressIndicator(
                    progress = { slot.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isSlotFinished) Color(0xFF10B981) else ThemeNeonCyan,
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Action Buttons Section
            if (isUnassigned) {
                AppButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onJoinSlot(slot)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                ) {
                    Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(tr("Bu Kotayı Al & Ortak Ol (%${slot.sharePercentage.toInt()} Pay)", "Take Quota & Partner Up (%${slot.sharePercentage.toInt()} Share)"), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isMySlot) {
                if (isSlotFinished) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("✅ Bu kotalık malzemenin tamamını teslim ettin.", "✅ You have fully delivered materials for this quota."),
                            fontSize = 9.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLeaveSlot(slot)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = tr("Ayrıl", "Leave"), tint = Color.Gray, modifier = Modifier.size(13.dp))
                        }
                    }
                } else {
                    // Delivery Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.OneTapDeliverToConsortium(project.id, slot.slotId))
                            },
                            enabled = oneTapAmount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                            modifier = Modifier.weight(1.2f).height(28.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(
                                text = if (oneTapAmount > 0) tr("⚡ Hızlı Ver ($oneTapAmount)", "⚡ Quick Deliver ($oneTapAmount)") else tr("⚡ Depoda Yok", "⚡ Out of Stock"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeliverClick(slot)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(28.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(tr("📦 Miktar Seç", "📦 Select Qty"), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick links row (Market, Facilities, Leave)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyText(
                                text = tr("🛒 Pazardan Al", "🛒 Buy Market"),
                                fontSize = 8.5.sp,
                                color = ThemeNeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigateToMarket() }
                            )
                            CurrencyText("•", fontSize = 8.5.sp, color = Color.DarkGray)
                            CurrencyText(
                                text = tr("🏭 Tesisimde Üret", "🏭 Manufacture"),
                                fontSize = 8.5.sp,
                                color = ThemeGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigateToFacilities() }
                            )
                        }

                        CurrencyText(
                            text = tr("🚪 Ayrıl", "🚪 Leave"),
                            fontSize = 8.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.clickable { onLeaveSlot(slot) }
                        )
                    }
                }
            } else {
                // Another partner's slot
                if (!isSlotFinished) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.handleIntent(com.example.viewmodel.GameIntent.NudgeConsortiumPartner(project.id, slot.slotId))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFFFBBF24)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(26.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(tr("📢 Telsizle Dürt", "📢 Radio Nudge"), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }

                        if (slot.isBottleneckWarning) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTakeoverBottleneck(slot)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                                modifier = Modifier.weight(1.2f).height(26.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                CurrencyText(tr("⚠️ Darboğazı Devral", "⚠️ Take Over"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (isLeader) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onKickPartnerFromSlot(slot)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Rounded.PersonRemove, contentDescription = tr("Slotu Boşalt", "Kick Partner"), tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: MEGA PROJECT CARD
// ==========================================
@Composable
fun MegaProjectCard(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    project: MegaProject,
    viewModel: GameViewModel,
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToFacilities: () -> Unit = {},
    onNavigateToBorsa: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {},
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onJoinSlot: (ConsortiumSupplierSlot) -> Unit,
    onDeliverClick: (ConsortiumSupplierSlot) -> Unit,
    onLeaveSlot: (ConsortiumSupplierSlot) -> Unit,
    onLeaveProject: () -> Unit,
    onTakeoverBottleneck: (ConsortiumSupplierSlot) -> Unit,
    onKickPartnerFromSlot: (ConsortiumSupplierSlot) -> Unit,
    onSellWarehouseStock: () -> Unit,
    onAdvanceStage: () -> Unit,
    onClaimDividend: () -> Unit,
    onProduceBrandItem: () -> Unit,
    onStartNewBatch: () -> Unit,
    onOpenChat: () -> Unit,
    onDisbandConsortium: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()
    var showDisbandDialog by remember { mutableStateOf(false) }
    var showLeaveProjectDialog by remember { mutableStateOf(false) }
    var selectedDetailTab by remember { mutableStateOf(0) }

    var showStampOverlay by remember { mutableStateOf(false) }
    var stampTitle by remember { mutableStateOf("SERİ ÜRETİM ONAYLANDI") }
    var stampSubtitle by remember { mutableStateOf("KONSORSİYUM FABRİKA MÜHÜRÜ") }

    val playerGuildShares = uiState.guildsState.playerGuildShares
    val isFinished = project.isAllStagesFinished || project.currentStage == MegaProjectStage.COMPLETED
    val isStage4 = project.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION
    val pId = uiState.playerState.player?.id ?: "local_player"
    val pName = uiState.playerState.player?.name ?: ""
    val pEmail = viewModel.onlineEmail.value
    val isLeader = project.leaderPlayerId == "local_player" ||
            project.leaderPlayerId == pId ||
            (pName.isNotBlank() && project.leaderPlayerName == pName) ||
            (pEmail.isNotBlank() && project.leaderPlayerId.contains(pEmail.replace(".", "_")))
    val isProjectUnlocked = viewModel.isProductUnlocked(project.targetProductId)
    val cardBorderColor = if (!isProjectUnlocked) Color(0xFFE040FB) else if (isFinished) Color(0xFF10B981) else if (isStage4) Color(0xFFA855F7) else if (isExpanded) ThemeNeonCyan else Color(0xFF233554)
    val openSlotsCount = remember(project.slots) { project.slots.count { it.assignedPartnerId == null } }

    val mySlot = remember(project.slots, pId, pName) {
        project.slots.find { it.assignedPartnerId == pId || it.assignedPartnerId == "local_player" || (pName.isNotBlank() && it.assignedPartnerName == pName) }
    }
    val isMember = mySlot != null || isLeader || project.participatingPartnerIds.contains(pId) || project.participatingPartnerIds.contains("local_player")

    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .luxeShimmerBorder(
                enabled = isFinished || isStage4 || isLeader,
                shape = cardShape,
                borderWidth = 1.5.dp
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleExpand()
            },
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (isFinished) listOf(Color(0xFF064E3B).copy(alpha = 0.6f), Color(0xFF022C22).copy(alpha = 0.4f))
                        else if (isStage4) listOf(Color(0xFF4C1D95).copy(alpha = 0.6f), Color(0xFF2E1065).copy(alpha = 0.4f))
                        else listOf(Color(0xFF1E293B).copy(alpha = 0.8f), Color(0xFF0F172A).copy(alpha = 0.5f))
                    )
                )
                .padding(16.dp)
        ) {
            // --- DYNAMIC MEGA PROJECT BANNER ---
            val bannerResId = remember(project.targetProductId) {
                ProductDrawables.getProjectBannerDrawableResId(project.targetProductId)
            }
            if (bannerResId != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = bannerResId),
                    contentDescription = project.targetProductName,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ProjectDynamicIcon(project = project, size = 46.dp, iconSize = 25.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(
                                text = project.consortiumName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (openSlotsCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF34D399).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                                ) {
                                    CurrencyText(
                                        text = tr("🤝 $openSlotsCount AÇIK SLOT", "🤝 $openSlotsCount OPEN SLOTS"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7F3D0),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText(
                                text = "🏷️ Marka: ${project.brandName} (${project.targetProductName})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeGold,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val tierBadgeText = when (project.qualityTier) {
                                ConsortiumQualityTier.GRADE_A -> tr("🏆 A Kalite", "🏆 A Grade")
                                ConsortiumQualityTier.GRADE_B -> tr("⭐ B Kalite", "⭐ B Grade")
                                ConsortiumQualityTier.GRADE_C -> tr("📦 C Kalite", "📦 C Grade")
                            }
                            val tierBadgeColor = when (project.qualityTier) {
                                ConsortiumQualityTier.GRADE_A -> Color(0xFFF59E0B)
                                ConsortiumQualityTier.GRADE_B -> ThemeNeonCyan
                                ConsortiumQualityTier.GRADE_C -> Color(0xFF94A3B8)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = tierBadgeColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, tierBadgeColor.copy(alpha = 0.6f))
                            ) {
                                CurrencyText(
                                    text = tierBadgeText,
                                    color = tierBadgeColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Stage Badge Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isFinished -> Color(0xFF064E3B)
                        isStage4 -> Color(0xFF3B125B)
                        else -> Color(0xFF1E2D4A)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isFinished -> Color(0xFF10B981)
                            isStage4 -> Color(0xFFA855F7)
                            else -> ThemeNeonCyan
                        }
                    )
                ) {
                    CurrencyText(
                        text = when (project.currentStage) {
                            MegaProjectStage.STAGE_1_BODY -> tr("Aşama I: Altyapı & Gövde", "Phase I: Infrastructure & Body")
                            MegaProjectStage.STAGE_2_HARDWARE -> tr("Aşama II: Donanım & Sistem", "Phase II: Hardware & System")
                            MegaProjectStage.STAGE_3_TESTING -> tr("Aşama III: Test & Lansman", "Phase III: Test & Launch")
                            MegaProjectStage.STAGE_4_MASS_PRODUCTION -> tr("🏭 IV: Seri Üretim", "🏭 IV: Mass Production")
                            MegaProjectStage.COMPLETED -> tr("✅ TAMAMLANDI", "✅ COMPLETED")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isFinished -> Color(0xFF34D399)
                            isStage4 -> Color(0xFFE9D5FF)
                            else -> ThemeNeonCyan
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stage Progress Milestones Visualizer (4 Stages)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                StageStepBar(
                    title = "I. Gövde",
                    isCurrent = project.currentStage == MegaProjectStage.STAGE_1_BODY,
                    isDone = project.currentStage.ordinal > 0 || isFinished,
                    modifier = Modifier.weight(1f)
                )
                StageStepBar(
                    title = "II. Donanım",
                    isCurrent = project.currentStage == MegaProjectStage.STAGE_2_HARDWARE,
                    isDone = project.currentStage.ordinal > 1 || isFinished,
                    modifier = Modifier.weight(1f)
                )
                StageStepBar(
                    title = "III. Test",
                    isCurrent = project.currentStage == MegaProjectStage.STAGE_3_TESTING,
                    isDone = project.currentStage.ordinal > 2 || isFinished,
                    modifier = Modifier.weight(1f)
                )
                StageStepBar(
                    title = "IV. Seri Üretim",
                    isCurrent = project.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION,
                    isDone = isFinished,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress & Value Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CurrencyText(
                        text = "👑 ${project.leaderPlayerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )
                    CurrencyText(text = "•", color = Color.DarkGray, fontSize = 9.sp)
                    CurrencyText(
                        text = "💰 ${formatCredit(project.totalProjectValue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    CurrencyText(text = "•", color = Color.DarkGray, fontSize = 9.sp)
                    CurrencyText(
                        text = "%${(project.overallProgressFraction * 100).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFinished) Color(0xFF10B981) else ThemeNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasUnread = uiState.consortiumState.unreadChatProjects.contains(project.id)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isMember) Color(0xFF0F2942) else Color(0xFF1B232D),
                        border = BorderStroke(1.dp, if (hasUnread) Color.Red else if (isMember) ThemeNeonCyan else Color.Gray.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenChat()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forum,
                                contentDescription = null,
                                tint = if (hasUnread) Color.Red else if (isMember) ThemeNeonCyan else Color.Gray,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = "💬 SOHBET",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasUnread) Color.Red else if (isMember) ThemeNeonCyan else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2D4A).copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            CurrencyText(
                                text = if (isExpanded) "GİZLE " else "📋 DETAY ",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = ThemeNeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AR-GE Locked Alert
                    if (!isProjectUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2D1236),
                            border = BorderStroke(1.dp, Color(0xFFE040FB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    CurrencyText(
                                        text = "🔬 " + tr("AR-GE ARAŞTIRMASI GEREKİYOR", "R&D RESEARCH REQUIRED"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = Color(0xFFE040FB)
                                    )
                                    CurrencyText(
                                        text = tr("Bu projede işlem yapmak için '${project.targetProductName}' araştırması tamamlanmalıdır.", "Research '${project.targetProductName}' to participate."),
                                        fontSize = 8.5.sp,
                                        color = Color.LightGray
                                    )
                                }
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val techNode = com.example.data.TechTree.nodes.find { it.unlockedProductIds.contains(project.targetProductId) }
                                        onNavigateToRd(techNode?.id?.removePrefix("tech_"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    CurrencyText(tr("AR-GE'YE GİT", "GO TO R&D"), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // ================= 🎯 ACTION HUD: SMART CONTEXT BAR =================
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F1E36),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Explore, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CurrencyText(tr("🎯 Ne Yapmalısın? (Hızlı Eylem)", "🎯 What to do next? (Quick Action)"), fontSize = 10.sp, fontWeight = FontWeight.Black, color = ThemeGold)
                                }

                                val hasUnread = uiState.consortiumState.unreadChatProjects.contains(project.id)
                                Row(
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenChat()
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = if (hasUnread) Color.Red else ThemeNeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    CurrencyText(tr("💬 Telsiz", "💬 Radio"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (hasUnread) Color.Red else ThemeNeonCyan)
                                }
                            }

                            if (mySlot != null) {
                                if (!mySlot.isFullyDelivered) {
                                    val invItem = uiState.inventoryState.items.find { it.itemId == mySlot.productId }
                                    val availableInInventory = invItem?.quantity ?: 0
                                    val remaining = (mySlot.quantityRequired - mySlot.quantityDelivered).coerceAtLeast(0)
                                    val fastDeliverQty = minOf(availableInInventory, remaining)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            CurrencyText(
                                                text = "📦 ${mySlot.productName}: ${mySlot.quantityDelivered}/${mySlot.quantityRequired} Adet",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            CurrencyText(
                                                text = if (fastDeliverQty > 0) tr("Depoda $availableInInventory adet hazır malzeme var!", "$availableInInventory items ready in storage!") else tr("Depoda malzeme yok. Pazardan al veya üret.", "Out of stock. Buy or produce."),
                                                fontSize = 8.5.sp,
                                                color = if (fastDeliverQty > 0) Color(0xFF34D399) else Color(0xFFFBBF24)
                                            )
                                        }

                                        if (fastDeliverQty > 0) {
                                            AppButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.OneTapDeliverToConsortium(project.id, mySlot.slotId))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                CurrencyText("⚡ $fastDeliverQty Adet Ver", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        } else {
                                            AppButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onNavigateToMarket()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color.Black),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                CurrencyText("🛒 Pazardan Al", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                } else {
                                    CurrencyText(
                                        text = tr("✅ Kotanı eksiksiz teslim ettin! Diğer ortakların parçaları tamamlaması bekleniyor.", "✅ You fully delivered your quota! Waiting for other partners to complete assembly."),
                                        fontSize = 9.sp,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (openSlotsCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(
                                        text = tr("🤝 Projede $openSlotsCount açık kota var. Katılıp kâr payı kazan!", "🤝 $openSlotsCount open quota available. Join & earn dividends!"),
                                        fontSize = 9.sp,
                                        color = Color(0xFF93C5FD),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AppButton(
                                        onClick = { selectedDetailTab = 0 },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                                        modifier = Modifier.height(26.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        CurrencyText(tr("Kotaları Gör", "View Quotas"), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                CurrencyText(
                                    text = tr("⚙️ Tüm kotalar dolu. Montaj ve seri üretim süreçleri devam ediyor.", "⚙️ All quotas filled. Assembly and mass production active."),
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    // ================= 3 CLEAN TABS =================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDetailTab == 0) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selectedDetailTab == 0) ThemeNeonCyan else Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(34.dp).clickable { selectedDetailTab = 0 }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, tint = if (selectedDetailTab == 0) ThemeNeonCyan else Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(tr("🏭 1. Kotalar", "🏭 1. Quotas"), fontSize = 9.5.sp, fontWeight = if (selectedDetailTab == 0) FontWeight.Black else FontWeight.Normal, color = if (selectedDetailTab == 0) Color.White else Color.Gray)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDetailTab == 1) ThemeGold.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selectedDetailTab == 1) ThemeGold else Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(34.dp).clickable { selectedDetailTab = 1 }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = if (selectedDetailTab == 1) ThemeGold else Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(tr("💰 2. Finans", "💰 2. Finance"), fontSize = 9.5.sp, fontWeight = if (selectedDetailTab == 1) FontWeight.Black else FontWeight.Normal, color = if (selectedDetailTab == 1) Color.White else Color.Gray)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDetailTab == 2) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selectedDetailTab == 2) Color(0xFF8B5CF6) else Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(34.dp).clickable { selectedDetailTab = 2 }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = if (selectedDetailTab == 2) Color(0xFFC084FC) else Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(tr("🏛️ 3. Yönetim", "🏛️ 3. Board"), fontSize = 9.5.sp, fontWeight = if (selectedDetailTab == 2) FontWeight.Black else FontWeight.Normal, color = if (selectedDetailTab == 2) Color.White else Color.Gray)
                            }
                        }
                    }

                    // ================= TAB CONTENT 0: ÜRETİM & KOTALAR =================
                    if (selectedDetailTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Isometric Line Preview
                            ConsortiumAssemblyLineCanvas(
                                project = project,
                                modifier = Modifier.fillMaxWidth().height(130.dp)
                            )

                            // 1 Günlük İlk Malzeme Geri Sayımı (varsa)
                            if (project.isPreparationCountdownActive) {
                                val remainingMs = project.remainingPreparationCountdownMs
                                val hours = remainingMs / (1000 * 3600)
                                val mins = (remainingMs / (1000 * 60)) % 60
                                val secs = (remainingMs / 1000) % 60
                                val countdownStr = String.format("%02d:%02d:%02d", hours, mins, secs)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            CurrencyText(tr("İlk Teslimat & Hazırlık Süreci", "Initial Delivery & Preparation"), color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                        CurrencyText(countdownStr, color = ThemeNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            // Seri Üretim Bandı Durumu
                            ConsortiumProductionLineCard(project = project)

                            // Depo Durumu
                            val totalStockDelivered = project.slots.sumOf { it.quantityDelivered }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Warehouse, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CurrencyText(
                                            text = "Depo: $totalStockDelivered / ${project.warehouseCapacity} Birim",
                                            color = Color.LightGray,
                                            fontSize = 9.5.sp
                                        )
                                    }

                                    if (isLeader) {
                                        AppButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.upgradeConsortiumWarehouseWithGems(project.id, 100)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Icon(Icons.Rounded.Diamond, contentDescription = null, tint = Color(0xFF67E8F9), modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            CurrencyText("100 💎 (+1000)", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            // 1 PARTİ TEST ÜRETİMİ & KURUCU ONAYI
                            if (project.isTestProductProduced && !project.isMassProductionApproved) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF064E3B),
                                    border = BorderStroke(1.2.dp, Color(0xFF34D399)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            CurrencyText(tr("🧪 1. PARTİ TEST ÜRETİMİ BAŞARILI!", "🧪 1ST BATCH TEST PRODUCTION SUCCESS!"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                        if (isLeader) {
                                            AppButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.approveConsortiumMassProduction(project.id)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                                modifier = Modifier.fillMaxWidth().height(32.dp)
                                            ) {
                                                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                CurrencyText(tr("👑 SERİ ÜRETİMİ ONAYLA & BAŞLAT", "👑 APPROVE & START MASS PRODUCTION"), fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                            }
                                        } else {
                                            CurrencyText(
                                                text = tr("⏳ Kurucu (${project.leaderPlayerName}) onayı bekleniyor...", "⏳ Waiting for Founder (${project.leaderPlayerName}) approval..."),
                                                color = Color(0xFFFDE68A),
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Tedarikçi Kotaları Başlığı
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = tr("📋 Tedarikçi Kotaları", "📋 Supplier Quotas"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                CurrencyText(
                                    text = "${project.slots.count { it.isFullyDelivered }}/${project.slots.size} " + tr("Tamamlandı", "Completed"),
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                            }

                            // Slots Cards
                            project.slots.forEach { slot ->
                                ConsortiumSlotItemCard(
                                    project = project,
                                    slot = slot,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    isLeader = isLeader,
                                    onJoinSlot = onJoinSlot,
                                    onDeliverClick = onDeliverClick,
                                    onLeaveSlot = onLeaveSlot,
                                    onTakeoverBottleneck = onTakeoverBottleneck,
                                    onKickPartnerFromSlot = onKickPartnerFromSlot,
                                    onNavigateToMarket = onNavigateToMarket,
                                    onNavigateToFacilities = onNavigateToFacilities
                                )
                            }

                            // Advance Stage Button
                            if (!isFinished && project.isCurrentStageFinished) {
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAdvanceStage()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Rounded.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CurrencyText(tr("AŞAMAYI TAMAMLA & SONRAKİNE GEÇ", "COMPLETE STAGE & ADVANCE"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // ================= TAB CONTENT 1: FİNANS & TEMETTÜ =================
                    if (selectedDetailTab == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val shareValue = project.unitBatchPrice.toDouble() / 1000.0
                            val currentSharePriceDisplay = shareValue * (1.0 + ((playerGuildShares[project.id] ?: 0).toDouble() * 0.002))

                            // Financial Indicators Summary
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CurrencyText(tr("PARTİ FİNANSAL GÖSTERGELERİ", "BATCH FINANCIAL INDICATORS"), color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp)

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(tr("Parti Başına Maliyet:", "Cost Per Batch:"), color = Color.LightGray, fontSize = 9.5.sp)
                                        CurrencyText(formatCurrency(project.unitBatchCost, isEng), color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(tr("Parti Satış Fiyatı:", "Batch Selling Price:"), color = Color.LightGray, fontSize = 9.5.sp)
                                        CurrencyText(formatCurrency(project.unitBatchPrice, isEng), color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(tr("Parti Üretim Kârı:", "Batch Net Profit:"), color = Color.LightGray, fontSize = 9.5.sp)
                                        CurrencyText(formatCurrency(project.unitBatchPrice - project.unitBatchCost, isEng), color = ThemeGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(tr("👑 Kurucu Yönetim Payı (%10):", "👑 Founder Management Share (%10):"), color = ThemeGold, fontSize = 9.sp)
                                        CurrencyText(formatCurrency((project.unitBatchPrice * 0.10f).toLong(), isEng), color = ThemeGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(tr("📦 Tedarikçi Havuzu (%90):", "📦 Supplier Pool (%90):"), color = ThemeNeonCyan, fontSize = 9.sp)
                                        CurrencyText(formatCurrency((project.unitBatchPrice * 0.90f).toLong(), isEng), color = ThemeNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(tr("Güncel Hisse Değeri (1/1000):", "Current Share Price (1/1000):"), color = Color.LightGray, fontSize = 9.sp)
                                        CurrencyText(formatCurrency(currentSharePriceDisplay.toLong(), isEng), color = ThemeNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Partners Balance Sheet
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF131D31),
                                border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CurrencyText(tr("👥 Ortaklar Bilanço & K/Z Tablosu", "👥 Partners Balance & P/L Sheet"), color = ThemeGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)

                                    val groupedPartners = project.slots.filter { it.assignedPartnerId != null }.groupBy { it.assignedPartnerId }

                                    if (groupedPartners.isEmpty()) {
                                        CurrencyText(tr("Henüz katılan ortak yok.", "No joined partners yet."), color = Color.Gray, fontSize = 9.sp)
                                    } else {
                                        groupedPartners.forEach { (_, slotsForPartner) ->
                                            val partnerName = slotsForPartner.first().assignedPartnerName ?: tr("Bilinmeyen", "Unknown")
                                            val totalDividends = slotsForPartner.sumOf { it.totalDividendsEarned }
                                            val mySharePercent = slotsForPartner.sumOf { it.sharePercentage.toDouble() }.toFloat()
                                            val myShareFraction = if (mySharePercent > 0f) mySharePercent / 100f else 0f
                                            val unrealizedBatchRevenue = (project.warehouseStock * project.unitBatchPrice)
                                            val unrealizedDividends = (unrealizedBatchRevenue * myShareFraction).toLong()

                                            val activeBeltRevenue = slotsForPartner.sumOf { slot ->
                                                val slotShareFraction = if (slot.sharePercentage > 0f) slot.sharePercentage / 100f else 0f
                                                val expectedPayout = (project.unitBatchPrice * slotShareFraction).toLong()
                                                val deliveryRatio = if (slot.quantityRequired > 0) slot.quantityDelivered.toFloat() / slot.quantityRequired.toFloat() else 0f
                                                (expectedPayout * deliveryRatio).toLong()
                                            }
                                            val totalValue = totalDividends + unrealizedDividends + activeBeltRevenue

                                            val idealCost = slotsForPartner.sumOf { slot ->
                                                val realizedBatches = if (project.unitBatchPrice > 0 && slot.sharePercentage > 0f) {
                                                    slot.totalDividendsEarned.toDouble() / (project.unitBatchPrice * (slot.sharePercentage / 100f))
                                                } else 0.0
                                                val warehouseBatches = project.warehouseStock.toDouble()
                                                val activeBatches = if (slot.quantityRequired > 0) slot.quantityDelivered.toDouble() / slot.quantityRequired else 0.0
                                                (slot.costContributionValue * (realizedBatches + warehouseBatches + activeBatches)).toLong()
                                            }
                                            val netProfit = totalValue - idealCost

                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                CurrencyText("👤 $partnerName", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    CurrencyText(tr("Girdi Maliyeti:", "Input Cost:"), color = Color.LightGray, fontSize = 8.5.sp)
                                                    CurrencyText(formatCurrency(idealCost, isEng), color = Color(0xFFEF4444), fontSize = 8.5.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    CurrencyText(tr("Toplam Hakediş:", "Total Payout:"), color = Color.LightGray, fontSize = 8.5.sp)
                                                    CurrencyText(formatCurrency(totalValue, isEng), color = Color(0xFF34D399), fontSize = 8.5.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    CurrencyText(tr("Net Bilanço:", "Net Balance:"), color = Color.LightGray, fontSize = 8.5.sp)
                                                    CurrencyText(
                                                        text = (if (netProfit >= 0) "+" else "-") + formatCurrency(kotlin.math.abs(netProfit), isEng),
                                                        color = if (netProfit >= 0) Color(0xFF34D399) else Color(0xFFEF4444),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Warehouse Stock Sell & Dividend Actions
                            if (project.warehouseStock > 0) {
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSellWarehouseStock()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CurrencyText(tr("💰 DEPOYU SAT (${project.warehouseStock} Adet)", "💰 SELL WAREHOUSE STOCK (${project.warehouseStock} Units)"), fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                                }
                            }

                            if (isFinished && !project.isDividendClaimed) {
                                AppButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onClaimDividend()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().height(34.dp)
                                ) {
                                    Icon(Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CurrencyText(tr("KÂR PAYLARINI (TEMETTÜ) HESAPLARA DAĞIT", "DISTRIBUTE DIVIDENDS TO ACCOUNTS"), fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // ================= TAB CONTENT 2: YÖNETİM & STRATEJİ REHBERİ =================
                    if (selectedDetailTab == 2) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. Proje Durum ve Üretim Yol Haritası (Executive Stage Tracker)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D172A),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp))
                                            CurrencyText(
                                                text = tr("KONSORSİYUM ÜRETİM SÜREÇ REHBERİ", "CONSORTIUM PRODUCTION ROADMAP"),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (project.currentStage) {
                                                MegaProjectStage.STAGE_1_BODY -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                MegaProjectStage.STAGE_2_HARDWARE -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                MegaProjectStage.STAGE_3_TESTING -> Color(0xFFA855F7).copy(alpha = 0.2f)
                                                MegaProjectStage.STAGE_4_MASS_PRODUCTION -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                MegaProjectStage.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            },
                                            border = BorderStroke(1.dp, when (project.currentStage) {
                                                MegaProjectStage.STAGE_1_BODY -> Color(0xFF60A5FA)
                                                MegaProjectStage.STAGE_2_HARDWARE -> Color(0xFFFBBF24)
                                                MegaProjectStage.STAGE_3_TESTING -> Color(0xFFC084FC)
                                                MegaProjectStage.STAGE_4_MASS_PRODUCTION -> Color(0xFF34D399)
                                                MegaProjectStage.COMPLETED -> Color(0xFF34D399)
                                            })
                                        ) {
                                            CurrencyText(
                                                text = if (isEng) project.currentStage.titleEn else project.currentStage.titleTr,
                                                color = Color.White,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Stage explanation text
                                    val guidanceText = if (isEng) project.currentStage.descriptionEn else project.currentStage.descriptionTr

                                    CurrencyText(
                                        text = "ℹ️ $guidanceText",
                                        fontSize = 8.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Kurucu Yönetim Merkezi (Leader controls)
                            if (isLeader) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CurrencyText(tr("👑 KURUCU YÖNETİM KONTROLLERİ", "👑 FOUNDER MANAGEMENT CONTROLS"), color = ThemeGold, fontSize = 9.sp, fontWeight = FontWeight.Black)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText(
                                                text = if (project.isProductionPaused) tr("Üretim Durduruldu", "Production Paused") else tr("Üretim Devam Ediyor", "Production Running"),
                                                fontSize = 9.sp,
                                                color = if (project.isProductionPaused) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)
                                            )
                                            AppButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.ToggleConsortiumProductionState(project.id))
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (project.isProductionPaused) Color(0xFF10B981) else Color(0xFFEF4444),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.height(24.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                CurrencyText(if (project.isProductionPaused) tr("BAŞLAT", "START") else tr("DURDUR", "PAUSE"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText(
                                                text = tr("🤖 İK Otomatik Satış: ", "🤖 HR Auto Sell: ") + if (project.isAutoSellActive) tr("AÇIK", "ON") else tr("KAPALI", "OFF"),
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                            AppButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.ToggleConsortiumAutoSell(project.id, !project.isAutoSellActive))
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (project.isAutoSellActive) ThemeNeonCyan else Color(0xFF334155),
                                                    contentColor = if (project.isAutoSellActive) Color.Black else Color.White
                                                ),
                                                modifier = Modifier.height(24.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                CurrencyText(if (project.isAutoSellActive) tr("KAPAT", "DISABLE") else tr("AÇ", "ENABLE"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }

                            // Board Voting Card
                            ConsortiumBoardVotingCard(
                                project = project,
                                currentUserId = pId,
                                onSetStrategy = { strategy -> viewModel.handleIntent(com.example.viewmodel.GameIntent.SetConsortiumProductionStrategy(project.id, strategy)) },
                                onChangeSalesChannel = { channel -> viewModel.handleIntent(com.example.viewmodel.GameIntent.ChangeConsortiumSalesChannel(project.id, channel)) },
                                onVoteProposal = { propId, yes -> viewModel.handleIntent(com.example.viewmodel.GameIntent.VoteOnConsortiumBoardProposal(project.id, propId, yes)) },
                                onCreateProposal = { titleTr, titleEn, descTr, descEn, type, proposedVal ->
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.CreateConsortiumBoardProposal(project.id, titleTr, titleEn, descTr, descEn, type, proposedVal))
                                }
                            )

                            // Telsiz SOS Broadcast Bar
                            ConsortiumRadioSosBroadcastBar(
                                project = project,
                                onBroadcastSos = { targetSlotId ->
                                    viewModel.handleIntent(com.example.viewmodel.GameIntent.BroadcastConsortiumRadioSos(project.id, targetSlotId))
                                }
                            )

                            // Honor Podium
                            ConsortiumHonorPodium(project = project, currentUserId = pId)

                            // Leave / Disband Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isLeader) {
                                    AppButton(
                                        onClick = { showDisbandDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF450A0A), contentColor = Color(0xFFFCA5A5)),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f).height(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText(tr("Konsorsiyumu Feshet", "Disband Consortium"), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (isMember) {
                                    AppButton(
                                        onClick = { showLeaveProjectDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1065), contentColor = Color(0xFFE9D5FF)),
                                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f).height(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        CurrencyText(tr("Konsorsiyumdan Ayrıl", "Leave Consortium"), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } // Close AnimatedVisibility Column
            } // Close AnimatedVisibility
        } // Close Column inside Card
    } // Close Card

    if (showDisbandDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisbandDialog = false },
            title = { CurrencyText("Konsorsiyumu Feshet?", color = Color.White) },
            text = {
                CurrencyText(
                    "Bu konsorsiyumu feshetmek istediğinize emin misiniz?\n\n" +
                    "• Depodaki hazır ürünler satılıp geliri hisse oranında dağıtılır.\n" +
                    "• Kotalardaki ürünler (sadece size ait olan kısımlar) envanterinize iade edilir.\n" +
                    "• Bu işlem geri alınamaz ve tüm konsorsiyum verileri bulut sunucudan silinir.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDisbandDialog = false
                        onDisbandConsortium()
                    }
                ) {
                    CurrencyText("Evet, Feshet", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDisbandDialog = false }
                ) {
                    CurrencyText("İptal", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    if (showLeaveProjectDialog) {
        val mySlots = project.slots.filter {
            it.assignedPartnerId == pId || it.assignedPartnerId == "local_player" || (pName.isNotBlank() && it.assignedPartnerName == pName)
        }
        val totalDeliveredTons = mySlots.sumOf { it.quantityDelivered }
        val totalRefundTons = (totalDeliveredTons * 80) / 100
        val totalPenaltyTons = totalDeliveredTons - totalRefundTons

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLeaveProjectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText("Tüm Kotalardan Ayrıl?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyText(
                        "Bu konsorsiyumda üstlendiğiniz tüm tedarik kotalarından ayrılmak istediğinize emin misiniz?",
                        color = Color.LightGray,
                        fontSize = 13.5.sp
                    )
                    if (totalDeliveredTons > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CurrencyText("📦 Toplam Sevk Edilen: $totalDeliveredTons Ton", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                CurrencyText("🔄 Deponuza İade (%80): $totalRefundTons Ton", color = Color(0xFF34D399), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                CurrencyText("⚠️ Konsorsiyum Ceza Payı (%20): $totalPenaltyTons Ton", color = Color(0xFFF87171), fontSize = 12.5.sp)
                            }
                        }
                    } else {
                        CurrencyText(
                            "Kotalarınıza henüz ürün sevk etmediniz. Ayrılma durumunda herhangi bir ceza veya iade oluşmaz.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showLeaveProjectDialog = false
                        onLeaveProject()
                    }
                ) {
                    CurrencyText(if (totalDeliveredTons > 0) "Ayrıl ve %80 İade Al" else "Ayrıl", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showLeaveProjectDialog = false }
                ) {
                    CurrencyText("Vazgeç", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    CorporateStampOverlay(
        isVisible = showStampOverlay,
        title = stampTitle,
        subtitle = stampSubtitle,
        onDismiss = { showStampOverlay = false }
    )
}

// ==========================================
// COMPONENT: CONSORTIUM SLOT ROW
// ==========================================
@Composable
fun ConsortiumSlotRow(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    slot: ConsortiumSupplierSlot,
    viewModel: GameViewModel,
    projectId: String = "",
    onNavigateToRd: (String?) -> Unit = {},
    onNavigateToFacilities: () -> Unit = {},
    onNavigateToBorsa: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {},
    currentProjectStage: MegaProjectStage,
    isProjectFinished: Boolean,
    isLeader: Boolean = false,
    onJoin: () -> Unit,
    onDeliver: () -> Unit,
    onLeave: () -> Unit,
    onTakeover: () -> Unit,
    onKickPartner: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isSlotInCurrentStage = slot.stage == currentProjectStage || isProjectFinished
    val myPlayer = uiState.playerState.player
    val myId = myPlayer?.id ?: "local_player"
    val myName = myPlayer?.name ?: ""
    val isUserSlot = slot.assignedPartnerId == "local_player" || slot.assignedPartnerId == myId || (myName.isNotBlank() && slot.assignedPartnerName == myName)
    val isSlotProductUnlocked = viewModel.isProductUnlocked(slot.productId)
    var showDetailModal by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF162238),
        border = BorderStroke(
            1.dp,
            if (isUserSlot) ThemeNeonCyan else if (slot.isBottleneckWarning) Color(0xFFEF4444) else Color(0xFF233554)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable { showDetailModal = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SlotDynamicIcon(
                    productId = slot.productId,
                    isUserSlot = isUserSlot,
                    isFullyDelivered = slot.isFullyDelivered,
                    size = 36.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CurrencyText(
                            text = slot.productName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = ThemeGold.copy(alpha = 0.15f)
                        ) {
                            CurrencyText(
                                text = "%${slot.sharePercentage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        if (!isSlotProductUnlocked) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF451A03),
                                border = BorderStroke(0.8.dp, Color(0xFFF59E0B))
                            ) {
                                CurrencyText(
                                    text = "🔒 Ar-Ge",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFEF3C7),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isUserSlot && !slot.isFullyDelivered) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.15f)
                            ) {
                                CurrencyText(
                                    text = "🤖 İK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeNeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val userStockInRow = uiState.inventoryState.items.find { it.itemId == slot.productId }?.quantity ?: 0
                    val stockColor = when {
                        userStockInRow >= slot.remainingQuantity && slot.remainingQuantity > 0 -> Color(0xFF34D399)
                        userStockInRow > 0 -> Color(0xFFFBBF24)
                        else -> Color(0xFF94A3B8)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CurrencyText(
                            text = "${slot.quantityDelivered}/${slot.quantityRequired} Ton • ${slot.assignedPartnerName ?: "Açık Slot"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = stockColor.copy(alpha = 0.15f)
                        ) {
                            CurrencyText(
                                text = "Depo: $userStockInRow T",
                                color = stockColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val animatedSlotProgress by animateFloatAsState(
                        targetValue = slot.progressFraction,
                        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                        label = "slotProgress"
                    )

                    LinearProgressIndicator(
                        progress = { animatedSlotProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = if (slot.isFullyDelivered) Color(0xFF10B981) else ThemeNeonCyan,
                        trackColor = Color(0xFF0F172A)
                    )
                }
            }

            // Action button container per slot
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .wrapContentWidth(Alignment.End),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (slot.assignedPartnerId == null) {
                    if (isSlotProductUnlocked) {
                        // Empty slot available to join
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onJoin()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            CurrencyText("🤝 KATIL", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        // Slot product requires R&D research first
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val techNode = com.example.data.TechTree.nodes.find { it.unlockedProductIds.contains(slot.productId) }
                                onNavigateToRd(techNode?.id?.removePrefix("tech_"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFFE040FB)),
                            border = BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText("🔬 " + tr("AR-GE", "R&D"), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else if (isUserSlot) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!slot.isFullyDelivered) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDeliver()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                CurrencyText("TESLİM ET", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF064E3B)
                            ) {
                                CurrencyText(
                                    text = "✅ DOLU",
                                    color = Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Leave slot button
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showLeaveConfirmDialog = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            CurrencyText("🚪 Ayrıl", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (slot.isFullyDelivered) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF064E3B)
                            ) {
                                CurrencyText(
                                    text = "✅ DOLU",
                                    color = Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                )
                            }
                        } else if (slot.isBottleneckWarning) {
                            // Stalled slot -> Bottleneck SLA Takeover
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTakeover()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                CurrencyText("⚠️ SLA DEVİR", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E2D4A)
                            ) {
                                CurrencyText(
                                    text = slot.assignedPartnerName ?: "Dolu",
                                    color = Color.LightGray,
                                    fontSize = 8.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isLeader) {
                            val kickColor = if (slot.isBottleneckWarning) Color(0xFFEF4444) else Color(0xFF1E2D4A)
                            val kickTextColor = if (slot.isBottleneckWarning) Color.White else Color(0xFFF87171)
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onKickPartner()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = kickColor, contentColor = kickTextColor),
                                border = if (!slot.isBottleneckWarning) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                CurrencyText(if (slot.isBottleneckWarning) "⚠️ Çıkar" else "🚫 Çıkar", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailModal) {
        val inventoryItem = uiState.inventoryState.items.find { it.itemId == slot.productId }
        val currentStock = inventoryItem?.quantity ?: 0

        AlertDialog(
            onDismissRequest = { showDetailModal = false },
            containerColor = Color(0xFF162238),
            title = {
                CurrencyText(
                    "Tedarik: ${slot.productName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    CurrencyText(
                        "Gereken Miktar: ${slot.quantityRequired - slot.quantityDelivered} Ton\nEnvanterinizdeki Stok: $currentStock Ton",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (projectId.isNotBlank()) {
                        OneTapWarehouseSyncBadge(
                            userStock = currentStock,
                            remainingNeeded = slot.remainingQuantity,
                            onOneTapSync = {
                                showDetailModal = false
                                onIntent(com.example.viewmodel.GameIntent.OneTapDeliverToConsortium(projectId, slot.slotId))
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                showDetailModal = false
                                onIntent(com.example.viewmodel.GameIntent.BroadcastConsortiumRadioSos(projectId, slot.slotId))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(36.dp).padding(bottom = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.Podcasts, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText("📻 Telsiz İlanı Yayınla (Acil Çağrı)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AppButton(
                        onClick = {
                            showDetailModal = false
                            onNavigateToFacilities()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan)
                    ) {
                        CurrencyText("🏭 Tesis Kur", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = {
                            showDetailModal = false
                            onNavigateToBorsa()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold)
                    ) {
                        CurrencyText("📊 Borsadan Satın Al", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = {
                            showDetailModal = false
                            onNavigateToMarket()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        CurrencyText("🛒 Pazardan Bul", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailModal = false }) {
                    CurrencyText("Kapat", color = Color.White)
                }
            }
        )
    }

    if (showLeaveConfirmDialog) {
        val delivered = slot.quantityDelivered
        val returnQty = if (delivered > 0) (delivered * 80) / 100 else 0
        val penaltyQty = delivered - returnQty

        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            containerColor = Color(0xFF162238),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(8.dp))
                    CurrencyText("Slottan Ayrıl ve İade Al?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyText(
                        "${slot.productName} tedarik slotundan ayrılmak istediğinize emin misiniz?",
                        color = Color.LightGray,
                        fontSize = 13.5.sp
                    )
                    if (delivered > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CurrencyText("📦 Sevk Edilen Toplam: $delivered Ton", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                CurrencyText("🔄 Deponuza İade Edilecek (%80): $returnQty Ton", color = Color(0xFF34D399), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                CurrencyText("⚠️ Konsorsiyum Ceza Payı (%20): $penaltyQty Ton", color = Color(0xFFF87171), fontSize = 12.5.sp)
                            }
                        }
                    } else {
                        CurrencyText(
                            "Bu slota henüz ürün sevk etmediniz. Ayrılma durumunda herhangi bir ceza veya iade oluşmaz.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                AppButton(
                    onClick = {
                        showLeaveConfirmDialog = false
                        onLeave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    CurrencyText(if (delivered > 0) "Ayrıl ve %80 İade Al" else "Slottan Ayrıl", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    CurrencyText("Vazgeç", color = Color.White)
                }
            }
        )
    }
}

// ==========================================
// COMPONENT: STAGE STEP BAR
// ==========================================
@Composable
fun StageStepBar(
    title: String,
    isCurrent: Boolean,
    isDone: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isDone -> Color(0xFF10B981).copy(alpha = 0.15f)
        isCurrent -> ThemeNeonCyan.copy(alpha = 0.15f)
        else -> Color(0xFF0F172A).copy(alpha = 0.4f)
    }
    val borderColor = when {
        isDone -> Color(0xFF10B981).copy(alpha = 0.4f)
        isCurrent -> ThemeNeonCyan.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Surface(
        shape = androidx.compose.foundation.shape.CutCornerShape(bottomEnd = 6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = if (isDone) Icons.Rounded.CheckCircle else if (isCurrent) Icons.Rounded.Sync else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) Color(0xFF10B981) else if (isCurrent) ThemeNeonCyan else Color.Gray,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            CurrencyText(
                text = title.replace("I. ", "").replace("II. ", "").replace("III. ", "").replace("IV. ", ""),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDone) Color.White else if (isCurrent) ThemeNeonCyan else Color.Gray,
                fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// COMPONENT: METRIC PILL
// ==========================================
@Composable
fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF162238),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                CurrencyText(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 7.5.sp)
                CurrencyText(text = value, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        }
    }
}

// ==========================================
// COMPONENT: FILTER TAB BUTTON
// ==========================================
@Composable
fun FilterTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ThemeNeonCyan else Color(0xFF162238),
        border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else Color(0xFF233554)),
        modifier = modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
            CurrencyText(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color(0xFF002026) else Color.White,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

// ==========================================
// DIALOG: CREATE CONSORTIUM
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConsortiumDialog(
    viewModel: GameViewModel,
    tier4Products: List<Product>,
    onDismiss: () -> Unit,
    onNavigateToRd: (String?) -> Unit = {},
    onCreate: (
        consortiumName: String,
        brandName: String,
        targetProductId: String,
        qualityTier: ConsortiumQualityTier,
        claimedProductIds: Set<String>,
        cityId: String
    ) -> Unit
) {
    var consortiumName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf(tier4Products.firstOrNull()?.id ?: "defense_frigate") }
    var selectedQualityTier by remember { mutableStateOf(ConsortiumQualityTier.GRADE_C) }
    var claimedProductIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val allProducts = remember { Product.values().toList() }
    val eligibleCities = remember(selectedProductId) {
        com.example.data.cities.filter { city ->
            val prod = allProducts.find { it.id == selectedProductId }
            prod?.canBeBuiltIn(city.id) == true
        }
    }
    var selectedCityId by remember(eligibleCities) { mutableStateOf(eligibleCities.firstOrNull()?.id ?: "istanbul") }

    val inventoryState by viewModel.inventory.collectAsStateWithLifecycle(initialValue = emptyList())

    val selectedProduct = remember(selectedProductId, tier4Products) {
        tier4Products.find { it.id == selectedProductId } ?: tier4Products.firstOrNull()
    }
    val isSelectedUnlocked = remember(selectedProductId) {
        viewModel.isProductUnlocked(selectedProductId)
    }

    // Seçilen ürün ve kaliteye göre 4 tedarik gereksinimini dinamik oluştur
    val previewSlots = remember(selectedProductId, selectedQualityTier) {
        val prod = allProducts.find { it.id == selectedProductId }
        val multiplier = selectedQualityTier.requirementMultiplier
        val recipeItems = prod?.recipe ?: emptyList()
        val slotsList = mutableListOf<Triple<Product, Int, Float>>()

        if (recipeItems.isNotEmpty()) {
            recipeItems.take(4).forEach { req ->
                val ingredient = allProducts.find { it.id == req.productId }
                if (ingredient != null) {
                    val qty = (req.amountPerUnit * 25 * multiplier).toInt().coerceAtLeast(20)
                    slotsList.add(Triple(ingredient, qty, 25f))
                }
            }
        }
        while (slotsList.size < 4) {
            val tierOptions = allProducts.filter { it.tier == ProductTier.TIER_2 || it.tier == ProductTier.TIER_3 }
            val fallback = tierOptions.getOrNull(slotsList.size) ?: allProducts.first()
            val qty = (60 * multiplier).toInt()
            slotsList.add(Triple(fallback, qty, 25f))
        }
        slotsList
    }

    val estimatedUnitRevenue = remember(selectedProductId, selectedQualityTier) {
        val base = (selectedProduct?.basePrice ?: 10_000_000L) * 20L
        (base * selectedQualityTier.borsaValueMultiplier).toLong()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0C1322),
            border = BorderStroke(1.5.dp, ThemeNeonCyan.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Handshake, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            CurrencyText(
                                text = tr("🏗️ Konsorsiyum Kurulum Sözleşmesi", "🏗️ Consortium Creation Contract"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            CurrencyText(
                                text = tr("Yüksek Teknoloji & Ortak İmalat Teşekkülü", "High Technology & Joint Manufacturing Enterprise"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. ADIM: HEDEF MEGA PROJE ÜRÜNÜ SEÇİMİ
                    item {
                        CurrencyText(
                            text = tr("1. Hedef Mega Proje Ürünü (Tier 4):", "1. Target Mega Project Product (Tier 4):"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(tier4Products, key = { it.id }) { prod ->
                        val isSelected = prod.id == selectedProductId
                        val isUnlocked = viewModel.isProductUnlocked(prod.id)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.7f) else Color(0xFF162238).copy(alpha = 0.6f),
                            border = BorderStroke(1.5.dp, if (isSelected) ThemeNeonCyan else Color(0xFF233554)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProductId = prod.id
                                    claimedProductIds = emptySet()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else Color(0xFF334155)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(prod.icon, contentDescription = null, tint = if (isSelected) ThemeNeonCyan else ThemeGold, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CurrencyText(prod.getDisplayName(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.5.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isUnlocked) Color(0xFF065F46) else Color(0xFF451A03),
                                            border = BorderStroke(0.8.dp, if (isUnlocked) Color(0xFF34D399) else Color(0xFFF59E0B))
                                        ) {
                                            CurrencyText(
                                                text = if (isUnlocked) "✅ Ar-Ge Hazır" else "🔒 Ar-Ge Gerekli",
                                                color = if (isUnlocked) Color(0xFFA7F3D0) else Color(0xFFFEF3C7),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(tr("Birim İmalat Değeri: ${formatCredit(prod.basePrice)}", "Unit Manufacturing Value: ${formatCredit(prod.basePrice)}"), color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // 1.5. ADIM: ŞEHİR SEÇİMİ
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        CurrencyText(
                            text = tr("1.5. Üretim Tesisinin Kurulacağı Şehir & Coğrafi Konum:", "1.5. City & Geographic Location of the Production Facility:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeNeonCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (eligibleCities.isEmpty()) {
                            CurrencyText(tr("Seçilen ürün için uygun bir şehir bulunamadı.", "No eligible city found for the selected product."), color = Color.Red, fontSize = 11.sp)
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                eligibleCities.forEach { city ->
                                    val isSelected = city.id == selectedCityId
                                    val cardColor = if (isSelected) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF162238)
                                    val borderStroke = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) ThemeNeonCyan else Color(0xFF233554)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = cardColor,
                                        border = borderStroke,
                                        modifier = Modifier
                                            .width(115.dp)
                                            .clickable { selectedCityId = city.id }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CurrencyText(
                                                text = "${city.countryFlag} ${city.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isSelected) ThemeNeonCyan else Color.White,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            CurrencyText(
                                                text = city.getCountryDisplayName(false),
                                                fontSize = 8.5.sp,
                                                color = Color.LightGray,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. ADIM: KALİTE DERECESİ SEÇİMİ (A, B, C)
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        CurrencyText(
                            text = tr("2. Üretim Kalite Derecesi & Borsa Standardı:", "2. Production Quality Grade & Stock Standard:"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ConsortiumQualityTier.values().forEach { tier ->
                                val isSelected = tier == selectedQualityTier
                                val tierColor = when (tier) {
                                    ConsortiumQualityTier.GRADE_A -> Color(0xFFF59E0B)
                                    ConsortiumQualityTier.GRADE_B -> ThemeNeonCyan
                                    ConsortiumQualityTier.GRADE_C -> Color(0xFF94A3B8)
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) tierColor.copy(alpha = 0.2f) else Color(0xFF162238),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) tierColor else Color(0xFF233554)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedQualityTier = tier }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CurrencyText(
                                            text = when (tier) {
                                                ConsortiumQualityTier.GRADE_A -> tr("🏆 A Kalite", "🏆 A Grade")
                                                ConsortiumQualityTier.GRADE_B -> tr("⭐ B Kalite", "⭐ B Grade")
                                                ConsortiumQualityTier.GRADE_C -> tr("📦 C Kalite", "📦 C Grade")
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = if (isSelected) tierColor else Color.White
                                        )
                                        CurrencyText(
                                            text = tr("${tier.requirementMultiplier}x İhtiyaç", "${tier.requirementMultiplier}x Required"),
                                            fontSize = 8.5.sp,
                                            color = Color.LightGray
                                        )
                                        CurrencyText(
                                            text = tr("${tier.borsaValueMultiplier}x Borsa", "${tier.borsaValueMultiplier}x Exchange Value"),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. ADIM: GEREKSİNİMLER & KURUCU KOTA SEÇİMİ
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = "3. Parça İhtiyaçları & Kurucu Kotaları:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNeonCyan
                            )
                            CurrencyText(
                                text = "👑 ${claimedProductIds.size}/4 Parça Kurucuda",
                                fontSize = 9.5.sp,
                                color = ThemeGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CurrencyText(
                            text = "Tedarik edeceğiniz parçaları seçin. Seçmediğiniz parçalar ortak arayan açık slot olarak yayınlanır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 9.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(previewSlots, key = { it.first.id }) { (ingProd, reqQty, share) ->
                        val isClaimedByFounder = claimedProductIds.contains(ingProd.id)
                        val invStock = inventoryState.find { it.itemId == ingProd.id }?.quantity ?: 0
                        val hasEnoughStock = invStock >= reqQty

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isClaimedByFounder) Color(0xFF1E293B) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isClaimedByFounder) ThemeGold else Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    claimedProductIds = if (isClaimedByFounder) {
                                        claimedProductIds - ingProd.id
                                    } else {
                                        claimedProductIds + ingProd.id
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(ingProd.icon, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        CurrencyText(
                                            text = ingProd.getDisplayName(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color.White
                                        )
                                        CurrencyText(
                                            text = tr("Gereken: $reqQty Ton • Deponuzda: $invStock Ton", "Required: $reqQty Tons • In Stock: $invStock Tons"),
                                            fontSize = 9.sp,
                                            color = if (hasEnoughStock) Color(0xFF34D399) else Color.LightGray
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isClaimedByFounder) ThemeGold.copy(alpha = 0.2f) else ThemeNeonCyan.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, if (isClaimedByFounder) ThemeGold else ThemeNeonCyan.copy(alpha = 0.4f))
                                ) {
                                    CurrencyText(
                                        text = if (isClaimedByFounder) tr("👑 BEN ÜRETECEĞİM", "👑 I WILL PRODUCE") else tr("🤝 AÇIK SLOT", "🤝 OPEN SLOT"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = if (isClaimedByFounder) ThemeGold else ThemeNeonCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. ADIM: FİNANSAL TAHMİN ÖZETİ
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF162238),
                            border = BorderStroke(1.dp, Color(0xFF233554)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CurrencyText(tr("Tahmini Parti Satış Geliri:", "Est. Batch Sales Revenue:"), fontSize = 10.5.sp, color = Color.LightGray)
                                    CurrencyText(formatCredit(estimatedUnitRevenue), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ThemeGold)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CurrencyText(tr("Kurucu Yönetim Payı (%10):", "Founder Mgmt Share (10%):"), fontSize = 10.5.sp, color = Color.LightGray)
                                    CurrencyText(formatCredit((estimatedUnitRevenue * 0.10).toLong()), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF34D399))
                                }
                            }
                        }
                    }

                    // 5. ADIM: İSİMLENDİRME
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = consortiumName,
                            onValueChange = { consortiumName = it },
                            label = { CurrencyText(tr("Konsorsiyum Şirket Adı", "Consortium Company Name"), fontSize = 11.sp) },
                            placeholder = { CurrencyText(tr("Örn: Anadolu Ağır Sanayi Konsorsiyumu", "E.g. Anatolian Heavy Industry Consortium")) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeNeonCyan,
                                unfocusedBorderColor = Color(0xFF233554),
                                focusedLabelColor = ThemeNeonCyan,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = brandName,
                            onValueChange = { brandName = it },
                            label = { CurrencyText(tr("Özel Ürün Marka İsmi", "Custom Product Brand Name"), fontSize = 11.sp) },
                            placeholder = { CurrencyText(tr("Örn: TCG HİSAR-V Korvet", "E.g. TCG HISAR-V Corvette")) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeGold,
                                unfocusedBorderColor = Color(0xFF233554),
                                focusedLabelColor = ThemeGold,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isSelectedUnlocked) {
                    AppButton(
                        onClick = {
                            if (consortiumName.isBlank() || brandName.isBlank()) {
                                SmartNotificationManager.show("Lütfen şirket adı ve marka adını giriniz", "Please enter the company name and brand name", NotificationType.ALERT)
                            } else {
                                onCreate(consortiumName, brandName, selectedProductId, selectedQualityTier, claimedProductIds, selectedCityId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CurrencyText(tr("SÖZLEŞMEYİ İMZALA (10 💎)", "SIGN CONTRACT (10 💎)"), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    AppButton(
                        onClick = {
                            onDismiss()
                            val techNode = com.example.data.TechTree.nodes.find { it.unlockedProductIds.contains(selectedProductId) }
                            onNavigateToRd(techNode?.id?.removePrefix("tech_"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFFE040FB)),
                        border = BorderStroke(1.dp, Color(0xFFE040FB)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        CurrencyText("🔬 " + tr("AR-GE ARAŞTIRMASINA GİT", "GO TO R&D RESEARCH"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// DIALOG: DELIVER MATERIAL
// ==========================================
@Composable
fun DeliverMaterialDialog(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    project: MegaProject,
    slot: ConsortiumSupplierSlot,
    userStock: Int,
    onDismiss: () -> Unit,
    onDeliver: (quantity: Int) -> Unit
) {
    var deliverAmountText by remember { mutableStateOf(slot.remainingQuantity.coerceAtMost(userStock).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF111A2E).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, ThemeNeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = "📦 Tedarik Şantiyesine Teslimat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131D31),
                    border = BorderStroke(1.dp, Color(0xFF233554)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProjectDynamicIcon(project = project, size = 42.dp, iconSize = 22.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            CurrencyText(
                                text = "Proje: ${project.consortiumName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SlotDynamicIcon(productId = slot.productId, isUserSlot = true, isFullyDelivered = slot.isFullyDelivered, size = 20.dp, iconSize = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = "${slot.productName} • Kalan Kota: ${slot.remainingQuantity} Ton",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeGold
                                )
                            }
                        }
                    }
                }
                // 4. HIZLI SEVKİYAT ROZETİ (One-Tap Warehouse Sync)
                OneTapWarehouseSyncBadge(
                    userStock = userStock,
                    remainingNeeded = slot.remainingQuantity,
                    onOneTapSync = {
                        val toTransfer = minOf(userStock, slot.remainingQuantity)
                        if (toTransfer > 0) {
                            onDeliver(toTransfer)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deliverAmountText,
                    onValueChange = { deliverAmountText = it.filter { c -> c.isDigit() } },
                    label = { CurrencyText("Teslim Edilecek Miktar (Ton)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeNeonCyan,
                        unfocusedBorderColor = Color(0xFF233554),
                        focusedLabelColor = ThemeNeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppButton(
                        onClick = { deliverAmountText = (userStock / 2).coerceAtMost(slot.remainingQuantity).toString() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D4A), contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) { CurrencyText("%50", fontSize = 10.sp) }

                    AppButton(
                        onClick = { deliverAmountText = userStock.coerceAtMost(slot.remainingQuantity).toString() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D4A), contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) { CurrencyText("MAX STOK", fontSize = 10.sp) }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val qty = deliverAmountText.toIntOrNull() ?: 0
                AppButton(
                    onClick = {
                        if (qty <= 0) {
                            SmartNotificationManager.show("Lütfen geçerli miktar giriniz", NotificationType.ALERT)
                        } else if (qty > userStock) {
                            SmartNotificationManager.show("Deponuzda yeterli stok bulunmuyor!", NotificationType.ALERT)
                        } else {
                            onDeliver(qty)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CurrencyText("ŞANTİYEYE TESLİM ET", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: CONSORTIUM CHAT DIALOG
// ==========================================
@Composable
fun ConsortiumChatDialog(
    project: MegaProject,
    messages: List<ConsortiumChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val isMember = remember(project) {
        project.participatingPartnerIds.contains("local_player") ||
        project.leaderPlayerId == "local_player" ||
        project.slots.any { it.assignedPartnerId == "local_player" } ||
        true
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0D1527).copy(alpha = 0.85f),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(ThemeNeonCyan, ThemeGold))),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ProjectDynamicIcon(project = project, size = 42.dp, iconSize = 22.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyText(
                                    text = project.consortiumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    CurrencyText(
                                        text = "🔴 CANLI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF34D399),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            CurrencyText(
                                text = "Ortaklar Grup Sohbeti & Anlık Koordinasyon Hub'ı",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                if (!isMember) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2B1717),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = "🔒 Yalnızca konsorsiyum üyeleri sohbet edebilir. Mesaj yazmak için bir tedarikçi slotuna katılın.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Messages Stream
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CurrencyText(
                                text = if (isMember) "Sohbet henüz boş. İlk mesajı siz gönderin veya hızlı butonları kullanın!" else "Sohbet geçmişi henüz boş.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageBubble(msg = msg)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Chips (Only for members)
                if (isMember) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickChips = listOf(
                            "📦 Teslimat yolda!",
                            "⚡ Kota devri bekleniyor",
                            "🤝 Teşekkürler ortak!",
                            "🏭 Seri üretim aktif!",
                            "💰 Temettü dağıtımı harika"
                        )
                        quickChips.forEach { chipText ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSendMessage(chipText)
                                }
                            ) {
                                CurrencyText(
                                    text = chipText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeNeonCyan,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Message Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { if (isMember) messageInput = it },
                        enabled = isMember,
                        placeholder = { CurrencyText(if (isMember) "Mesajınızı yazın..." else "🔒 Sadece üyeler mesaj gönderebilir", fontSize = 11.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeNeonCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledContainerColor = Color(0xFF0B101D),
                            disabledBorderColor = Color(0xFF1E293B)
                        ),
                        singleLine = true
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMember && messageInput.isNotBlank()) ThemeNeonCyan else Color(0xFF334155),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(enabled = isMember && messageInput.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendMessage(messageInput)
                                messageInput = ""
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMember) Icons.Rounded.Send else Icons.Rounded.Lock,
                                contentDescription = "Gönder",
                                tint = if (isMember && messageInput.isNotBlank()) Color(0xFF002026) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: CHAT MESSAGE BUBBLE
// ==========================================
@Composable
fun ChatMessageBubble(msg: ConsortiumChatMessage) {
    val isMe = msg.senderId == "local_player"
    val formattedTime = remember(msg.timestampMs) {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(msg.timestampMs))
    }

    if (msg.isSystemMessage) {
        // System Event Message Badge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E1B4B),
                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = msg.messageText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC7D2FE),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                }
            }
        }
    } else {
        // User / Partner Chat Message
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyText(
                    text = msg.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) ThemeNeonCyan else ThemeGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF1E293B)
                ) {
                    CurrencyText(
                        text = msg.senderRole,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                CurrencyText(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomStart = if (isMe) 10.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 10.dp
                ),
                color = if (isMe) Color(0xFF0284C7) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isMe) ThemeNeonCyan else Color(0xFF334155))
            ) {
                CurrencyText(
                    text = msg.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: DYNAMIC PROJECT ICON
// ==========================================
@Composable
fun ProjectDynamicIcon(
    project: MegaProject,
    size: Dp = 48.dp,
    iconSize: Dp = 26.dp,
    showBadges: Boolean = true
) {
    val primaryColor = remember(project.targetProductId) { project.getProductPrimaryColor() }
    val isFinished = project.isAllStagesFinished
    val isStage4 = project.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION
    val openSlotsCount = remember(project.slots) { project.slots.count { it.assignedPartnerId == null } }
    val productDrawableRes = remember(project.targetProductId) {
        ProductDrawables.getProductDrawableResId(project.targetProductId)
    }

    Box(contentAlignment = Alignment.Center) {
        if (productDrawableRes != 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                modifier = Modifier.size(size)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = productDrawableRes),
                        contentDescription = project.targetProductName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        } else {
            Tier4PremiumIcon(
                productId = project.targetProductId,
                size = size,
                primaryColor = primaryColor
            )
        }
        
        if (showBadges) {
            if (isFinished || isStage4) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(16.dp)
                        .background(ThemePositive, CircleShape)
                        .border(1.dp, Color(0xFF070B14), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            } else if (openSlotsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(18.dp)
                        .background(ThemeNegative, CircleShape)
                        .border(1.dp, Color(0xFF070B14), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CurrencyText(
                        text = openSlotsCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: DYNAMIC SLOT PRODUCT ICON
// ==========================================
@Composable
fun SlotDynamicIcon(
    productId: String,
    isUserSlot: Boolean,
    isFullyDelivered: Boolean,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp
) {
    val accentColor = if (isFullyDelivered) Color(0xFF34D399) else if (isUserSlot) ThemeNeonCyan else ThemeGold

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
        modifier = Modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            UniversalProductIcon(
                productId = productId,
                size = iconSize,
                tint = accentColor
            )
        }
    }
}

fun getSlotProductIcon(productId: String): ImageVector {
    val p = productId.lowercase()
    return when {
        p.contains("chip") || p.contains("cpu") || p.contains("processor") || p.contains("ai") || p.contains("semiconductor") -> Icons.Rounded.Memory
        p.contains("engine") || p.contains("motor") || p.contains("turbine") || p.contains("propulsion") -> Icons.Rounded.Settings
        p.contains("metal") || p.contains("steel") || p.contains("iron") || p.contains("composite") || p.contains("titanium") || p.contains("aluminum") -> Icons.Rounded.ViewInAr
        p.contains("battery") || p.contains("plasma") || p.contains("fuel") || p.contains("core") || p.contains("fusion") -> Icons.Rounded.Bolt
        p.contains("panel") || p.contains("glass") || p.contains("hull") || p.contains("body") || p.contains("armor") -> Icons.Rounded.Layers
        p.contains("sensor") || p.contains("radar") || p.contains("laser") || p.contains("optics") || p.contains("lidar") -> Icons.Rounded.Sensors
        p.contains("software") || p.contains("os") || p.contains("code") || p.contains("quantum") -> Icons.Rounded.Code
        else -> Icons.Rounded.Category
    }
}

// ==========================================
// COMPONENT: CONSORTIUM PRODUCTION LINE CARD
// ==========================================
@Composable
fun ConsortiumProductionLineCard(project: MegaProject) {
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(project.isBatchInProduction, project.batchProductionStartTimeMs) {
        if (project.isBatchInProduction) {
            while (true) {
                currentTimeMs = System.currentTimeMillis()
                kotlinx.coroutines.delay(250)
            }
        }
    }

    val isBatchInProd = project.isBatchInProduction
    val remainingMs = project.remainingProductionTimeMs
    val remainingSec = (remainingMs / 1000L).coerceAtLeast(0L)
    val totalSec = project.standardBatchDurationSeconds
    val progressFraction = project.productionProgressFraction

    val formattedTotalDuration = if (totalSec >= 60) {
        val mins = totalSec / 60
        val secs = totalSec % 60
        if (secs > 0) "$mins Dk $secs Sn" else "$mins Dk"
    } else {
        "${totalSec}s"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isBatchInProd) Color(0xFF1E1B4B) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isBatchInProd) Color(0xFFA855F7) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isBatchInProd) Icons.Rounded.PrecisionManufacturing else Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = if (isBatchInProd) ThemeNeonCyan else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = if (isBatchInProd) "⚙️ SERİ ÜRETİM BANDI ÇALIŞIYOR" else "📦 HAMMADDE & TEDARİK AŞAMASI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isBatchInProd) ThemeNeonCyan else Color.White,
                        fontSize = 10.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isBatchInProd) Color(0xFF581C87) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isBatchInProd) Color(0xFFC084FC) else Color(0xFF475569))
                ) {
                    CurrencyText(
                        text = "⏱️ Standart Süre: $formattedTotalDuration",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBatchInProd) Color(0xFFF3E8FF) else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isBatchInProd) {
                CurrencyText(
                    text = "Tüm hammaddeler bantta hassas montaj ve kalite kontrolden geçiyor. Üretim süreci tamamlandığında ürün depoya kaldırılacaktır.",
                    fontSize = 8.5.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ThemeNeonCyan,
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = "Montaj İlerlemesi: %${(progressFraction * 100).toInt()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeNeonCyan
                    )
                    CurrencyText(
                        text = "⏱️ Kalan Süre: ${String.format("%02d:%02d", remainingSec / 60, remainingSec % 60)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = ThemeGold
                    )
                }
            } else {
                CurrencyText(
                    text = "Ortaklar tüm hammaddeleri (%100) kotalara teslim ettiğinde $formattedTotalDuration sürecek Seri Üretim Bandı otomatik başlatılacaktır.",
                    fontSize = 8.5.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Isolated, high-performance Consortium Crisis Banner.
 * Uses remember and derived state to prevent recomposition of the main hub during crisis state changes.
 */
@Composable
fun ConsortiumCrisisBanner(
    crisisState: ConsortiumCrisisState,
    allProducts: Array<Product>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(crisisState.crisisTitle, crisisState.isCrisisActive) {
        if (crisisState.isCrisisActive) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val bottleneckText = remember(crisisState.bottleneckPercentage) {
        "DARBOĞAZ: %${crisisState.bottleneckPercentage.toInt()}"
    }

    val missingNames = remember(crisisState.criticalMissingMaterials, allProducts) {
        if (crisisState.criticalMissingMaterials.isNotEmpty()) {
            crisisState.criticalMissingMaterials.mapNotNull { matId ->
                allProducts.find { p -> p.id == matId }?.getDisplayName()
            }.joinToString(", ")
        } else ""
    }

    val infiniteTransition = rememberInfiniteTransition(label = "crisis_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crisis_border_pulse"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF3B0B0B).copy(alpha = 0.90f),
        border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = pulseAlpha)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrencyText(
                        text = crisisState.crisisTitle,
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF7F1D1D)
                ) {
                    CurrencyText(
                        text = bottleneckText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            CurrencyText(
                text = crisisState.crisisDescription,
                color = Color(0xFFFEE2E2),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            if (missingNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                CurrencyText(
                    text = "Kritik Eksikler: $missingNames",
                    color = ThemeGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun ConsortiumHolographicMatrix(
    project: MegaProject,
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel,
    onNavigateToRd: (String?) -> Unit,
    onNavigateToFacilities: () -> Unit,
    onNavigateToBorsa: () -> Unit,
    onNavigateToMarket: () -> Unit,
    isLeader: Boolean,
    onJoinSlot: (ConsortiumSupplierSlot) -> Unit,
    onDeliverClick: (ConsortiumSupplierSlot) -> Unit,
    onLeaveSlot: (ConsortiumSupplierSlot) -> Unit,
    onTakeoverBottleneck: (ConsortiumSupplierSlot) -> Unit,
    onKickPartnerFromSlot: (ConsortiumSupplierSlot) -> Unit
) {
    var selectedSlot by remember { mutableStateOf<ConsortiumSupplierSlot?>(null) }
    val slots = project.slots
    val isFinished = project.currentStage == MegaProjectStage.COMPLETED

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, ThemeNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        val width = maxWidth
        val height = maxHeight
        val centerX = width / 2
        val centerY = height / 2

        val radiusX = width * 0.38f
        val radiusY = height * 0.35f

        // Draw connections
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw grid lines for blueprint effect
            val gridStep = 40.dp.toPx()
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

            // Draw lines from center to each node
            slots.forEachIndexed { index, slot ->
                val angle = (2 * Math.PI * index / slots.size).toFloat() - (Math.PI / 2).toFloat()
                val nodeX = centerX.toPx() + radiusX.toPx() * cos(angle)
                val nodeY = centerY.toPx() + radiusY.toPx() * sin(angle)
                
                // Draw connecting line
                drawLine(
                    color = if (slot.isFullyDelivered) ThemeNeonCyan else Color.Gray.copy(alpha = 0.4f),
                    start = Offset(centerX.toPx(), centerY.toPx()),
                    end = Offset(nodeX, nodeY),
                    strokeWidth = if (slot.isFullyDelivered) 3f else 1.5f,
                    pathEffect = if (!slot.isFullyDelivered) PathEffect.dashPathEffect(floatArrayOf(15f, 10f)) else null
                )
                
                // Draw a small dot at the end
                drawCircle(
                    color = if (slot.isFullyDelivered) ThemeNeonCyan else Color.Gray.copy(alpha = 0.8f),
                    radius = 4.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }
        }

        // Center Node
        Box(
            modifier = Modifier
                .offset(x = centerX - 40.dp, y = centerY - 40.dp)
                .size(80.dp)
                .shadow(24.dp, CircleShape, spotColor = ThemeNeonCyan)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))))
                .border(2.dp, if (isFinished) Color(0xFF10B981) else ThemeNeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ProjectDynamicIcon(project = project, size = 60.dp, iconSize = 35.dp)
        }

        // Orbit Nodes
        slots.forEachIndexed { index, slot ->
            val angle = (2 * Math.PI * index / slots.size).toFloat() - (Math.PI / 2).toFloat()
            val offsetX = centerX + (radiusX.value * cos(angle)).dp
            val offsetY = centerY + (radiusY.value * sin(angle)).dp

            // Each node is a Box offset to its position
            Box(
                modifier = Modifier
                    .offset(x = offsetX - 25.dp, y = offsetY - 25.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (selectedSlot == slot) Color(0xFF1E293B) else Color(0xFF0F172A))
                    .border(
                        width = if (slot.isFullyDelivered) 2.dp else if (selectedSlot == slot) 1.5.dp else 1.dp,
                        color = if (slot.isFullyDelivered) ThemeNeonCyan else if (selectedSlot == slot) Color.White else Color.Gray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { selectedSlot = if (selectedSlot == slot) null else slot },
                contentAlignment = Alignment.Center
            ) {
                // Real product image for the slot's product from drawable
                UniversalProductIcon(
                    productId = slot.productId,
                    displayName = slot.productName,
                    fallbackVector = getSlotProductIcon(slot.productId),
                    size = 28.dp,
                    tint = if (slot.isFullyDelivered) ThemeNeonCyan else Color.LightGray
                )
                if (!slot.isFullyDelivered) {
                    CircularProgressIndicator(
                        progress = { slot.progressFraction },
                        color = ThemeNeonCyan,
                        trackColor = Color.Transparent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
            }
        }
        
        // Detailed panel for selected slot overlaying at the bottom
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedSlot != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedSlot?.let { slot ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF101828).copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.8f)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UniversalProductIcon(
                                    productId = slot.productId,
                                    displayName = slot.productName,
                                    fallbackVector = getSlotProductIcon(slot.productId),
                                    size = 20.dp,
                                    tint = ThemeNeonCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(
                                    text = slot.productName + " " + com.example.ui.theme.tr("Entegrasyonu", "Integration"),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { selectedSlot = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, tint = Color.LightGray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ConsortiumSlotRow(
                            uiState = uiState,
                            onIntent = onIntent,
                            slot = slot,
                            viewModel = viewModel,
                            projectId = project.id,
                            onNavigateToRd = onNavigateToRd,
                            onNavigateToFacilities = onNavigateToFacilities,
                            onNavigateToBorsa = onNavigateToBorsa,
                            onNavigateToMarket = onNavigateToMarket,
                            currentProjectStage = project.currentStage,
                            isProjectFinished = isFinished,
                            isLeader = isLeader,
                            onJoin = { onJoinSlot(slot) },
                            onDeliver = { onDeliverClick(slot) },
                            onLeave = { onLeaveSlot(slot) },
                            onTakeover = { onTakeoverBottleneck(slot) },
                            onKickPartner = { onKickPartnerFromSlot(slot) }
                        )
                    }
                }
            }
        }
    }
}
