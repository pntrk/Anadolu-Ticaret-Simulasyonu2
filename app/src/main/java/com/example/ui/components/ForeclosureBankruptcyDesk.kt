package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.utils.HapticManager
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay

/**
 * Banka İflas Masası & Tasfiye Müzayede Masası Arayüzü.
 * Temerrüde düşen veya iflas eden şirketlerin hacizli tesislerinin açık artırma ile
 * tasfiye edildiği, dinamik çekiç / son çağrı mekanizmasına sahip müzayede merkezi.
 */
@Composable
fun ForeclosureBankruptcyDesk(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val auctions by ForeclosureManager.auctionsState.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()

    // Düzenli olarak sonuçlanan ihaleleri kontrol et ve kazanılan tesisleri devret
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            viewModel.settleForeclosureAuctions()
        }
    }

    val pId = player?.id ?: "local_player"
    val pName = player?.name ?: ""

    // Aktif (henüz sonuçlanmamış) ihaleler
    val activeAuctions = remember(auctions) {
        auctions.filter { !it.isSettled }
    }

    val userLeadingCount = remember(activeAuctions, pId, pName) {
        activeAuctions.count {
            (it.highestBidderId != null && (it.highestBidderId == pId || it.highestBidderId == "local_player")) ||
            (pName.isNotBlank() && it.highestBidderName == pName)
        }
    }

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: Tümü, 1: Lider Olduklarım, 2: Son Çağrı

    val filteredAuctions = remember(activeAuctions, selectedFilterTab, pId, pName) {
        val now = System.currentTimeMillis()
        when (selectedFilterTab) {
            1 -> activeAuctions.filter {
                (it.highestBidderId != null && (it.highestBidderId == pId || it.highestBidderId == "local_player")) ||
                (pName.isNotBlank() && it.highestBidderName == pName)
            }
            2 -> activeAuctions.filter { (it.endsAtMs - now) in 1..30_000L }
            else -> activeAuctions
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================= BAŞLIK & ÖZET PANELİ =================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(ThemeGold.copy(alpha = 0.85f), ThemeNeonCyan.copy(alpha = 0.65f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.5.dp, ThemeGold),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Gavel,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "İFLAS MASASI & TASFİYE MERKEZİ",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = ThemeGold,
                                fontFamily = RobotoMonoFontFamily
                            )
                            Text(
                                text = "Mali temerrüde düşen şirketlerin hacizli tesisleri %50 indirimle açık artırmada.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Taze İhale Ara / Yenile Butonu
                    IconButton(
                        onClick = {
                            HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                            viewModel.settleForeclosureAuctions()
                            ForeclosureManager.generateBotAuctions()
                            ForeclosureManager.processBotBiddingCycle()
                            SmartNotificationManager.show("Tasfiye masası güncellendi.", NotificationType.INFO)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_foreclosure_auctions_button")
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Yenile",
                            tint = ThemeNeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bilgilendirme Rozetleri Şeridi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kasa Nakit
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Kasanız", fontSize = 9.sp, color = Color.Gray)
                                Text("₳${formatMoney(player?.money ?: 0L)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemePositive, fontFamily = RobotoMonoFontFamily)
                            }
                        }
                    }

                    // Aktif İhale
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Aktif İhale", fontSize = 9.sp, color = Color.Gray)
                                Text("${activeAuctions.size} Tesis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = RobotoMonoFontFamily)
                            }
                        }
                    }

                    // Liderlik
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (userLeadingCount > 0) ThemePositive.copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = BorderStroke(0.8.dp, if (userLeadingCount > 0) ThemePositive else Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = if (userLeadingCount > 0) ThemePositive else Color.Gray, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Lidersiniz", fontSize = 9.sp, color = Color.Gray)
                                Text("$userLeadingCount İhalede", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (userLeadingCount > 0) ThemePositive else Color.LightGray, fontFamily = RobotoMonoFontFamily)
                            }
                        }
                    }
                }
            }
        }

        // ================= FİLTRELEME SEKMELERİ =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                "Tümü (${activeAuctions.size})",
                "👑 Lider (${userLeadingCount})",
                "⚡ Son Çağrı"
            )
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedFilterTab == index
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) ThemeGold.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) ThemeGold else Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                            selectedFilterTab = index
                        }
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ThemeGold else Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        // ================= İHALE LİSTESİ VEYA BOŞ DURUM =================
        if (filteredAuctions.isEmpty()) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
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
                    Icon(
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        tint = ThemePositive,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedFilterTab == 1) "Şu Anda Lider Olduğunuz İhale Yok" else "Şu Anda İcralık Tesis Bulunmuyor",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedFilterTab == 1)
                            "Aktif ihalelere pey sürerek açık artırmada liderliği ele geçirebilirsiniz."
                        else
                            "Mali darboğaza giren şirketlerin tesisleri otomatik olarak burada hacizli satışa çıkarılır.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                            selectedFilterTab = 0
                            ForeclosureManager.generateBotAuctions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("search_fresh_foreclosure_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Taze İhale Havuzunu Tara", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }
        } else {
            filteredAuctions.forEach { auction ->
                ForeclosureAuctionCard(
                    auction = auction,
                    playerMoney = player?.money ?: 0L,
                    playerId = player?.id,
                    playerName = player?.name,
                    onBuyout = { viewModel.buyoutForeclosedFacility(auction.id) },
                    onPlaceBid = { bidAmount -> viewModel.placeForeclosureBid(auction.id, bidAmount) },
                    onFinalizeAuction = { viewModel.finalizeForeclosureAuction(auction.id) }
                )
            }
        }
    }
}

/**
 * Tek bir icralık tesis kartı.
 * Gerçek zamanlı çekiç sayacı, min pey koruması ve anında devir mekanizması barındırır.
 */
@Composable
fun ForeclosureAuctionCard(
    auction: ForeclosureAuction,
    playerMoney: Long,
    playerId: String? = null,
    playerName: String? = null,
    onBuyout: () -> Unit,
    onPlaceBid: (Long) -> Unit,
    onFinalizeAuction: () -> Unit
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
    val isExpired = remainingMs <= 0L
    val isGavelCountDown = remainingMs in 1..25000L // Son 25 saniye çekiç sayacı

    val timeFormatted = if (isExpired) "00:00" else String.format("%02d:%02d", remainingMinutes, remainingSeconds)

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

    // Kullanıcının en yüksek teklif verip vermediği kontrolü
    val isUserHighestBidder = remember(auction.highestBidderId, auction.highestBidderName, playerId, playerName) {
        (playerId != null && auction.highestBidderId == playerId) ||
        (auction.highestBidderId == "local_player") ||
        (playerId == "local_player" && auction.highestBidderId != null) ||
        (playerName != null && playerName.isNotBlank() && auction.highestBidderName == playerName)
    }

    // Minimum geçerli teklif hesaplaması:
    // Eğer henüz kimse pey sürmediyse (highestBidderId == null), başlangıç teklifi geçerlidir.
    // Eğer biri teklif vermişse, en az (currentHighestBid + 5.000 veya %3) geçerlidir.
    val minIncrement = maxOf(5_000L, (auction.buyoutPrice * 0.03).toLong())
    val nextMinBid = remember(auction.highestBidderId, auction.currentHighestBid, auction.startingBid, minIncrement) {
        if (auction.highestBidderId == null) {
            auction.startingBid
        } else {
            (auction.currentHighestBid + minIncrement).coerceAtMost(auction.buyoutPrice)
        }
    }

    // Seçilen pey tutarı state'i
    var selectedBidAmount by remember(auction.id, auction.currentHighestBid, nextMinBid) {
        mutableLongStateOf(nextMinBid)
    }

    // Eğer teklif başkası tarafından geçildiyse seçilen teklifi otomatik asgari geçerli değere yükselt
    LaunchedEffect(nextMinBid) {
        if (selectedBidAmount < nextMinBid) {
            selectedBidAmount = nextMinBid
        }
    }

    val hasEnoughMoneyForBid = playerMoney >= selectedBidAmount
    val hasEnoughMoneyForBuyout = playerMoney >= auction.buyoutPrice

    val estimatedFactoryMarketValue = remember(product, auction.level) {
        val base = product?.facilityCost ?: (auction.buyoutPrice * 2)
        base * auction.level
    }

    // Kart çerçeve rengi
    val cardBorderColor = when {
        isUserHighestBidder -> ThemePositive
        isGavelCountDown -> Color(0xFFEF4444)
        else -> brandColor.copy(alpha = 0.5f)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isUserHighestBidder) 1.8.dp else 1.dp, cardBorderColor, RoundedCornerShape(12.dp))
            .testTag("foreclosure_card_${auction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isUserHighestBidder) Color(0xFF0C1F28) else Color(0xFF131D33)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ================= ÜST ŞERİT: TESİS BİLGİSİ VE SAYAÇ =================
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
                    // Tesis / Ürün İkon Rozeti
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = brandColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.2.dp, brandColor.copy(alpha = 0.8f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeGold.copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, ThemeGold.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "★ Seviye ${auction.level}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "📍 $cityDisplayName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeNeonCyan
                            )
                        }
                    }
                }

                // Geri Sayım Sayacı Rozeti
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isExpired -> Color(0xFF334155)
                        isGavelCountDown -> Color(0xFFEF4444).copy(alpha = 0.22f)
                        else -> Color(0xFF1E293B)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isExpired -> Color.Gray
                            isGavelCountDown -> Color(0xFFEF4444)
                            else -> ThemeGold.copy(alpha = 0.8f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGavelCountDown) Icons.Rounded.Gavel else Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (isGavelCountDown) Color(0xFFEF4444) else ThemeGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isGavelCountDown) "⚡ $timeFormatted" else timeFormatted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isGavelCountDown) Color(0xFFEF4444) else ThemeGold,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }

            // ================= İCRA GEREKÇESİ & DEĞER KARŞILAŞTIRMASI =================
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0B1322),
                border = BorderStroke(0.8.dp, Color(0xFF1E293B)),
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
                        Text(
                            text = "Haciz Sebebi: ${auction.reason}",
                            fontSize = 10.sp,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThemePositive.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "%50+ İNDİRİMLİ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = ThemePositive,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                    // Fiyat Karşılaştırma Şeridi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Piyasa Değeri:", fontSize = 9.5.sp, color = Color.Gray)
                            Text("₳${formatMoney(estimatedFactoryMarketValue)}", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold, fontFamily = RobotoMonoFontFamily)
                        }
                        Column {
                            Text("Taban Pey:", fontSize = 9.5.sp, color = Color.Gray)
                            Text("₳${formatMoney(auction.startingBid)}", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold, fontFamily = RobotoMonoFontFamily)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tasfiye / Hemen Al:", fontSize = 9.5.sp, color = Color.Gray)
                            Text("₳${formatMoney(auction.buyoutPrice)}", fontSize = 11.5.sp, color = ThemeGold, fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
                        }
                    }
                }
            }

            // ================= PEY & LİDERLİK BİLGİ KUTUSU =================
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isUserHighestBidder) ThemePositive.copy(alpha = 0.12f) else Color(0xFF0E1A2D),
                border = BorderStroke(1.dp, if (isUserHighestBidder) ThemePositive.copy(alpha = 0.8f) else Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mevcut En Yüksek Pey:",
                                fontSize = 10.sp,
                                color = if (isUserHighestBidder) ThemePositive else Color(0xFF94A3B8)
                            )
                            Text(
                                text = "₳${formatMoney(auction.currentHighestBid)}",
                                fontSize = 14.5.sp,
                                color = if (isUserHighestBidder) ThemePositive else ThemeNeonCyan,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Lider Durumu:",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = when {
                                    isUserHighestBidder -> "🏆 SİZ (LİDERSİNİZ)"
                                    auction.highestBidderName != null -> "👤 ${auction.highestBidderName}"
                                    else -> "⚪ Henüz Teklif Yok"
                                },
                                fontSize = 11.5.sp,
                                color = if (isUserHighestBidder) ThemePositive else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isUserHighestBidder) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isGavelCountDown)
                                    "Çekiç sayacı devrede (${remainingSeconds}s)! 'Çekici Vur' ile anında portföyünüze alabilirsiniz."
                                else
                                    "En yüksek pey sizin adınıza kayıtlı! Süre sonunda tesis kasanızdan kesilip teslim edilecektir.",
                                fontSize = 10.sp,
                                color = ThemePositive,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ================= PEY MİKTARI SEÇİCİ & HIZLI ARTIRMA =================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sürülecek Pey:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )

                    // Stepper (- ve + butonları)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.8.dp, Color(0xFF334155)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                                    selectedBidAmount = (selectedBidAmount - minIncrement).coerceAtLeast(nextMinBid)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Azalt", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        Text(
                            text = "₳${formatMoney(selectedBidAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasEnoughMoneyForBid) ThemeNeonCyan else Color(0xFFEF4444),
                            fontFamily = RobotoMonoFontFamily
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.8.dp, Color(0xFF334155)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                                    selectedBidAmount = (selectedBidAmount + minIncrement).coerceAtMost(auction.buyoutPrice)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Artır", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Hızlı Pey Çipleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Min Geçerli Pey Çipi
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedBidAmount == nextMinBid) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B),
                        border = BorderStroke(0.8.dp, if (selectedBidAmount == nextMinBid) ThemeNeonCyan else Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                                selectedBidAmount = nextMinBid
                            }
                    ) {
                        Text(
                            text = "Min Pey",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedBidAmount == nextMinBid) ThemeNeonCyan else Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }

                    val quickIncrements = listOf(10_000L, 25_000L, 50_000L, 100_000L)
                    quickIncrements.forEach { inc ->
                        val targetAmount = (auction.currentHighestBid + inc).coerceAtMost(auction.buyoutPrice)
                        val isSelected = selectedBidAmount == targetAmount
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = BorderStroke(0.8.dp, if (isSelected) ThemeNeonCyan else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    HapticManager.performHaptic(HapticManager.HapticType.LIGHT_CLICK)
                                    selectedBidAmount = targetAmount
                                }
                        ) {
                            Text(
                                text = "+${formatMoney(inc)}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ThemeNeonCyan else Color.LightGray,
                                textAlign = TextAlign.Center,
                                fontFamily = RobotoMonoFontFamily,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Yetersiz Bakiye Uyarısı
            if (!hasEnoughMoneyForBid) {
                Text(
                    text = "⚠️ Kasanızda yeterli bakiye yok! (Gerekli: ₳${formatMoney(selectedBidAmount)}, Eksik: ₳${formatMoney(selectedBidAmount - playerMoney)})",
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Medium
                )
            }

            // ================= İŞLEM BUTONLARI: [🔨 PEY SÜR / ÇEKİCİ VUR] ve [⚡ HEMEN AL] =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. ANA PEY / ÇEKİÇ BUTONU
                if (isUserHighestBidder) {
                    // Lider oyuncu için hızlı tamamlama ("Çekici Vur & Tesisini Al")
                    Button(
                        onClick = {
                            HapticManager.performHaptic(HapticManager.HapticType.CONSORTIUM_APPROVAL)
                            onFinalizeAuction()
                        },
                        enabled = !isExpired && playerMoney >= auction.currentHighestBid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemePositive,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finalize_auction_button_${auction.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Çekici Vur: ₳${formatMoney(auction.currentHighestBid)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    // Lider olmayan oyuncu için pey sürme
                    Button(
                        onClick = {
                            HapticManager.performHaptic(HapticManager.HapticType.BUY_SELL)
                            onPlaceBid(selectedBidAmount.coerceAtLeast(nextMinBid))
                        },
                        enabled = !isExpired && hasEnoughMoneyForBid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasEnoughMoneyForBid) ThemeNeonCyan else Color(0xFF334155),
                            contentColor = if (hasEnoughMoneyForBid) Color(0xFF002229) else Color.Gray
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("place_bid_button_${auction.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (!hasEnoughMoneyForBid) "Yetersiz Bakiye" else "Pey Sür: ₳${formatMoney(selectedBidAmount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. HEMEN AL BUTONU (TASFİYE SATIŞI)
                Button(
                    onClick = {
                        HapticManager.performHaptic(HapticManager.HapticType.CONSORTIUM_APPROVAL)
                        onBuyout()
                    },
                    enabled = !isExpired && hasEnoughMoneyForBuyout,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasEnoughMoneyForBuyout) ThemeGold else Color(0xFF334155),
                        contentColor = if (hasEnoughMoneyForBuyout) Color(0xFF241600) else Color.Gray
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("buyout_foreclosure_button_${auction.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Hemen Al: ₳${formatMoney(auction.buyoutPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
