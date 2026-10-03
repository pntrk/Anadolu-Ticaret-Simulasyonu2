package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.example.data.network.AppJson
import com.example.data.network.CompanyManagerDto
import com.example.data.network.ManagerActionLogDto
import com.example.data.network.anyToJsonElement
import com.example.data.network.jsonElementToAny
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class SupabasePlayerPayload(
    val id: String,
    val name: String,
    val companyName: String,
    val money: Long,
    val loanAmount: Long,
    val depositBalance: Long,
    val dailyIncome: Long,
    val dailyExpense: Long,
    val totalProfit: Long,
    val xp: Int,
    val level: Int,
    val inventoryCapacity: Int,
    val currentCity: String,
    val isVip: Boolean,
    val gems: Int,
    val lastDailyRewardMs: Long,
    val loginStreak: Int,
    val dollarBalance: Long = 0L,
    val dollarDepositBalance: Long = 0L,
    val dollarLoanAmount: Long = 0L,
    val isOnlineRegistered: Boolean,
    val onlineEmail: String,
    val businessesJson: String,
    val inventoryJson: String,
    val activeDeliveriesJson: String = "[]",
    val activeProductionsJson: String = "[]",
    val managersJson: String = "[]",
    val dailyQuestStateJson: String = "{}",
    val activeResearchesJson: String,
    val researchLevelsJson: String,
    val guildSharesJson: String,
    val guildBuyPricesJson: String,
    val rawSaveJson: String?,
    val lastSavedTime: Long = 0L,
    val saveVersion: Long = 1L
) {
    fun toSaveJson(): String {
        val baseMap = mutableMapOf<String, JsonElement>()
        if (!rawSaveJson.isNullOrBlank()) {
            try {
                val parsed = AppJson.parseToJsonElement(rawSaveJson) as? JsonObject
                if (parsed != null) {
                    baseMap.putAll(parsed)
                }
            } catch (_: Exception) {}
        }

        val effectiveLastSavedTime = if (lastSavedTime > 0L) {
            lastSavedTime
        } else {
            (baseMap["last_saved_time"] as? JsonPrimitive)?.longOrNull 
                ?: (baseMap["exportedAtMs"] as? JsonPrimitive)?.longOrNull 
                ?: System.currentTimeMillis()
        }

        val effectiveSaveVersion = if (saveVersion > 0L) {
            saveVersion
        } else {
            (baseMap["save_version"] as? JsonPrimitive)?.longOrNull 
                ?: (baseMap["backup_version"] as? JsonPrimitive)?.longOrNull
                ?: (baseMap["saveVersion"] as? JsonPrimitive)?.longOrNull
                ?: 1L
        }

        // Smart lossless merge for managers
        val rawManagersStr = when (val elem = baseMap["managers_json"] ?: baseMap["managers"]) {
            is JsonPrimitive -> elem.content
            is JsonArray -> elem.toString()
            is JsonObject -> elem.toString()
            else -> null
        }
        val colManagersStr = managersJson.takeIf { it.isNotBlank() && it != "null" && it != "[]" }
        val mergedManagersJson = SupabaseManager.mergeManagersPreservingHighest(colManagersStr, rawManagersStr)

        // Smart lossless merge for research levels
        val rawRLevelsStr = when (val elem = baseMap["research_levels_json"] ?: baseMap["research_levels"]) {
            is JsonPrimitive -> elem.content
            is JsonObject -> elem.toString()
            else -> null
        }
        val mergedResearchLevelsJson = SupabaseManager.mergeResearchLevelsPreservingHighest(rawRLevelsStr, researchLevelsJson)

        // Smart lossless merge for active researches (preserving ongoing timestamps)
        val rawActiveStr = when (val elem = baseMap["active_researches_json"] ?: baseMap["active_researches"]) {
            is JsonPrimitive -> elem.content
            is JsonObject, is JsonArray -> elem.toString()
            else -> null
        }
        val directActiveKey = (baseMap["active_research_tech_key"] as? JsonPrimitive)?.content?.removePrefix("tech_")
        val directActiveEnd = (baseMap["research_end_time_ms"] as? JsonPrimitive)?.longOrNull ?: 0L
        val mergedActiveResearchesJson = SupabaseManager.mergeActiveResearchesPreservingHighest(
            rawActiveStr,
            activeResearchesJson,
            directActiveKey,
            directActiveEnd
        )

        val allTechKeys = listOf(
            "green_energy", "quality_control", "logistics", "automation",
            "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
            "aerospace", "heavy_industry", "consumer_goods", "petrochem",
            "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics",
            "global_finance", "cultural_heritage"
        )
        val mergedLevels = try {
            AppJson.decodeFromString<Map<String, Int>>(mergedResearchLevelsJson)
        } catch (_: Exception) { emptyMap() }
        val mergedActive = try {
            AppJson.decodeFromString<Map<String, Long>>(mergedActiveResearchesJson)
        } catch (_: Exception) { emptyMap() }
        val now = System.currentTimeMillis()

        fun safeElem(jsonStr: String): JsonElement? {
            if (jsonStr.isBlank()) return null
            return try {
                AppJson.parseToJsonElement(jsonStr)
            } catch (_: Exception) {
                null
            }
        }

        val columnOverrides = buildJsonObject {
            put("name", name)
            put("player_name", name)
            put("company_name", companyName)
            put("money", money)
            put("loan_amount", loanAmount)
            put("deposit_balance", depositBalance)
            put("daily_income", dailyIncome)
            put("daily_expense", dailyExpense)
            put("total_profit", totalProfit)
            put("xp", xp)
            put("level", level)
            val effectiveCapacity = maxOf(
                inventoryCapacity,
                (baseMap["inventory_capacity"] as? JsonPrimitive)?.intOrNull ?: 0,
                (baseMap["inventoryCapacity"] as? JsonPrimitive)?.intOrNull ?: 0,
                ((baseMap["warehouse_level"] as? JsonPrimitive)?.intOrNull ?: 0).let { if (it > 0) 5000 + (it - 1) * 2500 else 0 },
                ((baseMap["warehouseLevel"] as? JsonPrimitive)?.intOrNull ?: 0).let { if (it > 0) 5000 + (it - 1) * 2500 else 0 },
                5000
            )
            val effectiveWarehouseLevel = maxOf(
                1 + ((effectiveCapacity - 5000) / 2500),
                (baseMap["warehouse_level"] as? JsonPrimitive)?.intOrNull ?: 1,
                (baseMap["warehouseLevel"] as? JsonPrimitive)?.intOrNull ?: 1,
                1
            )
            put("inventory_capacity", effectiveCapacity)
            put("inventoryCapacity", effectiveCapacity)
            put("warehouse_level", effectiveWarehouseLevel)
            put("warehouseLevel", effectiveWarehouseLevel)
            put("current_city", migrateLegacyCity(currentCity))
            put("is_vip", isVip)
            put("gems", gems)
            put("last_daily_reward_ms", lastDailyRewardMs)
            put("login_streak", loginStreak)
            put("dollar_balance", money)
            put("dollar_deposit_balance", depositBalance)
            put("dollar_loan_amount", loanAmount)
            put("is_single_currency_migrated", true)
            put("is_online_registered", isOnlineRegistered)
            put("online_email", onlineEmail.ifBlank { id })
            if (businessesJson.isNotBlank() && businessesJson != "null") {
                put("businesses_json", businessesJson)
                safeElem(businessesJson)?.let { put("businesses", it) }
            }
            if (inventoryJson.isNotBlank() && inventoryJson != "null") {
                put("inventory_json", inventoryJson)
                safeElem(inventoryJson)?.let { put("inventory", it) }
            }
            if (activeDeliveriesJson.isNotBlank() && activeDeliveriesJson != "null") put("active_deliveries_json", activeDeliveriesJson)
            if (activeProductionsJson.isNotBlank() && activeProductionsJson != "null") put("active_productions_json", activeProductionsJson)
            put("managers_json", mergedManagersJson)
            safeElem(mergedManagersJson)?.let { put("managers", it) }
            if (dailyQuestStateJson.isNotBlank() && dailyQuestStateJson != "null") put("daily_quest_state_json", dailyQuestStateJson)
            put("active_researches_json", mergedActiveResearchesJson)
            safeElem(mergedActiveResearchesJson)?.let { put("active_researches", it) }
            put("research_levels_json", mergedResearchLevelsJson)
            safeElem(mergedResearchLevelsJson)?.let { put("research_levels", it) }
            allTechKeys.forEach { tech ->
                put("tech_$tech", mergedLevels[tech] ?: 0)
            }
            val firstOngoing = mergedActive.entries.firstOrNull { it.value > now }
            if (firstOngoing != null) {
                put("active_research_tech_key", firstOngoing.key)
                put("research_end_time_ms", firstOngoing.value)
            } else {
                put("active_research_tech_key", "")
                put("research_end_time_ms", 0L)
            }
            if (guildSharesJson.isNotBlank() && guildSharesJson != "null") put("player_guild_shares_json", guildSharesJson)
            if (guildBuyPricesJson.isNotBlank() && guildBuyPricesJson != "null") put("player_guild_buy_prices_json", guildBuyPricesJson)
            put("is_data_saved", true)
            put("has_set_warehouse", true)
            put("last_saved_time", effectiveLastSavedTime)
            put("save_version", effectiveSaveVersion)
            put("backup_version", effectiveSaveVersion)
            put("saveVersion", effectiveSaveVersion)
        }

        baseMap.putAll(columnOverrides)
        baseMap.remove("save_signature")
        return JsonObject(baseMap).toString()
    }
}

object SupabaseManager {
    private const val TAG = "SupabaseManager"
    const val SUPABASE_URL = "https://oztbbhxxrpeyaoyudtjw.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_s6nVXNsyMN4MVdlPf_kaVg_tPvchpQS"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getCurrentIsoTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    private fun parseIsoToEpochMs(isoStr: String): Long {
        if (isoStr.isBlank()) return 0L
        return try {
            val clean = if (isoStr.length >= 19) isoStr.substring(0, 19) else isoStr
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(clean)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun executeAndLog(request: Request, label: String): Boolean {
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Log.e(TAG, "[$label] Failed with status code ${response.code}: $body")
                    false
                } else {
                    Log.d(TAG, "[$label] Succeeded with code ${response.code}")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$label] Exception occurred: ${e.message}", e)
            false
        }
    }

    private fun safeParseJsonElement(jsonStr: String): JsonElement? {
        if (jsonStr.isBlank()) return null
        return try {
            AppJson.parseToJsonElement(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.optString(key: String, default: String = ""): String {
        val elem = this[key] as? JsonPrimitive ?: return default
        return elem.content
    }

    private fun JsonObject.optLong(key: String, default: Long = 0L): Long {
        val elem = this[key] as? JsonPrimitive ?: return default
        return elem.longOrNull ?: elem.content.toLongOrNull() ?: default
    }

    private fun JsonObject.optInt(key: String, default: Int = 0): Int {
        val elem = this[key] as? JsonPrimitive ?: return default
        return elem.longOrNull?.toInt() ?: elem.content.toIntOrNull() ?: default
    }

    private fun JsonObject.optDouble(key: String, default: Double = 0.0): Double {
        val elem = this[key] as? JsonPrimitive ?: return default
        return elem.doubleOrNull ?: elem.content.toDoubleOrNull() ?: default
    }

    private fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean {
        val elem = this[key] as? JsonPrimitive ?: return default
        return elem.booleanOrNull ?: elem.content.toBooleanStrictOrNull() ?: default
    }

    private fun JsonObject.optJsonObject(key: String): JsonObject? {
        return this[key] as? JsonObject
    }

    private fun JsonObject.optJsonString(key: String, fallbackKey: String? = null, default: String = ""): String {
        val elem = this[key]
        if (elem != null && elem !is JsonNull) {
            val s = if (elem is JsonPrimitive) elem.content else elem.toString()
            if (s.isNotBlank() && s != "null") return s
        }
        if (fallbackKey != null) {
            val fb = this[fallbackKey]
            if (fb != null && fb !is JsonNull) {
                val s = if (fb is JsonPrimitive) fb.content else fb.toString()
                if (s.isNotBlank() && s != "null") return s
            }
        }
        return default
    }

    private val unsupportedPlayerColumns = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val missingColumnRegex = Regex("""Could not find the '([^']+)' column of 'players' in the schema cache""", RegexOption.IGNORE_CASE)
    private val genericColumnRegex = Regex("""column "([^"]+)" of relation "players" does not exist""", RegexOption.IGNORE_CASE)
    private val genericColNotExistRegex = Regex("""column "([^"]+)" does not exist""", RegexOption.IGNORE_CASE)

    private suspend fun upsertPlayerDoc(initialObj: JsonObject): Boolean = withContext(Dispatchers.IO) {
        val currentMap = initialObj.toMutableMap()
        for (col in unsupportedPlayerColumns) {
            currentMap.remove(col)
        }

        var attempt = 0
        val maxAttempts = 10

        while (attempt < maxAttempts) {
            attempt++
            val requestBody = JsonObject(currentMap).toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?on_conflict=id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "[players_upsert] Successfully synced player to Supabase on attempt $attempt")
                        return@withContext true
                    }
                    val body = response.body?.string().orEmpty()
                    Log.w(TAG, "[players_upsert] Attempt $attempt failed with code ${response.code}: $body")

                    val match = missingColumnRegex.find(body)
                        ?: genericColumnRegex.find(body)
                        ?: genericColNotExistRegex.find(body)

                    if (match != null) {
                        val missingCol = match.groupValues[1]
                        Log.w(TAG, "[players_upsert] Column '$missingCol' not found in schema cache. Pruning and retrying...")
                        unsupportedPlayerColumns.add(missingCol)
                        currentMap.remove(missingCol)
                        continue
                    } else {
                        Log.e(TAG, "[players_upsert] Terminal error from Supabase: $body")
                        return@withContext false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[players_upsert] Network exception on attempt $attempt: ${e.message}", e)
                return@withContext false
            }
        }
        false
    }

    /**
     * Oyuncu kaydını SupabasePlayerPayload nesnesi ile Supabase PostgreSQL tablosuna Upsert eder.
     */
    suspend fun syncPlayerToSupabase(payload: SupabasePlayerPayload): Boolean = withContext(Dispatchers.IO) {
        val effectiveEmail = payload.onlineEmail.trim().ifBlank { if (payload.id != "local_player" && payload.id != "p_local") payload.id else "" }
        val effectiveUid = if (payload.id.isNotBlank() && payload.id != "local_player" && payload.id != "p_local") {
            payload.id
        } else {
            effectiveEmail.replace(".", "_")
        }
        if (!payload.isOnlineRegistered || effectiveEmail.isBlank() || effectiveEmail == "misafir_tuccar" || effectiveEmail == "local_trader" || effectiveUid.isBlank()) {
            Log.d(TAG, "Skipping Supabase sync for offline / guest player: ${payload.id}")
            return@withContext true
        }
        try {
            val remoteMeta = try {
                fetchPlayerMetadataFromSupabase(effectiveEmail) ?: fetchPlayerMetadataFromSupabase(effectiveUid)
            } catch (_: Exception) { null }

            val nextSaveVersion = payload.saveVersion + 1L

            val finalPayload = if (remoteMeta != null) {
                val remoteLevel = remoteMeta.level
                val remoteGems = remoteMeta.gems
                val remoteProfit = remoteMeta.totalProfit
                val remoteMoney = remoteMeta.money
                val localLevel = payload.level
                val localMoney = payload.money

                val isRemoteStrictlyHigher = (remoteLevel > localLevel) ||
                        (remoteLevel > 1 && localLevel <= 1) ||
                        (remoteProfit > payload.totalProfit + 1_000_000L) ||
                        (remoteGems > payload.gems + 50) ||
                        (remoteMoney > localMoney + 5_000_000L && localLevel <= 2)

                // CRITICAL SAFETY SHIELD: If local is a brand new start on a fresh device, NEVER overwrite a veteran cloud save!
                if (localLevel <= 1 && remoteLevel > 1 && payload.totalProfit <= 0L && localMoney <= 500_000L) {
                    Log.w(TAG, "[AntiDegradationGuard] CRITICAL SHIELD: Local device is at beginner state (Lvl $localLevel, Money ₳$localMoney) while cloud has Level $remoteLevel (Money ₳$remoteMoney, Gems $remoteGems). ABORTING destructive overwrite to protect player progress!")
                    return@withContext true
                }

                if (isRemoteStrictlyHigher) {
                    val fullRemote = try { fetchPlayerFromSupabase(effectiveEmail) ?: fetchPlayerFromSupabase(effectiveUid) } catch (_: Exception) { null }
                    if (fullRemote != null) {
                        Log.w(TAG, "[DataProtection] Preserving rich remote cloud save state (Remote Lvl ${fullRemote.level}, Gems ${fullRemote.gems}, Money ₳${fullRemote.money}) against lower local state (Local Lvl $localLevel)!")
                        payload.copy(
                            saveVersion = nextSaveVersion,
                            level = maxOf(payload.level, fullRemote.level),
                            xp = maxOf(payload.xp, fullRemote.xp),
                            gems = maxOf(payload.gems, fullRemote.gems),
                            money = maxOf(payload.money, fullRemote.money),
                            depositBalance = maxOf(payload.depositBalance, fullRemote.depositBalance),
                            totalProfit = maxOf(payload.totalProfit, fullRemote.totalProfit),
                            businessesJson = if (fullRemote.businessesJson.length > payload.businessesJson.length) fullRemote.businessesJson else payload.businessesJson,
                            inventoryJson = if (fullRemote.inventoryJson.length > payload.inventoryJson.length) fullRemote.inventoryJson else payload.inventoryJson,
                            managersJson = mergeManagersPreservingHighest(payload.managersJson, fullRemote.managersJson),
                            researchLevelsJson = mergeResearchLevelsPreservingHighest(payload.researchLevelsJson, fullRemote.researchLevelsJson),
                            activeResearchesJson = mergeActiveResearchesPreservingHighest(payload.activeResearchesJson, fullRemote.activeResearchesJson),
                            activeProductionsJson = if (fullRemote.activeProductionsJson.length > payload.activeProductionsJson.length) fullRemote.activeProductionsJson else payload.activeProductionsJson,
                            rawSaveJson = fullRemote.rawSaveJson ?: payload.rawSaveJson
                        )
                    } else {
                        payload.copy(
                            saveVersion = nextSaveVersion,
                            level = maxOf(payload.level, remoteLevel),
                            gems = maxOf(payload.gems, remoteGems),
                            money = maxOf(payload.money, remoteMoney),
                            depositBalance = maxOf(payload.depositBalance, remoteMeta.depositBalance),
                            totalProfit = maxOf(payload.totalProfit, remoteProfit),
                            xp = maxOf(payload.xp, remoteMeta.xp)
                        )
                    }
                } else {
                    payload.copy(saveVersion = nextSaveVersion)
                }
            } else {
                payload.copy(saveVersion = nextSaveVersion)
            }

            val businessesElem = safeParseJsonElement(finalPayload.businessesJson) ?: JsonArray(emptyList())
            val inventoryElem = safeParseJsonElement(finalPayload.inventoryJson) ?: JsonArray(emptyList())
            val managersElem = safeParseJsonElement(finalPayload.managersJson) ?: JsonArray(emptyList())
            val activeResearchesElem = safeParseJsonElement(finalPayload.activeResearchesJson) ?: JsonObject(emptyMap())
            val researchLevelsElem = safeParseJsonElement(finalPayload.researchLevelsJson) ?: JsonObject(emptyMap())
            val guildSharesElem = safeParseJsonElement(finalPayload.guildSharesJson) ?: JsonObject(emptyMap())
            val guildBuyPricesElem = safeParseJsonElement(finalPayload.guildBuyPricesJson) ?: JsonObject(emptyMap())
            val rawSaveElem = if (!finalPayload.rawSaveJson.isNullOrBlank()) safeParseJsonElement(finalPayload.rawSaveJson) else null

            val parsedTechLevels = try {
                AppJson.decodeFromString<Map<String, Int>>(finalPayload.researchLevelsJson)
            } catch (_: Exception) { emptyMap() }
            val parsedActiveMap = try {
                AppJson.decodeFromString<Map<String, Long>>(finalPayload.activeResearchesJson)
            } catch (_: Exception) { emptyMap() }
            val now = System.currentTimeMillis()
            val firstOngoing = parsedActiveMap.entries.firstOrNull { it.value > now }

            val jsonObject = buildJsonObject {
                put("id", effectiveUid)
                put("name", finalPayload.name)
                put("company_name", finalPayload.companyName)
                put("money", finalPayload.money)
                put("loan_amount", finalPayload.loanAmount)
                put("deposit_balance", finalPayload.depositBalance)
                put("daily_income", finalPayload.dailyIncome)
                put("daily_expense", finalPayload.dailyExpense)
                put("total_profit", finalPayload.totalProfit)
                put("xp", finalPayload.xp)
                put("level", finalPayload.level)
                put("inventory_capacity", finalPayload.inventoryCapacity)
                put("current_city", migrateLegacyCity(finalPayload.currentCity))
                put("is_vip", finalPayload.isVip)
                put("gems", finalPayload.gems)
                put("last_daily_reward_ms", finalPayload.lastDailyRewardMs)
                put("login_streak", finalPayload.loginStreak)
                put("is_online_registered", finalPayload.isOnlineRegistered)
                put("online_email", effectiveEmail)
                put("businesses", businessesElem)
                put("businesses_json", finalPayload.businessesJson)
                put("inventory", inventoryElem)
                put("inventory_json", finalPayload.inventoryJson)
                put("managers", managersElem)
                put("managers_json", finalPayload.managersJson)
                put("active_researches", activeResearchesElem)
                put("active_researches_json", finalPayload.activeResearchesJson)
                put("research_levels", researchLevelsElem)
                put("research_levels_json", finalPayload.researchLevelsJson)
                listOf(
                    "green_energy", "quality_control", "logistics", "automation",
                    "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
                    "aerospace", "heavy_industry", "consumer_goods", "petrochem",
                    "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics",
                    "global_finance", "cultural_heritage"
                ).forEach { tech ->
                    val lvl = parsedTechLevels[tech] ?: parsedTechLevels["tech_$tech"] ?: 0
                    put("tech_$tech", lvl)
                }
                if (firstOngoing != null) {
                    put("active_research_tech_key", firstOngoing.key.removePrefix("tech_"))
                    put("research_end_time_ms", firstOngoing.value)
                } else {
                    put("active_research_tech_key", "")
                    put("research_end_time_ms", 0L)
                }
                put("guild_shares", guildSharesElem)
                put("guild_buy_prices", guildBuyPricesElem)
                if (rawSaveElem != null) {
                    put("raw_save_json", rawSaveElem)
                }
                // Optional columns adaptively handled
                safeParseJsonElement(finalPayload.activeDeliveriesJson)?.let { put("active_deliveries", it) }
                safeParseJsonElement(finalPayload.activeProductionsJson)?.let { put("active_productions", it) }
                safeParseJsonElement(finalPayload.dailyQuestStateJson)?.let { put("daily_quest_state", it) }
                put("dollar_balance", finalPayload.dollarBalance)
                put("dollar_deposit_balance", finalPayload.dollarDepositBalance)
                put("dollar_loan_amount", finalPayload.dollarLoanAmount)
                put("save_version", nextSaveVersion)
                put("backup_version", nextSaveVersion)
                put("saveVersion", nextSaveVersion)
                put("last_saved_time", if (finalPayload.lastSavedTime > 0L) finalPayload.lastSavedTime else System.currentTimeMillis())
                put("updated_at", getCurrentIsoTimestamp())
            }

            upsertPlayerDoc(jsonObject)
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing player payload to Supabase", e)
            false
        }
    }

    suspend fun getPlayerMoney(playerId: String): Long? = withContext(Dispatchers.IO) {
        try {
            val encodedId = java.net.URLEncoder.encode(playerId, "UTF-8")
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?id=eq.$encodedId&select=money")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: ""
                val array = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
                if (array.isEmpty()) return@withContext null
                val obj = array[0] as? JsonObject ?: return@withContext null
                obj.optLong("money")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getPlayerMoney: ${e.message}")
            null
        }
    }

    suspend fun patchPlayerMoney(playerId: String, newMoney: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedId = java.net.URLEncoder.encode(playerId, "UTF-8")
            val jsonObject = buildJsonObject {
                put("money", newMoney)
            }
            val requestBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?id=eq.$encodedId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .patch(requestBody)
                .build()

            executeAndLog(request, "patchPlayerMoney")
        } catch (e: Exception) {
            Log.e(TAG, "Exception patchPlayerMoney: ${e.message}")
            false
        }
    }

    /**
     * Oyuncu kaydını Supabase PostgreSQL tablosuna Upsert (ekle/güncelle) eder.
     */
    suspend fun syncPlayerToSupabase(
        uid: String,
        name: String,
        companyName: String,
        money: Long,
        loanAmount: Long,
        depositBalance: Long,
        dailyIncome: Long,
        dailyExpense: Long,
        totalProfit: Long,
        xp: Int,
        level: Int,
        inventoryCapacity: Int,
        currentCity: String,
        isVip: Boolean,
        gems: Int,
        lastDailyRewardMs: Long,
        loginStreak: Int,
        dollarBalance: Long = 0L,
        dollarDepositBalance: Long = 0L,
        dollarLoanAmount: Long = 0L,
        isOnlineRegistered: Boolean,
        onlineEmail: String,
        businesses: List<BusinessEntity>,
        inventory: List<InventoryEntity>,
        activeDeliveriesJson: String = "[]",
        activeProductionsJson: String = "[]",
        managersJson: String,
        dailyQuestStateJson: String = "{}",
        activeResearchesJson: String,
        researchLevelsJson: String,
        guildSharesJson: String,
        guildBuyPricesJson: String,
        rawSaveJson: String? = null,
        saveVersion: Long = 1L
    ): Boolean = withContext(Dispatchers.IO) {
        val bArray = buildJsonArray {
            businesses.forEach { b ->
                add(buildJsonObject {
                    put("id", b.id)
                    put("type", b.type)
                    put("level", b.level)
                    put("wearLevel", b.wearLevel.toDouble())
                    put("wear_level", b.wearLevel.toDouble())
                    put("storageCapacity", b.getEffectiveStorageCapacity())
                    put("storage_capacity", b.getEffectiveStorageCapacity())
                    put("storedItemsJson", b.storedItemsJson)
                    put("isUpgrading", b.isUpgrading)
                    put("is_upgrading", b.isUpgrading)
                    if (b.upgradeEndTime != null) {
                        put("upgradeEndTime", b.upgradeEndTime)
                        put("upgrade_end_time", b.upgradeEndTime)
                    }
                    put("isConstructing", b.isConstructing)
                    put("is_constructing", b.isConstructing)
                    if (b.constructionEndTime != null) {
                        put("constructionEndTime", b.constructionEndTime)
                        put("construction_end_time", b.constructionEndTime)
                    }
                })
            }
        }

        val invArray = buildJsonArray {
            inventory.forEach { inv ->
                add(buildJsonObject {
                    put("itemId", inv.itemId)
                    put("quantity", inv.quantity)
                })
            }
        }

        val payload = SupabasePlayerPayload(
            id = uid,
            name = name,
            companyName = companyName,
            money = money,
            loanAmount = loanAmount,
            depositBalance = depositBalance,
            dailyIncome = dailyIncome,
            dailyExpense = dailyExpense,
            totalProfit = totalProfit,
            xp = xp,
            level = level,
            inventoryCapacity = inventoryCapacity,
            currentCity = currentCity,
            isVip = isVip,
            gems = gems,
            lastDailyRewardMs = lastDailyRewardMs,
            loginStreak = loginStreak,
            dollarBalance = dollarBalance,
            dollarDepositBalance = dollarDepositBalance,
            dollarLoanAmount = dollarLoanAmount,
            isOnlineRegistered = isOnlineRegistered,
            onlineEmail = onlineEmail,
            businessesJson = bArray.toString(),
            inventoryJson = invArray.toString(),
            activeDeliveriesJson = activeDeliveriesJson,
            activeProductionsJson = activeProductionsJson,
            managersJson = managersJson,
            dailyQuestStateJson = dailyQuestStateJson,
            activeResearchesJson = activeResearchesJson,
            researchLevelsJson = researchLevelsJson,
            guildSharesJson = guildSharesJson,
            guildBuyPricesJson = guildBuyPricesJson,
            rawSaveJson = rawSaveJson,
            lastSavedTime = System.currentTimeMillis(),
            saveVersion = saveVersion
        )

        syncPlayerToSupabase(payload)
    }

    /**
     * Supabase'den oyuncunun son ve en yüksek ilerlemeli kaydını çeker.
     */
    suspend fun fetchPlayerFromSupabase(uid: String): SupabasePlayerPayload? = withContext(Dispatchers.IO) {
        if (uid.isBlank() || uid == "local_player" || uid == "p_local" || uid == "misafir_tuccar") {
            return@withContext null
        }
        try {
            val cleanUid = uid.trim()
            val lowerEmail = cleanUid.lowercase()

            val candidates = mutableSetOf<String>()
            candidates.add(cleanUid)
            candidates.add(lowerEmail)

            // Gmail dot normalization: john.doe@gmail.com == johndoe@gmail.com
            if (lowerEmail.contains("@gmail.com") || lowerEmail.contains("@googlemail.com")) {
                val userPart = lowerEmail.substringBefore("@").replace(".", "")
                val domainPart = lowerEmail.substringAfter("@")
                val dotless = "$userPart@$domainPart"
                candidates.add(dotless)
                candidates.add(dotless.replace(".", "_"))
                candidates.add(dotless.replace("@", "_").replace(".", "_"))
            }

            if (cleanUid.contains(".")) {
                candidates.add(cleanUid.replace(".", "_"))
                candidates.add(lowerEmail.replace(".", "_"))
            }
            if (cleanUid.contains("@")) {
                candidates.add(cleanUid.replace("@", "_").replace(".", "_"))
                candidates.add(lowerEmail.replace("@", "_").replace(".", "_"))
                val nameOnly = cleanUid.substringBefore("@")
                if (nameOnly.isNotBlank()) {
                    candidates.add(nameOnly)
                    candidates.add(nameOnly.lowercase())
                    candidates.add("player_$nameOnly")
                    candidates.add("p_$nameOnly")
                }
            }
            if (cleanUid.contains("_gmail_com")) {
                candidates.add(cleanUid.replace("_gmail_com", "@gmail.com"))
                candidates.add(lowerEmail.replace("_gmail_com", "@gmail.com"))
            }

            val distinctCandidates = candidates.filter { it.isNotBlank() }.distinct()
            val foundPayloads = mutableListOf<SupabasePlayerPayload>()

            // Targeted queries for each candidate across online_email and id
            for (target in distinctCandidates) {
                foundPayloads.addAll(queryPlayerRecords("online_email", target, isIlike = false))
                foundPayloads.addAll(queryPlayerRecords("online_email", target, isIlike = true))
                foundPayloads.addAll(queryPlayerRecords("id", target, isIlike = false))
                foundPayloads.addAll(queryPlayerRecords("id", target, isIlike = true))
                if (foundPayloads.isNotEmpty()) break
            }

            // Fallback: Wildcard substring matching for username part if exact match wasn't found
            if (foundPayloads.isEmpty() && lowerEmail.contains("@")) {
                val userPart = lowerEmail.substringBefore("@")
                if (userPart.length >= 4) {
                    foundPayloads.addAll(queryPlayerRecords("online_email", "*$userPart*", isIlike = true))
                    foundPayloads.addAll(queryPlayerRecords("id", "*$userPart*", isIlike = true))
                }
            }

            if (foundPayloads.isEmpty()) {
                Log.w(TAG, "No Supabase player record found for candidates: $distinctCandidates")
                return@withContext null
            }

            // Return the BEST record by Save Version, Money, Level, Gems, Total Profit, and Timestamp to ensure highest progress is always restored
            val bestPayload = foundPayloads.distinctBy { it.id + "_" + it.onlineEmail + "_" + it.level + "_" + it.gems + "_" + it.money + "_" + it.saveVersion }.maxWithOrNull(
                compareBy<SupabasePlayerPayload> { it.saveVersion }
                    .thenBy { it.money }
                    .thenBy { it.level }
                    .thenBy { it.gems }
                    .thenBy { it.totalProfit }
                    .thenBy { it.lastSavedTime }
            )

            if (bestPayload != null) {
                Log.i(TAG, "Selected best Supabase player record: ${bestPayload.name} (v#${bestPayload.saveVersion}, Money ₳${bestPayload.money}, Lvl ${bestPayload.level}, Gems ${bestPayload.gems}, Profit ${bestPayload.totalProfit}, email=${bestPayload.onlineEmail})")
            }
            bestPayload
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching player from Supabase", e)
            null
        }
    }

    data class PlayerMetadata(
        val id: String,
        val saveVersion: Long,
        val level: Int,
        val gems: Int,
        val money: Long,
        val depositBalance: Long,
        val totalProfit: Long,
        val xp: Int
    )

    /**
     * Sadece seviye, para ve temel değerleri çeken ultra hafif (Egress dostu) sorgu.
     */
    suspend fun fetchPlayerMetadataFromSupabase(uid: String): PlayerMetadata? = withContext(Dispatchers.IO) {
        if (uid.isBlank() || uid == "local_player" || uid == "p_local" || uid == "misafir_tuccar") {
            return@withContext null
        }
        try {
            val cleanUid = uid.trim()
            val encodedUid = java.net.URLEncoder.encode(cleanUid, "UTF-8")
            val url = "$SUPABASE_URL/rest/v1/players?or=(id.eq.$encodedUid,online_email.eq.$encodedUid)&select=id,name,level,gems,money,deposit_balance,total_profit,xp,updated_at&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: ""
                val array = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
                if (array.isEmpty()) return@withContext null
                val obj = array[0] as? JsonObject ?: return@withContext null
                PlayerMetadata(
                    id = obj.optString("id", cleanUid),
                    saveVersion = 1L,
                    level = obj.optInt("level", 1),
                    gems = obj.optInt("gems", 0),
                    money = obj.optLong("money", 0L),
                    depositBalance = obj.optLong("deposit_balance", 0L),
                    totalProfit = obj.optLong("total_profit", 0L),
                    xp = obj.optInt("xp", 0)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchPlayerMetadataFromSupabase error for $uid: ${e.message}")
            null
        }
    }

    fun parseManagerDtoList(json: String?): List<CompanyManagerDto> {
        if (json.isNullOrBlank() || json == "[]" || json == "null" || json == "{}") return emptyList()
        try {
            return AppJson.decodeFromString<List<CompanyManagerDto>>(json)
        } catch (_: Exception) {
            try {
                val elem = safeParseJsonElement(json) ?: return emptyList()
                val itemsList: List<JsonObject> = when (elem) {
                    is JsonArray -> elem.mapNotNull { it as? JsonObject }
                    is JsonObject -> elem.values.mapNotNull { it as? JsonObject }
                    else -> emptyList()
                }
                return itemsList.mapNotNull { obj ->
                    val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                    val isHired = (obj["isHired"] as? JsonPrimitive)?.booleanOrNull
                        ?: (obj["is_hired"] as? JsonPrimitive)?.booleanOrNull
                        ?: ((obj["isHired"] as? JsonPrimitive)?.content?.let { it == "1" || it.equals("true", ignoreCase = true) })
                        ?: ((obj["is_hired"] as? JsonPrimitive)?.content?.let { it == "1" || it.equals("true", ignoreCase = true) })
                        ?: ((obj["isHired"] as? JsonPrimitive)?.intOrNull?.let { it == 1 })
                        ?: ((obj["is_hired"] as? JsonPrimitive)?.intOrNull?.let { it == 1 })
                        ?: false
                    val level = (obj["level"] as? JsonPrimitive)?.intOrNull 
                        ?: (obj["level"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 1
                    val eff = (obj["efficiency"] as? JsonPrimitive)?.doubleOrNull 
                        ?: (obj["efficiency"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 1.0
                    val sal = (obj["dailySalary"] as? JsonPrimitive)?.longOrNull
                        ?: (obj["daily_salary"] as? JsonPrimitive)?.longOrNull 
                        ?: (obj["dailySalary"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                    val name = (obj["name"] as? JsonPrimitive)?.content ?: ""
                    val title = (obj["title"] as? JsonPrimitive)?.content ?: ""
                    val spec = (obj["specialty"] as? JsonPrimitive)?.content ?: ""
                    val desc = (obj["description"] as? JsonPrimitive)?.content ?: ""
                    val isActive = (obj["isActive"] as? JsonPrimitive)?.booleanOrNull
                        ?: (obj["is_active"] as? JsonPrimitive)?.booleanOrNull
                        ?: ((obj["isActive"] as? JsonPrimitive)?.content?.let { it == "1" || it.equals("true", ignoreCase = true) })
                        ?: ((obj["is_active"] as? JsonPrimitive)?.content?.let { it == "1" || it.equals("true", ignoreCase = true) })
                        ?: true
                    val logs = mutableListOf<ManagerActionLogDto>()
                    val logsElem = (obj["actionLogs"] ?: obj["action_logs"]) as? JsonArray
                    logsElem?.forEach { lItem ->
                        val lObj = lItem as? JsonObject ?: return@forEach
                        val logId = (lObj["id"] as? JsonPrimitive)?.content ?: java.util.UUID.randomUUID().toString()
                        val ts = ((lObj["timestampMs"] ?: lObj["timestamp_ms"]) as? JsonPrimitive)?.longOrNull 
                            ?: ((lObj["timestampMs"] ?: lObj["timestamp_ms"]) as? JsonPrimitive)?.content?.toLongOrNull() ?: System.currentTimeMillis()
                        val lDesc = (lObj["description"] as? JsonPrimitive)?.content ?: ""
                        val impact = ((lObj["financialImpact"] ?: lObj["financial_impact"]) as? JsonPrimitive)?.longOrNull 
                            ?: ((lObj["financialImpact"] ?: lObj["financial_impact"]) as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                        logs.add(ManagerActionLogDto(logId, ts, lDesc, impact))
                    }
                    CompanyManagerDto(
                        id = id,
                        name = name,
                        title = title,
                        specialty = spec,
                        level = level.coerceIn(1, 5),
                        dailySalary = sal,
                        efficiency = eff,
                        isHired = isHired || level > 1 || logs.isNotEmpty(),
                        isActive = isActive,
                        description = desc,
                        actionLogs = logs
                    )
                }
            } catch (_: Exception) {
                return emptyList()
            }
        }
    }

    fun mergeManagersPreservingHighest(localJson: String?, remoteJson: String?): String {
        val defaultList = getDefaultCompanyManagers()
        val localDtos = parseManagerDtoList(localJson)
        val remoteDtos = parseManagerDtoList(remoteJson)
        
        val allIds = (localDtos.map { it.id } + remoteDtos.map { it.id } + defaultList.map { it.id }).distinct().filter { it.isNotBlank() }
        val merged = allIds.map { id ->
            val loc = localDtos.find { it.id == id }
            val rem = remoteDtos.find { it.id == id }
            val def = defaultList.find { it.id == id }
            
            val maxLevel = maxOf(loc?.level ?: 1, rem?.level ?: 1, 1).coerceIn(1, 5)
            val hasHireEvidence = (rem?.isHired == true) || (loc?.isHired == true) || (maxLevel > 1) ||
                    (rem?.actionLogs?.isNotEmpty() == true) || (loc?.actionLogs?.isNotEmpty() == true) ||
                    (rem?.name?.isNotBlank() == true && rem.name != def?.name) ||
                    (loc?.name?.isNotBlank() == true && loc.name != def?.name)
            val isHired = hasHireEvidence
            val eff = maxOf(loc?.efficiency ?: 1.0, rem?.efficiency ?: 1.0, if (maxLevel >= 5) 1.5 else 1.0)
            val name = when {
                rem != null && rem.name.isNotBlank() && rem.name != def?.name -> rem.name
                loc != null && loc.name.isNotBlank() && loc.name != def?.name -> loc.name
                rem != null && rem.name.isNotBlank() -> rem.name
                loc != null && loc.name.isNotBlank() -> loc.name
                def != null && def.name.isNotBlank() -> def.name
                else -> ""
            }
            val title = rem?.title?.ifBlank { loc?.title }?.ifBlank { def?.title } ?: def?.title.orEmpty()
            val spec = rem?.specialty?.ifBlank { loc?.specialty }?.ifBlank { def?.specialty } ?: def?.specialty.orEmpty()
            val desc = rem?.description?.ifBlank { loc?.description }?.ifBlank { def?.description } ?: def?.description.orEmpty()
            
            var expectedSalary = def?.dailySalary ?: 10000L
            for (lvl in 2..maxLevel) {
                expectedSalary = (expectedSalary * 1.25f).toLong()
            }
            val sal = maxOf(rem?.dailySalary ?: 0L, loc?.dailySalary ?: 0L, expectedSalary)
            val logs = when {
                rem != null && loc != null -> (rem.actionLogs + loc.actionLogs).distinctBy { it.id }.sortedByDescending { it.timestampMs }.take(50)
                rem != null && rem.actionLogs.isNotEmpty() -> rem.actionLogs
                loc != null && loc.actionLogs.isNotEmpty() -> loc.actionLogs
                else -> emptyList()
            }
            
            CompanyManagerDto(
                id = id,
                name = if (name.isNotBlank()) name else (def?.name ?: ""),
                title = title,
                specialty = spec,
                level = maxLevel,
                dailySalary = sal,
                efficiency = eff,
                isHired = isHired,
                isActive = rem?.isActive ?: loc?.isActive ?: true,
                description = desc,
                actionLogs = logs
            )
        }
        return AppJson.encodeToString(merged)
    }

    fun mergeResearchLevelsPreservingHighest(localJson: String?, remoteJson: String?): String {
        val allTechKeys = listOf(
            "green_energy", "quality_control", "logistics", "automation",
            "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
            "aerospace", "heavy_industry", "consumer_goods", "petrochem",
            "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics",
            "global_finance", "cultural_heritage"
        )
        val map = mutableMapOf<String, Int>()
        
        fun extract(json: String?) {
            if (json.isNullOrBlank() || json == "{}" || json == "null" || json == "[]") return
            try {
                val parsed = AppJson.decodeFromString<Map<String, Int>>(json)
                parsed.forEach { (k, v) ->
                    val base = k.removePrefix("tech_")
                    if (v > 0) {
                        map[base] = maxOf(map[base] ?: 0, v).coerceIn(0, 5)
                    }
                }
            } catch (_: Exception) {
                try {
                    val elem = safeParseJsonElement(json) as? JsonObject
                    elem?.forEach { (k, vElem) ->
                        val lvl = (vElem as? JsonPrimitive)?.intOrNull ?: (vElem as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        if (lvl > 0) {
                            val base = k.removePrefix("tech_")
                            map[base] = maxOf(map[base] ?: 0, lvl).coerceIn(0, 5)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        
        extract(localJson)
        extract(remoteJson)
        
        val result = mutableMapOf<String, Int>()
        allTechKeys.forEach { tech ->
            val lvl = (map[tech] ?: 0).coerceIn(0, 5)
            result[tech] = lvl
            result["tech_$tech"] = lvl
        }
        return AppJson.encodeToString(result)
    }

    fun mergeActiveResearchesPreservingHighest(
        localJson: String?,
        remoteJson: String?,
        singleKey: String? = null,
        singleEndTimeMs: Long = 0L
    ): String {
        val mergedActive = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()

        fun extract(json: String?) {
            if (json.isNullOrBlank() || json == "{}" || json == "null" || json == "[]") return
            try {
                val elem = safeParseJsonElement(json)
                when (elem) {
                    is JsonObject -> {
                        elem.forEach { (k, vElem) ->
                            val v = (vElem as? JsonPrimitive)?.longOrNull 
                                ?: (vElem as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                            if (v > now) {
                                val base = k.removePrefix("tech_")
                                mergedActive[base] = maxOf(mergedActive[base] ?: 0L, v)
                            }
                        }
                    }
                    is JsonArray -> {
                        elem.forEach { item ->
                            val obj = item as? JsonObject ?: return@forEach
                            val k = (obj["techKey"] ?: obj["tech_key"] ?: obj["key"] ?: obj["id"])?.jsonPrimitive?.content ?: return@forEach
                            val v = (obj["endTimeMs"] ?: obj["end_time_ms"] ?: obj["endTime"])?.jsonPrimitive?.longOrNull
                                ?: (obj["endTimeMs"] ?: obj["end_time_ms"])?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                            if (v > now) {
                                val base = k.removePrefix("tech_")
                                mergedActive[base] = maxOf(mergedActive[base] ?: 0L, v)
                            }
                        }
                    }
                    else -> {}
                }
            } catch (_: Exception) {}
        }

        extract(localJson)
        extract(remoteJson)

        if (!singleKey.isNullOrBlank() && singleEndTimeMs > now) {
            val base = singleKey.removePrefix("tech_")
            mergedActive[base] = maxOf(mergedActive[base] ?: 0L, singleEndTimeMs)
        }

        return AppJson.encodeToString(mergedActive)
    }

    private fun queryPlayerRecords(column: String, value: String, isIlike: Boolean = false): List<SupabasePlayerPayload> {
        val results = mutableListOf<SupabasePlayerPayload>()
        try {
            val op = if (isIlike) "ilike" else "eq"
            val httpUrl = ("$SUPABASE_URL/rest/v1/players").toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter(column, "$op.$value")
                ?.addQueryParameter("select", "*")
                ?.addQueryParameter("limit", "10")
                ?.build() ?: return emptyList()

            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "queryPlayerRecords error: code=${response.code}, query=$column.$op.$value")
                    return emptyList()
                }
                val responseBody = response.body?.string() ?: ""
                val jsonElement = safeParseJsonElement(responseBody) as? JsonArray ?: return emptyList()
                for (docElem in jsonElement) {
                    val doc = docElem as? JsonObject ?: continue
                    results.add(parsePlayerDoc(doc))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing player query: $column=$value", e)
        }
        return results
    }

    private fun parsePlayerDoc(doc: JsonObject): SupabasePlayerPayload {
        val rawMoney = doc.optLong("money", 200_000L)
        val rawLoan = doc.optLong("loan_amount", 0L)
        val rawDeposit = doc.optLong("deposit_balance", 0L)
        val rawTotalProfit = doc.optLong("total_profit", 0L)

        val parsedSavedTime = doc.optLong("last_saved_time", 0L)
        val effectiveSavedTime = if (parsedSavedTime > 0L) {
            parsedSavedTime
        } else {
            val updatedIso = doc.optString("updated_at", "")
            val epoch = parseIsoToEpochMs(updatedIso)
            if (epoch > 0L) epoch else System.currentTimeMillis()
        }

        val rawSaveElem = doc["raw_save_json"]
        val rawSaveStr = if (rawSaveElem != null && rawSaveElem !is JsonNull) {
            val s = if (rawSaveElem is JsonPrimitive) rawSaveElem.content else rawSaveElem.toString()
            if (s.isNotBlank() && s != "null") s else null
        } else null

        val rawSaveVersion = doc.optLong("save_version", 0L).let { if (it > 0L) it else doc.optLong("backup_version", 0L) }
        val effectiveSaveVersion = if (rawSaveVersion > 0L) {
            rawSaveVersion
        } else {
            if (rawSaveStr != null) {
                val parsed = safeParseJsonElement(rawSaveStr) as? JsonObject
                parsed?.optLong("save_version", 0L)?.takeIf { it > 0L }
                    ?: parsed?.optLong("backup_version", 0L)?.takeIf { it > 0L }
                    ?: parsed?.optLong("saveVersion", 0L)?.takeIf { it > 0L }
                    ?: 1L
            } else 1L
        }

        // Extract and combine research levels from research_levels column, research_levels_json, and individual tech_* columns
        val techKeys = listOf(
            "green_energy", "quality_control", "logistics", "automation",
            "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
            "aerospace", "heavy_industry", "consumer_goods", "petrochem",
            "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics",
            "global_finance", "cultural_heritage"
        )
        val extractedLevels = mutableMapOf<String, Int>()
        
        // 1. Direct tech_* columns in doc
        techKeys.forEach { tech ->
            val lvl = doc.optInt("tech_$tech", 0).let { if (it > 0) it else doc.optInt(tech, 0) }
            if (lvl > 0) {
                extractedLevels[tech] = lvl
                extractedLevels["tech_$tech"] = lvl
            }
        }

        // 2. research_levels column / json
        val rawRLevelsStr = doc.optJsonString("research_levels", "research_levels_json", "{}")
        if (rawRLevelsStr.isNotBlank() && rawRLevelsStr != "{}" && rawRLevelsStr != "null") {
            try {
                val parsed = AppJson.decodeFromString<Map<String, Int>>(rawRLevelsStr)
                parsed.forEach { (k, v) ->
                    val base = k.removePrefix("tech_")
                    if (v > 0) {
                        val maxLvl = maxOf(extractedLevels[base] ?: 0, v).coerceIn(0, 5)
                        extractedLevels[base] = maxLvl
                        extractedLevels["tech_$base"] = maxLvl
                    }
                }
            } catch (_: Exception) {
                try {
                    val elem = safeParseJsonElement(rawRLevelsStr) as? JsonObject
                    elem?.forEach { (k, vElem) ->
                        val lvl = (vElem as? JsonPrimitive)?.intOrNull ?: (vElem as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        if (lvl > 0) {
                            val base = k.removePrefix("tech_")
                            val maxLvl = maxOf(extractedLevels[base] ?: 0, lvl).coerceIn(0, 5)
                            extractedLevels[base] = maxLvl
                            extractedLevels["tech_$base"] = maxLvl
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        val finalResearchLevelsJson = if (extractedLevels.isNotEmpty()) AppJson.encodeToString(extractedLevels) else rawRLevelsStr

        // Extract active researches
        val extractedActiveResearches = mutableMapOf<String, Long>()
        val rawActiveResStr = doc.optJsonString("active_researches", "active_researches_json", "{}")
        if (rawActiveResStr.isNotBlank() && rawActiveResStr != "{}" && rawActiveResStr != "null") {
            try {
                val parsed = AppJson.decodeFromString<Map<String, Long>>(rawActiveResStr)
                parsed.forEach { (k, v) ->
                    val base = k.removePrefix("tech_")
                    if (v > 0L) extractedActiveResearches[base] = v
                }
            } catch (_: Exception) {}
        }
        val singleActiveKey = doc.optString("active_research_tech_key", "").removePrefix("tech_")
        val singleActiveEndMs = doc.optLong("research_end_time_ms", 0L)
        if (singleActiveKey.isNotBlank() && singleActiveEndMs > System.currentTimeMillis()) {
            extractedActiveResearches[singleActiveKey] = singleActiveEndMs
        }
        val finalActiveResearchesJson = if (extractedActiveResearches.isNotEmpty()) AppJson.encodeToString(extractedActiveResearches) else rawActiveResStr

        return SupabasePlayerPayload(
            id = doc.optString("id", ""),
            name = doc.optString("name", "Tüccar"),
            companyName = doc.optString("company_name", "Tüccar Holding"),
            money = rawMoney,
            loanAmount = rawLoan,
            depositBalance = rawDeposit,
            dailyIncome = doc.optLong("daily_income", 0L),
            dailyExpense = doc.optLong("daily_expense", 0L),
            totalProfit = rawTotalProfit,
            xp = doc.optInt("xp", 0),
            level = doc.optInt("level", 1),
            inventoryCapacity = run {
                val rawSaveObj = if (rawSaveStr != null) {
                    try { safeParseJsonElement(rawSaveStr) as? JsonObject } catch (_: Exception) { null }
                } else null

                val rawSaveCap = rawSaveObj?.optInt("inventory_capacity", 0)?.takeIf { it > 0 }
                    ?: rawSaveObj?.optInt("inventoryCapacity", 0)?.takeIf { it > 0 }
                    ?: 0
                val rawSaveWLvl = rawSaveObj?.optInt("warehouse_level", 0)?.takeIf { it > 0 }
                    ?: rawSaveObj?.optInt("warehouseLevel", 0)?.takeIf { it > 0 }
                    ?: 0
                val docCap = doc.optInt("inventory_capacity", 0).takeIf { it > 0 } ?: 5000
                val docWLvl = doc.optInt("warehouse_level", 0)

                val finalWLvl = maxOf(
                    rawSaveWLvl,
                    docWLvl,
                    if (rawSaveCap > 5000) 1 + ((rawSaveCap - 5000) / 2500) else 1,
                    if (docCap > 5000) 1 + ((docCap - 5000) / 2500) else 1,
                    1
                )
                maxOf(docCap, rawSaveCap, 5000 + (finalWLvl - 1) * 2500)
            },
            currentCity = migrateLegacyCity(doc.optString("current_city", "istanbul")),
            isVip = doc.optBoolean("is_vip", false),
            gems = doc.optInt("gems", 0),
            lastDailyRewardMs = doc.optLong("last_daily_reward_ms", 0L),
            loginStreak = doc.optInt("login_streak", 0),
            dollarBalance = doc.optLong("dollar_balance", 0L),
            dollarDepositBalance = doc.optLong("dollar_deposit_balance", 0L),
            dollarLoanAmount = doc.optLong("dollar_loan_amount", 0L),
            isOnlineRegistered = doc.optBoolean("is_online_registered", true),
            onlineEmail = doc.optString("online_email", ""),
            businessesJson = doc.optJsonString("businesses", "businesses_json", "[]"),
            inventoryJson = doc.optJsonString("inventory", "inventory_json", "[]"),
            activeDeliveriesJson = doc.optJsonString("active_deliveries", "active_deliveries_json", "[]"),
            activeProductionsJson = doc.optJsonString("active_productions", "active_productions_json", "[]"),
            managersJson = doc.optJsonString("managers", "managers_json", "[]"),
            dailyQuestStateJson = doc.optJsonString("daily_quest_state", "daily_quest_state_json", "{}"),
            activeResearchesJson = finalActiveResearchesJson,
            researchLevelsJson = finalResearchLevelsJson,
            guildSharesJson = doc.optJsonString("guild_shares", "guild_shares_json", "{}"),
            guildBuyPricesJson = doc.optJsonString("guild_buy_prices", "guild_buy_prices_json", "{}"),
            rawSaveJson = rawSaveStr,
            lastSavedTime = effectiveSavedTime,
            saveVersion = effectiveSaveVersion
        )
    }

    /**
     * Verilen playerId veya email'e göre oyuncunun kayıtlı JSON verisini (EconomicSnapshot yapısı)
     * Supabase veritabanından çeker. Hata durumunda (ağ kopması vb.) null döndürür.
     */
    suspend fun fetchPlayerSaveData(uid: String): String? = withContext(Dispatchers.IO) {
        if (uid.isBlank() || uid == "local_player" || uid == "p_local" || uid == "misafir_tuccar") {
            return@withContext null
        }
        try {
            val payload = fetchPlayerFromSupabase(uid) ?: return@withContext null
            payload.toSaveJson()
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching player save data from Supabase", e)
            null
        }
    }

    /**
     * İstenmeyen veya geçersiz oyuncu kaydını (örn. local_player) Supabase veritabanından siler.
     */
    suspend fun deletePlayerFromSupabase(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedUid = java.net.URLEncoder.encode(uid, "UTF-8")
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?id=eq.$encodedUid")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting player $uid from Supabase", e)
            false
        }
    }

    /**
     * Tekil Borsa Ürün Fiyat ve Stok Değişimini Supabase'e Tek İstekte Gönderir (Single Upsert)
     */
    suspend fun syncSingleMarketPrice(price: MarketPriceEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("item_id", price.itemId)
                put("item_name", price.itemId)
                put("base_price", price.price)
                put("current_price", price.price)
                put("stock", price.borsaStock)
                put("borsa_stock", price.borsaStock)
                put("origin_country", price.originCountry)
                put("is_usd", price.isUsd)
                put("updated_at", getCurrentIsoTimestamp())
            }
            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/market_prices?on_conflict=item_id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing single market price for ${price.itemId} to Supabase", e)
            false
        }
    }

    /**
     * Toplu Borsa Fiyatlarını Supabase'e Tek İstekte (Batch Upsert: Prefer resolution=merge-duplicates) Gönderir
     */
    suspend fun syncMarketPricesBatch(prices: List<MarketPriceEntity>): Boolean = withContext(Dispatchers.IO) {
        if (prices.isEmpty()) return@withContext true
        try {
            val sanitized = sanitizeMarketPrices(prices)
            val jsonArray = buildJsonArray {
                sanitized.forEach { entity ->
                    add(buildJsonObject {
                        put("item_id", entity.itemId)
                        put("item_name", entity.itemId)
                        put("base_price", entity.price)
                        put("current_price", entity.price)
                        put("stock", entity.borsaStock)
                        put("borsa_stock", entity.borsaStock)
                        put("origin_country", entity.originCountry)
                        put("is_usd", entity.isUsd)
                        put("updated_at", getCurrentIsoTimestamp())
                    })
                }
            }

            val requestBody = jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/market_prices?on_conflict=item_id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing market prices batch to Supabase", e)
            false
        }
    }

    /**
     * Borsa Fiyatlarını Supabase'e günceller (Tek İstekli Batch Upsert delegasyonu)
     */
    suspend fun syncMarketPrices(prices: List<MarketPriceEntity>): Boolean {
        return syncMarketPricesBatch(prices)
    }

    /**
     * Tekil Borsa Ürün Stok ve Fiyatını Anlık Olarak Supabase'e İşler (Tek İstek)
     */
    suspend fun updateSingleMarketPriceInSupabase(
        itemId: String,
        newStock: Long,
        newPrice: Long,
        originCountry: String = "Türkiye"
    ): Boolean {
        return syncSingleMarketPrice(
            MarketPriceEntity(
                itemId = itemId,
                originCountry = originCountry,
                originCityId = "istanbul",
                price = newPrice,
                borsaStock = newStock,
                isUsd = false
            )
        )
    }

    /**
     * Konsorsiyum / Mega Proje katkılarını ve IPO / Halka Arz Şirketlerini Supabase'e günceller
     */
    suspend fun syncGuildToSupabase(guild: GuildGroup): Boolean = withContext(Dispatchers.IO) {
        try {
            val reqsElem = anyToJsonElement(guild.megaProjectRequirements)
            val contribsElem = anyToJsonElement(guild.megaProjectContributions)

            val jsonObj = buildJsonObject {
                put("id", guild.id)
                put("name", guild.name)
                put("leader_name", guild.leaderName)
                put("member_count", guild.memberCount)
                put("mega_project_title", guild.megaProjectTitle)
                put("mega_project_target", guild.megaProjectTarget)
                put("mega_project_current", guild.megaProjectCurrent)
                put("mega_project_requirements", reqsElem)
                put("mega_project_contributions", contribsElem)
                put("perk_description", guild.perkDescription)
                put("bank_balance", guild.bankBalance)
                put("is_ipo_active", guild.isIpoActive)
                put("public_share_percent", guild.publicSharePercent)
                if (guild.targetProductName.isNotBlank()) put("target_product_name", guild.targetProductName)
                if (guild.targetProductId.isNotBlank()) put("target_product_id", guild.targetProductId)
                put("warehouse_stock", guild.warehouseStock)
                put("total_items_produced", guild.totalItemsProduced)
                put("unit_batch_price", guild.unitBatchPrice)
                if (guild.currentStage.isNotBlank()) put("current_stage", guild.currentStage)
                if (guild.rawProjectJson.isNotBlank()) put("raw_project_json", guild.rawProjectJson)
            }

            val requestBody = jsonObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val req1 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_guilds")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()
            executeAndLog(req1, "global_guilds_upsert")

            val req2 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/guilds")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()
            executeAndLog(req2, "guilds_upsert")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing guild to Supabase", e)
            false
        }
    }

    /**
     * Konsorsiyum (MegaProject) verilerini Supabase global_guilds ve guilds tablolarına senkronize eder.
     * Tüm 4 tedarikçi slotu, aşamaları, kotaları ve teslimatları JSON olarak PostgreSQL'e yazar.
     */
    suspend fun syncMegaProjectToSupabase(megaProject: com.example.data.MegaProject): Boolean = withContext(Dispatchers.IO) {
        try {
            val rawJson = anyToJsonElement(megaProject.toMap()).toString()

            val reqsMap = mutableMapOf<String, Int>()
            val contribsMap = mutableMapOf<String, Int>()
            megaProject.slots.forEach { slot ->
                reqsMap[slot.productId] = slot.quantityRequired
                contribsMap[slot.productId] = slot.quantityDelivered
            }

            val participantIds = (megaProject.slots.mapNotNull { it.assignedPartnerId }.filter { it.isNotBlank() } + megaProject.leaderPlayerId).distinct()
            val memberCount = participantIds.size.coerceAtLeast(1)

            val guild = GuildGroup(
                id = megaProject.id,
                name = megaProject.consortiumName,
                leaderName = megaProject.leaderPlayerName,
                memberCount = memberCount,
                megaProjectTitle = megaProject.targetProductName,
                megaProjectTarget = megaProject.totalProjectValue,
                megaProjectCurrent = megaProject.slots.sumOf { it.costContributionValue },
                megaProjectRequirements = reqsMap,
                megaProjectContributions = contribsMap,
                perkDescription = "Marka: ${megaProject.brandName} • Marka Çarpanı: %${((megaProject.brandMultiplier - 1f) * 100).toInt().coerceAtLeast(0)}",
                bankBalance = (megaProject.warehouseStock.toLong() * megaProject.unitBatchPrice).coerceAtLeast(0L),
                isIpoActive = megaProject.salesChannel == com.example.data.ConsortiumSalesChannel.BORSA,
                publicSharePercent = 20,
                targetProductName = megaProject.targetProductName,
                targetProductId = megaProject.targetProductId,
                warehouseStock = megaProject.warehouseStock,
                totalItemsProduced = megaProject.totalItemsProduced,
                unitBatchPrice = megaProject.unitBatchPrice,
                currentStage = megaProject.currentStage.name,
                rawProjectJson = rawJson
            )

            syncGuildToSupabase(guild)
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing MegaProject to Supabase: ${megaProject.id}", e)
            false
        }
    }

    /**
     * Feshedilen veya silinen bir Mega Proje konsorsiyumunu Supabase veritabanından kalıcı olarak kaldırır.
     */
    suspend fun deleteMegaProjectFromSupabase(projectId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req1 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_guilds?id=eq.$projectId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()
            executeAndLog(req1, "global_guilds_delete")

            val req2 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/guilds?id=eq.$projectId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()
            executeAndLog(req2, "guilds_delete")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting MegaProject from Supabase: $projectId", e)
            false
        }
    }

    /**
     * Konsorsiyum verilerini ve IPO Halka Arz Şirketlerini Supabase'den çeker
     */
    suspend fun fetchGuildsFromSupabase(): List<GuildGroup>? = withContext(Dispatchers.IO) {
        try {
            val columns = "id,name,leader_name,member_count,mega_project_title,mega_project_target,mega_project_current,mega_project_requirements,mega_project_contributions,perk_description,bank_balance,is_ipo_active,public_share_percent,target_product_name,target_product_id,warehouse_stock,total_items_produced,unit_batch_price,current_stage,raw_project_json"
            var list = queryGuildsFromEndpoint("$SUPABASE_URL/rest/v1/global_guilds?select=$columns")
            if (list.isNullOrEmpty()) {
                list = queryGuildsFromEndpoint("$SUPABASE_URL/rest/v1/guilds?select=$columns")
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching guilds from Supabase", e)
            null
        }
    }

    private fun queryGuildsFromEndpoint(url: String): List<GuildGroup>? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: ""
                val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return null
                val resultList = mutableListOf<GuildGroup>()
                for (i in 0 until jsonArray.size) {
                    val doc = jsonArray[i] as? JsonObject ?: continue
                    val reqsObj = doc.optJsonObject("mega_project_requirements") ?: JsonObject(emptyMap())
                    val contribsObj = doc.optJsonObject("mega_project_contributions") ?: JsonObject(emptyMap())

                    val reqsMap = mutableMapOf<String, Int>()
                    reqsObj.forEach { (k, v) ->
                        val num = (v as? JsonPrimitive)?.longOrNull?.toInt() ?: (v as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        reqsMap[k] = num
                    }

                    val contribsMap = mutableMapOf<String, Int>()
                    contribsObj.forEach { (k, v) ->
                        val num = (v as? JsonPrimitive)?.longOrNull?.toInt() ?: (v as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        contribsMap[k] = num
                    }

                    resultList.add(
                        GuildGroup(
                            id = doc.optString("id", "guild-$i"),
                            name = doc.optString("name", "Konsorsiyum"),
                            leaderName = doc.optString("leader_name", ""),
                            memberCount = doc.optInt("member_count", 1),
                            megaProjectTitle = doc.optString("mega_project_title", ""),
                            megaProjectTarget = doc.optLong("mega_project_target", 0L),
                            megaProjectCurrent = doc.optLong("mega_project_current", 0L),
                            megaProjectRequirements = reqsMap,
                            megaProjectContributions = contribsMap,
                            perkDescription = doc.optString("perk_description", ""),
                            isJoined = false,
                            bankBalance = doc.optLong("bank_balance", 0L),
                            isIpoActive = doc.optBoolean("is_ipo_active", false),
                            publicSharePercent = doc.optInt("public_share_percent", 20),
                            targetProductName = doc.optString("target_product_name", ""),
                            targetProductId = doc.optString("target_product_id", ""),
                            warehouseStock = doc.optInt("warehouse_stock", 0),
                            totalItemsProduced = doc.optInt("total_items_produced", 0),
                            unitBatchPrice = doc.optLong("unit_batch_price", 0L),
                            currentStage = doc.optString("current_stage", ""),
                            rawProjectJson = doc.optString("raw_project_json", "")
                        )
                    )
                }
                if (resultList.isNotEmpty()) resultList else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying endpoint $url", e)
            null
        }
    }

    /**
     * Converts a Supabase GuildGroup entity into a fully functional MegaProject.
     */
    fun guildToMegaProject(guild: GuildGroup): com.example.data.MegaProject? {
        if (guild.rawProjectJson.isNotBlank() && guild.rawProjectJson != "null" && guild.rawProjectJson != "{}") {
            try {
                val element = safeParseJsonElement(guild.rawProjectJson)
                if (element != null) {
                    val map = jsonElementToAny(element) as? Map<String, Any?>
                    if (map != null) {
                        return com.example.data.MegaProject.fromMap(map)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing rawProjectJson for guild: ${guild.id}", e)
            }
        }
        val defaultBot = com.example.data.ConsortiumBotRegistry.getBotMegaProject(guild.id)
        if (defaultBot != null) {
            return defaultBot
        }
        return try {
            val stageVal = try {
                com.example.data.MegaProjectStage.valueOf(guild.currentStage)
            } catch (_: Exception) {
                com.example.data.MegaProjectStage.STAGE_1_BODY
            }
            val slotsList = guild.megaProjectRequirements.map { (prodId, reqQty) ->
                val delQty = guild.megaProjectContributions[prodId] ?: 0
                val prod = Product.values().find { it.id == prodId }
                com.example.data.ConsortiumSupplierSlot(
                    slotId = "slot_$prodId",
                    productId = prodId,
                    productName = prod?.getDisplayName() ?: prodId,
                    quantityRequired = reqQty,
                    quantityDelivered = delQty,
                    stage = stageVal,
                    costContributionValue = (reqQty * 1000L)
                )
            }
            com.example.data.MegaProject(
                id = guild.id,
                consortiumName = guild.name,
                brandName = guild.name,
                leaderPlayerId = guild.leaderName,
                leaderPlayerName = guild.leaderName,
                targetProductName = guild.targetProductName.ifBlank { guild.megaProjectTitle },
                targetProductId = guild.targetProductId,
                slots = slotsList,
                currentStage = stageVal,
                totalProjectValue = guild.megaProjectTarget,
                warehouseStock = guild.warehouseStock,
                totalItemsProduced = guild.totalItemsProduced,
                salesChannel = if (guild.isIpoActive) com.example.data.ConsortiumSalesChannel.BORSA else com.example.data.ConsortiumSalesChannel.PAZAR,
                cityId = guild.cityId
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Borsa Fiyatlarını Supabase'den çeker
     */
    suspend fun fetchMarketPrices(): List<MarketPriceEntity>? = withContext(Dispatchers.IO) {
        try {
            val marketCols = "item_id,id,symbol,current_price,base_price,borsa_stock,stock,origin_country,origin_city_id,is_usd"
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/market_prices?select=$marketCols")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val body = response.body?.string() ?: ""
                val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
                val resultList = mutableListOf<MarketPriceEntity>()
                var hasLegacyValues = false
                for (i in 0 until jsonArray.size) {
                    val item = jsonArray[i] as? JsonObject ?: continue
                    val id = item.optString("item_id").ifBlank { item.optString("id").ifBlank { item.optString("symbol") } }
                    val price = item.optLong("current_price", item.optLong("base_price", 0L))
                    val rawStock = item.optLong("borsa_stock", item.optLong("stock", MacroEconomyEngine.DEFAULT_BORSA_STOCK))
                    val stock = if (rawStock <= 0L || rawStock == 999_999_999L || rawStock == 50_000L || rawStock == 5_000L) {
                        hasLegacyValues = true
                        MacroEconomyEngine.DEFAULT_BORSA_STOCK
                    } else {
                        rawStock
                    }
                    val originCountry = item.optString("origin_country", "Türkiye")
                    val originCityId = item.optString("origin_city_id", "istanbul")
                    val isUsd = item.optBoolean("is_usd", false)
                    if (id.isNotBlank() && price > 0L) {
                        resultList.add(MarketPriceEntity(
                            itemId = id, 
                            originCountry = originCountry,
                            originCityId = originCityId,
                            price = price, 
                            borsaStock = stock,
                            isUsd = isUsd
                        ))
                    }
                }
                val sanitized = sanitizeMarketPrices(resultList)
                if (hasLegacyValues || resultList.size < Product.values().size) {
                    // Supabase'deki eski kalıntıları ve eksik ürünleri standart 999.999L stok ile Supabase'e geri yaz
                    syncMarketPrices(sanitized)
                }
                if (sanitized.isNotEmpty()) sanitized else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching market prices from Supabase", e)
            null
        }
    }

    data class GlobalMarketBundle(
        val listings: List<com.example.data.MarketListing> = emptyList(),
        val futuresContracts: List<com.example.data.FuturesContract> = emptyList(),
        val buyOrders: List<com.example.data.BuyOrder> = emptyList()
    )

    suspend fun fetchGlobalMarketBundleFromSupabase(): GlobalMarketBundle? = withContext(Dispatchers.IO) {
        try {
            val marketCols = "id,item_id,product_id,product_name,seller_id,seller_name,seller_company,quantity,price_per_unit,city_id,type,created_at,delivery_time_ms,buyer_id,buyer_name,target_price,max_budget,contract_status"
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market?select=$marketCols")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: ""
            }

            val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
            val listings = mutableListOf<com.example.data.MarketListing>()
            val futures = mutableListOf<com.example.data.FuturesContract>()
            val buyOrders = mutableListOf<com.example.data.BuyOrder>()

            for (i in 0 until jsonArray.size) {
                val item = jsonArray[i] as? JsonObject ?: continue
                val id = item.optString("id", "")
                val sellerId = item.optString("seller_id", "")
                val sellerName = item.optString("seller_name", "")
                val itemId = item.optString("item_id", "")
                val quantity = item.optInt("quantity", 0)
                val pricePerUnit = item.optLong("price_per_unit", 0L)
                val city = item.optString("city", "")

                val createdAtStr = item.optString("created_at")
                val parsedCreatedAt = if (createdAtStr.isNotBlank()) {
                    try {
                        java.time.Instant.parse(createdAtStr).toEpochMilli()
                    } catch (e: Exception) { System.currentTimeMillis() }
                } else System.currentTimeMillis()

                val qualityLevel = item.optInt("quality_level", 0).let { q ->
                    if (q in 1..5) q else com.example.data.ItemQuality.extractQuality(itemId).stars
                }
                val qualityTier = item.optString("quality_tier").takeIf { it.isNotBlank() }
                    ?: com.example.data.ItemQuality.fromStars(qualityLevel).label

                if (id.startsWith("fc_") || sellerName.startsWith("FUTURES:")) {
                    // Vadeli Sözleşme
                    var durationDays = 30
                    var creatorName = sellerName
                    if (sellerName.startsWith("FUTURES:")) {
                        val parts = sellerName.split(":", limit = 3)
                        if (parts.size >= 3) {
                            durationDays = parts[1].toIntOrNull() ?: 30
                            creatorName = parts[2]
                        } else if (parts.size == 2) {
                            creatorName = parts[1]
                        }
                    }
                    futures.add(
                        com.example.data.FuturesContract(
                            id = id,
                            creatorId = sellerId,
                            creatorName = creatorName,
                            itemId = itemId,
                            quantity = quantity,
                            lockedPricePerUnit = pricePerUnit,
                            durationDays = durationDays,
                            cityId = city,
                            qualityLevel = qualityLevel,
                            createdAt = parsedCreatedAt
                        )
                    )
                } else if (id.startsWith("bo_") || sellerName.startsWith("BUY_ORDER:")) {
                    // Tedarik Talebi (Alım Emri)
                    val buyerName = if (sellerName.startsWith("BUY_ORDER:")) {
                        sellerName.removePrefix("BUY_ORDER:")
                    } else sellerName

                    val minQ = item.optInt("min_quality_level", qualityLevel)

                    buyOrders.add(
                        com.example.data.BuyOrder(
                            id = id,
                            buyerId = sellerId,
                            buyerName = buyerName,
                            itemId = itemId,
                            quantity = quantity,
                            pricePerUnit = pricePerUnit,
                            destinationCityId = city,
                            qualityLevel = qualityLevel,
                            minQualityLevel = minQ,
                            createdAt = parsedCreatedAt
                        )
                    )
                } else {
                    // Normal Pazar Satış İlanı
                    listings.add(
                        com.example.data.MarketListing(
                            id = id,
                            sellerId = sellerId,
                            sellerName = sellerName,
                            itemId = itemId,
                            quantity = quantity,
                            pricePerUnit = pricePerUnit,
                            originCityId = city,
                            qualityLevel = qualityLevel,
                            qualityTier = qualityTier,
                            createdAt = parsedCreatedAt
                        )
                    )
                }
            }
            GlobalMarketBundle(listings, futures, buyOrders)
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching global market bundle from Supabase", e)
            null
        }
    }

    suspend fun fetchGlobalMarketFromSupabase(): List<com.example.data.MarketListing>? = withContext(Dispatchers.IO) {
        val bundle = fetchGlobalMarketBundleFromSupabase()
        bundle?.listings
    }

    suspend fun syncMarketListingToSupabase(listing: com.example.data.MarketListing): Boolean = withContext(Dispatchers.IO) {
        if (listing.isBotListing || listing.sellerId.startsWith("BOT-") || listing.id.startsWith("BOT_LISTING_") || listing.sellerId == "BOT_AUTO") {
            return@withContext true
        }
        try {
            val jsonObject = buildJsonObject {
                put("id", listing.id)
                put("seller_id", listing.sellerId)
                put("seller_name", listing.sellerName)
                put("item_id", listing.itemId)
                put("quantity", listing.quantity)
                put("price_per_unit", listing.pricePerUnit)
                put("city", listing.originCityId)
            }
            val reqBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(reqBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "syncMarketListingToSupabase failed: code=${response.code} body=$errBody")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing market listing to Supabase", e)
            false
        }
    }

    suspend fun deleteMarketListingFromSupabase(listingId: String): Boolean = withContext(Dispatchers.IO) {
        if (listingId.startsWith("BOT_LISTING_") || listingId.startsWith("BOT-")) {
            return@withContext true
        }
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market?id=eq.$listingId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting market listing from Supabase", e)
            false
        }
    }

    suspend fun syncFuturesContractToSupabase(contract: com.example.data.FuturesContract): Boolean = withContext(Dispatchers.IO) {
        try {
            val recordId = if (contract.id.startsWith("fc_")) contract.id else "fc_${contract.id}"
            val encodedSellerName = "FUTURES:${contract.durationDays}:${contract.creatorName}"
            val jsonObject = buildJsonObject {
                put("id", recordId)
                put("seller_id", contract.creatorId)
                put("seller_name", encodedSellerName)
                put("item_id", contract.itemId)
                put("quantity", contract.quantity)
                put("price_per_unit", contract.lockedPricePerUnit)
                put("city", contract.cityId)
            }
            val reqBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(reqBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing futures contract to Supabase", e)
            false
        }
    }

    suspend fun deleteFuturesContractFromSupabase(contractId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val idParam = if (contractId.startsWith("fc_")) {
                "id=in.($contractId,${contractId.removePrefix("fc_")})"
            } else {
                "id=in.($contractId,fc_$contractId)"
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market?$idParam")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting futures contract from Supabase", e)
            false
        }
    }

    suspend fun syncBuyOrderToSupabase(order: com.example.data.BuyOrder): Boolean = withContext(Dispatchers.IO) {
        try {
            val recordId = if (order.id.startsWith("bo_")) order.id else "bo_${order.id}"
            val encodedBuyerName = "BUY_ORDER:${order.buyerName}"
            val jsonObject = buildJsonObject {
                put("id", recordId)
                put("seller_id", order.buyerId)
                put("seller_name", encodedBuyerName)
                put("item_id", order.itemId)
                put("quantity", order.quantity)
                put("price_per_unit", order.pricePerUnit)
                put("city", order.destinationCityId)
            }
            val reqBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(reqBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing buy order to Supabase", e)
            false
        }
    }

    suspend fun deleteBuyOrderFromSupabase(orderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val idParam = if (orderId.startsWith("bo_")) {
                "id=in.($orderId,${orderId.removePrefix("bo_")})"
            } else {
                "id=in.($orderId,bo_$orderId)"
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market?$idParam")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting buy order from Supabase", e)
            false
        }
    }

    /**
     * Supabase'den En Zengin Oyuncular Sıralamasını (Liderlik Tablosunu) çeker.
     */
    suspend fun fetchLeaderboardFromSupabase(limit: Int = 50): List<OnlinePlayer>? = withContext(Dispatchers.IO) {
        try {
            val leaderboardCols = "id,name,company_name,money,deposit_balance,loan_amount,level,is_online_registered,current_city,total_profit,xp,updated_at"
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?select=$leaderboardCols&order=money.desc&limit=$limit")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchLeaderboardFromSupabase error response code: ${response.code}")
                    return@withContext null
                }
                response.body?.string() ?: ""
            }

            val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
            val resultList = mutableListOf<OnlinePlayer>()
            for (i in 0 until jsonArray.size) {
                val doc = jsonArray[i] as? JsonObject ?: continue
                val id = doc.optString("id", "")
                val name = doc.optString("name", "Tüccar")
                val botIds = setOf("BOT-KAYA-01", "BOT-NOVA-02", "BOT-TOROS-03", "BOT-EGE-04", "BOT-AVRASYA-05", "BOT-ANADOLU-05")
                val botNames = setOf("Selim Kaya", "Dr. Aylin Soylu", "Burak Demirci", "Zehra Aydın", "Hakan Erkin", "Defne Aras", "Kaan Yıldırım")

                // Only include authentic registered players who signed in (skip offline/guest entries and bot entries)
                if (id.isBlank() || id == "local" || id == "misafir_tuccar" || id.startsWith("guest", ignoreCase = true) || id.startsWith("BOT-", ignoreCase = true) || id.startsWith("BOT_", ignoreCase = true) || id.contains("bot", ignoreCase = true) || id in botIds || name in botNames) {
                    continue
                }

                val money = doc.optLong("money", 0L)
                val deposit = doc.optLong("deposit_balance", 0L)
                val loan = doc.optLong("loan_amount", 0L)
                val netWorth = (money + deposit - loan).coerceAtLeast(0L)
                val lvl = doc.optInt("level", 1)
                val xpVal = doc.optInt("xp", 0)
                val totProfit = doc.optLong("total_profit", 0L)
                val monthlyScore = if (totProfit > 0L) (totProfit * 0.20f).toLong() else (netWorth * 0.20f).toLong().coerceAtLeast(100L)

                val compName = doc.optString("company_name", "").ifBlank { "${name} Holding" }
                val currentCity = doc.optString("current_city", "istanbul")

                resultList.add(
                    OnlinePlayer(
                        id = id,
                        name = name,
                        companyName = compName,
                        netWorth = netWorth,
                        city = currentCity,
                        level = lvl,
                        isOnline = true,
                        badge = if (lvl > 15) "CEO" else if (lvl > 10) "LİDER" else "TÜCCAR",
                        bankBalance = deposit,
                        monthlyScore = monthlyScore,
                        xp = xpVal,
                        facilities = emptyList()
                    )
                )
            }
            resultList
                .groupBy { player ->
                    val cleanId = player.id.lowercase().trim().removeSuffix("_backup").removePrefix("vault_").replace(".", "_")
                    val emailKey = if (cleanId.contains("@")) cleanId.substringBefore("@") else ""
                    val cleanName = player.name.lowercase().trim().replace(" ", "").replace("_", "")
                    when {
                        emailKey.isNotBlank() -> "email_$emailKey"
                        cleanName.isNotBlank() && cleanName != "tüccar" && cleanName != "tuccar" -> "name_$cleanName"
                        else -> "id_$cleanId"
                    }
                }
                .map { (_, duplicates) ->
                    duplicates.maxByOrNull { it.netWorth }!!
                }
                .sortedByDescending { it.netWorth }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching leaderboard from Supabase", e)
            null
        }
    }

    /**
     * Supabase HTTP başlığından hilesiz ortak sunucu zamanını çeker.
     */
    suspend fun fetchServerTimeMsFromSupabase(): Long? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/")
                .addHeader("apikey", SUPABASE_KEY)
                .head()
                .build()

            val dateHeader = httpClient.newCall(request).execute().use { response ->
                response.header("Date")
            }

            if (!dateHeader.isNullOrBlank()) {
                val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
                val date = format.parse(dateHeader)
                date?.time
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching server time from Supabase", e)
            null
        }
    }

    /**
     * Küresel makroekonomik verileri Supabase'e senkronize eder.
     */
    suspend fun syncMacroStateToSupabase(
        macroState: MacroEconomyState,
        loanRate: Float? = null,
        depositRate: Float? = null,
        centralBankLoanRate: Float? = null,
        centralBankDepositRate: Float? = null,
        usdTryRate: Double = 50.0,
        forexChange24h: Double = 0.45
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val effectiveLoanRate = loanRate ?: centralBankLoanRate ?: 0.22f
            val effectiveDepositRate = depositRate ?: centralBankDepositRate ?: 0.10f
            val jsonObj = buildJsonObject {
                put("id", "current_macro_state")
                put("cycle", macroState.cycle.name)
                put("global_inflation_rate", macroState.globalInflationRate)
                put("central_bank_interest_rate", macroState.centralBankInterestRate)
                put("central_bank_loan_rate", effectiveLoanRate)
                put("central_bank_deposit_rate", effectiveDepositRate)
                put("market_liquidity_multiplier", macroState.marketLiquidityMultiplier)
                put("pe_ratio_base", macroState.peRatioBase)
                put("usd_try_rate", usdTryRate)
                put("forex_change_24h", forexChange24h)
                put("forex_source", "Merkez Bankası & Canlı Forex API (09:00)")
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = jsonObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_economy")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing macro state to Supabase global_economy", e)
            false
        }
    }

    suspend fun fetchMacroStateFromSupabase(): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_economy?id=eq.current_macro_state&select=*")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val responseBody = httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
            if (!responseBody.isNullOrBlank()) {
                val array = safeParseJsonElement(responseBody) as? JsonArray
                if (array != null && array.isNotEmpty()) {
                    val obj = array[0] as? JsonObject
                    if (obj != null) {
                        val map = mutableMapOf<String, Any>()
                        map["cycle"] = obj.optString("cycle", "RECOVERY")
                        map["globalInflationRate"] = obj.optDouble("global_inflation_rate", 0.08)
                        map["centralBankInterestRate"] = obj.optDouble("central_bank_interest_rate", 0.12)
                        map["centralBankLoanRate"] = obj.optDouble("central_bank_loan_rate", 0.22)
                        map["centralBankDepositRate"] = obj.optDouble("central_bank_deposit_rate", 0.10)
                        map["marketLiquidityMultiplier"] = obj.optDouble("market_liquidity_multiplier", 1.0)
                        map["peRatioBase"] = obj.optDouble("pe_ratio_base", 10.0)
                        map["usdTryRate"] = obj.optDouble("usd_try_rate", 50.0)
                        map["forexChange24h"] = obj.optDouble("forex_change_24h", 0.45)
                        map["forexUpdatedAt"] = obj.optString("updated_at", "")
                        return@withContext map
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching macro state from Supabase", e)
            null
        }
    }

    // =========================================================================
    // 1-OF-1 GLOBAL MUSEUM ARTIFACTS REGISTRY & REAL PLAYER AUCTIONS
    // =========================================================================

    /**
     * Supabase veritabanından 1-of-1 global antika eser sahiplik listesini çeker.
     */
    suspend fun fetchMuseumRegistryFromSupabase(): List<MuseumArtifactOwnershipEntity>? = withContext(Dispatchers.IO) {
        try {
            val museumCols = "artifact_id,owner_id,owner_name,status,active_auction_id,last_price,updated_at_ms"
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_artifacts?select=$museumCols")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: ""
            }

            val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
            val list = mutableListOf<MuseumArtifactOwnershipEntity>()
            for (i in 0 until jsonArray.size) {
                val obj = jsonArray[i] as? JsonObject ?: continue
                val ownerIdRaw = obj.optString("owner_id")
                val activeAuctionIdRaw = obj.optString("active_auction_id")

                list.add(
                    MuseumArtifactOwnershipEntity(
                        artifactId = obj.optString("artifact_id"),
                        ownerId = ownerIdRaw.ifBlank { null },
                        ownerName = obj.optString("owner_name", "T.C. Kültür ve Turizm Bakanlığı"),
                        status = obj.optString("status", "UNCLAIMED_TREASURY"),
                        activeAuctionId = activeAuctionIdRaw.ifBlank { null },
                        lastPrice = obj.optLong("last_price", 0L),
                        updatedAtMs = obj.optLong("updated_at_ms", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching museum registry from Supabase", e)
            null
        }
    }

    suspend fun fetchMuseumArtifactsFromSupabase(): List<MuseumArtifactOwnershipEntity> {
        return fetchMuseumRegistryFromSupabase() ?: emptyList()
    }

    suspend fun syncMuseumArtifactToSupabase(artifact: MuseumArtifactOwnershipEntity): Boolean {
        return syncMuseumArtifactOwnershipToSupabase(
            artifactId = artifact.artifactId,
            ownerId = artifact.ownerId,
            ownerName = artifact.ownerName,
            status = artifact.status,
            activeAuctionId = artifact.activeAuctionId,
            lastPrice = artifact.lastPrice
        )
    }

    /**
     * Eser sahipliğini ve durumunu Supabase'e günceller (Upsert).
     */
    suspend fun syncMuseumArtifactOwnershipToSupabase(
        artifactId: String,
        ownerId: String?,
        ownerName: String,
        status: String,
        activeAuctionId: String? = null,
        lastPrice: Long = 0L
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObj = buildJsonObject {
                put("artifact_id", artifactId)
                if (ownerId != null) put("owner_id", ownerId) else put("owner_id", JsonNull)
                put("owner_name", ownerName)
                put("status", status)
                if (activeAuctionId != null) put("active_auction_id", activeAuctionId) else put("active_auction_id", JsonNull)
                put("last_price", lastPrice)
                put("updated_at_ms", System.currentTimeMillis())
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = jsonObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_artifacts")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing museum artifact ownership to Supabase", e)
            false
        }
    }

    /**
     * Supabase veritabanından aktif gerçek oyuncu müzayede ilanlarını çeker.
     */
    suspend fun fetchMuseumAuctionsFromSupabase(): List<MuseumAuctionEntity>? = withContext(Dispatchers.IO) {
        try {
            val auctionCols = "id,artifact_id,seller_id,seller_name,is_player_seller,starting_bid,current_highest_bid,current_highest_bidder_id,current_highest_bidder_name,buyout_price,ends_at_ms,bid_count,created_at_ms,is_settled"
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions?select=$auctionCols&is_settled=eq.false")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: ""
            }

            val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
            val list = mutableListOf<MuseumAuctionEntity>()
            for (i in 0 until jsonArray.size) {
                val obj = jsonArray[i] as? JsonObject ?: continue
                list.add(
                    MuseumAuctionEntity(
                        id = obj.optString("id"),
                        artifactId = obj.optString("artifact_id"),
                        sellerId = obj.optString("seller_id", "treasury"),
                        sellerName = obj.optString("seller_name", "T.C. Kültür ve Turizm Bakanlığı"),
                        isPlayerSeller = obj.optBoolean("is_player_seller", false),
                        startingBid = obj.optLong("starting_bid", 0L),
                        currentHighestBid = obj.optLong("current_highest_bid", 0L),
                        currentHighestBidderId = obj.optString("current_highest_bidder_id", ""),
                        currentHighestBidderName = obj.optString("current_highest_bidder_name", ""),
                        buyoutPrice = obj.optLong("buyout_price", 0L),
                        endsAtMs = obj.optLong("ends_at_ms", 0L),
                        bidCount = obj.optInt("bid_count", 0),
                        createdAtMs = obj.optLong("created_at_ms", System.currentTimeMillis()),
                        isSettled = obj.optBoolean("is_settled", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching museum auctions from Supabase", e)
            null
        }
    }

    suspend fun getMuseumAuctionByIdFromSupabase(auctionId: String): MuseumAuctionEntity? = withContext(Dispatchers.IO) {
        try {
            val auctionCols = "id,artifact_id,seller_id,seller_name,is_player_seller,starting_bid,current_highest_bid,current_highest_bidder_id,current_highest_bidder_name,buyout_price,ends_at_ms,bid_count,created_at_ms,is_settled"
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions?id=eq.$auctionId&select=$auctionCols")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: ""
            }

            val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
            if (jsonArray.isEmpty()) return@withContext null

            val obj = jsonArray[0] as? JsonObject ?: return@withContext null
            MuseumAuctionEntity(
                id = obj.optString("id"),
                artifactId = obj.optString("artifact_id"),
                sellerId = obj.optString("seller_id", "treasury"),
                sellerName = obj.optString("seller_name", "T.C. Kültür ve Turizm Bakanlığı"),
                isPlayerSeller = obj.optBoolean("is_player_seller", false),
                startingBid = obj.optLong("starting_bid", 0L),
                currentHighestBid = obj.optLong("current_highest_bid", 0L),
                currentHighestBidderId = obj.optString("current_highest_bidder_id", ""),
                currentHighestBidderName = obj.optString("current_highest_bidder_name", ""),
                buyoutPrice = obj.optLong("buyout_price", 0L),
                endsAtMs = obj.optLong("ends_at_ms", 0L),
                bidCount = obj.optInt("bid_count", 0),
                createdAtMs = obj.optLong("created_at_ms", System.currentTimeMillis()),
                isSettled = obj.optBoolean("is_settled", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching museum auction by id from Supabase", e)
            null
        }
    }

    /**
     * Yeni bir müzayede ilanını Supabase'e ekler veya günceller.
     */
    suspend fun publishMuseumAuctionToSupabase(auction: MuseumAuctionEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val artifactObj = com.example.data.MuseumHeritageManager.allArtifacts.find { it.id == auction.artifactId }
            val artifactName = artifactObj?.name ?: auction.artifactId
            val endTimeIso = try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.format(java.util.Date(if (auction.endsAtMs > 0) auction.endsAtMs else System.currentTimeMillis() + 3600000L))
            } catch (e: Exception) {
                getCurrentIsoTimestamp()
            }

            val jsonObj = buildJsonObject {
                put("id", auction.id)
                put("artifact_id", auction.artifactId)
                put("artifact_name", artifactName)
                put("seller_id", auction.sellerId)
                put("seller_name", auction.sellerName)
                put("is_player_seller", auction.isPlayerSeller)
                put("starting_bid", auction.startingBid)
                put("current_highest_bid", auction.currentHighestBid)
                put("current_highest_bidder_id", auction.currentHighestBidderId)
                put("current_highest_bidder_name", auction.currentHighestBidderName)
                put("buyout_price", auction.buyoutPrice)
                put("ends_at_ms", auction.endsAtMs)
                put("end_time", endTimeIso)
                put("bid_count", auction.bidCount)
                put("created_at_ms", auction.createdAtMs)
                put("is_settled", auction.isSettled)
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = jsonObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val err = response.body?.string() ?: ""
                    Log.e(TAG, "Failed to publish museum auction to Supabase: ${response.code} $err")
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception publishing museum auction to Supabase", e)
            false
        }
    }

    /**
     * Müzayedeye verilen peyi doğrudan Supabase'e PATCH yöntemiyle anında yazar.
     */
    suspend fun updateMuseumAuctionBidInSupabase(
        auctionId: String,
        newBid: Long,
        bidderId: String,
        bidderName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObj = buildJsonObject {
                put("current_highest_bid", newBid)
                put("current_highest_bidder_id", bidderId)
                put("current_highest_bidder_name", bidderName)
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = jsonObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions?id=eq.$auctionId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val err = response.body?.string() ?: ""
                    Log.e(TAG, "Failed to update auction bid on Supabase: ${response.code} $err")
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating auction bid in Supabase", e)
            false
        }
    }

    /**
     * Supabase Auth: Signs in using Google ID Token directly (No Firebase required).
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val bodyObj = buildJsonObject {
                put("id_token", idToken)
                put("provider", "google")
            }
            val requestBody = bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=id_token")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val (success, responseBody) = httpClient.newCall(request).execute().use { response ->
                Pair(response.isSuccessful, response.body?.string() ?: "")
            }

            if (success) {
                val json = safeParseJsonElement(responseBody) as? JsonObject
                val userObj = json?.optJsonObject("user")
                val uid = userObj?.optString("id") ?: json?.optString("access_token")
                Pair(true, uid)
            } else {
                Log.w(TAG, "Supabase Google idToken auth failed: $responseBody")
                Pair(false, responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase Google Auth", e)
            Pair(false, e.localizedMessage)
        }
    }

    /**
     * Supabase Auth: Register with Email & Password.
     */
    suspend fun signUpWithEmail(email: String, password: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val bodyObj = buildJsonObject {
                put("email", email)
                put("password", password)
            }
            val requestBody = bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val (success, responseBody) = httpClient.newCall(request).execute().use { response ->
                Pair(response.isSuccessful, response.body?.string() ?: "")
            }

            if (success) {
                val json = safeParseJsonElement(responseBody) as? JsonObject
                val userObj = json?.optJsonObject("user")
                val uid = userObj?.optString("id") ?: ""
                Pair(true, uid)
            } else {
                Pair(false, responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase SignUp", e)
            Pair(false, e.localizedMessage)
        }
    }

    /**
     * Supabase Auth: Login with Email & Password.
     */
    suspend fun signInWithEmail(email: String, password: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val bodyObj = buildJsonObject {
                put("email", email)
                put("password", password)
            }
            val requestBody = bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val (success, responseBody) = httpClient.newCall(request).execute().use { response ->
                Pair(response.isSuccessful, response.body?.string() ?: "")
            }

            if (success) {
                val json = safeParseJsonElement(responseBody) as? JsonObject
                val userObj = json?.optJsonObject("user")
                val uid = userObj?.optString("id") ?: ""
                Pair(true, uid)
            } else {
                Pair(false, responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Supabase SignIn", e)
            Pair(false, e.localizedMessage)
        }
    }

    /**
     * Executes a PostgreSQL Remote Procedure Call (RPC) on Supabase.
     * Guarantees atomic server-side execution without client-side race conditions.
     */
    suspend fun executeRpc(functionName: String, paramsJson: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val requestBody = paramsJson.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/$functionName")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful) {
                    Log.d(TAG, "RPC $functionName executed successfully: $bodyStr")
                    Pair(true, bodyStr)
                } else {
                    Log.w(TAG, "RPC $functionName returned status ${response.code}: $bodyStr")
                    Pair(false, bodyStr)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception executing RPC $functionName", e)
            Pair(false, e.localizedMessage)
        }
    }

    suspend fun executeRpc(functionName: String, params: JsonObject): Pair<Boolean, String?> =
        executeRpc(functionName, params.toString())

    data class RemoteBorsaBuyResult(
        val success: Boolean,
        val itemId: String = "",
        val quantity: Int = 0,
        val unitPrice: Long = 0L,
        val totalCost: Long = 0L,
        val newStock: Long = 0L,
        val newPrice: Long = 0L,
        val errorMessage: String? = null
    )

    /**
     * Oyuncu Meta verilerini hafif UPSERT ile 'player_meta' tablosuna senkronize eder.
     * Supabase kotalarını korumak için sadece liderlik tablosu ve hile koruma verisini taşır.
     */
    suspend fun syncPlayerMeta(
        playerId: String,
        name: String,
        level: Int,
        netWorth: Long,
        hash: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("player_id", playerId)
                put("name", name)
                put("level", level)
                put("net_worth", netWorth)
                put("anti_cheat_hash", hash)
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/player_meta?on_conflict=player_id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Player meta synced to player_meta successfully for $playerId")
                    true
                } else {
                    Log.w(TAG, "Failed to sync player_meta for $playerId: code=${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during syncPlayerMeta", e)
            false
        }
    }

    /**
     * Supabase RPC execute_borsa_buy çağrısı ile PostgreSQL FOR UPDATE kilidi üzerinden
     * yarış durumu (race condition) ve stok çakışması olmaksızın borsa alımını atomik gerçekleştirir.
     */
    suspend fun executeRemoteBorsaBuy(
        playerId: String,
        itemId: String,
        quantity: Int,
        maxAcceptablePrice: Long = 0L
    ): RemoteBorsaBuyResult = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_player_id", playerId)
                put("p_item_id", itemId)
                put("p_quantity", quantity)
                put("p_max_acceptable_price", maxAcceptablePrice)
            }
            val (success, body) = executeRpc("execute_borsa_buy", params)
            if (success && !body.isNullOrBlank()) {
                val json = Json.parseToJsonElement(body).jsonObject
                val isSuccess = json["success"]?.jsonPrimitive?.booleanOrNull ?: false
                if (isSuccess) {
                    RemoteBorsaBuyResult(
                        success = true,
                        itemId = json["item_id"]?.jsonPrimitive?.content ?: itemId,
                        quantity = json["quantity"]?.jsonPrimitive?.intOrNull ?: quantity,
                        unitPrice = json["unit_price"]?.jsonPrimitive?.longOrNull ?: 0L,
                        totalCost = json["total_cost"]?.jsonPrimitive?.longOrNull ?: 0L,
                        newStock = json["new_stock"]?.jsonPrimitive?.longOrNull ?: 0L,
                        newPrice = json["new_price"]?.jsonPrimitive?.longOrNull ?: 0L
                    )
                } else {
                    val err = json["message"]?.jsonPrimitive?.content
                        ?: json["error"]?.jsonPrimitive?.content
                        ?: "Borsa alım işlemi başarısız"
                    RemoteBorsaBuyResult(success = false, errorMessage = err)
                }
            } else {
                RemoteBorsaBuyResult(success = false, errorMessage = body ?: "Ağ bağlantı hatası")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during executeRemoteBorsaBuy", e)
            RemoteBorsaBuyResult(success = false, errorMessage = e.localizedMessage)
        }
    }

    /**
     * Müzayedeyi Supabase'den siler (iptal veya transfer tamamlandığında).
     */
    suspend fun deleteMuseumAuctionFromSupabase(auctionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions?id=eq.$auctionId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting museum auction from Supabase", e)
            false
        }
    }

    // =========================================================================
    // CONSORTIUM CHAT SYSTEM (Local Only - Server Sync Disabled for Redesign)
    // =========================================================================

    /**
     * Konsorsiyum sohbeti sunucu senkronizasyonu kullanıcı isteği üzerine yerel mantık yeniden yapılandırılana kadar devre dışı bırakılmıştır.
     */
    suspend fun sendConsortiumChatMessageToSupabase(message: ConsortiumChatMessage): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObject = buildJsonObject {
                put("id", message.id)
                put("project_id", message.projectId)
                put("sender_id", message.senderId)
                put("sender_name", message.senderName)
                put("sender_role", message.senderRole)
                put("message_text", message.messageText)
                put("timestamp_ms", message.timestampMs)
                put("is_system_message", message.isSystemMessage)
            }
            val req = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/consortium_chats")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
                .build()
            executeAndLog(req, "chat_send")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Chat send error", e)
            false
        }
    }

    suspend fun fetchConsortiumChatMessagesFromSupabase(projectId: String): List<ConsortiumChatMessage>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/consortium_chats?project_id=eq.$projectId&order=timestamp_ms.asc&limit=100")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()
            
            httpClient.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return@use null
                    val jsonArray = safeParseJsonElement(bodyString) as? kotlinx.serialization.json.JsonArray ?: return@use null
                    jsonArray.mapNotNull { element ->
                        val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                        ConsortiumChatMessage(
                            id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                            projectId = (obj["project_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                            senderId = (obj["sender_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                            senderName = (obj["sender_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                            senderRole = (obj["sender_role"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                            messageText = (obj["message_text"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                            timestampMs = (obj["timestamp_ms"] as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull ?: 0L,
                            isSystemMessage = (obj["is_system_message"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull ?: false
                        )
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat fetch error", e)
            null
        }
    }

    // =========================================================================
    // GOOGLE PLAY IN-APP PURCHASE LEDGER & SERVER-SIDE AUDITING
    // =========================================================================
    suspend fun recordIapPurchaseTransaction(
        playerId: String,
        productId: String,
        gems: Int,
        orderId: String,
        purchaseToken: String,
        purchaseTimeMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObject = buildJsonObject {
                put("player_id", playerId)
                put("product_id", productId)
                put("gems_delivered", gems)
                put("order_id", orderId)
                put("purchase_token", purchaseToken)
                put("purchase_time_ms", purchaseTimeMs)
                put("verified_at_ms", System.currentTimeMillis())
            }

            val requestBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/iap_transactions")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            executeAndLog(request, "recordIapPurchaseTransaction")
        } catch (e: Exception) {
            Log.w(TAG, "Notice: Supabase IAP ledger recording note: ${e.message}")
            false
        }
    }

}
