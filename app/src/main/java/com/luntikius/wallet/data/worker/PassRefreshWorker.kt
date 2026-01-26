package com.luntikius.wallet.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.luntikius.wallet.data.local.PassDatabase
import com.luntikius.wallet.data.model.PassFormat
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.repository.PassRepositoryImpl

/**
 * Background worker for refreshing passes daily.
 * Runs once per day when WiFi is available and battery is not low.
 */
class PassRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        // Instantiate repository manually (no DI framework)
        val database = PassDatabase.getInstance(applicationContext)
        val parserRegistry = ParserRegistry(applicationContext)
        val repository = PassRepositoryImpl(
            passDao = database.passDao(),
            parserRegistry = parserRegistry,
            context = applicationContext,
        )

        // Get all passes and filter only those with autoRefreshEnabled = true
        val allPasses = database.passDao().getAllPassesList()
        val passesToRefresh = allPasses.filter { pass ->
            pass.autoRefreshEnabled &&
                pass.format == PassFormat.PKPASS
        }

        // Refresh each enabled pass
        var hasError = false
        passesToRefresh.forEach { pass ->
            val result = repository.refreshPass(pass.id)
            if (result.isFailure) {
                hasError = true
            }
        }

        if (!hasError) {
            Result.success()
        } else {
            // Retry with exponential backoff on failure
            Result.retry()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Retry with exponential backoff on exception
        Result.retry()
    }
}
