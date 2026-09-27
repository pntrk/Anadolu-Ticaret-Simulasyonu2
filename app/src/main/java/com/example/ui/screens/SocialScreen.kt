package com.example.ui.screens

import com.example.viewmodel.calculateCompanyValuation
import com.example.ui.components.CurrencyText
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.AppLanguage
import com.example.ui.components.formatCredit

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MultiplayerManager
import com.example.data.OnlinePlayer
import com.example.data.PlayerFacilityInfo
import com.example.data.MuseumHeritageManager
import com.example.data.AntiqueArtifact
import com.example.ui.components.formatMoney
import com.example.ui.components.formatCurrency
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import com.example.viewmodel.GameViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun getCleanPlayerKey(p: OnlinePlayer): String {
    val cleanId = p.id.lowercase().trim().removeSuffix("_backup").removePrefix("vault_").replace(".", "_")
    val emailKey = if (cleanId.contains("@")) cleanId.substringBefore("@") else ""
    val cleanName = p.name.lowercase().trim().replace(" ", "").replace("_", "")
    val cleanCompany = p.companyName.lowercase().trim().replace(" ", "").removeSuffix("holding").removeSuffix("a.ş.").removeSuffix("inc")

    return when {
        emailKey.isNotBlank() && emailKey != "local" && emailKey != "misafir_tuccar" -> "email_$emailKey"
        cleanName.isNotBlank() && cleanName != "oyuncu" && cleanName != "tüccar" && cleanName != "tuccar" -> "name_$cleanName"
        cleanCompany.isNotBlank() && cleanCompany != "tüccar" -> "company_$cleanCompany"
        else -> "id_$cleanId"
    }
}

@Composable
fun SocialScreen(
    uiState: com.example.viewmodel.GameUiState,
    onIntent: (com.example.viewmodel.GameIntent) -> Unit,
    viewModel: GameViewModel? = null
) {
    val theme = LocalAppThemeOption.current
    val isOnlineRegistered = uiState.settingsState.isOnlineRegistered
    val isGoogleSignedIn = uiState.settingsState.isGoogleSignedIn
    val localPlayerState = uiState.playerState.player
    val localBusinesses = uiState.businesses

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    var selectedPlayerForDetail by remember { mutableStateOf<OnlinePlayer?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: CANLI LİG (Mevcut Ay), 1: GEÇMİŞ AY SONUÇLARI (Top 20)

    LaunchedEffect(Unit) {
        MultiplayerManager.listenToLeaderboard()
    }

    val onlinePlayers by MultiplayerManager.onlinePlayers.collectAsStateWithLifecycle()
    val pastMonthLeaderboard by MultiplayerManager.pastMonthLeaderboard.collectAsStateWithLifecycle()

    val botIds = remember { setOf("BOT-KAYA-01", "BOT-NOVA-02", "BOT-TOROS-03", "BOT-EGE-04", "BOT-AVRASYA-05", "BOT-ANADOLU-05") }
    val botNames = remember { setOf("Selim Kaya", "Dr. Aylin Soylu", "Burak Demirci", "Zehra Aydın", "Hakan Erkin", "Defne Aras", "Kaan Yıldırım") }

    fun isBotPlayer(player: OnlinePlayer): Boolean {
        return player.id.isBlank() ||
               player.id == "local" ||
               player.id == "misafir_tuccar" ||
               player.id.startsWith("guest", ignoreCase = true) ||
               player.id.startsWith("BOT-", ignoreCase = true) ||
               player.id.startsWith("BOT_", ignoreCase = true) ||
               player.id.contains("bot", ignoreCase = true) ||
               player.id in botIds ||
               player.name in botNames ||
               com.example.data.BotTycoonManager.getAllBots().any {
                   it.id.equals(player.id, ignoreCase = true) ||
                   it.name.equals(player.name, ignoreCase = true)
               }
    }

    val isEnglish = isEnglishLanguage()

    // Calculate current & past month display strings
    val currentMonthName = remember(isEnglish) {
        val locale = if (isEnglish) Locale.ENGLISH else Locale("tr", "TR")
        val sdf = SimpleDateFormat("MMMM yyyy", locale)
        sdf.format(Calendar.getInstance().time).uppercase(locale)
    }

    val pastMonthName = remember(isEnglish) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val locale = if (isEnglish) Locale.ENGLISH else Locale("tr", "TR")
        val sdf = SimpleDateFormat("MMMM yyyy", locale)
        sdf.format(cal.time).uppercase(locale)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // --- SCREEN HEADER ---
        Box(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    CurrencyText(
                        text = tr("AYLIK HOLDİNG SIRALAMASI", "MONTHLY HOLDING LEADERBOARD"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = theme.primaryColor
                    )
                    CurrencyText(
                        text = tr("Lider Ticaret Holdingleri Ligi & Elmas Ödülleri", "Leading Trade Holdings League & Diamond Awards"),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondaryColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = ThemePositive.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ThemePositive.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(14.dp))
                        CurrencyText(
                            text = tr("CANLI SÜREÇ", "LIVE PROCESS"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemePositive,
                            fontFamily = RobotoMonoFontFamily
                        )
                    }
                }
            }
        }

        // --- TAB SELECTOR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (theme.isDark) Color(0xFF10192A) else theme.surfaceVariantColor)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (selectedTab == 0) theme.primaryColor else Color.Transparent
                    )
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Leaderboard,
                        contentDescription = null,
                        tint = if (selectedTab == 0) Color.White else theme.textSecondaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    CurrencyText(
                        text = tr("CANLI LİG ($currentMonthName)", "LIVE LEAGUE ($currentMonthName)"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (selectedTab == 0) Color.White else theme.textSecondaryColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (selectedTab == 1) theme.primaryColor else Color.Transparent
                    )
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (selectedTab == 1) Color.White else theme.textSecondaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    CurrencyText(
                        text = tr("GEÇMİŞ AY (TOP 20)", "PAST MONTH (TOP 20)"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = if (selectedTab == 1) Color.White else theme.textSecondaryColor
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.img_leaderboard_hero),
                            contentDescription = "Leaderboard Hero Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.1f),
                                            Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeGold,
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                CurrencyText(
                                    text = tr("GLOBAL LİG", "GLOBAL LEAGUE"),
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = RobotoMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            CurrencyText(
                                text = tr("TÜRKİYE SANAYİ VE TİCARET LİGİ", "TURKISH INDUSTRY AND TRADE LEAGUE"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = RobotoMonoFontFamily
                            )
                            CurrencyText(
                                text = tr("En büyük holdingler arasında zirve yarışı ve elmas ödülleri", "The race for the top among the largest holdings and diamond rewards"),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // --- CURRENT MONTH LEAGUE BANNER ---
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor,
                        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemeGold, theme.primaryColor)))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
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
                                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(22.dp))
                                    CurrencyText(
                                        text = tr("$currentMonthName DÖNEMİ LİGİ", "$currentMonthName LEAGUE PERIOD"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = theme.textPrimaryColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeGold.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, ThemeGold)
                                ) {
                                    CurrencyText(
                                        text = tr("CANLI SIRALAMA", "LIVE RANKINGS"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Divider(color = theme.borderColor)

                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(18.dp))
                                Column {
                                    CurrencyText(
                                        text = tr("Sıralama Mantığı & Ay Sonu Elmas Ödülleri:", "Ranking Logic & End of Month Diamond Rewards:"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primaryColor,
                                        fontFamily = RobotoMonoFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    CurrencyText(
                                        text = tr("Holding Sıralaması = Oyuncuların güncel Net Şirket Değerine (Nakit, Banka Mevduatı, Döviz, Fabrikalar, Depo Malları ve Konsorsiyum Varlıkları) göre büyükten küçüğe oluşturulur.\nAy sonunda ilk 3'e giren holdinglere elmas ödülleri aktarılır:\n🥇 1. 2000 💎  |  🥈 2. 1000 💎  |  🥉 3. 500 💎", "Holding Leaderboard = Ranked descending by players' current Net Company Valuation (Cash, Bank Deposits, FX, Factories, Warehouse Stocks, and Consortium Assets).\nAt month end, top 3 holdings receive diamond rewards:\n🥇 1st: 2000 💎  |  🥈 2nd: 1000 💎  |  🥉 3rd: 500 💎"),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = theme.textSecondaryColor
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (!isGoogleSignedIn) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isEnglish) "Please sign in with Google to claim monthly leaderboard rewards." else "Aylık sıralama ödüllerini toplamak için lütfen Google ile giriş yapınız.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        viewModel?.handleIntent(com.example.viewmodel.GameIntent.CheckAndClaimMonthlyLeaderboardReward(true))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.primaryColor
                                ),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Diamond, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                                    CurrencyText(
                                        text = tr("ÖDÜL HAK EDİŞİ SORGULA / TOPLA", "CHECK / CLAIM REWARD ELIGIBILITY"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // OFFLINE GUEST NOTICE BANNER (Only when not signed in with Google)
                if (!isGoogleSignedIn) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (theme.isDark) Color(0xFF131D31) else Color(0xFFF0F4F9)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    CurrencyText(
                                        text = tr("Çevrimdışı Mod (Yalnızca Cihaz Belleği)", "Offline Mode (Device Memory Only)"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = theme.textPrimaryColor
                                    )
                                }
                                CurrencyText(
                                    text = tr(
                                        "Google hesabı ile giriş yapılmadığında ilerlemeleriniz sunucuya aktarılmaz ve oyun cihaz belleğinde çevrimdışı çalışır. Aylık holding liginde yer almak, küresel sıralamaya girmek ve ay sonu Elmas ödüllerini toplamak için Google hesabınızla giriş yapabilirsiniz.",
                                        "When not signed in with Google, your progress is not synced to the cloud server and runs offline on device memory. To participate in the monthly holding league, appear on the leaderboard, and claim month-end Diamond rewards, sign in with your Google account."
                                    ),
                                    fontSize = 11.sp,
                                    color = theme.textSecondaryColor,
                                    lineHeight = 15.sp
                                )
                                if (isGoogleSigningIn) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color(0xFF4285F4),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (viewModel != null) {
                                                com.example.utils.GoogleAuthHelper.launchGoogleSignIn(
                                                    context = context,
                                                    scope = scope,
                                                    viewModel = viewModel,
                                                    onStart = { isGoogleSigningIn = true },
                                                    onComplete = { success, msg ->
                                                        isGoogleSigningIn = false
                                                        val msgText = if (success) {
                                                            if (isEnglish) "Signed in with Google!" else "Google ile giriş yapıldı!"
                                                        } else {
                                                            msg ?: (if (isEnglish) "Sign-in failed" else "Giriş yapılamadı")
                                                        }
                                                        android.widget.Toast.makeText(context, msgText, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                        shape = RoundedCornerShape(3.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            CurrencyText(
                                                text = tr("Google ile Giriş Yap & Sıralamaya Katıl", "Sign In With Google & Join Leaderboard"),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CURRENT MONTH LEADERBOARD ITEMS (SORTED BY NET COMPANY VALUATION DESCENDING)
                // Sadece gerçek oyuncular sıralanır; bot oyuncular sıralama listelerinde yer almaz

                val validOnlinePlayers = onlinePlayers
                    .filter { player -> !isBotPlayer(player) }
                    .groupBy { getCleanPlayerKey(it) }
                    .map { (_, list) -> list.maxByOrNull { it.netWorth }!! }

                val rawSortedPlayers = if (isGoogleSignedIn && localPlayerState != null) {
                    val myId = localPlayerState.id
                    val myName = localPlayerState.name
                    val myCleanKey = getCleanPlayerKey(OnlinePlayer(id = myId, name = myName, companyName = "${myName} Holding", netWorth = 0L, city = "istanbul", level = 1))
                    val myNetWorth = if (uiState.netWorth > 0L) uiState.netWorth else (viewModel?.calculateCompanyValuation() ?: ((localPlayerState.money + localPlayerState.depositBalance + localPlayerState.lockedDepositBalance - localPlayerState.loanAmount).coerceAtLeast(0L)))
                    val existsInList = validOnlinePlayers.any { getCleanPlayerKey(it) == myCleanKey || it.id == myId || it.name.equals(myName, ignoreCase = true) }
                    val baseList = if (existsInList) {
                        validOnlinePlayers.map { player ->
                            if (getCleanPlayerKey(player) == myCleanKey || player.id == myId || player.name.equals(myName, ignoreCase = true)) {
                                val myGrowth = if (player.monthlyScore > 0L) player.monthlyScore else (myNetWorth * 0.20f).toLong()
                                player.copy(id = myId, name = myName, netWorth = myNetWorth, monthlyScore = myGrowth)
                            } else {
                                val growth = if (player.monthlyScore > 0L) player.monthlyScore else (player.netWorth * 0.20f).toLong()
                                player.copy(monthlyScore = growth)
                            }
                        }
                    } else {
                        val myGrowth = (myNetWorth * 0.20f).toLong()
                        validOnlinePlayers.map { player ->
                            val growth = if (player.monthlyScore > 0L) player.monthlyScore else (player.netWorth * 0.20f).toLong()
                            player.copy(monthlyScore = growth)
                        } + OnlinePlayer(
                            id = myId,
                            name = myName,
                            companyName = "${myName} Holding",
                            netWorth = myNetWorth,
                            city = localPlayerState.currentCity,
                            level = localPlayerState.level,
                            isOnline = true,
                            badge = if (localPlayerState.level > 15) "CEO" else if (localPlayerState.level > 10) "LİDER" else "TÜCCAR",
                            bankBalance = localPlayerState.depositBalance,
                            monthlyScore = myGrowth
                        )
                    }
                    baseList.sortedByDescending { it.netWorth }
                } else {
                    // Google girişi yoksa oyuncu çevrimdışı yerel belleğinde oynar; sadece sunucudaki kayıtlı oyuncular listelenir
                    validOnlinePlayers.map { player ->
                        val growth = if (player.monthlyScore > 0L) player.monthlyScore else (player.netWorth * 0.20f).toLong()
                        player.copy(monthlyScore = growth)
                    }.sortedByDescending { it.netWorth }
                }

                val sortedPlayers = rawSortedPlayers
                    .groupBy { getCleanPlayerKey(it) }
                    .map { (_, list) -> list.maxByOrNull { it.netWorth }!! }
                    .sortedByDescending { it.netWorth }

                if (sortedPlayers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = ThemeNeonCyan,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                CurrencyText(
                                    text = tr("Liderlik Tablosu Verileri Yükleniyor...", "Loading Leaderboard Data..."),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = theme.textPrimaryColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                CurrencyText(
                                    text = tr("Zirve yarışı devam ediyor. İlk sıralara adını yazdırmak için holdingini büyüt!", "The race to the top is on. Grow your holding to claim the top spots!"),
                                    fontSize = 11.sp,
                                    color = theme.textSecondaryColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(sortedPlayers.size, key = { "lb_rank_${it}_${getCleanPlayerKey(sortedPlayers[it])}" }) { index ->
                    val player = sortedPlayers[index]
                    val score = player.monthlyScore
                    val isMe = (player.id == localPlayerState?.id || player.name == localPlayerState?.name)

                    // Enrich player details for local user if clicked
                    val displayPlayer = if (isMe && localPlayerState != null) {
                        player.copy(
                            netWorth = player.netWorth,
                            city = localPlayerState!!.currentCity.uppercase(),
                            centralWarehouseLocation = tr("${localPlayerState!!.currentCity.uppercase()} Ana Lojistik Merkezi", "${localPlayerState!!.currentCity.uppercase()} Main Logistics Center"),
                            facilities = if (localBusinesses.isNotEmpty()) {
                                localBusinesses.map { b ->
                                    PlayerFacilityInfo(
                                        name = b.type.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                                        city = b.cityId.uppercase(),
                                        level = b.level,
                                        category = tr("Tesis", "Facility")
                                    )
                                }
                            } else player.facilities
                        )
                    } else player

                    EliteLeaderboardCard(
                        rank = index + 1,
                        player = displayPlayer,
                        score = score,
                        isMe = isMe,
                        onClick = {
                            selectedPlayerForDetail = displayPlayer
                        }
                    )
                }
            }
        } else {
            // --- PAST MONTH RESULTS BANNER & TOP 20 ---
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor,
                        border = BorderStroke(1.5.dp, ThemeGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
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
                                    Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                                    CurrencyText(
                                        text = tr("$pastMonthName SONUÇLARI", "$pastMonthName RESULTS"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = theme.textPrimaryColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeGold.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, ThemeGold)
                                ) {
                                    CurrencyText(
                                        text = tr("TOP 20 DERECE", "TOP 20 RANKS"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Divider(color = theme.borderColor)

                            CurrencyText(
                                text = tr("Geçmiş ayın en başarılı ilk 20 holdingi aşağıda sıralanmıştır. İlk 3 sırada tamamlayan kullanıcıların hesabına elmas ödülleri otomatik tanımlanmıştır:", "The top 20 most successful holdings of the past month are listed below. Diamond rewards have been automatically credited to the top 3 users:"),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = theme.textSecondaryColor
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = ThemeGold.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ThemeGold)
                                ) {
                                    CurrencyText("🥇 1. 2000 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeGold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFFE0E0E0).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                ) {
                                    CurrencyText("🥈 2. 1000 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (theme.isDark) Color.White else Color.DarkGray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFFCD7F32).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFCD7F32))
                                ) {
                                    CurrencyText("🥉 3. 500 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCD7F32), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                val sortedPastPlayers = pastMonthLeaderboard
                    .filter { !isBotPlayer(it) }
                    .groupBy { getCleanPlayerKey(it) }
                    .map { (_, list) -> list.maxByOrNull { it.netWorth }!! }
                    .sortedByDescending { it.netWorth }
                    .take(20)

                if (sortedPastPlayers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor),
                            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = ThemeGold,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                CurrencyText(
                                    text = tr("Geçmiş Ay Dereceleri Henüz Arşivlenmedi", "Past Month Ranks Not Archived Yet"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = theme.textPrimaryColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                CurrencyText(
                                    text = tr("İlk dönemin tamamlanmasının ardından ödül kazanan şampiyonlar burada listelenecektir.", "Once the first period is completed, reward-winning champions will be listed here."),
                                    fontSize = 11.sp,
                                    color = theme.textSecondaryColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(sortedPastPlayers.size, key = { sortedPastPlayers[it].id }) { index ->
                    val player = sortedPastPlayers[index]
                    val isMe = (player.id == localPlayerState?.id || player.name == localPlayerState?.name)

                    EliteLeaderboardCard(
                        rank = index + 1,
                        player = player,
                        score = player.monthlyScore,
                        isMe = isMe,
                        isPastMonth = true,
                        onClick = {
                            selectedPlayerForDetail = player
                        }
                    )
                }
            }
        }
    }
}

    // PLAYER DETAIL DIALOG / BOTTOM SHEET CARD
    selectedPlayerForDetail?.let { player ->
        val isMe = (player.id == localPlayerState?.id || player.name == localPlayerState?.name)
        OnlinePlayerDetailDialog(
            player = player,
            isMe = isMe,
            onDismiss = { selectedPlayerForDetail = null }
        )
    }
}

private data class LeaderboardCardStyle(
    val borderBrush: Brush,
    val rankColor: Color,
    val bgContainer: Color,
    val rankBadge: String
)

@Composable
fun EliteLeaderboardCard(
    rank: Int,
    player: OnlinePlayer,
    score: Long,
    isMe: Boolean = false,
    isPastMonth: Boolean = false,
    onClick: () -> Unit
) {
    val theme = LocalAppThemeOption.current

    val style = when (rank) {
        1 -> LeaderboardCardStyle(
            borderBrush = Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFF099), Color(0xFFFFAB00))),
            rankColor = ThemeGold,
            bgContainer = if (theme.isDark) Color(0xFF1E1703) else Color(0xFFFFFDF5),
            rankBadge = if (isPastMonth) tr("🏆 GEÇMİŞ AY ŞAMPİYONU (+2000 💎)", "🏆 PAST MONTH CHAMPION (+2000 💎)") else tr("👑 ŞAMPİYON HOLDİNG", "👑 CHAMPION HOLDING")
        )
        2 -> LeaderboardCardStyle(
            borderBrush = Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFF78909C))),
            rankColor = Color(0xFFB0BEC5),
            bgContainer = if (theme.isDark) Color(0xFF11171A) else Color(0xFFF1F5F9),
            rankBadge = if (isPastMonth) tr("🥈 GEÇMİŞ AY 2.'Sİ (+1000 💎)", "🥈 PAST MONTH 2ND (+1000 💎)") else tr("🥈 ELİT SEVİYE HOLDİNG", "🥈 ELITE LEVEL HOLDING")
        )
        3 -> LeaderboardCardStyle(
            borderBrush = Brush.horizontalGradient(listOf(Color(0xFFCD7F32), Color(0xFFFFB07C), Color(0xFFA0522D))),
            rankColor = Color(0xFFCD7F32),
            bgContainer = if (theme.isDark) Color(0xFF1B110B) else Color(0xFFFFFBF7),
            rankBadge = if (isPastMonth) tr("🥉 GEÇMİŞ AY 3.'SÜ (+500 💎)", "🥉 PAST MONTH 3RD (+500 💎)") else tr("🥉 LİDER SEVİYE HOLDİNG", "🥉 LEADING LEVEL HOLDING")
        )
        else -> LeaderboardCardStyle(
            borderBrush = Brush.horizontalGradient(listOf(theme.borderColor, theme.borderColor)),
            rankColor = theme.textSecondaryColor,
            bgContainer = if (isMe) {
                if (theme.isDark) Color(0xFF0F2027) else Color(0xFFF0F9FF)
            } else {
                theme.surfaceColor
            },
            rankBadge = ""
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(4.dp),
        border = if (rank <= 3) {
            BorderStroke(2.dp, style.borderBrush)
        } else if (isMe) {
            BorderStroke(1.5.dp, theme.primaryColor)
        } else {
            BorderStroke(1.dp, theme.borderColor)
        },
        colors = CardDefaults.cardColors(containerColor = style.bgContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rank <= 3) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700).copy(alpha = 0.15f)
                            2 -> Color(0xFFB0BEC5).copy(alpha = 0.15f)
                            3 -> Color(0xFFCD7F32).copy(alpha = 0.15f)
                            else -> theme.surfaceVariantColor
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when (rank) {
                            1 -> Color(0xFFFFD700).copy(alpha = 0.4f)
                            2 -> Color(0xFFB0BEC5).copy(alpha = 0.4f)
                            3 -> Color(0xFFCD7F32).copy(alpha = 0.4f)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (rank) {
                    1 -> Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(28.dp))
                    2 -> Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(28.dp))
                    3 -> Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = Color(0xFFCD7F32), modifier = Modifier.size(28.dp))
                    else -> CurrencyText(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        color = theme.textPrimaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Player Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Online/Offline status dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (player.isOnline) ThemePositive else Color.Gray)
                    )

                    CurrencyText(
                        text = player.companyName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = theme.textPrimaryColor,
                        maxLines = 1
                    )
                    
                    if (isMe) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = theme.primaryColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, theme.primaryColor)
                        ) {
                            CurrencyText(
                                text = tr("SİZ", "YOU"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontFamily = RobotoMonoFontFamily,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Holding Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (theme.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        border = BorderStroke(0.5.dp, if (theme.isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ) {
                        CurrencyText(
                            text = tr(player.badge, player.badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Player Name & City
                    CurrencyText(
                        text = "${player.name} • ${player.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textSecondaryColor,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                if (style.rankBadge.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = style.rankColor.copy(alpha = 0.1f)
                    ) {
                        CurrencyText(
                            text = style.rankBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = style.rankColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Primary Metric: Net Company Valuation (and secondary Growth diff)
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (rank == 1) ThemeGold.copy(alpha = 0.2f) else ThemePositive.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (rank == 1) ThemeGold else ThemePositive.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = if (rank == 1) ThemeGold else ThemePositive,
                            modifier = Modifier.size(13.dp)
                        )
                        CurrencyText(
                            text = "₳${formatCredit(player.netWorth)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            color = if (rank == 1) ThemeGold else ThemePositive,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    CurrencyText(
                        text = tr("Şirket Değeri", "Net Worth"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily,
                        color = theme.textSecondaryColor
                    )
                    if (score > 0L) {
                        CurrencyText(
                            text = "• (+${formatCredit(score)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemePositive
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnlinePlayerDetailDialog(
    player: OnlinePlayer,
    isMe: Boolean = false,
    onDismiss: () -> Unit
) {
    val theme = LocalAppThemeOption.current
    val context = LocalContext.current

    // Trigger visitor ticket inspection mechanic
    LaunchedEffect(player.id) {
        MuseumHeritageManager.recordProfileInspection(context, visitorName = player.companyName)
    }

    val artifacts = remember(player.id) {
        if (isMe) MuseumHeritageManager.getOwnedArtifacts(context)
        else MuseumHeritageManager.getArtifactsForOnlinePlayer(player.id, player.level)
    }
    val museumPrestige = remember(player.id) {
        if (isMe) MuseumHeritageManager.getTotalMuseumPrestige(context)
        else artifacts.sumOf { it.prestigeScore }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (theme.isDark) Color(0xFF10192A) else theme.surfaceColor
            ),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ThemeGold, theme.primaryColor)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // TOP HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(theme.primaryColor.copy(alpha = 0.15f))
                                .border(1.5.dp, theme.primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CurrencyText(
                                text = player.badge.take(1),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = theme.primaryColor
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CurrencyText(
                                    text = player.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimaryColor
                                )
                                if (isMe) {
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = ThemeGold
                                    ) {
                                        CurrencyText(
                                            text = tr("SEN", "YOU"),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            CurrencyText(
                                text = tr("Yönetici: ${player.name} (${player.badge})", "Manager: ${player.name} (${player.badge})"),
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.textSecondaryColor
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = tr("Kapat", "Close"), tint = theme.textSecondaryColor)
                    }
                }

                Divider(color = theme.borderColor)

                // 2x2 CORE STATS GRID
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // MERKEZİ DEPO KONUMU
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Store, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(14.dp))
                                    CurrencyText(tr("MERKEZİ DEPO", "CENTRAL WAREHOUSE"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor, fontFamily = RobotoMonoFontFamily)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                CurrencyText(
                                    text = player.centralWarehouseLocation,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.textPrimaryColor
                                )
                            }
                        }

                        // NET ŞİRKET DEĞERİ
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Business, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(14.dp))
                                    CurrencyText(tr("NET ŞİRKET DEĞERİ", "COMPANY NET WORTH"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                CurrencyText(
                                    text = formatCurrency(player.netWorth, isEnglishLanguage()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.textPrimaryColor,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ŞİRKET DEĞERİ
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(14.dp))
                                    CurrencyText(tr("ŞİRKET DEĞERİ", "COMPANY VALUATION"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemeGold, fontFamily = RobotoMonoFontFamily)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                CurrencyText(
                                    text = formatCredit(player.netWorth),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimaryColor
                                )
                            }
                        }

                        // AYLIK ŞİRKET BÜYÜMESİ
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor,
                            border = BorderStroke(1.dp, theme.borderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = ThemePositive, modifier = Modifier.size(14.dp))
                                    CurrencyText(tr("AYLIK ŞİRKET BÜYÜMESİ", "MONTHLY GROWTH"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemePositive, fontFamily = RobotoMonoFontFamily)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                CurrencyText(
                                    text = "+${formatCredit(player.monthlyScore)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.textPrimaryColor,
                                    fontFamily = RobotoMonoFontFamily
                                )
                            }
                        }
                    }
                }

                Divider(color = theme.borderColor)

                // AHİLİK MİRASI MÜZESİ & SERGİLENEN ESERLER
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("🏛️ AHİLİK MİRASI MÜZESİ (${artifacts.size} Eser)", "🏛️ AHILIK HERITAGE MUSEUM (${artifacts.size} Artifacts)"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeGold
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThemeGold.copy(alpha = 0.15f)
                        ) {
                            CurrencyText(
                                text = tr("+$museumPrestige Prestij Puanı", "+$museumPrestige Prestige Points"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Ticket Revenue Explanatory Note
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F1E33),
                        border = BorderStroke(0.5.dp, ThemeGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(14.dp)
                            )
                            CurrencyText(
                                text = if (isMe) tr("Profilinizi inceleyen diğer oyuncular müzenize bilet geliri kazandırır! Biriken hasılatınızı Ana Menü > Ahilik Müzesi bölümünden tahsil edebilirsiniz.", "Other players reviewing your profile earn ticket revenue for your museum! You can collect your accumulated revenue from the Main Menu > Ahilik Museum section.")
                                else tr("Holdingin sergilediği tarihi Ahilik şaheserleri, profil incelemeleri ve müze ziyaretlerinden düzenli bilet geliri üretir.", "The historical Ahilik masterpieces exhibited by the holding generate regular ticket revenue from profile reviews and museum visits."),
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    if (artifacts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor
                        ) {
                            CurrencyText(
                                text = tr("Bu holding henüz müzesine tarihi Ahilik eseri dahil etmedi.", "This holding has not included historical Ahilik artifacts in its museum yet."),
                                fontSize = 11.sp,
                                color = theme.textSecondaryColor,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            artifacts.forEach { artifact ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor,
                                    border = BorderStroke(1.dp, Color(artifact.rarity.colorHex).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CurrencyText(artifact.iconEmoji, fontSize = 20.sp)
                                            Column {
                                                CurrencyText(
                                                    text = artifact.getLocalizedName(isEnglishLanguage()),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textPrimaryColor
                                                )
                                                CurrencyText(
                                                    text = "${artifact.getLocalizedEra(isEnglishLanguage())} • 📍 ${artifact.getLocalizedOriginCity(isEnglishLanguage())}",
                                                    fontSize = 10.sp,
                                                    color = theme.textSecondaryColor
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(artifact.rarity.colorHex).copy(alpha = 0.15f)
                                        ) {
                                            CurrencyText(
                                                text = tr("${artifact.rarity.badgeEmoji} +${artifact.prestigeScore} P", "${artifact.rarity.badgeEmoji} +${artifact.prestigeScore} PTS"),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(artifact.rarity.colorHex),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = theme.borderColor)

                // FACILITIES & LOCATIONS LIST
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("🏭 TESİSLER VE LOKASYONLARI (${player.facilities.size})", "🏭 FACILITIES AND LOCATIONS (${player.facilities.size})"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = theme.primaryColor
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = theme.primaryColor.copy(alpha = 0.15f)
                        ) {
                            CurrencyText(
                                text = tr("${player.facilities.size} Faal Tesis", "${player.facilities.size} Active Facilities"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (player.facilities.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor
                        ) {
                            CurrencyText(
                                text = tr("Bu holding henüz dış şehirlere fabrika açmadı.", "This holding has not opened factories in other cities yet."),
                                fontSize = 11.sp,
                                color = theme.textSecondaryColor,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            player.facilities.forEach { facility ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (theme.isDark) Color(0xFF162136) else theme.surfaceVariantColor,
                                    border = BorderStroke(1.dp, theme.borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Business,
                                                contentDescription = null,
                                                tint = theme.primaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                CurrencyText(
                                                    text = facility.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textPrimaryColor
                                                )
                                                CurrencyText(
                                                    text = tr("Lokasyon: ${facility.city} • Seviye ${facility.level}", "Location: ${facility.city} • Level ${facility.level}"),
                                                    fontSize = 10.sp,
                                                    color = theme.textSecondaryColor
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThemeGold.copy(alpha = 0.15f)
                                        ) {
                                            CurrencyText(
                                                text = facility.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemeGold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FOOTER BUTTON
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    CurrencyText(tr("KAPAT", "CLOSE"), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
