package com.example.ui.components
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.tr
import com.example.ui.theme.trAuto
import com.example.ui.theme.isEnglishLanguage
import com.example.data.Product
import com.example.data.ItemQuality

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
import androidx.compose.ui.graphics.vector.ImageVector
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
    var isRead: Boolean = false,
    val productId: String? = null,
    val quality: ItemQuality? = null
) {
    fun getResolvedProductId(): String? {
        if (!productId.isNullOrBlank()) {
            return ItemQuality.extractBaseProductId(productId).lowercase().trim()
        }
        val (prod, _) = NotificationTextFormatter.extractProductAndQuality(message)
        if (prod != null) return prod.id
        if (enMessage != null) {
            val (enProd, _) = NotificationTextFormatter.extractProductAndQuality(enMessage)
            if (enProd != null) return enProd.id
        }
        return null
    }

    fun getResolvedQuality(): ItemQuality? {
        if (quality != null) return quality
        if (!productId.isNullOrBlank()) {
            val q = ItemQuality.extractQuality(productId)
            if (q != ItemQuality.STAR_1 || productId.contains("_star", ignoreCase = true) || productId.contains("_1", ignoreCase = true)) {
                return q
            }
        }
        val (_, qTr) = NotificationTextFormatter.extractProductAndQuality(message)
        if (qTr != null) return qTr
        if (enMessage != null) {
            val (_, qEn) = NotificationTextFormatter.extractProductAndQuality(enMessage)
            if (qEn != null) return qEn
        }
        return null
    }

    fun getFormattedMessage(isEnglish: Boolean): String {
        val raw = if (isEnglish) (enMessage ?: message) else message
        return NotificationTextFormatter.formatProductReferences(raw, isEnglish)
    }
}

object NotificationTextFormatter {
    private val turkishShortNames = mapOf(
        "iron" to "Demir",
        "copper" to "Bakır",
        "silicon" to "Silisyum",
        "aluminum" to "Alüminyum",
        "bauxite" to "Boksit",
        "coal" to "Kömür",
        "limestone" to "Kireçtaşı",
        "lithium" to "Lityum",
        "timber" to "Kereste",
        "chemicals" to "Kimyasal",
        "crude_oil" to "Ham Petrol",
        "natural_gas" to "Doğalgaz",
        "titanium" to "Titanyum",
        "graphite_ore" to "Grafit",
        "rubber_latex" to "Kauçuk",
        "cotton" to "Pamuk",
        "steel" to "Çelik",
        "wire" to "Kablo",
        "glass" to "Cam",
        "fabric" to "Kumaş",
        "battery" to "Akü",
        "plastic" to "Plastik",
        "wood_plank" to "Ahşap",
        "cement" to "Çimento",
        "refined_fuel" to "Akaryakıt",
        "tire" to "Lastik",
        "packaging" to "Ambalaj",
        "aluminum_ingot" to "Külçe Alüminyum",
        "liquefied_gas" to "Sıvı Gaz (LNG)",
        "titanium_ingot" to "Külçe Titanyum",
        "pcb_substrate" to "Devre Kartı (PCB)",
        "synthetic_textile" to "Sentetik Elyaf",
        "titanium_alloy" to "Titanyum Alaşım",
        "carbon_fiber" to "Karbon Elyaf",
        "semiconductor_wafer" to "Yarı İletken Plaka",
        "optical_fiber" to "Fiber Optik",
        "graphene_sheet" to "Grafen Tabaka",
        "chip" to "Mikroçip",
        "smartphone" to "Akıllı Telefon",
        "clothing" to "Hazır Giyim",
        "auto_part" to "Oto Parçası",
        "appliance" to "Beyaz Eşya",
        "furniture" to "Mobilya",
        "building_block" to "İnşaat Malzemesi",
        "pharma" to "İlaç & Biyo",
        "machinery" to "İş Makinesi",
        "solar_panel" to "Güneş Paneli",
        "petrochem" to "Petrokimya",
        "biotech_med" to "Medikal Cihaz",
        "turbine_engine" to "Türbin Motoru",
        "industrial_container" to "Konteyner",
        "composite_structure" to "Kompozit Panel",
        "robotics_arm" to "Robot Kolu",
        "telecom_station" to "Baz İstasyonu",
        "nano_battery" to "Nano Batarya",
        "ev" to "Elektrikli Araç",
        "uav" to "İHA / SİHA",
        "satellite" to "Uydu",
        "cargo_ship" to "Kargo Gemisi",
        "bullet_train" to "Hızlı Tren",
        "ai_datacenter" to "AI Veri Merkezi",
        "defense_frigate" to "Fırkateyn",
        "hydrogen_plant" to "Hidrojen Santrali",
        "smart_grid" to "Akıllı Şebeke",
        "super_yacht" to "Süper Yat",
        "space_rocket" to "Uzay Roketi",
        "smart_skyscraper" to "Akıllı Gökdelen",
        "fusion_reactor_core" to "Füzyon Reaktörü",
        "quantum_supercomputer" to "Kuantum Bilgisayar",
        "autonomous_drone_swarm" to "İHA Sürüsü",
        "hyperloop_capsule" to "Hyperloop Kapsülü",
        "luxury_aircraft_interior" to "VIP Uçak Kabini"
    )

    private val englishShortNames = mapOf(
        "iron" to "Iron",
        "copper" to "Copper",
        "silicon" to "Silicon",
        "aluminum" to "Aluminum",
        "bauxite" to "Bauxite",
        "coal" to "Coal",
        "limestone" to "Limestone",
        "lithium" to "Lithium",
        "timber" to "Timber",
        "chemicals" to "Chemicals",
        "crude_oil" to "Crude Oil",
        "natural_gas" to "Natural Gas",
        "titanium" to "Titanium",
        "graphite_ore" to "Graphite",
        "rubber_latex" to "Rubber",
        "cotton" to "Cotton",
        "steel" to "Steel",
        "wire" to "Wire",
        "glass" to "Glass",
        "fabric" to "Fabric",
        "battery" to "Battery",
        "plastic" to "Plastic",
        "wood_plank" to "Wood Plank",
        "cement" to "Cement",
        "refined_fuel" to "Refined Fuel",
        "tire" to "Tire",
        "packaging" to "Packaging",
        "aluminum_ingot" to "Aluminum Ingot",
        "liquefied_gas" to "Liquefied Gas (LNG)",
        "titanium_ingot" to "Titanium Ingot",
        "pcb_substrate" to "PCB Substrate",
        "synthetic_textile" to "Synthetic Textile",
        "titanium_alloy" to "Titanium Alloy",
        "carbon_fiber" to "Carbon Fiber",
        "semiconductor_wafer" to "Semiconductor Wafer",
        "optical_fiber" to "Optical Fiber",
        "graphene_sheet" to "Graphene Sheet",
        "chip" to "Microchip",
        "smartphone" to "Smartphone",
        "clothing" to "Clothing",
        "auto_part" to "Auto Part",
        "appliance" to "Appliance",
        "furniture" to "Furniture",
        "building_block" to "Building Material",
        "pharma" to "Pharmaceuticals",
        "machinery" to "Heavy Machinery",
        "solar_panel" to "Solar Panel",
        "petrochem" to "Petrochemical",
        "biotech_med" to "Biotech Medical",
        "turbine_engine" to "Turbine Engine",
        "industrial_container" to "Container",
        "composite_structure" to "Composite Panel",
        "robotics_arm" to "Robotic Arm",
        "telecom_station" to "Telecom Station",
        "nano_battery" to "Nano Battery",
        "ev" to "Electric Vehicle",
        "uav" to "UAV / Drone",
        "satellite" to "Satellite",
        "cargo_ship" to "Cargo Ship",
        "bullet_train" to "Bullet Train",
        "ai_datacenter" to "AI Datacenter",
        "defense_frigate" to "Frigate",
        "hydrogen_plant" to "Hydrogen Plant",
        "smart_grid" to "Smart Grid",
        "super_yacht" to "Super Yacht",
        "space_rocket" to "Space Rocket",
        "smart_skyscraper" to "Smart Skyscraper",
        "fusion_reactor_core" to "Fusion Reactor",
        "quantum_supercomputer" to "Quantum Computer",
        "autonomous_drone_swarm" to "Drone Swarm",
        "hyperloop_capsule" to "Hyperloop Capsule",
        "luxury_aircraft_interior" to "VIP Aircraft Interior"
    )

    fun getProductName(productId: String, isEnglish: Boolean): String {
        val cleanId = ItemQuality.extractBaseProductId(productId).lowercase().trim()
        return if (isEnglish) {
            englishShortNames[cleanId] ?: Product.values().find { it.id.equals(cleanId, ignoreCase = true) }?.getDisplayName(true) ?: cleanId.replace("_", " ").replaceFirstChar { it.uppercase() }
        } else {
            turkishShortNames[cleanId] ?: Product.values().find { it.id.equals(cleanId, ignoreCase = true) }?.getDisplayName(false) ?: cleanId.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    fun formatProductReferences(text: String, isEnglish: Boolean): String {
        var result = text
        val qualityWord = if (isEnglish) "Quality" else "Kalite"
        val allKeys = (turkishShortNames.keys + Product.values().map { it.id }).distinct().sortedByDescending { it.length }

        for (key in allKeys) {
            val name = getProductName(key, isEnglish)

            // Matches: [key]_star_?([1-5]) or [key]-star-?([1-5]) (e.g. iron_star_4, iron_star4, iron-star-4)
            val regexStar = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(key)}[_-]star[_-]?([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            result = regexStar.replace(result) { m ->
                val q = m.groupValues[1]
                "$name $qualityWord $q"
            }

            // Matches: [key]_quality_?([1-5]) or [key]-quality-?([1-5])
            val regexQuality = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(key)}[_-]quality[_-]?([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            result = regexQuality.replace(result) { m ->
                val q = m.groupValues[1]
                "$name $qualityWord $q"
            }

            // Matches: [key]_([1-5]) (e.g. iron_4, steel_5, bauxite_3)
            val regexNum = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(key)}_([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            result = regexNum.replace(result) { m ->
                val q = m.groupValues[1]
                "$name $qualityWord $q"
            }

            // Matches: [key]_star or [key]-star (e.g. iron_star)
            val regexBareStar = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(key)}[_-]star(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            result = regexBareStar.replace(result, "$name $qualityWord 1")

            // Multi-word raw keys like crude_oil, synthetic_textile, pcb_substrate without quality suffix
            if (key.contains("_")) {
                val regexMultiWord = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(key)}(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
                result = regexMultiWord.replace(result, name)
            }
        }
        return result
    }

    fun extractProductAndQuality(text: String): Pair<Product?, ItemQuality?> {
        val allProducts = Product.values().sortedByDescending { it.id.length }

        // 1. Strict pattern check for key_star_X or key_quality_X or key_X
        for (prod in allProducts) {
            val regexStar = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(prod.id)}[_-]star[_-]?([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            val matchStar = regexStar.find(text)
            if (matchStar != null) {
                val stars = matchStar.groupValues[1].toIntOrNull() ?: 1
                return Pair(prod, ItemQuality.fromStars(stars))
            }

            val regexQuality = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(prod.id)}[_-]quality[_-]?([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            val matchQuality = regexQuality.find(text)
            if (matchQuality != null) {
                val stars = matchQuality.groupValues[1].toIntOrNull() ?: 1
                return Pair(prod, ItemQuality.fromStars(stars))
            }

            val regexNum = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)${Regex.escape(prod.id)}_([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            val matchNum = regexNum.find(text)
            if (matchNum != null) {
                val stars = matchNum.groupValues[1].toIntOrNull() ?: 1
                return Pair(prod, ItemQuality.fromStars(stars))
            }
        }

        // 2. Check for exact localized product names with quality or production/trade context
        // Skip short IDs like "ev" or "uav" unless full localized names are used
        for (prod in allProducts) {
            val trName = getProductName(prod.id, false)
            val enName = getProductName(prod.id, true)

            // Pattern: [ProductName] Kalite [1-5] or [ProductName] Quality [1-5] or [ProductName] ★[1-5]
            val regexNamedQuality = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)(?:${Regex.escape(trName)}|${Regex.escape(enName)})\\s*(?:Kalite|Quality|★)\\s*([1-5])(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
            val namedQualityMatch = regexNamedQuality.find(text)
            if (namedQualityMatch != null) {
                val stars = namedQualityMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(prod, ItemQuality.fromStars(stars))
            }

            // Pattern: Explicit product trade with quantity e.g. "500 Ton Demir", "100 Adet Çelik", "50 Tons Iron"
            if (trName.length >= 3 || enName.length >= 3) {
                val regexTrade = Regex("(?i)(?<=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|^)(?:\\d+\\s*(?:Ton|Adet|Units?|Tons?)\\s*(?:★[1-5]\\s*)?(?:${Regex.escape(trName)}|${Regex.escape(enName)}))(?=[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ]|$)")
                if (regexTrade.containsMatchIn(text)) {
                    val starsMatch = Regex("(?i)★\\s*([1-5])|(?:Kalite|Quality)\\s*([1-5])").find(text)
                    val stars = starsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: starsMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
                    return Pair(prod, stars?.let { ItemQuality.fromStars(it) })
                }
            }
        }

        return Pair(null, null)
    }
}

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

    fun show(
        message: String,
        type: NotificationType = NotificationType.INFO,
        durationMs: Long = 1800L
    ) {
        show(message, null, type, durationMs)
    }

    fun show(
        message: String,
        enMessage: String? = null,
        type: NotificationType = NotificationType.INFO,
        durationMs: Long = 1800L,
        productId: String? = null,
        quality: ItemQuality? = null
    ) {
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

        val (detectedProduct, detectedQuality) = NotificationTextFormatter.extractProductAndQuality(cleanMessage)
        val resolvedProductId = productId ?: detectedProduct?.id
        val resolvedQuality = quality ?: detectedQuality

        val formattedTr = NotificationTextFormatter.formatProductReferences(cleanMessage, isEnglish = false)
        val formattedEn = NotificationTextFormatter.formatProductReferences(cleanEnMessage ?: cleanMessage, isEnglish = true)

        val data = NotificationData(
            message = formattedTr,
            enMessage = formattedEn,
            type = type,
            productId = resolvedProductId,
            quality = resolvedQuality
        )
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
        val isEnglish = isEnglishLanguage()
        val displayText = data.getFormattedMessage(isEnglish)
        val resolvedProductId = data.getResolvedProductId()
        val resolvedQuality = data.getResolvedQuality()

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (resolvedProductId != null) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.7f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            UniversalProductIcon(
                                productId = resolvedProductId,
                                size = 22.dp
                            )
                            if (resolvedQuality != null) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 3.dp),
                                    color = resolvedQuality.badgeColor.copy(alpha = 0.95f),
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    Text(
                                        text = "★${resolvedQuality.stars}",
                                        color = Color.Black,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = displayText,
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

                                val isEnglish = isEnglishLanguage()
                                val displayText = data.getFormattedMessage(isEnglish)
                                val resolvedProdId = data.getResolvedProductId()
                                val resolvedQuality = data.getResolvedQuality()
                                val prodDisplayName = resolvedProdId?.let { NotificationTextFormatter.getProductName(it, isEnglish) }

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
                                        if (resolvedProdId != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0F1829),
                                                border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    UniversalProductIcon(
                                                        productId = resolvedProdId,
                                                        size = 26.dp
                                                    )
                                                    if (resolvedQuality != null) {
                                                        Surface(
                                                            shape = RoundedCornerShape(topStart = 4.dp),
                                                            color = resolvedQuality.badgeColor.copy(alpha = 0.95f),
                                                            modifier = Modifier.align(Alignment.BottomEnd)
                                                        ) {
                                                            Text(
                                                                text = "★${resolvedQuality.stars}",
                                                                color = Color.Black,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black,
                                                                modifier = Modifier.padding(horizontal = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(color.copy(alpha = 0.2f))
                                                    .border(1.dp, color, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayText,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tr("Saat: $timeStr", "Time: $timeStr"),
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    fontFamily = RobotoMonoFontFamily
                                                )
                                                if (prodDisplayName != null) {
                                                    val qSuffix = resolvedQuality?.let {
                                                        if (isEnglish) " • Quality ${it.stars}" else " • Kalite ${it.stars}"
                                                    } ?: ""
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = color.copy(alpha = 0.15f),
                                                        border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
                                                    ) {
                                                        Text(
                                                            text = "$prodDisplayName$qSuffix",
                                                            color = color,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = RobotoMonoFontFamily,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
