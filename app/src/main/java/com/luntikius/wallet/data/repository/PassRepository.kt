package com.luntikius.wallet.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.luntikius.wallet.data.local.PassDao
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassFormat
import com.luntikius.wallet.data.network.PKPassUpdateService
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.data.parser.pkpass.PKPassParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

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
    suspend fun refreshAllPasses(): Result<Map<String, com.luntikius.wallet.data.network.PKPassUpdateService.UpdateResult>>

    /**
     * Toggle automatic background refresh for a pass.
     */
    suspend fun setAutoRefreshEnabled(passId: String, enabled: Boolean): Result<Unit>
}

/**
 * Implementation of PassRepository.
 */
class PassRepositoryImpl(
    private val passDao: PassDao,
    private val parserRegistry: ParserRegistry,
    private val context: Context
) : PassRepository {

    private val pkPassParser = PKPassParser(context)
    private val updateService = PKPassUpdateService(context, pkPassParser)
    private val gson = Gson()

    override fun getAllPasses(): Flow<List<Pass>> {
        return passDao.getAllPasses()
    }

    override suspend fun getPassById(passId: String): Pass? {
        return passDao.getPassById(passId)
    }

    override suspend fun importPass(uri: Uri): Result<Pass> = withContext(Dispatchers.IO) {
        try {
            // Resolve correct parser based on file type
            val mimeType = context.contentResolver.getType(uri)
            val parser = parserRegistry.resolveParser(uri, mimeType)
                ?: return@withContext Result.failure(
                    Exception("Unsupported pass format")
                )

            // Parse the pass file
            val result = parser.parse(uri)
                ?: return@withContext Result.failure(
                    Exception("Failed to parse pass file")
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
                        val passJson = gson.fromJson(pass.rawData, PKPassJson::class.java)
                        val voidedPassJson = passJson.copy(voided = true)
                        val voidedPass = pass.copy(
                            rawData = gson.toJson(voidedPassJson),
                            lastRefreshDate = System.currentTimeMillis()
                        )
                        passDao.updatePass(voidedPass)
                    }
                    is PKPassUpdateService.UpdateResult.NotModified -> {
                        // Update lastRefreshDate even though data didn't change
                        val refreshedPass = pass.copy(
                            lastRefreshDate = System.currentTimeMillis()
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
}
