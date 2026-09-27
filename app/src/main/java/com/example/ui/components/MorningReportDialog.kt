package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.data.billing.findActivity
import com.example.data.OfflineProducedItem
import com.example.data.OfflineReport
import com.example.ui.theme.*

@Composable
fun MorningReportDialog(
    report: OfflineReport,
    playerGems: Int = 0,
    gemCost: Int = 2,
    onClaimNormal: () -> Unit,
    onClaimDoubleWithGems: () -> Unit = {},
    onClaimDoubleWithAd: () -> Unit = {},
    onClaimDoubleBonus: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val isEng = isEnglishLanguage()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity() ?: (context as? android.app.Activity) ?: com.example.MainActivity.currentActivity
    var isClaimingAd by remember { androidx.compose.runtime.mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD54F), Color(0xFFD4AF37), Color(0xFF8D6E63))
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                color = Color(0xFF1B1917), // Koyu Ahşap/Deri Tonu
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. ÜST BAŞLIK & NOSTALJİK ÇERÇEVE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF26221F))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD4AF37).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("SABAH RAPORU (GECE VARDİYASI)", "MORNING REPORT (NIGHT SHIFT)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = RajdhaniFontFamily,
                                color = Color(0xFFFFD54F),
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = tr("Sen uyurken atölyelerin harıl harıl çalıştı!", "Your workshops were working tirelessly while you slept!"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Divider(color = Color(0xFF3D3730), thickness = 1.dp)

                    // 2. KAYDIRILABİLİR İÇERİK
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SÜRE BİLGİSİ
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2B2621),
                                border = BorderStroke(1.dp, Color(0xFF4A4137))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Nightlight,
                                            contentDescription = null,
                                            tint = Color(0xFFB0BEC5),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tr("Geçen Süre: ${report.formattedDuration}", "Elapsed: ${report.formattedDuration}"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (report.isCapReached) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFBF360C)
                                        ) {
                                            Text(
                                                text = tr("8 SAAT DOLDU (CAP)", "8H MAX CAP"),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // HASILAT KARTI
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF132413),
                                border = BorderStroke(1.2.dp, Color(0xFF4CAF50))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tr("GECE HASILATI (NET KAZANÇ)", "NIGHT REVENUE (NET EARNINGS)"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF81C784),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₳${formatMoney(report.netRevenue)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color(0xFF69F0AE)
                                    )
                                    if (report.totalExpGained > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "+${report.totalExpGained} ${tr("Ticaret Tecrübesi (XP)", "Trade XP")}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold
                                        )
                                    }
                                    if (report.maintenanceCosts > 0) {
                                        Text(
                                            text = tr("(${formatMoney(report.maintenanceCosts)} ₺ Tesis Gece Masrafı Düşüldü)", "(${formatMoney(report.maintenanceCosts)} ₺ Facility Maintenance Deducted)"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // ÜRETİLEN MAHSUL & MALLAR (Zanaatkarlık Kalitesi)
                        if (report.producedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = tr("ÜRETİLEN MAHSULLER & KALİTE", "PRODUCED COMMODITIES & QUALITY"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            items(report.producedItems) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF26221F),
                                    border = BorderStroke(1.dp, item.quality.primaryColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        QualityBadge(quality = item.quality, size = 12.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.productName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (isEng) item.quality.labelEng else item.quality.labelTr,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = item.quality.primaryColor
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${item.quantity} ${tr("Adet", "Units")}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                            Text(
                                                text = "₳${formatMoney(item.totalPrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = Color(0xFF69F0AE)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // KULLANILAN HAMMADDE
                        if (report.consumedResources.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = tr("KULLANILAN HAMMADDE", "CONSUMED RAW MATERIALS"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFAB91),
                                        letterSpacing = 0.8.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        report.consumedResources.forEach { res ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF332B25)
                                            ) {
                                                Text(
                                                    text = "${res.resourceName}: -${res.amount}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFFFCCBC),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFF3D3730), thickness = 1.dp)

                    // 3. EYLEM BUTONLARI (2 Elmas 💎 veya 1 Reklam 🎬 ile 2X Bereket Bonusu)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF241F1C))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hasEnoughGems = playerGems >= gemCost
                        // 1. Button: 2 Gems for 2X
                        Button(
                            onClick = {
                                onClaimDoubleWithGems()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasEnoughGems) Color(0xFFFFD54F) else Color(0xFF8D6E63),
                                contentColor = Color.Black
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💎", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tr("$gemCost ELMAS İLE 2X BEREKET BONUSU", "DOUBLE WITH $gemCost GEMS"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.Black
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$playerGems 💎",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        // 2. Button: Watch 1 Ad for 2X
                        Button(
                            onClick = {
                                if (!isClaimingAd) {
                                    isClaimingAd = true
                                    if (activity != null && com.example.data.AdMobManager.isAdReady()) {
                                        com.example.data.AdMobManager.showRewardedAd(
                                            activity = activity,
                                            onRewardEarned = {
                                                onClaimDoubleWithAd()
                                            },
                                            onAdDismissed = { isClaimingAd = false }
                                        )
                                    } else {
                                        onClaimDoubleWithAd()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3E2723),
                                contentColor = Color(0xFFFFD54F)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFFD54F))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎬", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tr("1 REKLAM İZLE VE 2X KATLA", "WATCH 1 AD FOR 2X BONUS"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }

                        // 3. Button: Standard 1X Collection
                        OutlinedButton(
                            onClick = onClaimNormal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF5D4037)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFFCCBC)
                            )
                        ) {
                            Text(
                                text = tr("STANDART HASILAT OLARAK TOPLA (1X)", "CLAIM STANDARD (1X)"),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
