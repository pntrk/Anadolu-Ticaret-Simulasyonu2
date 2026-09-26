package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Optimized, Haptic-Enabled AppButton:
 * - Emits tactile haptic micro-vibrations upon tap and press
 * - Uses hardware-accelerated graphicsLayer scale bounce with spring physics
 * - Memoizes interaction sources and eliminates unneeded layout passes
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(6.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    hapticFeedbackEnabled: Boolean = true,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()

    // Trigger haptic micro-tick on immediate press down
    LaunchedEffect(isPressed) {
        if (isPressed && enabled && hapticFeedbackEnabled) {
            haptic.performHapticFeedback(hapticType)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_button_scale"
    )

    Button(
        onClick = {
            if (hapticFeedbackEnabled) {
                haptic.performHapticFeedback(hapticType)
            }
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Micro-Interaction Enabled Outlined App Button
 */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF3B82F6)),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    hapticFeedbackEnabled: Boolean = true,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled && hapticFeedbackEnabled) {
            haptic.performHapticFeedback(hapticType)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_outlined_button_scale"
    )

    OutlinedButton(
        onClick = {
            if (hapticFeedbackEnabled) {
                haptic.performHapticFeedback(hapticType)
            }
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Micro-Interaction Enabled Icon Button with Tactile Feedback
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    hapticFeedbackEnabled: Boolean = true,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled && hapticFeedbackEnabled) {
            haptic.performHapticFeedback(hapticType)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_icon_button_scale"
    )

    IconButton(
        onClick = {
            if (hapticFeedbackEnabled) {
                haptic.performHapticFeedback(hapticType)
            }
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

