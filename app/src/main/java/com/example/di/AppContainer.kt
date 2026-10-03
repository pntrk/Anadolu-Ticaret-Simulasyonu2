package com.example.di

import android.content.Context
import com.example.data.save.impl.AndroidFileStorageProvider
import com.example.data.save.impl.CloudServerTimeProvider
import com.example.data.save.impl.GoogleDriveSaveDataSource
import com.example.data.save.impl.StranglerFigGameSaveGateway
import com.example.data.save.impl.SupabaseNoSqlSaveSource
import com.example.data.save.interfaces.ICloudNoSqlSaveSource
import com.example.data.save.interfaces.IFileStorageProvider
import com.example.data.save.interfaces.IGameSaveGateway
import com.example.data.save.interfaces.IGoogleDriveDataSource
import com.example.data.save.interfaces.IServerTimeProvider
import java.lang.ref.WeakReference

/**
 * Dependency Injection container for Android Application:
 * Provides loosely coupled, testable, and injectable singletons
 * without binding directly or leaking Activity contexts.
 */
object AppContainer {

    private var appContextRef: WeakReference<Context>? = null

    fun initialize(context: Context) {
        appContextRef = WeakReference(context.applicationContext)
    }

    val appContext: Context?
        get() = appContextRef?.get()

    val fileStorageProvider: IFileStorageProvider by lazy {
        AndroidFileStorageProvider { appContextRef?.get() }
    }

    val googleDriveDataSource: IGoogleDriveDataSource by lazy {
        GoogleDriveSaveDataSource(fileStorageProvider)
    }

    val cloudNoSqlSaveSource: ICloudNoSqlSaveSource by lazy {
        SupabaseNoSqlSaveSource()
    }

    val serverTimeProvider: IServerTimeProvider by lazy {
        CloudServerTimeProvider()
    }

    val saveGateway: IGameSaveGateway by lazy {
        StranglerFigGameSaveGateway(
            legacyDriveDataSource = googleDriveDataSource,
            cloudNoSqlSaveSource = cloudNoSqlSaveSource
        )
    }
}
