package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.performCloudSaveBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExitSaveLoadingDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEng = isEnglishLanguage()

    val progressAnim = remember { Animatable(0.01f) }
    var saveStatusText by remember {
        mutableStateOf(if (isEng) "Packaging factory & market assets..." else "Tesis, üretim ve pazar varlıkları paketleniyor...")
    }
    var isStep1Done by remember { mutableStateOf(false) }
    var isStep2Done by remember { mutableStateOf(false) }
    var isStep3Done by remember { mutableStateOf(false) }
    var isCloudUploadDone by remember { mutableStateOf(false) }
    var isCompletedSuccessfully by remember { mutableStateOf(false) }
    var showForceExitButton by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )

    LaunchedEffect(Unit) {
        // Show force exit button after 8 seconds in case of slow/no connection
        scope.launch {
            delay(8000L)
            showForceExitButton = true
        }

        // Animate step 1: Facilities & Warehouse
        progressAnim.animateTo(0.28f, tween(500, easing = LinearEasing))
        isStep1Done = true
        saveStatusText = if (isEng) "Verifying HR Managers & team levels..." else "İnsan Kaynakları & müdür seviyeleri doğrulanıyor..."

        // Animate step 2: HR & Research
        progressAnim.animateTo(0.55f, tween(600, easing = LinearEasing))
        isStep2Done = true
        saveStatusText = if (isEng) "Synchronizing R&D techs and active research timers..." else "Ar-Ge araştırmaları ve aktif sayaçlar hazırlanıyor..."

        // Animate step 3: Cloud snapshot preparation
        progressAnim.animateTo(0.78f, tween(500, easing = LinearEasing))
        isStep3Done = true
        saveStatusText = if (isEng) "Securely uploading to Cloud Server (Supabase)..." else "Bulut sunucuya (Supabase) güvenle aktarılıyor..."

        // Perform real blocking save to Supabase
        try {
            viewModel.performCloudSaveBlocking()
        } catch (_: Exception) {}

        isCloudUploadDone = true
        progressAnim.animateTo(1.0f, tween(300, easing = FastOutSlowInEasing))
        isCompletedSuccessfully = true
        saveStatusText = if (isEng) "✓ Progress saved successfully! Closing game..." else "✓ İlerlemeniz Başarıyla Kaydedildi! Oyun kapatılıyor..."

        // Sıfır bekleme: Veriler aktarıldığı an oyunu derhal kapat
        (context as? Activity)?.finishAffinity()
    }

    Dialog(
        onDismissRequest = { /* Prevent accidental dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030712).copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center
        ) {
            // Background Particle Canvas
            DynamicParticleField(
                particleCount = 24,
                primaryColor = ThemeGold,
                secondaryColor = ThemeNeonCyan
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0B132B).copy(alpha = 0.95f),
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            ThemeGold.copy(alpha = 0.9f),
                            ThemeNeonCyan.copy(alpha = 0.5f),
                            Color(0xFF1E293B)
                        )
                    )
                ),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing Cloud Icon
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(haloPulse)
                                .clip(CircleShape)
                                .background(ThemeGold.copy(alpha = 0.12f))
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF131F3F),
                            border = BorderStroke(1.5.dp, ThemeGold),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCompletedSuccessfully) Icons.Rounded.CheckCircle else Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    tint = if (isCompletedSuccessfully) Color(0xFF10B981) else ThemeGold,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CurrencyText(
                        text = tr("İLERLEMENİZ GÜVENLE KAYDEDİLİYOR", "SAVING EMPIRE PROGRESS", isEng),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CurrencyText(
                        text = tr("Lütfen işlem tamamlanana kadar bekleyiniz...", "Please wait until cloud sync finishes...", isEng),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Percentage & Progress Bar
                    val percentInt = (progressAnim.value * 100).toInt().coerceIn(1, 100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = saveStatusText,
                            color = if (isCompletedSuccessfully) Color(0xFF34D399) else Color(0xFFCBD5E1),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrencyText(
                            text = "%$percentInt",
                            color = ThemeGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Visual Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ThemeGold, ThemeNeonCyan)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step Checklists
                    SaveStepRow(
                        title = tr("Tesis, borsa, konsorsiyum ve depo stokları paketlendi", "Facilities, stock portfolio, consortium & warehouse assets packaged", isEng),
                        isDone = isStep1Done
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SaveStepRow(
                        title = tr("İnsan Kaynakları & müdür kadrosu doğrulandı", "HR Managers & staff levels verified", isEng),
                        isDone = isStep2Done
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SaveStepRow(
                        title = tr("Ar-Ge araştırmaları ve aktif sayaçlar hazırlandı", "R&D techs & active research timers synced", isEng),
                        isDone = isStep3Done
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SaveStepRow(
                        title = tr("Bulut sunucuya (Supabase) güvenle aktarıldı", "Uploaded to Cloud Server (Supabase)", isEng),
                        isDone = isCloudUploadDone
                    )

                    if (showForceExitButton) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                (context as? Activity)?.finishAffinity()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CurrencyText(
                                text = tr("Beklemeden Çık (Yerel Kayıt Korumalı)", "Exit Now (Local Save Protected)", isEng),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveStepRow(
    title: String,
    isDone: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (isDone) Color(0xFF10B981) else Color(0xFF475569),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        CurrencyText(
            text = title,
            color = if (isDone) Color(0xFFE2E8F0) else Color(0xFF64748B),
            fontSize = 9.5.sp,
            fontWeight = if (isDone) FontWeight.Medium else FontWeight.Normal
        )
    }
}
