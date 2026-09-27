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
        labelTr = "Standart / Halk Tipi",
        labelEng = "Standard Grade",
        priceMultiplier = 1.0,
        prestigeReward = 0
    ),
    SELECTED(
        tier = 2,
        labelTr = "Seçme Mahsul",
        labelEng = "Selected Harvest",
        priceMultiplier = 1.3,
        prestigeReward = 0
    ),
    SPECIAL(
        tier = 3,
        labelTr = "Özel Üretim",
        labelEng = "Special Reserve",
        priceMultiplier = 1.8,
        prestigeReward = 5
    ),
    MASTERWORK(
        tier = 4,
        labelTr = "Usta İşi / İhracat",
        labelEng = "Masterwork Export",
        priceMultiplier = 2.6,
        prestigeReward = 15
    ),
    PALACE_GRAND(
        tier = 5,
        labelTr = "Coğrafi İşaretli Saray / Gurme",
        labelEng = "Imperial Palace Grand",
        priceMultiplier = 4.5,
        prestigeReward = 40
    );

    val starsText: String
        get() = "★".repeat(tier)

    val primaryColor: Color
        get() = when (this) {
            STANDARD -> Color(0xFF9E9E9E) // Bronz / Gri
            SELECTED -> Color(0xFF4CAF50) // Yeşil
            SPECIAL -> Color(0xFF00E5FF)  // Camgöbeği / Gümüş
            MASTERWORK -> Color(0xFFD500F9) // Kraliyet Moru
            PALACE_GRAND -> Color(0xFFFFD700) // Saray Altını / Amber
        }

    val gradientColors: List<Color>
        get() = when (this) {
            STANDARD -> listOf(Color(0xFF616161), Color(0xFF9E9E9E))
            SELECTED -> listOf(Color(0xFF1B5E20), Color(0xFF4CAF50))
            SPECIAL -> listOf(Color(0xFF006064), Color(0xFF00E5FF))
            MASTERWORK -> listOf(Color(0xFF4A148C), Color(0xFFD500F9))
            PALACE_GRAND -> listOf(Color(0xFFE65100), Color(0xFFFFB300), Color(0xFFFFE082))
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
