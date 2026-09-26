package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.trAuto
import kotlinx.coroutines.delay

@Composable
fun CorporateStampOverlay(
    isVisible: Boolean,
    title: String = "ONAYLANDI",
    subtitle: String = "RESMİ KONSORSİYUM & BORA MÜHÜRÜ",
    stampDate: String = "2026 / T.C. BORA AKD.",
    isWaxSeal: Boolean = true,
    onDismiss: () -> Unit
) {
    val displayTitle = title.trAuto()
    val displaySubtitle = subtitle.trAuto()
    val displayDate = stampDate.trAuto()
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2600)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            initialScale = 2.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val rotationState by remember { mutableStateOf((-12..-6).random().toFloat()) }

            if (isWaxSeal) {
                // Gold Wax Seal & Stamp
                Box(
                    modifier = Modifier
                        .rotate(rotationState)
                        .padding(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFB8860B),
                                    Color(0xFF785200)
                                )
                            )
                        )
                        .border(
                            BorderStroke(4.dp, Brush.sweepGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFD700), Color(0xFF8D6E63)))),
                            CircleShape
                        )
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF3E2723),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = displayTitle,
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            color = Color(0xFF261005),
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(2.dp)
                                .background(Color(0xFF4E342E))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = displaySubtitle,
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3E2723),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = displayDate,
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 8.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            } else {
                // Red Corporate Rubber Stamp
                Box(
                    modifier = Modifier
                        .rotate(rotationState)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22D32F2F))
                        .border(BorderStroke(4.dp, Color(0xFFD32F2F)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "★ $displayTitle ★",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = Color(0xFFE53935),
                            letterSpacing = 3.sp
                        )

                        Text(
                            text = displaySubtitle,
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350)
                        )
                    }
                }
            }
        }
    }
}
