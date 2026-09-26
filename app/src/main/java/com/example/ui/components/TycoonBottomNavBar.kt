package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.tr
import com.example.viewmodel.GameUiState

data class TycoonNavTab(
    val route: String,
    val titleTr: String,
    val titleEn: String,
    val icon: ImageVector,
    val iconRes: Int? = null,
    val accentColor: Color
)

data class QuickMenuItem(
    val route: String,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val iconRes: Int? = null,
    val accentColor: Color,
    val categoryTr: String,
    val categoryEn: String,
    val badgeText: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TycoonBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    uiState: GameUiState? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showAllMenusSheet by remember { mutableStateOf(false) }
    var lockedDialogInfo by remember { mutableStateOf<com.example.data.FeatureLockInfo?>(null) }

    val playerLevel = uiState?.playerState?.player?.level ?: 1
    val completedTechMap = uiState?.hrState?.researchLevels ?: emptyMap()

    fun checkRouteLock(route: String): com.example.data.FeatureLockInfo? {
        val feature = when (route) {
            "production" -> com.example.data.GameFeature.FACILITIES
            "market" -> com.example.data.GameFeature.MARKET_P2P
            "borsa" -> com.example.data.GameFeature.BORSA
            "inventory" -> com.example.data.GameFeature.WAREHOUSE
            "bank" -> com.example.data.GameFeature.BANKING
            "rd" -> com.example.data.GameFeature.RD_LAB
            "hr" -> com.example.data.GameFeature.HR_MANAGERS
            "megaproject" -> com.example.data.GameFeature.CONSORTIUM
            "museum" -> com.example.data.GameFeature.MUSEUM
            else -> null
        } ?: return null

        val lockInfo = com.example.data.FeatureLockManager.getLockInfo(feature, playerLevel, completedTechMap)
        return if (!lockInfo.isUnlocked) lockInfo else null
    }

    // Primary 5 Navigation Tabs on the Bottom Bar with Custom WebP / 3D Graphics
    val tabs = listOf(
        TycoonNavTab("home", "Ana Üs", "Base", Icons.Rounded.Dashboard, R.drawable.ic_tab_headquarters, Color(0xFF00E5FF)),
        TycoonNavTab("production", "Üretim", "Production", Icons.Rounded.PrecisionManufacturing, R.drawable.bg_menu_production, Color(0xFF10B981)),
        TycoonNavTab("market", "Pazar", "Market", Icons.Rounded.Storefront, R.drawable.ic_tab_marketplace, Color(0xFFF59E0B)),
        TycoonNavTab("borsa", "Borsa", "Exchange", Icons.AutoMirrored.Filled.ShowChart, R.drawable.bg_borsa_header, Color(0xFF38BDF8)),
        TycoonNavTab("menu", "Tüm Menüler", "All Menus", Icons.Rounded.GridView, null, Color(0xFFA855F7))
    )

    // Derived states for live badges
    val activeBusinessesCount = uiState?.businesses?.size ?: 0
    val activeResearchesCount = uiState?.hrState?.activeResearches?.size ?: 0
    val totalInventoryCount = uiState?.currentInvCount ?: 0
    val maxInventoryCapacity = uiState?.playerState?.player?.inventoryCapacity ?: 5000
    val invFillPercent = if (maxInventoryCapacity > 0) ((totalInventoryCount.toFloat() / maxInventoryCapacity) * 100).toInt() else 0
    val megaProjectsCount = uiState?.consortiumState?.megaProjects?.size ?: 0
    val managersCount = uiState?.managers?.size ?: 0

    // Full catalog of all modules in the simulation with matched drawable assets
    val allMenuItems = remember(
        activeBusinessesCount,
        activeResearchesCount,
        invFillPercent,
        megaProjectsCount,
        managersCount
    ) {
        listOf(
            // SANAYİ & OPERASYON
            QuickMenuItem(
                route = "production",
                titleTr = "Üretim Tesisleri",
                titleEn = "Production Plants",
                subtitleTr = "$activeBusinessesCount Aktif Tesis",
                subtitleEn = "$activeBusinessesCount Active Plants",
                icon = Icons.Rounded.Factory,
                iconRes = R.drawable.bg_menu_production,
                accentColor = Color(0xFF10B981),
                categoryTr = "Sanayi & Operasyon",
                categoryEn = "Industry & Operations",
                badgeText = if (activeBusinessesCount > 0) "$activeBusinessesCount Tesis" else null
            ),
            QuickMenuItem(
                route = "inventory",
                titleTr = "Depo & Lojistik",
                titleEn = "Warehouse & Logistics",
                subtitleTr = "Doluluk: %$invFillPercent",
                subtitleEn = "Capacity: %$invFillPercent",
                icon = Icons.Rounded.Inventory2,
                iconRes = R.drawable.bg_menu_warehouse,
                accentColor = Color(0xFF38BDF8),
                categoryTr = "Sanayi & Operasyon",
                categoryEn = "Industry & Operations",
                badgeText = "%$invFillPercent"
            ),
            QuickMenuItem(
                route = "map",
                titleTr = "81 İl Ticaret Haritası",
                titleEn = "81 Cities Trade Map",
                subtitleTr = "Şehirler & Sevkiyat",
                subtitleEn = "Logistics & Routes",
                icon = Icons.Rounded.Map,
                iconRes = R.drawable.bg_menu_map,
                accentColor = Color(0xFFEC4899),
                categoryTr = "Sanayi & Operasyon",
                categoryEn = "Industry & Operations"
            ),
            QuickMenuItem(
                route = "rd",
                titleTr = "Ar-Ge & İnovasyon",
                titleEn = "R&D & Innovation",
                subtitleTr = "$activeResearchesCount Aktif Araştırma",
                subtitleEn = "$activeResearchesCount Active Researches",
                icon = Icons.Rounded.Science,
                iconRes = R.drawable.ic_tab_rd_tech,
                accentColor = Color(0xFFA855F7),
                categoryTr = "Sanayi & Operasyon",
                categoryEn = "Industry & Operations",
                badgeText = if (activeResearchesCount > 0) "⚡ $activeResearchesCount" else null
            ),

            // FİNANS & TİCARET
            QuickMenuItem(
                route = "market",
                titleTr = "Toptan Pazar",
                titleEn = "Wholesale Market",
                subtitleTr = "Oyuncu P2P Alım/Satım",
                subtitleEn = "P2P Marketplace",
                icon = Icons.Rounded.Storefront,
                iconRes = R.drawable.ic_tab_marketplace,
                accentColor = Color(0xFFF59E0B),
                categoryTr = "Finans & Ticaret",
                categoryEn = "Finance & Trade"
            ),
            QuickMenuItem(
                route = "borsa",
                titleTr = "Emtia Borsası",
                titleEn = "Commodity Exchange",
                subtitleTr = "Anlık Fiyatlar & Arbitraj",
                subtitleEn = "Live Prices & Arbitrage",
                icon = Icons.AutoMirrored.Filled.ShowChart,
                iconRes = R.drawable.bg_borsa_header,
                accentColor = Color(0xFF00E5FF),
                categoryTr = "Finans & Ticaret",
                categoryEn = "Finance & Trade",
                badgeText = "CANLI"
            ),
            QuickMenuItem(
                route = "bank",
                titleTr = "Banka & Kredi",
                titleEn = "Bank & Loans",
                subtitleTr = "Mevduat, Kredi & Tahvil",
                subtitleEn = "Deposits, Loans & Bonds",
                icon = Icons.Rounded.AccountBalance,
                iconRes = R.drawable.smart_skyscraper,
                accentColor = Color(0xFF22C55E),
                categoryTr = "Finans & Ticaret",
                categoryEn = "Finance & Trade"
            ),
            QuickMenuItem(
                route = "weekly_growth",
                titleTr = "Haftalık Büyüme",
                titleEn = "Weekly Growth",
                subtitleTr = "Ciro & Net Gelir Grafiği",
                subtitleEn = "Revenue & Net Income Chart",
                icon = Icons.Rounded.TrendingUp,
                iconRes = R.drawable.smart_grid,
                accentColor = Color(0xFF6366F1),
                categoryTr = "Finans & Ticaret",
                categoryEn = "Finance & Trade"
            ),

            // KURUMSAL & YÖNETİM
            QuickMenuItem(
                route = "hr",
                titleTr = "İnsan Kaynakları",
                titleEn = "Human Resources",
                subtitleTr = "$managersCount Müdür Görevde",
                subtitleEn = "$managersCount Managers Assigned",
                icon = Icons.Rounded.People,
                iconRes = R.drawable.bg_menu_hr,
                accentColor = Color(0xFF8B5CF6),
                categoryTr = "Kurumsal & Yönetim",
                categoryEn = "Corporate & Management"
            ),
            QuickMenuItem(
                route = "megaproject",
                titleTr = "Konsorsiyum & Mega Proje",
                titleEn = "Consortium & Mega Projects",
                subtitleTr = "$megaProjectsCount İhale & Proje",
                subtitleEn = "$megaProjectsCount Projects",
                icon = Icons.Rounded.CorporateFare,
                iconRes = R.drawable.bg_consortium_header,
                accentColor = Color(0xFFF43F5E),
                categoryTr = "Kurumsal & Yönetim",
                categoryEn = "Corporate & Management",
                badgeText = if (megaProjectsCount > 0) "$megaProjectsCount İhale" else null
            ),
            QuickMenuItem(
                route = "statistics",
                titleTr = "Şirket Raporu",
                titleEn = "Company Report",
                subtitleTr = "Mali Tablolar & Bilanço",
                subtitleEn = "Financial Balance & Audits",
                icon = Icons.Rounded.Assessment,
                iconRes = R.drawable.bg_company_summary,
                accentColor = Color(0xFF0EA5E9),
                categoryTr = "Kurumsal & Yönetim",
                categoryEn = "Corporate & Management"
            ),
            QuickMenuItem(
                route = "social",
                titleTr = "Liderlik Sıralaması",
                titleEn = "Leaderboard & Ranks",
                subtitleTr = "Anadolu Zenginler Sıralaması",
                subtitleEn = "Top Tycoon Rankings",
                icon = Icons.Rounded.EmojiEvents,
                iconRes = R.drawable.ic_tab_leaderboard,
                accentColor = Color(0xFFFFD54F),
                categoryTr = "Kurumsal & Yönetim",
                categoryEn = "Corporate & Management",
                badgeText = "TOP 100"
            )
        )
    }

    // Is the current route one of the secondary routes accessed from All Menus?
    val isSecondaryRouteActive = currentRoute in listOf("rd", "map", "inventory", "bank", "megaproject", "hr", "statistics", "social", "weekly_growth")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0A1120).copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    ThemeNeonCyan.copy(alpha = 0.55f),
                    Color(0xFF10B981).copy(alpha = 0.45f),
                    Color(0xFFA855F7).copy(alpha = 0.45f),
                    ThemeGold.copy(alpha = 0.55f)
                )
            )
        ),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = when (tab.route) {
                    "menu" -> showAllMenusSheet || isSecondaryRouteActive
                    "home" -> currentRoute == "home" || currentRoute.isBlank() || currentRoute == "dashboard"
                    else -> currentRoute == tab.route
                }

                val tabLock = checkRouteLock(tab.route)
                val isLocked = (tabLock != null)

                val animatedColor by animateColorAsState(
                    targetValue = if (isLocked) Color(0xFF64748B).copy(alpha = 0.6f) else if (isSelected) tab.accentColor else Color(0xFF64748B),
                    animationSpec = tween(250),
                    label = "tab_color"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (tab.route == "menu") {
                                showAllMenusSheet = true
                            } else if (tabLock != null) {
                                lockedDialogInfo = tabLock
                            } else {
                                onNavigate(tab.route)
                            }
                        }
                        .padding(vertical = 3.dp, horizontal = 1.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(tab.accentColor.copy(alpha = 0.22f))
                            )
                        }

                        if (tab.iconRes != null) {
                            Image(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = tr(tab.titleTr, tab.titleEn),
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit,
                                alpha = if (isLocked) 0.40f else if (isSelected) 1f else 0.70f
                            )
                        } else {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tr(tab.titleTr, tab.titleEn),
                                tint = animatedColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // Notification / Indicator dot for Menu or Active statuses
                        if (tab.route == "menu" && (activeResearchesCount > 0 || megaProjectsCount > 0)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E))
                            )
                        }

                        // Lock Indicator Badge on Tab
                        if (isLocked) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A),
                                border = BorderStroke(0.6.dp, Color(0xFFEF4444)),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    CurrencyText(
                        text = tr(tab.titleTr, tab.titleEn),
                        fontSize = 8.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        fontFamily = RobotoMonoFontFamily,
                        color = animatedColor,
                        maxLines = 1
                    )

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(tab.accentColor)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.5.dp))
                    }
                }
            }
        }
    }

    // ========================================================================
    // MODAL BOTTOM SHEET: HIZLI ERİŞİM & TÜM MENÜLER HUB
    // ========================================================================
    if (showAllMenusSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllMenusSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF070F1E),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header: [ 🧭 TÜM MENÜLER & HIZLI ERİŞİM ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFA855F7).copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.7f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            CurrencyText(
                                text = tr("HIZLI ERİŞİM & TÜM MENÜLER", "QUICK ACCESS & ALL MENUS"),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 13.5.sp
                            )
                            CurrencyText(
                                text = tr("Tüm sanayi, ticaret ve finans modüllerine doğrudan ulaşın", "Direct access to all industry, trade and finance modules"),
                                color = Color(0xFF94A3B8),
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAllMenusSheet = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // --- CATEGORIZED GRID OF ALL 12 MENUS ---
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allMenuItems) { item ->
                        val isCurrent = (currentRoute == item.route)
                        val itemLock = checkRouteLock(item.route)
                        val isLocked = (itemLock != null)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) Color(0xFF11233E) else if (isLocked) Color(0xFF070D18) else Color(0xFF0A1424),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isLocked) Color(0xFFEF4444).copy(alpha = 0.35f) else if (isCurrent) item.accentColor else Color(0xFF1E2F48)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (itemLock != null) {
                                        lockedDialogInfo = itemLock
                                    } else {
                                        showAllMenusSheet = false
                                        onNavigate(item.route)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isLocked) Color(0xFF1E293B).copy(alpha = 0.6f) else item.accentColor.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, if (isLocked) Color(0xFF334155) else item.accentColor.copy(alpha = 0.6f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (item.iconRes != null) {
                                            Image(
                                                painter = painterResource(id = item.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                contentScale = ContentScale.Fit,
                                                alpha = if (isLocked) 0.35f else 1f
                                            )
                                        } else {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = if (isLocked) Color.Gray else item.accentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        if (isLocked) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = "Locked",
                                                tint = Color(0xFFFCA5A5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CurrencyText(
                                            text = tr(item.titleTr, item.titleEn),
                                            color = if (isLocked) Color(0xFF94A3B8) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(1.dp))

                                    CurrencyText(
                                        text = if (isLocked) tr("🔒 Seviye ${itemLock.minRequiredLevel}", "🔒 Level ${itemLock.minRequiredLevel}") else tr(item.subtitleTr, item.subtitleEn),
                                        color = if (isLocked) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val displayBadge = if (isLocked) "🔒" else item.badgeText
                                if (displayBadge != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isLocked) Color(0xFF7F1D1D).copy(alpha = 0.4f) else item.accentColor.copy(alpha = 0.20f),
                                        border = BorderStroke(0.6.dp, if (isLocked) Color(0xFFEF4444).copy(alpha = 0.6f) else item.accentColor)
                                    ) {
                                        CurrencyText(
                                            text = displayBadge,
                                            color = if (isLocked) Color(0xFFFCA5A5) else item.accentColor,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = RobotoMonoFontFamily,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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

    // Locked Feature Explanatory Dialog
    FeatureLockedDialog(
        lockInfo = lockedDialogInfo,
        onDismiss = { lockedDialogInfo = null },
        onNavigateToRd = {
            lockedDialogInfo = null
            showAllMenusSheet = false
            onNavigate("rd")
        },
        onNavigateToProduction = {
            lockedDialogInfo = null
            showAllMenusSheet = false
            onNavigate("production")
        }
    )
}
