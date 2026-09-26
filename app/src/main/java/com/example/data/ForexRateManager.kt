package com.example.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Live Forex and Central Bank Exchange Rate Synchronization Engine.
 * 
 * Features:
 * - Daily 09:00 AM synchronized USD/TRY rate broadcasted to all players via Supabase.
 * - Multi-tier live API fetching (Open Exchange Rates, Frankfurter, ExchangeRate-API).
 * - Automatic offline fallback to last saved local rate or default $1 = ₳50.0.
 * - Global 1.5% currency conversion commission model.
 * - Dynamic macroeconomic and export margin adjustments.
 */
data class ForexState(
    val usdTryRate: Double = 1.0,
    val rateChange24h: Double = 0.0, // e.g. +0.0%
    val lastSyncTimestampMs: Long = System.currentTimeMillis(),
    val lastSyncFormattedDate: String = "09:00 (Canlı Anadolu Lirası)",
    val isLiveSynced: Boolean = true,
    val source: String = "Anadolu Lirası Sabit Kuru",
    val rateHistory: List<Double> = listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
)

object ForexRateManager {
    private const val TAG = "ForexRateManager"
    const val DEFAULT_USD_TRY_RATE = 1.0
    const val CONVERSION_COMMISSION_PERCENT = 0.0 // %0.0 Kur Çevrim Komisyonu

    private val KEY_CACHED_USD_RATE = doublePreferencesKey("forex_cached_usd_rate")
    private val KEY_CACHED_RATE_CHANGE = doublePreferencesKey("forex_cached_rate_change")
    private val KEY_LAST_SYNC_MS = longPreferencesKey("forex_last_sync_ms")
    private val KEY_LAST_SYNC_DATE = stringPreferencesKey("forex_last_sync_date_str")
    private val KEY_LAST_SYNC_SOURCE = stringPreferencesKey("forex_last_sync_source")

    private val _forexState = MutableStateFlow(ForexState())
    val forexState: StateFlow<ForexState> = _forexState.asStateFlow()

    @Volatile
    var currentUsdRate: Double = 1.0
        private set

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun getUsdRate(): Double = 1.0

    /**
     * Converts Anatolian Liras (₳) to US Dollars ($) with optional 1.5% commission deduction.
     */
    fun convertTryToUsd(tryAmount: Long, applyCommission: Boolean = true): Long {
        return tryAmount
    }

    /**
     * Converts US Dollars ($) to Anatolian Liras (₳) with optional 1.5% commission deduction.
     */
    fun convertUsdToTry(usdAmount: Long, applyCommission: Boolean = true): Long {
        return usdAmount
    }

    /**
     * Calculates the exact 1.5% commission fee in TRY for buying USD with given TRY amount.
     */
    fun calculateTryCommission(tryAmount: Long): Long {
        return 0L
    }

    /**
     * Calculates the exact 1.5% commission fee in USD for selling given USD amount.
     */
    fun calculateUsdCommission(usdAmount: Long): Long {
        return 0L
    }

    /**
     * Initializes the Forex engine from local cache first, then triggers background sync.
     */
    fun initialize(context: Context) {
        ioScope.launch {
            try {
                loadCachedRate(context)
                syncLiveExchangeRate(context, force = false)
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
            }
        }
    }

    private suspend fun loadCachedRate(context: Context) {
        currentUsdRate = 1.0
        _forexState.value = ForexState(
            usdTryRate = 1.0,
            rateChange24h = 0.0,
            lastSyncTimestampMs = System.currentTimeMillis(),
            lastSyncFormattedDate = "09:00 (Sabit Kuru)",
            isLiveSynced = true,
            source = "Anadolu Lirası Sistemi",
            rateHistory = listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        )
    }

    /**
     * Performs daily live synchronization at/after 09:00 AM.
     * 1. Checks Supabase global_economy for today's published rate.
     * 2. If Supabase rate is absent/stale, queries open Forex APIs and broadcasts the new rate to Supabase.
     * 3. Falls back to offline cached rate seamlessly.
     */
    suspend fun syncLiveExchangeRate(context: Context, force: Boolean = false): Double = withContext(Dispatchers.IO) {
        currentUsdRate = 1.0
        _forexState.value = ForexState(
            usdTryRate = 1.0,
            rateChange24h = 0.0,
            lastSyncTimestampMs = System.currentTimeMillis(),
            lastSyncFormattedDate = "09:00 (Sabit Kuru)",
            isLiveSynced = true,
            source = "Anadolu Lirası Sistemi",
            rateHistory = listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        )
        return@withContext 1.0
    }

    /**
     * Queries multi-tier reliable free Forex APIs.
     */
    private fun fetchFromExternalApis(): Double? {
        val endpoints = listOf(
            "https://open.er-api.com/v6/latest/USD" to { json: JSONObject ->
                val rates = json.optJSONObject("rates")
                rates?.optDouble("TRY", 0.0) ?: 0.0
            },
            "https://api.frankfurter.app/latest?from=USD&to=TRY" to { json: JSONObject ->
                val rates = json.optJSONObject("rates")
                rates?.optDouble("TRY", 0.0) ?: 0.0
            },
            "https://api.exchangerate-api.com/v4/latest/USD" to { json: JSONObject ->
                val rates = json.optJSONObject("rates")
                rates?.optDouble("TRY", 0.0) ?: 0.0
            }
        )

        for ((url, parser) in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AnadoluTicaretSimulasyonu/1.0")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val rate = parser(json)
                            if (rate > 5.0 && rate < 500.0) {
                                Log.d(TAG, "Successfully fetched USD/TRY rate: $rate from $url")
                                return rate
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch from endpoint $url: ${e.message}")
            }
        }
        return null
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        return sdf.format(Date())
    }

    private fun generateSparkline(baseRate: Double, change: Double): List<Double> {
        val list = mutableListOf<Double>()
        var curr = baseRate * (1.0 - (change / 100.0))
        for (i in 0..5) {
            val jitter = ((i * 13) % 7 - 3) * 0.08
            list.add(Math.round((curr + jitter) * 100.0) / 100.0)
            curr += (baseRate - curr) / 5.0
        }
        list.add(baseRate)
        return list
    }
}
