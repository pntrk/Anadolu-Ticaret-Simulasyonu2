package com.example.data.save.impl

import android.content.Context
import android.util.Log
import com.example.data.save.interfaces.IFileStorageProvider
import java.io.File

/**
 * Concrete implementation of IFileStorageProvider wrapping Android Application storage.
 */
class AndroidFileStorageProvider(
    private val appContextProvider: () -> Context?
) : IFileStorageProvider {

    private val TAG = "FileStorageProvider"

    override fun readLocalFile(fileName: String): String? {
        val dir = getFilesDirectory() ?: return null
        return try {
            val file = File(dir, fileName)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading local file $fileName", e)
            null
        }
    }

    override fun writeLocalFile(fileName: String, content: String): Boolean {
        val dir = getFilesDirectory() ?: return false
        return try {
            val file = File(dir, fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing local file $fileName", e)
            false
        }
    }

    override fun getFilesDirectory(): File? {
        return try {
            appContextProvider()?.filesDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed resolving filesDir", e)
            null
        }
    }
}
