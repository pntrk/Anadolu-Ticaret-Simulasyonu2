package com.example.data.save.interfaces

import com.example.data.save.GameSaveData
import com.example.data.save.SaveResult
import com.example.data.save.SaveSourceType

/**
 * Strangler Fig Gateway Interface:
 * Manages parallel execution between legacy Local/Drive JSON storage and modern Cloud NoSQL storage.
 * Seamlessly migrates schema versions, balances, and player assets without downtime or data loss.
 */
interface IGameSaveGateway {
    /**
     * Dual-writes save data to both Supabase NoSQL and Legacy Drive/JSON storage.
     */
    suspend fun saveGame(playerId: String, data: GameSaveData, rawJson: String? = null): SaveResult

    /**
     * Reads player state with Strangler fallback strategy (Cloud NoSQL -> Legacy Drive/JSON -> Migration).
     */
    suspend fun loadGame(playerId: String): Pair<GameSaveData?, SaveSourceType>

    /**
     * Migrates legacy local/Drive JSON payload into the modern Supabase NoSQL database schema.
     */
    suspend fun migrateLegacySaveToNoSql(playerId: String, legacyJson: String): Boolean

    /**
     * Reconciles and transfers currency and wealth values during format upgrades.
     */
    fun reconcileCurrencies(legacyData: GameSaveData, cloudData: GameSaveData?): GameSaveData
}
