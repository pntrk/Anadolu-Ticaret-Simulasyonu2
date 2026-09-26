package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdvisorGoalType
import com.example.data.AdvisorRecommendation
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr

@Composable
fun SmartAdvisorHeroCard(
    recommendation: AdvisorRecommendation,
    onActionClick: (AdvisorRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val haptic = LocalHapticFeedback.current

    // Pulsing animation for the glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "advisor_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    val title = if (isEng) recommendation.titleEn else recommendation.titleTr
    val message = if (isEng) recommendation.messageEn else recommendation.messageTr
    val actionText = if (isEng) recommendation.actionTextEn else recommendation.actionTextTr
    val badgeText = if (isEng) recommendation.badgeEn else recommendation.badgeTr

    // Dynamic accent color based on goal type
    val accentColor = when (recommendation.goalType) {
        AdvisorGoalType.BUILD_FIRST_FACILITY -> Color(0xFF10B981) // Emerald
        AdvisorGoalType.START_PRODUCTION -> Color(0xFFF59E0B)     // Amber / Gold
        AdvisorGoalType.WAIT_PRODUCTION -> Color(0xFF94A3B8)      // Gray (Waiting)
        AdvisorGoalType.TRANSFER_TO_CENTRAL_WAREHOUSE -> Color(0xFFF97316) // Orange
        AdvisorGoalType.SELL_ON_BORSA -> Color(0xFF38BDF8)        // Sky Blue
        AdvisorGoalType.START_FIRST_RESEARCH -> Color(0xFFA855F7) // Purple
        AdvisorGoalType.OPEN_MARKET_DELIVERY -> Color(0xFFEC4899) // Pink / Magenta
        AdvisorGoalType.HIRE_AUTOMATION_MANAGER -> Color(0xFF6366F1) // Indigo
        AdvisorGoalType.DEPOSIT_IDLE_CASH -> Color(0xFF14B8A6)    // Teal
        AdvisorGoalType.SUPPLY_MEGA_CONSORTIUM -> Color(0xFFE11D48) // Rose
        AdvisorGoalType.EXPAND_EMPIRE -> ThemeGold
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF091224).copy(alpha = 0.90f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = glowAlpha),
                    ThemeGold.copy(alpha = 0.45f),
                    Color(0xFF1E293B)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onActionClick(recommendation)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Advisor Badge & Step / Level Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(
                            text = badgeText,
                            color = accentColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Step Counter & Progress
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyText(
                        text = tr("İlerleme: %${(recommendation.progressRatio * 100).toInt()}", "Progress: %${(recommendation.progressRatio * 100).toInt()}"),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(recommendation.progressRatio.coerceIn(0.05f, 1f))
                                .background(accentColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Row: Icon / Glowing Avatar + Title & Explanatory Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Advisor Icon Orb
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(46.dp)
                        .scale(iconScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (recommendation.goalType) {
                                AdvisorGoalType.BUILD_FIRST_FACILITY -> Icons.Rounded.Factory
                                AdvisorGoalType.START_PRODUCTION -> Icons.Rounded.PlayArrow
                                AdvisorGoalType.WAIT_PRODUCTION -> Icons.Rounded.HourglassEmpty
                                AdvisorGoalType.TRANSFER_TO_CENTRAL_WAREHOUSE -> Icons.Rounded.MoveToInbox
                                AdvisorGoalType.SELL_ON_BORSA -> Icons.Rounded.TrendingUp
                                AdvisorGoalType.START_FIRST_RESEARCH -> Icons.Rounded.Science
                                AdvisorGoalType.OPEN_MARKET_DELIVERY -> Icons.Rounded.LocalShipping
                                AdvisorGoalType.HIRE_AUTOMATION_MANAGER -> Icons.Rounded.Badge
                                AdvisorGoalType.DEPOSIT_IDLE_CASH -> Icons.Rounded.AccountBalance
                                AdvisorGoalType.SUPPLY_MEGA_CONSORTIUM -> Icons.Rounded.RocketLaunch
                                AdvisorGoalType.EXPAND_EMPIRE -> Icons.Rounded.Stars
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    CurrencyText(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    CurrencyText(
                        text = message,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Click Action Button with glowing gradient
            val buttonScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "btn_pulse"
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onActionClick(recommendation)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .scale(buttonScale)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.85f),
                                    ThemeGold
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF070D1A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CurrencyText(
                            text = actionText,
                            color = Color(0xFF070D1A),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = RobotoMonoFontFamily,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF070D1A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
