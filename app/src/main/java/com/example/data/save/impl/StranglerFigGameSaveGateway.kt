package com.example.data.save.impl

import android.util.Log
import com.example.data.save.GameSaveData
import com.example.data.save.PlayerCurrencySnapshot
import com.example.data.save.SaveResult
import com.example.data.save.SaveSourceType
import com.example.data.save.interfaces.ICloudNoSqlSaveSource
import com.example.data.save.interfaces.IGameSaveGateway
import com.example.data.save.interfaces.IGoogleDriveDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.network.AppJson
import com.example.data.network.GameSaveDataDto

/**
 * StranglerFigGameSaveGateway:
 * Implements the Strangler Fig architectural pattern to migrate monolithic/local JSON saves
 * to a cloud-based NoSQL database (Supabase JSONB documents) in zero-downtime parallel operation.
 */
class StranglerFigGameSaveGateway(
    private val legacyDriveDataSource: IGoogleDriveDataSource,
    private val cloudNoSqlSaveSource: ICloudNoSqlSaveSource,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : IGameSaveGateway {

    private val TAG = "StranglerSaveGateway"

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

    override suspend fun saveGame(playerId: String, data: GameSaveData, rawJson: String?): SaveResult = withContext(Dispatchers.IO) {
        val savedSources = mutableListOf<SaveSourceType>()
        var lastError: Throwable? = null

        val isEligible = isEligibleForCloudSync(playerId)

        // 1. Save to Cloud NoSQL (Supabase JSONB Document) ONLY if user is authenticated with Google
        val cloudSaveDeferred = async {
            if (!isEligible) {
                Log.d(TAG, "Player $playerId is offline/guest. Cloud NoSQL save skipped.")
                return@async null
            }
            try {
                if (cloudNoSqlSaveSource.savePlayerDocument(playerId, data)) {
                    SaveSourceType.SUPABASE_NOSQL
                } else null
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Cloud NoSQL save failed, falling back to legacy storage", e)
                null
            }
        }

        // 2. Save to Local JSON (and Google Drive AppData if authenticated)
        val legacySaveDeferred = async {
            try {
                val jsonToSave = rawJson ?: serializeGameSaveDataToJson(data)
                if (legacyDriveDataSource.uploadSaveJson(jsonToSave)) {
                    SaveSourceType.GOOGLE_DRIVE
                } else null
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Legacy storage save failed", e)
                null
            }
        }

        val cloudResult = cloudSaveDeferred.await()
        if (cloudResult != null) savedSources.add(cloudResult)

        val legacyResult = legacySaveDeferred.await()
        if (legacyResult != null) savedSources.add(legacyResult)

        when {
            savedSources.contains(SaveSourceType.SUPABASE_NOSQL) && savedSources.contains(SaveSourceType.GOOGLE_DRIVE) -> {
                SaveResult.Success(SaveSourceType.STRANGLER_HYBRID, System.currentTimeMillis())
            }
            savedSources.isNotEmpty() -> {
                SaveResult.PartialSuccess(savedSources, "Saved to ${savedSources.joinToString()}")
            }
            else -> {
                SaveResult.Error(lastError ?: Exception("All save targets failed"), "Save failure on all backends")
            }
        }
    }

    override suspend fun loadGame(playerId: String): Pair<GameSaveData?, SaveSourceType> = withContext(Dispatchers.IO) {
        val isEligible = isEligibleForCloudSync(playerId)

        // Step 1: Attempt to load from Cloud NoSQL (Supabase) ONLY if user is authenticated with Google
        val cloudDocDeferred = async {
            if (!isEligible) return@async null
            try {
                cloudNoSqlSaveSource.fetchPlayerDocument(playerId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading from Cloud NoSQL", e)
                null
            }
        }

        // Step 2: Attempt to read legacy JSON from Drive/Local backup
        val legacyJsonDeferred = async {
            try {
                legacyDriveDataSource.downloadSaveJson()
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading from Legacy Drive/Local", e)
                null
            }
        }

        val cloudData = cloudDocDeferred.await()
        val legacyRawJson = legacyJsonDeferred.await()
        val legacyData = legacyRawJson?.let { parseJsonToGameSaveData(it, playerId) }

        // Step 3: Strangler Fig Reconciliation & Migration
        if (cloudData != null && legacyData != null) {
            // Both exist: check timestamps
            if (cloudData.saveTimestampMs >= legacyData.saveTimestampMs) {
                Log.d(TAG, "Loaded latest state from Supabase NoSQL Document Store")
                return@withContext Pair(cloudData, SaveSourceType.SUPABASE_NOSQL)
            } else {
                Log.d(TAG, "Legacy save is newer. Migrating currencies & state to Supabase NoSQL...")
                val reconciled = reconcileCurrencies(legacyData, cloudData)
                if (isEligible) {
                    // Async strangle migration to Cloud ONLY if authenticated
                    appScope.launch {
                        cloudNoSqlSaveSource.savePlayerDocument(playerId, reconciled)
                    }
                }
                return@withContext Pair(reconciled, SaveSourceType.STRANGLER_HYBRID)
            }
        } else if (cloudData != null) {
            Log.d(TAG, "Loaded exclusively from Supabase NoSQL")
            return@withContext Pair(cloudData, SaveSourceType.SUPABASE_NOSQL)
        } else if (legacyData != null) {
            Log.d(TAG, "Cloud state empty. Preserving local JSON state...")
            val reconciled = reconcileCurrencies(legacyData, null)
            if (isEligible) {
                appScope.launch {
                    cloudNoSqlSaveSource.savePlayerDocument(playerId, reconciled)
                }
            }
            return@withContext Pair(reconciled, SaveSourceType.LOCAL_JSON)
        }

        Pair(null, SaveSourceType.LOCAL_JSON)
    }

    override suspend fun migrateLegacySaveToNoSql(playerId: String, legacyJson: String): Boolean = withContext(Dispatchers.IO) {
        if (!isEligibleForCloudSync(playerId)) {
            Log.d(TAG, "Player $playerId is offline/guest. Cloud migration skipped.")
            return@withContext false
        }
        try {
            val parsed = parseJsonToGameSaveData(legacyJson, playerId) ?: return@withContext false
            val reconciled = reconcileCurrencies(parsed, null)
            cloudNoSqlSaveSource.savePlayerDocument(playerId, reconciled)
        } catch (e: Exception) {
            Log.e(TAG, "Migration to NoSQL failed", e)
            false
        }
    }

    override fun reconcileCurrencies(legacyData: GameSaveData, cloudData: GameSaveData?): GameSaveData {
        if (cloudData == null) {
            return legacyData.copy(isMigratedToNoSql = true)
        }

        // Preserve maximum legitimate wealth across versions
        val maxMoney = maxOf(legacyData.currencies.money, cloudData.currencies.money)
        val maxDeposit = maxOf(legacyData.currencies.depositBalance, cloudData.currencies.depositBalance)
        val minLoan = if (legacyData.currencies.loanAmount > 0 && cloudData.currencies.loanAmount > 0) {
            minOf(legacyData.currencies.loanAmount, cloudData.currencies.loanAmount)
        } else {
            maxOf(legacyData.currencies.loanAmount, cloudData.currencies.loanAmount)
        }
        val maxGems = maxOf(legacyData.currencies.gems, cloudData.currencies.gems)
        val maxXp = maxOf(legacyData.currencies.xp, cloudData.currencies.xp)
        val maxLevel = maxOf(legacyData.currencies.level, cloudData.currencies.level)

        val updatedCurrencies = legacyData.currencies.copy(
            money = maxMoney,
            depositBalance = maxDeposit,
            loanAmount = minLoan,
            gems = maxGems,
            xp = maxXp,
            level = maxLevel
        )

        return legacyData.copy(
            currencies = updatedCurrencies,
            isMigratedToNoSql = true,
            saveTimestampMs = System.currentTimeMillis()
        )
    }

    private fun serializeGameSaveDataToJson(data: GameSaveData): String {
        val dto = GameSaveDataDto(
            id = data.playerId,
            name = data.playerName,
            companyName = data.companyName,
            money = data.currencies.money,
            depositBalance = data.currencies.depositBalance,
            loanAmount = data.currencies.loanAmount,
            lockedDepositBalance = data.currencies.lockedDepositBalance,
            lockedDepositStartTimeMs = data.currencies.lockedDepositStartTimeMs,
            lockedDepositDurationMs = data.currencies.lockedDepositDurationMs,
            gems = data.currencies.gems,
            xp = data.currencies.xp,
            level = data.currencies.level,
            dailyIncome = data.currencies.dailyIncome,
            dailyExpense = data.currencies.dailyExpense,
            totalProfit = data.currencies.totalProfit,
            inventoryCapacity = data.inventoryCapacity,
            currentCity = data.currentCity,
            isVip = data.isVip,
            loginStreak = data.loginStreak,
            lastDailyRewardMs = data.lastDailyRewardMs,
            businessesJson = data.businessesJson,
            inventoryJson = data.inventoryJson,
            managersJson = data.managersJson,
            dailyQuestStateJson = data.dailyQuestStateJson,
            activeResearchesJson = data.activeResearchesJson,
            researchLevelsJson = data.researchLevelsJson,
            guildSharesJson = data.guildSharesJson,
            guildBuyPricesJson = data.guildBuyPricesJson,
            megaProjectsJson = data.megaProjectsJson,
            selectedTheme = data.selectedTheme,
            schemaVersion = data.schemaVersion,
            saveTimestampMs = data.saveTimestampMs,
            isMigratedToNoSql = data.isMigratedToNoSql
        )
        return AppJson.encodeToString(dto)
    }

    private fun parseJsonToGameSaveData(jsonStr: String, fallbackPlayerId: String): GameSaveData? {
        return try {
            val dto = AppJson.decodeFromString<GameSaveDataDto>(jsonStr)
            val pDto = dto.player

            val pId = (pDto?.id?.ifBlank { null } ?: dto.id.ifBlank { null }) ?: fallbackPlayerId
            val pName = (pDto?.name?.ifBlank { null } ?: dto.name.ifBlank { null }) ?: "Tüccar"
            val compName = (pDto?.companyName?.ifBlank { null } ?: dto.companyName.ifBlank { null }) ?: "Tüccar Holding"
            val money = pDto?.money ?: dto.money
            val deposit = pDto?.depositBalance ?: dto.depositBalance
            val loan = pDto?.loanAmount ?: dto.loanAmount
            val lockedDeposit = pDto?.lockedDepositBalance ?: dto.lockedDepositBalance
            val lockedDepositStart = pDto?.lockedDepositStartTimeMs ?: dto.lockedDepositStartTimeMs
            val lockedDepositDuration = pDto?.lockedDepositDurationMs ?: dto.lockedDepositDurationMs
            val gems = pDto?.gems ?: dto.gems
            val xp = pDto?.xp ?: dto.xp
            val level = pDto?.level ?: dto.level
            val dailyIncome = pDto?.dailyIncome ?: dto.dailyIncome
            val dailyExpense = pDto?.dailyExpense ?: dto.dailyExpense
            val totalProfit = pDto?.totalProfit ?: dto.totalProfit
            val invCap = pDto?.inventoryCapacity ?: dto.inventoryCapacity
            val city = (pDto?.currentCity?.ifBlank { null } ?: dto.currentCity.ifBlank { null }) ?: "istanbul"
            val isVip = pDto?.isVip ?: dto.isVip
            val loginStreak = pDto?.loginStreak ?: dto.loginStreak
            val lastReward = pDto?.lastDailyRewardMs ?: dto.lastDailyRewardMs

            GameSaveData(
                playerId = pId,
                playerName = pName,
                companyName = compName,
                currencies = PlayerCurrencySnapshot(
                    money = money,
                    depositBalance = deposit,
                    loanAmount = loan,
                    lockedDepositBalance = lockedDeposit,
                    lockedDepositStartTimeMs = lockedDepositStart,
                    lockedDepositDurationMs = lockedDepositDuration,
                    gems = gems,
                    xp = xp,
                    level = level,
                    totalProfit = totalProfit,
                    dailyIncome = dailyIncome,
                    dailyExpense = dailyExpense
                ),
                currentCity = city,
                inventoryCapacity = invCap,
                isVip = isVip,
                loginStreak = loginStreak,
                lastDailyRewardMs = lastReward,
                businessesJson = dto.businessesJson,
                inventoryJson = dto.inventoryJson,
                managersJson = dto.managersJson,
                dailyQuestStateJson = dto.dailyQuestStateJson,
                activeResearchesJson = dto.activeResearchesJson,
                researchLevelsJson = dto.researchLevelsJson,
                guildSharesJson = dto.guildSharesJson,
                guildBuyPricesJson = dto.guildBuyPricesJson,
                megaProjectsJson = dto.megaProjectsJson,
                selectedTheme = dto.selectedTheme,
                schemaVersion = dto.schemaVersion,
                saveTimestampMs = if (dto.saveTimestampMs > 0L) dto.saveTimestampMs else System.currentTimeMillis(),
                isMigratedToNoSql = dto.isMigratedToNoSql
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing JSON to GameSaveData", e)
            null
        }
    }
}
