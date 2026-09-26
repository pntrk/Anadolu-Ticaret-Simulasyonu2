package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.GoogleAuthHelper
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.launch

enum class LockFeatureType {
    MARKET,
    CONSORTIUM,
    MUSEUM
}

data class LockFeatureConfig(
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val descTr: String,
    val descEn: String,
    val primaryIcon: ImageVector,
    val accentColor: Color,
    val secondaryColor: Color,
    val highlights: List<Triple<String, String, String>> // Icon emoji/symbol, titleTr, titleEn
)

@Composable
fun OnlineFeatureLockGate(
    feature: LockFeatureType,
    viewModel: GameViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
    isDialog: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isEnglish = isEnglishLanguage()

    var isSigningIn by remember { mutableStateOf(false) }

    val config = remember(feature) {
        when (feature) {
            LockFeatureType.MARKET -> LockFeatureConfig(
                titleTr = "B2B Küresel Pazar",
                titleEn = "Global B2B Market",
                subtitleTr = "Canlı Ticaret Ağı (Çevrimdışı Oyunculara Kapalı)",
                subtitleEn = "Live Trade Network (Locked for Offline Players)",
                descTr = "Diğer holdingler ve şirketlerle anlık hammadde ticareti yapmak, arz-talep dengesine göre serbest fiyat belirlemek ve küresel pazar tekliflerini canlı görmek için Google Play hesabınızla giriş yapmalısınız.",
                descEn = "To trade commodities in real-time with other companies, set free-market prices based on supply and demand, and browse live peer offers, please sign in with Google Play.",
                primaryIcon = Icons.Default.Storefront,
                accentColor = Color(0xFF00E5FF), // Neon Cyan
                secondaryColor = Color(0xFFFFD700), // Gold
                highlights = listOf(
                    Triple("⚡", "Anlık Canlı İlanlar & Siparişler", "Real-Time Listings & Orders"),
                    Triple("🌐", "Gerçek Zamanlı Bulut Senkronizasyonu", "Realtime Cloud Synchronization"),
                    Triple("💰", "Otomatik Ticaret & Bakiye Hakedişi", "Automated Settlement & Revenue")
                )
            )
            LockFeatureType.CONSORTIUM -> LockFeatureConfig(
                titleTr = "Milli Konsorsiyum Merkezi",
                titleEn = "National Consortium Hub",
                subtitleTr = "Çok Oyunculu Sanayi Ortaklığı (Çevrimdışı Oyunculara Kapalı)",
                subtitleEn = "Multiplayer Industrial Union (Locked for Offline Players)",
                descTr = "Türkiye çapında diğer şirketlerle devasa mega projeler kurmak, ortak hammadde tedarik hatlarını yönetmek, seri üretime geçmek ve holding sıralamasında yarışmak için Google Play hesabınızla giriş yapmalısınız.",
                descEn = "To partner with other companies on national mega projects, supply critical raw materials jointly, run mass production lines, and compete on the leaderboard, please sign in with Google Play.",
                primaryIcon = Icons.Default.Groups,
                accentColor = Color(0xFFFFD700), // Gold
                secondaryColor = Color(0xFFFF5252), // Red
                highlights = listOf(
                    Triple("🏭", "Ortak Mega Projeler & Fabrikalar", "Joint Mega Projects & Factories"),
                    Triple("📦", "Seri Üretim & Kriz Lojistiği", "Mass Production & Supply Chains"),
                    Triple("🏆", "Türkiye Holding Liderlik Sıralaması", "National Holding Leaderboards")
                )
            )
            LockFeatureType.MUSEUM -> LockFeatureConfig(
                titleTr = "Anadolu Antika & Miras Müzesi",
                titleEn = "Anatolian Heritage & Antique Museum",
                subtitleTr = "Küresel Eşsiz Müzayede (Çevrimdışı Oyunculara Kapalı)",
                subtitleEn = "Global Unique Auctions (Locked for Offline Players)",
                descTr = "Dünyada sadece 1 adet bulunan eşsiz 1-of-1 tarihi eserlerin canlı müzayedelerine katılmak, açık artırmada teklif vermek ve ziyaretçi gelirlerini güvenle buluta kaydetmek için Google Play hesabınızla giriş yapmalısınız.",
                descEn = "To participate in live auctions for global 1-of-1 unique historical artifacts, place real-time bids, and safely secure visitor passive income to the cloud, please sign in with Google Play.",
                primaryIcon = Icons.Default.AccountBalance,
                accentColor = Color(0xFFF59E0B), // Amber
                secondaryColor = Color(0xFF3B82F6), // Blue
                highlights = listOf(
                    Triple("🏛️", "Dünyada Tek: 1-of-1 Tarihi Eserler", "Unique Global 1-of-1 Artifacts"),
                    Triple("🔨", "Canlı Oyuncu Açık Artırmaları", "Live Player Auctions & Bidding"),
                    Triple("🎟️", "Saatlik Pasif Ziyaretçi Hasılatı", "Hourly Passive Visitor Revenue")
                )
            )
        }
    }

    // Infinite breathing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow_anim")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060A12),
                        Color(0xFF09101E),
                        Color(0xFF04060A)
                    )
                )
            )
            .padding(if (isDialog) 16.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), spotColor = config.accentColor.copy(alpha = 0.5f))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                config.accentColor.copy(alpha = pulseGlow),
                                config.secondaryColor.copy(alpha = 0.4f),
                                Color(0xFF1E293B)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0B1324).copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isDialog) 20.dp else 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badge & Lock Stack Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    // Outer glow halo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        config.accentColor.copy(alpha = pulseGlow * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Core icon background
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = Color(0xFF111D35),
                        border = BorderStroke(1.2.dp, config.accentColor.copy(alpha = 0.6f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = config.primaryIcon,
                                contentDescription = null,
                                tint = config.accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Small Red Lock Badge Overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp),
                        shape = CircleShape,
                        color = Color(0xFFD32F2F),
                        border = BorderStroke(1.5.dp, Color(0xFF0B1324))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Kilitli",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = if (isEnglish) config.titleEn else config.titleTr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RajdhaniFontFamily,
                    color = Color(0xFFF1F5F9),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF331418),
                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEnglish) config.subtitleEn else config.subtitleTr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Explanatory Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF080D1A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) config.descEn else config.descTr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Start
                        )

                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)

                        // Highlight items
                        config.highlights.forEach { (emoji, itemTr, itemEn) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(emoji, fontSize = 14.sp)
                                Text(
                                    text = if (isEnglish) itemEn else itemTr,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action 1: Google Play Sign In (Primary Action)
                Button(
                    onClick = {
                        if (isSigningIn) return@Button
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSigningIn = true
                        GoogleAuthHelper.launchGoogleSignIn(
                            context = context,
                            scope = coroutineScope,
                            viewModel = viewModel,
                            onStart = {
                                isSigningIn = true
                            },
                            onComplete = { success, msg ->
                                isSigningIn = false
                                if (success) {
                                    SmartNotificationManager.show(
                                        if (isEnglish) "Google account connected successfully! Welcome." else "Google hesabı başarıyla bağlandı! Çevrimiçi bölümler açıldı.",
                                        NotificationType.SUCCESS
                                    )
                                } else {
                                    SmartNotificationManager.show(
                                        msg ?: if (isEnglish) "Sign in failed" else "Giriş yapılamadı",
                                        NotificationType.ALERT
                                    )
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F2937)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF1F2937),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isEnglish) "Connecting..." else "Bağlanıyor...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                    } else {
                        // Google 'G' stylized branding or Play icon
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Google Play",
                            tint = Color(0xFF0086F8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isEnglish) "Sign in with Google Play" else "Google Play ile Giriş Yap",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color(0xFF111827)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Return to Main Menu / Dashboard (Secondary Action)
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFF94A3B8)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Ana Menü",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Return to Main Menu" else "Ana Menüye Dön",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trust & Security badge footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Realtime Cloud Sync & Verified Security" else "Bulut Senkronizasyonu & Güvenli Oturum",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }
    }
}
