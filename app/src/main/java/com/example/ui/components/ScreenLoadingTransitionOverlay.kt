package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan

@Composable
fun ScreenLoadingTransitionOverlay(
    isLoading: Boolean,
    targetRoute: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(animationSpec = tween(120)),
        exit = fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        var loadProgress by remember { mutableFloatStateOf(0f) }
        
        LaunchedEffect(Unit) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) { value, _ ->
                loadProgress = value
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
        
        val scalePulse by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        val routeData = remember(targetRoute) {
            getRouteLoadingDetails(targetRoute)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x800A0E17)), // More transparent to show MeshBackground behind
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF162032),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.sweepGradient(
                                listOf(
                                    ThemeNeonCyan.copy(alpha = glowAlpha),
                                    ThemeGold.copy(alpha = glowAlpha),
                                    ThemeNeonCyan.copy(alpha = glowAlpha)
                                )
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pulsing Glow Ring around Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = scalePulse
                                scaleY = scalePulse
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            ThemeNeonCyan.copy(alpha = 0.4f * glowAlpha),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D1527))
                                .border(1.dp, ThemeNeonCyan.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = routeData.icon,
                                contentDescription = null,
                                tint = ThemeNeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = routeData.title,
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = routeData.subtitle,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 12.sp,
                        color = ThemeNeonCyan.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YÜKLENİYOR...",
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            color = ThemeNeonCyan.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "%${(loadProgress * 100).toInt()}",
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ThemeGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Animated Gradient Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(loadProgress.coerceAtLeast(0.02f))
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            ThemeNeonCyan,
                                            ThemeGold,
                                            ThemeNeonCyan
                                        )
                                    )
                                )
                                .align(Alignment.CenterStart)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "⚡ ULTRA AKICI İŞLEM • AĞ SENKRONİZE EDİLDİ",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private data class RouteLoadingInfo(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private fun getRouteLoadingDetails(route: String): RouteLoadingInfo {
    val cleanRoute = route.substringBefore("?")
    return when (cleanRoute) {
        "home" -> RouteLoadingInfo(
            title = "ANA DASHBOARD",
            subtitle = "Tesis durumları ve finansal özet yükleniyor...",
            icon = Icons.Default.Home
        )
        "map" -> RouteLoadingInfo(
            title = "TİCARET HARİTASI",
            subtitle = "Şehir pazarları ve hammadde yolları taranıyor...",
            icon = Icons.Default.Map
        )
        "production" -> RouteLoadingInfo(
            title = "FABRİKA & ÜRETİM",
            subtitle = "Üretim bantları ve depo kotaları hazırlanıyor...",
            icon = Icons.Default.Business
        )
        "megaproject" -> RouteLoadingInfo(
            title = "MEGA PROJE KONSORSİYUMU",
            subtitle = "Ortak kotalar ve canlı senkronizasyon kuruluyor...",
            icon = Icons.Default.Groups
        )
        "market" -> RouteLoadingInfo(
            title = "KÜRESEL PAZAR YERİ",
            subtitle = "Tüccar ilanları ve canlı teklifler bağlanıyor...",
            icon = Icons.Default.ShoppingCart
        )
        "borsa" -> RouteLoadingInfo(
            title = "SPOT BORSA",
            subtitle = "Emtia fiyat grafikleri ve endeksler alınıyor...",
            icon = Icons.AutoMirrored.Filled.ShowChart
        )
        "hr" -> RouteLoadingInfo(
            title = "İ.K. & AR-GE MERKEZİ",
            subtitle = "Teknoloji ağaçları ve personel bilgileri yükleniyor...",
            icon = Icons.Default.People
        )
        "statistics" -> RouteLoadingInfo(
            title = "FİNANSAL RAPORLAR",
            subtitle = "Gelir-gider analizleri derleniyor...",
            icon = Icons.Default.BarChart
        )
        "social" -> RouteLoadingInfo(
            title = "LİDERLİK TABLOSU",
            subtitle = "Küresel tüccar sıralamaları güncelleniyor...",
            icon = Icons.Default.Public
        )
        "inventory" -> RouteLoadingInfo(
            title = "MERKEZ DEPO",
            subtitle = "Depo envanteri ve kargo hacmi doğrulanıyor...",
            icon = Icons.Default.Inventory
        )
        "bank" -> RouteLoadingInfo(
            title = "TİCARİ BANKA",
            subtitle = "Mevduat hesapları ve kredi oranları hazırlanıyor...",
            icon = Icons.Default.AccountBalance
        )
        else -> RouteLoadingInfo(
            title = "VERİ SERVİSİ",
            subtitle = "Piyasa verileri senkronize ediliyor...",
            icon = Icons.Default.ShowChart
        )
    }
}
