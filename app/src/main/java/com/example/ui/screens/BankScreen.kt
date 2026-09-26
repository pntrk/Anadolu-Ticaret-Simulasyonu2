package com.example.ui.screens

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppButton
import com.example.ui.components.GameCurrencyBadge
import com.example.ui.components.GameCurrencyIcon
import com.example.ui.components.GemStoreDialog
import com.example.ui.components.NotificationType
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.formatCredit
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatMoney
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.tr
import com.example.viewmodel.GameIntent
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

/**
 * Modern, sade ve tek para birimli (◈ Game Currency) Bankacılık Portalı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    viewModel: GameViewModel
) {
    val player = uiState.playerState.player ?: return
    val gameState = uiState.gameStateObj ?: return
    val theme = LocalAppThemeOption.current
    val isEn = isEnglishLanguage()

    val nextBankSettlementMs by viewModel.nextBankSettlementRemainingMs.collectAsStateWithLifecycle()
    val dailyDepositYield by viewModel.dailyDepositYieldTry.collectAsStateWithLifecycle()
    val dailyLoanInstallment by viewModel.dailyLoanInstallmentTry.collectAsStateWithLifecycle()
    val dailyLoanPrincipal by viewModel.dailyLoanPrincipalTry.collectAsStateWithLifecycle()
    val dailyLoanInterest by viewModel.dailyLoanInterestTry.collectAsStateWithLifecycle()

    val countdownHours = nextBankSettlementMs / (1000 * 60 * 60)
    val countdownMins = (nextBankSettlementMs % (1000 * 60 * 60)) / (1000 * 60)
    val countdownSecs = (nextBankSettlementMs % (1000 * 60)) / 1000
    val countdownFormatted = String.format("%02d:%02d:%02d", countdownHours, countdownMins, countdownSecs)

    // Selected Operation Module: 0 = Mevduat (Deposit), 1 = Kredi (Loan), 2 = Elmas Kasası (Gems)
    var selectedModule by remember { mutableIntStateOf(0) }

    var amountInput by remember { mutableStateOf("") }
    var selectedGemCount by remember { mutableIntStateOf(10) }
    var showGemStoreDialog by remember { mutableStateOf(false) }

    // Dynamic Financial Loan Limit (Scaled for Single Currency)
    val maxLoanLimit = 50_000L + (player.level * 250_000L)
    val availableLoanLimit = (maxLoanLimit - player.loanAmount).coerceAtLeast(0L)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. BANK HERO HEADER
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = theme.surfaceColor,
                border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.4f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        theme.primaryColor.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        ThemeGold.copy(alpha = 0.08f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.primaryColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, theme.primaryColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        tint = theme.primaryColor,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(24.dp)
                                    )
                                }
                                Column {
                                    CurrencyText(
                                        text = tr("KÜRESEL FİNANS BANKASI", "GLOBAL FINANCIAL BANK"),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = theme.textPrimaryColor,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = tr("Merkezi Likidite & Kredi Portalı", "Central Liquidity & Credit Portal"),
                                        fontSize = 11.sp,
                                        color = theme.textSecondaryColor
                                    )
                                }
                            }

                            GameCurrencyBadge(
                                amount = player.money,
                                badgeColor = ThemeGold
                            )
                        }
                    }
                }
            }
        }

        // 2. TOTAL ASSET OVERVIEW CARD
        item {
            val netPosition = player.money + player.depositBalance - player.loanAmount

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = theme.surfaceVariantColor,
                border = BorderStroke(1.dp, theme.borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("📊 TÜM BANKA VARLIKLARI VE HESAPLAR", "📊 BANK ASSETS & FINANCIAL OVERVIEW"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textSecondaryColor,
                            fontFamily = RobotoMonoFontFamily
                        )

                        CurrencyText(
                            text = "Net: ${formatCredit(netPosition)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netPosition >= 0) ThemePositive else ThemeNegative,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Liquid Cash Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = theme.surfaceColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CurrencyText(
                                    text = tr("Harcanabilir Nakit", "Available Cash"),
                                    fontSize = 10.sp,
                                    color = theme.textSecondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = formatCredit(player.money),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemeGold,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Deposit Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = theme.surfaceColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CurrencyText(
                                    text = tr("Vadeli Mevduat", "Term Deposit"),
                                    fontSize = 10.sp,
                                    color = theme.textSecondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = formatCredit(player.depositBalance),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemePositive,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Loan Debt Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = theme.surfaceColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CurrencyText(
                                    text = tr("Kredi Borcu", "Loan Debt"),
                                    fontSize = 10.sp,
                                    color = theme.textSecondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CurrencyText(
                                    text = formatCredit(player.loanAmount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (player.loanAmount > 0) ThemeNegative else theme.textSecondaryColor,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Daily Real-Time Net Cash Flow Bar
                    val dailyNetDiff = dailyDepositYield - dailyLoanInstallment
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = (if (dailyNetDiff >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, (if (dailyNetDiff >= 0) ThemePositive else ThemeNegative).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    CurrencyText("📅", fontSize = 13.sp)
                                    CurrencyText(
                                        text = tr("Günlük Net Banka Akışı:", "Daily Net Bank Cash Flow:"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                CurrencyText(
                                    text = (if (dailyNetDiff >= 0) "+" else "-") + formatCredit(kotlin.math.abs(dailyNetDiff)) + tr(" / gün", " / day"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dailyNetDiff >= 0) ThemePositive else ThemeNegative,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = tr("Mevduat: +${formatCredit(dailyDepositYield)} | Kredi: -${formatCredit(dailyLoanInstallment)}", "Yield: +${formatCredit(dailyDepositYield)} | Loan: -${formatCredit(dailyLoanInstallment)}"),
                                    fontSize = 10.sp,
                                    color = theme.textSecondaryColor,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = theme.surfaceColor.copy(alpha = 0.8f),
                                    border = BorderStroke(0.5.dp, theme.borderColor)
                                ) {
                                    CurrencyText(
                                        text = "⏱️ $countdownFormatted",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // Gem Balance Quick Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = ThemeNeonCyan.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText("💎", fontSize = 15.sp)
                                CurrencyText(
                                    text = tr("Elmas Kasası Bakiyesi:", "Gem Vault Balance:"),
                                    fontSize = 12.sp,
                                    color = theme.textPrimaryColor
                                )
                            }
                            CurrencyText(
                                text = "${player.gems} Elmas",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNeonCyan,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 3. OPERATION MODULE TABS (MEVDUAT | KREDİ | ELMAS BOZDUR)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyText(
                    text = tr("İŞLEM MODÜLÜNÜ SEÇİN:", "SELECT OPERATION MODULE:"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textSecondaryColor,
                    fontFamily = RobotoMonoFontFamily
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modules = listOf(
                        tr("🏦 Vadeli Mevduat", "🏦 High-Yield Deposit"),
                        tr("💳 Kurumsal Kredi", "💳 Corporate Loan"),
                        tr("💎 Elmas Gişesi", "💎 Gem Exchange")
                    )

                    modules.forEachIndexed { index, title ->
                        val isSelected = selectedModule == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedModule = index },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) ThemeGold.copy(alpha = 0.18f) else theme.surfaceColor,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ThemeGold else theme.borderColor
                            )
                        ) {
                            CurrencyText(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ThemeGold else theme.textSecondaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. MODULE CONTENT
        item {
            when (selectedModule) {
                0 -> {
                    // MEVDUAT MODULE
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = theme.surfaceColor,
                        border = BorderStroke(1.dp, theme.borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    CurrencyText(
                                        text = tr("VADELİ MEVDUAT HESABI", "HIGH-YIELD TERM DEPOSIT"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimaryColor,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = tr("Gerçek gün bazlı günlük bileşik getiri", "Real-time daily compound yield"),
                                        fontSize = 11.sp,
                                        color = theme.textSecondaryColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemePositive.copy(alpha = 0.15f)
                                ) {
                                    CurrencyText(
                                        text = tr("Yıllık Getiri: %${"%.1f".format(gameState.centralBankDepositRate * 100)}", "Annual Rate: %${"%.1f".format(gameState.centralBankDepositRate * 100)}"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemePositive,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }

                            // Active Deposit & Daily Yield Cards
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.surfaceVariantColor,
                                    border = BorderStroke(1.dp, theme.borderColor)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText(
                                                text = tr("💵 Harcanabilir Nakit Paranız:", "💵 Available Cash Balance:"),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ThemeGold
                                            )
                                            CurrencyText(
                                                text = formatCredit(player.money),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ThemeGold,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }
                                        HorizontalDivider(color = theme.borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText(
                                                text = tr("🏦 Mevcut Vadeli Bakiyeniz:", "🏦 Current Deposit Balance:"),
                                                fontSize = 12.sp,
                                                color = theme.textSecondaryColor
                                            )
                                            CurrencyText(
                                                text = formatCredit(player.depositBalance),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemePositive,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThemePositive.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            CurrencyText(
                                                text = tr("📈 Günlük Net Faiz Getirisi:", "📈 Daily Interest Yield:"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textPrimaryColor
                                            )
                                            CurrencyText(
                                                text = tr("⏱️ Aktarıma Kalan Süre: $countdownFormatted", "⏱️ Due in: $countdownFormatted"),
                                                fontSize = 10.sp,
                                                color = ThemeGold,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }

                                        CurrencyText(
                                            text = "+${formatCredit(dailyDepositYield)}" + tr(" / gün", " / day"),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemePositive,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }

                            // Info Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = theme.surfaceColor,
                                border = BorderStroke(0.5.dp, theme.borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    CurrencyText("💡", fontSize = 12.sp)
                                    CurrencyText(
                                        text = tr(
                                            "Vadeli mevduatınız her gerçek takvim günü gece 00:00'da günlük faiz kazandırır ve otomatik olarak bileşik mevduat bakiyenize eklenir.",
                                            "Your term deposit generates interest every calendar day at midnight (00:00) and automatically compounds into your deposit balance."
                                        ),
                                        fontSize = 10.sp,
                                        color = theme.textSecondaryColor,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Amount Input & Quick Preset Buttons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = amountInput,
                                    onValueChange = { amountInput = it.filter { char -> char.isDigit() } },
                                    label = { CurrencyText(tr("İşlem Miktarı (◈)", "Amount (◈)")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThemeGold,
                                        unfocusedBorderColor = theme.borderColor
                                    )
                                )

                                // Preset for Cash (Deposit)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(
                                        text = tr("Nakit:", "Cash:"),
                                        fontSize = 10.sp,
                                        color = theme.textSecondaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { ratio ->
                                        val targetVal = (player.money * ratio).toLong()
                                        val labelStr = if (ratio == 1.0f) tr("Nakit Tümü", "All Cash") else "%${(ratio * 100).toInt()}"

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { amountInput = targetVal.toString() },
                                            shape = RoundedCornerShape(4.dp),
                                            color = theme.surfaceVariantColor,
                                            border = BorderStroke(1.dp, theme.borderColor)
                                        ) {
                                            CurrencyText(
                                                text = labelStr,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                color = theme.textPrimaryColor,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Preset for Deposit Balance (Withdraw)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(
                                        text = tr("Mevduat:", "Deposit:"),
                                        fontSize = 10.sp,
                                        color = ThemePositive,
                                        fontWeight = FontWeight.Bold
                                    )
                                    listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { ratio ->
                                        val targetVal = (player.depositBalance * ratio).toLong()
                                        val labelStr = if (ratio == 1.0f) tr("Mevduat Tümü", "All Deposit") else "%${(ratio * 100).toInt()}"

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { amountInput = targetVal.toString() },
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThemePositive.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.4f))
                                        ) {
                                            CurrencyText(
                                                text = labelStr,
                                                fontSize = 9.sp,
                                                textAlign = TextAlign.Center,
                                                color = ThemePositive,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Deposit & Withdraw Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    onClick = {
                                        val valToDeposit = amountInput.toLongOrNull() ?: 0L
                                        if (valToDeposit <= 0L) {
                                            SmartNotificationManager.show(tr("Lütfen geçerli bir miktar giriniz!", "Please enter a valid amount!", isEn), NotificationType.INFO)
                                            return@AppButton
                                        }
                                        viewModel.depositMoney(valToDeposit)
                                        amountInput = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemePositive)
                                ) {
                                    CurrencyText(tr("📥 MEVDUAT YATIR", "📥 DEPOSIT"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                AppButton(
                                    onClick = {
                                        val valToWithdraw = amountInput.toLongOrNull() ?: 0L
                                        if (valToWithdraw <= 0L) {
                                            SmartNotificationManager.show(tr("Lütfen geçerli bir miktar giriniz!", "Please enter a valid amount!", isEn), NotificationType.INFO)
                                            return@AppButton
                                        }
                                        viewModel.withdrawDeposit(valToWithdraw)
                                        amountInput = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
                                ) {
                                    CurrencyText(tr("📤 MEVDUAT ÇEK", "📤 WITHDRAW"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // KREDİ MODULE
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = theme.surfaceColor,
                        border = BorderStroke(1.dp, theme.borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    CurrencyText(
                                        text = tr("KURUMSAL KREDİ İMKANI", "CORPORATE CREDIT FACILITY"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimaryColor,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = tr("30 Günlük günlük taksitli finansman", "30-Day daily installment financing"),
                                        fontSize = 11.sp,
                                        color = theme.textSecondaryColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeGold.copy(alpha = 0.15f)
                                ) {
                                    CurrencyText(
                                        text = tr("Limit: ${formatCredit(maxLoanLimit)}", "Limit: ${formatCredit(maxLoanLimit)}"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }

                            // Credit Progress Bar & Debt Summary
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = theme.surfaceVariantColor,
                                    border = BorderStroke(1.dp, theme.borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText(
                                            text = tr("💵 Harcanabilir Nakit Paranız:", "💵 Available Cash Balance:"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ThemeGold
                                        )
                                        CurrencyText(
                                            text = formatCredit(player.money),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeGold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    CurrencyText(
                                        text = "Borç: ${formatCredit(player.loanAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (player.loanAmount > 0) ThemeNegative else theme.textSecondaryColor,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = "Kalan Limit: ${formatCredit(availableLoanLimit)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemePositive,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = if (maxLoanLimit > 0) (player.loanAmount.toFloat() / maxLoanLimit.toFloat()).coerceIn(0f, 1f) else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = ThemeNegative,
                                    trackColor = theme.borderColor
                                )
                            }

                            // Daily Installment Details Card
                            if (player.loanAmount > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThemeNegative.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                CurrencyText(
                                                    text = tr("📉 Günlük Kredi Taksiti (30 Gün):", "📉 Daily Installment (30-Day):"),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textPrimaryColor
                                                )
                                                CurrencyText(
                                                    text = tr("⏱️ Taksit Vadesi: $countdownFormatted", "⏱️ Due in: $countdownFormatted"),
                                                    fontSize = 10.sp,
                                                    color = ThemeGold,
                                                    fontFamily = RobotoMonoFontFamily
                                                )
                                            }

                                            CurrencyText(
                                                text = "-${formatCredit(dailyLoanInstallment)}" + tr(" / gün", " / day"),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ThemeNegative,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CurrencyText(
                                                text = tr("• Anapara Taksiti: ${formatCredit(dailyLoanPrincipal)}", "• Principal: ${formatCredit(dailyLoanPrincipal)}"),
                                                fontSize = 10.sp,
                                                color = theme.textSecondaryColor,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                            CurrencyText(
                                                text = tr("• Günlük Faiz: ${formatCredit(dailyLoanInterest)}", "• Daily Interest: ${formatCredit(dailyLoanInterest)}"),
                                                fontSize = 10.sp,
                                                color = theme.textSecondaryColor,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }

                                        // Quick Installment Action Buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AppButton(
                                                onClick = { viewModel.payDailyLoanInstallmentNow() },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemeGold)
                                            ) {
                                                CurrencyText(
                                                    text = tr("⚡ Taksiti Şimdi Öde", "⚡ Pay Installment"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }

                                            AppButton(
                                                onClick = { viewModel.payAllLoanDebt() },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemePositive)
                                            ) {
                                                CurrencyText(
                                                    text = tr("🎉 Tüm Borcu Kapat", "🎉 Pay Off All Debt"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Info Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = theme.surfaceColor,
                                border = BorderStroke(0.5.dp, theme.borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    CurrencyText("💡", fontSize = 12.sp)
                                    CurrencyText(
                                        text = tr(
                                            "Kurumsal kredi taksitleri 30 günlük plan üzerinden her gerçek takvim günü gece 00:00'da nakit hesabınızdan otomatik tahsil edilir. Yetersiz bakiye durumunda mevduatınızdan karşılanır.",
                                            "Corporate loan installments are calculated over a 30-day term and automatically deducted from your cash balance at midnight (00:00). Shortfalls are covered by your deposit."
                                        ),
                                        fontSize = 10.sp,
                                        color = theme.textSecondaryColor,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Amount Input & Quick Buttons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = amountInput,
                                    onValueChange = { amountInput = it.filter { char -> char.isDigit() } },
                                    label = { CurrencyText(tr("Kredi / Borç Ödeme Miktarı (◈)", "Loan / Repay Amount (◈)")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThemeGold,
                                        unfocusedBorderColor = theme.borderColor
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { ratio ->
                                        val targetVal = (availableLoanLimit * ratio).toLong()
                                        val labelStr = if (ratio == 1.0f) tr("Maks Limit", "Max Limit") else "%${(ratio * 100).toInt()}"

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { amountInput = targetVal.toString() },
                                            shape = RoundedCornerShape(4.dp),
                                            color = theme.surfaceVariantColor,
                                            border = BorderStroke(1.dp, theme.borderColor)
                                        ) {
                                            CurrencyText(
                                                text = labelStr,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                color = theme.textPrimaryColor,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Take Loan & Repay Loan Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    onClick = {
                                        val valToTake = amountInput.toLongOrNull() ?: 0L
                                        if (valToTake <= 0L) {
                                            SmartNotificationManager.show(tr("Lütfen geçerli bir miktar giriniz!", "Please enter a valid amount!", isEn), NotificationType.INFO)
                                            return@AppButton
                                        }
                                        viewModel.takeLoan(valToTake)
                                        amountInput = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeGold)
                                ) {
                                    CurrencyText(tr("💵 KREDİ ÇEK", "💵 TAKE LOAN"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }

                                AppButton(
                                    onClick = {
                                        val valToRepay = amountInput.toLongOrNull() ?: 0L
                                        if (valToRepay <= 0L) {
                                            SmartNotificationManager.show(tr("Lütfen geçerli bir miktar giriniz!", "Please enter a valid amount!", isEn), NotificationType.INFO)
                                            return@AppButton
                                        }
                                        viewModel.repayLoan(valToRepay)
                                        amountInput = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemePositive)
                                ) {
                                    CurrencyText(tr("💸 ÖZEL MİKTAR ÖDE", "💸 REPAY CUSTOM"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ELMAS BOZDUR MODULE
                    val rewardPerGem = 3_000L
                    val rewardForSelectedGems = selectedGemCount * rewardPerGem

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = theme.surfaceColor,
                        border = BorderStroke(1.dp, theme.borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    CurrencyText(
                                        text = tr("💎 ELMAS BOZDURMA GİŞESİ", "💎 GEM EXCHANGE COUNTER"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimaryColor,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = tr("Elmaslarınızı anında likit oyun parasına dönüştürün", "Convert gems to instant liquid game currency"),
                                        fontSize = 11.sp,
                                        color = theme.textSecondaryColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.15f)
                                ) {
                                    CurrencyText(
                                        text = "${player.gems} 💎",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeNeonCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }

                            // Exchange Rate Info Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText(
                                        text = tr("Dönüşüm Kuru (1 Elmas):", "Exchange Rate (1 Gem):"),
                                        fontSize = 12.sp,
                                        color = theme.textPrimaryColor
                                    )
                                    CurrencyText(
                                        text = "1 💎 = ${formatCredit(rewardPerGem)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeNeonCyan,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                }
                            }

                            // Gem Amount Selector Cards
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CurrencyText(
                                    text = tr("Bozdurulacak Elmas Miktarı:", "Amount of Gems to Exchange:"),
                                    fontSize = 11.sp,
                                    color = theme.textSecondaryColor
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(10, 50, 100, player.gems.coerceAtLeast(1)).distinct().forEach { gemCount ->
                                        val isSelected = selectedGemCount == gemCount
                                        val totalGain = gemCount * rewardPerGem

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedGemCount = gemCount },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) ThemeNeonCyan.copy(alpha = 0.2f) else theme.surfaceVariantColor,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) ThemeNeonCyan else theme.borderColor
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CurrencyText(
                                                    text = "$gemCount 💎",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) ThemeNeonCyan else theme.textPrimaryColor
                                                )
                                                CurrencyText(
                                                    text = formatCredit(totalGain),
                                                    fontSize = 9.sp,
                                                    color = theme.textSecondaryColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Exchange Action Button
                            AppButton(
                                onClick = {
                                    if (player.gems < selectedGemCount || selectedGemCount <= 0) {
                                        SmartNotificationManager.show(tr("Yeterli elmasınız bulunmamaktadır!", "You don't have enough gems!", isEn), NotificationType.ALERT)
                                        return@AppButton
                                    }
                                    viewModel.exchangeGemsForMoney(selectedGemCount)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan)
                            ) {
                                CurrencyText(
                                    text = tr("💎 ELMASLARI HESABA AKTAR (+${formatCredit(rewardForSelectedGems)})", "💎 TRANSFER GEMS TO ACCOUNT (+${formatCredit(rewardForSelectedGems)})"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            // Gem Store Shortcut Button
                            OutlinedButton(
                                onClick = { showGemStoreDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, ThemeGold)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ShoppingCart,
                                        contentDescription = null,
                                        tint = ThemeGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    CurrencyText(
                                        text = tr("Daha Fazla Elmas Satın Al / Mağaza", "Buy More Gems / Store"),
                                        fontSize = 11.sp,
                                        color = ThemeGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // GEM STORE DIALOG
    if (showGemStoreDialog) {
        GemStoreDialog(
            viewModel = viewModel,
            onDismiss = { showGemStoreDialog = false }
        )
    }
}
