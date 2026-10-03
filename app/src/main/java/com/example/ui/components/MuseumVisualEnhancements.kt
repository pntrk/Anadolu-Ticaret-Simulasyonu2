package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.GameViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AntiqueArtifact
import com.example.data.ArtifactRarity
import com.example.data.MuseumHeritageManager
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.tr
import com.example.ui.theme.isEnglishLanguage

/**
 * Üst Düzey Kompakt Ahilik Müzesi Başlık ve Küratörlük Paneli (Compact Museum Hero Banner)
 */
@Composable
fun GrandMuseumHeroBanner(
    ownedCount: Int,
    totalArtifacts: Int,
    totalPrestige: Int,
    hourlyIncome: Long,
    collectorTitle: String,
    onClose: (() -> Unit)? = null
) {
    val theme = LocalAppThemeOption.current
    val progressPct = if (totalArtifacts > 0) (ownedCount.toFloat() / totalArtifacts.toFloat()).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "banner_gold_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        color = Color(0xFF091322),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    ThemeGold.copy(alpha = glowAlpha),
                    Color(0xFF38BDF8),
                    ThemeGold.copy(alpha = glowAlpha)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Row: Icon + Title/Title Badge + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Brush.radialGradient(listOf(ThemeGold.copy(alpha = 0.35f), Color(0xFF0F172A))))
                            .border(1.dp, ThemeGold, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CurrencyText("🏛️", fontSize = 16.sp)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CurrencyText(
                                text = tr("AHİLİK MİRASI MÜZESİ", "AHILIK HERITAGE MUSEUM"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = ThemePositive.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, ThemePositive.copy(alpha = 0.7f))
                            ) {
                                CurrencyText(
                                    text = tr("AÇIK", "LIVE"),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemePositive,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        CurrencyText(
                            text = collectorTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr("Kapat", "Close"),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Compact Stat Capsules Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pill 1: Exhibits / Progress
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF101B2B),
                    border = BorderStroke(0.6.dp, Color(0xFF1E3A5F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurrencyText("🏛️ ", fontSize = 10.sp)
                        CurrencyText(
                            text = "$ownedCount/$totalArtifacts " + tr("Eser", "Items"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = Color.White
                        )
                    }
                }

                // Pill 2: Prestige
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF18180A),
                    border = BorderStroke(0.6.dp, ThemeGold.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurrencyText("⭐ ", fontSize = 10.sp)
                        CurrencyText(
                            text = "+$totalPrestige",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )
                    }
                }

                // Pill 3: Ticket Inflow
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0C2018),
                    border = BorderStroke(0.6.dp, ThemePositive.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurrencyText("🎟️ ", fontSize = 10.sp)
                        CurrencyText(
                            text = "+${formatCredit(hourlyIncome)}" + tr("/s", "/h"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemePositive
                        )
                    }
                }
            }

            // Slim 2.5dp Progress Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPct)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0284C7), ThemeGold, ThemePositive)))
                )
            }
        }
    }
}

/**
 * 3D Kadife Kaide & Işıklandırılmış Cam Vitrin Kartı (Luxury Glass Vitrine Card)
 */
@Composable
fun LuxuryGlassVitrineCard(
    artifact: AntiqueArtifact,
    isOwned: Boolean,
    onClick: () -> Unit,
    onAuctionClick: (() -> Unit)? = null
) {
    val isMasterpiece = artifact.rarity == ArtifactRarity.MASTERPIECE
    val rarityColor = Color(artifact.rarity.colorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "vitrine_glow")
    val haloPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    val cardBorder = if (isOwned) {
        if (isMasterpiece) BorderStroke(1.5.dp, rarityColor.copy(alpha = haloPulseAlpha))
        else BorderStroke(1.dp, rarityColor.copy(alpha = 0.8f))
    } else {
        BorderStroke(1.dp, Color(0xFF1E293B))
    }

    val cardBgGradient = if (isOwned) {
        Brush.verticalGradient(
            listOf(
                rarityColor.copy(alpha = if (isMasterpiece) 0.22f else 0.14f),
                Color(0xFF0F1B2E),
                Color(0xFF09101C)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0E1624),
                Color(0xFF080C14)
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(
                if (isOwned && isMasterpiece) Modifier.shadow(8.dp, RoundedCornerShape(12.dp), spotColor = rarityColor)
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = cardBorder
    ) {
        Box(
            modifier = Modifier
                .background(cardBgGradient)
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Spotlight & Pedestal Showcase Box
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isOwned) rarityColor.copy(alpha = if (isMasterpiece) haloPulseAlpha * 0.6f else 0.4f) else Color(0xFF1E293B),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            if (isOwned && isMasterpiece) 1.5.dp else 1.dp,
                            if (isOwned) rarityColor.copy(alpha = if (isMasterpiece) haloPulseAlpha else 0.6f) else Color(0xFF283548),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOwned) {
                        CurrencyText(
                            text = artifact.iconEmoji,
                            fontSize = 42.sp,
                            modifier = Modifier.shadow(if (isMasterpiece) 12.dp else 4.dp, shape = CircleShape, spotColor = rarityColor)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = tr("KİLİTLİ", "LOCKED"),
                                fontSize = 8.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }

                // Official Certificate & Rarity Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val certCode = MuseumHeritageManager.getArtifactCertificateCode(artifact.id)

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = rarityColor.copy(alpha = if (isOwned) 0.25f else 0.1f),
                        border = BorderStroke(0.6.dp, rarityColor.copy(alpha = if (isOwned) 1f else 0.5f))
                    ) {
                        CurrencyText(
                            text = "${artifact.rarity.badgeEmoji} ${artifact.rarity.getLocalizedName(isEnglishLanguage())}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = rarityColor.copy(alpha = if (isOwned) 1f else 0.8f),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0369A1).copy(alpha = 0.2f),
                        border = BorderStroke(0.6.dp, Color(0xFF38BDF8))
                    ) {
                        CurrencyText(
                            text = "$certCode • 1/1",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFBAE6FD),
                            fontFamily = RobotoMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Name & Era
                CurrencyText(
                    text = artifact.getLocalizedName(isEnglishLanguage()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isOwned) Color.White else Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                CurrencyText(
                    text = "📍 ${artifact.getLocalizedOriginCity(isEnglishLanguage())} • ${artifact.getLocalizedEra(isEnglishLanguage())}",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Passive Buff Tag
                val buff = com.example.data.ArtifactBuffRegistry.buffs.find { it.artifactId == artifact.artifactId }
                if (buff != null) {
                    Surface(
                        color = Color(0xFF1E2C42),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CurrencyText(
                            text = "✨ ${buff.loreDescription}",
                            fontSize = 8.5.sp,
                            color = Color(0xFF7DD3FC),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Revenue & Prestige Footer
                if (isOwned) {
                    HorizontalDivider(color = Color(0xFF1E3A5F), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText("⭐ +${artifact.prestigeScore}", fontSize = 9.5.sp, color = ThemeGold, fontWeight = FontWeight.Black)
                        CurrencyText("+${formatCredit(artifact.hourlyVisitorIncome)}" + tr("/s", "/h"), fontSize = 9.5.sp, color = ThemePositive, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
                    }
                    val dailyGems = (artifact.baseValue / 15_000_000_000L).toInt().coerceIn(1, 10)
                    CurrencyText("+💎$dailyGems ${tr("Günlük Elmas", "Daily Gems")}", fontSize = 9.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Black)
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF142033),
                        border = BorderStroke(0.5.dp, Color(0xFF283B55)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CurrencyText(
                            text = tr("Müzayededen Kazanılabilir", "Available via Auction"),
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Müze Ziyaretçi Defteri & VIP Ziyaretçi Hasılat Kayıtları (Visitor Ledger Panel)
 */
@Composable
fun MuseumVisitorLedgerPanel(
    unclaimedRevenue: Long,
    hourlyIncome: Long,
    totalInspections: Int,
    onClaimClick: () -> Unit
) {
    val theme = LocalAppThemeOption.current

    // Realistic VIP Visitor log
    val visitorLogItems = remember(totalInspections) {
        listOf(
            VisitorLogItem("🏛️ Selçuklu Sanayi Konsorsiyumu", "Konya", "+₳145.000", "12 dk önce"),
            VisitorLogItem("👑 Anadolu Taşımacılık Holding", "Ankara", "+₳120.000", "28 dk önce"),
            VisitorLogItem("⚡ Marmara İhracat İttifakı", "Bursa", "+₳95.000", "45 dk önce"),
            VisitorLogItem("🌍 Küresel Ahilik Heyeti", "Kayseri", "+₳230.000", "1 saat önce")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Vault Display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0B192C),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemeGold, ThemePositive)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrencyText(
                    text = tr("🎟️ BİRİKEN MÜZE BİLET GELİRİ", "🎟️ ACCUMULATED TICKET REVENUE"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemeNeonCyan,
                    fontFamily = RobotoMonoFontFamily
                )

                CurrencyText(
                    text = formatCredit(unclaimedRevenue),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemePositive,
                    fontFamily = RobotoMonoFontFamily
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyText(
                        text = "⚡ " + tr("Saatlik Akış: ", "Hourly Flow: ") + formatCredit(hourlyIncome) + "/saat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    CurrencyText(
                        text = "👁️ $totalInspections " + tr("İnceleme", "Views"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onClaimClick,
                    enabled = unclaimedRevenue > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePositive,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color.Gray
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        CurrencyText(
                            text = if (unclaimedRevenue > 0L) tr("HASILATI KASAYA AKTAR", "COLLECT REVENUE TO VAULT")
                            else tr("BİRİKEN HASILAT YOK", "NO REVENUE ACCUMULATED"),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }
        }

        // Live VIP Visitor Activity Feed
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F1B2C),
            border = BorderStroke(1.dp, Color(0xFF1E3A5F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyText("📖", fontSize = 16.sp)
                        CurrencyText(
                            text = tr("CANLI ZİYARETÇİ DEFTERİ & HASILAT AKIŞI", "LIVE VISITOR LOG & REVENUE INFLOW"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = ThemePositive.copy(alpha = 0.15f)
                    ) {
                        CurrencyText(
                            text = "CANLI AKIŞ",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = ThemePositive,
                            fontFamily = RobotoMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)

                visitorLogItems.forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            CurrencyText(
                                text = log.holdingName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            CurrencyText(
                                text = "📍 ${log.city} • ${log.timeAgo}",
                                fontSize = 9.5.sp,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThemePositive.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, ThemePositive.copy(alpha = 0.5f))
                        ) {
                            CurrencyText(
                                text = log.ticketAmount,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = ThemePositive,
                                fontFamily = RobotoMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class VisitorLogItem(
    val holdingName: String,
    val city: String,
    val ticketAmount: String,
    val timeAgo: String
)

/**
 * Vitrin Arama ve Dönem / Sahiplik Filtreleme Barı
 */
@Composable
fun MuseumVitrineFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: Int,
    onFilterSelect: (Int) -> Unit,
    ownedCount: Int,
    totalCount: Int
) {
    val theme = LocalAppThemeOption.current
    val filters = listOf(
        0 to tr("TÜMÜ ($totalCount)", "ALL ($totalCount)"),
        1 to tr("SERGİLENENLER ($ownedCount)", "EXHIBITED ($ownedCount)"),
        2 to tr("KİLİTLİ (${totalCount - ownedCount})", "LOCKED (${totalCount - ownedCount})"),
        3 to tr("SELÇUKLU", "SELJUK"),
        4 to tr("OSMANLI", "OTTOMAN"),
        5 to tr("ANTİK ÇAĞ", "ANCIENT")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = {
                CurrencyText(
                    text = tr("Eser adı, köken şehir (Konya, Bursa...) veya dönem ara...", "Search artifact, origin city or era..."),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ThemeGold,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F1A2A),
                unfocusedContainerColor = Color(0xFF091220),
                focusedBorderColor = ThemeGold,
                unfocusedBorderColor = Color(0xFF1E3A5F),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filters.forEach { (filterId, label) ->
                val isSelected = selectedFilter == filterId
                Surface(
                    onClick = { onFilterSelect(filterId) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) ThemeGold else Color(0xFF101C2E),
                    border = BorderStroke(1.dp, if (isSelected) ThemeGold else Color(0xFF1E3A5F))
                ) {
                    CurrencyText(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (isSelected) Color(0xFF0F172A) else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Müze Salonları & Kanat Geliştirme Bölümü (Pavilion Wings Section)
 */
@Composable
fun MuseumWingPavilionsSection(
    viewModel: GameViewModel,
    playerCash: Long,
    onWingUpgraded: () -> Unit
) {
    val context = LocalContext.current
    val theme = LocalAppThemeOption.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0C1728),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(ThemeGold, Color(0xFF38BDF8))))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CurrencyText("🏛️", fontSize = 28.sp)
                Column {
                    CurrencyText(
                        text = tr("MÜZE KANATLARI & SERGİ SARAYLARI", "MUSEUM WINGS & EXHIBIT PAVILIONS"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold
                    )
                    CurrencyText(
                        text = tr("Müze salonlarınızı restore edip genişleterek ziyaretçi çekiciliğini, bilet gelirlerini ve holdinge sağlanan kalıcı avantajları katlayın.", "Restore and expand your museum pavilions to multiply visitor appeal, ticket revenue, and permanent holding bonuses."),
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // List of Wings
        MuseumHeritageManager.allWings.forEach { wing ->
            val currentLevel = MuseumHeritageManager.getWingLevel(context, wing.id)
            val isMaxLevel = currentLevel >= wing.maxLevel
            val upgradeCost = MuseumHeritageManager.getWingUpgradeCost(wing, currentLevel)
            val canAfford = playerCash >= upgradeCost && !isMaxLevel

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (currentLevel > 0) Color(0xFF0E1B2E) else Color(0xFF0A1320),
                border = BorderStroke(
                    1.dp,
                    if (currentLevel > 0) ThemeGold.copy(alpha = 0.6f) else Color(0xFF1E3A5F)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyText(wing.iconEmoji, fontSize = 22.sp)
                            Column {
                                CurrencyText(
                                    text = tr(wing.nameTr, wing.nameEn),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    (1..wing.maxLevel).forEach { starIdx ->
                                        CurrencyText(
                                            text = if (starIdx <= currentLevel) "⭐" else "☆",
                                            fontSize = 11.sp,
                                            color = if (starIdx <= currentLevel) ThemeGold else Color.Gray
                                        )
                                    }
                                    CurrencyText(
                                        text = if (currentLevel > 0) tr("Seviye $currentLevel / ${wing.maxLevel}", "Level $currentLevel / ${wing.maxLevel}")
                                        else tr("Henüz Açılmadı", "Not Restored"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentLevel > 0) ThemeGold else Color.Gray,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }

                        if (isMaxLevel) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemePositive.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, ThemePositive)
                            ) {
                                CurrencyText(
                                    text = tr("ZİRVE SEVİYE", "MAX LEVEL"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemePositive,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    CurrencyText(
                        text = tr(wing.descriptionTr, wing.descriptionEn),
                        fontSize = 10.5.sp,
                        color = Color.LightGray,
                        lineHeight = 14.sp
                    )

                    // Active Perk Badge
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF13233A),
                        border = BorderStroke(0.5.dp, Color(0xFF0284C7))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurrencyText("⚡", fontSize = 12.sp)
                            CurrencyText(
                                text = tr(wing.bonusSummaryTr, wing.bonusSummaryEn),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7DD3FC),
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }

                    // Upgrade Action
                    if (!isMaxLevel) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                CurrencyText(
                                    text = tr("Restorasyon & Genişletme Bedeli:", "Restoration Cost:"),
                                    fontSize = 9.5.sp,
                                    color = Color.Gray
                                )
                                CurrencyText(
                                    text = formatCredit(upgradeCost),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = if (canAfford) ThemePositive else Color(0xFFEF4444)
                                )
                            }

                            Button(
                                onClick = {
                                    val success = viewModel.deductMoneyDirectly(upgradeCost)
                                    if (success) {
                                        MuseumHeritageManager.upgradeWing(context, wing.id)
                                        SmartNotificationManager.show(
                                            message = "🏛️ ${wing.nameTr} Seviye ${currentLevel + 1} seviyesine yükseltildi!",
                                            enMessage = "🏛️ ${wing.nameEn} upgraded to Level ${currentLevel + 1}!",
                                            type = NotificationType.SUCCESS
                                        )
                                        onWingUpgraded()
                                    } else {
                                        SmartNotificationManager.show(
                                            message = "Yetersiz bakiye! Gereken: ${formatCredit(upgradeCost)}",
                                            enMessage = "Insufficient funds! Required: ${formatCredit(upgradeCost)}",
                                            type = NotificationType.ALERT
                                        )
                                    }
                                },
                                enabled = canAfford,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeGold,
                                    contentColor = Color(0xFF0F172A),
                                    disabledContainerColor = Color(0xFF1E293B),
                                    disabledContentColor = Color.Gray
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                CurrencyText(
                                    text = tr("YÜKSELT (Seviye ${currentLevel + 1})", "UPGRADE (Level ${currentLevel + 1})"),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1/1 Tescilli Kültür Mirası Beratı (Imperial Wax Seal Certificate)
 */
@Composable
fun ImperialArtifactCertificateCard(
    artifact: AntiqueArtifact,
    certCode: String
) {
    val rarityColor = Color(artifact.rarity.colorHex)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0B1729),
        border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(ThemeGold, rarityColor))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyText("📜", fontSize = 16.sp)
                    Column {
                        CurrencyText(
                            text = tr("T.C. VAKIFLAR & KÜLTÜR VARLIKLARI BAŞKANLIĞI", "T.R. DIRECTORATE OF FOUNDATIONS & HERITAGE"),
                            color = Color(0xFF38BDF8),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        CurrencyText(
                            text = tr("1/1 TESCİLLİ AHİLİK MİRASI ŞAHESERİ", "1/1 REGISTERED AHILIK HERITAGE MASTERPIECE"),
                            color = ThemeGold,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }

                // Wax Seal Stamp
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF881337))
                        .border(1.2.dp, ThemeGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CurrencyText("AHİ", fontSize = 9.sp, fontWeight = FontWeight.Black, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                }
            }

            HorizontalDivider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CurrencyText(tr("Sertifika Seri No:", "Certificate Serial:"), color = Color.Gray, fontSize = 9.sp, fontFamily = RobotoMonoFontFamily)
                CurrencyText(certCode, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CurrencyText(tr("Sicil Durumu:", "Registry Status:"), color = Color.Gray, fontSize = 9.sp, fontFamily = RobotoMonoFontFamily)
                CurrencyText(tr("TEK NÜSHA • 1/1 MİRAS", "UNIQUE COPY • 1/1 HERITAGE"), color = ThemePositive, fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = RobotoMonoFontFamily)
            }
        }
    }
}

/**
 * Küratör Sesli Rehberi ve Tarihsel Kronoloji Kartı (Curator Audio Chronicle)
 */
@Composable
fun CuratorAudioChronicle(
    artifact: AntiqueArtifact
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val waveHeight3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0A1424),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyText("🎙️", fontSize = 16.sp)
                    CurrencyText(
                        text = tr("KÜRATÖR SESLİ REHBERİ & TARİHSEL BAĞLAM", "CURATOR AUDIO GUIDE & HISTORICAL CONTEXT"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold
                    )
                }

                // Animated Sound Wave
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(3.dp).height(waveHeight1.dp).background(ThemeGold, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(3.dp).height(waveHeight2.dp).background(Color(0xFF38BDF8), RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(3.dp).height(waveHeight3.dp).background(ThemePositive, RoundedCornerShape(1.dp)))
                }
            }

            CurrencyText(
                text = artifact.getLocalizedHistoricalLore(isEnglishLanguage()),
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Şirket Güçlendirmesi Odak Kartı (Artifact Holding Buff Spotlight)
 */
@Composable
fun ArtifactHoldingBuffSpotlight(
    artifact: AntiqueArtifact
) {
    val buff = com.example.data.ArtifactBuffRegistry.buffs.find { it.artifactId == artifact.id } ?: return

    val (label, icon, desc) = when (buff.buffType) {
        com.example.data.ArtifactBuffType.LOAN_INTEREST_DISCOUNT -> Triple(
            "Kredi Faiz İndirimi -%${(buff.buffValue * 100).toInt()}",
            "🏦",
            "Banka kredi maliyetlerinizi kalıcı olarak hafifletir."
        )
        com.example.data.ArtifactBuffType.DEPOSIT_INTEREST_BONUS -> Triple(
            "Mevduat Getiri Bonusu +%${String.format(java.util.Locale.US, "%.1f", buff.buffValue * 100)}",
            "💰",
            "Günlük banka mevduat faiz kazancınızı artırır."
        )
        com.example.data.ArtifactBuffType.LOGISTICS_COST_DISCOUNT -> Triple(
            "Lojistik Maliyet İndirimi -%${(buff.buffValue * 100).toInt()}",
            "🚚",
            "Şehirlerarası kargo ve sevkiyat giderlerini düşürür."
        )
        com.example.data.ArtifactBuffType.LOGISTICS_SPEED_BONUS -> Triple(
            "Kervan & Lojistik Hız +%${(buff.buffValue * 100).toInt()}",
            "⚡",
            "Sevkiyat sürelerini kısaltır, teslimatları hızlandırır."
        )
        com.example.data.ArtifactBuffType.CONSTRUCTION_SPEED_BONUS -> Triple(
            "Tesis İnşaat Hızı +%${(buff.buffValue * 100).toInt()}",
            "🏗️",
            "Yeni fabrika ve tesislerin tamamlanma süresini azaltır."
        )
        com.example.data.ArtifactBuffType.UPGRADE_COST_DISCOUNT -> Triple(
            "Yükseltme İndirimi -%${(buff.buffValue * 100).toInt()}",
            "⚙️",
            "Tesis seviye artırım maliyetlerinde tasarruf sağlar."
        )
        com.example.data.ArtifactBuffType.WEAR_LEVEL_REDUCTION -> Triple(
            "Tesis Aşınma Azaltımı -%${(buff.buffValue * 100).toInt()}",
            "🛡️",
            "Makinelerin bakım ihtiyacını ve amortismanını geciktirir."
        )
        com.example.data.ArtifactBuffType.MAINTENANCE_COST_DISCOUNT -> Triple(
            "Bakım Masraf İndirimi -%${(buff.buffValue * 100).toInt()}",
            "🔧",
            "Rutin tesis bakım faturalarını düşürür."
        )
        com.example.data.ArtifactBuffType.TIER1_PRODUCTION_BONUS -> Triple(
            "Hammadde Üretim Artışı +%${(buff.buffValue * 100).toInt()}",
            "🌾",
            "Temel tarım ve madencilik çıktılarını artırır."
        )
        com.example.data.ArtifactBuffType.TIER4_PRODUCTION_BONUS -> Triple(
            "Yüksek Teknoloji Çıktısı +%${(buff.buffValue * 100).toInt()}",
            "🚀",
            "İleri sanayi ve savunma ürünlerinin üretim verimini katlar."
        )
        com.example.data.ArtifactBuffType.BORSA_SELL_BONUS -> Triple(
            "Borsa Satış Primi +%${(buff.buffValue * 100).toInt()}",
            "📈",
            "Emtia ve hisse satışlarında ekstra gelir sağlar."
        )
        com.example.data.ArtifactBuffType.BORSA_BUY_DISCOUNT -> Triple(
            "Borsa Alım İndirimi -%${(buff.buffValue * 100).toInt()}",
            "📉",
            "Borsadan mal tedariğinde maliyet avantajı tanır."
        )
        com.example.data.ArtifactBuffType.MANAGER_SALARY_DISCOUNT -> Triple(
            "Yönetici Maaş İndirimi -%${(buff.buffValue * 100).toInt()}",
            "👔",
            "Şirket idari bordro giderlerini optimize eder."
        )
        com.example.data.ArtifactBuffType.RD_RESEARCH_SPEED -> Triple(
            "Ar-Ge Araştırma Hızı +%${(buff.buffValue * 100).toInt()}",
            "🔬",
            "Teknoloji ağacındaki kilitleri daha süratli açar."
        )
        com.example.data.ArtifactBuffType.CONSORTIUM_PRESTIGE_BONUS -> Triple(
            "Konsorsiyum Prestiji +%${(buff.buffValue * 100).toInt()}",
            "👑",
            "Liderlik sıralaması ve ihale kazanma şansını artırır."
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF162512),
        border = BorderStroke(1.dp, ThemePositive)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurrencyText(icon, fontSize = 20.sp)
            Column {
                CurrencyText(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = RobotoMonoFontFamily,
                    color = ThemePositive
                )
                CurrencyText(
                    text = desc,
                    fontSize = 9.5.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

