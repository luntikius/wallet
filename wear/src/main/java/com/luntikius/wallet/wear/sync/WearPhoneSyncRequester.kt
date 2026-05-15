package com.luntikius.wallet.wear.sync

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.luntikius.wallet.wearsync.WearSyncDataKeys
import com.luntikius.wallet.wearsync.WearSyncPaths
import kotlinx.coroutines.tasks.await

class WearPhoneSyncRequester(private val dataClient: DataClient) {
    suspend fun requestSync() {
        PutDataMapRequest.create(WearSyncPaths.REQUEST_SYNC).apply {
            dataMap.putLong(WearSyncDataKeys.UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent().also { request ->
            runCatching {
                dataClient.putDataItem(request).await()
            }
        }
    }
}
