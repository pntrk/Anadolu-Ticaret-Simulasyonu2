package com.example.data.remote.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PostgreSQL Event Types for Realtime Logical Replication.
 */
enum class PostgresEventType {
    INSERT,
    UPDATE,
    DELETE,
    ALL
}

/**
 * Wrapper for Postgres Logical Replication / Realtime WebSockets CDC (Change Data Capture) events.
 */
data class RealtimeChangeEvent<T>(
    val eventType: PostgresEventType,
    val table: String,
    val record: T?,
    val oldRecord: T? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * RDBMS Table: `market_prices`
 * Represents global Borsa prices streaming in real-time (~6ms updates).
 */
@Serializable
data class MarketPriceDto(
    @SerialName("id") val id: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("current_price") val currentPrice: Double,
    @SerialName("previous_price") val previousPrice: Double = currentPrice,
    @SerialName("change_percent") val changePercent: Double = 0.0,
    @SerialName("volume_24h") val volume24h: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * RDBMS Table: `player_orders` / `market_listings`
 * Represents active orders in the global order book.
 */
@Serializable
data class PlayerOrderDto(
    @SerialName("id") val id: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("seller_name") val sellerName: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unit_price") val unitPrice: Long,
    @SerialName("total_price") val totalPrice: Long,
    @SerialName("status") val status: String = "ACTIVE", // ACTIVE, FULFILLED, CANCELLED
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * RDBMS Table: `futures_positions`
 * Represents high-frequency leveraged futures/options positions.
 */
@Serializable
data class FuturesPositionDto(
    @SerialName("id") val id: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("leverage") val leverage: Int,
    @SerialName("entry_price") val entryPrice: Double,
    @SerialName("position_type") val positionType: String, // LONG, SHORT
    @SerialName("margin_amount") val marginAmount: Long,
    @SerialName("liquidation_price") val liquidationPrice: Double,
    @SerialName("is_open") val isOpen: Boolean = true,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * RDBMS Table: `global_macro_state`
 * Represents global economic parameters (inflation, loan/deposit rates).
 */
@Serializable
data class GlobalMacroDto(
    @SerialName("id") val id: String = "global_macro",
    @SerialName("economic_cycle") val economicCycle: String = "RECOVERY",
    @SerialName("inflation_rate") val inflationRate: Double = 0.08,
    @SerialName("interest_rate") val interestRate: Double = 0.12,
    @SerialName("loan_rate") val loanRate: Double = 0.22,
    @SerialName("deposit_rate") val depositRate: Double = 0.10,
    @SerialName("liquidity_multiplier") val liquidityMultiplier: Double = 1.0,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * RDBMS Table: `lobby_players`
 * Represents active player nodes in a multiplayer lobby or guild.
 */
@Serializable
data class LobbyPlayerDto(
    @SerialName("id") val id: String,
    @SerialName("player_name") val playerName: String,
    @SerialName("company_name") val companyName: String,
    @SerialName("net_worth") val netWorth: Long,
    @SerialName("level") val level: Int,
    @SerialName("is_online") val isOnline: Boolean = true,
    @SerialName("last_seen_at") val lastSeenAt: Long = System.currentTimeMillis()
)
