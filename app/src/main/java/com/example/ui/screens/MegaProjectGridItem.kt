package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    val cardBorderColor = if (isFinished) Color(0xFF10B981) else if (isStage4) Color(0xFFA855F7) else Color(0xFF233554)
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

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF09121F).copy(alpha = 0.85f),
        border = BorderStroke(1.2.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Visual Preview Box with Real Drawable Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0B111E)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (productDrawableRes != 0) {
                    Image(
                        painter = painterResource(id = productDrawableRes),
                        contentDescription = project.targetProductName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    ProjectDynamicIcon(
                        project = project,
                        size = 54.dp,
                        iconSize = 30.dp,
                        showBadges = false
                    )
                }

                // Top Quality Tier & Open Slots Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tierBadgeColor.copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, tierBadgeColor)
                    ) {
                        Text(
                            text = tierBadgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = tierBadgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (openSlotsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.9f),
                            border = BorderStroke(0.8.dp, Color(0xFF34D399))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Handshake,
                                    contentDescription = null,
                                    tint = Color(0xFFA7F3D0),
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = tr("$openSlotsCount Açık", "$openSlotsCount Open"),
                                    fontSize = 7.5.sp,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Titles
            Text(
                text = project.targetProductName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${project.consortiumName} (${project.brandName})",
                color = ThemeGold,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Supply Slots Icons (Real Images from Drawables)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                project.slots.take(4).forEach { slot ->
                    val isFilled = slot.assignedPartnerId != null
                    val isDelivered = slot.isFullyDelivered
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDelivered) Color(0xFF064E3B) else if (isFilled) Color(0xFF1E293B) else Color(0xFF0F172A),
                        border = BorderStroke(
                            0.8.dp,
                            if (isDelivered) Color(0xFF34D399) else if (isFilled) ThemeNeonCyan.copy(alpha = 0.6f) else Color(0xFF334155)
                        ),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            UniversalProductIcon(
                                productId = slot.productId,
                                displayName = slot.productName,
                                size = 16.dp
                            )
                            if (isDelivered) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF064E3B).copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = if (openSlotsCount > 0) ThemeNeonCyan.copy(alpha = 0.15f) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (openSlotsCount > 0) ThemeNeonCyan.copy(alpha = 0.7f) else Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (openSlotsCount > 0) tr("Katıl / İncele", "Join / Inspect") else tr("İncele", "Inspect"),
                        color = if (openSlotsCount > 0) ThemeNeonCyan else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

