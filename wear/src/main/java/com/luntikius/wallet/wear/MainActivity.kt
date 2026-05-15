package com.luntikius.wallet.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.luntikius.wallet.wear.data.WearPassDatabase
import com.luntikius.wallet.wear.data.WearPassRepository
import com.luntikius.wallet.wear.ui.WearWalletApp
import com.luntikius.wallet.wearsync.WearSyncDataKeys
import com.luntikius.wallet.wearsync.WearSyncPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        WearPassRepository(
            context = applicationContext,
            dao = WearPassDatabase.getInstance(applicationContext).wearPassDao(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            requestPhoneSync()
        }

        setContent {
            WearWalletApp(repository = repository)
        }
    }

    private suspend fun requestPhoneSync() {
        PutDataMapRequest.create(WearSyncPaths.REQUEST_SYNC).apply {
            dataMap.putLong(WearSyncDataKeys.UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent().also { request ->
            runCatching {
                Wearable.getDataClient(applicationContext).putDataItem(request).await()
            }
        }
    }
}
