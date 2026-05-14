package com.luntikius.wallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.RefreshStatus
import com.luntikius.wallet.data.model.ShareStatus
import com.luntikius.wallet.data.network.PKPassUpdateService
import com.luntikius.wallet.data.repository.PassRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PassGridViewModel(private val passRepository: PassRepository, importStatusHolder: ImportStatusHolder) :
    ViewModel() {

    val importStatus: StateFlow<ImportStatus> = importStatusHolder.importStatus

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    val passes: StateFlow<List<Pass>> = passRepository.getAllPasses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            passes.first()
            _isInitialLoading.value = false
        }
    }

    private val _refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    val refreshStatus: StateFlow<RefreshStatus> = _refreshStatus.asStateFlow()

    private val _shareStatus = MutableStateFlow<ShareStatus>(ShareStatus.Idle)
    val shareStatus: StateFlow<ShareStatus> = _shareStatus.asStateFlow()

    fun refreshPass(passId: String) {
        viewModelScope.launch {
            _refreshStatus.value = RefreshStatus.Loading(passId)
            val result = passRepository.refreshPass(passId)

            if (result.isSuccess) {
                val updateResult = result.getOrThrow()
                _refreshStatus.value = when (updateResult) {
                    is PKPassUpdateService.UpdateResult.Updated -> RefreshStatus.Success(1)
                    is PKPassUpdateService.UpdateResult.NotModified -> RefreshStatus.Success(0)
                    is PKPassUpdateService.UpdateResult.Deleted ->
                        RefreshStatus.Error("Pass was deleted", passId)
                    is PKPassUpdateService.UpdateResult.Unauthorized ->
                        RefreshStatus.Error("Update authorization failed", passId)
                    is PKPassUpdateService.UpdateResult.NetworkError ->
                        RefreshStatus.Error(updateResult.message, passId)
                    is PKPassUpdateService.UpdateResult.NoWebService ->
                        RefreshStatus.Error("This pass does not support updates", passId)
                }
            } else {
                _refreshStatus.value = RefreshStatus.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error",
                    passId,
                )
            }

            kotlinx.coroutines.delay(2000)
            _refreshStatus.value = RefreshStatus.Idle
        }
    }

    fun refreshAllPasses() {
        viewModelScope.launch {
            _refreshStatus.value = RefreshStatus.Loading(null)
            val result = passRepository.refreshAllPasses()

            if (result.isSuccess) {
                val results = result.getOrThrow()
                val updatedCount = results.values.count { it is PKPassUpdateService.UpdateResult.Updated }
                val errorResults = results.values.filterIsInstance<PKPassUpdateService.UpdateResult.NetworkError>()

                _refreshStatus.value = if (errorResults.isNotEmpty()) {
                    RefreshStatus.Error(errorResults.first().message, null)
                } else {
                    RefreshStatus.Success(updatedCount)
                }
            } else {
                _refreshStatus.value = RefreshStatus.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error",
                    null,
                )
            }

            kotlinx.coroutines.delay(2000)
            _refreshStatus.value = RefreshStatus.Idle
        }
    }

    fun deletePass(pass: Pass) {
        viewModelScope.launch {
            passRepository.deletePass(pass)
        }
    }

    fun updatePassOrder(passOrderMap: Map<String, Int>) {
        viewModelScope.launch {
            passRepository.updateDisplayOrders(passOrderMap)
        }
    }

    suspend fun getPassById(passId: String): Pass? = passRepository.getPassById(passId)

    fun setAutoRefreshEnabled(passId: String, enabled: Boolean) {
        viewModelScope.launch {
            passRepository.setAutoRefreshEnabled(passId, enabled)
        }
    }

    fun prepareSharePass(passId: String) {
        viewModelScope.launch {
            _shareStatus.value = ShareStatus.Loading(passId)
            val result = passRepository.exportPassForSharing(passId)

            _shareStatus.value = if (result.isSuccess) {
                ShareStatus.Success(result.getOrThrow(), passId)
            } else {
                ShareStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to share pass",
                    passId,
                )
            }
        }
    }

    fun resetShareStatus() {
        _shareStatus.value = ShareStatus.Idle
    }
}
