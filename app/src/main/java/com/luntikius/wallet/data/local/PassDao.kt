package com.luntikius.wallet.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luntikius.wallet.data.model.Pass
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Pass entities.
 * Provides format-agnostic database operations.
 */
@Dao
interface PassDao {
    /**
     * Get all passes as a Flow for reactive updates.
     * Ordered by imported date (newest first).
     */
    @Query("SELECT * FROM passes ORDER BY importedDate DESC")
    fun getAllPasses(): Flow<List<Pass>>

    /**
     * Get a specific pass by ID.
     */
    @Query("SELECT * FROM passes WHERE id = :passId")
    suspend fun getPassById(passId: String): Pass?

    /**
     * Insert a new pass. Replaces if already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPass(pass: Pass)

    /**
     * Delete a pass.
     */
    @Delete
    suspend fun deletePass(pass: Pass)

    /**
     * Delete a pass by ID.
     */
    @Query("DELETE FROM passes WHERE id = :passId")
    suspend fun deletePassById(passId: String)
}
