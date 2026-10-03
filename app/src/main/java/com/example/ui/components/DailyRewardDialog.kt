package com.example.ui.components
import com.example.ui.theme.RobotoMonoFontFamily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.viewmodel.DailyRewardData

@Composable
fun DailyRewardDialog(
    data: DailyRewardData,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    GameAdaptiveModalSheet(
        onDismissRequest = onDismiss,
        title = "GÜNLÜK GİRİŞ ÖDÜLÜ",
        subtitle = "${data.streakDay}. Günlük Giriş Serisi • 7 Günlük Zincir",
        icon = Icons.Default.EmojiEvents,
        iconTint = ThemeGold,
        badgeText = "GÜN ${data.streakDay}",
        badgeColor = ThemeGold,
        primaryButtonText = "ÖDÜLÜ TOPLA 🎁",
        primaryButtonColor = ThemeGold,
        onPrimaryAction = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDismiss()
        },
        secondaryButtonText = "Kapat",
        onSecondaryAction = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reward Display Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141F36),
                border = BorderStroke(1.dp, Color(0xFF283856))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KAZANILAN GÜNLÜK ÖDÜL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = data.rewardText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = ThemePositive,
                        fontFamily = RobotoMonoFontFamily,
                        textAlign = TextAlign.Center
                    )

                    if (data.isVipBonus) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ThemeGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, ThemeGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "VIP",
                                    tint = ThemeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "👑 VIP Ayrıcalığı: +10 Ekstra Elmas 💎",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold
                                )
                            }
                        }
                    }
                }
            }

            // 7-Day Streak Preview Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "7 GÜNLÜK SERİ İLERLEMESİ:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..7).forEach { day ->
                        val isDone = day <= data.streakDay
                        val isCurrent = day == data.streakDay
                        val dayText = when (day) {
                            1 -> "+100K"
                            2 -> "100B"
                            3 -> "100g"
                            4 -> "+500K"
                            5 -> "50Z"
                            6 -> "250g"
                            else -> "25💎"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrent) ThemeGold
                                        else if (isDone) ThemePositive.copy(alpha = 0.25f)
                                        else Color(0xFF162032)
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) ThemeGold else if (isDone) ThemePositive else Color(0xFF26334D),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone && !isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ThemePositive,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$day",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isCurrent) Color.Black else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayText,
                                fontSize = 9.sp,
                                color = if (isDone) ThemeGold else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
