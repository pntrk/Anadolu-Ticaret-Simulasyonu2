package com.example.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 1 - 5 Yıldız Üretim Kalite Kademeleri (Zanaatkarlık Sistemi)
 */
enum class ProductQuality(
    val tier: Int,
    val labelTr: String,
    val labelEng: String,
    val priceMultiplier: Double,
    val prestigeReward: Int
) {
    STANDARD(
        tier = 1,
        labelTr = "★",
        labelEng = "★",
        priceMultiplier = 1.0,
        prestigeReward = 0
    ),
    SELECTED(
        tier = 2,
        labelTr = "★★",
        labelEng = "★★",
        priceMultiplier = 1.3,
        prestigeReward = 0
    ),
    SPECIAL(
        tier = 3,
        labelTr = "★★★",
        labelEng = "★★★",
        priceMultiplier = 1.8,
        prestigeReward = 5
    ),
    MASTERWORK(
        tier = 4,
        labelTr = "★★★★",
        labelEng = "★★★★",
        priceMultiplier = 2.6,
        prestigeReward = 15
    ),
    PALACE_GRAND(
        tier = 5,
        labelTr = "★★★★★",
        labelEng = "★★★★★",
        priceMultiplier = 4.5,
        prestigeReward = 40
    );

    val starsText: String
        get() = "★".repeat(tier)

    val label: String
        get() = starsText

    val primaryColor: Color
        get() = when (this) {
            STANDARD -> Color(0xFF9E9E9E) // 1 Yıldız: Gri
            SELECTED -> Color(0xFF8D6E63) // 2 Yıldız: Kahve
            SPECIAL -> Color(0xFFFF9800)  // 3 Yıldız: Turuncu
            MASTERWORK -> Color(0xFF7C4DFF) // 4 Yıldız: Mavi Mor
            PALACE_GRAND -> Color(0xFFFFEB3B) // 5 Yıldız: Parlak Sarı
        }

    val gradientColors: List<Color>
        get() = when (this) {
            STANDARD -> listOf(Color(0xFF616161), Color(0xFF9E9E9E))
            SELECTED -> listOf(Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFFA1887F))
            SPECIAL -> listOf(Color(0xFFE65100), Color(0xFFFF9800), Color(0xFFFFB74D))
            MASTERWORK -> listOf(Color(0xFF2979FF), Color(0xFF651FFF), Color(0xFF7C4DFF))
            PALACE_GRAND -> listOf(Color(0xFFFFA000), Color(0xFFFFD700), Color(0xFFFFEB3B))
        }

    val gradientBrush: Brush
        get() = Brush.horizontalGradient(gradientColors)

    companion object {
        fun fromTier(tier: Int): ProductQuality {
            return values().find { it.tier == tier } ?: STANDARD
        }
    }
}

/**
 * Envanter ve Pazar İçin Kaliteli Ticaret Öğesi
 */
data class TradeItemWithQuality(
    val id: String,
    val name: String,
    val basePrice: Long,
    val quality: ProductQuality = ProductQuality.STANDARD,
    val quantity: Int = 1
) {
    val unitSellingPrice: Long
        get() = (basePrice * quality.priceMultiplier).toLong()

    val totalSellingPrice: Long
        get() = unitSellingPrice * quantity
}
