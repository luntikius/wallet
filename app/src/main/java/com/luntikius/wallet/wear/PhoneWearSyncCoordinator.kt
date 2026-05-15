package com.luntikius.wallet.wear

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.repository.PassRepository
import com.luntikius.wallet.wearsync.WearPassIndex
import com.luntikius.wallet.wearsync.WearSyncDataKeys
import com.luntikius.wallet.wearsync.WearSyncJson
import com.luntikius.wallet.wearsync.WearSyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PhoneWearSyncCoordinator(private val dataClient: DataClient, private val passRepository: PassRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            passRepository.getAllPasses().collectLatest { passes ->
                publishPasses(passes)
            }
        }
    }

    suspend fun publishNow() {
        publishPasses(passRepository.getAllPasses().first())
    }

    private suspend fun publishPasses(passes: List<Pass>) = withContext(Dispatchers.IO) {
        val snapshotsByPass = passes.mapNotNull { pass ->
            pass.toWearPassSnapshot()?.let { snapshot -> pass to snapshot }
        }

        val index = WearPassIndex(
            passIds = snapshotsByPass.map { (_, snapshot) -> snapshot.id },
            updatedAt = System.currentTimeMillis(),
        )

        PutDataMapRequest.create(WearSyncPaths.PASSES_INDEX).apply {
            dataMap.putString(WearSyncDataKeys.INDEX_JSON, WearSyncJson.encodeIndex(index))
            dataMap.putLong(WearSyncDataKeys.UPDATED_AT, index.updatedAt)
        }.asPutDataRequest().setUrgent().also { request ->
            dataClient.putDataItem(request).await()
        }

        snapshotsByPass.forEach { (pass, snapshot) ->
            PutDataMapRequest.create(WearSyncPaths.pass(snapshot.id)).apply {
                dataMap.putString(WearSyncDataKeys.SNAPSHOT_JSON, WearSyncJson.encodeSnapshot(snapshot))
                dataMap.putLong(WearSyncDataKeys.UPDATED_AT, System.currentTimeMillis())
                snapshot.assets.forEach { assetRef ->
                    pass.assetFile(assetRef.type)?.let { file ->
                        dataMap.putAsset(assetRef.type.dataMapKey, Asset.createFromBytes(file.readBytes()))
                    }
                }
            }.asPutDataRequest().setUrgent().also { request ->
                dataClient.putDataItem(request).await()
            }
        }
    }
}

val com.luntikius.wallet.wearsync.WearPassAssetType.dataMapKey: String
    get() = when (this) {
        com.luntikius.wallet.wearsync.WearPassAssetType.ICON -> WearSyncDataKeys.ICON_ASSET
        com.luntikius.wallet.wearsync.WearPassAssetType.LOGO -> WearSyncDataKeys.LOGO_ASSET
    }
