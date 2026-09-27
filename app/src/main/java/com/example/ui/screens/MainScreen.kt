package com.example.ui.screens

import com.example.viewmodel.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.RobotoMonoFontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppButton
import com.example.ui.components.GameTopBar
import com.example.ui.components.MeshBackground
import com.example.ui.components.ParticleSystem
import com.example.ui.components.DynamicIslandNotification
import com.example.ui.components.TycoonBottomNavBar
import androidx.compose.material.icons.filled.Lock
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationType
import com.example.ui.components.ScreenLoadingTransitionOverlay
import com.example.ui.components.AppIntroLoadingScreen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.viewmodel.GameViewModel
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearOutSlowInEasing

import androidx.compose.animation.core.Spring
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainScreen(gameViewModel: GameViewModel) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    
    
    val anomalyState by com.example.data.telemetry.AppExceptionHandler.anomalyState.collectAsStateWithLifecycle()
    
    val gameState = uiState.gameStateObj
    val isOnlineRegistered = uiState.settingsState.isOnlineRegistered
    val dailyRewardData = uiState.dailyRewardData
    val offlineEarningsData = uiState.offlineEarningsData
    val newsTickerMessage = uiState.newsTickerMessage
    val economicSnapshot = uiState.economicSnapshot
    
    val haptic = LocalHapticFeedback.current
    val hasSetWarehouse = economicSnapshot?.hasSetWarehouse == true || (economicSnapshot?.isDataSaved == true)
    var showOnboarding by remember { mutableStateOf(false) }
    var showAppIntroLoading by rememberSaveable { mutableStateOf(true) }

    val hasCompletedFirstTrade = uiState.hasCompletedFirstTrade
    val tradeTutorialStep = uiState.tradeTutorialStep

    var activeRouteForOverlay by remember { mutableStateOf(currentRoute) }

    LaunchedEffect(currentRoute) {
        if (activeRouteForOverlay != currentRoute) {
            activeRouteForOverlay = currentRoute
            // Asynchronous background local persist without blocking the UI thread or adding navigation latency
            gameViewModel.saveEconomicDataToDataStore(immediate = false)
        }
    }

    LaunchedEffect(economicSnapshot) {
        if (economicSnapshot != null && !hasSetWarehouse) {
            showOnboarding = true
        }
    }

    LaunchedEffect(currentRoute, tradeTutorialStep) {
        if ((currentRoute == "borsa" || currentRoute == "market") && tradeTutorialStep in 1..3) {
            gameViewModel.advanceTradeTutorialStep(4)
        }
    }

    val currentAppLanguage = LocalAppLanguage.current
    val navItems = remember(currentAppLanguage) {
        if (currentAppLanguage == AppLanguage.ENGLISH) {
            listOf(
                NavItem("home", "Home", Icons.Default.Home),
                NavItem("map", "Map", Icons.Default.Map),
                NavItem("production", "Facilities", Icons.Default.Business),
                NavItem("megaproject", "Consortium", Icons.Default.Groups),
                NavItem("market", "Market", Icons.Default.ShoppingCart),
                NavItem("borsa", "Exchange", Icons.AutoMirrored.Filled.ShowChart),
                NavItem("hr", "HR", Icons.Default.People),
                NavItem("rd", "R&D", Icons.Default.Science),
                NavItem("statistics", "Report", Icons.Default.BarChart),
                NavItem("social", "Leaderboard", Icons.Default.Public)
            )
        } else {
            listOf(
                NavItem("home", "Ana Ekran", Icons.Default.Home),
                NavItem("map", "Harita", Icons.Default.Map),
                NavItem("production", "Üretim", Icons.Default.Business),
                NavItem("megaproject", "Konsorsiyum", Icons.Default.Groups),
                NavItem("market", "Pazar", Icons.Default.ShoppingCart),
                NavItem("borsa", "Borsa", Icons.AutoMirrored.Filled.ShowChart),
                NavItem("hr", "İ.K.", Icons.Default.People),
                NavItem("rd", "AR-GE", Icons.Default.Science),
                NavItem("statistics", "Rapor", Icons.Default.BarChart),
                NavItem("social", "Liderlik Tablosu", Icons.Default.Public)
            )
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            viewModel = gameViewModel,
            onComplete = { selectedInitialCityId ->
                gameViewModel.setInitialWarehouseCity(selectedInitialCityId)
                gameViewModel.resetTradeTutorial()
                showOnboarding = false
                navController.navigate("map") { launchSingleTop = true }
            }
        )
    }

    var showAuthGate by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    if (showAuthGate) {
        AuthScreen(
            viewModel = gameViewModel,
            onSuccess = {
                showAuthGate = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            GameTopBar(
                viewModel = gameViewModel,
                currentRoute = currentRoute ?: "home",
                onNavigateToHome = {
                    if (currentRoute != "home") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToBank = {
                    if (currentRoute != "bank") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("bank") {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToInventory = {
                    if (currentRoute != "inventory") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("inventory") {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToWeeklyGrowth = {
                    if (currentRoute != "weekly_growth") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("weekly_growth") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        },
        bottomBar = {
            TycoonBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                uiState = uiState
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            MeshBackground(gameState = gameState, isCelebrating = false)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 1400.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    // Main content area with padding to avoid overlap with floating bar
                    NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) + slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight / 10 },
                            animationSpec = tween(300, easing = LinearOutSlowInEasing)
                        )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(200))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(300)) + slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight / 10 },
                            animationSpec = tween(300, easing = LinearOutSlowInEasing)
                        )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(200))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 1200.dp)
                        
                ) {
                    composable("home") {
                        DashboardScreen(
                            uiState = uiState,
                            onIntent = gameViewModel::handleIntent,
                            viewModel = gameViewModel,
                            onNavigateToMarket = { navController.navigate("market") { launchSingleTop = true } },
                            onNavigateToBorsa = { navController.navigate("borsa") { launchSingleTop = true } },
                            onNavigateToProduction = { productId -> navController.navigate("production" + (if (productId != null) "?productId=$productId" else "")) { launchSingleTop = true } },
                            onNavigateToHr = { navController.navigate("hr") { launchSingleTop = true } },
                            onNavigateToRd = { navController.navigate("rd") { launchSingleTop = true } },
                            onNavigateToBank = { navController.navigate("bank") { launchSingleTop = true } },
                            onNavigateToMap = { navController.navigate("map") { launchSingleTop = true } },
                            onNavigateToStatistics = { navController.navigate("statistics") { launchSingleTop = true } },
                            onNavigateToSocial = { navController.navigate("social") { launchSingleTop = true } },
                            onNavigateToInventory = { navController.navigate("inventory") { launchSingleTop = true } },
                            onNavigateToMegaProject = { navController.navigate("megaproject") { launchSingleTop = true } },
                            onNavigateToWeeklyGrowth = { navController.navigate("weekly_growth") { launchSingleTop = true } }
                        )
                    }
                    composable("map") {
                        CityMapScreen(
                            uiState = uiState,
                            onIntent = gameViewModel::handleIntent,
                            viewModel = gameViewModel,
                            onNavigateToMarket = {
                                if (!isOnlineRegistered) {
                                    SmartNotificationManager.show("Yalnızca kayıtlı kullanıcılar için", NotificationType.ALERT)
                                } else {
                                    navController.navigate("market") { launchSingleTop = true }
                                }
                            },
                            onNavigateToBorsa = { navController.navigate("borsa") { launchSingleTop = true } },
                            onNavigateToProduction = { navController.navigate("production") { launchSingleTop = true } },
                            onNavigateToHr = { navController.navigate("hr") { launchSingleTop = true } },
                            onNavigateToBank = { navController.navigate("bank") { launchSingleTop = true } }
                        )
                    }
                    composable(
                        route = "production?productId={productId}",
                        arguments = listOf(androidx.navigation.navArgument("productId") { nullable = true })
                    ) { backStackEntry -> 
                        val productId = backStackEntry.arguments?.getString("productId")
                        AssetsScreen(
                            uiState = uiState,
                            onIntent = gameViewModel::handleIntent,
                            viewModel = gameViewModel, 
                            initialProductId = productId,
                            onNavigateToRd = { techId -> 
                                val route = if (techId != null) "rd?techId=$techId" else "rd"
                                navController.navigate(route) { launchSingleTop = true } 
                            },
                            onNavigateToConsortium = {
                                navController.navigate("megaproject") { launchSingleTop = true }
                            }
                        ) 
                    }
                    composable("inventory") { InventoryScreen(uiState, gameViewModel::handleIntent, gameViewModel, onNavigateToMarket = { navController.navigate("market") }) }
                    composable("borsa") { BorsaScreen(uiState, gameViewModel::handleIntent, gameViewModel) }
                    composable("market") { 
                        MarketScreen(
                            uiState = uiState, 
                            onIntent = gameViewModel::handleIntent, 
                            viewModel = gameViewModel,
                            onNavigateHome = { navController.navigate("home") { popUpTo("home") { inclusive = false } } }
                        ) 
                    }
                    composable("hr") {
                        HrScreen(uiState, gameViewModel::handleIntent, gameViewModel)
                    }
                    composable(
                        route = "rd?techId={techId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("techId") { nullable = true }
                        )
                    ) { backStackEntry ->
                        val techId = backStackEntry.arguments?.getString("techId")
                        RdScreen(uiState, gameViewModel::handleIntent, gameViewModel, initialHighlightedTechKey = techId)
                    }
                    composable("statistics") { 
                        StatisticsScreen(
                            uiState = uiState,
                            onIntent = gameViewModel::handleIntent,
                            viewModel = gameViewModel,
                            onNavigateToInventory = { navController.navigate("inventory") { launchSingleTop = true } },
                            onNavigateToWeeklyGrowth = { navController.navigate("weekly_growth") { launchSingleTop = true } },
                            onNavigateToProduction = { productId -> 
                                val route = if (productId != null) "production?productId=$productId" else "production"
                                navController.navigate(route) { launchSingleTop = true } 
                            },
                            onNavigateToMarket = { navController.navigate("market") { launchSingleTop = true } },
                            onNavigateToBorsa = { navController.navigate("borsa") { launchSingleTop = true } },
                            onNavigateToBank = { navController.navigate("bank") { launchSingleTop = true } },
                            onNavigateToHr = { navController.navigate("hr") { launchSingleTop = true } },
                            onNavigateToRd = { techId ->
                                val route = if (techId != null) "rd?techId=$techId" else "rd"
                                navController.navigate(route) { launchSingleTop = true }
                            },
                            onNavigateToMap = { navController.navigate("map") { launchSingleTop = true } }
                        ) 
                    }
                    composable("weekly_growth") {
                        WeeklyGrowthChartScreen(
                            uiState = uiState,
                            onIntent = gameViewModel::handleIntent,
                            viewModel = gameViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("social") { SocialScreen(uiState, gameViewModel::handleIntent, gameViewModel) }
                    composable("bank") { BankScreen(uiState, gameViewModel::handleIntent, gameViewModel) }
                    composable("megaproject") {
                        MegaProjectHubScreen(uiState, gameViewModel::handleIntent, 
                            viewModel = gameViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRd = { techId ->
                                val route = if (techId != null) "rd?techId=$techId" else "rd"
                                navController.navigate(route) { launchSingleTop = true }
                            },
                            onNavigateToFacilities = { navController.navigate("map") { launchSingleTop = true } },
                            onNavigateToBorsa = { navController.navigate("borsa") { launchSingleTop = true } },
                            onNavigateToMarket = { navController.navigate("market") { launchSingleTop = true } }
                        )
                    }
                }

                ParticleSystem()

                // Google Play In-App Update Downloaded Banner
                val isUpdateDownloaded by com.example.utils.InAppUpdateManager.isUpdateDownloaded.collectAsStateWithLifecycle()
                if (isUpdateDownloaded) {
                    val isEng = currentAppLanguage == AppLanguage.ENGLISH
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                            .clickable { com.example.utils.InAppUpdateManager.completeUpdate() },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ThemeNeonCyan),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEng) "Update Ready to Install" else "Yeni Güncelleme İndirildi!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isEng) "Tap to restart and apply new features" else "Son sürüme geçmek için dokunup yeniden başlatın",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            TextButton(onClick = { com.example.utils.InAppUpdateManager.completeUpdate() }) {
                                Text(if (isEng) "RESTART" else "BAŞLAT", color = ThemeGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }

            val showDailyQuestsDialog by gameViewModel.showDailyQuestsDialog.collectAsStateWithLifecycle()
            if (showDailyQuestsDialog) {
                com.example.ui.components.DailyQuestsAndPassDialog(
                    viewModel = gameViewModel,
                    onDismiss = { gameViewModel.dismissDailyQuestsDialog() },
                    onNavigateToRoute = { route ->
                        gameViewModel.dismissDailyQuestsDialog()
                        navController.navigate(route) { launchSingleTop = true }
                    }
                )
            }

            dailyRewardData?.let { rewardData ->
                com.example.ui.components.DailyRewardDialog(
                    data = rewardData,
                    onDismiss = { gameViewModel.dismissDailyRewardDialog() }
                )
            }

            val morningReport by gameViewModel.morningReport.collectAsStateWithLifecycle()
            val playerGems = uiState.playerState.player?.gems ?: 0
            if (morningReport != null) {
                com.example.ui.components.MorningReportDialog(
                    report = morningReport!!,
                    playerGems = playerGems,
                    gemCost = 2,
                    onClaimNormal = { gameViewModel.claimMorningReport(doubleBonus = false) },
                    onClaimDoubleWithGems = { gameViewModel.claimMorningReportWithGems(2) },
                    onClaimDoubleWithAd = { gameViewModel.claimMorningReportWithAd() },
                    onClaimDoubleBonus = { gameViewModel.claimMorningReport(doubleBonus = true) },
                    onDismiss = { gameViewModel.dismissMorningReport() }
                )
            } else if (offlineEarningsData != null) {
                val playerGems = uiState.playerState.player?.gems ?: 0
                com.example.ui.components.OfflineEarningsDialog(
                    data = offlineEarningsData,
                    playerGems = playerGems,
                    gemCost = 2,
                    onDismiss = { gameViewModel.dismissOfflineEarningsDialog() },
                    onDoubleWithGems = { gameViewModel.claimOfflineBonusWithGems(2) },
                    onDoubleBonusClaim = { gameViewModel.claimOfflineBonusWithAd() }
                )
            }

            anomalyState?.let { anomaly ->
                com.example.ui.components.SafeFallbackDialog(
                    anomaly = anomaly,
                    onDismiss = { com.example.data.telemetry.AppExceptionHandler.clearAnomaly() }
                )
            }
            }
        }
    }

    // 3-Second Cyber Entrance & Cloud Sync Loading Screen
    AnimatedVisibility(
        visible = showAppIntroLoading,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.05f, animationSpec = tween(400)),
        modifier = Modifier.fillMaxSize()
    ) {
        AppIntroLoadingScreen(
            gameViewModel = gameViewModel,
            onFinished = {
                showAppIntroLoading = false
            }
        )
    }
}
}
}
