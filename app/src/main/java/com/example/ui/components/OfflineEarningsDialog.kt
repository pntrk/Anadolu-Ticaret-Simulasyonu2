package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.MainActivity
import com.example.data.AdMobManager
import com.example.data.Product
import com.example.data.billing.findActivity
import com.example.ui.theme.*
import com.example.viewmodel.OfflineEarningsData
import kotlinx.coroutines.launch

@Composable
fun OfflineEarningsDialog(
    data: OfflineEarningsData,
    playerGems: Int = 0,
    gemCost: Int = 10,
    onDismiss: () -> Unit,
    onDoubleWithGems: () -> Unit = {},
    onDoubleBonusClaim: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: (context as? Activity) ?: MainActivity.currentActivity
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isEn = isEnglishLanguage()

    val totalMinutes = (data.offlineDurationMs / (60 * 1000L)).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val durationFormatted = if (hours > 0) {
        if (isEn) "${hours}h ${minutes}m" else "${hours} Saat ${minutes} Dakika"
    } else {
        if (isEn) "${minutes}m" else "${minutes} Dakika"
    }

    var isClaimingAd by remember { mutableStateOf(false) }

    // Pulsing glowing border animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE070D18))
                .systemBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .padding(bottom = 28.dp), // Elevated above physical/virtual Android navigation buttons
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.85f)
                    .shadow(24.dp, shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            ThemeNeonCyan.copy(alpha = borderAlpha),
                            ThemeGold.copy(alpha = borderAlpha * 0.8f),
                            ThemeNeonCyan.copy(alpha = borderAlpha * 0.4f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Scrollable Content Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header Section with Animated Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeNeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, ThemeNeonCyan),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PrecisionManufacturing,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Titles
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CurrencyText(
                            text = if (isEn) "WELCOME BACK!" else "HOŞ GELDİNİZ!",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = ThemeGold,
                            fontFamily = RobotoMonoFontFamily,
                            letterSpacing = 1.2.sp
                        )
                        CurrencyText(
                            text = if (isEn) "OFFLINE PRODUCTION & REVENUE REPORT" else "ÇEVRİMDİŞİ GELİR VE ÜRETİM RAPORU",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        CurrencyText(
                            text = if (isEn) "Your industrial empire kept working while you were away" else "Siz yokken endüstriyel imparatorluğunuz çalışmaya devam etti",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Duration Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = (if (isEn) "Offline Time: " else "Geçen Süre: ") + durationFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }

                    // Hero Net Earnings Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF131E33),
                        border = BorderStroke(1.dp, if (data.netMoneyEarned >= 0) ThemePositive.copy(alpha = 0.6f) else ThemeNegative.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText(
                                text = if (isEn) "NET OFFLINE PROFIT" else "NET ÇEVRİMDİŞİ KÂR / BAKİYE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray,
                                fontFamily = RobotoMonoFontFamily
                            )

                            val isPositive = data.netMoneyEarned >= 0
                            val sign = if (isPositive) "+" else "-"
                            val moneyColor = if (isPositive) ThemePositive else ThemeNegative

                            CurrencyText(
                                text = "$sign₳${formatMoney(kotlin.math.abs(data.netMoneyEarned))}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = moneyColor,
                                fontFamily = RobotoMonoFontFamily
                            )

                            // Quick Stats Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CurrencyText(
                                        text = if (isEn) "Gross Revenue" else "Brüt Gelir",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    CurrencyText(
                                        text = "+₳${formatMoney(data.grossRevenue)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemePositive,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }

                                Divider(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(1.dp),
                                    color = Color(0xFF2E3D56)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CurrencyText(
                                        text = if (isEn) "Upkeep & Wages" else "Tesis & Gider",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    CurrencyText(
                                        text = "-₳${formatMoney(data.totalUpkeepCost)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (data.totalUpkeepCost > 0) Color(0xFFEF5350) else Color.Gray,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }

                                if (data.depositInterestEarned > 0) {
                                    Divider(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .width(1.dp),
                                        color = Color(0xFF2E3D56)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CurrencyText(
                                            text = if (isEn) "Interest" else "Faiz Getirisi",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        CurrencyText(
                                            text = "+₳${formatMoney(data.depositInterestEarned)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Production & Operations Summary Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF162238), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Factory,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CurrencyText(
                                    text = if (isEn) "Production Output" else "Üretilen Ürünler",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            CurrencyText(
                                text = "${data.itemsProducedCount} Ton",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        // Top Produced Products Row if available
                        if (data.producedItemsSummary.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(data.producedItemsSummary.toList(), key = { it.first }) { (prodId, qty) ->
                                    val product = Product.values().find { it.id == prodId }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color(0xFF2E3D56))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (product != null) {
                                                UniversalProductIcon(
                                                    product = product,
                                                    size = 18.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            CurrencyText(
                                                text = (product?.getDisplayName(isEn) ?: prodId) + ": +$qty Ton",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Color(0xFF26354D))

                        // Consortium & Manager Automation Summary
                        if (data.consortiumDeliveriesCount > 0 || data.consortiumDividendsEarned > 0 || data.managerActionsSummary.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1B182B), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Badge,
                                            contentDescription = null,
                                            tint = ThemeGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CurrencyText(
                                            text = if (isEn) "HR & Manager Automation" else "İnsan Kaynakları & Yönetici Otomasyonu",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold
                                        )
                                    }
                                    if (data.consortiumDeliveriesCount > 0) {
                                        CurrencyText(
                                            text = "${data.consortiumDeliveriesCount} Ton",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeNeonCyan,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }

                                data.managerActionsSummary.forEach { summary ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = ThemePositive,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        CurrencyText(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Divider(color = Color(0xFF26354D))
                        }

                        // Extra Milestone Badges (Logistics, R&D, XP)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active Facilities Tag
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Apartment,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = "${data.activeFacilitiesCount} " + if (isEn) "Facilities" else "Tesis",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }

                            // Deliveries Completed
                            if (data.deliveriesCompletedCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalShipping,
                                        contentDescription = null,
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(
                                        text = "${data.deliveriesCompletedCount} " + if (isEn) "Deliveries" else "Teslimat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            // XP Gained
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CurrencyText(
                                    text = "+${data.xpGained} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }

                    }

                    // 2. Elevated Bottom Action Bar (Fixed, never overlapped by system navigation)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0B1322),
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .padding(bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Double with Gems Button (Primary Feature)
                            val hasEnoughGems = playerGems >= gemCost
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDoubleWithGems()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasEnoughGems) ThemeGold else Color(0xFF6A541A),
                                    contentColor = Color.Black
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Diamond,
                                            contentDescription = null,
                                            tint = if (hasEnoughGems) Color.Black else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            CurrencyText(
                                                text = if (isEn) "DOUBLE WITH $gemCost GEMS (2X)" else "💎 $gemCost ELMAS İLE 2X KATLA",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = if (hasEnoughGems) Color.Black else Color.White
                                            )
                                            CurrencyText(
                                                text = if (isEn) "Instant 2X Cash & XP Gain" else "Anında 2 Kat Nakit ve XP Kazancı",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (hasEnoughGems) Color(0xFF3B2F00) else Color.LightGray
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (hasEnoughGems) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, if (hasEnoughGems) Color.Black.copy(alpha = 0.3f) else Color.Gray)
                                    ) {
                                        CurrencyText(
                                            text = "$playerGems 💎",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (hasEnoughGems) Color.Black else ThemeGold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }

                            // 2. 2X Bonus Button with Video Ad / Direct fallback
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!isClaimingAd) {
                                        isClaimingAd = true
                                        if (activity != null && AdMobManager.isAdReady()) {
                                            AdMobManager.showRewardedAd(
                                                activity = activity,
                                                onRewardEarned = {
                                                    onDoubleBonusClaim()
                                                },
                                                onAdDismissed = {
                                                    isClaimingAd = false
                                                }
                                            )
                                        } else {
                                            // Direct fallback with bonus if ad is loading/unavailable
                                            onDoubleBonusClaim()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CurrencyText(
                                        text = if (isEn) "Watch Ad for 2X (+5 Gems)" else "🎬 Reklam İzleyerek 2X (+5 Elmas)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeNeonCyan,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }

                            // 3. Regular 1X Claim Button
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                CurrencyText(
                                    text = if (isEn) "Claim Standard (1X) & Continue" else "Standart Nakit Olarak Topla (1X)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
