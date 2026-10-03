package com.example.data.save.impl

import android.util.Log
import com.example.data.SupabaseManager
import com.example.data.SupabasePlayerPayload
import com.example.data.network.AppJson
import com.example.data.network.RawSavePayloadDto
import com.example.data.network.RpcSyncCurrenciesRequestDto
import com.example.data.save.GameSaveData
import com.example.data.save.PlayerCurrencySnapshot
import com.example.data.save.interfaces.ICloudNoSqlSaveSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseNoSqlSaveSource : ICloudNoSqlSaveSource {

    private val TAG = "SupabaseNoSqlSave"

    private fun isEligibleForCloudSync(playerId: String): Boolean {
        if (playerId.isBlank() ||
            playerId == "local" ||
            playerId == "local_trader" ||
            playerId == "misafir_tuccar" ||
            playerId.startsWith("guest") ||
            playerId.startsWith("trader_") ||
            playerId.startsWith("demo") ||
            !playerId.contains("@") && !playerId.contains("_")
        ) {
            return false
        }
        return true
    }

    override suspend fun savePlayerDocument(playerId: String, data: GameSaveData): Boolean = withContext(Dispatchers.IO) {
        if (!isEligibleForCloudSync(playerId)) {
            Log.d(TAG, "Player $playerId is not authenticated. Supabase save skipped.")
            return@withContext false
        }
        try {
            val rawSaveDto = RawSavePayloadDto(
                schemaVersion = data.schemaVersion,
                saveTimestampMs = data.saveTimestampMs,
                selectedTheme = data.selectedTheme,
                megaProjectsJson = data.megaProjectsJson,
                lockedDepositBalance = data.currencies.lockedDepositBalance,
                lockedDepositStartTimeMs = data.currencies.lockedDepositStartTimeMs,
                lockedDepositDurationMs = data.currencies.lockedDepositDurationMs,
                saveVersion = data.saveVersion
            )

            val payload = SupabasePlayerPayload(
                id = playerId,
                name = data.playerName,
                companyName = data.companyName,
                money = data.currencies.money,
                loanAmount = data.currencies.loanAmount,
                depositBalance = data.currencies.depositBalance,
                dailyIncome = data.currencies.dailyIncome,
                dailyExpense = data.currencies.dailyExpense,
                totalProfit = data.currencies.totalProfit,
                xp = data.currencies.xp,
                level = data.currencies.level,
                inventoryCapacity = data.inventoryCapacity,
                currentCity = data.currentCity,
                isVip = data.isVip,
                gems = data.currencies.gems,
                lastDailyRewardMs = data.lastDailyRewardMs,
                loginStreak = data.loginStreak,
                isOnlineRegistered = true,
                onlineEmail = if (playerId.contains("@")) playerId else (data.playerName.ifBlank { playerId }),
                businessesJson = data.businessesJson,
                inventoryJson = data.inventoryJson,
                managersJson = data.managersJson,
                dailyQuestStateJson = data.dailyQuestStateJson,
                activeResearchesJson = data.activeResearchesJson,
                researchLevelsJson = data.researchLevelsJson,
                guildSharesJson = data.guildSharesJson,
                guildBuyPricesJson = data.guildBuyPricesJson,
                rawSaveJson = AppJson.encodeToString(rawSaveDto),
                saveVersion = data.saveVersion
            )

            val success = SupabaseManager.syncPlayerToSupabase(payload)
            if (success) {
                Log.d(TAG, "Successfully synced player document $playerId to Supabase NoSQL")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving player document to Supabase NoSQL", e)
            false
        }
    }

    override suspend fun fetchPlayerDocument(playerId: String): GameSaveData? = withContext(Dispatchers.IO) {
        try {
            val payload = SupabaseManager.fetchPlayerFromSupabase(playerId) ?: return@withContext null

            var megaProjects = "[]"
            var theme = ""
            var lockedDeposit = 0L
            var lockedStart = 0L
            var lockedDuration = 0L
            var timestamp = System.currentTimeMillis()
            var schemaVer = 2

            if (!payload.rawSaveJson.isNullOrBlank()) {
                try {
                    val rawDto = AppJson.decodeFromString<RawSavePayloadDto>(payload.rawSaveJson)
                    megaProjects = rawDto.megaProjectsJson
                    theme = rawDto.selectedTheme
                    lockedDeposit = rawDto.lockedDepositBalance
                    lockedStart = rawDto.lockedDepositStartTimeMs
                    lockedDuration = rawDto.lockedDepositDurationMs
                    timestamp = rawDto.saveTimestampMs
                    schemaVer = rawDto.schemaVersion
                } catch (_: Exception) { }
            }

            GameSaveData(
                playerId = payload.id,
                playerName = payload.name,
                companyName = payload.companyName,
                currencies = PlayerCurrencySnapshot(
                    money = payload.money,
                    depositBalance = payload.depositBalance,
                    loanAmount = payload.loanAmount,
                    lockedDepositBalance = lockedDeposit,
                    lockedDepositStartTimeMs = lockedStart,
                    lockedDepositDurationMs = lockedDuration,
                    gems = payload.gems,
                    xp = payload.xp,
                    level = payload.level,
                    totalProfit = payload.totalProfit,
                    dailyIncome = payload.dailyIncome,
                    dailyExpense = payload.dailyExpense
                ),
                currentCity = payload.currentCity,
                inventoryCapacity = payload.inventoryCapacity,
                isVip = payload.isVip,
                loginStreak = payload.loginStreak,
                lastDailyRewardMs = payload.lastDailyRewardMs,
                businessesJson = payload.businessesJson,
                inventoryJson = payload.inventoryJson,
                managersJson = payload.managersJson,
                dailyQuestStateJson = payload.dailyQuestStateJson,
                activeResearchesJson = payload.activeResearchesJson,
                researchLevelsJson = payload.researchLevelsJson,
                guildSharesJson = payload.guildSharesJson,
                guildBuyPricesJson = payload.guildBuyPricesJson,
                megaProjectsJson = megaProjects,
                selectedTheme = theme,
                schemaVersion = schemaVer,
                saveVersion = maxOf(payload.saveVersion, 1L),
                saveTimestampMs = timestamp,
                isMigratedToNoSql = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching player document from Supabase NoSQL", e)
            null
        }
    }

    override suspend fun syncPlayerCurrencies(playerId: String, currencies: PlayerCurrencySnapshot): Boolean = withContext(Dispatchers.IO) {
        if (!isEligibleForCloudSync(playerId)) return@withContext false
        try {
            val rpcParams = RpcSyncCurrenciesRequestDto(
                playerId = playerId,
                money = currencies.money,
                depositBalance = currencies.depositBalance,
                loanAmount = currencies.loanAmount,
                gems = currencies.gems
            )
            val jsonParamsStr = AppJson.encodeToString(rpcParams)
            val (success, _) = SupabaseManager.executeRpc("sync_player_currencies", jsonParamsStr)
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing player currencies", e)
            false
        }
    }

    override suspend fun isCloudAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.fetchServerTimeMsFromSupabase() != null
        } catch (_: Exception) {
            false
        }
    }
}
