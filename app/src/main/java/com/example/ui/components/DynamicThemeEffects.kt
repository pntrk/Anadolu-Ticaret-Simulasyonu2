package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive

/**
 * Applies a smooth pulsating glow border to cards based on profitability state.
 * Green glow when profitable, Red glow when losing money/in deficit.
 */
@Composable
fun Modifier.profitGlowBorder(
    isProfitable: Boolean,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(4.dp)
): Modifier {
    if (!enabled) return this
    val glowColor = if (isProfitable) ThemePositive else ThemeNegative
    val transition = rememberInfiniteTransition(label = "profitGlow")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    return this.border(
        border = BorderStroke(1.2.dp, glowColor.copy(alpha = alpha)),
        shape = shape
    )
}

/**
 * Reusable Theme Picker Section displaying selectable theme options.
 * Tıklanınca açılan liste (dropdown/expandable selector) gösterimi.
 */
@Composable
fun ThemePickerSection(
    selectedThemeId: String,
    onSelectTheme: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("all") } // "all", "light", "dark"

    val currentTheme = remember(selectedThemeId) {
        AppThemeOption.fromId(selectedThemeId)
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "themeDropdownArrow"
    )

    val filteredThemes = remember(selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "light" -> AppThemeOption.values().filter { !it.isDark }
            "dark" -> AppThemeOption.values().filter { it.isDark }
            else -> AppThemeOption.values().toList()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tıklanınca Açılan Ana Kart (Dropdown Trigger Card)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isExpanded) currentTheme.surfaceVariantColor else currentTheme.surfaceColor,
            border = BorderStroke(
                width = if (isExpanded) 1.5.dp else 1.dp,
                color = if (isExpanded) currentTheme.primaryColor else currentTheme.borderColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isExpanded = !isExpanded
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Tema Emojisi & İkon Kutusu
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentTheme.backgroundColor)
                            .border(1.dp, currentTheme.borderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentTheme.emoji, fontSize = 20.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentTheme.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.textPrimaryColor
                            )

                            // Mod Etiketi (Aydınlık / Karanlık)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (currentTheme.isDark) Color(0xFF1E293B) else Color(0xFFFEF3C7),
                                border = BorderStroke(0.5.dp, if (currentTheme.isDark) Color(0xFF475569) else Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = if (currentTheme.isDark) "🌙 KARANLIK" else "☀️ AYDINLIK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isExpanded) "Listeyi kapatmak için dokunun" else "Temayı değiştirmek için dokunun (${AppThemeOption.values().size} seçenek)",
                            style = MaterialTheme.typography.bodySmall,
                            color = currentTheme.textSecondaryColor,
                            fontSize = 11.sp
                        )
                    }
                }

                // Sağ Taraf: Renk önizleme noktaları ve Açılır Liste Butonu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(currentTheme.primaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(currentTheme.secondaryColor)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isExpanded) currentTheme.primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            0.5.dp,
                            if (isExpanded) currentTheme.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Kapat" else "Seç",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpanded) currentTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Listeyi Kapat" else "Listeyi Aç",
                                tint = if (isExpanded) currentTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }
                }
            }
        }

        // Tıklanınca Açılan Tema Listesi (AnimatedVisibility)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(200))
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kategori Filtre Butonları (Tüm / Aydınlık / Karanlık)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "all" to "🌐 TÜMÜ (${AppThemeOption.values().size})",
                            "light" to "☀️ AYDINLIK (${AppThemeOption.values().count { !it.isDark }})",
                            "dark" to "🌙 KARANLIK (${AppThemeOption.values().count { it.isDark }})"
                        ).forEach { (filterKey, label) ->
                            val isFilterSelected = selectedCategoryFilter == filterKey
                            FilterChip(
                                selected = isFilterSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategoryFilter = filterKey
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isFilterSelected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // Tema Seçenekleri Listesi
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredThemes.forEach { theme: AppThemeOption ->
                            val isSelected = theme.id == selectedThemeId

                            Surface(
                                shape = CutCornerShape(8.dp),
                                color = if (isSelected) theme.surfaceVariantColor else theme.surfaceColor,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) theme.primaryColor else theme.borderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectTheme(theme.id)
                                        isExpanded = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(theme.backgroundColor)
                                                .border(1.dp, theme.borderColor, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = theme.emoji, fontSize = 16.sp)
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = theme.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) theme.primaryColor else theme.textPrimaryColor
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (theme.isDark) Color(0xFF1E293B) else Color(0xFFFEF3C7),
                                                    border = BorderStroke(0.5.dp, if (theme.isDark) Color(0xFF475569) else Color(0xFFF59E0B))
                                                ) {
                                                    Text(
                                                        text = if (theme.isDark) "🌙 KARANLIK" else "☀️ AYDINLIK",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (theme.isDark) Color(0xFF94A3B8) else Color(0xFFB45309),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }

                                                if (isSelected) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = theme.primaryColor.copy(alpha = 0.2f),
                                                        border = BorderStroke(0.5.dp, theme.primaryColor)
                                                    ) {
                                                        Text(
                                                            text = "AKTİF",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = theme.primaryColor,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 8.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = theme.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = theme.textSecondaryColor,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    // Vurgu Renkleri ve Seçim İkonu
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(theme.primaryColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(theme.secondaryColor)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = theme.primaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Listeyi Kapat Butonu
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Text(
                            text = "▲ Listeyi Kapat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
