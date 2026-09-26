package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.viewmodel.GameViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class SmartRecommendation(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionText: String,
    val accentColor: Color,
    val onAction: () -> Unit
)

@Composable
fun SmartActionCard(
    viewModel: GameViewModel,
    onNavigateToMarket: () -> Unit,
    onNavigateToBorsa: () -> Unit,
    onNavigateToProduction: () -> Unit,
    onNavigateToHr: () -> Unit,
    onNavigateToBank: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEnglish = isEnglishLanguage()
    val playerState by viewModel.player.collectAsStateWithLifecycle()
    val player = playerState ?: return

    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()
    val managers by viewModel.managers.collectAsStateWithLifecycle()

    var isDismissed by remember { mutableStateOf(false) }
    var currentCardIndex by remember { mutableStateOf(0) }

    val usedCapacity = remember(inventory) { inventory.sumOf { it.quantity } }
    val maxCapacity = player.inventoryCapacity
    val fillRatio = if (maxCapacity > 0) usedCapacity.toFloat() / maxCapacity else 0f

    // Calculate smart recommendations dynamically based on current game state
    val recommendations = remember(usedCapacity, maxCapacity, fillRatio, player.money, player.depositBalance, businesses, managers, isEnglish) {
        val list = mutableListOf<SmartRecommendation>()

        // 1. Depo Doluluk Durumu
        if (fillRatio >= 0.70f) {
            list.add(
                SmartRecommendation(
                    id = "inventory_full",
                    icon = Icons.Rounded.Inventory2,
                    title = "📦 " + tr("Depo Dolmak Üzere (%%%d)", "Warehouse Almost Full (%d%%)", isEnglish).format((fillRatio * 100).toInt()),
                    description = tr("Deponda %d/%d Ton ürün birikti. Lojistik müdürün pazara satış yapmaya hazır!", "Accumulated %d/%d Tons of products in warehouse. Your logistics manager is ready to sell!", isEnglish).format(usedCapacity, maxCapacity),
                    actionText = tr("Pazarda Sat", "Sell in Market", isEnglish),
                    accentColor = ThemeGold,
                    onAction = onNavigateToMarket
                )
            )
        }

        // 2. Tesis Hammadde / Bakım / Üretim Durumu
        if (businesses.isEmpty()) {
            list.add(
                SmartRecommendation(
                    id = "no_facilities",
                    icon = Icons.Rounded.Factory,
                    title = "🏭 " + tr("İlk Üretim Tesisini Kur", "Build First Production Facility", isEnglish),
                    description = tr("Henüz üretici tesisin yok. Çiftlik, maden veya fabrika kurarak üretim ve ciro akışını başlat.", "You have no facilities yet. Build a farm, mine, or factory to start production and revenue flow.", isEnglish),
                    actionText = tr("Tesis Kur", "Build Facility", isEnglish),
                    accentColor = ThemeNeonCyan,
                    onAction = onNavigateToProduction
                )
            )
        } else if (businesses.any { it.wearLevel > 0.4f }) {
            list.add(
                SmartRecommendation(
                    id = "facility_maintenance",
                    icon = Icons.Rounded.Warning,
                    title = "⚠️ " + tr("Tesis Bakımı & Aşınma Uyarısı", "Facility Maintenance & Wear Warning", isEnglish),
                    description = tr("Tesislerinizde yıpranma oranı yükseldi. Bakım yaparak üretim verimliliğini maksimuma çıkarın.", "Wear level in your facilities has increased. Perform maintenance to maximize production efficiency.", isEnglish),
                    actionText = tr("Bakım Yap", "Maintain", isEnglish),
                    accentColor = Color(0xFFFF9800),
                    onAction = onNavigateToProduction
                )
            )
        }

        // 3. Atıl Nakit Bakiye & Banka
        if (player.money > 200_000L && player.depositBalance == 0L) {
            list.add(
                SmartRecommendation(
                    id = "idle_cash",
                    icon = Icons.Rounded.AccountBalance,
                    title = "🏦 " + tr("Atıl Nakit Bakiye (₳%s)", "Idle Cash Balance (₳%s)", isEnglish).format(formatMoney(player.money, isEnglish)),
                    description = tr("Kasandaki nakit paradan günlük pasif faiz geliri elde etmek için banka vadeli mevduatına yatırabilirsin.", "You can deposit cash into bank time deposits to earn daily passive interest income.", isEnglish),
                    actionText = tr("Bankaya Yatır", "Deposit to Bank", isEnglish),
                    accentColor = ThemePositive,
                    onAction = onNavigateToBank
                )
            )
        }

        // 4. İK & Yönetici Kadrosu
        if (managers.none { it.isHired }) {
            list.add(
                SmartRecommendation(
                    id = "hire_managers",
                    icon = Icons.Rounded.People,
                    title = "👥 " + tr("Yönetici Kadrosunu Kur", "Build Executive Team", isEnglish),
                    description = tr("Borsa, Lojistik veya Hazine müdürü işe alarak otomatik alım-satım ve risk analizini başlat.", "Hire Stock, Logistics, or Treasury managers to start automated trading and risk analysis.", isEnglish),
                    actionText = tr("Müdür İşe Al", "Hire Manager", isEnglish),
                    accentColor = Color(0xFFCE93D8),
                    onAction = onNavigateToHr
                )
            )
        }

        // 5. Borsa Fırsatı (Varsayılan Daimi Öneri)
        list.add(
            SmartRecommendation(
                id = "borsa_opportunity",
                icon = Icons.Rounded.TrendingUp,
                title = "📉 " + tr("Borsa Yatırım Fırsatı", "Stock Market Opportunity", isEnglish),
                description = tr("Şirket hisse senetlerinde ve vadeli sözleşmelerde haftalık fırsatları değerlendirip portföyünü büyüt.", "Grow your portfolio by leveraging weekly opportunities in stocks and futures contracts.", isEnglish),
                actionText = tr("Borsaya Git", "Go to Exchange", isEnglish),
                accentColor = ThemeNeonCyan,
                onAction = onNavigateToBorsa
            )
        )

        list
    }

    if (isDismissed || recommendations.isEmpty()) return

    val activeRec = recommendations.getOrElse(currentCardIndex % recommendations.size) { recommendations.first() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF0F1B2E),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(activeRec.accentColor, ThemeNeonCyan))),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
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
                        shape = RoundedCornerShape(2.dp),
                        color = ThemeGold.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(12.dp))
                            CurrencyText(tr("AKILLI ASİSTAN ÖNERİSİ", "SMART ASSISTANT TIP"), color = ThemeGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    if (recommendations.size > 1) {
                        CurrencyText(
                            text = "${(currentCardIndex % recommendations.size) + 1}/${recommendations.size}",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (recommendations.size > 1) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentCardIndex = (currentCardIndex + 1) % recommendations.size
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = tr("Sonraki Öneri", "Next Tip"), tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(
                        onClick = { isDismissed = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = tr("Kapat", "Close"), tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Body Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = activeRec.accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, activeRec.accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = activeRec.icon, contentDescription = null, tint = activeRec.accentColor, modifier = Modifier.size(22.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    CurrencyText(
                        text = activeRec.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    CurrencyText(
                        text = activeRec.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                // Action Button (Tek Tıkla Aksiyon)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeRec.onAction()
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeRec.accentColor, contentColor = Color(0xFF0F1B2E)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CurrencyText(activeRec.actionText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
