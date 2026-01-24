package com.luntikius.wallet.data.repository

import android.content.Context
import android.net.Uri
import com.luntikius.wallet.data.local.PassDao
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.parser.ParserRegistry
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
}

/**
 * Implementation of PassRepository.
 */
class PassRepositoryImpl(
    private val passDao: PassDao,
    private val parserRegistry: ParserRegistry,
    private val context: Context
) : PassRepository {

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
}
