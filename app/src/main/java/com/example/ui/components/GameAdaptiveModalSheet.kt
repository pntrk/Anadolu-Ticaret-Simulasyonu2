package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

/**
 * Tek Tip Modern Fintech Açılır Menü ve Modal Şablon Sistemi.
 * Küçük ekranlarda (telefon) alttan açılır ModalBottomSheet,
 * Geniş ekranlarda (tablet / yatay mod) merkezî Modal Dialog olarak çalışır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameAdaptiveModalSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = ThemeNeonCyan,
    badgeText: String? = null,
    badgeColor: Color = ThemeNeonCyan,
    primaryButtonText: String? = null,
    primaryButtonEnabled: Boolean = true,
    primaryButtonIcon: ImageVector? = null,
    primaryButtonColor: Color = ThemeGold,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    maxHeightRatio: Float = 0.92f,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTabletOrWide = configuration.screenWidthDp >= 600

    if (isTabletOrWide) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3050914)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = modifier
                        .widthIn(max = 580.dp)
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight(maxHeightRatio)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF223147)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GameModalHeader(
                            title = title,
                            subtitle = subtitle,
                            icon = icon,
                            iconTint = iconTint,
                            badgeText = badgeText,
                            badgeColor = badgeColor,
                            onClose = onDismissRequest
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color(0xFF1E293B)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                content()
                            }
                        }

                        if (primaryButtonText != null || secondaryButtonText != null) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = Color(0xFF1E293B)
                            )
                            GameModalFooter(
                                primaryButtonText = primaryButtonText,
                                primaryButtonEnabled = primaryButtonEnabled,
                                primaryButtonIcon = primaryButtonIcon,
                                primaryButtonColor = primaryButtonColor,
                                onPrimaryAction = onPrimaryAction,
                                secondaryButtonText = secondaryButtonText,
                                onSecondaryAction = onSecondaryAction ?: onDismissRequest
                            )
                        }
                    }
                }
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = Color(0xFF0F172A),
            scrimColor = Color(0xB3050914),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .background(Color(0xFF334155), CircleShape)
                )
            }
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(maxHeightRatio)
                    .navigationBarsPadding()
            ) {
                GameModalHeader(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    iconTint = iconTint,
                    badgeText = badgeText,
                    badgeColor = badgeColor,
                    onClose = onDismissRequest
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFF1E293B)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        content()
                    }
                }

                if (primaryButtonText != null || secondaryButtonText != null) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFF1E293B)
                    )
                    GameModalFooter(
                        primaryButtonText = primaryButtonText,
                        primaryButtonEnabled = primaryButtonEnabled,
                        primaryButtonIcon = primaryButtonIcon,
                        primaryButtonColor = primaryButtonColor,
                        onPrimaryAction = onPrimaryAction,
                        secondaryButtonText = secondaryButtonText,
                        onSecondaryAction = onSecondaryAction ?: onDismissRequest
                    )
                }
            }
        }
    }
}

/**
 * Tek Tip Başlık Barı (Icon + Başlık + Rozet + Kapatma Butonu)
 */
@Composable
fun GameModalHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = ThemeNeonCyan,
    badgeText: String? = null,
    badgeColor: Color = ThemeNeonCyan
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badgeText != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClose()
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B).copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Kapat",
                tint = ThemeTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Tek Tip Alt Aksiyon Çubuğu (Action Footer)
 */
@Composable
fun GameModalFooter(
    primaryButtonText: String?,
    onPrimaryAction: (() -> Unit)?,
    secondaryButtonText: String?,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    primaryButtonEnabled: Boolean = true,
    primaryButtonIcon: ImageVector? = null,
    primaryButtonColor: Color = ThemeGold
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (secondaryButtonText != null) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSecondaryAction()
                },
                modifier = Modifier
                    .weight(if (primaryButtonText != null) 0.8f else 1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ThemeTextSecondary
                )
            ) {
                Text(
                    text = secondaryButtonText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        if (primaryButtonText != null && onPrimaryAction != null) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPrimaryAction()
                },
                enabled = primaryButtonEnabled,
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryButtonColor,
                    contentColor = if (primaryButtonColor == ThemeGold) Color(0xFF1A1300) else Color.White,
                    disabledContainerColor = Color(0xFF26334D),
                    disabledContentColor = Color.Gray
                )
            ) {
                if (primaryButtonIcon != null) {
                    Icon(
                        imageVector = primaryButtonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = primaryButtonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Tek Tip Koyu Fintech Açılır Seçim Menüsü (Unified Dropdown Menu)
 */
@Composable
fun GameUnifiedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    widthMin: Dp = 180.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .widthIn(min = widthMin)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF2C3C56), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

/**
 * Tek Tip Seçim Menüsü Öğesi (Unified Dropdown Menu Item)
 */
@Composable
fun GameUnifiedDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = ThemeNeonCyan,
    trailingText: String? = null,
    isSelected: Boolean = false,
    selectedColor: Color = ThemeNeonCyan
) {
    val haptic = LocalHapticFeedback.current

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = text,
                    color = if (isSelected) selectedColor else ThemeTextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trailingText != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = trailingText,
                        color = ThemeTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isSelected) selectedColor else iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = selectedColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .background(if (isSelected) selectedColor.copy(alpha = 0.12f) else Color.Transparent)
            .fillMaxWidth()
    )
}
