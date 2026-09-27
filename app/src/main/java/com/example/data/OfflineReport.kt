package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Offline sürede üretilen her bir ürünün miktar ve kalite dökümü
 */
data class OfflineProducedItem(
    val productId: String,
    val productName: String,
    val quality: ProductQuality,
    val quantity: Int,
    val unitPrice: Long,
    val icon: ImageVector = Icons.Rounded.Inventory2,
    val colorTint: Long = 0xFF4DD0E1
) {
    val totalPrice: Long
        get() = (quantity * unitPrice * quality.priceMultiplier).toLong()
}

/**
 * Offline sürede tüketilen hammadde bilgisi
 */
data class OfflineConsumedResource(
    val resourceId: String,
    val resourceName: String,
    val amount: Int
)

/**
 * Sabah Raporu Veri Modeli ("Sen Uyurken Ne Oldu?")
 */
data class OfflineReport(
    val logoutTime: Long,
    val loginTime: Long,
    val elapsedDurationMs: Long,
    val simulatedDurationMs: Long, // 8 saatlik Cap uygulanmış efektif süre
    val isCapReached: Boolean,
    val producedItems: List<OfflineProducedItem>,
    val consumedResources: List<OfflineConsumedResource>,
    val grossRevenue: Long, // Toplam Brüt Hasılat (₳ / ₺)
    val maintenanceCosts: Long = 0L, // Tesis Gece Bakım/Yıpranma Masrafı
    val totalExpGained: Int = 0
) {
    val netRevenue: Long
        get() = (grossRevenue - maintenanceCosts).coerceAtLeast(0L)

    val doubleNetRevenue: Long
        get() = netRevenue * 2L

    val formattedDuration: String
        get() {
            val totalMinutes = (elapsedDurationMs / (1000 * 60)).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) {
                "$hours saat $minutes dakika"
            } else {
                "$minutes dakika"
            }
        }

    val hasProduction: Boolean
        get() = producedItems.isNotEmpty() || netRevenue > 0L
}
