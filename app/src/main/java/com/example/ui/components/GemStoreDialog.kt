package com.example.ui.components

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.data.billing.BillingManager
import com.example.data.billing.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemStoreDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player by viewModel.player.collectAsStateWithLifecycle()
    val billingProducts by BillingManager.productsMap.collectAsStateWithLifecycle()
    val isBillingReady by BillingManager.isBillingReady.collectAsStateWithLifecycle()
    val isQuerying by BillingManager.isQuerying.collectAsStateWithLifecycle()
    val lastBillingStatus by BillingManager.lastStatusMessage.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val p = player ?: return

    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Google Play Paketleri, 1: Ücretsiz Kazanım, 2: Avantajlar

    LaunchedEffect(Unit) {
        BillingManager.initialize(context.applicationContext)
        BillingManager.queryProducts()
        BillingManager.queryAndConsumeUnfinishedPurchases()
    }

    // Child Dialog: AdMob Rewarded Ad
    if (showRewardedAdDialog) {
        RewardedAdDialog(
            rewardGemsAmount = 5,
            onRewardEarned = { gems ->
                viewModel.claimRewardedAdGems(gems)
            },
            onDismiss = { showRewardedAdDialog = false }
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
                .padding(6.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0A101D),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        ThemeNeonCyan,
                        ThemeGold.copy(alpha = 0.8f),
                        Color(0xFF1E3A8A)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ==========================================
                // 1. TOP HEADER & CLOSE
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
                            shape = RoundedCornerShape(8.dp),
                            color = ThemeNeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeNeonCyan)
                        ) {
                            Box(
                                modifier = Modifier.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            CurrencyText(
                                text = tr("ELMAS MERKEZİ & MAĞAZA", "DIAMOND CENTER & STORE"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily,
                                letterSpacing = 0.5.sp
                            )
                            CurrencyText(
                                text = tr("Google Play Resmi Paketleri & Ücretsiz Ödüller", "Official Google Play Packages & Free Rewards"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeNeonCyan,
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
                            .size(36.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 2. LIVE DIAMOND BALANCE CARD
                // ==========================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF101B2E),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ThemeNeonCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CurrencyText("💎", fontSize = 20.sp)
                                }
                            }
                            Column {
                                CurrencyText(
                                    text = tr("MEVCUT ELMAS BAKİYENİZ", "CURRENT DIAMOND BALANCE"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    fontFamily = RobotoMonoFontFamily
                                )
                                CurrencyText(
                                    text = "${p.gems} 💎 ELMAS",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemeNeonCyan,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                            ) {
                                CurrencyText(
                                    text = tr("💎 RESMİ MAĞAZA", "💎 OFFICIAL STORE"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemeNeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = tr("Google Play Korumalı", "Google Play Protected"),
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // 3. SEGMENTED TABS
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        tr("⚡ Elmas Paketleri", "⚡ Diamond Packages"),
                        tr("🎁 Ücretsiz Kazan", "🎁 Earn Free"),
                        tr("📖 Avantajlar", "📖 Benefits")
                    )

                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = index
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) ThemeNeonCyan else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CurrencyText(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF002026) else Color.LightGray,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 4. TAB CONTENTS (SCROLLABLE)
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // ----------------------------------------------------
                            // TAB 0: GOOGLE PLAY STORE PACKAGES
                            // ----------------------------------------------------

                            if (!isBillingReady && lastBillingStatus != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = ThemeNeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        CurrencyText(
                                            text = lastBillingStatus ?: "",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            // Standard Google Play Packages Grid
                            val isEng = isEnglishLanguage()
                            val googlePlayPackages = listOf(
                                GooglePlayPackage(
                                    id = "gem_50",
                                    name = tr("Giriş Paketi", "Starter Pack"),
                                    gems = 50,
                                    bonusGems = 0,
                                    priceText = if (isEng) "$0.30" else "₺14.99",
                                    priceTry = 14.99
                                ),
                                GooglePlayPackage(
                                    id = "gem_150",
                                    name = tr("Tüccar Paketi", "Merchant Pack"),
                                    gems = 150,
                                    bonusGems = 15,
                                    priceText = if (isEng) "$0.60" else "₺29.99",
                                    priceTry = 29.99,
                                    badge = tr("🔥 POPÜLER", "🔥 POPULAR")
                                ),
                                GooglePlayPackage(
                                    id = "gem_500",
                                    name = tr("Gelişim Paketi", "Growth Pack"),
                                    gems = 500,
                                    bonusGems = 100,
                                    priceText = if (isEng) "$1.80" else "₺89.99",
                                    priceTry = 89.99,
                                    badge = tr("⚡ +100 HEDİYE", "⚡ +100 BONUS")
                                ),
                                GooglePlayPackage(
                                    id = "gem_1500",
                                    name = tr("CEO Paketi", "CEO Pack"),
                                    gems = 1500,
                                    bonusGems = 450,
                                    priceText = if (isEng) "$5.00" else "₺249.99",
                                    priceTry = 249.99,
                                    badge = tr("👑 ÇOK SATAN", "👑 BESTSELLER"),
                                    isBestValue = true
                                ),
                                GooglePlayPackage(
                                    id = "gem_4000",
                                    name = tr("Holding Paketi", "Holding Pack"),
                                    gems = 4000,
                                    bonusGems = 1500,
                                    priceText = if (isEng) "$12.00" else "₺599.99",
                                    priceTry = 599.99,
                                    badge = tr("🚀 DEV AVANTAJ", "🚀 MEGA VALUE")
                                ),
                                GooglePlayPackage(
                                    id = "gem_10000",
                                    name = tr("İmparatorluk Paketi", "Empire Pack"),
                                    gems = 10000,
                                    bonusGems = 5000,
                                    priceText = if (isEng) "$20.00" else "₺999.99",
                                    priceTry = 999.99,
                                    badge = tr("💎 EFSANEVİ", "💎 LEGENDARY")
                                )
                            )

                            googlePlayPackages.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { pkg ->
                                        val bgGradient = if (pkg.isBestValue) {
                                            Brush.verticalGradient(listOf(Color(0xFF2A2000), Color(0xFF1A1400)))
                                        } else {
                                            Brush.verticalGradient(listOf(Color(0xFF132036), Color(0xFF0C1422)))
                                        }
                                        
                                        val productDetails = billingProducts[pkg.id]
                                        val displayPrice = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: pkg.priceText

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val currentAct = context.findActivity() ?: com.example.MainActivity.currentActivity
                                                    if (currentAct != null) {
                                                        BillingManager.activePlayerId = p.id
                                                        BillingManager.launchBillingFlow(currentAct, pkg.id) { statusFeedback ->
                                                            com.example.ui.components.SmartNotificationManager.show(
                                                                statusFeedback,
                                                                statusFeedback,
                                                                com.example.ui.components.NotificationType.INFO
                                                            )
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.Transparent,
                                            border = BorderStroke(
                                                if (pkg.isBestValue) 1.5.dp else 1.dp,
                                                if (pkg.isBestValue) ThemeGold else ThemeNeonCyan.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(bgGradient)
                                                    .padding(top = 14.dp, bottom = 12.dp, start = 10.dp, end = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    if (pkg.badge != null) {
                                                        Surface(
                                                            shape = RoundedCornerShape(percent = 50),
                                                            color = if (pkg.isBestValue) ThemeGold else ThemeNeonCyan
                                                        ) {
                                                            CurrencyText(
                                                                text = pkg.badge,
                                                                color = Color(0xFF001520),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                    } else {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                    }

                                                    CurrencyText(
                                                        text = pkg.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (pkg.isBestValue) ThemeGold else Color.White,
                                                        fontSize = 13.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    // Gem amount with icon
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                        Text("💎", fontSize = 20.sp)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        CurrencyText(
                                                            text = "${pkg.gems}",
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (pkg.isBestValue) Color.White else ThemeNeonCyan,
                                                            fontSize = 20.sp,
                                                            fontFamily = RobotoMonoFontFamily,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    if (pkg.bonusGems > 0) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = ThemePositive.copy(alpha = 0.15f)
                                                        ) {
                                                            CurrencyText(
                                                                text = "+${pkg.bonusGems} " + tr("HEDİYE", "BONUS"),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = ThemePositive,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.height(18.dp))
                                                    }

                                                    Spacer(modifier = Modifier.height(14.dp))

                                                    // Buy Button
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (pkg.isBestValue) ThemeGold else ThemeNeonCyan,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.ShoppingCart,
                                                                contentDescription = null,
                                                                tint = Color(0xFF001520),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            CurrencyText(
                                                                text = displayPrice,
                                                                color = Color(0xFF001520),
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 13.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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

                        1 -> {
                            // ----------------------------------------------------
                            // TAB 1: FREE DIAMOND EARNING METHODS
                            // ----------------------------------------------------


                            // 2. AdMob Video Rewarded Ads
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF17253D),
                                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemeGold, ThemeNeonCyan)))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEA4335)) {
                                                CurrencyText("AdMob", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                            CurrencyText("🎬 ÜCRETSİZ SPONSOR VİDEOSU", color = ThemeGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }

                                        Surface(shape = RoundedCornerShape(4.dp), color = ThemePositive.copy(alpha = 0.2f), border = BorderStroke(0.8.dp, ThemePositive)) {
                                            CurrencyText("+5 💎 ANINDA", color = ThemePositive, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    CurrencyText(
                                        text = "Kısa bir sponsorlu reklam videosu izleyerek doğrudan +5 💎 Elmas kazanın. Günlük sınır bulunmamaktadır!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    AppButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showRewardedAdDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color(0xFF0B192C))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CurrencyText("REKLAM İZLE VE +5 💎 KAZAN", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // 3. Monthly League & Achievements Information Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF101C30),
                                border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CurrencyText("🏆", fontSize = 18.sp)
                                        CurrencyText(
                                            text = "AYLIK HOLDİNG LİGİ ÖDÜLLERİ",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }

                                    CurrencyText(
                                        text = "Her ay sonunda Anadolu Holdingler Sıralaması'nda ilk 3'e giren tüccarlara dev elmas ödülleri otomatik aktarılır:",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CurrencyText(tr("🥇 1. LİK", "🥇 1ST PLACE"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                                                CurrencyText("2.000 💎", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CurrencyText(tr("🥈 2. LİK", "🥈 2ND PLACE"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                                CurrencyText("1.000 💎", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CurrencyText(tr("🥉 3. LÜK", "🥉 3RD PLACE"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCD7F32))
                                                CurrencyText("500 💎", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ----------------------------------------------------
                            // TAB 2: GEM PERKS & USAGE GUIDE
                            // ----------------------------------------------------
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF111E33),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CurrencyText(
                                        text = tr("💎 ELMASIN OYUNDAKİ KULLANIM ALANLARI", "💎 DIAMOND USE CASES IN THE GAME"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = ThemeNeonCyan,
                                        fontFamily = RobotoMonoFontFamily
                                    )

                                    val perks = listOf(
                                        Triple("👔", tr("C-Level & Üst Düzey Müdür Transferi", "C-Level & Executive Manager Recruitment"), tr("Hazine, Sözleşme, Borsa, Lojistik, İK ve Operasyon müdürlerini şirketinize transfer edip işe almak için elmas bütçesi kullanılır. İşe alınan müdürlerin terfileri ve maaşları nakit ile devam eder.", "Diamond budget is used to recruit Treasury, Contracts, Stock, Logistics, HR and Operations managers to your firm. Subsequent promotions and salaries continue with cash.")),
                                        Triple("🔬", tr("Ar-Ge İleri Teknolojileri", "R&D Advanced Technologies"), tr("Ar-Ge merkezinde Seviye 1 ileri sanayi ve kuantum teknolojilerinin kilidini başlatmak için kullanılır.", "Used to unlock Level 1 advanced industrial and quantum technologies in the R&D center.")),
                                        Triple("🏢", tr("Konsorsiyum & Lonca Kurulumu", "Consortium & Guild Setup"), tr("Anadolu çapında kendi büyük ticaret konsorsiyumunuzu kurup diğer tüccarları davet etmek için gereklidir.", "Required to establish your own large trade consortium across Anatolia and invite other merchants.")),
                                        Triple("🚚", tr("Merkez Şehir & Depo Taşıma", "HQ & Warehouse Relocation"), tr("Lojistik ana merkezinizi veya fabrika depolarınızı stratejik ticaret şehirlerine anında taşırken muafiyet sağlar.", "Provides relocation exemption when instantly moving your logistics headquarters or factory warehouses to strategic trading cities.")),
                                        Triple("🏛️", tr("Tarihi Eser Müzayedesi & Prestij", "Historic Artifacts Auction & Prestige"), tr("Nadir Anadolu şaheserlerini koleksiyonunuza katmak ve şirket prestijinizi artırmak için kullanılır.", "Used to acquire rare Anatolian masterpieces for your corporate collection and increase holding prestige."))
                                    )

                                    perks.forEach { (icon, title, desc) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    CurrencyText(icon, fontSize = 16.sp)
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                CurrencyText(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                CurrencyText(desc, color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
