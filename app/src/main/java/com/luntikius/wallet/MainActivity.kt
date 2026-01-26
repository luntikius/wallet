package com.luntikius.wallet

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.luntikius.wallet.data.local.PassDatabase
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.repository.PassRepositoryImpl
import com.luntikius.wallet.data.worker.PassRefreshWorker
import com.luntikius.wallet.ui.navigation.PassNavGraph
import com.luntikius.wallet.ui.theme.WalletTheme
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PassViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Test obsolete SDK check - should fail lint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // This check is obsolete since minSdk is 33
        }

        // Initialize database, repository, and ViewModel
        val database = PassDatabase.getInstance(applicationContext)
        val parserRegistry = ParserRegistry(applicationContext)
        val repository = PassRepositoryImpl(
            passDao = database.passDao(),
            parserRegistry = parserRegistry,
            context = applicationContext,
        )
        viewModel = PassViewModel(repository)

        // Schedule background pass refresh
        schedulePassRefresh()

        setContent {
            WalletTheme {
                val navController = rememberNavController()

                PassNavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )

                // Handle intent for opening pass files
                LaunchedEffect(Unit) {
                    handleIntent(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.importPass(uri)
            }
        }
    }

    /**
     * Schedules daily background refresh of passes.
     * Runs once per day when WiFi is available and battery is not low.
     */
    private fun schedulePassRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
            .setRequiresBatteryNotLow(true)
            .build()

        val refreshRequest = PeriodicWorkRequestBuilder<PassRefreshWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "pass_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                refreshRequest,
            )
    }
}
