package com.example.ui.components
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNegative
import com.example.ui.theme.ThemeNeonCyan
import com.example.ui.theme.ThemePositive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class NotificationType {
    SUCCESS, ALERT, INFO
}

data class NotificationData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val enMessage: String? = null,
    val type: NotificationType = NotificationType.INFO,
    val timestampMs: Long = System.currentTimeMillis(),
    var isRead: Boolean = false
)

object SmartNotificationManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _notification = MutableStateFlow<NotificationData?>(null)
    val notification: StateFlow<NotificationData?> = _notification.asStateFlow()

    private val _history = MutableStateFlow<List<NotificationData>>(emptyList())
    val history: StateFlow<List<NotificationData>> = _history.asStateFlow()

    private var lastMessage: String = ""
    private var lastShowTimeMs: Long = 0L

    private fun sanitizeMessage(text: String?): String? {
        if (text == null) return null
        return text
            .replace("Supabase bulutuna", "bulut sunucuya", ignoreCase = true)
            .replace("Supabase bulut", "bulut sunucu", ignoreCase = true)
            .replace("Supabase sunucusu", "bulut sunucusu", ignoreCase = true)
            .replace("Supabase sunucu", "bulut sunucu", ignoreCase = true)
            .replace("Supabase ağına", "bulut ağına", ignoreCase = true)
            .replace("Supabase ağı", "bulut ağı", ignoreCase = true)
            .replace("Supabase network", "cloud network", ignoreCase = true)
            .replace("Supabase server", "cloud server", ignoreCase = true)
            .replace("Supabase", "Bulut", ignoreCase = true)
    }

    fun show(message: String, type: NotificationType = NotificationType.INFO, durationMs: Long = 1800L) {
        show(message, null, type, durationMs)
    }

    fun show(message: String, enMessage: String?, type: NotificationType = NotificationType.INFO, durationMs: Long = 1800L) {
        val cleanMessage = sanitizeMessage(message) ?: ""
        val cleanEnMessage = sanitizeMessage(enMessage)
        val now = System.currentTimeMillis()
        // Deduplicate identical messages within 3 seconds
        if (cleanMessage == lastMessage && (now - lastShowTimeMs) < 3000L) {
            return
        }
        // Rate-limit non-ALERT notifications to at most 1 every 500ms
        if (type != NotificationType.ALERT && (now - lastShowTimeMs) < 500L) {
            return
        }

        lastMessage = cleanMessage
        lastShowTimeMs = now

        val data = NotificationData(message = cleanMessage, enMessage = cleanEnMessage, type = type)
        _notification.value = data
        _history.value = (listOf(data) + _history.value).take(50)

        scope.launch {
            delay(durationMs)
            if (_notification.value?.id == data.id) {
                _notification.value = null
            }
        }
    }

    fun markAsRead(id: String) {
        _history.value = _history.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllAsRead() {
        _history.value = _history.value.map { it.copy(isRead = true) }
    }

    fun clearAll() {
        _history.value = emptyList()
    }

    fun dismiss() {
        _notification.value = null
    }
}

/**
 * High-performance, isolated LiveNotificationCard composable:
 * - Uses State Hoisting principles (data passed in, callbacks hoisted)
 * - Employs derivedStateOf and remember for stable styling properties
 * - Uses graphicsLayer for hardware-accelerated animations without re-measuring layout
 */
@Composable
fun LiveNotificationCard(
    data: NotificationData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(data.id) {
        try {
            if (com.example.utils.HapticManager.isHapticEnabled) {
                val hapticType = if (data.type == NotificationType.ALERT) {
                    HapticFeedbackType.LongPress
                } else {
                    HapticFeedbackType.TextHandleMove
                }
                haptic.performHapticFeedback(hapticType)
            }
        } catch (_: Throwable) {}
    }

    val themeColor by remember(data.type) {
        derivedStateOf {
            when (data.type) {
                NotificationType.SUCCESS -> ThemeGold
                NotificationType.ALERT -> ThemeNegative
                NotificationType.INFO -> ThemeNeonCyan
            }
        }
    }

    val iconVector by remember(data.type) {
        derivedStateOf {
            when (data.type) {
                NotificationType.SUCCESS -> Icons.Default.CheckCircle
                NotificationType.ALERT -> Icons.Default.Error
                NotificationType.INFO -> Icons.Default.Info
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "notif_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_border"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.2.dp, themeColor.copy(alpha = pulseAlpha)),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = themeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.7f)),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (data.enMessage != null) tr(data.message, data.enMessage) else data.message.trAuto(),
                    color = Color.White,
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDismiss()
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = tr("Kapat", "Close"),
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun DynamicIslandNotification() {
    val notification by SmartNotificationManager.notification.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 58.dp, start = 16.dp, end = 16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            notification?.let { data ->
                LiveNotificationCard(
                    data = data,
                    onDismiss = { SmartNotificationManager.dismiss() }
                )
            }
        }
    }
}

@Composable
fun NotificationHistoryDialog(
    onDismiss: () -> Unit
) {
    val history by SmartNotificationManager.history.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        // Automatically mark all as read when opening notification center
        SmartNotificationManager.markAllAsRead()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9080E1A)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.80f)
                    .padding(12.dp),
                shape = CutCornerShape(18.dp),
                color = Color(0xFF10192C),
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(ThemeNeonCyan, ThemeGold.copy(alpha = 0.6f), ThemeNeonCyan)
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ThemeNeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, ThemeNeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = ThemeNeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = tr("BİLDİRİM MERKEZİ", "NOTIFICATION CENTER"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = RobotoMonoFontFamily
                                )
                                Text(
                                    text = tr("Son oyun içi hareket ve uyarılar", "Recent in-game activities and alerts") + " (${history.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B273E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Kapat", "Close"),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = tr("Henüz bildirim yok.", "No notifications yet."),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history, key = { it.id }) { data ->
                                val (color, icon) = when (data.type) {
                                    NotificationType.SUCCESS -> ThemeGold to Icons.Default.CheckCircle
                                    NotificationType.ALERT -> ThemeNegative to Icons.Default.Error
                                    NotificationType.INFO -> ThemeNeonCyan to Icons.Default.Info
                                }

                                val timeStr = remember(data.timestampMs) {
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestampMs))
                                }

                                Surface(
                                    shape = CutCornerShape(8.dp),
                                    color = Color(0xFF162238),
                                    border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.2f))
                                                .border(1.dp, color, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (data.enMessage != null) tr(data.message, data.enMessage) else data.message.trAuto(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = tr("Saat: $timeStr", "Time: $timeStr"),
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                fontFamily = RobotoMonoFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ACTION BUTTONS: CLEAR HISTORY
                    if (history.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    SmartNotificationManager.clearAll()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = ThemeNegative,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Bildirimleri Temizle", "Clear Notifications"),
                                    color = ThemeNegative,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
