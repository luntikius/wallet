package com.luntikius.wallet.data.exporter.pkpass

import android.content.Context
import com.luntikius.wallet.data.exporter.ExportResult
import com.luntikius.wallet.data.exporter.PassExporter
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassFormat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exporter for PKPass (Apple Wallet) format.
 * Reconstructs a .pkpass ZIP file from stored pass data and assets.
 */
class PKPassExporter(private val context: Context) : PassExporter {

    override suspend fun export(pass: Pass): ExportResult? = withContext(Dispatchers.IO) {
        try {
            // Validate format
            if (!canExport(pass)) {
                return@withContext null
            }

            // Validate required data
            if (pass.rawData.isBlank()) {
                return@withContext null
            }

            // Create output file
            val outputFile = createOutputFile(pass)
            outputFile.parentFile?.mkdirs()

            // Create ZIP archive
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                // Add pass.json
                addPassJson(zipOut, pass.rawData)

                // Add image files if assets directory exists
                pass.assetsDirectory?.let { assetsDir ->
                    val assetsFolder = File(assetsDir)
                    if (assetsFolder.exists() && assetsFolder.isDirectory) {
                        addImages(zipOut, assetsFolder)
                    }
                }
            }

            ExportResult(
                file = outputFile,
                mimeType = "application/vnd.apple.pkpass",
                formatName = "PKPass",
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getSupportedFormats(): List<PassFormat> = listOf(PassFormat.PKPASS)

    override fun canExport(pass: Pass): Boolean = pass.format == PassFormat.PKPASS

    /**
     * Create output file in cache directory with sanitized filename.
     */
    private fun createOutputFile(pass: Pass): File {
        val cacheDir = File(context.cacheDir, "shared_passes")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val organizationName = sanitizeFileName(pass.organizationName ?: "pass")
        val fileName = "${organizationName}_$timestamp.pkpass"
        return File(cacheDir, fileName)
    }

    /**
     * Add pass.json to the ZIP archive.
     */
    private fun addPassJson(zipOut: ZipOutputStream, rawData: String) {
        val entry = ZipEntry("pass.json")
        zipOut.putNextEntry(entry)
        zipOut.write(rawData.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }

    /**
     * Add all image files from the assets directory to the ZIP archive.
     * Preserves original filenames including @2x/@3x suffixes.
     */
    private fun addImages(zipOut: ZipOutputStream, assetsFolder: File) {
        assetsFolder.listFiles()?.forEach { file ->
            if (file.isFile && file.extension.lowercase() == "png") {
                val entry = ZipEntry(file.name)
                zipOut.putNextEntry(entry)
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }

    /**
     * Sanitize filename by removing/replacing invalid characters.
     */
    private fun sanitizeFileName(name: String): String = name
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .take(50) // Limit length
        .trim('_')
        .ifEmpty { "pass" }
}
