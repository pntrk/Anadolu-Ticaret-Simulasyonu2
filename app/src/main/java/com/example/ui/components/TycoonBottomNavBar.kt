package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.tr
import com.example.viewmodel.GameUiState

@Composable
fun TycoonBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    uiState: GameUiState? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isHome = currentRoute == "home" || currentRoute.isBlank() || currentRoute == "dashboard"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth press scaling
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_scale"
    )

    // Animated glowing rotation / shimmer ratio
    val infiniteTransition = rememberInfiniteTransition(label = "bottom_nav_shimmer")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val accentColor by animateColorAsState(
        targetValue = if (isHome) ThemeNeonCyan else ThemeGold,
        animationSpec = tween(350),
        label = "button_accent"
    )

    val routeDisplayName = when (currentRoute) {
        "market" -> tr("Pazar", "Market")
        "exchange" -> tr("Borsa", "Stock Exch.")
        "consortium" -> tr("Konsorsiyum", "Consortium")
        "production" -> tr("Üretim", "Production")
        "logistics" -> tr("Lojistik", "Logistics")
        "company" -> tr("Şirket", "Company")
        "bank" -> tr("Banka", "Bank")
        "rd" -> tr("Ar-Ge", "R&D")
        "weekly_growth" -> tr("Gelişim", "Growth")
        "leaderboard" -> tr("Liderlik", "Leaderboard")
        "settings" -> tr("Ayarlar", "Settings")
        else -> ""
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .scale(buttonScale)
            .testTag("bottom_nav_main_menu_container"),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF070D18).copy(alpha = 0.96f),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    ThemeNeonCyan.copy(alpha = if (isHome) glowAlpha else 0.4f),
                    Color(0xFF38BDF8).copy(alpha = 0.6f),
                    Color(0xFFA855F7).copy(alpha = 0.5f),
                    ThemeGold.copy(alpha = if (!isHome) glowAlpha else 0.4f)
                )
            )
        ),
        shadowElevation = 18.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate("home")
                }
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Icon with Pulsing Halo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp)
                ) {
                    // Subtle background glow aura
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f * glowAlpha))
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A),
                        border = BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = glowAlpha),
                                    accentColor.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isHome) Icons.Rounded.Home else Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = tr("Ana Menü", "Main Menu"),
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Text Stack
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = if (isHome) tr("ANA MENÜ", "MAIN MENU") else tr("ANA MENÜ'YE DÖN", "BACK TO MAIN MENU"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status Badge
                        if (isHome) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ThemeNeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CurrencyText(
                                        text = tr("AKTİF", "ACTIVE"),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = ThemeNeonCyan
                                    )
                                }
                            }
                        } else if (routeDisplayName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ThemeGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f))
                            ) {
                                CurrencyText(
                                    text = routeDisplayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = ThemeGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isHome) {
                        Text(
                            text = tr("Ana ekrana dönmek için dokunun", "Tap to return to main dashboard"),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
