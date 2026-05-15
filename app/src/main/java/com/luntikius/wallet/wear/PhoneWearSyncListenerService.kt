package com.luntikius.wallet.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.luntikius.wallet.wearsync.WearSyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PhoneWearSyncListenerService : WearableListenerService() {
    private val wearSyncCoordinator: PhoneWearSyncCoordinator by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        if (dataEvents.any { event ->
                event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == WearSyncPaths.REQUEST_SYNC
            }
        ) {
            scope.launch {
                wearSyncCoordinator.publishNow()
            }
        }
    }
}
