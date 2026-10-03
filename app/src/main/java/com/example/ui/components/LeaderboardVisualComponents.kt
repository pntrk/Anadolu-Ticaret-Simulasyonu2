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
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OnlinePlayer
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.tr

/**
 * 3D Şampiyonlar Kürsüsü (Olympic-style Top 3 Podium)
 * 1. Ortada (En Yüksek - Altın), 2. Solda (Gümüş), 3. Sağda (Bronz)
 */
@Composable
fun LeaderboardPodiumSection(
    topThreePlayers: List<OnlinePlayer>,
    myPlayerId: String? = null,
    onPlayerClick: (OnlinePlayer) -> Unit
) {
    if (topThreePlayers.isEmpty()) return

    val theme = LocalAppThemeOption.current
    val p1 = topThreePlayers.getOrNull(0)
    val p2 = topThreePlayers.getOrNull(1)
    val p3 = topThreePlayers.getOrNull(2)

    // Breathing glow animation for Champion
    val infiniteTransition = rememberInfiniteTransition(label = "podium_glow")
    val championGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = if (theme.isDark) Color(0xFF0B1424) else Color(0xFFF4F7FC),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFCD7F32), ThemeGold, Color(0xFFB0BEC5)))),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = ThemeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    CurrencyText(
                        text = tr("ZİRVE KÜRSÜSÜ (ŞAMPİYONLAR)", "CHAMPIONS PODIUM"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ThemeGold.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, ThemeGold.copy(alpha = 0.5f))
                ) {
                    CurrencyText(
                        text = tr("AYLIK ELMAS ÖDÜLLÜ", "MONTHLY DIAMOND PRIZES"),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Podium Columns: [Rank 2 (Silver), Rank 1 (Gold), Rank 3 (Bronze)]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // RANK 2 (SILVER - LEFT)
                PodiumColumn(
                    modifier = Modifier.weight(1f),
                    player = p2,
                    rank = 2,
                    pedestalHeight = 90.dp,
                    badgeText = "2. 1000 💎",
                    accentColor = Color(0xFFB0BEC5),
                    containerColor = if (theme.isDark) Color(0xFF16202E) else Color(0xFFE2E8F0),
                    isMe = p2?.id == myPlayerId,
                    onClick = { p2?.let { onPlayerClick(it) } }
                )

                // RANK 1 (GOLD - CENTER - HIGHEST)
                PodiumColumn(
                    modifier = Modifier.weight(1.15f),
                    player = p1,
                    rank = 1,
                    pedestalHeight = 118.dp,
                    badgeText = "👑 1. 2000 💎",
                    accentColor = ThemeGold,
                    containerColor = if (theme.isDark) Color(0xFF231F0A) else Color(0xFFFFF9E6),
                    isMe = p1?.id == myPlayerId,
                    crownGlowAlpha = championGlowAlpha,
                    onClick = { p1?.let { onPlayerClick(it) } }
                )

                // RANK 3 (BRONZE - RIGHT)
                PodiumColumn(
                    modifier = Modifier.weight(1f),
                    player = p3,
                    rank = 3,
                    pedestalHeight = 72.dp,
                    badgeText = "3. 500 💎",
                    accentColor = Color(0xFFCD7F32),
                    containerColor = if (theme.isDark) Color(0xFF22160F) else Color(0xFFFBEFE8),
                    isMe = p3?.id == myPlayerId,
                    onClick = { p3?.let { onPlayerClick(it) } }
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    modifier: Modifier = Modifier,
    player: OnlinePlayer?,
    rank: Int,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    badgeText: String,
    accentColor: Color,
    containerColor: Color,
    isMe: Boolean,
    crownGlowAlpha: Float = 0f,
    onClick: () -> Unit
) {
    val theme = LocalAppThemeOption.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = player != null) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (player != null) {
            // Crown / Medal Icon
            Box(contentAlignment = Alignment.Center) {
                if (rank == 1) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = crownGlowAlpha * 0.35f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (rank) {
                        1 -> CurrencyText("👑", fontSize = 18.sp)
                        2 -> CurrencyText("🥈", fontSize = 18.sp)
                        3 -> CurrencyText("🥉", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Company Name
            CurrencyText(
                text = player.companyName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = theme.textPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = if (rank == 1) 10.5.sp else 9.5.sp
            )

            // Net Worth - Clean Single Anadolu Lira Symbol
            CurrencyText(
                text = formatCredit(player.netWorth),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                fontFamily = RobotoMonoFontFamily,
                color = accentColor,
                fontSize = if (rank == 1) 11.sp else 10.sp
            )

            if (isMe) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = accentColor,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    CurrencyText(
                        text = tr("SEN", "YOU"),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        } else {
            // Empty placeholder for this rank
            CurrencyText(
                text = tr("Boş", "Empty"),
                fontSize = 9.sp,
                color = theme.textSecondaryColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // The Physical Pedestal Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = containerColor,
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CurrencyText(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = RobotoMonoFontFamily,
                    color = accentColor
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, accentColor)
                ) {
                    CurrencyText(
                        text = badgeText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lig İstatistikleri ve Ödül Havuzu Kartı
 */
@Composable
fun LeaderboardLeagueMetricsStrip(
    totalHoldingsCount: Int,
    currentMonthName: String,
    userRank: Int?
) {
    val theme = LocalAppThemeOption.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor,
        border = BorderStroke(1.dp, theme.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Total Active Holdings
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Business, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(13.dp))
                    CurrencyText(
                        text = "$totalHoldingsCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = theme.textPrimaryColor
                    )
                }
                CurrencyText(
                    text = tr("Kayıtlı Holding", "Registered"),
                    fontSize = 9.sp,
                    color = theme.textSecondaryColor
                )
            }

            Box(modifier = Modifier.width(1.dp).height(24.dp).background(theme.borderColor))

            // Diamond Prize Pool
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Diamond, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(13.dp))
                    CurrencyText(
                        text = "3.500 💎",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = ThemeGold
                    )
                }
                CurrencyText(
                    text = tr("Ödül Havuzu", "Prize Pool"),
                    fontSize = 9.sp,
                    color = theme.textSecondaryColor
                )
            }

            Box(modifier = Modifier.width(1.dp).height(24.dp).background(theme.borderColor))

            // User Rank Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = if (userRank != null && userRank <= 3) ThemeGold else ThemePositive,
                        modifier = Modifier.size(13.dp)
                    )
                    CurrencyText(
                        text = if (userRank != null && userRank > 0) "#$userRank" else "---",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (userRank != null && userRank <= 3) ThemeGold else ThemePositive
                    )
                }
                CurrencyText(
                    text = tr("Sıralamanız", "Your Rank"),
                    fontSize = 9.sp,
                    color = theme.textSecondaryColor
                )
            }
        }
    }
}

/**
 * Arama ve Hızlı Filtre Barı
 */
@Composable
fun LeaderboardSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: LeaderboardFilterOption,
    onFilterSelect: (LeaderboardFilterOption) -> Unit
) {
    val theme = LocalAppThemeOption.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                CurrencyText(
                    text = tr("Holding veya tüccar adı ara...", "Search holding or trader name..."),
                    fontSize = 11.5.sp,
                    color = theme.textSecondaryColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr("Temizle", "Clear"),
                            tint = theme.textSecondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primaryColor,
                unfocusedBorderColor = theme.borderColor,
                focusedContainerColor = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor,
                unfocusedContainerColor = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor
            )
        )

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LeaderboardFilterOption.values().forEach { option ->
                val isSelected = selectedFilter == option
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onFilterSelect(option) },
                    color = if (isSelected) theme.primaryColor else if (theme.isDark) Color(0xFF131D30) else theme.surfaceVariantColor,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) theme.primaryColor else theme.borderColor
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = option.title,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (isSelected) Color.White else theme.textSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

enum class LeaderboardFilterOption(val title: String) {
    ALL("TÜMÜ"),
    TOP_10("TOP 10"),
    REWARDS_ZONE("ÖDÜLLÜ"),
    MUSEUM_OWNERS("MÜZELİ")
}

/**
 * Kullanıcı Canlı Holding Durum Paneli (Pinned Bottom Sticky HUD)
 */
@Composable
fun LeaderboardUserStickyHud(
    rank: Int?,
    holdingName: String,
    netWorth: Long,
    gapToNextRank: Long?,
    onScrollToMe: () -> Unit
) {
    val theme = LocalAppThemeOption.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        color = if (theme.isDark) Color(0xFF0D1726) else Color(0xFFFFFFFF),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemeGold, theme.primaryColor)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User Rank & Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor.copy(alpha = 0.2f))
                        .border(1.5.dp, theme.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CurrencyText(
                        text = if (rank != null && rank > 0) "#$rank" else "---",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = theme.primaryColor
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyText(
                            text = holdingName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimaryColor
                        )
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = ThemeGold
                        ) {
                            CurrencyText(
                                text = tr("SEN", "YOU"),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Net Worth - Clean Single Symbol
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CurrencyText(
                            text = formatCredit(netWorth),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemePositive
                        )

                        if (gapToNextRank != null && gapToNextRank > 0L) {
                            CurrencyText(
                                text = "• (+${formatCredit(gapToNextRank)} " + tr("1 üste", "to next") + ")",
                                fontSize = 9.sp,
                                fontFamily = RobotoMonoFontFamily,
                                color = theme.textSecondaryColor
                            )
                        } else if (rank == 1) {
                            CurrencyText(
                                text = "• 👑 " + tr("ZİRVEDESİN!", "AT THE TOP!"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold
                            )
                        }
                    }
                }
            }

            // Quick Scroll / View Button
            FilledTonalButton(
                onClick = onScrollToMe,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = theme.primaryColor.copy(alpha = 0.2f),
                    contentColor = theme.primaryColor
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    CurrencyText(
                        text = tr("SIRAMA GİT", "MY RANK"),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily
                    )
                }
            }
        }
    }
}
