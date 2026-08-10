package com.eldora25.tayfnotes.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object BackupImportHelper {

    fun importBackup(context: Context, uri: Uri, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Dosya açılamadı")
            val zis = ZipInputStream(inputStream)
            var entry = zis.getNextEntry()
            
            while (entry != null) {
                val fileName = entry.name
                val outFile = when {
                    fileName.startsWith("databases/") -> {
                        val dbName = fileName.substringAfter("databases/")
                        context.getDatabasePath(dbName)
                    }
                    fileName.startsWith("media/") -> {
                        val mediaName = fileName.substringAfter("media/")
                        File(context.cacheDir, mediaName)
                    }
                    else -> null
                }

                if (outFile != null) {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.getNextEntry()
            }
            zis.close()
            onComplete()
        } catch (e: Exception) {
            onError(e)
        }
    }
}
