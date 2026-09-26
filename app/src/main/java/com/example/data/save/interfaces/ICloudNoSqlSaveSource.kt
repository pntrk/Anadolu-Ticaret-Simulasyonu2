package com.example.data.save.interfaces

import com.example.data.save.GameSaveData
import com.example.data.save.PlayerCurrencySnapshot

/**
 * Interface contract for cloud-based NoSQL / Document Store (e.g. Supabase JSONB / Postgres documents).
 */
interface ICloudNoSqlSaveSource {
    suspend fun savePlayerDocument(playerId: String, data: GameSaveData): Boolean
    suspend fun fetchPlayerDocument(playerId: String): GameSaveData?
    suspend fun syncPlayerCurrencies(playerId: String, currencies: PlayerCurrencySnapshot): Boolean
    suspend fun isCloudAvailable(): Boolean
}
