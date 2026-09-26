package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CaravanTravelAnimationDialog(
    originCityName: String,
    destinationCityName: String,
    onArrivalCompleted: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isFinished by remember { mutableStateOf(false) }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
        isFinished = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(600)
        onArrivalCompleted()
    }

    Dialog(onDismissRequest = { /* Non-cancellable during travel */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070E1E)),
            border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(ThemeGold, ThemeNeonCyan)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFinished) tr("HEDEFE VARIŞ SAĞLANDI! 🏁", "DESTINATION REACHED! 🏁") else tr("ANADOLU KERVAN SEFERİ 🐪", "ANATOLIAN CARAVAN EXPEDITION 🐪"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = ThemeGold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$originCityName ➔ $destinationCityName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Animated Journey Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        // Background road track
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(20f, centerY),
                            end = Offset(size.width - 20f, centerY),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                        // Traveled gold path
                        val currentX = 20f + (size.width - 40f) * animProgress.value
                        drawLine(
                            color = ThemeGold,
                            start = Offset(20f, centerY),
                            end = Offset(currentX, centerY),
                            strokeWidth = 6f
                        )
                    }

                    // Origin City Icon
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📍", fontSize = 14.sp)
                        }
                    }

                    // Moving Caravan / Vehicle
                    val progressFraction = animProgress.value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeGold,
                            border = BorderStroke(2.dp, Color.White),
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.CenterStart)
                                .offset(x = ((300.dp - 40.dp) * progressFraction).coerceAtLeast(0.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (isFinished) "✨" else "🐪", fontSize = 18.sp)
                            }
                        }
                    }

                    // Destination City Icon
                    Surface(
                        shape = CircleShape,
                        color = if (isFinished) ThemePositive else Color(0xFF1E293B),
                        border = BorderStroke(1.5.dp, if (isFinished) ThemePositive else ThemeNeonCyan),
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (isFinished) "🏁" else "🏰", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animProgress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ThemeGold,
                    trackColor = Color.White.copy(alpha = 0.1f),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isFinished)
                        tr("Kervanınız ve lojistik ekibiniz %s pazarına ulaştı!", "Your caravan and logistics team arrived at the %s market!").format(destinationCityName)
                    else
                        tr("Ticaret kervanı ve tırlar güvenli rotada ilerliyor...", "Trade caravan and trucks are advancing on a safe route..."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
