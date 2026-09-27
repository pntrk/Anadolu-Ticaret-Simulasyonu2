package com.example.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * 1 - 5 Yıldız Ürün Kalite Sistemi (Zanaatkarlık & Kalite Kademeleri)
 */
enum class ItemQuality(
    val stars: Int,
    val priceMultiplier: Double,
    val label: String,
    val badgeColor: Color
) {
    STAR_1(
        stars = 1,
        priceMultiplier = 1.0,
        label = "★",
        badgeColor = Color(0xFF9E9E9E) // 1 Yıldız: Gri
    ),
    STAR_2(
        stars = 2,
        priceMultiplier = 1.35,
        label = "★★",
        badgeColor = Color(0xFF8D6E63) // 2 Yıldız: Kahve
    ),
    STAR_3(
        stars = 3,
        priceMultiplier = 1.9,
        label = "★★★",
        badgeColor = Color(0xFFFF9800) // 3 Yıldız: Turuncu
    ),
    STAR_4(
        stars = 4,
        priceMultiplier = 2.8,
        label = "★★★★",
        badgeColor = Color(0xFF7C4DFF) // 4 Yıldız: Mavi Mor
    ),
    STAR_5(
        stars = 5,
        priceMultiplier = 4.5,
        label = "★★★★★",
        badgeColor = Color(0xFFFFEB3B) // 5 Yıldız: Parlak Sarı
    );

    val starsText: String
        get() = "★".repeat(stars)

    val gradientColors: List<Color>
        get() = when (this) {
            STAR_1 -> listOf(Color(0xFF616161), Color(0xFF9E9E9E))
            STAR_2 -> listOf(Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFFA1887F))
            STAR_3 -> listOf(Color(0xFFE65100), Color(0xFFFF9800), Color(0xFFFFB74D))
            STAR_4 -> listOf(Color(0xFF2979FF), Color(0xFF651FFF), Color(0xFF7C4DFF))
            STAR_5 -> listOf(Color(0xFFFFA000), Color(0xFFFFD700), Color(0xFFFFEB3B))
        }

    val gradientBrush: Brush
        get() = Brush.horizontalGradient(gradientColors)

    fun toProductQuality(): ProductQuality = ProductQuality.fromTier(stars)

    companion object {
        val star1 = STAR_1
        val star2 = STAR_2
        val star3 = STAR_3
        val star4 = STAR_4
        val star5 = STAR_5

        fun fromStars(stars: Int): ItemQuality {
            return values().find { it.stars == stars.coerceIn(1, 5) } ?: STAR_1
        }

        fun fromTier(tier: Int): ItemQuality = fromStars(tier)

        fun fromProductQuality(quality: ProductQuality): ItemQuality = fromStars(quality.tier)

        /**
         * Tesis Seviyesine Göre Ürün Kalitesi Belirleme:
         * 1-2 seviye tesis -> 1 yıldızlı (STAR_1)
         * 3-4 seviye tesis -> 2 yıldızlı (STAR_2)
         * 5-6 seviye tesis -> 3 yıldızlı (STAR_3)
         * 7-8 seviye tesis -> 4 yıldızlı (STAR_4)
         * 9-10 seviye tesis -> 5 yıldızlı (STAR_5)
         */
        fun fromFacilityLevel(level: Int): ItemQuality {
            return when {
                level >= 9 -> STAR_5
                level >= 7 -> STAR_4
                level >= 5 -> STAR_3
                level >= 3 -> STAR_2
                else -> STAR_1
            }
        }

        /**
         * Envanterde aynı ürünün farklı kalitedeki versiyonları için benzersiz anahtar üretir.
         * Format: 'productId_quality' (örn: 'iron_star1', 'steel_star5')
         */
        fun makeKey(productId: String, quality: ItemQuality = STAR_1): String {
            val baseId = extractBaseProductId(productId)
            return "${baseId}_${quality.name.lowercase()}"
        }

        /**
         * Envanter benzersiz anahtarından ham ürün kimliğini ayıklar.
         * 'steel_star5' -> 'steel', 'iron' -> 'iron'
         */
        fun extractBaseProductId(key: String): String {
            return when {
                key.contains("_star") -> key.substringBeforeLast("_star")
                key.contains("_star_") -> key.substringBeforeLast("_star_")
                else -> key
            }
        }

        /**
         * Envanter benzersiz anahtarından kalite seviyesini ayıklar.
         */
        fun extractQuality(key: String): ItemQuality {
            return when {
                key.endsWith("_star5") || key.endsWith("_5") -> STAR_5
                key.endsWith("_star4") || key.endsWith("_4") -> STAR_4
                key.endsWith("_star3") || key.endsWith("_3") -> STAR_3
                key.endsWith("_star2") || key.endsWith("_2") -> STAR_2
                key.endsWith("_star1") || key.endsWith("_1") -> STAR_1
                else -> STAR_1
            }
        }

        /**
         * String'den güvenli parse eder.
         */
        fun fromKey(key: String): ItemQuality = extractQuality(key)
    }
}

/**
 * Kalite nitelikli Emtia/Ürün Modeli
 */
data class ItemWithQuality(
    val productId: String,
    val basePrice: Long,
    val quality: ItemQuality = ItemQuality.STAR_1,
    val quantity: Int = 1
) {
    val uniqueInventoryKey: String
        get() = ItemQuality.makeKey(productId, quality)

    val calculatedPrice: Long
        get() = (basePrice * quality.priceMultiplier).toLong()

    val totalCalculatedPrice: Long
        get() = calculatedPrice * quantity
}
