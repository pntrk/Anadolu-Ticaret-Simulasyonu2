package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.save.interfaces.IGoogleDriveDataSource
import com.example.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GoogleDriveSaveManager:
 * Manages player save JSON synchronization with Google Drive AppData Space via Google Drive REST API v3.
 * Protects Supabase storage and network quotas by storing heavy save JSONs in the player's personal Drive.
 */
object GoogleDriveSaveManager {
    private const val TAG = "GoogleDriveSaveManager"
    const val SAVE_FILE_NAME = "anadolu_ticaret_save.json"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var lastSyncTimestampMs: Long = 0L

    fun setAccessToken(token: String?) {
        cachedAccessToken = token
        try {
            AppContainer.googleDriveDataSource.setAccessToken(token)
        } catch (_: Exception) {}
    }

    fun getAccessToken(): String? = cachedAccessToken

    fun hasAccessToken(): Boolean = !cachedAccessToken.isNullOrBlank()

    fun getLastSyncTimestamp(): Long = lastSyncTimestampMs

    /**
     * Google Drive drive.appdata (appDataFolder) klasöründe "anadolu_ticaret_save.json" dosyasını arar.
     * Dosya varsa PATCH ile içeriğini günceller, yoksa multipart POST ile yükler.
     * Tüm ağ çağrıları Dispatchers.IO üzerinde güvenli try-catch blokları ile çalıştırılır.
     */
    suspend fun uploadSaveJson(jsonContent: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            Log.w(TAG, "uploadSaveJson: Access token is empty or blank")
            return@withContext false
        }
        try {
            val existingFileId = findSaveFileId(accessToken)
            if (existingFileId != null) {
                // Update existing save file via PATCH
                val patchUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val body = jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(patchUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        lastSyncTimestampMs = System.currentTimeMillis()
                        Log.i(TAG, "Successfully updated $SAVE_FILE_NAME on Google Drive AppData (PATCH)")
                        return@withContext true
                    } else {
                        Log.e(TAG, "Failed to PATCH $SAVE_FILE_NAME on Google Drive: HTTP ${response.code} ${response.message}")
                    }
                }
            } else {
                // Upload new save file via multipart POST into appDataFolder
                val boundary = "======" + System.currentTimeMillis() + "======"
                val metadata = JSONObject().apply {
                    put("name", SAVE_FILE_NAME)
                    put("parents", org.json.JSONArray().put("appDataFolder"))
                }.toString()

                val multipartBody = StringBuilder().apply {
                    append("--").append(boundary).append("\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata).append("\r\n")
                    append("--").append(boundary).append("\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(jsonContent).append("\r\n")
                    append("--").append(boundary).append("--\r\n")
                }.toString()

                val postUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val request = Request.Builder()
                    .url(postUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "multipart/related; boundary=$boundary")
                    .post(multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        lastSyncTimestampMs = System.currentTimeMillis()
                        Log.i(TAG, "Successfully created $SAVE_FILE_NAME in Google Drive AppData (POST)")
                        return@withContext true
                    } else {
                        Log.e(TAG, "Failed to POST $SAVE_FILE_NAME to Google Drive: HTTP ${response.code} ${response.message}")
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadSaveJson", e)
            false
        }
    }

    /**
     * drive.appdata (appDataFolder) klasöründen dosyanın içeriğini okuyup string olarak döner.
     * Tüm ağ çağrıları Dispatchers.IO üzerinde güvenli try-catch blokları ile çalıştırılır.
     */
    suspend fun downloadSaveJson(accessToken: String): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            Log.w(TAG, "downloadSaveJson: Access token is empty or blank")
            return@withContext null
        }
        try {
            val fileId = findSaveFileId(accessToken) ?: run {
                Log.i(TAG, "downloadSaveJson: $SAVE_FILE_NAME not found in Google Drive appDataFolder")
                return@withContext null
            }

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val content = response.body?.string()
                    if (!content.isNullOrBlank()) {
                        Log.i(TAG, "Successfully downloaded $SAVE_FILE_NAME from Google Drive AppData (${content.length} chars)")
                        return@withContext content
                    }
                } else {
                    Log.e(TAG, "Failed to download $SAVE_FILE_NAME from Google Drive: HTTP ${response.code} ${response.message}")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception in downloadSaveJson", e)
            null
        }
    }

    /**
     * Searches for the save file inside Google Drive's appDataFolder space.
     */
    private fun findSaveFileId(accessToken: String): String? {
        return try {
            val listUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='$SAVE_FILE_NAME'+and+trashed=false&fields=files(id,name)"
            val request = Request.Builder()
                .url(listUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val json = JSONObject(bodyStr)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        return files.getJSONObject(0).optString("id")
                    }
                } else {
                    Log.w(TAG, "findSaveFileId query failed: HTTP ${response.code}")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception in findSaveFileId", e)
            null
        }
    }

    // Helper overloads
    suspend fun downloadSaveJson(): String? {
        val token = cachedAccessToken ?: return null
        return downloadSaveJson(token)
    }

    suspend fun uploadSaveJson(jsonContent: String): Boolean {
        val token = cachedAccessToken ?: return false
        return uploadSaveJson(jsonContent, token)
    }

    suspend fun uploadSaveToDrive(context: Context, jsonContent: String, accessToken: String? = null): Boolean {
        AppContainer.initialize(context)
        val token = accessToken ?: cachedAccessToken ?: resolveAccessToken(context)
        return if (!token.isNullOrBlank()) {
            uploadSaveJson(jsonContent, token)
        } else {
            false
        }
    }

    suspend fun downloadSaveFromDrive(context: Context, accessToken: String? = null): String? {
        AppContainer.initialize(context)
        val token = accessToken ?: cachedAccessToken ?: resolveAccessToken(context)
        return if (!token.isNullOrBlank()) {
            downloadSaveJson(token)
        } else {
            null
        }
    }

    fun resolveAccessToken(context: Context): String? {
        return try {
            if (!cachedAccessToken.isNullOrBlank()) return cachedAccessToken
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val scope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
                val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account.account!!, scope)
                if (!token.isNullOrBlank()) {
                    setAccessToken(token)
                    return token
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve OAuth token via GoogleAuthUtil: ${e.message}")
            null
        }
    }
}
