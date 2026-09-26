package com.example.data.save.interfaces

import java.io.File

/**
 * Decouples file system interactions from direct Android Context in classes.
 */
interface IFileStorageProvider {
    fun readLocalFile(fileName: String): String?
    fun writeLocalFile(fileName: String, content: String): Boolean
    fun getFilesDirectory(): File?
}
