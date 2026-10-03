package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * High-performance 3D interactive tilt container with specular glass shine
 * and realistic spring physics.
 */
@Composable
fun Interactive3DCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    maxTiltAngle: Float = 12f,
    specularShine: Boolean = true,
    scaleOnPress: Float = 1.025f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val shineAlpha = remember { Animatable(0f) }
    var shinePos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var cardSize by remember { mutableStateOf(IntSize(1, 1)) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    Box(
        modifier = modifier
            .onSizeChanged { cardSize = it }
            .graphicsLayer {
                this.rotationX = rotX.value
                this.rotationY = rotY.value
                this.scaleX = scale.value
                this.scaleY = scale.value
                this.cameraDistance = 16f * density.density
            }
            .clip(shape)
            .pointerInput(cardSize, onClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = cardSize.width.toFloat().coerceAtLeast(1f)
                    val h = cardSize.height.toFloat().coerceAtLeast(1f)

                    fun updateTouch(pos: Offset) {
                        val normX = ((pos.x / w) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
                        val normY = ((pos.y / h) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
                        shinePos = Offset(pos.x / w, pos.y / h)
                        
                        scope.launch {
                            rotY.snapTo(normX * maxTiltAngle)
                            rotX.snapTo(-normY * maxTiltAngle)
                            scale.animateTo(scaleOnPress, spring(stiffness = Spring.StiffnessHigh))
                            if (specularShine) shineAlpha.animateTo(0.28f, spring(stiffness = Spring.StiffnessHigh))
                        }
                    }

                    updateTouch(down.position)

                    var isDragged = false
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            if (change.positionChange() != Offset.Zero) {
                                isDragged = true
                                updateTouch(change.position)
                            }
                        } else {
                            // Touch released
                            if (!isDragged && onClick != null) {
                                onClick()
                            }
                            break
                        }
                    } while (event.changes.any { it.pressed })

                    // Reset with smooth bouncy physics
                    scope.launch {
                        launch { rotX.animateTo(0f, springSpec) }
                        launch { rotY.animateTo(0f, springSpec) }
                        launch { scale.animateTo(1f, springSpec) }
                        if (specularShine) launch { shineAlpha.animateTo(0f, springSpec) }
                    }
                }
            }
    ) {
        content()

        if (specularShine && shineAlpha.value > 0.01f) {
            val shineBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = shineAlpha.value),
                    Color.White.copy(alpha = shineAlpha.value * 0.35f),
                    Color.Transparent
                ),
                center = Offset(shinePos.x * cardSize.width, shinePos.y * cardSize.height),
                radius = (cardSize.width.coerceAtLeast(cardSize.height) * 0.85f).coerceAtLeast(100f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shineBrush)
            )
        }
    }
}
