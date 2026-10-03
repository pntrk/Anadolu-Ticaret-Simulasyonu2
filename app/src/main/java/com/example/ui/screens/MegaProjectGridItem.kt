package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConsortiumQualityTier
import com.example.data.MegaProject
import com.example.data.MegaProjectStage
import com.example.ui.components.ProductDrawables
import com.example.ui.components.UniversalProductIcon
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.tr

@Composable
fun MegaProjectGridItem(
    project: MegaProject,
    onClick: () -> Unit
) {
    val isFinished = project.isAllStagesFinished
    val isStage4 = project.currentStage == MegaProjectStage.STAGE_4_MASS_PRODUCTION
    val cardBorderColor = when {
        isFinished -> Color(0xFF10B981)
        isStage4 -> Color(0xFFA855F7)
        else -> Color(0xFF233554)
    }
    val openSlotsCount = project.slots.count { it.assignedPartnerId == null }
    val productDrawableRes = remember(project.targetProductId) {
        ProductDrawables.getProductDrawableResId(project.targetProductId)
    }

    val tierBadgeText = when (project.qualityTier) {
        ConsortiumQualityTier.GRADE_A -> "🏆 A"
        ConsortiumQualityTier.GRADE_B -> "⭐ B"
        ConsortiumQualityTier.GRADE_C -> "📦 C"
    }
    val tierBadgeColor = when (project.qualityTier) {
        ConsortiumQualityTier.GRADE_A -> Color(0xFFF59E0B)
        ConsortiumQualityTier.GRADE_B -> ThemeNeonCyan
        ConsortiumQualityTier.GRADE_C -> Color(0xFF94A3B8)
    }

    com.example.ui.components.Interactive3DCard(
        shape = RoundedCornerShape(16.dp),
        maxTiltAngle = 6f,
        specularShine = true,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF09121F),
            border = BorderStroke(1.2.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Project Name & Quality Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.targetProductName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tierBadgeColor.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, tierBadgeColor)
                    ) {
                        Text(
                            text = tierBadgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = tierBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Brand Subtitle
                Text(
                    text = "${project.consortiumName} (${project.brandName})",
                    color = ThemeGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Visual Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0B111E)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
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
                        ProjectDynamicIcon(
                            project = project,
                            size = 50.dp,
                            iconSize = 28.dp,
                            showBadges = false
                        )
                    }
                }

                // Open Slot / Status Banner
                if (openSlotsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Handshake,
                                contentDescription = null,
                                tint = Color(0xFFA7F3D0),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tr("$openSlotsCount Açık Tedarik Kotası (Katıl)", "$openSlotsCount Open Quotas (Join)"),
                                fontSize = 9.5.sp,
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4 Supply Slots Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    project.slots.take(4).forEach { slot ->
                        val isFilled = slot.assignedPartnerId != null
                        val isDelivered = slot.isFullyDelivered
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isDelivered -> Color(0xFF064E3B)
                                isFilled -> Color(0xFF1E293B)
                                else -> Color(0xFF0B132B)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isDelivered -> Color(0xFF34D399)
                                    isFilled -> ThemeNeonCyan.copy(alpha = 0.7f)
                                    else -> Color(0xFF334155)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                UniversalProductIcon(
                                    productId = slot.productId,
                                    displayName = slot.productName,
                                    size = 18.dp
                                )
                                if (isDelivered) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF064E3B).copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Stage & Warehouse Stock Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📦 " + tr("Depo: ", "Depot: ") + "${project.warehouseStock} " + tr("Adet", "Units"),
                        fontSize = 9.sp,
                        color = if (project.warehouseStock > 0) Color(0xFF34D399) else Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (project.currentStage) {
                            MegaProjectStage.STAGE_1_BODY -> Color(0xFF3B82F6).copy(alpha = 0.25f)
                            MegaProjectStage.STAGE_2_HARDWARE -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                            MegaProjectStage.STAGE_3_TESTING -> Color(0xFFA855F7).copy(alpha = 0.25f)
                            MegaProjectStage.STAGE_4_MASS_PRODUCTION -> Color(0xFF10B981).copy(alpha = 0.25f)
                            MegaProjectStage.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.25f)
                        }
                    ) {
                        Text(
                            text = when (project.currentStage) {
                                MegaProjectStage.STAGE_1_BODY -> "I. Gövde"
                                MegaProjectStage.STAGE_2_HARDWARE -> "II. Donanım"
                                MegaProjectStage.STAGE_3_TESTING -> "III. Test"
                                MegaProjectStage.STAGE_4_MASS_PRODUCTION -> "IV. Seri Üretim"
                                MegaProjectStage.COMPLETED -> "Tamamlandı"
                            },
                            fontSize = 8.5.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Action Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (openSlotsCount > 0) ThemeNeonCyan.copy(alpha = 0.18f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (openSlotsCount > 0) ThemeNeonCyan else Color(0xFF334155))
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (openSlotsCount > 0) tr("🤝 Projeye Katıl & İncele", "🤝 Join & Inspect") else tr("📋 Projeyi İncele", "📋 Inspect Project"),
                            color = if (openSlotsCount > 0) ThemeNeonCyan else Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
