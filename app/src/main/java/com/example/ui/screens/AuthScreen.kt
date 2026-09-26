package com.example.ui.screens

import com.example.viewmodel.*

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.ui.components.AppButton
import com.example.ui.theme.ThemeNeonCyan
import com.example.viewmodel.GameViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: GameViewModel,
    onSuccess: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGoogleLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF10192C),
            border = BorderStroke(1.5.dp, ThemeNeonCyan.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ThemeNeonCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = ThemeNeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "GOOGLE DRIVE BULUT SİSTEMİ",
                            color = ThemeNeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = "ANADOLU TİCARET SİMÜLASYONU",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Oyundaki tüm şirket ilerlemeniz (para, fabrikalar, seviyeler) kendi Google Drive hesabınızda %100 güvence altındadır.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                // Feature List Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuthFeatureRow(
                        icon = Icons.Default.CloudDone,
                        title = "Sınırsız Bulut Kaydı (Google Drive)",
                        description = "Tüm ilerlemeniz gizli AppData alanınızda saklanır. Sıfır kota kaybı."
                    )
                    AuthFeatureRow(
                        icon = Icons.Default.Sync,
                        title = "Çok Cihazlı Senkronizasyon",
                        description = "Yeni telefona geçseniz bile oyununuz kaldığı yerden anında yüklenir."
                    )
                    AuthFeatureRow(
                        icon = Icons.Default.Lock,
                        title = "Tek Tıkla Güvenli Giriş",
                        description = "Şifre hatırlama derdi yok. Google hesabınızla anında oyuna girin."
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isGoogleLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = ThemeNeonCyan, modifier = Modifier.size(32.dp))
                        Text(
                            text = statusText.ifEmpty { "Google ile bağlanılıyor..." },
                            color = ThemeNeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            com.example.utils.GoogleAuthHelper.launchGoogleSignIn(
                                context = context,
                                scope = scope,
                                viewModel = viewModel,
                                onStart = {
                                    isGoogleLoading = true
                                    statusText = "Google Kimlik Doğrulaması Başlatılıyor..."
                                },
                                onComplete = { success, msg ->
                                    isGoogleLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Google Hesabı ile Giriş Yapıldı!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        Toast.makeText(context, msg ?: "Giriş yapılamadı", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(24.dp))
                            Text(
                                text = "Google Hesabı İle Giriş Yap",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }

                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.signInAnonymously { success, _ ->
                                Toast.makeText(context, "Tüccar Profilinizle Giriş Yapıldı!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
                    ) {
                        Text(
                            text = "Tüccar Olarak Doğrudan Başla",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0B132B),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ThemeNeonCyan.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ThemeNeonCyan,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
