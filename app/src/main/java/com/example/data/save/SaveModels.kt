package com.example.data.save

/**
 * Encapsulates the core currency and wealth snapshot for seamless migration.
 */
data class PlayerCurrencySnapshot(
    val money: Long = 0L,
    val depositBalance: Long = 0L,
    val loanAmount: Long = 0L,
    val lockedDepositBalance: Long = 0L,
    val lockedDepositStartTimeMs: Long = 0L,
    val lockedDepositDurationMs: Long = 0L,
    val gems: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val totalProfit: Long = 0L,
    val dailyIncome: Long = 0L,
    val dailyExpense: Long = 0L,
    val dollarBalance: Long = 0L,
    val dollarDepositBalance: Long = 0L,
    val dollarLoanAmount: Long = 0L
)

/**
 * Unified Game Save Document model representing player state in NoSQL (JSONB) and local formats.
 */
data class GameSaveData(
    val playerId: String,
    val playerName: String = "Tüccar",
    val companyName: String = "Tüccar Holding",
    val currencies: PlayerCurrencySnapshot = PlayerCurrencySnapshot(),
    val currentCity: String = "istanbul",
    val inventoryCapacity: Int = 5000,
    val isVip: Boolean = false,
    val loginStreak: Int = 0,
    val lastDailyRewardMs: Long = 0L,
    val businessesJson: String = "[]",
    val inventoryJson: String = "[]",
    val managersJson: String = "[]",
    val dailyQuestStateJson: String = "{}",
    val activeResearchesJson: String = "{}",
    val researchLevelsJson: String = "{}",
    val guildSharesJson: String = "{}",
    val guildBuyPricesJson: String = "{}",
    val megaProjectsJson: String = "[]",
    val selectedTheme: String = "",
    val schemaVersion: Int = 2,
    val saveTimestampMs: Long = System.currentTimeMillis(),
    val isMigratedToNoSql: Boolean = false
)

/**
 * Source type indicator for Strangler Fig pattern resolution.
 */
enum class SaveSourceType {
    LOCAL_JSON,
    GOOGLE_DRIVE,
    SUPABASE_NOSQL,
    STRANGLER_HYBRID
}

/**
 * Result state of save operations.
 */
sealed class SaveResult {
    data class Success(val source: SaveSourceType, val timestampMs: Long) : SaveResult()
    data class PartialSuccess(val savedSources: List<SaveSourceType>, val message: String) : SaveResult()
    data class Error(val cause: Throwable, val message: String) : SaveResult()
}
