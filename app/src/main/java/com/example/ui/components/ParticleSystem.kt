package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float = 1f,
    val color: Color,
    val size: Float
)

object ParticleManager {
    private val _trigger = MutableStateFlow<Long>(0)
    val trigger = _trigger.asStateFlow()

    fun spawnCelebration() {
        _trigger.value = System.currentTimeMillis()
    }
}

@Composable
fun ParticleSystem() {
    val triggerTime by ParticleManager.trigger.collectAsStateWithLifecycle()
    val particles = remember { mutableStateListOf<Particle>() }
    val scope = rememberCoroutineScope()
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(triggerTime) {
        if (triggerTime > 0) {
            // Spawn 50+ particles
            val newParticles = List(60) {
                val angle = Random.nextFloat() * 2 * Math.PI
                val speed = Random.nextFloat() * 20f + 5f
                Particle(
                    x = 0f, // Centered logically, rendered relatively
                    y = 0f,
                    vx = (kotlin.math.cos(angle) * speed).toFloat(),
                    vy = (kotlin.math.sin(angle) * speed).toFloat(),
                    color = if (Random.nextBoolean()) ThemeGold else ThemeNeonCyan,
                    size = Random.nextFloat() * 8f + 4f
                )
            }
            particles.clear()
            particles.addAll(newParticles)
            isAnimating = true

            scope.launch {
                val animatable = Animatable(1f)
                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(1500)
                ) {
                    particles.forEach { p ->
                        p.x += p.vx
                        p.y += p.vy
                        p.vy += 0.5f // gravity
                        p.life = this.value
                    }
                }
                isAnimating = false
                particles.clear()
            }
        }
    }

    if (isAnimating) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.life),
                    radius = p.size * p.life,
                    center = center + Offset(p.x, p.y)
                )
            }
        }
    }
}
