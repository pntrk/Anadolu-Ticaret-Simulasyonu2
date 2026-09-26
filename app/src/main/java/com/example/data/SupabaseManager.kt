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
    val lastSavedTime: Long = 0L
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

        // Smart merge for managers: preserve hired status, assigned name, level, and logs across all sources
        val rawManagersStr = when (val elem = baseMap["managers_json"] ?: baseMap["managers"]) {
            is JsonPrimitive -> elem.content
            is JsonArray -> elem.toString()
            else -> null
        }
        val colManagersStr = managersJson.takeIf { it.isNotBlank() && it != "null" && it != "[]" }

        fun parseManagerDtos(json: String?): List<CompanyManagerDto> {
            if (json.isNullOrBlank() || json == "[]" || json == "null" || json == "{}") return emptyList()
            return try {
                AppJson.decodeFromString<List<CompanyManagerDto>>(json)
            } catch (_: Exception) {
                try {
                    val elem = AppJson.parseToJsonElement(json) as? JsonArray ?: return emptyList()
                    elem.mapNotNull { item ->
                        val obj = item as? JsonObject ?: return@mapNotNull null
                        val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                        val isHired = (obj["isHired"] as? JsonPrimitive)?.booleanOrNull
                            ?: (obj["is_hired"] as? JsonPrimitive)?.booleanOrNull
                            ?: false
                        val level = (obj["level"] as? JsonPrimitive)?.intOrNull ?: 1
                        val eff = (obj["efficiency"] as? JsonPrimitive)?.doubleOrNull ?: 1.0
                        val sal = (obj["dailySalary"] as? JsonPrimitive)?.longOrNull
                            ?: (obj["daily_salary"] as? JsonPrimitive)?.longOrNull ?: 0L
                        val name = (obj["name"] as? JsonPrimitive)?.content ?: ""
                        val title = (obj["title"] as? JsonPrimitive)?.content ?: ""
                        val spec = (obj["specialty"] as? JsonPrimitive)?.content ?: ""
                        val desc = (obj["description"] as? JsonPrimitive)?.content ?: ""
                        CompanyManagerDto(
                            id = id,
                            name = name,
                            title = title,
                            specialty = spec,
                            level = level,
                            dailySalary = sal,
                            efficiency = eff,
                            isHired = isHired || level > 1 || name.isNotBlank(),
                            isActive = true,
                            description = desc
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val colList = parseManagerDtos(colManagersStr)
        val rawList = parseManagerDtos(rawManagersStr)
        val defaultManagers = getDefaultCompanyManagers()
        val allIds = (colList.map { it.id } + rawList.map { it.id } + defaultManagers.map { it.id }).distinct().filter { it.isNotBlank() }

        val mergedManagersJson = if (allIds.isNotEmpty()) {
            val mergedDtos = allIds.map { id ->
                val col = colList.find { it.id == id }
                val raw = rawList.find { it.id == id }
                val def = defaultManagers.find { it.id == id }
                
                val hasHireEvidence = (col?.isHired == true) || (raw?.isHired == true) || 
                                      ((col?.level ?: 1) > 1) || ((raw?.level ?: 1) > 1) ||
                                      (col?.actionLogs?.isNotEmpty() == true) || (raw?.actionLogs?.isNotEmpty() == true) ||
                                      (col?.name?.isNotBlank() == true && col.name != def?.name) ||
                                      (raw?.name?.isNotBlank() == true && raw.name != def?.name)
                val isHired = hasHireEvidence
                val level = maxOf(col?.level ?: 1, raw?.level ?: 1).coerceIn(1, 5)
                val eff = maxOf(col?.efficiency ?: 1.0, raw?.efficiency ?: 1.0)
                val name = when {
                    col != null && col.name.isNotBlank() -> col.name
                    raw != null && raw.name.isNotBlank() -> raw.name
                    def != null && def.name.isNotBlank() -> def.name
                    else -> ""
                }
                val title = col?.title?.ifBlank { raw?.title }?.ifBlank { def?.title } ?: def?.title.orEmpty()
                val spec = col?.specialty?.ifBlank { raw?.specialty }?.ifBlank { def?.specialty } ?: def?.specialty.orEmpty()
                val desc = col?.description?.ifBlank { raw?.description }?.ifBlank { def?.description } ?: def?.description.orEmpty()
                val sal = maxOf(col?.dailySalary ?: 0L, raw?.dailySalary ?: 0L, def?.dailySalary ?: 0L)
                val logs = if (col != null && col.actionLogs.isNotEmpty()) col.actionLogs else (raw?.actionLogs ?: emptyList())

                CompanyManagerDto(
                    id = id,
                    name = name,
                    title = title,
                    specialty = spec,
                    level = level,
                    dailySalary = sal,
                    efficiency = eff,
                    isHired = isHired,
                    isActive = col?.isActive ?: raw?.isActive ?: true,
                    description = desc,
                    actionLogs = logs
                )
            }
            AppJson.encodeToString(mergedDtos)
        } else {
            colManagersStr ?: rawManagersStr ?: "[]"
        }

        // Smart merge for research levels: preserve highest level across all sources
        val allTechKeys = listOf(
            "green_energy", "quality_control", "logistics", "automation",
            "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
            "aerospace", "heavy_industry", "consumer_goods", "petrochem",
            "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics"
        )
        val mergedLevels = mutableMapOf<String, Int>()

        allTechKeys.forEach { tech ->
            val directKey = "tech_$tech"
            val directVal = (baseMap[directKey] as? JsonPrimitive)?.longOrNull?.toInt()
                ?: (baseMap[tech] as? JsonPrimitive)?.longOrNull?.toInt() ?: 0
            if (directVal > 0) mergedLevels[tech] = directVal
        }

        val rawRLevelsStr = when (val elem = baseMap["research_levels_json"] ?: baseMap["research_levels"]) {
            is JsonPrimitive -> elem.content
            is JsonObject -> elem.toString()
            else -> null
        }
        if (!rawRLevelsStr.isNullOrBlank() && rawRLevelsStr != "null" && rawRLevelsStr != "{}") {
            try {
                val parsed = AppJson.decodeFromString<Map<String, Int>>(rawRLevelsStr)
                parsed.forEach { (k, v) ->
                    val base = k.removePrefix("tech_")
                    if (v > 0) mergedLevels[base] = maxOf(mergedLevels[base] ?: 0, v).coerceAtMost(5)
                }
            } catch (_: Exception) {
                try {
                    val elem = AppJson.parseToJsonElement(rawRLevelsStr) as? JsonObject
                    elem?.forEach { (k, vElem) ->
                        val lvl = (vElem as? JsonPrimitive)?.intOrNull ?: (vElem as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        if (lvl > 0) {
                            val base = k.removePrefix("tech_")
                            mergedLevels[base] = maxOf(mergedLevels[base] ?: 0, lvl).coerceAtMost(5)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        if (researchLevelsJson.isNotBlank() && researchLevelsJson != "null" && researchLevelsJson != "{}") {
            try {
                val parsed = AppJson.decodeFromString<Map<String, Int>>(researchLevelsJson)
                parsed.forEach { (k, v) ->
                    val base = k.removePrefix("tech_")
                    if (v > 0) mergedLevels[base] = maxOf(mergedLevels[base] ?: 0, v).coerceAtMost(5)
                }
            } catch (_: Exception) {
                try {
                    val elem = AppJson.parseToJsonElement(researchLevelsJson) as? JsonObject
                    elem?.forEach { (k, vElem) ->
                        val lvl = (vElem as? JsonPrimitive)?.intOrNull ?: (vElem as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        if (lvl > 0) {
                            val base = k.removePrefix("tech_")
                            mergedLevels[base] = maxOf(mergedLevels[base] ?: 0, lvl).coerceAtMost(5)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Smart merge for active researches & promote completed ones
        val mergedActive = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()

        fun parseActive(jsonStr: String?) {
            if (jsonStr.isNullOrBlank() || jsonStr == "null" || jsonStr == "{}") return
            try {
                val parsed = AppJson.decodeFromString<Map<String, Long>>(jsonStr)
                parsed.forEach { (k, v) ->
                    val base = k.removePrefix("tech_")
                    if (v > now) {
                        mergedActive[base] = maxOf(mergedActive[base] ?: 0L, v)
                    } else if (v > 0L) {
                        val cur = mergedLevels[base] ?: 0
                        mergedLevels[base] = (cur + 1).coerceAtMost(5)
                    }
                }
            } catch (_: Exception) {}
        }

        val rawActiveStr = (baseMap["active_researches_json"] as? JsonPrimitive)?.content
        parseActive(rawActiveStr)
        parseActive(activeResearchesJson)

        val finalResearchLevelsMap = mutableMapOf<String, Int>()
        allTechKeys.forEach { tech ->
            val lvl = (mergedLevels[tech] ?: 0).coerceAtMost(5)
            finalResearchLevelsMap[tech] = lvl
            finalResearchLevelsMap["tech_$tech"] = lvl
        }
        val mergedResearchLevelsJson = AppJson.encodeToString(finalResearchLevelsMap)
        val mergedActiveResearchesJson = AppJson.encodeToString(mergedActive)

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
            put("inventory_capacity", inventoryCapacity)
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

    /**
     * Oyuncu kaydını SupabasePlayerPayload nesnesi ile Supabase PostgreSQL tablosuna Upsert eder.
     */
    suspend fun syncPlayerToSupabase(payload: SupabasePlayerPayload): Boolean = withContext(Dispatchers.IO) {
        val effectiveEmail = payload.onlineEmail.ifBlank { if (payload.id != "local_player" && payload.id != "p_local") payload.id else "" }
        val effectiveUid = if (payload.id.isNotBlank() && payload.id != "local_player" && payload.id != "p_local") {
            payload.id
        } else {
            effectiveEmail.replace(".", "_")
        }
        if (!payload.isOnlineRegistered || effectiveEmail.isBlank() || effectiveEmail == "misafir_tuccar" || effectiveEmail == "local_trader" || effectiveUid.isBlank()) {
            Log.d(TAG, "Skipping Supabase sync for offline / guest player: ${payload.id}")
            return@withContext false
        }
        try {
            val businessesElem = safeParseJsonElement(payload.businessesJson) ?: JsonArray(emptyList())
            val inventoryElem = safeParseJsonElement(payload.inventoryJson) ?: JsonArray(emptyList())
            val managersElem = safeParseJsonElement(payload.managersJson) ?: JsonArray(emptyList())
            val activeResearchesElem = safeParseJsonElement(payload.activeResearchesJson) ?: JsonObject(emptyMap())
            val researchLevelsElem = safeParseJsonElement(payload.researchLevelsJson) ?: JsonObject(emptyMap())
            val guildSharesElem = safeParseJsonElement(payload.guildSharesJson) ?: JsonObject(emptyMap())
            val guildBuyPricesElem = safeParseJsonElement(payload.guildBuyPricesJson) ?: JsonObject(emptyMap())

            val jsonObject = buildJsonObject {
                put("id", effectiveUid)
                put("name", payload.name)
                put("company_name", payload.companyName)
                put("money", payload.money)
                put("loan_amount", payload.loanAmount)
                put("deposit_balance", payload.depositBalance)
                put("daily_income", payload.dailyIncome)
                put("daily_expense", payload.dailyExpense)
                put("total_profit", payload.totalProfit)
                put("xp", payload.xp)
                put("level", payload.level)
                put("inventory_capacity", payload.inventoryCapacity)
                put("current_city", migrateLegacyCity(payload.currentCity))
                put("is_vip", payload.isVip)
                put("gems", payload.gems)
                put("last_daily_reward_ms", payload.lastDailyRewardMs)
                put("login_streak", payload.loginStreak)
                put("is_online_registered", payload.isOnlineRegistered)
                put("online_email", effectiveEmail)
                put("businesses", businessesElem)
                put("inventory", inventoryElem)
                put("managers", managersElem)
                put("active_researches", activeResearchesElem)
                put("research_levels", researchLevelsElem)
                put("guild_shares", guildSharesElem)
                put("guild_buy_prices", guildBuyPricesElem)
                if (!payload.rawSaveJson.isNullOrBlank()) {
                    put("raw_save_json", payload.rawSaveJson)
                }
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?on_conflict=id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            executeAndLog(request, "syncPlayerPayloadToSupabase")
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
        rawSaveJson: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val effectiveEmail = onlineEmail.ifBlank { if (uid != "local_player" && uid != "p_local") uid else "" }
        val effectiveUid = if (uid.isNotBlank() && uid != "local_player" && uid != "p_local") {
            uid
        } else {
            effectiveEmail.replace(".", "_")
        }
        if (!isOnlineRegistered || effectiveEmail.isBlank() || effectiveEmail == "misafir_tuccar" || effectiveEmail == "local_trader" || effectiveUid.isBlank()) {
            Log.d(TAG, "Skipping Supabase sync for offline / guest player: $uid")
            return@withContext false
        }
        try {
            val bArray = buildJsonArray {
                businesses.forEach { b ->
                    add(buildJsonObject {
                        put("id", b.id)
                        put("type", b.type)
                        put("level", b.level)
                        put("cityId", migrateLegacyCity(b.cityId))
                        put("wearLevel", b.wearLevel.toDouble())
                        put("storageCapacity", b.getEffectiveStorageCapacity())
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

            val managersElem = safeParseJsonElement(managersJson) ?: JsonArray(emptyList())
            val activeResearchesElem = safeParseJsonElement(activeResearchesJson) ?: JsonObject(emptyMap())
            val researchLevelsElem = safeParseJsonElement(researchLevelsJson) ?: JsonObject(emptyMap())
            val guildSharesElem = safeParseJsonElement(guildSharesJson) ?: JsonObject(emptyMap())
            val guildBuyPricesElem = safeParseJsonElement(guildBuyPricesJson) ?: JsonObject(emptyMap())
            val activeDeliveriesElem = safeParseJsonElement(activeDeliveriesJson) ?: JsonArray(emptyList())
            val activeProductionsElem = try {
                val prods = AppJson.decodeFromString<List<ActiveProduction>>(activeProductionsJson)
                buildJsonArray {
                    prods.forEach { ap ->
                        add(buildJsonObject {
                            put("id", ap.id)
                            put("productId", ap.productId)
                            put("product_id", ap.productId)
                            put("quantity", ap.quantity)
                            put("facilityId", ap.facilityId)
                            put("facility_id", ap.facilityId)
                            put("businessId", ap.businessId)
                            put("cityId", ap.cityId)
                            put("targetCityId", ap.targetCityId)
                            put("startTimeMs", ap.startTimeMs)
                            put("start_time_ms", ap.startTimeMs)
                            put("totalDurationMs", ap.totalDurationMs)
                            put("total_duration_ms", ap.totalDurationMs)
                            put("endTimeMs", ap.effectiveEndTimeMs)
                            put("end_time_ms", ap.effectiveEndTimeMs)
                            put("isAgriProduct", ap.isAgriProduct)
                            put("originCountry", ap.originCountry)
                        })
                    }
                }
            } catch (_: Exception) {
                safeParseJsonElement(activeProductionsJson) ?: JsonArray(emptyList())
            }
            val dailyQuestElem = safeParseJsonElement(dailyQuestStateJson)

            val jsonObject = buildJsonObject {
                put("id", effectiveUid)
                put("name", name)
                put("company_name", companyName)
                put("money", money)
                put("loan_amount", loanAmount)
                put("deposit_balance", depositBalance)
                put("daily_income", dailyIncome)
                put("daily_expense", dailyExpense)
                put("total_profit", totalProfit)
                put("xp", xp)
                put("level", level)
                put("inventory_capacity", inventoryCapacity)
                put("current_city", migrateLegacyCity(currentCity))
                put("is_vip", isVip)
                put("gems", gems)
                put("last_daily_reward_ms", lastDailyRewardMs)
                put("login_streak", loginStreak)
                put("is_online_registered", isOnlineRegistered)
                put("online_email", effectiveEmail)
                put("businesses", bArray)
                put("inventory", invArray)
                put("managers", managersElem)
                put("active_researches", activeResearchesElem)
                put("research_levels", researchLevelsElem)
                put("guild_shares", guildSharesElem)
                put("guild_buy_prices", guildBuyPricesElem)
                if (!rawSaveJson.isNullOrBlank()) {
                    put("raw_save_json", rawSaveJson)
                }
                put("updated_at", getCurrentIsoTimestamp())
            }

            val requestBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
            
            // Send to 'players' table
            val req1 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?on_conflict=id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()
            val success = executeAndLog(req1, "players_upsert")

            Log.d(TAG, "Successfully synced player $uid to Supabase (players table)")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing player to Supabase", e)
            false
        }
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
            val candidates = mutableListOf<String>()
            candidates.add(cleanUid)
            candidates.add(cleanUid.lowercase())
            if (cleanUid.contains(".")) {
                candidates.add(cleanUid.replace(".", "_"))
                candidates.add(cleanUid.lowercase().replace(".", "_"))
            }
            if (cleanUid.contains("@")) {
                candidates.add(cleanUid.replace("@", "_").replace(".", "_"))
                candidates.add(cleanUid.lowercase().replace("@", "_").replace(".", "_"))
            }
            if (cleanUid.contains("_gmail_com")) {
                candidates.add(cleanUid.replace("_gmail_com", "@gmail.com"))
                candidates.add(cleanUid.lowercase().replace("_gmail_com", "@gmail.com"))
            }
            val distinctCandidates = candidates.filter { it.isNotBlank() }.distinct()

            val foundPayloads = mutableListOf<SupabasePlayerPayload>()

            for (target in distinctCandidates) {
                // 1. Try querying by online_email (exact and case-insensitive)
                foundPayloads.addAll(queryPlayerRecords("online_email", target, isIlike = false))
                foundPayloads.addAll(queryPlayerRecords("online_email", target, isIlike = true))

                // 2. Try querying by email column fallback
                foundPayloads.addAll(queryPlayerRecords("email", target, isIlike = false))
                foundPayloads.addAll(queryPlayerRecords("email", target, isIlike = true))

                // 3. Try querying by id (exact and case-insensitive)
                foundPayloads.addAll(queryPlayerRecords("id", target, isIlike = false))
                foundPayloads.addAll(queryPlayerRecords("id", target, isIlike = true))
            }

            if (foundPayloads.isEmpty()) {
                Log.w(TAG, "No Supabase player record found for candidates: $distinctCandidates")
                return@withContext null
            }

            // Return the best record by level/profit/money first (to protect actual progress from empty overwrites), then timestamp
            val bestPayload = foundPayloads.distinctBy { it.id + "_" + it.onlineEmail + "_" + it.level + "_" + it.money }.maxWithOrNull(
                compareBy<SupabasePlayerPayload> { it.level }
                    .thenBy { it.totalProfit }
                    .thenBy { it.money }
                    .thenBy { it.lastSavedTime }
            )

            if (bestPayload != null) {
                Log.i(TAG, "Selected best Supabase player record: ${bestPayload.name} (Lvl ${bestPayload.level}, Money ${bestPayload.money}, email=${bestPayload.onlineEmail})")
            }
            bestPayload
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching player from Supabase", e)
            null
        }
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

        // Extract and combine research levels from research_levels column, research_levels_json, and individual tech_* columns
        val techKeys = listOf(
            "green_energy", "quality_control", "logistics", "automation",
            "quantum_ai", "nanotech", "cyber_security", "biotech_cloning",
            "aerospace", "heavy_industry", "consumer_goods", "petrochem",
            "biotech_med", "battery_tech", "cyber_automation", "biotech_synthesis", "quantum_logistics"
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
            inventoryCapacity = doc.optInt("inventory_capacity", 5000),
            currentCity = migrateLegacyCity(doc.optString("current_city", "istanbul")),
            isVip = doc.optBoolean("is_vip", false),
            gems = doc.optInt("gems", 0),
            lastDailyRewardMs = doc.optLong("last_daily_reward_ms", 0L),
            loginStreak = doc.optInt("login_streak", 0),
            dollarBalance = rawMoney,
            dollarDepositBalance = rawDeposit,
            dollarLoanAmount = rawLoan,
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
            lastSavedTime = effectiveSavedTime
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
     * Borsa Fiyatlarını Supabase'e günceller (Bulk Upsert & Fallback Direct Patch)
     */
    suspend fun syncMarketPrices(prices: List<MarketPriceEntity>): Boolean = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeMarketPrices(prices)
            val jsonArray = buildJsonArray {
                sanitized.forEach { entity ->
                    add(buildJsonObject {
                        put("id", entity.itemId)
                        put("item_id", entity.itemId)
                        put("symbol", entity.itemId)
                        put("item_name", entity.itemId)
                        put("base_price", entity.price)
                        put("current_price", entity.price)
                        put("stock", entity.borsaStock)
                        put("borsa_stock", entity.borsaStock)
                        put("origin_country", entity.originCountry)
                        put("origin_city_id", entity.originCityId)
                        put("is_usd", entity.isUsd)
                    })
                }
            }

            val requestBody = jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE)
            
            // 1. Try Upsert with on_conflict=item_id
            val request1 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/market_prices?on_conflict=item_id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build()

            var success = httpClient.newCall(request1).execute().use { response -> response.isSuccessful }
            
            // 2. If failed, try on_conflict=id
            if (!success) {
                val request2 = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/market_prices?on_conflict=id")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(requestBody)
                    .build()
                success = httpClient.newCall(request2).execute().use { response -> response.isSuccessful }
            }
            
            // 3. Guaranteed direct PATCH for every single product row
            sanitized.forEach { entity ->
                updateSingleMarketPriceInSupabase(
                    itemId = entity.itemId,
                    newStock = entity.borsaStock,
                    newPrice = entity.price,
                    originCountry = entity.originCountry
                )
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing market prices to Supabase", e)
            false
        }
    }

    /**
     * Tekil Borsa Ürün Stok ve Fiyatını Anlık Olarak Supabase'e İşler (Direct SQL PATCH)
     */
    suspend fun updateSingleMarketPriceInSupabase(
        itemId: String,
        newStock: Long,
        newPrice: Long,
        originCountry: String = "Türkiye"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("id", itemId)
                put("item_id", itemId)
                put("symbol", itemId)
                put("stock", newStock)
                put("borsa_stock", newStock)
                put("current_price", newPrice)
                put("base_price", newPrice)
            }
            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

            // 1. Direct PATCH by item_id
            val patchReq1 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/market_prices?item_id=eq.$itemId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .patch(requestBody)
                .build()

            var success = httpClient.newCall(patchReq1).execute().use { it.isSuccessful }

            // 2. If not matched, try PATCH by id
            if (!success) {
                val patchReq2 = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/market_prices?id=eq.$itemId")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .patch(requestBody)
                    .build()
                success = httpClient.newCall(patchReq2).execute().use { it.isSuccessful }
            }

            // 3. If row does not exist, insert via POST
            if (!success) {
                val upsertReq = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/market_prices?on_conflict=item_id")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(requestBody)
                    .build()
                success = httpClient.newCall(upsertReq).execute().use { it.isSuccessful }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating single market price for $itemId to Supabase", e)
            false
        }
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
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()
            executeAndLog(req1, "global_guilds_upsert")

            val req2 = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/guilds")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
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
            var list = queryGuildsFromEndpoint("$SUPABASE_URL/rest/v1/global_guilds?select=*")
            if (list.isNullOrEmpty()) {
                list = queryGuildsFromEndpoint("$SUPABASE_URL/rest/v1/guilds?select=*")
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
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/market_prices?select=*")
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
                    val stock = if (rawStock <= 50_000L || rawStock > MacroEconomyEngine.DEFAULT_BORSA_STOCK) {
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
                    // Supabase'deki eski 50.000L kalıntılarını ve eksik ürünleri 999.999.999.999L stok ile Supabase'e geri yaz
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
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/global_market?select=*")
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
                            createdAt = parsedCreatedAt
                        )
                    )
                } else if (id.startsWith("bo_") || sellerName.startsWith("BUY_ORDER:")) {
                    // Tedarik Talebi (Alım Emri)
                    val buyerName = if (sellerName.startsWith("BUY_ORDER:")) {
                        sellerName.removePrefix("BUY_ORDER:")
                    } else sellerName

                    buyOrders.add(
                        com.example.data.BuyOrder(
                            id = id,
                            buyerId = sellerId,
                            buyerName = buyerName,
                            itemId = itemId,
                            quantity = quantity,
                            pricePerUnit = pricePerUnit,
                            destinationCityId = city,
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
                            qualityTier = "Standart",
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
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(reqBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing market listing to Supabase", e)
            false
        }
    }

    suspend fun deleteMarketListingFromSupabase(listingId: String): Boolean = withContext(Dispatchers.IO) {
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
                .addHeader("Prefer", "resolution=merge-duplicates")
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
                .addHeader("Prefer", "resolution=merge-duplicates")
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
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/players?select=*&order=money.desc&limit=$limit")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: ""
            }

            val jsonArray = safeParseJsonElement(body) as? JsonArray ?: return@withContext null
            val resultList = mutableListOf<OnlinePlayer>()
            for (i in 0 until jsonArray.size) {
                val doc = jsonArray[i] as? JsonObject ?: continue
                val id = doc.optString("id", "")
                val isOnlineReg = doc.optBoolean("is_online_registered", false)

                // Only include authentic registered players who signed in (skip offline/guest entries)
                if (id.isBlank() || id == "local" || id == "misafir_tuccar" || id.startsWith("guest") || (!id.contains("@") && !id.contains("_") && !isOnlineReg)) {
                    continue
                }

                val money = doc.optLong("money", 0L)
                val deposit = doc.optLong("deposit_balance", 0L)
                val loan = doc.optLong("loan_amount", 0L)
                val dollarBal = doc.optLong("dollar_balance", 0L)
                val dollarDep = doc.optLong("dollar_deposit_balance", 0L)
                val dollarLoan = doc.optLong("dollar_loan_amount", 0L)
                val usdRate = ForexRateManager.currentUsdRate.coerceAtLeast(1.0)
                val tryNet = money + deposit - loan
                val usdNet = dollarBal + dollarDep - dollarLoan
                val netWorth = (tryNet + (usdNet * usdRate)).toLong().coerceAtLeast(0L)
                val lvl = doc.optInt("level", 1)

                resultList.add(
                    OnlinePlayer(
                        id = id,
                        name = doc.optString("name", "Tüccar"),
                        companyName = doc.optString("company_name", "${doc.optString("name", "Tüccar")} Holding"),
                        netWorth = netWorth,
                        city = doc.optString("current_city", "istanbul"),
                        level = lvl,
                        isOnline = true,
                        badge = if (lvl > 15) "CEO" else if (lvl > 10) "LİDER" else "TÜCCAR",
                        bankBalance = deposit + (dollarDep * usdRate).toLong(),
                        monthlyScore = doc.optLong("monthly_growth", (netWorth * 0.20f).toLong())
                    )
                )
            }
            resultList.sortedByDescending { it.netWorth }
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
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_artifacts?select=*")
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
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions?select=*&is_settled=eq.false")
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
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/museum_auctions?id=eq.$auctionId&select=*")
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
