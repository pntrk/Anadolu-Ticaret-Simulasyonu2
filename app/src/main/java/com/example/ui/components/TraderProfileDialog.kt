package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.lazy.LazyRow
import com.example.data.MuseumHeritageManager
import com.example.data.ArtifactBuffRegistry
import com.example.data.ArtifactBuffType
import com.example.data.PlayerEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.billing.findActivity
import com.example.data.cities
import com.example.ui.theme.*
import com.example.viewmodel.calculateCompanyValuation
import com.example.viewmodel.loginOnline
import com.example.viewmodel.registerOnline

enum class ProfileTab(val titleTr: String, val titleEn: String, val icon: ImageVector) {
    OVERVIEW("Künye", "Overview", Icons.Rounded.Person),
    MUSEUM("Müze", "Museum", Icons.Rounded.Museum),
    APPEARANCE("Tema & Dil", "Theme & Lang", Icons.Rounded.Palette),
    SETTINGS("Ayarlar", "Settings", Icons.Rounded.Settings)
}

@Composable
fun TraderProfileDialog(
    player: PlayerEntity,
    onNavigateToBank: () -> Unit,
    onDismiss: () -> Unit,
    isOnlineRegistered: Boolean = false,
    onlineEmail: String = "",
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: (String, String) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {},
    onGoogleSignIn: ((String, (Boolean, String?) -> Unit) -> Unit)? = null,
    onGoogleSignInAnon: (((Boolean, String?) -> Unit) -> Unit)? = null,
    onUpdateCompanyName: (String) -> Unit = {},
    onUpdateTraderName: (String) -> Boolean = { false },
    selectedTheme: String = "cyber_blue",
    onSelectTheme: (String) -> Unit = {},
    selectedLanguage: String = "tr",
    onSelectLanguage: (String) -> Unit = {},
    isExpertMode: Boolean = false,
    onSelectUiMode: (Boolean) -> Unit = {},
    onNavigateToGemStore: () -> Unit = {},
    onNavigateToWeeklyGrowth: () -> Unit = {},
    onForceSyncCloud: () -> Unit = {},
    onForceRestoreCloud: () -> Unit = {},
    viewModel: com.example.viewmodel.GameViewModel? = null
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showEmailAuthDialog by remember { mutableStateOf(false) }
    val isEn = isEnglishLanguage()

    val netWorth = if (viewModel != null) {
        viewModel.calculateCompanyValuation()
    } else {
        (player.money + player.depositBalance + player.lockedDepositBalance - player.loanAmount).coerceAtLeast(0L)
    }
    var selectedTab by remember { mutableStateOf(ProfileTab.OVERVIEW) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var companyNameInput by remember { mutableStateOf(player.name) }

    val currentCityObj = cities.find { it.id == player.currentCity }
    val cityName = currentCityObj?.name ?: if (player.currentCity.isBlank() || player.currentCity == "-") "Çanakkale" else player.currentCity.replaceFirstChar { it.uppercase() }

    // XP Progress & Level Calculations
    val levelProgress = remember(player.xp) {
        com.example.data.XpLevelEngine.getProgress(player.xp.toLong())
    }
    val effectiveLevel = remember(player.level, levelProgress.level) {
        maxOf(player.level, levelProgress.level)
    }
    val xpProgress = levelProgress.progressFraction

    // Level Title Calculator
    val traderTitle = remember(effectiveLevel) {
        when {
            effectiveLevel >= 50 -> if (isEn) "Empire Industrialist" else "Holding İmparatoru"
            effectiveLevel >= 30 -> if (isEn) "Magnate Merchant" else "Borsa & Ticaret Baronu"
            effectiveLevel >= 20 -> if (isEn) "Senior Industrialist" else "Kıdemli Sanayici"
            effectiveLevel >= 10 -> if (isEn) "Master Trader" else "Usta Tacir"
            effectiveLevel >= 5 -> if (isEn) "Journeyman Merchant" else "Kalfalık Taciri"
            else -> if (isEn) "Apprentice Trader" else "Çırak Tüccar"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEB060A14)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.90f)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        if (player.isVip) listOf(ThemeGold, Color(0xFFFFF0A0), ThemeGold.copy(alpha = 0.5f))
                        else listOf(ThemeNeonCyan, Color(0xFF1E293B), ThemeNeonCyan.copy(alpha = 0.3f))
                    )
                ),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // TOP BAR: Dialog Title + Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (player.isVip) ThemeGold.copy(alpha = 0.2f) else ThemeNeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (player.isVip) Icons.Rounded.WorkspacePremium else Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = if (player.isVip) ThemeGold else ThemeNeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = tr("TÜCCAR PROFİLİ", "TRADER PROFILE"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (player.isVip) ThemeGold else Color.White,
                                    fontFamily = RobotoMonoFontFamily,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = tr("Anadolu Ticaret Ağı Künyesi", "Anatolian Trade Network Dossier"),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // HERO PROFILE CARD: Avatar + Level Indicator + Name + VIP Badge + Online Sync
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF131F37),
                        border = BorderStroke(1.dp, if (player.isVip) ThemeGold.copy(alpha = 0.4f) else ThemeNeonCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Circular Avatar with Progress Ring
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { xpProgress },
                                        modifier = Modifier.fillMaxSize(),
                                        color = if (player.isVip) ThemeGold else ThemeNeonCyan,
                                        trackColor = Color(0xFF1E293B),
                                        strokeWidth = 3.5.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                                                )
                                            )
                                            .border(1.dp, if (player.isVip) ThemeGold else ThemeNeonCyan.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (player.isVip) "👑" else "💼",
                                            fontSize = 22.sp
                                        )
                                    }

                                    // Online Status Dot
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnlineRegistered) ThemePositive else Color.Gray)
                                            .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                                    )
                                }

                                // Name, Level & Title
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = player.name.uppercase(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontFamily = RobotoMonoFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (player.isVip) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ThemeGold,
                                                shadowElevation = 2.dp
                                            ) {
                                                Text(
                                                    text = "VIP",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = ThemeNeonCyan.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "LVL $effectiveLevel",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemeNeonCyan,
                                                fontFamily = RobotoMonoFontFamily,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        Text(
                                            text = traderTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.LightGray,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    // XP Linear Progress Bar
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { xpProgress },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (player.isVip) ThemeGold else ThemeNeonCyan,
                                            trackColor = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val currentXpCompact = when {
                                            levelProgress.currentLevelXp >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", levelProgress.currentLevelXp / 1_000_000.0)
                                            levelProgress.currentLevelXp >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", levelProgress.currentLevelXp / 1_000.0)
                                            else -> levelProgress.currentLevelXp.toString()
                                        }
                                        val targetXpCompact = when {
                                            levelProgress.targetLevelXp >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", levelProgress.targetLevelXp / 1_000_000.0)
                                            levelProgress.targetLevelXp >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", levelProgress.targetLevelXp / 1_000.0)
                                            else -> levelProgress.targetLevelXp.toString()
                                        }
                                        val percent = (xpProgress * 100).toInt().coerceIn(0, 100)
                                        Text(
                                            text = "$currentXpCompact / $targetXpCompact XP (%$percent)",
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }

                                // Quick Rename Button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        companyNameInput = player.name
                                        showRenameDialog = true
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, ThemeGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "İsim Değiştir",
                                        tint = ThemeGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Cloud Account / Sign In Status Strip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF091224),
                                border = BorderStroke(0.5.dp, if (isOnlineRegistered) ThemePositive.copy(alpha = 0.3f) else Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isOnlineRegistered) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = ThemePositive,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (onlineEmail.isNotBlank()) onlineEmail else tr("Çevrimiçi Hesap", "Online Account"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onLogout()
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text(tr("Çıkış", "Logout"), fontSize = 10.sp, color = ThemeNegative, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = tr("Yerel Misafir Oturumu", "Local Guest Session"),
                                                fontSize = 10.5.sp,
                                                color = Color.LightGray
                                            )
                                        }

                                        if (isGoogleLoading) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color(0xFF4285F4)
                                                )
                                                Text(
                                                    text = tr("Bağlanıyor...", "Connecting..."),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ThemeNeonCyan
                                                )
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                GoogleSignInButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        if (viewModel != null) {
                                                            com.example.utils.GoogleAuthHelper.launchGoogleSignIn(
                                                                context = context,
                                                                scope = scope,
                                                                viewModel = viewModel,
                                                                onStart = { isGoogleLoading = true },
                                                                onComplete = { success, msg ->
                                                                    isGoogleLoading = false
                                                                    val successMsg = if (isEn) "Connected with Google Account!" else "Google Hesabı ile Bağlanıldı!"
                                                                    val failMsg = msg ?: (if (isEn) "Sign-in failed" else "Giriş yapılamadı")
                                                                    Toast.makeText(context, if (success) successMsg else failMsg, Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        } else {
                                                            onGoogleSignInAnon?.invoke { _, _ -> }
                                                        }
                                                    },
                                                    isLoading = false,
                                                    isCompact = true,
                                                    text = tr("Google Girişi", "Sign in with Google")
                                                )

                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showEmailAuthDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF1E293B))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Email,
                                                            contentDescription = "Email Giriş",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = tr("E-posta", "Email"),
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
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

                    // SEGMENTED TAB BAR (Dinamik ve Akıcı Gezinme)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF091122),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            ProfileTab.values().forEach { tab ->
                                val isSelected = selectedTab == tab
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, ThemeNeonCyan) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedTab = tab
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) ThemeNeonCyan else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isEn) tab.titleEn else tab.titleTr,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ACTIVE TAB CONTENT CONTAINER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val handleGoogleSignIn: () -> Unit = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (viewModel != null) {
                                com.example.utils.GoogleAuthHelper.launchGoogleSignIn(
                                    context = context,
                                    scope = scope,
                                    viewModel = viewModel,
                                    onStart = { isGoogleLoading = true },
                                    onComplete = { success, msg ->
                                        isGoogleLoading = false
                                        val successMsg = if (isEn) "Connected with Google Account!" else "Google Hesabı ile Bağlanıldı!"
                                        val failMsg = msg ?: (if (isEn) "Sign-in failed" else "Giriş yapılamadı")
                                        Toast.makeText(context, if (success) successMsg else failMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                onGoogleSignInAnon?.invoke { _, _ -> }
                            }
                        }

                        when (selectedTab) {
                            ProfileTab.OVERVIEW -> {
                                ProfileOverviewTab(
                                    player = player,
                                    cityName = cityName,
                                    netWorth = netWorth,
                                    onNavigateToBank = onNavigateToBank,
                                    onNavigateToGemStore = onNavigateToGemStore
                                )
                            }
                            ProfileTab.MUSEUM -> {
                                ProfileMuseumTab(context = context, viewModel = viewModel)
                            }
                            ProfileTab.APPEARANCE -> {
                                ProfileAppearanceTab(
                                    selectedTheme = selectedTheme,
                                    onSelectTheme = onSelectTheme,
                                    selectedLanguage = selectedLanguage,
                                    onSelectLanguage = onSelectLanguage
                                )
                            }
                            ProfileTab.SETTINGS -> {
                                ProfileSettingsTab(
                                    isOnlineRegistered = isOnlineRegistered,
                                    onlineEmail = onlineEmail,
                                    onForceSyncCloud = onForceSyncCloud,
                                    onForceRestoreCloud = onForceRestoreCloud,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // BOTTOM ACTION BUTTON: Gem Store Quick Link
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                            onNavigateToGemStore()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A),
                            contentColor = ThemeNeonCyan
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemeGold)
                            Text(
                                text = tr("💎 ELMAS MAĞAZASI & VIP AVANTAJLARI", "💎 GEM STORE & VIP PRIVILEGES"),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // RENAME TRADER DIALOG
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                    Text(
                        text = tr("Tüccar İsmini Güncelle", "Update Trader Name"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = tr(
                            "Yeni tüccar veya şirket ünvanınızı belirleyin. Bu işlem 10 💎 Elmas gerektirir.",
                            "Choose your new company/trader name. This action costs 10 💎 Gems."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = companyNameInput,
                        onValueChange = { companyNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeGold,
                            unfocusedBorderColor = ThemeBorder
                        )
                    )
                    Text(
                        text = "${tr("Mevcut Elmas:", "Current Gems:")} ${player.gems} 💎",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (player.gems >= 10) ThemePositive else ThemeNegative,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onUpdateTraderName(companyNameInput)
                        if (success) {
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                ) {
                    Text(tr("DEĞİŞTİR (10 💎)", "CHANGE (10 💎)"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(tr("İptal", "Cancel"), color = Color.Gray)
                }
            },
            containerColor = Color(0xFF101726),
            shape = RoundedCornerShape(12.dp)
        )
    }

    EmailAuthDialogWrapper(
        show = showEmailAuthDialog,
        onDismiss = { showEmailAuthDialog = false },
        viewModel = viewModel,
        playerName = player.name
    )

}

// ----------------------------------------------------
// TAB 1: OVERVIEW & FINANCIAL KPI METRICS
// ----------------------------------------------------
@Composable
private fun ProfileOverviewTab(
    player: PlayerEntity,
    cityName: String,
    netWorth: Long,
    onNavigateToBank: () -> Unit,
    onNavigateToGemStore: () -> Unit
) {
    val isEn = isEnglishLanguage()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 2x2 Metric Dashboard Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = tr("ŞİRKET DEĞERİ", "NET WORTH"),
                    value = "₳${formatMoney(netWorth)}",
                    icon = Icons.Rounded.AccountBalance,
                    color = ThemeGold,
                    subtitle = tr("Toplam Net Varlık", "Total Net Assets")
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = tr("KASA NAKİT", "LIQUID CASH"),
                    value = "₳${formatMoney(player.money)}",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    color = ThemePositive,
                    subtitle = tr("Likit Ticaret Sermayesi", "Liquid Trading Capital")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = tr("ELMAS VARLIĞI", "GEM RESERVES"),
                    value = "${player.gems} 💎",
                    icon = Icons.Rounded.Diamond,
                    color = ThemeNeonCyan,
                    subtitle = tr("Özel Ticaret Kaynağı", "Premium Trade Asset")
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = tr("MERKEZ OFİS", "HEADQUARTERS"),
                    value = cityName,
                    icon = Icons.Rounded.LocationOn,
                    color = Color(0xFF60A5FA),
                    subtitle = tr("Operasyonel Ana Üs", "Operational HQ Base")
                )
            }
        }

        // Additional Financial Details Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0C1426),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr("📊 FİNANSAL AYRINTILAR", "📊 FINANCIAL BREAKDOWN"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    FinancialRow(
                        label = tr("Mevduat Hesabı (Banka):", "Deposit Account (Bank):"),
                        value = "₳${formatMoney(player.depositBalance)}",
                        color = ThemePositive
                    )

                    FinancialRow(
                        label = tr("Aktif Banka Kredisi Borcu:", "Active Bank Loan:"),
                        value = if (player.loanAmount > 0) "-₳${formatMoney(player.loanAmount)}" else "₳0",
                        color = if (player.loanAmount > 0) ThemeNegative else Color.LightGray
                    )

                    FinancialRow(
                        label = tr("Envanter Taşıma Kapasitesi:", "Inventory Cargo Capacity:"),
                        value = "${player.inventoryCapacity} ${tr("Birim", "Units")}",
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F1B2E),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontFamily = RobotoMonoFontFamily
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color,
                fontFamily = RobotoMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinancialRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
        )
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = RobotoMonoFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ----------------------------------------------------
// TAB 2: AHİLİK MİRASI MÜZESİ & PRESTİJ
// ----------------------------------------------------
@Composable
private fun ProfileMuseumTab(
    context: android.content.Context,
    viewModel: com.example.viewmodel.GameViewModel? = null
) {
    val ownedArtifacts = remember { MuseumHeritageManager.getOwnedArtifacts(context) }
    val museumPrestige = remember { MuseumHeritageManager.getTotalMuseumPrestige(context) }
    val hourlyVisitorIncome = remember { MuseumHeritageManager.getTotalHourlyVisitorIncome(context) }
    val profileVisitCount = remember { MuseumHeritageManager.getTotalProfileInspections(context) }
    val profileBonusRevenue = remember { MuseumHeritageManager.getUnclaimedProfileVisitRevenue(context) }
    val isEn = isEnglishLanguage()

    val activeBuffs = if (viewModel != null) {
        val vmBuffs by viewModel.activeArtifactBuffs.collectAsStateWithLifecycle()
        vmBuffs
    } else {
        remember(ownedArtifacts) {
            ArtifactBuffRegistry.getActiveBuffs(ownedArtifacts.map { it.id })
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Aktif Şirket Güçleri (Artifact Buffs)
        if (activeBuffs.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1E33),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "✨ " + tr("AKTİF ŞİRKET GÜÇLERİ", "ACTIVE COMPANY BUFFS"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color(0xFFFBBF24),
                            letterSpacing = 0.5.sp
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeBuffs.entries.toList(), key = { it.key.name }) { (buffType, value) ->
                                val label = when (buffType) {
                                    ArtifactBuffType.LOAN_INTEREST_DISCOUNT -> "Kredi Faizi: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.DEPOSIT_INTEREST_BONUS -> "Mevduat: +%${String.format(java.util.Locale.US, "%.1f", value * 100)}"
                                    ArtifactBuffType.LOGISTICS_COST_DISCOUNT -> "Lojistik: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.LOGISTICS_SPEED_BONUS -> "Lojistik Hız: +%${(value * 100).toInt()}"
                                    ArtifactBuffType.CONSTRUCTION_SPEED_BONUS -> "İnşaat Hızı: +%${(value * 100).toInt()}"
                                    ArtifactBuffType.UPGRADE_COST_DISCOUNT -> "Yükseltme: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.WEAR_LEVEL_REDUCTION -> "Aşınma: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.MAINTENANCE_COST_DISCOUNT -> "Bakım: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.TIER1_PRODUCTION_BONUS -> "Tier 1 Üretim: +%${(value * 100).toInt()}"
                                    ArtifactBuffType.TIER4_PRODUCTION_BONUS -> "Tier 4 Üretim: +%${(value * 100).toInt()}"
                                    ArtifactBuffType.BORSA_SELL_BONUS -> "Borsa Satış: +%${(value * 100).toInt()}"
                                    ArtifactBuffType.BORSA_BUY_DISCOUNT -> "Borsa Alım: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.MANAGER_SALARY_DISCOUNT -> "Yönetici Maaşı: -%${(value * 100).toInt()}"
                                    ArtifactBuffType.RD_RESEARCH_SPEED -> "Ar-Ge Hızı: +%${(value * 100).toInt()}"
                                    ArtifactBuffType.CONSORTIUM_PRESTIGE_BONUS -> "Prestij: +%${(value * 100).toInt()}"
                                }
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visitor Analytics Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F1E33),
                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = tr("SIRALAMA PROFİL ZİYARETÇİ HASILATI", "LEADERBOARD PROFILE VISITOR YIELD"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )
                    }

                    Text(
                        text = tr(
                            "Sıralama listesinde diğer oyuncular profilinizi her incelediğinde müzenize bilet geliri kazandırır!",
                            "Every time other players inspect your profile on the Leaderboard, they generate ticket revenue for your museum!"
                        ),
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        lineHeight = 14.sp
                    )

                    HorizontalDivider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)

                    FinancialRow(
                        label = tr("Profil Ziyaretçisi:", "Profile Inspections:"),
                        value = "$profileVisitCount ${tr("kez", "times")}",
                        color = Color.White
                    )

                    FinancialRow(
                        label = tr("Müze Prestij Puanı:", "Museum Prestige:"),
                        value = "+$museumPrestige ${tr("Puan", "Pts")}",
                        color = ThemeGold
                    )

                    FinancialRow(
                        label = tr("Saatlik Pasif Bilet Geliri:", "Hourly Passive Yield:"),
                        value = "+${formatCurrency(hourlyVisitorIncome, isEn)} / ${tr("saat", "hr")}",
                        color = ThemePositive
                    )

                    FinancialRow(
                        label = tr("Biriken Ziyaretçi Hasılatı:", "Accumulated Ticket Yield:"),
                        value = formatCurrency(profileBonusRevenue, isEn),
                        color = ThemePositive
                    )
                }
            }
        }

        // Owned Artifacts Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tr("SERGİLENEN ŞAHESERLER", "EXHIBITED MASTERPIECES")} (${ownedArtifacts.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (ownedArtifacts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0C1322),
                    border = BorderStroke(0.5.dp, ThemeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🏛️", fontSize = 28.sp)
                        Text(
                            text = tr("Henüz sergilenen tarihi eser bulunmuyor", "No historical artifacts displayed yet"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tr(
                                "Ana Menü > Ahilik Müzesi müzayedelerine katılarak eser toplayabilir ve profil ziyaretçilerinden bilet geliri kazanabilirsiniz.",
                                "Participate in auctions at Main Menu > Ahilik Museum to collect artifacts and generate ticket revenue."
                            ),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(ownedArtifacts, key = { it.id }) { artifact ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0C1322),
                    border = BorderStroke(1.dp, Color(artifact.rarity.colorHex).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(artifact.iconEmoji, fontSize = 22.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = artifact.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${artifact.era} • 📍 ${artifact.originCity}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                                val buff = ArtifactBuffRegistry.buffs.find { it.artifactId == artifact.id }
                                if (buff != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "✨ Pasif Güç: ${buff.loreDescription}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(artifact.rarity.colorHex).copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, Color(artifact.rarity.colorHex))
                            ) {
                                Text(
                                    text = "${artifact.rarity.badgeEmoji} ${artifact.rarity.displayName}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(artifact.rarity.colorHex),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "+${artifact.prestigeScore} ${tr("Puan", "Pts")}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 3: APPEARANCE (THEME & LANGUAGE)
// ----------------------------------------------------
@Composable
private fun ProfileAppearanceTab(
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = tr("🎨 UYGULAMA RENK VE TEMASI", "🎨 COLOR THEME SELECTION"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            ThemePickerSection(
                selectedThemeId = selectedTheme,
                onSelectTheme = onSelectTheme
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tr("🌐 DİL SEÇENEĞİ / LANGUAGE", "🌐 LANGUAGE SELECTION"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            LanguagePickerSection(
                selectedLanguageCode = selectedLanguage,
                onSelectLanguage = onSelectLanguage
            )
        }
    }
}

// ----------------------------------------------------
// TAB 4: SETTINGS (PERFORMANCE & PREFERENCES & CLOUD)
// ----------------------------------------------------
@Composable
private fun ProfileSettingsTab(
    isOnlineRegistered: Boolean = false,
    onlineEmail: String = "",
    onForceSyncCloud: () -> Unit = {},
    onForceRestoreCloud: () -> Unit = {},
    viewModel: com.example.viewmodel.GameViewModel? = null
) {
    val haptic = LocalHapticFeedback.current
    var hapticEnabled by remember { mutableStateOf(com.example.utils.HapticManager.isHapticEnabled) }
    var animationsEnabled by remember { mutableStateOf(com.example.utils.HapticManager.isAnimationsEnabled) }
    val backupStatus by (viewModel?.lastCloudBackupStatus?.collectAsState() ?: remember { mutableStateOf("") })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cloud Backup & Sync Section
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, if (isOnlineRegistered) ThemePositive.copy(alpha = 0.4f) else Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("☁️ BULUT SENKRONİZASYONU", "☁️ CLOUD SYNCHRONIZATION"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOnlineRegistered) ThemePositive.copy(alpha = 0.15f) else Color(0xFF334155)
                        ) {
                            Text(
                                text = if (isOnlineRegistered) tr("● ÇEVRİMİÇİ", "● ONLINE") else tr("○ YEREL", "○ LOCAL"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnlineRegistered) ThemePositive else Color.LightGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isOnlineRegistered) {
                            tr("Şirket verileriniz her dakika başında Supabase bulutuna otomatik olarak yedeklenir ($onlineEmail).", "Your company data is automatically backed up to Supabase cloud at the start of every minute ($onlineEmail).")
                        } else {
                            tr("Her dakika başı otomatik bulut yedeklemesini etkinleştirmek için yukarıdaki Google Girişi veya E-posta ile oturum açın.", "Sign in with Google or Email above to activate automatic cloud backup at the start of every minute.")
                        },
                        fontSize = 10.5.sp,
                        color = Color.LightGray
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tr("Otomatik Yedekleme: Her Dakika Başı (:00)", "Auto Backup: Every Minute (:00)"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ThemeNeonCyan
                                )
                            }
                            if (backupStatus.isNotBlank() && backupStatus != "Hata") {
                                Text(
                                    text = tr("Son: $backupStatus ✓", "Last: $backupStatus ✓"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemePositive
                                )
                            } else if (backupStatus == "Hata") {
                                Text(
                                    text = tr("Yeniden deneniyor...", "Retrying..."),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeNegative
                                )
                            } else {
                                Text(
                                    text = tr("⏱️ Aktif", "⏱️ Active"),
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onForceSyncCloud()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemePositive.copy(alpha = 0.85f),
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr("Buluta Yedekle", "Backup to Cloud"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onForceRestoreCloud()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ThemeNeonCyan
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr("Buluttan Yükle", "Restore Cloud"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Effects & Performance Section
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = tr("⚡ PERFORMANS VE EFEKTLER", "⚡ PERFORMANCE & EFFECTS"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Haptic switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr("Titreşim Geri Bildirimi", "Haptic Feedback"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(tr("Ticari işlemlerde dokunma titreşimi", "Device vibration on actions"), color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { checked ->
                                hapticEnabled = checked
                                com.example.utils.HapticManager.isHapticEnabled = checked
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Animations switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr("Canlı Animasyonlar & Efektler", "Live Animations & Effects"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(tr("Dinamik fabrika ve hareketli gradyanlar", "Dynamic particle and gradient effects"), color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = animationsEnabled,
                            onCheckedChange = { checked ->
                                animationsEnabled = checked
                                com.example.utils.HapticManager.isAnimationsEnabled = checked
                            }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// MODERN GOOGLE LOGO & SIGN-IN BUTTON COMPONENTS
// ----------------------------------------------------
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val strokeW = sizePx * 0.22f
        val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = (sizePx - strokeW) / 2f

        val redColor = Color(0xFFEA4335)
        val yellowColor = Color(0xFFFBBC05)
        val greenColor = Color(0xFF34A853)
        val blueColor = Color(0xFF4285F4)

        val arcStyle = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeW,
            cap = androidx.compose.ui.graphics.StrokeCap.Butt
        )
        val arcRect = androidx.compose.ui.geometry.Rect(
            left = centerOffset.x - radius,
            top = centerOffset.y - radius,
            right = centerOffset.x + radius,
            bottom = centerOffset.y + radius
        )

        // Red: Top arc (from 215° to 325°)
        drawArc(
            color = redColor,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            style = arcStyle,
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Blue: Right arc (from 325° to 45°)
        drawArc(
            color = blueColor,
            startAngle = 325f,
            sweepAngle = 80f,
            useCenter = false,
            style = arcStyle,
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Green: Bottom arc (from 45° to 145°)
        drawArc(
            color = greenColor,
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            style = arcStyle,
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Yellow: Left arc (from 145° to 215°)
        drawArc(
            color = yellowColor,
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            style = arcStyle,
            topLeft = arcRect.topLeft,
            size = arcRect.size
        )

        // Blue horizontal crossbar: Center to Right
        val barHeight = strokeW * 0.96f
        val barLeft = centerOffset.x - strokeW * 0.1f
        val barRight = centerOffset.x + radius + strokeW / 2f
        val barTop = centerOffset.y - barHeight / 2f
        drawRect(
            color = blueColor,
            topLeft = androidx.compose.ui.geometry.Offset(barLeft, barTop),
            size = androidx.compose.ui.geometry.Size(barRight - barLeft, barHeight)
        )
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isCompact: Boolean = false,
    text: String = "Google ile Giriş Yap"
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
        color = Color.White,
        shadowElevation = if (isCompact) 2.dp else 4.dp,
        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 10.dp else 16.dp,
                vertical = if (isCompact) 6.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (isCompact) 14.dp else 18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bağlanıyor...",
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3C4043)
                )
            } else {
                GoogleLogoIcon(modifier = Modifier.size(if (isCompact) 16.dp else 20.dp))
                Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 10.dp))
                Text(
                    text = text,
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF3C4043),
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
fun EmailAuthDialogWrapper(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: com.example.viewmodel.GameViewModel?,
    playerName: String
) {
    if (!show) return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val isEn = isEnglishLanguage()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = if (isEn) "Alternative Email Login" else "Alternatif E-posta Girişi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEn)
                        "If Google Sign-In is blocked, you can connect directly to your Supabase profile with your email and password."
                    else
                        "Google girişi engellendiğinde, Supabase üzerinde kayıtlı profilinize e-posta ve şifrenizle doğrudan bağlanabilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(if (isEn) "Email Address" else "E-posta Adresi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeNeonCyan,
                        unfocusedBorderColor = ThemeBorder
                    )
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(if (isEn) "Password" else "Şifre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeNeonCyan,
                        unfocusedBorderColor = ThemeBorder
                    )
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = ThemeNegative,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    enabled = !isSubmitting && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSubmitting = true
                        errorMessage = ""
                        viewModel?.loginOnline(emailInput.trim(), passwordInput) { success, msg ->
                            isSubmitting = false
                            if (success) {
                                onDismiss()
                                Toast.makeText(context, if (isEn) "Login Successful!" else "Giriş Başarılı!", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMessage = msg ?: (if (isEn) "Login failed." else "Giriş başarısız oldu.")
                            }
                        }
                    }
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isEn) "Log In" else "Giriş Yap", color = ThemeNeonCyan, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    enabled = !isSubmitting && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSubmitting = true
                        errorMessage = ""
                        viewModel?.registerOnline(emailInput.trim(), passwordInput, playerName) { success, msg ->
                            isSubmitting = false
                            if (success) {
                                onDismiss()
                                Toast.makeText(context, if (isEn) "Registration Successful!" else "Kayıt ve Giriş Başarılı!", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMessage = msg ?: (if (isEn) "Registration failed." else "Kayıt başarısız oldu.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan)
                ) {
                    Text(if (isEn) "Register" else "Kayıt Ol", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = { onDismiss() }
            ) {
                Text(if (isEn) "Cancel" else "İptal", color = Color.Gray)
            }
        }
    )
}
