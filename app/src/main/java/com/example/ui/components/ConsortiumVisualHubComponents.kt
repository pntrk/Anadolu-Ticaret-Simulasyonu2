package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Konsorsiyum Adım Adım Rehberli Menü Seçenekleri
 */
enum class ConsortiumGuidedStep(
    val stepIndex: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val titleTr: String,
    val titleEn: String,
    val badgeTr: String,
    val badgeEn: String,
    val accentColor: Color
) {
    OVERVIEW(
        0,
        Icons.Rounded.RocketLaunch,
        "Genel Durum",
        "Overview",
        "1. Hedef",
        "1. Target",
        Color(0xFFF59E0B) // Amber Gold
    ),
    PRODUCTION_SLOTS(
        1,
        Icons.Rounded.PrecisionManufacturing,
        "Üretim & Kotalar",
        "Production",
        "2. Tedarik",
        "2. Supply",
        Color(0xFF06B6D4) // Cyan
    ),
    WAREHOUSE_DEPOT(
        2,
        Icons.Rounded.Warehouse,
        "Konsorsiyum Deposu",
        "Depot & Stock",
        "3. Ambar",
        "3. Depot",
        Color(0xFF10B981) // Emerald Green
    ),
    MEMBERS_EQUITY(
        3,
        Icons.Rounded.Groups,
        "Konsorsiyum Üyeleri",
        "Members & Equity",
        "4. Ortaklar",
        "4. Partners",
        Color(0xFFA855F7) // Purple
    ),
    TACTICAL_CHAT(
        4,
        Icons.Rounded.Forum,
        "Canlı Telsiz",
        "Live Radio",
        "5. Telsiz",
        "5. Radio",
        Color(0xFFF97316) // Orange
    )
}

/**
 * 1. ADIM ADIM YÖNLENDİREN ÜST MENÜ (STEPPER / SEGMENTED TABS)
 * Canlı animasyonlu neon göstergesi ve durum sayaçları içerir.
 */
@Composable
fun ConsortiumGuidedStepBar(
    currentStep: ConsortiumGuidedStep,
    onStepSelected: (ConsortiumGuidedStep) -> Unit,
    hasUnreadChat: Boolean = false,
    openSlotsCount: Int = 0,
    warehouseStock: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    // Breathing glow animation for active step
    val infiniteTransition = rememberInfiniteTransition(label = "step_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
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
            ConsortiumGuidedStep.values().forEach { step ->
                val isSelected = currentStep == step
                val badgeCountText = when (step) {
                    ConsortiumGuidedStep.PRODUCTION_SLOTS -> if (openSlotsCount > 0) "$openSlotsCount Açık" else "✓ Aktif"
                    ConsortiumGuidedStep.WAREHOUSE_DEPOT -> if (warehouseStock > 0) "$warehouseStock Adet" else "0 Adet"
                    ConsortiumGuidedStep.TACTICAL_CHAT -> if (hasUnreadChat) "🔴 Yeni" else "● Canlı"
                    ConsortiumGuidedStep.OVERVIEW -> "Vitrin"
                    ConsortiumGuidedStep.MEMBERS_EQUITY -> "Kâr Payı"
                }

                val targetBorderColor = when {
                    isSelected -> step.accentColor.copy(alpha = pulseAlpha)
                    step == ConsortiumGuidedStep.TACTICAL_CHAT && hasUnreadChat -> Color(0xFFEF4444)
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
                        // Icon with halo when selected
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
                                tint = when {
                                    isSelected -> step.accentColor
                                    step == ConsortiumGuidedStep.TACTICAL_CHAT && hasUnreadChat -> Color(0xFFEF4444)
                                    else -> Color(0xFF94A3B8)
                                },
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

                        // Badge counter
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = when {
                                step == ConsortiumGuidedStep.TACTICAL_CHAT && hasUnreadChat -> Color(0xFFEF4444).copy(alpha = 0.25f)
                                isSelected -> step.accentColor.copy(alpha = 0.22f)
                                else -> Color(0xFF1E293B)
                            },
                            border = BorderStroke(
                                0.8.dp,
                                when {
                                    step == ConsortiumGuidedStep.TACTICAL_CHAT && hasUnreadChat -> Color(0xFFEF4444)
                                    isSelected -> step.accentColor.copy(alpha = 0.6f)
                                    else -> Color(0xFF334155)
                                }
                            )
                        ) {
                            CurrencyText(
                                text = badgeCountText,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    step == ConsortiumGuidedStep.TACTICAL_CHAT && hasUnreadChat -> Color(0xFFFCA5A5)
                                    isSelected -> step.accentColor
                                    else -> Color.LightGray
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. ÜRETİLEN ÜRÜN VİTRİNİ VE GÖREV REHBERİ KARTI (OVERVIEW HERO)
 * Yüksek çözünürlüklü ürün görseli, holografik aura, aşama yol haritası ve temel metrikleri sunar.
 */
@Composable
fun ConsortiumProductHeroCard(
    project: MegaProject,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val productDrawableRes = remember(project.targetProductId) {
        ProductDrawables.getProductDrawableResId(project.targetProductId)
    }

    val tierBadgeText = when (project.qualityTier) {
        ConsortiumQualityTier.GRADE_A -> tr("🏆 A Kalite (Ultra Lüks)", "🏆 Grade A (Ultra Luxury)")
        ConsortiumQualityTier.GRADE_B -> tr("⭐ B Kalite (Gelişmiş Sanayi)", "⭐ Grade B (Advanced)")
        ConsortiumQualityTier.GRADE_C -> tr("📦 C Kalite (Standart Üretim)", "📦 Grade C (Standard)")
    }
    val tierBadgeColor = when (project.qualityTier) {
        ConsortiumQualityTier.GRADE_A -> Color(0xFFF59E0B)
        ConsortiumQualityTier.GRADE_B -> ThemeNeonCyan
        ConsortiumQualityTier.GRADE_C -> Color(0xFF94A3B8)
    }

    // Rotating holographic border animation
    val infiniteTransition = rememberInfiniteTransition(label = "hero_aura")
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1324),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    ThemeGold.copy(alpha = 0.7f),
                    ThemeNeonCyan.copy(alpha = 0.7f),
                    Color(0xFFA855F7).copy(alpha = 0.5f)
                )
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Product image and core details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-Tech Product Showcase Frame with Glowing Aura
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    // Pulsing background glow
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tierBadgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, tierBadgeColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    if (productDrawableRes != 0) {
                        Image(
                            painter = painterResource(id = productDrawableRes),
                            contentDescription = project.targetProductName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PrecisionManufacturing,
                            contentDescription = null,
                            tint = ThemeGold,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product details & stats
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tierBadgeColor.copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, tierBadgeColor)
                        ) {
                            CurrencyText(
                                text = tierBadgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = tierBadgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, Color(0xFF38BDF8))
                        ) {
                            CurrencyText(
                                text = "📍 ${project.cityId.replaceFirstChar { it.uppercase() }}",
                                fontSize = 8.sp,
                                color = Color(0xFF7DD3FC),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    CurrencyText(
                        text = project.targetProductName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    CurrencyText(
                        text = "🏷️ Marka: ${project.brandName}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CurrencyText(
                        text = "👑 ${project.consortiumName}",
                        fontSize = 9.5.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, Color(0xFF34D399))
                        ) {
                            CurrencyText(
                                text = "🏭 " + tr("Toplam: ", "Total: ") + "${project.totalItemsProduced} " + tr("Adet", "Units"),
                                fontSize = 8.5.sp,
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, Color(0xFF38BDF8))
                        ) {
                            CurrencyText(
                                text = "📦 " + tr("Depo: ", "Depot: ") + "${project.warehouseStock} " + tr("Adet", "Units"),
                                fontSize = 8.5.sp,
                                color = Color(0xFFBAE6FD),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }

            // 4-Phase Milestone Progression Line
            ConsortiumPhaseRoadmap(project = project)

            // Key Economics Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF050B16))
                    .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CurrencyText(tr("Birim Parti Fiyatı", "Batch Sell Price"), fontSize = 8.sp, color = Color.Gray)
                    CurrencyText(formatCredit(project.unitBatchPrice), fontSize = 11.sp, fontWeight = FontWeight.Black, color = ThemeGold)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CurrencyText(tr("Toplam Proje Değeri", "Project Valuation"), fontSize = 8.sp, color = Color.Gray)
                    CurrencyText(formatCredit(project.totalProjectValue), fontSize = 11.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CurrencyText(tr("Genel İlerleme", "Overall Progress"), fontSize = 8.sp, color = Color.Gray)
                    CurrencyText("%${(project.overallProgressFraction * 100).toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
                }
            }
        }
    }
}

/**
 * 4 FAZLI MEGA PROJE YOL HARİTASI (MILESTONE ROADMAP)
 */
@Composable
fun ConsortiumPhaseRoadmap(
    project: MegaProject,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val isFinished = project.isAllStagesFinished || project.currentStage == MegaProjectStage.COMPLETED
    val isStage1Done = project.currentStage.ordinal >= 1 || isFinished
    val isStage2Done = project.currentStage.ordinal >= 2 || isFinished
    val isStage3Done = (project.isTestProductProduced && project.isMassProductionApproved) || isFinished
    val isStage4Done = isFinished

    val milestones = listOf(
        Triple("1. Kurulum", "1. Setup", isStage1Done),
        Triple("2. Tedarik", "2. Supply", isStage2Done),
        Triple("3. Test & Onay", "3. Test & Approval", isStage3Done),
        Triple("4. Seri Üretim", "4. Mass Prod", isStage4Done)
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF081220),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CurrencyText(
                    text = tr("🗺️ Mega Sanayi Yol Haritası:", "🗺️ Mega Industry Roadmap:"),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
                CurrencyText(
                    text = if (isEng) project.currentStage.titleEn else project.currentStage.titleTr,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = ThemeNeonCyan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                milestones.forEachIndexed { index, (trTitle, enTitle, isDone) ->
                    val isCurrent = when (index) {
                        0 -> project.currentStage.ordinal == 0
                        1 -> project.currentStage.ordinal == 1
                        2 -> project.currentStage.ordinal == 2
                        3 -> project.currentStage.ordinal >= 3
                        else -> false
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Node circle
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isDone -> Color(0xFF10B981)
                                isCurrent -> ThemeNeonCyan
                                else -> Color(0xFF1E293B)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isDone -> Color(0xFF34D399)
                                    isCurrent -> Color.White
                                    else -> Color(0xFF334155)
                                }
                            ),
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDone) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                                } else {
                                    CurrencyText("${index + 1}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isCurrent) Color.Black else Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        CurrencyText(
                            text = if (isEng) enTitle else trTitle,
                            fontSize = 7.5.sp,
                            fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isDone -> Color(0xFFA7F3D0)
                                isCurrent -> ThemeNeonCyan
                                else -> Color.Gray
                            }
                        )
                    }

                    if (index < milestones.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.5.dp)
                                .padding(horizontal = 4.dp)
                                .background(if (isDone) Color(0xFF10B981) else Color(0xFF1E2D4A))
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. KULLANICI İÇİN ANLAŞILIR "SIRADAKİ ADIMINIZ" REHBERİ
 * Oyuncunun o anda ne yapması gerektiğini tek cümlede ve tek tıkla yönlendirir.
 */
@Composable
fun ConsortiumNextStepGuidanceCard(
    project: MegaProject,
    mySlot: ConsortiumSupplierSlot?,
    isLeader: Boolean,
    openSlotsCount: Int,
    inventory: List<com.example.data.InventoryEntity>,
    onNavigateToProductionTab: () -> Unit,
    onNavigateToDepotTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isFinished = project.isAllStagesFinished || project.currentStage == MegaProjectStage.COMPLETED

    // Pulsing beacon animation
    val infiniteTransition = rememberInfiniteTransition(label = "guidance_pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

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
                        text = tr("🎯 Sıradaki Eyleminiz:", "🎯 Your Next Action:"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = ThemeGold
                    )

                    val actionDescription = when {
                        isFinished -> tr("Proje tamamlandı! Seri üretim döngüsünden rutin kâr payınızı toplayın.", "Project finished! Collect routine dividends from the mass production loop.")
                        project.isTestProductProduced && !project.isMassProductionApproved -> {
                            if (isLeader) tr("🧪 Test üretimi başarılı! Seri üretimi başlatmak için onay butonuna basın.", "🧪 Test production ready! Tap approve to start mass production loop.")
                            else tr("⏳ Test üretimi hazır. Kurucunun (${project.leaderPlayerName}) seri üretime onay vermesi bekleniyor.", "⏳ Waiting for founder (${project.leaderPlayerName}) to approve mass production.")
                        }
                        mySlot != null && !mySlot.isFullyDelivered -> {
                            val invItem = inventory.find { it.itemId == mySlot.productId }
                            val avail = invItem?.quantity ?: 0
                            val rem = (mySlot.quantityRequired - mySlot.quantityDelivered).coerceAtLeast(0)
                            if (avail > 0) tr("📦 ${mySlot.productName} kotanız için depoda $avail adet hazır! Tek tıkla teslim edin.", "📦 $avail items ready in storage for your ${mySlot.productName} quota! Deliver now.")
                            else tr("⚠️ ${mySlot.productName} kotanız için $rem adet eksik. Pazardan temin edin veya fabrikanızda üretin.", "⚠️ $rem items needed for your ${mySlot.productName} quota. Buy from market or manufacture.")
                        }
                        mySlot != null && mySlot.isFullyDelivered -> tr("✅ Kotanızı eksiksiz tamamladınız! Diğer ortakların teslimatlarını bekleyin.", "✅ You completed your quota! Waiting for other partners to complete delivery.")
                        openSlotsCount > 0 -> tr("🤝 Projede $openSlotsCount açık kota var. Katılarak %25 kâr payı ortağı olun.", "🤝 $openSlotsCount open quotas available. Join to become a 25% profit partner.")
                        project.warehouseStock > 0 -> tr("💰 Depoda ${project.warehouseStock} adet hazır ürün var! Depoyu satarak kârınızı nakde çevirin.", "💰 ${project.warehouseStock} units waiting in depot! Sell warehouse stock to collect profits.")
                        else -> tr("🏭 Tüm kotalar aktif. Parça montajı ve bant üretimleri sürüyor.", "🏭 All quotas active. Material assembly and production lines ongoing.")
                    }

                    CurrencyText(
                        text = actionDescription,
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
                    if (project.warehouseStock > 0 && (mySlot == null || mySlot.isFullyDelivered)) {
                        onNavigateToDepotTab()
                    } else {
                        onNavigateToProductionTab()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                CurrencyText(tr("GİT ➔", "GO ➔"), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/**
 * 4. KONSORSİYUM DEPOSU GÖRSEL YÖNETİM PANELİ (DEPOT & WAREHOUSE)
 * 3D/İzometrik Kargo Ambarı Çizimi, Palet Kutuları, Satış Kanalları, Hasılat Simülatörü ve Kapasite Geliştirme.
 */
@Composable
fun ConsortiumDepotVisualCard(
    project: MegaProject,
    isLeader: Boolean,
    isMember: Boolean,
    onSellWarehouseStock: (quantity: Int) -> Unit = {},
    onListStockOnMarket: ((quantity: Int, pricePerUnit: Long) -> Unit)? = null,
    onUpgradeWarehouse: () -> Unit = {},
    onChangeSalesChannel: (ConsortiumSalesChannel) -> Unit = {},
    onToggleAutoSell: (Boolean) -> Unit = {},
    activeMarketListings: List<MarketListing> = emptyList(),
    onCancelMarketListing: ((String) -> Unit)? = null,
    currentGems: Int = 0,
    userSharePercentage: Float = 0f,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()
    val productDrawableRes = remember(project.targetProductId) {
        ProductDrawables.getProductDrawableResId(project.targetProductId)
    }

    val warehouseStock = project.warehouseStock
    val capacity = project.warehouseCapacity.coerceAtLeast(1)
    val fillFraction = (warehouseStock.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "depot_fill"
    )
    val totalWarehouseValuation = warehouseStock.toLong() * project.unitBatchPrice

    // Kar payi dagilimi
    val founderCut = (totalWarehouseValuation * 0.10).toLong()
    val supplierPoolCut = (totalWarehouseValuation * 0.90).toLong()
    val userEstimatedCut = if (userSharePercentage > 0f) {
        (supplierPoolCut * (userSharePercentage / 100.0)).toLong()
    } else if (isLeader) {
        founderCut
    } else 0L

    var selectedSubTab by remember { mutableStateOf(ConsortiumDepotSubTab.STOCK) }
    var showListingDialog by remember { mutableStateOf(false) }
    var showBorsaSaleDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1424),
        border = BorderStroke(1.2.dp, Color(0xFF1E3A5F)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. SLEEK WAREHOUSE CAPACITY & LEVEL HEADER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101726),
                border = BorderStroke(1.dp, if (fillFraction > 0.85f) ThemeNegative.copy(alpha = 0.6f) else ThemeBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = ThemeNeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warehouse,
                                        contentDescription = null,
                                        tint = ThemeNeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CurrencyText(
                                        text = tr("KONSORSİYUM MERKEZ DEPOSU", "CONSORTIUM CENTRAL DEPOT"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = RobotoMonoFontFamily,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = ThemeGold.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.6f))
                                    ) {
                                        CurrencyText(
                                            text = "SEV ${capacity / 1000}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = ThemeGold,
                                            fontFamily = RobotoMonoFontFamily,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                CurrencyText(
                                    text = "1 " + tr("Çeşit Ürün", "Type of Product") + " • ${project.targetProductName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeNeonCyan,
                                    fontSize = 10.sp,
                                    fontFamily = RobotoMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Upgrade Button
                        if (isLeader) {
                            AppButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpgradeWarehouse()
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentGems >= 100) ThemeNeonCyan else Color(0xFF1E283A),
                                    contentColor = if (currentGems >= 100) Color(0xFF002026) else Color.Gray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Upgrade,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                CurrencyText(
                                    text = "+1000 Depo (100 💎)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Progress Bar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyText(
                            text = "$warehouseStock / $capacity " + tr("Adet", "Units"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (fillFraction > 0.85f) ThemeNegative else Color.LightGray,
                            maxLines = 1
                        )

                        val progressColor = if (fillFraction > 0.85f) ThemeNegative else if (fillFraction > 0.6f) ThemeGold else ThemeNeonCyan
                        LinearProgressIndicator(
                            progress = { animatedFill },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = Color(0xFF1E283A)
                        )

                        CurrencyText(
                            text = "%${(fillFraction * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = progressColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. INTERACTIVE SUB-TABS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConsortiumDepotSubTab.values().forEach { tab ->
                    val isSelected = selectedSubTab == tab
                    val tabBadge = if (tab == ConsortiumDepotSubTab.ACTIVE_LISTINGS && activeMarketListings.isNotEmpty()) {
                        " (${activeMarketListings.size})"
                    } else ""

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedSubTab = tab
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) ThemeNeonCyan else Color(0xFF101726),
                        border = BorderStroke(1.dp, if (isSelected) ThemeNeonCyan else ThemeBorder),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CurrencyText(
                                text = (if (isEng) tab.titleEn else tab.titleTr) + tabBadge,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color(0xFF002026) else Color.LightGray,
                                fontFamily = RobotoMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. TAB CONTENT
            AnimatedContent(
                targetState = selectedSubTab,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "depot_tab_content"
            ) { tab ->
                when (tab) {
                    ConsortiumDepotSubTab.STOCK -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // High-Tech Isometric Warehouse Bay Canvas
                            ConsortiumDepotBayCanvas(
                                fillFraction = animatedFill,
                                warehouseStock = warehouseStock,
                                capacity = capacity,
                                project = project,
                                modifier = Modifier.height(105.dp)
                            )

                            // President Authority Strip / Member Monitoring Strip
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isLeader) Color(0xFF131F33) else Color(0xFF0F172A),
                                border = BorderStroke(1.dp, if (isLeader) ThemeGold.copy(alpha = 0.5f) else ThemeBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLeader) Icons.Rounded.VerifiedUser else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (isLeader) ThemeGold else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    CurrencyText(
                                        text = if (isLeader)
                                            tr("👑 Konsorsiyum Başkanı Satış Yetkisi: Depodaki ürünleri Pazarda veya Borsada dilediğiniz adette satışa sunabilirsiniz.", "👑 President Selling Authority: You can list or sell finished units on Market or Borsa in any desired quantity.")
                                        else
                                            tr("🔒 İzleme Modu: Depodaki ürünleri satma yetkisi Başkan'a (${project.leaderPlayerName}) aittir. Hasılat ortaklar havuzuna aktarılır.", "🔒 View Mode: Only President (${project.leaderPlayerName}) can list or sell finished units. Proceeds go to partner pool."),
                                        fontSize = 9.sp,
                                        color = if (isLeader) Color.White else Color.LightGray,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // MERKEZ DEPO EMTİA KARTI (CommodityStockCard)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF121824),
                                border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Header Row: Quantity Pill + Tier Pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(2.dp),
                                            color = ThemeNeonCyan.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, ThemeNeonCyan.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(ThemeNeonCyan)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                CurrencyText(
                                                    text = "$warehouseStock " + tr("Adet", "Units"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = RobotoMonoFontFamily,
                                                    color = ThemeNeonCyan,
                                                    fontSize = 10.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF1E283A)
                                        ) {
                                            CurrencyText(
                                                text = "T4 Mega Proje",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                fontFamily = RobotoMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.LightGray,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Product Info Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF162238),
                                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (productDrawableRes != 0) {
                                                    Image(
                                                        painter = painterResource(id = productDrawableRes),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(34.dp)
                                                    )
                                                } else {
                                                    Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            CurrencyText(
                                                text = project.targetProductName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            CurrencyText(
                                                text = "${project.brandName} • ${project.consortiumName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = Color(project.qualityTier.badgeColor).copy(alpha = 0.2f),
                                                    border = BorderStroke(0.6.dp, Color(project.qualityTier.badgeColor))
                                                ) {
                                                    val starsCount = when (project.qualityTier) {
                                                        ConsortiumQualityTier.GRADE_A -> "★★★★★"
                                                        ConsortiumQualityTier.GRADE_B -> "★★★"
                                                        ConsortiumQualityTier.GRADE_C -> "★"
                                                    }
                                                    CurrencyText(
                                                        text = "${project.qualityTier.gradeCode} ($starsCount)",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(project.qualityTier.badgeColor),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }

                                                CurrencyText(
                                                    text = tr("Birim Spot: ", "Unit Spot: ") + formatCredit(project.unitBatchPrice),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = ThemeGold,
                                                    fontFamily = RobotoMonoFontFamily,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Total Valuation Box
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(2.dp),
                                        color = Color(0xFF182030),
                                        border = BorderStroke(0.5.dp, ThemeBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText(
                                                text = tr("Toplam Stok Değeri:", "Total Stock Value:"),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.5.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                            CurrencyText(
                                                text = formatCredit(totalWarehouseValuation),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = RobotoMonoFontFamily,
                                                color = ThemeGold,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Quick Spot Liquidation Bar for President
                                    if (isLeader && warehouseStock > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CurrencyText(
                                                text = tr("Hızlı Spot:", "Fast Spot:"),
                                                fontSize = 8.5.sp,
                                                color = Color.Gray,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                            ConsortiumPresetChip(
                                                label = "%25",
                                                onClick = {
                                                    val q = (warehouseStock * 0.25f).toInt().coerceAtLeast(1)
                                                    onSellWarehouseStock(q)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ConsortiumPresetChip(
                                                label = "%50",
                                                onClick = {
                                                    val q = (warehouseStock * 0.50f).toInt().coerceAtLeast(1)
                                                    onSellWarehouseStock(q)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ConsortiumPresetChip(
                                                label = "%100",
                                                onClick = {
                                                    onSellWarehouseStock(warehouseStock)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action Buttons: Pazarda Sat & Borsada Sat
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // BUTTON 1: PAZARDA SAT (İLAN VER)
                                        AppButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showListingDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = warehouseStock > 0 && isLeader,
                                            shape = RoundedCornerShape(3.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ThemeNeonCyan,
                                                contentColor = Color(0xFF002026),
                                                disabledContainerColor = Color(0xFF1E283A),
                                                disabledContentColor = Color.Gray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Sell,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            CurrencyText(
                                                text = tr("Pazarda Sat", "Sell on Market"),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // BUTTON 2: BORSADA SAT
                                        AppButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showBorsaSaleDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = warehouseStock > 0 && isLeader,
                                            shape = RoundedCornerShape(3.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ThemeGold,
                                                contentColor = Color.Black,
                                                disabledContainerColor = Color(0xFF1E283A),
                                                disabledContentColor = Color.Gray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FlashOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            CurrencyText(
                                                text = tr("Borsada Sat", "Sell on Borsa"),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ConsortiumDepotSubTab.ACTIVE_LISTINGS -> {
                        ConsortiumActiveListingsView(
                            project = project,
                            isLeader = isLeader,
                            listings = activeMarketListings,
                            onCancelListing = { listingId ->
                                onCancelMarketListing?.invoke(listingId)
                            }
                        )
                    }

                    ConsortiumDepotSubTab.SETTINGS -> {
                        // KÂR PAYI DAĞILIMI VE AYARLAR
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF091426),
                            border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CurrencyText(
                                    text = tr("📊 SATIŞ GELİRİ DAĞILIM PROTOKOLÜ", "📊 REVENUE SPLIT PROTOCOL"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF94A3B8)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(shape = CircleShape, color = ThemeGold, modifier = Modifier.size(6.dp)) {}
                                        CurrencyText(tr("👑 Kurucu Payı (%10):", "👑 Founder Cut (%10):"), fontSize = 9.sp, color = Color.LightGray)
                                    }
                                    CurrencyText(formatCredit(founderCut), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = ThemeGold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(shape = CircleShape, color = Color(0xFF34D399), modifier = Modifier.size(6.dp)) {}
                                        CurrencyText(tr("📦 Tedarikçi Ortaklar Havuzu (%90):", "📦 Partner Supplier Pool (%90):"), fontSize = 9.sp, color = Color.LightGray)
                                    }
                                    CurrencyText(formatCredit(supplierPoolCut), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }

                                if (userEstimatedCut > 0L) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(shape = CircleShape, color = ThemeNeonCyan, modifier = Modifier.size(6.dp)) {}
                                            CurrencyText(tr("👤 Sizin Tahmini Payınız:", "👤 Your Estimated Share:"), fontSize = 9.sp, color = Color.LightGray)
                                        }
                                        CurrencyText(formatCredit(userEstimatedCut), fontSize = 10.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 0.8.dp)

                                // Sales Channel Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        CurrencyText(
                                            text = tr("Varsayılan Kanal: ", "Default Channel: ") + if (isEng) project.salesChannel.titleEn else project.salesChannel.titleTr,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        val channelBonus = when (project.salesChannel) {
                                            ConsortiumSalesChannel.PAZAR -> tr("+%25 Marka Primi & Perakende", "+25% Brand Premium & Retail")
                                            ConsortiumSalesChannel.DEVLET_IHALE -> tr("+%40 Teşvik Primi & +100 Prestij", "+40% Subsidy & +100 Prestige")
                                            ConsortiumSalesChannel.BORSA -> tr("Spot Anında Likidite & +50 Prestij", "Spot Instant Cash & +50 Prestige")
                                        }
                                        CurrencyText(channelBonus, fontSize = 7.5.sp, color = ThemeGold)
                                    }

                                    if (isLeader) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            ConsortiumSalesChannel.values().forEach { channel ->
                                                val isSelected = project.salesChannel == channel
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) ThemeGold else Color(0xFF1E293B),
                                                    border = BorderStroke(0.8.dp, if (isSelected) ThemeGold else Color(0xFF334155)),
                                                    modifier = Modifier.clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onChangeSalesChannel(channel)
                                                    }
                                                ) {
                                                    CurrencyText(
                                                        text = when (channel) {
                                                            ConsortiumSalesChannel.PAZAR -> "Pazar"
                                                            ConsortiumSalesChannel.BORSA -> "Borsa"
                                                            ConsortiumSalesChannel.DEVLET_IHALE -> "İhale"
                                                        },
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isSelected) Color.Black else Color.LightGray,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Auto-Sell Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        CurrencyText(
                                            text = tr("Otomatik Satış (Bot Alıcılar):", "Auto-Sell (Bot Buyers):"),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        CurrencyText(
                                            text = if (project.isAutoSellActive)
                                                tr("Açık: Ürünler üretilir üretilmez bot alıcılara satılır.", "Active: Units sold to bot buyers upon production.")
                                            else
                                                tr("Kapalı: Ürünler depoda birikir, başkan istediği adette satar.", "Inactive: Products accumulate in depot; president sells at will."),
                                            fontSize = 7.5.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    if (isLeader) {
                                        Switch(
                                            checked = project.isAutoSellActive,
                                            onCheckedChange = { onToggleAutoSell(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = ThemeGold,
                                                checkedTrackColor = ThemeGold.copy(alpha = 0.5f),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 1: PAZARA İLAN VER DİALOGU
    // ==========================================
    if (showListingDialog) {
        ConsortiumListingCreationDialog(
            project = project,
            onDismiss = { showListingDialog = false },
            onSubmit = { quantity, pricePerUnit ->
                onListStockOnMarket?.invoke(quantity, pricePerUnit)
                showListingDialog = false
            }
        )
    }

    // ==========================================
    // DIALOG 2: BORSADA SAT DİALOGU
    // ==========================================
    if (showBorsaSaleDialog) {
        ConsortiumBorsaSaleDialog(
            project = project,
            onDismiss = { showBorsaSaleDialog = false },
            onSubmit = { quantity ->
                onSellWarehouseStock(quantity)
                showBorsaSaleDialog = false
            }
        )
    }
}

/**
 * Backward compatibility overloads
 */
@Composable
fun ConsortiumDepotVisualCard(
    project: MegaProject,
    isLeader: Boolean,
    isMember: Boolean,
    onSellWarehouseStock: (quantity: Int) -> Unit = {},
    onListStockOnMarket: ((quantity: Int, pricePerUnit: Long) -> Unit)? = null,
    onUpgradeWarehouse: () -> Unit = {},
    onChangeSalesChannel: (ConsortiumSalesChannel) -> Unit = {},
    onToggleAutoSell: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ConsortiumDepotVisualCard(
        project = project,
        isLeader = isLeader,
        isMember = isMember,
        onSellWarehouseStock = onSellWarehouseStock,
        onListStockOnMarket = onListStockOnMarket,
        onUpgradeWarehouse = onUpgradeWarehouse,
        onChangeSalesChannel = onChangeSalesChannel,
        onToggleAutoSell = onToggleAutoSell,
        activeMarketListings = emptyList(),
        onCancelMarketListing = null,
        currentGems = 0,
        userSharePercentage = 0f,
        modifier = modifier
    )
}

@Composable
fun ConsortiumDepotVisualCard(
    project: MegaProject,
    isLeader: Boolean,
    isMember: Boolean,
    onSellWarehouseStock: () -> Unit,
    onUpgradeWarehouse: () -> Unit,
    onChangeSalesChannel: (ConsortiumSalesChannel) -> Unit,
    onToggleAutoSell: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ConsortiumDepotVisualCard(
        project = project,
        isLeader = isLeader,
        isMember = isMember,
        onSellWarehouseStock = { _ -> onSellWarehouseStock() },
        onListStockOnMarket = null,
        onUpgradeWarehouse = onUpgradeWarehouse,
        onChangeSalesChannel = onChangeSalesChannel,
        onToggleAutoSell = onToggleAutoSell,
        activeMarketListings = emptyList(),
        onCancelMarketListing = null,
        currentGems = 0,
        userSharePercentage = 0f,
        modifier = modifier
    )
}

/**
 * KONSORSİYUM PAZARA İLAN VER DİALOGU
 * InventoryScreen.kt ListingCreationDialog ile birebir aynı yapı ve estetik
 */
@Composable
fun ConsortiumListingCreationDialog(
    project: MegaProject,
    onDismiss: () -> Unit,
    onSubmit: (quantity: Int, pricePerUnit: Long) -> Unit
) {
    var saleQuantityText by remember { mutableStateOf(project.warehouseStock.coerceAtLeast(1).toString()) }
    var salePriceText by remember { mutableStateOf(project.unitBatchPrice.toString()) }
    val isEnglish = isEnglishLanguage()
    val productDrawableRes = remember(project.targetProductId) {
        ProductDrawables.getProductDrawableResId(project.targetProductId)
    }

    val currentQty = saleQuantityText.toIntOrNull() ?: 0
    val currentPrice = salePriceText.toLongOrNull() ?: 0L
    val totalRevenue = currentQty * currentPrice
    val spotPrice = project.unitBatchPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ThemeNeonCyan.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (productDrawableRes != 0) {
                            Image(
                                painter = painterResource(id = productDrawableRes),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.PrecisionManufacturing, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            text = project.targetProductName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(project.qualityTier.badgeColor).copy(alpha = 0.2f),
                            border = BorderStroke(0.8.dp, Color(project.qualityTier.badgeColor).copy(alpha = 0.8f))
                        ) {
                            CurrencyText(
                                text = project.qualityTier.gradeCode,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(project.qualityTier.badgeColor),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    CurrencyText(
                        text = tr("B2B Pazarında İlana Çıkarın (Konsorsiyum)", "List on B2B Marketplace (Consortium)"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Stock Availability Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF182030),
                    border = BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("Mevcut Konsorsiyum Stoğu:", "Current Consortium Stock:"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        CurrencyText(
                            text = "${project.warehouseStock} " + tr("Adet", "Units"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan
                        )
                    }
                }

                // Quantity Input & Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = saleQuantityText,
                        onValueChange = { saleQuantityText = it },
                        label = { CurrencyText(tr("Satış Miktarı (Adet)", "Sales Quantity (Units)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeNeonCyan,
                            unfocusedBorderColor = ThemeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Percentage Preset Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ConsortiumPresetChip(
                            label = "%25",
                            onClick = { saleQuantityText = (project.warehouseStock * 0.25f).toInt().coerceAtLeast(1).toString() },
                            modifier = Modifier.weight(1f)
                        )
                        ConsortiumPresetChip(
                            label = "%50",
                            onClick = { saleQuantityText = (project.warehouseStock * 0.50f).toInt().coerceAtLeast(1).toString() },
                            modifier = Modifier.weight(1f)
                        )
                        ConsortiumPresetChip(
                            label = "%100",
                            onClick = { saleQuantityText = project.warehouseStock.toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Price Input & Tweaks
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = salePriceText,
                        onValueChange = { salePriceText = it },
                        label = { CurrencyText(tr("Birim Fiyat (₳ / Adet)", "Unit Price (₳ / Unit)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeGold,
                            unfocusedBorderColor = ThemeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ConsortiumPresetChip(
                            label = "-10%",
                            onClick = { salePriceText = (spotPrice * 0.90f).toLong().toString() },
                            modifier = Modifier.weight(1f)
                        )
                        ConsortiumPresetChip(
                            label = tr("Piyasa", "Market"),
                            onClick = { salePriceText = spotPrice.toString() },
                            modifier = Modifier.weight(1f)
                        )
                        ConsortiumPresetChip(
                            label = "+10%",
                            onClick = { salePriceText = (spotPrice * 1.10f).toLong().toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Smart Price Assistant & Expected Revenue Box
                val diffPercent = if (spotPrice > 0) ((currentPrice - spotPrice).toFloat() / spotPrice.toFloat() * 100).toInt() else 0
                val color = if (diffPercent > 0) ThemeNegative else if (diffPercent < 0) ThemePositive else Color.Gray
                val prefix = if (diffPercent > 0) "+" else ""

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(2.dp), color = Color(0xFF161F33)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        CurrencyText(tr("Piyasa Ortalaması: ", "Market Average: ") + formatCredit(spotPrice), color = Color.Gray, fontSize = 11.sp)
                        val relationshipText = if (diffPercent > 0) tr("üstünde", "above") else if (diffPercent < 0) tr("altında", "below") else tr("tam seviyesinde", "on par")
                        CurrencyText(tr("Fiyatınız piyasanın %$prefix$diffPercent ", "Your Price is %$prefix$diffPercent ") + relationshipText + tr(".", "."), color = color, fontSize = 10.5.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val stateTax = (totalRevenue * 0.05f).toLong()
                        val netRevenue = totalRevenue - stateTax

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("Brüt Satış Tutarı:", "Gross Revenue:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            CurrencyText(formatCredit(totalRevenue), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("Ticaret Vergisi (%5):", "Trade Tax (%5):"), style = MaterialTheme.typography.labelSmall, color = ThemeNegative)
                            CurrencyText("-${formatCredit(stateTax)}", style = MaterialTheme.typography.labelSmall, color = ThemeNegative)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(tr("NET KAZANÇ:", "NET EARNINGS:"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeGold)
                            CurrencyText(formatCredit(netRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ThemeGold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    if (currentQty <= 0) {
                        return@AppButton
                    }
                    if (currentQty > project.warehouseStock) {
                        return@AppButton
                    }
                    if (currentPrice <= 0) {
                        return@AppButton
                    }
                    onSubmit(currentQty, currentPrice)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeNeonCyan,
                    contentColor = Color(0xFF002026)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                CurrencyText(tr("İlanı Yayınla", "Publish Listing"), fontWeight = FontWeight.Bold, fontFamily = RobotoMonoFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText(tr("İptal", "Cancel"), color = Color.Gray)
            }
        }
    )
}

/**
 * KONSORSİYUM BORSADA SAT DİALOGU
 * Başkanın istediği adette ürünü spot Borsa fiyatından anında nakde çevirmesini sağlar
 */
@Composable
fun ConsortiumBorsaSaleDialog(
    project: MegaProject,
    onDismiss: () -> Unit,
    onSubmit: (quantity: Int) -> Unit
) {
    var saleQuantityText by remember { mutableStateOf(project.warehouseStock.coerceAtLeast(1).toString()) }
    val currentQty = saleQuantityText.toIntOrNull() ?: 0
    val unitPrice = project.unitBatchPrice
    val totalRevenue = currentQty.toLong() * unitPrice
    val founderCut = (totalRevenue * 0.10).toLong()
    val supplierPoolCut = (totalRevenue * 0.90).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101726),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ThemeGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = ThemeGold, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    CurrencyText(
                        text = tr("Borsada Spot Satış", "Spot Borsa Sale"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoMonoFontFamily
                    )
                    CurrencyText(
                        text = "${project.targetProductName} (${project.brandName})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Available Stock
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF182030),
                    border = BorderStroke(0.5.dp, ThemeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyText(
                            text = tr("Mevcut Konsorsiyum Stoğu:", "Available Consortium Stock:"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        CurrencyText(
                            text = "${project.warehouseStock} " + tr("Adet", "Units"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoMonoFontFamily,
                            color = ThemeNeonCyan
                        )
                    }
                }

                // Quantity Input & Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = saleQuantityText,
                        onValueChange = { saleQuantityText = it },
                        label = { CurrencyText(tr("Satılacak Miktar (Adet)", "Quantity to Sell (Units)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeGold,
                            unfocusedBorderColor = ThemeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ConsortiumPresetChip(
                            label = "%25",
                            onClick = { saleQuantityText = (project.warehouseStock * 0.25f).toInt().coerceAtLeast(1).toString() },
                            modifier = Modifier.weight(1f)
                        )
                        ConsortiumPresetChip(
                            label = "%50",
                            onClick = { saleQuantityText = (project.warehouseStock * 0.50f).toInt().coerceAtLeast(1).toString() },
                            modifier = Modifier.weight(1f)
                        )
                        ConsortiumPresetChip(
                            label = "%100",
                            onClick = { saleQuantityText = project.warehouseStock.toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Spot Price & Payout Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF161E2E),
                    border = BorderStroke(1.dp, ThemeGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("Birim Spot Fiyat:", "Unit Spot Price:"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            CurrencyText(formatCredit(unitPrice), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("👑 Kurucu Payı (%10):", "👑 Founder Cut (%10):"), style = MaterialTheme.typography.labelSmall, color = ThemeGold)
                            CurrencyText(formatCredit(founderCut), style = MaterialTheme.typography.labelSmall, color = ThemeGold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CurrencyText(tr("📦 Ortaklar Havuzu (%90):", "📦 Partner Pool (%90):"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399))
                            CurrencyText(formatCredit(supplierPoolCut), style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399))
                        }
                        HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 0.8.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            CurrencyText(tr("TOPLAM BRÜT HASILAT:", "TOTAL GROSS REVENUE:"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ThemeGold)
                            CurrencyText(formatCredit(totalRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ThemeGold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    if (currentQty in 1..project.warehouseStock) {
                        onSubmit(currentQty)
                    }
                },
                enabled = currentQty in 1..project.warehouseStock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                CurrencyText(
                    text = tr("Borsada Sat (Anında)", "Sell on Borsa (Instant)"),
                    fontWeight = FontWeight.Bold,
                    fontFamily = RobotoMonoFontFamily
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CurrencyText(tr("İptal", "Cancel"), color = Color.Gray)
            }
        }
    )
}

@Composable
private fun ConsortiumPresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(2.dp),
        color = Color(0xFF1A2130),
        border = BorderStroke(1.dp, ThemeBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            CurrencyText(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = RobotoMonoFontFamily,
                color = Color.LightGray
            )
        }
    }
}

/**
 * 5. KONSORSİYUM ÜYELERİ & DETAYLI KATKI / KÂR PAYI LİSTESİ
 * Katkı liderleri podyumu, görsel üye kartları, hak ediş yüzdeleri ve karne modalı.
 */
@Composable
fun ConsortiumMembersVisualSection(
    project: MegaProject,
    currentUserId: String,
    onMemberClick: (partnerId: String, partnerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEng = isEnglishLanguage()
    val groupedPartners = remember(project.slots) {
        project.slots.filter { it.assignedPartnerId != null }.groupBy { it.assignedPartnerId!! }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0B1424),
        border = BorderStroke(1.2.dp, Color(0xFF1E2D4A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFA855F7).copy(alpha = 0.2f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                        }
                    }
                    CurrencyText(
                        text = tr("KONSORSİYUM ÜYELERİ & KÂR PAYLARI", "CONSORTIUM MEMBERS & EQUITY"),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E293B)
                ) {
                    CurrencyText(
                        text = "${groupedPartners.size} " + tr("Aktif Ortak", "Active Partners"),
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            CurrencyText(
                text = tr("💡 Üye kartına dokunarak sunduğu hammadde katkısı, teslimat karnesi ve hak ediş detaylarını inceleyin.", "💡 Tap any partner card to view their delivered contributions, equity grade and dividend dossier."),
                fontSize = 8.5.sp,
                color = Color.Gray
            )

            // Top Contributors Leaderboard / Podium
            if (groupedPartners.isNotEmpty()) {
                val sortedPartners = remember(groupedPartners) {
                    groupedPartners.entries.sortedByDescending { (_, slots) -> slots.sumOf { it.quantityDelivered } }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF081120),
                    border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sortedPartners.take(3).forEachIndexed { index, (partnerId, slots) ->
                            val partnerName = slots.first().assignedPartnerName ?: tr("Ortak", "Partner")
                            val totalDelivered = slots.sumOf { it.quantityDelivered }
                            val medalEmoji = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                else -> "🥉"
                            }
                            val rankColor = when (index) {
                                0 -> ThemeGold
                                1 -> Color(0xFFE2E8F0)
                                else -> Color(0xFFCD7F32)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                CurrencyText(medalEmoji, fontSize = 14.sp)
                                CurrencyText(partnerName, fontSize = 9.sp, fontWeight = FontWeight.Black, color = rankColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                CurrencyText("$totalDelivered " + tr("Teslimat", "Delivered"), fontSize = 7.5.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            if (groupedPartners.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CurrencyText(
                        text = tr("Henüz projeye katılan ortak bulunmuyor.", "No partners joined the consortium yet."),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    groupedPartners.forEach { (partnerId, slotsForPartner) ->
                        val partnerName = slotsForPartner.first().assignedPartnerName ?: tr("Ortak", "Partner")
                        val isLeader = partnerId == project.leaderPlayerId || (project.leaderPlayerName.isNotBlank() && partnerName == project.leaderPlayerName)
                        val isMe = partnerId == currentUserId || partnerId == "local_player"

                        val totalDelivered = slotsForPartner.sumOf { it.quantityDelivered }
                        val totalRequired = slotsForPartner.sumOf { it.quantityRequired }.coerceAtLeast(1)
                        val deliveryFraction = (totalDelivered.toFloat() / totalRequired.toFloat()).coerceIn(0f, 1f)
                        val sharePercent = slotsForPartner.sumOf { it.sharePercentage.toDouble() }.toFloat()
                        val totalDividends = slotsForPartner.sumOf { it.totalDividendsEarned }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isMe -> Color(0xFF0D233D)
                                isLeader -> Color(0xFF1D1736)
                                else -> Color(0xFF0F1829)
                            },
                            border = BorderStroke(
                                1.2.dp,
                                when {
                                    isMe -> ThemeNeonCyan.copy(alpha = 0.8f)
                                    isLeader -> ThemeGold.copy(alpha = 0.7f)
                                    else -> Color(0xFF1E2D4A)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMemberClick(partnerId, partnerName) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Avatar Badge
                                        Surface(
                                            shape = CircleShape,
                                            color = when {
                                                isLeader -> ThemeGold.copy(alpha = 0.2f)
                                                isMe -> ThemeNeonCyan.copy(alpha = 0.2f)
                                                else -> Color(0xFF1E293B)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isLeader -> ThemeGold
                                                    isMe -> ThemeNeonCyan
                                                    else -> Color(0xFF334155)
                                                }
                                            ),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isLeader) Icons.Rounded.WorkspacePremium else Icons.Rounded.Person,
                                                    contentDescription = null,
                                                    tint = if (isLeader) ThemeGold else if (isMe) ThemeNeonCyan else Color.LightGray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                CurrencyText(
                                                    text = partnerName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                                if (isLeader) {
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = ThemeGold.copy(alpha = 0.25f)
                                                    ) {
                                                        CurrencyText("👑 Kurucu", fontSize = 7.5.sp, color = ThemeGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                                if (isMe) {
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = ThemeNeonCyan.copy(alpha = 0.25f)
                                                    ) {
                                                        CurrencyText("Sen", fontSize = 7.5.sp, color = ThemeNeonCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }

                                            CurrencyText(
                                                text = tr("Teslimat: ", "Delivered: ") + "$totalDelivered / $totalRequired " + tr("Adet", "Units"),
                                                fontSize = 9.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(5.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            border = BorderStroke(0.8.dp, Color(0xFF34D399))
                                        ) {
                                            CurrencyText(
                                                text = "%${sharePercent.toInt()} " + tr("Kâr Payı", "Share"),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF34D399),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        CurrencyText(
                                            text = formatCredit(totalDividends),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ThemeGold
                                        )
                                    }
                                }

                                // Delivery Fulfillment Progress Bar
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    LinearProgressIndicator(
                                        progress = { deliveryFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (deliveryFraction >= 1f) Color(0xFF10B981) else ThemeNeonCyan,
                                        trackColor = Color(0xFF1E293B)
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        CurrencyText(
                                            text = if (deliveryFraction >= 1f) tr("✅ KOTA TAMAMLANDI", "✅ QUOTA FULFILLED") else tr("Montaj Sürecinde", "In Assembly"),
                                            fontSize = 7.5.sp,
                                            color = if (deliveryFraction >= 1f) Color(0xFF34D399) else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                        CurrencyText(tr("Karneyi Aç ➔", "View Dossier ➔"), fontSize = 7.5.sp, color = ThemeNeonCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. TIKLANDIĞINDA AÇILAN DETAYLI "ÜYE KATKI VE HAK EDİŞ KARNESİ" DİYALOĞU
 * Yönetici ve ortaklar için VIP kurumsal dosya / karne arayüzü.
 */
@Composable
fun ConsortiumMemberDetailDialog(
    project: MegaProject,
    partnerId: String,
    partnerName: String,
    currentUserId: String,
    isLeader: Boolean,
    onDismiss: () -> Unit,
    onNudgePartner: (slotId: String) -> Unit,
    onKickPartner: (slotId: String) -> Unit,
    onOpenChat: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isEng = isEnglishLanguage()

    val partnerSlots = remember(project.slots, partnerId) {
        project.slots.filter { it.assignedPartnerId == partnerId || (partnerName.isNotBlank() && it.assignedPartnerName == partnerName) }
    }

    val totalDelivered = partnerSlots.sumOf { it.quantityDelivered }
    val totalRequired = partnerSlots.sumOf { it.quantityRequired }.coerceAtLeast(1)
    val fulfillmentRatio = (totalDelivered.toFloat() / totalRequired.toFloat()).coerceIn(0f, 1f)
    val sharePercent = partnerSlots.sumOf { it.sharePercentage.toDouble() }.toFloat()
    val totalDividends = partnerSlots.sumOf { it.totalDividendsEarned }

    val isPartnerLeader = partnerId == project.leaderPlayerId || (project.leaderPlayerName.isNotBlank() && partnerName == project.leaderPlayerName)
    val isMe = partnerId == currentUserId || partnerId == "local_player"

    val performanceGrade = when {
        fulfillmentRatio >= 1.0f -> "A+"
        fulfillmentRatio >= 0.75f -> "A"
        fulfillmentRatio >= 0.50f -> "B"
        fulfillmentRatio >= 0.25f -> "C"
        else -> "D"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF091220),
            border = BorderStroke(1.5.dp, ThemeNeonCyan.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (isPartnerLeader) ThemeGold.copy(alpha = 0.25f) else ThemeNeonCyan.copy(alpha = 0.25f),
                            border = BorderStroke(1.2.dp, if (isPartnerLeader) ThemeGold else ThemeNeonCyan),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPartnerLeader) Icons.Rounded.WorkspacePremium else Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = if (isPartnerLeader) ThemeGold else ThemeNeonCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CurrencyText(
                                    text = partnerName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                if (isPartnerLeader) {
                                    Surface(shape = RoundedCornerShape(3.dp), color = ThemeGold.copy(alpha = 0.25f)) {
                                        CurrencyText("👑 Kurucu", fontSize = 7.5.sp, color = ThemeGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            CurrencyText(
                                text = tr("Konsorsiyum Üye Katkı & Hak Ediş Karnesi", "Partner Contribution & Equity Dossier"),
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2D4A))

                // Stats Matrix with Grade Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1E36))
                        .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Katkı Notu", "Grade"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText(performanceGrade, fontSize = 14.sp, fontWeight = FontWeight.Black, color = ThemeGold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Kâr Payı", "Equity Share"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText("%${sharePercent.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Teslimat", "Delivered"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText("$totalDelivered / $totalRequired", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = ThemeNeonCyan)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E2D4A)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CurrencyText(tr("Temettü", "Dividends"), fontSize = 8.sp, color = Color.Gray)
                        CurrencyText(formatCredit(totalDividends), fontSize = 11.sp, fontWeight = FontWeight.Black, color = ThemeGold)
                    }
                }

                // Breakdown of Contributed Products
                CurrencyText(
                    text = tr("📦 Üstlendiği Malzeme Kotaları & Teslimat Durumu:", "📦 Committed Material Quotas & Status:"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    partnerSlots.forEach { slot ->
                        val slotProgress = (slot.quantityDelivered.toFloat() / slot.quantityRequired.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF121F36),
                            border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF080F1D),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                UniversalProductIcon(
                                                    productId = slot.productId,
                                                    displayName = slot.productName,
                                                    size = 22.dp
                                                )
                                            }
                                        }

                                        Column {
                                            CurrencyText(
                                                text = slot.productName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            CurrencyText(
                                                text = "${slot.quantityDelivered} / ${slot.quantityRequired} " + tr("Adet", "Units"),
                                                fontSize = 8.5.sp,
                                                color = if (slot.isFullyDelivered) Color(0xFF34D399) else Color.LightGray
                                            )
                                        }
                                    }

                                    if (slot.isFullyDelivered) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF065F46)
                                        ) {
                                            CurrencyText(
                                                text = tr("✅ Tamamlandı", "✅ Done"),
                                                fontSize = 8.sp,
                                                color = Color(0xFFA7F3D0),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isLeader && !isMe) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onNudgePartner(slot.slotId)
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Rounded.Campaign, contentDescription = "Dürt", tint = ThemeGold, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onKickPartner(slot.slotId)
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Rounded.PersonRemove, contentDescription = "Kov", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { slotProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (slot.isFullyDelivered) Color(0xFF10B981) else ThemeNeonCyan,
                                    trackColor = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                            onOpenChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeNeonCyan, contentColor = Color(0xFF002026)),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        CurrencyText(tr("Telsizden Mesaj At", "Send Radio Message"), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    AppButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                        modifier = Modifier.weight(0.6f).height(34.dp)
                    ) {
                        CurrencyText(tr("Kapat", "Close"), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

/**
 * 7. CANLI TELSİZ VE GRUP SOHBETİ EKRANI (TACTICAL LIVE RADIO & CHAT)
 * Frekans spektrum animasyonu, hızlı taktik telsiz mesajları, emoji reaksiyonları ve mesaj akışı.
 */
@Composable
fun ConsortiumLiveTacticalChat(
    project: MegaProject,
    messages: List<ConsortiumChatMessage>,
    onSendMessage: (String) -> Unit,
    isMember: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var messageInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Animated audio frequency bars in header
    val infiniteTransition = rememberInfiniteTransition(label = "radio_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF080F1D),
        border = BorderStroke(1.2.dp, Color(0xFF1E3A5F)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Live Status Header with Audio Spectrum Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                        modifier = Modifier.size(12.dp)
                    ) {}

                    Column {
                        CurrencyText(
                            text = tr("CANLI TELSİZ KANALI", "LIVE TACTICAL RADIO"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        CurrencyText(
                            text = "FREQ: 142.85 MHz ● ŞİFRELİ HAT",
                            fontSize = 7.5.sp,
                            color = ThemeNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Animated frequency equalizer visualizer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val barCount = 7
                    for (i in 0 until barCount) {
                        val barHeight = 4f + (sin(wavePhase + i * 0.9f) * 6f + 6f).coerceIn(2f, 16f)
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(barHeight.dp)
                                .background(Color(0xFF34D399), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E2D4A))

            // Quick Tactical Phrases Chips
            if (isMember) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTacticalPhrases = listOf(
                        "📦 Malzeme yolda!",
                        "⚡ Acil parça desteği!",
                        "🚀 Seri üretime hazırız!",
                        "🤝 Harika iş çıkardık!",
                        "💰 Temettüler dağıtıldı",
                        "📢 Liderden duyuru",
                        "🔥 Üretim bandı tam güç!"
                    )

                    quickTacticalPhrases.forEach { phrase ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF121F33),
                            border = BorderStroke(1.dp, ThemeNeonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendMessage(phrase)
                            }
                        ) {
                            CurrencyText(
                                text = phrase,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeNeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick Reaction Emoji Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickEmojis = listOf("🚀", "💰", "📦", "🔥", "👏", "⚡", "🏆")
                    quickEmojis.forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F1829),
                            border = BorderStroke(0.8.dp, Color(0xFF1E2D4A)),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSendMessage(emoji)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CurrencyText(text = emoji, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Message Stream Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF040913))
                    .border(1.dp, Color(0xFF152238), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Forum, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        CurrencyText(
                            text = tr("Telsiz frekansı açık. Konsorsiyum ortaklarınıza ilk mesajı iletin!", "Radio channel active. Broadcast the first message to partners!"),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ConsortiumTacticalMessageBubble(msg = msg)
                        }
                    }
                }
            }

            // Send Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { if (isMember) messageInput = it },
                    enabled = isMember,
                    placeholder = {
                        CurrencyText(
                            text = if (isMember) tr("Telsizden mesaj gönder...", "Broadcast message...") else tr("🔒 Sadece üyeler mesaj gönderebilir", "🔒 Only members can broadcast"),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeNeonCyan,
                        unfocusedBorderColor = Color(0xFF1E2D4A),
                        focusedContainerColor = Color(0xFF0B1424),
                        unfocusedContainerColor = Color(0xFF0B1424),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val trimmed = messageInput.trim()
                            if (trimmed.isNotBlank() && isMember) {
                                onSendMessage(trimmed)
                                messageInput = ""
                                focusManager.clearFocus()
                            }
                        }
                    )
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMember && messageInput.isNotBlank()) ThemeNeonCyan else Color(0xFF1E293B),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(enabled = isMember && messageInput.isNotBlank()) {
                            val trimmed = messageInput.trim()
                            if (trimmed.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSendMessage(trimmed)
                                messageInput = ""
                                focusManager.clearFocus()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = if (isMember && messageInput.isNotBlank()) Color(0xFF002026) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Taktik Telsiz Mesaj Balonu
 */
@Composable
fun ConsortiumTacticalMessageBubble(msg: ConsortiumChatMessage) {
    val isMe = msg.senderId == "local_player"
    val formattedTime = remember(msg.timestampMs) {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(msg.timestampMs))
    }

    if (msg.isSystemMessage) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E1B4B),
                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CurrencyText(text = "📢 " + msg.messageText, fontSize = 8.5.sp, color = Color(0xFFC7D2FE), fontWeight = FontWeight.Medium)
                    CurrencyText(text = formattedTime, fontSize = 7.5.sp, color = Color.Gray)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CurrencyText(
                    text = msg.senderName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isMe) ThemeNeonCyan else ThemeGold
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF1E2D4A)
                ) {
                    CurrencyText(
                        text = msg.senderRole,
                        fontSize = 7.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
                CurrencyText(text = formattedTime, fontSize = 7.5.sp, color = Color.Gray)
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = if (isMe) 8.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 8.dp
                ),
                color = if (isMe) Color(0xFF0284C7) else Color(0xFF182338),
                border = BorderStroke(1.dp, if (isMe) ThemeNeonCyan.copy(alpha = 0.6f) else Color(0xFF283B5C))
            ) {
                CurrencyText(
                    text = msg.messageText,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}
