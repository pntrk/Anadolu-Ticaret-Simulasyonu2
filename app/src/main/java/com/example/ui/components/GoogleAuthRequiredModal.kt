package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.tr
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun GoogleAuthRequiredModal(
    featureName: String,
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isGoogleLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val startingAuthText = tr("Google Kimlik Doğrulaması Başlatılıyor...", "Starting Google Auth...")
    val loginSuccessText = tr("Google Girişi Başarılı! Kilitler kaldırıldı.", "Google Sign-In Success! Features unlocked.")
    val loginFailedText = tr("Giriş yapılamadı", "Sign-in failed")

    GameAdaptiveModalSheet(
        onDismissRequest = {
            if (!isGoogleLoading) onDismiss()
        },
        title = tr("GOOGLE GİRİŞİ GEREKLİ", "GOOGLE SIGN-IN REQUIRED"),
        subtitle = tr("$featureName Bölümü İçin Doğrulama", "Authentication for $featureName"),
        icon = Icons.Rounded.Lock,
        iconTint = Color(0xFFEF4444),
        badgeText = tr("ONLİNE MOD", "ONLINE MODE"),
        badgeColor = Color(0xFFEF4444),
        secondaryButtonText = if (!isGoogleLoading) tr("Misafir Olarak Kal", "Stay as Guest") else null,
        onSecondaryAction = onDismiss
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Hero Banner with Cyber Lock Effect
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0B1222),
                border = BorderStroke(1.dp, Color(0xFF1E2D4A))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_city_night),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.25f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x66080F1E),
                                        Color(0xDD080F1E)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.18f),
                            border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = tr("$featureName Sunucu Doğrulaması Gerektirir", "$featureName Requires Server Authentication"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. Info Description
            Text(
                text = tr(
                    "Pazar, Konsorsiyum, Müze Müzayedeleri ve Global Sıralama doğrudan canlı sunucu veritabanına bağlanır. Adil rekabet ve hile koruması için Google hesabıyla kayıt zorunludur.\n\n" +
                    "⚠️ Misafir modundaki paranız, fabrikalarınız ve seviyeniz kaybolmaz; Google hesabınıza otomatik aktarılır.",
                    "Market, Consortium, Museum Auctions and Global Leaderboard connect directly to live servers. Google Sign-In is required to prevent cheating and ensure fair play.\n\n" +
                    "⚠️ Your guest cash, factories and level won't be lost; they will be transferred automatically to your Google account."
                ),
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Feature Highlights
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuthBenefitRow(
                    icon = Icons.Default.CloudDone,
                    title = tr("Bulut Senkronizasyonu", "Cloud Synchronization"),
                    desc = tr("İlerlemeniz Google Drive ve sunucuda %100 güvende kalır.", "Your progress is 100% safe on Google Drive and cloud servers.")
                )
                AuthBenefitRow(
                    icon = Icons.Default.Public,
                    title = tr("Canlı Global Ticaret", "Live Global Trade"),
                    desc = tr("Pazar yerinde diğer tüccarlarla doğrudan hammadde alım-satımı.", "Trade directly with other merchants in the global market.")
                )
                AuthBenefitRow(
                    icon = Icons.Default.Security,
                    title = tr("Korumalı Sıralama", "Protected Leaderboards"),
                    desc = tr("Holdinginizin servetiyle Türkiye ve dünya zenginler liginde yarışın.", "Compete in national and global wealth leagues with your holding.")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Action Button (Google Sign-In)
            if (isGoogleLoading) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF131D33),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = ThemeNeonCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = statusMessage.ifEmpty { tr("Google bağlantısı kuruluyor...", "Connecting to Google...") },
                            color = ThemeNeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        com.example.utils.GoogleAuthHelper.launchGoogleSignIn(
                            context = context,
                            scope = scope,
                            viewModel = viewModel,
                            onStart = {
                                isGoogleLoading = true
                                statusMessage = startingAuthText
                            },
                            onComplete = { success, msg ->
                                isGoogleLoading = false
                                if (success) {
                                    Toast.makeText(context, loginSuccessText, Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    Toast.makeText(context, msg ?: loginFailedText, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Google İle Giriş Yap ve Kilidi Aç", "Sign In with Google & Unlock"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthBenefitRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1324),
        border = BorderStroke(1.dp, Color(0xFF1E2B45)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ThemeNeonCyan.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.35f)),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ThemeNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 13.sp
                )
            }
        }
    }
}
