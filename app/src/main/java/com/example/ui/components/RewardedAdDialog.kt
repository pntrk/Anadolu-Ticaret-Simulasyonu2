package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.data.AdMobConfig
import com.example.data.AdMobManager
import com.example.data.AdMobStatus
import com.example.data.billing.findActivity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RewardedAdDialog(
    adUnitId: String = AdMobConfig.REWARDED_AD_UNIT_ID,
    rewardGemsAmount: Int = AdMobConfig.REWARD_GEMS_AMOUNT,
    onRewardEarned: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: (context as? Activity) ?: MainActivity.currentActivity
    val haptic = LocalHapticFeedback.current
    val adStatus by AdMobManager.adStatus.collectAsStateWithLifecycle()
    val adStatusMsg by AdMobManager.statusMessage.collectAsStateWithLifecycle()

    var isAttemptingShow by remember { mutableStateOf(false) }
    var loadTimeout by remember { mutableStateOf(false) }

    // Automatic trigger: if ad is already ready when dialog opens, launch immediately!
    LaunchedEffect(Unit) {
        if (activity != null && AdMobManager.isAdReady()) {
            isAttemptingShow = true
            AdMobManager.showRewardedAd(
                activity = activity,
                onRewardEarned = onRewardEarned,
                onAdDismissed = onDismiss
            )
        } else {
            // Initiate load and show as soon as loaded
            if (activity != null) {
                AdMobManager.loadRewardedAd(
                    context = context,
                    onLoaded = {
                        isAttemptingShow = true
                        AdMobManager.showRewardedAd(
                            activity = activity,
                            onRewardEarned = onRewardEarned,
                            onAdDismissed = onDismiss
                        )
                    },
                    onFailed = {
                        loadTimeout = true
                    }
                )
            }
        }
    }

    // Fallback timeout: if ad doesn't load in 8 seconds (e.g. offline)
    LaunchedEffect(Unit) {
        delay(8000L)
        if (!AdMobManager.isAdReady() && !isAttemptingShow) {
            loadTimeout = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemeGold, ThemeNeonCyan)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEA4335) // AdMob red
                            ) {
                                Text(
                                    text = "AdMob",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "ÖDÜLLÜ VİDEO",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Content State Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!loadTimeout) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ThemeNeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = ThemeNeonCyan,
                                        strokeWidth = 3.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        tint = ThemeGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "Google AdMob Reklamı Yükleniyor...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Tam ekran sponsorlu video hazırlanıyor. Video bitiminde +$rewardGemsAmount 💎 Elmas hesabınıza eklenecektir.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(48.dp)
                                )

                                Text(
                                    text = "Google AdMob Bağlantısı",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Google AdMob reklam sunucusundan yanıt bekleniyor veya internet bağlantısı yavaş.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("Vazgeç", color = Color.LightGray, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                loadTimeout = false
                                if (activity != null) {
                                    if (AdMobManager.isAdReady()) {
                                        AdMobManager.showRewardedAd(activity, onRewardEarned, onDismiss)
                                    } else {
                                        AdMobManager.loadRewardedAd(
                                            context = context,
                                            onLoaded = {
                                                AdMobManager.showRewardedAd(activity, onRewardEarned, onDismiss)
                                            },
                                            onFailed = {
                                                loadTimeout = true
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeGold,
                                contentColor = Color(0xFF0B192C)
                            )
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yeniden Dene", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
