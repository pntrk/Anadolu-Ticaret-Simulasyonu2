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

/**
 * Modern Dependency Injection Module.
 * Follows Dagger / Hilt provider conventions and maps interface bindings to implementations.
 */
class SaveModule(private val context: Context) {

    fun provideFileStorageProvider(): IFileStorageProvider {
        return AndroidFileStorageProvider { context.applicationContext }
    }

    fun provideGoogleDriveDataSource(fileStorage: IFileStorageProvider): IGoogleDriveDataSource {
        return GoogleDriveSaveDataSource(fileStorage)
    }

    fun provideCloudNoSqlSaveSource(): ICloudNoSqlSaveSource {
        return SupabaseNoSqlSaveSource()
    }

    fun provideServerTimeProvider(): IServerTimeProvider {
        return CloudServerTimeProvider()
    }

    fun provideGameSaveGateway(
        googleDriveDS: IGoogleDriveDataSource,
        cloudNoSqlDS: ICloudNoSqlSaveSource
    ): IGameSaveGateway {
        return StranglerFigGameSaveGateway(googleDriveDS, cloudNoSqlDS)
    }
}
