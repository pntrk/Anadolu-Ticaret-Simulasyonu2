package com.example.ui.components

import com.example.ui.components.CurrencyText

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AntiqueArtifact
import com.example.data.MuseumHeritageManager
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AntiqueMuseumSection(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val player by viewModel.player.collectAsStateWithLifecycle()
    val p = player ?: return
    val onlineEmail by viewModel.onlineEmail.collectAsStateWithLifecycle()
    val onlineUid = remember(onlineEmail) { if (onlineEmail.isNotBlank() && onlineEmail != "misafir_tuccar") onlineEmail.replace(".", "_") else "" }
    val isEnglish = isEnglishLanguage()

    var refreshKey by remember { mutableIntStateOf(0) }
    var showFullMuseumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        MuseumHeritageManager.syncWithSupabase(context, p.name, onlineUid)
    }

    val ownedIds = remember(refreshKey) { MuseumHeritageManager.getOwnedArtifactIds(context) }
    val totalPrestige = remember(refreshKey) { MuseumHeritageManager.getTotalMuseumPrestige(context) }
    val hourlyIncome = remember(refreshKey) { MuseumHeritageManager.getTotalHourlyVisitorIncome(context) }
    val unclaimedRevenue = remember(refreshKey) { MuseumHeritageManager.calculateUnclaimedVisitorRevenue(context) }
    var auctions by remember { mutableStateOf<List<com.example.data.MuseumAuctionItem>>(emptyList()) }
    LaunchedEffect(refreshKey) {
        auctions = MuseumHeritageManager.getAllMuseumAuctions(context, p.name, onlineUid)
    }
    val activeAuction = remember(auctions) { auctions.firstOrNull() }

    val auctionArtifact = remember(activeAuction?.artifactId) {
        MuseumHeritageManager.allArtifacts.find { it.id == activeAuction?.artifactId } ?: MuseumHeritageManager.allArtifacts.first()
    }

    if (showFullMuseumDialog) {
        AntiqueMuseumDialog(
            viewModel = viewModel,
            onDismiss = {
                showFullMuseumDialog = false
                refreshKey++
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0D1829),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFF3B82F6))))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            CurrencyText(
                                text = tr("AHİLİK MİRASI MÜZESİ", "AHILIK HERITAGE MUSEUM"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily
                            )
                            CurrencyText(
                                text = tr("Tarihi Eser Koleksiyonu & Bilet Gelirleri", "Historical Artifact Collection & Ticket Revenues"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }

                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFullMuseumDialog = true
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                    ) {
                        CurrencyText(tr("🏛️ MÜZEYE GİR", "🏛️ ENTER MUSEUM"), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                    }
                }

                Divider(color = Color(0xFF1E3A5F))

                // Stats Bar (Balanced Grid Layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF14223A),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CurrencyText(tr("SERGİLENEN", "EXHIBITED"), fontSize = 8.5.sp, color = Color.Gray, fontFamily = RobotoMonoFontFamily, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText("${ownedIds.size}/${MuseumHeritageManager.allArtifacts.size} " + tr("Eser", "Arts"), fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF14223A),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CurrencyText(tr("PRESTİJ", "PRESTIGE"), fontSize = 8.5.sp, color = Color.Gray, fontFamily = RobotoMonoFontFamily, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText("+${totalPrestige} ⭐", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ThemePositive.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, ThemePositive.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1.15f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CurrencyText(tr("BİLET HASILATI", "TICKET REVENUE"), fontSize = 8.5.sp, color = ThemePositive.copy(alpha = 0.9f), fontFamily = RobotoMonoFontFamily, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText("+₳${formatMoney(hourlyIncome)}" + tr("/saat", "/hr"), fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = ThemePositive, fontFamily = RobotoMonoFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Quick Unclaimed Ticket Revenue Bar
        if (unclaimedRevenue > 0L) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0C241D),
                border = BorderStroke(1.dp, ThemePositive)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyText("🎟️", fontSize = 20.sp)
                        Column {
                            CurrencyText(tr("BİRİKEN BİLET GELİRİ", "ACCUMULATED TICKET INCOME"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemePositive, fontFamily = RobotoMonoFontFamily)
                            CurrencyText("₳${formatMoney(unclaimedRevenue)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = RobotoMonoFontFamily)
                        }
                    }

                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val earned = MuseumHeritageManager.claimVisitorRevenue(context)
                            viewModel.addMoneyDirectly(earned)
                            SmartNotificationManager.show(tr("🎟️ +₳${formatMoney(earned)} müze hasılatı kasaya aktarıldı!", "🎟️ +₳${formatMoney(earned)} museum revenue claimed and added to treasury!", isEnglish), NotificationType.SUCCESS)
                            refreshKey++
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePositive, contentColor = Color.Black)
                    ) {
                        CurrencyText(tr("TAHSİL ET", "CLAIM"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Live Auction Quick Teaser
        if (activeAuction != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF151D2F),
                border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyText(auctionArtifact.iconEmoji, fontSize = 24.sp)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0xFFEAB308)
                                ) {
                                    CurrencyText(tr("1/1 MÜZAYEDE", "1/1 AUCTION"), color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                                CurrencyText(auctionArtifact.getLocalizedName(isEnglishLanguage()), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            CurrencyText(
                                text = if (activeAuction.currentHighestBidder.isNotBlank()) tr("En Yüksek Pey: ", "Highest Bid: ") + "₳${formatMoney(activeAuction.currentHighestBid)} (${activeAuction.currentHighestBidder})" else tr("Başlangıç Fiyatı: ", "Starting Bid: ") + "₳${formatMoney(activeAuction.startingBid)}",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFullMuseumDialog = true
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F), contentColor = Color.White)
                    ) {
                        CurrencyText(tr("PEY SÜR", "PLACE BID") + " (10💎)", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Artifact Collection Grid Preview
        CurrencyText(tr("🏛️ MÜZE ESERLERİ VE VİTRİN:", "🏛️ MUSEUM ARTIFACTS & SHOWCASE:"), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.LightGray, fontFamily = RobotoMonoFontFamily)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MuseumHeritageManager.allArtifacts.chunked(2).forEach { rowArtifacts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowArtifacts.forEach { artifact ->
                        val isOwned = artifact.id in ownedIds
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showFullMuseumDialog = true
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOwned) Color(0xFF0F1E33) else Color(0xFF080E17),
                            border = BorderStroke(1.dp, if (isOwned) Color(artifact.rarity.colorHex).copy(alpha = 0.6f) else Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CurrencyText(if (isOwned) artifact.iconEmoji else "🔒", fontSize = 20.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    CurrencyText(
                                        text = artifact.getLocalizedName(isEnglishLanguage()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOwned) Color.White else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    CurrencyText(
                                        text = if (isOwned) "+₳${formatMoney(artifact.hourlyVisitorIncome)}" + tr("/s", "/h") else tr("Müzayedede", "In Auction"),
                                        fontSize = 9.sp,
                                        color = if (isOwned) ThemePositive else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
