package com.luntikius.wallet.data.repository

import android.content.Context
import android.net.Uri
import com.luntikius.wallet.data.archive.WalletArchive
import com.luntikius.wallet.data.archive.WalletArchiveImportResult
import com.luntikius.wallet.data.archive.WalletArchivePassMetadata
import com.luntikius.wallet.data.exporter.ExportResult
import com.luntikius.wallet.data.exporter.ExporterRegistry
import com.luntikius.wallet.data.json.WalletJson
import com.luntikius.wallet.data.local.PassDao
import com.luntikius.wallet.data.model.CustomPassJson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassFormat
import com.luntikius.wallet.data.network.PKPassUpdateService
import com.luntikius.wallet.data.network.PassDownloadService
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.data.parser.pkpass.PKPassParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

/**
 * Repository interface for pass operations.
 * Format-agnostic - works with any pass type.
 */
interface PassRepository {
    /**
     * Get all passes as a Flow for reactive updates.
     */
    fun getAllPasses(): Flow<List<Pass>>

    /**
     * Get a specific pass by ID.
     */
    suspend fun getPassById(passId: String): Pass?

    /**
     * Import a pass from a URI.
     * Uses ParserRegistry to determine the correct parser.
     */
    suspend fun importPass(uri: Uri): Result<Pass>

    /**
     * Delete a pass and its associated files.
     */
    suspend fun deletePass(pass: Pass): Result<Unit>

    /**
     * Update display orders for multiple passes.
     */
    suspend fun updateDisplayOrders(passOrderMap: Map<String, Int>): Result<Unit>

    /**
     * Refresh a single pass by fetching updated data from its web service.
     */
    suspend fun refreshPass(passId: String): Result<com.luntikius.wallet.data.network.PKPassUpdateService.UpdateResult>

    /**
     * Refresh all passes by fetching updated data from their web services.
     * Returns a map of pass IDs to their update results.
     */
    suspend fun refreshAllPasses(): Result<
        Map<String, com.luntikius.wallet.data.network.PKPassUpdateService.UpdateResult>,
        >

    /**
     * Toggle automatic background refresh for a pass.
     */
    suspend fun setAutoRefreshEnabled(passId: String, enabled: Boolean): Result<Unit>

    /**
     * Parse a pass file for preview without saving to database.
     * Extracts assets to temporary storage (cacheDir).
     */
    suspend fun parsePassForPreview(uri: Uri): Result<Pass>

    /**
     * Finalize adding a previewed pass to the wallet.
     * Moves assets from temp to permanent storage and saves to database.
     */
    suspend fun finalizePassImport(pass: Pass): Result<Pass>

    /**
     * Clean up temporary assets for a previewed pass.
     * Deletes the temp directory when preview is cancelled.
     */
    suspend fun cleanupPreviewAssets(pass: Pass): Result<Unit>

    /**
     * Download a pass from a URL and parse for preview.
     * Returns a temporary pass ready for preview.
     */
    suspend fun downloadAndPreviewPass(url: String): Result<Pass>

    /**
     * Create and save a custom pass from barcode scan data.
     */
    suspend fun createCustomPass(pass: Pass): Result<Pass>

    /**
     * Export a pass as a shareable file.
     * Uses ExporterRegistry to find appropriate exporter for the pass format.
     */
    suspend fun exportPassForSharing(passId: String): Result<com.luntikius.wallet.data.exporter.ExportResult>
}

interface WalletArchiveRepository {
    /**
     * Export all supported passes as a wallet backup ZIP.
     */
    suspend fun exportAllPassesForSharing(): Result<ExportResult>

    /**
     * Import a wallet backup ZIP. Imported passes replace existing passes with the same IDs.
     */
    suspend fun importWalletArchive(uri: Uri): Result<WalletArchiveImportResult>
}

/**
 * Implementation of PassRepository.
 */
class PassRepositoryImpl(
    private val passDao: PassDao,
    private val parserRegistry: ParserRegistry,
    private val context: Context,
) : PassRepository,
    WalletArchiveRepository {

    private val pkPassParser = PKPassParser(context)
    private val updateService = PKPassUpdateService(context, pkPassParser)
    private val downloadService = PassDownloadService(context)
    private val exporterRegistry = ExporterRegistry(context)

    override fun getAllPasses(): Flow<List<Pass>> = passDao.getAllPasses()

    override suspend fun getPassById(passId: String): Pass? = passDao.getPassById(passId)

    override suspend fun importPass(uri: Uri): Result<Pass> = withContext(Dispatchers.IO) {
        try {
            // Resolve correct parser based on file type
            val mimeType = context.contentResolver.getType(uri)
            val parser = parserRegistry.resolveParser(uri, mimeType)
                ?: return@withContext Result.failure(
                    Exception("Unsupported pass format"),
                )

            // Parse the pass file
            val result = parser.parse(uri)
                ?: return@withContext Result.failure(
                    Exception("Failed to parse pass file"),
                )

            // Assign displayOrder to put new pass at end
            val maxOrder = passDao.getMaxDisplayOrder() ?: -1
            val passWithOrder = result.pass.copy(displayOrder = maxOrder + 1)

            // Save to database
            passDao.insertPass(passWithOrder)

            Result.success(passWithOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePass(pass: Pass): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete from database
            passDao.deletePass(pass)

            // Delete associated files
            val assetsDir = File(pass.assetsDirectory)
            if (assetsDir.exists()) {
                assetsDir.deleteRecursively()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayOrders(passOrderMap: Map<String, Int>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val updates = passOrderMap.map { (passId, order) -> passId to order }
                passDao.updateDisplayOrders(updates)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun refreshPass(passId: String): Result<PKPassUpdateService.UpdateResult> =
        withContext(Dispatchers.IO) {
            try {
                // Get pass from database
                val pass = passDao.getPassById(passId)
                    ?: return@withContext Result.failure(Exception("Pass not found"))

                // Only PKPASS format supports refresh
                if (pass.format != PassFormat.PKPASS) {
                    return@withContext Result.success(PKPassUpdateService.UpdateResult.NoWebService)
                }

                // Attempt to update the pass
                val updateResult = updateService.updatePass(pass)

                // Handle the result
                when (updateResult) {
                    is PKPassUpdateService.UpdateResult.Updated -> {
                        // Update the pass with new data
                        val updatedPass = pkPassParser.updatePassFromJson(pass, updateResult.newPassJson)
                        passDao.updatePass(updatedPass)
                    }
                    is PKPassUpdateService.UpdateResult.Deleted -> {
                        // Mark pass as voided in rawData JSON
                        val passJson = WalletJson.json.decodeFromString<PKPassJson>(pass.rawData)
                        val voidedPassJson = passJson.copy(voided = true)
                        val voidedPass = pass.copy(
                            rawData = WalletJson.json.encodeToString(voidedPassJson),
                            lastRefreshDate = System.currentTimeMillis(),
                        )
                        passDao.updatePass(voidedPass)
                    }
                    is PKPassUpdateService.UpdateResult.NotModified -> {
                        // Update lastRefreshDate even though data didn't change
                        val refreshedPass = pass.copy(
                            lastRefreshDate = System.currentTimeMillis(),
                        )
                        passDao.updatePass(refreshedPass)
                    }
                    else -> {
                        // No database update needed for other results
                    }
                }

                Result.success(updateResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun refreshAllPasses(): Result<Map<String, PKPassUpdateService.UpdateResult>> =
        withContext(Dispatchers.IO) {
            try {
                // Get all passes from database
                val passes = passDao.getAllPassesList()
                val results = mutableMapOf<String, PKPassUpdateService.UpdateResult>()

                // Refresh each pass
                passes.forEach { pass ->
                    // Only PKPASS format supports refresh
                    if (pass.format == PassFormat.PKPASS) {
                        val result = refreshPass(pass.id)
                        if (result.isSuccess) {
                            results[pass.id] = result.getOrThrow()
                        }
                    }
                }

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun setAutoRefreshEnabled(passId: String, enabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                passDao.updateAutoRefreshEnabled(passId, enabled)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun parsePassForPreview(uri: Uri): Result<Pass> = withContext(Dispatchers.IO) {
        try {
            // Resolve correct parser based on file type
            val mimeType = context.contentResolver.getType(uri)
            val parser = parserRegistry.resolveParser(uri, mimeType)
                ?: return@withContext Result.failure(
                    Exception("Unsupported pass format"),
                )

            // Parse the pass file to temporary location
            val result = parser.parseToTemp(uri)
                ?: return@withContext Result.failure(
                    Exception("Failed to parse pass file"),
                )

            Result.success(result.pass)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finalizePassImport(pass: Pass): Result<Pass> = withContext(Dispatchers.IO) {
        try {
            // Determine new permanent location
            val permanentDir = File(context.filesDir, "passes/pkpass/${pass.id}")
            permanentDir.mkdirs()

            // Move assets from temp to permanent location
            val tempDir = File(pass.assetsDirectory)
            if (tempDir.exists()) {
                tempDir.copyRecursively(permanentDir, overwrite = true)
                tempDir.deleteRecursively()
            } else {
                return@withContext Result.failure(
                    Exception("Temporary assets not found"),
                )
            }

            // Update pass with new paths
            val updatedPass = pass.copy(
                assetsDirectory = permanentDir.absolutePath,
                iconPath = pass.iconPath.replace(tempDir.absolutePath, permanentDir.absolutePath),
                logoPath = pass.logoPath?.replace(tempDir.absolutePath, permanentDir.absolutePath),
                stripPath = pass.stripPath?.replace(tempDir.absolutePath, permanentDir.absolutePath),
                backgroundPath = pass.backgroundPath?.replace(tempDir.absolutePath, permanentDir.absolutePath),
                importedDate = System.currentTimeMillis(),
            )

            // Assign displayOrder to put new pass at end
            val maxOrder = passDao.getMaxDisplayOrder() ?: -1
            val passWithOrder = updatedPass.copy(displayOrder = maxOrder + 1)

            // Save to database
            passDao.insertPass(passWithOrder)

            Result.success(passWithOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupPreviewAssets(pass: Pass): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete temporary directory
            val tempDir = File(pass.assetsDirectory)
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadAndPreviewPass(url: String): Result<Pass> = withContext(Dispatchers.IO) {
        try {
            // Download the pass file
            val downloadResult = downloadService.downloadPass(url)

            when (downloadResult) {
                is PassDownloadService.DownloadResult.Success -> {
                    // Parse the downloaded file for preview
                    parsePassForPreview(downloadResult.fileUri)
                }

                is PassDownloadService.DownloadResult.Error -> {
                    Result.failure(Exception(downloadResult.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCustomPass(pass: Pass): Result<Pass> = withContext(Dispatchers.IO) {
        try {
            // Custom passes don't need asset extraction since they have no images
            // Assign displayOrder to put new pass at end
            val maxOrder = passDao.getMaxDisplayOrder() ?: -1
            val passWithOrder = pass.copy(displayOrder = maxOrder + 1)

            // Save to database
            passDao.insertPass(passWithOrder)

            Result.success(passWithOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportPassForSharing(passId: String): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val pass = passDao.getPassById(passId)
                ?: return@withContext Result.failure(Exception("Pass not found"))

            val exporter = exporterRegistry.resolveExporter(pass)
                ?: return@withContext Result.failure(
                    Exception("No exporter available for ${pass.format}"),
                )

            val result = exporter.export(pass)
                ?: return@withContext Result.failure(Exception("Export failed"))

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportAllPassesForSharing(): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val passes = passDao.getAllPassesList()
                .filter { pass -> pass.format == PassFormat.PKPASS || pass.format == PassFormat.CUSTOM }
            if (passes.isEmpty()) {
                return@withContext Result.failure(Exception("No passes available to export"))
            }

            Result.success(
                WalletArchive.export(
                    passes = passes,
                    outputFile = WalletArchive.createExportFile(context.cacheDir),
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importWalletArchive(uri: Uri): Result<WalletArchiveImportResult> =
        withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "archive_import")
            try {
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                val readResult = context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) {
                        return@withContext Result.failure(Exception("Unable to open wallet archive"))
                    }
                    WalletArchive.read(input)
                }

                var importedCount = 0
                var replacedCount = 0
                var failedCount = 0
                val importedIds = mutableListOf<String>()

                readResult.passes.forEach { payload ->
                    val metadata = payload.metadata
                    try {
                        val existingPass = passDao.getPassById(metadata.id)
                        val pass = when (metadata.format) {
                            PassFormat.PKPASS -> {
                                val pkPassBytes = payload.pkPassBytes
                                    ?: throw IllegalArgumentException("PKPass payload is missing")
                                importPkPassFromArchive(metadata, pkPassBytes, tempDir)
                            }
                            PassFormat.CUSTOM -> {
                                WalletJson.json.decodeFromString<CustomPassJson>(metadata.rawData)
                                with(WalletArchive) { metadata.toCustomPass() }
                            }
                            PassFormat.GOOGLE_WALLET -> throw IllegalArgumentException("Unsupported pass format")
                        }

                        existingPass?.let { existing ->
                            replacedCount++
                            if (existing.assetsDirectory != pass.assetsDirectory) {
                                deleteAssets(existing)
                            }
                        }
                        passDao.insertPass(pass)
                        importedIds += pass.id
                        importedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failedCount++
                    }
                }

                if (importedCount == 0) {
                    return@withContext Result.failure(Exception("No passes could be imported"))
                }

                normalizeDisplayOrderAfterArchiveImport(importedIds)

                Result.success(
                    WalletArchiveImportResult(
                        importedCount = importedCount,
                        replacedCount = replacedCount,
                        failedCount = failedCount,
                    ),
                )
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                tempDir.deleteRecursively()
            }
        }

    private suspend fun importPkPassFromArchive(
        metadata: WalletArchivePassMetadata,
        pkPassBytes: ByteArray,
        tempDir: File,
    ): Pass {
        val archivePassFile = File(tempDir, "${metadata.id}.pkpass")
        archivePassFile.writeBytes(pkPassBytes)

        val parsedPass = pkPassParser.parseToTemp(Uri.fromFile(archivePassFile))?.pass
            ?: throw IllegalArgumentException("Failed to parse PKPass payload")

        if (parsedPass.id != metadata.id) {
            cleanupPreviewAssets(parsedPass)
            throw IllegalArgumentException("PKPass payload ID does not match archive metadata")
        }

        val tempAssetsDir = File(parsedPass.assetsDirectory)
        val permanentDir = File(context.filesDir, "passes/pkpass/${parsedPass.id}")
        permanentDir.deleteRecursively()
        permanentDir.mkdirs()
        tempAssetsDir.copyRecursively(permanentDir, overwrite = true)
        tempAssetsDir.deleteRecursively()

        return parsedPass.copy(
            assetsDirectory = permanentDir.absolutePath,
            iconPath = parsedPass.iconPath.replace(tempAssetsDir.absolutePath, permanentDir.absolutePath),
            logoPath = parsedPass.logoPath?.replace(tempAssetsDir.absolutePath, permanentDir.absolutePath),
            stripPath = parsedPass.stripPath?.replace(tempAssetsDir.absolutePath, permanentDir.absolutePath),
            backgroundPath = parsedPass.backgroundPath?.replace(tempAssetsDir.absolutePath, permanentDir.absolutePath),
            importedDate = metadata.importedDate,
            lastRefreshDate = metadata.lastRefreshDate,
            autoRefreshEnabled = metadata.autoRefreshEnabled,
            displayOrder = metadata.displayOrder,
        )
    }

    private suspend fun normalizeDisplayOrderAfterArchiveImport(importedIds: List<String>) {
        val importedSet = importedIds.toSet()
        val currentPasses = passDao.getAllPassesList()
        val newOrder = importedIds + currentPasses
            .filterNot { pass -> pass.id in importedSet }
            .map { pass -> pass.id }

        passDao.updateDisplayOrders(
            newOrder.mapIndexed { index, passId -> passId to index },
        )
    }

    private fun deleteAssets(pass: Pass) {
        if (pass.assetsDirectory.isNotBlank()) {
            val assetsDir = File(pass.assetsDirectory)
            if (assetsDir.exists()) {
                assetsDir.deleteRecursively()
            }
        }
    }
}
