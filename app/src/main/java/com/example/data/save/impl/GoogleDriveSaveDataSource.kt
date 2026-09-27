package com.example.data.save.impl

import android.util.Log
import com.example.data.network.AppJson
import com.example.data.network.GoogleDriveFileListDto
import com.example.data.network.GoogleDriveMetadataDto
import com.example.data.save.interfaces.IFileStorageProvider
import com.example.data.save.interfaces.IGoogleDriveDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GoogleDriveSaveDataSource(
    private val fileStorage: IFileStorageProvider,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : IGoogleDriveDataSource {

    private val TAG = "GoogleDriveSaveDS"
    private val SAVE_FILE_NAME = "anadolu_ticaret_save.json"
    private val BACKUP_FILE_NAME = "google_drive_save_backup.json"

    private var cachedAccessToken: String? = null
    private var lastSyncTimestampMs: Long = 0L

    override fun setAccessToken(token: String?) {
        cachedAccessToken = token
    }

    override fun hasAccessToken(): Boolean = !cachedAccessToken.isNullOrBlank()

    override fun getLastSyncTimestamp(): Long = lastSyncTimestampMs

    override suspend fun uploadSaveJson(jsonContent: String, accessToken: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            // Always save to local backup file first
            fileStorage.writeLocalFile(BACKUP_FILE_NAME, jsonContent)

            val token = accessToken ?: cachedAccessToken
            if (token.isNullOrBlank()) {
                Log.w(TAG, "No Google Drive OAuth Access Token available. Saved to local backup.")
                return@withContext true
            }

            val existingFileId = findSaveFileId(token)
            if (existingFileId != null) {
                // Update existing file
                val url = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val body = jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .patch(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        lastSyncTimestampMs = System.currentTimeMillis()
                        Log.d(TAG, "Successfully updated savegame.json on Google Drive AppData")
                        return@withContext true
                    } else {
                        Log.e(TAG, "Failed to update savegame.json on Google Drive: ${response.code}")
                    }
                }
            } else {
                // Create new file in appDataFolder
                val boundary = "=======" + System.currentTimeMillis() + "======="
                val metadata = AppJson.encodeToString(
                    GoogleDriveMetadataDto(
                        name = SAVE_FILE_NAME,
                        parents = listOf("appDataFolder")
                    )
                )

                val multipartBody = StringBuilder().apply {
                    append("--").append(boundary).append("\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata).append("\r\n")
                    append("--").append(boundary).append("\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(jsonContent).append("\r\n")
                    append("--").append(boundary).append("--\r\n")
                }.toString()

                val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "multipart/related; boundary=$boundary")
                    .post(multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        lastSyncTimestampMs = System.currentTimeMillis()
                        Log.d(TAG, "Successfully created savegame.json in Google Drive AppData")
                        return@withContext true
                    } else {
                        Log.e(TAG, "Failed to create savegame.json on Google Drive: ${response.code}")
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception during uploadSaveJson", e)
            false
        }
    }

    override suspend fun downloadSaveJson(accessToken: String?): String? = withContext(Dispatchers.IO) {
        try {
            val token = accessToken ?: cachedAccessToken
            if (!token.isNullOrBlank()) {
                val fileId = findSaveFileId(token)
                if (fileId != null) {
                    val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val content = response.body?.string()
                            if (!content.isNullOrBlank()) {
                                fileStorage.writeLocalFile(BACKUP_FILE_NAME, content)
                                return@withContext content
                            }
                        }
                    }
                }
            }
            return@withContext fileStorage.readLocalFile(BACKUP_FILE_NAME)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during downloadSaveJson", e)
            return@withContext fileStorage.readLocalFile(BACKUP_FILE_NAME)
        }
    }

    private fun findSaveFileId(accessToken: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='$SAVE_FILE_NAME'"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: return null
                try {
                    val fileListDto = AppJson.decodeFromString<GoogleDriveFileListDto>(bodyStr)
                    return fileListDto.files.firstOrNull()?.id
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Google Drive file list", e)
                }
            }
        }
        return null
    }
}
