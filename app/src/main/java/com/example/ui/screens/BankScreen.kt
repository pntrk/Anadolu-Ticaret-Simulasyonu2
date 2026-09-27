package com.example.ui.screens

import com.example.viewmodel.*

import com.example.ui.components.CurrencyText

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
    val businesses by viewModel.businesses.collectAsStateWithLifecycle()

    val countdownHours = nextBankSettlementMs / (1000 * 60 * 60)
    val countdownMins = (nextBankSettlementMs % (1000 * 60 * 60)) / (1000 * 60)
    val countdownSecs = (nextBankSettlementMs % (1000 * 60)) / 1000
    val countdownFormatted = String.format("%02d:%02d:%02d", countdownHours, countdownMins, countdownSecs)

    // Selected Operation Module: 0 = Mevduat (Deposit), 1 = Kredi (Loan), 2 = İflas Masası (Bankruptcy), 3 = Elmas Kasası (Gems)
    var selectedModule by remember { mutableIntStateOf(0) }

    var amountInput by remember { mutableStateOf("") }
    var selectedGemCount by remember { mutableIntStateOf(10) }
    var gemInputText by remember { mutableStateOf("10") }
    var showGemStoreDialog by remember { mutableStateOf(false) }

    // Dynamic Financial Loan Limit based on 50% of Total Facility Valuation
    val totalFacilityValuation = viewModel.calculateTotalFacilityValuation()
    val maxLoanLimit = viewModel.calculateMaxLoanLimit()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedModule = 3 },
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyText(
                                    text = "${player.gems} Elmas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeNeonCyan,
                                    fontFamily = RobotoMonoFontFamily
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, ThemeNeonCyan)
                                ) {
                                    CurrencyText(
                                        text = tr("₳ Çevir ❯", "Exchange ❯"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeNeonCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modules = listOf(
                        tr("🏦 Vadeli Mevduat", "🏦 High-Yield Deposit"),
                        tr("💳 Kurumsal Kredi", "💳 Corporate Loan"),
                        tr("🏛️ İflas Masası", "🏛️ Bankruptcy Desk"),
                        tr("💎 Elmas ➔ Anadolu Lirası", "💎 Gems ➔ Anatolian Lira")
                    )

                    modules.forEachIndexed { index, title ->
                        val isSelected = selectedModule == index
                        Surface(
                            modifier = Modifier
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp)
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
                                        text = tr("Tesis teminatlı %50 limitli finansman", "Facility-collateralized 50% limit financing"),
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

                            // 1. Facility Collateral Analysis Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = if (totalFacilityValuation > 0L) ThemeNeonCyan.copy(alpha = 0.06f) else ThemeNegative.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (totalFacilityValuation > 0L) ThemeNeonCyan.copy(alpha = 0.3f) else ThemeNegative.copy(alpha = 0.3f))
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText("🏭", fontSize = 12.sp)
                                            CurrencyText(
                                                text = tr("Tesis Teminat Değeri (${businesses.size} Tesis):", "Total Facility Collateral (${businesses.size} Facilities):"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = theme.textPrimaryColor
                                            )
                                        }

                                        CurrencyText(
                                            text = formatCredit(totalFacilityValuation),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeNeonCyan,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText(
                                            text = tr("🛡️ %50 Kredi Limit Tavanı:", "🛡️ 50% Collateral Limit Ceiling:"),
                                            fontSize = 10.5.sp,
                                            color = theme.textSecondaryColor
                                        )
                                        CurrencyText(
                                            text = formatCredit(maxLoanLimit),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }

                                    if (totalFacilityValuation <= 0L) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThemeNegative.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CurrencyText("⚠️", fontSize = 12.sp)
                                                CurrencyText(
                                                    text = tr(
                                                        "Teminatsız Kredi Çekilemez! Kredi limiti tesislerinizin toplam piyasa değerinin %50'si kadardır. Kredi çekebilmek için en az 1 tesise sahip olmalısınız.",
                                                        "No Collateral Available! Loan limit is 50% of your total facility valuation. You must own at least 1 facility to take a loan."
                                                    ),
                                                    fontSize = 10.sp,
                                                    color = ThemeNegative,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }
                                    }
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

                            // Foreclosure & Bankruptcy Desk Protection Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF131D31),
                                border = BorderStroke(0.8.dp, ThemeGold.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText("⚖️", fontSize = 13.sp)
                                        CurrencyText(
                                            text = tr("İflas Masası & Haciz Güvencesi", "Bankruptcy Desk & Foreclosure Collateral"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold
                                        )
                                    }

                                    CurrencyText(
                                        text = tr(
                                            "Kredi taksitleri her gece 00:00 mutabakatında otomatik tahsil edilir. Yetersiz bakiye durumunda kredi borcuna karşılık tesisleriniz (Tier 4 Mega Tesisler hariç) en değerli olandan başlamak üzere haczedilir ve İflas Masasından tasfiye edilerek borcunuz kapatılır.",
                                            "Loan installments are settled daily at midnight (00:00). If defaulted, your facilities (except Tier 4 Mega Facilities) are foreclosed starting from the most valuable and liquidated via Bankruptcy Desk to settle your debt."
                                        ),
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        lineHeight = 14.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Surface(
                                            modifier = Modifier.clickable { selectedModule = 2 },
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThemeGold.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, ThemeGold.copy(alpha = 0.6f))
                                        ) {
                                            CurrencyText(
                                                text = tr("🏛️ İflas Masasını Gör ➔", "🏛️ View Bankruptcy Desk ➔"),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemeGold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
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
                                    enabled = totalFacilityValuation > 0L && availableLoanLimit > 0L,
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
                                    enabled = player.loanAmount > 0L,
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemePositive)
                                ) {
                                    CurrencyText(tr("💸 ÖZEL MİKTAR ÖDE", "💸 REPAY CUSTOM"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // İFLAS MASASI MODULE
                    com.example.ui.components.ForeclosureBankruptcyDesk(viewModel = viewModel)
                }

                3 -> {
                    // ELMAS ➔ ANADOLU LİRASI BOZDURMA MODÜLÜ
                    val rewardPerGem = 3_000L
                    val parsedGems = gemInputText.toIntOrNull() ?: selectedGemCount
                    val safeGemsToExchange = parsedGems.coerceAtLeast(0)
                    val totalReward = safeGemsToExchange.toLong() * rewardPerGem

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = theme.surfaceColor,
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Başlık & Bakiye Rozeti
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    CurrencyText(
                                        text = tr("💎 ELMAS ➔ ANADOLU LİRASI BOZDURMA", "💎 GEMS ➔ ANATOLIAN LIRA EXCHANGE"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeNeonCyan,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    CurrencyText(
                                        text = tr("Elmaslarınızı anında nakit Anadolu Lirası'na (₳) dönüştürün", "Convert your gems instantly to liquid Anatolian Lira (₳)"),
                                        fontSize = 11.sp,
                                        color = theme.textSecondaryColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText("💎", fontSize = 12.sp)
                                        CurrencyText(
                                            text = "${player.gems}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeNeonCyan,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }

                            // Kur ve Kasa Durumu Bilgi Kartları
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThemeNeonCyan.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.25f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        CurrencyText(
                                            text = tr("Dönüşüm Kuru (1 💎):", "Conversion Rate (1 💎):"),
                                            fontSize = 10.sp,
                                            color = theme.textSecondaryColor
                                        )
                                        CurrencyText(
                                            text = "1 💎 = 3.000 ₳",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeNeonCyan,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = ThemeGold.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.25f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        CurrencyText(
                                            text = tr("Mevcut Anadolu Lirası:", "Current Anatolian Lira:"),
                                            fontSize = 10.sp,
                                            color = theme.textSecondaryColor
                                        )
                                        CurrencyText(
                                            text = formatCredit(player.money),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeGold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }

                            // Miktar Giriş Alanı (Özel Miktar Yazma)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = gemInputText,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        gemInputText = digitsOnly
                                        selectedGemCount = digitsOnly.toIntOrNull() ?: 0
                                    },
                                    label = {
                                        CurrencyText(
                                            text = tr("Çevrilecek Elmas Miktarı (💎)", "Amount of Gems to Exchange (💎)"),
                                            fontSize = 11.sp
                                        )
                                    },
                                    placeholder = {
                                        CurrencyText(
                                            text = tr("Örn: 25", "e.g. 25"),
                                            fontSize = 11.sp,
                                            color = theme.textSecondaryColor.copy(alpha = 0.6f)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThemeNeonCyan,
                                        unfocusedBorderColor = theme.borderColor
                                    ),
                                    trailingIcon = {
                                        if (gemInputText.isNotEmpty()) {
                                            IconButton(onClick = {
                                                gemInputText = ""
                                                selectedGemCount = 0
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Clear,
                                                    contentDescription = "Temizle",
                                                    tint = theme.textSecondaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                )

                                // Hızlı Miktar Seçim Butonları
                                CurrencyText(
                                    text = tr("Hızlı Miktar Seçenekleri:", "Quick Selection Presets:"),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.textSecondaryColor
                                )

                                // 1. Satır: Sabit Miktarlar (10, 50, 100, 500)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(10, 50, 100, 500).forEach { count ->
                                        val isCurrent = safeGemsToExchange == count
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    gemInputText = count.toString()
                                                    selectedGemCount = count
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurrent) ThemeNeonCyan.copy(alpha = 0.22f) else theme.surfaceVariantColor,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCurrent) ThemeNeonCyan else theme.borderColor
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CurrencyText(
                                                    text = "$count 💎",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) ThemeNeonCyan else theme.textPrimaryColor
                                                )
                                                CurrencyText(
                                                    text = formatCredit(count.toLong() * rewardPerGem),
                                                    fontSize = 9.sp,
                                                    color = theme.textSecondaryColor
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Satır: Oransal / Maksimum Seçenekler (%25, %50, %75, TÜMÜ)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        0.25f to "%25",
                                        0.50f to "%50",
                                        0.75f to "%75",
                                        1.00f to tr("TÜMÜ", "ALL")
                                    ).forEach { (ratio, label) ->
                                        val targetCount = (player.gems * ratio).toInt()
                                        val isSelected = targetCount > 0 && safeGemsToExchange == targetCount

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (targetCount > 0) {
                                                        gemInputText = targetCount.toString()
                                                        selectedGemCount = targetCount
                                                    }
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) ThemeGold.copy(alpha = 0.22f) else theme.surfaceVariantColor,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) ThemeGold else theme.borderColor
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CurrencyText(
                                                    text = label,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) ThemeGold else theme.textPrimaryColor
                                                )
                                                CurrencyText(
                                                    text = "$targetCount 💎",
                                                    fontSize = 9.sp,
                                                    color = theme.textSecondaryColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Canlı Çeviri Hesaplama Önizleme Kartı
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.07f),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.35f))
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
                                        CurrencyText(
                                            text = tr("Dönüştürülecek:", "To Exchange:"),
                                            fontSize = 11.sp,
                                            color = theme.textSecondaryColor
                                        )
                                        CurrencyText(
                                            text = "$safeGemsToExchange 💎 Elmas ➔ +${formatCredit(totalReward)} Anadolu Lirası",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemePositive,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText(
                                            text = tr("İşlem Sonrası Kasa:", "Cash After Exchange:"),
                                            fontSize = 11.sp,
                                            color = theme.textSecondaryColor
                                        )
                                        CurrencyText(
                                            text = formatCredit(player.money + totalReward),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold,
                                            fontFamily = RobotoMonoFontFamily
                                        )
                                    }
                                }
                            }

                            // Dönüştürme Aksiyon Butonu
                            AppButton(
                                onClick = {
                                    if (safeGemsToExchange <= 0) {
                                        SmartNotificationManager.show(
                                            tr("Lütfen geçerli bir elmas miktarı giriniz!", "Please enter a valid gem amount!", isEn),
                                            NotificationType.INFO
                                        )
                                        return@AppButton
                                    }
                                    if (player.gems < safeGemsToExchange) {
                                        SmartNotificationManager.show(
                                            tr("Yetersiz elmas! Sahip olduğunuz: ${player.gems} 💎", "Insufficient gems! You have: ${player.gems} 💎", isEn),
                                            NotificationType.ALERT
                                        )
                                        return@AppButton
                                    }
                                    viewModel.exchangeGemsForMoney(safeGemsToExchange)
                                    gemInputText = ""
                                    selectedGemCount = 0
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyText("💎 ➔ ₳", fontSize = 12.sp)
                                    CurrencyText(
                                        text = tr(
                                            "ELMASLARI ANADOLU LİRASI'NA ÇEVİR (+${formatCredit(totalReward)})",
                                            "EXCHANGE GEMS TO ANATOLIAN LIRA (+${formatCredit(totalReward)})"
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }

                            // Elmas Satın Alma / Mağaza Kısayolu
                            OutlinedButton(
                                onClick = { showGemStoreDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f))
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
                                        text = tr("Daha Fazla Elmas Al (Mağaza)", "Buy More Gems (Store)"),
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
