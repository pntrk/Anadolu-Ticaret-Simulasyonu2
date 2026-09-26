package com.example.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.quest.DailyQuest
import com.example.data.quest.DailyQuestManager
import com.example.data.quest.DailyQuestState
import com.example.data.quest.SeasonPassTier
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.tr
import com.example.viewmodel.GameViewModel

/**
 * Daily Quests & Season Pass Dialog.
 * Drives strong player retention (D1/D7/D30) through daily objectives and tiered season milestones.
 */
@Composable
fun DailyQuestsAndPassDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val questState by viewModel.dailyQuestState.collectAsState()
    val player by viewModel.player.collectAsState()
    val haptic = LocalHapticFeedback.current

    val currentXp = questState.seasonXp
    val currentLevel = questState.seasonLevel

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90A0F1D))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(ThemeGold.copy(alpha = 0.8f), ThemeNeonCyan.copy(alpha = 0.5f))))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // TOP BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(ThemeGold, Color(0xFFD97706)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF0A0F1D),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tr("GÖREVLER & SEZON PASOSU", "QUESTS & SEASON PASS"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold
                                )
                                Text(
                                    text = tr("Sezon Seviyesi: $currentLevel (TP: $currentXp)", "Season Level: $currentLevel (XP: $currentXp)"),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TAB CONTENTS
                    DailyQuestsList(
                        questState = questState,
                        onClaim = { quest ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.claimDailyQuestReward(quest)
                        },
                        onNavigateToRoute = { route ->
                            onDismiss()
                            onNavigateToRoute(route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyQuestsList(
    questState: DailyQuestState,
    onClaim: (DailyQuest) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = tr("Görevlerin üzerine tıklayarak ilgili ekrana doğrudan gidebilirsiniz. Hedefleri tamamlayıp ödülleri toplayın!", "Click on any quest to jump directly to its screen. Complete objectives and claim rewards!"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        items(questState.quests, key = { it.id }) { quest ->
            DailyQuestCard(
                quest = quest,
                onClaim = { onClaim(quest) },
                onNavigateToRoute = onNavigateToRoute
            )
        }
    }
}

@Composable
private fun DailyQuestCard(
    quest: DailyQuest,
    onClaim: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val isCompleted = quest.isCompleted
    val isClaimed = quest.isClaimed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isCompleted && !isClaimed && quest.targetRoute.isNotEmpty()) {
                    Modifier.clickable { onNavigateToRoute(quest.targetRoute) }
                } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isClaimed -> Color(0xFF131D2E).copy(alpha = 0.6f)
                isCompleted -> Color(0xFF1B2D3B)
                else -> Color(0xFF1E293B)
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isClaimed -> Color(0xFF334155)
                isCompleted -> ThemePositive.copy(alpha = 0.8f)
                else -> Color(0xFF334155)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val iconVector = when (quest.iconName) {
                        "storefront" -> Icons.Default.ShoppingCart
                        "trending_up" -> Icons.Default.Star
                        "factory" -> Icons.Default.Build
                        "local_shipping" -> Icons.Default.Send
                        else -> Icons.Default.Check
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) ThemePositive.copy(alpha = 0.2f) else ThemeNeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (isCompleted) ThemePositive else ThemeNeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = tr(quest.title, quest.titleEn),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tr(quest.description, quest.descriptionEn),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PROGRESS BAR
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tr("İlerleme", "Progress"),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${quest.currentProgress} / ${quest.targetAmount} (%${(quest.progressFraction * 100).toInt()})",
                        fontSize = 11.sp,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) ThemePositive else ThemeNeonCyan
                    )
                }

                LinearProgressIndicator(
                    progress = { quest.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isCompleted) ThemePositive else ThemeNeonCyan,
                    trackColor = Color(0xFF0B1220),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // REWARD AND ACTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyText(
                        text = "🎁 +${formatCredit(quest.rewardMoney)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold
                    )
                    Text(
                        text = "⭐ +${quest.rewardSeasonXp} TP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA855F7)
                    )
                }

                when {
                    isClaimed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(16.dp))
                            Text(
                                text = tr("Alındı", "Claimed"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive
                            )
                        }
                    }
                    isCompleted -> {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePositive),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tr("Ödülü Al", "Claim"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = { onNavigateToRoute(quest.targetRoute) },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, ThemeNeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tr("Göreve Git ➔", "Go to Quest ➔"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNeonCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompact(value: Long): String {
    return when {
        value >= 1_000_000_000_000L -> "%.1fT".format(value / 1_000_000_000_000.0)
        value >= 1_000_000_000L -> "%.1fMr".format(value / 1_000_000_000.0)
        value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
        value >= 1_000L -> "%.1fB".format(value / 1_000.0)
        else -> value.toString()
    }
}
