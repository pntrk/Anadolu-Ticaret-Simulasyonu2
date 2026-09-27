package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.lightColorScheme

enum class AppThemeOption(

    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceVariantColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val borderColor: Color,
    val navigationBarColor: Color,
    val isDark: Boolean = true
) {
    TICARIUM_CORPORATE(
        id = "ticarium_corporate",
        title = "Ticarium Kurumsal (Önerilen)",
        subtitle = "Sert ve endüstriyel koyu lacivert/yeşil simülasyon teması",
        emoji = "📈",
        primaryColor = Color(0xFF00E676), // Vivid Neon Green for positive/accents
        secondaryColor = Color(0xFF00B0FF), // Sharp Blue
        backgroundColor = Color(0xFF0B101E), // Deep corporate navy dark
        surfaceColor = Color(0xFF141C2F),
        surfaceVariantColor = Color(0xFF1D273D),
        textPrimaryColor = Color(0xFFF1F5F9),
        textSecondaryColor = Color(0xFF94A3B8),
        borderColor = Color(0xFF273654),
        navigationBarColor = Color(0xFF070B15),
        isDark = true
    ),
    CYBER_BLUE(
        id = "cyber_blue",
        title = "Siber Mavi (Varsayılan)",
        subtitle = "Derin uzay mavisi ve elektrik siyanı",
        emoji = "🌌",
        primaryColor = Color(0xFFFFD700), // ThemeGold
        secondaryColor = Color(0xFF00E5FF), // ThemeNeonCyan
        backgroundColor = Color(0xFF070B14),
        surfaceColor = Color(0xFF0F172A),
        surfaceVariantColor = Color(0xFF1E293B),
        textPrimaryColor = Color(0xFFECF1F8),
        textSecondaryColor = Color(0xFF94A3B8),
        borderColor = Color(0xFF2E384D),
        navigationBarColor = Color(0xFF070B14),
        isDark = true
    ),
    AMOLED_MIDNIGHT(
        id = "amoled_midnight",
        title = "AMOLED Gece Yarısı",
        subtitle = "Tam siyah, batarya dostu derin gece teması",
        emoji = "🌑",
        primaryColor = Color(0xFF38BDF8), // Electric Sky Blue
        secondaryColor = Color(0xFF818CF8), // Indigo
        backgroundColor = Color(0xFF000000), // Pure OLED Black
        surfaceColor = Color(0xFF0C101A),
        surfaceVariantColor = Color(0xFF181F30),
        textPrimaryColor = Color(0xFFF8FAFC),
        textSecondaryColor = Color(0xFF94A3B8),
        borderColor = Color(0xFF1E293B),
        navigationBarColor = Color(0xFF000000),
        isDark = true
    ),
    GOLDEN_LUXURY(
        id = "golden_luxury",
        title = "Altın ve Lüks",
        subtitle = "Şirket holdingine yakışır altın ve bronzlar",
        emoji = "👑",
        primaryColor = Color(0xFFFFD700), // Pure Imperial Gold
        secondaryColor = Color(0xFFFFB300), // Warm Amber
        backgroundColor = Color(0xFF0B0804), // Deep Dark Chocolate
        surfaceColor = Color(0xFF171108),
        surfaceVariantColor = Color(0xFF261D0F),
        textPrimaryColor = Color(0xFFFFF8E7),
        textSecondaryColor = Color(0xFFD4AF37), // Luminous Bronze Text
        borderColor = Color(0xFF4A3818),
        navigationBarColor = Color(0xFF0B0804),
        isDark = true
    ),
    NEON_CYBERPUNK(
        id = "neon_cyberpunk",
        title = "Neon Siberpunk",
        subtitle = "Yüksek kontrastlı canlı yeşil ve mor vurgulu retro-siber tema",
        emoji = "🟢",
        primaryColor = Color(0xFF00FF66), // Vibrant Neon Green
        secondaryColor = Color(0xFFE040FB), // Neon Magenta
        backgroundColor = Color(0xFF070210), // Dark Synth Indigo Void
        surfaceColor = Color(0xFF150A2E),
        surfaceVariantColor = Color(0xFF24114B),
        textPrimaryColor = Color(0xFFF3E8FF),
        textSecondaryColor = Color(0xFFC09FE6),
        borderColor = Color(0xFF4A1A80),
        navigationBarColor = Color(0xFF070210),
        isDark = true
    ),
    LIGHT_CRYSTAL_WHITE(
        id = "light_crystal_white",
        title = "Aydınlık Kristal",
        subtitle = "Ferah, yüksek kontrastlı ve modern dinamik aydınlık tema",
        emoji = "☀️",
        primaryColor = Color(0xFF0284C7), // Bright Sky Sapphire
        secondaryColor = Color(0xFF0D9488), // Vibrant Teal Green
        backgroundColor = Color(0xFFF1F5F9), // Slate Light Background
        surfaceColor = Color(0xFFFFFFFF), // Pure White Card Surface
        surfaceVariantColor = Color(0xFFE2E8F0), // Elevated Light Slate
        textPrimaryColor = Color(0xFF0F172A), // Dark Slate Text
        textSecondaryColor = Color(0xFF334155), // Clear Medium Slate
        borderColor = Color(0xFFCBD5E1), // Crisp Light Border
        navigationBarColor = Color(0xFFF8FAFC),
        isDark = false
    ),
    LIGHT_GOLDEN_SUN(
        id = "light_golden_sun",
        title = "Aydınlık Altın Güneş",
        subtitle = "Sıcak altın, kehribar ve canlı aydınlık ticaret teması",
        emoji = "🌅",
        primaryColor = Color(0xFFD97706), // Rich Golden Amber
        secondaryColor = Color(0xFF2563EB), // Vibrant Azure
        backgroundColor = Color(0xFFFAF8F5), // Warm Ivory Cream
        surfaceColor = Color(0xFFFFFFFF), // Pure White Surface
        surfaceVariantColor = Color(0xFFF3EDE2), // Soft Warm Variant
        textPrimaryColor = Color(0xFF1C1917), // Dark Stone Text
        textSecondaryColor = Color(0xFF57534E), // Medium Stone Text
        borderColor = Color(0xFFE7E5E4), // Soft Border
        navigationBarColor = Color(0xFFFAF8F5),
        isDark = false
    ),
    LIGHT_EMERALD_MINT(
        id = "light_emerald_mint",
        title = "Aydınlık Zümrüt & Nane",
        subtitle = "Canlı finansal büyüme ve taze zümrüt aydınlık tema",
        emoji = "🌿",
        primaryColor = Color(0xFF059669), // Vibrant Emerald Mint
        secondaryColor = Color(0xFF0284C7), // Ocean Sapphire
        backgroundColor = Color(0xFFF0FDF4), // Refreshing Mint Light Canvas
        surfaceColor = Color(0xFFFFFFFF), // Pure White Surface
        surfaceVariantColor = Color(0xFFDCFCE7), // Soft Green Variant
        textPrimaryColor = Color(0xFF064E3B), // Dark Forest Text
        textSecondaryColor = Color(0xFF047857), // Forest Green Muted Text
        borderColor = Color(0xFFA7F3D0), // Emerald Accent Border
        navigationBarColor = Color(0xFFF0FDF4),
        isDark = false
    );

    companion object {
        fun fromId(id: String): AppThemeOption = values().firstOrNull { it.id == id } ?: CYBER_BLUE
    }
}

val LocalAppThemeOption = staticCompositionLocalOf { AppThemeOption.TICARIUM_CORPORATE }

@Composable
fun MyApplicationTheme(
    selectedThemeId: String = "ticarium_corporate",
    selectedLanguageCode: String = "tr",
    content: @Composable () -> Unit,
) {
    val themeOption = AppThemeOption.fromId(selectedThemeId)
    val appLanguage = AppLanguage.fromCode(selectedLanguageCode)

    val animatedPrimary = animateColorAsState(themeOption.primaryColor, animationSpec = tween(400), label = "primary")
    val animatedSecondary = animateColorAsState(themeOption.secondaryColor, animationSpec = tween(400), label = "secondary")
    val animatedBackground = animateColorAsState(themeOption.backgroundColor, animationSpec = tween(400), label = "bg")
    val animatedSurface = animateColorAsState(themeOption.surfaceColor, animationSpec = tween(400), label = "surface")
    val animatedSurfaceVariant = animateColorAsState(themeOption.surfaceVariantColor, animationSpec = tween(400), label = "surfaceVar")
    val animatedTextPrimary = animateColorAsState(themeOption.textPrimaryColor, animationSpec = tween(400), label = "textPrim")
    val animatedTextSecondary = animateColorAsState(themeOption.textSecondaryColor, animationSpec = tween(400), label = "textSec")
    val animatedBorder = animateColorAsState(themeOption.borderColor, animationSpec = tween(400), label = "border")
    val animatedNav = animateColorAsState(themeOption.navigationBarColor, animationSpec = tween(400), label = "nav")

    val colorScheme = if (themeOption.isDark) {
        darkColorScheme(
            primary = animatedPrimary.value,
            onPrimary = Color.Black,
            secondary = animatedSecondary.value,
            onSecondary = Color.Black,
            background = animatedBackground.value,
            surface = animatedSurface.value,
            surfaceVariant = animatedSurfaceVariant.value,
            onBackground = animatedTextPrimary.value,
            onSurface = animatedTextPrimary.value,
            onSurfaceVariant = animatedTextSecondary.value,
            outline = animatedBorder.value,
            outlineVariant = animatedNav.value,
            tertiary = animatedSecondary.value,
            error = ThemeNegative
        )
    } else {
        lightColorScheme(
            primary = animatedPrimary.value,
            onPrimary = Color.White,
            secondary = animatedSecondary.value,
            onSecondary = Color.White,
            background = animatedBackground.value,
            surface = animatedSurface.value,
            surfaceVariant = animatedSurfaceVariant.value,
            onBackground = animatedTextPrimary.value,
            onSurface = animatedTextPrimary.value,
            onSurfaceVariant = animatedTextSecondary.value,
            outline = animatedBorder.value,
            outlineVariant = animatedNav.value,
            tertiary = animatedSecondary.value,
            error = ThemeNegative
        )
    }

    globalCurrentLanguage = appLanguage
    val currentHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val safeHaptic = androidx.compose.runtime.remember(currentHaptic) {
        object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            private var lastHapticTime = 0L
            private var isSupported = true
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                if (!isSupported) return
                val now = System.currentTimeMillis()
                if (now - lastHapticTime < 200L) return
                lastHapticTime = now
                try {
                    currentHaptic.performHapticFeedback(hapticFeedbackType)
                } catch (e: Throwable) {
                    isSupported = false
                    android.util.Log.w("MyApplicationTheme", "Swallowed haptic binder transaction failure safely", e)
                }
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val isPc = androidx.compose.runtime.remember(context) { 
        context.packageManager.hasSystemFeature("android.hardware.type.pc") ||
        context.packageManager.hasSystemFeature("org.chromium.arc.device_management") ||
        context.resources.configuration.screenWidthDp >= 720
    }
    
    // Scale up UI by 15% on PC to compensate for the further viewing distance
    // making text and touch targets more readable and proportionate for desktop monitors.
    val pcOptimizedDensity = androidx.compose.runtime.remember(currentDensity, isPc) {
        if (isPc) {
            androidx.compose.ui.unit.Density(
                density = currentDensity.density * 1.15f,
                fontScale = currentDensity.fontScale
            )
        } else {
            currentDensity
        }
    }

    CompositionLocalProvider(
        LocalAppThemeOption provides themeOption,
        LocalAppLanguage provides appLanguage,
        androidx.compose.ui.platform.LocalHapticFeedback provides safeHaptic,
        androidx.compose.ui.platform.LocalDensity provides pcOptimizedDensity
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}

