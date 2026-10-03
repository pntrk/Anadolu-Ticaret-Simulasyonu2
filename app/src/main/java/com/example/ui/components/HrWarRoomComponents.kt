package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompanyManager
import com.example.data.ManagerActionLog
import com.example.data.automation.AutopilotEngine
import com.example.data.automation.SmartDirective
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * 1. C-SUITE İNTERAKTİF İCRA KURULU MASASI (EXECUTIVE BOARDROOM ROUND TABLE)
 * 8 Yönetici koltuğu, holografik merkez ve canlı durum halkaları.
 */
@Composable
fun HrWarRoomBoardTable(
    managers: List<CompanyManager>,
    onSelectManager: (CompanyManager) -> Unit,
    onQuickHire: (CompanyManager) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    // Rotating radar / hologram animation
    val infiniteTransition = rememberInfiniteTransition(label = "boardroom_hologram")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF070E1B),
        border = BorderStroke(1.2.dp, Color(0xFF1E3A5F)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = ThemeGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ThemeGold),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MeetingRoom, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column {
                        CurrencyText(
                            text = tr("C-SUITE İCRA KURULU MASASI", "C-SUITE EXECUTIVE BOARDROOM"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        CurrencyText(
                            text = tr("İnteraktif Yönetim Masası & Departman Koltukları", "Interactive Board Table & Department Seats"),
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                }

                val hiredCount = managers.count { it.isHired }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hiredCount >= 8) Color(0xFF10B981).copy(alpha = 0.2f) else ThemeGold.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, if (hiredCount >= 8) Color(0xFF34D399) else ThemeGold)
                ) {
                    CurrencyText(
                        text = "$hiredCount/8 " + tr("Koltuk Dolu", "Seats Filled"),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (hiredCount >= 8) Color(0xFF34D399) else ThemeGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            // Central Boardroom Table Canvas with Hologram Beam
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF040812),
                border = BorderStroke(1.dp, Color(0xFF152238)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w / 2f, h / 2f)

                        // Outer conference table perimeter (oval / rounded rect)
                        drawRoundRect(
                            color = Color(0xFF0F1A2D),
                            topLeft = Offset(w * 0.12f, h * 0.15f),
                            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.70f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                        )
                        drawRoundRect(
                            color = Color(0xFF1E3A5F),
                            topLeft = Offset(w * 0.12f, h * 0.15f),
                            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.70f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                            style = Stroke(width = 1.5f)
                        )

                        // Central Hologram Ring
                        drawCircle(
                            color = Color(0xFF06B6D4).copy(alpha = 0.25f),
                            radius = 26f,
                            center = center
                        )
                        drawCircle(
                            color = ThemeGold.copy(alpha = 0.6f),
                            radius = 18f,
                            center = center,
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        )

                        // Rotating Radar Beam Line
                        val rad = Math.toRadians(radarAngle.toDouble())
                        val endX = center.x + (cos(rad) * 26f).toFloat()
                        val endY = center.y + (sin(rad) * 26f).toFloat()
                        drawLine(
                            color = Color(0xFF67E8F9),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2f
                        )
                    }

                    // Center Logo / Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                        CurrencyText(
                            text = "HOLDİNG HQ",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 8 Interactive Seat Cards (Arranged in 2 rows of 4)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val row1 = managers.take(4)
                val row2 = managers.drop(4).take(4)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row1.forEach { mgr ->
                        Box(modifier = Modifier.weight(1f)) {
                            BoardSeatChip(
                                manager = mgr,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (mgr.isHired) onSelectManager(mgr) else onQuickHire(mgr)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row2.forEach { mgr ->
                        Box(modifier = Modifier.weight(1f)) {
                            BoardSeatChip(
                                manager = mgr,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (mgr.isHired) onSelectManager(mgr) else onQuickHire(mgr)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tekil İcra Koltuğu Butonu
 */
@Composable
private fun BoardSeatChip(
    manager: CompanyManager,
    onClick: () -> Unit
) {
    val isEng = isEnglishLanguage()
    val isHired = manager.isHired
    val isActive = manager.isActive

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

    val (tierColor, tierCode) = when (manager.id) {
        "mgr_treasury" -> ThemeGold to "#1"
        "mgr_contracts" -> ThemeNeonCyan to "#2"
        "mgr_borsa" -> Color(0xFFFFB74D) to "#3"
        "mgr_logistics" -> Color(0xFF81C784) to "#4"
        "mgr_hr" -> Color(0xFFCE93D8) to "#5"
        else -> Color(0xFF64B5F6) to "#5"
    }

    val shortTitle = when (manager.specialty) {
        "treasury" -> "Hazine"
        "contracts" -> "Tedarik"
        "borsa" -> "Borsa"
        "logistics" -> "Lojistik"
        "hr" -> "İK"
        "rd" -> "Ar-Ge"
        "production" -> "Üretim"
        "maintenance" -> "Bakım"
        else -> "Yönetici"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = when {
            !isHired -> Color(0xFF09111E)
            !isActive -> Color(0xFF1A1324)
            else -> Color(0xFF0E1A2E)
        },
        border = BorderStroke(
            1.dp,
            when {
                !isHired -> Color(0xFF1E2D4A)
                !isActive -> Color(0xFFEF4444).copy(alpha = 0.6f)
                else -> tierColor.copy(alpha = 0.7f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Seat Avatar with Status Ring
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        !isHired -> Color(0xFF1E293B)
                        !isActive -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        else -> tierColor.copy(alpha = 0.2f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            !isHired -> Color(0xFF334155)
                            !isActive -> Color(0xFFEF4444)
                            else -> tierColor
                        }
                    ),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isHired) specialtyIcon else Icons.Rounded.PersonOutline,
                            contentDescription = null,
                            tint = if (isHired) tierColor else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Tier badge pill
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    CurrencyText(tierCode, fontSize = 6.sp, fontWeight = FontWeight.Black, color = tierColor, modifier = Modifier.padding(horizontal = 2.dp))
                }
            }

            // Department Label
            CurrencyText(
                text = shortTitle,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHired) Color.White else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Status label
            if (isHired) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (isActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                ) {
                    CurrencyText(
                        text = if (isActive) "Lvl ${manager.level}" else tr("İzin", "Leave"),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF34D399) else Color(0xFFFCA5A5),
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = ThemeGold.copy(alpha = 0.15f)
                ) {
                    CurrencyText(
                        text = "+AL",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = ThemeGold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. STRATEJİK AKILLI DİREKTİFLER & EMİR MASASI (AUTOPILOT DIRECTIVES PANEL)
 * Müdürlerin ürettiği canlı fırsatlar ve tek dokunuşla otomatik veya anlık uygulama.
 */
@Composable
fun HrActiveDirectivesPanel(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isEng = isEnglishLanguage()
    val directives by AutopilotEngine.directivesState.collectAsState()

    var executingDirectiveId by remember { mutableStateOf<String?>(null) }
    var executionFeedback by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1324),
        border = BorderStroke(1.2.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Psychology, contentDescription = null, tint = ThemeNeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column {
                        CurrencyText(
                            text = tr("C-SUITE STRATEJİK DİREKTİFLERİ", "C-SUITE STRATEGIC DIRECTIVES"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        CurrencyText(
                            text = tr("Otonom Holding Kararları & Akıllı İşlemler", "Autonomous Enterprise Decisions & Smart Actions"),
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, Color(0xFF34D399))
                ) {
                    CurrencyText(
                        text = "● OTO-PİLOT AKTİF",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF34D399),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            executionFeedback?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF064E3B),
                    border = BorderStroke(1.dp, Color(0xFF34D399)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CurrencyText(
                        text = "✅ $msg",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (directives.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF060B14),
                    border = BorderStroke(1.dp, Color(0xFF152238)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        CurrencyText(
                            text = tr("Şu anda beklemede olan kritik bir holding direktifi bulunmuyor. Tüm operasyonlar rayında!", "No pending critical holding directives at the moment. All operations running smoothly!"),
                            fontSize = 9.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    directives.forEach { dir ->
                        DirectiveItemCard(
                            directive = dir,
                            isExecuting = executingDirectiveId == dir.id,
                            onToggleAuto = { enabled ->
                                AutopilotEngine.setAutopilotEnabled(dir.id, enabled)
                            },
                            onExecuteNow = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                executingDirectiveId = dir.id
                                coroutineScope.launch {
                                    val success = AutopilotEngine.executeDirective(dir.id, viewModel)
                                    executingDirectiveId = null
                                    executionFeedback = if (success) {
                                        if (isEng) "${dir.title} executed successfully!" else "${dir.title} başarıyla icra edildi!"
                                    } else {
                                        if (isEng) "Cooldown active or insufficient funds." else "İşlem bekleme süresinde veya bütçe yetersiz."
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tekil Direktif Kartı
 */
@Composable
private fun DirectiveItemCard(
    directive: SmartDirective,
    isExecuting: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onExecuteNow: () -> Unit
) {
    val isEng = isEnglishLanguage()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F1B2E),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(directive.managerAvatarEmoji, fontSize = 16.sp)
                    Column {
                        CurrencyText(directive.title, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                        CurrencyText("${directive.managerName} (${directive.managerRole})", fontSize = 8.sp, color = ThemeGold)
                    }
                }

                // Autopilot Switch
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurrencyText(
                        text = if (directive.isAutoPilotEnabled) tr("Oto: Açık", "Auto: On") else tr("Oto: Kapalı", "Auto: Off"),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (directive.isAutoPilotEnabled) Color(0xFF34D399) else Color.Gray
                    )
                    Switch(
                        checked = directive.isAutoPilotEnabled,
                        onCheckedChange = onToggleAuto,
                        modifier = Modifier.height(20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF34D399),
                            checkedTrackColor = Color(0xFF064E3B),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            CurrencyText(directive.description, fontSize = 9.sp, color = Color(0xFFCBD5E1))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (directive.estimatedFinancialImpact != 0L) {
                    val sign = if (directive.estimatedFinancialImpact >= 0) "+" else ""
                    val color = if (directive.estimatedFinancialImpact >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
                    CurrencyText(
                        text = tr("Tahmini Bütçe: ", "Est. Budget: ") + "$sign${formatCredit(directive.estimatedFinancialImpact)}",
                        fontSize = 8.5.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    CurrencyText(tr("Operasyonel Görev", "Operational Task"), fontSize = 8.5.sp, color = ThemeNeonCyan)
                }

                AppButton(
                    onClick = onExecuteNow,
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    CurrencyText(if (isExecuting) tr("İcra Ediliyor...", "Executing...") else tr("Şimdi Uygula", "Execute Now"), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * 3. HOLDİNG FİNANSAL GÜVENCE & BORDRO SAĞLIĞI KARTI (FINANCIAL PULSE)
 */
@Composable
fun HrHoldingFinancialPulseCard(
    playerMoney: Long,
    totalDailySalaries: Long,
    treasuryReserve: Long,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val dailyBufferDays = if (totalDailySalaries > 0) (playerMoney / totalDailySalaries).toInt() else 999
    val bufferStatusColor = when {
        dailyBufferDays >= 30 -> Color(0xFF10B981)
        dailyBufferDays >= 10 -> ThemeGold
        else -> Color(0xFFEF4444)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF091222),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(16.dp))
                    CurrencyText(tr("HOLDİNG BORDRO GÜVENCESİ", "PAYROLL RESERVE SAFETY"), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = bufferStatusColor.copy(alpha = 0.2f),
                    border = BorderStroke(0.8.dp, bufferStatusColor)
                ) {
                    CurrencyText(
                        text = "$dailyBufferDays " + tr("Günlük Maaş Kasada Güvende", "Days Reserve Secured"),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = bufferStatusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Financial Gauge Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF060B14))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CurrencyText(tr("Günlük Bordro", "Daily Payroll"), fontSize = 8.sp, color = Color.Gray)
                    CurrencyText(formatCurrency(totalDailySalaries, isEng), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                }
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF1E2D4A)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CurrencyText(tr("Hazine Rezervi", "Treasury Guard"), fontSize = 8.sp, color = Color.Gray)
                    CurrencyText(if (treasuryReserve > 0) formatCurrency(treasuryReserve, isEng) else tr("Pasif", "Inactive"), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ThemeGold)
                }
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF1E2D4A)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CurrencyText(tr("Risk Durumu", "Risk Level"), fontSize = 8.sp, color = Color.Gray)
                    CurrencyText(if (dailyBufferDays >= 10) tr("GÜVENLİ", "SAFE") else tr("RİSKLİ", "ALERT"), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = bufferStatusColor)
                }
            }
        }
    }
}
