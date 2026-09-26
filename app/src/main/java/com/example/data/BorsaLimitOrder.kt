package com.example.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class LimitOrderType {
    BUY_BELOW,       // Fiyat hedef fiyata veya altına indiğinde AL
    SELL_ABOVE,      // Fiyat hedef fiyata veya üstüne çıktığında SAT
    ARBITRAGE_AUTO   // Akıllı Çift Yönlü Arbitraj (Dipte Al, Tepede Sat)
}

@Serializable
data class BorsaLimitOrder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemId: String,
    val orderType: LimitOrderType = LimitOrderType.BUY_BELOW,
    val targetPrice: Long,
    val targetSellPrice: Long = 0L, // Used in ARBITRAGE_AUTO cycle
    val quantity: Int = 10,
    val executedCount: Int = 0,
    val totalRealizedProfit: Long = 0L,
    val isActive: Boolean = true,
    val autoRepeat: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastExecutedAtMs: Long = 0L,
    val originCountry: String = "Global",
    val note: String = ""
)

object BorsaLimitOrderSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun serializeList(orders: List<BorsaLimitOrder>): String {
        return try {
            json.encodeToString(orders)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeList(raw: String): List<BorsaLimitOrder> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
