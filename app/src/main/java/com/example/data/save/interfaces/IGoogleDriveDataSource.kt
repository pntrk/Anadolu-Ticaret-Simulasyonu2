package com.example.data.save.interfaces

/**
 * Interface contract for Google Drive AppData save synchronization.
 */
interface IGoogleDriveDataSource {
    fun setAccessToken(token: String?)
    fun hasAccessToken(): Boolean
    fun getLastSyncTimestamp(): Long
    suspend fun uploadSaveJson(jsonContent: String, accessToken: String? = null): Boolean
    suspend fun downloadSaveJson(accessToken: String? = null): String?
}
