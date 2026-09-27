package com.example.ui.components

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto

import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.cities
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.LocalAppThemeOption
import com.example.viewmodel.GameViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameTopBar(
    viewModel: GameViewModel,
    currentRoute: String = "home",
    onNavigateToHome: () -> Unit = {},
    onNavigateToBank: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToWeeklyGrowth: () -> Unit = {}
) {
    val player by viewModel.player.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val notificationHistory by SmartNotificationManager.history.collectAsStateWithLifecycle()
    val isOnlineRegistered by viewModel.isOnlineRegistered.collectAsStateWithLifecycle()
    val onlineEmail by viewModel.onlineEmail.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val nextDailyRewardRemainingMs by viewModel.nextDailyRewardRemainingMs.collectAsStateWithLifecycle()
    val questState by viewModel.dailyQuestState.collectAsStateWithLifecycle()
    val hasClaimableQuests = questState.quests.any { it.isCompleted && !it.isClaimed }
    val haptic = LocalHapticFeedback.current
    val theme = LocalAppThemeOption.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var showGemStoreDialog by remember { mutableStateOf(false) }
    var showFinancialSummaryCard by remember { mutableStateOf(false) }

    if (showGemStoreDialog) {
        GemStoreDialog(
            viewModel = viewModel,
            onDismiss = { showGemStoreDialog = false }
        )
    }

    if (showRewardedAdDialog) {
        RewardedAdDialog(
            rewardGemsAmount = 5,
            onRewardEarned = { gems ->
                viewModel.claimRewardedAdGems(gems)
            },
            onDismiss = { showRewardedAdDialog = false }
        )
    }

    val p = player ?: com.example.data.PlayerEntity(
        id = "local_player",
        name = "Tüccar",
        level = 1,
        currentCity = "canakkale",
        money = 100_000L,
        inventoryCapacity = 5000
    )
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isExpertMode by viewModel.isExpertMode.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val unreadCount = notificationHistory.count { !it.isRead }
    val cityName = cities.find { it.id == p.currentCity }?.name ?: if (p.currentCity.isBlank() || p.currentCity == "-") "Canakkale" else p.currentCity.replaceFirstChar { it.uppercase() }
    val usedCapacity = remember(inventory) { kotlin.math.max(0, inventory.sumOf { it.quantity }) }
    val maxCapacity = p.inventoryCapacity
    val isRewardReady = nextDailyRewardRemainingMs <= 0L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = theme.surfaceColor,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // TIER 1: PROFILE & CITY CHIP (LEFT) + ARCADE CONTROLS (RIGHT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: PROFILE & CURRENT LOCATION
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showProfileDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeNeonCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(17.dp))
                            }
                        }
                        Column {
                            CurrencyText(
                                text = "LV.${p.level} ${p.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                CurrencyText(
                                    text = cityName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // RIGHT: ARCADE ACTION CONTROLS (BANK, GEMS, QUESTS, GIFTS, NOTIFICATIONS)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BANK BUTTON
                    TopBarIconButton(
                        icon = Icons.Rounded.AccountBalance,
                        contentDesc = tr("Banka ve Finans", "Bank & Finance"),
                        iconTint = if (showFinancialSummaryCard) ThemeNeonCyan else Color(0xFF94A3B8),
                        backgroundColor = if (showFinancialSummaryCard) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFinancialSummaryCard = !showFinancialSummaryCard
                        }
                    )

                    // GEMS STORE BUTTON
                    TopBarIconButtonWithBadge(
                        icon = Icons.Default.Diamond,
                        contentDesc = tr("Elmas", "Diamond"),
                        iconTint = ThemeGold,
                        badgeText = if (p.gems > 0) (if (p.gems > 999) "999+" else "${p.gems}") else null,
                        badgeColor = ThemeGold,
                        badgeTextColor = Color(0xFF0F172A),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showGemStoreDialog = true
                        }
                    )

                    // DAILY QUESTS & SEASON PASS
                    TopBarIconButtonWithBadge(
                        icon = Icons.Default.Star,
                        contentDesc = tr("Görevler", "Quests"),
                        iconTint = if (hasClaimableQuests) ThemeGold else Color(0xFF94A3B8),
                        badgeText = if (hasClaimableQuests) "!" else null,
                        showDot = hasClaimableQuests,
                        badgeColor = ThemeGold,
                        badgeTextColor = Color(0xFF0F172A),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openDailyQuestsDialog()
                        }
                    )

                    // REWARD GIFT
                    TopBarIconButtonWithBadge(
                        icon = Icons.Rounded.CardGiftcard,
                        contentDesc = tr("Hediye", "Reward"),
                        iconTint = if (isRewardReady) ThemePositive else Color(0xFF94A3B8),
                        badgeText = null,
                        showDot = isRewardReady,
                        badgeColor = ThemePositive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isRewardReady) viewModel.checkDailyLoginBonus(context)
                            else showRewardedAdDialog = true
                        }
                    )

                    // NOTIFICATIONS
                    TopBarIconButtonWithBadge(
                        icon = Icons.Default.Notifications,
                        contentDesc = tr("Bildirimler", "Notifications"),
                        iconTint = if (unreadCount > 0) ThemeNeonCyan else Color(0xFF94A3B8),
                        badgeText = if (unreadCount > 0) (if (unreadCount > 9) "9+" else "$unreadCount") else null,
                        badgeColor = ThemeNegative,
                        badgeTextColor = Color.White,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showNotificationDialog = true
                        }
                    )
                }
            }

            // TIER 2: 4-POD FINANCIAL HUD RIBBON (KASA, BANKA, ELMAS, DEPO)
            val totalBankBalance = p.depositBalance + p.lockedDepositBalance
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.4f),
                            Color(0xFF10B981).copy(alpha = 0.4f),
                            ThemeGold.copy(alpha = 0.4f),
                            Color(0xFFA855F7).copy(alpha = 0.4f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. KASA / NAKİT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFinancialSummaryCard = !showFinancialSummaryCard
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = tr("KASA", "CASH"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        CurrencyText(
                            text = formatCredit(p.money),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .width(1.dp)
                            .background(Color(0xFF334155))
                    )

                    // 2. BANKA MEVDUAT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFinancialSummaryCard = !showFinancialSummaryCard
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = tr("BANKA", "BANK"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        CurrencyText(
                            text = formatCredit(totalBankBalance),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color(0xFF34D399),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .width(1.dp)
                            .background(Color(0xFF334155))
                    )

                    // 3. ELMAS
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showGemStoreDialog = true
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(ThemeGold)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = tr("ELMAS", "GEMS"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        CurrencyText(
                            text = "${p.gems} 💎",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .width(1.dp)
                            .background(Color(0xFF334155))
                    )

                    // 4. DEPO KAPASİTE
                    val fillRatio = if (maxCapacity > 0) (usedCapacity.toFloat() / maxCapacity.toFloat()).coerceIn(0f, 1f) else 0f
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToInventory()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA855F7))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(
                                text = tr("DEPO", "STORAGE"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.3.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        CurrencyText(
                            text = "$usedCapacity/${maxCapacity}T",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFA855F7),
                                                Color(0xFFC084FC)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // FINANCIAL SUMMARY CARD (Progressive Disclosure)
            AnimatedVisibility(
                visible = showFinancialSummaryCard,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = theme.surfaceColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                                CurrencyText(tr("Finansal Özet & Banka Detayları", "Financial Summary & Bank Details"), color = theme.textPrimaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(
                                onClick = { showFinancialSummaryCard = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = theme.textSecondaryColor, modifier = Modifier.size(18.dp))
                            }
                        }

                        HorizontalDivider(color = theme.borderColor, thickness = 1.dp)

                        // Oyun Kredisi Banka Hesabı ve Nakit Para Özeti
                        val totalBank = p.depositBalance + p.lockedDepositBalance
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF10B981), ThemeGold)))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 1. NAKİT & ELMAS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Column {
                                            CurrencyText(tr("💵 Nakit Para (Kasa)", "💵 Cash on Hand (Wallet)"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                            CurrencyText(tr("Anlık Harcanabilir Bakiye", "Spendable Liquid Balance"), fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                                        }
                                    }
                                    CurrencyText(
                                        text = formatCredit(p.money),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8),
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)

                                // 2. BANKA MEVDUATLARI
                                val rate = gameState?.centralBankDepositRate ?: 0.15f
                                val dailyYield = com.example.data.BankDailySettlementManager.calculateDailyDepositYield(p.depositBalance, rate)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        CurrencyText(tr("🏦 Vadeli Mevduat", "🏦 Term Deposit"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        CurrencyText(formatCredit(p.depositBalance), fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399), fontFamily = RobotoMonoFontFamily)
                                    }
                                    if (p.lockedDepositBalance > 0L) {
                                        Column {
                                            CurrencyText(tr("🔒 Kilitli Vadeli", "🔒 Locked Deposit"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            CurrencyText(formatCredit(p.lockedDepositBalance), fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981), fontFamily = RobotoMonoFontFamily)
                                        }
                                    } else {
                                        Column {
                                            CurrencyText(tr("📈 Günlük Faiz", "📈 Daily Interest"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            CurrencyText(
                                                if (dailyYield > 0L) "+${formatCredit(dailyYield)}" else "₳ 0",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF10B981),
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        CurrencyText(tr("💎 Elmas Rezervi", "💎 Gem Reserve"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        CurrencyText("${p.gems} 💎", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)

                                // 3. BORÇ & TOPLAM BANKA
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        CurrencyText(tr("🏦 Toplam Banka Varlığı", "🏦 Total Bank Assets"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        CurrencyText(formatCredit(totalBank), fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF6EE7B7), fontFamily = RobotoMonoFontFamily)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        CurrencyText(tr("💳 Kredi Borcu", "💳 Loan Debt"), fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        CurrencyText(
                                            if (p.loanAmount > 0) formatCredit(p.loanAmount) else tr("Borç Yok", "No Debt"),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (p.loanAmount > 0) ThemeNegative else Color(0xFF94A3B8),
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FinancialSummaryItem(
                                icon = Icons.Rounded.MonetizationOn,
                                label = tr("Harcanabilir Nakit", "Liquid Cash"),
                                value = formatCredit(p.money),
                                accentColor = ThemeGold,
                                modifier = Modifier.weight(1f)
                            )
                            FinancialSummaryItem(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                label = tr("Toplam Net Varlık", "Total Net Worth"),
                                value = formatCredit(netWorth),
                                accentColor = ThemeNeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val netDaily = p.dailyIncome - p.dailyExpense
                            FinancialSummaryItem(
                                icon = if (netDaily >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                label = tr("Günlük Net Akış", "Daily Net Flow"),
                                value = "${if (netDaily >= 0) "+" else ""}${formatCredit(netDaily)} / " + tr("gün", "day"),
                                accentColor = if (netDaily >= 0) ThemePositive else ThemeNegative,
                                modifier = Modifier.weight(1f)
                            )
                            val fillPercentage = if (maxCapacity > 0) (usedCapacity * 100 / maxCapacity) else 0
                            FinancialSummaryItem(
                                icon = Icons.Rounded.Inventory2,
                                label = tr("Depo Kapasitesi", "Warehouse Capacity"),
                                value = "${usedCapacity} / ${maxCapacity} " + tr("Ton", "Tons") + " (%${fillPercentage})",
                                accentColor = Color(0xFFFFB74D),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Shortcut Button to Bank Screen
                        OutlinedButton(
                            onClick = {
                                showFinancialSummaryCard = false
                                onNavigateToBank()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, theme.primaryColor)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CurrencyText(tr("Banka & Finans Merkezine Git", "Go to Bank & Finance Center"), color = theme.primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG POPUPS
    if (showProfileDialog) {
        TraderProfileDialog(
            player = p,
            onNavigateToBank = {
                showProfileDialog = false
                onNavigateToBank()
            },
            onDismiss = { showProfileDialog = false },
            isOnlineRegistered = isOnlineRegistered,
            onlineEmail = onlineEmail,
            onLogin = { email, pass -> viewModel.loginOnline(email, pass) },
            onRegister = { email, pass -> viewModel.registerOnline(email, pass, viewModel.player.value?.name ?: "Tüccar") },
            onLogout = { viewModel.logoutOnline() },
            onGoogleSignIn = { idToken, onResult -> viewModel.signInWithGoogle(idToken, onResult) },
            onGoogleSignInAnon = { onResult -> viewModel.signInAnonymously(onResult) },
            onUpdateTraderName = { newName -> viewModel.updateTraderNameWithDiamond(newName) },
            selectedTheme = selectedTheme,
            onSelectTheme = { themeId -> viewModel.setSelectedTheme(themeId) },
            selectedLanguage = selectedLanguage,
            onSelectLanguage = { langCode -> viewModel.setSelectedLanguage(langCode) },
            isExpertMode = isExpertMode,
            onSelectUiMode = { isExpert -> viewModel.setExpertMode(isExpert) },
            onNavigateToGemStore = {
                showProfileDialog = false
                showGemStoreDialog = true
            },
            onForceSyncCloud = {
                viewModel.forceSyncCloudSaveToSupabase(context)
            },
            onForceRestoreCloud = {
                viewModel.forceRestoreFromCloud(context)
            },
            viewModel = viewModel
        )
    }

    if (showNotificationDialog) {
        NotificationHistoryDialog(
            onDismiss = { showNotificationDialog = false }
        )
    }
}

@Composable
private fun FinancialSummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppThemeOption.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = theme.surfaceVariantColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
            }
            Column {
                CurrencyText(text = label, color = theme.textSecondaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                CurrencyText(text = value, color = theme.textPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDesc: String,
    iconTint: Color,
    backgroundColor: Color? = null,
    onClick: () -> Unit
) {
    val theme = LocalAppThemeOption.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor ?: theme.surfaceVariantColor,
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TopBarProfileButton(
    player: com.example.data.PlayerEntity,
    onClick: () -> Unit
) {
    val theme = LocalAppThemeOption.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = theme.surfaceVariantColor,
        border = BorderStroke(
            1.dp,
            if (player.isVip) Brush.horizontalGradient(listOf(ThemeGold, Color(0xFFFFF176)))
            else Brush.horizontalGradient(listOf(theme.primaryColor.copy(alpha = 0.8f), theme.primaryColor.copy(alpha = 0.4f)))
        ),
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurrencyText(
                text = "💼",
                fontSize = 16.sp
            )
            // Online Status Dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ThemePositive)
                    .border(1.dp, theme.surfaceVariantColor, CircleShape)
            )
        }
    }
}

@Composable
private fun TopBarIconButtonWithBadge(
    icon: ImageVector,
    contentDesc: String,
    iconTint: Color,
    badgeText: String?,
    showDot: Boolean = false,
    badgeColor: Color,
    badgeTextColor: Color = Color.White,
    onClick: () -> Unit
) {
    val theme = LocalAppThemeOption.current
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.surfaceVariantColor,
            border = BorderStroke(0.8.dp, Color(0xFF334155)),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDesc,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (badgeText != null) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                border = BorderStroke(1.dp, Color(0xFF0F172A)),
                modifier = Modifier
                    .height(14.dp)
                    .defaultMinSize(minWidth = 14.dp)
                    .offset(x = 4.dp, y = (-3).dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 3.dp)) {
                    CurrencyText(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        } else if (showDot) {
            Surface(
                shape = CircleShape,
                color = badgeColor,
                border = BorderStroke(1.dp, Color(0xFF0F172A)),
                modifier = Modifier
                    .size(9.dp)
                    .offset(x = 2.dp, y = (-2).dp)
            ) {}
        }
    }
}


