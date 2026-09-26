package com.example.data

import android.content.Context
import com.example.data.save.interfaces.IGoogleDriveDataSource
import com.example.di.AppContainer

/**
 * GoogleDriveSaveManager (Facade / Compatibility Adapter):
 * Delegates save operations to the injected IGoogleDriveDataSource and AppContainer.
 * Decouples direct Android Context dependencies while maintaining full backward compatibility.
 */
object GoogleDriveSaveManager {

    private val dataSource: IGoogleDriveDataSource
        get() = AppContainer.googleDriveDataSource

    fun setAccessToken(token: String?) {
        dataSource.setAccessToken(token)
    }

    fun hasAccessToken(): Boolean = dataSource.hasAccessToken()

    fun getLastSyncTimestamp(): Long = dataSource.getLastSyncTimestamp()

    /**
     * Uploads the game save JSON to Google Drive AppData Space (and local backup).
     */
    suspend fun uploadSaveToDrive(context: Context, jsonContent: String, accessToken: String? = null): Boolean {
        AppContainer.initialize(context)
        return dataSource.uploadSaveJson(jsonContent, accessToken)
    }

    /**
     * Downloads the game save JSON from Google Drive AppData Space.
     */
    suspend fun downloadSaveFromDrive(context: Context, accessToken: String? = null): String? {
        AppContainer.initialize(context)
        return dataSource.downloadSaveJson(accessToken)
    }
}
