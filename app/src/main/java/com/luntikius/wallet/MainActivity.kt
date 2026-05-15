package com.luntikius.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.luntikius.wallet.data.worker.PassRefreshWorker
import com.luntikius.wallet.designsystem.theme.WalletTheme
import com.luntikius.wallet.ui.navigation.PassNavGraph
import com.luntikius.wallet.ui.navigation.Routes
import com.luntikius.wallet.ui.viewmodel.EducationViewModel
import com.luntikius.wallet.ui.viewmodel.PassGridViewModel
import com.luntikius.wallet.ui.viewmodel.PassPreviewViewModel
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val gridViewModel: PassGridViewModel by viewModel()
    private val previewViewModel: PassPreviewViewModel by viewModel()
    private val educationViewModel: EducationViewModel by viewModel()

    private var intentUri by mutableStateOf<Uri?>(null)
    private var newIntentUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If launched from external app with ACTION_VIEW, relaunch in our own task
        if (intent?.action == Intent.ACTION_VIEW && !isTaskRoot) {
            val relaunchIntent = Intent(this, MainActivity::class.java).apply {
                action = intent.action
                data = intent.data
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(relaunchIntent)
            finish()
            return
        }

        enableEdgeToEdge()

        // Clean up temporary preview files on startup
        lifecycleScope.launch {
            File(cacheDir, "preview_passes").deleteRecursively()
            File(cacheDir, "shared_passes").deleteRecursively()
        }

        // Schedule background pass refresh
        schedulePassRefresh()

        // Extract intent URI if present
        intentUri = if (intent?.action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            null
        }

        setContent {
            WalletTheme {
                val navController = rememberNavController()

                PassNavGraph(
                    navController = navController,
                    gridViewModel = gridViewModel,
                    previewViewModel = previewViewModel,
                    educationViewModel = educationViewModel,
                    intentUri = intentUri,
                    modifier = Modifier.fillMaxSize(),
                )

                // Handle new intents while app is running
                LaunchedEffect(newIntentUri) {
                    newIntentUri?.let { uri ->
                        educationViewModel.startAppEntry(isExternalImport = true)
                        previewViewModel.previewPass(uri)
                        navController.navigate(Routes.PREVIEW) {
                            popUpTo(Routes.GRID) { inclusive = false }
                            launchSingleTop = true
                        }
                        newIntentUri = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new intent while app is running
        if (intent.action == Intent.ACTION_VIEW) {
            newIntentUri = intent.data
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
