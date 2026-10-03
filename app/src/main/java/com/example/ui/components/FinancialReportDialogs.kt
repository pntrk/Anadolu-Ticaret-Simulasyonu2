package com.example.ui.components

import com.example.ui.components.CurrencyText

import com.example.ui.theme.tr

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*

/**
 * Günlük Gelir Detay Raporu Dialogu
 * Kullanıcının günlük gelirinin hangi kaynaklardan (Tesis üretimi, pazar satışları, mevduat faizi, borsa vb.)
 * geldiğini kurumsal, şık ve detaylı bir bilanço raporu kartı ile gösterir.
 * Kartlara tıklandığında ilgili yönetim ekranına (Üretim, Pazar, Banka, Borsa, Depo) hızlıca yönlendirir.
 */
@Composable
fun DailyIncomeBreakdownDialog(
    player: PlayerEntity,
    gameState: GameStateEntity,
    businesses: List<BusinessEntity>,
    managers: List<CompanyManager>,
    marketListings: List<MarketListing> = emptyList(),
    activeDeliveries: List<DeliveryItem> = emptyList(),
    onNavigateToProduction: ((String?) -> Unit)? = null,
    onNavigateToMarket: (() -> Unit)? = null,
    onNavigateToBorsa: (() -> Unit)? = null,
    onNavigateToBank: (() -> Unit)? = null,
    onNavigateToInventory: (() -> Unit)? = null,
    onNavigateToMap: (() -> Unit)? = null,
    onNavigateToRd: ((String?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val theme = LocalAppThemeOption.current
    val isEng = isEnglishLanguage()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Mevduat günlük faiz hesaplaması
    val depositDailyInterest = if (player.depositBalance > 0) {
        (player.depositBalance * (gameState.centralBankDepositRate / 365.0)).toLong()
    } else 0L

    // Tesisler tahmini günlük üretim geliri
    val activeBusinesses = businesses.filter { it.level > 0 }
    val estimatedBusinessRevenue = activeBusinesses.sumOf { b ->
        val product = Product.values().find { it.facilityId == b.type }
        val baseVal = (product?.basePrice ?: 120L) * b.level * 8L
        (baseVal * (1.0f - (b.wearLevel * 0.4f))).toLong().coerceAtLeast(100L)
    }

    // Pazar ve İlan Satışları (kullanıcının kendi ilanları veya gün içi pazar cirosu)
    val myListingValue = marketListings.filter { it.sellerName == player.name }.sumOf { it.pricePerUnit * it.quantity.toLong() }
    val estimatedMarketSales = if (player.dailyIncome > 0) {
        (player.dailyIncome - depositDailyInterest).coerceAtLeast(0L)
    } else {
        myListingValue.coerceAtLeast(0L)
    }

    // Gerçek veya efektif günlük gelir
    val totalGrossIncome = player.dailyIncome.coerceAtLeast(
        if (businesses.isNotEmpty() || player.depositBalance > 0) {
            depositDailyInterest + (estimatedBusinessRevenue / 2)
        } else 0L
    )

    GameAdaptiveModalSheet(
        onDismissRequest = onDismiss,
        title = tr("GÜNLÜK GELİR RAPORU", "DAILY INCOME REPORT"),
        subtitle = tr("Nakit Girişleri ve Kaynak Dağılımı", "Cash Inflows and Source Distribution"),
        icon = Icons.Rounded.TrendingUp,
        iconTint = ThemePositive,
        badgeText = "+₺${formatMoney(totalGrossIncome)}",
        badgeColor = ThemePositive,
        secondaryButtonText = tr("Raporu Kapat", "Close Report"),
        onSecondaryAction = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

                // 2. Gross Daily Income Hero Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D231A).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("TOPLAM BRÜT GÜNLÜK GELİR", "TOTAL GROSS DAILY INCOME"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemePositive.copy(alpha = 0.9f),
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemePositive.copy(alpha = 0.25f),
                                border = BorderStroke(0.5.dp, ThemePositive)
                            ) {
                                CurrencyText(
                                    text = tr("CANLI AKIŞ", "LIVE STREAM"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemePositive,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        CurrencyText(
                            text = "+${formatCurrency(totalGrossIncome, isEng)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ThemePositive,
                            fontFamily = RobotoMonoFontFamily
                        )

                        CurrencyText(
                            text = tr("Her 24 saatlik ekonomik döngüde şirketinize aktarılan toplam tahmini nakit girdisi.", "Total estimated cash inflow transferred to your company in each 24-hour economic cycle."),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Navigation hint banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0C1D2A).copy(alpha = 0.6f),
                    border = BorderStroke(0.6.dp, ThemeNeonCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyText("💡", fontSize = 12.sp)
                        CurrencyText(
                            text = tr("İlgili bölüme gitmek ve işlem yapmak için maliyet kartlarına dokunun.", "Tap any cost card to jump directly to its management section."),
                            fontSize = 9.5.sp,
                            color = ThemeNeonCyan.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Detailed Source Breakdown List
                CurrencyText(
                    text = tr("GELİR KAYNAKLARI VE İŞLEM DETAYLARI", "INCOME SOURCES AND TRANSACTION DETAILS"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeGold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kategori 1: Tesis Üretim ve Satışları -> ÜRETİM EKRANI
                    item {
                        val firstFacilityType = activeBusinesses.firstOrNull()?.type
                        val activeFacilityNames = activeBusinesses.mapNotNull { b ->
                            val product = Product.values().find { it.facilityId == b.type }
                            if (product != null) context.getString(product.facilityNameRes) else b.type
                        }
                        IncomeSourceCard(
                            title = tr("Tesis Üretim ve İmalat Satışları", "Facility Production and Manufacturing Sales"),
                            subtitle = if (activeBusinesses.isNotEmpty()) {
                                tr("${activeBusinesses.size} adet aktif tesis", "${activeBusinesses.size} active facilities") + " (${activeFacilityNames.take(3).joinToString(", ")}${if (activeFacilityNames.size > 3) "..." else ""})"
                            } else {
                                tr("Henüz aktif üretim tesisi bulunmuyor (Tesis kurarak düzenli gelir sağlayabilirsiniz).", "No active production facilities yet (You can earn regular income by establishing facilities).")
                            },
                            amount = if (totalGrossIncome > 0) (totalGrossIncome * 0.55).toLong().coerceAtLeast(estimatedBusinessRevenue) else estimatedBusinessRevenue,
                            icon = Icons.Rounded.Factory,
                            badgeText = "${activeBusinesses.size} " + tr("Tesis", "Facilities"),
                            accentColor = ThemeNeonCyan,
                            actionButtonText = tr("Tesisler", "Facilities"),
                            onClick = onNavigateToProduction?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav(firstFacilityType)
                                }
                            }
                        )
                    }

                    // Kategori 2: Pazar, İhale ve İhracat Satışları -> PAZAR EKRANI
                    item {
                        IncomeSourceCard(
                            title = tr("Pazar, İhale ve İhracat Satışları", "Market, Tender & Export Sales"),
                            subtitle = tr("İç pazar doğrudan satışları, şehir ticaret koridorları ve Borsa İstanbul satış emirleri.", "Domestic market direct sales, city trade corridors, and Borsa Istanbul sales orders."),
                            amount = if (totalGrossIncome > 0) (totalGrossIncome * 0.30).toLong().coerceAtLeast(1) else estimatedMarketSales,
                            icon = Icons.Rounded.Storefront,
                            badgeText = tr("Pazar & Borsa", "Market & Exchange"),
                            accentColor = ThemeGold,
                            actionButtonText = tr("Pazar", "Market"),
                            onClick = onNavigateToMarket?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // Kategori 3: Banka Vadeli Mevduat Faizi -> BANKA EKRANI
                    item {
                        IncomeSourceCard(
                            title = tr("Banka Vadeli Mevduat Faizi", "Bank Term Deposit Interest"),
                            subtitle = tr("TCMB %${String.format(java.util.Locale.US, "%.1f", gameState.centralBankDepositRate * 100)} faiz oranı ile vadeli mevduat getirisidir (Mevduat: ${formatCurrency(player.depositBalance, isEng)}).", "Term deposit yield at Central Bank %${String.format(java.util.Locale.US, "%.1f", gameState.centralBankDepositRate * 100)} interest rate (Deposit: ${formatCurrency(player.depositBalance, isEng)})."),
                            amount = depositDailyInterest,
                            icon = Icons.Rounded.Savings,
                            badgeText = tr("%${String.format(java.util.Locale.US, "%.1f", gameState.centralBankDepositRate * 100)} Faiz", "%${String.format(java.util.Locale.US, "%.1f", gameState.centralBankDepositRate * 100)} Interest"),
                            accentColor = ThemePositive,
                            actionButtonText = tr("Banka", "Bank"),
                            onClick = onNavigateToBank?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // Kategori 4: Lojistik ve Teslimat Sözleşmeleri -> DEPO/ENVANTER EKRANI
                    item {
                        val activeDeliveryCount = activeDeliveries.size
                        IncomeSourceCard(
                            title = tr("Lojistik & Teslimat Sözleşmeleri", "Logistics & Delivery Contracts"),
                            subtitle = if (activeDeliveryCount > 0) {
                                "$activeDeliveryCount " + tr("adet aktif şehirlerarası sevkiyat tamamlanma primleri.", "active intercity shipment completion premiums.")
                            } else {
                                tr("Şehirlerarası kargo ve toptan sevkiyat tamamlandıkça doğrudan nakit kazandırır.", "Earns direct cash as intercity cargo and wholesale shipments are completed.")
                            },
                            amount = if (totalGrossIncome > 0) (totalGrossIncome * 0.10).toLong() else 0L,
                            icon = Icons.Rounded.LocalShipping,
                            badgeText = if (activeDeliveryCount > 0) "$activeDeliveryCount " + tr("Aktif", "Active") else tr("Lojistik", "Logistics"),
                            accentColor = Color(0xFF60A5FA),
                            actionButtonText = tr("Depo", "Warehouse"),
                            onClick = onNavigateToInventory?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // Kategori 5: Borsa Temettü & Hisse Yatırım Kazançları -> BORSA EKRANI
                    item {
                        val borsaMgr = managers.find { it.id == "mgr_borsa" && it.isHired && it.isActive }
                        IncomeSourceCard(
                            title = tr("Borsa Temettü & Portföy Kazançları", "Exchange Dividend & Portfolio Earnings"),
                            subtitle = if (borsaMgr != null) {
                                tr("Borsa Analisti", "Stock Analyst") + " (${borsaMgr.name}) " + tr("yönetimindeki hisse senedi alım-satım ve temettü kâr realizasyonları.", "managed stock trading and dividend profit realizations.")
                            } else {
                                tr("Borsa ekranından şirket hisseleri alıp temettü ve sermaye kazancı elde edebilirsiniz.", "You can buy company stocks from the exchange screen to earn dividends and capital gains.")
                            },
                            amount = if (totalGrossIncome > 0) (totalGrossIncome * 0.05).toLong() else 0L,
                            icon = Icons.Rounded.ShowChart,
                            badgeText = if (borsaMgr != null) tr("Analist Aktif", "Analyst Active") else tr("Portföy", "Portfolio"),
                            accentColor = Color(0xFFA78BFA),
                            actionButtonText = tr("Borsa", "Exchange"),
                            onClick = onNavigateToBorsa?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // 4. Finans Direktörü Notu & Strateji (Hızlı Aksiyonlu)
                    item {
                        val strategyTarget = when {
                            player.depositBalance < 100_000L && player.money > 500_000L -> "bank"
                            activeBusinesses.isEmpty() -> "production"
                            else -> "production"
                        }
                        val strategyButtonText = when (strategyTarget) {
                            "bank" -> tr("Bankaya Git", "Go to Bank")
                            else -> tr("Tesis Kur", "Build Facility")
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    if (strategyTarget == "bank") onNavigateToBank?.invoke() else onNavigateToProduction?.invoke(null)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText("💡", fontSize = 16.sp)
                                        CurrencyText(
                                            text = tr("Hazine & Finans Tavsiyesi", "Treasury & Finance Advice"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ThemeGold.copy(alpha = 0.20f),
                                        border = BorderStroke(0.6.dp, ThemeGold.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            CurrencyText(
                                                text = strategyButtonText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemeGold,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = ThemeGold,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                CurrencyText(
                                    text = if (player.depositBalance < 100_000L && player.money > 500_000L) {
                                        tr("Kasada biriken atıl nakdinizi vadeli mevduata yatırarak her gün risksiz faiz geliri elde edebilirsiniz.", "You can earn risk-free daily interest income by depositing your idle cash accumulated in the treasury into a term deposit.")
                                    } else if (activeBusinesses.isEmpty()) {
                                        tr("Üretim ekranından tesis (Alabalık Tesisleri, Kauçuk Üretim Merkezi vb.) kurarak düzenli nakit akışınızı katlayabilirsiniz.", "You can multiply your regular cash flow by establishing facilities (Trout Facilities, Rubber Production Center, etc.) from the production screen.")
                                    } else {
                                        tr("Tesislerinizin seviyesini yükseltmek ve lojistik teslimatlarını artırmak günlük gelirinizi en üst düzeye çıkaracaktır.", "Upgrading your facilities and increasing your logistics deliveries will maximize your daily income.")
                                    },
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

/**
 * Günlük Gider Detay Raporu Dialogu
 * Kullanıcının günlük giderlerinin hangi maliyet merkezlerinden (Tesis bakımı, personel/yönetici maaşları,
 * kredi borç servisi faizi, hammadde alımları vb.) kaynaklandığını açık ve detaylı bir bilanço raporuyla gösterir.
 * Maliyet kartlarına tıklandığında ilgili bölüme (Tesisler, İnsan Kaynakları, Banka, Pazar, Depo) hızlıca yönlendirir.
 */
@Composable
fun DailyExpenseBreakdownDialog(
    player: PlayerEntity,
    gameState: GameStateEntity,
    businesses: List<BusinessEntity>,
    managers: List<CompanyManager>,
    onNavigateToProduction: ((String?) -> Unit)? = null,
    onNavigateToHr: (() -> Unit)? = null,
    onNavigateToBank: (() -> Unit)? = null,
    onNavigateToMarket: (() -> Unit)? = null,
    onNavigateToInventory: (() -> Unit)? = null,
    onNavigateToMap: (() -> Unit)? = null,
    onNavigateToRd: ((String?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    // 1. Tesis Bakım & İşletme Masrafları (OPEX)
    val activeBusinesses = businesses.filter { it.level > 0 }
    val facilityUpkeepTotal = activeBusinesses.sumOf { b ->
        val product = Product.values().find { it.facilityId == b.type }
        val baseCost = product?.facilityCost ?: 5000L
        val cityProfile = cities.find { it.id == b.cityId }
        val laborMult = cityProfile?.laborCostMultiplier ?: 1.0f
        val wearMult = 1.0f + (b.wearLevel * 0.5f)
        (baseCost * b.level * 0.0015f * laborMult * wearMult).toLong().coerceAtLeast(50L)
    }

    // 2. İşe Alınan ve Aktif Yönetici Maaşları
    val hiredManagers = managers.filter { it.isHired }
    val activeManagers = hiredManagers.filter { it.isActive }
    val totalManagerSalaries = activeManagers.sumOf { it.dailySalary }

    // 3. Banka Kredi Faiz & Borç Servisi
    val loanDailyInterest = if (player.loanAmount > 0) {
        (player.loanAmount * (gameState.centralBankLoanRate / 365.0)).toLong().coerceAtLeast(1L)
    } else 0L

    // Gerçek veya hesaplanmış toplam günlük gider
    val calculatedFixedExpenses = facilityUpkeepTotal + totalManagerSalaries + loanDailyInterest
    val totalGrossExpense = player.dailyExpense.coerceAtLeast(calculatedFixedExpenses)

    GameAdaptiveModalSheet(
        onDismissRequest = onDismiss,
        title = tr("GÜNLÜK GİDER RAPORU", "DAILY EXPENSE REPORT"),
        subtitle = tr("Maliyet Merkezleri ve Bilanço Kesintileri", "Cost Centers and Balance Sheet Deductions"),
        icon = Icons.Rounded.TrendingDown,
        iconTint = ThemeNegative,
        badgeText = "-₺${formatMoney(totalGrossExpense)}",
        badgeColor = ThemeNegative,
        secondaryButtonText = tr("Raporu Kapat", "Close Report"),
        onSecondaryAction = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Gross Daily Expense Hero Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2B0E11).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyText(
                                text = tr("TOPLAM BRÜT GÜNLÜK GİDER", "TOTAL GROSS DAILY EXPENSE"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNegative.copy(alpha = 0.9f),
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeNegative.copy(alpha = 0.25f),
                                border = BorderStroke(0.5.dp, ThemeNegative)
                            ) {
                                CurrencyText(
                                    text = tr("DÜZENLİ MALİYET", "REGULAR COST"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ThemeNegative,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        CurrencyText(
                            text = "-${formatCurrency(totalGrossExpense, isEng)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ThemeNegative,
                            fontFamily = RobotoMonoFontFamily
                        )

                        CurrencyText(
                            text = tr("Her gün tesis işletmesi, bordro, banka faizi ve hammadde tedariki için kasadan çıkan nakit.", "Cash leaving the treasury daily for facility operations, payroll, bank interest, and raw material procurement."),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Navigation hint banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF230D12).copy(alpha = 0.6f),
                    border = BorderStroke(0.6.dp, ThemeNegative.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurrencyText("💡", fontSize = 12.sp)
                        CurrencyText(
                            text = tr("İlgili bölüme gitmek ve maliyetleri düşürmek için kartlara dokunun.", "Tap any cost card to jump to its management section and cut expenses."),
                            fontSize = 9.5.sp,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Detailed Cost Breakdown List
                CurrencyText(
                    text = tr("GİDER KALEMLERİ VE DAĞILIMI", "EXPENSE ITEMS AND DISTRIBUTION"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeGold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kalem 1: Tesis Bakım & İşletme Giderleri -> ÜRETİM/TESİSLER
                    item {
                        ExpenseSourceCard(
                            title = tr("Tesis Bakım & İşletme Giderleri (OPEX)", "Facility Maintenance & Operating Expenses (OPEX)"),
                            subtitle = if (activeBusinesses.isNotEmpty()) {
                                "${activeBusinesses.size} " + tr("tesisin enerji, amortisman ve yıpranma bakım payı.", "facilities' energy, depreciation, and wear maintenance share.")
                            } else {
                                tr("Henüz sahip olunan tesis yok. Tesis kurulduğunda günlük işletme bedeli yansır.", "No facilities owned yet. Daily operating cost is reflected when a facility is established.")
                            },
                            amount = facilityUpkeepTotal,
                            icon = Icons.Rounded.Handyman,
                            badgeText = "${activeBusinesses.size} " + tr("Tesis", "Facilities"),
                            accentColor = Color(0xFFF97316),
                            actionButtonText = tr("Tesisler", "Facilities"),
                            onClick = onNavigateToProduction?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav(null)
                                }
                            }
                        )
                    }

                    // Kalem 2: Yönetici & Personel Maaşları -> İNSAN KAYNAKLARI (İ.K.)
                    item {
                        val managerNames = hiredManagers.joinToString(", ") { it.name }
                        ExpenseSourceCard(
                            title = tr("Yönetici & Uzman Personel Bordrosu", "Manager & Specialist Personnel Payroll"),
                            subtitle = if (hiredManagers.isNotEmpty()) {
                                "${hiredManagers.size} " + tr("kadrolu yönetici", "hired managers") + " ($managerNames). " + tr("Toplam Bordro:", "Total Payroll:") + " ${formatCurrency(totalManagerSalaries, isEng)}."
                            } else {
                                tr("Şirketinizde henüz sözleşmeli yönetici istihdam edilmiyor.", "Contracted managers are not yet employed in your company.")
                            },
                            amount = totalManagerSalaries,
                            icon = Icons.Rounded.Badge,
                            badgeText = "${hiredManagers.size} " + tr("Yönetici", "Managers"),
                            accentColor = Color(0xFFEF4444),
                            actionButtonText = tr("İ.K. Yönetimi", "HR"),
                            onClick = onNavigateToHr?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // Kalem 3: Banka Kredi Faiz & Borç Servisi -> BANKA
                    item {
                        ExpenseSourceCard(
                            title = tr("Banka Kredi Faiz Ödemesi", "Bank Loan Interest Payment"),
                            subtitle = if (player.loanAmount > 0) {
                                tr("TCMB %${String.format(java.util.Locale.US, "%.1f", gameState.centralBankLoanRate * 100)} kredi faizi ile borç servisi (Toplam Kredi Borcu: ${formatCurrency(player.loanAmount, isEng)}).", "Debt service at Central Bank %${String.format(java.util.Locale.US, "%.1f", gameState.centralBankLoanRate * 100)} loan interest (Total Debt: ${formatCurrency(player.loanAmount, isEng)}).")
                            } else {
                                tr("Şirketinizin aktif kredi borcu bulunmuyor (Finansman faiz yükü %0).", "Your company has no active loan debt (Financing interest burden 0%).")
                            },
                            amount = loanDailyInterest,
                            icon = Icons.Rounded.AccountBalance,
                            badgeText = if (player.loanAmount > 0) tr("%${String.format(java.util.Locale.US, "%.1f", gameState.centralBankLoanRate * 100)} Faiz", "%${String.format(java.util.Locale.US, "%.1f", gameState.centralBankLoanRate * 100)} Interest") else tr("Borçsuz", "Debt Free"),
                            accentColor = if (player.loanAmount > 0) ThemeNegative else ThemePositive,
                            actionButtonText = if (player.loanAmount > 0) tr("Borcu Kapat", "Pay Loan") else tr("Banka", "Bank"),
                            onClick = onNavigateToBank?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // Kalem 4: Hammadde Alımı & Üretim Tedariki -> PAZAR EKRANI
                    item {
                        val rawMatExpense = (totalGrossExpense - calculatedFixedExpenses).coerceAtLeast(0L)
                        ExpenseSourceCard(
                            title = tr("Hammadde Alımı & Tedarik Girdileri", "Raw Material Procurement & Supply Inputs"),
                            subtitle = tr("Fabrikalarda işlenen ham maddelerin (Buğday, Demir, Kauçuk vb.) alım ve parti üretim maliyetleri.", "Procurement and batch production costs of raw materials (Wheat, Iron, Rubber, etc.) processed in factories."),
                            amount = rawMatExpense,
                            icon = Icons.Rounded.Inventory2,
                            badgeText = tr("Tedarik", "Supply"),
                            accentColor = Color(0xFFEAB308),
                            actionButtonText = tr("Pazar", "Market"),
                            onClick = onNavigateToMarket?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // Kalem 5: Lojistik Nakliye & Filo Masrafları -> DEPO / HARİTA
                    item {
                        ExpenseSourceCard(
                            title = tr("Lojistik, Yakıt & Geçiş Harçları", "Logistics, Fuel & Toll Fees"),
                            subtitle = tr("Şehirlerarası ticarette filo operasyonel yakıt, otoyol ve kargo transit bedelleri.", "Fleet operational fuel, highway, and cargo transit costs in intercity trade."),
                            amount = (totalGrossExpense * 0.08).toLong().coerceAtLeast(0L),
                            icon = Icons.Rounded.LocalShipping,
                            badgeText = tr("Lojistik", "Logistics"),
                            accentColor = Color(0xFF38BDF8),
                            actionButtonText = tr("Depo", "Warehouse"),
                            onClick = onNavigateToInventory?.let { nav ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    nav()
                                }
                            }
                        )
                    }

                    // 4. Operasyonel Verimlilik & Tasarruf Notu (Hızlı Aksiyonlu)
                    item {
                        val adviceTarget = when {
                            player.loanAmount > 100_000L && player.money > player.loanAmount -> "bank"
                            activeBusinesses.any { it.wearLevel > 0.4f } -> "production"
                            else -> "rd"
                        }
                        val adviceButtonText = when (adviceTarget) {
                            "bank" -> tr("Borcu Öde", "Pay Debt")
                            "production" -> tr("Onar", "Repair")
                            else -> tr("AR-GE", "R&D")
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    when (adviceTarget) {
                                        "bank" -> onNavigateToBank?.invoke()
                                        "production" -> onNavigateToProduction?.invoke(null)
                                        else -> onNavigateToRd?.invoke(null)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1C1318),
                            border = BorderStroke(1.dp, ThemeNegative.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CurrencyText("📊", fontSize = 16.sp)
                                        CurrencyText(
                                            text = tr("Operasyonel Tasarruf Tavsiyesi", "Operational Savings Advice"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeGold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ThemeGold.copy(alpha = 0.20f),
                                        border = BorderStroke(0.6.dp, ThemeGold.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            CurrencyText(
                                                text = adviceButtonText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemeGold,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = ThemeGold,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                CurrencyText(
                                    text = if (player.loanAmount > 100_000L && player.money > player.loanAmount) {
                                        tr("Banka kredi borcunuzu erken kapatarak günlük faiz yükünüzü anında sıfırlayabilirsiniz.", "You can instantly eliminate your daily interest burden by paying off your bank loan early.")
                                    } else if (activeBusinesses.any { it.wearLevel > 0.4f }) {
                                        tr("Yıpranmış tesislerinizi onararak bakım masraflarını %30 oranında düşürebilirsiniz.", "You can reduce maintenance costs by 30% by repairing worn facilities.")
                                    } else {
                                        tr("Ar-Ge ekranında Yeşil Enerji ve Lojistik teknolojilerini geliştirerek tesis ve nakliye masraflarınızı minimize edebilirsiniz.", "You can minimize facility and transport costs by developing Green Energy and Logistics technologies in the R&D screen.")
                                    },
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

@Composable
private fun IncomeSourceCard(
    title: String,
    subtitle: String,
    amount: Long,
    icon: ImageVector,
    badgeText: String,
    accentColor: Color,
    actionButtonText: String? = null,
    customAmountText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("income_card_" + title.take(8).replace(" ", "_").lowercase()),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F1A24).copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, if (onClick != null) accentColor.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyText(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.6f))
                    ) {
                        CurrencyText(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = RobotoMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                CurrencyText(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyText(
                    text = customAmountText ?: "+${formatCurrency(amount, isEnglishLanguage())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemePositive,
                    fontFamily = RobotoMonoFontFamily
                )

                if (actionButtonText != null && onClick != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            CurrencyText(
                                text = actionButtonText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = RobotoMonoFontFamily
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseSourceCard(
    title: String,
    subtitle: String,
    amount: Long,
    icon: ImageVector,
    badgeText: String,
    accentColor: Color,
    actionButtonText: String? = null,
    customAmountText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_" + title.take(8).replace(" ", "_").lowercase()),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1114).copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, if (onClick != null) accentColor.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurrencyText(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.6f))
                    ) {
                        CurrencyText(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = RobotoMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                CurrencyText(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyText(
                    text = "-${formatCurrency(amount, isEnglishLanguage())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemeNegative,
                    fontFamily = RobotoMonoFontFamily
                )

                if (actionButtonText != null && onClick != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            CurrencyText(
                                text = actionButtonText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = RobotoMonoFontFamily
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
