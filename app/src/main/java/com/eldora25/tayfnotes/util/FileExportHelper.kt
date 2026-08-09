package com.eldora25.tayfnotes.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.eldora25.tayfnotes.shared.model.Note
import java.io.File

object FileExportHelper {
    fun exportNoteToTxt(context: Context, note: Note) {
        val fileName = "${note.title.ifEmpty { "AdsizNot" }}.txt"
        val file = File(context.cacheDir, fileName)
        file.writeText("${note.title}\n\n${note.content}")

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Notu Paylaş"))
    }
}
