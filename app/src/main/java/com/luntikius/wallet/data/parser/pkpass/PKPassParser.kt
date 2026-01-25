package com.luntikius.wallet.data.parser.pkpass

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassCategory
import com.luntikius.wallet.data.model.PassFormat
import com.luntikius.wallet.data.parser.ParseResult
import com.luntikius.wallet.data.parser.PassParser
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parser for Apple Wallet PKPass files (.pkpass).
 * Extracts ZIP archives and parses pass.json.
 */
class PKPassParser(private val context: Context) : PassParser {

    private val gson = Gson()

    override suspend fun parse(uri: Uri): ParseResult? = withContext(Dispatchers.IO) {
        try {
            // Open input stream from URI
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null

            // Parse pass.json and extract assets
            val zipInputStream = ZipInputStream(inputStream)
            var passJson: PKPassJson? = null
            val extractedFiles = mutableMapOf<String, ByteArray>()

            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val fileName = entry.name

                if (!entry.isDirectory) {
                    val bytes = zipInputStream.readBytes()

                    when {
                        fileName == "pass.json" -> {
                            passJson = gson.fromJson(String(bytes), PKPassJson::class.java)
                        }
                        fileName.matches(Regex("icon(@\\dx)?\\.png")) ||
                            fileName.matches(Regex("logo(@\\dx)?\\.png")) ||
                            fileName.matches(Regex("strip(@\\dx)?\\.png")) ||
                            fileName.matches(Regex("background(@\\dx)?\\.png")) -> {
                            extractedFiles[fileName] = bytes
                        }
                    }
                }

                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }

            zipInputStream.close()

            if (passJson == null) {
                return@withContext null
            }

            // Create directory for this pass
            val passDir = File(context.filesDir, "passes/pkpass/${passJson.serialNumber}")
            passDir.mkdirs()

            // Save extracted files
            extractedFiles.forEach { (fileName, bytes) ->
                File(passDir, fileName).writeBytes(bytes)
            }

            // Find best resolution for all images (@3x > @2x > @1x)
            val iconPath = findBestImage(passDir, "icon")
                ?: return@withContext null
            val logoPath = findBestImage(passDir, "logo")
            val stripPath = findBestImage(passDir, "strip")
            val backgroundPath = findBestImage(passDir, "background")

            // Determine pass category
            val category = when {
                passJson.boardingPass != null -> PassCategory.BOARDING_PASS
                passJson.eventTicket != null -> PassCategory.EVENT_TICKET
                passJson.coupon != null -> PassCategory.COUPON
                passJson.storeCard != null -> PassCategory.STORE_CARD
                else -> PassCategory.GENERIC
            }

            // Convert to common Pass entity
            val pass = Pass(
                id = passJson.serialNumber,
                format = PassFormat.PKPASS,
                organizationName = passJson.organizationName,
                description = passJson.description,
                iconPath = iconPath,
                logoPath = logoPath,
                stripPath = stripPath,
                backgroundPath = backgroundPath,
                foregroundColor = normalizeColor(passJson.foregroundColor),
                backgroundColor = normalizeColor(passJson.backgroundColor),
                labelColor = normalizeColor(passJson.labelColor),
                assetsDirectory = passDir.absolutePath,
                rawData = gson.toJson(passJson),
                importedDate = System.currentTimeMillis(),
                category = category,
            )

            ParseResult(pass, gson.toJson(passJson))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getSupportedFormats(): List<PassFormat> = listOf(PassFormat.PKPASS)

    override fun canParse(uri: Uri, mimeType: String?): Boolean = mimeType == "application/vnd.apple.pkpass" ||
        uri.path?.endsWith(".pkpass") == true ||
        uri.toString().endsWith(".pkpass")

    /**
     * Find the best available image resolution for a given image type.
     * Preference: image@3x.png > image@2x.png > image.png
     */
    private fun findBestImage(passDir: File, baseName: String): String? {
        val imageOptions = listOf("$baseName@3x.png", "$baseName@2x.png", "$baseName.png")
        return imageOptions
            .map { File(passDir, it) }
            .firstOrNull { it.exists() }
            ?.absolutePath
    }

    /**
     * Normalize color format from rgb(R,G,B) or #RRGGBB to #RRGGBB.
     */
    private fun normalizeColor(color: String?): String? {
        if (color == null) return null

        // Already in hex format
        if (color.startsWith("#")) {
            return color
        }

        // Parse rgb(R, G, B) format
        val rgbPattern = Regex("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)")
        val match = rgbPattern.matchEntire(color)
        if (match != null) {
            val (r, g, b) = match.destructured
            return String.format("#%02X%02X%02X", r.toInt(), g.toInt(), b.toInt())
        }

        return null
    }

    /**
     * Updates an existing pass with new pass.json data.
     * Updates files in existing assetsDirectory and returns new Pass entity.
     *
     * @param existingPass The current pass to update
     * @param newPassJson The new pass.json data from server
     * @return Updated Pass entity with new data and updated timestamp
     */
    suspend fun updatePassFromJson(existingPass: Pass, newPassJson: PKPassJson): Pass = withContext(Dispatchers.IO) {
        // Use existing assets directory
        val passDir = File(existingPass.assetsDirectory)

        // Note: In a full implementation, we would fetch new images from server if URLs changed
        // For now, we keep existing images and only update the pass.json data

        // Find best resolution for all images (use existing logic)
        val iconPath = findBestImage(passDir, "icon") ?: existingPass.iconPath
        val logoPath = findBestImage(passDir, "logo") ?: existingPass.logoPath
        val stripPath = findBestImage(passDir, "strip") ?: existingPass.stripPath
        val backgroundPath = findBestImage(passDir, "background") ?: existingPass.backgroundPath

        // Determine pass category (may have changed)
        val category = when {
            newPassJson.boardingPass != null -> PassCategory.BOARDING_PASS
            newPassJson.eventTicket != null -> PassCategory.EVENT_TICKET
            newPassJson.coupon != null -> PassCategory.COUPON
            newPassJson.storeCard != null -> PassCategory.STORE_CARD
            else -> PassCategory.GENERIC
        }

        // Return updated Pass entity
        existingPass.copy(
            organizationName = newPassJson.organizationName,
            description = newPassJson.description,
            iconPath = iconPath,
            logoPath = logoPath,
            stripPath = stripPath,
            backgroundPath = backgroundPath,
            foregroundColor = normalizeColor(newPassJson.foregroundColor),
            backgroundColor = normalizeColor(newPassJson.backgroundColor),
            labelColor = normalizeColor(newPassJson.labelColor),
            rawData = gson.toJson(newPassJson),
            lastRefreshDate = System.currentTimeMillis(),
            category = category,
        )
    }
}
