package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CompanyManager
import com.example.data.ManagerActionLog
import com.example.data.PlayerEntity
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * İnsan Kaynakları Rehberli Menü Sekmeleri
 */
enum class HrGuidedStep(
    val stepIndex: Int,
    val icon: ImageVector,
    val titleTr: String,
    val titleEn: String,
    val badgeTr: String,
    val badgeEn: String,
    val accentColor: Color
) {
    OVERVIEW(
        0,
        Icons.Rounded.Dashboard,
        "Karargah & C-Suite",
        "HQ & C-Suite",
        "1. Karargah",
        "1. HQ",
        Color(0xFFF59E0B) // Amber Gold
    ),
    STAFFING(
        1,
        Icons.Rounded.Badge,
        "Müdürler & Atama",
        "Directors & Staff",
        "2. Kadro",
        "2. Staff",
        Color(0xFF06B6D4) // Cyan
    ),
    HIERARCHY(
        2,
        Icons.Rounded.AccountTree,
        "Hiyerarşi & Onay",
        "Hierarchy & Scale",
        "3. Hiyerarşi",
        "3. Hierarchy",
        Color(0xFFA855F7) // Purple
    ),
    AUDIT_LOGS(
        3,
        Icons.Rounded.HistoryEdu,
        "Karar Kayıtları",
        "Decision Logs",
        "4. Denetim",
        "4. Audit",
        Color(0xFF10B981) // Emerald Green
    ),
    EFFICIENCY(
        4,
        Icons.Rounded.AutoGraph,
        "Verim & Akademi",
        "Efficiency & KPIs",
        "5. Akademi",
        "5. Academy",
        Color(0xFF38BDF8) // Sky Blue
    )
}

/**
 * 1. ADIM ADIM İK REHBERLİ ÜST MENÜ (STEPPER / SEGMENTED TABS)
 */
@Composable
fun HrGuidedStepBar(
    currentStep: HrGuidedStep,
    onStepSelected: (HrGuidedStep) -> Unit,
    hiredCount: Int,
    totalCount: Int,
    averageEfficiency: Float,
    recentLogsCount: Int,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    // Breathing glow animation for active step
    val infiniteTransition = rememberInfiniteTransition(label = "hr_step_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hrPulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF08101E),
        border = BorderStroke(1.2.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HrGuidedStep.values().forEach { step ->
                val isSelected = currentStep == step
                val badgeCountText = when (step) {
                    HrGuidedStep.OVERVIEW -> "$hiredCount/$totalCount"
                    HrGuidedStep.STAFFING -> if (totalCount - hiredCount > 0) "${totalCount - hiredCount} Boş" else "✓ Tam"
                    HrGuidedStep.HIERARCHY -> "#1 - #5"
                    HrGuidedStep.AUDIT_LOGS -> if (recentLogsCount > 0) "$recentLogsCount Kayıt" else "0"
                    HrGuidedStep.EFFICIENCY -> "%${averageEfficiency.toInt()}"
                }

                val targetBorderColor = when {
                    isSelected -> step.accentColor.copy(alpha = pulseAlpha)
                    else -> Color(0xFF1E2D4A)
                }

                val targetBgColor = when {
                    isSelected -> step.accentColor.copy(alpha = 0.20f)
                    else -> Color(0xFF0F1A2D)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = targetBgColor,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, targetBorderColor),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStepSelected(step)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = step.accentColor.copy(alpha = 0.25f),
                                    modifier = Modifier.size(24.dp)
                                ) {}
                            }
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = if (isSelected) step.accentColor else Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CurrencyText(
                                    text = if (isEng) step.badgeEn else step.badgeTr,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) step.accentColor else Color.Gray
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = step.accentColor,
                                        modifier = Modifier.size(4.dp)
                                    ) {}
                                }
                            }

                            CurrencyText(
                                text = if (isEng) step.titleEn else step.titleTr,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isSelected) step.accentColor.copy(alpha = 0.22f) else Color(0xFF1E293B),
                            border = BorderStroke(0.8.dp, if (isSelected) step.accentColor.copy(alpha = 0.6f) else Color(0xFF334155))
                        ) {
                            CurrencyText(
                                text = badgeCountText,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) step.accentColor else Color.LightGray,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. C-SUITE KARARGAH HERO KARTI (EXECUTIVE KPI SUMMARY)
 */
@Composable
fun HrExecutiveHeroCard(
    hiredCount: Int,
    totalCount: Int,
    totalDailySalaries: Long,
    averageEfficiency: Float,
    playerGems: Int,
    treasuryReserve: Long,
    onShowEfficiencyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0B1424),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    ThemeGold.copy(alpha = 0.7f),
                    ThemeNeonCyan.copy(alpha = 0.7f),
                    Color(0xFFA855F7).copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF152238), Color(0xFF091220))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = ThemeGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.2.dp, ThemeGold),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CorporateFare,
                                contentDescription = null,
                                tint = ThemeGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        CurrencyText(
                            text = tr("C-Suite İcra Kurulu & Karargah", "C-Suite Executive Board & HQ"),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            fontFamily = RobotoMonoFontFamily
                        )
                        CurrencyText(
                            text = tr("Holding Üst Düzey Yönetim & Departman Direktörleri", "Holding Executive Leadership & Department Directors"),
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hiredCount > 0) ThemeNeonCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (hiredCount > 0) ThemeNeonCyan else Color.Gray)
                ) {
                    CurrencyText(
                        text = "$hiredCount/$totalCount " + tr("Kadro Dolu", "Staffed"),
                        color = if (hiredCount > 0) ThemeNeonCyan else Color.LightGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

            // 4 KPI Micro Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bordro Yükü
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF101B2E),
                    border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CurrencyText(tr("Bordro Yükü", "Daily Payroll"), color = Color.Gray, fontSize = 8.5.sp)
                        CurrencyText(formatCurrency(totalDailySalaries, isEng), color = Color(0xFFEF4444), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                // Ortalama Verim
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF101B2E),
                    border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onShowEfficiencyDialog() }
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CurrencyText(tr("Yönetim Verimi", "Efficiency"), color = Color.Gray, fontSize = 8.5.sp)
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(10.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText("%${averageEfficiency.toInt()}", color = Color(0xFF34D399), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            LinearProgressIndicator(
                                progress = { (averageEfficiency / 100f).coerceIn(0f, 1f) },
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.height(4.dp).weight(1f).clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                // Elmas Fonu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF101B2E),
                    border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CurrencyText(tr("Elmas Bütçesi", "Gem Budget"), color = Color.Gray, fontSize = 8.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText("$playerGems 💎", color = ThemeNeonCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                // Hazine Rezervi
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF101B2E),
                    border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CurrencyText(tr("Hazine Rezervi", "Treasury Cash"), color = Color.Gray, fontSize = 8.5.sp)
                        CurrencyText(
                            text = if (treasuryReserve > 0L) formatCurrency(treasuryReserve, isEng) else tr("Pasif", "Inactive"),
                            color = if (treasuryReserve > 0L) ThemeGold else Color.Gray,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. KULLANICI İÇİN ANLAŞILIR "SIRADAKİ İK EYLEMİNİZ" REHBERİ
 */
@Composable
fun HrNextActionGuidanceCard(
    managers: List<CompanyManager>,
    playerGems: Int,
    playerMoney: Long,
    onNavigateToStaffing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hiredCount = managers.count { it.isHired }
    val anyLeave = managers.any { it.isHired && !it.isActive }
    val treasury = managers.find { it.id == "mgr_treasury" }
    val promotable = managers.find { it.isHired && it.level < 5 && playerMoney >= (it.dailySalary * 5 * it.level) }

    val (actionTitle, actionDesc, buttonLabel) = when {
        hiredCount == 0 -> Triple(
            tr("🎯 İlk Yöneticiyi İşe Alın", "🎯 Hire Your First Director"),
            tr("Holding hazinesini korumak ve nakit desteği sağlamak için Hazine Müdürü Ayşe Kaya'yı göreve başlatın.", "Start by appointing Treasury Director Ayşe Kaya to safeguard holding reserves and cash liquidity."),
            tr("KADROYA GİT ➔", "GO TO STAFF ➔")
        )
        treasury != null && !treasury.isHired -> Triple(
            tr("👑 Hazine Lideri Eksik!", "👑 Treasury Leader Missing!"),
            tr("Hiyerarşi #1 Hazine Müdürü atanmadı. Holding finansal koruması ve faiz getirileri için işe alın.", "Hierarchy #1 Treasury Director not hired. Appoint to protect funds and unlock interest."),
            tr("İŞE AL ➔", "HIRE NOW ➔")
        )
        anyLeave -> Triple(
            tr("🏖️ Bazı Müdürler İzinli", "🏖️ Some Directors on Leave"),
            tr("İzinli müdürlerin otomasyon döngüleri duraklatıldı. 'Göreve Çağır' ile operasyonları yeniden başlatın.", "Leave status pauses automation loops. Resume work with 'Call to Duty'."),
            tr("YÖNET ➔", "MANAGE ➔")
        )
        promotable != null -> Triple(
            tr("⭐ Terfi Fırsatı!", "⭐ Promotion Available!"),
            tr("${promotable.name} terfiye hazır! Seviye atlatarak departman verimini +%10 artırın.", "${promotable.name} is ready for promotion! Level up to boost department efficiency by +%10."),
            tr("TERFİ ET ➔", "PROMOTE ➔")
        )
        hiredCount < managers.size -> Triple(
            tr("📋 Kadroda Açık Pozisyonlar Var", "📋 Vacant Positions Available"),
            tr("Tüm C-Suite koltuklarını doldurarak holdinginizi tam otomasyona kavuşturun.", "Staff all C-Suite seats to transition your holding into full enterprise autopilot."),
            tr("İNCELE ➔", "EXPLORE ➔")
        )
        else -> Triple(
            tr("✅ Mükemmel Kadro!", "✅ Fully Staffed Enterprise!"),
            tr("Tüm departmanlar aktif, otomasyon döngüleri ve onay zincirleri tam kapasite çalışıyor.", "All departments active, automation loops and approval chains running at full capacity."),
            tr("KADRO ➔", "STAFF ➔")
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0E1C33),
        border = BorderStroke(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.8f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ThemeNeonCyan.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ThemeNeonCyan),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = ThemeNeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    CurrencyText(
                        text = actionTitle,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = ThemeGold
                    )
                    CurrencyText(
                        text = actionDesc,
                        fontSize = 9.5.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            AppButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToStaffing()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                CurrencyText(buttonLabel, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/**
 * 4. VIP YÖNETİCİ KARTI (EXECUTIVE DIRECTOR CARD)
 */
@Composable
fun ExecutiveDirectorCard(
    manager: CompanyManager,
    player: PlayerEntity,
    onHire: () -> Unit,
    onFire: () -> Unit,
    onUpgrade: () -> Unit,
    onToggleActive: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    val specialtyIcon = when (manager.specialty) {
        "production" -> Icons.Rounded.Factory
        "logistics" -> Icons.Rounded.LocalShipping
        "maintenance" -> Icons.Rounded.Build
        "contracts" -> Icons.Rounded.Description
        "rd" -> Icons.Rounded.Science
        "borsa" -> Icons.Rounded.ShowChart
        "hr" -> Icons.Rounded.Group
        else -> Icons.Rounded.AccountBalance
    }

    val localizedTitle = if (isEng) manager.title.trAuto() else (if (manager.titleRes != 0) androidx.compose.ui.res.stringResource(id = manager.titleRes) else manager.title)

    val (hierarchyTag, hierarchyColor) = when (manager.id) {
        "mgr_treasury" -> ("👑 " + tr("Hiyerarşi #1 (Finans)", "Hierarchy #1 (Finance)")) to ThemeGold
        "mgr_contracts" -> ("🥈 " + tr("Hiyerarşi #2 (Tedarik)", "Hierarchy #2 (Procurement)")) to ThemeNeonCyan
        "mgr_borsa" -> ("🥉 " + tr("Hiyerarşi #3 (Borsa)", "Hierarchy #3 (Exchange)")) to Color(0xFFFFB74D)
        "mgr_logistics" -> ("🚚 " + tr("Hiyerarşi #4 (Lojistik)", "Hierarchy #4 (Logistics)")) to Color(0xFF81C784)
        "mgr_hr" -> ("👥 " + tr("Hiyerarşi #5 (İK)", "Hierarchy #5 (HR)")) to Color(0xFFCE93D8)
        else -> ("🛠️ " + tr("Hiyerarşi #5 (Operasyon)", "Hierarchy #5 (Operations)")) to Color(0xFF64B5F6)
    }

    val isHired = manager.isHired
    val containerColor = if (isHired) Color(0xFF101B2E) else Color(0xFF0A1220)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isHired) 1.2.dp else 1.dp, if (isHired) hierarchyColor.copy(alpha = 0.7f) else Color(0xFF1E2D4A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Avatar + Name/Title + Level / Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHired) hierarchyColor.copy(alpha = 0.18f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isHired) hierarchyColor.copy(alpha = 0.8f) else Color(0xFF334155)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isHired) specialtyIcon else Icons.Rounded.PersonOutline,
                                contentDescription = null,
                                tint = if (isHired) hierarchyColor else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CurrencyText(
                                text = if (isHired && manager.name.isNotBlank()) manager.name else tr("AÇIK POZİSYON", "VACANT POSITION"),
                                color = if (isHired) Color.White else ThemeGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        CurrencyText(
                            text = localizedTitle,
                            color = if (isHired) hierarchyColor else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isHired) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Level Stars
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0B1424),
                            border = BorderStroke(0.8.dp, Color(0xFF2E3E5C))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                repeat(manager.level.coerceIn(1, 5)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(10.dp))
                                }
                            }
                        }

                        // Duty Toggle Switch
                        Surface(
                            onClick = onToggleActive,
                            shape = RoundedCornerShape(6.dp),
                            color = if (manager.isActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (manager.isActive) Color(0xFF34D399) else Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (manager.isActive) Color(0xFF34D399) else Color(0xFFEF4444))
                                )
                                CurrencyText(
                                    text = if (manager.isActive) tr("GÖREVDE", "ACTIVE") else tr("İZİNLİ", "LEAVE"),
                                    color = if (manager.isActive) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        CurrencyText(
                            text = tr("Kadro Boş", "Vacant"),
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Key Economics Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF080F1D))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    CurrencyText(
                        text = if (isHired) tr("Günlük Bordro (Maaş)", "Daily Salary") else tr("Başlangıç Maaşı", "Starting Salary"),
                        color = Color.Gray,
                        fontSize = 8.5.sp
                    )
                    CurrencyText(
                        text = formatCurrency(manager.dailySalary, isEng),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (!isHired) {
                        CurrencyText(tr("Transfer Ücreti", "Hiring Fee"), color = ThemeNeonCyan, fontSize = 8.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            CurrencyText("${manager.hireGemCost} 💎", color = ThemeNeonCyan, fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                        }
                    } else {
                        val effectText = when (manager.specialty) {
                            "production" -> tr("Üretim Hızı", "Production Speed") + " +%${manager.level * 25}"
                            "logistics" -> tr("Nakliye İndirimi", "Logistics Discount") + " %${(manager.level * 15).coerceAtMost(75)}"
                            "maintenance" -> tr("Bakım İndirimi", "Maintenance Discount") + " %${(manager.level * 15).coerceAtMost(75)}"
                            "treasury" -> tr("Nakit Rezerv Koruma", "Cash Reserve Protection") + " %${20 + (manager.level * 10)}"
                            "rd" -> tr("Ar-Ge Hızı", "R&D Speed") + " +%${manager.level * 20}"
                            "contracts" -> tr("B2B İhale Verimi", "B2B Tender Efficiency") + " +%${manager.level * 10}"
                            "hr" -> tr("Oto-Terfi & Koçluk", "Auto-Promote & Coach")
                            else -> tr("Oto-Borsa Alımı", "Auto-Stock Buying")
                        }
                        CurrencyText(hierarchyTag, color = hierarchyColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        CurrencyText(effectText, color = ThemeGold, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Live Recent Action Badge
            if (isHired && manager.actionLogs.isNotEmpty()) {
                val latestLog = manager.actionLogs.first()
                val logTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(latestLog.timestampMs))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF060B14),
                    border = BorderStroke(0.8.dp, hierarchyColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(hierarchyColor))
                        CurrencyText(
                            text = "⚡ ${latestLog.description.trAuto(isEng)}",
                            color = Color(0xFFE2E8F0),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        CurrencyText(text = logTimeStr, color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = RobotoMonoFontFamily)
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHired) {
                    val hireGemCost = manager.hireGemCost
                    val hasEnoughGems = player.gems >= hireGemCost

                    AppButton(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                        modifier = Modifier.weight(0.8f).height(32.dp)
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        CurrencyText(tr("Görev Tanımı", "Job Role"), fontSize = 9.sp)
                    }

                    AppButton(
                        onClick = onHire,
                        enabled = hasEnoughGems,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                        modifier = Modifier.weight(1.2f).height(32.dp)
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (hasEnoughGems) Color.Black else ThemeNeonCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(tr("İşe Al", "Hire") + " ($hireGemCost 💎)", fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                    }
                } else {
                    // View Dossier & Logs Button
                    AppButton(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = ThemeNeonCyan),
                        border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(tr("Sicil & Loglar", "Dossier & Logs") + " (${manager.actionLogs.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    // Promote Button
                    if (manager.level < 5) {
                        val upgradeCost = manager.dailySalary * 5 * manager.level
                        val canAfford = player.money >= upgradeCost

                        AppButton(
                            onClick = onUpgrade,
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            CurrencyText(tr("Terfi", "Promote") + " (${formatCredit(upgradeCost)})", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ThemeGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CurrencyText(tr("⭐ Maks Seviye (Lvl 5)", "⭐ Max Level (Lvl 5)"), color = ThemeGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. TIKLANDIĞINDA AÇILAN VIP YÖNETİCİ SİCİL VE KARAR DOSYASI (EXECUTIVE DOSSIER DIALOG)
 */
@Composable
fun ExecutiveDossierDialog(
    manager: CompanyManager,
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onHire: () -> Unit,
    onFire: () -> Unit,
    onUpgrade: () -> Unit,
    onToggleActive: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()
    val localizedTitle = if (isEng) manager.title.trAuto() else (if (manager.titleRes != 0) androidx.compose.ui.res.stringResource(id = manager.titleRes) else manager.title)
    val localizedDesc = if (isEng) manager.description.trAuto() else (if (manager.descriptionRes != 0) androidx.compose.ui.res.stringResource(id = manager.descriptionRes) else manager.description)
    val titleName = if (manager.name.isNotBlank()) manager.name else localizedTitle

    val totalNetFinancialImpact = manager.actionLogs.sumOf { it.financialImpact }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(min = 360.dp, max = 580.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF091220),
            border = BorderStroke(1.5.dp, ThemeNeonCyan.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = ThemeGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.2.dp, ThemeGold),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.AccountBox, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(26.dp))
                            }
                        }

                        Column {
                            CurrencyText(
                                text = titleName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            CurrencyText(
                                text = localizedTitle,
                                fontSize = 9.5.sp,
                                color = ThemeGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2D4A))

                // Performance Scorecard Matrix
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1E36))
                        .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Kıdem Seviyesi", "Level"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText("Lvl ${manager.level}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ThemeGold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Günlük Bordro", "Daily Salary"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText(formatCurrency(manager.dailySalary, isEng), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Toplam Karar", "Actions"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText("${manager.actionLogs.size}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Finansal Katkı", "Impact"), fontSize = 8.sp, color = Color.Gray)
                        val sign = if (totalNetFinancialImpact >= 0) "+" else "-"
                        val color = if (totalNetFinancialImpact >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
                        CurrencyText("$sign${formatCredit(abs(totalNetFinancialImpact))}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
                    }
                }

                // Job Description Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0A1322),
                    border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        CurrencyText(tr("📋 Görev & Yetki Alanı:", "📋 Role & Authority Scope:"), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        CurrencyText(localizedDesc, fontSize = 9.sp, color = Color(0xFFCBD5E1))
                    }
                }

                // Action Logs Stream
                CurrencyText(
                    text = tr("📜 Karar ve Müdahale Sicili:", "📜 Executive Decision & Action Log:"),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF050B14))
                        .border(1.dp, Color(0xFF152238), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (manager.actionLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = tr("Henüz bu yöneticiye ait kayıtlı işlem bulunmuyor.", "No recorded actions for this manager yet."),
                                fontSize = 9.5.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(manager.actionLogs, key = { it.id }) { log ->
                                val impactColor = if (log.financialImpact >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
                                val impactSign = if (log.financialImpact >= 0) "+" else "-"
                                val timeStr = remember(log.timestampMs) {
                                    java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestampMs))
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0D1728),
                                    border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            CurrencyText(text = log.description.trAuto(isEng), fontSize = 9.sp, color = Color.White)
                                            CurrencyText(text = timeStr, fontSize = 7.5.sp, color = Color.Gray)
                                        }
                                        CurrencyText(
                                            text = "$impactSign${formatCredit(abs(log.financialImpact))}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = impactColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Executive Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (manager.isHired) {
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                                onFire()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF450A0A), contentColor = Color(0xFFFCA5A5)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Rounded.PersonRemove, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            CurrencyText(tr("İşten Çıkar", "Fire"), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleActive()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (manager.isActive) Color(0xFF334155) else Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            CurrencyText(if (manager.isActive) tr("İzne Çıkar", "Grant Leave") else tr("İşbaşı Yaptır", "Resume Work"), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        AppButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                                onHire()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeGold, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            CurrencyText(tr("Kadroya Al (${manager.hireGemCost} 💎)", "Hire (${manager.hireGemCost} 💎)"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    AppButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                        modifier = Modifier.weight(0.6f).height(32.dp)
                    ) {
                        CurrencyText(tr("Kapat", "Close"), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

/**
 * 6. KURUMSAL HİYERARŞİ & ONAY SİLSİLESİ ŞEMASI (ORG CHART DIAGRAM)
 */
@Composable
fun HrOrgChartDiagram(
    managers: List<CompanyManager>,
    inflationRate: Float,
    marketPriceRatio: Float,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()

    val hierarchyLevels = listOf(
        Triple("👑 Hiyerarşi #1", tr("Hazine ve Makroekonomi Müdürü (Ayşe Kaya)", "Treasury & Macroeconomics Director"), "mgr_treasury"),
        Triple("🥈 Hiyerarşi #2", tr("Vadeli Sözleşme ve Tedarik Müdürü (Canan Çelik)", "Forward Contracts & Procurement Director"), "mgr_contracts"),
        Triple("🥉 Hiyerarşi #3", tr("Borsa ve Yatırım Analisti (Burak Koç)", "Equities & Investment Analyst"), "mgr_borsa"),
        Triple("🚚 Hiyerarşi #4", tr("Lojistik ve Satış Müdürü (Mehmet Demir)", "Logistics & Global Sales Director"), "mgr_logistics"),
        Triple("👥 Hiyerarşi #5", tr("Operasyon, İK, Ar-Ge & Bakım Departmanları", "Operations, HR, R&D & Maintenance"), "mgr_prod")
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0B1424),
        border = BorderStroke(1.2.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(18.dp))
                    CurrencyText(
                        text = tr("YÖNETİCİ HİYERARŞİSİ & ONAY SİLSİLESİ", "EXECUTIVE HIERARCHY & APPROVAL CHAIN"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Surface(shape = RoundedCornerShape(4.dp), color = ThemeGold.copy(alpha = 0.2f)) {
                    CurrencyText(tr("Alttan Üste Onay", "Bottom-Up Flow"), fontSize = 8.sp, color = ThemeGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }

            CurrencyText(
                text = tr(
                    "Şirket içi operasyonel talepler (üretim malzemesi, bakım, borsa alımı) en alttaki operasyon birimlerinden doğar; Lojistik, Borsa ve Sözleşme süzgecinden geçtikten sonra en üst merci olan Hazine Müdürü tarafından bütçelenir.",
                    "Operational requests initiate at bottom operational tiers, pass through Logistics, Exchange, and Contracts filters, and are ultimately budgeted by Treasury at the top apex."
                ),
                fontSize = 8.5.sp,
                color = Color.LightGray
            )

            // Flow Nodes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                hierarchyLevels.forEachIndexed { index, (tier, title, mgrId) ->
                    val mgr = managers.find { it.id == mgrId }
                    val isHired = mgr?.isHired == true

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHired) Color(0xFF101B2E) else Color(0xFF070E1A),
                        border = BorderStroke(1.dp, if (isHired) ThemeGold.copy(alpha = 0.6f) else Color(0xFF1E2D4A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isHired) ThemeGold.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CurrencyText("${index + 1}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isHired) ThemeGold else Color.Gray)
                                    }
                                }

                                Column {
                                    CurrencyText(tier, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isHired) ThemeGold else Color.Gray)
                                    CurrencyText(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isHired) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B)
                            ) {
                                CurrencyText(
                                    text = if (isHired) tr("Kadroda", "Staffed") else tr("Açık", "Vacant"),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHired) Color(0xFF34D399) else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (index < hierarchyLevels.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = Color(0xFF1E3A5F), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Macroeconomic Multipliers Strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF070F1D),
                border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CurrencyText("📈 " + tr("Enflasyon Endeksi: ", "Inflation: ") + "%${String.format(java.util.Locale.US, "%.1f", inflationRate * 100)}", fontSize = 9.sp, color = ThemeNeonCyan, fontWeight = FontWeight.Bold)
                        CurrencyText("🛍️ " + tr("Piyasa Fiyat Çarpanı: ", "Price Multiplier: ") + "${String.format(java.util.Locale.US, "%.2f", marketPriceRatio)}x", fontSize = 9.sp, color = ThemeGold, fontWeight = FontWeight.Bold)
                    }
                    CurrencyText(
                        tr("Şirket Koruma Kuralı: Enflasyon artsa dahi tavan maaş 400.000 ₳ ile sınırlandırılmıştır. Şirket kasası korunur.", "Budget Safety Rule: Even during hyperinflation, daily salary is capped at ₳400,000 to safeguard cash reserves."),
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 7. YÖNETİM VERİMİ & AKADEMİ PANOSU (EFFICIENCY ACADEMY)
 */
@Composable
fun HrEfficiencyAcademyCard(
    averageEfficiency: Float,
    managers: List<CompanyManager>,
    modifier: Modifier = Modifier
) {
    val benefits = listOf(
        Pair("🏭 " + tr("Üretim Hızı", "Production Speed"), tr("Ahmet Yılmaz (Üretim): Seviye başına +%25 fabrika üretim hızı kazandırır.", "Ahmet Yılmaz (Prod): +25% factory production speed per level.")),
        Pair("🚚 " + tr("Lojistik Tasarrufu", "Logistics Savings"), tr("Mehmet Demir (Lojistik): Seviye başına %15 nakliye ve taşıma indirimi sağlar.", "Mehmet Demir (Logistics): 15% logistics discount per level.")),
        Pair("🛠️ " + tr("Tesis Bakımı", "Facility Maintenance"), tr("Murat Usta (Bakım): Seviye başına %15 fabrika aşınma ve arıza indirimi sağlar.", "Murat Usta (Maintenance): 15% maintenance discount per level.")),
        Pair("🏛️ " + tr("Hazine Koruma Kalkanı", "Treasury Shield"), tr("Ayşe Kaya (Hazine): Şirket mevduatını ve nakit likiditesini banka faizleriyle korur.", "Ayşe Kaya (Treasury): Safeguards deposits and generates passive bank interest.")),
        Pair("🔬 " + tr("Ar-Ge Hızlandırma", "R&D Speed"), tr("Dr. Selin Erdem (Ar-Ge): Seviye başına +%20 araştırma puanı ve teknoloji hızlandırması.", "Dr. Selin Erdem (R&D): +20% research speed per level.")),
        Pair("📋 " + tr("B2B İhale Sözleşmeleri", "B2B Contracts"), tr("Canan Çelik (Sözleşmeler): Otomatik piyasa taraması ve en ucuz hammadde tedariği.", "Canan Çelik (Contracts): Automated market scans and optimal input sourcing."))
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0B1424),
        border = BorderStroke(1.2.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(18.dp))
                    CurrencyText(
                        text = tr("YÖNETİM VERİMİ & DEPARTMAN ETKİLERİ", "EFFICIENCY & DEPARTMENT IMPACTS"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                    CurrencyText("%${averageEfficiency.toInt()} " + tr("Ortalama", "Average"), fontSize = 9.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            CurrencyText(
                text = tr("Her müdürün seviyesi arttıkça holdingin operasyon maliyetleri düşer, üretim ve ticaret gelirleri katlanır:", "As each director's level advances, holding operational expenses decrease while revenues compound:"),
                fontSize = 8.5.sp,
                color = Color.LightGray
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                benefits.forEach { (title, desc) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF070F1D),
                        border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CurrencyText(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                            CurrencyText(desc, fontSize = 8.5.sp, color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }
    }
}
