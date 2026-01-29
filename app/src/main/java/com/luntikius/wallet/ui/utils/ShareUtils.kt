package com.luntikius.wallet.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.luntikius.wallet.data.exporter.ExportResult

/**
 * Share a pass file using the system share sheet.
 * Uses ExportResult to get proper MIME type and file.
 */
fun sharePassFile(context: Context, exportResult: ExportResult) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.luntikius.wallet.fileprovider",
            exportResult.file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = exportResult.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserTitle = "Share ${exportResult.formatName}"
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
