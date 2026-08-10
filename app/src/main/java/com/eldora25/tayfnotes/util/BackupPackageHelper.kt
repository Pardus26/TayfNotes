package com.eldora25.tayfnotes.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupPackageHelper {

    fun createFullBackup(context: Context, onComplete: (File) -> Unit, onError: (Exception) -> Unit) {
        try {
            val backupFile = File(context.cacheDir, "TayfNotes_FullBackup_${System.currentTimeMillis()}.zip")
            val zos = ZipOutputStream(FileOutputStream(backupFile))

            // 1. Export Database
            val dbFile = context.getDatabasePath("tayfnotes_database")
            if (dbFile.exists()) {
                addToZip(dbFile, "databases/tayfnotes_database", zos)
            }
            
            // 2. Export Media (from cacheDir or filesDir if stored there)
            // For this project, we stored audio in cacheDir and images as URIs.
            // Note: Real implementation should resolve URIs and copy bytes.
            // For now, let's zip everything in cacheDir that looks like our media.
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("audio_") || file.name.endsWith(".txt")) {
                    addToZip(file, "media/${file.name}", zos)
                }
            }

            zos.close()
            onComplete(backupFile)
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun addToZip(file: File, zipPath: String, zos: ZipOutputStream) {
        val entry = ZipEntry(zipPath)
        zos.putNextEntry(entry)
        val fis = FileInputStream(file)
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    fun shareBackup(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_SUBJECT, "TayfNotes Full Backup")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Yedeği Paylaş"))
    }
}
