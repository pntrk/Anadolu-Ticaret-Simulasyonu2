package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.data.GameFeature
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF091122),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEF4444).copy(alpha = 0.8f),
                        ThemeGold.copy(alpha = 0.4f),
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
                // Lock Badge with Glow Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
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
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title: [ BÖLÜM KİLİTLİ ]
                CurrencyText(
                    text = tr("🔒 BÖLÜM HENÜZ KİLİTLİ", "🔒 FEATURE CURRENTLY LOCKED"),
                    color = Color(0xFFF87171),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = RobotoMonoFontFamily,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                CurrencyText(
                    text = featureTitle,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Requirement Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F1B30),
                    border = BorderStroke(1.dp, Color(0xFF1E2F4D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Level Requirement Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                CurrencyText(
                                    text = tr("Şirket Seviyesi Gereksinimi", "Company Level Requirement"),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                                CurrencyText(
                                    text = tr(
                                        "Mevcut: Seviye ${lockInfo.currentLevel} / Hedef: Seviye ${lockInfo.minRequiredLevel}",
                                        "Current: Lvl ${lockInfo.currentLevel} / Target: Lvl ${lockInfo.minRequiredLevel}"
                                    ),
                                    color = if (lockInfo.currentLevel >= lockInfo.minRequiredLevel) Color(0xFF10B981) else ThemeGold,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { lockInfo.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ThemeGold,
                            trackColor = Color(0xFF1E293B)
                        )

                        // R&D Tech Requirement (if applicable)
                        if (reqTechName != null) {
                            HorizontalDivider(color = Color(0xFF1E2F4D), thickness = 0.8.dp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Science,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    CurrencyText(
                                        text = tr("Alternatif Ar-Ge Araştırması", "Alternative R&D Research"),
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                    CurrencyText(
                                        text = "🔬 $reqTechName",
                                        color = ThemeNeonCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Explanatory Tip
                CurrencyText(
                    text = tr(
                        "💡 İpucu: Fabrikalarınızda üretim yapıp borsada satış yaparak seviye atlayabilir ve yeni modüllerin kilidini açabilirsiniz!",
                        "💡 Tip: Produce in your facilities and sell on the exchange to level up and unlock new modules!"
                    ),
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Secondary Button (Kapat)
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                    ) {
                        CurrencyText(
                            text = tr("Kapat", "Close"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    // Primary Action: If R&D is unlocked (Lvl >= 3), go to R&D, else go to Production
                    if (lockInfo.currentLevel >= 3 && lockInfo.requiredTechId != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onNavigateToRd()
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CurrencyText(
                                text = tr("Ar-Ge'ye Git", "Go to R&D"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onNavigateToProduction()
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            CurrencyText(
                                text = tr("Üretime Başla", "Start Production"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
