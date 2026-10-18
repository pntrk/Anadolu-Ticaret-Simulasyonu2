package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.R
import com.example.data.CompanyManager
import com.example.data.PlayerEntity
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.isEnglishLanguage
import com.example.ui.theme.trAuto
import java.util.Locale

/**
 * Data model for Manager matching user specification.
 */
data class Manager(
    val name: String,
    val title: String,
    val salary: String,
    val passiveEffect: String,
    val logText: String,
    val level: Int,
    val hierarchy: Int,
    val timestamp: String = "19:54",
    val logCount: Int = 10,
    @DrawableRes val templateRes: Int = R.drawable.k1,
    val isHired: Boolean = true,
    val isActive: Boolean = true,
    val hireCostGems: Int = 0
)

val TURKISH_FEMALE_NAMES = listOf(
    "Ayşe Kaya, CFA",
    "Canan Çelik",
    "Zeynep Demir",
    "Banu Aydın, MBA",
    "Elif Yılmaz",
    "Merve Şahin",
    "Ebru Öztürk",
    "Gamze Doğan",
    "Fatma Arslan",
    "Leyla Çelik"
)

val TURKISH_MALE_NAMES = listOf(
    "Burak Koç",
    "Dr. Selim Arslan",
    "Ahmet Yılmaz",
    "Mehmet Öz",
    "Caner Demir",
    "Murat Şahin",
    "Emre Yıldız",
    "Kemal Öztürk",
    "Hakan Doğan",
    "Erdem Arslan"
)

/**
 * ManagerProfileCard
 * 
 * Renders the static executive card template plate (female/male) as background
 * and places dynamic game database information onto designated slots with ConstraintLayout.
 * 
 * STRICT RULES & SHIFT PREVENTION:
 * 1. Background uses Box + Image(painterResource, contentScale = ContentScale.FillBounds).
 * 2. Avatar/Portrait is anchored on the left side (0.00f to ~0.415f). Content strictly starts at 0.425f
 *    to prevent any text from overlapping or shifting into the avatar photo.
 * 3. Left and right boxes use exact matching guidelines with 0.265f symmetrical width and a 0.015f gutter.
 * 4. All texts have strict maxLines, ellipsis, and scaled typography to guarantee zero layout shift.
 */
@Composable
fun ManagerProfileCard(
    manager: Manager,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    onLevelClick: (() -> Unit)? = null,
    onSalaryClick: (() -> Unit)? = null,
    onEffectClick: (() -> Unit)? = null,
    onLogClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.84f)
            .testTag("manager_profile_card_${manager.name}")
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color(0xFFFFD700).copy(alpha = 0.2f)),
                        onClick = onCardClick
                    )
                } else Modifier
            )
    ) {
        // 1. Static Plate Background Template
        Image(
            painter = painterResource(id = manager.templateRes),
            contentDescription = "${manager.name} - ${manager.title}",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // 2. ConstraintLayout with strict guidelines to lock text in position without shift
        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            // Strict Content Guidelines anchored to the visual slots on the card templates
            val glTopTitle = createGuidelineFromTop(0.040f)
            val glBottomTitle = createGuidelineFromTop(0.260f)
            val glStartContent = createGuidelineFromStart(0.425f)
            val glDividerHeader = createGuidelineFromStart(0.710f)
            val glEndHierarchy = createGuidelineFromEnd(0.030f)

            val glTopMid = createGuidelineFromTop(0.275f)
            val glBottomMid = createGuidelineFromTop(0.660f)

            val glStartSalary = createGuidelineFromStart(0.425f)
            val glEndSalary = createGuidelineFromStart(0.690f)

            val glStartEffect = createGuidelineFromStart(0.705f)
            val glEndEffect = createGuidelineFromEnd(0.030f)

            val glTopBottomBar = createGuidelineFromTop(0.675f)
            val glBottomBottomBar = createGuidelineFromTop(0.795f)

            val glStartLog = createGuidelineFromStart(0.425f)
            val glEndLog = createGuidelineFromEnd(0.030f)

            val glTopButtons = createGuidelineFromTop(0.810f)
            val glBottomButtons = createGuidelineFromTop(0.965f)

            val glStartLeftBtn = createGuidelineFromStart(0.425f)
            val glEndLeftBtn = createGuidelineFromStart(0.690f)

            val glStartRightBtn = createGuidelineFromStart(0.705f)
            val glEndRightBtn = createGuidelineFromEnd(0.030f)

            // References
            val (titleRef, starsRef, salaryRef, effectRef, logRef, leftBtnRef, rightBtnRef) = createRefs()

            // 1. Üst Sol (İsim ve Ünvan)
            Surface(
                modifier = Modifier
                    .constrainAs(titleRef) {
                        top.linkTo(glTopTitle)
                        bottom.linkTo(glBottomTitle)
                        start.linkTo(glStartContent)
                        end.linkTo(glDividerHeader)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .padding(end = 3.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101B2C),
                border = BorderStroke(1.dp, Color(0xFF1E2D4A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = manager.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = manager.title,
                        color = Color(0xFFFFD700),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Sağ Üst (Yıldızlar ve Hiyerarşi Mührü)
            Surface(
                modifier = Modifier
                    .constrainAs(starsRef) {
                        top.linkTo(glTopTitle)
                        bottom.linkTo(glBottomTitle)
                        start.linkTo(glDividerHeader)
                        end.linkTo(glEndHierarchy)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .padding(start = 3.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101B2C),
                border = BorderStroke(1.dp, Color(0xFF1E2D4A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        for (i in 1..5) {
                            val isFilled = i <= manager.level
                            Text(
                                text = "★",
                                color = if (isFilled) Color(0xFFFFD700) else Color(0xFFFFD700).copy(alpha = 0.25f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = "#${manager.hierarchy}",
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RobotoMonoFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Orta Sol Kutu (Maaş & Canlı Mini Sparkline Çizgi Grafiği)
            Surface(
                modifier = Modifier
                    .constrainAs(salaryRef) {
                        top.linkTo(glTopMid)
                        bottom.linkTo(glBottomMid)
                        start.linkTo(glStartSalary)
                        end.linkTo(glEndSalary)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .then(
                        if (onSalaryClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color(0xFFFFD700).copy(alpha = 0.3f)),
                                onClick = onSalaryClick
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101B2C),
                border = BorderStroke(1.dp, Color(0xFF1E2D4A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (manager.isHired) "Maaş" else "Bordro",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (manager.isHired) {
                            Text(
                                text = if (manager.isActive) "● Aktif" else "● İzinli",
                                color = if (manager.isActive) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Ⓐ",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = manager.salary.replace("₳", "").trim(),
                            color = Color(0xFFFFD700),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = RobotoMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Sparkline chart
                    SparklineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(13.dp)
                    )
                }
            }

            // 4. Orta Sağ Kutu (Hiyerarşi & Pasif Etki + Dairesel Yüzde Göstergesi)
            Surface(
                modifier = Modifier
                    .constrainAs(effectRef) {
                        top.linkTo(glTopMid)
                        bottom.linkTo(glBottomMid)
                        start.linkTo(glStartEffect)
                        end.linkTo(glEndEffect)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .then(
                        if (onEffectClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                                onClick = onEffectClick
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101B2C),
                border = BorderStroke(1.dp, Color(0xFF1E2D4A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pasif Etki & Verim",
                        color = Color(0xFF94A3B8),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressGauge(
                            percentage = (50 + manager.level * 10).coerceAtMost(100),
                            modifier = Modifier.size(23.dp)
                        )

                        Text(
                            text = manager.passiveEffect,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Alt İnce Bar (Son Karar Logu & Zaman Damgası)
            Surface(
                modifier = Modifier
                    .constrainAs(logRef) {
                        top.linkTo(glTopBottomBar)
                        bottom.linkTo(glBottomBottomBar)
                        start.linkTo(glStartLog)
                        end.linkTo(glEndLog)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .then(
                        if (onLogClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.Cyan.copy(alpha = 0.2f)),
                                onClick = onLogClick
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(5.dp),
                color = Color(0xFF0C1524),
                border = BorderStroke(0.8.dp, Color(0xFF1E2D4A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "●",
                            color = Color(0xFFFFD700),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = manager.logText,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = manager.timestamp,
                        color = Color(0xFF64748B),
                        fontSize = 7.5.sp,
                        fontFamily = RobotoMonoFontFamily,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
            }

            // 6. Sol Alt Buton: "📜 Sicil & Loglar"
            Surface(
                modifier = Modifier
                    .constrainAs(leftBtnRef) {
                        top.linkTo(glTopButtons)
                        bottom.linkTo(glBottomButtons)
                        start.linkTo(glStartLeftBtn)
                        end.linkTo(glEndLeftBtn)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .then(
                        if (onLogClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color(0xFFFFD700).copy(alpha = 0.3f)),
                                onClick = onLogClick
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF142033),
                border = BorderStroke(1.dp, Color(0xFF263959))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = if (manager.isHired) "📜" else "📄", fontSize = 8.5.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (manager.isHired) "Sicil (${manager.logCount})" else "Aday Dosyası",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 7. Sağ Alt Buton: "★ Terfi / Maks (Lvl X)" veya "💎 İşe Al"
            Surface(
                modifier = Modifier
                    .constrainAs(rightBtnRef) {
                        top.linkTo(glTopButtons)
                        bottom.linkTo(glBottomButtons)
                        start.linkTo(glStartRightBtn)
                        end.linkTo(glEndRightBtn)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .then(
                        if (onLevelClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color(0xFFFFD700).copy(alpha = 0.4f)),
                                onClick = onLevelClick
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(6.dp),
                color = if (manager.isHired) Color(0xFF18263D) else Color(0xFF2B2208),
                border = BorderStroke(1.dp, if (manager.isHired) Color(0xFF384E75) else Color(0xFFFFD700).copy(alpha = 0.6f))
            ) {
                if (manager.isHired) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "★", color = Color(0xFFFFD700), fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (manager.level >= 5) "Maks (Lvl 5)" else "Terfi (Lvl ${manager.level})",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Circular level badge at the right edge
                        Box(
                            modifier = Modifier.size(15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${manager.level}",
                                color = Color(0xFFFFD700),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RobotoMonoFontFamily
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "💎", fontSize = 8.5.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (manager.hireCostGems > 0) "İşe Al (${manager.hireCostGems}💎)" else "İşe Al",
                            color = Color(0xFFFFD700),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Canvas Sparkline Chart Component
 */
@Composable
fun SparklineChart(
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFFD700)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0) return@Canvas

        val points = listOf(
            0.1f to 0.7f,
            0.25f to 0.5f,
            0.4f to 0.8f,
            0.55f to 0.3f,
            0.7f to 0.6f,
            0.85f to 0.2f,
            1.0f to 0.4f
        )

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { index, (xRatio, yRatio) ->
            val x = xRatio * width
            val y = yRatio * height

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = points[index - 1].first * width
                val prevY = points[index - 1].second * height
                val controlX = (prevX + x) / 2f
                path.cubicTo(controlX, prevY, controlX, y, x, y)
                fillPath.cubicTo(controlX, prevY, controlX, y, x, y)
            }

            if (index == points.lastIndex) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Fill under chart
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Draw sparkline path
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw end dot
        val lastPoint = points.last()
        drawCircle(
            color = lineColor,
            radius = 2.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastPoint.first * width, lastPoint.second * height)
        )
    }
}

/**
 * Canvas Circular Progress Gauge Ring (%70)
 */
@Composable
fun CircularProgressGauge(
    percentage: Int,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD700)
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()

            // Background Track
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Active Arc
            val sweep = (percentage / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }

        Text(
            text = "%$percentage",
            color = Color.White,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = RobotoMonoFontFamily
        )
    }
}

/**
 * Overloaded ManagerProfileCard for CompanyManager entity integration
 */
@Composable
fun ManagerProfileCard(
    manager: CompanyManager,
    player: PlayerEntity? = null,
    modifier: Modifier = Modifier,
    onHire: (() -> Unit)? = null,
    onFire: (() -> Unit)? = null,
    onUpgrade: (() -> Unit)? = null,
    onToggleActive: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val isEng = isEnglishLanguage()

    val templateRes = remember(manager.id, manager.name) {
        getManagerTemplateDrawable(manager)
    }

    val hierarchy = remember(manager.id) {
        getManagerHierarchyNumber(manager)
    }

    val salaryFormatted = remember(manager.dailySalary) {
        val inK = manager.dailySalary / 1000.0
        if (inK >= 1.0) {
            String.format(Locale.US, "₳ %.1f K", inK)
        } else {
            "₳ ${manager.dailySalary}"
        }
    }

    val passiveEffect = remember(manager.specialty, manager.level, isEng) {
        getManagerPassiveEffect(manager, isEng)
    }

    val logText = remember(manager.actionLogs, manager.name, manager.isHired, isEng) {
        val firstLog = manager.actionLogs.firstOrNull()?.description
        if (firstLog != null) {
            "${manager.name}: ${firstLog.trAuto(isEng)}"
        } else if (manager.isHired) {
            if (isEng) "${manager.name}: Autopilot decision engine active." else "${manager.name}: Operasyonel süreçler ve karar motoru devrede."
        } else {
            if (isEng) "${manager.name}: Candidate ready for review and appointment." else "${manager.name}: Aday havuzunda incelemeye ve atamaya hazır."
        }
    }

    val displayName = remember(manager.id, manager.name) {
        getManagerDisplayName(manager)
    }

    val localizedTitle = remember(manager.title, isEng) {
        manager.title.trAuto(isEng)
    }

    val timestamp = remember(manager.actionLogs) {
        val ms = manager.actionLogs.firstOrNull()?.timestampMs ?: System.currentTimeMillis()
        java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(ms))
    }

    val managerData = remember(manager, templateRes, hierarchy, salaryFormatted, passiveEffect, logText, displayName, localizedTitle, timestamp) {
        Manager(
            name = displayName,
            title = localizedTitle,
            salary = salaryFormatted,
            passiveEffect = passiveEffect,
            logText = logText,
            level = manager.level,
            hierarchy = hierarchy,
            timestamp = timestamp,
            logCount = manager.actionLogs.size.coerceAtLeast(1),
            templateRes = templateRes,
            isHired = manager.isHired,
            isActive = manager.isActive,
            hireCostGems = manager.hireGemCost
        )
    }

    ManagerProfileCard(
        manager = managerData,
        modifier = modifier,
        onCardClick = onClick,
        onLevelClick = if (manager.isHired) onUpgrade else onHire,
        onSalaryClick = if (manager.isHired) onToggleActive else onClick,
        onEffectClick = onClick,
        onLogClick = onClick
    )
}

/**
 * Returns matching male (e1..e10) or female (k1..k10) name for the manager.
 */
fun getManagerDisplayName(manager: CompanyManager): String {
    if (manager.name.isNotBlank()) return manager.name
    val isFemale = isFemaleManager(manager)
    val hash = Math.abs(manager.id.hashCode())
    return if (isFemale) {
        val index = hash % TURKISH_FEMALE_NAMES.size
        TURKISH_FEMALE_NAMES[index]
    } else {
        val index = hash % TURKISH_MALE_NAMES.size
        TURKISH_MALE_NAMES[index]
    }
}

private val TURKISH_FEMALE_FIRST_NAMES = setOf(
    "ayşe", "fatma", "emine", "hatice", "zeynep", "elif", "merve", "büşra", "selin", "ece",
    "derya", "gözde", "ceren", "aslı", "canan", "nazlı", "pınar", "tuğba", "ebru", "gizem",
    "irem", "seda", "hande", "banu", "gamze", "leyla", "esra", "eda", "sevgi", "özlem",
    "melek", "songül", "burcu", "yasemin", "deniz", "defne", "damla", "dilek", "berna",
    "nilgün", "filiz", "hale", "jale", "melis", "özge", "şeyma", "tülay", "yeşim", "zuhal"
)

private val TURKISH_MALE_FIRST_NAMES = setOf(
    "ahmet", "mehmet", "mustafa", "ali", "hüseyin", "hasan", "ibrahim", "ismail", "osman", "murat",
    "ömer", "yusuf", "emre", "kaan", "burak", "eren", "oğuz", "serkan", "volkan",
    "cem", "koray", "onur", "barış", "kerem", "mert", "alper", "selim", "kemal", "hakan", "erdem",
    "caner", "tolga", "tarık", "engin", "cihan", "fatih", "yavuz", "orhan", "sinan", "zafer"
)

fun isFemaleManager(manager: CompanyManager): Boolean {
    val cleanName = manager.name.trim().lowercase(Locale("tr"))
    val tokens = cleanName.split(" ", ".", ",").filter { it.isNotBlank() }
    val firstName = tokens.firstOrNull { it !in listOf("dr", "prof", "müh", "av", "uzm") } ?: ""

    // 1. Direct female first name match (e.g. Nazlı Bozkurt -> female)
    if (TURKISH_FEMALE_FIRST_NAMES.contains(firstName)) return true
    if (TURKISH_FEMALE_FIRST_NAMES.any { cleanName.startsWith(it) || cleanName.contains(it) }) return true

    // 2. Direct male first name match
    if (TURKISH_MALE_FIRST_NAMES.contains(firstName)) return false
    if (TURKISH_MALE_FIRST_NAMES.any { cleanName.startsWith(it) || cleanName.contains(it) }) return false

    // 3. Fallback to default specialty only when name is generic or blank
    return when (manager.id) {
        "mgr_treasury", "mgr_contracts", "mgr_logistics", "mgr_hr" -> true
        else -> false
    }
}

/**
 * Deterministically maps e1-e10 templates to male names, k1-k10 templates to female names.
 */
fun getManagerTemplateDrawable(manager: CompanyManager): Int {
    val isFemale = isFemaleManager(manager)
    val nameSeed = if (manager.name.isNotBlank()) manager.name else manager.id
    val hash = Math.abs(nameSeed.hashCode())
    val index = (hash % 10) + 1

    return if (isFemale) {
        getFemaleTemplateResource(index)
    } else {
        getMaleTemplateResource(index)
    }
}

fun getFemaleTemplateResource(index: Int): Int {
    return when (index) {
        1 -> R.drawable.k1
        2 -> R.drawable.k2
        3 -> R.drawable.k3
        4 -> R.drawable.k4
        5 -> R.drawable.k5
        6 -> R.drawable.k6
        7 -> R.drawable.k7
        8 -> R.drawable.k8
        9 -> R.drawable.k9
        10 -> R.drawable.k10
        else -> R.drawable.k1
    }
}

fun getMaleTemplateResource(index: Int): Int {
    return when (index) {
        1 -> R.drawable.e1
        2 -> R.drawable.e2
        3 -> R.drawable.e3
        4 -> R.drawable.e4
        5 -> R.drawable.e5
        6 -> R.drawable.e6
        7 -> R.drawable.e7
        8 -> R.drawable.e8
        9 -> R.drawable.e9
        10 -> R.drawable.e10
        else -> R.drawable.e1
    }
}

fun getManagerHierarchyNumber(manager: CompanyManager): Int {
    return when (manager.id) {
        "mgr_treasury" -> 1
        "mgr_contracts" -> 2
        "mgr_borsa" -> 3
        "mgr_logistics" -> 4
        "mgr_hr" -> 5
        "mgr_rd" -> 5
        "mgr_prod" -> 5
        "mgr_maintenance" -> 5
        else -> 5
    }
}

fun getManagerPassiveEffect(manager: CompanyManager, isEng: Boolean = false): String {
    return if (isEng) {
        when (manager.specialty) {
            "treasury" -> "Cash Reserve Buffer %${50 + manager.level * 10}"
            "contracts" -> "Forward Tender Discount %${30 + manager.level * 10}"
            "borsa" -> "Exchange Fee Advantage %${25 + manager.level * 15}"
            "logistics" -> "Logistics Speed +%${40 + manager.level * 15}"
            "hr" -> "Staff Efficiency +%${30 + manager.level * 12}"
            "rd" -> "R&D Tech Innovation +%${35 + manager.level * 15}"
            "production" -> "Factory Output Speed +%${30 + manager.level * 14}"
            "maintenance" -> "Plant Maintenance Discount %${40 + manager.level * 12}"
            else -> "Operational Efficiency +%${manager.level * 20}"
        }
    } else {
        when (manager.specialty) {
            "treasury" -> "Nakit Rezerv Koruma %${50 + manager.level * 10}"
            "contracts" -> "Vadeli İhale İndirimi %${30 + manager.level * 10}"
            "borsa" -> "Borsa Komisyon Avantajı %${25 + manager.level * 15}"
            "logistics" -> "Lojistik Sevkiyat Hızı +%${40 + manager.level * 15}"
            "hr" -> "Holding Personel Verimi +%${30 + manager.level * 12}"
            "rd" -> "Ar-Ge İnovasyon Hızı +%${35 + manager.level * 15}"
            "production" -> "Fabrika Üretim Hızı +%${30 + manager.level * 14}"
            "maintenance" -> "Tesis Bakım İndirimi %${40 + manager.level * 12}"
            else -> "Operasyonel Verim +%${manager.level * 20}"
        }
    }
}
