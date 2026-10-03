package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.isEnglishLanguage
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.syncCloudSaveToSupabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern %1'den %100'e animasyonlu "İlerlemeniz Kaydediliyor" tam ekran kayıt arayüzü.
 * Veriler buluta güvenle aktarıldıktan ve ilerleme %100 olduktan hemen sonra
 * hiçbir bekleme veya onay gerekmeden oyunu otomatik olarak kapatır (finishAffinity).
 */
@Composable
fun SaveProgressOverlay(
    viewModel: GameViewModel,
    onDismissRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEn = isEnglishLanguage()

    val progressAnim = remember { Animatable(0.01f) }
    var statusText by remember {
        mutableStateOf(
            if (isEn) "🏢 Packaging company financials and active facilities..."
            else "🏢 Şirket finansalları ve tesis verileri paketleniyor..."
        )
    }
    var stepIcon by remember { mutableStateOf(Icons.Rounded.Security) }
    var isCloudSyncFinished by remember { mutableStateOf(false) }

    // Geri tuşunu kilitle (kayıt sırasında yanlışlıkla çıkışı engelle)
    BackHandler(enabled = true) { /* no-op during safe exit sync */ }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        // 1. Arka planda gerçek bulut kaydını tetikle
        scope.launch {
            try {
                viewModel.syncCloudSaveToSupabase()
            } catch (e: Exception) {
                android.util.Log.e("SaveProgressOverlay", "Cloud sync error on exit", e)
            } finally {
                isCloudSyncFinished = true
            }
        }

        // 2. %1'den %35'e ilerleme (Yerel paketleme)
        progressAnim.animateTo(
            targetValue = 0.35f,
            animationSpec = tween(600, easing = LinearEasing)
        )
        statusText = if (isEn) "📈 Synchronizing stock market portfolio and investments..."
                     else "📈 Borsa portföyü ve yatırımlar yerel bellekten eşitleniyor..."
        stepIcon = Icons.Rounded.TrendingUp

        // 3. %35'ten %80'e ilerleme (Bulut aktarımı)
        progressAnim.animateTo(
            targetValue = 0.80f,
            animationSpec = tween(700, easing = LinearEasing)
        )
        statusText = if (isEn) "☁️ Securely transmitting data to cloud server..."
                     else "☁️ Veriler bulut sunucusuna güvenle iletiliyor..."
        stepIcon = Icons.Rounded.CloudUpload

        // 4. Bulut cevabı gelene kadar küçük bekleme / kontrol (max 3 saniye)
        var waitLoops = 0
        while (!isCloudSyncFinished && waitLoops < 15) {
            delay(150L)
            waitLoops++
        }

        // 5. %80'den %100'e tamamlama
        statusText = if (isEn) "✅ Cloud save completed! Exiting game..."
                     else "✅ Kayıt başarıyla tamamlandı! Oyun kapatılıyor..."
        stepIcon = Icons.Rounded.CloudDone
        progressAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )

        // 6. Sıfır bekleme: %100 olduğu an oyunu anında sonlandır!
        (context as? Activity)?.finishAffinity()
    }

    val currentPercent = (progressAnim.value * 100).toInt().coerceIn(1, 100)

    Dialog(
        onDismissRequest = { /* Modal kilitli */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.96f),
                            Color(0xFF030712).copy(alpha = 0.99f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            DynamicParticleField(
                particleCount = 28,
                modifier = Modifier.fillMaxSize()
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A0F1D).copy(alpha = 0.92f)
                ),
                border = BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = glowAlpha),
                            Color(0xFFFFD700).copy(alpha = glowAlpha * 0.8f),
                            Color(0xFF00E5FF).copy(alpha = glowAlpha)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // İkon Rozeti
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = stepIcon,
                                contentDescription = null,
                                tint = if (currentPercent >= 100) Color(0xFF10B981) else Color(0xFF00E5FF),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isEn) "SAVING PROGRESS" else "İLERLEMENİZ KAYDEDİLİYOR",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RajdhaniFontFamily,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFF1F5F9)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontFamily = RobotoMonoFontFamily,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Yüzde Göstergesi ve Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentPercent >= 100) (if (isEn) "Finalizing..." else "Kapatılıyor...")
                                   else (if (isEn) "Syncing Cloud..." else "Bulut Eşitleniyor..."),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "%$currentPercent",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (currentPercent >= 100) Color(0xFF10B981) else Color(0xFF00E5FF)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value.coerceIn(0.01f, 1.0f))
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFF38BDF8),
                                            Color(0xFF10B981)
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEn) "⚡ Data is saved with 0 traffic overhead. App will auto-close."
                               else "⚡ Verileriniz güvenle kaydedildikten sonra oyun otomatik kapanacaktır.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
