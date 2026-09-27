package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ForeclosureAuction
import com.example.data.ForeclosureManager
import com.example.data.Product
import com.example.data.cities
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun ForeclosureBankruptcyDesk(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val auctions by ForeclosureManager.auctionsState.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // İflas Masası Başlık Kartı
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF131D31),
            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ThemeGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ThemeGold),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🏛️", fontSize = 20.sp)
                    }
                }
                Column {
                    Text(
                        text = "🏛️ İFLAS MASASI & MÜZAYEDE MERKEZİ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ThemeGold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    Text(
                        text = "Temerrüde düşen şirketlerin haczedilen tesisleri %50 indirimli açık artırmada veya hemen al ile tasfiye edilir (Tier 4 hariç)",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        if (auctions.isEmpty() || auctions.none { !it.isSettled }) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF131D31)),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🏛️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Şu an icradan satılık tesis bulunmuyor.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            auctions.filter { !it.isSettled }.forEach { auction ->
                ForeclosureAuctionCard(
                    auction = auction,
                    playerId = player?.id,
                    onBuyout = { viewModel.buyoutForeclosedFacility(auction.id) },
                    onPlaceBid = { bidAmount -> viewModel.placeForeclosureBid(auction.id, bidAmount) }
                )
            }
        }
    }
}

@Composable
fun ForeclosureAuctionCard(
    auction: ForeclosureAuction,
    playerId: String? = null,
    onBuyout: () -> Unit,
    onPlaceBid: (Long) -> Unit
) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(auction.id) {
        while (true) {
            delay(1000L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val remainingMs = (auction.endsAtMs - currentTimeMs).coerceAtLeast(0L)
    val remainingMinutes = remainingMs / 60000L
    val remainingSeconds = (remainingMs % 60000L) / 1000L
    val timeFormatted = String.format("%02d:%02d", remainingMinutes, remainingSeconds)

    val product = remember(auction.facilityType) {
        Product.values().find { it.facilityId == auction.facilityType || it.id == auction.facilityType }
    }
    val brandColor = if (product != null) Color(product.colorTint) else ThemeNeonCyan

    val facilityTitle = product?.getFacilityName() ?: auction.facilityType
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    val cityObj = remember(auction.cityId) { cities.find { it.id == auction.cityId } }
    val cityDisplayName = cityObj?.let { "${it.countryFlag} ${it.name}" } ?: auction.cityId.uppercase()

    val isUserHighestBidder = playerId != null && auction.highestBidderId == playerId

    val minIncrement = maxOf(5_000L, (auction.buyoutPrice * 0.04).toLong())
    val nextMinBid = if (auction.highestBidderId == null) {
        auction.startingBid
    } else {
        (auction.currentHighestBid + minIncrement).coerceAtMost(auction.buyoutPrice)
    }

    var selectedBidAmount by remember(auction.currentHighestBid) {
        mutableLongStateOf(nextMinBid)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, brandColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF151F33)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tesis Görseli, Adı (facilityType), Şehir (cityId), Seviye (level) & Kalan Süre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tesis / Ürün Oyun İçi Görsel Rozeti
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = brandColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.2.dp, brandColor.copy(alpha = 0.6f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (product != null) {
                                UniversalProductIcon(product = product, size = 30.dp, tint = brandColor)
                            } else {
                                UniversalProductIcon(productId = auction.facilityType, size = 30.dp, tint = brandColor)
                            }
                        }
                    }

                    Column {
                        Text(
                            text = facilityTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        if (product != null) {
                            Text(
                                text = "Üretim: ${product.getDisplayName()}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Text(
                            text = "📍 $cityDisplayName • Seviye ${auction.level} ★",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemeNeonCyan
                        )
                    }
                }

                // Kalan Süre Sayacı
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "⏳ $timeFormatted",
                        color = ThemeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

            // Eski Sahibi: originalOwnerName (reason bilgisiyle birlikte)
            Text(
                text = "Eski Sahibi: ${auction.originalOwnerName} • ${auction.reason}",
                fontSize = 11.sp,
                color = Color(0xFFEF9A9A),
                fontWeight = FontWeight.Medium
            )

            // Fiyat & Teklif Şeridi
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(0.8.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mevcut En Yüksek Pey:",
                                fontSize = 10.5.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "₳${formatMoney(auction.currentHighestBid)}",
                                fontSize = 13.sp,
                                color = if (isUserHighestBidder) ThemePositive else ThemeNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Teklif Sahibi:",
                                fontSize = 10.5.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = when {
                                    isUserHighestBidder -> "🏆 Siz (Lidersiniz)"
                                    auction.highestBidderName != null -> auction.highestBidderName
                                    else -> "Henüz Teklif Yok"
                                },
                                fontSize = 11.sp,
                                color = if (isUserHighestBidder) ThemePositive else Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Taban Pey: ₳${formatMoney(auction.startingBid)}",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Text(
                            text = "Tasfiye / Hemen Al: ₳${formatMoney(auction.buyoutPrice)}",
                            fontSize = 11.sp,
                            color = ThemeGold,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }

            // Hızlı Pey Artırma Butonları (Müzayede Sistemi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val increments = listOf(10_000L, 25_000L, 50_000L, 100_000L)
                increments.forEach { inc ->
                    val bidOption = (auction.currentHighestBid + inc).coerceAtMost(auction.buyoutPrice)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (selectedBidAmount == bidOption) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = BorderStroke(0.8.dp, if (selectedBidAmount == bidOption) ThemeNeonCyan else Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Button(
                            onClick = { selectedBidAmount = bidOption },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "+${formatMoney(inc)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedBidAmount == bidOption) ThemeNeonCyan else Color.LightGray,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }

            // İki Ana İşlem Butonu: [🔨 Teklif Ver] ve [⚡ Hemen Al]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Teklif Ver Butonu (Müzayede)
                Button(
                    onClick = { onPlaceBid(selectedBidAmount.coerceAtLeast(nextMinBid)) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUserHighestBidder) Color(0xFF1E293B) else ThemeNeonCyan,
                        contentColor = if (isUserHighestBidder) ThemePositive else Color(0xFF002229)
                    ),
                    contentPadding = PaddingValues(vertical = 9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isUserHighestBidder) "🏆 En Yüksek (₳${formatMoney(auction.currentHighestBid)})" else "🔨 Pey Sür: ₳${formatMoney(selectedBidAmount.coerceAtLeast(nextMinBid))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }

                // Hemen Al Butonu
                Button(
                    onClick = onBuyout,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeGold,
                        contentColor = Color(0xFF241600)
                    ),
                    contentPadding = PaddingValues(vertical = 9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "⚡ Hemen Al: ₳${formatMoney(auction.buyoutPrice)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
