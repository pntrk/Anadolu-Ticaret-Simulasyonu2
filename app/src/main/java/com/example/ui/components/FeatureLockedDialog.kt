package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FeatureLockInfo
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr

/**
 * Faz 4: Kademeli Onboarding Kilit Diyalog Ekranı
 * 
 * Özellikler:
 * 1. Animasyonlu LinearProgressIndicator (İlerleme yüzdesi).
 * 2. Kilit açıldığında kazanılacak "Hoş Geldin Teşvik Hibesi" önizlemesi (Nakit & Elmas).
 * 3. Çift Koşullu (Seviye VEYA Net Servet) kilit açılış gereksinimi gösterimi.
 * 4. "Hemen XP & Nakit Kazan" aksiyon butonu.
 */
@Composable
fun FeatureLockedDialog(
    lockInfo: FeatureLockInfo?,
    onDismiss: () -> Unit,
    onNavigateToRd: () -> Unit,
    onNavigateToProduction: () -> Unit
) {
    if (lockInfo == null || lockInfo.isUnlocked) return

    val isEng = isEnglishLanguage()
    val featureTitle = if (isEng) lockInfo.feature.titleEn else lockInfo.feature.titleTr
    val reqTechName = if (isEng) lockInfo.requiredTechNameEn else lockInfo.requiredTechNameTr

    // Animated progress indicator
    val animatedProgress by animateFloatAsState(
        targetValue = lockInfo.progressPercent,
        animationSpec = tween(durationMillis = 800),
        label = "lock_progress_anim"
    )

    val progressPctInt = (animatedProgress * 100).toInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF091122),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEF4444).copy(alpha = 0.8f),
                        ThemeGold.copy(alpha = 0.5f),
                        Color(0xFF1E293B)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Stage Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1B4B),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f))
                ) {
                    CurrencyText(
                        text = "🔒 AŞAMA ${lockInfo.stageNumber} MİL TAŞI",
                        color = Color(0xFFA5B4FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lock Icon Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.8f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                CurrencyText(
                    text = featureTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Requirement & Progress Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F1B30),
                    border = BorderStroke(1.dp, Color(0xFF1E2F4D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level Condition Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = tr("Şirket Seviyesi", "Company Level"),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp
                                )
                                CurrencyText(
                                    text = "Lvl ${lockInfo.currentLevel} / Hedef Lvl ${lockInfo.minRequiredLevel}",
                                    color = if (lockInfo.currentLevel >= lockInfo.minRequiredLevel) ThemePositive else ThemeGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }

                        // Net Worth Condition Row
                        if (lockInfo.minRequiredNetWorth > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    CurrencyText(
                                        text = tr("VEYA Net Servet", "OR Net Worth"),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp
                                    )
                                    CurrencyText(
                                        text = "₳${formatMoney(lockInfo.currentNetWorth)} / Hedef ₳${formatMoney(lockInfo.minRequiredNetWorth)}",
                                        color = if (lockInfo.currentNetWorth >= lockInfo.minRequiredNetWorth) ThemePositive else ThemeNeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }

                        // R&D Tech Requirement (if applicable)
                        if (reqTechName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Science,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    CurrencyText(
                                        text = tr("VEYA Ar-Ge Araştırması", "OR R&D Research"),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp
                                    )
                                    CurrencyText(
                                        text = "🔬 $reqTechName",
                                        color = Color(0xFFC084FC),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E2F4D), thickness = 0.8.dp)

                        // Animated Progress Indicator Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("Kilit Açılış İlerlemesi", "Unlock Progress"),
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            CurrencyText(
                                text = "%$progressPctInt",
                                color = ThemeGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        // Animated LinearProgressIndicator
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ThemeGold,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Welcome Teşvik Hibesi Preview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CurrencyText(
                            text = tr("🎁 HOŞ GELDİN TEŞVİK HİBESİ ÖNİZLEMESİ", "🎁 WELCOME GRANT REWARD PREVIEW"),
                            color = Color(0xFF34D399),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Money Grant
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CurrencyText(
                                    text = "₳",
                                    color = ThemeGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                CurrencyText(
                                    text = "+₳${formatMoney(lockInfo.unlockRewardMoney)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }

                            // Gems Grant (if > 0)
                            if (lockInfo.unlockRewardGems > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Diamond,
                                        contentDescription = null,
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    CurrencyText(
                                        text = "+${lockInfo.unlockRewardGems} 💎",
                                        color = ThemeNeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Secondary Button (Kapat)
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.9f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                    ) {
                        CurrencyText(
                            text = tr("Kapat", "Close"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                    }

                    // Primary Action Button: "Hemen XP & Nakit Kazan"
                    Button(
                        onClick = {
                            onDismiss()
                            if (lockInfo.currentLevel >= 3 && lockInfo.requiredTechId != null) {
                                onNavigateToRd()
                            } else {
                                onNavigateToProduction()
                            }
                        },
                        modifier = Modifier.weight(1.4f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(
                            text = tr("Hemen XP & Nakit Kazan 🚀", "Earn XP & Cash Now 🚀"),
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
            }
        }
    }
}
}
